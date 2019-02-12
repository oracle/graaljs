# Graal JavaScript Options

GraalVM JavaScript can be configured with several options provided when starting the engine.
Additional options control the behaviour of the `js` binary launcher.

## Launcher options

The `js` launcher can be configured with the following options:

### -e, --eval CODE 	

Evaluate the passed in JavaScript source code, then exit the engine.

```
js -e 'print(1+2);'
```

### -f, --file FILE
	
Load and executed the provided script file.

```
js -f myfile.js
```

Note that the `-f` flag is optional and can be omitted in most cases, as any additional argument to `js` will be interpreted as file anyway.

### --version

Prints the version information of Graal JavaScript and then exits.

### --strict

Executes the engine in JavaScript's _strict mode_.

## Engine options

The engine options configure the behavior of the JavaScript engine.
Depending on how the engine is started, the options can be passed in different ways:

To the launcher, the options are passed with `--js.<option-name>=<value>`:

```
js --js.ecmascript-version=6
```

When started from Java via GraalVM's Polyglot feature, the options are passed to the `Context` object:

```java
Context context = Context.newBuilder("js")
                         .option("js.ecmascript-version", "6")
                         .build();
context.eval("js", "42");
```

### ecmascript-version

Emulate a specific version of the ECMAScript specification.
Expects an integer value (6-10), default is the latest (draft) version of the specification available.
Graal.js implements features from the latest specification and thus sets the defaults to `latest`.
For production settings, it is recommended to set the `ecmascript-version` to an existing, finalized version of the specification.
The versions are numbered with ascending integer values, with `5` for ECMAScript 5.x, `6` for ECMAScript 2015 (formerly ECMAScript 6), and counting from there.

### intl-402

Enable ECMAScript's [Internationalization API](https://tc39.github.io/ecma402/).
Expects a Boolean value, the default is `false`.

### strict

Enables JavaScript's strict mode for all scripts.
Expects a boolean value, default is false.

