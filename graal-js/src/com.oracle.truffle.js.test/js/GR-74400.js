/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of possible internal errors.
 * 
 * @option ecmascript-version=2026
 */

load('./assert.js');

assertThrows(() => Uint8Array.fromBase64("\u0100"), SyntaxError);
assertThrows(() => new Uint8Array(8).setFromBase64("\u0100"), SyntaxError);

assertThrows(() => Uint8Array.fromHex("0\u0100"), SyntaxError);
assertThrows(() => Uint8Array.fromHex("\u01000"), SyntaxError);
assertThrows(() => new Uint8Array(8).setFromHex("0\u0100"), SyntaxError);
assertThrows(() => new Uint8Array(8).setFromHex("\u01000"), SyntaxError);
