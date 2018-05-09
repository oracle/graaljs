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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSException extends GraalJSException {

    private static final long serialVersionUID = -2139936643139844157L;
    private final JSErrorType type;
    private DynamicObject exceptionObj;
    private JSRealm realm;
    private boolean useCallerRealm;

    private JSException(JSErrorType type, String message, Throwable cause, Node originatingNode, int stackTraceLimit) {
        super(message, cause, originatingNode, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = null;
    }

    private JSException(JSErrorType type, String message, Node originatingNode, DynamicObject exceptionObj, int stackTraceLimit) {
        super(message, originatingNode, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = exceptionObj;
    }

    private JSException(JSErrorType type, String message, SourceSection sourceLocation, int stackTraceLimit) {
        super(message, sourceLocation, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = null;
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, DynamicObject exceptionObj, int stackTraceLimit, DynamicObject skipFramesUpTo) {
        return fillInStackTrace(new JSException(type, message, null, exceptionObj, stackTraceLimit), skipFramesUpTo, true);
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, DynamicObject exceptionObj) {
        return createCapture(type, message, exceptionObj, JSTruffleOptions.StackTraceLimit, Undefined.instance);
    }

    public static JSException create(JSErrorType type, String message) {
        return create(type, message, (Node) null);
    }

    public static JSException create(JSErrorType type, String message, Node originatingNode) {
        return fillInStackTrace(new JSException(type, message, originatingNode, null, JSTruffleOptions.StackTraceLimit), Undefined.instance, false);
    }

    public static JSException create(JSErrorType type, String message, Throwable cause, Node originatingNode) {
        return fillInStackTrace(new JSException(type, message, cause, originatingNode, JSTruffleOptions.StackTraceLimit), Undefined.instance, false);
    }

    public static JSException create(JSErrorType type, String message, SourceSection sourceLocation) {
        return fillInStackTrace(new JSException(type, message, sourceLocation, JSTruffleOptions.StackTraceLimit), Undefined.instance, false);
    }

    @Override
    @TruffleBoundary
    public String getMessage() {
        String message = getRawMessage();
        return (message == null || message.isEmpty()) ? type.name() : type.name() + ": " + message;
    }

    public String getRawMessage() {
        return super.getMessage();
    }

    public JSErrorType getErrorType() {
        return this.type;
    }

    @Override
    public DynamicObject getErrorObject() {
        return exceptionObj;
    }

    public void setErrorObject(DynamicObject exceptionObj) {
        this.exceptionObj = exceptionObj;
    }

    @TruffleBoundary
    @Override
    public Object getErrorObjectEager(JSContext context) {
        if (exceptionObj == null) { // not thread safe, but should be all right in this case
            JSRealm innerRealm = this.realm == null ? context.getRealm() : this.realm;
            String message = getRawMessage();
            exceptionObj = JSError.createFromJSException(this, innerRealm, (message == null) ? "" : message);
        }
        return exceptionObj;
    }

    public JSException setRealm(JSRealm realm) {
        if (this.realm == null) {
            if (useCallerRealm) {
                // ignore the first set, that is the callee realm!
                useCallerRealm = false;
            } else {
                this.realm = realm;
            }
        }
        return this;
    }

    public JSRealm getRealm() {
        return this.realm;
    }

    public JSException useCallerRealm() {
        this.useCallerRealm = true;
        return this;
    }

    @Override
    public boolean isSyntaxError() {
        return this.type == JSErrorType.SyntaxError;
    }
}
