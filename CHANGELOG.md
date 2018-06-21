# Graal.js Changelog

This changelog summarizes major changes between GraalVM versions of the Graal JavaScript (ECMAScript) language runtime.
The main focus is on user-observable behavior of the engine.

## Version 1.0

* LICENSE set to The Universal Permissive License (UPL), Version 1.0.
* Updated Node.js to 8.11.1.
* Enabled code sharing between Contexts with the same Engine.
* Added support for BigInt arithmetic expressions
* Provide a flag for a Nashorn compatibility mode (--js.nashorn-compat)
* Rename flag for V8 compatibility mode (to --js.v8-compat)

## Version 0.33

* Added object rest/spread support.
* Added support for async generators.
* Unified Polyglot primitives across all Truffle languages; e.g., rename `Interop` builtin to `Polyglot`.

