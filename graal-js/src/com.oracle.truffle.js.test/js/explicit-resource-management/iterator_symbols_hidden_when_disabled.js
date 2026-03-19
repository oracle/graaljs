/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 *
 * @option iterator-helpers
 * @option async-iterator-helpers
 * @option explicit-resource-management=false
 */

load('../assert.js');

assertFalse(Object.getOwnPropertySymbols(Iterator.prototype).some(symbol => String(symbol) === 'Symbol.dispose'));
assertFalse(Object.getOwnPropertySymbols(AsyncIterator.prototype).some(symbol => String(symbol) === 'Symbol.asyncDispose'));
