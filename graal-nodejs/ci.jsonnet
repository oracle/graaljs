local common = import '../common.jsonnet';
local ci = import '../ci.jsonnet';
local defs = ci.defs;

{
  local useArtifacts = ci.useArtifacts,

  local ce = {graalvm:: defs.ce},
  local ee = {graalvm:: defs.ee},

  local jobtemplate = {
    graalvm:: defs.ce,
    enabled:: self.graalvm.available,
    suiteimports+:: [],
    nativeimages+:: [],
    dynamicimports:: [self.graalvm.suites[s].dynamicimport for s in self.suiteimports],
    local di = std.join(',', self.dynamicimports),
    local ni = std.join(',', self.nativeimages),
    envopts:: (if std.length(di) > 0 then ['--dynamicimports=' + di] else [])
            + (if std.length(ni) > 0 then ['--native-images=' + ni] else []),
    envvars:: {
      DYNAMIC_IMPORTS: di,
      NATIVE_IMAGES: ni,
    },
    export_envvars:: []
      + (if std.length(di) > 0 then [['set-export', 'DYNAMIC_IMPORTS', di]] else [])
      + (if std.length(ni) > 0 then [['set-export', 'NATIVE_IMAGES', ni]] else []),
    cd:: '',
    cd_run:: if self.cd != '' then [['cd', self.cd]] else [],
    setup+: self.graalvm.setup,
    run+: []
      + self.export_envvars
      + self.cd_run
      + (if std.length(self.cd_run) > 0 then [['mx', 'sversions']] else []),
    timelimit: "00:30:00",
  },

  local vm_env = {
    suiteimports+:: ['vm', 'substratevm', 'tools'],
    nativeimages+:: ['lib:graal-nodejs', 'lib:jvmcicompiler'], // js
  },

  local graalNodeJs = jobtemplate + {
    cd:: 'graal-nodejs',
  },

  local artifact = {
    artifact:: 'nodejs',
  },

  local artifact_name(jdk, edition, os, arch, suffix='') =
    local desc = edition + "-" + jdk + "-" + os + "-" + arch + suffix;
    "js-graalvm-" + desc,

  local build_js_graalvm_artifact(build) =
    local jdk = build.jdk;
    local edition = build.graalvm.edition;
    local os = build.os;
    local arch = build.arch;
    local os_arch = os + (if arch == 'aarch64' then '_aarch64' else '') + (if os == 'windows' then '_' + jdk else '');
    local artifactName = artifact_name(jdk, edition, os, arch);
    jobtemplate + common[jdk] + common[os_arch] + {
    graalvm:: build.graalvm,
    suiteimports:: build.suiteimports,
    nativeimages:: build.nativeimages,
    name: "build-" + artifactName,
    run+: [
      ["mx", "-p", "graal-nodejs", "sversions"],
      ["mx", "-p", "graal-nodejs", "graalvm-show"],
      ["mx", "-p", "graal-nodejs", "build"],
    ],
    publishArtifacts+: [
      {
        name: artifactName,
        dir: "../",
        patterns: [
          "*/*/mxbuild",
          "*/mxbuild",
          "*/graal-nodejs/out", # js/graal-nodejs/out
        ],
      },
    ],
    timelimit: "00:30:00"
  },

  local use_js_graalvm_artifact(build) =
    local jdk = build.jdk;
    local edition = build.graalvm.edition;
    local os = build.os;
    local arch = build.arch;
    local artifactName = artifact_name(jdk, edition, os, arch);
    {
    environment+: {
      ARTIFACT_NAME: artifactName
    },
    requireArtifacts+: [
      {
        name: artifactName,
        dir: "../",
        autoExtract: false,
      },
    ],
    setup+: [
      ["unpack-artifact", "${ARTIFACT_NAME}"],
    ],
  },

  local gateTags(tags) = common.gateTags + {
    environment+: {
      TAGS: tags,
    },
  },

  local build = {
    run+: [
      ['[', '${ARTIFACT_NAME}', ']', '||', 'mx', 'build', '--force-javac'], // build only if no artifact is being used
    ],
  },

  local gateSubstrateVmSmokeTest = {
    run+: [
      ['mx', '--env', 'svm', 'build'],
      ['set-export', 'GRAALVM_HOME', ['mx', '--quiet', '--env', 'svm', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', 'console.log(\'Hello, World!\')'],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
    ],
    timelimit: '30:00',
  },

  local gateVmSmokeTest = build + {
    run+: [
      ['set-export', 'GRAALVM_HOME', ['mx', '--quiet', 'graalvm-home']],
      ['${GRAALVM_HOME}/bin/node', '-e', 'console.log(\'Hello, World!\')'],
      ['${GRAALVM_HOME}/bin/npm', '--version'],
    ],
    timelimit: '30:00',
  },

  local testNode(suite, part='-r0,1', max_heap='8G') = {
    environment+: {
      SUITE: suite,
      PART: part,
      MAX_HEAP: max_heap,
    },
    run+: [
      ['mx', 'graalvm-show'],
      ['mx', 'testnode', '-Xmx${MAX_HEAP}', '${SUITE}', '${PART}'],
    ],
    timelimit: '1:15:00',
  },

  local buildAddons = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-addons'],
    ],
    timelimit: '30:00',
  },

  local buildNodeAPI = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-node-api-tests'],
    ],
    timelimit: '30:00',
  },

  local buildJSNativeAPI = build + {
    run+: [
      ['mx', 'makeinnodeenv', 'build-js-native-api-tests'],
    ],
    timelimit: '30:00',
  },

  local parallelHttp2 = 'parallel/test-http2-.*',
  local parallelNoHttp2 = 'parallel/(?!test-http2-).*',

  local isBuildUsingArtifact(build) = std.objectHasAll(build, 'artifact') && artifact != '',

  local applyArtifact(build) =
    if isBuildUsingArtifact(build) then build + use_js_graalvm_artifact(build) else build,

  local deriveArtifactBuilds(builds) =
    local buildKey(b) = artifact_name(b.jdk, b.graalvm.edition, b.os, b.arch);
    local buildsUsingArtifact = [a for a in builds if isBuildUsingArtifact(a)];
    [build_js_graalvm_artifact(b) for b in std.uniq(std.sort(buildsUsingArtifact, keyF=buildKey), keyF=buildKey)],

  local finishBuilds(allBuilds) =
    local builds = [b for b in allBuilds if b.enabled];
    if useArtifacts then [applyArtifact(b) for b in builds] + deriveArtifactBuilds(builds) else builds,

  builds: finishBuilds([
    // gates
    graalNodeJs + common.jdk8  + common.gate      + common.linux                             + common.gateStyleFullBuild                                                            + {name: 'nodejs-gate-style-fullbuild-jdk8-linux-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.linux                             + common.gateStyleFullBuild                                                            + {name: 'nodejs-gate-style-fullbuild-jdk11-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux                             + common.gateStyleFullBuild                                                            + {name: 'nodejs-gate-style-fullbuild-jdk17-linux-amd64'},

    graalNodeJs + common.jdk11 + common.gate      + common.linux                             + gateTags('all')                                                                      + {name: 'nodejs-gate-jdk11-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux                             + gateTags('all')                                                                 + ee + {name: 'nodejs-gate-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux_aarch64                     + gateTags('all')                                                                      + {name: 'nodejs-gate-jdk17-linux-aarch64'},
    graalNodeJs + common.jdk11 + common.gate      + common.darwin                            + gateTags('all')                                                                      + {name: 'nodejs-gate-jdk11-darwin-amd64', timelimit: '55:00'},
    graalNodeJs + common.jdk17 + common.gate      + common.darwin                            + gateTags('all')                                                                      + {name: 'nodejs-gate-jdk17-darwin-amd64', timelimit: '55:00'},
    graalNodeJs + common.jdk11 + common.gate      + common.windows_jdk11                     + gateTags('windows')                                                                  + {name: 'nodejs-gate-jdk11-windows-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.windows_jdk17                     + gateTags('windows')                                                                  + {name: 'nodejs-gate-jdk17-windows-amd64'},

    graalNodeJs + common.jdk11 + common.gate      + common.linux                             + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk11-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux                             + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk17-linux-amd64'},
    graalNodeJs + common.jdk11 + common.gate      + common.darwin                            + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk11-darwin-amd64', timelimit: '55:00'},
    graalNodeJs + common.jdk17 + common.gate      + common.darwin                            + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk17-darwin-amd64', timelimit: '55:00'},
    graalNodeJs + common.jdk11 + common.gate      + common.windows_jdk11                     + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk11-windows-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.windows_jdk17                     + gateSubstrateVmSmokeTest                                                             + {name: 'nodejs-gate-substratevm-jdk17-windows-amd64'},

    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env                    + gateVmSmokeTest                                                    + artifact   + ce + {name: 'nodejs-gate-substratevm-ce-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env                    + gateVmSmokeTest                                                    + artifact   + ee + {name: 'nodejs-gate-substratevm-ee-jdk17-linux-amd64'},

    graalNodeJs + common.jdk17 + common.gate      + common.linux          + buildAddons      + testNode('addons',        part='-r0,1', max_heap='8G')                               + {name: 'nodejs-gate-addons-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux          + buildNodeAPI     + testNode('node-api',      part='-r0,1', max_heap='8G')                               + {name: 'nodejs-gate-node-api-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux          + buildJSNativeAPI + testNode('js-native-api', part='-r0,1', max_heap='8G')                               + {name: 'nodejs-gate-js-native-api-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode('async-hooks',   part='-r0,1', max_heap='8G')             + artifact        + {name: 'nodejs-gate-async-hooks-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode('es-module',     part='-r0,1', max_heap='8G')             + artifact        + {name: 'nodejs-gate-es-module-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode('sequential',    part='-r0,1', max_heap='8G')             + artifact        + {name: 'nodejs-gate-sequential-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode(parallelNoHttp2, part='-r0,5', max_heap='8G')             + artifact        + {name: 'nodejs-gate-parallel-1-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode(parallelNoHttp2, part='-r1,5', max_heap='8G')             + artifact        + {name: 'nodejs-gate-parallel-2-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode(parallelNoHttp2, part='-r2,5', max_heap='8G')             + artifact        + {name: 'nodejs-gate-parallel-3-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode(parallelNoHttp2, part='-r3,5', max_heap='8G')             + artifact        + {name: 'nodejs-gate-parallel-4-jdk17-linux-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.linux + vm_env + build            + testNode(parallelNoHttp2, part='-r4,5', max_heap='8G')             + artifact        + {name: 'nodejs-gate-parallel-5-jdk17-linux-amd64'},

    graalNodeJs + common.jdk17 + common.gate      + common.windows_jdk17  + build            + testNode('async-hooks',   part='-r0,1', max_heap='8G')                               + {name: 'nodejs-gate-async-hooks-jdk17-windows-amd64'},
    graalNodeJs + common.jdk17 + common.gate      + common.windows_jdk17  + build            + testNode('es-module',     part='-r0,1', max_heap='8G')                               + {name: 'nodejs-gate-es-module-jdk17-windows-amd64'},
    # We run the `sequential` tests with a smaller heap because `test/sequential/test-child-process-pass-fd.js` starts 80 child processes.
    graalNodeJs + common.jdk17 + common.gate      + common.windows_jdk17  + build            + testNode('sequential',    part='-r0,1', max_heap='512M')                             + {name: 'nodejs-gate-sequential-jdk17-windows-amd64'},

    // post-merges
    graalNodeJs + common.jdk17 + common.postMerge + common.linux + vm_env + build            + testNode(parallelHttp2,   part='-r0,1', max_heap='8G')                               + {name: 'nodejs-postmerge-parallel-http2-jdk8-linux-amd64'},
  ]),
}
