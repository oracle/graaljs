/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
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
        return JSFunction.callDirect((DynamicObject) getter, (DynamicObject) receiver, JSArguments.EMPTY_ARGUMENTS_ARRAY);
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
            JSFunction.call(JSArguments.createOneArg(receiver, setter, value));
            return true;
        }
    }
}
