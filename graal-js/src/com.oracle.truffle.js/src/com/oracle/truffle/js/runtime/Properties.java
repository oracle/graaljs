/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;

public final class Properties {

    private static boolean validKey(Object key) {
        return !(key instanceof String);
    }

    private static boolean validKeyValue(Object key, Object value) {
        return !(key instanceof String) && !(value instanceof String);
    }

    public static void putWithFlags(DynamicObjectLibrary lib, DynamicObject obj, Object key, Object value, int flags) {
        assert validKeyValue(key, value);
        lib.putWithFlags(obj, key, value, flags);
    }

    public static void putWithFlagsUncached(DynamicObject obj, Object key, Object value, int flags) {
        putWithFlags(DynamicObjectLibrary.getUncached(), obj, key, value, flags);
    }

    public static void putConstant(DynamicObjectLibrary lib, DynamicObject obj, Object key, Object value, int flags) {
        assert validKeyValue(key, value);
        lib.putConstant(obj, key, value, flags);
    }

    public static void putConstantUncached(DynamicObject obj, Object key, Object value, int flags) {
        putConstant(DynamicObjectLibrary.getUncached(), obj, key, value, flags);
    }

    public static Object getOrDefault(DynamicObjectLibrary lib, DynamicObject obj, Object key, Object defaultValue) {
        assert validKeyValue(key, defaultValue);
        return lib.getOrDefault(obj, key, defaultValue);
    }

    public static Object getOrDefaultUncached(DynamicObject obj, Object key, Object defaultValue) {
        return getOrDefault(DynamicObjectLibrary.getUncached(), obj, key, defaultValue);
    }

    public static void put(DynamicObjectLibrary lib, DynamicObject obj, Object key, Object value) {
        assert validKeyValue(key, value);
        lib.put(obj, key, value);
    }

    public static void putUncached(DynamicObject obj, Object key, Object value) {
        put(DynamicObjectLibrary.getUncached(), obj, key, value);
    }

    public static boolean putIfPresent(DynamicObjectLibrary lib, DynamicObject obj, Object key, Object value) {
        assert validKeyValue(key, value);
        return lib.putIfPresent(obj, key, value);
    }

    public static boolean putIfPresentUncached(DynamicObject obj, Object key, Object value) {
        return putIfPresent(DynamicObjectLibrary.getUncached(), obj, key, value);
    }

    public static boolean removeKey(DynamicObjectLibrary lib, DynamicObject obj, Object key) {
        assert validKey(key);
        return lib.removeKey(obj, key);
    }

    public static boolean removeKeyUncached(DynamicObject obj, Object key) {
        return removeKey(DynamicObjectLibrary.getUncached(), obj, key);
    }

    public static boolean containsKey(DynamicObjectLibrary lib, DynamicObject obj, Object key) {
        assert validKey(key);
        return lib.containsKey(obj, key);
    }

    public static boolean containsKeyUncached(DynamicObject obj, Object key) {
        return containsKey(DynamicObjectLibrary.getUncached(), obj, key);
    }

    public static Property getProperty(DynamicObjectLibrary lib, DynamicObject obj, Object key) {
        assert validKey(key);
        return lib.getProperty(obj, key);
    }

    public static Property getPropertyUncached(DynamicObject obj, Object key) {
        return getProperty(DynamicObjectLibrary.getUncached(), obj, key);
    }

    public static void setPropertyFlags(DynamicObjectLibrary lib, DynamicObject obj, Object key, int flags) {
        assert validKey(key);
        lib.setPropertyFlags(obj, key, flags);
    }

    public static void setPropertyFlagsUncached(DynamicObject obj, Object key, int flags) {
        setPropertyFlags(DynamicObjectLibrary.getUncached(), obj, key, flags);
    }
}
