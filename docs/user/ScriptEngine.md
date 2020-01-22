# GraalVM JavaScript ScriptEngine Implementation

GraalVM JavaScript provides a JSR-223 compliant `javax.script.ScriptEngine` implementation.
Note that this is feature is provided for legacy reasons in order to allow easier migration for implementations currently based on a `ScriptEngine`.
We strongly encourage users to use the `org.graalvm.polyglot.Context` interface in order to control many of the settings directly.

Since the  `ScriptEngine` interface does not provide a way to set options, `GraalJSScriptEngine` supports setting some `Context` options
through `Bindings`. These options are:
* `polyglot.js.allowHostAccess <boolean>`
* `polyglot.js.allowNativeAccess <boolean>`
* `polyglot.js.allowCreateThread <boolean>`
* `polyglot.js.allowIO <boolean>`
* `polyglot.js.allowHostClassLookup <boolean or Predicate<String>>`
* `polyglot.js.allowHostClassLoading <boolean>`
* `polyglot.js.allowAllAccess <boolean>`
Note that using the ScriptEngine implies allowing experimental options.

These options control the sandboxing rules applied to evaluated JavaScript code and are set to `false` by default, unless the application was
started in Nashorn compatibility mode (`--js.nashorn-compat=true`).

To set an option via `Bindings`, use `Bindings.put(<option name>, true)` **before** the engine's script context is initialized. Note that
even a call to `Bindings#get(String)` may lead to context initialization.
The following code shows how to enable `polyglot.js.allowHostAccess` via `Bindings`:
```
ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
bindings.put("polyglot.js.allowHostAccess", true);
bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
bindings.put("javaObj", new Object());
engine.eval("(javaObj instanceof Java.type('java.lang.Object'));"); // would not work without allowHostAccess and allowHostClassLookup
```
This example would not work if the user would call e.g. `engine.eval("var x = 1;")` before calling `bindings.put("polyglot.js.allowHostAccess", true);`, since
any call to `eval` forces context initialization.

`Context` options can also be passed to `GraalJSScriptEngine` directly, via an instance of `Context.Builder`:
```
ScriptEngine engine = GraalJSScriptEngine.create(null,
        Context.newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup(s -> true));
engine.put("javaObj", new Object());
engine.eval("(javaObj instanceof Java.type('java.lang.Object'));");
```
