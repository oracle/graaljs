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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * Provides static utility services to generated Java adapter classes.
 */
public final class JavaAdapterServices {
    private static final MethodHandle VALUE_EXECUTE_METHOD_HANDLE;
    private static final MethodHandle VALUE_EXECUTE_VOID_METHOD_HANDLE;
    private static final MethodHandle VALUE_INVOKE_MEMBER_HANDLE;
    private static final MethodHandle VALUE_AS_METHOD_HANDLE;
    private static final Source HAS_OWN_PROPERTY_SOURCE = Source.newBuilder(ID, "(function(obj, name){return Object.prototype.hasOwnProperty.call(obj, name);})", "hasOwnProperty").buildLiteral();

    static {
        try {
            VALUE_EXECUTE_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "execute", MethodType.methodType(Value.class, Object[].class));
            VALUE_EXECUTE_VOID_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "executeVoid", MethodType.methodType(void.class, Object[].class));
            VALUE_INVOKE_MEMBER_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "invokeMember", MethodType.methodType(Value.class, String.class, Object[].class));
            VALUE_AS_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Value.class, "as", MethodType.methodType(Object.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaAdapterServices() {
        assert !JSTruffleOptions.SubstrateVM;
    }

    /**
     * Returns a JS object used to define methods for the adapter class being initialized. The
     * object is retrieved from the class loader.
     *
     * This method is public solely for implementation reasons, so the adapter classes can invoke it
     * from their static initializers.
     *
     * @return the JS object used to define methods for the class being initialized.
     */
    public static Value getClassOverrides(ClassLoader classLoader) {
        final Value overrides = JavaAdapterClassLoader.getClassOverrides(classLoader);
        assert overrides != null;
        return overrides;
    }

    /**
     * Given a JS object and a method name, checks if a method can be called.
     *
     * This method is public mainly for implementation reasons, so the adapter classes can invoke
     * it.
     *
     * @param obj the script object
     * @param name the name of the property that contains the function
     * @return true if the {@link Value} has an invocable member function, or false if the member
     *         does not exist or is either null, undefined, not callable, or "toString" was
     *         requested and the object does not have an own "toString" method but just inherits it.
     */
    public static boolean hasMethod(final Value obj, final String name) {
        // Since every JS Object has a toString, we only override "String toString()" it if it's
        // explicitly specified
        if (JSRuntime.TO_STRING.equals(name) && !hasOwnProperty(obj, JSRuntime.TO_STRING)) {
            return false;
        }

        return obj.canInvokeMember(name);
    }

    /**
     * Invokes the method of the given object with the given name.
     *
     * This method is public mainly for implementation reasons, so the adapter classes can invoke
     * it.
     *
     * @param obj the script object
     * @param name the name of the property that contains the function
     * @param args the arguments to the method
     * @return the result {@link Value value} of the method invocation
     * @throws org.graalvm.polyglot.PolyglotException if a guest language error occurred during the
     *             method invocation
     */
    public static Value invokeMethod(final Value obj, final String name, final Object[] args) {
        return obj.invokeMember(name, args);
    }

    private static boolean hasOwnProperty(final Value obj, final String name) {
        Value hasOwnProperty = obj.getContext().eval(HAS_OWN_PROPERTY_SOURCE);
        try {
            return hasOwnProperty.execute(obj, name).asBoolean();
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
        MethodHandle call = VALUE_INVOKE_MEMBER_HANDLE;

        call = call.asCollector(Object[].class, type.parameterCount());

        if (type.returnType() != void.class) {
            call = MethodHandles.filterReturnValue(call, createReturnValueConverter(type.returnType()));
        }

        // insert [object, methodName]
        call = call.asType(type.insertParameterTypes(0, Value.class, String.class));
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
        return MethodHandles.insertArguments(VALUE_AS_METHOD_HANDLE, 1, returnType);
    }
}
