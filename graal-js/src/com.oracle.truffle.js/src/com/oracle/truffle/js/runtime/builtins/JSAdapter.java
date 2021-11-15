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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public final class JSAdapter extends AbstractJSClass implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final String CLASS_NAME = "JSAdapter";

    public static final JSAdapter INSTANCE = new JSAdapter();

    private static final String GET = "__get__";
    private static final String PUT = "__put__";
    private static final String HAS = "__has__";
    private static final String CALL = "__call__";
    private static final String DELETE = "__delete__";
    public static final String NEW = "__new__";
    public static final String GET_IDS = "__getIds__";
    public static final String GET_VALUES = "__getValues__";

    private JSAdapter() {
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public String toString() {
        return getClassName();
    }

    public static DynamicObject create(JSContext context, JSRealm realm, DynamicObject adaptee, DynamicObject overrides, DynamicObject proto) {
        JSObjectFactory factory = context.getJSAdapterFactory();
        DynamicObject obj = new JSAdapterObject(factory.getShape(realm), adaptee, overrides);
        factory.initProto(obj, realm);
        if (proto != null) {
            JSObject.setPrototype(obj, proto);
        }
        return context.trackAllocation(obj);
    }

    public static DynamicObject getAdaptee(DynamicObject obj) {
        assert isJSAdapter(obj);
        return ((JSAdapterObject) obj).getAdaptee();
    }

    public static DynamicObject getOverrides(DynamicObject obj) {
        assert isJSAdapter(obj);
        return ((JSAdapterObject) obj).getOverrides();
    }

    public static boolean isJSAdapter(Object obj) {
        return obj instanceof JSAdapterObject;
    }

    private static JSException typeError() {
        return Errors.createTypeError("operation not supported");
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject overrides = getOverrides(store);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return JSObject.get(overrides, key);
        }

        return getIntl(store, key);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        DynamicObject overrides = getOverrides(store);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            return JSObject.get(overrides, index);
        }

        assert JSRuntime.longIsRepresentableAsInt(index);
        return getIntl(store, (int) index);
    }

    private static Object getIntl(DynamicObject thisObj, Object key) {
        if (key instanceof Symbol) {
            return null;
        }
        DynamicObject adaptee = getAdaptee(thisObj);
        Object get = JSObject.get(adaptee, GET);
        if (JSFunction.isJSFunction(get)) {
            return JSFunction.call((DynamicObject) get, thisObj, new Object[]{key});
        }
        return null;
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            return true;
        }
        return hasOwnPropertyIntl(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return true;
        }
        return hasOwnPropertyIntl(thisObj, key);
    }

    private static boolean hasOwnPropertyIntl(DynamicObject thisObj, Object key) {
        DynamicObject adaptee = getAdaptee(thisObj);
        Object has = JSObject.get(adaptee, HAS);
        if (JSFunction.isJSFunction(has)) {
            return JSRuntime.toBoolean(JSFunction.call((DynamicObject) has, thisObj, new Object[]{key}));
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            JSObject.set(overrides, index, value, isStrict, encapsulatingNode);
            return true;
        }

        DynamicObject adaptee = getAdaptee(thisObj);
        Object set = JSObject.get(adaptee, PUT);
        if (JSFunction.isJSFunction(set)) {
            assert JSRuntime.longIsRepresentableAsInt(index);
            JSFunction.call((DynamicObject) set, thisObj, new Object[]{(int) index, value});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return JSObject.set(overrides, key, value, isStrict, encapsulatingNode);
        }

        DynamicObject adaptee = getAdaptee(thisObj);
        Object set = JSObject.get(adaptee, PUT);
        if (JSFunction.isJSFunction(set)) {
            JSFunction.call((DynamicObject) set, thisObj, new Object[]{key, value});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.delete(overrides, index, isStrict)) {
            return true;
        }

        DynamicObject adaptee = getAdaptee(thisObj);
        Object delete = JSObject.get(adaptee, DELETE);
        if (JSFunction.isJSFunction(delete)) {
            JSFunction.call((DynamicObject) delete, thisObj, new Object[]{index});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        DynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.delete(overrides, key, isStrict)) {
            return true;
        }

        DynamicObject adaptee = getAdaptee(thisObj);
        Object delete = JSObject.get(adaptee, DELETE);
        if (JSFunction.isJSFunction(delete)) {
            JSFunction.call((DynamicObject) delete, thisObj, new Object[]{key});
        }
        return true;
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        return set(thisObj, key, desc.getValue(), thisObj, doThrow, null);
    }

    @Override
    public boolean preventExtensions(DynamicObject thisObj, boolean doThrow) {
        throw typeError();
    }

    @Override
    public boolean isExtensible(DynamicObject thisObj) {
        throw typeError();
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        DynamicObject adaptee = getAdaptee(thisObj);
        Object getIds = JSObject.get(adaptee, GET_IDS);
        if (JSFunction.isJSFunction(getIds)) {
            Object returnValue = JSFunction.call((DynamicObject) getIds, thisObj, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            if (JSRuntime.isObject(returnValue)) {
                return filterOwnPropertyKeys(JSRuntime.createListFromArrayLikeAllowSymbolString(returnValue), strings, symbols);
            }
        }
        return super.getOwnPropertyKeys(thisObj, strings, symbols);
    }

    @Override
    public String toDisplayStringImpl(DynamicObject object, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        return defaultToString(object);
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(realm.getContext(), prototype, ctor);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @TruffleBoundary
    @Override
    public Object getMethodHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        if (key instanceof Symbol) {
            return null;
        }
        DynamicObject adaptee = getAdaptee(store);
        Object call = JSObject.get(adaptee, CALL);
        if (JSFunction.isJSFunction(call)) {
            return JSFunction.bind(JSFunction.getRealm((DynamicObject) call), (DynamicObject) call, store, new Object[]{key});
        } else {
            throw createTypeErrorNoSuchFunction(store, key);
        }
    }

    @TruffleBoundary
    private JSException createTypeErrorNoSuchFunction(DynamicObject thisObj, Object key) {
        return Errors.createTypeErrorFormat("%s has no such function \"%s\"", defaultToString(thisObj), key);
    }

    @TruffleBoundary
    @Override
    public DynamicObject getPrototypeOf(DynamicObject thisObj) {
        return JSObjectUtil.getPrototype(thisObj);
    }

    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        return JSNonProxy.setPrototypeStatic(thisObj, newPrototype);
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        throw typeError();
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getJSAdapterPrototype();
    }

}
