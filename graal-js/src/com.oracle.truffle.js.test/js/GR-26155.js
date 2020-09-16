/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var javaObjectClass = Java.type('java.lang.Object');

assertFalse(Polyglot.isExecutable(javaObjectClass));
assertTrue(Polyglot.isInstantiable(javaObjectClass));

// Ensure that Function.prototype.toString does not throw.
// It throws for non-callable arguments (but instantiable
// foreign objects should be callable).
Function.prototype.toString.call(javaObjectClass);

true;
