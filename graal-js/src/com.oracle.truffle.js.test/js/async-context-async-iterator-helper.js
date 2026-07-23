/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests AsyncContext propagation through async iterator helpers.
 *
 * @option async-context=true
 * @option async-iterator-helpers=true
 * @option unhandled-rejections=throw
 */

load("./assert.js");

const context = new AsyncContext.Variable({defaultValue: "default"});
let helper;
context.run("helper", () => {
    helper = AsyncIterator.from([1, 2]).map(value => {
        assertSame(context.get(), "helper");
        return value;
    });
});

context.run("helper caller 1", () => {
    const first = helper.next();
    assertSame(context.get(), "helper caller 1");
    first.then(result => {
        assertSame(result.value, 1);
        assertSame(context.get(), "helper caller 1");
        let second;
        context.run("helper caller 2", () => {
            second = helper.next();
            assertSame(context.get(), "helper caller 2");
        });
        return second;
    }).then(result => {
        assertSame(result.value, 2);
        assertSame(context.get(), "helper caller 1");
    });
});

const returnSource = {
    returnCalled: false,
    [Symbol.asyncIterator]() {
        return this;
    },
    next() {
        return Promise.resolve({value: 42, done: false});
    },
    return() {
        assertSame(context.get(), "return helper");
        this.returnCalled = true;
        return Promise.resolve({value: undefined, done: true});
    },
};
let returnHelper;
context.run("return helper", () => {
    returnHelper = AsyncIterator.from(returnSource).map(value => value);
});

context.run("return helper caller 1", () => {
    const first = returnHelper.next();
    assertSame(context.get(), "return helper caller 1");
    first.then(result => {
        assertSame(result.value, 42);
        assertSame(context.get(), "return helper caller 1");
        let returned;
        context.run("return helper caller 2", () => {
            returned = returnHelper.return();
            assertSame(context.get(), "return helper caller 2");
        });
        return returned;
    }).then(result => {
        assertTrue(result.done);
        assertTrue(returnSource.returnCalled);
        assertSame(context.get(), "return helper caller 1");
    });
});
