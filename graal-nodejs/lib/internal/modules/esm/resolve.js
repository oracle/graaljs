'use strict';

const {
  ArrayIsArray,
  ArrayPrototypeJoin,
  ArrayPrototypeShift,
  JSONStringify,
  ObjectGetOwnPropertyNames,
  ObjectPrototypeHasOwnProperty,
  RegExp,
  RegExpPrototypeExec,
  RegExpPrototypeSymbolReplace,
  SafeMap,
  SafeSet,
  String,
  StringPrototypeEndsWith,
  StringPrototypeIncludes,
  StringPrototypeIndexOf,
  StringPrototypeLastIndexOf,
  StringPrototypeReplace,
  StringPrototypeSlice,
  StringPrototypeSplit,
  StringPrototypeStartsWith,
} = primordials;
const internalFS = require('internal/fs/utils');
const { BuiltinModule } = require('internal/bootstrap/realm');
const { realpathSync } = require('fs');
const { getOptionValue } = require('internal/options');
const pendingDeprecation = getOptionValue('--pending-deprecation');
// Do not eagerly grab .manifest, it may be in TDZ
const policy = getOptionValue('--experimental-policy') ?
  require('internal/process/policy') :
  null;
const { sep, relative, resolve, toNamespacedPath } = require('path');
const preserveSymlinks = getOptionValue('--preserve-symlinks');
const preserveSymlinksMain = getOptionValue('--preserve-symlinks-main');
const experimentalNetworkImports =
  getOptionValue('--experimental-network-imports');
const inputTypeFlag = getOptionValue('--input-type');
const { URL, pathToFileURL, fileURLToPath, isURL, toPathIfFileURL } = require('internal/url');
const { getCWDURL } = require('internal/util');
const { canParse: URLCanParse } = internalBinding('url');
const {
  ERR_INPUT_TYPE_NOT_ALLOWED,
  ERR_INVALID_ARG_TYPE,
  ERR_INVALID_MODULE_SPECIFIER,
  ERR_INVALID_PACKAGE_CONFIG,
  ERR_INVALID_PACKAGE_TARGET,
  ERR_MANIFEST_DEPENDENCY_MISSING,
  ERR_MODULE_NOT_FOUND,
  ERR_PACKAGE_IMPORT_NOT_DEFINED,
  ERR_PACKAGE_PATH_NOT_EXPORTED,
  ERR_UNSUPPORTED_DIR_IMPORT,
  ERR_NETWORK_IMPORT_DISALLOWED,
} = require('internal/errors').codes;

const { Module: CJSModule } = require('internal/modules/cjs/loader');
const { getPackageScopeConfig } = require('internal/modules/esm/package_config');
const { getConditionsSet } = require('internal/modules/esm/utils');
const packageJsonReader = require('internal/modules/package_json_reader');
const { internalModuleStat } = internalBinding('fs');

/**
 * @typedef {import('internal/modules/esm/package_config.js').PackageConfig} PackageConfig
 */


const emittedPackageWarnings = new SafeSet();

/**
 * Emits a deprecation warning for the use of a deprecated trailing slash pattern mapping in the "exports" field
 * module resolution of a package.
 * @param {string} match - The deprecated trailing slash pattern mapping.
 * @param {string} pjsonUrl - The URL of the package.json file.
 * @param {string} base - The URL of the module that imported the package.
 */
function emitTrailingSlashPatternDeprecation(match, pjsonUrl, base) {
  const pjsonPath = fileURLToPath(pjsonUrl);
  if (emittedPackageWarnings.has(pjsonPath + '|' + match)) { return; }
  emittedPackageWarnings.add(pjsonPath + '|' + match);
  process.emitWarning(
    `Use of deprecated trailing slash pattern mapping "${match}" in the ` +
    `"exports" field module resolution of the package at ${pjsonPath}${
      base ? ` imported from ${fileURLToPath(base)}` :
        ''}. Mapping specifiers ending in "/" is no longer supported.`,
    'DeprecationWarning',
    'DEP0155',
  );
}

const doubleSlashRegEx = /[/\\][/\\]/;

/**
 * Emits a deprecation warning for invalid segment in module resolution.
 * @param {string} target - The target module.
 * @param {string} request - The requested module.
 * @param {string} match - The matched module.
 * @param {string} pjsonUrl - The package.json URL.
 * @param {boolean} internal - Whether the module is in the "imports" or "exports" field.
 * @param {string} base - The base URL.
 * @param {boolean} isTarget - Whether the target is a module.
 */
function emitInvalidSegmentDeprecation(target, request, match, pjsonUrl, internal, base, isTarget) {
  if (!pendingDeprecation) { return; }
  const pjsonPath = fileURLToPath(pjsonUrl);
  const double = RegExpPrototypeExec(doubleSlashRegEx, isTarget ? target : request) !== null;
  process.emitWarning(
    `Use of deprecated ${double ? 'double slash' :
      'leading or trailing slash matching'} resolving "${target}" for module ` +
      `request "${request}" ${request !== match ? `matched to "${match}" ` : ''
      }in the "${internal ? 'imports' : 'exports'}" field module resolution of the package at ${
        pjsonPath}${base ? ` imported from ${fileURLToPath(base)}` : ''}.`,
    'DeprecationWarning',
    'DEP0166',
  );
}

/**
 * Emits a deprecation warning if the given URL is a module and
 * the package.json file does not define a "main" or "exports" field.
 * @param {URL} url - The URL of the module being resolved.
 * @param {URL} packageJSONUrl - The URL of the package.json file for the module.
 * @param {string | URL} [base] - The base URL for the module being resolved.
 * @param {string} [main] - The "main" field from the package.json file.
 */
