/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option nashorn-compat=false
 */

load('assert.js');

 // instantiable class
assertSame('function', typeof java.lang.Object);
assertTrue(typeof java.lang.Object === 'function')
assertFalse(typeof java.lang.Object === 'object')

// class with a private constructor
assertSame('object', typeof java.lang.System);
assertFalse(typeof java.lang.System === 'function')
assertTrue(typeof java.lang.System === 'object')

// interface
assertSame('object', typeof java.lang.Runnable);
assertFalse(typeof java.lang.Runnable === 'function')
assertTrue(typeof java.lang.Runnable === 'object')

// annotation
assertSame('object', typeof java.lang.Deprecated);
assertFalse(typeof java.lang.Deprecated === 'function')
assertTrue(typeof java.lang.Deprecated === 'object')

// enum
assertSame('object', typeof java.lang.annotation.ElementType);
assertFalse(typeof java.lang.annotation.ElementType === 'function')
assertTrue(typeof java.lang.annotation.ElementType === 'object')
