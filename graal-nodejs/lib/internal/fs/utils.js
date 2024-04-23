'use strict';

const {
  ArrayIsArray,
  BigInt,
  Date,
  DateNow,
  DatePrototypeGetTime,
  ErrorCaptureStackTrace,
  FunctionPrototypeCall,
  Number,
  NumberIsFinite,
  MathMin,
  MathRound,
  ObjectIs,
  ObjectPrototypeHasOwnProperty,
  ObjectSetPrototypeOf,
  ReflectApply,
  ReflectOwnKeys,
  RegExpPrototypeSymbolReplace,
  StringPrototypeEndsWith,
  StringPrototypeIncludes,
  Symbol,
  TypedArrayPrototypeAt,
  TypedArrayPrototypeIncludes,
} = primordials;

const { Buffer } = require('buffer');
const {
  codes: {
    ERR_FS_EISDIR,
    ERR_FS_INVALID_SYMLINK_TYPE,
    ERR_INCOMPATIBLE_OPTION_PAIR,
    ERR_INVALID_ARG_TYPE,
    ERR_INVALID_ARG_VALUE,
    ERR_OUT_OF_RANGE,
  },
  hideStackFrames,
  uvException,
} = require('internal/errors');
const {
  isArrayBufferView,
  isBigInt64Array,
  isDate,
  isUint8Array,
} = require('internal/util/types');
const {
  kEmptyObject,
  once,
} = require('internal/util');
const { toPathIfFileURL } = require('internal/url');
const {
  validateAbortSignal,
  validateBoolean,
  validateFunction,
  validateInt32,
  validateInteger,
  validateObject,
  validateUint32,
} = require('internal/validators');
const pathModule = require('path');
const kType = Symbol('type');
const kStats = Symbol('stats');
const assert = require('internal/assert');

const {
  fs: {
    F_OK = 0,
    W_OK = 0,
    R_OK = 0,
    X_OK = 0,
    COPYFILE_EXCL,
    COPYFILE_FICLONE,
    COPYFILE_FICLONE_FORCE,
    O_APPEND,
    O_CREAT,
    O_EXCL,
    O_RDONLY,
    O_RDWR,
    O_SYNC,
    O_TRUNC,
    O_WRONLY,
    S_IFBLK,
    S_IFCHR,
    S_IFDIR,
    S_IFIFO,
    S_IFLNK,
    S_IFMT,
    S_IFREG,
    S_IFSOCK,
    UV_FS_SYMLINK_DIR,
    UV_FS_SYMLINK_JUNCTION,
    UV_DIRENT_UNKNOWN,
    UV_DIRENT_FILE,
    UV_DIRENT_DIR,
    UV_DIRENT_LINK,
    UV_DIRENT_FIFO,
    UV_DIRENT_SOCKET,
    UV_DIRENT_CHAR,
    UV_DIRENT_BLOCK,
  },
  os: {
    errno: {
      EISDIR,
    },
  },
} = internalBinding('constants');

// The access modes can be any of F_OK, R_OK, W_OK or X_OK. Some might not be
// available on specific systems. They can be used in combination as well
// (F_OK | R_OK | W_OK | X_OK).
const kMinimumAccessMode = MathMin(F_OK, W_OK, R_OK, X_OK);
const kMaximumAccessMode = F_OK | W_OK | R_OK | X_OK;

const kDefaultCopyMode = 0;
// The copy modes can be any of COPYFILE_EXCL, COPYFILE_FICLONE or
// COPYFILE_FICLONE_FORCE. They can be used in combination as well
// (COPYFILE_EXCL | COPYFILE_FICLONE | COPYFILE_FICLONE_FORCE).
const kMinimumCopyMode = MathMin(
  kDefaultCopyMode,
  COPYFILE_EXCL,
  COPYFILE_FICLONE,
  COPYFILE_FICLONE_FORCE,
);
const kMaximumCopyMode = COPYFILE_EXCL |
                         COPYFILE_FICLONE |
                         COPYFILE_FICLONE_FORCE;

