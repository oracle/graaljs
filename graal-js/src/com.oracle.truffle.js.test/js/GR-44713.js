/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression tests of Function.prototype.bind.
 */

load("assert.js");

// Bound callable proxy with an apply trap should invoke the trap
var target = function() { return 'incorrect'; };
var handler = { apply() { return 'correct' } };
var proxy = new Proxy(target, handler);
var bound = proxy.bind();
assertSame('correct', bound());

// Foreign executables can be bound
var addTwo = java.lang.Math.addExact.bind(undefined, 2);
assertSame(42, addTwo(40));

// Foreign instantiable can be bound
var Point = java.awt.Point;
var PointWithFixedX = Point.bind(undefined, 42);
var point = new PointWithFixedX(211);
assertSame(42, point.x);
assertSame(211, point.y);
