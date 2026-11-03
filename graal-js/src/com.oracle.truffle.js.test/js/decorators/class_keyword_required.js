/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Decorated class declarations and expressions must require the literal class keyword after the
 * decorator list, rather than treating an arbitrary token as the class introducer.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

for (const introducer of ['bogus', 'function', 'null', 'true', '17', '"class"', 'cl\\u0061ss', '+']) {
    assertThrows(() => Function('@d ' + introducer + ' C {}'), SyntaxError);
    assertThrows(() => Function('const C = @d ' + introducer + ' {};'), SyntaxError);
    assertThrows(() => Function('return @d ' + introducer + ' C {};'), SyntaxError);
}

let calls = 0;
function decorate(value, context) {
    assertSame('class', context.kind);
    calls++;
    return value;
}

@decorate
class Declaration {
    value = 42;
}
const Named = @decorate class Expression {
    value = 42;
};
const Anonymous = @decorate class {
    value = 42;
};

assertSame(3, calls);
assertSame(42, new Declaration().value);
assertSame(42, new Named().value);
assertSame(42, new Anonymous().value);
