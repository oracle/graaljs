
suite = {
  "mxversion" : "7.27.0",
  "name" : "graal-nodejs",
  "versionConflictResolution" : "latest",

  "imports" : {
    "suites" : [
      {
        "name" : "graal-js",
        "subdir" : True,
      }
    ],
  },

  "developer" : {
    "name" : "GraalVM Development",
    "email" : "graalvm-dev@oss.oracle.com",
    "organization" : "Oracle Corporation",
    "organizationUrl" : "http://www.graalvm.org/",
  },
  "url" : "http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/index.html",

  "repositories" : {
    "graalnodejs-lafo" : {
      "snapshotsUrl" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
      "releasesUrl": "https://curio.ssw.jku.at/nexus/content/repositories/releases",
      "licenses" : ["UPL"]
    },
  },

  "licenses" : {
    "UPL" : {
      "name" : "Universal Permissive License, Version 1.0",
      "url" : "http://opensource.org/licenses/UPL",
    }
  },

  "defaultLicense" : "UPL",

  "libraries" : {
    "NASM" : {
      "packedResource": True,
      "os_arch" : {
        "windows" : {
          "amd64" : {
            "urls": ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/nodejs/nasm-2.14.02-windows-amd64.tar.gz"],
            "digest": "sha512:f11495fe7b3b50587161d5cc9e604770c0470aef04468865e7d775d96dbdc33903989ee76aed51ec09f923da452be50993bc7b3aa73b635fc93c7bca17807951",
          },
          "<others>": {
            "optional": True,
          }
        },
        "<others>": {
          "<others>": {
            "optional": True,
          }
        },
      },
    },
  },

  "projects" : {
    "trufflenodeNative" : {
      "dependencies" : [
        "coremodules",
      ],
      "class" : "GraalNodeJsProject",
      "output" : "out",
      "results" : [
        "Release/<exe:node>",
        "headers/include",
      ],
      "os" : {
        "windows" : {},
        "<others>" : {
          "results" : [
            "lib/<lib:jsig>",
          ],
        }
      },
    },
    "com.oracle.truffle.trufflenode" : {
      "subDir" : "mx.graal-nodejs",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "graal-js:GRAALJS",
        "sdk:LAUNCHER_COMMON",
      ],
      "requires" : [
        "jdk.unsupported",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "checkstyleVersion" : "10.7.0",
      "workingSets" : "Truffle,JavaScript,NodeJS",
    },
    "com.oracle.truffle.trufflenode.test" : {
      "subDir" : "mx.graal-nodejs",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.trufflenode"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "spotbugs" : "true",
      "checkstyle" : "com.oracle.truffle.trufflenode",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript,NodeJS",
    },
    "coremodules" : {
      "buildDependencies" : [
        "graal-js:TRUFFLE_JS_SNAPSHOT_TOOL",
      ],
      "class" : "PreparsedCoreModulesProject",
      "prefix" : "",
      "outputDir" : "out/coremodules",
    },
  },

  "distributions" : {
    "TRUFFLENODE" : {
      "moduleInfo" : {
        "name" : "org.graalvm.nodejs",
        "requires" : [
          "org.graalvm.launcher",
          "org.graalvm.collections",
          "org.graalvm.polyglot",
          "org.graalvm.truffle",
        ],
      },
      "subdir" : "mx.graal-nodejs",
      "dependencies" : ["com.oracle.truffle.trufflenode"],
      "distDependencies" : [
        "graal-js:GRAALJS",
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "description" : "Graal Node.js",
      "maven" : {
        "artifactId" : "graal-nodejs",
      }
    },
    "TRUFFLENODE_GRAALVM_SUPPORT" : {
      "native" : True,
      "platformDependent" : True,
      "description" : "Graal.nodejs support distribution for the GraalVM",
      "layout" : {
        "./" : [
          {
            "source_type": "file",
            "path": "deps/npm",
            "exclude": [
              "deps/npm/test",
              "deps/npm/docs/package-lock.json"
            ]
          },
          "dependency:trufflenodeNative/headers/include",
        ],
        "NODE_README.md" : "file:README.md",
        "native-image.properties": "file:mx.graal-nodejs/graal-nodejs-native-image.properties",
        "bin/" : [
          "dependency:trufflenodeNative/Release/<exe:node>",
          "file:mx.graal-nodejs/graalvm_launchers/<cmd:npm>",
          "file:mx.graal-nodejs/graalvm_launchers/<cmd:npx>",
        ],
        "bin/<cmd:node-polyglot-get>": "file:../graal-js/mx.graal-js/graalvm_launchers/<cmd:js-polyglot-get>",
      },
      "os" : {
        "windows" : {},
        "<others>" : {
          "layout" : {
            "lib/" : [
              "dependency:trufflenodeNative/lib/<lib:jsig>",
            ],
          },
        }
      }
    },
    "TRUFFLENODE_GRAALVM_LICENSES" : {
      "fileListPurpose": 'native-image-resources',
      "native" : True,
      "platformDependent" : False,
      "description" : "Graal.nodejs license files for the GraalVM",
      "layout" : {
        "LICENSE_GRAALNODEJS.txt" : "file:LICENSE_GRAAL_NODEJS",
        "THIRD_PARTY_LICENSE_GRAALNODEJS.txt" : "file:LICENSE",
      },
    },
    "TRUFFLENODE_TEST" : {
      "moduleInfo" : {
        "name" : "com.oracle.truffle.trufflenode.test",
        "requires": [
          "org.graalvm.js",
          "org.graalvm.polyglot",
          "org.graalvm.truffle",
        ],
      },
      "subdir" : "mx.graal-nodejs",
      "dependencies" : ["com.oracle.truffle.trufflenode.test"],
      "distDependencies" : [
        "TRUFFLENODE"
      ],
      "description" : "Graal Node.js testing",
      "maven" : {
        "artifactId" : "graal-nodejs-test",
      },
      "useModulePath": True,
    },
  },
}