function emitLegacyIndexDeprecation(url, packageJSONUrl, base, main) {
  const format = defaultGetFormatWithoutErrors(url);
  if (format !== 'module') { return; }
  const path = fileURLToPath(url);
  const pkgPath = fileURLToPath(new URL('.', packageJSONUrl));
  const basePath = fileURLToPath(base);
  if (!main) {
    process.emitWarning(
      `No "main" or "exports" field defined in the package.json for ${pkgPath
      } resolving the main entry point "${
        StringPrototypeSlice(path, pkgPath.length)}", imported from ${basePath
      }.\nDefault "index" lookups for the main are deprecated for ES modules.`,
      'DeprecationWarning',
      'DEP0151',
    );
  } else if (resolve(pkgPath, main) !== path) {
    process.emitWarning(
      `Package ${pkgPath} has a "main" field set to "${main}", ` +
      `excluding the full filename and extension to the resolved file at "${
        StringPrototypeSlice(path, pkgPath.length)}", imported from ${
        basePath}.\n Automatic extension resolution of the "main" field is ` +
      'deprecated for ES modules.',
      'DeprecationWarning',
      'DEP0151',
    );
  }
}

const realpathCache = new SafeMap();

/**
 * @param {string | URL} url
 * @returns {boolean}
 */
function fileExists(url) {
  return internalModuleStat(toNamespacedPath(toPathIfFileURL(url))) === 0;
}

/**
 * Legacy CommonJS main resolution:
 * 1. let M = pkg_url + (json main field)
 * 2. TRY(M, M.js, M.json, M.node)
 * 3. TRY(M/index.js, M/index.json, M/index.node)
 * 4. TRY(pkg_url/index.js, pkg_url/index.json, pkg_url/index.node)
 * 5. NOT_FOUND
 * @param {URL} packageJSONUrl
 * @param {PackageConfig} packageConfig
 * @param {string | URL | undefined} base
 * @returns {URL}
 */
function legacyMainResolve(packageJSONUrl, packageConfig, base) {
  let guess;
  if (packageConfig.main !== undefined) {
    // Note: fs check redundances will be handled by Descriptor cache here.
    if (fileExists(guess = new URL(`./${packageConfig.main}`, packageJSONUrl))) {
      return guess;
    } else if (fileExists(guess = new URL(`./${packageConfig.main}.js`, packageJSONUrl))) {
      // Handled below.
    } else if (fileExists(guess = new URL(`./${packageConfig.main}.json`, packageJSONUrl))) {
      // Handled below.
    } else if (fileExists(guess = new URL(`./${packageConfig.main}.node`, packageJSONUrl))) {
      // Handled below.
    } else if (fileExists(guess = new URL(`./${packageConfig.main}/index.js`, packageJSONUrl))) {
      // Handled below.
    } else if (fileExists(guess = new URL(`./${packageConfig.main}/index.json`, packageJSONUrl))) {
      // Handled below.
    } else if (fileExists(guess = new URL(`./${packageConfig.main}/index.node`, packageJSONUrl))) {
      // Handled below.
    } else {
      guess = undefined;
    }
    if (guess) {
      emitLegacyIndexDeprecation(guess, packageJSONUrl, base,
                                 packageConfig.main);
      return guess;
    }
    // Fallthrough.
  }
  if (fileExists(guess = new URL('./index.js', packageJSONUrl))) {
    // Handled below.
  } else if (fileExists(guess = new URL('./index.json', packageJSONUrl))) {
    // Handled below.
  } else if (fileExists(guess = new URL('./index.node', packageJSONUrl))) {
    // Handled below.
  } else {
    guess = undefined;
  }
  if (guess) {
    emitLegacyIndexDeprecation(guess, packageJSONUrl, base, packageConfig.main);
    return guess;
  }
  // Not found.
  throw new ERR_MODULE_NOT_FOUND(
    fileURLToPath(new URL('.', packageJSONUrl)), fileURLToPath(base));
}

/**
 * @param {URL} search
 * @returns {URL | undefined}
 */
function resolveExtensionsWithTryExactName(search) {
  if (fileExists(search)) { return search; }
  return resolveExtensions(search);
}

const extensions = ['.js', '.json', '.node', '.mjs'];

/**
 * @param {URL} search
 * @returns {URL | undefined}
 */
function resolveExtensions(search) {
  for (let i = 0; i < extensions.length; i++) {
    const extension = extensions[i];
    const guess = new URL(`${search.pathname}${extension}`, search);
    if (fileExists(guess)) { return guess; }
  }
  return undefined;
}

/**
 * @param {URL} search
 * @returns {URL | undefined}
 */
function resolveDirectoryEntry(search) {
  const dirPath = fileURLToPath(search);
  const pkgJsonPath = resolve(dirPath, 'package.json');
  if (fileExists(pkgJsonPath)) {
    const pkgJson = packageJsonReader.read(pkgJsonPath);
    if (pkgJson.exists) {
      const { main } = pkgJson;
      if (main != null) {
        const mainUrl = pathToFileURL(resolve(dirPath, main));
        return resolveExtensionsWithTryExactName(mainUrl);
      }
    }
  }
  return resolveExtensions(new URL('index', search));
}

const encodedSepRegEx = /%2F|%5C/i;
/**
 * Finalizes the resolution of a module specifier by checking if the resolved pathname contains encoded "/" or "\\"
 * characters, checking if the resolved pathname is a directory or file, and resolving any symlinks if necessary.
 * @param {URL} resolved - The resolved URL object.
 * @param {string | URL | undefined} base - The base URL object.
 * @param {boolean} preserveSymlinks - Whether to preserve symlinks or not.
 * @returns {URL} - The finalized URL object.
 * @throws {ERR_INVALID_MODULE_SPECIFIER} - If the resolved pathname contains encoded "/" or "\\" characters.
 * @throws {ERR_UNSUPPORTED_DIR_IMPORT} - If the resolved pathname is a directory.
 * @throws {ERR_MODULE_NOT_FOUND} - If the resolved pathname is not a file.
 */
