/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class AbstractJSClass extends JSClass {

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object name, Node encapsulatingNode) {
        throw Errors.createTypeErrorCannotGetProperty(JavaScriptLanguage.getCurrentLanguage().getJSContext(), name, thisObj, false, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        throw Errors.createTypeErrorCannotGetProperty(JavaScriptLanguage.getCurrentLanguage().getJSContext(), String.valueOf(index), thisObj, false, encapsulatingNode);
    }

    @Override
    public Object getMethodHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        return getHelper(store, thisObj, key, encapsulatingNode);
    }

    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        return getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @Override
    public Object getHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        return getOwnHelper(store, thisObj, index, encapsulatingNode);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        throw Errors.createTypeErrorNotAnObject(thisObj);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        throw Errors.createTypeErrorNotAnObject(thisObj);
    }

    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        return hasOwnProperty(thisObj, key);
    }

    @Override
    public boolean hasProperty(DynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        throw Errors.createTypeErrorCannotSetProperty(key, thisObj, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        throw Errors.createTypeErrorCannotSetProperty(String.valueOf(index), thisObj, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        throw Errors.createTypeErrorCannotDeletePropertyOf(key, thisObj);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        throw Errors.createTypeErrorCannotDeletePropertyOf(String.valueOf(index), thisObj);
    }

    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        return Collections.emptyList();
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        if (doThrow) {
            throw Errors.createTypeErrorCannotSetProperty(key, thisObj, null);
        }
        return false;
    }

    @Override
    public boolean preventExtensions(DynamicObject thisObj, boolean doThrow) {
        throw Errors.createTypeErrorNotAnObject(thisObj);
    }

    @Override
    public boolean isExtensible(DynamicObject thisObj) {
        throw Errors.createTypeErrorNotAnObject(thisObj);
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return false;
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }

    @Override
    public boolean usesOrdinaryIsExtensible() {
        return false;
    }

    @Override
    public DynamicObject getPrototypeOf(DynamicObject thisObj) {
        return Null.instance;
    }

    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        return true;
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        throw Errors.createTypeErrorNotAnObject(thisObj);
    }
}
