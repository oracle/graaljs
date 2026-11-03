/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Legacy call-result assignment targets must raise guest errors, regardless of the callee's token.
 */

load('assert.js');

for (const [target, errorType] of [
    ['f()', ReferenceError],
    ['{call: f}.call()', ReferenceError],
    ['[f][0]()', ReferenceError],
    ['{}()', TypeError],
    ['[]()', TypeError]
]) {
    for (const expression of [
        target + ' = rhs()',
        target + ' += rhs()',
        target + ' *= rhs()',
        '++' + target,
        '--' + target,
        target + '++',
        target + '--'
    ]) {
        const run = Function('f', 'rhs', 'return ' + expression + ';');
        let calls = 0;
        let rhsCalls = 0;
        const f = () => { calls++; return 1; };
        const rhs = () => { rhsCalls++; return 2; };
        assertThrows(() => run(f, rhs), errorType);
        assertSame(errorType === ReferenceError ? 1 : 0, calls);
        assertSame(0, rhsCalls);
        Function('if (false) { return ' + expression + '; }');
        assertThrows(() => Function('"use strict"; return ' + expression + ';'), SyntaxError);
    }
}
