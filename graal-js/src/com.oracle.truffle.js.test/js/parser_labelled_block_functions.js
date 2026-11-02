/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Annex B labelled functions use the enclosing declaration scope, including blocks and switch cases.
 */

load('assert.js');

for (const labels of ['label: ', 'outer: inner: ', 'outer /*\n*/: inner:\n', '\\u006cabel: ']) {
    assertSameContent(['undefined', 'function', 42, 'function'], Function(`
        const result = [typeof f];
        {
            result.push(typeof f);
            ${labels} function f() { return 42; }
            result.push(f());
        }
        result.push(typeof f);
        return result;
    `)());

    assertSameContent([42, 21], Function(`
        let f = 21;
        const result = [];
        {
            result.push(f());
            ${labels} function f() { return 42; }
        }
        result.push(f);
        return result;
    `)());

    assertSameContent([21, 42], Function(`
        var f = 21;
        const readOuter = () => f;
        const result = [];
        {
            result.push(readOuter());
            ${labels} function f() { return 42; }
            result.push(readOuter()());
        }
        return result;
    `)());

    assertSame('undefined', Function(`
        if (false) { ${labels} function f() {} }
        return typeof f;
    `)());
    assertSame('undefined', Function(`
        exit: { break exit; ${labels} function f() {} }
        return typeof f;
    `)());

    assertSameContent([42, 42], Function(`
        const result = [];
        switch (0) {
            case 0:
                result.push(f());
                ${labels} function f() { return 42; }
        }
        result.push(f());
        return result;
    `)());

    assertSame(42, Function(`{
        function f() { return 21; }
        ${labels} function f() { return 42; }
        return f();
    }`)());
    assertSame(21, Function(`{
        ${labels} function f() { return 42; }
        function f() { return 21; }
        return f();
    }`)());
    assertSame(42, Function(`${labels} function f() { return 42; } return f();`)());
    const closure = Function(`{
        let value = 42;
        ${labels} function f() { return value; }
        return f;
    }`)();
    assertSame(42, closure());
    const evalSource = JSON.stringify(`{ ${labels} function f() { return 42; } }`);
    assertSame(42, Function(`eval(${evalSource}); return f();`)());

    const declaration = `${labels} function f() {}`;
    for (const source of [`21; ${declaration}`, `21; { ${declaration} }`]) {
        assertSame(21, Function(`return eval(${JSON.stringify(source)});`)());
    }
    for (const source of [
        `21; if (true) { ${declaration} }`,
        `21; if (false) {} else { ${declaration} }`,
        `21; switch (0) { case 0: ${declaration} }`,
        `21; try { ${declaration} } finally {}`
    ]) {
        assertSame(undefined, Function(`return eval(${JSON.stringify(source)});`)());
    }

    for (const source of [
        `{ let f; ${labels} function f() {} }`,
        `{ ${labels} function f() {} let f; }`,
        `if (true) ${labels} function f() {}`,
        `while (false) ${labels} function f() {}`,
        `for (;;) ${labels} function f() {}`,
        `with ({}) ${labels} function f() {}`,
        `{ ${labels} function* f() {} }`,
        `{ ${labels} async function f() {} }`,
        `"use strict"; { ${labels} function f() {} }`
    ]) {
        assertThrows(() => Function(source), SyntaxError);
    }
}

assertThrows(() => Function('{ duplicate: duplicate: function f() {} }'), SyntaxError);
assertThrows(() => Function('outer: { outer: function f() {} }'), SyntaxError);
assertThrows(() => Function('outer: function f() { break outer; }'), SyntaxError);
assertSame(42, Function('outer: function f() { outer: { break outer; } return 42; } return f();')());
assertSame(42, Function('outer: inner: { break outer; } return 42;')());

const longLabelChain = Array.from({length: 1000}, (_, i) => 'label' + i + ':').join('');
assertSame(42, Function(longLabelChain + 'function f() { return 42; } return f();')());
assertThrows(() => Function(longLabelChain + 'label0: function f() {}'), SyntaxError);
