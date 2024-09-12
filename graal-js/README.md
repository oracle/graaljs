# GraalJS

GraalJS is a JavaScript language implementation, built on top of GraalVM.
It is ECMAScript-compliant, implemented using the [Truffle framework](https://www.graalvm.org/jdk23/graalvm-as-a-platform/language-implementation-framework/), and can be used to embed JavaScript code in Java applications.

GraalJS is a suitable replacement for projects wanting to migrate from Nashorn or Rhino to a JavaScript engine that supports new ECMAScript standards and features.

## Building

1. Clone `mx`: `git clone https://github.com/graalvm/mx.git`.
2. Append the `mx` directory to your `PATH`.
3. Create a work directory and enter it.
4. Clone GraalJS: `git clone https://github.com/oracle/graaljs`.
5. Move into the _graal-js_ subdirectory.
6. Run `mx build`.

## Running

### As a Normal Java Application

To execute GraalJS as a normal Java application, run this command:
```bash
mx js <option>... <file>... -- <arg>...
```

Note that this will run GraalJS on your default JVM.
You will likely experience mediocre performance, as GraalJS will not use the Graal JIT compiler to compile frequently executed JavaScript code to optimized machine code.

### With Graal as Compiler

Executing GraalJS with Graal as a compiler will improve performance significantly.
The Graal compiler will compile frequently executed methods run by the JavaScript interpreter - this is called "partial evaluation".
To use GraalJS together with the Graal compiler built from source, use the following command:

1. Enter the _graal-js_ subdirectory.
2. Run `mx --dynamicimports /compiler build`:
    ```bash
    mx --jdk jvmci --dynamicimports /compiler js <option>... <file>... -- <arg>...
    ```
    The `graal` and `graaljs` directories should be sibling directories.

### With Internationalization API (ECMA-262) Support

Use the following option with `mx js` to turn on the ECMA-262 features: `-Dpolyglot.js.intl-262=true`.

## Testing

To run tests, execute:
```bash
mx gate
```