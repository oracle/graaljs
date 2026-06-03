/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const holeNaNBits = 0x7ff8000000000001n;

function doubleFromBits(bits) {
    return new Float64Array(new BigUint64Array([bits]).buffer)[0];
}

const holeNaN = doubleFromBits(holeNaNBits);
assertTrue(Number.isNaN(holeNaN));

function assertHasNaNAt(array, index) {
    assertTrue(Object.hasOwn(array, index));
    assertTrue(Number.isNaN(array[index]));
}

function makeHolesDoubleArray() {
    const array = [1.1, 2.2, 3.3];
    delete array[1];
    assertFalse(Object.hasOwn(array, 1));
    return array;
}

const reflectSetArray = [1.1];
Reflect.set(reflectSetArray, 0, holeNaN);
assertHasNaNAt(reflectSetArray, 0);

const assignedArray = [1.1, 2.2];
assignedArray[0] = holeNaN;
delete assignedArray[1];
assertHasNaNAt(assignedArray, 0);

const literalArray = [holeNaN, 1.1];
delete literalArray[1];
assertHasNaNAt(literalArray, 0);

const holeWriteArray = makeHolesDoubleArray();
holeWriteArray[1] = holeNaN;
assertHasNaNAt(holeWriteArray, 1);

const nonHoleWriteArray = makeHolesDoubleArray();
nonHoleWriteArray[0] = holeNaN;
assertHasNaNAt(nonHoleWriteArray, 0);

const reflectSetHolesArray = makeHolesDoubleArray();
Reflect.set(reflectSetHolesArray, 1, holeNaN);
assertHasNaNAt(reflectSetHolesArray, 1);

const definePropertyArray = makeHolesDoubleArray();
Object.defineProperty(definePropertyArray, "1", { value: holeNaN, configurable: true, enumerable: true, writable: true });
assertHasNaNAt(definePropertyArray, 1);

const fillArray = makeHolesDoubleArray();
fillArray.fill(holeNaN, 1, 2);
assertHasNaNAt(fillArray, 1);

const pushArray = makeHolesDoubleArray();
pushArray.push(holeNaN);
assertHasNaNAt(pushArray, 3);

const spliceArray = makeHolesDoubleArray();
spliceArray.splice(1, 0, holeNaN);
assertHasNaNAt(spliceArray, 1);
