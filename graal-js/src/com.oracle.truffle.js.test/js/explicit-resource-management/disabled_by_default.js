/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

assertThrows(() => eval('{ using x = null; }'), SyntaxError);
assertSame('undefined', typeof DisposableStack);
assertSame('undefined', typeof Symbol.dispose);
