# JavaScript Compatibility

GraalVM JavaScript is a JavaScript (ECMAScript) language execution runtime.
This document explains the public API it provides to user applications written in JavaScript.

* [ECMAScript language compliance](#ecmascript-language-compliance)
* [Compatibility extensions](#compatibility-extensions)
* [GraalVM JavaScript extensions](#graal-javascript-extensions)

## ECMAScript Language Compliance

GraalVM JavaScript implements JavaScript as prescribed in the ECMAScript (ECMA-262) specification.
It is fully compatible with the [ECMAScript 2020 specification](http://www.ecma-international.org/ecma-262/11.0/index.html) (sometimes referred to as "version 11" or "ES11").
Starting with GraalVM 20.1.0, ECMAScript 2020 features are enabled by default.
Older versions starting from ECMAScript 5 can be enabled with a config flag (by number: `--js.ecmascript-version=5` or by year: `--js.ecmascript-version=2019`).
In a production setup you might consider specifying a fixed ECMAScript version to be used, as future versions of GraalVM JavaScript will use newer versions of the specification once available.
For informations on the flags, see the *--help* message of the executable.

GraalVM JavaScript provides the following function objects in the global scope as specified by ECMAScript, representing the JavaScript core library:
Array, ArrayBuffer, Boolean, DataView, Date, Error, Function, JSON, Map, Math, Number, Object, Promise, Proxy, Reflect, RegExp, Set, SharedArrayBuffer, String, Symbol, TypedArray, WeakMap, WeakSet

Additional objects are available under flags, for instance `Intl` (flag: `--js.intl-402`).
Run `js --help` or `js --help:languages` for the list of available flags.

Several of these function objects and some of their members are only available when a certain version of the spec is selected for execution.
For a list of methods provided, inspect the ECMAScript specification.
Extensions to the specification are specified below.

### Internationalization API (ECMA-402)

Internationalization API implementation (see [https://tc39.github.io/ecma402](https://tc39.github.io/ecma402)) can be activated using the following flag: `--js.intl-402=true`.
If you run in native mode (default option), you also need to specify path to your ICU data directory using the following option: `--vm.Dcom.ibm.icu.impl.ICUBinary.dataPath=$GRAAL_VM_DIR/jre/languages/js/icu4j/icudt`,
where `$GRAAL_VM_DIR` refers to your GraalVM installation directory.
If you run in JVM mode (a jvm flag is used), you do not need to specify where your ICU data are located, although you can do it with the mentioned option.

Once you activate the Internationalization API, you can use the following built-ins:

- Intl.NumberFormat
- Intl.DateTimeFormat
- Intl.Collator
- Intl.PluralRules

Functionality of a few other built-ins is then also updated according to the specification linked above.

### JavaScript modules

GraalVM JavaScript supports modules as defined by ECMAScript 6 and later.
Be aware that the support for this feature grew and still grows over time, be sure to use the latest ECMAScript version for the all the latest features.

When loading modules via a polyglot `Source`, you can use the inofficial `application/javascript+module` mime type to specify you are loading a module.
When loading with JavaScript code from a file, make sure the module is loaded from a file with the `.mjs` extension.
Loading with the `import` keyword is not limited by that, you can `import` from a file of any extension.

## Compatibility Extensions

The following objects and methods are available in GraalVM JavaScript for compatibility with other JavaScript execution engines.
Note that the behaviour of such methods might not strictly match the semantics of those methods in all existing engines.

### Language features

#### Conditional catch clauses
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

### Global properties

#### `load(source)`

Loads (parses and executes) the specified JavaScript source code.

Source can be of type:

* a String: the path of the source file or a URL to execute.
* `java.lang.URL`: the URL is queried for the source code to execute if the `js.load-from-url` option is set to `true`.
* `java.io.File`: the File is read for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property, which represent the source name and code, respectively.
* all other types: the source is converted to a String.

`load` is available by default and can be deactivated by setting the `js.load` option to `false`.

#### `print(...arg)` and `printErr(...arg)`

Prints the arguments on the console (stdout and stderr, respectively).
Provides a best-effort human readable output.

`print` and `printErr` are available by default and can be deactivated by setting the `js.print` option to `false`.

#### Methods of the `console` global object

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

### Additional global functions in the `js` shell

#### `quit(status)`

Exits the engine and returns the specified status code.

#### `read(file)`

This function reads the content of `file`.
The result is returned as String.

The argument `file` can be of type:

* `java.io.File`: the file is used directly.
* all other types: `file` is converted to a String and interpreted as file name.

#### `readbuffer(file)`

This function reads the content of `file` similar to the `read` function.
The result is returned as a JavaScript `ArrayBuffer` object.

#### `readline()`

This function reads one line of input from the input stream.
It returns a String as result.

### Object

#### `Object.prototype.__defineGetter__(prop, func)`

Defines the `prop` property of `this` to be the getter function `func`.
This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__defineSetter__(prop, func)`

Defines the `prop` property of `this` to be the setter function `func`.
This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__lookupGetter__(prop)`

Returns the getter function for property `prop` of the object as set by `__defineGetter__`.
This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

#### `Object.prototype.__lookupSetter__(prop)`

Returns the setter function for property `prop` of the object as set by `__defineSetter__`.
This functionality is deprecated in most JavaScript engines.
In recent ECMAScript versions, getters and setters are natively supported by the language.

### Nashorn scripting mode
GraalVM JavaScript provides a scripting mode compatible to the one provided by the Nashorn engine.
It is enabled with the `js.scripting` option, make also sure to have `--experimental-options` set:

```
$ js --experimental-options --js.scripting=true
```

In scripting mode, several properties and functions are added to the global object, including [readFully](#readfile), [readLine](#readline), `$ARG`, `$ENV`, and `$EXEC`.

We provide migration guides for code previously targeted to the [Nashorn](https://github.com/graalvm/graaljs/blob/master/docs/user/NashornMigrationGuide.md) or [Rhino](https://github.com/graalvm/graaljs/blob/master/docs/user/RhinoMigrationGuide.md) engines.

## GraalVM JavaScript Extensions

### Graal

The `Graal` object is provided as property of the global object.
It provides Graal-specific information.
The existence of the property can be used to identify whether the GraalVM JavaScript engine is the current language engine.

```js
if (typeof Graal != 'undefined') {
    print(Graal.versionJS);
    print(Graal.versionGraalVM);
    print(Graal.isGraalRuntime);
}
```

The Graal object is available in GraalVM JavaScript by default, unless deactivated by an option (`js.graal-builtin=false`).

#### `Graal.versionJS`

Provides the version number of GraalVM JavaScript.

#### `Graal.versionGraalVM`

Provides the version of the GraalVM, if the current engine is executed on a GraalVM.

#### `Graal.isGraalRuntime`

Provides whether GraalVM JavaScript is executed on a Graal-enabled runtime.
If `true`, hot code is compiled by the GraalVM Compiler, resulting in high peak performance.
If `false`, GraalVM JavaScript will not be optimized by the GraalVM Compiler, typically resulting in lower performance.

### Java

The `Java` object is only available when the engine is started in JVM mode (`--jvm` flag).

Note that some functions require a Nashorn compatibility mode flag to be set.
On the GraalVM, this flag can be set with:

```
$ js --jvm --experimental-options --js.nashorn-compat=true
```

#### `Java.type(className)`

The `type` function loads the specified Java class and provides it as an object.
Fields of this object can be read directly from it, and new instances can be created with the JavaScript ```new``` keyword.

```js
var BigDec = Java.type('java.math.BigDecimal');
var bd = new BigDec("0.1");
console.log(bd.add(bd).toString());
```

#### `Java.from(javaData)`

The `from` function creates a shallow copy of the Java datastructure (Array, List) as a JavaScript array.
In many cases, this is not necessary, you can typically use the Java datastructure directly from JavaScript.

#### `Java.to(jsData, toType)`

The `to` function converts the argument to a Java dataype.
The source object `jsData` is expected to be a JavaScript array, or object with a `length` property.
The target `toType` can either be a String (e.g. `"int[]"`) or a type object (e.g., `Java.type("int[]")`).
Valid target types are Java arrays.
When no target type is provided, `Object[]` is assumed.

```js
var jsArr = ["a", "b", "c"];
var strArrType = Java.type("java.lang.String[]");
var javaArr = Java.to(jsArr, strArrType);
assertEquals('class [Ljava.lang.String;', String(javaArr.getClass()));
```

The conversion methods as defined by ECMAScript (e.g., `ToString`, `ToDouble`) are executed when a JavaScript value has to be converted to a Java type.
Lossy conversion is disallowed and results in a TypeError.

#### `Java.isJavaObject(obj)`

The `isJavaObject` method returns whether `obj` is an object of the Java language.
It returns `false` for native JavaScript objects, as well as for objects of other polyglot languages.

#### `Java.isType(obj)`

The `isType` method returns whether `obj` is an object of the Java language, representing a Java `Class` instance.
It returns `false` for all other arguments.

#### `Java.typeName(obj)`

The `typeName` method returns the Java `Class` name of `obj`.
`obj` is expected to represent a Java `Class` instance, i.e., `isType(obj)` should return true; otherwise, `undefined` is returned.

#### `Java.isJavaFunction(fn)`

The `isJavaFunction` method returns whether `fn` is an object of the Java language that represents a Java function.
It returns `false` for all other typies, including native JavaScript function, and functions of other polyglot languages.

This function requires the Nashorn compatibility mode flag.

#### `Java.isScriptObject(obj)`

The `isScriptObject` method returns whether `obj` is an object of the JavaScript language.
It returns `false` for all other types, including objects of Java and other polyglot languages.

This function requires the Nashorn compatibility mode flag.

#### `Java.isScriptFunction(fn)`

The `isScriptFunction` method returns whether `fn` is a JavaScript function.
It returns `false` for all other types, including Java function, and functions of other polyglot languages.

This function requires the Nashorn compatibility mode flag.

#### `Java.addToClasspath(location)`

The `addToClasspath` method adds the specified location (file name or path name, as String) to Java's classpath.

### Polyglot

The functions of the `Polyglot` object allow to interact with values from other polyglot languages.

The `Polyglot` object is available by default, unless deactivated by setting the `js.polyglot-builtin` option to `false`.

#### `Polyglot.export(key, value)`

Exports the JavaScript `value` under the name `key` (a string) to the polyglot bindings.

```js
function helloWorld() { print("Hello, JavaScript world"); }
Polyglot.export("helloJSWorld", helloWorld);
```

If the polyglot bindings already had a value identified by `key`, it is overwritten with the new value.
Throws a `TypeError` if `key` is not a string or missing.
The `value` may be any valid Polyglot value.

#### `Polyglot.import(key)`

Imports the value identified by `key` (a string) from the polyglot bindings and returns it.

```js
var rubyHelloWorld = Polyglot.import("helloRubyWorld");
rubyHelloWorld();
```

If no language has exported a value identified by `key`, `undefined` is returned.
Throws a `TypeError` if `key` is not a string or missing.

#### `Polyglot.eval(languageId, sourceCode)`

Parses and evaluates the `sourceCode` with the interpreter identified by `languageId`.
The value of `sourceCode` is expected to be a String (or convertable to one).
Returns the evaluation result, depending on the `sourceCode` and/or the semantics of the language evaluated.

```js
var rArray = Polyglot.eval('R', 'runif(1000)');
```

Exceptions can occur when an invalid `languageId` is passed, when the `sourceCode` cannot be evaluated by the language, or when the executed program throws one.

#### `Polyglot.evalFile(languageId, sourceFileName)`

Parses the file `sourceFileName` with the interpreter identified by `languageId`.
The value of `sourceFileName` is expected to be a String (or convertable to one), representing a file reachable by the current path.
Returns an executable object, typically a function.

```js
var rFunc = Polyglot.evalFile('R', 'myExample.r');
var result = rFunc();
```

Exceptions can occur when an invalid `languageId` is passed, when the file identified by `sourceFileName` cannot be found, or when the language throws an exception during parsing (parse time errors, e.g. syntax errors).
Exceptions thrown by the evaluated program are only thrown once the resulting function is evaluated.

The `Polyglot.evalFile` function is available by default when the `Polyglot` builtin is available, unless deactivated by setting the `js.polyglot-evalfile` option to `false`.
It is also available when `js.debug-builtin` is activated.

### Debug

requires starting the engine with the `js.debug-builtin` flag.

`Debug` is a GraalVM JavaScript specific function object that provides functionality for debugging JavaScript code and the GraalVM JavaScript compiler.
This API might change without notice, do not use for production purposes!

### Global functions

#### `printErr(...arg)`

The method `printErr` behaves identical to `print`.
The only difference is, that the error stream is used to print to, instead of the default output stream.

#### `loadWithNewGlobal(source, arguments)`

This method behaves similar to `load` function.
Relevant difference is that the code is evaluated in a new global scope (Realm, as defined by ECMAScript).

Source can be of type:

* `java.lang.URL`: the URL is queried for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property.
* all other types: the source is converted to a String.

The value of `arguments` is provided to the loaded code upon execution.
