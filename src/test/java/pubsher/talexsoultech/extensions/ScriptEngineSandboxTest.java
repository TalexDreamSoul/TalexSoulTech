package pubsher.talexsoultech.extensions;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ScriptEngineSandboxTest {

    private static final Duration ENGINE_TEST_CEILING = Duration.ofSeconds(2);

    @Test
    void luaRunsWithoutFileSystemJavaPackageOrDynamicLoadingGlobals() throws Exception {
        RestrictedBridge bridge = new RestrictedBridge();
        ScriptExtensionEngine engine = new LuaScriptEngine(manifest(ExtensionManifest.Engine.LUA, Set.of(
                ExtensionManifest.Capability.LOG
        )), """
                function boot(api)
                  if io == nil and os == nil and luajava == nil and package == nil and load == nil then
                    api.log("sandboxed")
                  else
                    api.log("unsafe")
                  end
                end
                """);

        try {
            engine.initialize(bridge);
            assertEquals(List.of("sandboxed"), bridge.logs());
        } finally {
            engine.close();
        }
    }

    @Test
    void rhinoRunsWithoutPackageJavaOrReflectionGlobals() throws Exception {
        RestrictedBridge bridge = new RestrictedBridge();
        ScriptExtensionEngine engine = new RhinoScriptEngine(manifest(ExtensionManifest.Engine.JAVASCRIPT, Set.of(
                ExtensionManifest.Capability.LOG
        )), """
                function boot(api) {
                  api.log(
                    typeof Packages === "undefined"
                    && typeof java === "undefined"
                    && typeof getClass === "undefined"
                    ? "sandboxed" : "unsafe"
                  );
                }
                """);

        try {
            engine.initialize(bridge);
            assertEquals(List.of("sandboxed"), bridge.logs());
        } finally {
            engine.close();
        }
    }

    @Test
    void luaInstructionBudgetStopsAnInfiniteLoop() {
        assertInstructionBudgetStopsLoop(
                ExtensionManifest.Engine.LUA,
                """
                        function boot(api)
                          while true do
                          end
                        end
                        """
        );
    }

    @Test
    void rhinoInstructionBudgetStopsAnInfiniteLoop() {
        assertInstructionBudgetStopsLoop(
                ExtensionManifest.Engine.JAVASCRIPT,
                "function boot(api) { while (true) {} }"
        );
    }

    @Test
    void rhinoTimeBudgetStopsAnInfiniteLoopWithoutInterruptingForCorrectness() {
        assertTimeoutPreemptively(ENGINE_TEST_CEILING, () -> {
            ScriptExtensionEngine engine = new RhinoScriptEngine(
                    manifest(ExtensionManifest.Engine.JAVASCRIPT, Set.of()),
                    "function boot(api) { while (true) {} }"
            );
            try {
                ScriptExecutionBudget budget = ScriptExecutionBudget.enter(1L, Long.MAX_VALUE);
                try {
                    assertThrows(
                            ScriptExecutionBudget.ScriptBudgetExceededException.class,
                            () -> engine.initialize(new RestrictedBridge())
                    );
                } finally {
                    budget.close();
                }
            } finally {
                engine.close();
            }
        });
    }

    @Test
    void rejectedCapabilityCallsEscapeBothLanguageAdapters() throws Exception {
        assertCapabilityRejection(
                new LuaScriptEngine(
                        manifest(ExtensionManifest.Engine.LUA, Set.of()),
                        """
                                function boot(api)
                                  api.catalog.list()
                                end
                                """
                )
        );
        assertCapabilityRejection(
                new RhinoScriptEngine(
                        manifest(ExtensionManifest.Engine.JAVASCRIPT, Set.of()),
                        "function boot(api) { api.catalog.list(); }"
                )
        );
    }

    private static void assertInstructionBudgetStopsLoop(ExtensionManifest.Engine engine, String source) {
        assertTimeoutPreemptively(ENGINE_TEST_CEILING, () -> {
            ScriptExtensionEngine scriptEngine = switch (engine) {
                case LUA -> new LuaScriptEngine(manifest(engine, Set.of()), source);
                case JAVASCRIPT -> new RhinoScriptEngine(manifest(engine, Set.of()), source);
            };
            try {
                ScriptExecutionBudget budget = ScriptExecutionBudget.enter(10_000L, 256L);
                try {
                    assertThrows(
                            ScriptExecutionBudget.ScriptBudgetExceededException.class,
                            () -> scriptEngine.initialize(new RestrictedBridge())
                    );
                } finally {
                    budget.close();
                }
            } finally {
                scriptEngine.close();
            }
        });
    }

    private static void assertCapabilityRejection(ScriptExtensionEngine engine) throws Exception {
        try {
            assertThrows(ScriptSandboxViolation.class, () -> engine.initialize(new RestrictedBridge()));
        } finally {
            engine.close();
        }
    }

    private static ExtensionManifest manifest(
            ExtensionManifest.Engine engine,
            Set<ExtensionManifest.Capability> permissions
    ) {
        return new ExtensionManifest(
                "sandbox-test",
                "Sandbox test",
                "1.0.0",
                engine,
                "boot",
                List.of(),
                permissions
        );
    }

    private static final class RestrictedBridge implements ExtensionBridge {
        private final List<String> logMessages = new ArrayList<>();

        List<String> logs() {
            return List.copyOf(logMessages);
        }

        @Override
        public void log(String message) {
            logMessages.add(message);
        }

        @Override
        public ExtensionDisposer onEvent(String eventType, ExtensionCallback callback) {
            throw unexpected("events");
        }

        @Override
        public ExtensionDisposer registerCommand(String command, ExtensionCallback callback) {
            throw unexpected("commands");
        }

        @Override
        public ExtensionDisposer schedule(long delayTicks, boolean repeating, ExtensionCallback callback) {
            throw unexpected("schedule");
        }

        @Override
        public String getKv(String key) {
            throw unexpected("kv.get");
        }

        @Override
        public void putKv(String key, String value) {
            throw unexpected("kv.set");
        }

        @Override
        public void removeKv(String key) {
            throw unexpected("kv.remove");
        }

        @Override
        public List<String> catalogIds() {
            throw new ScriptSandboxViolation();
        }

        private static AssertionError unexpected(String operation) {
            return new AssertionError("Unexpected extension bridge call: " + operation);
        }
    }
}
