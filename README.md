[![Join the GraalVM Slack chat](https://img.shields.io/badge/slack-join%20chat-E01563.svg)](https://join.slack.com/t/graalvm/shared_invite/enQtNzk0NTc5MzUyNzg5LTAwY2YyODQ4MzJjMGJjZGQzMWY2ZDA3NWI3YzEzNDRlNGQ1MTZkYzkzM2JkYjIxMTY2NGQzNjUxOGQzZGExZmU)

A high performance implementation of the JavaScript programming language.
Built on the GraalVM by Oracle Labs.

The goals of GraalVM JavaScript are:

* Execute JavaScript code with best possible performance
* [Full compatibility with the latest ECMAScript specification](docs/user/JavaScriptCompatibility.md)
* Support Node.js applications, including native packages ([check](https://www.graalvm.org/docs/reference-manual/compatibility/))
* Allow simple upgrading from [Nashorn](docs/user/NashornMigrationGuide.md) or [Rhino](docs/user/RhinoMigrationGuide.md) based applications
* [Fast interoperability](https://www.graalvm.org/docs/reference-manual/polyglot/) with Java, Scala, or Kotlin, or with other GraalVM languages like Ruby, Python, or R
* Be embeddable in systems like [Oracle RDBMS](https://oracle.github.io/oracle-db-mle/) or MySQL


## Getting Started
See the documentation on the [GraalVM website](https://www.graalvm.org/docs/getting-started/) how to install and use GraalVM JavaScript.

```
$ $GRAALVM/bin/js
> print("Hello JavaScript");
Hello JavaScript
>
```

The preferred way to run GraalVM JavaScript is from a [GraalVM](https://www.graalvm.org/downloads/).
If you prefer running it on a stock JVM, please have a look at the documentation in [`RunOnJDK.md`](docs/user/RunOnJDK.md).

## Documentation

Extensive documentation is available in [`docs`](docs), for [`users`](docs/user) and [`contributors`](docs/contributor) of the engine.

For contributors, a guide how to build GraalVM JavaScript from source code can be found in [`Building.md`](docs/Building.md).

## Current Status

GraalVM JavaScript is compatible with the [ECMAScript 2019 specification](http://www.ecma-international.org/ecma-262/10.0/index.html).
New features, e.g. for the upcoming 2020 edition, are added frequently.
In addition, some popular extensions of other engines are supported, see [`JavaScriptCompatibility.md`](docs/user/JavaScriptCompatibility.md).

GraalVM JavaScript can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelyhood that your application will run out of the box.
This includes npm packages with native implementations.
Note that you will need to re-compile from source with GraalVM JavaScript if you want to run binaries that have been compiled for Node.js based on V8, or any other compatible engine.

### Compatibility on Operating Systems

The core JavaScript engine is a Java application and is thus in principle compatible with every operating system that provides a compatible JVM, [see `RunOnJDK.md`](docs/user/RunOnJDK.md).
We test and support GraalVM JavaScript currently in full extent on Linux and MacOS.
For Windows, a preliminary preview version is available.

Some features, including the Node.js support, are currently not supported on all platforms (e.g. Windows).

## GraalVM JavaScript Reference Manual

A reference manual for GraalVM JavaScript is available on the [GraalVM website](https://www.graalvm.org/docs/reference-manual/languages/js/).

## Stay connected with the community

See [graalvm.org/community](https://www.graalvm.org/community/) on how to stay connected with the development community.
The discussion on [gitter](https://gitter.im/graalvm/graal-core) is a good way to get in touch with us.

## Authors

The main authors of GraalVM JavaScript in order of joining the project are:

Andreas Woess, Christian Wirth, Danilo Ansaloni, Daniele Bonetta, Jan Stola, Jakub Podlesak, Tomas Mysik, Jirka Marsik, Josef Haider

Additionally:

Thomas Würthinger, Christian Humer

Collaborations with:

* [Institut für Systemsoftware at Johannes Kepler University Linz](http://ssw.jku.at)

and others.

## Licence

GraalVM JavaScript is available under the following license:

* [The Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL)


