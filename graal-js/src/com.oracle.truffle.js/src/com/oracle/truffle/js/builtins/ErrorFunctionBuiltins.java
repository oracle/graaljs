/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.ErrorFunctionBuiltinsFactory.ErrorCaptureStackTraceNodeGen;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSError} function (constructor).
 */
public final class ErrorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<ErrorFunctionBuiltins.ErrorFunction> {
    public static final JSBuiltinsContainer BUILTINS = new ErrorFunctionBuiltins();

    protected ErrorFunctionBuiltins() {
        super(JSError.CLASS_NAME, ErrorFunction.class);
    }

    public enum ErrorFunction implements BuiltinEnum<ErrorFunction> {
        captureStackTrace(2);

        private final int length;

        ErrorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ErrorFunction builtinEnum) {
        switch (builtinEnum) {
            case captureStackTrace:
                return ErrorCaptureStackTraceNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ErrorCaptureStackTraceNode extends JSBuiltinNode {
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;

        public ErrorCaptureStackTraceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create();
        }

        @Specialization
        protected Object captureStackTrace(VirtualFrame frame, Object object, Object skipUpTo,
                        @Cached InlinedBranchProfile errorProfile) {
            if (!JSRuntime.isObject(object)) {
                errorProfile.enter(this);
                throw Errors.createTypeError("invalid_argument");
            }
            JSObject obj = (JSObject) object;
            if (!JSObject.isExtensible(obj)) {
                errorProfile.enter(this);
                throw Errors.createTypeError("Cannot define property:stack, object is not extensible.");
            }
            int stackTraceLimit = stackTraceLimitNode.executeInt();
            boolean customSkip = JSFunction.isJSFunction(skipUpTo);
            JSFunctionObject skipFramesUpTo = customSkip ? (JSFunctionObject) skipUpTo : (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
            UserScriptException ex = UserScriptException.createCapture(obj, getContext().isOptionNashornCompatibilityMode() ? this : null, stackTraceLimit, skipFramesUpTo, customSkip);
            initErrorObjectNode.execute(obj, ex, null);
            return Undefined.instance;
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }
}
