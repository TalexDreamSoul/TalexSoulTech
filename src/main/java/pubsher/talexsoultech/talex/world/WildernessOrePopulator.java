package pubsher.talexsoultech.talex.world;

import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

final class WildernessOrePopulator extends BlockPopulator {

    private final WildernessSettings settings;
    private volatile boolean active = true;

    WildernessOrePopulator(WildernessSettings settings) {
        this.settings = settings;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random ignoredRandom, int chunkX, int chunkZ, LimitedRegion region) {
        if (!active || worldInfo.getEnvironment() != World.Environment.NORMAL || !settings.isOreGenerationEnabledFor(worldInfo.getName())) {
            return;
        }

        WildernessSettings.OreSettings ore = settings.ore();
        int minY = Math.max(ore.minY(), worldInfo.getMinHeight());
        int maxY = Math.min(ore.maxY(), worldInfo.getMaxHeight() - 1);
        if (minY > maxY) {
            return;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int candidates = WildernessOrePlan.candidateCount(ore);
        for (int index = 0; index < candidates; index++) {
            int position = WildernessOrePlan.positionAt(worldInfo.getSeed(), chunkX, chunkZ, minY, maxY, ore, index);
            if (!WildernessOrePlan.isValid(position)) {
                continue;
            }

            BlockState state = region.getBlockState(baseX + WildernessOrePlan.localX(position),
                    WildernessOrePlan.y(position), baseZ + WildernessOrePlan.localZ(position));
            if (!ore.replaceable().contains(state.getType())) {
                continue;
            }

            state.setType(ore.material());
            state.update(true, false);
        }
    }

    void close() {
        active = false;
    }
}
