const os = require('os')
const fs = require('fs').promises
const path = require('path')
const tap = require('tap')
const { output, META } = require('proc-log')
const errorMessage = require('../../lib/utils/error-message')
const mockLogs = require('./mock-logs.js')
const mockGlobals = require('@npmcli/mock-globals')
const tmock = require('./tmock')
const defExitCode = process.exitCode

const changeDir = (dir) => {
  if (dir) {
    const cwd = process.cwd()
    process.chdir(dir)
    return () => process.chdir(cwd)
  }
  return () => {}
}

const setGlobalNodeModules = (globalDir) => {
  const updateSymlinks = (obj, visit) => {
    for (const [key, value] of Object.entries(obj)) {
      if (/Fixture<symlink>/.test(value.toString())) {
        obj[key] = tap.fixture('symlink', path.join('..', value.content))
      } else if (typeof value === 'object') {
        obj[key] = updateSymlinks(value, visit)
      }
    }
    return obj
  }

  if (globalDir.lib) {
    throw new Error('`globalPrefixDir` should not have a top-level `lib/` directory, only a ' +
      'top-level `node_modules/` dir that gets set in the correct location based on platform. ' +
      `Received the following top level entries: ${Object.keys(globalDir).join(', ')}.`
    )
  }

  if (process.platform !== 'win32' && globalDir.node_modules) {
    const { node_modules: nm, ...rest } = globalDir
    return {
      ...rest,
      lib: { node_modules: updateSymlinks(nm) },
    }
  }

  return globalDir
}

const buildMocks = (t, mocks) => {
  const allMocks = { ...mocks }
  // The definitions must be mocked since they are a singleton that reads from
  // process and environs to build defaults in order to break the requiure
  // cache. We also need to mock them with any mocks that were passed in for the
  // test in case those mocks are for things like ci-info which is used there.
  const definitions = '@npmcli/config/lib/definitions'
  allMocks[definitions] = tmock(t, definitions, allMocks)
  return allMocks
}

const getMockNpm = async (t, { mocks, init, load, npm: npmOpts }) => {
  const { streams, logs } = mockLogs()
  const allMocks = buildMocks(t, mocks)
  const Npm = tmock(t, '{LIB}/npm.js', allMocks)

  class MockNpm extends Npm {
    constructor (opts) {
      super({
        ...opts,
        ...streams,
        ...npmOpts,
      })
    }

    async load () {
      const res = await super.load()
      // Wait for any promises (currently only log file cleaning) to be
      // done before returning from load in tests. This helps create more
      // deterministic testing behavior because in reality that promise
      // is left hanging on purpose as a best-effort and the process gets
      // closed regardless of if it has finished or not.
      await Promise.all(this.unrefPromises)
      return res
    }

    async exec (...args) {
      const [res, err] = await super.exec(...args).then((r) => [r]).catch(e => [null, e])
      // This mimics how the exit handler flushes output for commands that have
      // buffered output. It also uses the same json error processing from the
      // error message fn. This is necessary for commands with buffered output
      // to read the output after exec is called. This is not *exactly* how it
      // works in practice, but it is close enough for now.
      const jsonError = err && errorMessage(err, this).json
      output.flush({ [META]: true, jsonError })
      if (err) {
        throw err
      }
      return res
    }
  }

  const npm = init ? new MockNpm() : null
  if (npm && load) {
    await npm.load()
  }

  return {
    Npm: MockNpm,
    npm,
    ...logs,
  }
}

const mockNpms = new Map()

