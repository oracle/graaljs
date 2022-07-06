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
When executed without the GraalVM compiler, JavaScript performance will be significantly worse.
While the JIT compilers available on stock JVMs can execute and JIT-compile the GraalVM JavaScript codebase, they cannot optimize it to its full performance potential.
This document describes how to run GraalVM JavaScript on stock Java VMs, and shows how you can use the GraalVM compiler as a JIT compiler to guarantee the best possible performance.

## GraalVM JavaScript on Maven Central
GraalVM JavaScript is open source and regularly pushed to Maven Central Repository by the community.
You can find it as package [org.graalvm.js](https://mvnrepository.com/artifact/org.graalvm.js/js).

There is an example Maven project for GraalVM JavaScript on JDK11 (or later) using the GraalVM compiler at [graal-js-jdk11-maven-demo](https://github.com/graalvm/graal-js-jdk11-maven-demo).
The example contains a Maven project for a JavaScript benchmark (a prime number generator).
It allows a user to compare the performance of GraalVM JavaScript running with or without the GraalVM compiler as the optimizing compiler.
Running with the GraalVM compiler will siginificantly improve the execution performance of any relatively large JavaScript codebase.

In essence, the example POM file activates JVMCI to install additional JIT compilers, and configures the JIT compiler to be the GraalVM compiler by providing it on `--module-path` and `--upgrade-module-path`.

## GraalVM JavaScript on JDK 11+
The Maven example given above is the preferred way to start on JDK 11 (or newer).
Working without Maven, you can provide the JAR files manually to the `java` command.
Using `--upgrade-module-path` executes GraalVM JavaScript with the GraalVM compiler, guaranteeing the best performance.
The GraalVM JAR files can be downloaded from [org.graalvm at Maven](https://mvnrepository.com/artifact/org.graalvm), and the ICU4J library from [org.ibm.icu at Maven](https://mvnrepository.com/artifact/com.ibm.icu/icu4j).

*On Linux and MacOS*
```shell
JARS=/path/to/JARs
JDK=/path/to/JDK
$JDK/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler --module-path=$JARS/graal-sdk-22.2.0.jar:$JARS/truffle-api-22.2.0.jar --upgrade-module-path=$JARS/compiler-22.2.0.jar:$JARS/compiler-management-22.2.0.jar -cp $JARS/launcher-common-22.2.0.jar:$JARS/js-launcher-22.2.0.jar:$JARS/js-22.2.0.jar:$JARS/truffle-api-22.2.0.jar:$JARS/graal-sdk-22.2.0.jar:$JARS/js-scriptengine-22.2.0.jar:$JARS/regex-22.2.0.jar:$JARS/icu4j-71.1.jar com.oracle.truffle.js.shell.JSLauncher
```

*On Windows* - similar to the Linux/MacOS command but adapted to the syntax of Window's shell:
```shell
set JARs=c:\path\to\jars
set JDK=c:\path\to\jdk
%JDK%\bin\java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler --module-path=%JARS%\graal-sdk-22.2.0.jar;%JARS%\truffle-api-22.2.0.jar --upgrade-module-path=%JARS%\compiler-22.2.0.jar;%JARS%\compiler-management-22.2.0.jar -cp %JARS%\launcher-common-22.2.0.jar;%JARS%\js-launcher-22.2.0.jar;%JARS%\js-22.2.0.jar;%JARS%\truffle-api-22.2.0.jar;%JARS%\graal-sdk-22.2.0.jar;%JARS%\js-scriptengine-22.2.0.jar;%JARS%\regex-22.2.0.jar;%JARS\icu4j-71.1.jar com.oracle.truffle.js.shell.JSLauncher
```

To start a Java application instead and launch GraalVM JavaScript via GraalVM SDK's `Context` (encouraged) or a `ScriptEngine` (supported, but discouraged), _launcher-common-*.jar_ and  _js-launcher-*.jar_ can be omitted.

### ScriptEngine JSR 223
GraalVM JavaScript can be started via `ScriptEngine` when _js-scriptengine.jar_ is included on the classpath.
The engine registers under several different names, e.g., `Graal.js`.
Note that the Nashorn engine might be available under its names as well, if available on the JDK.

To start GraalVM JavaScript from `ScriptEngine`, the following code can be used:

```java
new ScriptEngineManager().getEngineByName("graal.js");
```

To list all available engines:

```java
List<ScriptEngineFactory> engines = (new ScriptEngineManager()).getEngineFactories();
for (ScriptEngineFactory f: engines) {
    System.out.println(f.getLanguageName()+" "+f.getEngineName()+" "+f.getNames().toString());
}
```

### Inspecting the Setup - Is the GraalVM Compiler Used as a JIT Compiler?
The `--engine.TraceCompilation` flag enables a debug output whenever a JavaScript method is compiled by the GraalVM compiler.
JavaScript source code with long-enough run time will trigger the compilation and print a log output:

```shell
> function add(a,b) { return a+b; }; for (var i=0;i<1000*1000;i++) { add(i,i); }
[truffle] opt done         add <opt> <split-c0875dd>                                   |ASTSize       7/    7 |Time    99(  90+9   )ms |DirectCallNodes I    0/D    0 |GraalNodes    22/   71 |CodeSize          274 |CodeAddress 0x7f76e4c1fe10 |Source    <shell>:1:1
```
