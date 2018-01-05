/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.util.EnumSet;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JavaPackage extends JSBuiltinObject {
    public static final String TYPE_NAME = "object";
    public static final String CLASS_NAME = "JavaPackage";
    public static final JavaPackage INSTANCE = JSTruffleOptions.NashornJavaInterop ? new JavaPackage() : null;
    private static final Property PACKAGE_PROPERTY;
    private static final HiddenKey PACKAGE_NAME_ID = new HiddenKey("packageName");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        PACKAGE_PROPERTY = JSObjectUtil.makeHiddenProperty(PACKAGE_NAME_ID, allocator.locationForType(String.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JavaPackage() {
    }

    public static DynamicObject create(JSContext context, String packageName) {
        assert JSTruffleOptions.NashornJavaInterop;
        DynamicObject obj = JSObject.create(context, context.getRealm().getJavaPackageFactory(), packageName);
        JSObjectUtil.putDataProperty(obj, Symbol.SYMBOL_TO_PRIMITIVE, context.getRealm().getJavaPackageToPrimitiveFunction(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        assert isJavaPackage(obj);
        return obj;
    }

    public static boolean isJavaPackage(Object obj) {
        return JSObject.isDynamicObject(obj) && isJavaPackage((DynamicObject) obj);
    }

    public static boolean isJavaPackage(DynamicObject obj) {
        return JSTruffleOptions.NashornJavaInterop && isInstance(obj, INSTANCE);
    }

    public static String getPackageName(DynamicObject obj) {
        assert isJavaPackage(obj);
        return (String) PACKAGE_PROPERTY.get(obj, isJavaPackage(obj));
    }

    @TruffleBoundary
    public static Class<?> getClass(DynamicObject thisObj, String className) {
        JSContext context = JSObject.getJSContext(thisObj);
        assert context.getEnv().isHostLookupAllowed();
        String qualifiedName = prependPackageName(thisObj, className);
        Object javaType;
        try {
            javaType = context.getEnv().lookupHostSymbol(qualifiedName);
        } catch (Exception e) {
            return null;
        }
        if (JavaInterop.isJavaObject(javaType)) {
            Object clazz = JavaInterop.asJavaObject((TruffleObject) javaType);
            if (clazz instanceof Class<?>) {
                return (Class<?>) clazz;
            }
        }
        return null;
    }

    public static DynamicObject subpackage(JSContext context, DynamicObject thisObj, String name) {
        return create(context, prependPackageName(thisObj, name));
    }

    public static Object getJavaClassOrConstructorOrSubPackage(JSContext context, DynamicObject thisObj, String name) {
        if (Boundaries.stringEndsWith(name, ")")) {
            // constructor directly? e.g. java.awt["Color(int,int,int)"]
            int openParen = Boundaries.stringIndexOf(name, '(');
            if (openParen != -1) {
                String className = Boundaries.substring(name, 0, openParen);
                Class<?> clazz = getClass(thisObj, className);
                if (clazz != null) {
                    return JavaClass.forClass(clazz).getBestConstructor(Boundaries.substring(name, openParen + 1, name.length() - 1));
                } else {
                    throw Errors.createTypeError("No such Java class: %s", prependPackageName(thisObj, className));
                }
            }
        }
        return getJavaClassOrSubPackage(context, thisObj, name);
    }

    private static Object getJavaClassOrSubPackage(JSContext context, DynamicObject thisObj, String name) {
        Class<?> clazz = getClass(thisObj, name);
        return clazz != null ? JavaClass.forClass(clazz) : subpackage(context, thisObj, name);
    }

    @TruffleBoundary
    private static String prependPackageName(DynamicObject thisObj, String className) {
        String packageName = getPackageName(thisObj);
        return (!packageName.isEmpty()) ? packageName + "." + className : className;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    public static String toPrimitiveString(DynamicObject obj) {
        return "[" + CLASS_NAME + " " + getPackageName(obj) + "]";
    }

    public static DynamicObject createToPrimitiveFunction(JSRealm realm) {
        JSContext context = realm.getContext();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                Object obj = JSArguments.getThisObject(arguments);
                Object hint = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : null;

                if (!JSRuntime.isObject(obj)) {
                    throw Errors.createTypeError("cannot call JavaPackage[@@toPrimitive] with non-object argument");
                }
                if (JSRuntime.HINT_STRING.equals(hint)) {
                    return toPrimitiveString((DynamicObject) obj);
                } else if (JSRuntime.HINT_DEFAULT.equals(hint) || JSRuntime.HINT_NUMBER.equals(hint)) {
                    return JSObject.ordinaryToPrimitive((DynamicObject) obj, JSRuntime.HINT_NUMBER);
                } else {
                    throw Errors.createTypeError("invalid hint");
                }
            }
        });
        return JSFunction.create(realm, JSFunctionData.createCallOnly(context, callTarget, 1, "[Symbol.toPrimitive]"));
    }

    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object name) {
        Object propertyValue = super.getHelper(store, thisObj, name);
        if (propertyValue != null) {
            return propertyValue;
        }
        if (name instanceof String) {
            return getJavaClassOrConstructorOrSubPackage(JSObject.getJSContext(store), store, (String) name);
        } else {
            return null;
        }
    }

    public static Shape createInitialShape(JSRealm realm) {
        return JSObjectUtil.getProtoChildShape(realm.getObjectPrototype(), INSTANCE, realm.getContext()).addProperty(PACKAGE_PROPERTY);
    }
}
