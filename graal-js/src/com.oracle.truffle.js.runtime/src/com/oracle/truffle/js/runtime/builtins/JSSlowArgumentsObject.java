/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
