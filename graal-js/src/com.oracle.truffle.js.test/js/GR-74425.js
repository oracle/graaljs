/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('./assert.js');

var buf = new ArrayBuffer(8, {maxByteLength: 16});
new Uint8Array(buf).fill(1);
var buf2 = buf.transfer(4);
buf2.resize(8);
assertSameContent([1,1,1,1,0,0,0,0], new Uint8Array(buf2));
