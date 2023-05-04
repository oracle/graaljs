local common = import '../common.jsonnet';
local ci = import '../ci.jsonnet';

{
  local graalJs = ci.jobtemplate + {
    cd:: 'graal-js',
    // increase default timelimit on windows and darwin-amd64
    timelimit: if 'os' in self && (self.os == 'windows' || (self.os == 'darwin' && self.arch == 'amd64')) then '1:15:00' else '45:00',
  },

  local compiler = {suiteimports+:: ['compiler']},
  local ce = ci.ce + compiler,
  local ee = ci.ee + compiler,

  local gateTags(tags) = common.gateTags + {
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
    suiteimports+:: ['substratevm'],
    nativeimages+:: ['lib:jsvm'],
    extraimagebuilderarguments+:: ['-H:+TruffleCheckBlockListMethods', '-H:+ReportExceptionStackTraces'],
    run+: [
      ['mx', 'build'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--quiet', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/js', '--native', '-e', "print('hello:' + Array.from(new Array(10), (x,i) => i*i ).join('|'))"],
      ['${GRAALVM_HOME}/bin/js', '--native', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
    ],
    timelimit: '30:00',
  },

  local mavenDeployDryRun = {
    run+: [
      ['mx', 'build'],
      ['mx', '-v', 'maven-deploy', '--suppress-javadoc', '--validate', 'full', '--licenses', 'UPL,MIT', '--dry-run', 'ossrh', 'https://this-is-only-a-test'],
      ['mx', '--dynamicimports', '/tools,/compiler', 'build'],
      ['mx', '--dynamicimports', '/tools,/regex,/compiler,/truffle,/sdk', 'maven-deploy', '--suppress-javadoc', '--all-suites', '--all-distribution-types', '--version-string', 'GATE'],
      ['cd', 'test/maven-demo'],
      ['mvn', '-Dgraalvm.version=GATE', 'package'],
      ['mvn', '-Dgraalvm.version=GATE', 'exec:exec'],
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
      ['mx', '--dynamicimports', '/graal-js,/substratevm', 'gate', '--no-warning-as-error', '--strict-mode', '--tags', 'build,svm-truffle-tck-js'],
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
      ['mx', 'build'],
      ['python', '../../graalvm-tests/test.py', '-g', ['mx', '--quiet', 'graalvm-home'], '--print-revisions', '--keep-on-error', 'test/aux-engine-cache'],
    ],
    timelimit: '1:00:00',
  },

  local generateBuilds = ci.generateBuilds,
  local promoteToTarget = ci.promoteToTarget,
  local defaultToTarget = ci.defaultToTarget,
  local includePlatforms = ci.includePlatforms,
  local excludePlatforms = ci.excludePlatforms,
  local gateOnMain = ci.gateOnMain,

  // Builds that should run on all supported platforms
  local testingBuilds = generateBuilds([
    graalJs + common.gateStyleFullBuild                                                                   + {name: 'js-style-fullbuild'} +
      defaultToTarget(common.gate) +
      includePlatforms([common.linux_amd64]),

    graalJs + gateTags('default')                                                                    + ce + {name: 'js-default-ce'} +
      promoteToTarget(common.gate, [common.jdk17 + common.linux_amd64, common.jdk20 + common.linux_amd64, common.jdk17 + common.linux_aarch64, common.jdk17 + common.windows_amd64]),
    graalJs + gateTags('default')                                                                    + ee + {name: 'js-default-ee'} +
      promoteToTarget(common.gate, [common.jdk17 + common.linux_amd64, common.jdk17 + common.darwin_aarch64]) +
      promoteToTarget(common.postMerge, [common.jdk17 + common.darwin_amd64]),

    graalJs + gateTags('noic')                                                                            + {name: 'js-noic'} + gateOnMain,
    graalJs + gateTags('directbytebuffer')                                                                + {name: 'js-directbytebuffer'} + gateOnMain,
    graalJs + gateTags('cloneuninitialized')                                                              + {name: 'js-cloneuninitialized'} + gateOnMain,
    graalJs + gateTags('lazytranslation')                                                                 + {name: 'js-lazytranslation'} + gateOnMain,
    graalJs + gateTags('shareengine')                                                                     + {name: 'js-shareengine'} + gateOnMain,
    graalJs + gateTags('latestversion')                                                                   + {name: 'js-latestversion'} + gateOnMain,
    graalJs + gateTags('instrument')                                                                      + {name: 'js-instrument'} + gateOnMain,
    graalJs + gateTags('tck')                                                                             + {name: 'js-tck'} + gateOnMain +
      excludePlatforms([common.darwin_amd64]), # Timeout/OOME
    graalJs + webassemblyTest                                                                             + {name: 'js-webassembly'} + gateOnMain,
    graalJs + nativeImageSmokeTest                                                                        + {name: 'js-native-image-smoke-test'} + gateOnMain,
    graalJs + auxEngineCache                                                                         + ee + {name: 'js-aux-engine-cache'} + gateOnMain +
      excludePlatforms([common.windows_amd64, common.darwin_amd64]), # unsupported on windows, too slow on darwin-amd64

    // downstream graal gate
    graalJs + downstreamGraal                                                                             + {name: 'js-downstream-graal'} +
      promoteToTarget(common.gate, [common.jdk17 + common.linux_amd64, common.jdk20 + common.linux_amd64]),
    graalJs + downstreamSubstratevmEE   + {environment+: {TAGS: 'downtest_js'}}                           + {name: 'js-downstream-substratevm-enterprise'} + gateOnMain +
      excludePlatforms([common.darwin_amd64]) + # Too slow
      excludePlatforms([common.linux_aarch64]), # Fails on Linux AArch64 with "Creation of the VM failed."

    // js.zone-rules-based-time-zones
    graalJs + gateTags('zonerulesbasedtimezones')                                                         + {name: 'js-zonerulesbasedtimezones'} +
      promoteToTarget(common.postMerge, [ci.mainGatePlatform]),

    // PGO profiles
    graalJs + downstreamSubstratevmEE   + {environment+: {TAGS: 'pgo_collect_js'}}                        + {name: 'js-pgo-profiles'} +
      promoteToTarget(common.postMerge, [ci.mainGatePlatform]) +
      excludePlatforms([common.darwin_amd64]) +  # Too slow
      excludePlatforms([common.darwin_aarch64]), # No such file or directory: 'mvn'
  ], defaultTarget=common.weekly),

  // Builds that only need to run on one platform
  local otherBuilds = generateBuilds([
    graalJs + common.gate      + mavenDeployDryRun                                                        + {name: 'js-maven-dry-run'},
    graalJs + common.weekly    + gateCoverage                                                             + {name: 'js-coverage'},
  ], platforms=[ci.mainGatePlatform]),

  // Benchmark builds; need to run on a benchmark machine
  local benchBuilds = generateBuilds([
    graalJs + common.bench     + interopJmhBenchmarks                                                     + {name: 'js-interop-jmh'},
  ], platforms=[common.jdk17 + common.x52]),

  builds: testingBuilds + otherBuilds + benchBuilds,
}
