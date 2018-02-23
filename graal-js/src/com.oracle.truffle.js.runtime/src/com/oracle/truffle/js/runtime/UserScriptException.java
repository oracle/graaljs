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

    private UserScriptException(Object exceptionObject, Node originatingNode, int stackTraceLimit) {
        super(getMessage(exceptionObject), originatingNode, stackTraceLimit);
        this.exceptionObject = exceptionObject;
    }

    private UserScriptException(Throwable exception, Node originatingNode, int stackTraceLimit) {
        super(exception.toString(), exception, originatingNode, stackTraceLimit);
        this.exceptionObject = exception;
    }

    @TruffleBoundary
    public static UserScriptException createCapture(Object exceptionObject, Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo) {
        return fillInStackTrace(new UserScriptException(exceptionObject, originatingNode, stackTraceLimit), skipFramesUpTo, true);
    }

    @TruffleBoundary
    public static UserScriptException create(Object exceptionObject, Node originatingNode) {
        return fillInStackTrace(new UserScriptException(exceptionObject, originatingNode, JSTruffleOptions.StackTraceLimit), Undefined.instance, false);
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
        return fillInStackTrace(new UserScriptException(exception, originatingNode, JSTruffleOptions.StackTraceLimit), Undefined.instance, false);
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
