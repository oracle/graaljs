# GraalVM JavaScript Implementation

GraalVM provides the ECMAScript-compliant runtime to execute JavaScript
and Node.js applications. It is fully standard compliant, execute applications
with high performance, and provide all the benefits from the GraalVM stack,
including language interoperability and common tooling. This reference
documentation provides information on available JavaScript engine
configurations, the Node.js runtime, the `javax.script.ScriptEngine`
implementation, multithreading support details, possible embedding scenarios,
and more. To migrate the code previously targeted to the Nashorn or Rhino
engines, migration guides are available.

## Running JavaScript

GraalVM can run plain JavaScript code:
```shell
js [options] [filename...] -- [args]
```

## Running Node.js
GraalVM is adapted to run unmodified Node.js applications. Applications can
import npm modules, including native ones.

To run Node.js-based applications, use the `node` utility bundled with GraalVM by default:
```shell
node [options] [filename] [args]
```

To install a Node.js module, use the `npm` launcher from the GraalVM's `/bin` folder. The `npm` command is equivalent to the default Node.js command and supports all Node.js APIs.

1&#46; Install the `colors` and `ansispan` modules using `npm install` as
follows:
```shell
npm install colors ansispan
```
After the modules are installed, you can use them from your application.

2&#46; Add the following code snippet to a file named `app.js` and save it in the same directory where you installed the Node.js modules:

{% include snippet-highlight tabtype="javascript" path="js/app.js" %}

3&#46; Execute it on GraalVM using the `node` command as follows:
```shell
node app.js
```
For more information about running Node.js, continue to [Node.js Runtime](NodeJS.md).

## Interoperability

GraalVM supports several other programming languages like Ruby, R, Python, and
LLVM languages. While GraalVM is designed to run Node.js and JavaScript applications, it
also provides interoperability between those languages and lets you execute
code from or call methods in any of those languages using GraalVM Polyglot APIs.

To enable Node.js or JavaScript interoperability with other languages, pass the
`--jvm` and `--polyglot` options. For example:
```shell
node --jvm --polyglot
Welcome to Node.js v12.15.0.
Type ".help" for more information.
> var array = Polyglot.eval("python", "[1,2,42,4]")
> console.log(array[2]);
42
> console.log(Polyglot.eval('R', 'runif(100)')[0]);
0.8198353068437427
```

For more information about interoperability with other programming
languages, see [Polyglot Programming](https://www.graalvm.org/docs/reference-manual/polyglot-programming/).

## Interoperability with Java

To access Java from JavaScript, use `Java.type`, as in the following example:
```shell
node --jvm
> var BigInteger = Java.type('java.math.BigInteger');
> console.log(BigInteger.valueOf(2).pow(100).toString(16));
10000000000000000000000000
```

Vice versa, you can execute JavaScript from Java by embedding the JavaScript context in the Java program:
```java
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

public class HelloPolyglot {

    static String JS_CODE = "(function myFun(param){console.log('hello '+param);})";

    public static void main(String[] args) {
        System.out.println("Hello Java!");
        try (Context context = Context.create()) {
            Value value = context.eval("js", JS_CODE);
            value.execute(args[0]);
        }
    }
}
```
By wrapping the function definition (`()`), you return the function immediately.
The source code unit can be represented with a String, as in the example, a file, read from URL, and [other means](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html).

Run the above Java program:
```shell
javac HelloPolyglot.java
java HelloPolyglot JavaScript
```
This way you can evaluate JavaScript context embedded in Java, but you will not be able to
call a function and set parameters in the function directly from the Java code.
The Node.js runtime cannot be embedded into a JVM but has to be started as a separate process.

For example, save this code as _app.js_:
```js
var HelloPolyglot = Java.type("HelloPolyglot");

HelloPolyglot.main(["from node.js"]);

console.log("done");
```
Then start `node` with the `--jvm` option to enable interoperability with Java:
```shell
node --jvm --vm.cp=. app.js
Hello Java!
hello from node.js
done
```
By setting the classpath, you instruct `node` to start a JVM properly. Both Node.js and JVM then run in the same process and the interoperability works using the same `Value` classes as above.

Learn more about language interoperability in the [Java Interoperability](JavaInteroperability.md) guide.
