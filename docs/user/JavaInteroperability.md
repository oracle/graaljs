# Java Interoperability

GraalVM JavaScript is a JavaScript (ECMAScript) language execution runtime.
It allows interoperability with Java code.
This document describes the features and usage of this JavaScript to Java interoperability feature.

For a reference of GraalVM JavaScript's public API, see [JavaScriptCompatibility.md](JavaScriptCompatibility.md).
Migration guides for [Rhino](RhinoMigrationGuide.md) and [Nashorn](NashornMigrationGuide.md) are available.

### Launching GraalVM JavaScript
Depending on how you build GraalVM JavaScript, it is started in different ways.
GraalVM CE or EE by default ships with a `js` and `node` native launcher.
The following examples assume this setup is used.

### Enabling Java interoperability
In GraalVM CE or EE images, the `js` and `node` binaries are started in an ahead-of-time compiled native mode by default.
In that mode, Java interoperability is not available.

To enable Java interoperability, the `--jvm` option has to be provided to the native launcher.
This way, GraalVM JavaScript is executed on a traditional JVM and allows full Java interoperability.

#### Classpath
To load Java classes you need to have them on the Java classpath.
You can specify the classpath with the `--vm.classpath=<classpath>` option (or short: `--vm.cp=<classpath>`).
```
    node --jvm --vm.cp=/my/class/path
    js --jvm --vm.cp=/my/class/path
```
The method `Java.addToClasspath()` can be used to programmatically add to the classpath at runtime.

### Polyglot Context
The preferred method of launching GraalVM JavaScript with Java interop support instance is via polyglot `Context`.
For that, a new `org.graalvm.polyglot.Context` is built with the `hostAccess` option allowing access and a `hostClassLookup` predicate defining the Java classes you allow access to:

```java
Context context = Context.newBuilder("js").
    allowHostAccess(HostAccess.ALL).
    allowHostClassLookup(className -> true).  //allows access to all Java classes
    build();
context.eval("js", jsSourceCode);
```

