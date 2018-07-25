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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.interop.Converters.Converter;
import com.oracle.truffle.js.runtime.interop.Converters.ConverterFactory;
import com.oracle.truffle.js.runtime.interop.JavaMethod.AbstractJavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaMethod.IncompatibleArgumentsException;
import com.oracle.truffle.js.runtime.interop.JavaMethod.JavaMethodGetter;
import com.oracle.truffle.js.runtime.interop.JavaMethod.JavaMethodSetter;
import com.oracle.truffle.js.runtime.interop.JavaMethod.SingleJavaConstructor;
import com.oracle.truffle.js.runtime.java.adapter.JavaAdapterFactory;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * A mirror object for Java classes. Provides member access and introspection services.
 *
 * NB: Instances of this class should be persisted only in {@link ClassValue}s in order not to
 * prevent class unloading.
 */
public final class JavaClass {
    public static final String TYPE_NAME = "function";

    private static class JavaClassValue extends ClassValue<JavaClass> {
        @Override
        protected JavaClass computeValue(final Class<?> type) {
            return new JavaClass(type);
        }
    }

    private static final ClassValue<JavaClass> javaClassValue = new JavaClassValue();

    public static final boolean INSTANCE = false;
    public static final boolean STATIC = true;

    @SuppressWarnings({"unchecked"}) public static final Class<? extends JavaMember>[] METHOD = (Class<? extends JavaMember>[]) new Class<?>[]{JavaMethod.class};
    @SuppressWarnings({"unchecked"}) public static final Class<? extends JavaMember>[] GETTER_METHOD = (Class<? extends JavaMember>[]) new Class<?>[]{JavaGetter.class, JavaMethod.class};
    @SuppressWarnings({"unchecked"}) public static final Class<? extends JavaMember>[] METHOD_GETTER = (Class<? extends JavaMember>[]) new Class<?>[]{JavaMethod.class, JavaGetter.class};
    @SuppressWarnings({"unchecked"}) public static final Class<? extends JavaMember>[] SETTER = (Class<? extends JavaMember>[]) new Class<?>[]{JavaSetter.class};

    private final Class<?> type;
    private Map<String, JavaGetter> staticGetters;
    private Map<String, JavaSetter> staticSetters;
    private Map<String, JavaMethod> staticMethods;
    private Map<String, JavaMethod> instanceMethods;
    private Map<String, JavaGetter> instanceGetters;
    private Map<String, JavaSetter> instanceSetters;
    private Map<String, JavaClass> innerClasses;
    private List<AbstractJavaMethod> constructors;
    private Boolean isSamType;
    private JavaClass cachedExt;

    private JavaClass(Class<?> type) {
        this.type = type;
    }

    @TruffleBoundary
    public static JavaClass forClass(Class<?> clazz) {
        assert !JavaClass.class.isAssignableFrom(clazz);
        return javaClassValue.get(clazz);
    }

