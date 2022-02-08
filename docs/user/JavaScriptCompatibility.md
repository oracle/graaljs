---
layout: docs
toc_group: js
link_title: JavaScript Compatibility
permalink: /reference-manual/js/JavaScriptCompatibility/
---
# JavaScript Compatibility

GraalVM provides an ECMAScript-compliant JavaScript language runtime.
This document explains the public API it presents for user applications written in JavaScript.

* [ECMAScript Language Compliance](#ecmascript-language-compliance)
* [Compatibility Extensions](#compatibility-extensions)
* [GraalVM JavaScript Extensions](#graalvm-javascript-extensions)

## ECMAScript Language Compliance

GraalVM JavaScript implements JavaScript as prescribed in the ECMAScript (ECMA-262) specification.
It is fully compatible with the [ECMAScript 2021 specification](https://262.ecma-international.org/12.0/) (sometimes referred to as the 12th edition or "ES12").
Starting with GraalVM 22.0.0, all available features of the [ECMAScript 2022 draft specification](https://tc39.es/ecma262/) are enabled by default.
New features are frequently added to GraalVM when they are confirmed to be part of ECMAScript 2022, see the [CHANGELOG.md](https://github.com/oracle/graaljs/blob/master/CHANGELOG.md) for details.
Older versions starting from ECMAScript 5 can be enabled with a config flag (by number: `--js.ecmascript-version=5` or by year: `--js.ecmascript-version=2019`).
In a production setup you might consider specifying a fixed ECMAScript version to be used, as future versions of GraalVM JavaScript will use newer versions of the specification once available.

GraalVM JavaScript provides the following function objects in the global scope as specified by ECMAScript, representing the JavaScript core library:
Array, ArrayBuffer, Boolean, DataView, Date, Error, Function, JSON, Map, Math, Number, Object, Promise, Proxy, Reflect, RegExp, Set, SharedArrayBuffer, String, Symbol, TypedArray, WeakMap, and WeakSet.

Additional objects are available under flags, for instance `Intl` (flag: `--js.intl-402`).
Run `js --help` or `js --help:languages` for the list of available flags.

Several of these function objects and some of their members are only available when a certain version of the specification is selected for execution.
For a list of methods provided, inspect the ECMAScript specification.
Extensions to the specification are specified below.

### Internationalization API (ECMA-402)

Internationalization API implementation (see [https://tc39.github.io/ecma402](https://tc39.github.io/ecma402)) can be activated using the following flag: `--js.intl-402=true`.
If you run in native mode (default option), you also need to specify the path to your ICU data directory using the following option: `--vm.Dcom.ibm.icu.impl.ICUBinary.dataPath=$GRAAL_VM_DIR/jre/languages/js/icu4j/icudt`,
where `$GRAAL_VM_DIR` refers to your GraalVM installation directory.
If you run in the JVM mode (the `--jvm` flag is used), you do not need to specify where your ICU data are located, although you can do it with the above option.

Once you activate the Internationalization API, you can use the following built-ins:

- `Intl.NumberFormat`
- `Intl.DateTimeFormat`
- `Intl.Collator`
- `Intl.PluralRules`

The functionality of a few other built-ins is then also updated according to the specification linked above.

### JavaScript Modules

GraalVM JavaScript supports modules as defined by ECMAScript 6 and later.
Be aware that the support for this feature grew and still grows over time. Be sure to use the latest ECMAScript version for the all the latest features.

When loading modules via a polyglot `Source`, you can use the inofficial `application/javascript+module` mime type to specify you are loading a module.
When loading with JavaScript code from a file, make sure the module is loaded from a file with the `.mjs` extension.
Loading with the `import` keyword is not limited by that, and can `import` from a file of any extension.

## Compatibility Extensions

The following objects and methods are available in GraalVM JavaScript for compatibility with other JavaScript execution engines.
Note that the behavior of such methods might not strictly match the semantics of those methods in all existing engines.

### Language Features

#### Conditional Catch Clauses

GraalVM JavaScript supports conditional catch clauses if the `js.syntax-extensions` option is enabled:

```js
try {
    myMethod(); // can throw
} catch (e if e instanceof TypeError) {
    print("TypeError caught");
} catch (e) {
    print("another Error caught");
}
```

### Global Properties

#### `load(source)`

- loads (parses and executes) the specified JavaScript source code

Source can be of type:

* a String: the path of the source file or a URL to execute.
* `java.lang.URL`: the URL is queried for the source code to execute if the `js.load-from-url` option is set to `true`.
* `java.io.File`: the file is read for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property, which represent the source name and code, respectively.
* all other types: the source is converted to a String.

`load` is available by default and can be deactivated by setting the `js.load` option to `false`.

#### `print(...arg)` and `printErr(...arg)`

- prints the arguments on the console (stdout and stderr, respectively)
- provides a best-effort human readable output

`print` and `printErr` are available by default and can be deactivated by setting the `js.print` option to `false`.

#### Methods of the `console` Global Object

A global `console` object is provided that offers several methods for debugging purposes.
These methods strive to provide similar functionality as provided in other engines, but do not guarantee identical results.

Note that those methods behave differently when GraalVM JavaScript is executed in Node.js mode (i.e., the `node` executable is started instead of `js`).
Node.js provides its own implementation that is used instead.

* `console.log`, `console.info`, and `console.debug`: an alias for `print(...arg)`
* `console.error`, and `console.warn`: similar to `print`, but using the error IO stream
* `console.assert(check, message)`: prints `message` when `check` is falsy
* `console.clear`: clears the console window if possible
* `console.count()`, and `console.countReset()`: counts and print how many times it has been called, or resets this counter
* `console.group`, and `console.groupEnd`: increases or decreases the indentation for succeeding outputs to the console
* `console.time()`, `console.timeLog()`, and `console.timeEnd()`: starts a timer, prints the duration the timer has been active, or prints the duration and stops the timer, respectively

The `console` object is available by default and can be deactivated by setting the option `js.console` to `false`.

### Additional Global Functions in the `js` Shell

#### `quit(status)`

- exits the engine and returns the specified status code

#### `read(file)`

- reads the content of `file`

The result is returned as a String.

The argument `file` can be of type:
* `java.io.File`: the file is used directly.
* all other types: `file` is converted to a String and interpreted as a file name.

#### `readbuffer(file)`

- reads the content of `file` similar to the `read` function

The result is returned as a JavaScript `ArrayBuffer` object.

#### `readline()`

- reads one line of input from the input stream

The result is returned as a String.

### Object

#### `Object.prototype.__defineGetter__(prop, func)`

- defines the `prop` property of `this` to be the getter function `func`

This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__defineSetter__(prop, func)`

- defines the `prop` property of `this` to be the setter function `func`

This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__lookupGetter__(prop)`

- returns the getter function for property `prop` of the object as set by `__defineGetter__`

This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__lookupSetter__(prop)`

- returns the setter function for property `prop` of the object as set by `__defineSetter__`

This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

### Nashorn Scripting Mode
GraalVM JavaScript provides a scripting mode compatible with the one provided by the Nashorn engine.
It is enabled with the `js.scripting` option. Make sure to have `--experimental-options` set:
```shell
js --experimental-options --js.scripting=true
```

In scripting mode, several properties and functions are added to the global object, including [readFully](#readfile), [readLine](#readline), `$ARG`, `$ENV`, and `$EXEC`.

There are migration guides available for code previously targeted to the [Nashorn](https://github.com/graalvm/graaljs/blob/master/docs/user/NashornMigrationGuide.md) or [Rhino](https://github.com/graalvm/graaljs/blob/master/docs/user/RhinoMigrationGuide.md) engines.

## GraalVM JavaScript Extensions

### Graal Object

The `Graal` object is provided as a property of the global object.
It provides Graal-specific information.
The existence of the property can be used to identify whether the GraalVM JavaScript engine is the current language engine:

```js
if (typeof Graal != 'undefined') {
    print(Graal.versionJS);
    print(Graal.versionGraalVM);
    print(Graal.isGraalRuntime());
}
```

The Graal object is available in GraalVM JavaScript by default, unless deactivated by an option (`js.graal-builtin=false`).

#### `Graal.versionJS`

- provides the version number of GraalVM JavaScript

#### `Graal.versionGraalVM`

- provides the version of GraalVM, if the current engine is executed on GraalVM

#### `Graal.isGraalRuntime()`

- provides whether GraalVM JavaScript is executed on a GraalVM-enabled runtime
- If `true`, hot code is compiled by the GraalVM compiler, resulting in high peak performance.
- If `false`, GraalVM JavaScript will not be optimized by the GraalVM Compiler, typically resulting in lower performance.

### Java

The `Java` object is only available when the engine is started in JVM mode (`--jvm` flag).

Note that some functions require a Nashorn compatibility mode flag to be set.
On GraalVM, this flag can be set with:
```shell
js --jvm --experimental-options --js.nashorn-compat=true
```

#### `Java.type(className)`

- loads the specified Java class and provides it as an object
- fields of this object can be read directly from it, and new instances can be created with the JavaScript ```new``` keyword:
```js
var BigDec = Java.type('java.math.BigDecimal');
var bd = new BigDec("0.1");
console.log(bd.add(bd).toString());
```

#### `Java.from(javaData)`

- creates a shallow copy of the Java datastructure (Array, List) as a JavaScript array

In many cases, this is not necessary; you can typically use the Java datastructure directly from JavaScript.

#### `Java.to(jsData, toType)`

- converts the argument to a Java dataype

The source object `jsData` is expected to be a JavaScript array, or an object with a `length` property.
The target `toType` can either be a String (e.g. `"int[]"`) or a type object (e.g., `Java.type("int[]")`).
Valid target types are Java arrays.
When no target type is provided, `Object[]` is assumed:
```js
var jsArr = ["a", "b", "c"];
var strArrType = Java.type("java.lang.String[]");
var javaArr = Java.to(jsArr, strArrType);
assertEquals('class java.lang.String[]', String(javaArr.getClass()));
```

The conversion methods as defined by ECMAScript (e.g., `ToString` and `ToDouble`) are executed when a JavaScript value has to be converted to a Java type.
Lossy conversion is disallowed and results in a TypeError.

#### `Java.isJavaObject(obj)`

- returns whether `obj` is an object of the Java language
- returns `false` for native JavaScript objects, as well as for objects of other polyglot languages

#### `Java.isType(obj)`

- returns whether `obj` is an object of the Java language, representing a Java `Class` instance
- returns `false` for all other arguments

#### `Java.typeName(obj)`

- returns the Java `Class` name of `obj`

`obj` is expected to represent a Java `Class` instance, i.e., `isType(obj)` should return true; otherwise, `undefined` is returned.

#### `Java.isJavaFunction(fn)`

- returns whether `fn` is an object of the Java language that represents a Java function
- returns `false` for all other types, including native JavaScript function, and functions of other polyglot languages

This function requires the Nashorn compatibility mode flag.

#### `Java.isScriptObject(obj)`

- returns whether `obj` is an object of the JavaScript language
- returns `false` for all other types, including objects of Java and other polyglot languages

This function requires the Nashorn compatibility mode flag.

#### `Java.isScriptFunction(fn)`

- returns whether `fn` is a JavaScript function
- returns `false` for all other types, including Java function, and functions of other polyglot languages

This function requires the Nashorn compatibility mode flag.

#### `Java.addToClasspath(location)`

- adds the specified location (file name or path name, as String) to Java's classpath

### Polyglot

The functions of the `Polyglot` object allow to interact with values from other polyglot languages.

The `Polyglot` object is available by default, unless deactivated by setting the `js.polyglot-builtin` option to `false`.

#### `Polyglot.export(key, value)`

- exports the JavaScript `value` under the name `key` (a string) to the polyglot bindings:
```js
function helloWorld() { print("Hello, JavaScript world"); }
Polyglot.export("helloJSWorld", helloWorld);
```

If the polyglot bindings already had a value identified by `key`, it is overwritten with the new value. The `value` may be any valid Polyglot value.

- throws a `TypeError` if `key` is not a String or is missing

#### `Polyglot.import(key)`

- imports the value identified by `key` (a string) from the polyglot bindings and returns it:
```js
var rubyHelloWorld = Polyglot.import("helloRubyWorld");
rubyHelloWorld();
```

If no language has exported a value identified by `key`, `undefined` is returned.

- throws a `TypeError` if `key` is not a string or missing

#### `Polyglot.eval(languageId, sourceCode)`

- parses and evaluates the `sourceCode` with the interpreter identified by `languageId`

The value of `sourceCode` is expected to be a String (or convertable to one).

- returns the evaluation result, depending on the `sourceCode` and/or the semantics of the language evaluated:
```js
var rArray = Polyglot.eval('R', 'runif(1000)');
```

Exceptions can occur when an invalid `languageId` is passed, when the `sourceCode` cannot be evaluated by the language, or when the executed program throws one.

#### `Polyglot.evalFile(languageId, sourceFileName)`

- parses the file `sourceFileName` with the interpreter identified by `languageId`

The value of `sourceFileName` is expected to be a String (or convertable to one), representing a file reachable by the current path.

- returns an executable object, typically a function:
```js
var rFunc = Polyglot.evalFile('R', 'myExample.r');
var result = rFunc();
```

Exceptions can occur when an invalid `languageId` is passed, when the file identified by `sourceFileName` cannot be found, or when the language throws an exception during parsing (parse time errors, e.g. syntax errors).
Exceptions thrown by the evaluated program are only thrown once the resulting function is evaluated.

The `Polyglot.evalFile` function is available by default when the `Polyglot` builtin is available, unless deactivated by setting the `js.polyglot-evalfile` option to `false`.
It is also available when `js.debug-builtin` is activated.

### Debug

- requires starting the engine with the `js.debug-builtin` flag

`Debug` is a GraalVM JavaScript specific function object that provides functionality for debugging JavaScript code and the GraalVM JavaScript compiler.
This API might change without notice. Do not use for production purposes.

### Global Functions

#### `printErr(...arg)`

- behaves identical to `print`

The only difference is that the error stream is used to print to, instead of the default output stream.

#### `loadWithNewGlobal(source, arguments)`

- behaves similarly to `load` function

The relevant difference is that the code is evaluated in a new global scope (Realm, as defined by ECMAScript).

Source can be of type:

* `java.lang.URL`: the URL is queried for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property.
* all other types: the source is converted to a String.

The value of `arguments` is provided to the loaded code upon execution.
