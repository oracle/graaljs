You can build Graal.js from source code with this guide.

## Prerequisites

* Python 2.7 (required by `mx`)
* a C++ compiler (`gcc`, `clang`)
* git (to download and update repositories)
* Java JDK 9 or later

## Building Graal.js from source code

You need to download three separate repositories to fully build Graal.js:
* `mx` (build system)
* `js` (the actual graal.js source code)
* `graal` (to provide the Polyglot API and the Truffle framework). In most cases, this repo will be cloned automatically, see below.

Create a directory where you put in all three, e.g. `/home/username/graalvm`, and `cd` into that directory.

### build system: `mx`
First, you will need Graal's build system `mx`.

```bash
$ git clone https://github.com/graalvm/mx.git/
$ mx/mx
```

For convenience, add the mx folder to your `PATH` variable.

### Graal.js

Download the `js` JavaScript engine repository.

```bash
$ git clone https://github.com/graalvm/graaljs.git
$ cd graaljs/graal-js
$ mx sforceimports
$ mx build
```

After a successfull build, a JavaScript command line interpreter can be started with the following command (a JavaScript source file name can be added to execute that instead): 

```bash
$ mx js
```

The `mx sforceimports` step will download all required dependencies.
This will trigger a clone (or update) of the `graal` (Graal java compiler) repository from `https://github.com/graalvm/graal` into a sibling directory of `js`, named `graal`.
Graal.js depends on the `truffle` API provided in the graal repository.
There is no need to build from this repository, this will be done automatically when you build Graal.js with `mx build`.

### Graal.js with Node.js

Graal.js is compatible with Node.js.
To build Node.js powered by Graal.js run the following commands:

```bash
$ cd graaljs/graal-nodejs
$ mx sforceimports
$ mx build
```

The Node.js command line interpreter can be started with the following command (optionally, a source file name can passed to be executed):
```
$ mx node
```

