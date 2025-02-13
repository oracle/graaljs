/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array.dyn;

import java.util.Arrays;

import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

final class ArrayCopy {

    private ArrayCopy() {
        // private constructor
    }

    static int[] byteToInt(byte[] array) {
        int[] copyArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            copyArray[i] = array[i];
        }
        return copyArray;
    }

    static double[] byteToDouble(byte[] array) {
        double[] copyArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            copyArray[i] = array[i];
        }
        return copyArray;
    }

    static Object[] byteToObject(byte[] array) {
        Object[] copyArray = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            copyArray[i] = (int) array[i];
        }
        return copyArray;
    }

    static int[] intToInt(int[] array) {
        return Arrays.copyOf(array, array.length);
    }

    static double[] intToDouble(int[] array) {
        return intToDouble(array, 0, array.length);
    }

    static double[] intToDouble(int[] array, int arrayOffset, int usedLength) {
        double[] copyArray = new double[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            copyArray[i] = array[i];
        }
        return copyArray;
    }

    static double[] intToDoubleHoles(int[] array, int arrayOffset, int usedLength) {
        double[] copyArray = new double[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            int intValue = array[i];
            if (HolesIntArray.isHoleValue(intValue)) {
                copyArray[i] = HolesDoubleArray.HOLE_VALUE_DOUBLE;
            } else {
                copyArray[i] = intValue;
            }
        }
        return copyArray;
    }

    static Object[] intToObject(int[] array) {
        return intToObject(array, 0, array.length);
    }

    static Object[] intToObject(int[] array, int arrayOffset, int usedLength) {
        Object[] copyArray = new Object[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            copyArray[i] = array[i];
        }
        return copyArray;
    }

    static Object[] intToObjectHoles(int[] array, int arrayOffset, int usedLength) {
        Object[] copyArray = new Object[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            int intValue = array[i];
            if (intValue == HolesIntArray.HOLE_VALUE) {
                copyArray[i] = null;
            } else {
                copyArray[i] = intValue;
            }
        }
        return copyArray;
    }

    static double[] doubleToDouble(double[] array) {
        return Arrays.copyOf(array, array.length);
    }

    static Object[] doubleToObject(double[] array) {
        return doubleToObject(array, 0, array.length);
    }

    static Object[] doubleToObject(double[] array, int arrayOffset, int usedLength) {
        Object[] copyArray = new Object[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            copyArray[i] = array[i];
        }
        return copyArray;
    }

    static Object[] doubleToObjectHoles(double[] array, int arrayOffset, int usedLength) {
        Object[] copyArray = new Object[array.length];
        for (int i = arrayOffset; i < arrayOffset + usedLength; i++) {
            double value = array[i];
            if (HolesDoubleArray.isHoleValue(value)) {
                copyArray[i] = null;
            } else {
                copyArray[i] = value;
            }
        }
        return copyArray;
    }

    static Object[] objectToObject(Object[] array) {
        return objectToObject(array, array.length);
    }

    static Object[] objectToObject(Object[] array, int usedLength) {
        Object[] newArray = new Object[usedLength];
        System.arraycopy(array, 0, newArray, 0, usedLength);
        return newArray;
    }

    static Object[] jsobjectToObjectHoles(JSDynamicObject[] array, int arrayOffset, int usedLength) {
        return jsobjectToObject(array, arrayOffset, usedLength);
    }

    static Object[] jsobjectToObject(JSDynamicObject[] array, int arrayOffset, int usedLength) {
        Object[] newArray = new Object[array.length];
        System.arraycopy(array, arrayOffset, newArray, arrayOffset, usedLength);
        return newArray;
    }

}
