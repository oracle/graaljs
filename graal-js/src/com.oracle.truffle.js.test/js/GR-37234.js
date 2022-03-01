/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the handling of primitive values by Intl.ListFormat.prototype.format().
 * 
 * @option intl-402
 */

load('assert.js');

let lf = new Intl.ListFormat('en');

assertSame('', lf.format(undefined));
assertSame('G, r, a, a, and l', lf.format('Graal'));

assertThrows(() => lf.format(42), TypeError);
assertThrows(() => lf.format(true), TypeError);
assertThrows(() => lf.format(Symbol()), TypeError);
assertThrows(() => lf.format(211n), TypeError);
assertThrows(() => lf.format(null), TypeError);

