package pubsher.talexsoultech.extensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable data crossing from Paper into an untrusted script. */
public record ExtensionInvocation(String type, Map<String, Object> data) {
    private static final int MAX_FIELDS = 32;
    private static final int MAX_STRING_LENGTH = 512;

    public ExtensionInvocation {
        type = Objects.requireNonNull(type, "type");
        if (type.isBlank() || type.length() > 64) {
            throw new IllegalArgumentException("Invocation type is invalid");
        }
        Objects.requireNonNull(data, "data");
        if (data.size() > MAX_FIELDS) {
            throw new IllegalArgumentException("Invocation data is too large");
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "data key");
            if (key.isBlank() || key.length() > 64) {
                throw new IllegalArgumentException("Invocation key is invalid");
            }
            copied.put(key, copyValue(entry.getValue()));
        }
        data = Map.copyOf(copied);
    }

    private static Object copyValue(Object value) {
        if (value == null || value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof Double) {
            return value;
        }
        if (value instanceof String text) {
            if (text.length() > MAX_STRING_LENGTH) {
                throw new IllegalArgumentException("Invocation text is too large");
            }
            return text;
        }
        if (value instanceof List<?> values) {
            if (values.size() > MAX_FIELDS) {
                throw new IllegalArgumentException("Invocation list is too large");
            }
            List<String> copied = new ArrayList<>(values.size());
            for (Object item : values) {
                if (!(item instanceof String text) || text.length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("Invocation list value is invalid");
                }
                copied.add(text);
            }
            return List.copyOf(copied);
        }
        throw new IllegalArgumentException("Invocation value is not a basic DTO value");
    }
}