function finalizeResolution(resolved, base, preserveSymlinks) {
  if (RegExpPrototypeExec(encodedSepRegEx, resolved.pathname) !== null) {
    throw new ERR_INVALID_MODULE_SPECIFIER(
      resolved.pathname, 'must not include encoded "/" or "\\" characters',
      fileURLToPath(base));
  }

  let path;
  try {
    path = fileURLToPath(resolved);
  } catch (err) {
    const { setOwnProperty } = require('internal/util');
    setOwnProperty(err, 'input', `${resolved}`);
    setOwnProperty(err, 'module', `${base}`);
    throw err;
  }

  if (getOptionValue('--experimental-specifier-resolution') === 'node') {
    let file = resolveExtensionsWithTryExactName(resolved);

    // Directory
    if (file === undefined) {
      file = StringPrototypeEndsWith(path, '/') ?
        (resolveDirectoryEntry(resolved) || resolved) : resolveDirectoryEntry(new URL(`${resolved}/`));

      if (file === resolved) { return file; }

      if (file === undefined) {
        throw new ERR_MODULE_NOT_FOUND(
          resolved.pathname, fileURLToPath(base), 'module');
      }
    }
    // If `preserveSymlinks` is false, `resolved` is returned and `path`
    // is used only to check that the resolved path exists.
    resolved = file;
    path = fileURLToPath(resolved);
  }

  const stats = internalModuleStat(toNamespacedPath(StringPrototypeEndsWith(path, '/') ?
    StringPrototypeSlice(path, -1) : path));

  // Check for stats.isDirectory()
  if (stats === 1) {
    throw new ERR_UNSUPPORTED_DIR_IMPORT(path, fileURLToPath(base), String(resolved));
  } else if (stats !== 0) {
    // Check for !stats.isFile()
    if (process.env.WATCH_REPORT_DEPENDENCIES && process.send) {
      process.send({ 'watch:require': [path || resolved.pathname] });
    }
    throw new ERR_MODULE_NOT_FOUND(
      path || resolved.pathname, base && fileURLToPath(base), resolved);
  }

  if (!preserveSymlinks) {
    const real = realpathSync(path, {
      [internalFS.realpathCacheKey]: realpathCache,
    });
    const { search, hash } = resolved;
    resolved =
        pathToFileURL(real + (StringPrototypeEndsWith(path, sep) ? '/' : ''));
    resolved.search = search;
    resolved.hash = hash;
  }

  return resolved;
}

/**
 * Returns an error object indicating that the specified import is not defined.
 * @param {string} specifier - The import specifier that is not defined.
 * @param {URL} packageJSONUrl - The URL of the package.json file, or null if not available.
 * @param {string | URL | undefined} base - The base URL to use for resolving relative URLs.
 * @returns {ERR_PACKAGE_IMPORT_NOT_DEFINED} - The error object.
 */
function importNotDefined(specifier, packageJSONUrl, base) {
  return new ERR_PACKAGE_IMPORT_NOT_DEFINED(
    specifier, packageJSONUrl && fileURLToPath(new URL('.', packageJSONUrl)),
    fileURLToPath(base));
}

/**
 * Returns an error object indicating that the specified subpath was not exported by the package.
 * @param {string} subpath - The subpath that was not exported.
 * @param {URL} packageJSONUrl - The URL of the package.json file.
 * @param {string | URL | undefined} [base] - The base URL to use for resolving the subpath.
 * @returns {ERR_PACKAGE_PATH_NOT_EXPORTED} - The error object.
 */
function exportsNotFound(subpath, packageJSONUrl, base) {
  return new ERR_PACKAGE_PATH_NOT_EXPORTED(
    fileURLToPath(new URL('.', packageJSONUrl)), subpath,
    base && fileURLToPath(base));
}

/**
 * Throws an error indicating that the given request is not a valid subpath match for the specified pattern.
 * @param {string} request - The request that failed to match the pattern.
 * @param {string} match - The pattern that the request was compared against.
 * @param {URL} packageJSONUrl - The URL of the package.json file being resolved.
 * @param {boolean} internal - Whether the resolution is for an "imports" or "exports" field in package.json.
 * @param {string | URL | undefined} base - The base URL for the resolution.
 * @throws {ERR_INVALID_MODULE_SPECIFIER} When the request is not a valid match for the pattern.
 */
function throwInvalidSubpath(request, match, packageJSONUrl, internal, base) {
  const reason = `request is not a valid match in pattern "${match}" for the "${
    internal ? 'imports' : 'exports'}" resolution of ${
    fileURLToPath(packageJSONUrl)}`;
  throw new ERR_INVALID_MODULE_SPECIFIER(request, reason,
                                         base && fileURLToPath(base));
}

/**
 * Creates an error object for an invalid package target.
 * @param {string} subpath - The subpath.
 * @param {import('internal/modules/esm/package_config.js').PackageTarget} target - The target.
 * @param {URL} packageJSONUrl - The URL of the package.json file.
 * @param {boolean} internal - Whether the package is internal.
 * @param {string | URL | undefined} base - The base URL.
 * @returns {ERR_INVALID_PACKAGE_TARGET} - The error object.
 */
function invalidPackageTarget(
  subpath, target, packageJSONUrl, internal, base) {
  if (typeof target === 'object' && target !== null) {
    target = JSONStringify(target, null, '');
  } else {
    target = `${target}`;
  }
  return new ERR_INVALID_PACKAGE_TARGET(
    fileURLToPath(new URL('.', packageJSONUrl)), subpath, target,
    internal, base && fileURLToPath(base));
}

const invalidSegmentRegEx = /(^|\\|\/)((\.|%2e)(\.|%2e)?|(n|%6e|%4e)(o|%6f|%4f)(d|%64|%44)(e|%65|%45)(_|%5f)(m|%6d|%4d)(o|%6f|%4f)(d|%64|%44)(u|%75|%55)(l|%6c|%4c)(e|%65|%45)(s|%73|%53))?(\\|\/|$)/i;
const deprecatedInvalidSegmentRegEx = /(^|\\|\/)((\.|%2e)(\.|%2e)?|(n|%6e|%4e)(o|%6f|%4f)(d|%64|%44)(e|%65|%45)(_|%5f)(m|%6d|%4d)(o|%6f|%4f)(d|%64|%44)(u|%75|%55)(l|%6c|%4c)(e|%65|%45)(s|%73|%53))(\\|\/|$)/i;
const invalidPackageNameRegEx = /^\.|%|\\/;
const patternRegEx = /\*/g;

