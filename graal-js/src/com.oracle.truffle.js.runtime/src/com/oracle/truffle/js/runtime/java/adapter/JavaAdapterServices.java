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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Provides static utility services to generated Java adapter classes.
 */
public final class JavaAdapterServices {
    private static final MethodHandle VALUE_EXECUTE_METHOD_HANDLE;
    private static final MethodHandle VALUE_EXECUTE_VOID_METHOD_HANDLE;
    private static final MethodHandle VALUE_AS_METHOD_HANDLE;
    private static final ThreadLocal<Value> classOverrides = new ThreadLocal<>();

    static {
        try {
            VALUE_EXECUTE_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "execute", MethodType.methodType(Value.class, Object[].class));
            VALUE_EXECUTE_VOID_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "executeVoid", MethodType.methodType(void.class, Object[].class));
            VALUE_AS_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "as", MethodType.methodType(Object.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaAdapterServices() {
    }

    /**
     * Returns a thread-local JS object used to define methods for the adapter class being
     * initialized on the current thread. This method is public solely for implementation reasons,
     * so the adapter classes can invoke it from their static initializers.
     *
     * @return the thread-local JS object used to define methods for the class being initialized.
     */
    public static Value getClassOverrides() {
        final Value overrides = classOverrides.get();
        assert overrides != null;
        return overrides;
    }

    static void setClassOverrides(Value overrides) {
        classOverrides.set(overrides);
    }

    /**
     * Given a JS object, retrieves a function from it by name, bound to the object.
     *
     * This method is public mainly for implementation reasons, so the adapter classes can invoke it
     * from their constructors that take a {@link Value} as last argument to obtain the functions
     * for their method implementations.
     *
     * @param obj the script object
     * @param name the name of the property that contains the function
     * @return a {@link Value} representing a member function (bound to the object if need be), or
     *         null if the value of the property is either null or undefined, or "toString" was
     *         requested as the name and the object doesn't directly define it but just inherits it.
     */
    @TruffleBoundary
    public static Value getFunction(final Value obj, final String name) {
        // Since every JS Object has a toString, we only override "String toString()" it if it's
        // explicitly specified
        if (JSRuntime.TO_STRING.equals(name) && !hasOwnProperty(obj, JSRuntime.TO_STRING)) {
            return null;
        }

        final Value fnObj = obj.getMember(name);
        if (fnObj.canExecute()) {
            return fnObj;
        } else if (fnObj.isNull()) { // null or undefined
            return null;
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    private static boolean hasOwnProperty(final Value obj, final String name) {
        Value bindings = Context.getCurrent().getBindings("js");
        try {
            return bindings.getMember("Object").getMember("prototype").getMember("hasOwnProperty").getMember("call").execute(obj, name).asBoolean();
        } catch (Exception e) {
            // probably due to monkey patching, ignore
            return false;
        }
    }

    /**
     * @see JavaAdapterBytecodeGenerator#emitIsFunction
     */
    public static boolean isFunction(final Object obj) {
        return obj instanceof Value && ((Value) obj).canExecute();
    }

    /**
     * Obtains a method handle executing a {@link Value}, adapted for the given {@link MethodType}.
     */
    @TruffleBoundary
    public static MethodHandle getHandle(final MethodType type) {
        MethodHandle call;
        if (type.returnType() == void.class) {
            call = VALUE_EXECUTE_VOID_METHOD_HANDLE;
        } else {
            call = VALUE_EXECUTE_METHOD_HANDLE;
        }
        // TODO this has to be adapted to work with varargs methods as well
        call = call.asCollector(Object[].class, type.parameterCount());

        if (type.returnType() != void.class) {
            call = MethodHandles.filterReturnValue(call, createReturnValueConverter(type.returnType()));
        }

        // insert [function object]
        call = call.asType(type.insertParameterTypes(0, Value.class));
        return call;
    }

    /**
     * Creates and returns a new {@link UnsupportedOperationException}. Makes generated bytecode
     * smaller by doing {@code INVOKESTATIC} to this method rather than the {@code NEW}, {@code DUP}
     * ({@code DUP_X1}, {@code SWAP}), {@code INVOKESPECIAL <init>} sequence.
     *
     * @return a newly created {@link UnsupportedOperationException}.
     */
    public static UnsupportedOperationException unsupported(String methodName) {
        return new UnsupportedOperationException(methodName);
    }

    /**
     * Returns a new {@link RuntimeException} wrapping the passed throwable if necessary. Makes
     * generated bytecode smaller by doing an {@code INVOKESTATIC} to this method rather than the
     * {@code NEW}, {@code DUP_X1}, {@code SWAP}, {@code INVOKESPECIAL <init>} sequence.
     *
     * @param t the original throwable to wrap
     * @return a newly created runtime exception wrapping the passed throwable.
     */
    public static RuntimeException wrapThrowable(final Throwable t) {
        return new RuntimeException(t);
    }

    private static MethodHandle createReturnValueConverter(Class<?> returnType) {
        MethodHandle converterHandle = returnValueConverter.get(returnType);
        if (converterHandle != null) {
            return converterHandle;
        }

        if (returnType == byte.class || returnType == short.class || returnType == int.class) {
            ToIntFunction<Value> converter = value -> {
                if (value.fitsInInt()) {
                    return value.asInt();
                } else if (value.fitsInLong()) {
                    return (int) value.asLong();
                } else if (value.fitsInDouble()) {
                    return (int) value.asDouble();
                }
                return value.asInt();
            };
            try {
                MethodHandle apply = MethodHandles.publicLookup().findVirtual(ToIntFunction.class, "applyAsInt", MethodType.methodType(int.class, Object.class));
                converterHandle = apply.bindTo(converter);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else if (returnType == long.class) {
            ToLongFunction<Value> converter = value -> {
                if (value.fitsInLong()) {
                    return value.asLong();
                } else if (value.fitsInDouble()) {
                    return (long) value.asDouble();
                }
                return value.asLong();
            };
            try {
                MethodHandle apply = MethodHandles.publicLookup().findVirtual(ToLongFunction.class, "applyAsLong", MethodType.methodType(long.class, Object.class));
                converterHandle = apply.bindTo(converter);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else if (returnType == float.class || returnType == double.class) {
            ToDoubleFunction<Value> converter = value -> {
                return value.asDouble();
            };
            try {
                MethodHandle apply = MethodHandles.publicLookup().findVirtual(ToDoubleFunction.class, "applyAsDouble", MethodType.methodType(double.class, Object.class));
                converterHandle = apply.bindTo(converter);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        if (converterHandle != null) {
            converterHandle = converterHandle.asType(converterHandle.type().changeParameterType(0, Value.class));
            MethodHandle existing = returnValueConverter.putIfAbsent(returnType, converterHandle);
            return existing == null ? converterHandle : existing;
        }

        return MethodHandles.insertArguments(VALUE_AS_METHOD_HANDLE, 1, returnType);
    }

    private static final ConcurrentHashMap<Class<?>, MethodHandle> returnValueConverter = new ConcurrentHashMap<>();
}
