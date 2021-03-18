/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const ByteBuffer = Java.type("java.nio.ByteBuffer");
const ByteOrder = Java.type("java.nio.ByteOrder");

for (const direct of [false, true]) {
	let buffer = direct ? ByteBuffer.allocateDirect(16) : ByteBuffer.allocate(16);
	buffer.order(ByteOrder.nativeOrder());
	buffer.putInt(0, 42);
	buffer.order(ByteOrder.LITTLE_ENDIAN);
	buffer.putInt(4, 43);
	buffer.putDouble(8, Number.MAX_VALUE / 42);
	buffer = buffer.asReadOnlyBuffer();

	let ab = new ArrayBuffer(buffer);
    let dv = new DataView(ab);
	let ia = new Int32Array(ab);

	assertSame(42, ia[0]);
	assertSame(43, dv.getInt32(4, true));

	assertThrows(() => {ia[0] = 41;}, TypeError);
	assertThrows(() => {dv.setInt32(4, 40, true);}, TypeError);

	assertSame(undefined, ia[4]);
	assertThrows(() => {dv.getInt32(16, true);}, RangeError);
	assertThrows(() => {dv.setInt32(16, 40, true);}, RangeError);
}

