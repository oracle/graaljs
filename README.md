[![Join the chat at https://gitter.im/graalvm/graal-core](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graalvm/graal-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A high performance implementation of the JavaScript programming language.
Built on the GraalVM by Oracle Labs.

The goals of Graal JavaScript are:

* Execute JavaScript code with best possible performance
* Full compatibility with the latest ECMAScript specification
* Support Node.js applications, including native packages
* Allow simple upgrading from Nashorn or Rhino based applications
* [Fast interoperability](https://www.graalvm.org/docs/reference-manual/polyglot/) with Java, Scala, or Kotlin, or with other GraalVM languages like Ruby, Python, or R
* Be embeddable in systems like [Oracle RDBMS](https://oracle.github.io/oracle-db-mle/) or MySQL


## Getting Started
See the documentation on the [GraalVM website](https://www.graalvm.org/docs/getting-started/) how to install and use Graal JavaScript.

```
$ $GRAALVM/bin/js
> print("Hello JavaScript");
Hello JavaScript
>
```

## Documentation

Extensive documentation is available in [`docs`](docs), for [`users`](docs/user) and [`contributors`](docs/contributor) of the engine.

For instance, a guide how to build Graal JavaScript from source code can be found in [`Building.md`](docs/Building.md).

## Current Status

Graal JavaScript is compatible with the ECMAScript 2017 specification.
New features, e.g. for the upcoming 2018 edition, are added frequently.
In addition, some popular extensions of other engines are supported, see [`JavaScriptCompatibility.md`](docs/user/JavaScriptCompatibility.md).

Graal JavaScript can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelyhood that your application will run out of the box.
This includes npm packages with native implementations.
Note that you will need to re-compile from source with Graal JavaScript if you want to run binaries that have beeen compiled for Node.js based on V8, or any other compatible engine.

## Graal JavaScript Reference Manual

A reference manual for Graal JavaScript is available on the [GraalVM website](https://www.graalvm.org/docs/reference-manual/languages/js/).

## Stay connected with the community

See [graalvm.org/community](https://www.graalvm.org/community/) on how to stay connected with the development community.
The discussion on [gitter](https://gitter.im/graalvm/graal-core) is a good way to get in touch with us.

## Authors

The main authors of Graal JavaScript in order of joining the project are:

Andreas Woess, Christian Wirth, Danilo Ansaloni, Daniele Bonetta, Jan Stola, Jakub Podlesak, Tomas Mysik, Jirka Marsik

Additionally:

Thomas Würthinger, Josef Haider, Christian Humer

Collaborations with:

* [Institut für Systemsoftware at Johannes Kepler University Linz](http://ssw.jku.at)

and others.

## Licence

Graal JavaScript is available under the following license:

* [The Universal Permissive License (UPL), Version 1.0](https://opensource.org/licenses/UPL)


