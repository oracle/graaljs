{
  local labsjdk8 = {name: 'labsjdk', version: '8u141-jvmci-0.36', platformspecific: true},

  local labsjdk9 = {name: 'labsjdk', version: '9+181', platformspecific: true},

  local jdk8 = {
    downloads: {
      JAVA_HOME: labsjdk8,
      JDT: {name: 'ecj', version: '4.5.1', platformspecific: false},
    },
  },

  local jdk9 = {
    downloads: {
      EXTRA_JAVA_HOMES: labsjdk8,
      JAVA_HOME: labsjdk9,
    },
  },

  local common = {
    packages: {
      git: '>=1.8.3',
      maven: '==3.3.9',
      mercurial: '>=3.2.4',
      'pip:astroid': '==1.1.0',
      'pip:pylint': '==1.1.0',
    },
    catch_files: [
      'Graal diagnostic output saved in (?P<filename>.+.zip)',
    ],
    timelimit: '30:00',
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

  local deploy    = {targets: ['deploy']},
  local gate      = {targets: ['gate']},
  local postMerge = {targets: ['post-merge']},

  local linux = {capabilities: ['linux', 'amd64']},
  local ol65  = {capabilities: ['ol65', 'amd64']},
  local sparc = {capabilities: ['solaris', 'sparcv9']},

  builds: [
    // gates
    common + jdk8 + gateGraalImport + gate + linux + {name: 'js-gate-graal-import-jdk8-linux-amd64'},
    common + jdk9 + gateGraalImport + gate + linux + {name: 'js-gate-graal-import-jdk9-linux-amd64'},
    common + jdk8 + gateGraalTip    + gate + linux + {name: 'js-gate-graal-tip-jdk8-linux-amd64'},
    common + jdk8 + gateGraalTip    + gate + sparc + {name: 'js-gate-graal-tip-jdk8-solaris-sparcv9'},
    common + jdk9 + gateGraalTip    + gate + linux + {name: 'js-gate-graal-tip-jdk9-linux-amd64'},

    // post-merges
    common + jdk8 + deployBinary + deploy + postMerge + ol65 + {name: 'js-deploybinary-ol65-amd64'},
  ],
}
