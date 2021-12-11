
suite = {
  "mxversion" : "5.264.0",
  "name" : "graal-nodejs",
  "versionConflictResolution" : "latest",

  "imports" : {
    "suites" : [
      {
        "name" : "graal-js",
        "subdir" : True,
        "urls" : [
          {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
        ]
      }
    ],
  },

  "developer" : {
    "name" : "Graal JS developers",
    "email" : "graal_js_ww_grp@oracle.com",
    "organization" : "Graal JS",
    "organizationUrl" : "https://labs.oracle.com/pls/apex/f?p=labs:49:::::P49_PROJECT_ID:129",
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
            "sha1": "2a7caf509b5d9f56fad303538d2a5f0e783e7a1e",
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
      "results" : ["Release/<exe:node>", "headers/include"],
      "output" : "out"
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
      "javaCompliance" : "11+",
      "checkstyleVersion" : "8.8",
      "workingSets" : "Truffle,JavaScript,NodeJS",
    },
    "com.oracle.truffle.trufflenode.test" : {
      "subDir" : "mx.graal-nodejs",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.trufflenode"
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.trufflenode",
      "javaCompliance" : "11+",
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
      "subdir" : "mx.graal-nodejs",
      "dependencies" : ["com.oracle.truffle.trufflenode"],
      "distDependencies" : [
        "graal-js:GRAALJS",
        "sdk:LAUNCHER_COMMON",
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
        "LICENSE_GRAALNODEJS.txt" : "file:LICENSE_GRAAL_NODEJS",
        "THIRD_PARTY_LICENSE_GRAALNODEJS.txt" : "file:LICENSE",
        "NODE_README.md" : "file:README.md",
        "native-image.properties": "file:mx.graal-nodejs/graal-nodejs-native-image.properties",
        "bin/" : [
          "dependency:trufflenodeNative/Release/<exe:node>",
          "file:mx.graal-nodejs/graalvm_launchers/<cmd:npm>",
          "file:mx.graal-nodejs/graalvm_launchers/<cmd:npx>",
        ],
        "include/src/graal/" : "file:deps/v8/src/graal/graal_handle_content.h",
      },
    },
    "TRUFFLENODE_TEST" : {
      "subdir" : "mx.graal-nodejs",
      "dependencies" : ["com.oracle.truffle.trufflenode.test"],
      "distDependencies" : [
        "TRUFFLENODE"
      ],
      "description" : "Graal Node.js testing",
      "maven" : {
        "artifactId" : "graal-nodejs-test",
      }
    },
  },
}
