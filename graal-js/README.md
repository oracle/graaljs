# Graal.js
Graal.js is a High-Performance JavaScript implementation built atop [Truffle](https://github.com/graalvm/truffle) and [Graal](https://github.com/graalvm/graal-core).
Truffle is a framework for implementing languages as self-optimizing interpreters written in Java.
Graal is a dynamic compiler that is used to generate efficient machine code from partially evaluated Truffle interpreters.

Graal.js is compatible with [ECMAScript 2017](http://www.ecma-international.org/ecma-262/8.0/index.html).

## Building
1. Clone `mx`: `git clone https://github.com/graalvm/mx.git`
2. Append the `mx` directory to your `PATH`
3. Create a work directory and enter it
4. Clone graal-js
5. Run `mx build`

## Running
### As normal Java application
To execute Graal.js as a normal Java application, execute this command:
```
mx js [OPTION]... [FILE]... -- [ARG]...
```
Note that this will execute Graal.js on your default JVM.
You will likely experience mediocre performance, as Graal.js will not use Graal to compile frequently executed JavaScript code to optimized machine code.

### With Graal as compiler
Executing Graal.js with Graal as a compiler will improve performance significantly.
Graal will compile frequently executed methods run by the Graal.js interpreter - this is called "partial evaluation".
To use Graal.js together with the Graal compiler built from source, use the following command:

1. Enter the graal-js dir
2. Run `mx --dynamicimports /compiler build`
```
mx --jdk jvmci --dynamicimports /compiler js [OPTION]... [FILE]... -- [ARG]...
```

The `graal` and `graaljs` directories should be sibling directories.

### With Internationalization API (ECMA-402) support
1. Use the following option with `mx js` to turn on the ECMA-402 features: `-Dpolyglot.js.intl-402=true`
2. To run with SVM image: extract localization data files first with `mx unpackIcuData`
3. The above command will return a directory name, that you need to pass to the binary using `-Dcom.ibm.icu.impl.ICUBinary.dataPath=...` (you still need to use the option from point 2: `-Dpolyglot.js.intl-402=true`)

## Testing
```
mx gate
```

