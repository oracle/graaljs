suite = {
  "mxversion" : "5.127.1",

  "name" : "graal-js",

  "imports" : {
    "suites" : [
        {
           "name" : "truffle",
           "subdir" : True,
           "version" : "f79bb4aeb56bf080f374bfffdc5f725079a14c78",
           "urls" : [
                {"url" : "https://github.com/graalvm/graal.git", "kind" : "git"},
                {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
            ]
        },
    ],
  },

  "repositories" : {
    "graaljs-binary-snapshots" : {
      "url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
      "licenses" : ["Oracle Proprietary"]
    },
  },

  "licenses" : {
    "Oracle Proprietary" : {
      "name" : "ORACLE PROPRIETARY/CONFIDENTIAL",
      "url" : "http://www.oracle.com/us/legal/copyright/index.html"
    }
  },

  "defaultLicense" : "Oracle Proprietary",

  "javac.lint.overrides" : "none",

  "libraries" : {
    "NETBEANS_PROFILER" : {
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/org-netbeans-lib-profiler-8.2-201609300101.jar"],
      "sha1" : "4b52bd03014f6d080ef0528865c1ee50621e35c6",
    },

    "ICU4J" : {
      "sha1" : "6f06e820cf4c8968bbbaae66ae0b33f6a256b57f",
      "maven" : {
        "groupId" : "com.ibm.icu",
        "artifactId" : "icu4j",
        "version" : "59.1",
      },
    },
  },

  "projects" : {
    "com.oracle.truffle.js.runtime" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.runtime.doubleconv",
        "com.oracle.truffle.regex",
        "mx:ASM_DEBUG_ALL",
        "ICU4J",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.nodes" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.runtime",
        "com.oracle.truffle.js.annotations",
        "com.oracle.truffle.js.codec",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR", "TRUFFLE_JS_FACTORY_PROCESSOR"],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.builtins" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : ["com.oracle.truffle.js.nodes"],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.parser" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.builtins",
        "com.oracle.js.parser",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.js.parser" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "javaCompliance" : "1.8",
      "annotationProcessors" : [],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.shell" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:LAUNCHER_COMMON",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.engine" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.parser",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "annotationProcessors" : [
        "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.annotations" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.codec" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.snapshot" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.parser",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.factory.processor" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.annotations",
        "com.oracle.truffle.js.codec",
        "truffle:TRUFFLE_API",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.runtime.doubleconv" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
      ],
      "findbugs" : "false",
#     checkstyle and findbugs turned off to keep the source aligned
#     with the original nashorn version as much as possible
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.regex.nashorn" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
      ],
      "findbugs" : "false",
#     checkstyle and findbugs turned off to keep the source aligned
#     with the original nashorn version as much as possible
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,Regex",
    },

    "com.oracle.truffle.regex" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
        "sdk:GRAAL_SDK",
        "com.oracle.truffle.regex.nashorn",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,Regex",
    },

    "com.oracle.truffle.js.stats" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.shell",
        "NETBEANS_PROFILER",
        "graaljs",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.scriptengine" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.scriptengine.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.scriptengine",
        "sdk:GRAAL_SDK",
        "mx:JUNIT",
        "GRAALJS",
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "graaljs" : {
      "dependencies" : [
        "com.oracle.truffle.js.engine",
      ],
      "buildDependencies" : ["com.oracle.truffle.js.snapshot"],
      "class" : "GraalJsProject",
      "prefix" : "",
      "outputDir" : "mxbuild/graal/graaljs"
    },

    "com.oracle.truffle.js.test.tck" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT_TCK"
      ],
      "checkstyle" : "com.oracle.truffle.js.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,JavaScript,Test",
    },

    "icu4j-data": {
        "native": True,
        "class": "Icu4jDataProject",
        "outputDir": "lib/icu4j",
        "prefix": "icu4j"
    },
  },

  "distributions" : {
    "GRAALJS" : {
      "subDir" : "src",
      "dependencies" : ["graaljs"],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "sdk:GRAAL_SDK",
      ],
      "exclude": [
        "mx:ASM_DEBUG_ALL",
        "ICU4J",
      ],
      "description" : "Graal JavaScript engine",
      "maven" : {
        "artifactId" : "graal-js",
      },
    },

    "GRAALJS_LAUNCHER" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.shell"],
      "mainClass" : "com.oracle.truffle.js.shell.JSLauncher",
      "distDependencies" : ["sdk:LAUNCHER_COMMON"],
      "description" : "Graal JavaScript Launcher",
    },

    "TRUFFLE_JS_FACTORY_PROCESSOR" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.factory.processor"],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "sdk:GRAAL_SDK"
      ],
      "maven" : False,
      "overlaps" : ["GRAALJS"],
    },

    "TRUFFLE_JS_SNAPSHOT_TOOL" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.snapshot"],
      "mainClass" : "com.oracle.truffle.js.snapshot.SnapshotTool",
      "distDependencies" : [
        "GRAALJS",
      ],
      "maven" : False,
    },

    "TRUFFLE_STATS" : {
      "subDir" : "src",
      "mainClass" : "com.oracle.truffle.js.stats.heap.HeapDumpAnalyzer",
      "dependencies" : ["com.oracle.truffle.js.stats"],
      "distDependencies" : [
        "GRAALJS",
        "NETBEANS_PROFILER",
        "GRAALJS_LAUNCHER"
      ],
      "maven" : False,
    },

    "GRAALJS_SCRIPTENGINE" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.scriptengine"],
      "distDependencies" : [
        "sdk:GRAAL_SDK"
      ],
      "maven" : False,
    },

    "TRUFFLE_JS_TCK" : {
      "subDir" : "src",
      "javaCompliance" : "1.8",
      "dependencies" : [
        "com.oracle.truffle.js.test.tck"
      ],
      "exclude" : [
        "mx:JUNIT",
      ],
      "distDependencies" : [
        "sdk:POLYGLOT_TCK"
      ],
      "maven" : False
    },

    "ICU4J-DIST" : {
      "native" : True,
      "relpath" : True,
      "platformDependent" : False,
      "dependencies" : [
        "icu4j-data",
      ],
      "description" : "ICU4J localization library and data files",
    },
  }
}
