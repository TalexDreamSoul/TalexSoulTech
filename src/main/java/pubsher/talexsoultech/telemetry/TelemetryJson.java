package pubsher.talexsoultech.telemetry;

import java.util.List;
import java.util.Map;

/**
 * Renders a drain as the frozen {@code telemetry} object of the sync payload.
 *
 * <p>Built with the same manual {@code StringBuilder} approach the rest of the
 * snapshot uses so telemetry adds no serialization dependency. The output is
 * deterministic: days ascending, groups in declaration order, keys sorted.</p>
 */
public final class TelemetryJson {

    private TelemetryJson() {
    }

    /** Returns {@code {"v":1,"days":[...]}} for a non-empty drain, or null when there is nothing to send. */
    public static String toJson(TelemetryDrain drain) {
        if (drain == null || drain.isEmpty()) {
            return null;
        }

        List<TelemetryDrain.Day> days = drain.days();
        StringBuilder json = new StringBuilder(512);
        json.append("{\"v\":").append(TelemetryDrain.VERSION).append(",\"days\":[");
        int emitted = 0;
        for (TelemetryDrain.Day day : days) {
            if (emitted >= TelemetryCollector.MAX_DAYS) {
                break;
            }
            if (emitted++ > 0) {
                json.append(',');
            }
            appendDay(json, day);
        }
        json.append("]}");
        return json.toString();
    }

    private static void appendDay(StringBuilder json, TelemetryDrain.Day day) {
        json.append("{\"day\":");
        appendJsonString(json, day.day());
        json.append(",\"counters\":{");
        int groups = 0;
        for (TelemetryMetric metric : day.metrics()) {
            if (groups++ > 0) {
                json.append(',');
            }
            appendJsonString(json, metric.wireName());
            json.append(':');
            appendCounters(json, day.counters().get(metric));
        }
        json.append("}}");
    }

    private static void appendCounters(StringBuilder json, Map<String, Long> counters) {
        json.append('{');
        int index = 0;
        for (Map.Entry<String, Long> entry : counters.entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            appendJsonString(json, entry.getKey());
            json.append(':').append(entry.getValue().longValue());
        }
        json.append('}');
    }

    private static void appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        appendUnicodeEscape(json, character);
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder json, char value) {
        json.append("\\u");
        for (int shift = 12; shift >= 0; shift -= 4) {
            json.append(Character.forDigit((value >>> shift) & 0xF, 16));
        }
    }
}
