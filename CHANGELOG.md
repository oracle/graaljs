# Graal.js Changelog

This changelog summarizes major changes between GraalVM versions of the Graal JavaScript (ECMAScript) language runtime.
The main focus is on user-observable behavior of the engine.

## Version 1.0.0 RC5
* Add support for `Symbol.prototype.description`, a Stage 3 proposal.
* Add support for `String.prototype.matchAll`, a Stage 3 proposal.
* Implement optional catch binding proposal, targeted for ES2019.
* removed legacy `NashornExtensions` option, use `--js.nashorn-compat` instead.
* Nashorn compatibility mode was turned on for ScriptEngine provided by Graal JavaScript.

## Version 1.0.0 RC4
* Added stack trace limit option (--js.stack-trace-limit).
* Enable SharedArrayBuffers by default.
* Provide $EXEC for Nashorn compatibility in scripting mode.
* Provide Java.isScriptFunction, Java.isScriptObject, Java.isJavaMethod, and Java.isJavaFunction in Nashorn compatibility mode.
* Provide support to access getters and setters like a field, in Nashorn compatibility mode.
* Provide top-level package globals in Nashorn compatibility mode: `java`, `javafx`, `javax`, `com`, `org`, `edu`.
* Provide Java.extend, Java.super, and `new Interface|AbstractClass(fn|obj)` in Nashorn compatibility mode.
* Provide `java.lang.String` methods on string values, in Nashorn compatibility mode.
* Provide `JavaImporter` class in Nashorn compatibility mode.
* Provide `JSAdapter` class only in Nashorn compatibility mode.

## Version 1.0.0 RC3
* Added support for BigInt arithmetic expressions.
* Provide a flag for a Nashorn compatibility mode (--js.nashorn-compat).
* Rename flag for V8 compatibility mode (to --js.v8-compat).

## Version 1.0.0 RC2
* Enabled code sharing between Contexts with the same Engine.
* Updated Node.js to 8.11.1.

## Version 1.0.0 RC1
* LICENSE set to The Universal Permissive License (UPL), Version 1.0.

## Version 0.33

* Added object rest/spread support.
* Added support for async generators.
* Unified Polyglot primitives across all Truffle languages; e.g., rename `Interop` builtin to `Polyglot`.

