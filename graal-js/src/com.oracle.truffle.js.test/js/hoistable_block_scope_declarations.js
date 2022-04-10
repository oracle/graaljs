/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of hoistable block-scope declarations.
 *
 * @option annex-b
 */

load('assert.js');

// Declarations are hoisted
function test1(x) {
    if (x) {
        return f();
        function f() { return 1; };
    }
}

assertSame(1, test1(true));

// Declarations are hoisted in the right order
function test2a(x) {
    if (x) {
        return f();
        function f() { return 1; };
        function f() { return 2; };
    }
}
function test2b(x) {
    if (x) {
        function f() { return 1; };
        return f();
        function f() { return 2; };
    }
}
function test2c(x) {
    if (x) {
        function f() { return 1; };
        function f() { return 2; };
        return f();
    }
}

assertSame(2, test2a(true));
assertSame(2, test2b(true));
assertSame(2, test2c(true));

// Declarations are not visible before the block
function test3(x) {
    var result = typeof f;
    if (x) {
        function f() {};
    }
    return result;
}

assertSame('undefined', test3(true));

// Declarations are not visible in "parallel" block
function test4(x) {
    if (x) {
        function f() {};
    } else {
        return typeof f;
    }
}

assertSame('undefined', test4(false));

// Declarations are visible after the block (when the declaration is evaluated)
function test5(x) {
    if (x) {
        function f() {}
    }
    return typeof f;
}
assertSame('function', test5(true));
assertSame('undefined', test5(false));

// Evaluation of one declaration promotes the last declaration into function scope
function test6a() {
    foo: {
        break foo;
        function f() { return 1; }
        function f() { return 2; }
    }
    return typeof f;
}
function test6b() {
    foo: {
        function f() { return 1; }
        break foo;
        function f() { return 2; }
    }
    return f();
}
function test6c() {
    foo: {
        function f() { return 1; }
        function f() { return 2; }
        break foo;
    }
    return f();
}
assertSame('undefined', test6a());
assertSame(2, test6b());
assertSame(2, test6c());

// Generators and async functions are not visible outside the block
function test7a(x) {
    if (x) {
        function* f() {}
    }
    return typeof f;
}
function test7b(x) {
    if (x) {
        async function f() {};
    }
    return typeof f;
}

// Declaration is not visible outside the block in strict mode
function test8(x) {
    "use strict";
    if (x) {
        function f() {}
    }
    return typeof f;
}
assertSame('undefined', test8(true));

// Declaration of arguments() function is visible after the block
function test9(x) {
    if (x) {
        function arguments() {};
    }
    return typeof arguments;
}
assertSame('function', test9(true));
assertSame('object', test9(false));

// Declarations are visible in other case blocks
function testSwitch1a(x) {
    switch (x) {
        case 1:
            function f() { return 1; }
            break;
        case 2:
            return f();
    }
}
function testSwitch1b(x) {
    switch (x) {
        case 1:
            function f() { return 1; }
            break;
        default:
            return f();
    }
}
function testSwitch1c(x) {
    switch (x) {
        case 1:
            return f();
        case 2:
            function f() { return 1; }
            break;
    }
}

assertSame(1, testSwitch1a(2));
assertSame(1, testSwitch1b(2));
assertSame(1, testSwitch1c(1));

// Test does not see the declared function
function testSwitch2() {
    switch (typeof f) {
        case 'object':
            function f() {};
            break;
        case 'function':
            return false;
        case 'undefined':
            return true;
        default: 
            function f() {};
            break;
    }
}

assertSame(true, testSwitch2());

// Declarations are hoisted in the right order
function testSwitch3(x) {
    switch (x) {
        case 0:
            return f();
        case 1:
            function f() { return 1; }
            break;
        case 2:
            function f() { return 2; }
            break;
    }
}

assertSame(2, testSwitch3(0));

// Declarations are visible after switch when they are evaluated only
function testSwitch4(x) {
    switch (x) {
        case 0:
            break;
        case 1:
            function f() {}
            break;
    }
    return typeof f;
}

assertSame('undefined', testSwitch4(0));
assertSame('function', testSwitch4(1));
assertSame('undefined', testSwitch4(2));

// Generators and async functions are not visible outside switch
function testSwitch5a(x) {
    switch (x) {
        case 1:
            function* f() {}
            break;
    }
    return typeof f;
}
function testSwitch5b(x) {
    switch (x) {
        case 1:
            async function f() {}
            break;
    }
    return typeof f;
}
assertSame('undefined', testSwitch5a(1));
assertSame('undefined', testSwitch5b(1));

// Evaluation of one declaration promotes the last declaration into function scope
function testSwitch6(x) {
    switch (x) {
        case 1:
            function f() { return 1; }
            break;
        case 2:
            function f() { return 2; }
            break;
    }
    return f();
}

assertSame(2, testSwitch6(1));
assertSame(2, testSwitch6(2));

// Declaration is not visible after switch in strict mode
function testSwitch7(x) {
    "use strict";
    switch (x) {
        case 1:
            function f() {}
            break;
    }
    return typeof f;
}
assertSame('undefined', testSwitch7(1));

// Declaration of arguments() function is visible after switch
function testSwitch8(x) {
    switch (x) {
        case  1:
            function arguments() {};
            break;
    }
    return typeof arguments;
}
assertSame('function', testSwitch8(1));
assertSame('object', testSwitch8(2));
