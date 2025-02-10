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

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public final class JSSet extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies, PrototypeSupplier {

    public static final JSSet INSTANCE = new JSSet();

    public static final TruffleString CLASS_NAME = Strings.constant("Set");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Set.prototype");

    private JSSet() {
    }

    public static JSSetObject create(JSContext context, JSRealm realm) {
        JSObjectFactory factory = context.getSetFactory();
        return create(factory, realm, factory.getPrototype(realm));
    }

    public static JSSetObject create(JSContext context, JSRealm realm, JSHashMap internalMap) {
        JSObjectFactory factory = context.getSetFactory();
        return create(factory, realm, factory.getPrototype(realm), internalMap);
    }

    public static JSSetObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        JSObjectFactory factory = context.getSetFactory();
        return create(factory, realm, proto);
    }

    private static JSSetObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto) {
        return create(factory, realm, proto, new JSHashMap());
    }

    private static JSSetObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, JSHashMap internalMap) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSSetObject(shape, proto, internalMap), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static Object normalize(Object value) {
        if (value instanceof Double) {
            return normalizeDouble((Double) value);
        }
        return value;
    }

    public static Object normalizeDouble(double value) {
        if (JSRuntime.isNegativeZero(value)) {
            return 0;
        } else if (JSRuntime.doubleIsRepresentableAsInt(value)) {
            return (int) value;
        }
        return value;
    }

    public static JSHashMap getInternalSet(JSDynamicObject obj) {
        return ((JSSetObject) obj).getMap();
    }

    public static int getSetSize(JSDynamicObject obj) {
        return getInternalSet(obj).size();
    }

    @Override
    public JSDynamicObject createPrototype(final JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        // sets the size just for the prototype
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, SetPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SetPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        Object values = JSDynamicObject.getOrNull(prototype, Strings.VALUES);
        // The initial value of the keys and @@iterator properties is the same function object as
        // the initial value of the values property.
        JSObjectUtil.putDataProperty(prototype, Strings.KEYS, values, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(prototype, Symbol.SYMBOL_ITERATOR, values, JSAttributes.getDefaultNotEnumerable());
        if (ctx.getLanguageOptions().newSetMethods()) {
            JSObjectUtil.putFunctionsFromContainer(realm, prototype, SetPrototypeBuiltins.NEW_SET_BUILTINS);
        }
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSSet.INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static boolean isJSSet(Object obj) {
        return obj instanceof JSSetObject;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSetPrototype();
    }

}
