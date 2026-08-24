package pubsher.talexsoultech.extensions;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.StringLib;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** LuaJ adapter built from explicit core libraries; JSE platform libraries are never loaded. */
final class LuaScriptEngine implements ScriptExtensionEngine {
    private final ExtensionManifest manifest;
    private final String source;
    private final Globals globals;

    LuaScriptEngine(ExtensionManifest manifest, String source) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.source = Objects.requireNonNull(source, "source");
        this.globals = safeGlobals();
    }

    @Override
    public void initialize(ExtensionBridge bridge) {
        LuaValue chunk = globals.load(source, "extension-" + manifest.id());
        chunk.call();
        LuaValue entry = resolveEntry(globals, manifest.entry());
        if (!entry.isfunction()) {
            throw new IllegalArgumentException("Extension entry is not a function");
        }
        entry.call(api(bridge));
    }

    @Override
    public void close() {
        globals.set("_G", LuaValue.NIL);
    }

    private static Globals safeGlobals() {
        Globals globals = new Globals();
        LuaTable privatePackage = new LuaTable();
        privatePackage.set("loaded", new LuaTable());
        globals.set("package", privatePackage);
        globals.load(new BaseLib());
        globals.load(new StringLib());
        globals.load(new TableLib());
        globals.load(new MathLib());
        freezeLibrary(globals, "string");
        freezeLibrary(globals, "table");
        freezeLibrary(globals, "math");
        LoadState.install(globals);
        LuaC.install(globals);

        // DebugLib is private host instrumentation: its global table is removed before user source runs.
        globals.load(new BudgetedDebugLib());
        globals.set("debug", LuaValue.NIL);
        privatePackage.get("loaded").set("debug", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("coroutine", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("loadstring", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        globals.set("module", LuaValue.NIL);
        globals.set("print", LuaValue.NIL);
        globals.set("collectgarbage", LuaValue.NIL);
        globals.set("setmetatable", LuaValue.NIL);
        globals.set("getmetatable", LuaValue.NIL);
        globals.set("rawset", LuaValue.NIL);
        return globals;
    }


    private static void freezeLibrary(Globals globals, String name) {
        LuaTable original = globals.get(name).checktable();
        LuaTable view = new LuaTable();
        LuaTable metatable = new LuaTable();
        metatable.set("__index", original);
        metatable.set("__newindex", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs arguments) {
                throw new ScriptSandboxViolation();
            }
        });
        metatable.set("__metatable", LuaValue.FALSE);
        view.setmetatable(metatable);
        globals.set(name, view);
    }

    private static LuaValue resolveEntry(LuaValue root, String entry) {
        LuaValue current = root;
        for (String part : entry.split("\\.")) {
            current = current.get(part);
            if (current.isnil()) {
                return LuaValue.NIL;
            }
        }
        return current;
    }

    private LuaTable api(ExtensionBridge bridge) {
        LuaTable api = new LuaTable();
        api.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue argument) {
                bridge.log(argument.checkjstring());
                return LuaValue.NIL;
            }
        });

        LuaTable events = new LuaTable();
        events.set("on", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue eventType, LuaValue callback) {
                return disposer(bridge.onEvent(eventType.checkjstring(), callback(callback)));
            }
        });
        api.set("events", events);

        LuaTable commands = new LuaTable();
        commands.set("register", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue command, LuaValue callback) {
                return disposer(bridge.registerCommand(command.checkjstring(), callback(callback)));
            }
        });
        api.set("commands", commands);

        LuaTable schedule = new LuaTable();
        schedule.set("after", scheduleFunction(bridge, false));
        schedule.set("every", scheduleFunction(bridge, true));
        api.set("schedule", schedule);

        LuaTable kv = new LuaTable();
        kv.set("get", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                String value = bridge.getKv(key.checkjstring());
                return value == null ? LuaValue.NIL : LuaValue.valueOf(value);
            }
        });
        kv.set("set", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue key, LuaValue value) {
                bridge.putKv(key.checkjstring(), value.checkjstring());
                return LuaValue.NIL;
            }
        });
        kv.set("remove", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                bridge.removeKv(key.checkjstring());
                return LuaValue.NIL;
            }
        });
        api.set("kv", kv);

        LuaTable catalog = new LuaTable();
        catalog.set("list", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return stringArray(bridge.catalogIds());
            }
        });
        api.set("catalog", catalog);
        return api;
    }

    private VarArgFunction scheduleFunction(ExtensionBridge bridge, boolean repeating) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs arguments) {
                long ticks = arguments.checklong(1);
                LuaValue callback = arguments.arg(2);
                return disposer(bridge.schedule(ticks, repeating, callback(callback)));
            }
        };
    }

    private ExtensionCallback callback(LuaValue value) {
        LuaValue function = value.checkfunction();
        return invocation -> {
            LuaValue result = function.call(invocationValue(invocation));
            return result.isstring() ? result.tojstring() : null;
        };
    }

    private static LuaTable invocationValue(ExtensionInvocation invocation) {
        LuaTable table = new LuaTable();
        table.set("type", LuaValue.valueOf(invocation.type()));
        for (Map.Entry<String, Object> entry : invocation.data().entrySet()) {
            table.set(entry.getKey(), luaValue(entry.getValue()));
        }
        return table;
    }

    private static LuaValue luaValue(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof String text) {
            return LuaValue.valueOf(text);
        }
        if (value instanceof Boolean bool) {
            return LuaValue.valueOf(bool);
        }
        if (value instanceof Number number) {
            return LuaValue.valueOf(number.doubleValue());
        }
        if (value instanceof List<?> values) {
            LuaTable table = new LuaTable();
            for (int index = 0; index < values.size(); index++) {
                table.set(index + 1, luaValue(values.get(index)));
            }
            return table;
        }
        throw new IllegalArgumentException("Unsupported extension DTO value");
    }

    private static LuaValue disposer(ExtensionDisposer disposer) {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                disposer.close();
                return LuaValue.NIL;
            }
        };
    }

    private static LuaTable stringArray(List<String> values) {
        LuaTable table = new LuaTable();
        for (int index = 0; index < values.size(); index++) {
            table.set(index + 1, LuaValue.valueOf(values.get(index)));
        }
        return table;
    }

    private static final class BudgetedDebugLib extends DebugLib {
        @Override
        public void onInstruction(int pc, Varargs values, int top) {
            ScriptExecutionBudget.observe(1L);
            super.onInstruction(pc, values, top);
        }
    }
}
