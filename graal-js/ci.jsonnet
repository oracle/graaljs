local common = import '../common.jsonnet';

{
  local graalJs = {
    setup: [
      ['cd', 'graal-js'],
      ['mx', 'sversions'],
    ],
  },

  local gateCoverage = common.eclipse + {
    run+: [
      ['set-export', 'GRAALJS_HOME', ['pwd']],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', '${TAGS}', '--jacocout', 'html'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', 'sonarqube-upload', '-Dsonar.host.url=$SONAR_HOST_URL', '-Dsonar.projectKey=com.oracle.graalvm.js', '-Dsonar.projectName=GraalVM - JS', '--exclude-generated'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', 'coverage-upload']
    ],
    timelimit: '30:00',
  },

  local nativeImageSmokeTest = {
    local baseNativeImageCmd = ['mx', '--dynamicimports', '/substratevm', '--native-images=js', '--extra-image-builder-argument=-H:+TruffleCheckBlackListedMethods', '--extra-image-builder-argument=-H:+ReportExceptionStackTraces'],
    run+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/js-benchmarks.git'], '../../js-benchmarks'],
      baseNativeImageCmd + ['build'],
      ['set-export', 'GRAALVM_HOME', baseNativeImageCmd + ['graalvm-home']],
      ['${GRAALVM_HOME}/bin/js', '-e', 'print("hello:" + Array.from(new Array(10), (x,i) => i*i ).join("|"))'],
      ['${GRAALVM_HOME}/bin/js', '../../js-benchmarks/harness.js', '--', '../../js-benchmarks/octane-richards.js', '--show-warmup'],
    ],
  },

  local mavenDeployDryRun = {
    run+: [
      ['mx', 'build'],
      ['mx', '-v', 'maven-deploy', '--suppress-javadoc', '--validate', 'full', '--licenses', 'UPL,MIT', '--dry-run', 'ossrh', 'https://this-is-only-a-test'],
      ['mx', '--dynamicimports', '/tools,/compiler', 'build'],
      ['mx', '--dynamicimports', '/tools,/regex,/compiler,/truffle,/sdk', 'maven-deploy', '--suppress-javadoc', '--all-suites', '--all-distribution-types', '--version-string', 'GATE'],
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal-js-jdk11-maven-demo.git'], 'graal-js-jdk11-maven-demo'],
      ['cd', 'graal-js-jdk11-maven-demo'],
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

  local interopJmhBenchmarks = common.buildCompiler + {
    run+: [
        ['mx', '--dynamicimports', '/compiler', '--kill-with-sigquit', 'benchmark', '--results-file', 'bench-results.json', 'js-interop-jmh:JS_INTEROP_MICRO_BENCHMARKS', '--', '-Dpolyglot.engine.TraceCompilation=true'],
        ['bench-uploader.py', 'bench-results.json'],
    ],
    timelimit: '30:00',
  },

  builds: [
    // jdk 8 - linux
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateStyleFullBuild                                                + {name: 'js-gate-style-fullbuild-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'default'}}            + {name: 'js-gate-default-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'noic'}}               + {name: 'js-gate-noic-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'directbytebuffer'}}   + {name: 'js-gate-directbytebuffer-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'cloneuninitialized'}} + {name: 'js-gate-cloneuninitialized-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'lazytranslation'}}    + {name: 'js-gate-lazytranslation-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'shareengine'}}        + {name: 'js-gate-shareengine-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'latestversion'}}      + {name: 'js-gate-latestversion-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'instrument'}}         + {name: 'js-gate-instrument-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'tck'}}                + {name: 'js-gate-tck-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + webassemblyTest                                                          + {name: 'js-gate-webassembly-jdk8-linux-amd64'},
    graalJs + common.jdk8  + common.gate   + common.linux          + nativeImageSmokeTest                                                     + {name: 'js-gate-native-image-smoke-test-jdk8-linux-amd64'},
    graalJs + common.jdk17 + common.daily  + common.linux          + nativeImageSmokeTest                                                     + {name: 'js-daily-native-image-smoke-test-jdk17-linux-amd64'},

    // jdk 8 - coverage
    graalJs + common.jdk8  + common.weekly + common.linux          + gateCoverage              + {environment+: {TAGS: 'build,default,tck'}}  + {name: 'js-coverage-jdk8-linux-amd64'},

    // jdk 8 - windows
    graalJs + common.jdk8  + common.gate   + common.windows_jdk8   + common.gateTags           + {environment+: {TAGS: 'Test262-default'}}    + {name: 'js-gate-test262-default-jdk8-windows-amd64'},

    // jdk 11 - linux
    graalJs + common.jdk11 + common.gate   + common.linux          + common.gateStyleFullBuild                                                + {name: 'js-gate-style-fullbuild-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate   + common.linux          + common.gateTags           + {environment+: {TAGS: 'default'}}            + {name: 'js-gate-default-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate   + common.linux          + mavenDeployDryRun                                                        + {name: 'js-gate-maven-dry-run-jdk11-linux-amd64'},

    // jdk 11 - linux aarch64
    graalJs + common.jdk11 + common.gate   + common.linux_aarch64  + common.gateTags           + {environment+: {TAGS: 'default'}}            + {name: 'js-gate-default-jdk11-linux-aarch64'},

    // jdk 11 - windows
    graalJs + common.jdk11 + common.gate   + common.windows_jdk11  + common.gateTags           + {environment+: {TAGS: 'Test262-default'}}    + {name: 'js-gate-test262-default-jdk11-windows-amd64'},

    // jdk 16 - linux
    graalJs + common.jdk17 + common.daily  + common.linux          + common.gateTags           + {environment+: {TAGS: 'default'}}            + {name: 'js-daily-default-jdk17-linux-amd64'},

    // interop benchmarks
    graalJs + common.jdk8  + common.bench  + common.x52            + interopJmhBenchmarks                                                     + {name: 'js-bench-interop-jmh-jdk8-linux-amd64'},
  ],
}
