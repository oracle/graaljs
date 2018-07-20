This document serves as migration guide for code previously targeted to the Nashorn engine.
See the [JavaInterop.md](JavaInterop.md) for an overview of supported Java interoperability features.

Both Nashorn and Graal JavaScript support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migration are listed here.

## Nashorn compatibility mode
Graal JavaScript provides a Nashorn compatibility mode.
Some of the functionality necessary for Nashorn compatibility is only available when this flag is set.
This is the case for Nashorn-specific extensions that Graal JavaScript does not want to expose by default.

```
$ js --js.nashorn-compat=true
```

When you start from a Java application, set the flag on Java invocation:

```
$ java -Dpolyglot.js.nashorn-compat=true MyApplication
```

Functionality only available under this flag includes:
* `Java.isJavaFunction`, `Java.isJavaMethod`, `Java.isScriptObject`, `Java.isScriptFunction`
* `new Interface|AbstractClass(fn|obj)`
* Java package globals: `java`, `javafx`, `javax`, `com`, `org`, `edu`
* `JavaImporter`
* `JSAdapter`
* `java.lang.String` methods on string values
* `load("nashorn:parser.js")`, `load("nashorn:mozilla_compat.js")`

## Intentional design differences
Graal JavaScript differs from Nashorn in some aspects that were intentional design decisions.

### Launcher name `js`
When shipped with GraalVM, Graal JavaScript comes with a binary launcher named `js`.
Note that, depending on the build setup, GraalVM might still ship Nashorn and its `jjs` launcher.

### ScriptEngine name `graal.js`
Graal JavaScript is shipped with ScriptEngine support.
Nashorn compatibility mode is turned on for this ScriptEngine.
It registers under several names, including `graal.js`.
Depending on the build setup, GraalVM might still ship Nashorn and provide it via ScriptEngine.

### `ClassFilter`
Graal JavaScript supports a class filter when starting with a polyglot `Context`.
See the [JavaDoc of `Context.Builder.hostClassFilter`](http://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.Builder.html#hostClassFilter-java.util.function.Predicate-)

### Fully qualified names
Graal Javascript requires the use of `Java.type(typename)`.
It does not support accessing classes just by their fully qualified class name by default.
`Java.type` brings more clarity and avoids the accidental use of Java classes in JavaScript code.
Patterns like this:

```js
var bd = new java.math.BigDecimal('10');
```

should be expressed as:

```js
var BigDecimal = Java.type('java.math.BigDecimal');
var bd = new BigDecimal('10');
```

Note that some Java packages (like `java`) are added to the global object in the Nashorn compatibility mode, see below.

### Lossy conversion
Graal JavaScript does not allow lossy conversions of arguments when calling Java methods.
This could lead to bugs with numeric values that are hard to detect.

Graal JavaScript will always select the overloaded method with the narrowest possible argument types that can be converted to without loss.
If no such overloaded method is available, Graal JavaScript throws a `TypeError` instead of lossy conversion.
In general, this affects which overloaded method is executed.

### `ScriptObjectMirror` objects
Graal JavaScript does not provide objects of the class `ScriptObjectMirror`.
Instead, JavaScript objects are exposed to Java code as objects implementing Java's `Map` interface.

Code referencing `ScriptObjectMirror` instances can be rewritten by changing the type to either an interface (`Map`, `List`) or the polyglot [Value](http://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Value.html) class which provides similar capabilities.

### Multithreading
Graal JavaScript supports multithreading by creating several `Context` objects from Java code.
Multiple JavaScript engines can be created from a Java application, and can be safely executed in parallel on multiple threads.

```js
Context polyglot = Context.create();
Value array = polyglot.eval("js", "[1,2,42,4]");

```

Graal JavaScript does not allow the creation of threads from JavaScript applications with access to the current `Context`.
This could lead to unmanagable synchronization problems like data races in a language that is not prepared for multithreading.

```js
new Thread(function() {
    print('printed from another thread'); // throws Exception due to potential synchronization problems
}).start();
```

JavaScript code can create and start threads with `Runnable`s implemented in Java.
The child thread may not access the `Context` of the parent thread or of any other polyglot thread.
In case of violations, an `IllegalStateException` will be thrown.
A child thread may create a new `Context` instance, though.

```js
new Thread(aJavaRunnable).start(); // allowed on Graal JavaScript
```

## Extensions only available in Nashorn compatibility mode
The following extensions to JavaScript available in Nashorn are deactivated in Graal JavaScript by default.
They are provided in GraalVM's Nashorn compatibility mode.
It is highly recommended not to implement new applications based on those features, but only to use it as a means to migrate existing applications to GraalVM.

### String `length` property
Graal JavaScript does not treat the length property of a String specially.
The canonical way of accessing the String length is reading the `length` property.

```
myJavaString.length;
```

Nashorn allows to both access `length` as a property and a function.
Existing function calls `length()` should be expressed as property access.
Nashorn behavior is mimicked in the Nashorn compatibility mode.

### Java packages in the JavaScript global object
Graal JavaScript requires the use of `Java.type` instead of fully qualified names.
In Nashorn compatibility mode, the following Java package are added to the JavaScript global object: `java`, `javafx`, `javax`, `com`, `org`, `edu`.

### JavaImporter
The `JavaImporter` feature is available only in Nashorn compatibility mode.

### JSAdapter
Use of the non-standard `JSAdapter` is discouraged and should be replaced with the equivalent standard `Proxy` feature.
For compatibility, `JSAdapter` is still available in Nashorn compatibility mode.

### Java.* methods
Several methods provided by Nashorn on the `Java` global object are available only in Nashorn compatibility mode or currently not supported by Graal JavaScript.
Available in Nashorn compatibility mode are: `Java.isJavaFunction`, `Java.isJavaMethod`, `Java.isScriptObject`, `Java.isScriptFunction`.
Currently not supported: `Java.asJSONCompatible`.

### Accessors
In Nashorn compatibility mode, Graal JavaScript allows to access getters and setters just by the name as properties, while omitting `get`, `set`, or `is`.

```js
var Date = Java.type('java.util.Date');
var date = new Date();

var myYear = date.year; // calls date.getYear()
date.year = myYear + 1; // calls date.setYear(myYear + 1);
```

Graal JavaScript defines an ordering in which it searches for the field or getters.
It will always first try to read or write the field with the name as provided in the property.
If the field cannot be read or written, it will try to call a getter or setter:
* In case of a read operation, Graal JavaScript will first try to call a getter with the name `get` and the property name in camel case. If that is not available, a getter with the name `is` and the property name in camel case is called. In the second case, the value is returned even if it is not of type boolean.
* In case of a write operation, Graal JavaScript will try to call a setter with the name `set` and the property name in camel case, providing the value as argument to that function.

Nashorn can expose random behavior when both `getFieldName` and `isFieldName` are available.
Nashorn also gives precedence to getters, even when a public field of the exact name is available.

## Additional aspects to consider

### Features of Graal JavaScript
Graal JavaScript supports features of the newest ECMAScript specification and some extensions to that, see [JavaScriptCompatibility.md](JavaScriptCompatibility.md).
Note that this e.g. adds objects to the global scope that might interfere with existing source code unaware of those extensions.

### Console output
Graal JavaScript provides a `print` builtin function compatible with Nashorn.

Note that Graal JavaScript also provides a `console.log` function.
This is an alias for `print` in pure JavaScript mode, but uses an implementation provided by Node.js when running in Node mode.
Behavior around Java objects differs for `console.log` in Node mode as Node.js does not implement special treatment for such objects.