See the [Polyglot Programming](https://www.graalvm.org/docs/reference-manual/polyglot-programming/) reference for more details.

### ScriptEngine (JSR 223)
The `org.graalvm.polyglot.Context` is the preferred execution method for interoperability with languages and tool of the GraalVM.
In addition, GraalVM JavaScript is fully compatible with JSR 223 and supports the `ScriptEngine API`.
Internally, the GraalVM JavaScript ScriptEngine wraps a polyglot context instance.

```java
ScriptEngine eng = new ScriptEngineManager().getEngineByName("graal.js");
Object fn = eng.eval("(function() { return this; })");
Invocable inv = (Invocable) eng;
Object result = inv.invokeMethod(fn, "call", fn);
```

## Java Interoperability Features
Rhino, Nashorn and GraalVM JavaScript provide a set of features to allow interoperability from `JavaScript` to `Java`.
While the overall feature set is mostly comparable, the engines differ in exact syntax, and partly, semantics.

### Class access
To access a Java class, GraalVM JavaScript supports the `Java.type(typeName)` function.

```js
var FileClass = Java.type('java.io.File');
```

By default, Java classes are not automatically mapped to global variables, e.g., there is no `java` global property in GraalVM JavaScript.
Existing code accessing e.g. `java.io.File` should be rewritten to use the `Java.type(name)` function.

```js
var FileClass = Java.type("java.io.File"); //GraalVM JavaScript compliant syntax
var FileClass = java.io.File;              //FAILS in GraalVM JavaScript
```

GraalVM JavaScript provides a `Packages` global property (and `java` etc. if the `js.nashorn-compat` option is set) for compatibility.
However, explicitly accessing the required class with `Java.type` should be preferred whenever possible for two reasons:
1. It allows resolving the class in one step rather than trying to resolve each property as a class.
2. `Java.type` immediately throws a `TypeError` if the class cannot be found or is not accessible rather than silently treating an unresolved name as a package.

### Constructing Java objects
Java objects can be constructed with JavaScript's `new` keyword.

```js
var FileClass = Java.type('java.io.File');
var file = new FileClass("myFile.md");
```

### Field and method access
Static fields of a Java class or fields of a Java object can be accessed like JavaScript properties.

```js
var JavaPI = Java.type('java.lang.Math').PI;
```

Java methods can be called like JavaScript functions.

```js
var file = new (Java.type('java.io.File'))("test.md");
var fileName = file.getName();
```

#### Conversion of method arguments
JavaScript is defined to operate on the `double` number type.
GraalVM JavaScript might internally use additional Java data types for performance reasons (e.g., the `int` type).

When calling Java methods, a value conversion might be required.
This happens when the Java method expects a `long` parameter, and a `int` is provided from GraalVM JavaScript (`type widening`).
If this conversion caused a lossy conversion, a `TypeError` is thrown.

```java
//Java
void longArg   (long arg1);
void doubleArg (double arg2);
void intArg    (int arg3);
```
```js
//JavaScript
javaObject.longArg(1);     //widening, OK
javaObject.doubleArg(1);   //widening, OK
javaObject.intArg(1);      //match, OK

javaObject.longArg(1.1);   //lossy conversion, TypeError!
javaObject.doubleArg(1.1); //match, OK
javaObject.intArg(1.1);    //lossy conversion, TypeError!
```

#### Selection of method
Java allows overloading of methods by argument types.
When calling from JavaScript to Java, the method with the narrowest available type that the actual argument can be converted to without loss is selected.

```java
//Java
void foo(int arg);
void foo(short arg);
void foo(double arg);
void foo(long arg);
```
```js
//JavaScript
javaObject.foo(1);              //will call foo(short arg);
javaObject.foo(Math.pow(2,16)); //will call foo(int arg);
javaObject.foo(1.1);            //will call foo(double arg);
javaObject.foo(Math.pow(2,32)); //will call foo(long arg);
```

Note that there currently is no way of overriding this behavior from GraalVM JavaScript.
In the example above, one might want to always call `foo(int arg)`, even when `foo(short arg)` can be reached with lossless conversion (`foo(1)`).
Future versions of GraalVM JavaScript might lift that restriction by providing an explicit way to select the method to be called.

### Package access
GraalVM JavaScript provides a `Packages` global property.

```
> Packages.java.io.File
JavaClass[java.io.File]
```

### Array access
GraalVM JavaScript supports the creation of Java arrays from JavaScript code.
Both the patterns suggested by Rhino and Nashorn are supported:

```js
//Rhino pattern
var JArray = Java.type('java.lang.reflect.Array');
var JString = Java.type('java.lang.String');
var sarr = JArray.newInstance(JString, 5);

//Nashorn pattern
var IntArray = Java.type("int[]");
var iarr = new IntArray(5);
```

The arrays created are Java types, but can be used in JavaScript code:

```js
iarr[0] = iarr[iarr.length] * 2;
```

### Map access
In GraalVM JavaScript you can create and access Java Maps, e.g. `java.util.HashMap`.

```js
var HashMap = Java.type('java.util.HashMap');
var map = new HashMap();
map.put(1, "a");
map.get(1);
```

GraalVM JavaScript supports iterating over such map similar to Nashorn:

```js
for (var key in map) {
    print(key);
    print(map.get(key));
}
```

### List access
In GraalVM JavaScript you can create and access Java Lists, e.g. `java.util.ArrayList`.

```js
var ArrayList = Java.type('java.util.ArrayList');
var list = new ArrayList();
list.add(42);
list.add("23");
list.add({});

for (var idx in list) {
    print(idx);
    print(list.get(idx));
}
```

### String access
GraalVM JavaScript can create Java strings with Java interoperability.
The length of the string can be queried with the `length` property.
Note that `length` is a value property and cannot be called as a function.

```js
var javaString = new (Java.type('java.lang.String'))("Java");
javaString.length === 4;
```

Note that GraalVM JavaScript uses Java strings internally to represent JavaScript strings, so above code and the JavaScript string literal `"Java"` are actually not distinguishable.

### Iterating properties
Properties (fields and methods) of Java classes and Java objects can be iterated with a JavaScript `for..in` loop.

    var m = Java.type('java.lang.Math')
    for (var i in m) { print(i); }
    > E
    > PI
    > abs
    > sin
    > ...

This is working as in existing Rhino code.

### Access to JavaScript objects from Java
JavaScript objects are exposed to Java code as instances of `com.oracle.truffle.api.interop.java.TruffleMap`.
This class implements Java's `Map` interface.

### JavaImporter
The `JavaImporter` feature is available only in Nashorn compatibility mode (`js.nashorn-compat` option).

### Console output of Java classes and Java objects
GraalVM JavaScript provides both `print` and `console.log`.

GraalVM JavaScript provides a `print` builtin function compatible with Nashorn.

The `console.log` is provided by Node.js directly.
It does not provide special treatment of interop objects.
Note that the default implementation of `console.log` on GraalVM JavaScript is just an alias for `print`, and Node's implementation is only available when running on Node.js.

### Exceptions
Exceptions thrown in Java code can be caught in JavaScript code.
They are represented as Java objects.

```js
try {
    Java.type('java.lang.Class').forName("nonexistent");
} catch (e) {
    print(e.getMessage());
}
```

### Promises

GraalVM JavaScript provides support for interoperability between JavaScript `Promise` objects and Java.
Java objects can be exposed to JavaScript code as _thenable_ objects, allowing JavaScript code to `await` Java objects.
Moreover, JavaScript `Promise` objects are regular JavaScript objects, and can be accessed from Java using the mechanisms described in this document.
This allows Java code to be called back from JavaScript when a JavaScript promise is resolved or rejected.

#### Creating JavaScript `Promise` objects that can be resolved from Java

JavaScript applications can create `Promise` objects delegating to Java the resolution of the `Promise` instance.
This can be achieved from JavaScript by using a Java object as the ["executor"](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise) function of the JavaScript `Promise`.
For example, Java objects implementing the following functional interface can be used to create new `Promise` objects:
```
@FunctionalInterface
public interface PromiseExecutor {
    void onPromiseCreation(Value onResolve, Value onReject);
}
```
Any Java object implementing `PromiseExecutor` can be used to create a JavaScript `Promise`:
```
// `javaExecutable` is a Java object implementing the `PromiseExecutor` interface
var myPromise = new Promise(javaExecutable).then(...);
```
JavaScript `Promise` objects can be created not only using functional interfaces, but also using any other Java object that can be executed by the GraalVM JavaScript engine (for example, any Java object implementing the Polyglot [ProxyExecutable](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/proxy/ProxyExecutable.html) interface).
More detailed example usages are available in the GraalVM JavaScript [unit tests](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/interop/AsyncInteropTest.java).

#### Using `await` with Java Objects

JavaScript applications can use the `await` expression with Java objects.
This can be useful when Java and JavaScript have to interact with asynchronous events.
To expose a Java object to GraalVM JavaScript as a _thenable_ object, the Java object should implement a method called `then()` having the following signature:
```
void then(Value onResolve, Value onReject);
```
When `await` is used with a Java object implementing `then()`, the GraalVM JavaScript runtime will treat the object as a JavaScript `Promise`.
The `onResolve` and `onReject` arguments are executable `Value` objects that should be used by the Java code to resume or abort the JavaScript `await` expression associated with the corresponding Java object.
More detailed example usages are available in the GraalVM JavaScript [unit tests](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/interop/AsyncInteropTest.java).

#### Using JavaScript Promises from Java

`Promise` objects created in JavaScript can be exposed to Java code like any other JavaScript object.
Java code can access such objects like normal `Value` objects, with the possibility to register new promise resolution functions using the `Promise`'s default `then()` and `catch()` functions.
As an example, the following Java code registers a Java callback to be executed when a JavaScript promise resolves:
```
Value jsPromise = context.eval(ID, "Promise.resolve(42);");
Consumer<Object> javaThen = (value) -> System.out.println("Resolved from JavaScript: " + value);
jsPromise.invokeMember("then", javaThen);
```
More detailed example usages are available in the GraalVM JavaScript [unit tests](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/interop/AsyncInteropTest.java).

## Multithreading

GraalVM JavaScript supports multithreading when used in combination with Java. More details about the GraalVM JavaScript multithreading model can be found in the [Multithreading](Multithreading.md) documentation.
