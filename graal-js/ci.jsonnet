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
    // jdk 8 - linux
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalImport + {environment+: {GATE_TAGS: 'style,fullbuild'}}    + {name: 'js-gate-style-fullbuild-graal-import-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'noic'}}               + {name: 'js-gate-noic-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'directbytebuffer'}}   + {name: 'js-gate-directbytebuffer-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'cloneuninitialized'}} + {name: 'js-gate-cloneuninitialized-graal-tip-jdk8-linux-amd64'},
    graalJs + common.jdk8 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'lazytranslation'}}    + {name: 'js-gate-lazytranslation-graal-tip-jdk8-linux-amd64'},

    // jdk 8 - sparc
    graalJs + common.jdk8 + common.gate + common.sparc + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}            + {name: 'js-gate-default-graal-tip-jdk8-solaris-sparcv9'},

    // jdk 11 - linux
    graalJs + common.jdk11 + common.gate + common.linux + gateGraalImport + {environment+: {GATE_TAGS: 'style,fullbuild'}} + {name: 'js-gate-style-fullbuild-graal-import-jdk11-linux-amd64'},
    graalJs + common.jdk11 + common.gate + common.linux + gateGraalTip    + {environment+: {GATE_TAGS: 'default'}}         + {name: 'js-gate-default-graal-tip-jdk11-linux-amd64'},
  ],
}
