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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSBigInt extends JSPrimitiveObject implements JSConstructorFactory.Default.WithFunctions {

    public static final String TYPE_NAME = "bigint";
    public static final String CLASS_NAME = "BigInt";
    public static final String PROTOTYPE_NAME = "BigInt.prototype";

    public static final JSBigInt INSTANCE = new JSBigInt();

    private static final Property VALUE_PROPERTY;
    private static final HiddenKey VALUE_ID = new HiddenKey("value");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        VALUE_PROPERTY = JSObjectUtil.makeHiddenProperty(VALUE_ID, allocator.locationForType(BigInt.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSBigInt() {
    }

    public static DynamicObject create(JSContext context, BigInt value) {
        DynamicObject objBigInt = JSObject.create(context, context.getBigIntFactory(), value);
        assert isJSBigInt(objBigInt);
        return objBigInt;
    }

    private static BigInt getBigIntegerField(DynamicObject obj) {
        assert isJSBigInt(obj);
        return (BigInt) VALUE_PROPERTY.get(obj, isJSBigInt(obj));
    }

    public static BigInt valueOf(DynamicObject obj) {
        return getBigIntegerField(obj);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject bigIntPrototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(context, bigIntPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, bigIntPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(context, bigIntPrototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return bigIntPrototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        initialShape = initialShape.addProperty(VALUE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean isJSBigInt(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSBigInt((DynamicObject) obj);
    }

    public static boolean isJSBigInt(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @TruffleBoundary
    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return super.safeToString(obj);
        } else {
            BigInt primitiveValue = JSBigInt.valueOf(obj);
            return JSRuntime.objectToConsoleString(obj, getBuiltinToStringTag(obj),
                            new String[]{JSRuntime.PRIMITIVE_VALUE}, new Object[]{primitiveValue});
        }
    }
}
