# Node.js 14 ChangeLog

<!--lint disable prohibited-strings-->
<!--lint disable maximum-line-length-->
<!--lint disable no-literal-urls-->

<table>
<tr>
<th>LTS 'Fermium'</th>
<th>Current</th>
</tr>
<tr>
<td valign="top">
<a href="#14.16.1">14.16.1</a><br/>
<a href="#14.16.0">14.16.0</a><br/>
<a href="#14.15.5">14.15.5</a><br/>
<a href="#14.15.4">14.15.4</a><br/>
<a href="#14.15.3">14.15.3</a><br/>
<a href="#14.15.2">14.15.2</a><br/>
<a href="#14.15.1">14.15.1</a><br/>
<a href="#14.15.0">14.15.0</a><br/>
</td>
<td valign="top">
<a href="#14.14.0">14.14.0</a><br/>
<a href="#14.13.1">14.13.1</a><br/>
<a href="#14.13.0">14.13.0</a><br/>
<a href="#14.12.0">14.12.0</a><br/>
<a href="#14.11.0">14.11.0</a><br/>
<a href="#14.10.1">14.10.1</a><br/>
<a href="#14.10.0">14.10.0</a><br/>
<a href="#14.9.0">14.9.0</a><br/>
<a href="#14.8.0">14.8.0</a><br/>
<a href="#14.7.0">14.7.0</a><br/>
<a href="#14.6.0">14.6.0</a><br/>
<a href="#14.5.0">14.5.0</a><br/>
<a href="#14.4.0">14.4.0</a><br/>
<a href="#14.3.0">14.3.0</a><br/>
<a href="#14.2.0">14.2.0</a><br/>
<a href="#14.1.0">14.1.0</a><br/>
<a href="#14.0.0">14.0.0</a><br/>
</td>
</tr>
</table>

* Other Versions
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

<a id="14.16.1"></a>
## 2021-04-06, Version 14.16.1 'Fermium' (LTS), @mylesborins

This is a security release.

### Notable Changes

Vulnerabilities fixed:

* **CVE-2021-3450**: OpenSSL - CA certificate check bypass with X509_V_FLAG_X509_STRICT (High)
  * This is a vulnerability in OpenSSL which may be exploited through Node.js. You can read more about it in https://www.openssl.org/news/secadv/20210325.txt
  * Impacts:
    * All versions of the 15.x, 14.x, 12.x and 10.x releases lines
* **CVE-2021-3449**: OpenSSL - NULL pointer deref in signature_algorithms processing (High)
  * This is a vulnerability in OpenSSL which may be exploited through Node.js. You can read more about it in https://www.openssl.org/news/secadv/20210325.txt
  * Impacts:
    * All versions of the 15.x, 14.x, 12.x and 10.x releases lines
* **CVE-2020-7774**: npm upgrade - Update y18n to fix Prototype-Pollution (High)
  * This is a vulnerability in the y18n npm module which may be exploited by prototype pollution. You can read more about it in https://github.com/advisories/GHSA-c4w7-xm78-47vh
  * Impacts:
    * All versions of the 14.x, 12.x and 10.x releases lines

### Commits

* [[`467be7a950`](https://github.com/nodejs/node/commit/467be7a950)] - **deps**: upgrade npm to 6.14.12 (Ruy Adorno) [#37918](https://github.com/nodejs/node/pull/37918)
* [[`6bc8f58182`](https://github.com/nodejs/node/commit/6bc8f58182)] - **deps**: update archs files for OpenSSL-1.1.1k (Tobias Nießen) [#37938](https://github.com/nodejs/node/pull/37938)
* [[`403a014ef6`](https://github.com/nodejs/node/commit/403a014ef6)] - **deps**: upgrade openssl sources to 1.1.1k (Tobias Nießen) [#37938](https://github.com/nodejs/node/pull/37938)

<a id="14.16.0"></a>
## 2021-02-23, Version 14.16.0 'Fermium' (LTS), @BethGriggs

This is a security release.

### Notable changes

Vulnerabilities fixed:

* **CVE-2021-22883**: HTTP2 'unknownProtocol' cause Denial of Service by resource exhaustion
  * Affected Node.js versions are vulnerable to denial of service attacks when too many connection attempts with an 'unknownProtocol' are established. This leads to a leak of file descriptors. If a file descriptor limit is configured on the system, then the server is unable to accept new connections and prevent the process also from opening, e.g. a file. If no file descriptor limit is configured, then this lead to an excessive memory usage and cause the system to run out of memory.
* **CVE-2021-22884**: DNS rebinding in --inspect
  * Affected Node.js versions are vulnerable to denial of service attacks when the whitelist includes “localhost6”. When “localhost6” is not present in /etc/hosts, it is just an ordinary domain that is resolved via DNS, i.e., over network. If the attacker controls the victim's DNS server or can spoof its responses, the DNS rebinding protection can be bypassed by using the “localhost6” domain. As long as the attacker uses the “localhost6” domain, they can still apply the attack described in CVE-2018-7160.
* **CVE-2021-23840**: OpenSSL - Integer overflow in CipherUpdate
  * This is a vulnerability in OpenSSL which may be exploited through Node.js. You can read more about it in https://www.openssl.org/news/secadv/20210216.txt

### Commits

* [[`313d26800c`](https://github.com/nodejs/node/commit/313d26800c)] - **deps**: update archs files for OpenSSL-1.1.1j (Daniel Bevenius) [#37412](https://github.com/nodejs/node/pull/37412)
* [[`6098012b48`](https://github.com/nodejs/node/commit/6098012b48)] - **deps**: upgrade openssl sources to 1.1.1j (Daniel Bevenius) [#37412](https://github.com/nodejs/node/pull/37412)
* [[`afea10b097`](https://github.com/nodejs/node/commit/afea10b097)] - **(SEMVER-MINOR)** **http2**: add unknownProtocol timeout (Daniel Bevenius) [nodejs-private/node-private#246](https://github.com/nodejs-private/node-private/pull/246)
* [[`1ca3f5abcb`](https://github.com/nodejs/node/commit/1ca3f5abcb)] - **src**: drop localhost6 as allowed host for inspector (Matteo Collina) [nodejs-private/node-private#244](https://github.com/nodejs-private/node-private/pull/244)

<a id="14.15.5"></a>
## 2021-02-09, Version 14.15.5 'Fermium' (LTS), @BethGriggs

### Notable Changes

* **deps**:
  * upgrade npm to 6.14.11 (Ruy Adorno) [#37173](https://github.com/nodejs/node/pull/37173)
  * V8: backport dfcf1e86fac0 (Michaël Zasso) [#37245](https://github.com/nodejs/node/pull/37245)
    * **Note**: Node.js is not believed to be vulnerable to CVE-2021-21148.
* **stream,zlib**: do not use \_stream\_\* anymore (Matteo Collina) [#36618](https://github.com/nodejs/node/pull/36618)

### Commits

* [[`20b1e6c802`](https://github.com/nodejs/node/commit/20b1e6c802)] - **deps**: V8: backport dfcf1e86fac0 (Michaël Zasso) [#37245](https://github.com/nodejs/node/pull/37245)
* [[`408c7a65f3`](https://github.com/nodejs/node/commit/408c7a65f3)] - **deps**: upgrade npm to 6.14.11 (Ruy Adorno) [#37173](https://github.com/nodejs/node/pull/37173)
* [[`017eed665b`](https://github.com/nodejs/node/commit/017eed665b)] - **http**: do not loop over prototype in Agent (Michaël Zasso) [#36410](https://github.com/nodejs/node/pull/36410)
* [[`25a3204fe2`](https://github.com/nodejs/node/commit/25a3204fe2)] - **http**: don't cork .end when not needed (Dimitris Halatsis) [#36633](https://github.com/nodejs/node/pull/36633)
* [[`2a1e4e9244`](https://github.com/nodejs/node/commit/2a1e4e9244)] - **stream**: accept iterable as a valid first argument (ZiJian Liu) [#36479](https://github.com/nodejs/node/pull/36479)
* [[`9ff73fcdbe`](https://github.com/nodejs/node/commit/9ff73fcdbe)] - **stream,zlib**: do not use \_stream\_\* anymore (Matteo Collina) [#36618](https://github.com/nodejs/node/pull/36618)
* [[`c03cddb46f`](https://github.com/nodejs/node/commit/c03cddb46f)] - **test**: http complete response after socket double end (Dimitris Halatsis) [#36633](https://github.com/nodejs/node/pull/36633)
* [[`f206505e9d`](https://github.com/nodejs/node/commit/f206505e9d)] - **util**: fix instanceof checks with null prototypes during inspection (Ruben Bridgewater) [#36178](https://github.com/nodejs/node/pull/36178)
* [[`2f7944b18b`](https://github.com/nodejs/node/commit/2f7944b18b)] - **util**: fix module prefixes during inspection (Ruben Bridgewater) [#36178](https://github.com/nodejs/node/pull/36178)

<a id="14.15.4"></a>
## 2021-01-04, Version 14.15.4 'Fermium' (LTS), @BethGriggs

This is a security release.

### Notable Changes

Vulnerabilities fixed:

* **CVE-2020-1971**: OpenSSL - EDIPARTYNAME NULL pointer de-reference (High)
  * This is a vulnerability in OpenSSL which may be exploited through
  Node.js. You can read more about it in
  https://www.openssl.org/news/secadv/20201208.txt

* **CVE-2020-8265**: use-after-free in TLSWrap (High)
  * Affected Node.js versions are vulnerable to a use-after-free bug in
  its TLS implementation. When writing to a TLS enabled socket,
  node::StreamBase::Write calls node::TLSWrap::DoWrite with a freshly
  allocated WriteWrap object as first argument. If the DoWrite method
  does not return an error, this object is passed back to the caller as
  part of a StreamWriteResult structure. This may be exploited to
  corrupt memory leading to a Denial of Service or potentially other
  exploits.

* **CVE-2020-8287**: HTTP Request Smuggling in nodejs (Low)
  * Affected versions of Node.js allow two copies of a header field in
  a http request. For example, two Transfer-Encoding header fields. In
  this case Node.js identifies the first header field and ignores the
  second. This can lead to HTTP Request Smuggling
  (https://cwe.mitre.org/data/definitions/444.html).

### Commits

* [[`305c0f4977`](https://github.com/nodejs/node/commit/305c0f4977)] - **deps**: upgrade npm to 6.14.10 (Ruy Adorno) [#36571](https://github.com/nodejs/node/pull/36571)
* [[`d62c650f75`](https://github.com/nodejs/node/commit/d62c650f75)] - **deps**: update archs files for OpenSSL-1.1.1i (Myles Borins) [#36521](https://github.com/nodejs/node/pull/36521)
* [[`2de2672eb5`](https://github.com/nodejs/node/commit/2de2672eb5)] - **deps**: upgrade openssl sources to 1.1.1i (Myles Borins) [#36521](https://github.com/nodejs/node/pull/36521)
* [[`7ecac8143f`](https://github.com/nodejs/node/commit/7ecac8143f)] - **http**: add test for http transfer encoding smuggling (Matteo Collina) [nodejs-private/node-private#228](https://github.com/nodejs-private/node-private/pull/228)
* [[`641f786bb1`](https://github.com/nodejs/node/commit/641f786bb1)] - **http**: unset `F_CHUNKED` on new `Transfer-Encoding` (Matteo Collina) [nodejs-private/node-private#228](https://github.com/nodejs-private/node-private/pull/228)
* [[`4f8772f9b7`](https://github.com/nodejs/node/commit/4f8772f9b7)] - **src**: retain pointers to WriteWrap/ShutdownWrap (James M Snell) [nodejs-private/node-private#23](https://github.com/nodejs-private/node-private/pull/23)

<a id="14.15.3"></a>
## 2020-12-17, Version 14.15.3 'Fermium' (LTS), @BethGriggs

### Notable Changes

Node.js v14.15.2 included a commit that has caused reported breakages when cloning request objects. This release reverts the commit that introduced the behaviour change. See https://github.com/nodejs/node/issues/36550 for more details.

### Commits

* [[`4264d9aa67`](https://github.com/nodejs/node/commit/4264d9aa67)] - ***Revert*** "**http**: lazy create IncomingMessage.headers" (Beth Griggs) [#36553](https://github.com/nodejs/node/pull/36553)

<a id="14.15.2"></a>
## 2020-12-15, Version 14.15.2 'Fermium' (LTS), @BethGriggs

### Notable Changes

* **deps**:
  * upgrade npm to 6.14.9 (Myles Borins) [#36450](https://github.com/nodejs/node/pull/36450)
  * update acorn to v8.0.4 (Michaël Zasso) [#35791](https://github.com/nodejs/node/pull/35791)
* **doc**: add release key for Danielle Adams (Danielle Adams) [#35545](https://github.com/nodejs/node/pull/35545)
* **http2**: check write not scheduled in scope destructor (David Halls) [#36241](https://github.com/nodejs/node/pull/36241)
* **stream**: fix regression on duplex end (Momtchil Momtchev) [#35941](https://github.com/nodejs/node/pull/35941)

### Commits

* [[`c508bfc66b`](https://github.com/nodejs/node/commit/c508bfc66b)] - **assert**: refactor to use more primordials (Antoine du Hamel) [#35998](https://github.com/nodejs/node/pull/35998)
* [[`a9d3a0df29`](https://github.com/nodejs/node/commit/a9d3a0df29)] - **assert,repl**: enable ecmaVersion 2021 in acorn parser (Michaël Zasso) [#35827](https://github.com/nodejs/node/pull/35827)
* [[`6d43c8dd69`](https://github.com/nodejs/node/commit/6d43c8dd69)] - **async_hooks**: refactor to use more primordials (Antoine du Hamel) [#36168](https://github.com/nodejs/node/pull/36168)
* [[`029ea16a24`](https://github.com/nodejs/node/commit/029ea16a24)] - **async_hooks**: fix leak in AsyncLocalStorage exit (Stephen Belanger) [#35779](https://github.com/nodejs/node/pull/35779)
* [[`d49e0ca73a`](https://github.com/nodejs/node/commit/d49e0ca73a)] - **benchmark**: fix build warnings (Gabriel Schulhof) [#36157](https://github.com/nodejs/node/pull/36157)
* [[`d027be0551`](https://github.com/nodejs/node/commit/d027be0551)] - **benchmark**: ignore build artifacts for napi addons (Richard Lau) [#35970](https://github.com/nodejs/node/pull/35970)
* [[`fdb1c0d31c`](https://github.com/nodejs/node/commit/fdb1c0d31c)] - **benchmark**: remove modules that require intl (Richard Lau) [#35968](https://github.com/nodejs/node/pull/35968)
* [[`f6487960b5`](https://github.com/nodejs/node/commit/f6487960b5)] - **benchmark**: make the benchmark tool work with Node 10 (Joyee Cheung) [#35817](https://github.com/nodejs/node/pull/35817)
* [[`21d3ccf5df`](https://github.com/nodejs/node/commit/21d3ccf5df)] - **benchmark**: add startup benchmark for loading public modules (Joyee Cheung) [#35816](https://github.com/nodejs/node/pull/35816)
* [[`0477e000bf`](https://github.com/nodejs/node/commit/0477e000bf)] - **bootstrap**: refactor to use more primordials (Antoine du Hamel) [#35999](https://github.com/nodejs/node/pull/35999)
* [[`699bb348d9`](https://github.com/nodejs/node/commit/699bb348d9)] - **build**: replace which with command -v (raisinten) [#36118](https://github.com/nodejs/node/pull/36118)
* [[`304e269001`](https://github.com/nodejs/node/commit/304e269001)] - **build**: try “python3” as a last resort for 3.x (Ole André Vadla Ravnås) [#35983](https://github.com/nodejs/node/pull/35983)
* [[`6bafe04911`](https://github.com/nodejs/node/commit/6bafe04911)] - **build**: conditionally clear vcinstalldir (Brian Ingenito) [#36009](https://github.com/nodejs/node/pull/36009)
* [[`f498127c41`](https://github.com/nodejs/node/commit/f498127c41)] - **build**: fix zlib inlining for IA-32 (raisinten) [#35679](https://github.com/nodejs/node/pull/35679)
* [[`f33fa264cc`](https://github.com/nodejs/node/commit/f33fa264cc)] - **build**: fix lint-js-fix target (Antoine du Hamel) [#35927](https://github.com/nodejs/node/pull/35927)
* [[`67d31827ac`](https://github.com/nodejs/node/commit/67d31827ac)] - **build**: add vcbuilt test-doc target (Antoine du Hamel) [#35708](https://github.com/nodejs/node/pull/35708)
* [[`2a8c2ddcb1`](https://github.com/nodejs/node/commit/2a8c2ddcb1)] - **build**: add license-builder GitHub Action (Tierney Cyren) [#35712](https://github.com/nodejs/node/pull/35712)
* [[`6c61b9372b`](https://github.com/nodejs/node/commit/6c61b9372b)] - **build**: use make functions instead of echo (Antoine du Hamel) [#35707](https://github.com/nodejs/node/pull/35707)
* [[`4813d913e3`](https://github.com/nodejs/node/commit/4813d913e3)] - **build**: use GITHUB\_ENV file to set env variables (Michaël Zasso) [#35638](https://github.com/nodejs/node/pull/35638)
* [[`71e0f33751`](https://github.com/nodejs/node/commit/71e0f33751)] - **build**: do not install jq in workflows (Michaël Zasso) [#35638](https://github.com/nodejs/node/pull/35638)
* [[`8ab7f258d4`](https://github.com/nodejs/node/commit/8ab7f258d4)] - **build, tools**: look for local installation of NASM (Richard Lau) [#36014](https://github.com/nodejs/node/pull/36014)
* [[`50552facb7`](https://github.com/nodejs/node/commit/50552facb7)] - **build,tools**: gitHub Actions: use Node.js Fermium (Antoine du Hamel) [#35840](https://github.com/nodejs/node/pull/35840)
* [[`77b7c985f6`](https://github.com/nodejs/node/commit/77b7c985f6)] - **build,tools**: add lint-js-doc target (Antoine du Hamel) [#35708](https://github.com/nodejs/node/pull/35708)
* [[`929e1272ee`](https://github.com/nodejs/node/commit/929e1272ee)] - **cluster**: refactor to use more primordials (Antoine du Hamel) [#36011](https://github.com/nodejs/node/pull/36011)
* [[`568e6177c9`](https://github.com/nodejs/node/commit/568e6177c9)] - **console**: use more primordials (Antoine du Hamel) [#35734](https://github.com/nodejs/node/pull/35734)
* [[`6cea3152fe`](https://github.com/nodejs/node/commit/6cea3152fe)] - **deps**: upgrade npm to 6.14.9 (Myles Borins) [#36450](https://github.com/nodejs/node/pull/36450)
* [[`d2ee676eb9`](https://github.com/nodejs/node/commit/d2ee676eb9)] - **deps**: cherry-pick 9a49b22 from V8 upstream (Daniel Bevenius) [#35939](https://github.com/nodejs/node/pull/35939)
* [[`7367e6c6be`](https://github.com/nodejs/node/commit/7367e6c6be)] - **deps**: update acorn to v8.0.4 (Michaël Zasso) [#35791](https://github.com/nodejs/node/pull/35791)
* [[`4937a34be6`](https://github.com/nodejs/node/commit/4937a34be6)] - **deps**: fix typo in zlib.gyp that break arm-fpu-neon build (lucasg) [#35659](https://github.com/nodejs/node/pull/35659)
* [[`1e8dfb9d2c`](https://github.com/nodejs/node/commit/1e8dfb9d2c)] - **deps**: upgrade to cjs-module-lexer@1.0.0 (Guy Bedford) [#35928](https://github.com/nodejs/node/pull/35928)
* [[`0356963f0e`](https://github.com/nodejs/node/commit/0356963f0e)] - **deps**: update to cjs-module-lexer@0.5.2 (Guy Bedford) [#35901](https://github.com/nodejs/node/pull/35901)
* [[`172be4ffe0`](https://github.com/nodejs/node/commit/172be4ffe0)] - **deps**: upgrade to cjs-module-lexer@0.5.0 (Guy Bedford) [#35871](https://github.com/nodejs/node/pull/35871)
* [[`1f7740691d`](https://github.com/nodejs/node/commit/1f7740691d)] - **deps**: update to cjs-module-lexer@0.4.3 (Guy Bedford) [#35745](https://github.com/nodejs/node/pull/35745)
* [[`47bd445e56`](https://github.com/nodejs/node/commit/47bd445e56)] - **doc**: remove stray comma in url.md (Rich Trott) [#36175](https://github.com/nodejs/node/pull/36175)
* [[`2f76a75fc6`](https://github.com/nodejs/node/commit/2f76a75fc6)] - **doc**: revise agent.destroy() text (Rich Trott) [#36163](https://github.com/nodejs/node/pull/36163)
* [[`72fb6f88ab`](https://github.com/nodejs/node/commit/72fb6f88ab)] - **doc**: add compatibility/interop technical value (Geoffrey Booth) [#35323](https://github.com/nodejs/node/pull/35323)
* [[`f5efd54727`](https://github.com/nodejs/node/commit/f5efd54727)] - **doc**: de-emphasize wrapping in napi\_define\_class (Gabriel Schulhof) [#36159](https://github.com/nodejs/node/pull/36159)
* [[`8a7c2b951d`](https://github.com/nodejs/node/commit/8a7c2b951d)] - **doc**: clarify text about process not responding (Rich Trott) [#36117](https://github.com/nodejs/node/pull/36117)
* [[`800e1db83d`](https://github.com/nodejs/node/commit/800e1db83d)] - **doc**: esm docs consolidation and reordering (Guy Bedford) [#36046](https://github.com/nodejs/node/pull/36046)
* [[`4fad888fe1`](https://github.com/nodejs/node/commit/4fad888fe1)] - **doc**: move shigeki to emeritus (Rich Trott) [#36093](https://github.com/nodejs/node/pull/36093)
* [[`c088434b4d`](https://github.com/nodejs/node/commit/c088434b4d)] - **doc**: document the error when cwd not exists in child\_process.spawn (FeelyChau) [#34505](https://github.com/nodejs/node/pull/34505)
* [[`4dbbbaa2e9`](https://github.com/nodejs/node/commit/4dbbbaa2e9)] - **doc**: fix typo in debugger.md (Rich Trott) [#36066](https://github.com/nodejs/node/pull/36066)
* [[`d796bc7348`](https://github.com/nodejs/node/commit/d796bc7348)] - **doc**: update list styles for remark-parse@9 rendering (Rich Trott) [#36049](https://github.com/nodejs/node/pull/36049)
* [[`6daf204f32`](https://github.com/nodejs/node/commit/6daf204f32)] - **doc**: escape asterisk in cctest gtest-filter (raisinten) [#36034](https://github.com/nodejs/node/pull/36034)
* [[`9470bf5872`](https://github.com/nodejs/node/commit/9470bf5872)] - **doc**: move v8.getHeapCodeStatistics() (Rich Trott) [#36027](https://github.com/nodejs/node/pull/36027)
* [[`30cd797c15`](https://github.com/nodejs/node/commit/30cd797c15)] - **doc**: add note regarding file structure in src/README.md (Denys Otrishko) [#35000](https://github.com/nodejs/node/pull/35000)
* [[`cddcfcde9f`](https://github.com/nodejs/node/commit/cddcfcde9f)] - **doc**: advise users to import the full set of trusted release keys (Reşat SABIQ) [#32655](https://github.com/nodejs/node/pull/32655)
* [[`1ca1f262a5`](https://github.com/nodejs/node/commit/1ca1f262a5)] - **doc**: fix crypto doc linter errors (Antoine du Hamel) [#36035](https://github.com/nodejs/node/pull/36035)
* [[`b11725eb9e`](https://github.com/nodejs/node/commit/b11725eb9e)] - **doc**: revise v8.getHeapSnapshot() (Rich Trott) [#35849](https://github.com/nodejs/node/pull/35849)
* [[`990facbc3e`](https://github.com/nodejs/node/commit/990facbc3e)] - **doc**: update core-validate-commit link in guide (Daijiro Wachi) [#35938](https://github.com/nodejs/node/pull/35938)
* [[`773685c2a4`](https://github.com/nodejs/node/commit/773685c2a4)] - **doc**: update benchmark CI test indicator in README (Rich Trott) [#35945](https://github.com/nodejs/node/pull/35945)
* [[`c90571ff2a`](https://github.com/nodejs/node/commit/c90571ff2a)] - **doc**: add new wordings to the API description (Pooja D.P) [#35588](https://github.com/nodejs/node/pull/35588)
* [[`6259c2d231`](https://github.com/nodejs/node/commit/6259c2d231)] - **doc**: option --prof documentation help added (krank2me) [#34991](https://github.com/nodejs/node/pull/34991)
* [[`98e4b77b89`](https://github.com/nodejs/node/commit/98e4b77b89)] - **doc**: fix release-schedule link in backport guide (Daijiro Wachi) [#35920](https://github.com/nodejs/node/pull/35920)
* [[`51ce1a2fa8`](https://github.com/nodejs/node/commit/51ce1a2fa8)] - **doc**: update tables in README files for linting changes (Rich Trott) [#35905](https://github.com/nodejs/node/pull/35905)
* [[`513bed2776`](https://github.com/nodejs/node/commit/513bed2776)] - **doc**: temporarily disable list-item-bullet-indent (Nick Schonning) [#35647](https://github.com/nodejs/node/pull/35647)
* [[`733c9da1e9`](https://github.com/nodejs/node/commit/733c9da1e9)] - **doc**: disable no-undefined-references workarounds (Nick Schonning) [#35647](https://github.com/nodejs/node/pull/35647)
* [[`6e1612fa15`](https://github.com/nodejs/node/commit/6e1612fa15)] - **doc**: adjust table alignment for remark v13 (Nick Schonning) [#35647](https://github.com/nodejs/node/pull/35647)
* [[`a15dede26d`](https://github.com/nodejs/node/commit/a15dede26d)] - **doc**: move bnoordhuis to emeritus (Ben Noordhuis) [#35865](https://github.com/nodejs/node/pull/35865)
* [[`26e42939f2`](https://github.com/nodejs/node/commit/26e42939f2)] - **doc**: add on statement in the APIs docs (Pooja D.P) [#35610](https://github.com/nodejs/node/pull/35610)
* [[`9486f5fc37`](https://github.com/nodejs/node/commit/9486f5fc37)] - **doc**: move ronkorving to emeritus (Rich Trott) [#35828](https://github.com/nodejs/node/pull/35828)
* [[`3f3d2d781b`](https://github.com/nodejs/node/commit/3f3d2d781b)] - **doc**: recommend test-doc instead of lint-md (Antoine du Hamel) [#35708](https://github.com/nodejs/node/pull/35708)
* [[`8131d954d9`](https://github.com/nodejs/node/commit/8131d954d9)] - **doc**: fix reference to googletest test fixture (Tobias Nießen) [#35813](https://github.com/nodejs/node/pull/35813)
* [[`34d6ca3bef`](https://github.com/nodejs/node/commit/34d6ca3bef)] - **doc**: add conditional example for setBreakpoint() (Chris Opperwall) [#35823](https://github.com/nodejs/node/pull/35823)
* [[`29849743b8`](https://github.com/nodejs/node/commit/29849743b8)] - **doc**: make small improvements to REPL doc (Rich Trott) [#35808](https://github.com/nodejs/node/pull/35808)
* [[`02f9a2a77a`](https://github.com/nodejs/node/commit/02f9a2a77a)] - **doc**: update MessagePort documentation for EventTarget inheritance (Anna Henningsen) [#35839](https://github.com/nodejs/node/pull/35839)
* [[`9c7d4bd0f3`](https://github.com/nodejs/node/commit/9c7d4bd0f3)] - **doc**: use case-sensitive in the example (Pooja D.P) [#35624](https://github.com/nodejs/node/pull/35624)
* [[`600cffae3c`](https://github.com/nodejs/node/commit/600cffae3c)] - **doc**: consolidate and clarify breakOnSigInt text (Rich Trott) [#35787](https://github.com/nodejs/node/pull/35787)
* [[`0de3f564b2`](https://github.com/nodejs/node/commit/0de3f564b2)] - **doc**: add a subsystems header in pull-requests.md (Pooja D.P) [#35718](https://github.com/nodejs/node/pull/35718)
* [[`47b4b2be29`](https://github.com/nodejs/node/commit/47b4b2be29)] - **doc**: add require statement in the example (Pooja D.P) [#35554](https://github.com/nodejs/node/pull/35554)
* [[`77cfcba7c8`](https://github.com/nodejs/node/commit/77cfcba7c8)] - **doc**: modified memory set statement set size (Pooja D.P) [#35517](https://github.com/nodejs/node/pull/35517)
* [[`41937f76f0`](https://github.com/nodejs/node/commit/41937f76f0)] - **doc**: use kbd element in readline doc prose (Rich Trott) [#35737](https://github.com/nodejs/node/pull/35737)
* [[`eee62b05f6`](https://github.com/nodejs/node/commit/eee62b05f6)] - **doc**: fix header level in fs.md (ax1) [#35771](https://github.com/nodejs/node/pull/35771)
* [[`63533d7d56`](https://github.com/nodejs/node/commit/63533d7d56)] - **doc**: remove stability warning in v8 module doc (Rich Trott) [#35774](https://github.com/nodejs/node/pull/35774)
* [[`62bf1a63d6`](https://github.com/nodejs/node/commit/62bf1a63d6)] - **doc**: mark optional parameters in timers.md (Vse Mozhe Buty) [#35764](https://github.com/nodejs/node/pull/35764)
* [[`4dc5e4a354`](https://github.com/nodejs/node/commit/4dc5e4a354)] - **doc**: add a example code to API doc property (Pooja D.P) [#35738](https://github.com/nodejs/node/pull/35738)
* [[`8ef0652566`](https://github.com/nodejs/node/commit/8ef0652566)] - **doc**: update console.error example (Lee, Bonggi) [#34964](https://github.com/nodejs/node/pull/34964)
* [[`47ba12265e`](https://github.com/nodejs/node/commit/47ba12265e)] - **doc**: improve text for breakOnSigint (Rich Trott) [#35692](https://github.com/nodejs/node/pull/35692)
* [[`c0d9756163`](https://github.com/nodejs/node/commit/c0d9756163)] - **doc**: this prints replaced with this is printed (Pooja D.P) [#35515](https://github.com/nodejs/node/pull/35515)
* [[`2feb86e635`](https://github.com/nodejs/node/commit/2feb86e635)] - **doc**: update package.json field definitions (Myles Borins) [#35741](https://github.com/nodejs/node/pull/35741)
* [[`d0d67c67c0`](https://github.com/nodejs/node/commit/d0d67c67c0)] - **doc**: add Installing Node.js header in BUILDING.md (Pooja D.P) [#35710](https://github.com/nodejs/node/pull/35710)
* [[`7c089ad04c`](https://github.com/nodejs/node/commit/7c089ad04c)] - **doc**: use kbd element in readline doc (Rich Trott) [#35698](https://github.com/nodejs/node/pull/35698)
* [[`ba623ef35a`](https://github.com/nodejs/node/commit/ba623ef35a)] - **doc**: add release key for Danielle Adams (Danielle Adams) [#35545](https://github.com/nodejs/node/pull/35545)
* [[`df4043bed3`](https://github.com/nodejs/node/commit/df4043bed3)] - **doc**: use kbd element in os doc (Rich Trott) [#35656](https://github.com/nodejs/node/pull/35656)
* [[`4d72e982de`](https://github.com/nodejs/node/commit/4d72e982de)] - **doc**: add a statement in the documentation. (Pooja D.P) [#35585](https://github.com/nodejs/node/pull/35585)
* [[`238885288d`](https://github.com/nodejs/node/commit/238885288d)] - **doc**: clarify experimental API elements in vm.md (Rich Trott) [#35594](https://github.com/nodejs/node/pull/35594)
* [[`806a269a83`](https://github.com/nodejs/node/commit/806a269a83)] - **doc**: importModuleDynamically gets Script, not Module (Simen Bekkhus) [#35593](https://github.com/nodejs/node/pull/35593)
* [[`6c4e697f56`](https://github.com/nodejs/node/commit/6c4e697f56)] - **doc**: fix EventEmitter examples (Sourav Shaw) [#33513](https://github.com/nodejs/node/pull/33513)
* [[`f6ebd81693`](https://github.com/nodejs/node/commit/f6ebd81693)] - **doc**: add example code for process.getgroups() (Pooja D.P) [#35625](https://github.com/nodejs/node/pull/35625)
* [[`2c342662e5`](https://github.com/nodejs/node/commit/2c342662e5)] - **doc**: use kbd element in tty doc (Rich Trott) [#35613](https://github.com/nodejs/node/pull/35613)
* [[`f723335f9e`](https://github.com/nodejs/node/commit/f723335f9e)] - **doc**: remove documentation for stream.\_construct() (Luigi Pinca) [#36119](https://github.com/nodejs/node/pull/36119)
* [[`e71b4baa88`](https://github.com/nodejs/node/commit/e71b4baa88)] - **doc**: Remove reference to io.js (Hussaina Begum Nandyala) [#35618](https://github.com/nodejs/node/pull/35618)
* [[`4faf71b474`](https://github.com/nodejs/node/commit/4faf71b474)] - **doc,crypto**: added sign/verify method changes about dsaEncoding (Filip Skokan) [#35480](https://github.com/nodejs/node/pull/35480)
* [[`e9d485f878`](https://github.com/nodejs/node/commit/e9d485f878)] - **doc,esm**: document experimental warning removal (Antoine du Hamel) [#35750](https://github.com/nodejs/node/pull/35750)
* [[`17c3fc67cf`](https://github.com/nodejs/node/commit/17c3fc67cf)] - **doc,fs**: document value of stats.isDirectory on symbolic links (coderaiser) [#27413](https://github.com/nodejs/node/pull/27413)
* [[`fc17ead531`](https://github.com/nodejs/node/commit/fc17ead531)] - **doc,net**: document socket.timeout (Brandon Kobel) [#34543](https://github.com/nodejs/node/pull/34543)
* [[`dc589b541f`](https://github.com/nodejs/node/commit/dc589b541f)] - **doc,src,test**: revise C++ code for linter update (Rich Trott) [#35719](https://github.com/nodejs/node/pull/35719)
* [[`0a944a42c0`](https://github.com/nodejs/node/commit/0a944a42c0)] - **doc,stream**: write(chunk, encoding, cb) encoding can be null (dev-script) [#35372](https://github.com/nodejs/node/pull/35372)
* [[`be79250aad`](https://github.com/nodejs/node/commit/be79250aad)] - **doc,test**: update v8 method doc and comment (Rich Trott) [#35795](https://github.com/nodejs/node/pull/35795)
* [[`8fdf077efc`](https://github.com/nodejs/node/commit/8fdf077efc)] - **doc,url**: fix url.hostname example (Rishabh Mehan) [#33735](https://github.com/nodejs/node/pull/33735)
* [[`3a08afc402`](https://github.com/nodejs/node/commit/3a08afc402)] - **domain**: refactor to use more primordials (Antoine du Hamel) [#35885](https://github.com/nodejs/node/pull/35885)
* [[`8d672b8e53`](https://github.com/nodejs/node/commit/8d672b8e53)] - **esm**: refactor to use more primordials (Antoine du Hamel) [#36019](https://github.com/nodejs/node/pull/36019)
* [[`570a8bfe12`](https://github.com/nodejs/node/commit/570a8bfe12)] - **events**: port some wpt tests (Benjamin Gruenbaum) [#33621](https://github.com/nodejs/node/pull/33621)
* [[`8ef4557c65`](https://github.com/nodejs/node/commit/8ef4557c65)] - **events**: make eventTarget.removeAllListeners() return this (Luigi Pinca) [#35805](https://github.com/nodejs/node/pull/35805)
* [[`d27e56356b`](https://github.com/nodejs/node/commit/d27e56356b)] - **fs**: remove experimental from promises.rmdir recursive (Anders Kaseorg) [#36131](https://github.com/nodejs/node/pull/36131)
* [[`8d84bdc46b`](https://github.com/nodejs/node/commit/8d84bdc46b)] - **fs**: filehandle read now accepts object as argument (Nikola Glavina) [#34180](https://github.com/nodejs/node/pull/34180)
* [[`7c3b6f17e3`](https://github.com/nodejs/node/commit/7c3b6f17e3)] - **fs**: replace finally with PromisePrototypeFinally (Baruch Odem (Rothkoff)) [#35995](https://github.com/nodejs/node/pull/35995)
* [[`2f692c4cc6`](https://github.com/nodejs/node/commit/2f692c4cc6)] - **fs**: remove unnecessary Function#bind() in fs/promises (Ben Noordhuis) [#35208](https://github.com/nodejs/node/pull/35208)
* [[`5f0c8142b7`](https://github.com/nodejs/node/commit/5f0c8142b7)] - **fs**: remove unused assignment (Rich Trott) [#35642](https://github.com/nodejs/node/pull/35642)
* [[`e2b8734d20`](https://github.com/nodejs/node/commit/e2b8734d20)] - **gyp,build**: consistent shared library location (Rod Vagg) [#35635](https://github.com/nodejs/node/pull/35635)
* [[`45aee0d25e`](https://github.com/nodejs/node/commit/45aee0d25e)] - **http**: fix typo in comment (Hollow Man) [#36193](https://github.com/nodejs/node/pull/36193)
* [[`b58725c4c0`](https://github.com/nodejs/node/commit/b58725c4c0)] - **http**: lazy create IncomingMessage.headers (Robert Nagy) [#35281](https://github.com/nodejs/node/pull/35281)
* [[`71c3efe278`](https://github.com/nodejs/node/commit/71c3efe278)] - **http2**: check write not scheduled in scope destructor (David Halls) [#36241](https://github.com/nodejs/node/pull/36241)
* [[`ab2b066fc1`](https://github.com/nodejs/node/commit/ab2b066fc1)] - **http2**: delay session.receive() by a tick (Szymon Marczak) [#35985](https://github.com/nodejs/node/pull/35985)
* [[`c4e17cfa25`](https://github.com/nodejs/node/commit/c4e17cfa25)] - **http2**: add has method to proxySocketHandler (masx200) [#35197](https://github.com/nodejs/node/pull/35197)
* [[`c455b848d9`](https://github.com/nodejs/node/commit/c455b848d9)] - **http2**: centralise socket event binding in Http2Session (Momtchil Momtchev) [#35772](https://github.com/nodejs/node/pull/35772)
* [[`dce01fd27f`](https://github.com/nodejs/node/commit/dce01fd27f)] - **http2**: move events to the JSStreamSocket (Momtchil Momtchev) [#35772](https://github.com/nodejs/node/pull/35772)
* [[`92bd7b522a`](https://github.com/nodejs/node/commit/92bd7b522a)] - **http2**: fix error stream write followed by destroy (David Halls) [#35951](https://github.com/nodejs/node/pull/35951)
* [[`ec9fae96bc`](https://github.com/nodejs/node/commit/ec9fae96bc)] - **http2**: fix reinjection check (Momtchil Momtchev) [#35678](https://github.com/nodejs/node/pull/35678)
* [[`57f2fe0609`](https://github.com/nodejs/node/commit/57f2fe0609)] - **http2**: reinject data received before http2 is attached (Momtchil Momtchev) [#35678](https://github.com/nodejs/node/pull/35678)
* [[`2dbaaf92e5`](https://github.com/nodejs/node/commit/2dbaaf92e5)] - **http2**: remove unsupported %.\* specifier (Momtchil Momtchev) [#35694](https://github.com/nodejs/node/pull/35694)
* [[`de3c8045ac`](https://github.com/nodejs/node/commit/de3c8045ac)] - **lib**: refactor to use more primordials (Antoine du Hamel) [#35875](https://github.com/nodejs/node/pull/35875)
* [[`41d997cc72`](https://github.com/nodejs/node/commit/41d997cc72)] - **lib**: use primordials when calling methods of Error (Antoine du Hamel) [#35837](https://github.com/nodejs/node/pull/35837)
* [[`d58a466da0`](https://github.com/nodejs/node/commit/d58a466da0)] - **lib**: honor setUncaughtExceptionCaptureCallback (Gireesh Punathil) [#35595](https://github.com/nodejs/node/pull/35595)
* [[`1fdf72765b`](https://github.com/nodejs/node/commit/1fdf72765b)] - **module**: only try to enrich CJS syntax errors (Michaël Zasso) [#35691](https://github.com/nodejs/node/pull/35691)
* [[`81b0562c62`](https://github.com/nodejs/node/commit/81b0562c62)] - **n-api**: clean up binding creation (Gabriel Schulhof) [#36170](https://github.com/nodejs/node/pull/36170)
* [[`7a01e241ee`](https://github.com/nodejs/node/commit/7a01e241ee)] - **n-api**: fix test\_async\_context warnings (Gabriel Schulhof) [#36171](https://github.com/nodejs/node/pull/36171)
* [[`dde727e72f`](https://github.com/nodejs/node/commit/dde727e72f)] - **n-api**: improve consistency of how we get context (Michael Dawson) [#36068](https://github.com/nodejs/node/pull/36068)
* [[`08657e7e11`](https://github.com/nodejs/node/commit/08657e7e11)] - **n-api**: factor out calling pattern (Gabriel Schulhof) [#36113](https://github.com/nodejs/node/pull/36113)
* [[`88aa4e0d25`](https://github.com/nodejs/node/commit/88aa4e0d25)] - **n-api**: unlink reference during its destructor (Gabriel Schulhof) [#35933](https://github.com/nodejs/node/pull/35933)
* [[`1cb50c17d3`](https://github.com/nodejs/node/commit/1cb50c17d3)] - **n-api**: napi\_make\_callback emit async init with resource of async\_context (legendecas) [#32930](https://github.com/nodejs/node/pull/32930)
* [[`f1e84f4dd8`](https://github.com/nodejs/node/commit/f1e84f4dd8)] - **n-api**: revert change to finalization (Michael Dawson) [#35777](https://github.com/nodejs/node/pull/35777)
* [[`e16124979d`](https://github.com/nodejs/node/commit/e16124979d)] - **querystring**: reduce memory usage by Int8Array (sapics) [#34179](https://github.com/nodejs/node/pull/34179)
* [[`5c81a1071e`](https://github.com/nodejs/node/commit/5c81a1071e)] - **src**: refactor using-declarations node\_env\_var.cc (raisinten) [#36128](https://github.com/nodejs/node/pull/36128)
* [[`2770cd941e`](https://github.com/nodejs/node/commit/2770cd941e)] - **src**: remove duplicate logic for getting buffer (Yash Ladha) [#34553](https://github.com/nodejs/node/pull/34553)
* [[`f2300390aa`](https://github.com/nodejs/node/commit/f2300390aa)] - **src**: create helper for reading Uint32BE (Juan José Arboleda) [#34944](https://github.com/nodejs/node/pull/34944)
* [[`34c870e9f0`](https://github.com/nodejs/node/commit/34c870e9f0)] - **src**: use MaybeLocal.ToLocal instead of IsEmpty (Daniel Bevenius) [#35716](https://github.com/nodejs/node/pull/35716)
* [[`00d9499b14`](https://github.com/nodejs/node/commit/00d9499b14)] - **src**: large pages support in illumos/solaris systems (David Carlier) [#34320](https://github.com/nodejs/node/pull/34320)
* [[`7c99885a9b`](https://github.com/nodejs/node/commit/7c99885a9b)] - **stream**: fix thrown object reference (Gil Pedersen) [#36065](https://github.com/nodejs/node/pull/36065)
* [[`1cefb7e710`](https://github.com/nodejs/node/commit/1cefb7e710)] - **stream**: fix regression on duplex end (Momtchil Momtchev) [#35941](https://github.com/nodejs/node/pull/35941)
* [[`d1fd3f27e4`](https://github.com/nodejs/node/commit/d1fd3f27e4)] - **stream**: remove redundant context from comments (Yash Ladha) [#35728](https://github.com/nodejs/node/pull/35728)
* [[`fb14acb22c`](https://github.com/nodejs/node/commit/fb14acb22c)] - **stream**: move to internal/streams (Matteo Collina) [#35239](https://github.com/nodejs/node/pull/35239)
* [[`40d59281f7`](https://github.com/nodejs/node/commit/40d59281f7)] - **test**: update comments in test-fs-read-offset-null (Rich Trott) [#36152](https://github.com/nodejs/node/pull/36152)
* [[`a563f79d80`](https://github.com/nodejs/node/commit/a563f79d80)] - **test**: fix typo in inspector-helper.js (Luigi Pinca) [#36127](https://github.com/nodejs/node/pull/36127)
* [[`3e77536c6b`](https://github.com/nodejs/node/commit/3e77536c6b)] - **test**: deflake test-http-destroyed-socket-write2 (Luigi Pinca) [#36120](https://github.com/nodejs/node/pull/36120)
* [[`402e29a87c`](https://github.com/nodejs/node/commit/402e29a87c)] - **test**: make test-http2-client-jsstream-destroy.js reliable (Rich Trott) [#36129](https://github.com/nodejs/node/pull/36129)
* [[`b6aa42c349`](https://github.com/nodejs/node/commit/b6aa42c349)] - **test**: add test for fs.read when offset key is null (mayank agarwal) [#35918](https://github.com/nodejs/node/pull/35918)
* [[`8516c2ef90`](https://github.com/nodejs/node/commit/8516c2ef90)] - **test**: improve test-stream-duplex-readable-end (Luigi Pinca) [#36056](https://github.com/nodejs/node/pull/36056)
* [[`b53068ec0d`](https://github.com/nodejs/node/commit/b53068ec0d)] - **test**: add util.inspect test for null maxStringLength (Rich Trott) [#36086](https://github.com/nodejs/node/pull/36086)
* [[`3029872631`](https://github.com/nodejs/node/commit/3029872631)] - **test**: replace var with const (Aleksandr Krutko) [#36069](https://github.com/nodejs/node/pull/36069)
* [[`b05cdfee64`](https://github.com/nodejs/node/commit/b05cdfee64)] - **test**: remove flaky designation for fixed test (Rich Trott) [#35961](https://github.com/nodejs/node/pull/35961)
* [[`002005f537`](https://github.com/nodejs/node/commit/002005f537)] - **test**: improve error message for policy failures (Bradley Meck) [#35633](https://github.com/nodejs/node/pull/35633)
* [[`1453de1381`](https://github.com/nodejs/node/commit/1453de1381)] - **test**: update old comment style test\_util.cc (raisinten) [#35884](https://github.com/nodejs/node/pull/35884)
* [[`de375e16f4`](https://github.com/nodejs/node/commit/de375e16f4)] - **test**: add missing ref comments to parallel.status (Rich Trott) [#35896](https://github.com/nodejs/node/pull/35896)
* [[`cab65fbe63`](https://github.com/nodejs/node/commit/cab65fbe63)] - **test**: mark test-worker-eventlooputil flaky (Myles Borins) [#35886](https://github.com/nodejs/node/pull/35886)
* [[`4ed4b64293`](https://github.com/nodejs/node/commit/4ed4b64293)] - **test**: mark test-http2-respond-file-error-pipe-offset flaky (Myles Borins) [#35883](https://github.com/nodejs/node/pull/35883)
* [[`a5b94180fe`](https://github.com/nodejs/node/commit/a5b94180fe)] - **test**: fix reference to WPT testharness.js (Tobias Nießen) [#35814](https://github.com/nodejs/node/pull/35814)
* [[`3bb7f3602b`](https://github.com/nodejs/node/commit/3bb7f3602b)] - **test**: add onerror test cases to policy (Daijiro Wachi) [#35797](https://github.com/nodejs/node/pull/35797)
* [[`0aba12218a`](https://github.com/nodejs/node/commit/0aba12218a)] - **test**: add upstream test cases to encoding (Daijiro Wachi) [#35794](https://github.com/nodejs/node/pull/35794)
* [[`f535d6252f`](https://github.com/nodejs/node/commit/f535d6252f)] - **test**: add test for listen callback runtime binding (H Adinarayana) [#35657](https://github.com/nodejs/node/pull/35657)
* [[`d62e72b341`](https://github.com/nodejs/node/commit/d62e72b341)] - **test**: refactor test-https-host-headers (himself65) [#32805](https://github.com/nodejs/node/pull/32805)
* [[`70cb70812d`](https://github.com/nodejs/node/commit/70cb70812d)] - **test**: add common.mustSucceed (Tobias Nießen) [#35086](https://github.com/nodejs/node/pull/35086)
* [[`226c1800a8`](https://github.com/nodejs/node/commit/226c1800a8)] - **test**: check for AbortController existence (James M Snell) [#35616](https://github.com/nodejs/node/pull/35616)
* [[`41aac465cc`](https://github.com/nodejs/node/commit/41aac465cc)] - **timers**: correct explanation in comment (Turner Jabbour) [#35437](https://github.com/nodejs/node/pull/35437)
* [[`713d1ebe75`](https://github.com/nodejs/node/commit/713d1ebe75)] - **tools**: bump unist-util-find@1.0.1 to unist-util-find@1.0.2 (Rich Trott) [#36106](https://github.com/nodejs/node/pull/36106)
* [[`127a4fb810`](https://github.com/nodejs/node/commit/127a4fb810)] - **tools**: only use 2 cores for macos action (Myles Borins) [#36169](https://github.com/nodejs/node/pull/36169)
* [[`75e49b833b`](https://github.com/nodejs/node/commit/75e49b833b)] - **tools**: remove bashisms from license builder script (Antoine du Hamel) [#36122](https://github.com/nodejs/node/pull/36122)
* [[`28d6283f96`](https://github.com/nodejs/node/commit/28d6283f96)] - **tools**: hide commit queue action link (Antoine du Hamel) [#36124](https://github.com/nodejs/node/pull/36124)
* [[`b7441ea4d2`](https://github.com/nodejs/node/commit/b7441ea4d2)] - **tools**: update doc tools to remark-parse@9.0.0 (Rich Trott) [#36049](https://github.com/nodejs/node/pull/36049)
* [[`5a41282ef5`](https://github.com/nodejs/node/commit/5a41282ef5)] - **tools**: enforce use of single quotes in editorconfig (Antoine du Hamel) [#36020](https://github.com/nodejs/node/pull/36020)
* [[`23dd2b00dd`](https://github.com/nodejs/node/commit/23dd2b00dd)] - **tools**: fix config serialization w/ long strings (Ole André Vadla Ravnås) [#35982](https://github.com/nodejs/node/pull/35982)
* [[`4664681220`](https://github.com/nodejs/node/commit/4664681220)] - **tools**: don't print gold linker warning w/o flag (Myles Borins) [#35955](https://github.com/nodejs/node/pull/35955)
* [[`dfd6ad9d99`](https://github.com/nodejs/node/commit/dfd6ad9d99)] - **tools**: refloat 7 Node.js patches to cpplint.py (Rich Trott) [#35866](https://github.com/nodejs/node/pull/35866)
* [[`d177cb3993`](https://github.com/nodejs/node/commit/d177cb3993)] - **tools**: bump cpplint to 1.5.1 (Rich Trott) [#35866](https://github.com/nodejs/node/pull/35866)
* [[`b19a85ed2a`](https://github.com/nodejs/node/commit/b19a85ed2a)] - **tools**: add update-npm script (Myles Borins) [#35822](https://github.com/nodejs/node/pull/35822)
* [[`07e5d35d14`](https://github.com/nodejs/node/commit/07e5d35d14)] - **tools**: refloat 7 Node.js patches to cpplint.py (Rich Trott) [#35719](https://github.com/nodejs/node/pull/35719)
* [[`c7301533de`](https://github.com/nodejs/node/commit/c7301533de)] - **tools**: bump cpplint to 1.5.0 (Rich Trott) [#35719](https://github.com/nodejs/node/pull/35719)
* [[`985efdfa09`](https://github.com/nodejs/node/commit/985efdfa09)] - **tools**: update gyp-next to v0.6.2 (Michaël Zasso) [#35690](https://github.com/nodejs/node/pull/35690)
* [[`baca8ee873`](https://github.com/nodejs/node/commit/baca8ee873)] - **tools**: update gyp-next to v0.6.0 (Ujjwal Sharma) [#35635](https://github.com/nodejs/node/pull/35635)
* [[`3e7598da9b`](https://github.com/nodejs/node/commit/3e7598da9b)] - **tools,doc**: enable ecmaVersion 2021 in acorn parser (Antoine du Hamel) [#35994](https://github.com/nodejs/node/pull/35994)
* [[`dfb353b882`](https://github.com/nodejs/node/commit/dfb353b882)] - **util**: fix to inspect getters that access this (raisinten) [#36052](https://github.com/nodejs/node/pull/36052)
* [[`1906f19e49`](https://github.com/nodejs/node/commit/1906f19e49)] - **vm**: refactor to use more primordials (Antoine du Hamel) [#36023](https://github.com/nodejs/node/pull/36023)
* [[`ffe517b40d`](https://github.com/nodejs/node/commit/ffe517b40d)] - **win, build**: fix build time on Windows (Bartosz Sosnowski) [#35932](https://github.com/nodejs/node/pull/35932)
* [[`c4c8541621`](https://github.com/nodejs/node/commit/c4c8541621)] - **win,build,tools**: support VS prerelease (Baruch Odem) [#36033](https://github.com/nodejs/node/pull/36033)
* [[`f59e225675`](https://github.com/nodejs/node/commit/f59e225675)] - **zlib**: test BrotliCompress throws invalid arg value (raisinten) [#35830](https://github.com/nodejs/node/pull/35830)

<a id="14.15.1"></a>
## 2020-11-16, Version 14.15.1 'Fermium' (LTS), @BethGriggs

### Notable changes

This is a security release.

Vulnerabilities fixed:

* **CVE-2020-8277**: Denial of Service through DNS request (High). A Node.js application that allows an attacker to trigger a DNS request for a host of their choice could trigger a Denial of Service by getting the application to resolve a DNS record with a larger number of responses.

### Commits

* [[`1fd2c8142b`](https://github.com/nodejs/node/commit/1fd2c8142b)] - **deps**: cherry-pick 0d252eb from upstream c-ares (Michael Dawson) [nodejs-private/node-private#231](https://github.com/nodejs-private/node-private/pull/231)

<a id="14.15.0"></a>
## 2020-10-27, Version 14.15.0 'Fermium' (LTS), @richardlau

### Notable Changes

This release marks the transition of Node.js 14.x into Long Term Support (LTS)
with the codename 'Fermium'. The 14.x release line now moves into "Active LTS"
and will remain so until October 2021. After that time, it will move into
"Maintenance" until end of life in April 2023.

### Commits

* [[`5b7a08c902`](https://github.com/nodejs/node/commit/5b7a08c902)] - **doc**: add missing link in Node.js 14 Changelog (Antoine du Hamel) [#35782](https://github.com/nodejs/node/pull/35782)
* [[`90a5d59824`](https://github.com/nodejs/node/commit/90a5d59824)] - **doc**: fix Node.js 14.x changelogs (Richard Lau) [#35756](https://github.com/nodejs/node/pull/35756)
* [[`7f788573b3`](https://github.com/nodejs/node/commit/7f788573b3)] - ***Revert*** "**test**: mark test-webcrypto-encrypt-decrypt-aes flaky" (Myles Borins) [#35666](https://github.com/nodejs/node/pull/35666)

<a id="14.14.0"></a>
## 2020-10-15, Version 14.14.0 (Current), @MylesBorins

### Notable Changes

* [[`7e7afc5186`](https://github.com/nodejs/node/commit/7e7afc5186)] - **crypto**: update certdata to NSS 3.56 (Shelley Vohr) [#35546](https://github.com/nodejs/node/pull/35546)
* [[`8877430530`](https://github.com/nodejs/node/commit/8877430530)] - **doc**: add aduh95 to collaborators (Antoine du Hamel) [#35542](https://github.com/nodejs/node/pull/35542)
* [[`1610728d7c`](https://github.com/nodejs/node/commit/1610728d7c)] - **(SEMVER-MINOR)** **fs**: add rm method (Ian Sutherland) [#35494](https://github.com/nodejs/node/pull/35494)
* [[`6ff152cc67`](https://github.com/nodejs/node/commit/6ff152cc67)] - **(SEMVER-MINOR)** **http**: allow passing array of key/val into writeHead (Robert Nagy) [#35274](https://github.com/nodejs/node/pull/35274)
* [[`93f947af0a`](https://github.com/nodejs/node/commit/93f947af0a)] - **(SEMVER-MINOR)** **src**: expose v8::Isolate setup callbacks (Shelley Vohr) [#35512](https://github.com/nodejs/node/pull/35512)

### Commits

* [[`16c17ddd88`](https://github.com/nodejs/node/commit/16c17ddd88)] - **build**: fix Commit Queue failure comment (Antoine du Hamel) [#35599](https://github.com/nodejs/node/pull/35599)
* [[`b7305284e2`](https://github.com/nodejs/node/commit/b7305284e2)] - **build**: gitHub actions: Python 3.9 and actions/setup-python@v2 (Christian Clauss) [#35521](https://github.com/nodejs/node/pull/35521)
* [[`e8fcbc8318`](https://github.com/nodejs/node/commit/e8fcbc8318)] - **build**: fix landed message for multiple commits in commit-queue (Denys Otrishko) [#35226](https://github.com/nodejs/node/pull/35226)
* [[`84c0adefa0`](https://github.com/nodejs/node/commit/84c0adefa0)] - **build**: add Commit Queue actions url to failure comment (Denys Otrishko) [#35206](https://github.com/nodejs/node/pull/35206)
* [[`9c74d4598d`](https://github.com/nodejs/node/commit/9c74d4598d)] - **build**: fuzzer that targets node::LoadEnvironment() (davkor) [#34844](https://github.com/nodejs/node/pull/34844)
* [[`f552e5c251`](https://github.com/nodejs/node/commit/f552e5c251)] - **build**: improved release lint error message (Shelley Vohr) [#35523](https://github.com/nodejs/node/pull/35523)
* [[`7e7afc5186`](https://github.com/nodejs/node/commit/7e7afc5186)] - **crypto**: update certdata to NSS 3.56 (Shelley Vohr) [#35546](https://github.com/nodejs/node/pull/35546)
* [[`b8529a7104`](https://github.com/nodejs/node/commit/b8529a7104)] - **deps**: V8: cherry-pick 3176bfd447a9 (Anna Henningsen) [#35612](https://github.com/nodejs/node/pull/35612)
* [[`0c877762ea`](https://github.com/nodejs/node/commit/0c877762ea)] - **doc**: document Buffer.concat may use internal pool (Andrey Pechkurov) [#35541](https://github.com/nodejs/node/pull/35541)
* [[`6284f0dbc2`](https://github.com/nodejs/node/commit/6284f0dbc2)] - **doc**: use test username instead of real (Pooja D.P) [#35611](https://github.com/nodejs/node/pull/35611)
* [[`78259b6519`](https://github.com/nodejs/node/commit/78259b6519)] - **doc**: add doc for starting ci job via label (Juan José Arboleda) [#35551](https://github.com/nodejs/node/pull/35551)
* [[`41d7500bf9`](https://github.com/nodejs/node/commit/41d7500bf9)] - **doc**: fix unit of size argument of readable.read (Tobias Nießen) [#35051](https://github.com/nodejs/node/pull/35051)
* [[`00eff4a534`](https://github.com/nodejs/node/commit/00eff4a534)] - **doc**: add missing deprecation number (Benjamin Coe) [#35630](https://github.com/nodejs/node/pull/35630)
* [[`9288f9d74b`](https://github.com/nodejs/node/commit/9288f9d74b)] - **doc**: harmonize YAML comments (Antoine du Hamel) [#35575](https://github.com/nodejs/node/pull/35575)
* [[`16f8298170`](https://github.com/nodejs/node/commit/16f8298170)] - **doc**: revise description of process.ppid (Pooja D.P) [#35589](https://github.com/nodejs/node/pull/35589)
* [[`cad86d40de`](https://github.com/nodejs/node/commit/cad86d40de)] - **doc**: add symlink information for process.execpath (Pooja D.P) [#35590](https://github.com/nodejs/node/pull/35590)
* [[`4025fc811d`](https://github.com/nodejs/node/commit/4025fc811d)] - **doc**: add PoojaDurgad as a triager (Pooja D.P) [#35153](https://github.com/nodejs/node/pull/35153)
* [[`809cd07968`](https://github.com/nodejs/node/commit/809cd07968)] - **doc**: document rmdir/recursive deprecation (Benjamin Coe) [#35579](https://github.com/nodejs/node/pull/35579)
* [[`9d1b7ac334`](https://github.com/nodejs/node/commit/9d1b7ac334)] - **doc**: edit fs.md for minor style changes (Rich Trott) [#35505](https://github.com/nodejs/node/pull/35505)
* [[`c1bb364105`](https://github.com/nodejs/node/commit/c1bb364105)] - **doc**: run license builder (Myles Borins) [#35577](https://github.com/nodejs/node/pull/35577)
* [[`970975b588`](https://github.com/nodejs/node/commit/970975b588)] - **doc**: use kbd element in process doc (Rich Trott) [#35584](https://github.com/nodejs/node/pull/35584)
* [[`64ebbddb5f`](https://github.com/nodejs/node/commit/64ebbddb5f)] - **doc**: fixup perf\_hooks (Antoine du Hamel) [#35527](https://github.com/nodejs/node/pull/35527)
* [[`b074717af7`](https://github.com/nodejs/node/commit/b074717af7)] - **doc**: remove incorrect synchronous label (Colin Ihrig) [#35561](https://github.com/nodejs/node/pull/35561)
* [[`ddf13e0df0`](https://github.com/nodejs/node/commit/ddf13e0df0)] - **doc**: make fs.rm()'s force docs consistent (Colin Ihrig) [#35561](https://github.com/nodejs/node/pull/35561)
* [[`4164477b43`](https://github.com/nodejs/node/commit/4164477b43)] - **doc**: simplify wording in tracing APIs doc (Pooja D.P) [#35556](https://github.com/nodejs/node/pull/35556)
* [[`c865b02397`](https://github.com/nodejs/node/commit/c865b02397)] - **doc**: improve SIGINT error text (Rich Trott) [#35558](https://github.com/nodejs/node/pull/35558)
* [[`7d1cdd411f`](https://github.com/nodejs/node/commit/7d1cdd411f)] - **doc**: move package.import content higher (Myles Borins) [#35535](https://github.com/nodejs/node/pull/35535)
* [[`62755b6230`](https://github.com/nodejs/node/commit/62755b6230)] - **doc**: harmonize changes list ordering (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`8cacca0f62`](https://github.com/nodejs/node/commit/8cacca0f62)] - **doc**: changes description must end with a period (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`28c94ca123`](https://github.com/nodejs/node/commit/28c94ca123)] - **doc**: harmonize version list style in YAML comments (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`b3f15b7d89`](https://github.com/nodejs/node/commit/b3f15b7d89)] - **doc**: fix missing PR-URLs in YAML comments (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`02bf73e239`](https://github.com/nodejs/node/commit/02bf73e239)] - **doc**: remove outstanding YAML comment (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`a5552468d2`](https://github.com/nodejs/node/commit/a5552468d2)] - **doc**: harmonize YAML comments style in deprecations.md (Antoine du Hamel) [#35454](https://github.com/nodejs/node/pull/35454)
* [[`863ba4b7da`](https://github.com/nodejs/node/commit/863ba4b7da)] - **doc**: refactor the n-api matrix (Michael Dawson) [#35345](https://github.com/nodejs/node/pull/35345)
* [[`85dc84d1bb`](https://github.com/nodejs/node/commit/85dc84d1bb)] - **doc**: use sentence case for class property (Rich Trott) [#35540](https://github.com/nodejs/node/pull/35540)
* [[`01c9c59c85`](https://github.com/nodejs/node/commit/01c9c59c85)] - **doc**: add history entry for exports patterns (Antoine du Hamel) [#35410](https://github.com/nodejs/node/pull/35410)
* [[`51b988ba0f`](https://github.com/nodejs/node/commit/51b988ba0f)] - **doc**: fix deprecation history (Antoine du Hamel) [#35455](https://github.com/nodejs/node/pull/35455)
* [[`328c624cdf`](https://github.com/nodejs/node/commit/328c624cdf)] - **doc**: fix util.inspect change history (Antoine du Hamel) [#35528](https://github.com/nodejs/node/pull/35528)
* [[`d53cfcd9e7`](https://github.com/nodejs/node/commit/d53cfcd9e7)] - **doc**: improve kbd element rendering (Rich Trott) [#35497](https://github.com/nodejs/node/pull/35497)
* [[`8877430530`](https://github.com/nodejs/node/commit/8877430530)] - **doc**: add aduh95 to collaborators (Antoine du Hamel) [#35542](https://github.com/nodejs/node/pull/35542)
* [[`8cdc59b34c`](https://github.com/nodejs/node/commit/8cdc59b34c)] - **doc**: fix YAML syntax errors (Antoine du Hamel) [#35529](https://github.com/nodejs/node/pull/35529)
* [[`3c90b1a278`](https://github.com/nodejs/node/commit/3c90b1a278)] - **errors**: support possible deletion of globalThis.Error (Michaël Zasso) [#35499](https://github.com/nodejs/node/pull/35499)
* [[`a3c7f8e576`](https://github.com/nodejs/node/commit/a3c7f8e576)] - **fs**: rimraf should not recurse on failure (Benjamin Coe) [#35566](https://github.com/nodejs/node/pull/35566)
* [[`939f8e8bfa`](https://github.com/nodejs/node/commit/939f8e8bfa)] - **fs**: throw rm() validation errors (Colin Ihrig) [#35602](https://github.com/nodejs/node/pull/35602)
* [[`3a401b8695`](https://github.com/nodejs/node/commit/3a401b8695)] - **fs**: update rm/rmdir validation messages (Colin Ihrig) [#35565](https://github.com/nodejs/node/pull/35565)
* [[`937fa5d292`](https://github.com/nodejs/node/commit/937fa5d292)] - **fs**: use validateBoolean() in rm/rmdir validation (Colin Ihrig) [#35565](https://github.com/nodejs/node/pull/35565)
* [[`1ad9aca194`](https://github.com/nodejs/node/commit/1ad9aca194)] - **fs**: remove extraneous assignments in rmdir() (Colin Ihrig) [#35567](https://github.com/nodejs/node/pull/35567)
* [[`1fadcf2163`](https://github.com/nodejs/node/commit/1fadcf2163)] - **fs**: simplify validateRmOptions() error handling (Colin Ihrig) [#35567](https://github.com/nodejs/node/pull/35567)
* [[`8e3b11adb3`](https://github.com/nodejs/node/commit/8e3b11adb3)] - **fs**: use errno constant with ERR\_FS\_EISDIR (Colin Ihrig) [#35563](https://github.com/nodejs/node/pull/35563)
* [[`1610728d7c`](https://github.com/nodejs/node/commit/1610728d7c)] - **(SEMVER-MINOR)** **fs**: add rm method (Ian Sutherland) [#35494](https://github.com/nodejs/node/pull/35494)
* [[`6ff152cc67`](https://github.com/nodejs/node/commit/6ff152cc67)] - **(SEMVER-MINOR)** **http**: allow passing array of key/val into writeHead (Robert Nagy) [#35274](https://github.com/nodejs/node/pull/35274)
* [[`2f1319967c`](https://github.com/nodejs/node/commit/2f1319967c)] - **http**: make response.setTimeout() work (Ben Noordhuis) [#34913](https://github.com/nodejs/node/pull/34913)
* [[`59a2cb5ebb`](https://github.com/nodejs/node/commit/59a2cb5ebb)] - **inspector**: do not hardcode Debugger.CallFrameId in tests (Dmitry Gozman) [#35570](https://github.com/nodejs/node/pull/35570)
* [[`cd0b1365ae`](https://github.com/nodejs/node/commit/cd0b1365ae)] - **lib**: fix readFile flag option typo (Daniil Demidovich) [#35292](https://github.com/nodejs/node/pull/35292)
* [[`70927560f6`](https://github.com/nodejs/node/commit/70927560f6)] - **lib**: change http client path assignment (Yohanan Baruchel) [#35508](https://github.com/nodejs/node/pull/35508)
* [[`fa171dbb7f`](https://github.com/nodejs/node/commit/fa171dbb7f)] - **lib**: use remaining typed arrays from primordials (Michaël Zasso) [#35499](https://github.com/nodejs/node/pull/35499)
* [[`7e8fdd399f`](https://github.com/nodejs/node/commit/7e8fdd399f)] - **lib**: use Number.parseFloat from primordials (Michaël Zasso) [#35499](https://github.com/nodejs/node/pull/35499)
* [[`5d727f0308`](https://github.com/nodejs/node/commit/5d727f0308)] - **lib**: use Number.parseInt from primordials (Michaël Zasso) [#35499](https://github.com/nodejs/node/pull/35499)
* [[`77f1e1ea57`](https://github.com/nodejs/node/commit/77f1e1ea57)] - **lib**: use global Error constructors from primordials (Michaël Zasso) [#35499](https://github.com/nodejs/node/pull/35499)
* [[`30c6b3e809`](https://github.com/nodejs/node/commit/30c6b3e809)] - **lib**: replace String global with primordials (Sebastien Ahkrin) [#35397](https://github.com/nodejs/node/pull/35397)
* [[`ebf3900795`](https://github.com/nodejs/node/commit/ebf3900795)] - **lib**: replace Int8Array global with primordials (Sebastien Ahkrin) [#35397](https://github.com/nodejs/node/pull/35397)
* [[`d6ba4ecc4d`](https://github.com/nodejs/node/commit/d6ba4ecc4d)] - **lib**: replace Int32Array global with primordials (Sebastien Ahkrin) [#35397](https://github.com/nodejs/node/pull/35397)
* [[`f544f7a102`](https://github.com/nodejs/node/commit/f544f7a102)] - **lib**: replace Float64Array global with primordials (Sebastien Ahkrin) [#35397](https://github.com/nodejs/node/pull/35397)
* [[`b82fc409ca`](https://github.com/nodejs/node/commit/b82fc409ca)] - **module**: cjs-module-lexer@0.4.1 big endian fix (Guy Bedford) [#35634](https://github.com/nodejs/node/pull/35634)
* [[`cb2f6ffd3e`](https://github.com/nodejs/node/commit/cb2f6ffd3e)] - **module**: use Wasm CJS lexer when available (Guy Bedford) [#35583](https://github.com/nodejs/node/pull/35583)
* [[`c995242068`](https://github.com/nodejs/node/commit/c995242068)] - **n-api**: support for object freeze/seal (Shelley Vohr) [#35359](https://github.com/nodejs/node/pull/35359)
* [[`4d1d3f454d`](https://github.com/nodejs/node/commit/4d1d3f454d)] - **src**: reduced substring calls (Yash Ladha) [#34808](https://github.com/nodejs/node/pull/34808)
* [[`5946b1ee23`](https://github.com/nodejs/node/commit/5946b1ee23)] - **(SEMVER-MINOR)** **src**: move node\_contextify to modern THROW\_ERR\_\* (James M Snell) [#35470](https://github.com/nodejs/node/pull/35470)
* [[`541082ccd9`](https://github.com/nodejs/node/commit/541082ccd9)] - **(SEMVER-MINOR)** **src**: move node\_process to modern THROW\_ERR\* (James M Snell) [#35472](https://github.com/nodejs/node/pull/35472)
* [[`2e67d650bb`](https://github.com/nodejs/node/commit/2e67d650bb)] - **src**: fix freeing unintialized pointer bug in ParseSoaReply (Aastha Gupta) [#35502](https://github.com/nodejs/node/pull/35502)
* [[`93f947af0a`](https://github.com/nodejs/node/commit/93f947af0a)] - **(SEMVER-MINOR)** **src**: expose v8::Isolate setup callbacks (Shelley Vohr) [#35512](https://github.com/nodejs/node/pull/35512)
* [[`573410fb69`](https://github.com/nodejs/node/commit/573410fb69)] - **(SEMVER-MINOR)** **stream**: multiple stream backports (Robert Nagy) [#34887](https://github.com/nodejs/node/pull/34887)
* [[`775af7af4f`](https://github.com/nodejs/node/commit/775af7af4f)] - **test**: add regression test for v8.getHeapSnapshot() crash (Anna Henningsen) [#35612](https://github.com/nodejs/node/pull/35612)
* [[`3d21792f86`](https://github.com/nodejs/node/commit/3d21792f86)] - **test**: mark test-webcrypto-encrypt-decrypt-aes flaky (James M Snell) [#35587](https://github.com/nodejs/node/pull/35587)
* [[`4a2ba4384b`](https://github.com/nodejs/node/commit/4a2ba4384b)] - **test**: do not use the same EventEmitter instance (Luigi Pinca) [#35560](https://github.com/nodejs/node/pull/35560)
* [[`768529736a`](https://github.com/nodejs/node/commit/768529736a)] - **test**: add ALPNProtocols option to clientOptions (Luigi Pinca) [#35482](https://github.com/nodejs/node/pull/35482)
* [[`3a77d1e208`](https://github.com/nodejs/node/commit/3a77d1e208)] - **test**: adjust comments for upcoming lint rule (Rich Trott) [#35485](https://github.com/nodejs/node/pull/35485)
* [[`2992d0b82c`](https://github.com/nodejs/node/commit/2992d0b82c)] - **tools**: refloat 7 Node.js patches to cpplint.py (Rich Trott) [#35569](https://github.com/nodejs/node/pull/35569)
* [[`a19b320a31`](https://github.com/nodejs/node/commit/a19b320a31)] - **tools**: bump cpplint.py to 1.4.6 (Rich Trott) [#35569](https://github.com/nodejs/node/pull/35569)
* [[`bd344108eb`](https://github.com/nodejs/node/commit/bd344108eb)] - ***Revert*** "**tools**: add missing uv\_setup\_argv() calls" (Beth Griggs) [#35641](https://github.com/nodejs/node/pull/35641)
* [[`e8434d88fe`](https://github.com/nodejs/node/commit/e8434d88fe)] - **tools,test**: enable multiline-comment-style rule in tests (Rich Trott) [#35485](https://github.com/nodejs/node/pull/35485)

<a id="14.13.1"></a>
## 2020-10-07, Version 14.13.1 (Current), @BethGriggs prepared by @danielleadams

### Notable Changes

* **fs**:
  * remove experimental from rmdir recursive (Benjamin Coe) [#35171](https://github.com/nodejs/node/pull/35171)

### Commits

* [[`f36476d9d6`](https://github.com/nodejs/node/commit/f36476d9d6)] - **build**: fix CQ after latest ncu release (Mary Marchini) [#35468](https://github.com/nodejs/node/pull/35468)
* [[`7dc6b2fac7`](https://github.com/nodejs/node/commit/7dc6b2fac7)] - **build**: add support for section ordering (Gabriel Schulhof) [#35272](https://github.com/nodejs/node/pull/35272)
* [[`05e08bdf13`](https://github.com/nodejs/node/commit/05e08bdf13)] - **console**: add Symbol.toStringTag property (Leko) [#35399](https://github.com/nodejs/node/pull/35399)
* [[`a01154e3fd`](https://github.com/nodejs/node/commit/a01154e3fd)] - **crypto**: fix KeyObject garbage collection (Anna Henningsen) [#35481](https://github.com/nodejs/node/pull/35481)
* [[`1f15e34dc8`](https://github.com/nodejs/node/commit/1f15e34dc8)] - **crypto**: set env values in KeyObject Deserialize method (ThakurKarthik) [#35416](https://github.com/nodejs/node/pull/35416)
* [[`85062b3aad`](https://github.com/nodejs/node/commit/85062b3aad)] - **deps**: update llhttp to 2.1.3 (Fedor Indutny) [#35435](https://github.com/nodejs/node/pull/35435)
* [[`a995dd7a89`](https://github.com/nodejs/node/commit/a995dd7a89)] - **doc**: revise introductory child\_process text (Rich Trott) [#35344](https://github.com/nodejs/node/pull/35344)
* [[`6585b161ba`](https://github.com/nodejs/node/commit/6585b161ba)] - **doc**: improve eventLoopUtilization documentation (Andrey Pechkurov) [#35479](https://github.com/nodejs/node/pull/35479)
* [[`f08577c4a6`](https://github.com/nodejs/node/commit/f08577c4a6)] - **doc**: update AUTHORS list (Anna Henningsen) [#35280](https://github.com/nodejs/node/pull/35280)
* [[`91ef862365`](https://github.com/nodejs/node/commit/91ef862365)] - **doc**: mention adding YAML for APIs in PR contributing guide (Denys Otrishko) [#35459](https://github.com/nodejs/node/pull/35459)
* [[`885840b543`](https://github.com/nodejs/node/commit/885840b543)] - **doc**: adopt MDN style for kbd elements (Rich Trott) [#35460](https://github.com/nodejs/node/pull/35460)
* [[`1313c8c33a`](https://github.com/nodejs/node/commit/1313c8c33a)] - **doc**: update sxa's email address to Red Hat from IBM (Stewart X Addison) [#35442](https://github.com/nodejs/node/pull/35442)
* [[`3f95440334`](https://github.com/nodejs/node/commit/3f95440334)] - **doc**: fix conditional exports flag removal version (Antoine du Hamel) [#35428](https://github.com/nodejs/node/pull/35428)
* [[`e40876a5e5`](https://github.com/nodejs/node/commit/e40876a5e5)] - **doc**: specify how to detect EOF (Luigi Pinca) [#35445](https://github.com/nodejs/node/pull/35445)
* [[`541ce17386`](https://github.com/nodejs/node/commit/541ce17386)] - **doc**: add entry to console.timeEnd() changes array (Luigi Pinca) [#35441](https://github.com/nodejs/node/pull/35441)
* [[`38a5715c1a`](https://github.com/nodejs/node/commit/38a5715c1a)] - **doc**: avoid using deprecated connection property (Luigi Pinca) [#35439](https://github.com/nodejs/node/pull/35439)
* [[`862b75d35e`](https://github.com/nodejs/node/commit/862b75d35e)] - **doc**: added details around console.timeEnd changes (Yash Ladha) [#35027](https://github.com/nodejs/node/pull/35027)
* [[`4dbb3a91fe`](https://github.com/nodejs/node/commit/4dbb3a91fe)] - **doc**: copyedit packages.md (Rich Trott) [#35427](https://github.com/nodejs/node/pull/35427)
* [[`368a3ae415`](https://github.com/nodejs/node/commit/368a3ae415)] - **doc**: update contact information for @BethGriggs (Beth Griggs) [#35451](https://github.com/nodejs/node/pull/35451)
* [[`a11ddee8b9`](https://github.com/nodejs/node/commit/a11ddee8b9)] - **doc**: update contact information for richardlau (Richard Lau) [#35450](https://github.com/nodejs/node/pull/35450)
* [[`35b62ef0a7`](https://github.com/nodejs/node/commit/35b62ef0a7)] - **doc**: importable node protocol URLs (Bradley Meck) [#35434](https://github.com/nodejs/node/pull/35434)
* [[`95c62a1dca`](https://github.com/nodejs/node/commit/95c62a1dca)] - **doc**: copyedit esm.md (Rich Trott) [#35414](https://github.com/nodejs/node/pull/35414)
* [[`86f2f83a2f`](https://github.com/nodejs/node/commit/86f2f83a2f)] - **doc**: implement fancier rendering for keys (Rich Trott) [#35400](https://github.com/nodejs/node/pull/35400)
* [[`d6427886b8`](https://github.com/nodejs/node/commit/d6427886b8)] - **doc**: unhide resolver spec (Guy Bedford) [#35358](https://github.com/nodejs/node/pull/35358)
* [[`5a730b5def`](https://github.com/nodejs/node/commit/5a730b5def)] - **doc**: sort md references in ASCII order (Antoine du Hamel) [#35191](https://github.com/nodejs/node/pull/35191)
* [[`ce789f1ca4`](https://github.com/nodejs/node/commit/ce789f1ca4)] - **doc**: use .md extension for internal links (Antoine du Hamel) [#35191](https://github.com/nodejs/node/pull/35191)
* [[`79e3e5008d`](https://github.com/nodejs/node/commit/79e3e5008d)] - **doc**: update example ICU URL (Daijiro Wachi) [#35373](https://github.com/nodejs/node/pull/35373)
* [[`998b24ef17`](https://github.com/nodejs/node/commit/998b24ef17)] - **doc**: align to function signature (anlex N) [#34930](https://github.com/nodejs/node/pull/34930)
* [[`af360ace37`](https://github.com/nodejs/node/commit/af360ace37)] - **doc**: make minor edits for consistency (Rich Trott) [#35377](https://github.com/nodejs/node/pull/35377)
* [[`53e27b3f67`](https://github.com/nodejs/node/commit/53e27b3f67)] - **doc,esm**: add history support info (Antoine du Hamel) [#35395](https://github.com/nodejs/node/pull/35395)
* [[`91b820e939`](https://github.com/nodejs/node/commit/91b820e939)] - **esm**: use "node:" namespace for builtins (Guy Bedford) [#35387](https://github.com/nodejs/node/pull/35387)
* [[`541be526c3`](https://github.com/nodejs/node/commit/541be526c3)] - **fs**: do not throw exception after creating FSReqCallback (Anna Henningsen) [#35487](https://github.com/nodejs/node/pull/35487)
* [[`f29451dece`](https://github.com/nodejs/node/commit/f29451dece)] - **fs**: simplify realpathSync (himself65) [#35413](https://github.com/nodejs/node/pull/35413)
* [[`fcbdb0686d`](https://github.com/nodejs/node/commit/fcbdb0686d)] - **fs**: remove experimental from rmdir recursive (Benjamin Coe) [#35171](https://github.com/nodejs/node/pull/35171)
* [[`e4a4f81d19`](https://github.com/nodejs/node/commit/e4a4f81d19)] - **fs**: use Promise.resolve from primordials (Michaël Zasso) [#35379](https://github.com/nodejs/node/pull/35379)
* [[`8e3a81eed9`](https://github.com/nodejs/node/commit/8e3a81eed9)] - **http2,tls**: store WriteWrap using BaseObjectPtr (Anna Henningsen) [#35488](https://github.com/nodejs/node/pull/35488)
* [[`62ddc77a5b`](https://github.com/nodejs/node/commit/62ddc77a5b)] - **meta**: add nodejs/streams to CODEOWNERS (Matteo Collina) [#35411](https://github.com/nodejs/node/pull/35411)
* [[`23022066ec`](https://github.com/nodejs/node/commit/23022066ec)] - **meta**: add startup team in CODEOWNERS (Joyee Cheung) [#35406](https://github.com/nodejs/node/pull/35406)
* [[`0ac5fa703e`](https://github.com/nodejs/node/commit/0ac5fa703e)] - **module**: update to cjs-module-lexer@0.4.0 (Guy Bedford) [#35501](https://github.com/nodejs/node/pull/35501)
* [[`5c879a97e6`](https://github.com/nodejs/node/commit/5c879a97e6)] - **module**: fix builtin reexport tracing (Guy Bedford) [#35500](https://github.com/nodejs/node/pull/35500)
* [[`f23a0e250c`](https://github.com/nodejs/node/commit/f23a0e250c)] - **module**: refine module type mismatch error cases (Guy Bedford) [#35426](https://github.com/nodejs/node/pull/35426)
* [[`3f62f997a2`](https://github.com/nodejs/node/commit/3f62f997a2)] - **src**: more idiomatic error pattern in node\_wasi (James M Snell) [#35493](https://github.com/nodejs/node/pull/35493)
* [[`392c8815fe`](https://github.com/nodejs/node/commit/392c8815fe)] - **src**: use env-\>ThrowUVException in pipe\_wrap (James M Snell) [#35493](https://github.com/nodejs/node/pull/35493)
* [[`e09f7f023a`](https://github.com/nodejs/node/commit/e09f7f023a)] - **src**: limit GetProcessTitle() result to 1MB (Anna Henningsen) [#35492](https://github.com/nodejs/node/pull/35492)
* [[`fbb9dd9ac9`](https://github.com/nodejs/node/commit/fbb9dd9ac9)] - **src**: fix aliased buffer import warning in env.h (Yash Ladha) [#35436](https://github.com/nodejs/node/pull/35436)
* [[`7bbf8ee256`](https://github.com/nodejs/node/commit/7bbf8ee256)] - **src**: remove invalid ToLocalChecked in EmitBeforeExit (Anna Henningsen) [#35484](https://github.com/nodejs/node/pull/35484)
* [[`5a85d4f2c6`](https://github.com/nodejs/node/commit/5a85d4f2c6)] - **src**: make MakeCallback() check can\_call\_into\_js before getting method (Anna Henningsen) [#35424](https://github.com/nodejs/node/pull/35424)
* [[`4aed176b68`](https://github.com/nodejs/node/commit/4aed176b68)] - **src**: document required else block at src/node\_platform.cc (Juan José Arboleda) [#34688](https://github.com/nodejs/node/pull/34688)
* [[`552ebafd06`](https://github.com/nodejs/node/commit/552ebafd06)] - **test**: update wpt tests for encoding (Daijiro Wachi) [#35330](https://github.com/nodejs/node/pull/35330)
* [[`27cd99b217`](https://github.com/nodejs/node/commit/27cd99b217)] - **test**: improve test coverage for eventtarget (Juan José Arboleda) [#33733](https://github.com/nodejs/node/pull/33733)
* [[`5790c40c32`](https://github.com/nodejs/node/commit/5790c40c32)] - **tools**: add missing uv\_setup\_argv() calls (Anna Henningsen) [#35491](https://github.com/nodejs/node/pull/35491)
* [[`f499302ac0`](https://github.com/nodejs/node/commit/f499302ac0)] - **tools**: fix typo in error message (Antoine du Hamel) [#35417](https://github.com/nodejs/node/pull/35417)
* [[`5f1d792a09`](https://github.com/nodejs/node/commit/5f1d792a09)] - **tools**: update gyp to v0.5.0 (Ujjwal Sharma) [#32698](https://github.com/nodejs/node/pull/32698)
* [[`ad8fce6e61`](https://github.com/nodejs/node/commit/ad8fce6e61)] - **tools**: update gyp to v0.4.0 (Ujjwal Sharma) [#32698](https://github.com/nodejs/node/pull/32698)
* [[`3e75907dea`](https://github.com/nodejs/node/commit/3e75907dea)] - **tools**: update gyp-next to v0.3.0 (Ujjwal Sharma) [#32698](https://github.com/nodejs/node/pull/32698)
* [[`7361a3fd1a`](https://github.com/nodejs/node/commit/7361a3fd1a)] - **tools**: update gyp-next to v0.2.1 (Ujjwal Sharma) [#32698](https://github.com/nodejs/node/pull/32698)
* [[`190d46bdde`](https://github.com/nodejs/node/commit/190d46bdde)] - **tools**: update gyp to 0.2.0 (Ujjwal Sharma) [#32698](https://github.com/nodejs/node/pull/32698)
* [[`166f14ac98`](https://github.com/nodejs/node/commit/166f14ac98)] - **tools**: check links in api docs (Antoine du Hamel) [#35191](https://github.com/nodejs/node/pull/35191)
* [[`a4e5a3af85`](https://github.com/nodejs/node/commit/a4e5a3af85)] - **tools**: exclude gyp from markdown link checker (Michaël Zasso) [#35423](https://github.com/nodejs/node/pull/35423)
* [[`4d29fb56f2`](https://github.com/nodejs/node/commit/4d29fb56f2)] - **tools**: add pythonenv to .gitignore (Michaël Zasso) [#35389](https://github.com/nodejs/node/pull/35389)
* [[`6e9e5c3381`](https://github.com/nodejs/node/commit/6e9e5c3381)] - **tools,doc**: fix broken link in module.html (Rich Trott) [#35446](https://github.com/nodejs/node/pull/35446)

<a id="14.13.0"></a>
## 2020-09-29, Version 14.13.0 (Current), @MylesBorins

### Notable Changes

* [[`19b95a7fa9`](https://github.com/nodejs/node/commit/19b95a7fa9)] - **(SEMVER-MINOR)** **deps**: upgrade to libuv 1.40.0 (Colin Ihrig) [#35333](https://github.com/nodejs/node/pull/35333)
* [[`f551f52f83`](https://github.com/nodejs/node/commit/f551f52f83)] - **(SEMVER-MINOR)** **module**: named exports for CJS via static analysis (Guy Bedford) [#35249](https://github.com/nodejs/node/pull/35249)
* [[`505731871e`](https://github.com/nodejs/node/commit/505731871e)] - **(SEMVER-MINOR)** **module**: exports pattern support (Guy Bedford) [#34718](https://github.com/nodejs/node/pull/34718)
* [[`0d8eaa3942`](https://github.com/nodejs/node/commit/0d8eaa3942)] - **(SEMVER-MINOR)** **src**: allow N-API addon in `AddLinkedBinding()` (Anna Henningsen) [#35301](https://github.com/nodejs/node/pull/35301)

### Commits

* [[`19b95a7fa9`](https://github.com/nodejs/node/commit/19b95a7fa9)] - **(SEMVER-MINOR)** **deps**: upgrade to libuv 1.40.0 (Colin Ihrig) [#35333](https://github.com/nodejs/node/pull/35333)
* [[`353a567235`](https://github.com/nodejs/node/commit/353a567235)] - **deps**: upgrade to c-ares v1.16.1 (Shelley Vohr) [#35324](https://github.com/nodejs/node/pull/35324)
* [[`2e10616d48`](https://github.com/nodejs/node/commit/2e10616d48)] - **doc**: remove http2 non-link anchor tags (Rich Trott) [#35161](https://github.com/nodejs/node/pull/35161)
* [[`02db136c49`](https://github.com/nodejs/node/commit/02db136c49)] - **doc**: alphabetize error list (Rich Trott) [#35219](https://github.com/nodejs/node/pull/35219)
* [[`46a4154cab`](https://github.com/nodejs/node/commit/46a4154cab)] - **doc**: packages docs feedback (Guy Bedford) [#35370](https://github.com/nodejs/node/pull/35370)
* [[`70ad69ba46`](https://github.com/nodejs/node/commit/70ad69ba46)] - **doc**: outline when origin is set to unhandledRejection (Matthieu Larcher) [#35294](https://github.com/nodejs/node/pull/35294)
* [[`010173a4b7`](https://github.com/nodejs/node/commit/010173a4b7)] - **doc**: edit n-api.md for minor improvements (Rich Trott) [#35361](https://github.com/nodejs/node/pull/35361)
* [[`86ac7497e0`](https://github.com/nodejs/node/commit/86ac7497e0)] - **doc**: add history entry for breaking destroy() change (Gil Pedersen) [#35326](https://github.com/nodejs/node/pull/35326)
* [[`857e321baf`](https://github.com/nodejs/node/commit/857e321baf)] - **doc**: set encoding to hex before piping hash (Victor Antonio Barzana Crespo) [#35338](https://github.com/nodejs/node/pull/35338)
* [[`87dfed012c`](https://github.com/nodejs/node/commit/87dfed012c)] - **doc**: add gpg key export directions to releases doc (Danielle Adams) [#35298](https://github.com/nodejs/node/pull/35298)
* [[`1758ac8237`](https://github.com/nodejs/node/commit/1758ac8237)] - **doc**: added version 7 to N-API version matrix (NickNaso) [#35319](https://github.com/nodejs/node/pull/35319)
* [[`5da5d41b1c`](https://github.com/nodejs/node/commit/5da5d41b1c)] - **doc**: refine require/import conditions constraints (Guy Bedford) [#35311](https://github.com/nodejs/node/pull/35311)
* [[`482ce6ce1d`](https://github.com/nodejs/node/commit/482ce6ce1d)] - **doc**: improve N-API string-to-native doc (Gabriel Schulhof) [#35322](https://github.com/nodejs/node/pull/35322)
* [[`6dc6dadfc6`](https://github.com/nodejs/node/commit/6dc6dadfc6)] - **doc**: avoid referring to C array size (Tobias Nießen) [#35300](https://github.com/nodejs/node/pull/35300)
* [[`0a847ca729`](https://github.com/nodejs/node/commit/0a847ca729)] - **doc**: update napi\_make\_callback documentation (Gerhard Stoebich) [#35321](https://github.com/nodejs/node/pull/35321)
* [[`a8d3a7f742`](https://github.com/nodejs/node/commit/a8d3a7f742)] - **doc**: put landing specifics in details tag (Rich Trott) [#35296](https://github.com/nodejs/node/pull/35296)
* [[`dd530364d0`](https://github.com/nodejs/node/commit/dd530364d0)] - **doc**: fixup lutimes metadata (Anna Henningsen) [#35328](https://github.com/nodejs/node/pull/35328)
* [[`d7282c0ae3`](https://github.com/nodejs/node/commit/d7282c0ae3)] - **doc**: edit subpath export patterns introduction (Rich Trott) [#35254](https://github.com/nodejs/node/pull/35254)
* [[`1d1ce1fc2c`](https://github.com/nodejs/node/commit/1d1ce1fc2c)] - **doc**: document support for package.json fields (Antoine du HAMEL) [#34970](https://github.com/nodejs/node/pull/34970)
* [[`ef0d2ef5a2`](https://github.com/nodejs/node/commit/ef0d2ef5a2)] - **doc**: move package config docs to separate page (Antoine du HAMEL) [#34748](https://github.com/nodejs/node/pull/34748)
* [[`b9d767c4d5`](https://github.com/nodejs/node/commit/b9d767c4d5)] - **doc**: change type of child\_process.signalCode to string (Linn Dahlgren) [#35223](https://github.com/nodejs/node/pull/35223)
* [[`b4514d464d`](https://github.com/nodejs/node/commit/b4514d464d)] - **doc**: replace "this guide" link text with guide title (Rich Trott) [#35283](https://github.com/nodejs/node/pull/35283)
* [[`1893449724`](https://github.com/nodejs/node/commit/1893449724)] - **doc**: revise dependency redirection text in policy.md (Rich Trott) [#35276](https://github.com/nodejs/node/pull/35276)
* [[`0c4540b050`](https://github.com/nodejs/node/commit/0c4540b050)] - **doc**: fix heading space bug in assert.md (Thomas Hunter II) [#35310](https://github.com/nodejs/node/pull/35310)
* [[`ec6b78ae73`](https://github.com/nodejs/node/commit/ec6b78ae73)] - **doc**: add `socket.readyState` (Clark Kozak) [#35262](https://github.com/nodejs/node/pull/35262)
* [[`2a4ae0926d`](https://github.com/nodejs/node/commit/2a4ae0926d)] - **doc**: update crypto.createSecretKey accepted types (Filip Skokan) [#35246](https://github.com/nodejs/node/pull/35246)
* [[`c09f3dc2f3`](https://github.com/nodejs/node/commit/c09f3dc2f3)] - **doc**: put release script specifics in details (Myles Borins) [#35260](https://github.com/nodejs/node/pull/35260)
* [[`99a79e32a6`](https://github.com/nodejs/node/commit/99a79e32a6)] - **fs**: fix fs.promises.writeFile with typed arrays (Michaël Zasso) [#35376](https://github.com/nodejs/node/pull/35376)
* [[`ab7d0e92b1`](https://github.com/nodejs/node/commit/ab7d0e92b1)] - **meta**: update module pages in CODEOWNERS (Antoine du Hamel) [#34932](https://github.com/nodejs/node/pull/34932)
* [[`f551f52f83`](https://github.com/nodejs/node/commit/f551f52f83)] - **(SEMVER-MINOR)** **module**: named exports for CJS via static analysis (Guy Bedford) [#35249](https://github.com/nodejs/node/pull/35249)
* [[`505731871e`](https://github.com/nodejs/node/commit/505731871e)] - **(SEMVER-MINOR)** **module**: exports pattern support (Guy Bedford) [#34718](https://github.com/nodejs/node/pull/34718)
* [[`68ea7f5560`](https://github.com/nodejs/node/commit/68ea7f5560)] - **module**: fix crash on multiline named cjs imports (Christoph Tavan) [#35275](https://github.com/nodejs/node/pull/35275)
* [[`0f4ecaa741`](https://github.com/nodejs/node/commit/0f4ecaa741)] - **repl**: standardize Control key indications (Rich Trott) [#35270](https://github.com/nodejs/node/pull/35270)
* [[`1e1cb94e69`](https://github.com/nodejs/node/commit/1e1cb94e69)] - **src**: fix incorrect SIGSEGV handling in NODE\_USE\_V8\_WASM\_TRAP\_HANDLER (Anatoly Korniltsev) [#35282](https://github.com/nodejs/node/pull/35282)
* [[`0d8eaa3942`](https://github.com/nodejs/node/commit/0d8eaa3942)] - **(SEMVER-MINOR)** **src**: allow N-API addon in `AddLinkedBinding()` (Anna Henningsen) [#35301](https://github.com/nodejs/node/pull/35301)
* [[`f2635b317e`](https://github.com/nodejs/node/commit/f2635b317e)] - **test**: replace annonymous functions with arrow (Pooja D.P) [#34921](https://github.com/nodejs/node/pull/34921)
* [[`d7c28c9243`](https://github.com/nodejs/node/commit/d7c28c9243)] - **test,child_process**: add tests for signalCode value (Rich Trott) [#35327](https://github.com/nodejs/node/pull/35327)
* [[`80eb22185e`](https://github.com/nodejs/node/commit/80eb22185e)] - **tools**: update ESLint to 7.10.0 (Colin Ihrig) [#35366](https://github.com/nodejs/node/pull/35366)
* [[`7f355735df`](https://github.com/nodejs/node/commit/7f355735df)] - **tools**: ignore build folder when checking links (Ash Cripps) [#35315](https://github.com/nodejs/node/pull/35315)
* [[`c5d27e1e29`](https://github.com/nodejs/node/commit/c5d27e1e29)] - **tools,doc**: enforce alphabetical order for md refs (Antoine du Hamel) [#35244](https://github.com/nodejs/node/pull/35244)
* [[`9d91842a9d`](https://github.com/nodejs/node/commit/9d91842a9d)] - **tools,doc**: upgrade dependencies (Antoine du Hamel) [#35244](https://github.com/nodejs/node/pull/35244)

<a id="14.12.0"></a>
## 2020-09-22, Version 14.12.0 (Current), @ruyadorno

### Notable changes

* **deps**:
  * update to uvwasi 0.0.11 (Colin Ihrig) [#35104](https://github.com/nodejs/node/pull/35104)
* **n-api**:
  * create N-API version 7 (Gabriel Schulhof) [#35199](https://github.com/nodejs/node/pull/35199)
  * add more property defaults (Gerhard Stoebich) [#35214](https://github.com/nodejs/node/pull/35214)

### Commits
* [[`5229ffadcf`](https://github.com/nodejs/node/commit/5229ffadcf)] - **buffer**: adjust validation to account for buffer.kMaxLength (Anna Henningsen) [#35134](https://github.com/nodejs/node/pull/35134)
* [[`3d78686987`](https://github.com/nodejs/node/commit/3d78686987)] - **build**: increase API requests for stale action (Phillip Johnsen) [#35235](https://github.com/nodejs/node/pull/35235)
* [[`f2cc1c22c1`](https://github.com/nodejs/node/commit/f2cc1c22c1)] - **build**: filter issues & PRs to auto close by matching on stalled label (Phillip Johnsen) [#35159](https://github.com/nodejs/node/pull/35159)
* [[`f3c45a1cef`](https://github.com/nodejs/node/commit/f3c45a1cef)] - ***Revert*** "**build**: require "allow edits" to be checked" (Rich Trott) [#35094](https://github.com/nodejs/node/pull/35094)
* [[`1bb0ed3c6a`](https://github.com/nodejs/node/commit/1bb0ed3c6a)] - **crypto**: improve invalid arg type message for randomInt() (Rich Trott) [#35089](https://github.com/nodejs/node/pull/35089)
* [[`3573545315`](https://github.com/nodejs/node/commit/3573545315)] - **crypto**: improve randomInt out-of-range error message (Rich Trott) [#35088](https://github.com/nodejs/node/pull/35088)
* [[`d4389b59b4`](https://github.com/nodejs/node/commit/d4389b59b4)] - **deps**: update to uvwasi 0.0.11 (Colin Ihrig) [#35104](https://github.com/nodejs/node/pull/35104)
* [[`836680a4ea`](https://github.com/nodejs/node/commit/836680a4ea)] - **doc**: use present tense in error messages (Rich Trott) [#35164](https://github.com/nodejs/node/pull/35164)
* [[`b860a7f61c`](https://github.com/nodejs/node/commit/b860a7f61c)] - **doc**: standardize on \_backward\_ (Rich Trott) [#35243](https://github.com/nodejs/node/pull/35243)
* [[`d82dd0c773`](https://github.com/nodejs/node/commit/d82dd0c773)] - **doc**: revise stability section of values doc (Rich Trott) [#35242](https://github.com/nodejs/node/pull/35242)
* [[`2bc335dcf6`](https://github.com/nodejs/node/commit/2bc335dcf6)] - **doc**: clarify napi\_property\_attributes text (Rich Trott) [#35253](https://github.com/nodejs/node/pull/35253)
* [[`b62d9b47be`](https://github.com/nodejs/node/commit/b62d9b47be)] - **doc**: remove excessive formatting in dgram.md (Rich Trott) [#35234](https://github.com/nodejs/node/pull/35234)
* [[`b9161f408f`](https://github.com/nodejs/node/commit/b9161f408f)] - **doc**: sort repl references in ASCII order (Rich Trott) [#35230](https://github.com/nodejs/node/pull/35230)
* [[`d195d20dbc`](https://github.com/nodejs/node/commit/d195d20dbc)] - **doc**: relax prohibition on personal pronouns (Rich Trott) [#34353](https://github.com/nodejs/node/pull/34353)
* [[`6119e9511c`](https://github.com/nodejs/node/commit/6119e9511c)] - **doc**: clarify use of NAPI\_EXPERIMENTAL (Michael Dawson) [#35195](https://github.com/nodejs/node/pull/35195)
* [[`6d4ab36175`](https://github.com/nodejs/node/commit/6d4ab36175)] - **doc**: update attributes used by n-api samples (#35220) (Gerhard Stoebich)
* [[`7610fe500e`](https://github.com/nodejs/node/commit/7610fe500e)] - **doc**: add issue labels sections to release guide (Michaël Zasso) [#35224](https://github.com/nodejs/node/pull/35224)
* [[`2308be06b3`](https://github.com/nodejs/node/commit/2308be06b3)] - **doc**: fix header level for error code (Rich Trott) [#35219](https://github.com/nodejs/node/pull/35219)
* [[`64ac5c68aa`](https://github.com/nodejs/node/commit/64ac5c68aa)] - **doc**: fix small grammatical issues in timers.md (Turner Jabbour) [#35203](https://github.com/nodejs/node/pull/35203)
* [[`92a14d3c72`](https://github.com/nodejs/node/commit/92a14d3c72)] - **doc**: add technical values document (Michael Dawson) [#35145](https://github.com/nodejs/node/pull/35145)
* [[`dbfd3b261d`](https://github.com/nodejs/node/commit/dbfd3b261d)] - **doc**: remove "end user" (Rich Trott) [#35200](https://github.com/nodejs/node/pull/35200)
* [[`c1c93a6cde`](https://github.com/nodejs/node/commit/c1c93a6cde)] - **doc**: use command-line/command line consistently (Rich Trott) [#35198](https://github.com/nodejs/node/pull/35198)
* [[`70ec369b76`](https://github.com/nodejs/node/commit/70ec369b76)] - **doc**: replace "you should do X" with "do X" (Rich Trott) [#35194](https://github.com/nodejs/node/pull/35194)
* [[`e5fffe2f8f`](https://github.com/nodejs/node/commit/e5fffe2f8f)] - **doc**: fix missing word in dgram.md (Tom Atkinson) [#35231](https://github.com/nodejs/node/pull/35231)
* [[`c1e16d15dd`](https://github.com/nodejs/node/commit/c1e16d15dd)] - **doc**: fix deprecation documentation inconsistencies (Antoine du HAMEL) [#35082](https://github.com/nodejs/node/pull/35082)
* [[`910deff2de`](https://github.com/nodejs/node/commit/910deff2de)] - **doc**: fix broken link in crypto.md (Rich Trott) [#35181](https://github.com/nodejs/node/pull/35181)
* [[`066148d229`](https://github.com/nodejs/node/commit/066148d229)] - **doc**: remove problematic auto-linking of curl man pages (Rich Trott) [#35174](https://github.com/nodejs/node/pull/35174)
* [[`aea3f77c8d`](https://github.com/nodejs/node/commit/aea3f77c8d)] - **doc**: update eventLoopUtilization documentation (Anna Henningsen) [#35155](https://github.com/nodejs/node/pull/35155)
* [[`32d1731ae1`](https://github.com/nodejs/node/commit/32d1731ae1)] - **doc**: update process.release (schamberg97) [#35167](https://github.com/nodejs/node/pull/35167)
* [[`176e9e4054`](https://github.com/nodejs/node/commit/176e9e4054)] - **doc**: fix broken links in modules.md (Rich Trott) [#35182](https://github.com/nodejs/node/pull/35182)
* [[`dc4c5696da`](https://github.com/nodejs/node/commit/dc4c5696da)] - **doc**: add missing copyFile change history (Shelley Vohr) [#35056](https://github.com/nodejs/node/pull/35056)
* [[`e8d479ed67`](https://github.com/nodejs/node/commit/e8d479ed67)] - **doc**: perform cleanup on security-release-process.md (Rich Trott) [#35154](https://github.com/nodejs/node/pull/35154)
* [[`99785e48ea`](https://github.com/nodejs/node/commit/99785e48ea)] - **doc**: fix minor punctuation issue in path.md (Amila Welihinda) [#35127](https://github.com/nodejs/node/pull/35127)
* [[`ae85228e54`](https://github.com/nodejs/node/commit/ae85228e54)] - **doc**: perform minor cleanup on cli.md (Rich Trott) [#35152](https://github.com/nodejs/node/pull/35152)
* [[`e4105140e7`](https://github.com/nodejs/node/commit/e4105140e7)] - **doc**: improve table accessibility (Rich Trott) [#35146](https://github.com/nodejs/node/pull/35146)
* [[`7dbcd24541`](https://github.com/nodejs/node/commit/7dbcd24541)] - **doc**: fix left nav color contrast (Rich Trott) [#35141](https://github.com/nodejs/node/pull/35141)
* [[`331142ca25`](https://github.com/nodejs/node/commit/331142ca25)] - **doc**: update contact info for Ash Cripps (Ash Cripps) [#35139](https://github.com/nodejs/node/pull/35139)
* [[`df70861863`](https://github.com/nodejs/node/commit/df70861863)] - **doc**: simplify circular dependencies text in modules.md (Rich Trott) [#35126](https://github.com/nodejs/node/pull/35126)
* [[`f4e714aaf5`](https://github.com/nodejs/node/commit/f4e714aaf5)] - **doc**: update my email address (Michael Dawson) [#35121](https://github.com/nodejs/node/pull/35121)
* [[`46b9f4b376`](https://github.com/nodejs/node/commit/46b9f4b376)] - **doc**: add missing changes entry for breakEvalOnSigint REPL option (Anna Henningsen) [#35143](https://github.com/nodejs/node/pull/35143)
* [[`122edad98b`](https://github.com/nodejs/node/commit/122edad98b)] - **doc**: update security process (Michael Dawson) [#35107](https://github.com/nodejs/node/pull/35107)
* [[`aa93c1c22d`](https://github.com/nodejs/node/commit/aa93c1c22d)] - **doc**: fix broken link in perf\_hooks.md (Rich Trott) [#35113](https://github.com/nodejs/node/pull/35113)
* [[`5c8d2083c5`](https://github.com/nodejs/node/commit/5c8d2083c5)] - **doc**: fix broken link in http2.md (Rich Trott) [#35112](https://github.com/nodejs/node/pull/35112)
* [[`f4e958fc0c`](https://github.com/nodejs/node/commit/f4e958fc0c)] - **doc**: fix broken link in fs.md (Rich Trott) [#35111](https://github.com/nodejs/node/pull/35111)
* [[`79c6d92e49`](https://github.com/nodejs/node/commit/79c6d92e49)] - **doc**: fix broken links in deprecations.md (Rich Trott) [#35109](https://github.com/nodejs/node/pull/35109)
* [[`93e4d545d8`](https://github.com/nodejs/node/commit/93e4d545d8)] - **doc**: add note about path.basename on Windows (Tobias Nießen) [#35065](https://github.com/nodejs/node/pull/35065)
* [[`0a2610a7aa`](https://github.com/nodejs/node/commit/0a2610a7aa)] - **doc**: avoid double-while sentence in perf\_hooks.md (Rich Trott) [#35078](https://github.com/nodejs/node/pull/35078)
* [[`1cf9934e4e`](https://github.com/nodejs/node/commit/1cf9934e4e)] - **doc**: make minor improvements to module.md (Rich Trott) [#35083](https://github.com/nodejs/node/pull/35083)
* [[`fb89be63be`](https://github.com/nodejs/node/commit/fb89be63be)] - **doc**: use correct Error type for EventEmitter.defaultMaxListener (Rich Trott) [#35069](https://github.com/nodejs/node/pull/35069)
* [[`b75822dd93`](https://github.com/nodejs/node/commit/b75822dd93)] - **doc,test**: specify and test CLI option precedence rules (Anna Henningsen) [#35106](https://github.com/nodejs/node/pull/35106)
* [[`4bde865145`](https://github.com/nodejs/node/commit/4bde865145)] - **errors**: simplify ERR\_REQUIRE\_ESM message generation (Rich Trott) [#35123](https://github.com/nodejs/node/pull/35123)
* [[`6e622d6337`](https://github.com/nodejs/node/commit/6e622d6337)] - **esm**: better package.json parser errors (Guy Bedford) [#35117](https://github.com/nodejs/node/pull/35117)
* [[`beb75bd031`](https://github.com/nodejs/node/commit/beb75bd031)] - **fs**: loosen validation to allow objects with an own toString function (Jordan Harband) [#34993](https://github.com/nodejs/node/pull/34993)
* [[`8ea28536d0`](https://github.com/nodejs/node/commit/8ea28536d0)] - **http**: only set keep-alive when not exists (atian25@qq.com) [#35138](https://github.com/nodejs/node/pull/35138)
* [[`977b0ed865`](https://github.com/nodejs/node/commit/977b0ed865)] - **http**: allow Content-Length header for 304 responses (Arnaud Lefebvre) [#34835](https://github.com/nodejs/node/pull/34835)
* [[`d8b57140b4`](https://github.com/nodejs/node/commit/d8b57140b4)] - **https**: set requestTimeout default to 0 (Robert Nagy) [#35264](https://github.com/nodejs/node/pull/35264)
* [[`12f2934224`](https://github.com/nodejs/node/commit/12f2934224)] - **meta**: add links to OpenJSF Slack (Mary Marchini) [#35128](https://github.com/nodejs/node/pull/35128)
* [[`f21a5c6200`](https://github.com/nodejs/node/commit/f21a5c6200)] - **meta**: update my collab entry (devsnek) [#35160](https://github.com/nodejs/node/pull/35160)
* [[`76e24f9aa9`](https://github.com/nodejs/node/commit/76e24f9aa9)] - **module**: use isURLInstance instead of instanceof (Antoine du HAMEL) [#34951](https://github.com/nodejs/node/pull/34951)
* [[`314483cd09`](https://github.com/nodejs/node/commit/314483cd09)] - **module**: fix specifier resolution option value (himself65) [#35098](https://github.com/nodejs/node/pull/35098)
* [[`ca1181615e`](https://github.com/nodejs/node/commit/ca1181615e)] - **(SEMVER-MINOR)** **n-api**: create N-API version 7 (Gabriel Schulhof) [#35199](https://github.com/nodejs/node/pull/35199)
* [[`7f3b2b2a1f`](https://github.com/nodejs/node/commit/7f3b2b2a1f)] - **(SEMVER-MINOR)** **n-api**: add more property defaults (Gerhard Stoebich) [#35214](https://github.com/nodejs/node/pull/35214)
* [[`4f4faa8e3c`](https://github.com/nodejs/node/commit/4f4faa8e3c)] - **readline**: fix key name for function keys with modifiers (DrunkenPoney) [#35268](https://github.com/nodejs/node/pull/35268)
* [[`e29e2daf4c`](https://github.com/nodejs/node/commit/e29e2daf4c)] - **test**: improve assertions in pummel/test-timers (Rich Trott) [#35216](https://github.com/nodejs/node/pull/35216)
* [[`8357b56984`](https://github.com/nodejs/node/commit/8357b56984)] - **test**: add wasi readdir() test (Colin Ihrig) [#35202](https://github.com/nodejs/node/pull/35202)
* [[`49da459ce6`](https://github.com/nodejs/node/commit/49da459ce6)] - **test**: improve pummel/test-timers.js (Rich Trott) [#35175](https://github.com/nodejs/node/pull/35175)
* [[`379c5cefd7`](https://github.com/nodejs/node/commit/379c5cefd7)] - **test**: revise test-policy-integrity (Rich Trott) [#35101](https://github.com/nodejs/node/pull/35101)
* [[`6627c1f8e4`](https://github.com/nodejs/node/commit/6627c1f8e4)] - **test**: remove setMaxListeners in test-crypto-random (Tobias Nießen) [#35079](https://github.com/nodejs/node/pull/35079)
* [[`bc38485fb1`](https://github.com/nodejs/node/commit/bc38485fb1)] - **test**: fix comment about DNS lookup test (Tobias Nießen) [#35080](https://github.com/nodejs/node/pull/35080)
* [[`9faaa659b2`](https://github.com/nodejs/node/commit/9faaa659b2)] - **test**: separate the test fixtures between ICU and URL (Leko) [#35077](https://github.com/nodejs/node/pull/35077)
* [[`25f93f3ec3`](https://github.com/nodejs/node/commit/25f93f3ec3)] - **test**: add more valid results to test-trace-atomics-wait (Anna Henningsen) [#35066](https://github.com/nodejs/node/pull/35066)
* [[`0a97f44b50`](https://github.com/nodejs/node/commit/0a97f44b50)] - **tools**: update ESLint to 7.9.0 (Colin Ihrig) [#35170](https://github.com/nodejs/node/pull/35170)

<a id="14.11.0"></a>
## 2020-09-15, Version 14.11.0 (Current), @richardlau

### Notable Changes

This is a security release.

Vulnerabilities fixed:

* **CVE-2020-8251**: Denial of Service by resource exhaustion CWE-400 due to unfinished HTTP/1.1 requests (Critical).
* **CVE-2020-8201**: HTTP Request Smuggling due to CR-to-Hyphen conversion (High).

### Commits

* [[`dd828376a0`](https://github.com/nodejs/node/commit/dd828376a0)] - **deps**: update llhttp to 2.1.2 (Fedor Indutny) [nodejs-private/node-private#215](https://github.com/nodejs-private/node-private/pull/215)
* [[`753f3b247a`](https://github.com/nodejs/node/commit/753f3b247a)] - **http**: add requestTimeout (Matteo Collina, Paolo Insogna, Robert Nagy) [nodejs-private/node-private#208](https://github.com/nodejs-private/node-private/pull/208)

<a id="14.10.1"></a>
## 2020-09-10, Version 14.10.1 (Current), @richardlau

### Notable Changes

Node.js 14.10.0 included a streams regression with async generators
and a docs rendering regression that are being fixed in this release.

### Commits

* [[`3c92f93b44`](https://github.com/nodejs/node/commit/3c92f93b44)] - **doc**: restore color for visited links (Rich Trott) [#35108](https://github.com/nodejs/node/pull/35108)
* [[`0f94c6b4e4`](https://github.com/nodejs/node/commit/0f94c6b4e4)] - ***Revert*** "**stream**: simpler and faster Readable async iterator" (Richard Lau)

<a id="14.10.0"></a>
## 2020-09-08, Version 14.10.0 (Current), @richardlau

### Notable Changes

* [[`2ab33c58ae`](https://github.com/nodejs/node/commit/2ab33c58ae)] - **(SEMVER-MINOR)** **buffer**: also alias BigUInt methods (Anna Henningsen) [#34960](https://github.com/nodejs/node/pull/34960)
* [[`44d89a9faa`](https://github.com/nodejs/node/commit/44d89a9faa)] - **(SEMVER-MINOR)** **crypto**: add randomInt function (Oli Lalonde) [#34600](https://github.com/nodejs/node/pull/34600)
* [[`8aac42caf2`](https://github.com/nodejs/node/commit/8aac42caf2)] - **(SEMVER-MINOR)** **perf_hooks**: add idleTime and event loop util (Trevor Norris) [#34938](https://github.com/nodejs/node/pull/34938)
* [[`4bb40078da`](https://github.com/nodejs/node/commit/4bb40078da)] - **(SEMVER-MINOR)** **stream**: simpler and faster Readable async iterator (Robert Nagy) [#34035](https://github.com/nodejs/node/pull/34035)
* [[`ffae5f3809`](https://github.com/nodejs/node/commit/ffae5f3809)] - **(SEMVER-MINOR)** **stream**: save error in state (Robert Nagy) [#34103](https://github.com/nodejs/node/pull/34103)

### Commits

* [[`1fdfaa578f`](https://github.com/nodejs/node/commit/1fdfaa578f)] - **bootstrap**: correct --frozen-intrinsics override fix (Guy Bedford) [#35041](https://github.com/nodejs/node/pull/35041)
* [[`2ab33c58ae`](https://github.com/nodejs/node/commit/2ab33c58ae)] - **(SEMVER-MINOR)** **buffer**: also alias BigUInt methods (Anna Henningsen) [#34960](https://github.com/nodejs/node/pull/34960)
* [[`1be6956ee0`](https://github.com/nodejs/node/commit/1be6956ee0)] - **build**: require "allow edits" to be checked (Jordan Harband) [#35002](https://github.com/nodejs/node/pull/35002)
* [[`7b7299012e`](https://github.com/nodejs/node/commit/7b7299012e)] - **build**: comment about auto close when stalled via with github action (Phillip Johnsen) [#34555](https://github.com/nodejs/node/pull/34555)
* [[`d6c796b4ab`](https://github.com/nodejs/node/commit/d6c796b4ab)] - **build**: close stalled issues and PRs with github action (Phillip Johnsen) [#34555](https://github.com/nodejs/node/pull/34555)
* [[`46766a10df`](https://github.com/nodejs/node/commit/46766a10df)] - **build**: use autorebase option for git node land (Denys Otrishko) [#34969](https://github.com/nodejs/node/pull/34969)
* [[`7afb67f491`](https://github.com/nodejs/node/commit/7afb67f491)] - **build**: use latest node-core-utils from npm (Denys Otrishko) [#34969](https://github.com/nodejs/node/pull/34969)
* [[`d06e158253`](https://github.com/nodejs/node/commit/d06e158253)] - **build**: add support for build on arm64 (Evan Lucas) [#34238](https://github.com/nodejs/node/pull/34238)
* [[`755f9e4bc8`](https://github.com/nodejs/node/commit/755f9e4bc8)] - **build,deps**: add gen-openssl target (Evan Lucas) [#34642](https://github.com/nodejs/node/pull/34642)
* [[`178a740caf`](https://github.com/nodejs/node/commit/178a740caf)] - **crypto**: simplify KeyObject constructor (Rich Trott) [#35064](https://github.com/nodejs/node/pull/35064)
* [[`a12d92c97b`](https://github.com/nodejs/node/commit/a12d92c97b)] - **crypto**: fix randomInt range check (Tobias Nießen) [#35052](https://github.com/nodejs/node/pull/35052)
* [[`6d0d5b2ec2`](https://github.com/nodejs/node/commit/6d0d5b2ec2)] - **crypto**: align parameter names with documentation (Rich Trott) [#35054](https://github.com/nodejs/node/pull/35054)
* [[`44d89a9faa`](https://github.com/nodejs/node/commit/44d89a9faa)] - **(SEMVER-MINOR)** **crypto**: add randomInt function (Oli Lalonde) [#34600](https://github.com/nodejs/node/pull/34600)
* [[`791a85b880`](https://github.com/nodejs/node/commit/791a85b880)] - **deps**: V8: cherry-pick 6be2f6e26e8d (Benjamin Coe) [#35055](https://github.com/nodejs/node/pull/35055)
* [[`96ae05a770`](https://github.com/nodejs/node/commit/96ae05a770)] - **deps**: V8: backport 3f071e3e7e15 (Milad Farazmand) [#35036](https://github.com/nodejs/node/pull/35036)
* [[`90f9348297`](https://github.com/nodejs/node/commit/90f9348297)] - **deps**: update brotli to v1.0.9 (Anna Henningsen) [#34937](https://github.com/nodejs/node/pull/34937)
* [[`f1fcd6646d`](https://github.com/nodejs/node/commit/f1fcd6646d)] - **deps**: add openssl support for arm64 (Evan Lucas) [#34238](https://github.com/nodejs/node/pull/34238)
* [[`bbf7b925a2`](https://github.com/nodejs/node/commit/bbf7b925a2)] - **doc**: use present tense in events.md (Rich Trott) [#35068](https://github.com/nodejs/node/pull/35068)
* [[`f6b2286e12`](https://github.com/nodejs/node/commit/f6b2286e12)] - **doc**: change stablility-2 color for accessibility (Rich Trott) [#35061](https://github.com/nodejs/node/pull/35061)
* [[`8044533e87`](https://github.com/nodejs/node/commit/8044533e87)] - **doc**: add link to safe integer definition (Tobias Nießen) [#35049](https://github.com/nodejs/node/pull/35049)
* [[`f03a4d78a2`](https://github.com/nodejs/node/commit/f03a4d78a2)] - **doc**: format exponents better (Tobias Nießen) [#35050](https://github.com/nodejs/node/pull/35050)
* [[`1a9ca52716`](https://github.com/nodejs/node/commit/1a9ca52716)] - **doc**: add ESM examples in `module` API doc page (Antoine du HAMEL) [#34875](https://github.com/nodejs/node/pull/34875)
* [[`0ac7d5423f`](https://github.com/nodejs/node/commit/0ac7d5423f)] - **doc**: add deprecated badge to legacy URL methods (Antoine du HAMEL) [#34931](https://github.com/nodejs/node/pull/34931)
* [[`a08e853edc`](https://github.com/nodejs/node/commit/a08e853edc)] - **doc**: spruce up user journey to local docs browsing (Derek Lewis) [#34986](https://github.com/nodejs/node/pull/34986)
* [[`83a3e3b681`](https://github.com/nodejs/node/commit/83a3e3b681)] - **doc**: update syntax highlighting color for accessibility (Rich Trott) [#35063](https://github.com/nodejs/node/pull/35063)
* [[`5bd0e0803d`](https://github.com/nodejs/node/commit/5bd0e0803d)] - **doc**: fix incorrect URL in cli.md (Rich Trott) [#35043](https://github.com/nodejs/node/pull/35043)
* [[`28e89f6766`](https://github.com/nodejs/node/commit/28e89f6766)] - **doc**: remove style for empty links (Antoine du HAMEL) [#35034](https://github.com/nodejs/node/pull/35034)
* [[`cdc1198a62`](https://github.com/nodejs/node/commit/cdc1198a62)] - **doc**: fix certificate display in tls doc (Rich Trott) [#35032](https://github.com/nodejs/node/pull/35032)
* [[`72d03cd802`](https://github.com/nodejs/node/commit/72d03cd802)] - **doc**: remove duplicate error code entry (Rich Trott) [#35031](https://github.com/nodejs/node/pull/35031)
* [[`680782ea64`](https://github.com/nodejs/node/commit/680782ea64)] - **doc**: use consistent header typography (Rich Trott) [#35030](https://github.com/nodejs/node/pull/35030)
* [[`1ae674c67a`](https://github.com/nodejs/node/commit/1ae674c67a)] - **doc**: fix malformed hashes in assert.md (Rich Trott) [#35028](https://github.com/nodejs/node/pull/35028)
* [[`c3a3cb69aa`](https://github.com/nodejs/node/commit/c3a3cb69aa)] - **doc**: fix a typo of microtaskMode (Shigma) [#34980](https://github.com/nodejs/node/pull/34980)
* [[`a846a9f116`](https://github.com/nodejs/node/commit/a846a9f116)] - **doc**: change 'be will' to 'will be' (Victory Osikwemhe) [#34999](https://github.com/nodejs/node/pull/34999)
* [[`593236ad33`](https://github.com/nodejs/node/commit/593236ad33)] - **doc**: change color contrast for accessibility (Rich Trott) [#35047](https://github.com/nodejs/node/pull/35047)
* [[`8c207c67d1`](https://github.com/nodejs/node/commit/8c207c67d1)] - **doc**: refactor deprecation anchors (Antoine du HAMEL) [#34955](https://github.com/nodejs/node/pull/34955)
* [[`cc0aaf2384`](https://github.com/nodejs/node/commit/cc0aaf2384)] - **doc**: error code fix in resolver spec (Guy Bedford) [#34998](https://github.com/nodejs/node/pull/34998)
* [[`a4201843e7`](https://github.com/nodejs/node/commit/a4201843e7)] - **doc**: use period consistently in man page (Rich Trott) [#34939](https://github.com/nodejs/node/pull/34939)
* [[`f1217d6d8b`](https://github.com/nodejs/node/commit/f1217d6d8b)] - **doc**: revise commit-queue.md (Rich Trott) [#35006](https://github.com/nodejs/node/pull/35006)
* [[`9aba579acb`](https://github.com/nodejs/node/commit/9aba579acb)] - **doc**: change effected to affected (Turner Jabbour) [#34989](https://github.com/nodejs/node/pull/34989)
* [[`2598527112`](https://github.com/nodejs/node/commit/2598527112)] - **doc**: drop the --production flag for installing windows-build-tools (DeeDeeG) [#34979](https://github.com/nodejs/node/pull/34979)
* [[`287ce7b810`](https://github.com/nodejs/node/commit/287ce7b810)] - **doc**: fix broken link to response.writableFinished in deprecations doc (Rich Trott) [#34983](https://github.com/nodejs/node/pull/34983)
* [[`a0656ff863`](https://github.com/nodejs/node/commit/a0656ff863)] - **doc**: fix broken link to response.finished in deprecations doc (Rich Trott) [#34982](https://github.com/nodejs/node/pull/34982)
* [[`f4524b8936`](https://github.com/nodejs/node/commit/f4524b8936)] - **doc**: fix broken link to writableEnded in deprecations doc (Rich Trott) [#34984](https://github.com/nodejs/node/pull/34984)
* [[`514a538f64`](https://github.com/nodejs/node/commit/514a538f64)] - **doc**: fix typos in buffer doc (Robert Williams) [#34981](https://github.com/nodejs/node/pull/34981)
* [[`df76c89b78`](https://github.com/nodejs/node/commit/df76c89b78)] - **doc**: recommend URL() over url.parse() in http2 doc (Rich Trott) [#34978](https://github.com/nodejs/node/pull/34978)
* [[`ca0302e4f1`](https://github.com/nodejs/node/commit/ca0302e4f1)] - **doc**: arrange perf\_hooks entries alphabetically (Rich Trott) [#34973](https://github.com/nodejs/node/pull/34973)
* [[`94c6e09367`](https://github.com/nodejs/node/commit/94c6e09367)] - **doc**: replace require() with reference links in http2.md (Rich Trott) [#34956](https://github.com/nodejs/node/pull/34956)
* [[`2407a7a671`](https://github.com/nodejs/node/commit/2407a7a671)] - **doc**: add a note about possible missing lines to readline.asyncIterator (Igor Mikhalev) [#34675](https://github.com/nodejs/node/pull/34675)
* [[`31098a4c0e`](https://github.com/nodejs/node/commit/31098a4c0e)] - **doc**: make minor improvements to query string sentence in http2.md (Rich Trott) [#34929](https://github.com/nodejs/node/pull/34929)
* [[`1589f0e6f4`](https://github.com/nodejs/node/commit/1589f0e6f4)] - **doc**: make general copy-edit changes to policy.md (Rich Trott) [#34943](https://github.com/nodejs/node/pull/34943)
* [[`aee3b8510b`](https://github.com/nodejs/node/commit/aee3b8510b)] - **doc**: simplify "make use of" to "use" (Rich Trott) [#34861](https://github.com/nodejs/node/pull/34861)
* [[`0e09ff8ab1`](https://github.com/nodejs/node/commit/0e09ff8ab1)] - **doc**: make minor fixes to maintaining-openssl.md (Rich Trott) [#34926](https://github.com/nodejs/node/pull/34926)
* [[`b091681d25`](https://github.com/nodejs/node/commit/b091681d25)] - **doc**: fix CHANGELOG.md parsing issue (Juan José Arboleda) [#34923](https://github.com/nodejs/node/pull/34923)
* [[`fbd18be459`](https://github.com/nodejs/node/commit/fbd18be459)] - **doc**: provide more guidance about process.version (Rich Trott) [#34909](https://github.com/nodejs/node/pull/34909)
* [[`4782ec7b3b`](https://github.com/nodejs/node/commit/4782ec7b3b)] - **doc**: use consistent typography for node-addon-api (Rich Trott) [#34910](https://github.com/nodejs/node/pull/34910)
* [[`2fe95094fd`](https://github.com/nodejs/node/commit/2fe95094fd)] - **doc**: improve link-local text in dgram.md (Rich Trott) [#34868](https://github.com/nodejs/node/pull/34868)
* [[`657292e2dd`](https://github.com/nodejs/node/commit/657292e2dd)] - **doc**: fix broken markdown/display in cli.html (Rich Trott) [#34892](https://github.com/nodejs/node/pull/34892)
* [[`4cf93bb3cf`](https://github.com/nodejs/node/commit/4cf93bb3cf)] - **doc**: use "previous"/"preceding" instead of "above" as modifier (Rich Trott) [#34877](https://github.com/nodejs/node/pull/34877)
* [[`29b048b06b`](https://github.com/nodejs/node/commit/29b048b06b)] - **doc**: use links to MS guide in style guide (Rich Trott) [#34871](https://github.com/nodejs/node/pull/34871)
* [[`52be37cf39`](https://github.com/nodejs/node/commit/52be37cf39)] - **doc,tools**: remove malfunctioning Linux manpage linker (Rich Trott) [#34985](https://github.com/nodejs/node/pull/34985)
* [[`fffba3a270`](https://github.com/nodejs/node/commit/fffba3a270)] - **errors**: use `ErrorPrototypeToString` from `primordials` object (ExE Boss) [#34891](https://github.com/nodejs/node/pull/34891)
* [[`db8c66b8c2`](https://github.com/nodejs/node/commit/db8c66b8c2)] - **esm**: shorten ERR\_UNSUPPORTED\_ESM\_URL\_SCHEME message (Rich Trott) [#34836](https://github.com/nodejs/node/pull/34836)
* [[`be71e717c5`](https://github.com/nodejs/node/commit/be71e717c5)] - **meta**: enable wasi for CODEOWNERS (gengjiawen) [#34889](https://github.com/nodejs/node/pull/34889)
* [[`a43b7ff72e`](https://github.com/nodejs/node/commit/a43b7ff72e)] - **meta**: remove non-existent quic from CODEOWNERS (Richard Lau) [#34947](https://github.com/nodejs/node/pull/34947)
* [[`3c32fe09e9`](https://github.com/nodejs/node/commit/3c32fe09e9)] - **n-api**: re-implement async env cleanup hooks (Gabriel Schulhof) [#34819](https://github.com/nodejs/node/pull/34819)
* [[`fcb211f38a`](https://github.com/nodejs/node/commit/fcb211f38a)] - **net**: replace usage of internal stream state with public api (Denys Otrishko) [#34885](https://github.com/nodejs/node/pull/34885)
* [[`8aac42caf2`](https://github.com/nodejs/node/commit/8aac42caf2)] - **(SEMVER-MINOR)** **perf_hooks**: add idleTime and event loop util (Trevor Norris) [#34938](https://github.com/nodejs/node/pull/34938)
* [[`18b04ab4c8`](https://github.com/nodejs/node/commit/18b04ab4c8)] - **policy**: implement scopes field (Bradley Farias) [#34552](https://github.com/nodejs/node/pull/34552)
* [[`1bf5d1a39b`](https://github.com/nodejs/node/commit/1bf5d1a39b)] - **querystring**: manage percent character at unescape (Daijiro Wachi) [#35013](https://github.com/nodejs/node/pull/35013)
* [[`f21d78d537`](https://github.com/nodejs/node/commit/f21d78d537)] - **src**: shutdown libuv before exit() (Anna Henningsen) [#35021](https://github.com/nodejs/node/pull/35021)
* [[`789798bedf`](https://github.com/nodejs/node/commit/789798bedf)] - **src**: add get/set pair for env context awareness (Shelley Vohr) [#35024](https://github.com/nodejs/node/pull/35024)
* [[`73ef3f2f05`](https://github.com/nodejs/node/commit/73ef3f2f05)] - **src**: disallow JS execution during exit() (Anna Henningsen) [#35020](https://github.com/nodejs/node/pull/35020)
* [[`f6a5999a9d`](https://github.com/nodejs/node/commit/f6a5999a9d)] - **src,doc**: fix wording to refer to context, not environment (Turner Jabbour) [#34880](https://github.com/nodejs/node/pull/34880)
* [[`bcc1d431f8`](https://github.com/nodejs/node/commit/bcc1d431f8)] - **src,doc**: fix grammar due to missing 'is' (Turner Jabbour) [#34897](https://github.com/nodejs/node/pull/34897)
* [[`044297ff10`](https://github.com/nodejs/node/commit/044297ff10)] - **src,doc**: rephrase for clarity (Turner Jabbour) [#34879](https://github.com/nodejs/node/pull/34879)
* [[`4bb40078da`](https://github.com/nodejs/node/commit/4bb40078da)] - **(SEMVER-MINOR)** **stream**: simpler and faster Readable async iterator (Robert Nagy) [#34035](https://github.com/nodejs/node/pull/34035)
* [[`ffae5f3809`](https://github.com/nodejs/node/commit/ffae5f3809)] - **(SEMVER-MINOR)** **stream**: save error in state (Robert Nagy) [#34103](https://github.com/nodejs/node/pull/34103)
* [[`5f24cea11a`](https://github.com/nodejs/node/commit/5f24cea11a)] - **stream**: fix Readable stream state properties (Denys Otrishko) [#34886](https://github.com/nodejs/node/pull/34886)
* [[`f537c868b9`](https://github.com/nodejs/node/commit/f537c868b9)] - **stream**: allow using `.push()`/`.unshift()` during `once('data')` (Anna Henningsen) [#34957](https://github.com/nodejs/node/pull/34957)
* [[`4d533858cf`](https://github.com/nodejs/node/commit/4d533858cf)] - **test**: make .out checks embedder-friendly (Shelley Vohr) [#35040](https://github.com/nodejs/node/pull/35040)
* [[`a756b92c4a`](https://github.com/nodejs/node/commit/a756b92c4a)] - **test**: use mustCall() in test-http-timeout (Pooja D.P) [#34996](https://github.com/nodejs/node/pull/34996)
* [[`9011c87c1c`](https://github.com/nodejs/node/commit/9011c87c1c)] - **test**: change var to let (Pooja D.P) [#34902](https://github.com/nodejs/node/pull/34902)
* [[`b698d2ec81`](https://github.com/nodejs/node/commit/b698d2ec81)] - **test**: remove incorrect debug() in test-policy-integrity (Rich Trott) [#34961](https://github.com/nodejs/node/pull/34961)
* [[`ee6a583b9f`](https://github.com/nodejs/node/commit/ee6a583b9f)] - **test**: fix typo in test/parallel/test-icu-punycode.js (Daijiro Wachi) [#34934](https://github.com/nodejs/node/pull/34934)
* [[`9057a1644d`](https://github.com/nodejs/node/commit/9057a1644d)] - **test**: add readline test for escape sequence (Rich Trott) [#34952](https://github.com/nodejs/node/pull/34952)
* [[`75d16125e1`](https://github.com/nodejs/node/commit/75d16125e1)] - **test**: make test-tls-reuse-host-from-socket pass without internet (Rich Trott) [#34953](https://github.com/nodejs/node/pull/34953)
* [[`971b7ac087`](https://github.com/nodejs/node/commit/971b7ac087)] - **test**: simplify test-vm-memleak (Rich Trott) [#34881](https://github.com/nodejs/node/pull/34881)
* [[`577978a96c`](https://github.com/nodejs/node/commit/577978a96c)] - **tools**: fix docopen target (Antoine du HAMEL) [#35062](https://github.com/nodejs/node/pull/35062)
* [[`2b445bb3ee`](https://github.com/nodejs/node/commit/2b445bb3ee)] - **tools**: fix doc build targets (Antoine du HAMEL) [#35060](https://github.com/nodejs/node/pull/35060)
* [[`3d41ff25b7`](https://github.com/nodejs/node/commit/3d41ff25b7)] - **tools**: add banner to lint-md.js by rollup.config.js (KuthorX) [#34233](https://github.com/nodejs/node/pull/34233)
* [[`62cc3b8249`](https://github.com/nodejs/node/commit/62cc3b8249)] - **tools**: update ESLint to 7.8.1 (cjihrig) [#35004](https://github.com/nodejs/node/pull/35004)
* [[`c47d319ac6`](https://github.com/nodejs/node/commit/c47d319ac6)] - **tools**: update ESLint to 7.8.0 (cjihrig) [#35004](https://github.com/nodejs/node/pull/35004)
* [[`b6f3ae8ffc`](https://github.com/nodejs/node/commit/b6f3ae8ffc)] - **tools,doc**: allow page titles to contain inline code (Antoine du HAMEL) [#35003](https://github.com/nodejs/node/pull/35003)
* [[`fb2111e300`](https://github.com/nodejs/node/commit/fb2111e300)] - **tools,doc**: fix global table of content active element (Antoine du Hamel) [#34976](https://github.com/nodejs/node/pull/34976)
* [[`7ad629e4e4`](https://github.com/nodejs/node/commit/7ad629e4e4)] - **tools,doc**: remove "toc" anchor name (Rich Trott) [#34893](https://github.com/nodejs/node/pull/34893)
* [[`94528f510e`](https://github.com/nodejs/node/commit/94528f510e)] - **zlib**: replace usage of internal stream state with public api (Denys Otrishko) [#34884](https://github.com/nodejs/node/pull/34884)

<a id="14.9.0"></a>
## 2020-08-27, Version 14.9.0 (Current), @BethGriggs prepared by @danielleadams

### Notable Changes

* **build**: set --v8-enable-object-print by default (Mary Marchini) [#34705](https://github.com/nodejs/node/pull/34705)
* **deps**:
  * upgrade to libuv 1.39.0 (cjihrig) [#34915](https://github.com/nodejs/node/pull/34915)
  * upgrade npm to 6.14.8 (Ruy Adorno) [#34834](https://github.com/nodejs/node/pull/34834)
  * V8: cherry-pick e06ace6b5cdb (Anna Henningsen) [#34673](https://github.com/nodejs/node/pull/34673)
* **n-api**: handle weak no-finalizer refs correctly (Gabriel Schulhof) [#34839](https://github.com/nodejs/node/pull/34839)
* **tools**: add debug entitlements for macOS 10.15+ (Gabriele Greco) [#34378](https://github.com/nodejs/node/pull/34378)

### Commits

* [[`aaa6e43d3c`](https://github.com/nodejs/node/commit/aaa6e43d3c)] - Forces Powershell to use tls1.2 (Bartosz Sosnowski) [#33609](https://github.com/nodejs/node/pull/33609)
* [[`8de6b72efa`](https://github.com/nodejs/node/commit/8de6b72efa)] - **benchmark**: add benchmark script for resourceUsage (Yash Ladha) [#34691](https://github.com/nodejs/node/pull/34691)
* [[`e4450a199f`](https://github.com/nodejs/node/commit/e4450a199f)] - **benchmark**: update function\_args addon code (Anna Henningsen) [#34725](https://github.com/nodejs/node/pull/34725)
* [[`332e38433b`](https://github.com/nodejs/node/commit/332e38433b)] - **(SEMVER-MINOR)** **buffer**: alias UInt ➡️ Uint in buffer methods (Anna Henningsen) [#34729](https://github.com/nodejs/node/pull/34729)
* [[`7f0869f963`](https://github.com/nodejs/node/commit/7f0869f963)] - **build**: run link checker in linter workflow (Richard Lau) [#34810](https://github.com/nodejs/node/pull/34810)
* [[`9ca4b2ad5c`](https://github.com/nodejs/node/commit/9ca4b2ad5c)] - **build**: add CODEOWNERS linter action (Mary Marchini) [#34739](https://github.com/nodejs/node/pull/34739)
* [[`bdf26aebb4`](https://github.com/nodejs/node/commit/bdf26aebb4)] - **(SEMVER-MINOR)** **build**: add build flag for OSS-Fuzz integration (davkor) [#34761](https://github.com/nodejs/node/pull/34761)
* [[`d89a83c62c`](https://github.com/nodejs/node/commit/d89a83c62c)] - **build**: move compiling for Windows ARM64 to Tier 2 (João Reis) [#34721](https://github.com/nodejs/node/pull/34721)
* [[`aed82379dd`](https://github.com/nodejs/node/commit/aed82379dd)] - **build**: implement a Commit Queue in Actions (Mary Marchini) [#34112](https://github.com/nodejs/node/pull/34112)
* [[`15c92083b5`](https://github.com/nodejs/node/commit/15c92083b5)] - **build**: set --v8-enable-object-print by default (Mary Marchini) [#34705](https://github.com/nodejs/node/pull/34705)
* [[`201d3d7074`](https://github.com/nodejs/node/commit/201d3d7074)] - **build**: cover all benchmark addons with C++ linter (Anna Henningsen) [#34725](https://github.com/nodejs/node/pull/34725)
* [[`2abc98e9ff`](https://github.com/nodejs/node/commit/2abc98e9ff)] - **build**: add flag to build V8 with OBJECT\_PRINT (Mary Marchini) [#32834](https://github.com/nodejs/node/pull/32834)
* [[`6048421726`](https://github.com/nodejs/node/commit/6048421726)] - **build,win**: use x64 Node when building for ARM64 (Dennis Ameling) [#34009](https://github.com/nodejs/node/pull/34009)
* [[`69bcca122e`](https://github.com/nodejs/node/commit/69bcca122e)] - **crypto**: avoid unitializing ECDH objects on error (Tobias Nießen) [#34302](https://github.com/nodejs/node/pull/34302)
* [[`cf348542c6`](https://github.com/nodejs/node/commit/cf348542c6)] - **deps**: upgrade to libuv 1.39.0 (cjihrig) [#34915](https://github.com/nodejs/node/pull/34915)
* [[`68b7a8db6f`](https://github.com/nodejs/node/commit/68b7a8db6f)] - **deps**: upgrade npm to 6.14.8 (Ruy Adorno) [#34834](https://github.com/nodejs/node/pull/34834)
* [[`9527a2a8a7`](https://github.com/nodejs/node/commit/9527a2a8a7)] - **deps**: V8: cherry-pick e06ace6b5cdb (Anna Henningsen) [#34673](https://github.com/nodejs/node/pull/34673)
* [[`cd32522c92`](https://github.com/nodejs/node/commit/cd32522c92)] - **doc**: add missing DEP ID for 'new crypto.Certificate()' (Beth Griggs) [#34940](https://github.com/nodejs/node/pull/34940)
* [[`ff15c92a7f`](https://github.com/nodejs/node/commit/ff15c92a7f)] - **doc**: improve fs doc intro (James M Snell) [#34843](https://github.com/nodejs/node/pull/34843)
* [[`dae93ca0cb`](https://github.com/nodejs/node/commit/dae93ca0cb)] - **doc**: indicate the format of process.version (Danny Guo) [#34872](https://github.com/nodejs/node/pull/34872)
* [[`bf7f492cb6`](https://github.com/nodejs/node/commit/bf7f492cb6)] - **doc**: rename module pages (Antoine du HAMEL) [#34663](https://github.com/nodejs/node/pull/34663)
* [[`f2c2f42195`](https://github.com/nodejs/node/commit/f2c2f42195)] - **doc**: improve wording in deprecations.md (Rich Trott) [#34860](https://github.com/nodejs/node/pull/34860)
* [[`4b3b0e3f98`](https://github.com/nodejs/node/commit/4b3b0e3f98)] - **doc**: fix ESM/CJS wrapper example (Maksim Sinik) [#34853](https://github.com/nodejs/node/pull/34853)
* [[`d6bb2ad5ea`](https://github.com/nodejs/node/commit/d6bb2ad5ea)] - **doc**: adopt Microsoft Style Guide officially (Rich Trott) [#34821](https://github.com/nodejs/node/pull/34821)
* [[`e4679bd45d`](https://github.com/nodejs/node/commit/e4679bd45d)] - **doc**: use 'console' info string for console output (Rich Trott) [#34837](https://github.com/nodejs/node/pull/34837)
* [[`b1c3fb73fc`](https://github.com/nodejs/node/commit/b1c3fb73fc)] - **doc**: fix bulleted list punctuation in BUILDING.md (Rich Trott) [#34849](https://github.com/nodejs/node/pull/34849)
* [[`ef41ddf5cb`](https://github.com/nodejs/node/commit/ef41ddf5cb)] - **doc**: sort references lexically (Rich Trott) [#34848](https://github.com/nodejs/node/pull/34848)
* [[`3133b75b68`](https://github.com/nodejs/node/commit/3133b75b68)] - **doc**: move addaleax to TSC emeritus (Anna Henningsen) [#34809](https://github.com/nodejs/node/pull/34809)
* [[`5214de78cd`](https://github.com/nodejs/node/commit/5214de78cd)] - **doc**: remove space above version picker (Justice Almanzar) [#34768](https://github.com/nodejs/node/pull/34768)
* [[`34430abd71`](https://github.com/nodejs/node/commit/34430abd71)] - **doc**: move module core module doc to separate page (Antoine du HAMEL) [#34747](https://github.com/nodejs/node/pull/34747)
* [[`b356b79ca4`](https://github.com/nodejs/node/commit/b356b79ca4)] - **doc**: reorder deprecated tls docs (Jerome T.K. Covington) [#34687](https://github.com/nodejs/node/pull/34687)
* [[`5c987ffc96`](https://github.com/nodejs/node/commit/5c987ffc96)] - **doc**: fix file name to main.mjs and not main.js in esm.md (Frank Lemanschik) [#34786](https://github.com/nodejs/node/pull/34786)
* [[`969fb1c5e3`](https://github.com/nodejs/node/commit/969fb1c5e3)] - **doc**: improve async\_hooks snippets (Andrey Pechkurov) [#34829](https://github.com/nodejs/node/pull/34829)
* [[`3360dcbfab`](https://github.com/nodejs/node/commit/3360dcbfab)] - **doc**: fix some typos and grammar mistakes (Hilla Shahrabani) [#34800](https://github.com/nodejs/node/pull/34800)
* [[`47f2f45dd8`](https://github.com/nodejs/node/commit/47f2f45dd8)] - **doc**: deprecate (doc-only) crypto.Certificate() (Rich Trott) [#34697](https://github.com/nodejs/node/pull/34697)
* [[`3bfe199c28`](https://github.com/nodejs/node/commit/3bfe199c28)] - **doc**: remove "is recommended from crypto legacy API text (Rich Trott) [#34697](https://github.com/nodejs/node/pull/34697)
* [[`258f64f578`](https://github.com/nodejs/node/commit/258f64f578)] - **doc**: edit filehandle.close() entry in fs.md (Rich Trott) [#34782](https://github.com/nodejs/node/pull/34782)
* [[`e54a6842e0`](https://github.com/nodejs/node/commit/e54a6842e0)] - **doc**: fix broken links in commit-queue.md (Luigi Pinca) [#34789](https://github.com/nodejs/node/pull/34789)
* [[`3925fd6550`](https://github.com/nodejs/node/commit/3925fd6550)] - **doc**: avoid \_may\_ in collaborator guide (Rich Trott) [#34749](https://github.com/nodejs/node/pull/34749)
* [[`cb0960635b`](https://github.com/nodejs/node/commit/cb0960635b)] - **doc**: use sentence-casing for headers in collaborator guide (Rich Trott) [#34713](https://github.com/nodejs/node/pull/34713)
* [[`8b5690287c`](https://github.com/nodejs/node/commit/8b5690287c)] - **doc**: edit (general) collaborator guide (Rich Trott) [#34712](https://github.com/nodejs/node/pull/34712)
* [[`b933eef1f3`](https://github.com/nodejs/node/commit/b933eef1f3)] - **doc**: reduce repetitiveness on Consensus Seeking (Mary Marchini) [#34702](https://github.com/nodejs/node/pull/34702)
* [[`f7563f811a`](https://github.com/nodejs/node/commit/f7563f811a)] - **doc**: remove typo in crypto.md (Rich Trott) [#34698](https://github.com/nodejs/node/pull/34698)
* [[`ea98122a51`](https://github.com/nodejs/node/commit/ea98122a51)] - **doc**: n-api environment life cycle APIs are stable (Jim Schlight) [#34641](https://github.com/nodejs/node/pull/34641)
* [[`b00f71b660`](https://github.com/nodejs/node/commit/b00f71b660)] - **doc**: add padding in the sidebar column (Antoine du HAMEL) [#34665](https://github.com/nodejs/node/pull/34665)
* [[`91f53245ae`](https://github.com/nodejs/node/commit/91f53245ae)] - **doc**: use semantically appropriate tag for lines (Antoine du HAMEL) [#34660](https://github.com/nodejs/node/pull/34660)
* [[`230bcaf276`](https://github.com/nodejs/node/commit/230bcaf276)] - **doc**: add HPE\_UNEXPECTED\_CONTENT\_LENGTH error description (Nikolay Krashnikov) [#34596](https://github.com/nodejs/node/pull/34596)
* [[`d29b805569`](https://github.com/nodejs/node/commit/d29b805569)] - **doc**: update http server response 'close' event (Renato Mariscal) [#34472](https://github.com/nodejs/node/pull/34472)
* [[`b93ba07fa5`](https://github.com/nodejs/node/commit/b93ba07fa5)] - **doc**: add writable and readable options to Duplex docs (Priyank Singh) [#34383](https://github.com/nodejs/node/pull/34383)
* [[`7cde699115`](https://github.com/nodejs/node/commit/7cde699115)] - **doc**: harden policy around objections (Mary Marchini) [#34639](https://github.com/nodejs/node/pull/34639)
* [[`7d0970ca66`](https://github.com/nodejs/node/commit/7d0970ca66)] - **doc,lib**: remove unused error code (Rich Trott) [#34792](https://github.com/nodejs/node/pull/34792)
* [[`9ebae0a758`](https://github.com/nodejs/node/commit/9ebae0a758)] - **doc,n-api**: add link to n-api tutorial website (Jim Schlight) [#34870](https://github.com/nodejs/node/pull/34870)
* [[`cdd4540124`](https://github.com/nodejs/node/commit/cdd4540124)] - **doc,tools**: annotate broken links in actions workflow (Richard Lau) [#34810](https://github.com/nodejs/node/pull/34810)
* [[`dbcb36d553`](https://github.com/nodejs/node/commit/dbcb36d553)] - **errors**: improve ERR\_INVALID\_OPT\_VALUE error (Denys Otrishko) [#34671](https://github.com/nodejs/node/pull/34671)
* [[`8f38c19c08`](https://github.com/nodejs/node/commit/8f38c19c08)] - **esm**: improve error message of ERR\_UNSUPPORTED\_ESM\_URL\_SCHEME (Denys Otrishko) [#34795](https://github.com/nodejs/node/pull/34795)
* [[`7ef5591d06`](https://github.com/nodejs/node/commit/7ef5591d06)] - **fs**: guard against undefined behavior (Robert Nagy) [#34746](https://github.com/nodejs/node/pull/34746)
* [[`952f233e39`](https://github.com/nodejs/node/commit/952f233e39)] - **http**: add RFC references for each status code (Voltra) [#33671](https://github.com/nodejs/node/pull/33671)
* [[`cc7258469c`](https://github.com/nodejs/node/commit/cc7258469c)] - **http2**: fix Http2Response.sendDate (João Lucas Lucchetta) [#34850](https://github.com/nodejs/node/pull/34850)
* [[`9e0d18fd3f`](https://github.com/nodejs/node/commit/9e0d18fd3f)] - **http2**: use and support non-empty DATA frame with END\_STREAM flag (Carlos Lopez) [#33875](https://github.com/nodejs/node/pull/33875)
* [[`6ee2578427`](https://github.com/nodejs/node/commit/6ee2578427)] - **http2**: add maxHeaderSize option to http2 (Priyank Singh) [#33636](https://github.com/nodejs/node/pull/33636)
* [[`04defbaacd`](https://github.com/nodejs/node/commit/04defbaacd)] - **lib**: allow to validate enums with validateOneOf (Denys Otrishko) [#34070](https://github.com/nodejs/node/pull/34070)
* [[`1a9496a79d`](https://github.com/nodejs/node/commit/1a9496a79d)] - **lib**: add UNC support to url.pathToFileURL() (Matthew McEachen) [#34743](https://github.com/nodejs/node/pull/34743)
* [[`124a01d487`](https://github.com/nodejs/node/commit/124a01d487)] - **lib**: use full URL to GitHub issues in comments (Rich Trott) [#34686](https://github.com/nodejs/node/pull/34686)
* [[`756c058c45`](https://github.com/nodejs/node/commit/756c058c45)] - **meta**: fix codeowners docs path (Mary Marchini) [#34811](https://github.com/nodejs/node/pull/34811)
* [[`2781f646c9`](https://github.com/nodejs/node/commit/2781f646c9)] - **meta**: add TSC as owner of governance-related docs (Mary Marchini) [#34737](https://github.com/nodejs/node/pull/34737)
* [[`a69d30eb3f`](https://github.com/nodejs/node/commit/a69d30eb3f)] - **module**: drop `-u` alias for `--conditions` (Richard Lau) [#34935](https://github.com/nodejs/node/pull/34935)
* [[`e4a0e5bc1a`](https://github.com/nodejs/node/commit/e4a0e5bc1a)] - **module**: fix check for package.json at volume root (Derek Lewis) [#34595](https://github.com/nodejs/node/pull/34595)
* [[`698cae7625`](https://github.com/nodejs/node/commit/698cae7625)] - **module**: share CJS/ESM resolver fns, refactoring (Guy Bedford) [#34744](https://github.com/nodejs/node/pull/34744)
* [[`6929649793`](https://github.com/nodejs/node/commit/6929649793)] - **module**: custom --conditions flag option (Guy Bedford) [#34637](https://github.com/nodejs/node/pull/34637)
* [[`9a7c87df37`](https://github.com/nodejs/node/commit/9a7c87df37)] - **module**: use cjsCache over esm injection (Guy Bedford) [#34605](https://github.com/nodejs/node/pull/34605)
* [[`98f7d8ec81`](https://github.com/nodejs/node/commit/98f7d8ec81)] - **n-api**: handle weak no-finalizer refs correctly (Gabriel Schulhof) [#34839](https://github.com/nodejs/node/pull/34839)
* [[`90abdd3dd4`](https://github.com/nodejs/node/commit/90abdd3dd4)] - **net**: validate custom lookup() output (cjihrig) [#34813](https://github.com/nodejs/node/pull/34813)
* [[`84031183bc`](https://github.com/nodejs/node/commit/84031183bc)] - **policy**: support conditions for redirects (Bradley Farias) [#34414](https://github.com/nodejs/node/pull/34414)
* [[`a16f0f427e`](https://github.com/nodejs/node/commit/a16f0f427e)] - **process**: correctly parse Unicode in NODE\_OPTIONS (Bartosz Sosnowski) [#34476](https://github.com/nodejs/node/pull/34476)
* [[`fff1e7f86c`](https://github.com/nodejs/node/commit/fff1e7f86c)] - **src**: fix abort on uv\_loop\_init() failure (Ben Noordhuis) [#34874](https://github.com/nodejs/node/pull/34874)
* [[`7666d95c7d`](https://github.com/nodejs/node/commit/7666d95c7d)] - **src**: usage of modernize-use-equals-default (Yash Ladha) [#34807](https://github.com/nodejs/node/pull/34807)
* [[`3022e0d614`](https://github.com/nodejs/node/commit/3022e0d614)] - **src**: prefer C++ empty() in boolean expressions (Tobias Nießen) [#34432](https://github.com/nodejs/node/pull/34432)
* [[`e16b3e72f9`](https://github.com/nodejs/node/commit/e16b3e72f9)] - **test**: fix test-cluster-net-listen-relative-path.js to run in / (Rich Trott) [#34820](https://github.com/nodejs/node/pull/34820)
* [[`2a78c33445`](https://github.com/nodejs/node/commit/2a78c33445)] - **test**: run REPL preview test regardless of terminal type (Rich Trott) [#34798](https://github.com/nodejs/node/pull/34798)
* [[`6b45bf3475`](https://github.com/nodejs/node/commit/6b45bf3475)] - **test**: modernize test-cluster-master-error (Anna Henningsen) [#34685](https://github.com/nodejs/node/pull/34685)
* [[`c080fc590d`](https://github.com/nodejs/node/commit/c080fc590d)] - **test**: move test-inspector-already-activated-cli to parallel (Rich Trott) [#34755](https://github.com/nodejs/node/pull/34755)
* [[`7ed7ef7ad8`](https://github.com/nodejs/node/commit/7ed7ef7ad8)] - **test**: move execution of WPT to worker threads (Michaël Zasso) [#34796](https://github.com/nodejs/node/pull/34796)
* [[`e8eed5c426`](https://github.com/nodejs/node/commit/e8eed5c426)] - **test**: convert assertion that always fails to assert.fail() (Rich Trott) [#34793](https://github.com/nodejs/node/pull/34793)
* [[`c458e8406e`](https://github.com/nodejs/node/commit/c458e8406e)] - **test**: remove common.rootDir (Rich Trott) [#34772](https://github.com/nodejs/node/pull/34772)
* [[`1c324d5939`](https://github.com/nodejs/node/commit/1c324d5939)] - **test**: allow ENOENT in test-worker-init-failure (Rich Trott) [#34769](https://github.com/nodejs/node/pull/34769)
* [[`88919e584b`](https://github.com/nodejs/node/commit/88919e584b)] - **test**: allow ENFILE in test-worker-init-failure (Rich Trott) [#34769](https://github.com/nodejs/node/pull/34769)
* [[`a78c638fc3`](https://github.com/nodejs/node/commit/a78c638fc3)] - **test**: use process.env.PYTHON to spawn python (Anna Henningsen) [#34700](https://github.com/nodejs/node/pull/34700)
* [[`9a790203ed`](https://github.com/nodejs/node/commit/9a790203ed)] - **test**: remove error message checking in test-worker-init-failure (Rich Trott) [#34727](https://github.com/nodejs/node/pull/34727)
* [[`0472d1629a`](https://github.com/nodejs/node/commit/0472d1629a)] - **test**: skip node-api/test\_worker\_terminate\_finalization (Anna Henningsen) [#34732](https://github.com/nodejs/node/pull/34732)
* [[`8e91f3ec0a`](https://github.com/nodejs/node/commit/8e91f3ec0a)] - **test**: fix test\_worker\_terminate\_finalization (Anna Henningsen) [#34726](https://github.com/nodejs/node/pull/34726)
* [[`fd5153c822`](https://github.com/nodejs/node/commit/fd5153c822)] - **test**: split test-crypto-dh-hash (Rich Trott) [#34631](https://github.com/nodejs/node/pull/34631)
* [[`9f0917e656`](https://github.com/nodejs/node/commit/9f0917e656)] - **test**: use block-scoping in test/pummel/test-timers.js (Rich Trott) [#34630](https://github.com/nodejs/node/pull/34630)
* [[`b261895d2b`](https://github.com/nodejs/node/commit/b261895d2b)] - **test**: remove test-child-process-fork-args flaky designation (Rich Trott) [#34684](https://github.com/nodejs/node/pull/34684)
* [[`27c0653517`](https://github.com/nodejs/node/commit/27c0653517)] - **test**: add vm crash regression test (Anna Henningsen) [#34673](https://github.com/nodejs/node/pull/34673)
* [[`093a4b0ae4`](https://github.com/nodejs/node/commit/093a4b0ae4)] - **test**: add tests for validateNumber/validateString (Denys Otrishko) [#34672](https://github.com/nodejs/node/pull/34672)
* [[`5009d82b0c`](https://github.com/nodejs/node/commit/5009d82b0c)] - **test,doc**: add missing uv\_setup\_args() calls (cjihrig) [#34751](https://github.com/nodejs/node/pull/34751)
* [[`cca0372022`](https://github.com/nodejs/node/commit/cca0372022)] - **(SEMVER-MINOR)** **timers**: allow timers to be used as primitives (Denys Otrishko) [#34017](https://github.com/nodejs/node/pull/34017)
* [[`e90cb49390`](https://github.com/nodejs/node/commit/e90cb49390)] - **tls**: enable renegotiation when using BoringSSL (Jeremy Rose) [#34832](https://github.com/nodejs/node/pull/34832)
* [[`8766b5bfd5`](https://github.com/nodejs/node/commit/8766b5bfd5)] - **tools**: add debug entitlements for macOS 10.15+ (Gabriele Greco) [#34378](https://github.com/nodejs/node/pull/34378)
* [[`77bbd73919`](https://github.com/nodejs/node/commit/77bbd73919)] - **util**: add debug and debuglog.enabled (Bradley Farias) [#33424](https://github.com/nodejs/node/pull/33424)
* [[`513ab0e02f`](https://github.com/nodejs/node/commit/513ab0e02f)] - **worker**: fix --abort-on-uncaught-exception handling (Anna Henningsen) [#34724](https://github.com/nodejs/node/pull/34724)
* [[`03d601344a`](https://github.com/nodejs/node/commit/03d601344a)] - **worker**: do not crash when JSTransferable lists untransferable value (Anna Henningsen) [#34766](https://github.com/nodejs/node/pull/34766)
* [[`b73943e476`](https://github.com/nodejs/node/commit/b73943e476)] - **workers**: add support for data: URLs (Antoine du HAMEL) [#34584](https://github.com/nodejs/node/pull/34584)

<a id="14.8.0"></a>
## 2020-08-11, Version 14.8.0 (Current), @codebytere

### Notable Changes

* [[`16aa927216`](https://github.com/nodejs/node/commit/16aa927216)] - **(SEMVER-MINOR)** **async_hooks**: add AsyncResource.bind utility (James M Snell) [#34574](https://github.com/nodejs/node/pull/34574)
* [[`dc49561e8d`](https://github.com/nodejs/node/commit/dc49561e8d)] - **deps**: update to uvwasi 0.0.10 (Colin Ihrig) [#34623](https://github.com/nodejs/node/pull/34623)
* [[`6cd1c41604`](https://github.com/nodejs/node/commit/6cd1c41604)] - **doc**: add Ricky Zhou to collaborators (rickyes) [#34676](https://github.com/nodejs/node/pull/34676)
* [[`f0a41b2530`](https://github.com/nodejs/node/commit/f0a41b2530)] - **doc**: add release key for Ruy Adorno (Ruy Adorno) [#34628](https://github.com/nodejs/node/pull/34628)
* [[`10dd7a0eda`](https://github.com/nodejs/node/commit/10dd7a0eda)] - **doc**: add DerekNonGeneric to collaborators (Derek Lewis) [#34602](https://github.com/nodejs/node/pull/34602)
* [[`62bb2e757f`](https://github.com/nodejs/node/commit/62bb2e757f)] - **(SEMVER-MINOR)** **module**: unflag Top-Level Await (Myles Borins) [#34558](https://github.com/nodejs/node/pull/34558)
* [[`8cc9e5eb52`](https://github.com/nodejs/node/commit/8cc9e5eb52)] - **(SEMVER-MINOR)** **n-api**: support type-tagging objects (Gabriel Schulhof) [#28237](https://github.com/nodejs/node/pull/28237)
* [[`e89ec46ba9`](https://github.com/nodejs/node/commit/e89ec46ba9)] - **(SEMVER-MINOR)** **n-api,src**: provide asynchronous cleanup hooks (Anna Henningsen) [#34572](https://github.com/nodejs/node/pull/34572)

### Commits

* [[`650248922b`](https://github.com/nodejs/node/commit/650248922b)] - **async_hooks**: avoid GC tracking of AsyncResource in ALS (Gerhard Stoebich) [#34653](https://github.com/nodejs/node/pull/34653)
* [[`0a51aa8fdb`](https://github.com/nodejs/node/commit/0a51aa8fdb)] - **async_hooks**: avoid unneeded AsyncResource creation (Gerhard Stoebich) [#34616](https://github.com/nodejs/node/pull/34616)
* [[`0af9bee4c3`](https://github.com/nodejs/node/commit/0af9bee4c3)] - **async_hooks**: improve property descriptors in als.bind (Gerhard Stoebich) [#34620](https://github.com/nodejs/node/pull/34620)
* [[`16aa927216`](https://github.com/nodejs/node/commit/16aa927216)] - **(SEMVER-MINOR)** **async_hooks**: add AsyncResource.bind utility (James M Snell) [#34574](https://github.com/nodejs/node/pull/34574)
* [[`e45c68af27`](https://github.com/nodejs/node/commit/e45c68af27)] - **async_hooks**: don't read resource if ALS is disabled (Gerhard Stoebich) [#34617](https://github.com/nodejs/node/pull/34617)
* [[`e9aebc3a8f`](https://github.com/nodejs/node/commit/e9aebc3a8f)] - **async_hooks**: fix id assignment in fast-path promise hook (Andrey Pechkurov) [#34548](https://github.com/nodejs/node/pull/34548)
* [[`5aed83c77f`](https://github.com/nodejs/node/commit/5aed83c77f)] - **async_hooks**: fix resource stack for deep stacks (Anna Henningsen) [#34573](https://github.com/nodejs/node/pull/34573)
* [[`9af62641c6`](https://github.com/nodejs/node/commit/9af62641c6)] - **async_hooks**: execute destroy hooks earlier (Gerhard Stoebich) [#34342](https://github.com/nodejs/node/pull/34342)
* [[`14656e1703`](https://github.com/nodejs/node/commit/14656e1703)] - **async_hooks**: don't reuse resource in HttpAgent when queued (Andrey Pechkurov) [#34439](https://github.com/nodejs/node/pull/34439)
* [[`c4457d873f`](https://github.com/nodejs/node/commit/c4457d873f)] - **benchmark**: always throw the same Error instance (Anna Henningsen) [#34523](https://github.com/nodejs/node/pull/34523)
* [[`6a129d0cf5`](https://github.com/nodejs/node/commit/6a129d0cf5)] - **build**: do not run auto-start-ci on forks (Evan Lucas) [#34650](https://github.com/nodejs/node/pull/34650)
* [[`2cd299b217`](https://github.com/nodejs/node/commit/2cd299b217)] - **build**: run CI on release branches (Shelley Vohr) [#34649](https://github.com/nodejs/node/pull/34649)
* [[`9ed9ccc5b3`](https://github.com/nodejs/node/commit/9ed9ccc5b3)] - **build**: enable build for node-v8 push (gengjiawen) [#34634](https://github.com/nodejs/node/pull/34634)
* [[`10f29e7550`](https://github.com/nodejs/node/commit/10f29e7550)] - **build**: increase startCI verbosity and fix job name (Mary Marchini) [#34635](https://github.com/nodejs/node/pull/34635)
* [[`befbaf384e`](https://github.com/nodejs/node/commit/befbaf384e)] - **build**: don't run auto-start-ci on push (Mary Marchini) [#34588](https://github.com/nodejs/node/pull/34588)
* [[`4af5dbd3bf`](https://github.com/nodejs/node/commit/4af5dbd3bf)] - **build**: fix auto-start-ci script path (Mary Marchini) [#34588](https://github.com/nodejs/node/pull/34588)
* [[`70cf3cbdfa`](https://github.com/nodejs/node/commit/70cf3cbdfa)] - **build**: auto start Jenkins CI via PR labels (Mary Marchini) [#34089](https://github.com/nodejs/node/pull/34089)
* [[`70e9eceeee`](https://github.com/nodejs/node/commit/70e9eceeee)] - **build**: toolchain.gypi and node\_gyp.py cleanup (iandrc) [#34268](https://github.com/nodejs/node/pull/34268)
* [[`465968c5f8`](https://github.com/nodejs/node/commit/465968c5f8)] - **console**: document the behavior of console.assert() (iandrc) [#34501](https://github.com/nodejs/node/pull/34501)
* [[`a7b4318df9`](https://github.com/nodejs/node/commit/a7b4318df9)] - **crypto**: add OP flag constants added in OpenSSL v1.1.1 (Mateusz Krawczuk) [#33929](https://github.com/nodejs/node/pull/33929)
* [[`dc49561e8d`](https://github.com/nodejs/node/commit/dc49561e8d)] - **deps**: update to uvwasi 0.0.10 (Colin Ihrig) [#34623](https://github.com/nodejs/node/pull/34623)
* [[`8b1ec43da4`](https://github.com/nodejs/node/commit/8b1ec43da4)] - **doc**: use \_Static method\_ instead of \_Class Method\_ (Rich Trott) [#34659](https://github.com/nodejs/node/pull/34659)
* [[`a1b9d7f42e`](https://github.com/nodejs/node/commit/a1b9d7f42e)] - **doc**: tidy some addons.md text (Rich Trott) [#34654](https://github.com/nodejs/node/pull/34654)
* [[`b78278b922`](https://github.com/nodejs/node/commit/b78278b922)] - **doc**: use \_Class Method\_ in async\_hooks.md (Rich Trott) [#34626](https://github.com/nodejs/node/pull/34626)
* [[`6cd1c41604`](https://github.com/nodejs/node/commit/6cd1c41604)] - **doc**: add Ricky Zhou to collaborators (rickyes) [#34676](https://github.com/nodejs/node/pull/34676)
* [[`d8e0deaa7c`](https://github.com/nodejs/node/commit/d8e0deaa7c)] - **doc**: edit process.title note for brevity and clarity (Rich Trott) [#34627](https://github.com/nodejs/node/pull/34627)
* [[`dd6bf20e8f`](https://github.com/nodejs/node/commit/dd6bf20e8f)] - **doc**: update fs.watch() availability for IBM i (iandrc) [#34611](https://github.com/nodejs/node/pull/34611)
* [[`f260bdd57b`](https://github.com/nodejs/node/commit/f260bdd57b)] - **doc**: fix typo in path.md (aetheryx) [#34550](https://github.com/nodejs/node/pull/34550)
* [[`f0a41b2530`](https://github.com/nodejs/node/commit/f0a41b2530)] - **doc**: add release key for Ruy Adorno (Ruy Adorno) [#34628](https://github.com/nodejs/node/pull/34628)
* [[`3f55dcd723`](https://github.com/nodejs/node/commit/3f55dcd723)] - **doc**: clarify process.title inconsistencies (Corey Butler) [#34557](https://github.com/nodejs/node/pull/34557)
* [[`6cd9ea82f6`](https://github.com/nodejs/node/commit/6cd9ea82f6)] - **doc**: document the connection event for HTTP2 & TLS servers (Tim Perry) [#34531](https://github.com/nodejs/node/pull/34531)
* [[`0a9389bb1a`](https://github.com/nodejs/node/commit/0a9389bb1a)] - **doc**: mention null special-case for `napi\_typeof` (Renée Kooi) [#34577](https://github.com/nodejs/node/pull/34577)
* [[`10dd7a0eda`](https://github.com/nodejs/node/commit/10dd7a0eda)] - **doc**: add DerekNonGeneric to collaborators (Derek Lewis) [#34602](https://github.com/nodejs/node/pull/34602)
* [[`d7eaf3a027`](https://github.com/nodejs/node/commit/d7eaf3a027)] - **doc**: revise N-API versions matrix text (Rich Trott) [#34566](https://github.com/nodejs/node/pull/34566)
* [[`e2bea73b03`](https://github.com/nodejs/node/commit/e2bea73b03)] - **doc**: clarify N-API version 1 (Michael Dawson) [#34344](https://github.com/nodejs/node/pull/34344)
* [[`be23e23361`](https://github.com/nodejs/node/commit/be23e23361)] - **doc**: use consistent spelling for "falsy" (Rich Trott) [#34545](https://github.com/nodejs/node/pull/34545)
* [[`f393ae9296`](https://github.com/nodejs/node/commit/f393ae9296)] - **doc**: simplify and clarify console.assert() documentation (Rich Trott) [#34544](https://github.com/nodejs/node/pull/34544)
* [[`b69ff2ff60`](https://github.com/nodejs/node/commit/b69ff2ff60)] - **doc**: use consistent capitalization for addons (Rich Trott) [#34536](https://github.com/nodejs/node/pull/34536)
* [[`212d17fa06`](https://github.com/nodejs/node/commit/212d17fa06)] - **doc**: add mmarchini pronouns (Mary Marchini) [#34586](https://github.com/nodejs/node/pull/34586)
* [[`7a28c3d543`](https://github.com/nodejs/node/commit/7a28c3d543)] - **doc**: update mmarchini contact info (Mary Marchini) [#34586](https://github.com/nodejs/node/pull/34586)
* [[`c8104f3d10`](https://github.com/nodejs/node/commit/c8104f3d10)] - **doc**: update .mailmap for mmarchini (Mary Marchini) [#34586](https://github.com/nodejs/node/pull/34586)
* [[`692a735881`](https://github.com/nodejs/node/commit/692a735881)] - **doc**: use sentence-case for headers in SECURITY.md (Rich Trott) [#34525](https://github.com/nodejs/node/pull/34525)
* [[`44e6c010b4`](https://github.com/nodejs/node/commit/44e6c010b4)] - **esm**: fix hook mistypes and links to types (Derek Lewis) [#34240](https://github.com/nodejs/node/pull/34240)
* [[`7322e58d11`](https://github.com/nodejs/node/commit/7322e58d11)] - **http**: reset headers timeout on headers complete (Robert Nagy) [#34578](https://github.com/nodejs/node/pull/34578)
* [[`36fd3daae6`](https://github.com/nodejs/node/commit/36fd3daae6)] - **http**: provide keep-alive timeout response header (Robert Nagy) [#34561](https://github.com/nodejs/node/pull/34561)
* [[`d0efaf2fe3`](https://github.com/nodejs/node/commit/d0efaf2fe3)] - **lib**: use non-symbols in isURLInstance check (Shelley Vohr) [#34622](https://github.com/nodejs/node/pull/34622)
* [[`335cb0d1d1`](https://github.com/nodejs/node/commit/335cb0d1d1)] - **lib**: absorb `path` error cases (Gireesh Punathil) [#34519](https://github.com/nodejs/node/pull/34519)
* [[`521e620533`](https://github.com/nodejs/node/commit/521e620533)] - **meta**: uncomment all codeowners (Mary Marchini) [#34670](https://github.com/nodejs/node/pull/34670)
* [[`650adeca22`](https://github.com/nodejs/node/commit/650adeca22)] - **meta**: enable http2 team for CODEOWNERS (Rich Trott) [#34534](https://github.com/nodejs/node/pull/34534)
* [[`35ef9907aa`](https://github.com/nodejs/node/commit/35ef9907aa)] - **module**: handle Top-Level Await non-fulfills better (Anna Henningsen) [#34640](https://github.com/nodejs/node/pull/34640)
* [[`62bb2e757f`](https://github.com/nodejs/node/commit/62bb2e757f)] - **(SEMVER-MINOR)** **module**: unflag Top-Level Await (Myles Borins) [#34558](https://github.com/nodejs/node/pull/34558)
* [[`fbd411d28a`](https://github.com/nodejs/node/commit/fbd411d28a)] - **n-api**: fix use-after-free with napi\_remove\_async\_cleanup\_hook (Anna Henningsen) [#34662](https://github.com/nodejs/node/pull/34662)
* [[`8cc9e5eb52`](https://github.com/nodejs/node/commit/8cc9e5eb52)] - **(SEMVER-MINOR)** **n-api**: support type-tagging objects (Gabriel Schulhof) [#28237](https://github.com/nodejs/node/pull/28237)
* [[`2703fe498e`](https://github.com/nodejs/node/commit/2703fe498e)] - **n-api**: simplify bigint-from-word creation (Gabriel Schulhof) [#34554](https://github.com/nodejs/node/pull/34554)
* [[`e89ec46ba9`](https://github.com/nodejs/node/commit/e89ec46ba9)] - **(SEMVER-MINOR)** **n-api,src**: provide asynchronous cleanup hooks (Anna Henningsen) [#34572](https://github.com/nodejs/node/pull/34572)
* [[`b1890e0866`](https://github.com/nodejs/node/commit/b1890e0866)] - **net**: don't return the stream object from onStreamRead (Robey Pointer) [#34375](https://github.com/nodejs/node/pull/34375)
* [[`35fdfb44a2`](https://github.com/nodejs/node/commit/35fdfb44a2)] - **policy**: increase tests via permutation matrix (Bradley Meck) [#34404](https://github.com/nodejs/node/pull/34404)
* [[`ddd339ff45`](https://github.com/nodejs/node/commit/ddd339ff45)] - **repl**: use \_Node.js\_ in user-facing REPL text (Rich Trott) [#34644](https://github.com/nodejs/node/pull/34644)
* [[`276e2980e2`](https://github.com/nodejs/node/commit/276e2980e2)] - **repl**: use \_REPL\_ in user-facing text (Rich Trott) [#34643](https://github.com/nodejs/node/pull/34643)
* [[`465c262ac6`](https://github.com/nodejs/node/commit/465c262ac6)] - **repl**: improve static import error message in repl (Myles Borins) [#33588](https://github.com/nodejs/node/pull/33588)
* [[`12cb0fb8a0`](https://github.com/nodejs/node/commit/12cb0fb8a0)] - **repl**: give repl entries unique names (Bradley Meck) [#34372](https://github.com/nodejs/node/pull/34372)
* [[`2dbd15a075`](https://github.com/nodejs/node/commit/2dbd15a075)] - **src**: fix linter failures (Anna Henningsen) [#34582](https://github.com/nodejs/node/pull/34582)
* [[`2761f349ec`](https://github.com/nodejs/node/commit/2761f349ec)] - **src**: spin shutdown loop while immediates are pending (Anna Henningsen) [#34662](https://github.com/nodejs/node/pull/34662)
* [[`39ca48c840`](https://github.com/nodejs/node/commit/39ca48c840)] - **src**: fix `size` underflow in CallbackQueue (Anna Henningsen) [#34662](https://github.com/nodejs/node/pull/34662)
* [[`c1abc8d3e5`](https://github.com/nodejs/node/commit/c1abc8d3e5)] - **src**: fix unused namespace member in node\_util (Andrey Pechkurov) [#34565](https://github.com/nodejs/node/pull/34565)
* [[`e146686972`](https://github.com/nodejs/node/commit/e146686972)] - **test**: fix wrong method call (gengjiawen) [#34629](https://github.com/nodejs/node/pull/34629)
* [[`ca89c375f7`](https://github.com/nodejs/node/commit/ca89c375f7)] - **test**: add debugging for callbacks in test-https-foafssl.js (Rich Trott) [#34603](https://github.com/nodejs/node/pull/34603)
* [[`2133b18bee`](https://github.com/nodejs/node/commit/2133b18bee)] - **test**: add debugging for test-https-foafssl.js (Rich Trott) [#34603](https://github.com/nodejs/node/pull/34603)
* [[`b9fb0c63b3`](https://github.com/nodejs/node/commit/b9fb0c63b3)] - **test**: convert most N-API tests from C++ to C (Gabriel Schulhof) [#34615](https://github.com/nodejs/node/pull/34615)
* [[`54a4c6a39c`](https://github.com/nodejs/node/commit/54a4c6a39c)] - **test**: replace flaky pummel regression tests (Anna Henningsen) [#34530](https://github.com/nodejs/node/pull/34530)
* [[`bd55236788`](https://github.com/nodejs/node/commit/bd55236788)] - **test**: change Fixes: to Refs: (Rich Trott) [#34568](https://github.com/nodejs/node/pull/34568)
* [[`a340587cfd`](https://github.com/nodejs/node/commit/a340587cfd)] - **test**: fix flaky http-parser-timeout-reset (Robert Nagy) [#34609](https://github.com/nodejs/node/pull/34609)
* [[`9c442f9786`](https://github.com/nodejs/node/commit/9c442f9786)] - **test**: remove unneeded flag check in test-vm-memleak (Rich Trott) [#34528](https://github.com/nodejs/node/pull/34528)
* [[`05100e1eec`](https://github.com/nodejs/node/commit/05100e1eec)] - **tools**: fix C++ import checker argument expansion (Anna Henningsen) [#34582](https://github.com/nodejs/node/pull/34582)
* [[`bf6c8aaae3`](https://github.com/nodejs/node/commit/bf6c8aaae3)] - **tools**: update ESLint to 7.6.0 (Colin Ihrig) [#34589](https://github.com/nodejs/node/pull/34589)
* [[`0b1616c2f0`](https://github.com/nodejs/node/commit/0b1616c2f0)] - **tools**: add meta.fixable to fixable lint rules (Colin Ihrig) [#34589](https://github.com/nodejs/node/pull/34589)
* [[`f46649bc5b`](https://github.com/nodejs/node/commit/f46649bc5b)] - **util**: print External address from inspect (unknown) [#34398](https://github.com/nodejs/node/pull/34398)
* [[`2fa24c0ccc`](https://github.com/nodejs/node/commit/2fa24c0ccc)] - **wasi**: add \_\_wasi\_fd\_filestat\_set\_times() test (Colin Ihrig) [#34623](https://github.com/nodejs/node/pull/34623)

<a id="14.7.0"></a>
## 2020-07-29, Version 14.7.0 (Current), @MylesBorins prepared by @ruyadorno

### Notable Changes

* **deps**:
  * upgrade npm to 6.14.7 (claudiahdz) [#34468](https://github.com/nodejs/node/pull/34468)
* **dgram**:
  * **(SEMVER-MINOR)** add IPv6 scope id suffix to received udp6 dgrams (Pekka Nikander) [#14500](https://github.com/nodejs/node/pull/14500)
* **src**:
  * **(SEMVER-MINOR)** allow preventing SetPromiseRejectCallback (Shelley Vohr) [#34387](https://github.com/nodejs/node/pull/34387)
  * **(SEMVER-MINOR)** allow setting a dir for all diagnostic output (AshCripps) [#33584](https://github.com/nodejs/node/pull/33584)
* **worker**:
  * **(SEMVER-MINOR)** make MessagePort inherit from EventTarget (Anna Henningsen) [#34057](https://github.com/nodejs/node/pull/34057)
* **zlib**:
  * switch to lazy init for zlib streams (Andrey Pechkurov) [#34048](https://github.com/nodejs/node/pull/34048)
* **New Collaborators**:
  * add rexagod to collaborators (Pranshu Srivastava) [#34457](https://github.com/nodejs/node/pull/34457)
  * add AshCripps to collaborators (AshCripps) [#34494](https://github.com/nodejs/node/pull/34494)
  * add HarshithaKP to collaborators (Harshitha K P) [#34417](https://github.com/nodejs/node/pull/34417)
  * add release key for Richard Lau (Richard Lau) [#34397](https://github.com/nodejs/node/pull/34397)

### Commits

* [[`dd2988917f`](https://github.com/nodejs/node/commit/dd2988917f)] - **async_hooks**: optimize fast-path promise hook for ALS (Andrey Pechkurov) [#34512](https://github.com/nodejs/node/pull/34512)
* [[`358b934284`](https://github.com/nodejs/node/commit/358b934284)] - **build**: fix test-ci-js task in Makefile (Rich Trott) [#34433](https://github.com/nodejs/node/pull/34433)
* [[`24e1beb829`](https://github.com/nodejs/node/commit/24e1beb829)] - **build**: do not run benchmark tests on 'make test' (Rich Trott) [#34434](https://github.com/nodejs/node/pull/34434)
* [[`b24f254472`](https://github.com/nodejs/node/commit/b24f254472)] - **build**: add benchmark tests to CI runs (Rich Trott) [#34288](https://github.com/nodejs/node/pull/34288)
* [[`a4806e2d12`](https://github.com/nodejs/node/commit/a4806e2d12)] - **build**: speed up source tarball creation (Richard Lau) [#34508](https://github.com/nodejs/node/pull/34508)
* [[`cce1f3e3a8`](https://github.com/nodejs/node/commit/cce1f3e3a8)] - **build**: don't run test-asan workflow on non-master pushes (Richard Lau) [#34509](https://github.com/nodejs/node/pull/34509)
* [[`70f23eb405`](https://github.com/nodejs/node/commit/70f23eb405)] - **build**: remove test-tarball action for windows + osx (Myles Borins) [#34440](https://github.com/nodejs/node/pull/34440)
* [[`3fda3d4bf3`](https://github.com/nodejs/node/commit/3fda3d4bf3)] - **build**: don't run Actions on non-master pushes (Shelley Vohr) [#34464](https://github.com/nodejs/node/pull/34464)
* [[`f7600d5ab6`](https://github.com/nodejs/node/commit/f7600d5ab6)] - **deps**: upgrade npm to 6.14.7 (claudiahdz) [#34468](https://github.com/nodejs/node/pull/34468)
* [[`02ae6d65d4`](https://github.com/nodejs/node/commit/02ae6d65d4)] - **(SEMVER-MINOR)** **dgram**: add IPv6 scope id suffix to received udp6 dgrams (Pekka Nikander) [#14500](https://github.com/nodejs/node/pull/14500)
* [[`e5f380052f`](https://github.com/nodejs/node/commit/e5f380052f)] - ***Revert*** "**doc**: move ronkorving to emeritus" (Rich Trott) [#34507](https://github.com/nodejs/node/pull/34507)
* [[`17bca62428`](https://github.com/nodejs/node/commit/17bca62428)] - **doc**: use sentence-case for GOVERNANCE.md headers (Rich Trott) [#34503](https://github.com/nodejs/node/pull/34503)
* [[`37752cde43`](https://github.com/nodejs/node/commit/37752cde43)] - **doc**: revise onboarding-extras (Rich Trott) [#34496](https://github.com/nodejs/node/pull/34496)
* [[`050866ddf1`](https://github.com/nodejs/node/commit/050866ddf1)] - **doc**: remove breaking-change-helper from onboarding-extras (Rich Trott) [#34497](https://github.com/nodejs/node/pull/34497)
* [[`2297d74fd8`](https://github.com/nodejs/node/commit/2297d74fd8)] - **doc**: add Triagers section to table of contents in GOVERNANCE.md (Rich Trott) [#34504](https://github.com/nodejs/node/pull/34504)
* [[`99a648738c`](https://github.com/nodejs/node/commit/99a648738c)] - **doc**: onboarding process extras (Gireesh Punathil) [#34455](https://github.com/nodejs/node/pull/34455)
* [[`bbc7eeadd9`](https://github.com/nodejs/node/commit/bbc7eeadd9)] - **doc**: mention triage in GOVERNANCE.md (Gireesh Punathil) [#34426](https://github.com/nodejs/node/pull/34426)
* [[`92c57b284b`](https://github.com/nodejs/node/commit/92c57b284b)] - **doc**: move thefourtheye to emeritus (Rich Trott) [#34471](https://github.com/nodejs/node/pull/34471)
* [[`657f2d78ee`](https://github.com/nodejs/node/commit/657f2d78ee)] - **doc**: move ronkorving to emeritus (Rich Trott) [#34471](https://github.com/nodejs/node/pull/34471)
* [[`455dd9cc76`](https://github.com/nodejs/node/commit/455dd9cc76)] - **doc**: match link text in index to doc headline (Rich Trott) [#34449](https://github.com/nodejs/node/pull/34449)
* [[`f4a63f3d9a`](https://github.com/nodejs/node/commit/f4a63f3d9a)] - **doc**: add AshCripps to collaborators (AshCripps) [#34494](https://github.com/nodejs/node/pull/34494)
* [[`7d058a4c01`](https://github.com/nodejs/node/commit/7d058a4c01)] - **doc**: add author-ready label ref to onboarding doc (Ruy Adorno) [#34381](https://github.com/nodejs/node/pull/34381)
* [[`a3c9f75b7e`](https://github.com/nodejs/node/commit/a3c9f75b7e)] - **doc**: add HarshithaKP to collaborators (Harshitha K P) [#34417](https://github.com/nodejs/node/pull/34417)
* [[`4b4eb5f130`](https://github.com/nodejs/node/commit/4b4eb5f130)] - **doc**: add rexagod to collaborators (Pranshu Srivastava) [#34457](https://github.com/nodejs/node/pull/34457)
* [[`29ad6fb34e`](https://github.com/nodejs/node/commit/29ad6fb34e)] - **doc**: add statement of purpose to documentation style guide (Rich Trott) [#34424](https://github.com/nodejs/node/pull/34424)
* [[`631dd21709`](https://github.com/nodejs/node/commit/631dd21709)] - **doc**: mark Node.js 13 as End-of-Life (Antoine du Hamel) [#34436](https://github.com/nodejs/node/pull/34436)
* [[`905e3d18c0`](https://github.com/nodejs/node/commit/905e3d18c0)] - **doc**: fix line length in worker\_threads.md (Jucke) [#34419](https://github.com/nodejs/node/pull/34419)
* [[`d67a2b8d38`](https://github.com/nodejs/node/commit/d67a2b8d38)] - **doc**: fix typos in n-api, tls and worker\_threads (Jucke) [#34419](https://github.com/nodejs/node/pull/34419)
* [[`39894f8842`](https://github.com/nodejs/node/commit/39894f8842)] - **doc**: add release key for Richard Lau (Richard Lau) [#34397](https://github.com/nodejs/node/pull/34397)
* [[`4a828c6c06`](https://github.com/nodejs/node/commit/4a828c6c06)] - **doc**: use correct identifier for callback argument (Rich Trott) [#34405](https://github.com/nodejs/node/pull/34405)
* [[`10830732f6`](https://github.com/nodejs/node/commit/10830732f6)] - **doc**: add changes metadata to TLS newSession event (Tobias Nießen) [#34294](https://github.com/nodejs/node/pull/34294)
* [[`10962c81e1`](https://github.com/nodejs/node/commit/10962c81e1)] - **doc**: introduce a triager role (Gireesh Punathil) [#34295](https://github.com/nodejs/node/pull/34295)
* [[`50fd2b9de9`](https://github.com/nodejs/node/commit/50fd2b9de9)] - **doc**: strengthen suggestion in errors.md (Rich Trott) [#34390](https://github.com/nodejs/node/pull/34390)
* [[`346c201c4e`](https://github.com/nodejs/node/commit/346c201c4e)] - **doc**: strengthen wording about fs.access() misuse (Rich Trott) [#34352](https://github.com/nodejs/node/pull/34352)
* [[`c28453aff4`](https://github.com/nodejs/node/commit/c28453aff4)] - **doc**: fix typo in assert.md (Ye-hyoung Kang) [#34316](https://github.com/nodejs/node/pull/34316)
* [[`f60e58b6c9`](https://github.com/nodejs/node/commit/f60e58b6c9)] - **doc,tools**: syntax highlight api docs at compile-time (Francisco Ryan Tolmasky I) [#34148](https://github.com/nodejs/node/pull/34148)
* [[`d90967b346`](https://github.com/nodejs/node/commit/d90967b346)] - **events**: re-use the same isTrusted getter (Anna Henningsen) [#34459](https://github.com/nodejs/node/pull/34459)
* [[`c93a898028`](https://github.com/nodejs/node/commit/c93a898028)] - **(SEMVER-MINOR)** **events**: expand NodeEventTarget functionality (Anna Henningsen) [#34057](https://github.com/nodejs/node/pull/34057)
* [[`9b91467aac`](https://github.com/nodejs/node/commit/9b91467aac)] - **http**: don't write error to socket (Robert Nagy) [#34465](https://github.com/nodejs/node/pull/34465)
* [[`098b193eab`](https://github.com/nodejs/node/commit/098b193eab)] - **http2**: avoid unnecessary buffer resize (Denys Otrishko) [#34480](https://github.com/nodejs/node/pull/34480)
* [[`3024927c9b`](https://github.com/nodejs/node/commit/3024927c9b)] - **lib**: initialize instance members in class constructors (Joyee Cheung) [#32984](https://github.com/nodejs/node/pull/32984)
* [[`82fad58ade`](https://github.com/nodejs/node/commit/82fad58ade)] - **lib**: simplify assignment (sapics) [#33718](https://github.com/nodejs/node/pull/33718)
* [[`e1199af50a`](https://github.com/nodejs/node/commit/e1199af50a)] - **module**: self referential modules in repl or `-r` (Daniele Belardi) [#32261](https://github.com/nodejs/node/pull/32261)
* [[`e7c64af404`](https://github.com/nodejs/node/commit/e7c64af404)] - **n-api**: run all finalizers via SetImmediate() (Gabriel Schulhof) [#34386](https://github.com/nodejs/node/pull/34386)
* [[`668632d531`](https://github.com/nodejs/node/commit/668632d531)] - **net**: allow wider regex in interface name (Stewart X Addison) [#34364](https://github.com/nodejs/node/pull/34364)
* [[`c05b63d8b2`](https://github.com/nodejs/node/commit/c05b63d8b2)] - **src**: skip weak references for memory tracking (Anna Henningsen) [#34469](https://github.com/nodejs/node/pull/34469)
* [[`b12211eeca`](https://github.com/nodejs/node/commit/b12211eeca)] - **src**: prefer internal fields in ModuleWrap (Anna Henningsen) [#34470](https://github.com/nodejs/node/pull/34470)
* [[`cbe6385880`](https://github.com/nodejs/node/commit/cbe6385880)] - **src**: remove unused variable in node\_file.cc (sapics) [#34317](https://github.com/nodejs/node/pull/34317)
* [[`d6ee1fd0c2`](https://github.com/nodejs/node/commit/d6ee1fd0c2)] - **src**: do not crash if ToggleAsyncHook fails during termination (Anna Henningsen) [#34362](https://github.com/nodejs/node/pull/34362)
* [[`bd9ab00acd`](https://github.com/nodejs/node/commit/bd9ab00acd)] - **(SEMVER-MINOR)** **src**: allow preventing SetPromiseRejectCallback (Shelley Vohr) [#34387](https://github.com/nodejs/node/pull/34387)
* [[`5c943588bc`](https://github.com/nodejs/node/commit/5c943588bc)] - **(SEMVER-MINOR)** **src**: allow setting a dir for all diagnostic output (AshCripps) [#33584](https://github.com/nodejs/node/pull/33584)
* [[`9d40af54a6`](https://github.com/nodejs/node/commit/9d40af54a6)] - **src**: avoid strcmp in SecureContext::Init (Tobias Nießen) [#34329](https://github.com/nodejs/node/pull/34329)
* [[`aef41e5b52`](https://github.com/nodejs/node/commit/aef41e5b52)] - **src**: refactor CertCbDone to avoid goto statement (Tobias Nießen) [#34325](https://github.com/nodejs/node/pull/34325)
* [[`3d4f608e42`](https://github.com/nodejs/node/commit/3d4f608e42)] - **stream**: rename opts to options (rickyes) [#34339](https://github.com/nodejs/node/pull/34339)
* [[`fced3ce5ad`](https://github.com/nodejs/node/commit/fced3ce5ad)] - **test**: add ref comment to test-regress-GH-814\_2 (Rich Trott) [#34516](https://github.com/nodejs/node/pull/34516)
* [[`d5c8b386c6`](https://github.com/nodejs/node/commit/d5c8b386c6)] - **test**: add ref comment to test-regress-GH-814 (Rich Trott) [#34516](https://github.com/nodejs/node/pull/34516)
* [[`cc279db29f`](https://github.com/nodejs/node/commit/cc279db29f)] - **test**: remove superfluous check in pummel/test-timers (Rich Trott) [#34488](https://github.com/nodejs/node/pull/34488)
* [[`3f11ba1c69`](https://github.com/nodejs/node/commit/3f11ba1c69)] - **test**: fix test-heapdump-zlib (Andrey Pechkurov) [#34499](https://github.com/nodejs/node/pull/34499)
* [[`81eaaa27d5`](https://github.com/nodejs/node/commit/81eaaa27d5)] - **test**: remove duplicate checks in pummel/test-timers (Rich Trott) [#34473](https://github.com/nodejs/node/pull/34473)
* [[`1a9138d679`](https://github.com/nodejs/node/commit/1a9138d679)] - **test**: delete invalid test (Anna Henningsen) [#34445](https://github.com/nodejs/node/pull/34445)
* [[`4e2f5fa907`](https://github.com/nodejs/node/commit/4e2f5fa907)] - **test**: fixup worker + source map test (Anna Henningsen) [#34446](https://github.com/nodejs/node/pull/34446)
* [[`cd35d00518`](https://github.com/nodejs/node/commit/cd35d00518)] - **test**: force resigning of app (Colin Ihrig) [#34331](https://github.com/nodejs/node/pull/34331)
* [[`eecb92c9da`](https://github.com/nodejs/node/commit/eecb92c9da)] - **test**: fix flaky test-watch-file (Rich Trott) [#34420](https://github.com/nodejs/node/pull/34420)
* [[`30da332314`](https://github.com/nodejs/node/commit/30da332314)] - **test**: fix flaky test-heapdump-http2 (Rich Trott) [#34415](https://github.com/nodejs/node/pull/34415)
* [[`77542a4a7a`](https://github.com/nodejs/node/commit/77542a4a7a)] - **test**: do not write to fixtures dir in test-watch-file (Rich Trott) [#34376](https://github.com/nodejs/node/pull/34376)
* [[`699da05b29`](https://github.com/nodejs/node/commit/699da05b29)] - **test**: remove common.localhostIPv6 (Rich Trott) [#34373](https://github.com/nodejs/node/pull/34373)
* [[`ec1393db63`](https://github.com/nodejs/node/commit/ec1393db63)] - **test**: fix test-net-pingpong pummel test for non-IPv6 hosts (Rich Trott) [#34359](https://github.com/nodejs/node/pull/34359)
* [[`8ca80427db`](https://github.com/nodejs/node/commit/8ca80427db)] - **test**: fix flaky test-net-connect-econnrefused (Rich Trott) [#34330](https://github.com/nodejs/node/pull/34330)
* [[`e9c7722ea4`](https://github.com/nodejs/node/commit/e9c7722ea4)] - **tls**: remove setMaxSendFragment guards (Tobias Nießen) [#34323](https://github.com/nodejs/node/pull/34323)
* [[`f4d61c7ce9`](https://github.com/nodejs/node/commit/f4d61c7ce9)] - **tools**: update ESLint to 7.5.0 (Colin Ihrig) [#34423](https://github.com/nodejs/node/pull/34423)
* [[`74da2c44ca`](https://github.com/nodejs/node/commit/74da2c44ca)] - **util**: improve getStringWidth performance (Ruben Bridgewater) [#33674](https://github.com/nodejs/node/pull/33674)
* [[`c9b652f13f`](https://github.com/nodejs/node/commit/c9b652f13f)] - **vm**: add tests for function declarations using \[\[DefineOwnProperty\]\] (ExE Boss) [#34032](https://github.com/nodejs/node/pull/34032)
* [[`0aa3809b6b`](https://github.com/nodejs/node/commit/0aa3809b6b)] - **(SEMVER-MINOR)** **worker**: make MessagePort inherit from EventTarget (Anna Henningsen) [#34057](https://github.com/nodejs/node/pull/34057)
* [[`252f37630a`](https://github.com/nodejs/node/commit/252f37630a)] - **zlib**: switch to lazy init for zlib streams (Andrey Pechkurov) [#34048](https://github.com/nodejs/node/pull/34048)

<a id="14.6.0"></a>
## 2020-07-21, Version 14.6.0 (Current), @MylesBorins

### Notable Changes

* **deps**:
  * upgrade to libuv 1.38.1 (Colin Ihrig) [#34187](https://github.com/nodejs/node/pull/34187)
  * upgrade npm to 6.14.6 (claudiahdz) [#34246](https://github.com/nodejs/node/pull/34246)
  * **(SEMVER-MINOR)** update V8 to 8.4.371.19 (Michaël Zasso) [#33579](https://github.com/nodejs/node/pull/33579)
* **module**:
  * **(SEMVER-MINOR)** doc only deprecation of module.parent (Antoine du HAMEL) [#32217](https://github.com/nodejs/node/pull/32217)
  * **(SEMVER-MINOR)** package "imports" field (Guy Bedford) [#34117](https://github.com/nodejs/node/pull/34117)
* **src**:
  * **(SEMVER-MINOR)** allow embedders to disable esm loader (Shelley Vohr) [#34060](https://github.com/nodejs/node/pull/34060)
* **tls**:
  * **(SEMVER-MINOR)** make 'createSecureContext' honor more options (Mateusz Krawczuk) [#33974](https://github.com/nodejs/node/pull/33974)
* **vm**:
  * **(SEMVER-MINOR)** add run-after-evaluate microtask mode (Anna Henningsen) [#34023](https://github.com/nodejs/node/pull/34023)
* **worker**:
  * **(SEMVER-MINOR)** add option to track unmanaged file descriptors (Anna Henningsen) [#34303](https://github.com/nodejs/node/pull/34303)
* **New Collaborators**:
  * add danielleadams to collaborators (Danielle Adams) [#34360](https://github.com/nodejs/node/pull/34360)
  * add ruyadorno to collaborators (Ruy Adorno) [#34297](https://github.com/nodejs/node/pull/34297)
  * add sxa as collaborator (Stewart X Addison) [#34338](https://github.com/nodejs/node/pull/34338)

### Commits

* [[`afec0d7f51`](https://github.com/nodejs/node/commit/afec0d7f51)] - **async_hooks**: improve resource stack performance (Anna Henningsen) [#34319](https://github.com/nodejs/node/pull/34319)
* [[`f340571301`](https://github.com/nodejs/node/commit/f340571301)] - **(SEMVER-MINOR)** **build**: reset embedder string to "-node.0" (Michaël Zasso) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`de250c136c`](https://github.com/nodejs/node/commit/de250c136c)] - **build**: recommend Python 3.8 to build on Windows (Michaël Zasso) [#34182](https://github.com/nodejs/node/pull/34182)
* [[`a130771d4f`](https://github.com/nodejs/node/commit/a130771d4f)] - **build,tools**: fix cmd\_regen\_makefile (Daniel Bevenius) [#34255](https://github.com/nodejs/node/pull/34255)
* [[`cfd4c8012d`](https://github.com/nodejs/node/commit/cfd4c8012d)] - **crypto**: move typechecking for timingSafeEqual into C++ (Anna Henningsen) [#34141](https://github.com/nodejs/node/pull/34141)
* [[`95afc2e50e`](https://github.com/nodejs/node/commit/95afc2e50e)] - **deps**: V8: update headers for ABI compatibility (Anna Henningsen) [#34356](https://github.com/nodejs/node/pull/34356)
* [[`2c9fd6ebd4`](https://github.com/nodejs/node/commit/2c9fd6ebd4)] - **deps**: V8: revert de4c0042cbe6 from upstream V8 (Anna Henningsen) [#34356](https://github.com/nodejs/node/pull/34356)
* [[`447b1e86a5`](https://github.com/nodejs/node/commit/447b1e86a5)] - **deps**: V8: re-add dummy Isolate::CheckMemoryPressure (Anna Henningsen) [#34356](https://github.com/nodejs/node/pull/34356)
* [[`2079fefacf`](https://github.com/nodejs/node/commit/2079fefacf)] - **deps**: V8: undo header change of 9dbab9bbdb979 (Anna Henningsen) [#34356](https://github.com/nodejs/node/pull/34356)
* [[`9f886c968c`](https://github.com/nodejs/node/commit/9f886c968c)] - **(SEMVER-MINOR)** **deps**: bump minimum icu version to 67 (Michaël Zasso) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`3fa7ad3375`](https://github.com/nodejs/node/commit/3fa7ad3375)] - **(SEMVER-MINOR)** **deps**: update V8 postmortem metadata script (Colin Ihrig) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`4c37837424`](https://github.com/nodejs/node/commit/4c37837424)] - **deps**: V8: cherry-pick eec10a2fd8fa (Stephen Belanger) [#33778](https://github.com/nodejs/node/pull/33778)
* [[`fb180ac110`](https://github.com/nodejs/node/commit/fb180ac110)] - **deps**: V8: backport 22014de00115 (Joyee Cheung) [#33300](https://github.com/nodejs/node/pull/33300)
* [[`01e788622c`](https://github.com/nodejs/node/commit/01e788622c)] - **(SEMVER-MINOR)** **deps**: V8: fix compilation on VS2017 (Jiawen Geng) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`f269dff06e`](https://github.com/nodejs/node/commit/f269dff06e)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick 9868b2aefa1a (Michaël Zasso) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`335e3861c3`](https://github.com/nodejs/node/commit/335e3861c3)] - **(SEMVER-MINOR)** **deps**: patch V8 to run on Xcode 8 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`355e2f2b6a`](https://github.com/nodejs/node/commit/355e2f2b6a)] - **(SEMVER-MINOR)** **deps**: V8: silence irrelevant warnings (Michaël Zasso) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`eb6ded61b7`](https://github.com/nodejs/node/commit/eb6ded61b7)] - **(SEMVER-MINOR)** **deps**: make v8.h compatible with VS2015 (Joao Reis) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`a4b71e02ca`](https://github.com/nodejs/node/commit/a4b71e02ca)] - **(SEMVER-MINOR)** **deps**: V8: forward declaration of `Rtl\*FunctionTable` (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`1e37442fdd`](https://github.com/nodejs/node/commit/1e37442fdd)] - **(SEMVER-MINOR)** **deps**: V8: patch register-arm64.h (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`eac35c6061`](https://github.com/nodejs/node/commit/eac35c6061)] - **(SEMVER-MINOR)** **deps**: patch V8 to run on older XCode versions (Ujjwal Sharma) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`51d86f4b59`](https://github.com/nodejs/node/commit/51d86f4b59)] - **(SEMVER-MINOR)** **deps**: V8: un-cherry-pick bd019bd (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`9cd523d148`](https://github.com/nodejs/node/commit/9cd523d148)] - **(SEMVER-MINOR)** **deps**: update V8 to 8.4.371.19 (Michaël Zasso) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`24f76cf004`](https://github.com/nodejs/node/commit/24f76cf004)] - **deps**: upgrade npm to 6.14.6 (claudiahdz) [#34246](https://github.com/nodejs/node/pull/34246)
* [[`a9ca4204e0`](https://github.com/nodejs/node/commit/a9ca4204e0)] - **deps**: upgrade to libuv 1.38.1 (Colin Ihrig) [#34187](https://github.com/nodejs/node/pull/34187)
* [[`601ed8ef7e`](https://github.com/nodejs/node/commit/601ed8ef7e)] - **deps**: V8: backport 2d5017a0fc02 (Benjamin Coe) [#34272](https://github.com/nodejs/node/pull/34272)
* [[`17174e69ce`](https://github.com/nodejs/node/commit/17174e69ce)] - **doc**: clarify conditional exports guidance (Guy Bedford) [#34306](https://github.com/nodejs/node/pull/34306)
* [[`1dd265384b`](https://github.com/nodejs/node/commit/1dd265384b)] - **doc**: reword warnings about sockets passed to subprocesses (Rich Trott) [#34273](https://github.com/nodejs/node/pull/34273)
* [[`ef31f179e0`](https://github.com/nodejs/node/commit/ef31f179e0)] - **doc**: sync deprecation numbers with v14.x (Myles Borins) [#34368](https://github.com/nodejs/node/pull/34368)
* [[`0b42e5d205`](https://github.com/nodejs/node/commit/0b42e5d205)] - **doc**: add danielleadams to collaborators (Danielle Adams) [#34360](https://github.com/nodejs/node/pull/34360)
* [[`1cc65332b0`](https://github.com/nodejs/node/commit/1cc65332b0)] - **doc**: buffer documentation improvements (James M Snell) [#34230](https://github.com/nodejs/node/pull/34230)
* [[`d11496174d`](https://github.com/nodejs/node/commit/d11496174d)] - **doc**: improve text in fs docs about omitting callbacks (Rich Trott) [#34307](https://github.com/nodejs/node/pull/34307)
* [[`d2c58948e9`](https://github.com/nodejs/node/commit/d2c58948e9)] - **doc**: add sxa as collaborator (Stewart X Addison) [#34338](https://github.com/nodejs/node/pull/34338)
* [[`d865be4cab`](https://github.com/nodejs/node/commit/d865be4cab)] - **doc**: move sebdeckers to emeritus (Rich Trott) [#34298](https://github.com/nodejs/node/pull/34298)
* [[`24fe55872f`](https://github.com/nodejs/node/commit/24fe55872f)] - **doc**: add ruyadorno to collaborators (Ruy Adorno) [#34297](https://github.com/nodejs/node/pull/34297)
* [[`e6776fe194`](https://github.com/nodejs/node/commit/e6776fe194)] - **doc**: move kfarnung to collaborator emeriti list (Rich Trott) [#34258](https://github.com/nodejs/node/pull/34258)
* [[`7416028f99`](https://github.com/nodejs/node/commit/7416028f99)] - **doc**: specify encoding in text/html examples (James M Snell) [#34222](https://github.com/nodejs/node/pull/34222)
* [[`9339f9f602`](https://github.com/nodejs/node/commit/9339f9f602)] - **doc**: document the ready event for Http2Stream (James M Snell) [#34221](https://github.com/nodejs/node/pull/34221)
* [[`25ac669be9`](https://github.com/nodejs/node/commit/25ac669be9)] - **doc**: add comment to example about 2xx status codes (James M Snell) [#34223](https://github.com/nodejs/node/pull/34223)
* [[`6f014d0b13`](https://github.com/nodejs/node/commit/6f014d0b13)] - **doc**: document that whitespace is ignored in base64 decoding (James M Snell) [#34227](https://github.com/nodejs/node/pull/34227)
* [[`431bfe177f`](https://github.com/nodejs/node/commit/431bfe177f)] - **doc**: add note about multiple sync events and once (James M Snell) [#34220](https://github.com/nodejs/node/pull/34220)
* [[`ffe6886de9`](https://github.com/nodejs/node/commit/ffe6886de9)] - **doc**: document behavior for once(ee, 'error') (James M Snell) [#34225](https://github.com/nodejs/node/pull/34225)
* [[`a6a656abaa`](https://github.com/nodejs/node/commit/a6a656abaa)] - **doc**: document security issues with url.parse() (James M Snell) [#34226](https://github.com/nodejs/node/pull/34226)
* [[`abfab9892b`](https://github.com/nodejs/node/commit/abfab9892b)] - **doc**: replace http to https of link urls (sapics) [#34158](https://github.com/nodejs/node/pull/34158)
* [[`2e20cd4fde`](https://github.com/nodejs/node/commit/2e20cd4fde)] - **doc**: remove errors that were never released (Rich Trott) [#34197](https://github.com/nodejs/node/pull/34197)
* [[`c83d98619d`](https://github.com/nodejs/node/commit/c83d98619d)] - **doc**: move ERR\_FEATURE\_UNAVAILABLE\_ON\_PLATFORM to current errors (Rich Trott) [#34196](https://github.com/nodejs/node/pull/34196)
* [[`59bb6d6663`](https://github.com/nodejs/node/commit/59bb6d6663)] - **doc**: move digitalinfinity to emeritus (Rich Trott) [#34191](https://github.com/nodejs/node/pull/34191)
* [[`39d6ecdea9`](https://github.com/nodejs/node/commit/39d6ecdea9)] - **doc**: move gibfahn to emeritus (Rich Trott) [#34190](https://github.com/nodejs/node/pull/34190)
* [[`938de338ef`](https://github.com/nodejs/node/commit/938de338ef)] - **doc**: specify how fs.WriteStream/ReadStreams are created (James M Snell) [#34188](https://github.com/nodejs/node/pull/34188)
* [[`326b854e6e`](https://github.com/nodejs/node/commit/326b854e6e)] - **doc**: remove parenthetical \\r\\n comment in http and http2 docs (Rich Trott) [#34178](https://github.com/nodejs/node/pull/34178)
* [[`a2dd2589c1`](https://github.com/nodejs/node/commit/a2dd2589c1)] - **doc**: remove stability from unreleased errors (Rich Trott) [#33764](https://github.com/nodejs/node/pull/33764)
* [[`8dd8b1a8be`](https://github.com/nodejs/node/commit/8dd8b1a8be)] - **doc**: util.debuglog callback (Bradley Meck) [#33856](https://github.com/nodejs/node/pull/33856)
* [[`aaba1c08dc`](https://github.com/nodejs/node/commit/aaba1c08dc)] - **doc**: update wording in "Two reading modes" (Julien Poissonnier) [#34119](https://github.com/nodejs/node/pull/34119)
* [[`6aa0dac362`](https://github.com/nodejs/node/commit/6aa0dac362)] - **doc**: clarify that the ctx argument is optional (Luigi Pinca) [#34097](https://github.com/nodejs/node/pull/34097)
* [[`1558800217`](https://github.com/nodejs/node/commit/1558800217)] - **doc**: add a reference to the list of OpenSSL flags. (Mateusz Krawczuk) [#34050](https://github.com/nodejs/node/pull/34050)
* [[`25d310b631`](https://github.com/nodejs/node/commit/25d310b631)] - **doc**: no longer maintain a CNA structure (Sam Roberts) [#33639](https://github.com/nodejs/node/pull/33639)
* [[`5ae2b74350`](https://github.com/nodejs/node/commit/5ae2b74350)] - **doc**: use consistent naming in stream doc (Saleem) [#30506](https://github.com/nodejs/node/pull/30506)
* [[`a0cfa62338`](https://github.com/nodejs/node/commit/a0cfa62338)] - **doc**: clarify how to read process.stdin (Anentropic) [#27350](https://github.com/nodejs/node/pull/27350)
* [[`e8184554ba`](https://github.com/nodejs/node/commit/e8184554ba)] - **doc**: fix entry for `napi\_create\_external\_buffer` (Gabriel Schulhof) [#34125](https://github.com/nodejs/node/pull/34125)
* [[`167a21a66a`](https://github.com/nodejs/node/commit/167a21a66a)] - **doc**: fix source link margin to sub-header mark (Rodion Abdurakhimov) [#33664](https://github.com/nodejs/node/pull/33664)
* [[`146538de65`](https://github.com/nodejs/node/commit/146538de65)] - **doc**: improve async\_hooks asynchronous context example (Denys Otrishko) [#33730](https://github.com/nodejs/node/pull/33730)
* [[`e386188775`](https://github.com/nodejs/node/commit/e386188775)] - **doc**: clarify esm conditional exports prose (Derek Lewis) [#33886](https://github.com/nodejs/node/pull/33886)
* [[`e273edf943`](https://github.com/nodejs/node/commit/e273edf943)] - **doc**: Add maxTotalSockets option to agent constructor (rickyes) [#34013](https://github.com/nodejs/node/pull/34013)
* [[`ab6b786e9d`](https://github.com/nodejs/node/commit/ab6b786e9d)] - **doc**: add streams to the pipeline function signature (rickyes) [#34153](https://github.com/nodejs/node/pull/34153)
* [[`9f0bf5c9e1`](https://github.com/nodejs/node/commit/9f0bf5c9e1)] - **doc**: improve triaging text in issues.md (Rich Trott) [#34164](https://github.com/nodejs/node/pull/34164)
* [[`22c1fbf4cb`](https://github.com/nodejs/node/commit/22c1fbf4cb)] - **doc**: simply dns.ADDRCONFIG language (Rich Trott) [#34155](https://github.com/nodejs/node/pull/34155)
* [[`7fc56ebd0d`](https://github.com/nodejs/node/commit/7fc56ebd0d)] - **doc**: remove "considered" in errors.md (Rich Trott) [#34152](https://github.com/nodejs/node/pull/34152)
* [[`e33c09cb3a`](https://github.com/nodejs/node/commit/e33c09cb3a)] - **doc**: simplify and clarify ReferenceError material in errors.md (Rich Trott) [#34151](https://github.com/nodejs/node/pull/34151)
* [[`af9e6f6e1b`](https://github.com/nodejs/node/commit/af9e6f6e1b)] - **doc**: add http highlight grammar (Derek Lewis) [#33785](https://github.com/nodejs/node/pull/33785)
* [[`26ecdf8ade`](https://github.com/nodejs/node/commit/26ecdf8ade)] - **doc**: move sam-github to TSC Emeriti (Sam Roberts) [#34095](https://github.com/nodejs/node/pull/34095)
* [[`78a4d97b82`](https://github.com/nodejs/node/commit/78a4d97b82)] - **doc**: change "considered experimental" to "experimental" in n-api.md (Rich Trott) [#34129](https://github.com/nodejs/node/pull/34129)
* [[`da5fde6594`](https://github.com/nodejs/node/commit/da5fde6594)] - **doc**: changed "considered experimental" to "experimental" in cli.md (Rich Trott) [#34128](https://github.com/nodejs/node/pull/34128)
* [[`49d2d49336`](https://github.com/nodejs/node/commit/49d2d49336)] - **doc**: improve text in issues.md (falguniraina) [#33973](https://github.com/nodejs/node/pull/33973)
* [[`9d30f0542c`](https://github.com/nodejs/node/commit/9d30f0542c)] - **doc**: change "currently not considered public" to "not supported" (Rich Trott) [#34114](https://github.com/nodejs/node/pull/34114)
* [[`64bd518f26`](https://github.com/nodejs/node/commit/64bd518f26)] - **doc**: clarify that APIs are no longer experimental (Rich Trott) [#34113](https://github.com/nodejs/node/pull/34113)
* [[`ee6ccef091`](https://github.com/nodejs/node/commit/ee6ccef091)] - **doc**: clarify O\_EXCL text in fs.md (Rich Trott) [#34096](https://github.com/nodejs/node/pull/34096)
* [[`05a69e2e88`](https://github.com/nodejs/node/commit/05a69e2e88)] - **doc**: clarify ambiguous rdev description (Rich Trott) [#34094](https://github.com/nodejs/node/pull/34094)
* [[`4927fed9ea`](https://github.com/nodejs/node/commit/4927fed9ea)] - **doc**: make minor improvements to paragraph in child\_process.md (Rich Trott) [#34063](https://github.com/nodejs/node/pull/34063)
* [[`585f3a5f84`](https://github.com/nodejs/node/commit/585f3a5f84)] - **doc**: improve paragraph in esm.md (Rich Trott) [#34064](https://github.com/nodejs/node/pull/34064)
* [[`556e55db72`](https://github.com/nodejs/node/commit/556e55db72)] - **doc**: clarify require/import mutual exclusivity (Guy Bedford) [#33832](https://github.com/nodejs/node/pull/33832)
* [[`eb04ba3080`](https://github.com/nodejs/node/commit/eb04ba3080)] - **doc**: add dynamic source code links (Alec Davidson) [#33996](https://github.com/nodejs/node/pull/33996)
* [[`2ca6a45ba9`](https://github.com/nodejs/node/commit/2ca6a45ba9)] - **doc**: mention errors thrown by methods called on an unbound dgram.Socket (Mateusz Krawczuk) [#33983](https://github.com/nodejs/node/pull/33983)
* [[`b8a17ccc9a`](https://github.com/nodejs/node/commit/b8a17ccc9a)] - **doc**: document n-api callback scope usage (Gabriel Schulhof) [#33915](https://github.com/nodejs/node/pull/33915)
* [[`3b268094cc`](https://github.com/nodejs/node/commit/3b268094cc)] - **doc**: use sentence-case for headings in docs (Rich Trott) [#33889](https://github.com/nodejs/node/pull/33889)
* [[`280cd967d3`](https://github.com/nodejs/node/commit/280cd967d3)] - **domain**: fix unintentional deprecation warning (Anna Henningsen) [#34245](https://github.com/nodejs/node/pull/34245)
* [[`96ebd5f352`](https://github.com/nodejs/node/commit/96ebd5f352)] - **http**: add note about timer unref (Robert Nagy) [#34143](https://github.com/nodejs/node/pull/34143)
* [[`16160e654f`](https://github.com/nodejs/node/commit/16160e654f)] - ***Revert*** "**http2**: streamline OnStreamRead streamline memory accounting" (Rich Trott) [#34315](https://github.com/nodejs/node/pull/34315)
* [[`8bafba2e56`](https://github.com/nodejs/node/commit/8bafba2e56)] - **lib**: always initialize esm loader callbackMap (Shelley Vohr) [#34127](https://github.com/nodejs/node/pull/34127)
* [[`daf2abf393`](https://github.com/nodejs/node/commit/daf2abf393)] - **lib**: replace http to https of comment link urls (sapics) [#34158](https://github.com/nodejs/node/pull/34158)
* [[`8f8d16849c`](https://github.com/nodejs/node/commit/8f8d16849c)] - **meta**: make issue template mobile friendly and address nits (Derek Lewis) [#34243](https://github.com/nodejs/node/pull/34243)
* [[`de58eb6286`](https://github.com/nodejs/node/commit/de58eb6286)] - **meta**: add N-API to codeowners coverage (Michael Dawson) [#34039](https://github.com/nodejs/node/pull/34039)
* [[`4dc89c6d30`](https://github.com/nodejs/node/commit/4dc89c6d30)] - **meta**: fixup CODEOWNERS so it hopefully works (James M Snell) [#34147](https://github.com/nodejs/node/pull/34147)
* [[`8d7330be0e`](https://github.com/nodejs/node/commit/8d7330be0e)] - **(SEMVER-MINOR)** **module**: deprecate module.parent (Antoine du HAMEL) [#32217](https://github.com/nodejs/node/pull/32217)
* [[`1ae76bd075`](https://github.com/nodejs/node/commit/1ae76bd075)] - **(SEMVER-MINOR)** **module**: package "imports" field (Guy Bedford) [#34117](https://github.com/nodejs/node/pull/34117)
* [[`0e1361cb8b`](https://github.com/nodejs/node/commit/0e1361cb8b)] - **net**: doc deprecate bufferSize (Robert Nagy) [#34088](https://github.com/nodejs/node/pull/34088)
* [[`b7e9b43b2f`](https://github.com/nodejs/node/commit/b7e9b43b2f)] - **net**: fix bufferSize (Robert Nagy) [#34088](https://github.com/nodejs/node/pull/34088)
* [[`02ea320e0c`](https://github.com/nodejs/node/commit/02ea320e0c)] - **policy**: add startup benchmark and make SRI lazier (Bradley Farias) [#29527](https://github.com/nodejs/node/pull/29527)
* [[`73d6792a05`](https://github.com/nodejs/node/commit/73d6792a05)] - **repl**: support --loader option in builtin REPL (Michaël Zasso) [#33437](https://github.com/nodejs/node/pull/33437)
* [[`b20e6ed94e`](https://github.com/nodejs/node/commit/b20e6ed94e)] - **repl**: fix verb conjugation in deprecation message (Rich Trott) [#34198](https://github.com/nodejs/node/pull/34198)
* [[`b878e3223e`](https://github.com/nodejs/node/commit/b878e3223e)] - **src**: add callback scope for native immediates (Anna Henningsen) [#34366](https://github.com/nodejs/node/pull/34366)
* [[`0f6805d507`](https://github.com/nodejs/node/commit/0f6805d507)] - **(SEMVER-MINOR)** **src**: add option to track unmanaged file descriptors (Anna Henningsen) [#34303](https://github.com/nodejs/node/pull/34303)
* [[`e4c7b59665`](https://github.com/nodejs/node/commit/e4c7b59665)] - **(SEMVER-MINOR)** **src**: allow embedders to disable esm loader (Shelley Vohr) [#34060](https://github.com/nodejs/node/pull/34060)
* [[`9c12e53d47`](https://github.com/nodejs/node/commit/9c12e53d47)] - **src**: remove redundant snprintf (Anna Henningsen) [#34282](https://github.com/nodejs/node/pull/34282)
* [[`844bf770f8`](https://github.com/nodejs/node/commit/844bf770f8)] - **src**: use FromMaybe instead of ToLocal in GetCert (Daniel Bevenius) [#34276](https://github.com/nodejs/node/pull/34276)
* [[`ec876eecc0`](https://github.com/nodejs/node/commit/ec876eecc0)] - **src**: add GetCipherValue function (Daniel Bevenius) [#34287](https://github.com/nodejs/node/pull/34287)
* [[`9c98af71db`](https://github.com/nodejs/node/commit/9c98af71db)] - **src**: exit explicitly after printing V8 help (Anna Henningsen) [#34136](https://github.com/nodejs/node/pull/34136)
* [[`3e3d908c81`](https://github.com/nodejs/node/commit/3e3d908c81)] - **src**: add encoding\_type variable in WritePrivateKey (Daniel Bevenius) [#34181](https://github.com/nodejs/node/pull/34181)
* [[`ed0f5697d8`](https://github.com/nodejs/node/commit/ed0f5697d8)] - **src**: fix minor comment typo in KeyObjectData (Daniel Bevenius) [#34167](https://github.com/nodejs/node/pull/34167)
* [[`8f7ed40fc4`](https://github.com/nodejs/node/commit/8f7ed40fc4)] - **src**: fix unused namespace member (Nikola Glavina) [#34212](https://github.com/nodejs/node/pull/34212)
* [[`e378b681d0`](https://github.com/nodejs/node/commit/e378b681d0)] - **src**: remove unused fields from IsolateData (Anna Henningsen) [#34139](https://github.com/nodejs/node/pull/34139)
* [[`b2cd87e611`](https://github.com/nodejs/node/commit/b2cd87e611)] - **src,doc,test**: remove String::New default parameter (Anna Henningsen) [#34248](https://github.com/nodejs/node/pull/34248)
* [[`41c80f6abe`](https://github.com/nodejs/node/commit/41c80f6abe)] - **stream**: destroy wrapped streams on error (Robert Nagy) [#34102](https://github.com/nodejs/node/pull/34102)
* [[`1af8943622`](https://github.com/nodejs/node/commit/1af8943622)] - **(SEMVER-MINOR)** **test**: remove test/v8-updates/test-postmortem-metadata.js (Colin Ihrig) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`58dfeac133`](https://github.com/nodejs/node/commit/58dfeac133)] - **test**: use mustCall() in pummel test (Rich Trott) [#34327](https://github.com/nodejs/node/pull/34327)
* [[`28ce378e17`](https://github.com/nodejs/node/commit/28ce378e17)] - **test**: fix flaky test-http2-reset-flood (Rich Trott) [#34318](https://github.com/nodejs/node/pull/34318)
* [[`060c95a3b1`](https://github.com/nodejs/node/commit/060c95a3b1)] - **test**: add n-api null checks for conversions (Gabriel Schulhof) [#34142](https://github.com/nodejs/node/pull/34142)
* [[`3ee8f5342c`](https://github.com/nodejs/node/commit/3ee8f5342c)] - **test**: add regression tests for HTTP parser crash (Anna Henningsen) [#34250](https://github.com/nodejs/node/pull/34250)
* [[`6925ef3b1c`](https://github.com/nodejs/node/commit/6925ef3b1c)] - **test**: add WASI test for file resizing (Colin Ihrig) [#31617](https://github.com/nodejs/node/pull/31617)
* [[`1aad61eeec`](https://github.com/nodejs/node/commit/1aad61eeec)] - **test**: add issue ref for known\_issues test (Rich Trott) [#34267](https://github.com/nodejs/node/pull/34267)
* [[`ec9b49a9b9`](https://github.com/nodejs/node/commit/ec9b49a9b9)] - **test**: add known issue for fs.open() keeping event loop open (Rich Trott) [#34228](https://github.com/nodejs/node/pull/34228)
* [[`38b3c2a300`](https://github.com/nodejs/node/commit/38b3c2a300)] - **test**: add arrayOfStreams to pipeline (rickyes) [#34156](https://github.com/nodejs/node/pull/34156)
* [[`0f9bafd03d`](https://github.com/nodejs/node/commit/0f9bafd03d)] - **test**: skip an ipv6 test on IBM i (Xu Meng) [#34209](https://github.com/nodejs/node/pull/34209)
* [[`a38219f962`](https://github.com/nodejs/node/commit/a38219f962)] - **test**: add regression test for C++-created Buffer transfer (Anna Henningsen) [#34140](https://github.com/nodejs/node/pull/34140)
* [[`09faebd9ad`](https://github.com/nodejs/node/commit/09faebd9ad)] - **test**: replace deprecated function call from test-repl-history-navigation (Rich Trott) [#34199](https://github.com/nodejs/node/pull/34199)
* [[`bddc99ec7f`](https://github.com/nodejs/node/commit/bddc99ec7f)] - **test**: skip some IBM i unsupported test cases (Xu Meng) [#34118](https://github.com/nodejs/node/pull/34118)
* [[`f5691fa6b6`](https://github.com/nodejs/node/commit/f5691fa6b6)] - **test**: report actual error code on failure (Richard Lau) [#34134](https://github.com/nodejs/node/pull/34134)
* [[`46d183c86e`](https://github.com/nodejs/node/commit/46d183c86e)] - **test**: update test-child-process-spawn-loop for Python 3 (Richard Lau) [#34071](https://github.com/nodejs/node/pull/34071)
* [[`a89bcf72fb`](https://github.com/nodejs/node/commit/a89bcf72fb)] - **(SEMVER-MINOR)** **tls**: make 'createSecureContext' honor more options (Mateusz Krawczuk) [#33974](https://github.com/nodejs/node/pull/33974)
* [[`fbcd1fa0f4`](https://github.com/nodejs/node/commit/fbcd1fa0f4)] - **tls**: remove unnecessary close listener (Robert Nagy) [#34105](https://github.com/nodejs/node/pull/34105)
* [[`4e2fa439c9`](https://github.com/nodejs/node/commit/4e2fa439c9)] - **(SEMVER-MINOR)** **tools**: update V8 gypfiles for 8.4 (Ujjwal Sharma) [#33579](https://github.com/nodejs/node/pull/33579)
* [[`440642d00b`](https://github.com/nodejs/node/commit/440642d00b)] - **tools**: remove lint-js.js (Rich Trott) [#30955](https://github.com/nodejs/node/pull/30955)
* [[`e0206bafe6`](https://github.com/nodejs/node/commit/e0206bafe6)] - **util**: restrict custom inspect function + vm.Context interaction (Anna Henningsen) [#33690](https://github.com/nodejs/node/pull/33690)
* [[`70c4045aa5`](https://github.com/nodejs/node/commit/70c4045aa5)] - **(SEMVER-MINOR)** **vm**: add run-after-evaluate microtask mode (Anna Henningsen) [#34023](https://github.com/nodejs/node/pull/34023)
* [[`6be685a99d`](https://github.com/nodejs/node/commit/6be685a99d)] - **wasi**: add reactor support (Gus Caplan) [#34046](https://github.com/nodejs/node/pull/34046)
* [[`1bc4def18f`](https://github.com/nodejs/node/commit/1bc4def18f)] - **worker**: fix nested uncaught exception handling (Anna Henningsen) [#34310](https://github.com/nodejs/node/pull/34310)
* [[`9e04070d3c`](https://github.com/nodejs/node/commit/9e04070d3c)] - **(SEMVER-MINOR)** **worker**: add option to track unmanaged file descriptors (Anna Henningsen) [#34303](https://github.com/nodejs/node/pull/34303)
* [[`105d5607a8`](https://github.com/nodejs/node/commit/105d5607a8)] - **zlib**: remove redundant variable in zlibBufferOnEnd (Andrey Pechkurov) [#34072](https://github.com/nodejs/node/pull/34072)

<a id="14.5.0"></a>
## 2020-06-30, Version 14.5.0 (Current), @codebytere

### Notable Changes

#### V8 engine is updated to version 8.3

This version includes performance improvements and now allows WebAssembly
modules to request memories up to 4GB in size.

For more information, have a look at the [official V8 blog post](https://v8.dev/blog/v8-release-83).

Contributed by Matheus Marchini and Michaël Zasso - [#33376](https://github.com/nodejs/node/pull/33376).

#### Initial experimental implementation of EventTarget

This version introduces an new experimental API `EventTarget`, which provides a DOM interface implemented by objects that can receive events and may have listeners for them.

It is an adaptation of the Web API EventTarget.

Example Usage:

```js
const target = getEventTargetSomehow();

target.addEventListener('foo', (event) => {
  console.log('foo event happened!');
});
```

Contributed by James Snell - [#33556](https://github.com/nodejs/node/pull/33556).

### Semver-Minor Commits

* [[`4ccaa537d4`](https://github.com/nodejs/node/commit/4ccaa537d4)] - **(SEMVER-MINOR)** **build**: reset embedder string to "-node.0" (Michaël Zasso) [#33376](https://github.com/nodejs/node/pull/33376)
* [[`d194d20828`](https://github.com/nodejs/node/commit/d194d20828)] - **(SEMVER-MINOR)** **cli**: add alias for report-directory to make it consistent (AshCripps) [#33587](https://github.com/nodejs/node/pull/33587)
* [[`70398dbf60`](https://github.com/nodejs/node/commit/70398dbf60)] - **(SEMVER-MINOR)** **crypto**: allow KeyObjects in postMessage (Tobias Nießen) [#33360](https://github.com/nodejs/node/pull/33360)
* [[`9b7ba87aa6`](https://github.com/nodejs/node/commit/9b7ba87aa6)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick 0d6debcc5f08 (Michaël Zasso) [#33376](https://github.com/nodejs/node/pull/33376)
* [[`ce1a1ae621`](https://github.com/nodejs/node/commit/ce1a1ae621)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick 74d50c5063b3 (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`aa7267a344`](https://github.com/nodejs/node/commit/aa7267a344)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick e29c62b74854 (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`1512757a22`](https://github.com/nodejs/node/commit/1512757a22)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick 3f8dc4b2e5ba (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`3d9cf4bde6`](https://github.com/nodejs/node/commit/3d9cf4bde6)] - **(SEMVER-MINOR)** **deps**: V8: cherry-pick e1eac1b16c96 (Milad Farazmand) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`cdeade308e`](https://github.com/nodejs/node/commit/cdeade308e)] - **(SEMVER-MINOR)** **deps**: fix V8 8.3 on SmartOS (Colin Ihrig) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`883840bc17`](https://github.com/nodejs/node/commit/883840bc17)] - **(SEMVER-MINOR)** **deps**: patch V8 to run on Xcode 8 (Matheus Marchini) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`3831a541fb`](https://github.com/nodejs/node/commit/3831a541fb)] - **(SEMVER-MINOR)** **deps**: V8: silence irrelevant warnings (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`e2fc08f216`](https://github.com/nodejs/node/commit/e2fc08f216)] - **(SEMVER-MINOR)** **deps**: make v8.h compatible with VS2015 (Joao Reis) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`74b623bd51`](https://github.com/nodejs/node/commit/74b623bd51)] - **(SEMVER-MINOR)** **deps**: V8: forward declaration of `Rtl\*FunctionTable` (Refael Ackermann) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`0f5764aec2`](https://github.com/nodejs/node/commit/0f5764aec2)] - **(SEMVER-MINOR)** **deps**: V8: patch register-arm64.h (Refael Ackermann) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`be773fc3cf`](https://github.com/nodejs/node/commit/be773fc3cf)] - **(SEMVER-MINOR)** **deps**: patch V8 to run on older XCode versions (Ujjwal Sharma) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`7aa41c6e6f`](https://github.com/nodejs/node/commit/7aa41c6e6f)] - **(SEMVER-MINOR)** **deps**: V8: un-cherry-pick bd019bd (Refael Ackermann) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`ce901e3906`](https://github.com/nodejs/node/commit/ce901e3906)] - **(SEMVER-MINOR)** **deps**: update V8 dtrace & postmortem metadata (Colin Ihrig) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`1123425dd1`](https://github.com/nodejs/node/commit/1123425dd1)] - **(SEMVER-MINOR)** **deps**: update V8 to 8.3.110.9 (Michaël Zasso) [#33376](https://github.com/nodejs/node/pull/33376)
* [[`1c70b18da8`](https://github.com/nodejs/node/commit/1c70b18da8)] - **(SEMVER-MINOR)** **events**: initial implementation of experimental EventTarget (James M Snell) [#33556](https://github.com/nodejs/node/pull/33556)
* [[`cf97c56dab`](https://github.com/nodejs/node/commit/cf97c56dab)] - **(SEMVER-MINOR)** **fs**: implement lutimes (Maël Nison) [#33399](https://github.com/nodejs/node/pull/33399)
* [[`a24b8df7fb`](https://github.com/nodejs/node/commit/a24b8df7fb)] - **(SEMVER-MINOR)** **http**: expose host and protocol on ClientRequest (wenningplus) [#33803](https://github.com/nodejs/node/pull/33803)
* [[`507a2ef31c`](https://github.com/nodejs/node/commit/507a2ef31c)] - **(SEMVER-MINOR)** **http**: add maxTotalSockets to agent class (rickyes) [#33617](https://github.com/nodejs/node/pull/33617)
* [[`e1e3ae1567`](https://github.com/nodejs/node/commit/e1e3ae1567)] - **(SEMVER-MINOR)** **http**: return this from OutgoingMessage#destroy() (Colin Ihrig) [#32789](https://github.com/nodejs/node/pull/32789)
* [[`d87031def4`](https://github.com/nodejs/node/commit/d87031def4)] - **(SEMVER-MINOR)** **http**: return this from ClientRequest#destroy() (Colin Ihrig) [#32789](https://github.com/nodejs/node/pull/32789)
* [[`c7959557db`](https://github.com/nodejs/node/commit/c7959557db)] - **(SEMVER-MINOR)** **http**: return this from IncomingMessage#destroy() (Colin Ihrig) [#32789](https://github.com/nodejs/node/pull/32789)
* [[`a3a0c0e0fc`](https://github.com/nodejs/node/commit/a3a0c0e0fc)] - **(SEMVER-MINOR)** **http**: added scheduling option to http agent (delvedor) [#33278](https://github.com/nodejs/node/pull/33278)
* [[`e3fd2f5a48`](https://github.com/nodejs/node/commit/e3fd2f5a48)] - **(SEMVER-MINOR)** **http2**: return this for Http2ServerRequest#setTimeout (Pranshu Srivastava) [#33994](https://github.com/nodejs/node/pull/33994)
* [[`7ccb021ffc`](https://github.com/nodejs/node/commit/7ccb021ffc)] - **(SEMVER-MINOR)** **http2**: do not modify explicity set date headers (Pranshu Srivastava) [#33160](https://github.com/nodejs/node/pull/33160)
* [[`f66bb57c13`](https://github.com/nodejs/node/commit/f66bb57c13)] - **(SEMVER-MINOR)** **process**: add unhandled-rejection throw and warn-with-error-code (Dan Fabulich) [#33475](https://github.com/nodejs/node/pull/33475)
* [[`33020256de`](https://github.com/nodejs/node/commit/33020256de)] - **(SEMVER-MINOR)** **src**: store key data in separate class (Tobias Nießen) [#33360](https://github.com/nodejs/node/pull/33360)
* [[`44b9d08344`](https://github.com/nodejs/node/commit/44b9d08344)] - **(SEMVER-MINOR)** **src**: add NativeKeyObject base class (Tobias Nießen) [#33360](https://github.com/nodejs/node/pull/33360)
* [[`13e633873e`](https://github.com/nodejs/node/commit/13e633873e)] - **(SEMVER-MINOR)** **src**: rename internal key handles to KeyObjectHandle (Tobias Nießen) [#33360](https://github.com/nodejs/node/pull/33360)
* [[`a3d0b0e2d7`](https://github.com/nodejs/node/commit/a3d0b0e2d7)] - **(SEMVER-MINOR)** **src**: add equality operators for BaseObjectPtr (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`0720d1ff24`](https://github.com/nodejs/node/commit/0720d1ff24)] - **(SEMVER-MINOR)** **src**: introduce BaseObject base FunctionTemplate (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`5362fef3f5`](https://github.com/nodejs/node/commit/5362fef3f5)] - **(SEMVER-MINOR)** **src**: add public APIs to manage v8::TracingController (Anna Henningsen) [#33850](https://github.com/nodejs/node/pull/33850)
* [[`db2d1ca51b`](https://github.com/nodejs/node/commit/db2d1ca51b)] - **(SEMVER-MINOR)** **stream**: runtime deprecate Transform.\_transformState (Robert Nagy) [#32763](https://github.com/nodejs/node/pull/32763)
* [[`b6da77756e`](https://github.com/nodejs/node/commit/b6da77756e)] - **(SEMVER-MINOR)** **test**: stop testing --interpreted-frames-native-stack for s390x (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`5cad007408`](https://github.com/nodejs/node/commit/5cad007408)] - **(SEMVER-MINOR)** **test**: fix test-zlib-unused-weak on V8 8.2 (Matheus Marchini) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`2c59f9bbe2`](https://github.com/nodejs/node/commit/2c59f9bbe2)] - **(SEMVER-MINOR)** **tools**: update V8 gypfiles for V8 8.3 (Michaël Zasso) [#32831](https://github.com/nodejs/node/pull/32831)
* [[`0ef6e0426f`](https://github.com/nodejs/node/commit/0ef6e0426f)] - **(SEMVER-MINOR)** **win**: allow skipping the supported platform check (João Reis) [#33176](https://github.com/nodejs/node/pull/33176)
* [[`4e42eb5e14`](https://github.com/nodejs/node/commit/4e42eb5e14)] - **(SEMVER-MINOR)** **worker**: add public method for marking objects as untransferable (Anna Henningsen) [#33979](https://github.com/nodejs/node/pull/33979)
* [[`4a37180b09`](https://github.com/nodejs/node/commit/4a37180b09)] - **(SEMVER-MINOR)** **worker**: emit `'messagerror'` events for failed deserialization (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`9692208a91`](https://github.com/nodejs/node/commit/9692208a91)] - **(SEMVER-MINOR)** **worker**: allow passing JS wrapper objects via postMessage (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`eaccf842eb`](https://github.com/nodejs/node/commit/eaccf842eb)] - **(SEMVER-MINOR)** **worker**: allow transferring/cloning generic BaseObjects (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`5b1fd10048`](https://github.com/nodejs/node/commit/5b1fd10048)] - **(SEMVER-MINOR)** **worker,fs**: make FileHandle transferable (Anna Henningsen) [#33772](https://github.com/nodejs/node/pull/33772)
* [[`c1f625fe1f`](https://github.com/nodejs/node/commit/c1f625fe1f)] - **(SEMVER-MINOR)** **zlib**: add `maxOutputLength` option (unknown) [#33516](https://github.com/nodejs/node/pull/33516)

### Semver-Patch Commits

* [[`ef05e1526a`](https://github.com/nodejs/node/commit/ef05e1526a)] - **async_hooks**: callback trampoline for MakeCallback (Stephen Belanger) [#33801](https://github.com/nodejs/node/pull/33801)
* [[`0eed22d6ed`](https://github.com/nodejs/node/commit/0eed22d6ed)] - **benchmark**: fix EventTarget benchmark (Brian White) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`bf56decc79`](https://github.com/nodejs/node/commit/bf56decc79)] - **benchmark**: fix async-resource benchmark (Anna Henningsen) [#33642](https://github.com/nodejs/node/pull/33642)
* [[`26269be510`](https://github.com/nodejs/node/commit/26269be510)] - **benchmark**: fixing http\_server\_for\_chunky\_client.js (Adrian Estrada) [#33271](https://github.com/nodejs/node/pull/33271)
* [[`c31d5145d9`](https://github.com/nodejs/node/commit/c31d5145d9)] - **buffer**: remove hoisted variable (Nikolai Vavilov) [#33470](https://github.com/nodejs/node/pull/33470)
* [[`43fd4746e9`](https://github.com/nodejs/node/commit/43fd4746e9)] - **build**: configure byte order for mips targets (Ben Noordhuis) [#33898](https://github.com/nodejs/node/pull/33898)
* [[`ebb2fb81fa`](https://github.com/nodejs/node/commit/ebb2fb81fa)] - **build**: add target specific build\_type variable (Daniel Bevenius) [#33925](https://github.com/nodejs/node/pull/33925)
* [[`e8f7670b77`](https://github.com/nodejs/node/commit/e8f7670b77)] - **build**: add LINT\_CPP\_FILES to checkimports check (Daniel Bevenius) [#33697](https://github.com/nodejs/node/pull/33697)
* [[`1355d35a61`](https://github.com/nodejs/node/commit/1355d35a61)] - **build**: output dots in "Build from tarball" action (Michaël Zasso) [#33696](https://github.com/nodejs/node/pull/33696)
* [[`153f5eda0e`](https://github.com/nodejs/node/commit/153f5eda0e)] - **build**: fix compiling addons with older versions of Node.js (Richard Lau) [#33688](https://github.com/nodejs/node/pull/33688)
* [[`7a4c689912`](https://github.com/nodejs/node/commit/7a4c689912)] - **build**: fix node.gyp config (gengjiawen) [#33685](https://github.com/nodejs/node/pull/33685)
* [[`1f7a65529d`](https://github.com/nodejs/node/commit/1f7a65529d)] - **build**: add --v8-lite-mode flag (Maciej Kacper Jagiełło) [#33541](https://github.com/nodejs/node/pull/33541)
* [[`3ac05b75ca`](https://github.com/nodejs/node/commit/3ac05b75ca)] - **build**: zlib build error on Windows on Arm (Richard Townsend) [#33511](https://github.com/nodejs/node/pull/33511)
* [[`fc032247e0`](https://github.com/nodejs/node/commit/fc032247e0)] - **build**: fix GetCurrentThreadStackLimits error on Windows on Arm (Richard Townsend) [#33511](https://github.com/nodejs/node/pull/33511)
* [[`e393e879cf`](https://github.com/nodejs/node/commit/e393e879cf)] - **build**: fix python-version selection with actions (Richard Lau) [#33589](https://github.com/nodejs/node/pull/33589)
* [[`8ed25eda60`](https://github.com/nodejs/node/commit/8ed25eda60)] - **build**: fix inability to detect correct python command in configure (Eli Schwartz) [#32925](https://github.com/nodejs/node/pull/32925)
* [[`8b887c4462`](https://github.com/nodejs/node/commit/8b887c4462)] - **build**: fix makefile script on windows (Thomas) [#33136](https://github.com/nodejs/node/pull/33136)
* [[`85ce30fe57`](https://github.com/nodejs/node/commit/85ce30fe57)] - **build**: run full test suite in ASAN action (Anna Henningsen) [#33170](https://github.com/nodejs/node/pull/33170)
* [[`71c4d9174e`](https://github.com/nodejs/node/commit/71c4d9174e)] - **build,win**: add support for MSVC cross-compilation (Richard Townsend) [#32867](https://github.com/nodejs/node/pull/32867)
* [[`ac7946eb08`](https://github.com/nodejs/node/commit/ac7946eb08)] - **build,win**: add support for MSVC cross-compilation (Richard Townsend) [#32867](https://github.com/nodejs/node/pull/32867)
* [[`22b5ec19a2`](https://github.com/nodejs/node/commit/22b5ec19a2)] - **cli**: support --experimental-top-level-await in NODE\_OPTIONS (Dan Fabulich) [#33495](https://github.com/nodejs/node/pull/33495)
* [[`0a7f13e26b`](https://github.com/nodejs/node/commit/0a7f13e26b)] - **configure**: account for CLANG\_VENDOR when checking for llvm version (Nathan Blair) [#33860](https://github.com/nodejs/node/pull/33860)
* [[`a6a74ae1d5`](https://github.com/nodejs/node/commit/a6a74ae1d5)] - **console**: name console functions appropriately (Ruben Bridgewater) [#33524](https://github.com/nodejs/node/pull/33524)
* [[`9d24f71d45`](https://github.com/nodejs/node/commit/9d24f71d45)] - **console**: mark special console properties as non-enumerable (Ruben Bridgewater) [#33524](https://github.com/nodejs/node/pull/33524)
* [[`bce99867f7`](https://github.com/nodejs/node/commit/bce99867f7)] - **console**: remove dead code (Ruben Bridgewater) [#33524](https://github.com/nodejs/node/pull/33524)
* [[`134ed0eea3`](https://github.com/nodejs/node/commit/134ed0eea3)] - **crypto**: fix wrong error message (Ben Bucksch) [#33482](https://github.com/nodejs/node/pull/33482)
* [[`5957afc31a`](https://github.com/nodejs/node/commit/5957afc31a)] - **deps**: V8: cherry-pick 767e65f945e7 (Gus Caplan) [#33859](https://github.com/nodejs/node/pull/33859)
* [[`162092ea2a`](https://github.com/nodejs/node/commit/162092ea2a)] - **deps**: V8: cherry-pick eec10a2fd8fa (Stephen Belanger) [#33778](https://github.com/nodejs/node/pull/33778)
* [[`499c7402b1`](https://github.com/nodejs/node/commit/499c7402b1)] - **deps**: V8: cherry-pick 4e1bf2bc92bd (Milad Farazmand) [#33702](https://github.com/nodejs/node/pull/33702)
* [[`0524c7ad5d`](https://github.com/nodejs/node/commit/0524c7ad5d)] - **deps**: V8: cherry-pick b5939c758924 (Milad Farazmand) [#33702](https://github.com/nodejs/node/pull/33702)
* [[`7ad6cfa005`](https://github.com/nodejs/node/commit/7ad6cfa005)] - **deps**: V8: backport 22014de00115 (Joyee Cheung) [#33300](https://github.com/nodejs/node/pull/33300)
* [[`817befde11`](https://github.com/nodejs/node/commit/817befde11)] - **deps**: V8: backport bb9f0c2b2fe9 (Joyee Cheung) [#33300](https://github.com/nodejs/node/pull/33300)
* [[`8f82692999`](https://github.com/nodejs/node/commit/8f82692999)] - **deps**: V8: backport ea0719b8ed08 (Joyee Cheung) [#33300](https://github.com/nodejs/node/pull/33300)
* [[`773d76ea04`](https://github.com/nodejs/node/commit/773d76ea04)] - **deps**: uvwasi: cherry-pick 9e75217 (Colin Ihrig) [#33521](https://github.com/nodejs/node/pull/33521)
* [[`748720e7b6`](https://github.com/nodejs/node/commit/748720e7b6)] - **deps**: V8: cherry-pick 548f6c81d424 (Dominykas Blyžė) [#33484](https://github.com/nodejs/node/pull/33484)
* [[`b0bce9b2a4`](https://github.com/nodejs/node/commit/b0bce9b2a4)] - **deps**: update node-inspect to v2.0.0 (Jan Krems) [#33447](https://github.com/nodejs/node/pull/33447)
* [[`ac459b34e7`](https://github.com/nodejs/node/commit/ac459b34e7)] - **deps**: V8: cherry-pick fa3e37e511ee (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`2bc79f5b50`](https://github.com/nodejs/node/commit/2bc79f5b50)] - **deps**: V8: cherry-pick 2db93c023379 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`8d47e8bf7b`](https://github.com/nodejs/node/commit/8d47e8bf7b)] - **deps**: update to uvwasi 0.0.9 (Colin Ihrig) [#33445](https://github.com/nodejs/node/pull/33445)
* [[`9d6fd4599d`](https://github.com/nodejs/node/commit/9d6fd4599d)] - **deps**: upgrade to libuv 1.38.0 (Colin Ihrig) [#33446](https://github.com/nodejs/node/pull/33446)
* [[`33a662ad2d`](https://github.com/nodejs/node/commit/33a662ad2d)] - **deps**: update icu to include tzdata2020a (Shelley Vohr) [#33362](https://github.com/nodejs/node/pull/33362)
* [[`f151bde312`](https://github.com/nodejs/node/commit/f151bde312)] - **(SEMVER-MINOR)** **dgram**: allow typed arrays in .send() (Sarat Addepalli) [#22413](https://github.com/nodejs/node/pull/22413)
* [[`d4442b15bf`](https://github.com/nodejs/node/commit/d4442b15bf)] - **dns**: make dns.Resolver timeout configurable (Ben Noordhuis) [#33472](https://github.com/nodejs/node/pull/33472)
* [[`eb55d9e4b1`](https://github.com/nodejs/node/commit/eb55d9e4b1)] - **dns**: use ternary operator simplify statement (Wenning Zhang) [#33234](https://github.com/nodejs/node/pull/33234)
* [[`d61de303c9`](https://github.com/nodejs/node/commit/d61de303c9)] - **doc**: specify maxHeaderCount alias for maxHeaderListPairs (Pranshu Srivastava) [#33519](https://github.com/nodejs/node/pull/33519)
* [[`4323346f5a`](https://github.com/nodejs/node/commit/4323346f5a)] - **doc**: add allowed info strings to style guide (Derek Lewis) [#34024](https://github.com/nodejs/node/pull/34024)
* [[`0dbad26db4`](https://github.com/nodejs/node/commit/0dbad26db4)] - **doc**: fix lexical sorting of bottom-references in http doc (Pranshu Srivastava) [#34007](https://github.com/nodejs/node/pull/34007)
* [[`ec07e61f6a`](https://github.com/nodejs/node/commit/ec07e61f6a)] - **doc**: clarify thread-safe function references (legendecas) [#33871](https://github.com/nodejs/node/pull/33871)
* [[`5a4dcfcf4c`](https://github.com/nodejs/node/commit/5a4dcfcf4c)] - **doc**: use npm team for npm upgrades in collaborator guide (Rich Trott) [#33999](https://github.com/nodejs/node/pull/33999)
* [[`319707add2`](https://github.com/nodejs/node/commit/319707add2)] - **doc**: correct default values in http2 docs (Rich Trott) [#33997](https://github.com/nodejs/node/pull/33997)
* [[`b4d0eebe7c`](https://github.com/nodejs/node/commit/b4d0eebe7c)] - **doc**: use a single space between sentences (Rich Trott) [#33995](https://github.com/nodejs/node/pull/33995)
* [[`24105a7f44`](https://github.com/nodejs/node/commit/24105a7f44)] - **doc**: piping from async generators using pipeline() (WilliamConnatser) [#33992](https://github.com/nodejs/node/pull/33992)
* [[`9590d81349`](https://github.com/nodejs/node/commit/9590d81349)] - **doc**: revise text in dns module documentation introduction (Rich Trott) [#33986](https://github.com/nodejs/node/pull/33986)
* [[`ed26e8e2fb`](https://github.com/nodejs/node/commit/ed26e8e2fb)] - **doc**: update fs.md (Shakil-Shahadat) [#33820](https://github.com/nodejs/node/pull/33820)
* [[`6dc541778e`](https://github.com/nodejs/node/commit/6dc541778e)] - **doc**: warn that tls.connect() doesn't set SNI (Alba Mendez) [#33855](https://github.com/nodejs/node/pull/33855)
* [[`d9c78ac270`](https://github.com/nodejs/node/commit/d9c78ac270)] - **doc**: fix lexical sorting of bottom-references in dns doc (Rich Trott) [#33987](https://github.com/nodejs/node/pull/33987)
* [[`98228b25af`](https://github.com/nodejs/node/commit/98228b25af)] - **doc**: change "GitHub Repo" to "Code repository" (Rich Trott) [#33985](https://github.com/nodejs/node/pull/33985)
* [[`645cd481e9`](https://github.com/nodejs/node/commit/645cd481e9)] - **doc**: use Class: consistently (Rich Trott) [#33978](https://github.com/nodejs/node/pull/33978)
* [[`72e2fd315e`](https://github.com/nodejs/node/commit/72e2fd315e)] - **doc**: update WASM code sample (Pragyan Das) [#33626](https://github.com/nodejs/node/pull/33626)
* [[`894ec7d5c6`](https://github.com/nodejs/node/commit/894ec7d5c6)] - **doc**: standardize on sentence case for headers (Rich Trott) [#33889](https://github.com/nodejs/node/pull/33889)
* [[`61de26a2f3`](https://github.com/nodejs/node/commit/61de26a2f3)] - **doc**: link readable.\_read in stream.md (Pranshu Srivastava) [#33767](https://github.com/nodejs/node/pull/33767)
* [[`76fe2a93a9`](https://github.com/nodejs/node/commit/76fe2a93a9)] - **doc**: specify default encoding in writable.write (Pranshu Srivastava) [#33765](https://github.com/nodejs/node/pull/33765)
* [[`2427d6544b`](https://github.com/nodejs/node/commit/2427d6544b)] - **doc**: move --force-context-aware option in cli.md (Daniel Bevenius) [#33823](https://github.com/nodejs/node/pull/33823)
* [[`fdaf0ca550`](https://github.com/nodejs/node/commit/fdaf0ca550)] - **doc**: add snippet for AsyncResource and EE integration (Andrey Pechkurov) [#33751](https://github.com/nodejs/node/pull/33751)
* [[`8f5ac3865c`](https://github.com/nodejs/node/commit/8f5ac3865c)] - **doc**: use single quotes in --tls-cipher-list (Daniel Bevenius) [#33709](https://github.com/nodejs/node/pull/33709)
* [[`922c13c6bb`](https://github.com/nodejs/node/commit/922c13c6bb)] - **doc**: fix misc. mislabeled code block info strings (Derek Lewis) [#33548](https://github.com/nodejs/node/pull/33548)
* [[`114d77e30b`](https://github.com/nodejs/node/commit/114d77e30b)] - **doc**: standardize constructor doc header layout (Rich Trott) [#33781](https://github.com/nodejs/node/pull/33781)
* [[`b10d20385e`](https://github.com/nodejs/node/commit/b10d20385e)] - **doc**: update V8 inspector example (Colin Ihrig) [#33758](https://github.com/nodejs/node/pull/33758)
* [[`785760448b`](https://github.com/nodejs/node/commit/785760448b)] - **doc**: fix linting in doc-style-guide.md (Pranshu Srivastava) [#33787](https://github.com/nodejs/node/pull/33787)
* [[`2288840a8f`](https://github.com/nodejs/node/commit/2288840a8f)] - **doc**: remove "currently" from repl.md (Rich Trott) [#33756](https://github.com/nodejs/node/pull/33756)
* [[`cc0f827182`](https://github.com/nodejs/node/commit/cc0f827182)] - **doc**: remove "currently" from events.md (Rich Trott) [#33756](https://github.com/nodejs/node/pull/33756)
* [[`4a738e6462`](https://github.com/nodejs/node/commit/4a738e6462)] - **doc**: remove "currently" from vm.md (Rich Trott) [#33756](https://github.com/nodejs/node/pull/33756)
* [[`bb29a8177f`](https://github.com/nodejs/node/commit/bb29a8177f)] - **doc**: remove "currently" from addons.md (Rich Trott) [#33756](https://github.com/nodejs/node/pull/33756)
* [[`f0597d9a6e`](https://github.com/nodejs/node/commit/f0597d9a6e)] - **doc**: remove "currently" from util.md (Rich Trott) [#33756](https://github.com/nodejs/node/pull/33756)
* [[`095efac2ef`](https://github.com/nodejs/node/commit/095efac2ef)] - **doc**: add formatting for version numbers to doc-style-guide.md (Rich Trott) [#33755](https://github.com/nodejs/node/pull/33755)
* [[`843ab3eb94`](https://github.com/nodejs/node/commit/843ab3eb94)] - **doc**: change "pre Node.js v0.10" to "prior to Node.js 0.10" (Rich Trott) [#33754](https://github.com/nodejs/node/pull/33754)
* [[`b565897996`](https://github.com/nodejs/node/commit/b565897996)] - **doc**: remove default parameter value from header (Rich Trott) [#33752](https://github.com/nodejs/node/pull/33752)
* [[`ebf2378731`](https://github.com/nodejs/node/commit/ebf2378731)] - **doc**: fix typo in cli.md for report-dir (AshCripps) [#33725](https://github.com/nodejs/node/pull/33725)
* [[`16b69818ba`](https://github.com/nodejs/node/commit/16b69818ba)] - **doc**: remove shell dollar signs without output (Nick Schonning) [#33692](https://github.com/nodejs/node/pull/33692)
* [[`b3d500f949`](https://github.com/nodejs/node/commit/b3d500f949)] - **doc**: add lint disabling comment for collaborator list (Rich Trott) [#33719](https://github.com/nodejs/node/pull/33719)
* [[`61bb789fa0`](https://github.com/nodejs/node/commit/61bb789fa0)] - **doc**: use consistent Default: in events (Colin Ihrig) [#33678](https://github.com/nodejs/node/pull/33678)
* [[`1e4edd8d75`](https://github.com/nodejs/node/commit/1e4edd8d75)] - **doc**: remove "it is important" (Colin Ihrig) [#33678](https://github.com/nodejs/node/pull/33678)
* [[`cb8b9ec98a`](https://github.com/nodejs/node/commit/cb8b9ec98a)] - **doc**: fix urls to avoid redirection (sapics) [#33614](https://github.com/nodejs/node/pull/33614)
* [[`c184929975`](https://github.com/nodejs/node/commit/c184929975)] - **doc**: improve buffer.md a tiny bit (Tom Nagle) [#33547](https://github.com/nodejs/node/pull/33547)
* [[`6d25b5753a`](https://github.com/nodejs/node/commit/6d25b5753a)] - **doc**: normalize Markdown code block info strings (Derek Lewis) [#33542](https://github.com/nodejs/node/pull/33542)
* [[`e7c3890901`](https://github.com/nodejs/node/commit/e7c3890901)] - **doc**: normalize JavaScript code block info strings (Derek Lewis) [#33531](https://github.com/nodejs/node/pull/33531)
* [[`352adcb437`](https://github.com/nodejs/node/commit/352adcb437)] - **doc**: outline when origin is set to unhandledRejection (Ruben Bridgewater) [#33530](https://github.com/nodejs/node/pull/33530)
* [[`94177dae8e`](https://github.com/nodejs/node/commit/94177dae8e)] - **doc**: add --experimental-top-level-await to man page (Colin Ihrig) [#33529](https://github.com/nodejs/node/pull/33529)
* [[`8e3a0d7773`](https://github.com/nodejs/node/commit/8e3a0d7773)] - **doc**: update ```txt ```fandamental and ```raw code blocks (Zeke Sikelianos) [#33028](https://github.com/nodejs/node/pull/33028)
* [[`4cc391b495`](https://github.com/nodejs/node/commit/4cc391b495)] - **doc**: normalize shell code block info strings (Derek Lewis) [#33486](https://github.com/nodejs/node/pull/33486)
* [[`24ada7acd4`](https://github.com/nodejs/node/commit/24ada7acd4)] - **doc**: normalize C code block info strings (Derek Lewis) [#33507](https://github.com/nodejs/node/pull/33507)
* [[`8c04e61f16`](https://github.com/nodejs/node/commit/8c04e61f16)] - **doc**: normalize Bash code block info strings (Derek Lewis) [#33510](https://github.com/nodejs/node/pull/33510)
* [[`7c87fc1c48`](https://github.com/nodejs/node/commit/7c87fc1c48)] - **doc**: correct tls.rootCertificates to match implementation (Eric Bickle) [#33313](https://github.com/nodejs/node/pull/33313)
* [[`0c2b7c0adf`](https://github.com/nodejs/node/commit/0c2b7c0adf)] - **doc**: fix Buffer.from(object) documentation (Nikolai Vavilov) [#33327](https://github.com/nodejs/node/pull/33327)
* [[`de608c3124`](https://github.com/nodejs/node/commit/de608c3124)] - **doc**: fix typo in pathToFileURL example (Antoine du HAMEL) [#33418](https://github.com/nodejs/node/pull/33418)
* [[`23cf39ab78`](https://github.com/nodejs/node/commit/23cf39ab78)] - **doc**: eliminate dead space in API section's sidebar (John Gardner) [#33469](https://github.com/nodejs/node/pull/33469)
* [[`95e7a80cbf`](https://github.com/nodejs/node/commit/95e7a80cbf)] - **doc**: mention --experimental-top-level-await flag (dfabulich) [#33473](https://github.com/nodejs/node/pull/33473)
* [[`64410f206e`](https://github.com/nodejs/node/commit/64410f206e)] - **doc**: normalize C++ code block info strings (Derek Lewis) [#33483](https://github.com/nodejs/node/pull/33483)
* [[`c8f79d80a4`](https://github.com/nodejs/node/commit/c8f79d80a4)] - **doc**: fixed a grammatical error in path.md (Deep310) [#33489](https://github.com/nodejs/node/pull/33489)
* [[`500bad1103`](https://github.com/nodejs/node/commit/500bad1103)] - **doc**: correct CommonJS self-resolve spec (Guy Bedford) [#33391](https://github.com/nodejs/node/pull/33391)
* [[`4e74f050a7`](https://github.com/nodejs/node/commit/4e74f050a7)] - **doc**: fix readline key binding documentation (Ruben Bridgewater) [#33361](https://github.com/nodejs/node/pull/33361)
* [[`7c553cd4f6`](https://github.com/nodejs/node/commit/7c553cd4f6)] - **doc**: claim ABI version 85 for Electron 11 (Shelley Vohr) [#33375](https://github.com/nodejs/node/pull/33375)
* [[`4cc5e9668f`](https://github.com/nodejs/node/commit/4cc5e9668f)] - **doc**: document module.path (Antoine du Hamel) [#33323](https://github.com/nodejs/node/pull/33323)
* [[`c1fe152132`](https://github.com/nodejs/node/commit/c1fe152132)] - **doc**: add fs.open() multiple constants example (Ethan Arrowood) [#33281](https://github.com/nodejs/node/pull/33281)
* [[`b02cfef510`](https://github.com/nodejs/node/commit/b02cfef510)] - **doc**: fix typos in handle scope descriptions (Tobias Nießen) [#33267](https://github.com/nodejs/node/pull/33267)
* [[`d4e871424f`](https://github.com/nodejs/node/commit/d4e871424f)] - **doc**: update function description for `decipher.setAAD` (Jonathan Buhacoff) [#33095](https://github.com/nodejs/node/pull/33095)
* [[`e2484b24cb`](https://github.com/nodejs/node/commit/e2484b24cb)] - **doc**: add comment about highWaterMark limit (Benjamin Gruenbaum) [#33432](https://github.com/nodejs/node/pull/33432)
* [[`b8c88891a6`](https://github.com/nodejs/node/commit/b8c88891a6)] - **doc**: clarify about the Node.js-only extensions in perf\_hooks (Joyee Cheung) [#33199](https://github.com/nodejs/node/pull/33199)
* [[`d1efdb29b4`](https://github.com/nodejs/node/commit/d1efdb29b4)] - **doc**: document ICU time zone data update process (Andrew Paprocki) [#30364](https://github.com/nodejs/node/pull/30364)
* [[`1d918b67ca`](https://github.com/nodejs/node/commit/1d918b67ca)] - **doc,stream**: split finish and end events into separate entries (Rich Trott) [#33881](https://github.com/nodejs/node/pull/33881)
* [[`af9fb5969d`](https://github.com/nodejs/node/commit/af9fb5969d)] - **doc,tools**: properly syntax highlight API ref docs (Derek Lewis) [#33442](https://github.com/nodejs/node/pull/33442)
* [[`122d2b5c02`](https://github.com/nodejs/node/commit/122d2b5c02)] - **domain**: remove native domain code (Stephen Belanger) [#33801](https://github.com/nodejs/node/pull/33801)
* [[`e060060aa2`](https://github.com/nodejs/node/commit/e060060aa2)] - **errors**: fully inspect errors on exit (Ruben Bridgewater) [#33523](https://github.com/nodejs/node/pull/33523)
* [[`aca07f428e`](https://github.com/nodejs/node/commit/aca07f428e)] - **errors**: skip fatal error highlighting on windows (Thomas) [#33132](https://github.com/nodejs/node/pull/33132)
* [[`50adccadc1`](https://github.com/nodejs/node/commit/50adccadc1)] - **esm**: fix loader hooks doc annotations (Derek Lewis) [#33563](https://github.com/nodejs/node/pull/33563)
* [[`5bef20c2fc`](https://github.com/nodejs/node/commit/5bef20c2fc)] - **esm**: share package.json cache between ESM and CJS loaders (Kirill Shatskiy) [#33229](https://github.com/nodejs/node/pull/33229)
* [[`828d5d22eb`](https://github.com/nodejs/node/commit/828d5d22eb)] - **esm**: doc & validate source values for formats (Bradley Farias) [#32202](https://github.com/nodejs/node/pull/32202)
* [[`2724514f53`](https://github.com/nodejs/node/commit/2724514f53)] - **event**: cancelBubble is a property (Benjamin Gruenbaum) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`c9dec0c0f0`](https://github.com/nodejs/node/commit/c9dec0c0f0)] - **event**: cancelBubble is a property (Benjamin Gruenbaum) [#33613](https://github.com/nodejs/node/pull/33613)
* [[`0c32920a82`](https://github.com/nodejs/node/commit/0c32920a82)] - **events**: fix add-remove-add case in EventTarget (Anna Henningsen) [#34056](https://github.com/nodejs/node/pull/34056)
* [[`c34f4743c4`](https://github.com/nodejs/node/commit/c34f4743c4)] - **events**: improve argument handling, start passive (James M Snell) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`ea1a2d7bc9`](https://github.com/nodejs/node/commit/ea1a2d7bc9)] - **events**: support dispatching event from event (James M Snell) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`5ce153365e`](https://github.com/nodejs/node/commit/5ce153365e)] - **events**: add event-target tests (James M Snell) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`91b6c093b1`](https://github.com/nodejs/node/commit/91b6c093b1)] - **events**: support event handlers (Benjamin Gruenbaum) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`b392fdd4aa`](https://github.com/nodejs/node/commit/b392fdd4aa)] - **events**: expose Event statics (Benjamin Gruenbaum) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`cd3a1429a3`](https://github.com/nodejs/node/commit/cd3a1429a3)] - **events**: Handle a range of this values for dispatchEvent (Zirak) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`aa1cb3f186`](https://github.com/nodejs/node/commit/aa1cb3f186)] - **events**: fix EventTarget support (Benjamin Gruenbaum) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`0f0f4e0c40`](https://github.com/nodejs/node/commit/0f0f4e0c40)] - **events**: fix depth in customInspectSymbol and clean up (Denys Otrishko) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`6ce3293cc4`](https://github.com/nodejs/node/commit/6ce3293cc4)] - **events**: use internal/validators in event\_target.js (Denys Otrishko) [#34015](https://github.com/nodejs/node/pull/34015)
* [[`eb01214ab2`](https://github.com/nodejs/node/commit/eb01214ab2)] - **events**: use property, primordials (Benjamin Gruenbaum) [#33775](https://github.com/nodejs/node/pull/33775)
* [[`667195ef8f`](https://github.com/nodejs/node/commit/667195ef8f)] - **events**: improve listeners() performance (Brian White) [#33863](https://github.com/nodejs/node/pull/33863)
* [[`f1b0291d82`](https://github.com/nodejs/node/commit/f1b0291d82)] - **events**: lazy load perf\_hooks for EventTarget (James M Snell) [#33717](https://github.com/nodejs/node/pull/33717)
* [[`c291ce599c`](https://github.com/nodejs/node/commit/c291ce599c)] - **events**: improve arrayClone performance (Brian White) [#33774](https://github.com/nodejs/node/pull/33774)
* [[`a3ef2b7335`](https://github.com/nodejs/node/commit/a3ef2b7335)] - **events**: support useCapture boolean (Benjamin Gruenbaum) [#33618](https://github.com/nodejs/node/pull/33618)
* [[`2e6eceac5c`](https://github.com/nodejs/node/commit/2e6eceac5c)] - **events**: set target property to null (Benjamin Gruenbaum) [#33615](https://github.com/nodejs/node/pull/33615)
* [[`bc2e821ccc`](https://github.com/nodejs/node/commit/bc2e821ccc)] - **events**: deal with no argument case (Benjamin Gruenbaum) [#33611](https://github.com/nodejs/node/pull/33611)
* [[`e7bce2e03a`](https://github.com/nodejs/node/commit/e7bce2e03a)] - **events**: deal with Symbol() passed to event constructor (Benjamin Gruenbaum) [#33612](https://github.com/nodejs/node/pull/33612)
* [[`27c90efce0`](https://github.com/nodejs/node/commit/27c90efce0)] - **events**: variable originalListener is useless (fuxingZhang) [#33596](https://github.com/nodejs/node/pull/33596)
* [[`2a29ced050`](https://github.com/nodejs/node/commit/2a29ced050)] - **events**: fix event-target enumerable keys (Benjamin Gruenbaum) [#33616](https://github.com/nodejs/node/pull/33616)
* [[`f3d0d3089d`](https://github.com/nodejs/node/commit/f3d0d3089d)] - **events**: add tests, better toString (Benjamin Gruenbaum) [#33622](https://github.com/nodejs/node/pull/33622)
* [[`95cbfcec99`](https://github.com/nodejs/node/commit/95cbfcec99)] - **fs**: fix readdir failure when libuv returns UV\_DIRENT\_UNKNOWN (Kirill Shatskiy) [#33395](https://github.com/nodejs/node/pull/33395)
* [[`b894df860a`](https://github.com/nodejs/node/commit/b894df860a)] - **fs**: fix realpath inode link caching (Denys Otrishko) [#33945](https://github.com/nodejs/node/pull/33945)
* [[`b280c86213`](https://github.com/nodejs/node/commit/b280c86213)] - **fs**: support util.promisify for fs.readv (Lucas Holmquist) [#33590](https://github.com/nodejs/node/pull/33590)
* [[`2c03661860`](https://github.com/nodejs/node/commit/2c03661860)] - **fs**: unify style in preprocessSymlinkDestination (Bartosz Sosnowski) [#33496](https://github.com/nodejs/node/pull/33496)
* [[`b675ea0272`](https://github.com/nodejs/node/commit/b675ea0272)] - **fs**: replace checkPosition with validateInteger (rickyes) [#33277](https://github.com/nodejs/node/pull/33277)
* [[`a90b96f338`](https://github.com/nodejs/node/commit/a90b96f338)] - **fs**: refactor the import of internalUtil (rickyes) [#33296](https://github.com/nodejs/node/pull/33296)
* [[`a0a61b81a5`](https://github.com/nodejs/node/commit/a0a61b81a5)] - **http**: used already defined validator for boolean check (Yash Ladha) [#33731](https://github.com/nodejs/node/pull/33731)
* [[`6dbd63c8ba`](https://github.com/nodejs/node/commit/6dbd63c8ba)] - ***Revert*** "**http**: set IncomingMessage.destroyed" (Robert Nagy) [#33686](https://github.com/nodejs/node/pull/33686)
* [[`feb6e1ffb8`](https://github.com/nodejs/node/commit/feb6e1ffb8)] - **http**: don't throw on `Uint8Array`s for `http.ServerResponse#write` (Pranshu Srivastava) [#33155](https://github.com/nodejs/node/pull/33155)
* [[`bcdf7e94be`](https://github.com/nodejs/node/commit/bcdf7e94be)] - **http**: simplify Agent initialization (himself65) [#33551](https://github.com/nodejs/node/pull/33551)
* [[`c2aad813c0`](https://github.com/nodejs/node/commit/c2aad813c0)] - **http**: tidy up exposure of header validation (Osher) [#33371](https://github.com/nodejs/node/pull/33371)
* [[`0752d2309f`](https://github.com/nodejs/node/commit/0752d2309f)] - **http2**: always call callback on Http2ServerResponse#end (Pranshu Srivastava) [#33911](https://github.com/nodejs/node/pull/33911)
* [[`d8aeafb4bf`](https://github.com/nodejs/node/commit/d8aeafb4bf)] - **http2**: add writable\* properties to compat api (Pranshu Srivastava) [#33506](https://github.com/nodejs/node/pull/33506)
* [[`0b34c4fb75`](https://github.com/nodejs/node/commit/0b34c4fb75)] - **http2**: add type checks for Http2ServerResponse.end (Pranshu Srivastava) [#33146](https://github.com/nodejs/node/pull/33146)
* [[`cc74f3c67c`](https://github.com/nodejs/node/commit/cc74f3c67c)] - **http2**: use `Object.create(null)` for `getHeaders` (Pranshu Srivastava) [#33188](https://github.com/nodejs/node/pull/33188)
* [[`8457033d83`](https://github.com/nodejs/node/commit/8457033d83)] - **http2**: reuse .\_onTimeout() in Http2Session and Http2Stream classes (rickyes) [#33354](https://github.com/nodejs/node/pull/33354)
* [[`c972ce200e`](https://github.com/nodejs/node/commit/c972ce200e)] - **http2**: comment on usage of `Object.create(null)` (Pranshu Srivastava) [#33183](https://github.com/nodejs/node/pull/33183)
* [[`e58f14fee7`](https://github.com/nodejs/node/commit/e58f14fee7)] - **inspector**: drop 'chrome-' from inspector url (Colin Ihrig) [#33758](https://github.com/nodejs/node/pull/33758)
* [[`42df2baa21`](https://github.com/nodejs/node/commit/42df2baa21)] - **inspector**: throw error when activating an already active inspector (Joyee Cheung) [#33015](https://github.com/nodejs/node/pull/33015)
* [[`c9489f2f23`](https://github.com/nodejs/node/commit/c9489f2f23)] - **internal**: rename error-serdes for consistency (Evan Lucas) [#33793](https://github.com/nodejs/node/pull/33793)
* [[`b7690da65e`](https://github.com/nodejs/node/commit/b7690da65e)] - **lib**: improve debuglog() performance (Brian White) [#32260](https://github.com/nodejs/node/pull/32260)
* [[`b6ef6c8476`](https://github.com/nodejs/node/commit/b6ef6c8476)] - **lib**: remove manual exception handling in queueMicrotask (Gus Caplan) [#33859](https://github.com/nodejs/node/pull/33859)
* [[`ec01867623`](https://github.com/nodejs/node/commit/ec01867623)] - **lib**: replace charCodeAt with fixed Unicode (rickyes) [#32758](https://github.com/nodejs/node/pull/32758)
* [[`76123b9ae7`](https://github.com/nodejs/node/commit/76123b9ae7)] - **lib**: add Int16Array primordials (Sebastien Ahkrin) [#31205](https://github.com/nodejs/node/pull/31205)
* [[`59d435ed4d`](https://github.com/nodejs/node/commit/59d435ed4d)] - **lib**: update TODO comments (Ruben Bridgewater) [#33361](https://github.com/nodejs/node/pull/33361)
* [[`e62a8b5007`](https://github.com/nodejs/node/commit/e62a8b5007)] - **lib**: update executionAsyncId/triggerAsyncId comment (Daniel Bevenius) [#33396](https://github.com/nodejs/node/pull/33396)
* [[`4ae4073abf`](https://github.com/nodejs/node/commit/4ae4073abf)] - **lib,src**: remove cpu profiler idle notifier (Ben Noordhuis) [#34010](https://github.com/nodejs/node/pull/34010)
* [[`fc7cad828b`](https://github.com/nodejs/node/commit/fc7cad828b)] - **meta**: introduce codeowners again (James M Snell) [#33895](https://github.com/nodejs/node/pull/33895)
* [[`b162c532d7`](https://github.com/nodejs/node/commit/b162c532d7)] - **meta**: fix a typo in the flaky test template (Colin Ihrig) [#33677](https://github.com/nodejs/node/pull/33677)
* [[`148c1f1344`](https://github.com/nodejs/node/commit/148c1f1344)] - **meta**: wrap flaky test template at 80 characters (Colin Ihrig) [#33677](https://github.com/nodejs/node/pull/33677)
* [[`2aa6469bea`](https://github.com/nodejs/node/commit/2aa6469bea)] - **meta**: add flaky test issue template (Ash Cripps) [#33500](https://github.com/nodejs/node/pull/33500)
* [[`84a5e6cec8`](https://github.com/nodejs/node/commit/84a5e6cec8)] - **module**: fix error message about importing names from cjs (Fábio Santos) [#33882](https://github.com/nodejs/node/pull/33882)
* [[`8c9e3a9dfb`](https://github.com/nodejs/node/commit/8c9e3a9dfb)] - **module**: remove dynamicInstantiate loader hook (Jan Krems) [#33501](https://github.com/nodejs/node/pull/33501)
* [[`53dbb9d232`](https://github.com/nodejs/node/commit/53dbb9d232)] - **n-api**: add version to wasm registration (Gus Caplan) [#34045](https://github.com/nodejs/node/pull/34045)
* [[`e924439d96`](https://github.com/nodejs/node/commit/e924439d96)] - **n-api**: document nextTick timing in callbacks (Mathias Buus) [#33804](https://github.com/nodejs/node/pull/33804)
* [[`524daf89a1`](https://github.com/nodejs/node/commit/524daf89a1)] - **n-api**: ensure scope present for finalization (Michael Dawson) [#33508](https://github.com/nodejs/node/pull/33508)
* [[`e83642f73d`](https://github.com/nodejs/node/commit/e83642f73d)] - **n-api**: remove `napi\_env::CallIntoModuleThrow` (Gabriel Schulhof) [#33570](https://github.com/nodejs/node/pull/33570)
* [[`4c235b07ae`](https://github.com/nodejs/node/commit/4c235b07ae)] - ***Revert*** "**n-api**: detect deadlocks in thread-safe function" (Anna Henningsen) [#33453](https://github.com/nodejs/node/pull/33453)
* [[`022dcebcd8`](https://github.com/nodejs/node/commit/022dcebcd8)] - **napi**: add \_\_wasm32\_\_ guards (Gus Caplan) [#33597](https://github.com/nodejs/node/pull/33597)
* [[`164461edfd`](https://github.com/nodejs/node/commit/164461edfd)] - **net**: refactor check for Windows (rickyes) [#33497](https://github.com/nodejs/node/pull/33497)
* [[`e0b0ddd257`](https://github.com/nodejs/node/commit/e0b0ddd257)] - **querystring**: fix stringify for empty array (sapics) [#33918](https://github.com/nodejs/node/pull/33918)
* [[`e8572e7070`](https://github.com/nodejs/node/commit/e8572e7070)] - **querystring**: improve stringify() performance (Brian White) [#33669](https://github.com/nodejs/node/pull/33669)
* [[`011fe1d443`](https://github.com/nodejs/node/commit/011fe1d443)] - **repl**: add builtinModules (Ruben Bridgewater) [#33295](https://github.com/nodejs/node/pull/33295)
* [[`71d6599191`](https://github.com/nodejs/node/commit/71d6599191)] - **repl**: simplify repl autocompletion (Ruben Bridgewater) [#33450](https://github.com/nodejs/node/pull/33450)
* [[`1330cfc2a9`](https://github.com/nodejs/node/commit/1330cfc2a9)] - **repl**: support optional chaining during autocompletion (Ruben Bridgewater) [#33450](https://github.com/nodejs/node/pull/33450)
* [[`9760c6caff`](https://github.com/nodejs/node/commit/9760c6caff)] - **src**: add errorProperties on process.report (himself65) [#28426](https://github.com/nodejs/node/pull/28426)
* [[`da81930b13`](https://github.com/nodejs/node/commit/da81930b13)] - **src**: tolerate EPERM returned from tcsetattr (patr0nus) [#33944](https://github.com/nodejs/node/pull/33944)
* [[`c1664a9008`](https://github.com/nodejs/node/commit/c1664a9008)] - **src**: clang\_format base\_object (Yash Ladha) [#33680](https://github.com/nodejs/node/pull/33680)
* [[`a789474945`](https://github.com/nodejs/node/commit/a789474945)] - **src**: fix ParseEncoding (sapics) [#33957](https://github.com/nodejs/node/pull/33957)
* [[`74f4aae22f`](https://github.com/nodejs/node/commit/74f4aae22f)] - **src**: remove unnecessary calculation in base64.h (sapics) [#33839](https://github.com/nodejs/node/pull/33839)
* [[`c492a2715e`](https://github.com/nodejs/node/commit/c492a2715e)] - **src**: use ToLocal in node\_os.cc (wenningplus) [#33939](https://github.com/nodejs/node/pull/33939)
* [[`9a52cd9cc0`](https://github.com/nodejs/node/commit/9a52cd9cc0)] - **src**: handle empty Maybe(Local) in node\_util.cc (Anna Henningsen) [#33867](https://github.com/nodejs/node/pull/33867)
* [[`e1bebf13db`](https://github.com/nodejs/node/commit/e1bebf13db)] - **src**: fix FastStringKey equal operator (sapics) [#33748](https://github.com/nodejs/node/pull/33748)
* [[`0dd67d992e`](https://github.com/nodejs/node/commit/0dd67d992e)] - **src**: reduce scope of code cache mutex (Anna Henningsen) [#33980](https://github.com/nodejs/node/pull/33980)
* [[`cd0ae4007f`](https://github.com/nodejs/node/commit/cd0ae4007f)] - **src**: improve indention for upd\_wrap.cc (gengjiawen) [#33976](https://github.com/nodejs/node/pull/33976)
* [[`6014e4e0b8`](https://github.com/nodejs/node/commit/6014e4e0b8)] - **src**: remove unnecessary ToLocalChecked call (Daniel Bevenius) [#33902](https://github.com/nodejs/node/pull/33902)
* [[`4715a41c1c`](https://github.com/nodejs/node/commit/4715a41c1c)] - **src**: simplify alignment-handling code (Anna Henningsen) [#33884](https://github.com/nodejs/node/pull/33884)
* [[`33cff40bb7`](https://github.com/nodejs/node/commit/33cff40bb7)] - **src**: remove ref to tools/generate\_code\_cache.js (Daniel Bevenius) [#33825](https://github.com/nodejs/node/pull/33825)
* [[`dfa0ee13ee`](https://github.com/nodejs/node/commit/dfa0ee13ee)] - **src**: remove unused vector include in string\_bytes (Daniel Bevenius) [#33824](https://github.com/nodejs/node/pull/33824)
* [[`fb2b0a094b`](https://github.com/nodejs/node/commit/fb2b0a094b)] - **src**: avoid unnecessary ToLocalChecked calls (Daniel Bevenius) [#33824](https://github.com/nodejs/node/pull/33824)
* [[`07c21d0d27`](https://github.com/nodejs/node/commit/07c21d0d27)] - **src**: reduce FileHandle size by reordering fields (Anna Henningsen) [#33784](https://github.com/nodejs/node/pull/33784)
* [[`83aaad7ec3`](https://github.com/nodejs/node/commit/83aaad7ec3)] - **src**: do not track BaseObjects via cleanup hooks (Anna Henningsen) [#33809](https://github.com/nodejs/node/pull/33809)
* [[`f8dddd3416`](https://github.com/nodejs/node/commit/f8dddd3416)] - **src**: handle missing TracingController everywhere (Anna Henningsen) [#33815](https://github.com/nodejs/node/pull/33815)
* [[`3b71aa8029`](https://github.com/nodejs/node/commit/3b71aa8029)] - **src**: remove unused `ERR\_TRANSFERRING\_EXTERNALIZED\_SHAREDARRAYBUFFER` (Anna Henningsen) [#33810](https://github.com/nodejs/node/pull/33810)
* [[`1f996b7372`](https://github.com/nodejs/node/commit/1f996b7372)] - **src**: simplify Reindent function in json\_utils.cc (sapics) [#33722](https://github.com/nodejs/node/pull/33722)
* [[`cdcd76810e`](https://github.com/nodejs/node/commit/cdcd76810e)] - **src**: add "missing" bash completion options (Daniel Bevenius) [#33744](https://github.com/nodejs/node/pull/33744)
* [[`cc8d70531d`](https://github.com/nodejs/node/commit/cc8d70531d)] - **src**: use Check() instead of FromJust in environment (Daniel Bevenius) [#33706](https://github.com/nodejs/node/pull/33706)
* [[`858c6b9dfd`](https://github.com/nodejs/node/commit/858c6b9dfd)] - **src**: use ToLocal in SafeGetenv (Daniel Bevenius) [#33695](https://github.com/nodejs/node/pull/33695)
* [[`c2f49319b7`](https://github.com/nodejs/node/commit/c2f49319b7)] - **src**: remove unnecessary ToLocalChecked call (Daniel Bevenius) [#33683](https://github.com/nodejs/node/pull/33683)
* [[`21f1e64737`](https://github.com/nodejs/node/commit/21f1e64737)] - **src**: simplify format in node\_file.cc (himself65) [#33660](https://github.com/nodejs/node/pull/33660)
* [[`c3728c6235`](https://github.com/nodejs/node/commit/c3728c6235)] - **src**: simplify MaybeStackBuffer::capacity() (Ben Noordhuis) [#33602](https://github.com/nodejs/node/pull/33602)
* [[`7725ff392c`](https://github.com/nodejs/node/commit/7725ff392c)] - **src**: remove superfluous inline keywords (James M Snell) [#33291](https://github.com/nodejs/node/pull/33291)
* [[`27e9cb7e85`](https://github.com/nodejs/node/commit/27e9cb7e85)] - **src**: turn AllocatedBuffer into thin wrapper around v8::BackingStore (James M Snell) [#33291](https://github.com/nodejs/node/pull/33291)
* [[`d8f040e33d`](https://github.com/nodejs/node/commit/d8f040e33d)] - **src**: extract AllocatedBuffer from env.h (James M Snell) [#33291](https://github.com/nodejs/node/pull/33291)
* [[`a8824ae0a5`](https://github.com/nodejs/node/commit/a8824ae0a5)] - **src**: avoid OOB read in URL parser (Anna Henningsen) [#33640](https://github.com/nodejs/node/pull/33640)
* [[`6ef2efe33a`](https://github.com/nodejs/node/commit/6ef2efe33a)] - **src**: use MaybeLocal.ToLocal instead of IsEmpty worker (Daniel Bevenius) [#33599](https://github.com/nodejs/node/pull/33599)
* [[`522fbbc8d9`](https://github.com/nodejs/node/commit/522fbbc8d9)] - **src**: don't use semicolon outside function (Shelley Vohr) [#33592](https://github.com/nodejs/node/pull/33592)
* [[`ad970996cf`](https://github.com/nodejs/node/commit/ad970996cf)] - **src**: remove unused using declarations (Daniel Bevenius) [#33268](https://github.com/nodejs/node/pull/33268)
* [[`20d54f6908`](https://github.com/nodejs/node/commit/20d54f6908)] - **src**: use MaybeLocal.ToLocal instead of IsEmpty (Daniel Bevenius) [#33554](https://github.com/nodejs/node/pull/33554)
* [[`5438611984`](https://github.com/nodejs/node/commit/5438611984)] - **src**: use NewFromUtf8Literal in GetLinkedBinding (Daniel Bevenius) [#33552](https://github.com/nodejs/node/pull/33552)
* [[`a5e860cd29`](https://github.com/nodejs/node/commit/a5e860cd29)] - **src**: use const in constant args.Length() (himself65) [#33555](https://github.com/nodejs/node/pull/33555)
* [[`7e351f15cb`](https://github.com/nodejs/node/commit/7e351f15cb)] - **src**: use MaybeLocal::FromMaybe to return exception (Daniel Bevenius) [#33514](https://github.com/nodejs/node/pull/33514)
* [[`3f1c756f89`](https://github.com/nodejs/node/commit/3f1c756f89)] - ***Revert*** "**src**: fix missing extra ca in tls.rootCertificates" (Eric Bickle) [#33313](https://github.com/nodejs/node/pull/33313)
* [[`d1e1dbf188`](https://github.com/nodejs/node/commit/d1e1dbf188)] - **src**: remove BeforeExit callback list (Ben Noordhuis) [#33386](https://github.com/nodejs/node/pull/33386)
* [[`ee45b78b7f`](https://github.com/nodejs/node/commit/ee45b78b7f)] - **src**: use MaybeLocal.ToLocal instead of IsEmpty (Daniel Bevenius) [#33457](https://github.com/nodejs/node/pull/33457)
* [[`9018e92b13`](https://github.com/nodejs/node/commit/9018e92b13)] - **src**: remove unused headers in src/util.h (Juan José Arboleda) [#33070](https://github.com/nodejs/node/pull/33070)
* [[`7d1d00f97a`](https://github.com/nodejs/node/commit/7d1d00f97a)] - **src**: use enum for refed flag on native immediates (Anna Henningsen) [#33444](https://github.com/nodejs/node/pull/33444)
* [[`e8cc269ee0`](https://github.com/nodejs/node/commit/e8cc269ee0)] - **src**: use symbol to store `AsyncWrap` resource (Anna Henningsen) [#31745](https://github.com/nodejs/node/pull/31745)
* [[`ab2454dec5`](https://github.com/nodejs/node/commit/ab2454dec5)] - **src**: prefer make\_unique (Michael Dawson) [#33378](https://github.com/nodejs/node/pull/33378)
* [[`a942f7280a`](https://github.com/nodejs/node/commit/a942f7280a)] - **src**: remove unnecessary else in base\_object-inl.h (Daniel Bevenius) [#33413](https://github.com/nodejs/node/pull/33413)
* [[`f6227c0577`](https://github.com/nodejs/node/commit/f6227c0577)] - **src**: reduce duplication in RegisterHandleCleanups (Daniel Bevenius) [#33421](https://github.com/nodejs/node/pull/33421)
* [[`f24292e106`](https://github.com/nodejs/node/commit/f24292e106)] - **src**: remove unused IsolateSettings variable (Daniel Bevenius) [#33417](https://github.com/nodejs/node/pull/33417)
* [[`308be6ca0c`](https://github.com/nodejs/node/commit/308be6ca0c)] - **src**: remove unused misc variable (Daniel Bevenius) [#33417](https://github.com/nodejs/node/pull/33417)
* [[`7fd0519a91`](https://github.com/nodejs/node/commit/7fd0519a91)] - **src**: add promise\_resolve to SetupHooks comment (Daniel Bevenius) [#33365](https://github.com/nodejs/node/pull/33365)
* [[`26a3cf058d`](https://github.com/nodejs/node/commit/26a3cf058d)] - **src,build**: add --openssl-default-cipher-list (Daniel Bevenius) [#33708](https://github.com/nodejs/node/pull/33708)
* [[`b0fa611e68`](https://github.com/nodejs/node/commit/b0fa611e68)] - **stream**: fix the spellings (antsmartian) [#33635](https://github.com/nodejs/node/pull/33635)
* [[`1db0d51ab2`](https://github.com/nodejs/node/commit/1db0d51ab2)] - **stream**: forward writableObjectMode (Robert Nagy) [#33390](https://github.com/nodejs/node/pull/33390)
* [[`2c568c80f3`](https://github.com/nodejs/node/commit/2c568c80f3)] - **test**: add non-ASCII character embedding test (Anna Henningsen) [#33972](https://github.com/nodejs/node/pull/33972)
* [[`d4a2ae094e`](https://github.com/nodejs/node/commit/d4a2ae094e)] - **test**: add test for Http2ServerResponse#\[writableCorked,cork,uncork\] (Pranshu Srivastava) [#33956](https://github.com/nodejs/node/pull/33956)
* [[`4a61013fb2`](https://github.com/nodejs/node/commit/4a61013fb2)] - **test**: print arguments passed to mustNotCall function (Denys Otrishko) [#33951](https://github.com/nodejs/node/pull/33951)
* [[`1b55d90975`](https://github.com/nodejs/node/commit/1b55d90975)] - **test**: AsyncLocalStorage works with thenables (Gerhard Stoebich) [#34008](https://github.com/nodejs/node/pull/34008)
* [[`195980d667`](https://github.com/nodejs/node/commit/195980d667)] - **test**: account for non-node basename (Shelley Vohr) [#33952](https://github.com/nodejs/node/pull/33952)
* [[`90223f0a88`](https://github.com/nodejs/node/commit/90223f0a88)] - **test**: fix typo in common/index.js (gengjiawen) [#33976](https://github.com/nodejs/node/pull/33976)
* [[`d427d7f905`](https://github.com/nodejs/node/commit/d427d7f905)] - **test**: add common/udppair utility (James M Snell) [#33380](https://github.com/nodejs/node/pull/33380)
* [[`b8fdde400a`](https://github.com/nodejs/node/commit/b8fdde400a)] - ***Revert*** "**test**: stop testing --interpreted-frames-native-stack for s390x" (Michaël Zasso) [#33794](https://github.com/nodejs/node/pull/33794)
* [[`e3a53329c2`](https://github.com/nodejs/node/commit/e3a53329c2)] - **test**: temporarily exclude test on arm (Michael Dawson) [#33814](https://github.com/nodejs/node/pull/33814)
* [[`b6e3616911`](https://github.com/nodejs/node/commit/b6e3616911)] - **test**: fix invalid regular expressions in case test-trace-exit (legendecas) [#33769](https://github.com/nodejs/node/pull/33769)
* [[`c3ac47c03d`](https://github.com/nodejs/node/commit/c3ac47c03d)] - **test**: changed function to arrow function (Sagar Jadhav) [#33711](https://github.com/nodejs/node/pull/33711)
* [[`15eb5a3da4`](https://github.com/nodejs/node/commit/15eb5a3da4)] - **test**: uv\_tty\_init now returns EINVAL on IBM i (Xu Meng) [#33629](https://github.com/nodejs/node/pull/33629)
* [[`da5e970a8c`](https://github.com/nodejs/node/commit/da5e970a8c)] - **test**: make flaky test stricter (Robert Nagy) [#33539](https://github.com/nodejs/node/pull/33539)
* [[`47396a42cf`](https://github.com/nodejs/node/commit/47396a42cf)] - **test**: fix flaky test-trace-atomics-wait (Anna Henningsen) [#33428](https://github.com/nodejs/node/pull/33428)
* [[`eb877a4c49`](https://github.com/nodejs/node/commit/eb877a4c49)] - **test**: mark test-dgram-multicast-ssmv6-multi-process flaky (AshCripps) [#33498](https://github.com/nodejs/node/pull/33498)
* [[`5dca04ee8e`](https://github.com/nodejs/node/commit/5dca04ee8e)] - **tools**: remove superfluous regex in tools/doc/json.js (Rich Trott) [#33998](https://github.com/nodejs/node/pull/33998)
* [[`1791d5727c`](https://github.com/nodejs/node/commit/1791d5727c)] - **tools**: update remark-preset-lint-node@1.15.1 to 1.16.0 (Rich Trott) [#33852](https://github.com/nodejs/node/pull/33852)
* [[`01d8b91942`](https://github.com/nodejs/node/commit/01d8b91942)] - **tools**: prevent js2c from running if nothing changed (Daniel Bevenius) [#33844](https://github.com/nodejs/node/pull/33844)
* [[`e837f00b4f`](https://github.com/nodejs/node/commit/e837f00b4f)] - **tools**: remove unused vector include in mkdcodecache (Daniel Bevenius) [#33828](https://github.com/nodejs/node/pull/33828)
* [[`800dbb6bdd`](https://github.com/nodejs/node/commit/800dbb6bdd)] - **tools**: update ESLint to 7.2.0 (Colin Ihrig) [#33776](https://github.com/nodejs/node/pull/33776)
* [[`a14e38a6c0`](https://github.com/nodejs/node/commit/a14e38a6c0)] - **tools**: remove unused using declarations code\_cache (Daniel Bevenius) [#33697](https://github.com/nodejs/node/pull/33697)
* [[`9fb1eb09d9`](https://github.com/nodejs/node/commit/9fb1eb09d9)] - **tools**: update remark-preset-lint-node from 1.15.0 to 1.15.1 (Rich Trott) [#33727](https://github.com/nodejs/node/pull/33727)
* [[`a331a00eac`](https://github.com/nodejs/node/commit/a331a00eac)] - **tools**: fix check-imports.py to match on word boundaries (Richard Lau) [#33268](https://github.com/nodejs/node/pull/33268)
* [[`9325ed9e1c`](https://github.com/nodejs/node/commit/9325ed9e1c)] - **tools**: update ESLint to 7.1.0 (Colin Ihrig) [#33526](https://github.com/nodejs/node/pull/33526)
* [[`6dab63f36d`](https://github.com/nodejs/node/commit/6dab63f36d)] - **tools**: add docserve target (Antoine du HAMEL) [#33221](https://github.com/nodejs/node/pull/33221)
* [[`2384044c95`](https://github.com/nodejs/node/commit/2384044c95)] - **tools,gyp**: add support for MSVC cross-compilation (Richard Townsend) [#32867](https://github.com/nodejs/node/pull/32867)
* [[`987c927225`](https://github.com/nodejs/node/commit/987c927225)] - **util**: fix width detection for DEL without ICU (Ruben Bridgewater) [#33650](https://github.com/nodejs/node/pull/33650)
* [[`91d0d53b59`](https://github.com/nodejs/node/commit/91d0d53b59)] - **util**: support Combining Diacritical Marks for Symbols (Ruben Bridgewater) [#33650](https://github.com/nodejs/node/pull/33650)
* [[`e3d53f999d`](https://github.com/nodejs/node/commit/e3d53f999d)] - **util**: gracefully handle unknown colors (Ruben Bridgewater) [#33797](https://github.com/nodejs/node/pull/33797)
* [[`a90c9aa858`](https://github.com/nodejs/node/commit/a90c9aa858)] - **util**: fix inspection of class instance prototypes (Ruben Bridgewater) [#33449](https://github.com/nodejs/node/pull/33449)
* [[`2380d90f0a`](https://github.com/nodejs/node/commit/2380d90f0a)] - **util**: mark classes while inspecting them (Ruben Bridgewater) [#32332](https://github.com/nodejs/node/pull/32332)
* [[`879c9322ce`](https://github.com/nodejs/node/commit/879c9322ce)] - **vm**: allow proxy callbacks to throw (Gus Caplan) [#33808](https://github.com/nodejs/node/pull/33808)
* [[`af14c1f776`](https://github.com/nodejs/node/commit/af14c1f776)] - **wasi**: allow WASI stdio to be configured (Colin Ihrig) [#33544](https://github.com/nodejs/node/pull/33544)
* [[`5eecea375f`](https://github.com/nodejs/node/commit/5eecea375f)] - **wasi**: simplify WASI memory management (Colin Ihrig) [#33525](https://github.com/nodejs/node/pull/33525)
* [[`f98e888fdd`](https://github.com/nodejs/node/commit/f98e888fdd)] - **wasi**: refactor and enable poll\_oneoff() test (Colin Ihrig) [#33521](https://github.com/nodejs/node/pull/33521)
* [[`6b20e8442f`](https://github.com/nodejs/node/commit/6b20e8442f)] - **wasi**: relax WebAssembly.Instance type check (Ben Noordhuis) [#33431](https://github.com/nodejs/node/pull/33431)
* [[`d15383253a`](https://github.com/nodejs/node/commit/d15383253a)] - **wasi,worker**: handle termination exception (Ben Noordhuis) [#33386](https://github.com/nodejs/node/pull/33386)
* [[`3f971d89a9`](https://github.com/nodejs/node/commit/3f971d89a9)] - **win,fs**: use namespaced path in absolute symlinks (Bartosz Sosnowski) [#33351](https://github.com/nodejs/node/pull/33351)
* [[`3520a134af`](https://github.com/nodejs/node/commit/3520a134af)] - **win,msi**: add arm64 config for windows msi (Dennis Ameling) [#33689](https://github.com/nodejs/node/pull/33689)
* [[`b79495905f`](https://github.com/nodejs/node/commit/b79495905f)] - **worker**: fix variable referencing in template string (Harshitha KP) [#33467](https://github.com/nodejs/node/pull/33467)
* [[`9c3008005d`](https://github.com/nodejs/node/commit/9c3008005d)] - **worker**: perform initial port.unref() before preload modules (Anna Henningsen) [#33455](https://github.com/nodejs/node/pull/33455)
* [[`64cae13799`](https://github.com/nodejs/node/commit/64cae13799)] - **worker**: use \_writev in internal communication (Anna Henningsen) [#33454](https://github.com/nodejs/node/pull/33454)
* [[`7817b875a7`](https://github.com/nodejs/node/commit/7817b875a7)] - **worker**: fix race condition in node\_messaging.cc (Anna Henningsen) [#33429](https://github.com/nodejs/node/pull/33429)

<a id="14.4.0"></a>
## 2020-06-02, Version 14.4.0 (Current), @targos

### Notable changes

This is a security release.

Vulnerabilities fixed:
* **CVE-2020-8172**: TLS session reuse can lead to host certificate verification bypass (High).
* **CVE-2020-11080**: HTTP/2 Large Settings Frame DoS (Low).
* **CVE-2020-8174**: `napi_get_value_string_*()` allows various kinds of memory corruption (High).

### Commits

* [[`07a4d5061f`](https://github.com/nodejs/node/commit/07a4d5061f)] - **crypto**: update root certificates (AshCripps) [#33682](https://github.com/nodejs/node/pull/33682)
* [[`0a7bf50fd4`](https://github.com/nodejs/node/commit/0a7bf50fd4)] - **(SEMVER-MINOR)** **deps**: update nghttp2 to 1.41.0 (James M Snell) [nodejs-private/node-private#204](https://github.com/nodejs-private/node-private/pull/204)
* [[`55e4c72af8`](https://github.com/nodejs/node/commit/55e4c72af8)] - **(SEMVER-MINOR)** **http2**: implement support for max settings entries (James M Snell) [nodejs-private/node-private#204](https://github.com/nodejs-private/node-private/pull/204)
* [[`290720d16a`](https://github.com/nodejs/node/commit/290720d16a)] - **napi**: fix memory corruption vulnerability (Tobias Nießen) [nodejs-private/node-private#195](https://github.com/nodejs-private/node-private/pull/195)
* [[`94571c1001`](https://github.com/nodejs/node/commit/94571c1001)] - **tls**: emit `session` after verifying certificate (Fedor Indutny) [nodejs-private/node-private#200](https://github.com/nodejs-private/node-private/pull/200)
* [[`1658cf9ee6`](https://github.com/nodejs/node/commit/1658cf9ee6)] - **tools**: update certdata.txt (AshCripps) [#33682](https://github.com/nodejs/node/pull/33682)

<a id="14.3.0"></a>
## 2020-05-19, Version 14.3.0 (Current), @codebytere

### Notable Changes

#### REPL previews improvements with autocompletion

The output preview is changed to generate previews for autocompleted input
instead of the actual input.

![image](https://user-images.githubusercontent.com/8822573/82209841-4259e180-990e-11ea-93d7-0ea4b3bfc76f.png)

Pressing `<enter>` during a preview is now going to evaluate the whole string
including the autocompleted part. Pressing `<escape>` cancels that behavior.

#### Support for Top-Level Await

It's now possible to use the await keyword outside of async functions, with the
`--experimental-top-level-await` flag.

#### Other Notable Changes

* [[`7aa581f4ff`](https://github.com/nodejs/node/commit/7aa581f4ff)] - **(SEMVER-MINOR)** **repl**: deprecate repl.\_builtinLibs (Ruben Bridgewater) [#33294](https://github.com/nodejs/node/pull/33294)
* [[`db7bb941a3`](https://github.com/nodejs/node/commit/db7bb941a3)] - **(SEMVER-MINOR)** **repl**: deprecate repl.inputStream and repl.outputStream (Ruben Bridgewater) [#33294](https://github.com/nodejs/node/pull/33294)
* [[`2dc5db8c07`](https://github.com/nodejs/node/commit/2dc5db8c07)] - **(SEMVER-MINOR)** **cli**: add `--trace-atomics-wait` flag (Anna Henningsen) [#33292](https://github.com/nodejs/node/pull/33292)
* [[`6257cf256e`](https://github.com/nodejs/node/commit/6257cf256e)] - **(SEMVER-MINOR)** **repl**: improve repl autocompletion for require calls (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`d33dcf1d5f`](https://github.com/nodejs/node/commit/d33dcf1d5f)] - **(SEMVER-MINOR)** **repl**: show reference errors during preview (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`1dcf66cf87`](https://github.com/nodejs/node/commit/1dcf66cf87)] - **(SEMVER-MINOR)** **fs**: add .ref() and .unref() methods to watcher classes (rickyes) [#33134](https://github.com/nodejs/node/pull/33134)
* [[`f33e86649e`](https://github.com/nodejs/node/commit/f33e86649e)] - **(SEMVER-MINOR)** **http**: expose http.validate-header-name/value (osher) [#33119](https://github.com/nodejs/node/pull/33119)
* [[`b06165584e`](https://github.com/nodejs/node/commit/b06165584e)] - **(SEMVER-MINOR)** **async_hooks**: move PromiseHook handler to JS (Stephen Belanger) [#32891](https://github.com/nodejs/node/pull/32891)

### Commits

* [[`dd4789b8ee`](https://github.com/nodejs/node/commit/dd4789b8ee)] - **async_hooks**: clear async\_id\_stack for terminations in more places (Anna Henningsen) [#33347](https://github.com/nodejs/node/pull/33347)
* [[`b06165584e`](https://github.com/nodejs/node/commit/b06165584e)] - **(SEMVER-MINOR)** **async_hooks**: move PromiseHook handler to JS (Stephen Belanger) [#32891](https://github.com/nodejs/node/pull/32891)
* [[`cae2051b83`](https://github.com/nodejs/node/commit/cae2051b83)] - **buffer**: improve copy() performance (Nikolai Vavilov) [#33214](https://github.com/nodejs/node/pull/33214)
* [[`24faa37a09`](https://github.com/nodejs/node/commit/24faa37a09)] - **buffer,n-api**: release external buffers from BackingStore callback (Anna Henningsen) [#33321](https://github.com/nodejs/node/pull/33321)
* [[`34e7400fc1`](https://github.com/nodejs/node/commit/34e7400fc1)] - **build**: enable `--error-on-warn` for POSIX workflows (Richard Lau) [#33357](https://github.com/nodejs/node/pull/33357)
* [[`7d4db35f84`](https://github.com/nodejs/node/commit/7d4db35f84)] - **build**: fix `--error-on-warn` for macOS (Richard Lau) [#33357](https://github.com/nodejs/node/pull/33357)
* [[`2dc5db8c07`](https://github.com/nodejs/node/commit/2dc5db8c07)] - **(SEMVER-MINOR)** **cli**: add `--trace-atomics-wait` flag (Anna Henningsen) [#33292](https://github.com/nodejs/node/pull/33292)
* [[`331f0b3420`](https://github.com/nodejs/node/commit/331f0b3420)] - **deps**: update to ICU 67.1 (Michaël Zasso) [#33324](https://github.com/nodejs/node/pull/33324)
* [[`ba66b21c37`](https://github.com/nodejs/node/commit/ba66b21c37)] - **deps**: upgrade npm to 6.14.5 (Ruy Adorno) [#33239](https://github.com/nodejs/node/pull/33239)
* [[`cc279490ce`](https://github.com/nodejs/node/commit/cc279490ce)] - **doc**: prepare 14.x changelog for remark update (Rich Trott) [#33412](https://github.com/nodejs/node/pull/33412)
* [[`7f9ccd6d89`](https://github.com/nodejs/node/commit/7f9ccd6d89)] - **doc**: fix extension in esm example (Gus Caplan) [#33408](https://github.com/nodejs/node/pull/33408)
* [[`8f91338f6e`](https://github.com/nodejs/node/commit/8f91338f6e)] - **doc**: fix stream example (Anna Henningsen) [#33426](https://github.com/nodejs/node/pull/33426)
* [[`182aaf5622`](https://github.com/nodejs/node/commit/182aaf5622)] - **doc**: enhance guides by fixing and making grammar more consistent (Chris Holland) [#33152](https://github.com/nodejs/node/pull/33152)
* [[`0ffa0402a5`](https://github.com/nodejs/node/commit/0ffa0402a5)] - **doc**: add examples for implementing ESM (unknown) [#33168](https://github.com/nodejs/node/pull/33168)
* [[`b41affb9e2`](https://github.com/nodejs/node/commit/b41affb9e2)] - **doc**: add note about clientError writable handling (Paolo Insogna) [#33308](https://github.com/nodejs/node/pull/33308)
* [[`4f0cd648bb`](https://github.com/nodejs/node/commit/4f0cd648bb)] - **doc**: fix typo in n-api.md (Daniel Bevenius) [#33319](https://github.com/nodejs/node/pull/33319)
* [[`0cbee57109`](https://github.com/nodejs/node/commit/0cbee57109)] - **doc**: add warning for socket.connect reuse (Robert Nagy) [#33204](https://github.com/nodejs/node/pull/33204)
* [[`a9e4fdbd1b`](https://github.com/nodejs/node/commit/a9e4fdbd1b)] - **doc**: correct description of `decipher.setAuthTag` in crypto.md (Jonathan Buhacoff)
* [[`84974d3f2c`](https://github.com/nodejs/node/commit/84974d3f2c)] - **doc**: mention python3-distutils dependency in BUILDING.md (osher) [#33174](https://github.com/nodejs/node/pull/33174)
* [[`b5dcfbf634`](https://github.com/nodejs/node/commit/b5dcfbf634)] - **doc**: removed unnecessary util imports from vm examples (Karol Walasek) [#33179](https://github.com/nodejs/node/pull/33179)
* [[`e20fe535a5`](https://github.com/nodejs/node/commit/e20fe535a5)] - **doc**: update Buffer(size) documentation (Nikolai Vavilov) [#33198](https://github.com/nodejs/node/pull/33198)
* [[`5b42d812cc`](https://github.com/nodejs/node/commit/5b42d812cc)] - **doc**: add Uint8Array to `end` and `write` (Pranshu Srivastava) [#33217](https://github.com/nodejs/node/pull/33217)
* [[`c6a8cd0fa1`](https://github.com/nodejs/node/commit/c6a8cd0fa1)] - **doc**: fix md issue in src/README.md (Juan José Arboleda) [#33224](https://github.com/nodejs/node/pull/33224)
* [[`2c49dd3d01`](https://github.com/nodejs/node/commit/2c49dd3d01)] - **doc**: specify unit of time passed to `fs.utimes` (Simen Bekkhus) [#33230](https://github.com/nodejs/node/pull/33230)
* [[`6ffec50494`](https://github.com/nodejs/node/commit/6ffec50494)] - **doc**: add troubleshooting guide for AsyncLocalStorage (Andrey Pechkurov) [#33248](https://github.com/nodejs/node/pull/33248)
* [[`dab5c38f98`](https://github.com/nodejs/node/commit/dab5c38f98)] - **doc**: remove AsyncWrap mentions from async\_hooks.md (Andrey Pechkurov) [#33249](https://github.com/nodejs/node/pull/33249)
* [[`05729430bf`](https://github.com/nodejs/node/commit/05729430bf)] - **doc**: add warnings about transferring Buffers and ArrayBuffer (James M Snell) [#33252](https://github.com/nodejs/node/pull/33252)
* [[`cf88ed8664`](https://github.com/nodejs/node/commit/cf88ed8664)] - **doc**: update napi\_async\_init documentation (Michael Dawson) [#33181](https://github.com/nodejs/node/pull/33181)
* [[`25443fa7f2`](https://github.com/nodejs/node/commit/25443fa7f2)] - **doc**: doc and test URLSearchParams discrepancy (James M Snell) [#33236](https://github.com/nodejs/node/pull/33236)
* [[`07372e9d5b`](https://github.com/nodejs/node/commit/07372e9d5b)] - **doc**: explicitly doc package.exports is breaking (Myles Borins) [#33074](https://github.com/nodejs/node/pull/33074)
* [[`c5a38fe6d7`](https://github.com/nodejs/node/commit/c5a38fe6d7)] - **doc**: fix style and grammer in buffer.md (Nikolai Vavilov) [#33194](https://github.com/nodejs/node/pull/33194)
* [[`e53de96a89`](https://github.com/nodejs/node/commit/e53de96a89)] - **esm**: improve commonjs hint on module not found (Antoine du Hamel) [#33220](https://github.com/nodejs/node/pull/33220)
* [[`c7c420ec87`](https://github.com/nodejs/node/commit/c7c420ec87)] - **fs**: forbid concurrent operations on Dir handle (Anna Henningsen) [#33274](https://github.com/nodejs/node/pull/33274)
* [[`12391c7a20`](https://github.com/nodejs/node/commit/12391c7a20)] - **fs**: clean up Dir.read() uv\_fs\_t data before calling into JS (Anna Henningsen) [#33274](https://github.com/nodejs/node/pull/33274)
* [[`1dcf66cf87`](https://github.com/nodejs/node/commit/1dcf66cf87)] - **(SEMVER-MINOR)** **fs**: add .ref() and .unref() methods to watcher classes (rickyes) [#33134](https://github.com/nodejs/node/pull/33134)
* [[`f33e86649e`](https://github.com/nodejs/node/commit/f33e86649e)] - **(SEMVER-MINOR)** **http**: expose http.validate-header-name/value (osher) [#33119](https://github.com/nodejs/node/pull/33119)
* [[`cc5c8e039d`](https://github.com/nodejs/node/commit/cc5c8e039d)] - **http**: don't destroy completed request (Robert Nagy) [#33120](https://github.com/nodejs/node/pull/33120)
* [[`b634d4b000`](https://github.com/nodejs/node/commit/b634d4b000)] - **http**: set IncomingMessage.destroyed (Robert Nagy) [#33131](https://github.com/nodejs/node/pull/33131)
* [[`cc02c73e53`](https://github.com/nodejs/node/commit/cc02c73e53)] - **http**: fixes memory retention issue with FreeList and HTTPParser (John Leidegren) [#33190](https://github.com/nodejs/node/pull/33190)
* [[`41c5524432`](https://github.com/nodejs/node/commit/41c5524432)] - **http2**: add `bytesWritten` test for `Http2Stream` (Pranshu Srivastava) [#33162](https://github.com/nodejs/node/pull/33162)
* [[`a133a88234`](https://github.com/nodejs/node/commit/a133a88234)] - **lib**: fix typo in timers insert function comment (Daniel Bevenius) [#33301](https://github.com/nodejs/node/pull/33301)
* [[`94d0a088ec`](https://github.com/nodejs/node/commit/94d0a088ec)] - **lib**: refactored scheduling policy assignment (Yash Ladha) [#32663](https://github.com/nodejs/node/pull/32663)
* [[`6bca487b8b`](https://github.com/nodejs/node/commit/6bca487b8b)] - **lib**: fix grammar in internal/bootstrap/loaders.js (szTheory) [#33211](https://github.com/nodejs/node/pull/33211)
* [[`0a78925146`](https://github.com/nodejs/node/commit/0a78925146)] - **meta**: add issue template for API reference docs (Derek Lewis) [#32944](https://github.com/nodejs/node/pull/32944)
* [[`35aae31968`](https://github.com/nodejs/node/commit/35aae31968)] - **module**: add specific error for dir import (Antoine du HAMEL) [#33220](https://github.com/nodejs/node/pull/33220)
* [[`c2d2dfc09f`](https://github.com/nodejs/node/commit/c2d2dfc09f)] - **module**: do not check circular dependencies for exported proxies (Ruben Bridgewater) [#33338](https://github.com/nodejs/node/pull/33338)
* [[`ad8680773e`](https://github.com/nodejs/node/commit/ad8680773e)] - **module**: better error for named exports from cjs (Myles Borins) [#33256](https://github.com/nodejs/node/pull/33256)
* [[`27b814c79b`](https://github.com/nodejs/node/commit/27b814c79b)] - **module**: lazy load 'getOptionValue' in initializeLoader (himself65) [#33212](https://github.com/nodejs/node/pull/33212)
* [[`4ae6130010`](https://github.com/nodejs/node/commit/4ae6130010)] - **n-api**: add uint32 test for -1 (Gabriel Schulhof)
* [[`398bdf40e5`](https://github.com/nodejs/node/commit/398bdf40e5)] - **perf_hooks**: fix error message for invalid entryTypes (Michaël Zasso) [#33285](https://github.com/nodejs/node/pull/33285)
* [[`7aa581f4ff`](https://github.com/nodejs/node/commit/7aa581f4ff)] - **(SEMVER-MINOR)** **repl**: deprecate repl.\_builtinLibs (Ruben Bridgewater) [#33294](https://github.com/nodejs/node/pull/33294)
* [[`ed83202307`](https://github.com/nodejs/node/commit/ed83202307)] - **repl**: remove obsolete completer variable (Ruben Bridgewater) [#33294](https://github.com/nodejs/node/pull/33294)
* [[`db7bb941a3`](https://github.com/nodejs/node/commit/db7bb941a3)] - **(SEMVER-MINOR)** **repl**: deprecate repl.inputStream and repl.outputStream (Ruben Bridgewater) [#33294](https://github.com/nodejs/node/pull/33294)
* [[`6257cf256e`](https://github.com/nodejs/node/commit/6257cf256e)] - **repl**: improve repl autocompletion for require calls (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`69061dc73e`](https://github.com/nodejs/node/commit/69061dc73e)] - **repl**: replace hard coded core module list with actual list (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`d33dcf1d5f`](https://github.com/nodejs/node/commit/d33dcf1d5f)] - **(SEMVER-MINOR)** **repl**: show reference errors during preview (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`1a9771a50a`](https://github.com/nodejs/node/commit/1a9771a50a)] - **(SEMVER-MINOR)** **repl**: improve repl preview (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`e4ad4642d7`](https://github.com/nodejs/node/commit/e4ad4642d7)] - **src**: add default: case to silence compiler warning (Anna Henningsen) [#33451](https://github.com/nodejs/node/pull/33451)
* [[`099f18e89b`](https://github.com/nodejs/node/commit/099f18e89b)] - **src**: distinguish refed/unrefed threadsafe Immediates (Anna Henningsen) [#33320](https://github.com/nodejs/node/pull/33320)
* [[`5e5aa0bc6c`](https://github.com/nodejs/node/commit/5e5aa0bc6c)] - **src**: add #include \<string\> in json\_utils.h (Cheng Zhao) [#33332](https://github.com/nodejs/node/pull/33332)
* [[`8ada953ef2`](https://github.com/nodejs/node/commit/8ada953ef2)] - **src**: replace to CHECK\_NOT\_NULL in node\_crypto (himself65) [#33383](https://github.com/nodejs/node/pull/33383)
* [[`0257386cd4`](https://github.com/nodejs/node/commit/0257386cd4)] - **src**: remove deprecated FinalizationRegistry hooks (Gus Caplan) [#33373](https://github.com/nodejs/node/pull/33373)
* [[`354ff4f21b`](https://github.com/nodejs/node/commit/354ff4f21b)] - **src**: small modification to NgHeader (James M Snell) [#33289](https://github.com/nodejs/node/pull/33289)
* [[`fd89ef1478`](https://github.com/nodejs/node/commit/fd89ef1478)] - **src**: refactor Reallocate since it introduced in upstream v8 (Jiawen Geng) [#33402](https://github.com/nodejs/node/pull/33402)
* [[`d292633ed4`](https://github.com/nodejs/node/commit/d292633ed4)] - **src**: add primordials to arguments comment (Daniel Bevenius) [#33318](https://github.com/nodejs/node/pull/33318)
* [[`19996073ca`](https://github.com/nodejs/node/commit/19996073ca)] - **src**: remove unused using declarations in node.cc (Daniel Bevenius) [#33261](https://github.com/nodejs/node/pull/33261)
* [[`c9c16c03c4`](https://github.com/nodejs/node/commit/c9c16c03c4)] - **src**: delete unused variables to resolve compile time print warning (rickyes) [#33358](https://github.com/nodejs/node/pull/33358)
* [[`066ca98069`](https://github.com/nodejs/node/commit/066ca98069)] - **src**: use MaybeLocal.ToLocal instead of IsEmpty (Daniel Bevenius) [#33312](https://github.com/nodejs/node/pull/33312)
* [[`f3129b290d`](https://github.com/nodejs/node/commit/f3129b290d)] - **src**: fix typo in comment in async\_wrap.cc (Daniel Bevenius) [#33350](https://github.com/nodejs/node/pull/33350)
* [[`0d77eec4b0`](https://github.com/nodejs/node/commit/0d77eec4b0)] - **src**: add support for TLA (Gus Caplan) [#30370](https://github.com/nodejs/node/pull/30370)
* [[`fd9c7c2118`](https://github.com/nodejs/node/commit/fd9c7c2118)] - **src**: fix compiler warning in async\_wrap.cc (Anna Henningsen) [#33322](https://github.com/nodejs/node/pull/33322)
* [[`3de9dd9c8d`](https://github.com/nodejs/node/commit/3de9dd9c8d)] - **src**: remove unnecessary Isolate::GetCurrent() calls (Anna Henningsen) [#33298](https://github.com/nodejs/node/pull/33298)
* [[`ef2503375b`](https://github.com/nodejs/node/commit/ef2503375b)] - **src**: fix invalid windowBits=8 gzip segfault (Ben Noordhuis) [#33045](https://github.com/nodejs/node/pull/33045)
* [[`548cedd870`](https://github.com/nodejs/node/commit/548cedd870)] - **src**: split out callback queue implementation from Environment (Anna Henningsen) [#33272](https://github.com/nodejs/node/pull/33272)
* [[`ed41494397`](https://github.com/nodejs/node/commit/ed41494397)] - **src**: clean up large pages code (Gabriel Schulhof) [#33255](https://github.com/nodejs/node/pull/33255)
* [[`cf476984f6`](https://github.com/nodejs/node/commit/cf476984f6)] - **src**: use BaseObjectPtr in StreamReq::Dispose (James M Snell) [#33102](https://github.com/nodejs/node/pull/33102)
* [[`5ff31921cc`](https://github.com/nodejs/node/commit/5ff31921cc)] - ***Revert*** "**src**: add test/abort build tasks" (Richard Lau) [#33196](https://github.com/nodejs/node/pull/33196)
* [[`a56b600e93`](https://github.com/nodejs/node/commit/a56b600e93)] - ***Revert*** "**src**: add aliased-buffer-overflow abort test" (Richard Lau) [#33196](https://github.com/nodejs/node/pull/33196)
* [[`a292630baf`](https://github.com/nodejs/node/commit/a292630baf)] - **src**: retrieve binding data from the context (Joyee Cheung) [#33139](https://github.com/nodejs/node/pull/33139)
* [[`b2fb01a68d`](https://github.com/nodejs/node/commit/b2fb01a68d)] - **stream**: make from read one at a time (Robert Nagy) [#33201](https://github.com/nodejs/node/pull/33201)
* [[`b93a723fe6`](https://github.com/nodejs/node/commit/b93a723fe6)] - **test**: regression tests for async\_hooks + Promise + Worker interaction (Anna Henningsen) [#33347](https://github.com/nodejs/node/pull/33347)
* [[`d3e2fc81e8`](https://github.com/nodejs/node/commit/d3e2fc81e8)] - **test**: fix test-dns-idna2008 (Rich Trott) [#33367](https://github.com/nodejs/node/pull/33367)
* [[`95842db17e`](https://github.com/nodejs/node/commit/95842db17e)] - **test**: refactor test/parallel/test-bootstrap-modules.js (Ruben Bridgewater) [#33282](https://github.com/nodejs/node/pull/33282)
* [[`f31b262f50`](https://github.com/nodejs/node/commit/f31b262f50)] - **test**: refactor WPTRunner (Joyee Cheung) [#33297](https://github.com/nodejs/node/pull/33297)
* [[`85cffb8e4c`](https://github.com/nodejs/node/commit/85cffb8e4c)] - **test**: update WPT interfaces and hr-time (Joyee Cheung) [#33297](https://github.com/nodejs/node/pull/33297)
* [[`5b2cd440a1`](https://github.com/nodejs/node/commit/5b2cd440a1)] - **test**: fix test-net-throttle (Rich Trott) [#33329](https://github.com/nodejs/node/pull/33329)
* [[`1d2c81fee9`](https://github.com/nodejs/node/commit/1d2c81fee9)] - **test**: add hr-time Web platform tests (Michaël Zasso) [#33287](https://github.com/nodejs/node/pull/33287)
* [[`6f54c2bbb6`](https://github.com/nodejs/node/commit/6f54c2bbb6)] - **test**: rename test-lookupService-promises (rickyes) [#33100](https://github.com/nodejs/node/pull/33100)
* [[`302408e515`](https://github.com/nodejs/node/commit/302408e515)] - **test**: skip some console tests on dumb terminal (Adam Majer) [#33165](https://github.com/nodejs/node/pull/33165)
* [[`676ef952ab`](https://github.com/nodejs/node/commit/676ef952ab)] - **test**: add tests for options.fs in fs streams (Julian Duque) [#33185](https://github.com/nodejs/node/pull/33185)
* [[`6d2aaaf6b4`](https://github.com/nodejs/node/commit/6d2aaaf6b4)] - **tls**: fix --tls-keylog option (Alba Mendez) [#33366](https://github.com/nodejs/node/pull/33366)
* [[`eedc13174e`](https://github.com/nodejs/node/commit/eedc13174e)] - **tls**: reset secureConnecting on client socket (David Halls) [#33209](https://github.com/nodejs/node/pull/33209)
* [[`453affebb0`](https://github.com/nodejs/node/commit/453affebb0)] - **tools**: update dependencies for markdown linting (Rich Trott) [#33412](https://github.com/nodejs/node/pull/33412)
* [[`91193447fb`](https://github.com/nodejs/node/commit/91193447fb)] - **tools**: enable no-else-return lint rule (Luigi Pinca) [#32667](https://github.com/nodejs/node/pull/32667)
* [[`e1e57a4223`](https://github.com/nodejs/node/commit/e1e57a4223)] - **tools**: update ESLint to 7.0.0 (Colin Ihrig) [#33316](https://github.com/nodejs/node/pull/33316)
* [[`cf03fe5b67`](https://github.com/nodejs/node/commit/cf03fe5b67)] - **tools**: remove obsolete no-restricted-syntax eslint rules (Ruben Bridgewater) [#32161](https://github.com/nodejs/node/pull/32161)
* [[`804982c1b6`](https://github.com/nodejs/node/commit/804982c1b6)] - **tools**: add eslint rule to only pass through 'test' to debuglog (Ruben Bridgewater) [#32161](https://github.com/nodejs/node/pull/32161)
* [[`c2cf9782ab`](https://github.com/nodejs/node/commit/c2cf9782ab)] - ***Revert*** "**vm**: add importModuleDynamically option to compileFunction" (Matteo Collina) [#33364](https://github.com/nodejs/node/pull/33364)
* [[`6a26eee3c5`](https://github.com/nodejs/node/commit/6a26eee3c5)] - **wasi**: fix poll\_oneoff memory interface (Colin Ihrig) [#33250](https://github.com/nodejs/node/pull/33250)
* [[`4465d23c30`](https://github.com/nodejs/node/commit/4465d23c30)] - **wasi**: prevent syscalls before start (Tobias Nießen) [#33235](https://github.com/nodejs/node/pull/33235)
* [[`9d1e577109`](https://github.com/nodejs/node/commit/9d1e577109)] - **worker**: fix crash when .unref() is called during exit (Anna Henningsen) [#33394](https://github.com/nodejs/node/pull/33394)
* [[`b1a7fdac43`](https://github.com/nodejs/node/commit/b1a7fdac43)] - **worker**: call CancelTerminateExecution() before exiting Locker (Anna Henningsen) [#33347](https://github.com/nodejs/node/pull/33347)
* [[`736ca65c2c`](https://github.com/nodejs/node/commit/736ca65c2c)] - **zlib**: reject windowBits=8 when mode=GZIP (Ben Noordhuis) [#33045](https://github.com/nodejs/node/pull/33045)

<a id="14.2.0"></a>
## 2020-05-05, Version 14.2.0 (Current), @targos

### Notable Changes

#### Track function calls with `assert.CallTracker` (experimental)

`assert.CallTracker` is a new experimental API that allows to track and later
verify the number of times a function was called. This works by creating a
`CallTracker` object and using its `calls` method to create wrapper functions
that will count each time they are called. Then the `verify` method can be used
to assert that the expected number of calls happened:

```js
const assert = require('assert');

const tracker = new assert.CallTracker();

function func() {}
// callsfunc() must be called exactly twice before tracker.verify().
const callsfunc = tracker.calls(func, 2);
callsfunc();
callsfunc();

function otherFunc() {}
// The second parameter defaults to `1`.
const callsotherFunc = tracker.calls(otherFunc);
callsotherFunc();

// Calls tracker.verify() and verifies if all tracker.calls() functions have
// been called the right number of times.
process.on('exit', () => {
  tracker.verify();
});
```

Additionally, `tracker.report()` will return an array which contains information
about the errors, if there are any:

<!-- eslint-disable max-len -->
```js
const assert = require('assert');

const tracker = new assert.CallTracker();

function func() {}
const callsfunc = tracker.calls(func);

console.log(tracker.report());
/*
[
  {
    message: 'Expected the func function to be executed 1 time(s) but was executed 0 time(s).',
    actual: 0,
    expected: 1,
    operator: 'func',
    stack: Error
        ...
  }
]
*/
```

Contributed by ConorDavenport - [#31982](https://github.com/nodejs/node/pull/31982).

#### Console `groupIndentation` option

The Console constructor (`require('console').Console`) now supports different group indentations.

This is useful in case you want different grouping width than 2 spaces.

```js
const { Console } = require('console');
const customConsole = new Console({
  stdout: process.stdout,
  stderr: process.stderr,
  groupIndentation: 10
});

customConsole.log('foo');
// 'foo'
customConsole.group();
customConsole.log('foo');
//           'foo'
```

Contributed by rickyes - [#32964](https://github.com/nodejs/node/pull/32964).

### Commits

#### Semver-minor commits

* [[`c87ed21fdf`](https://github.com/nodejs/node/commit/c87ed21fdf)] - **(SEMVER-MINOR)** **assert**: port common.mustCall() to assert (ConorDavenport) [#31982](https://github.com/nodejs/node/pull/31982)
* [[`c49e3ea20c`](https://github.com/nodejs/node/commit/c49e3ea20c)] - **(SEMVER-MINOR)** **console**: support console constructor groupIndentation option (rickyes) [#32964](https://github.com/nodejs/node/pull/32964)
* [[`bc9e413dae`](https://github.com/nodejs/node/commit/bc9e413dae)] - **(SEMVER-MINOR)** **worker**: add stack size resource limit option (Anna Henningsen) [#33085](https://github.com/nodejs/node/pull/33085)

#### Semver-patch commits

* [[`f62d92b900`](https://github.com/nodejs/node/commit/f62d92b900)] - **build**: add --error-on-warn configure flag (Daniel Bevenius) [#32685](https://github.com/nodejs/node/pull/32685)
* [[`db293c47dd`](https://github.com/nodejs/node/commit/db293c47dd)] - **cluster**: fix error on worker disconnect/destroy (Santiago Gimeno) [#32793](https://github.com/nodejs/node/pull/32793)
* [[`83e165bf88`](https://github.com/nodejs/node/commit/83e165bf88)] - **crypto**: check DiffieHellman p and g params (Ben Noordhuis) [#32739](https://github.com/nodejs/node/pull/32739)
* [[`e07cca6af6`](https://github.com/nodejs/node/commit/e07cca6af6)] - **crypto**: generator must be int32 in DiffieHellman() (Ben Noordhuis) [#32739](https://github.com/nodejs/node/pull/32739)
* [[`637442fec9`](https://github.com/nodejs/node/commit/637442fec9)] - **crypto**: key size must be int32 in DiffieHellman() (Ben Noordhuis) [#32739](https://github.com/nodejs/node/pull/32739)
* [[`c5a4534d5c`](https://github.com/nodejs/node/commit/c5a4534d5c)] - **deps**: V8: backport e29c62b74854 (Anna Henningsen) [#33125](https://github.com/nodejs/node/pull/33125)
* [[`8325c29e92`](https://github.com/nodejs/node/commit/8325c29e92)] - **deps**: update to uvwasi 0.0.8 (Colin Ihrig) [#33078](https://github.com/nodejs/node/pull/33078)
* [[`2174159598`](https://github.com/nodejs/node/commit/2174159598)] - **esm**: improve commonjs hint on module not found (Daniele Belardi) [#31906](https://github.com/nodejs/node/pull/31906)
* [[`74b0e8c3a8`](https://github.com/nodejs/node/commit/74b0e8c3a8)] - **http**: ensure client request emits close (Robert Nagy) [#33178](https://github.com/nodejs/node/pull/33178)
* [[`a4ec01c55b`](https://github.com/nodejs/node/commit/a4ec01c55b)] - **http**: simplify sending header (Robert Nagy) [#33200](https://github.com/nodejs/node/pull/33200)
* [[`451993ea94`](https://github.com/nodejs/node/commit/451993ea94)] - **http**: set default timeout in agent keepSocketAlive (Owen Smith) [#33127](https://github.com/nodejs/node/pull/33127)
* [[`3cb1713a59`](https://github.com/nodejs/node/commit/3cb1713a59)] - **http2,doc**: minor fixes (Alba Mendez) [#28044](https://github.com/nodejs/node/pull/28044)
* [[`eab4be1b93`](https://github.com/nodejs/node/commit/eab4be1b93)] - **lib**: cosmetic change to builtinLibs list for maintainability (James M Snell) [#33106](https://github.com/nodejs/node/pull/33106)
* [[`542da430ff`](https://github.com/nodejs/node/commit/542da430ff)] - **lib**: fix validateport error message when allowZero is false (rickyes) [#32861](https://github.com/nodejs/node/pull/32861)
* [[`5eccf1e9ad`](https://github.com/nodejs/node/commit/5eccf1e9ad)] - **module**: no type module resolver side effects (Guy Bedford) [#33086](https://github.com/nodejs/node/pull/33086)
* [[`466213d726`](https://github.com/nodejs/node/commit/466213d726)] - **n-api**: simplify uv\_idle wrangling (Ben Noordhuis) [#32997](https://github.com/nodejs/node/pull/32997)
* [[`ed45b51642`](https://github.com/nodejs/node/commit/ed45b51642)] - **path**: fix comment grammar (thecodrr) [#32942](https://github.com/nodejs/node/pull/32942)
* [[`bb2d2f6e0e`](https://github.com/nodejs/node/commit/bb2d2f6e0e)] - **src**: remove unused v8 Message namespace (Adrian Estrada) [#33180](https://github.com/nodejs/node/pull/33180)
* [[`de643bc325`](https://github.com/nodejs/node/commit/de643bc325)] - **src**: use unique\_ptr for CachedData in ContextifyScript::New (Anna Henningsen) [#33113](https://github.com/nodejs/node/pull/33113)
* [[`f61928ba35`](https://github.com/nodejs/node/commit/f61928ba35)] - **src**: return undefined when validation err == 0 (James M Snell) [#33107](https://github.com/nodejs/node/pull/33107)
* [[`f4e5ab14da`](https://github.com/nodejs/node/commit/f4e5ab14da)] - **src**: crypto::UseSNIContext to use BaseObjectPtr (James M Snell) [#33107](https://github.com/nodejs/node/pull/33107)
* [[`541ea035bf`](https://github.com/nodejs/node/commit/541ea035bf)] - **src**: separate out NgLibMemoryManagerBase (James M Snell) [#33104](https://github.com/nodejs/node/pull/33104)
* [[`10a87c81cf`](https://github.com/nodejs/node/commit/10a87c81cf)] - **src**: remove unnecessary fully qualified names (rickyes) [#33077](https://github.com/nodejs/node/pull/33077)
* [[`45032a39e8`](https://github.com/nodejs/node/commit/45032a39e8)] - **stream**: fix stream.finished on Duplex (Robert Nagy) [#33133](https://github.com/nodejs/node/pull/33133)
* [[`4cfa7e0716`](https://github.com/nodejs/node/commit/4cfa7e0716)] - **stream**: simplify Readable push/unshift logic (himself65) [#32899](https://github.com/nodejs/node/pull/32899)
* [[`bc40ed31b3`](https://github.com/nodejs/node/commit/bc40ed31b3)] - **stream**: add null check in Readable.from (Pranshu Srivastava) [#32873](https://github.com/nodejs/node/pull/32873)
* [[`b183d0a18a`](https://github.com/nodejs/node/commit/b183d0a18a)] - **stream**: let Duplex re-use Writable properties (Robert Nagy) [#33079](https://github.com/nodejs/node/pull/33079)
* [[`ec24577406`](https://github.com/nodejs/node/commit/ec24577406)] - **v8**: use AliasedBuffers for passing heap statistics around (Joyee Cheung) [#32929](https://github.com/nodejs/node/pull/32929)
* [[`d39254ada6`](https://github.com/nodejs/node/commit/d39254ada6)] - **vm**: fix vm.measureMemory() and introduce execution option (Joyee Cheung) [#32988](https://github.com/nodejs/node/pull/32988)
* [[`4423304ac4`](https://github.com/nodejs/node/commit/4423304ac4)] - **vm**: throw error when duplicated exportNames in SyntheticModule (himself65) [#32810](https://github.com/nodejs/node/pull/32810)
* [[`3866dc1311`](https://github.com/nodejs/node/commit/3866dc1311)] - **wasi**: use free() to release preopen array (Anna Henningsen) [#33110](https://github.com/nodejs/node/pull/33110)
* [[`d7d9960d38`](https://github.com/nodejs/node/commit/d7d9960d38)] - **wasi**: update start() behavior to match spec (Colin Ihrig) [#33073](https://github.com/nodejs/node/pull/33073)
* [[`8d5ac1bbf0`](https://github.com/nodejs/node/commit/8d5ac1bbf0)] - **wasi**: rename \_\_wasi\_unstable\_reactor\_start() (Colin Ihrig) [#33073](https://github.com/nodejs/node/pull/33073)
* [[`c6d632a72a`](https://github.com/nodejs/node/commit/c6d632a72a)] - **worker**: unify custom error creation (Anna Henningsen) [#33084](https://github.com/nodejs/node/pull/33084)

#### Documentation commits

* [[`6925b358f9`](https://github.com/nodejs/node/commit/6925b358f9)] - **doc**: mark assert.CallTracker experimental (Ruben Bridgewater) [#33124](https://github.com/nodejs/node/pull/33124)
* [[`413f5d3581`](https://github.com/nodejs/node/commit/413f5d3581)] - **doc**: add missing deprecation not (Robert Nagy) [#33203](https://github.com/nodejs/node/pull/33203)
* [[`7893bde07e`](https://github.com/nodejs/node/commit/7893bde07e)] - **doc**: fix a typo in crypto.generateKeyPairSync() (himself65) [#33187](https://github.com/nodejs/node/pull/33187)
* [[`d02ced8af6`](https://github.com/nodejs/node/commit/d02ced8af6)] - **doc**: add util.types.isArrayBufferView() (Kevin Locke) [#33092](https://github.com/nodejs/node/pull/33092)
* [[`36d50027af`](https://github.com/nodejs/node/commit/36d50027af)] - **doc**: clarify when not to run CI on docs (Juan José Arboleda) [#33101](https://github.com/nodejs/node/pull/33101)
* [[`a99013718c`](https://github.com/nodejs/node/commit/a99013718c)] - **doc**: fix the spelling error in stream.md (白一梓) [#31561](https://github.com/nodejs/node/pull/31561)
* [[`23962191c1`](https://github.com/nodejs/node/commit/23962191c1)] - **doc**: correct Nodejs to Node.js spelling (Nick Schonning) [#33088](https://github.com/nodejs/node/pull/33088)
* [[`de15edcfc0`](https://github.com/nodejs/node/commit/de15edcfc0)] - **doc**: improve worker pool example (Ranjan Purbey) [#33082](https://github.com/nodejs/node/pull/33082)
* [[`289a5c8dfb`](https://github.com/nodejs/node/commit/289a5c8dfb)] - **doc**: some grammar fixes (Chris Holland) [#33081](https://github.com/nodejs/node/pull/33081)
* [[`82e459d9af`](https://github.com/nodejs/node/commit/82e459d9af)] - **doc**: don't check links in tmp dirs (Ben Noordhuis) [#32996](https://github.com/nodejs/node/pull/32996)
* [[`c5a2f9a02a`](https://github.com/nodejs/node/commit/c5a2f9a02a)] - **doc**: fix markdown parsing on doc/api/os.md (Juan José Arboleda) [#33067](https://github.com/nodejs/node/pull/33067)

#### Other commits

* [[`60ebbc4386`](https://github.com/nodejs/node/commit/60ebbc4386)] - **test**: update c8 ignore comment (Benjamin Coe) [#33151](https://github.com/nodejs/node/pull/33151)
* [[`e276524fcc`](https://github.com/nodejs/node/commit/e276524fcc)] - **test**: skip memory usage tests when ASAN is enabled (Anna Henningsen) [#33129](https://github.com/nodejs/node/pull/33129)
* [[`89ed7a5862`](https://github.com/nodejs/node/commit/89ed7a5862)] - **test**: move test-process-title to sequential (Anna Henningsen) [#33150](https://github.com/nodejs/node/pull/33150)
* [[`af7da46d9b`](https://github.com/nodejs/node/commit/af7da46d9b)] - **test**: fix out-of-bound reads from invalid sizeof usage (Anna Henningsen) [#33115](https://github.com/nodejs/node/pull/33115)
* [[`9ccb6b2e8c`](https://github.com/nodejs/node/commit/9ccb6b2e8c)] - **test**: add missing calls to napi\_async\_destroy (Anna Henningsen) [#33114](https://github.com/nodejs/node/pull/33114)
* [[`3c2f608a8d`](https://github.com/nodejs/node/commit/3c2f608a8d)] - **test**: correct typo in test name (Colin Ihrig) [#33083](https://github.com/nodejs/node/pull/33083)
* [[`92c7e0620f`](https://github.com/nodejs/node/commit/92c7e0620f)] - **test**: check args on SourceTextModule cachedData (Juan José Arboleda) [#32956](https://github.com/nodejs/node/pull/32956)
* [[`f79ef96fea`](https://github.com/nodejs/node/commit/f79ef96fea)] - **test**: mark test flaky on freebsd (Sam Roberts) [#32849](https://github.com/nodejs/node/pull/32849)
* [[`aced1f5d70`](https://github.com/nodejs/node/commit/aced1f5d70)] - **test**: flaky test-stdout-close-catch on freebsd (Sam Roberts) [#32849](https://github.com/nodejs/node/pull/32849)
* [[`6734cc43df`](https://github.com/nodejs/node/commit/6734cc43df)] - **tools**: bump remark-preset-lint-node to 1.15.0 (Rich Trott) [#33157](https://github.com/nodejs/node/pull/33157)
* [[`a87d371014`](https://github.com/nodejs/node/commit/a87d371014)] - **tools**: fix redundant-move warning in inspector (Daniel Bevenius) [#32685](https://github.com/nodejs/node/pull/32685)
* [[`12426f59f5`](https://github.com/nodejs/node/commit/12426f59f5)] - **tools**: update remark-preset-lint-node@1.14.0 (Rich Trott) [#33072](https://github.com/nodejs/node/pull/33072)
* [[`8c40ffc329`](https://github.com/nodejs/node/commit/8c40ffc329)] - **tools**: update broken types in type parser (Colin Ihrig) [#33068](https://github.com/nodejs/node/pull/33068)

<a id="14.1.0"></a>
## 2020-04-29, Version 14.1.0 (Current), @BethGriggs

### Notable Changes

* **deps**: upgrade openssl sources to 1.1.1g (Hassaan Pasha) [#32971](https://github.com/nodejs/node/pull/32971)
* **doc**: add juanarbol as collaborator (Juan José Arboleda) [#32906](https://github.com/nodejs/node/pull/32906)
* **http**: doc deprecate abort and improve docs (Robert Nagy) [#32807](https://github.com/nodejs/node/pull/32807)
* **module**: do not warn when accessing `__esModule` of unfinished exports (Anna Henningsen) [#33048](https://github.com/nodejs/node/pull/33048)
* **n-api**: detect deadlocks in thread-safe function (Gabriel Schulhof) [#32860](https://github.com/nodejs/node/pull/32860)
* **src**: deprecate embedder APIs with replacements (Anna Henningsen) [#32858](https://github.com/nodejs/node/pull/32858)
* **stream**:
  * don't emit end after close (Robert Nagy) [#33076](https://github.com/nodejs/node/pull/33076)
  * don't wait for close on legacy streams (Robert Nagy) [#33058](https://github.com/nodejs/node/pull/33058)
  * pipeline should only destroy un-finished streams (Robert Nagy) [#32968](https://github.com/nodejs/node/pull/32968)
* **vm**: add importModuleDynamically option to compileFunction (Gus Caplan) [#32985](https://github.com/nodejs/node/pull/32985)

### Commits

* [[`1af08e1c5e`](https://github.com/nodejs/node/commit/1af08e1c5e)] - **buffer,n-api**: fix double ArrayBuffer::Detach() during cleanup (Anna Henningsen) [#33039](https://github.com/nodejs/node/pull/33039)
* [[`91e30e35a1`](https://github.com/nodejs/node/commit/91e30e35a1)] - **build**: fix vcbuild error for missing Visual Studio (Thomas) [#32658](https://github.com/nodejs/node/pull/32658)
* [[`4035cbe631`](https://github.com/nodejs/node/commit/4035cbe631)] - **cluster**: removed unused addressType argument from constructor (Yash Ladha) [#32963](https://github.com/nodejs/node/pull/32963)
* [[`56f50aeff0`](https://github.com/nodejs/node/commit/56f50aeff0)] - **deps**: patch V8 to 8.1.307.31 (Michaël Zasso) [#33080](https://github.com/nodejs/node/pull/33080)
* [[`fad188fe23`](https://github.com/nodejs/node/commit/fad188fe23)] - **deps**: update archs files for OpenSSL-1.1.1g (Hassaan Pasha) [#32971](https://github.com/nodejs/node/pull/32971)
* [[`b12f1630fc`](https://github.com/nodejs/node/commit/b12f1630fc)] - **deps**: upgrade openssl sources to 1.1.1g (Hassaan Pasha) [#32971](https://github.com/nodejs/node/pull/32971)
* [[`b50333e001`](https://github.com/nodejs/node/commit/b50333e001)] - **deps**: V8: backport 3f8dc4b2e5ba (Ujjwal Sharma) [#32993](https://github.com/nodejs/node/pull/32993)
* [[`bbed1e56cd`](https://github.com/nodejs/node/commit/bbed1e56cd)] - **deps**: V8: cherry-pick e1eac1b16c96 (Milad Farazmand) [#32974](https://github.com/nodejs/node/pull/32974)
* [[`3fed735099`](https://github.com/nodejs/node/commit/3fed735099)] - **doc**: fix LTS replaceme tags (Anna Henningsen) [#33041](https://github.com/nodejs/node/pull/33041)
* [[`343c6ac639`](https://github.com/nodejs/node/commit/343c6ac639)] - **doc**: assign missing deprecation code (Richard Lau) [#33109](https://github.com/nodejs/node/pull/33109)
* [[`794b8796dd`](https://github.com/nodejs/node/commit/794b8796dd)] - **doc**: improve WHATWG url constructor code example (Liran Tal) [#32782](https://github.com/nodejs/node/pull/32782)
* [[`14e559df87`](https://github.com/nodejs/node/commit/14e559df87)] - **doc**: make openssl maintenance position independent (Sam Roberts) [#32977](https://github.com/nodejs/node/pull/32977)
* [[`8a4de2ef25`](https://github.com/nodejs/node/commit/8a4de2ef25)] - **doc**: improve release documentation (Michaël Zasso) [#33042](https://github.com/nodejs/node/pull/33042)
* [[`401ab610e7`](https://github.com/nodejs/node/commit/401ab610e7)] - **doc**: document major finished changes in v14 (Robert Nagy) [#33065](https://github.com/nodejs/node/pull/33065)
* [[`a534d8282c`](https://github.com/nodejs/node/commit/a534d8282c)] - **doc**: add documentation for transferList arg at worker threads (Juan José Arboleda) [#32881](https://github.com/nodejs/node/pull/32881)
* [[`f116825d56`](https://github.com/nodejs/node/commit/f116825d56)] - **doc**: avoid tautology in README (Ishaan Jain) [#33005](https://github.com/nodejs/node/pull/33005)
* [[`7e9f88e005`](https://github.com/nodejs/node/commit/7e9f88e005)] - **doc**: updated directory entry information (Eileen) [#32791](https://github.com/nodejs/node/pull/32791)
* [[`bf331b4e21`](https://github.com/nodejs/node/commit/bf331b4e21)] - **doc**: ignore no-literal-urls in README (Nick Schonning) [#32676](https://github.com/nodejs/node/pull/32676)
* [[`f92b398c76`](https://github.com/nodejs/node/commit/f92b398c76)] - **doc**: convert bare email addresses to mailto links (Nick Schonning) [#32676](https://github.com/nodejs/node/pull/32676)
* [[`2bde11607d`](https://github.com/nodejs/node/commit/2bde11607d)] - **doc**: ignore no-literal-urls in changelogs (Nick Schonning) [#32676](https://github.com/nodejs/node/pull/32676)
* [[`71f90234f9`](https://github.com/nodejs/node/commit/71f90234f9)] - **doc**: add angle brackets around implicit links (Nick Schonning) [#32676](https://github.com/nodejs/node/pull/32676)
* [[`aec7bc754e`](https://github.com/nodejs/node/commit/aec7bc754e)] - **doc**: remove repeated word in modules.md (Prosper Opara) [#32931](https://github.com/nodejs/node/pull/32931)
* [[`005c2bab29`](https://github.com/nodejs/node/commit/005c2bab29)] - **doc**: elevate diagnostic report to tier1 (Gireesh Punathil) [#32732](https://github.com/nodejs/node/pull/32732)
* [[`4dd3a7ddc9`](https://github.com/nodejs/node/commit/4dd3a7ddc9)] - **doc**: set module version 83 to node 14 (Gerhard Stoebich) [#32975](https://github.com/nodejs/node/pull/32975)
* [[`b5b3efeb90`](https://github.com/nodejs/node/commit/b5b3efeb90)] - **doc**: add more info to v14 changelog (Gus Caplan) [#32979](https://github.com/nodejs/node/pull/32979)
* [[`f6be140222`](https://github.com/nodejs/node/commit/f6be140222)] - **doc**: fix typo in security-release-process.md (Edward Elric) [#32926](https://github.com/nodejs/node/pull/32926)
* [[`fa710732bf`](https://github.com/nodejs/node/commit/fa710732bf)] - **doc**: corrected ERR\_SOCKET\_CANNOT\_SEND message (William Armiros) [#32847](https://github.com/nodejs/node/pull/32847)
* [[`68b7c80a44`](https://github.com/nodejs/node/commit/68b7c80a44)] - **doc**: fix usage of folder and directory terms in fs.md (karan singh virdi) [#32919](https://github.com/nodejs/node/pull/32919)
* [[`57c170c75c`](https://github.com/nodejs/node/commit/57c170c75c)] - **doc**: fix typo in zlib.md (雨夜带刀) [#32901](https://github.com/nodejs/node/pull/32901)
* [[`a8ed8f5d0a`](https://github.com/nodejs/node/commit/a8ed8f5d0a)] - **doc**: synch SECURITY.md with website (Rich Trott) [#32903](https://github.com/nodejs/node/pull/32903)
* [[`ccf6d3e5ed`](https://github.com/nodejs/node/commit/ccf6d3e5ed)] - **doc**: add `tsc-agenda` to onboarding labels list (Rich Trott) [#32832](https://github.com/nodejs/node/pull/32832)
* [[`fc71a85c49`](https://github.com/nodejs/node/commit/fc71a85c49)] - **doc**: add N-API version 6 to table (Michael Dawson) [#32829](https://github.com/nodejs/node/pull/32829)
* [[`87605f0ed3`](https://github.com/nodejs/node/commit/87605f0ed3)] - **doc**: add juanarbol as collaborator (Juan José Arboleda) [#32906](https://github.com/nodejs/node/pull/32906)
* [[`4c643c0d42`](https://github.com/nodejs/node/commit/4c643c0d42)] - **fs**: update validateOffsetLengthRead in utils.js (daemon1024) [#32896](https://github.com/nodejs/node/pull/32896)
* [[`baa8231728`](https://github.com/nodejs/node/commit/baa8231728)] - **fs**: extract kWriteFileMaxChunkSize constant (rickyes) [#32640](https://github.com/nodejs/node/pull/32640)
* [[`03d02d74f3`](https://github.com/nodejs/node/commit/03d02d74f3)] - **fs**: remove unnecessary else statement (Jesus Hernandez) [#32662](https://github.com/nodejs/node/pull/32662)
* [[`31c797cb11`](https://github.com/nodejs/node/commit/31c797cb11)] - **http**: doc deprecate abort and improve docs (Robert Nagy) [#32807](https://github.com/nodejs/node/pull/32807)
* [[`4ef91a640e`](https://github.com/nodejs/node/commit/4ef91a640e)] - **http2**: wait for secureConnect before initializing (Benjamin Coe) [#32958](https://github.com/nodejs/node/pull/32958)
* [[`6fc4d174b5`](https://github.com/nodejs/node/commit/6fc4d174b5)] - **http2**: refactor and cleanup http2 (James M Snell) [#32884](https://github.com/nodejs/node/pull/32884)
* [[`4b6aa077fe`](https://github.com/nodejs/node/commit/4b6aa077fe)] - **inspector**: only write coverage in fully bootstrapped Environments (Joyee Cheung) [#32960](https://github.com/nodejs/node/pull/32960)
* [[`737bd6205b`](https://github.com/nodejs/node/commit/737bd6205b)] - **lib**: unnecessary const assignment for class (Yash Ladha) [#32962](https://github.com/nodejs/node/pull/32962)
* [[`98b30b06ff`](https://github.com/nodejs/node/commit/98b30b06ff)] - **lib**: simplify function process.emitWarning (himself65) [#32992](https://github.com/nodejs/node/pull/32992)
* [[`b957895ff7`](https://github.com/nodejs/node/commit/b957895ff7)] - **lib**: remove unnecesary else block (David Daza) [#32644](https://github.com/nodejs/node/pull/32644)
* [[`cb4d8ce889`](https://github.com/nodejs/node/commit/cb4d8ce889)] - **module**: refactor condition (Myles Borins) [#32989](https://github.com/nodejs/node/pull/32989)
* [[`4abc45a4b9`](https://github.com/nodejs/node/commit/4abc45a4b9)] - **module**: do not warn when accessing `__esModule` of unfinished exports (Anna Henningsen) [#33048](https://github.com/nodejs/node/pull/33048)
* [[`21d314e7fc`](https://github.com/nodejs/node/commit/21d314e7fc)] - **module**: exports not exported for null resolutions (Guy Bedford) [#32838](https://github.com/nodejs/node/pull/32838)
* [[`eaf841d585`](https://github.com/nodejs/node/commit/eaf841d585)] - **module**: improve error for invalid package targets (Myles Borins) [#32052](https://github.com/nodejs/node/pull/32052)
* [[`8663fd5f88`](https://github.com/nodejs/node/commit/8663fd5f88)] - **module**: partial doc removal of --experimental-modules (Myles Borins) [#32915](https://github.com/nodejs/node/pull/32915)
* [[`68656cf588`](https://github.com/nodejs/node/commit/68656cf588)] - **n-api**: fix false assumption on napi\_async\_context structures (legendecas) [#32928](https://github.com/nodejs/node/pull/32928)
* [[`861eb39307`](https://github.com/nodejs/node/commit/861eb39307)] - **(SEMVER-MINOR)** **n-api**: detect deadlocks in thread-safe function (Gabriel Schulhof) [#32860](https://github.com/nodejs/node/pull/32860)
* [[`a133ac17eb`](https://github.com/nodejs/node/commit/a133ac17eb)] - **perf_hooks**: remove unnecessary assignment when name is undefined (rickyes) [#32910](https://github.com/nodejs/node/pull/32910)
* [[`59b64adb79`](https://github.com/nodejs/node/commit/59b64adb79)] - **src**: add AsyncWrapObject constructor template factory (Stephen Belanger) [#33051](https://github.com/nodejs/node/pull/33051)
* [[`23eda417b6`](https://github.com/nodejs/node/commit/23eda417b6)] - **src**: do not compare against wide characters (Christopher Beeson) [#32921](https://github.com/nodejs/node/pull/32921)
* [[`d10c2c6968`](https://github.com/nodejs/node/commit/d10c2c6968)] - **src**: fix empty-named env var assertion failure (Christopher Beeson) [#32921](https://github.com/nodejs/node/pull/32921)
* [[`44c157e45d`](https://github.com/nodejs/node/commit/44c157e45d)] - **src**: assignment to valid type (Yash Ladha) [#32879](https://github.com/nodejs/node/pull/32879)
* [[`d82c3c28de`](https://github.com/nodejs/node/commit/d82c3c28de)] - **src**: delete MicroTaskPolicy namespace (Juan José Arboleda) [#32853](https://github.com/nodejs/node/pull/32853)
* [[`bc755fc4c2`](https://github.com/nodejs/node/commit/bc755fc4c2)] - **src**: fix compiler warnings in node\_http2.cc (Daniel Bevenius) [#33014](https://github.com/nodejs/node/pull/33014)
* [[`30c2b0f798`](https://github.com/nodejs/node/commit/30c2b0f798)] - **(SEMVER-MINOR)** **src**: deprecate embedder APIs with replacements (Anna Henningsen) [#32858](https://github.com/nodejs/node/pull/32858)
* [[`95e897edfc`](https://github.com/nodejs/node/commit/95e897edfc)] - **src**: use using NewStringType (rickyes) [#32843](https://github.com/nodejs/node/pull/32843)
* [[`4221b1c8c9`](https://github.com/nodejs/node/commit/4221b1c8c9)] - **src**: fix null deref in AllocatedBuffer::clear (Matt Kulukundis) [#32892](https://github.com/nodejs/node/pull/32892)
* [[`f9b8988df6`](https://github.com/nodejs/node/commit/f9b8988df6)] - **src**: remove validation of unreachable code (Juan José Arboleda) [#32818](https://github.com/nodejs/node/pull/32818)
* [[`307e43da4d`](https://github.com/nodejs/node/commit/307e43da4d)] - **src**: elevate v8 namespaces (Nimit) [#32872](https://github.com/nodejs/node/pull/32872)
* [[`ca7e0a226e`](https://github.com/nodejs/node/commit/ca7e0a226e)] - **src**: remove redundant v8::HeapSnapshot namespace (Juan José Arboleda) [#32854](https://github.com/nodejs/node/pull/32854)
* [[`ae157b8ca7`](https://github.com/nodejs/node/commit/ae157b8ca7)] - **(SEMVER-MINOR)** **stream**: don't emit end after close (Robert Nagy) [#33076](https://github.com/nodejs/node/pull/33076)
* [[`184e80a144`](https://github.com/nodejs/node/commit/184e80a144)] - **stream**: don't wait for close on legacy streams (Robert Nagy) [#33058](https://github.com/nodejs/node/pull/33058)
* [[`e07c4ffc39`](https://github.com/nodejs/node/commit/e07c4ffc39)] - **stream**: fix sync write perf regression (Robert Nagy) [#33032](https://github.com/nodejs/node/pull/33032)
* [[`2bb4ac409b`](https://github.com/nodejs/node/commit/2bb4ac409b)] - **stream**: avoid drain for sync streams (Robert Nagy) [#32887](https://github.com/nodejs/node/pull/32887)
* [[`c21f1f03c5`](https://github.com/nodejs/node/commit/c21f1f03c5)] - **stream**: removes unnecessary params (Jesus Hernandez) [#32936](https://github.com/nodejs/node/pull/32936)
* [[`4c10b5f378`](https://github.com/nodejs/node/commit/4c10b5f378)] - **stream**: consistent punctuation (Robert Nagy) [#32934](https://github.com/nodejs/node/pull/32934)
* [[`1a2b3eb3a4`](https://github.com/nodejs/node/commit/1a2b3eb3a4)] - **stream**: fix broken pipeline test (Robert Nagy) [#33030](https://github.com/nodejs/node/pull/33030)
* [[`7abc61f668`](https://github.com/nodejs/node/commit/7abc61f668)] - **stream**: refactor Writable buffering (Robert Nagy) [#31046](https://github.com/nodejs/node/pull/31046)
* [[`180b935b58`](https://github.com/nodejs/node/commit/180b935b58)] - **stream**: pipeline should only destroy un-finished streams (Robert Nagy) [#32968](https://github.com/nodejs/node/pull/32968)
* [[`7647860000`](https://github.com/nodejs/node/commit/7647860000)] - **stream**: finished should complete with read-only Duplex (Robert Nagy) [#32967](https://github.com/nodejs/node/pull/32967)
* [[`36a4f54d69`](https://github.com/nodejs/node/commit/36a4f54d69)] - **stream**: close iterator in Readable.from (Vadzim Zieńka) [#32844](https://github.com/nodejs/node/pull/32844)
* [[`7f498125e4`](https://github.com/nodejs/node/commit/7f498125e4)] - **stream**: inline unbuffered \_write (Robert Nagy) [#32886](https://github.com/nodejs/node/pull/32886)
* [[`2ab4ebc8bf`](https://github.com/nodejs/node/commit/2ab4ebc8bf)] - **stream**: simplify Writable.end() (Robert Nagy) [#32882](https://github.com/nodejs/node/pull/32882)
* [[`11ea13f96c`](https://github.com/nodejs/node/commit/11ea13f96c)] - **test**: refactor test-async-hooks-constructor (himself65) [#33063](https://github.com/nodejs/node/pull/33063)
* [[`8fad112d93`](https://github.com/nodejs/node/commit/8fad112d93)] - **test**: remove timers-blocking-callback (Jeremiah Senkpiel) [#32870](https://github.com/nodejs/node/pull/32870)
* [[`988c2fe287`](https://github.com/nodejs/node/commit/988c2fe287)] - **test**: better error validations for event-capture (Adrian Estrada) [#32771](https://github.com/nodejs/node/pull/32771)
* [[`45e188b2e3`](https://github.com/nodejs/node/commit/45e188b2e3)] - **test**: refactor events tests for invalid listeners (Adrian Estrada) [#32769](https://github.com/nodejs/node/pull/32769)
* [[`b4ef06267d`](https://github.com/nodejs/node/commit/b4ef06267d)] - **test**: test-async-wrap-constructor prefer forEach (Daniel Estiven Rico Posada) [#32631](https://github.com/nodejs/node/pull/32631)
* [[`c9ae385abf`](https://github.com/nodejs/node/commit/c9ae385abf)] - **test**: mark test-child-process-fork-args as flaky on Windows (Andrey Pechkurov) [#32950](https://github.com/nodejs/node/pull/32950)
* [[`b12204e27e`](https://github.com/nodejs/node/commit/b12204e27e)] - **test**: changed function to arrow function (Nimit) [#32875](https://github.com/nodejs/node/pull/32875)
* [[`323da6f251`](https://github.com/nodejs/node/commit/323da6f251)] - **tls**: add highWaterMark option for connect (rickyes) [#32786](https://github.com/nodejs/node/pull/32786)
* [[`308681307f`](https://github.com/nodejs/node/commit/308681307f)] - **tls**: move getAllowUnauthorized to internal/options (James M Snell) [#32917](https://github.com/nodejs/node/pull/32917)
* [[`6a8e266a3b`](https://github.com/nodejs/node/commit/6a8e266a3b)] - **tools**: update ESLint to 7.0.0-rc.0 (himself65) [#33062](https://github.com/nodejs/node/pull/33062)
* [[`fa7d969237`](https://github.com/nodejs/node/commit/fa7d969237)] - **tools**: remove unused code in doc generation tool (Rich Trott) [#32913](https://github.com/nodejs/node/pull/32913)
* [[`ca5ebcfb67`](https://github.com/nodejs/node/commit/ca5ebcfb67)] - **tools**: fix mkcodecache when run with ASAN (Anna Henningsen) [#32850](https://github.com/nodejs/node/pull/32850)
* [[`22ccf2ba1f`](https://github.com/nodejs/node/commit/22ccf2ba1f)] - **tools**: decrease timeout in test.py (Anna Henningsen) [#32868](https://github.com/nodejs/node/pull/32868)
* [[`c82c08416f`](https://github.com/nodejs/node/commit/c82c08416f)] - **util,readline**: NFC-normalize strings before getStringWidth (Anna Henningsen) [#33052](https://github.com/nodejs/node/pull/33052)
* [[`4143c747fc`](https://github.com/nodejs/node/commit/4143c747fc)] - **(SEMVER-MINOR)** **vm**: add importModuleDynamically option to compileFunction (Gus Caplan) [#32985](https://github.com/nodejs/node/pull/32985)
* [[`c84d802449`](https://github.com/nodejs/node/commit/c84d802449)] - **worker**: fix process.env var empty key access (Christopher Beeson) [#32921](https://github.com/nodejs/node/pull/32921)

<a id="14.0.0"></a>
## 2020-04-21, Version 14.0.0 (Current), @BethGriggs

### Notable Changes

#### Deprecations

* **(SEMVER-MAJOR)** **crypto**: move pbkdf2 without digest to EOL (James M Snell) [#31166](https://github.com/nodejs/node/pull/31166)
* **(SEMVER-MAJOR)** **fs**: deprecate closing FileHandle on garbage collection (James M Snell) [#28396](https://github.com/nodejs/node/pull/28396)
* **(SEMVER-MAJOR)** **http**: move OutboundMessage.prototype.flush to EOL (James M Snell) [#31164](https://github.com/nodejs/node/pull/31164)
* **(SEMVER-MAJOR)** **lib**: move GLOBAL and root aliases to EOL (James M Snell) [#31167](https://github.com/nodejs/node/pull/31167)
* **(SEMVER-MAJOR)** **os**: move tmpDir() to EOL (James M Snell) [#31169](https://github.com/nodejs/node/pull/31169)
* **(SEMVER-MAJOR)** **src**: remove deprecated wasm type check (Clemens Backes) [#32116](https://github.com/nodejs/node/pull/32116)
* **(SEMVER-MAJOR)** **stream**: move \_writableState.buffer to EOL (James M Snell) [#31165](https://github.com/nodejs/node/pull/31165)
* **(SEMVER-MINOR)** **doc**: deprecate process.mainModule (Antoine du HAMEL) [#32232](https://github.com/nodejs/node/pull/32232)
* **(SEMVER-MINOR)** **doc**: deprecate process.umask() with no arguments (Colin Ihrig) [#32499](https://github.com/nodejs/node/pull/32499)

#### ECMAScript Modules - Experimental Warning Removal

* **module**: remove experimental modules warning (Guy Bedford) [#31974](https://github.com/nodejs/node/pull/31974)

In Node.js 13 we removed the need to include the --experimental-modules flag, but when running EcmaScript Modules in Node.js, this would still result in a warning ExperimentalWarning: The ESM module loader is experimental.

As of Node.js 14 there is no longer this warning when using ESM in Node.js. However, the ESM implementation in Node.js remains experimental. As per our stability index: “The feature is not subject to Semantic Versioning rules. Non-backward compatible changes or removal may occur in any future release.” Users should be cautious when using the feature in production environments.

Please keep in mind that the implementation of ESM in Node.js differs from the developer experience you might be familiar with. Most transpilation workflows support features such as optional file extensions or JSON modules that the Node.js ESM implementation does not support. It is highly likely that modules from transpiled environments will require a certain degree of refactoring to work in Node.js. It is worth mentioning that many of our design decisions were made with two primary goals. Spec compliance and Web Compatibility. It is our belief that the current implementation offers a future proof model to authoring ESM modules that paves the path to Universal JavaScript. Please read more in our documentation.

The ESM implementation in Node.js is still experimental but we do believe that we are getting very close to being able to call ESM in Node.js “stable”. Removing the warning is a huge step in that direction.

#### New V8 ArrayBuffer API

* **src**: migrate to new V8 ArrayBuffer API (Thang Tran) [#30782](https://github.com/nodejs/node/pull/30782)

Multiple ArrayBuffers pointing to the same base address are no longer allowed by V8. This may impact native addons.

#### Toolchain and Compiler Upgrades

* **(SEMVER-MAJOR)** **build**: update macos deployment target to 10.13 for 14.x (AshCripps) [#32454](https://github.com/nodejs/node/pull/32454)
* **(SEMVER-MAJOR)** **doc**: update cross compiler machine for Linux armv7 (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* **(SEMVER-MAJOR)** **doc**: update Centos/RHEL releases use devtoolset-8 (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* **(SEMVER-MAJOR)** **doc**: remove SmartOS from official binaries (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* **(SEMVER-MAJOR)** **win**: block running on EOL Windows versions (João Reis) [#31954](https://github.com/nodejs/node/pull/31954)

It is expected that there will be an ABI mismatch on ARM between the Node.js binary and native addons. Native addons are only broken if they
interact with `std::shared_ptr`. This is expected to be fixed in a later version of Node.js 14. - [#30786](https://github.com/nodejs/node/issues/30786)

#### Update to V8 8.1

* **(SEMVER-MAJOR)** **deps**: update V8 to 8.1.307.20 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
  * Enables Optional Chaining by default ([MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Optional_chaining), [v8.dev](https://v8.dev/features/optional-chaining))
  * Enables Nullish Coalescing by default ([MDN](https://wiki.developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Nullish_Coalescing_Operator), [v8.dev](https://v8.dev/features/nullish-coalescing))
  * Enables `Intl.DisplayNames` by default ([MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/DisplayNames), [v8.dev](https://v8.dev/features/intl-displaynames))
  * Enables `calendar` and `numberingSystem` options for `Intl.DateTimeFormat` by default ([MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/DateTimeFormat))

#### Other Notable Changes:

* **cli, report**: move --report-on-fatalerror to stable (Colin Ihrig) [#32496](https://github.com/nodejs/node/pull/32496)
* **deps**: upgrade to libuv 1.37.0 (Colin Ihrig) [#32866](https://github.com/nodejs/node/pull/32866)
* **fs**: add fs/promises alias module (Gus Caplan) [#31553](https://github.com/nodejs/node/pull/31553)

### Semver-Major Commits

* [[`5360dd151d`](https://github.com/nodejs/node/commit/5360dd151d)] - **(SEMVER-MAJOR)** **assert**: handle (deep) equal(NaN, NaN) as being identical (Ruben Bridgewater) [#30766](https://github.com/nodejs/node/pull/30766)
* [[`a621608f12`](https://github.com/nodejs/node/commit/a621608f12)] - **(SEMVER-MAJOR)** **build**: update macos deployment target to 10.13 for 14.x (AshCripps) [#32454](https://github.com/nodejs/node/pull/32454)
* [[`e65bed1b7e`](https://github.com/nodejs/node/commit/e65bed1b7e)] - **(SEMVER-MAJOR)** **child_process**: create proper public API for `channel` (Anna Henningsen) [#30165](https://github.com/nodejs/node/pull/30165)
* [[`1b9a62cff4`](https://github.com/nodejs/node/commit/1b9a62cff4)] - **(SEMVER-MAJOR)** **crypto**: make DH error messages consistent (Tobias Nießen) [#31873](https://github.com/nodejs/node/pull/31873)
* [[`bffa5044c5`](https://github.com/nodejs/node/commit/bffa5044c5)] - **(SEMVER-MAJOR)** **crypto**: move pbkdf2 without digest to EOL (James M Snell) [#31166](https://github.com/nodejs/node/pull/31166)
* [[`10f5fa7513`](https://github.com/nodejs/node/commit/10f5fa7513)] - **(SEMVER-MAJOR)** **crypto**: forbid setting the PBKDF2 iter count to 0 (Tobias Nießen) [#30578](https://github.com/nodejs/node/pull/30578)
* [[`2883c855e0`](https://github.com/nodejs/node/commit/2883c855e0)] - **(SEMVER-MAJOR)** **deps**: update V8 to 8.1.307.20 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`1b2e2944bc`](https://github.com/nodejs/node/commit/1b2e2944bc)] - **(SEMVER-MAJOR)** **dgram**: don't hide implicit bind errors (Colin Ihrig) [#31958](https://github.com/nodejs/node/pull/31958)
* [[`1a1ce93317`](https://github.com/nodejs/node/commit/1a1ce93317)] - **(SEMVER-MAJOR)** **doc**: update cross compiler machine for Linux armv7 (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* [[`dad96e4fc1`](https://github.com/nodejs/node/commit/dad96e4fc1)] - **(SEMVER-MAJOR)** **doc**: update Centos/RHEL releases use devtoolset-8 (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* [[`5317202aa1`](https://github.com/nodejs/node/commit/5317202aa1)] - **(SEMVER-MAJOR)** **doc**: remove SmartOS from official binaries (Richard Lau) [#32812](https://github.com/nodejs/node/pull/32812)
* [[`75ee5b2622`](https://github.com/nodejs/node/commit/75ee5b2622)] - **(SEMVER-MAJOR)** **doc**: deprecate process.umask() with no arguments (Colin Ihrig) [#32499](https://github.com/nodejs/node/pull/32499)
* [[`afe353061b`](https://github.com/nodejs/node/commit/afe353061b)] - **(SEMVER-MAJOR)** **doc**: fs.write is not longer coercing strings (Juan José Arboleda) [#31030](https://github.com/nodejs/node/pull/31030)
* [[`a45c1aa39f`](https://github.com/nodejs/node/commit/a45c1aa39f)] - **(SEMVER-MAJOR)** **doc**: fix mode and flags being mistaken in fs (Ruben Bridgewater) [#27044](https://github.com/nodejs/node/pull/27044)
* [[`331d636240`](https://github.com/nodejs/node/commit/331d636240)] - **(SEMVER-MAJOR)** **errors**: remove unused ERR\_SOCKET\_CANNOT\_SEND error (Colin Ihrig) [#31958](https://github.com/nodejs/node/pull/31958)
* [[`b8e41774d4`](https://github.com/nodejs/node/commit/b8e41774d4)] - **(SEMVER-MAJOR)** **fs**: add fs/promises alias module (Gus Caplan) [#31553](https://github.com/nodejs/node/pull/31553)
* [[`fb6df3bfac`](https://github.com/nodejs/node/commit/fb6df3bfac)] - **(SEMVER-MAJOR)** **fs**: validate the input data to be of expected types (Ruben Bridgewater) [#31030](https://github.com/nodejs/node/pull/31030)
* [[`2d8febceef`](https://github.com/nodejs/node/commit/2d8febceef)] - **(SEMVER-MAJOR)** **fs**: deprecate closing FileHandle on garbage collection (James M Snell) [#28396](https://github.com/nodejs/node/pull/28396)
* [[`67e067eb06`](https://github.com/nodejs/node/commit/67e067eb06)] - **(SEMVER-MAJOR)** **fs**: watch signals for recursive incompatibility (Eran Levin) [#29947](https://github.com/nodejs/node/pull/29947)
* [[`f0d2df41f8`](https://github.com/nodejs/node/commit/f0d2df41f8)] - **(SEMVER-MAJOR)** **fs**: change streams to always emit close by default (Robert Nagy) [#31408](https://github.com/nodejs/node/pull/31408)
* [[`a13500f503`](https://github.com/nodejs/node/commit/a13500f503)] - **(SEMVER-MAJOR)** **fs**: improve mode and flags validation (Ruben Bridgewater) [#27044](https://github.com/nodejs/node/pull/27044)
* [[`535e9571f5`](https://github.com/nodejs/node/commit/535e9571f5)] - **(SEMVER-MAJOR)** **fs**: make FSStatWatcher.start private (Lucas Holmquist) [#29971](https://github.com/nodejs/node/pull/29971)
* [[`c1b2f6afbe`](https://github.com/nodejs/node/commit/c1b2f6afbe)] - **(SEMVER-MAJOR)** **http**: detach socket from IncomingMessage on keep-alive (Robert Nagy) [#32153](https://github.com/nodejs/node/pull/32153)
* [[`173d044d09`](https://github.com/nodejs/node/commit/173d044d09)] - **(SEMVER-MAJOR)** **http**: align OutgoingMessage and ClientRequest destroy (Robert Nagy) [#32148](https://github.com/nodejs/node/pull/32148)
* [[`d3715c76b5`](https://github.com/nodejs/node/commit/d3715c76b5)] - **(SEMVER-MAJOR)** **http**: move OutboundMessage.prototype.flush to EOL (James M Snell) [#31164](https://github.com/nodejs/node/pull/31164)
* [[`c776a37791`](https://github.com/nodejs/node/commit/c776a37791)] - **(SEMVER-MAJOR)** **http**: end with data can cause write after end (Robert Nagy) [#28666](https://github.com/nodejs/node/pull/28666)
* [[`ff2ed3ec85`](https://github.com/nodejs/node/commit/ff2ed3ec85)] - **(SEMVER-MAJOR)** **http**: remove unused hasItems() from freelist (Rich Trott) [#30744](https://github.com/nodejs/node/pull/30744)
* [[`d247a8e1dc`](https://github.com/nodejs/node/commit/d247a8e1dc)] - **(SEMVER-MAJOR)** **http**: emit close on socket re-use (Robert Nagy) [#28685](https://github.com/nodejs/node/pull/28685)
* [[`6f0ec79e42`](https://github.com/nodejs/node/commit/6f0ec79e42)] - **(SEMVER-MAJOR)** **http,stream**: make virtual methods throw an error (Luigi Pinca) [#31912](https://github.com/nodejs/node/pull/31912)
* [[`ec0dd6fa1c`](https://github.com/nodejs/node/commit/ec0dd6fa1c)] - **(SEMVER-MAJOR)** **lib**: move GLOBAL and root aliases to EOL (James M Snell) [#31167](https://github.com/nodejs/node/pull/31167)
* [[`d7452b7140`](https://github.com/nodejs/node/commit/d7452b7140)] - **(SEMVER-MAJOR)** **module**: warn on using unfinished circular dependency (Anna Henningsen) [#29935](https://github.com/nodejs/node/pull/29935)
* [[`eeccd52b4e`](https://github.com/nodejs/node/commit/eeccd52b4e)] - **(SEMVER-MAJOR)** **net**: make readable/writable start as true (Robert Nagy) [#32272](https://github.com/nodejs/node/pull/32272)
* [[`ab4115f17c`](https://github.com/nodejs/node/commit/ab4115f17c)] - **(SEMVER-MAJOR)** **os**: move tmpDir() to EOL (James M Snell) [#31169](https://github.com/nodejs/node/pull/31169)
* [[`8c18e91c8a`](https://github.com/nodejs/node/commit/8c18e91c8a)] - **(SEMVER-MAJOR)** **process**: remove undocumented `now` argument from emitWarning() (Rich Trott) [#31643](https://github.com/nodejs/node/pull/31643)
* [[`84c426cb60`](https://github.com/nodejs/node/commit/84c426cb60)] - **(SEMVER-MAJOR)** **repl**: properly handle `repl.repl` (Ruben Bridgewater) [#30981](https://github.com/nodejs/node/pull/30981)
* [[`4f523c2c1a`](https://github.com/nodejs/node/commit/4f523c2c1a)] - **(SEMVER-MAJOR)** **src**: migrate to new V8 ArrayBuffer API (Thang Tran) [#30782](https://github.com/nodejs/node/pull/30782)
* [[`c712fb7cd6`](https://github.com/nodejs/node/commit/c712fb7cd6)] - **(SEMVER-MAJOR)** **src**: add abstract `IsolatePlatformDelegate` (Marcel Laverdet) [#30324](https://github.com/nodejs/node/pull/30324)
* [[`1428a92492`](https://github.com/nodejs/node/commit/1428a92492)] - **(SEMVER-MAJOR)** **stream**: make pipeline try to wait for 'close' (Robert Nagy) [#32158](https://github.com/nodejs/node/pull/32158)
* [[`388cef61e8`](https://github.com/nodejs/node/commit/388cef61e8)] - **(SEMVER-MAJOR)** **stream**: align stream.Duplex with net.Socket (Robert Nagy) [#32139](https://github.com/nodejs/node/pull/32139)
* [[`7cafd5f3a9`](https://github.com/nodejs/node/commit/7cafd5f3a9)] - **(SEMVER-MAJOR)** **stream**: fix finished w/ 'close' before 'end' (Robert Nagy) [#31545](https://github.com/nodejs/node/pull/31545)
* [[`311e12b962`](https://github.com/nodejs/node/commit/311e12b962)] - **(SEMVER-MAJOR)** **stream**: fix multiple destroy calls (Robert Nagy) [#29197](https://github.com/nodejs/node/pull/29197)
* [[`1f209129c7`](https://github.com/nodejs/node/commit/1f209129c7)] - **(SEMVER-MAJOR)** **stream**: throw invalid argument errors (Robert Nagy) [#31831](https://github.com/nodejs/node/pull/31831)
* [[`d016b9d708`](https://github.com/nodejs/node/commit/d016b9d708)] - **(SEMVER-MAJOR)** **stream**: finished callback for closed streams (Robert Nagy) [#31509](https://github.com/nodejs/node/pull/31509)
* [[`e559842188`](https://github.com/nodejs/node/commit/e559842188)] - **(SEMVER-MAJOR)** **stream**: make readable & writable computed (Robert Nagy) [#31197](https://github.com/nodejs/node/pull/31197)
* [[`907c07fa85`](https://github.com/nodejs/node/commit/907c07fa85)] - **(SEMVER-MAJOR)** **stream**: move \_writableState.buffer to EOL (James M Snell) [#31165](https://github.com/nodejs/node/pull/31165)
* [[`66f4e4edcb`](https://github.com/nodejs/node/commit/66f4e4edcb)] - **(SEMVER-MAJOR)** **stream**: do not emit 'end' after 'error' (Robert Nagy) [#31182](https://github.com/nodejs/node/pull/31182)
* [[`75b30c606c`](https://github.com/nodejs/node/commit/75b30c606c)] - **(SEMVER-MAJOR)** **stream**: emit 'error' asynchronously (Robert Nagy) [#29744](https://github.com/nodejs/node/pull/29744)
* [[`4bec6d13f9`](https://github.com/nodejs/node/commit/4bec6d13f9)] - **(SEMVER-MAJOR)** **stream**: enable autoDestroy by default (Robert Nagy) [#30623](https://github.com/nodejs/node/pull/30623)
* [[`20d009d2fd`](https://github.com/nodejs/node/commit/20d009d2fd)] - **(SEMVER-MAJOR)** **stream**: pipe should not swallow error (Robert Nagy) [#30993](https://github.com/nodejs/node/pull/30993)
* [[`67ed526ab0`](https://github.com/nodejs/node/commit/67ed526ab0)] - **(SEMVER-MAJOR)** **stream**: error state cleanup (Robert Nagy) [#30851](https://github.com/nodejs/node/pull/30851)
* [[`e902fadc5e`](https://github.com/nodejs/node/commit/e902fadc5e)] - **(SEMVER-MAJOR)** **stream**: do not throw multiple callback errors in writable (Robert Nagy) [#30614](https://github.com/nodejs/node/pull/30614)
* [[`e13a37e49d`](https://github.com/nodejs/node/commit/e13a37e49d)] - **(SEMVER-MAJOR)** **stream**: ensure finish is emitted in next tick (Robert Nagy) [#30733](https://github.com/nodejs/node/pull/30733)
* [[`9d09969f4c`](https://github.com/nodejs/node/commit/9d09969f4c)] - **(SEMVER-MAJOR)** **stream**: always invoke end callback (Robert Nagy) [#29747](https://github.com/nodejs/node/pull/29747)

* [[`0f78dcc86d`](https://github.com/nodejs/node/commit/0f78dcc86d)] - **(SEMVER-MAJOR)** **util**: escape C1 control characters and switch to hex format (Ruben Bridgewater) [#29826](https://github.com/nodejs/node/pull/29826)
* [[`cb8898c48f`](https://github.com/nodejs/node/commit/cb8898c48f)] - **(SEMVER-MAJOR)** **win**: block running on EOL Windows versions (João Reis) [#31954](https://github.com/nodejs/node/pull/31954)
* [[`a9401439c7`](https://github.com/nodejs/node/commit/a9401439c7)] - **(SEMVER-MAJOR)** **zlib**: align with streams (Robert Nagy) [#32220](https://github.com/nodejs/node/pull/32220)

### Semver-Minor Commits

* [[`63f0dd1ab9`](https://github.com/nodejs/node/commit/63f0dd1ab9)] - **(SEMVER-MINOR)** **async_hooks**: merge run and exit methods (Andrey Pechkurov) [#31950](https://github.com/nodejs/node/pull/31950)
* [[`a683e87cd0`](https://github.com/nodejs/node/commit/a683e87cd0)] - **(SEMVER-MINOR)** **async_hooks**: prevent sync methods of async storage exiting outer context (Stephen Belanger) [#31950](https://github.com/nodejs/node/pull/31950)
* [[`f571b294f5`](https://github.com/nodejs/node/commit/f571b294f5)] - **(SEMVER-MINOR)** **doc**: deprecate process.mainModule (Antoine du HAMEL) [#32232](https://github.com/nodejs/node/pull/32232)
* [[`e04f599258`](https://github.com/nodejs/node/commit/e04f599258)] - **(SEMVER-MINOR)** **doc**: add basic embedding example documentation (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`e93503be83`](https://github.com/nodejs/node/commit/e93503be83)] - **(SEMVER-MINOR)** **embedding**: provide hook for custom process.exit() behaviour (Anna Henningsen) [#32531](https://github.com/nodejs/node/pull/32531)
* [[`a8cf886de7`](https://github.com/nodejs/node/commit/a8cf886de7)] - **(SEMVER-MINOR)** **src**: shutdown platform from FreePlatform() (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`0e576740dc`](https://github.com/nodejs/node/commit/0e576740dc)] - **(SEMVER-MINOR)** **src**: fix what a dispose without checking (Jichan) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`887b6a143b`](https://github.com/nodejs/node/commit/887b6a143b)] - **(SEMVER-MINOR)** **src**: allow non-Node.js TracingControllers (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`7e0264d932`](https://github.com/nodejs/node/commit/7e0264d932)] - **(SEMVER-MINOR)** **src**: add ability to look up platform based on `Environment\*` (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`d7f11077f1`](https://github.com/nodejs/node/commit/d7f11077f1)] - **(SEMVER-MINOR)** **src**: make InitializeNodeWithArgs() official public API (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`821e21de8c`](https://github.com/nodejs/node/commit/821e21de8c)] - **(SEMVER-MINOR)** **src**: add unique\_ptr equivalent of CreatePlatform (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`7dead8440c`](https://github.com/nodejs/node/commit/7dead8440c)] - **(SEMVER-MINOR)** **src**: add LoadEnvironment() variant taking a string (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`c44edec4da`](https://github.com/nodejs/node/commit/c44edec4da)] - **(SEMVER-MINOR)** **src**: provide a variant of LoadEnvironment taking a callback (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`a9fb51f9be`](https://github.com/nodejs/node/commit/a9fb51f9be)] - **(SEMVER-MINOR)** **src**: align worker and main thread code with embedder API (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`084c379648`](https://github.com/nodejs/node/commit/084c379648)] - **(SEMVER-MINOR)** **src**: associate is\_main\_thread() with worker\_context() (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`64c01222d9`](https://github.com/nodejs/node/commit/64c01222d9)] - **(SEMVER-MINOR)** **src**: move worker\_context from Environment to IsolateData (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`288382a4ce`](https://github.com/nodejs/node/commit/288382a4ce)] - **(SEMVER-MINOR)** **src**: fix memory leak in CreateEnvironment when bootstrap fails (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`d7bc5816a5`](https://github.com/nodejs/node/commit/d7bc5816a5)] - **(SEMVER-MINOR)** **src**: make `FreeEnvironment()` perform all necessary cleanup (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`43d32b073f`](https://github.com/nodejs/node/commit/43d32b073f)] - **(SEMVER-MINOR)** **src,test**: add full-featured embedder API test (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`2061c33670`](https://github.com/nodejs/node/commit/2061c33670)] - **(SEMVER-MINOR)** **test**: add extended embedder cctest (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)
* [[`2561484dcb`](https://github.com/nodejs/node/commit/2561484dcb)] - **(SEMVER-MINOR)** **test**: re-enable cctest that was commented out (Anna Henningsen) [#30467](https://github.com/nodejs/node/pull/30467)

### Semver-Patch Commits

* [[`9b6e797379`](https://github.com/nodejs/node/commit/9b6e797379)] - ***Revert*** "**assert**: fix line number calculation after V8 upgrade" (Michaël Zasso) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`c740fbda9d`](https://github.com/nodejs/node/commit/c740fbda9d)] - **buffer**: add type check in bidirectionalIndexOf (Gerhard Stoebich) [#32770](https://github.com/nodejs/node/pull/32770)
* [[`c8e3470e53`](https://github.com/nodejs/node/commit/c8e3470e53)] - **buffer**: mark pool ArrayBuffer as untransferable (Anna Henningsen) [#32759](https://github.com/nodejs/node/pull/32759)
* [[`f2c22db580`](https://github.com/nodejs/node/commit/f2c22db580)] - **build**: remove .git folders when testing V8 (Richard Lau) [#32877](https://github.com/nodejs/node/pull/32877)
* [[`c0f43bfda8`](https://github.com/nodejs/node/commit/c0f43bfda8)] - **build**: add configure flag to build V8 with DCHECKs (Anna Henningsen) [#32787](https://github.com/nodejs/node/pull/32787)
* [[`99e7f878ce`](https://github.com/nodejs/node/commit/99e7f878ce)] - **build**: re-enable ASAN Action using clang (Matheus Marchini) [#32776](https://github.com/nodejs/node/pull/32776)
* [[`3e55284e9b`](https://github.com/nodejs/node/commit/3e55284e9b)] - **build**: use same flags as V8 for ASAN (Matheus Marchini) [#32776](https://github.com/nodejs/node/pull/32776)
* [[`4e5ec41024`](https://github.com/nodejs/node/commit/4e5ec41024)] - **build**: add build from tarball (John Kleinschmidt) [#32129](https://github.com/nodejs/node/pull/32129)
* [[`6a349019da`](https://github.com/nodejs/node/commit/6a349019da)] - **build**: temporarily skip ASAN build (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`da92f15413`](https://github.com/nodejs/node/commit/da92f15413)] - **build**: reset embedder string to "-node.0" (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`e883059c24`](https://github.com/nodejs/node/commit/e883059c24)] - **cli, report**: move --report-on-fatalerror to stable (Colin Ihrig) [#32496](https://github.com/nodejs/node/pull/32496)
* [[`bf86f55e22`](https://github.com/nodejs/node/commit/bf86f55e22)] - **deps**: patch V8 to 8.1.307.30 (Michaël Zasso) [#32693](https://github.com/nodejs/node/pull/32693)
* [[`b5bbde8cf1`](https://github.com/nodejs/node/commit/b5bbde8cf1)] - **deps**: upgrade to libuv 1.37.0 (Colin Ihrig) [#32866](https://github.com/nodejs/node/pull/32866)
* [[`7afe24dba6`](https://github.com/nodejs/node/commit/7afe24dba6)] - **deps**: upgrade to libuv 1.36.0 (Colin Ihrig) [#32866](https://github.com/nodejs/node/pull/32866)
* [[`1cd235d1a0`](https://github.com/nodejs/node/commit/1cd235d1a0)] - **deps**: patch V8 to run on Xcode 8 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`5d867badd0`](https://github.com/nodejs/node/commit/5d867badd0)] - **deps**: V8: silence irrelevant warnings (Michaël Zasso) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`8d2c441e4d`](https://github.com/nodejs/node/commit/8d2c441e4d)] - **deps**: V8: cherry-pick 931bdbd76f5b (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`049160dfb6`](https://github.com/nodejs/node/commit/049160dfb6)] - **deps**: V8: cherry-pick 1e36e21acc40 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`0220c298c5`](https://github.com/nodejs/node/commit/0220c298c5)] - **deps**: bump minimum icu version to 65 (Michaël Zasso) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`f90eba1d91`](https://github.com/nodejs/node/commit/f90eba1d91)] - **deps**: make v8.h compatible with VS2015 (Joao Reis) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`56b6a4f732`](https://github.com/nodejs/node/commit/56b6a4f732)] - **deps**: V8: forward declaration of `Rtl\*FunctionTable` (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`40c9419b35`](https://github.com/nodejs/node/commit/40c9419b35)] - **deps**: V8: patch register-arm64.h (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`55407ab73e`](https://github.com/nodejs/node/commit/55407ab73e)] - **deps**: patch V8 to run on older XCode versions (Ujjwal Sharma) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`990bc9adb4`](https://github.com/nodejs/node/commit/990bc9adb4)] - **deps**: V8: un-cherry-pick bd019bd (Refael Ackermann) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`17a6def4e8`](https://github.com/nodejs/node/commit/17a6def4e8)] - **deps**: update V8 dtrace & postmortem metadata (Colin Ihrig) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`0f14123186`](https://github.com/nodejs/node/commit/0f14123186)] - **deps**: V8: stub backport fast API call changes (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`bf412ed77b`](https://github.com/nodejs/node/commit/bf412ed77b)] - **deps**: V8: stub backport d5b444bc5a84 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`fdaa365b0b`](https://github.com/nodejs/node/commit/fdaa365b0b)] - **deps**: V8: stub backport 65238018ca4b and 8d08318e1a85 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`8198e7882c`](https://github.com/nodejs/node/commit/8198e7882c)] - **deps**: V8: stub backport 9e52d5c5d717 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`a27852ae7c`](https://github.com/nodejs/node/commit/a27852ae7c)] - **deps**: V8: cherry-pick 98b1ef80c722 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`e8c7b7a2df`](https://github.com/nodejs/node/commit/e8c7b7a2df)] - **deps**: V8: cherry-pick b5c917ee80cb (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`552cee0cc0`](https://github.com/nodejs/node/commit/552cee0cc0)] - **deps**: V8: cherry-pick 700b1b97e9ab (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`9b7a1b048a`](https://github.com/nodejs/node/commit/9b7a1b048a)] - **deps**: V8: cherry-pick e8ba5699c648 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`1f02617b05`](https://github.com/nodejs/node/commit/1f02617b05)] - **deps**: V8: cherry-pick 55a01ec7519a (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`da728c482c`](https://github.com/nodejs/node/commit/da728c482c)] - **deps**: V8: cherry-pick 9f0f2cb7f08d (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`2ee8b4a512`](https://github.com/nodejs/node/commit/2ee8b4a512)] - **deps**: V8: cherry-pick e395d1698453 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`dfc66a6af4`](https://github.com/nodejs/node/commit/dfc66a6af4)] - **deps**: V8: cherry-pick d1253ae95b09 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`c3ecbc758b`](https://github.com/nodejs/node/commit/c3ecbc758b)] - **deps**: V8: cherry-pick fa3e37e511ee (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`9568fbc7cd`](https://github.com/nodejs/node/commit/9568fbc7cd)] - **deps**: V8: cherry-pick f0057afc2fb6 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`07d4372d5a`](https://github.com/nodejs/node/commit/07d4372d5a)] - **deps**: V8: cherry-pick 94723c197199 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`4a11a54f9a`](https://github.com/nodejs/node/commit/4a11a54f9a)] - **deps**: V8: backport 844fe8f7d965 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`1b7878558a`](https://github.com/nodejs/node/commit/1b7878558a)] - **deps**: V8: cherry-pick 2db93c023379 (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`122937fc67`](https://github.com/nodejs/node/commit/122937fc67)] - **deps**: V8: cherry-pick 4b1447e4bb0e (Anna Henningsen) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`01573ba4ae`](https://github.com/nodejs/node/commit/01573ba4ae)] - **deps**: remove duplicated postmortem metadata entry (Matheus Marchini) [#32521](https://github.com/nodejs/node/pull/32521)
* [[`9290febefa`](https://github.com/nodejs/node/commit/9290febefa)] - **deps**: patch V8 to 8.1.307.26 (Matheus Marchini) [#32521](https://github.com/nodejs/node/pull/32521)
* [[`a9e4cec70d`](https://github.com/nodejs/node/commit/a9e4cec70d)] - ***Revert*** "**deps**: V8: cherry-pick f9257802c1c0" (Matheus Marchini) [#32521](https://github.com/nodejs/node/pull/32521)
* [[`77542a5d57`](https://github.com/nodejs/node/commit/77542a5d57)] - **deps**: revert whitespace changes on V8 (Matheus Marchini) [#32587](https://github.com/nodejs/node/pull/32587)
* [[`9add24ecd3`](https://github.com/nodejs/node/commit/9add24ecd3)] - **doc**: missing brackets (William Bonawentura) [#32657](https://github.com/nodejs/node/pull/32657)
* [[`1796cc0df5`](https://github.com/nodejs/node/commit/1796cc0df5)] - **doc**: improve consistency in usage of NULL (Michael Dawson) [#32726](https://github.com/nodejs/node/pull/32726)
* [[`2662b0c9e3`](https://github.com/nodejs/node/commit/2662b0c9e3)] - **doc**: improve net docs (Robert Nagy) [#32811](https://github.com/nodejs/node/pull/32811)
* [[`5d940de17b`](https://github.com/nodejs/node/commit/5d940de17b)] - **doc**: note that signatures of binary may be from subkeys (Reşat SABIQ) [#32591](https://github.com/nodejs/node/pull/32591)
* [[`3c8dd6d0c3`](https://github.com/nodejs/node/commit/3c8dd6d0c3)] - **doc**: add transform stream destroy() return value (Colin Ihrig) [#32788](https://github.com/nodejs/node/pull/32788)
* [[`39368b34eb`](https://github.com/nodejs/node/commit/39368b34eb)] - **doc**: updated guidance for n-api changes (Michael Dawson) [#32721](https://github.com/nodejs/node/pull/32721)
* [[`cba6e5dc09`](https://github.com/nodejs/node/commit/cba6e5dc09)] - **doc**: remove warning from `response.writeHead` (Cecchi MacNaughton) [#32700](https://github.com/nodejs/node/pull/32700)
* [[`8f7fd8d6aa`](https://github.com/nodejs/node/commit/8f7fd8d6aa)] - **doc**: improve AsyncLocalStorage sample (Andrey Pechkurov) [#32757](https://github.com/nodejs/node/pull/32757)
* [[`a7c75f956f`](https://github.com/nodejs/node/commit/a7c75f956f)] - **doc**: document `buffer.from` returns internal pool buffer (Harshitha KP) [#32703](https://github.com/nodejs/node/pull/32703)
* [[`f6a91156c7`](https://github.com/nodejs/node/commit/f6a91156c7)] - **doc**: add puzpuzpuz to collaborators (Andrey Pechkurov) [#32817](https://github.com/nodejs/node/pull/32817)
* [[`1db8da21f2`](https://github.com/nodejs/node/commit/1db8da21f2)] - **doc**: split process.umask() entry into two (Rich Trott) [#32711](https://github.com/nodejs/node/pull/32711)
* [[`6ade42bb3c`](https://github.com/nodejs/node/commit/6ade42bb3c)] - **doc**: stream.end(cb) cb can be invoked with error (Pranshu Srivastava) [#32238](https://github.com/nodejs/node/pull/32238)
* [[`edb3ffb003`](https://github.com/nodejs/node/commit/edb3ffb003)] - **doc**: fix os.version() Windows API (Colin Ihrig) [#32156](https://github.com/nodejs/node/pull/32156)
* [[`a777cfa843`](https://github.com/nodejs/node/commit/a777cfa843)] - **doc**: remove repetition (Luigi Pinca) [#31868](https://github.com/nodejs/node/pull/31868)
* [[`7c524fb092`](https://github.com/nodejs/node/commit/7c524fb092)] - **doc**: fix Writable.write callback description (Robert Nagy) [#31812](https://github.com/nodejs/node/pull/31812)
* [[`43fb664701`](https://github.com/nodejs/node/commit/43fb664701)] - **doc**: fix missing changelog corrections (Myles Borins) [#31854](https://github.com/nodejs/node/pull/31854)
* [[`a2d6f98e1a`](https://github.com/nodejs/node/commit/a2d6f98e1a)] - **doc**: fix typo (Colin Ihrig) [#31675](https://github.com/nodejs/node/pull/31675)
* [[`17e3f3be76`](https://github.com/nodejs/node/commit/17e3f3be76)] - **doc**: update pr-url for DEP0022 EOL (Colin Ihrig) [#31675](https://github.com/nodejs/node/pull/31675)
* [[`cd0f5a239e`](https://github.com/nodejs/node/commit/cd0f5a239e)] - **doc**: update pr-url for DEP0016 EOL (Colin Ihrig) [#31675](https://github.com/nodejs/node/pull/31675)
* [[`5170daaca5`](https://github.com/nodejs/node/commit/5170daaca5)] - **doc**: fix changelog for v10.18.1 (Andrew Hughes) [#31358](https://github.com/nodejs/node/pull/31358)
* [[`d845915d46`](https://github.com/nodejs/node/commit/d845915d46)] - **doc**: mark Node.js 8 End-of-Life in CHANGELOG (Beth Griggs) [#31152](https://github.com/nodejs/node/pull/31152)
* [[`009a9c475b`](https://github.com/nodejs/node/commit/009a9c475b)] - **doc,src,test**: assign missing deprecation code (Colin Ihrig) [#31674](https://github.com/nodejs/node/pull/31674)
* [[`ed4fbefb71`](https://github.com/nodejs/node/commit/ed4fbefb71)] - **fs**: use finished over destroy w/ cb (Robert Nagy) [#32809](https://github.com/nodejs/node/pull/32809)
* [[`3e9302b2b3`](https://github.com/nodejs/node/commit/3e9302b2b3)] - **fs**: validate the input data before opening file (Yongsheng Zhang) [#31731](https://github.com/nodejs/node/pull/31731)
* [[`1a3e358a1d`](https://github.com/nodejs/node/commit/1a3e358a1d)] - **http**: refactor agent 'free' handler (Robert Nagy) [#32801](https://github.com/nodejs/node/pull/32801)
* [[`399749e4d8`](https://github.com/nodejs/node/commit/399749e4d8)] - **lib**: created isValidCallback helper (Yash Ladha) [#32665](https://github.com/nodejs/node/pull/32665)
* [[`bc55b57e64`](https://github.com/nodejs/node/commit/bc55b57e64)] - **lib**: fix few comment typos in fs/watchers.js (Denys Otrishko) [#31705](https://github.com/nodejs/node/pull/31705)
* [[`f98668ade3`](https://github.com/nodejs/node/commit/f98668ade3)] - **module**: remove experimental modules warning (Guy Bedford) [#31974](https://github.com/nodejs/node/pull/31974)
* [[`fe1bda9aeb`](https://github.com/nodejs/node/commit/fe1bda9aeb)] - **module**: fix memory leak when require error occurs (Qinhui Chen) [#32837](https://github.com/nodejs/node/pull/32837)
* [[`076ba3150d`](https://github.com/nodejs/node/commit/076ba3150d)] - ***Revert*** "**n-api**: detect deadlocks in thread-safe function" (Gabriel Schulhof) [#32880](https://github.com/nodejs/node/pull/32880)
* [[`1092bb94f4`](https://github.com/nodejs/node/commit/1092bb94f4)] - **process**: suggest --trace-warnings when printing warning (Anna Henningsen) [#32797](https://github.com/nodejs/node/pull/32797)
* [[`d19a2c33b3`](https://github.com/nodejs/node/commit/d19a2c33b3)] - **src**: migrate measureMemory to new v8 api (gengjiawen) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`a63db7fb5e`](https://github.com/nodejs/node/commit/a63db7fb5e)] - **src**: remove deprecated wasm type check (Clemens Backes) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`c080b2d821`](https://github.com/nodejs/node/commit/c080b2d821)] - **src**: avoid calling deprecated method (Clemens Backes) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`7ed0d1439e`](https://github.com/nodejs/node/commit/7ed0d1439e)] - **src**: remove use of deprecated Symbol::Name() (Colin Ihrig) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`59eeb3b5b9`](https://github.com/nodejs/node/commit/59eeb3b5b9)] - **src**: stop overriding deprecated V8 methods (Clemens Backes) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`339c192ddb`](https://github.com/nodejs/node/commit/339c192ddb)] - **src**: update NODE\_MODULE\_VERSION to 83 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`6681a685a9`](https://github.com/nodejs/node/commit/6681a685a9)] - **src**: remove unused using in node\_worker.cc (Daniel Bevenius) [#32840](https://github.com/nodejs/node/pull/32840)
* [[`b9d9f91a80`](https://github.com/nodejs/node/commit/b9d9f91a80)] - **src**: use basename(argv0) for --trace-uncaught suggestion (Anna Henningsen) [#32798](https://github.com/nodejs/node/pull/32798)
* [[`24e1e28b38`](https://github.com/nodejs/node/commit/24e1e28b38)] - **src**: ignore GCC -Wcast-function-type for v8.h (Daniel Bevenius) [#32679](https://github.com/nodejs/node/pull/32679)
* [[`a946189ccd`](https://github.com/nodejs/node/commit/a946189ccd)] - **src**: add AliasedStruct utility (James M Snell) [#32778](https://github.com/nodejs/node/pull/32778)
* [[`457f1f1ed0`](https://github.com/nodejs/node/commit/457f1f1ed0)] - **src**: remove unused v8 Array namespace (Juan José Arboleda) [#32749](https://github.com/nodejs/node/pull/32749)
* [[`b68e26ee70`](https://github.com/nodejs/node/commit/b68e26ee70)] - **src**: flush V8 interrupts from Environment dtor (Anna Henningsen) [#32523](https://github.com/nodejs/node/pull/32523)
* [[`96bf137cca`](https://github.com/nodejs/node/commit/96bf137cca)] - **src**: use env-\>RequestInterrupt() for inspector MainThreadInterface (Anna Henningsen) [#32523](https://github.com/nodejs/node/pull/32523)
* [[`72da426780`](https://github.com/nodejs/node/commit/72da426780)] - **src**: use env-\>RequestInterrupt() for inspector io thread start (Anna Henningsen) [#32523](https://github.com/nodejs/node/pull/32523)
* [[`99c9b2368c`](https://github.com/nodejs/node/commit/99c9b2368c)] - **src**: fix cleanup hook removal for InspectorTimer (Anna Henningsen) [#32523](https://github.com/nodejs/node/pull/32523)
* [[`6dffd6b3de`](https://github.com/nodejs/node/commit/6dffd6b3de)] - **src**: make `Environment::interrupt\_data\_` atomic (Anna Henningsen) [#32523](https://github.com/nodejs/node/pull/32523)
* [[`8c5ad1392f`](https://github.com/nodejs/node/commit/8c5ad1392f)] - **src**: initialize inspector before RunBootstrapping() (Anna Henningsen) [#32672](https://github.com/nodejs/node/pull/32672)
* [[`eafd64b1c8`](https://github.com/nodejs/node/commit/eafd64b1c8)] - **src**: consistently declare BindingData class (Sam Roberts) [#32677](https://github.com/nodejs/node/pull/32677)
* [[`78c82a38ac`](https://github.com/nodejs/node/commit/78c82a38ac)] - **src**: move fs state out of Environment (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`7005670f34`](https://github.com/nodejs/node/commit/7005670f34)] - **src**: move http parser state out of Environment (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`19b671506c`](https://github.com/nodejs/node/commit/19b671506c)] - **src**: move v8 stats buffers out of Environment (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`4df24f040d`](https://github.com/nodejs/node/commit/4df24f040d)] - **src**: move HTTP/2 state out of Environment (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`1fc3de908e`](https://github.com/nodejs/node/commit/1fc3de908e)] - **src**: make creating per-binding data structures easier (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`0e9f9b7592`](https://github.com/nodejs/node/commit/0e9f9b7592)] - **src**: include AsyncWrap provider strings in snapshot (Anna Henningsen) [#32572](https://github.com/nodejs/node/pull/32572)
* [[`effebf87ab`](https://github.com/nodejs/node/commit/effebf87ab)] - **src**: remove unused v8 namespace (Juan José Arboleda) [#32375](https://github.com/nodejs/node/pull/32375)
* [[`d23eed256b`](https://github.com/nodejs/node/commit/d23eed256b)] - **src**: remove calls to deprecated ArrayBuffer methods (Michaël Zasso) [#32358](https://github.com/nodejs/node/pull/32358)
* [[`f3682102dc`](https://github.com/nodejs/node/commit/f3682102dc)] - **src**: give Http2Session JS fields their own backing store (Anna Henningsen) [#31648](https://github.com/nodejs/node/pull/31648)
* [[`90f7a5c010`](https://github.com/nodejs/node/commit/90f7a5c010)] - **src**: set arraybuffer\_untransferable\_private\_symbol (Thang Tran) [#31053](https://github.com/nodejs/node/pull/31053)
* [[`d06efafe6b`](https://github.com/nodejs/node/commit/d06efafe6b)] - **src**: explicitly allocate backing stores for v8 stat buffers (Anna Henningsen) [#30946](https://github.com/nodejs/node/pull/30946)
* [[`917fedd21a`](https://github.com/nodejs/node/commit/917fedd21a)] - **src**: unset NODE\_VERSION\_IS\_RELEASE from master (Michaël Zasso) [#30584](https://github.com/nodejs/node/pull/30584)
* [[`69f19f4ccd`](https://github.com/nodejs/node/commit/69f19f4ccd)] - **src**: remove uses of deprecated wasm TransferrableModule (Clemens Backes) [#30026](https://github.com/nodejs/node/pull/30026)
* [[`acac5df260`](https://github.com/nodejs/node/commit/acac5df260)] - **src,doc**: add documentation for per-binding state pattern (Anna Henningsen) [#32538](https://github.com/nodejs/node/pull/32538)
* [[`ad4c10e824`](https://github.com/nodejs/node/commit/ad4c10e824)] - **stream**: improve comments regarding end() errors (Robert Nagy) [#32839](https://github.com/nodejs/node/pull/32839)
* [[`6e5c23b6c8`](https://github.com/nodejs/node/commit/6e5c23b6c8)] - **stream**: update comment to indicate unused API (Robert Nagy) [#32808](https://github.com/nodejs/node/pull/32808)
* [[`21bd6679ce`](https://github.com/nodejs/node/commit/21bd6679ce)] - **stream**: fix finished typo (Robert Nagy) [#31881](https://github.com/nodejs/node/pull/31881)
* [[`85c6fcd1cd`](https://github.com/nodejs/node/commit/85c6fcd1cd)] - **stream**: avoid writing to writable (Robert Nagy) [#31805](https://github.com/nodejs/node/pull/31805)
* [[`0875837417`](https://github.com/nodejs/node/commit/0875837417)] - **stream**: fix async iterator destroyed error order (Robert Nagy) [#31700](https://github.com/nodejs/node/pull/31700)
* [[`b9a7625fdf`](https://github.com/nodejs/node/commit/b9a7625fdf)] - **stream**: removed outdated TODO (Robert Nagy) [#31701](https://github.com/nodejs/node/pull/31701)
* [[`68e1288e00`](https://github.com/nodejs/node/commit/68e1288e00)] - **test**: mark addons/zlib-bindings/test flaky on arm (Michaël Zasso) [#32885](https://github.com/nodejs/node/pull/32885)
* [[`a09bf3ad5f`](https://github.com/nodejs/node/commit/a09bf3ad5f)] - **test**: replace console.log/error() with debuglog (daemon1024) [#32692](https://github.com/nodejs/node/pull/32692)
* [[`d1b41bbd86`](https://github.com/nodejs/node/commit/d1b41bbd86)] - **test**: only detect uname on supported os (Xu Meng) [#32833](https://github.com/nodejs/node/pull/32833)
* [[`4bb29ed044`](https://github.com/nodejs/node/commit/4bb29ed044)] - **test**: mark cpu-prof-dir-worker flaky on all (Sam Roberts) [#32828](https://github.com/nodejs/node/pull/32828)
* [[`e18a40e42d`](https://github.com/nodejs/node/commit/e18a40e42d)] - **test**: replace equal with strictEqual (Jesus Hernandez) [#32727](https://github.com/nodejs/node/pull/32727)
* [[`320f297a35`](https://github.com/nodejs/node/commit/320f297a35)] - **test**: mark test-worker-prof flaky on arm (Sam Roberts) [#32826](https://github.com/nodejs/node/pull/32826)
* [[`4b5658b536`](https://github.com/nodejs/node/commit/4b5658b536)] - **test**: mark test-http2-reset-flood flaky on all (Sam Roberts) [#32825](https://github.com/nodejs/node/pull/32825)
* [[`ead51be541`](https://github.com/nodejs/node/commit/ead51be541)] - **test**: cover node entry type in perf\_hooks (Julian Duque) [#32751](https://github.com/nodejs/node/pull/32751)
* [[`9e5189a560`](https://github.com/nodejs/node/commit/9e5189a560)] - **test**: use symlinks to copy shells (John Kleinschmidt) [#32129](https://github.com/nodejs/node/pull/32129)
* [[`c5763e8dc1`](https://github.com/nodejs/node/commit/c5763e8dc1)] - **test**: wait for message from parent in embedding cctest (Anna Henningsen) [#32563](https://github.com/nodejs/node/pull/32563)
* [[`c3204a8787`](https://github.com/nodejs/node/commit/c3204a8787)] - **test**: use common.buildType in embedding test (Anna Henningsen) [#32422](https://github.com/nodejs/node/pull/32422)
* [[`f2cc28aec3`](https://github.com/nodejs/node/commit/f2cc28aec3)] - **test**: use InitializeNodeWithArgs in cctest (Anna Henningsen) [#32406](https://github.com/nodejs/node/pull/32406)
* [[`df1592d2e9`](https://github.com/nodejs/node/commit/df1592d2e9)] - **test**: async iterate destroyed stream (Robert Nagy) [#28995](https://github.com/nodejs/node/pull/28995)
* [[`5100e84f4b`](https://github.com/nodejs/node/commit/5100e84f4b)] - **test**: fix flaky test-fs-promises-file-handle-close (Anna Henningsen) [#31687](https://github.com/nodejs/node/pull/31687)
* [[`52944b834a`](https://github.com/nodejs/node/commit/52944b834a)] - **test**: remove test (Clemens Backes) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`119fdf6813`](https://github.com/nodejs/node/commit/119fdf6813)] - **test**: remove checks for deserializing wasm (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`add5f6e5cd`](https://github.com/nodejs/node/commit/add5f6e5cd)] - **tls**: provide default cipher list from command line (Anna Henningsen) [#32760](https://github.com/nodejs/node/pull/32760)
* [[`405ae1909b`](https://github.com/nodejs/node/commit/405ae1909b)] - **tools**: update V8 gypfiles for 8.1 (Matheus Marchini) [#32116](https://github.com/nodejs/node/pull/32116)
* [[`7fe61222ef`](https://github.com/nodejs/node/commit/7fe61222ef)] - **worker**: mention argument name in type check message (Anna Henningsen) [#32815](https://github.com/nodejs/node/pull/32815)
* [[`7147df53e8`](https://github.com/nodejs/node/commit/7147df53e8)] - **worker**: fix type check in receiveMessageOnPort (Anna Henningsen) [#32745](https://github.com/nodejs/node/pull/32745)
* [[`0c545f0f72`](https://github.com/nodejs/node/commit/0c545f0f72)] - **zlib**: emits 'close' event after readable 'end' (Sergey Zelenov) [#32050](https://github.com/nodejs/node/pull/32050)
