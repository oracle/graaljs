/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ErrorFunctionBuiltinsFactory.ErrorCaptureStackTraceNodeGen;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSError} function (constructor).
 */
public final class ErrorFunctionBuiltins extends JSBuiltinsContainer.Lambda {
    public ErrorFunctionBuiltins() {
        super(JSError.CLASS_NAME);
        defineFunction("captureStackTrace", 2, JSAttributes.getDefault(),
                        (context, builtin) -> ErrorCaptureStackTraceNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context)));
    }

    public abstract static class ErrorCaptureStackTraceNode extends JSBuiltinNode {
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;

        public ErrorCaptureStackTraceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initErrorObjectNode = InitErrorObjectNode.create(context, false);
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create(context);
        }

        @Specialization
        protected Object captureStackTrace(VirtualFrame frame, Object object, Object skipUpTo) {
            if (!JSRuntime.isObject(object)) {
                throw Errors.createTypeError("invalid_argument");
            }
            DynamicObject obj = (DynamicObject) object;
            if (!JSObject.isExtensible(obj)) {
                throw Errors.createTypeError("Cannot define property:stack, object is not extensible.");
            }
            int stackTraceLimit = stackTraceLimitNode.executeInt(frame);
            Object skipFramesUpTo = JSFunction.isJSFunction(skipUpTo) ? skipUpTo : JSArguments.getFunctionObject(frame.getArguments());
            UserScriptException ex = UserScriptException.create(obj, JSTruffleOptions.NashornCompatibilityMode ? this : null, stackTraceLimit, (DynamicObject) skipFramesUpTo);
            initErrorObjectNode.execute(obj, ex, getContext().getRealm());
            return Undefined.instance;
        }

    }
}
