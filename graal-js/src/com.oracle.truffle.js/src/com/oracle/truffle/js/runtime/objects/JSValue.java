/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class JSValue extends JSDynamicObject {

    protected JSValue(Shape shape) {
        super(shape);
    }

    @Override
    boolean isObject() {
        return false;
    }

    static JSException typeError() {
        return Errors.createTypeError("not an object");
    }

    @TruffleBoundary
    static JSException cannotDoPropertyOf(String doWhat, Object index, Object thisObj) {
        return Errors.createTypeErrorFormat("Cannot %s property \"%s\" of %s", doWhat, index, JSRuntime.safeToString(thisObj));
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(Object thisObj, Object name) {
        throw cannotDoPropertyOf("get", name, thisObj);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(Object thisObj, long index) {
        throw cannotDoPropertyOf("get", index, thisObj);
    }

    @Override
    public Object getMethodHelper(Object thisObj, Object name) {
        return getHelper(thisObj, name);
    }

    @Override
    public Object getHelper(Object thisObj, Object name) {
        return getOwnHelper(thisObj, name);
    }

    @Override
    public Object getHelper(Object thisObj, long index) {
        return getOwnHelper(thisObj, index);
    }

    @Override
    public boolean hasOwnProperty(Object propName) {
        throw typeError();
    }

    @Override
    public boolean hasOwnProperty(long propIdx) {
        throw typeError();
    }

    @Override
    public boolean hasProperty(Object propName) {
        return hasOwnProperty(propName);
    }

    @Override
    public boolean hasProperty(long propIdx) {
        return hasOwnProperty(propIdx);
    }

    @TruffleBoundary
    @Override
    public boolean set(Object key, Object value, Object receiver, boolean isStrict) {
        throw cannotDoPropertyOf("set", key, this);
    }

    @TruffleBoundary
    @Override
    public boolean set(long index, Object value, Object receiver, boolean isStrict) {
        throw cannotDoPropertyOf("set", index, this);
    }

    @TruffleBoundary
    @Override
    public boolean delete(Object index, boolean isStrict) {
        throw cannotDoPropertyOf("delete", index, this);
    }

    @TruffleBoundary
    @Override
    public boolean delete(long index, boolean isStrict) {
        throw cannotDoPropertyOf("delete", index, this);
    }

    @Override
    public List<Object> getOwnPropertyKeys(boolean string, boolean symbols) {
        return Collections.emptyList();
    }

    @Override
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc, boolean doThrow) {
        if (doThrow) {
            throw Errors.createTypeErrorCannotSetProperty(key, this, null);
        }
        return false;
    }

    @Override
    public boolean preventExtensions(boolean doThrow) {
        throw typeError();
    }

    @Override
    public boolean isExtensible() {
        throw typeError();
    }

    @Override
    public boolean hasOnlyShapeProperties() {
        return false;
    }

    // internal methods

    @Override
    public JSDynamicObject getPrototypeOf() {
        return Null.instance;
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject newPrototype) {
        return true;
    }

    @Override
    public PropertyDescriptor getOwnProperty(Object propertyKey) {
        throw typeError();
    }
}
