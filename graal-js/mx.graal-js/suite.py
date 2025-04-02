suite = {
  "mxversion" : "6.41.0",

  "name" : "graal-js",

  "version" : "23.1.7",
  "release" : True,
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
           "version" : "f795ef233f3e325aab46db7241f367b6c88de1b0",
           "urls" : [
                {"url" : "https://github.com/oracle/graal.git", "kind" : "git"},
            ]
        },
    ],
  },

  "repositories" : {
    "graaljs-lafo" : {
      "snapshotsUrl" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
      "releasesUrl": "https://curio.ssw.jku.at/nexus/content/repositories/releases",
      "licenses" : ["UPL", "MIT", "GPLv2-CPE"]
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

    "JACKSON_CORE" : {
      "sha1" : "a6fe1836469a69b3ff66037c324d75fc66ef137c",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-core",
        "version" : "2.15.2",
      },
      "moduleName" : "com.fasterxml.jackson.core",
    },

    "JACKSON_ANNOTATIONS" : {
      "sha1" : "4724a65ac8e8d156a24898d50fd5dbd3642870b8",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-annotations",
        "version" : "2.15.2",
      },
      "moduleName" : "com.fasterxml.jackson.annotation",
    },

    "JACKSON_DATABIND" : {
      "sha1" : "9353b021f10c307c00328f52090de2bdb4b6ff9c",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-databind",
        "version" : "2.15.2",
      },
      "moduleName" : "com.fasterxml.jackson.databind",
    },
  },

  "projects" : {
    "com.oracle.truffle.js" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.js.parser",
        "com.oracle.truffle.js.annotations",
        "com.oracle.truffle.js.codec",
        "com.oracle.truffle.js.runtime.doubleconv",
        "truffle:TRUFFLE_ICU4J",
      ],
      "requires" : [
        "java.management",
        "jdk.management",
        "jdk.unsupported",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR", "TRUFFLE_JS_FACTORY_PROCESSOR"],
      "jacoco" : "include",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "checkstyleVersion" : "10.7.0",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.parser" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.js.parser" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "jacoco" : "include",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "checkstyleVersion" : "10.7.0",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.shell" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.annotations" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.codec" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
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
      "spotbugs" : "true",
      "javaCompliance" : "17+",
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
      "requires": [
        "java.compiler",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.runtime.doubleconv" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "jacoco" : "include",
      "spotbugs" : "false",
#     checkstyle and spotbugs turned off to keep the source aligned
#     with the original nashorn version as much as possible
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.stats" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.shell",
        "NETBEANS_PROFILER",
        "com.oracle.truffle.js",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:POLYGLOT",
        "mx:JUNIT",
        "GRAALJS",
        "com.oracle.truffle.js.snapshot",
      ],
      "requires" : [
        "java.desktop",
        "jdk.unsupported",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.debug" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:POLYGLOT",
        "mx:JUNIT",
        "GRAALJS",
        "TRUFFLE_JS_TESTS",
        "truffle:TRUFFLE_TCK",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
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
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.threading" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.fetch" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT",
        "com.oracle.truffle.js.test",
      ],
      "requires" : [
        "java.net.http",
        "jdk.httpserver",
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.scriptengine" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:POLYGLOT",
      ],
      "requires" : [
        "java.scripting",
      ],
      "jacoco" : "include",
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
    },

    "com.oracle.truffle.js.scriptengine.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.scriptengine",
        "sdk:POLYGLOT",
        "mx:JUNIT",
        "GRAALJS",
      ],
      "requires" : [
        "java.scripting",
        "java.desktop",
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript",
      "testProject" : True,
    },

    "com.oracle.truffle.js.jmh" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:POLYGLOT",
        "GRAALJS",
        "mx:JMH_1_21"
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "annotationProcessors" : ["mx:JMH_1_21"],
      "spotbugsIgnoresGenerated" : True,
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
        "TRUFFLE_JS_SNAPSHOT_TOOL",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript,Test",
      "testProject" : True,
    },

    "com.oracle.truffle.js.test.sdk" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT_TCK"
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,JavaScript,Test",
      "testProject" : True,
    },

    "com.oracle.truffle.js.isolate": {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "spotbugs" : "true",
      "javaCompliance" : "17+",
      "checkstyle" : "com.oracle.truffle.js",
      "workingSets" : "Truffle,JavaScript",
    },
  },

  "distributions" : {
    "GRAALJS" : {
      "moduleInfo" : {
        "name" : "org.graalvm.js",
        "exports" : [
          "com.oracle.truffle.js.lang to org.graalvm.truffle",
          "com.oracle.js.parser to org.graalvm.nodejs",
          "com.oracle.js.parser.ir to org.graalvm.nodejs",
          "com.oracle.truffle.js.builtins to org.graalvm.nodejs",
          "com.oracle.truffle.js.builtins.helper to org.graalvm.nodejs",
          "com.oracle.truffle.js.lang to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.access to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.arguments to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.cast to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.function to org.graalvm.nodejs",
          "com.oracle.truffle.js.parser to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.array to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.builtins to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.builtins.wasm to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.interop to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.objects to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.util to org.graalvm.nodejs",
        ],
      },
      "subDir" : "src",
      "dependencies" : [
        "com.oracle.truffle.js",
        "com.oracle.truffle.js.parser",
      ],
      "distDependencies" : [
        "regex:TREGEX",
        "truffle:TRUFFLE_API",
        "sdk:POLYGLOT",
        "truffle:TRUFFLE_ICU4J",
      ],
      "exclude" : [
      ],
      "description" : "Graal JavaScript implementation",
      "maven" : {
        "artifactId" : "js-language",
        "tag": ["default", "public"],
      },
      "license": [
        "UPL",  # Main code
        "MIT",  # JONI regexp engine
      ],
      "allowsJavadocWarnings": True,
      "useModulePath": True,
    },

    "JS_COMMUNITY" : {
      "type":"pom",
      "runtimeDependencies" : [
        "GRAALJS",
        "truffle:TRUFFLE_RUNTIME",
      ],
      "description" : "Graal JavaScript engine.",
      "maven" : {
        "groupId": "org.graalvm.polyglot",
        "artifactId" : "js-community",
        "tag": ["default", "public"],
      },
      "license": [
        "UPL",  # Main code
        "MIT",  # JONI regexp engine
      ],
    },

    "GRAALJS_LAUNCHER" : {
      "moduleInfo" : {
        "name" : "org.graalvm.js.launcher",
        "exports" : [
            "com.oracle.truffle.js.shell to org.graalvm.launcher",
        ],
      },
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.shell"],
      "mainClass" : "com.oracle.truffle.js.shell.JSLauncher",
      "distDependencies" : [
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "description" : "Graal JavaScript Launcher",
      "maven" : {
        "artifactId" : "js-launcher",
        "tag": ["default", "public"],
      },
      "allowsJavadocWarnings": True,
    },

    "GRAALJS_SCRIPTENGINE" : {
      "moduleInfo" : {
        "name" : "org.graalvm.js.scriptengine",
        "requires" : ["java.scripting"],
        "exports" : [
          "com.oracle.truffle.js.scriptengine",
        ],
      },
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.scriptengine"],
      "distDependencies" : [
        "sdk:POLYGLOT"
      ],
      "description" : "Graal JavaScript ScriptEngine",
      "maven" : {
        "artifactId" : "js-scriptengine",
        "tag": ["default", "public"],
      },
      "allowsJavadocWarnings": True,
    },

    "TRUFFLE_JS_FACTORY_PROCESSOR" : {
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.factory.processor"],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "sdk:POLYGLOT"
      ],
      "maven" : False,
      "overlaps" : ["GRAALJS"],
    },

    "TRUFFLE_JS_SNAPSHOT_TOOL" : {
      "moduleInfo" : {
        "name" : "com.oracle.truffle.js.snapshot",
        "exports" : [
          "com.oracle.truffle.js.snapshot",
        ],
      },
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
        "sdk:POLYGLOT",
        "GRAALJS",
        "GRAALJS_SCRIPTENGINE",
      ],
      "maven" : False,
    },

    "TRUFFLE_JS_TESTS" : {
      "moduleInfo" : {
        "name" : "com.oracle.truffle.js.test",
        "exports" : [
          # Export everything to junit and dependent test distributions.
          "com.oracle.truffle.js.test*",
        ],
        "opens" : [
          "com.oracle.truffle.js.test.external.suite to com.fasterxml.jackson.databind",
        ],
      },
      "dependencies" : [
        "com.oracle.truffle.js.test",
        "com.oracle.truffle.js.test.external",
        "com.oracle.truffle.js.test.fetch",
        "com.oracle.truffle.js.test.instrumentation",
        "com.oracle.truffle.js.test.threading",
      ],
      "exclude" : [
        "mx:HAMCREST",
        "mx:JUNIT",
        "JACKSON_CORE",
        "JACKSON_ANNOTATIONS",
        "JACKSON_DATABIND",
      ],
      "distDependencies" : [
        "GRAALJS",
        "TRUFFLE_JS_SNAPSHOT_TOOL",
      ],
      "license": [
        "UPL",
      ],
      "maven" : False,
      "description" : "Graal JavaScript Tests",
      "allowsJavadocWarnings": True,
      "useModulePath": True,
    },

    "JS_DEBUG_TESTS" : {
      "subDir" : "src",
      "javaCompliance" : "17+",
      "dependencies" : [
        "com.oracle.truffle.js.test.debug",
      ],
      "exclude" : [
        "mx:HAMCREST",
        "mx:JUNIT",
      ],
      "distDependencies" : [
        "GRAALJS",
        "TRUFFLE_JS_TESTS",
        "sdk:POLYGLOT_TCK",
        "truffle:TRUFFLE_TCK",
      ],
      "maven" : False
    },

    "SDK_JS_TESTS" : {
      "subDir" : "src",
      "javaCompliance" : "17+",
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
        "./": [
          "file:README.md",
        ],
      },
    },

    "GRAALJS_GRAALVM_LICENSES" : {
      "fileListPurpose": 'native-image-resources',
      "native" : True,
      "description" : "Graal.js license files for the GraalVM",
      "layout" : {
        "LICENSE_GRAALJS.txt" : "file:LICENSE_GRAALJS",
        "THIRD_PARTY_LICENSE_GRAALJS.txt": "file:THIRD_PARTY_LICENSE_GRAALJS.txt",
      },
    },

    "JS_INTEROP_MICRO_BENCHMARKS" : {
      "subDir" : "src",
      "description" : "Graal.js JMH Interop Suite",
      "dependencies" : ["com.oracle.truffle.js.jmh"],
      "exclude" : [
        "mx:JUNIT"
      ],
      "distDependencies" : [
        "sdk:POLYGLOT",
        "GRAALJS"
      ],
      "maven" : False,
    }
  }
}
