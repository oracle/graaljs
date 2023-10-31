---
layout: docs
toc_group: js
link_title: JavaScript and Node.js Reference
permalink: /reference-manual/js/
---
# GraalVM JavaScript and Node.js Runtime

GraalVM provides an ECMAScript-compliant runtime to execute JavaScript and Node.js applications.
It is fully standard compliant, executes applications with high performance, and provides all benefits from the GraalVM stack, including language interoperability and common tooling.
This reference documentation provides information on available JavaScript engine configurations, the Node.js runtime, the [`ScriptEngine` implementation](ScriptEngine.md), multithreading support details, possible embedding scenarios, and more.
To migrate the code previously targeted to the Nashorn or Rhino engines, [migration guides are available](NashornMigrationGuide.md).

## Getting Started

As of GraalVM for JDK 21, the JavaScript (GraalJS) and Node.js runtimes are available as standalone distributions. 
Two standalone language runtime options are available for both Oracle GraalVM and GraalVM Community Edition: a Native Image compiled native launcher or a JVM-based runtime (included).
To distinguish between them, the GraalVM Community Edition version has the suffix `-community` in the name: `graaljs-community-<version>-<os>-<arch>.tar.gz`, `graalnodejs-community-<version>-<os>-<arch>.tar.gz`, 
A standalone that comes with a JVM has a `-jvm` suffix in a name.

1. Navigate to [GitHub releases](https://github.com/oracle/graaljs/releases/) and select a desired standalone for your operating system. 

2. Unzip the archive:

    > Note: If you are using macOS Catalina and later, first remove the quarantine attribute:
    ```shell
    sudo xattr -r -d com.apple.quarantine <archive>.tar.gz
    ```
    Unzip:
    ```shell
    tar -xzf <archive>.tar.gz
    ```
    Alternatively, open the file in the Finder.

3. Check the version to see if the runtime is active:
    ```shell
    ./path/to/bin/js --version
    ```
    ```shell
    ./path/to/bin/node --version
    ```

## Running JavaScript

Use the `js` launcher to run plain JavaScript (ECMAScript) code:
```shell
js [options] [filename...] -- [args]
```

## Running Node.js

The Node.js standalone provides `node` and `npm` launchers.

Use the `node` utility to execute Node.js applications:
```shell
node [options] [filename] [args]
```
The `npm` command is equivalent to the default Node.js command and supports all Node.js APIs.

1. Install the `colors` and `ansispan` packages using `npm install` as follows:
    ```shell
    npm install colors ansispan
    ```
    After the packages are installed, you can use them from your application.

2. Add the following code snippet to a file named `app.js` and save it in the same directory where you installed the Node.js packages:
    ```js
    const http = require("http");
    const span = require("ansispan");
    require("colors");

    http.createServer(function (request, response) {
        response.writeHead(200, {"Content-Type": "text/html"});
        response.end(span("Hello Graal.js!".green));
    }).listen(8000, function() { console.log("Graal.js server running at http://127.0.0.1:8000/".red); });

    setTimeout(function() { console.log("DONE!"); process.exit(); }, 2000);
    ```

3. Execute it on GraalVM using the `node` command as follows:
    ```shell
    node app.js
    ```

For more information about running Node.js, go to [Node.js Runtime](NodeJS.md).
The Node.js functionality is available when an application is started from the `node` binary launcher.
Certain limits apply when launching a Node.js application or accessing NPM packages from a Java context, see [Node.js vs. Java Script Context](NodeJSVSJavaScriptContext.md).

## Interoperability with Java

To embed JavaScript in a Java host application, enable JavaScript by adding it as a project dependency.
Below is the Maven configuration for a JavaScript embedding:
```xml
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>${graalvm.version}</version>
</dependency>
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js</artifactId>
    <version>${graalvm.version}</version>
    <type>pom</type>
</dependency>
```
It enables the Oracle GraalVM JavaScript runtime by default.
Use `js-community` if you need the artifact built on top of GraalVM Community Edition.

To access Java from JavaScript, use `Java.type`, as in the following example:
```shell
node
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

This way you can evaluate JavaScript context embedded in Java, but you will not be able to call a function and set parameters in the function directly from the Java code.

The Node.js runtime cannot be embedded into a JVM but has to be started as a separate process.

For example, save this code as _app.js_:
```js
var HelloPolyglot = Java.type("HelloPolyglot");

HelloPolyglot.main(["from node.js"]);

console.log("done");
```

Then run it:
```shell
node --vm.cp=. app.js
Hello Java!
hello from node.js
done
```

Both Node.js and JVM then run in the same process and the interoperability works using the same `Value` classes as above.

Learn more about language interoperability in the [Java Interoperability](JavaInteroperability.md) guide.

## Further documentation

For additional information, see the following documentation.

Using GraalVM JavaScript:
* [JavaScript Compatibility](JavaScriptCompatibility.md)
* [Options and Flags to the Engine](Options.md)
* [Multithreading Support](Multithreading.md)
* [Java Interoperability](JavaInteroperability.md)
* [Execute GraalVM JavaScript on a Stock JDK](RunOnJDK.md)

Legacy environments:
* [Migration Guide from Nashorn](NashornMigrationGuide.md)
* [Migration Guide from Rhino](RhinoMigrationGuide.md)
* [Work with a javax.script.ScriptEngine](ScriptEngine.md)

Node.js support:
* [Node.js Support](NodeJS.md)
* [Differences between node's native launcher and a Java Context](NodeJSVSJavaScriptContext.md)
