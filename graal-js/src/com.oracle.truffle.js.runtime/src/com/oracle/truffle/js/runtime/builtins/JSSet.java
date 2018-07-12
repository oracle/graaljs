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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public final class JSSet extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final JSSet INSTANCE = new JSSet();

    public static final String CLASS_NAME = "Set";
    public static final String PROTOTYPE_NAME = "Set.prototype";

    public static final String ITERATOR_CLASS_NAME = "Set Iterator";
    public static final String ITERATOR_PROTOTYPE_NAME = "Set Iterator.prototype";

    private static final String SIZE = "size";

    private static final HiddenKey SET_ID = new HiddenKey("set");
    private static final Property SET_PROPERTY;

    public static final HiddenKey SET_ITERATION_KIND_ID = new HiddenKey("SetIterationKind");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        SET_PROPERTY = JSObjectUtil.makeHiddenProperty(SET_ID, allocator.locationForType(JSHashMap.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSSet() {
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

    public static JSHashMap getInternalSet(DynamicObject obj) {
        assert isJSSet(obj);
        return (JSHashMap) SET_PROPERTY.get(obj, isJSSet(obj));
    }

    public static int getSetSize(DynamicObject obj) {
        assert isJSSet(obj);
        return getInternalSet(obj).size();
    }

    private static DynamicObject createSizeGetterFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = frame.getArguments()[0];
                if (JSSet.isJSSet(obj)) {
                    return JSSet.getSetSize((DynamicObject) obj);
                } else {
                    throw Errors.createTypeError("Set expected");
                }
            }
        });
        DynamicObject sizeGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, callTarget, 0, "get " + SIZE));
        JSObject.preventExtensions(sizeGetter);
        return sizeGetter;
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        // sets the size just for the prototype
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, SIZE, createSizeGetterFunction(realm), Undefined.instance);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        Object values = prototype.get("values");
        // The initial value of the keys and @@iterator properties is the same function object as
        // the initial value of the values property.
        JSObjectUtil.putDataProperty(ctx, prototype, "keys", values, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_ITERATOR, values, JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSSet.INSTANCE, context);
        initialShape = initialShape.addProperty(SET_PROPERTY);
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
            JSHashMap set = JSSet.getInternalSet(obj);
            return JSRuntime.collectionToConsoleString(obj, getClassName(obj), set);
        }
    }

    public static boolean isJSSet(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSet((DynamicObject) obj);
    }

    public static boolean isJSSet(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}
