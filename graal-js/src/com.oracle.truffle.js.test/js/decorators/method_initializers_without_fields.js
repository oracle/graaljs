/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Method, getter, and setter extra initializers must run even without instance fields.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

for (const isPrivate of [false, true]) {
    for (const isDerived of [false, true]) {
        for (const explicitConstructor of [false, true]) {
            for (const staticField of [false, true]) {
                const events = [];
                const receivers = [];
                function decorate(value, context) {
                    for (const i of [1, 2]) {
                        context.addInitializer(function(...args) {
                            assertSame(0, args.length);
                            assertTrue(context.access.has(this));
                            receivers.push(this);
                            events.push(`${context.kind} ${i}`);
                        });
                    }
                }
                class Base {
                    constructor() {
                        events.push('super');
                    }
                }
                const prefix = isPrivate ? '#' : '';
                const constructor = !explicitConstructor ? '' : isDerived ? `
                    constructor() {
                        events.push('before super');
                        super();
                        events.push('body');
                    }
                ` : `constructor() { events.push('body'); }`;
                const Class = Function('decorate', 'Base', 'events', `return class ${isDerived ? 'extends Base' : ''} {
                    ${constructor}
                    ${staticField ? 'static field = 42;' : ''}
                    @decorate ${prefix}method() {}
                    @decorate get ${prefix}getter() { return 42; }
                    @decorate set ${prefix}setter(value) {}
                };`)(decorate, Base, events);

                assertSameContent([], events);
                const expected = [
                    ...(isDerived && explicitConstructor ? ['before super'] : []),
                    ...(isDerived ? ['super'] : []),
                    'method 1', 'method 2', 'getter 1', 'getter 2', 'setter 1', 'setter 2',
                    ...(explicitConstructor ? ['body'] : [])
                ];
                for (let i = 0; i < 2; i++) {
                    events.length = 0;
                    receivers.length = 0;
                    const instance = new Class();
                    assertSameContent(expected, events);
                    assertSame(6, receivers.length);
                    for (const receiver of receivers) {
                        assertSame(instance, receiver);
                    }
                }
            }
        }
    }
}

// An abrupt initializer completion must prevent the constructor body from running.
{
    const failure = new Error('initializer');
    let bodyCalled = false;
    function decorate(value, context) {
        context.addInitializer(function() { throw failure; });
    }
    class Class {
        @decorate method() {}
        constructor() { bodyCalled = true; }
    }
    try {
        new Class();
        throw new Error('expected initializer to throw');
    } catch (error) {
        assertSame(failure, error);
    }
    assertFalse(bodyCalled);
}
