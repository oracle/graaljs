/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.sort;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;

import java.util.Comparator;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class SortComparator implements Comparator<Object> {
    private final Object compFnObj;
    private final boolean isFunction;

    public SortComparator(Object compFnObj) {
        this.compFnObj = compFnObj;
        this.isFunction = JSFunction.isJSFunction(compFnObj);
    }

    @Override
    public int compare(Object arg0, Object arg1) {
        if (arg0 == Undefined.instance) {
            if (arg1 == Undefined.instance) {
                return 0;
            }
            return 1;
        } else if (arg1 == Undefined.instance) {
            return -1;
        }
        Object retObj;
        if (isFunction) {
            retObj = JSFunction.call((JSFunctionObject) compFnObj, Undefined.instance, new Object[]{arg0, arg1});
        } else {
            retObj = JSRuntime.call(compFnObj, Undefined.instance, new Object[]{arg0, arg1});
        }
        return convertResult(retObj);
    }

    private static int convertResult(Object retObj) {
        if (retObj instanceof Integer) {
            return (int) retObj;
        } else {
            double d = JSRuntime.toDouble(retObj);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            } else {
                // +/-0 or NaN
                return 0;
            }
        }
    }

    public static Comparator<Object> getDefaultComparator(Object thisObj, boolean isTypedArrayImplementation) {
        if (isTypedArrayImplementation) {
            return null; // use Comparable.compareTo (equivalent to Comparator.naturalOrder())
        } else {
            if (JSArray.isJSFastArray(thisObj)) {
                ScriptArray array = arrayGetArrayType((JSDynamicObject) thisObj);
                if (array instanceof AbstractIntArray || array instanceof ConstantByteArray || array instanceof ConstantIntArray) {
                    return JSArray.DEFAULT_JSARRAY_INTEGER_COMPARATOR;
                } else if (array instanceof AbstractDoubleArray || array instanceof ConstantDoubleArray) {
                    return JSArray.DEFAULT_JSARRAY_DOUBLE_COMPARATOR;
                }
            }
            return JSArray.DEFAULT_JSARRAY_COMPARATOR;
        }
    }
}
