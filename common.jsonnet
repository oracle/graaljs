{
  local labsjdk8 = {name: 'labsjdk', version: '8u141-jvmci-0.36', platformspecific: true},

  local labsjdk9 = {name: 'labsjdk', version: '9+181', platformspecific: true},

  jdk8: {
    downloads: {
      JAVA_HOME: labsjdk8,
      JDT: {name: 'ecj', version: '4.5.1', platformspecific: false},
    },
  },

  jdk9: {
    downloads: {
      EXTRA_JAVA_HOMES: labsjdk8,
      JAVA_HOME: labsjdk9,
    },
  },

  deploy:    {targets: ['deploy']},
  gate:      {targets: ['gate']},
  postMerge: {targets: ['post-merge']},

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
    capabilities: ['linux', 'amd64']
  },

  ol65:  common + {
    capabilities: ['ol65', 'amd64']
  },

  sparc: common + {
    capabilities: ['solaris', 'sparcv9']
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
    capabilities: ['darwin_sierra', 'amd64']
  },
}
