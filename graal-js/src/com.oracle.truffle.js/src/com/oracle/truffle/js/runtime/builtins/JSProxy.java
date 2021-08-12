/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.ProxyFunctionBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

public final class JSProxy extends AbstractJSClass implements PrototypeSupplier {

    public static final String CLASS_NAME = "Proxy";

    public static final JSProxy INSTANCE = new JSProxy();

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
    public static final String OWN_KEYS = "ownKeys";
    public static final String APPLY = "apply";
    public static final String CONSTRUCT = "construct";

    public static final HiddenKey REVOCABLE_PROXY = new HiddenKey("RevocableProxy");
    public static final HiddenKey REVOKED_CALLABLE = new HiddenKey("RevokedCallable");

    @TruffleBoundary
    public static boolean checkPropertyIsSettable(Object truffleTarget, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (!JSDynamicObject.isJSDynamicObject(truffleTarget)) {
            return true; // best guess for foreign TruffleObject
        }
        DynamicObject target = (DynamicObject) truffleTarget;
        PropertyDescriptor desc = JSObject.getOwnProperty(target, key);
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

    public static DynamicObject create(JSContext context, JSRealm realm, Object target, DynamicObject handler) {
        return JSProxyObject.create(realm, context.getProxyFactory(), target, handler);
    }

    public static Object getTarget(DynamicObject obj) {
        assert isJSProxy(obj);
        return ((JSProxyObject) obj).getProxyTarget();
    }

    /**
     * Gets the target of the proxy. As the target can be a proxy again, retrieves the first
     * non-proxy target.
     */
    public static Object getTargetNonProxy(DynamicObject thisObj) {
        Object obj = thisObj;
        while (JSProxy.isJSProxy(obj)) {
            obj = JSProxy.getTarget((DynamicObject) obj);
        }
        return obj;
    }

    public static DynamicObject getHandler(DynamicObject obj) {
        assert isJSProxy(obj);
        return ((JSProxyObject) obj).getProxyHandler();
    }

    public static DynamicObject getHandlerChecked(DynamicObject obj) {
        DynamicObject handler = getHandler(obj);
        if (handler == Null.instance) {
            throw Errors.createTypeErrorProxyRevoked();
        }
        return handler;
    }

    public static DynamicObject getHandlerChecked(DynamicObject obj, BranchProfile errorBranch) {
        DynamicObject handler = getHandler(obj);
        if (handler == Null.instance) {
            errorBranch.enter();
            throw Errors.createTypeErrorProxyRevoked();
        }
        return handler;
    }

    // ES2015, 26.2.2.1.1
    public static void revoke(DynamicObject obj) {
        assert JSProxy.isJSProxy(obj);
        try {
            ((JSProxyObject) obj).revoke();
        } catch (Exception ex) {
            throw Errors.createTypeError("cannot revoke proxy");
        }
    }

    public static boolean isJSProxy(Object obj) {
        return obj instanceof JSProxyObject;
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        return proxyGetHelper(store, key, receiver, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object receiver, long index, Node encapsulatingNode) {
        assert JSRuntime.isSafeInteger(index);
        return proxyGetHelper(store, Boundaries.stringValueOf(index), receiver, encapsulatingNode);
    }

    @TruffleBoundary
    private static Object proxyGetHelper(DynamicObject proxy, Object key, Object receiver, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(proxy);
        Object target = getTarget(proxy);
        Object trap = getTrapFromObject(handler, GET);
        if (trap == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                JSDynamicObject jsobj = (JSDynamicObject) target;
                return JSObject.getJSClass(jsobj).getHelper(jsobj, receiver, key, encapsulatingNode);
            } else {
                return JSInteropUtil.readMemberOrDefault(target, key, null);
            }
        }

        Object trapResult = JSRuntime.call(trap, handler, new Object[]{target, key, receiver}, encapsulatingNode);
        if (!(handler instanceof JSUncheckedProxyHandlerObject)) {
            checkProxyGetTrapInvariants(target, key, trapResult);
        }
        return trapResult;
    }

    @TruffleBoundary
    public static void checkProxyGetTrapInvariants(Object truffleTarget, Object key, Object trapResult) {
        assert JSRuntime.isPropertyKey(key);
        if (!JSDynamicObject.isJSDynamicObject(truffleTarget)) {
            return; // best effort, cannot check for foreign objects
        }
        DynamicObject target = (DynamicObject) truffleTarget;
        PropertyDescriptor targetDesc = JSObject.getOwnProperty(target, key);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.getConfigurable() && !targetDesc.getWritable()) {
                Object targetValue = targetDesc.getValue();
                if (!JSRuntime.isSameValue(trapResult, targetValue)) {
                    throw Errors.createTypeErrorProxyGetInvariantViolated(key, targetValue, trapResult);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.getConfigurable() && targetDesc.getGet() == Undefined.instance) {
                if (trapResult != Undefined.instance) {
                    throw Errors.createTypeError("Trap result must be undefined since the proxy target has a corresponding non-configurable own accessor property with undefined getter");
                }
            }
        }
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return proxySet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return proxySet(thisObj, Boundaries.stringValueOf(index), value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    private static boolean proxySet(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object trap = getTrapFromObject(handler, SET);
        if (trap == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                JSDynamicObject jsobj = (JSDynamicObject) target;
                return JSObject.getJSClass(jsobj).set(jsobj, key, value, receiver, isStrict, encapsulatingNode);
            } else {
                JSInteropUtil.writeMember(target, key, value);
                return true;
            }
        }

        Object trapResult = JSRuntime.call(trap, handler, new Object[]{target, key, value, receiver}, encapsulatingNode);
        boolean booleanTrapResult = JSRuntime.toBoolean(trapResult);
        if (!booleanTrapResult) {
            if (isStrict) {
                throw Errors.createTypeErrorTrapReturnedFalsish(JSProxy.SET, key);
            } else {
                return false;
            }
        }
        if (handler instanceof JSUncheckedProxyHandlerObject) {
            return true;
        }
        return checkProxySetTrapInvariants(thisObj, key, value);
    }

    @TruffleBoundary
    public static boolean checkProxySetTrapInvariants(DynamicObject proxy, Object key, Object value) {
        assert JSProxy.isJSProxy(proxy);
        assert JSRuntime.isPropertyKey(key);
        Object target = JSProxy.getTarget(proxy);
        if (!JSDynamicObject.isJSDynamicObject(target)) {
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
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, JSRuntime.toString(index));
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
    public boolean hasProperty(DynamicObject thisObj, long index) {
        return hasProperty(thisObj, JSRuntime.toString(index));
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object trap = getTrapFromObject(handler, HAS);
        if (trap == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.hasProperty((DynamicObject) target, key);
            } else {
                return JSInteropUtil.hasProperty(target, key);
            }
        }

        boolean trapResult = JSRuntime.toBoolean(JSRuntime.call(trap, handler, new Object[]{target, key}));
        if (!trapResult) {
            if (!JSProxy.checkPropertyIsSettable(target, key)) {
                throw Errors.createTypeErrorConfigurableExpected();
            }
        }
        return trapResult;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return delete(thisObj, String.valueOf(index), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);

        Object deleteFn = getTrapFromObject(handler, DELETE_PROPERTY);
        if (deleteFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.delete((DynamicObject) target, key, isStrict);
            } else {
                return JSInteropUtil.remove(target, key);
            }
        }
        Object trapResult = JSRuntime.call(deleteFn, handler, new Object[]{target, key});
        boolean booleanTrapResult = JSRuntime.toBoolean(trapResult);
        if (!booleanTrapResult) {
            if (isStrict) {
                throw Errors.createTypeErrorTrapReturnedFalsish(JSProxy.DELETE_PROPERTY, key);
            }
            return false;
        }
        if (!JSDynamicObject.isJSDynamicObject(target)) {
            return true;
        }
        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, key);
        if (targetDesc == null) {
            return true;
        }
        if (targetDesc.hasConfigurable() && !targetDesc.getConfigurable()) {
            throw Errors.createTypeErrorConfigurableExpected();
        }
        JSContext context = JSObject.getJSContext(thisObj);
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2020) {
            boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
            if (!extensibleTarget) {
                throw Errors.createTypeErrorProxyTargetNotExtensible();
            }
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object definePropertyFn = getTrapFromObject(handler, DEFINE_PROPERTY);
        if (definePropertyFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.defineOwnProperty((DynamicObject) target, key, desc, doThrow);
            } else {
                JSInteropUtil.writeMember(target, key, Null.instance);
                return true;
            }
        }
        JSContext context = JSObject.getJSContext(thisObj);
        DynamicObject descObj = JSRuntime.fromPropertyDescriptor(desc, context);
        boolean trapResult = JSRuntime.toBoolean(JSRuntime.call(definePropertyFn, handler, new Object[]{target, key, descObj}));
        if (!trapResult) {
            if (doThrow) {
                // path only hit in V8CompatibilityMode; see JSRuntime.definePropertyOrThrow
                if (handler instanceof JSUncheckedProxyHandlerObject) {
                    throw Errors.createTypeErrorCannotRedefineProperty(key);
                } else {
                    throw Errors.createTypeErrorTrapReturnedFalsish(JSProxy.DEFINE_PROPERTY, key);
                }
            } else {
                return false;
            }
        }
        if (!JSDynamicObject.isJSDynamicObject(target) || handler instanceof JSUncheckedProxyHandlerObject) {
            return true;
        }
        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, key);
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.getConfigurable();
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw Errors.createTypeError("'defineProperty' on proxy: trap returned truish for adding property to the non-extensible proxy target");
            }
            if (settingConfigFalse) {
                throw Errors.createTypeError("'defineProperty' on proxy: trap returned truish for defining non-configurable property which is non-existant in the proxy target");
            }
        } else {
            if (!isCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw Errors.createTypeError("'defineProperty' on proxy: trap returned truish for adding property that is incompatible with the existing property in the proxy target");
            }
            if (settingConfigFalse && targetDesc.getConfigurable()) {
                throw Errors.createTypeError("'defineProperty' on proxy: trap returned truish for defining non-configurable property which is configurable in the proxy target");
            }
            if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2020 && targetDesc.isDataDescriptor() && !targetDesc.getConfigurable() && targetDesc.getWritable() && desc.hasWritable() &&
                            !desc.getWritable()) {
                throw Errors.createTypeError("'defineProperty' on proxy: trap returned truish for defining non-configurable property " +
                                "which cannot be non-writable, unless there exists a corresponding non-configurable, non-writable own property of the proxy target");
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
    public boolean preventExtensions(DynamicObject thisObj, boolean doThrow) {
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object preventExtensionsFn = getTrapFromObject(handler, PREVENT_EXTENSIONS);
        if (preventExtensionsFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.preventExtensions((DynamicObject) target, doThrow);
            } else {
                return true; // unsupported foreign object
            }
        }
        Object returnValue = JSRuntime.call(preventExtensionsFn, handler, new Object[]{target});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (booleanTrapResult && JSDynamicObject.isJSDynamicObject(target)) {
            boolean targetIsExtensible = JSObject.isExtensible((DynamicObject) target);
            if (targetIsExtensible) {
                throw Errors.createTypeError("target is extensible");
            }
        }
        if (doThrow && !booleanTrapResult) {
            throw Errors.createTypeError("'preventExtensions' on proxy: trap returned falsish");
        }
        return booleanTrapResult;
    }

    @TruffleBoundary
    @Override
    public boolean isExtensible(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object isExtensibleFn = getTrapFromObject(handler, IS_EXTENSIBLE);
        if (isExtensibleFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.isExtensible((DynamicObject) target);
            } else {
                return true; // cannot check for foreign objects
            }
        }
        Object returnValue = JSRuntime.call(isExtensibleFn, handler, new Object[]{target});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (!JSDynamicObject.isJSDynamicObject(target)) {
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
        Object targetNonProxy = getTargetNonProxy(object);
        if (JSDynamicObject.isJSDynamicObject(targetNonProxy)) {
            if (JSArray.isJSArray(targetNonProxy)) {
                return JSArray.CLASS_NAME;
            } else if (JSFunction.isJSFunction(targetNonProxy)) {
                return JSFunction.CLASS_NAME;
            } else {
                return "Object";
            }
        } else {
            return "Foreign";
        }
    }

    @Override
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            Object target = getTarget(obj);
            Object handler = getHandler(obj);
            return "Proxy(" + JSRuntime.toDisplayString(target, depth, obj, allowSideEffects) + ", " + JSRuntime.toDisplayString(handler, depth, obj, allowSideEffects) + ")";
        }
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        DynamicObject proxyConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(realm, proxyConstructor, ProxyFunctionBuiltins.BUILTINS);
        // Proxy constructor does not have a prototype property (ES6 26.2.2)
        // Still, makeInitialShape currently needs a dummy prototype
        DynamicObject dummyPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        return new JSConstructor(proxyConstructor, dummyPrototype);
    }

    public static Object getTrapFromObject(DynamicObject maybeHandler, String trapName) {
        Object method = JSObject.get(maybeHandler, trapName);
        if (method == Undefined.instance || method == Null.instance) {
            return Undefined.instance;
        }
        if (!JSRuntime.isCallable(method)) {
            throw Errors.createTypeErrorNotAFunction(method);
        }
        return method;
    }

    @TruffleBoundary
    @Override
    public DynamicObject getPrototypeOf(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object getPrototypeOfFn = getTrapFromObject(handler, GET_PROTOTYPE_OF);
        if (getPrototypeOfFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.getPrototype((DynamicObject) target);
            } else {
                return Null.instance;
            }
        }

        Object handlerProto = JSRuntime.call(getPrototypeOfFn, handler, new Object[]{target});
        if (!JSDynamicObject.isJSDynamicObject(handlerProto) || handlerProto == Undefined.instance) {
            throw Errors.createTypeError("object or null expected");
        }
        DynamicObject handlerProtoObj = (DynamicObject) handlerProto;
        if (!JSDynamicObject.isJSDynamicObject(target)) {
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
        assert JSObjectUtil.isValidPrototype(newPrototype);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object setPrototypeOfFn = getTrapFromObject(handler, SET_PROTOTYPE_OF);
        if (setPrototypeOfFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.setPrototype((DynamicObject) target, newPrototype);
            } else {
                return true; // cannot do for foreign Object
            }
        }
        Object returnValue = JSRuntime.call(setPrototypeOfFn, handler, new Object[]{target, newPrototype});
        boolean booleanTrapResult = JSRuntime.toBoolean(returnValue);
        if (!booleanTrapResult) {
            return false;
        }
        if (!JSDynamicObject.isJSDynamicObject(target)) {
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
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        return filterOwnPropertyKeys(ownPropertyKeysProxy(thisObj), strings, symbols);
    }

    private static List<Object> ownPropertyKeysProxy(DynamicObject thisObj) {
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object ownKeysFn = getTrapFromObject(handler, OWN_KEYS);
        if (ownKeysFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.ownPropertyKeys((DynamicObject) target);
            } else {
                return JSInteropUtil.keys(target);
            }
        }

        Object trapResultArray = JSRuntime.call(ownKeysFn, handler, new Object[]{target});
        List<Object> trapResult = JSRuntime.createListFromArrayLikeAllowSymbolString(trapResultArray);
        if (!JSDynamicObject.isJSDynamicObject(target)) {
            List<Object> uncheckedResultKeys = new ArrayList<>();
            Boundaries.listAddAll(uncheckedResultKeys, trapResult);
            return uncheckedResultKeys;
        }
        if (handler instanceof JSUncheckedProxyHandlerObject) {
            return trapResult;
        }
        JSContext context = JSObject.getJSContext(thisObj);
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2018 && containsDuplicateEntries(trapResult)) {
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
                throw Errors.createTypeErrorOwnKeysTrapMissingKey(key);
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
                throw Errors.createTypeErrorOwnKeysTrapMissingKey(key);
            }
            while (uncheckedResultKeys.remove(key)) {
                // harmony/proxies-ownkeys.js
            }
        }
        if (!uncheckedResultKeys.isEmpty()) {
            throw Errors.createTypeError("'ownKeys' on proxy: trap returned extra keys but proxy target is non-extensible");
        }
        return trapResult;
    }

    @TruffleBoundary
    private static boolean containsDuplicateEntries(List<Object> trapResult) {
        // as spec does not specify, Object.equals should suffice?
        Set<Object> set = new HashSet<>();
        for (Object entry : trapResult) {
            if (!set.add(entry)) {
                return true;
            }
        }
        return false;
    }

    // implements 9.5.5 [[GetOwnProperty]]
    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject handler = getHandlerChecked(thisObj);
        Object target = getTarget(thisObj);
        Object getOwnPropertyFn = getTrapFromObject(handler, GET_OWN_PROPERTY_DESCRIPTOR);
        if (getOwnPropertyFn == Undefined.instance) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.getOwnProperty((DynamicObject) target, key);
            } else {
                return null;
            }
        }
        Object trapResultObj = checkTrapReturnValue(JSRuntime.call(getOwnPropertyFn, handler, new Object[]{target, key}));
        if (!JSDynamicObject.isJSDynamicObject(target)) {
            return JSRuntime.toPropertyDescriptor(trapResultObj);
        }

        PropertyDescriptor targetDesc = JSObject.getOwnProperty((DynamicObject) target, key);
        if (trapResultObj == Undefined.instance) {
            if (targetDesc == null) {
                return null; // undefined
            }
            if (targetDesc.hasConfigurable() && !targetDesc.getConfigurable()) {
                throw Errors.createTypeErrorConfigurableExpected();
            }
            boolean isExtensible = JSObject.isExtensible((DynamicObject) target);
            if (!isExtensible) {
                throw Errors.createTypeErrorProxyTargetNotExtensible();
            }
            return null; // undefined
        }
        boolean extensibleTarget = JSObject.isExtensible((DynamicObject) target);
        PropertyDescriptor resultDesc = JSRuntime.toPropertyDescriptor(trapResultObj);
        completePropertyDescriptor(resultDesc);
        if (handler instanceof JSUncheckedProxyHandlerObject) {
            return resultDesc;
        }
        boolean valid = isCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        if (!valid) {
            throw Errors.createTypeError("not a valid descriptor");
        }
        if (!resultDesc.getConfigurable()) {
            if (targetDesc == null || (targetDesc.hasConfigurable() && targetDesc.getConfigurable())) {
                throw Errors.createTypeErrorFormat(
                                "'getOwnPropertyDescriptor' on proxy: trap reported non-configurability for property '%s' which is either non-existent or configurable in the proxy target", key);
            }
            JSContext context = JSObject.getJSContext(thisObj);
            if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2020 && resultDesc.hasWritable() && !resultDesc.getWritable() && targetDesc.getWritable()) {
                throw Errors.createTypeError("target is missing the corresponding non-configurable and non-writable own property");
            }
        }
        return resultDesc;
    }

    public static boolean isRevoked(DynamicObject proxy) {
        assert JSProxy.isJSProxy(proxy) : "Only proxy objects can be revoked";
        // if the internal handler slot is null, this proxy must have been revoked
        return JSProxy.getHandler(proxy) == Null.instance;
    }

    public static Object checkTrapReturnValue(Object trapResult) {
        if (JSDynamicObject.isJSDynamicObject(trapResult) || trapResult == Undefined.instance) {
            return trapResult;
        } else {
            throw Errors.createTypeError("proxy must return an object");
        }
    }

    @TruffleBoundary
    public static Object call(DynamicObject proxyObj, Object holder, Object[] arguments) {
        DynamicObject handler = getHandlerChecked(proxyObj);
        Object target = getTarget(proxyObj);
        Object trap = getTrapFromObject(handler, APPLY);
        if (trap == Undefined.instance) {
            return JSRuntime.call(target, holder, arguments);
        }
        JSContext ctx = JSObject.getJSContext(proxyObj);
        return JSRuntime.call(trap, handler, new Object[]{target, holder, JSArray.createConstant(ctx, JSRealm.get(null), arguments)});
    }

    @TruffleBoundary
    public static Object construct(DynamicObject proxyObj, Object[] arguments) {
        if (!JSRuntime.isConstructorProxy(proxyObj)) {
            throw Errors.createTypeErrorNotAFunction(proxyObj);
        }
        DynamicObject handler = getHandlerChecked(proxyObj);
        Object target = getTarget(proxyObj);
        Object trap = getTrapFromObject(handler, CONSTRUCT);
        Object newTarget = proxyObj;
        if (trap == Undefined.instance) {
            return JSRuntime.construct(target, arguments);
        }
        JSContext ctx = JSObject.getJSContext(proxyObj);
        Object result = JSRuntime.call(trap, handler, new Object[]{target, JSArray.createConstant(ctx, JSRealm.get(null), arguments), newTarget});
        if (!JSRuntime.isObject(result)) {
            throw Errors.createTypeErrorNotAnObject(result);
        }
        return result;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getProxyPrototype();
    }
}