/**
 * Resolves the package target string to a URL object.
 * @param {string} target - The target string to resolve.
 * @param {string} subpath - The subpath to append to the resolved URL.
 * @param {RegExpMatchArray} match - The matched string array from the import statement.
 * @param {string} packageJSONUrl - The URL of the package.json file.
 * @param {string} base - The base URL to resolve the target against.
 * @param {RegExp} pattern - The pattern to replace in the target string.
 * @param {boolean} internal - Whether the target is internal to the package.
 * @param {boolean} isPathMap - Whether the target is a path map.
 * @param {string[]} conditions - The import conditions.
 * @returns {URL} - The resolved URL object.
 * @throws {ERR_INVALID_PACKAGE_TARGET} - If the target is invalid.
 * @throws {ERR_INVALID_SUBPATH} - If the subpath is invalid.
 */
function resolvePackageTargetString(
  target,
  subpath,
  match,
  packageJSONUrl,
  base,
  pattern,
  internal,
  isPathMap,
  conditions,
) {

  if (subpath !== '' && !pattern && target[target.length - 1] !== '/') {
    throw invalidPackageTarget(match, target, packageJSONUrl, internal, base);
  }

  if (!StringPrototypeStartsWith(target, './')) {
    if (internal && !StringPrototypeStartsWith(target, '../') &&
        !StringPrototypeStartsWith(target, '/')) {
      // No need to convert target to string, since it's already presumed to be
      if (!URLCanParse(target)) {
        const exportTarget = pattern ?
          RegExpPrototypeSymbolReplace(patternRegEx, target, () => subpath) :
          target + subpath;
        return packageResolve(
          exportTarget, packageJSONUrl, conditions);
      }
    }
    throw invalidPackageTarget(match, target, packageJSONUrl, internal, base);
  }

  if (RegExpPrototypeExec(invalidSegmentRegEx, StringPrototypeSlice(target, 2)) !== null) {
    if (RegExpPrototypeExec(deprecatedInvalidSegmentRegEx, StringPrototypeSlice(target, 2)) === null) {
      if (!isPathMap) {
        const request = pattern ?
          StringPrototypeReplace(match, '*', () => subpath) :
          match + subpath;
        const resolvedTarget = pattern ?
          RegExpPrototypeSymbolReplace(patternRegEx, target, () => subpath) :
          target;
        emitInvalidSegmentDeprecation(resolvedTarget, request, match, packageJSONUrl, internal, base, true);
      }
    } else {
      throw invalidPackageTarget(match, target, packageJSONUrl, internal, base);
    }
  }

  const resolved = new URL(target, packageJSONUrl);
  const resolvedPath = resolved.pathname;
  const packagePath = new URL('.', packageJSONUrl).pathname;

  if (!StringPrototypeStartsWith(resolvedPath, packagePath)) {
    throw invalidPackageTarget(match, target, packageJSONUrl, internal, base);
  }

  if (subpath === '') { return resolved; }

  if (RegExpPrototypeExec(invalidSegmentRegEx, subpath) !== null) {
    const request = pattern ? StringPrototypeReplace(match, '*', () => subpath) : match + subpath;
    if (RegExpPrototypeExec(deprecatedInvalidSegmentRegEx, subpath) === null) {
      if (!isPathMap) {
        const resolvedTarget = pattern ?
          RegExpPrototypeSymbolReplace(patternRegEx, target, () => subpath) :
          target;
        emitInvalidSegmentDeprecation(resolvedTarget, request, match, packageJSONUrl, internal, base, false);
      }
    } else {
      throwInvalidSubpath(request, match, packageJSONUrl, internal, base);
    }
  }

  if (pattern) {
    return new URL(
      RegExpPrototypeSymbolReplace(patternRegEx, resolved.href, () => subpath),
    );
  }

  return new URL(subpath, resolved);
}

/**
 * Checks if the given key is a valid array index.
 * @param {string} key - The key to check.
 * @returns {boolean} - Returns `true` if the key is a valid array index, else `false`.
 */
function isArrayIndex(key) {
  const keyNum = +key;
  if (`${keyNum}` !== key) { return false; }
  return keyNum >= 0 && keyNum < 0xFFFF_FFFF;
}

/**
 * Resolves the target of a package based on the provided parameters.
 * @param {string} packageJSONUrl - The URL of the package.json file.
 * @param {import('internal/modules/esm/package_config.js').PackageTarget} target - The target to resolve.
 * @param {string} subpath - The subpath to resolve.
 * @param {string} packageSubpath - The subpath of the package to resolve.
 * @param {string} base - The base path to resolve.
 * @param {RegExp} pattern - The pattern to match.
 * @param {boolean} internal - Whether the package is internal.
 * @param {boolean} isPathMap - Whether the package is a path map.
 * @param {Set<string>} conditions - The conditions to match.
 * @returns {URL | null | undefined} - The resolved target, or null if not found, or undefined if not resolvable.
 */
function resolvePackageTarget(packageJSONUrl, target, subpath, packageSubpath,
                              base, pattern, internal, isPathMap, conditions) {
  if (typeof target === 'string') {
    return resolvePackageTargetString(
      target, subpath, packageSubpath, packageJSONUrl, base, pattern, internal,
      isPathMap, conditions);
  } else if (ArrayIsArray(target)) {
    if (target.length === 0) {
      return null;
    }

    let lastException;
    for (let i = 0; i < target.length; i++) {
      const targetItem = target[i];
      let resolveResult;
      try {
        resolveResult = resolvePackageTarget(
          packageJSONUrl, targetItem, subpath, packageSubpath, base, pattern,
          internal, isPathMap, conditions);
      } catch (e) {
        lastException = e;
        if (e.code === 'ERR_INVALID_PACKAGE_TARGET') {
          continue;
        }
        throw e;
      }
      if (resolveResult === undefined) {
        continue;
      }
      if (resolveResult === null) {
        lastException = null;
        continue;
      }
      return resolveResult;
    }
    if (lastException === undefined || lastException === null) {
      return lastException;
    }
    throw lastException;
  } else if (typeof target === 'object' && target !== null) {
    const keys = ObjectGetOwnPropertyNames(target);
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i];
      if (isArrayIndex(key)) {
        throw new ERR_INVALID_PACKAGE_CONFIG(
          fileURLToPath(packageJSONUrl), base,
          '"exports" cannot contain numeric property keys.');
      }
    }
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i];
      if (key === 'default' || conditions.has(key)) {
        const conditionalTarget = target[key];
        const resolveResult = resolvePackageTarget(
          packageJSONUrl, conditionalTarget, subpath, packageSubpath, base,
          pattern, internal, isPathMap, conditions);
        if (resolveResult === undefined) { continue; }
        return resolveResult;
      }
    }
    return undefined;
  } else if (target === null) {
    return null;
  }
  throw invalidPackageTarget(packageSubpath, target, packageJSONUrl, internal,
                             base);
}

