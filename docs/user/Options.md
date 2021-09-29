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
* `--version`: print the version information of GraalVM JavaScript, then exit.
* `--strict`: execute the engine in JavaScript's _strict mode_.

##  GraalVM JavaScript Engine Options

These options are to configure the behavior of the GraalVM JavaScript engine.
Depending on how the engine is started, the options can be passed either to the launcher or programmatically.

Note that most of these options are experimental and require an `--experimental-options` flag.

### To the Launcher
To the launcher, the options are passed with `--js.<option-name>=<value>`:
```shell
js --js.ecmascript-version=6
```

The following options are currently available:
   * `--js.annex-b`: enable ECMAScript Annex B web compatibility features. Boolean value, default is `true`.
   * `--js.array-sort-inherited`: define whether `Array.protoype.sort` should sort inherited keys (implementation-defined behavior). Boolean value, default is `true`.
   * `--js.atomics`: enable *ES2017 Atomics*. Boolean value, default is `true`.
   * `--js.ecmascript-version`: emulate a specific ECMAScript version. Integer value (`5`-`13`, or `2015`-`2022`), default is the latest stable version.
   * `--js.foreign-object-prototype`: provide JavaScript's default prototype to foreign objects that mimic JavaScript's own types (foreign Arrays, Objects and Functions). Boolean value, default is `false`.
   * `--js.intl-402`: enable ECMAScript Internationalization API. Boolean value, default is `false`.
   * `--js.regexp-static-result`: provide static `RegExp` properties containing the results of the last successful match, e.g., `RegExp.$1` (legacy). Boolean value, default is `true`.
   * `--js.shared-array-buffer`: enable *ES2017 SharedArrayBuffer*. Boolean value, default is `false`.
   * `--js.strict`: enable strict mode for all scripts. Boolean value, default is `false`.
   * `--js.timezone`: set the local time zone. String value, default is the system default.
   * `--js.v8-compat`: provide better compatibility with Google's V8 engine. Boolean value, default is `false`.

### Programmatically
When started from Java via GraalVM's Polyglot feature, the options are passed programmatically to the `Context` object:
```java
Context context = Context.newBuilder("js")
                         .option("js.ecmascript-version", "6")
                         .build();
context.eval("js", "42");
```

See the [Polyglot Programming](../polyglot-programming.md/#passing-options-programmatically) reference for information on how to set options programmatically.

## Stable and Experimental Options

The available options are distinguished in stable and experimental options.
If an experimental option is used, an extra flag has to be provided upfront.

In the native launchers (`js` and `node`), `--experimental-options` has to be passed before all experimental options.
When using a `Context`, the option `allowExperimentalOptions(true)` has to be called on the `Context.Builder`.
See [ScriptEngine Implementation](ScriptEngine.md) on how to use experimental options with a `ScriptEngine`.

## ECMAScript Version

This option provides compatibility to a specific version of the ECMAScript specification.
It expects an integer value, where both the counting version numbers (`5` to `11`) and the publication years (starting from `2015`) are supported.
The default in the development version of GraalVM is the [`ECMAScript 2021 specification`](https://tc39.es/ecma262/).
GraalVM JavaScript implements some features of the future draft specification and of open proposals, if you explicitly select that version and/or enable specific experimental flags.
For production settings, it is recommended to set the `ecmascript-version` to an existing, finalized version of the specification.

Available versions are:
* `5` for ECMAScript 5.x
* `6` or `2015` for ECMAScript 2015
* `7` or `2016` for ECMAScript 2016
* `8` or `2017` for ECMAScript 2017
* `9` or `2018` for ECMAScript 2018
* `10` or `2019` for ECMAScript 2019
* `11` or `2020` for ECMAScript 2020
* `12` or `2021` for ECMAScript 2021 (**default**, latest finalized version of the specification)
* `13` or `2022` for ECMAScript 2022 (future changes and proposals)

As of GraalVM 21.2, the flag can also be set to `latest` or `staging`, to use the latest stable version (which is the default), or the staging version including experimental functionality under development.

## intl-402

This option enables ECMAScript's [Internationalization API](https://tc39.github.io/ecma402/).
It expects a Boolean value and the default is `false`.

## Strict Mode

This option enables JavaScript's strict mode for all scripts.
It expects a Boolean value and the default is `false`.
