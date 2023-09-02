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

var buffer = new ArrayBuffer(8);
Debug.typedArrayDetachBuffer(buffer);
assertThrows(() => buffer.slice(2,6), TypeError);
assertThrows(() => buffer.slice(), TypeError);

var javaBuffer = java.nio.ByteBuffer.allocate(8);
buffer = new ArrayBuffer(javaBuffer);
Debug.typedArrayDetachBuffer(buffer);
assertThrows(() => buffer.slice(), TypeError);