/**
 * Is the given exports object using the shorthand syntax?
 * @param {import('internal/modules/esm/package_config.js').PackageConfig['exports']} exports
 * @param {URL} packageJSONUrl The URL of the package.json file.
 * @param {string | URL | undefined} base The base URL.
 */
function isConditionalExportsMainSugar(exports, packageJSONUrl, base) {
  if (typeof exports === 'string' || ArrayIsArray(exports)) { return true; }
  if (typeof exports !== 'object' || exports === null) { return false; }

  const keys = ObjectGetOwnPropertyNames(exports);
  let isConditionalSugar = false;
  let i = 0;
  for (let j = 0; j < keys.length; j++) {
    const key = keys[j];
    const curIsConditionalSugar = key === '' || key[0] !== '.';
    if (i++ === 0) {
      isConditionalSugar = curIsConditionalSugar;
    } else if (isConditionalSugar !== curIsConditionalSugar) {
      throw new ERR_INVALID_PACKAGE_CONFIG(
        fileURLToPath(packageJSONUrl), base,
        '"exports" cannot contain some keys starting with \'.\' and some not.' +
        ' The exports object must either be an object of package subpath keys' +
        ' or an object of main entry condition name keys only.');
    }
  }
  return isConditionalSugar;
}

/**
 * Resolves the exports of a package.
 * @param {URL} packageJSONUrl - The URL of the package.json file.
 * @param {string} packageSubpath - The subpath of the package to resolve.
 * @param {import('internal/modules/esm/package_config.js').PackageConfig} packageConfig - The package metadata.
 * @param {string | URL | undefined} base - The base path to resolve from.
 * @param {Set<string>} conditions - An array of conditions to match.
 * @returns {URL} - The resolved package target.
 */
function packageExportsResolve(
  packageJSONUrl, packageSubpath, packageConfig, base, conditions) {
  let exports = packageConfig.exports;
  if (isConditionalExportsMainSugar(exports, packageJSONUrl, base)) {
    exports = { '.': exports };
  }

  if (ObjectPrototypeHasOwnProperty(exports, packageSubpath) &&
      !StringPrototypeIncludes(packageSubpath, '*') &&
      !StringPrototypeEndsWith(packageSubpath, '/')) {
    const target = exports[packageSubpath];
    const resolveResult = resolvePackageTarget(
      packageJSONUrl, target, '', packageSubpath, base, false, false, false,
      conditions,
    );

    if (resolveResult == null) {
      throw exportsNotFound(packageSubpath, packageJSONUrl, base);
    }

    return resolveResult;
  }

  let bestMatch = '';
  let bestMatchSubpath;
  const keys = ObjectGetOwnPropertyNames(exports);
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    const patternIndex = StringPrototypeIndexOf(key, '*');
    if (patternIndex !== -1 &&
        StringPrototypeStartsWith(packageSubpath,
                                  StringPrototypeSlice(key, 0, patternIndex))) {
      // When this reaches EOL, this can throw at the top of the whole function:
      //
      // if (StringPrototypeEndsWith(packageSubpath, '/'))
      //   throwInvalidSubpath(packageSubpath)
      //
      // To match "imports" and the spec.
      if (StringPrototypeEndsWith(packageSubpath, '/')) {
        emitTrailingSlashPatternDeprecation(packageSubpath, packageJSONUrl,
                                            base);
      }
      const patternTrailer = StringPrototypeSlice(key, patternIndex + 1);
      if (packageSubpath.length >= key.length &&
          StringPrototypeEndsWith(packageSubpath, patternTrailer) &&
          patternKeyCompare(bestMatch, key) === 1 &&
          StringPrototypeLastIndexOf(key, '*') === patternIndex) {
        bestMatch = key;
        bestMatchSubpath = StringPrototypeSlice(
          packageSubpath, patternIndex,
          packageSubpath.length - patternTrailer.length);
      }
    }
  }

  if (bestMatch) {
    const target = exports[bestMatch];
    const resolveResult = resolvePackageTarget(
      packageJSONUrl,
      target,
      bestMatchSubpath,
      bestMatch,
      base,
      true,
      false,
      StringPrototypeEndsWith(packageSubpath, '/'),
      conditions);

    if (resolveResult == null) {
      throw exportsNotFound(packageSubpath, packageJSONUrl, base);
    }
    return resolveResult;
  }

  throw exportsNotFound(packageSubpath, packageJSONUrl, base);
}

/**
 * Compares two strings that may contain a wildcard character ('*') and returns a value indicating their order.
 * @param {string} a - The first string to compare.
 * @param {string} b - The second string to compare.
 * @returns {number} - A negative number if `a` should come before `b`, a positive number if `a` should come after `b`,
 * or 0 if they are equal.
 */
function patternKeyCompare(a, b) {
  const aPatternIndex = StringPrototypeIndexOf(a, '*');
  const bPatternIndex = StringPrototypeIndexOf(b, '*');
  const baseLenA = aPatternIndex === -1 ? a.length : aPatternIndex + 1;
  const baseLenB = bPatternIndex === -1 ? b.length : bPatternIndex + 1;
  if (baseLenA > baseLenB) { return -1; }
  if (baseLenB > baseLenA) { return 1; }
  if (aPatternIndex === -1) { return 1; }
  if (bPatternIndex === -1) { return -1; }
  if (a.length > b.length) { return -1; }
  if (b.length > a.length) { return 1; }
  return 0;
}

/**
 * Resolves the given import name for a package.
 * @param {string} name - The name of the import to resolve.
 * @param {string | URL | undefined} base - The base URL to resolve the import from.
 * @param {Set<string>} conditions - An object containing the import conditions.
 * @throws {ERR_INVALID_MODULE_SPECIFIER} If the import name is not valid.
 * @throws {ERR_PACKAGE_IMPORT_NOT_DEFINED} If the import name cannot be resolved.
 * @returns {URL} The resolved import URL.
 */
