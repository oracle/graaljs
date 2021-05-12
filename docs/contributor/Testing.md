GraalVM JavaScript publishes tests that can be used to verify the correctness of commits.
A pull request against our codebase can only be accepted when all tests pass.

## Test engines
Test runners for different externally published test sets are provided.

ECMAScript's official testsuite:
```
$ mx test262 gate
```

Tests provided by the V8 project:
```
$ mx testv8 gate
```

All test runners should result in 0 (unexpected) failures.
Note that we maintain an ignore list of tests we know not to pass currently.
A test not expected to pass typically means it tests a feature not yet supported by our engine.
The tests runners suggest to automatically update those lists based on the tests results (add or remove known-to-fail tests from those lists).

You can run individual tests with this command:
```
$ mx test262 single=built-ins/Array/length.js
```

This allows you to debug problems when working on the engine's codebase.
Use `mx -d ...` to connect with a debugger to this process.

## Unit tests
GraalVM JavaScript is also published with its own unit tests.
To execute them use:

```
$ mx unittest com.oracle.truffle.js
```

This should result in a passing result as well (e.g. `OK (435 tests)`).

Consider contributing your own unittests when working on GraalVM JavaScript.

