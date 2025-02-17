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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public final class JSAdapter extends AbstractJSClass implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("JSAdapter");

    public static final JSAdapter INSTANCE = new JSAdapter();

    private static final TruffleString GET = Strings.constant("__get__");
    private static final TruffleString PUT = Strings.constant("__put__");
    private static final TruffleString HAS = Strings.constant("__has__");
    private static final TruffleString CALL = Strings.constant("__call__");
    private static final TruffleString DELETE = Strings.constant("__delete__");
    public static final TruffleString NEW = Strings.constant("__new__");
    public static final TruffleString GET_IDS = Strings.constant("__getIds__");
    public static final TruffleString GET_VALUES = Strings.constant("__getValues__");

    private JSAdapter() {
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static JSObject create(JSContext context, JSRealm realm, JSDynamicObject adaptee, JSDynamicObject overrides, JSDynamicObject protoOpt) {
        JSObjectFactory factory = context.getJSAdapterFactory();
        JSDynamicObject prototype = protoOpt != null ? protoOpt : factory.getPrototype(realm);
        var shape = factory.getShape(realm, prototype);
        var newObj = new JSAdapterObject(shape, prototype, adaptee, overrides);
        factory.initProto(newObj, realm, prototype);
        factory.trackAllocation(newObj);
        return newObj;
    }

    public static JSDynamicObject getAdaptee(JSDynamicObject obj) {
        return ((JSAdapterObject) obj).getAdaptee();
    }

    public static JSDynamicObject getOverrides(JSDynamicObject obj) {
        return ((JSAdapterObject) obj).getOverrides();
    }

    public static boolean isJSAdapter(Object obj) {
        return obj instanceof JSAdapterObject;
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        JSDynamicObject overrides = getOverrides(store);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return JSObject.get(overrides, key);
        }

        return getIntl(store, key);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        JSDynamicObject overrides = getOverrides(store);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            return JSObject.get(overrides, index);
        }

        assert JSRuntime.longIsRepresentableAsInt(index);
        return getIntl(store, (int) index);
    }

    private static Object getIntl(JSDynamicObject thisObj, Object key) {
        if (key instanceof Symbol) {
            return null;
        }
        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object get = JSObject.get(adaptee, GET);
        if (JSFunction.isJSFunction(get)) {
            return JSFunction.call((JSFunctionObject) get, thisObj, new Object[]{key});
        }
        return null;
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, long index) {
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            return true;
        }
        return hasOwnPropertyIntl(thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return true;
        }
        return hasOwnPropertyIntl(thisObj, key);
    }

    private static boolean hasOwnPropertyIntl(JSDynamicObject thisObj, Object key) {
        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object has = JSObject.get(adaptee, HAS);
        if (JSFunction.isJSFunction(has)) {
            return JSRuntime.toBoolean(JSFunction.call((JSFunctionObject) has, thisObj, new Object[]{key}));
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, index)) {
            JSObject.set(overrides, index, value, isStrict, encapsulatingNode);
            return true;
        }

        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object set = JSObject.get(adaptee, PUT);
        if (JSFunction.isJSFunction(set)) {
            assert JSRuntime.longIsRepresentableAsInt(index);
            JSFunction.call((JSFunctionObject) set, thisObj, new Object[]{(int) index, value});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.hasOwnProperty(overrides, key)) {
            return JSObject.set(overrides, key, value, isStrict, encapsulatingNode);
        }

        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object set = JSObject.get(adaptee, PUT);
        if (JSFunction.isJSFunction(set)) {
            JSFunction.call((JSFunctionObject) set, thisObj, new Object[]{key, value});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.delete(overrides, index, isStrict)) {
            return true;
        }

        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object delete = JSObject.get(adaptee, DELETE);
        if (JSFunction.isJSFunction(delete)) {
            JSFunction.call((JSFunctionObject) delete, thisObj, new Object[]{index});
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        JSDynamicObject overrides = getOverrides(thisObj);
        if (overrides != null && JSObject.delete(overrides, key, isStrict)) {
            return true;
        }

        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object delete = JSObject.get(adaptee, DELETE);
        if (JSFunction.isJSFunction(delete)) {
            JSFunction.call((JSFunctionObject) delete, thisObj, new Object[]{key});
        }
        return true;
    }

    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        return set(thisObj, key, desc.getValue(), thisObj, doThrow, null);
    }

    @Override
    public boolean preventExtensions(JSDynamicObject thisObj, boolean doThrow) {
        return true;
    }

    @Override
    public boolean isExtensible(JSDynamicObject thisObj) {
        return true;
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        JSDynamicObject adaptee = getAdaptee(thisObj);
        Object getIds = JSObject.get(adaptee, GET_IDS);
        if (JSFunction.isJSFunction(getIds)) {
            Object returnValue = JSFunction.call((JSFunctionObject) getIds, thisObj, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            if (JSRuntime.isObject(returnValue)) {
                return filterOwnPropertyKeys(JSRuntime.createListFromArrayLikeAllowSymbolString(returnValue), strings, symbols);
            }
        }
        return super.getOwnPropertyKeys(thisObj, strings, symbols);
    }

    @Override
    public JSDynamicObject createPrototype(final JSRealm realm, JSFunctionObject ctor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @TruffleBoundary
    @Override
    public Object getMethodHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        if (key instanceof Symbol) {
            return null;
        }
        JSDynamicObject adaptee = getAdaptee(store);
        Object call = JSObject.get(adaptee, CALL);
        if (JSFunction.isJSFunction(call)) {
            return JSFunction.bind(JSFunction.getRealm((JSFunctionObject) call), (JSFunctionObject) call, store, new Object[]{key});
        } else {
            throw createTypeErrorNoSuchFunction(store, key);
        }
    }

    @TruffleBoundary
    private static JSException createTypeErrorNoSuchFunction(JSDynamicObject thisObj, Object key) {
        return Errors.createTypeErrorFormat("%s has no such function \"%s\"", thisObj.defaultToString(), key);
    }

    @TruffleBoundary
    @Override
    public JSDynamicObject getPrototypeOf(JSDynamicObject thisObj) {
        return JSObjectUtil.getPrototype(thisObj);
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        return JSNonProxy.setPrototypeStatic(thisObj, newPrototype);
    }

    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        return null;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getJSAdapterPrototype();
    }

}
