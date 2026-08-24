package pubsher.talexsoultech.extensions;

import org.bukkit.configuration.file.FileConfiguration;

/** Bounded extension runtime settings owned by the plugin configuration. */
record ExtensionSettings(
        boolean enabled,
        long refreshIntervalSeconds,
        int maxSourceBytes,
        long callbackBudgetMillis,
        long instructionBudget
) {
    private static final long DEFAULT_REFRESH_SECONDS = 60L;
    private static final int DEFAULT_MAX_SOURCE_BYTES = 131_072;
    private static final long DEFAULT_CALLBACK_BUDGET_MILLIS = 50L;
    private static final long DEFAULT_INSTRUCTION_BUDGET = 100_000L;

    static ExtensionSettings read(FileConfiguration config) {
        return new ExtensionSettings(
                config.getBoolean("Settings.extensions.enabled", true),
                clamp(config.getLong("Settings.extensions.refresh-interval-seconds", DEFAULT_REFRESH_SECONDS), 15L, 3_600L),
                (int) clamp(config.getLong("Settings.extensions.max-source-bytes", DEFAULT_MAX_SOURCE_BYTES), 1_024L, DEFAULT_MAX_SOURCE_BYTES),
                clamp(config.getLong("Settings.extensions.callback-budget-millis", DEFAULT_CALLBACK_BUDGET_MILLIS), 1L, 500L),
                DEFAULT_INSTRUCTION_BUDGET
        );
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
