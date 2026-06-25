# Maintaining TestV8 Expectations

GraalJS runs a subset of the V8 test suite through the `mx testv8` runner.
The expected failures are recorded in `graal-js/test/testV8.json`.
Keep this file aligned with the current behavior of the engine and avoid carrying stale failure entries after tests start passing.

## Key files

- `graal-js/test/testV8.json`
- `graal-js/lib/testv8/test/mjsunit/...`
- `graal-js/src/com.oracle.truffle.js.test.external/src/com/oracle/truffle/js/test/external/resources/v8mockup.js`
- `graal-js/src/com.oracle.truffle.js.test.external/src/com/oracle/truffle/js/test/external/testv8/TestV8Runnable.java`
- `graal-js/mx.graal-js/mx_graal_js.py`

## Workflow

Before retesting, rebuild the suite so the runner uses the current Java and resource files:

```bash
cd graal-js
mx build
# If wasm or polyglot coverage is involved:
mx --dy /wasm build
```

Reproduce failures one test at a time:

```bash
mx testv8 single=<test-path>
# If wasm or polyglot coverage is involved:
mx --dy /wasm testv8 polyglot single=<test-path>
```

Classify the first concrete failure:

- Harness or intrinsic gap: a missing V8 helper, `%...` intrinsic, or d8 test harness behavior.
- Unsupported feature, proposal, or flag: the test requires functionality that GraalJS intentionally does not implement yet.
- Behavior mismatch: GraalJS implements the feature but differs in observable class, message, ordering, or semantics.
- Implementation bug: crash, internal error, assertion failure, or incorrect behavior in supported functionality.

Validate the classification against the upstream test source under `lib/testv8/test/mjsunit/...`.
Check `// Flags:` header comments as part of this step.
If a new V8 flag clearly represents an unimplemented proposal, feature set, or V8-only test mode, prefer adding that exact flag to `TestV8Runnable.UNSUPPORTED_FLAGS` instead of keeping per-test `FAIL` entries.
Keep unsupported-flag additions narrow; do not add broad flags such as `--allow-natives-syntax`, `--wasm-staging`, or optimization and tiering flags unless the entire flagged set is intentionally unsupported.

When the failure is a harness gap, add the smallest compatible shim in `v8mockup.js`.
Prefer explicit no-op implementations or `v8IgnoreResult` only for V8 internals that do not affect GraalJS correctness.

## Updating `testV8.json`

After rebuilding and rerunning the affected tests:

- Remove entries for tests that now pass.
- Remove entries for tests filtered by `UNSUPPORTED_FLAGS`; filtered tests are treated as passed by the runner and should not remain in `testV8.json`.
- Keep entries for tests that still fail, using a concrete comment that names the unsupported feature, proposal, helper, intrinsic, or behavioral mismatch.
- Replace vague comments such as `new failures`, `test update`, or date-only notes when the root cause is known.

Comment examples:

- `Uses WasmFX continuation type definitions (addCont/cont.*), not implemented in GraalWasm.`
- `Uses V8-specific wasm module serialization APIs (serializeModule/deserializeModule).`
- `Different wording of divide-by-zero trap ('integer divide by zero' vs 'divide by zero').`
- `Uses assertStringContains, but upstream mjsunit.js does not define this helper.`

## Practical checks

Run focused retests for every edited or removed `testV8.json` entry.
When adding an `UNSUPPORTED_FLAGS` entry, retest at least one representative file for the new flag and confirm that it is filtered rather than executed.

After large feature work or a V8 test update, re-sweep the affected historical failure area.
Old `FAIL` entries often become stale when support lands or when a missing harness shim is added.
