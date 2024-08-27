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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;

/**
 * @see JSOrdinaryObject
 */
public final class JSOrdinary extends JSNonProxy implements PrototypeSupplier {

    public static final TruffleString TYPE_NAME = Strings.OBJECT;
    public static final TruffleString CLASS_NAME = Strings.UC_OBJECT;
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Object.prototype");

    public static final JSOrdinary INSTANCE = new JSOrdinary();
    public static final CompilableBiFunction<JSContext, JSDynamicObject, Shape> SHAPE_SUPPLIER = (ctx, proto) -> JSObjectUtil.getProtoChildShape(proto, INSTANCE, ctx);

    public static final JSOrdinary BARE_INSTANCE = new JSOrdinary();
    public static final JSOrdinary INTERNAL_FIELD_INSTANCE = new JSOrdinary();
    public static final JSOrdinary OVERLOADED_OPERATORS_INSTANCE = new JSOrdinary();

    /**
     * Initial shape for ordinary-like objects, i.e. objects that behave like ordinary objects but
     * may not extend {@link JSOrdinaryObject}.
     */
    public static final CompilableBiFunction<JSContext, JSDynamicObject, Shape> BARE_SHAPE_SUPPLIER = (ctx, proto) -> JSObjectUtil.getProtoChildShape(proto, BARE_INSTANCE, ctx);

    private JSOrdinary() {
    }

    public static JSObject create(JSContext context, JSObjectFactory factory, JSRealm realm) {
        return createWithRealm(context, factory, realm);
    }

    public static JSObject createWithRealm(JSContext context, JSObjectFactory factory, JSRealm realm) {
        JSObject obj = JSOrdinaryObject.create(factory.getShape(realm), factory.getPrototype(realm));
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }

    public static JSObject create(JSContext context, JSRealm realm) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm));
    }

    public static JSObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        JSObjectFactory factory = context.getOrdinaryObjectFactory();
        JSObject obj = JSOrdinaryObject.createWithDefaultLayout(factory.getShape(realm, proto), proto);
        factory.initProto(obj, realm, proto);
        return context.trackAllocation(obj);
    }

    public static JSObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto) {
        JSObject obj = JSOrdinaryObject.create(factory.getShape(realm), proto);
        factory.initProto(obj, realm);
        return factory.trackAllocation(obj);
    }

    @TruffleBoundary
    public static JSObject createWithPrototype(JSDynamicObject prototype, JSContext context) {
        return createWithPrototype(prototype, context, INSTANCE);
    }

    public static JSObject createWithNullPrototype(JSContext context) {
        return context.trackAllocation(JSOrdinaryObject.createWithDefaultLayout(context.getEmptyShapeNullPrototype(), Null.instance));
    }

    @TruffleBoundary
    public static JSObject createWithPrototype(JSDynamicObject prototype, JSContext context, JSOrdinary instanceLayout) {
        assert JSObjectUtil.isValidPrototype(prototype);
        JSObject obj;
        if (prototype == Null.instance) {
            obj = JSOrdinaryObject.create(context.makeEmptyShapeWithNullPrototype(instanceLayout), prototype);
        } else if (!context.isMultiContext()) {
            obj = JSOrdinaryObject.create(JSObjectUtil.getProtoChildShape(prototype, instanceLayout, context), prototype);
        } else {
            obj = JSOrdinaryObject.create(context.makeEmptyShapeWithPrototypeInObject(instanceLayout), prototype);
            setProtoSlow(obj, prototype);
        }
        return context.trackAllocation(obj);
    }

    public static JSObject createInitWithInstancePrototype(JSDynamicObject prototype, JSContext context) {
        assert JSObjectUtil.isValidPrototype(prototype);
        Shape shape = context.getEmptyShapePrototypeInObject();
        JSOrdinaryObject obj = JSOrdinaryObject.create(shape, prototype);
        setProtoSlow(obj, prototype);
        return obj;
    }

    private static void setProtoSlow(JSObject obj, JSDynamicObject prototype) {
        JSObjectUtil.putHiddenProperty(obj, JSObject.HIDDEN_PROTO, prototype);
    }

    public static JSObject createWithoutPrototype(JSContext context, JSDynamicObject proto) {
        Shape shape = context.getEmptyShapePrototypeInObject();
        JSObject obj = create(context, shape, proto);
        // prototype is set in caller
        return obj;
    }

    public static JSObject create(JSContext context, Shape shape, JSDynamicObject proto) {
        assert JSShape.getJSClass(shape) instanceof JSOrdinary;
        return context.trackAllocation(JSOrdinaryObject.create(shape, proto));
    }

    public static JSObject createInit(JSRealm realm) {
        CompilerAsserts.neverPartOfCompilation();
        return createInit(realm, realm.getObjectPrototype());
    }

    public static JSObject createInit(JSRealm realm, JSDynamicObject prototype) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSObjectUtil.isValidPrototype(prototype);
        JSContext context = realm.getContext();
        if (context.isMultiContext()) {
            return createInitWithInstancePrototype(prototype, context);
        } else {
            return JSOrdinaryObject.create(prototype == Null.instance ? context.getEmptyShapeNullPrototype() : JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context), prototype);
        }
    }

    public static JSObject createWithNullPrototypeInit(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        return JSOrdinaryObject.create(context.getEmptyShapeNullPrototype(), Null.instance);
    }

    public static boolean isJSOrdinaryObject(Object obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && isJSOrdinaryObject((JSDynamicObject) obj);
    }

    public static boolean isJSOrdinaryObject(JSDynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @TruffleBoundary
    @Override
    public Object get(JSDynamicObject thisObj, long index) {
        // convert index only once
        return get(thisObj, Strings.fromLong(index));
    }

    @Override
    public boolean hasOnlyShapeProperties(JSDynamicObject obj) {
        return true;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getObjectPrototype();
    }
}
