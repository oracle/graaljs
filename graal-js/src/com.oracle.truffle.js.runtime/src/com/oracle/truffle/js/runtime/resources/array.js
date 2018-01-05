/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

"use strict";

(function(){

// ECMAScript 6
if (typeof Symbol !== 'undefined' && typeof Symbol.unscopables !== 'undefined') {
  var unscopables = {__proto__: null, copyWithin: true, entries: true, fill: true, find: true, findIndex: true, includes: true, keys: true, values: true };
  Internal.ObjectDefineProperty(Array.prototype, Symbol.unscopables, {value: unscopables, writable: false, enumerable: false, configurable: true});
}

})();
