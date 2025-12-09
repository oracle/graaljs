/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.interop.JSMetaType;

@ExportLibrary(InteropLibrary.class)
public final class Nullish extends JSDynamicObject {

    public Nullish() {
        super(Null.SHAPE);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isNull() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguageId() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    String getLanguageId() {
        return JavaScriptLanguage.ID;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return this == Undefined.instance ? Undefined.NAME : Null.NAME;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMetaObject() {
        if (JSGuards.isUndefined(this)) {
            return JSMetaType.JS_UNDEFINED;
        } else {
            return JSMetaType.JS_NULL;
        }
    }

    @Override
    public TruffleString getClassName() {
        return getBuiltinToStringTag();
    }

    @Override
    public TruffleString getBuiltinToStringTag() {
        return this == Undefined.instance ? Undefined.NAME : Null.NAME;
    }

    @Override
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        return this == Undefined.instance ? Strings.TO_STRING_VALUE_UNDEFINED : Strings.TO_STRING_VALUE_NULL;
    }

    @Override
    public TruffleString defaultToString() {
        return this == Undefined.instance ? Undefined.NAME : Null.NAME;
    }

    @Override
    boolean isObject() {
        return false;
    }

    @TruffleBoundary
    static JSException cannotDoPropertyOf(String doWhat, Object index, Object thisObj) {
        return Errors.createTypeErrorFormat("Cannot %s property \"%s\" of %s", doWhat, index, JSRuntime.safeToString(thisObj));
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(Object thisObj, Object name, Node encapsulatingNode) {
        throw cannotDoPropertyOf("get", name, thisObj);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(Object thisObj, long index, Node encapsulatingNode) {
        throw cannotDoPropertyOf("get", index, thisObj);
    }

    @Override
    public Object getMethodHelper(Object thisObj, Object name, Node encapsulatingNode) {
        return getHelper(thisObj, name, encapsulatingNode);
    }

    @Override
    public Object getHelper(Object thisObj, Object name, Node encapsulatingNode) {
        return getOwnHelper(thisObj, name, encapsulatingNode);
    }

    @Override
    public Object getHelper(Object thisObj, long index, Node encapsulatingNode) {
        return getOwnHelper(thisObj, index, encapsulatingNode);
    }

    @Override
    public boolean hasOwnProperty(Object propName) {
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @Override
    public boolean hasOwnProperty(long propIdx) {
        throw Errors.createTypeErrorNotAnObject(this);
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
    public boolean set(Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        throw cannotDoPropertyOf("set", key, this);
    }

    @TruffleBoundary
    @Override
    public boolean set(long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
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
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @Override
    public boolean isExtensible() {
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @Override
    public boolean testIntegrityLevel(boolean frozen) {
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @Override
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @Override
    public boolean hasOnlyShapeProperties() {
        return false;
    }

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
        throw Errors.createTypeErrorNotAnObject(this);
    }

    @TruffleBoundary
    @Override
    public String toString() {
        if (this == Undefined.instance) {
            return "JSUndefined";
        } else if (this == Null.instance) {
            return "JSNull";
        }
        return super.toString();
    }

}
