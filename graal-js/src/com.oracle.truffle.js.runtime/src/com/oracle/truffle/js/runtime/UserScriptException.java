/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class UserScriptException extends GraalJSException {

    private static final long serialVersionUID = -6624166672101791072L;
    private final Object exceptionObject;

    private UserScriptException(Object exceptionObject, Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo, boolean capture) {
        super(getMessage(exceptionObject), originatingNode, stackTraceLimit, skipFramesUpTo, capture);
        this.exceptionObject = exceptionObject;
    }

    private UserScriptException(Throwable exception, Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo, boolean capture) {
        super(exception.toString(), exception, originatingNode, stackTraceLimit, skipFramesUpTo, capture);
        this.exceptionObject = exception;
    }

    @TruffleBoundary
    public static UserScriptException createCapture(Object exceptionObject, Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo) {
        return new UserScriptException(exceptionObject, originatingNode, stackTraceLimit, skipFramesUpTo, true);
    }

    @TruffleBoundary
    public static UserScriptException create(Object exceptionObject, Node originatingNode) {
        return new UserScriptException(exceptionObject, originatingNode, JSTruffleOptions.StackTraceLimit, Undefined.instance, false);
    }

    @TruffleBoundary
    public static UserScriptException create(Object exceptionObject) {
        return create(exceptionObject, null);
    }

    @TruffleBoundary
    public static UserScriptException createJavaException(Throwable exception) {
        return createJavaException(exception, null);
    }

    @TruffleBoundary
    public static UserScriptException createJavaException(Throwable exception, Node originatingNode) {
        return new UserScriptException(exception, originatingNode, JSTruffleOptions.StackTraceLimit, Undefined.instance, false);
    }

    @Override
    public Object getErrorObject() {
        return exceptionObject;
    }

    @Override
    public Object getErrorObjectEager(JSContext context) {
        return exceptionObject;
    }

    /**
     * Best effort method to get the error message without side effects.
     */
    @TruffleBoundary
    private static String getMessage(Object exc) {
        if (JSObject.isJSObject(exc)) {
            // try to get the constructor name, and then the message
            DynamicObject prototype = JSObject.getPrototype((DynamicObject) exc);
            if (prototype != Null.instance) {
                Object constructor = prototype.get(JSObject.CONSTRUCTOR, null);
                if (JSFunction.isJSFunction(constructor)) {
                    String name = JSFunction.getName((DynamicObject) constructor);
                    if (!name.isEmpty()) {
                        Object message = ((DynamicObject) exc).get(JSError.MESSAGE, null);
                        if (JSRuntime.isString(message)) {
                            return name + ": " + message;
                        }
                        return name;
                    }
                }
            }
            return JSObject.safeToString((DynamicObject) exc);
        }
        return Boundaries.stringValueOf(exc);
    }

}
