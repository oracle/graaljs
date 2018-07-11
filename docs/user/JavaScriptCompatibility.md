# Graal JavaScript language compatibility

Graal JavaScript is a JavaScript (ECMAScript) language execution runtime.
This document explains the public API it provides to user applications written in JavaScript.

* [ECMAScript language compliance](#ecmascript-language-compliance)
* [Compatibility extensions](#compatibility-extensions)
* [Graal JavaScript extensions](#graal-javascript-extensions)

## ECMAScript language compliance

Graal JavaScript implements JavaScript as prescribed in the ECMAScript (ECMA-262) specification.
By default, Graal JavaScript is compatible with the 2017 edition of ECMAScript (sometimes referred to as "version 8" or "ES8"), see [http://www.ecma-international.org/ecma-262/8.0/](http://www.ecma-international.org/ecma-262/8.0/).
Older versions, as well as some features of the most recent version, can be enabled with special flags on startup.
For informations on the flags, see the *--help* message of the executable.

Graal JavaScript provides the following function objects in the global scope as specified by ECMAScript, representing the JavaScript core library:
Array, ArrayBuffer, Boolean, DataView, Date, Error, Function, JSON, Map, Math, Number, Object, Promise, Proxy, Reflect, RegExp, Set, SharedArrayBuffer, String, Symbol, TypedArray, WeakMap, WeakSet

Some additional objects are available under flags (run `js --help` for the list of available flags):
Atomics, Intl, SIMD

Several of these function objects and some of their members are only available when a certain version of the spec is selected for execution.
For a list of methods provided, inspect the ECMAScript specification.
Extensions to the specification are specified below.

### Internationalization API (ECMA-402)

Internationalization API implementation (see [https://tc39.github.io/ecma402](https://tc39.github.io/ecma402)) can be activated using the following flag: `--js.intl-402=true`.
If you run in native mode (default option), you also need to specify path to your ICU data directory using the following flag: `--native.Dcom.ibm.icu.impl.ICUBinary.dataPath=$GRAAL_VM_DIR/jre/languages/js/icu4j/icudt`,
where `$GRAAL_VM_DIR` refers to your GraalVM installation directory.
If you run in JVM mode (a jvm flag is used), you do not need to specify where your ICU data are located, although you can do it with `--jvm.Dcom.ibm.icu.impl.ICUBinary.dataPath=$GRAAL_VM_DIR/jre/languages/js/icu4j/icudt`.

Once you activate the Internationalization API, you can use the following built-ins:

- Intl.NumberFormat
- Intl.DateTimeFormat
- Intl.Collator
- Intl.PluralRules

Functionality of a few other built-ins is then also updated according to the specification linked above.

### Regular Expressions

Graal JavaScript strives to support all regular expression features of ECMAScript.
It employs two regular expression engines:
* [TRegex](https://github.com/oracle/graal/tree/master/regex), an advanced engine producing tree-based automata with high peak performance.
* [Joni](https://github.com/jruby/joni), an adopted port of Nashorn's regular expression engine (a bytecode compiler).

While both engines support most regular expressions, they lack support for some of the advaced features of the newest ECMAScript specifications.
TRegex is the default engine, and Joni will be used in case a feature is not supported in TRegex.
In the rare case several features are mixed in one regular expression so no single engine can execute the expression, Graal JavaScript has to throw a JavaString Error.
We are working on improving both engines to limit the number of unsupported regular expressions.

The current status of feature support in both engines is listed below (features not listed are supported by both engines):

Feature                                                                                      | TRegex | Joni
-------------------------------------------------------------------------------------------- | ------ | ----
Backreferences                                                                               | ❌     | ✓
Negative lookaround<sup>[1](#fn1)</sup>                                                      | ❌     | ✓
Unicode mode (`'u'` flag)                                                                    | ✓      | ❌
[Unicode property escapes](https://github.com/tc39/proposal-regexp-unicode-property-escapes) | ✓      | ❌
[Full lookbehind](https://github.com/tc39/proposal-regexp-lookbehind)<sup>[2](#fn2)</sup>    | ❌     | ❌

<sub>
<a name="fn1">1</a>: Positive lookaround is supported in both engines.
<br/>
<a name="fn2">2</a>: TRegex and Joni only support a subset of the lookbehind assertions that can match at most a bounded number of characters.
</sub>

<br/>
<br/>

We are currently working on implementing negative lookahead and more support for lookbehind in TRegex. On the other hand, full support of backreferences is out of scope for a finite state automaton engine like TRegex.
Graal JavaScript uses [Nashorn](http://openjdk.java.net/projects/nashorn/)'s port of the Joni engine, which is based on ECMAScript 5 and misses support for most features of ECMAScript 6 and beyond.
For more details on the implementation of the engines, see [RegExpImplementation.md](../contributor/RegExpImplementation.md).

## Compatibility extensions

The following objects and methods are available in Graal JavaScript for compatibility with other JavaScript execution engines.
Note that the behaviour of such methods might not strictly match the semantics of those methods in all existing engines.

### Language features

#### Conditional catch clauses
Graal JavaScript supports conditional catch clauses if the `js.syntax-extensions` option is enabled:

```js
try {
    myMethod(); // can throw
} catch (e if e instanceof TypeError) {
    print("TypeError caught");
} catch (e) {
    print("another Error caught");
}
```

### Global methods

#### `exit(status)` or `quit(status)`

Exits the engine and returns the specified status code.

#### `load(source)`

Loads (parses and executes) the specified JavaScript source code.

Source can be of type:

* a String: the path of the source file or a URL to execute.
* `java.lang.URL`: the URL is queried for the source code to execute.
* `java.io.File`: the File is read for the source code to execute.
* a JavaScript object: the object is queried for a `name` and a `script` property, which represent the source name and code, respectively.
* all other types: the source is converted to a String.

#### `print(...arg)` and `console.log(...arg)`

Prints the arguments on the console.
Provides a best-effort human readable output.

Note that `console.log` behaves differently when Graal JavaScript is executed in Node.js mode (i.e., the `node` executable is started instead of `js`).
While normally, `console.log` is just an alias for `print`, on Node.js Node's own implementation is executed.

#### `read(file)` or `readFully(file)`

This function reads the content of `file`.
The result is returned as String.

The argument `file` can be of type:

* `java.io.File`: the file is used directly.
* all other types: `file` is converted to a String and interpreted as file name.

#### `readbuffer(file)`

This function reads the content of `file` similar to the `read` function.
The result is returned as a JavaScript `ArrayBuffer` object.

#### `readLine(prompt)` or `readline(prompt)`

This function reads one line of input from the input stream.
It returns a String as result.

An optional `prompt` value can be provided, that is print to the output stream.
`prompt` is ignored when its value is `undefined`.

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
Graal JavaScript provides a scripting mode compatible to the one provided by the Nashorn engine.
It is enabled with the `js.scripting` option:

```
$ js --js.scripting=true
```

In scripting mode, several properties and functions are added to the global object, including `$ARG`, `$ENV`, and `$EXEC`.

## Graal JavaScript extensions

### Graal

The `Graal` object is provided as property of the global object.
It provides Graal-specific information.
The existence of the property can be used to identify whether the Graal JavaScript engine is the current language engine.

```js
if (typeof Graal != 'undefined') {
    print(Graal.versionJS);
    print(Graal.versionGraalVM);
    print(Graal.isGraalRuntime);
}
```

The Graal object is available in Graal JavaScript by default, unless deactivated by an option (`truffle.js.GraalBuiltin=false`).

#### `Graal.versionJS`

Provides the version number of Graal JavaScript.

#### `Graal.versionGraalVM`

Provides the version of the GraalVM, if the current engine is executed on a GraalVM.

#### `Graal.isGraalRuntime`

Provides whether Graal JavaScript is executed on a Graal-enabled runtime.
If `true`, hot code is compiled by the Graal compiler, resulting in high peak performance.
If `false`, Graal JavaScript will not be optimized by the Graal compiler, typically resulting in lower performance.

### Java

The `Java` object is only available when the engine is started in JVM mode (`--jvm` flag).

Note that some functions require a Nashorn compatibility mode flag to be set.
On the GraalVM, this flag can be set with:

```
$ js --jvm --js.nashorn-compat=true
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
When no `toType` is provided, `Object[]` is assumed.

```js
var jsArr = ["a", "b", "c"]
var strArrType = Java.type("java.lang.String[]")
var javaArr = Java.to(jsArr, strArrType)
assertEquals('class [Ljava.lang.String;', String(javaArr.getClass()));
```

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

### Polyglot

The functions of the `Polyglot` object allow to interact with values from other polyglot languages.

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

If no language has exported a value identified by `key`, `null` is returned.
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

### Debug

requires starting the engine with the `js.debug-builtin` flag.

`Debug` is a Graal JavaScript specific function object that provides functionality for debugging JavaScript code and the Graal JavaScript compiler.
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

