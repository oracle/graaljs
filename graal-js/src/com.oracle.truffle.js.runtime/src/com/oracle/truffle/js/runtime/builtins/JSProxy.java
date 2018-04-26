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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.js.runtime.util.JSReflectUtils;

public final class JSProxy extends AbstractJSClass {

    public static final String CLASS_NAME = "Proxy";

    public static final JSProxy INSTANCE = new JSProxy();

    /* 9.5.15: Internal slots */
    private static final Property PROXY_TARGET_PROPERTY;
    private static final Property PROXY_HANDLER_PROPERTY;

    /* 9.5: Internal methods */
    public static final String GET_PROTOTYPE_OF = "getPrototypeOf";
    public static final String SET_PROTOTYPE_OF = "setPrototypeOf";
    public static final String IS_EXTENSIBLE = "isExtensible";
    public static final String PREVENT_EXTENSIONS = "preventExtensions";
    public static final String GET_OWN_PROPERTY_DESCRIPTOR = "getOwnPropertyDescriptor";
    public static final String HAS = "has";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String DELETE_PROPERTY = "deleteProperty";
    public static final String DEFINE_PROPERTY = "defineProperty";
    public static final String ENUMERATE = "enumerate";
    public static final String OWN_KEYS = "ownKeys";
    public static final String APPLY = "apply";
    public static final String CONSTRUCT = "construct";

    private static final HiddenKey PROXY_TARGET = new HiddenKey("ProxyTarget");
    private static final HiddenKey PROXY_HANDLER = new HiddenKey("ProxyHandler");

