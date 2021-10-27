{
  // overlay enabled?
  enabled:: false,

  ce:: {
    edition:: 'ce',
    available:: true,

    graal_repo:: 'graal',
    suites:: {
      compiler:: {name:: 'compiler', dynamicimport:: '/' + self.name},
      vm:: {name:: 'vm', dynamicimport:: '/' + self.name},
      substratevm:: {name:: 'substratevm', dynamicimport:: '/' + self.name},
      tools:: {name:: 'tools', dynamicimport:: '/' + self.name},
      wasm:: {name:: 'wasm', dynamicimport:: '/' + self.name},
    },

    setup+: [
      // clone the imported revision of `graal`
      ['mx', '-p', 'graal-js', 'sforceimports'],
    ],
  },

  ee:: self.ce + {
    available: false,
  },
}
