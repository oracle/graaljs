/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option testV8-mode
 */

load('assert.js');

const result = [];
TestV8.setTimeout(() => {
    result.push('task 1');
    Promise.resolve().then(() => result.push('microtask'));
});
TestV8.setTimeout(() => {
    result.push('task 2');
    assertSame('task 1,microtask,task 2', result.join(','));
});
