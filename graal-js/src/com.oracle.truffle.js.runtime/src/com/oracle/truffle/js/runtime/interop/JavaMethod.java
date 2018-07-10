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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.interop.Converters.ArrayConverter;
import com.oracle.truffle.js.runtime.interop.Converters.Converter;
import com.oracle.truffle.js.runtime.interop.Converters.ConverterFactory;
import com.oracle.truffle.js.runtime.interop.Converters.ObjectArrayConverter;
import com.oracle.truffle.js.runtime.interop.Converters.VarArgsConverter;
import com.oracle.truffle.js.runtime.java.adapter.JavaSuperAdapter;
import com.oracle.truffle.js.runtime.util.IteratorUtil;
import com.oracle.truffle.js.runtime.util.Pair;

public abstract class JavaMethod implements JavaMember {

    public static final String TYPE_NAME = "function";
    private static final int ACCESS_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    public static AbstractJavaMethod forMethod(Method reflectionMethod) {
        return new SingleJavaMethod(reflectionMethod);
    }

    public static AbstractJavaMethod forConstructor(Constructor<?> reflectionConstructor) {
        return new SingleJavaConstructor(reflectionConstructor);
    }

    public static AbstractJavaMethod forFieldGetter(Field reflectionField) {
        return new JavaFieldGetter(reflectionField);
    }

    public static AbstractJavaMethod forFieldSetter(Field reflectionMethod) {
        return new JavaFieldSetter(reflectionMethod);
    }

    static JavaMethod toSuperMethod(JavaMethod actualMethod) {
        if (actualMethod instanceof AbstractJavaMethod) {
            return new SuperJavaMethod((AbstractJavaMethod) actualMethod);
        } else {
            return new OverloadedJavaMethod(Arrays.stream(actualMethod.overloads()).map((overload) -> new SuperJavaMethod(overload)).toArray(AbstractJavaMethod[]::new));
        }
    }

    @Override
    public final boolean isSynthetic() {
        return false;
    }

    @Override
    public final boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    final JavaMethod merge(JavaMethod other) {
        assert this.isStatic() == other.isStatic();
        assert this.getName().equals(other.getName());
        List<AbstractJavaMethod> merged = new ArrayList<>(Arrays.asList(this.overloads()));
        List<AbstractJavaMethod> append = new ArrayList<>(Arrays.asList(other.overloads()));
        append.removeAll(merged);
        merged.addAll(append);
        assert !merged.isEmpty();
        return fromOverloads(merged);
    }

