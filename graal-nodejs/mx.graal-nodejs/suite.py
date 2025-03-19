
suite = {
  "mxversion" : "7.45.0",
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
      "checkstyleVersion" : "10.21.0",
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

    "graalnodejs_licenses": {
      "class": "StandaloneLicenses",
      "community_license_file": "LICENSE_GRAAL_NODEJS",
      "community_3rd_party_license_file": "LICENSE",
    },

    "libgraal-nodejs": {
      "class": "NativeImageLibraryProject",
      "dependencies": [
        "GRAALNODEJS_STANDALONE_DEPENDENCIES",
      ],
      "build_args": [
        # Also set in AbstractLanguageLauncher but better to be explicit
        "-R:+EnableSignalHandling",
        "-R:+InstallSegfaultHandler",
        # From mx_graal_nodejs.py
        "-Dgraalvm.libpolyglot=true",  # `lib:graal-nodejs` should be initialized like `lib:polyglot` (GR-10038)
        # From mx.graal-nodejs/native-image.properties
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
        # Configure home
        "-Dorg.graalvm.launcher.relative.js.home=..",
      ],
      "dynamicBuildArgs": "libgraalnodejs_build_args",
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
      "maven" : False
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

    "GRAALNODEJS_STANDALONE_DEPENDENCIES": {
      "description": "Graal.nodejs standalone dependencies",
      "class": "DynamicPOMDistribution",
      "distDependencies": [
        "graal-nodejs:TRUFFLENODE",
        "sdk:TOOLS_FOR_STANDALONE",
      ],
      "dynamicDistDependencies": "graalnodejs_standalone_deps",
      "maven": False,
    },

    "GRAALNODEJS_STANDALONE_COMMON": {
      "description": "Common layout for Native and JVM standalones",
      "type": "dir",
      "platformDependent": True,
      "platforms": "local",
      "layout": {
        "./": [
          "extracted-dependency:TRUFFLENODE_GRAALVM_SUPPORT",
          "dependency:graalnodejs_licenses/*",
        ],
        "release": "dependency:sdk:STANDALONE_JAVA_HOME/release",
      },
    },

    "GRAALNODEJS_NATIVE_STANDALONE": {
      "description": "Graal.nodejs Native standalone",
      "type": "dir",
      "platformDependent": True,
      "platforms": "local",
      "layout": {
        "./": [
          "dependency:GRAALNODEJS_STANDALONE_COMMON/*",
        ],
        "lib/<lib:graal-nodejs>": "dependency:libgraal-nodejs",
      },
    },

    "GRAALNODEJS_JVM_STANDALONE": {
      "description": "Graal.nodejs JVM standalone",
      "type": "dir",
      "platformDependent": True,
      "platforms": "local",
      "layout": {
        "./": [
          "dependency:GRAALNODEJS_STANDALONE_COMMON/*",
        ],
        "jvm/": {
          "source_type": "dependency",
          "dependency": "sdk:STANDALONE_JAVA_HOME",
          "path": "*",
          "exclude": [
            # Native Image-related
            "bin/native-image*",
            "lib/static",
            "lib/svm",
            "lib/<lib:native-image-agent>",
            "lib/<lib:native-image-diagnostics-agent>",
            # Unnecessary and big
            "lib/src.zip",
            "jmods",
          ],
        },
        "jvmlibs/": [
          "extracted-dependency:truffle:TRUFFLE_ATTACH_GRAALVM_SUPPORT",
        ],
        "modules/": [
          {
            "source_type": "classpath-dependencies",
            "dependencies": [
              "GRAALNODEJS_STANDALONE_DEPENDENCIES",
              "sdk:MAVEN_DOWNLOADER",
            ],
          },
        ],
      },
    },

    "GRAALNODEJS_NATIVE_STANDALONE_RELEASE_ARCHIVE": {
        "class": "DeliverableStandaloneArchive",
        "platformDependent": True,
        "standalone_dist": "GRAALNODEJS_NATIVE_STANDALONE",
        "community_archive_name": "graalnodejs-community",
        "enterprise_archive_name": "graalnodejs",
    },

    "GRAALNODEJS_JVM_STANDALONE_RELEASE_ARCHIVE": {
        "class": "DeliverableStandaloneArchive",
        "platformDependent": True,
        "standalone_dist": "GRAALNODEJS_JVM_STANDALONE",
        "community_archive_name": "graalnodejs-community-jvm",
        "enterprise_archive_name": "graalnodejs-jvm",
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
      "maven" : False,
      "useModulePath": True,
    },
  },
}
