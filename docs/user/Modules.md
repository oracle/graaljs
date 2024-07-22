---
layout: docs
toc_group: js
link_title: Using JavaScript Modules and Packages
permalink: /reference-manual/js/Modules/
---

# Using JavaScript Modules and Packages in GraalJS

GraalJS is compatible with the latest ECMAScript standard, and can be run in a variety of Java-based embedding scenarios.
Depending on the embedding, JavaScript packages and modules may be used in different ways.

## Java Embedding via `Context` API

When embedded in a Java application (using the [`Context` API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html)), GraalJS can execute JavaScript applications and modules that _do not_ depend on Node.js' built-in modules such as `'fs'`, `'events'`, or `'http'` or Node.js-specific functions such as `setTimeout()` or `setInterval()`.
On the other hand, modules that depend on such Node.js builtins cannot be loaded in a GraalVM polyglot `Context`.

Supported NPM packages can be used in a JavaScript `Context` using one of the following approaches:

1. Using a package bundler.
For example, to combine multiple NPM packages in a single JavaScript [Source file](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html).
2. Using ECMAScript (ES) modules on the local FileSystem.
Optionally, a custom [Truffle FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html) can be used to configure how files are resolved.

By default, a Java `Context` does not load modules using the CommonJS `require()` function.
This is because `require()` is a Node.js built-in function, and is not part of the ECMAScript specification.
Experimental support for CommonJS modules can be enabled through the `js.commonjs-require` option as described below.

### ECMAScript Modules (ESM)

GraalJS supports the full ES modules specification, including `import` statements, dynamic modules import using `import()`, and advanced features such as [top-level `await`](https://github.com/tc39/proposal-top-level-await).

ECMAScript modules can be loaded in a `Context` simply by evaluating the module sources. 
GraalJS loads ECMAScript modules based on their file extension. 
Therefore, any ECMAScript module should have file name extension _.mjs_. 
Alternatively, the module [Source](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html) should have MIME type `"application/javascript+module"`.

As an example, let's assume that you have a file named _foo.mjs_ containing the following simple ES module:
```js
export class Foo {

    square(x) {
        return x * x;
    }
}
```

This ES module can be loaded in a polyglot `Context` in the following way:
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

Note that the ES module file has _.mjs_ extension.
Also note that the `allowIO()` option is provided to enable IO access.
More examples of ES modules usage are available [here](https://github.com/oracle/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test/src/com/oracle/truffle/js/test/interop/ESModuleTest.java).

#### Module namespace exports

The `--js.esm-eval-returns-exports` option (false by default) can be used to expose the ES module namespace exported object to a Polyglot `Context`.
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

### Truffle FileSystem

By default, GraalJS uses the built-in FileSystem of the polyglot `Context` to load and resolve ES modules.
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

### CommonJS Modules

By default, the `Context` API does not support CommonJS modules, and has no built-in `require()` function.
In order to be loaded and used from a `Context` in Java, a CommonJS module needs to be _bundled_ into a self-contained JavaScript source file.
This can be achieved using one of the many popular open-source bundling tools such as Parcel, Browserify, and Webpack.
Experimental support for CommonJS modules can be enabled through the `js.commonjs-require` option as described below.

#### Experimental support for CommonJS NPM modules in the `Context` API

The `js.commonjs-require` option provides a built-in `require()` function that can be used to load NPM-compatible CommonJS modules in a JavaScript `Context`.
Currently, this is an experimental feature and not for production usage.

To enable CommonJS support, a JavaScript context can be created in the following way:
```java
Map<String, String> options = new HashMap<>();
// Enable CommonJS experimental support.
options.put("js.commonjs-require", "true");
// (optional) directory where the NPM modules to be loaded are located.
options.put("js.commonjs-require-cwd", "/path/to/root/directory");
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
As an example, this can be the directory where the `npm install` command was executed, or the directory containing your main _node\_modules/_ directory.
Any NPM module will be resolved relative to that directory, including any built-in replacement specified using `"js.commonjs-core-modules-replacements"`.

##### Differences with Node.js built-in `require()` function

The `Context` built-in `require()` function can load regular NPM modules implemented in JavaScript, but cannot load native NPM modules.
The built-in `require()` relies on the [FileSystem](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/io/FileSystem.html), therefore I/O access needs to be enabled at context creation time using the [`allowIO` option](https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/Context.Builder.html#allowIO-boolean-).
The built-in `require()` aims to be largely compatible with Node.js, and we expect it to work with any NPM module that would work in a browser (for example, created using a package bundler).

##### Installing an NPM module to be used via the `Context` API

To be used from a JavaScript `Context`, an NPM module needs to be installed to a local directory, for example, by running the `npm install` command.
At runtime, the option `js.commonjs-require-cwd` can be used to specify the main installation directory for NPM packages.
The `require()` built-in function resolves packages according to the default Node.js' [package resolution protocol](https://nodejs.org/api/modules.html#modules_all_together) starting from the directory specified via `js.commonjs-require-cwd`.
When no directory is provided with the option, the current working directory of the application will be used.

##### Node.js core modules mockups

Some JavaScript applications or NPM modules might need functionalities that are available in Node.js' built-in modules (for example, `'fs'` and `'buffer'`).
Such modules are not available in the `Context` API.
Thankfully, the Node.js community has developed high-quality JavaScript implementations for many Node.js core modules (for example, the ['buffer'](https://www.npmjs.com/package/buffer) module for the browser).
Such alternative module implementations can be exposed to a JavaScript `Context` using the `js.commonjs-core-modules-replacements` option, in the following way:
```java
options.put("js.commonjs-core-modules-replacements", "buffer:my-buffer-implementation");
```

As the code suggests, the option instructs GraalJS to load a module called `my-buffer-implementation` when an application attempts to load the Node.js `buffer` built-in module using `require('buffer')`.

##### Global symbols pre-initialization

An NPM module or a JavaScript application might expect certain global properties to be defined in the global scope.
For example, applications or modules might expect the `Buffer` global symbol to be defined in the JavaScript global object.
To this end, the application user code can use `globalThis` to patch the application's global scope:
```java
// define an empty object called 'process'
globalThis.process = {};
// define the 'Buffer' global symbol
globalThis.Buffer = require('some-buffer-implementation').Buffer;
// import another module that might use 'Buffer'
require('another-module');
```

### Related Documentation

* [GraalJS Compatibility](JavaScriptCompatibility.md)