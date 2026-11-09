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
                    assertSame(1, get.length);
                    const expected = target.read();
                    assertSame(expected, get(target));
                    for (const receiver of [undefined, null, {}, 42]) {
                        assertSame(expected, get.call(receiver, target, 'ignored'));
                    }
                    assertThrows(() => get.call(target), TypeError);
                    if (isPrivate) {
                        assertThrows(() => get.call(target, {}), TypeError);
                    }
                }
                if (set !== undefined) {
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
                    if (isPrivate) {
                        assertThrows(() => set.call(target, {}, 213), TypeError);
                    }
                }
            }
        }
    }
}