    public static final HiddenKey REVOCABLE_PROXY = new HiddenKey("RevocableProxy");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        PROXY_TARGET_PROPERTY = JSObjectUtil.makeHiddenProperty(PROXY_TARGET, allocator.locationForType(TruffleObject.class));
        PROXY_HANDLER_PROPERTY = JSObjectUtil.makeHiddenProperty(PROXY_HANDLER, allocator.locationForType(DynamicObject.class));
    }

    public static boolean isAccessibleProperty(DynamicObject proxy, Object key) {
        TruffleObject target = JSProxy.getTarget(proxy);
        if (JSObject.isJSObject(target)) {
            return checkPropertyIsSettable(target, key);
        } else {
            return true; // best guess
        }
    }

    public static boolean checkPropertyIsSettable(TruffleObject truffleTarget, Object key) {
        if (!JSObject.isJSObject(truffleTarget)) {
            return true; // best guess for foreign TruffleObject
        }
        DynamicObject target = (DynamicObject) truffleTarget;
        PropertyDescriptor desc = JSObject.getOwnProperty(target, JSRuntime.toPropertyKey(key));
        if (desc != null) {
            if (!desc.getConfigurable()) {
                return false;
            }
            if (!JSObject.isExtensible(target)) {
                return false;
            }
        }
        return true;
    }

    private JSProxy() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String toString() {
        return CLASS_NAME;
    }

    public static DynamicObject create(JSContext context, TruffleObject target, DynamicObject handler) {
        return JSObject.create(context, context.getProxyFactory(), target, handler);
    }

    public static TruffleObject getTarget(DynamicObject obj) {
        if (isProxy(obj)) {
            return getTarget(obj, isProxy(obj));
        } else {
            return Undefined.instance;
        }
    }

    public static TruffleObject getTarget(DynamicObject obj, boolean floatingCondition) {
        assert isProxy(obj);
        return (TruffleObject) PROXY_TARGET_PROPERTY.get(obj, floatingCondition);
    }

    /**
     * Gets the target of the proxy. As the target can be a proxy again, retrieves the first
     * non-proxy target.
     */
    public static TruffleObject getTargetNonProxy(DynamicObject thisObj) {
        TruffleObject obj = thisObj;
        while (JSProxy.isProxy(obj)) {
            obj = JSProxy.getTarget((DynamicObject) obj);
        }
        return obj;
    }

    public static DynamicObject getHandler(DynamicObject obj) {
        if (isProxy(obj)) {
            return getHandler(obj, isProxy(obj));
        } else {
            return Undefined.instance;
        }
    }

    public static DynamicObject getHandlerChecked(DynamicObject obj) {
        DynamicObject handler = getHandler(obj);
        if (handler == Null.instance) {
            throw Errors.createTypeError("proxy handler must not be null");
        }
        return handler;
    }

    public static DynamicObject getHandler(DynamicObject obj, boolean floatingCondition) {
        assert isProxy(obj);
        return (DynamicObject) PROXY_HANDLER_PROPERTY.get(obj, floatingCondition);
    }

    // ES2015, 26.2.2.1.1
    public static void revoke(DynamicObject obj) {
        assert JSProxy.isProxy(obj);
        try {
            PROXY_TARGET_PROPERTY.set(obj, Null.instance, null);
            PROXY_HANDLER_PROPERTY.set(obj, Null.instance, null);
        } catch (Exception ex) {
            throw Errors.createTypeError("cannot revoke proxy");
        }
    }

    public static boolean isProxy(Object obj) {
        return JSObject.isDynamicObject(obj) && isProxy((DynamicObject) obj);
    }

    public static boolean isProxy(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, Object name) {
        assert JSRuntime.isPropertyKey(name);
        return proxyGet(store, name, receiver);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, long index) {
        assert JSRuntime.isSafeInteger(index);
        return proxyGet(store, Boundaries.stringValueOf(index), receiver);
    }

    @TruffleBoundary
    private static Object proxyGet(DynamicObject proxy, Object propertyKey, Object receiver) {
        assert JSRuntime.isPropertyKey(propertyKey);
        DynamicObject handler = getHandler(proxy);
        TruffleObject target = getTarget(proxy);
        Object trap = getTrapFromObject(handler, GET);
        if (trap == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSReflectUtils.performOrdinaryGet((DynamicObject) target, propertyKey, receiver);
            } else {
                return JSInteropNodeUtil.read(target, propertyKey);
            }
        }

        Object trapResult = JSFunction.call((DynamicObject) trap, handler, new Object[]{target, propertyKey, receiver});
        checkProxyGetTrapInvariants(target, propertyKey, trapResult);
        return trapResult;
    }

    @TruffleBoundary
    public static void checkProxyGetTrapInvariants(TruffleObject truffleTarget, Object propertyKey, Object trapResult) {
        assert JSRuntime.isPropertyKey(propertyKey);
        if (!JSObject.isJSObject(truffleTarget)) {
            return; // best effort, cannot check for foreign objects
        }
        DynamicObject target = (DynamicObject) truffleTarget;
        PropertyDescriptor targetDesc = JSObject.getOwnProperty(target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.getConfigurable() && !targetDesc.getWritable()) {
                if (!JSRuntime.isSameValue(trapResult, targetDesc.getValue())) {
                    throw Errors.createTypeError("Trap result must be the same as the value of the proxy target's corresponding non-writable, non-configurable own data property");
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.getConfigurable() && targetDesc.getGet() == Undefined.instance) {
                if (trapResult != Undefined.instance) {
                    throw Errors.createTypeError("Trap result must be undefined since the proxy target has a corresponding non-configurable own accessor property with undefined getter");
                }
            }
        }
    }

    @Override
    public boolean set(DynamicObject thisObj, Object propertyKey, Object value, Object receiver, boolean isStrict) {
        return setOwn(thisObj, propertyKey, value, receiver, isStrict);
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        return setOwn(thisObj, index, value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        return proxySet(thisObj, Boundaries.stringValueOf(index), value, receiver);
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        return proxySet(thisObj, key, value, receiver);
    }

    @TruffleBoundary
    private static boolean proxySet(DynamicObject thisObj, Object propertyKey, Object value, Object receiver) {
        assert JSRuntime.isPropertyKey(propertyKey);
        DynamicObject handler = getHandler(thisObj);
        TruffleObject target = getTarget(thisObj);
        Object trap = getTrapFromObject(handler, SET);
        if (trap == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSReflectUtils.performOrdinarySet((DynamicObject) target, propertyKey, value, receiver);
            } else {
                JSInteropNodeUtil.write(target, propertyKey, value);
                return true;
            }
        }

        Object trapResult = JSFunction.call((DynamicObject) trap, handler, new Object[]{target, propertyKey, value, receiver});
        boolean booleanTrapResult = JSRuntime.toBoolean(trapResult);
        if (!booleanTrapResult) {
            return false;
        }
        checkProxySetTrapInvariants(thisObj, propertyKey, value);
        return booleanTrapResult;
    }

    @TruffleBoundary
    public static boolean checkProxySetTrapInvariants(DynamicObject proxy, Object key, Object value) {
        assert JSProxy.isProxy(proxy);
        assert JSRuntime.isPropertyKey(key);
        TruffleObject target = JSProxy.getTarget(proxy);
        if (!JSObject.isJSObject(target)) {
            return true;
        }
        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, key);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.getConfigurable() && !targetDesc.getWritable()) {
                if (!JSRuntime.isSameValue(value, targetDesc.getValue())) {
                    throw Errors.createTypeError("Cannot change the value of a non-writable, non-configurable own data property");
                }
            } else if (targetDesc.isAccessorDescriptor() && !targetDesc.getConfigurable()) {
                if (targetDesc.getSet() == Undefined.instance) {
                    throw Errors.createTypeError("Cannot set the value of a non-configurable own accessor property with undefined setter");
                }
            }
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long propIdx) {
        return hasOwnProperty(thisObj, JSRuntime.toString(propIdx));
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isObject(thisObj);
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor desc = JSObject.getOwnProperty(thisObj, key);
        return desc != null;
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, long propIdx) {
        return hasProperty(thisObj, JSRuntime.toString(propIdx));
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandler(thisObj);
        TruffleObject target = getTarget(thisObj);
        Object trap = getTrapFromObject(handler, HAS);
        if (trap == Undefined.instance) {
            return JSObject.hasOwnProperty(target, key);
        }

        boolean trapResult = JSRuntime.toBoolean(JSFunction.call((DynamicObject) trap, handler, new Object[]{target, key}));
        if (!trapResult && !isAccessibleProperty(thisObj, key)) {
            throw Errors.createTypeErrorConfigurableExpected();
        }
        return trapResult;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long propIdx, boolean isStrict) {
        return delete(thisObj, String.valueOf(propIdx), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object propertyKey, boolean isStrict) {
        assert JSRuntime.isPropertyKey(propertyKey);
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);

        DynamicObject deleteFn = getTrapFromObject(handler, DELETE_PROPERTY);
        if (deleteFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.delete((DynamicObject) target, propertyKey, isStrict);
            } else {
                return JSInteropNodeUtil.remove(target, propertyKey);
            }
        }
        Object trapResult = JSFunction.call(deleteFn, handler, new Object[]{target, propertyKey});
        boolean booleanTrapResult = JSRuntime.toBoolean(trapResult);
        if (!booleanTrapResult) {
            if (isStrict) {
                throw Errors.createTypeErrorTrapReturnedFalsish(JSProxy.DELETE_PROPERTY, propertyKey);
            }
            return false;
        }
        if (!JSObject.isJSObject(target)) {
            return true;
        }
        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, propertyKey);
        if (targetDesc == null) {
            return true;
        }
        if (targetDesc.hasConfigurable() && !targetDesc.getConfigurable()) {
            throw Errors.createTypeErrorConfigurableExpected();
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject definePropertyFn = getTrapFromObject(handler, DEFINE_PROPERTY);
        if (definePropertyFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.defineOwnProperty((DynamicObject) target, key, desc, doThrow);
            } else {
                JSInteropNodeUtil.write(target, key, Null.instance);
                return true;
            }
        }
        JSContext context = JSObject.getJSContext(thisObj);
        DynamicObject descObj = JSRuntime.fromPropertyDescriptor(desc, context);
        boolean trapResult = JSRuntime.toBoolean(JSFunction.call(JSArguments.create(handler, definePropertyFn, target, key, descObj)));
        if (!trapResult) {
            if (doThrow) {
                // path only hit in V8CompatibilityMode; see JSRuntime.definePropertyOrThrow
                throw Errors.createTypeErrorTrapReturnedFalsish(JSProxy.DEFINE_PROPERTY, key);
            } else {
                return false;
            }
        }
        if (!JSObject.isJSObject(target)) {
            return true;
        }
        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, key);
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.getConfigurable();
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw Errors.createTypeError("ES 9.5.6 19.a");
            }
            if (settingConfigFalse) {
                throw Errors.createTypeError("ES 9.5.6 19.b");
            }
        } else {
            if (!isCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw Errors.createTypeError("ES 9.5.6 20.a");
            }
            if (settingConfigFalse && targetDesc.getConfigurable()) {
                throw Errors.createTypeError("ES 9.5.6 20.b");
            }
        }
        return true;
    }

    // ES2015, 6.2.4.6, CompletePropertyDescriptor
    @TruffleBoundary
    private static PropertyDescriptor completePropertyDescriptor(PropertyDescriptor desc) {
        if (desc.isGenericDescriptor() || desc.isDataDescriptor()) {
            if (!desc.hasValue()) {
                desc.setValue(Undefined.instance);
            }
            if (!desc.hasWritable()) {
                desc.setWritable(false);
            }
        } else {
            if (!desc.hasGet()) {
                desc.setGet(null);
            }
            if (!desc.hasSet()) {
                desc.setSet(null);
            }
        }
        if (!desc.hasEnumerable()) {
            desc.setEnumerable(false);
        }
        if (!desc.hasConfigurable()) {
            desc.setConfigurable(false);
        }
        return desc;
    }

    // ES2015, 9.1.6.2 IsCompatiblePropertyDescriptor
    private static boolean isCompatiblePropertyDescriptor(boolean extensibleTarget, PropertyDescriptor desc, PropertyDescriptor current) {
        return DefinePropertyUtil.isCompatiblePropertyDescriptor(extensibleTarget, desc, current);
    }

    @TruffleBoundary
    @Override
    public boolean preventExtensions(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject preventExtensionsFn = getTrapFromObject(handler, PREVENT_EXTENSIONS);
        if (preventExtensionsFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.preventExtensions((DynamicObject) target);
            } else {
                return true; // unsupported foreign object
            }
        }
        Object returnValue = JSFunction.call(preventExtensionsFn, handler, new Object[]{target});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (booleanTrapResult && JSObject.isJSObject(target)) {
            boolean targetIsExtensible = JSObject.isExtensible((DynamicObject) target);
            if (targetIsExtensible) {
                throw Errors.createTypeError("target is extensible");
            }
        }
        return booleanTrapResult;
    }

    @TruffleBoundary
    @Override
    public boolean isExtensible(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject isExtensibleFn = getTrapFromObject(handler, IS_EXTENSIBLE);
        if (isExtensibleFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.isExtensible((DynamicObject) target);
            } else {
                return true; // cannot check for foreign objects
            }
        }
        Object returnValue = JSFunction.call(isExtensibleFn, handler, new Object[]{target});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (!JSObject.isJSObject(target)) {
            return booleanTrapResult;
        }
        boolean targetResult = JSObject.isExtensible((DynamicObject) target);
        if (booleanTrapResult != targetResult) {
            throw Errors.createTypeErrorSameResultExpected();
        }
        return booleanTrapResult;
    }

    // internal methods

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        TruffleObject targetNonProxy = getTargetNonProxy(object);
        if (JSObject.isJSObject(targetNonProxy)) {
            return JSObject.getJSClass((DynamicObject) targetNonProxy).getBuiltinToStringTag((DynamicObject) targetNonProxy);
        } else {
            return "Foreign";
        }
    }

    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return defaultToString(obj);
        } else {
            TruffleObject target = JSProxy.getTargetNonProxy(obj);
            if (JSFunction.isJSFunction(target)) {
                return "Proxy " + JSObject.safeToString((DynamicObject) target); // callable proxy
            } else if (JSObject.isJSObject(target)) {
                return JSRuntime.objectToConsoleString(target, "Proxy");
            } else {
                return JSRuntime.safeToString(target); // eg. foreign TruffleObject
            }
        }
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype, INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        initialShape = initialShape.addProperty(PROXY_TARGET_PROPERTY);
        initialShape = initialShape.addProperty(PROXY_HANDLER_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        DynamicObject proxyConstructor = realm.lookupFunction(JSConstructor.BUILTINS, CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(realm, proxyConstructor, CLASS_NAME);
        // Proxy constructor does not have a prototype property (ES6 26.2.2)
        // Still, makeInitialShape currently needs a dummy prototype
        DynamicObject dummyPrototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        return new JSConstructor(proxyConstructor, dummyPrototype);
    }

    public static DynamicObject getTrapFromObject(DynamicObject maybeHandler, String trapName) {
        Object method = JSObject.get(maybeHandler, trapName);
        if (method == Undefined.instance || method == Null.instance) {
            return Undefined.instance;
        }
        if (!JSFunction.isJSFunction(method)) {
            throw Errors.createTypeErrorNotAFunction(method);
        }
        return (DynamicObject) method;
    }

    @TruffleBoundary
    @Override
    public DynamicObject getPrototypeOf(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject getPrototypeOfFn = getTrapFromObject(handler, GET_PROTOTYPE_OF);
        if (getPrototypeOfFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.getPrototype((DynamicObject) target);
            } else {
                return Null.instance;
            }
        }

        Object handlerProto = JSFunction.call(getPrototypeOfFn, handler, new Object[]{target});
        if ((!JSObject.isJSObject(handlerProto) && handlerProto != Null.instance) || handlerProto == Undefined.instance) {
            throw Errors.createTypeError("object or null expected");
        }
        DynamicObject handlerProtoObj = (DynamicObject) handlerProto;
        if (!JSObject.isJSObject(target)) {
            return handlerProtoObj;
        }
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        if (extensibleTarget) {
            return handlerProtoObj;
        }
        DynamicObject targetProtoObj = JSObject.getPrototype((DynamicObject) target);
        if (handlerProtoObj != targetProtoObj) {
            throw Errors.createTypeErrorSameResultExpected();
        }
        return handlerProtoObj;
    }

    @TruffleBoundary
    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        assert JSObject.isDynamicObject(newPrototype) || newPrototype == Null.instance;
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject setPrototypeOfFn = getTrapFromObject(handler, SET_PROTOTYPE_OF);
        if (setPrototypeOfFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.setPrototype((DynamicObject) target, newPrototype);
            } else {
                return true; // cannot do for foreign Object
            }
        }
        Object returnValue = JSFunction.call(setPrototypeOfFn, handler, new Object[]{target, newPrototype});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (!booleanTrapResult) {
            return false;
        }
        if (!JSObject.isJSObject(target)) {
            return true;
        }
        boolean targetIsExtensible = JSObject.isExtensible((DynamicObject) target);
        if (targetIsExtensible) {
            return true;
        }
        Object targetProto = JSObject.getPrototype((DynamicObject) target);
        if (newPrototype != targetProto) {
            throw Errors.createTypeErrorSameResultExpected();
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public Iterable<Object> ownPropertyKeys(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject ownKeysFn = getTrapFromObject(handler, OWN_KEYS);
        if (ownKeysFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.ownPropertyKeys((DynamicObject) target);
            } else {
                return JSInteropNodeUtil.keys(target);
            }
        }

        Object trapResultArray = JSFunction.call(ownKeysFn, handler, new Object[]{target});
        List<Object> trapResult = JSRuntime.createListFromArrayLikeAllowSymbolString(trapResultArray);
        if (!JSObject.isJSObject(target)) {
            List<Object> uncheckedResultKeys = new ArrayList<>();
            Boundaries.listAddAll(uncheckedResultKeys, trapResult);
            return uncheckedResultKeys;
        }
        JSContext context = JSObject.getJSContext(thisObj);
        if (!context.isOptionV8CompatibilityMode() && context.getEcmaScriptVersion() >= 9 && containsDuplicateEntries(trapResult)) {
            throw Errors.createTypeError("trap result contains duplicate entries");
        }
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        Iterable<Object> targetKeys = JSObject.ownPropertyKeys((DynamicObject) target);
        List<Object> targetConfigurableKeys = new ArrayList<>();
        List<Object> targetNonconfigurableKeys = new ArrayList<>();
        for (Object key : targetKeys) {
            PropertyDescriptor desc = JSObject.getOwnProperty((DynamicObject) target, key);
            if (desc != null && !desc.getConfigurable()) {
                Boundaries.listAdd(targetNonconfigurableKeys, key);
            } else {
                Boundaries.listAdd(targetConfigurableKeys, key);
            }
        }
        if (extensibleTarget && targetNonconfigurableKeys.isEmpty()) {
            return trapResult;
        }
        List<Object> uncheckedResultKeys = new ArrayList<>();
        Boundaries.listAddAll(uncheckedResultKeys, trapResult);
        assert trapResult.size() == uncheckedResultKeys.size();
        for (Object key : targetNonconfigurableKeys) {
            if (!uncheckedResultKeys.contains(key)) {
                throw Errors.createTypeError("Proxy.ownPropertyKeys, 21.a");
            }
            while (uncheckedResultKeys.remove(key)) {
                // harmony/proxies-ownkeys.js
            }
        }
        if (extensibleTarget) {
            return trapResult;
        }
        for (Object key : targetConfigurableKeys) {
            if (!uncheckedResultKeys.contains(key)) {
                throw Errors.createTypeError("Proxy.ownPropertyKeys, 23.a");
            }
            while (uncheckedResultKeys.remove(key)) {
                // harmony/proxies-ownkeys.js
            }
        }
        if (!uncheckedResultKeys.isEmpty()) {
            throw Errors.createTypeError("Proxy.ownPropertyKeys, 24");
        }
        return trapResult;
    }

    private static boolean containsDuplicateEntries(List<Object> trapResult) {
        // as spec does not specify, Object.equals should suffice?
        for (int i = 0; i < trapResult.size(); i++) {
            Object entry = trapResult.get(i);
            for (int j = i + 1; j < trapResult.size(); j++) {
                if (entry.equals(trapResult.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    // implements 9.5.5 [[GetOwnProperty]]
    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey);
        DynamicObject handler = getHandlerChecked(thisObj);
        TruffleObject target = getTarget(thisObj);
        DynamicObject getOwnPropertyFn = getTrapFromObject(handler, GET_OWN_PROPERTY_DESCRIPTOR);
        if (getOwnPropertyFn == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSObject.getOwnProperty((DynamicObject) target, propertyKey);
            } else {
                return null;
            }
        }
        Object trapResultObj = checkTrapReturnValue(JSFunction.call(getOwnPropertyFn, handler, new Object[]{target, propertyKey}));
        if (!JSObject.isJSObject(target)) {
            return JSRuntime.toPropertyDescriptor(trapResultObj);
        }

        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, propertyKey);
        if (trapResultObj == Undefined.instance) {
            if (targetDesc == null) {
                return null; // undefined
            }
            if (targetDesc.hasConfigurable() && !targetDesc.getConfigurable()) {
                throw Errors.createTypeErrorConfigurableExpected();
            }
            boolean isExtensible = JSObject.isExtensible((DynamicObject) target);
            if (!isExtensible) {
                throw Errors.createTypeError("not extensible");
            }
            return null; // undefined
        }
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        PropertyDescriptor resultDesc = JSRuntime.toPropertyDescriptor(trapResultObj);
        completePropertyDescriptor(resultDesc);
        boolean valid = isCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        if (!valid) {
            throw Errors.createTypeError("not a valid descriptor");
        }
        if (!resultDesc.getConfigurable()) {
            if (targetDesc == null || (targetDesc.hasConfigurable() && targetDesc.getConfigurable())) {
                throw Errors.createTypeErrorConfigurableExpected();
            }
        }
        return resultDesc;
    }

    public static boolean isRevoked(DynamicObject proxy) {
        assert JSProxy.isProxy(proxy) : "Only proxy objects can be revoked";
        // if the internal handler slot is null, this proxy must have been revoked
        return JSProxy.getHandler(proxy) == Null.instance;
    }

    public static Object checkTrapReturnValue(Object trapResult) {
        if (JSObject.isDynamicObject(trapResult) || trapResult == Undefined.instance) {
            return trapResult;
        } else {
            throw Errors.createTypeError("proxy must return an object");
        }
    }

    @TruffleBoundary
    public static Object call(DynamicObject proxyObj, Object holder, Object[] arguments) {
        DynamicObject handler = getHandlerChecked(proxyObj);
        TruffleObject target = getTarget(proxyObj);
        DynamicObject trap = getTrapFromObject(handler, APPLY);
        if (trap == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                return JSRuntime.call(target, holder, arguments);
            } else {
                return JSInteropNodeUtil.call(target, arguments);
            }
        }
        JSContext ctx = JSObject.getJSContext(proxyObj);
        return JSRuntime.call(trap, handler, new Object[]{target, holder, JSArray.createConstant(ctx, arguments)});
    }

    @Override
    public ForeignAccess getForeignAccessFactory(DynamicObject object) {
        return JSObject.getJSContext(object).getInteropRuntime().getForeignAccessFactory();
    }

}
