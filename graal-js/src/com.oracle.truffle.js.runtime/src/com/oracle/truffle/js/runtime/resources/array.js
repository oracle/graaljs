/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

"use strict";

(function(){

// ECMAScript 6
if (typeof Symbol !== 'undefined' && typeof Symbol.unscopables !== 'undefined') {
  var unscopables = {__proto__: null, copyWithin: true, entries: true, fill: true, find: true, findIndex: true, includes: true, keys: true, values: true };
  Internal.ObjectDefineProperty(Array.prototype, Symbol.unscopables, {value: unscopables, writable: false, enumerable: false, configurable: true});
}

})();
