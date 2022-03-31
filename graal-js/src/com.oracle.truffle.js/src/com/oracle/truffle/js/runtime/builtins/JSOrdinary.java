/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
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

    public static final TruffleString TYPE_NAME = Strings.constant("object");
    public static final TruffleString CLASS_NAME = Strings.constant("Object");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Object.prototype");

    public static final JSOrdinary INSTANCE = new JSOrdinary();
    public static final CompilableBiFunction<JSContext, JSDynamicObject, Shape> SHAPE_SUPPLIER = (ctx, proto) -> JSObjectUtil.getProtoChildShape(proto, INSTANCE, ctx);

    public static final JSOrdinary BARE_INSTANCE = new JSOrdinary();
    public static final JSOrdinary INTERNAL_FIELD_INSTANCE = new JSOrdinary();
    public static final JSOrdinary OVERLOADED_OPERATORS_INSTANCE = new JSOrdinary();

    private JSOrdinary() {
    }

    public static JSDynamicObject create(JSContext context, JSObjectFactory factory, JSRealm realm) {
        return createWithRealm(context, factory, realm);
    }

    public static JSDynamicObject createWithRealm(JSContext context, JSObjectFactory factory, JSRealm realm) {
        JSDynamicObject obj = JSOrdinaryObject.create(factory.getShape(realm));
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }

    public static JSDynamicObject create(JSContext context, JSRealm realm) {
        return createWithRealm(context, context.getOrdinaryObjectFactory(), realm);
    }

    @TruffleBoundary
    public static JSDynamicObject createWithPrototype(JSDynamicObject prototype, JSContext context) {
        return createWithPrototype(prototype, context, INSTANCE);
    }

    public static JSDynamicObject createWithNullPrototype(JSContext context) {
        return context.trackAllocation(JSOrdinaryObject.create(context.getEmptyShapeNullPrototype()));
    }

    @TruffleBoundary
    public static JSDynamicObject createWithPrototype(JSDynamicObject prototype, JSContext context, JSOrdinary instanceLayout) {
        assert JSObjectUtil.isValidPrototype(prototype);
        JSDynamicObject obj;
        if (prototype == Null.instance) {
            obj = JSOrdinaryObject.create(context.makeEmptyShapeWithNullPrototype(instanceLayout));
        } else if (!context.isMultiContext()) {
            obj = JSOrdinaryObject.create(JSObjectUtil.getProtoChildShape(prototype, instanceLayout, context));
        } else {
            obj = JSOrdinaryObject.create(context.makeEmptyShapeWithPrototypeInObject(instanceLayout));
            setProtoSlow(obj, prototype);
        }
        return context.trackAllocation(obj);
    }

    public static JSDynamicObject createInitWithInstancePrototype(JSDynamicObject prototype, JSContext context) {
        assert JSObjectUtil.isValidPrototype(prototype);
        Shape shape = context.getEmptyShapePrototypeInObject();
        JSOrdinaryObject obj = JSOrdinaryObject.create(shape);
        setProtoSlow(obj, prototype);
        return obj;
    }

    private static void setProtoSlow(JSDynamicObject obj, JSDynamicObject prototype) {
        JSObjectUtil.putHiddenProperty(obj, JSObject.HIDDEN_PROTO, prototype);
    }

    public static JSDynamicObject createWithoutPrototype(JSContext context) {
        Shape shape = context.getEmptyShapePrototypeInObject();
        JSDynamicObject obj = create(context, shape);
        // prototype is set in caller
        return obj;
    }

    public static JSDynamicObject create(JSContext context, Shape shape) {
        assert JSShape.getJSClass(shape) instanceof JSOrdinary;
        return context.trackAllocation(JSOrdinaryObject.create(shape));
    }

    public static JSDynamicObject createInit(JSRealm realm) {
        CompilerAsserts.neverPartOfCompilation();
        return createInit(realm, realm.getObjectPrototype());
    }

    public static JSDynamicObject createInit(JSRealm realm, JSDynamicObject prototype) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSObjectUtil.isValidPrototype(prototype);
        JSContext context = realm.getContext();
        if (context.isMultiContext()) {
            return createInitWithInstancePrototype(prototype, context);
        } else {
            return JSOrdinaryObject.create(prototype == Null.instance ? context.getEmptyShapeNullPrototype() : JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context));
        }
    }

    public static JSDynamicObject createWithNullPrototypeInit(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        return JSOrdinaryObject.create(context.getEmptyShapeNullPrototype());
    }

    public static boolean isJSOrdinaryObject(Object obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && isJSOrdinaryObject((JSDynamicObject) obj);
    }

    public static boolean isJSOrdinaryObject(JSDynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    @TruffleBoundary
    public TruffleString getClassName(JSDynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        if (context.getEcmaScriptVersion() <= 5) {
            Object toStringTag = get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (Strings.isTString(toStringTag)) {
                return JSRuntime.toStringIsString(toStringTag);
            }
        }
        return CLASS_NAME;
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToDisplayString(obj, allowSideEffects, format, depth, null);
        }
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
