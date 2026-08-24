package pubsher.talexsoultech.extensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded JSON reader for the two extension cloud contracts. */
final class ExtensionJson {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_COLLECTION_ITEMS = 1_024;

    private ExtensionJson() {
    }

    static List<ExtensionDescriptor> manifestResponse(String input) {
        Map<String, Object> root = object(parse(input));
        List<Object> rawExtensions = array(root.get("extensions"));
        if (rawExtensions.size() > MAX_COLLECTION_ITEMS) {
            throw invalid();
        }
        List<ExtensionDescriptor> descriptors = new ArrayList<>(rawExtensions.size());
        Set<String> ids = new LinkedHashSet<>();
        for (Object rawExtension : rawExtensions) {
            ExtensionDescriptor descriptor = descriptor(object(rawExtension), true);
            if (!ids.add(descriptor.manifest().id())) {
                throw invalid();
            }
            descriptors.add(descriptor);
        }
        return List.copyOf(descriptors);
    }

    static SourceResponse sourceResponse(String input) {
        Map<String, Object> root = object(parse(input));
        ExtensionDescriptor descriptor = descriptor(root, false);
        Object source = root.get("source");
        if (!(source instanceof String text)) {
            throw invalid();
        }
        return new SourceResponse(descriptor, text);
    }

    private static ExtensionDescriptor descriptor(Map<String, Object> object, boolean includesEnabled) {
        ExtensionManifest manifest = manifest(object(object.get("manifest")));
        long revision = nonNegativeLong(object.get("revision"));
        String sha256 = string(object.get("sha256"));
        boolean enabled;
        if (includesEnabled) {
            Object value = object.get("enabled");
            if (!(value instanceof Boolean booleanValue)) {
                throw invalid();
            }
            enabled = booleanValue;
        } else {
            enabled = true;
        }
        return new ExtensionDescriptor(manifest, revision, sha256, enabled);
    }

    private static ExtensionManifest manifest(Map<String, Object> object) {
        String id = string(object.get("id"));
        String name = string(object.get("name"));
        String version = string(object.get("version"));
        ExtensionManifest.Engine engine = ExtensionManifest.Engine.fromWire(string(object.get("engine")));
        String entry = string(object.get("entry"));
        List<String> dependencies = strings(array(object.get("dependencies")));
        Set<ExtensionManifest.Capability> permissions = permissions(array(object.get("permissions")));
        return new ExtensionManifest(id, name, version, engine, entry, dependencies, permissions);
    }

    private static Set<ExtensionManifest.Capability> permissions(List<Object> rawPermissions) {
        if (rawPermissions.size() > ExtensionManifest.Capability.values().length) {
            throw invalid();
        }
        Set<ExtensionManifest.Capability> values = new LinkedHashSet<>();
        for (Object rawPermission : rawPermissions) {
            if (!values.add(ExtensionManifest.Capability.fromWire(string(rawPermission)))) {
                throw invalid();
            }
        }
        return Set.copyOf(values);
    }

    private static List<String> strings(List<Object> values) {
        if (values.size() > MAX_COLLECTION_ITEMS) {
            throw invalid();
        }
        List<String> strings = new ArrayList<>(values.size());
        for (Object value : values) {
            strings.add(string(value));
        }
        return List.copyOf(strings);
    }

    private static Object parse(String input) {
        if (input == null || input.isEmpty()) {
            throw invalid();
        }
        Reader reader = new Reader(input);
        Object result = reader.value(0);
        reader.whitespace();
        if (!reader.finished()) {
            throw invalid();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid();
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        if (!(value instanceof List<?> list)) {
            throw invalid();
        }
        return (List<Object>) list;
    }

    private static String string(Object value) {
        if (!(value instanceof String text)) {
            throw invalid();
        }
        return text;
    }

    private static long nonNegativeLong(Object value) {
        if (!(value instanceof Long number) || number < 0L) {
            throw invalid();
        }
        return number;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Extension cloud response is invalid");
    }

    record SourceResponse(ExtensionDescriptor descriptor, String source) {
    }

    private static final class Reader {
        private final String input;
        private int index;

        private Reader(String input) {
            this.input = input;
        }

        private Object value(int depth) {
            if (depth > MAX_DEPTH || index >= input.length()) {
                throw invalid();
            }
            return switch (input.charAt(index)) {
                case '{' -> object(depth + 1);
                case '[' -> array(depth + 1);
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object(int depth) {
            expect('{');
            whitespace();
            Map<String, Object> values = new LinkedHashMap<>();
            if (consume('}')) {
                return Map.copyOf(values);
            }
            while (true) {
                if (values.size() >= MAX_COLLECTION_ITEMS) {
                    throw invalid();
                }
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                whitespace();
                if (values.containsKey(key)) {
                    throw invalid();
                }
                Object parsed = value(depth);
                if (parsed == null) {
                    throw invalid();
                }
                values.put(key, parsed);
                whitespace();
                if (consume('}')) {
                    return Map.copyOf(values);
                }
                expect(',');
                whitespace();
            }
        }

        private List<Object> array(int depth) {
            expect('[');
            whitespace();
            List<Object> values = new ArrayList<>();
            if (consume(']')) {
                return List.copyOf(values);
            }
            while (true) {
                if (values.size() >= MAX_COLLECTION_ITEMS) {
                    throw invalid();
                }
                Object parsed = value(depth);
                if (parsed == null) {
                    throw invalid();
                }
                values.add(parsed);
                whitespace();
                if (consume(']')) {
                    return List.copyOf(values);
                }
                expect(',');
                whitespace();
            }
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character < 0x20) {
                    throw invalid();
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (index >= input.length()) {
                    throw invalid();
                }
                switch (input.charAt(index++)) {
                    case '"', '\\', '/' -> value.append(input.charAt(index - 1));
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(unicode());
                    default -> throw invalid();
                }
            }
            throw invalid();
        }

        private char unicode() {
            if (index + 4 > input.length()) {
                throw invalid();
            }
            int result = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = Character.digit(input.charAt(index++), 16);
                if (digit < 0) {
                    throw invalid();
                }
                result = (result << 4) | digit;
            }
            return (char) result;
        }

        private Long number() {
            int start = index;
            if (consume('-') || consume('+')) {
                throw invalid();
            }
            if (index >= input.length()) {
                throw invalid();
            }
            if (input.charAt(index) == '0') {
                index++;
            } else if (isDigitOneToNine(input.charAt(index))) {
                do {
                    index++;
                } while (index < input.length() && Character.isDigit(input.charAt(index)));
            } else {
                throw invalid();
            }
            if (index < input.length() && (input.charAt(index) == '.' || input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                throw invalid();
            }
            try {
                return Long.parseLong(input.substring(start, index));
            } catch (NumberFormatException exception) {
                throw invalid();
            }
        }

        private Object literal(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw invalid();
            }
            index += literal.length();
            return value;
        }

        private void whitespace() {
            while (index < input.length()) {
                char character = input.charAt(index);
                if (character != ' ' && character != '\n' && character != '\r' && character != '\t') {
                    return;
                }
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw invalid();
            }
        }

        private boolean finished() {
            return index == input.length();
        }

        private static boolean isDigitOneToNine(char character) {
            return character >= '1' && character <= '9';
        }
    }
}