function packageImportsResolve(name, base, conditions) {
  if (name === '#' || StringPrototypeStartsWith(name, '#/') ||
      StringPrototypeEndsWith(name, '/')) {
    const reason = 'is not a valid internal imports specifier name';
    throw new ERR_INVALID_MODULE_SPECIFIER(name, reason, fileURLToPath(base));
  }
  let packageJSONUrl;
  const packageConfig = getPackageScopeConfig(base);
  if (packageConfig.exists) {
    packageJSONUrl = pathToFileURL(packageConfig.pjsonPath);
    const imports = packageConfig.imports;
    if (imports) {
      if (ObjectPrototypeHasOwnProperty(imports, name) &&
          !StringPrototypeIncludes(name, '*')) {
        const resolveResult = resolvePackageTarget(
          packageJSONUrl, imports[name], '', name, base, false, true, false,
          conditions,
        );
        if (resolveResult != null) {
          return resolveResult;
        }
      } else {
        let bestMatch = '';
        let bestMatchSubpath;
        const keys = ObjectGetOwnPropertyNames(imports);
        for (let i = 0; i < keys.length; i++) {
          const key = keys[i];
          const patternIndex = StringPrototypeIndexOf(key, '*');
          if (patternIndex !== -1 &&
              StringPrototypeStartsWith(name,
                                        StringPrototypeSlice(key, 0,
                                                             patternIndex))) {
            const patternTrailer = StringPrototypeSlice(key, patternIndex + 1);
            if (name.length >= key.length &&
                StringPrototypeEndsWith(name, patternTrailer) &&
                patternKeyCompare(bestMatch, key) === 1 &&
                StringPrototypeLastIndexOf(key, '*') === patternIndex) {
              bestMatch = key;
              bestMatchSubpath = StringPrototypeSlice(
                name, patternIndex, name.length - patternTrailer.length);
            }
          }
        }

        if (bestMatch) {
          const target = imports[bestMatch];
          const resolveResult = resolvePackageTarget(packageJSONUrl, target,
                                                     bestMatchSubpath,
                                                     bestMatch, base, true,
                                                     true, false, conditions);
          if (resolveResult != null) {
            return resolveResult;
          }
        }
      }
    }
  }
  throw importNotDefined(name, packageJSONUrl, base);
}

/**
 * Returns the package type for a given URL.
 * @param {URL} url - The URL to get the package type for.
 */
function getPackageType(url) {
  const packageConfig = getPackageScopeConfig(url);
  return packageConfig.type;
}

/**
 * Parse a package name from a specifier.
 * @param {string} specifier - The import specifier.
 * @param {string | URL | undefined} base - The parent URL.
 */
function parsePackageName(specifier, base) {
  let separatorIndex = StringPrototypeIndexOf(specifier, '/');
  let validPackageName = true;
  let isScoped = false;
  if (specifier[0] === '@') {
    isScoped = true;
    if (separatorIndex === -1 || specifier.length === 0) {
      validPackageName = false;
    } else {
      separatorIndex = StringPrototypeIndexOf(
        specifier, '/', separatorIndex + 1);
    }
  }

  const packageName = separatorIndex === -1 ?
    specifier : StringPrototypeSlice(specifier, 0, separatorIndex);

  // Package name cannot have leading . and cannot have percent-encoding or
  // \\ separators.
  if (RegExpPrototypeExec(invalidPackageNameRegEx, packageName) !== null) {
    validPackageName = false;
  }

  if (!validPackageName) {
    throw new ERR_INVALID_MODULE_SPECIFIER(
      specifier, 'is not a valid package name', fileURLToPath(base));
  }

  const packageSubpath = '.' + (separatorIndex === -1 ? '' :
    StringPrototypeSlice(specifier, separatorIndex));

  return { packageName, packageSubpath, isScoped };
}

/**
 * Resolves a package specifier to a URL.
 * @param {string} specifier - The package specifier to resolve.
 * @param {string | URL | undefined} base - The base URL to use for resolution.
 * @param {Set<string>} conditions - An object containing the conditions for resolution.
 * @returns {URL} - The resolved URL.
 */
function packageResolve(specifier, base, conditions) {
  if (BuiltinModule.canBeRequiredWithoutScheme(specifier)) {
    return new URL('node:' + specifier);
  }

  const { packageName, packageSubpath, isScoped } =
    parsePackageName(specifier, base);

  // ResolveSelf
  const packageConfig = getPackageScopeConfig(base);
  if (packageConfig.exists) {
    const packageJSONUrl = pathToFileURL(packageConfig.pjsonPath);
    if (packageConfig.exports != null && packageConfig.name === packageName) {
      return packageExportsResolve(
        packageJSONUrl, packageSubpath, packageConfig, base, conditions);
    }
  }

  let packageJSONUrl =
    new URL('./node_modules/' + packageName + '/package.json', base);
  let packageJSONPath = fileURLToPath(packageJSONUrl);
  let lastPath;
  do {
    const stat = internalModuleStat(toNamespacedPath(StringPrototypeSlice(packageJSONPath, 0,
                                                                          packageJSONPath.length - 13)));
    // Check for !stat.isDirectory()
    if (stat !== 1) {
      lastPath = packageJSONPath;
      packageJSONUrl = new URL((isScoped ?
        '../../../../node_modules/' : '../../../node_modules/') +
        packageName + '/package.json', packageJSONUrl);
      packageJSONPath = fileURLToPath(packageJSONUrl);
      continue;
    }

    // Package match.
    const packageConfig = packageJsonReader.read(packageJSONPath, { __proto__: null, specifier, base, isESM: true });
    if (packageConfig.exports != null) {
      return packageExportsResolve(
        packageJSONUrl, packageSubpath, packageConfig, base, conditions);
    }
    if (packageSubpath === '.') {
      return legacyMainResolve(
        packageJSONUrl,
        packageConfig,
        base,
      );
    }

    return new URL(packageSubpath, packageJSONUrl);
    // Cross-platform root check.
  } while (packageJSONPath.length !== lastPath.length);

  // eslint can't handle the above code.
  // eslint-disable-next-line no-unreachable
  throw new ERR_MODULE_NOT_FOUND(packageName, fileURLToPath(base), null);
}

