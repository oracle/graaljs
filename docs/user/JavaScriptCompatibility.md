---
layout: docs
toc_group: js
link_title: GraalJS Compatibility
permalink: /reference-manual/js/JavaScriptCompatibility/
---

# GraalJS Compatibility

GraalJS is an ECMAScript-compliant JavaScript language runtime.
This document explains the public API it presents for user applications written in JavaScript.

* [ECMAScript Language Compliance](#ecmascript-language-compliance)
* [Compatibility Extensions](#compatibility-extensions)
* [GraalJS Extensions](#graalvm-javascript-extensions)

## ECMAScript Language Compliance

GraalJS implements the ECMAScript (ECMA-262) specification and is fully compatible with the [ECMAScript 2024 specification](https://262.ecma-international.org/) (sometimes referred to as the 15th edition).
New features are frequently added to GraalVM when they are confirmed to be part of ECMAScript 2024, see the [CHANGELOG.md](https://github.com/oracle/graaljs/blob/master/CHANGELOG.md) for details.
Older versions starting from ECMAScript 5 can be enabled with a configuration option (by number: `--js.ecmascript-version=5` or by year: `--js.ecmascript-version=2024`).
In a production environment, you might consider specifying a fixed ECMAScript version to be used, as future versions of GraalJS will use newer versions of the specification once available.

GraalJS provides the following function objects in the global scope as specified by ECMAScript, representing the JavaScript core library:
Array, ArrayBuffer, Boolean, DataView, Date, Error, Function, JSON, Map, Math, Number, Object, Promise, Proxy, Reflect, RegExp, Set, SharedArrayBuffer, String, Symbol, TypedArray, WeakMap, and WeakSet.

Additional objects are available under options, for example, `--js.temporal`.
Run `js --help` for the list of available options.

Several of these function objects and some of their members are only available when a certain version of the specification is selected for execution.
For a list of methods provided, inspect the ECMAScript specification.
Extensions to the specification are specified below.

### Internationalization API (ECMA-402)

GraalJS comes with an implementation of the [ECMA-402 Internationalization API](https://tc39.github.io/ecma402), enabled by default (can be disabled using the following option: `--js.intl-402=false`).
This includes the following extensions:
- `Intl.Collator`
- `Intl.DateTimeFormat`
- `Intl.DisplayNames`
- `Intl.ListFormat`
- `Intl.Locale`
- `Intl.NumberFormat`
- `Intl.PluralRules`
- `Intl.RelativeTimeFormat`
- `Intl.Segmenter`

The functionality of a few other built-ins, such as `toLocaleString`, is also updated according to the ECMA-402 specification.

### JavaScript Modules

GraalJS supports modules as defined by ECMAScript 6 and later.
Be aware that the support for this feature continues to increase. 
Be sure to use the latest ECMAScript version for the all the latest features.

When loading modules via a polyglot `Source`, you can use the unofficial `application/javascript+module` MIME type to specify that you are loading a module.
When loading with JavaScript code from a file, make sure the module is loaded from a file with the _.mjs_ extension.
Loading with the `import` keyword is not limited by that, and can `import` from a file of any extension.

## Compatibility Extensions

The following objects and methods are available in GraalJS for compatibility with other JavaScript engines.
Note that the behavior of such methods might not strictly match the semantics of those methods in all existing engines.

### Language Features

#### Conditional Catch Clauses

GraalJS supports conditional catch clauses if the `js.syntax-extensions` option is enabled:
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

- prints the arguments on the console (`stdout` and `stderr`, respectively)
- provides a best-effort human readable output

`print` and `printErr` are available by default and can be deactivated by setting the `js.print` option to `false`.

#### Methods of the `console` Global Object

A global `console` object is provided that offers several methods for debugging purposes.
These methods strive to provide similar functionality as provided in other engines, but do not guarantee identical results.

Note that those methods behave differently when GraalJS is executed in Node.js mode (for example, the `node` executable is started instead of `js`).
Node.js provides its own implementation that is used instead.
* `console.log`, `console.info`, and `console.debug`: an alias for `print(...arg)`
* `console.error`, and `console.warn`: similar to `print`, but using the error IO stream
* `console.assert(check, message)`: prints `message` when `check` is falsy
* `console.clear`: clears the console window if possible
* `console.count()`, and `console.countReset()`: counts and prints how many times it has been called, or resets this counter
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

GraalJS provides a scripting mode compatible with the one provided by the Nashorn engine.
It is enabled with the `js.scripting` option. Make sure to have `--experimental-options` set:
```shell
js --experimental-options --js.scripting=true
```

In scripting mode, several properties and functions are added to the global object, including [readFully](#readfile), [readLine](#readline), `$ARG`, `$ENV`, and `$EXEC`.

There are migration guides available for code previously targeted to the [Nashorn](https://github.com/graalvm/graaljs/blob/master/docs/user/NashornMigrationGuide.md) or [Rhino](https://github.com/graalvm/graaljs/blob/master/docs/user/RhinoMigrationGuide.md) engines.

## GraalJS Extensions

### Graal Object

The `Graal` object is provided as a property of the global object.
It provides Graal-specific information.
The existence of the property can be used to identify whether GraalJS is the current language engine:
```js
if (typeof Graal != 'undefined') {
    print(Graal.versionECMAScript);
    print(Graal.versionGraalVM);
    print(Graal.isGraalRuntime());
}
```

The Graal object is available in GraalJS by default, unless deactivated by an option (`js.graal-builtin=false`).

#### `Graal.versionECMAScript`

- provides the version number (year value) of the GraalJS ECMAScript compatibility mode

#### `Graal.versionGraalVM`

- provides the version of GraalVM, if the current engine is executed on GraalVM

#### `Graal.isGraalRuntime()`

- indicates if GraalJS is executed on a GraalVM-enabled runtime
- If `true`, hot code is compiled by the Graal compiler, resulting in high peak performance.
- If `false`, GraalJS will not be optimized by the Graal Compiler, typically resulting in lower performance.

### `Graal.setUnhandledPromiseRejectionHandler(handler)`

- provides the unhandled promise rejection handler when using option (`js.unhandled-rejections=handler`).
- the handler is called with two arguments: (rejectionReason, unhandledPromise).
- `Graal.setUnhandledPromiseRejectionHandler` can be called with `null`, `undefined`, or empty arguments to clear the handler.

### Java

The `Java` object is only available when [host class lookup](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.Builder.html#allowHostClassLookup-java.util.function.Predicate-) is allowed.
To access Java host classes and its members, they first need to be allowed by the [host access policy](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/HostAccess.html), and when running from a native executable, be registered for [runtime reflection](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/Reflection/).

Note that some functions require the Nashorn compatibility mode to be set (`--js.nashorn-compat=true`).

#### `Java.type(className)`

`Java.type` loads the specified Java class and returns a constructible object that has the static members (for example, methods and fields) of the class and can be used with the `new` keyword to construct new instances:
```js
var BigDecimal = Java.type('java.math.BigDecimal');
var point1 = new BigDecimal("0.1");
var two = BigDecimal.TWO;
console.log(point1.multiply(two).toString());
```

Note that when used directly with the `new` operator, `Java.type(...)` needs to be enclosed in parentheses:
```js
console.log(new (Java.type('java.math.BigDecimal'))("1.1").pow(15));
```

#### `Java.from(javaData)`

`Java.from` creates a shallow copy of the Java data structure (Array, List) as a JavaScript array.

In many cases, this is not necessary; you can typically use the Java data structure directly from JavaScript.

#### `Java.to(jsData, javaType)`

`Java.to` converts the argument to the Java type.

The source object `jsData` is expected to be a JavaScript array, or an array-like object with a `length` property.
The target `javaType` can either be a String (for example, an `"int[]"`) or a type object (such as `Java.type("int[]")`).
Valid target types are Java arrays.
When the target type is omitted, it defaults to `Object[]`.
```js
var jsArray = ["a", "b", "c"];
var stringArrayType = Java.type("java.lang.String[]");
var javaArray = Java.to(jsArray, stringArrayType);
assertEquals('class java.lang.String[]', String(javaArray.getClass()));
var javaArray = Java.to(jsArray);
assertEquals('class java.lang.Object[]', String(javaArray.getClass()));
```

The conversion methods as defined by ECMAScript (for example, `ToString` and `ToDouble`) are executed when a JavaScript value has to be converted to a Java type.
Lossy conversion is disallowed and results in a `TypeError`.

#### `Java.isJavaObject(obj)`

- returns `true` if `obj` is a Java host object
- returns `false` for native JavaScript objects, as well as for objects of other polyglot languages

#### `Java.isType(obj)`

- returns `true` if `obj` is an object representing the constructor and static members of a Java class, as obtained by `Java.type()` or package objects.
- returns `false` for all other arguments

#### `Java.typeName(obj)`

- returns the Java `Class` name of `obj` when `obj` represents a Java type (`isType(obj) === true`) or Java `Class` instance
- returns `undefined` otherwise

#### `Java.isJavaFunction(fn)`

- returns whether `fn` is an object of the Java language that represents a Java function
- returns `false` for all other types, including native JavaScript function, and functions of other polyglot languages

> This function is only available in Nashorn compatibility mode (`--js.nashorn-compat=true`).

#### `Java.isScriptObject(obj)`

- returns whether `obj` is an object of the JavaScript language
- returns `false` for all other types, including objects of Java and other polyglot languages

> This function is only available in Nashorn compatibility mode (`--js.nashorn-compat=true`).

#### `Java.isScriptFunction(fn)`

- returns whether `fn` is a JavaScript function
- returns `false` for all other types, including Java function, and functions of other polyglot languages

> This function is only available in Nashorn compatibility mode (`--js.nashorn-compat=true`).

#### `Java.addToClasspath(location)`

- adds the specified location (a `.jar` file or directory path string) to Java's classpath

### Polyglot

The functions of the `Polyglot` object allow to interact with values from other polyglot languages.

The `Polyglot` object is available by default, unless deactivated by setting the `js.polyglot-builtin` option to `false`.

#### `Polyglot.export(key, value)`

- exports the JavaScript `value` under the name `key` (a string) to the polyglot bindings:
```js
function helloWorld() { print("Hello, JavaScript world"); }
Polyglot.export("helloJSWorld", helloWorld);
```

If the polyglot bindings already had a value identified by `key`, it is overwritten with the new value.
The `value` may be any valid Polyglot value.

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

The value of `sourceCode` is expected to be a String (or convertible to one).

- returns the evaluation result, depending on the `sourceCode` and/or the semantics of the language evaluated:
```js
var pyArray = Polyglot.eval('python', 'import random; [random.uniform(0.0, 1.0) for _ in range(1000)]');
```

Exceptions can occur when an invalid `languageId` is passed, when the `sourceCode` cannot be evaluated by the language, or when the executed program throws one.

#### `Polyglot.evalFile(languageId, sourceFileName)`

- parses the file `sourceFileName` with the interpreter identified by `languageId`

The value of `sourceFileName` is expected to be a String (or convertible to one), representing a file reachable by the current path.

- returns an executable object, typically a function:
```js
var rFunc = Polyglot.evalFile('R', 'myExample.r');
var result = rFunc();
```

Exceptions can occur when an invalid `languageId` is passed, when the file identified by `sourceFileName` cannot be found, or when the language throws an exception during parsing (parse time errors, for example, syntax errors).
Exceptions thrown by the evaluated program are only thrown once the resulting function is evaluated.

The `Polyglot.evalFile` function is available by default when the `Polyglot` builtin is available, unless deactivated by setting the `js.polyglot-evalfile` option to `false`.
It is also available when `js.debug-builtin` is activated.

### Debug

- requires starting the engine with the `js.debug-builtin` option

`Debug` is a GraalJS specific function object that provides functionality for debugging JavaScript code and the JavaScript engine.
This API might change without notice. Do not use for production purposes.

### Global Functions

#### `printErr(...arg)`

- behaves identically to `print`

The only difference is that the error stream is used to print to, instead of the default output stream.

#### `loadWithNewGlobal(source, arguments)`

- behaves similarly to `load` function

The relevant difference is that the code is evaluated in a new global scope (Realm, as defined by ECMAScript).

Source can be of type:

* `java.lang.URL`: the URL is queried for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property.
* all other types: the source is converted to a String.

The value of `arguments` is provided to the loaded code upon execution.

### Related Documentation

* [Java Interoperability](JavaInteroperability.md)