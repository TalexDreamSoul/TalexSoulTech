package pubsher.talexsoultech.talex.electricity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * 与 Bukkit 无关的电网拓扑与能量结算引擎。
 *
 * <p>拓扑仅在注册表变化时重建。每个周期先运行端点生产逻辑，再优先满足消费者，
 * 最后用发电机剩余电量为储能设备充电。储能只在发电机不足时补充消费者，且同一
 * 周期不会同时充放电。</p>
 */
public final class PowerGrid {

    private static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0},
            {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final Comparator<PowerEndpoint> ENDPOINT_KEY_ORDER =
            Comparator.comparing(PowerEndpoint::key);

    private final int maxNetworkNodes;
    private final Map<BlockKey, PowerEndpoint> endpoints = new HashMap<>();
    private final Map<BlockKey, PowerCable> cables = new HashMap<>();

    private List<PowerNetwork> networks = List.of();
    private boolean topologyDirty = true;
    private long topologyVersion;
    private long cycle;

    public PowerGrid(int maxNetworkNodes) {
        if (maxNetworkNodes <= 0) throw new IllegalArgumentException("maxNetworkNodes must be positive");
        this.maxNetworkNodes = maxNetworkNodes;
    }

    public void register(PowerEndpoint endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        PowerEndpoint existing = endpoints.get(endpoint.key());
        if (existing == endpoint) return;
        if (existing != null || cables.containsKey(endpoint.key())) {
            throw new IllegalStateException("power node already registered at " + endpoint.key());
        }
        endpoints.put(endpoint.key(), endpoint);
        topologyDirty = true;
    }

    public void register(PowerCable cable) {
        Objects.requireNonNull(cable, "cable");
        PowerCable existing = cables.get(cable.key());
        if (cable.equals(existing)) return;
        if (existing != null || endpoints.containsKey(cable.key())) {
            throw new IllegalStateException("power node already registered at " + cable.key());
        }
        cables.put(cable.key(), cable);
        topologyDirty = true;
    }

    public boolean unregister(BlockKey key) {
        Objects.requireNonNull(key, "key");
        boolean changed = endpoints.remove(key) != null;
        changed |= cables.remove(key) != null;
        topologyDirty |= changed;
        return changed;
    }

    public Optional<PowerEndpoint> endpoint(BlockKey key) {
        return Optional.ofNullable(endpoints.get(key));
    }

    public List<PowerEndpoint> endpoints() {
        return List.copyOf(endpoints.values());
    }

    public List<PowerCable> cables() {
        return List.copyOf(cables.values());
    }

    public void clear() {
        endpoints.clear();
        cables.clear();
        networks = List.of();
        topologyDirty = true;
    }

    public PowerCycleStats tick() {
        long startedAt = System.nanoTime();

        List<PowerEndpoint> endpointSnapshot = new ArrayList<>(endpoints.values());
        endpointSnapshot.sort(ENDPOINT_KEY_ORDER);
        for (PowerEndpoint endpoint : endpointSnapshot) {
            if (endpoints.get(endpoint.key()) == endpoint) endpoint.beforePowerCycle();
        }

        if (topologyDirty) rebuildNetworks();
        cycle++;

        FlowTotals totals = new FlowTotals();
        Set<BlockKey> changedEndpoints = new TreeSet<>();
        int oversizedNetworks = 0;

        for (PowerNetwork network : networks) {
            if (network.oversized) {
                oversizedNetworks++;
                continue;
            }
            settle(network, totals, changedEndpoints);
        }

        for (BlockKey key : changedEndpoints) {
            PowerEndpoint endpoint = endpoints.get(key);
            if (endpoint != null) endpoint.onPowerChanged();
        }

        return new PowerCycleStats(
                cycle,
                topologyVersion,
                networks.size(),
                oversizedNetworks,
                endpoints.size(),
                cables.size(),
                totals.gross,
                totals.delivered,
                totals.loss,
                totals.unmetDemand,
                System.nanoTime() - startedAt
        );
    }

    private void rebuildNetworks() {
        Set<BlockKey> allNodes = new TreeSet<>();
        allNodes.addAll(endpoints.keySet());
        allNodes.addAll(cables.keySet());

        Set<BlockKey> visited = new HashSet<>(allNodes.size());
        List<PowerNetwork> rebuilt = new ArrayList<>();

        for (BlockKey seed : allNodes) {
            if (!visited.add(seed)) continue;

            Set<BlockKey> component = new HashSet<>();
            ArrayDeque<BlockKey> queue = new ArrayDeque<>();
            queue.add(seed);
            boolean oversized = false;

            while (!queue.isEmpty()) {
                BlockKey current = queue.removeFirst();
                component.add(current);
                if (component.size() > maxNetworkNodes) oversized = true;

                for (int[] offset : NEIGHBOR_OFFSETS) {
                    BlockKey neighbor = current.relative(offset[0], offset[1], offset[2]);
                    if (!allNodes.contains(neighbor) || !visited.add(neighbor)) continue;
                    queue.addLast(neighbor);
                }
            }

            List<PowerEndpoint> componentEndpoints = component.stream()
                    .map(endpoints::get)
                    .filter(Objects::nonNull)
                    .sorted(ENDPOINT_KEY_ORDER)
                    .toList();
            Map<BlockKey, PowerCable> componentCables = new HashMap<>();
            for (BlockKey key : component) {
                PowerCable cable = cables.get(key);
                if (cable != null) componentCables.put(key, cable);
            }
            rebuilt.add(new PowerNetwork(component, componentEndpoints, componentCables, oversized));
        }

        networks = List.copyOf(rebuilt);
        topologyVersion++;
        topologyDirty = false;
    }

    private void settle(PowerNetwork network, FlowTotals totals, Set<BlockKey> changedEndpoints) {
        long fairnessCursor = cycle;

        List<PowerEndpoint> producers = ordered(network.endpoints, PowerEndpointType.PRODUCER, fairnessCursor);
        List<PowerEndpoint> storageSources = ordered(network.endpoints, PowerEndpointType.STORAGE, fairnessCursor);
        List<PowerEndpoint> consumers = ordered(network.endpoints, PowerEndpointType.CONSUMER, fairnessCursor);
        List<PowerEndpoint> storageTargets = ordered(network.endpoints, PowerEndpointType.STORAGE, fairnessCursor);

        Map<BlockKey, Long> sourceBudgets = new HashMap<>();
        for (PowerEndpoint source : producers) sourceBudgets.put(source.key(), extractBudget(source));
        for (PowerEndpoint source : storageSources) sourceBudgets.put(source.key(), extractBudget(source));

        Map<BlockKey, Long> cableBudgets = new HashMap<>();
        for (PowerCable cable : network.cables.values()) {
            cableBudgets.put(cable.key(), cable.throughputPerCycle());
        }

        Set<BlockKey> dischargedStorage = new HashSet<>();
        List<PowerEndpoint> consumerSources = new ArrayList<>(producers.size() + storageSources.size());
        consumerSources.addAll(producers);
        consumerSources.addAll(storageSources);

        transferToTargets(
                network,
                consumers,
                consumerSources,
                sourceBudgets,
                cableBudgets,
                dischargedStorage,
                true,
                totals,
                changedEndpoints
        );

        List<PowerEndpoint> chargeableStorage = storageTargets.stream()
                .filter(storage -> !dischargedStorage.contains(storage.key()))
                .toList();
        transferToTargets(
                network,
                chargeableStorage,
                producers,
                sourceBudgets,
                cableBudgets,
                dischargedStorage,
                false,
                totals,
                changedEndpoints
        );
    }

    private void transferToTargets(
            PowerNetwork network,
            Collection<PowerEndpoint> targets,
            Collection<PowerEndpoint> sources,
            Map<BlockKey, Long> sourceBudgets,
            Map<BlockKey, Long> cableBudgets,
            Set<BlockKey> dischargedStorage,
            boolean countUnmetDemand,
            FlowTotals totals,
            Set<BlockKey> changedEndpoints
    ) {
        for (PowerEndpoint target : targets) {
            long remainingDemand = receiveBudget(target);
            if (remainingDemand == 0) continue;

            for (PowerEndpoint source : sources) {
                if (source.key().equals(target.key())) continue;

                long sourceBudget = sourceBudgets.getOrDefault(source.key(), 0L);
                while (sourceBudget > 0 && remainingDemand > 0) {
                    Route route = network.route(source.key(), target.key(), cableBudgets);
                    if (route == null) break;

                    long pathBudget = route.availableThroughput(cableBudgets);
                    long grossLimit = Math.min(sourceBudget, pathBudget);
                    long gross = route.grossForDemand(grossLimit, remainingDemand);
                    if (gross == 0) break;

                    long delivered = route.delivered(gross);
                    if (delivered == 0) break;

                    long extracted = source.buffer().extract(gross, false);
                    if (extracted != gross) {
                        source.buffer().receive(extracted, false);
                        throw new IllegalStateException("source budget changed during power settlement");
                    }

                    long accepted = target.buffer().receive(delivered, false);
                    if (accepted != delivered) {
                        source.buffer().receive(extracted, false);
                        target.buffer().extract(accepted, false);
                        throw new IllegalStateException("target budget changed during power settlement");
                    }

                    route.consumeThroughput(cableBudgets, gross);
                    sourceBudget -= gross;
                    sourceBudgets.put(source.key(), sourceBudget);
                    remainingDemand -= delivered;

                    if (source.type() == PowerEndpointType.STORAGE) dischargedStorage.add(source.key());
                    changedEndpoints.add(source.key());
                    changedEndpoints.add(target.key());

                    totals.gross += gross;
                    totals.delivered += delivered;
                    totals.loss += gross - delivered;
                }

                if (remainingDemand == 0) break;
            }

            if (countUnmetDemand) totals.unmetDemand += remainingDemand;
        }
    }

    private static long extractBudget(PowerEndpoint source) {
        long limit = source.maxExtractPerCycle();
        EnergyUnits.requireNonNegative(limit);
        return source.buffer().extract(limit, true);
    }

    private static long receiveBudget(PowerEndpoint target) {
        long limit = target.maxReceivePerCycle();
        EnergyUnits.requireNonNegative(limit);
        return target.buffer().receive(limit, true);
    }

    private static List<PowerEndpoint> ordered(
            Collection<PowerEndpoint> candidates,
            PowerEndpointType type,
            long fairnessCursor
    ) {
        List<PowerEndpoint> ordered = candidates.stream()
                .filter(endpoint -> endpoint.type() == type)
                .sorted(Comparator.comparingInt(PowerEndpoint::priority).reversed()
                        .thenComparing(PowerEndpoint::key))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int groupStart = 0;
        while (groupStart < ordered.size()) {
            int priority = ordered.get(groupStart).priority();
            int groupEnd = groupStart + 1;
            while (groupEnd < ordered.size() && ordered.get(groupEnd).priority() == priority) groupEnd++;
            List<PowerEndpoint> group = ordered.subList(groupStart, groupEnd);
            if (group.size() > 1) Collections.rotate(group, -(int) Math.floorMod(fairnessCursor, group.size()));
            groupStart = groupEnd;
        }
        return ordered;
    }

    private static final class PowerNetwork {
        private final Set<BlockKey> nodes;
        private final List<PowerEndpoint> endpoints;
        private final Map<BlockKey, PowerCable> cables;
        private final boolean oversized;
        private final Map<RouteKey, List<Route>> routeCandidates = new HashMap<>();
        private final Set<RouteKey> missingRoutes = new HashSet<>();

        private PowerNetwork(
                Set<BlockKey> nodes,
                List<PowerEndpoint> endpoints,
                Map<BlockKey, PowerCable> cables,
                boolean oversized
        ) {
            this.nodes = Set.copyOf(nodes);
            this.endpoints = List.copyOf(endpoints);
            this.cables = Map.copyOf(cables);
            this.oversized = oversized;
        }

        private Route route(
                BlockKey source,
                BlockKey target,
                Map<BlockKey, Long> cableBudgets
        ) {
            RouteKey key = new RouteKey(source, target);
            if (missingRoutes.contains(key)) return null;

            List<Route> candidates = routeCandidates.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (candidates.isEmpty()) {
                Route primary = findRoute(source, target, null);
                if (primary == null) {
                    missingRoutes.add(key);
                    return null;
                }
                candidates.add(primary);
            }

            Map<BlockKey, Long> searchBudgets = cableBudgets;
            for (Route candidate : candidates) {
                long throughput = candidate.availableThroughput(cableBudgets);
                if (throughput <= 0) continue;
                if (candidate.delivered(throughput) > 0) return candidate;
                if (searchBudgets == cableBudgets) searchBudgets = new HashMap<>(cableBudgets);
                candidate.blockBottlenecks(searchBudgets);
            }

            for (int attempt = 0; attempt <= cables.size(); attempt++) {
                Route alternate = findRoute(source, target, searchBudgets);
                if (alternate == null) return null;

                long throughput = alternate.availableThroughput(searchBudgets);
                if (throughput > 0 && alternate.delivered(throughput) > 0) {
                    if (!candidates.contains(alternate)) candidates.add(alternate);
                    return alternate;
                }

                if (searchBudgets == cableBudgets) searchBudgets = new HashMap<>(cableBudgets);
                alternate.blockBottlenecks(searchBudgets);
            }
            return null;
        }

        private Route findRoute(
                BlockKey source,
                BlockKey target,
                Map<BlockKey, Long> cableBudgets
        ) {
            Map<BlockKey, PathCost> best = new HashMap<>();
            Map<BlockKey, BlockKey> parent = new HashMap<>();
            PriorityQueue<PathState> queue = new PriorityQueue<>(Comparator
                    .comparing(PathState::cost)
                    .thenComparing(PathState::key));

            PathCost startCost = new PathCost(0, 0);
            best.put(source, startCost);
            queue.add(new PathState(source, startCost));

            while (!queue.isEmpty()) {
                PathState state = queue.remove();
                if (!state.cost.equals(best.get(state.key))) continue;
                if (state.key.equals(target)) break;
                if (!state.key.equals(source) && containsEndpoint(state.key)) continue;

                for (int[] offset : NEIGHBOR_OFFSETS) {
                    BlockKey neighbor = state.key.relative(offset[0], offset[1], offset[2]);
                    if (!nodes.contains(neighbor)) continue;

                    PowerCable cable = cables.get(neighbor);
                    if (cable != null
                            && cableBudgets != null
                            && cableBudgets.getOrDefault(neighbor, 0L) <= 0L) {
                        continue;
                    }
                    int addedLoss = cable == null ? 0 : cable.lossPermille();
                    PathCost candidate = new PathCost(state.cost.lossPermille + addedLoss, state.cost.hops + 1);
                    PathCost known = best.get(neighbor);
                    if (known != null && candidate.compareTo(known) >= 0) continue;

                    best.put(neighbor, candidate);
                    parent.put(neighbor, state.key);
                    queue.add(new PathState(neighbor, candidate));
                }
            }

            if (!best.containsKey(target)) return null;

            List<PowerCable> routeCables = new ArrayList<>();
            BlockKey cursor = target;
            while (!cursor.equals(source)) {
                PowerCable cable = cables.get(cursor);
                if (cable != null) routeCables.add(cable);
                cursor = parent.get(cursor);
                if (cursor == null) return null;
            }
            Collections.reverse(routeCables);
            return new Route(List.copyOf(routeCables));
        }

        private boolean containsEndpoint(BlockKey key) {
            for (PowerEndpoint endpoint : endpoints) {
                if (endpoint.key().equals(key)) return true;
            }
            return false;
        }
    }

    private record RouteKey(BlockKey source, BlockKey target) {
    }

    private record PathState(BlockKey key, PathCost cost) {
    }

    private record PathCost(int lossPermille, int hops) implements Comparable<PathCost> {
        @Override
        public int compareTo(PathCost other) {
            int lossCompare = Integer.compare(lossPermille, other.lossPermille);
            return lossCompare != 0 ? lossCompare : Integer.compare(hops, other.hops);
        }
    }

    private record Route(List<PowerCable> cables) {

        private long delivered(long gross) {
            long delivered = gross;
            for (PowerCable cable : cables) delivered = cable.transmit(delivered);
            return delivered;
        }

        private long availableThroughput(Map<BlockKey, Long> cableBudgets) {
            long available = Long.MAX_VALUE;
            for (PowerCable cable : cables) {
                available = Math.min(available, cableBudgets.getOrDefault(cable.key(), 0L));
            }
            return available;
        }

        private void blockBottlenecks(Map<BlockKey, Long> cableBudgets) {
            long bottleneck = availableThroughput(cableBudgets);
            if (bottleneck == Long.MAX_VALUE) return;
            for (PowerCable cable : cables) {
                if (cableBudgets.getOrDefault(cable.key(), 0L) == bottleneck) {
                    cableBudgets.put(cable.key(), 0L);
                }
            }
        }

        private void consumeThroughput(Map<BlockKey, Long> cableBudgets, long gross) {
            for (PowerCable cable : cables) {
                cableBudgets.compute(cable.key(), (key, remaining) -> {
                    if (remaining == null || remaining < gross) {
                        throw new IllegalStateException("cable throughput changed during power settlement");
                    }
                    return remaining - gross;
                });
            }
        }

        private long grossForDemand(long grossLimit, long demand) {
            if (grossLimit <= 0 || demand <= 0) return 0L;
            long targetDelivery = Math.min(demand, delivered(grossLimit));
            if (targetDelivery == 0) return 0L;

            long low = 1L;
            long high = grossLimit;
            while (low < high) {
                long middle = low + (high - low) / 2L;
                if (delivered(middle) >= targetDelivery) high = middle;
                else low = middle + 1L;
            }
            return low;
        }
    }

    private static final class FlowTotals {
        private long gross;
        private long delivered;
        private long loss;
        private long unmetDemand;
    }
}
