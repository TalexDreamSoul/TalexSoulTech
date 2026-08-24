package pubsher.talexsoultech.talex.multiblock;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 灵魂科技通用紧凑型和工业型外壳。
 */
public final class MultiblockTemplates {

    private static final Set<Material> COMPACT_CASING = Set.of(
            Material.IRON_BLOCK,
            Material.COPPER_BLOCK,
            Material.WAXED_COPPER_BLOCK
    );
    private static final Set<Material> INDUSTRIAL_CASING = Set.of(
            Material.IRON_BLOCK,
            Material.CUT_COPPER,
            Material.WAXED_CUT_COPPER
    );
    private static final Set<Material> WINDOWS = Set.of(
            Material.GLASS,
            Material.TINTED_GLASS,
            Material.IRON_BARS
    );

    private static final MultiblockTemplate COMPACT = create("compact_3x3x3", 3, Material.REDSTONE_BLOCK);
    private static final MultiblockTemplate INDUSTRIAL = create("industrial_5x5x5", 5, Material.LODESTONE);

    private MultiblockTemplates() {
    }

    public static MultiblockTemplate compact3x3x3() {
        return COMPACT;
    }

    public static MultiblockTemplate industrial5x5x5() {
        return INDUSTRIAL;
    }

    private static MultiblockTemplate create(String id, int size, Material coreMaterial) {
        int radius = size / 2;
        int middle = size / 2;
        Set<Material> casing = size == 3 ? COMPACT_CASING : INDUSTRIAL_CASING;
        Map<MultiblockTemplate.Offset, MultiblockTemplate.Requirement> requirements = new LinkedHashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    MultiblockTemplate.Offset offset = new MultiblockTemplate.Offset(x, y, z);
                    if (x == 0 && y == middle && z == middle) {
                        requirements.put(offset, MultiblockTemplate.Requirement.exact(coreMaterial, "能量核心"));
                        continue;
                    }

                    boolean shell = Math.abs(x) == radius || y == 0 || y == size - 1 || z == 0 || z == size - 1;
                    if (!shell) {
                        requirements.put(offset, MultiblockTemplate.Requirement.air());
                        continue;
                    }

                    boolean window = y == middle
                            && ((z == 0 || z == size - 1) && Math.abs(x) < radius);
                    requirements.put(
                            offset,
                            window
                                    ? MultiblockTemplate.Requirement.anyOf(WINDOWS, "观察窗")
                                    : MultiblockTemplate.Requirement.anyOf(casing, "机器外壳")
                    );
                }
            }
        }

        return new MultiblockTemplate(id, size, requirements);
    }
}