    @TruffleBoundary
    static JavaMethod fromOverloads(List<AbstractJavaMethod> overloads) {
        if (overloads.size() > 1) {
            return new OverloadedJavaMethod(overloads.toArray(new AbstractJavaMethod[overloads.size()]));
        } else if (overloads.size() == 1) {
            return overloads.get(0);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected abstract AbstractJavaMethod[] overloads();

    public abstract MethodHandle getMethodHandle();

    public Pair<AbstractJavaMethod, Converter> getBestMethod(Object[] arguments) {
        return selectBestMethod(Arrays.asList(overloads()), arguments);
    }

    public ArrayList<Pair<AbstractJavaMethod, Converter>> getApplicableMethods(Object[] arguments) {
        return selectApplicableMethods(Arrays.asList(overloads()), arguments, Converters.DEFAULT_CONVERTER_FACTORIES);
    }

    @TruffleBoundary
    public final Object invoke(Object receiver, Object[] arguments) {
        Pair<AbstractJavaMethod, Converter> bestMethod = getBestMethod(arguments);

        AbstractJavaMethod actualMethod = bestMethod.getFirst();
        Object actualReceiver = receiver;
        if (actualMethod instanceof SuperJavaMethod) {
            actualMethod = ((SuperJavaMethod) actualMethod).getActualMethod();
            actualReceiver = ((JavaSuperAdapter) receiver).getAdapter();
        }

        Method reflectionMethod = ((SingleJavaMethod) actualMethod).getReflectionMethod();
        Converter converter = bestMethod.getSecond();
        assert converter.guard(arguments);
        Object[] convertedArguments = (Object[]) converter.convert(arguments);
        try {
            return Converters.JAVA_TO_JS_CONVERTER.convert(reflectionMethod.invoke(actualReceiver, convertedArguments));
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw Errors.createError(e.toString());
        } catch (InvocationTargetException e) {
            if (DEBUG) {
                e.getCause().printStackTrace();
            }
            throw UserScriptException.createJavaException(e.getCause());
        }
    }

    static <T extends Member> Pair<T, Converter> selectBestMethod(Collection<T> availableMethods, Object[] arguments) {
        // first try without conversions
        ArrayList<Pair<T, Converter>> possibleMethods = selectApplicableMethods(availableMethods, arguments, Converters.COERCIONLESS_CONVERTER_FACTORIES);
        if (possibleMethods.size() == 1) {
            return possibleMethods.get(0);
        } else if (possibleMethods.isEmpty()) {
            // no exact matches, now try with conversions
            possibleMethods = selectApplicableMethods(availableMethods, arguments, Converters.DEFAULT_CONVERTER_FACTORIES);

            if (possibleMethods.size() == 1) {
                return possibleMethods.get(0);
            } else if (possibleMethods.isEmpty()) {
                // no exact matches, now try with conversions
                possibleMethods = selectApplicableMethods(availableMethods, arguments, Converters.FORCING_CONVERTER_FACTORIES);

                if (possibleMethods.isEmpty()) {
                    throw Errors.createTypeError("none of the " + availableMethods.size() + " methods are compatible with the argument list:\n" + join("\n", methodsToSignatures(availableMethods)));
                } else if (possibleMethods.size() == 1) {
                    return possibleMethods.get(0);
                }
            }
        }
        assert possibleMethods.size() > 1;
        // multiple compatible methods found, try to select a best match

        // TODO decision criteria:
        // select between primitive and boxed (prefer primitive)
        // select between compatible (sub)types
        // select between varargs and non-varargs (prefer non-varargs)

        boolean allNonVarArgs = true;
        for (Pair<T, Converter> member : possibleMethods) {
            if (isVarArgs(member.getFirst())) {
                allNonVarArgs = false;
                break;
            }
        }

        if (allNonVarArgs) {
            possibleMethods = selectBestMethodAllNonVarArgs(arguments, possibleMethods);
        } else {
            selectBestNotAllNonVarargs(possibleMethods);
        }

        if (possibleMethods.size() == 1) {
            return possibleMethods.get(0);
        }

        Pair<T, Converter> singleNonBridge = findExactlyOneMatch(possibleMethods, p -> !isBridge(p.getFirst()));
        if (singleNonBridge != null) {
            return singleNonBridge;
        }

        throw Errors.createError("cannot select between " + possibleMethods.size() + " methods compatible with the argument list:\n" +
                        join("\n", methodsToSignatures(IteratorUtil.convertIterable(possibleMethods, Pair::getFirst))));
    }

    private static <T extends Member> ArrayList<Pair<T, Converter>> selectBestMethodAllNonVarArgs(Object[] arguments, ArrayList<Pair<T, Converter>> possibleMethodsParam) {
        ArrayList<Pair<T, Converter>> possibleMethods = possibleMethodsParam;
        Class<?>[][] parameterTypeMatrix = new Class<?>[possibleMethods.size()][];
        for (int i = 0; i < possibleMethods.size(); i++) {
            parameterTypeMatrix[i] = getParameterTypes(possibleMethods.get(i).getFirst());
        }
        boolean eliminatedAny = false;
        k: for (int k = 0; k < possibleMethods.size(); k++) {
            for (int j = 0; j < possibleMethods.size(); j++) {
                if (j == k) {
                    continue;
                }
                if (possibleMethods.get(j) == null) {
                    continue;
                } else if (possibleMethods.get(k) == null) {
                    continue k;
                }

                boolean allAssignable = true;
                for (int i = 0; i < arguments.length; i++) {
                    if (!isAssignableFrom(parameterTypeMatrix[j][i], parameterTypeMatrix[k][i])) {
                        allAssignable = false;
                        break;
                    }
                }
                if (allAssignable) {
                    // j more generic than k
                    if (DEBUG) {
                        System.out.println(possibleMethods.get(j) + " > " + possibleMethods.get(k));
                    }
                    possibleMethods.set(j, null);
                    eliminatedAny = true;
                }
            }
        }
        if (eliminatedAny) {
            ArrayList<Pair<T, Converter>> temp = new ArrayList<>();
            for (Pair<T, Converter> member : possibleMethods) {
                if (member != null) {
                    temp.add(member);
                }
            }
            possibleMethods = temp;
        }
        return possibleMethods;
    }

    private static <T extends Member> void selectBestNotAllNonVarargs(ArrayList<Pair<T, Converter>> possibleMethods) {
        boolean allVarArgs = true;
        for (Pair<T, Converter> member : possibleMethods) {
            if (!isVarArgs(member.getFirst())) {
                allVarArgs = false;
                break;
            }
        }
        if (!allVarArgs) {
            // if we have at least one non-varargs, remove all varargs
            for (Iterator<Pair<T, Converter>> iterator = possibleMethods.iterator(); iterator.hasNext();) {
                T member = iterator.next().getFirst();
                if (isVarArgs(member)) {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean isAssignableFrom(Class<?> left, Class<?> right) {
        return (left.isAssignableFrom(right)) || (right.isPrimitive() && left.isAssignableFrom(getBoxedType(right)));
    }

    static <T extends Member> ArrayList<Pair<T, Converter>> selectApplicableMethods(Collection<T> availableMethods, Object[] arguments, ConverterFactory[] availableConverters) {
        ArrayList<Pair<T, Converter>> possibleMethods = new ArrayList<>();
        next: for (T method : availableMethods) {
            Class<?>[] parameterTypes = getParameterTypes(method);
            if (parameterTypes.length != arguments.length && !isVarArgs(method)) {
                continue next;
            }
            Converter converter;
            if (!isVarArgs(method)) {
                assert parameterTypes.length == arguments.length;
                Converter[] converters = new Converter[parameterTypes.length];
                for (int i = 0; i < arguments.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    Object argument = arguments[i];
                    if ((converters[i] = findConverter(parameterType, argument, availableConverters)) == null) {
                        continue next;
                    }
                }
                converter = new ObjectArrayConverter(converters);
            } else {
                if (parameterTypes.length - 1 > arguments.length) {
                    // not enough arguments to satisfy signature
                    continue next;
                }

                Converter[] converters = new Converter[parameterTypes.length];
                // compare non-varargs parameters first
                for (int i = 0; i < parameterTypes.length - 1; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    Object argument = arguments[i];
                    if ((converters[i] = findConverter(parameterType, argument, availableConverters)) == null) {
                        continue next;
                    }
                }

                int varArgsParameterIndex = parameterTypes.length - 1;
                assert parameterTypes[varArgsParameterIndex].isArray();

                if (arguments.length == varArgsParameterIndex + 1 && parameterTypes[varArgsParameterIndex].isAssignableFrom(classOf(arguments[varArgsParameterIndex]))) {
                    // number of args == number of params, last parameter is a compatible array
                    // => varArgs passed as array directly
                    converters[varArgsParameterIndex] = Converters.IDENTITY_CONVERTER_FACTORY.make(parameterTypes[varArgsParameterIndex]);
                    converter = new ArrayConverter(Object.class, converters);
                } else {
                    Class<?> varArgsComponentType = parameterTypes[varArgsParameterIndex].getComponentType();
                    Converter[] varArgsConverters = new Converter[arguments.length - varArgsParameterIndex];
                    converters[varArgsParameterIndex] = new ArrayConverter(varArgsComponentType, varArgsConverters);
                    for (int i = varArgsParameterIndex; i < arguments.length; i++) {
                        Object argument = arguments[i];
                        if ((varArgsConverters[i - varArgsParameterIndex] = findConverter(varArgsComponentType, argument, availableConverters)) == null) {
                            continue next;
                        }
                    }
                    converter = new VarArgsConverter(Object.class, converters);
                }
            }
            possibleMethods.add(new Pair<>(method, converter));
        }
        return possibleMethods;
    }

    private static Converter findConverter(Class<?> parameterType, Object argument, ConverterFactory[] availableConverters) {
        for (ConverterFactory converterFactory : availableConverters) {
            if (converterFactory.accept(parameterType, argument)) {
                return converterFactory.make(parameterType);
            }
        }
        return null;
    }

    private static Class<?>[] getParameterTypes(Member method) {
        if (method instanceof AbstractJavaMethod) {
            return ((AbstractJavaMethod) method).getParameterTypes();
        }
        return ((Executable) method).getParameterTypes();
    }

    private static Class<?> getReturnType(Member method) {
        if (method instanceof AbstractJavaMethod) {
            return ((AbstractJavaMethod) method).getReturnType();
        }
        return method instanceof Method ? ((Method) method).getReturnType() : ((Constructor<?>) method).getDeclaringClass();
    }

    private static boolean isVarArgs(Member method) {
        if (method instanceof AbstractJavaMethod) {
            return ((AbstractJavaMethod) method).isVarArgs();
        }
        return ((Executable) method).isVarArgs();
    }

    private static boolean isDefault(Member method) {
        if (method instanceof SingleJavaMethod) {
            return ((SingleJavaMethod) method).isDefault();
        } else if (method instanceof Method) {
            return ((Method) method).isDefault();
        } else {
            return false;
        }
    }

    private static boolean isBridge(Member method) {
        if (method instanceof SingleJavaMethod) {
            return ((SingleJavaMethod) method).isBridge();
        } else if (method instanceof Method) {
            return ((Method) method).isBridge();
        } else {
            return false;
        }
    }

    private static Class<?> classOf(Object obj) {
        return obj != null ? obj.getClass() : null;
    }

    private static String join(String delimiter, Iterable<?> collection) {
        Iterator<?> iterator = collection.iterator();
        if (!iterator.hasNext()) {
            return "";
        }
        StringBuilder joined = new StringBuilder(String.valueOf(iterator.next()));
        while (iterator.hasNext()) {
            joined.append(delimiter).append(iterator.next());
        }
        return joined.toString();
    }

    private static String methodToSignature(Member method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getClass().getSimpleName());
        sb.append(" ");

        int methodModifiers = method.getModifiers() & Modifier.methodModifiers();
        if (methodModifiers != 0 && !isDefault(method)) {
            sb.append(Modifier.toString(methodModifiers)).append(' ');
        } else {
            int accessMod = methodModifiers & ACCESS_MODIFIERS;
            if (accessMod != 0) {
                sb.append(Modifier.toString(accessMod)).append(' ');
            }
            if (isDefault(method)) {
                sb.append("default").append(' ');
            }
            int otherMod = methodModifiers & ~ACCESS_MODIFIERS;
            if (otherMod != 0) {
                sb.append(Modifier.toString(otherMod)).append(' ');
            }
        }

        sb.append(method.getDeclaringClass().getName());
        sb.append(".");
        sb.append(method.getName());
        sb.append(MethodType.methodType(getReturnType(method), getParameterTypes(method)));
        return sb.toString();
    }

    private static <T extends Member> Iterable<String> methodsToSignatures(Iterable<T> methods) {
        return IteratorUtil.convertIterable(methods, JavaMethod::methodToSignature);
    }

    public static Class<?> getBoxedType(Class<?> clazz) {
        assert clazz.isPrimitive();
        if (clazz == boolean.class) {
            return Boolean.class;
        } else if (clazz == byte.class) {
            return Byte.class;
        } else if (clazz == short.class) {
            return Short.class;
        } else if (clazz == char.class) {
            return Character.class;
        } else if (clazz == int.class) {
            return Integer.class;
        } else if (clazz == long.class) {
            return Long.class;
        } else if (clazz == float.class) {
            return Float.class;
        } else if (clazz == double.class) {
            return Double.class;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static <T> T findExactlyOneMatch(Iterable<T> iterable, Predicate<T> predicate) {
        T singleMatch = null;
        for (T item : iterable) {
            if (predicate.test(item)) {
                if (singleMatch == null) {
                    singleMatch = item;
                } else {
                    // more than one match found
                    singleMatch = null;
                    break;
                }
            }
        }
        return singleMatch;
    }

    @TruffleBoundary
    public AbstractJavaMethod lookupOverloadByParamTypeString(String paramTypeString) {
        StringTokenizer tokenizer = new StringTokenizer(paramTypeString, ", ");
        int count = tokenizer.countTokens();
        String[] paramTypes = new String[count];
        for (int i = 0; i < count; i++) {
            paramTypes[i] = tokenizer.nextToken();
        }

        List<AbstractJavaMethod> found = new ArrayList<>();
        for (AbstractJavaMethod overload : overloads()) {
            if (overload.getParameterTypes().length == paramTypes.length && typeNamesMatchParameterTypes(paramTypes, overload.getParameterTypes())) {
                found.add(overload);
            }
        }

        if (found.isEmpty()) {
            return null;
        } else if (found.size() == 1) {
            return found.get(0);
        } else {
            AbstractJavaMethod singleNonBridge = findExactlyOneMatch(found, m -> !isBridge(m));
            if (singleNonBridge != null) {
                return singleNonBridge;
            }

            throw Errors.createError("Can't choose among " + join("\n", methodsToSignatures(found)) + " for argument types " + paramTypeString + " for method " + getName());
        }
    }

    private static boolean typeNamesMatchParameterTypes(String[] typesNames, Class<?>[] paramTypes) {
        for (int i = 0; i < typesNames.length; i++) {
            if (!typeNameMatchesParameterType(typesNames[i], paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean typeNameMatchesParameterType(String typeName, Class<?> paramType) {
        return typeName.equals(typeName.indexOf('.') < 0 ? paramType.getSimpleName() : paramType.getCanonicalName());
    }

    public final boolean isConstructor() {
        return overloads()[0] instanceof SingleJavaConstructor;
    }

    @SuppressWarnings("serial")
    public static final class IncompatibleArgumentsException extends ControlFlowException {
        protected static final IncompatibleArgumentsException INSTANCE = new IncompatibleArgumentsException();

        private IncompatibleArgumentsException() {
        }
    }

    static final boolean DEBUG = false;

    public abstract static class AbstractJavaMethod extends JavaMethod {
        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaMethod " + getName() + "]";
        }

        @Override
        protected final AbstractJavaMethod[] overloads() {
            return new AbstractJavaMethod[]{this};
        }

        public abstract boolean isVarArgs();

        public abstract Class<?>[] getParameterTypes();

        public abstract Class<?> getReturnType();
    }

    public static final class SingleJavaMethod extends AbstractJavaMethod {
        private final Method reflectionMethod;

        private SingleJavaMethod(Method reflectionMethod) {
            this.reflectionMethod = reflectionMethod;
        }

        public Method getReflectionMethod() {
            return reflectionMethod;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return reflectionMethod.getParameterTypes();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return reflectionMethod.getDeclaringClass();
        }

        @Override
        public String getName() {
            return reflectionMethod.getName();
        }

        @Override
        public int getModifiers() {
            return reflectionMethod.getModifiers();
        }

        @Override
        public boolean isVarArgs() {
            return reflectionMethod.isVarArgs();
        }

        @Override
        public Class<?> getReturnType() {
            return reflectionMethod.getReturnType();
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().unreflect(reflectionMethod);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SingleJavaMethod && ((SingleJavaMethod) other).reflectionMethod.equals(this.reflectionMethod);
        }

        @Override
        public int hashCode() {
            return reflectionMethod.hashCode();
        }

        public boolean isDefault() {
            return reflectionMethod.isDefault();
        }

        public boolean isBridge() {
            return reflectionMethod.isBridge();
        }
    }

    public static final class SingleJavaConstructor extends AbstractJavaMethod {
        private final Constructor<?> reflectionConstructor;

        private SingleJavaConstructor(Constructor<?> reflectionConstructor) {
            this.reflectionConstructor = reflectionConstructor;
        }

        public Constructor<?> getReflectionConstructor() {
            return reflectionConstructor;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return reflectionConstructor.getParameterTypes();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return reflectionConstructor.getDeclaringClass();
        }

        @Override
        public String getName() {
            return reflectionConstructor.getName();
        }

        @Override
        public int getModifiers() {
            return reflectionConstructor.getModifiers();
        }

        @Override
        public boolean isVarArgs() {
            return reflectionConstructor.isVarArgs();
        }

        @Override
        public Class<?> getReturnType() {
            return getDeclaringClass();
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().unreflectConstructor(reflectionConstructor);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SingleJavaConstructor && ((SingleJavaConstructor) other).reflectionConstructor.equals(this.reflectionConstructor);
        }

        @Override
        public int hashCode() {
            return reflectionConstructor.hashCode();
        }
    }

    private abstract static class JavaFieldAccessor extends AbstractJavaMethod {
        protected final Field reflectionField;

        private JavaFieldAccessor(Field reflectionField) {
            this.reflectionField = reflectionField;
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaField " + getName() + "]";
        }

        @Override
        public Class<?> getDeclaringClass() {
            return reflectionField.getDeclaringClass();
        }

        @Override
        public String getName() {
            return reflectionField.getName();
        }

        @Override
        public int getModifiers() {
            return reflectionField.getModifiers();
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }
    }

    public static final class JavaFieldGetter extends JavaFieldAccessor implements JavaGetter {

        private JavaFieldGetter(Field reflectionField) {
            super(reflectionField);
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{};
        }

        @Override
        public Class<?> getReturnType() {
            return reflectionField.getType();
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().unreflectGetter(reflectionField);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @TruffleBoundary
        @Override
        public Object getValue(Object obj) {
            assert (isStatic() && obj == null) || (!isStatic() && obj != null);
            try {
                return reflectionField.get(obj);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Errors.createError(e.toString());
            }
        }
    }

    public static final class JavaFieldSetter extends JavaFieldAccessor implements JavaSetter {

        private JavaFieldSetter(Field reflectionField) {
            super(reflectionField);
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{reflectionField.getType()};
        }

        @Override
        public Class<?> getReturnType() {
            return void.class;
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().unreflectSetter(reflectionField);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @TruffleBoundary
        @Override
        public void setValue(Object obj, Object value) {
            assert (isStatic() && obj == null) || (!isStatic() && obj != null);
            try {
                reflectionField.set(obj, value);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Errors.createError(e.toString());
            }
        }
    }

    public static final class OverloadedJavaMethod extends JavaMethod {
        private final AbstractJavaMethod[] overloads;

        private OverloadedJavaMethod(AbstractJavaMethod[] overloads) {
            assert overloads.length > 0;
            this.overloads = overloads;
        }

        @Override
        @TruffleBoundary
        public String toString() {
            String overloadsSuffix = overloads.length == 1 ? "" : " (" + overloads.length + " overloads)";
            return "[JavaMethod " + getName() + overloadsSuffix + "]";
        }

        @Override
        protected AbstractJavaMethod[] overloads() {
            return overloads;
        }

        protected AbstractJavaMethod first() {
            return overloads[0];
        }

        @Override
        public Class<?> getDeclaringClass() {
            return first().getDeclaringClass();
        }

        @Override
        public String getName() {
            return first().getName();
        }

        @Override
        public int getModifiers() {
            return first().getModifiers();
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().findVirtual(JavaMethod.class, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class)).bindTo(this);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static final class ArrayLengthFieldGetter extends AbstractJavaMethod implements JavaGetter {
        public ArrayLengthFieldGetter() {
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaField " + getName() + "]";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{};
        }

        @Override
        public Class<?> getDeclaringClass() {
            return null;
        }

        @Override
        public String getName() {
            return JSAbstractArray.LENGTH;
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return int.class;
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().findStatic(Array.class, "getLength", MethodType.methodType(int.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Object getValue(Object obj) {
            return Array.getLength(obj);
        }
    }

    public static final class ClassFieldGetter extends AbstractJavaMethod implements JavaGetter {
        private final Class<?> declaringClass;

        public ClassFieldGetter(Class<?> declaringClass) {
            this.declaringClass = declaringClass;
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaField " + getName() + "]";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{};
        }

        @Override
        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        @Override
        public String getName() {
            return "class";
        }

        @Override
        public int getModifiers() {
            return Modifier.STATIC;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return Class.class;
        }

        @Override
        public MethodHandle getMethodHandle() {
            return MethodHandles.constant(Class.class, declaringClass);
        }

        @Override
        public Object getValue(Object obj) {
            assert obj == null;
            return declaringClass;
        }
    }

    public static final class StaticClassFieldGetter extends AbstractJavaMethod implements JavaGetter {
        public StaticClassFieldGetter() {
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaField " + getName() + "]";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{};
        }

        @Override
        public Class<?> getDeclaringClass() {
            return null;
        }

        @Override
        public String getName() {
            return "static";
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return Class.class;
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().findStatic(JavaClass.class, "forClass", MethodType.methodType(JavaClass.class, Class.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Object getValue(Object obj) {
            return JavaClass.forClass((Class<?>) obj);
        }
    }

    public static final class CollectionLengthFieldGetter extends AbstractJavaMethod implements JavaGetter {
        public CollectionLengthFieldGetter() {
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return "[JavaField " + getName() + "]";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[]{};
        }

        @Override
        public Class<?> getDeclaringClass() {
            return null;
        }

        @Override
        public String getName() {
            return JSAbstractArray.LENGTH;
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return int.class;
        }

        @Override
        public MethodHandle getMethodHandle() {
            try {
                return MethodHandles.lookup().findVirtual(Collection.class, "size", MethodType.methodType(int.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @TruffleBoundary
        @Override
        public Object getValue(Object obj) {
            return ((Collection<?>) obj).size();
        }
    }

    public abstract static class JavaMethodAccessor extends JavaMethod {
        protected final JavaMethod method;
        private final String name;

        public JavaMethodAccessor(JavaMethod method, String name) {
            this.method = method;
            this.name = name;
        }

        @Override
        public final Class<?> getDeclaringClass() {
            return method.getDeclaringClass();
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        public final JavaMethod getUnderlyingMethod() {
            return method;
        }

        @Override
        protected final AbstractJavaMethod[] overloads() {
            return method.overloads();
        }

        @Override
        public MethodHandle getMethodHandle() {
            return method.getMethodHandle();
        }
    }

    public static final class JavaMethodGetter extends JavaMethodAccessor implements JavaGetter {
        public JavaMethodGetter(JavaMethod method, String name) {
            super(method, name);
        }

        @TruffleBoundary
        @Override
        public Object getValue(Object obj) {
            try {
                return method.invoke(obj, ScriptArray.EMPTY_OBJECT_ARRAY);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class JavaMethodSetter extends JavaMethodAccessor implements JavaSetter {
        public JavaMethodSetter(JavaMethod method, String name) {
            super(method, name);
        }

        @TruffleBoundary
        @Override
        public void setValue(Object obj, Object value) {
            try {
                method.invoke(obj, new Object[]{value});
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class SuperJavaMethod extends AbstractJavaMethod {
        private final AbstractJavaMethod actualMethod;

        private SuperJavaMethod(AbstractJavaMethod actualMethod) {
            this.actualMethod = actualMethod;
            assert !(actualMethod instanceof SuperJavaMethod);
            assert actualMethod instanceof SingleJavaMethod;
            assert !Modifier.isFinal(actualMethod.getModifiers());
        }

        public AbstractJavaMethod getActualMethod() {
            return actualMethod;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return actualMethod.getParameterTypes();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return actualMethod.getDeclaringClass();
        }

        @Override
        public String getName() {
            return actualMethod.getName();
        }

        @Override
        public int getModifiers() {
            return actualMethod.getModifiers();
        }

        @Override
        public boolean isVarArgs() {
            return actualMethod.isVarArgs();
        }

        @Override
        public Class<?> getReturnType() {
            return actualMethod.getReturnType();
        }

        @Override
        public MethodHandle getMethodHandle() {
            MethodHandle actualMethodHandle = actualMethod.getMethodHandle();
            return MethodHandles.filterArguments(actualMethod.getMethodHandle(), 0, UNADAPT_RECEIVER.asType(UNADAPT_RECEIVER.type().changeReturnType(actualMethodHandle.type().parameterType(0))));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SuperJavaMethod && ((SuperJavaMethod) other).actualMethod.equals(this.actualMethod);
        }

        @Override
        public int hashCode() {
            return actualMethod.hashCode();
        }

        @SuppressWarnings("unused")
        private static Object unadaptReceiver(Object receiver) {
            return ((JavaSuperAdapter) receiver).getAdapter();
        }

        private static final MethodHandle UNADAPT_RECEIVER;
        static {
            try {
                UNADAPT_RECEIVER = MethodHandles.lookup().findStatic(SuperJavaMethod.class, "unadaptReceiver", MethodType.methodType(Object.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
