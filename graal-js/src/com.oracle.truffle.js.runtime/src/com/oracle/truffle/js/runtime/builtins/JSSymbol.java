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
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Object wrapper around a primitive symbol.
 *
 * @see Symbol
 */
public final class JSSymbol extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions {

    public static final JSSymbol INSTANCE = new JSSymbol();

    public static final String TYPE_NAME = "symbol";
    public static final String CLASS_NAME = "Symbol";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";
    public static final String DESCRIPTION = "description";

    private static final HiddenKey SYMBOL_DATA_ID = new HiddenKey("Symbol");
    private static final Property SYMBOL_DATA_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        SYMBOL_DATA_PROPERTY = JSObjectUtil.makeHiddenProperty(SYMBOL_DATA_ID, allocator.locationForType(Symbol.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSSymbol() {
    }

    public static DynamicObject create(JSContext context, Symbol symbol) {
        DynamicObject mapObj = JSObject.create(context, context.getSymbolFactory(), symbol);
        assert isJSSymbol(mapObj);
        return mapObj;
    }

    public static Symbol getSymbolData(DynamicObject symbolWrapper) {
        assert JSSymbol.isJSSymbol(symbolWrapper);
        return (Symbol) SYMBOL_DATA_PROPERTY.get(symbolWrapper, JSSymbol.isJSSymbol(symbolWrapper));
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_TO_PRIMITIVE, createToPrimitiveFunction(realm), JSAttributes.configurableNotEnumerableNotWritable());
        if (ctx.getContextOptions().getEcmaScriptVersion() >= JSTruffleOptions.ECMAScript2019) {
            JSObjectUtil.putConstantAccessorProperty(ctx, prototype, DESCRIPTION, createDescriptionGetterFunction(realm), Undefined.instance);
        }
        return prototype;
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSSymbol.INSTANCE, context);
        initialShape = initialShape.addProperty(SYMBOL_DATA_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    private static DynamicObject createToPrimitiveFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSFrameUtil.getThisObj(frame);
                if (obj instanceof Symbol) {
                    return obj;
                } else if (JSSymbol.isJSSymbol(obj)) {
                    return JSSymbol.getSymbolData((DynamicObject) obj);
                } else {
                    throw Errors.createTypeError("Symbol expected");
                }
            }
        });
        return JSFunction.create(realm, JSFunctionData.createCallOnly(context, callTarget, 1, "[Symbol.toPrimitive]"));
    }

    private static DynamicObject createDescriptionGetterFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = frame.getArguments()[0];
                if (obj instanceof Symbol) {
                    return ((Symbol) obj).getDescription();
                } else if (isJSSymbol(obj)) {
                    return JSSymbol.getSymbolData((DynamicObject) obj).getDescription();
                } else {
                    throw Errors.createTypeError("Symbol expected");
                }
            }
        });
        DynamicObject descriptionGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, callTarget, 0, "get " + DESCRIPTION));
        return descriptionGetter;
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
        return "[" + CLASS_NAME + "]";
    }

    public static boolean isJSSymbol(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSymbol((DynamicObject) obj);
    }

    public static boolean isJSSymbol(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSymbolConstructor().getPrototype();
    }
}
