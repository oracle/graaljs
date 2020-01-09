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
      ECLIPSE: {name: 'eclipse', version: '4.5.2.1', platformspecific: true},
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
      ECLIPSE: {name: 'eclipse', version: '4.5.2.1', platformspecific: true},
    },
    environment+: {
      ECLIPSE_EXE: '$ECLIPSE/eclipse',
    },
    setup+: [
      ['mx', 'sversions'],
    ],
    run+: [
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', '${GATE_TAGS}', '--jacocout', 'html'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', 'sonarqube-upload', "-Dsonar.host.url=$SONAR_HOST_URL", "-Dsonar.projectKey=com.oracle.graalvm.js", "-Dsonar.projectName=GraalVM - JS", '--exclude-generated'],
      ['mx', '--jacoco-whitelist-package', 'com.oracle.js.parser', '--jacoco-whitelist-package', 'com.oracle.truffle.js', '--jacoco-exclude-annotation', '@GeneratedBy', 'coverage-upload']
    ],
    timelimit: '30:00',
  },

  local gateGraalTip = {
    setup+: [
      ['git', 'clone', '--branch', 'release/graal-vm/19.3', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../../graal'],
      ['mx', 'sversions'],
    ],
    run+: [
      ['mx', 'build', '--force-javac'],
      gateCmd,
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

  builds: [
    // jdk 8 - linux
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalImport  + {environment+: {GATE_TAGS: 'style,fullbuild'}}    + {name: 'js-gate-style-fullbuild-graal-import-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'noic'}}               + {name: 'js-gate-noic-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'directbytebuffer'}}   + {name: 'js-gate-directbytebuffer-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'cloneuninitialized'}} + {name: 'js-gate-cloneuninitialized-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'lazytranslation'}}    + {name: 'js-gate-lazytranslation-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'shareengine'}}        + {name: 'js-gate-shareengine-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate   + common.linux + gateGraalImport  + {environment+: {GATE_TAGS: 'tck,build'}}          + {name: 'js-gate-tck-build-graal-import-jdk8-linux-amd64'},

    // jdk8 - coverage
    graalJs + common.jdk8 + common.weekly + common.linux + gateCoverage     + {environment+: {GATE_TAGS: 'fullbuild,default'}}  + {name: 'js-coverage-jdk8-linux-amd64'},

    // jdk 8 - sparc
    graalJs + common.jdk8  + common.gate  + common.sparc + gateGraalTip     + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-solaris-sparcv9'},

    // jdk 11 - linux
    graalJs + common.jdk11 + common.gate  + common.linux + gateGraalImport  + {environment+: {GATE_TAGS: 'style,fullbuild'}}    + {name: 'js-gate-style-fullbuild-graal-import-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate  + common.linux + gateGraalTip     + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate  + common.linux                    + mavenDeployDryRun                                 + {name: 'js-gate-maven-dry-run-jdk11-linux-amd64'},

    // jdk 11 - linux aarch64
    graalJs + common.jdk11 + common.gate  + common.linux_aarch64 + gateGraalTip     + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk11-linux-aarch64'},
  ],
}
