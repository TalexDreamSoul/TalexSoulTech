package pubsher.talexsoultech.extensions;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Isolated Rhino scope with no Java package, class, reflection, or host-object surface. */
final class RhinoScriptEngine implements ScriptExtensionEngine {
    private final ExtensionManifest manifest;
    private final String source;
    private final SandboxedContextFactory contextFactory = new SandboxedContextFactory();
    private Scriptable scope;

    RhinoScriptEngine(ExtensionManifest manifest, String source) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void initialize(ExtensionBridge bridge) {
        contextFactory.call(context -> {
            scope = context.initSafeStandardObjects();
            removeDynamicAndJavaBridges(scope);
            context.evaluateString(scope, source, "extension-" + manifest.id(), 1, null);
            Object entry = resolveEntry(scope, manifest.entry());
            if (!(entry instanceof Function function)) {
                throw new IllegalArgumentException("Extension entry is not a function");
            }
            function.call(context, scope, scope, new Object[]{api(scope, bridge)});
            return null;
        });
    }

    @Override
    public void close() {
        scope = null;
    }

    private static void removeDynamicAndJavaBridges(Scriptable scope) {
        for (String name : List.of(
                "Packages", "java", "javax", "org", "com", "edu", "net",
                "JavaAdapter", "JavaImporter", "importClass", "importPackage", "getClass",
                "eval", "Function"
        )) {
            ScriptableObject.deleteProperty(scope, name);
        }
    }

    private static Object resolveEntry(Scriptable root, String entry) {
        Scriptable currentScope = root;
        Object current = ScriptableObject.NOT_FOUND;
        String[] parts = entry.split("\\.");
        for (int index = 0; index < parts.length; index++) {
            current = ScriptableObject.getProperty(currentScope, parts[index]);
            if (current == ScriptableObject.NOT_FOUND || current == Undefined.instance) {
                return ScriptableObject.NOT_FOUND;
            }
            if (index + 1 < parts.length) {
                if (!(current instanceof Scriptable nested)) {
                    return ScriptableObject.NOT_FOUND;
                }
                currentScope = nested;
            }
        }
        return current;
    }

    private ExtensionCallback callback(Function function) {
        return invocation -> contextFactory.call(context -> {
            Scriptable payload = invocationValue(scope, invocation);
            Object result = function.call(context, scope, scope, new Object[]{payload});
            return result instanceof CharSequence text ? text.toString() : null;
        });
    }

    private BaseFunction scheduleFunction(Scriptable parent, ExtensionBridge bridge, boolean repeating) {
        return new HostFunction(parent) {
            @Override
            public Object call(Context context, Scriptable callScope, Scriptable thisObject, Object[] arguments) {
                long ticks = requireTicks(argument(arguments, 0));
                Function callback = requireFunction(argument(arguments, 1));
                return disposer(parent, bridge.schedule(ticks, repeating, RhinoScriptEngine.this.callback(callback)));
            }
        };
    }

    private BaseFunction function(Scriptable parent, HostCall call) {
        return new HostFunction(parent) {
            @Override
            public Object call(Context context, Scriptable callScope, Scriptable thisObject, Object[] arguments) {
                return call.apply(arguments);
            }
        };
    }

    private Scriptable api(Scriptable parent, ExtensionBridge bridge) {
        NativeObject api = object(parent);
        ScriptableObject.putProperty(api, "log", function(parent, arguments -> {
            bridge.log(requireString(argument(arguments, 0)));
            return Undefined.instance;
        }));

        NativeObject events = object(parent);
        ScriptableObject.putProperty(events, "on", function(parent, arguments -> disposer(
                parent,
                bridge.onEvent(
                        requireString(argument(arguments, 0)),
                        callback(requireFunction(argument(arguments, 1)))
                )
        )));
        ScriptableObject.putProperty(api, "events", events);

        NativeObject commands = object(parent);
        ScriptableObject.putProperty(commands, "register", function(parent, arguments -> disposer(
                parent,
                bridge.registerCommand(
                        requireString(argument(arguments, 0)),
                        callback(requireFunction(argument(arguments, 1)))
                )
        )));
        ScriptableObject.putProperty(api, "commands", commands);

        NativeObject schedule = object(parent);
        ScriptableObject.putProperty(schedule, "after", scheduleFunction(parent, bridge, false));
        ScriptableObject.putProperty(schedule, "every", scheduleFunction(parent, bridge, true));
        ScriptableObject.putProperty(api, "schedule", schedule);

        NativeObject kv = object(parent);
        ScriptableObject.putProperty(kv, "get", function(parent, arguments -> {
            String value = bridge.getKv(requireString(argument(arguments, 0)));
            return value == null ? Undefined.instance : value;
        }));
        ScriptableObject.putProperty(kv, "set", function(parent, arguments -> {
            bridge.putKv(requireString(argument(arguments, 0)), requireString(argument(arguments, 1)));
            return Undefined.instance;
        }));
        ScriptableObject.putProperty(kv, "remove", function(parent, arguments -> {
            bridge.removeKv(requireString(argument(arguments, 0)));
            return Undefined.instance;
        }));
        ScriptableObject.putProperty(api, "kv", kv);

        NativeObject catalog = object(parent);
        ScriptableObject.putProperty(catalog, "list", function(parent, arguments -> stringArray(parent, bridge.catalogIds())));
        ScriptableObject.putProperty(api, "catalog", catalog);
        return api;
    }

