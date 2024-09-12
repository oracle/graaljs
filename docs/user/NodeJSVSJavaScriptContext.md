---
layout: docs
toc_group: js
link_title: Differences Between Node.js and Java Embeddings
permalink: /reference-manual/js/NodeJSvsJavaScriptContext/
---

# Differences Between Node.js and Java Embeddings

GraalVM provides a fully-compliant ECMAScript 2024 JavaScript runtime.
As such, it can run JavaScript code in a variety of embedding scenarios, including [Oracle Database](https://medium.com/graalvm/mle-executing-javascript-in-oracle-database-c545feb1a010), any Java-based application, and Node.js.

Depending on the embedding scenario, applications have access to different built-in capabilities.
For example, Node.js applications executed using GraalVM's `bin/node` executable have access to all of Node.js' APIs, including built-in Node.js modules such as `fs`, `http`, and so on.
Conversely, JavaScript code embedded in a Java application has access to limited capabilities, as specified through the [Context API](https://github.com/oracle/graal/blob/master/docs/reference-manual/embedding/embed-languages.md#compile-and-run-a-polyglot-application), and do not have access to Node.js built-in modules.

This guide describes the main differences between a Node.js application and JavaScript embedded in a Java application.

## Context Creation

JavaScript code in GraalVM can be executed using an execution _context_.

In a Java application, a new context can be created using the [`Context` API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html).
New contexts can be configured in multiple ways, and configuration options include exposing access to Java classes, allowing access to IO, and so on.
A list of context creation options can be found in the [API documentation](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html).
In this scenario, Java classes can be exposed to JavaScript by using GraalVM's [Polyglot `Bindings`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#getPolyglotBindings).

In a Node.js application, the GraalVM `Context` executing the application is pre-initialized by the Node.js runtime, and cannot be configured by the user application.
In this scenario, Java classes can be exposed to the Node.js application by using the `--vm.cp=` command line option of the `bin/node` command, as described below.

## Java Interoperability

JavaScript applications can interact with Java classes using the `Java` built-in object.
This object is available by default in the `js` and `node` launchers, but accessing Java classes is only possible in the JVM standalone (that have `-jvm` in the name).

When embedding JavaScript using the Polyglot API, you have to explicitly enable host access in the [`Context.Builder`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.Builder.html) (`allowHostAccess`, `allowHostClassLookup`).
More details on the JavaScript-Java interoperability are available in the [Java Interoperability guide](JavaInteroperability.md).

## Multithreading

A polyglot `Context` running JavaScript enforces a "share-nothing" model of parallelism: no JavaScript values can be accessed by two concurrent Java threads at the same time.
In order to leverage parallel execution, multiple contexts have to be created and executed from multiple threads:

1. In Node.js mode, multiple contexts can be created using Node.js' [Worker threads](https://nodejs.org/api/worker_threads.html) API.
The Worker threads API ensures that no sharing can happen between two parallel contexts.
2. In Java, multiple contexts can be executed from multiple threads.
As long as a context is not accessed by two threads at the same time, parallel execution happens safely.

More details on parallel execution in GraalJS are available in [this blog post](https://medium.com/graalvm/multi-threaded-java-javascript-language-interoperability-in-graalvm-2f19c1f9c37b).

## Java Libraries

Java libraries can be accessed from GraalJS through the `Java` built-in object.
In order for a Java library to be accessible from a `Context`, its JAR files need to be added to the class path. 
This can be done in the following way:
1. In Node.js mode, the class path can be modified using the `--vm.cp` option.
2. In Java, the default Java's `-cp` option can be used.

Read more in [Command-line Options](Options.md).

## JavaScript Packages and Modules

Many popular JavaScript modules such as those available on the `npm` package registry can be used from Node.js as well as from Java:

1. In Node.js mode, JavaScript modules are handled by the Node.js runtime.
Therefore, GraalJS supports all modules supported by Node.js (including ES modules, CommonJS modules, and native modules).
2. In Java mode, GraalJS can execute any JavaScript module or package that does not depend on native Node.js built-in modules (such as `fs`, `http`, and so on).
Modules can be loaded using a package bundler, or using the available built-in mechanisms for ES modules.
CommonJS modules are supported in Java mode under an experimental option.

More details on JavaScript modules are available in [Modules](Modules.md).

### Related Documentation

* [Getting Started with Node.js](NodeJS.md)
* [Using JavaScript Modules and Packages in GraalJS](Modules.md)
