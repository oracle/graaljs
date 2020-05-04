/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/*
 * Test otherwise untested JSToObjectArray node.
 */
 
load('../assert.js');

function fn() { return arguments; }
var obj = {};

function testReflect(value) {
    assertThrows( ()=>{Reflect.apply(fn, obj, value);}, TypeError, "is not an Object");
}

testReflect(true);
testReflect(42);
testReflect(42.5);
testReflect("test");

true;