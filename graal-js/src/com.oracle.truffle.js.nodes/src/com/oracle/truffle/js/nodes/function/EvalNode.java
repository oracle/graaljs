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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class EvalNode extends JavaScriptNode {
    private final JSContext context;
    private final Object currEnv;
    @Child @Executed protected JavaScriptNode functionNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    @Child private JavaScriptNode thisObject;
    @Child private AbstractFunctionArgumentsNode otherArguments;

    protected EvalNode(JSContext context, Object currEnv, JavaScriptNode function, JavaScriptNode source, JavaScriptNode thisObject, AbstractFunctionArgumentsNode otherArguments) {
        assert currEnv != null;

        this.context = context;
        this.currEnv = currEnv;
        this.functionNode = function;
        this.sourceNode = source;
        this.thisObject = thisObject;
        this.otherArguments = otherArguments;
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
    protected Object directEval(VirtualFrame frame, @SuppressWarnings("unused") Object evalFunction, CharSequence sourceCode) {
        evalOtherArgs(frame);
        final Source source = sourceFromString(sourceCode);
        return context.getEvaluator().evaluate(context.getRealm(), this, source, currEnv, frame.materialize(), thisObject.execute(frame));
    }

    @TruffleBoundary
    private Source sourceFromString(CharSequence sourceCode) {
        String evalSourceName = null;
        if (context.isOptionV8CompatibilityMode()) {
            evalSourceName = formatEvalOrigin(this);
        }
        if (evalSourceName == null) {
            evalSourceName = Evaluator.EVAL_SOURCE_NAME;
        }
        return Source.newBuilder(AbstractJavaScriptLanguage.ID, sourceCode.toString(), evalSourceName).build();
    }

    @TruffleBoundary
    private static String formatEvalOrigin(Node callNode) {
        if (callNode == null) {
            return null;
        }
        SourceSection sourceSection = callNode.getEncapsulatingSourceSection();
        String sourceName = sourceSection.getSource().getName();
        String callerName = callNode.getRootNode().getName();
        if (sourceName.startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX)) {
            return Evaluator.EVAL_AT_SOURCE_NAME_PREFIX + callerName + " (" + sourceName + ")";
        } else {
            return Evaluator.EVAL_AT_SOURCE_NAME_PREFIX + callerName + " (" + sourceName + ":" + sourceSection.getStartLine() + ":" + sourceSection.getStartColumn() + ")";
        }
    }

    @TruffleBoundary
    public static String findAndFormatEvalOrigin(Node evalNode) {
        String evalOrigin = formatEvalOrigin(evalNode);
        if (evalOrigin != null) {
            return evalOrigin;
        }
        return Truffle.getRuntime().iterateFrames(frameInstance -> formatEvalOrigin(frameInstance.getCallNode()));
    }

    @Specialization(guards = {"!isEvalOverridden(evalFunction)", "!isString(arg0)"})
    protected Object directEval(VirtualFrame frame, @SuppressWarnings("unused") Object evalFunction, Object arg0) {
        evalOtherArgs(frame);
        return arg0;
    }

    @Specialization(guards = {"isEvalOverridden(evalFunction)"})
    protected Object directEvalOverridden(VirtualFrame frame, Object evalFunction, Object arg0,
                    @Cached("createCall()") JSFunctionCallNode redirectCall) {
        Object thisObj = Undefined.instance;
        Object[] evaluatedArgs;
        if (otherArguments == null) {
            evaluatedArgs = new Object[]{arg0};
        } else {
            evaluatedArgs = new Object[1 + otherArguments.getCount(frame)];
            evaluatedArgs[0] = arg0;
            evaluatedArgs = otherArguments.executeFillObjectArray(frame, evaluatedArgs, 1);
        }
        return redirectCall.executeCall(JSArguments.create(thisObj, evalFunction, evaluatedArgs));
    }

    protected final boolean isEvalOverridden(Object function) {
        return function != context.getRealm().getEvalFunctionObject();
    }

    /**
     * Execute surplus arguments for the sake of side-effects.
     */
    private void evalOtherArgs(VirtualFrame frame) {
        if (otherArguments != null) {
            Object[] evaluatedArgs = new Object[otherArguments.getCount(frame)];
            otherArguments.executeFillObjectArray(frame, evaluatedArgs, 0);
        }
    }

    /**
     * In case of multiple arguments, only the first one is used. The others are ignored, but have
     * to be evaluated for the sake of side-effects.
     */
    public static EvalNode create(JSContext context, Object env, JavaScriptNode sourceArg, JavaScriptNode[] otherArgs, JavaScriptNode functionNode, JavaScriptNode thisObject) {
        return EvalNodeGen.create(context, env, functionNode, sourceArg, thisObject, otherArgs.length > 0 ? JSFunctionArgumentsNode.create(otherArgs) : null);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return EvalNodeGen.create(context, currEnv, cloneUninitialized(functionNode), cloneUninitialized(sourceNode), cloneUninitialized(thisObject),
                        AbstractFunctionArgumentsNode.cloneUninitialized(otherArguments));
    }
}
