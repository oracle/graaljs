# GraalJS Changelog

This changelog summarizes major changes between GraalVM versions of the GraalVM JavaScript (ECMAScript) language runtime.
The main focus is on user-observable behavior of the engine.
Changelog may include unreleased versions.
See [release calendar](https://www.graalvm.org/release-calendar/) for release dates.

## Version 24.2.0
* Updated Node.js to version 20.15.1.
* Implemented the [`Promise.try`](https://github.com/tc39/proposal-promise-try) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Source Phase Imports](https://github.com/tc39/proposal-source-phase-imports) proposal. It is available behind the experimental option (`--js.source-phase-imports`).
* Implemented basic Worker API (resembling the API available in `d8`). It is available behind the experimental option `--js.worker`.
* Added option `js.stack-trace-api` that enables/disables `Error.captureStackTrace`, `Error.prepareStackTrace` and `Error.stackTraceLimit`. These non-standard extensions are disabled by default (unless `js.v8-compat` or `js.nashorn-compat` is used).
* Made option `js.webassembly` stable.
* Added an experimental `java.util.concurrent.Executor` that can be used to post tasks into the event loop thread in `graal-nodejs`. It is available as `require('node:graal').eventLoopExecutor`.

## Version 24.1.0
* ECMAScript 2024 mode/features enabled by default.
* Implemented the [Make eval-introduced global vars redeclarable](https://github.com/tc39/proposal-redeclarable-global-eval-vars) proposal.
* Implemented the [Float16Array](https://github.com/tc39/proposal-float16array) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Array.fromAsync](https://github.com/tc39/proposal-array-from-async) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Resizable and Growable ArrayBuffers](https://github.com/tc39/proposal-resizablearraybuffer) proposal.
* Updated Node.js to version 20.13.1.
* Made option `js.esm-eval-returns-exports` stable and allowed in `SandboxPolicy.UNTRUSTED`.

## Version 24.0.0
* Implemented the [WebAssembly threads](https://github.com/WebAssembly/threads) proposal.
* Implemented the [Promise.withResolvers](https://github.com/tc39/proposal-promise-with-resolvers) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implementation of [Async Iterator Helpers](https://github.com/tc39/proposal-async-iterator-helpers) proposal (that was split out from Iterator Helpers proposal) was moved behind the experimental option `--js.async-iterator-helpers`.
* Implemented the [Well-Formed Unicode Strings](https://github.com/tc39/proposal-is-usv-string) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [JSON.parse source text access](https://github.com/tc39/proposal-json-parse-with-source) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Updated Node.js to version 18.18.2.
* WebAssembly support in Node.js has been enabled by default. It can be disabled using the experimental option `--js.webassembly=false`.
* `--js.import-assertions` option has been replaced by `--js.import-attributes` option because [the corresponding proposal](https://github.com/tc39/proposal-import-attributes) has migrated from the usage of assertions to the usage of attributes.

## Version 23.1.0
* NOTE: GraalVM no longer ships with a "js" ScriptEngine. Please either use the Maven dependency or explicitly put `js-scriptengine.jar` on the module path. See [ScriptEngine documentation](docs/user/ScriptEngine.md) for details.
* ECMAScript 2023 mode/features enabled by default.
* Updated Node.js to version 18.17.1.
* Implemented the [Async Context](https://github.com/tc39/proposal-async-context) proposal. It is available behind the experimental option `--js.async-context`.
* `FinalizationRegistry.prototype.cleanupSome` is not enabled by default any more; it has been moved to ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Added an experimental option `--js.allow-narrow-spaces-in-date-format` (enabled by default). When this option is set to `false` then narrow spaces in date/time formats are replaced by a space (`0x20`).
* Made option `js.console` stable and allowed in `SandboxPolicy.UNTRUSTED`.
* Made option `js.unhandled-rejections` stable and allowed in `SandboxPolicy.CONSTRAINED`.
* Added option `js.allow-eval` that is stable and allowed in `SandboxPolicy.UNTRUSTED`.
* Deprecated option `js.disable-eval`, superseded by `js.allow-eval`.
* Implemented the [String.dedent](https://github.com/tc39/proposal-string-dedent) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Duplicate named capture groups are now supported in regular expressions, as per the [duplicate named capturing groups](https://github.com/tc39/proposal-duplicate-named-capturing-groups) proposal.
* Implemented the [RegExp v flag](https://github.com/tc39/proposal-regexp-v-flag) proposal. It is available behind the experimental option `--js.regexp-unicode-sets`.
* The Maven coordinates for embedding the GraalVM JavaScript have been updated.
  For consuming the enterprise GraalVM JavaScript, use:
  ```xml
  <dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js</artifactId>
    <version>${graalvm.version}</version>
    <type>pom</type>
  </dependency>
  ```
  For consuming the community GraalVM JavaScript, use:
  ```xml
  <dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js-community</artifactId>
    <version>${graalvm.version}</version>
    <type>pom</type>
  </dependency>
  ```

## Version 23.0.0
* Implemented the [WebAssembly reference types](https://github.com/WebAssembly/reference-types) proposal.
* Implemented the [Iterator Helpers](https://github.com/tc39/proposal-iterator-helpers) proposal. It is available behind the experimental option `--js.iterator-helpers`.
* Implemented the [ShadowRealm](https://github.com/tc39/proposal-shadowrealm) proposal. It is available behind the experimental option `--js.shadow-realm`.
* Removed experimental option `v8-legacy-const`.
* Removed non-standard `SharedArrayBuffer.isView`.
* Updated Node.js to version 18.14.1.
* Implemented the [Symbols as WeakMap keys](https://github.com/tc39/proposal-symbols-as-weakmap-keys) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [ArrayBuffer.prototype.transfer and friends](https://github.com/tc39/proposal-arraybuffer-transfer) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Change Array by copy](https://github.com/tc39/proposal-change-array-by-copy) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Added BigInteger interop support.
  Note that foreign BigIntegers require an explicit type cast using the `BigInt` function to opt into JS BigInt semantics.
  The default semantics is to treat all foreign numbers like JS Number values, regardless of the original value or type.
  Arithmetic operators perform an implicit lossy conversion to double; mixing a JS BigInt with any non-JS number always throws.
  Comparison operators attempt to do a precise value comparison where possible.
  JS BigInt values can now be converted to `java.math.BigInteger` host objects, although a [target type mapping](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/HostAccess.Builder.html#targetTypeMapping-java.lang.Class-java.lang.Class-java.util.function.Predicate-java.util.function.Function-) may still be necessary to ensure consistent type mapping if the target type is ambiguous or absent.

## Version 22.3.0
* Implemented the [WebAssembly multi-value](https://github.com/WebAssembly/multi-value) proposal.
* Added an experimental option `--js.unhandled-rejections=handler` that allows to use a custom callback to track unhandled promise rejections.
* Updated Node.js to version 16.17.1.
* ECMA-402 Internationalization API has been enabled by default. It can be disabled by `--js.intl-402=false` option.
* Implemented the [Decorators (stage 3)](https://github.com/tc39/proposal-decorators) proposal.

## Version 22.2.0
* GraalVM JavaScript is now an installable component of GraalVM. It can be installed with `gu install js`.
* Enabled option `js.foreign-object-prototype` by default. Polyglot Interop objects now get a fitting JavaScript prototype assigned unless explicitly turned off using this flag.
* Added intermediate prototype for foreign objects to simplify adapting functionality.
* Removed deprecated experimental option `experimental-foreign-object-prototype`.
* Removed experimental option `commonjs-global-properties`. The same functionality can be achieved in user code with a direct call to `require()` after context creation.
* Added an experimental option `--js.zone-rules-based-time-zones` that allows to use timezone-related data from `ZoneRulesProvider` (instead of ICU4J data files).
* Temporal objects can be converted to compatible Java objects when possible, using the `Value` API's methods like `asDate()`.

## Version 22.1.0
* Updated Node.js to version 16.14.2.
* Graal.js now requires Java 11+ and no longer supports Java 8.
* Implemented the [Intl.NumberFormat v3](https://github.com/tc39/proposal-intl-numberformat-v3) proposal.
* Implemented the [Array Grouping v3](https://github.com/tc39/proposal-array-grouping) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Temporal](https://github.com/tc39/proposal-temporal) proposal. It is available behind the experimental option `--js.temporal`.
* Implemented the [Array Find from Last](https://github.com/tc39/proposal-array-find-from-last) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Optimized closures to only keep references on variables in the lexical environment that are needed by (one or more) closures. This optimization can be disabled with the experimental option `--js.scope-optimization=false`.
* Moved the internal string representation to the new TruffleString type.
* Added option `--js.string-lazy-substrings` (default: true) to allow toggling the copying behavior of string slices. When enabled, string slices internally create string views instead of copying the given string region, which increases performance but may also increase memory utilization.

## Version 22.0.0
* ECMAScript 2022 mode/features enabled by default.
* Implemented the [Intl.DisplayNames v2](https://github.com/tc39/intl-displaynames-v2) proposal.
* Implemented the [Intl Locale Info](https://github.com/tc39/proposal-intl-locale-info) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Intl.DateTimeFormat.prototype.formatRange](https://github.com/tc39/proposal-intl-DateTimeFormat-formatRange) proposal.
* Implemented the [Extend TimeZoneName Option](https://github.com/tc39/proposal-intl-extend-timezonename) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Implemented the [Intl Enumeration API](https://github.com/tc39/proposal-intl-enumeration) proposal. It is available in ECMAScript staging mode (`--js.ecmascript-version=staging`).
* Updated Node.js to version 14.18.1.
* Added option `js.esm-bare-specifier-relative-lookup` (default: false) to customize how bare specifiers for ES Modules are resolved. When disabled, bare specifiers are resolved with an absolute path lookup. When enabled, bare specifiers are resolved relative to the importing module's path.
* ICU4J library moved to `truffle`.

## Version 21.3.0
* Implemented the [Private Fields in `in`](https://tc39.es/proposal-private-fields-in-in) proposal. It is available behind the experimental option `--js.private-fields-in`. 
* Implemented the [JavaScript BigInt to WebAssembly i64 integration](https://github.com/WebAssembly/JS-BigInt-integration) proposal. It can be disabled using the `--js.wasm-bigint=false` option.
* Updated Node.js to version 14.17.6.
* Implemented the [Error Cause](https://github.com/tc39/proposal-error-cause) proposal. It is available behind the experimental option `--js.error-cause`.
* Implemented the [Import Assertions](https://tc39.es/proposal-import-assertions) proposal. It is available behind the experimental option `--js.import-assertions`.
* Added support for `collation` option of `Intl.Collator`.
* Added support for `dayPeriod` and `fractionalSecondDigits` options of `Intl.DateTimeFormat`.
* Changed foreign hash map access using `map[key]` syntax to convert the key to a string or symbol.
* Implemented `Object.hasOwn` ([Accessible Object.hasOwnProperty](https://github.com/tc39/proposal-accessible-object-hasownproperty) proposal). It is available in ECMAScript 2022 (`--js.ecmascript-version=2022`).
* Implemented [class static initialization blocks](https://github.com/tc39/proposal-class-static-block) proposal. It is available in ECMAScript 2022 (`--js.ecmascript-version=2022`).
* Added experimental Polyglot `Context` option `--js.esm-eval-returns-exports`. When enabled, `eval()` of an ES module will return a Polyglot `Value` containing the ES module exported namespace object. The option is disabled by default.

## Version 21.2.0
* Graal.js now prints a warning when runtime compilation is not supported. This warning can be disabled using the '--engine.WarnInterpreterOnly=false' option or the '-Dpolyglot.engine.WarnInterpreterOnly=false' system property.
* Added the `js.unhandled-rejections` option to track unhandled promise rejections in a polyglot `Context`. By default, the option is set to `none`, and unhandled promise rejections are not tracked.
* Implemented the [New Set Methods](https://github.com/tc39/proposal-set-methods) proposal. It is available behind an experimental flag (`--js.new-set-methods`).
* Implemented experimental operator overloading support. Use the experimental option `--js.operator-overloading` to enable it and consult [the documentation](docs/user/OperatorOverloading.md).
* Updated RegExp Match Indices proposal with opt-in using the `d` flag. Available in ECMAScript 2022 (`--js.ecmascript-version=2022`). Deprecated `--js.regexp-match-indices` option.
* Nashorn compatibility mode now defaults to compatiblity with ECMAScript version 5, unless another version is explicitly selected.
* `Date.prototype` built-ins use ICU (not JDK) algorithms and data (like timezone data) by now (in order to reduce inconsistencies between `Date` and `Intl.DateTimeFormat`).
* Updated ICU4J library to version 69.1.

## Version 21.1.0
* Updated Node.js to version 14.16.1.
* Prototype of WebAssembly JavaScript Interface implemented. It is available behind the `--js.webassembly` flag.
* Implemented iterator interop support, enabling foreign objects that have an iterator to be used where JS expects iterables, as well as JS iterables to be used in other languages and via the [Value](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Value.html#getIterator--) API.
* Adopted new buffer interop support, allowing foreign buffers to be used with typed arrays and `DataView`, without copying. Likewise enables `ArrayBuffer` to be used in other languages.
* Experimental option `js.array-sort-inherited` was removed. Values visible through holes in array(-like) object are always sorted according to the latest version of ECMAScript specification.
* Updated ICU4J library to version 68.2.
* Implemented the [Atomics.waitAsync](https://github.com/tc39/proposal-atomics-wait-async) proposal. It is available in ECMAScript 2022 mode (`--js.ecmascript-version=2022`).
* Implemented hash map interop support. Allows foreign hash maps to be iterated using `for in/of` loops, `new Map(hash)`, `Array.from(hash)`, etc. If the `--js.foreign-hash-properties` option is enabled (default), foreign hash maps can also be accessed using `hash[key]`, `hash.key`, and used in `{...hash}`. If the `--js.foreign-object-prototype` option is enabled, foreign hash maps also have `Map.prototype` methods.
* Moved GraalVM-Node.js to a separate installable component. In a GraalVM distribution it needs to be manually installed with `$GRAALVM/bin/gu install nodejs`.

## Version 21.0.0
* ECMAScript 2021 mode/features enabled by default.
* Updated Node.js to version 12.20.1.
* Adopted new interop exception handling and made JS exceptions extend `AbstractTruffleException`.
* Implemented interop identity messages.
* Expose `Graal.versionECMAScript` instead of `Graal.versionJS`.
* Implemented the [relative indexing method](https://tc39.es/proposal-relative-indexing-method/) proposal. It is available in ECMAScript 2022 mode (`--js.ecmascript-version=2022`).

## Version 20.3.0
* Updated Node.js to version 12.18.4.
* Fixed field/getter/setter access order in nashorn-compat mode, see [issue #343](https://github.com/graalvm/graaljs/issues/343).
* ScriptEngine: Fixed "Multiple applicable overloads found" error in nashorn-compat mode, see [issue #286](https://github.com/graalvm/graaljs/issues/286).
* ScriptEngine: Enabled low precedence lossy number, string-to-boolean, and number-to-boolean conversions in nashorn-compat mode.
* Fixed `Java.extend` to respect `HostAccess.Builder.allowImplementations`, see [issue #294](https://github.com/graalvm/graaljs/issues/294).
* Added Java host interop support for mapping JS objects to abstract classes (if `HostAccess` allows it).
* `js.foreign-object-prototype` is a supported option to set JavaScript prototypes for foreign objects mimicing JavaScript types. It was renamed from `js.experimental-foreign-object-prototype`.
* Changed `ToPrimitive` abstract operation to follow the specification for foreign objects. `InteropLibrary.toDisplayString` is not used by `ToPrimitive/ToString` conversions anymore.

## Version 20.2.0
* Implemented the [Intl.NumberFormat Unified API](https://github.com/tc39/proposal-unified-intl-numberformat) proposal.
* Implemented the [Logical Assignment Operators](https://github.com/tc39/proposal-logical-assignment) proposal.
* Implemented the [Top-level Await](https://github.com/tc39/proposal-top-level-await) proposal.
* Implemented the [Promise.any](https://github.com/tc39/proposal-promise-any) proposal. It is available in ECMAScript 2021 mode (`--js.ecmascript-version=2021`).
* Implemented support for async stack traces.
* Removed deprecation warning for flags previously passed via system properties.
* Updated Node.js to version 12.18.0.
* Updated ICU4J library to version 67.1
* Fixed `Date.toLocaleString` and `Intl.DateTimeFormat` to use the context's default time zone rather than the system default if no explicit time zone is requested.
* Improved `js.timezone` option to validate the time zone ID and support zone offsets like "-07:00".
* Changed the `===` operator to treat foreign null values the same as JS `null` and use `InteropLibrary.isIdentical` for foreign objects.
* Unified JSON parsing, providing more compatible error messages.

## Version 20.1.0
* ECMAScript 2020 mode/features enabled by default.
* Implemented the [Intl.DateTimeFormat dateStyle & timeStyle](https://github.com/tc39/proposal-intl-datetime-style) proposal.
* Implemented the [Intl.DisplayNames](https://github.com/tc39/proposal-intl-displaynames) proposal.
* Implemented the [Intl.Locale](https://github.com/tc39/proposal-intl-locale) proposal.
* Implemented support for `export * as ns from "mod"`. It is available in ECMAScript 2020 or later.
* Implemented support for optional chaining (`a?.b`, `a?.[b]`, `a?.()`). It is available in ECMAScript 2020 or later.
* Implemented Nashorn extension `Object.bindProperties()`. It is available in Nashorn compatibility mode (`--js.nashorn-compat`).
* Changed `Polyglot.eval[File]` to propagate syntax errors from the source language.
* Changed `Polyglot.import` to return `undefined` for missing polyglot bindings.
* Added `npx` (an `npm` package runner) into GraalVM.
* Implemented the [RegExp Match Indices](https://github.com/tc39/proposal-regexp-match-indices) proposal. It is available behind the `--js.regexp-match-indices` flag.
* Removed the JOni RegExp-engine and the "use-tregex" option.
* Implemented support for [private class methods](https://github.com/tc39/ecma262/pull/1668/); available behind the `--js.ecmascript-version=2021` flag.
* Implemented the [FinalizationRegistry](https://github.com/tc39/proposal-weakrefs) proposal. It is available behind the `--js.ecmascript-version=2021` flag.
* Enabled the [Hashbang Grammar](https://github.com/tc39/proposal-hashbang) proposal, by enabling the `--js.shebang` option by default in ECMAScript 2020 or later.
* Updated ICU4J library to version 66.1.
* Moved `String.prototype.replaceAll` to ECMAScript version 2021 (`--js.ecmascript-version=2021`).
* Removed [SIMD.js](https://github.com/tc39/ecmascript_simd) implementation.

## Version 20.0.0
* Implemented support for public and private class fields, both instance and static (based on the [class fields](https://github.com/tc39/proposal-class-fields) and [static class features](https://github.com/tc39/proposal-static-class-features) proposals). This feature is available by default in Node.js and can be enabled using the experimental option `js.class-fields`.
* Added option `js.global-arguments` (default: true) for the non-standard `arguments` global property (disabled in Node.js).
* Updated Node.js to version 12.15.0.
* Added `name` property to anonymous functions.
* `String.prototype.matchAll` is available in ECMAScript 2020 mode (`--js.ecmascript-version=2020`) only.
* Implemented the [String.prototype.replaceAll](https://github.com/tc39/proposal-string-replaceall) proposal. It is available in ECMAScript 2020 mode (`--js.ecmascript-version=2020`).
* Added `js.load-from-classpath` option to allow loading files from the classpath via `classpath:` pseudo URLs (disabled by default). Do not use with untrusted code.
* Added option `js.bind-member-functions` for the implicit binding of unbound functions returned by [Value.getMember](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Value.html#getMember-java.lang.String-) to the receiver. The preferred way of calling members is using [Value.invokeMember](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Value.html#invokeMember-java.lang.String-java.lang.Object...-).
* Added option `js.commonjs-require` to load Npm-compatible CommonJS modules from plain JavaScript. This is an experimental feature. See [NodeJSVSJavaScriptContext.md](https://github.com/graalvm/graaljs/blob/master/docs/user/NodeJSVSJavaScriptContext.md) for details.

## Version 19.3.0
* Implemented the [Promise.allSettled](https://github.com/tc39/proposal-promise-allSettled) proposal. It is available in ECMAScript 2020 mode (`--js.ecmascript-version=2020`).
* Implemented the [nullish coalescing](https://github.com/tc39/proposal-nullish-coalescing) proposal. It is available in ECMAScript 2020 mode (`--js.ecmascript-version=2020`).
* Updated ICU4J library to version 64.2.
* Updated ASM library to version 7.1.
* Updated Node.js to version 12.10.0.
* Disabled the non-standard `global` global property by default, which has been superseded by `globalThis`; use option `js.global-property` to reenable.
* Disabled the non-standard `performance` global property by default except for the `js` launcher; use option `js.performance` to reenable.
* Disabled the non-standard `print` and `printErr` functions in Node.js; use option `js.print` to reenable.

## Version 19.2.0
* Added support for date and time interop.
* Added support for setting the time zone via `Context.Builder.timeZone`.
* Implemented [Numeric separators](https://github.com/tc39/proposal-numeric-separator) proposal. It is available in ECMAScript 2020 mode (`--js.ecmascript-version=2020`).
* Moved ICU data previously distributed as separate files into the native image.
* Added `import.meta.url` property.

## Version 19.1.0
* Added (experimental) option `js.locale` to set the default locale for locale-sensitive operations.
* Allow making evaluated sources internal using the `sourceURL` directive by prefixing the URL with `internal:`, e.g. `//# sourceURL=internal:myname.js`.
* Object.getOwnPropertyDescriptor(s) supported for non-JavaScript objects as well.
* Enabled code sharing across threads using `ContextPolicy.SHARED`.

## Version 19.0.0
* Various bugfixes for compilation problems.

## Version 1.0.0 RC16
* provide `js.load-from-url` to allow loading from a URL.
* provide `js.bigint` option to enable the BigInt proposal, available only in ECMAScript 2019 or later (enabled by default for those version, set to `false` to disable).

## Version 1.0.0 RC15
* Removed non-standard `String.prototype.contains`; use `includes` instead.
* ScriptEngine: security-relevant Context-options are now disabled by default.
* Migrated to Truffle Libraries for interop.
* `Polyglot` builtin enabled based on `Context.Builder.allowPolyglotAccess()`.
* Added object rest and spread properties support for foreign objects.

## Version 1.0.0 RC14
* Option `js.function-arguments-limit` to set an upper bound for function arguments and argument spreading (default: 65535).
* Support for [HTML-like comments](https://tc39.github.io/ecma262/#sec-html-like-comments) added.
* Option `js.experimental-array-prototype` has been renamed to `js.experimental-foreign-object-prototype`.
  In addition to setting the prototype of array-like non-JS objects to `Array.prototype`
  it sets the prototype of executable non-JS objects to `Function.prototype` and
  the prototype of all other non-JS objects to `Object.prototype`.
* `--jvm.*` and `--native.*` command line options are deprecated and replaced by `--vm.*` options
* Implemented [JSON superset](https://github.com/tc39/proposal-json-superset) proposal.

## Version 1.0.0 RC13
* Made Java interop available in native images. Note that you have to configure the accessible classes and methods at native image build time (see [reflection configuration](https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md#manual-configuration)).
* Removed deprecated experimental `Java.Worker` API. Node.js Workers should be used instead.
* Removed deprecated `NashornJavaInterop` mode.
* [Object.fromEntries](https://tc39.github.io/proposal-object-from-entries/) proposal implemented.
* Implemented [import()](https://tc39.github.io/proposal-dynamic-import/) proposal.
* Updated Node.js to version 10.15.2.

## Version 1.0.0 RC12
* Added option `js.experimental-array-prototype` that sets prototype of
  array-like non-JS objects (like `ProxyArray` or Java `List`) to `Array.prototype`.
  It is possible to use functions like `map` or `forEach` on these objects directly then.
* Updated Node.js to version 10.15.0.
* Added mime type `application/javascript+module` for ES module sources.
* Changed the option name for the non-standard `global` property to `js.global-property`.

## Version 1.0.0 RC11
* Graal.js only supports ECMAScript 5 (ES5) and newer, and enforces that rule.
* Added option `js.disable-eval` to enable eval() and similar methods of dynamic code evaluation (enabled by default, set to `false` to disable).
* Added option `js.disable-with` to enable the with statement (enabled by default, set to `false` to disable).
* Instrumentation: Extended inline parsing to support statements.
* Added support for sharing Java objects using the experimental Node.js Worker Threads API.
* Added support for ScriptEngine `GLOBAL_SCOPE` bindings.
* Made `Bindings` created by `ScriptEngine#createBindings()` implement `AutoCloseable` to allow closing the underlying `Context`.
* Do not provide `Java` builtin object when Java interop is disabled.
* Added option `js.print` to enable the `print` and `printErr` builtins (enabled by default, set to `false` to disable).
* Added option `js.load` to enable the `load` and `loadWithNewGlobal` builtins (enabled by default, set to `false` to disable).
* Added option `js.polyglot-builtin` to enable the `Polyglot` builtins (enabled by default, set to `false` to disable).

## Version 1.0.0 RC10
* Added support for `Array.prototype.{flat,flatMap}`, [a Stage 3 proposal](https://github.com/tc39/proposal-flatMap).
* `Atomics.wake` available under its new name (`Atomics.notify`) as well.
* [Well-formed JSON.stringify](https://github.com/tc39/proposal-well-formed-stringify) proposal implemented.
* [globalThis](https://github.com/tc39/proposal-global) proposal implemented.
* Added `Java.addToClasspath(path)` for adding jar files and directories to the host classpath dynamically.
* Disabled non-standard global functions `quit`, `read`, `readbuffer`, and `readline` by default, except for the `js` launcher (`js.shell` option).
* Disabled non-standard global functions `readFully` and `readLine` by default, now available only in Nashorn scripting mode (`--js.scripting`).
* Disabled Nashorn syntax extensions by default (`js.syntax-extensions` option), barring `ScriptEngine`.
* Note: As a result, `eval` requires function expressions to be parenthesized, e.g.: `eval("(function(){...})")` (not `eval("function(){...}")`).

## Version 1.0.0 RC8
* Provide simplified implementations for methods of the global `console` object even outside Node.js mode.
* Updated Node.js to version 10.9.0.
* Fix: Can construct `Proxy(JavaType)` and correctly reports as type `function`. Github #60.

## Version 1.0.0 RC7
* Improved support for sharing of shapes between Contexts with the same Engine.
* Provide support for BigInteger TypedArrays, cf. [ECMAScript BigInt proposal](https://tc39.github.io/proposal-bigint/#sec-typedarrays-and-dataview).
* Extended instrumentation support to more types of interpreter nodes.

## Version 1.0.0 RC6
* [Serialization API](https://nodejs.org/api/v8.html#v8_serialization_api) of v8/Node.js implemented.
* Update version of Unicode to 11 in `RegExp` and `Intl`.
* Implement Truffle file virtualization for JavaScript.
* Support polyglot Truffle objects in `Array.prototype.map` et al and `Array.prototype.sort`.
* Support for fuzzy time in `performance.now()` and `Date`.

## Version 1.0.0 RC5
* Add support for `Symbol.prototype.description`, a Stage 3 proposal.
* Add support for `String.prototype.matchAll`, a Stage 3 proposal.
* Implement optional catch binding proposal, targeted for ES2019.
* Removed legacy `NashornExtensions` option, use `--js.nashorn-compat` instead.
* Provide Java package globals by default.

## Version 1.0.0 RC4
* Added stack trace limit option (--js.stack-trace-limit).
* Enable SharedArrayBuffers by default.
* Provide $EXEC for Nashorn compatibility in scripting mode.
* Provide Java.isScriptFunction, Java.isScriptObject, Java.isJavaMethod, and Java.isJavaFunction in Nashorn compatibility mode.
* Provide support to access getters and setters like a field, in Nashorn compatibility mode.
* Provide top-level package globals in Nashorn compatibility mode: `java`, `javafx`, `javax`, `com`, `org`, `edu`.
* Provide Java.extend, Java.super, and `new Interface|AbstractClass(fn|obj)` in Nashorn compatibility mode.
* Provide `java.lang.String` methods on string values, in Nashorn compatibility mode.
* Provide `JavaImporter` class in Nashorn compatibility mode.
* Provide `JSAdapter` class only in Nashorn compatibility mode.

## Version 1.0.0 RC3
* Added support for BigInt arithmetic expressions.
* Provide a flag for a Nashorn compatibility mode (--js.nashorn-compat).
* Rename flag for V8 compatibility mode (to --js.v8-compat).

## Version 1.0.0 RC2
* Enabled code sharing between Contexts with the same Engine.
* Updated Node.js to version 8.11.1.

## Version 1.0.0 RC1
* LICENSE set to The Universal Permissive License (UPL), Version 1.0.

## Version 0.33

* Added object rest/spread support.
* Added support for async generators.
* Unified Polyglot primitives across all Truffle languages; e.g., rename `Interop` builtin to `Polyglot`.

