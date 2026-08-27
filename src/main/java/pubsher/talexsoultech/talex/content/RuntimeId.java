package pubsher.talexsoultech.talex.content;

import java.util.Locale;

/** Canonical conversion between dotted planning identity and runtime snake-case identity. */
public final class RuntimeId {
    private RuntimeId() {
    }

    /**
     * Normalizes a planning ID to a lower snake-case runtime ID.
     * Dots and hyphens are separators, repeated separators collapse, and the generated
     * identity never carries the historical {@code st_} prefix.
     */
    public static String normalize(String planningId) {
        if (planningId == null || planningId.isBlank()) {
            throw new IllegalArgumentException("planning ID must not be blank");
        }
        String source = planningId.trim();
        if (!source.equals(planningId)) {
            throw new IllegalArgumentException("planning ID must not contain surrounding whitespace: " + planningId);
        }
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            if (!(character == '.' || character == '-' || character == '_'
                    || character >= 'a' && character <= 'z'
                    || character >= 'A' && character <= 'Z'
                    || character >= '0' && character <= '9')) {
                throw new IllegalArgumentException("planning ID contains unsupported character: " + planningId);
            }
        }
        if (source.charAt(0) == '.' || source.charAt(0) == '-'
                || source.charAt(0) == '_' || source.charAt(source.length() - 1) == '.'
                || source.charAt(source.length() - 1) == '-'
                || source.charAt(source.length() - 1) == '_') {
            throw new IllegalArgumentException("planning ID has an invalid separator boundary: " + planningId);
        }

        StringBuilder normalized = new StringBuilder(source.length());
        boolean separatorPending = false;
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            if (character == '.' || character == '-' || character == '_') {
                separatorPending = true;
                continue;
            }
            if (separatorPending && normalized.length() > 0) {
                normalized.append('_');
            }
            normalized.append(Character.toLowerCase(character));
            separatorPending = false;
        }
        String result = normalized.toString();
        if (!isValid(result)) {
            throw new IllegalArgumentException("normalized runtime ID is invalid: " + result);
        }
        return result;
    }

    public static String fromPlanningId(String planningId) {
        return normalize(planningId);
    }

    public static String normalizeRuntimeId(String planningId) {
        return normalize(planningId);
    }

    /** Returns whether a persisted runtime/PDC ID follows the canonical pattern. */
    public static boolean isValid(String runtimeId) {
        if (runtimeId == null || runtimeId.isEmpty() || runtimeId.startsWith("st_")) return false;
        if (runtimeId.charAt(0) < 'a' || runtimeId.charAt(0) > 'z') return false;
        if (runtimeId.charAt(runtimeId.length() - 1) == '_') return false;
        boolean previousSeparator = false;
        for (int i = 0; i < runtimeId.length(); i++) {
            char character = runtimeId.charAt(i);
            boolean letter = character >= 'a' && character <= 'z';
            boolean digit = character >= '0' && character <= '9';
            if (character == '_') {
                if (previousSeparator) return false;
                previousSeparator = true;
            } else if (letter || digit) {
                previousSeparator = false;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean isCanonical(String planningId, String runtimeId) {
        try {
            return runtimeId != null && normalize(planningId).equals(runtimeId);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
