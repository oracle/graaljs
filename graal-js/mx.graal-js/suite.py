suite = {
  "mxversion" : "5.210.4",

  "name" : "graal-js",

  "version" : "20.0.0-beta.01",
  "release" : False,
  "groupId" : "org.graalvm.js",
  "url" : "http://www.graalvm.org/",
  "developer" : {
    "name" : "Truffle and Graal developers",
    "email" : "graalvm-users@oss.oracle.com",
    "organization" : "Graal",
    "organizationUrl" : "http://www.graalvm.org/",
  },
  "scm" : {
    "url" : "https://github.com/graalvm/graaljs",
    "read" : "https://github.com/graalvm/graaljs.git",
    "write" : "git@github.com:graalvm/graaljs.git",
  },

  "imports" : {
    "suites" : [
        {
           "name" : "regex",
           "subdir" : True,
           "version" : "445077de669592367c0519ed38e5e1e5cbd7267d",
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
      "licenses" : ["UPL", "MIT"]
    },
  },

  "licenses" : {
    "UPL" : { #bulk of the code
      "name" : "Universal Permissive License, Version 1.0",
      "url" : "http://opensource.org/licenses/UPL",
    },
  },

  "defaultLicense" : "UPL",

  "javac.lint.overrides" : "none",

  "libraries" : {
    "NETBEANS_PROFILER" : {
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/org-netbeans-lib-profiler-8.2-201609300101.jar"],
      "sha1" : "4b52bd03014f6d080ef0528865c1ee50621e35c6",
    },

    "ICU4J" : {
      # automatic module
      "sha1" : "7a4d00d5ec5febd252a6182e8b6e87a0a9821f81",
      "maven" : {
        "groupId" : "com.ibm.icu",
        "artifactId" : "icu4j",
        "version" : "62.1",
      },
    },

    "ASM-6.2.1" : {
      "moduleName" : "org.objectweb.asm",
      "sha1" : "c01b6798f81b0fc2c5faa70cbe468c275d4b50c7",
      "sourceSha1" : "cee28077ac7a63d3de0b205ec314d83944ff6267",
      "maven" : {
        "groupId" : "org.ow2.asm",
        "artifactId" : "asm",
        "version" : "6.2.1",
      },
    },

    "ASM_TREE-6.2.1" : {
      "moduleName" : "org.objectweb.asm.tree",
      "sha1" : "332b022092ecec53cdb6272dc436884b2d940615",
      "sourceSha1" : "072bd64989090e4ed58e4657e3d4481d96f643af",
      "maven" : {
        "groupId" : "org.ow2.asm",
        "artifactId" : "asm-tree",
        "version" : "6.2.1",
      },
      "dependencies" : [
        "ASM-6.2.1",
      ],
    },

    "ASM_ANALYSIS-6.2.1" : {
      "moduleName" : "org.objectweb.asm.tree.analysis",
      "sha1" : "e8b876c5ccf226cae2f44ed2c436ad3407d0ec1d",
      "sourceSha1" : "b0b249bd185677648692e7c57b488b6d7c2a6653",
      "maven" : {
        "groupId" : "org.ow2.asm",
        "artifactId" : "asm-analysis",
        "version" : "6.2.1",
      },
      "dependencies" : [
        "ASM_TREE-6.2.1",
      ],
    },

    "ASM_COMMONS-6.2.1" : {
      "moduleName" : "org.objectweb.asm.commons",
      "sha1" : "eaf31376d741a3e2017248a4c759209fe25c77d3",
      "sourceSha1" : "667fa0f9d370e7848b0e3d173942855a91fd1daf",
      "maven" : {
        "groupId" : "org.ow2.asm",
        "artifactId" : "asm-commons",
        "version" : "6.2.1",
      },
      "dependencies" : [
        "ASM-6.2.1",
        "ASM_TREE-6.2.1",
        "ASM_ANALYSIS-6.2.1",
      ],
    },

    "ASM_UTIL-6.2.1" : {
      "moduleName" : "org.objectweb.asm.util",
      "sha1" : "400d664d7c92a659d988c00cb65150d1b30cf339",
      "sourceSha1" : "c9f7246bf93bb0dc7fe9e7c9eca531a8fb98d252",
      "maven" : {
        "groupId" : "org.ow2.asm",
        "artifactId" : "asm-util",
        "version" : "6.2.1",
      },
      "dependencies" : [
        "ASM-6.2.1",
        "ASM_TREE-6.2.1",
        "ASM_ANALYSIS-6.2.1",
      ],
    },

    "TEST262" : {
      "sha1" : "607efaa4ea80083bf434e803b87a8e4693d185e0",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/test262-af984c01.tar.bz2"],
    },

    "TESTNASHORN" : {
      "sha1" : "1a31d35e485247e0edf2738a248e1bc2b97f1054",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/testnashorn-e118c818dbf8.tar.bz2"],
    },

    "TESTNASHORN_EXTERNAL" : {
      "sha1" : "3e3edc251d800bc74f28c78f75844c7086cb5216",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/testnashorn-external-0f91116bb4bd.tar.bz2"],
    },

    "NASHORN_INTERNAL_TESTS" : {
      "sha1" : "b5840706cc8ce639fcafeab1bc61da2d8aa37afd",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/nashorn-internal-tests-700f5e3f5ff2.jar"],
    },

    "TESTV8" : {
      "sha1" : "36d09db47c8e3183c2399826cfbf93093be1c8a5",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/testv8-20190115.tar.gz"],
    },

    "JACKSON_CORE" : {
      "sha1" : "2ef7b1cc34de149600f5e75bc2d5bf40de894e60",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-core",
        "version" : "2.8.6",
      },
    },

    "JACKSON_ANNOTATIONS" : {
      "sha1" : "9577018f9ce3636a2e1cb0a0c7fe915e5098ded5",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-annotations",
        "version" : "2.8.6",
      },
    },

    "JACKSON_DATABIND" : {
      "sha1" : "c43de61f74ecc61322ef8f402837ba65b0aa2bf4",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-databind",
        "version" : "2.8.6",
      },
    },
  },

  "projects" : {
    "com.oracle.truffle.js" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.annotations",
        "com.oracle.truffle.js.codec",
        "com.oracle.truffle.js.runtime.doubleconv",
        "regex:TREGEX",
        "com.oracle.truffle.regex.nashorn",
        "ASM-6.2.1",
        "ASM_COMMONS-6.2.1",
        "ASM_UTIL-6.2.1",
        "ICU4J",
      ],
      "exports" : ["com.oracle.truffle.js.runtime.java.adapter"],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR", "TRUFFLE_JS_FACTORY_PROCESSOR"],
      "jacoco" : "include",
      "javaCompliance" : "8+",
      "checkstyleVersion" : "8.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.builtins" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : ["com.oracle.truffle.js"],
      "exports" : [],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
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
      "exports" : ["com.oracle.truffle.js.lang to org.graalvm.truffle"],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.js.parser" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK"
      ],
      "exports" : [],
      "jacoco" : "include",
      "javaCompliance" : "8+",
      "checkstyleVersion" : "8.8",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.shell" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:LAUNCHER_COMMON",
      ],
      "exports" : [],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.annotations" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "exports" : [],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.codec" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "exports" : [],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.snapshot" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.parser",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
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
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.runtime.doubleconv" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "exports" : [],
      "jacoco" : "include",
      "findbugs" : "false",
