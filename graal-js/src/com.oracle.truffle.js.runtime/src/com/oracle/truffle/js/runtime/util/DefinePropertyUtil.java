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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implements ECMAScript 2015's OrdinaryDefineOwnProperty as defined in 9.1.6.1., and connected
 * functionality.
 */
public class DefinePropertyUtil {

    /**
     * Implementation of OrdinaryDefineOwnProperty as defined in ECMAScript 2015, 9.1.6.1.
     */
    @TruffleBoundary
    public static boolean ordinaryDefineOwnProperty(DynamicObject thisObj, Object propertyKey, PropertyDescriptor descriptor, boolean doThrow) {
        PropertyDescriptor current = JSObject.getOwnProperty(thisObj, propertyKey);
        return validateAndApplyPropertyDescriptor(thisObj, propertyKey, JSObject.isExtensible(thisObj), descriptor, current, doThrow);
    }

    /**
     * Implementation of OrdinaryDefineOwnProperty as defined in ECMAScript 2015, 9.1.6.1.
     */
    public static boolean isCompatiblePropertyDescriptor(boolean extensible, PropertyDescriptor descriptor, PropertyDescriptor current) {
        return isCompatiblePropertyDescriptor(extensible, descriptor, current, false);
    }

    public static boolean isCompatiblePropertyDescriptor(boolean extensible, PropertyDescriptor descriptor, PropertyDescriptor current, boolean doThrow) {
        return validateAndApplyPropertyDescriptor(Undefined.instance, Undefined.instance, extensible, descriptor, current, doThrow);
    }

    /**
     * Implementation of ValidateAndApplyPropertyDescriptor as defined in ECMAScript 2015, 9.1.6.3.
     */
    private static boolean validateAndApplyPropertyDescriptor(DynamicObject thisObj, Object propertyKey, boolean extensible, PropertyDescriptor descriptor, PropertyDescriptor current,
                    boolean doThrow) {
        CompilerAsserts.neverPartOfCompilation();
        if (current == null) {
            if (!extensible) {
                return reject(doThrow, "object is not extensible");
            }
            if (thisObj == Undefined.instance) {
                return true;
            }
            return definePropertyNew(thisObj, propertyKey, descriptor, doThrow);
        } else {
            return definePropertyExisting(thisObj, propertyKey, descriptor, doThrow, current);
        }
    }

    public static Property getPropertyByKey(DynamicObject thisObj, Object key) {
        return thisObj.getShape().getProperty(key);
    }

