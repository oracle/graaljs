/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

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
}
