# Frequently Asked Questions

Find the most frequently asked questions and answers around GraalVM JavaScript.

## Compatibility

### Is GraalVM compatible with the JavaScript language?
GraalVM is compatible to the ECMAScript 2020 specification. The compatibility of GraalVM JavaScript is verified by external sources, like the [Kangax ECMAScript compatibility table](https://kangax.github.io/compat-table/es6/).

GraalVM JavaScript is tested against a set of test engines, like the official test suite of ECMAScript, [test262](https://github.com/tc39/test262), as well as tests published by V8 and Nashorn, Node.js unit tests and GraalVM's own unit tests.

For a reference of the JavaScript APIs that GraalVM supports, see [GRAAL.JS-API](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaScriptCompatibility.md).

### Is GraalVM compatible with the original node implementation?
Node.js based on GraalVM is largely compatible with the original Node.js (based on the V8 engine).
This leads to a high number of npm-based modules being compatible with GraalVM (out of the 95k modules we test, more than 90% of them pass all tests).
Several sources of differences have to be considered.

- **Setup:**
GraalVM mostly mimicks the original setup of Node, including the `node` executable, `npm` and similar. However, not all command-line options are supported (or behave exactly identically), you need to (re-)compile native modules against the v8.h file, etc..

- **Internals:**
GraalVM is implemented on top of a JVM, and thus has a different internal architecture. This implies that some internal mechanisms behave differently and cannot exactly replicate V8 behaviour. This will hardly ever affect user code, but might affect modules implemented natively, depending on V8 internals.

- **Performance:**
Due to GraalVM being implemented on top of a JVM, performance characteristics vary from the original native implementation. While GraalVM's peak performance can match V8 on many benchmarks, it will typically take longer to reach the peak (known as _warmup_). Be sure to give the GraalVM compiler some extra time when measuring (peak) performance.

In addition, GraalVM uses the following approaches to check and retain compatibility to Node.js code:

* node-compat-table: GraalVM is compared against other engines using the _node-compat-table_ module, highlighting incompatibilities that might break Node.js code.
* automated mass-testing of modules using _mocha_: in order to test a large set of modules, GraalVM is tested against 95k modules that use the mocha test framework. Using mocha allows automating the process of executing the test and comprehending the test result.
* manual testing of popular modules: a select list of npm modules is tested in a manual test setup. These highly-relevant modules are tested in a more sophisticated manner.

### My application used to run on Nashorn, it does not work on GraalVM JavaScript
Reason:
* GraalVM JavaScript tries to be compatible to the ECMAScript specification, as well as competing engines (including Nashorn). In some cases, this is a contradicting requirement; then, ECMAScript is given precedence. Also, there are cases where GraalVM Javascript is not exactly replicating Nashorn features intentionally, e.g. for security reasons.

Solution:
* In many cases, enabling GraalVM's "Nashorn compatibility mode" enables features not enabled by default. Note that this can have negative effects on application security! See [NashornMigrationGuide.md](NashornMigrationGuide.md) for details.

Specific applications:
* For JSR 223 ScriptEngine, you might want to set the system property `polyglot.js.nashorn-compat` to `true` in order to use the Nashorn compatibility mode
* For `ant`, use `ANT_OPTS="-Dpolyglot.js.nashorn-compat=true" ant` when using GraalVM JavaScript via ScriptEngine

### Builtin functions like `array.map()` or `fn.apply()` are not available on non-JavaScript objects like `ProxyArray`s from Java.

Reason:
* Java objects provided to JavaScript are treated as close as possible to their JavaScript counterpart. For instance, Java arrays provided to JavaScript are treated like JavaScript _Array exotic objects_ (JavaScript arrays) whenever possible; the same is true for _functions_. One obvious difference is that such object's prototype is `null`. This means that while you can e.g. read the `length` or read and write the values of a Java array in JavaScript code, you cannot call `sort()` on it, as the `Array.prototype` is not provided by default.

Solution:
* While the objects don't have the methods of the prototype assigned, you can explicitly call them, e.g. `Array.prototype.call.sort(myArray)`.
* We offer the experimental option `js.experimental-foreign-object-prototype`. When enabled, objects on the JavaScript side get the most prototype (e.g. `Array.prototype`, `Function.prototype`, `Object.prototype`) set and can thus behave more similarly to native JavaScript objects of the respective type. Normal JavaScript precedence rules apply here, e.g. own properties (of the Java object in that case) take precedence over and hide properties from the prototype.

Note that the JavaScript builtin functions e.g., from `Array.prototype` can be called on the respective Java types, those functions expect JavaScript semantics.
This for instance means that operations might fail (typically with a `TypeError`: `Message not supported`) when an operation is not supported in Java.
Consider `Array.prototype.push` as an example: while arrays can grow in size in JavaScript, they are fixed-size in Java, thus pushing a value is semantically not possible and will fail.
In such cases, you can wrap the Java object and handle that case explicitly.
Use the interfaces `ProxyObject` and `ProxyArray` for that purpose.

### How can one verify GraalVM works on their application?
If your module ships with tests, execute them with GraalVM. Of course, this will
only test your application, but not its dependencies. You can use the
[Compatibility](https://www.graalvm.org/docs/reference-manual/compatibility/)
tool to find whether the module you are interested in is tested on GraalVM,
whether the tests pass successfully. Additionally, you can upload your
`package-lock.json` or `package.json` file into that tool and it will analyze
all your dependencies at once.

## Performance

### My application is slower on GraalVM JavaScript than on another engine
Reason:
* Ensure your benchmark considers warmup. During the first few iterations, GraalVM JavaScript will be slower than natively implemented engines, while on peak performance, this difference should level out.
* GraalVM JavaScript is shipped in two different modes: `native` (default) and `JVM`. While the default of `native` offers fast startup, it might show slower peak performance once the application is warmed up. In the `JVM` mode, the application might need a few hundred milliseconds more to start, but typically shows better peak performance.

Solution:
* Use proper warmup in your benchmark, and disregard the first few iterations where the application still warms up.
* Use the `--jvm` option for slower startup, but higher peak performance.
* Double-check you have no flags set that might lower your performance, e.g. `-ea`/`-esa`.
* Try to minify the problem to the root cause and [file an issue](https://github.com/graalvm/graaljs/issues) so the GraalVM team can have a look.

### How to achieve the best peak performance?
Here are a few tips you can follow to analyse and improve peak performance:

* When measuring, ensure you have given the GraalVM compiler enough time to compile all hot methods before starting to measure peak performance. A useful command line option for that is `--vm.Dgraal.TraceTruffleCompilation=true` -- this outputs a message whenever a (JavaScript) method is compiled. As long as this still prints frequently, measurement should not yet start.
* Compare the performance between the Native Image and the JVM mode if possible. Depending on the characteristics of your application, one or the other might show better peak performance.
* The Polyglot API comes with several tools and options to inspect the performance of your application:
    * `--cpusampler` and `--cputracer` will print a list of the hottest methods when the application is terminated. Use that list to figure out where most time is spent in your application.
    * `--experimental-options --memtracer` can help you understand the memory allocations of your application. Refer to the [Profiling Command Line Tool](https://www.graalvm.org/docs/tools/profiler) reference for more detail.

### What is the difference between running GraalVM's JavaScript in a Native Image compared to the JVM?_
In essence, the JavaScript engine of GraalVM is a plain Java application.
Running it on any JVM (JDK 8 or higher) is possible, but, for a better result, it should be GraalVM or a compatible JVMCI-enabled JDK using the GraalVM compiler.
This mode gives the JavaScript engine full access to Java at runtime, but also requires the JVM to first (just-in-time) compile the JavaScript engine when executed, just like any other Java application.

Running in a Native Image means that the JavaScript engine, including all its dependencies from, e.g., the JDK, is pre-compiled into a native binary. This will tremendously speed up the startup of any JavaScript application, as GraalVM can immediately start to compile JavaScript code, without itself requiring to be compiled first.
This mode, however, will only give GraalVM access to Java classes known at the time of image creation.
Most significantly, this means that the JavaScript-to-Java interoperability features are not available in this mode, as they would require dynamic class loading and execution of arbitrary Java code at runtime.

## Errors

### TypeError: Access to host class com.myexample.MyClass is not allowed or does not exist
Reason:
* You are trying to access a Java class that is not known to the `js` or `node` process, or is not among the allowed classes your code can access.

Solution:
* Ensure there is no typo in the class name.
* Ensure the class is on the classpath. Use the `--vm.cp=<classpath>` option of the launchers.
* Ensure access to the class is permitted, by having `@HostAccess.Export` on your class and/or the `Context.Builder.allowHostAccess()` set to a permissive setting. See [JavaDoc of org.graalvm.polyglot.Context](https://graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.html).

### TypeError: UnsupportedTypeException
```
TypeError: execute on JavaObject[Main$$Lambda$63/1898325501@1be2019a (Main$$Lambda$63/1898325501)] failed due to: UnsupportedTypeException
```

Reason:
* GraalVM JavaScript in some cases does not allow concrete callback types when calling from JavaScript to Java. A Java function expecting e.g. a `Value` object might fail with the quoted error message due to that.

Solution:
* Change the signature in the Java callback method

Status:
* This is a [known limitation](https://github.com/graalvm/graaljs/issues/120) and should be resolved in future versions

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
    try(Context ctx = Context.newBuilder().allowHostAccess(HostAccess.ALL).build()) {
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
* you are trying to execute an operation (a message) on a polyglot object that this object does now handle. E.g., you are calling `Value.execute()` on a non-executable object.

Solution:
* Ensure the object (type) in question does handle the respective message.
* Specifically, ensure the JavaScript operation you try to execute on a Java type is possible semantically in Java. For instance, while you can `push` a value to an array in JavaScript and thus automatically grow the array, arrays in Java are of fixed length and trying to push to them will result in a `Message not supported` failure. You might want to wrap Java objects for such cases, e.g. as a `ProxyArray`.
* Ensure access to the class is permitted, by having `@HostAccess.Export` on your class and/or the `Context.Builder.allowHostAccess()` set to a permissive setting. See [JavaDoc of org.graalvm.polyglot.Context](https://graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.html).
