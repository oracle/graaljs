---
layout: docs
toc_group: js
link_title: Node.js Runtime
permalink: /reference-manual/js/NodeJS/
---
# Node.js Runtime

GraalVM can run unmodified Node.js applications.
Applications can freely import and use NPM packages, including native ones.

For the differences between running the `node` native launcher and accessing Node.js/npm modules/ECMAScript modules from a Java Context, see [NodeJSVSJavaScriptContext](NodeJSVSJavaScriptContext.md).

## Installing Node.js Component

Since GraalVM 21.1, the Node.js support is packaged in a separate GraalVM component.
It can be installed with the _GraalVM Updater_.

```shell
$GRAALVM/bin/gu install nodejs
```

This installs the `node` and `npm` binaries in the `$GRAALVM/bin` directory.

### Polyglot Support in Node.js

The Node.js component is able to use the polyglot language interoperability (flag: `--polyglot`) with other installed polyglot languages.
This feature is available by default in JVM mode (flag: `--jvm`).
For polyglot access to the Ruby language, you can e.g. use this command:

```shell
$GRAALVM_HOME/bin/node --jvm --polyglot -e 'var array = Polyglot.eval("ruby", "[1,2,42,4]"); console.log(array[2]);'
```

To use the polyglot capabilities of `node` in the native mode (flag: `--native`), the `libpolyglot` needs to be rebuilt first.
For this, the `native-image` component and the other languages need to be installed first, before the image can be rebuilt:

```shell
$GRAALVM_HOME/bin/gu install native-image
$GRAALVM_HOME/bin/gu rebuild-images libpolyglot
```

After a successfull rebuild, the polyglot access is also available in the `--native` mode:

```shell
$GRAALVM_HOME/bin/node --native --polyglot -e 'var array = Polyglot.eval("ruby", "[1,2,42,4]"); console.log(array[2]);'
```

## Running Node.js Applications

To run Node.js-based applications, use the `node` launcher in the GraalVM distribution:
```shell
$GRAALVM_HOME/bin/node [options] [filename] [args]
```

GraalVM's Node.js runtime is based on a recent version of Node.js, and runs the
GraalVM JavaScript engine instead of Google V8. Thus, some internal features (e.g., VM-internal statistics, configuration, profiling, debugging, etc.) are unsupported, or supported with potentially different behavior.

The `node` command is largely compatible with Node.js, and features additional GraalVM-specific functionalities (e.g., interoperability with Java and all other GraalVM languages).
A list of available options can be obtained with `node --help`.

## Installing Packages Using `npm`

To install a Node.js package, you can use the `npm` launcher from the GraalVM's `/bin` folder.
The `npm` command is equivalent to the default NPM command, and supports most of its options.

An NPM package can be installed with:
```shell
$GRAALVM_HOME/bin/npm install <package>
```

As the `npm` command of GraalVM is largely compatible with NPM, packages will be installed in the `node_modules` folder, as expected.

### Installing `npm` Packages Globally

Node packages can be installed globally using `npm` and the `-g` option.
By default, `npm` installs global packages (links to their executables) in the path where the `node` executable is installed, typically `NODE/bin`.
In GraalVM, while there is a `node` executable in `GRAALVM/bin`, this is just a link to the actual executable in the `GRAALVM/jre/languages/js/bin` folder.
That folder is where global packages are installed.
You might want to add that directory to your `$PATH` if you regularly use globally installed packages, especially their command line interfaces.

Another option is to specify the global installation folder of `npm` by setting the `$PREFIX` environment variable, or by specifying the `--prefix` option when running `npm install`.
For example, the following command will install global packages in the `/foo/bar` folder:
```shell
$GRAALVM_HOME/bin/npm install --prefix /foo/bar -g <package>
```
More details about `prefix` can be found in the [official NPM documentation](https://docs.npmjs.com/cli/prefix.html).
