# Differences Between Node.js and Java Embeddings in GraalVM JavaScript

GraalVM's JavaScript engine is a fully-compliant ECMA2020 language runtime.
As such, it can run JavaScript code in a variety of embedding scenarios, including the Oracle [RDBMS](https://www.graalvm.org/docs/examples/mle-oracle/), any Java-based application, and Node.js.

Depending on the GraalVM's JavaScript embedding scenario, applications have access to different built-in capabilities.
For example, Node.js applications running on GraalVM's JavaScript engine have access to all of Node.js' APIs, including built-in Node.js' modules such as `'fs'`, `'http'`, etc.
Conversely, JavaScript code embedded in a Java application has access to limited capabilities, as specified through the [Context API](https://www.graalvm.org/docs/reference-manual/embed/), and do _not_ have access to Node.js' built-in modules.

In this document we focus on the main differences between a Node.js application and a GraalVM JavaScript application embedded in Java.

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
More details on the Java interoperability capabilities of GraalVM JavaScript are available in [docs/user/JavaInterop.md](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaInterop.md).

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

More details on GraalVM command line options are available in [docs/user/Options.md](https://github.com/graalvm/graaljs/blob/master/docs/user/Options.md).


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
2. The `Context` API does not support CommonJS modules, and has no built-in `require()` function.
In order to be loaded and used from a `Context` in Java, a CJS module needs to be _bundled_ into a self-contained JavaScript source file.
This can be done using one of the many popular open-source bundling tools such as Parcel, Browserify and Webpack.
