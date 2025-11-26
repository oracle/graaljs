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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNodeFactory.DefineStackPropertyNodeGen;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.GraalJSException.JSStackTraceElement;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class InitErrorObjectNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private DynamicObject.PutNode setException;
    @Child private DynamicObject.PutNode setFormattedStack;
    @Child private DynamicObject.PutNode setMessage;
    @Child private DynamicObject.PutNode setErrors;
    @Child private DefineStackPropertyNode defineStackProperty;
    private final boolean defaultColumnNumber;
    @Child private CreateMethodPropertyNode setLineNumber;
    @Child private CreateMethodPropertyNode setColumnNumber;
    @Child private InstallErrorCauseNode installErrorCauseNode;

    private InitErrorObjectNode(JSContext context, boolean defaultColumnNumber) {
        this.context = context;
        this.setFormattedStack = DynamicObject.PutNode.create();
        this.setMessage = DynamicObject.PutNode.create();
        this.defineStackProperty = DefineStackPropertyNode.create();
        this.defaultColumnNumber = defaultColumnNumber;
        if (context.isOptionNashornCompatibilityMode()) {
            this.setLineNumber = CreateMethodPropertyNode.create(context, JSError.LINE_NUMBER_PROPERTY_NAME);
            this.setColumnNumber = CreateMethodPropertyNode.create(context, JSError.COLUMN_NUMBER_PROPERTY_NAME);
        }
    }

    public static InitErrorObjectNode create(JSContext context) {
        return new InitErrorObjectNode(context, false);
    }

    public static InitErrorObjectNode create(JSContext context, boolean defaultColumnNumber) {
        return new InitErrorObjectNode(context, defaultColumnNumber);
    }

    public JSObject execute(JSObject errorObj, GraalJSException exception, TruffleString messageOpt) {
        return execute(errorObj, exception, messageOpt, null);
    }

    public JSObject execute(JSObject errorObj, GraalJSException exception, TruffleString messageOpt, JSObject errorsOpt) {
        return execute(errorObj, exception, messageOpt, errorsOpt, Undefined.instance);
    }

    public JSObject execute(JSObject errorObj, GraalJSException exception, TruffleString messageOpt, JSObject errorsOpt, Object options) {
        if (messageOpt != null) {
            Properties.putWithFlags(setMessage, errorObj, JSError.MESSAGE, messageOpt, JSError.MESSAGE_ATTRIBUTES);
        }
        if (errorsOpt != null) {
            Properties.putWithFlags(setErrorsNode(), errorObj, JSError.ERRORS_NAME, errorsOpt, JSError.ERRORS_ATTRIBUTES);
        }
        if (context.getLanguageOptions().errorCause() && options != Undefined.instance) {
            installErrorCause(errorObj, options);
        }

        setException(errorObj, exception);
        // stack is not formatted until it is accessed
        setFormattedStack.execute(errorObj, JSError.FORMATTED_STACK_NAME, null);
        defineStackProperty.execute(errorObj);

        if (setLineNumber != null && exception.getJSStackTrace().length > 0) {
            JSStackTraceElement topStackTraceElement = exception.getJSStackTrace()[0];
            setLineNumber.executeVoid(errorObj, topStackTraceElement.getLineNumber());
            setColumnNumber.executeVoid(errorObj, defaultColumnNumber ? JSError.DEFAULT_COLUMN_NUMBER : topStackTraceElement.getColumnNumber());
        }
        return errorObj;
    }

    private void setException(JSObject errorObj, GraalJSException exception) {
        if (errorObj instanceof JSErrorObject jsErrorObj) {
            jsErrorObj.setException(exception);
        } else {
            // May be any JSObject when called by Error.captureStackTrace.
            if (setException == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.setException = insert(DynamicObject.PutNode.create());
            }
            setException.execute(errorObj, JSError.EXCEPTION_PROPERTY_NAME, exception);
        }
    }

    private void installErrorCause(JSObject errorObj, Object options) {
        if (installErrorCauseNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            installErrorCauseNode = insert(new InstallErrorCauseNode(context));
        }
        installErrorCauseNode.executeVoid(errorObj, options);
    }

    private DynamicObject.PutNode setErrorsNode() {
        DynamicObject.PutNode errorsLib = setErrors;
        if (errorsLib == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setErrors = errorsLib = insert(DynamicObject.PutNode.create());
        }
        return errorsLib;
    }

    public abstract static class DefineStackPropertyNode extends JavaScriptBaseNode {
        static DefineStackPropertyNode create() {
            return DefineStackPropertyNodeGen.create();
        }

        abstract void execute(JSObject errorObj);

        @Specialization
        void doCached(JSObject errorObj,
                        @Cached DynamicObject.GetPropertyFlagsNode getStackPropertyFlags,
                        @Cached DynamicObject.PutConstantNode putStackProperty) {
            int stackPropertyFlags = getStackPropertyFlags.execute(errorObj, JSError.STACK_NAME, JSProperty.MISSING);
            int attrs = JSAttributes.getDefaultNotEnumerable();
            if (stackPropertyFlags != JSProperty.MISSING) {
                if (!JSProperty.isConfigurable(stackPropertyFlags)) {
                    throw Errors.createTypeErrorCannotRedefineProperty(JSError.STACK_NAME);
                }
                if (JSProperty.isEnumerable(stackPropertyFlags)) {
                    attrs = JSAttributes.getDefault();
                }
            }
            Properties.putConstant(putStackProperty, errorObj, JSError.STACK_NAME, JSError.STACK_PROXY, attrs | JSProperty.PROXY);
        }
    }
}
