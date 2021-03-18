/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const ByteBuffer = Java.type("java.nio.ByteBuffer");

for (const direct of [false, true]) {
	let construct = (len) => (direct ? ByteBuffer.allocateDirect(16) : ByteBuffer.allocate(16));

	let array1 = new Uint8Array(construct(16));
	let array2 = new Uint8Array(construct(16));

	fill(array2);

	array1.set(array2.subarray(6, 12), 10);
	array1.set(array2.subarray(0, 6));

	assertSame("42,43,44,45,46,47,0,0,0,0,48,49,50,51,52,53", array1.join(","));

	array1.set(array2);

	assertSame("42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57", array1.join(","));

	array1.set(array1);

	assertSame("42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57", array1.join(","));

	array1.set(array1.subarray(6, 12), 10);

	assertSame("42,43,44,45,46,47,48,49,50,51,48,49,50,51,52,53", array1.join(","));

	let buffer = array2.buffer;
	let slice = buffer.slice(6, 12);
	assertSame("48,49,50,51,52,53", new Uint8Array(slice).join(","));

	buffer = construct(16);
	fill(new Uint8Array(buffer));
	slice = ArrayBuffer.prototype.slice.call(buffer, 6, 12);
	assertSame("48,49,50,51,52,53", new Uint8Array(slice).join(","));
}

function fill(array) {
	for (let i = 0; i < array.length; i++) {
		array[i] = 42 + i;
	}
}
