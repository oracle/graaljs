
suite = {
  "mxversion" : "5.115.0",
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
    "graalnodejs-binary-snapshots" : {
      "url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
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

  "projects" : {
    "trufflenodeNative" : {
      "dependencies" : [
        "graal-js:GRAALJS",
        "coremodules",
      ],
      "class" : "GraalNodeJsProject",
      "results" : ["Release/node"],
      "output" : "out"
    },
    "com.oracle.truffle.trufflenode" : {
      "subDir" : "mx.graal-nodejs",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "graal-js:GRAALJS",
        "sdk:LAUNCHER_COMMON",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript,NodeJS",
    },
    "com.oracle.truffle.trufflenode.jniboundaryprofiler" : {
      "subDir" : "mx.graal-nodejs",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.trufflenode"
      ],
      "checkstyle" : "com.oracle.truffle.trufflenode",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript,NodeJS",
    },
    "coremodules" : {
      "subDir" : "trufflenode",
      "buildDependencies" : [
        "graal-js:TRUFFLE_JS_SNAPSHOT_TOOL",
      ],
      "class" : "PreparsedCoreModulesProject",
      "prefix" : "",
      "outputDir" : "mxbuild/trufflenode/coremodules",
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
    "TRUFFLENODE_JNI_BOUNDARY_PROFILER" : {
      "subdir" : "mx.graal-nodejs",
      "dependencies" : ["com.oracle.truffle.trufflenode.jniboundaryprofiler"],
      "distDependencies" : [
        "TRUFFLENODE"
      ],
      "description" : "Graal Node.js JNI Boundary Profiler Agent",
      "maven" : {
        "artifactId" : "graal-nodejs-jniboundaryprofiler",
      }
    },
    "TRUFFLENODE_GRAALVM_SUPPORT" : {
      "native" : True,
      "platformDependent" : True,
      "description" : "Graal.nodejs support distribution for the GraalVM",
      "layout" : {
        "./" : ["file:deps/npm"],
        "NODE_README.md" : "file:README.md",
        "bin/" : [
          "dependency:trufflenodeNative/Release/node"
        ],
        "bin/npm" : "file:mx.graal-nodejs/graalvm_launchers/npm",
        "include/node/" : [
          "file:common.gypi",
          "file:src/node.h",
          "file:src/node_buffer.h",
          "file:src/node_object_wrap.h",
          "file:src/node_version.h",
          "file:deps/uv/include/*",
          "file:deps/v8/include/*",
        ],
        "include/src/graal/" : "file:deps/v8/src/graal/graal_handle_content.h",
      },
    },
  },
}
