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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public final class JSSlowArgumentsArray extends JSAbstractArgumentsArray {
    static final JSSlowArgumentsArray INSTANCE = new JSSlowArgumentsArray();

    private JSSlowArgumentsArray() {
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        boolean isMappedArguments = isMappedArguments(thisObj);
        boolean indexDisconnected = isMappedArguments && wasIndexDisconnected(thisObj, index);
        Object oldValue = indexDisconnected ? null : get(thisObj, index);

        boolean wasDeleted;
        ScriptArray arrayType = arrayGetArrayType(thisObj);
        if (arrayType.hasElement(thisObj, index)) {
            if (arrayType.isSealed()) {
                if (isStrict) {
                    throw Errors.createTypeErrorCannotDeletePropertyOfSealedArray(index);
                }
                return false;
            }
            arraySetArrayType(thisObj, arrayType.deleteElement(thisObj, index, false));
            wasDeleted = true;
        } else {
            wasDeleted = JSOrdinary.INSTANCE.delete(thisObj, index, isStrict);
        }

        if (wasDeleted && isMappedArguments && !indexDisconnected) {
            disconnectIndex(thisObj, index, oldValue);
        }
        return wasDeleted;
    }

    public static boolean isJSSlowArgumentsObject(JSDynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    protected JSDynamicObject makeSlowArray(JSDynamicObject thisObj) {
        assert JSSlowArgumentsArray.isJSSlowArgumentsObject(thisObj);
        return thisObj;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        Object indexAsString = Strings.fromLong(index);
        if (JSOrdinary.INSTANCE.hasOwnProperty(thisObj, indexAsString)) {
            return ordinarySet(thisObj, indexAsString, value, receiver, isStrict, encapsulatingNode);
        }
        return super.set(thisObj, index, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        Object indexAsString = Strings.fromLong(index);
        if (JSOrdinary.INSTANCE.hasOwnProperty(store, indexAsString)) {
            return JSOrdinary.INSTANCE.getOwnHelper(store, thisObj, indexAsString, encapsulatingNode);
        }
        return super.getOwnHelper(store, thisObj, index, encapsulatingNode);
    }
}
