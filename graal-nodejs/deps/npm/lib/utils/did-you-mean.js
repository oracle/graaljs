const Npm = require('../npm')
const { distance } = require('fastest-levenshtein')
const pkgJson = require('@npmcli/package-json')
const { commands } = require('./cmd-list.js')

const didYouMean = async (path, scmd) => {
  const close = commands.filter(cmd => distance(scmd, cmd) < scmd.length * 0.4 && scmd !== cmd)
  let best = []
  for (const str of close) {
    const cmd = Npm.cmd(str)
    best.push(`  npm ${str} # ${cmd.description}`)
  }
  // We would already be suggesting this in `npm x` so omit them here
  const runScripts = ['stop', 'start', 'test', 'restart']
  try {
    const { content: { scripts, bin } } = await pkgJson.normalize(path)
    best = best.concat(
      Object.keys(scripts || {})
        .filter(cmd => distance(scmd, cmd) < scmd.length * 0.4 && !runScripts.includes(cmd))
        .map(str => `  npm run ${str} # run the "${str}" package script`),
      Object.keys(bin || {})
        .filter(cmd => distance(scmd, cmd) < scmd.length * 0.4)
        /* eslint-disable-next-line max-len */
        .map(str => `  npm exec ${str} # run the "${str}" command from either this or a remote npm package`)
    )
  } catch {
    // gracefully ignore not being in a folder w/ a package.json
  }

  if (best.length === 0) {
    return ''
  }

  return best.length === 1
    ? `\n\nDid you mean this?\n${best[0]}`
    : `\n\nDid you mean one of these?\n${best.slice(0, 3).join('\n')}`
}

module.exports = didYouMean
