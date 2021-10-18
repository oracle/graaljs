local common_json = (import "common.json");

{
  jdk8: {
    jdk:: 'jdk8',
    downloads+: {
      JAVA_HOME: common_json.jdks.oraclejdk8,
    },
  },

  jdk11: {
    jdk:: 'jdk11',
    downloads+: {
      JAVA_HOME: common_json.jdks["labsjdk-ce-11"],
    },
  },

  jdk17: {
    jdk:: 'jdk17',
    downloads+: {
      JAVA_HOME: common_json.jdks["labsjdk-ce-17"],
    },
  },

  deploy:      {targets+: ['deploy']},
  gate:        {targets+: ['gate']},
  postMerge:   {targets+: ['post-merge']},
  bench:       {targets+: ['bench', 'post-merge']},
  dailyBench:  {targets+: ['bench', 'daily']},
  weeklyBench: {targets+: ['bench', 'weekly']},
  manualBench: {targets+: ['bench']},
  daily:       {targets+: ['daily']},
  weekly:      {targets+: ['weekly']},

  local python3 = {
    environment+: {
      MX_PYTHON: "python3",
    },
  },

  local common = python3 + {
    packages+: {
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
    os:: 'linux',
    arch:: 'amd64',
    packages+: common_json.sulong.deps.linux.packages + {
      'apache/ab': '==2.3',
      devtoolset: '==7', # GCC 7.3.1, make 4.2.1, binutils 2.28, valgrind 3.13.0
      git: '>=1.8.3',
      maven: '==3.3.9',
    },
    capabilities+: ['linux', 'amd64'],
  },

  ol65: self.linux + {
    capabilities+: ['ol65'],
  },

  x52: self.linux + {
    capabilities+: ['no_frequency_scaling', 'tmpfs25g', 'x52'],
  },

  linux_aarch64: common + {
    arch:: 'aarch64',
    capabilities+: ['linux', 'aarch64'],
    packages+: {
      devtoolset: '==7', # GCC 7.3.1, make 4.2.1, binutils 2.28, valgrind 3.13.0
    }
  },

  darwin: common + {
    os:: 'darwin',
    arch:: 'amd64',
    packages+: common_json.sulong.deps.darwin.packages,
    environment+: {
      // for compatibility with macOS El Capitan
      MACOSX_DEPLOYMENT_TARGET: '10.11',
    },
    capabilities: ['darwin_mojave', 'amd64'],
  },

  windows: common + {
    os:: 'windows',
    arch:: 'amd64',
    capabilities: ['windows', 'amd64'],
  },

  windows_jdk17: self.windows + common_json.devkits["windows-jdk17"] + {
    setup+: [
      ['set-export', 'DEVKIT_VERSION', '2019'],
    ],
  },

 windows_jdk11: self.windows + common_json.devkits["windows-jdk11"] + {
    setup+: [
      ['set-export', 'DEVKIT_VERSION', '2017'],
    ],
  },

  windows_jdk8: self.windows + common_json.devkits["windows-oraclejdk8"] + {
    setup+: [
      ['set-export', 'DEVKIT_VERSION', '2017'],
    ],
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
