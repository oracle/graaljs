{
  local labsjdk8 = {name: 'labsjdk', version: '8u172-jvmci-0.46', platformspecific: true},

  local oraclejdk11 = {name : 'oraclejdk', version : "11+20", platformspecific: true},

  jdk8: {
    downloads: {
      JAVA_HOME: labsjdk8,
      JDT: {name: 'ecj', version: '4.5.1', platformspecific: false},
    },
  },

  jdk11: {
    downloads: {
      EXTRA_JAVA_HOMES: labsjdk8,
      JAVA_HOME: oraclejdk11,
    },
  },

  deploy:    {targets: ['deploy']},
  gate:      {targets: ['gate']},
  postMerge: {targets: ['post-merge']},
  bench:     {targets: ['bench', 'post-merge']},

  local common = {
    packages: {
      'apache/ab': '==2.3',
      gcc: '==4.9.1',
      git: '>=1.8.3',
      maven: '==3.3.9',
      mercurial: '>=3.2.4',
      valgrind: '>=3.9.0',
      'pip:astroid': '==1.1.0',
      'pip:pylint': '==1.1.0',
    },
    catch_files+: [
      'Graal diagnostic output saved in (?P<filename>.+.zip)',
      'npm-debug.log', // created on npm errors
    ],
  },

  linux: common + {
    capabilities: ['linux', 'amd64'],
  },

  ol65: common + {
    capabilities: ['ol65', 'linux', 'amd64'],
  },

  x52: common + {
    capabilities: ['no_frequency_scaling', 'tmpfs25g', 'x52', 'linux', 'amd64'],
  },

  sparc: common + {
    capabilities: ['solaris', 'sparcv9'],
  },

  darwin: common + {
    packages: {
      'pip:astroid': '==1.1.0',
      'pip:pylint': '==1.1.0',
    },
    environment+: {
      HTTP_PROXY: "${http_proxy}",
      HTTPS_PROXY: "${https_proxy}",
      // for compatibility with macOS El Capitan
      MACOSX_DEPLOYMENT_TARGET: '10.11',
    },
    capabilities: ['darwin_sierra', 'amd64'],
  },
}
