/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

var point = new java.awt.Point(42, 211);

assertSame('{}', JSON.stringify(point, ['z']));
assertSame('{"x":42}', JSON.stringify(point, ['x', 'z']));
assertSame('{"x":42,"y":211}', JSON.stringify(point, ['x', 'y', 'z']));
assertSame('{"y":211,"x":42}', JSON.stringify(point, ['z', 'y', 'x']));
