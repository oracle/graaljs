/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 *
 * @option v8-compat
 */

load('../assert.js');

let disposed = 0;
{
    using x = { [Symbol.dispose]() { disposed++; } };
}
assertSame(1, disposed);
assertSame('function', typeof DisposableStack);
assertSame('symbol', typeof Symbol.dispose);
