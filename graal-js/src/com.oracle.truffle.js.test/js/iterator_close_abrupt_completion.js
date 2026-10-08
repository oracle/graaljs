/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that abrupt IteratorStepValue completion does not close the iterator.
 *
 * @option ecmascript-version=staging
 * @option unhandled-rejections=throw
 */

load("assert.js");

const abruptKinds = ["next", "non-object", "done", "value"];

function createSyncIteratorRecord(abruptKind) {
    const error = {};
    const record = {
        error,
        returnCalls: 0,
    };
    const iterator = {
        next() {
            if (abruptKind === "next") {
                throw error;
            } else if (abruptKind === "non-object") {
                return null;
            } else if (abruptKind === "done") {
                return {get done() { throw error; }};
            } else if (abruptKind === "value") {
                return {done: false, get value() { throw error; }};
            }
            return {done: true};
        },
        return() {
            record.returnCalls++;
            return {};
        },
    };
    record.iterable = {
        [Symbol.iterator]() {
            return iterator;
        },
    };
    return record;
}

function createSingleValueIteratorRecord(value) {
    const record = {
        nextCalls: 0,
        returnCalls: 0,
    };
    const iterator = {
        next() {
            if (record.nextCalls++ === 0) {
                return {done: false, value};
            }
            return {done: true};
        },
        return() {
            record.returnCalls++;
            return {};
        },
    };
    record.iterable = {
        [Symbol.iterator]() {
            return iterator;
        },
    };
    return record;
}

function assertSyncIteratorStepAbruptDoesNotClose(operation) {
    for (const abruptKind of abruptKinds) {
        const record = createSyncIteratorRecord(abruptKind);
        let caught;
        try {
            operation(record.iterable);
        } catch (error) {
            caught = error;
        }
        if (abruptKind === "non-object") {
            assertTrue(caught instanceof TypeError);
        } else {
            assertSame(record.error, caught);
        }
        assertSame(0, record.returnCalls);
    }
}

assertSyncIteratorStepAbruptDoesNotClose(iterable => Object.fromEntries(iterable));
assertSyncIteratorStepAbruptDoesNotClose(iterable => Object.groupBy(iterable, value => value));
assertSyncIteratorStepAbruptDoesNotClose(iterable => Map.groupBy(iterable, value => value));
assertSyncIteratorStepAbruptDoesNotClose(iterable => Math.sumPrecise(iterable));
assertSyncIteratorStepAbruptDoesNotClose(iterable => Iterator.concat(iterable).next());

{
    const error = {};
    const entry = {get 0() { throw error; }};
    const record = createSingleValueIteratorRecord(entry);
    let caught;
    try {
        Object.fromEntries(record.iterable);
    } catch (ex) {
        caught = ex;
    }
    assertSame(error, caught);
    assertSame(1, record.returnCalls);
}

for (const groupBy of [Object.groupBy, Map.groupBy]) {
    const error = {};
    const record = createSingleValueIteratorRecord(42);
    let caught;
    try {
        groupBy(record.iterable, () => { throw error; });
    } catch (ex) {
        caught = ex;
    }
    assertSame(error, caught);
    assertSame(1, record.returnCalls);
}

{
    const record = createSingleValueIteratorRecord("not a number");
    assertThrows(() => Math.sumPrecise(record.iterable), TypeError);
    assertSame(1, record.returnCalls);
}

{
    const record = createSingleValueIteratorRecord(42);
    const iterator = Iterator.concat(record.iterable);
    assertSame(42, iterator.next().value);
    iterator.return();
    assertSame(1, record.returnCalls);
}

function createAsyncIteratorRecord(abruptKind) {
    const error = {};
    const record = {
        error,
        returnCalls: 0,
    };
    record.iterable = {
        [Symbol.asyncIterator]() {
            return this;
        },
        next() {
            if (abruptKind === "next") {
                throw error;
            } else if (abruptKind === "rejected-next") {
                return Promise.reject(error);
            } else if (abruptKind === "non-object") {
                return null;
            } else if (abruptKind === "done") {
                return {get done() { throw error; }};
            } else if (abruptKind === "value") {
                return {done: false, get value() { throw error; }};
            }
            return {done: true};
        },
        return() {
            record.returnCalls++;
            return {};
        },
    };
    return record;
}

async function assertArrayFromAsyncAbruptDoesNotClose(record, abruptKind) {
    let caught;
    try {
        await Array.fromAsync(record.iterable);
    } catch (error) {
        caught = error;
    }
    if (abruptKind === "non-object") {
        assertTrue(caught instanceof TypeError);
    } else {
        assertSame(record.error, caught);
    }
    assertSame(0, record.returnCalls);
}

async function assertRejectsWithTypeError(operation) {
    let caught;
    try {
        await operation();
    } catch (error) {
        caught = error;
    }
    assertTrue(caught instanceof TypeError);
}

(async function testArrayFromAsync() {
    for (const abruptKind of abruptKinds) {
        await assertArrayFromAsyncAbruptDoesNotClose(createSyncIteratorRecord(abruptKind), abruptKind);
    }
    for (const abruptKind of ["next", "rejected-next", "non-object", "done", "value"]) {
        await assertArrayFromAsyncAbruptDoesNotClose(createAsyncIteratorRecord(abruptKind), abruptKind);
    }

    {
        const record = createAsyncIteratorRecord();
        function Result() {
            return Object.defineProperty({}, "length", {value: 0, writable: false});
        }
        await assertRejectsWithTypeError(() => Array.fromAsync.call(Result, record.iterable));
        assertSame(0, record.returnCalls);
    }

    {
        const record = createAsyncIteratorRecord();
        let nextCalls = 0;
        record.iterable.next = () => nextCalls++ === 0 ? {done: false, value: 42} : {done: true};
        function Result() {
            return Object.preventExtensions({});
        }
        await assertRejectsWithTypeError(() => Array.fromAsync.call(Result, record.iterable));
        assertSame(1, record.returnCalls);
    }

    for (const rejectMapper of [false, true]) {
        const error = {};
        const record = createAsyncIteratorRecord();
        record.iterable.next = () => ({done: false, value: 42});
        let caught;
        try {
            await Array.fromAsync(record.iterable, () => {
                if (rejectMapper) {
                    return Promise.reject(error);
                }
                throw error;
            });
        } catch (ex) {
            caught = ex;
        }
        assertSame(error, caught);
        assertSame(1, record.returnCalls);
    }
})().catch(error => {
    console.error(error.stack || error);
    throw error;
});