const setupMockNpm = async (t, {
  init = true,
  load = init,
  // preload a command
  command = null, // string name of the command
  exec = null, // optionally exec the command before returning
  // test dirs
  prefixDir = {},
  homeDir = {},
  cacheDir = {},
  globalPrefixDir = { node_modules: {} },
  otherDirs = {},
  chdir = ({ prefix }) => prefix,
  // setup config, env vars, mocks, npm opts
  config: _config = {},
  mocks = {},
  globals = {},
  npm: npmOpts = {},
  argv: rawArgv = [],
} = {}) => {
  // easy to accidentally forget to pass in tap
  if (!(t instanceof tap.Test)) {
    throw new Error('first argument must be a tap instance')
  }

  // mockNpm is designed to only be run once per test chain so we assign it to
  // the test in the cache and error if it is attempted to run again
  let tapInstance = t
  while (tapInstance) {
    if (mockNpms.has(tapInstance)) {
      throw new Error('mockNpm can only be called once in each t.test chain')
    }
    tapInstance = tapInstance.parent
  }
  mockNpms.set(t, true)

  if (!init && load) {
    throw new Error('cant `load` without `init`')
  }

  // These are globals manipulated by npm itself that we need to reset to their
  // original values between tests
  const npmEnvs = Object.keys(process.env).filter(k => k.startsWith('npm_'))
  mockGlobals(t, {
    process: {
      title: process.title,
      execPath: process.execPath,
      env: {
        NODE_ENV: process.env.NODE_ENV,
        COLOR: process.env.COLOR,
        // further, these are npm controlled envs that we need to zero out before
        // before the test. setting them to undefined ensures they are not set and
        // also returned to their original value after the test
        ...npmEnvs.reduce((acc, k) => {
          acc[k] = undefined
          return acc
        }, {}),
      },
    },
  })

  const dir = t.testdir({
    home: homeDir,
    prefix: prefixDir,
    cache: cacheDir,
    global: setGlobalNodeModules(globalPrefixDir),
    other: otherDirs,
  })

  const dirs = {
    testdir: dir,
    prefix: path.join(dir, 'prefix'),
    cache: path.join(dir, 'cache'),
    globalPrefix: path.join(dir, 'global'),
    home: path.join(dir, 'home'),
    other: path.join(dir, 'other'),
  }

  // Option objects can also be functions that are called with all the dir paths
  // so they can be used to set configs that need to be based on paths
  const withDirs = (v) => typeof v === 'function' ? v(dirs) : v

  const teardownDir = changeDir(withDirs(chdir))

  const defaultConfigs = {
    // We want to fail fast when writing tests. Default this to 0 unless it was
    // explicitly set in a test.
    'fetch-retries': 0,
    cache: dirs.cache,
    // This will give us all the loglevels including timing in a non-colorized way
    // so we can easily assert their contents. Individual tests can overwrite these
    // with my passing in configs if they need to test other forms of output.
    loglevel: 'silly',
    color: false,
  }

  const { argv, env, config } = Object.entries({ ...defaultConfigs, ...withDirs(_config) })
    .reduce((acc, [key, value]) => {
      // nerfdart configs passed in need to be set via env var instead of argv
      // and quoted with `"` so mock globals will ignore that it contains dots
      if (key.startsWith('//')) {
        acc.env[`process.env."npm_config_${key}"`] = value
      } else if (value !== undefined) {
        const values = [].concat(value)
        acc.argv.push(...values.flatMap(v => v === '' ? `--${key}` : `--${key}=${v.toString()}`))
      }
      if (value !== undefined) {
        acc.config[key] = value
      }
      return acc
    }, { argv: [...rawArgv], env: {}, config: {} })

  const mockedGlobals = mockGlobals(t, {
    'process.env.HOME': dirs.home,
    // global prefix cannot be (easily) set via argv so this is the easiest way
    // to set it that also closely mimics the behavior a user would see since it
    // will already be set while `npm.load()` is being run
    // Note that this only sets the global prefix and the prefix is set via chdir
    'process.env.PREFIX': dirs.globalPrefix,
    ...withDirs(globals),
    ...env,
  })

  const { npm, ...mockNpm } = await getMockNpm(t, {
    init,
    load,
    mocks: withDirs(mocks),
    npm: {
      argv: command ? [command, ...argv] : argv,
      excludeNpmCwd: true,
      ...withDirs(npmOpts),
    },
  })

  if (config.omit?.includes('prod')) {
    // XXX(HACK): --omit=prod is not a valid config according to the definitions but
    // it was being hacked in via flatOptions for older tests so this is to
    // preserve that behavior and reduce churn in the snapshots. this should be
    // removed or fixed in the future
    npm.flatOptions.omit.push('prod')
  }

  t.teardown(() => {
    if (npm) {
      npm.unload()
    }
    // only set exitCode back if we're passing tests
    if (t.passing()) {
      process.exitCode = defExitCode
    }
    teardownDir()
  })

  const mockCommand = {}
  if (command) {
    const Cmd = mockNpm.Npm.cmd(command)
    mockCommand.cmd = new Cmd(npm)
    mockCommand[command] = {
      usage: Cmd.describeUsage,
      exec: (args) => npm.exec(command, args),
      completion: (args) => Cmd.completion(args, npm),
    }
    if (exec) {
      await mockCommand[command].exec(exec === true ? [] : exec)
      // assign string output to the command now that we have it
      // for easier testing
      mockCommand[command].output = mockNpm.joinedOutput()
    }
  }

  return {
    npm,
    mockedGlobals,
    ...mockNpm,
    ...dirs,
    ...mockCommand,
    debugFile: async () => {
      const readFiles = npm.logFiles.map(f => fs.readFile(f))
      const logFiles = await Promise.all(readFiles)
      return logFiles
        .flatMap((d) => d.toString().trim().split(os.EOL))
        .filter(Boolean)
        .join('\n')
    },
    timingFile: async () => {
      const data = await fs.readFile(npm.logPath + 'timing.json', 'utf8')
      return JSON.parse(data)
    },
  }
}

module.exports = setupMockNpm
module.exports.load = setupMockNpm
module.exports.setGlobalNodeModules = setGlobalNodeModules
