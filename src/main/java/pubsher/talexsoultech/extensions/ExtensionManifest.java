package pubsher.talexsoultech.extensions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, server-validated extension declaration. */
public record ExtensionManifest(
        String id,
        String name,
        String version,
        Engine engine,
        String entry,
        List<String> dependencies,
        Set<Capability> permissions
) {
    public static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    public static final Pattern LUA_ENTRY_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*$");
    public static final Pattern JAVASCRIPT_ENTRY_PATTERN = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*$");
    private static final int MAX_TEXT_LENGTH = 128;

    public ExtensionManifest {
        id = requireId(id);
        name = requireText(name, "name");
        version = requireText(version, "version");
        engine = Objects.requireNonNull(engine, "engine");
        entry = requireEntry(entry, engine);
        dependencies = normalizeDependencies(id, dependencies);
        permissions = normalizePermissions(permissions);
    }

    public String sourceExtension() {
        return engine == Engine.LUA ? "lua" : "js";
    }

    public static String requireId(String value) {
        String normalized = requireText(value, "id");
        if (!ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Extension id is invalid");
        }
        return normalized;
    }

    private static String requireEntry(String value, Engine engine) {
        String normalized = requireText(value, "entry");
        Pattern pattern = engine == Engine.LUA ? LUA_ENTRY_PATTERN : JAVASCRIPT_ENTRY_PATTERN;
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Extension entry is invalid");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()
                || normalized.length() > MAX_TEXT_LENGTH
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Extension " + field + " is invalid");
        }
        return normalized;
    }

    private static List<String> normalizeDependencies(String id, List<String> values) {
        Objects.requireNonNull(values, "dependencies");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String dependency : values) {
            String dependencyId = requireId(dependency);
            if (id.equals(dependencyId) || !normalized.add(dependencyId)) {
                throw new IllegalArgumentException("Extension dependencies are invalid");
            }
        }
        return List.copyOf(normalized);
    }

    private static Set<Capability> normalizePermissions(Set<Capability> values) {
        Objects.requireNonNull(values, "permissions");
        return Set.copyOf(values);
    }

    public enum Engine {
        LUA("lua"),
        JAVASCRIPT("javascript");

        private final String wireName;

        Engine(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        public static Engine fromWire(String value) {
            String normalized = Objects.requireNonNull(value, "engine").toLowerCase(Locale.ROOT);
            for (Engine engine : values()) {
                if (engine.wireName.equals(normalized)) {
                    return engine;
                }
            }
            throw new IllegalArgumentException("Extension engine is invalid");
        }
    }

    public enum Capability {
        LOG("log"),
        SCHEDULE("schedule"),
        EVENTS("events"),
        COMMANDS("commands"),
        KV("kv"),
        CATALOG("catalog");

        private final String wireName;

        Capability(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        public static Capability fromWire(String value) {
            String normalized = Objects.requireNonNull(value, "permission").toLowerCase(Locale.ROOT);
            for (Capability capability : values()) {
                if (capability.wireName.equals(normalized)) {
                    return capability;
                }
            }
            throw new IllegalArgumentException("Extension permission is invalid");
        }
    }
}
