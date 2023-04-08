local common = (import "ci/common.jsonnet");

{
  jdk17:: common.jdks["labsjdk-ee-17"] + {
    jdk:: 'jdk17',
  },

  jdk20:: common.jdks["labsjdk-ee-20"] + {
    jdk:: 'jdk20',
  },

  jdk21:: common.jdks["oraclejdk21"] + {
    jdk:: 'jdk21',
  },

  deploy::      {targets+: ['deploy']},
  gate::        {targets+: ['gate']},
  postMerge::   {targets+: ['post-merge']},
  bench::       {targets+: ['bench', 'post-merge']},
  dailyBench::  {targets+: ['bench', 'daily']},
  weeklyBench:: {targets+: ['bench', 'weekly']},
  manualBench:: {targets+: ['bench']},
  daily::       {targets+: ['daily']},
  weekly::      {targets+: ['weekly']},

  deps:: common.deps,

  common_deps:: common.deps.sulong + {
    catch_files+: [
      'npm-debug.log', // created on npm errors
    ],
    environment+: {
      GRAALVM_CHECK_EXPERIMENTAL_OPTIONS: "true",
    },
  },

  linux_common:: {
    packages+: {
      devtoolset: '==7', # GCC 7.3.1, make 4.2.1, binutils 2.28, valgrind 3.13.0
    },
  },

  linux_amd64:: common.linux_amd64 + self.linux_common + self.common_deps + {
    packages+: {
      'apache/ab': '==2.3',
      maven: '==3.3.9',
    },
  },

  x52:: self.linux_amd64 + {
    capabilities+: ['no_frequency_scaling', 'tmpfs25g', 'x52'],
  },

  linux_aarch64:: common.linux_aarch64 + self.linux_common + self.common_deps,

  darwin_amd64:: common.darwin_amd64 + self.common_deps + {
    environment+: {
      // for compatibility with macOS El Capitan
      MACOSX_DEPLOYMENT_TARGET: '10.11',
    },
    capabilities+: ['darwin_mojave'],
  },

  darwin_aarch64:: common.darwin_aarch64 + self.common_deps + {
    environment+: {
      // for compatibility with macOS BigSur
      MACOSX_DEPLOYMENT_TARGET: '11.0',
    },
  },

  windows_amd64:: common.windows_amd64 + self.common_deps + {
    packages+: common.devkits["windows-" + self.jdk].packages,
    local devkit_version = std.filterMap(function(p) std.startsWith(p, 'devkit:VS'), function(p) std.substr(p, std.length('devkit:VS'), 4), std.objectFields(self.packages))[0],
    environment+: {
      DEVKIT_VERSION: devkit_version,
    },
  },

  gateCmd:: ['mx', 'gate', '-B=--force-deprecation-as-warning', '-B=-A-J-Dtruffle.dsl.SuppressWarnings=truffle', '--strict-mode'],
  gateCmdWithTags:: self.gateCmd + ['--tags', '${TAGS}'],

  build:: {
    run+: [
      ['mx', 'build', '--force-javac'],
    ],
  },

  buildCompiler:: {
    run+: [
      ['mx', '--dynamicimports', '/compiler', 'build', '--force-javac'],
    ],
  },

  gateTags:: self.build + {
    run+: [
      $.gateCmdWithTags,
    ],
    timelimit: '45:00',
  },

  gateStyleFullBuild:: common.deps.pylint + common.deps.eclipse + common.deps.jdt + {
    environment+: {
      TAGS: 'style,fullbuild',
    },
    run+: [
      $.gateCmdWithTags,
    ],
    timelimit: '45:00',
  },
}
