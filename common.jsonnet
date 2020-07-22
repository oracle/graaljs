local jdks = (import "common.json").jdks;

{
  jdk8: {
    downloads+: {
      JAVA_HOME: jdks.oraclejdk8,
    },
  },

  jdk11: {
    downloads+: {
      JAVA_HOME: jdks["labsjdk-ce-11"],
    },
  },

  deploy:      {targets+: ['deploy']},
  gate:        {targets+: ['gate']},
  postMerge:   {targets+: ['post-merge']},
  bench:       {targets+: ['bench']},
  dailyBench:  {targets+: ['bench', 'daily']},
  weeklyBench: {targets+: ['bench', 'weekly']},
  manualBench: {targets+: ['bench']},
  weekly:      {targets+: ['weekly']},

  local python3 = {
    environment+: {
      MX_PYTHON: "python3",
    },
  },

  local common = python3 + {
    packages+: {
      'mx': '5.268.1',
      'pip:pylint': '==1.9.3',
      'pip:ninja_syntax': '==1.7.2',
    },
    catch_files+: [
      'Graal diagnostic output saved in (?P<filename>.+.zip)',
      'npm-debug.log', // created on npm errors
    ],
    environment+: {
      GRAALVM_CHECK_EXPERIMENTAL_OPTIONS: "true",
    },
  },

  linux: common + {
    packages+: {
      'apache/ab': '==2.3',
      binutils: '==2.23.2',
      cmake: '==3.6.1',
      gcc: '==8.3.0',
      git: '>=1.8.3',
      maven: '==3.3.9',
      valgrind: '>=3.9.0',
    },
    capabilities+: ['linux', 'amd64'],
  },

  ol65: self.linux + {
    capabilities+: ['ol65'],
  },

  x52: self.linux + {
    capabilities+: ['no_frequency_scaling', 'tmpfs25g', 'x52'],
  },

  sparc: common + {
    capabilities: ['solaris', 'sparcv9'],
  },

  linux_aarch64: common + {
    capabilities+: ['linux', 'aarch64'],
    packages+: {
      gcc: '==8.3.0',
    }
  },

  darwin: common + {
    environment+: {
      // for compatibility with macOS El Capitan
      MACOSX_DEPLOYMENT_TARGET: '10.11',
    },
    capabilities: ['darwin', 'amd64'],
  },

  windows: common + {
    packages+: {
      'devkit:VS2017-15.5.5+1': '==0',
    },
    downloads+: {
      NASM: {name: 'nasm', version: '2.14.02', platformspecific: true},
    },
    environment+: {
      PATH: '$PATH;$NASM',
    },
    setup+: [
      ['set-export', 'DEVKIT_ROOT', '$VS2017_15_5_5_1_0_ROOT'],
      ['set-export', 'DEVKIT_VERSION', '2017'],
    ],
    capabilities: ['windows', 'amd64'],
  },

  windows_vs2010: self.windows + {
    packages+: {
      msvc : '==10.0',
    },
  },

  local gateCmd = ['mx', '--strict-compliance', 'gate', '-B=--force-deprecation-as-warning', '--strict-mode', '--tags', '${TAGS}'],

  eclipse : {
    downloads+: {
      ECLIPSE: {name: 'eclipse', version: '4.14.0', platformspecific: true},
      JDT: {name: 'ecj', version: '4.14.0', platformspecific: false},
    },
    environment+: {
      ECLIPSE_EXE: '$ECLIPSE/eclipse',
    },
  },

  build : {
    run+: [
      ['mx', 'build', '--force-javac'],
    ],
  },

  buildCompiler : {
    run+: [
      ['mx', '--dynamicimports', '/compiler', 'build', '--force-javac'],
    ],
  },

  gateTags : self.build + {
    run+: [
      gateCmd,
    ],
    timelimit: '30:00',
  },

  gateStyleFullBuild : self.eclipse + {
    run+: [
      ['set-export', 'TAGS', 'style,fullbuild'],
      gateCmd,
    ],
    timelimit: '30:00',
  },
}