// Most platforms don't allow reads or writes >= 2 GiB.
// See https://github.com/libuv/libuv/pull/1501.
const kIoMaxLength = 2 ** 31 - 1;

// Use 64kb in case the file type is not a regular file and thus do not know the
// actual file size. Increasing the value further results in more frequent over
// allocation for small files and consumes CPU time and memory that should be
// used else wise.
// Use up to 512kb per read otherwise to partition reading big files to prevent
// blocking other threads in case the available threads are all in use.
const kReadFileUnknownBufferLength = 64 * 1024;
const kReadFileBufferLength = 512 * 1024;

const kWriteFileMaxChunkSize = 512 * 1024;

const kMaxUserId = 2 ** 32 - 1;

const isWindows = process.platform === 'win32';

let fs;
function lazyLoadFs() {
  if (!fs) {
    fs = require('fs');
  }
  return fs;
}

function assertEncoding(encoding) {
  if (encoding && !Buffer.isEncoding(encoding)) {
    const reason = 'is invalid encoding';
    throw new ERR_INVALID_ARG_VALUE(encoding, 'encoding', reason);
  }
}

class Dirent {
  constructor(name, type, path, filepath = path && join(path, name)) {
    this.name = name;
    this.parentPath = path;
    this.path = filepath;
    this[kType] = type;
  }

  isDirectory() {
    return this[kType] === UV_DIRENT_DIR;
  }

  isFile() {
    return this[kType] === UV_DIRENT_FILE;
  }

  isBlockDevice() {
    return this[kType] === UV_DIRENT_BLOCK;
  }

  isCharacterDevice() {
    return this[kType] === UV_DIRENT_CHAR;
  }

  isSymbolicLink() {
    return this[kType] === UV_DIRENT_LINK;
  }

  isFIFO() {
    return this[kType] === UV_DIRENT_FIFO;
  }

  isSocket() {
    return this[kType] === UV_DIRENT_SOCKET;
  }
}

class DirentFromStats extends Dirent {
  constructor(name, stats, path, filepath) {
    super(name, null, path, filepath);
    this[kStats] = stats;
  }
}

for (const name of ReflectOwnKeys(Dirent.prototype)) {
  if (name === 'constructor') {
    continue;
  }
  DirentFromStats.prototype[name] = function() {
    return this[kStats][name]();
  };
}

function copyObject(source) {
  const target = {};
  for (const key in source)
    target[key] = source[key];
  return target;
}

const bufferSep = Buffer.from(pathModule.sep);

function join(path, name) {
  if ((typeof path === 'string' || isUint8Array(path)) &&
      name === undefined) {
    return path;
  }

  if (typeof path === 'string' && isUint8Array(name)) {
    const pathBuffer = Buffer.from(pathModule.join(path, pathModule.sep));
    return Buffer.concat([pathBuffer, name]);
  }

  if (typeof path === 'string' && typeof name === 'string') {
    return pathModule.join(path, name);
  }

  if (isUint8Array(path) && isUint8Array(name)) {
    return Buffer.concat([path, bufferSep, name]);
  }

  throw new ERR_INVALID_ARG_TYPE(
    'path', ['string', 'Buffer'], path);
}

function getDirents(path, { 0: names, 1: types }, callback) {
  let i;
  if (typeof callback === 'function') {
    const len = names.length;
    let toFinish = 0;
    callback = once(callback);
    for (i = 0; i < len; i++) {
      const type = types[i];
      if (type === UV_DIRENT_UNKNOWN) {
        const name = names[i];
        const idx = i;
        toFinish++;
        let filepath;
        try {
          filepath = join(path, name);
        } catch (err) {
          callback(err);
          return;
        }
        lazyLoadFs().lstat(filepath, (err, stats) => {
          if (err) {
            callback(err);
            return;
          }
          names[idx] = new DirentFromStats(name, stats, path, filepath);
          if (--toFinish === 0) {
            callback(null, names);
          }
        });
      } else {
        names[i] = new Dirent(names[i], types[i], path);
      }
    }
    if (toFinish === 0) {
      callback(null, names);
    }
  } else {
    const len = names.length;
    for (i = 0; i < len; i++) {
      names[i] = getDirent(path, names[i], types[i]);
    }
    return names;
  }
}

