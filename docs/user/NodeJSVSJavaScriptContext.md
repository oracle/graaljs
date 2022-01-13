---
layout: docs
toc_group: js
link_title: Differences Between Node.js and Java Embeddings
permalink: /reference-manual/js/NodeJSvsJavaScriptContext/
---
# Differences Between Node.js and Java Embeddings

GraalVM provides a fully-compliant ECMAScript 2021 JavaScript language runtime.
As such, it can run JavaScript code in a variety of embedding scenarios, including [Oracle Database](https://medium.com/graalvm/mle-executing-javascript-in-oracle-database-c545feb1a010), any Java-based application, and Node.js.

Depending on the GraalVM's JavaScript embedding scenario, applications have access to different built-in capabilities.
For example, Node.js applications executed using GraalVM's `bin/node` executable have access to all of Node.js' APIs, including built-in Node.js modules such as `fs`, `http`, etc.
Conversely, JavaScript code embedded in a Java application has access to limited capabilities, as specified through the [Context API](https://github.com/oracle/graal/blob/master/docs/reference-manual/embedding/embed-languages.md#compile-and-run-a-polyglot-application), and do not have access to Node.js built-in modules.

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

1. In Node.js mode, start GraalVM using the `bin/node --jvm` command.
2. In Java, create a GraalVM context using the `withHostInterop()` option, e.g.:
```java
Context.create("js").withHostInterop()
```
More details on the Java interoperability capabilities of GraalVM JavaScript are available in [Java Interoperability](JavaInteroperability.md).

## Multithreading

A GraalVM context running JavaScript enforces a "share-nothing" model of parallelism: no JavaScript values can be accessed by two concurrent Java threads at the same time.
In order to leverage parallel execution, multiple contexts have to be created and executed from multiple threads:

1. In Node.js mode, multiple contexts can be created using Node.js' [Worker threads](https://nodejs.org/api/worker_threads.html) API.
The Worker threads API ensures that no sharing can happen between two parallel contexts.
2. In Java, multiple contexts can be executed from multiple threads.
As long as a context is not accessed by two threads at the same time, parallel execution happens safely.

More details on parallel execution in GraalVM JavaScript are available in [this blog post](https://medium.com/graalvm/multi-threaded-java-javascript-language-interoperability-in-graalvm-2f19c1f9c37b).

## Java Libraries

Java libraries can be accessed from JavaScript in GraalVM through the `Java` built-in object.
In order for a Java library to be accessible from a `Context`, its `jar` files need to be added to the GraalVM classpath. This can be done in the following way:

1. In Node.js mode, the classpath can be modified using the `--jvm.cp` option.
2. In Java, the default Java's `-cp` option can be used.

More details on GraalVM command line options are available in [Options](Options.md).

## JavaScript Packages and Modules

Many popular JavaScript modules such as those available on the `npm` package registry can be used from Node.js as well as from Java:

1. In Node.js mode, JavaScript modules are handled by the Node.js runtime.
Therefore, GraalVM JavaScript supports all modules supported by Node.js (including ES modules, CommonJS modules, and native modules).
2. In Java mode, GraalVM JavaScript can execute any JavaScript module or package that does not depend on native Node.js built-in modules (such as `'fs'`, `'http'`, etc.)
Modules can be loaded using a package bundler, or using the available built-in mechanisms for ES modules.
CommonJS modules are supported in Java mode under an experimental flag.

More details on JavaScript modules are available in [Modules](Modules.md).
