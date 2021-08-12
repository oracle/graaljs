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
import com.oracle.truffle.js.builtins.BooleanPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSBoolean extends JSPrimitive implements JSConstructorFactory.Default {

    public static final String TYPE_NAME = "boolean";
    public static final String CLASS_NAME = "Boolean";
    public static final String PROTOTYPE_NAME = "Boolean.prototype";
    public static final String TRUE_NAME = "true";
    public static final String FALSE_NAME = "false";

    public static final JSBoolean INSTANCE = new JSBoolean();

    private JSBoolean() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, boolean value) {
        DynamicObject obj = JSBooleanObject.create(realm, context.getBooleanFactory(), value);
        assert isJSBoolean(obj);
        return context.trackAllocation(obj);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        Shape protoShape = JSShape.createPrototypeShape(realm.getContext(), INSTANCE, realm.getObjectPrototype());
        DynamicObject booleanPrototype = JSBooleanObject.create(protoShape, false);
        JSObjectUtil.setOrVerifyPrototype(ctx, booleanPrototype, realm.getObjectPrototype());

        JSObjectUtil.putConstructorProperty(ctx, booleanPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, booleanPrototype, BooleanPrototypeBuiltins.BUILTINS);
        return booleanPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean valueOf(DynamicObject obj) {
        assert isJSBoolean(obj);
        return ((JSBooleanObject) obj).getBooleanValue();
    }

    public static boolean isJSBoolean(Object obj) {
        return obj instanceof JSBooleanObject;
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
    public static JSException noBooleanError() {
        throw Errors.createTypeError("not a Boolean object");
    }

    @TruffleBoundary
    @Override
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return "[Boolean " + valueOf(obj) + "]";
        } else {
            boolean primitiveValue = JSBoolean.valueOf(obj);
            return JSRuntime.objectToConsoleString(obj, getBuiltinToStringTag(obj), depth,
                            new String[]{JSRuntime.PRIMITIVE_VALUE}, new Object[]{primitiveValue}, allowSideEffects);
        }
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getBooleanPrototype();
    }
}
