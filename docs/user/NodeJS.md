---
layout: docs
toc_group: js
link_title: Node.js Runtime
permalink: /reference-manual/js/NodeJS/
---
# Node.js Runtime

GraalVM can run unmodified Node.js applications.
Applications can freely import and use NPM packages, including native ones.

To enable the GraalVM Node.js runtime, install the Node.js distribution based on Oracle GraalVM or GraalVM Community Edition for your operating system.

1. Navigate to [GitHub releases](https://github.com/oracle/graaljs/releases/) and download Node.js.

2. Unzip the archive:

    > Note: If you are using macOS Catalina and later, first remove the quarantine attribute:
    ```shell
    sudo xattr -r -d com.apple.quarantine <archive>.tar.gz
    ```
    Unzip:
    ```shell
    tar -xzf <archive>.tar.gz
    ```
    Alternatively, open the file in the Finder.

3. Check the version to see if the runtime is active:
    ```shell
    ./path/to/bin/node --version
    ```

The Node.js standalone provides `node` and `npm` launchers.
The `npm` command is equivalent to the default Node.js command and supports all Node.js APIs.

For the differences between running the `node` native launcher and accessing Node.js NPM modules or ECMAScript modules from a Java Context, see [NodeJSVSJavaScriptContext](NodeJSVSJavaScriptContext.md).

## Running Node.js Applications

To run a Node.js-based application, use the `node` launcher:
```shell
node [options] [filename] [args]
```

GraalVM's Node.js runtime is based on a recent version of Node.js, and runs the GraalVM JavaScript engine instead of Google V8. 
Thus, some internal features (e.g., VM-internal statistics, configuration, profiling, debugging, etc.) are unsupported, or supported with potentially different behavior.

The `node` command is largely compatible with Node.js, and features additional GraalVM-specific functionalities (e.g., interoperability with Java and all other GraalVM languages).
A list of available options can be obtained with `node --help`.

## Installing Packages Using `npm`

To install a Node.js package, use the `npm` launcher.
The `npm` command is equivalent to the default NPM command, and supports most of its options.

An NPM package can be installed with:
```shell
npm install <package>
```

As the `npm` command of GraalVM Node.js is largely compatible with NPM, packages will be installed in the `node_modules` folder, as expected.

### Installing `npm` Packages Globally

Node packages can be installed globally using `npm` and the `-g` option.
By default, `npm` installs global packages (links to their executables) in the path where the `node` executable is installed, typically `NODE/bin`.
That folder is where global packages are installed.
You might want to add that directory to your `$PATH` if you regularly use globally installed packages, especially their command line interfaces.

Another option is to specify the global installation folder of `npm` by setting the `$PREFIX` environment variable, or by specifying the `--prefix` option when running `npm install`.
For example, the following command will install global packages in the `/foo/bar` folder:
```shell
npm install --prefix /foo/bar -g <package>
```
More details about `prefix` can be found in the [official NPM documentation](https://docs.npmjs.com/cli/prefix.html).
