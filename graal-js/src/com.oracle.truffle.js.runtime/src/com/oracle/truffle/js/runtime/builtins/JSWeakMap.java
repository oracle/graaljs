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
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSWeakMap extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSWeakMap INSTANCE = new JSWeakMap();

    public static final String CLASS_NAME = "WeakMap";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    private static final HiddenKey WEAKMAP_ID = new HiddenKey("weakmap");
    private static final Property WEAKMAP_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        WEAKMAP_PROPERTY = JSObjectUtil.makeHiddenProperty(WEAKMAP_ID, allocator.locationForType(Map.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSWeakMap() {
    }

    @SuppressWarnings("unchecked")
    public static Map<DynamicObject, Object> getInternalWeakMap(DynamicObject obj) {
        assert isJSWeakMap(obj);
        return (Map<DynamicObject, Object>) WEAKMAP_PROPERTY.get(obj, isJSWeakMap(obj));
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSWeakMap.INSTANCE, context);
        initialShape = initialShape.addProperty(WEAKMAP_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
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
    @TruffleBoundary
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return "[" + getClassName() + "]";
        } else {
            return getClassName();
        }
    }

    public static boolean isJSWeakMap(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSWeakMap((DynamicObject) obj);
    }

    public static boolean isJSWeakMap(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWeakMapConstructor().getPrototype();
    }
}
