/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSException extends GraalJSException {

    private static final long serialVersionUID = -2139936643139844157L;
    private final JSErrorType type;
    private DynamicObject exceptionObj;
    private JSRealm realm;
    private boolean useCallerRealm;

    private JSException(JSErrorType type, String message, Throwable cause, Node originatingNode, int stackTraceLimit, DynamicObject skipFramesUpTo, boolean capture) {
        super(message, cause, originatingNode, stackTraceLimit, skipFramesUpTo, capture);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = null;
    }

    private JSException(JSErrorType type, String message, Node originatingNode, DynamicObject exceptionObj, int stackTraceLimit, DynamicObject skipFramesUpTo, boolean capture) {
        super(message, originatingNode, stackTraceLimit, skipFramesUpTo, capture);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = exceptionObj;
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, DynamicObject exceptionObj, int stackTraceLimit, DynamicObject skipFramesUpTo) {
        return new JSException(type, message, null, exceptionObj, stackTraceLimit, skipFramesUpTo, true);
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, DynamicObject exceptionObj) {
        return new JSException(type, message, null, exceptionObj, JSTruffleOptions.StackTraceLimit, Undefined.instance, true);
    }

    public static JSException create(JSErrorType type, String message) {
        return create(type, message, (Node) null);
    }

    public static JSException create(JSErrorType type, String message, Node originatingNode) {
        return new JSException(type, message, originatingNode, null, JSTruffleOptions.StackTraceLimit, Undefined.instance, false);
    }

    public static JSException create(JSErrorType type, String message, Throwable cause, Node originatingNode) {
        return new JSException(type, message, cause, originatingNode, JSTruffleOptions.StackTraceLimit, Undefined.instance, false);
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
            exceptionObj = JSError.createFromJSException(this, innerRealm, getRawMessage());
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
