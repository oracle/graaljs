local common = import '../common.jsonnet';

{
  local graalNodeJs = {
    setup+: [
      ['cd', 'graal-nodejs'],
    ],
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode'],

  local gateTruffleImport = {
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
      gateCmd + ['--tags', 'style,fullbuild'],
    ],
    timelimit: '30:00',
  },

  local cloneGraalTip = {
    run+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../../graal'],
      ['mx', 'sversions'],
    ],
  },

  local buildTruffleTip = cloneGraalTip + {
    run+: [
      ['mx', 'build', '--force-javac'],
    ],
  },

  local buildCompilerTip = cloneGraalTip + {
    run+: [
      ['mx', '--dynamicimports', '/compiler', 'build', '--force-javac'],
    ],
  },

  local gateTruffleTip = buildTruffleTip + {
    run+: [
      gateCmd + ['--tags', '$TAGS'],
    ],
    timelimit: '30:00',
  },

  local gateSubstrateVmTip = buildTruffleTip + {
    run+: [
      ['mx', '-p', '../../graal/substratevm', 'build', '--force-javac'],
      ['mx', '--env', 'svm', 'build'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--no-warning', '--env', 'svm', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', 'console.log(\'Hello, World!\')'],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
    ],
    timelimit: '45:00',
  },

  local testNodeTruffleTip = {
    run+: [
      ['mx', 'testnode', '-Xmx${MAX_HEAP}', '${SUITE}', '${PART}'],
    ],
    timelimit: '1:15:00',
  },

  local buildAddons = {
    run+: [
      ['mx', 'makeinnodeenv', 'build-addons'],
    ],
    timelimit: '30:00',
  },

  local buildNodeAPI = {
    run+: [
      ['mx', 'makeinnodeenv', 'build-node-api-tests'],
    ],
    timelimit: '30:00',
  },

  local buildJSNativeAPI = {
    run+: [
      ['mx', 'makeinnodeenv', 'build-js-native-api-tests'],
    ],
    timelimit: '30:00',
  },

  local parallelHttp2 = 'parallel/test-http2-.*',
  local parallelNoHttp2 = 'parallel/(?!test-http2-).*',

  builds: [
    // gates
    graalNodeJs + common.jdk8  + gateTruffleImport                                                                                                                   + common.gate + common.linux          + {name: 'nodejs-gate-graal-import-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + gateTruffleTip                                          + {environment+: {TAGS: 'all'}}                                             + common.gate + common.linux          + {name: 'nodejs-gate-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk11 + gateTruffleTip                                          + {environment+: {TAGS: 'all'}}                                             + common.gate + common.linux          + {name: 'nodejs-gate-graal-tip-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + gateTruffleTip                                          + {environment+: {TAGS: 'all'}}                                             + common.gate + common.linux_aarch64  + {name: 'nodejs-gate-graal-tip-jdk11-linux-aarch64'},
    graalNodeJs + common.jdk8  + gateTruffleTip                                          + {environment+: {TAGS: 'all'}}                                             + common.gate + common.darwin         + {name: 'nodejs-gate-graal-tip-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + gateTruffleTip                                          + {environment+: {TAGS: 'all'}}                                             + common.gate + common.darwin         + {name: 'nodejs-gate-graal-tip-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk8  + gateTruffleTip                                          + {environment+: {TAGS: 'windows'}}                                         + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk11 + gateTruffleTip                                          + {environment+: {TAGS: 'windows'}}                                         + common.gate + common.windows        + {name: 'nodejs-gate-graal-tip-jdk11-windows-amd64'},
    graalNodeJs + common.jdk8  + gateSubstrateVmTip                                                                                                                  + common.gate + common.linux          + {name: 'nodejs-gate-substratevm-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + gateSubstrateVmTip                                                                                                                  + common.gate + common.darwin         + {name: 'nodejs-gate-substratevm-tip-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + gateSubstrateVmTip                                                                                                                  + common.gate + common.linux          + {name: 'nodejs-gate-substratevm-tip-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + gateSubstrateVmTip                                                                                                                  + common.gate + common.darwin         + {name: 'nodejs-gate-substratevm-tip-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk8  + gateSubstrateVmTip                                                                                                                  + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-substratevm-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk11 + gateSubstrateVmTip                                                                                                                  + common.gate + common.windows        + {name: 'nodejs-gate-substratevm-tip-jdk11-windows-amd64'},

    graalNodeJs + common.jdk8  + buildTruffleTip + buildAddons      + testNodeTruffleTip + {environment+: {SUITE: 'addons',        PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-addons-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip + buildNodeAPI     + testNodeTruffleTip + {environment+: {SUITE: 'node-api',      PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-node-api-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip + buildJSNativeAPI + testNodeTruffleTip + {environment+: {SUITE: 'js-native-api', PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-js-native-api-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'async-hooks',   PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-async-hooks-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'es-module',     PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-es-module-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'sequential',    PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-sequential-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r0,5', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-parallel-1-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r1,5', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-parallel-2-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r2,5', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-parallel-3-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r3,5', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-parallel-4-graal-tip-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r4,5', MAX_HEAP: '8G'}}   + common.gate + common.linux          + {name: 'nodejs-gate-parallel-5-graal-tip-jdk8-linux-amd64'},

    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'async-hooks',   PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-async-hooks-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'es-module',     PART: '-r0,1', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-es-module-graal-tip-jdk8-windows-amd64'},
    # We run the `sequential` tests with a smaller heap because `test/sequential/test-child-process-pass-fd.js` starts 80 child processes.
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: 'sequential',    PART: '-r0,1', MAX_HEAP: '512M'}} + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-sequential-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r0,5', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-parallel-1-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r1,5', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-parallel-2-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r2,5', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-parallel-3-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r3,5', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-parallel-4-graal-tip-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelNoHttp2, PART: '-r4,5', MAX_HEAP: '8G'}}   + common.gate + common.windows_vs2010 + {name: 'nodejs-gate-parallel-5-graal-tip-jdk8-windows-amd64'},

    // post-merges
    graalNodeJs + common.jdk8  + buildTruffleTip                    + testNodeTruffleTip + {environment+: {SUITE: parallelHttp2,   PART: '-r0,1', MAX_HEAP: '8G'}}   + common.postMerge + common.linux     + {name: 'nodejs-postmerge-parallel-http2-graal-tip-jdk8-linux-amd64'},
  ],
}