#     checkstyle and findbugs turned off to keep the source aligned
#     with the original nashorn version as much as possible
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.regex.nashorn" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "regex:TREGEX",
      ],
      "exports" : [],
      "jacoco" : "include",
      "findbugs" : "false",
#     checkstyle and findbugs turned off to keep the source aligned
#     with the original nashorn version as much as possible
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,Regex",
    },

    "com.oracle.truffle.js.stats" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.shell",
        "NETBEANS_PROFILER",
        "com.oracle.truffle.js.builtins",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "exports" : ["com.oracle.truffle.js.test"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
        "mx:JUNIT",
        "GRAALJS",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.instrumentation" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "com.oracle.truffle.js",
        "com.oracle.truffle.js.parser",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.threading" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:GRAAL_SDK",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.scriptengine" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
      ],
      "exports" : ["com.oracle.truffle.js.scriptengine"],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
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
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.external" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "graal-js:GRAALJS",
        "mx:JUNIT",
        "JACKSON_CORE",
        "JACKSON_ANNOTATIONS",
        "JACKSON_DATABIND",
        "NASHORN_INTERNAL_TESTS",
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript,Test",
      "testProject" : True,
    },

    "icu4j-data": {
        "native": True,
        "class": "Icu4jDataProject",
        "outputDir": "lib/icu4j",
        "prefix": "icu4j"
    },

    "com.oracle.truffle.js.test.sdk" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT_TCK"
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,JavaScript,Test",
      "testProject" : True,
    },

  },

  "distributions" : {
    "GRAALJS" : {
      "moduleName" : "org.graalvm.js",
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.parser"],
      "distDependencies" : [
        "regex:TREGEX",
        "truffle:TRUFFLE_API",
        "sdk:GRAAL_SDK",
      ],
      "exclude": [
        "ASM-6.2.1",
        "ASM_TREE-6.2.1",
        "ASM_ANALYSIS-6.2.1",
        "ASM_COMMONS-6.2.1",
        "ASM_UTIL-6.2.1",
        "ICU4J",
      ],
      "description" : "Graal JavaScript engine",
      "maven" : {
        "artifactId" : "js",
      },
      "license": [
        "UPL",  # Main code
        "MIT",  # JONI regexp engine
      ],
      "allowsJavadocWarnings": True,
    },

    "GRAALJS_LAUNCHER" : {
      "moduleName" : "org.graalvm.js.launcher",
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.shell"],
      "mainClass" : "com.oracle.truffle.js.shell.JSLauncher",
      "distDependencies" : ["sdk:LAUNCHER_COMMON"],
      "description" : "Graal JavaScript Launcher",
      "maven" : {
        "artifactId" : "js-launcher",
      },
      "allowsJavadocWarnings": True,
    },

    "GRAALJS_SCRIPTENGINE" : {
      "moduleName" : "org.graalvm.js.scriptengine",
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.scriptengine"],
      "distDependencies" : [
        "sdk:GRAAL_SDK"
      ],
      "description" : "Graal JavaScript ScriptEngine",
      "maven" : {
        "artifactId" : "js-scriptengine",
      },
      "allowsJavadocWarnings": True,
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

    "GRAALJS_SCRIPTENGINE_TESTS" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.scriptengine.test"],
      "distDependencies" : [
        "mx:JUNIT",
        "sdk:GRAAL_SDK",
        "GRAALJS",
        "GRAALJS_SCRIPTENGINE",
      ],
      "maven" : False,
    },

    "TRUFFLE_JS_TESTS" : {
      "dependencies" : ["com.oracle.truffle.js.test", "com.oracle.truffle.js.test.external", "com.oracle.truffle.js.test.instrumentation", "com.oracle.truffle.js.test.threading"],
      "exclude" : [
        "mx:HAMCREST",
        "mx:JUNIT",
        "JACKSON_CORE",
        "JACKSON_ANNOTATIONS",
        "JACKSON_DATABIND",
        "NASHORN_INTERNAL_TESTS",
      ],
      "distDependencies" : ["GRAALJS"],
      "license": [
        "UPL",
      ],
      "maven" : False,
      "description" : "Graal JavaScript Tests",
      "allowsJavadocWarnings": True,
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

    "SDK_JS_TESTS" : {
      "subDir" : "src",
      "javaCompliance" : "8+",
      "dependencies" : ["com.oracle.truffle.js.test.sdk"],
      "exclude" : [
        "mx:JUNIT",
      ],
      "distDependencies" : [
        "sdk:POLYGLOT_TCK"
      ],
      "maven" : False
    },

    "GRAALJS_GRAALVM_SUPPORT" : {
      "native" : True,
      "description" : "Graal.js support distribution for the GraalVM",
      "layout" : {
        "native-image.properties": "file:mx.graal-js/native-image.properties",
        "./": "file:README.md",
      },
    },
  }
}