/**
 * Checks if a specifier is a bare specifier.
 * @param {string} specifier - The specifier to check.
 */
function isBareSpecifier(specifier) {
  return specifier[0] && specifier[0] !== '/' && specifier[0] !== '.';
}

/**
 * Determines whether a specifier is a relative path.
 * @param {string} specifier - The specifier to check.
 */
function isRelativeSpecifier(specifier) {
  if (specifier[0] === '.') {
    if (specifier.length === 1 || specifier[1] === '/') { return true; }
    if (specifier[1] === '.') {
      if (specifier.length === 2 || specifier[2] === '/') { return true; }
    }
  }
  return false;
}

/**
 * Determines whether a specifier should be treated as a relative or absolute path.
 * @param {string} specifier - The specifier to check.
 */
function shouldBeTreatedAsRelativeOrAbsolutePath(specifier) {
  if (specifier === '') { return false; }
  if (specifier[0] === '/') { return true; }
  return isRelativeSpecifier(specifier);
}

/**
 * Resolves a module specifier to a URL.
 * @param {string} specifier - The module specifier to resolve.
 * @param {string | URL | undefined} base - The base URL to resolve against.
 * @param {Set<string>} conditions - An object containing environment conditions.
 * @param {boolean} preserveSymlinks - Whether to preserve symlinks in the resolved URL.
 */
function moduleResolve(specifier, base, conditions, preserveSymlinks) {
  const isRemote = base.protocol === 'http:' ||
    base.protocol === 'https:';
  // Order swapped from spec for minor perf gain.
  // Ok since relative URLs cannot parse as URLs.
  let resolved;
  if (shouldBeTreatedAsRelativeOrAbsolutePath(specifier)) {
    resolved = new URL(specifier, base);
  } else if (!isRemote && specifier[0] === '#') {
    resolved = packageImportsResolve(specifier, base, conditions);
  } else {
    try {
      resolved = new URL(specifier);
    } catch {
      if (!isRemote) {
        resolved = packageResolve(specifier, base, conditions);
      }
    }
  }
  if (resolved.protocol !== 'file:') {
    return resolved;
  }
  return finalizeResolution(resolved, base, preserveSymlinks);
}

/**
 * Try to resolve an import as a CommonJS module.
 * @param {string} specifier - The specifier to resolve.
 * @param {string} parentURL - The base URL.
 */
function resolveAsCommonJS(specifier, parentURL) {
  try {
    const parent = fileURLToPath(parentURL);
    const tmpModule = new CJSModule(parent, null);
    tmpModule.paths = CJSModule._nodeModulePaths(parent);

    let found = CJSModule._resolveFilename(specifier, tmpModule, false);

    // If it is a relative specifier return the relative path
    // to the parent
    if (isRelativeSpecifier(specifier)) {
      found = relative(parent, found);
      // Add '.separator if the path does not start with '..separator'
      // This should be a safe assumption because when loading
      // esm modules there should be always a file specified so
      // there should not be a specifier like '..' or '.'
      if (!StringPrototypeStartsWith(found, `..${sep}`)) {
        found = `.${sep}${found}`;
      }
    } else if (isBareSpecifier(specifier)) {
      // If it is a bare specifier return the relative path within the
      // module
      const pkg = StringPrototypeSplit(specifier, '/')[0];
      const index = StringPrototypeIndexOf(found, pkg);
      if (index !== -1) {
        found = StringPrototypeSlice(found, index);
      }
    }
    // Normalize the path separator to give a valid suggestion
    // on Windows
    if (process.platform === 'win32') {
      found = RegExpPrototypeSymbolReplace(new RegExp(`\\${sep}`, 'g'),
                                           found, '/');
    }
    return found;
  } catch {
    return false;
  }
}

/**
 * Throw an error if an import is not allowed.
 * TODO(@JakobJingleheimer): de-dupe `specifier` & `parsed`
 * @param {string} specifier - The import specifier.
 * @param {URL} parsed - The parsed URL of the import specifier.
 * @param {URL} parsedParentURL - The parsed URL of the parent module.
 * @throws {ERR_NETWORK_IMPORT_DISALLOWED} - If the import is disallowed.
 */
function checkIfDisallowedImport(specifier, parsed, parsedParentURL) {
  if (parsedParentURL) {
    // Avoid accessing the `protocol` property due to the lazy getters.
    const parentProtocol = parsedParentURL.protocol;
    if (
      parentProtocol === 'http:' ||
      parentProtocol === 'https:'
    ) {
      if (shouldBeTreatedAsRelativeOrAbsolutePath(specifier)) {
        // Avoid accessing the `protocol` property due to the lazy getters.
        const parsedProtocol = parsed?.protocol;
        // data: and blob: disallowed due to allowing file: access via
        // indirection
        if (parsedProtocol &&
          parsedProtocol !== 'https:' &&
          parsedProtocol !== 'http:'
        ) {
          throw new ERR_NETWORK_IMPORT_DISALLOWED(
            specifier,
            parsedParentURL,
            'remote imports cannot import from a local location.',
          );
        }

        return { url: parsed.href };
      }
      if (BuiltinModule.canBeRequiredWithoutScheme(specifier)) {
        throw new ERR_NETWORK_IMPORT_DISALLOWED(
          specifier,
          parsedParentURL,
          'remote imports cannot import from a local location.',
        );
      }

      throw new ERR_NETWORK_IMPORT_DISALLOWED(
        specifier,
        parsedParentURL,
        'only relative and absolute specifiers are supported.',
      );
    }
  }
}

/**
 * Validate user-input in `context` supplied by a custom loader.
 * @param {string | URL | undefined} parentURL - The parent URL.
 */
function throwIfInvalidParentURL(parentURL) {
  if (parentURL === undefined) {
    return; // Main entry point, so no parent
  }
  if (typeof parentURL !== 'string' && !isURL(parentURL)) {
    throw new ERR_INVALID_ARG_TYPE('parentURL', ['string', 'URL'], parentURL);
  }
}

