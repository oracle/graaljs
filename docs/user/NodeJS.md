# Node.js Runtime

GraalVM can run unmodified Node.js applications.
Applications can freely import and use NPM packages, including native ones.

## Running Node.js Applications

To run Node.js-based applications, use the `node` binary executable in the GraalVM distribution:
```
$GRAALVM_HOME/bin/node [options] [filename] [args]
```

GraalVM's Node.js is based on a recent version of Node.js, and runs the GraalVM JavaScript engine instead of Google V8. Thus, some internal features (e.g., VM-internal statistics or configuration, profiling, debugging, etc.) are unsupported, or supported with potentially different behavior.

The `node` executable is largely compatible with Node.js, and features additional GraalVM-specific functionalities (e.g., interoperability with Java and all other GraalVM languages).
A list of available options can be obtained with `node --help`.

## Installing Packages Using `npm`

To install a Node.js package, you can use the `npm` executable in the `$GRAALVM_HOME/bin` folder of GraalVM.
The `npm` executable is equivalent to the default NPM command, and supports most of its options.

An NPM package can be installed with:
```
$GRAALVM_HOME/bin/npm install <package>
```
As the `npm` executable of GraalVM is largely compatible with NPM, packages will be installed in the `node_modules` folder, as expected.

### Installing `npm` Packages Globally

Node packages can be installed globally using `npm` and the `-g` option.
By default, `npm` installs global packages in the path where the `node` executable is installed.
In GraalVM, this means that global packages will create files in the GraalVM folder.
To avoid writing to the GraalVM folder, the default global installation folder of `npm` can be modified by setting the `$PREFIX` environment variable, or by specifying the `--prefix` option when running `npm install`.
For example, the following command will install global packages in the `/foo/bar` folder:
```
$GRAALVM_HOME/bin/npm install --prefix /foo/bar -g <package>
```
More details about `prefix` can be found in the official npm documentation.
