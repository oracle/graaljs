[![https://graalvm.slack.com](https://img.shields.io/badge/slack-join%20channel-active)](https://www.graalvm.org/slack-invitation/)

A high performance implementation of the JavaScript programming language.
Built on the GraalVM by Oracle Labs.

The goals of GraalVM JavaScript are:

* Execute JavaScript code with best possible performance
* [Full compatibility with the latest ECMAScript specification](docs/user/JavaScriptCompatibility.md)
* Support Node.js applications, including native packages ([check](https://www.graalvm.org/compatibility/))
* Allow simple upgrading from [Nashorn](docs/user/NashornMigrationGuide.md) or [Rhino](docs/user/RhinoMigrationGuide.md) based applications
* [Fast interoperability](https://www.graalvm.org/reference-manual/polyglot-programming/) with Java, Scala, or Kotlin, or with other GraalVM languages like Ruby, Python, or R
* Be embeddable in systems like [Oracle RDBMS](https://labs.oracle.com/pls/apex/f?p=LABS:project_details:0:15) or MySQL


## Getting Started
The preferred way to run GraalVM JavaScript (a.k.a. GraalJS) is from a [GraalVM](https://www.graalvm.org/downloads/).
As of GraalVM for JDK 21, (23.1), GraalVM JavaScript and Node.js runtimes are available as [standalone distributions](https://github.com/oracle/graaljs/releases) and [Maven artifacts](https://central.sonatype.com/artifact/org.graalvm.polyglot/js) (but no longer as GraalVM components).
See the documentation on the [GraalVM website](https://www.graalvm.org/latest/reference-manual/js/) for more information on how to set up GraalVM JavaScript.

### Standalone Distribution
To install GraalVM JavaScript using the [standalone distribution], simply download and extract the desired archive from the [GitHub Releases](https://github.com/oracle/graaljs/releases) page.
The standalone archives for the JavaScript and Node.js distributions are named `graaljs[-community][-jvm]-<version>-<os>-<arch>.tar.gz` and `graalnodejs[-community][-jvm]-<version>-<os>-<arch>.tar.gz`, respectively.
Four different available configurations are available for each component and platform combination:

| Runtime      | License | Archive Infix    |
| -------------| ------- | ---------------- |
| Native       | GFTC    | _none_           |
| JVM          | GFTC    | `-jvm`           |
| Native       | UPL     | `-community`     |
| JVM          | UPL     | `-community-jvm` |

After installation, the `js` or `node` executable in the `bin` subdirectory can be used to run JavaScript files or Node modules, respectively.
If no file is provided on the command line, an interactive shell (REPL) will be spawned.

### Maven Artifact
All required artifacts for embedding GraalVM JavaScript can be found in the Maven dependency group [`org.graalvm.polyglot`](https://central.sonatype.com/namespace/org.graalvm.polyglot).

Here is a minimal Maven dependency setup that you can copy into your `pom.xml`:
```xml
<dependency>
	<groupId>org.graalvm.polyglot</groupId>
	<artifactId>polyglot</artifactId>
	<version>23.1.0</version>
</dependency>
<dependency>
	<groupId>org.graalvm.polyglot</groupId>
	<artifactId>js</artifactId>
	<version>23.1.0</version>
	<type>pom</type>
</dependency>
<!-- add additional languages and tools, if needed -->
```

See the [polyglot embedding demonstration](https://github.com/graalvm/polyglot-embedding-demo) on GitHub for a complete runnable example.

Language and tool dependencies use the [GraalVM Free Terms and Conditions (GFTC)](https://www.oracle.com/downloads/licenses/graal-free-license.html) license by default.
To use community-licensed versions instead, add the `-community` suffix to each language and tool dependency, e.g.:
```xml
<dependency>
	<groupId>org.graalvm.polyglot</groupId>
	<artifactId>js-community</artifactId>
	<version>23.1.0</version>
	<type>pom</type>
</dependency>
```
To access [polyglot isolate](https://www.graalvm.org/latest/reference-manual/embed-languages/#polyglot-isolates) artifacts (GFTC only), use the `-isolate` suffix instead (e.g. `js-isolate`).

If you prefer running it on a stock JVM, please have a look at the documentation in [`RunOnJDK.md`](https://github.com/graalvm/graaljs/blob/master/docs/user/RunOnJDK.md).
Note that in this mode many features and optimizations of GraalVM are not available.
Due to those limitations, running on a stock JVM is not a supported feature - please use a GraalVM instead.

## Documentation

Extensive documentation is available on [graalvm.org](https://www.graalvm.org/): see [Getting Started](https://www.graalvm.org/docs/getting-started/) and the more extensive [JavaScript and Node.js Reference Manual](https://www.graalvm.org/reference-manual/js/).
In addition, there is documentation in the source code repository in the [`docs`](https://github.com/graalvm/graaljs/tree/master/docs) directory, for [users](https://github.com/graalvm/graaljs/tree/master/docs/user) and [contributors](https://github.com/graalvm/graaljs/tree/master/docs/contributor).

For contributors, a guide how to build GraalVM JavaScript from source code can be found in [`Building.md`](https://github.com/graalvm/graaljs/tree/master/docs/Building.md).

## Current Status

GraalVM JavaScript is compatible with the [ECMAScript 2023 specification](https://262.ecma-international.org/14.0/).
New features, e.g. `ECMAScript proposals` scheduled to land in future editions, are added frequently and are accessible behind a flag.
See the [CHANGELOG.md](https://github.com/graalvm/graaljs/tree/master/CHANGELOG.md) for the proposals already adopted.

In addition, some popular extensions of other engines are supported, see [`JavaScriptCompatibility.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/JavaScriptCompatibility.md).

### Node.js support

GraalVM JavaScript can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelihood that your application will run out of the box.
This includes npm packages with native implementations.
Note that some npm modules will require to be re-compiled from source with GraalVM JavaScript if they ship with binaries that have been compiled for Node.js based on V8.
Node.js is a separate [standalone distribution](#standalone-distribution).

### Compatibility on Operating Systems

The core JavaScript engine is a Java application and is thus in principle compatible with every operating system that provides a compatible JVM, [see `RunOnJDK.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/RunOnJDK.md).
We provide binary distributions and fully support GraalVM JavaScript on Linux (AMD64, AArch64), MacOS (AMD64, AArch64), and Windows (AMD64), currently.

## GraalVM JavaScript Reference Manual

A reference manual for GraalVM JavaScript is available on the [GraalVM website](https://www.graalvm.org/reference-manual/js/).

## Stay connected with the community

See [graalvm.org/community](https://www.graalvm.org/community/) on how to stay connected with the development community.
The channel _graaljs_ on [graalvm.slack.com](https://www.graalvm.org/slack-invitation) is a good way to get in touch with us.
Please report JavaScript-specific issues on the [`oracle/graaljs`](https://github.com/oracle/graaljs/) GitHub repository.

## License

GraalVM JavaScript source code and community distributions are available under the following license:

* [The Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL)

Non-community artifacts are provided under the following license:

* [GraalVM Free Terms and Conditions (GFTC) including License for Early Adopter Versions](https://www.oracle.com/downloads/licenses/graal-free-license.html)
