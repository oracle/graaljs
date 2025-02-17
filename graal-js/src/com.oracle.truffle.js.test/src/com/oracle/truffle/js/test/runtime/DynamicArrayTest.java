/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray.SetSupportedProfileAccess;
import com.oracle.truffle.js.runtime.array.dyn.ContiguousIntArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;

public class DynamicArrayTest extends JSTest {

    static final int INT = Byte.MAX_VALUE + 42;
    static final int BYTE = 42;
    static final String OBJECT = "42";
    static final double DOUBLE = 42.0d;

    @Override
    public void setup() {
        super.setup();
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void testEmpty() {
        assertEmpty(createEmptyArray());
        assertEmpty(createEmptyArray(0));
    }

    private static void assertEmpty(JSArrayObject arrayObject) {
        ScriptArray array = arrayObject.getArrayType();
        assertEquals(0, array.length(arrayObject));
        assertEquals(0, array.firstElementIndex(arrayObject));
        assertEquals(-1, array.lastElementIndex(arrayObject));

        assertEquals(true, array.isInBoundsFast(arrayObject, 0) == array.isInBoundsFast(arrayObject, 0));

        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 1));

        assertEquals(0, array.toArray(arrayObject).length);
    }

    @Test
    public void testInBoundsContiguousAccess() {
        JSArrayObject arrayObject = setArrayElement(createEmptyArray(4), 3, INT);
        AbstractIntArray array = (AbstractIntArray) arrayObject.getArrayType();
        assertEquals(true, array.isInBounds(arrayObject, 3));
        assertEquals(true, array.isInBoundsFast(arrayObject, 3));
        assertEquals(true, array.isInBounds(arrayObject, 2));
        assertEquals(false, array.isInBoundsFast(arrayObject, 2));
        assertEquals(true, array.isInBounds(arrayObject, 4));
        assertEquals(false, array.isInBoundsFast(arrayObject, 4));

        array.setInBounds(arrayObject, 2, INT, null, SetSupportedProfileAccess.getUncached());

        assertEquals(true, array.isInBounds(arrayObject, 2));
        assertEquals(true, array.isInBoundsFast(arrayObject, 2));
        assertEquals(true, array.isInBounds(arrayObject, 1));
        assertEquals(false, array.isInBoundsFast(arrayObject, 1));

        array.setInBounds(arrayObject, 1, INT, null, SetSupportedProfileAccess.getUncached());

        assertEquals(true, array.isInBounds(arrayObject, 1));
        assertEquals(true, array.isInBoundsFast(arrayObject, 1));
        assertEquals(true, array.isInBounds(arrayObject, 0));
        assertEquals(false, array.isInBoundsFast(arrayObject, 0));
    }

    @Test
    public void testInBoundsZeroAccess() {
        JSArrayObject arrayObject = setArrayElement(createEmptyArray(4), 0, INT);
        AbstractIntArray array = (AbstractIntArray) arrayObject.getArrayType();
        assertEquals(true, array.isInBounds(arrayObject, 0));
        assertEquals(true, array.isInBoundsFast(arrayObject, 0));
        assertEquals(true, array.isInBounds(arrayObject, 1));
        assertEquals(false, array.isInBoundsFast(arrayObject, 1));

        array.setInBounds(arrayObject, 1, INT, null, SetSupportedProfileAccess.getUncached());

        assertEquals(true, array.isInBounds(arrayObject, 1));
        assertEquals(true, array.isInBoundsFast(arrayObject, 1));
        assertEquals(true, array.isInBounds(arrayObject, 2));
        assertEquals(false, array.isInBoundsFast(arrayObject, 2));

        array.setInBounds(arrayObject, 2, INT, null, SetSupportedProfileAccess.getUncached());

        assertEquals(true, array.isInBounds(arrayObject, 2));
        assertEquals(true, array.isInBoundsFast(arrayObject, 2));
        assertEquals(true, array.isInBounds(arrayObject, 3));
        assertEquals(false, array.isInBoundsFast(arrayObject, 3));

        array.setInBounds(arrayObject, 3, INT, null, SetSupportedProfileAccess.getUncached());

        assertEquals(true, array.isInBounds(arrayObject, 3));
        assertEquals(true, array.isInBoundsFast(arrayObject, 3));
        assertEquals(false, array.isInBounds(arrayObject, 4));
        assertEquals(false, array.isInBoundsFast(arrayObject, 4));
    }

    @Test
    public void testInBoundsZeroAccessLengthExtension() {
        JSArrayObject arrayObject = setArrayElement(createEmptyArray(), 0, INT);
        AbstractIntArray array = (AbstractIntArray) arrayObject.getArrayType();
        assertEquals(ZeroBasedIntArray.class, array.getClass());
        assertEquals(1, array.length(arrayObject));

        for (int i = 1; i < JSConfig.InitialArraySize; i++) {
            array.setInBounds(arrayObject, i, INT, null, SetSupportedProfileAccess.getUncached());
            assertEquals(i + 1, array.length(arrayObject));
            assertEquals(0, array.firstElementIndex(arrayObject));
            assertEquals(i, array.lastElementIndex(arrayObject));
        }
    }

    @Test
    public void testInBoundsContiguousLengthExtension() {
        JSArrayObject arrayObject = setArrayElement(createEmptyArray(), 1, INT);
        AbstractIntArray array = (AbstractIntArray) arrayObject.getArrayType();
        assertEquals(ContiguousIntArray.class, array.getClass());
        assertEquals(2, array.length(arrayObject));

        for (int i = 2; i < JSConfig.InitialArraySize; i++) {
            array.setInBounds(arrayObject, i, INT, null, SetSupportedProfileAccess.getUncached());
            assertEquals(i + 1, array.length(arrayObject));
            assertEquals(1, array.firstElementIndex(arrayObject));
            assertEquals(i, array.lastElementIndex(arrayObject));
        }
    }

    @Test
    public void testInBoundsHoles() {
        JSArrayObject arrayObject = setArrayElement(createEmptyArray(8), 4, INT);
        AbstractIntArray array = (AbstractIntArray) arrayObject.getArrayType();
        array = (AbstractIntArray) array.setElement(arrayObject, 2, INT, false);
        assertEquals(HolesIntArray.class, array.getClass());
        assertEquals(8, array.length(arrayObject));

        for (int i = 2; i < 5; i++) {
            assertEquals("idx" + i, true, array.isInBoundsFast(arrayObject, i));
        }
        for (int i = 0; i < 8; i++) {
            assertEquals("idx" + i, true, array.isInBounds(arrayObject, i));
        }

        array.setInBounds(arrayObject, 6, INT, null, SetSupportedProfileAccess.getUncached());

        for (int i = 2; i < 7; i++) {
            assertEquals("idx" + i, true, array.isInBoundsFast(arrayObject, i));
        }
        for (int i = 0; i < 8; i++) {
            assertEquals("idx" + i, true, array.isInBounds(arrayObject, i));
        }

        array.setInBounds(arrayObject, 0, INT, null, SetSupportedProfileAccess.getUncached());

        for (int i = 0; i < 7; i++) {
            assertEquals("idx" + i, true, array.isInBoundsFast(arrayObject, i));
        }
        for (int i = 0; i < 8; i++) {
            assertEquals("idx" + i, true, array.isInBounds(arrayObject, i));
        }

        array.setInBounds(arrayObject, 7, INT, null, SetSupportedProfileAccess.getUncached());

        for (int i = 0; i < 8; i++) {
            assertEquals("idx" + i, true, array.isInBoundsFast(arrayObject, i));
        }
        for (int i = 0; i < 8; i++) {
            assertEquals("idx" + i, true, array.isInBounds(arrayObject, i));
        }

    }

    @Test
    public void testInitialInt() {
        assertScriptArray(setArrayElement(createEmptyArray(), 0, INT), INT);
        assertScriptArray(setArrayElement(createEmptyArray(0), 0, INT), INT);
        assertScriptArray(setArrayElement(createEmptyArray(1), 0, INT), INT);
        assertScriptArray(setArrayElement(createConstantArray(new int[]{INT}), 0, INT), INT);
        assertScriptArray(setArrayElement(createConstantArray(new double[]{DOUBLE}), 0, INT), (double) INT);
        assertScriptArray(setArrayElement(createConstantArray(new byte[]{(byte) BYTE}), 0, INT), INT);
        assertScriptArray(setArrayElement(createConstantArray(new Object[]{OBJECT}), 0, INT), INT);
    }

    @Test
    public void testInitialDouble() {
        assertScriptArray(setArrayElement(createEmptyArray(), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createEmptyArray(0), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createEmptyArray(1), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createConstantArray(new double[]{DOUBLE}), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createConstantArray(new byte[]{(byte) BYTE}), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createConstantArray(new int[]{INT}), 0, DOUBLE), DOUBLE);
        assertScriptArray(setArrayElement(createConstantArray(new Object[]{OBJECT}), 0, DOUBLE), DOUBLE);
    }

    @Test
    public void testInitialByte() {
        assertScriptArray(setArrayElement(createEmptyArray(), 0, BYTE), BYTE);
        assertScriptArray(setArrayElement(createEmptyArray(0), 0, BYTE), BYTE);
        assertScriptArray(setArrayElement(createEmptyArray(1), 0, BYTE), BYTE);
        assertScriptArray(setArrayElement(createConstantArray(new byte[]{(byte) BYTE}), 0, BYTE), BYTE);
        assertScriptArray(setArrayElement(createConstantArray(new int[]{INT}), 0, BYTE), BYTE);
        assertScriptArray(setArrayElement(createConstantArray(new double[]{DOUBLE}), 0, BYTE), (double) BYTE);
        assertScriptArray(setArrayElement(createConstantArray(new Object[]{OBJECT}), 0, BYTE), BYTE);
    }

    @Test
    public void testInitialObject() {
        assertScriptArray(setArrayElement(createEmptyArray(), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createEmptyArray(0), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createEmptyArray(1), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createConstantArray(new Object[]{OBJECT}), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createConstantArray(new byte[]{(byte) BYTE}), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createConstantArray(new int[]{INT}), 0, OBJECT), OBJECT);
        assertScriptArray(setArrayElement(createConstantArray(new double[]{DOUBLE}), 0, OBJECT), OBJECT);
    }

    @Test
    public void testConstantIntArray() {
        assertScriptArray(createConstantArray(new int[]{INT}), INT);
    }

    @Test
    public void testConstantDoubleArray() {
        assertScriptArray(createConstantArray(new double[]{DOUBLE}), DOUBLE);
    }

    @Test
    public void testConstantByteArray() {
        assertScriptArray(createConstantArray(new byte[]{BYTE}), BYTE);
    }

    @Test
    public void testConstantObjectArray() {
        assertScriptArray(createConstantArray(new Object[]{OBJECT}), OBJECT);
    }

    private JSArrayObject createEmptyArray(int capacity) {
        return JSArray.createConstantEmptyArray(testHelper.getJSContext(), testHelper.getRealm(), capacity);
    }

    private JSArrayObject createEmptyArray() {
        return JSArray.createConstantEmptyArray(testHelper.getJSContext(), testHelper.getRealm());
    }

    private JSArrayObject createConstantArray(Object array) {
        JSContext context = testHelper.getJSContext();
        JSRealm realm = testHelper.getRealm();
        if (array instanceof byte[]) {
            return JSArray.createConstantByteArray(context, realm, (byte[]) array);
        } else if (array instanceof int[]) {
            return JSArray.createConstantIntArray(context, realm, (int[]) array);
        } else if (array instanceof double[]) {
            return JSArray.createConstantDoubleArray(context, realm, (double[]) array);
        } else if (array instanceof Object[]) {
            return JSArray.createConstantObjectArray(context, realm, (Object[]) array);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static JSArrayObject setArrayElement(JSArrayObject arrayObject, int index, Object value) {
        arrayObject.setArrayType(arrayObject.getArrayType().setElement(arrayObject, index, value, false));
        return arrayObject;
    }

    private static void assertScriptArray(JSArrayObject arrayObject, Object value) {
        ScriptArray array = arrayObject.getArrayType();

        assertEquals(value, array.getElement(arrayObject, 0));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 1));
        assertEquals(1, array.length(arrayObject));

        assertEquals(0, array.firstElementIndex(arrayObject));
        assertEquals(0, array.lastElementIndex(arrayObject));

        assertEquals(true, array.hasElement(arrayObject, 0));
        assertEquals(false, array.hasElement(arrayObject, 1));

        assertEquals((long) JSRuntime.MAX_SAFE_INTEGER, array.nextElementIndex(arrayObject, 0));
        assertEquals(-1, array.previousElementIndex(arrayObject, 0));

        assertEquals(0, array.firstElementIndex(arrayObject));
        assertEquals(0, array.lastElementIndex(arrayObject));
        assertArrayEquals(new Object[]{value}, array.toArray(arrayObject));

        assertLength(value, arrayObject);
        assertDelete(value, arrayObject);
    }

    private static void assertDelete(Object value, JSArrayObject arrayObject) {
        ScriptArray array = arrayObject.getArrayType();

        // delete at end
        arrayObject.setArrayType(array = array.setElement(arrayObject, 0, value, false));
        arrayObject.setArrayType(array = array.setElement(arrayObject, 1, value, false));
        arrayObject.setArrayType(array = array.deleteElement(arrayObject, 1, false));

        assertEquals(2, array.length(arrayObject));
        assertEquals(value, array.getElement(arrayObject, 0));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 1));

        arrayObject.setArrayType(array = array.setElement(arrayObject, 2, value, false));
        arrayObject.setArrayType(array = array.deleteElement(arrayObject, 2, false));
        assertEquals(value, array.getElement(arrayObject, 0));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 1));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 2));
    }

    private static void assertLength(Object value, JSArrayObject arrayObject) {
        ScriptArray array = arrayObject.getArrayType();
        // test length
        arrayObject.setArrayType(array = array.setLength(arrayObject, 10, false));
        assertEquals(10, array.length(arrayObject));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 1, false));
        assertEquals(1, array.length(arrayObject));

        assertEquals(value, array.getElement(arrayObject, 0));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 1));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 0, false));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 1, false));
        arrayObject.setArrayType(array = array.setElement(arrayObject, 0, value, false));
        assertEquals(value, array.getElement(arrayObject, 0));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 0, false));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));

        arrayObject.setArrayType(array = array.setElement(arrayObject, 2, value, false));
        assertEquals(value, array.getElement(arrayObject, 2));
        assertEquals(3, array.length(arrayObject));

        arrayObject.setArrayType(array = array.setElement(arrayObject, 3, value, false));
        assertEquals(value, array.getElement(arrayObject, 3));
        assertEquals(4, array.length(arrayObject));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 3, false));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 3));

        arrayObject.setArrayType(array = array.setElement(arrayObject, 5, value, false));
        assertEquals(6, array.length(arrayObject));

        arrayObject.setArrayType(array = array.setLength(arrayObject, 2, false));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 2));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 5));
    }

    @Test
    public void testForwardRanges1() {
        assertForward(INT, 0);
        assertForward(BYTE, 0);
        assertForward(DOUBLE, 0);
        assertForward(OBJECT, 0);
    }

    @Test
    public void testForwardRanges2() {
        assertForward(INT, 50);
        assertForward(BYTE, 50);
        assertForward(DOUBLE, 50);
        assertForward(OBJECT, 50);
    }

    @Test
    public void testBackwardRanges1() {
        assertBackward(INT);
        assertBackward(BYTE);
        assertBackward(DOUBLE);
        assertBackward(OBJECT);
    }

    // TODO backward range
    // TODO Transforms between arrays

    @Test
    public void testMixedTypes1() {
        JSArrayObject array = createEmptyArray(0);
        assertForward(array, 1, BYTE, 0, 50);
        assertForward(array, 1, INT, 0, 50);
        assertForward(array, 1, DOUBLE, 0, 50);
        assertForward(array, 1, OBJECT, 0, 50);
    }

    @Test
    public void testMixedTypes2() {
        JSArrayObject array = createEmptyArray(0);
        assertForward(array, 1, OBJECT, 0, 50);
        assertForward(array, 1, DOUBLE, 0, 50);
        assertForward(array, 1, INT, 0, 50);
        assertForward(array, 1, BYTE, 0, 50);
    }

    @Test
    public void testValidHoles() {
        assertForward(HolesIntArray.HOLE_VALUE, 0);
    }

    @Ignore
    @Test(expected = AssertionError.class)
    public void testInvalidDoubleHoles() {
        JSArrayObject arrayObject = createEmptyArray();
        arrayObject.getArrayType().setElement(arrayObject, 0, HolesDoubleArray.HOLE_VALUE_DOUBLE, false);
    }

    @Ignore
    @Test(expected = AssertionError.class)
    public void testInvalidObjectHoles() {
        JSArrayObject arrayObject = createEmptyArray();
        arrayObject.getArrayType().setElement(arrayObject, 0, null, false);
    }

    private void assertBackward(Object value) {
        assertBackward(createEmptyArray(0), 1, value, 50, 0);
        assertBackward(createEmptyArray(0), 1, value, 100, 50);
        assertBackward(createEmptyArray(0), 2, value, 100, 50);
        assertBackward(createEmptyArray(0), 3, value, 100, 50);
        assertBackward(createEmptyArray(0), JSConfig.MaxArrayHoleSize - 1, value, 500, 50);
        assertBackward(createEmptyArray(0), JSConfig.MaxArrayHoleSize, value, 500, 50);
        assertBackward(createEmptyArray(0), JSConfig.MaxArrayHoleSize + 1, value, 500, 50);
    }

    private void assertForward(Object value, int cap) {
        int stepSize = 1;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);

        stepSize = 2;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);

        stepSize = 3;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);

        stepSize = 5;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);

        stepSize = JSConfig.MaxArrayHoleSize - 1;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);

        stepSize = JSConfig.MaxArrayHoleSize;
        assertForward(createEmptyArray(cap), stepSize, value, 0, 50);
        assertForward(createEmptyArray(cap), stepSize, value, 0, 100);
        assertForward(createEmptyArray(cap), stepSize, value, 100, 200);
    }

    private static void assertBackward(JSArrayObject arrayObject, int stepSize, Object value, int rangeStart, int rangeEnd) {
        String message = "Range " + rangeStart + " to " + rangeEnd + " stepsize " + stepSize;
        ScriptArray array = arrayObject.getArrayType();

        assert rangeEnd < rangeStart;

        int start = rangeStart * stepSize;
        int end = rangeEnd * stepSize;

        for (int i = start; i >= end; i -= stepSize) {
            array = array.setElement(arrayObject, i, value, false);
            arrayObject.setArrayType(array);
        }

        verifyRange(stepSize, value, message, arrayObject);
    }

    private static void verifyRange(int stepSize, Object value, String message, JSArrayObject arrayObject) {
        ScriptArray array = arrayObject.getArrayType();
        Object[] objectArray = array.toArray(arrayObject);
        for (long i = array.firstElementIndex(arrayObject); i <= array.lastElementIndex(arrayObject); i += 1) {
            if (i % stepSize == 0) {
                assertEquals(message, value, array.getElement(arrayObject, i));
                assertEquals(message, value, objectArray[(int) i]);
                assertEquals(true, array.hasElement(arrayObject, i));
            } else {
                assertEquals(message, Undefined.instance, array.getElement(arrayObject, i));
                if (i < objectArray.length) {
                    assertEquals(message, Undefined.instance, objectArray[(int) i]);
                }
                assertEquals(false, array.hasElement(arrayObject, i));
            }
        }

        for (Object element : array.asIterable(arrayObject)) {
            assertEquals(message, value, element);
        }

        for (long i = array.firstElementIndex(arrayObject); i <= array.lastElementIndex(arrayObject); i = array.nextElementIndex(arrayObject, i)) {
            assertEquals(message, value, array.getElement(arrayObject, i));
        }

        for (long i = array.lastElementIndex(arrayObject); i >= array.firstElementIndex(arrayObject); i = array.previousElementIndex(arrayObject, i)) {
            assertEquals(message, value, array.getElement(arrayObject, i));
        }
    }

    private static void assertForward(JSArrayObject arrayObject, int stepSize, Object value, int rangeStart, int rangeEnd) {
        String message = "Range " + rangeStart + " to " + rangeEnd + " stepsize " + stepSize;
        ScriptArray array = arrayObject.getArrayType();

        assert rangeEnd > rangeStart;

        int start = rangeStart * stepSize;
        int end = rangeEnd * stepSize;

        for (int i = start; i < end; i += stepSize) {
            array = array.setElement(arrayObject, i, value, false);
            arrayObject.setArrayType(array);
        }

        verifyRange(stepSize, value, message, arrayObject);
    }

}
