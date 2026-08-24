package pubsher.talexsoultech.talex.multiblock;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 以控制器为原点、面向玩家方向定义的多方块结构模板。
 */
public final class MultiblockTemplate {

    private final String id;
    private final int size;
    private final Map<Offset, Requirement> requirements;

    public MultiblockTemplate(String id, int size, Map<Offset, Requirement> requirements) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (size != 3 && size != 5) throw new IllegalArgumentException("only 3x3x3 and 5x5x5 are supported");
        this.id = id;
        this.size = size;
        this.requirements = Map.copyOf(new LinkedHashMap<>(requirements));
    }

    public String id() {
        return id;
    }

    public int size() {
        return size;
    }

    public Map<Offset, Requirement> requirements() {
        return requirements;
    }

    public record Offset(int x, int y, int z) {
    }

    public record Requirement(Set<Material> acceptedMaterials, String description) {
        public Requirement {
            acceptedMaterials = Set.copyOf(Objects.requireNonNull(acceptedMaterials, "acceptedMaterials"));
            if (acceptedMaterials.isEmpty()) throw new IllegalArgumentException("acceptedMaterials must not be empty");
            if (description == null || description.isBlank()) throw new IllegalArgumentException("description must not be blank");
        }

        public static Requirement exact(Material material, String description) {
            return new Requirement(Set.of(material), description);
        }

        public static Requirement anyOf(Set<Material> materials, String description) {
            return new Requirement(materials, description);
        }

        public static Requirement air() {
            return exact(Material.AIR, "空气");
        }

        public boolean matches(Material material) {
            return acceptedMaterials.contains(material);
        }
    }
}
