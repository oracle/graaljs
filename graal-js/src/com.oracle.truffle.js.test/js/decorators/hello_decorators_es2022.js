/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=2022
 */

load('../assert.js');

function syntaxError(code, messageContains) {
    assertThrows(() => eval(code), SyntaxError, messageContains);
}


//---------------------------------------------//
syntaxError(`
    function dec() {};

    @dec
    class C {};
    
`,'Expected an operand ');


//---------------------------------------------//
syntaxError(`
    class C {
        accessor foo = 42;
    };
    
`, 'Expected ( but found foo');


//---------------------------------------------//
syntaxError(`
    class C {
        static accessor foo = 42;
    };
    
`, 'Expected ( but found foo');


//---------------------------------------------//
syntaxError(`
    @ C { constructor() { console.log('Hi from class without class keyword'); } };

`, 'Expected an operand but found error');


//---------------------------------------------//
// 'accessor' is a valid field name in es2022: no exceptions.
class C1 { accessor }
class C2 { static accessor }
class C3 { static accessor = 42; }
assertSame(42, C3.accessor);
class C4 { accessor = 42; }
assertSame(42, (new C4).accessor);
