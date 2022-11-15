/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests ShadowRealm.prototype.evaluate().
 *
 * @option shadow-realm=true
 */

load('../assert.js');

void function testEvaluate() {
    const shadowRealm = new ShadowRealm();

    // primitive result
    assertSame(42, shadowRealm.evaluate(`42`));
    assertSame("w", shadowRealm.evaluate(`"w"`));
    assertSame(true, shadowRealm.evaluate(`true`));
    assertSame(10n, shadowRealm.evaluate(`10n`));
    assertSame(null, shadowRealm.evaluate(`null`));
    assertSame(undefined, shadowRealm.evaluate(`undefined`));
    assertSame(Symbol.iterator, shadowRealm.evaluate(`Symbol.iterator`));

    assertSame(50, shadowRealm.evaluate(`
        function add(a, b) {
            return a + b;
        }
        add(13, 37);
    `));
    assertFalse('add' in globalThis);

    let add = shadowRealm.evaluate(`add;`);
    assertSame('function', typeof add);
    assertSame(42, add(16, 26));
    assertThrows(() => add.call({}, 16, 26), TypeError);
    assertThrows(() => add.call(undefined, {}, {}), TypeError);

    assertThrows(() => shadowRealm.evaluate(`({})`), TypeError);
    assertThrows(() => shadowRealm.evaluate(`throw new Error("expected")`), TypeError);

    let returnsObject = shadowRealm.evaluate(`
        (function returnsObject() {
            return {};
        });
    `);
    assertSame('function', typeof returnsObject);
    assertThrows(() => returnsObject(), TypeError);

    let throwsError = shadowRealm.evaluate(`
        (function throwsError() {
            throw new Error("expected");
        });
    `);
    assertSame('function', typeof throwsError);
    assertThrows(() => throwsError(), TypeError);

    let identityFunction = shadowRealm.evaluate(`a => a;`);

    let uniqueSymbol = Symbol("unique");
    assertSame(uniqueSymbol, identityFunction(uniqueSymbol));

    assertThrows(() => shadowRealm.evaluate(`import "x"`), SyntaxError);
    assertThrows(() => shadowRealm.evaluate(`undefinedReference`), TypeError);

    assertThrows(() => shadowRealm.evaluate(new String(`42`)), TypeError);
}();

void function testWrappedFunctionProperties() {
    const shadowRealm = new ShadowRealm();

    let foo = shadowRealm.evaluate("(function foo(a, b) {})");
    assertSame("foo", foo.name);
    assertSame(2, foo.length);

    // Should have strict function properties regardless of strict mode.
    assertThrows(() => foo.caller, TypeError);
    assertThrows(() => foo.arguments, TypeError);

    assertFalse(foo.hasOwnProperty('arguments'));
    assertFalse(foo.hasOwnProperty('caller'));
    assertFalse(foo.hasOwnProperty('prototype'));

    // Wrapped function exotic objects do not have a [[Construct]] internal method.
    assertThrows(() => new foo(), TypeError);
}();
