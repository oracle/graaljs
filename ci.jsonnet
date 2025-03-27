local common = import 'common.jsonnet';
local graalJs = import 'graal-js/ci.jsonnet';
local graalNodeJs = import 'graal-nodejs/ci.jsonnet';

{
  // Used to run fewer jobs
  local useOverlay = true,

  local overlay = 'ac2b03008a765064fba41da97cbcd096f6d19809',

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
    local has_svm = std.find('substratevm', self.suiteimports) != [],
    export_envvars:: [['set-export', key, self.envvars[key]] for key in std.objectFields(self.envvars) if std.length(self.envvars[key]) > 0 || (key == 'NATIVE_IMAGES' && has_svm)],
    suite_prefix:: error 'suite_prefix not set',
    cd:: '',
    cd_run:: if self.cd != '' then [['cd', self.cd]] else [],
    graalvmtests:: '',
    graalvmtests_run:: if self.graalvmtests != '' then [
      ['git', 'clone', ['mx', 'urlrewrite', 'https://github.com/graalvm/graalvm-tests.git'], self.graalvmtests],
      ['git', '-C', self.graalvmtests, 'checkout', '75b6a9e16ebbfd8b9b0a24e4be7c4378e3281204'],
    ] else [],
    using_artifact:: false,
    build_standalones:: false,
    build_dependencies:: [],
    setup+: self.graalvm.setup,
    run+: []
      + self.export_envvars
      + self.cd_run
      + self.graalvmtests_run
      + (if std.length(self.cd_run) > 0 then [['mx', 'sversions']] else []),
    timelimit: error "timelimit not set for '" + self.name + "'",
    defined_in: error "defined_in not set for '" + self.name + "'",
    diskspace_required: '30GB',
    environment+: (if 'os' in self && self.os == 'darwin' then {'SYSTEM_VERSION_COMPAT': '0'} else {}), # ensure correct platform.mac_ver()
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

  ce:: {defs:: $.defs, graalvm:: self.defs.ce},
  ee:: {defs:: $.defs, graalvm:: self.defs.ee},

  supportedPlatforms:: [
    common.jdk21 + common.linux_amd64,
    common.jdk21 + common.linux_aarch64,
    common.jdk21 + common.darwin_aarch64,
    common.jdk21 + common.windows_amd64,
    common.jdklatest + common.linux_amd64,
    common.jdklatest + common.linux_aarch64,
    common.jdklatest + common.darwin_amd64,
    common.jdklatest + common.darwin_aarch64,
    common.jdklatest + common.windows_amd64,
  ],
  mainGatePlatform:: common.jdklatest + common.linux_amd64,
  styleGatePlatforms:: [
    common.jdk21 + common.linux_amd64,
    common.jdklatest + common.linux_amd64,
  ],
  jdklatestPlatforms:: [p for p in $.supportedPlatforms if p.jdk_name == 'jdk-latest'],

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
    local mx_base_cmd = ["mx", "-p", build.cd];
    self.jobtemplate + common[jdk] + common[os_arch] + {
    graalvm:: build.graalvm,
    suiteimports:: build.suiteimports,
    nativeimages:: build.nativeimages,
    extraimagebuilderarguments:: build.extraimagebuilderarguments,
    build_dependencies:: build.build_dependencies,
    name: "build-" + artifactName,
    run+: [
      mx_base_cmd + ["sversions"],
      mx_base_cmd + ["graalvm-show"],
      mx_base_cmd + ["build"] + (if self.build_dependencies == [] then [] else ["--dependencies", std.join(',', self.build_dependencies)]),
    ],
    publishArtifacts+: [
      {
        name: artifactName,
        dir: "../",
        patterns: [
          "graal/sdk/mxbuild/" + os + '-' + arch + "/GRAAL*",
          "*/*/mxbuild/jdk*",
          "*/mxbuild",
          "*/graal-nodejs/out", # js/graal-nodejs/out
        ] + (if build.build_standalones then [
          "*/*/mxbuild/" + os + '-' + arch + "/GRAAL*JS*_STANDALONE",
        ] else []),
      },
    ],
    // Avoid building native images on machines with very little RAM.
    capabilities+: if 'os' in self && (self.os == 'darwin' && self.arch == 'amd64') then ['ram16gb'] else [],
    targets: ['ondemand'],
    timelimit: if 'os' in self && (self.os == 'darwin' && self.arch == 'amd64') then '1:30:00' else '1:00:00',
    notify_groups: ['javascript'],
    defined_in: std.thisFile,
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

  local platformName(b) = (if 'jdk' in b then b.jdk + '-' else '') + b.os + '-' + b.arch,
  local platformMatches(platform, query) =
    (!std.objectHasAll(query, 'os') || platform.os == query.os) &&
    (!std.objectHasAll(query, 'arch') || platform.arch == query.arch) &&
    (!std.objectHasAll(query, 'jdk') || platform.jdk == query.jdk),
  local platformMatchesAny(platform, queries) = std.foldl(function(found, query) found || platformMatches(platform, query), queries, false),
  local generateTargets(build, platform, defaultTarget) =
    if (std.objectHasAll(build, 'targets')) then
      [{}] // targets are manually specified already
    else if (std.objectHasAll(build, 'targetSelector')) then
      local selectedTargets = build.targetSelector(platform);
      assert std.isArray(selectedTargets);
      // just a sanity check; remove this assertion to allow multiple targets
      assert std.length(selectedTargets) <= 1 : "build '" + build.name + "-" + platformName(platform) + "' has multiple targets: " + std.toString(selectedTargets);
      if std.length(selectedTargets) != 0 then
        selectedTargets
      else
        [defaultTarget]
    else
      [defaultTarget],
  local generatePlatforms(build, platforms) =
    assert std.isObject(build);
    if std.objectHasAll(build, 'os') then
      [{}] // platform is manually specified already
    else if std.objectHasAll(build, 'platformSelector') && std.isFunction(build.platformSelector) then
      [p for p in platforms if build.platformSelector(p)]
    else platforms,

  // Expand builds to all platforms, unless otherwise specified (explicitly or using filterPlatforms).
  // If any builds are missing targets, <defaultTarget> must be provided that is then applied to those builds.
  generateBuilds(builds, platforms=$.supportedPlatforms, defaultTarget={}):: [
    target + platform + build + {
      assert 'targets' in super : "build '" + super.name + "-" + platformName(self) + "' has no targets and no default targets specified",
      name: std.join('-', [super.suite_prefix, super.targetName, super.name, platformName(self)]),
    }
    for build in flattenArrayRec(builds)
    for platform in generatePlatforms(build, platforms)
    for target in generateTargets(build, platform, defaultTarget)
  ],
  local makePlatformPredicate(include=null, exclude=null) =
    local includePredicate = if std.isFunction(include) then include else if std.isArray(include) then function(platform) platformMatchesAny(platform, include) else function(_) true;
    local excludePredicate = if std.isFunction(exclude) then exclude else if std.isArray(exclude) then function(platform) platformMatchesAny(platform, exclude) else function(_) false;
    function(platform) includePredicate(platform) && !excludePredicate(platform),
  // Promote selected platforms of this build to <target> (e.g. common.gate).
  promoteToTarget(target, platformSelector, override=false)::
    assert std.isObject(target);
    assert std.isArray(platformSelector) || std.isFunction(platformSelector);
    local platformPredicate = makePlatformPredicate(platformSelector);
    {
      targetSelector:: function(platform)
        local superResult = if 'targetSelector' in super then super.targetSelector(platform) else [];
        local thisResult = if platformPredicate(platform) then [target] else [];
        if override && thisResult != [] then thisResult else thisResult + superResult,
    },
  // Set this build's default target for all non-promoted platforms.
  defaultToTarget(defaultTarget)::
    assert std.isObject(defaultTarget);
    {
      targetSelector:: function(platform)
        local superResult = if 'targetSelector' in super then super.targetSelector(platform) else [];
        if superResult != [] then superResult else [defaultTarget],
    },
  // Filter platforms this build should be run on. Composable. Empty array = disable on all.
  local filterPlatforms(platformSelector) =
    assert std.isArray(platformSelector) || std.isFunction(platformSelector);
    local platformPredicate = makePlatformPredicate(platformSelector);
    {
      platformSelector:: function(platform)
        local superResult = if 'platformSelector' in super then super.platformSelector(platform) else true;
        local thisResult = platformPredicate(platform);
        superResult && thisResult,
    },
  includePlatforms(platformSelector):: filterPlatforms(platformSelector),
  excludePlatforms(platformSelector):: filterPlatforms(makePlatformPredicate(exclude=platformSelector)),

  gateOnMain:: self.promoteToTarget(common.gate, [self.mainGatePlatform]),

  local flattenArrayRec(arr) =
    if std.isArray(arr) then
      if std.filter(std.isArray, arr) != [] then
        flattenArrayRec(std.flatMap(function(x) if std.isArray(x) then x else [x], arr))
      else
        arr // already flat
    else
      [arr], // allow single argument; just wrap it in an array
}
