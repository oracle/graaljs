---
layout: docs
toc_group: js
link_title: Using JavaScript Modules and Packages
permalink: /reference-manual/js/Modules/
---
# Using JavaScript Modules and Packages in GraalVM JavaScript

GraalVM JavaScript is compatible with the latest ECMAScript standard, and can be executed in a variety of embedding scenarios such as Java-based applications or Node.js.
Depending on the GraalVM's JavaScript embedding scenario, JavaScript packages and modules may be used in different ways.

## Node.js

GraalVM ships with a specific Node.js version that it is compatible with.
Applications can therefore freely import and use NPM packages compatible with the supported Node.js version, including CommonJS, ES modules, and modules that use native bindings.
To check and verify the Node.js version supported by GraalVM, simply run `bin/node --version`.

## Java-based applications (`Context` API)

When embedded in a Java application (using the `Context` API), GraalVM JavaScript can execute JavaScript applications and modules that _do not_ depend on Node.js' built-in modules such as `'fs'`, `'events'`, or `'http'` or Node.js-specific functions such as `setTimeout()` or `setInterval()`.
On the other hand, modules that depend on such Node.js builtins cannot be loaded in a GraalVM polyglot `Context`.

Supported NPM packages can be used in a GraalVM JavaScript `Context` using one of the following approaches:

1. Using a package bundler.
For example, to combine multiple NPM packages in a single JavaScript [Source file](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html).
2. Using ES modules on the local FileSystem.
Optionally, a custom [Truffle FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html) can be used to configure how files are resolved.

By default, a Java `Context` does not support loading modules using the CommonJS `require()` function.
This is because `require()` is a Node.js built-in function, and is not part of the ECMAScript specification.
Experimental support for CommonJS modules can be enabled through the `js.commonjs-require` option as described below.

### ECMAScript Modules (ESM)

