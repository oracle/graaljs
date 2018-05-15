# Graal.js JavaScript to Java interoperability

Graal.js is a JavaScript (ECMAScript) language execution runtime.
It allows interoperability with Java code.
This document describes the usage of this JavaScript to Java interoperability feature.
It also serves as migration guide to users porting existing applications written for Rhino or Nashorn.

For a reference of Graal.js' public API, see [GRAAL.JS-API.md](GRAAL.JS-API.md).

* [Launching Graal.js](#launching-graaljs)
* [Java interoperability features](#java-interoperability-features)
* [Rhino to Graal.js migration guide](#rhino-to-graaljs-migration-guide)
* [Nashorn to Graal.js migration guide](#nashorn-to-graaljs-migration-guide)

## Launching Graal.js

### Native launcher
In a standalone binary distribution, e.g. as part of GraalVM, Graal.js is started with a native launcher.
The binaries provided typically are `js` and `node`.

### Enabling Java interoperability
By default, the `js` and `node` binaries are started in an ahead-of-time compiled native mode.
In that mode, Java interoperability is not available.

To enable Java interoperability, the `--jvm` option has to be provided to the native launcher.
This way, Graal.js is executed on a traditional JVM and allow full Java interoperability.

#### Classpath
To load Java classes you need to have them on the Java classpath.
You can specify the classpath with the `--jvm.classpath=<classpath>` option (or short: `--jvm.cp=<classpath>`).

    node --jvm --jvm.cp=/my/class/path
    js --jvm --jvm.cp=/my/class/path

### Polyglot engine
The preferred method of launching Graal.js with Java interop support instance is via `Polyglot Engine`.
For that, a new `org.graalvm.polyglot.Context` is built with the `hostAccess` option set:
 
    Context context = Context.newBuilder("js").allowHostAccess(true).build();
    context.eval("js", jsSourceCode);

### ScriptEngine (JSR 223)
The Polyglot engine is the preferred execution method for interoperability with languages and tool of the GraalVM.
In addition, Graal.js is fully compatible with JSR 223 and supports the `ScriptEngine API`.
Internally, the Graal.js ScriptEngine wraps a Polyglot engine.

    ScriptEngine eng = new ScriptEngineManager().getEngineByName("graal.js");
    java.lang.Object fn = eng.eval("(function() { return this; })");
    Invocable inv = (Invocable) eng;
    java.lang.Object result = inv.invokeMethod(fn, "call", fn);

## Java interoperability features
Rhino, Nashorn and Graal.js provide a set of features to allow interoperability from `JavaScript` to `Java`.
While the overall feature-set is mostly comparable, the engines differ in exact syntax, and partly, semantics.

### Class access
To access a Java class, Graal.js supports the `Java.type(typeName)` function.

    var FileClass = Java.type('java.io.File');

Java classes are not automatically mapped to global variables, e.g., there is no `java` global property in Graal.js.
Existing code accessing e.g. `java.io.File` needs to be rewritten to use the `Java.type()` function.

    var FileClass = java.io.File;              //FAILS in Graal.js
    var FileClass = Java.type("java.io.File"); //Graal.js compliant syntax

Graal.js does provide a `Packages` global property.
Explicitly accessing the required class with `Java.type` should preferred whenever possible.

### Constructing Java objects
Java objects can be constructed with JavaScript's `new` keyword.

    var FileClass = Java.type('java.io.File');
    var file = new FileClass("myFile.md");

### Field and method access
Static fields of a Java class or fields of a Java object can be accessed like JavaScript properties.

    var JavaPI = Java.type('java.lang.Math').PI;

Java methods can be called like JavaScript functions.

    var file = new (Java.type('java.io.File'))("test.md");
    var fileName = file.getName(); 

#### Conversion of method arguments
JavaScript is defined to operate on the `double` number type.
Graal.js might internally use additional Java data types for performance reasons (e.g., the `int` type).

When calling Java methods, a value conversion might be required.
This happens when the Java method expects a `long` parameter, and a `int` is provided from Graal.js (`type widening`).
If this conversion caused a lossy conversion, a `TypeError` is thrown.

    //Java
    void longArg   (long arg1);
    void doubleArg (double arg2);
    void intArg    (int arg3);

    //JavaScript
    javaObject.longArg(1);     //widening, OK
    javaObject.doubleArg(1);   //widening, OK
    javaObject.intArg(1);      //match, OK

    javaObject.longArg(1.1);   //lossy conversion, TypeError!
    javaObject.doubleArg(1.1); //match, OK
    javaObject.intArg(1.1);    //lossy conversion, TypeError!

#### Selection of method
Java allows overloading of methods by argument types.
When calling from JavaScript to Java, the method with the narrowest available type that the actual argument can be converted to without loss is selected.

    //Java
    void foo(int arg);
    void foo(short arg);
    void foo(double arg);
    void foo(long arg);

    //JavaScript
    javaObject.foo(1);              //will call foo(short arg);
    javaObject.foo(Math.pow(2,16)); //will call foo(int arg);
    javaObject.foo(1.1);            //will call foo(double arg);
    javaObject.foo(Math.pow(2,32)); //will call foo(long arg);

Note that there currently is no way of overriding this behavior from Graal.js.
In the example above, one might want to always call `foo(int arg)`, even when `foo(short arg)` can be reached with lossless conversion (`foo(1)`).
Future versions of Graal.js might lift that restriction by providing an explicit way to select the method to be called.

### Package access
Package access is currently not supported in Graal.js.
There is no package abstraction provided.

This also means there is no `Packages` global property.

### Array access
Graal.js supports the creation of Java arrays from JavaScript code.
Both the patterns suggested by Rhino and Nashorn are supported:

    //Rhino pattern
    var JArray = Java.type('java.lang.reflect.Array');
    var JString = Java.type('java.lang.String');
    var sarr = JArray.newInstance(JString, 5);
    
    //Nashorn pattern
    var IntArray = Java.type("int[]");
    var iarr = new IntArray(5);

The arrays created are Java types, but can be used in JavaScript code:

    iarr[0] = iarr[iarr.length] * 2; 

### Map access
In Graal.js you can create and access Java Maps, e.g. `java.util.HashMap`.

    var HashMap = Java.type('java.util.HashMap');
    var map = new HashMap();
    map.put(1, "a");
    map.get(1);

Graal.js supports iterating over such map similar to Nashorn:

    for (var key in map) {
        print(key);
        print(map[key]);
    }

### List access
In Graal.js you can create and access Java Lists, e.g. `java.util.ArrayList`.

    var ArrayList = Java.type('java.util.ArrayList');
    var list = new ArrayList();
    list.add(42);
    list.add("23");
    list.add({});
    
    for (var idx in list) {
        print(idx);
        print(list.get(idx));
    }

### String access
Graal.js can create Java strings with Java interoperability.
The length of the string can be queried with the `length` property.
Note that `length` is a value property and cannot be called as a function.

    var javaString = new (Java.type('java.lang.String'))("Java");
    javaString.length === 4;

Note that Graal.js uses Java strings internally to represent JavaScript strings, so above code and the JavaScript string literal `"Java"` are actually not distinguishable.

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

### JavaImporter
The `JavaImporter` feature of Rhino is currently not supported by Graal.js

### Exceptions
Exceptions thrown in Java code can be caught in JavaScript code.
They are represented as Java objects.

    try {
        Java.type('java.lang.Class').forName("nonexistent");
    } catch (e) {
        print(e.getMessage());
    }

#### Conditional catch
Conditional catches of Java exceptions are currently not supported in Graal.js.
While conditional catches are supported in principle, this feature currently does not extend to Exceptions.
Future versions of Graal.js might lift that limitation.

## Rhino to Graal.js migration guide
Both Rhino and Graal.js support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migration are listed here.

### `Java.type(typename)` instead of `java.a.b.c.typename`
Graal.js does not put available Java classes in the JavaScript scope.
You have to explicitly load the classes using `Java.type(typename)`.
Graal.js supports the `Packages` global object, but loading the classes explicitly is still encouraged.

### Conditional catch of exceptions
Graal.js does not support conditional catch of exceptions.
You need to catch all exceptions in one `catch` and only then branch on the type of exceptions caught.

### Console output of Java classes and Java objects
Graal.js provides both `print` and `console.log`.

The `print` function is implemented by Graal.js.
It tries to special-case its behavior on Java classes and Java objects to provide the most useful output.

The `console.log` is provided by Node.js directly.
It does not provide special treatment of interop objects.

### JavaScript vs Java Strings
Similar to Nashorn, Graal.js uses Java strings internally to represent JavaScript strings.
This makes it impossible to differentiate whether a specific string was created by JavaScript or by Java code.


## Nashorn to Graal.js migration guide
Both Nashorn and Graal.js support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migration are listed here.

Items already covered in the Rhino to Graal.js migration guide above are not repeated, unless Nashorn exhibits behavior different to Rhino.

### Overloading Java functions and argument conversion
Graal.js does not allow lossy conversions of arguments when calling Java methods.
It will always select the overloaded method with the narrowest possible argument types that can be converted to without loss.
If no such overloaded method is available, Graal.js throws a `TypeError` instead of lossy conversion.
In general, this affects which overloaded method is executed.

Future versions of Graal.js might provide a method to specifically select with overloaded variant of the method should be selected.

### Java Strings have a `length` property, not a `length` function.
If you treat the `length` property of a Java String as a function, an error will be thrown: `TypeError: myJavaString.length is not a function`.
In Graal.js, you need to access `length` as a data property instead.

    myJavaString.length;   //FINE in Graal.js
    myJavaString.length(); // will trigger an Error in Graal.js



