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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractObjectArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;

public class ScriptArrayTest extends JSTest {

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

    private JSArrayObject createEmptyArray(int capacity) {
        return JSArray.createConstantEmptyArray(testHelper.getJSContext(), testHelper.getRealm(), capacity);
    }

    private JSArrayObject createEmptyArray() {
        return JSArray.createConstantEmptyArray(testHelper.getJSContext(), testHelper.getRealm());
    }

    @Test
    public void testIntArray() {
        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();
        assertEquals(0, array.length(arrayObject));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));

        arrayObject = createEmptyArray();
        array = arrayObject.getArrayType();
        for (int i = 0; i < 100; i++) {
            array = array.setElement(arrayObject, i, i, false);
        }
        assertEquals(100, array.length(arrayObject));
        for (int i = 0; i < 100; i++) {
            assertEquals(i, array.getElement(arrayObject, i));
        }
        assertTrue(array instanceof AbstractIntArray);
    }

    @Test
    public void testIntArrayWithHole() {
        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();
        assertEquals(0, array.length(arrayObject));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));

        array = array.setElement(arrayObject, 1, 2, false);
        assertEquals(2, array.length(arrayObject));
        assertEquals(1, array.firstElementIndex(arrayObject));
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));
        array = array.setElement(arrayObject, 0, 1, false);
        assertEquals(2, array.length(arrayObject));
        assertTrue(array instanceof AbstractIntArray);
    }

    @Test
    public void testIntArrayReverseFill() {
        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();
        for (int i = 99; i >= 0; i--) {
            array = array.setElement(arrayObject, i, i, false);
        }
        for (int i = 0; i < 100; i++) {
            assertEquals(i, array.getElement(arrayObject, i));
        }
        assertTrue(array instanceof AbstractIntArray);
    }

    @Test
    public void testCapacity() {
        JSArrayObject arrayObject = createEmptyArray(51);
        ScriptArray array = arrayObject.getArrayType();
        assertEquals(51, array.length(arrayObject));
        array = array.setElement(arrayObject, 50, 50, false);
        assertEquals(Undefined.instance, array.getElement(arrayObject, 0));
        assertEquals(50, array.getElement(arrayObject, 50));
        assertTrue(array instanceof AbstractIntArray);
        array = array.setElement(arrayObject, 51, 51, false);
        assertEquals(52, array.length(arrayObject));
        array.setLength(arrayObject, 49, false);
        assertEquals(49, array.length(arrayObject));
        assertTrue(array.firstElementIndex(arrayObject) >= array.lastElementIndex(arrayObject));
    }

    @Test
    public void testHolesIntArray() {
        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();

        for (int i = 0; i < 100; i += 2) {
            array = array.setElement(arrayObject, i, i, false);
            arrayObject.setArrayType(array);
        }
        assertEquals(99, array.length(arrayObject));
        for (int i = 0; i < 100; i += 2) {
            assertEquals(i, array.getElement(arrayObject, i));
        }
        for (int i = 1; i < 100; i += 2) {
            assertEquals(Undefined.instance, array.getElement(arrayObject, i));
        }
        assertTrue(array instanceof AbstractIntArray);
        assertEquals(50, elementCount(arrayObject));
    }

    @Test
    public void testRewriteIntToDoubleToObject() {
        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();

        for (int i = 0; i < 10; i++) {
            array = array.setElement(arrayObject, i, i, false);
        }
        assertTrue(array instanceof AbstractIntArray);
        array = array.setElement(arrayObject, 4, 4.2, false);
        for (int i = 0; i < 10; i++) {
            if (i != 4) {
                assertEquals((double) i, array.getElement(arrayObject, i));
            } else {
                assertEquals(4.2d, array.getElement(arrayObject, i));
            }
        }
        assertTrue(array instanceof AbstractDoubleArray);
        array = array.setElement(arrayObject, 4, "42", false);
        for (int i = 0; i < 10; i++) {
            if (i != 4) {
                assertEquals((double) i, array.getElement(arrayObject, i));
            } else {
                assertEquals("42", array.getElement(arrayObject, i));
            }
        }
        assertTrue(array instanceof AbstractObjectArray);
    }

    @Test
    public void testSparseArray() {
        Assume.assumeTrue(JSConfig.MaxArrayHoleSize > 0);

        JSArrayObject arrayObject = createEmptyArray();
        ScriptArray array = arrayObject.getArrayType();
        int interval = JSConfig.MaxArrayHoleSize * 2;
        int elementCount = 100;
        int limit = elementCount * interval;

        for (int i = 0; i < limit; i += interval) {
            array = array.setElement(arrayObject, i, i, false);
            arrayObject.setArrayType(array);
        }
        assertEquals(limit - ((limit - 1) % interval), array.length(arrayObject));
        for (int i = 0; i < limit; i += interval) {
            assertEquals(i, array.getElement(arrayObject, i));
        }
        for (int i = 0; i < limit; i += 1) {
            if (i % interval != 0) {
                assertEquals(Undefined.instance, array.getElement(arrayObject, i));
            }
        }
        assertEquals(elementCount, elementCount(arrayObject));
        assertTrue(array instanceof SparseArray);
    }

    @SuppressWarnings("unused")
    private static int elementCount(JSArrayObject arrayObject) {
        int count = 0;
        for (Object x : arrayObject.getArrayType().asIterable(arrayObject)) {
            count++;
        }
        return count;
    }
}
