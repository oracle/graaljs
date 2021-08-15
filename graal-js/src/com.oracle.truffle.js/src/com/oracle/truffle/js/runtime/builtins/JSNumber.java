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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.NumberFunctionBuiltins;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSNumber extends JSPrimitive implements JSConstructorFactory.Default.WithFunctions {

    public static final String TYPE_NAME = "number";
    public static final String CLASS_NAME = "Number";
    public static final String PROTOTYPE_NAME = "Number.prototype";

    public static final JSNumber INSTANCE = new JSNumber();

    private static final double NUMBER_EPSILON = 2.220446049250313e-16;
    private static final double MAX_SAFE_INTEGER = 9007199254740991d;
    private static final double MIN_SAFE_INTEGER = -9007199254740991d;

    private JSNumber() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, Number value) {
        DynamicObject obj = JSNumberObject.create(realm, context.getNumberFactory(), value);
        assert isJSNumber(obj);
        return context.trackAllocation(obj);
    }

    private static Number getNumberField(DynamicObject obj) {
        assert isJSNumber(obj);
        return ((JSNumberObject) obj).getNumber();
    }

    public static Number valueOf(DynamicObject obj) {
        return getNumberField(obj);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        Shape protoShape = JSShape.createPrototypeShape(realm.getContext(), INSTANCE, realm.getObjectPrototype());
        DynamicObject numberPrototype = JSNumberObject.create(protoShape, 0);
        JSObjectUtil.setOrVerifyPrototype(context, numberPrototype, realm.getObjectPrototype());

        JSObjectUtil.putConstructorProperty(context, numberPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberPrototype, NumberPrototypeBuiltins.BUILTINS);
        return numberPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctions.super.fillConstructor(realm, constructor);

        JSContext context = realm.getContext();
        JSObjectUtil.putDataProperty(context, constructor, JSRuntime.NAN_STRING, Double.NaN);
        JSObjectUtil.putDataProperty(context, constructor, "POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        JSObjectUtil.putDataProperty(context, constructor, "NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY);
        JSObjectUtil.putDataProperty(context, constructor, "MAX_VALUE", Double.MAX_VALUE);
        JSObjectUtil.putDataProperty(context, constructor, "MIN_VALUE", Double.MIN_VALUE);
        if (context.getEcmaScriptVersion() >= 6) {
            JSObjectUtil.putDataProperty(context, constructor, "EPSILON", NUMBER_EPSILON);
            JSObjectUtil.putDataProperty(context, constructor, "MAX_SAFE_INTEGER", MAX_SAFE_INTEGER);
            JSObjectUtil.putDataProperty(context, constructor, "MIN_SAFE_INTEGER", MIN_SAFE_INTEGER);
        }
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, NumberFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSNumber(Object obj) {
        return obj instanceof JSNumberObject;
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
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    @TruffleBoundary
    @Override
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return super.toDisplayStringImpl(obj, depth, allowSideEffects);
        } else {
            Number primitiveValue = JSNumber.valueOf(obj);
            return JSRuntime.objectToConsoleString(obj, getBuiltinToStringTag(obj), depth,
                            new String[]{JSRuntime.PRIMITIVE_VALUE}, new Object[]{primitiveValue}, allowSideEffects);
        }
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getNumberPrototype();
    }
}
