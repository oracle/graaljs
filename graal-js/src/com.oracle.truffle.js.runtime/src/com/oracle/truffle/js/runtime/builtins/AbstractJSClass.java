/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.Collections;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class AbstractJSClass extends JSClass {

    private static JSException typeError() {
        return Errors.createNotAnObjectError(null);
    }

    @TruffleBoundary
    private static JSException cannotDoPropertyOf(String doWhat, Object index, Object thisObj) {
        return Errors.createTypeError("Cannot %s property \"%s\" of %s", doWhat, index, JSRuntime.objectToString(thisObj));
    }

    @TruffleBoundary
    protected final JSException typeErrorNoSuchFunction(DynamicObject thisObj, Object name) {
        return Errors.createTypeError("%s has no such function \"%s\"", defaultToString(thisObj), name);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object name) {
        throw cannotDoPropertyOf("get", name, thisObj);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        throw cannotDoPropertyOf("get", index, thisObj);
    }

    @Override
    public Object getMethodHelper(DynamicObject store, Object thisObj, Object name) {
        return getHelper(store, thisObj, name);
    }

    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object name) {
        return getOwnHelper(store, thisObj, name);
    }

    @Override
    public Object getHelper(DynamicObject store, Object thisObj, long index) {
        return getOwnHelper(store, thisObj, index);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object propName) {
        throw typeError();
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long propIdx) {
        throw typeError();
    }

    @Override
    public boolean hasProperty(DynamicObject thisObj, Object propName) {
        return hasOwnProperty(thisObj, propName);
    }

    @Override
    public boolean hasProperty(DynamicObject thisObj, long propIdx) {
        return hasOwnProperty(thisObj, propIdx);
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, Object index, Object value, Object receiver, boolean isStrict) {
        throw cannotDoPropertyOf("set", index, thisObj);
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        throw cannotDoPropertyOf("set", index, thisObj);
    }

    @Override
    public boolean set(DynamicObject thisObj, Object name, Object value, Object receiver, boolean isStrict) {
        return setOwn(thisObj, name, value, receiver, isStrict);
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        return setOwn(thisObj, index, value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object index, boolean isStrict) {
        throw cannotDoPropertyOf("delete", index, thisObj);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        throw cannotDoPropertyOf("delete", index, thisObj);
    }

    @Override
    public Iterable<Object> ownPropertyKeys(DynamicObject thisObj) {
        return Collections.emptyList();
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        if (!setOwn(thisObj, key, desc.getValue(), thisObj, doThrow)) {
            if (isExtensible(thisObj)) {
                JSObjectUtil.putDataProperty(thisObj, key, desc, desc.getFlags());
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean preventExtensions(DynamicObject thisObj) {
        throw typeError();
    }

    @Override
    public boolean isExtensible(DynamicObject thisObj) {
        throw typeError();
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return false;
    }

    // internal methods

    @Override
    public DynamicObject getPrototypeOf(DynamicObject thisObj) {
        return Null.instance;
    }

    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        return true;
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object propertyKey) {
        throw typeError();
    }
}
