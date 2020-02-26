GraalVM includes an ECMAScript compliant JavaScript engine.
It is designed to be fully standard compliant, execute applications with high performance, and provide all the benefits from the GraalVM stack, including language interoperability and common tooling.
With that engine, GraalVM can execute JavaScript and Node.js applications.

# Running JavaScript
GraalVM can execute plain JavaScript code:
```shell
js [options] [filename...] -- [args]
```
This example assumes you have `/path/to/graalvm/bin` on your `$PATH` so the `js` binary can be found.

# Running Node.js apps
GraalVM is adapted to run unmodified Node.js applications.
Applications can import npm modules, including native ones.

To run Node.js-based applications, use the `node` binary in the GraalVM distribution:

```
node [options] [filename] [args]
```

To install a Node.js module, use the `npm` executable.
The npm command is equivalent to the default Node.js command and supports all Node.js APIs.

1. Install the `colors` and `ansispan` modules using npm install as follows:

```
npm install colors ansispan
```

After the modules are installed, you can use them from your application.

2. Add the following code snippet to a file named `app.js` and save it in the same directory where you installed Node.js modules:

```
const http = require("http");
const span = require("ansispan");
require("colors");

http.createServer(function (request, response) {
    response.writeHead(200, {"Content-Type": "text/html"});
    response.end(span("Hello Graal.js!".green));
}).listen(8000, function() { console.log("Graal.js server running at http://127.0.0.1:8000/".red); });
```

3. Execute it on GraalVM using the node command as follows:

```
node app.js
```

# Interoperability
GraalVM supports several other programming languages, including Ruby, R, Python and LLVM.
While GraalVM is designed to run Node.js and JavaScript applications, it also provides an API for programming language interoperability that lets you execute code from any other language that GraalVM supports.

From JavaScript code you can access Java applications, as in the following example:

```
$ node --jvm
> var BigInteger = Java.type('java.math.BigInteger');
> console.log(BigInteger.valueOf(2).pow(100).toString(16));
10000000000000000000000000
```

You can also execute code written in other programming languages that GraalVM supports:

```
$ node --jvm --polyglot
> console.log(Polyglot.eval('R', 'runif(100)')[0]);
0.8198353068437427
```

# GraalVM JavaScript Compatibility
GraalVM is ECMAScript 2019 compliant and fully compatible with a diverse range of active Node.js (npm) modules.
It will also be compliant to ECMAScript 2020 once this updated specification is published (draft spec).
More than 100,000 npm packages are regularly tested and are compatible with GraalVM, including modules like `express`, `react`, `async`, `request`, `browserify`, `grunt`, `mocha`, and `underscore`.

