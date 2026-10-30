/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Decorated private getters and setters must remain separate class elements so both decorator lists,
 * source ordering, replacements, access closures, and initializers are preserved when pairing them.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

for (const isStatic of [false, true]) {
    for (const setterFirst of [false, true]) {
        for (const [decorateGetter, decorateSetter] of [[true, true], [true, false], [false, true], [false, false]]) {
            const evaluated = [];
            const applied = [];
            const initialized = [];
            const initializerReceivers = [];
            const contexts = {};
            function decorate(kind) {
                evaluated.push(kind);
                return function(value, context) {
                    assertSame(kind, context.kind);
                    assertSame('#x', context.name);
                    assertSame(true, context.private);
                    assertSame(isStatic, context.static);
                    contexts[kind] = context;
                    applied.push(kind);
                    context.addInitializer(function() {
                        initialized.push(kind);
                        initializerReceivers.push(this);
                    });
                    if (kind === 'getter') {
                        return function() { return value.call(this) + 1; };
                    }
                    return function(v) { value.call(this, v * 2); };
                };
            }
            const prefix = isStatic ? 'static ' : '';
            const getter = (decorateGetter ? '@decorate("getter") ' : '') + prefix + 'get #x() { return this.value; }';
            const setter = (decorateSetter ? '@decorate("setter") ' : '') + prefix + 'set #x(v) { this.value = v; }';
            const Class = Function('decorate', 'evaluated', `return class {
                ${prefix}value = 40;
                ${setterFirst ? setter : getter}
                [evaluated.push('middle')]() {}
                ${setterFirst ? getter : setter}
                ${prefix}read() { return this.#x; }
                ${prefix}write(v) { this.#x = v; }
            };`)(decorate, evaluated);
            const kinds = setterFirst ? ['setter', 'getter'] : ['getter', 'setter'];
            const decorated = kind => kind === 'getter' ? decorateGetter : decorateSetter;
            assertSameContent([
                ...(decorated(kinds[0]) ? [kinds[0]] : []),
                'middle',
                ...(decorated(kinds[1]) ? [kinds[1]] : [])
            ], evaluated);
            assertSameContent(kinds.filter(decorated), applied);

            const target = isStatic ? Class : new Class();
            assertSameContent(kinds.filter(decorated), initialized);
            for (const receiver of initializerReceivers) {
                assertSame(target, receiver);
            }
            assertSame(40 + (decorateGetter ? 1 : 0), target.read());
            target.write(21);
            assertSame(decorateSetter ? 42 : 21, target.value);
            assertSame(target.value + (decorateGetter ? 1 : 0), target.read());

            // A completed pair must reject a third accessor of either kind.
            for (const duplicate of [prefix + 'get #x() {}', prefix + 'set #x(v) {}']) {
                assertThrows(() => Function('decorate', `return class {
                    ${setterFirst ? setter : getter}
                    ${setterFirst ? getter : setter}
                    ${duplicate}
                }`), SyntaxError);
            }

            if (decorateGetter) {
                assertSame(target.read(), contexts.getter.access.get.call(target));
                assertThrows(() => contexts.getter.access.get.call({}), TypeError);
            }
            if (decorateSetter) {
                contexts.setter.access.set.call(target, 5);
                assertSame(10, target.value);
                assertSame(10 + (decorateGetter ? 1 : 0), target.read());
                assertThrows(() => contexts.setter.access.set.call({}, 5), TypeError);
            }
        }
    }
}

for (const body of [
    '@dec get #x() {} @dec set #x(v) {} get #x() {}',
    '@dec set #x(v) {} @dec get #x() {} set #x(v) {}',
    '@dec get #x() {} @dec get #x() {}',
    '@dec get #x() {} @dec static set #x(v) {}',
    '@dec get #x() {} @dec set #x(v) {} #x;'
]) {
    assertThrows(() => Function('dec', 'return class {' + body + '}'), SyntaxError);
}
