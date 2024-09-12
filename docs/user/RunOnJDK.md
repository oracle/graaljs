---
layout: docs
toc_group: js
link_title: Run GraalJS on a Stock JDK
permalink: /reference-manual/js/RunOnJDK/
---

# Run GraalJS on a Stock JDK

GraalJS is optimized for execution as part of GraalVM, primarily recommended for use in a Java application.
This guarantees the best possible performance by using the [Graal compiler](https://www.graalvm.org/reference-manual/java/compiler/) as the optimizing compiler, and potentially [Native Image](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/README.md) to compile the engine ahead of time into a native binary.

It is, however, possible to execute GraalJS on a standard Java VM such as Oracle JDK or OpenJDK.
When executed without the Graal Compiler, JavaScript performance will be significantly worse.
While the JIT compiler available on a standard JVM can execute and JIT-compile the GraalJS codebase, it cannot optimize GraalJS to its full performance potential.
This document describes how to run GraalJS on a standard Java VM, and shows how you can use the Graal compiler as a JIT compiler to guarantee the best possible performance.

## GraalJS on Maven Central

GraalJS is open source and regularly pushed to Maven Central Repository by the community.
You can find it under [`org.graalvm.polyglot:js`](https://mvnrepository.com/artifact/org.graalvm.polyglot/js).

We provide example projects running GraalJS embedded in Java on JDK 21 (or later) and using the Graal compiler:
* [Polyglot Embedding Demo](https://github.com/graalvm/polyglot-embedding-demo).
Maven and Gradle projects for a simple JavaScript "Hello World" application.
* [JS Maven Demo](https://github.com/oracle/graaljs/tree/master/graal-js/test/maven-demo).
This example contains a Maven project for a JavaScript benchmark (a prime number generator).
It enables a user to compare the performance of GraalJS running with or without the Graal compiler as the optimizing compiler.
Running with the Graal compiler significantly improves the execution performance of any relatively large JavaScript codebase.
In essence, the example _pom.xml_ file activates the JVM Compiler Interface (JVMCI) and configures the JIT compiler to be the Graal compiler by providing it on `--module-path` and `--upgrade-module-path`.

## ScriptEngine JSR 223

GraalJS can be started via `ScriptEngine` when _js-scriptengine.jar_ is included on the module path.
The engine registers under several different names, including `Graal.js`, `js`, `JavaScript`, and `javascript`.
Note that the Nashorn engine might be available under its names as well, if on the module path.

To start GraalJS from `ScriptEngine`, the following code can be used:
```java
new ScriptEngineManager().getEngineByName("Graal.js");
```

To list all available engines:
```java
List<ScriptEngineFactory> engines = new ScriptEngineManager().getEngineFactories();
for (ScriptEngineFactory f : engines) {
    System.out.println(f.getLanguageName() + " " + f.getEngineName() + " " + f.getNames());
}
```

### Inspecting the Setup - Is the GraalVM Compiler Used as a JIT Compiler?

The `--engine.TraceCompilation` option enables a debug output whenever a JavaScript method is compiled by the Graal compiler.
JavaScript source code with a long-enough run time will trigger the compilation and print a log output:
```shell
> function add(a,b) { return a+b; }; for (var i=0;i<1000*1000;i++) { add(i,i); }
[truffle] opt done         add <opt> <split-c0875dd>                                   |ASTSize       7/    7 |Time    99(  90+9   )ms |DirectCallNodes I    0/D    0 |GraalNodes    22/   71 |CodeSize          274 |CodeAddress 0x7f76e4c1fe10 |Source    <shell>:1:1
```

### Related Documentation

* [Getting Started with GraalJS on the JVM](README.md)