/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * continue labels should not pass through blocks
 * See: https://github.com/tc39/ecma262/pull/2482
 */

load('assert.js');

assertThrows(() => new Function(`let a = 0; lbl: { for (;;) { if (a == 0) { break } else { a += 1; continue lbl; } } }`), SyntaxError);
