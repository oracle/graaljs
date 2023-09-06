/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option debug-builtin
 */

load("assert.js");

var array = new Uint8Array(8);
Debug.typedArrayDetachBuffer(array.buffer);

assertThrows(function() {
    Object.getOwnPropertyDescriptor(SharedArrayBuffer.prototype, 'byteLength').get.call(array);
}, TypeError);

assertSameContent([], Object.getOwnPropertyNames(array));
