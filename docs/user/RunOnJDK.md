# Run GraalVM JavaScript on a Stock JDK

GraalVM JavaScript is optimized for execution as part of GraalVM, or in an embedding scenario built on GraalVM.
This guarantees best possible performance by using the [GraalVM compiler](https://github.com/oracle/graal) as the optimizing compiler, and potentially [Native Image](https://www.graalvm.org/reference-manual/native-image/) to ahead-of-time compile the engine into a native binary.

As GraalVM JavaScript is a Java application, it is possible to execute it on a stock Java VM like OpenJDK.
When executed without the GraalVM compiler, JavaScript performance will be significantly worse.
While the JIT compilers available on stock JVMs can execute and JIT-compile the GraalVM JavaScript codebase, they cannot optimize it to its full performance potential.
This document describes how to run GraalVM JavaScript on stock Java VMs, and shows how you can use the GraalVM compiler as a JIT compiler to guarantee the best possible performance.

## GraalVM JavaScript on Maven Central
GraalVM JavaScript is open source and regularly pushed to Maven Central Repository by the community.
You can find it as package [org.graalvm.js](https://mvnrepository.com/artifact/org.graalvm.js/js).

There is an exemple Maven project for GraalVM JavaScript on JDK11 (or later) using the GraalVM compiler at [graal-js-jdk11-maven-demo](https://github.com/graalvm/graal-js-jdk11-maven-demo).
The example contains a Maven project for a JavaScript benchmark (a prime number generator).
It allows a user to compare the performance of GraalVM JavaScript running with or without the GraalVM compiler as the optimizing compiler.
Running with the GraalVM compiler will siginificantly improve the execution performance of any relatively large JavaScript codebase.

In essence, the example POM file activates JVMCI to install additional JIT compilers, and configures the JIT compiler to be the GraalVM compiler by providing it on `--module-path` and `--upgrade-module-path`.

### GraalVM JavaScript without Maven - JAR Files from GraalVM
To work without Maven, the JAR files from a GraalVM release can be used as well.
GraalVM is available as [Enterprise](https://www.oracle.com/downloads/graalvm-downloads.html) and [Community](https://github.com/oracle/graal/releases) Editions. Both editions' files can be used.

The relevant files are:
* _$GRAALVM/jre/languages/js/graaljs.jar_ - core component of GraalVM JavaScript (always required)
* _$GRAALVM/jre/tools/regex/tregex.jar_ - GraalVM's regular expression engine (always required)
* _$GRAALVM/jre/lib/boot/graal-sdk.jar_ - GraalVM's SDK to implement languages (always required)
* _$GRAALVM/jre/lib/truffle/truffle-api.jar_ - GraalVM's Language API, to implement language interpreters (always required)
* _$GRAALVM/jre/lib/graalvm/graaljs-launcher.jar_ - GraalVM JavaScript's command line interpreter (optional)
* _$GRAALVM/jre/lib/graalvm/launcher-common.jar_ - common launcher code shared by all languages (required by _graaljs-launcher.jar_)
* _$GRAALVM/jre/lib/boot/graaljs-scriptengine.jar_ - GraalVM JavaScript's ScriptEngine/JSR 223 support (optional)

## GraalVM JavaScript on JDK 8

The following command line executes GraalVM JavaScript on a JDK 8, starting a JavaScript console.
Note that this variant does not include the GraalVM compiler as the optimizing compiler, so the performance of GraalVM JavaScript will be suboptimal.
See the JDK 11 example below for how to improve on this.

*On Linux*
```shell
GRAALVM=/path/to/GraalVM
/path/to/jdk8/bin/java -cp $GRAALVM/jre/lib/graalvm/launcher-common.jar:$GRAALVM/jre/lib/graalvm/graaljs-launcher.jar:$GRAALVM/jre/languages/js/graaljs.jar:$GRAALVM/jre/lib/truffle/truffle-api.jar:$GRAALVM/jre/lib/boot/graal-sdk.jar:$GRAALVM/jre/lib/boot/graaljs-scriptengine.jar:$GRAALVM/jre/tools/regex/tregex.jar com.oracle.truffle.js.shell.JSLauncher
```

*On MacOS* - identical to the Linux command except for the path to GraalVM you need to add `Contents/Home`:
```shell
GRAALVM=/path/to/graalvm/Contents/Home
```

*On Windows* - GraalVM JavaScript offers preliminary support for Windows:
```shell
set GRAALVM=c:\path\to\graalvm
%GRAALVM%\bin\java -cp %GRAALVM%\jre\lib\graalvm\launcher-common.jar;%GRAALVM%\jre\lib\graalvm\graaljs-launcher.jar;%GRAALVM%\jre\languages\js\graaljs.jar;%GRAALVM%\jre\lib\truffle\truffle-api.jar;%GRAALVM%\jre\lib\boot\graal-sdk.jar;%GRAALVM%\jre\lib\boot\graaljs-scriptengine.jar;%GRAALVM%\jre\tools\regex\tregex.jar com.oracle.truffle.js.shell.JSLauncher
```

To start a Java application instead and launch GraalVM JavaScript via GraalVM SDK's `Context` (encouraged) or a `ScriptEngine` (supported, but discouraged), _launcher-common.jar_ and  _graaljs-launcher.jar_ can be omitted (see example below).

### ScriptEngine JSR 223
GraalVM JavaScript can be started via `ScriptEngine` when _graaljs-scriptengine.jar_ is included on the classpath.
The engine registers under several different names, e.g., `Graal.js`.
Note that the Nashorn engine might be available under its names as well.

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

Assuming this code is called from `MyJavaApp.java` and is properly compiled to a class file, it can be executed with:

```shell
GRAALVM=/path/to/GraalVM
/path/to/jdk8/bin/java -cp $GRAALVM/jre/languages/js/graaljs.jar:$GRAALVM/jre/lib/truffle/truffle-api.jar:$GRAALVM/jre/lib/boot/graal-sdk.jar:$GRAALVM/jre/lib/boot/graaljs-scriptengine.jar:$GRAALVM/jre/tools/regex/tregex.jar:. MyJavaApp
```

## GraalVM JavaScript on JDK 11
The Maven example given above is the preferred way to start on JDK 11.
Working without Maven, you have to provide the JAR files manually to the `java` command.
Using `--upgrade-module-path` executes GraalVM JavaScript with the GraalVM compiler, guaranteeing the best performance.
For that, the [GraalVM compiler](https://github.com/oracle/graal/tree/master/compiler) built with JDK 11 is required.

```shell
GRAALVM=/path/to/GraalVM
GRAAL_JDK11=/path/to/Graal
/path/to/jdk-11/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler --module-path=$GRAAL_JDK11/graal/sdk/mxbuild/modules/org.graalvm.graal_sdk.jar:$GRAAL_JDK11/graal/truffle/mxbuild/modules/com.oracle.truffle.truffle_api.jar --upgrade-module-path=$GRAAL_HOME/graal/compiler/mxbuild/modules/jdk.internal.vm.compiler.jar:$GRAAL_HOME/graal/compiler/mxbuild/modules/jdk.internal.vm.compiler.management.jar -cp $GRAALVM/jre/lib/graalvm/launcher-common.jar:$GRAALVM/jre/lib/graalvm/graaljs-launcher.jar:$GRAALVM/jre/languages/js/graaljs.jar:$GRAALVM/jre/lib/truffle/truffle-api.jar:$GRAALVM/jre/lib/boot/graal-sdk.jar:$GRAALVM/jre/lib/boot/graaljs-scriptengine.jar:$GRAALVM/jre/tools/regex/tregex.jar com.oracle.truffle.js.shell.JSLauncher
```

### Inspecting the Setup - Is the GraalVM Compiler Used as a JIT Compiler?
The `--engine.TraceCompilation` flag enables a debug output whenever a JavaScript method is compiled by the GraalVM compiler.
JavaScript source code with long-enough run time will trigger the compilation and print a log output:

```shell
> function add(a,b) { return a+b; }; for (var i=0;i<1000*1000;i++) { add(i,i); }
[truffle] opt done         add <opt> <split-c0875dd>                                   |ASTSize       7/    7 |Time    99(  90+9   )ms |DirectCallNodes I    0/D    0 |GraalNodes    22/   71 |CodeSize          274 |CodeAddress 0x7f76e4c1fe10 |Source    <shell>:1:1
```
