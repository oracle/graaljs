/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class JSSlowArgumentsObject extends JSAbstractArgumentsObject {
    static final JSSlowArgumentsObject INSTANCE = new JSSlowArgumentsObject();

    private JSSlowArgumentsObject() {
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        if (isSealedOrFrozen(thisObj)) {
            return true;
        }

        Object oldValue = get(thisObj, index);

        boolean wasDeleted;
        if (arrayGetArrayType(thisObj, isJSSlowArgumentsObject(thisObj)).hasElement(thisObj, index)) {
            arraySetArrayType(thisObj, arrayGetArrayType(thisObj, isJSSlowArgumentsObject(thisObj)).deleteElement(thisObj, index, false));
            wasDeleted = true;
        } else {
            wasDeleted = JSUserObject.INSTANCE.delete(thisObj, index, isStrict);
        }

        if (wasDeleted && !wasIndexDisconnected(thisObj, index)) {
            disconnectIndex(thisObj, index, oldValue);
        }
        return wasDeleted;
    }

    private static boolean isSealedOrFrozen(DynamicObject thisObj) {
        ScriptArray array = arrayGetArrayType(thisObj);
        return array.isSealed() || array.isFrozen();
    }

    public static boolean isJSSlowArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    protected DynamicObject makeSlowArray(DynamicObject thisObj) {
        assert JSSlowArgumentsObject.isJSSlowArgumentsObject(thisObj);
        return thisObj;
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSUserObject.INSTANCE.hasOwnProperty(thisObj, indexAsString)) {
            return JSUserObject.INSTANCE.setOwn(thisObj, indexAsString, value, receiver, isStrict);
        }
        return super.set(thisObj, index, value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSUserObject.INSTANCE.hasOwnProperty(store, indexAsString)) {
            return JSUserObject.INSTANCE.getOwnHelper(store, thisObj, indexAsString);
        }
        return super.getOwnHelper(store, thisObj, index);
    }
}
