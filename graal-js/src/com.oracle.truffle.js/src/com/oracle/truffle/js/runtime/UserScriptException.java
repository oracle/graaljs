/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@SuppressWarnings("serial")
@ImportStatic({JSConfig.class})
@ExportLibrary(value = InteropLibrary.class, delegateTo = "exceptionObject")
public final class UserScriptException extends GraalJSException {

    final Object exceptionObject;

    private UserScriptException(Object exceptionObject, Node originatingNode, int stackTraceLimit) {
        super(getMessage(exceptionObject), originatingNode, stackTraceLimit);
        this.exceptionObject = exceptionObject;
    }

    private UserScriptException(Throwable exception, Node originatingNode, int stackTraceLimit) {
        super(exception.toString(), exception, originatingNode, stackTraceLimit);
        this.exceptionObject = exception;
    }

    @TruffleBoundary
    public static UserScriptException createCapture(Object exceptionObject, Node originatingNode, int stackTraceLimit, JSDynamicObject skipFramesUpTo, boolean customSkip) {
        return fillInStackTrace(new UserScriptException(exceptionObject, originatingNode, stackTraceLimit), true, skipFramesUpTo, customSkip);
    }

    @TruffleBoundary
    public static UserScriptException createCapture(Object exceptionObject, Node originatingNode, int stackTraceLimit) {
        return createCapture(exceptionObject, originatingNode, stackTraceLimit, Undefined.instance, false);
    }

    @TruffleBoundary
    public static UserScriptException create(Object exceptionObject, Node originatingNode, int stackTraceLimit) {
        return fillInStackTrace(new UserScriptException(exceptionObject, originatingNode, stackTraceLimit), false);
    }

    @TruffleBoundary
    public static UserScriptException create(Object exceptionObject) {
        int stackTraceLimit = JavaScriptLanguage.getCurrentLanguage().getJSContext().getLanguageOptions().stackTraceLimit();
        return create(exceptionObject, null, stackTraceLimit);
    }

    @Override
    public Object getErrorObject() {
        return exceptionObject;
    }

    @Override
    public Object getErrorObjectLazy() {
        return exceptionObject;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isException() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public RuntimeException throwException() {
        throw this;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public ExceptionType getExceptionType() {
        return ExceptionType.RUNTIME_ERROR;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isExceptionIncompleteSource() {
        return false;
    }

    @ExportMessage
    public boolean hasExceptionMessage() {
        return getMessage() != null;
    }

    @ExportMessage
    public Object getExceptionMessage() throws UnsupportedMessageException {
        String message = getMessage();
        if (message == null) {
            throw UnsupportedMessageException.create();
        } else {
            return message;
        }
    }

    /**
     * Best effort method to get the error message without side effects.
     */
    @TruffleBoundary
    private static String getMessage(Object exc) {
        if (exc instanceof JSObject errorObj) {
            // try to get the constructor name, and then the message
            String name = getErrorNameSafe(errorObj, "Error");
            String message = getErrorMessageSafe(errorObj, null);
            if (message == null && JSObject.getJSContext(errorObj).getLanguageOptions().testV8Mode()) {
                // allow side effect (MjsUnitAssertionError.prototype.message is a getter)
                Object messageValue = JSObject.get(errorObj, JSError.MESSAGE);
                if (messageValue instanceof TruffleString messageStr && !messageStr.isEmpty()) {
                    message = Strings.toJavaString(messageStr);
                }
            }
            return concatErrorNameAndMessage(name, message);
        }
        return Strings.toJavaString(JSRuntime.safeToString(exc));
    }

}
