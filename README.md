A high performance implementation of the JavaScript programming language.
Built on the GraalVM by Oracle Labs.

## Getting Started
The best way to get started with Graal.js is via the GraalVM, which includes compatible versions of everything you need as well as Graal.js.

http://www.oracle.com/technetwork/oracle-labs/program-languages/

GraalVM provides two main commands to execute applications, `bin/js` and `bin/node`.
The first, `js`, is a pure JavaScript (ECMAScript 2017) engine.
The second, `node`, executes an instance of Node.js (v8.9.4) on top of Graal.js

In addition, you can use `bin/npm` to install Node.js packages as normal with the Node package manager.

## Aim
Graal.js aims to:

* Run idiomatic JavaScript code as fast as possible
* Provide full compatibility with Node.js applications, including native packages
* Add fast and low-overhead interopability with languages natively supported on the JVM, e.g., Java, Scala, or Kotlin
* Add fast and low-overhead interopability with other GraalVM languages like Ruby, Python, or R
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

### Common questions about the status of Graal.js

#### Does the npm modules `XYZ` work?

We test a large share of npm modules, and most of them pass all their tests.
Among those we fail one or more tests, often the setup is causing the failure, not incompatibilities in the JavaScript engine.
We will publish a list of passing and/or failing modules shortly.

If you are interested in executing an npm module and it fails on Graal.js, we would like to hear about the failure and your usage scenario.
Please get in touch with us!

#### What is SubstrateVM and how does it influence JavaScript execution?

You don't need a JVM to run Graal.js.
With the SubstrateVM it is possible to produce a single, statically linked native binary executable version of Graal.js, which doesn't need any JVM to run.

This SubstrateVM version of Graal.js has startup performance and memory footprint similar to other, natively implemented JavaScript engines.
As all Java code is precompiled, the Java interop features are disabled on SubstrateVM.

If you use a binary build of GraalVM to run Graal.js, by default you will be using SubstrateVM.
You can deactivate it and use a normal JVM instead with the `--jvm` flag.

#### Can Graal.js run on a standard JVM?

It is possible to run on an unmodified JDK 9 but you will have to build Graal yourself and we recommend using GraalVM instead.

It is possible to run just the JavaScript engine (without Node.js) support on any JDK 8 or newer.
Note, however, that this will imply a performance penalty, as only the Graal compiler can optimize Graal.js execution.

## Contact

The discussion on [gitter](https://gitter.im/graalvm/graal-core) is a good way to get in touch with us.
You can also send an email to christian.wirth@oracle.com.

## Mailing list

Announcements about GraalVM, including Graal.js, are made on the [graalvm-dev](https://oss.oracle.com/mailman/listinfo/graalvm-dev) mailing list.

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

