---
layout: docs
toc_group: js
link_title: Node.js Runtime
permalink: /reference-manual/js/NodeJS/
---

# Node.js Runtime

GraalVM can run unmodified Node.js applications. 
GraalVM's Node.js runtime is based on a recent version of Node.js, and runs the GraalVM JavaScript engine (GraalJS) instead of Google V8. 
Some internal features (for example, VM-internal statistics, configuration, profiling, debugging, and so on) are unsupported, or supported with potentially different behavior.

Applications can freely import and use NPM packages, including native ones.

## Getting Started with Node.js

As of GraalVM for JDK 21, the GraalVM Node.js runtime is available as a separate distribution.
Two standalone runtime options are available for both Oracle GraalVM and GraalVM Community Edition: a Native Image compiled launcher or a JVM-based runtime.
To distinguish between them, the GraalVM Community Edition version has the suffix `-community` in the name: `graaljs-community-<version>-<os>-<arch>.tar.gz`, `graalnodejs-community-<version>-<os>-<arch>.tar.gz`.
A standalone that comes with a JVM has a `-jvm` suffix in a name.

To enable the GraalVM Node.js runtime, install the Node.js distribution based on Oracle GraalVM or GraalVM Community Edition for your operating system.

1. Navigate to [GitHub releases](https://github.com/oracle/graaljs/releases/) and select a desired standalone for your operating system.

2. Unzip the archive:
    ```shell
    tar -xzf <archive>.tar.gz
    ```
    Alternatively, open the file in the Finder.

3. Check the version to see if the runtime is active:
    ```shell
    ./path/to/bin/node --version
    ```

## Running Node.js Applications

The Node.js installation provides `node` and `npm` launchers:
```shell
node [options] [filename] [args]
```

The `npm` command is equivalent to the default Node.js command, and features additional GraalVM-specific functionalities (for example, interoperability with Java). 
A list of available options can be obtained with `node --help`.

Use the `node` launcher to execute a Node.js application. For example:

1. Install the `colors` and `ansispan` packages using `npm install` as follows:
    ```shell
    npm install colors ansispan
    ```
    After the packages are installed, you can use them from your application.

2. Add the following code snippet to a file named _app.js_ and save it in the same directory where you installed the Node.js packages:
    ```js
    const http = require("http");
    const span = require("ansispan");
    require("colors");

    http.createServer(function (request, response) {
        response.writeHead(200, {"Content-Type": "text/html"});
        response.end(span("Hello Node.js!".green));
    }).listen(8000, function() { console.log("Node.js server running at http://127.0.0.1:8000/".red); });

    setTimeout(function() { console.log("DONE!"); process.exit(); }, 2000);
    ```

3. Execute it on the GraalVM Node.js runtime using the `node` command as follows:
    ```shell
    node app.js
    ```

The Node.js functionality is available when an application is started from the `node` binary launcher.
Certain limits apply when launching a Node.js application or accessing NPM packages from a Java context, see [Node.js vs. Java Script Context](NodeJSVSJavaScriptContext.md).

## Installing Packages Using `npm`

To install a Node.js package, use the `npm` launcher.
The `npm` command is equivalent to the default NPM command, and supports most of its options.

An NPM package can be installed with:
```shell
npm install [package]
```

As the `npm` command of GraalVM Node.js is largely compatible with NPM, packages are installed in the _node\_modules/_ directory, as expected.

## Installing `npm` Packages Globally

Node packages can be installed globally using `npm` and the `-g` option.
By default, `npm` installs global packages (links to their executables) in the path where the `node` executable is installed, typically _node/bin/_.
That directory is where global packages are installed.
You might want to add that directory to your `$PATH` if you regularly use globally installed packages, especially their command line interfaces.

Another option is to specify the global installation directory of `npm` by setting the `$PREFIX` environment variable, or by specifying the `--prefix` option when running `npm install`.
For example, the following command will install global packages in the _/foo/bar/_ directory:
```shell
npm install --prefix /foo/bar -g <package>
```
More details about `prefix` can be found in the [official NPM documentation](https://docs.npmjs.com/cli/prefix.html).

## Interoperability with Java

The Node.js runtime cannot be embedded into a JVM but has to be started as a separate process.

1. Save the following code in a file named _HelloPolyglot.java_ and compile:
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

2. Then save this code a file named _app.js_:
    ```js
    var HelloPolyglot = Java.type("HelloPolyglot");

    HelloPolyglot.main(["from node.js"]);

    console.log("done");
    ```

3. Run it with `node`:
    ```shell
    node --vm.cp=. app.js
    ```
    You should see the following output:
    ```
    Hello Java!
    hello from node.js
    done
    ```

Both Node.js and JVM then run in the same process and the interoperability works using the same `Value` classes as above.

For the differences between running the `node` launcher and accessing Node.js NPM modules or ECMAScript modules from a Java `Context`, see [NodeJSVSJavaScriptContext](NodeJSVSJavaScriptContext.md).

## Multithreading with Node.js

The basic [multithreading model of GraalJS](Multithreading.md) applies to Node.js applications as well.
In Node.js, a [Worker](https://nodejs.org/api/worker_threads.html#worker_threads_worker_threads) thread can be created to execute JavaScript code in parallel, but JavaScript objects cannot be shared between Workers.
On the contrary, a Java object created with GraalVM Java interoperability (for example, using `Java.type()`) can be shared between Node.js Workers.
This allows multithreaded Node.js applications to share Java objects.

The GraalVM Node.js [unit tests](https://github.com/graalvm/graaljs/tree/master/graal-nodejs/test/graal/unit) contain several examples of multithreaded Node.js applications.
The most notable examples show how:
1. [Node.js worker threads can execute Java code](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/worker.js).
2. [Java objects can be shared between Node.js worker threads](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/javaMessages.js).
3. [JavaScript `Promise` objects can be used to `await` on messages from workers, using Java objects to bind promises to worker messages](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/workerInteropPromises.js).

## Frequently Asked Questions

### Is GraalVM's Node.js runtime compatible with the original Node implementation?
GraalVM's Node.js runtime is largely compatible with the original Node.js (based on the V8 engine).
This leads to a high number of `npm`-based modules being compatible.
In fact, out of the 100k `npm` modules we test, more than 94% of them pass all tests.
Still, several sources of differences have to be considered:

- **Setup:**
GraalVM's Node.js mostly mimicks the original setup of Node, including the `node` executable, `npm`, and similar.
However, not all command-line options are supported (or behave exactly identically).
Modules might require that native modules are (re)compiled against the _v8.h_ file.

    As of GraalVM for JDK 21, the GraalVM Node.js runtime is available as a separate distribution. 
    See [Getting Started with Node.js](#getting-started-with-nodejs).

- **Internals:**
GraalVM's Node.js is implemented on top of a JVM, and thus has a different internal architecture than Node.js based on V8.
This implies that some internal mechanisms behave differently and cannot exactly replicate V8 behavior.
This will hardly ever affect user code, but might affect modules implemented natively, depending on V8 internals.

- **Performance:**
Due to GraalVM's Node.js being implemented on top of a JVM, performance characteristics vary from the original native implementation.
While GraalVM's peak performance can match V8 on many benchmarks, it will typically take longer to reach the peak (known as _warmup_).
Be sure to give the Graal compiler some extra time when measuring (peak) performance.

- **Compatibility:**
GraalVM's Node.js runtime uses the following approaches to check and retain compatibility with Node.js code:
    * node-compat-table: GraalVM's Node.js is compared against other engines using the _node-compat-table_ module, highlighting incompatibilities that might break Node.js code.
    * automated mass-testing of modules using _mocha_: in order to test a large set of modules, GraalVM's Node.js runtime is tested against 95k modules that use the mocha test framework. Using mocha allows automating the process of executing the test and comprehending the test result.
    * manual testing of popular modules: a select list of `npm` modules is tested in a manual test setup. These highly-relevant modules are tested in a more sophisticated manner.

### Can NPM packages be installed globally?
Node packages can be installed globally using `npm` and the `-g` option, both with the GraalVM's Node.js implementation.

While the original Node.js implementation has one main directory (_node/bin/_) to put binaries and globally installed packages and their command-line tools, GraalVM's Node.js puts binaries in the _/path/to/graaljs/bin/_ directory.
When installing NPM packages globally on the GraalVM Node.js runtime, links to the executables, for example, for command line interface tools are put to the JavaScript-specific directory.
In order for globally installed packages to function properly, you might need to add `/path/to/graaljs/bin` to your `$PATH`.

Another option is to specify the global installation directory of `npm` by setting the `$PREFIX` environment variable, or by specifying the `--prefix` option when running `npm install`.

For more details, see [Installing `npm` Packages Globally](NodeJS.md#installing-npm-packages-globally).

### Related Documentation

* [Differences Between `node` Native Launcher and a Java `Context`](NodeJSVSJavaScriptContext.md)

