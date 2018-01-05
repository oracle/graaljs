/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import java.util.Arrays;

import com.oracle.truffle.api.object.DynamicObject;

final class ArrayCopy {

    private ArrayCopy() {
        // private constructor
    }

    static byte[] byteToByte(byte[] array) {
        return Arrays.copyOf(array, array.length);
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

    static Object[] intToObject(int[] array, int arrayOffset, int usedLength, int newLength) {
        Object[] copyArray = new Object[newLength];
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

    static DynamicObject[] jsobjectToJSObject(DynamicObject[] array) {
        return jsobjectToJSObject(array, array.length);
    }

    static DynamicObject[] jsobjectToJSObject(DynamicObject[] array, int usedLength) {
        DynamicObject[] newArray = new DynamicObject[usedLength];
        System.arraycopy(array, 0, newArray, 0, usedLength);
        return newArray;
    }

    static Object[] jsobjectToObjectHoles(DynamicObject[] array, int arrayOffset, int usedLength) {
        return jsobjectToObject(array, arrayOffset, usedLength);
    }

    static Object[] jsobjectToObject(DynamicObject[] array, int arrayOffset, int usedLength) {
        Object[] newArray = new Object[array.length];
        System.arraycopy(array, arrayOffset, newArray, arrayOffset, usedLength);
        return newArray;
    }

}
