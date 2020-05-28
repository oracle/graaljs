# Frequently Asked Questions

This file lists frequently asked questions and answers around GraalVM JavaScript.

## Compatibility

### My application used to run on Nashorn, it does not work on GraalVM JavaScript

Reason:
* GraalVM JavaScript tries to be compatible to the ECMAScript specification, as well as competing engines (including Nashorn). In some cases, this is a contradicting requirement; then, ECMAScript is given precedence. Also, there are cases where GraalVM Javascript is not exactly replicating Nashorn features intentionally, e.g. for security reasons.

Solution:
* In many cases, enabling GraalVM's "Nashorn compatibility mode" enables features not enabled by default. Note that this can have negative effects on application security! See [NashornMigrationGuide.md](NashornMigrationGuide.md) for details.

Specific applications:
* For JSR 223 ScriptEngine, you might want to set the system property `polyglot.js.nashorn-compat` to `true` in order to use the Nashorn compatibility mode
* For `ant`, use `ANT_OPTS="-Dpolyglot.js.nashorn-compat=true" ant` when using GraalVM JavaScript via ScriptEngine

## Performance

### My application is slower on GraalVM JavaScript than on [another engine]

Reason:
* Ensure your benchmark considers warmup. During the first few iterations, GraalVM JavaScript will be slower than natively implemented engines, while on peak performance, this difference should level out.
* GraalVM JavaScript is shipped in two different modes: `native` (default) and `JVM`. While the default of `native` offers fast startup, it might show slower peak performance once the application is warmed up. In the `JVM` mode, the application might need a few hundred milliseconds more to start, but typically shows better peak performance.

Solution:
* Use proper warmup in your benchmark, and disregard the first few iterations where the application still warms up.
* Use the `--jvm` option for slower startup, but higher peak performance.
* Double-check you have no flags set that might lower your performance, e.g. `-ea`/`-esa`.
* Try to minify the problem to the root cause, and [file an issue](https://github.com/graalvm/graaljs/issues) so the GraalVM team can have a look.

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
* Ensure access to the class is permitted, by having `@HostAccess.Export` on your class and/or the `Context.Builder.allowHostAccess()` set to a permissive setting. See [JavaDoc of org.graalvm.polyglot.Context](https://graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.html).
