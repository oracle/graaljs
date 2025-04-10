# pylint: disable=line-too-long
suite = {
  "mxversion" : "7.27.0",

  "name" : "graal-js",

  "version" : "25.0.0",
  "release" : False,
  "groupId" : "org.graalvm.js",
  "url" : "https://www.graalvm.org/javascript",
  "developer" : {
    "name" : "GraalVM Development",
    "email" : "graalvm-dev@oss.oracle.com",
    "organization" : "Oracle Corporation",
    "organizationUrl" : "https://www.graalvm.org/",
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
           "version" : "d838152189a8aaab0bdbe8272d23bd4d979201ff",
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

  "libraries" : {
    "NETBEANS_PROFILER" : {
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/org-netbeans-lib-profiler-8.2-201609300101.jar"],
      "digest" : "sha512:8238c5985036cdcd6a361426cfe0b980709ed4e73a46aa18fce16acf029ada23fa18a18edc54fb444fffba33f498a8582f499c0229e849c475621758c463a632",
    },

    "TESTNASHORN" : {
      "digest" : "sha512:0ccae1ea92fd8bd24ebedb0d0f77d3a81c6afb079a1a294adc296bd12d6b2c5e079b3e959cc0acb8b35b6b81a906a42f6d7a2338a6e4d33306fcfdc5fa2d462d",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/testnashorn-e118c818dbf8.tar.bz2"],
    },

    "TESTNASHORN_EXTERNAL" : {
      "digest" : "sha512:6d0ed48fd95fdd3d8e1ee5ff640dfc651438a60ac4896140d158c621963df8881af8b98dc2366605e58a218d19e87a99af21349a72ae44f805a0fc09b0018ab7",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/testnashorn-external-0f91116bb4bd.tar.bz2"],
    },

    "NASHORN_INTERNAL_TESTS" : {
      "digest" : "sha512:98eed94eabf90e4642bb0c5c2f1f025790879a03e61794c7559367cd6d9942afad32e2a24236413074b1940526ada592bb26237159332197c9d7fc6d0a5ecd7a",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/truffle/js/nashorn-internal-tests-700f5e3f5ff2.jar"],
    },

    "JACKSON_CORE" : {
      "digest" : "sha512:a8a3ddf5c8a732fc3810f9c113d88fd59bf613d15dbf9d3e24dd196b2b8c2195f4088375e3d03906f2629e62983fef3267b5478abd5ab1df733ec58cd00efae6",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-core",
        "version" : "2.15.2",
      },
      "moduleName" : "com.fasterxml.jackson.core",
    },

    "JACKSON_ANNOTATIONS" : {
      "digest" : "sha512:c9ffb4cf3e409921bca1fa6126ca8746c611042ac3fcf0e4f991d23d12b20ef0946ef1421d991ae8ed86012059df4e08fb776d96db6d13147c2ec85e22254537",
      "maven" : {
        "groupId" : "com.fasterxml.jackson.core",
        "artifactId" : "jackson-annotations",
        "version" : "2.15.2",
      },
      "moduleName" : "com.fasterxml.jackson.annotation",
    },

    "JACKSON_DATABIND" : {
      "digest" : "sha512:edf622f3d2bb2cdf308875e467f28eafdd581c6ad47992a2b49a2c803b597c7fe4330c8f887687599c8a6a529d8b11054f8b354b7ddddd2bf904ef347d4f1cd2",
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
      "checkstyleVersion" : "10.21.0",
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
      "checkstyleVersion" : "10.21.0",
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

    "com.oracle.truffle.js.fuzzillilauncher" : {
      "subDir" : "src",
      "sourceDirs": ["src"],
      "dependencies" : [
        "com.oracle.truffle.js.shell"
      ],
      "checkstyle" : "com.oracle.truffle.js",
      "spotbugs" : "true",
      "javaCompliance": "17+",
      "testProject" : True,
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
        "NETBEANS_PROFILER",
        "truffle:TRUFFLE_API",
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
        "TRUFFLE_JS_SNAPSHOT_TOOL",
      ],
      "requires" : [
        "java.scripting", # required by testnashorn
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
          "com.oracle.truffle.js.builtins.json to org.graalvm.nodejs",
          "com.oracle.truffle.js.builtins.web to org.graalvm.nodejs",
          "com.oracle.truffle.js.lang to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.access to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.array to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.arguments to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.cast to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.function to org.graalvm.nodejs",
          "com.oracle.truffle.js.nodes.promise to org.graalvm.nodejs",
          "com.oracle.truffle.js.parser to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.array to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.builtins to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.builtins.wasm to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.interop to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.objects to org.graalvm.nodejs",
          "com.oracle.truffle.js.runtime.util to org.graalvm.nodejs",
        ],
        "requires": [
          "org.graalvm.collections",
          "org.graalvm.nativeimage",
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
      "description": "GraalJS, a high-performance embeddable JavaScript runtime for Java. This artifact includes the core language runtime without standard libraries. It is not recommended to depend on the artifact directly. Instead, use \'org.graalvm.polyglot:js\' or \'org.graalvm.polyglot:js-community\' to ensure all dependencies are pulled in correctly.",
      "maven": {
        "artifactId": "js-language",
        "tag": ["default", "public"],
      },
      "license": [
        "UPL",  # Main code
        "MIT",  # JONI regexp engine
      ],
      "allowsJavadocWarnings": True,
      "useModulePath": True,
    },

    "JS_COMMUNITY": {
      "type": "pom",
      "runtimeDependencies": [
        "GRAALJS",
        "truffle:TRUFFLE_RUNTIME",
      ],
      "description":     "GraalJS, a high-performance embeddable JavaScript runtime for Java. This POM dependency includes GraalJS dependencies and Truffle Community Edition.",
      "descriptionGFTC": "GraalJS, a high-performance embeddable JavaScript runtime for Java. This POM dependency includes GraalJS dependencies and Truffle.",
      "maven": {
        "artifactId": "js-community",
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
            "com.oracle.truffle.js.shell to org.graalvm.js.fuzzillilauncher",
        ],
        "requires": [
          "org.graalvm.polyglot",
        ],
      },
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.shell"],
      "mainClass" : "com.oracle.truffle.js.shell.JSLauncher",
      "distDependencies" : [
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "description" : "GraalJS, a high-performance embeddable JavaScript runtime for Java. This artifact provides a command-line launcher for GraalJS.",
      "maven" : {
        "artifactId" : "js-launcher",
        "tag": ["default", "public"],
      },
      "allowsJavadocWarnings": True,
      "useModulePath": True,
    },

    "GRAALJS_FUZZILLI_LAUNCHER" : {
      "moduleInfo" : {
        "name" : "org.graalvm.js.fuzzillilauncher",
        "requires": [
          "org.graalvm.js.launcher",
          "org.graalvm.polyglot",
        ],
      },
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.fuzzillilauncher"],
      "mainClass" : "com.oracle.truffle.js.fuzzillilauncher.JSFuzzilliLauncher",
      "distDependencies" : [
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
        "GRAALJS_LAUNCHER",
        "GRAALJS"
      ],
      "description" : "GraalJS, a high-performance embeddable JavaScript runtime for Java. This artifact provides a command-line launcher for fuzzing GraalJS with Fuzzilli.",
      "allowsJavadocWarnings": True,
      "useModulePath": True,
      "testDistribution": True,
    },

    "GRAALJS_SCRIPTENGINE" : {
      "moduleInfo" : {
        "name" : "org.graalvm.js.scriptengine",
        "requires" : [
          "java.scripting",
          "org.graalvm.collections",
        ],
        "exports" : [
          "com.oracle.truffle.js.scriptengine",
        ],
      },
      "subDir" : "src",
      "dependencies" : ["com.oracle.truffle.js.scriptengine"],
      "distDependencies" : [
        "sdk:POLYGLOT"
      ],
      "description" : "GraalJS, a high-performance embeddable JavaScript runtime for Java. This artifact provides an implementation of javax.script.ScriptEngine (JSR-223) based on GraalJS. Note that this is provided for legacy reasons to allow easier migration for implementations currently based on a ScriptEngine. We strongly encourage users to only use \'org.graalvm.polyglot:js\' or \'org.graalvm.polyglot:js-community\' via org.graalvm.polyglot.Context to control many of the settings directly and benefit from fine-grained security settings in GraalVM.",
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
        "requires": [
          "org.graalvm.polyglot",
          "org.graalvm.truffle",
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
        "NETBEANS_PROFILER",
        "truffle:TRUFFLE_API",
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
          # This non-qualified open is needed by the LoadFromClasspathTest to allow resource loading from a module path,
          # see jdk.internal.loader.BuiltinClassLoader#isOpen
          "com.oracle.truffle.js.test.nashorn",
          "com.oracle.truffle.js.test.external.suite to com.fasterxml.jackson.databind",
        ],
        "requires": [
          "org.graalvm.collections",
          "org.graalvm.truffle",
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
      "description" : "GraalJS Tests.",
      "allowsJavadocWarnings": True,
      "useModulePath": True,
      "unittestConfig": "js",
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
        "sdk:POLYGLOT_TCK",
        "GRAALJS",
      ],
      "description" : "Truffle TCK provider for GraalJS.",
      "license": "UPL",
      "testDistribution": False,
      "maven": {
        "artifactId": "js-truffle-tck",
        "tag": ["default", "public"],
      },
      "noMavenJavadoc": True,
    },

    "GRAALJS_GRAALVM_SUPPORT" : {
      "native" : True,
      "platformDependent" : True,
      "description" : "GraalJS support distribution for GraalVM.",
      "layout" : {
        "native-image.properties": "file:mx.graal-js/native-image.properties",
        "./": [
          "file:README.md",
        ],
        "bin/": [
          "file:mx.graal-js/graalvm_launchers/<cmd:js-polyglot-get>",
        ]
      },
    },

    "GRAALJS_GRAALVM_LICENSES" : {
      "fileListPurpose": 'native-image-resources',
      "native" : True,
      "description" : "GraalJS license files for GraalVM.",
      "layout" : {
        "LICENSE_GRAALJS.txt" : "file:LICENSE_GRAALJS",
        "THIRD_PARTY_LICENSE_GRAALJS.txt": "file:THIRD_PARTY_LICENSE_GRAALJS.txt",
      },
    },

    "JS_INTEROP_MICRO_BENCHMARKS" : {
      "subDir" : "src",
      "description" : "GraalJS JMH Interop Suite.",
      "dependencies" : ["com.oracle.truffle.js.jmh"],
      "exclude" : [
        "mx:JUNIT"
      ],
      "distDependencies" : [
        "sdk:POLYGLOT",
        "GRAALJS"
      ],
      "testDistribution": True,
      "maven" : False,
    }
  }
}
