# Graal JavaScript ScriptEngine implementation 

For easier migration from JSR-223-based JavaScript implementations, Graal JavaScript provides an implementation of `javax.script.ScriptEngine`.
Since the  `ScriptEngine` interface does not provide a way to set options, `GraalJSScriptEngine` supports setting some `Context` options
through `Bindings`. These options are:
* `polyglot.js.allowHostAccess`
* `polyglot.js.allowNativeAccess`
* `polyglot.js.allowCreateThread`
* `polyglot.js.allowIO`
* `polyglot.js.allowHostClassLoading`
* `polyglot.js.allowAllAccess`

These options control the sandboxing rules applied to evaluated JavaScript code and are set to `false` by default, unless the application was
started in Nashorn compatibility mode (`--js.nashorn-compat=true`).

To set an option via `Bindings`, use `Bindings.put(<option name>, true)` **before** the engine's script context is initialized. Note that
even a call to `Bindings#get(String)` may lead to context initialization. 
The following code shows how to enable `polyglot.js.allowHostAccess` via `Bindings`:
```
ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
bindings.put("polyglot.js.allowHostAccess", true);
bindings.put("javaObj", new Object());
engine.eval("(javaObj instanceof Java.type('java.lang.Object'));"); // would not work without allowHostAccess
```
This example would not work if the user would call e.g. `engine.eval("var x = 1;")` before calling `bindings.put("polyglot.js.allowHostAccess", true);`, since
any call to `eval` forces context initialization. 