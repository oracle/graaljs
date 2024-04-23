'use strict';

const {
  Symbol,
  RegExpPrototypeExec,
  globalThis,
} = primordials;

const path = require('path');

const {
  codes: {
    ERR_INVALID_ARG_TYPE,
    ERR_UNCAUGHT_EXCEPTION_CAPTURE_ALREADY_SET,
    ERR_EVAL_ESM_CANNOT_PRINT,
  },
} = require('internal/errors');

const {
  executionAsyncId,
  clearDefaultTriggerAsyncId,
  clearAsyncIdStack,
  hasAsyncIdStack,
  afterHooksExist,
  emitAfter,
  popAsyncContext,
} = require('internal/async_hooks');
const {
  makeContextifyScript, runScriptInThisContext,
} = require('internal/vm');
// shouldAbortOnUncaughtToggle is a typed array for faster
// communication with JS.
const { shouldAbortOnUncaughtToggle } = internalBinding('util');

function tryGetCwd() {
  try {
    return process.cwd();
  } catch {
    // getcwd(3) can fail if the current working directory has been deleted.
    // Fall back to the directory name of the (absolute) executable path.
    // It's not really correct but what are the alternatives?
    return path.dirname(process.execPath);
  }
}

function evalModule(source, print) {
  if (print) {
    throw new ERR_EVAL_ESM_CANNOT_PRINT();
  }
  const { loadESM } = require('internal/process/esm_loader');
  const { handleMainPromise } = require('internal/modules/run_main');
  RegExpPrototypeExec(/^/, ''); // Necessary to reset RegExp statics before user code runs.
  return handleMainPromise(loadESM((loader) => loader.eval(source)));
}

function evalScript(name, body, breakFirstLine, print, shouldLoadESM = false) {
  const CJSModule = require('internal/modules/cjs/loader').Module;
  const { pathToFileURL } = require('internal/url');

  const cwd = tryGetCwd();
  const origModule = globalThis.module;  // Set e.g. when called from the REPL.

  const module = new CJSModule(name);
  module.filename = path.join(cwd, name);
  module.paths = CJSModule._nodeModulePaths(cwd);

  const { handleMainPromise } = require('internal/modules/run_main');
  const asyncESM = require('internal/process/esm_loader');
  const baseUrl = pathToFileURL(module.filename).href;
  const { loadESM } = asyncESM;

  const runScript = () => {
    // Create wrapper for cache entry
    const script = `
      globalThis.module = module;
      globalThis.exports = exports;
      globalThis.__dirname = __dirname;
      globalThis.require = require;
      return (main) => main();
    `;
    globalThis.__filename = name;
    RegExpPrototypeExec(/^/, ''); // Necessary to reset RegExp statics before user code runs.
    const result = module._compile(script, `${name}-wrapper`)(() => {
      const hostDefinedOptionId = Symbol(name);
      async function importModuleDynamically(specifier, _, importAttributes) {
        const loader = asyncESM.esmLoader;
        return loader.import(specifier, baseUrl, importAttributes);
      }
      const script = makeContextifyScript(
        body,                    // code
        name,                    // filename,
        0,                       // lineOffset
        0,                       // columnOffset,
        undefined,               // cachedData
        false,                   // produceCachedData
        undefined,               // parsingContext
        hostDefinedOptionId,     // hostDefinedOptionId
        importModuleDynamically, // importModuleDynamically
      );
      return runScriptInThisContext(script, true, !!breakFirstLine);
    });
    if (print) {
      const { log } = require('internal/console/global');
      log(result);
    }

    if (origModule !== undefined)
      globalThis.module = origModule;
  };

  if (shouldLoadESM) {
    return handleMainPromise(loadESM(runScript));
  }
  return runScript();
}

const exceptionHandlerState = {
  captureFn: null,
  reportFlag: false,
};

function setUncaughtExceptionCaptureCallback(fn) {
  if (fn === null) {
    exceptionHandlerState.captureFn = fn;
    shouldAbortOnUncaughtToggle[0] = 1;
    process.report.reportOnUncaughtException = exceptionHandlerState.reportFlag;
    return;
  }
  if (typeof fn !== 'function') {
    throw new ERR_INVALID_ARG_TYPE('fn', ['Function', 'null'], fn);
  }
  if (exceptionHandlerState.captureFn !== null) {
    throw new ERR_UNCAUGHT_EXCEPTION_CAPTURE_ALREADY_SET();
  }
  exceptionHandlerState.captureFn = fn;
  shouldAbortOnUncaughtToggle[0] = 0;
  exceptionHandlerState.reportFlag =
    process.report.reportOnUncaughtException === true;
  process.report.reportOnUncaughtException = false;
}

function hasUncaughtExceptionCaptureCallback() {
  return exceptionHandlerState.captureFn !== null;
}

function noop() {}

// XXX(joyeecheung): for some reason this cannot be defined at the top-level
// and exported to be written to process._fatalException, it has to be
// returned as an *anonymous function* wrapped inside a factory function,
// otherwise it breaks the test-timers.setInterval async hooks test -
// this may indicate that node::errors::TriggerUncaughtException() should
// fix up the callback scope before calling into process._fatalException,
// or this function should take extra care of the async hooks before it
// schedules a setImmediate.
function createOnGlobalUncaughtException() {
  // The C++ land node::errors::TriggerUncaughtException() will
  // exit the process if it returns false, and continue execution if it
  // returns true (which indicates that the exception is handled by the user).
  return (er, fromPromise) => {
    // It's possible that defaultTriggerAsyncId was set for a constructor
    // call that threw and was never cleared. So clear it now.
    clearDefaultTriggerAsyncId();

    const type = fromPromise ? 'unhandledRejection' : 'uncaughtException';
    process.emit('uncaughtExceptionMonitor', er, type);
    if (exceptionHandlerState.captureFn !== null) {
      exceptionHandlerState.captureFn(er);
    } else if (!process.emit('uncaughtException', er, type)) {
      // If someone handled it, then great. Otherwise, die in C++ land
      // since that means that we'll exit the process, emit the 'exit' event.
      try {
        if (!process._exiting) {
          process._exiting = true;
          process.exitCode = 1;
          process.emit('exit', 1);
        }
      } catch {
        // Nothing to be done about it at this point.
      }
      return false;
    }

    // If we handled an error, then make sure any ticks get processed
    // by ensuring that the next Immediate cycle isn't empty.
    require('timers').setImmediate(noop);

    // Emit the after() hooks now that the exception has been handled.
    if (afterHooksExist()) {
      do {
        const asyncId = executionAsyncId();
        if (asyncId === 0)
          popAsyncContext(0);
        else
          emitAfter(asyncId);
      } while (hasAsyncIdStack());
    }
    // And completely empty the id stack, including anything that may be
    // cached on the native side.
    clearAsyncIdStack();

    return true;
  };
}

function readStdin(callback) {
  process.stdin.setEncoding('utf8');

  let code = '';
  process.stdin.on('data', (d) => {
    code += d;
  });

  process.stdin.on('end', () => {
    callback(code);
  });
}

module.exports = {
  readStdin,
  tryGetCwd,
  evalModule,
  evalScript,
  onGlobalUncaughtException: createOnGlobalUncaughtException(),
  setUncaughtExceptionCaptureCallback,
  hasUncaughtExceptionCaptureCallback,
};
