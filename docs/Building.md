## Building GraalVM JavaScript

This document describes how to build and run GraalVM JavaScript.

Building yourself should only be necessary if you are contributing to GraalVM JavaScript or you want to try out the latest features.
The average user will prefer the pre-built binaries as part of [GraalVM](http://www.graalvm.org/downloads/) or the GraalVM JavaScript JAR files published to Maven (see [blog post](https://medium.com/graalvm/graalvms-javascript-engine-on-jdk11-with-high-performance-3e79f968a819)).

### Prerequisites

* Python 2.7 (required by `mx`)
* git (to download, update, and locate repositories)
* Java JDK 8 or newer

### Building

1. to build GraalVM JavaScript from sources you need to clone more than one repository. We recommend to do it in a dedicated directory:
    ```bash
    mkdir graalvm
    cd graalvm
    ```

2. clone `mx` and add it to your `PATH`:
    ```
    git clone https://github.com/graalvm/mx.git
    export PATH=$PWD/mx:$PATH
    ```

3. clone the `graaljs` repository and enter it:
    ```bash
    git clone https://github.com/graalvm/graaljs.git
    cd graaljs/graal-js
    ```

4. setup your environment:
    - if you build with JDK8:
        ```bash
        export JAVA_HOME=[path to JDK8]
        ```
    - if you build with JDK9+:
        ```bash
        export JAVA_HOME=[path to JDK9+]
        export EXTRA_JAVA_HOMES=[path to JDK8]
        ```

5. build:
    ```bash
    mx build
    ```

### Running

To start the GraalVM JavaScript command line interpreter or run a JavaScript file:
```bash
cd graaljs/graal-js
mx js [OPTION]... [FILE]...
```

Assuming that you also built the GraalVM compiler (using the instructions in `graal/compiler/README.md`), here is how you can use it to run GraalVM JavaScript:
```bash
cd graaljs/graal-js
mx --dynamicimports /compiler --jdk jvmci js [OPTION]... [FILE]...
```


## Node.js on GraalVM JavaScript

Here we describe how to build and run Node.js on GraalVM JavaScript.


### Prerequisites

* the same for GraalVM JavaScript
* the same for Node.js:
  * `gcc` and `g++` 4.9.4 or newer, or
  * `clang` and `clang++` 3.4.2 or newer (macOS: latest Xcode Command Line Tools)
  * GNU Make 3.81 or newer


### Building

To build both GraalVM JavaScript and Node.js:
```bash
cd graaljs/graal-nodejs
mx build
```


### Running

To start the Node.js command line interpreter or run a Node.js file:
```bash
cd graaljs/graal-nodejs
mx node [OPTION]... [FILE]...
```

Assuming that you also built the GraalVM Compiler (using the instructions in `graal/compiler/README.md`), here is how you can use it to run Node.js on GraalVM JavaScript:
```bash
cd graaljs/graal-nodejs
mx --dynamicimports /compiler --jdk jvmci node [OPTION]... [FILE]...
```


## Notes:

- `mx` clones the `graal` repository next to `graaljs`

- if you already cloned `graaljs`, you can update to a newer version of `graal` by running:
    ```bash
    cd graaljs
    git pull
    cd graal-js # or "graal-nodejs"
    mx sforceimports # updates the "graal" repository to the version imported by the current suite
    mx build
    ```

- GraalVM JavaScript depends on `truffle` and `sdk` provided in the `graal` repository. There is no need to build code from this repository, this is done automatically when you build GraalVM JavaScript with `mx build`

- to print the help message of both GraalVM JavaScript and Node.js on GraalVM JavaScript, pass the `--help` option:
```bash
cd graaljs/graal-js
mx js --help
```

