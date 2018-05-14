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
package com.oracle.truffle.js.runtime.interop;

import java.util.EnumSet;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
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
    public static final JavaPackage INSTANCE = JSTruffleOptions.SubstrateVM ? null : new JavaPackage();
    private static final Property PACKAGE_PROPERTY;
    private static final HiddenKey PACKAGE_NAME_ID = new HiddenKey("packageName");

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        PACKAGE_PROPERTY = JSObjectUtil.makeHiddenProperty(PACKAGE_NAME_ID, allocator.locationForType(String.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JavaPackage() {
        assert !JSTruffleOptions.SubstrateVM;
    }

    public static DynamicObject create(JSRealm realm, String packageName) {
        DynamicObject obj = JSObject.create(realm.getContext(), realm.getJavaPackageFactory(), packageName);
        JSObjectUtil.putDataProperty(obj, Symbol.SYMBOL_TO_PRIMITIVE, realm.getJavaPackageToPrimitiveFunction(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        assert isJavaPackage(obj);
        return obj;
    }

    public static boolean isJavaPackage(Object obj) {
        return JSObject.isDynamicObject(obj) && isJavaPackage((DynamicObject) obj);
    }

    public static boolean isJavaPackage(DynamicObject obj) {
        return INSTANCE != null && isInstance(obj, INSTANCE);
    }

    public static String getPackageName(DynamicObject obj) {
        assert isJavaPackage(obj);
        return (String) PACKAGE_PROPERTY.get(obj, isJavaPackage(obj));
    }

    @TruffleBoundary
    public static <T> T getClass(DynamicObject thisObj, String className, Class<? extends T> returnType) {
        JSContext context = JSObject.getJSContext(thisObj);
        TruffleLanguage.Env env = context.getRealm().getEnv();
        assert env.isHostLookupAllowed();
        String qualifiedName = prependPackageName(thisObj, className);
        Object javaType;
        try {
            javaType = env.lookupHostSymbol(qualifiedName);
        } catch (Exception e) {
            return null;
        }
        if (javaType == null) {
            return null;
        }
        if (env.isHostObject(javaType)) {
            Object clazz = env.asHostObject(javaType);
            if (clazz instanceof Class<?>) {
                if (returnType == Class.class) {
                    return returnType.cast(clazz);
                } else if (returnType == JavaClass.class) {
                    return returnType.cast(JavaClass.forClass((Class<?>) clazz));
                } else {
                    return returnType.cast(javaType);
                }
            }
        }
        return null;
    }

    public static DynamicObject subpackage(JSRealm realm, DynamicObject thisObj, String name) {
        return create(realm, prependPackageName(thisObj, name));
    }

    public static Object getJavaClassOrConstructorOrSubPackage(JSContext context, DynamicObject thisObj, String name) {
        if (JSTruffleOptions.NashornJavaInterop && Boundaries.stringEndsWith(name, ")")) {
            // constructor directly? e.g. java.awt["Color(int,int,int)"]
            int openParen = Boundaries.stringIndexOf(name, '(');
            if (openParen != -1) {
                String className = Boundaries.substring(name, 0, openParen);
                JavaClass javaClass = getClass(thisObj, className, JavaClass.class);
                if (javaClass != null) {
                    return javaClass.getBestConstructor(Boundaries.substring(name, openParen + 1, name.length() - 1));
                } else {
                    throw Errors.createTypeErrorFormat("No such Java class: %s", prependPackageName(thisObj, className));
                }
            }
        }
        return getJavaClassOrSubPackage(context.getRealm(), thisObj, name);
    }

    private static Object getJavaClassOrSubPackage(JSRealm realm, DynamicObject thisObj, String name) {
        Object javaClass = getClass(thisObj, name, JSTruffleOptions.NashornJavaInterop ? JavaClass.class : Object.class);
        if (javaClass != null) {
            return javaClass;
        }
        return subpackage(realm, thisObj, name);
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