    private static Object argument(Object[] arguments, int index) {
        if (index >= arguments.length || arguments[index] == Undefined.instance) {
            throw new IllegalArgumentException("Missing extension API argument");
        }
        return arguments[index];
    }

    private static String requireString(Object value) {
        if (!(value instanceof CharSequence sequence)) {
            throw new IllegalArgumentException("Extension API string is invalid");
        }
        String text = sequence.toString();
        if (text.length() > 8_192) {
            throw new IllegalArgumentException("Extension API string is too large");
        }
        return text;
    }

    private static long requireTicks(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Extension schedule delay is invalid");
        }
        double numeric = number.doubleValue();
        if (!Double.isFinite(numeric) || numeric != Math.rint(numeric) || numeric < 1D || numeric > 72_000D) {
            throw new IllegalArgumentException("Extension schedule delay is invalid");
        }
        return (long) numeric;
    }

    private static Function requireFunction(Object value) {
        if (!(value instanceof Function function)) {
            throw new IllegalArgumentException("Extension callback is invalid");
        }
        return function;
    }

    private static NativeObject object(Scriptable parent) {
        NativeObject object = new NativeObject();
        object.setParentScope(parent);
        object.setPrototype(ScriptableObject.getObjectPrototype(parent));
        return object;
    }

    private static NativeArray stringArray(Scriptable parent, List<String> values) {
        NativeArray array = new NativeArray(values.toArray(String[]::new));
        array.setParentScope(parent);
        array.setPrototype(ScriptableObject.getClassPrototype(parent, "Array"));
        return array;
    }

    private static Scriptable invocationValue(Scriptable parent, ExtensionInvocation invocation) {
        NativeObject payload = object(parent);
        ScriptableObject.putProperty(payload, "type", invocation.type());
        for (Map.Entry<String, Object> entry : invocation.data().entrySet()) {
            ScriptableObject.putProperty(payload, entry.getKey(), javascriptValue(parent, entry.getValue()));
        }
        return payload;
    }

    private static Object javascriptValue(Scriptable parent, Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        if (value instanceof List<?> values) {
            Object[] copied = values.toArray();
            NativeArray array = new NativeArray(copied);
            array.setParentScope(parent);
            array.setPrototype(ScriptableObject.getClassPrototype(parent, "Array"));
            return array;
        }
        throw new IllegalArgumentException("Unsupported extension DTO value");
    }

    private static BaseFunction disposer(Scriptable parent, ExtensionDisposer disposer) {
        return new HostFunction(parent) {
            @Override
            public Object call(Context context, Scriptable callScope, Scriptable thisObject, Object[] arguments) {
                disposer.close();
                return Undefined.instance;
            }
        };
    }

    private abstract static class HostFunction extends BaseFunction {
        private HostFunction(Scriptable parent) {
            setParentScope(parent);
            setPrototype(ScriptableObject.getFunctionPrototype(parent));
        }
    }

    @FunctionalInterface
    private interface HostCall {
        Object apply(Object[] arguments);
    }

    private static final class SandboxedContextFactory extends ContextFactory {
        @Override
        protected Context makeContext() {
            Context context = super.makeContext();
            context.setInterpretedMode(true);
            context.setMaximumInterpreterStackDepth(256);
            context.setInstructionObserverThreshold(1_000);
            context.setClassShutter(new DenyAllClassShutter());
            DenyJavaWrapFactory wrapFactory = new DenyJavaWrapFactory();
            wrapFactory.setJavaPrimitiveWrap(false);
            context.setWrapFactory(wrapFactory);
            return context;
        }

        @Override
        protected void observeInstructionCount(Context context, int instructionCount) {
            ScriptExecutionBudget.observe(instructionCount);
        }
    }

    private static final class DenyAllClassShutter implements ClassShutter {
        @Override
        public boolean visibleToScripts(String fullClassName) {
            return false;
        }
    }

    private static final class DenyJavaWrapFactory extends WrapFactory {
        @Override
        public Object wrap(Context context, Scriptable scope, Object value, Class<?> staticType) {
            if (value == null || value == Undefined.instance || value instanceof Scriptable
                    || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
                return super.wrap(context, scope, value, staticType);
            }
            throw new ScriptSandboxViolation();
        }

        @Override
        public Scriptable wrapNewObject(Context context, Scriptable scope, Object value) {
            if (value instanceof Scriptable scriptable) {
                return scriptable;
            }
            throw new ScriptSandboxViolation();
        }

        @Override
        public Scriptable wrapAsJavaObject(Context context, Scriptable scope, Object value, Class<?> staticType) {
            throw new ScriptSandboxViolation();
        }

        @Override
        public Scriptable wrapJavaClass(Context context, Scriptable scope, Class<?> javaClass) {
            throw new ScriptSandboxViolation();
        }
    }
}
