const { resolve } = require('path')
const libexec = require('libnpmexec')
const BaseCommand = require('../base-cmd.js')

class Exec extends BaseCommand {
  static description = 'Run a command from a local or remote npm package'
  static params = [
    'package',
    'call',
    'workspace',
    'workspaces',
    'include-workspace-root',
  ]

  static name = 'exec'
  static usage = [
    '-- <pkg>[@<version>] [args...]',
    '--package=<pkg>[@<version>] -- <cmd> [args...]',
    '-c \'<cmd> [args...]\'',
    '--package=foo -c \'<cmd> [args...]\'',
  ]

  static workspaces = true
  static ignoreImplicitWorkspace = false
  static isShellout = true

  async exec (args) {
    return this.callExec(args)
  }

  async execWorkspaces (args) {
    await this.setWorkspaces()

    for (const [name, path] of this.workspaces) {
      const locationMsg =
        `in workspace ${this.npm.chalk.green(name)} at location:\n${this.npm.chalk.dim(path)}`
      await this.callExec(args, { name, locationMsg, runPath: path })
    }
  }

  async callExec (args, { name, locationMsg, runPath } = {}) {
    // This is where libnpmexec will look for locally installed packages at the project level
    const localPrefix = this.npm.localPrefix
    // This is where libnpmexec will look for locally installed packages at the workspace level
    let localBin = this.npm.localBin
    let path = localPrefix

    // This is where libnpmexec will actually run the scripts from
    if (!runPath) {
      runPath = process.cwd()
    } else {
      // We have to consider if the workspace has its own separate versions
      // libnpmexec will walk up to localDir after looking here
      localBin = resolve(this.npm.localDir, name, 'node_modules', '.bin')
      // We also need to look for `bin` entries in the workspace package.json
      // libnpmexec will NOT look in the project root for the bin entry
      path = runPath
    }

    const call = this.npm.config.get('call')
    let globalPath
    const {
      flatOptions,
      globalBin,
      globalDir,
      chalk,
    } = this.npm
    const scriptShell = this.npm.config.get('script-shell') || undefined
    const packages = this.npm.config.get('package')
    const yes = this.npm.config.get('yes')
    // --prefix sets both of these to the same thing, meaning the global prefix
    // is invalid (i.e. no lib/node_modules).  This is not a trivial thing to
    // untangle and fix so we work around it here.
    if (this.npm.localPrefix !== this.npm.globalPrefix) {
      globalPath = resolve(globalDir, '..')
    }

    if (call && args.length) {
      throw this.usageError()
    }

    return libexec({
      ...flatOptions,
      // we explicitly set packageLockOnly to false because if it's true
      // when we try to install a missing package, we won't actually install it
      packageLockOnly: false,
      // copy args so they dont get mutated
      args: [...args],
      call,
      chalk,
      globalBin,
      globalPath,
      localBin,
      locationMsg,
      packages,
      path,
      runPath,
      scriptShell,
      yes,
    })
  }
}

module.exports = Exec
