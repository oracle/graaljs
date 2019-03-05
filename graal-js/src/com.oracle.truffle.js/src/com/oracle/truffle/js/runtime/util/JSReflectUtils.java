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
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSReflectUtils {

    // Implementation of OrdinaryGet (O, P, Receiver)
    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object performOrdinaryGet(DynamicObject target, Object key, Object receiver) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor descriptor = JSObject.getOwnProperty(target, key);
        if (descriptor == null) {
            DynamicObject parent = JSObject.getPrototype(target);
            if (parent == Null.instance) {
                return Undefined.instance;
            }
            return performOrdinaryGet(parent, key, receiver);
        }
        if (descriptor.isDataDescriptor()) {
            return descriptor.getValue();
        }
        Object getter = descriptor.getGet();
        if (getter == Undefined.instance) {
            return Undefined.instance;
        }
        return JSRuntime.call(getter, receiver, JSArguments.EMPTY_ARGUMENTS_ARRAY);
    }

    // Implementation of OrdinarySet (O, P, V, Receiver)
    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean performOrdinarySet(DynamicObject target, Object key, Object value, Object receiver) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor descriptor = JSObject.getOwnProperty(target, key);
        if (descriptor == null) {
            DynamicObject parent = JSObject.getPrototype(target);
            if (parent != Null.instance) {
                return performOrdinarySet(parent, key, value, receiver);
            } else {
                descriptor = PropertyDescriptor.undefinedDataDesc;
            }
        }
        if (descriptor.isDataDescriptor()) {
            if (!descriptor.getWritable()) {
                return false;
            }
            if (!JSRuntime.isObject(receiver)) {
                return false;
            }
            DynamicObject receiverObj = (DynamicObject) receiver;
            PropertyDescriptor existingDesc = JSObject.getOwnProperty(receiverObj, key);
            if (existingDesc != null) {
                if (existingDesc.isAccessorDescriptor()) {
                    return false;
                }
                if (!existingDesc.getWritable()) {
                    return false;
                }
                PropertyDescriptor valueDesc = PropertyDescriptor.createData(value);
                return JSObject.defineOwnProperty(receiverObj, key, valueDesc);
            } else {
                return JSRuntime.createDataProperty(receiverObj, key, value);
            }
        } else {
            assert descriptor.isAccessorDescriptor();
            Object setter = descriptor.getSet();
            if (setter == Undefined.instance || setter == null) {
                return false;
            }
            JSRuntime.call(setter, receiver, new Object[]{value});
            return true;
        }
    }
}
