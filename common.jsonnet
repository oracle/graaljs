{
  local labsjdk8 = {name: 'oraclejdk', version: '8u251+08-jvmci-20.1-b02', platformspecific: true},

  local labsjdk_ce_11 = {name : 'labsjdk', version : 'ce-11.0.7+10-jvmci-20.1-b02', platformspecific: true},

  jdk8: {
    downloads+: {
      JAVA_HOME: labsjdk8,
      JDT: {name: 'ecj', version: '4.14.0', platformspecific: false},
    },
  },

  jdk11: {
    downloads+: {
      JAVA_HOME: labsjdk_ce_11,
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
      'mx': '5.261.3',
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
}
