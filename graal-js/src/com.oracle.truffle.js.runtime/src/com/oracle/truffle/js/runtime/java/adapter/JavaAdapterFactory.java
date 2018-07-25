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
package com.oracle.truffle.js.runtime.java.adapter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Provides static utility services to generated Java adapter classes.
 */
public final class JavaAdapterFactory {
    private static final Class<?> INTERNAL_CLASS = JavaAdapterServices.class;

    @TruffleBoundary
    public static Class<?> getAdapterClassFor(Class<?>[] types, DynamicObject classOverrides) {
        return getAdapterClassFor(types, classOverrides, null);
    }

    @TruffleBoundary
    public static Class<?> getAdapterClassFor(Class<?>[] types, DynamicObject classOverrides, ClassLoader classLoader) {
        assert types.length > 0;
        assert classOverrides == null || JSRuntime.isObject(classOverrides);

        if (types.length == 1) {
            return getAdapterClassFor(types[0], classOverrides, classLoader);
        }

        Class<?> superClass = null;
        final List<Class<?>> interfaces = new ArrayList<>();
        for (final Class<?> t : types) {
            final int mod = t.getModifiers();
            if (!t.isInterface()) {
                if (superClass != null) {
                    throwCannotExtendMultipleClassesError(superClass, t);
                } else if (Modifier.isFinal(mod)) {
                    throw Errors.createTypeErrorFormat("Can not extend final class %s.", t.getCanonicalName());
                } else {
                    superClass = t;
                }
            } else {
                if (interfaces.size() >= 65535) {
                    throw new IllegalArgumentException("interface limit exceeded");
                }

                interfaces.add(t);
            }

            if (!Modifier.isPublic(mod)) {
                throw Errors.createTypeErrorFormat("Class not public: %s.", t.getCanonicalName());
            }
        }
        superClass = superClass != null ? superClass : Object.class;

        ClassLoader commonLoader = classLoader != null ? classLoader : getCommonClassLoader(types);
        return getAdapterClassForCommon(superClass, interfaces, classOverrides, commonLoader);
    }

    @TruffleBoundary
    public static Class<?> getAdapterClassFor(Class<?> type) {
        return getAdapterClassFor(type, null, null);
    }

    public static Class<?> getAdapterClassFor(Class<?> type, DynamicObject classOverrides, ClassLoader classLoader) {
        boolean isInterface = Modifier.isInterface(type.getModifiers());
        Class<?> superClass = !isInterface ? type : Object.class;
        List<Class<?>> interfaces = !isInterface ? Collections.<Class<?>> emptyList() : Collections.<Class<?>> singletonList(type);

        ClassLoader commonLoader = classLoader != null ? classLoader : getClassLoaderWithAccessTo(type);
        return getAdapterClassForCommon(superClass, interfaces, classOverrides, commonLoader);
    }

    private static Class<?> getAdapterClassForCommon(Class<?> superClass, List<Class<?>> interfaces, DynamicObject classOverrides, ClassLoader commonLoader) {
        boolean classOverride = classOverrides != null && JSRuntime.isObject(classOverrides);
        JavaAdapterBytecodeGenerator bytecodeGenerator = new JavaAdapterBytecodeGenerator(superClass, interfaces, commonLoader, classOverride);
        JavaAdapterClassLoader generatedClassLoader = bytecodeGenerator.createAdapterClassLoader();

        JavaAdapterServices.setClassOverrides(Context.getCurrent().asValue(classOverrides));
        try {
            Class<?> generatedClass = generatedClassLoader.generateClass(commonLoader);
            return generatedClass;
        } finally {
            JavaAdapterServices.setClassOverrides(null);
        }
    }

    @TruffleBoundary
    private static void throwCannotExtendMultipleClassesError(Class<?> superClass, Class<?> t) {
        throw Errors.createTypeErrorFormat("Can not extend multiple classes %s and %s. At most one of the specified types can be a class, the rest must all be interfaces.", t.getCanonicalName(),
                        superClass.getCanonicalName());
    }

    public static Object getSuperAdapter(Object adapter) {
        if (adapter instanceof JavaSuperAdapter) {
            throw new IllegalArgumentException();
        }
        return new JavaSuperAdapter(adapter);
    }

    @TruffleBoundary
    public static String getSuperMethodName(String methodName) {
        assert !methodName.startsWith(JavaAdapterBytecodeGenerator.SUPER_PREFIX);
        return JavaAdapterBytecodeGenerator.SUPER_PREFIX + methodName;
    }

    private static boolean classLoaderCanSee(ClassLoader loader, Class<?> clazz) {
        if (clazz.getClassLoader() == loader) {
            return true;
        }
        try {
            return Class.forName(clazz.getName(), false, loader) == clazz;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean classLoaderCanSee(ClassLoader loader, Class<?>[] classes) {
        for (Class<?> c : classes) {
            if (!classLoaderCanSee(loader, c)) {
                return false;
            }
        }
        return true;
    }

    private static ClassLoader getClassLoaderWithAccessTo(Class<?> superType) {
        if (classLoaderCanSee(superType.getClassLoader(), INTERNAL_CLASS)) {
            return superType.getClassLoader();
        } else if (classLoaderCanSee(INTERNAL_CLASS.getClassLoader(), superType)) {
            return INTERNAL_CLASS.getClassLoader();
        } else {
            throw Errors.createTypeErrorFormat("Could not determine a class loader with access to the JS engine and %s", superType);
        }
    }

    private static ClassLoader getCommonClassLoader(Class<?>[] types) {
        Map<ClassLoader, Boolean> distinctLoaders = new HashMap<>();
        for (Class<?> type : types) {
            ClassLoader loader = type.getClassLoader();
            if (distinctLoaders.computeIfAbsent(loader, cl -> classLoaderCanSee(cl, types))) {
                return getClassLoaderWithAccessTo(type);
            }
        }
        throw Errors.createTypeErrorFormat("Could not determine a class loader that can see all types: %s", Arrays.toString(types));
    }
}
