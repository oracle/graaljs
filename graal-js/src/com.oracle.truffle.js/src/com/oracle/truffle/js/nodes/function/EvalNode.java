/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.function;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class EvalNode extends JavaScriptNode {
    private final JSContext context;
    @Child @Executed protected JavaScriptNode functionNode;
    @Child protected AbstractFunctionArgumentsNode arguments;
    @Child protected DirectEvalNode directEvalNode;

    protected EvalNode(JSContext context, JavaScriptNode function, JavaScriptNode[] args, JavaScriptNode thisObject, Object env, FrameSlot blockScopeSlot) {
        this(context, function, JSFunctionArgumentsNode.create(context, args), DirectEvalNode.create(context, thisObject, env, blockScopeSlot));
    }

    protected EvalNode(JSContext context, JavaScriptNode functionNode, AbstractFunctionArgumentsNode arguments, DirectEvalNode directEvalNode) {
        this.context = context;
        this.functionNode = functionNode;
        this.arguments = arguments;
        this.directEvalNode = directEvalNode;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == EvalCallTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Specialization(guards = {"!isEvalOverridden(evalFunction)"})
    protected Object evalNotOverridden(VirtualFrame frame, @SuppressWarnings("unused") Object evalFunction) {
        int argCount = arguments.getCount(frame);
        Object[] args = new Object[argCount];
        args = arguments.executeFillObjectArray(frame, args, 0);
        Object source = args.length == 0 ? Undefined.instance : args[0];
        return directEvalNode.executeWithSource(frame, source);
    }

    @Specialization(guards = {"isEvalOverridden(evalFunction)"})
    protected Object evalOverridden(VirtualFrame frame, Object evalFunction,
                    @Cached("createCall()") JSFunctionCallNode redirectCall) {
        int argCount = arguments.getCount(frame);
        Object[] args = JSArguments.createInitial(Undefined.instance, evalFunction, argCount);
        args = arguments.executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        return redirectCall.executeCall(args);
    }

    protected final boolean isEvalOverridden(Object function) {
        return function != getRealm().getEvalFunctionObject();
    }

    public static EvalNode create(JSContext context, JavaScriptNode functionNode, JavaScriptNode[] args, JavaScriptNode thisObject, Object env, FrameSlot blockScopeSlot) {
        return EvalNodeGen.create(context, functionNode, args, thisObject, env, blockScopeSlot);
    }

    @TruffleBoundary
    private static String formatEvalOrigin(Node callNode, JSContext context) {
        if (callNode == null) {
            return null;
        }
        SourceSection sourceSection = callNode.getEncapsulatingSourceSection();
        if (sourceSection == null) {
            return null;
        }
        String sourceName = sourceSection.getSource().getName();
        String callerName = callNode.getRootNode().getName();
        if (callerName == null || callerName.startsWith(":")) {
            callerName = JSError.getAnonymousFunctionNameStackTrace(context);
        }
        if (sourceName.startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX)) {
            return Evaluator.EVAL_AT_SOURCE_NAME_PREFIX + callerName + " (" + sourceName + ")";
        } else {
            return Evaluator.EVAL_AT_SOURCE_NAME_PREFIX + callerName + " (" + sourceName + ":" + sourceSection.getStartLine() + ":" + sourceSection.getStartColumn() + ")";
        }
    }

    @TruffleBoundary
    public static String findAndFormatEvalOrigin(Node evalNode, JSContext context) {
        String evalOrigin = formatEvalOrigin(evalNode, context);
        if (evalOrigin != null) {
            return evalOrigin;
        }
        return Truffle.getRuntime().iterateFrames(frameInstance -> formatEvalOrigin(frameInstance.getCallNode(), context));
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return EvalNodeGen.create(context, cloneUninitialized(functionNode, materializedTags), AbstractFunctionArgumentsNode.cloneUninitialized(arguments, materializedTags),
                        directEvalNode.copyUninitialized(materializedTags));
    }

    protected abstract static class DirectEvalNode extends JavaScriptBaseNode {
        private final JSContext context;
        private final Object currEnv;
        @Child private JavaScriptNode thisNode;
        @Child private IndirectCallNode callNode;
        private final FrameSlot blockScopeSlot;

        protected DirectEvalNode(JSContext context, JavaScriptNode thisNode, Object currEnv, FrameSlot blockScopeSlot) {
            assert currEnv != null;
            this.context = context;
            this.currEnv = currEnv;
            this.thisNode = thisNode;
            this.callNode = IndirectCallNode.create();
            this.blockScopeSlot = blockScopeSlot;
        }

        protected static DirectEvalNode create(JSContext context, JavaScriptNode thisNode, Object currEnv, FrameSlot blockScopeSlot) {
            return EvalNodeGen.DirectEvalNodeGen.create(context, thisNode, currEnv, blockScopeSlot);
        }

        public abstract Object executeWithSource(VirtualFrame frame, Object source);

        @Specialization
        protected int directEvalInt(int source) {
            return source;
        }

        @Specialization
        protected SafeInteger directEvalSafeInteger(SafeInteger source) {
            return source;
        }

        @Specialization
        protected long directEvalLong(long source) {
            return source;
        }

        @Specialization
        protected double directEvalDouble(double source) {
            return source;
        }

        @Specialization
        protected boolean directEvalBoolean(boolean source) {
            return source;
        }

        @Specialization
        protected Symbol directEvalSymbol(Symbol source) {
            return source;
        }

        @Specialization
        protected BigInt directEvalBigInt(BigInt source) {
            return source;
        }

        @Specialization(guards = {"isJSDynamicObject(source)"})
        protected DynamicObject directEvalJSType(DynamicObject source) {
            return source;
        }

        @Specialization
        protected Object directEvalCharSequence(VirtualFrame frame, CharSequence source) {
            return directEvalImpl(frame, source);
        }

        private Object directEvalImpl(VirtualFrame frame, CharSequence sourceCode) {
            final Source source = sourceFromString(sourceCode);
            JSRealm realm = getRealm();
            Object evalThis = thisNode.execute(frame);
            ScriptNode script = context.getEvaluator().parseDirectEval(context, getParent(), source, currEnv);
            MaterializedFrame blockScopeFrame;
            if (blockScopeSlot != null) {
                Object maybeFrame = frame.getObject(blockScopeSlot);
                blockScopeFrame = JSFrameUtil.castMaterializedFrame(maybeFrame);
            } else {
                blockScopeFrame = frame.materialize();
            }
            return script.runEval(callNode, realm, evalThis, blockScopeFrame);
        }

        @Specialization(guards = {"isForeignObject(sourceCode)"}, limit = "3")
        protected Object directEvalForeignObject(VirtualFrame frame, Object sourceCode,
                        @CachedLibrary("sourceCode") InteropLibrary interop) {
            if (interop.isString(sourceCode)) {
                try {
                    return directEvalImpl(frame, interop.asString(sourceCode));
                } catch (UnsupportedMessageException ex) {
                    throw Errors.createTypeErrorInteropException(sourceCode, ex, "asString", this);
                }
            } else {
                return sourceCode;
            }
        }

        @TruffleBoundary
        private Source sourceFromString(CharSequence sourceCode) {
            String evalSourceName = null;
            if (context.isOptionV8CompatibilityMode()) {
                evalSourceName = formatEvalOrigin(this, context);
            }
            if (evalSourceName == null) {
                evalSourceName = Evaluator.EVAL_SOURCE_NAME;
            }
            return Source.newBuilder(JavaScriptLanguage.ID, sourceCode.toString(), evalSourceName).build();
        }

        protected DirectEvalNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return create(context, cloneUninitialized(thisNode, materializedTags), currEnv, blockScopeSlot);
        }

    }

}
