/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Test of properties of foreign errors.
 *
 * @option foreign-object-prototype
 */

load('../assert.js');

function throwJavaError(message) {
    throw new java.lang.RuntimeException(message);
}

var message = 'someMessage';
try {
    throwJavaError(message);
    assert.fail('should have thrown');
} catch (e) {
    assertTrue(Java.isJavaObject(e));
    assertTrue(e instanceof Error);
    assertSame(message, e.message);
    assertTrue(e.stack.includes('foreign_error_prototype'));
    // assertTrue(e.stack.includes('throwJavaError'));
    assertSame('Error: ' + message, Error.prototype.toString.call(e));
}
