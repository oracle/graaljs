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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Provides static utility services to generated Java adapter classes.
 *
 * @see JSFunction#call(DynamicObject, Object, Object[])
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

    public static void setClassOverrides(Value overrides) {
        classOverrides.set(overrides);
    }

    /**
     * Given a JS script object, retrieves a function from it by name, binds it to the script object
     * as its "this", and adapts its parameter types, return types, and arity to the specified type
     * and arity. This method is public mainly for implementation reasons, so the adapter classes
     * can invoke it from their constructors that take a Object in its first argument to obtain the
     * method handles for their method implementations.
     *
     * @param obj the script obj
     * @param name the name of the property that contains the function
     * @return the appropriately adapted method handle for invoking the script function, or null if
     *         the value of the property is either null or undefined, or "toString" was requested as
     *         the name, but the object doesn't directly define it but just inherits it through
     *         prototype.
     */
    @TruffleBoundary
    public static DynamicObject getFunction(final DynamicObject obj, final String name) {
        // Since every JS Object has a toString, we only override "String toString()" it if it's
        // explicitly specified
        if (JSRuntime.TO_STRING.equals(name) && !JSObject.hasOwnProperty(obj, JSRuntime.TO_STRING)) {
            return null;
        }

        final Object fnObj = JSObject.get(obj, name);
        if (JSFunction.isJSFunction(fnObj)) {
            return (DynamicObject) fnObj;
        } else if (fnObj == Null.instance || fnObj == Undefined.instance) {
            return null;
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    @TruffleBoundary
    public static Value getFunction(final Value obj, final String name) {
        // Since every JS Object has a toString, we only override "String toString()" it if it's
        // explicitly specified
        if (JSRuntime.TO_STRING.equals(name)) {
            // && !JSObject.hasOwnProperty(obj, JSRuntime.TO_STRING)
            return null;
        }

        final Value fnObj = obj.getMember(name);
        if (fnObj.canExecute()) {
            return fnObj;
        } else if (fnObj.isNull()) {
            return null;
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    /**
     * @see JavaAdapterBytecodeGenerator#emitIsFunction
     */
    public static boolean isFunction(final Object obj) {
        return obj instanceof Value && ((Value) obj).canExecute();
    }

    /**
     * Entry point for JS function calls by generated Java adapter classes. Enters the function's
     * {@link TruffleContext} for the duration of the call.
     */
    public static Object callFunction(DynamicObject functionObject, Object thisObject, Object[] argumentValues) {
        JSRealm functionRealm = JSFunction.getRealm(functionObject);
        TruffleContext truffleContext = functionRealm.getTruffleContext();
        Object prev = truffleContext.enter();
        try {
            return JSFunction.call(functionObject, thisObject, argumentValues);
        } finally {
            truffleContext.leave(prev);
        }
    }

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

    public static Value asValue(Object obj) {
        return Context.getCurrent().asValue(obj);
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
        return MethodHandles.insertArguments(VALUE_AS_METHOD_HANDLE, 1, returnType);
    }
}
