/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
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
            JSFUNCTION_CALL_METHOD_HANDLE = MethodHandles.publicLookup().findStatic(JSFunction.class, "call", MethodType.methodType(Object.class, DynamicObject.class, Object.class, Object[].class));
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

    public static boolean isJSFunction(final Object obj) {
        return JSObject.isDynamicObject(obj) && JSFunction.isJSFunction((DynamicObject) obj);
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
