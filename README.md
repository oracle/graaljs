A high performance implementation of the JavaScript programming language.
Built on the GraalVM by Oracle Labs.

## Getting Started
See the documentation at [graalvm.org](http://www.graalvm.org/docs/getting-started/) how to install and use Graal.js.

## Goals
The goals of Graal.js are:

* Run idiomatic JavaScript code as fast as possible
* Provide full compatibility with Node.js applications, including native packages
* Add fast and low-overhead interoperability with languages natively supported on the JVM, e.g., Java, Scala, or Kotlin
* Add fast and low-overhead interoperability with other GraalVM languages like Ruby, Python, or R
* Provide new tooling such as debuggers and monitoring
* All while being fully standard compliant to the latest ECMAScript specification (2017, currently)

## Documentation

Extensive documentation is available in [`docs`](docs).

For instance, a guide how to build Graal.js from source code can be found in [`BUILDING-GRAAL.JS.md`](docs/BUILDING-GRAAL.JS.md).

## Current Status

Graal.js is compatible with the ECMAScript 2017 specification.
New features, e.g. for the upcoming 2018 edition, are added frequently.
In addition, some popular extensions of other engines are supported, see [`GRAAL.JS-API.md`](docs/GRAAL.JS-API.md).

Graal.js can execute Node.js applications.
It provides high compatibility with existing npm packages, with high likelyhood that your application will run out of the box.
This includes npm packages with native implementations.
Note that you will need to re-compile from source with Graal.js if you want to run binaries that have beeen compiled for Node.js based on V8, or any other compatible engine.

### Graal.js Reference Manual

A reference manual for Graal.js is available at [graalvm.org](http://www.graalvm.org/docs/reference-manual/languages/js/).

## Stay connected with the community

See [graalvm.org/community](http://www.graalvm.org/community/) on how to stay connected with the development community.
The discussion on [gitter](https://gitter.im/graalvm/graal-core) is a good way to get in touch with us.

## Authors

The main authors of Graal.js in order of joining the project are:

* Andreas Woess
* Christian Wirth
* Danilo Ansaloni
* Daniele Bonetta
* Jan Stola
* Jakub Podlesak
* Tomas Mysik
* Jirka Marsik

Additionally:

* Thomas Würthinger
* Josef Haider
* Christian Humer

Collaborations with:

* [Institut für Systemsoftware at Johannes Kepler University Linz](http://ssw.jku.at)

And others.

## Licence

Graal.js is available under the following license:

* The Universal Permissive License (UPL), Version 1.0; https://opensource.org/licenses/UPL

## Attribution

Graal.js contains code derived from OpenJDK Nashorn, Node.js, and V8.