    @SuppressWarnings("hiding")
    @TruffleBoundary
    private synchronized void introspect() {
        CompilerAsserts.neverPartOfCompilation("do not introspect JavaClass from compiled code");
        Map<String, JavaGetter> staticGetters = new HashMap<>();
        Map<String, JavaSetter> staticSetters = new HashMap<>();
        Map<String, JavaMethod> instanceMethods = new HashMap<>();
        Map<String, JavaMethod> staticMethods = new HashMap<>();
        Map<String, JavaGetter> instanceGetters = new HashMap<>();
        Map<String, JavaSetter> instanceSetters = new HashMap<>();
        Map<String, JavaClass> innerClasses = new HashMap<>();
        List<AbstractJavaMethod> constructors = new ArrayList<>();

        for (Method method0 : type.getMethods()) {
            Method method = method0;
            /*
             * If the method that contains the class is not public, we need to find a public
             * superclass or interface that contains the method.
             */
            if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method = findParentMethod(type, method.getName(), method.getParameterTypes());
                if (method == null) {
                    continue;
                }
            }

            AbstractJavaMethod wrapped = JavaMethod.forMethod(method);
            Map<String, JavaMethod> methods = wrapped.isStatic() ? staticMethods : instanceMethods;
            JavaMethod existing = methods.put(method.getName(), wrapped);
            if (existing != null) {
                methods.put(method.getName(), existing.merge(wrapped));
            }
            if (isGetter(method) || isBooleanGetter(method)) {
                String getterName = decapitalize(method.getName().substring(isBooleanGetter(method) ? 2 : 3));
                Map<String, JavaGetter> getters = Modifier.isStatic(method.getModifiers()) ? staticGetters : instanceGetters;
                JavaGetter existingGetter = getters.put(getterName, new JavaMethodGetter(wrapped, getterName));
                if (existingGetter != null) {
                    getters.put(method.getName(), new JavaMethodGetter(((JavaMethodGetter) existingGetter).getUnderlyingMethod().merge(wrapped), getterName));
                }
            } else if (isSetter(method)) {
                String setterName = decapitalize(method.getName().substring(3));
                Map<String, JavaSetter> setters = Modifier.isStatic(method.getModifiers()) ? staticSetters : instanceSetters;
                JavaSetter existingSetter = setters.put(setterName, new JavaMethodSetter(wrapped, setterName));
                if (existingSetter != null) {
                    setters.put(method.getName(), new JavaMethodSetter(((JavaMethodSetter) existingSetter).getUnderlyingMethod().merge(wrapped), setterName));
                }
            }
        }
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                String fieldName = field.getName();
                if (!staticGetters.containsKey(fieldName)) {
                    staticGetters.put(fieldName, (JavaGetter) JavaMethod.forFieldGetter(field));
                }
                if (!Modifier.isFinal(field.getModifiers()) && !staticSetters.containsKey(fieldName)) {
                    staticSetters.put(fieldName, (JavaSetter) JavaMethod.forFieldSetter(field));
                }
            } else {
                if (!instanceGetters.containsKey(field.getName())) {
                    instanceGetters.put(field.getName(), (JavaGetter) JavaMethod.forFieldGetter(field));
                }
                if (!Modifier.isFinal(field.getModifiers()) && !instanceSetters.containsKey(field.getName())) {
                    instanceSetters.put(field.getName(), (JavaSetter) JavaMethod.forFieldSetter(field));
                }
            }
        }
        for (Class<?> innerClass : type.getClasses()) {
            String simpleName = innerClass.getSimpleName();
            if (!simpleName.isEmpty()) {
                innerClasses.put(simpleName, JavaClass.forClass(innerClass));
            }
        }
        for (Constructor<?> constructor : type.getConstructors()) {
            constructors.add(JavaMethod.forConstructor(constructor));
        }

        staticGetters.put("class", new JavaMethod.ClassFieldGetter(type));
        if (type.isArray()) {
            instanceGetters.put(JSAbstractArray.LENGTH, new JavaMethod.ArrayLengthFieldGetter());
        } else if (type == Class.class) {
            instanceGetters.put("static", new JavaMethod.StaticClassFieldGetter());
        } else if (List.class.isAssignableFrom(type)) {
            instanceGetters.put(JSAbstractArray.LENGTH, new JavaMethod.CollectionLengthFieldGetter());
        }

        // now set the fully populated maps
        this.staticGetters = staticGetters;
        this.staticSetters = staticSetters;
        this.instanceMethods = instanceMethods;
        this.staticMethods = staticMethods;
        this.instanceGetters = instanceGetters;
        this.instanceSetters = instanceSetters;
        this.innerClasses = innerClasses;
        this.constructors = constructors;
    }

    private Method findParentMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        Method other;
        try {
            other = clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            other = null;
        }
        if (other != null && Modifier.isPublic(other.getDeclaringClass().getModifiers())) {
            return other;
        } else {
            if (clazz.getSuperclass() != null) {
                other = findParentMethod(clazz.getSuperclass(), name, parameterTypes);
                if (other != null) {
                    return other;
                }
            }
            for (Class<?> intf : clazz.getInterfaces()) {
                other = findParentMethod(intf, name, parameterTypes);
                if (other != null) {
                    return other;
                }
            }
        }
        return null;
    }

    public Class<?> getType() {
        return type;
    }

    @TruffleBoundary
    public JavaMember getMember(String name, boolean staticModifier, Class<? extends JavaMember>[] memberTypes, boolean allowReflection) {
        for (Class<? extends JavaMember> memberType : memberTypes) {
            final Map<String, ? extends JavaMember> members;
            if (memberType == JavaMethod.class) {
                members = staticModifier == STATIC ? staticMethods() : instanceMethods();
            } else if (memberType == JavaGetter.class) {
                members = staticModifier == STATIC ? staticGetters() : instanceGetters();
            } else if (memberType == JavaSetter.class) {
                members = staticModifier == STATIC ? staticSetters() : instanceSetters();
            } else {
                throw new IllegalArgumentException();
            }
            JavaAccess.checkReflectionAccess(type, staticModifier == STATIC, allowReflection);
            JavaMember member = members.get(name);
            if (member != null) {
                assert memberType.isInstance(member) && (staticModifier == member.isStatic());
                return member;
            } else if (memberType == JavaMethod.class && name.endsWith(")")) {
                int openParen = name.indexOf('(');
                if (openParen >= 0) {
                    String namePart = name.substring(0, openParen);
                    String paramTypesPart = name.substring(openParen + 1, name.length() - 1);
                    if (!namePart.isEmpty()) {
                        member = members.get(namePart);
                        if (member != null) {
                            assert memberType.isInstance(member) && (staticModifier == member.isStatic());
                            return ((JavaMethod) member).lookupOverloadByParamTypeString(paramTypesPart);
                        }
                    } else {
                        // constructors().stream().map(a -> (JavaMethod) a).reduce((a, b) ->
                        // a.merge(b)).get().lookupOverloadByParamTypeString(paramTypesPart);
                        return JavaMethod.fromOverloads(constructors()).lookupOverloadByParamTypeString(paramTypesPart);
                    }
                }
            }
        }
        return null;
    }

    @TruffleBoundary
    public JavaClass getInnerClass(String name) {
        return innerClasses.get(name);
    }

    private Map<String, JavaGetter> staticGetters() {
        if (staticGetters == null) {
            introspect();
        }
        return staticGetters;
    }

    private Map<String, JavaSetter> staticSetters() {
        if (staticSetters == null) {
            introspect();
        }
        return staticSetters;
    }

    private Map<String, JavaMethod> instanceMethods() {
        if (instanceMethods == null) {
            introspect();
        }
        return instanceMethods;
    }

    private Map<String, JavaMethod> staticMethods() {
        if (staticMethods == null) {
            introspect();
        }
        return staticMethods;
    }

    private Map<String, JavaGetter> instanceGetters() {
        if (instanceGetters == null) {
            introspect();
        }
        return instanceGetters;
    }

    private Map<String, JavaSetter> instanceSetters() {
        if (instanceSetters == null) {
            introspect();
        }
        return instanceSetters;
    }

    private List<AbstractJavaMethod> constructors() {
        if (constructors == null) {
            introspect();
        }
        return constructors;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "[JavaClass " + type.getName() + "]";
    }

    @TruffleBoundary
    public Object newInstance(Object[] arguments) {
        if (!type.isArray()) {
            Pair<AbstractJavaMethod, Converter> bestConstructor = getBestConstructor(arguments);
            assert bestConstructor.getSecond().guard(arguments);
            Object[] convertedArguments = (Object[]) bestConstructor.getSecond().convert(arguments);
            try {
                return ((SingleJavaConstructor) bestConstructor.getFirst()).getReflectionConstructor().newInstance(convertedArguments);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                throw Errors.createError(e.toString());
            } catch (InvocationTargetException e) {
                // e.getCause().printStackTrace();
                throw UserScriptException.createJavaException(e.getCause());
            }
        } else {
            if (arguments.length != 1) {
                throw Errors.createTypeError("exactly 1 argument expected");
            }
            for (ConverterFactory converterFactory : Converters.DEFAULT_CONVERTER_FACTORIES) {
                if (converterFactory.accept(int.class, arguments[0])) {
                    return Array.newInstance(type.getComponentType(), (int) converterFactory.make(int.class).convert(arguments[0]));
                }
            }
            throw IncompatibleArgumentsException.INSTANCE;
        }
    }

    public Pair<AbstractJavaMethod, Converter> getBestConstructor(Object[] arguments) {
        return JavaMethod.selectBestMethod(constructors(), arguments);
    }

    public AbstractJavaMethod getBestConstructor(String paramTypeString) {
        AbstractJavaMethod ctor = JavaMethod.fromOverloads(constructors()).lookupOverloadByParamTypeString(paramTypeString);
        if (ctor == null) {
            throw Errors.createTypeErrorFormat("No such Java constructor: %s(%s)", type.getSimpleName(), paramTypeString);
        }
        return ctor;
    }

    private static boolean isBooleanGetter(Method method) {
        return method.getParameterTypes().length == 0 && method.getReturnType() == boolean.class && method.getName().startsWith("is") && method.getName().length() > 2;
    }

    private static boolean isGetter(Method method) {
        return method.getParameterTypes().length == 0 && method.getName().startsWith("get") && method.getName().length() > 3;
    }

    private static boolean isSetter(Method method) {
        return method.getParameterTypes().length == 1 && method.getName().startsWith("set") && method.getName().length() > 3;
    }

    /*
     * From Dynalink: jdk.internal.dynalink.beans.AbstractJavaLinker.
     */
    private static String decapitalize(String str) {
        assert str != null;
        if (str.isEmpty()) {
            return str;
        }

        final char c0 = str.charAt(0);
        if (Character.isLowerCase(c0)) {
            return str;
        }

        // If it has two consecutive upper-case characters, i.e. "URL", don't decapitalize
        if (str.length() > 1 && Character.isUpperCase(str.charAt(1))) {
            return str;
        }

        final char[] c = str.toCharArray();
        c[0] = Character.toLowerCase(c0);
        return new String(c);
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(type.getModifiers()) && !type.isArray();
    }

    public boolean isPublic() {
        return Modifier.isPublic(type.getModifiers());
    }

    public JavaClass extend(DynamicObject classOverrides) {
        return extend(classOverrides, null);
    }

    @TruffleBoundary
    public JavaClass extend(DynamicObject classOverrides, ClassLoader classLoader) {
        if (cachedExt != null && classOverrides == null) {
            return cachedExt;
        } else {
            JavaClass extended = JavaClass.forClass(JavaAdapterFactory.getAdapterClassFor(getType(), classOverrides, classLoader));
            if (classOverrides == null) {
                cachedExt = extended;
            }
            return extended;
        }
    }

    public boolean isSamType() {
        if (isSamType == null) {
            isSamType = isSamType(type);
        }
        return isSamType;
    }

    @TruffleBoundary
    private static boolean isSamType(Class<?> type) {
        return isAbstract(type) && (type.isInterface() || hasDefaultConstructor(type)) && hasSingleAbstractMethod(type);
    }

    public static boolean isAbstract(Class<?> type) {
        return Modifier.isAbstract(type.getModifiers()) && !type.isArray();
    }

    private static boolean hasDefaultConstructor(Class<?> type) {
        assert !type.isInterface();
        for (Constructor<?> ctor : type.getDeclaredConstructors()) {
            int modifiers = ctor.getModifiers();
            if ((Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) && ctor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSingleAbstractMethod(Class<?> type) {
        int abstractMethodCount = 0;
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                abstractMethodCount++;
            }
        }

        return abstractMethodCount == 1;
    }

    public JavaMethod getSuperMethod(String propertyName, boolean allowReflection) {
        JavaMethod method = (JavaMethod) getMember(JavaAdapterFactory.getSuperMethodName(propertyName), JavaClass.INSTANCE, JavaClass.METHOD, allowReflection);
        return method != null ? JavaMethod.toSuperMethod(method) : null;
    }
}
