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
See the documentation on the [GraalVM website](https://www.graalvm.org/docs/getting-started/) how to install and use GraalVM JavaScript.

```
$ $GRAALVM/bin/js
> print("Hello JavaScript");
Hello JavaScript
>
```

The preferred way to run GraalVM JavaScript is from a [GraalVM](https://www.graalvm.org/downloads/).
If you prefer running it on a stock JVM, please have a look at the documentation in [`RunOnJDK.md`](https://github.com/graalvm/graaljs/blob/master/docs/user/RunOnJDK.md).

## Documentation

Extensive documentation is available on [graalvm.org](https://www.graalvm.org/): how to [`Run JavaScript`](https://www.graalvm.org/docs/getting-started/#running-javascript) and the more extensive [`JavaScript & Node.js Reference Manual`](https://www.graalvm.org/reference-manual/js/).
In addition there is documentation in the source code repository in the [`docs`](https://github.com/graalvm/graaljs/tree/master/docs) folder, for [`users`](https://github.com/graalvm/graaljs/tree/master/docs/user) and [`contributors`](https://github.com/graalvm/graaljs/tree/master/docs/contributor) of the engine.

For contributors, a guide how to build GraalVM JavaScript from source code can be found in [`Building.md`](https://github.com/graalvm/graaljs/tree/master/docs/Building.md).

## Current Status

GraalVM JavaScript is compatible with the [ECMAScript 2021 specification](https://262.ecma-international.org/12.0/).
Starting with GraalVM 22.0.0, ECMAScript 2022 - currently at the draft stage - is the default compatibility level.
New features, e.g. `ECMAScript proposals` scheduled to land in future editions, are added frequently and are accessible behind a flag.

In addition, some popular extensions of other engines are supported, see [`JavaScriptCompatibility.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/JavaScriptCompatibility.md).

### Node.js support

GraalVM JavaScript can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelyhood that your application will run out of the box.
This includes npm packages with native implementations.
Note that some npm modules will require to be re-compiled from source with GraalVM JavaScript if they ship with binaries that have been compiled for Node.js based on V8.

Node.js support is not included in the main GraalVM distribution (since 21.1) but packaged as a separate component that can be installed using the _GraalVM Updater_:

```shell
$ $GRAALVM/bin/gu install nodejs
$ $GRAALVM/bin/node --version
```

### Compatibility on Operating Systems

The core JavaScript engine is a Java application and is thus in principle compatible with every operating system that provides a compatible JVM, [see `RunOnJDK.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/RunOnJDK.md).
We test and support GraalVM JavaScript currently in full extent on Linux and MacOS.
For Windows, a preliminary preview version is available.

Some features, including the Node.js support, are currently not supported on all platforms (e.g. Windows).

## GraalVM JavaScript Reference Manual

A reference manual for GraalVM JavaScript is available on the [GraalVM website](https://www.graalvm.org/reference-manual/js/).

## Stay connected with the community

See [graalvm.org/community](https://www.graalvm.org/community/) on how to stay connected with the development community.
The channel _graaljs_ on [graalvm.slack.com](https://www.graalvm.org/slack-invitation) is a good way to get in touch with us.

## Licence

GraalVM JavaScript is available under the following license:

* [The Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL)


