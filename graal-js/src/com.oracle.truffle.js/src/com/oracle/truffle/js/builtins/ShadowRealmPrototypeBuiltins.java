/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltins.CopyFunctionNameAndLengthNode;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.GetWrappedValueNodeGen;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.ShadowRealmEvaluateNodeGen;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.ShadowRealmImportValueNodeGen;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.ImportCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealmObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in functions of the {@code %ShadowRealm.prototype%}.
 */
public final class ShadowRealmPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ShadowRealmPrototypeBuiltins.ShadowRealmPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ShadowRealmPrototypeBuiltins();

    protected ShadowRealmPrototypeBuiltins() {
        super(JSShadowRealm.PROTOTYPE_NAME, ShadowRealmPrototype.class);
    }

    public enum ShadowRealmPrototype implements BuiltinEnum<ShadowRealmPrototype> {
        evaluate(1),
        importValue(2);

        private final int length;

        ShadowRealmPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ShadowRealmPrototype builtinEnum) {
        switch (builtinEnum) {
            case evaluate:
                return ShadowRealmEvaluateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case importValue:
                return ShadowRealmImportValueNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSFunction.class)
    abstract static class GetWrappedValueNode extends JavaScriptBaseNode {

        abstract Object execute(JSContext context, JSRealm callerRealm, Object value);

        @Specialization(guards = "isCallable.executeBoolean(value)", limit = "1")
        protected final Object objectCallable(JSContext context, JSRealm callerRealm, Object value,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("create(context)") CopyFunctionNameAndLengthNode copyNameAndLengthNode) {
            CompilerAsserts.partialEvaluationConstant(context);
            JSFunctionData wrappedFunctionCall = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.OrdinaryWrappedFunctionCall, ShadowRealmPrototypeBuiltins::createWrappedFunctionImpl);
            JSFunctionObject wrapped = JSFunction.createWrapped(context, callerRealm, wrappedFunctionCall, value);
            try {
                copyNameAndLengthNode.execute(wrapped, value, Strings.EMPTY_STRING, 0);
            } catch (AbstractTruffleException ex) {
                throw toTypeError(ex, callerRealm);
            }
            return wrapped;
        }

        @Specialization(guards = {"isObject.executeBoolean(value)", "!isCallable.executeBoolean(value)"})
        protected final Object objectNotCallable(@SuppressWarnings("unused") JSContext context, @SuppressWarnings("unused") JSRealm callerRealm, Object value,
                        @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObject,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            throw Errors.createTypeErrorNotAFunction(value, this);
        }

        @Specialization(guards = {"!isObject.executeBoolean(value)"})
        protected static Object primitive(@SuppressWarnings("unused") JSContext context, @SuppressWarnings("unused") JSRealm callerRealm, Object value,
                        @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObject) {
            return value;
        }

        @TruffleBoundary
        private AbstractTruffleException toTypeError(AbstractTruffleException exception, JSRealm callerRealm) {
            if (exception instanceof JSException) {
                var jsException = (JSException) exception;
                if (jsException.getErrorType() == JSErrorType.TypeError && jsException.getRealm() == callerRealm) {
                    return jsException;
                }
            }
            return Errors.createTypeError(exception, this);
        }

        @NeverDefault
        public static GetWrappedValueNode create() {
            return GetWrappedValueNodeGen.create();
        }
    }

    private static JSFunctionData createWrappedFunctionImpl(JSContext context) {
        final class WrappedFunctionRootNode extends JavaScriptRootNode {
            @Child private JSFunctionCallNode callWrappedTargetFunction = JSFunctionCallNode.createCall();
            @Child private GetWrappedValueNode getWrappedValue = GetWrappedValueNode.create();

            protected WrappedFunctionRootNode(JavaScriptLanguage lang) {
                super(lang, null, null);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                JSFunctionObject.Wrapped functionObject = (JSFunctionObject.Wrapped) JSArguments.getFunctionObject(args);
                Object target = functionObject.getWrappedTargetFunction();
                assert JSRuntime.isCallable(target) : target;
                JSRealm callerRealm = functionObject.getRealm();
                JSRealm targetRealm = JSRuntime.getFunctionRealm(target, callerRealm);
                JSRealm mainRealm = JSRealm.getMain(this);
                JSRealm prevRealm = mainRealm.enterRealm(this, callerRealm);
                assert getRealm() == callerRealm;
                try {
                    int argCount = JSArguments.getUserArgumentCount(args);
                    Object[] wrappedArgs = JSArguments.createInitial(Undefined.instance, target, argCount);
                    for (int i = 0; i < argCount; i++) {
                        JSArguments.setUserArgument(wrappedArgs, i, getWrappedValue.execute(context, targetRealm, JSArguments.getUserArgument(args, i)));
                    }
                    Object wrappedThisArgument = getWrappedValue.execute(context, targetRealm, JSArguments.getThisObject(args));
                    JSArguments.setThisObject(wrappedArgs, wrappedThisArgument);
                    Object result;
                    try {
                        JSRealm prevCallerRealm = mainRealm.enterRealm(this, targetRealm);
                        assert prevCallerRealm == callerRealm;
                        try {
                            result = callWrappedTargetFunction.executeCall(wrappedArgs);
                        } finally {
                            mainRealm.leaveRealm(this, callerRealm);
                        }
                    } catch (AbstractTruffleException ex) {
                        throw wrapErrorFromShadowRealm(ex);
                    }
                    return getWrappedValue.execute(context, callerRealm, result);
                } finally {
                    mainRealm.leaveRealm(this, prevRealm);
                }
            }

            @TruffleBoundary
            private JSException wrapErrorFromShadowRealm(AbstractTruffleException ex) {
                String message = ex.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "Wrapped function call failed";
                } else {
                    String typeErrorPrefix = "TypeError: ";
                    String messagePrefix = "Wrapped function call failed with: ";
                    if (message.startsWith(typeErrorPrefix) && message.startsWith(messagePrefix, typeErrorPrefix.length())) {
                        message = message.substring(typeErrorPrefix.length());
                    } else {
                        message = messagePrefix + message;
                    }
                }
                return Errors.createTypeError(message, ex, this);
            }
        }
        return JSFunctionData.createCallOnly(context, new WrappedFunctionRootNode(context.getLanguage()).getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    @ImportStatic(JSShadowRealm.class)
    public abstract static class ShadowRealmEvaluateNode extends JSBuiltinNode {

        public ShadowRealmEvaluateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object evaluate(JSShadowRealmObject thisObj, TruffleString sourceText,
                        @Cached IndirectCallNode callNode,
                        @Cached GetWrappedValueNode getWrappedValue) {
            JSRealm callerRealm = getRealm();
            JSRealm evalRealm = thisObj.getShadowRealm();
            getContext().checkEvalAllowed();
            var script = parseScript(Strings.toJavaString(sourceText));
            Object result;
            try {
                JSRealm mainRealm = JSRealm.getMain(this);
                JSRealm prevRealm = mainRealm.enterRealm(this, evalRealm);
                try {
                    result = script.runEval(callNode, evalRealm);
                } finally {
                    mainRealm.leaveRealm(this, prevRealm);
                }
            } catch (AbstractTruffleException ex) {
                throw wrapErrorFromShadowRealm(ex);
            }
            return getWrappedValue.execute(getContext(), callerRealm, result);
        }

        @TruffleBoundary
        private JSException wrapErrorFromShadowRealm(AbstractTruffleException ex) {
            return Errors.createTypeError("ShadowRealm.prototype.evaluate failed with: " + ex.getMessage(), ex, this);
        }

        private ScriptNode parseScript(String sourceCode) {
            CompilerAsserts.neverPartOfCompilation();
            assert getContext().getLanguageOptions().allowEval();
            Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceCode, Evaluator.EVAL_SOURCE_NAME).build();
            return getContext().getEvaluator().parseEval(getContext(), this, source, null);
        }

        @TruffleBoundary
        @Specialization(guards = "!isString(sourceText)")
        protected Object invalidSourceText(@SuppressWarnings("unused") JSShadowRealmObject thisObj, @SuppressWarnings("unused") Object sourceText) {
            throw Errors.createTypeErrorNotAString(sourceText);
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSShadowRealm(thisObj)")
        protected Object invalidReceiver(Object thisObj, @SuppressWarnings("unused") Object sourceText) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSShadowRealm.class)
    public abstract static class ShadowRealmImportValueNode extends JSBuiltinNode {
        protected static final HiddenKey EXPORT_NAME_STRING = new HiddenKey("ExportNameString");

        public ShadowRealmImportValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object importValue(@SuppressWarnings("unused") JSShadowRealmObject thisObj, Object specifier, Object exportName,
                        @Cached JSToStringNode toStringNode,
                        @Cached("create(getContext())") NewPromiseCapabilityNode newPromiseCapabilityNode,
                        @Cached("create(getContext())") PerformPromiseThenNode performPromiseThenNode,
                        @Cached("createSetHidden(EXPORT_NAME_STRING, getContext())") PropertySetNode setExportNameStringNode,
                        @Cached("create(getContext())") ImportCallNode importNode) {
            TruffleString specifierString = toStringNode.executeString(specifier);
            if (!JSGuards.isString(exportName)) {
                throw Errors.createTypeErrorNotAString(exportName);
            }
            TruffleString exportNameString = (TruffleString) exportName;
            JSRealm callerRealm = getRealm();
            JSRealm evalRealm = thisObj.getShadowRealm();

            PromiseCapabilityRecord innerCapability = newPromiseCapabilityNode.executeDefault();
            JSRealm mainRealm = JSRealm.getMain(this);
            JSRealm prevRealm = mainRealm.enterRealm(this, evalRealm);
            try {
                // Off-spec: Provide a caller source that allow relative paths to be resolved.
                Source callerSource = (Strings.startsWith(specifierString, Strings.DOT_SLASH) || Strings.startsWith(specifierString, Strings.DOT_DOT_SLASH)) ? retrieveCallerSource() : null;
                ScriptOrModule activeScriptOrModule = callerSource == null ? null : new ScriptOrModule(getContext(), callerSource);
                importNode.hostImportModuleDynamically(activeScriptOrModule, ModuleRequest.create(specifierString), innerCapability);
            } finally {
                mainRealm.leaveRealm(this, prevRealm);
            }

            JSFunctionData functionData = getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ExportGetter, ShadowRealmImportValueNode::createExportGetterImpl);
            var onFulfilled = JSFunction.create(callerRealm, functionData);
            setExportNameStringNode.setValue(onFulfilled, exportNameString);
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            return performPromiseThenNode.execute((JSPromiseObject) innerCapability.getPromise(), onFulfilled, callerRealm.getThrowTypeErrorFunction(), promiseCapability);
        }

        private static JSFunctionData createExportGetterImpl(JSContext context) {
            final class ExportGetterRootNode extends JavaScriptRealmBoundaryRootNode {
                @Child private JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);
                @Child private PropertyGetNode getExportNameString = PropertyGetNode.createGetHidden(EXPORT_NAME_STRING, context);
                @Child private JSHasPropertyNode hasOwnProperty = JSHasPropertyNode.create(true);
                @Child private ReadElementNode getExport = ReadElementNode.create(context);
                @Child private GetWrappedValueNode getWrappedValue = GetWrappedValueNode.create();

                protected ExportGetterRootNode(JavaScriptLanguage lang) {
                    super(lang);
                }

                @Override
                public Object executeInRealm(VirtualFrame frame) {
                    JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    TruffleString exportNameString = (TruffleString) getExportNameString.getValue(functionObject);
                    Object exports = argumentNode.execute(frame);
                    JSRealm callerRealm = functionObject.getRealm();
                    assert getRealm() == callerRealm;
                    if (!hasOwnProperty.executeBoolean(exports, exportNameString)) {
                        throw Errors.createTypeErrorCannotGetProperty(context, exportNameString, exports, false, this);
                    }
                    Object value = getExport.executeWithTargetAndIndex(exports, exportNameString);
                    return getWrappedValue.execute(context, callerRealm, value);
                }
            }
            return JSFunctionData.createCallOnly(context, new ExportGetterRootNode(context.getLanguage()).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSShadowRealm(thisObj)")
        protected Object invalidReceiver(Object thisObj, @SuppressWarnings("unused") Object specifier, @SuppressWarnings("unused") Object exportName) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }

        @TruffleBoundary
        private static Source retrieveCallerSource() {
            Source callerSource = Truffle.getRuntime().iterateFrames(frameInstance -> {
                if (!(frameInstance.getCallTarget() instanceof RootCallTarget)) {
                    return null;
                }
                RootNode root = ((RootCallTarget) frameInstance.getCallTarget()).getRootNode();
                if (root.isInternal()) {
                    return null;
                }
                SourceSection sourceSection = root.getSourceSection();
                if (sourceSection != null && sourceSection.isAvailable()) {
                    return sourceSection.getSource();
                }
                return null;
            });
            return callerSource;
        }
    }
}
