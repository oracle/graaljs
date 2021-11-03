local graalJs = import 'graal-js/ci.jsonnet';
local graalNodeJs = import 'graal-nodejs/ci.jsonnet';
local common = import 'common.jsonnet';
local defs = import 'defs.jsonnet';

{
  // Used to run fewer jobs
  local debug = false,

  local overlay = 'a8924b132d15daad84eb48d7de799fc6783a3846',

  local no_overlay = 'cb733e564850cd37b685fcef6f3c16b59802b22c',

  overlay: if debug then no_overlay else overlay,

  specVersion: "2",

  local deployBinary = {
    setup+: [
      ['mx', '-p', 'graal-nodejs', 'sversions'],
      ['mx', '-p', 'graal-nodejs', 'build', '--force-javac'],
    ],
    run+: [
      ['mx', '-p', 'graal-js', 'deploy-binary-if-master', '--skip-existing', 'graaljs-lafo'],
      ['mx', '-p', 'graal-nodejs', 'deploy-binary-if-master', '--skip-existing', 'graalnodejs-lafo'],
    ],
    timelimit: '30:00',
  },

  builds: finishBuilds(graalJs.builds + graalNodeJs.builds) + [
    common.jdk8 + deployBinary + common.deploy + common.postMerge + common.ol65 + {name: 'js-deploybinary-ol65-amd64'},
    common.jdk8 + deployBinary + common.deploy + common.postMerge + common.darwin + {name: 'js-deploybinary-darwin-amd64'},
  ],

  // Set this flag to false to switch off the use of artifacts (pipelined builds).
  useArtifacts:: defs.enabled,

  jobtemplate:: {
    graalvm:: defs.ce,
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
    setup+: self.graalvm.setup,
    run+: []
      + self.export_envvars
      + self.cd_run
      + (if std.length(self.cd_run) > 0 then [['mx', 'sversions']] else []),
    timelimit: "00:30:00",
  },

  defs:: defs,
  ce:: {graalvm:: defs.ce},
  ee:: {graalvm:: defs.ee},

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

  local isBuildUsingArtifact(build) = std.objectHasAll(build, 'artifact') && build.artifact != '',

  local applyArtifact(build) =
    if isBuildUsingArtifact(build) then build + use_js_graalvm_artifact(build) else build,

  local deriveArtifactBuilds(builds) =
    local buildKey(b) = artifact_name(b.jdk, b.graalvm.edition, b.os, b.arch);
    local buildsUsingArtifact = [a for a in builds if isBuildUsingArtifact(a)];
    [build_js_graalvm_artifact(b) for b in std.uniq(std.sort(buildsUsingArtifact, keyF=buildKey), keyF=buildKey)],

  local finishBuilds(allBuilds) =
    local builds = [b for b in allBuilds if !std.objectHasAll(b, 'enabled') || b.enabled];
    if self.useArtifacts then [applyArtifact(b) for b in builds] + deriveArtifactBuilds(builds) else builds,
}
