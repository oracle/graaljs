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

Functionality only available under this flag includes:
* `load("nashorn:parser.js")`

## Intentional design differences
Graal JavaScript differs from Nashorn in some aspects that were intentional design decisions.

### Launcher name `js`
When shipped with GraalVM, Graal JavaScript comes with a binary launcher named `js`.
Note that, depending on the build setup, GraalVM might still ship Nashorn and its `jjs` launcher.

### ScriptEngine name `graal.js`
Graal JavaScript is shipped with ScriptEngine support.
It registers under several names, including `graal.js`.
Depending on the build setup, GraalVM might still ship Nashorn and provide it via ScriptEngine.

### `ClassFilter`
Graal JavaScript supports a class filter when starting with a polyglot `Context`.
See the [JavaDoc of `Context.Builder.hostClassFilter`](http://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.Builder.html#hostClassFilter-java.util.function.Predicate-)

### Fully qualified names
Graal Javascript requires the use of `Java.type(typename)`.
It does not support accessing classes just by their fully qualified class name.
`Java.type` brings more clarity and avoids the accidental use of Java classes in JavaScript code.
Patterns like this:

```
var bd = new java.math.BigDecimal('10');
```

should be expressed as:

```
var BigDecimal = Java.type('java.math.BigDecimal');
var bd = new BigDecimal('10');
```

### Lossy conversion
Graal JavaScript does not allow lossy conversions of arguments when calling Java methods.
This could lead to severe bugs with numeric values that are hard to detect.

Graal JavaScript will always select the overloaded method with the narrowest possible argument types that can be converted to without loss.
If no such overloaded method is available, Graal JavaScript throws a `TypeError` instead of lossy conversion.
In general, this affects which overloaded method is executed.

### `ScriptObjectMirror` objects
Graal JavaScript does not provide objects of the class `ScriptObjectMirror`.
Instead, JavaScript objects are exposed to Java code as objects implementing Java's `Map` interface.

Code referencing `ScriptObjectMirror` instances can be rewritten by changing the type to `Map`.

## Incompatibilities being worked on
The following incompatibilities are present in Graal JavaScript currently.
Future versions of Graal JavaScript will provide better compatibility to Nashorn in those areas.

### String `length` property
Graal JavaScript does not tread the length property of a String specially.
I only provides one canonical way of accessing a String length: by reading the `length` property.

```
myJavaString.length;
```

Nashorn allows to both access `length` as a property and a function.
Existing function calls `length()` should be expressed as property access.

### Using JavaBeans

Graal JavaScript does not treat accessor and mutator methods in JavaBeans as equivalent JavaScript properties. So that e.g. the last line of the following example:

```
var Date = Java.type("java.util.Date")
var date = new Date()
date.year + 1900
```

should be expressed as:

```
date.getYear() + 1900
```

### Multithreading
Graal JavaScript supports multithreading by creating several `Context` objects from Java code.
Multiple JavaScript engines can be created from a Java application, and can be safely executed in parallel on multiple threads.

```
Context polyglot = Context.create();
Value array = polyglot.eval("js", "[1,2,42,4]");
```

Graal JavaScript does not allow the creation of threads from JavaScript applications with access to the current `Context`.
This could lead to unmanagable synchronization problems like data races in a language that is not prepared for multithreading.

```
new Thread(function() {
    print('printed from another thread'); // throws Exception due to potential synchronization problems
}).start();
```

JavaScript code can create and start threads with `Runnable`s implemented in Java.
The child thread may not access the `Context` of the parent thread or of any other polyglot thread.
In case of violations, an `IllegalStateException` will be thrown.
A child thread may create a new `Context` instance, though.

```
new Thread(aJavaRunnable).start(); // allowed on Graal JavaScript
```

### JavaImporter
The `JavaImporter` feature is currently not supported by Graal JavaScript.

### Java.* methods
Several methods provided by Nashorn on the `Java` global object are currently not supported by Graal JavaScript.
This applies to `Java.extend`, `Java.super`, `Java.isJavaMethod`, `Java.isJavaFunction`, `Java.isScriptFunction`, `Java.isScriptObject`, and `Java.asJSONCompatible`.
We are evaluating their use in real-world applications and might add them in the future, by default or behind a flag.

## Additional aspects to consider

### Features of Graal JavaScript
Graal JavaScript supports features of the newest ECMAScript specification and some extensions to that, see [JavaScriptCompatibility.md](JavaScriptCompatibility.md).
Note that this e.g. adds objects to the global scope that might interfere with existing source code unaware of those extensions.

### Console output
Graal JavaScript provides a `print` builtin function compatible with Nashorn.

Note that Graal JavaScript also provides a `console.log` function.
This is an alias for `print` in pure JavaScript mode, but uses an implementation provided by Node.js when running in Node mode.
Behavior around Java objects differs for `console.log` in Node mode as Node.js does not implement special treatment for such objects.

