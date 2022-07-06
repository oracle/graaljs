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
The preferred way to run GraalVM JavaScript is from a [GraalVM](https://www.graalvm.org/downloads/).
Starting with GraalVM 22.2., GraalVM JavaScript is an installable component that needs to be installed with `gu install js` after downloading GraalVM.
See the documentation on the [GraalVM website](https://www.graalvm.org/docs/getting-started/) for more information on how to install and use GraalVM JavaScript.

Installing GraalVM JavaScript using the _GraalVM Updater_:

```shell
$ $GRAALVM/bin/gu install js
$ $GRAALVM/bin/js --version
```

After installation, the `js` shell can be executed and used to run JavaScript code or execute JavaScript files.

```
$ $GRAALVM/bin/js
> print("Hello JavaScript");
Hello JavaScript
>
```

If you prefer running it on a stock JVM, please have a look at the documentation in [`RunOnJDK.md`](https://github.com/graalvm/graaljs/blob/master/docs/user/RunOnJDK.md).
Note that in this mode many features and optimizations of GraalVM are not available.
Due to those limitations, running on a stock JVM is not a supported feature - please use a GraalVM instead.

## Documentation

Extensive documentation is available on [graalvm.org](https://www.graalvm.org/): how to [`Run JavaScript`](https://www.graalvm.org/docs/getting-started/#running-javascript) and the more extensive [`JavaScript & Node.js Reference Manual`](https://www.graalvm.org/reference-manual/js/).
In addition there is documentation in the source code repository in the [`docs`](https://github.com/graalvm/graaljs/tree/master/docs) folder, for [`users`](https://github.com/graalvm/graaljs/tree/master/docs/user) and [`contributors`](https://github.com/graalvm/graaljs/tree/master/docs/contributor) of the engine.

For contributors, a guide how to build GraalVM JavaScript from source code can be found in [`Building.md`](https://github.com/graalvm/graaljs/tree/master/docs/Building.md).

## Current Status

GraalVM JavaScript is compatible with the [ECMAScript 2022 specification](https://262.ecma-international.org/13.0/).
New features, e.g. `ECMAScript proposals` scheduled to land in future editions, are added frequently and are accessible behind a flag.
See the [CHANGELOG.md](https://github.com/graalvm/graaljs/tree/master/CHANGELOG.md) for the proposals already adopted.

In addition, some popular extensions of other engines are supported, see [`JavaScriptCompatibility.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/JavaScriptCompatibility.md).

### Node.js support

GraalVM JavaScript can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelyhood that your application will run out of the box.
This includes npm packages with native implementations.
Note that some npm modules will require to be re-compiled from source with GraalVM JavaScript if they ship with binaries that have been compiled for Node.js based on V8.

Similar to JavaScript itself, Node.js is a separately installable component of GraalVM (since 21.1).
It can be installed using the _GraalVM Updater_:

```shell
$ $GRAALVM/bin/gu install nodejs
$ $GRAALVM/bin/node --version
```

### Compatibility on Operating Systems

The core JavaScript engine is a Java application and is thus in principle compatible with every operating system that provides a compatible JVM, [see `RunOnJDK.md`](https://github.com/graalvm/graaljs/tree/master/docs/user/RunOnJDK.md).
We test and support GraalVM JavaScript currently in full extent on Linux AMD64, Linux AArch64, MacOS, and Windows.

## GraalVM JavaScript Reference Manual

A reference manual for GraalVM JavaScript is available on the [GraalVM website](https://www.graalvm.org/reference-manual/js/).

## Stay connected with the community

See [graalvm.org/community](https://www.graalvm.org/community/) on how to stay connected with the development community.
The channel _graaljs_ on [graalvm.slack.com](https://www.graalvm.org/slack-invitation) is a good way to get in touch with us.

## Licence

GraalVM JavaScript is available under the following license:

* [The Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL)


