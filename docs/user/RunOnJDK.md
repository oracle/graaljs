---
layout: docs
toc_group: js
link_title: Run GraalVM JavaScript on a Stock JDK
permalink: /reference-manual/js/RunOnJDK/
---
# Run GraalVM JavaScript on a Stock JDK

GraalVM JavaScript is optimized for execution as part of GraalVM, or in an embedding scenario built on GraalVM.
This guarantees best possible performance by using the [GraalVM compiler](https://github.com/oracle/graal) as the optimizing compiler, and potentially [Native Image](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/README.md) to ahead-of-time compile the engine into a native binary.

As GraalVM JavaScript is a Java application, it is possible to execute it on a stock Java VM like OpenJDK.
When executed without the Graal Compiler, JavaScript performance will be significantly worse.
While the JIT compilers available on stock JVMs can execute and JIT-compile the GraalVM JavaScript codebase, they cannot optimize it to its full performance potential.
This document describes how to run GraalVM JavaScript on stock Java VMs, and shows how you can use the GraalVM compiler as a JIT compiler to guarantee the best possible performance.

## GraalVM JavaScript on Maven Central
GraalVM JavaScript is open source and regularly pushed to Maven Central Repository by the community.
You can find it as pom artifact [org.graalvm.polyglot:js](https://mvnrepository.com/artifact/org.graalvm.polyglot/js).

There are example Maven projects for GraalVM JavaScript on JDK21 (or later) using the Graal compiler:
* [Polyglot Embedding Demo](https://github.com/graalvm/polyglot-embedding-demo).
Example Maven and Gradle projects for a simple JavaScript "Hello World" application.
* [JS Maven Demo](https://github.com/oracle/graaljs/tree/master/graal-js/test/maven-demo)
This example contains a Maven project for a JavaScript benchmark (a prime number generator).
It allows a user to compare the performance of GraalVM JavaScript running with or without the GraalVM compiler as the optimizing compiler.
Running with the GraalVM compiler will significantly improve the execution performance of any relatively large JavaScript codebase.

In essence, the example `pom.xml` file activates JVMCI to install additional JIT compilers, and configures the JIT compiler to be the Graal Compiler by providing it on `--module-path` and `--upgrade-module-path`.

### ScriptEngine JSR 223
GraalVM JavaScript can be started via `ScriptEngine` when _js-scriptengine.jar_ is included on the module path.
The engine registers under several different names, including `Graal.js`, `js`, `JavaScript`, and `javascript`.
Note that the Nashorn engine might be available under its names as well, if on the module path.

To start GraalVM JavaScript from `ScriptEngine`, the following code can be used:

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
The `--engine.TraceCompilation` flag enables a debug output whenever a JavaScript method is compiled by the GraalVM compiler.
JavaScript source code with long-enough run time will trigger the compilation and print a log output:

```shell
> function add(a,b) { return a+b; }; for (var i=0;i<1000*1000;i++) { add(i,i); }
[truffle] opt done         add <opt> <split-c0875dd>                                   |ASTSize       7/    7 |Time    99(  90+9   )ms |DirectCallNodes I    0/D    0 |GraalNodes    22/   71 |CodeSize          274 |CodeAddress 0x7f76e4c1fe10 |Source    <shell>:1:1
```
