local common = import '../common.jsonnet';

{
  local graalNodeJsCommon = common.common + {
    setup+: [
      ['cd', 'graal-nodejs'],
    ],
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '--strict-mode', '--tags', '${GATE_TAGS}'],

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
      gateCmd + ['--tags', 'style,fullbuild,mvnPackage,sharedBuild'],
    ],
  },

  local buildGraalTip = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../graal'],
      ['mx', 'sversions'],
      ['mx', 'build', '--force-javac'],
    ],
  },

  local gateGraalTip = buildGraalTip + {
    run+: [
      gateCmd,
    ],
  },

  local testNodeGraalTip = buildGraalTip + {
    run+: [
      ['mx', 'testnode', '${SUITE}', '${PART}'],
    ],
    timelimit: '1:15:00',
  },

  local buildAddons = {
    setup+: [
      ['mx', 'makeinnodeenv', 'build-addons'],
      ['mx', 'makeinnodeenv', 'build-addons-napi'],
    ],
  },

  local deployBinary = {
    setup+: [
      ['mx', 'sversions'],
      ['mx', 'build', '--force-javac'],
    ],
    run+: [
      ['mx', 'deploy-binary-if-master', '--all-suites', '--skip-existing', 'graalnodejs-binary-snapshots'],
    ],
    timelimit: '10:00',
  },

  builds: [
    // gates
    graalNodeJsCommon + common.jdk8 + gateGraalImport                                                                        + common.gate + common.linux + {name: 'nodejs-gate-graal-import-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + gateGraalTip                   + {environment+: {GATE_TAGS: 'build'}}                  + common.gate + common.linux + {name: 'nodejs-gate-alltests-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip + buildAddons + {environment+: {SUITE: 'addons', PART: '-r0,1'}}      + common.gate + common.linux + {name: 'nodejs-gate-addons-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip + buildAddons + {environment+: {SUITE: 'addons-napi', PART: '-r0,1'}} + common.gate + common.linux + {name: 'nodejs-gate-addons-napi-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'async-hooks', PART: '-r0,1'}} + common.gate + common.linux + {name: 'nodejs-gate-async-hooks-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'es-module', PART: '-r0,1'}}   + common.gate + common.linux + {name: 'nodejs-gate-es-module-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'sequential', PART: '-r0,1'}}  + common.gate + common.linux + {name: 'nodejs-gate-sequential-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'parallel', PART: '-r0,5'}}    + common.gate + common.linux + {name: 'nodejs-gate-parallel-1-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'parallel', PART: '-r1,5'}}    + common.gate + common.linux + {name: 'nodejs-gate-parallel-2-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'parallel', PART: '-r2,5'}}    + common.gate + common.linux + {name: 'nodejs-gate-parallel-3-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'parallel', PART: '-r3,5'}}    + common.gate + common.linux + {name: 'nodejs-gate-parallel-4-graal-tip-jdk8-linux-amd64'},
    graalNodeJsCommon + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: 'parallel', PART: '-r4,5'}}    + common.gate + common.linux + {name: 'nodejs-gate-parallel-5-graal-tip-jdk8-linux-amd64'},

    // post-merges
    graalNodeJsCommon + common.jdk8 + deployBinary + common.deploy + common.postMerge + common.ol65 + {name: 'nodejs-deploybinary-ol65-amd64'},
  ],
}
