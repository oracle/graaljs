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

function throwJavaErrorWithCause(message, cause) {
    throw new java.lang.RuntimeException(message, cause);
}

function assertJavaError(e) {
    assertTrue(Java.isJavaObject(e));
    assertTrue(e instanceof Error);
    assertTrue(e instanceof java.lang.RuntimeException);
}

var message = 'someMessage';
var otherMessage = 'someOtherMessage';

try {
    throwJavaError(message);
    fail('should have thrown');
} catch (e) {
    assertJavaError(e);
    assertSame(message, e.message);
    assertTrue(e.stack.includes('foreign_error_prototype'));
    assertTrue(e.stack.includes('throwJavaError'));
    assertSame('Error: ' + message, Error.prototype.toString.call(e));

    assertSame(undefined, e.cause);
}

try {
    throwJavaError(message);
    fail('should have thrown');
} catch (cause) {
    try {
        throwJavaErrorWithCause(otherMessage, cause);
        fail('should have thrown');
    } catch (e) {
        assertJavaError(e);
        assertSame(otherMessage, e.message);
        assertTrue(e.stack.includes('foreign_error_prototype'));
        assertTrue(e.stack.includes('throwJavaErrorWithCause'));
        assertSame('Error: ' + otherMessage, Error.prototype.toString.call(e));

        assertSame(cause, e.cause);

        e = e.cause;
        assertJavaError(e);
        assertSame(message, e.message);
        assertTrue(e.stack.includes('foreign_error_prototype'));
        assertTrue(e.stack.includes('throwJavaError'));
        assertSame('Error: ' + message, Error.prototype.toString.call(e));

        assertSame(undefined, e.cause);
    }
}

class JSError extends Error {}

function throwJSErrorWithCause(message, cause) {
    throw new JSError(message, {cause});
}

try {
    throwJavaError(message);
    fail('should have thrown');
} catch (cause) {
    try {
        throwJSErrorWithCause(otherMessage, cause);
        fail('should have thrown');
    } catch (e) {
        assertTrue(e instanceof JSError);
        assertSame(otherMessage, e.message);
        assertTrue(e.stack.includes('foreign_error_prototype'));
        assertTrue(e.stack.includes('throwJSError'));

        assertSame(cause, e.cause);

        e = e.cause;
        assertJavaError(e);
        assertSame(message, e.message);
        assertTrue(e.stack.includes('foreign_error_prototype'));
        assertTrue(e.stack.includes('throwJavaError'));
        assertSame('Error: ' + message, Error.prototype.toString.call(e));

        assertSame(undefined, e.cause);
    }
}
