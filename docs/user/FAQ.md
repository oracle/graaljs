---
layout: docs
toc_group: js
link_title: FAQ
permalink: /reference-manual/js/FAQ/
---

# Frequently Asked Questions

Below are the most frequently asked questions and answers about JavaScript running on GraalVM.

## Compatibility

### Is GraalJS compatible with the JavaScript language?
GraalJS is compatible with the ECMAScript 2024 specification and is further developed alongside the 2025 draft specification.
The compatibility of GraalJS is verified by external sources, such as the [Kangax ECMAScript compatibility table](https://kangax.github.io/compat-table/es6/).

GraalJS is tested against a set of test engines, such as the official test suite of ECMAScript, [test262](https://github.com/tc39/test262), as well as tests published by V8 and Nashorn, Node.js unit tests, and GraalJS's own unit tests.

For reference documentation describing the JavaScript APIs that GraalVM supports, see [GRAAL.JS-API](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaScriptCompatibility.md).

### My application used to run on Nashorn, why does it not work on GraalJS?
Reason:
* GraalJS tries to be compatible with the ECMAScript specification, as well as competing engines (including Nashorn). In some cases, this is a contradicting requirement; in these cases, ECMAScript is given precedence. Also, there are cases where GraalJS does not exactly replicate Nashorn features intentionally, for example, for security reasons.

Solution:
* Enable GraalJS's Nashorn compatibility mode to add features not enabled by default&mdash;this should resolve most cases. However, note that this can have negative effects on application security! See the [Nashorn Migration Guide](NashornMigrationGuide.md) for details.

Specific applications:
* For JSR 223 ScriptEngine, you might want to set the system property `polyglot.js.nashorn-compat` to `true` in order to use the Nashorn compatibility mode.
* For `ant`, use the `ANT_OPTS` environment variable (`ANT_OPTS="-Dpolyglot.js.nashorn-compat=true"`) when using GraalJS via ScriptEngine.

### Why are built-in functions such as `array.map()` or `fn.apply()` not available on non-JavaScript objects such as `ProxyArray`s from Java?

Reason:
* Java objects provided to JavaScript are treated as closely as possible to their JavaScript counterparts. For example, Java arrays provided to JavaScript are treated like JavaScript _Array exotic objects_ (JavaScript arrays) whenever possible; the same is true for _functions_. One obvious difference is that such object's prototype is `null`. This means that while you can, for example, read the `length` or read and write the values of a Java array in JavaScript code, you cannot call `sort()` on it, as the `Array.prototype` is not provided by default.

Solution:
* While the objects do not have the methods of the prototype assigned, you can explicitly call them, for example, `Array.prototype.call.sort(myArray)`.
* We offer the option `js.foreign-object-prototype`. When enabled, objects on the JavaScript side get the most applicable prototype set (such as `Array.prototype`, `Function.prototype`, `Object.prototype`) and can thus behave more similarly to native JavaScript objects of the respective type. Normal JavaScript precedence rules apply here, for example, an object's own properties (of the Java object in that case) take precedence over and hide properties from the prototype.

Note that while the JavaScript built-in functions, for example, from `Array.prototype` can be called on the respective Java types, those functions expect JavaScript semantics.
This means that operations might fail (typically with a `TypeError`: `Message not supported`) when an operation is not supported in Java.
Consider `Array.prototype.push` as an example: arrays can grow in size in JavaScript, whereas they are fixed-size in Java, thus pushing a value is semantically not possible and will fail.
In such cases, you can wrap the Java object and handle that case explicitly.
Use the interfaces `ProxyObject` and `ProxyArray` for that purpose.

### How can I verify GraalJS works on my application?

If your module ships with tests, execute them with GraalJS. Of course, this will only test your application, not its dependencies.
You can use the [GraalVM Language Compatibility](https://www.graalvm.org/compatibility/) tool to discover if the module you are interested in is tested on GraalJS, and whether its tests pass successfully.
Additionally, you can upload your _package-lock.json_ or _package.json_ file into that tool and it will analyze all your dependencies.

## Performance

### Why is my application slower on GraalJS than on another engine?
Reason:
* Ensure your benchmark considers warmup. During the first few iterations, GraalJS may be slower than other engines, but after sufficient warmup, this difference should level out.
* GraalJS is shipped in two different standalones: Native (default) and JVM (with a `-jvm` infix). The default _Native_ mode offers faster startup and lower latency, but it might exhibit slower peak performance (lower throughput) once the application is warmed up. In _JVM_ mode, your application might need hundreds of milliseconds more to start, but typically exhibits better peak performance.
* Repeated execution of code via newly created `org.graalvm.polyglot.Context` is slow, despite the same code being executed every time.

Solution:
* Use proper warmup in your benchmark, and disregard the first few iterations where the application still warms up.
* When embedding GraalJS in a Java application, ensure you're running on a GraalVM JDK for best performance.
* Use a JVM standalone for slower startup, but higher peak performance.
* Double check you have no options set that might lower your performance, for example, `-ea`/`-esa`.
* When running code via `org.graalvm.polyglot.Context`, make sure that one `org.graalvm.polyglot.Engine` object is shared and passed to each newly created `Context`. Use `org.graalvm.polyglot.Source` objects and cache them when possible. Then, GraalVM shares existing compiled code across the Contexts, leading to improved performance. See [Code Caching Across Multiple Contexts](https://www.graalvm.org/latest/reference-manual/embed-languages/#code-caching-across-multiple-contexts) for more details and an example.
* Try to reduce the problem to its root cause and [file an issue](https://github.com/graalvm/graaljs/issues) so the GraalVM team can have a look.

### How can I achieve the best peak performance?
Here are a few tips you can follow to analyze and improve peak performance:

* When measuring, ensure you have given the Graal compiler enough time to compile all hot methods before starting to measure peak performance. A useful command line option for that is `--engine.TraceCompilation=true`&mdash;this outputs a message whenever a (JavaScript) method is compiled. Do not begin your measurement until this message becomes less frequent.
* Compare the performance between Native Image and JVM mode if possible. Depending on the characteristics of your application, one or the other might show better peak performance.
* The Polyglot API comes with several tools and options to inspect the performance of your application:
    * `--cpusampler` and `--cputracer` will print a list of the hottest methods when the application is terminated. Use that list to figure out where most time is spent in your application.
    * `--experimental-options --memtracer` can help you understand the memory allocations of your application. Refer to [Profiling Command Line Tool](https://github.com/oracle/graal/blob/master/docs/tools/profiling.md) for more detail.

### What is the difference between running GraalJS in Native Image compared to the JVM?
In essence, the GraalJS engine is a plain Java application.
Running it on any JVM (JDK 21 or later) is possible, but, for a better result, it should be a GraalVM JDK, or a compatible Oracle JDK using the Graal compiler.
This mode gives the JavaScript engine full access to Java at runtime, but also requires the JVM to first (just-in-time) compile the JavaScript engine when executed, just like any other Java application.

Running in Native Image means that the JavaScript engine, including all its dependencies from, for example, the JDK, is precompiled into a native executable.
This will tremendously reduce the startup of any JavaScript application, as GraalVM can immediately start to compile JavaScript code, without itself requiring to be compiled first.
This mode, however, will only give GraalVM access to Java classes known at the time of image creation.
Most significantly, this means that the JavaScript-to-Java interoperability features are not available in this mode, as they would require dynamic class loading and execution of arbitrary Java code at runtime.

## Errors

### TypeError: Access to host class com.myexample.MyClass is not allowed or does not exist
Reason:
* You are trying to access a Java class that is unknown to the `js` process, or is not among the allowed classes that your code can access.

Solution:
* Ensure there is no typo in the class name.
* Ensure the class is on the class path. Use the `--vm.cp=<classpath>` option.
* Ensure access to the class is permitted, by having a `@HostAccess.Export` annotation on your class and/or the `Context.Builder.allowHostAccess()` set to a permissive setting. See [org.graalvm.polyglot.Context](https://graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.html).

### TypeError: UnsupportedTypeException
```
TypeError: execute on JavaObject[Main$$Lambda$63/1898325501@1be2019a (Main$$Lambda$63/1898325501)] failed due to: UnsupportedTypeException
```

Reason:
* GraalJS in some cases does not allow concrete callback types when calling from JavaScript to Java. A Java function expecting, for example, a `Value` object, might fail with the quoted error message due to that.

Solution:
* Change the signature in the Java callback method.

Status:
* This is a [known limitation](https://github.com/graalvm/graaljs/issues/120) and should be resolved in future versions.

Example:
```java
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess;

public class Minified {
  public static void main(String ... args) {
    //change signature to Function<Object, String> to make it work
    Function<Value, String> javaCallback = (test) -> {
      return "passed";
    };
    try(Context ctx = Context.newBuilder()
    .allowHostAccess(HostAccess.ALL)
    .build()) {
      Value jsFn = ctx.eval("js", "f => function() { return f(arguments); }");
      Value javaFn = jsFn.execute(javaCallback);
      System.out.println("finished: "+javaFn.execute());
    }
  }
}
```

### TypeError: Message not supported
```
TypeError: execute on JavaObject[Main$$Lambda$62/953082513@4c60d6e9 (Main$$Lambda$62/953082513)] failed due to: Message not supported.
```

Reason:
* You are trying to execute an operation (a message) on a polyglot object that this object does not handle. For example, you are calling `Value.execute()` on a non-executable object.
* A security setting (for example, `org.graalvm.polyglot.HostAccess`) might prevent the operation.

Solution:
* Ensure the object (type) in question does handle the respective message.
* Specifically, ensure the JavaScript operation you try to execute on a Java type is possible semantically in Java. For example, while you can `push` a value to an array in JavaScript and thus automatically grow the array, arrays in Java are of fixed length and trying to push to a Java array will result in a `Message not supported` failure. You might want to wrap Java objects for such cases, for example, as a `ProxyArray`.
* Ensure access to the class is permitted, by having a `@HostAccess.Export` annotation on your class and/or the `Context.Builder.allowHostAccess()` set to a permissive setting. See [org.graalvm.polyglot.Context](https://graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.html).
* Are you trying to call a Java Lambda expression or Functional Interface? Annotating the proper method with a `@HostAccess.Export` annotation can be a pitfall. While you can annotate the method to which the functional interface refers, the interface itself (or the Lambda class created in the background) fails to be properly annotated and recognized as _exported_. See below for examples highlighting the problem and a working solution.

An example that triggers a `Message not supported` error with certain `HostAccess` settings, e.g., `HostAccess.EXPLICIT`:
```java
{
  ...
  //a JS function expecting a function as argument
  Value jsFn = ...;
  //called with a functional interface as argument
  jsFn.execute((Function<Integer, Integer>)this::javaFn);
  ...
}

@Export
public Object javaFn(Object x) { ... }

@Export
public Callable<Integer> lambda42 = () -> 42;
```

In the example above, the method `javaFn` is seemingly annotated with `@Export`, but the functional interface passed to `jsFn` is **not**, as the functional interface behaves like a wrapper around `javaFn`, thus hiding the annotation.
Neither is `lambda42` properly annotated&mdash;that pattern annotates the _field_ `lambda42`, nor its executable function in the generated lambda class.

In order to add the `@Export` annotation to a functional interface, use this pattern instead:
```java
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess;

public class FAQ {
  public static void main(String[] args) {
    try(Context ctx = Context.newBuilder()
    .allowHostAccess(HostAccess.EXPLICIT)
    .build()) {
      Value jsFn = ctx.eval("js", "f => function() { return f(arguments); }");
      Value javaFn = jsFn.execute(new MyExportedFunction());
      System.out.println("finished: " + javaFn.execute());
    }
  }

  @FunctionalInterface
  public static class MyExportedFunction implements Function<Object, String> {
    @Override
    @HostAccess.Export
    public String apply(Object s) {
      return "passed";
    }
  };
}
```

Another option is to allow access to `java.function.Function`'s `apply` method.
However, note that this allows access to _ALL_ instances of this interface&mdash;in most production environments, this will be too permissive and open potential security holes.
```java
HostAccess ha = HostAccess.newBuilder(HostAccess.EXPLICIT)
  //warning: too permissive for use in production
  .allowAccess(Function.class.getMethod("apply", Object.class))
  .build();
```

### Warning: Implementation does not support runtime compilation.

If you get the following warning, you are not running on GraalVM JDK, or a compatible Oracle JDK or OpenJDK using the Graal Compiler:
```
[engine] WARNING: The polyglot context is using an implementation that does not support runtime compilation.
The guest application code will therefore be executed in interpreted mode only.
Execution only in interpreted mode will strongly impair guest application performance.
To disable this warning, use the '--engine.WarnInterpreterOnly=false' option or the '-Dpolyglot.engine.WarnInterpreterOnly=false' system property.
```

To resolve this, use [GraalVM](https://www.graalvm.org/downloads/) or see how to [Run GraalJS on a Stock JDK guide](RunOnJDK.md) for instructions on how to set up the Graal compiler on a compatible Graal-enabled stock JDK.

Nevertheless, if this is intentional, you can disable the warning and continue to run with degraded performance by setting the above mentioned option, either via the command line or using the `Context.Builder`, for example:
```java
try (Context ctx = Context.newBuilder("js")
    .option("engine.WarnInterpreterOnly", "false")
    .build()) {
  ctx.eval("js", "console.log('Greetings!');");
}
```
Note that when using an explicit polyglot engine, the option has to be set on the `Engine`, for example:
```java
try (Engine engine = Engine.newBuilder()
    .option("engine.WarnInterpreterOnly", "false")
    .build()) {
  try (Context ctx = Context.newBuilder("js").engine(engine).build()) {
    ctx.eval("js", "console.log('Greetings!');");
  }
}
```