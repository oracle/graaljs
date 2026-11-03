/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Only the exact, unescaped "use strict" directive may enable strict mode; shorter prefixes must
 * preserve sloppy-mode syntax, this binding, duplicate parameters, and mapped arguments.
 */

load('assert.js');

for (const quote of ['"', "'"]) {
    for (let length = 0; length < 'use strict'.length; length++) {
        const directive = quote + 'use strict'.slice(0, length) + quote + ';';
        assertSame(globalThis, Function('a', 'a', directive + 'return this;')(1, 2));
        assertSame(42, Function(directive + 'with ({value: 42}) { return value; }')());
        assertSame(42, Function('a', directive + 'arguments[0] = 42; return a;')(0));
    }
    const strictDirective = quote + 'use strict' + quote + ';';
    assertSame(undefined, Function(strictDirective + 'return this;')());
    assertThrows(() => Function('a', 'a', strictDirective), SyntaxError);
    assertThrows(() => Function(strictDirective + 'with ({}) {}'), SyntaxError);
}

assertSame(globalThis, Function('"use strictness"; return this;')());
assertSame(globalThis, Function('"use\\x20strict"; return this;')());
assertSame(undefined, Function('""; "use strict"; return this;')());