# Nashorn Compatibility Mode
[GraalVM announced support for Nashorn migration](https://medium.com/graalvm/oracle-graalvm-announces-support-for-nashorn-migration-c04810d75c1f) due to the announced deprecation of Nashorn in the JDK.
GraalVM JavaScript provides a Nashorn compatibility mode for some of the functionality not exposed by default, but necessary to run an application that was built for Nashorn specifically.
[Check the list of extensions](https://github.com/graalvm/graaljs/blob/master/docs/user/NashornMigrationGuide.md#extensions-only-available-in-nashorn-compatibility-mode) to JavaScript (ECMAScript) that are available in GraalVM JavaScript.
Some of them provide features that are available in Nashorn as well.
For ECMAScript compatibility, most of those features are deactivated by default in GraalVM but can be activated with flags.
Note that using the Nashorn compatibility mode allows your application to bypass the GraalVM security model.

If the source code includes some Nashorn-specific extensions, the Nashorn compatibility mode should be enabled.
It can be activated:

1. with the `js.nashorn-compat` option from the command line:

```
js --experimental-options --js.nashorn-compat=true
```

2. by using the Polyglot API:

```
import org.graalvm.polyglot.Context;
try (Context context = Context.newBuilder().allowExperimentalOptions(true).option("js.nashorn-compat", "true").build()) {
  context.eval("js", "print(__LINE__)");
}
```

3. by using a system property when starting a Java application:

```
java -Dpolyglot.js.nashorn-compat=true MyApplication
```

We strongly encourage you to use the Nashorn compatibility mode only as a means of getting your application running on GraalVM JavaScript initially.
Enabling the Nashorn compatibility mode gives your application full access to the Java mode.
This bypasses the GraalVM security model of limiting JavaScript user code to “less trusted code” that is consistent with all other GraalVM languages.
It is highly recommended not to use the Nashorn compatibility mode in production code if you execute less trusted user code.
Some features of the Nashorn compatibility mode might in the future conflict with new ECMAScript and/or GraalVM JavaScript features.

We provide migration guides for code previously targeted to the [Nashorn](https://github.com/graalvm/graaljs/blob/master/docs/user/NashornMigrationGuide.md) or [Rhino](https://github.com/graalvm/graaljs/blob/master/docs/user/RhinoMigrationGuide.md) engines.
See the [JavaInterop.md](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaInterop.md) for an overview of supported Java interoperability features.
For additional information, see the [Polyglot Reference](https://www.graalvm.org/docs/reference-manual/polyglot/) and the [Embedding documentation](https://www.graalvm.org/docs/graalvm-as-a-platform/embed/) for more information about interoperability with other programming languages.

# Is GraalVM compatible with the JavaScript language?
_What version of ECMAScript do we support?_

GraalVM is compatible to the ECMAScript 2019 specification.
Some features of ECMAScript 2020 including some proposed features and extensions are available as well, but might not be fully implemented, compliant, or stable, yet.

_How do we know it?_

The compatibility of GraalVM JavaScript is verified by external sources, like the [Kangax ECMAScript compatibility table](https://kangax.github.io/compat-table/es6/).

On our CI system, we test GraalVM JavaScript against a set of test engines, like the official test suite of ECMAScript, [test262](https://github.com/tc39/test262), as well as tests published by V8 and Nashorn, Node.js unit tests, and GraalVM’s own unit tests.

For a reference of the JavaScript APIs that GraalVM supports, see [JavaScript compatibility](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaScriptCompatibility.md).

# Is GraalVM compatible with the original node implementation?
Node.js based on GraalVM is largely compatible with the original Node.js (based on the V8 engine).
This leads to a high number of npm-based modules being compatible with GraalVM (out of the 100k modules we test, more than 90% of them pass all tests).
Several sources of differences have to be considered.

* **Setup**: GraalVM mostly mimicks the original setup of Node, including the `node` executable, npm, and similar. However, not all command-line options are supported (or behave exactly identically), you need to (re-)compile native modules against our v8.h file, etc.

* **Internals**: GraalVM is implemented on top of a JVM, and thus has a different internal architecture. This implies that some internal mechanisms behave differently and cannot exactly replicate V8 behavior. This will hardly ever affect user code, but might affect modules implemented natively, depending on V8 internals.

* **Performance**: Due to GraalVM being implemented on top of a JVM, performance characteristics vary from the original native implementation. While GraalVM’s peak performance can match V8 on many benchmarks, it will typically take longer to reach the peak (known as _warmup_). Be sure to give the GraalVM compiler some extra time when measuring (peak) performance.

_How do we determine GraalVM’s JavaScript compatibility?_

GraalVM is compatible to ECMAScript 2019, guaranteeing compatibility on the language level. In addition, GraalVM uses the following approaches to check and retain compatibility to Node.js code:

* node-compat-table: GraalVM is compared against other engines using the `node-compat-table` module, highlighting incompatibilities that might break Node.js code.

* automated mass-testing of modules using mocha: in order to test a large set of modules, GraalVM is tested against 95k modules that use the mocha test framework. Using mocha allows automating the process of executing the test and comprehending the test result.

* manual testing of popular modules: a select list of npm modules is tested in a manual test setup. These highly-relevant modules are tested in a more sophisticated manner.

If you want your module to be tested by GraalVM in the future, ensure the module provides some mocha tests (and send us an email so we can ensure it is on the list of tested modules).

_How can one verify GraalVM works on their application?_

If your module ships with tests, execute them with GraalVM.
Of course, this will only test your app, but not its dependencies.
You can use the [compatibility checker](https://www.graalvm.org/docs/reference-manual/compatibility/) to find whether the module you’re interested in is tested on GraalVM, whether the tests pass successfully and so on.
Additionally, you can upload your `package-lock.json` or `package.json` file into that utility and it’ll analyze all your dependencies at once.

# GraalVM JavaScript Options
On the command line, `--js.<property>=<value>` sets options that tune language features and extensions.
The following options are currently supported:

* `--js.annex-b`: enables ECMAScript Annex B web compatibility features. Boolean value, default is `true`.
* `--js.array-sort-inherited`: defines whether `Array.protoype.sort` should sort inherited keys (implementation-defined behavior). Boolean value, default is `true`.
* `--js.atomics`: enables ES2017 Atomics. Boolean value, default is `true`.
* `--js.ecmascript-version`: emulates a specific ECMAScript version. Integer value (`5`-`9`), default is the latest version.
* `--js.intl-402`: enables ECMAScript Internationalization API. Boolean value, default is `false`.
* `--js.regexp-static-result`: provides static RegExp properties containing results of the last successful match, e.g.: `RegExp.$1` (legacy). Boolean value, default is `true`.
* `--js.shared-array-buffer`: enables ES2017 SharedArrayBuffer. Boolean value, default is `false`.
* `--js.strict`: enables strict mode for all scripts. Boolean value, default is `false`.
* `--js.timezone`: sets the local time zone. String value, default is the system default.
* `--js.v8-compat`: provides better compatibility with Google’s V8 engine. Boolean value, default is `false`.

Use `--help:languages` to see the full list of available options.

See the [Polyglot Reference](https://www.graalvm.org/docs/reference-manual/polyglot/) for information on how to set options programmatically when embedding.

# GraalVM Options
`--jvm` executes the application on the JVM instead of in the Native Image.

`--vm.<option>` passes VM options and system properties to the Native Image.
To pass JVM options to GraalVM you need to provide `--jvm` before, i.e., `--jvm --vm.<option>`.
List all available system properties to the Native Image, JVM and VM options and with `--help:vm`.

System properties can be set as follows: `--vm.D<name>=<value>`.
For example, `--vm.Dgraal.TraceTruffleCompilation=true` will print finished compilations.

`--compiler.<property>=<value>` passes settings to the compiler.
For example, `--compiler.CompilationThreshold=<Integer>` sets the minimum number of invocations or loop iterations before a function is compiled.

# Polyglot Options
`--polyglot` enables you to interoperate with other programming languages.

`--<languageID>.<property>=<value>` passes options to guest languages through the GraalVM Polyglot SDK.
Use `--help:languages` to find out which options are available.

# GraalVM JavaScript Operations Manual

_What’s the difference between running GraalVM’s JavaScript in a Native Image compared to the JVM?_

In essence, the JavaScript engine of GraalVM is a plain Java application.
Running it on any JVM (JDK 8 or higher) is possible; for best performance, it should be the GraalVM or a compatible JVMCI-enabled JDK using the GraalVM compiler.
This mode gives the JavaScript engine full access to Java at runtime, but also requires the JVM to first (just-in-time) compile the JavaScript engine when executed, just like any other Java application.

Running in a Native Image means that the JavaScript engine, including all its dependencies from, e.g., the JDK, is pre-compiled into a native binary.
This will tremendously speed up the startup of any JavaScript application, as GraalVM can immediately start to compile JavaScript code, without itself requiring to be compiled first.
This mode, however, will only give GraalVM access to Java classes known at the time of image creation.
Most significantly, this means that the JavaScript-to-Java interoperability features are not available in this mode, as they would require dynamic class loading and execution of arbitrary Java code at runtime.

_How to achieve the best peak performance?_

Optimizing JVM-based applications is a science in itself.
Here are a few tips and tricks you can follow to analyse and improve peak performance:

* When measuring, ensure you have given the GraalVM compiler enough time to compile all hot methods before starting to measure peak performance. A useful command line option for that is `--vm.Dgraal.TraceTruffleCompilation=true` – this outputs a message whenever a (JavaScript) method is compiled. As long as this still prints frequently, measurement should not yet start.
* Compare the performance between the Native Image and the JVM mode if possible. Depending on the characteristics of your application, one or the other might show better peak performance.

The Polyglot API comes with several tools and options to inspect the performance of your application:
* `--cpusampler` and `--cputracer` will print a list of the hottest methods when the application is terminated. Use that list to figure out where most time is spent in your application. More details about the command line options for the polyglot commands can be found from the [polyglot documentation](https://www.graalvm.org/docs/reference-manual/polyglot/#polyglot-options).
* `--experimental-options --memtracer` can help you understand the memory allocations of your application. Refer to [Profiling command line tools](https://www.graalvm.org/docs/reference-manual/tools/#profiler) reference for more detail.

