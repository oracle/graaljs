/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of long values.
 * 
 * @option temporal
 */

load('assert.js');

var long0 = java.lang.Long.parseLong("0");
var long20 = java.lang.Long.parseLong("20");
var long42 = java.lang.Long.parseLong("42");

assertSame('22', 42..toString(long20));
assertSameContent([], [].splice(long0));
assertSameContent([], (new Array('foo'), new Array(long0)));
assertSameContent([long20, long42], [long42, long20].sort());
assertSame(Number.prototype, Object.getPrototypeOf(long42));

var o = { 42: 'foo'};

assertSame(JSON.stringify(o, null, 20), JSON.stringify(o, null, long20));

assertThrows(() => (0 instanceof long0), TypeError);

var fooTimeZone = { getOffsetNanosecondsFor() { return long42; }, getPossibleInstantsFor() {}, id: 'foo' };
var zonedDateTime = new Temporal.ZonedDateTime(0n, fooTimeZone);
assertSame(42, zonedDateTime.offsetNanoseconds);
