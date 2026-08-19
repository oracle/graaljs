/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('./assert.js');

// A trailing comma after an object rest property is invalid in assignment and binding patterns,
// but remains valid after an object spread property in an object literal.
assertThrows(() => new Function('let {...rest,} = {};'), SyntaxError);
assertThrows(() => new Function('({...rest,} = {});'), SyntaxError);
assertThrows(() => new Function('function f({...rest,}) {}'), SyntaxError);
assertThrows(() => new Function('for ({...rest,} of [{}]) {}'), SyntaxError);

new Function('({...rest,});');
