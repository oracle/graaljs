# Node.js 20 ChangeLog

<!--lint disable maximum-line-length no-literal-urls prohibited-strings-->

<table>
<tr>
<th>LTS 'Iron'</th>
<th>Current</th>
</tr>
<tr>
<td>
<a href="#20.12.2">20.12.2</a><br/>
<a href="#20.12.1">20.12.1</a><br/>
<a href="#20.12.0">20.12.0</a><br/>
<a href="#20.11.1">20.11.1</a><br/>
<a href="#20.11.0">20.11.0</a><br/>
<a href="#20.10.0">20.10.0</a><br/>
<a href="#20.9.0">20.9.0</a><br/>
</td>
<td>
<a href="#20.8.1">20.8.1</a><br/>
<a href="#20.8.0">20.8.0</a><br/>
<a href="#20.7.0">20.7.0</a><br/>
<a href="#20.6.1">20.6.1</a><br/>
<a href="#20.6.0">20.6.0</a><br/>
<a href="#20.5.1">20.5.1</a><br/>
<a href="#20.5.0">20.5.0</a><br/>
<a href="#20.4.0">20.4.0</a><br/>
<a href="#20.3.1">20.3.1</a><br/>
<a href="#20.3.0">20.3.0</a><br/>
<a href="#20.2.0">20.2.0</a><br/>
<a href="#20.1.0">20.1.0</a><br/>
<a href="#20.0.0">20.0.0</a><br/>
</td>
</tr>
</table>

* Other Versions
  * [22.x](CHANGELOG_V22.md)
  * [21.x](CHANGELOG_V21.md)
  * [19.x](CHANGELOG_V19.md)
  * [18.x](CHANGELOG_V18.md)
  * [17.x](CHANGELOG_V17.md)
  * [16.x](CHANGELOG_V16.md)
  * [15.x](CHANGELOG_V15.md)
  * [14.x](CHANGELOG_V14.md)
  * [13.x](CHANGELOG_V13.md)
  * [12.x](CHANGELOG_V12.md)
  * [11.x](CHANGELOG_V11.md)
  * [10.x](CHANGELOG_V10.md)
  * [9.x](CHANGELOG_V9.md)
  * [8.x](CHANGELOG_V8.md)
  * [7.x](CHANGELOG_V7.md)
  * [6.x](CHANGELOG_V6.md)
  * [5.x](CHANGELOG_V5.md)
  * [4.x](CHANGELOG_V4.md)
  * [0.12.x](CHANGELOG_V012.md)
  * [0.10.x](CHANGELOG_V010.md)
  * [io.js](CHANGELOG_IOJS.md)
  * [Archive](CHANGELOG_ARCHIVE.md)

<a id="20.12.2"></a>

## 2024-04-10, Version 20.12.2 'Iron' (LTS), @RafaelGSS

This is a security release.

### Notable Changes

* CVE-2024-27980 - Command injection via args parameter of `child_process.spawn` without shell option enabled on Windows

### Commits

* \[[`69ffc6d50d`](https://github.com/nodejs/node/commit/69ffc6d50d)] - **src**: disallow direct .bat and .cmd file spawning (Ben Noordhuis) [nodejs-private/node-private#563](https://github.com/nodejs-private/node-private/pull/563)

<a id="20.12.1"></a>

## 2024-04-03, Version 20.12.1 'Iron' (LTS), @RafaelGSS

This is a security release

### Notable Changes

* CVE-2024-27983 - Assertion failed in node::http2::Http2Session::\~Http2Session() leads to HTTP/2 server crash- (High)
* CVE-2024-27982 - HTTP Request Smuggling via Content Length Obfuscation - (Medium)
* llhttp version 9.2.1
* undici version 5.28.4

### Commits

* \[[`bd8f10a257`](https://github.com/nodejs/node/commit/bd8f10a257)] - **deps**: update undici to v5.28.4 (Matteo Collina) [nodejs-private/node-private#576](https://github.com/nodejs-private/node-private/pull/576)
* \[[`5e34540a96`](https://github.com/nodejs/node/commit/5e34540a96)] - **http**: do not allow OBS fold in headers by default (Paolo Insogna) [nodejs-private/node-private#557](https://github.com/nodejs-private/node-private/pull/557)
* \[[`ba1ae6d188`](https://github.com/nodejs/node/commit/ba1ae6d188)] - **src**: ensure to close stream when destroying session (Anna Henningsen) [nodejs-private/node-private#561](https://github.com/nodejs-private/node-private/pull/561)

<a id="20.12.0"></a>

## 2024-03-26, Version 20.12.0 'Iron' (LTS), @richardlau

### Notable Changes

#### crypto: implement crypto.hash()

This patch introduces a helper crypto.hash() that computes
a digest from the input at one shot. This can be 1.2-2x faster
than the object-based createHash() for smaller inputs (<= 5MB)
that are readily available (not streamed) and incur less memory
overhead since no intermediate objects will be created.

```js
const crypto = require('node:crypto');

// Hashing a string and return the result as a hex-encoded string.
const string = 'Node.js';
// 10b3493287f831e81a438811a1ffba01f8cec4b7
console.log(crypto.hash('sha1', string));
```

Contributed by Joyee Cheung in [#51044](https://github.com/nodejs/node/pull/51044).

#### Loading and parsing environment variables

* `process.loadEnvFile(path)`:
  * Use this function to load the `.env` file. If no path is specified, it automatically loads the .env file in the current directory. Example: `process.loadEnvFile()`.
  * Load a specific .env file by specifying its path. Example: `process.loadEnvFile('./development.env')`.

* `util.parseEnv(content)`:
  * Use this function to parse an existing string containing environment variable assignments.
  * Example usage: `require('node:util').parseEnv('HELLO=world')`.

Contributed by Yagiz Nizipli in [#51476](https://github.com/nodejs/node/pull/51476).

#### New connection attempt events

Three new events were added in the `net.createConnection` flow:

* `connectionAttempt`: Emitted when a new connection attempt is established. In case of Happy Eyeballs, this might emitted multiple times.
* `connectionAttemptFailed`: Emitted when a connection attempt failed. In case of Happy Eyeballs, this might emitted multiple times.
* `connectionAttemptTimeout`: Emitted when a connection attempt timed out. In case of Happy Eyeballs, this will not be emitted for the last attempt. This is not emitted at all if Happy Eyeballs is not used.

Additionally, a previous bug has been fixed where a new connection attempt could have been started after a previous one failed and after the connection was destroyed by the user.
This led to a failed assertion.

Contributed by Paolo Insogna in [#51045](https://github.com/nodejs/node/pull/51045).

#### Permission Model changes

Node.js 20.12.0 comes with several fixes for the experimental permission model and two new semver-minor commits.
We're adding a new flag `--allow-addons` to enable addon usage when using the Permission Model.

```console
$ node --experimental-permission --allow-addons
```

Contributed by Rafael Gonzaga in [#51183](https://github.com/nodejs/node/pull/51183)

And relative paths are now supported through the `--allow-fs-*` flags.
Therefore, with this release one can use:

```console
$ node --experimental-permission --allow-fs-read=./index.js
```

To give only read access to the entrypoint of the application.

Contributed by Rafael Gonzaga and Carlos Espa in [#50758](https://github.com/nodejs/node/pull/50758).

#### sea: support embedding assets

Users can now include assets by adding a key-path dictionary
to the configuration as the `assets` field. At build time, Node.js
would read the assets from the specified paths and bundle them into
the preparation blob. In the generated executable, users can retrieve
the assets using the `sea.getAsset()` and `sea.getAssetAsBlob()` API.

```json
{
  "main": "/path/to/bundled/script.js",
  "output": "/path/to/write/the/generated/blob.blob",
  "assets": {
    "a.jpg": "/path/to/a.jpg",
    "b.txt": "/path/to/b.txt"
  }
}
```

The single-executable application can access the assets as follows:

```cjs
const { getAsset } = require('node:sea');
// Returns a copy of the data in an ArrayBuffer
const image = getAsset('a.jpg');
// Returns a string decoded from the asset as UTF8.
const text = getAsset('b.txt', 'utf8');
// Returns a Blob containing the asset without copying.
const blob = getAssetAsBlob('a.jpg');
```

Contributed by Joyee Cheung in [#50960](https://github.com/nodejs/node/pull/50960).

#### Support configurable snapshot through `--build-snapshot-config` flag

We are adding a new flag `--build-snapshot-config` to configure snapshots through a custom JSON configuration file.

```console
$ node --build-snapshot-config=/path/to/myconfig.json
```

When using this flag, additional script files provided on the command line will
not be executed and instead be interpreted as regular command line arguments.

These changes were contributed by Joyee Cheung and Anna Henningsen in [#50453](https://github.com/nodejs/node/pull/50453)

#### Text Styling

* `util.styleText(format, text)`: This function returns a formatted text considering the `format` passed.

A new API has been created to format text based on `util.inspect.colors`, enabling you to style text in different colors (such as red, blue, ...) and emphasis (italic, bold, ...).

```cjs
const { styleText } = require('node:util');
const errorMessage = styleText('red', 'Error! Error!');
console.log(errorMessage);
```

Contributed by Rafael Gonzaga in [#51850](https://github.com/nodejs/node/pull/51850).

#### vm: support using the default loader to handle dynamic import()

This patch adds support for using `vm.constants.USE_MAIN_CONTEXT_DEFAULT_LOADER` as the
`importModuleDynamically` option in all vm APIs that take this option except `vm.SourceTextModule`. This allows users to have a shortcut to support dynamic `import()` in the compiled code without missing the compilation cache if they don't need customization of the loading process. We emit an experimental warning when the `import()` is actually handled by the default loader through this option instead of requiring `--experimental-vm-modules`.

```js
const { Script, constants } = require('node:vm');
const { resolve } = require('node:path');
const { writeFileSync } = require('node:fs');

// Write test.js and test.txt to the directory where the current script
// being run is located.
writeFileSync(resolve(__dirname, 'test.mjs'),
              'export const filename = "./test.json";');
writeFileSync(resolve(__dirname, 'test.json'),
              '{"hello": "world"}');

// Compile a script that loads test.mjs and then test.json
// as if the script is placed in the same directory.
const script = new Script(
  `(async function() {
    const { filename } = await import('./test.mjs');
    return import(filename, { with: { type: 'json' } })
  })();`,
  {
    filename: resolve(__dirname, 'test-with-default.js'),
    importModuleDynamically: constants.USE_MAIN_CONTEXT_DEFAULT_LOADER,
  });

// { default: { hello: 'world' } }
script.runInThisContext().then(console.log);
```

Contributed by Joyee Cheung in [#51244](https://github.com/nodejs/node/pull/51244).

#### Root certificates updated to NSS 3.98

Certificates added:

* Telekom Security TLS ECC Root 2020
* Telekom Security TLS RSA Root 2023

Certificates removed:

* Security Communication Root CA

#### Updated dependencies

* acorn updated to 8.11.3.
* ada updated to 2.7.6.
* base64 updated to 0.5.2.
* brotli updated to 1.1.0.
* c-ares updated to 1.27.0.
* corepack updated to 0.25.2.
* ICU updated to 74.2. Includes CLDR 44.1 and Unicode 15.1.
* nghttp2 updated to 1.60.0.
* npm updated to 10.5.0. Fixes a regression in signals not being passed onto child processes.
* simdutf8 updated to 4.0.8.
* Timezone updated to 2024a.
* zlib updated to 1.3.0.1-motley-40e35a7.

#### Other notable changes

* \[[`4f49e9d000`](https://github.com/nodejs/node/commit/4f49e9d000)] - **(SEMVER-MINOR)** **build**: build opt to set local location of headers (Michael Dawson) [#51525](https://github.com/nodejs/node/pull/51525)
* \[[`ccdb01187b`](https://github.com/nodejs/node/commit/ccdb01187b)] - **doc**: add zcbenz to collaborators (Cheng Zhao) [#51812](https://github.com/nodejs/node/pull/51812)
* \[[`481af53aea`](https://github.com/nodejs/node/commit/481af53aea)] - **doc**: add lemire to collaborators (Daniel Lemire) [#51572](https://github.com/nodejs/node/pull/51572)
* \[[`5ba4d96525`](https://github.com/nodejs/node/commit/5ba4d96525)] - **(SEMVER-MINOR)** **http2**: add h2 compat support for appendHeader (Tim Perry) [#51412](https://github.com/nodejs/node/pull/51412)
* \[[`0861498e8b`](https://github.com/nodejs/node/commit/0861498e8b)] - **(SEMVER-MINOR)** **http2**: add server handshake utility (snek) [#51172](https://github.com/nodejs/node/pull/51172)
* \[[`6b08d006ee`](https://github.com/nodejs/node/commit/6b08d006ee)] - **(SEMVER-MINOR)** **http2**: receive customsettings (Marten Richter) [#51323](https://github.com/nodejs/node/pull/51323)
* \[[`7894989bf0`](https://github.com/nodejs/node/commit/7894989bf0)] - **(SEMVER-MINOR)** **lib**: move encodingsMap to internal/util (Joyee Cheung) [#51044](https://github.com/nodejs/node/pull/51044)
* \[[`a58c98ea85`](https://github.com/nodejs/node/commit/a58c98ea85)] - **(SEMVER-MINOR)** **src**: print string content better in BlobDeserializer (Joyee Cheung) [#50960](https://github.com/nodejs/node/pull/50960)
* \[[`c3c0a3ee5c`](https://github.com/nodejs/node/commit/c3c0a3ee5c)] - **(SEMVER-MINOR)** **src**: support multi-line values for .env file (IlyasShabi) [#51289](https://github.com/nodejs/node/pull/51289)
* \[[`2a921966c6`](https://github.com/nodejs/node/commit/2a921966c6)] - **(SEMVER-MINOR)** **src**: do not coerce dotenv paths (Tobias Nießen) [#51425](https://github.com/nodejs/node/pull/51425)
* \[[`0dee86f295`](https://github.com/nodejs/node/commit/0dee86f295)] - **(SEMVER-MINOR)** **src**: support configurable snapshot (Joyee Cheung) [#50453](https://github.com/nodejs/node/pull/50453)
* \[[`ade6614067`](https://github.com/nodejs/node/commit/ade6614067)] - **(SEMVER-MINOR)** **stream**: add support for `deflate-raw` format to webstreams compression (Damian Krzeminski) [#50097](https://github.com/nodejs/node/pull/50097)
* \[[`fe922f05e4`](https://github.com/nodejs/node/commit/fe922f05e4)] - **(SEMVER-MINOR)** **timers**: export timers.promises (Marco Ippolito) [#51246](https://github.com/nodejs/node/pull/51246)

### Commits

* \[[`cbda4e9fc5`](https://github.com/nodejs/node/commit/cbda4e9fc5)] - **assert,crypto**: make KeyObject and CryptoKey testable for equality (Filip Skokan) [#50897](https://github.com/nodejs/node/pull/50897)
* \[[`92fca59647`](https://github.com/nodejs/node/commit/92fca59647)] - **async\_hooks,inspector**: implement inspector api without async\_wrap (Gabriel Bota) [#51501](https://github.com/nodejs/node/pull/51501)
* \[[`029ca982dc`](https://github.com/nodejs/node/commit/029ca982dc)] - **benchmark**: update iterations of benchmark/async\_hooks/async-local- (Lei Shi) [#51420](https://github.com/nodejs/node/pull/51420)
* \[[`350e9fee8d`](https://github.com/nodejs/node/commit/350e9fee8d)] - **benchmark**: update iterations of benchmark/domain/domain-fn-args.js (Lei Shi) [#51408](https://github.com/nodejs/node/pull/51408)
* \[[`40fda97deb`](https://github.com/nodejs/node/commit/40fda97deb)] - **benchmark**: update iterations of assert/deepequal-typedarrays.js (Lei Shi) [#51419](https://github.com/nodejs/node/pull/51419)
* \[[`1b2e3b7049`](https://github.com/nodejs/node/commit/1b2e3b7049)] - **benchmark**: update iterations of benchmark/assert/deepequal-map.js (Lei Shi) [#51416](https://github.com/nodejs/node/pull/51416)
* \[[`7639259203`](https://github.com/nodejs/node/commit/7639259203)] - **benchmark**: rename startup.js to startup-core.js (Joyee Cheung) [#51669](https://github.com/nodejs/node/pull/51669)
* \[[`4be33b5577`](https://github.com/nodejs/node/commit/4be33b5577)] - **benchmark**: remove dependency on unshipped tools (Adam Majer) [#51146](https://github.com/nodejs/node/pull/51146)
* \[[`bd03a154a9`](https://github.com/nodejs/node/commit/bd03a154a9)] - **benchmark**: update iterations in benchmark/perf\_hooks (Lei Shi) [#50869](https://github.com/nodejs/node/pull/50869)
* \[[`19b943b909`](https://github.com/nodejs/node/commit/19b943b909)] - **benchmark**: update iterations in benchmark/crypto/aes-gcm-throughput.js (Lei Shi) [#50929](https://github.com/nodejs/node/pull/50929)
* \[[`278c990dea`](https://github.com/nodejs/node/commit/278c990dea)] - **benchmark**: update iteration and size in benchmark/crypto/randomBytes.js (Lei Shi) [#50868](https://github.com/nodejs/node/pull/50868)
* \[[`443d4fcff3`](https://github.com/nodejs/node/commit/443d4fcff3)] - **benchmark**: add undici websocket benchmark (Chenyu Yang) [#50586](https://github.com/nodejs/node/pull/50586)
* \[[`3ab6143380`](https://github.com/nodejs/node/commit/3ab6143380)] - **benchmark**: add create-hash benchmark (Joyee Cheung) [#51026](https://github.com/nodejs/node/pull/51026)
* \[[`6a8ff09332`](https://github.com/nodejs/node/commit/6a8ff09332)] - **benchmark**: update interations and len in benchmark/util/text-decoder.js (Lei Shi) [#50938](https://github.com/nodejs/node/pull/50938)
* \[[`22b53bc1fa`](https://github.com/nodejs/node/commit/22b53bc1fa)] - **benchmark**: update iterations of benchmark/util/type-check.js (Lei Shi) [#50937](https://github.com/nodejs/node/pull/50937)
* \[[`f56bda5109`](https://github.com/nodejs/node/commit/f56bda5109)] - **benchmark**: update iterations in benchmark/util/normalize-encoding.js (Lei Shi) [#50934](https://github.com/nodejs/node/pull/50934)
* \[[`4fc83e1ce3`](https://github.com/nodejs/node/commit/4fc83e1ce3)] - **benchmark**: update iterations in benchmark/util/inspect-array.js (Lei Shi) [#50933](https://github.com/nodejs/node/pull/50933)
* \[[`0edddcfc19`](https://github.com/nodejs/node/commit/0edddcfc19)] - **benchmark**: update iterations in benchmark/util/format.js (Lei Shi) [#50932](https://github.com/nodejs/node/pull/50932)
* \[[`f109961fd1`](https://github.com/nodejs/node/commit/f109961fd1)] - **benchmark**: update iterations in benchmark/crypto/hkdf.js (Lei Shi) [#50866](https://github.com/nodejs/node/pull/50866)
* \[[`1e923f11f2`](https://github.com/nodejs/node/commit/1e923f11f2)] - **benchmark**: update iterations in benchmark/crypto/get-ciphers.js (Lei Shi) [#50863](https://github.com/nodejs/node/pull/50863)
* \[[`f13643da06`](https://github.com/nodejs/node/commit/f13643da06)] - **benchmark**: update number of iterations for `util.inspect` (kylo5aby) [#50651](https://github.com/nodejs/node/pull/50651)
* \[[`03b19cbd2a`](https://github.com/nodejs/node/commit/03b19cbd2a)] - **bootstrap**: improve snapshot unsupported builtin warnings (Joyee Cheung) [#50944](https://github.com/nodejs/node/pull/50944)
* \[[`51ea5b60a9`](https://github.com/nodejs/node/commit/51ea5b60a9)] - **build**: fix arm64 host cross-compilation in GN (Cheng Zhao) [#51903](https://github.com/nodejs/node/pull/51903)
* \[[`9f5547afa2`](https://github.com/nodejs/node/commit/9f5547afa2)] - _**Revert**_ "**build**: workaround for node-core-utils" (Richard Lau) [#51975](https://github.com/nodejs/node/pull/51975)
* \[[`58255e73ae`](https://github.com/nodejs/node/commit/58255e73ae)] - **build**: respect the `NODE` env variable in `Makefile` (Antoine du Hamel) [#51743](https://github.com/nodejs/node/pull/51743)
* \[[`0a7419bf0b`](https://github.com/nodejs/node/commit/0a7419bf0b)] - _**Revert**_ "**build**: fix warning in cares under GN build" (Luigi Pinca) [#51865](https://github.com/nodejs/node/pull/51865)
* \[[`4118174b85`](https://github.com/nodejs/node/commit/4118174b85)] - **build**: remove `librt` libs link for Android compatibility (BuShe Pie) [#51632](https://github.com/nodejs/node/pull/51632)
* \[[`012da16b85`](https://github.com/nodejs/node/commit/012da16b85)] - **build**: do not rely on gn\_helpers in GN build (Cheng Zhao) [#51439](https://github.com/nodejs/node/pull/51439)
* \[[`93fcf52990`](https://github.com/nodejs/node/commit/93fcf52990)] - **build**: fix warning in cares under GN build (Cheng Zhao) [#51687](https://github.com/nodejs/node/pull/51687)
* \[[`2176495455`](https://github.com/nodejs/node/commit/2176495455)] - **build**: fix building js2c with GN (Cheng Zhao) [#51818](https://github.com/nodejs/node/pull/51818)
* \[[`d6e702f885`](https://github.com/nodejs/node/commit/d6e702f885)] - **build**: encode non-ASCII Latin1 characters as one byte in JS2C (Joyee Cheung) [#51605](https://github.com/nodejs/node/pull/51605)
* \[[`4f49e9d000`](https://github.com/nodejs/node/commit/4f49e9d000)] - **(SEMVER-MINOR)** **build**: build opt to set local location of headers (Michael Dawson) [#51525](https://github.com/nodejs/node/pull/51525)
* \[[`8e84aad0ef`](https://github.com/nodejs/node/commit/8e84aad0ef)] - **build**: use macOS m1 machines for testing (Yagiz Nizipli) [#51620](https://github.com/nodejs/node/pull/51620)
* \[[`5fce1a17e2`](https://github.com/nodejs/node/commit/5fce1a17e2)] - **build**: check before removing %config% link (liudonghua) [#51437](https://github.com/nodejs/node/pull/51437)
* \[[`46d6dce1a8`](https://github.com/nodejs/node/commit/46d6dce1a8)] - **build**: increase parallel executions in github (Yagiz Nizipli) [#51554](https://github.com/nodejs/node/pull/51554)
* \[[`8b3ead1f3e`](https://github.com/nodejs/node/commit/8b3ead1f3e)] - **build**: remove copyright header in node.gni (Cheng Zhao) [#51535](https://github.com/nodejs/node/pull/51535)
* \[[`d8b86ad363`](https://github.com/nodejs/node/commit/d8b86ad363)] - **build**: update GN build files for ngtcp2 (Cheng Zhao) [#51313](https://github.com/nodejs/node/pull/51313)
* \[[`ba0ffddd2d`](https://github.com/nodejs/node/commit/ba0ffddd2d)] - **build**: fix for VScode "Reopen in Container" (Serg Kryvonos) [#51271](https://github.com/nodejs/node/pull/51271)
* \[[`8b97e2e0a7`](https://github.com/nodejs/node/commit/8b97e2e0a7)] - **build**: add `-flax-vector-conversions` to V8 build (Michaël Zasso) [#51257](https://github.com/nodejs/node/pull/51257)
* \[[`bd528c7dc0`](https://github.com/nodejs/node/commit/bd528c7dc0)] - **build**: fix warnings from uv for gn build (Cheng Zhao) [#51069](https://github.com/nodejs/node/pull/51069)
* \[[`ffe467b062`](https://github.com/nodejs/node/commit/ffe467b062)] - **build,tools**: make addons tests work with GN (Cheng Zhao) [#50737](https://github.com/nodejs/node/pull/50737)
* \[[`448d67109a`](https://github.com/nodejs/node/commit/448d67109a)] - **(SEMVER-MINOR)** **crypto**: implement crypto.hash() (Joyee Cheung) [#51044](https://github.com/nodejs/node/pull/51044)
* \[[`48959dd2b4`](https://github.com/nodejs/node/commit/48959dd2b4)] - **crypto**: update root certificates to NSS 3.98 (Node.js GitHub Bot) [#51794](https://github.com/nodejs/node/pull/51794)
* \[[`68e8b2c492`](https://github.com/nodejs/node/commit/68e8b2c492)] - **crypto**: use EVP\_MD\_fetch and cache EVP\_MD for hashes (Joyee Cheung) [#51034](https://github.com/nodejs/node/pull/51034)
* \[[`adb5d69621`](https://github.com/nodejs/node/commit/adb5d69621)] - **crypto**: update CryptoKey symbol properties (Filip Skokan) [#50897](https://github.com/nodejs/node/pull/50897)
* \[[`df0213fd3d`](https://github.com/nodejs/node/commit/df0213fd3d)] - **deps**: update nghttp2 to 1.60.0 (Node.js GitHub Bot) [#51948](https://github.com/nodejs/node/pull/51948)
* \[[`208dd887a5`](https://github.com/nodejs/node/commit/208dd887a5)] - **deps**: upgrade npm to 10.5.0 (npm team) [#51913](https://github.com/nodejs/node/pull/51913)
* \[[`587e70e1ee`](https://github.com/nodejs/node/commit/587e70e1ee)] - **deps**: update corepack to 0.25.2 (Node.js GitHub Bot) [#51810](https://github.com/nodejs/node/pull/51810)
* \[[`38343c4857`](https://github.com/nodejs/node/commit/38343c4857)] - **deps**: update c-ares to 1.27.0 (Node.js GitHub Bot) [#51846](https://github.com/nodejs/node/pull/51846)
* \[[`c9974f621c`](https://github.com/nodejs/node/commit/c9974f621c)] - **deps**: update c-ares to 1.26.0 (Node.js GitHub Bot) [#51582](https://github.com/nodejs/node/pull/51582)
* \[[`0aa18e1a1c`](https://github.com/nodejs/node/commit/0aa18e1a1c)] - **deps**: update googletest to 6a59382 (Node.js GitHub Bot) [#51580](https://github.com/nodejs/node/pull/51580)
* \[[`f871bc6ddc`](https://github.com/nodejs/node/commit/f871bc6ddc)] - **deps**: update nghttp2 to 1.59.0 (Node.js GitHub Bot) [#51581](https://github.com/nodejs/node/pull/51581)
* \[[`94f8ee8717`](https://github.com/nodejs/node/commit/94f8ee8717)] - **deps**: update corepack to 0.24.1 (Node.js GitHub Bot) [#51459](https://github.com/nodejs/node/pull/51459)
* \[[`c23ce06e6b`](https://github.com/nodejs/node/commit/c23ce06e6b)] - **deps**: update ada to 2.7.6 (Node.js GitHub Bot) [#51542](https://github.com/nodejs/node/pull/51542)
* \[[`372ce69de1`](https://github.com/nodejs/node/commit/372ce69de1)] - **deps**: update ada to 2.7.5 (Node.js GitHub Bot) [#51542](https://github.com/nodejs/node/pull/51542)
* \[[`133719b2c9`](https://github.com/nodejs/node/commit/133719b2c9)] - **deps**: update googletest to 7c07a86 (Node.js GitHub Bot) [#51458](https://github.com/nodejs/node/pull/51458)
* \[[`35675aa07f`](https://github.com/nodejs/node/commit/35675aa07f)] - **deps**: update acorn-walk to 8.3.2 (Node.js GitHub Bot) [#51457](https://github.com/nodejs/node/pull/51457)
* \[[`ca73f55a22`](https://github.com/nodejs/node/commit/ca73f55a22)] - **deps**: update base64 to 0.5.2 (Node.js GitHub Bot) [#51455](https://github.com/nodejs/node/pull/51455)
* \[[`c9dad18191`](https://github.com/nodejs/node/commit/c9dad18191)] - **deps**: compile c-ares with C11 support (Michaël Zasso) [#51410](https://github.com/nodejs/node/pull/51410)
* \[[`a727fa73ee`](https://github.com/nodejs/node/commit/a727fa73ee)] - **deps**: upgrade npm to 10.3.0 (npm team) [#51431](https://github.com/nodejs/node/pull/51431)
* \[[`834bbfd039`](https://github.com/nodejs/node/commit/834bbfd039)] - **deps**: update c-ares to 1.25.0 (Node.js GitHub Bot) [#51385](https://github.com/nodejs/node/pull/51385)
* \[[`4c8fa3e7c2`](https://github.com/nodejs/node/commit/4c8fa3e7c2)] - **deps**: update uvwasi to 0.0.20 and fixup tests (Michael Dawson) [#51355](https://github.com/nodejs/node/pull/51355)
* \[[`bd183ef2af`](https://github.com/nodejs/node/commit/bd183ef2af)] - **deps**: add nghttp3/\*\*/.deps to .gitignore (Luigi Pinca) [#51400](https://github.com/nodejs/node/pull/51400)
* \[[`1d8169995c`](https://github.com/nodejs/node/commit/1d8169995c)] - **deps**: update corepack to 0.24.0 (Node.js GitHub Bot) [#51318](https://github.com/nodejs/node/pull/51318)
* \[[`4dfbbb8789`](https://github.com/nodejs/node/commit/4dfbbb8789)] - **deps**: update acorn to 8.11.3 (Node.js GitHub Bot) [#51317](https://github.com/nodejs/node/pull/51317)
* \[[`7d60877fa3`](https://github.com/nodejs/node/commit/7d60877fa3)] - **deps**: update brotli to 1.1.0 (Node.js GitHub Bot) [#50804](https://github.com/nodejs/node/pull/50804)
* \[[`1b99a3f0af`](https://github.com/nodejs/node/commit/1b99a3f0af)] - **deps**: update zlib to 1.3.0.1-motley-40e35a7 (Node.js GitHub Bot) [#51274](https://github.com/nodejs/node/pull/51274)
* \[[`2270285839`](https://github.com/nodejs/node/commit/2270285839)] - **deps**: update simdutf to 4.0.8 (Node.js GitHub Bot) [#51000](https://github.com/nodejs/node/pull/51000)
* \[[`61d1535d84`](https://github.com/nodejs/node/commit/61d1535d84)] - **deps**: V8: cherry-pick de611e69ad51 (Keyhan Vakil) [#51200](https://github.com/nodejs/node/pull/51200)
* \[[`04323fd595`](https://github.com/nodejs/node/commit/04323fd595)] - **deps**: update googletest to 530d5c8 (Node.js GitHub Bot) [#51191](https://github.com/nodejs/node/pull/51191)
* \[[`454b4f8d7e`](https://github.com/nodejs/node/commit/454b4f8d7e)] - **deps**: update acorn-walk to 8.3.1 (Node.js GitHub Bot) [#50457](https://github.com/nodejs/node/pull/50457)
* \[[`cc693eb908`](https://github.com/nodejs/node/commit/cc693eb908)] - **deps**: update acorn-walk to 8.3.0 (Node.js GitHub Bot) [#50457](https://github.com/nodejs/node/pull/50457)
* \[[`09519c6655`](https://github.com/nodejs/node/commit/09519c6655)] - **deps**: update zlib to 1.3.0.1-motley-dd5fc13 (Node.js GitHub Bot) [#51105](https://github.com/nodejs/node/pull/51105)
* \[[`a2f39e9168`](https://github.com/nodejs/node/commit/a2f39e9168)] - **deps**: V8: cherry-pick 0fd478bcdabd (Joyee Cheung) [#50572](https://github.com/nodejs/node/pull/50572)
* \[[`1aaf156ea7`](https://github.com/nodejs/node/commit/1aaf156ea7)] - **deps**: update zlib to 1.3-22124f5 (Node.js GitHub Bot) [#50910](https://github.com/nodejs/node/pull/50910)
* \[[`3f4e254047`](https://github.com/nodejs/node/commit/3f4e254047)] - **deps**: update googletest to 76bb2af (Node.js GitHub Bot) [#50555](https://github.com/nodejs/node/pull/50555)
* \[[`702684c008`](https://github.com/nodejs/node/commit/702684c008)] - **deps**: update googletest to b10fad3 (Node.js GitHub Bot) [#50555](https://github.com/nodejs/node/pull/50555)
* \[[`4ee7f29657`](https://github.com/nodejs/node/commit/4ee7f29657)] - **deps**: update timezone to 2024a (Michaël Zasso) [#51723](https://github.com/nodejs/node/pull/51723)
* \[[`452d74c8b6`](https://github.com/nodejs/node/commit/452d74c8b6)] - **deps**: update icu to 74.2 (Michaël Zasso) [#51723](https://github.com/nodejs/node/pull/51723)
* \[[`e6fc5a5ee1`](https://github.com/nodejs/node/commit/e6fc5a5ee1)] - **deps**: update timezone to 2023d (Node.js GitHub Bot) [#51461](https://github.com/nodejs/node/pull/51461)
* \[[`4ee0f8306b`](https://github.com/nodejs/node/commit/4ee0f8306b)] - **deps**: update icu to 74.1 (Node.js GitHub Bot) [#50515](https://github.com/nodejs/node/pull/50515)
* \[[`cb49f31480`](https://github.com/nodejs/node/commit/cb49f31480)] - **deps**: cherry-pick libuv/libuv\@d09441c (Richard Lau) [#51976](https://github.com/nodejs/node/pull/51976)
* \[[`ea50540c5e`](https://github.com/nodejs/node/commit/ea50540c5e)] - _**Revert**_ "**deps**: V8: cherry-pick 13192d6e10fa" (kxxt) [#51495](https://github.com/nodejs/node/pull/51495)
* \[[`6fd1617ab4`](https://github.com/nodejs/node/commit/6fd1617ab4)] - **doc**: add policy for distribution (Geoffrey Booth) [#51918](https://github.com/nodejs/node/pull/51918)
* \[[`fc0b389006`](https://github.com/nodejs/node/commit/fc0b389006)] - **doc**: fix actual result of example is different in events (Deokjin Kim) [#51925](https://github.com/nodejs/node/pull/51925)
* \[[`93d6d66339`](https://github.com/nodejs/node/commit/93d6d66339)] - **doc**: clarify Corepack threat model (Antoine du Hamel) [#51917](https://github.com/nodejs/node/pull/51917)
* \[[`276d1d1d65`](https://github.com/nodejs/node/commit/276d1d1d65)] - **doc**: add stability index to crypto.hash() (Joyee Cheung) [#51978](https://github.com/nodejs/node/pull/51978)
* \[[`473af948b5`](https://github.com/nodejs/node/commit/473af948b5)] - **doc**: remove redundant backquote which breaks sentence (JounQin) [#51904](https://github.com/nodejs/node/pull/51904)
* \[[`b52b249b05`](https://github.com/nodejs/node/commit/b52b249b05)] - **doc**: update node-api/node-addon-api team link to sharing project news (Ulises Gascón) [#51877](https://github.com/nodejs/node/pull/51877)
* \[[`a74c373ea4`](https://github.com/nodejs/node/commit/a74c373ea4)] - **doc**: add website team to sharing project news (Ulises Gascón) [#49002](https://github.com/nodejs/node/pull/49002)
* \[[`b7ce547d41`](https://github.com/nodejs/node/commit/b7ce547d41)] - **doc**: update guide link for Event Loop (Shrujal Shah) [#51874](https://github.com/nodejs/node/pull/51874)
* \[[`3dfee7ee33`](https://github.com/nodejs/node/commit/3dfee7ee33)] - **doc**: change `ExperimentalWarnings` to `ExperimentalWarning` (Ameet Kaustav) [#51741](https://github.com/nodejs/node/pull/51741)
* \[[`740d0679e7`](https://github.com/nodejs/node/commit/740d0679e7)] - **doc**: add Paolo to TSC members (Michael Dawson) [#51825](https://github.com/nodejs/node/pull/51825)
* \[[`3240a2f349`](https://github.com/nodejs/node/commit/3240a2f349)] - **doc**: reserve 123 for Electron 30 (Keeley Hammond) [#51803](https://github.com/nodejs/node/pull/51803)
* \[[`597e3db0f9`](https://github.com/nodejs/node/commit/597e3db0f9)] - **doc**: add mention to GPG\_TTY (Rafael Gonzaga) [#51806](https://github.com/nodejs/node/pull/51806)
* \[[`ccdb01187b`](https://github.com/nodejs/node/commit/ccdb01187b)] - **doc**: add zcbenz to collaborators (Cheng Zhao) [#51812](https://github.com/nodejs/node/pull/51812)
* \[[`3a3de00437`](https://github.com/nodejs/node/commit/3a3de00437)] - **doc**: add entry to stewards (Rafael Gonzaga) [#51760](https://github.com/nodejs/node/pull/51760)
* \[[`06b882d2fa`](https://github.com/nodejs/node/commit/06b882d2fa)] - **doc**: update technical priorities for 2023 (Jean Burellier) [#47523](https://github.com/nodejs/node/pull/47523)
* \[[`9a68b47fe1`](https://github.com/nodejs/node/commit/9a68b47fe1)] - **doc**: mark isWebAssemblyCompiledModule eol (Marco Ippolito) [#51442](https://github.com/nodejs/node/pull/51442)
* \[[`8016628710`](https://github.com/nodejs/node/commit/8016628710)] - **doc**: fix `globals.md` introduction (Antoine du Hamel) [#51742](https://github.com/nodejs/node/pull/51742)
* \[[`9ddbe4523f`](https://github.com/nodejs/node/commit/9ddbe4523f)] - **doc**: updates for better json generating (Dmitry Semigradsky) [#51592](https://github.com/nodejs/node/pull/51592)
* \[[`140cf26d47`](https://github.com/nodejs/node/commit/140cf26d47)] - **doc**: document the GN build (Cheng Zhao) [#51676](https://github.com/nodejs/node/pull/51676)
* \[[`ecfb3f18b3`](https://github.com/nodejs/node/commit/ecfb3f18b3)] - **doc**: fix uncaught exception example (Gabriel Schulhof) [#51638](https://github.com/nodejs/node/pull/51638)
* \[[`b3157a08bf`](https://github.com/nodejs/node/commit/b3157a08bf)] - **doc**: clarify execution of `after` hook on test suite completion (Ognjen Jevremović) [#51523](https://github.com/nodejs/node/pull/51523)
* \[[`1dae1873d9`](https://github.com/nodejs/node/commit/1dae1873d9)] - **doc**: fix `dns.lookup` and `dnsPromises.lookup` description (Duncan Chiu) [#51517](https://github.com/nodejs/node/pull/51517)
* \[[`50df052087`](https://github.com/nodejs/node/commit/50df052087)] - **doc**: note that path.normalize deviates from POSIX (Tobias Nießen) [#51513](https://github.com/nodejs/node/pull/51513)
* \[[`481af53aea`](https://github.com/nodejs/node/commit/481af53aea)] - **doc**: add lemire to collaborators (Daniel Lemire) [#51572](https://github.com/nodejs/node/pull/51572)
* \[[`dec0d5d19a`](https://github.com/nodejs/node/commit/dec0d5d19a)] - **doc**: fix historical experimental fetch flag (Kenrick) [#51506](https://github.com/nodejs/node/pull/51506)
* \[[`96c480b1a1`](https://github.com/nodejs/node/commit/96c480b1a1)] - **doc**: fix type of connectionAttempt parameter (Rafael Gonzaga) [#51490](https://github.com/nodejs/node/pull/51490)
* \[[`76968ab112`](https://github.com/nodejs/node/commit/76968ab112)] - **doc**: remove reference to resolved child\_process v8 issue (Ian Kerins) [#51467](https://github.com/nodejs/node/pull/51467)
* \[[`bdd3a2a9fd`](https://github.com/nodejs/node/commit/bdd3a2a9fd)] - **doc**: update typos (Aranđel Šarenac) [#51475](https://github.com/nodejs/node/pull/51475)
* \[[`3532f5587c`](https://github.com/nodejs/node/commit/3532f5587c)] - **doc**: add notes on inspector breakpoints (Chengzhong Wu) [#51417](https://github.com/nodejs/node/pull/51417)
* \[[`0dffb9f049`](https://github.com/nodejs/node/commit/0dffb9f049)] - **doc**: add links in `offboarding.md` (Antoine du Hamel) [#51440](https://github.com/nodejs/node/pull/51440)
* \[[`58d2442f0f`](https://github.com/nodejs/node/commit/58d2442f0f)] - **doc**: fix spelling mistake (u9g) [#51454](https://github.com/nodejs/node/pull/51454)
* \[[`a09f440dbd`](https://github.com/nodejs/node/commit/a09f440dbd)] - **doc**: add check for security reverts (Michael Dawson) [#51376](https://github.com/nodejs/node/pull/51376)
* \[[`401837bfc4`](https://github.com/nodejs/node/commit/401837bfc4)] - **doc**: fix some policy scope typos (Tim Kuijsten) [#51234](https://github.com/nodejs/node/pull/51234)
* \[[`f301f829ba`](https://github.com/nodejs/node/commit/f301f829ba)] - **doc**: improve subtests documentation (Marco Ippolito) [#51379](https://github.com/nodejs/node/pull/51379)
* \[[`1e40f552fd`](https://github.com/nodejs/node/commit/1e40f552fd)] - **doc**: add missing word in `child_process.md` (Joseph Joy) [#50370](https://github.com/nodejs/node/pull/50370)
* \[[`42b4f0f5ab`](https://github.com/nodejs/node/commit/42b4f0f5ab)] - **doc**: fixup alignment of warning subsection (James M Snell) [#51374](https://github.com/nodejs/node/pull/51374)
* \[[`b5bc597871`](https://github.com/nodejs/node/commit/b5bc597871)] - **doc**: the GN files should use Node's license (Cheng Zhao) [#50694](https://github.com/nodejs/node/pull/50694)
* \[[`01a41041d6`](https://github.com/nodejs/node/commit/01a41041d6)] - **doc**: improve localWindowSize event descriptions (Davy Landman) [#51071](https://github.com/nodejs/node/pull/51071)
* \[[`63aa27df10`](https://github.com/nodejs/node/commit/63aa27df10)] - **doc**: mark `--jitless` as experimental (Antoine du Hamel) [#51247](https://github.com/nodejs/node/pull/51247)
* \[[`c8233912e9`](https://github.com/nodejs/node/commit/c8233912e9)] - **doc**: run license-builder (github-actions\[bot]) [#51199](https://github.com/nodejs/node/pull/51199)
* \[[`9e360df521`](https://github.com/nodejs/node/commit/9e360df521)] - **doc**: fix limitations and known issues in pm (Rafael Gonzaga) [#51184](https://github.com/nodejs/node/pull/51184)
* \[[`52d8222d32`](https://github.com/nodejs/node/commit/52d8222d32)] - **doc**: mention node:wasi in the Threat Model (Rafael Gonzaga) [#51211](https://github.com/nodejs/node/pull/51211)
* \[[`cb3270e4c8`](https://github.com/nodejs/node/commit/cb3270e4c8)] - **doc**: remove ambiguous 'considered' (Rich Trott) [#51207](https://github.com/nodejs/node/pull/51207)
* \[[`979e183e0c`](https://github.com/nodejs/node/commit/979e183e0c)] - **doc**: set exit code in custom test runner example (Matteo Collina) [#51056](https://github.com/nodejs/node/pull/51056)
* \[[`eaadebb1f4`](https://github.com/nodejs/node/commit/eaadebb1f4)] - **doc**: remove version from `maintaining-dependencies.md` (Antoine du Hamel) [#51195](https://github.com/nodejs/node/pull/51195)
* \[[`256db6e056`](https://github.com/nodejs/node/commit/256db6e056)] - **doc**: mention native addons are restricted in pm (Rafael Gonzaga) [#51185](https://github.com/nodejs/node/pull/51185)
* \[[`2a61602ab2`](https://github.com/nodejs/node/commit/2a61602ab2)] - **doc**: correct note on behavior of stats.isDirectory (Nick Reilingh) [#50946](https://github.com/nodejs/node/pull/50946)
* \[[`184b8bea5b`](https://github.com/nodejs/node/commit/184b8bea5b)] - **doc**: fix `TestsStream` parent class (Jungku Lee) [#51181](https://github.com/nodejs/node/pull/51181)
* \[[`c61597ffe4`](https://github.com/nodejs/node/commit/c61597ffe4)] - **(SEMVER-MINOR)** **doc**: add documentation for --build-snapshot-config (Anna Henningsen) [#50453](https://github.com/nodejs/node/pull/50453)
* \[[`b88170d602`](https://github.com/nodejs/node/commit/b88170d602)] - **doc**: run license-builder (github-actions\[bot]) [#51111](https://github.com/nodejs/node/pull/51111)
* \[[`f2b4626ab8`](https://github.com/nodejs/node/commit/f2b4626ab8)] - **doc**: deprecate hash constructor (Marco Ippolito) [#51077](https://github.com/nodejs/node/pull/51077)
* \[[`6c241550cd`](https://github.com/nodejs/node/commit/6c241550cd)] - **doc**: add note regarding `--experimental-detect-module` (Shubherthi Mitra) [#51089](https://github.com/nodejs/node/pull/51089)
* \[[`8ee30ea900`](https://github.com/nodejs/node/commit/8ee30ea900)] - **doc**: correct tracingChannel.traceCallback() (Gerhard Stöbich) [#51068](https://github.com/nodejs/node/pull/51068)
* \[[`1cd27b6eff`](https://github.com/nodejs/node/commit/1cd27b6eff)] - **doc**: use length argument in pbkdf2Key (Tobias Nießen) [#51066](https://github.com/nodejs/node/pull/51066)
* \[[`09ad974537`](https://github.com/nodejs/node/commit/09ad974537)] - **doc**: add deprecation notice to `dirent.path` (Antoine du Hamel) [#51059](https://github.com/nodejs/node/pull/51059)
* \[[`1113e58f87`](https://github.com/nodejs/node/commit/1113e58f87)] - **doc**: deprecate `dirent.path` (Antoine du Hamel) [#51020](https://github.com/nodejs/node/pull/51020)
* \[[`37979d750e`](https://github.com/nodejs/node/commit/37979d750e)] - **doc**: add additional details about `--input-type` (Shubham Pandey) [#50796](https://github.com/nodejs/node/pull/50796)
* \[[`3ff00e1e79`](https://github.com/nodejs/node/commit/3ff00e1e79)] - **doc**: add procedure when CVEs don't get published (Rafael Gonzaga) [#50945](https://github.com/nodejs/node/pull/50945)
* \[[`0930be6bd5`](https://github.com/nodejs/node/commit/0930be6bd5)] - **doc**: fix some errors in esm resolution algorithms (Christopher Jeffrey (JJ)) [#50898](https://github.com/nodejs/node/pull/50898)
* \[[`ddc7964b03`](https://github.com/nodejs/node/commit/ddc7964b03)] - **doc**: reserve 121 for Electron 29 (Shelley Vohr) [#50957](https://github.com/nodejs/node/pull/50957)
* \[[`625fd69b76`](https://github.com/nodejs/node/commit/625fd69b76)] - **doc**: run license-builder (github-actions\[bot]) [#50926](https://github.com/nodejs/node/pull/50926)
* \[[`f18269607a`](https://github.com/nodejs/node/commit/f18269607a)] - **doc**: document non-node\_modules-only runtime deprecation (Joyee Cheung) [#50748](https://github.com/nodejs/node/pull/50748)
* \[[`5f8e7a0fdb`](https://github.com/nodejs/node/commit/5f8e7a0fdb)] - **doc**: add doc for Unix abstract socket (theanarkh) [#50904](https://github.com/nodejs/node/pull/50904)
* \[[`e0598787e0`](https://github.com/nodejs/node/commit/e0598787e0)] - **doc**: remove flicker on page load on dark theme (Dima Demakov) [#50942](https://github.com/nodejs/node/pull/50942)
* \[[`2a7047d933`](https://github.com/nodejs/node/commit/2a7047d933)] - **doc,crypto**: further clarify RSA\_PKCS1\_PADDING support (Tobias Nießen) [#51799](https://github.com/nodejs/node/pull/51799)
* \[[`31c4ba4dfd`](https://github.com/nodejs/node/commit/31c4ba4dfd)] - **doc,crypto**: add changelog and note about disabled RSA\_PKCS1\_PADDING (Filip Skokan) [#51782](https://github.com/nodejs/node/pull/51782)
* \[[`90da41548f`](https://github.com/nodejs/node/commit/90da41548f)] - **doc,module**: clarify hook chain execution sequence (Jacob Smith) [#51884](https://github.com/nodejs/node/pull/51884)
* \[[`bb7d7f3d1c`](https://github.com/nodejs/node/commit/bb7d7f3d1c)] - **errors**: fix stacktrace of SystemError (uzlopak) [#49956](https://github.com/nodejs/node/pull/49956)
* \[[`db7459b57b`](https://github.com/nodejs/node/commit/db7459b57b)] - **errors**: improve hideStackFrames (Aras Abbasi) [#49990](https://github.com/nodejs/node/pull/49990)
* \[[`a6b3569121`](https://github.com/nodejs/node/commit/a6b3569121)] - **esm**: improve error when calling `import.meta.resolve` from `data:` URL (Antoine du Hamel) [#49516](https://github.com/nodejs/node/pull/49516)
* \[[`38f4000905`](https://github.com/nodejs/node/commit/38f4000905)] - **esm**: fix hint on invalid module specifier (Antoine du Hamel) [#51223](https://github.com/nodejs/node/pull/51223)
* \[[`e39e37bbd5`](https://github.com/nodejs/node/commit/e39e37bbd5)] - **esm**: fix hook name in error message (Bruce MacNaughton) [#50466](https://github.com/nodejs/node/pull/50466)
* \[[`d9b5cd533c`](https://github.com/nodejs/node/commit/d9b5cd533c)] - **events**: no stopPropagation call in cancelBubble (mert.altin) [#50405](https://github.com/nodejs/node/pull/50405)
* \[[`287a02c4b2`](https://github.com/nodejs/node/commit/287a02c4b2)] - **fs**: load rimraf lazily in fs/promises (Joyee Cheung) [#51617](https://github.com/nodejs/node/pull/51617)
* \[[`bbd1351ef0`](https://github.com/nodejs/node/commit/bbd1351ef0)] - **fs**: remove race condition for recursive watch on Linux (Matteo Collina) [#51406](https://github.com/nodejs/node/pull/51406)
* \[[`1b7ccec5a7`](https://github.com/nodejs/node/commit/1b7ccec5a7)] - **fs**: update jsdoc for `filehandle.createWriteStream` and `appendFile` (Jungku Lee) [#51494](https://github.com/nodejs/node/pull/51494)
* \[[`25056f5024`](https://github.com/nodejs/node/commit/25056f5024)] - **fs**: fix fs.promises.realpath for long paths on Windows (翠 / green) [#51032](https://github.com/nodejs/node/pull/51032)
* \[[`a8fd01a5a2`](https://github.com/nodejs/node/commit/a8fd01a5a2)] - **fs**: make offset, position & length args in fh.read() optional (Pulkit Gupta) [#51087](https://github.com/nodejs/node/pull/51087)
* \[[`721557c6d8`](https://github.com/nodejs/node/commit/721557c6d8)] - **fs**: add missing jsdoc parameters to `readSync` (Yagiz Nizipli) [#51225](https://github.com/nodejs/node/pull/51225)
* \[[`3ce9aacfcd`](https://github.com/nodejs/node/commit/3ce9aacfcd)] - **fs**: remove `internalModuleReadJSON` binding (Yagiz Nizipli) [#51224](https://github.com/nodejs/node/pull/51224)
* \[[`65df2c6787`](https://github.com/nodejs/node/commit/65df2c6787)] - **fs**: improve mkdtemp performance for buffer prefix (Yagiz Nizipli) [#51078](https://github.com/nodejs/node/pull/51078)
* \[[`6705b48012`](https://github.com/nodejs/node/commit/6705b48012)] - **fs**: validate fd synchronously on c++ (Yagiz Nizipli) [#51027](https://github.com/nodejs/node/pull/51027)
* \[[`afd5d67f89`](https://github.com/nodejs/node/commit/afd5d67f89)] - **fs**: throw fchownSync error from c++ (Yagiz Nizipli) [#51075](https://github.com/nodejs/node/pull/51075)
* \[[`bac982bce5`](https://github.com/nodejs/node/commit/bac982bce5)] - **fs**: update params in jsdoc for createReadStream and createWriteStream (Jungku Lee) [#51063](https://github.com/nodejs/node/pull/51063)
* \[[`6764f0c9a8`](https://github.com/nodejs/node/commit/6764f0c9a8)] - **fs**: improve error performance of readvSync (IlyasShabi) [#50100](https://github.com/nodejs/node/pull/50100)
* \[[`0225fce776`](https://github.com/nodejs/node/commit/0225fce776)] - **(SEMVER-MINOR)** **fs**: introduce `dirent.parentPath` (Antoine du Hamel) [#50976](https://github.com/nodejs/node/pull/50976)
* \[[`4adea6c405`](https://github.com/nodejs/node/commit/4adea6c405)] - **fs,test**: add URL to string to fs.watch (Rafael Gonzaga) [#51346](https://github.com/nodejs/node/pull/51346)
* \[[`6bf148e12b`](https://github.com/nodejs/node/commit/6bf148e12b)] - **http**: fix `close` return value mismatch between doc and implementation (kylo5aby) [#51797](https://github.com/nodejs/node/pull/51797)
* \[[`66318602d0`](https://github.com/nodejs/node/commit/66318602d0)] - **http**: split set-cookie when using setHeaders (Marco Ippolito) [#51649](https://github.com/nodejs/node/pull/51649)
* \[[`f7b53d05bd`](https://github.com/nodejs/node/commit/f7b53d05bd)] - **http**: remove misleading warning (Luigi Pinca) [#51204](https://github.com/nodejs/node/pull/51204)
* \[[`9062d30600`](https://github.com/nodejs/node/commit/9062d30600)] - **http**: do not override user-provided options object (KuthorX) [#33633](https://github.com/nodejs/node/pull/33633)
* \[[`4e38dee4ee`](https://github.com/nodejs/node/commit/4e38dee4ee)] - **http**: handle multi-value content-disposition header (Arsalan Ahmad) [#50977](https://github.com/nodejs/node/pull/50977)
* \[[`b560bfbb84`](https://github.com/nodejs/node/commit/b560bfbb84)] - **http2**: close idle connections when allowHTTP1 is true (xsbchen) [#51569](https://github.com/nodejs/node/pull/51569)
* \[[`5ba4d96525`](https://github.com/nodejs/node/commit/5ba4d96525)] - **(SEMVER-MINOR)** **http2**: add h2 compat support for appendHeader (Tim Perry) [#51412](https://github.com/nodejs/node/pull/51412)
* \[[`0861498e8b`](https://github.com/nodejs/node/commit/0861498e8b)] - **(SEMVER-MINOR)** **http2**: add server handshake utility (snek) [#51172](https://github.com/nodejs/node/pull/51172)
* \[[`6b08d006ee`](https://github.com/nodejs/node/commit/6b08d006ee)] - **(SEMVER-MINOR)** **http2**: receive customsettings (Marten Richter) [#51323](https://github.com/nodejs/node/pull/51323)
* \[[`23414a6120`](https://github.com/nodejs/node/commit/23414a6120)] - **http2**: addtl http/2 settings (Marten Richter) [#49025](https://github.com/nodejs/node/pull/49025)
* \[[`3fe59ba224`](https://github.com/nodejs/node/commit/3fe59ba224)] - **inspector**: add NodeRuntime.waitingForDebugger event (mary marchini) [#51560](https://github.com/nodejs/node/pull/51560)
* \[[`44f05e0d30`](https://github.com/nodejs/node/commit/44f05e0d30)] - **lib**: make sure close net server (theanarkh) [#51929](https://github.com/nodejs/node/pull/51929)
* \[[`3be5ff9c45`](https://github.com/nodejs/node/commit/3be5ff9c45)] - **lib**: return directly if udp socket close before lookup (theanarkh) [#51914](https://github.com/nodejs/node/pull/51914)
* \[[`dcbf88f4c7`](https://github.com/nodejs/node/commit/dcbf88f4c7)] - **lib**: account for cwd access from snapshot serialization cb (Anna Henningsen) [#51901](https://github.com/nodejs/node/pull/51901)
* \[[`da8fa484f8`](https://github.com/nodejs/node/commit/da8fa484f8)] - **lib**: fix http client socket path (theanarkh) [#51900](https://github.com/nodejs/node/pull/51900)
* \[[`55011d2c71`](https://github.com/nodejs/node/commit/55011d2c71)] - **lib**: only build the ESM facade for builtins when they are needed (Joyee Cheung) [#51669](https://github.com/nodejs/node/pull/51669)
* \[[`7894989bf0`](https://github.com/nodejs/node/commit/7894989bf0)] - **(SEMVER-MINOR)** **lib**: move encodingsMap to internal/util (Joyee Cheung) [#51044](https://github.com/nodejs/node/pull/51044)
* \[[`9082cc557d`](https://github.com/nodejs/node/commit/9082cc557d)] - **lib**: do not access process.noDeprecation at build time (Joyee Cheung) [#51447](https://github.com/nodejs/node/pull/51447)
* \[[`6679e6b616`](https://github.com/nodejs/node/commit/6679e6b616)] - **lib**: add assertion for user ESM execution (Joyee Cheung) [#51748](https://github.com/nodejs/node/pull/51748)
* \[[`d6e8d03afc`](https://github.com/nodejs/node/commit/d6e8d03afc)] - **lib**: create global console properties at snapshot build time (Joyee Cheung) [#51700](https://github.com/nodejs/node/pull/51700)
* \[[`bd2a3c10ae`](https://github.com/nodejs/node/commit/bd2a3c10ae)] - **lib**: define FormData and fetch etc. in the built-in snapshot (Joyee Cheung) [#51598](https://github.com/nodejs/node/pull/51598)
* \[[`da79876ef0`](https://github.com/nodejs/node/commit/da79876ef0)] - **lib**: allow checking the test result from afterEach (Tim Stableford) [#51485](https://github.com/nodejs/node/pull/51485)
* \[[`bff7e3cf7a`](https://github.com/nodejs/node/commit/bff7e3cf7a)] - **lib**: remove unnecessary refreshHrtimeBuffer() (Joyee Cheung) [#51446](https://github.com/nodejs/node/pull/51446)
* \[[`562947e012`](https://github.com/nodejs/node/commit/562947e012)] - **lib**: fix use of `--frozen-intrinsics` with `--jitless` (Antoine du Hamel) [#51248](https://github.com/nodejs/node/pull/51248)
* \[[`7b83ef749e`](https://github.com/nodejs/node/commit/7b83ef749e)] - **lib**: move function declaration outside of loop (Sanjaiyan Parthipan) [#51242](https://github.com/nodejs/node/pull/51242)
* \[[`0a85b0fd9d`](https://github.com/nodejs/node/commit/0a85b0fd9d)] - **lib**: reduce overhead of `SafePromiseAllSettledReturnVoid` calls (Antoine du Hamel) [#51243](https://github.com/nodejs/node/pull/51243)
* \[[`f4d7f0498e`](https://github.com/nodejs/node/commit/f4d7f0498e)] - **lib**: expose default prepareStackTrace (Chengzhong Wu) [#50827](https://github.com/nodejs/node/pull/50827)
* \[[`5c7a9c8d4a`](https://github.com/nodejs/node/commit/5c7a9c8d4a)] - **lib**: don't parse windows drive letters as schemes (华) [#50580](https://github.com/nodejs/node/pull/50580)
* \[[`9da6384f5a`](https://github.com/nodejs/node/commit/9da6384f5a)] - **lib**: refactor to use validateFunction in diagnostics\_channel (Deokjin Kim) [#50955](https://github.com/nodejs/node/pull/50955)
* \[[`be3205ae24`](https://github.com/nodejs/node/commit/be3205ae24)] - **lib**: streamline process.binding() handling (Joyee Cheung) [#50773](https://github.com/nodejs/node/pull/50773)
* \[[`f4987eb91e`](https://github.com/nodejs/node/commit/f4987eb91e)] - **lib,permission**: handle buffer on fs.symlink (Rafael Gonzaga) [#51212](https://github.com/nodejs/node/pull/51212)
* \[[`861e040b40`](https://github.com/nodejs/node/commit/861e040b40)] - **lib,src**: extract sourceMappingURL from module (unbyte) [#51690](https://github.com/nodejs/node/pull/51690)
* \[[`8a082754e0`](https://github.com/nodejs/node/commit/8a082754e0)] - **lib,src**: replace toUSVString with `toWellFormed()` (Yagiz Nizipli) [#47342](https://github.com/nodejs/node/pull/47342)
* \[[`3badc1139c`](https://github.com/nodejs/node/commit/3badc1139c)] - **(SEMVER-MINOR)** **lib,src,permission**: port path.resolve to C++ (Rafael Gonzaga) [#50758](https://github.com/nodejs/node/pull/50758)
* \[[`4b3cc3ce18`](https://github.com/nodejs/node/commit/4b3cc3ce18)] - **loader**: speed up line length calc used by moduleProvider (Mudit) [#50969](https://github.com/nodejs/node/pull/50969)
* \[[`960d67c51f`](https://github.com/nodejs/node/commit/960d67c51f)] - **meta**: bump github/codeql-action from 3.23.2 to 3.24.6 (dependabot\[bot]) [#51942](https://github.com/nodejs/node/pull/51942)
* \[[`1783b93af2`](https://github.com/nodejs/node/commit/1783b93af2)] - **meta**: bump actions/upload-artifact from 4.3.0 to 4.3.1 (dependabot\[bot]) [#51941](https://github.com/nodejs/node/pull/51941)
* \[[`1db603db2f`](https://github.com/nodejs/node/commit/1db603db2f)] - **meta**: bump codecov/codecov-action from 4.0.1 to 4.1.0 (dependabot\[bot]) [#51940](https://github.com/nodejs/node/pull/51940)
* \[[`2ddec64d5a`](https://github.com/nodejs/node/commit/2ddec64d5a)] - **meta**: bump actions/cache from 4.0.0 to 4.0.1 (dependabot\[bot]) [#51939](https://github.com/nodejs/node/pull/51939)
* \[[`92490421be`](https://github.com/nodejs/node/commit/92490421be)] - **meta**: bump actions/download-artifact from 4.1.1 to 4.1.3 (dependabot\[bot]) [#51938](https://github.com/nodejs/node/pull/51938)
* \[[`f3fa2b72b8`](https://github.com/nodejs/node/commit/f3fa2b72b8)] - **meta**: bump actions/setup-node from 4.0.1 to 4.0.2 (dependabot\[bot]) [#51937](https://github.com/nodejs/node/pull/51937)
* \[[`a62b042e83`](https://github.com/nodejs/node/commit/a62b042e83)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#51726](https://github.com/nodejs/node/pull/51726)
* \[[`491f9f9902`](https://github.com/nodejs/node/commit/491f9f9902)] - **meta**: bump codecov/codecov-action from 3.1.4 to 4.0.1 (dependabot\[bot]) [#51648](https://github.com/nodejs/node/pull/51648)
* \[[`2765077a47`](https://github.com/nodejs/node/commit/2765077a47)] - **meta**: bump actions/download-artifact from 4.1.0 to 4.1.1 (dependabot\[bot]) [#51644](https://github.com/nodejs/node/pull/51644)
* \[[`152a07b854`](https://github.com/nodejs/node/commit/152a07b854)] - **meta**: bump actions/upload-artifact from 4.0.0 to 4.3.0 (dependabot\[bot]) [#51643](https://github.com/nodejs/node/pull/51643)
* \[[`53826920fb`](https://github.com/nodejs/node/commit/53826920fb)] - **meta**: bump step-security/harden-runner from 2.6.1 to 2.7.0 (dependabot\[bot]) [#51641](https://github.com/nodejs/node/pull/51641)
* \[[`3d1dc9b030`](https://github.com/nodejs/node/commit/3d1dc9b030)] - **meta**: bump actions/cache from 3.3.2 to 4.0.0 (dependabot\[bot]) [#51640](https://github.com/nodejs/node/pull/51640)
* \[[`287bdf6bda`](https://github.com/nodejs/node/commit/287bdf6bda)] - **meta**: bump github/codeql-action from 3.22.12 to 3.23.2 (dependabot\[bot]) [#51639](https://github.com/nodejs/node/pull/51639)
* \[[`90068fb0f1`](https://github.com/nodejs/node/commit/90068fb0f1)] - **meta**: add .mailmap entry for lemire (Daniel Lemire) [#51600](https://github.com/nodejs/node/pull/51600)
* \[[`f91786bd70`](https://github.com/nodejs/node/commit/f91786bd70)] - **meta**: mark security-wg codeowner for deps folder (Marco Ippolito) [#51529](https://github.com/nodejs/node/pull/51529)
* \[[`e51221be8d`](https://github.com/nodejs/node/commit/e51221be8d)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#51468](https://github.com/nodejs/node/pull/51468)
* \[[`4a8a012c6d`](https://github.com/nodejs/node/commit/4a8a012c6d)] - **meta**: move RaisinTen to emeritus and remove from strategic initiatives (Darshan Sen) [#51411](https://github.com/nodejs/node/pull/51411)
* \[[`e9276bab3f`](https://github.com/nodejs/node/commit/e9276bab3f)] - **meta**: add .temp and .lock tags to ignore (Rafael Gonzaga) [#51343](https://github.com/nodejs/node/pull/51343)
* \[[`ae6fecbc8d`](https://github.com/nodejs/node/commit/ae6fecbc8d)] - **meta**: bump actions/setup-python from 4.7.1 to 5.0.0 (dependabot\[bot]) [#51335](https://github.com/nodejs/node/pull/51335)
* \[[`f4be49a618`](https://github.com/nodejs/node/commit/f4be49a618)] - **meta**: bump actions/setup-node from 4.0.0 to 4.0.1 (dependabot\[bot]) [#51334](https://github.com/nodejs/node/pull/51334)
* \[[`e24aa7ced1`](https://github.com/nodejs/node/commit/e24aa7ced1)] - **meta**: bump github/codeql-action from 2.22.8 to 3.22.12 (dependabot\[bot]) [#51333](https://github.com/nodejs/node/pull/51333)
* \[[`287c2bcf56`](https://github.com/nodejs/node/commit/287c2bcf56)] - **meta**: bump actions/stale from 8.0.0 to 9.0.0 (dependabot\[bot]) [#51332](https://github.com/nodejs/node/pull/51332)
* \[[`1cad0dfaff`](https://github.com/nodejs/node/commit/1cad0dfaff)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#51329](https://github.com/nodejs/node/pull/51329)
* \[[`eef64b782e`](https://github.com/nodejs/node/commit/eef64b782e)] - **meta**: notify tsc on changes in SECURITY.md (Rafael Gonzaga) [#51259](https://github.com/nodejs/node/pull/51259)
* \[[`95a880f728`](https://github.com/nodejs/node/commit/95a880f728)] - **meta**: update artifact actions to v4 (Michaël Zasso) [#51219](https://github.com/nodejs/node/pull/51219)
* \[[`59805f6879`](https://github.com/nodejs/node/commit/59805f6879)] - **meta**: bump step-security/harden-runner from 2.6.0 to 2.6.1 (dependabot\[bot]) [#50999](https://github.com/nodejs/node/pull/50999)
* \[[`d74e0b97c3`](https://github.com/nodejs/node/commit/d74e0b97c3)] - **meta**: bump github/codeql-action from 2.22.5 to 2.22.8 (dependabot\[bot]) [#50998](https://github.com/nodejs/node/pull/50998)
* \[[`91cd9183d1`](https://github.com/nodejs/node/commit/91cd9183d1)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#50931](https://github.com/nodejs/node/pull/50931)
* \[[`c621491aba`](https://github.com/nodejs/node/commit/c621491aba)] - **module**: fix crash when built-in module export a `default` key (Antoine du Hamel) [#51481](https://github.com/nodejs/node/pull/51481)
* \[[`43a8d3e984`](https://github.com/nodejs/node/commit/43a8d3e984)] - **module**: fix `--preserve-symlinks-main` (per4uk) [#51312](https://github.com/nodejs/node/pull/51312)
* \[[`d8da197f86`](https://github.com/nodejs/node/commit/d8da197f86)] - **module**: move the CJS exports cache to internal/modules/cjs/loader (Joyee Cheung) [#51157](https://github.com/nodejs/node/pull/51157)
* \[[`5fc10ca4d6`](https://github.com/nodejs/node/commit/5fc10ca4d6)] - **module**: load source maps in `commonjs` translator (Hiroki Osame) [#51033](https://github.com/nodejs/node/pull/51033)
* \[[`43e9f0bc65`](https://github.com/nodejs/node/commit/43e9f0bc65)] - **module**: document `parentURL` in register options (Hiroki Osame) [#51039](https://github.com/nodejs/node/pull/51039)
* \[[`870ef5a73f`](https://github.com/nodejs/node/commit/870ef5a73f)] - **net**: fix connect crash when call destroy in lookup handler (theanarkh) [#51826](https://github.com/nodejs/node/pull/51826)
* \[[`caf71e05a6`](https://github.com/nodejs/node/commit/caf71e05a6)] - **net**: fix example IPv4 in dns docs (Aras Abbasi) [#51377](https://github.com/nodejs/node/pull/51377)
* \[[`58a636be0e`](https://github.com/nodejs/node/commit/58a636be0e)] - **(SEMVER-MINOR)** **net**: add connection attempt events (Paolo Insogna) [#51045](https://github.com/nodejs/node/pull/51045)
* \[[`06a29f830a`](https://github.com/nodejs/node/commit/06a29f830a)] - **node-api**: make napi\_get\_buffer\_info check if passed buffer is valid (Janrupf) [#51571](https://github.com/nodejs/node/pull/51571)
* \[[`0fb98438e4`](https://github.com/nodejs/node/commit/0fb98438e4)] - **node-api**: move NAPI\_EXPERIMENTAL definition to gyp file (Gabriel Schulhof) [#51254](https://github.com/nodejs/node/pull/51254)
* \[[`242139fb98`](https://github.com/nodejs/node/commit/242139fb98)] - **node-api**: optimize napi\_set\_property for perf (Mert Can Altın) [#50282](https://github.com/nodejs/node/pull/50282)
* \[[`dc3d70c040`](https://github.com/nodejs/node/commit/dc3d70c040)] - **node-api**: type tag external values without v8::Private (Chengzhong Wu) [#51149](https://github.com/nodejs/node/pull/51149)
* \[[`0ac070ccb7`](https://github.com/nodejs/node/commit/0ac070ccb7)] - **node-api**: segregate nogc APIs from rest via type system (Gabriel Schulhof) [#50060](https://github.com/nodejs/node/pull/50060)
* \[[`de65cada70`](https://github.com/nodejs/node/commit/de65cada70)] - **node-api**: introduce experimental feature flags (Gabriel Schulhof) [#50991](https://github.com/nodejs/node/pull/50991)
* \[[`e192ba18cd`](https://github.com/nodejs/node/commit/e192ba18cd)] - **perf\_hooks**: performance milestone time origin timestamp improvement (IlyasShabi) [#51713](https://github.com/nodejs/node/pull/51713)
* \[[`f94336f95a`](https://github.com/nodejs/node/commit/f94336f95a)] - **repl**: fix `NO_COLORS` env var is ignored (Moshe Atlow) [#51568](https://github.com/nodejs/node/pull/51568)
* \[[`e08649caa0`](https://github.com/nodejs/node/commit/e08649caa0)] - **repl**: fix prepareStackTrace frames array order (Chengzhong Wu) [#50827](https://github.com/nodejs/node/pull/50827)
* \[[`07614072f1`](https://github.com/nodejs/node/commit/07614072f1)] - **sea**: update stability index (Joyee Cheung) [#51774](https://github.com/nodejs/node/pull/51774)
* \[[`eea0d74454`](https://github.com/nodejs/node/commit/eea0d74454)] - **(SEMVER-MINOR)** **sea**: support sea.getRawAsset() (Joyee Cheung) [#50960](https://github.com/nodejs/node/pull/50960)
* \[[`db0efa3f40`](https://github.com/nodejs/node/commit/db0efa3f40)] - **(SEMVER-MINOR)** **sea**: support embedding assets (Joyee Cheung) [#50960](https://github.com/nodejs/node/pull/50960)
* \[[`9b164c6eec`](https://github.com/nodejs/node/commit/9b164c6eec)] - **src**: fix --disable-single-executable-application (Joyee Cheung) [#51808](https://github.com/nodejs/node/pull/51808)
* \[[`306c1d35e5`](https://github.com/nodejs/node/commit/306c1d35e5)] - **src**: simplify direct queries of env vars in C++ land (Joyee Cheung) [#51829](https://github.com/nodejs/node/pull/51829)
* \[[`696063a47c`](https://github.com/nodejs/node/commit/696063a47c)] - **src**: stop the profiler and the inspector before snapshot serialization (Joyee Cheung) [#51815](https://github.com/nodejs/node/pull/51815)
* \[[`be40c8286c`](https://github.com/nodejs/node/commit/be40c8286c)] - **src**: simplify embedder entry point execution (Joyee Cheung) [#51557](https://github.com/nodejs/node/pull/51557)
* \[[`90391ff256`](https://github.com/nodejs/node/commit/90391ff256)] - **src**: compile code eagerly in snapshot builder (Joyee Cheung) [#51672](https://github.com/nodejs/node/pull/51672)
* \[[`3875fa1dc5`](https://github.com/nodejs/node/commit/3875fa1dc5)] - **src**: check empty before accessing string (Cheng Zhao) [#51665](https://github.com/nodejs/node/pull/51665)
* \[[`a58c98ea85`](https://github.com/nodejs/node/commit/a58c98ea85)] - **(SEMVER-MINOR)** **src**: print string content better in BlobDeserializer (Joyee Cheung) [#50960](https://github.com/nodejs/node/pull/50960)
* \[[`62707a9d27`](https://github.com/nodejs/node/commit/62707a9d27)] - **src**: fix vm bug for configurable globalThis (F. Hinkelmann) [#51602](https://github.com/nodejs/node/pull/51602)
* \[[`c3c0a3ee5c`](https://github.com/nodejs/node/commit/c3c0a3ee5c)] - **(SEMVER-MINOR)** **src**: support multi-line values for .env file (IlyasShabi) [#51289](https://github.com/nodejs/node/pull/51289)
* \[[`dc8fe9ebf4`](https://github.com/nodejs/node/commit/dc8fe9ebf4)] - **(SEMVER-MINOR)** **src**: add `process.loadEnvFile` and `util.parseEnv` (Yagiz Nizipli) [#51476](https://github.com/nodejs/node/pull/51476)
* \[[`a5afad2a4d`](https://github.com/nodejs/node/commit/a5afad2a4d)] - **src**: terminate correctly double-quote in env variable (Marco Ippolito) [#51510](https://github.com/nodejs/node/pull/51510)
* \[[`2a921966c6`](https://github.com/nodejs/node/commit/2a921966c6)] - **(SEMVER-MINOR)** **src**: do not coerce dotenv paths (Tobias Nießen) [#51425](https://github.com/nodejs/node/pull/51425)
* \[[`50ec55c268`](https://github.com/nodejs/node/commit/50ec55c268)] - **src**: refactor `GetCreationContext` calls (Jungku Lee) [#51367](https://github.com/nodejs/node/pull/51367)
* \[[`2e65389922`](https://github.com/nodejs/node/commit/2e65389922)] - **src**: do not read string out of bounds (Cheng Zhao) [#51358](https://github.com/nodejs/node/pull/51358)
* \[[`a653531089`](https://github.com/nodejs/node/commit/a653531089)] - **src**: avoid shadowed string in fs\_permission (Shelley Vohr) [#51123](https://github.com/nodejs/node/pull/51123)
* \[[`c190a057ff`](https://github.com/nodejs/node/commit/c190a057ff)] - **src**: avoid draining platform tasks at FreeEnvironment (Chengzhong Wu) [#51290](https://github.com/nodejs/node/pull/51290)
* \[[`00227674f5`](https://github.com/nodejs/node/commit/00227674f5)] - **src**: add fast api for Histogram (James M Snell) [#51296](https://github.com/nodejs/node/pull/51296)
* \[[`4733c8e4df`](https://github.com/nodejs/node/commit/4733c8e4df)] - **src**: refactor `GetCreationContext` calls (Yagiz Nizipli) [#51287](https://github.com/nodejs/node/pull/51287)
* \[[`d76e16bb47`](https://github.com/nodejs/node/commit/d76e16bb47)] - **src**: enter isolate before destructing IsolateData (Ben Noordhuis) [#51138](https://github.com/nodejs/node/pull/51138)
* \[[`4ffdd37d2c`](https://github.com/nodejs/node/commit/4ffdd37d2c)] - **src**: eliminate duplicate code in histogram.cc (James M Snell) [#51263](https://github.com/nodejs/node/pull/51263)
* \[[`2ce8b974a0`](https://github.com/nodejs/node/commit/2ce8b974a0)] - **src**: fix unix abstract socket path for trace event (theanarkh) [#50858](https://github.com/nodejs/node/pull/50858)
* \[[`9b25268cb8`](https://github.com/nodejs/node/commit/9b25268cb8)] - **src**: use BignumPointer and use BN\_clear\_free (James M Snell) [#50454](https://github.com/nodejs/node/pull/50454)
* \[[`a80f660343`](https://github.com/nodejs/node/commit/a80f660343)] - **src**: implement FastByteLengthUtf8 with simdutf::utf8\_length\_from\_latin1 (Daniel Lemire) [#50840](https://github.com/nodejs/node/pull/50840)
* \[[`0dee86f295`](https://github.com/nodejs/node/commit/0dee86f295)] - **(SEMVER-MINOR)** **src**: support configurable snapshot (Joyee Cheung) [#50453](https://github.com/nodejs/node/pull/50453)
* \[[`90b5ed1d1d`](https://github.com/nodejs/node/commit/90b5ed1d1d)] - **src**: implement countObjectsWithPrototype (Joyee Cheung) [#50572](https://github.com/nodejs/node/pull/50572)
* \[[`9365e129ed`](https://github.com/nodejs/node/commit/9365e129ed)] - **src**: register udp\_wrap external references (Joyee Cheung) [#50943](https://github.com/nodejs/node/pull/50943)
* \[[`b05d496b6c`](https://github.com/nodejs/node/commit/b05d496b6c)] - **src**: register spawn\_sync external references (Joyee Cheung) [#50943](https://github.com/nodejs/node/pull/50943)
* \[[`642fb44982`](https://github.com/nodejs/node/commit/642fb44982)] - **src**: register process\_wrap external references (Joyee Cheung) [#50943](https://github.com/nodejs/node/pull/50943)
* \[[`c7c9e81a1a`](https://github.com/nodejs/node/commit/c7c9e81a1a)] - **src**: fix double free reported by coverity (Michael Dawson) [#51046](https://github.com/nodejs/node/pull/51046)
* \[[`358793e28e`](https://github.com/nodejs/node/commit/358793e28e)] - **src**: remove unused headers in `node_file.cc` (Jungku Lee) [#50927](https://github.com/nodejs/node/pull/50927)
* \[[`c705b73a74`](https://github.com/nodejs/node/commit/c705b73a74)] - **src**: implement --trace-promises (Joyee Cheung) [#50899](https://github.com/nodejs/node/pull/50899)
* \[[`97aa67f006`](https://github.com/nodejs/node/commit/97aa67f006)] - **src**: fix dynamically linked zlib version (Richard Lau) [#51007](https://github.com/nodejs/node/pull/51007)
* \[[`d6f46a44f2`](https://github.com/nodejs/node/commit/d6f46a44f2)] - **src**: make ModifyCodeGenerationFromStrings more robust (Joyee Cheung) [#50763](https://github.com/nodejs/node/pull/50763)
* \[[`362135a1f9`](https://github.com/nodejs/node/commit/362135a1f9)] - **src**: disable uncaught exception abortion for ESM syntax detection (Yagiz Nizipli) [#50987](https://github.com/nodejs/node/pull/50987)
* \[[`d82b0d4320`](https://github.com/nodejs/node/commit/d82b0d4320)] - **src**: fix backtrace with tail \[\[noreturn]] abort (Chengzhong Wu) [#50849](https://github.com/nodejs/node/pull/50849)
* \[[`6df3e31bff`](https://github.com/nodejs/node/commit/6df3e31bff)] - **src**: print MKSNAPSHOT debug logs to stderr (Joyee Cheung) [#50759](https://github.com/nodejs/node/pull/50759)
* \[[`fd5efac176`](https://github.com/nodejs/node/commit/fd5efac176)] - **(SEMVER-MINOR)** **src,permission**: add --allow-addon flag (Rafael Gonzaga) [#51183](https://github.com/nodejs/node/pull/51183)
* \[[`b616f6fa06`](https://github.com/nodejs/node/commit/b616f6fa06)] - **src,stream**: improve WriteString (ywave620) [#51155](https://github.com/nodejs/node/pull/51155)
* \[[`16d8cd5b22`](https://github.com/nodejs/node/commit/16d8cd5b22)] - **stream**: do not defer construction by one microtick (Matteo Collina) [#52005](https://github.com/nodejs/node/pull/52005)
* \[[`7931c3bbc8`](https://github.com/nodejs/node/commit/7931c3bbc8)] - **stream**: fix eventNames() to not return not defined events (IlyasShabi) [#51331](https://github.com/nodejs/node/pull/51331)
* \[[`d0a6f3515d`](https://github.com/nodejs/node/commit/d0a6f3515d)] - **stream**: fix cloned webstreams not being unref correctly (tsctx) [#51526](https://github.com/nodejs/node/pull/51526)
* \[[`8750070a47`](https://github.com/nodejs/node/commit/8750070a47)] - **stream**: fix fd is null when calling clearBuffer (kylo5aby) [#50994](https://github.com/nodejs/node/pull/50994)
* \[[`ade6614067`](https://github.com/nodejs/node/commit/ade6614067)] - **(SEMVER-MINOR)** **stream**: add support for `deflate-raw` format to webstreams compression (Damian Krzeminski) [#50097](https://github.com/nodejs/node/pull/50097)
* \[[`905c48fc6e`](https://github.com/nodejs/node/commit/905c48fc6e)] - **test**: add regression test for test\_runner after hook (Colin Ihrig) [#51998](https://github.com/nodejs/node/pull/51998)
* \[[`60f008b65e`](https://github.com/nodejs/node/commit/60f008b65e)] - **test**: reduce flakiness of `test-runner-output` (Antoine du Hamel) [#51952](https://github.com/nodejs/node/pull/51952)
* \[[`0ad88f6a5c`](https://github.com/nodejs/node/commit/0ad88f6a5c)] - **test**: fix flaky http-chunk-extensions-limit test (Ethan Arrowood) [#51943](https://github.com/nodejs/node/pull/51943)
* \[[`3f85c7ac97`](https://github.com/nodejs/node/commit/3f85c7ac97)] - **test**: remove flaky designation (Luigi Pinca) [#51736](https://github.com/nodejs/node/pull/51736)
* \[[`f37648ee5c`](https://github.com/nodejs/node/commit/f37648ee5c)] - **test**: skip SEA tests when SEA generation fails (Joyee Cheung) [#51887](https://github.com/nodejs/node/pull/51887)
* \[[`136b6a998b`](https://github.com/nodejs/node/commit/136b6a998b)] - **test**: fix unreliable assumption in js-native-api/test\_cannot\_run\_js (Joyee Cheung) [#51898](https://github.com/nodejs/node/pull/51898)
* \[[`d90594aefa`](https://github.com/nodejs/node/commit/d90594aefa)] - **test**: deflake test-http2-large-write-multiple-requests (Joyee Cheung) [#51863](https://github.com/nodejs/node/pull/51863)
* \[[`a0b36e33d1`](https://github.com/nodejs/node/commit/a0b36e33d1)] - **test**: fix test-debugger-profile for coverage generation (Joyee Cheung) [#51816](https://github.com/nodejs/node/pull/51816)
* \[[`dd0f164ca3`](https://github.com/nodejs/node/commit/dd0f164ca3)] - **test**: fix test-bootstrap-modules for coverage generation (Joyee Cheung) [#51816](https://github.com/nodejs/node/pull/51816)
* \[[`e4c7d62496`](https://github.com/nodejs/node/commit/e4c7d62496)] - **test**: ensure delay in recursive fs watch tests (Joyee Cheung) [#51842](https://github.com/nodejs/node/pull/51842)
* \[[`963d7d7dea`](https://github.com/nodejs/node/commit/963d7d7dea)] - **test**: fix test-child-process-fork-net (Joyee Cheung) [#51841](https://github.com/nodejs/node/pull/51841)
* \[[`dd708d337e`](https://github.com/nodejs/node/commit/dd708d337e)] - **test**: split wasi tests (Joyee Cheung) [#51836](https://github.com/nodejs/node/pull/51836)
* \[[`853b48d905`](https://github.com/nodejs/node/commit/853b48d905)] - **test**: remove test-fs-stat-bigint flaky designation (Luigi Pinca) [#51735](https://github.com/nodejs/node/pull/51735)
* \[[`fdc7d751de`](https://github.com/nodejs/node/commit/fdc7d751de)] - **test**: skip test-http-correct-hostname on loong64 (Shi Pujin) [#51663](https://github.com/nodejs/node/pull/51663)
* \[[`c33f860d2b`](https://github.com/nodejs/node/commit/c33f860d2b)] - **test**: remove test-cli-node-options flaky designation (Luigi Pinca) [#51716](https://github.com/nodejs/node/pull/51716)
* \[[`f528e965f6`](https://github.com/nodejs/node/commit/f528e965f6)] - **test**: remove test-domain-error-types flaky designation (Luigi Pinca) [#51717](https://github.com/nodejs/node/pull/51717)
* \[[`7e3ee828f1`](https://github.com/nodejs/node/commit/7e3ee828f1)] - **test**: fix `internet/test-inspector-help-page` (Richard Lau) [#51693](https://github.com/nodejs/node/pull/51693)
* \[[`170278c25d`](https://github.com/nodejs/node/commit/170278c25d)] - **test**: remove duplicate entry for flaky test (Luigi Pinca) [#51654](https://github.com/nodejs/node/pull/51654)
* \[[`d0d5bd0e54`](https://github.com/nodejs/node/commit/d0d5bd0e54)] - **test**: remove test-crypto-keygen flaky designation (Luigi Pinca) [#51567](https://github.com/nodejs/node/pull/51567)
* \[[`bca6dcca0b`](https://github.com/nodejs/node/commit/bca6dcca0b)] - **test**: remove test-fs-rmdir-recursive flaky designation (Luigi Pinca) [#51566](https://github.com/nodejs/node/pull/51566)
* \[[`af3f229d6b`](https://github.com/nodejs/node/commit/af3f229d6b)] - **test**: remove common.expectsError calls for asserts (Paulo Chaves) [#51504](https://github.com/nodejs/node/pull/51504)
* \[[`f6fcd200e6`](https://github.com/nodejs/node/commit/f6fcd200e6)] - **test**: mark test-http2-large-file as flaky (Michaël Zasso) [#51549](https://github.com/nodejs/node/pull/51549)
* \[[`1d8e65a230`](https://github.com/nodejs/node/commit/1d8e65a230)] - **test**: use checkIfCollectableByCounting in SourceTextModule leak test (Joyee Cheung) [#51512](https://github.com/nodejs/node/pull/51512)
* \[[`713afed6b0`](https://github.com/nodejs/node/commit/713afed6b0)] - **test**: remove test-file-write-stream4 flaky designation (Luigi Pinca) [#51472](https://github.com/nodejs/node/pull/51472)
* \[[`292d0174df`](https://github.com/nodejs/node/commit/292d0174df)] - **test**: add URL tests to fs-write (Rafael Gonzaga) [#51352](https://github.com/nodejs/node/pull/51352)
* \[[`954e2f2f58`](https://github.com/nodejs/node/commit/954e2f2f58)] - **test**: remove unneeded common.expectsError for asserts (Andrés Morelos) [#51353](https://github.com/nodejs/node/pull/51353)
* \[[`f2dfe0fa80`](https://github.com/nodejs/node/commit/f2dfe0fa80)] - **test**: add regression test for 51586 (Matteo Collina) [#51491](https://github.com/nodejs/node/pull/51491)
* \[[`6ee5f50789`](https://github.com/nodejs/node/commit/6ee5f50789)] - **test**: fix flaky conditions for ppc64 SEA tests (Richard Lau) [#51422](https://github.com/nodejs/node/pull/51422)
* \[[`06a6eef9a4`](https://github.com/nodejs/node/commit/06a6eef9a4)] - **test**: replace forEach() with for...of (Alexander Jones) [#50608](https://github.com/nodejs/node/pull/50608)
* \[[`a98102a6de`](https://github.com/nodejs/node/commit/a98102a6de)] - **test**: replace forEach with for...of (Ospite Privilegiato) [#50787](https://github.com/nodejs/node/pull/50787)
* \[[`e9080a94d3`](https://github.com/nodejs/node/commit/e9080a94d3)] - **test**: replace foreach with for of (lucacapocci94-dev) [#50790](https://github.com/nodejs/node/pull/50790)
* \[[`42b162b06d`](https://github.com/nodejs/node/commit/42b162b06d)] - **test**: replace forEach() with for...of (Jia) [#50610](https://github.com/nodejs/node/pull/50610)
* \[[`cab7737f7e`](https://github.com/nodejs/node/commit/cab7737f7e)] - **test**: fix inconsistency write size in `test-fs-readfile-tostring-fail` (Jungku Lee) [#51141](https://github.com/nodejs/node/pull/51141)
* \[[`15731b4b2f`](https://github.com/nodejs/node/commit/15731b4b2f)] - **test**: replace forEach test-http-server-multiheaders2 (Marco Mac) [#50794](https://github.com/nodejs/node/pull/50794)
* \[[`9cedaa62fa`](https://github.com/nodejs/node/commit/9cedaa62fa)] - **test**: replace forEach with for-of in test-webcrypto-export-import-ec (Chiara Ricciardi) [#51249](https://github.com/nodejs/node/pull/51249)
* \[[`7f301e04be`](https://github.com/nodejs/node/commit/7f301e04be)] - **test**: move to for of loop in test-http-hostname-typechecking.js (Luca Del Puppo) [#50782](https://github.com/nodejs/node/pull/50782)
* \[[`6e62e649df`](https://github.com/nodejs/node/commit/6e62e649df)] - **test**: skip test-watch-mode-inspect on arm (Michael Dawson) [#51210](https://github.com/nodejs/node/pull/51210)
* \[[`c3c2b2b041`](https://github.com/nodejs/node/commit/c3c2b2b041)] - **test**: replace forEach with for of in file test-trace-events-net.js (Ianna83) [#50789](https://github.com/nodejs/node/pull/50789)
* \[[`55c423ba4f`](https://github.com/nodejs/node/commit/55c423ba4f)] - **test**: replace forEach() with for...of in test/parallel/test-util-log.js (Edoardo Dusi) [#50783](https://github.com/nodejs/node/pull/50783)
* \[[`8ac05cf3c4`](https://github.com/nodejs/node/commit/8ac05cf3c4)] - **test**: replace forEach with for of in test-trace-events-api.js (Andrea Pavone) [#50784](https://github.com/nodejs/node/pull/50784)
* \[[`d10d39e8ba`](https://github.com/nodejs/node/commit/d10d39e8ba)] - **test**: replace forEach with for-of in test-v8-serders.js (Mattia Iannone) [#50791](https://github.com/nodejs/node/pull/50791)
* \[[`576adc5e5b`](https://github.com/nodejs/node/commit/576adc5e5b)] - **test**: add URL tests to fs-read in pm (Rafael Gonzaga) [#51213](https://github.com/nodejs/node/pull/51213)
* \[[`996cef51b7`](https://github.com/nodejs/node/commit/996cef51b7)] - **test**: use tmpdir.refresh() in test-esm-loader-resolve-type.mjs (Luigi Pinca) [#51206](https://github.com/nodejs/node/pull/51206)
* \[[`8f2d982342`](https://github.com/nodejs/node/commit/8f2d982342)] - **test**: use tmpdir.refresh() in test-esm-json.mjs (Luigi Pinca) [#51205](https://github.com/nodejs/node/pull/51205)
* \[[`efd6630143`](https://github.com/nodejs/node/commit/efd6630143)] - **test**: fix flakiness in worker\*.test-free-called (Jithil P Ponnan) [#51013](https://github.com/nodejs/node/pull/51013)
* \[[`54a29ee506`](https://github.com/nodejs/node/commit/54a29ee506)] - **test**: deflake test-diagnostics-channel-memory-leak (Joyee Cheung) [#50572](https://github.com/nodejs/node/pull/50572)
* \[[`6319ea6183`](https://github.com/nodejs/node/commit/6319ea6183)] - **test**: test syncrhnous methods of child\_process in snapshot (Joyee Cheung) [#50943](https://github.com/nodejs/node/pull/50943)
* \[[`50df4aee2b`](https://github.com/nodejs/node/commit/50df4aee2b)] - **test**: handle relative https redirect (Richard Lau) [#51121](https://github.com/nodejs/node/pull/51121)
* \[[`9f88f40cae`](https://github.com/nodejs/node/commit/9f88f40cae)] - **test**: fix test runner colored output test (Moshe Atlow) [#51064](https://github.com/nodejs/node/pull/51064)
* \[[`a1feae24cb`](https://github.com/nodejs/node/commit/a1feae24cb)] - **test**: resolve path of embedtest binary correctly (Cheng Zhao) [#50276](https://github.com/nodejs/node/pull/50276)
* \[[`a4f1805c92`](https://github.com/nodejs/node/commit/a4f1805c92)] - **test**: escape cwd in regexp (Jérémy Lal) [#50980](https://github.com/nodejs/node/pull/50980)
* \[[`1c28db8116`](https://github.com/nodejs/node/commit/1c28db8116)] - **test**: replace forEach to for.. test-webcrypto-export-import-cfrg.js (Angelo Parziale) [#50785](https://github.com/nodejs/node/pull/50785)
* \[[`a4f505213e`](https://github.com/nodejs/node/commit/a4f505213e)] - **test**: log more information in SEA tests (Joyee Cheung) [#50759](https://github.com/nodejs/node/pull/50759)
* \[[`c91b817a5c`](https://github.com/nodejs/node/commit/c91b817a5c)] - **test**: consolidate utf8 text fixtures in tests (Joyee Cheung) [#50732](https://github.com/nodejs/node/pull/50732)
* \[[`26a06b093b`](https://github.com/nodejs/node/commit/26a06b093b)] - **test**: give more time to GC in test-shadow-realm-gc-\* (Joyee Cheung) [#50735](https://github.com/nodejs/node/pull/50735)
* \[[`e8f5735149`](https://github.com/nodejs/node/commit/e8f5735149)] - **test**: test surrogate pair filenames on windows (Mert Can Altın) [#51800](https://github.com/nodejs/node/pull/51800)
* \[[`1ab9ff46a5`](https://github.com/nodejs/node/commit/1ab9ff46a5)] - **test**: mark test-wasi as flaky on Windows on ARM (Joyee Cheung) [#51834](https://github.com/nodejs/node/pull/51834)
* \[[`1c47da1453`](https://github.com/nodejs/node/commit/1c47da1453)] - **test,crypto**: update WebCryptoAPI WPT (Filip Skokan) [#51533](https://github.com/nodejs/node/pull/51533)
* \[[`91c8624608`](https://github.com/nodejs/node/commit/91c8624608)] - **test\_runner**: serialize 'expected' and 'actual' in isolation (Malthe Borch) [#51851](https://github.com/nodejs/node/pull/51851)
* \[[`cea90dcfe3`](https://github.com/nodejs/node/commit/cea90dcfe3)] - **test\_runner**: add ref methods to mocked timers (Marco Ippolito) [#51809](https://github.com/nodejs/node/pull/51809)
* \[[`9ff0df1793`](https://github.com/nodejs/node/commit/9ff0df1793)] - **test\_runner**: check if timeout was cleared by own callback (Ben Richeson) [#51673](https://github.com/nodejs/node/pull/51673)
* \[[`34ecd1e36b`](https://github.com/nodejs/node/commit/34ecd1e36b)] - **test\_runner**: fixed test object is incorrectly passed to setup() (Pulkit Gupta) [#50982](https://github.com/nodejs/node/pull/50982)
* \[[`da17a2538e`](https://github.com/nodejs/node/commit/da17a2538e)] - **test\_runner**: fixed to run after hook if before throws an error (Pulkit Gupta) [#51062](https://github.com/nodejs/node/pull/51062)
* \[[`b8f0ea6f60`](https://github.com/nodejs/node/commit/b8f0ea6f60)] - **test\_runner**: fix infinite loop when files are undefined in test runner (Pulkit Gupta) [#51047](https://github.com/nodejs/node/pull/51047)
* \[[`fe922f05e4`](https://github.com/nodejs/node/commit/fe922f05e4)] - **(SEMVER-MINOR)** **timers**: export timers.promises (Marco Ippolito) [#51246](https://github.com/nodejs/node/pull/51246)
* \[[`f4ac7baf85`](https://github.com/nodejs/node/commit/f4ac7baf85)] - **tools**: fix installing node with shared mode (Cheng Zhao) [#51910](https://github.com/nodejs/node/pull/51910)
* \[[`f07605fa7b`](https://github.com/nodejs/node/commit/f07605fa7b)] - **tools**: update eslint to 8.57.0 (Node.js GitHub Bot) [#51867](https://github.com/nodejs/node/pull/51867)
* \[[`d16b235fca`](https://github.com/nodejs/node/commit/d16b235fca)] - **tools**: update lint-md-dependencies to rollup\@4.12.0 (Node.js GitHub Bot) [#51795](https://github.com/nodejs/node/pull/51795)
* \[[`d27e811a01`](https://github.com/nodejs/node/commit/d27e811a01)] - **tools**: fix missing \[\[fallthrough]] in js2c (Cheng Zhao) [#51845](https://github.com/nodejs/node/pull/51845)
* \[[`7eb69308da`](https://github.com/nodejs/node/commit/7eb69308da)] - **tools**: disable automated libuv updates (Rafael Gonzaga) [#51775](https://github.com/nodejs/node/pull/51775)
* \[[`1f15af425c`](https://github.com/nodejs/node/commit/1f15af425c)] - **tools**: update lint-md-dependencies to rollup\@4.10.0 (Node.js GitHub Bot) [#51720](https://github.com/nodejs/node/pull/51720)
* \[[`c7ae13e6bc`](https://github.com/nodejs/node/commit/c7ae13e6bc)] - **tools**: update github\_reporter to 1.6.0 (Node.js GitHub Bot) [#51658](https://github.com/nodejs/node/pull/51658)
* \[[`0fb079bd85`](https://github.com/nodejs/node/commit/0fb079bd85)] - **tools**: run `build-windows` workflow only on source changes (Antoine du Hamel) [#51596](https://github.com/nodejs/node/pull/51596)
* \[[`c2538e31fa`](https://github.com/nodejs/node/commit/c2538e31fa)] - **tools**: update lint-md-dependencies to rollup\@4.9.6 (Node.js GitHub Bot) [#51583](https://github.com/nodejs/node/pull/51583)
* \[[`e02dbf074b`](https://github.com/nodejs/node/commit/e02dbf074b)] - **tools**: fix loong64 build (Shi Pujin) [#51401](https://github.com/nodejs/node/pull/51401)
* \[[`ce49cb6656`](https://github.com/nodejs/node/commit/ce49cb6656)] - **tools**: set normalizeTD text default to empty string (Marco Ippolito) [#51543](https://github.com/nodejs/node/pull/51543)
* \[[`e8dc5ac552`](https://github.com/nodejs/node/commit/e8dc5ac552)] - **tools**: limit parallelism with ninja in V8 builds (Richard Lau) [#51473](https://github.com/nodejs/node/pull/51473)
* \[[`97470b179b`](https://github.com/nodejs/node/commit/97470b179b)] - **tools**: do not pass invalid flag to C compiler (Michaël Zasso) [#51409](https://github.com/nodejs/node/pull/51409)
* \[[`59af1d7923`](https://github.com/nodejs/node/commit/59af1d7923)] - **tools**: update lint-md-dependencies to rollup\@4.9.5 (Node.js GitHub Bot) [#51460](https://github.com/nodejs/node/pull/51460)
* \[[`6385c7ad57`](https://github.com/nodejs/node/commit/6385c7ad57)] - **tools**: update inspector\_protocol to 83b1154 (Kohei Ueno) [#51309](https://github.com/nodejs/node/pull/51309)
* \[[`5235aaf299`](https://github.com/nodejs/node/commit/5235aaf299)] - **tools**: update github\_reporter to 1.5.4 (Node.js GitHub Bot) [#51395](https://github.com/nodejs/node/pull/51395)
* \[[`4ce2ecb1ce`](https://github.com/nodejs/node/commit/4ce2ecb1ce)] - **tools**: fix version parsing in brotli update script (Richard Lau) [#51373](https://github.com/nodejs/node/pull/51373)
* \[[`86102078f5`](https://github.com/nodejs/node/commit/86102078f5)] - **tools**: update lint-md-dependencies to rollup\@4.9.4 (Node.js GitHub Bot) [#51396](https://github.com/nodejs/node/pull/51396)
* \[[`e658208159`](https://github.com/nodejs/node/commit/e658208159)] - **tools**: remove openssl v1 update script (Marco Ippolito) [#51378](https://github.com/nodejs/node/pull/51378)
* \[[`4372f6a5b8`](https://github.com/nodejs/node/commit/4372f6a5b8)] - **tools**: remove deprecated python api (Alex Yang) [#49731](https://github.com/nodejs/node/pull/49731)
* \[[`2b24059e53`](https://github.com/nodejs/node/commit/2b24059e53)] - **tools**: update lint-md-dependencies to rollup\@4.9.2 (Node.js GitHub Bot) [#51320](https://github.com/nodejs/node/pull/51320)
* \[[`1da2e8d15e`](https://github.com/nodejs/node/commit/1da2e8d15e)] - **tools**: fix dep\_updaters dir updates (Michaël Zasso) [#51294](https://github.com/nodejs/node/pull/51294)
* \[[`b264dda7f2`](https://github.com/nodejs/node/commit/b264dda7f2)] - **tools**: update inspector\_protocol to c488ba2 (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`fdb07d5418`](https://github.com/nodejs/node/commit/fdb07d5418)] - **tools**: update inspector\_protocol to 9b4a4aa (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`6863fb84a6`](https://github.com/nodejs/node/commit/6863fb84a6)] - **tools**: update inspector\_protocol to 2f51e05 (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`6b85f5c6e0`](https://github.com/nodejs/node/commit/6b85f5c6e0)] - **tools**: update inspector\_protocol to d7b099b (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`cf029ca24f`](https://github.com/nodejs/node/commit/cf029ca24f)] - **tools**: update inspector\_protocol to 912eb68 (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`af119447f5`](https://github.com/nodejs/node/commit/af119447f5)] - **tools**: update inspector\_protocol to 547c5b8 (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`5a72506823`](https://github.com/nodejs/node/commit/5a72506823)] - **tools**: update inspector\_protocol to ca525fc (cola119) [#51293](https://github.com/nodejs/node/pull/51293)
* \[[`c7aa3976f9`](https://github.com/nodejs/node/commit/c7aa3976f9)] - **tools**: update lint-md-dependencies to rollup\@4.9.1 (Node.js GitHub Bot) [#51276](https://github.com/nodejs/node/pull/51276)
* \[[`8e02d08a82`](https://github.com/nodejs/node/commit/8e02d08a82)] - **tools**: check timezone current version (Marco Ippolito) [#51178](https://github.com/nodejs/node/pull/51178)
* \[[`fa1e88775d`](https://github.com/nodejs/node/commit/fa1e88775d)] - **tools**: update lint-md-dependencies to rollup\@4.9.0 (Node.js GitHub Bot) [#51193](https://github.com/nodejs/node/pull/51193)
* \[[`04c0bf9cc5`](https://github.com/nodejs/node/commit/04c0bf9cc5)] - **tools**: update eslint to 8.56.0 (Node.js GitHub Bot) [#51194](https://github.com/nodejs/node/pull/51194)
* \[[`e896cbd0d5`](https://github.com/nodejs/node/commit/e896cbd0d5)] - **tools**: update lint-md-dependencies to rollup\@4.7.0 (Node.js GitHub Bot) [#51106](https://github.com/nodejs/node/pull/51106)
* \[[`c7350c2083`](https://github.com/nodejs/node/commit/c7350c2083)] - **tools**: update doc to highlight.js\@11.9.0 unified\@11.0.4 (Node.js GitHub Bot) [#50459](https://github.com/nodejs/node/pull/50459)
* \[[`00dfabf8fb`](https://github.com/nodejs/node/commit/00dfabf8fb)] - **tools**: update eslint to 8.55.0 (Node.js GitHub Bot) [#51025](https://github.com/nodejs/node/pull/51025)
* \[[`f91d56157b`](https://github.com/nodejs/node/commit/f91d56157b)] - **tools**: update lint-md-dependencies to rollup\@4.6.1 (Node.js GitHub Bot) [#51022](https://github.com/nodejs/node/pull/51022)
* \[[`450163cf9b`](https://github.com/nodejs/node/commit/450163cf9b)] - **tools**: add triggers to update release links workflow (Moshe Atlow) [#50974](https://github.com/nodejs/node/pull/50974)
* \[[`b1442024ea`](https://github.com/nodejs/node/commit/b1442024ea)] - **tools**: update lint-md-dependencies to rollup\@4.5.2 (Node.js GitHub Bot) [#50913](https://github.com/nodejs/node/pull/50913)
* \[[`6fc6a62daf`](https://github.com/nodejs/node/commit/6fc6a62daf)] - **tools**: fix current version check (Marco Ippolito) [#50951](https://github.com/nodejs/node/pull/50951)
* \[[`bc6bdda8b1`](https://github.com/nodejs/node/commit/bc6bdda8b1)] - **tools**: fix update-icu.sh (Michaël Zasso) [#51723](https://github.com/nodejs/node/pull/51723)
* \[[`a7a4cce75d`](https://github.com/nodejs/node/commit/a7a4cce75d)] - **typings**: lib/internal/vm.js (Geoffrey Booth) [#50112](https://github.com/nodejs/node/pull/50112)
* \[[`6375540507`](https://github.com/nodejs/node/commit/6375540507)] - **typings**: fix JSDoc in `internal/modules/esm/hooks` (Alex Yang) [#50887](https://github.com/nodejs/node/pull/50887)
* \[[`4bc8e98d7c`](https://github.com/nodejs/node/commit/4bc8e98d7c)] - **url**: don't update URL immediately on update to URLSearchParams (Matt Cowley) [#51520](https://github.com/nodejs/node/pull/51520)
* \[[`2acbcbd8ad`](https://github.com/nodejs/node/commit/2acbcbd8ad)] - **url**: throw error if argument length of revokeObjectURL is 0 (DylanTet) [#50433](https://github.com/nodejs/node/pull/50433)
* \[[`c50134615e`](https://github.com/nodejs/node/commit/c50134615e)] - **(SEMVER-MINOR)** **util**: add styleText API to text formatting (Rafael Gonzaga) [#51850](https://github.com/nodejs/node/pull/51850)
* \[[`f79ac336ad`](https://github.com/nodejs/node/commit/f79ac336ad)] - **util**: pass invalidSubtypeIndex instead of trimmedSubtype to error (Gaurish Sethia) [#51264](https://github.com/nodejs/node/pull/51264)
* \[[`c3b89c310f`](https://github.com/nodejs/node/commit/c3b89c310f)] - **util**: improve performance of function areSimilarFloatArrays (Liu Jia) [#51040](https://github.com/nodejs/node/pull/51040)
* \[[`5202995b48`](https://github.com/nodejs/node/commit/5202995b48)] - **vm**: implement isContext() directly in JS land with private symbol (Joyee Cheung) [#51685](https://github.com/nodejs/node/pull/51685)
* \[[`0211a3d65f`](https://github.com/nodejs/node/commit/0211a3d65f)] - **(SEMVER-MINOR)** **vm**: support using the default loader to handle dynamic import() (Joyee Cheung) [#51244](https://github.com/nodejs/node/pull/51244)
* \[[`07fc077c5d`](https://github.com/nodejs/node/commit/07fc077c5d)] - **vm**: use v8::DeserializeInternalFieldsCallback explicitly (Joyee Cheung) [#50984](https://github.com/nodejs/node/pull/50984)
* \[[`5183e3a4b1`](https://github.com/nodejs/node/commit/5183e3a4b1)] - **watch**: clarify that the fileName parameter can be null (Luigi Pinca) [#51305](https://github.com/nodejs/node/pull/51305)
* \[[`63bf8a66df`](https://github.com/nodejs/node/commit/63bf8a66df)] - **watch**: fix null `fileName` on windows systems (vnc5) [#49891](https://github.com/nodejs/node/pull/49891)
* \[[`07da4e9b58`](https://github.com/nodejs/node/commit/07da4e9b58)] - **watch**: fix infinite loop when passing --watch=true flag (Pulkit Gupta) [#51160](https://github.com/nodejs/node/pull/51160)

<a id="20.11.1"></a>

## 2024-02-14, Version 20.11.1 'Iron' (LTS), @RafaelGSS prepared by @marco-ippolito

### Notable changes

This is a security release.

### Notable changes

* CVE-2024-21892 - Code injection and privilege escalation through Linux capabilities- (High)
* CVE-2024-22019 - http: Reading unprocessed HTTP request with unbounded chunk extension allows DoS attacks- (High)
* CVE-2024-21896 - Path traversal by monkey-patching Buffer internals- (High)
* CVE-2024-22017 - setuid() does not drop all privileges due to io\_uring - (High)
* CVE-2023-46809 - Node.js is vulnerable to the Marvin Attack (timing variant of the Bleichenbacher attack against PKCS#1 v1.5 padding) - (Medium)
* CVE-2024-21891 - Multiple permission model bypasses due to improper path traversal sequence sanitization - (Medium)
* CVE-2024-21890 - Improper handling of wildcards in --allow-fs-read and --allow-fs-write (Medium)
* CVE-2024-22025 - Denial of Service by resource exhaustion in fetch() brotli decoding - (Medium)
* undici version 5.28.3
* libuv version 1.48.0
* OpenSSL version 3.0.13+quic1

### Commits

* \[[`7079c062bb`](https://github.com/nodejs/node/commit/7079c062bb)] - **crypto**: disable PKCS#1 padding for privateDecrypt (Michael Dawson) [nodejs-private/node-private#525](https://github.com/nodejs-private/node-private/pull/525)
* \[[`186a6e1ffb`](https://github.com/nodejs/node/commit/186a6e1ffb)] - **deps**: fix GHSA-f74f-cvh7-c6q6/CVE-2024-24806 (Santiago Gimeno) [#51737](https://github.com/nodejs/node/pull/51737)
* \[[`686da19abb`](https://github.com/nodejs/node/commit/686da19abb)] - **deps**: disable io\_uring support in libuv by default (Tobias Nießen) [nodejs-private/node-private#529](https://github.com/nodejs-private/node-private/pull/529)
* \[[`f7b44bfbce`](https://github.com/nodejs/node/commit/f7b44bfbce)] - **deps**: update archs files for openssl-3.0.13+quic1 (Node.js GitHub Bot) [#51614](https://github.com/nodejs/node/pull/51614)
* \[[`7a30fecea2`](https://github.com/nodejs/node/commit/7a30fecea2)] - **deps**: upgrade openssl sources to quictls/openssl-3.0.13+quic1 (Node.js GitHub Bot) [#51614](https://github.com/nodejs/node/pull/51614)
* \[[`480fc169a8`](https://github.com/nodejs/node/commit/480fc169a8)] - **fs**: protect against modified Buffer internals in possiblyTransformPath (Tobias Nießen) [nodejs-private/node-private#497](https://github.com/nodejs-private/node-private/pull/497)
* \[[`77ac7c3153`](https://github.com/nodejs/node/commit/77ac7c3153)] - **http**: add maximum chunk extension size (Paolo Insogna) [nodejs-private/node-private#519](https://github.com/nodejs-private/node-private/pull/519)
* \[[`ed7d149675`](https://github.com/nodejs/node/commit/ed7d149675)] - **lib**: use cache fs internals against path traversal (RafaelGSS) [nodejs-private/node-private#516](https://github.com/nodejs-private/node-private/pull/516)
* \[[`89bd5fc38f`](https://github.com/nodejs/node/commit/89bd5fc38f)] - **lib**: update undici to v5.28.3 (Matteo Collina) [nodejs-private/node-private#539](https://github.com/nodejs-private/node-private/pull/539)
* \[[`d01dd4291d`](https://github.com/nodejs/node/commit/d01dd4291d)] - **permission**: fix wildcard when children > 1 (Rafael Gonzaga) [#51209](https://github.com/nodejs/node/pull/51209)
* \[[`40ff37dfcc`](https://github.com/nodejs/node/commit/40ff37dfcc)] - **src**: fix HasOnly(capability) in node::credentials (Tobias Nießen) [nodejs-private/node-private#505](https://github.com/nodejs-private/node-private/pull/505)
* \[[`3f6addd590`](https://github.com/nodejs/node/commit/3f6addd590)] - **src,deps**: disable setuid() etc if io\_uring enabled (Tobias Nießen) [nodejs-private/node-private#529](https://github.com/nodejs-private/node-private/pull/529)
* \[[`d6da413aa4`](https://github.com/nodejs/node/commit/d6da413aa4)] - **test,doc**: clarify wildcard usage (RafaelGSS) [nodejs-private/node-private#517](https://github.com/nodejs-private/node-private/pull/517)
* \[[`c213910aea`](https://github.com/nodejs/node/commit/c213910aea)] - **zlib**: pause stream if outgoing buffer is full (Matteo Collina) [nodejs-private/node-private#541](https://github.com/nodejs-private/node-private/pull/541)

<a id="20.11.0"></a>

## 2024-01-09, Version 20.11.0 'Iron' (LTS), @UlisesGascon

### Notable Changes

* \[[`833190fe7c`](https://github.com/nodejs/node/commit/833190fe7c)] - **crypto**: update root certificates to NSS 3.95 (Node.js GitHub Bot) [#50805](https://github.com/nodejs/node/pull/50805)
* \[[`a541b78bdb`](https://github.com/nodejs/node/commit/a541b78bdb)] - **doc**: add MrJithil to collaborators (Jithil P Ponnan) [#50666](https://github.com/nodejs/node/pull/50666)
* \[[`d4be8fad83`](https://github.com/nodejs/node/commit/d4be8fad83)] - **doc**: add Ethan-Arrowood as a collaborator (Ethan Arrowood) [#50393](https://github.com/nodejs/node/pull/50393)
* \[[`c1a196c897`](https://github.com/nodejs/node/commit/c1a196c897)] - **(SEMVER-MINOR)** **esm**: add import.meta.dirname and import.meta.filename (James Sumners) [#48740](https://github.com/nodejs/node/pull/48740)
* \[[`aa3209b880`](https://github.com/nodejs/node/commit/aa3209b880)] - **fs**: add c++ fast path for writeFileSync utf8 (CanadaHonk) [#49884](https://github.com/nodejs/node/pull/49884)
* \[[`8e886a2fff`](https://github.com/nodejs/node/commit/8e886a2fff)] - **(SEMVER-MINOR)** **module**: remove useCustomLoadersIfPresent flag (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`21ab3c0f0b`](https://github.com/nodejs/node/commit/21ab3c0f0b)] - **(SEMVER-MINOR)** **module**: bootstrap module loaders in shadow realm (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`29d91b13e3`](https://github.com/nodejs/node/commit/29d91b13e3)] - **(SEMVER-MINOR)** **src**: add `--disable-warning` option (Ethan Arrowood) [#50661](https://github.com/nodejs/node/pull/50661)
* \[[`11b3e470db`](https://github.com/nodejs/node/commit/11b3e470db)] - **(SEMVER-MINOR)** **src**: create per isolate proxy env template (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`621c4d66c2`](https://github.com/nodejs/node/commit/621c4d66c2)] - **(SEMVER-MINOR)** **src**: make process binding data weak (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`139d6c8d3b`](https://github.com/nodejs/node/commit/139d6c8d3b)] - **stream**: use Array for Readable buffer (Robert Nagy) [#50341](https://github.com/nodejs/node/pull/50341)
* \[[`6206957e8d`](https://github.com/nodejs/node/commit/6206957e8d)] - **stream**: optimize creation (Robert Nagy) [#50337](https://github.com/nodejs/node/pull/50337)
* \[[`e64378643d`](https://github.com/nodejs/node/commit/e64378643d)] - **(SEMVER-MINOR)** **test\_runner**: adds built in lcov reporter (Phil Nash) [#50018](https://github.com/nodejs/node/pull/50018)
* \[[`4a830c2d9d`](https://github.com/nodejs/node/commit/4a830c2d9d)] - **(SEMVER-MINOR)** **test\_runner**: add Date to the supported mock APIs (Lucas Santos) [#48638](https://github.com/nodejs/node/pull/48638)
* \[[`842dc01def`](https://github.com/nodejs/node/commit/842dc01def)] - **(SEMVER-MINOR)** **test\_runner, cli**: add --test-timeout flag (Shubham Pandey) [#50443](https://github.com/nodejs/node/pull/50443)

### Commits

* \[[`e40a559ab1`](https://github.com/nodejs/node/commit/e40a559ab1)] - **benchmark**: update iterations in benchmark/util/splice-one.js (Liu Jia) [#50698](https://github.com/nodejs/node/pull/50698)
* \[[`00f7a5d26f`](https://github.com/nodejs/node/commit/00f7a5d26f)] - **benchmark**: increase the iteration number to an appropriate value (Lei Shi) [#50766](https://github.com/nodejs/node/pull/50766)
* \[[`be6ad3f375`](https://github.com/nodejs/node/commit/be6ad3f375)] - **benchmark**: rewrite import.meta benchmark (Joyee Cheung) [#50683](https://github.com/nodejs/node/pull/50683)
* \[[`9857364129`](https://github.com/nodejs/node/commit/9857364129)] - **benchmark**: add misc/startup-cli-version benchmark (Joyee Cheung) [#50684](https://github.com/nodejs/node/pull/50684)
* \[[`22d729e7f5`](https://github.com/nodejs/node/commit/22d729e7f5)] - **benchmark**: remove punycode from require-builtins fixture (Joyee Cheung) [#50689](https://github.com/nodejs/node/pull/50689)
* \[[`4cf10a149a`](https://github.com/nodejs/node/commit/4cf10a149a)] - **benchmark**: change iterations in benchmark/es/string-concatenations.js (Liu Jia) [#50585](https://github.com/nodejs/node/pull/50585)
* \[[`15c2ed93a8`](https://github.com/nodejs/node/commit/15c2ed93a8)] - **benchmark**: add benchmarks for encodings (Aras Abbasi) [#50348](https://github.com/nodejs/node/pull/50348)
* \[[`8a896428ca`](https://github.com/nodejs/node/commit/8a896428ca)] - **benchmark**: add more cases to Readable.from (Raz Luvaton) [#50351](https://github.com/nodejs/node/pull/50351)
* \[[`dbe6c5f354`](https://github.com/nodejs/node/commit/dbe6c5f354)] - **benchmark**: skip test-benchmark-os on IBMi (Michael Dawson) [#50286](https://github.com/nodejs/node/pull/50286)
* \[[`179b4b6e62`](https://github.com/nodejs/node/commit/179b4b6e62)] - **benchmark**: move permission-fs-read to permission-processhas-fs-read (Aki Hasegawa-Johnson) [#49770](https://github.com/nodejs/node/pull/49770)
* \[[`32d65c001d`](https://github.com/nodejs/node/commit/32d65c001d)] - **buffer**: improve Buffer.equals performance (kylo5aby) [#50621](https://github.com/nodejs/node/pull/50621)
* \[[`80ea83757e`](https://github.com/nodejs/node/commit/80ea83757e)] - **build**: add GN configurations for simdjson (Cheng Zhao) [#50831](https://github.com/nodejs/node/pull/50831)
* \[[`904e645bcd`](https://github.com/nodejs/node/commit/904e645bcd)] - **build**: add configuration flag to enable Maglev (Keyhan Vakil) [#50692](https://github.com/nodejs/node/pull/50692)
* \[[`019efa8a5a`](https://github.com/nodejs/node/commit/019efa8a5a)] - **build**: fix GN configuration for deps/base64 (Cheng Zhao) [#50696](https://github.com/nodejs/node/pull/50696)
* \[[`a645d5ac54`](https://github.com/nodejs/node/commit/a645d5ac54)] - **build**: disable flag v8\_scriptormodule\_legacy\_lifetime (Chengzhong Wu) [#50616](https://github.com/nodejs/node/pull/50616)
* \[[`8705058b09`](https://github.com/nodejs/node/commit/8705058b09)] - **build**: add GN build files (Cheng Zhao) [#47637](https://github.com/nodejs/node/pull/47637)
* \[[`0a5e9c12cf`](https://github.com/nodejs/node/commit/0a5e9c12cf)] - **build**: fix build with Python 3.12 (Luigi Pinca) [#50582](https://github.com/nodejs/node/pull/50582)
* \[[`ff5713dd43`](https://github.com/nodejs/node/commit/ff5713dd43)] - **build**: support Python 3.12 (Shi Pujin) [#50209](https://github.com/nodejs/node/pull/50209)
* \[[`cfd50f229a`](https://github.com/nodejs/node/commit/cfd50f229a)] - **build**: fix building when there is only python3 (Cheng Zhao) [#48462](https://github.com/nodejs/node/pull/48462)
* \[[`833190fe7c`](https://github.com/nodejs/node/commit/833190fe7c)] - **crypto**: update root certificates to NSS 3.95 (Node.js GitHub Bot) [#50805](https://github.com/nodejs/node/pull/50805)
* \[[`54c46dae9e`](https://github.com/nodejs/node/commit/54c46dae9e)] - **deps**: update zlib to 1.2.13.1-motley-5daffc7 (Node.js GitHub Bot) [#50803](https://github.com/nodejs/node/pull/50803)
* \[[`0be84e5a28`](https://github.com/nodejs/node/commit/0be84e5a28)] - **deps**: update undici to 5.27.2 (Node.js GitHub Bot) [#50813](https://github.com/nodejs/node/pull/50813)
* \[[`ec67890824`](https://github.com/nodejs/node/commit/ec67890824)] - **deps**: V8: cherry-pick 0f9ebbc672c7 (Chengzhong Wu) [#50867](https://github.com/nodejs/node/pull/50867)
* \[[`bc2ebb972b`](https://github.com/nodejs/node/commit/bc2ebb972b)] - **deps**: V8: cherry-pick 13192d6e10fa (Levi Zim) [#50552](https://github.com/nodejs/node/pull/50552)
* \[[`656135d70a`](https://github.com/nodejs/node/commit/656135d70a)] - **deps**: update zlib to 1.2.13.1-motley-dfc48fc (Node.js GitHub Bot) [#50456](https://github.com/nodejs/node/pull/50456)
* \[[`41ee4bcc5d`](https://github.com/nodejs/node/commit/41ee4bcc5d)] - **deps**: update ada to 2.7.4 (Node.js GitHub Bot) [#50815](https://github.com/nodejs/node/pull/50815)
* \[[`a40948b5c5`](https://github.com/nodejs/node/commit/a40948b5c5)] - **deps**: update minimatch to 9.0.3 (Node.js GitHub Bot) [#50806](https://github.com/nodejs/node/pull/50806)
* \[[`7be1222c4a`](https://github.com/nodejs/node/commit/7be1222c4a)] - **deps**: update simdutf to 4.0.4 (Node.js GitHub Bot) [#50772](https://github.com/nodejs/node/pull/50772)
* \[[`68e7d49db6`](https://github.com/nodejs/node/commit/68e7d49db6)] - **deps**: upgrade npm to 10.2.4 (npm team) [#50751](https://github.com/nodejs/node/pull/50751)
* \[[`3d82d38336`](https://github.com/nodejs/node/commit/3d82d38336)] - **deps**: escape Python strings correctly (Michaël Zasso) [#50695](https://github.com/nodejs/node/pull/50695)
* \[[`d3870ac957`](https://github.com/nodejs/node/commit/d3870ac957)] - **deps**: update base64 to 0.5.1 (Node.js GitHub Bot) [#50629](https://github.com/nodejs/node/pull/50629)
* \[[`4b219b6ece`](https://github.com/nodejs/node/commit/4b219b6ece)] - **deps**: update corepack to 0.23.0 (Node.js GitHub Bot) [#50563](https://github.com/nodejs/node/pull/50563)
* \[[`6c41b50922`](https://github.com/nodejs/node/commit/6c41b50922)] - **deps**: update nghttp2 to 1.58.0 (Node.js GitHub Bot) [#50441](https://github.com/nodejs/node/pull/50441)
* \[[`3beee0ae8f`](https://github.com/nodejs/node/commit/3beee0ae8f)] - **deps**: update acorn to 8.11.2 (Node.js GitHub Bot) [#50460](https://github.com/nodejs/node/pull/50460)
* \[[`220916fa93`](https://github.com/nodejs/node/commit/220916fa93)] - **deps**: update undici to 5.27.0 (Node.js GitHub Bot) [#50463](https://github.com/nodejs/node/pull/50463)
* \[[`f9960b3545`](https://github.com/nodejs/node/commit/f9960b3545)] - **deps**: update googletest to 116b7e5 (Node.js GitHub Bot) [#50324](https://github.com/nodejs/node/pull/50324)
* \[[`d5c16f897a`](https://github.com/nodejs/node/commit/d5c16f897a)] - **dns**: call handle.setServers() with a valid array (Luigi Pinca) [#50811](https://github.com/nodejs/node/pull/50811)
* \[[`1bd6537c97`](https://github.com/nodejs/node/commit/1bd6537c97)] - **doc**: recommend supported Python versions (Luigi Pinca) [#50407](https://github.com/nodejs/node/pull/50407)
* \[[`402e257520`](https://github.com/nodejs/node/commit/402e257520)] - **doc**: update notable changes in v21.1.0 (Joyee Cheung) [#50388](https://github.com/nodejs/node/pull/50388)
* \[[`032535e270`](https://github.com/nodejs/node/commit/032535e270)] - **doc**: make theme consistent across api and other docs (Dima Demakov) [#50877](https://github.com/nodejs/node/pull/50877)
* \[[`d53842683f`](https://github.com/nodejs/node/commit/d53842683f)] - **doc**: add a section regarding `instanceof` in `primordials.md` (Antoine du Hamel) [#50874](https://github.com/nodejs/node/pull/50874)
* \[[`fe315055a7`](https://github.com/nodejs/node/commit/fe315055a7)] - **doc**: update email to reflect affiliation (Yagiz Nizipli) [#50856](https://github.com/nodejs/node/pull/50856)
* \[[`e14f661950`](https://github.com/nodejs/node/commit/e14f661950)] - **doc**: shard not supported with watch mode (Pulkit Gupta) [#50640](https://github.com/nodejs/node/pull/50640)
* \[[`b3d015de71`](https://github.com/nodejs/node/commit/b3d015de71)] - **doc**: get rid of unnecessary `eslint-skip` comments (Antoine du Hamel) [#50829](https://github.com/nodejs/node/pull/50829)
* \[[`168cbf9cb9`](https://github.com/nodejs/node/commit/168cbf9cb9)] - **doc**: create deprecation code for isWebAssemblyCompiledModule (Marco Ippolito) [#50486](https://github.com/nodejs/node/pull/50486)
* \[[`30baacba41`](https://github.com/nodejs/node/commit/30baacba41)] - **doc**: add CanadaHonk to triagers (CanadaHonk) [#50848](https://github.com/nodejs/node/pull/50848)
* \[[`e6e7cbceac`](https://github.com/nodejs/node/commit/e6e7cbceac)] - **doc**: fix typos in --allow-fs-\* (Tobias Nießen) [#50845](https://github.com/nodejs/node/pull/50845)
* \[[`e22ce9586f`](https://github.com/nodejs/node/commit/e22ce9586f)] - **doc**: update Crypto API doc for x509.keyUsage (Daniel Meechan) [#50603](https://github.com/nodejs/node/pull/50603)
* \[[`549d4422b7`](https://github.com/nodejs/node/commit/549d4422b7)] - **doc**: fix fs.writeFileSync return value documentation (Ryan Zimmerman) [#50760](https://github.com/nodejs/node/pull/50760)
* \[[`3c79e3cdba`](https://github.com/nodejs/node/commit/3c79e3cdba)] - **doc**: update print results(detail) in `PerformanceEntry` (Jungku Lee) [#50723](https://github.com/nodejs/node/pull/50723)
* \[[`aeaf96d06e`](https://github.com/nodejs/node/commit/aeaf96d06e)] - **doc**: fix `Buffer.allocUnsafe` documentation (Mert Can Altın) [#50686](https://github.com/nodejs/node/pull/50686)
* \[[`347e1dd06a`](https://github.com/nodejs/node/commit/347e1dd06a)] - **doc**: run license-builder (github-actions\[bot]) [#50691](https://github.com/nodejs/node/pull/50691)
* \[[`a541b78bdb`](https://github.com/nodejs/node/commit/a541b78bdb)] - **doc**: add MrJithil to collaborators (Jithil P Ponnan) [#50666](https://github.com/nodejs/node/pull/50666)
* \[[`90f415dd61`](https://github.com/nodejs/node/commit/90f415dd61)] - **doc**: fix typo in fs.md (fwio) [#50570](https://github.com/nodejs/node/pull/50570)
* \[[`e2388151ba`](https://github.com/nodejs/node/commit/e2388151ba)] - **doc**: add missing description of argument in `subtle.encrypt` (Deokjin Kim) [#50578](https://github.com/nodejs/node/pull/50578)
* \[[`39cc013465`](https://github.com/nodejs/node/commit/39cc013465)] - **doc**: update pm documentation to include resource (Ranieri Innocenti Spada) [#50601](https://github.com/nodejs/node/pull/50601)
* \[[`ba6d427c23`](https://github.com/nodejs/node/commit/ba6d427c23)] - **doc**: correct attribution in v20.6.0 changelog (Jacob Smith) [#50564](https://github.com/nodejs/node/pull/50564)
* \[[`1b2dab8254`](https://github.com/nodejs/node/commit/1b2dab8254)] - **doc**: update to align `console.table` row to the left (Jungku Lee) [#50553](https://github.com/nodejs/node/pull/50553)
* \[[`5d48ef7778`](https://github.com/nodejs/node/commit/5d48ef7778)] - **doc**: underline links (Rich Trott) [#50481](https://github.com/nodejs/node/pull/50481)
* \[[`5e6057c9d2`](https://github.com/nodejs/node/commit/5e6057c9d2)] - **doc**: remove duplicate word (Gerhard Stöbich) [#50475](https://github.com/nodejs/node/pull/50475)
* \[[`64bf2fd4ee`](https://github.com/nodejs/node/commit/64bf2fd4ee)] - **doc**: fix typo in `webstreams.md` (André Santos) [#50426](https://github.com/nodejs/node/pull/50426)
* \[[`cca55b8414`](https://github.com/nodejs/node/commit/cca55b8414)] - **doc**: add information about Node-API versions >=9 (Michael Dawson) [#50168](https://github.com/nodejs/node/pull/50168)
* \[[`d4be8fad83`](https://github.com/nodejs/node/commit/d4be8fad83)] - **doc**: add Ethan-Arrowood as a collaborator (Ethan Arrowood) [#50393](https://github.com/nodejs/node/pull/50393)
* \[[`0b311838f6`](https://github.com/nodejs/node/commit/0b311838f6)] - **doc**: fix TOC in `releases.md` (Bryce Seefieldt) [#50372](https://github.com/nodejs/node/pull/50372)
* \[[`843d5f84ca`](https://github.com/nodejs/node/commit/843d5f84ca)] - **esm**: fallback to `getSource` when `load` returns nullish `source` (Antoine du Hamel) [#50825](https://github.com/nodejs/node/pull/50825)
* \[[`8d5469c84b`](https://github.com/nodejs/node/commit/8d5469c84b)] - **esm**: do not call `getSource` when format is `commonjs` (Francesco Trotta) [#50465](https://github.com/nodejs/node/pull/50465)
* \[[`b48cf314d3`](https://github.com/nodejs/node/commit/b48cf314d3)] - **esm**: bypass CJS loader in default load under `--default-type=module` (Antoine du Hamel) [#50004](https://github.com/nodejs/node/pull/50004)
* \[[`c1a196c897`](https://github.com/nodejs/node/commit/c1a196c897)] - **(SEMVER-MINOR)** **esm**: add import.meta.dirname and import.meta.filename (James Sumners) [#48740](https://github.com/nodejs/node/pull/48740)
* \[[`435f9c9276`](https://github.com/nodejs/node/commit/435f9c9276)] - **fs**: use default w flag for writeFileSync with utf8 encoding (Murilo Kakazu) [#50990](https://github.com/nodejs/node/pull/50990)
* \[[`aa3209b880`](https://github.com/nodejs/node/commit/aa3209b880)] - **fs**: add c++ fast path for writeFileSync utf8 (CanadaHonk) [#49884](https://github.com/nodejs/node/pull/49884)
* \[[`05e25e0230`](https://github.com/nodejs/node/commit/05e25e0230)] - **fs**: improve error perf of sync `lstat`+`fstat` (CanadaHonk) [#49868](https://github.com/nodejs/node/pull/49868)
* \[[`f94a24cb4b`](https://github.com/nodejs/node/commit/f94a24cb4b)] - **fs**: improve error performance for `rmdirSync` (CanadaHonk) [#49846](https://github.com/nodejs/node/pull/49846)
* \[[`cada22e2a4`](https://github.com/nodejs/node/commit/cada22e2a4)] - **fs**: fix to not return for void function (Jungku Lee) [#50769](https://github.com/nodejs/node/pull/50769)
* \[[`ba40b2e33e`](https://github.com/nodejs/node/commit/ba40b2e33e)] - **fs**: replace deprecated `path._makeLong` in copyFile (CanadaHonk) [#50844](https://github.com/nodejs/node/pull/50844)
* \[[`d1b6bd660a`](https://github.com/nodejs/node/commit/d1b6bd660a)] - **fs**: update param in jsdoc for `readdir` (Jungku Lee) [#50448](https://github.com/nodejs/node/pull/50448)
* \[[`11412e863a`](https://github.com/nodejs/node/commit/11412e863a)] - **fs**: do not throw error on cpSync internals (Yagiz Nizipli) [#50185](https://github.com/nodejs/node/pull/50185)
* \[[`868a464c15`](https://github.com/nodejs/node/commit/868a464c15)] - **fs,url**: move `FromNamespacedPath` to `node_url` (Yagiz Nizipli) [#50090](https://github.com/nodejs/node/pull/50090)
* \[[`de7fe08c7b`](https://github.com/nodejs/node/commit/de7fe08c7b)] - **fs,url**: refactor `FileURLToPath` method (Yagiz Nizipli) [#50090](https://github.com/nodejs/node/pull/50090)
* \[[`186e6e0395`](https://github.com/nodejs/node/commit/186e6e0395)] - **fs,url**: move `FileURLToPath` to node\_url (Yagiz Nizipli) [#50090](https://github.com/nodejs/node/pull/50090)
* \[[`aea7fe54af`](https://github.com/nodejs/node/commit/aea7fe54af)] - **inspector**: use private fields instead of symbols (Yagiz Nizipli) [#50776](https://github.com/nodejs/node/pull/50776)
* \[[`48dbde71d8`](https://github.com/nodejs/node/commit/48dbde71d8)] - **lib**: use primordials for navigator.userAgent (Aras Abbasi) [#50467](https://github.com/nodejs/node/pull/50467)
* \[[`fa220cac87`](https://github.com/nodejs/node/commit/fa220cac87)] - **lib**: remove deprecated string methods (Jithil P Ponnan) [#50592](https://github.com/nodejs/node/pull/50592)
* \[[`f1cf1c385f`](https://github.com/nodejs/node/commit/f1cf1c385f)] - **lib**: fix assert shows diff messages in ESM and CJS (Jithil P Ponnan) [#50634](https://github.com/nodejs/node/pull/50634)
* \[[`3844af288f`](https://github.com/nodejs/node/commit/3844af288f)] - **lib**: make event static properties non writable and configurable (Muthukumar) [#50425](https://github.com/nodejs/node/pull/50425)
* \[[`0a0b416d6c`](https://github.com/nodejs/node/commit/0a0b416d6c)] - **lib**: avoid memory allocation on nodeprecation flag (Vinicius Lourenço) [#50231](https://github.com/nodejs/node/pull/50231)
* \[[`e7551d5770`](https://github.com/nodejs/node/commit/e7551d5770)] - **lib**: align console.table row to the left (Jithil P Ponnan) [#50135](https://github.com/nodejs/node/pull/50135)
* \[[`0c85cebdf2`](https://github.com/nodejs/node/commit/0c85cebdf2)] - **meta**: clarify nomination process according to Node.js charter (Matteo Collina) [#50834](https://github.com/nodejs/node/pull/50834)
* \[[`f4070dd8d4`](https://github.com/nodejs/node/commit/f4070dd8d4)] - **meta**: clarify recommendation for bug reproductions (Antoine du Hamel) [#50882](https://github.com/nodejs/node/pull/50882)
* \[[`2ddeead436`](https://github.com/nodejs/node/commit/2ddeead436)] - **meta**: move cjihrig to TSC regular member (Colin Ihrig) [#50816](https://github.com/nodejs/node/pull/50816)
* \[[`34a789d9be`](https://github.com/nodejs/node/commit/34a789d9be)] - **meta**: add web-standards as WPTs owner (Filip Skokan) [#50636](https://github.com/nodejs/node/pull/50636)
* \[[`40bbffa266`](https://github.com/nodejs/node/commit/40bbffa266)] - **meta**: bump github/codeql-action from 2.21.9 to 2.22.5 (dependabot\[bot]) [#50513](https://github.com/nodejs/node/pull/50513)
* \[[`c49553631d`](https://github.com/nodejs/node/commit/c49553631d)] - **meta**: bump step-security/harden-runner from 2.5.1 to 2.6.0 (dependabot\[bot]) [#50512](https://github.com/nodejs/node/pull/50512)
* \[[`99df0138b0`](https://github.com/nodejs/node/commit/99df0138b0)] - **meta**: bump ossf/scorecard-action from 2.2.0 to 2.3.1 (dependabot\[bot]) [#50509](https://github.com/nodejs/node/pull/50509)
* \[[`9db6227ac6`](https://github.com/nodejs/node/commit/9db6227ac6)] - **meta**: fix spacing in collaborator list (Antoine du Hamel) [#50641](https://github.com/nodejs/node/pull/50641)
* \[[`2589a5a566`](https://github.com/nodejs/node/commit/2589a5a566)] - **meta**: bump actions/setup-python from 4.7.0 to 4.7.1 (dependabot\[bot]) [#50510](https://github.com/nodejs/node/pull/50510)
* \[[`5a86661a95`](https://github.com/nodejs/node/commit/5a86661a95)] - **meta**: add crypto as crypto and webcrypto docs owner (Filip Skokan) [#50579](https://github.com/nodejs/node/pull/50579)
* \[[`ac8d2b9cc2`](https://github.com/nodejs/node/commit/ac8d2b9cc2)] - **meta**: bump actions/setup-node from 3.8.1 to 4.0.0 (dependabot\[bot]) [#50514](https://github.com/nodejs/node/pull/50514)
* \[[`bee2c0cf11`](https://github.com/nodejs/node/commit/bee2c0cf11)] - **meta**: bump actions/checkout from 4.1.0 to 4.1.1 (dependabot\[bot]) [#50511](https://github.com/nodejs/node/pull/50511)
* \[[`91a0944e5f`](https://github.com/nodejs/node/commit/91a0944e5f)] - **meta**: add <ethan.arrowood@vercel.com> to mailmap (Ethan Arrowood) [#50491](https://github.com/nodejs/node/pull/50491)
* \[[`8d3cf8c4ee`](https://github.com/nodejs/node/commit/8d3cf8c4ee)] - **meta**: add web-standards as web api visibility owner (Chengzhong Wu) [#50418](https://github.com/nodejs/node/pull/50418)
* \[[`807c12de36`](https://github.com/nodejs/node/commit/807c12de36)] - **meta**: mention other notable changes section (Rafael Gonzaga) [#50309](https://github.com/nodejs/node/pull/50309)
* \[[`21ab3c0f0b`](https://github.com/nodejs/node/commit/21ab3c0f0b)] - **(SEMVER-MINOR)** **module**: bootstrap module loaders in shadow realm (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`8e886a2fff`](https://github.com/nodejs/node/commit/8e886a2fff)] - **(SEMVER-MINOR)** **module**: remove useCustomLoadersIfPresent flag (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`77e8361213`](https://github.com/nodejs/node/commit/77e8361213)] - **module**: execute `--import` sequentially (Antoine du Hamel) [#50474](https://github.com/nodejs/node/pull/50474)
* \[[`fffc4951ac`](https://github.com/nodejs/node/commit/fffc4951ac)] - **module**: add application/json in accept header when fetching json module (Marco Ippolito) [#50119](https://github.com/nodejs/node/pull/50119)
* \[[`f808e7a650`](https://github.com/nodejs/node/commit/f808e7a650)] - **net**: check pipe mode and path (theanarkh) [#50770](https://github.com/nodejs/node/pull/50770)
* \[[`cf3a4c5b84`](https://github.com/nodejs/node/commit/cf3a4c5b84)] - **node-api**: factor out common code into macros (Gabriel Schulhof) [#50664](https://github.com/nodejs/node/pull/50664)
* \[[`a7d8f6b529`](https://github.com/nodejs/node/commit/a7d8f6b529)] - **perf\_hooks**: implement performance.now() with fast API calls (Joyee Cheung) [#50492](https://github.com/nodejs/node/pull/50492)
* \[[`076dc7540b`](https://github.com/nodejs/node/commit/076dc7540b)] - **permission**: do not create symlinks if target is relative (Tobias Nießen) [#49156](https://github.com/nodejs/node/pull/49156)
* \[[`43160dcd2d`](https://github.com/nodejs/node/commit/43160dcd2d)] - **permission**: mark const functions as such (Tobias Nießen) [#50705](https://github.com/nodejs/node/pull/50705)
* \[[`7a661d7ad9`](https://github.com/nodejs/node/commit/7a661d7ad9)] - **permission**: address coverity warning (Michael Dawson) [#50215](https://github.com/nodejs/node/pull/50215)
* \[[`b2b4132c3e`](https://github.com/nodejs/node/commit/b2b4132c3e)] - **src**: iterate on import attributes array correctly (Michaël Zasso) [#50703](https://github.com/nodejs/node/pull/50703)
* \[[`11b3e470db`](https://github.com/nodejs/node/commit/11b3e470db)] - **(SEMVER-MINOR)** **src**: create per isolate proxy env template (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`d00412a083`](https://github.com/nodejs/node/commit/d00412a083)] - **(SEMVER-MINOR)** **src**: create fs\_dir per isolate properties (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`14cc3b9b90`](https://github.com/nodejs/node/commit/14cc3b9b90)] - **(SEMVER-MINOR)** **src**: create worker per isolate properties (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`621c4d66c2`](https://github.com/nodejs/node/commit/621c4d66c2)] - **(SEMVER-MINOR)** **src**: make process binding data weak (Chengzhong Wu) [#48655](https://github.com/nodejs/node/pull/48655)
* \[[`07a4e94e84`](https://github.com/nodejs/node/commit/07a4e94e84)] - **src**: assert return value of BN\_bn2binpad (Tobias Nießen) [#50860](https://github.com/nodejs/node/pull/50860)
* \[[`158db2d61e`](https://github.com/nodejs/node/commit/158db2d61e)] - **src**: fix coverity warning (Michael Dawson) [#50846](https://github.com/nodejs/node/pull/50846)
* \[[`94363bb3fd`](https://github.com/nodejs/node/commit/94363bb3fd)] - **src**: fix compatility with upcoming V8 12.1 APIs (Cheng Zhao) [#50709](https://github.com/nodejs/node/pull/50709)
* \[[`29d91b13e3`](https://github.com/nodejs/node/commit/29d91b13e3)] - **(SEMVER-MINOR)** **src**: add `--disable-warning` option (Ethan Arrowood) [#50661](https://github.com/nodejs/node/pull/50661)
* \[[`f054c337f8`](https://github.com/nodejs/node/commit/f054c337f8)] - **src**: add IsolateScopes before using isolates (Keyhan Vakil) [#50680](https://github.com/nodejs/node/pull/50680)
* \[[`d08eb382cd`](https://github.com/nodejs/node/commit/d08eb382cd)] - **src**: avoid copying strings in FSPermission::Apply (Tobias Nießen) [#50662](https://github.com/nodejs/node/pull/50662)
* \[[`6620df1c05`](https://github.com/nodejs/node/commit/6620df1c05)] - **src**: remove erroneous default argument in RadixTree (Tobias Nießen) [#50736](https://github.com/nodejs/node/pull/50736)
* \[[`436c3aef15`](https://github.com/nodejs/node/commit/436c3aef15)] - **src**: fix JSONParser leaking internal V8 scopes (Keyhan Vakil) [#50688](https://github.com/nodejs/node/pull/50688)
* \[[`6f46d31018`](https://github.com/nodejs/node/commit/6f46d31018)] - **src**: return error --env-file if file is not found (Ardi Nugraha) [#50588](https://github.com/nodejs/node/pull/50588)
* \[[`3d43fd359c`](https://github.com/nodejs/node/commit/3d43fd359c)] - **src**: avoid silent coercion to signed/unsigned int (Tobias Nießen) [#50663](https://github.com/nodejs/node/pull/50663)
* \[[`c253e39b56`](https://github.com/nodejs/node/commit/c253e39b56)] - **src**: handle errors from uv\_pipe\_connect2() (Deokjin Kim) [#50657](https://github.com/nodejs/node/pull/50657)
* \[[`3a9713bb5a`](https://github.com/nodejs/node/commit/3a9713bb5a)] - **src**: use v8::Isolate::TryGetCurrent() in DumpJavaScriptBacktrace() (Joyee Cheung) [#50518](https://github.com/nodejs/node/pull/50518)
* \[[`94f8a925a8`](https://github.com/nodejs/node/commit/94f8a925a8)] - **src**: print more information in C++ assertions (Joyee Cheung) [#50242](https://github.com/nodejs/node/pull/50242)
* \[[`23f830616b`](https://github.com/nodejs/node/commit/23f830616b)] - **src**: hide node::credentials::HasOnly outside unit (Tobias Nießen) [#50450](https://github.com/nodejs/node/pull/50450)
* \[[`b7ecb0a390`](https://github.com/nodejs/node/commit/b7ecb0a390)] - **src**: readiterable entries may be empty (Matthew Aitken) [#50398](https://github.com/nodejs/node/pull/50398)
* \[[`4ef1d68715`](https://github.com/nodejs/node/commit/4ef1d68715)] - **src**: implement structuredClone in native (Joyee Cheung) [#50330](https://github.com/nodejs/node/pull/50330)
* \[[`9346f15138`](https://github.com/nodejs/node/commit/9346f15138)] - **src**: use find instead of char-by-char in FromFilePath() (Daniel Lemire) [#50288](https://github.com/nodejs/node/pull/50288)
* \[[`8414fb4d2a`](https://github.com/nodejs/node/commit/8414fb4d2a)] - **src**: add commit hash shorthand in zlib version (Jithil P Ponnan) [#50158](https://github.com/nodejs/node/pull/50158)
* \[[`a878e3abb0`](https://github.com/nodejs/node/commit/a878e3abb0)] - **stream**: fix enumerability of ReadableStream.from (Mattias Buelens) [#50779](https://github.com/nodejs/node/pull/50779)
* \[[`95ed4ffc1e`](https://github.com/nodejs/node/commit/95ed4ffc1e)] - **stream**: fix enumerability of ReadableStream.prototype.values (Mattias Buelens) [#50779](https://github.com/nodejs/node/pull/50779)
* \[[`4cf155ca0c`](https://github.com/nodejs/node/commit/4cf155ca0c)] - **stream**: add Symbol.toStringTag to Compression Streams (Filip Skokan) [#50712](https://github.com/nodejs/node/pull/50712)
* \[[`6012e3e781`](https://github.com/nodejs/node/commit/6012e3e781)] - **stream**: fix Writable.destroy performance regression (Robert Nagy) [#50478](https://github.com/nodejs/node/pull/50478)
* \[[`dd5206820c`](https://github.com/nodejs/node/commit/dd5206820c)] - **stream**: pre-allocate \_events (Robert Nagy) [#50428](https://github.com/nodejs/node/pull/50428)
* \[[`829b82ed0f`](https://github.com/nodejs/node/commit/829b82ed0f)] - **stream**: remove no longer relevant comment (Robert Nagy) [#50446](https://github.com/nodejs/node/pull/50446)
* \[[`98ae1b4132`](https://github.com/nodejs/node/commit/98ae1b4132)] - **stream**: use bit fields for construct/destroy (Robert Nagy) [#50408](https://github.com/nodejs/node/pull/50408)
* \[[`08a0c6c56c`](https://github.com/nodejs/node/commit/08a0c6c56c)] - **stream**: improve from perf (Raz Luvaton) [#50359](https://github.com/nodejs/node/pull/50359)
* \[[`59f7316b8f`](https://github.com/nodejs/node/commit/59f7316b8f)] - **stream**: avoid calls to listenerCount (Robert Nagy) [#50357](https://github.com/nodejs/node/pull/50357)
* \[[`9d52430eb9`](https://github.com/nodejs/node/commit/9d52430eb9)] - **stream**: readable use bitmap accessors (Robert Nagy) [#50350](https://github.com/nodejs/node/pull/50350)
* \[[`139d6c8d3b`](https://github.com/nodejs/node/commit/139d6c8d3b)] - **stream**: use Array for Readable buffer (Robert Nagy) [#50341](https://github.com/nodejs/node/pull/50341)
* \[[`6206957e8d`](https://github.com/nodejs/node/commit/6206957e8d)] - **stream**: optimize creation (Robert Nagy) [#50337](https://github.com/nodejs/node/pull/50337)
* \[[`f87921de3b`](https://github.com/nodejs/node/commit/f87921de3b)] - **stream**: refactor writable \_write (Robert Nagy) [#50198](https://github.com/nodejs/node/pull/50198)
* \[[`b338f3d3c2`](https://github.com/nodejs/node/commit/b338f3d3c2)] - **stream**: avoid getter for defaultEncoding (Robert Nagy) [#50203](https://github.com/nodejs/node/pull/50203)
* \[[`1862235a26`](https://github.com/nodejs/node/commit/1862235a26)] - **test**: fix message v8 not normalising alphanumeric paths (Jithil P Ponnan) [#50730](https://github.com/nodejs/node/pull/50730)
* \[[`7c28a4ca8f`](https://github.com/nodejs/node/commit/7c28a4ca8f)] - **test**: fix dns test case failures after c-ares update to 1.21.0+ (Brad House) [#50743](https://github.com/nodejs/node/pull/50743)
* \[[`4544593d31`](https://github.com/nodejs/node/commit/4544593d31)] - **test**: replace forEach with for of (Conor Watson) [#50594](https://github.com/nodejs/node/pull/50594)
* \[[`96143a3293`](https://github.com/nodejs/node/commit/96143a3293)] - **test**: replace forEach to for at test-webcrypto-sign-verify-ecdsa.js (Alessandro Di Nisio) [#50795](https://github.com/nodejs/node/pull/50795)
* \[[`107b5e63c5`](https://github.com/nodejs/node/commit/107b5e63c5)] - **test**: replace foreach with for in test-https-simple.js (Shikha Mehta) [#49793](https://github.com/nodejs/node/pull/49793)
* \[[`9b2e5e9db4`](https://github.com/nodejs/node/commit/9b2e5e9db4)] - **test**: add note about unresolved spec issue (Mattias Buelens) [#50779](https://github.com/nodejs/node/pull/50779)
* \[[`edce637c1a`](https://github.com/nodejs/node/commit/edce637c1a)] - **test**: add note about readable streams with type owning (Mattias Buelens) [#50779](https://github.com/nodejs/node/pull/50779)
* \[[`641044670b`](https://github.com/nodejs/node/commit/641044670b)] - **test**: replace forEach with for-of in test-url-relative (vitosorriso) [#50788](https://github.com/nodejs/node/pull/50788)
* \[[`75ee78438c`](https://github.com/nodejs/node/commit/75ee78438c)] - **test**: replace forEach() with for ... of in test-tls-getprotocol.js (Steve Goode) [#50600](https://github.com/nodejs/node/pull/50600)
* \[[`24f9d3fbeb`](https://github.com/nodejs/node/commit/24f9d3fbeb)] - **test**: enable idlharness tests for encoding (Mattias Buelens) [#50778](https://github.com/nodejs/node/pull/50778)
* \[[`a9d290956e`](https://github.com/nodejs/node/commit/a9d290956e)] - **test**: replace forEach in whatwg-encoding-custom-interop (Honza Machala) [#50607](https://github.com/nodejs/node/pull/50607)
* \[[`6584dd80f7`](https://github.com/nodejs/node/commit/6584dd80f7)] - **test**: replace forEach() with for-loop (Jan) [#50596](https://github.com/nodejs/node/pull/50596)
* \[[`be54a22869`](https://github.com/nodejs/node/commit/be54a22869)] - **test**: improve test-bootstrap-modules.js (Joyee Cheung) [#50708](https://github.com/nodejs/node/pull/50708)
* \[[`660e70e73b`](https://github.com/nodejs/node/commit/660e70e73b)] - **test**: skip parallel/test-macos-app-sandbox if disk space < 120MB (Joyee Cheung) [#50764](https://github.com/nodejs/node/pull/50764)
* \[[`5712c41122`](https://github.com/nodejs/node/commit/5712c41122)] - **test**: replace foreach with for (Markus Muschol) [#50599](https://github.com/nodejs/node/pull/50599)
* \[[`49e5f47b1c`](https://github.com/nodejs/node/commit/49e5f47b1c)] - **test**: test streambase has already has a consumer (Jithil P Ponnan) [#48059](https://github.com/nodejs/node/pull/48059)
* \[[`bb7d764c8e`](https://github.com/nodejs/node/commit/bb7d764c8e)] - **test**: change forEach to for...of in path extname (Kyriakos Markakis) [#50667](https://github.com/nodejs/node/pull/50667)
* \[[`4d28ced079`](https://github.com/nodejs/node/commit/4d28ced079)] - **test**: replace forEach with for...of (Ryan Williams) [#50611](https://github.com/nodejs/node/pull/50611)
* \[[`92a153ecde`](https://github.com/nodejs/node/commit/92a153ecde)] - **test**: migrate message v8 tests from Python to JS (Joshua LeMay) [#50421](https://github.com/nodejs/node/pull/50421)
* \[[`a376284d8a`](https://github.com/nodejs/node/commit/a376284d8a)] - **test**: use destructuring for accessing setting values (Honza Jedlička) [#50609](https://github.com/nodejs/node/pull/50609)
* \[[`7b9b1fba27`](https://github.com/nodejs/node/commit/7b9b1fba27)] - **test**: replace forEach() with for .. of (Evgenia Blajer) [#50605](https://github.com/nodejs/node/pull/50605)
* \[[`9397b2da7e`](https://github.com/nodejs/node/commit/9397b2da7e)] - **test**: replace forEach() with for ... of in test-readline-keys.js (William Liang) [#50604](https://github.com/nodejs/node/pull/50604)
* \[[`9043ba4cfb`](https://github.com/nodejs/node/commit/9043ba4cfb)] - **test**: replace forEach() with for ... of in test-http2-single-headers.js (spiritualized) [#50606](https://github.com/nodejs/node/pull/50606)
* \[[`9f911d31f6`](https://github.com/nodejs/node/commit/9f911d31f6)] - **test**: replace forEach with for of (john-mcinall) [#50602](https://github.com/nodejs/node/pull/50602)
* \[[`8a5f36fe74`](https://github.com/nodejs/node/commit/8a5f36fe74)] - **test**: remove unused file (James Sumners) [#50528](https://github.com/nodejs/node/pull/50528)
* \[[`9950203340`](https://github.com/nodejs/node/commit/9950203340)] - **test**: replace forEach with for of (Kevin Kühnemund) [#50597](https://github.com/nodejs/node/pull/50597)
* \[[`03ba28f102`](https://github.com/nodejs/node/commit/03ba28f102)] - **test**: replace forEach with for of (CorrWu) [#49785](https://github.com/nodejs/node/pull/49785)
* \[[`ea61261b54`](https://github.com/nodejs/node/commit/ea61261b54)] - **test**: replace forEach with for \[...] of (Gabriel Bota) [#50615](https://github.com/nodejs/node/pull/50615)
* \[[`4349790913`](https://github.com/nodejs/node/commit/4349790913)] - **test**: add WPT report test duration (Filip Skokan) [#50574](https://github.com/nodejs/node/pull/50574)
* \[[`7cacddfcc1`](https://github.com/nodejs/node/commit/7cacddfcc1)] - **test**: replace forEach() with for ... of loop in test-global.js (Kajol) [#49772](https://github.com/nodejs/node/pull/49772)
* \[[`889f58d07f`](https://github.com/nodejs/node/commit/889f58d07f)] - **test**: skip test-diagnostics-channel-memory-leak.js (Joyee Cheung) [#50327](https://github.com/nodejs/node/pull/50327)
* \[[`41644ee071`](https://github.com/nodejs/node/commit/41644ee071)] - **test**: improve `UV_THREADPOOL_SIZE` tests on `.env` (Yagiz Nizipli) [#49213](https://github.com/nodejs/node/pull/49213)
* \[[`1db44b9a53`](https://github.com/nodejs/node/commit/1db44b9a53)] - **test**: recognize wpt completion error (Chengzhong Wu) [#50429](https://github.com/nodejs/node/pull/50429)
* \[[`ecfc951ddc`](https://github.com/nodejs/node/commit/ecfc951ddc)] - **test**: report error wpt test results (Chengzhong Wu) [#50429](https://github.com/nodejs/node/pull/50429)
* \[[`deb0351d95`](https://github.com/nodejs/node/commit/deb0351d95)] - **test**: replace forEach() with for...of (Ram) [#49794](https://github.com/nodejs/node/pull/49794)
* \[[`f885dfe5e3`](https://github.com/nodejs/node/commit/f885dfe5e3)] - **test**: replace forEach() with for...of in test-trace-events-http (Chand) [#49795](https://github.com/nodejs/node/pull/49795)
* \[[`9dc63c56db`](https://github.com/nodejs/node/commit/9dc63c56db)] - **test**: replace forEach with for...of in test-fs-realpath-buffer-encoding (Niya Shiyas) [#49804](https://github.com/nodejs/node/pull/49804)
* \[[`600d1260da`](https://github.com/nodejs/node/commit/600d1260da)] - **test**: fix timeout of test-cpu-prof-dir-worker.js in LoongArch devices (Shi Pujin) [#50363](https://github.com/nodejs/node/pull/50363)
* \[[`099f5cfa0a`](https://github.com/nodejs/node/commit/099f5cfa0a)] - **test**: fix vm assertion actual and expected order (Chengzhong Wu) [#50371](https://github.com/nodejs/node/pull/50371)
* \[[`a31f9bfe01`](https://github.com/nodejs/node/commit/a31f9bfe01)] - **test**: v8: Add test-linux-perf-logger test suite (Luke Albao) [#50352](https://github.com/nodejs/node/pull/50352)
* \[[`6c59114947`](https://github.com/nodejs/node/commit/6c59114947)] - **test**: ensure never settling promises are detected (Antoine du Hamel) [#50318](https://github.com/nodejs/node/pull/50318)
* \[[`9830ae4bf7`](https://github.com/nodejs/node/commit/9830ae4bf7)] - **test\_runner**: add tests for various mock timer issues (Mika Fischer) [#50384](https://github.com/nodejs/node/pull/50384)
* \[[`2c72ed85fb`](https://github.com/nodejs/node/commit/2c72ed85fb)] - **test\_runner**: pass abortSignal to test files (Moshe Atlow) [#50630](https://github.com/nodejs/node/pull/50630)
* \[[`c33a84af11`](https://github.com/nodejs/node/commit/c33a84af11)] - **test\_runner**: replace forEach with for of (Tom Haddad) [#50595](https://github.com/nodejs/node/pull/50595)
* \[[`29c68a22bb`](https://github.com/nodejs/node/commit/29c68a22bb)] - **test\_runner**: output errors of suites (Moshe Atlow) [#50361](https://github.com/nodejs/node/pull/50361)
* \[[`e64378643d`](https://github.com/nodejs/node/commit/e64378643d)] - **(SEMVER-MINOR)** **test\_runner**: adds built in lcov reporter (Phil Nash) [#50018](https://github.com/nodejs/node/pull/50018)
* \[[`4aaaff413b`](https://github.com/nodejs/node/commit/4aaaff413b)] - **test\_runner**: test return value of mocked promisified timers (Mika Fischer) [#50331](https://github.com/nodejs/node/pull/50331)
* \[[`4a830c2d9d`](https://github.com/nodejs/node/commit/4a830c2d9d)] - **(SEMVER-MINOR)** **test\_runner**: add Date to the supported mock APIs (Lucas Santos) [#48638](https://github.com/nodejs/node/pull/48638)
* \[[`842dc01def`](https://github.com/nodejs/node/commit/842dc01def)] - **(SEMVER-MINOR)** **test\_runner, cli**: add --test-timeout flag (Shubham Pandey) [#50443](https://github.com/nodejs/node/pull/50443)
* \[[`613a9072b7`](https://github.com/nodejs/node/commit/613a9072b7)] - **tls**: fix order of setting cipher before setting cert and key (Kumar Rishav) [#50186](https://github.com/nodejs/node/pull/50186)
* \[[`d905c61e16`](https://github.com/nodejs/node/commit/d905c61e16)] - **tls**: use `validateFunction` for `options.SNICallback` (Deokjin Kim) [#50530](https://github.com/nodejs/node/pull/50530)
* \[[`c8d6dd58e7`](https://github.com/nodejs/node/commit/c8d6dd58e7)] - **tools**: add macOS notarization verification step (Ulises Gascón) [#50833](https://github.com/nodejs/node/pull/50833)
* \[[`c9bd0b0c0f`](https://github.com/nodejs/node/commit/c9bd0b0c0f)] - **tools**: use macOS keychain to notarize the releases (Ulises Gascón) [#50715](https://github.com/nodejs/node/pull/50715)
* \[[`932a5d7b2c`](https://github.com/nodejs/node/commit/932a5d7b2c)] - **tools**: update eslint to 8.54.0 (Node.js GitHub Bot) [#50809](https://github.com/nodejs/node/pull/50809)
* \[[`d7114d97be`](https://github.com/nodejs/node/commit/d7114d97be)] - **tools**: update lint-md-dependencies to rollup\@4.5.0 (Node.js GitHub Bot) [#50807](https://github.com/nodejs/node/pull/50807)
* \[[`93085cf844`](https://github.com/nodejs/node/commit/93085cf844)] - **tools**: add workflow to update release links (Michaël Zasso) [#50710](https://github.com/nodejs/node/pull/50710)
* \[[`66764c5d04`](https://github.com/nodejs/node/commit/66764c5d04)] - **tools**: recognize GN files in dep\_updaters (Cheng Zhao) [#50693](https://github.com/nodejs/node/pull/50693)
* \[[`2a451e176a`](https://github.com/nodejs/node/commit/2a451e176a)] - **tools**: remove unused file (Ulises Gascon) [#50622](https://github.com/nodejs/node/pull/50622)
* \[[`8ce6403230`](https://github.com/nodejs/node/commit/8ce6403230)] - **tools**: change minimatch install strategy (Marco Ippolito) [#50476](https://github.com/nodejs/node/pull/50476)
* \[[`97778e2e77`](https://github.com/nodejs/node/commit/97778e2e77)] - **tools**: update lint-md-dependencies to rollup\@4.3.1 (Node.js GitHub Bot) [#50675](https://github.com/nodejs/node/pull/50675)
* \[[`797f6a9ba8`](https://github.com/nodejs/node/commit/797f6a9ba8)] - **tools**: add macOS notarization stapler (Ulises Gascón) [#50625](https://github.com/nodejs/node/pull/50625)
* \[[`8fa1319352`](https://github.com/nodejs/node/commit/8fa1319352)] - **tools**: update eslint to 8.53.0 (Node.js GitHub Bot) [#50559](https://github.com/nodejs/node/pull/50559)
* \[[`592f57970f`](https://github.com/nodejs/node/commit/592f57970f)] - **tools**: update lint-md-dependencies to rollup\@4.3.0 (Node.js GitHub Bot) [#50556](https://github.com/nodejs/node/pull/50556)
* \[[`2fd78fc39e`](https://github.com/nodejs/node/commit/2fd78fc39e)] - **tools**: compare ICU checksums before file changes (Michaël Zasso) [#50522](https://github.com/nodejs/node/pull/50522)
* \[[`631d710fc4`](https://github.com/nodejs/node/commit/631d710fc4)] - **tools**: improve update acorn-walk script (Marco Ippolito) [#50473](https://github.com/nodejs/node/pull/50473)
* \[[`33fd2af2ab`](https://github.com/nodejs/node/commit/33fd2af2ab)] - **tools**: update lint-md-dependencies to rollup\@4.2.0 (Node.js GitHub Bot) [#50496](https://github.com/nodejs/node/pull/50496)
* \[[`22b7a74838`](https://github.com/nodejs/node/commit/22b7a74838)] - **tools**: update gyp-next to v0.16.1 (Michaël Zasso) [#50380](https://github.com/nodejs/node/pull/50380)
* \[[`f5ccab5005`](https://github.com/nodejs/node/commit/f5ccab5005)] - **tools**: skip ruff on tools/gyp (Michaël Zasso) [#50380](https://github.com/nodejs/node/pull/50380)
* \[[`408fd90508`](https://github.com/nodejs/node/commit/408fd90508)] - **tools**: update lint-md-dependencies to rollup\@4.1.5 unified\@11.0.4 (Node.js GitHub Bot) [#50461](https://github.com/nodejs/node/pull/50461)
* \[[`685f936ccd`](https://github.com/nodejs/node/commit/685f936ccd)] - **tools**: avoid npm install in deps installation (Marco Ippolito) [#50413](https://github.com/nodejs/node/pull/50413)
* \[[`7d43c5a094`](https://github.com/nodejs/node/commit/7d43c5a094)] - _**Revert**_ "**tools**: update doc dependencies" (Richard Lau) [#50414](https://github.com/nodejs/node/pull/50414)
* \[[`8fd67c2e3e`](https://github.com/nodejs/node/commit/8fd67c2e3e)] - **tools**: update doc dependencies (Node.js GitHub Bot) [#49988](https://github.com/nodejs/node/pull/49988)
* \[[`586becb507`](https://github.com/nodejs/node/commit/586becb507)] - **tools**: run coverage CI only on relevant files (Antoine du Hamel) [#50349](https://github.com/nodejs/node/pull/50349)
* \[[`2d06eea6c5`](https://github.com/nodejs/node/commit/2d06eea6c5)] - **tools**: update eslint to 8.52.0 (Node.js GitHub Bot) [#50326](https://github.com/nodejs/node/pull/50326)
* \[[`6a897baf16`](https://github.com/nodejs/node/commit/6a897baf16)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#50190](https://github.com/nodejs/node/pull/50190)
* \[[`e6e7f39b9e`](https://github.com/nodejs/node/commit/e6e7f39b9e)] - **util**: improve performance of normalizeEncoding (kylo5aby) [#50721](https://github.com/nodejs/node/pull/50721)
* \[[`3b6b1afa47`](https://github.com/nodejs/node/commit/3b6b1afa47)] - **v8,tools**: expose necessary V8 defines (Cheng Zhao) [#50820](https://github.com/nodejs/node/pull/50820)
* \[[`2664012617`](https://github.com/nodejs/node/commit/2664012617)] - **vm**: allow dynamic import with a referrer realm (Chengzhong Wu) [#50360](https://github.com/nodejs/node/pull/50360)
* \[[`c6c0a74b54`](https://github.com/nodejs/node/commit/c6c0a74b54)] - **wasi**: document security sandboxing status (Guy Bedford) [#50396](https://github.com/nodejs/node/pull/50396)
* \[[`989814093e`](https://github.com/nodejs/node/commit/989814093e)] - **win,tools**: upgrade Windows signing to smctl (Stefan Stojanovic) [#50956](https://github.com/nodejs/node/pull/50956)

<a id="20.10.0"></a>

## 2023-11-22, Version 20.10.0 'Iron' (LTS), @targos

### Notable Changes

#### `--experimental-default-type` flag to flip module defaults

The new flag `--experimental-default-type` can be used to flip the default
module system used by Node.js. Input that is already explicitly defined as ES
modules or CommonJS, such as by a `package.json` `"type"` field or `.mjs`/`.cjs`
file extension or the `--input-type` flag, is unaffected. What is currently
implicitly CommonJS would instead be interpreted as ES modules under
`--experimental-default-type=module`:

* String input provided via `--eval` or STDIN, if `--input-type` is unspecified.

* Files ending in `.js` or with no extension, if there is no `package.json` file
  present in the same folder or any parent folder.

* Files ending in `.js` or with no extension, if the nearest parent
  `package.json` field lacks a `type` field; unless the folder is inside a
  `node_modules` folder.

In addition, extensionless files are interpreted as Wasm if
`--experimental-wasm-modules` is passed and the file contains the "magic bytes"
Wasm header.

Contributed by Geoffrey Booth in [#49869](https://github.com/nodejs/node/pull/49869).

#### Detect ESM syntax in ambiguous JavaScript

The new flag `--experimental-detect-module` can be used to automatically run ES
modules when their syntax can be detected. For “ambiguous” files, which are
`.js` or extensionless files with no `package.json` with a `type` field, Node.js
will parse the file to detect ES module syntax; if found, it will run the file
as an ES module, otherwise it will run the file as a CommonJS module. The same
applies to string input via `--eval` or `STDIN`.

We hope to make detection enabled by default in a future version of Node.js.
Detection increases startup time, so we encourage everyone—especially package
authors—to add a `type` field to `package.json`, even for the default
`"type": "commonjs"`. The presence of a `type` field, or explicit extensions
such as `.mjs` or `.cjs`, will opt out of detection.

Contributed by Geoffrey Booth in [#50096](https://github.com/nodejs/node/pull/50096).

#### New `flush` option in file system functions

When writing to files, it is possible that data is not immediately flushed to
permanent storage. This allows subsequent read operations to see stale data.
This PR adds a `'flush'` option to the `fs.writeFile` family of functions which
forces the data to be flushed at the end of a successful write operation.

Contributed by Colin Ihrig in [#50009](https://github.com/nodejs/node/pull/50009) and [#50095](https://github.com/nodejs/node/pull/50095).

#### Experimental WebSocket client

Adds a `--experimental-websocket` flag that adds a [`WebSocket`](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
global, as [standardized by WHATWG](https://websockets.spec.whatwg.org/#the-websocket-interface).

Contributed by Matthew Aitken in [#49830](https://github.com/nodejs/node/pull/49830).

#### vm: fix V8 compilation cache support for vm.Script

Previously repeated compilation of the same source code using `vm.Script`
stopped hitting the V8 compilation cache after v16.x when support for
`importModuleDynamically` was added to `vm.Script`, resulting in a performance
regression that blocked users (in particular Jest users) from upgrading from
v16.x.

The recent fixes allow the compilation cache to be hit again
for `vm.Script` when `--experimental-vm-modules` is not used even in the
presence of the `importModuleDynamically` option, so that users affected by the
performance regression can now upgrade. Ongoing work is also being done to
enable compilation cache support for `vm.CompileFunction`.

Contributed by Joyee Cheung in [#49950](https://github.com/nodejs/node/pull/49950)
and [#50137](https://github.com/nodejs/node/pull/50137).

#### Other notable changes

* \[[`21453ae555`](https://github.com/nodejs/node/commit/21453ae555)] - **(SEMVER-MINOR)** **deps**: update uvwasi to 0.0.19 (Node.js GitHub Bot) [#49908](https://github.com/nodejs/node/pull/49908)
* \[[`ee65e44c31`](https://github.com/nodejs/node/commit/ee65e44c31)] - **esm**: use import attributes instead of import assertions (Antoine du Hamel) [#50140](https://github.com/nodejs/node/pull/50140)
* \[[`ffdc357167`](https://github.com/nodejs/node/commit/ffdc357167)] - **(SEMVER-MINOR)** **stream**: allow passing stream class to `stream.compose` (Alex Yang) [#50187](https://github.com/nodejs/node/pull/50187)
* \[[`4861ad6431`](https://github.com/nodejs/node/commit/4861ad6431)] - **stream**: improve performance of readable stream reads (Raz Luvaton) [#50173](https://github.com/nodejs/node/pull/50173)
* \[[`4b27087b30`](https://github.com/nodejs/node/commit/4b27087b30)] - **stream**: optimize Writable (Robert Nagy) [#50012](https://github.com/nodejs/node/pull/50012)
* \[[`709c6c0cab`](https://github.com/nodejs/node/commit/709c6c0cab)] - **(SEMVER-MINOR)** **test\_runner, cli**: add --test-concurrency flag (Colin Ihrig) [#49996](https://github.com/nodejs/node/pull/49996)
* \[[`57efd5292c`](https://github.com/nodejs/node/commit/57efd5292c)] - **(SEMVER-MINOR)** **vm**: use import attributes instead of import assertions (Antoine du Hamel) [#50141](https://github.com/nodejs/node/pull/50141)

### Commits

* \[[`73757a5f42`](https://github.com/nodejs/node/commit/73757a5f42)] - **benchmark**: fix race condition on fs benchs (Vinicius Lourenço) [#50035](https://github.com/nodejs/node/pull/50035)
* \[[`23269717bb`](https://github.com/nodejs/node/commit/23269717bb)] - **benchmark**: add warmup to accessSync bench (Rafael Gonzaga) [#50073](https://github.com/nodejs/node/pull/50073)
* \[[`88611d199a`](https://github.com/nodejs/node/commit/88611d199a)] - **benchmark**: improved config for blob,file benchmark (Vinícius Lourenço) [#49730](https://github.com/nodejs/node/pull/49730)
* \[[`b70757476c`](https://github.com/nodejs/node/commit/b70757476c)] - **benchmark**: added new benchmarks for blob (Vinícius Lourenço) [#49730](https://github.com/nodejs/node/pull/49730)
* \[[`458d9a82e3`](https://github.com/nodejs/node/commit/458d9a82e3)] - **buffer**: remove unnecessary assignment in fromString (Tobias Nießen) [#50199](https://github.com/nodejs/node/pull/50199)
* \[[`878c0b332e`](https://github.com/nodejs/node/commit/878c0b332e)] - **build**: fix IBM i build with Python 3.9 (Richard Lau) [#48056](https://github.com/nodejs/node/pull/48056)
* \[[`773320e1de`](https://github.com/nodejs/node/commit/773320e1de)] - **crypto**: ensure valid point on elliptic curve in SubtleCrypto.importKey (Filip Skokan) [#50234](https://github.com/nodejs/node/pull/50234)
* \[[`edb0ffd7d4`](https://github.com/nodejs/node/commit/edb0ffd7d4)] - **crypto**: use X509\_ALGOR accessors instead of reaching into X509\_ALGOR (David Benjamin) [#50057](https://github.com/nodejs/node/pull/50057)
* \[[`3f98c06dbb`](https://github.com/nodejs/node/commit/3f98c06dbb)] - **crypto**: account for disabled SharedArrayBuffer (Shelley Vohr) [#50034](https://github.com/nodejs/node/pull/50034)
* \[[`55485ff1cc`](https://github.com/nodejs/node/commit/55485ff1cc)] - **crypto**: return clear errors when loading invalid PFX data (Tim Perry) [#49566](https://github.com/nodejs/node/pull/49566)
* \[[`68ec1e5eeb`](https://github.com/nodejs/node/commit/68ec1e5eeb)] - **deps**: upgrade npm to 10.2.3 (npm team) [#50531](https://github.com/nodejs/node/pull/50531)
* \[[`b00c11ad7c`](https://github.com/nodejs/node/commit/b00c11ad7c)] - **deps**: V8: cherry-pick d90d4533b053 (Michaël Zasso) [#50077](https://github.com/nodejs/node/pull/50077)
* \[[`e63aef91b4`](https://github.com/nodejs/node/commit/e63aef91b4)] - **deps**: V8: cherry-pick f7d000a7ae7b (Luke Albao) [#50077](https://github.com/nodejs/node/pull/50077)
* \[[`4b243b553a`](https://github.com/nodejs/node/commit/4b243b553a)] - **deps**: V8: cherry-pick 9721082687c9 (Shi Pujin) [#50077](https://github.com/nodejs/node/pull/50077)
* \[[`9d3cdcbebf`](https://github.com/nodejs/node/commit/9d3cdcbebf)] - **deps**: V8: cherry-pick 840650f2ff4e (Michaël Zasso) [#50077](https://github.com/nodejs/node/pull/50077)
* \[[`0c40b513fd`](https://github.com/nodejs/node/commit/0c40b513fd)] - **deps**: V8: cherry-pick a1efa5343880 (Michaël Zasso) [#50077](https://github.com/nodejs/node/pull/50077)
* \[[`68cddd79f7`](https://github.com/nodejs/node/commit/68cddd79f7)] - **deps**: update archs files for openssl-3.0.12+quic1 (Node.js GitHub Bot) [#50411](https://github.com/nodejs/node/pull/50411)
* \[[`3308189180`](https://github.com/nodejs/node/commit/3308189180)] - **deps**: upgrade openssl sources to quictls/openssl-3.0.12+quic1 (Node.js GitHub Bot) [#50411](https://github.com/nodejs/node/pull/50411)
* \[[`b61707e535`](https://github.com/nodejs/node/commit/b61707e535)] - **deps**: update ada to 2.7.2 (Node.js GitHub Bot) [#50338](https://github.com/nodejs/node/pull/50338)
* \[[`1aecf0c17b`](https://github.com/nodejs/node/commit/1aecf0c17b)] - **deps**: update corepack to 0.22.0 (Node.js GitHub Bot) [#50325](https://github.com/nodejs/node/pull/50325)
* \[[`f5924f174c`](https://github.com/nodejs/node/commit/f5924f174c)] - **deps**: update c-ares to 1.20.1 (Node.js GitHub Bot) [#50082](https://github.com/nodejs/node/pull/50082)
* \[[`b705e19a95`](https://github.com/nodejs/node/commit/b705e19a95)] - **deps**: update c-ares to 1.20.0 (Node.js GitHub Bot) [#50082](https://github.com/nodejs/node/pull/50082)
* \[[`f72cbb7e02`](https://github.com/nodejs/node/commit/f72cbb7e02)] - **deps**: V8: cherry-pick 25902244ad1a (Joyee Cheung) [#50156](https://github.com/nodejs/node/pull/50156)
* \[[`6547bd2493`](https://github.com/nodejs/node/commit/6547bd2493)] - **deps**: V8: cherry-pick ea996ad04a68 (Antoine du Hamel) [#50183](https://github.com/nodejs/node/pull/50183)
* \[[`16fd730e95`](https://github.com/nodejs/node/commit/16fd730e95)] - **deps**: V8: cherry-pick a0fd3209dda8 (Antoine du Hamel) [#50183](https://github.com/nodejs/node/pull/50183)
* \[[`614c3620c3`](https://github.com/nodejs/node/commit/614c3620c3)] - **deps**: update corepack to 0.21.0 (Node.js GitHub Bot) [#50088](https://github.com/nodejs/node/pull/50088)
* \[[`545aa74ae2`](https://github.com/nodejs/node/commit/545aa74ae2)] - **deps**: update simdutf to 3.2.18 (Node.js GitHub Bot) [#50091](https://github.com/nodejs/node/pull/50091)
* \[[`9302806c0a`](https://github.com/nodejs/node/commit/9302806c0a)] - **deps**: update zlib to 1.2.13.1-motley-fef5869 (Node.js GitHub Bot) [#50085](https://github.com/nodejs/node/pull/50085)
* \[[`03bf5c5d9a`](https://github.com/nodejs/node/commit/03bf5c5d9a)] - **deps**: update googletest to 2dd1c13 (Node.js GitHub Bot) [#50081](https://github.com/nodejs/node/pull/50081)
* \[[`cd8e90690b`](https://github.com/nodejs/node/commit/cd8e90690b)] - **deps**: update googletest to e47544a (Node.js GitHub Bot) [#49982](https://github.com/nodejs/node/pull/49982)
* \[[`40672cfe53`](https://github.com/nodejs/node/commit/40672cfe53)] - **deps**: update ada to 2.6.10 (Node.js GitHub Bot) [#49984](https://github.com/nodejs/node/pull/49984)
* \[[`34c7eb0eb2`](https://github.com/nodejs/node/commit/34c7eb0eb2)] - **deps**: fix call to undeclared functions 'ntohl' and 'htons' (MatteoBax) [#49979](https://github.com/nodejs/node/pull/49979)
* \[[`03654b44b6`](https://github.com/nodejs/node/commit/03654b44b6)] - **deps**: update ada to 2.6.9 (Node.js GitHub Bot) [#49340](https://github.com/nodejs/node/pull/49340)
* \[[`4c740b1dd8`](https://github.com/nodejs/node/commit/4c740b1dd8)] - **deps**: update ada to 2.6.8 (Node.js GitHub Bot) [#49340](https://github.com/nodejs/node/pull/49340)
* \[[`759cf5a760`](https://github.com/nodejs/node/commit/759cf5a760)] - **deps**: update ada to 2.6.7 (Node.js GitHub Bot) [#49340](https://github.com/nodejs/node/pull/49340)
* \[[`31a4e9781a`](https://github.com/nodejs/node/commit/31a4e9781a)] - **deps**: update ada to 2.6.5 (Node.js GitHub Bot) [#49340](https://github.com/nodejs/node/pull/49340)
* \[[`2ca867f2ab`](https://github.com/nodejs/node/commit/2ca867f2ab)] - **deps**: update ada to 2.6.3 (Node.js GitHub Bot) [#49340](https://github.com/nodejs/node/pull/49340)
* \[[`21453ae555`](https://github.com/nodejs/node/commit/21453ae555)] - **(SEMVER-MINOR)** **deps**: update uvwasi to 0.0.19 (Node.js GitHub Bot) [#49908](https://github.com/nodejs/node/pull/49908)
* \[[`7ca1228be8`](https://github.com/nodejs/node/commit/7ca1228be8)] - **deps**: V8: cherry-pick 8ec2651fbdd8 (Abdirahim Musse) [#49862](https://github.com/nodejs/node/pull/49862)
* \[[`3cc41d253c`](https://github.com/nodejs/node/commit/3cc41d253c)] - **deps**: upgrade npm to 10.2.0 (npm team) [#50027](https://github.com/nodejs/node/pull/50027)
* \[[`61b4afb7dd`](https://github.com/nodejs/node/commit/61b4afb7dd)] - **deps**: update undici to 5.26.4 (Node.js GitHub Bot) [#50274](https://github.com/nodejs/node/pull/50274)
* \[[`ea28738336`](https://github.com/nodejs/node/commit/ea28738336)] - **doc**: add loong64 info into platform list (Shi Pujin) [#50086](https://github.com/nodejs/node/pull/50086)
* \[[`00c12b7a20`](https://github.com/nodejs/node/commit/00c12b7a20)] - **doc**: update release process LTS step (Richard Lau) [#50299](https://github.com/nodejs/node/pull/50299)
* \[[`a9ba29ba10`](https://github.com/nodejs/node/commit/a9ba29ba10)] - **doc**: fix release process table of contents (Richard Lau) [#50216](https://github.com/nodejs/node/pull/50216)
* \[[`4b5033519e`](https://github.com/nodejs/node/commit/4b5033519e)] - **doc**: update api `stream.compose` (Alex Yang) [#50206](https://github.com/nodejs/node/pull/50206)
* \[[`d4659e2080`](https://github.com/nodejs/node/commit/d4659e2080)] - **doc**: add ReflectConstruct to known perf issues (Vinicius Lourenço) [#50111](https://github.com/nodejs/node/pull/50111)
* \[[`ffa94612fd`](https://github.com/nodejs/node/commit/ffa94612fd)] - **doc**: fix typo in dgram docs (Peter Johnson) [#50211](https://github.com/nodejs/node/pull/50211)
* \[[`f37b577b14`](https://github.com/nodejs/node/commit/f37b577b14)] - **doc**: fix H4ad collaborator sort (Vinicius Lourenço) [#50218](https://github.com/nodejs/node/pull/50218)
* \[[`c75264b1f9`](https://github.com/nodejs/node/commit/c75264b1f9)] - **doc**: add H4ad to collaborators (Vinícius Lourenço) [#50217](https://github.com/nodejs/node/pull/50217)
* \[[`5025e24ac7`](https://github.com/nodejs/node/commit/5025e24ac7)] - **doc**: update release-stewards with last sec-release (Rafael Gonzaga) [#50179](https://github.com/nodejs/node/pull/50179)
* \[[`63379313d5`](https://github.com/nodejs/node/commit/63379313d5)] - **doc**: add command to keep major branch sync (Rafael Gonzaga) [#50102](https://github.com/nodejs/node/pull/50102)
* \[[`85de4b8254`](https://github.com/nodejs/node/commit/85de4b8254)] - **doc**: add loong64 to list of architectures (Shi Pujin) [#50172](https://github.com/nodejs/node/pull/50172)
* \[[`ff8e1b860e`](https://github.com/nodejs/node/commit/ff8e1b860e)] - **doc**: update security release process (Michael Dawson) [#50166](https://github.com/nodejs/node/pull/50166)
* \[[`33470d965c`](https://github.com/nodejs/node/commit/33470d965c)] - **doc**: improve ccache explanation (Chengzhong Wu) [#50133](https://github.com/nodejs/node/pull/50133)
* \[[`7b97c44e2a`](https://github.com/nodejs/node/commit/7b97c44e2a)] - **doc**: move danielleadams to TSC non-voting member (Danielle Adams) [#50142](https://github.com/nodejs/node/pull/50142)
* \[[`3d03ca9f31`](https://github.com/nodejs/node/commit/3d03ca9f31)] - **doc**: fix description of `fs.readdir` `recursive` option (RamdohokarAngha) [#48902](https://github.com/nodejs/node/pull/48902)
* \[[`aab045ec4b`](https://github.com/nodejs/node/commit/aab045ec4b)] - **doc**: mention files read before env setup (Rafael Gonzaga) [#50072](https://github.com/nodejs/node/pull/50072)
* \[[`26a7608a24`](https://github.com/nodejs/node/commit/26a7608a24)] - **doc**: move permission model to Active Development (Rafael Gonzaga) [#50068](https://github.com/nodejs/node/pull/50068)
* \[[`d7bbf7f2c4`](https://github.com/nodejs/node/commit/d7bbf7f2c4)] - **doc**: add command to get patch minors and majors (Rafael Gonzaga) [#50067](https://github.com/nodejs/node/pull/50067)
* \[[`9830165e34`](https://github.com/nodejs/node/commit/9830165e34)] - **doc**: use precise promise terminology in fs (Benjamin Gruenbaum) [#50029](https://github.com/nodejs/node/pull/50029)
* \[[`585cbb211d`](https://github.com/nodejs/node/commit/585cbb211d)] - **doc**: use precise terminology in test runner (Benjamin Gruenbaum) [#50028](https://github.com/nodejs/node/pull/50028)
* \[[`2862f07124`](https://github.com/nodejs/node/commit/2862f07124)] - **doc**: clarify explaination text on how to run the example (Anshul Sinha) [#39020](https://github.com/nodejs/node/pull/39020)
* \[[`fe47c8ad91`](https://github.com/nodejs/node/commit/fe47c8ad91)] - **doc**: reserve 119 for Electron 28 (David Sanders) [#50020](https://github.com/nodejs/node/pull/50020)
* \[[`36ecd2c588`](https://github.com/nodejs/node/commit/36ecd2c588)] - **doc**: update Collaborator pronouns (Tierney Cyren) [#50005](https://github.com/nodejs/node/pull/50005)
* \[[`315d82a73e`](https://github.com/nodejs/node/commit/315d82a73e)] - **doc**: update link to Abstract Modules Records spec (Rich Trott) [#49961](https://github.com/nodejs/node/pull/49961)
* \[[`f63a92bb6c`](https://github.com/nodejs/node/commit/f63a92bb6c)] - **doc**: updated building docs for windows (Claudio W) [#49767](https://github.com/nodejs/node/pull/49767)
* \[[`ad17126501`](https://github.com/nodejs/node/commit/ad17126501)] - **doc**: update CHANGELOG\_V20 about vm fixes (Joyee Cheung) [#49951](https://github.com/nodejs/node/pull/49951)
* \[[`24458e2ac3`](https://github.com/nodejs/node/commit/24458e2ac3)] - **doc**: document dangerous symlink behavior (Tobias Nießen) [#49154](https://github.com/nodejs/node/pull/49154)
* \[[`337a676d1f`](https://github.com/nodejs/node/commit/337a676d1f)] - **doc**: add main ARIA landmark to API docs (Rich Trott) [#49882](https://github.com/nodejs/node/pull/49882)
* \[[`959ef7ac6b`](https://github.com/nodejs/node/commit/959ef7ac6b)] - **doc**: add navigation ARIA landmark to doc ToC (Rich Trott) [#49882](https://github.com/nodejs/node/pull/49882)
* \[[`a60fbf2ab3`](https://github.com/nodejs/node/commit/a60fbf2ab3)] - **doc**: mark Node.js 19 as End-of-Life (Richard Lau) [#48283](https://github.com/nodejs/node/pull/48283)
* \[[`c255575699`](https://github.com/nodejs/node/commit/c255575699)] - **errors**: improve performance of determine-specific-type (Aras Abbasi) [#49696](https://github.com/nodejs/node/pull/49696)
* \[[`e66991e6b2`](https://github.com/nodejs/node/commit/e66991e6b2)] - **errors**: improve formatList in errors.js (Aras Abbasi) [#49642](https://github.com/nodejs/node/pull/49642)
* \[[`c71e548b65`](https://github.com/nodejs/node/commit/c71e548b65)] - **errors**: improve performance of instantiation (Aras Abbasi) [#49654](https://github.com/nodejs/node/pull/49654)
* \[[`3b867e4256`](https://github.com/nodejs/node/commit/3b867e4256)] - **esm**: do not give wrong hints when detecting file format (Antoine du Hamel) [#50314](https://github.com/nodejs/node/pull/50314)
* \[[`a589a1a905`](https://github.com/nodejs/node/commit/a589a1a905)] - **(SEMVER-MINOR)** **esm**: detect ESM syntax in ambiguous JavaScript (Geoffrey Booth) [#50096](https://github.com/nodejs/node/pull/50096)
* \[[`c64490e9aa`](https://github.com/nodejs/node/commit/c64490e9aa)] - **esm**: improve check for ESM syntax (Geoffrey Booth) [#50127](https://github.com/nodejs/node/pull/50127)
* \[[`ee65e44c31`](https://github.com/nodejs/node/commit/ee65e44c31)] - **esm**: use import attributes instead of import assertions (Antoine du Hamel) [#50140](https://github.com/nodejs/node/pull/50140)
* \[[`4de838fdeb`](https://github.com/nodejs/node/commit/4de838fdeb)] - **esm**: bypass CommonJS loader under --default-type (Geoffrey Booth) [#49986](https://github.com/nodejs/node/pull/49986)
* \[[`27e02b633d`](https://github.com/nodejs/node/commit/27e02b633d)] - **esm**: unflag extensionless javascript and wasm in module scope (Geoffrey Booth) [#49974](https://github.com/nodejs/node/pull/49974)
* \[[`1e762ddf63`](https://github.com/nodejs/node/commit/1e762ddf63)] - **esm**: improve `getFormatOfExtensionlessFile` speed (Yagiz Nizipli) [#49965](https://github.com/nodejs/node/pull/49965)
* \[[`112cc7f9f2`](https://github.com/nodejs/node/commit/112cc7f9f2)] - **esm**: improve JSDoc annotation of internal functions (Antoine du Hamel) [#49959](https://github.com/nodejs/node/pull/49959)
* \[[`c48cd84188`](https://github.com/nodejs/node/commit/c48cd84188)] - **esm**: fix cache collision on JSON files using file: URL (Antoine du Hamel) [#49887](https://github.com/nodejs/node/pull/49887)
* \[[`dc80ccef25`](https://github.com/nodejs/node/commit/dc80ccef25)] - **esm**: --experimental-default-type flag to flip module defaults (Geoffrey Booth) [#49869](https://github.com/nodejs/node/pull/49869)
* \[[`01039795a2`](https://github.com/nodejs/node/commit/01039795a2)] - **esm**: require braces for modules code (Geoffrey Booth) [#49657](https://github.com/nodejs/node/pull/49657)
* \[[`e49ebf8f9a`](https://github.com/nodejs/node/commit/e49ebf8f9a)] - **fs**: improve error performance for `readSync` (Jungku Lee) [#50033](https://github.com/nodejs/node/pull/50033)
* \[[`eb33f70260`](https://github.com/nodejs/node/commit/eb33f70260)] - **fs**: improve error performance for `fsyncSync` (Jungku Lee) [#49880](https://github.com/nodejs/node/pull/49880)
* \[[`8d0edc6399`](https://github.com/nodejs/node/commit/8d0edc6399)] - **fs**: improve error performance for `mkdirSync` (CanadaHonk) [#49847](https://github.com/nodejs/node/pull/49847)
* \[[`36df27509e`](https://github.com/nodejs/node/commit/36df27509e)] - **fs**: improve error performance of `realpathSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`4242cb7d7f`](https://github.com/nodejs/node/commit/4242cb7d7f)] - **fs**: improve error performance of `lchownSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`89e7878e44`](https://github.com/nodejs/node/commit/89e7878e44)] - **fs**: improve error performance of `symlinkSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`af6a0611fe`](https://github.com/nodejs/node/commit/af6a0611fe)] - **fs**: improve error performance of `readlinkSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`12cda31c52`](https://github.com/nodejs/node/commit/12cda31c52)] - **fs**: improve error performance of `mkdtempSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`9dba771acb`](https://github.com/nodejs/node/commit/9dba771acb)] - **fs**: improve error performance of `linkSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`ea7902de13`](https://github.com/nodejs/node/commit/ea7902de13)] - **fs**: improve error performance of `chownSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`39f31a38cf`](https://github.com/nodejs/node/commit/39f31a38cf)] - **fs**: improve error performance of `renameSync` (Yagiz Nizipli) [#49962](https://github.com/nodejs/node/pull/49962)
* \[[`35164fa466`](https://github.com/nodejs/node/commit/35164fa466)] - **(SEMVER-MINOR)** **fs**: add flush option to appendFile() functions (Colin Ihrig) [#50095](https://github.com/nodejs/node/pull/50095)
* \[[`06aa4b9fe9`](https://github.com/nodejs/node/commit/06aa4b9fe9)] - **fs**: improve error performance of `readdirSync` (Yagiz Nizipli) [#50131](https://github.com/nodejs/node/pull/50131)
* \[[`b5aecebcd6`](https://github.com/nodejs/node/commit/b5aecebcd6)] - **fs**: fix `unlinkSync` typings (Yagiz Nizipli) [#49859](https://github.com/nodejs/node/pull/49859)
* \[[`6ddea07225`](https://github.com/nodejs/node/commit/6ddea07225)] - **fs**: improve error perf of sync `chmod`+`fchmod` (CanadaHonk) [#49859](https://github.com/nodejs/node/pull/49859)
* \[[`841367078e`](https://github.com/nodejs/node/commit/841367078e)] - **fs**: improve error perf of sync `*times` (CanadaHonk) [#49864](https://github.com/nodejs/node/pull/49864)
* \[[`eb52f73e3e`](https://github.com/nodejs/node/commit/eb52f73e3e)] - **fs**: improve error performance of writevSync (IlyasShabi) [#50038](https://github.com/nodejs/node/pull/50038)
* \[[`d1aa62f1f5`](https://github.com/nodejs/node/commit/d1aa62f1f5)] - **fs**: add flush option to createWriteStream() (Colin Ihrig) [#50093](https://github.com/nodejs/node/pull/50093)
* \[[`57eb06edff`](https://github.com/nodejs/node/commit/57eb06edff)] - **fs**: improve error performance for `ftruncateSync` (André Alves) [#50032](https://github.com/nodejs/node/pull/50032)
* \[[`22e3eb659a`](https://github.com/nodejs/node/commit/22e3eb659a)] - **fs**: add flush option to writeFile() functions (Colin Ihrig) [#50009](https://github.com/nodejs/node/pull/50009)
* \[[`d7132d9214`](https://github.com/nodejs/node/commit/d7132d9214)] - **fs**: improve error performance for `fdatasyncSync` (Jungku Lee) [#49898](https://github.com/nodejs/node/pull/49898)
* \[[`bc2c0410d3`](https://github.com/nodejs/node/commit/bc2c0410d3)] - **fs**: throw errors from sync branches instead of separate implementations (Joyee Cheung) [#49913](https://github.com/nodejs/node/pull/49913)
* \[[`f46bcf1749`](https://github.com/nodejs/node/commit/f46bcf1749)] - **http**: refactor to make servername option normalization testable (Rongjian Zhang) [#38733](https://github.com/nodejs/node/pull/38733)
* \[[`1bfcf817af`](https://github.com/nodejs/node/commit/1bfcf817af)] - **http2**: allow streams to complete gracefully after goaway (Michael Lumish) [#50202](https://github.com/nodejs/node/pull/50202)
* \[[`5c66ec9e66`](https://github.com/nodejs/node/commit/5c66ec9e66)] - **inspector**: simplify dispatchProtocolMessage (Daniel Lemire) [#49780](https://github.com/nodejs/node/pull/49780)
* \[[`251ae1dd72`](https://github.com/nodejs/node/commit/251ae1dd72)] - **lib**: improve performance of validateStringArray and validateBooleanArray (Aras Abbasi) [#49756](https://github.com/nodejs/node/pull/49756)
* \[[`d9c791a508`](https://github.com/nodejs/node/commit/d9c791a508)] - **lib**: fix compileFunction throws range error for negative numbers (Jithil P Ponnan) [#49855](https://github.com/nodejs/node/pull/49855)
* \[[`24cbc550c2`](https://github.com/nodejs/node/commit/24cbc550c2)] - **lib**: reduce overhead of validateObject (Vinicius Lourenço) [#49928](https://github.com/nodejs/node/pull/49928)
* \[[`b80e9497f3`](https://github.com/nodejs/node/commit/b80e9497f3)] - **lib**: make fetch sync and return a Promise (Matthew Aitken) [#49936](https://github.com/nodejs/node/pull/49936)
* \[[`d9eda6761b`](https://github.com/nodejs/node/commit/d9eda6761b)] - **lib**: fix `primordials` typings (Sam Verschueren) [#49895](https://github.com/nodejs/node/pull/49895)
* \[[`3e0d47c1f4`](https://github.com/nodejs/node/commit/3e0d47c1f4)] - **lib**: update params in jsdoc for `HTTPRequestOptions` (Jungku Lee) [#49872](https://github.com/nodejs/node/pull/49872)
* \[[`a01050dec4`](https://github.com/nodejs/node/commit/a01050dec4)] - **(SEMVER-MINOR)** **lib**: add WebSocket client (Matthew Aitken) [#49830](https://github.com/nodejs/node/pull/49830)
* \[[`5bca8feed2`](https://github.com/nodejs/node/commit/5bca8feed2)] - **lib,test**: do not hardcode Buffer.kMaxLength (Michaël Zasso) [#49876](https://github.com/nodejs/node/pull/49876)
* \[[`e8ebed7a24`](https://github.com/nodejs/node/commit/e8ebed7a24)] - **meta**: move Trott to TSC regular member (Rich Trott) [#50297](https://github.com/nodejs/node/pull/50297)
* \[[`27e957cea8`](https://github.com/nodejs/node/commit/27e957cea8)] - **meta**: ping TSC for offboarding (Tobias Nießen) [#50147](https://github.com/nodejs/node/pull/50147)
* \[[`fab39062d5`](https://github.com/nodejs/node/commit/fab39062d5)] - **meta**: bump actions/upload-artifact from 3.1.2 to 3.1.3 (dependabot\[bot]) [#50000](https://github.com/nodejs/node/pull/50000)
* \[[`46ec82496c`](https://github.com/nodejs/node/commit/46ec82496c)] - **meta**: bump actions/cache from 3.3.1 to 3.3.2 (dependabot\[bot]) [#50003](https://github.com/nodejs/node/pull/50003)
* \[[`a634fb431e`](https://github.com/nodejs/node/commit/a634fb431e)] - **meta**: bump github/codeql-action from 2.21.5 to 2.21.9 (dependabot\[bot]) [#50002](https://github.com/nodejs/node/pull/50002)
* \[[`c221f72911`](https://github.com/nodejs/node/commit/c221f72911)] - **meta**: bump actions/checkout from 3.6.0 to 4.1.0 (dependabot\[bot]) [#50001](https://github.com/nodejs/node/pull/50001)
* \[[`d356e5e395`](https://github.com/nodejs/node/commit/d356e5e395)] - **meta**: update website team with new name (Rich Trott) [#49883](https://github.com/nodejs/node/pull/49883)
* \[[`2ff4e71452`](https://github.com/nodejs/node/commit/2ff4e71452)] - **module**: move helpers out of cjs loader (Geoffrey Booth) [#49912](https://github.com/nodejs/node/pull/49912)
* \[[`142ac3f82d`](https://github.com/nodejs/node/commit/142ac3f82d)] - **module, esm**: jsdoc for modules files (Geoffrey Booth) [#49523](https://github.com/nodejs/node/pull/49523)
* \[[`e2f0ef2a60`](https://github.com/nodejs/node/commit/e2f0ef2a60)] - **node-api**: update headers for better wasm support (Toyo Li) [#49037](https://github.com/nodejs/node/pull/49037)
* \[[`db2a07fcd6`](https://github.com/nodejs/node/commit/db2a07fcd6)] - **node-api**: run finalizers directly from GC (Vladimir Morozov) [#42651](https://github.com/nodejs/node/pull/42651)
* \[[`c25716be8b`](https://github.com/nodejs/node/commit/c25716be8b)] - **os**: cache homedir, remove getCheckedFunction (Aras Abbasi) [#50037](https://github.com/nodejs/node/pull/50037)
* \[[`e8f024b4db`](https://github.com/nodejs/node/commit/e8f024b4db)] - **perf\_hooks**: reduce overhead of new user timings (Vinicius Lourenço) [#49914](https://github.com/nodejs/node/pull/49914)
* \[[`a517be0a5a`](https://github.com/nodejs/node/commit/a517be0a5a)] - **perf\_hooks**: reducing overhead of performance observer entry list (Vinicius Lourenço) [#50008](https://github.com/nodejs/node/pull/50008)
* \[[`42e49ec381`](https://github.com/nodejs/node/commit/42e49ec381)] - **perf\_hooks**: reduce overhead of new resource timings (Vinicius Lourenço) [#49837](https://github.com/nodejs/node/pull/49837)
* \[[`c99e51ed1b`](https://github.com/nodejs/node/commit/c99e51ed1b)] - **src**: generate default snapshot with --predictable (Joyee Cheung) [#48749](https://github.com/nodejs/node/pull/48749)
* \[[`47164e238f`](https://github.com/nodejs/node/commit/47164e238f)] - **src**: fix TLSWrap lifetime bug in ALPN callback (Ben Noordhuis) [#49635](https://github.com/nodejs/node/pull/49635)
* \[[`e1df69e73e`](https://github.com/nodejs/node/commit/e1df69e73e)] - **src**: set port in node\_options to uint16\_t (Yagiz Nizipli) [#49151](https://github.com/nodejs/node/pull/49151)
* \[[`1eb2af29b4`](https://github.com/nodejs/node/commit/1eb2af29b4)] - **src**: name scoped lock (Mohammed Keyvanzadeh) [#50010](https://github.com/nodejs/node/pull/50010)
* \[[`5131fde655`](https://github.com/nodejs/node/commit/5131fde655)] - **src**: use exact return value for `uv_os_getenv` (Yagiz Nizipli) [#49149](https://github.com/nodejs/node/pull/49149)
* \[[`ba169be5ca`](https://github.com/nodejs/node/commit/ba169be5ca)] - **src**: move const variable in `node_file.h` to `node_file.cc` (Jungku Lee) [#49688](https://github.com/nodejs/node/pull/49688)
* \[[`5a2351d3ab`](https://github.com/nodejs/node/commit/5a2351d3ab)] - **src**: remove unused variable (Michaël Zasso) [#49665](https://github.com/nodejs/node/pull/49665)
* \[[`f2f993a32f`](https://github.com/nodejs/node/commit/f2f993a32f)] - **stream**: simplify prefinish (Robert Nagy) [#50204](https://github.com/nodejs/node/pull/50204)
* \[[`6d7274e3ca`](https://github.com/nodejs/node/commit/6d7274e3ca)] - **stream**: reduce scope of readable bitmap details (Robert Nagy) [#49963](https://github.com/nodejs/node/pull/49963)
* \[[`ffdc357167`](https://github.com/nodejs/node/commit/ffdc357167)] - **(SEMVER-MINOR)** **stream**: allow pass stream class to `stream.compose` (Alex Yang) [#50187](https://github.com/nodejs/node/pull/50187)
* \[[`4861ad6431`](https://github.com/nodejs/node/commit/4861ad6431)] - **stream**: call helper function from push and unshift (Raz Luvaton) [#50173](https://github.com/nodejs/node/pull/50173)
* \[[`e60b3ab31b`](https://github.com/nodejs/node/commit/e60b3ab31b)] - **stream**: use private symbol for bitmap state (Robert Nagy) [#49993](https://github.com/nodejs/node/pull/49993)
* \[[`ecbfb23f6b`](https://github.com/nodejs/node/commit/ecbfb23f6b)] - **stream**: lazy allocate back pressure buffer (Robert Nagy) [#50013](https://github.com/nodejs/node/pull/50013)
* \[[`88c739bef4`](https://github.com/nodejs/node/commit/88c739bef4)] - **stream**: avoid unnecessary drain for sync stream (Robert Nagy) [#50014](https://github.com/nodejs/node/pull/50014)
* \[[`4b27087b30`](https://github.com/nodejs/node/commit/4b27087b30)] - **stream**: optimize Writable (Robert Nagy) [#50012](https://github.com/nodejs/node/pull/50012)
* \[[`def55f80a1`](https://github.com/nodejs/node/commit/def55f80a1)] - **stream**: avoid tick in writable hot path (Robert Nagy) [#49966](https://github.com/nodejs/node/pull/49966)
* \[[`35ec93115d`](https://github.com/nodejs/node/commit/35ec93115d)] - **stream**: writable state bitmap (Robert Nagy) [#49899](https://github.com/nodejs/node/pull/49899)
* \[[`6e0f0fafe4`](https://github.com/nodejs/node/commit/6e0f0fafe4)] - **test**: use ppc and ppc64 to skip SEA tests on PowerPC (Joyee Cheung) [#50828](https://github.com/nodejs/node/pull/50828)
* \[[`a528bbceca`](https://github.com/nodejs/node/commit/a528bbceca)] - **test**: mark SEA tests as flaky on PowerPC (Joyee Cheung) [#50750](https://github.com/nodejs/node/pull/50750)
* \[[`4e34f9a26e`](https://github.com/nodejs/node/commit/4e34f9a26e)] - **test**: relax version check with shared OpenSSL (Luigi Pinca) [#50505](https://github.com/nodejs/node/pull/50505)
* \[[`41ca1132eb`](https://github.com/nodejs/node/commit/41ca1132eb)] - **test**: fix crypto-dh error message for OpenSSL 3.x (Kerem Kat) [#50395](https://github.com/nodejs/node/pull/50395)
* \[[`a6a05e8a88`](https://github.com/nodejs/node/commit/a6a05e8a88)] - **test**: fix testsuite against zlib version 1.3 (Dominique Leuenberger) [#50364](https://github.com/nodejs/node/pull/50364)
* \[[`8dd895e574`](https://github.com/nodejs/node/commit/8dd895e574)] - **test**: replace forEach with for..of in test-process-env (Niya Shiyas) [#49825](https://github.com/nodejs/node/pull/49825)
* \[[`81886c66d1`](https://github.com/nodejs/node/commit/81886c66d1)] - **test**: replace forEach with for..of in test-http-url (Niya Shiyas) [#49840](https://github.com/nodejs/node/pull/49840)
* \[[`7d8a18b257`](https://github.com/nodejs/node/commit/7d8a18b257)] - **test**: improve watch mode test (Moshe Atlow) [#50319](https://github.com/nodejs/node/pull/50319)
* \[[`baa04b79ca`](https://github.com/nodejs/node/commit/baa04b79ca)] - **test**: set `test-watch-mode-inspect` as flaky (Yagiz Nizipli) [#50259](https://github.com/nodejs/node/pull/50259)
* \[[`3d9130bc2e`](https://github.com/nodejs/node/commit/3d9130bc2e)] - _**Revert**_ "**test**: set `test-esm-loader-resolve-type` as flaky" (Antoine du Hamel) [#50315](https://github.com/nodejs/node/pull/50315)
* \[[`72626f9a35`](https://github.com/nodejs/node/commit/72626f9a35)] - **test**: replace forEach with for..of in test-http-perf\_hooks.js (Niya Shiyas) [#49818](https://github.com/nodejs/node/pull/49818)
* \[[`379a7255e8`](https://github.com/nodejs/node/commit/379a7255e8)] - **test**: replace forEach with for..of in test-net-isipv4.js (Niya Shiyas) [#49822](https://github.com/nodejs/node/pull/49822)
* \[[`b55fcd75da`](https://github.com/nodejs/node/commit/b55fcd75da)] - **test**: deflake `test-esm-loader-resolve-type` (Antoine du Hamel) [#50273](https://github.com/nodejs/node/pull/50273)
* \[[`0134af3eeb`](https://github.com/nodejs/node/commit/0134af3eeb)] - **test**: replace forEach with for..of in test-http2-server (Niya Shiyas) [#49819](https://github.com/nodejs/node/pull/49819)
* \[[`8c15281d06`](https://github.com/nodejs/node/commit/8c15281d06)] - **test**: replace forEach with for..of in test-http2-client-destroy.js (Niya Shiyas) [#49820](https://github.com/nodejs/node/pull/49820)
* \[[`c37a75a898`](https://github.com/nodejs/node/commit/c37a75a898)] - **test**: update `url` web platform tests (Yagiz Nizipli) [#50264](https://github.com/nodejs/node/pull/50264)
* \[[`ab5985d0e9`](https://github.com/nodejs/node/commit/ab5985d0e9)] - **test**: set `test-emit-after-on-destroyed` as flaky (Yagiz Nizipli) [#50246](https://github.com/nodejs/node/pull/50246)
* \[[`50181a19b8`](https://github.com/nodejs/node/commit/50181a19b8)] - **test**: set inspector async stack test as flaky (Yagiz Nizipli) [#50244](https://github.com/nodejs/node/pull/50244)
* \[[`b9e0fed995`](https://github.com/nodejs/node/commit/b9e0fed995)] - **test**: set test-worker-nearheaplimit-deadlock flaky (StefanStojanovic) [#50277](https://github.com/nodejs/node/pull/50277)
* \[[`2cfc4007d1`](https://github.com/nodejs/node/commit/2cfc4007d1)] - **test**: set `test-cli-node-options` as flaky (Yagiz Nizipli) [#50296](https://github.com/nodejs/node/pull/50296)
* \[[`788714b28f`](https://github.com/nodejs/node/commit/788714b28f)] - **test**: reduce the number of requests and parsers (Luigi Pinca) [#50240](https://github.com/nodejs/node/pull/50240)
* \[[`0dce19c8f6`](https://github.com/nodejs/node/commit/0dce19c8f6)] - **test**: set crypto-timing test as flaky (Yagiz Nizipli) [#50232](https://github.com/nodejs/node/pull/50232)
* \[[`5d4b5ff1b8`](https://github.com/nodejs/node/commit/5d4b5ff1b8)] - **test**: set `test-structuredclone-*` as flaky (Yagiz Nizipli) [#50261](https://github.com/nodejs/node/pull/50261)
* \[[`5c56081d67`](https://github.com/nodejs/node/commit/5c56081d67)] - **test**: deflake `test-loaders-workers-spawned` (Antoine du Hamel) [#50251](https://github.com/nodejs/node/pull/50251)
* \[[`3441e1982d`](https://github.com/nodejs/node/commit/3441e1982d)] - **test**: improve code coverage of diagnostics\_channel (Jithil P Ponnan) [#50053](https://github.com/nodejs/node/pull/50053)
* \[[`696ba93329`](https://github.com/nodejs/node/commit/696ba93329)] - **test**: set `test-esm-loader-resolve-type` as flaky (Yagiz Nizipli) [#50226](https://github.com/nodejs/node/pull/50226)
* \[[`8b260c5d6b`](https://github.com/nodejs/node/commit/8b260c5d6b)] - **test**: set inspector async hook test as flaky (Yagiz Nizipli) [#50252](https://github.com/nodejs/node/pull/50252)
* \[[`f3296d25e8`](https://github.com/nodejs/node/commit/f3296d25e8)] - **test**: skip test-benchmark-os.js on IBM i (Abdirahim Musse) [#50208](https://github.com/nodejs/node/pull/50208)
* \[[`fefe17b02e`](https://github.com/nodejs/node/commit/fefe17b02e)] - **test**: set parallel http server test as flaky (Yagiz Nizipli) [#50227](https://github.com/nodejs/node/pull/50227)
* \[[`228c87f329`](https://github.com/nodejs/node/commit/228c87f329)] - **test**: set test-worker-nearheaplimit-deadlock flaky (Stefan Stojanovic) [#50238](https://github.com/nodejs/node/pull/50238)
* \[[`c2c2506eab`](https://github.com/nodejs/node/commit/c2c2506eab)] - **test**: set `test-runner-watch-mode` as flaky (Yagiz Nizipli) [#50221](https://github.com/nodejs/node/pull/50221)
* \[[`16a07983d4`](https://github.com/nodejs/node/commit/16a07983d4)] - **test**: set sea snapshot tests as flaky (Yagiz Nizipli) [#50223](https://github.com/nodejs/node/pull/50223)
* \[[`7cd406a0b8`](https://github.com/nodejs/node/commit/7cd406a0b8)] - **test**: fix defect path traversal tests (Tobias Nießen) [#50124](https://github.com/nodejs/node/pull/50124)
* \[[`1cf3f8da32`](https://github.com/nodejs/node/commit/1cf3f8da32)] - **test**: replace forEach with for..of in test-net-isipv6.js (Niya Shiyas) [#49823](https://github.com/nodejs/node/pull/49823)
* \[[`214997a99e`](https://github.com/nodejs/node/commit/214997a99e)] - **test**: reduce number of repetition in test-heapdump-shadowrealm.js (Chengzhong Wu) [#50104](https://github.com/nodejs/node/pull/50104)
* \[[`9d836516e6`](https://github.com/nodejs/node/commit/9d836516e6)] - **test**: replace forEach with for..of in test-parse-args.mjs (Niya Shiyas) [#49824](https://github.com/nodejs/node/pull/49824)
* \[[`fee8b24603`](https://github.com/nodejs/node/commit/fee8b24603)] - **test**: replace forEach() in test-net-perf\_hooks with for of (Narcisa Codreanu) [#49831](https://github.com/nodejs/node/pull/49831)
* \[[`4c58b92ba8`](https://github.com/nodejs/node/commit/4c58b92ba8)] - **test**: change forEach to for...of (Tiffany Lastimosa) [#49799](https://github.com/nodejs/node/pull/49799)
* \[[`01b01527d7`](https://github.com/nodejs/node/commit/01b01527d7)] - **test**: update skip for moved `test-wasm-web-api` (Richard Lau) [#49958](https://github.com/nodejs/node/pull/49958)
* \[[`179e293103`](https://github.com/nodejs/node/commit/179e293103)] - _**Revert**_ "**test**: mark test-runner-output as flaky" (Luigi Pinca) [#49905](https://github.com/nodejs/node/pull/49905)
* \[[`829eb99afd`](https://github.com/nodejs/node/commit/829eb99afd)] - **test**: disambiguate AIX and IBM i (Richard Lau) [#48056](https://github.com/nodejs/node/pull/48056)
* \[[`126407d438`](https://github.com/nodejs/node/commit/126407d438)] - **test**: deflake test-perf-hooks.js (Joyee Cheung) [#49892](https://github.com/nodejs/node/pull/49892)
* \[[`393fd5b7c9`](https://github.com/nodejs/node/commit/393fd5b7c9)] - **test**: migrate message error tests from Python to JS (Yiyun Lei) [#49721](https://github.com/nodejs/node/pull/49721)
* \[[`5dfe4236f8`](https://github.com/nodejs/node/commit/5dfe4236f8)] - **test**: fix edge snapshot stack traces (Geoffrey Booth) [#49659](https://github.com/nodejs/node/pull/49659)
* \[[`d164e537bf`](https://github.com/nodejs/node/commit/d164e537bf)] - **test,crypto**: update WebCryptoAPI WPT (Filip Skokan) [#50039](https://github.com/nodejs/node/pull/50039)
* \[[`55a03fedae`](https://github.com/nodejs/node/commit/55a03fedae)] - **test\_runner**: add test location for FileTests (Colin Ihrig) [#49999](https://github.com/nodejs/node/pull/49999)
* \[[`10b35cfb6e`](https://github.com/nodejs/node/commit/10b35cfb6e)] - **test\_runner**: replace spurious if with else (Colin Ihrig) [#49943](https://github.com/nodejs/node/pull/49943)
* \[[`27558c4314`](https://github.com/nodejs/node/commit/27558c4314)] - **test\_runner**: catch reporter errors (Moshe Atlow) [#49646](https://github.com/nodejs/node/pull/49646)
* \[[`709c6c0cab`](https://github.com/nodejs/node/commit/709c6c0cab)] - **(SEMVER-MINOR)** **test\_runner, cli**: add --test-concurrency flag (Colin Ihrig) [#49996](https://github.com/nodejs/node/pull/49996)
* \[[`64ef108dd9`](https://github.com/nodejs/node/commit/64ef108dd9)] - **test\_runner,test**: fix flaky test-runner-cli-concurrency.js (Colin Ihrig) [#50108](https://github.com/nodejs/node/pull/50108)
* \[[`d2def152d9`](https://github.com/nodejs/node/commit/d2def152d9)] - **tls**: reduce TLS 'close' event listener warnings (Tim Perry) [#50136](https://github.com/nodejs/node/pull/50136)
* \[[`294b650f5c`](https://github.com/nodejs/node/commit/294b650f5c)] - **tls**: handle cases where the raw socket is destroyed (Luigi Pinca) [#49980](https://github.com/nodejs/node/pull/49980)
* \[[`52b5693949`](https://github.com/nodejs/node/commit/52b5693949)] - **tls**: ciphers allow bang syntax (Chemi Atlow) [#49712](https://github.com/nodejs/node/pull/49712)
* \[[`05ee35028b`](https://github.com/nodejs/node/commit/05ee35028b)] - **tools**: update comment in `update-uncidi.sh` and `acorn_version.h` (Jungku Lee) [#50175](https://github.com/nodejs/node/pull/50175)
* \[[`35b160e6a3`](https://github.com/nodejs/node/commit/35b160e6a3)] - **tools**: refactor checkimports.py (Mohammed Keyvanzadeh) [#50011](https://github.com/nodejs/node/pull/50011)
* \[[`b959d36b77`](https://github.com/nodejs/node/commit/b959d36b77)] - **tools**: fix comments referencing dep\_updaters scripts (Keksonoid) [#50165](https://github.com/nodejs/node/pull/50165)
* \[[`bd5d5331b0`](https://github.com/nodejs/node/commit/bd5d5331b0)] - **tools**: remove no-return-await lint rule (翠 / green) [#50118](https://github.com/nodejs/node/pull/50118)
* \[[`b9adf3d66e`](https://github.com/nodejs/node/commit/b9adf3d66e)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#50083](https://github.com/nodejs/node/pull/50083)
* \[[`4978bdc4ec`](https://github.com/nodejs/node/commit/4978bdc4ec)] - **tools**: update eslint to 8.51.0 (Node.js GitHub Bot) [#50084](https://github.com/nodejs/node/pull/50084)
* \[[`e323a367fd`](https://github.com/nodejs/node/commit/e323a367fd)] - **tools**: remove genv8constants.py (Ben Noordhuis) [#50023](https://github.com/nodejs/node/pull/50023)
* \[[`1cc6cbff26`](https://github.com/nodejs/node/commit/1cc6cbff26)] - **tools**: update eslint to 8.50.0 (Node.js GitHub Bot) [#49989](https://github.com/nodejs/node/pull/49989)
* \[[`924231be2a`](https://github.com/nodejs/node/commit/924231be2a)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#49983](https://github.com/nodejs/node/pull/49983)
* \[[`732b5661ea`](https://github.com/nodejs/node/commit/732b5661ea)] - **tools**: add navigation ARIA landmark to generated API ToC (Rich Trott) [#49882](https://github.com/nodejs/node/pull/49882)
* \[[`353a14278e`](https://github.com/nodejs/node/commit/353a14278e)] - **tools**: update github\_reporter to 1.5.3 (Node.js GitHub Bot) [#49877](https://github.com/nodejs/node/pull/49877)
* \[[`0aaab45d7c`](https://github.com/nodejs/node/commit/0aaab45d7c)] - **tools**: improve macOS notarization process output readability (Ulises Gascón) [#50389](https://github.com/nodejs/node/pull/50389)
* \[[`ad326033e2`](https://github.com/nodejs/node/commit/ad326033e2)] - **tools**: remove unused `version` function (Ulises Gascón) [#50390](https://github.com/nodejs/node/pull/50390)
* \[[`2f32472544`](https://github.com/nodejs/node/commit/2f32472544)] - **tools**: drop support for osx notarization with gon (Ulises Gascón) [#50291](https://github.com/nodejs/node/pull/50291)
* \[[`3b1c15aeb0`](https://github.com/nodejs/node/commit/3b1c15aeb0)] - **tools**: use osx notarytool for future releases (Ulises Gascon) [#48701](https://github.com/nodejs/node/pull/48701)
* \[[`0ddb87ede3`](https://github.com/nodejs/node/commit/0ddb87ede3)] - **typings**: use `Symbol.dispose` and `Symbol.asyncDispose` in types (Niklas Mollenhauer) [#50123](https://github.com/nodejs/node/pull/50123)
* \[[`bf5b2115a0`](https://github.com/nodejs/node/commit/bf5b2115a0)] - **util**: remove internal mime fns from benchmarks (Aras Abbasi) [#50201](https://github.com/nodejs/node/pull/50201)
* \[[`ac02cdb0ad`](https://github.com/nodejs/node/commit/ac02cdb0ad)] - **util**: lazy parse mime parameters (Aras Abbasi) [#49889](https://github.com/nodejs/node/pull/49889)
* \[[`9853fd96df`](https://github.com/nodejs/node/commit/9853fd96df)] - **vm**: reject in importModuleDynamically without --experimental-vm-modules (Joyee Cheung) [#50137](https://github.com/nodejs/node/pull/50137)
* \[[`3697c19c80`](https://github.com/nodejs/node/commit/3697c19c80)] - **vm**: use internal versions of compileFunction and Script (Joyee Cheung) [#50137](https://github.com/nodejs/node/pull/50137)
* \[[`56bbc30a44`](https://github.com/nodejs/node/commit/56bbc30a44)] - **vm**: unify host-defined option generation in vm.compileFunction (Joyee Cheung) [#50137](https://github.com/nodejs/node/pull/50137)
* \[[`57efd5292c`](https://github.com/nodejs/node/commit/57efd5292c)] - **(SEMVER-MINOR)** **vm**: use import attributes instead of import assertions (Antoine du Hamel) [#50141](https://github.com/nodejs/node/pull/50141)
* \[[`17581c2716`](https://github.com/nodejs/node/commit/17581c2716)] - **vm**: use default HDO when importModuleDynamically is not set (Joyee Cheung) [#49950](https://github.com/nodejs/node/pull/49950)
* \[[`65e18aa8e7`](https://github.com/nodejs/node/commit/65e18aa8e7)] - **wasi**: address coverity warning (Michael Dawson) [#49866](https://github.com/nodejs/node/pull/49866)
* \[[`5b695d6a8d`](https://github.com/nodejs/node/commit/5b695d6a8d)] - **wasi**: fix up wasi tests for ibmi (Michael Dawson) [#49953](https://github.com/nodejs/node/pull/49953)
* \[[`b86e1f5cbd`](https://github.com/nodejs/node/commit/b86e1f5cbd)] - **(SEMVER-MINOR)** **wasi**: updates required for latest uvwasi version (Michael Dawson) [#49908](https://github.com/nodejs/node/pull/49908)
* \[[`b4d149b4d6`](https://github.com/nodejs/node/commit/b4d149b4d6)] - **worker**: handle detached `MessagePort` from a different context (Juan José) [#49150](https://github.com/nodejs/node/pull/49150)
* \[[`f564ed4e05`](https://github.com/nodejs/node/commit/f564ed4e05)] - **zlib**: fix discovery of cpu-features.h for android (MatteoBax) [#49828](https://github.com/nodejs/node/pull/49828)

<a id="20.9.0"></a>

## 2023-10-24, Version 20.9.0 'Iron' (LTS), @richardlau

### Notable Changes

This release marks the transition of Node.js 20.x into Long Term Support (LTS)
with the codename 'Iron'. The 20.x release line now moves into "Active LTS"
and will remain so until October 2024. After that time, it will move into
"Maintenance" until end of life in April 2026.

### Known issue

Collecting code coverage via the `NODE_V8_COVERAGE` environment variable may
lead to a hang. This is not thought to be a regression in Node.js 20 (some
reports are on Node.js 18). For more information, including some potential
workarounds, see issue [#49344](https://github.com/nodejs/node/issues/49344).

<a id="20.8.1"></a>

## 2023-10-13, Version 20.8.1 (Current), @RafaelGSS

This is a security release.

### Notable Changes

The following CVEs are fixed in this release:

* [CVE-2023-44487](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-44487): `nghttp2` Security Release (High)
* [CVE-2023-45143](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-45143): `undici` Security Release (High)
* [CVE-2023-39332](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-39332): Path traversal through path stored in Uint8Array (High)
* [CVE-2023-39331](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-39331): Permission model improperly protects against path traversal (High)
* [CVE-2023-38552](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-38552):  Integrity checks according to policies can be circumvented (Medium)
* [CVE-2023-39333](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-39333): Code injection via WebAssembly export names (Low)

More detailed information on each of the vulnerabilities can be found in [October 2023 Security Releases](https://nodejs.org/en/blog/vulnerability/october-2023-security-releases/) blog post.

### Commits

* \[[`c86883e844`](https://github.com/nodejs/node/commit/c86883e844)] - **deps**: update nghttp2 to 1.57.0 (James M Snell) [#50121](https://github.com/nodejs/node/pull/50121)
* \[[`2860631359`](https://github.com/nodejs/node/commit/2860631359)] - **deps**: update undici to v5.26.3 (Matteo Collina) [#50153](https://github.com/nodejs/node/pull/50153)
* \[[`cd37838bf8`](https://github.com/nodejs/node/commit/cd37838bf8)] - **lib**: let deps require `node` prefixed modules (Matthew Aitken) [#50047](https://github.com/nodejs/node/pull/50047)
* \[[`f5c90b2951`](https://github.com/nodejs/node/commit/f5c90b2951)] - **module**: fix code injection through export names (Tobias Nießen) [nodejs-private/node-private#461](https://github.com/nodejs-private/node-private/pull/461)
* \[[`fa5dae1944`](https://github.com/nodejs/node/commit/fa5dae1944)] - **permission**: fix Uint8Array path traversal (Tobias Nießen) [nodejs-private/node-private#456](https://github.com/nodejs-private/node-private/pull/456)
* \[[`cd35275111`](https://github.com/nodejs/node/commit/cd35275111)] - **permission**: improve path traversal protection (Tobias Nießen) [nodejs-private/node-private#456](https://github.com/nodejs-private/node-private/pull/456)
* \[[`a4cb7fc7c0`](https://github.com/nodejs/node/commit/a4cb7fc7c0)] - **policy**: use tamper-proof integrity check function (Tobias Nießen) [nodejs-private/node-private#462](https://github.com/nodejs-private/node-private/pull/462)

<a id="20.8.0"></a>

## 2023-09-28, Version 20.8.0 (Current), @ruyadorno

### Notable Changes

#### Stream performance improvements

Performance improvements to writable and readable streams, improving the creation and destruction by ±15% and reducing the memory overhead each stream takes in Node.js

Contributed by Benjamin Gruenbaum in [#49745](https://github.com/nodejs/node/pull/49745) and Raz Luvaton in [#49834](https://github.com/nodejs/node/pull/49834).

Performance improvements for readable webstream, improving readable stream async iterator consumption by ±140% and improving readable stream `pipeTo` consumption by ±60%

Contributed by Raz Luvaton in [#49662](https://github.com/nodejs/node/pull/49662) and [#49690](https://github.com/nodejs/node/pull/49690).

#### Rework of memory management in `vm` APIs with the `importModuleDynamically` option

This rework addressed a series of long-standing memory leaks and use-after-free issues in the following APIs that support `importModuleDynamically`:

* `vm.Script`
* `vm.compileFunction`
* `vm.SyntheticModule`
* `vm.SourceTextModule`

This should enable affected users to upgrade from older versions of Node.js.

Contributed by Joyee Cheung in [#48510](https://github.com/nodejs/node/pull/48510).

#### Other notable changes

* \[[`32d4d29d02`](https://github.com/nodejs/node/commit/32d4d29d02)] - **deps**: add v8::Object::SetInternalFieldForNodeCore() (Joyee Cheung) [#49874](https://github.com/nodejs/node/pull/49874)
* \[[`0e686d096b`](https://github.com/nodejs/node/commit/0e686d096b)] - **doc**: deprecate `fs.F_OK`, `fs.R_OK`, `fs.W_OK`, `fs.X_OK` (Livia Medeiros) [#49683](https://github.com/nodejs/node/pull/49683)
* \[[`a5dd057540`](https://github.com/nodejs/node/commit/a5dd057540)] - **doc**: deprecate `util.toUSVString` (Yagiz Nizipli) [#49725](https://github.com/nodejs/node/pull/49725)
* \[[`7b6a73172f`](https://github.com/nodejs/node/commit/7b6a73172f)] - **doc**: deprecate calling `promisify` on a function that returns a promise (Antoine du Hamel) [#49647](https://github.com/nodejs/node/pull/49647)
* \[[`1beefd5f16`](https://github.com/nodejs/node/commit/1beefd5f16)] - **esm**: set all hooks as release candidate (Geoffrey Booth) [#49597](https://github.com/nodejs/node/pull/49597)
* \[[`b0ce78a75b`](https://github.com/nodejs/node/commit/b0ce78a75b)] - **module**: fix the leak in SourceTextModule and ContextifySript (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`4e578f8ab1`](https://github.com/nodejs/node/commit/4e578f8ab1)] - **module**: fix leak of vm.SyntheticModule (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`69e4218772`](https://github.com/nodejs/node/commit/69e4218772)] - **module**: use symbol in WeakMap to manage host defined options (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`14ece0aa76`](https://github.com/nodejs/node/commit/14ece0aa76)] - **(SEMVER-MINOR)** **src**: allow embedders to override NODE\_MODULE\_VERSION (Cheng Zhao) [#49279](https://github.com/nodejs/node/pull/49279)
* \[[`9fd67fbff0`](https://github.com/nodejs/node/commit/9fd67fbff0)] - **stream**: use bitmap in writable state (Raz Luvaton) [#49834](https://github.com/nodejs/node/pull/49834)
* \[[`0ccd4638ac`](https://github.com/nodejs/node/commit/0ccd4638ac)] - **stream**: use bitmap in readable state (Benjamin Gruenbaum) [#49745](https://github.com/nodejs/node/pull/49745)
* \[[`7c5e322346`](https://github.com/nodejs/node/commit/7c5e322346)] - **stream**: improve webstream readable async iterator performance (Raz Luvaton) [#49662](https://github.com/nodejs/node/pull/49662)
* \[[`80b342cc38`](https://github.com/nodejs/node/commit/80b342cc38)] - **(SEMVER-MINOR)** **test\_runner**: accept `testOnly` in `run` (Moshe Atlow) [#49753](https://github.com/nodejs/node/pull/49753)
* \[[`17a05b141d`](https://github.com/nodejs/node/commit/17a05b141d)] - **(SEMVER-MINOR)** **test\_runner**: add junit reporter (Moshe Atlow) [#49614](https://github.com/nodejs/node/pull/49614)

### Commits

* \[[`4879e3fbbe`](https://github.com/nodejs/node/commit/4879e3fbbe)] - **benchmark**: add a benchmark for read() of ReadableStreams (Debadree Chatterjee) [#49622](https://github.com/nodejs/node/pull/49622)
* \[[`78a6c73157`](https://github.com/nodejs/node/commit/78a6c73157)] - **benchmark**: shorten pipe-to by reducing number of chunks (Raz Luvaton) [#49577](https://github.com/nodejs/node/pull/49577)
* \[[`4126a6e4c9`](https://github.com/nodejs/node/commit/4126a6e4c9)] - **benchmark**: fix webstream pipe-to (Raz Luvaton) [#49552](https://github.com/nodejs/node/pull/49552)
* \[[`6010a91825`](https://github.com/nodejs/node/commit/6010a91825)] - **bootstrap**: do not expand argv1 for snapshots (Joyee Cheung) [#49506](https://github.com/nodejs/node/pull/49506)
* \[[`8480280c4b`](https://github.com/nodejs/node/commit/8480280c4b)] - **bootstrap**: only use the isolate snapshot when compiling code cache (Joyee Cheung) [#49288](https://github.com/nodejs/node/pull/49288)
* \[[`b30754aa87`](https://github.com/nodejs/node/commit/b30754aa87)] - **build**: run embedtest using node executable (Joyee Cheung) [#49506](https://github.com/nodejs/node/pull/49506)
* \[[`31db0b8e2b`](https://github.com/nodejs/node/commit/31db0b8e2b)] - **build**: add --write-snapshot-as-array-literals to configure.py (Joyee Cheung) [#49312](https://github.com/nodejs/node/pull/49312)
* \[[`6fcb51d3ba`](https://github.com/nodejs/node/commit/6fcb51d3ba)] - **debugger**: use `internal/url.URL` instead of `url.parse` (LiviaMedeiros) [#49590](https://github.com/nodejs/node/pull/49590)
* \[[`32d4d29d02`](https://github.com/nodejs/node/commit/32d4d29d02)] - **deps**: add v8::Object::SetInternalFieldForNodeCore() (Joyee Cheung) [#49874](https://github.com/nodejs/node/pull/49874)
* \[[`ad37cadc3f`](https://github.com/nodejs/node/commit/ad37cadc3f)] - **deps**: V8: backport de9a5de2274f (Joyee Cheung) [#49703](https://github.com/nodejs/node/pull/49703)
* \[[`cdd1c66222`](https://github.com/nodejs/node/commit/cdd1c66222)] - **deps**: V8: cherry-pick b33bf2dfd261 (Joyee Cheung) [#49703](https://github.com/nodejs/node/pull/49703)
* \[[`61d18d6473`](https://github.com/nodejs/node/commit/61d18d6473)] - **deps**: update undici to 5.24.0 (Node.js GitHub Bot) [#49559](https://github.com/nodejs/node/pull/49559)
* \[[`b8a4fef393`](https://github.com/nodejs/node/commit/b8a4fef393)] - **deps**: remove pthread-fixes.c from uv.gyp (Ben Noordhuis) [#49744](https://github.com/nodejs/node/pull/49744)
* \[[`6c86c0683c`](https://github.com/nodejs/node/commit/6c86c0683c)] - **deps**: update googletest to d1467f5 (Node.js GitHub Bot) [#49676](https://github.com/nodejs/node/pull/49676)
* \[[`1424404742`](https://github.com/nodejs/node/commit/1424404742)] - **deps**: update nghttp2 to 1.56.0 (Node.js GitHub Bot) [#49582](https://github.com/nodejs/node/pull/49582)
* \[[`15b54ff95d`](https://github.com/nodejs/node/commit/15b54ff95d)] - **deps**: update googletest to 8a6feab (Node.js GitHub Bot) [#49463](https://github.com/nodejs/node/pull/49463)
* \[[`2ceab877c2`](https://github.com/nodejs/node/commit/2ceab877c2)] - **deps**: update corepack to 0.20.0 (Node.js GitHub Bot) [#49464](https://github.com/nodejs/node/pull/49464)
* \[[`4814872ddc`](https://github.com/nodejs/node/commit/4814872ddc)] - **doc**: fix `DEP0176` number (LiviaMedeiros) [#49858](https://github.com/nodejs/node/pull/49858)
* \[[`0e686d096b`](https://github.com/nodejs/node/commit/0e686d096b)] - **doc**: deprecate `fs.F_OK`, `fs.R_OK`, `fs.W_OK`, `fs.X_OK` (Livia Medeiros) [#49683](https://github.com/nodejs/node/pull/49683)
* \[[`5877c403a2`](https://github.com/nodejs/node/commit/5877c403a2)] - **doc**: add mertcanaltin as a triager (mert.altin) [#49826](https://github.com/nodejs/node/pull/49826)
* \[[`864fe56432`](https://github.com/nodejs/node/commit/864fe56432)] - **doc**: add `git node backport` way to the backporting guide (Raz Luvaton) [#49760](https://github.com/nodejs/node/pull/49760)
* \[[`e0f93492d5`](https://github.com/nodejs/node/commit/e0f93492d5)] - **doc**: improve documentation about ICU data fallback (Joyee Cheung) [#49666](https://github.com/nodejs/node/pull/49666)
* \[[`a5dd057540`](https://github.com/nodejs/node/commit/a5dd057540)] - **doc**: deprecate `util.toUSVString` (Yagiz Nizipli) [#49725](https://github.com/nodejs/node/pull/49725)
* \[[`774c1cfd52`](https://github.com/nodejs/node/commit/774c1cfd52)] - **doc**: add missing function call to example for `util.promisify` (Jungku Lee) [#49719](https://github.com/nodejs/node/pull/49719)
* \[[`fe78a34845`](https://github.com/nodejs/node/commit/fe78a34845)] - **doc**: update output of example in `mimeParams.set()` (Deokjin Kim) [#49718](https://github.com/nodejs/node/pull/49718)
* \[[`4175ea33bd`](https://github.com/nodejs/node/commit/4175ea33bd)] - **doc**: add missed `inspect` with numericSeparator to example (Deokjin Kim) [#49717](https://github.com/nodejs/node/pull/49717)
* \[[`3a88571972`](https://github.com/nodejs/node/commit/3a88571972)] - **doc**: fix history comments (Antoine du Hamel) [#49701](https://github.com/nodejs/node/pull/49701)
* \[[`db4ab1ccbb`](https://github.com/nodejs/node/commit/db4ab1ccbb)] - **doc**: add missing history info for `import.meta.resolve` (Antoine du Hamel) [#49700](https://github.com/nodejs/node/pull/49700)
* \[[`a304d1ee19`](https://github.com/nodejs/node/commit/a304d1ee19)] - **doc**: link maintaining deps to pull-request.md (Marco Ippolito) [#49716](https://github.com/nodejs/node/pull/49716)
* \[[`35294486ad`](https://github.com/nodejs/node/commit/35294486ad)] - **doc**: fix print results in `events` (Jungku Lee) [#49548](https://github.com/nodejs/node/pull/49548)
* \[[`9f0b0e15c9`](https://github.com/nodejs/node/commit/9f0b0e15c9)] - **doc**: alphabetize cli.md sections (Geoffrey Booth) [#49668](https://github.com/nodejs/node/pull/49668)
* \[[`7b6a73172f`](https://github.com/nodejs/node/commit/7b6a73172f)] - **doc**: deprecate calling `promisify` on a function that returns a promise (Antoine du Hamel) [#49647](https://github.com/nodejs/node/pull/49647)
* \[[`d316b32fff`](https://github.com/nodejs/node/commit/d316b32fff)] - **doc**: update `corepack.md` to account for 0.20.0 changes (Antoine du Hamel) [#49486](https://github.com/nodejs/node/pull/49486)
* \[[`c2eac7dc7c`](https://github.com/nodejs/node/commit/c2eac7dc7c)] - **doc**: remove `@anonrig` from performance initiative (Yagiz Nizipli) [#49641](https://github.com/nodejs/node/pull/49641)
* \[[`3d839fbf87`](https://github.com/nodejs/node/commit/3d839fbf87)] - **doc**: mark Node.js 16 as End-of-Life (Richard Lau) [#49651](https://github.com/nodejs/node/pull/49651)
* \[[`53fb5aead8`](https://github.com/nodejs/node/commit/53fb5aead8)] - **doc**: save user preference for JS flavor (Vidar Eldøy) [#49526](https://github.com/nodejs/node/pull/49526)
* \[[`e3594d5658`](https://github.com/nodejs/node/commit/e3594d5658)] - **doc**: update documentation for node:process warning (Shubham Pandey) [#49517](https://github.com/nodejs/node/pull/49517)
* \[[`8e033c3963`](https://github.com/nodejs/node/commit/8e033c3963)] - **doc**: rename possibly confusing variable and CSS class (Antoine du Hamel) [#49536](https://github.com/nodejs/node/pull/49536)
* \[[`d0e0eb4bb3`](https://github.com/nodejs/node/commit/d0e0eb4bb3)] - **doc**: update outdated history info (Antoine du Hamel) [#49530](https://github.com/nodejs/node/pull/49530)
* \[[`b4724e2e3a`](https://github.com/nodejs/node/commit/b4724e2e3a)] - **doc**: close a parenthesis (Sébastien Règne) [#49525](https://github.com/nodejs/node/pull/49525)
* \[[`0471c5798e`](https://github.com/nodejs/node/commit/0471c5798e)] - **doc**: cast GetInternalField() return type to v8::Value in addons.md (Joyee Cheung) [#49439](https://github.com/nodejs/node/pull/49439)
* \[[`9f8bea3dda`](https://github.com/nodejs/node/commit/9f8bea3dda)] - **doc**: fix documentation for input option in child\_process (Ariel Weiss) [#49481](https://github.com/nodejs/node/pull/49481)
* \[[`f3fea92f8a`](https://github.com/nodejs/node/commit/f3fea92f8a)] - **doc**: fix missing imports in `test.run` code examples (Oshri Asulin) [#49489](https://github.com/nodejs/node/pull/49489)
* \[[`e426b77b67`](https://github.com/nodejs/node/commit/e426b77b67)] - **doc**: fix documentation for fs.createWriteStream highWaterMark option (Mert Can Altın) [#49456](https://github.com/nodejs/node/pull/49456)
* \[[`2b119108ff`](https://github.com/nodejs/node/commit/2b119108ff)] - **doc**: updated releasers instructions for node.js website (Claudio W) [#49427](https://github.com/nodejs/node/pull/49427)
* \[[`b9d4a80183`](https://github.com/nodejs/node/commit/b9d4a80183)] - **doc**: edit `import.meta.resolve` documentation (Antoine du Hamel) [#49247](https://github.com/nodejs/node/pull/49247)
* \[[`f67433f666`](https://github.com/nodejs/node/commit/f67433f666)] - **doc,tools**: switch to `@node-core/utils` (Michaël Zasso) [#49851](https://github.com/nodejs/node/pull/49851)
* \[[`142e256fc5`](https://github.com/nodejs/node/commit/142e256fc5)] - **errors**: improve classRegExp in errors.js (Uzlopak) [#49643](https://github.com/nodejs/node/pull/49643)
* \[[`6377f1bce2`](https://github.com/nodejs/node/commit/6377f1bce2)] - **errors**: use `determineSpecificType` in more error messages (Antoine du Hamel) [#49580](https://github.com/nodejs/node/pull/49580)
* \[[`05f0fcb4c4`](https://github.com/nodejs/node/commit/05f0fcb4c4)] - **esm**: identify parent importing a url with invalid host (Jacob Smith) [#49736](https://github.com/nodejs/node/pull/49736)
* \[[`8a6f5fb8f3`](https://github.com/nodejs/node/commit/8a6f5fb8f3)] - **esm**: fix return type of `import.meta.resolve` (Antoine du Hamel) [#49698](https://github.com/nodejs/node/pull/49698)
* \[[`a6140f1b8c`](https://github.com/nodejs/node/commit/a6140f1b8c)] - **esm**: update loaders warning (Geoffrey Booth) [#49633](https://github.com/nodejs/node/pull/49633)
* \[[`521a9327e0`](https://github.com/nodejs/node/commit/521a9327e0)] - **esm**: fix support for `URL` instances in `register` (Antoine du Hamel) [#49655](https://github.com/nodejs/node/pull/49655)
* \[[`3a9ea0925a`](https://github.com/nodejs/node/commit/3a9ea0925a)] - **esm**: clarify ERR\_REQUIRE\_ESM errors (Daniel Compton) [#49521](https://github.com/nodejs/node/pull/49521)
* \[[`1beefd5f16`](https://github.com/nodejs/node/commit/1beefd5f16)] - **esm**: set all hooks as release candidate (Geoffrey Booth) [#49597](https://github.com/nodejs/node/pull/49597)
* \[[`be48267888`](https://github.com/nodejs/node/commit/be48267888)] - **esm**: remove return value for `Module.register` (Antoine du Hamel) [#49529](https://github.com/nodejs/node/pull/49529)
* \[[`e74a075124`](https://github.com/nodejs/node/commit/e74a075124)] - **esm**: refactor test-esm-loader-resolve-type (Geoffrey Booth) [#49493](https://github.com/nodejs/node/pull/49493)
* \[[`17823b3533`](https://github.com/nodejs/node/commit/17823b3533)] - **esm**: refactor test-esm-named-exports (Geoffrey Booth) [#49493](https://github.com/nodejs/node/pull/49493)
* \[[`f34bd15ac1`](https://github.com/nodejs/node/commit/f34bd15ac1)] - **esm**: refactor mocking test (Geoffrey Booth) [#49465](https://github.com/nodejs/node/pull/49465)
* \[[`ec323bbd99`](https://github.com/nodejs/node/commit/ec323bbd99)] - **fs**: replace `SetMethodNoSideEffect` in node\_file (CanadaHonk) [#49857](https://github.com/nodejs/node/pull/49857)
* \[[`6acf800123`](https://github.com/nodejs/node/commit/6acf800123)] - **fs**: improve error performance for `unlinkSync` (CanadaHonk) [#49856](https://github.com/nodejs/node/pull/49856)
* \[[`31702c9403`](https://github.com/nodejs/node/commit/31702c9403)] - **fs**: improve `readFileSync` with file descriptors (Yagiz Nizipli) [#49691](https://github.com/nodejs/node/pull/49691)
* \[[`835f9fe7b9`](https://github.com/nodejs/node/commit/835f9fe7b9)] - **fs**: fix file descriptor validator (Yagiz Nizipli) [#49752](https://github.com/nodejs/node/pull/49752)
* \[[`b618fe262f`](https://github.com/nodejs/node/commit/b618fe262f)] - **fs**: improve error performance of `opendirSync` (Yagiz Nizipli) [#49705](https://github.com/nodejs/node/pull/49705)
* \[[`938471ef55`](https://github.com/nodejs/node/commit/938471ef55)] - **fs**: improve error performance of sync methods (Yagiz Nizipli) [#49593](https://github.com/nodejs/node/pull/49593)
* \[[`db3fc6d087`](https://github.com/nodejs/node/commit/db3fc6d087)] - **fs**: fix readdir and opendir recursive with unknown file types (William Marlow) [#49603](https://github.com/nodejs/node/pull/49603)
* \[[`0f020ed22d`](https://github.com/nodejs/node/commit/0f020ed22d)] - **gyp**: put cctest filenames in variables (Cheng Zhao) [#49178](https://github.com/nodejs/node/pull/49178)
* \[[`0ce1e94d12`](https://github.com/nodejs/node/commit/0ce1e94d12)] - **lib**: update encoding sets in `WHATWG API` (Jungku Lee) [#49610](https://github.com/nodejs/node/pull/49610)
* \[[`efd6815a7a`](https://github.com/nodejs/node/commit/efd6815a7a)] - **lib**: fix `internalBinding` typings (Yagiz Nizipli) [#49742](https://github.com/nodejs/node/pull/49742)
* \[[`1287d5b74e`](https://github.com/nodejs/node/commit/1287d5b74e)] - **lib**: allow byob reader for 'blob.stream()' (Debadree Chatterjee) [#49713](https://github.com/nodejs/node/pull/49713)
* \[[`bbc710522d`](https://github.com/nodejs/node/commit/bbc710522d)] - **lib**: reset the cwd cache before execution (Maël Nison) [#49684](https://github.com/nodejs/node/pull/49684)
* \[[`f62d649e4d`](https://github.com/nodejs/node/commit/f62d649e4d)] - **lib**: use internal `fileURLToPath` (Deokjin Kim) [#49558](https://github.com/nodejs/node/pull/49558)
* \[[`e515046941`](https://github.com/nodejs/node/commit/e515046941)] - **lib**: use internal `pathToFileURL` (Livia Medeiros) [#49553](https://github.com/nodejs/node/pull/49553)
* \[[`00608e8070`](https://github.com/nodejs/node/commit/00608e8070)] - **lib**: check SharedArrayBuffer availability in freeze\_intrinsics.js (Milan Burda) [#49482](https://github.com/nodejs/node/pull/49482)
* \[[`8bfbe7079c`](https://github.com/nodejs/node/commit/8bfbe7079c)] - **meta**: fix linter error (Antoine du Hamel) [#49755](https://github.com/nodejs/node/pull/49755)
* \[[`58f7a9e096`](https://github.com/nodejs/node/commit/58f7a9e096)] - **meta**: add primordials strategic initiative (Benjamin Gruenbaum) [#49706](https://github.com/nodejs/node/pull/49706)
* \[[`5366027756`](https://github.com/nodejs/node/commit/5366027756)] - **meta**: bump github/codeql-action from 2.21.2 to 2.21.5 (dependabot\[bot]) [#49438](https://github.com/nodejs/node/pull/49438)
* \[[`fe26b74082`](https://github.com/nodejs/node/commit/fe26b74082)] - **meta**: bump rtCamp/action-slack-notify from 2.2.0 to 2.2.1 (dependabot\[bot]) [#49437](https://github.com/nodejs/node/pull/49437)
* \[[`b0ce78a75b`](https://github.com/nodejs/node/commit/b0ce78a75b)] - **module**: fix the leak in SourceTextModule and ContextifySript (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`4e578f8ab1`](https://github.com/nodejs/node/commit/4e578f8ab1)] - **module**: fix leak of vm.SyntheticModule (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`69e4218772`](https://github.com/nodejs/node/commit/69e4218772)] - **module**: use symbol in WeakMap to manage host defined options (Joyee Cheung) [#48510](https://github.com/nodejs/node/pull/48510)
* \[[`96874e8fbc`](https://github.com/nodejs/node/commit/96874e8fbc)] - **node-api**: enable uncaught exceptions policy by default (Chengzhong Wu) [#49313](https://github.com/nodejs/node/pull/49313)
* \[[`b931aeadfd`](https://github.com/nodejs/node/commit/b931aeadfd)] - **perf\_hooks**: reduce overhead of new performance\_entries (Vinicius Lourenço) [#49803](https://github.com/nodejs/node/pull/49803)
* \[[`ad043bac31`](https://github.com/nodejs/node/commit/ad043bac31)] - **process**: add custom dir support for heapsnapshot-signal (Jithil P Ponnan) [#47854](https://github.com/nodejs/node/pull/47854)
* \[[`8a7c10194c`](https://github.com/nodejs/node/commit/8a7c10194c)] - **repl**: don't accumulate excess indentation in .load (Daniel X Moore) [#49461](https://github.com/nodejs/node/pull/49461)
* \[[`10a2adeed5`](https://github.com/nodejs/node/commit/10a2adeed5)] - **src**: improve error message when ICU data cannot be initialized (Joyee Cheung) [#49666](https://github.com/nodejs/node/pull/49666)
* \[[`ce37688bac`](https://github.com/nodejs/node/commit/ce37688bac)] - **src**: remove unnecessary todo (Rafael Gonzaga) [#49227](https://github.com/nodejs/node/pull/49227)
* \[[`f611583b71`](https://github.com/nodejs/node/commit/f611583b71)] - **src**: use SNAPSHOT\_SERDES to log snapshot ser/deserialization (Joyee Cheung) [#49637](https://github.com/nodejs/node/pull/49637)
* \[[`a597cb8457`](https://github.com/nodejs/node/commit/a597cb8457)] - **src**: port Pipe to uv\_pipe\_bind2, uv\_pipe\_connect2 (Geoff Goodman) [#49667](https://github.com/nodejs/node/pull/49667)
* \[[`fb21062338`](https://github.com/nodejs/node/commit/fb21062338)] - **src**: set --rehash-snapshot explicitly (Joyee Cheung) [#49556](https://github.com/nodejs/node/pull/49556)
* \[[`14ece0aa76`](https://github.com/nodejs/node/commit/14ece0aa76)] - **(SEMVER-MINOR)** **src**: allow embedders to override NODE\_MODULE\_VERSION (Cheng Zhao) [#49279](https://github.com/nodejs/node/pull/49279)
* \[[`4b5e23c71b`](https://github.com/nodejs/node/commit/4b5e23c71b)] - **src**: set ModuleWrap internal fields only once (Joyee Cheung) [#49391](https://github.com/nodejs/node/pull/49391)
* \[[`2d3f5c7cab`](https://github.com/nodejs/node/commit/2d3f5c7cab)] - **src**: fix fs\_type\_to\_name default value (Mustafa Ateş Uzun) [#49239](https://github.com/nodejs/node/pull/49239)
* \[[`cfbcb1059c`](https://github.com/nodejs/node/commit/cfbcb1059c)] - **src**: fix comment on StreamResource (rogertyang) [#49193](https://github.com/nodejs/node/pull/49193)
* \[[`39fb83ad16`](https://github.com/nodejs/node/commit/39fb83ad16)] - **src**: do not rely on the internal field being default to undefined (Joyee Cheung) [#49413](https://github.com/nodejs/node/pull/49413)
* \[[`9fd67fbff0`](https://github.com/nodejs/node/commit/9fd67fbff0)] - **stream**: use bitmap in writable state (Raz Luvaton) [#49834](https://github.com/nodejs/node/pull/49834)
* \[[`0ccd4638ac`](https://github.com/nodejs/node/commit/0ccd4638ac)] - **stream**: use bitmap in readable state (Benjamin Gruenbaum) [#49745](https://github.com/nodejs/node/pull/49745)
* \[[`b29d927010`](https://github.com/nodejs/node/commit/b29d927010)] - **stream**: improve readable webstream `pipeTo` (Raz Luvaton) [#49690](https://github.com/nodejs/node/pull/49690)
* \[[`7c5e322346`](https://github.com/nodejs/node/commit/7c5e322346)] - **stream**: improve webstream readable async iterator performance (Raz Luvaton) [#49662](https://github.com/nodejs/node/pull/49662)
* \[[`be211ef818`](https://github.com/nodejs/node/commit/be211ef818)] - **test**: deflake test-vm-contextified-script-leak (Joyee Cheung) [#49710](https://github.com/nodejs/node/pull/49710)
* \[[`355f10dab2`](https://github.com/nodejs/node/commit/355f10dab2)] - **test**: use checkIfCollectable in vm leak tests (Joyee Cheung) [#49671](https://github.com/nodejs/node/pull/49671)
* \[[`17cfc531aa`](https://github.com/nodejs/node/commit/17cfc531aa)] - **test**: add checkIfCollectable to test/common/gc.js (Joyee Cheung) [#49671](https://github.com/nodejs/node/pull/49671)
* \[[`e49a573752`](https://github.com/nodejs/node/commit/e49a573752)] - **test**: add os setPriority, getPriority test coverage (Wael) [#38771](https://github.com/nodejs/node/pull/38771)
* \[[`5f02711522`](https://github.com/nodejs/node/commit/5f02711522)] - **test**: deflake test-runner-output (Moshe Atlow) [#49878](https://github.com/nodejs/node/pull/49878)
* \[[`cd9754d6a7`](https://github.com/nodejs/node/commit/cd9754d6a7)] - **test**: mark test-runner-output as flaky (Joyee Cheung) [#49854](https://github.com/nodejs/node/pull/49854)
* \[[`5ad00424dd`](https://github.com/nodejs/node/commit/5ad00424dd)] - **test**: use mustSucceed instead of mustCall (SiddharthDevulapalli) [#49788](https://github.com/nodejs/node/pull/49788)
* \[[`3db9b40081`](https://github.com/nodejs/node/commit/3db9b40081)] - **test**: refactor test-readline-async-iterators into a benchmark (Shubham Pandey) [#49237](https://github.com/nodejs/node/pull/49237)
* \[[`2cc5ad7859`](https://github.com/nodejs/node/commit/2cc5ad7859)] - _**Revert**_ "**test**: mark test-http-regr-gh-2928 as flaky" (Luigi Pinca) [#49708](https://github.com/nodejs/node/pull/49708)
* \[[`e5185b053c`](https://github.com/nodejs/node/commit/e5185b053c)] - **test**: use `fs.constants` for `fs.access` constants (Livia Medeiros) [#49685](https://github.com/nodejs/node/pull/49685)
* \[[`b9e5b43462`](https://github.com/nodejs/node/commit/b9e5b43462)] - **test**: deflake test-http-regr-gh-2928 (Luigi Pinca) [#49574](https://github.com/nodejs/node/pull/49574)
* \[[`1fffda504e`](https://github.com/nodejs/node/commit/1fffda504e)] - **test**: fix argument computation in embedtest (Joyee Cheung) [#49506](https://github.com/nodejs/node/pull/49506)
* \[[`6e56f2db52`](https://github.com/nodejs/node/commit/6e56f2db52)] - **test**: skip test-child-process-stdio-reuse-readable-stdio on Windows (Joyee Cheung) [#49621](https://github.com/nodejs/node/pull/49621)
* \[[`ab3afb330d`](https://github.com/nodejs/node/commit/ab3afb330d)] - **test**: mark test-runner-watch-mode as flaky (Joyee Cheung) [#49627](https://github.com/nodejs/node/pull/49627)
* \[[`185d9b50db`](https://github.com/nodejs/node/commit/185d9b50db)] - **test**: deflake test-tls-socket-close (Luigi Pinca) [#49575](https://github.com/nodejs/node/pull/49575)
* \[[`c70c74a9e6`](https://github.com/nodejs/node/commit/c70c74a9e6)] - **test**: show more info on failure in test-cli-syntax-require.js (Joyee Cheung) [#49561](https://github.com/nodejs/node/pull/49561)
* \[[`ed7c6d1114`](https://github.com/nodejs/node/commit/ed7c6d1114)] - **test**: mark test-http-regr-gh-2928 as flaky (Joyee Cheung) [#49565](https://github.com/nodejs/node/pull/49565)
* \[[`3599eebab9`](https://github.com/nodejs/node/commit/3599eebab9)] - **test**: use spawnSyncAndExitWithoutError in sea tests (Joyee Cheung) [#49543](https://github.com/nodejs/node/pull/49543)
* \[[`f79b153e89`](https://github.com/nodejs/node/commit/f79b153e89)] - **test**: use spawnSyncAndExitWithoutError in test/common/sea.js (Joyee Cheung) [#49543](https://github.com/nodejs/node/pull/49543)
* \[[`c079c73769`](https://github.com/nodejs/node/commit/c079c73769)] - **test**: use setImmediate() in test-heapdump-shadowrealm.js (Joyee Cheung) [#49573](https://github.com/nodejs/node/pull/49573)
* \[[`667a92493c`](https://github.com/nodejs/node/commit/667a92493c)] - **test**: skip test-child-process-pipe-dataflow\.js on Windows (Joyee Cheung) [#49563](https://github.com/nodejs/node/pull/49563)
* \[[`91af0a9a3c`](https://github.com/nodejs/node/commit/91af0a9a3c)] - _**Revert**_ "**test**: ignore the copied entry\_point.c" (Chengzhong Wu) [#49515](https://github.com/nodejs/node/pull/49515)
* \[[`567afc71b8`](https://github.com/nodejs/node/commit/567afc71b8)] - **test**: avoid copying test source files (Chengzhong Wu) [#49515](https://github.com/nodejs/node/pull/49515)
* \[[`ced25a976d`](https://github.com/nodejs/node/commit/ced25a976d)] - **test**: increase coverage of `Module.register` and `initialize` hook (Antoine du Hamel) [#49532](https://github.com/nodejs/node/pull/49532)
* \[[`be02fbdb8a`](https://github.com/nodejs/node/commit/be02fbdb8a)] - **test**: isolate `globalPreload` tests (Geoffrey Booth) [#49545](https://github.com/nodejs/node/pull/49545)
* \[[`f214428845`](https://github.com/nodejs/node/commit/f214428845)] - **test**: split test-crypto-dh to avoid timeout on slow machines in the CI (Joyee Cheung) [#49492](https://github.com/nodejs/node/pull/49492)
* \[[`3987094569`](https://github.com/nodejs/node/commit/3987094569)] - **test**: make `test-dotenv-node-options` locale-independent (Livia Medeiros) [#49470](https://github.com/nodejs/node/pull/49470)
* \[[`34c1741792`](https://github.com/nodejs/node/commit/34c1741792)] - **test**: add test for urlstrings usage in `node:fs` (Livia Medeiros) [#49471](https://github.com/nodejs/node/pull/49471)
* \[[`c3c6c4f007`](https://github.com/nodejs/node/commit/c3c6c4f007)] - **test**: make test-worker-prof more robust (Joyee Cheung) [#49274](https://github.com/nodejs/node/pull/49274)
* \[[`843df1a4da`](https://github.com/nodejs/node/commit/843df1a4da)] - **test,crypto**: update WebCryptoAPI WPT (Filip Skokan) [#49714](https://github.com/nodejs/node/pull/49714)
* \[[`80b342cc38`](https://github.com/nodejs/node/commit/80b342cc38)] - **(SEMVER-MINOR)** **test\_runner**: accept `testOnly` in `run` (Moshe Atlow) [#49753](https://github.com/nodejs/node/pull/49753)
* \[[`76865515b9`](https://github.com/nodejs/node/commit/76865515b9)] - **test\_runner**: fix test runner watch mode when no positional arguments (Moshe Atlow) [#49578](https://github.com/nodejs/node/pull/49578)
* \[[`17a05b141d`](https://github.com/nodejs/node/commit/17a05b141d)] - **(SEMVER-MINOR)** **test\_runner**: add junit reporter (Moshe Atlow) [#49614](https://github.com/nodejs/node/pull/49614)
* \[[`5672e38457`](https://github.com/nodejs/node/commit/5672e38457)] - **test\_runner**: add jsdocs to mock.js (Caio Borghi) [#49555](https://github.com/nodejs/node/pull/49555)
* \[[`b4d42a8f2b`](https://github.com/nodejs/node/commit/b4d42a8f2b)] - **test\_runner**: fix invalid timer call (Erick Wendel) [#49477](https://github.com/nodejs/node/pull/49477)
* \[[`f755e6786b`](https://github.com/nodejs/node/commit/f755e6786b)] - **test\_runner**: add jsdocs to MockTimers (Erick Wendel) [#49476](https://github.com/nodejs/node/pull/49476)
* \[[`e7285d4bf0`](https://github.com/nodejs/node/commit/e7285d4bf0)] - **test\_runner**: fix typescript coverage (Moshe Atlow) [#49406](https://github.com/nodejs/node/pull/49406)
* \[[`07a2e29bf3`](https://github.com/nodejs/node/commit/07a2e29bf3)] - **tools**: support updating @reporters/github manually (Moshe Atlow) [#49871](https://github.com/nodejs/node/pull/49871)
* \[[`5ac6722031`](https://github.com/nodejs/node/commit/5ac6722031)] - **tools**: skip ruff on tools/node\_modules (Moshe Atlow) [#49838](https://github.com/nodejs/node/pull/49838)
* \[[`462228bd24`](https://github.com/nodejs/node/commit/462228bd24)] - **tools**: fix uvwasi updater (Michael Dawson) [#49682](https://github.com/nodejs/node/pull/49682)
* \[[`ff81bfb958`](https://github.com/nodejs/node/commit/ff81bfb958)] - **tools**: update lint-md-dependencies to rollup\@3.29.2 (Node.js GitHub Bot) [#49679](https://github.com/nodejs/node/pull/49679)
* \[[`08ffc6344c`](https://github.com/nodejs/node/commit/08ffc6344c)] - **tools**: restrict internal code from using public `url` module (LiviaMedeiros) [#49590](https://github.com/nodejs/node/pull/49590)
* \[[`728ebf6c97`](https://github.com/nodejs/node/commit/728ebf6c97)] - **tools**: update eslint to 8.49.0 (Node.js GitHub Bot) [#49586](https://github.com/nodejs/node/pull/49586)
* \[[`20d038ffb1`](https://github.com/nodejs/node/commit/20d038ffb1)] - **tools**: update lint-md-dependencies to rollup\@3.29.0 unified\@11.0.3 (Node.js GitHub Bot) [#49584](https://github.com/nodejs/node/pull/49584)
* \[[`210c15bd12`](https://github.com/nodejs/node/commit/210c15bd12)] - **tools**: allow passing absolute path of config.gypi in js2c (Cheng Zhao) [#49162](https://github.com/nodejs/node/pull/49162)
* \[[`e341efe173`](https://github.com/nodejs/node/commit/e341efe173)] - **tools**: configure never-stale label correctly (Michaël Zasso) [#49498](https://github.com/nodejs/node/pull/49498)
* \[[`a8a8a498ce`](https://github.com/nodejs/node/commit/a8a8a498ce)] - **tools**: update doc dependencies (Node.js GitHub Bot) [#49467](https://github.com/nodejs/node/pull/49467)
* \[[`ac06607f9e`](https://github.com/nodejs/node/commit/ac06607f9e)] - **typings**: fix missing property in `ExportedHooks` (Antoine du Hamel) [#49567](https://github.com/nodejs/node/pull/49567)
* \[[`097b59807a`](https://github.com/nodejs/node/commit/097b59807a)] - **url**: improve invalid url performance (Yagiz Nizipli) [#49692](https://github.com/nodejs/node/pull/49692)
* \[[`7c2060cfac`](https://github.com/nodejs/node/commit/7c2060cfac)] - **util**: add `getCwdSafe` internal util fn (João Lenon) [#48434](https://github.com/nodejs/node/pull/48434)
* \[[`c23c60f545`](https://github.com/nodejs/node/commit/c23c60f545)] - **zlib**: disable CRC32 SIMD optimization (Luigi Pinca) [#49511](https://github.com/nodejs/node/pull/49511)

<a id="20.7.0"></a>

## 2023-09-18, Version 20.7.0 (Current), @UlisesGascon

### Notable Changes

* \[[`022f1b70c1`](https://github.com/nodejs/node/commit/022f1b70c1)] - **src**: support multiple `--env-file` declarations (Yagiz Nizipli) [#49542](https://github.com/nodejs/node/pull/49542)
* \[[`4a1d1cad61`](https://github.com/nodejs/node/commit/4a1d1cad61)] - **crypto**: update root certificates to NSS 3.93 (Node.js GitHub Bot) [#49341](https://github.com/nodejs/node/pull/49341)
* \[[`a1a65f593c`](https://github.com/nodejs/node/commit/a1a65f593c)] - **deps**: upgrade npm to 10.1.0 (npm team) [#49570](https://github.com/nodejs/node/pull/49570)
* \[[`6c2480cad9`](https://github.com/nodejs/node/commit/6c2480cad9)] - **(SEMVER-MINOR)** **deps**: upgrade npm to 10.0.0 (npm team) [#49423](https://github.com/nodejs/node/pull/49423)
* \[[`bef900e56b`](https://github.com/nodejs/node/commit/bef900e56b)] - **doc**: move and rename loaders section (Geoffrey Booth) [#49261](https://github.com/nodejs/node/pull/49261)
* \[[`db4ce8a593`](https://github.com/nodejs/node/commit/db4ce8a593)] - **doc**: add release key for Ulises Gascon (Ulises Gascón) [#49196](https://github.com/nodejs/node/pull/49196)
* \[[`11c85ffa98`](https://github.com/nodejs/node/commit/11c85ffa98)] - **(SEMVER-MINOR)** **lib**: add api to detect whether source-maps are enabled (翠 / green) [#46391](https://github.com/nodejs/node/pull/46391)
* \[[`ec51e25ed7`](https://github.com/nodejs/node/commit/ec51e25ed7)] - **src,permission**: add multiple allow-fs-\* flags (Carlos Espa) [#49047](https://github.com/nodejs/node/pull/49047)
* \[[`efdc95fbc0`](https://github.com/nodejs/node/commit/efdc95fbc0)] - **(SEMVER-MINOR)** **test\_runner**: expose location of tests (Colin Ihrig) [#48975](https://github.com/nodejs/node/pull/48975)

### Commits

* \[[`e84515594e`](https://github.com/nodejs/node/commit/e84515594e)] - **benchmark**: use `tmpdir.resolve()` (Livia Medeiros) [#49137](https://github.com/nodejs/node/pull/49137)
* \[[`f37444e896`](https://github.com/nodejs/node/commit/f37444e896)] - **bootstrap**: build code cache from deserialized isolate (Joyee Cheung) [#49099](https://github.com/nodejs/node/pull/49099)
* \[[`af6dc1754d`](https://github.com/nodejs/node/commit/af6dc1754d)] - **bootstrap**: do not generate code cache in an unfinalized isolate (Joyee Cheung) [#49108](https://github.com/nodejs/node/pull/49108)
* \[[`cade5716df`](https://github.com/nodejs/node/commit/cade5716df)] - **build**: add symlink to `compile_commands.json` file if needed (Juan José) [#49260](https://github.com/nodejs/node/pull/49260)
* \[[`34a2590b05`](https://github.com/nodejs/node/commit/34a2590b05)] - **build**: expand when we run internet tests (Michael Dawson) [#49218](https://github.com/nodejs/node/pull/49218)
* \[[`f637fd46ab`](https://github.com/nodejs/node/commit/f637fd46ab)] - **build**: fix typo `libray` -> `library` (configure.py) (michalbiesek) [#49106](https://github.com/nodejs/node/pull/49106)
* \[[`ef3d8dd493`](https://github.com/nodejs/node/commit/ef3d8dd493)] - **crypto**: remove webcrypto EdDSA key checks and properties (Filip Skokan) [#49408](https://github.com/nodejs/node/pull/49408)
* \[[`4a1d1cad61`](https://github.com/nodejs/node/commit/4a1d1cad61)] - **crypto**: update root certificates to NSS 3.93 (Node.js GitHub Bot) [#49341](https://github.com/nodejs/node/pull/49341)
* \[[`7eb10a38ea`](https://github.com/nodejs/node/commit/7eb10a38ea)] - **crypto**: remove getDefaultEncoding() (Tobias Nießen) [#49170](https://github.com/nodejs/node/pull/49170)
* \[[`772496c030`](https://github.com/nodejs/node/commit/772496c030)] - **crypto**: remove default encoding from DiffieHellman (Tobias Nießen) [#49169](https://github.com/nodejs/node/pull/49169)
* \[[`c795083232`](https://github.com/nodejs/node/commit/c795083232)] - **crypto**: remove default encoding from Hash/Hmac (Tobias Nießen) [#49167](https://github.com/nodejs/node/pull/49167)
* \[[`08197aa010`](https://github.com/nodejs/node/commit/08197aa010)] - **crypto**: remove default encoding from sign/verify (Tobias Nießen) [#49145](https://github.com/nodejs/node/pull/49145)
* \[[`a1a65f593c`](https://github.com/nodejs/node/commit/a1a65f593c)] - **deps**: upgrade npm to 10.1.0 (npm team) [#49570](https://github.com/nodejs/node/pull/49570)
* \[[`6c2480cad9`](https://github.com/nodejs/node/commit/6c2480cad9)] - **(SEMVER-MINOR)** **deps**: upgrade npm to 10.0.0 (npm team) [#49423](https://github.com/nodejs/node/pull/49423)
* \[[`84195d9584`](https://github.com/nodejs/node/commit/84195d9584)] - **deps**: add missing thread-common.c in uv.gyp (Santiago Gimeno) [#49410](https://github.com/nodejs/node/pull/49410)
* \[[`5b70b68b3d`](https://github.com/nodejs/node/commit/5b70b68b3d)] - **deps**: V8: cherry-pick eadaef581c29 (Adam Majer) [#49401](https://github.com/nodejs/node/pull/49401)
* \[[`fe34d632e8`](https://github.com/nodejs/node/commit/fe34d632e8)] - **deps**: update zlib to 1.2.13.1-motley-f5fd0ad (Node.js GitHub Bot) [#49252](https://github.com/nodejs/node/pull/49252)
* \[[`db4ce8a593`](https://github.com/nodejs/node/commit/db4ce8a593)] - **doc**: add release key for Ulises Gascon (Ulises Gascón) [#49196](https://github.com/nodejs/node/pull/49196)
* \[[`e5f3a694cf`](https://github.com/nodejs/node/commit/e5f3a694cf)] - **doc**: fix node-api call example (Chengzhong Wu) [#49395](https://github.com/nodejs/node/pull/49395)
* \[[`021345a724`](https://github.com/nodejs/node/commit/021345a724)] - **doc**: add news issue for Diagnostics WG (Michael Dawson) [#49306](https://github.com/nodejs/node/pull/49306)
* \[[`f82347266b`](https://github.com/nodejs/node/commit/f82347266b)] - **doc**: clarify policy expectations (Rafael Gonzaga) [#48947](https://github.com/nodejs/node/pull/48947)
* \[[`73cfd9c895`](https://github.com/nodejs/node/commit/73cfd9c895)] - **doc**: add print results for examples in `StringDecoder` (Jungku Lee) [#49326](https://github.com/nodejs/node/pull/49326)
* \[[`63ab591416`](https://github.com/nodejs/node/commit/63ab591416)] - **doc**: update outdated reference to NIST SP 800-131A (Tobias Nießen) [#49316](https://github.com/nodejs/node/pull/49316)
* \[[`935dfe2afd`](https://github.com/nodejs/node/commit/935dfe2afd)] - **doc**: use `cjs` as block code's type in `MockTimers` (Deokjin Kim) [#49309](https://github.com/nodejs/node/pull/49309)
* \[[`7c0cd2fb87`](https://github.com/nodejs/node/commit/7c0cd2fb87)] - **doc**: update `options.filter` description for `fs.cp` (Shubham Pandey) [#49289](https://github.com/nodejs/node/pull/49289)
* \[[`f72e79ea67`](https://github.com/nodejs/node/commit/f72e79ea67)] - **doc**: add riscv64 to list of architectures (Stewart X Addison) [#49284](https://github.com/nodejs/node/pull/49284)
* \[[`d19c710064`](https://github.com/nodejs/node/commit/d19c710064)] - **doc**: avoid "not currently recommended" (Tobias Nießen) [#49300](https://github.com/nodejs/node/pull/49300)
* \[[`ae656101c0`](https://github.com/nodejs/node/commit/ae656101c0)] - **doc**: update module hooks docs (Geoffrey Booth) [#49265](https://github.com/nodejs/node/pull/49265)
* \[[`fefbdb92f2`](https://github.com/nodejs/node/commit/fefbdb92f2)] - **doc**: modify param description for end(),write() in `StringDecoder` (Jungku Lee) [#49285](https://github.com/nodejs/node/pull/49285)
* \[[`59e66a1ebe`](https://github.com/nodejs/node/commit/59e66a1ebe)] - **doc**: use NODE\_API\_SUPPORTED\_VERSION\_MAX in release doc (Cheng Zhao) [#49268](https://github.com/nodejs/node/pull/49268)
* \[[`ac3b88449b`](https://github.com/nodejs/node/commit/ac3b88449b)] - **doc**: fix typo in `stream.finished` documentation (Antoine du Hamel) [#49271](https://github.com/nodejs/node/pull/49271)
* \[[`7428ebf6c3`](https://github.com/nodejs/node/commit/7428ebf6c3)] - **doc**: update description for `percent_encode` sets in `WHATWG API` (Jungku Lee) [#49258](https://github.com/nodejs/node/pull/49258)
* \[[`bef900e56b`](https://github.com/nodejs/node/commit/bef900e56b)] - **doc**: move and rename loaders section (Geoffrey Booth) [#49261](https://github.com/nodejs/node/pull/49261)
* \[[`a22e0d9696`](https://github.com/nodejs/node/commit/a22e0d9696)] - **doc**: clarify use of Uint8Array for n-api (Fedor Indutny) [#48742](https://github.com/nodejs/node/pull/48742)
* \[[`1704f24cb9`](https://github.com/nodejs/node/commit/1704f24cb9)] - **doc**: add signature for `module.register` (Geoffrey Booth) [#49251](https://github.com/nodejs/node/pull/49251)
* \[[`5a363bb01b`](https://github.com/nodejs/node/commit/5a363bb01b)] - **doc**: caveat unavailability of `import.meta.resolve` in custom loaders (Jacob Smith) [#49242](https://github.com/nodejs/node/pull/49242)
* \[[`8101f2b259`](https://github.com/nodejs/node/commit/8101f2b259)] - **doc**: use same name in the doc as in the code (Hyunjin Kim) [#49216](https://github.com/nodejs/node/pull/49216)
* \[[`edf278d60d`](https://github.com/nodejs/node/commit/edf278d60d)] - **doc**: add notable-change label mention to PR template (Rafael Gonzaga) [#49188](https://github.com/nodejs/node/pull/49188)
* \[[`3df2251a6a`](https://github.com/nodejs/node/commit/3df2251a6a)] - **doc**: add h1 summary to security release process (Rafael Gonzaga) [#49112](https://github.com/nodejs/node/pull/49112)
* \[[`9fcd99a744`](https://github.com/nodejs/node/commit/9fcd99a744)] - **doc**: update to semver-minor releases by default (Rafael Gonzaga) [#49175](https://github.com/nodejs/node/pull/49175)
* \[[`777931f499`](https://github.com/nodejs/node/commit/777931f499)] - **doc**: fix wording in napi\_async\_init (Tobias Nießen) [#49180](https://github.com/nodejs/node/pull/49180)
* \[[`f45c8e10c0`](https://github.com/nodejs/node/commit/f45c8e10c0)] - **doc,test**: add known path resolution issue in permission model (Tobias Nießen) [#49155](https://github.com/nodejs/node/pull/49155)
* \[[`a6cfea3f74`](https://github.com/nodejs/node/commit/a6cfea3f74)] - **esm**: align sync and async load implementations (Antoine du Hamel) [#49152](https://github.com/nodejs/node/pull/49152)
* \[[`9fac310b33`](https://github.com/nodejs/node/commit/9fac310b33)] - **fs**: add the options param description in openAsBlob() (Yeseul Lee) [#49308](https://github.com/nodejs/node/pull/49308)
* \[[`92772a8175`](https://github.com/nodejs/node/commit/92772a8175)] - **fs**: remove redundant code in readableWebStream() (Deokjin Kim) [#49298](https://github.com/nodejs/node/pull/49298)
* \[[`88ba79b083`](https://github.com/nodejs/node/commit/88ba79b083)] - **fs**: make sure to write entire buffer (Robert Nagy) [#49211](https://github.com/nodejs/node/pull/49211)
* \[[`11c85ffa98`](https://github.com/nodejs/node/commit/11c85ffa98)] - **(SEMVER-MINOR)** **lib**: add api to detect whether source-maps are enabled (翠 / green) [#46391](https://github.com/nodejs/node/pull/46391)
* \[[`c12711ebfe`](https://github.com/nodejs/node/commit/c12711ebfe)] - **lib**: implement WeakReference on top of JS WeakRef (Joyee Cheung) [#49053](https://github.com/nodejs/node/pull/49053)
* \[[`9a0891f88d`](https://github.com/nodejs/node/commit/9a0891f88d)] - **meta**: bump step-security/harden-runner from 2.5.0 to 2.5.1 (dependabot\[bot]) [#49435](https://github.com/nodejs/node/pull/49435)
* \[[`ae67f41ef1`](https://github.com/nodejs/node/commit/ae67f41ef1)] - **meta**: bump actions/checkout from 3.5.3 to 3.6.0 (dependabot\[bot]) [#49436](https://github.com/nodejs/node/pull/49436)
* \[[`71b4411fb2`](https://github.com/nodejs/node/commit/71b4411fb2)] - **meta**: bump actions/setup-node from 3.7.0 to 3.8.1 (dependabot\[bot]) [#49434](https://github.com/nodejs/node/pull/49434)
* \[[`83b7d3a395`](https://github.com/nodejs/node/commit/83b7d3a395)] - **meta**: remove modules team from CODEOWNERS (Benjamin Gruenbaum) [#49412](https://github.com/nodejs/node/pull/49412)
* \[[`81ff68c45c`](https://github.com/nodejs/node/commit/81ff68c45c)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#49264](https://github.com/nodejs/node/pull/49264)
* \[[`ab975233cc`](https://github.com/nodejs/node/commit/ab975233cc)] - **meta**: mention nodejs/tsc when changing GH templates (Rafael Gonzaga) [#49189](https://github.com/nodejs/node/pull/49189)
* \[[`ceaa5494de`](https://github.com/nodejs/node/commit/ceaa5494de)] - **meta**: add test/reporters to codeowners (Chemi Atlow) [#49186](https://github.com/nodejs/node/pull/49186)
* \[[`de0a51b7cf`](https://github.com/nodejs/node/commit/de0a51b7cf)] - **net**: improve performance of isIPv4 and isIPv6 (Uzlopak) [#49568](https://github.com/nodejs/node/pull/49568)
* \[[`8d0913bf95`](https://github.com/nodejs/node/commit/8d0913bf95)] - **net**: use asserts in JS Socket Stream to catch races in future (Tim Perry) [#49400](https://github.com/nodejs/node/pull/49400)
* \[[`2486836a7d`](https://github.com/nodejs/node/commit/2486836a7d)] - **net**: fix crash due to simultaneous close/shutdown on JS Stream Sockets (Tim Perry) [#49400](https://github.com/nodejs/node/pull/49400)
* \[[`7a808340cd`](https://github.com/nodejs/node/commit/7a808340cd)] - **node-api**: fix compiler warning in node\_api.h (Michael Graeb) [#49103](https://github.com/nodejs/node/pull/49103)
* \[[`30f26a99f4`](https://github.com/nodejs/node/commit/30f26a99f4)] - **permission**: ensure to resolve path when calling mkdtemp (RafaelGSS) [nodejs-private/node-private#440](https://github.com/nodejs-private/node-private/pull/440)
* \[[`5051c75a5b`](https://github.com/nodejs/node/commit/5051c75a5b)] - **policy**: fix path to URL conversion (Antoine du Hamel) [#49133](https://github.com/nodejs/node/pull/49133)
* \[[`173aed4757`](https://github.com/nodejs/node/commit/173aed4757)] - **report**: fix recent coverity warning (Michael Dawson) [#48954](https://github.com/nodejs/node/pull/48954)
* \[[`d7ff78b442`](https://github.com/nodejs/node/commit/d7ff78b442)] - **sea**: generate code cache with deserialized isolate (Joyee Cheung) [#49226](https://github.com/nodejs/node/pull/49226)
* \[[`022f1b70c1`](https://github.com/nodejs/node/commit/022f1b70c1)] - **src**: support multiple `--env-file` declarations (Yagiz Nizipli) [#49542](https://github.com/nodejs/node/pull/49542)
* \[[`154b1c2115`](https://github.com/nodejs/node/commit/154b1c2115)] - **src**: don't overwrite environment from .env file (Phil Nash) [#49424](https://github.com/nodejs/node/pull/49424)
* \[[`dc4de1c69b`](https://github.com/nodejs/node/commit/dc4de1c69b)] - **src**: modify code for empty string (pluris) [#49336](https://github.com/nodejs/node/pull/49336)
* \[[`701c46f967`](https://github.com/nodejs/node/commit/701c46f967)] - **src**: remove unused PromiseWrap-related code (Joyee Cheung) [#49335](https://github.com/nodejs/node/pull/49335)
* \[[`4a094dc7af`](https://github.com/nodejs/node/commit/4a094dc7af)] - **src**: rename IsAnyByteSource to IsAnyBufferSource (Tobias Nießen) [#49346](https://github.com/nodejs/node/pull/49346)
* \[[`55d6649175`](https://github.com/nodejs/node/commit/55d6649175)] - **src**: support snapshot deserialization in RAIIIsolate (Joyee Cheung) [#49226](https://github.com/nodejs/node/pull/49226)
* \[[`dc092864ef`](https://github.com/nodejs/node/commit/dc092864ef)] - **src**: remove unused function `GetName()` in node\_perf (Jungku Lee) [#49244](https://github.com/nodejs/node/pull/49244)
* \[[`f2552a410e`](https://github.com/nodejs/node/commit/f2552a410e)] - **src**: use ARES\_SUCCESS instead of 0 (Jungku Lee) [#49048](https://github.com/nodejs/node/pull/49048)
* \[[`4a9ae31519`](https://github.com/nodejs/node/commit/4a9ae31519)] - **src**: add a condition if the argument of `DomainToUnicode` is empty (Jungku Lee) [#49097](https://github.com/nodejs/node/pull/49097)
* \[[`f460362cdf`](https://github.com/nodejs/node/commit/f460362cdf)] - **src**: remove C++ WeakReference implementation (Joyee Cheung) [#49053](https://github.com/nodejs/node/pull/49053)
* \[[`2a35383b3e`](https://github.com/nodejs/node/commit/2a35383b3e)] - **src**: use per-realm GetBindingData() wherever applicable (Joyee Cheung) [#49007](https://github.com/nodejs/node/pull/49007)
* \[[`184bbddcf5`](https://github.com/nodejs/node/commit/184bbddcf5)] - **src**: add per-realm GetBindingData() method (Joyee Cheung) [#49007](https://github.com/nodejs/node/pull/49007)
* \[[`e9946885f9`](https://github.com/nodejs/node/commit/e9946885f9)] - **src**: serialize both BaseObject slots (Joyee Cheung) [#48996](https://github.com/nodejs/node/pull/48996)
* \[[`ec51e25ed7`](https://github.com/nodejs/node/commit/ec51e25ed7)] - **src,permission**: add multiple allow-fs-\* flags (Carlos Espa) [#49047](https://github.com/nodejs/node/pull/49047)
* \[[`8aac95de4b`](https://github.com/nodejs/node/commit/8aac95de4b)] - **stream**: improve tee perf by reduce `ReflectConstruct` usages (Raz Luvaton) [#49546](https://github.com/nodejs/node/pull/49546)
* \[[`0eea7fd8fb`](https://github.com/nodejs/node/commit/0eea7fd8fb)] - **stream**: use Buffer.from when constructor is a Buffer (Matthew Aitken) [#49250](https://github.com/nodejs/node/pull/49250)
* \[[`b961d9bd52`](https://github.com/nodejs/node/commit/b961d9bd52)] - **stream**: add `highWaterMark` for the map operator (Raz Luvaton) [#49249](https://github.com/nodejs/node/pull/49249)
* \[[`ca1384166d`](https://github.com/nodejs/node/commit/ca1384166d)] - **test**: fix warning for comment in embedtest (Jungku Lee) [#49416](https://github.com/nodejs/node/pull/49416)
* \[[`2a35782809`](https://github.com/nodejs/node/commit/2a35782809)] - **test**: simplify test-crypto-dh-group-setters (Tobias Nießen) [#49404](https://github.com/nodejs/node/pull/49404)
* \[[`6740f3c209`](https://github.com/nodejs/node/commit/6740f3c209)] - **test**: verify dynamic import call with absolute path strings (Chengzhong Wu) [#49275](https://github.com/nodejs/node/pull/49275)
* \[[`6ed47bd8fb`](https://github.com/nodejs/node/commit/6ed47bd8fb)] - **test**: reduce length in crypto keygen tests (Joyee Cheung) [#49221](https://github.com/nodejs/node/pull/49221)
* \[[`4faa30c553`](https://github.com/nodejs/node/commit/4faa30c553)] - **test**: split JWK async elliptic curve keygen tests (Joyee Cheung) [#49221](https://github.com/nodejs/node/pull/49221)
* \[[`e04a2603d8`](https://github.com/nodejs/node/commit/e04a2603d8)] - **test**: split test-crypto-keygen.js (Joyee Cheung) [#49221](https://github.com/nodejs/node/pull/49221)
* \[[`0d23c1d4ce`](https://github.com/nodejs/node/commit/0d23c1d4ce)] - **test**: rename test-crypto-modp1-error (Tobias Nießen) [#49348](https://github.com/nodejs/node/pull/49348)
* \[[`48e41569e2`](https://github.com/nodejs/node/commit/48e41569e2)] - **test**: migrate message source map tests from Python to JS (Yiyun Lei) [#49238](https://github.com/nodejs/node/pull/49238)
* \[[`a11e64e09c`](https://github.com/nodejs/node/commit/a11e64e09c)] - **test**: fix compiler warning in NodeCryptoEnv (Tobias Nießen) [#49206](https://github.com/nodejs/node/pull/49206)
* \[[`345543938f`](https://github.com/nodejs/node/commit/345543938f)] - **test**: handle EUNATCH (Abdirahim Musse) [#48050](https://github.com/nodejs/node/pull/48050)
* \[[`e391f4b197`](https://github.com/nodejs/node/commit/e391f4b197)] - **test**: use `tmpdir.resolve()` (Livia Medeiros) [#49136](https://github.com/nodejs/node/pull/49136)
* \[[`910378f93f`](https://github.com/nodejs/node/commit/910378f93f)] - **test**: reduce flakiness of `test-esm-loader-hooks` (Antoine du Hamel) [#49248](https://github.com/nodejs/node/pull/49248)
* \[[`4a85f70462`](https://github.com/nodejs/node/commit/4a85f70462)] - **test**: add spawnSyncAndExit() and spawnSyncAndExitWithoutError() (Joyee Cheung) [#49200](https://github.com/nodejs/node/pull/49200)
* \[[`9610008b79`](https://github.com/nodejs/node/commit/9610008b79)] - **test**: make test-perf-hooks more robust and work with workers (Joyee Cheung) [#49197](https://github.com/nodejs/node/pull/49197)
* \[[`dc8fff9a75`](https://github.com/nodejs/node/commit/dc8fff9a75)] - **test**: use gcUntil() in test-v8-serialize-leak (Joyee Cheung) [#49168](https://github.com/nodejs/node/pull/49168)
* \[[`ca9f801332`](https://github.com/nodejs/node/commit/ca9f801332)] - **test**: make WeakReference tests robust (Joyee Cheung) [#49053](https://github.com/nodejs/node/pull/49053)
* \[[`de103a4686`](https://github.com/nodejs/node/commit/de103a4686)] - **test**: add test for effect of UV\_THREADPOOL\_SIZE (Tobias Nießen) [#49165](https://github.com/nodejs/node/pull/49165)
* \[[`47d24f144b`](https://github.com/nodejs/node/commit/47d24f144b)] - **test**: use expectSyncExit{WithErrors} in snapshot tests (Joyee Cheung) [#49020](https://github.com/nodejs/node/pull/49020)
* \[[`c441f5a097`](https://github.com/nodejs/node/commit/c441f5a097)] - **test**: add expectSyncExitWithoutError() and expectSyncExit() utils (Joyee Cheung) [#49020](https://github.com/nodejs/node/pull/49020)
* \[[`4d184b5251`](https://github.com/nodejs/node/commit/4d184b5251)] - **test**: remove --no-warnings flag in test\_runner fixtures (Raz Luvaton) [#48989](https://github.com/nodejs/node/pull/48989)
* \[[`25e967a90b`](https://github.com/nodejs/node/commit/25e967a90b)] - **test**: reorder test files fixtures for better understanding (Raz Luvaton) [#48787](https://github.com/nodejs/node/pull/48787)
* \[[`fac56dbcc0`](https://github.com/nodejs/node/commit/fac56dbcc0)] - **test,benchmark**: use `tmpdir.fileURL()` (Livia Medeiros) [#49138](https://github.com/nodejs/node/pull/49138)
* \[[`36763fa532`](https://github.com/nodejs/node/commit/36763fa532)] - **test\_runner**: preserve original property descriptor (Erick Wendel) [#49433](https://github.com/nodejs/node/pull/49433)
* \[[`40e9fcdbea`](https://github.com/nodejs/node/commit/40e9fcdbea)] - **test\_runner**: add support for setImmediate (Erick Wendel) [#49397](https://github.com/nodejs/node/pull/49397)
* \[[`23216f1935`](https://github.com/nodejs/node/commit/23216f1935)] - **test\_runner**: report covered lines, functions and branches to reporters (Phil Nash) [#49320](https://github.com/nodejs/node/pull/49320)
* \[[`283f2806b1`](https://github.com/nodejs/node/commit/283f2806b1)] - **test\_runner**: expose spec reporter as newable function (Chemi Atlow) [#49184](https://github.com/nodejs/node/pull/49184)
* \[[`546ad5f770`](https://github.com/nodejs/node/commit/546ad5f770)] - **test\_runner**: reland run global after() hook earlier (Colin Ihrig) [#49116](https://github.com/nodejs/node/pull/49116)
* \[[`efdc95fbc0`](https://github.com/nodejs/node/commit/efdc95fbc0)] - **(SEMVER-MINOR)** **test\_runner**: expose location of tests (Colin Ihrig) [#48975](https://github.com/nodejs/node/pull/48975)
* \[[`4bc0a8fe99`](https://github.com/nodejs/node/commit/4bc0a8fe99)] - **test\_runner**: fix global after not failing the tests (Raz Luvaton) [#48913](https://github.com/nodejs/node/pull/48913)
* \[[`08738b2664`](https://github.com/nodejs/node/commit/08738b2664)] - **test\_runner**: fix timeout in \*Each hook failing further tests (Raz Luvaton) [#48925](https://github.com/nodejs/node/pull/48925)
* \[[`c2f1830f66`](https://github.com/nodejs/node/commit/c2f1830f66)] - **test\_runner**: cleanup test timeout abort listener (Raz Luvaton) [#48915](https://github.com/nodejs/node/pull/48915)
* \[[`75333f38b2`](https://github.com/nodejs/node/commit/75333f38b2)] - **test\_runner**: fix global before not called when no global test exists (Raz Luvaton) [#48877](https://github.com/nodejs/node/pull/48877)
* \[[`b28b85adf8`](https://github.com/nodejs/node/commit/b28b85adf8)] - **tls**: remove redundant code in onConnectSecure() (Deokjin Kim) [#49457](https://github.com/nodejs/node/pull/49457)
* \[[`83fc4dccbc`](https://github.com/nodejs/node/commit/83fc4dccbc)] - **tls**: refactor to use validateFunction (Deokjin Kim) [#49422](https://github.com/nodejs/node/pull/49422)
* \[[`8949cc79dd`](https://github.com/nodejs/node/commit/8949cc79dd)] - **tls**: ensure TLS Sockets are closed if the underlying wrap closes (Tim Perry) [#49327](https://github.com/nodejs/node/pull/49327)
* \[[`1df56e6f01`](https://github.com/nodejs/node/commit/1df56e6f01)] - **tools**: update eslint to 8.48.0 (Node.js GitHub Bot) [#49343](https://github.com/nodejs/node/pull/49343)
* \[[`ef50ec5b57`](https://github.com/nodejs/node/commit/ef50ec5b57)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#49342](https://github.com/nodejs/node/pull/49342)
* \[[`9a8fb4fc34`](https://github.com/nodejs/node/commit/9a8fb4fc34)] - **tools**: remove v8\_dump\_build\_config action (Cheng Zhao) [#49301](https://github.com/nodejs/node/pull/49301)
* \[[`91b2d4314b`](https://github.com/nodejs/node/commit/91b2d4314b)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#49253](https://github.com/nodejs/node/pull/49253)
* \[[`b51946ebdd`](https://github.com/nodejs/node/commit/b51946ebdd)] - **tools**: fix github reporter appended multiple times (Moshe Atlow) [#49199](https://github.com/nodejs/node/pull/49199)
* \[[`ae40cb1612`](https://github.com/nodejs/node/commit/ae40cb1612)] - **url**: validate `pathToFileURL(path)` argument as string (LiviaMedeiros) [#49161](https://github.com/nodejs/node/pull/49161)
* \[[`e787673dcf`](https://github.com/nodejs/node/commit/e787673dcf)] - **url**: handle unicode hostname if empty (Yagiz Nizipli) [#49396](https://github.com/nodejs/node/pull/49396)
* \[[`6ee74be87f`](https://github.com/nodejs/node/commit/6ee74be87f)] - **vm**: store MicrotaskQueue in ContextifyContext directly (Joyee Cheung) [#48982](https://github.com/nodejs/node/pull/48982)
* \[[`0179c6dc8f`](https://github.com/nodejs/node/commit/0179c6dc8f)] - **worker**: protect against user mutating well-known prototypes (Antoine du Hamel) [#49270](https://github.com/nodejs/node/pull/49270)

<a id="20.6.1"></a>

## 2023-09-08, Version 20.6.1 (Current), @RafaelGSS

### Commits

* \[[`8acbe6d8e8`](https://github.com/nodejs/node/commit/8acbe6d8e8)] - **esm**: fix loading of CJS modules from ESM (Antoine du Hamel) [#49500](https://github.com/nodejs/node/pull/49500)

<a id="20.6.0"></a>

## 2023-09-04, Version 20.6.0 (Current), @juanarbol prepared by @UlisesGascon

### Notable changes

#### built-in `.env` file support

Starting from Node.js v20.6.0, Node.js supports `.env` files for configuring environment variables.

Your configuration file should follow the INI file format, with each line containing a key-value pair for an environment variable.
To initialize your Node.js application with predefined configurations, use the following CLI command: `node --env-file=config.env index.js`.

For example, you can access the following environment variable using `process.env.PASSWORD` when your application is initialized:

```text
PASSWORD=nodejs
```

In addition to environment variables, this change allows you to define your `NODE_OPTIONS` directly in the `.env` file, eliminating the need to include it in your `package.json`.

This feature was contributed by Yagiz Nizipli in [#48890](https://github.com/nodejs/node/pull/48890).

#### `import.meta.resolve` unflagged

In ES modules, [`import.meta.resolve(specifier)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/import.meta/resolve) can be used to get an absolute URL string to which `specifier` resolves, similar to `require.resolve` in CommonJS. This aligns Node.js with browsers and other server-side runtimes.

This feature was contributed by Guy Bedford in <https://github.com/nodejs/node/pull/49028>

#### New `node:module` API `register` for module customization hooks; new `initialize` hook

There is a new API `register` available on `node:module` to specify a file that exports module customization hooks, and pass data to the hooks, and establish communication channels with them. The “define the file with the hooks” part was previously handled by a flag `--experimental-loader`, but when the hooks moved into a dedicated thread in 20.0.0 there was a need to provide a way to communicate between the main (application) thread and the hooks thread. This can now be done by calling `register` from the main thread and passing data, including `MessageChannel` instances.

We encourage users to migrate to an approach that uses [`--import`](https://nodejs.org/api/cli.html#--importmodule) with `register`, such as:

```bash
node --import ./file-that-calls-register.js ./app.js
```

Using `--import` ensures that the customization hooks are registered before any application code runs, even the entry point.

This feature was contributed by João Lenon and Jacob Smith in <https://github.com/nodejs/node/pull/46826>, Izaak Schroeder and Jacob Smith in <https://github.com/nodejs/node/pull/48842> and <https://github.com/nodejs/node/pull/48559>

#### Module customization `load` hook can now support CommonJS

Authors of module customization hooks can how handle both ES module and CommonJS sources in the `load` hook. This works for CommonJS modules referenced via either `import` or `require`, so long as [the main entry point of the application is handled by the ES module loader](https://nodejs.org/api/cli.html#program-entry-point) (such as because the entry point is an ES module file, or if the `--import` flag is passed). This should simplify the customization of the Node.js module loading process, as package authors can customize more of Node.js without relying on deprecated APIs such as `require.extensions`.

This feature was contributed by Antoine du Hamel in <https://github.com/nodejs/node/pull/47999>

#### Node.js C++ addons now have experimental support for cppgc (Oilpan), a C++ garbage collection library in V8.

Now when Node.js starts up, it makes sure that there is a `v8::CppHeap` attached to the V8 isolate. This enables users to allocate in the `v8::CppHeap` using `<cppgc/*>` headers from V8, which are now also included into the Node.js headers available to addons. Note that since Node.js only bundles the cppgc library coming from V8, [the ABI stability](https://nodejs.org/en/docs/guides/abi-stability#abi-stability-in-nodejs) of cppgc is currently not guaranteed in semver-minor and -patch updates, but we do not expect the ABI to break often, as it has been stable and battle-tested in Chromium for years. We may consider including cppgc into the ABI stability guarantees when it gets enough adoption internally and externally.

To help addon authors create JavaScript-to-C++ references of which V8's garbage collector can be aware, a helper function [`node::SetCppgcReference(isolate, js_object, cppgc_object)`](https://github.com/nodejs/node/blob/v20.6.0/test/addons/cppgc-object/binding.cc) has been added to `node.h`. V8 may provide a native alternative in the future, which could then replace this Node.js-specific helper. In the mean time, users can use this API to avoid having to hard-code the layout of JavaScript wrapper objects. An example of how to create garbage-collected C++ objects in the unified heap and wrap it in a JavaScript object can be found in the [Node.js addon tests](https://github.com/nodejs/node/blob/v20.6.0/test/addons/cppgc-object/binding.cc).

The existing `node::ObjectWrap` helper would continue to work, while cppgc-based object management serves as an alternative with some advantages mentioned in [the V8 blog post about Oilpan](https://v8.dev/blog/oilpan-library).

This feature was contributed by Daryl Haresign and Joyee Cheung in <https://github.com/nodejs/node/pull/48660> and <https://github.com/nodejs/node/pull/45704>.

#### Other notable changes

* \[[`d6862b085c`](https://github.com/nodejs/node/commit/d6862b085c)] - **deps**: V8: cherry-pick 93275031284c (Joyee Cheung) [#48660](https://github.com/nodejs/node/pull/48660)
* \[[`00fc8bb8b3`](https://github.com/nodejs/node/commit/00fc8bb8b3)] - **doc**: add rluvaton to collaborators (Raz Luvaton) [#49215](https://github.com/nodejs/node/pull/49215)
* \[[`d649339abd`](https://github.com/nodejs/node/commit/d649339abd)] - **doc**: add new TSC members (Michael Dawson) [#48841](https://github.com/nodejs/node/pull/48841)
* \[[`67f9896247`](https://github.com/nodejs/node/commit/67f9896247)] - **(SEMVER-MINOR)** **inspector**: open add `SymbolDispose` (Chemi Atlow) [#48765](https://github.com/nodejs/node/pull/48765)
* \[[`5aef593db3`](https://github.com/nodejs/node/commit/5aef593db3)] - **module**: implement `register` utility (João Lenon) [#46826](https://github.com/nodejs/node/pull/46826)

### Commits

* \[[`771abcb5da`](https://github.com/nodejs/node/commit/771abcb5da)] - **benchmark**: add benchmarks for the test\_runner (Raz Luvaton) [#48931](https://github.com/nodejs/node/pull/48931)
* \[[`6b27bb0dab`](https://github.com/nodejs/node/commit/6b27bb0dab)] - **benchmark**: add pm startup benchmark (Rafael Gonzaga) [#48905](https://github.com/nodejs/node/pull/48905)
* \[[`1f35c0ca55`](https://github.com/nodejs/node/commit/1f35c0ca55)] - **child\_process**: harden against prototype pollution (Livia Medeiros) [#48726](https://github.com/nodejs/node/pull/48726)
* \[[`d6862b085c`](https://github.com/nodejs/node/commit/d6862b085c)] - **deps**: V8: cherry-pick 93275031284c (Joyee Cheung) [#48660](https://github.com/nodejs/node/pull/48660)
* \[[`f71e383948`](https://github.com/nodejs/node/commit/f71e383948)] - **deps**: update simdutf to 3.2.17 (Node.js GitHub Bot) [#49019](https://github.com/nodejs/node/pull/49019)
* \[[`e14f0456ae`](https://github.com/nodejs/node/commit/e14f0456ae)] - **deps**: update googletest to 7e33b6a (Node.js GitHub Bot) [#49034](https://github.com/nodejs/node/pull/49034)
* \[[`bfaa0fb500`](https://github.com/nodejs/node/commit/bfaa0fb500)] - **deps**: update zlib to 1.2.13.1-motley-526382e (Node.js GitHub Bot) [#49033](https://github.com/nodejs/node/pull/49033)
* \[[`b79c652c85`](https://github.com/nodejs/node/commit/b79c652c85)] - **deps**: update undici to 5.23.0 (Node.js GitHub Bot) [#49021](https://github.com/nodejs/node/pull/49021)
* \[[`6ead86145c`](https://github.com/nodejs/node/commit/6ead86145c)] - **deps**: update googletest to c875c4e (Node.js GitHub Bot) [#48964](https://github.com/nodejs/node/pull/48964)
* \[[`4b0e50501e`](https://github.com/nodejs/node/commit/4b0e50501e)] - **deps**: update ada to 2.6.0 (Node.js GitHub Bot) [#48896](https://github.com/nodejs/node/pull/48896)
* \[[`d960ee0ba3`](https://github.com/nodejs/node/commit/d960ee0ba3)] - **deps**: upgrade npm to 9.8.1 (npm team) [#48838](https://github.com/nodejs/node/pull/48838)
* \[[`d92b0139ca`](https://github.com/nodejs/node/commit/d92b0139ca)] - **deps**: update zlib to 1.2.13.1-motley-61dc0bd (Node.js GitHub Bot) [#48788](https://github.com/nodejs/node/pull/48788)
* \[[`2a7835c376`](https://github.com/nodejs/node/commit/2a7835c376)] - **deps**: V8: cherry-pick 9f4b7699f68e (Joyee Cheung) [#48830](https://github.com/nodejs/node/pull/48830)
* \[[`c8e17829ac`](https://github.com/nodejs/node/commit/c8e17829ac)] - **deps**: V8: cherry-pick c1a54d5ffcd1 (Joyee Cheung) [#48830](https://github.com/nodejs/node/pull/48830)
* \[[`318e075b6f`](https://github.com/nodejs/node/commit/318e075b6f)] - **deps**: update googletest to cc36671 (Node.js GitHub Bot) [#48789](https://github.com/nodejs/node/pull/48789)
* \[[`114e088267`](https://github.com/nodejs/node/commit/114e088267)] - **diagnostics\_channel**: fix last subscriber removal (Gabriel Schulhof) [#48933](https://github.com/nodejs/node/pull/48933)
* \[[`00fc8bb8b3`](https://github.com/nodejs/node/commit/00fc8bb8b3)] - **doc**: add rluvaton to collaborators (Raz Luvaton) [#49215](https://github.com/nodejs/node/pull/49215)
* \[[`21949c45b6`](https://github.com/nodejs/node/commit/21949c45b6)] - **doc**: add print results for examples in `WebStreams` (Jungku Lee) [#49143](https://github.com/nodejs/node/pull/49143)
* \[[`032107a6fe`](https://github.com/nodejs/node/commit/032107a6fe)] - **doc**: fix `Type` notation in webstreams (Deokjin Kim) [#49121](https://github.com/nodejs/node/pull/49121)
* \[[`91d41e7c5a`](https://github.com/nodejs/node/commit/91d41e7c5a)] - **doc**: fix name of the flag in `initialize()` docs (Antoine du Hamel) [#49158](https://github.com/nodejs/node/pull/49158)
* \[[`aa4caf810e`](https://github.com/nodejs/node/commit/aa4caf810e)] - **doc**: make the NODE\_VERSION\_IS\_RELEASE revert clear (Rafael Gonzaga) [#49114](https://github.com/nodejs/node/pull/49114)
* \[[`f888a1dbe3`](https://github.com/nodejs/node/commit/f888a1dbe3)] - **doc**: update process.binding deprecation text (Tobias Nießen) [#49086](https://github.com/nodejs/node/pull/49086)
* \[[`89fa3faf92`](https://github.com/nodejs/node/commit/89fa3faf92)] - **doc**: update with latest security release (Rafael Gonzaga) [#49085](https://github.com/nodejs/node/pull/49085)
* \[[`3d36e7a941`](https://github.com/nodejs/node/commit/3d36e7a941)] - **doc**: add description for `--port` flag of `node inspect` (Michael Bianco) [#48785](https://github.com/nodejs/node/pull/48785)
* \[[`e9d9ca12a3`](https://github.com/nodejs/node/commit/e9d9ca12a3)] - **doc**: add missing period (Rich Trott) [#49094](https://github.com/nodejs/node/pull/49094)
* \[[`7e7b554de0`](https://github.com/nodejs/node/commit/7e7b554de0)] - **doc**: add ESM examples in http.md (btea) [#47763](https://github.com/nodejs/node/pull/47763)
* \[[`48f8ccfd54`](https://github.com/nodejs/node/commit/48f8ccfd54)] - **doc**: detailed description of keystrokes Ctrl-Y and Meta-Y (Ray) [#43529](https://github.com/nodejs/node/pull/43529)
* \[[`195885c8f8`](https://github.com/nodejs/node/commit/195885c8f8)] - **doc**: add "type" to test runner event details (Phil Nash) [#49014](https://github.com/nodejs/node/pull/49014)
* \[[`6ce25f8415`](https://github.com/nodejs/node/commit/6ce25f8415)] - **doc**: reserve 118 for Electron 27 (David Sanders) [#49023](https://github.com/nodejs/node/pull/49023)
* \[[`9c26c0f296`](https://github.com/nodejs/node/commit/9c26c0f296)] - **doc**: clarify use of process.env in worker threads on Windows (Daeyeon Jeong) [#49008](https://github.com/nodejs/node/pull/49008)
* \[[`7186e02aa0`](https://github.com/nodejs/node/commit/7186e02aa0)] - **doc**: remove v14 mention (Rafael Gonzaga) [#49005](https://github.com/nodejs/node/pull/49005)
* \[[`9641ac6c65`](https://github.com/nodejs/node/commit/9641ac6c65)] - **doc**: drop github actions check in sec release process (Rafael Gonzaga) [#48978](https://github.com/nodejs/node/pull/48978)
* \[[`f3d62abb19`](https://github.com/nodejs/node/commit/f3d62abb19)] - **doc**: improved joinDuplicateHeaders definition (Matteo Bianchi) [#48859](https://github.com/nodejs/node/pull/48859)
* \[[`0db104a08b`](https://github.com/nodejs/node/commit/0db104a08b)] - **doc**: fix second parameter name of `events.addAbortListener` (Deokjin Kim) [#48922](https://github.com/nodejs/node/pull/48922)
* \[[`5173c559b7`](https://github.com/nodejs/node/commit/5173c559b7)] - **doc**: add new reporter events to custom reporter examples (Chemi Atlow) [#48903](https://github.com/nodejs/node/pull/48903)
* \[[`660da785e6`](https://github.com/nodejs/node/commit/660da785e6)] - **doc**: run license-builder (github-actions\[bot]) [#48898](https://github.com/nodejs/node/pull/48898)
* \[[`092f9fe92a`](https://github.com/nodejs/node/commit/092f9fe92a)] - **doc**: change duration to duration\_ms on test documentation (Ardi\_Nugraha) [#48892](https://github.com/nodejs/node/pull/48892)
* \[[`5e4730858d`](https://github.com/nodejs/node/commit/5e4730858d)] - **doc**: improve requireHostHeader (Guido Penta) [#48860](https://github.com/nodejs/node/pull/48860)
* \[[`045e3c549a`](https://github.com/nodejs/node/commit/045e3c549a)] - **doc**: add ver of 18.x where Node-api 9 is supported (Michael Dawson) [#48876](https://github.com/nodejs/node/pull/48876)
* \[[`c20d35df34`](https://github.com/nodejs/node/commit/c20d35df34)] - **doc**: include experimental features assessment (Rafael Gonzaga) [#48824](https://github.com/nodejs/node/pull/48824)
* \[[`d649339abd`](https://github.com/nodejs/node/commit/d649339abd)] - **doc**: add new TSC members (Michael Dawson) [#48841](https://github.com/nodejs/node/pull/48841)
* \[[`aeac327f2b`](https://github.com/nodejs/node/commit/aeac327f2b)] - **doc**: refactor node-api support matrix (Michael Dawson) [#48774](https://github.com/nodejs/node/pull/48774)
* \[[`388c7d9232`](https://github.com/nodejs/node/commit/388c7d9232)] - **doc**: declare `path` on example of `async_hooks.executionAsyncId()` (Deokjin Kim) [#48556](https://github.com/nodejs/node/pull/48556)
* \[[`fe20528c8e`](https://github.com/nodejs/node/commit/fe20528c8e)] - **doc**: remove the . in the end to reduce confusing (Jason) [#48719](https://github.com/nodejs/node/pull/48719)
* \[[`e69c8e173f`](https://github.com/nodejs/node/commit/e69c8e173f)] - **doc**: nodejs-social over nodejs/tweet (Rafael Gonzaga) [#48769](https://github.com/nodejs/node/pull/48769)
* \[[`ea547849fd`](https://github.com/nodejs/node/commit/ea547849fd)] - **doc**: expand on squashing and rebasing to land a PR (Chengzhong Wu) [#48751](https://github.com/nodejs/node/pull/48751)
* \[[`31442b96a5`](https://github.com/nodejs/node/commit/31442b96a5)] - **esm**: fix `globalPreload` warning (Antoine du Hamel) [#49069](https://github.com/nodejs/node/pull/49069)
* \[[`eb1215878b`](https://github.com/nodejs/node/commit/eb1215878b)] - **esm**: unflag import.meta.resolve (Guy Bedford) [#49028](https://github.com/nodejs/node/pull/49028)
* \[[`57b24a34e6`](https://github.com/nodejs/node/commit/57b24a34e6)] - **esm**: import.meta.resolve exact module not found errors should return (Guy Bedford) [#49038](https://github.com/nodejs/node/pull/49038)
* \[[`f23b2a3066`](https://github.com/nodejs/node/commit/f23b2a3066)] - **esm**: protect `ERR_UNSUPPORTED_DIR_IMPORT` against prototype pollution (Antoine du Hamel) [#49060](https://github.com/nodejs/node/pull/49060)
* \[[`386e826a56`](https://github.com/nodejs/node/commit/386e826a56)] - **esm**: add `initialize` hook, integrate with `register` (Izaak Schroeder) [#48842](https://github.com/nodejs/node/pull/48842)
* \[[`74a2e1e0ab`](https://github.com/nodejs/node/commit/74a2e1e0ab)] - **esm**: fix typo `parentUrl` -> `parentURL` (Antoine du Hamel) [#48999](https://github.com/nodejs/node/pull/48999)
* \[[`0a4f7c669a`](https://github.com/nodejs/node/commit/0a4f7c669a)] - **esm**: unflag `Module.register` and allow nested loader `import()` (Izaak Schroeder) [#48559](https://github.com/nodejs/node/pull/48559)
* \[[`a5597470ce`](https://github.com/nodejs/node/commit/a5597470ce)] - **esm**: add back `globalPreload` tests and fix failing ones (Antoine du Hamel) [#48779](https://github.com/nodejs/node/pull/48779)
* \[[`d568600b42`](https://github.com/nodejs/node/commit/d568600b42)] - **events**: remove weak listener for event target (Raz Luvaton) [#48952](https://github.com/nodejs/node/pull/48952)
* \[[`3d942d9842`](https://github.com/nodejs/node/commit/3d942d9842)] - **fs**: fix readdir recursive sync & callback (Ethan Arrowood) [#48698](https://github.com/nodejs/node/pull/48698)
* \[[`c14ff69d69`](https://github.com/nodejs/node/commit/c14ff69d69)] - **fs**: mention `URL` in NUL character error message (LiviaMedeiros) [#48828](https://github.com/nodejs/node/pull/48828)
* \[[`d634d781d7`](https://github.com/nodejs/node/commit/d634d781d7)] - **fs**: make `mkdtemp` accept buffers and URL (LiviaMedeiros) [#48828](https://github.com/nodejs/node/pull/48828)
* \[[`4515a285a4`](https://github.com/nodejs/node/commit/4515a285a4)] - **fs**: remove redundant `nullCheck` (Livia Medeiros) [#48826](https://github.com/nodejs/node/pull/48826)
* \[[`742597b14a`](https://github.com/nodejs/node/commit/742597b14a)] - **http**: start connections checking interval on listen (Paolo Insogna) [#48611](https://github.com/nodejs/node/pull/48611)
* \[[`67f9896247`](https://github.com/nodejs/node/commit/67f9896247)] - **(SEMVER-MINOR)** **inspector**: open add `SymbolDispose` (Chemi Atlow) [#48765](https://github.com/nodejs/node/pull/48765)
* \[[`b66a3c1c96`](https://github.com/nodejs/node/commit/b66a3c1c96)] - **lib**: fix MIME overmatch in data URLs (André Alves) [#49104](https://github.com/nodejs/node/pull/49104)
* \[[`dca8678a22`](https://github.com/nodejs/node/commit/dca8678a22)] - **lib**: fix to add resolve() before return at Blob.stream()'s source.pull() (bellbind) [#48935](https://github.com/nodejs/node/pull/48935)
* \[[`420b85c00f`](https://github.com/nodejs/node/commit/420b85c00f)] - **lib**: remove invalid parameter to toASCII (Yagiz Nizipli) [#48878](https://github.com/nodejs/node/pull/48878)
* \[[`a12ce11b09`](https://github.com/nodejs/node/commit/a12ce11b09)] - **lib,permission**: drop repl autocomplete when pm enabled (Rafael Gonzaga) [#48920](https://github.com/nodejs/node/pull/48920)
* \[[`458eaf5e75`](https://github.com/nodejs/node/commit/458eaf5e75)] - **meta**: bump github/codeql-action from 2.20.1 to 2.21.2 (dependabot\[bot]) [#48986](https://github.com/nodejs/node/pull/48986)
* \[[`4f88cb10e0`](https://github.com/nodejs/node/commit/4f88cb10e0)] - **meta**: bump step-security/harden-runner from 2.4.1 to 2.5.0 (dependabot\[bot]) [#48985](https://github.com/nodejs/node/pull/48985)
* \[[`22fc2a6ec6`](https://github.com/nodejs/node/commit/22fc2a6ec6)] - **meta**: bump actions/setup-node from 3.6.0 to 3.7.0 (dependabot\[bot]) [#48984](https://github.com/nodejs/node/pull/48984)
* \[[`40103adabd`](https://github.com/nodejs/node/commit/40103adabd)] - **meta**: bump actions/setup-python from 4.6.1 to 4.7.0 (dependabot\[bot]) [#48983](https://github.com/nodejs/node/pull/48983)
* \[[`84c0c6848c`](https://github.com/nodejs/node/commit/84c0c6848c)] - **meta**: add mailmap entry for atlowChemi (Chemi Atlow) [#48810](https://github.com/nodejs/node/pull/48810)
* \[[`1a6e9450b8`](https://github.com/nodejs/node/commit/1a6e9450b8)] - **module**: make CJS load from ESM loader (Antoine du Hamel) [#47999](https://github.com/nodejs/node/pull/47999)
* \[[`a5322c4b4a`](https://github.com/nodejs/node/commit/a5322c4b4a)] - **module**: ensure successful import returns the same result (Antoine du Hamel) [#46662](https://github.com/nodejs/node/pull/46662)
* \[[`5aef593db3`](https://github.com/nodejs/node/commit/5aef593db3)] - **module**: implement `register` utility (João Lenon) [#46826](https://github.com/nodejs/node/pull/46826)
* \[[`015c4f788d`](https://github.com/nodejs/node/commit/015c4f788d)] - **node-api**: avoid macro redefinition (Tobias Nießen) [#48879](https://github.com/nodejs/node/pull/48879)
* \[[`53ee98566b`](https://github.com/nodejs/node/commit/53ee98566b)] - **permission**: move PrintTree into unnamed namespace (Tobias Nießen) [#48874](https://github.com/nodejs/node/pull/48874)
* \[[`30ea480135`](https://github.com/nodejs/node/commit/30ea480135)] - **permission**: fix data types in PrintTree (Tobias Nießen) [#48770](https://github.com/nodejs/node/pull/48770)
* \[[`8380800375`](https://github.com/nodejs/node/commit/8380800375)] - **readline**: add paste bracket mode (Jakub Jankiewicz) [#47150](https://github.com/nodejs/node/pull/47150)
* \[[`bc009d0c10`](https://github.com/nodejs/node/commit/bc009d0c10)] - **sea**: add support for V8 bytecode-only caching (Darshan Sen) [#48191](https://github.com/nodejs/node/pull/48191)
* \[[`f2f4ce9e29`](https://github.com/nodejs/node/commit/f2f4ce9e29)] - **src**: use effective cppgc wrapper id to deduce non-cppgc id (Joyee Cheung) [#48660](https://github.com/nodejs/node/pull/48660)
* \[[`bf7ff369f6`](https://github.com/nodejs/node/commit/bf7ff369f6)] - **src**: add built-in `.env` file support (Yagiz Nizipli) [#48890](https://github.com/nodejs/node/pull/48890)
* \[[`8d6948f8e2`](https://github.com/nodejs/node/commit/8d6948f8e2)] - **src**: remove duplicated code in `GenerateSingleExecutableBlob()` (Jungku Lee) [#49119](https://github.com/nodejs/node/pull/49119)
* \[[`b030004cee`](https://github.com/nodejs/node/commit/b030004cee)] - **src**: refactor vector writing in snapshot builder (Joyee Cheung) [#48851](https://github.com/nodejs/node/pull/48851)
* \[[`497df8288d`](https://github.com/nodejs/node/commit/497df8288d)] - **src**: add ability to overload fast api functions (Yagiz Nizipli) [#48993](https://github.com/nodejs/node/pull/48993)
* \[[`e5b0dfa359`](https://github.com/nodejs/node/commit/e5b0dfa359)] - **src**: remove redundant code for uv\_handle\_type (Jungku Lee) [#49061](https://github.com/nodejs/node/pull/49061)
* \[[`f126b9e3d1`](https://github.com/nodejs/node/commit/f126b9e3d1)] - **src**: modernize use-equals-default (Jason) [#48735](https://github.com/nodejs/node/pull/48735)
* \[[`db4370fc3e`](https://github.com/nodejs/node/commit/db4370fc3e)] - **src**: avoid string copy in BuiltinLoader::GetBuiltinIds (Yagiz Nizipli) [#48721](https://github.com/nodejs/node/pull/48721)
* \[[`9d13503c4e`](https://github.com/nodejs/node/commit/9d13503c4e)] - **src**: fix callback\_queue.h missing header (Jason) [#48733](https://github.com/nodejs/node/pull/48733)
* \[[`6c389df3aa`](https://github.com/nodejs/node/commit/6c389df3aa)] - **src**: cast v8::Object::GetInternalField() return value to v8::Value (Joyee Cheung) [#48943](https://github.com/nodejs/node/pull/48943)
* \[[`7b9adff0be`](https://github.com/nodejs/node/commit/7b9adff0be)] - **src**: do not pass user input to format string (Antoine du Hamel) [#48973](https://github.com/nodejs/node/pull/48973)
* \[[`e0fdb7b092`](https://github.com/nodejs/node/commit/e0fdb7b092)] - **src**: remove ContextEmbedderIndex::kBindingDataStoreIndex (Joyee Cheung) [#48836](https://github.com/nodejs/node/pull/48836)
* \[[`578c3d1e14`](https://github.com/nodejs/node/commit/578c3d1e14)] - **src**: use ARES\_SUCCESS instead of 0 (Hyunjin Kim) [#48834](https://github.com/nodejs/node/pull/48834)
* \[[`ed23426aac`](https://github.com/nodejs/node/commit/ed23426aac)] - **src**: save the performance milestone time origin in the AliasedArray (Joyee Cheung) [#48708](https://github.com/nodejs/node/pull/48708)
* \[[`5dec186663`](https://github.com/nodejs/node/commit/5dec186663)] - **src**: support snapshot in single executable applications (Joyee Cheung) [#46824](https://github.com/nodejs/node/pull/46824)
* \[[`d759d4f631`](https://github.com/nodejs/node/commit/d759d4f631)] - **src**: remove unnecessary temporary creation (Jason) [#48734](https://github.com/nodejs/node/pull/48734)
* \[[`409cc692db`](https://github.com/nodejs/node/commit/409cc692db)] - **src**: fix nullptr access on realm (Jan Olaf Krems) [#48802](https://github.com/nodejs/node/pull/48802)
* \[[`07d0fd61b1`](https://github.com/nodejs/node/commit/07d0fd61b1)] - **src**: remove OnScopeLeaveImpl's move assignment overload (Jason) [#48732](https://github.com/nodejs/node/pull/48732)
* \[[`41cc3efa23`](https://github.com/nodejs/node/commit/41cc3efa23)] - **src**: use string\_view for utf-8 string creation (Yagiz Nizipli) [#48722](https://github.com/nodejs/node/pull/48722)
* \[[`62a46d9335`](https://github.com/nodejs/node/commit/62a46d9335)] - **src,permission**: restrict by default when pm enabled (Rafael Gonzaga) [#48907](https://github.com/nodejs/node/pull/48907)
* \[[`099159ce04`](https://github.com/nodejs/node/commit/099159ce04)] - **src,tools**: initialize cppgc (Daryl Haresign) [#48660](https://github.com/nodejs/node/pull/48660)
* \[[`600c08d197`](https://github.com/nodejs/node/commit/600c08d197)] - **stream**: improve WebStreams performance (Raz Luvaton) [#49089](https://github.com/nodejs/node/pull/49089)
* \[[`609b25fa99`](https://github.com/nodejs/node/commit/609b25fa99)] - **stream**: implement ReadableStream.from (Debadree Chatterjee) [#48395](https://github.com/nodejs/node/pull/48395)
* \[[`750cca2738`](https://github.com/nodejs/node/commit/750cca2738)] - **test**: use `tmpdir.resolve()` (Livia Medeiros) [#49128](https://github.com/nodejs/node/pull/49128)
* \[[`6595367649`](https://github.com/nodejs/node/commit/6595367649)] - **test**: use `tmpdir.resolve()` (Livia Medeiros) [#49127](https://github.com/nodejs/node/pull/49127)
* \[[`661b055e75`](https://github.com/nodejs/node/commit/661b055e75)] - **test**: use `tmpdir.resolve()` in fs tests (Livia Medeiros) [#49126](https://github.com/nodejs/node/pull/49126)
* \[[`b3c56d206f`](https://github.com/nodejs/node/commit/b3c56d206f)] - **test**: use `tmpdir.resolve()` in fs tests (Livia Medeiros) [#49125](https://github.com/nodejs/node/pull/49125)
* \[[`3ddb155d16`](https://github.com/nodejs/node/commit/3ddb155d16)] - **test**: fix assertion message in test\_async.c (Tobias Nießen) [#49146](https://github.com/nodejs/node/pull/49146)
* \[[`1d17c1032d`](https://github.com/nodejs/node/commit/1d17c1032d)] - **test**: refactor `test-esm-loader-hooks` for easier debugging (Antoine du Hamel) [#49131](https://github.com/nodejs/node/pull/49131)
* \[[`13bd7a0293`](https://github.com/nodejs/node/commit/13bd7a0293)] - **test**: add `tmpdir.resolve()` (Livia Medeiros) [#49079](https://github.com/nodejs/node/pull/49079)
* \[[`89b1bce56d`](https://github.com/nodejs/node/commit/89b1bce56d)] - **test**: document `fixtures.fileURL()` (Livia Medeiros) [#49083](https://github.com/nodejs/node/pull/49083)
* \[[`2fcb855c76`](https://github.com/nodejs/node/commit/2fcb855c76)] - **test**: reduce flakiness of `test-esm-loader-hooks` (Antoine du Hamel) [#49105](https://github.com/nodejs/node/pull/49105)
* \[[`7816e040df`](https://github.com/nodejs/node/commit/7816e040df)] - **test**: stabilize the inspector-open-dispose test (Chemi Atlow) [#49000](https://github.com/nodejs/node/pull/49000)
* \[[`e70e9747e4`](https://github.com/nodejs/node/commit/e70e9747e4)] - **test**: print instruction for creating missing snapshot in assertSnapshot (Raz Luvaton) [#48914](https://github.com/nodejs/node/pull/48914)
* \[[`669ac03520`](https://github.com/nodejs/node/commit/669ac03520)] - **test**: add `tmpdir.fileURL()` (Livia Medeiros) [#49040](https://github.com/nodejs/node/pull/49040)
* \[[`b945d7be35`](https://github.com/nodejs/node/commit/b945d7be35)] - **test**: use `spawn` and `spawnPromisified` instead of `exec` (Antoine du Hamel) [#48991](https://github.com/nodejs/node/pull/48991)
* \[[`b3a7427583`](https://github.com/nodejs/node/commit/b3a7427583)] - **test**: refactor `test-node-output-errors` (Antoine du Hamel) [#48992](https://github.com/nodejs/node/pull/48992)
* \[[`6c3e5c4d69`](https://github.com/nodejs/node/commit/6c3e5c4d69)] - **test**: use `fixtures.fileURL` when appropriate (Antoine du Hamel) [#48990](https://github.com/nodejs/node/pull/48990)
* \[[`9138b78bcb`](https://github.com/nodejs/node/commit/9138b78bcb)] - **test**: validate error code rather than message (Antoine du Hamel) [#48972](https://github.com/nodejs/node/pull/48972)
* \[[`b4ca4a6f80`](https://github.com/nodejs/node/commit/b4ca4a6f80)] - **test**: fix snapshot tests when cwd contains spaces or backslashes (Antoine du Hamel) [#48959](https://github.com/nodejs/node/pull/48959)
* \[[`d4398d458c`](https://github.com/nodejs/node/commit/d4398d458c)] - **test**: order `common.mjs` in ASCII order (Antoine du Hamel) [#48960](https://github.com/nodejs/node/pull/48960)
* \[[`b5991f5250`](https://github.com/nodejs/node/commit/b5991f5250)] - **test**: fix some assumptions in tests (Antoine du Hamel) [#48958](https://github.com/nodejs/node/pull/48958)
* \[[`62e23f83f9`](https://github.com/nodejs/node/commit/62e23f83f9)] - **test**: improve internal/worker/io.js coverage (Yoshiki Kurihara) [#42387](https://github.com/nodejs/node/pull/42387)
* \[[`314bd6095c`](https://github.com/nodejs/node/commit/314bd6095c)] - **test**: fix `es-module/test-esm-initialization` (Antoine du Hamel) [#48880](https://github.com/nodejs/node/pull/48880)
* \[[`3680a66df4`](https://github.com/nodejs/node/commit/3680a66df4)] - **test**: validate host with commas on url.parse (Yagiz Nizipli) [#48878](https://github.com/nodejs/node/pull/48878)
* \[[`24c3742372`](https://github.com/nodejs/node/commit/24c3742372)] - **test**: delete test-net-bytes-per-incoming-chunk-overhead (Michaël Zasso) [#48811](https://github.com/nodejs/node/pull/48811)
* \[[`e01cce50f5`](https://github.com/nodejs/node/commit/e01cce50f5)] - **test**: skip experimental test with pointer compression (Colin Ihrig) [#48738](https://github.com/nodejs/node/pull/48738)
* \[[`d5e93b1074`](https://github.com/nodejs/node/commit/d5e93b1074)] - **test**: fix flaky test-string-decode.js on x86 (Stefan Stojanovic) [#48750](https://github.com/nodejs/node/pull/48750)
* \[[`9136667d7d`](https://github.com/nodejs/node/commit/9136667d7d)] - **test\_runner**: dont set exit code on todo tests (Moshe Atlow) [#48929](https://github.com/nodejs/node/pull/48929)
* \[[`52c94908c0`](https://github.com/nodejs/node/commit/52c94908c0)] - **test\_runner**: fix todo and only in spec reporter (Moshe Atlow) [#48929](https://github.com/nodejs/node/pull/48929)
* \[[`5ccfb8d515`](https://github.com/nodejs/node/commit/5ccfb8d515)] - **test\_runner**: unwrap error message in TAP reporter (Colin Ihrig) [#48942](https://github.com/nodejs/node/pull/48942)
* \[[`fa19b0ed05`](https://github.com/nodejs/node/commit/fa19b0ed05)] - **test\_runner**: add `__proto__` null (Raz Luvaton) [#48663](https://github.com/nodejs/node/pull/48663)
* \[[`65d23940bf`](https://github.com/nodejs/node/commit/65d23940bf)] - **test\_runner**: fix async callback in describe not awaited (Raz Luvaton) [#48856](https://github.com/nodejs/node/pull/48856)
* \[[`4bd5e55b43`](https://github.com/nodejs/node/commit/4bd5e55b43)] - **test\_runner**: fix test\_runner `test:fail` event type (Ethan Arrowood) [#48854](https://github.com/nodejs/node/pull/48854)
* \[[`41058beed8`](https://github.com/nodejs/node/commit/41058beed8)] - **test\_runner**: call abort on test finish (Raz Luvaton) [#48827](https://github.com/nodejs/node/pull/48827)
* \[[`821b11a59f`](https://github.com/nodejs/node/commit/821b11a59f)] - **tls**: fix bugs of double TLS (rogertyang) [#48969](https://github.com/nodejs/node/pull/48969)
* \[[`4439327e73`](https://github.com/nodejs/node/commit/4439327e73)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#49122](https://github.com/nodejs/node/pull/49122)
* \[[`21dc844309`](https://github.com/nodejs/node/commit/21dc844309)] - **tools**: use spec reporter in actions (Moshe Atlow) [#49129](https://github.com/nodejs/node/pull/49129)
* \[[`3471758696`](https://github.com/nodejs/node/commit/3471758696)] - **tools**: use @reporters/github when running in github actions (Moshe Atlow) [#49129](https://github.com/nodejs/node/pull/49129)
* \[[`95a6e7661e`](https://github.com/nodejs/node/commit/95a6e7661e)] - **tools**: add @reporters/github to tools (Moshe Atlow) [#49129](https://github.com/nodejs/node/pull/49129)
* \[[`995cbf93eb`](https://github.com/nodejs/node/commit/995cbf93eb)] - **tools**: update eslint to 8.47.0 (Node.js GitHub Bot) [#49124](https://github.com/nodejs/node/pull/49124)
* \[[`ed065bc56e`](https://github.com/nodejs/node/commit/ed065bc56e)] - **tools**: update lint-md-dependencies to rollup\@3.27.2 (Node.js GitHub Bot) [#49035](https://github.com/nodejs/node/pull/49035)
* \[[`a5f37178ad`](https://github.com/nodejs/node/commit/a5f37178ad)] - **tools**: limit the number of auto start CIs (Antoine du Hamel) [#49067](https://github.com/nodejs/node/pull/49067)
* \[[`c1bd680f89`](https://github.com/nodejs/node/commit/c1bd680f89)] - **tools**: update eslint to 8.46.0 (Node.js GitHub Bot) [#48966](https://github.com/nodejs/node/pull/48966)
* \[[`e09a6b4821`](https://github.com/nodejs/node/commit/e09a6b4821)] - **tools**: update lint-md-dependencies to rollup\@3.27.0 (Node.js GitHub Bot) [#48965](https://github.com/nodejs/node/pull/48965)
* \[[`0cd2393bd9`](https://github.com/nodejs/node/commit/0cd2393bd9)] - **tools**: update lint-md-dependencies to rollup\@3.26.3 (Node.js GitHub Bot) [#48888](https://github.com/nodejs/node/pull/48888)
* \[[`41929a2906`](https://github.com/nodejs/node/commit/41929a2906)] - **tools**: update lint-md-dependencies to @rollup/plugin-commonjs\@25.0.3 (Node.js GitHub Bot) [#48791](https://github.com/nodejs/node/pull/48791)
* \[[`1761bdfbd9`](https://github.com/nodejs/node/commit/1761bdfbd9)] - **tools**: update eslint to 8.45.0 (Node.js GitHub Bot) [#48793](https://github.com/nodejs/node/pull/48793)
* \[[`b82f05cc4b`](https://github.com/nodejs/node/commit/b82f05cc4b)] - **typings**: update JSDoc for `cwd` in `child_process` (LiviaMedeiros) [#49029](https://github.com/nodejs/node/pull/49029)
* \[[`be7b511255`](https://github.com/nodejs/node/commit/be7b511255)] - **typings**: sync JSDoc with the actual implementation (Hyunjin Kim) [#48853](https://github.com/nodejs/node/pull/48853)
* \[[`45c860035d`](https://github.com/nodejs/node/commit/45c860035d)] - **url**: overload `canParse` V8 fast api method (Yagiz Nizipli) [#48993](https://github.com/nodejs/node/pull/48993)
* \[[`60d614157b`](https://github.com/nodejs/node/commit/60d614157b)] - **url**: fix `isURL` detection by checking `path` (Zhuo Zhang) [#48928](https://github.com/nodejs/node/pull/48928)
* \[[`b12c3b5240`](https://github.com/nodejs/node/commit/b12c3b5240)] - **url**: ensure getter access do not mutate observable symbols (Antoine du Hamel) [#48897](https://github.com/nodejs/node/pull/48897)
* \[[`30fb7b7535`](https://github.com/nodejs/node/commit/30fb7b7535)] - **url**: reduce `pathToFileURL` cpp calls (Yagiz Nizipli) [#48709](https://github.com/nodejs/node/pull/48709)
* \[[`c3dbd0c1e4`](https://github.com/nodejs/node/commit/c3dbd0c1e4)] - **util**: use `primordials.ArrayPrototypeIndexOf` instead of mutable method (DaisyDogs07) [#48586](https://github.com/nodejs/node/pull/48586)
* \[[`b79b2927ca`](https://github.com/nodejs/node/commit/b79b2927ca)] - **watch**: decrease debounce rate (Moshe Atlow) [#48926](https://github.com/nodejs/node/pull/48926)
* \[[`a12996298e`](https://github.com/nodejs/node/commit/a12996298e)] - **watch**: use debounce instead of throttle (Moshe Atlow) [#48926](https://github.com/nodejs/node/pull/48926)

<a id="20.5.1"></a>

## 2023-08-09, Version 20.5.1 (Current), @RafaelGSS

This is a security release.

### Notable Changes

The following CVEs are fixed in this release:

* [CVE-2023-32002](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32002):  Policies can be bypassed via Module.\_load (High)
* [CVE-2023-32558](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32558): process.binding() can bypass the permission model through path traversal (High)
* [CVE-2023-32004](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32004): Permission model can be bypassed by specifying a path traversal sequence in a Buffer (High)
* [CVE-2023-32006](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32006): Policies can be bypassed by module.constructor.createRequire (Medium)
* [CVE-2023-32559](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32559): Policies can be bypassed via process.binding (Medium)
* [CVE-2023-32005](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32005): fs.statfs can bypass the permission model (Low)
* [CVE-2023-32003](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-32003): fs.mkdtemp() and fs.mkdtempSync() can bypass the permission model (Low)
* OpenSSL Security Releases
  * [OpenSSL security advisory 14th July](https://mta.openssl.org/pipermail/openssl-announce/2023-July/000264.html).
  * [OpenSSL security advisory 19th July](https://mta.openssl.org/pipermail/openssl-announce/2023-July/000265.html).
  * [OpenSSL security advisory 31st July](https://mta.openssl.org/pipermail/openssl-announce/2023-July/000267.html)

More detailed information on each of the vulnerabilities can be found in [August 2023 Security Releases](https://nodejs.org/en/blog/vulnerability/august-2023-security-releases/) blog post.

### Commits

* \[[`92300b51b4`](https://github.com/nodejs/node/commit/92300b51b4)] - **deps**: update archs files for openssl-3.0.10+quic1 (Node.js GitHub Bot) [#49036](https://github.com/nodejs/node/pull/49036)
* \[[`559698abf2`](https://github.com/nodejs/node/commit/559698abf2)] - **deps**: upgrade openssl sources to quictls/openssl-3.0.10+quic1 (Node.js GitHub Bot) [#49036](https://github.com/nodejs/node/pull/49036)
* \[[`1bf3429e8e`](https://github.com/nodejs/node/commit/1bf3429e8e)] - **lib,permission**: restrict process.binding when pm is enabled (RafaelGSS) [nodejs-private/node-private#438](https://github.com/nodejs-private/node-private/pull/438)
* \[[`98a83a67e6`](https://github.com/nodejs/node/commit/98a83a67e6)] - **permission**: ensure to resolve path when calling mkdtemp (RafaelGSS) [nodejs-private/node-private#464](https://github.com/nodejs-private/node-private/pull/464)
* \[[`1f0cde466b`](https://github.com/nodejs/node/commit/1f0cde466b)] - **permission**: handle buffer path on fs calls (RafaelGSS) [nodejs-private/node-private#439](https://github.com/nodejs-private/node-private/pull/439)
* \[[`bd094d60ea`](https://github.com/nodejs/node/commit/bd094d60ea)] - **permission**: handle fstatfs and add pm supported list (RafaelGSS) [nodejs-private/node-private#441](https://github.com/nodejs-private/node-private/pull/441)
* \[[`7337d21484`](https://github.com/nodejs/node/commit/7337d21484)] - **policy**: handle Module.constructor and main.extensions bypass (RafaelGSS) [nodejs-private/node-private#417](https://github.com/nodejs-private/node-private/pull/417)
* \[[`cf348ec640`](https://github.com/nodejs/node/commit/cf348ec640)] - **policy**: disable process.binding() when enabled (Tobias Nießen) [nodejs-private/node-private#397](https://github.com/nodejs-private/node-private/pull/397)

<a id="20.5.0"></a>

## 2023-07-18, Version 20.5.0 (Current), @juanarbol

### Notable Changes

* \[[`45be29d89f`](https://github.com/nodejs/node/commit/45be29d89f)] - **doc**: add atlowChemi to collaborators (atlowChemi) [#48757](https://github.com/nodejs/node/pull/48757)
* \[[`a316808136`](https://github.com/nodejs/node/commit/a316808136)] - **(SEMVER-MINOR)** **events**: allow safely adding listener to abortSignal (Chemi Atlow) [#48596](https://github.com/nodejs/node/pull/48596)
* \[[`986b46a567`](https://github.com/nodejs/node/commit/986b46a567)] - **fs**: add a fast-path for readFileSync utf-8 (Yagiz Nizipli) [#48658](https://github.com/nodejs/node/pull/48658)
* \[[`0ef73ff6f0`](https://github.com/nodejs/node/commit/0ef73ff6f0)] - **(SEMVER-MINOR)** **test\_runner**: add shards support (Raz Luvaton) [#48639](https://github.com/nodejs/node/pull/48639)

### Commits

* \[[`eb0aba59b8`](https://github.com/nodejs/node/commit/eb0aba59b8)] - **bootstrap**: use correct descriptor for Symbol.{dispose,asyncDispose} (Jordan Harband) [#48703](https://github.com/nodejs/node/pull/48703)
* \[[`e2d0195dcf`](https://github.com/nodejs/node/commit/e2d0195dcf)] - **bootstrap**: hide experimental web globals with flag kNoBrowserGlobals (Chengzhong Wu) [#48545](https://github.com/nodejs/node/pull/48545)
* \[[`67a1018389`](https://github.com/nodejs/node/commit/67a1018389)] - **build**: do not pass target toolchain flags to host toolchain (Ivan Trubach) [#48597](https://github.com/nodejs/node/pull/48597)
* \[[`7d843bb942`](https://github.com/nodejs/node/commit/7d843bb942)] - **child\_process**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`4e08160f8c`](https://github.com/nodejs/node/commit/4e08160f8c)] - **child\_process**: support `Symbol.dispose` (Moshe Atlow) [#48551](https://github.com/nodejs/node/pull/48551)
* \[[`ef7728bf36`](https://github.com/nodejs/node/commit/ef7728bf36)] - **deps**: update nghttp2 to 1.55.1 (Node.js GitHub Bot) [#48790](https://github.com/nodejs/node/pull/48790)
* \[[`1454f02499`](https://github.com/nodejs/node/commit/1454f02499)] - **deps**: update nghttp2 to 1.55.0 (Node.js GitHub Bot) [#48746](https://github.com/nodejs/node/pull/48746)
* \[[`fa94debf46`](https://github.com/nodejs/node/commit/fa94debf46)] - **deps**: update minimatch to 9.0.3 (Node.js GitHub Bot) [#48704](https://github.com/nodejs/node/pull/48704)
* \[[`c73cfcc144`](https://github.com/nodejs/node/commit/c73cfcc144)] - **deps**: update acorn to 8.10.0 (Node.js GitHub Bot) [#48713](https://github.com/nodejs/node/pull/48713)
* \[[`b7a076a052`](https://github.com/nodejs/node/commit/b7a076a052)] - **deps**: V8: cherry-pick cb00db4dba6c (Keyhan Vakil) [#48671](https://github.com/nodejs/node/pull/48671)
* \[[`150e15536b`](https://github.com/nodejs/node/commit/150e15536b)] - **deps**: upgrade npm to 9.8.0 (npm team) [#48665](https://github.com/nodejs/node/pull/48665)
* \[[`c47b2cbd35`](https://github.com/nodejs/node/commit/c47b2cbd35)] - **dgram**: socket add `asyncDispose` (atlowChemi) [#48717](https://github.com/nodejs/node/pull/48717)
* \[[`002ce31cca`](https://github.com/nodejs/node/commit/002ce31cca)] - **dgram**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`45be29d89f`](https://github.com/nodejs/node/commit/45be29d89f)] - **doc**: add atlowChemi to collaborators (atlowChemi) [#48757](https://github.com/nodejs/node/pull/48757)
* \[[`69b55d2261`](https://github.com/nodejs/node/commit/69b55d2261)] - **doc**: fix ambiguity in http.md and https.md (an5er) [#48692](https://github.com/nodejs/node/pull/48692)
* \[[`caccb051c7`](https://github.com/nodejs/node/commit/caccb051c7)] - **doc**: clarify transform.\_transform() callback argument logic (Rafael Sofi-zada) [#48680](https://github.com/nodejs/node/pull/48680)
* \[[`999ae0c8c3`](https://github.com/nodejs/node/commit/999ae0c8c3)] - **doc**: fix copy node executable in Windows (Yoav Vainrich) [#48624](https://github.com/nodejs/node/pull/48624)
* \[[`7daefaeb44`](https://github.com/nodejs/node/commit/7daefaeb44)] - **doc**: drop \<b> of v20 changelog (Rafael Gonzaga) [#48649](https://github.com/nodejs/node/pull/48649)
* \[[`dd7ea3e1df`](https://github.com/nodejs/node/commit/dd7ea3e1df)] - **doc**: mention git node release prepare (Rafael Gonzaga) [#48644](https://github.com/nodejs/node/pull/48644)
* \[[`cc7809df21`](https://github.com/nodejs/node/commit/cc7809df21)] - **esm**: fix emit deprecation on legacy main resolve (Antoine du Hamel) [#48664](https://github.com/nodejs/node/pull/48664)
* \[[`67b13d1dba`](https://github.com/nodejs/node/commit/67b13d1dba)] - **events**: fix bug listenerCount don't compare wrapped listener (yuzheng14) [#48592](https://github.com/nodejs/node/pull/48592)
* \[[`a316808136`](https://github.com/nodejs/node/commit/a316808136)] - **(SEMVER-MINOR)** **events**: allow safely adding listener to abortSignal (Chemi Atlow) [#48596](https://github.com/nodejs/node/pull/48596)
* \[[`986b46a567`](https://github.com/nodejs/node/commit/986b46a567)] - **fs**: add a fast-path for readFileSync utf-8 (Yagiz Nizipli) [#48658](https://github.com/nodejs/node/pull/48658)
* \[[`e4333ac41f`](https://github.com/nodejs/node/commit/e4333ac41f)] - **http2**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`4a0b66e4f9`](https://github.com/nodejs/node/commit/4a0b66e4f9)] - **http2**: send RST code 8 on AbortController signal (Devraj Mehta) [#48573](https://github.com/nodejs/node/pull/48573)
* \[[`1295c76fce`](https://github.com/nodejs/node/commit/1295c76fce)] - **lib**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`dff6c25a36`](https://github.com/nodejs/node/commit/dff6c25a36)] - **meta**: bump actions/checkout from 3.5.2 to 3.5.3 (dependabot\[bot]) [#48625](https://github.com/nodejs/node/pull/48625)
* \[[`b5cb69ceaa`](https://github.com/nodejs/node/commit/b5cb69ceaa)] - **meta**: bump step-security/harden-runner from 2.4.0 to 2.4.1 (dependabot\[bot]) [#48626](https://github.com/nodejs/node/pull/48626)
* \[[`332e480b46`](https://github.com/nodejs/node/commit/332e480b46)] - **meta**: bump ossf/scorecard-action from 2.1.3 to 2.2.0 (dependabot\[bot]) [#48628](https://github.com/nodejs/node/pull/48628)
* \[[`25c5a0aaee`](https://github.com/nodejs/node/commit/25c5a0aaee)] - **meta**: bump github/codeql-action from 2.3.6 to 2.20.1 (dependabot\[bot]) [#48627](https://github.com/nodejs/node/pull/48627)
* \[[`6406f50ab1`](https://github.com/nodejs/node/commit/6406f50ab1)] - **module**: add SourceMap.lineLengths (Isaac Z. Schlueter) [#48461](https://github.com/nodejs/node/pull/48461)
* \[[`cfa69bd48c`](https://github.com/nodejs/node/commit/cfa69bd48c)] - **net**: server add `asyncDispose` (atlowChemi) [#48717](https://github.com/nodejs/node/pull/48717)
* \[[`ac11264cc5`](https://github.com/nodejs/node/commit/ac11264cc5)] - **net**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`82d6b13bf6`](https://github.com/nodejs/node/commit/82d6b13bf6)] - **permission**: add debug log when inserting fs nodes (Rafael Gonzaga) [#48677](https://github.com/nodejs/node/pull/48677)
* \[[`f4333b1cdd`](https://github.com/nodejs/node/commit/f4333b1cdd)] - **permission**: v8.writeHeapSnapshot and process.report (Rafael Gonzaga) [#48564](https://github.com/nodejs/node/pull/48564)
* \[[`f691dca6c9`](https://github.com/nodejs/node/commit/f691dca6c9)] - **readline**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`227e6bd898`](https://github.com/nodejs/node/commit/227e6bd898)] - **src**: pass syscall on `fs.readFileSync` fail operation (Yagiz Nizipli) [#48815](https://github.com/nodejs/node/pull/48815)
* \[[`a9a4b73653`](https://github.com/nodejs/node/commit/a9a4b73653)] - **src**: make BaseObject iteration order deterministic (Joyee Cheung) [#48702](https://github.com/nodejs/node/pull/48702)
* \[[`d99ea4845a`](https://github.com/nodejs/node/commit/d99ea4845a)] - **src**: remove kEagerCompile for CompileFunction (Keyhan Vakil) [#48671](https://github.com/nodejs/node/pull/48671)
* \[[`df363d0010`](https://github.com/nodejs/node/commit/df363d0010)] - **src**: deduplicate X509 getter implementations (Tobias Nießen) [#48563](https://github.com/nodejs/node/pull/48563)
* \[[`9cf2e1f55b`](https://github.com/nodejs/node/commit/9cf2e1f55b)] - **src,lib**: reducing C++ calls of esm legacy main resolve (Vinicius Lourenço) [#48325](https://github.com/nodejs/node/pull/48325)
* \[[`daeb21dde9`](https://github.com/nodejs/node/commit/daeb21dde9)] - **stream**: fix deadlock when pipeing to full sink (Robert Nagy) [#48691](https://github.com/nodejs/node/pull/48691)
* \[[`5a382d02d6`](https://github.com/nodejs/node/commit/5a382d02d6)] - **stream**: use addAbortListener (atlowChemi) [#48550](https://github.com/nodejs/node/pull/48550)
* \[[`6e82077dd4`](https://github.com/nodejs/node/commit/6e82077dd4)] - **test**: deflake test-net-throttle (Luigi Pinca) [#48599](https://github.com/nodejs/node/pull/48599)
* \[[`d378b2c822`](https://github.com/nodejs/node/commit/d378b2c822)] - **test**: move test-net-throttle to parallel (Luigi Pinca) [#48599](https://github.com/nodejs/node/pull/48599)
* \[[`dfa0aee5bf`](https://github.com/nodejs/node/commit/dfa0aee5bf)] - _**Revert**_ "**test**: remove test-crypto-keygen flaky designation" (Luigi Pinca) [#48652](https://github.com/nodejs/node/pull/48652)
* \[[`0ef73ff6f0`](https://github.com/nodejs/node/commit/0ef73ff6f0)] - **(SEMVER-MINOR)** **test\_runner**: add shards support (Raz Luvaton) [#48639](https://github.com/nodejs/node/pull/48639)
* \[[`e2442bb7ef`](https://github.com/nodejs/node/commit/e2442bb7ef)] - **timers**: support Symbol.dispose (Moshe Atlow) [#48633](https://github.com/nodejs/node/pull/48633)
* \[[`4398ade426`](https://github.com/nodejs/node/commit/4398ade426)] - **tools**: run fetch\_deps.py with Python 3 (Richard Lau) [#48729](https://github.com/nodejs/node/pull/48729)
* \[[`38ce95d054`](https://github.com/nodejs/node/commit/38ce95d054)] - **tools**: update doc to unist-util-select\@5.0.0 unist-util-visit\@5.0.0 (Node.js GitHub Bot) [#48714](https://github.com/nodejs/node/pull/48714)
* \[[`b25e78a998`](https://github.com/nodejs/node/commit/b25e78a998)] - **tools**: update lint-md-dependencies to rollup\@3.26.2 (Node.js GitHub Bot) [#48705](https://github.com/nodejs/node/pull/48705)
* \[[`a1f4ff7c59`](https://github.com/nodejs/node/commit/a1f4ff7c59)] - **tools**: update eslint to 8.44.0 (Node.js GitHub Bot) [#48632](https://github.com/nodejs/node/pull/48632)
* \[[`42dc6eb698`](https://github.com/nodejs/node/commit/42dc6eb698)] - **tools**: update lint-md-dependencies to rollup\@3.26.0 (Node.js GitHub Bot) [#48631](https://github.com/nodejs/node/pull/48631)
* \[[`07bfcc45ab`](https://github.com/nodejs/node/commit/07bfcc45ab)] - **url**: fix `canParse` false value when v8 optimizes (Yagiz Nizipli) [#48817](https://github.com/nodejs/node/pull/48817)

<a id="20.4.0"></a>

## 2023-07-05, Version 20.4.0 (Current), @RafaelGSS

### Notable Changes

#### Mock Timers

The new feature allows developers to write more reliable and predictable tests for time-dependent functionality.
It includes `MockTimers` with the ability to mock `setTimeout`, `setInterval` from `globals`, `node:timers`, and `node:timers/promises`.

The feature provides a simple API to advance time, enable specific timers, and release all timers.

```mjs
import assert from 'node:assert';
import { test } from 'node:test';

test('mocks setTimeout to be executed synchronously without having to actually wait for it', (context) => {
  const fn = context.mock.fn();
  // Optionally choose what to mock
  context.mock.timers.enable(['setTimeout']);
  const nineSecs = 9000;
  setTimeout(fn, nineSecs);

  const threeSeconds = 3000;
  context.mock.timers.tick(threeSeconds);
  context.mock.timers.tick(threeSeconds);
  context.mock.timers.tick(threeSeconds);

  assert.strictEqual(fn.mock.callCount(), 1);
});
```

This feature was contributed by Erick Wendel in [#47775](https://github.com/nodejs/node/pull/47775).

#### Support to the explicit resource management proposal

Node is adding support to the [explicit resource management](https://github.com/tc39/proposal-explicit-resource-management)
proposal to its resources allowing users of TypeScript/babel to use `using`/`await using` with
V8 support for everyone else on the way.

This feature was contributed by Moshe Atlow and Benjamin Gruenbaum in [#48518](https://github.com/nodejs/node/pull/48518).

#### Other notable changes

* \[[`fe333d2584`](https://github.com/nodejs/node/commit/fe333d2584)] - **crypto**: update root certificates to NSS 3.90 (Node.js GitHub Bot) [#48416](https://github.com/nodejs/node/pull/48416)
* \[[`60c2ea4e79`](https://github.com/nodejs/node/commit/60c2ea4e79)] - **doc**: add vmoroz to collaborators (Vladimir Morozov) [#48527](https://github.com/nodejs/node/pull/48527)
* \[[`5cacdf9e6b`](https://github.com/nodejs/node/commit/5cacdf9e6b)] - **doc**: add kvakil to collaborators (Keyhan Vakil) [#48449](https://github.com/nodejs/node/pull/48449)
* \[[`504d1d7bdc`](https://github.com/nodejs/node/commit/504d1d7bdc)] - **(SEMVER-MINOR)** **tls**: add ALPNCallback server option for dynamic ALPN negotiation (Tim Perry) [#45190](https://github.com/nodejs/node/pull/45190)

### Commits

* \[[`8a611a387f`](https://github.com/nodejs/node/commit/8a611a387f)] - **benchmark**: add bar.R (Rafael Gonzaga) [#47729](https://github.com/nodejs/node/pull/47729)
* \[[`12fa716cf9`](https://github.com/nodejs/node/commit/12fa716cf9)] - **benchmark**: refactor crypto oneshot (Filip Skokan) [#48267](https://github.com/nodejs/node/pull/48267)
* \[[`d6ecbde592`](https://github.com/nodejs/node/commit/d6ecbde592)] - **benchmark**: add crypto.create\*Key (Filip Skokan) [#48284](https://github.com/nodejs/node/pull/48284)
* \[[`e60b6dedd8`](https://github.com/nodejs/node/commit/e60b6dedd8)] - **bootstrap**: unify snapshot builder and embedder entry points (Joyee Cheung) [#48242](https://github.com/nodejs/node/pull/48242)
* \[[`40662957b1`](https://github.com/nodejs/node/commit/40662957b1)] - **bootstrap**: simplify initialization of source map handlers (Joyee Cheung) [#48304](https://github.com/nodejs/node/pull/48304)
* \[[`6551538079`](https://github.com/nodejs/node/commit/6551538079)] - **build**: fix `configure --link-module` (Richard Lau) [#48522](https://github.com/nodejs/node/pull/48522)
* \[[`f7f32089e7`](https://github.com/nodejs/node/commit/f7f32089e7)] - **build**: sync libuv header change (Jiawen Geng) [#48429](https://github.com/nodejs/node/pull/48429)
* \[[`f60205c915`](https://github.com/nodejs/node/commit/f60205c915)] - **build**: update action to close stale PRs (Michael Dawson) [#48196](https://github.com/nodejs/node/pull/48196)
* \[[`4f4d0b802e`](https://github.com/nodejs/node/commit/4f4d0b802e)] - **child\_process**: improve spawn performance on Linux (Keyhan Vakil) [#48523](https://github.com/nodejs/node/pull/48523)
* \[[`fe333d2584`](https://github.com/nodejs/node/commit/fe333d2584)] - **crypto**: update root certificates to NSS 3.90 (Node.js GitHub Bot) [#48416](https://github.com/nodejs/node/pull/48416)
* \[[`89aaf16237`](https://github.com/nodejs/node/commit/89aaf16237)] - **crypto**: remove OPENSSL\_FIPS guard for OpenSSL 3 (Richard Lau) [#48392](https://github.com/nodejs/node/pull/48392)
* \[[`6199e1946c`](https://github.com/nodejs/node/commit/6199e1946c)] - **deps**: upgrade to libuv 1.46.0 (Santiago Gimeno) [#48618](https://github.com/nodejs/node/pull/48618)
* \[[`1b2b930fda`](https://github.com/nodejs/node/commit/1b2b930fda)] - **deps**: add loong64 config into openssl gypi (Shi Pujin) [#48043](https://github.com/nodejs/node/pull/48043)
* \[[`ba8d048929`](https://github.com/nodejs/node/commit/ba8d048929)] - **deps**: update acorn to 8.9.0 (Node.js GitHub Bot) [#48484](https://github.com/nodejs/node/pull/48484)
* \[[`d96f921d06`](https://github.com/nodejs/node/commit/d96f921d06)] - **deps**: update zlib to 1.2.13.1-motley-f81f385 (Node.js GitHub Bot) [#48541](https://github.com/nodejs/node/pull/48541)
* \[[`ed1d047e8f`](https://github.com/nodejs/node/commit/ed1d047e8f)] - **deps**: update googletest to ec4fed9 (Node.js GitHub Bot) [#48538](https://github.com/nodejs/node/pull/48538)
* \[[`f43d718c67`](https://github.com/nodejs/node/commit/f43d718c67)] - **deps**: update minimatch to 9.0.2 (Node.js GitHub Bot) [#48542](https://github.com/nodejs/node/pull/48542)
* \[[`2f66147cbf`](https://github.com/nodejs/node/commit/2f66147cbf)] - **deps**: update corepack to 0.19.0 (Node.js GitHub Bot) [#48540](https://github.com/nodejs/node/pull/48540)
* \[[`d91b0fde73`](https://github.com/nodejs/node/commit/d91b0fde73)] - **deps**: V8: cherry-pick 1a782f6543ae (Keyhan Vakil) [#48523](https://github.com/nodejs/node/pull/48523)
* \[[`112335e342`](https://github.com/nodejs/node/commit/112335e342)] - **deps**: update corepack to 0.18.1 (Node.js GitHub Bot) [#48483](https://github.com/nodejs/node/pull/48483)
* \[[`2b141c413f`](https://github.com/nodejs/node/commit/2b141c413f)] - **deps**: update icu to 73.2 (Node.js GitHub Bot) [#48502](https://github.com/nodejs/node/pull/48502)
* \[[`188b34d4a1`](https://github.com/nodejs/node/commit/188b34d4a1)] - **deps**: upgrade npm to 9.7.2 (npm team) [#48514](https://github.com/nodejs/node/pull/48514)
* \[[`bf0444b5d9`](https://github.com/nodejs/node/commit/bf0444b5d9)] - **deps**: update zlib to 1.2.13.1-motley-3ca9f16 (Node.js GitHub Bot) [#48413](https://github.com/nodejs/node/pull/48413)
* \[[`b339d80a56`](https://github.com/nodejs/node/commit/b339d80a56)] - **deps**: upgrade npm to 9.7.1 (npm team) [#48378](https://github.com/nodejs/node/pull/48378)
* \[[`4132931b87`](https://github.com/nodejs/node/commit/4132931b87)] - **deps**: update simdutf to 3.2.14 (Node.js GitHub Bot) [#48344](https://github.com/nodejs/node/pull/48344)
* \[[`8cd56c1e85`](https://github.com/nodejs/node/commit/8cd56c1e85)] - **deps**: update ada to 2.5.1 (Node.js GitHub Bot) [#48319](https://github.com/nodejs/node/pull/48319)
* \[[`78cffcd645`](https://github.com/nodejs/node/commit/78cffcd645)] - **deps**: update zlib to 982b036 (Node.js GitHub Bot) [#48327](https://github.com/nodejs/node/pull/48327)
* \[[`6d00c2e33b`](https://github.com/nodejs/node/commit/6d00c2e33b)] - **doc**: fix options order (Luigi Pinca) [#48617](https://github.com/nodejs/node/pull/48617)
* \[[`7ad2d3a5d1`](https://github.com/nodejs/node/commit/7ad2d3a5d1)] - **doc**: update security release stewards (Rafael Gonzaga) [#48569](https://github.com/nodejs/node/pull/48569)
* \[[`cc3a056fdd`](https://github.com/nodejs/node/commit/cc3a056fdd)] - **doc**: update return type for describe (Shrujal Shah) [#48572](https://github.com/nodejs/node/pull/48572)
* \[[`99ae0b98af`](https://github.com/nodejs/node/commit/99ae0b98af)] - **doc**: run license-builder (github-actions\[bot]) [#48552](https://github.com/nodejs/node/pull/48552)
* \[[`9750d8205c`](https://github.com/nodejs/node/commit/9750d8205c)] - **doc**: add description of autoAllocateChunkSize in ReadableStream (Debadree Chatterjee) [#48004](https://github.com/nodejs/node/pull/48004)
* \[[`417927bb41`](https://github.com/nodejs/node/commit/417927bb41)] - **doc**: fix `filename` type in `watch` result (Dmitry Semigradsky) [#48032](https://github.com/nodejs/node/pull/48032)
* \[[`ca2ae86bd7`](https://github.com/nodejs/node/commit/ca2ae86bd7)] - **doc**: unnest `mime` and `MIMEParams` from MIMEType constructor (Dmitry Semigradsky) [#47950](https://github.com/nodejs/node/pull/47950)
* \[[`bda1228135`](https://github.com/nodejs/node/commit/bda1228135)] - **doc**: update security-release-process.md (Rafael Gonzaga) [#48504](https://github.com/nodejs/node/pull/48504)
* \[[`60c2ea4e79`](https://github.com/nodejs/node/commit/60c2ea4e79)] - **doc**: add vmoroz to collaborators (Vladimir Morozov) [#48527](https://github.com/nodejs/node/pull/48527)
* \[[`37bc0eac4a`](https://github.com/nodejs/node/commit/37bc0eac4a)] - **doc**: improve inspector.close() description (mary marchini) [#48494](https://github.com/nodejs/node/pull/48494)
* \[[`2a403cdad5`](https://github.com/nodejs/node/commit/2a403cdad5)] - **doc**: link to Runtime Keys in export conditions (Jacob Hummer) [#48408](https://github.com/nodejs/node/pull/48408)
* \[[`e2d579e644`](https://github.com/nodejs/node/commit/e2d579e644)] - **doc**: update fs flags documentation (sinkhaha) [#48463](https://github.com/nodejs/node/pull/48463)
* \[[`38bf290115`](https://github.com/nodejs/node/commit/38bf290115)] - **doc**: revise `error.md` introduction (Antoine du Hamel) [#48423](https://github.com/nodejs/node/pull/48423)
* \[[`641a2e9c6d`](https://github.com/nodejs/node/commit/641a2e9c6d)] - **doc**: add preveen-stack to triagers (Preveen P) [#48387](https://github.com/nodejs/node/pull/48387)
* \[[`4ab5e8d2e3`](https://github.com/nodejs/node/commit/4ab5e8d2e3)] - **doc**: refine when file is undefined in test events (Moshe Atlow) [#48451](https://github.com/nodejs/node/pull/48451)
* \[[`5cacdf9e6b`](https://github.com/nodejs/node/commit/5cacdf9e6b)] - **doc**: add kvakil to collaborators (Keyhan Vakil) [#48449](https://github.com/nodejs/node/pull/48449)
* \[[`b9c643e3ef`](https://github.com/nodejs/node/commit/b9c643e3ef)] - **doc**: add additional info on TSFN dispatch (Michael Dawson) [#48367](https://github.com/nodejs/node/pull/48367)
* \[[`17a0e1d1bf`](https://github.com/nodejs/node/commit/17a0e1d1bf)] - **doc**: add link for news from security wg (Michael Dawson) [#48396](https://github.com/nodejs/node/pull/48396)
* \[[`3a62994a4f`](https://github.com/nodejs/node/commit/3a62994a4f)] - **doc**: fix typo in events.md (Darshan Sen) [#48436](https://github.com/nodejs/node/pull/48436)
* \[[`e10a4cdf68`](https://github.com/nodejs/node/commit/e10a4cdf68)] - **doc**: run license-builder (github-actions\[bot]) [#48336](https://github.com/nodejs/node/pull/48336)
* \[[`19fde638fd`](https://github.com/nodejs/node/commit/19fde638fd)] - **fs**: call the callback with an error if writeSync fails (killa) [#47949](https://github.com/nodejs/node/pull/47949)
* \[[`4cad9fd8bd`](https://github.com/nodejs/node/commit/4cad9fd8bd)] - **fs**: remove unneeded return statement (Luigi Pinca) [#48526](https://github.com/nodejs/node/pull/48526)
* \[[`d367b73f43`](https://github.com/nodejs/node/commit/d367b73f43)] - **fs**: use kResistStopPropagation (Chemi Atlow) [#48521](https://github.com/nodejs/node/pull/48521)
* \[[`e50c3169af`](https://github.com/nodejs/node/commit/e50c3169af)] - **fs, stream**: initial `Symbol.dispose` and `Symbol.asyncDispose` support (Moshe Atlow) [#48518](https://github.com/nodejs/node/pull/48518)
* \[[`7d8a0b6eb7`](https://github.com/nodejs/node/commit/7d8a0b6eb7)] - **http**: null the joinDuplicateHeaders property on cleanup (Luigi Pinca) [#48608](https://github.com/nodejs/node/pull/48608)
* \[[`94ebb02f59`](https://github.com/nodejs/node/commit/94ebb02f59)] - **http**: server add async dispose (atlowChemi) [#48548](https://github.com/nodejs/node/pull/48548)
* \[[`c6a69e31a3`](https://github.com/nodejs/node/commit/c6a69e31a3)] - **http**: remove useless ternary in test (geekreal) [#48481](https://github.com/nodejs/node/pull/48481)
* \[[`2f0f40328f`](https://github.com/nodejs/node/commit/2f0f40328f)] - **http**: fix for handling on boot timers headers and request (Franciszek Koltuniuk) [#48291](https://github.com/nodejs/node/pull/48291)
* \[[`5378ad8ab1`](https://github.com/nodejs/node/commit/5378ad8ab1)] - **http2**: server add `asyncDispose` (atlowChemi) [#48548](https://github.com/nodejs/node/pull/48548)
* \[[`97a58c5970`](https://github.com/nodejs/node/commit/97a58c5970)] - **https**: server add `asyncDispose` (atlowChemi) [#48548](https://github.com/nodejs/node/pull/48548)
* \[[`40ae6eb6aa`](https://github.com/nodejs/node/commit/40ae6eb6aa)] - **https**: fix connection checking interval not clearing on server close (Nitzan Uziely) [#48383](https://github.com/nodejs/node/pull/48383)
* \[[`15530fea4c`](https://github.com/nodejs/node/commit/15530fea4c)] - **lib**: merge cjs and esm package json reader caches (Yagiz Nizipli) [#48477](https://github.com/nodejs/node/pull/48477)
* \[[`32bda81c31`](https://github.com/nodejs/node/commit/32bda81c31)] - **lib**: reduce url getters on `makeRequireFunction` (Yagiz Nizipli) [#48492](https://github.com/nodejs/node/pull/48492)
* \[[`0da03f01ba`](https://github.com/nodejs/node/commit/0da03f01ba)] - **lib**: remove duplicated requires in check\_syntax (Yagiz Nizipli) [#48508](https://github.com/nodejs/node/pull/48508)
* \[[`97b00c347d`](https://github.com/nodejs/node/commit/97b00c347d)] - **lib**: add option to force handling stopped events (Chemi Atlow) [#48301](https://github.com/nodejs/node/pull/48301)
* \[[`fe16749649`](https://github.com/nodejs/node/commit/fe16749649)] - **lib**: fix output message when repl is used with pm (Rafael Gonzaga) [#48438](https://github.com/nodejs/node/pull/48438)
* \[[`8c2c02d28a`](https://github.com/nodejs/node/commit/8c2c02d28a)] - **lib**: create weakRef only if any signals provided (Chemi Atlow) [#48448](https://github.com/nodejs/node/pull/48448)
* \[[`b6ae411ea9`](https://github.com/nodejs/node/commit/b6ae411ea9)] - **lib**: remove obsolete deletion of bufferBinding.zeroFill (Chengzhong Wu) [#47881](https://github.com/nodejs/node/pull/47881)
* \[[`562b3d4856`](https://github.com/nodejs/node/commit/562b3d4856)] - **lib**: move web global bootstrapping to the expected file (Chengzhong Wu) [#47881](https://github.com/nodejs/node/pull/47881)
* \[[`f9c0d5acac`](https://github.com/nodejs/node/commit/f9c0d5acac)] - **lib**: fix blob.stream() causing hanging promises (Debadree Chatterjee) [#48232](https://github.com/nodejs/node/pull/48232)
* \[[`0162a0f5bf`](https://github.com/nodejs/node/commit/0162a0f5bf)] - **lib**: add support for inherited custom inspection methods (Antoine du Hamel) [#48306](https://github.com/nodejs/node/pull/48306)
* \[[`159ab6627a`](https://github.com/nodejs/node/commit/159ab6627a)] - **lib**: reduce URL invocations on http2 origins (Yagiz Nizipli) [#48338](https://github.com/nodejs/node/pull/48338)
* \[[`f0709fdc59`](https://github.com/nodejs/node/commit/f0709fdc59)] - **module**: add SourceMap.findOrigin (Isaac Z. Schlueter) [#47790](https://github.com/nodejs/node/pull/47790)
* \[[`4ec2d925b1`](https://github.com/nodejs/node/commit/4ec2d925b1)] - **module**: reduce url invocations in esm/load.js (Yagiz Nizipli) [#48337](https://github.com/nodejs/node/pull/48337)
* \[[`2c363971cc`](https://github.com/nodejs/node/commit/2c363971cc)] - **net**: improve network family autoselection handle handling (Paolo Insogna) [#48464](https://github.com/nodejs/node/pull/48464)
* \[[`dbf9e9ffc8`](https://github.com/nodejs/node/commit/dbf9e9ffc8)] - **node-api**: provide napi\_define\_properties fast path (Gabriel Schulhof) [#48440](https://github.com/nodejs/node/pull/48440)
* \[[`87ad657777`](https://github.com/nodejs/node/commit/87ad657777)] - **node-api**: implement external strings (Gabriel Schulhof) [#48339](https://github.com/nodejs/node/pull/48339)
* \[[`4efa6807ea`](https://github.com/nodejs/node/commit/4efa6807ea)] - **permission**: handle end nodes with children cases (Rafael Gonzaga) [#48531](https://github.com/nodejs/node/pull/48531)
* \[[`84fe811108`](https://github.com/nodejs/node/commit/84fe811108)] - **repl**: display dynamic import variant in static import error messages (Hemanth HM) [#48129](https://github.com/nodejs/node/pull/48129)
* \[[`bdcc037470`](https://github.com/nodejs/node/commit/bdcc037470)] - **report**: disable js stack when no context is entered (Chengzhong Wu) [#48495](https://github.com/nodejs/node/pull/48495)
* \[[`97bd9ccd04`](https://github.com/nodejs/node/commit/97bd9ccd04)] - **src**: fix uninitialized field access in AsyncHooks (Jan Olaf Krems) [#48566](https://github.com/nodejs/node/pull/48566)
* \[[`404958fc96`](https://github.com/nodejs/node/commit/404958fc96)] - **src**: fix Coverity issue regarding unnecessary copy (Yagiz Nizipli) [#48565](https://github.com/nodejs/node/pull/48565)
* \[[`c4b8edea24`](https://github.com/nodejs/node/commit/c4b8edea24)] - **src**: refactor `SplitString` in util (Yagiz Nizipli) [#48491](https://github.com/nodejs/node/pull/48491)
* \[[`5bc13a4772`](https://github.com/nodejs/node/commit/5bc13a4772)] - **src**: revert IS\_RELEASE (Rafael Gonzaga) [#48505](https://github.com/nodejs/node/pull/48505)
* \[[`4971e46051`](https://github.com/nodejs/node/commit/4971e46051)] - **src**: add V8 fast api to `guessHandleType` (Yagiz Nizipli) [#48349](https://github.com/nodejs/node/pull/48349)
* \[[`954e46e792`](https://github.com/nodejs/node/commit/954e46e792)] - **src**: return uint32 for `guessHandleType` (Yagiz Nizipli) [#48349](https://github.com/nodejs/node/pull/48349)
* \[[`05009675da`](https://github.com/nodejs/node/commit/05009675da)] - **src**: make realm binding data store weak (Chengzhong Wu) [#47688](https://github.com/nodejs/node/pull/47688)
* \[[`120ac74352`](https://github.com/nodejs/node/commit/120ac74352)] - **src**: remove aliased buffer weak callback (Chengzhong Wu) [#47688](https://github.com/nodejs/node/pull/47688)
* \[[`6591826e99`](https://github.com/nodejs/node/commit/6591826e99)] - **src**: handle wasm out of bound in osx will raise SIGBUS correctly (Congcong Cai) [#46561](https://github.com/nodejs/node/pull/46561)
* \[[`1b84ddeec2`](https://github.com/nodejs/node/commit/1b84ddeec2)] - **src**: implement constants binding directly (Joyee Cheung) [#48186](https://github.com/nodejs/node/pull/48186)
* \[[`06d49c1f10`](https://github.com/nodejs/node/commit/06d49c1f10)] - **src**: implement natives binding without special casing (Joyee Cheung) [#48186](https://github.com/nodejs/node/pull/48186)
* \[[`325441abf5`](https://github.com/nodejs/node/commit/325441abf5)] - **src**: add missing to\_ascii method in dns queries (Daniel Lemire) [#48354](https://github.com/nodejs/node/pull/48354)
* \[[`84d0eb74b8`](https://github.com/nodejs/node/commit/84d0eb74b8)] - **stream**: fix premature pipeline end (Robert Nagy) [#48435](https://github.com/nodejs/node/pull/48435)
* \[[`3df7368735`](https://github.com/nodejs/node/commit/3df7368735)] - **test**: add missing assertions to test-runner-cli (Moshe Atlow) [#48593](https://github.com/nodejs/node/pull/48593)
* \[[`07eb310b0d`](https://github.com/nodejs/node/commit/07eb310b0d)] - **test**: remove test-crypto-keygen flaky designation (Luigi Pinca) [#48575](https://github.com/nodejs/node/pull/48575)
* \[[`75aa0a7682`](https://github.com/nodejs/node/commit/75aa0a7682)] - **test**: remove test-timers-immediate-queue flaky designation (Luigi Pinca) [#48575](https://github.com/nodejs/node/pull/48575)
* \[[`a9756f3126`](https://github.com/nodejs/node/commit/a9756f3126)] - **test**: add Symbol.dispose support to mock timers (Benjamin Gruenbaum) [#48549](https://github.com/nodejs/node/pull/48549)
* \[[`0f912a7248`](https://github.com/nodejs/node/commit/0f912a7248)] - **test**: mark test-child-process-stdio-reuse-readable-stdio flaky (Luigi Pinca) [#48537](https://github.com/nodejs/node/pull/48537)
* \[[`30f4bc4985`](https://github.com/nodejs/node/commit/30f4bc4985)] - **test**: make IsolateData per-isolate in cctest (Joyee Cheung) [#48450](https://github.com/nodejs/node/pull/48450)
* \[[`407ce3fdcb`](https://github.com/nodejs/node/commit/407ce3fdcb)] - **test**: define NAPI\_VERSION before including node\_api.h (Chengzhong Wu) [#48376](https://github.com/nodejs/node/pull/48376)
* \[[`24a8fa95f0`](https://github.com/nodejs/node/commit/24a8fa95f0)] - **test**: remove unnecessary noop function args to `mustNotCall()` (Antoine du Hamel) [#48513](https://github.com/nodejs/node/pull/48513)
* \[[`09af579775`](https://github.com/nodejs/node/commit/09af579775)] - **test**: skip test-runner-watch-mode on IBMi (Moshe Atlow) [#48473](https://github.com/nodejs/node/pull/48473)
* \[[`77cb1ee0b2`](https://github.com/nodejs/node/commit/77cb1ee0b2)] - **test**: add missing \<algorithm> include for std::find (Sam James) [#48380](https://github.com/nodejs/node/pull/48380)
* \[[`7c790ca03c`](https://github.com/nodejs/node/commit/7c790ca03c)] - **test**: fix flaky test-watch-mode (Moshe Atlow) [#48147](https://github.com/nodejs/node/pull/48147)
* \[[`1398829746`](https://github.com/nodejs/node/commit/1398829746)] - **test**: fix `test-net-autoselectfamily` for kernel without IPv6 support (Livia Medeiros) [#48265](https://github.com/nodejs/node/pull/48265)
* \[[`764119ba4b`](https://github.com/nodejs/node/commit/764119ba4b)] - **test**: update url web-platform tests (Yagiz Nizipli) [#48319](https://github.com/nodejs/node/pull/48319)
* \[[`f1ead59629`](https://github.com/nodejs/node/commit/f1ead59629)] - **test**: ignore the copied entry\_point.c (Luigi Pinca) [#48297](https://github.com/nodejs/node/pull/48297)
* \[[`fc5d1bddcb`](https://github.com/nodejs/node/commit/fc5d1bddcb)] - **test**: refactor test-gc-http-client-timeout (Luigi Pinca) [#48292](https://github.com/nodejs/node/pull/48292)
* \[[`46a3d068a0`](https://github.com/nodejs/node/commit/46a3d068a0)] - **test**: update encoding web-platform tests (Yagiz Nizipli) [#48320](https://github.com/nodejs/node/pull/48320)
* \[[`141e5aad83`](https://github.com/nodejs/node/commit/141e5aad83)] - **test**: update FileAPI web-platform tests (Yagiz Nizipli) [#48322](https://github.com/nodejs/node/pull/48322)
* \[[`83cfc67099`](https://github.com/nodejs/node/commit/83cfc67099)] - **test**: update user-timing web-platform tests (Yagiz Nizipli) [#48321](https://github.com/nodejs/node/pull/48321)
* \[[`2c56835a33`](https://github.com/nodejs/node/commit/2c56835a33)] - **test\_runner**: fixed `test` shorthands return type (Shocker) [#48555](https://github.com/nodejs/node/pull/48555)
* \[[`7d01c8894a`](https://github.com/nodejs/node/commit/7d01c8894a)] - **(SEMVER-MINOR)** **test\_runner**: add initial draft for fakeTimers (Erick Wendel) [#47775](https://github.com/nodejs/node/pull/47775)
* \[[`de4f14c249`](https://github.com/nodejs/node/commit/de4f14c249)] - **test\_runner**: add enqueue and dequeue events (Moshe Atlow) [#48428](https://github.com/nodejs/node/pull/48428)
* \[[`5ebe3a4ea7`](https://github.com/nodejs/node/commit/5ebe3a4ea7)] - **test\_runner**: make `--test-name-pattern` recursive (Moshe Atlow) [#48382](https://github.com/nodejs/node/pull/48382)
* \[[`93bf447308`](https://github.com/nodejs/node/commit/93bf447308)] - **test\_runner**: refactor coverage report output for readability (Damien Seguin) [#47791](https://github.com/nodejs/node/pull/47791)
* \[[`504d1d7bdc`](https://github.com/nodejs/node/commit/504d1d7bdc)] - **(SEMVER-MINOR)** **tls**: add ALPNCallback server option for dynamic ALPN negotiation (Tim Perry) [#45190](https://github.com/nodejs/node/pull/45190)
* \[[`203c3cf4ca`](https://github.com/nodejs/node/commit/203c3cf4ca)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#48544](https://github.com/nodejs/node/pull/48544)
* \[[`333907b19d`](https://github.com/nodejs/node/commit/333907b19d)] - **tools**: speedup compilation of js2c output (Keyhan Vakil) [#48160](https://github.com/nodejs/node/pull/48160)
* \[[`10bd5f4d97`](https://github.com/nodejs/node/commit/10bd5f4d97)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#48486](https://github.com/nodejs/node/pull/48486)
* \[[`52de27b9fe`](https://github.com/nodejs/node/commit/52de27b9fe)] - **tools**: pin ruff version number (Rich Trott) [#48505](https://github.com/nodejs/node/pull/48505)
* \[[`4345526644`](https://github.com/nodejs/node/commit/4345526644)] - **tools**: replace sed with perl (Luigi Pinca) [#48499](https://github.com/nodejs/node/pull/48499)
* \[[`6c590835f3`](https://github.com/nodejs/node/commit/6c590835f3)] - **tools**: automate update openssl v16 (Marco Ippolito) [#48377](https://github.com/nodejs/node/pull/48377)
* \[[`90b5335338`](https://github.com/nodejs/node/commit/90b5335338)] - **tools**: update eslint to 8.43.0 (Node.js GitHub Bot) [#48487](https://github.com/nodejs/node/pull/48487)
* \[[`cd83530a11`](https://github.com/nodejs/node/commit/cd83530a11)] - **tools**: update doc to to-vfile\@8.0.0 (Node.js GitHub Bot) [#48485](https://github.com/nodejs/node/pull/48485)
* \[[`e500b439bd`](https://github.com/nodejs/node/commit/e500b439bd)] - **tools**: prepare tools/doc for to-vfile 8.0.0 (Rich Trott) [#48485](https://github.com/nodejs/node/pull/48485)
* \[[`d623616813`](https://github.com/nodejs/node/commit/d623616813)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#48417](https://github.com/nodejs/node/pull/48417)
* \[[`a2e107dde4`](https://github.com/nodejs/node/commit/a2e107dde4)] - **tools**: update create-or-update-pull-request-action (Richard Lau) [#48398](https://github.com/nodejs/node/pull/48398)
* \[[`8009e2c3be`](https://github.com/nodejs/node/commit/8009e2c3be)] - **tools**: update eslint-plugin-jsdoc (Richard Lau) [#48393](https://github.com/nodejs/node/pull/48393)
* \[[`10385c8565`](https://github.com/nodejs/node/commit/10385c8565)] - **tools**: add version update to external dependencies (Andrea Fassina) [#48081](https://github.com/nodejs/node/pull/48081)
* \[[`b1cef81b18`](https://github.com/nodejs/node/commit/b1cef81b18)] - **tools**: update eslint to 8.42.0 (Node.js GitHub Bot) [#48328](https://github.com/nodejs/node/pull/48328)
* \[[`0923dc0b8e`](https://github.com/nodejs/node/commit/0923dc0b8e)] - **tools**: disable jsdoc/no-defaults rule (Luigi Pinca) [#48328](https://github.com/nodejs/node/pull/48328)
* \[[`b03146da85`](https://github.com/nodejs/node/commit/b03146da85)] - **typings**: remove unused primordials (Yagiz Nizipli) [#48509](https://github.com/nodejs/node/pull/48509)
* \[[`e9c9d187b9`](https://github.com/nodejs/node/commit/e9c9d187b9)] - **typings**: fix JSDoc in ESM loader modules (Antoine du Hamel) [#48424](https://github.com/nodejs/node/pull/48424)
* \[[`fafe651d23`](https://github.com/nodejs/node/commit/fafe651d23)] - **url**: conform to origin getter spec changes (Yagiz Nizipli) [#48319](https://github.com/nodejs/node/pull/48319)

<a id="20.3.1"></a>

## 2023-06-20, Version 20.3.1 (Current), @RafaelGSS

This is a security release.

### Notable Changes

The following CVEs are fixed in this release:

* [CVE-2023-30581](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30581): `mainModule.__proto__` Bypass Experimental Policy Mechanism (High)
* [CVE-2023-30584](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30584): Path Traversal Bypass in Experimental Permission Model (High)
* [CVE-2023-30587](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30587): Bypass of Experimental Permission Model via Node.js Inspector (High)
* [CVE-2023-30582](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30582): Inadequate Permission Model Allows Unauthorized File Watching (Medium)
* [CVE-2023-30583](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30583): Bypass of Experimental Permission Model via fs.openAsBlob() (Medium)
* [CVE-2023-30585](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30585): Privilege escalation via Malicious Registry Key manipulation during Node.js installer repair process (Medium)
* [CVE-2023-30586](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30586): Bypass of Experimental Permission Model via Arbitrary OpenSSL Engines (Medium)
* [CVE-2023-30588](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30588): Process interuption due to invalid Public Key information in x509 certificates (Medium)
* [CVE-2023-30589](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30589): HTTP Request Smuggling via Empty headers separated by CR (Medium)
* [CVE-2023-30590](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-30590): DiffieHellman does not generate keys after setting a private key (Medium)
* OpenSSL Security Releases
  * [OpenSSL security advisory 28th March](https://www.openssl.org/news/secadv/20230328.txt).
  * [OpenSSL security advisory 20th April](https://www.openssl.org/news/secadv/20230420.txt).
  * [OpenSSL security advisory 30th May](https://www.openssl.org/news/secadv/20230530.txt)

More detailed information on each of the vulnerabilities can be found in [June 2023 Security Releases](https://nodejs.org/en/blog/vulnerability/june-2023-security-releases/) blog post.

### Commits

* \[[`dac08dafc9`](https://github.com/nodejs/node/commit/dac08dafc9)] - **crypto**: handle cert with invalid SPKI gracefully (Tobias Nießen) [nodejs-private/node-private#393](https://github.com/nodejs-private/node-private/pull/393)
* \[[`d274c3babc`](https://github.com/nodejs/node/commit/d274c3babc)] - **crypto,https,tls**: disable engines if perms enabled (Tobias Nießen) [nodejs-private/node-private#409](https://github.com/nodejs-private/node-private/pull/409)
* \[[`5621c1de38`](https://github.com/nodejs/node/commit/5621c1de38)] - **deps**: update archs files for openssl-3.0.9-quic1 (Node.js GitHub Bot) [#48402](https://github.com/nodejs/node/pull/48402)
* \[[`771caa9f1c`](https://github.com/nodejs/node/commit/771caa9f1c)] - **deps**: upgrade openssl sources to quictls/openssl-3.0.9-quic1 (Node.js GitHub Bot) [#48402](https://github.com/nodejs/node/pull/48402)
* \[[`0459bf9c99`](https://github.com/nodejs/node/commit/0459bf9c99)] - **doc,test**: clarify behavior of DH generateKeys (Tobias Nießen) [nodejs-private/node-private#426](https://github.com/nodejs-private/node-private/pull/426)
* \[[`27e20501aa`](https://github.com/nodejs/node/commit/27e20501aa)] - **http**: disable request smuggling via empty headers (Paolo Insogna) [nodejs-private/node-private#427](https://github.com/nodejs-private/node-private/pull/427)
* \[[`9c17e335f1`](https://github.com/nodejs/node/commit/9c17e335f1)] - **msi**: do not create AppData\Roaming\npm (Tobias Nießen) [nodejs-private/node-private#408](https://github.com/nodejs-private/node-private/pull/408)
* \[[`b51124c637`](https://github.com/nodejs/node/commit/b51124c637)] - **permission**: handle fs path traversal (RafaelGSS) [nodejs-private/node-private#403](https://github.com/nodejs-private/node-private/pull/403)
* \[[`ebc5927adc`](https://github.com/nodejs/node/commit/ebc5927adc)] - **permission**: handle fs.openAsBlob (RafaelGSS) [nodejs-private/node-private#405](https://github.com/nodejs-private/node-private/pull/405)
* \[[`c39a43bff5`](https://github.com/nodejs/node/commit/c39a43bff5)] - **permission**: handle fs.watchFile (RafaelGSS) [nodejs-private/node-private#404](https://github.com/nodejs-private/node-private/pull/404)
* \[[`d0a8264ec9`](https://github.com/nodejs/node/commit/d0a8264ec9)] - **policy**: handle mainModule.\_\_proto\_\_ bypass (RafaelGSS) [nodejs-private/node-private#416](https://github.com/nodejs-private/node-private/pull/416)
* \[[`3df13d5a79`](https://github.com/nodejs/node/commit/3df13d5a79)] - **src,permission**: restrict inspector when pm enabled (RafaelGSS) [nodejs-private/node-private#410](https://github.com/nodejs-private/node-private/pull/410)

<a id="20.3.0"></a>

## 2023-06-08, Version 20.3.0 (Current), @targos

### Notable Changes

* \[[`bfcb3d1d9a`](https://github.com/nodejs/node/commit/bfcb3d1d9a)] - **deps**: upgrade to libuv 1.45.0, including significant performance improvements to file system operations on Linux (Santiago Gimeno) [#48078](https://github.com/nodejs/node/pull/48078)
* \[[`5094d1b292`](https://github.com/nodejs/node/commit/5094d1b292)] - **doc**: add Ruy Adorno to list of TSC members (Michael Dawson) [#48172](https://github.com/nodejs/node/pull/48172)
* \[[`2f5dbca690`](https://github.com/nodejs/node/commit/2f5dbca690)] - **doc**: mark Node.js 14 as End-of-Life (Richard Lau) [#48023](https://github.com/nodejs/node/pull/48023)
* \[[`b1828b325e`](https://github.com/nodejs/node/commit/b1828b325e)] - **(SEMVER-MINOR)** **lib**: implement `AbortSignal.any()` (Chemi Atlow) [#47821](https://github.com/nodejs/node/pull/47821)
* \[[`f380953103`](https://github.com/nodejs/node/commit/f380953103)] - **module**: change default resolver to not throw on unknown scheme (Gil Tayar) [#47824](https://github.com/nodejs/node/pull/47824)
* \[[`a94f87ed99`](https://github.com/nodejs/node/commit/a94f87ed99)] - **(SEMVER-MINOR)** **node-api**: define version 9 (Chengzhong Wu) [#48151](https://github.com/nodejs/node/pull/48151)
* \[[`9e2b13dfa7`](https://github.com/nodejs/node/commit/9e2b13dfa7)] - **stream**: deprecate `asIndexedPairs` (Chemi Atlow) [#48102](https://github.com/nodejs/node/pull/48102)

### Commits

* \[[`35c96156d1`](https://github.com/nodejs/node/commit/35c96156d1)] - **benchmark**: use `cluster.isPrimary` instead of `cluster.isMaster` (Deokjin Kim) [#48002](https://github.com/nodejs/node/pull/48002)
* \[[`3e6e3abf32`](https://github.com/nodejs/node/commit/3e6e3abf32)] - **bootstrap**: throw ERR\_NOT\_SUPPORTED\_IN\_SNAPSHOT in unsupported operation (Joyee Cheung) [#47887](https://github.com/nodejs/node/pull/47887)
* \[[`c480559347`](https://github.com/nodejs/node/commit/c480559347)] - **bootstrap**: put is\_building\_snapshot state in IsolateData (Joyee Cheung) [#47887](https://github.com/nodejs/node/pull/47887)
* \[[`50c0a15535`](https://github.com/nodejs/node/commit/50c0a15535)] - **build**: set v8\_enable\_webassembly=false when lite mode is enabled (Cheng Shao) [#48248](https://github.com/nodejs/node/pull/48248)
* \[[`4562805cf6`](https://github.com/nodejs/node/commit/4562805cf6)] - **build**: speed up compilation of mksnapshot output (Keyhan Vakil) [#48162](https://github.com/nodejs/node/pull/48162)
* \[[`8b89f13933`](https://github.com/nodejs/node/commit/8b89f13933)] - **build**: add action to close stale PRs (Michael Dawson) [#48051](https://github.com/nodejs/node/pull/48051)
* \[[`5d92202220`](https://github.com/nodejs/node/commit/5d92202220)] - **build**: replace js2c.py with js2c.cc (Joyee Cheung) [#46997](https://github.com/nodejs/node/pull/46997)
* \[[`6cf2adc36e`](https://github.com/nodejs/node/commit/6cf2adc36e)] - **cluster**: use ObjectPrototypeHasOwnProperty (Daeyeon Jeong) [#48141](https://github.com/nodejs/node/pull/48141)
* \[[`f564b03c38`](https://github.com/nodejs/node/commit/f564b03c38)] - **crypto**: use openssl's own memory BIOs in crypto\_context.cc (GauriSpears) [#47160](https://github.com/nodejs/node/pull/47160)
* \[[`ac8dd61fc3`](https://github.com/nodejs/node/commit/ac8dd61fc3)] - **crypto**: remove default encoding from cipher (Tobias Nießen) [#47998](https://github.com/nodejs/node/pull/47998)
* \[[`15c2de4407`](https://github.com/nodejs/node/commit/15c2de4407)] - **crypto**: fix setEngine() when OPENSSL\_NO\_ENGINE set (Tobias Nießen) [#47977](https://github.com/nodejs/node/pull/47977)
* \[[`9e2dd5b5e2`](https://github.com/nodejs/node/commit/9e2dd5b5e2)] - **deps**: update zlib to 337322d (Node.js GitHub Bot) [#48218](https://github.com/nodejs/node/pull/48218)
* \[[`bfcb3d1d9a`](https://github.com/nodejs/node/commit/bfcb3d1d9a)] - **deps**: upgrade to libuv 1.45.0 (Santiago Gimeno) [#48078](https://github.com/nodejs/node/pull/48078)
* \[[`13930f092f`](https://github.com/nodejs/node/commit/13930f092f)] - **deps**: update ada to 2.5.0 (Node.js GitHub Bot) [#48223](https://github.com/nodejs/node/pull/48223)
* \[[`3047caebec`](https://github.com/nodejs/node/commit/3047caebec)] - **deps**: set `CARES_RANDOM_FILE` for c-ares (Richard Lau) [#48156](https://github.com/nodejs/node/pull/48156)
* \[[`0db79a0872`](https://github.com/nodejs/node/commit/0db79a0872)] - **deps**: update histogram 0.11.8 (Marco Ippolito) [#47742](https://github.com/nodejs/node/pull/47742)
* \[[`99af6716f5`](https://github.com/nodejs/node/commit/99af6716f5)] - **deps**: update histogram to 0.11.7 (Marco Ippolito) [#47742](https://github.com/nodejs/node/pull/47742)
* \[[`d4922bc985`](https://github.com/nodejs/node/commit/d4922bc985)] - **deps**: update c-ares to 1.19.1 (Node.js GitHub Bot) [#48115](https://github.com/nodejs/node/pull/48115)
* \[[`f6ccdb289f`](https://github.com/nodejs/node/commit/f6ccdb289f)] - **deps**: update simdutf to 3.2.12 (Node.js GitHub Bot) [#48118](https://github.com/nodejs/node/pull/48118)
* \[[`3ed0afc778`](https://github.com/nodejs/node/commit/3ed0afc778)] - **deps**: update minimatch to 9.0.1 (Node.js GitHub Bot) [#48094](https://github.com/nodejs/node/pull/48094)
* \[[`df7540fb73`](https://github.com/nodejs/node/commit/df7540fb73)] - **deps**: update ada to 2.4.2 (Node.js GitHub Bot) [#48092](https://github.com/nodejs/node/pull/48092)
* \[[`07df5c48e8`](https://github.com/nodejs/node/commit/07df5c48e8)] - **deps**: update corepack to 0.18.0 (Node.js GitHub Bot) [#48091](https://github.com/nodejs/node/pull/48091)
* \[[`d95a5bb559`](https://github.com/nodejs/node/commit/d95a5bb559)] - **deps**: update uvwasi to 0.0.18 (Node.js GitHub Bot) [#47866](https://github.com/nodejs/node/pull/47866)
* \[[`443477e041`](https://github.com/nodejs/node/commit/443477e041)] - **deps**: update uvwasi to 0.0.17 (Node.js GitHub Bot) [#47866](https://github.com/nodejs/node/pull/47866)
* \[[`03f67d6d6d`](https://github.com/nodejs/node/commit/03f67d6d6d)] - **deps**: upgrade npm to 9.6.7 (npm team) [#48062](https://github.com/nodejs/node/pull/48062)
* \[[`d3e3a911fd`](https://github.com/nodejs/node/commit/d3e3a911fd)] - **deps**: update nghttp2 to 1.53.0 (Node.js GitHub Bot) [#47997](https://github.com/nodejs/node/pull/47997)
* \[[`f7c4daaf67`](https://github.com/nodejs/node/commit/f7c4daaf67)] - **deps**: update ada to 2.4.1 (Node.js GitHub Bot) [#48036](https://github.com/nodejs/node/pull/48036)
* \[[`c6a752560d`](https://github.com/nodejs/node/commit/c6a752560d)] - **deps**: add loongarch64 into openssl Makefile and gen openssl-loongarch64 (Shi Pujin) [#46401](https://github.com/nodejs/node/pull/46401)
* \[[`d194241716`](https://github.com/nodejs/node/commit/d194241716)] - **deps**: update undici to 5.22.1 (Node.js GitHub Bot) [#47994](https://github.com/nodejs/node/pull/47994)
* \[[`02e919f4a2`](https://github.com/nodejs/node/commit/02e919f4a2)] - **deps,test**: update postject to 1.0.0-alpha.6 (Node.js GitHub Bot) [#48072](https://github.com/nodejs/node/pull/48072)
* \[[`2c19f596ad`](https://github.com/nodejs/node/commit/2c19f596ad)] - **doc**: clarify array args to Buffer.from() (Bryan English) [#48274](https://github.com/nodejs/node/pull/48274)
* \[[`d681e5f456`](https://github.com/nodejs/node/commit/d681e5f456)] - **doc**: document watch option for node:test run() (Moshe Atlow) [#48256](https://github.com/nodejs/node/pull/48256)
* \[[`96e54ddbca`](https://github.com/nodejs/node/commit/96e54ddbca)] - **doc**: reserve 117 for Electron 26 (Calvin) [#48245](https://github.com/nodejs/node/pull/48245)
* \[[`9aff8c7818`](https://github.com/nodejs/node/commit/9aff8c7818)] - **doc**: update documentation for FIPS support (Richard Lau) [#48194](https://github.com/nodejs/node/pull/48194)
* \[[`8c5338648f`](https://github.com/nodejs/node/commit/8c5338648f)] - **doc**: improve the documentation of the stdio option (Kumar Arnav) [#48110](https://github.com/nodejs/node/pull/48110)
* \[[`11918d705f`](https://github.com/nodejs/node/commit/11918d705f)] - **doc**: update Buffer.allocUnsafe description (sinkhaha) [#48183](https://github.com/nodejs/node/pull/48183)
* \[[`2b51ee5e22`](https://github.com/nodejs/node/commit/2b51ee5e22)] - **doc**: update codeowners with website team (Claudio Wunder) [#48197](https://github.com/nodejs/node/pull/48197)
* \[[`360df25d04`](https://github.com/nodejs/node/commit/360df25d04)] - **doc**: fix broken link to new folder doc/contributing/maintaining (Andrea Fassina) [#48205](https://github.com/nodejs/node/pull/48205)
* \[[`13e95e21a4`](https://github.com/nodejs/node/commit/13e95e21a4)] - **doc**: add atlowChemi to triagers (Chemi Atlow) [#48104](https://github.com/nodejs/node/pull/48104)
* \[[`5f83ce530f`](https://github.com/nodejs/node/commit/5f83ce530f)] - **doc**: fix typo in readline completer function section (Vadym) [#48188](https://github.com/nodejs/node/pull/48188)
* \[[`3c82165d27`](https://github.com/nodejs/node/commit/3c82165d27)] - **doc**: remove broken link for keygen (Rich Trott) [#48176](https://github.com/nodejs/node/pull/48176)
* \[[`0ca90a1e6d`](https://github.com/nodejs/node/commit/0ca90a1e6d)] - **doc**: add `auto` intrinsic height to prevent jitter/flicker (Daniel Holbert) [#48195](https://github.com/nodejs/node/pull/48195)
* \[[`f117855092`](https://github.com/nodejs/node/commit/f117855092)] - **doc**: add version info on the SEA docs (Antoine du Hamel) [#48173](https://github.com/nodejs/node/pull/48173)
* \[[`5094d1b292`](https://github.com/nodejs/node/commit/5094d1b292)] - **doc**: add Ruy to list of TSC members (Michael Dawson) [#48172](https://github.com/nodejs/node/pull/48172)
* \[[`39d8140227`](https://github.com/nodejs/node/commit/39d8140227)] - **doc**: update socket.remote\* properties documentation (Saba Kharanauli) [#48139](https://github.com/nodejs/node/pull/48139)
* \[[`5497c13efe`](https://github.com/nodejs/node/commit/5497c13efe)] - **doc**: update outdated section on TLSv1.3-PSK (Tobias Nießen) [#48123](https://github.com/nodejs/node/pull/48123)
* \[[`281dfaf727`](https://github.com/nodejs/node/commit/281dfaf727)] - **doc**: improve HMAC key recommendations (Tobias Nießen) [#48121](https://github.com/nodejs/node/pull/48121)
* \[[`bd311b6c70`](https://github.com/nodejs/node/commit/bd311b6c70)] - **doc**: clarify mkdir() recursive behavior (Stephen Odogwu) [#48109](https://github.com/nodejs/node/pull/48109)
* \[[`5b061c8922`](https://github.com/nodejs/node/commit/5b061c8922)] - **doc**: fix typo in crypto legacy streams API section (Tobias Nießen) [#48122](https://github.com/nodejs/node/pull/48122)
* \[[`10ccb2bd81`](https://github.com/nodejs/node/commit/10ccb2bd81)] - **doc**: update SEA source link (Rich Trott) [#48080](https://github.com/nodejs/node/pull/48080)
* \[[`415bf7f532`](https://github.com/nodejs/node/commit/415bf7f532)] - **doc**: clarify tty.isRaw (Roberto Vidal) [#48055](https://github.com/nodejs/node/pull/48055)
* \[[`0ac4b33c76`](https://github.com/nodejs/node/commit/0ac4b33c76)] - **doc**: correct line break for Windows terminals (Alex Schwartz) [#48083](https://github.com/nodejs/node/pull/48083)
* \[[`f30ba5c320`](https://github.com/nodejs/node/commit/f30ba5c320)] - **doc**: fix Windows code snippet tags (Antoine du Hamel) [#48100](https://github.com/nodejs/node/pull/48100)
* \[[`12fef9b68c`](https://github.com/nodejs/node/commit/12fef9b68c)] - **doc**: harmonize fenced code snippet flags (Antoine du Hamel) [#48082](https://github.com/nodejs/node/pull/48082)
* \[[`13f163eace`](https://github.com/nodejs/node/commit/13f163eace)] - **doc**: use secure key length for HMAC generateKey (Tobias Nießen) [#48052](https://github.com/nodejs/node/pull/48052)
* \[[`1e3e7c9f33`](https://github.com/nodejs/node/commit/1e3e7c9f33)] - **doc**: update broken EVP\_BytesToKey link (Rich Trott) [#48064](https://github.com/nodejs/node/pull/48064)
* \[[`5917ba1838`](https://github.com/nodejs/node/commit/5917ba1838)] - **doc**: update broken spkac link (Rich Trott) [#48063](https://github.com/nodejs/node/pull/48063)
* \[[`0e4a3b7db1`](https://github.com/nodejs/node/commit/0e4a3b7db1)] - **doc**: document node-api version process (Chengzhong Wu) [#47972](https://github.com/nodejs/node/pull/47972)
* \[[`85bbaa94ea`](https://github.com/nodejs/node/commit/85bbaa94ea)] - **doc**: update process.versions properties (Saba Kharanauli) [#48019](https://github.com/nodejs/node/pull/48019)
* \[[`7660eb591a`](https://github.com/nodejs/node/commit/7660eb591a)] - **doc**: fix typo in binding functions (Deokjin Kim) [#48003](https://github.com/nodejs/node/pull/48003)
* \[[`2f5dbca690`](https://github.com/nodejs/node/commit/2f5dbca690)] - **doc**: mark Node.js 14 as End-of-Life (Richard Lau) [#48023](https://github.com/nodejs/node/pull/48023)
* \[[`3b94a739f2`](https://github.com/nodejs/node/commit/3b94a739f2)] - **doc**: clarify CRYPTO\_CUSTOM\_ENGINE\_NOT\_SUPPORTED (Tobias Nießen) [#47976](https://github.com/nodejs/node/pull/47976)
* \[[`9e381cfa89`](https://github.com/nodejs/node/commit/9e381cfa89)] - **doc**: add heading for permission model limitations (Tobias Nießen) [#47989](https://github.com/nodejs/node/pull/47989)
* \[[`802db923e0`](https://github.com/nodejs/node/commit/802db923e0)] - **doc,vm**: clarify usage of cachedData in vm.compileFunction() (Darshan Sen) [#48193](https://github.com/nodejs/node/pull/48193)
* \[[`11a3434810`](https://github.com/nodejs/node/commit/11a3434810)] - **esm**: remove support for arrays in `import` internal method (Antoine du Hamel) [#48296](https://github.com/nodejs/node/pull/48296)
* \[[`3b00f3afef`](https://github.com/nodejs/node/commit/3b00f3afef)] - **esm**: handle `globalPreload` hook returning a nullish value (Antoine du Hamel) [#48249](https://github.com/nodejs/node/pull/48249)
* \[[`3c7846d7e1`](https://github.com/nodejs/node/commit/3c7846d7e1)] - **esm**: handle more error types thrown from the loader thread (Antoine du Hamel) [#48247](https://github.com/nodejs/node/pull/48247)
* \[[`60ce2bcabc`](https://github.com/nodejs/node/commit/60ce2bcabc)] - **http**: send implicit headers on HEAD with no body (Matteo Collina) [#48108](https://github.com/nodejs/node/pull/48108)
* \[[`72de4e7170`](https://github.com/nodejs/node/commit/72de4e7170)] - **lib**: do not disable linter for entire files (Antoine du Hamel) [#48299](https://github.com/nodejs/node/pull/48299)
* \[[`10cc60fc91`](https://github.com/nodejs/node/commit/10cc60fc91)] - **lib**: use existing `isWindows` variable (sinkhaha) [#48134](https://github.com/nodejs/node/pull/48134)
* \[[`a90010aae9`](https://github.com/nodejs/node/commit/a90010aae9)] - **lib**: support FORCE\_COLOR for non TTY streams (Moshe Atlow) [#48034](https://github.com/nodejs/node/pull/48034)
* \[[`b1828b325e`](https://github.com/nodejs/node/commit/b1828b325e)] - **(SEMVER-MINOR)** **lib**: implement AbortSignal.any() (Chemi Atlow) [#47821](https://github.com/nodejs/node/pull/47821)
* \[[`8f1b86961f`](https://github.com/nodejs/node/commit/8f1b86961f)] - **meta**: bump github/codeql-action from 2.3.3 to 2.3.6 (dependabot\[bot]) [#48287](https://github.com/nodejs/node/pull/48287)
* \[[`1b87ccdf70`](https://github.com/nodejs/node/commit/1b87ccdf70)] - **meta**: bump actions/setup-python from 4.6.0 to 4.6.1 (dependabot\[bot]) [#48286](https://github.com/nodejs/node/pull/48286)
* \[[`10715aea26`](https://github.com/nodejs/node/commit/10715aea26)] - **meta**: bump codecov/codecov-action from 3.1.3 to 3.1.4 (dependabot\[bot]) [#48285](https://github.com/nodejs/node/pull/48285)
* \[[`79f73778ab`](https://github.com/nodejs/node/commit/79f73778ab)] - **meta**: remove dont-land-on-v14 auto labeling (Shrujal Shah) [#48031](https://github.com/nodejs/node/pull/48031)
* \[[`9c5711f3ea`](https://github.com/nodejs/node/commit/9c5711f3ea)] - **meta**: move one or more collaborators to emeritus (Node.js GitHub Bot) [#48010](https://github.com/nodejs/node/pull/48010)
* \[[`6d6bf3ee52`](https://github.com/nodejs/node/commit/6d6bf3ee52)] - **module**: reduce the number of URL initializations (Yagiz Nizipli) [#48272](https://github.com/nodejs/node/pull/48272)
* \[[`f380953103`](https://github.com/nodejs/node/commit/f380953103)] - **module**: change default resolver to not throw on unknown scheme (Gil Tayar) [#47824](https://github.com/nodejs/node/pull/47824)
* \[[`950185b0c0`](https://github.com/nodejs/node/commit/950185b0c0)] - **net**: fix address iteration with autoSelectFamily (Fedor Indutny) [#48258](https://github.com/nodejs/node/pull/48258)
* \[[`5ddca72e62`](https://github.com/nodejs/node/commit/5ddca72e62)] - **net**: fix family autoselection SSL connection handling (Paolo Insogna) [#48189](https://github.com/nodejs/node/pull/48189)
* \[[`750e53ca3c`](https://github.com/nodejs/node/commit/750e53ca3c)] - **net**: fix family autoselection timeout handling (Paolo Insogna) [#47860](https://github.com/nodejs/node/pull/47860)
* \[[`a94f87ed99`](https://github.com/nodejs/node/commit/a94f87ed99)] - **(SEMVER-MINOR)** **node-api**: define version 9 (Chengzhong Wu) [#48151](https://github.com/nodejs/node/pull/48151)
* \[[`e834979818`](https://github.com/nodejs/node/commit/e834979818)] - **node-api**: add status napi\_cannot\_run\_js (Gabriel Schulhof) [#47986](https://github.com/nodejs/node/pull/47986)
* \[[`eafe0c3ec6`](https://github.com/nodejs/node/commit/eafe0c3ec6)] - **node-api**: napi\_ref on all types is experimental (Vladimir Morozov) [#47975](https://github.com/nodejs/node/pull/47975)
* \[[`9a034746f5`](https://github.com/nodejs/node/commit/9a034746f5)] - **src**: add Realm document in the src README.md (Chengzhong Wu) [#47932](https://github.com/nodejs/node/pull/47932)
* \[[`b8f4070f71`](https://github.com/nodejs/node/commit/b8f4070f71)] - **src**: check node\_extra\_ca\_certs after openssl cfg (Raghu Saxena) [#48159](https://github.com/nodejs/node/pull/48159)
* \[[`0347a18056`](https://github.com/nodejs/node/commit/0347a18056)] - **src**: include missing header in node\_sea.h (Joyee Cheung) [#48152](https://github.com/nodejs/node/pull/48152)
* \[[`45c3782c20`](https://github.com/nodejs/node/commit/45c3782c20)] - **src**: remove INT\_MAX asserts in SecretKeyGenTraits (Tobias Nießen) [#48053](https://github.com/nodejs/node/pull/48053)
* \[[`b25e7045ad`](https://github.com/nodejs/node/commit/b25e7045ad)] - **src**: avoid prototype access in binding templates (Joyee Cheung) [#47913](https://github.com/nodejs/node/pull/47913)
* \[[`33aa373eec`](https://github.com/nodejs/node/commit/33aa373eec)] - **src**: use Blob{Des|S}erializer for SEA blobs (Joyee Cheung) [#47962](https://github.com/nodejs/node/pull/47962)
* \[[`9e2b13dfa7`](https://github.com/nodejs/node/commit/9e2b13dfa7)] - **stream**: deprecate asIndexedPairs (Chemi Atlow) [#48102](https://github.com/nodejs/node/pull/48102)
* \[[`96c323dee2`](https://github.com/nodejs/node/commit/96c323dee2)] - **test**: mark test-child-process-pipe-dataflow as flaky (Moshe Atlow) [#48334](https://github.com/nodejs/node/pull/48334)
* \[[`9875885357`](https://github.com/nodejs/node/commit/9875885357)] - **test**: adapt tests for OpenSSL 3.1 (OttoHollmann) [#47859](https://github.com/nodejs/node/pull/47859)
* \[[`3440d7c6bf`](https://github.com/nodejs/node/commit/3440d7c6bf)] - **test**: unflake test-vm-timeout-escape-nexttick (Santiago Gimeno) [#48078](https://github.com/nodejs/node/pull/48078)
* \[[`215b2bc72c`](https://github.com/nodejs/node/commit/215b2bc72c)] - **test**: fix zlib version regex (Luigi Pinca) [#48227](https://github.com/nodejs/node/pull/48227)
* \[[`e12ee59d26`](https://github.com/nodejs/node/commit/e12ee59d26)] - **test**: use lower security level in s\_client (Luigi Pinca) [#48192](https://github.com/nodejs/node/pull/48192)
* \[[`1dabc7390c`](https://github.com/nodejs/node/commit/1dabc7390c)] - _**Revert**_ "**test**: unskip negative-settimeout.any.js WPT" (Filip Skokan) [#48182](https://github.com/nodejs/node/pull/48182)
* \[[`c1c4796a86`](https://github.com/nodejs/node/commit/c1c4796a86)] - **test**: mark test\_cannot\_run\_js as flaky (Keyhan Vakil) [#48181](https://github.com/nodejs/node/pull/48181)
* \[[`8c49d74002`](https://github.com/nodejs/node/commit/8c49d74002)] - **test**: fix flaky test-runner-watch-mode (Moshe Atlow) [#48144](https://github.com/nodejs/node/pull/48144)
* \[[`6388766862`](https://github.com/nodejs/node/commit/6388766862)] - **test**: skip test-http-pipeline-flood on IBM i (Abdirahim Musse) [#48048](https://github.com/nodejs/node/pull/48048)
* \[[`8d2a3b1952`](https://github.com/nodejs/node/commit/8d2a3b1952)] - **test**: ignore helper files in WPTs (Filip Skokan) [#48079](https://github.com/nodejs/node/pull/48079)
* \[[`7a96d825fd`](https://github.com/nodejs/node/commit/7a96d825fd)] - **test**: move `test-cluster-primary-error` flaky test (Yagiz Nizipli) [#48039](https://github.com/nodejs/node/pull/48039)
* \[[`a80dd3a8b3`](https://github.com/nodejs/node/commit/a80dd3a8b3)] - **test**: fix suite signal (Benjamin Gruenbaum) [#47800](https://github.com/nodejs/node/pull/47800)
* \[[`a41cfd183f`](https://github.com/nodejs/node/commit/a41cfd183f)] - **test**: fix parsing test flags (Daeyeon Jeong) [#48012](https://github.com/nodejs/node/pull/48012)
* \[[`4d4e506f2b`](https://github.com/nodejs/node/commit/4d4e506f2b)] - **test,doc,sea**: run SEA tests on ppc64 (Darshan Sen) [#48111](https://github.com/nodejs/node/pull/48111)
* \[[`44411fc40c`](https://github.com/nodejs/node/commit/44411fc40c)] - **test\_runner**: apply `runOnly` on suites (Moshe Atlow) [#48279](https://github.com/nodejs/node/pull/48279)
* \[[`3f259b7a30`](https://github.com/nodejs/node/commit/3f259b7a30)] - **test\_runner**: emit `test:watch:drained` event (Moshe Atlow) [#48259](https://github.com/nodejs/node/pull/48259)
* \[[`c9f8e8c562`](https://github.com/nodejs/node/commit/c9f8e8c562)] - **test\_runner**: stop watch mode when abortSignal aborted (Moshe Atlow) [#48259](https://github.com/nodejs/node/pull/48259)
* \[[`f3268d64cb`](https://github.com/nodejs/node/commit/f3268d64cb)] - **test\_runner**: fix global after hook (Moshe Atlow) [#48231](https://github.com/nodejs/node/pull/48231)
* \[[`15336c3139`](https://github.com/nodejs/node/commit/15336c3139)] - **test\_runner**: remove redundant check from coverage (Colin Ihrig) [#48070](https://github.com/nodejs/node/pull/48070)
* \[[`750d3e8606`](https://github.com/nodejs/node/commit/750d3e8606)] - **test\_runner**: pass FORCE\_COLOR to child process (Moshe Atlow) [#48057](https://github.com/nodejs/node/pull/48057)
* \[[`3278542243`](https://github.com/nodejs/node/commit/3278542243)] - **test\_runner**: dont split lines on `test:stdout` (Moshe Atlow) [#48057](https://github.com/nodejs/node/pull/48057)
* \[[`027c531766`](https://github.com/nodejs/node/commit/027c531766)] - **test\_runner**: fix test deserialize edge cases (Moshe Atlow) [#48106](https://github.com/nodejs/node/pull/48106)
* \[[`2b797a6d39`](https://github.com/nodejs/node/commit/2b797a6d39)] - **test\_runner**: delegate stderr and stdout formatting to reporter (Shiba) [#48045](https://github.com/nodejs/node/pull/48045)
* \[[`23d310bee8`](https://github.com/nodejs/node/commit/23d310bee8)] - **test\_runner**: display dot report as wide as the terminal width (Raz Luvaton) [#48038](https://github.com/nodejs/node/pull/48038)
* \[[`fd2620dcf1`](https://github.com/nodejs/node/commit/fd2620dcf1)] - **tls**: reapply servername on happy eyeballs connect (Fedor Indutny) [#48255](https://github.com/nodejs/node/pull/48255)
* \[[`62f847d0b3`](https://github.com/nodejs/node/commit/62f847d0b3)] - **tools**: update rollup lint-md-dependencies (Node.js GitHub Bot) [#48329](https://github.com/nodejs/node/pull/48329)
* \[[`3e97826a66`](https://github.com/nodejs/node/commit/3e97826a66)] - _**Revert**_ "**tools**: open issue when update workflow fails" (Marco Ippolito) [#48312](https://github.com/nodejs/node/pull/48312)
* \[[`5f08bfe35f`](https://github.com/nodejs/node/commit/5f08bfe35f)] - **tools**: don't gitignore base64 config.h (Ben Noordhuis) [#48174](https://github.com/nodejs/node/pull/48174)
* \[[`ded0e2d755`](https://github.com/nodejs/node/commit/ded0e2d755)] - **tools**: update LICENSE and license-builder.sh (Santiago Gimeno) [#48078](https://github.com/nodejs/node/pull/48078)
* \[[`07aa264366`](https://github.com/nodejs/node/commit/07aa264366)] - **tools**: automate histogram update (Marco Ippolito) [#48171](https://github.com/nodejs/node/pull/48171)
* \[[`1416b75eaa`](https://github.com/nodejs/node/commit/1416b75eaa)] - **tools**: use shasum instead of sha256sum (Luigi Pinca) [#48229](https://github.com/nodejs/node/pull/48229)
* \[[`b81e9d9b7b`](https://github.com/nodejs/node/commit/b81e9d9b7b)] - **tools**: harmonize `dep_updaters` scripts (Antoine du Hamel) [#48201](https://github.com/nodejs/node/pull/48201)
* \[[`a60bc41e53`](https://github.com/nodejs/node/commit/a60bc41e53)] - **tools**: deps update authenticate github api request (Andrea Fassina) [#48200](https://github.com/nodejs/node/pull/48200)
* \[[`7478ed014e`](https://github.com/nodejs/node/commit/7478ed014e)] - **tools**: order dependency jobs alphabetically (Luca) [#48184](https://github.com/nodejs/node/pull/48184)
* \[[`568a705799`](https://github.com/nodejs/node/commit/568a705799)] - **tools**: refactor v8\_pch config (Michaël Zasso) [#47364](https://github.com/nodejs/node/pull/47364)
* \[[`801573ba46`](https://github.com/nodejs/node/commit/801573ba46)] - **tools**: log and verify sha256sum (Andrea Fassina) [#48088](https://github.com/nodejs/node/pull/48088)
* \[[`db62325e18`](https://github.com/nodejs/node/commit/db62325e18)] - **tools**: open issue when update workflow fails (Marco Ippolito) [#48018](https://github.com/nodejs/node/pull/48018)
* \[[`ad8a68856d`](https://github.com/nodejs/node/commit/ad8a68856d)] - **tools**: alphabetize CODEOWNERS (Rich Trott) [#48124](https://github.com/nodejs/node/pull/48124)
* \[[`4cf5a9edaf`](https://github.com/nodejs/node/commit/4cf5a9edaf)] - **tools**: use latest upstream commit for zlib updates (Andrea Fassina) [#48054](https://github.com/nodejs/node/pull/48054)
* \[[`8d93af381b`](https://github.com/nodejs/node/commit/8d93af381b)] - **tools**: add security-wg as dep updaters owner (Marco Ippolito) [#48113](https://github.com/nodejs/node/pull/48113)
* \[[`5325be1d99`](https://github.com/nodejs/node/commit/5325be1d99)] - **tools**: port js2c.py to C++ (Joyee Cheung) [#46997](https://github.com/nodejs/node/pull/46997)
* \[[`6c60d90277`](https://github.com/nodejs/node/commit/6c60d90277)] - **tools**: fix race condition when npm installing (Tobias Nießen) [#48101](https://github.com/nodejs/node/pull/48101)
* \[[`0ab840a58f`](https://github.com/nodejs/node/commit/0ab840a58f)] - **tools**: refloat 7 Node.js patches to cpplint.py (Rich Trott) [#48098](https://github.com/nodejs/node/pull/48098)
* \[[`a298193378`](https://github.com/nodejs/node/commit/a298193378)] - **tools**: update cpplint to 1.6.1 (Yagiz Nizipli) [#48098](https://github.com/nodejs/node/pull/48098)
* \[[`f6725751b7`](https://github.com/nodejs/node/commit/f6725751b7)] - **tools**: update eslint to 8.41.0 (Node.js GitHub Bot) [#48097](https://github.com/nodejs/node/pull/48097)
* \[[`6539361f4e`](https://github.com/nodejs/node/commit/6539361f4e)] - **tools**: update lint-md-dependencies (Node.js GitHub Bot) [#48096](https://github.com/nodejs/node/pull/48096)
* \[[`5d94dbb951`](https://github.com/nodejs/node/commit/5d94dbb951)] - **tools**: update doc to remark-parse\@10.0.2 (Node.js GitHub Bot) [#48095](https://github.com/nodejs/node/pull/48095)
* \[[`2226088048`](https://github.com/nodejs/node/commit/2226088048)] - **tools**: add debug logs (Marco Ippolito) [#48060](https://github.com/nodejs/node/pull/48060)
* \[[`0c8c383583`](https://github.com/nodejs/node/commit/0c8c383583)] - **tools**: fix zconf.h path (Luigi Pinca) [#48089](https://github.com/nodejs/node/pull/48089)
* \[[`6adaf4c648`](https://github.com/nodejs/node/commit/6adaf4c648)] - **tools**: update remark-preset-lint-node to 4.0.0 (Node.js GitHub Bot) [#47995](https://github.com/nodejs/node/pull/47995)
* \[[`92b3334231`](https://github.com/nodejs/node/commit/92b3334231)] - **url**: clean vertical alignment of docs (Robin Ury) [#48037](https://github.com/nodejs/node/pull/48037)
* \[[`ebb6536775`](https://github.com/nodejs/node/commit/ebb6536775)] - **url**: call `ada::can_parse` directly (Yagiz Nizipli) [#47919](https://github.com/nodejs/node/pull/47919)
* \[[`ed4514294a`](https://github.com/nodejs/node/commit/ed4514294a)] - **vm**: properly handle defining symbol props (Nicolas DUBIEN) [#47572](https://github.com/nodejs/node/pull/47572)

<a id="20.2.0"></a>

## 2023-05-16, Version 20.2.0 (Current), @targos

### Notable Changes

* \[[`c092df9094`](https://github.com/nodejs/node/commit/c092df9094)] - **doc**: add ovflowd to collaborators (Claudio Wunder) [#47844](https://github.com/nodejs/node/pull/47844)
* \[[`4197a9a5a0`](https://github.com/nodejs/node/commit/4197a9a5a0)] - **(SEMVER-MINOR)** **http**: prevent writing to the body when not allowed by HTTP spec (Gerrard Lindsay) [#47732](https://github.com/nodejs/node/pull/47732)
* \[[`c4596b9ce7`](https://github.com/nodejs/node/commit/c4596b9ce7)] - **(SEMVER-MINOR)** **sea**: add option to disable the experimental SEA warning (Darshan Sen) [#47588](https://github.com/nodejs/node/pull/47588)
* \[[`17befe008c`](https://github.com/nodejs/node/commit/17befe008c)] - **(SEMVER-MINOR)** **test\_runner**: add `skip`, `todo`, and `only` shorthands to `test` (Chemi Atlow) [#47909](https://github.com/nodejs/node/pull/47909)
* \[[`a0634d7f89`](https://github.com/nodejs/node/commit/a0634d7f89)] - **(SEMVER-MINOR)** **url**: add value argument to `URLSearchParams` `has` and `delete` methods (Sankalp Shubham) [#47885](https://github.com/nodejs/node/pull/47885)

### Commits

* \[[`456fca0d9c`](https://github.com/nodejs/node/commit/456fca0d9c)] - **bootstrap**: initialize per-isolate properties of bindings separately (Joyee Cheung) [#47768](https://github.com/nodejs/node/pull/47768)
* \[[`d6d12bf978`](https://github.com/nodejs/node/commit/d6d12bf978)] - **bootstrap**: log isolate data info in mksnapshot debug logs (Joyee Cheung) [#47768](https://github.com/nodejs/node/pull/47768)
* \[[`e457d89a1b`](https://github.com/nodejs/node/commit/e457d89a1b)] - **buffer**: combine checking range of sourceStart in `buf.copy` (Deokjin Kim) [#47758](https://github.com/nodejs/node/pull/47758)
* \[[`00668fcfb4`](https://github.com/nodejs/node/commit/00668fcfb4)] - **child\_process**: use signal.reason in child process abort (Debadree Chatterjee) [#47817](https://github.com/nodejs/node/pull/47817)
* \[[`d7993474ea`](https://github.com/nodejs/node/commit/d7993474ea)] - **crypto**: remove default encoding from scrypt (Tobias Nießen) [#47943](https://github.com/nodejs/node/pull/47943)
* \[[`09fb74a7cc`](https://github.com/nodejs/node/commit/09fb74a7cc)] - **crypto**: fix webcrypto private/secret import with empty usages (Filip Skokan) [#47877](https://github.com/nodejs/node/pull/47877)
* \[[`e9c6ee74f3`](https://github.com/nodejs/node/commit/e9c6ee74f3)] - **crypto**: remove default encoding from pbkdf2 (Tobias Nießen) [#47869](https://github.com/nodejs/node/pull/47869)
* \[[`b7f13a8679`](https://github.com/nodejs/node/commit/b7f13a8679)] - **deps**: update simdutf to 3.2.9 (Node.js GitHub Bot) [#47983](https://github.com/nodejs/node/pull/47983)
* \[[`b16f6da153`](https://github.com/nodejs/node/commit/b16f6da153)] - **deps**: V8: cherry-pick 5f025d1ca2ca (Michaël Zasso) [#47610](https://github.com/nodejs/node/pull/47610)
* \[[`99f8fcab45`](https://github.com/nodejs/node/commit/99f8fcab45)] - **deps**: V8: cherry-pick a8a11a87cb72 (Michaël Zasso) [#47610](https://github.com/nodejs/node/pull/47610)
* \[[`c2b14b4c78`](https://github.com/nodejs/node/commit/c2b14b4c78)] - **deps**: update ada to 2.4.0 (Node.js GitHub Bot) [#47922](https://github.com/nodejs/node/pull/47922)
* \[[`cad42e7a56`](https://github.com/nodejs/node/commit/cad42e7a56)] - **deps**: V8: cherry-pick 1b471b796022 (Lu Yahan) [#47399](https://github.com/nodejs/node/pull/47399)
* \[[`7b2f17ca59`](https://github.com/nodejs/node/commit/7b2f17ca59)] - **deps**: upgrade npm to 9.6.6 (npm team) [#47862](https://github.com/nodejs/node/pull/47862)
* \[[`d23b1af562`](https://github.com/nodejs/node/commit/d23b1af562)] - **deps**: update ada to 2.3.1 (Node.js GitHub Bot) [#47893](https://github.com/nodejs/node/pull/47893)
* \[[`72340c98fb`](https://github.com/nodejs/node/commit/72340c98fb)] - **dgram**: convert macro to template (Tobias Nießen) [#47891](https://github.com/nodejs/node/pull/47891)
* \[[`9be922892f`](https://github.com/nodejs/node/commit/9be922892f)] - **dns**: call `ada::idna::to_ascii` directly from c++ (Yagiz Nizipli) [#47920](https://github.com/nodejs/node/pull/47920)
* \[[`4a1e97156a`](https://github.com/nodejs/node/commit/4a1e97156a)] - **doc**: add missing deprecated blocks to cluster (Tobias Nießen) [#47981](https://github.com/nodejs/node/pull/47981)
* \[[`13118a19ee`](https://github.com/nodejs/node/commit/13118a19ee)] - **doc**: update description of global (Tobias Nießen) [#47969](https://github.com/nodejs/node/pull/47969)
* \[[`372796440b`](https://github.com/nodejs/node/commit/372796440b)] - **doc**: update measure memory rejection information (Yash Ladha) [#41639](https://github.com/nodejs/node/pull/41639)
* \[[`7ecc6740e4`](https://github.com/nodejs/node/commit/7ecc6740e4)] - **doc**: fix broken link to TC39 import attributes proposal (Rich Trott) [#47954](https://github.com/nodejs/node/pull/47954)
* \[[`b9771c95c7`](https://github.com/nodejs/node/commit/b9771c95c7)] - **doc**: fix broken link (Rich Trott) [#47953](https://github.com/nodejs/node/pull/47953)
* \[[`6f5ba92e61`](https://github.com/nodejs/node/commit/6f5ba92e61)] - **doc**: remove broken link (Rich Trott) [#47942](https://github.com/nodejs/node/pull/47942)
* \[[`c9ffc555f1`](https://github.com/nodejs/node/commit/c9ffc555f1)] - **doc**: document make lint-md-clean (Matteo Collina) [#47926](https://github.com/nodejs/node/pull/47926)
* \[[`7ed99e8ba5`](https://github.com/nodejs/node/commit/7ed99e8ba5)] - **doc**: mark global object as legacy (Mert Can Altın) [#47819](https://github.com/nodejs/node/pull/47819)
* \[[`bf39f2d252`](https://github.com/nodejs/node/commit/bf39f2d252)] - **doc**: ntfs junction points must link to directories (Ben Noordhuis) [#47907](https://github.com/nodejs/node/pull/47907)
* \[[`4dfc3890d8`](https://github.com/nodejs/node/commit/4dfc3890d8)] - **doc**: improve `permission.has` description (Daeyeon Jeong) [#47875](https://github.com/nodejs/node/pull/47875)
* \[[`93f1aa2856`](https://github.com/nodejs/node/commit/93f1aa2856)] - **doc**: fix params names (Dmitry Semigradsky) [#47853](https://github.com/nodejs/node/pull/47853)
* \[[`9a362aa2fb`](https://github.com/nodejs/node/commit/9a362aa2fb)] - **doc**: update supported version of FreeBSD to 12.4 (Michaël Zasso) [#47838](https://github.com/nodejs/node/pull/47838)
* \[[`89c70dc6e6`](https://github.com/nodejs/node/commit/89c70dc6e6)] - **doc**: add stability experimental to pm (Rafael Gonzaga) [#47890](https://github.com/nodejs/node/pull/47890)
* \[[`f96fb2eee7`](https://github.com/nodejs/node/commit/f96fb2eee7)] - **doc**: swap Matteo with Rafael in the stewards (Rafael Gonzaga) [#47841](https://github.com/nodejs/node/pull/47841)
* \[[`1666a146e3`](https://github.com/nodejs/node/commit/1666a146e3)] - **doc**: add valgrind suppression details (Kevin Eady) [#47760](https://github.com/nodejs/node/pull/47760)
* \[[`e53e8231ff`](https://github.com/nodejs/node/commit/e53e8231ff)] - **doc**: replace EOL versions in README (Tobias Nießen) [#47833](https://github.com/nodejs/node/pull/47833)
* \[[`c092df9094`](https://github.com/nodejs/node/commit/c092df9094)] - **doc**: add ovflowd to collaborators (Claudio Wunder) [#47844](https://github.com/nodejs/node/pull/47844)
* \[[`f7106765b3`](https://github.com/nodejs/node/commit/f7106765b3)] - **doc**: update BUILDING.md previous versions links (Tobias Nießen) [#47835](https://github.com/nodejs/node/pull/47835)
* \[[`811b43c215`](https://github.com/nodejs/node/commit/811b43c215)] - **doc,test**: update the v8.startupSnapshot doc and test the example (Joyee Cheung) [#47468](https://github.com/nodejs/node/pull/47468)
* \[[`1ec640ac70`](https://github.com/nodejs/node/commit/1ec640ac70)] - **esm**: do not use `'beforeExit'` on the main thread (Antoine du Hamel) [#47964](https://github.com/nodejs/node/pull/47964)
* \[[`106dc612d6`](https://github.com/nodejs/node/commit/106dc612d6)] - **fs**: make readdir recursive algorithm iterative (Ethan Arrowood) [#47650](https://github.com/nodejs/node/pull/47650)
* \[[`a0da2348a8`](https://github.com/nodejs/node/commit/a0da2348a8)] - **fs**: move fs\_use\_promises\_symbol to per-isolate symbols (Joyee Cheung) [#47768](https://github.com/nodejs/node/pull/47768)
* \[[`4197a9a5a0`](https://github.com/nodejs/node/commit/4197a9a5a0)] - **(SEMVER-MINOR)** **http**: prevent writing to the body when not allowed by HTTP spec (Gerrard Lindsay) [#47732](https://github.com/nodejs/node/pull/47732)
* \[[`a4d6543598`](https://github.com/nodejs/node/commit/a4d6543598)] - **http2**: improve nghttp2 error callback (Tobias Nießen) [#47840](https://github.com/nodejs/node/pull/47840)
* \[[`a4fed6c580`](https://github.com/nodejs/node/commit/a4fed6c580)] - **lib**: update comment (sinkhaha) [#47884](https://github.com/nodejs/node/pull/47884)
* \[[`fd8bec7b2b`](https://github.com/nodejs/node/commit/fd8bec7b2b)] - **meta**: bump step-security/harden-runner from 2.3.1 to 2.4.0 (Rich Trott) [#47980](https://github.com/nodejs/node/pull/47980)
* \[[`f5b4b6d5dc`](https://github.com/nodejs/node/commit/f5b4b6d5dc)] - **meta**: bump github/codeql-action from 2.3.2 to 2.3.3 (Rich Trott) [#47979](https://github.com/nodejs/node/pull/47979)
* \[[`c05c0a2359`](https://github.com/nodejs/node/commit/c05c0a2359)] - **meta**: bump actions/setup-python from 4.5.0 to 4.6.0 (Rich Trott) [#47968](https://github.com/nodejs/node/pull/47968)
* \[[`2a3d6d97cb`](https://github.com/nodejs/node/commit/2a3d6d97cb)] - **meta**: add security-wg ping to permission.js (Rafael Gonzaga) [#47941](https://github.com/nodejs/node/pull/47941)
* \[[`6c158e8dd1`](https://github.com/nodejs/node/commit/6c158e8dd1)] - **meta**: bump step-security/harden-runner from 2.2.1 to 2.3.1 (dependabot\[bot]) [#47808](https://github.com/nodejs/node/pull/47808)
* \[[`f7a8094d37`](https://github.com/nodejs/node/commit/f7a8094d37)] - **meta**: bump actions/setup-python from 4.5.0 to 4.6.0 (dependabot\[bot]) [#47806](https://github.com/nodejs/node/pull/47806)
* \[[`0f58e48792`](https://github.com/nodejs/node/commit/0f58e48792)] - **meta**: bump actions/checkout from 3.3.0 to 3.5.2 (dependabot\[bot]) [#47805](https://github.com/nodejs/node/pull/47805)
* \[[`652b06dd82`](https://github.com/nodejs/node/commit/652b06dd82)] - **meta**: remove extra space in scorecard workflow (Mestery) [#47805](https://github.com/nodejs/node/pull/47805)
* \[[`9f06eaccaf`](https://github.com/nodejs/node/commit/9f06eaccaf)] - **meta**: bump github/codeql-action from 2.2.9 to 2.3.2 (dependabot\[bot]) [#47809](https://github.com/nodejs/node/pull/47809)
* \[[`977fd7cf35`](https://github.com/nodejs/node/commit/977fd7cf35)] - **meta**: bump codecov/codecov-action from 3.1.1 to 3.1.3 (dependabot\[bot]) [#47807](https://github.com/nodejs/node/pull/47807)
* \[[`c19385c154`](https://github.com/nodejs/node/commit/c19385c154)] - **module**: refactor to use `normalizeRequirableId` in the CJS module loader (Darshan Sen) [#47896](https://github.com/nodejs/node/pull/47896)
* \[[`739113f2fc`](https://github.com/nodejs/node/commit/739113f2fc)] - **module**: block requiring `test/reporters` without scheme (Moshe Atlow) [#47831](https://github.com/nodejs/node/pull/47831)
* \[[`f489c6710c`](https://github.com/nodejs/node/commit/f489c6710c)] - **(NODE-API-SEMVER-MAJOR)** **node-api**: get Node API version used by addon (Vladimir Morozov) [#45715](https://github.com/nodejs/node/pull/45715)
* \[[`7222f9d74b`](https://github.com/nodejs/node/commit/7222f9d74b)] - **path**: indicate index of wrong resolve() parameter (sosoba) [#47660](https://github.com/nodejs/node/pull/47660)
* \[[`7dd32f1536`](https://github.com/nodejs/node/commit/7dd32f1536)] - **permission**: remove unused function declaration (Deokjin Kim) [#47957](https://github.com/nodejs/node/pull/47957)
* \[[`af86625a05`](https://github.com/nodejs/node/commit/af86625a05)] - **permission**: resolve reference to absolute path only for fs permission (Daeyeon Jeong) [#47930](https://github.com/nodejs/node/pull/47930)
* \[[`1625ae11fe`](https://github.com/nodejs/node/commit/1625ae11fe)] - **quic**: address recent coverity warning (Michael Dawson) [#47753](https://github.com/nodejs/node/pull/47753)
* \[[`c4596b9ce7`](https://github.com/nodejs/node/commit/c4596b9ce7)] - **(SEMVER-MINOR)** **sea**: add option to disable the experimental SEA warning (Darshan Sen) [#47588](https://github.com/nodejs/node/pull/47588)
* \[[`1a7fc186bc`](https://github.com/nodejs/node/commit/1a7fc186bc)] - **sea**: allow requiring core modules with the "node:" prefix (Darshan Sen) [#47779](https://github.com/nodejs/node/pull/47779)
* \[[`786a1c5398`](https://github.com/nodejs/node/commit/786a1c5398)] - **src**: deduplicate X509Certificate::Fingerprint\* (Tobias Nießen) [#47978](https://github.com/nodejs/node/pull/47978)
* \[[`060c1d502b`](https://github.com/nodejs/node/commit/060c1d502b)] - **src**: stop copying code cache, part 2 (Keyhan Vakil) [#47958](https://github.com/nodejs/node/pull/47958)
* \[[`1aec718619`](https://github.com/nodejs/node/commit/1aec718619)] - **(SEMVER-MINOR)** **src**: add cjs\_module\_lexer\_version base64\_version (Jithil P Ponnan) [#45629](https://github.com/nodejs/node/pull/45629)
* \[[`0c06bfd8dc`](https://github.com/nodejs/node/commit/0c06bfd8dc)] - **src**: move BlobSerializerDeserializer to a separate header file (Darshan Sen) [#47933](https://github.com/nodejs/node/pull/47933)
* \[[`bd553e7521`](https://github.com/nodejs/node/commit/bd553e7521)] - **src**: rename SKIP\_CHECK\_SIZE to SKIP\_CHECK\_STRLEN (Tobias Nießen) [#47845](https://github.com/nodejs/node/pull/47845)
* \[[`190596c189`](https://github.com/nodejs/node/commit/190596c189)] - **src**: register external references for source code (Keyhan Vakil) [#47055](https://github.com/nodejs/node/pull/47055)
* \[[`4293cc47f4`](https://github.com/nodejs/node/commit/4293cc47f4)] - **src**: support V8 experimental shared values in messaging (Shu-yu Guo) [#47706](https://github.com/nodejs/node/pull/47706)
* \[[`9bc5d78f0c`](https://github.com/nodejs/node/commit/9bc5d78f0c)] - **src**: register ext reference for Fingerprint512 (Tobias Nießen) [#47892](https://github.com/nodejs/node/pull/47892)
* \[[`a11507e23b`](https://github.com/nodejs/node/commit/a11507e23b)] - **src**: stop copying code cache (Keyhan Vakil) [#47144](https://github.com/nodejs/node/pull/47144)
* \[[`515c9b8de6`](https://github.com/nodejs/node/commit/515c9b8de6)] - **src**: clarify the parameter name in `Permission::Apply` (Daeyeon Jeong) [#47874](https://github.com/nodejs/node/pull/47874)
* \[[`c4217613f5`](https://github.com/nodejs/node/commit/c4217613f5)] - **src**: fix creating an ArrayBuffer from a Blob created with `openAsBlob` (Daeyeon Jeong) [#47691](https://github.com/nodejs/node/pull/47691)
* \[[`4bc17fd67b`](https://github.com/nodejs/node/commit/4bc17fd67b)] - **src**: avoid strcmp() with Utf8Value (Tobias Nießen) [#47827](https://github.com/nodejs/node/pull/47827)
* \[[`d358317f70`](https://github.com/nodejs/node/commit/d358317f70)] - **src**: get binding data store directly from the realm (Joyee Cheung) [#47437](https://github.com/nodejs/node/pull/47437)
* \[[`b04d51a0b5`](https://github.com/nodejs/node/commit/b04d51a0b5)] - **src**: prefer data accessor of string and vector (Mohammed Keyvanzadeh) [#47750](https://github.com/nodejs/node/pull/47750)
* \[[`2952cc576c`](https://github.com/nodejs/node/commit/2952cc576c)] - **src**: add per-isolate SetFastMethod and Set\[Fast]MethodNoSideEffect (Joyee Cheung) [#47768](https://github.com/nodejs/node/pull/47768)
* \[[`010d2ecf94`](https://github.com/nodejs/node/commit/010d2ecf94)] - **test**: mark test-esm-loader-http-imports as flaky (Tobias Nießen) [#47987](https://github.com/nodejs/node/pull/47987)
* \[[`bb33c74c07`](https://github.com/nodejs/node/commit/bb33c74c07)] - **test**: add getRandomValues return length (Jithil P Ponnan) [#46357](https://github.com/nodejs/node/pull/46357)
* \[[`6e019586f7`](https://github.com/nodejs/node/commit/6e019586f7)] - **test**: unskip negative-settimeout.any.js WPT (Filip Skokan) [#47946](https://github.com/nodejs/node/pull/47946)
* \[[`8f547afe5f`](https://github.com/nodejs/node/commit/8f547afe5f)] - **test**: use appropriate usages for a negative import test (Filip Skokan) [#47878](https://github.com/nodejs/node/pull/47878)
* \[[`7e34f77518`](https://github.com/nodejs/node/commit/7e34f77518)] - **test**: fix webcrypto wrap unwrap tests (Filip Skokan) [#47876](https://github.com/nodejs/node/pull/47876)
* \[[`30f4f35244`](https://github.com/nodejs/node/commit/30f4f35244)] - **test**: fix output tests when path includes node version (Moshe Atlow) [#47843](https://github.com/nodejs/node/pull/47843)
* \[[`54607bfd68`](https://github.com/nodejs/node/commit/54607bfd68)] - **test**: reduce WPT concurrency (Filip Skokan) [#47834](https://github.com/nodejs/node/pull/47834)
* \[[`17945a2495`](https://github.com/nodejs/node/commit/17945a2495)] - **test**: migrate a pseudo\_tty test to use assertSnapshot (Moshe Atlow) [#47803](https://github.com/nodejs/node/pull/47803)
* \[[`c9233679e8`](https://github.com/nodejs/node/commit/c9233679e8)] - **test**: fix WPT state when process exits but workers are still running (Filip Skokan) [#47826](https://github.com/nodejs/node/pull/47826)
* \[[`34bfb69b5b`](https://github.com/nodejs/node/commit/34bfb69b5b)] - **test**: migrate message tests to use assertSnapshot (Moshe Atlow) [#47498](https://github.com/nodejs/node/pull/47498)
* \[[`d25c785c2a`](https://github.com/nodejs/node/commit/d25c785c2a)] - **test**: allow SIGBUS in signal-handler abort test (Michaël Zasso) [#47851](https://github.com/nodejs/node/pull/47851)
* \[[`aa2c7e00d7`](https://github.com/nodejs/node/commit/aa2c7e00d7)] - **test,crypto**: update WebCryptoAPI WPT (Filip Skokan) [#47921](https://github.com/nodejs/node/pull/47921)
* \[[`da27542058`](https://github.com/nodejs/node/commit/da27542058)] - **test\_runner**: use v8.serialize instead of TAP (Moshe Atlow) [#47867](https://github.com/nodejs/node/pull/47867)
* \[[`17befe008c`](https://github.com/nodejs/node/commit/17befe008c)] - **(SEMVER-MINOR)** **test\_runner**: add shorthands to `test` (Chemi Atlow) [#47909](https://github.com/nodejs/node/pull/47909)
* \[[`42db1d50a0`](https://github.com/nodejs/node/commit/42db1d50a0)] - **test\_runner**: fix ordering of test hooks (Phil Nash) [#47931](https://github.com/nodejs/node/pull/47931)
* \[[`d81c54e3a8`](https://github.com/nodejs/node/commit/d81c54e3a8)] - **test\_runner**: omit inaccessible files from coverage (Colin Ihrig) [#47850](https://github.com/nodejs/node/pull/47850)
* \[[`a4e261e910`](https://github.com/nodejs/node/commit/a4e261e910)] - **tools**: debug log for nghttp3 (Marco Ippolito) [#47992](https://github.com/nodejs/node/pull/47992)
* \[[`f6ff318d4c`](https://github.com/nodejs/node/commit/f6ff318d4c)] - **tools**: automate icu-small update (Marco Ippolito) [#47727](https://github.com/nodejs/node/pull/47727)
* \[[`706c305381`](https://github.com/nodejs/node/commit/706c305381)] - **tools**: update lint-md-dependencies to rollup\@3.21.5 (Node.js GitHub Bot) [#47903](https://github.com/nodejs/node/pull/47903)
* \[[`e22c686ca9`](https://github.com/nodejs/node/commit/e22c686ca9)] - **tools**: update eslint to 8.40.0 (Node.js GitHub Bot) [#47906](https://github.com/nodejs/node/pull/47906)
* \[[`36f7cfac93`](https://github.com/nodejs/node/commit/36f7cfac93)] - **tools**: update eslint to 8.39.0 (Node.js GitHub Bot) [#47789](https://github.com/nodejs/node/pull/47789)
* \[[`7323902a40`](https://github.com/nodejs/node/commit/7323902a40)] - **tools**: fix jsdoc lint (Moshe Atlow) [#47789](https://github.com/nodejs/node/pull/47789)
* \[[`a0634d7f89`](https://github.com/nodejs/node/commit/a0634d7f89)] - **(SEMVER-MINOR)** **url**: add value argument to has and delete methods (Sankalp Shubham) [#47885](https://github.com/nodejs/node/pull/47885)
* \[[`1b06c1e003`](https://github.com/nodejs/node/commit/1b06c1e003)] - **url**: improve `isURL` detection (Yagiz Nizipli) [#47886](https://github.com/nodejs/node/pull/47886)
* \[[`2bd869d20c`](https://github.com/nodejs/node/commit/2bd869d20c)] - **vm**: fix crash when setting \_\_proto\_\_ on context's globalThis (Feng Yu) [#47939](https://github.com/nodejs/node/pull/47939)
* \[[`e6685f9e82`](https://github.com/nodejs/node/commit/e6685f9e82)] - **vm,lib**: refactor microtaskQueue assignment logic (Khaidi Chu) [#47765](https://github.com/nodejs/node/pull/47765)
* \[[`47fea13dac`](https://github.com/nodejs/node/commit/47fea13dac)] - **worker**: support more cases when (de)serializing errors (Moshe Atlow) [#47925](https://github.com/nodejs/node/pull/47925)
* \[[`6f3876c035`](https://github.com/nodejs/node/commit/6f3876c035)] - **worker**: use snapshot in workers spawned by workers (Joyee Cheung) [#47731](https://github.com/nodejs/node/pull/47731)

<a id="20.1.0"></a>

## 2023-05-03, Version 20.1.0 (Current), @targos

### Notable Changes

* \[[`5e99598639`](https://github.com/nodejs/node/commit/5e99598639)] - **assert**: deprecate `CallTracker` (Moshe Atlow) [#47740](https://github.com/nodejs/node/pull/47740)
* \[[`2d97c89c6f`](https://github.com/nodejs/node/commit/2d97c89c6f)] - **crypto**: update root certificates to NSS 3.89 (Node.js GitHub Bot) [#47659](https://github.com/nodejs/node/pull/47659)
* \[[`ce8820e292`](https://github.com/nodejs/node/commit/ce8820e292)] - **(SEMVER-MINOR)** **dns**: expose `getDefaultResultOrder` (btea) [#46973](https://github.com/nodejs/node/pull/46973)
* \[[`9d30f469aa`](https://github.com/nodejs/node/commit/9d30f469aa)] - **doc**: add KhafraDev to collaborators (Matthew Aitken) [#47510](https://github.com/nodejs/node/pull/47510)
* \[[`439ea47a77`](https://github.com/nodejs/node/commit/439ea47a77)] - **(SEMVER-MINOR)** **fs**: add `recursive` option to `readdir` and `opendir` (Ethan Arrowood) [#41439](https://github.com/nodejs/node/pull/41439)
* \[[`a54e898dc8`](https://github.com/nodejs/node/commit/a54e898dc8)] - **(SEMVER-MINOR)** **fs**: add support for `mode` flag to specify the copy behavior of the `cp` methods (Tetsuharu Ohzeki) [#47084](https://github.com/nodejs/node/pull/47084)
* \[[`4fa773964b`](https://github.com/nodejs/node/commit/4fa773964b)] - **(SEMVER-MINOR)** **http**: add `highWaterMark` option `http.createServer` (HinataKah0) [#47405](https://github.com/nodejs/node/pull/47405)
* \[[`2b411f4b42`](https://github.com/nodejs/node/commit/2b411f4b42)] - **(SEMVER-MINOR)** **stream**: preserve object mode in `compose` (Raz Luvaton) [#47413](https://github.com/nodejs/node/pull/47413)
* \[[`5327483f31`](https://github.com/nodejs/node/commit/5327483f31)] - **(SEMVER-MINOR)** **test\_runner**: add `testNamePatterns` to `run` API (Chemi Atlow) [#47628](https://github.com/nodejs/node/pull/47628)
* \[[`bdd02a467d`](https://github.com/nodejs/node/commit/bdd02a467d)] - **(SEMVER-MINOR)** **test\_runner**: execute `before` hook on test (Chemi Atlow) [#47586](https://github.com/nodejs/node/pull/47586)
* \[[`0e70c187bc`](https://github.com/nodejs/node/commit/0e70c187bc)] - **(SEMVER-MINOR)** **test\_runner**: support combining coverage reports (Colin Ihrig) [#47686](https://github.com/nodejs/node/pull/47686)
* \[[`75c1d1b66e`](https://github.com/nodejs/node/commit/75c1d1b66e)] - **(SEMVER-MINOR)** **wasi**: make `returnOnExit` true by default (Michael Dawson) [#47390](https://github.com/nodejs/node/pull/47390)

### Commits

* \[[`33d1bd3e02`](https://github.com/nodejs/node/commit/33d1bd3e02)] - **assert**: deprecate callTracker (Moshe Atlow) [#47740](https://github.com/nodejs/node/pull/47740)
* \[[`6d87355e83`](https://github.com/nodejs/node/commit/6d87355e83)] - **benchmark**: add eventtarget creation bench (Rafael Gonzaga) [#47774](https://github.com/nodejs/node/pull/47774)
* \[[`40324a1dea`](https://github.com/nodejs/node/commit/40324a1dea)] - **benchmark**: differentiate whatwg and legacy url (Yagiz Nizipli) [#47377](https://github.com/nodejs/node/pull/47377)
* \[[`936d7cb069`](https://github.com/nodejs/node/commit/936d7cb069)] - **benchmark**: add a benchmark for `defaultResolve` (Antoine du Hamel) [#47543](https://github.com/nodejs/node/pull/47543)
* \[[`202042ee93`](https://github.com/nodejs/node/commit/202042ee93)] - **bootstrap**: support namespaced builtins in snapshot scripts (Joyee Cheung) [#47467](https://github.com/nodejs/node/pull/47467)
* \[[`30af5cee55`](https://github.com/nodejs/node/commit/30af5cee55)] - **build**: use pathlib for paths (Mohammed Keyvanzadeh) [#47581](https://github.com/nodejs/node/pull/47581)
* \[[`089c9c51e9`](https://github.com/nodejs/node/commit/089c9c51e9)] - **build**: refactor configure.py (Mohammed Keyvanzadeh) [#47667](https://github.com/nodejs/node/pull/47667)
* \[[`5b851c8074`](https://github.com/nodejs/node/commit/5b851c8074)] - **build**: add devcontainer configuration (Tierney Cyren) [#40825](https://github.com/nodejs/node/pull/40825)
* \[[`35e8b3b467`](https://github.com/nodejs/node/commit/35e8b3b467)] - **build**: bump ossf/scorecard-action from 2.1.2 to 2.1.3 (dependabot\[bot]) [#47367](https://github.com/nodejs/node/pull/47367)
* \[[`78c08243df`](https://github.com/nodejs/node/commit/78c08243df)] - **build**: replace Python linter flake8 with ruff (Christian Clauss) [#47519](https://github.com/nodejs/node/pull/47519)
* \[[`2d97c89c6f`](https://github.com/nodejs/node/commit/2d97c89c6f)] - **crypto**: update root certificates to NSS 3.89 (Node.js GitHub Bot) [#47659](https://github.com/nodejs/node/pull/47659)
* \[[`420feb41cf`](https://github.com/nodejs/node/commit/420feb41cf)] - **crypto**: remove INT\_MAX restriction in randomBytes (Tobias Nießen) [#47559](https://github.com/nodejs/node/pull/47559)
* \[[`6046779dd9`](https://github.com/nodejs/node/commit/6046779dd9)] - **deps**: disable V8 concurrent sparkplug compilation (Michaël Zasso) [#47450](https://github.com/nodejs/node/pull/47450)
* \[[`00d461e93f`](https://github.com/nodejs/node/commit/00d461e93f)] - **deps**: V8: cherry-pick c5ab3e4f0c5a (Richard Lau) [#47736](https://github.com/nodejs/node/pull/47736)
* \[[`d08dd8069f`](https://github.com/nodejs/node/commit/d08dd8069f)] - **deps**: update ada to 2.3.0 (Node.js GitHub Bot) [#47737](https://github.com/nodejs/node/pull/47737)
* \[[`996245976b`](https://github.com/nodejs/node/commit/996245976b)] - **deps**: update undici to 5.22.0 (Node.js GitHub Bot) [#47679](https://github.com/nodejs/node/pull/47679)
* \[[`f3ee3126df`](https://github.com/nodejs/node/commit/f3ee3126df)] - **deps**: update ada to 2.2.0 (Node.js GitHub Bot) [#47678](https://github.com/nodejs/node/pull/47678)
* \[[`1391d3b9ff`](https://github.com/nodejs/node/commit/1391d3b9ff)] - **deps**: add minimatch as a dependency (Moshe Atlow) [#47499](https://github.com/nodejs/node/pull/47499)
* \[[`315454350d`](https://github.com/nodejs/node/commit/315454350d)] - **deps**: update ada to 2.1.0 (Node.js GitHub Bot) [#47598](https://github.com/nodejs/node/pull/47598)
* \[[`7f7735cad9`](https://github.com/nodejs/node/commit/7f7735cad9)] - **deps**: update ICU to 73.1 release (Steven R. Loomis) [#47456](https://github.com/nodejs/node/pull/47456)
* \[[`13105c12b7`](https://github.com/nodejs/node/commit/13105c12b7)] - **deps**: patch V8 to 11.3.244.8 (Michaël Zasso) [#47536](https://github.com/nodejs/node/pull/47536)
* \[[`ede69d272a`](https://github.com/nodejs/node/commit/ede69d272a)] - **deps**: update undici to 5.21.2 (Node.js GitHub Bot) [#47508](https://github.com/nodejs/node/pull/47508)
* \[[`64b5a5f872`](https://github.com/nodejs/node/commit/64b5a5f872)] - **deps**: update simdutf to 3.2.8 (Node.js GitHub Bot) [#47507](https://github.com/nodejs/node/pull/47507)
* \[[`2664536796`](https://github.com/nodejs/node/commit/2664536796)] - **deps**: V8: cherry-pick 8e10685ff918 (Jiawen Geng) [#47440](https://github.com/nodejs/node/pull/47440)
* \[[`ba9ec91f0e`](https://github.com/nodejs/node/commit/ba9ec91f0e)] - **deps**: update undici to 5.21.1 (Node.js GitHub Bot) [#47488](https://github.com/nodejs/node/pull/47488)
* \[[`ce8820e292`](https://github.com/nodejs/node/commit/ce8820e292)] - **(SEMVER-MINOR)** **dns**: expose getDefaultResultOrder (btea) [#46973](https://github.com/nodejs/node/pull/46973)
* \[[`4c26e28c33`](https://github.com/nodejs/node/commit/4c26e28c33)] - **doc**: create maintaining folder for deps (Marco Ippolito) [#47589](https://github.com/nodejs/node/pull/47589)
* \[[`aa0ef3eabd`](https://github.com/nodejs/node/commit/aa0ef3eabd)] - **doc**: fix --allow-\* CLI flag references (Tobias Nießen) [#47804](https://github.com/nodejs/node/pull/47804)
* \[[`98603b6fd3`](https://github.com/nodejs/node/commit/98603b6fd3)] - **doc**: clarify fs permissions only affect fs module (Tobias Nießen) [#47782](https://github.com/nodejs/node/pull/47782)
* \[[`3befe5dac9`](https://github.com/nodejs/node/commit/3befe5dac9)] - **doc**: add copy node executable guide on windows (XLor) [#47781](https://github.com/nodejs/node/pull/47781)
* \[[`98450d9892`](https://github.com/nodejs/node/commit/98450d9892)] - **doc**: remove MoLow from Triagers (Moshe Atlow) [#47792](https://github.com/nodejs/node/pull/47792)
* \[[`d75036410d`](https://github.com/nodejs/node/commit/d75036410d)] - **doc**: fix typo in webstreams.md (Christian Takle) [#47766](https://github.com/nodejs/node/pull/47766)
* \[[`ceba37a74f`](https://github.com/nodejs/node/commit/ceba37a74f)] - **doc**: move BethGriggs to regular member (Rich Trott) [#47776](https://github.com/nodejs/node/pull/47776)
* \[[`b954ea9781`](https://github.com/nodejs/node/commit/b954ea9781)] - **doc**: mark signing the binary is macOS and Windows only in SEA (Xuguang Mei) [#47722](https://github.com/nodejs/node/pull/47722)
* \[[`26bccbcd10`](https://github.com/nodejs/node/commit/26bccbcd10)] - **doc**: move addaleax to TSC emeriti (Anna Henningsen) [#47752](https://github.com/nodejs/node/pull/47752)
* \[[`20b0de242f`](https://github.com/nodejs/node/commit/20b0de242f)] - **doc**: add link to news for Node.js core (Michael Dawson) [#47704](https://github.com/nodejs/node/pull/47704)
* \[[`5709133dc7`](https://github.com/nodejs/node/commit/5709133dc7)] - **doc**: fix a typo in `permissions.md` (Daeyeon Jeong) [#47730](https://github.com/nodejs/node/pull/47730)
* \[[`c5c40a89f2`](https://github.com/nodejs/node/commit/c5c40a89f2)] - **doc**: async\_hooks asynchronous content example add mjs code (btea) [#47401](https://github.com/nodejs/node/pull/47401)
* \[[`a1403a8df2`](https://github.com/nodejs/node/commit/a1403a8df2)] - **doc**: clarify concurrency model of test runner (Tobias Nießen) [#47642](https://github.com/nodejs/node/pull/47642)
* \[[`c0c23fbe42`](https://github.com/nodejs/node/commit/c0c23fbe42)] - **doc**: fix a typo in `fs.openAsBlob` (Daeyeon Jeong) [#47693](https://github.com/nodejs/node/pull/47693)
* \[[`4cef98812d`](https://github.com/nodejs/node/commit/4cef98812d)] - **doc**: fix typos (Mohammed Keyvanzadeh) [#47685](https://github.com/nodejs/node/pull/47685)
* \[[`f30ef242ef`](https://github.com/nodejs/node/commit/f30ef242ef)] - **doc**: fix capitalization of ASan (Mohammed Keyvanzadeh) [#47676](https://github.com/nodejs/node/pull/47676)
* \[[`78a3503406`](https://github.com/nodejs/node/commit/78a3503406)] - **doc**: fix typos in SECURITY.md (Mohammed Keyvanzadeh) [#47677](https://github.com/nodejs/node/pull/47677)
* \[[`9101630e05`](https://github.com/nodejs/node/commit/9101630e05)] - **doc**: update error code of buffer (Deokjin Kim) [#47617](https://github.com/nodejs/node/pull/47617)
* \[[`183f0c3e79`](https://github.com/nodejs/node/commit/183f0c3e79)] - **doc**: change offset of example in `Buffer.copyBytesFrom` (Deokjin Kim) [#47606](https://github.com/nodejs/node/pull/47606)
* \[[`d11ff4bc53`](https://github.com/nodejs/node/commit/d11ff4bc53)] - **doc**: improve fs permissions description (Tobias Nießen) [#47596](https://github.com/nodejs/node/pull/47596)
* \[[`b58920c3a9`](https://github.com/nodejs/node/commit/b58920c3a9)] - **doc**: remove markdown link from heading (Tobias Nießen) [#47585](https://github.com/nodejs/node/pull/47585)
* \[[`c36634e880`](https://github.com/nodejs/node/commit/c36634e880)] - **doc**: fix history ordering of `WASI` constructor (Antoine du Hamel) [#47611](https://github.com/nodejs/node/pull/47611)
* \[[`d3fadd889d`](https://github.com/nodejs/node/commit/d3fadd889d)] - **doc**: fix release-post script location (Rafael Gonzaga) [#47517](https://github.com/nodejs/node/pull/47517)
* \[[`2a0bbe7883`](https://github.com/nodejs/node/commit/2a0bbe7883)] - **doc**: fix typo in webcrypto metadata (Tobias Nießen) [#47595](https://github.com/nodejs/node/pull/47595)
* \[[`b0b16ee9f6`](https://github.com/nodejs/node/commit/b0b16ee9f6)] - **doc**: add link for news from uvwasi team (Michael Dawson) [#47531](https://github.com/nodejs/node/pull/47531)
* \[[`7ca416af15`](https://github.com/nodejs/node/commit/7ca416af15)] - **doc**: add missing setEncoding call in ESM example (Anna Henningsen) [#47558](https://github.com/nodejs/node/pull/47558)
* \[[`f9abd59b41`](https://github.com/nodejs/node/commit/f9abd59b41)] - **doc**: update darwin-x64 toolchain used for Node.js 20 releases (Michaël Zasso) [#47546](https://github.com/nodejs/node/pull/47546)
* \[[`0dc508070f`](https://github.com/nodejs/node/commit/0dc508070f)] - **doc**: fix split infinitive in Hooks caveat (Jacob Smith) [#47550](https://github.com/nodejs/node/pull/47550)
* \[[`4046280475`](https://github.com/nodejs/node/commit/4046280475)] - **doc**: fix typo in util.types.isNativeError() (Julian Dax) [#47532](https://github.com/nodejs/node/pull/47532)
* \[[`9d30f469aa`](https://github.com/nodejs/node/commit/9d30f469aa)] - **doc**: add KhafraDev to collaborators (Matthew Aitken) [#47510](https://github.com/nodejs/node/pull/47510)
* \[[`537c17ec48`](https://github.com/nodejs/node/commit/537c17ec48)] - **doc**: create maintaining-brotli.md (Marco Ippolito) [#47380](https://github.com/nodejs/node/pull/47380)
* \[[`09ff9eafd9`](https://github.com/nodejs/node/commit/09ff9eafd9)] - **doc,fs**: update description of fs.stat() method (Mert Can Altın) [#47654](https://github.com/nodejs/node/pull/47654)
* \[[`185d6090cd`](https://github.com/nodejs/node/commit/185d6090cd)] - **doc,test**: fix concurrency option of test() (Tobias Nießen) [#47734](https://github.com/nodejs/node/pull/47734)
* \[[`a793cf401d`](https://github.com/nodejs/node/commit/a793cf401d)] - **esm**: rename `URLCanParse` to be consistent (Antoine du Hamel) [#47668](https://github.com/nodejs/node/pull/47668)
* \[[`fbb6b72f87`](https://github.com/nodejs/node/commit/fbb6b72f87)] - **esm**: remove support for deprecated hooks (Antoine du Hamel) [#47580](https://github.com/nodejs/node/pull/47580)
* \[[`c150976c4f`](https://github.com/nodejs/node/commit/c150976c4f)] - **esm**: initialize `import.meta` on eval (Antoine du Hamel) [#47551](https://github.com/nodejs/node/pull/47551)
* \[[`55f70f6395`](https://github.com/nodejs/node/commit/55f70f6395)] - **esm**: propagate `process.exit` from the loader thread to the main thread (Antoine du Hamel) [#47548](https://github.com/nodejs/node/pull/47548)
* \[[`269482f61f`](https://github.com/nodejs/node/commit/269482f61f)] - **esm**: avoid accessing lazy getters for urls (Yagiz Nizipli) [#47542](https://github.com/nodejs/node/pull/47542)
* \[[`889add68e5`](https://github.com/nodejs/node/commit/889add68e5)] - **esm**: avoid try/catch when validating urls (Yagiz Nizipli) [#47541](https://github.com/nodejs/node/pull/47541)
* \[[`439ea47a77`](https://github.com/nodejs/node/commit/439ea47a77)] - **(SEMVER-MINOR)** **fs**: add recursive option to readdir and opendir (Ethan Arrowood) [#41439](https://github.com/nodejs/node/pull/41439)
* \[[`a54e898dc8`](https://github.com/nodejs/node/commit/a54e898dc8)] - **(SEMVER-MINOR)** **fs**: add support for mode flag to specify the copy behavior (Tetsuharu Ohzeki) [#47084](https://github.com/nodejs/node/pull/47084)
* \[[`96f93cc500`](https://github.com/nodejs/node/commit/96f93cc500)] - **(SEMVER-MINOR)** **http**: remove internal error in assignSocket (Matteo Collina) [#47723](https://github.com/nodejs/node/pull/47723)
* \[[`4fa773964b`](https://github.com/nodejs/node/commit/4fa773964b)] - **(SEMVER-MINOR)** **http**: add highWaterMark opt in http.createServer (HinataKah0) [#47405](https://github.com/nodejs/node/pull/47405)
* \[[`94a5abb1e0`](https://github.com/nodejs/node/commit/94a5abb1e0)] - **inspector**: add tips for Session (theanarkh) [#47195](https://github.com/nodejs/node/pull/47195)
* \[[`21ff33127a`](https://github.com/nodejs/node/commit/21ff33127a)] - **lib**: improve esm resolve performance (Yagiz Nizipli) [#46652](https://github.com/nodejs/node/pull/46652)
* \[[`b8bdaf86c4`](https://github.com/nodejs/node/commit/b8bdaf86c4)] - **lib**: disallow file-backed blob cloning (James M Snell) [#47574](https://github.com/nodejs/node/pull/47574)
* \[[`e8bc03b372`](https://github.com/nodejs/node/commit/e8bc03b372)] - **lib**: use webidl DOMString converter in EventTarget (Matthew Aitken) [#47514](https://github.com/nodejs/node/pull/47514)
* \[[`91e4a7cdee`](https://github.com/nodejs/node/commit/91e4a7cdee)] - **loader**: use default loader as cascaded loader in the in loader worker (Joyee Cheung) [#47620](https://github.com/nodejs/node/pull/47620)
* \[[`d5089fe00a`](https://github.com/nodejs/node/commit/d5089fe00a)] - **meta**: fix dependabot commit message (Mestery) [#47810](https://github.com/nodejs/node/pull/47810)
* \[[`92794400ce`](https://github.com/nodejs/node/commit/92794400ce)] - **meta**: ping nodejs/startup for startup test changes (Joyee Cheung) [#47771](https://github.com/nodejs/node/pull/47771)
* \[[`8d43689077`](https://github.com/nodejs/node/commit/8d43689077)] - **meta**: add mailmap entry for KhafraDev (Rich Trott) [#47512](https://github.com/nodejs/node/pull/47512)
* \[[`4d02901935`](https://github.com/nodejs/node/commit/4d02901935)] - **node-api**: test passing NULL to napi\_define\_class (Gabriel Schulhof) [#47567](https://github.com/nodejs/node/pull/47567)
* \[[`568256dca0`](https://github.com/nodejs/node/commit/568256dca0)] - **node-api**: test passing NULL to number APIs (Gabriel Schulhof) [#47549](https://github.com/nodejs/node/pull/47549)
* \[[`12f0fa386d`](https://github.com/nodejs/node/commit/12f0fa386d)] - **node-api**: remove unused mark\_arraybuffer\_as\_untransferable (Chengzhong Wu) [#47557](https://github.com/nodejs/node/pull/47557)
* \[[`e8ea83416a`](https://github.com/nodejs/node/commit/e8ea83416a)] - **quic**: add more QUIC implementation (James M Snell) [#47494](https://github.com/nodejs/node/pull/47494)
* \[[`af227b159d`](https://github.com/nodejs/node/commit/af227b159d)] - **readline**: fix issue with newline-less last line (Ian Harris) [#47317](https://github.com/nodejs/node/pull/47317)
* \[[`e948bec969`](https://github.com/nodejs/node/commit/e948bec969)] - **src**: avoid copying string in fs\_permission (Yagiz Nizipli) [#47746](https://github.com/nodejs/node/pull/47746)
* \[[`dc43ce7706`](https://github.com/nodejs/node/commit/dc43ce7706)] - **src**: replace idna functions with ada::idna (Yagiz Nizipli) [#47735](https://github.com/nodejs/node/pull/47735)
* \[[`1f9e7ce7e8`](https://github.com/nodejs/node/commit/1f9e7ce7e8)] - **src**: fix typo in comment in quic/sessionticket.cc (Tobias Nießen) [#47754](https://github.com/nodejs/node/pull/47754)
* \[[`2acb57b777`](https://github.com/nodejs/node/commit/2acb57b777)] - **src**: mark fatal error functions as noreturn (Chengzhong Wu) [#47695](https://github.com/nodejs/node/pull/47695)
* \[[`4431df7481`](https://github.com/nodejs/node/commit/4431df7481)] - **src**: split BlobSerializer/BlobDeserializer (Joyee Cheung) [#47458](https://github.com/nodejs/node/pull/47458)
* \[[`bf9a52cb3d`](https://github.com/nodejs/node/commit/bf9a52cb3d)] - **src**: prevent changing FunctionTemplateInfo after publish (Shelley Vohr) [#46979](https://github.com/nodejs/node/pull/46979)
* \[[`872e6706ca`](https://github.com/nodejs/node/commit/872e6706ca)] - **src**: add v8 fast api for url canParse (Matthew Aitken) [#47552](https://github.com/nodejs/node/pull/47552)
* \[[`cfafe431f2`](https://github.com/nodejs/node/commit/cfafe431f2)] - **src**: make AliasedBuffers in the binding data weak (Joyee Cheung) [#47354](https://github.com/nodejs/node/pull/47354)
* \[[`cf48db0034`](https://github.com/nodejs/node/commit/cf48db0034)] - **src**: use v8::Boolean(b) over b ? True() : False() (Tobias Nießen) [#47554](https://github.com/nodejs/node/pull/47554)
* \[[`ba255eda37`](https://github.com/nodejs/node/commit/ba255eda37)] - **src**: fix typo in process.env accessor error message (Moritz Raho) [#47014](https://github.com/nodejs/node/pull/47014)
* \[[`daf0c78232`](https://github.com/nodejs/node/commit/daf0c78232)] - **src**: replace static const string\_view by static constexpr (Daniel Lemire) [#47524](https://github.com/nodejs/node/pull/47524)
* \[[`57e7ed7f47`](https://github.com/nodejs/node/commit/57e7ed7f47)] - **src**: fix CSPRNG when length exceeds INT\_MAX (Tobias Nießen) [#47515](https://github.com/nodejs/node/pull/47515)
* \[[`cda36bfd8f`](https://github.com/nodejs/node/commit/cda36bfd8f)] - **src**: use correct variable in node\_builtins.cc (Michaël Zasso) [#47343](https://github.com/nodejs/node/pull/47343)
* \[[`adc1601ccd`](https://github.com/nodejs/node/commit/adc1601ccd)] - **src**: slim down stream\_base-inl.h (lilsweetcaligula) [#46972](https://github.com/nodejs/node/pull/46972)
* \[[`f88132f1b8`](https://github.com/nodejs/node/commit/f88132f1b8)] - **stream**: prevent pipeline hang with generator functions (Debadree Chatterjee) [#47712](https://github.com/nodejs/node/pull/47712)
* \[[`2b411f4b42`](https://github.com/nodejs/node/commit/2b411f4b42)] - **(SEMVER-MINOR)** **stream**: preserve object mode in compose (Raz Luvaton) [#47413](https://github.com/nodejs/node/pull/47413)
* \[[`159cf02920`](https://github.com/nodejs/node/commit/159cf02920)] - **test**: refactor to use `getEventListeners` in timers (Deokjin Kim) [#47759](https://github.com/nodejs/node/pull/47759)
* \[[`97a3d39b8f`](https://github.com/nodejs/node/commit/97a3d39b8f)] - **test**: add and use tmpdir.hasEnoughSpace() (Tobias Nießen) [#47767](https://github.com/nodejs/node/pull/47767)
* \[[`5bb7b26bb5`](https://github.com/nodejs/node/commit/5bb7b26bb5)] - **test**: remove spaces from test runner test names (Tobias Nießen) [#47733](https://github.com/nodejs/node/pull/47733)
* \[[`84fa9fd725`](https://github.com/nodejs/node/commit/84fa9fd725)] - **test**: refactor WPTRunner and enable parallel WPT execution (Filip Skokan) [#47635](https://github.com/nodejs/node/pull/47635)
* \[[`9d3768eb01`](https://github.com/nodejs/node/commit/9d3768eb01)] - _**Revert**_ "**test**: run WPT files in parallel again" (Filip Skokan) [#47627](https://github.com/nodejs/node/pull/47627)
* \[[`826f4041d1`](https://github.com/nodejs/node/commit/826f4041d1)] - **test**: mark test-cluster-primary-error flaky on asan (Yagiz Nizipli) [#47422](https://github.com/nodejs/node/pull/47422)
* \[[`e5251e31eb`](https://github.com/nodejs/node/commit/e5251e31eb)] - **test\_runner**: fix --require with --experimental-loader (Moshe Atlow) [#47751](https://github.com/nodejs/node/pull/47751)
* \[[`6ee5e42c73`](https://github.com/nodejs/node/commit/6ee5e42c73)] - **(SEMVER-MINOR)** **test\_runner**: support combining coverage reports (Colin Ihrig) [#47686](https://github.com/nodejs/node/pull/47686)
* \[[`f8581e7629`](https://github.com/nodejs/node/commit/f8581e7629)] - **test\_runner**: remove no-op validation (Colin Ihrig) [#47687](https://github.com/nodejs/node/pull/47687)
* \[[`40b38797c5`](https://github.com/nodejs/node/commit/40b38797c5)] - **test\_runner**: fix test runner concurrency (Moshe Atlow) [#47675](https://github.com/nodejs/node/pull/47675)
* \[[`2d7cac0c5b`](https://github.com/nodejs/node/commit/2d7cac0c5b)] - **test\_runner**: fix test counting (Moshe Atlow) [#47675](https://github.com/nodejs/node/pull/47675)
* \[[`5a9b71a52e`](https://github.com/nodejs/node/commit/5a9b71a52e)] - **test\_runner**: fix nested hooks (Moshe Atlow) [#47648](https://github.com/nodejs/node/pull/47648)
* \[[`5327483f31`](https://github.com/nodejs/node/commit/5327483f31)] - **(SEMVER-MINOR)** **test\_runner**: add testNamePatterns to run api (Chemi Atlow) [#47628](https://github.com/nodejs/node/pull/47628)
* \[[`b6fb7914ca`](https://github.com/nodejs/node/commit/b6fb7914ca)] - **test\_runner**: support coverage of unnamed functions (Colin Ihrig) [#47652](https://github.com/nodejs/node/pull/47652)
* \[[`1f120a396f`](https://github.com/nodejs/node/commit/1f120a396f)] - **test\_runner**: move coverage collection to root.postRun() (Colin Ihrig) [#47651](https://github.com/nodejs/node/pull/47651)
* \[[`bdd02a467d`](https://github.com/nodejs/node/commit/bdd02a467d)] - **(SEMVER-MINOR)** **test\_runner**: execute before hook on test (Chemi Atlow) [#47586](https://github.com/nodejs/node/pull/47586)
* \[[`ec24abaa03`](https://github.com/nodejs/node/commit/ec24abaa03)] - **test\_runner**: avoid reporting parents of failing tests in summary (Moshe Atlow) [#47579](https://github.com/nodejs/node/pull/47579)
* \[[`4203057740`](https://github.com/nodejs/node/commit/4203057740)] - **test\_runner**: fix spec skip detection (Moshe Atlow) [#47537](https://github.com/nodejs/node/pull/47537)
* \[[`57c69987ba`](https://github.com/nodejs/node/commit/57c69987ba)] - **tls**: accept SecureContext object in server.addContext() (HinataKah0) [#47570](https://github.com/nodejs/node/pull/47570)
* \[[`c620eb80a0`](https://github.com/nodejs/node/commit/c620eb80a0)] - **tools**: update doc to highlight.js\@11.8.0 (Node.js GitHub Bot) [#47786](https://github.com/nodejs/node/pull/47786)
* \[[`326c3f1593`](https://github.com/nodejs/node/commit/326c3f1593)] - **tools**: add the missing LoongArch64 definition in the v8.gyp file (Sun Haiyong) [#47641](https://github.com/nodejs/node/pull/47641)
* \[[`8d1588acdc`](https://github.com/nodejs/node/commit/8d1588acdc)] - **tools**: update lint-md-dependencies to rollup\@3.21.1 (Node.js GitHub Bot) [#47787](https://github.com/nodejs/node/pull/47787)
* \[[`226e5b83ee`](https://github.com/nodejs/node/commit/226e5b83ee)] - **tools**: move update-npm to dep updaters (Marco Ippolito) [#47619](https://github.com/nodejs/node/pull/47619)
* \[[`9d0bef6c0a`](https://github.com/nodejs/node/commit/9d0bef6c0a)] - **tools**: fix update-v8-patch cache (Marco Ippolito) [#47725](https://github.com/nodejs/node/pull/47725)
* \[[`63e8c95a66`](https://github.com/nodejs/node/commit/63e8c95a66)] - **tools**: automate v8 patch update (Marco Ippolito) [#47594](https://github.com/nodejs/node/pull/47594)
* \[[`d2994e52d3`](https://github.com/nodejs/node/commit/d2994e52d3)] - **tools**: fix skip message in update-cjs-module-lexer (Tobias Nießen) [#47701](https://github.com/nodejs/node/pull/47701)
* \[[`ccf9c37b43`](https://github.com/nodejs/node/commit/ccf9c37b43)] - **tools**: update lint-md-dependencies to @rollup/plugin-commonjs\@24.1.0 (Node.js GitHub Bot) [#47577](https://github.com/nodejs/node/pull/47577)
* \[[`0887fa0464`](https://github.com/nodejs/node/commit/0887fa0464)] - **tools**: keep PR titles/description up-to-date (Tobias Nießen) [#47621](https://github.com/nodejs/node/pull/47621)
* \[[`b8927ddf16`](https://github.com/nodejs/node/commit/b8927ddf16)] - **tools**: fix updating root certificates (Richard Lau) [#47607](https://github.com/nodejs/node/pull/47607)
* \[[`87cae0cb59`](https://github.com/nodejs/node/commit/87cae0cb59)] - **tools**: update PR label config (Mohammed Keyvanzadeh) [#47593](https://github.com/nodejs/node/pull/47593)
* \[[`c17f2688b8`](https://github.com/nodejs/node/commit/c17f2688b8)] - _**Revert**_ "**tools**: ensure failed daily wpt run still generates a report" (Filip Skokan) [#47627](https://github.com/nodejs/node/pull/47627)
* \[[`fbe7d73234`](https://github.com/nodejs/node/commit/fbe7d73234)] - **tools**: add execution permission to uvwasi script (Mert Can Altın) [#47600](https://github.com/nodejs/node/pull/47600)
* \[[`e3f4ff439e`](https://github.com/nodejs/node/commit/e3f4ff439e)] - **tools**: add update script for googletest (Tobias Nießen) [#47482](https://github.com/nodejs/node/pull/47482)
* \[[`7c552e650a`](https://github.com/nodejs/node/commit/7c552e650a)] - **tools**: add option to run workflow with specific tool id (Michaël Zasso) [#47591](https://github.com/nodejs/node/pull/47591)
* \[[`1509312170`](https://github.com/nodejs/node/commit/1509312170)] - **tools**: automate zlib update (Marco Ippolito) [#47417](https://github.com/nodejs/node/pull/47417)
* \[[`6af7f1ee03`](https://github.com/nodejs/node/commit/6af7f1ee03)] - **tools**: add url and whatwg-url labels automatically (Yagiz Nizipli) [#47545](https://github.com/nodejs/node/pull/47545)
* \[[`ff73c05d54`](https://github.com/nodejs/node/commit/ff73c05d54)] - **tools**: add performance label to benchmark changes (Yagiz Nizipli) [#47545](https://github.com/nodejs/node/pull/47545)
* \[[`9e3e0b0a84`](https://github.com/nodejs/node/commit/9e3e0b0a84)] - **tools**: automate uvwasi dependency update (Ranieri Innocenti Spada) [#47509](https://github.com/nodejs/node/pull/47509)
* \[[`233b628f22`](https://github.com/nodejs/node/commit/233b628f22)] - **tools**: add missing pinned dependencies (Mateo Nunez) [#47346](https://github.com/nodejs/node/pull/47346)
* \[[`e4d95859f5`](https://github.com/nodejs/node/commit/e4d95859f5)] - **tools**: automate ngtcp2 and nghttp3 update (Marco Ippolito) [#47402](https://github.com/nodejs/node/pull/47402)
* \[[`2e8338126b`](https://github.com/nodejs/node/commit/2e8338126b)] - **tools**: move update-undici.sh to dep\_updaters and create maintain md (Marco Ippolito) [#47380](https://github.com/nodejs/node/pull/47380)
* \[[`8712eafc87`](https://github.com/nodejs/node/commit/8712eafc87)] - **typings**: fix syntax error in tsconfig (Mohammed Keyvanzadeh) [#47584](https://github.com/nodejs/node/pull/47584)
* \[[`e4b6b79f18`](https://github.com/nodejs/node/commit/e4b6b79f18)] - **url**: reduce revokeObjectURL cpp calls (Yagiz Nizipli) [#47728](https://github.com/nodejs/node/pull/47728)
* \[[`9aae76727f`](https://github.com/nodejs/node/commit/9aae76727f)] - **url**: handle URL.canParse without base parameter (Yagiz Nizipli) [#47547](https://github.com/nodejs/node/pull/47547)
* \[[`180d365439`](https://github.com/nodejs/node/commit/180d365439)] - **url**: validate URL constructor arg length (Matthew Aitken) [#47513](https://github.com/nodejs/node/pull/47513)
* \[[`4839fc4369`](https://github.com/nodejs/node/commit/4839fc4369)] - **url**: validate argument length in canParse (Matthew Aitken) [#47513](https://github.com/nodejs/node/pull/47513)
* \[[`606523d37e`](https://github.com/nodejs/node/commit/606523d37e)] - **v8**: fix ERR\_NOT\_BUILDING\_SNAPSHOT is not a constructor (Chengzhong Wu) [#47721](https://github.com/nodejs/node/pull/47721)
* \[[`75c1d1b66e`](https://github.com/nodejs/node/commit/75c1d1b66e)] - **(SEMVER-MINOR)** **wasi**: make returnOnExit true by default (Michael Dawson) [#47390](https://github.com/nodejs/node/pull/47390)

<a id="20.0.0"></a>

## 2023-04-18, Version 20.0.0 (Current), @RafaelGSS

We're excited to announce the release of Node.js 20! Highlights include the new Node.js Permission Model,
a synchronous `import.meta.resolve`, a stable test\_runner, updates of the V8 JavaScript engine to 11.3, Ada to 2.0,
and more!

As a reminder, Node.js 20 will enter long-term support (LTS) in October, but until then, it will be the "Current" release for the next six months.
We encourage you to explore the new features and benefits offered by this latest release and evaluate their potential impact on your applications.

### Notable Changes

#### Permission Model

Node.js now has an experimental feature called the Permission Model.
It allows developers to restrict access to specific resources during program execution, such as file system operations,
child process spawning, and worker thread creation.
The API exists behind a flag `--experimental-permission` which when enabled will restrict access to all available permissions.
By using this feature, developers can prevent their applications from accessing or modifying sensitive data or running potentially harmful code.
More information about the Permission Model can be found in the [Node.js documentation](https://nodejs.org/api/permissions.html#process-based-permissions).

The Permission Model was a contribution by Rafael Gonzaga in [#44004](https://github.com/nodejs/node/pull/44004).

#### Custom ESM loader hooks run on dedicated thread

ESM hooks supplied via loaders (`--experimental-loader=foo.mjs`) now run in a dedicated thread, isolated from the main thread.
This provides a separate scope for loaders and ensures no cross-contamination between loaders and application code.

**Synchronous `import.meta.resolve()`**

In alignment with browser behavior, this function now returns synchronously.
Despite this, user loader `resolve` hooks can still be defined as async functions (or as sync functions, if the author prefers).
Even when there are async `resolve` hooks loaded, `import.meta.resolve` will still return synchronously for application code.

Contributed by Anna Henningsen, Antoine du Hamel, Geoffrey Booth, Guy Bedford, Jacob Smith, and Michaël Zasso in [#44710](https://github.com/nodejs/node/pull/44710)

#### V8 11.3

The V8 engine is updated to version 11.3, which is part of Chromium 113.
This version includes three new features to the JavaScript API:

* [String.prototype.isWellFormed and toWellFormed](https://chromestatus.com/feature/5200195346759680)
* [Methods that change Array and TypedArray by copy](https://chromestatus.com/feature/5068609911521280)
* [Resizable ArrayBuffer and growable SharedArrayBuffer](https://chromestatus.com/feature/4668361878274048)
* [RegExp v flag with set notation + properties of strings](https://chromestatus.com/feature/5144156542861312)
* [WebAssembly Tail Call](https://chromestatus.com/feature/5423405012615168)

The V8 update was a contribution by Michaël Zasso in [#47251](https://github.com/nodejs/node/pull/47251).

#### Stable Test Runner

The recent update to Node.js, version 20, includes an important change to the test\_runner module. The module has been marked as stable after a recent update.
Previously, the test\_runner module was experimental, but this change marks it as a stable module that is ready for production use.

Contributed by Colin Ihrig in [#46983](https://github.com/nodejs/node/pull/46983)

#### Ada 2.0

Node.js v20 comes with the latest version of the URL parser, Ada. This update brings significant performance improvements
to URL parsing, including enhancements to the `url.domainToASCII` and `url.domainToUnicode` functions in `node:url`.

Ada 2.0 has been integrated into the Node.js codebase, ensuring that all parts of the application can benefit from the
improved performance. Additionally, Ada 2.0 features a significant performance boost over its predecessor, Ada 1.0.4,
while also eliminating the need for the ICU requirement for URL hostname parsing.

Contributed by Yagiz Nizipli and Daniel Lemire in [#47339](https://github.com/nodejs/node/pull/47339)

#### Preparing single executable apps now requires injecting a Blob

Building a single executable app now requires injecting a blob prepared by
Node.js from a JSON config instead of injecting the raw JS file.
This opens up the possibility of embedding multiple co-existing resources into the SEA (Single Executable Apps).

Contributed by Joyee Cheung in [#47125](https://github.com/nodejs/node/pull/47125)

#### Web Crypto API

Web Crypto API functions' arguments are now coerced and validated as per their WebIDL definitions like in other Web Crypto API implementations.
This further improves interoperability with other implementations of Web Crypto API.

This change was made by Filip Skokan in [#46067](https://github.com/nodejs/node/pull/46067).

#### Official support for ARM64 Windows

Node.js now includes binaries for ARM64 Windows, allowing for native execution on the platform.
The MSI, zip/7z packages, and executable are available from the Node.js download site along with all other platforms.
The CI system was updated and all changes are now fully tested on ARM64 Windows, to prevent regressions and ensure compatibility.

ARM64 Windows was upgraded to tier 2 support by Stefan Stojanovic in [#47233](https://github.com/nodejs/node/pull/47233).

#### WASI version must now be specified

When `new WASI()` is called, the version option is now required and has no default value.
Any code that relied on the default for the version will need to be updated to request a specific version.

This change was made by Michael Dawson in [#47391](https://github.com/nodejs/node/pull/47391).

#### Deprecations and Removals

* \[[`3bed5f11e0`](https://github.com/nodejs/node/commit/3bed5f11e0)] - **(SEMVER-MAJOR)** **url**: runtime-deprecate url.parse() with invalid ports (Rich Trott) [#45526](https://github.com/nodejs/node/pull/45526)

`url.parse()` accepts URLs with ports that are not numbers. This behavior might result in host name spoofing with unexpected input.
These URLs will throw an error in future versions of Node.js, as the WHATWG URL API does already.
Starting with Node.js 20, these URLS cause `url.parse()` to emit a warning.

### Semver-Major Commits

* \[[`9fafb0a090`](https://github.com/nodejs/node/commit/9fafb0a090)] - **(SEMVER-MAJOR)** **async\_hooks**: deprecate the AsyncResource.bind asyncResource property (James M Snell) [#46432](https://github.com/nodejs/node/pull/46432)
* \[[`1948d37595`](https://github.com/nodejs/node/commit/1948d37595)] - **(SEMVER-MAJOR)** **buffer**: check INSPECT\_MAX\_BYTES with validateNumber (Umuoy) [#46599](https://github.com/nodejs/node/pull/46599)
* \[[`7bc0e6a4e7`](https://github.com/nodejs/node/commit/7bc0e6a4e7)] - **(SEMVER-MAJOR)** **buffer**: graduate File from experimental and expose as global (Khafra) [#47153](https://github.com/nodejs/node/pull/47153)
* \[[`671ffd7825`](https://github.com/nodejs/node/commit/671ffd7825)] - **(SEMVER-MAJOR)** **buffer**: use min/max of `validateNumber` (Deokjin Kim) [#45796](https://github.com/nodejs/node/pull/45796)
* \[[`ab1614d280`](https://github.com/nodejs/node/commit/ab1614d280)] - **(SEMVER-MAJOR)** **build**: reset embedder string to "-node.0" (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`c1bcdbcf79`](https://github.com/nodejs/node/commit/c1bcdbcf79)] - **(SEMVER-MAJOR)** **build**: warn for gcc versions earlier than 10.1 (Richard Lau) [#46806](https://github.com/nodejs/node/pull/46806)
* \[[`649f68fc1e`](https://github.com/nodejs/node/commit/649f68fc1e)] - **(SEMVER-MAJOR)** **build**: reset embedder string to "-node.0" (Yagiz Nizipli) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`9374700d7a`](https://github.com/nodejs/node/commit/9374700d7a)] - **(SEMVER-MAJOR)** **crypto**: remove DEFAULT\_ENCODING (Tobias Nießen) [#47182](https://github.com/nodejs/node/pull/47182)
* \[[`1640aeb680`](https://github.com/nodejs/node/commit/1640aeb680)] - **(SEMVER-MAJOR)** **crypto**: remove obsolete SSL\_OP\_\* constants (Tobias Nießen) [#47073](https://github.com/nodejs/node/pull/47073)
* \[[`c2e4b1fa9a`](https://github.com/nodejs/node/commit/c2e4b1fa9a)] - **(SEMVER-MAJOR)** **crypto**: remove ALPN\_ENABLED (Tobias Nießen) [#47028](https://github.com/nodejs/node/pull/47028)
* \[[`3ef38c4bd7`](https://github.com/nodejs/node/commit/3ef38c4bd7)] - **(SEMVER-MAJOR)** **crypto**: use WebIDL converters in WebCryptoAPI (Filip Skokan) [#46067](https://github.com/nodejs/node/pull/46067)
* \[[`08af023b1f`](https://github.com/nodejs/node/commit/08af023b1f)] - **(SEMVER-MAJOR)** **crypto**: runtime deprecate replaced rsa-pss keygen parameters (Filip Skokan) [#45653](https://github.com/nodejs/node/pull/45653)
* \[[`7eb0ac3cb6`](https://github.com/nodejs/node/commit/7eb0ac3cb6)] - **(SEMVER-MAJOR)** **deps**: patch V8 to support compilation on win-arm64 (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`a7c129f286`](https://github.com/nodejs/node/commit/a7c129f286)] - **(SEMVER-MAJOR)** **deps**: silence irrelevant V8 warning (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`6f5655a18e`](https://github.com/nodejs/node/commit/6f5655a18e)] - **(SEMVER-MAJOR)** **deps**: always define V8\_EXPORT\_PRIVATE as no-op (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`f226350fcb`](https://github.com/nodejs/node/commit/f226350fcb)] - **(SEMVER-MAJOR)** **deps**: update V8 to 11.3.244.4 (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`d6dae7420e`](https://github.com/nodejs/node/commit/d6dae7420e)] - **(SEMVER-MAJOR)** **deps**: V8: cherry-pick f1c888e7093e (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`56c436533e`](https://github.com/nodejs/node/commit/56c436533e)] - **(SEMVER-MAJOR)** **deps**: fix V8 build on Windows with MSVC (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`51ab98c71b`](https://github.com/nodejs/node/commit/51ab98c71b)] - **(SEMVER-MAJOR)** **deps**: silence irrelevant V8 warning (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`9f84d3eea8`](https://github.com/nodejs/node/commit/9f84d3eea8)] - **(SEMVER-MAJOR)** **deps**: V8: fix v8-cppgc.h for MSVC (Jiawen Geng) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`f2318cd4b5`](https://github.com/nodejs/node/commit/f2318cd4b5)] - **(SEMVER-MAJOR)** **deps**: fix V8 build issue with inline methods (Jiawen Geng) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`16e03e7968`](https://github.com/nodejs/node/commit/16e03e7968)] - **(SEMVER-MAJOR)** **deps**: update V8 to 10.9.194.4 (Yagiz Nizipli) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`6473f5e7f7`](https://github.com/nodejs/node/commit/6473f5e7f7)] - **(SEMVER-MAJOR)** **doc**: update toolchains used for Node.js 20 releases (Richard Lau) [#47352](https://github.com/nodejs/node/pull/47352)
* \[[`cc18fd9608`](https://github.com/nodejs/node/commit/cc18fd9608)] - **(SEMVER-MAJOR)** **events**: refactor to use `validateNumber` (Deokjin Kim) [#45770](https://github.com/nodejs/node/pull/45770)
* \[[`ff92b40ffc`](https://github.com/nodejs/node/commit/ff92b40ffc)] - **(SEMVER-MAJOR)** **http**: close the connection after sending a body without declared length (Tim Perry) [#46333](https://github.com/nodejs/node/pull/46333)
* \[[`2a29df6464`](https://github.com/nodejs/node/commit/2a29df6464)] - **(SEMVER-MAJOR)** **http**: keep HTTP/1.1 conns alive even if the Connection header is removed (Tim Perry) [#46331](https://github.com/nodejs/node/pull/46331)
* \[[`391dc74a10`](https://github.com/nodejs/node/commit/391dc74a10)] - **(SEMVER-MAJOR)** **http**: throw error if options of http.Server is array (Deokjin Kim) [#46283](https://github.com/nodejs/node/pull/46283)
* \[[`ed3604cd64`](https://github.com/nodejs/node/commit/ed3604cd64)] - **(SEMVER-MAJOR)** **http**: server check Host header, to meet RFC 7230 5.4 requirement (wwwzbwcom) [#45597](https://github.com/nodejs/node/pull/45597)
* \[[`88d71dc301`](https://github.com/nodejs/node/commit/88d71dc301)] - **(SEMVER-MAJOR)** **lib**: refactor to use min/max of `validateNumber` (Deokjin Kim) [#45772](https://github.com/nodejs/node/pull/45772)
* \[[`e4d641f02a`](https://github.com/nodejs/node/commit/e4d641f02a)] - **(SEMVER-MAJOR)** **lib**: refactor to use validators in http2 (Debadree Chatterjee) [#46174](https://github.com/nodejs/node/pull/46174)
* \[[`0f3e531096`](https://github.com/nodejs/node/commit/0f3e531096)] - **(SEMVER-MAJOR)** **lib**: performance improvement on readline async iterator (Thiago Oliveira Santos) [#41276](https://github.com/nodejs/node/pull/41276)
* \[[`5b5898ac86`](https://github.com/nodejs/node/commit/5b5898ac86)] - **(SEMVER-MAJOR)** **lib,src**: update exit codes as per todos (Debadree Chatterjee) [#45841](https://github.com/nodejs/node/pull/45841)
* \[[`55321bafd1`](https://github.com/nodejs/node/commit/55321bafd1)] - **(SEMVER-MAJOR)** **net**: enable autoSelectFamily by default (Paolo Insogna) [#46790](https://github.com/nodejs/node/pull/46790)
* \[[`2d0d99733b`](https://github.com/nodejs/node/commit/2d0d99733b)] - **(SEMVER-MAJOR)** **process**: remove `process.exit()`, `process.exitCode` coercion to integer (Daeyeon Jeong) [#43716](https://github.com/nodejs/node/pull/43716)
* \[[`dc06df31b6`](https://github.com/nodejs/node/commit/dc06df31b6)] - **(SEMVER-MAJOR)** **readline**: refactor to use `validateNumber` (Deokjin Kim) [#45801](https://github.com/nodejs/node/pull/45801)
* \[[`295b2f3ff4`](https://github.com/nodejs/node/commit/295b2f3ff4)] - **(SEMVER-MAJOR)** **src**: update NODE\_MODULE\_VERSION to 115 (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`3803b028dd`](https://github.com/nodejs/node/commit/3803b028dd)] - **(SEMVER-MAJOR)** **src**: share common code paths for SEA and embedder script (Anna Henningsen) [#46825](https://github.com/nodejs/node/pull/46825)
* \[[`e8bddac3e9`](https://github.com/nodejs/node/commit/e8bddac3e9)] - **(SEMVER-MAJOR)** **src**: apply ABI-breaking API simplifications (Anna Henningsen) [#46705](https://github.com/nodejs/node/pull/46705)
* \[[`f84de0ad4c`](https://github.com/nodejs/node/commit/f84de0ad4c)] - **(SEMVER-MAJOR)** **src**: use uint32\_t for process initialization flags enum (Anna Henningsen) [#46427](https://github.com/nodejs/node/pull/46427)
* \[[`a6242772ec`](https://github.com/nodejs/node/commit/a6242772ec)] - **(SEMVER-MAJOR)** **src**: fix ArrayBuffer::Detach deprecation (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`dd5c39a808`](https://github.com/nodejs/node/commit/dd5c39a808)] - **(SEMVER-MAJOR)** **src**: update NODE\_MODULE\_VERSION to 112 (Yagiz Nizipli) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`63eca7fec0`](https://github.com/nodejs/node/commit/63eca7fec0)] - **(SEMVER-MAJOR)** **stream**: validate readable defaultEncoding (Marco Ippolito) [#46430](https://github.com/nodejs/node/pull/46430)
* \[[`9e7093f416`](https://github.com/nodejs/node/commit/9e7093f416)] - **(SEMVER-MAJOR)** **stream**: validate writable defaultEncoding (Marco Ippolito) [#46322](https://github.com/nodejs/node/pull/46322)
* \[[`fb91ee4f26`](https://github.com/nodejs/node/commit/fb91ee4f26)] - **(SEMVER-MAJOR)** **test**: make trace-gc-flag tests less strict (Yagiz Nizipli) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`eca618071e`](https://github.com/nodejs/node/commit/eca618071e)] - **(SEMVER-MAJOR)** **test**: adapt test-v8-stats for V8 update (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`c03354d3e0`](https://github.com/nodejs/node/commit/c03354d3e0)] - **(SEMVER-MAJOR)** **test**: test case for multiple res.writeHead and res.getHeader (Marco Ippolito) [#45508](https://github.com/nodejs/node/pull/45508)
* \[[`c733cc0c7f`](https://github.com/nodejs/node/commit/c733cc0c7f)] - **(SEMVER-MAJOR)** **test\_runner**: mark module as stable (Colin Ihrig) [#46983](https://github.com/nodejs/node/pull/46983)
* \[[`7ce223273d`](https://github.com/nodejs/node/commit/7ce223273d)] - **(SEMVER-MAJOR)** **tools**: update V8 gypfiles for 11.1 (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`ca4bd3023e`](https://github.com/nodejs/node/commit/ca4bd3023e)] - **(SEMVER-MAJOR)** **tools**: update V8 gypfiles for 11.0 (Michaël Zasso) [#47251](https://github.com/nodejs/node/pull/47251)
* \[[`58b06a269a`](https://github.com/nodejs/node/commit/58b06a269a)] - **(SEMVER-MAJOR)** **tools**: update V8 gypfiles (Michaël Zasso) [#45579](https://github.com/nodejs/node/pull/45579)
* \[[`027841c964`](https://github.com/nodejs/node/commit/027841c964)] - **(SEMVER-MAJOR)** **url**: use private properties for brand check (Yagiz Nizipli) [#46904](https://github.com/nodejs/node/pull/46904)
* \[[`3bed5f11e0`](https://github.com/nodejs/node/commit/3bed5f11e0)] - **(SEMVER-MAJOR)** **url**: runtime-deprecate url.parse() with invalid ports (Rich Trott) [#45526](https://github.com/nodejs/node/pull/45526)
* \[[`7c76fddf25`](https://github.com/nodejs/node/commit/7c76fddf25)] - **(SEMVER-MAJOR)** **util,doc**: mark parseArgs() as stable (Colin Ihrig) [#46718](https://github.com/nodejs/node/pull/46718)
* \[[`4b52727976`](https://github.com/nodejs/node/commit/4b52727976)] - **(SEMVER-MAJOR)** **wasi**: make version non-optional (Michael Dawson) [#47391](https://github.com/nodejs/node/pull/47391)

### Semver-Minor Commits

* \[[`d4b440bfac`](https://github.com/nodejs/node/commit/d4b440bfac)] - **(SEMVER-MINOR)** **fs**: implement byob mode for readableWebStream() (Debadree Chatterjee) [#46933](https://github.com/nodejs/node/pull/46933)
* \[[`00c222593e`](https://github.com/nodejs/node/commit/00c222593e)] - **(SEMVER-MINOR)** **src,process**: add permission model (Rafael Gonzaga) [#44004](https://github.com/nodejs/node/pull/44004)
* \[[`978b57d750`](https://github.com/nodejs/node/commit/978b57d750)] - **(SEMVER-MINOR)** **wasi**: no longer require flag to enable wasi (Michael Dawson) [#47286](https://github.com/nodejs/node/pull/47286)

### Semver-Patch Commits

* \[[`e50c6b9a22`](https://github.com/nodejs/node/commit/e50c6b9a22)] - **bootstrap**: do not expand process.argv\[1] for snapshot entry points (Joyee Cheung) [#47466](https://github.com/nodejs/node/pull/47466)
* \[[`c81e1143e4`](https://github.com/nodejs/node/commit/c81e1143e4)] - **bootstrap**: store internal loaders in C++ via a binding (Joyee Cheung) [#47215](https://github.com/nodejs/node/pull/47215)
* \[[`8e673bdb84`](https://github.com/nodejs/node/commit/8e673bdb84)] - **build**: add node-core-utils to setup (Jiawen Geng) [#47442](https://github.com/nodejs/node/pull/47442)
* \[[`5b561d72a6`](https://github.com/nodejs/node/commit/5b561d72a6)] - **build**: sync cares source change (Jiawen Geng) [#47359](https://github.com/nodejs/node/pull/47359)
* \[[`8e6ee53e4e`](https://github.com/nodejs/node/commit/8e6ee53e4e)] - **build**: remove non-exist build file (Jiawen Geng) [#47361](https://github.com/nodejs/node/pull/47361)
* \[[`9a4d21d1d9`](https://github.com/nodejs/node/commit/9a4d21d1d9)] - **build, deps, tools**: avoid excessive LTO (Konstantin Demin) [#47313](https://github.com/nodejs/node/pull/47313)
* \[[`48c01485cd`](https://github.com/nodejs/node/commit/48c01485cd)] - **crypto**: replace THROW with CHECK for scrypt keylen (Tobias Nießen) [#47407](https://github.com/nodejs/node/pull/47407)
* \[[`4c1a27716b`](https://github.com/nodejs/node/commit/4c1a27716b)] - **crypto**: re-add padding for AES-KW wrapped JWKs (Filip Skokan) [#46563](https://github.com/nodejs/node/pull/46563)
* \[[`b66eb15d12`](https://github.com/nodejs/node/commit/b66eb15d12)] - **deps**: update simdutf to 3.2.7 (Node.js GitHub Bot) [#47473](https://github.com/nodejs/node/pull/47473)
* \[[`3fc11477ba`](https://github.com/nodejs/node/commit/3fc11477ba)] - **deps**: update corepack to 0.17.2 (Node.js GitHub Bot) [#47474](https://github.com/nodejs/node/pull/47474)
* \[[`c1776531ab`](https://github.com/nodejs/node/commit/c1776531ab)] - **deps**: upgrade npm to 9.6.4 (npm team) [#47432](https://github.com/nodejs/node/pull/47432)
* \[[`e7ca09f310`](https://github.com/nodejs/node/commit/e7ca09f310)] - **deps**: update zlib to upstream 5edb52d4 (Luigi Pinca) [#47151](https://github.com/nodejs/node/pull/47151)
* \[[`88387ccd12`](https://github.com/nodejs/node/commit/88387ccd12)] - **deps**: update ada to 2.0.0 (Node.js GitHub Bot) [#47339](https://github.com/nodejs/node/pull/47339)
* \[[`9f468cc37e`](https://github.com/nodejs/node/commit/9f468cc37e)] - **deps**: cherry-pick Windows ARM64 fix for openssl (Richard Lau) [#46570](https://github.com/nodejs/node/pull/46570)
* \[[`eeab210b1b`](https://github.com/nodejs/node/commit/eeab210b1b)] - **deps**: update archs files for quictls/openssl-3.0.8+quic (RafaelGSS) [#46570](https://github.com/nodejs/node/pull/46570)
* \[[`d93d7716c7`](https://github.com/nodejs/node/commit/d93d7716c7)] - **deps**: upgrade openssl sources to quictls/openssl-3.0.8+quic (RafaelGSS) [#46571](https://github.com/nodejs/node/pull/46571)
* \[[`0f69ec4dd7`](https://github.com/nodejs/node/commit/0f69ec4dd7)] - **deps**: patch V8 to 10.9.194.9 (Michaël Zasso) [#45995](https://github.com/nodejs/node/pull/45995)
* \[[`5890d09644`](https://github.com/nodejs/node/commit/5890d09644)] - **deps**: patch V8 to 10.9.194.6 (Michaël Zasso) [#45748](https://github.com/nodejs/node/pull/45748)
* \[[`c02a7e7e93`](https://github.com/nodejs/node/commit/c02a7e7e93)] - **diagnostics\_channel**: fix ref counting bug when reaching zero subscribers (Stephen Belanger) [#47520](https://github.com/nodejs/node/pull/47520)
* \[[`c7ad5bb37d`](https://github.com/nodejs/node/commit/c7ad5bb37d)] - **doc**: info on handling unintended breaking changes (Michael Dawson) [#47426](https://github.com/nodejs/node/pull/47426)
* \[[`7d2d40ed0d`](https://github.com/nodejs/node/commit/7d2d40ed0d)] - **doc**: add performance initiative (Yagiz Nizipli) [#47424](https://github.com/nodejs/node/pull/47424)
* \[[`d56c0f7318`](https://github.com/nodejs/node/commit/d56c0f7318)] - **doc**: do not create a backup file (Luigi Pinca) [#47151](https://github.com/nodejs/node/pull/47151)
* \[[`412d27b65b`](https://github.com/nodejs/node/commit/412d27b65b)] - **doc**: add MoLow to the TSC (Colin Ihrig) [#47436](https://github.com/nodejs/node/pull/47436)
* \[[`f131cca0c0`](https://github.com/nodejs/node/commit/f131cca0c0)] - **doc**: reserve 116 for Electron 25 (Keeley Hammond) [#47375](https://github.com/nodejs/node/pull/47375)
* \[[`1022c6f424`](https://github.com/nodejs/node/commit/1022c6f424)] - **doc**: add experimental stages (Geoffrey Booth) [#46100](https://github.com/nodejs/node/pull/46100)
* \[[`42d3d74717`](https://github.com/nodejs/node/commit/42d3d74717)] - **doc**: clarify release notes for Node.js 16.19.0 (Richard Lau) [#45846](https://github.com/nodejs/node/pull/45846)
* \[[`533c6512da`](https://github.com/nodejs/node/commit/533c6512da)] - **doc**: clarify release notes for Node.js 14.21.2 (Richard Lau) [#45846](https://github.com/nodejs/node/pull/45846)
* \[[`97165fc1a6`](https://github.com/nodejs/node/commit/97165fc1a6)] - **doc**: fix doc metadata for Node.js 16.19.0 (Richard Lau) [#45863](https://github.com/nodejs/node/pull/45863)
* \[[`a266b8b702`](https://github.com/nodejs/node/commit/a266b8b702)] - **doc**: add registry number for Electron 23 & 24 (Keeley Hammond) [#45661](https://github.com/nodejs/node/pull/45661)
* \[[`2613a9ced9`](https://github.com/nodejs/node/commit/2613a9ced9)] - **esm**: move hook execution to separate thread (Jacob Smith) [#44710](https://github.com/nodejs/node/pull/44710)
* \[[`841f6b3abf`](https://github.com/nodejs/node/commit/841f6b3abf)] - **esm**: increase test coverage of edge cases (Antoine du Hamel) [#47033](https://github.com/nodejs/node/pull/47033)
* \[[`0d575fe61a`](https://github.com/nodejs/node/commit/0d575fe61a)] - **gyp**: put filenames in variables (Cheng Zhao) [#46965](https://github.com/nodejs/node/pull/46965)
* \[[`41b186722c`](https://github.com/nodejs/node/commit/41b186722c)] - **lib**: distinguish webidl interfaces with the extended property "Exposed" (Chengzhong Wu) [#46809](https://github.com/nodejs/node/pull/46809)
* \[[`9b7db62276`](https://github.com/nodejs/node/commit/9b7db62276)] - **lib**: makeRequireFunction patch when experimental policy (RafaelGSS) [nodejs-private/node-private#358](https://github.com/nodejs-private/node-private/pull/358)
* \[[`d43b532789`](https://github.com/nodejs/node/commit/d43b532789)] - **lib**: refactor to use `validateBuffer` (Deokjin Kim) [#46489](https://github.com/nodejs/node/pull/46489)
* \[[`9a76a2521b`](https://github.com/nodejs/node/commit/9a76a2521b)] - **meta**: ping security-wg team on permission model changes (Rafael Gonzaga) [#47483](https://github.com/nodejs/node/pull/47483)
* \[[`a4dadde1ba`](https://github.com/nodejs/node/commit/a4dadde1ba)] - **meta**: ping startup and realm team on src/node\_realm\* changes (Joyee Cheung) [#47448](https://github.com/nodejs/node/pull/47448)
* \[[`631c3ef3de`](https://github.com/nodejs/node/commit/631c3ef3de)] - **module**: do less CJS module loader initialization at run time (Joyee Cheung) [#47194](https://github.com/nodejs/node/pull/47194)
* \[[`8bcf0a42f7`](https://github.com/nodejs/node/commit/8bcf0a42f7)] - **permission**: fix chmod,chown improve fs coverage (Rafael Gonzaga) [#47529](https://github.com/nodejs/node/pull/47529)
* \[[`54d17ff4b5`](https://github.com/nodejs/node/commit/54d17ff4b5)] - **permission**: support fs.mkdtemp (Rafael Gonzaga) [#47470](https://github.com/nodejs/node/pull/47470)
* \[[`b441b5dc65`](https://github.com/nodejs/node/commit/b441b5dc65)] - **permission**: drop process.permission.deny (Rafael Gonzaga) [#47335](https://github.com/nodejs/node/pull/47335)
* \[[`aa30e16716`](https://github.com/nodejs/node/commit/aa30e16716)] - **permission**: fix some vulnerabilities in fs (Tobias Nießen) [#47091](https://github.com/nodejs/node/pull/47091)
* \[[`1726da9300`](https://github.com/nodejs/node/commit/1726da9300)] - **permission**: add path separator to loader check (Rafael Gonzaga) [#47030](https://github.com/nodejs/node/pull/47030)
* \[[`b164038c86`](https://github.com/nodejs/node/commit/b164038c86)] - **permission**: fix spawnSync permission check (RafaelGSS) [#46975](https://github.com/nodejs/node/pull/46975)
* \[[`af91400886`](https://github.com/nodejs/node/commit/af91400886)] - **policy**: makeRequireFunction on mainModule.require (RafaelGSS) [nodejs-private/node-private#358](https://github.com/nodejs-private/node-private/pull/358)
* \[[`f8b4e26aee`](https://github.com/nodejs/node/commit/f8b4e26aee)] - **quic**: add more QUIC impl (James M Snell) [#47348](https://github.com/nodejs/node/pull/47348)
* \[[`d65ae9f678`](https://github.com/nodejs/node/commit/d65ae9f678)] - **quic**: add additional quic implementation utilities (James M Snell) [#47289](https://github.com/nodejs/node/pull/47289)
* \[[`9b104be502`](https://github.com/nodejs/node/commit/9b104be502)] - **quic**: do not dereference shared\_ptr after move (Tobias Nießen) [#47294](https://github.com/nodejs/node/pull/47294)
* \[[`09a4bb152f`](https://github.com/nodejs/node/commit/09a4bb152f)] - **quic**: add multiple internal utilities (James M Snell) [#47263](https://github.com/nodejs/node/pull/47263)
* \[[`2bde0059ca`](https://github.com/nodejs/node/commit/2bde0059ca)] - **sea**: use JSON configuration and blob content for SEA (Joyee Cheung) [#47125](https://github.com/nodejs/node/pull/47125)
* \[[`78c7475493`](https://github.com/nodejs/node/commit/78c7475493)] - **src**: allow simdutf::convert\_\* functions to return zero (Daniel Lemire) [#47471](https://github.com/nodejs/node/pull/47471)
* \[[`5250947a53`](https://github.com/nodejs/node/commit/5250947a53)] - **src**: track ShadowRealm native objects correctly in the heap snapshot (Joyee Cheung) [#47389](https://github.com/nodejs/node/pull/47389)
* \[[`8059764621`](https://github.com/nodejs/node/commit/8059764621)] - **src**: use the internal field to determine if an object is a BaseObject (Joyee Cheung) [#47217](https://github.com/nodejs/node/pull/47217)
* \[[`698508afa8`](https://github.com/nodejs/node/commit/698508afa8)] - **src**: bootstrap prepare stack trace callback in shadow realm (Chengzhong Wu) [#47107](https://github.com/nodejs/node/pull/47107)
* \[[`e6b4d30a2f`](https://github.com/nodejs/node/commit/e6b4d30a2f)] - **src**: bootstrap Web \[Exposed=\*] APIs in the shadow realm (Chengzhong Wu) [#46809](https://github.com/nodejs/node/pull/46809)
* \[[`3646a66044`](https://github.com/nodejs/node/commit/3646a66044)] - **src**: fix AliasedBuffer memory attribution in heap snapshots (Joyee Cheung) [#46817](https://github.com/nodejs/node/pull/46817)
* \[[`8b2126f63f`](https://github.com/nodejs/node/commit/8b2126f63f)] - **src**: move AliasedBuffer implementation to -inl.h (Joyee Cheung) [#46817](https://github.com/nodejs/node/pull/46817)
* \[[`3abbc3829a`](https://github.com/nodejs/node/commit/3abbc3829a)] - **src**: fix useless call in permission.cc (Tobias Nießen) [#46833](https://github.com/nodejs/node/pull/46833)
* \[[`7b1e153530`](https://github.com/nodejs/node/commit/7b1e153530)] - **src**: simplify exit code accesses (Daeyeon Jeong) [#45125](https://github.com/nodejs/node/pull/45125)
* \[[`7359b92a41`](https://github.com/nodejs/node/commit/7359b92a41)] - **test**: remove unnecessary status check on test-release-npm (RafaelGSS) [#47516](https://github.com/nodejs/node/pull/47516)
* \[[`a5a5d2fb7e`](https://github.com/nodejs/node/commit/a5a5d2fb7e)] - **test**: mark test/parallel/test-file-write-stream4 as flaky (Yagiz Nizipli) [#47423](https://github.com/nodejs/node/pull/47423)
* \[[`81ad73a205`](https://github.com/nodejs/node/commit/81ad73a205)] - **test**: remove unused callback variables (angellovc) [#47167](https://github.com/nodejs/node/pull/47167)
* \[[`757a586ead`](https://github.com/nodejs/node/commit/757a586ead)] - **test**: migrate test runner message tests to snapshot (Moshe Atlow) [#47392](https://github.com/nodejs/node/pull/47392)
* \[[`86f890539f`](https://github.com/nodejs/node/commit/86f890539f)] - **test**: remove stale entry from known\_issues.status (Richard Lau) [#47454](https://github.com/nodejs/node/pull/47454)
* \[[`1f3773d0c1`](https://github.com/nodejs/node/commit/1f3773d0c1)] - **test**: move more inspector sequential tests to parallel (Joyee Cheung) [#47412](https://github.com/nodejs/node/pull/47412)
* \[[`617b8d44c6`](https://github.com/nodejs/node/commit/617b8d44c6)] - **test**: use random port in test-inspector-enabled (Joyee Cheung) [#47412](https://github.com/nodejs/node/pull/47412)
* \[[`ade0170c4f`](https://github.com/nodejs/node/commit/ade0170c4f)] - **test**: use random port in test-inspector-debug-brk-flag (Joyee Cheung) [#47412](https://github.com/nodejs/node/pull/47412)
* \[[`1a78632cd3`](https://github.com/nodejs/node/commit/1a78632cd3)] - **test**: use random port in NodeInstance.startViaSignal() (Joyee Cheung) [#47412](https://github.com/nodejs/node/pull/47412)
* \[[`23f66b137e`](https://github.com/nodejs/node/commit/23f66b137e)] - **test**: move test-shadow-realm-gc.js to known\_issues (Joyee Cheung) [#47355](https://github.com/nodejs/node/pull/47355)
* \[[`9dfd0394c5`](https://github.com/nodejs/node/commit/9dfd0394c5)] - **test**: remove useless WPT init scripts (Khafra) [#47221](https://github.com/nodejs/node/pull/47221)
* \[[`1cfe058778`](https://github.com/nodejs/node/commit/1cfe058778)] - **test**: fix test-permission-deny-fs-wildcard (win32) (Tobias Nießen) [#47095](https://github.com/nodejs/node/pull/47095)
* \[[`b8ef1b476e`](https://github.com/nodejs/node/commit/b8ef1b476e)] - **test**: add coverage for custom loader hooks with permission model (Antoine du Hamel) [#46977](https://github.com/nodejs/node/pull/46977)
* \[[`4a7c3e9c50`](https://github.com/nodejs/node/commit/4a7c3e9c50)] - **test**: fix file path in permission symlink test (Livia Medeiros) [#46859](https://github.com/nodejs/node/pull/46859)
* \[[`10005de6a8`](https://github.com/nodejs/node/commit/10005de6a8)] - **tools**: make `js2c.py` usable for other build systems (Cheng Zhao) [#46930](https://github.com/nodejs/node/pull/46930)
* \[[`1e2f9aca72`](https://github.com/nodejs/node/commit/1e2f9aca72)] - **tools**: move update-acorn.sh to dep\_updaters and create maintaining md (Marco Ippolito) [#47382](https://github.com/nodejs/node/pull/47382)
* \[[`174662a463`](https://github.com/nodejs/node/commit/174662a463)] - **tools**: update eslint to 8.38.0 (Node.js GitHub Bot) [#47475](https://github.com/nodejs/node/pull/47475)
* \[[`a58ca61f35`](https://github.com/nodejs/node/commit/a58ca61f35)] - **tools**: update eslint to 8.38.0 (Node.js GitHub Bot) [#47475](https://github.com/nodejs/node/pull/47475)
* \[[`37d12730ab`](https://github.com/nodejs/node/commit/37d12730ab)] - **tools**: automate cjs-module-lexer dependency update (Marco Ippolito) [#47446](https://github.com/nodejs/node/pull/47446)
* \[[`4fbfa3c9f2`](https://github.com/nodejs/node/commit/4fbfa3c9f2)] - **tools**: fix notify-on-push Slack messages (Antoine du Hamel) [#47453](https://github.com/nodejs/node/pull/47453)
* \[[`b1f2ff1242`](https://github.com/nodejs/node/commit/b1f2ff1242)] - **tools**: update lint-md-dependencies to @rollup/plugin-node-resolve\@15.0.2 (Node.js GitHub Bot) [#47431](https://github.com/nodejs/node/pull/47431)
* \[[`26b2584b84`](https://github.com/nodejs/node/commit/26b2584b84)] - **tools**: add root certificate update script (Richard Lau) [#47425](https://github.com/nodejs/node/pull/47425)
* \[[`553b052648`](https://github.com/nodejs/node/commit/553b052648)] - **tools**: remove targets for individual test suites in `Makefile` (Antoine du Hamel) [#46892](https://github.com/nodejs/node/pull/46892)
* \[[`747ff43e5b`](https://github.com/nodejs/node/commit/747ff43e5b)] - **url**: more sophisticated brand check for URLSearchParams (Timothy Gu) [#47414](https://github.com/nodejs/node/pull/47414)
* \[[`e727eb066f`](https://github.com/nodejs/node/commit/e727eb066f)] - **url**: do not use object as hashmap (Timothy Gu) [#47415](https://github.com/nodejs/node/pull/47415)
* \[[`81c7875eb7`](https://github.com/nodejs/node/commit/81c7875eb7)] - **url**: drop ICU requirement for parsing hostnames (Yagiz Nizipli) [#47339](https://github.com/nodejs/node/pull/47339)
* \[[`a4895df94a`](https://github.com/nodejs/node/commit/a4895df94a)] - **url**: use ada::url\_aggregator for parsing urls (Yagiz Nizipli) [#47339](https://github.com/nodejs/node/pull/47339)
