---
layout: docs
toc_group: js
link_title: Options
permalink: /reference-manual/js/Options/
---
# Options

Running JavaScript on GraalVM can be configured with several options.

## GraalVM JavaScript Launcher Options

These options are to control the behaviour of the `js` launcher:
* `-e, --eval CODE `: evaluate the JavaScript source code, then exit the engine.
```shell
js -e 'print(1+2);'
```
* `-f, --file FILE`: load and execute the provided script file. Note that the `-f` flag is optional and can be omitted in most cases, as any additional argument to `js` will be interpreted as file anyway.
```shell
js -f myfile.js
```
* `--module FILE`: load and execute the provided module file. Note that `.mjs` files are treated as modules by default.
```shell
js --module myfile.mjs
```
* `--version`: print the version information of GraalVM JavaScript, then exit.
* `--strict`: execute the engine in JavaScript's _strict mode_.

##  GraalVM JavaScript Engine Options

There are several options to configure the behavior of the GraalVM JavaScript engine.
Depending on how the engine is started, the options can be passed either to the launcher or programmatically.

For a full list of options of the JavaScript engine, pass the `--help:js` flag to the `js` launcher (available from GraalVM 22.1, for older releases use `--help:languages`).
To include internal options, use `--help:js:internal`.
Note that those lists both include stable, supported options and experimental options.

### Provide Options to the Launcher
To the launcher, the options are passed with `--js.<option-name>=<value>`:
```shell
js --js.ecmascript-version=2015
```

### Provide Options Programmatically Using the Context API
When started from Java using GraalVM's Polyglot API, the options are passed programmatically to the `Context` object:
```java
Context context = Context.newBuilder("js")
                         .option("js.ecmascript-version", "2015")
                         .build();
context.eval("js", "42");
```

See the [Polyglot Programming](https://github.com/oracle/graal/blob/master/docs/reference-manual/polyglot-programming.md#passing-options-programmatically) reference for information on how to set options programmatically.

### Stable and Experimental Options

The available options are distinguished in stable and experimental options.
If an experimental option is used, an extra flag has to be provided upfront.

In the native launchers (`js` and `node`), `--experimental-options` has to be passed before all experimental options.
When using a `Context`, the option `allowExperimentalOptions(true)` has to be called on the `Context.Builder`.
See [ScriptEngine Implementation](ScriptEngine.md) on how to use experimental options with a `ScriptEngine`.

### Frequently Used Stable Options

The following stable options are frequently relevant:
   * `--js.ecmascript-version`: emulate a specific ECMAScript version. Integer value (`5`, `6`, etc., `2015`-`2022`), `"latest"` (latest supported version of the spec, including finished proposals), or `"staging"` (latest version including supported unfinished proposals), default is `"latest"`.
   * `--js.foreign-object-prototype`: provide JavaScript's default prototype to foreign objects that mimic JavaScript's own types (foreign Arrays, Objects and Functions). Boolean value, default is `true`.
   * `--js.intl-402`: enable ECMAScript Internationalization API. Boolean value, default is `true`.
   * `--js.regexp-static-result`: provide static `RegExp` properties containing the results of the last successful match, e.g., `RegExp.$1` (legacy). Boolean value, default is `true`.
   * `--js.strict`: enable strict mode for all scripts. Boolean value, default is `false`.
   * `--js.console`: enable the `console` global property. Boolean value, default is `true`.
   * `--js.allow-eval`: allow the code generation from strings, e.g. using `eval()` or the `Function` constructor. Boolean value, default is `true`.
   * `--js.timer-resolution`: sets the resolution of timing functions, like `Date.now()` and `performance.now()`, in nanoseconds. Default: `1000000` (i.e. 1 ms).
   * `--js.unhandled-rejections`: configure unhandled promise rejection tracking. Accepted values are `none` (default, no tracking), `warn` (print a warning to stderr), `throw` (throw an exception), and `handler` (invoke a custom handler).
   * `--js.esm-eval-returns-exports`: `context.eval` of an ES module `Source` returns its exported symbols.

For a complete list, use `js --help:js:internal`

#### ECMAScript Version

This option provides compatibility to a specific version of the ECMAScript specification.
It expects an integer value, where both the edition numbers (`5`, `6`, ...) and the publication years (starting from `2015`) are supported.
As of GraalVM 21.2, `latest`, `staging` are supported, too.
The default in GraalVM 23.1 is the [`ECMAScript 2023 specification`](https://262.ecma-international.org/14.0/).
GraalVM JavaScript implements some features of the future draft specification and of open proposals, if you explicitly select that version and/or enable specific experimental flags.
For production settings, it is recommended to set the `ecmascript-version` to a released, finalized version of the specification (e.g., `2022`).

Available versions are:
* `5` for ECMAScript 5.x
* `2015` (or `6`) for ECMAScript 2015
* `2016` (or `7`) for ECMAScript 2016
* `2017` (or `8`) for ECMAScript 2017
* `2018` (or `9`) for ECMAScript 2018
* `2019` (or `10`) for ECMAScript 2019
* `2020` (or `11`) for ECMAScript 2020
* `2021` (or `12`) for ECMAScript 2021 (**default** in 21.3)
* `2022` (or `13`) for ECMAScript 2022 (**default** in 22.0+)
* `2023` (or `14`) for [ECMAScript 2023](https://262.ecma-international.org/14.0/) (**default** in 23.1)
* `latest` for the latest supported language version (the default version)
* `staging` for the latest supported language features including experimental unstable, unfinished [proposals](https://github.com/tc39/proposals) (_do not use in production!_)

#### intl-402

This option enables ECMAScript's [Internationalization API](https://tc39.github.io/ecma402/).
It expects a Boolean value and the default is `true`.

#### Strict Mode

This option enables JavaScript's strict mode for all scripts.
It expects a Boolean value and the default is `false`.

### Frequently Used Experimental Options
Note that these options are experimental and are not guaranteed to be maintained or supported in the future.
To use them, the `--experimental-options` flag is required or the experimental options have to be enabled on the Context, see above.

   * `--js.nashorn-compat`: provide compatibility mode with the Nashorn engine. Sets ECMAScript version to 5 by default. Might conflict with newer ECMAScript versions. Boolean value, default is `false`.
   * `--js.timezone`: set the local time zone. String value, default is the system default.
   * `--js.v8-compat`: provide better compatibility with Google's V8 engine. Boolean value, default is `false`.
   * `--js.temporal`: enable [`Temporal` API](https://github.com/tc39/proposal-temporal).
   * `--js.webassembly`: enable `WebAssembly` API.
