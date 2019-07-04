# GraalVM JavaScript
GraalVM JavaScript is a High-Performance JavaScript implementation built atop the [Truffle Language Implementation Framework](https://github.com/oracle/graal/tree/master/truffle) and the [GraalVM compiler](https://github.com/oracle/graal).
Truffle is a framework for implementing languages as self-optimizing interpreters written in Java.
Graal is a dynamic compiler that is used to generate efficient machine code from partially evaluated Truffle interpreters.

GraalVM JavaScript is compatible with the releases of the ECMAScript specification, from version 5 up to [ECMAScript 2018](http://www.ecma-international.org/ecma-262/9.0/index.html) and the latest ECMAScript 2019.
Features of new ECMAScript proposals are added regularly but need to be activated by flags.

## Building
1. Clone `mx`: `git clone https://github.com/graalvm/mx.git`
2. Append the `mx` directory to your `PATH`
3. Create a work directory and enter it
4. Clone graal-js
5. Move into the graal-js subdirectory
6. Run `mx build`

## Running
### As normal Java application
To execute GraalVM JavaScript as a normal Java application, execute this command:
```
mx js [OPTION]... [FILE]... -- [ARG]...
```
Note that this will execute GraalVM JavaScript on your default JVM.
You will likely experience mediocre performance, as GraalVM JavaScript will not use Graal to compile frequently executed JavaScript code to optimized machine code.

### With Graal as compiler
Executing GraalVM JavaScript with Graal as a compiler will improve performance significantly.
Graal will compile frequently executed methods run by the GraalVM JavaScript interpreter - this is called "partial evaluation".
To use GraalVM JavaScript together with the Graal compiler built from source, use the following command:

1. Enter the graal-js dir
2. Run `mx --dynamicimports /compiler build`
```
mx --jdk jvmci --dynamicimports /compiler js [OPTION]... [FILE]... -- [ARG]...
```

The `graal` and `graaljs` directories should be sibling directories.

### With Internationalization API (ECMA-402) support
Use the following option with `mx js` to turn on the ECMA-402 features: `-Dpolyglot.js.intl-402=true`

## Testing
```
mx gate
```