function getDirent(path, name, type, callback) {
  if (typeof callback === 'function') {
    if (type === UV_DIRENT_UNKNOWN) {
      let filepath;
      try {
        filepath = join(path, name);
      } catch (err) {
        callback(err);
        return;
      }
      lazyLoadFs().lstat(filepath, (err, stats) => {
        if (err) {
          callback(err);
          return;
        }
        callback(null, new DirentFromStats(name, stats, path, filepath));
      });
    } else {
      callback(null, new Dirent(name, type, path));
    }
  } else if (type === UV_DIRENT_UNKNOWN) {
    const filepath = join(path, name);
    const stats = lazyLoadFs().lstatSync(filepath);
    // callback === true: Quirk to not introduce a breaking change.
    return new DirentFromStats(name, stats, path, callback === true ? filepath : path);
  } else if (callback === true) {
    // callback === true: Quirk to not introduce a breaking change.
    return new Dirent(name, type, path);
  } else {
    return new Dirent(name, type, path, path);
  }
}

function getOptions(options, defaultOptions = kEmptyObject) {
  if (options == null || typeof options === 'function') {
    return defaultOptions;
  }

  if (typeof options === 'string') {
    defaultOptions = { ...defaultOptions };
    defaultOptions.encoding = options;
    options = defaultOptions;
  } else if (typeof options !== 'object') {
    throw new ERR_INVALID_ARG_TYPE('options', ['string', 'Object'], options);
  }

  if (options.encoding !== 'buffer')
    assertEncoding(options.encoding);

  if (options.signal !== undefined) {
    validateAbortSignal(options.signal, 'options.signal');
  }

  return options;
}

/**
 * @param {InternalFSBinding.FSSyncContext} ctx
 */
function handleErrorFromBinding(ctx) {
  if (ctx.errno !== undefined) {  // libuv error numbers
    const err = uvException(ctx);
    ErrorCaptureStackTrace(err, handleErrorFromBinding);
    throw err;
  }
  if (ctx.error !== undefined) {  // Errors created in C++ land.
    // TODO(joyeecheung): currently, ctx.error are encoding errors
    // usually caused by memory problems. We need to figure out proper error
    // code(s) for this.
    ErrorCaptureStackTrace(ctx.error, handleErrorFromBinding);
    throw ctx.error;
  }
}

// Check if the path contains null types if it is a string nor Uint8Array,
// otherwise return silently.
const nullCheck = hideStackFrames((path, propName, throwError = true) => {
  const pathIsString = typeof path === 'string';
  const pathIsUint8Array = isUint8Array(path);

  // We can only perform meaningful checks on strings and Uint8Arrays.
  if ((!pathIsString && !pathIsUint8Array) ||
      (pathIsString && !StringPrototypeIncludes(path, '\u0000')) ||
      (pathIsUint8Array && !TypedArrayPrototypeIncludes(path, 0))) {
    return;
  }

  const err = new ERR_INVALID_ARG_VALUE(
    propName,
    path,
    'must be a string or Uint8Array without null bytes',
  );
  if (throwError) {
    throw err;
  }
  return err;
});