    /**
     * Implementing 8.12.9 [[DefineOwnProperty]], section "5"ff (an existing property is changed).
     *
     */
    private static boolean definePropertyExisting(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow, PropertyDescriptor currentDesc) {
        DynamicObject obj = thisObj;
        boolean currentEnumerable = currentDesc.getEnumerable();
        boolean currentConfigurable = currentDesc.getConfigurable();
        boolean currentWritable = currentDesc.getWritable();

        // 5. Return true, if every field in Desc is absent.
        if (everyFieldAbsent(descriptor)) {
            return true;
        }

        boolean enumerable = descriptor.getIfHasEnumerable(currentEnumerable);
        boolean configurable = descriptor.getIfHasConfigurable(currentConfigurable);

        // 7. If the [[Configurable]] field of current is false then
        // a. Reject, if the [[Configurable]] field of Desc is true.
        // b. Reject, if the [[Enumerable]] field of Desc is present and the [[Enumerable]] fields
        // of current and Desc are the Boolean negation of each other.
        if (!currentConfigurable) {
            if (configurable || (descriptor.hasEnumerable() && (enumerable != currentEnumerable))) {
                return reject(doThrow, nonConfigurableMessage(key));
            }
        }

        int newAttr;
        if (descriptor.isGenericDescriptor()) {
            // 8. "no further validation is required", however:
            // if (current instanceof AccessorProperty) {
            // // we need to adapt the attributes of the (existing) AccessorProperty
            // attributes = current.getAttributes();
            // }
            newAttr = JSAttributes.fromConfigurableEnumerableWritable(configurable, enumerable, currentWritable);
        } else if (currentDesc.isDataDescriptor() && descriptor.isDataDescriptor()) {
            // 10. IsDataDescriptor(current) and IsDataDescriptor(Desc) are both true
            boolean writable = descriptor.getIfHasWritable(currentWritable);
            if (!currentConfigurable) { // 10.a.
                if (!currentWritable) {
                    if (writable) {
                        // 10.a.i. Reject, if the [[Writable]] field of current is false and the
                        // [[Writable]] field of Desc is true.
                        return reject(doThrow, nonConfigurableMessage(key));
                    } else if (descriptor.hasValue()) {
                        // 10.a.ii.1. Reject, if the [[Value]] field of Desc is present and
                        // SameValue(Desc.[[Value]], current.[[Value]]) is false.
                        Object value = descriptor.getValue();
                        if (!JSRuntime.isSameValue(value, currentDesc.getValue())) {
                            return reject(doThrow, nonWritableMessage(key));
                        }
                    }
                    return true;
                }
            }
            newAttr = JSAttributes.fromConfigurableEnumerableWritable(configurable, enumerable, writable);
        } else if (currentDesc.isAccessorDescriptor() && descriptor.isAccessorDescriptor()) {
            // 11. IsAccessorDescriptor(current) and IsAccessorDescriptor(Desc) are both true
            if (!currentConfigurable) { // 11.a.
                // Accessor currentAccessor = (Accessor) current.get(obj, false);
                Accessor currentAccessor = getAccessorFromDescriptor(currentDesc, doThrow);

                if (descriptor.hasSet() && !JSRuntime.isSameValue(descriptor.getSet(), currentAccessor.getSetter())) {
                    return reject(doThrow, nonConfigurableMessage(key));
                }
                if (descriptor.hasGet() && !JSRuntime.isSameValue(descriptor.getGet(), currentAccessor.getGetter())) {
                    return reject(doThrow, nonConfigurableMessage(key));
                }
                return true;
            }
            newAttr = JSAttributes.fromConfigurableEnumerable(configurable, enumerable);
        } else {
            // 9. IsDataDescriptor(current) and IsDataDescriptor(Desc) have different results
            if (!currentConfigurable) {
                // 9.a. Reject, if the [[Configurable]] field of current is false.
                return reject(doThrow, nonConfigurableMessage(key));
            }
            // rest of 9 moved below, after duplicating the shapes

            // writable = false if Accessor->Data else true
            boolean writable = descriptor.getIfHasWritable(currentDesc.isDataDescriptor());
            newAttr = JSAttributes.fromConfigurableEnumerableWritable(configurable, enumerable, writable);
        }

        // verification passed

        if (thisObj == Undefined.instance) {
            return true;
        }
        Property currentProperty = getPropertyByKey(thisObj, key);

        if (JSProperty.isProxy(currentProperty) && descriptor.isDataDescriptor()) {
            PropertyProxy proxy = (PropertyProxy) currentProperty.get(obj, false);
            if (currentProperty.getFlags() != newAttr) {
                if (descriptor.hasValue()) {
                    JSObjectUtil.defineDataProperty(thisObj, key, descriptor.getValue(), newAttr);
                } else {
                    JSObjectUtil.defineProxyProperty(thisObj, key, proxy, newAttr);
                }
            } else if (descriptor.hasValue()) {
                JSObject.set(thisObj, key, descriptor.getValue(), doThrow);
            }
            return true;
        } else {
            if (currentDesc.isDataDescriptor() && descriptor.isDataDescriptor() && currentProperty.getFlags() == newAttr) {
                if (descriptor.hasValue()) {
                    currentProperty.setGeneric(thisObj, descriptor.getValue(), null);
                }
            } else if (currentDesc.isAccessorDescriptor() && descriptor.isAccessorDescriptor()) {
                if (descriptor.hasSet() || descriptor.hasGet()) {
                    // Accessor currentAccessor = (Accessor) current.get(obj, false);
                    Accessor currentAccessor = getAccessorFromDescriptor(currentDesc, doThrow);
                    Accessor newAccessor = getAccessorFromDescriptor(descriptor, doThrow);
                    if (newAccessor == null) {
                        assert !doThrow; // should have thrown
                        return false;
                    }
                    if (currentAccessor.getGetter() != Undefined.instance && !descriptor.hasGet()) {
                        newAccessor = new Accessor(currentAccessor.getGetter(), newAccessor.getSetter());
                    }
                    if (currentAccessor.getSetter() != Undefined.instance && !descriptor.hasSet()) {
                        newAccessor = new Accessor(newAccessor.getGetter(), currentAccessor.getSetter());
                    }

                    if (currentProperty.getFlags() == newAttr) {
                        currentProperty.setGeneric(obj, newAccessor, null);
                    } else {
                        JSObjectUtil.defineAccessorProperty(thisObj, key, newAccessor, newAttr);
                    }
                }
                return true;
            } else if (descriptor.isAccessorDescriptor()) {
                Accessor accessor = getAccessorFromDescriptor(descriptor, doThrow);
                if (accessor == null) {
                    assert !doThrow; // should have thrown
                    return false;
                }
                JSObjectUtil.defineAccessorProperty(thisObj, key, accessor, newAttr);
            } else if (descriptor.isDataDescriptor()) {
                Object value;
                if (descriptor.hasValue()) {
                    value = descriptor.getValue();
                } else if (currentDesc.isDataDescriptor()) {
                    value = currentDesc.getValue();
                } else {
                    value = Undefined.instance;
                }
                JSObjectUtil.defineDataProperty(thisObj, key, value, newAttr);
            } else {
                assert descriptor.isGenericDescriptor();
                if (currentProperty.getFlags() != newAttr) {
                    JSObjectUtil.changeFlags(thisObj, key, newAttr);
                }
            }
            return true;
        }
    }

