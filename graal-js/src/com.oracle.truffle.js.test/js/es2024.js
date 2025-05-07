/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Test of the presence of features in ES 2024 mode.
 * 
 * @option ecmascript-version=2024
 */

load('assert.js');

assertSame('undefined', typeof Iterator);

assertSame('undefined', typeof Set.prototype.intersection);
assertSame('undefined', typeof Set.prototype.union);
assertSame('undefined', typeof Set.prototype.difference);
assertSame('undefined', typeof Set.prototype.symmetricDifference);
assertSame('undefined', typeof Set.prototype.isSubsetOf);
assertSame('undefined', typeof Set.prototype.isSupersetOf);
assertSame('undefined', typeof Set.prototype.isDisjointFrom);

assertSame('undefined', typeof RegExp.escape);

assertSame('undefined', typeof Promise.try);

assertSame('undefined', typeof Float16Array);
assertSame('undefined', typeof Math.f16round);
assertSame('undefined', typeof DataView.prototype.getFloat16);
assertSame('undefined', typeof DataView.prototype.setFloat16);

assertSame('undefined', typeof Intl.DurationFormat);
