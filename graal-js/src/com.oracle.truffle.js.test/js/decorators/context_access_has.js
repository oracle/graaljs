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
        for (const [kind, definition, accessKeys] of [
            ['field', `${name};`, ['get', 'set', 'has']],
            ['method', `${name}() {}`, ['get', 'has']],
            ['getter', `get ${name}() { throw new Error('unexpected get'); }`, ['get', 'has']],
            ['setter', `set ${name}(v) { throw new Error('unexpected set'); }`, ['set', 'has']],
            ['accessor', `accessor ${name};`, ['get', 'set', 'has']],
        ]) {
            let access;
            function decorate(value, context) {
                access = context.access;
            }
            const createClass = Function('decorate', 'key', `return class {
                @decorate ${prefix}${definition}
                ${prefix}contains(target) { return ${isPrivate ? '#x' : 'key'} in target; }
            };`);
            let previousHas;
            let previousTarget;
            // Repeated evaluations must capture the current property key or private name.
            for (const key of ['a', 'b', 'c', 'd', 'e', Symbol('x')]) {
                const Class = createClass(decorate, key);
                const target = isStatic ? Class : new Class();
                const {has} = access;
                assertSameContent(accessKeys, Object.keys(access));
                assertSame(Object.prototype, Object.getPrototypeOf(access));
                assertSame('has', has.name);
                assertSame(1, has.length);
                const descriptor = Object.getOwnPropertyDescriptor(access, 'has');
                assertTrue(descriptor.writable);
                assertTrue(descriptor.enumerable);
                assertTrue(descriptor.configurable);
                assertTrue(has(target));
                assertFalse(has({}));
                assertFalse(has({'#x': 42}));
                for (const receiver of [undefined, null, {}, 42]) {
                    assertTrue(has.call(receiver, target, 'ignored'));
                }
                assertThrows(() => has.call(target), TypeError);
                assertThrows(() => new has(target), TypeError);
                for (const primitive of primitiveTargets) {
                    assertThrows(() => has(primitive), TypeError);
                }
                if (previousHas !== undefined) {
                    assertFalse(has === previousHas);
                    assertFalse(has(previousTarget));
                    assertFalse(previousHas(target));
                }
                previousHas = has;
                previousTarget = target;

                class Derived extends Class {}
                for (const candidate of [Class, Class.prototype, target, {}, Object.create(target), Derived, new Derived(), new Proxy(target, {})]) {
                    assertSame(target.contains(candidate), has(candidate));
                }
                assertSame(!isStatic, has(new Derived()));
                assertSame(!isPrivate, has(Object.create(target)));

                let hasCalls = 0;
                const proxy = new Proxy(target, {
                    has(object, property) {
                        hasCalls++;
                        assertSame(key, property);
                        return false;
                    },
                    get() { throw new Error('unexpected proxy get'); },
                    getOwnPropertyDescriptor() { throw new Error('unexpected proxy descriptor lookup'); }
                });
                assertFalse(has(proxy));
                assertSame(isPrivate ? 0 : 1, hasCalls);
                const failure = new Error('has trap');
                const throwingProxy = new Proxy({}, {has() { throw failure; }});
                if (isPrivate) {
                    assertFalse(has(throwingProxy));
                } else {
                    assertThrows(() => has(throwingProxy), Error, 'has trap');
                }
                const revocable = Proxy.revocable(target, {});
                revocable.revoke();
                if (isPrivate) {
                    assertFalse(has(revocable.proxy));
                } else {
                    assertThrows(() => has(revocable.proxy), TypeError);
                }

                if (!isPrivate) {
                    const prototype = Object.defineProperty({}, key, {
                        configurable: true,
                        get() { throw new Error('unexpected inherited get'); }
                    });
                    const object = Object.create(prototype);
                    assertTrue(has(object));
                    delete prototype[key];
                    assertFalse(has(object));
                    object[key] = undefined;
                    assertTrue(has(object));
                    delete object[key];
                    assertFalse(has(object));
                    for (const boxed of [Object(false), Object(1), Object(1n), Object('foo'), Object(Symbol())]) {
                        Object.defineProperty(boxed, key, {value: undefined});
                        assertTrue(has(boxed));
                    }
                }
            }
        }
    }
}

// Private methods and accessors are present before field initialization. A field is
// present only after its initializer has completed; an auto-accessor's backing field
// is separate from the private accessor whose presence is being tested.
for (const isStatic of [false, true]) {
    const prefix = isStatic ? 'static ' : '';
    const access = {};
    const checks = [];
    function decorate(value, context) {
        access[context.name] = context.access;
        if (context.kind === 'field' || context.kind === 'accessor') {
            const init = function(value) {
                assertSame(context.kind === 'accessor', context.access.has(this));
                checks.push(context.name);
                return value;
            };
            return context.kind === 'field' ? init : {init};
        }
    }
    function before(target) {
        assertFalse(access['#field'].has(target));
        for (const name of ['#method', '#getter', '#setter', '#auto']) {
            assertTrue(access[name].has(target));
        }
        checks.push('before');
    }
    const Class = Function('decorate', 'before', `return class {
        ${prefix}first = before(this);
        @decorate ${prefix}#field;
        @decorate ${prefix}accessor #auto;
        @decorate ${prefix}#method() {}
        @decorate ${prefix}get #getter() {}
        @decorate ${prefix}set #setter(v) {}
    };`)(decorate, before);
    const target = isStatic ? Class : new Class();
    assertSameContent(['before', '#field', '#auto'], checks);
    for (const name of Object.keys(access)) {
        assertTrue(access[name].has(target));
    }
}

// Each decorator receives a fresh access function, including stacked decorators.
const stacked = [];
function stack(value, context) {
    stacked.push(context.access.has);
}
class Stacked {
    @stack @stack #field;
}
assertFalse(stacked[0] === stacked[1]);
const stackedTarget = new Stacked();
assertTrue(stacked[0](stackedTarget));
assertTrue(stacked[1](stackedTarget));

// Class decorators have no access object.
function decorateClass(value, context) {
    assertFalse('access' in context);
}
@decorateClass class DecoratedClass {}

// Foreign objects are valid targets, but cannot carry JavaScript private elements.
const ProxyObject = Java.type('org.graalvm.polyglot.proxy.ProxyObject');
const LinkedHashMap = Java.type('java.util.LinkedHashMap');
let foreignHas;
function decorateForeign(value, context) {
    foreignHas = context.access.has;
}
function createForeignClass(key) {
    return class { @decorateForeign [key]; };
}
for (const key of ['a', 'b', 'c', 'd', 'e']) {
    createForeignClass(key);
    const members = new LinkedHashMap();
    const target = ProxyObject.fromMap(members);
    assertFalse(foreignHas(target));
    members.put(key, 42);
    assertTrue(foreignHas(target));
    assertFalse(stacked[0](target));
}
