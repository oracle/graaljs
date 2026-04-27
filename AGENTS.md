# AGENTS.md

## Overview
The `graaljs` repository contains two `mx` suites:

- `graal-js`: the GraalJS engine, implemented in Java on top of Truffle.
- `graal-nodejs`: a Node.js fork wired to GraalJS through Java and native glue.

Always run `mx` from a suite directory, not from the repository root. Use `graal-js/` for JavaScript engine work and `graal-nodejs/` for Node.js work.

## Structure
```text
./
├── graal-js/                               # Core GraalJS suite
│   ├── mx.graal-js/                        # suite.py + custom mx commands
│   ├── src/com.oracle.truffle.js/          # Runtime, AST nodes, builtins, language entrypoints
│   ├── src/com.oracle.js.parser/           # JavaScript parser
│   ├── src/com.oracle.truffle.js.test/     # JUnit + JS regression tests
│   ├── test/                               # Test262 / V8 / Nashorn expectations and smoke tests
│   └── benchmarks/                         # Compiler and interpreter benchmarks
├── graal-nodejs/                           # Graal Node.js suite
│   ├── mx.graal-nodejs/                    # suite.py + custom mx commands
│   ├── mx.graal-nodejs/com.oracle.truffle.trufflenode/      # Java bridge to GraalJS
│   ├── mx.graal-nodejs/com.oracle.truffle.trufflenode.test/ # Java tests for the bridge
│   ├── src/                                # Upstream-ish Node.js native sources
│   ├── lib/                                # Node.js JS standard library
│   ├── deps/                               # Vendored dependencies
│   ├── test/                               # Upstream Node.js tests
│   └── test/graal/                         # Graal-specific Node.js tests
└── docs/                                   # Contributor + user docs
```

## Where to look
| Task | Location | Notes |
|------|----------|-------|
| Build and setup | `docs/Building.md`, `docs/contributor/Testing.md` | Start here for repo-level build and engine test instructions. |
| GraalJS suite metadata and commands | `graal-js/mx.graal-js/suite.py`, `graal-js/mx.graal-js/mx_graal_js.py` | Read these first for project names, distributions, gate tasks, and custom commands. |
| GraalJS runtime and semantics | `graal-js/src/com.oracle.truffle.js/src/com/oracle/truffle/js/{runtime,nodes,builtins,lang}` | Main engine implementation. |
| GraalJS parser | `graal-js/src/com.oracle.js.parser/`, `graal-js/src/com.oracle.truffle.js.parser/` | Parser and parser integration layers. |
| GraalJS tests | `graal-js/src/com.oracle.truffle.js.test/`, `graal-js/test/` | JUnit, JS regression tests, and Test262/V8/Nashorn expectations. |
| Graal Node.js suite metadata and commands | `graal-nodejs/mx.graal-nodejs/suite.py`, `graal-nodejs/mx.graal-nodejs/mx_graal_nodejs.py` | Read these before touching build logic, launchers, or CI-facing tests. |
| Graal Node.js Java bridge | `graal-nodejs/mx.graal-nodejs/com.oracle.truffle.trufflenode/src/com/oracle/truffle/trufflenode/` | Java bridge and runtime integration. |
| Graal Node.js native/runtime code | `graal-nodejs/src/`, `graal-nodejs/lib/` | Mostly upstream Node.js layout and conventions. |
| Node.js tests | `graal-nodejs/test/README.md`, `graal-nodejs/test/`, `graal-nodejs/BUILDING.md` | Upstream Node.js test harness and subsystem layout. |
| Graal-specific Node.js tests | `graal-nodejs/test/graal/` | Mocha tests, instrumentation tests, and addon-facing regressions. |
| User-facing docs | `docs/user/` | Update the nearest page for behavioral changes. |

## Conventions
- `mx` is the primary build, run, and test entrypoint for both suites.
- `graal-js` imports dependencies from the sibling `graal/` checkout.
- Always ask the user to run `mx sforceimports` to sync revisions, never run it yourself.
- `graal-nodejs` imports `graal-js`; building `graal-nodejs` will also build the JavaScript engine pieces it needs.
- `mx build` can clone missing imports, but it does not update existing ones.
- The root `pyproject.toml` and `graal-nodejs/pyproject.toml` disable Black and `mx pyformat`. Do not assume Python auto-formatting is configured for this repo.
- Prefer `mx js`, `mx node`, `mx npm`, `mx npx`, and `mx testnode` over raw launchers when running from source trees; the wrappers set required classpath, module-path, and JVM environment.
- `graal-nodejs/out/` and any suite-local `mxbuild/` directories are generated outputs and should not be edited manually.

## Anti-patterns
- Do not edit generated files under `mxbuild/` or `graal-nodejs/out/`.
- Do not treat `graal-nodejs/src/`, `graal-nodejs/lib/` or `graal-nodejs/deps/` (with the exception of `graal-nodejs/deps/v8/src/graal`) as greenfield code. These files are upstream imports; keep diffs minimal and preserve upstream style.
- Do not use plain `tools/test.py` for Graal Node.js unless you intentionally want to bypass the `mx` environment wrapper. Prefer `mx testnode`.
- Do not update `graal-js/test/test262.json`, `graal-js/test/testV8.json` or `graal-js/test/testNashorn.json` unless you are intentionally changing expected failures or enabling new coverage.

## Commands

### Build and run GraalJS
```bash
cd graal-js
mx build
mx js -e 'console.log(42)'

# If the compiler suite is available and you want optimized execution:
mx --dynamicimports /compiler js -e 'console.log(42)'

# To build with GraalWasm
mx --dy /wasm build
```

### Build and run Graal Node.js
```bash
cd graal-nodejs
mx build
mx node -e "console.log(42)"
mx npm --version
mx npx --version

# Debug build / launcher
mx build --debug
mx node --debug -e "console.log(process.version)"
```

## Notes
- Start non-trivial work by reading the relevant `suite.py` and `mx_*.py` files. They are the quickest source of truth for project names, distributions, gate tasks, and launcher behavior.
- When fixing a GraalJS runtime bug, add the closest regression test under `graal-js/src/com.oracle.truffle.js.test/js/` or a JUnit test under `graal-js/src/com.oracle.truffle.js.test/src/`. Many focused JS regressions use `GR-*.js` filenames; follow nearby conventions.
- When fixing Graal-specific Node.js behavior, prefer `graal-nodejs/test/graal/unit/` for focused regressions and `graal-nodejs/test/graal/instrument/` for instrumentation-specific behavior.
- When fixing Node.js compatibility, add or update tests under the relevant upstream-style subtree in `graal-nodejs/test/`.
- Never run the full gate locally (e.g., via `mx gate`), create a PR and run all tests on the CI instead.
- Native addon builds often need the source tree as `nodedir`. If `npm` or `node-gyp` must compile against this checkout, pass `--nodedir=<path-to-graal-nodejs>`.
- Update `docs/` for user-visible behavior changes.
