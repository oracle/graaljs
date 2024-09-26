[![https://graalvm.slack.com](https://img.shields.io/badge/slack-join%20channel-active)](https://www.graalvm.org/slack-invitation/)

# GraalJS

GraalJS is a JavaScript engine implemented in Java on top of GraalVM. 
It is an ECMAScript-compliant runtime to execute JavaScript and Node.js applications, and includes all the benefits from the GraalVM stack including interoperability with Java.
GraalJS is an open-source project.

The goals of GraalJS are:
* [Full compatibility with the latest ECMAScript specification](docs/user/JavaScriptCompatibility.md)
* [Interoperability with Java](docs/user/JavaInteroperability.md)
* Interoperability with WebAssembly using the JavaScript WebAssembly API
* Running JavaScript with the best possible performance
* Support for Node.js applications, including native packages
* Simple upgrading from applications based on [Nashorn](docs/user/NashornMigrationGuide.md) or [Rhino](docs/user/RhinoMigrationGuide.md)
* Embeddability in systems like [Oracle RDBMS](https://labs.oracle.com/pls/apex/f?p=LABS:project_details:0:15) or MySQL

## Getting Started

As of version 23.1.0, GraalJS is available as [Maven artifacts](https://central.sonatype.com/artifact/org.graalvm.polyglot/js).
We also provide [standalone distributions](https://github.com/oracle/graaljs/releases) of the JavaScript and Node.js runtimes.

### Maven Artifacts

Thanks to GraalJS, you can easily embed JavaScript into a Java application.
All necessary artifacts can be downloaded directly from Maven Central.

All artifacts relevant to embedders can be found in the Maven dependency group [org.graalvm.polyglot](https://central.sonatype.com/namespace/org.graalvm.polyglot). 

Below is a minimal Maven dependency setup that you can copy into your _pom.xml_:
```xml
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>${graaljs.version}</version>
</dependency>
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js</artifactId>
    <version>${graaljs.version}</version>
    <type>pom</type>
</dependency>
```
This enables GraalJS which is built on top of Oracle GraalVM and licensed under the [GraalVM Free Terms and Conditions (GFTC)](https://www.oracle.com/downloads/licenses/graal-free-license.html).
Use `js-community` if you want to use GraalJS built on GraalVM Community Edition.


To access [polyglot isolate](https://www.graalvm.org/reference-manual/embed-languages/#polyglot-isolates) artifacts (GFTC only), use the `-isolate` suffix instead (e.g. `js-isolate`).

See the [polyglot embedding demonstration](https://github.com/graalvm/polyglot-embedding-demo) on GitHub for a complete runnable example.

You can use GraalJS with GraalVM JDK, Oracle JDK, or OpenJDK. 
If you prefer running on a stock JVM, have a look at [Run GraalJS on a Stock JDK](docs/user/RunOnJDK.md). 
Note that in this mode many features and optimizations of GraalVM are not available.
Due to those limitations, running on a stock JVM is not a supported feature - please use a GraalVM instead.

### Standalone Distributions

Standalone distributions are published on [GitHub](https://github.com/oracle/graaljs/releases). 
There are two language runtime options to choose from: 
- Native launcher compiled ahead of time with GraalVM Native Image;
- JVM-based runtime.

To distinguish between them, a standalone that comes with a JVM has a `-jvm` infix in the name. 
Also, the GraalVM Community Edition version has `-community` in the name, for example, `graaljs-community-<version>-<os>-<arch>.tar.gz`.

Four different configurations are available for each component and platform combination:

| Runtime      | License | Archive Infix    |
| -------------| ------- | ---------------- |
| Native       | GFTC    | _none_           |
| JVM          | GFTC    | `-jvm`           |
| Native       | UPL     | `-community`     |
| JVM          | UPL     | `-community-jvm` |

To install GraalJS from a standalone, download and extract the archive from the [GitHub Releases](https://github.com/oracle/graaljs/releases) page. 
After the installation, the `js` or `node` executable in the `bin` subdirectory can be used to run JavaScript files or Node modules, respectively.
If no file is provided on the command line, an interactive shell (REPL) will be spawned.

> Note: If you are using macOS, first remove the quarantine attribute from the archive:
    ```shell
    sudo xattr -r -d com.apple.quarantine <archive>.tar.gz
    ```

## Node.js Runtime

GraalJS can run unmodified Node.js applications. 
GraalVM's Node.js runtime is based on a recent version of Node.js, and runs the GraalJS engine instead of Google V8. 
It provides high compatibility with the existing NPM packages.
This includes NPM packages with native implementations.
Note that some NPM modules may require to be recompiled from source with GraalJS (if they ship with binaries that have been compiled for Node.js based on V8).

Node.js is available as a separate [standalone distribution](#standalone-distributions).
See [how to get started with Node.js](NodeJS.md).

## Documentation

Extensive user documentation is available on the [website](https://www.graalvm.org/reference-manual/js/).
In addition, there is documentation in this repository under [docs](https://github.com/oracle/graaljs/tree/master/docs), for [users](https://github.com/oracle/graaljs/tree/master/docs/user) and [contributors](https://github.com/oracle/graaljs/tree/master/docs/contributor).
For contributing, see also a [guide on how to build GraalJS from source code](docs/Building.md).

## Compatibility

GraalJS is compatible with the [ECMAScript 2024 specification](https://262.ecma-international.org/).
New features, new ECMAScript proposals, scheduled to land in future editions, are added frequently and are accessible behind an option.
See the [CHANGELOG.md](CHANGELOG.md) for the proposals already adopted.

In addition, some popular extensions of other engines are supported. See [GraalJS Compatibility](docs/user/JavaScriptCompatibility.md).

## Operating Systems Compatibility

The core JavaScript engine is a Java application and is thus compatible with every operating system that provides a compatible JVM. See [Run GraalJS on a Stock JDK](docs/user/RunOnJDK.md).
We provide binary distributions and fully support GraalJS on Linux (x64, AArch64), macOS (x64, AArch64), and Windows (x64), currently.

## Stay Connected with the Community

See [graalvm.org/community](https://www.graalvm.org/community/) for how to stay connected with the development community.
The channel _graaljs_ on [graalvm.slack.com](https://www.graalvm.org/slack-invitation) is a good way to get in touch with the team behind GraalJS.
Report any GraalJS-specific issues at the [oracle/graaljs](https://github.com/oracle/graaljs/) GitHub repository.

## License

GraalJS source code and community distributions are available under the [Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL).

Non-community artifacts are provided under the [GraalVM Free Terms and Conditions (GFTC) including License for Early Adopter Versions](https://www.oracle.com/downloads/licenses/graal-free-license.html).
