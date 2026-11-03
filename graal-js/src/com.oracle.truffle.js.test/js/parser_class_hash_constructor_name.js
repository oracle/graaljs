/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * The reserved private identifier #constructor must not restrict public string-named class elements.
 *
 * @option ecmascript-version=staging
 */

load('assert.js');

for (const prefix of ['', 'static ']) {
    for (const name of ['"#constructor"', '"#constr\\u0075ctor"', '["#constructor"]']) {
        for (const modifier of ['', 'async ', '*', 'async *']) {
            const C = Function('return class {' + prefix + modifier + name + '() { return 42; }}')();
            const target = prefix ? C : C.prototype;
            assertSame('#constructor', target['#constructor'].name);
            if (!modifier) {
                assertSame(42, target['#constructor']());
            }
        }

        const Accessor = Function('return class {' + prefix + 'get ' + name + '() { return this.value; } ' +
            prefix + 'set ' + name + '(v) { this.value = v; }}')();
        const descriptor = Object.getOwnPropertyDescriptor(prefix ? Accessor : Accessor.prototype, '#constructor');
        assertSame('get #constructor', descriptor.get.name);
        assertSame('set #constructor', descriptor.set.name);
        const accessorTarget = prefix ? Accessor : new Accessor();
        accessorTarget['#constructor'] = 42;
        assertSame(42, accessorTarget['#constructor']);

        for (const modifier of ['', 'accessor ']) {
            const Field = Function('return class {' + prefix + modifier + name + ' = 42;}')();
            const fieldTarget = prefix ? Field : new Field();
            assertSame(42, fieldTarget['#constructor']);
            fieldTarget['#constructor'] = 43;
            assertSame(43, fieldTarget['#constructor']);
        }

        const Named = Function('return class {' + prefix + name + ' = function() {};}')();
        const namedTarget = prefix ? Named : new Named();
        assertSame('#constructor', namedTarget['#constructor'].name);
    }

    for (const element of [
        '#constructor;',
        '#constructor = 42;',
        'accessor #constructor;',
        '#constructor() {}',
        '*#constructor() {}',
        'async #constructor() {}',
        'async *#constructor() {}',
        'get #constructor() {}',
        'set #constructor(v) {}'
    ]) {
        assertThrows(() => Function('class C {' + prefix + element + '}'), SyntaxError);
    }
}
