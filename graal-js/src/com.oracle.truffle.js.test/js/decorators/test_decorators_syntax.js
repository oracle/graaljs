/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

function syntaxError(code) {
    assertThrows(() => eval(code), SyntaxError);
}

//---------------------------------------------//
syntaxError(`
    class C { @({ f() {} }).f y }
`);


//---------------------------------------------//
syntaxError(`
    class C { @(42, 'xxx').f y }
`);


//---------------------------------------------//
syntaxError(`
    @ C { constructor() { console.log('Hi from class without class keyword'); } };
    new C();
`);


//---------------------------------------------//
syntaxError(`
    class C { 
      static accessor { } 
    };    
`);


//---------------------------------------------//
// 'accessor' is a valid field name in es2023: no exception.
class C1 { static accessor }
class C2 { accessor }
class C3 { static accessor foo = 42 }
class C4 { accessor foo = 42 }


//---------------------------------------------//
syntaxError(`
    function foo() {}
    
    class C { @foo constructor() {} }
`);


//---------------------------------------------//
syntaxError(`
    function f() {}
    
    (class { @f static {} })    
`);


//---------------------------------------------//
syntaxError(`
    var C = (class { accessor m() {} })
`);


//---------------------------------------------//
syntaxError(`
    var C = (class { accessor get m() {} })
`);


//---------------------------------------------//
syntaxError(`
    var C = (class { accessor set m(x) {} })
`);
