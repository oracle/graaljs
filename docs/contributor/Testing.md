GraalVM JavaScript publishes tests that can be used to verify the correctness of commits.

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
Note that we maintain a blacklist of tests we know not to pass currently.
The tests runners suggest to automatically update those lists based on the tests results (add or remove known-to-fail tests from those lists).

## Unit tests
GraalVM JavaScript is also published with its own unit tests.
To execute them use:

```
$ mx unittest com.oracle.truffle.js
```

This should result in a passing result as well (e.g. `OK (435 tests)`).

Consider contributing your own unittests when working on GraalVM JavaScript.

