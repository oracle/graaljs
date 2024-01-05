/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * An example of how to implement a custom delete on a foreign array
 * (by wrapping in a JS proxy with deleteProperty trap).
 */

load("assert.js");

const array = new java.util.ArrayList();
array.add('first');
array.add('second');
array.add('third');

// Elements of foreign array cannot be deleted
delete array[1];
assertSame('second', array[1]);

var handler = {};
handler.deleteProperty = function(target, prop) {
  const index = Number(prop);
  if (0 <= index && index < target.length) {
    target[prop] = 'deleted';
    return true;
  } else {
    return Reflect.deleteProperty(target, prop);
  }
};
const proxy = new Proxy(array, handler);

// deleteProperty trap of the proxy intercepts the delete
// and allows its custom implementation
delete proxy[1];
assertSame('deleted', proxy[1]);
assertSame('deleted', array[1]);
