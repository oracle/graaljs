local common = import 'common.jsonnet';
local graalJs = import 'graal-js/ci.jsonnet';
local graalNodeJs = import 'graal-nodejs/ci.jsonnet';

{
  // Used to run fewer jobs
  local useOverlay = true,

  local overlay = '94efee6144aa705d60b8c2ecf10c7887bcc21478',

  local no_overlay = 'cb733e564850cd37b685fcef6f3c16b59802b22c',

  overlay: if useOverlay then overlay else no_overlay,

  specVersion: "3",

  builds: graalJs.builds + graalNodeJs.builds,

  // Set this flag to false to switch off the use of artifacts (pipelined builds).
  useArtifacts:: useOverlay,

  jobtemplate:: {
    defs:: $.defs,
    graalvm:: self.defs.ce,
    enabled:: self.graalvm.available,
    suiteimports+:: [],
    nativeimages+:: [],
    extraimagebuilderarguments+:: [],
    dynamicimports:: [self.graalvm.suites[s].dynamicimport for s in self.suiteimports],
    local di = std.join(',', self.dynamicimports),
    local ni = std.join(',', self.nativeimages),
    local eiba = std.join(' ', self.extraimagebuilderarguments),
    envopts:: (if std.length(di) > 0 then ['--dynamicimports=' + di] else [])
            + (if std.length(ni) > 0 then ['--native-images=' + ni] + ['--extra-image-builder-argument=' + a for a in self.extraimagebuilderarguments] else []),
    envvars:: {
      DYNAMIC_IMPORTS: di,
      NATIVE_IMAGES: ni,
      EXTRA_IMAGE_BUILDER_ARGUMENTS: eiba,
    },
    export_envvars:: [['set-export', key, self.envvars[key]] for key in std.objectFields(self.envvars) if std.length(self.envvars[key]) > 0],
    cd:: '',
    cd_run:: if self.cd != '' then [['cd', self.cd]] else [],
    graalvmtests:: '',
    graalvmtests_run:: if self.graalvmtests != '' then [
      ['git', 'clone', ['mx', 'urlrewrite', 'https://github.com/graalvm/graalvm-tests.git'], self.graalvmtests],
      ['git', '-C', self.graalvmtests, 'checkout', '75b6a9e16ebbfd8b9b0a24e4be7c4378e3281204'],
    ] else [],
    using_artifact:: false,
    setup+: self.graalvm.setup,
    run+: []
      + self.export_envvars
      + self.cd_run
      + self.graalvmtests_run
      + (if std.length(self.cd_run) > 0 then [['mx', 'sversions']] else []),
    timelimit: error "timelimit not set for '" + (if std.objectHasAll(self, 'name') then self.name else '') + "'",
  },

  defs:: {
    ce:: {
      edition:: 'ce',
      available:: true,

      graal_repo:: 'graal',
      suites:: {
        compiler:: {name:: 'compiler', dynamicimport:: '/' + self.name},
        vm:: {name:: 'vm', dynamicimport:: '/' + self.name},
        substratevm:: {name:: 'substratevm', dynamicimport:: '/' + self.name},
        tools:: {name:: 'tools', dynamicimport:: '/' + self.name},
        wasm:: {name:: 'wasm', dynamicimport:: '/' + self.name},
      },

      setup+: [
        // clone the imported revision of `graal`
        ['mx', '-p', 'graal-js', 'sforceimports'],
      ],
    },

    ee:: self.ce + {
      available:: false,
    },
  },

  ce: {defs:: $.defs, graalvm:: self.defs.ce},
  ee: {defs:: $.defs, graalvm:: self.defs.ee},

  local artifact_name(jdk, edition, os, arch, prefix='js', suffix='') =
    assert prefix != '' && edition != '' && jdk != '' && os != '' && arch != '';
    local parts = [prefix, 'graalvm-' + edition, jdk, os, arch, suffix];
    std.join('-', std.filter(function(part) part != '', parts)),

  local artifact_name_from_build(b) =
    artifact_name(b.jdk, b.graalvm.edition, b.os, b.arch, b.artifact),

  local build_js_graalvm_artifact(build) =
    local jdk = build.jdk;
    local os = build.os;
    local arch = build.arch;
    local os_arch = os + '_' + arch;
    local artifactName = artifact_name_from_build(build);
    self.jobtemplate + common[jdk] + common[os_arch] + {
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
    timelimit: "40:00"
  },

  local use_js_graalvm_artifact(build) =
    local jdk = build.jdk;
    local os = build.os;
    local arch = build.arch;
    local artifactName = artifact_name_from_build(build);
    {
    using_artifact:: true,
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

  local deriveArtifactBuilds(builds) =
    local isBuildDeclaringArtifact(build) = std.objectHasAll(build, 'artifact') && build.artifact != '';
    local buildsDeclaringArtifact = [a for a in builds if isBuildDeclaringArtifact(a)];
    // Do not build artifacts that would only be used by a single job per target group.
    local artifactUseCountKey(b) = artifact_name_from_build(b) + (if 'targets' in b then ":" + std.join('-', std.sort(b.targets)) else '');
    local artifactUseCount(b) = std.count(std.map(function(x) artifactUseCountKey(x) == artifactUseCountKey(b), buildsDeclaringArtifact), true);
    local shouldUseArtifact(build) = isBuildDeclaringArtifact(build) && artifactUseCount(build) > 1;
    local applyArtifact(build) = if shouldUseArtifact(build) then build + use_js_graalvm_artifact(build) else build;
    local modifiedBuilds = [applyArtifact(b) for b in builds];
    // Derive builds for artifacts that are actually used.
    local buildsUsingArtifact = [a for a in modifiedBuilds if 'using_artifact' in a && a.using_artifact];
    local uniqueBuildArtifacts = std.uniq(std.sort(buildsUsingArtifact, keyF=artifact_name_from_build), keyF=artifact_name_from_build);
    modifiedBuilds + [build_js_graalvm_artifact(b) for b in uniqueBuildArtifacts],

  local finishBuilds(allBuilds) =
    local builds = [b for b in allBuilds if !std.objectHasAll(b, 'enabled') || b.enabled];
    if self.useArtifacts then deriveArtifactBuilds(builds) else builds,

  finishBuilds:: finishBuilds,
}