function preprocessSymlinkDestination(path, type, linkPath) {
  if (!isWindows) {
    // No preprocessing is needed on Unix.
    return path;
  }
  path = '' + path;
  if (type === 'junction') {
    // Junctions paths need to be absolute and \\?\-prefixed.
    // A relative target is relative to the link's parent directory.
    path = pathModule.resolve(linkPath, '..', path);
    return pathModule.toNamespacedPath(path);
  }
  if (pathModule.isAbsolute(path)) {
    // If the path is absolute, use the \\?\-prefix to enable long filenames
    return pathModule.toNamespacedPath(path);
  }
  // Windows symlinks don't tolerate forward slashes.
  return RegExpPrototypeSymbolReplace(/\//g, path, '\\');
}

// Constructor for file stats.
function StatsBase(dev, mode, nlink, uid, gid, rdev, blksize,
                   ino, size, blocks) {
  this.dev = dev;
  this.mode = mode;
  this.nlink = nlink;
  this.uid = uid;
  this.gid = gid;
  this.rdev = rdev;
  this.blksize = blksize;
  this.ino = ino;
  this.size = size;
  this.blocks = blocks;
}

StatsBase.prototype.isDirectory = function() {
  return this._checkModeProperty(S_IFDIR);
};

StatsBase.prototype.isFile = function() {
  return this._checkModeProperty(S_IFREG);
};

StatsBase.prototype.isBlockDevice = function() {
  return this._checkModeProperty(S_IFBLK);
};

StatsBase.prototype.isCharacterDevice = function() {
  return this._checkModeProperty(S_IFCHR);
};

StatsBase.prototype.isSymbolicLink = function() {
  return this._checkModeProperty(S_IFLNK);
};

StatsBase.prototype.isFIFO = function() {
  return this._checkModeProperty(S_IFIFO);
};

StatsBase.prototype.isSocket = function() {
  return this._checkModeProperty(S_IFSOCK);
};

const kNsPerMsBigInt = 10n ** 6n;
const kNsPerSecBigInt = 10n ** 9n;
const kMsPerSec = 10 ** 3;
const kNsPerMs = 10 ** 6;
function msFromTimeSpec(sec, nsec) {
  return sec * kMsPerSec + nsec / kNsPerMs;
}

function nsFromTimeSpecBigInt(sec, nsec) {
  return sec * kNsPerSecBigInt + nsec;
}

// The Date constructor performs Math.floor() on the absolute value
// of the timestamp: https://tc39.es/ecma262/#sec-timeclip
// Since there may be a precision loss when the timestamp is
// converted to a floating point number, we manually round
// the timestamp here before passing it to Date().
// Refs: https://github.com/nodejs/node/pull/12607
// Refs: https://github.com/nodejs/node/pull/43714
function dateFromMs(ms) {
  // Coercing to number, ms can be bigint
  return new Date(MathRound(Number(ms)));
}

function BigIntStats(dev, mode, nlink, uid, gid, rdev, blksize,
                     ino, size, blocks,
                     atimeNs, mtimeNs, ctimeNs, birthtimeNs) {
  ReflectApply(StatsBase, this, [dev, mode, nlink, uid, gid, rdev, blksize,
                                 ino, size, blocks]);

  this.atimeMs = atimeNs / kNsPerMsBigInt;
  this.mtimeMs = mtimeNs / kNsPerMsBigInt;
  this.ctimeMs = ctimeNs / kNsPerMsBigInt;
  this.birthtimeMs = birthtimeNs / kNsPerMsBigInt;
  this.atimeNs = atimeNs;
  this.mtimeNs = mtimeNs;
  this.ctimeNs = ctimeNs;
  this.birthtimeNs = birthtimeNs;
  this.atime = dateFromMs(this.atimeMs);
  this.mtime = dateFromMs(this.mtimeMs);
  this.ctime = dateFromMs(this.ctimeMs);
  this.birthtime = dateFromMs(this.birthtimeMs);
}

ObjectSetPrototypeOf(BigIntStats.prototype, StatsBase.prototype);
ObjectSetPrototypeOf(BigIntStats, StatsBase);

BigIntStats.prototype._checkModeProperty = function(property) {
  if (isWindows && (property === S_IFIFO || property === S_IFBLK ||
    property === S_IFSOCK)) {
    return false;  // Some types are not available on Windows
  }
  return (this.mode & BigInt(S_IFMT)) === BigInt(property);
};

function Stats(dev, mode, nlink, uid, gid, rdev, blksize,
               ino, size, blocks,
               atimeMs, mtimeMs, ctimeMs, birthtimeMs) {
  FunctionPrototypeCall(StatsBase, this, dev, mode, nlink, uid, gid, rdev,
                        blksize, ino, size, blocks);
  this.atimeMs = atimeMs;
  this.mtimeMs = mtimeMs;
  this.ctimeMs = ctimeMs;
  this.birthtimeMs = birthtimeMs;
  this.atime = dateFromMs(atimeMs);
  this.mtime = dateFromMs(mtimeMs);
  this.ctime = dateFromMs(ctimeMs);
  this.birthtime = dateFromMs(birthtimeMs);
}

ObjectSetPrototypeOf(Stats.prototype, StatsBase.prototype);
ObjectSetPrototypeOf(Stats, StatsBase);

// HACK: Workaround for https://github.com/standard-things/esm/issues/821.
// TODO(ronag): Remove this as soon as `esm` publishes a fixed version.
Stats.prototype.isFile = StatsBase.prototype.isFile;

Stats.prototype._checkModeProperty = function(property) {
  if (isWindows && (property === S_IFIFO || property === S_IFBLK ||
    property === S_IFSOCK)) {
    return false;  // Some types are not available on Windows
  }
  return (this.mode & S_IFMT) === property;
};

/**
 * @param {Float64Array | BigInt64Array} stats
 * @param {number} offset
 * @returns {BigIntStats | Stats}
 */
function getStatsFromBinding(stats, offset = 0) {
  if (isBigInt64Array(stats)) {
    return new BigIntStats(
      stats[0 + offset], stats[1 + offset], stats[2 + offset],
      stats[3 + offset], stats[4 + offset], stats[5 + offset],
      stats[6 + offset], stats[7 + offset], stats[8 + offset],
      stats[9 + offset],
      nsFromTimeSpecBigInt(stats[10 + offset], stats[11 + offset]),
      nsFromTimeSpecBigInt(stats[12 + offset], stats[13 + offset]),
      nsFromTimeSpecBigInt(stats[14 + offset], stats[15 + offset]),
      nsFromTimeSpecBigInt(stats[16 + offset], stats[17 + offset]),
    );
  }
  return new Stats(
    stats[0 + offset], stats[1 + offset], stats[2 + offset],
    stats[3 + offset], stats[4 + offset], stats[5 + offset],
    stats[6 + offset], stats[7 + offset], stats[8 + offset],
    stats[9 + offset],
    msFromTimeSpec(stats[10 + offset], stats[11 + offset]),
    msFromTimeSpec(stats[12 + offset], stats[13 + offset]),
    msFromTimeSpec(stats[14 + offset], stats[15 + offset]),
    msFromTimeSpec(stats[16 + offset], stats[17 + offset]),
  );
}

class StatFs {
  constructor(type, bsize, blocks, bfree, bavail, files, ffree) {
    this.type = type;
    this.bsize = bsize;
    this.blocks = blocks;
    this.bfree = bfree;
    this.bavail = bavail;
    this.files = files;
    this.ffree = ffree;
  }
}

function getStatFsFromBinding(stats) {
  return new StatFs(
    stats[0], stats[1], stats[2], stats[3], stats[4], stats[5], stats[6],
  );
}

function stringToFlags(flags, name = 'flags') {
  if (typeof flags === 'number') {
    validateInt32(flags, name);
    return flags;
  }

  if (flags == null) {
    return O_RDONLY;
  }

  switch (flags) {
    case 'r' : return O_RDONLY;
    case 'rs' : // Fall through.
    case 'sr' : return O_RDONLY | O_SYNC;
    case 'r+' : return O_RDWR;
    case 'rs+' : // Fall through.
    case 'sr+' : return O_RDWR | O_SYNC;

    case 'w' : return O_TRUNC | O_CREAT | O_WRONLY;
    case 'wx' : // Fall through.
    case 'xw' : return O_TRUNC | O_CREAT | O_WRONLY | O_EXCL;

    case 'w+' : return O_TRUNC | O_CREAT | O_RDWR;
    case 'wx+': // Fall through.
    case 'xw+': return O_TRUNC | O_CREAT | O_RDWR | O_EXCL;

    case 'a' : return O_APPEND | O_CREAT | O_WRONLY;
    case 'ax' : // Fall through.
    case 'xa' : return O_APPEND | O_CREAT | O_WRONLY | O_EXCL;
    case 'as' : // Fall through.
    case 'sa' : return O_APPEND | O_CREAT | O_WRONLY | O_SYNC;

    case 'a+' : return O_APPEND | O_CREAT | O_RDWR;
    case 'ax+': // Fall through.
    case 'xa+': return O_APPEND | O_CREAT | O_RDWR | O_EXCL;
    case 'as+': // Fall through.
    case 'sa+': return O_APPEND | O_CREAT | O_RDWR | O_SYNC;
  }

  throw new ERR_INVALID_ARG_VALUE('flags', flags);
}

const stringToSymlinkType = hideStackFrames((type) => {
  let flags = 0;
  if (typeof type === 'string') {
    switch (type) {
      case 'dir':
        flags |= UV_FS_SYMLINK_DIR;
        break;
      case 'junction':
        flags |= UV_FS_SYMLINK_JUNCTION;
        break;
      case 'file':
        break;
      default:
        throw new ERR_FS_INVALID_SYMLINK_TYPE(type);
    }
  }
  return flags;
});

// converts Date or number to a fractional UNIX timestamp
function toUnixTimestamp(time, name = 'time') {
  // eslint-disable-next-line eqeqeq
  if (typeof time === 'string' && +time == time) {
    return +time;
  }
  if (NumberIsFinite(time)) {
    if (time < 0) {
      return DateNow() / 1000;
    }
    return time;
  }
  if (isDate(time)) {
    // Convert to 123.456 UNIX timestamp
    return DatePrototypeGetTime(time) / 1000;
  }
  throw new ERR_INVALID_ARG_TYPE(name, ['Date', 'Time in seconds'], time);
}

const validateOffsetLengthRead = hideStackFrames(
  (offset, length, bufferLength) => {
    if (offset < 0) {
      throw new ERR_OUT_OF_RANGE('offset', '>= 0', offset);
    }
    if (length < 0) {
      throw new ERR_OUT_OF_RANGE('length', '>= 0', length);
    }
    if (offset + length > bufferLength) {
      throw new ERR_OUT_OF_RANGE('length',
                                 `<= ${bufferLength - offset}`, length);
    }
  },
);

const validateOffsetLengthWrite = hideStackFrames(
  (offset, length, byteLength) => {
    if (offset > byteLength) {
      throw new ERR_OUT_OF_RANGE('offset', `<= ${byteLength}`, offset);
    }

    if (length > byteLength - offset) {
      throw new ERR_OUT_OF_RANGE('length', `<= ${byteLength - offset}`, length);
    }

    if (length < 0) {
      throw new ERR_OUT_OF_RANGE('length', '>= 0', length);
    }

    validateInt32(length, 'length', 0);
  },
);

const validatePath = hideStackFrames((path, propName = 'path') => {
  if (typeof path !== 'string' && !isUint8Array(path)) {
    throw new ERR_INVALID_ARG_TYPE(propName, ['string', 'Buffer', 'URL'], path);
  }

  const err = nullCheck(path, propName, false);

  if (err !== undefined) {
    throw err;
  }
});

const getValidatedPath = hideStackFrames((fileURLOrPath, propName = 'path') => {
  const path = toPathIfFileURL(fileURLOrPath);
  validatePath(path, propName);
  return path;
});

const getValidatedFd = hideStackFrames((fd, propName = 'fd') => {
  if (ObjectIs(fd, -0)) {
    return 0;
  }

  validateInt32(fd, propName, 0);

  return fd;
});

const validateBufferArray = hideStackFrames((buffers, propName = 'buffers') => {
  if (!ArrayIsArray(buffers))
    throw new ERR_INVALID_ARG_TYPE(propName, 'ArrayBufferView[]', buffers);

  for (let i = 0; i < buffers.length; i++) {
    if (!isArrayBufferView(buffers[i]))
      throw new ERR_INVALID_ARG_TYPE(propName, 'ArrayBufferView[]', buffers);
  }

  return buffers;
});

let nonPortableTemplateWarn = true;

function warnOnNonPortableTemplate(template) {
  // Template strings passed to the mkdtemp() family of functions should not
  // end with 'X' because they are handled inconsistently across platforms.
  if (nonPortableTemplateWarn &&
    ((typeof template === 'string' && StringPrototypeEndsWith(template, 'X')) ||
    (typeof template !== 'string' && TypedArrayPrototypeAt(template, -1) === 0x58))) {
    process.emitWarning('mkdtemp() templates ending with X are not portable. ' +
                        'For details see: https://nodejs.org/api/fs.html');
    nonPortableTemplateWarn = false;
  }
}

const defaultCpOptions = {
  dereference: false,
  errorOnExist: false,
  filter: undefined,
  force: true,
  preserveTimestamps: false,
  recursive: false,
  verbatimSymlinks: false,
};

const defaultRmOptions = {
  recursive: false,
  force: false,
  retryDelay: 100,
  maxRetries: 0,
};

const defaultRmdirOptions = {
  retryDelay: 100,
  maxRetries: 0,
  recursive: false,
};

const validateCpOptions = hideStackFrames((options) => {
  if (options === undefined)
    return { ...defaultCpOptions };
  validateObject(options, 'options');
  options = { ...defaultCpOptions, ...options };
  validateBoolean(options.dereference, 'options.dereference');
  validateBoolean(options.errorOnExist, 'options.errorOnExist');
  validateBoolean(options.force, 'options.force');
  validateBoolean(options.preserveTimestamps, 'options.preserveTimestamps');
  validateBoolean(options.recursive, 'options.recursive');
  validateBoolean(options.verbatimSymlinks, 'options.verbatimSymlinks');
  options.mode = getValidMode(options.mode, 'copyFile');
  if (options.dereference === true && options.verbatimSymlinks === true) {
    throw new ERR_INCOMPATIBLE_OPTION_PAIR('dereference', 'verbatimSymlinks');
  }
  if (options.filter !== undefined) {
    validateFunction(options.filter, 'options.filter');
  }
  return options;
});

const validateRmOptions = hideStackFrames((path, options, expectDir, cb) => {
  options = validateRmdirOptions(options, defaultRmOptions);
  validateBoolean(options.force, 'options.force');

  lazyLoadFs().lstat(path, (err, stats) => {
    if (err) {
      if (options.force && err.code === 'ENOENT') {
        return cb(null, options);
      }
      return cb(err, options);
    }

    if (expectDir && !stats.isDirectory()) {
      return cb(false);
    }

    if (stats.isDirectory() && !options.recursive) {
      return cb(new ERR_FS_EISDIR({
        code: 'EISDIR',
        message: 'is a directory',
        path,
        syscall: 'rm',
        errno: EISDIR,
      }));
    }
    return cb(null, options);
  });
});

const validateRmOptionsSync = hideStackFrames((path, options, expectDir) => {
  options = validateRmdirOptions(options, defaultRmOptions);
  validateBoolean(options.force, 'options.force');

  if (!options.force || expectDir || !options.recursive) {
    const isDirectory = lazyLoadFs()
      .lstatSync(path, { throwIfNoEntry: !options.force })?.isDirectory();

    if (expectDir && !isDirectory) {
      return false;
    }

    if (isDirectory && !options.recursive) {
      throw new ERR_FS_EISDIR({
        code: 'EISDIR',
        message: 'is a directory',
        path,
        syscall: 'rm',
        errno: EISDIR,
      });
    }
  }

  return options;
});

let recursiveRmdirWarned = process.noDeprecation;
function emitRecursiveRmdirWarning() {
  if (!recursiveRmdirWarned) {
    process.emitWarning(
      'In future versions of Node.js, fs.rmdir(path, { recursive: true }) ' +
      'will be removed. Use fs.rm(path, { recursive: true }) instead',
      'DeprecationWarning',
      'DEP0147',
    );
    recursiveRmdirWarned = true;
  }
}

const validateRmdirOptions = hideStackFrames(
  (options, defaults = defaultRmdirOptions) => {
    if (options === undefined)
      return defaults;
    validateObject(options, 'options');

    options = { ...defaults, ...options };

    validateBoolean(options.recursive, 'options.recursive');
    validateInt32(options.retryDelay, 'options.retryDelay', 0);
    validateUint32(options.maxRetries, 'options.maxRetries');

    return options;
  });

const getValidMode = hideStackFrames((mode, type) => {
  let min = kMinimumAccessMode;
  let max = kMaximumAccessMode;
  let def = F_OK;
  if (type === 'copyFile') {
    min = kMinimumCopyMode;
    max = kMaximumCopyMode;
    def = mode || kDefaultCopyMode;
  } else {
    assert(type === 'access');
  }
  if (mode == null) {
    return def;
  }
  validateInteger(mode, 'mode', min, max);
  return mode;
});

const validateStringAfterArrayBufferView = hideStackFrames((buffer, name) => {
  if (typeof buffer === 'string') {
    return;
  }

  if (
    typeof buffer === 'object' &&
    buffer !== null &&
    typeof buffer.toString === 'function' &&
    ObjectPrototypeHasOwnProperty(buffer, 'toString')
  ) {
    return;
  }

  throw new ERR_INVALID_ARG_TYPE(
    name,
    ['string', 'Buffer', 'TypedArray', 'DataView'],
    buffer,
  );
});

const validatePrimitiveStringAfterArrayBufferView = hideStackFrames((buffer, name) => {
  if (typeof buffer !== 'string') {
    throw new ERR_INVALID_ARG_TYPE(
      name,
      ['string', 'Buffer', 'TypedArray', 'DataView'],
      buffer,
    );
  }
});

const validatePosition = hideStackFrames((position, name) => {
  if (typeof position === 'number') {
    validateInteger(position, name);
  } else if (typeof position === 'bigint') {
    if (!(position >= -(2n ** 63n) && position <= 2n ** 63n - 1n)) {
      throw new ERR_OUT_OF_RANGE(name,
                                 `>= ${-(2n ** 63n)} && <= ${2n ** 63n - 1n}`,
                                 position);
    }
  } else {
    throw new ERR_INVALID_ARG_TYPE(name, ['integer', 'bigint'], position);
  }
});

module.exports = {
  constants: {
    kIoMaxLength,
    kMaxUserId,
    kReadFileBufferLength,
    kReadFileUnknownBufferLength,
    kWriteFileMaxChunkSize,
  },
  assertEncoding,
  BigIntStats,  // for testing
  copyObject,
  Dirent,
  emitRecursiveRmdirWarning,
  getDirent,
  getDirents,
  getOptions,
  getValidatedFd,
  getValidatedPath,
  getValidMode,
  handleErrorFromBinding,
  nullCheck,
  preprocessSymlinkDestination,
  realpathCacheKey: Symbol('realpathCacheKey'),
  getStatFsFromBinding,
  getStatsFromBinding,
  stringToFlags,
  stringToSymlinkType,
  Stats,
  toUnixTimestamp,
  validateBufferArray,
  validateCpOptions,
  validateOffsetLengthRead,
  validateOffsetLengthWrite,
  validatePath,
  validatePosition,
  validateRmOptions,
  validateRmOptionsSync,
  validateRmdirOptions,
  validateStringAfterArrayBufferView,
  validatePrimitiveStringAfterArrayBufferView,
  warnOnNonPortableTemplate,
};
