# ScriptEngine Implementation

GraalVM provides a JSR-223 compliant `javax.script.ScriptEngine` implementation for running JavaScript.
Note that this feature is provided for legacy reasons in order to allow easier migration for implementations currently based on a `ScriptEngine`.
We strongly encourage users to use the `org.graalvm.polyglot.Context` interface in order to control many of the settings directly and benefit from finer-grained security settings in GraalVM.

## Setting Options via `Bindings`
The  `ScriptEngine` interface does not provide a default way to set options.
As a workaround, `GraalJSScriptEngine` supports setting some `Context` options
through `Bindings`.
These options are:
* `polyglot.js.allowHostAccess <boolean>`
* `polyglot.js.allowNativeAccess <boolean>`
* `polyglot.js.allowCreateThread <boolean>`
* `polyglot.js.allowIO <boolean>`
* `polyglot.js.allowHostClassLookup <boolean or Predicate<String>>`
* `polyglot.js.allowHostClassLoading <boolean>`
* `polyglot.js.allowAllAccess <boolean>`

These options control the sandboxing rules applied to evaluated JavaScript code and are set to `false` by default, unless the application was started in the Nashorn compatibility mode (`--js.nashorn-compat=true`).

Note that using `ScriptEngine` implies allowing experimental options.
This is an exhaustive list of allowed options to be passed via Bindings; in case you need to pass additional options to GraalVM JavaScript, you need to manually create a `Context` as shown below.

To set an option via `Bindings`, use `Bindings.put(<option name>, true)` **before** the engine's script context is initialized. Note that
even a call to `Bindings#get(String)` may lead to context initialization.
The following code shows how to enable `polyglot.js.allowHostAccess` via `Bindings`:
```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
bindings.put("polyglot.js.allowHostAccess", true);
bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
bindings.put("javaObj", new Object());
engine.eval("(javaObj instanceof Java.type('java.lang.Object'));"); // it will not work without allowHostAccess and allowHostClassLookup
```
This example will not work if the user calls, e.g., `engine.eval("var x = 1;")`, before calling `bindings.put("polyglot.js.allowHostAccess", true);`, since
any call to `eval` forces context initialization.

## Setting Options via System Properties
Options to the JavaScript engine can be set via system properties before starting the JVM by prepending `polyglot.`:
```java
java -Dpolyglot.js.ecmascript-version=2020 MyApplication
```

Or, options to the JavaScript engine can be set programmatically from within Java before creating `ScriptEngine`. This, however, only works for the options passed to the JavaScript engine (like `js.ecmascript`), not for the six options mentioned above that can be set via the `Bindings`.
Another caveat is that those system properties are shared by all concurrently executed `ScriptEngine`s.

## Manually Creating `Context` for More Flexibility
`Context` options can also be passed to `GraalJSScriptEngine` directly, via an instance of `Context.Builder`:
```java
ScriptEngine engine = GraalJSScriptEngine.create(null,
        Context.newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup(s -> true)
        .option("js.ecmascript-version", "2020"));
engine.put("javaObj", new Object());
engine.eval("(javaObj instanceof Java.type('java.lang.Object'));");
```

This allows setting all options available in GraalVM JavaScript.
It does come at the cost of a hard dependency on GraalVM JavaScript, e.g., the `GraalJSScriptEngine` and `Context` classes.

## Supported File Extensions
The GraalVM JavaScript implementation of `javax.script.ScriptEngine` supports the `js` file extension for JavaScript source files, as well as the `mjs` extension for ES modules.
