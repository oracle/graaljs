/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static JavaClass getAdapterClassFor(Class<?>[] types, DynamicObject classOverrides) {
        return getAdapterClassFor(types, classOverrides, null);
    }

    @TruffleBoundary
    public static JavaClass getAdapterClassFor(Class<?>[] types, DynamicObject classOverrides, ClassLoader classLoader) {
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
                    throw Errors.createTypeError("Can not extend final class %s.", t.getCanonicalName());
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
                throw Errors.createTypeError("Class not public: %s.", t.getCanonicalName());
            }
        }
        superClass = superClass != null ? superClass : Object.class;

        ClassLoader commonLoader = classLoader != null ? classLoader : getCommonClassLoader(types);
        return getAdapterClassForCommon(superClass, interfaces, classOverrides, commonLoader);
    }

    @TruffleBoundary
    public static JavaClass getAdapterClassFor(Class<?> type, DynamicObject classOverrides, ClassLoader classLoader) {
        boolean isInterface = Modifier.isInterface(type.getModifiers());
        Class<?> superClass = !isInterface ? type : Object.class;
        List<Class<?>> interfaces = !isInterface ? Collections.<Class<?>> emptyList() : Collections.<Class<?>> singletonList(type);

        ClassLoader commonLoader = classLoader != null ? classLoader : getClassLoaderWithAccessTo(type);
        return getAdapterClassForCommon(superClass, interfaces, classOverrides, commonLoader);
    }

    private static JavaClass getAdapterClassForCommon(Class<?> superClass, List<Class<?>> interfaces, DynamicObject classOverrides, ClassLoader commonLoader) {
        boolean classOverride = classOverrides != null && JSRuntime.isObject(classOverrides);
        JavaAdapterBytecodeGenerator bytecodeGenerator = new JavaAdapterBytecodeGenerator(superClass, interfaces, commonLoader, classOverride);
        JavaAdapterClassLoader generatedClassLoader = bytecodeGenerator.createAdapterClassLoader();

        JavaAdapterServices.setClassOverrides(classOverrides);
        Class<?> generatedClass = generatedClassLoader.generateClass(commonLoader);
        JavaAdapterServices.setClassOverrides(null);

        return JavaClass.forClass(generatedClass);
    }

    @TruffleBoundary
    private static void throwCannotExtendMultipleClassesError(Class<?> superClass, Class<?> t) {
        throw Errors.createTypeError("Can not extend multiple classes %s and %s. At most one of the specified types can be a class, the rest must all be interfaces.", t.getCanonicalName(),
                        superClass.getCanonicalName());
    }

    public static Object getSuperAdapter(Object adapter) {
        if (adapter instanceof JavaSuperAdapter) {
            throw new IllegalArgumentException();
        }
        return new JavaSuperAdapter(adapter);
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
            throw Errors.createTypeError("Could not determine a class loader with access to the JS engine and %s", superType);
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
        throw Errors.createTypeError("Could not determine a class loader that can see all types: %s", Arrays.toString(types));
    }
}
