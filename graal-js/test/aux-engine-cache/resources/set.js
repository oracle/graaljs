/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// This testcase derived from Node.js' `primordials.js` internal file.
const { bind, call } = Function.prototype;
const uncurryThis = bind.bind(call);

function copyProps(src, dest) {
  for (const key of Reflect.ownKeys(src)) {
    if (!Reflect.getOwnPropertyDescriptor(dest, key)) {
      Reflect.defineProperty(
        dest,
        key,
        Reflect.getOwnPropertyDescriptor(src, key));
    }
  }
}

const createSafeIterator = (factory, next) => {
  class SafeIterator {
    constructor(iterable) {
      this._iterator = factory(iterable);
    }
    next() {
      return next(this._iterator);
    }
    [Symbol.iterator]() {
      return this;
    }
  }
  Object.setPrototypeOf(SafeIterator.prototype, null);
  Object.freeze(SafeIterator.prototype);
  Object.freeze(SafeIterator);
  return SafeIterator;
};


function makeSafe(unsafe, safe) {
  if (Symbol.iterator in unsafe.prototype) {
    const dummy = new unsafe();
    let next;

    for (const key of Reflect.ownKeys(unsafe.prototype)) {
      if (!Reflect.getOwnPropertyDescriptor(safe.prototype, key)) {
        const desc = Reflect.getOwnPropertyDescriptor(unsafe.prototype, key);
        if (
          typeof desc.value === 'function' &&
          desc.value.length === 0 &&
          Symbol.iterator in (desc.value.call(dummy) ?? {})
        ) {
          const createIterator = uncurryThis(desc.value);
          if (next == null) next = uncurryThis(createIterator(dummy).next);
          const SafeIterator = createSafeIterator(createIterator, next);
          desc.value = function() {
            return new SafeIterator(this);
          };
        }
        Reflect.defineProperty(safe.prototype, key, desc);
      }
    }
  } else {
    copyProps(unsafe.prototype, safe.prototype);
  }
  copyProps(unsafe, safe);

  Object.setPrototypeOf(safe.prototype, null);
  Object.freeze(safe.prototype);
  Object.freeze(safe);
  return safe;
}


var SafeSet = makeSafe(
  Set,
  class SafeSet extends Set {
    constructor(i) { super(i); }
  }
);

for (var i = 0; i < 1000; i++) {

  var userConditions =[];
  const cjsConditions = new SafeSet(['require', 'node', ...userConditions]);

  var userConditions = [];
  var DEFAULT_CONDITIONS = Object.freeze(['node', 'import', ...userConditions]);
  var DEFAULT_CONDITIONS_SET = new SafeSet(DEFAULT_CONDITIONS);

  var internalBindingAllowlist = new SafeSet([
    'async_wrap',
    'buffer',
    'cares_wrap',
    'config',
    'constants',
    'contextify',
    'crypto',
    'fs',
    'fs_event_wrap',
    'http_parser',
    'icu',
    'inspector',
    'js_stream',
    'natives',
    'os',
    'pipe_wrap',
    'process_wrap',
    'signal_wrap',
    'spawn_sync',
    'stream_wrap',
    'tcp_wrap',
    'tls_wrap',
    'tty_wrap',
    'udp_wrap',
    'url',
    'util',
    'uv',
    'v8',
    'zlib'
  ]);
}
