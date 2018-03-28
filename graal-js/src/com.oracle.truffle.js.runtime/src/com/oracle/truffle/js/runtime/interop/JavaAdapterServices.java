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
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.interop.Converters.Converter;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Provides static utility services to generated Java adapter classes.
 *
 * @see JSFunction#call(DynamicObject, Object, Object[])
 */
public final class JavaAdapterServices {
    private static final MethodHandle JSFUNCTION_CALL_METHOD_HANDLE;
    private static final MethodHandle CONVERTER_CONVERT_METHOD_HANDLE;
    private static final MethodHandle JAVA_TO_JS_CONVERTER_METHOD_HANDLE;
    private static final ThreadLocal<DynamicObject> classOverrides = new ThreadLocal<>();

    static {
        try {
            JSFUNCTION_CALL_METHOD_HANDLE = MethodHandles.publicLookup().findStatic(JavaAdapterServices.class, "callFunction",
                            MethodType.methodType(Object.class, DynamicObject.class, Object.class, Object[].class));
            CONVERTER_CONVERT_METHOD_HANDLE = MethodHandles.publicLookup().findVirtual(Converter.class, "convert", MethodType.methodType(Object.class, Object.class));
            JAVA_TO_JS_CONVERTER_METHOD_HANDLE = CONVERTER_CONVERT_METHOD_HANDLE.bindTo(Converters.JAVA_TO_JS_CONVERTER);
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
    public static DynamicObject getClassOverrides() {
        final DynamicObject overrides = classOverrides.get();
        assert overrides != null;
        return overrides;
    }

    public static void setClassOverrides(DynamicObject overrides) {
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

    /**
     * @see JavaAdapterBytecodeGenerator#emitIsJSFunction
     */
    public static boolean isJSFunction(final Object obj) {
        return JSObject.isDynamicObject(obj) && JSFunction.isJSFunction((DynamicObject) obj);
    }

    /**
     * Entry point for JS function calls by generated Java adapter classes. Enters the function's
     * {@link TruffleContext} for the duration of the call.
     *
     * @see #JSFUNCTION_CALL_METHOD_HANDLE
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
        MethodHandle call = JSFUNCTION_CALL_METHOD_HANDLE;
        // TODO this has to be adapted to work with varargs methods as well
        call = call.asCollector(Object[].class, type.parameterCount());

        MethodHandle[] filters = new MethodHandle[type.parameterCount()];
        Arrays.fill(filters, JAVA_TO_JS_CONVERTER_METHOD_HANDLE);
        call = MethodHandles.filterArguments(call, 2, filters);

        if (type.returnType() != void.class) {
            call = MethodHandles.filterReturnValue(call,
                            CONVERTER_CONVERT_METHOD_HANDLE.bindTo(Converters.LazySerialConverterAdapter.fromFactories(type.returnType(), Converters.FORCING_CONVERTER_FACTORIES)));
        }

        // insert [function object, this object]
        call = call.asType(type.insertParameterTypes(0, DynamicObject.class, Object.class));
        return call;
    }

    public static void sameThreadCheck(long expectedThreadId) {
        if (Thread.currentThread().getId() != expectedThreadId) {
            throw new Error("attempted to execute javascript function in a different thread");
        }
    }
}
