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

  common: {
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
    catch_files: [
      'Graal diagnostic output saved in (?P<filename>.+.zip)',
      'npm-debug.log', // created on npm errors
    ],
    timelimit: '30:00',
  },

  deploy:    {targets: ['deploy']},
  gate:      {targets: ['gate']},
  postMerge: {targets: ['post-merge']},

  linux: {capabilities: ['linux', 'amd64']},
  ol65:  {capabilities: ['ol65', 'amd64']},
  sparc: {capabilities: ['solaris', 'sparcv9']},
}