    /**
     * Implements "return true, if every field in Desc is absent, as defined by 8.12.9 step 5.
     */
    private static boolean everyFieldAbsent(PropertyDescriptor descriptor) {
        return !descriptor.hasValue() && !descriptor.hasGet() && !descriptor.hasSet() && !descriptor.hasConfigurable() && !descriptor.hasEnumerable() && !descriptor.hasWritable();
    }

    /**
     * Implementing 8.12.9 [[DefineOwnProperty]], section "4" (a new property is defined).
     *
     * @return whether the operation was successful
     */
    private static boolean definePropertyNew(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        boolean enumerable = descriptor.getIfHasEnumerable(false);
        boolean configurable = descriptor.getIfHasConfigurable(false);

        JSContext context = JSObject.getJSContext(thisObj);
        if (descriptor.isGenericDescriptor() || descriptor.isDataDescriptor()) {
            return definePropertyNewData(thisObj, key, descriptor, enumerable, configurable, context);
        } else {
            return definePropertyNewAccessor(thisObj, key, descriptor, doThrow, enumerable, configurable, context);
        }
    }

    private static boolean definePropertyNewAccessor(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow, boolean enumerable, boolean configurable, JSContext context) {
        Accessor accessor = getAccessorFromDescriptor(descriptor, doThrow);
        if (accessor == null) {
            assert !doThrow; // should have thrown
            return false;
        }
        JSObjectUtil.putAccessorProperty(context, thisObj, key, accessor, JSAttributes.fromConfigurableEnumerable(configurable, enumerable));
        return true;
    }

    private static boolean definePropertyNewData(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean enumerable, boolean configurable, JSContext context) {
        boolean writable = descriptor.getIfHasWritable(false);

        int attributes = JSAttributes.fromConfigurableEnumerableWritable(configurable, enumerable, writable);
        if (descriptor.hasValue()) {
            JSObjectUtil.putDataProperty(context, thisObj, key, descriptor.getValue(), attributes);
        } else {
            JSObjectUtil.putDeclaredDataProperty(context, thisObj, key, Undefined.instance, attributes);
        }
        return true;
    }

    private static Accessor getAccessorFromDescriptor(PropertyDescriptor descriptor, boolean doThrow) {
        if (descriptor.hasValue()) {
            reject(doThrow, "Invalid property. A property cannot both have accessors and be writable or have a value");
            return null;
        }
        if (descriptor.hasSet() && (descriptor.getSet() != Undefined.instance && !JSRuntime.isCallable(descriptor.getSet()))) {
            reject(doThrow, "setter cannot be called");
            return null;
        }
        if (descriptor.hasGet() && (descriptor.getGet() != Undefined.instance && !JSRuntime.isCallable(descriptor.getGet()))) {
            reject(doThrow, "getter cannot be called");
            return null;
        }
        if (descriptor.hasWritable()) {
            reject(doThrow, "cannot have accessor and data properties");
            return null;
        }

        return new Accessor((DynamicObject) descriptor.getGet(), (DynamicObject) descriptor.getSet());
    }

    public static boolean reject(boolean doThrow, String message) {
        if (doThrow) {
            throw Errors.createTypeError(message);
        }
        return false;
    }

    private static String nonConfigurableMessage(Object key) {
        return JSTruffleOptions.NashornCompatibilityMode ? "property is not configurable" : cannotRedefineMessage(key);
    }

    private static String nonWritableMessage(Object key) {
        return JSTruffleOptions.NashornCompatibilityMode ? "property is not writable" : cannotRedefineMessage(key);
    }

    private static String cannotRedefineMessage(Object key) {
        return JSRuntime.stringConcat("Cannot redefine property: ", JSRuntime.javaToString(key));
    }

}
