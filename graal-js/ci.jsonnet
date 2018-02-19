local common = import '../common.jsonnet';

{
  local graalJs = {
    setup: [
      ['cd', 'graal-js'],
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
      gateCmd,
    ],
    timelimit: '15:00',
  },

  local gateGraalTip = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/graal.git'], '../../graal'],
      ['mx', 'sversions'],
    ],
    run+: [
      ['mx', 'build', '--force-javac'],
      gateCmd,
    ],
    timelimit: '30:00',
  },

  builds: [
    // gates
    graalJs + common.jdk8 + gateGraalImport + {environment+: {GATE_TAGS: 'style,fullbuild'}} + common.gate + common.linux + {name: 'js-gate-style-fullbuild-graal-import-jdk8-linux-amd64'},
    graalJs + common.jdk9 + gateGraalImport + {environment+: {GATE_TAGS: 'style,fullbuild'}} + common.gate + common.linux + {name: 'js-gate-style-fullbuild-graal-import-jdk9-linux-amd64'},
    graalJs + common.jdk8 + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}         + common.gate + common.linux + {name: 'js-gate-default-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}         + common.gate + common.sparc + {name: 'js-gate-default-graal-tip-jdk8-solaris-sparcv9'},
    graalJs + common.jdk9 + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}         + common.gate + common.linux + {name: 'js-gate-default-graal-tip-jdk9-linux-amd64'},
  ],
}
