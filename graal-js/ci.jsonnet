local common = import '../common.jsonnet';
local ci = import '../ci.jsonnet';

{
  local graalJs = ci.jobtemplate + {
    cd:: 'graal-js',
    suite_prefix:: 'js', # for build job names
    // increase default timelimit on windows and darwin-amd64
    timelimit: if 'os' in self && (self.os == 'windows' || (self.os == 'darwin' && self.arch == 'amd64')) then '1:30:00' else '45:00',
  },

  local compiler = {suiteimports+:: ['compiler']},
  local ce = ci.ce + compiler,
  local ee = ci.ee + compiler,

  local gateTags(tags) = common.build + common.gateTags + {
    environment+: {
      TAGS: tags,
    },
  },

  local gateCoverage = {
    coverage_gate_args:: ['--jacoco-omit-excluded', '--jacoco-relativize-paths', '--jacoco-omit-src-gen', '--jacoco-format', 'lcov', '--jacocout', 'coverage'],
    run+: [
      ['mx', '--dynamicimports', '/wasm', 'gate', '-B=--force-deprecation-as-warning', '-B=-A-J-Dtruffle.dsl.SuppressWarnings=truffle', '--strict-mode', '--tags', 'build,coverage'] + self.coverage_gate_args,
    ],
    teardown+: [
      ['mx', 'sversions', '--print-repositories', '--json', '|', 'coverage-uploader.py', '--associated-repos', '-'],
    ],
    timelimit: '1:00:00',
  },

  local checkoutJsBenchmarks = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/js-benchmarks.git'], '../js-benchmarks'],
    ],
  },

  local nativeImageSmokeTest = checkoutJsBenchmarks + {
    suiteimports+:: ['vm', 'substratevm', 'wasm'],
    nativeimages+:: ['lib:jsvm', 'lib:jvmcicompiler'],
    extraimagebuilderarguments+:: ['-H:+ReportExceptionStackTraces'],
    run+: [
      ['mx', 'build', '--dependencies', 'ALL_GRAALVM_ARTIFACTS'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--quiet', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/js', '--native', '-e', "print('hello:' + Array.from(new Array(10), (x,i) => i*i ).join('|'))"],
      ['${GRAALVM_HOME}/bin/js', '--native', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
      # standalone smoke tests
      ['set-export', 'STANDALONE_HOME', ['mx', '--quiet', 'standalone-home', 'js', '--type=native']],
      ['${STANDALONE_HOME}/bin/js', '--native', '-e', "print('hello:' + Array.from(new Array(10), (x,i) => i*i ).join('|'))"],
      ['${STANDALONE_HOME}/bin/js', '--native', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
      ['${STANDALONE_HOME}/bin/js', '--experimental-options', '--js.webassembly', '-e', 'new WebAssembly.Module(new Uint8Array([0x00,0x61,0x73,0x6d,0x01,0x00,0x00,0x00]))'],
      ['set-export', 'STANDALONE_HOME', ['mx', '--quiet', 'standalone-home', 'js', '--type=jvm']],
      ['${STANDALONE_HOME}/bin/js', '--jvm', '-e', "print('hello:' + Array.from(new Array(10), (x,i) => i*i ).join('|'))"],
      ['${STANDALONE_HOME}/bin/js', '--jvm', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
      ['${STANDALONE_HOME}/bin/js', '--experimental-options', '--js.webassembly', '-e', 'new WebAssembly.Module(new Uint8Array([0x00,0x61,0x73,0x6d,0x01,0x00,0x00,0x00]))'],
      # maven-downloader smoke test
      ['VERBOSE_GRAALVM_LAUNCHERS=true', '${STANDALONE_HOME}/bin/js-polyglot-get', '-o', 'maven downloader output', '-a', 'wasm', '-v', '23.1.3'],
    ],
    timelimit: '30:00',
  },

  local mavenDeployDryRun = {
    run+: [
      ['mx', 'build'],
      ['mx', '-v', 'maven-deploy', '--suppress-javadoc', '--validate', 'full', '--licenses', 'UPL,MIT', '--dry-run', 'ossrh', 'https://this-is-only-a-test'],
      ['mx', '-p', '../../graal/vm', '--dynamicimports', '/tools,/compiler,/graal-js,/wasm', 'build'],
      ['mx', '-p', '../../graal/vm', '--dynamicimports', '/tools,/regex,/compiler,/truffle,/sdk,/graal-js,/wasm', 'maven-deploy', '--suppress-javadoc', '--all-suites', '--version-string', 'GATE'],
      ['cd', 'test/maven-demo'],
      ['mvn', '-Dgraalvm.version=GATE', '--batch-mode', 'package'],
      ['mvn', '-Dgraalvm.version=GATE', '--batch-mode', 'exec:exec'],
      ['mvn', '-Dgraalvm.version=GATE', '--batch-mode', 'jlink:jlink'],
      ['target/maven-jlink/default/bin/myapp'],
    ],
    timelimit: '30:00',
  },

  local webassemblyTest = {
    run+: [
      ['mx', '--dynamicimports', '/wasm', 'build'],
      ['mx', '--dynamicimports', '/wasm', 'testv8', 'gate', 'polyglot'],
      ['mx', '--dynamicimports', '/wasm', 'gate', '--tags', 'webassembly'],
    ],
    timelimit: '45:00',
  },

  local downstreamGraal = {
    run+: [
      ['cd', '../../graal/vm'],
      ['set-export', 'NATIVE_IMAGES', 'native-image'],
      ['mx', '--dynamicimports', '/graal-js,/substratevm', 'gate', '--no-warning-as-error', '--strict-mode', '--tags', 'build,truffle-native-tck-js'],
    ],
  },

  local downstreamSubstratevmEE = checkoutJsBenchmarks + ee + {
    suiteimports+:: ['substratevm'],
    run+: [
      ['mx', 'gate', '--all-suites', '--no-warning-as-error', '--strict-mode', '--tags', 'build,${TAGS}'],
    ],
  },

  local interopJmhBenchmarks = common.buildCompiler + {
    run+: [
        ['mx', '--dynamicimports', '/compiler', '--kill-with-sigquit', 'benchmark', '--results-file', 'bench-results.json', 'js-interop-jmh:JS_INTEROP_MICRO_BENCHMARKS', '--', '-Dpolyglot.engine.TraceCompilation=true'],
        ['bench-uploader.py', 'bench-results.json'],
    ],
  },

  local auxEngineCache = {
    suiteimports+:: ['vm', 'substratevm', 'tools'],
    nativeimages+:: ['lib:jsvm'],
    graalvmtests:: '../../graalvm-tests',
    run+: [
      ['mx', 'build', '--dependencies', 'ALL_GRAALVM_ARTIFACTS'],
      ['python', self.graalvmtests + '/test.py', '-g', ['mx', '--quiet', 'standalone-home', 'js'], '--print-revisions', '--keep-on-error', 'test/aux-engine-cache', 'test/repl'],
    ],
    timelimit: '1:00:00',
  },

  local generateBuilds = ci.generateBuilds,
  local promoteToTarget = ci.promoteToTarget,
  local defaultToTarget = ci.defaultToTarget,
  local includePlatforms = ci.includePlatforms,
  local excludePlatforms = ci.excludePlatforms,
  local gateOnMain = ci.gateOnMain,

  // Style gates
  local styleBuilds = generateBuilds([
    graalJs + common.gateStyleFullBuild                                                                   + {name: 'style-fullbuild'}
  ], platforms=ci.styleGatePlatforms, defaultTarget=common.gate),

  // Builds that should run on all supported platforms
  local testingBuilds = generateBuilds([
    graalJs + gateTags('default')                                                                    + ce + {name: 'default-ce'} +
      promoteToTarget(common.gate, [common.jdklatest + common.linux_amd64, common.jdklatest + common.linux_aarch64, common.jdklatest + common.windows_amd64]),
    graalJs + gateTags('default')                                                                    + ee + {name: 'default-ee'} +
      promoteToTarget(common.gate, [common.jdklatest + common.linux_amd64, common.jdklatest + common.darwin_aarch64]) +
      promoteToTarget(common.postMerge, [common.jdklatest + common.darwin_amd64]),

    graalJs + gateTags('noic')                                                                            + {name: 'noic'} + gateOnMain,
    graalJs + gateTags('directbytebuffer')                                                                + {name: 'directbytebuffer'} + gateOnMain,
    graalJs + gateTags('cloneuninitialized')                                                              + {name: 'cloneuninitialized'} + gateOnMain,
    graalJs + gateTags('lazytranslation')                                                                 + {name: 'lazytranslation'} + gateOnMain,
    graalJs + gateTags('shareengine')                                                                     + {name: 'shareengine'} + gateOnMain,
    graalJs + gateTags('latestversion')                                                                   + {name: 'latestversion'} + gateOnMain,
    graalJs + gateTags('instrument')                                                                      + {name: 'instrument'} + gateOnMain,
    graalJs + gateTags('tck')                                                                             + {name: 'tck'} + gateOnMain +
      excludePlatforms([common.darwin_amd64]), # Timeout/OOME
    graalJs + webassemblyTest                                                                             + {name: 'webassembly'} + gateOnMain,
    graalJs + nativeImageSmokeTest                                                                        + {name: 'native-image-smoke-test'} + gateOnMain,
    graalJs + auxEngineCache                                                                         + ee + {name: 'aux-engine-cache'} + gateOnMain +
      excludePlatforms([common.windows_amd64, common.darwin_amd64]), # unsupported on windows, too slow on darwin-amd64

    // downstream graal gate
    graalJs + downstreamGraal                                                                             + {name: 'downstream-graal'} +
      promoteToTarget(common.gate, [common.jdk21 + common.linux_amd64]),
    graalJs + downstreamSubstratevmEE   + {environment+: {TAGS: 'downtest_js'}}                           + {name: 'downstream-substratevm-enterprise'} + gateOnMain +
      excludePlatforms([common.darwin_amd64]) + # Too slow
      excludePlatforms([common.linux_aarch64]), # Fails on Linux AArch64 with "Creation of the VM failed."

    // js.zone-rules-based-time-zones
    graalJs + gateTags('zonerulesbasedtimezones')                                                         + {name: 'zonerulesbasedtimezones'} +
      promoteToTarget(common.postMerge, [ci.mainGatePlatform]),

    // PGO profiles
    graalJs + downstreamSubstratevmEE   + {environment+: {TAGS: 'pgo_collect_js'}}                        + {name: 'pgo-profiles', timelimit: '1:00:00'} +
      promoteToTarget(common.postMerge, [ci.mainGatePlatform]) +
      excludePlatforms([common.darwin_amd64]),   # Too slow
  ], defaultTarget=common.weekly),

  // Builds that only need to run on one platform
  local otherBuilds = generateBuilds([
    graalJs + common.gate      + mavenDeployDryRun                                                        + {name: 'maven-dry-run'},
    # Note: weekly coverage is sync'ed with the graal repo (while ondemand is not).
    graalJs + common.weekly    + gateCoverage                                                             + {name: 'coverage'},
    graalJs + common.ondemand  + gateCoverage                                                             + {name: 'coverage'},
  ], platforms=[common.jdk21 + common.linux_amd64]),

  // Benchmark builds; need to run on a benchmark machine
  local benchBuilds = generateBuilds([
    graalJs + common.dailyBench+ interopJmhBenchmarks                                                     + {name: 'interop-jmh'},
  ], platforms=[common.jdk21 + common.e3]),

  builds: styleBuilds + testingBuilds + otherBuilds + benchBuilds,
}
