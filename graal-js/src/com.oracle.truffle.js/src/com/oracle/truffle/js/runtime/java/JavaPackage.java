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
package com.oracle.truffle.js.runtime.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JavaPackage extends JSNonProxy {
    public static final String TYPE_NAME = "object";
    public static final String CLASS_NAME = "JavaPackage";
    public static final JavaPackage INSTANCE = new JavaPackage();

    private JavaPackage() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, String packageName) {
        JavaPackageObject obj = createInstance(context, realm, packageName);
        return context.trackAllocation(obj);
    }

    public static DynamicObject createInit(JSRealm realm, String packageName) {
        CompilerAsserts.neverPartOfCompilation();
        JSContext context = realm.getContext();
        return createInstance(context, realm, packageName);
    }

    private static JavaPackageObject createInstance(JSContext context, JSRealm realm, String packageName) {
        JSObjectFactory factory = context.getJavaPackageFactory();
        JavaPackageObject obj = new JavaPackageObject(factory.getShape(realm), packageName);
        factory.initProto(obj, realm);
        JSObjectUtil.putDataProperty(context, obj, Symbol.SYMBOL_TO_PRIMITIVE, realm.getJavaPackageToPrimitiveFunction(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        assert isJavaPackage(obj);
        return obj;
    }

    public static boolean isJavaPackage(Object obj) {
        return obj instanceof JavaPackageObject;
    }

    public static String getPackageName(DynamicObject obj) {
        assert isJavaPackage(obj);
        return ((JavaPackageObject) obj).getPackageName();
    }

    @TruffleBoundary
    public static <T> T getClass(JSRealm realm, DynamicObject thisObj, String className, Class<? extends T> returnType) {
        TruffleLanguage.Env env = realm.getEnv();
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
                } else {
                    return returnType.cast(javaType);
                }
            }
        }
        return null;
    }

    public static DynamicObject subpackage(JSContext context, JSRealm realm, DynamicObject thisObj, String name) {
        return create(context, realm, prependPackageName(thisObj, name));
    }

    public static Object getJavaClassOrConstructorOrSubPackage(JSContext context, DynamicObject thisObj, String name) {
        JSRealm realm = JSRealm.get(null);
        if (context.isOptionNashornCompatibilityMode() && Boundaries.stringEndsWith(name, ")")) {
            // constructor directly? e.g. java.awt["Color(int,int,int)"]
            int openParen = name.indexOf('(');
            if (openParen != -1) {
                String className = Boundaries.substring(name, 0, openParen);
                Object javaClass = getClass(realm, thisObj, className, Object.class);
                if (javaClass != null) {
                    return javaClass;
                } else {
                    throw Errors.createTypeErrorFormat("No such Java class: %s", prependPackageName(thisObj, className));
                }
            }
        }
        return getJavaClassOrSubPackage(context, realm, thisObj, name);
    }

    private static Object getJavaClassOrSubPackage(JSContext context, JSRealm realm, DynamicObject thisObj, String name) {
        Object javaClass = getClass(realm, thisObj, name, Object.class);
        if (javaClass != null) {
            return javaClass;
        }
        return subpackage(context, realm, thisObj, name);
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

    @TruffleBoundary
    public static String toPrimitiveString(DynamicObject obj) {
        return "[" + CLASS_NAME + " " + getPackageName(obj) + "]";
    }

    public static DynamicObject createToPrimitiveFunction(JSContext context, JSRealm realm) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.JavaPackageToPrimitive, JavaPackage::createToPrimitiveFunctionImpl);
        return JSFunction.create(realm, functionData);
    }

    private static JSFunctionData createToPrimitiveFunctionImpl(JSContext context) {
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
        return JSFunctionData.createCallOnly(context, callTarget, 1, "[Symbol.toPrimitive]");
    }

    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object name, Node encapsulatingNode) {
        Object propertyValue = super.getHelper(store, thisObj, name, encapsulatingNode);
        if (propertyValue != null) {
            return propertyValue;
        }
        if (name instanceof String) {
            return getJavaClassOrConstructorOrSubPackage(JSObject.getJSContext(store), store, (String) name);
        } else {
            return null;
        }
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject objectPrototype) {
        return JSObjectUtil.getProtoChildShape(objectPrototype, INSTANCE, context);
    }
}
