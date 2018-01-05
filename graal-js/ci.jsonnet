local common = import '../common.jsonnet';

{
  local graalJsCommon = common.common + {
    setup+: [
      ['cd', 'graal-js'],
    ],
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '--strict-mode'],

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

  local gateGraalTip = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../graal'],
      ['mx', 'sversions'],
    ],
    run+: [
      ['mx', 'build', '--force-javac'],
    ],
  },

  local deployBinary = {
    setup+: [
      ['mx', 'sversions'],
      ['mx', 'build', '--force-javac'],
    ],
    run+: [
      ['mx', 'deploy-binary-if-master', '--skip-existing', 'graaljs-binary-snapshots'],
    ],
    timelimit: '10:00',
  },

  builds: [
    // gates
    graalJsCommon + common.jdk8 + gateGraalImport + common.gate + common.linux + {name: 'js-gate-graal-import-jdk8-linux-amd64'},
    graalJsCommon + common.jdk9 + gateGraalImport + common.gate + common.linux + {name: 'js-gate-graal-import-jdk9-linux-amd64'},
    graalJsCommon + common.jdk8 + gateGraalTip    + common.gate + common.linux + {name: 'js-gate-graal-tip-jdk8-linux-amd64'},
    graalJsCommon + common.jdk8 + gateGraalTip    + common.gate + common.sparc + {name: 'js-gate-graal-tip-jdk8-solaris-sparcv9'},
    graalJsCommon + common.jdk9 + gateGraalTip    + common.gate + common.linux + {name: 'js-gate-graal-tip-jdk9-linux-amd64'},

    // post-merges
    graalJsCommon + common.jdk8 + deployBinary + common.deploy + common.postMerge + common.ol65 + {name: 'js-deploybinary-ol65-amd64'},
  ],
}
