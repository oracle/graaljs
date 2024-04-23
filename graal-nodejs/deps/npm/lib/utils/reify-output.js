// pass in an arborist object, and it'll output the data about what
// was done, what was audited, etc.
//
// added ## packages, removed ## packages, and audited ## packages in 19.157s
//
// 1 package is looking for funding
//   run `npm fund` for details
//
// found 37 vulnerabilities (5 low, 7 moderate, 25 high)
//   run `npm audit fix` to fix them, or `npm audit` for details

const log = require('./log-shim.js')
const { depth } = require('treeverse')
const ms = require('ms')
const npmAuditReport = require('npm-audit-report')
const { readTree: getFundingInfo } = require('libnpmfund')
const auditError = require('./audit-error.js')
const Table = require('cli-table3')

// TODO: output JSON if flatOptions.json is true
const reifyOutput = (npm, arb) => {
  const { diff, actualTree } = arb

  // note: fails and crashes if we're running audit fix and there was an error
  // which is a good thing, because there's no point printing all this other
  // stuff in that case!
  const auditReport = auditError(npm, arb.auditReport) ? null : arb.auditReport

  // don't print any info in --silent mode, but we still need to
  // set the exitCode properly from the audit report, if we have one.
  if (npm.silent) {
    getAuditReport(npm, auditReport)
    return
  }

  const summary = {
    added: 0,
    removed: 0,
    changed: 0,
    audited: auditReport && !auditReport.error ? actualTree.inventory.size : 0,
    funding: 0,
  }

  if (diff) {
    let diffTable
    if (npm.config.get('dry-run') || npm.config.get('long')) {
      diffTable = new Table({
        chars: {
          top: '',
          'top-mid': '',
          'top-left': '',
          'top-right': '',
          bottom: '',
          'bottom-mid': '',
          'bottom-left': '',
          'bottom-right': '',
          left: '',
          'left-mid': '',
          mid: '',
          'mid-mid': '',
          right: '',
          'right-mid': '',
          middle: '  ',
        },
        style: {
          'padding-left': 0,
          'padding-right': 0,
          border: 0,
        },
      })
    }

    depth({
      tree: diff,
      visit: d => {
        switch (d.action) {
          case 'REMOVE':
            diffTable?.push(['remove', d.actual.name, d.actual.package.version])
            summary.removed++
            break
          case 'ADD':
            diffTable?.push(['add', d.ideal.name, d.ideal.package.version])
            actualTree.inventory.has(d.ideal) && summary.added++
            break
          case 'CHANGE':
            diffTable?.push(['change',
              d.actual.name,
              d.actual.package.version + ' -> ' + d.ideal.package.version,
            ])
            summary.changed++
            break
          default:
            return
        }
        const node = d.actual || d.ideal
        log.silly(d.action, node.location)
      },
      getChildren: d => d.children,
    })

    if (diffTable) {
      npm.output('\n' + diffTable.toString())
    }
  }

  if (npm.flatOptions.fund) {
    const fundingInfo = getFundingInfo(actualTree, { countOnly: true })
    summary.funding = fundingInfo.length
  }

  if (npm.flatOptions.json) {
    if (auditReport) {
      // call this to set the exit code properly
      getAuditReport(npm, auditReport)
      summary.audit = npm.command === 'audit' ? auditReport
        : auditReport.toJSON().metadata
    }
    npm.output(JSON.stringify(summary, null, 2))
  } else {
    packagesChangedMessage(npm, summary)
    packagesFundingMessage(npm, summary)
    printAuditReport(npm, auditReport)
  }
}

// if we're running `npm audit fix`, then we print the full audit report
// at the end if there's still stuff, because it's silly for `npm audit`
// to tell you to run `npm audit` for details.  otherwise, use the summary
// report.  if we get here, we know it's not quiet or json.
// If the loglevel is silent, then we just run the report
// to get the exitCode set appropriately.
const printAuditReport = (npm, report) => {
  const res = getAuditReport(npm, report)
  if (!res || !res.report) {
    return
  }
  npm.output(`\n${res.report}`)
}

const getAuditReport = (npm, report) => {
  if (!report) {
    return
  }

  // when in silent mode, we print nothing.  the JSON output is
  // going to just JSON.stringify() the report object.
  const reporter = npm.silent ? 'quiet'
    : npm.flatOptions.json ? 'quiet'
    : npm.command !== 'audit' ? 'install'
    : 'detail'
  const defaultAuditLevel = npm.command !== 'audit' ? 'none' : 'low'
  const auditLevel = npm.flatOptions.auditLevel || defaultAuditLevel

  const res = npmAuditReport(report, {
    reporter,
    ...npm.flatOptions,
    auditLevel,
    chalk: npm.chalk,
  })
  if (npm.command === 'audit') {
    process.exitCode = process.exitCode || res.exitCode
  }
  return res
}

const packagesChangedMessage = (npm, { added, removed, changed, audited }) => {
  const msg = ['\n']
  if (added === 0 && removed === 0 && changed === 0) {
    msg.push('up to date')
    if (audited) {
      msg.push(', ')
    }
  } else {
    if (added) {
      msg.push(`added ${added} package${added === 1 ? '' : 's'}`)
    }

    if (removed) {
      if (added) {
        msg.push(', ')
      }

      if (added && !audited && !changed) {
        msg.push('and ')
      }

      msg.push(`removed ${removed} package${removed === 1 ? '' : 's'}`)
    }
    if (changed) {
      if (added || removed) {
        msg.push(', ')
      }

      if (!audited && (added || removed)) {
        msg.push('and ')
      }

      msg.push(`changed ${changed} package${changed === 1 ? '' : 's'}`)
    }
    if (audited) {
      msg.push(', and ')
    }
  }
  if (audited) {
    msg.push(`audited ${audited} package${audited === 1 ? '' : 's'}`)
  }

  msg.push(` in ${ms(Date.now() - npm.started)}`)
  npm.output(msg.join(''))
}

const packagesFundingMessage = (npm, { funding }) => {
  if (!funding) {
    return
  }

  npm.output('')
  const pkg = funding === 1 ? 'package' : 'packages'
  const is = funding === 1 ? 'is' : 'are'
  npm.output(`${funding} ${pkg} ${is} looking for funding`)
  npm.output('  run `npm fund` for details')
}

module.exports = reifyOutput
