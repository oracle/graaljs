## Building GraalVM JavaScript

This document describes how to build and run GraalVM JavaScript.

Building yourself should only be necessary if you are contributing to GraalVM JavaScript or you want to try out the latest features.
The average user will prefer the pre-built binaries as part of [GraalVM](http://www.graalvm.org/downloads/) or the GraalVM JavaScript JAR files published to Maven (see [blog post](https://medium.com/graalvm/graalvms-javascript-engine-on-jdk11-with-high-performance-3e79f968a819)).

### Prerequisites

* Python 3.8+ (required by `mx`)
* git (to download, update, and locate repositories)
* Java JDK 21 or newer

Building the Node.js support is optional, and requires additional tools, see futher below.

### Building

1. to build GraalVM JavaScript from sources you need to clone several repositories, at least `graaljs`, `mx` and `graal`. We recommend to put them in a dedicated directory:
    ```bash
    mkdir graalvm
    cd graalvm
    ```

2. clone `mx` and add it to your `PATH`:
    ```
    git clone https://github.com/graalvm/mx.git
    export PATH=$PWD/mx:$PATH
    ```
    You can put `mx` in your `graalvm` folder, but anywhere else in the system is fine as you are putting it on the `PATH` anyway.

3. clone the `graaljs` repository and enter it:
    ```bash
    git clone https://github.com/oracle/graaljs.git
    cd graaljs/graal-js
    ```
    Note that the `graaljs` repository contains two so-called suites: `graal-js` (the core JavaScript engine) and `graal-nodejs` (the Node.js project modified so it uses GraalVM JavaScript as JavaScript engine).
    For the further steps you need to be in either of those directories (we assume you cd'ed to the `graal-js` directory).

4. setup your environment:
    - assuming you build with JDK21 or newer:
        ```bash
        export JAVA_HOME=[path to JDK21+]
        ```
5. (optional) clone or update the dependent repositories:
    ```bash
    mx sforceimports
    ```
    This will update the `graal` repository, where different required projects (`truffle`, `sdk`, `regex`) are found - or download it when no checkout of the repository can be found in the `graalvm` directory.
    The GraalVM compiler (found in `graal/compiler/`) is not required to build GraalVM JavaScript, but is used to execute it with high performance (see below).
    This step is marked as optional, as the following step (`mx build`) will automatically clone missing repositories, for instance, when you first build GraalVM JavaScript.
    However, `mx build` won't update dependencies if they already exist, so running `mx sforceimports` is a safe bet and guarantees you are using the right commits of dependent repositories.

6. build:
    ```bash
    mx build
    ```

7. set up projects for your IDE:
   ```bash
   mx ideinit
   ```

For future updates, you will want to `git pull` in the `graaljs` repository, then call `mx sforceimports` (in `graalvm/graaljs/graal-js`) to update the `graal` repository, and then (again in the `graal-js` suite) call `mx build && mx ideinit` to build and update the IDE configuration.

After cloning all required repositories, a possible directory structure could look similar to the following pattern (the first level is the project directory, on the second level are the repositories, on the third level are the `suites` and additional directories of the repositories):
```bash
graalvm
├── graal
|   ├── compiler
|   ├── regex
|   ├── sdk
|   ├── truffle
|   `── (some more directories here)
├── graaljs
|   ├── graal-js
|   ├── graal-nodejs
|   `── docs
`── mx
```
The main requirement here is that `mx` is on the `PATH` and that the two repositories `graaljs` and `graal` are checked out as sibling directories.


### Running

To start the GraalVM JavaScript command line interpreter or run a JavaScript file:
```bash
cd graaljs/graal-js
mx js [OPTION]... [FILE]...
```

Assuming that you also built the GraalVM compiler (using the instructions in `graal/compiler/README.md`), here is how you can use it to run GraalVM JavaScript:
```bash
cd graaljs/graal-js
mx --dynamicimports /compiler js [OPTION]... [FILE]...
```


## Node.js on GraalVM JavaScript

Here we describe how to build and run Node.js on GraalVM JavaScript.


### Prerequisites

* the same as for GraalVM JavaScript
* for building Node.js, the requirements according to the Node.js documentation are:
  * `gcc` and `g++` 6.3 or newer, or
  * on macOS: Xcode Command Line Tools version 8 or newer
  * GNU Make 3.81 or newer
  * Note, that for the GraalVM JavaScript integration of Node.js, some parts of the whole Node.js ecosystem needs not be built, relaxing those requirements. Most prominently, the V8 JavaScript engine is not built in that case. We are successfully building GraalVM+Node.js currently with `gcc version 4.9.4`.


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
mx --dynamicimports /compiler node [OPTION]... [FILE]...
```


## Notes:

- `mx sforceimports` clones the `graal` repository next to `graaljs` (step 5 above)

- if you already cloned `graaljs`, you can update to a newer version of `graal` by running:
    ```bash
    cd graaljs
    git pull
    cd graal-js # or "graal-nodejs"
    mx sforceimports # updates the "graal" repository to the version imported by the current suite
    mx build
    ```

- GraalVM JavaScript depends on `truffle`, `sdk`, and `regex` provided in the `graal` repository. There is no need to build code from this repository, this is done automatically when you build GraalVM JavaScript with `mx build`

- to print the help message of both GraalVM JavaScript and Node.js on GraalVM JavaScript, pass the `--help` option:
```bash
cd graaljs/graal-js
mx js --help
```

