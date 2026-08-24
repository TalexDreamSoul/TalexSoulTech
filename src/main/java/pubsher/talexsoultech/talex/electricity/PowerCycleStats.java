package pubsher.talexsoultech.talex.electricity;

/**
 * 最近一次电网周期的可观测统计。
 */
public record PowerCycleStats(
        long cycle,
        long topologyVersion,
        int networkCount,
        int oversizedNetworkCount,
        int endpointCount,
        int cableCount,
        long grossEnergy,
        long deliveredEnergy,
        long lostEnergy,
        long unmetConsumerDemand,
        long durationNanos
) {
}
