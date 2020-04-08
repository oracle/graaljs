local common = import '../common.jsonnet';

{
  local graalNodeJs = {
    setup+: [
      ['cd', 'graal-nodejs'],
    ],
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode'],

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
      gateCmd + ['--tags', 'style,fullbuild'],
    ],
    timelimit: '30:00',
  },

  local buildGraalTip = {
    setup+: [
      ['git', 'clone', '--branch', 'cpu/graal-vm/20.0.1', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../../graal'],
      ['mx', 'sversions'],
      ['mx', 'build', '--force-javac'],
    ],
  },

  local gateGraalTip = buildGraalTip + {
    run+: [
      gateCmd + ['--tags', 'all'],
    ],
    timelimit: '30:00',
  },

  local gateSubstrateVmTip = buildGraalTip + {
    run+: [
      ['mx', '-p', '../../graal/substratevm', 'build', '--force-javac'],
      ['mx', '--env', 'svm', 'build'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--env', 'svm', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', 'console.log(\'Hello, World!\')'],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
    ],
    timelimit: '45:00',
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
    ],
    timelimit: '30:00',
  },

  local buildNodeAPI = {
    setup+: [
      ['mx', 'makeinnodeenv', 'build-node-api-tests'],
    ],
    timelimit: '30:00',
  },

  local buildJSNativeAPI = {
    setup+: [
      ['mx', 'makeinnodeenv', 'build-js-native-api-tests'],
    ],
    timelimit: '30:00',
  },

  local parallelHttp2 = 'parallel/test-http2-.*',
  local parallelNoHttp2 = 'parallel/(?!test-http2-).*',

  builds: [
    // gates
    graalNodeJs + common.jdk8  + gateGraalImport                                                                          + common.gate + common.linux  + {name: 'nodejs-gate-graal-import-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + gateGraalTip                                                                             + common.gate + common.linux  + {name: 'nodejs-gate-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk11 + gateGraalTip                                                                             + common.gate + common.linux  + {name: 'nodejs-gate-graal-tip-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + gateGraalTip                                                                             + common.gate + common.linux_aarch64  + {name: 'nodejs-gate-graal-tip-jdk11-linux-aarch64'},
    graalNodeJs + common.jdk8  + gateGraalTip                                                                             + common.gate + common.darwin + {name: 'nodejs-gate-graal-tip-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + gateGraalTip                                                                             + common.gate + common.darwin + {name: 'nodejs-gate-graal-tip-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk8  + gateSubstrateVmTip                                                                       + common.gate + common.linux  + {name: 'nodejs-gate-substratevm-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + gateSubstrateVmTip                                                                       + common.gate + common.darwin + {name: 'nodejs-gate-substratevm-tip-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + gateSubstrateVmTip                                                                       + common.gate + common.linux  + {name: 'nodejs-gate-substratevm-tip-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + gateSubstrateVmTip                                                                       + common.gate + common.darwin + {name: 'nodejs-gate-substratevm-tip-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip + buildAddons + {environment+: {SUITE: 'addons', PART: '-r0,1'}}        + common.gate + common.linux  + {name: 'nodejs-gate-addons-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip + buildNodeAPI + {environment+: {SUITE: 'node-api', PART: '-r0,1'}}     + common.gate + common.linux  + {name: 'nodejs-gate-node-api-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip + buildJSNativeAPI + {environment+: {SUITE: 'js-native-api', PART: '-r0,1'}} + common.gate + common.linux + {name: 'nodejs-gate-js-native-api-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: 'async-hooks', PART: '-r0,1'}}   + common.gate + common.linux  + {name: 'nodejs-gate-async-hooks-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: 'es-module', PART: '-r0,1'}}     + common.gate + common.linux  + {name: 'nodejs-gate-es-module-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: 'sequential', PART: '-r0,1'}}    + common.gate + common.linux  + {name: 'nodejs-gate-sequential-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: parallelNoHttp2, PART: '-r0,5'}} + common.gate + common.linux  + {name: 'nodejs-gate-parallel-1-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: parallelNoHttp2, PART: '-r1,5'}} + common.gate + common.linux  + {name: 'nodejs-gate-parallel-2-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: parallelNoHttp2, PART: '-r2,5'}} + common.gate + common.linux  + {name: 'nodejs-gate-parallel-3-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: parallelNoHttp2, PART: '-r3,5'}} + common.gate + common.linux  + {name: 'nodejs-gate-parallel-4-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + testNodeGraalTip               + {environment+: {SUITE: parallelNoHttp2, PART: '-r4,5'}} + common.gate + common.linux  + {name: 'nodejs-gate-parallel-5-graal-tip-jdk8-linux-amd64'},

    // post-merges
    graalNodeJs + common.jdk8 + testNodeGraalTip               + {environment+: {SUITE: parallelHttp2, PART: '-r0,1'}} + common.postMerge + common.linux + {name: 'nodejs-postmerge-parallel-http2-graal-tip-jdk8-linux-amd64'},
  ],
}
