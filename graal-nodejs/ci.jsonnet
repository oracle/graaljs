local common = import '../common.jsonnet';
local ci = import '../ci.jsonnet';
local cicommon = import '../ci/common.jsonnet';

{
  local graalNodeJs = ci.jobtemplate + cicommon.deps.graalnodejs + {
    cd:: 'graal-nodejs',
    suite_prefix:: 'nodejs', # for build job names
    // increase default timelimit on windows and darwin-amd64
    timelimit: if 'os' in self && (self.os == 'windows' || (self.os == 'darwin' && self.arch == 'amd64')) then '1:30:00' else '45:00',
  },

  local ce = ci.ce,
  local ee = ci.ee,

  local vm_env = {
    // too slow on windows and darwin-amd64
    local enabled = 'os' in self && !(self.os == 'windows' || (self.os == 'darwin' && self.arch == 'amd64')),
    artifact:: if enabled then 'nodejs' else '',
    suiteimports+:: if enabled then ['vm', 'substratevm', 'tools'] else ['vm'],
    nativeimages+:: if enabled then ['lib:graal-nodejs', 'lib:jvmcicompiler'] else [], // 'js'
    build_standalones:: true,
  },

  local gateTags(tags) = common.gateTags + {
    environment+: {
      TAGS: tags,
    },
  },

  local build = {
    run+: [
      // build only if no artifact is being used to avoid rebuilding
      ['[', '${ARTIFACT_NAME}', ']', '||', 'mx', 'build', '--force-javac', '--dependencies', std.join(',', self.build_dependencies)],
    ],
  },

  local defaultGateTags = gateTags('all') + {
    local tags = if 'os' in super && super.os == 'windows' then 'windows' else 'all',
    environment+: {
      TAGS: tags,
    }
  },

  local gateVmSmokeTest = {
    run+: [
      ['set-export', 'GRAALVM_HOME', ['mx', '--quiet', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', "console.log('Hello, World!')"],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
      # standalone smoke tests
      ['set-export', 'STANDALONE_HOME', ['mx', '--quiet', 'standalone-home', 'nodejs', '--type=jvm']],
      ['${STANDALONE_HOME}/bin/node', '-e', "console.log('Hello, World!')"],
      ['${STANDALONE_HOME}/bin/npm', '--version'],
    ] + (if std.find('lib:graal-nodejs', super.nativeimages) != [] then [
      ['set-export', 'STANDALONE_HOME', ['mx', '--quiet', 'standalone-home', 'nodejs', '--type=native']],
      ['${STANDALONE_HOME}/bin/node', '-e', "console.log('Hello, World!')"],
      ['${STANDALONE_HOME}/bin/npm', '--version'],
      ['${STANDALONE_HOME}/bin/npm', '--prefix', 'test/graal', 'install'],
      ['${STANDALONE_HOME}/bin/npm', '--prefix', 'test/graal', 'test'],
    ] else []),
  },

  local gateCoverage = {
    suiteimports+:: ['wasm', 'tools'],
    coverage_gate_args:: ['--jacoco-omit-excluded', '--jacoco-relativize-paths', '--jacoco-omit-src-gen', '--jacoco-format', 'lcov', '--jacocout', 'coverage'],
    run+: [
      ['mx', 'gate', '-B=--force-deprecation-as-warning', '-B=-A-J-Dtruffle.dsl.SuppressWarnings=truffle', '--strict-mode', '--tags', 'build,coverage'] + self.coverage_gate_args,
    ],
    teardown+: [
      ['mx', 'sversions', '--print-repositories', '--json', '|', 'coverage-uploader.py', '--associated-repos', '-'],
    ],
    timelimit: '1:00:00',
  },

  local checkoutNodeJsBenchmarks = {
    setup+: [
      ['git', 'clone', '--depth', '1', ['mx', 'urlrewrite', 'https://github.com/graalvm/nodejs-benchmarks.git'], '../nodejs-benchmarks'],
    ],
  },

  local auxEngineCache = {
    graalvmtests:: '../../graalvm-tests',
    run+: [
      ['python', self.graalvmtests + '/test.py', '-g', ['mx', '--quiet', 'graalvm-home'], '--print-revisions', '--keep-on-error', 'test/graal/aux-engine-cache'],
    ],
    timelimit: '1:00:00',
  },

  local testNode(suite, part='-r0,1', max_heap='8G') = gateTags('testnode') + {
    environment+:
      {NODE_SUITE: suite} +
      (if part != '' then {NODE_PART: part} else {}) +
      (if max_heap != '' then {NODE_MAX_HEAP: max_heap} else {}),
    timelimit: '1:15:00',
  },
  local maxHeapOnWindows(max_heap) = {
    environment+: if 'os' in super && super.os == 'windows' then {
      NODE_MAX_HEAP: max_heap,
    } else {},
  },

  local buildAddons = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-addons'],
    ],
  },

  local buildNodeAPI = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-node-api-tests'],
    ],
  },

  local buildJSNativeAPI = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-js-native-api-tests'],
    ],
  },

  local parallelHttp2 = 'parallel/test-http2-.*',
  local parallelNoHttp2 = 'parallel/(?!test-http2-).*',

  local generateBuilds = ci.generateBuilds,
  local promoteToTarget = ci.promoteToTarget,
  local defaultToTarget = ci.defaultToTarget,
  local includePlatforms = ci.includePlatforms,
  local excludePlatforms = ci.excludePlatforms,
  local gateOnMain = ci.gateOnMain,

  // Style gates
  local styleBuilds = generateBuilds([
    graalNodeJs + common.gateStyleFullBuild                                                                                        + {name: 'style-fullbuild'}
  ], platforms=ci.styleGatePlatforms, defaultTarget=common.gate),

  // Builds that should run on all supported platforms
  local testingBuilds = generateBuilds([
    graalNodeJs          + build            + defaultGateTags          + {dynamicimports+:: ['/wasm']}                             + {name: 'default'} +
      promoteToTarget(common.gate, [common.jdk21 + common.linux_amd64, common.jdk21 + common.linux_aarch64, common.jdk21 + common.darwin_aarch64, common.jdk21 + common.windows_amd64]) +
      promoteToTarget(common.postMerge, [common.jdk21 + common.darwin_amd64]),

    graalNodeJs + vm_env + build            + gateVmSmokeTest                                                                 + ce + {name: 'graalvm-ce'} +
      promoteToTarget(common.gate, [ci.mainGatePlatform]) +
      promoteToTarget(common.gate, [common.jdk21 + common.darwin_aarch64, common.jdk21 + common.windows_amd64]) +
      promoteToTarget(common.postMerge, [common.jdk21 + common.darwin_amd64]),
    graalNodeJs + vm_env + build            + gateVmSmokeTest                                                                 + ee + {name: 'graalvm-ee'} +
      promoteToTarget(common.gate, [ci.mainGatePlatform]),

    graalNodeJs + vm_env + build            + auxEngineCache                                                                  + ee + {name: 'aux-engine-cache'} + gateOnMain +
      excludePlatforms([common.windows_amd64, common.darwin_amd64]), # unsupported on windows, too slow on darwin-amd64
  ] +
  // mx makeinnodeenv requires NASM on Windows.
  [gateOnMain + excludePlatforms([common.windows_amd64]) + b for b in [
    graalNodeJs          + buildAddons      + testNode('addons',        max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'addons'},
    graalNodeJs          + buildNodeAPI     + testNode('node-api',      max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'node-api'},
    graalNodeJs          + buildJSNativeAPI + testNode('js-native-api', max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'js-native-api'},
  ]] +
  [gateOnMain + promoteToTarget(common.gate, [common.jdk21 + common.windows_amd64]) + b for b in [
    graalNodeJs + vm_env + build            + testNode('async-hooks',   max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'async-hooks'},
    graalNodeJs + vm_env + build            + testNode('es-module',     max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'es-module'},
    # We run the `sequential` tests with a smaller heap because `test/sequential/test-child-process-pass-fd.js` starts 80 child processes.
    graalNodeJs + vm_env + build            + testNode('sequential',    max_heap='8G') + maxHeapOnWindows('512M')                  + {name: 'sequential'} +
      excludePlatforms([common.darwin_amd64]), # times out on darwin-amd64
  ]] +
  # too slow on darwin-amd64
  [gateOnMain + excludePlatforms([common.darwin_amd64]) + b for b in [
    graalNodeJs + vm_env + build            + testNode(parallelNoHttp2, part='-r0,5', max_heap='8G')                               + {name: 'parallel-1'},
    graalNodeJs + vm_env + build            + testNode(parallelNoHttp2, part='-r1,5', max_heap='8G')                               + {name: 'parallel-2'},
    graalNodeJs + vm_env + build            + testNode(parallelNoHttp2, part='-r2,5', max_heap='8G')                               + {name: 'parallel-3'},
    graalNodeJs + vm_env + build            + testNode(parallelNoHttp2, part='-r3,5', max_heap='8G')                               + {name: 'parallel-4'},
    graalNodeJs + vm_env + build            + testNode(parallelNoHttp2, part='-r4,5', max_heap='8G')                               + {name: 'parallel-5'},

    graalNodeJs + vm_env + build            + testNode(parallelHttp2, max_heap='8G')                                               + {name: 'parallel-http2'} +
      promoteToTarget(common.postMerge, [ci.mainGatePlatform], override=true),
  ]], defaultTarget=common.weekly),

  // Builds that only need to run on one platform
  local otherBuilds = generateBuilds([
    # Note: weekly coverage is sync'ed with the graal repo (while ondemand is not).
    graalNodeJs + common.weekly    + gateCoverage                                                                                  + {name: 'coverage'},
    graalNodeJs + common.ondemand  + gateCoverage                                                                                  + {name: 'coverage'},

  ], platforms=[ci.mainGatePlatform]),

  builds: styleBuilds + testingBuilds + otherBuilds,
}
