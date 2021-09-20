local common = import '../common.jsonnet';

{
  local graalNodeJs = {
    setup+: [
      ['cd', 'graal-nodejs'],
      ['mx', 'sversions'],
    ],
  },

  local gateSubstrateVm = {
    run+: [
      ['mx', '--env', 'svm', 'build'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--no-warning', '--env', 'svm', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', 'console.log(\'Hello, World!\')'],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
    ],
    timelimit: '45:00',
  },

  local testNode = common.build + {
    run+: [
      ['mx', 'testnode', '-Xmx${MAX_HEAP}', '${SUITE}', '${PART}'],
    ],
    timelimit: '1:15:00',
  },

  local buildAddons = common.build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-addons'],
    ],
    timelimit: '30:00',
  },

  local buildNodeAPI = common.build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-node-api-tests'],
    ],
    timelimit: '30:00',
  },

  local buildJSNativeAPI = common.build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-js-native-api-tests'],
    ],
    timelimit: '30:00',
  },

  local parallelHttp2 = 'parallel/test-http2-.*',
  local parallelNoHttp2 = 'parallel/(?!test-http2-).*',

  builds: [
    // gates
    graalNodeJs + common.jdk8  + common.gate      + common.linux          + common.gateStyleFullBuild                                                                               + {name: 'nodejs-gate-style-fullbuild-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux          + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk8-linux-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.linux          + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.linux_aarch64  + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk11-linux-aarch64'},
    graalNodeJs + common.jdk15 + common.gate      + common.linux          + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk15-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.darwin         + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.darwin         + common.gateTags             + {environment+: {TAGS: 'all'}}                                             + {name: 'nodejs-gate-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.windows_vs2010 + common.gateTags             + {environment+: {TAGS: 'windows'}}                                         + {name: 'nodejs-gate-jdk8-windows-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.windows_vs2017 + common.gateTags             + {environment+: {TAGS: 'windows'}}                                         + {name: 'nodejs-gate-jdk11-windows-amd64'},
//  disabled due to GR-26245
//    graalNodeJs + common.jdk15 + common.gate      + common.windows_vs2019 + common.gateTags             + {environment+: {TAGS: 'windows'}}                                         + {name: 'nodejs-gate-jdk15-windows-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux          + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.darwin         + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk8-darwin-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.linux          + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk11-linux-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.darwin         + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk11-darwin-amd64'},
    graalNodeJs + common.jdk15 + common.gate      + common.linux          + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk15-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.windows_vs2017 + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk8-windows-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.windows_vs2017 + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk11-windows-amd64'},
    graalNodeJs + common.jdk15 + common.gate      + common.windows_vs2019 + gateSubstrateVm                                                                                         + {name: 'nodejs-gate-substratevm-jdk15-windows-amd64'},

    graalNodeJs + common.jdk8  + common.gate      + common.linux          + buildAddons      + testNode + {environment+: {SUITE: 'addons',        PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-addons-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux          + buildNodeAPI     + testNode + {environment+: {SUITE: 'node-api',      PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-node-api-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux          + buildJSNativeAPI + testNode + {environment+: {SUITE: 'js-native-api', PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-js-native-api-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: 'async-hooks',   PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-async-hooks-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: 'es-module',     PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-es-module-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: 'sequential',    PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-sequential-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: parallelNoHttp2, PART: '-r0,5', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-parallel-1-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: parallelNoHttp2, PART: '-r1,5', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-parallel-2-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: parallelNoHttp2, PART: '-r2,5', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-parallel-3-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: parallelNoHttp2, PART: '-r3,5', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-parallel-4-jdk8-linux-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + testNode + {environment+: {SUITE: parallelNoHttp2, PART: '-r4,5', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-parallel-5-jdk8-linux-amd64'},

    graalNodeJs + common.jdk8  + common.gate      + common.windows_vs2010                    + testNode + {environment+: {SUITE: 'async-hooks',   PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-async-hooks-jdk8-windows-amd64'},
    graalNodeJs + common.jdk8  + common.gate      + common.windows_vs2010                    + testNode + {environment+: {SUITE: 'es-module',     PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-gate-es-module-jdk8-windows-amd64'},
    # We run the `sequential` tests with a smaller heap because `test/sequential/test-child-process-pass-fd.js` starts 80 child processes.
    graalNodeJs + common.jdk8  + common.gate      + common.windows_vs2010                    + testNode + {environment+: {SUITE: 'sequential',    PART: '-r0,1', MAX_HEAP: '512M'}} + {name: 'nodejs-gate-sequential-jdk8-windows-amd64'},

    // post-merges
    graalNodeJs + common.jdk8  + common.postMerge + common.linux                             + testNode + {environment+: {SUITE: parallelHttp2,   PART: '-r0,1', MAX_HEAP: '8G'}}   + {name: 'nodejs-postmerge-parallel-http2-jdk8-linux-amd64'},
  ],
}