GraalVM JavaScript supports the full ES modules specification, including `import` statements, dynamic import of modules using `import()`, and advanced features such as [top-level `await`](https://github.com/tc39/proposal-top-level-await).
ECMAScript modules can be loaded in a `Context` simply by evaluating the module sources.
GraalVM JavaScript loads ECMAScript modules based on their file extension.
Therefore, any ECMAScript module should have file name extension `.mjs`.
Alternatively, the module [Source](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html) should have Mime type `"application/javascript+module"`.

As an example, let's assume that you have a file named `foo.mjs` containing the following simple ES module:
```js
export class Foo {

    square(x) {
        return x * x;
    }
}
```

The ES module can be loaded in a polyglot `Context` in the following way:
```java
public static void main(String[] args) throws IOException {

    String src = "import {Foo} from '/path/to/foo.mjs';" +
                 "const foo = new Foo();" +
                 "console.log(foo.square(42));";

    Context cx = Context.newBuilder("js")
                .allowIO(true)
                .build();

	cx.eval(Source.newBuilder("js", src, "test.mjs").build());
}
```

Note that the ES module file has `.mjs` extension.
Also note that the `allowIO()` option is provided to enable IO access.
More examples of ES modules usage are available [here](https://github.com/oracle/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/interop/ESModuleTest.java).

#### Experimental module namespace exports

The `--js.esm-eval-returns-exports` experimental option can be used to expose the ES module namespace exported object to a Polyglot `Context`.
This can be handy when an ES module is used directly from Java:
```java
public static void main(String[] args) throws IOException {

    String code = "export const foo = 42;";

    Context cx = Context.newBuilder("js")
                .allowIO(true)
                .option("js.esm-eval-returns-exports", "true")
                .build();

    Source source = Source.newBuilder("js", code)
                .mimeType("application/javascript+module")
                .build();

    Value exports = cx.eval(source);
    // now the `exports` object contains the ES module exported symbols.
    System.out.println(exports.getMember("foo").toString()); // prints `42`
}
```
The option is disabled by default.


### Truffle FileSystem

By default, GraalVM JavaScript uses the built-in FileSystem of the polyglot `Context` to load and resolve ES modules.
A [FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html) can be used to customize the ES modules loading process.
For example, a custom FileSystem can be used to resolve ES modules using URLs:

```java
Context cx = Context.newBuilder("js").fileSystem(new FileSystem() {

	private final Path TMP = Paths.get("/some/tmp/path");

    @Override
    public Path parsePath(URI uri) {
    	// If the URL matches, return a custom (internal) Path
    	if ("http://localhost/foo".equals(uri.toString())) {
        	return TMP;
		} else {
        	return Paths.get(uri);
        }
    }

	@Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    	if (TMP.equals(path)) {
        	String moduleBody = "export class Foo {" +
                            "        square(x) {" +
                            "            return x * x;" +
                            "        }" +
                            "    }";
            // Return a dynamically-generated file for the ES module.
            return createByteChannelFrom(moduleBody);
        }
    }

    /* Other FileSystem methods not shown */

}).allowIO(true).build();

String src = "import {Foo} from 'http://localhost/foo';" +
             "const foo = new Foo();" +
             "console.log(foo.square(42));";

cx.eval(Source.newBuilder("js", src, "test.mjs").build());
```

In this simple example, a custom FileSystem is used to load a dynamically-generated ES module when an application attempts to import the `http://localhost/foo` URL.

A complete example of a custom Truffle FileSystem to load ES modules can be found [here](https://github.com/oracle/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/builtins/ImportWithCustomFsTest.java).

### CommonJS Modules (CJS)

By default, the `Context` API does not support CommonJS modules, and has no built-in `require()` function.
In order to be loaded and used from a `Context` in Java, a CommonJS module needs to be _bundled_ into a self-contained JavaScript source file.
This can be done using one of the many popular open-source bundling tools such as Parcel, Browserify, and Webpack.
Experimental support for CommonJS modules can be enabled through the `js.commonjs-require` option as described below.

#### Experimental support for CommonJS NPM modules in the `Context` API

The `js.commonjs-require` option provides a built-in `require()` function that can be used to load NPM-compatible CommonJS modules in a JavaScript `Context`.
Currently, this is an experimental feature not for production usage.

To enable CommonJS support, a JavaScript context can be created in the following way:
```java
Map<String, String> options = new HashMap<>();
// Enable CommonJS experimental support.
options.put("js.commonjs-require", "true");
// (optional) folder where the NPM modules to be loaded are located.
options.put("js.commonjs-require-cwd", "/path/to/root/folder");
// (optional) initialization script to pre-define globals.
options.put("js.commonjs-global-properties", "./globals.js");
// (optional) Node.js built-in replacements as a comma separated list.
options.put("js.commonjs-core-modules-replacements",
            "buffer:buffer/," +
            "path:path-browserify");
// Create context with IO support and experimental options.
Context cx = Context.newBuilder("js")
                            .allowExperimentalOptions(true)
                            .allowIO(true)
                            .options(options)
                            .build();
// Require a module
Value module = cx.eval("js", "require('some-module');");
```

The `"js.commonjs-require-cwd"` option can be used to specify the main folder where NPM packages have been installed.
As an example, this can be the folder where the `npm install` command was executed, or the folder containing your main `node_modules` folder.
Any NPM module will be resolved relative to that folder, including the `"js.commonjs-global-properties"` file and any built-in replacement specified using `"js.commonjs-core-modules-replacements"`.

##### Differences with Node.js built-in `require()` function

The `Context` built-in `require()` function can load regular NPM modules implemented in JavaScript, but cannot load native NPM modules.
The built-in `require()` relies on the [FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html), therefore I/O access needs to be enabled at context creation time using the [`allowIO` option](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.Builder.html#allowIO-boolean-).
The built-in `require()` aims to be largely compatible with Node.js, and we expect it to work with any NPM module that would work in a browser (e.g., created using a package bundler).

##### Installing an NPM module to be used via the `Context` API

In order to be used from a JavaScript `Context`, an NPM module needs to be installed to a local folder.
This can be done using GraalVM JavaScript's `npm install` command like one would normally do for Node.js applications.
At runtime, the option `js.commonjs-require-cwd` can be used to specify the main installation folder for NPM packages.
The `require()` built-in function will resolve packages according to the default Node.js' [package resolution protocol](https://nodejs.org/api/modules.html#modules_all_together) starting from the directory specified via `js.commonjs-require-cwd`.
When no directory is provided with the option, the current working directory of the application will be used.

##### Node.js core modules mockups

Some JavaScript applications or NPM modules might need functionalities that are available in Node.js' built-in modules (e.g., `'fs'` and `'buffer'`, etc.).
Such modules are not available in the `Context` API.
Thankfully, the Node.js community has developed high-quality JavaScript implementations for many Node.js core modules (e.g., the ['buffer'](https://www.npmjs.com/package/buffer) module for the browser).
Such alternative module implementations can be exposed to a JavaScript `Context` using the `js.commonjs-core-modules-replacements` option, in the following way:
```java
options.put("js.commonjs-core-modules-replacements", "buffer:my-buffer-implementation");
```

As the code suggests, the option instructs the GraalVM JavaScript runtime to load a module called `my-buffer-implementation` when an application attempts to load the Node.js `'buffer'` built-in module using `require('buffer')`.

##### Global symbols pre-initialization

An NPM module or a JavaScript application might expect certain global properties to be defined in the global scope.
For example, applications or modules might expect the `Buffer` global symbol to be defined in the JavaScript global object.
The option `js.commonjs-global-properties` can be used to pre-initialize such global symbols using the `Context` API.
The option can be used in the following way:
```java
options.put("js.commonjs-global-properties", "./globals.js");
```
Once specified, the file `globals.js` will be loaded at context creation time. That is, before any JavaScript source execution.
An example `globals.js` file might perform the following initialization steps:
```java
// define an empty object called 'process'
globalThis.process = {}
// define the 'Buffer' global symbol
globalThis.Buffer = require('some-buffer-implementation').Buffer;
```
The file will be executed at context creation time, and `Buffer` will be available as a global symbol in all new `Context` instances.
