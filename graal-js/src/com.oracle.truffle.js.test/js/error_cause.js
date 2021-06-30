/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test for checking the order of properties in AggregateError.
 *
 * @option error-cause=true
 */

load('assert.js');

try {
    throw new Error('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new Error('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new Error('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new RangeError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new RangeError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new RangeError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new TypeError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new TypeError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new TypeError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new ReferenceError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new ReferenceError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new ReferenceError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new SyntaxError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new SyntaxError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new SyntaxError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new EvalError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new EvalError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new EvalError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new URIError('message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new URIError('message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new URIError('message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new AggregateError([],'message', { cause: 'test'});
} catch(e) {
    assertSame('message', e.message);
    assertSame('test', e.cause);
}

try {
    throw new AggregateError([], 'message', 'test');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new AggregateError([new Error('message1')], 'message2', { cause: 'test'});
} catch(e) {
    assertSame('message2', e.message);
    assertSame('test', e.cause);
    assertSame('message1', e.errors[0].message);
    assertSame(undefined, e.errors[0].cause);
}

try {
    throw new AggregateError([new Error('message1', { cause: 'test1'})], 'message2', { cause: 'test2'});
} catch(e) {
    assertSame('message2', e.message);
    assertSame('test2', e.cause);
    assertSame('message1', e.errors[0].message);
    assertSame('test1', e.errors[0].cause);
}

try {
    throw new AggregateError([],'message');
} catch(e) {
    assertSame('message', e.message);
    assertSame(undefined, e.cause);
}

try {
    throw new AggregateError([new Error('message1')], 'message2');
} catch(e) {
    assertSame('message2', e.message);
    assertSame(undefined, e.cause);
    assertSame('message1', e.errors[0].message);
    assertSame(undefined, e.errors[0].cause);
}

try {
    throw new AggregateError([new Error('message1', { cause: 'test1'})], 'message2');
} catch(e) {
    assertSame('message2', e.message);
    assertSame(undefined, e.cause);
    assertSame('message1', e.errors[0].message);
    assertSame('test1', e.errors[0].cause);
}

try {
    throw new AggregateError({ get [Symbol.iterator]() { throw new Error('iterator') } }, 'unexpected', { get cause() { throw new Error('cause'); } });
} catch (e) {
    assertSame('cause', e.message);
}
