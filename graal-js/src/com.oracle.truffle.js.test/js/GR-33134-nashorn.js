/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option nashorn-compat=true
 */

load('assert.js');

 // instantiable class
assertSame('function', typeof java.lang.Object);
assertTrue(typeof java.lang.Object === 'function')
assertFalse(typeof java.lang.Object === 'object')

// class with a private constructor
assertSame('function', typeof java.lang.System);
assertTrue(typeof java.lang.System === 'function')
assertFalse(typeof java.lang.System === 'object')

// interface
assertSame('function', typeof java.lang.Runnable);
assertTrue(typeof java.lang.Runnable === 'function')
assertFalse(typeof java.lang.Runnable === 'object')

// annotation
assertSame('function', typeof java.lang.Deprecated);
assertTrue(typeof java.lang.Deprecated === 'function')
assertFalse(typeof java.lang.Deprecated === 'object')

// enum
assertSame('function', typeof java.lang.annotation.ElementType);
assertTrue(typeof java.lang.annotation.ElementType === 'function')
assertFalse(typeof java.lang.annotation.ElementType === 'object')
