local common = import '../common.jsonnet';

{
  local graalJs = {
    setup: [
      ['cd', 'graal-js'],
    ],
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', '${GATE_TAGS}'],

  local gateGraalImport = {
    downloads+: {
      ECLIPSE: {name: 'eclipse', version: '4.14.0', platformspecific: true},
    },
    environment+: {
      ECLIPSE_EXE: '$ECLIPSE/eclipse',
    },
    setup+: [
      ['mx', 'sversions'],
    ],
    run+: [
      gateCmd,
    ],
    timelimit: '15:00',
  },

  local gateCoverage = {
    downloads+: {
      ECLIPSE: {name: 'eclipse', version: '4.14.0', platformspecific: true},
    },
    environment+: {
      ECLIPSE_EXE: '$ECLIPSE/eclipse',
    },
    setup+: [
      ['mx', 'sversions'],
    ],
    run+: [
      ['set-export', 'GRAALJS_HOME', ['pwd']],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', '${GATE_TAGS}', '--jacocout', 'html'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', 'sonarqube-upload', "-Dsonar.host.url=$SONAR_HOST_URL", "-Dsonar.projectKey=com.oracle.graalvm.js", "-Dsonar.projectName=GraalVM - JS", '--exclude-generated'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--jacoco-dest-file', '${GRAALJS_HOME}/jacoco.exec', 'coverage-upload']
    ],
    timelimit: '30:00',
  },

  local graalTip = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../../graal'],
      ['mx', 'sversions'],
    ],
    timelimit: '30:00',
  },

  local gateGraalTip = graalTip + {
    run+: [
      ['mx', 'build', '--force-javac'],
      gateCmd,
    ],
    timelimit: '30:00',
  },

  local benchmarkGraalTip = graalTip + {
    run+: [
      ['mx', '--dynamicimports', '/compiler', 'build', '--force-javac'],
    ],
    timelimit: '30:00',
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
    timelimit: '10:00',
  },

  local interopJmhBenchmarks = {
    run+: [
        ["mx", "--dynamicimports", "/compiler", "--kill-with-sigquit", "benchmark", "--results-file", "bench-results.json", "js-interop-jmh:JS_INTEROP_MICRO_BENCHMARKS", "--", "-Dgraal.TraceTruffleCompilation=true"],
        ['bench-uploader.py', 'bench-results.json'],
    ],
    timelimit: '30:00',
  },

  builds: [
    // jdk 8 - linux
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalImport       + {environment+: {GATE_TAGS: 'style,fullbuild'}}    + {name: 'js-gate-style-fullbuild-graal-import-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'noic'}}               + {name: 'js-gate-noic-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'directbytebuffer'}}   + {name: 'js-gate-directbytebuffer-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'cloneuninitialized'}} + {name: 'js-gate-cloneuninitialized-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'lazytranslation'}}    + {name: 'js-gate-lazytranslation-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'shareengine'}}        + {name: 'js-gate-shareengine-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'latestesversion'}}    + {name: 'js-gate-latestesversion-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalImport       + {environment+: {GATE_TAGS: 'tck,build'}}          + {name: 'js-gate-tck-build-graal-import-jdk8-linux-amd64'},

    // jdk 8 - coverage
    graalJs + common.jdk8 + common.weekly + common.linux + gateCoverage          + {environment+: {GATE_TAGS: 'build,default,tck'}}  + {name: 'js-coverage-jdk8-linux-amd64'},

    // jdk 8 - windows
    graalJs + common.jdk8  + common.gate  + common.windows_vs2010 + gateGraalTip + {environment+: {GATE_TAGS: 'Test262-default'}}    + {name: 'js-gate-test262-default-graal-tip-jdk8-windows-amd64'},

    // jdk 8 - sparc
    graalJs + common.jdk8  + common.gate  + common.sparc + gateGraalTip          + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-solaris-sparcv9'},

    // jdk 11 - linux
    graalJs + common.jdk11 + common.gate  + common.linux + gateGraalImport       + {environment+: {GATE_TAGS: 'style,fullbuild'}}    + {name: 'js-gate-style-fullbuild-graal-import-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate  + common.linux + gateGraalTip          + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate  + common.linux                         + mavenDeployDryRun                                 + {name: 'js-gate-maven-dry-run-jdk11-linux-amd64'},

    // jdk 11 - linux aarch64
    graalJs + common.jdk11 + common.gate  + common.linux_aarch64 + gateGraalTip  + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk11-linux-aarch64'},

    // jdk 11 - windows
    graalJs + common.jdk11 + common.gate  + common.windows + gateGraalTip        + {environment+: {GATE_TAGS: 'Test262-default'}}    + {name: 'js-gate-test262-default-graal-tip-jdk11-windows-amd64'},

    // interop benchmarks
    graalJs + common.jdk8 + common.bench  + common.x52 + benchmarkGraalTip       + interopJmhBenchmarks                              + {name: 'js-interop-jmh-bechmarks-jdk8-x52'},
  ],
}
