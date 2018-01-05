/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Annex B "Additional ECMAScript Features for Web Browsers" compatibility functions.
 *
 * These need to be non-strict to ensure correct this binding.
 */

"use strict";

(function(){

function DefineOwnPropertyNoThrow(o, p, d) {
  try {
    Object.defineProperty(o, p, d);
  } catch (e) {
    // swallow TypeError to emulate [[DefineOwnProperty]] with Throw=false
    if (!(e instanceof TypeError)) {
      throw e;
    }
  }
}

function DefineOwnPropertyOrThrow(o, p, d) {
  Object.defineProperty(o, p, d);
}

function __defineGetter__(prop, func) {
  if (typeof func != 'function') {
    throw new TypeError("Object.prototype.__defineGetter__: Expecting function");
  }
  DefineOwnPropertyOrThrow(Internal.ToObject(this), prop, { get: func, enumerable: true, configurable: true });
}

function __defineSetter__(prop, func) {
  if (typeof func != 'function') {
    throw new TypeError("Object.prototype.__defineSetter__: Expecting function");
  }
  DefineOwnPropertyOrThrow(Internal.ToObject(this), prop, { set: func, enumerable: true, configurable: true });
}

function __lookupGetter__(prop) {
  var obj = Internal.ToObject(this);
  var key = Internal.ToPropertyKey(prop);
  do {
    var desc = Object.getOwnPropertyDescriptor(obj, key);
    if (desc) {
      return desc.get;
    }
    obj = Object.getPrototypeOf(obj);
  } while (obj !== null);
}

function __lookupSetter__(prop) {
  var obj = Internal.ToObject(this);
  var key = Internal.ToPropertyKey(prop);
  do {
    var desc = Object.getOwnPropertyDescriptor(obj, key);
    if (desc) {
      return desc.set;
    }
    obj = Object.getPrototypeOf(obj);
  } while (obj !== null);
}

Internal.CreateMethodProperty(Object.prototype, "__defineGetter__", __defineGetter__);
Internal.CreateMethodProperty(Object.prototype, "__defineSetter__", __defineSetter__);
Internal.CreateMethodProperty(Object.prototype, "__lookupGetter__", __lookupGetter__);
Internal.CreateMethodProperty(Object.prototype, "__lookupSetter__", __lookupSetter__);

Internal.CreateMethodProperty(Date.prototype, "toGMTString", Date.prototype.toUTCString);

})();
