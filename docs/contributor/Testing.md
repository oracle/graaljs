# Testing GraalJS

GraalJS publishes tests that can be used to verify the correctness of commits.
A pull request against our codebase can only be accepted when all tests pass.

Before running tests, make sure you have finished rebuilding the suite.
```bash
cd graal-js
# build GraalJS only
mx build
# build GraalJS with GraalWasm
mx --dy /wasm build
```

## Test engines
Test runners for different externally published test sets are provided.

ECMAScript's official testsuite:
```bash
mx test262 gate
```

Tests provided by the V8 project:
```bash
mx testv8 gate
# to include wasm-related tests as well
mx --dy /wasm testv8 gate
```

Nashorn tests:
```bash
mx testnashorn gate
```

All test runners should result in 0 (unexpected) failures.
Note that we maintain an ignore list of tests we know not to pass currently in `graal-js/test`.
A test not expected to pass typically means it tests a feature not yet supported by our engine.

```bash
# choose a specific test to run
mx test262 single=built-ins/Array/length.js
# or select multiple tests using regex
mx test262 regex='language/statements/with/.*'
```

This allows you to debug problems when working on the engine's codebase.
Use `mx -d ...` to connect with a debugger to this process.

## Unit tests
GraalJS is also published with its own unit tests.

```bash
# core unit tests
mx gate -t UnitTests
# WebAssembly interface tests
mx --dy /wasm gate -t WebAssemblyTests
```

This should result in a passing result as well (e.g. `OK (435 tests)`).

Consider contributing your own unittests when working on GraalJS.


# Testing GraalNode.js

Before running tests, make sure you have finished rebuilding the suite.
```bash
cd graal-nodejs
mx build
```

To run the Node.js tests:
```bash
# Run a specific Node.js core test suite by suite name
mx testnode es-module
mx testnode parallel
mx testnode async-hooks

# Run a single test file directly
mx node test/parallel/test-stream2-transform.js

# If you need to run a test-suite that uses native addons, build them first
mx makeinnodeenv build-addons               # addons test-suite
mx makeinnodeenv build-js-native-api-tests  # js-native-api test-suite
mx makeinnodeenv build-node-api-tests       # node-api test-suite

# Run all graal-nodejs unit tests
mx gate -t UnitTests
```
