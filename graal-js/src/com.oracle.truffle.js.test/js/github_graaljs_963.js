/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 */

load('assert.js');

assertSame('+275760-09-12', new Temporal.PlainDate(-271821, 4, 19).add('P28571428W4D').toString());
assertSame('+275760-09-12T05:38:49', new Temporal.PlainDateTime(-271821, 4, 19, 5, 38, 49).add('P28571428W4D').toString());

var d1 = Temporal.PlainDate.from('1969-01-01');
var d2 = Temporal.PlainDate.from('+275760-01-01');
assertSame(d2.toString(), d1.add(d1.until(d2)).toString());

assertSame('+013606-02', new Temporal.PlainYearMonth(-271821, 5).add(Temporal.Duration.from({milliseconds: 9007199254740991})).toString());
