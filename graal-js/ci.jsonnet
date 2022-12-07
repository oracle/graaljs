local common = import '../common.jsonnet';
local ci = import '../ci.jsonnet';

{
  local graalJs = ci.jobtemplate + {
    cd:: 'graal-js'
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
      ['mx', '--dynamicimports', '/wasm', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', 'build,coverage'] + self.coverage_gate_args,
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
      ['${GRAALVM_HOME}/bin/js', '--native', '-e', 'print("hello:" + Array.from(new Array(10), (x,i) => i*i ).join("|"))'],
      ['${GRAALVM_HOME}/bin/js', '--native', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
    ],
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
    timelimit: '15:00',
  },

  local webassemblyTest = {
    run+: [
      ['mx', '--dynamicimports', '/wasm', 'build'],
      ['mx', '--dynamicimports', '/wasm', 'testv8', 'gate', 'polyglot'],
      ['mx', '--dynamicimports', '/wasm', 'gate', '--tags', 'webassembly'],
    ],
    timelimit: '30:00',
  },

  local downstreamGraal = {
    run+: [
      ['cd', '../../graal/vm'],
      ['set-export', 'NATIVE_IMAGES', 'native-image'],
      ['mx', '--dynamicimports', '/graal-js,/substratevm', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', 'build,svm-truffle-tck-js'],
    ],
    timelimit: '45:00',
  },

  local downstreamSubstratevmEE = checkoutJsBenchmarks + ee + {
    suiteimports+:: ['substratevm'],
    run+: [
      ['mx', '--strict-compliance', 'gate', '--all-suites', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', 'build,${TAGS}'],
    ],
    timelimit: '45:00',
  },

  local interopJmhBenchmarks = common.buildCompiler + {
    run+: [
        ['mx', '--dynamicimports', '/compiler', '--kill-with-sigquit', 'benchmark', '--results-file', 'bench-results.json', 'js-interop-jmh:JS_INTEROP_MICRO_BENCHMARKS', '--', '-Dpolyglot.engine.TraceCompilation=true'],
        ['bench-uploader.py', 'bench-results.json'],
    ],
    timelimit: '30:00',
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

  builds: [
    // GATE
    // style
    graalJs + common.jdk17 + common.gate   + common.linux          + common.gateStyleFullBuild                                                + {name: 'js-gate-style-fullbuild-jdk17-linux-amd64'},
    graalJs + common.jdk20 + common.gate   + common.linux          + common.gateStyleFullBuild                                                + {name: 'js-gate-style-fullbuild-jdk20-linux-amd64'},

    // linux amd64
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('default')                                                 + ce + {name: 'js-gate-default-ce-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('default')                                                 + ee + {name: 'js-gate-default-ee-jdk17-linux-amd64'},
    graalJs + common.jdk20 + common.gate   + common.linux          + gateTags('default')                                                 + ce + {name: 'js-gate-default-ce-jdk20-linux-amd64'} ,
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('noic')                                                         + {name: 'js-gate-noic-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('directbytebuffer')                                             + {name: 'js-gate-directbytebuffer-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('cloneuninitialized')                                           + {name: 'js-gate-cloneuninitialized-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('lazytranslation')                                              + {name: 'js-gate-lazytranslation-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('shareengine')                                                  + {name: 'js-gate-shareengine-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('latestversion')                                                + {name: 'js-gate-latestversion-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('instrument')                                                   + {name: 'js-gate-instrument-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + gateTags('tck')                                                          + {name: 'js-gate-tck-jdk17-linux-amd64'}        ,
    graalJs + common.jdk17 + common.gate   + common.linux          + webassemblyTest                                                          + {name: 'js-gate-webassembly-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + nativeImageSmokeTest                                                     + {name: 'js-gate-native-image-smoke-test-jdk17-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + auxEngineCache                                                      + ee + {name: 'js-gate-aux-engine-cache-jdk17-linux-amd64'},

    // windows amd64
    graalJs + common.jdk17 + common.gate   + common.windows        + gateTags('Test262-default')                                              + {name: 'js-gate-test262-default-jdk17-windows-amd64'},
    graalJs + common.jdk17 + common.gate   + common.windows        + gateTags('default')                                                 + ce + {name: 'js-gate-default-ce-jdk17-windows-amd64'},

    // darwin aarch64
    graalJs + common.jdk17 + common.gate   + common.darwin_aarch64 + gateTags('default')                                                 + ee + {name: 'js-gate-default-ee-jdk17-darwin-aarch64'},

    // linux aarch64
    graalJs + common.jdk17 + common.gate   + common.linux_aarch64  + gateTags('default')                                                      + {name: 'js-gate-default-ce-jdk17-linux-aarch64'},

    // maven deploy dry run
    graalJs + common.jdk17 + common.gate   + common.linux          + mavenDeployDryRun                                                        + {name: 'js-gate-maven-dry-run-jdk17-linux-amd64'},

    // downstream graal gate
    graalJs + common.jdk17 + common.gate   + common.linux          + downstreamGraal                                                          + {name: 'js-gate-downstream-graal-jdk17-linux-amd64'},
    graalJs + common.jdk20 + common.gate   + common.linux          + downstreamGraal                                                          + {name: 'js-gate-downstream-graal-jdk20-linux-amd64'},
    graalJs + common.jdk17 + common.gate   + common.linux          + downstreamSubstratevmEE   + {environment+: {TAGS: 'downtest_js'}}        + {name: 'js-gate-downstream-substratevm-enterprise-jdk17-linux-amd64'},

    // interop benchmarks
    graalJs + common.jdk17 + common.bench  + common.x52            + interopJmhBenchmarks                                                     + {name: 'js-bench-interop-jmh-jdk17-linux-amd64'},

    // POST-MERGE
    graalJs + common.jdk17 + common.postMerge + common.darwin      + gateTags('default')                                                 + ee + {name: 'js-postmerge-default-ee-jdk17-darwin-amd64', timelimit: '45:00'},

    // PGO profiles
    graalJs + common.jdk17 + common.postMerge + common.linux       + downstreamSubstratevmEE   + {environment+: {TAGS: 'pgo_collect_js'}}     + {name: 'js-postmerge-pgo-profiles-jdk17-linux-amd64'},

    // js.zone-rules-based-time-zones
    graalJs + common.jdk17 + common.postMerge + common.linux       + gateTags('zonerulesbasedtimezones')                                      + {name: 'js-postmerge-zonerulesbasedtimezones-jdk17-linux-amd64'},

    // weekly
    graalJs + common.jdk17 + common.weekly + common.linux          + gateCoverage                                                             + {name: 'weekly-js-coverage-jdk17-linux-amd64'},
  ],
}
