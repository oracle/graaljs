// Base class for npm commands

const { relative } = require('path')

const { definitions } = require('@npmcli/config/lib/definitions')
const getWorkspaces = require('./workspaces/get-workspaces.js')
const { aliases: cmdAliases } = require('./utils/cmd-list')
const log = require('./utils/log-shim.js')

class BaseCommand {
  static workspaces = false
  static ignoreImplicitWorkspace = true

  // these are all overridden by individual commands
  static name = null
  static description = null
  static params = null

  // this is a static so that we can read from it without instantiating a command
  // which would require loading the config
  static get describeUsage () {
    const seenExclusive = new Set()
    const wrapWidth = 80
    const { description, usage = [''], name, params } = this

    const fullUsage = [
      `${description}`,
      '',
      'Usage:',
      ...usage.map(u => `npm ${name} ${u}`.trim()),
    ]

    if (params) {
      let results = ''
      let line = ''
      for (const param of params) {
        /* istanbul ignore next */
        if (seenExclusive.has(param)) {
          continue
        }
        const { exclusive } = definitions[param]
        let paramUsage = `${definitions[param].usage}`
        if (exclusive) {
          const exclusiveParams = [paramUsage]
          seenExclusive.add(param)
          for (const e of exclusive) {
            seenExclusive.add(e)
            exclusiveParams.push(definitions[e].usage)
          }
          paramUsage = `${exclusiveParams.join('|')}`
        }
        paramUsage = `[${paramUsage}]`
        if (line.length + paramUsage.length > wrapWidth) {
          results = [results, line].filter(Boolean).join('\n')
          line = ''
        }
        line = [line, paramUsage].filter(Boolean).join(' ')
      }
      fullUsage.push('')
      fullUsage.push('Options:')
      fullUsage.push([results, line].filter(Boolean).join('\n'))
    }

    const aliases = Object.entries(cmdAliases).reduce((p, [k, v]) => {
      return p.concat(v === name ? k : [])
    }, [])

    if (aliases.length) {
      const plural = aliases.length === 1 ? '' : 'es'
      fullUsage.push('')
      fullUsage.push(`alias${plural}: ${aliases.join(', ')}`)
    }

    fullUsage.push('')
    fullUsage.push(`Run "npm help ${name}" for more info`)

    return fullUsage.join('\n')
  }

  constructor (npm) {
    this.npm = npm

    const { config } = this.npm

    if (!this.constructor.skipConfigValidation) {
      config.validate()
    }

    if (config.get('workspaces') === false && config.get('workspace').length) {
      throw new Error('Can not use --no-workspaces and --workspace at the same time')
    }
  }

  get name () {
    return this.constructor.name
  }

  get description () {
    return this.constructor.description
  }

  get params () {
    return this.constructor.params
  }

  get usage () {
    return this.constructor.describeUsage
  }

  usageError (prefix = '') {
    if (prefix) {
      prefix += '\n\n'
    }
    return Object.assign(new Error(`\n${prefix}${this.usage}`), {
      code: 'EUSAGE',
    })
  }

  async cmdExec (args) {
    const { config } = this.npm

    if (config.get('usage')) {
      return this.npm.output(this.usage)
    }

    const hasWsConfig = config.get('workspaces') || config.get('workspace').length
    // if cwd is a workspace, the default is set to [that workspace]
    const implicitWs = config.get('workspace', 'default').length

    // (-ws || -w foo) && (cwd is not a workspace || command is not ignoring implicit workspaces)
    if (hasWsConfig && (!implicitWs || !this.constructor.ignoreImplicitWorkspace)) {
      if (this.npm.global) {
        throw new Error('Workspaces not supported for global packages')
      }
      if (!this.constructor.workspaces) {
        throw Object.assign(new Error('This command does not support workspaces.'), {
          code: 'ENOWORKSPACES',
        })
      }
      return this.execWorkspaces(args)
    }

    return this.exec(args)
  }

  // Compare the number of entries with what was expected
  checkExpected (entries) {
    if (!this.npm.config.isDefault('expect-results')) {
      const expected = this.npm.config.get('expect-results')
      if (!!entries !== !!expected) {
        log.warn(this.name, `Expected ${expected ? '' : 'no '}results, got ${entries}`)
        process.exitCode = 1
      }
    } else if (!this.npm.config.isDefault('expect-result-count')) {
      const expected = this.npm.config.get('expect-result-count')
      if (expected !== entries) {
        /* eslint-disable-next-line max-len */
        log.warn(this.name, `Expected ${expected} result${expected === 1 ? '' : 's'}, got ${entries}`)
        process.exitCode = 1
      }
    }
  }

  async setWorkspaces () {
    const includeWorkspaceRoot = this.isArboristCmd
      ? false
      : this.npm.config.get('include-workspace-root')

    const prefixInsideCwd = relative(this.npm.localPrefix, process.cwd()).startsWith('..')
    const relativeFrom = prefixInsideCwd ? this.npm.localPrefix : process.cwd()

    const filters = this.npm.config.get('workspace')
    const ws = await getWorkspaces(filters, {
      path: this.npm.localPrefix,
      includeWorkspaceRoot,
      relativeFrom,
    })

    this.workspaces = ws
    this.workspaceNames = [...ws.keys()]
    this.workspacePaths = [...ws.values()]
  }
}
module.exports = BaseCommand
