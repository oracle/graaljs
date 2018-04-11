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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.CallSitePrototypeBuiltinsFactory.CallSiteGetBooleanNodeGen;
import com.oracle.truffle.js.builtins.CallSitePrototypeBuiltinsFactory.CallSiteGetNodeGen;
import com.oracle.truffle.js.builtins.CallSitePrototypeBuiltinsFactory.CallSiteGetNumberNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException.JSStackTraceElement;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * @see JSError
 */
public final class CallSitePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<CallSitePrototypeBuiltins.CallSitePrototype> {
    protected CallSitePrototypeBuiltins() {
        super(JSError.CALL_SITE_PROTOTYPE_NAME, CallSitePrototype.class);
    }

    public enum CallSitePrototype implements BuiltinEnum<CallSitePrototype> {
        getThis(0),
        getTypeName(0),
        getFunction(0),
        getFunctionName(0),
        getMethodName(0),
        getFileName(0),
        getLineNumber(0),
        getColumnNumber(0),
        getPosition(0),
        getEvalOrigin(0),
        getScriptNameOrSourceURL(0),
        isToplevel(0),
        isEval(0),
        isNative(0),
        isConstructor(0),
        toString(0);

        private final int length;

        CallSitePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, CallSitePrototype builtinEnum) {
        switch (builtinEnum) {
            case getColumnNumber:
            case getLineNumber:
            case getPosition:
                return CallSiteGetNumberNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case getFunction:
            case getThis:
            case getFileName:
            case getFunctionName:
            case getMethodName:
            case getTypeName:
            case toString:
            case getEvalOrigin:
            case getScriptNameOrSourceURL:
                return CallSiteGetNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case isToplevel:
            case isEval:
            case isNative:
            case isConstructor:
                return CallSiteGetBooleanNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class CallSiteOperation extends JSBuiltinNode {
        @Child private PropertyGetNode getStackTraceElementNode;
        private final BranchProfile errorBranch = BranchProfile.create();

        public CallSiteOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getStackTraceElementNode = PropertyGetNode.createGetHidden(JSError.STACK_TRACE_ELEMENT_PROPERTY_NAME, context);
        }

        protected final JSStackTraceElement getStackTraceElement(DynamicObject thisObj) {
            Object element = getStackTraceElementNode.getValue(thisObj);
            if (!(element instanceof JSStackTraceElement)) {
                errorBranch.enter();
                throw Errors.createTypeError("Expected CallSite as receiver");
            }
            return (JSStackTraceElement) element;
        }
    }

    abstract static class CallSiteGetNumberNode extends CallSiteOperation {
        private final CallSitePrototype method;

        CallSiteGetNumberNode(JSContext context, JSBuiltin builtin, CallSitePrototype method) {
            super(context, builtin);
            this.method = method;
        }

        @Specialization
        final int getNumber(DynamicObject thisObj) {
            JSStackTraceElement stackTraceElement = getStackTraceElement(thisObj);
            switch (method) {
                case getLineNumber:
                    return stackTraceElement.getLineNumber();
                case getColumnNumber:
                    return stackTraceElement.getColumnNumber();
                case getPosition:
                    return stackTraceElement.getPosition();
                default:
                    throw Errors.shouldNotReachHere();
            }
        }
    }

    abstract static class CallSiteGetNode extends CallSiteOperation {
        private final CallSitePrototype method;

        CallSiteGetNode(JSContext context, JSBuiltin builtin, CallSitePrototype method) {
            super(context, builtin);
            this.method = method;
        }

        @Specialization
        final Object getFunctionName(DynamicObject thisObj) {
            JSStackTraceElement stackTraceElement = getStackTraceElement(thisObj);
            switch (method) {
                case getFunction:
                    if (stackTraceElement.isStrict()) {
                        return Undefined.instance;
                    }
                    return JSRuntime.nullToUndefined(stackTraceElement.getFunction());
                case getThis:
                    if (stackTraceElement.isStrict()) {
                        return Undefined.instance;
                    }
                    return JSRuntime.nullToUndefined(stackTraceElement.getThisOrGlobal());
                case toString:
                    return stackTraceElement.toString();
                case getTypeName:
                    return JSRuntime.toJSNull(stackTraceElement.getTypeName());
                case getFunctionName: {
                    String functionName = stackTraceElement.getFunctionName();
                    return functionName.isEmpty() ? Null.instance : functionName;
                }
                case getMethodName: {
                    String methodName = stackTraceElement.getMethodName();
                    if (methodName == null || methodName.isEmpty() || methodName.equals(JSError.ANONYMOUS_FUNCTION_NAME_STACK_TRACE)) {
                        return Null.instance;
                    } else {
                        return methodName;
                    }
                }
                case getFileName:
                case getScriptNameOrSourceURL: {
                    String fileName = stackTraceElement.getFileName();
                    if (fileName != null && !fileName.startsWith("<")) {
                        return fileName;
                    } else {
                        return Null.instance;
                    }
                }
                case getEvalOrigin: {
                    String evalOrigin = stackTraceElement.getEvalOrigin();
                    if (evalOrigin != null) {
                        return evalOrigin;
                    } else {
                        return Undefined.instance;
                    }
                }
                default:
                    throw Errors.shouldNotReachHere();
            }
        }
    }

    abstract static class CallSiteGetBooleanNode extends CallSiteOperation {
        private final CallSitePrototype method;

        CallSiteGetBooleanNode(JSContext context, JSBuiltin builtin, CallSitePrototype method) {
            super(context, builtin);
            this.method = method;
        }

        @Specialization
        final boolean getBoolean(DynamicObject thisObj) {
            JSStackTraceElement stackTraceElement = getStackTraceElement(thisObj);
            switch (method) {
                case isConstructor:
                    return stackTraceElement.isConstructor();
                case isEval:
                    return stackTraceElement.isEval();
                case isNative:
                    return JSFunction.isJSFunction(stackTraceElement.getFunction()) && JSFunction.isBuiltin((DynamicObject) stackTraceElement.getFunction());
                case isToplevel:
                    return JSRuntime.isNullOrUndefined(stackTraceElement.getThis()) || JSGlobalObject.isJSGlobalObject(stackTraceElement.getThis()) || stackTraceElement.isEval();
                default:
                    throw Errors.shouldNotReachHere();
            }
        }
    }
}
