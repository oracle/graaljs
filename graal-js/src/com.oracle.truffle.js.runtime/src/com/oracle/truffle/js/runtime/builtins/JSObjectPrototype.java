/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyPrototypeArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public final class JSObjectPrototype extends JSBuiltinObject {

    public static final String CLASS_NAME = "Object";

    public static final JSObjectPrototype INSTANCE = new JSObjectPrototype();

    private JSObjectPrototype() {
    }

    public static DynamicObject create(JSContext context) {
        Shape objectPrototypeShape = JSShape.makeEmptyRoot(JSObject.LAYOUT, INSTANCE, context);
        DynamicObject obj = JSObject.create(context, objectPrototypeShape);
        JSAbstractArray.putArrayProperties(obj, ConstantEmptyPrototypeArray.createConstantEmptyPrototypeArray());
        return obj;
    }

    public static boolean isJSObjectPrototype(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSObjectPrototype((DynamicObject) obj);
    }

    public static boolean isJSObjectPrototype(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String safeToString(DynamicObject obj) {
        return defaultToString(obj);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (super.hasOwnProperty(thisObj, key)) {
            return true;
        }
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(index)) {
            return JSObject.getArray(thisObj).hasElement(thisObj, index);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long propIdx) {
        ScriptArray array = JSObject.getArray(thisObj);
        if (array.hasElement(thisObj, propIdx)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, Boundaries.stringValueOf(propIdx));
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        ScriptArray array = JSObject.getArray(store);
        if (array.hasElement(store, index)) {
            return array.getElement(store, index);
        }
        return super.getOwnHelper(store, thisObj, Boundaries.stringValueOf(index));
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return getOwnHelper(store, thisObj, idx);
        }
        return super.getOwnHelper(store, thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0) {
            return delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, key, isStrict);
        }
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        ScriptArray array = JSObject.getArray(thisObj);
        if (array.hasElement(thisObj, index)) {
            JSObject.setArray(thisObj, array.deleteElement(thisObj, index, isStrict));
            return true;
        } else {
            return JSUserObject.INSTANCE.delete(thisObj, index, isStrict);
        }
    }

    @Override
    public Iterable<Object> ownPropertyKeys(DynamicObject thisObj) {
        ScriptArray array = JSObject.getArray(thisObj);
        if (array.length(thisObj) == 0) {
            return super.ownPropertyKeys(thisObj);
        }
        return JSAbstractArray.ownPropertyKeysImpl(thisObj);
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject thisObj) {
        return JSObject.getArray(thisObj).length(thisObj) == 0;
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSAbstractArray.ordinaryGetOwnPropertyArray(thisObj, key);
    }

    /**
     * 9.4.7.2 SetImmutablePrototype ( O, V ).
     */
    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        Object oldPrototype = JSObject.getPrototype(thisObj);
        if (oldPrototype == newPrototype) {
            return true;
        }
        return false;
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        boolean result = super.set(thisObj, index, value, receiver, isStrict);
        JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate();
        return result;
    }

    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        boolean result = super.set(thisObj, key, value, receiver, isStrict);
        if (JSRuntime.isArrayIndex(key)) {
            JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate();
        }
        return result;
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        boolean result = super.defineOwnProperty(thisObj, key, desc, doThrow);
        if (JSRuntime.isArrayIndex(key)) {
            JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate();
        }
        return result;
    }

}
