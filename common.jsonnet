local common = (import "ci/common.jsonnet");

local jdks = {
  [name]: common.jdks[name] + {
    jdk:: if self.jdk_name == 'jdk-latest' then 'jdklatest' else 'jdk' + super.jdk_version,
    tools_java_home:: {
      downloads+: {
        TOOLS_JAVA_HOME: common.jdks_data[name],
      },
    },
  },
  for name in std.objectFields(common.jdks_data)
} + {
  # Some convenient JDK aliases
  jdk21:: self["labsjdk-ee-21"],
  jdklatest:: self["labsjdk-ee-latest"],
};

local targets = {
  deploy::      {targets+: ['deploy'], targetName:: 'deploy'},
  gate::        {targets+: ['gate'], targetName:: 'gate'},
  postMerge::   {targets+: ['post-merge'], targetName:: 'postmerge'},
  daily::       {targets+: ['daily'], targetName:: 'daily'},
  weekly::      {targets+: ['weekly'], targetName:: 'weekly'},
  monthly::     {targets+: ['monthly'], targetName:: 'monthly'},
  ondemand::    {targets+: ['ondemand'], targetName:: 'ondemand'},

  bench::         self.postMerge + {targets+: ['bench'], targetName:: super.targetName + "-bench"},
  dailyBench::    self.daily     + {targets+: ['bench'], targetName:: super.targetName + "-bench"},
  weeklyBench::   self.weekly    + {targets+: ['bench'], targetName:: super.targetName + "-bench"},
  monthlyBench::  self.monthly   + {targets+: ['bench'], targetName:: super.targetName + "-bench"},
  ondemandBench:: self.ondemand  + {targets+: ['bench'], targetName:: super.targetName + "-bench"},
};

jdks +
targets +
{
  jdks:: jdks,
  targets:: targets,

  deps:: common.deps,

  common_deps:: common.deps.sulong + {
    catch_files+: [
      'npm-debug.log', // created on npm errors
    ],
    environment+: {
      GRAALVM_CHECK_EXPERIMENTAL_OPTIONS: "true",
    },
  },

  linux_common:: self.common_deps + {
    packages+: {
      maven: '==3.3.9',
    },
  },
  darwin_common:: self.common_deps + self.maven_download,
  windows_common:: self.common_deps + self.maven_download,

  # for cases where a maven package is not easily accessible
  maven_download:: {
    downloads+: {
      MAVEN_HOME: {name: 'maven', version: '3.3.9', platformspecific: false},
    },
    local is_windows = 'os' in self && self.os == 'windows',
    environment+: {
      PATH: if is_windows then '$MAVEN_HOME\\bin;$JAVA_HOME\\bin;$PATH' else '$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH',
    },
  },

  linux_amd64:: common.linux_amd64 + self.linux_common,

  e3:: self.linux_amd64 + {
    capabilities+: ['tmpfs25g', 'e3'],
  },

  x52:: self.linux_amd64 + {
    capabilities+: ['tmpfs25g', 'x52'],
  },

  linux_aarch64:: common.linux_aarch64 + self.linux_common,

  darwin_amd64:: common.darwin_amd64 + self.darwin_common + {
    environment+: {
      // for compatibility with macOS BigSur
      MACOSX_DEPLOYMENT_TARGET: '11.0',
    },
  },

  darwin_aarch64:: common.darwin_aarch64 + self.darwin_common + {
    environment+: {
      // for compatibility with macOS BigSur
      MACOSX_DEPLOYMENT_TARGET: '11.0',
    },
  },

  windows_amd64:: common.windows_amd64 + self.windows_common + {
    packages+: common.devkits["windows-" + (if self.jdk_name == "jdk-latest" then "jdkLatest" else self.jdk_name)].packages,
    local devkit_version = std.filterMap(function(p) std.startsWith(p, 'devkit:VS'), function(p) std.substr(p, std.length('devkit:VS'), 4), std.objectFields(self.packages))[0],
    environment+: {
      DEVKIT_VERSION: devkit_version,
    },
  },

  gateCmd:: ['mx', 'gate', '-B=--force-deprecation-as-warning', '-B=-A-J-Dtruffle.dsl.SuppressWarnings=truffle'],
  gateCmdWithTags:: self.gateCmd + ['--tags', '${TAGS}'],

  build:: {
    run+: [
      ['mx', 'build', '--force-javac'],
    ],
  },

  buildCompiler:: {
    run+: [
      ['mx', '--dynamicimports', '/compiler', 'build', '--force-javac'],
    ],
  },

  gateTags:: {
    run+: [
      $.gateCmdWithTags,
    ],
    timelimit: if 'timelimit' in super then super.timelimit else '45:00',
  },

  gateStyleFullBuild:: common.deps.pylint + common.deps.black + common.deps.eclipse + common.deps.jdt + common.deps.spotbugs + {
    local is_jdk_latest = 'jdk_name' in self && self.jdk_name == 'jdk-latest',
    local strict = !is_jdk_latest,
    components+: ['style'],
    environment+: {
      TAGS: 'style,fullbuild',
    },
    run+: [
      $.gateCmdWithTags + (if strict then ['--strict-mode'] else []),
    ],
    timelimit: if 'timelimit' in super then super.timelimit else '45:00',
  },
}
