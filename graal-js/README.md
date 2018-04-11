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
### Without Graal
```
mx js [OPTION]... [FILE]... -- [ARG]...
```
### With Graal
1. Enter the graal-js dir
2. Run `mx --dynamicimports /compiler build`
```
mx --dynamicimports /compiler --jdk jvmci js [OPTION]... [FILE]... -- [ARG]...
```

### With Internationalization API (ECMA-402) support
1. Use the following option with `mx js` to turn on the ECMA-402 features: `-Dpolyglot.js.intl-402=true`
2. To run with SVM image: extract localization data files first with `mx unpackIcuData`
3. The above command will return a directory name, that you need to pass to the binary using `-Dcom.ibm.icu.impl.ICUBinary.dataPath=...` (you still need to use the option from point 2: `-Dpolyglot.js.intl-402=true`)

## Testing
```
mx gate
```

