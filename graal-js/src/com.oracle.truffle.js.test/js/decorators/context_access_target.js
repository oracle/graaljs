/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

const primitiveTargets = [undefined, null, false, true, 0, 1, NaN, 1n, '', 'foo', Symbol('target')];

for (const isStatic of [false, true]) {
    for (const isPrivate of [false, true]) {
        const prefix = isStatic ? 'static ' : '';
        const name = isPrivate ? '#x' : '[key]';
        const reference = isPrivate ? 'this.#x' : 'this[key]';
        for (const [kind, definition] of [
            ['field', `${name} = 42;`],
            ['method', `${name}() {}`],
            ['getter', `get ${name}() { return this.value; }`],
            ['setter', `set ${name}(v) { this.value = v; }`],
            ['accessor', `accessor ${name} = 42;`],
        ]) {
            let access;
            function decorate(value, context) {
                access = context.access;
            }
            const createClass = Function('decorate', 'key', `return class {
                ${prefix}value = 42;
                @decorate ${prefix}${definition}
                ${prefix}read() { return ${reference}; }
            };`);

            // Reuse the class expression with enough string keys to exercise the generic path,
            // as well as a symbol key, which always uses that path.
            for (const key of ['a', 'b', 'c', 'd', 'e', Symbol('x')]) {
                const Class = createClass(decorate, key);
                const target = isStatic ? Class : new Class();
                const {get, set} = access;
                if (get !== undefined) {
                    assertSame('', get.name);
                    assertSame(1, get.length);
                    const expected = target.read();
                    assertSame(expected, get(target));
                    for (const receiver of [undefined, null, {}, 42]) {
                        assertSame(expected, get.call(receiver, target, 'ignored'));
                    }
                    assertThrows(() => get.call(target), TypeError);
                    for (const primitive of primitiveTargets) {
                        assertThrows(() => get(primitive), TypeError);
                    }
                    if (isPrivate) {
                        assertThrows(() => get.call(target, {}), TypeError);
                    }
                }
                if (set !== undefined) {
                    assertSame('', set.name);
                    assertSame(2, set.length);
                    assertSame(undefined, set(target, 211));
                    for (const receiver of [undefined, null, {}, 42]) {
                        assertSame(undefined, set.call(receiver, target, 212, 'ignored'));
                        assertSame(212, kind === 'setter' ? target.value : target.read());
                    }
                    // An omitted value defaults to undefined, independently of the target.
                    assertSame(undefined, set(target));
                    assertSame(undefined, kind === 'setter' ? target.value : target.read());
                    assertThrows(() => set.call(target), TypeError);
                    for (const primitive of primitiveTargets) {
                        assertThrows(() => set(primitive, 213), TypeError);
                    }
                    if (isPrivate) {
                        assertThrows(() => set.call(target, {}, 213), TypeError);
                    } else {
                        const readOnly = Object.defineProperty({}, key, {value: 42});
                        assertThrows(() => set(readOnly, 213), TypeError);
                        assertSame(42, readOnly[key]);

                        const getterOnly = Object.defineProperty({}, key, {get() { return 42; }});
                        assertThrows(() => set(getterOnly, 213), TypeError);
                        assertSame(42, getterOnly[key]);

                        const nonExtensible = Object.preventExtensions({});
                        assertThrows(() => set(nonExtensible, 213), TypeError);
                        assertFalse(Object.hasOwn(nonExtensible, key));

                        const rejectingProxy = new Proxy({}, {set() { return false; }});
                        assertThrows(() => set(rejectingProxy, 213), TypeError);
                    }
                }
                if (!isPrivate) {
                    // Primitive targets must be rejected before invoking prototype accessors.
                    let accessorCalls = 0;
                    const prototypes = [Boolean.prototype, Number.prototype, BigInt.prototype, String.prototype, Symbol.prototype];
                    for (const prototype of prototypes) {
                        Object.defineProperty(prototype, key, {
                            configurable: true,
                            get() { accessorCalls++; return 42; },
                            set(v) { accessorCalls++; }
                        });
                    }
                    try {
                        for (const primitive of primitiveTargets) {
                            if (get !== undefined) {
                                assertThrows(() => get(primitive), TypeError);
                            }
                            if (set !== undefined) {
                                assertThrows(() => set(primitive, 213), TypeError);
                            }
                        }
                        assertSame(0, accessorCalls);
                    } finally {
                        for (const prototype of prototypes) {
                            delete prototype[key];
                        }
                    }

                    // Boxed primitives are objects and remain valid targets.
                    for (const boxed of [Object(false), Object(1), Object(1n), Object('foo'), Object(Symbol())]) {
                        Object.defineProperty(boxed, key, {value: 42, writable: true});
                        if (get !== undefined) {
                            assertSame(42, get(boxed));
                        }
                        if (set !== undefined) {
                            assertSame(undefined, set(boxed, 213));
                            assertSame(213, boxed[key]);
                        }
                    }
                }
            }
        }
    }
}

// Foreign objects remain valid targets in both cached and generic access functions.
const ProxyObject = Java.type('org.graalvm.polyglot.proxy.ProxyObject');
const LinkedHashMap = Java.type('java.util.LinkedHashMap');
let foreignAccess;
function decorateForeign(value, context) {
    foreignAccess = context.access;
}
function createForeignClass(key) {
    return class { @decorateForeign [key]; };
}
for (const key of ['a', 'b', 'c', 'd', 'e']) {
    createForeignClass(key);
    const members = new LinkedHashMap();
    members.put(key, 42);
    const target = ProxyObject.fromMap(members);
    assertSame(42, foreignAccess.get(target));
    assertSame(undefined, foreignAccess.set(target, 213));
    assertSame(213, foreignAccess.get(target));
}