/**
 * Resolves the given specifier using the provided context, which includes the parent URL and conditions.
 * Throws an error if the parent URL is invalid or if the resolution is disallowed by the policy manifest.
 * Otherwise, attempts to resolve the specifier and returns the resulting URL and format.
 * @param {string} specifier - The specifier to resolve.
 * @param {object} [context={}] - The context object containing the parent URL and conditions.
 * @param {string} [context.parentURL] - The URL of the parent module.
 * @param {string[]} [context.conditions] - The conditions for resolving the specifier.
 */
function defaultResolve(specifier, context = {}) {
  let { parentURL, conditions } = context;
  throwIfInvalidParentURL(parentURL);
  if (parentURL && policy?.manifest) {
    const redirects = policy.manifest.getDependencyMapper(parentURL);
    if (redirects) {
      const { resolve, reaction } = redirects;
      const destination = resolve(specifier, new SafeSet(conditions));
      let missing = true;
      if (destination === true) {
        missing = false;
      } else if (destination) {
        const href = destination.href;
        return { url: href };
      }
      if (missing) {
        // Prevent network requests from firing if resolution would be banned.
        // Network requests can extract data by doing things like putting
        // secrets in query params
        reaction(new ERR_MANIFEST_DEPENDENCY_MISSING(
          parentURL,
          specifier,
          ArrayPrototypeJoin([...conditions], ', ')),
        );
      }
    }
  }

  let parsedParentURL;
  if (parentURL) {
    try {
      parsedParentURL = new URL(parentURL);
    } catch {
      // Ignore exception
    }
  }

  let parsed;
  try {
    if (shouldBeTreatedAsRelativeOrAbsolutePath(specifier)) {
      parsed = new URL(specifier, parsedParentURL);
    } else {
      parsed = new URL(specifier);
    }
    // Avoid accessing the `protocol` property due to the lazy getters.
    const protocol = parsed.protocol;

    if (protocol === 'data:' &&
      parsedParentURL.protocol !== 'file:' &&
      experimentalNetworkImports) {
      throw new ERR_NETWORK_IMPORT_DISALLOWED(
        specifier,
        parsedParentURL,
        'import data: from a non file: is not allowed',
      );
    }

    if (protocol === 'data:' ||
      (experimentalNetworkImports &&
        (
          protocol === 'https:' ||
          protocol === 'http:'
        )
      )
    ) {
      return { __proto__: null, url: parsed.href };
    }
  } catch (e) {
    if (e?.code === 'ERR_NETWORK_IMPORT_DISALLOWED') {
      throw e;
    }
    // Ignore exception
  }

  // There are multiple deep branches that can either throw or return; instead
  // of duplicating that deeply nested logic for the possible returns, DRY and
  // check for a return. This seems the least gnarly.
  const maybeReturn = checkIfDisallowedImport(
    specifier,
    parsed,
    parsedParentURL,
  );

  if (maybeReturn) { return maybeReturn; }

  // This must come after checkIfDisallowedImport
  if (parsed && parsed.protocol === 'node:') { return { __proto__: null, url: specifier }; }


  const isMain = parentURL === undefined;
  if (isMain) {
    parentURL = getCWDURL().href;

    // This is the initial entry point to the program, and --input-type has
    // been passed as an option; but --input-type can only be used with
    // --eval, --print or STDIN string input. It is not allowed with file
    // input, to avoid user confusion over how expansive the effect of the
    // flag should be (i.e. entry point only, package scope surrounding the
    // entry point, etc.).
    if (inputTypeFlag) { throw new ERR_INPUT_TYPE_NOT_ALLOWED(); }
  }

  conditions = getConditionsSet(conditions);
  let url;
  try {
    url = moduleResolve(
      specifier,
      parentURL,
      conditions,
      isMain ? preserveSymlinksMain : preserveSymlinks,
    );
  } catch (error) {
    // Try to give the user a hint of what would have been the
    // resolved CommonJS module
    if (error.code === 'ERR_MODULE_NOT_FOUND' ||
        error.code === 'ERR_UNSUPPORTED_DIR_IMPORT') {
      if (StringPrototypeStartsWith(specifier, 'file://')) {
        specifier = fileURLToPath(specifier);
      }
      decorateErrorWithCommonJSHints(error, specifier, parentURL);
    }
    throw error;
  }

  return {
    // Do NOT cast `url` to a string: that will work even when there are real
    // problems, silencing them
    url: url.href,
    format: defaultGetFormatWithoutErrors(url, context),
  };
}

/**
 * Decorates the given error with a hint for CommonJS modules.
 * @param {Error} error - The error to decorate.
 * @param {string} specifier - The specifier that was attempted to be imported.
 * @param {string} parentURL - The URL of the parent module.
 */
function decorateErrorWithCommonJSHints(error, specifier, parentURL) {
  const found = resolveAsCommonJS(specifier, parentURL);
  if (found) {
    // Modify the stack and message string to include the hint
    const lines = StringPrototypeSplit(error.stack, '\n');
    const hint = `Did you mean to import ${found}?`;
    error.stack =
      ArrayPrototypeShift(lines) + '\n' +
      hint + '\n' +
      ArrayPrototypeJoin(lines, '\n');
    error.message += `\n${hint}`;
  }
}

module.exports = {
  decorateErrorWithCommonJSHints,
  defaultResolve,
  encodedSepRegEx,
  getPackageScopeConfig,
  getPackageType,
  packageExportsResolve,
  packageImportsResolve,
  throwIfInvalidParentURL,
};

// cycle
const {
  defaultGetFormatWithoutErrors,
} = require('internal/modules/esm/get_format');

if (policy) {
  const $defaultResolve = defaultResolve;
  module.exports.defaultResolve = function defaultResolve(
    specifier,
    context,
  ) {
    const ret = $defaultResolve(specifier, context);
    // This is a preflight check to avoid data exfiltration by query params etc.
    policy.manifest.mightAllow(ret.url, () =>
      new ERR_MANIFEST_DEPENDENCY_MISSING(
        context.parentURL,
        specifier,
        context.conditions,
      ),
    );
    return ret;
  };
}
