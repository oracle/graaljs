# Differences Between Node.js and Java Embeddings

GraalVM's JavaScript engine is a fully-compliant ECMA2020 language runtime.
As such, it can run JavaScript code in a variety of embedding scenarios, including the Oracle [RDBMS](https://www.graalvm.org/docs/examples/mle-oracle/), any Java-based application and Node.js.

Depending on the GraalVM's JavaScript embedding scenario, applications have access to different built-in capabilities.
For example, Node.js applications running on GraalVM's JavaScript engine have access to all of Node.js' APIs, including built-in Node.js' modules such as `'fs'`, `'http'`, etc.
Conversely, JavaScript code embedded in a Java application has access to limited capabilities, as specified through the [Context API](https://www.graalvm.org/reference-manual/embed-languages/#compile-and-run-a-polyglot-application), and do _not_ have access to Node.js built-in modules.

This guide describes the main differences between a Node.js application and a GraalVM JavaScript application embedded in Java.

## Context Creation

JavaScript code in GraalVM can be executed using a GraalVM execution _Context_.

In a Java application, a new context can be created using the [`Context` API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html).
New contexts can be configured in multiple ways, and configuration options include exposing access to Java classes, allowing access to IO, etc.
A list of context creation options can be found in the [API documentation](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html).
In this scenario, Java classes can be exposed to JavaScript by using GraalVM's [polyglot `Bindings`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#getPolyglotBindings--).

In a Node.js application, the GraalVM `Context` executing the application is pre-initialized by the Node.js runtime, and cannot be configured by the user application.
In this scenario, Java classes can be exposed to the Node.js application by using the `--vm.cp=` command line option of the `bin/node` command, as described below.


## Java Interoperability

JavaScript applications can interact with Java classes using the `Java` built-in object.
The object is not available by default, and can be enabled in the following way:

1. In Node.js, start GraalVM using the `bin/node --jvm` command
2. In Java, create a GraalVM context using the `withHostInterop()` option, e.g.:
```
Context.create("js").withHostInterop()
```
More details on the Java interoperability capabilities of GraalVM JavaScript are available in the [Java Interoperability](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaInteroperability.md) guide.

## Multithreading

A GraalVM context running JavaScript enforces a "share-nothing" model of parallelism: no JavaScript values can be accessed by two concurrent Java threads at the same time.
In order to leverage parallel execution, multiple contexts have to be created and executed from multiple threads.

1. In Node.js, multiple contexts can be created using Node.js' [Worker threads](https://nodejs.org/api/worker_threads.html) API.
The worker threads API ensures that no sharing can happen between two parallel contexts.
2. In Java, multiple contexts can be executed from multiple threads.
As long as a context is not accessed by two threads at the same time, parallel execution happens safely.

More details on parallel execution in GraalVM JavaScript are available in [this blog post](https://medium.com/graalvm/multi-threaded-java-javascript-language-interoperability-in-graalvm-2f19c1f9c37b).


## Java Libraries

Java libraries can be accessed from JavaScript in GraalVM through the `Java` built-in object.
In order for a Java library to be accessible from a `Context`, its `jar` files need to be added to the GraalVM class path.
This can be done in the following way:

1. In Node.js, the classpath can be modified using the `--jvm.cp` option.
2. In Java, the default Java's `-cp` option can be used.

More details on GraalVM command line options are available in the [Options](https://github.com/graalvm/graaljs/blob/master/docs/user/Options.md) guide.

## JavaScript Modules

Many popular JavaScript modules such as those available on the `npm` package registry can be used from Node.js as well as from Java.
GraalVM JavaScript is compatible with the latest ECMA standard, and supports ECMAScript modules (ECM).
CommonJS (CJS) modules are supported when running with Node.js.
CommonJS modules cannot be used _directly_ from Java; to this end, any popular package bundlers (such as Parcel, Browserify or Webpack) can be used.

### ECMAScript Modules (ECM)

ECMAScript modules can be loaded in GraalVM JavaScript in the following ways:

1. The support of ECMAScript modules in Node.js is still experimental.
GraalVM  supports all features supported by the Node.js version that is compatible with GraalVM.
To check such version, simply run `bin/node --version`.
2. ECMAScript modules can be loaded in a `Context` simply by evaluating the module sources.
Currently, GraalVM JavaScript loads ECMAScript modules based on their file extension.
Therefore, any ECMAScript module must have file name extension `.mjs`.
This might change in future versions of GraalVM JavaScript.

More details about evaluating files using the Context API are available in the [API Javadoc](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html).

### CommonJS Modules (CJS)

CommonJS modules can be loaded in GraalVM JavaScript in the following way:

1. In Node.js, modules can be loaded using the `require()` built-in function, as expected.
2. By default, the `Context` API does not support CommonJS modules, and has no built-in `require()` function.
In order to be loaded and used from a `Context` in Java, a CJS module needs to be _bundled_ into a self-contained JavaScript source file.
This can be done using one of the many popular open-source bundling tools such as Parcel, Browserify and Webpack.
Experimental support for CommonJS modules can be enabled through the `js.commonjs-require` option as described below.

#### Experimental support for CommonJS NPM modules in the `Context` API

The `js.commonjs-require` option provides a built-in `require()` function that can be used to load NPM-compatible CommonJS modules in a JavaScript `Context`.
Currently, this is an experimental feature not for production usage.

To enable CommonJS support, a JavaScript context can be created in the following way:
```
Map<String, String> options = new HashMap<>();
// Enable CommonJS experimental support.
options.put("js.commonjs-require", "true");
// (optional) folder where the NPM modules to be loaded are located.
options.put("js.commonjs-require-cwd", "/path/to/root/folder");
// (optional) initialization script to pre-define globals.
options.put("js.commonjs-global-properties", "./globals.js");
// (optional) Node.js built-in replacements as a comma separated list.
options.put("js.commonjs-core-modules-replacements",
            "buffer:buffer/," +
            "path:path-browserify");
// Create context with IO support and experimental options.
Context cx = Context.newBuilder("js")
                            .allowExperimentalOptions(true)
                            .allowIO(true)
                            .options(options)
                            .build();
// Require a module
Value module = cx.eval("js", "require('some-module');");
```

##### Differences with Node.js built-in `require()` function

The `Context` built-in `require()` function can load regular NPM modules implemented in JavaScript, but cannot load _native_ NPM modules.
The built-in `require()` relies on the [Truffle FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html), therefore I/O access needs to be enabled at context creation time using the [`allowIO` option](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.Builder.html#allowIO-boolean-).
The built-in `require()` aims to be largely compatible with Node.js, and we expect it to work with any NPM module that would work in a browser (e.g., created using a package bundler).

##### Installing an NPM module to be used via the `Context` API

In order to be used from a JavaScript `Context`, an NPM module needs to be installed to a local folder.
This can be done using GraalVM JavaScript's `npm install` command like one would normally do for Node.js applications.
At runtime, the option `js.commonjs-require-cwd` can be used to specify the main installation folder for NPM packages.
The `require()` built-in function will resolve packages according to the default Node.js' [package resolution protocol](https://nodejs.org/api/modules.html#modules_all_together) starting from the directory specified via `js.commonjs-require-cwd`.
When no directory is provided with the option, the current working directory of the application will be used.

##### Node.js core modules mockups

Some JavaScript applications or NPM modules might need functionalities that are available in Node.js' built-in modules (e.g., `'fs'`, `'buffer'`, etc.)
Such modules are not available in the `Context` API.
Thankfully, the Node.js community has developed high-quality JavaScript implementations for many Node.js core modules (e.g.,: the ['buffer'](https://www.npmjs.com/package/buffer) module for the browser).
Such alternative module implementations can be exposed to a JavaScript `Context` using the `js.commonjs-core-modules-replacements` option, in the following way:
```
options.put("js.commonjs-core-modules-replacements", "buffer:my-buffer-implementation");
```
As the code suggests, the option instructs the GraalVM JavaScript runtime to load a module called `my-buffer-implementation` when an application attempts to load the Node.js `'buffer'` built-in module using `require('buffer')`.

##### Global symbols pre-initialization

An NPM module or a JavaScript application might expect certain global properties to be defined in the global scope.
For example, applications or modules might expect the `Buffer` global symbol to be defined in the JavaScript global object.
The option `js.commonjs-global-properties` can be used to pre-initialize such global symbols using the `Context` API.
The option can be used in the following way:
```
options.put("js.commonjs-global-properties", "./globals.js");
```
Once specified, the file `globals.js` will be loaded at context creation time, that is, before any JavaScript source execution.
An example `globals.js` file might perform the following initialization steps:
```
// define an empty object called 'process'
globalThis.process = {}
// define the 'Buffer' global symbol
globalThis.Buffer = require('some-buffer-implementation').Buffer;
```
The file will be executed at context creation time, and `Buffer` will be available as a global symbol in all new `Context` instances.
