/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/*
 * Test otherwise uncovered functionality in JSRuntime.
 */
 
load('../assert.js');

//coverage for toNumberFromPrimitive
var a = [1,2,3];
Object.defineProperty(a, 'length', { value: '42' });
assertFail(()=>{Object.defineProperty(a, 'length', { value: Debug.createLazyString('1234567890','12345678901234567890') })}, "Invalid array length");
assertFail(()=>{Object.defineProperty(a, 'length', { value: Symbol('42') })}, "Cannot convert a Symbol");
assertFail(()=>{Object.defineProperty(a, 'length', { value: BigInt('42') })}, "Cannot convert a BigInt");


//coverage for toString
assertFail(()=>{Debug.createLazyString(Symbol('42'), 'test')}, "Cannot convert a Symbol");
assertSame('12345678901234567890test', Debug.createLazyString(BigInt('12345678901234567890'), 'test'));
assertTrue(Debug.createLazyString('ABC', new (Java.type('java.lang.Object'))()).indexOf('ABCjava.lang.Object@') == 0);
true;
