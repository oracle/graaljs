/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/*
 * Test otherwise untested indirect eval.
 */
 
load('../assert.js');

var indirecteval = eval;
assertSame(42, indirecteval(42));
assertSame(42n, indirecteval(42n));
assertSame(42, indirecteval(Debug.createSafeInteger(42)));
assertSame(42.5, indirecteval(42.5));
assertSame(true, indirecteval(true));

var symbol = Symbol('test');
assertSame(symbol, indirecteval(symbol));

var obj = {foo:'bar'};
assertSame(obj, indirecteval(obj));

true;