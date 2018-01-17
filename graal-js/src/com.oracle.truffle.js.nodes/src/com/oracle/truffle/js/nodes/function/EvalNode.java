/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.EvalCallTag;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeChildren({@NodeChild(value = "function"), @NodeChild(value = "source")})
public abstract class EvalNode extends JavaScriptNode {
    private final JSContext context;
    private final Object currEnv;
    @Child private JavaScriptNode thisObject;
    @Child private AbstractFunctionArgumentsNode otherArguments;
    private final BranchProfile hasOtherArguments = BranchProfile.create();

    protected EvalNode(JSContext context, Object currEnv, JavaScriptNode thisObject, AbstractFunctionArgumentsNode otherArguments) {
        assert currEnv != null;

        this.context = context;
        this.currEnv = currEnv;
        this.thisObject = thisObject;
        this.otherArguments = otherArguments;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == EvalCallTag.class) {
            return true;
        }
        return super.hasTag(tag);
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
        return Source.newBuilder(sourceCode.toString()).name(evalSourceName).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
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
    protected Object directEvalOverridden(VirtualFrame frame, Object evalFunction, Object arg0, //
                    @Cached("createCall()") JSFunctionCallNode redirectCall) {

        Object[] evaluatedArgs = new Object[1 + otherArguments.getCount(frame)];
        evaluatedArgs[0] = arg0;
        evaluatedArgs = otherArguments.executeFillObjectArray(frame, evaluatedArgs, 1);
        Object thisObj = Undefined.instance;

        return redirectCall.executeCall(JSArguments.create(thisObj, evalFunction, evaluatedArgs));
    }

    protected final boolean isEvalOverridden(Object function) {
        return function != context.getRealm().getEvalFunctionObject();
    }

    /**
     * Execute surplus arguments for the sake of side-effects.
     */
    private void evalOtherArgs(VirtualFrame frame) {
        if (otherArguments.getCount(frame) > 0) {
            hasOtherArguments.enter();
            Object[] evaluatedArgs = new Object[otherArguments.getCount(frame)];
            otherArguments.executeFillObjectArray(frame, evaluatedArgs, 0);
        }
    }

    /**
     * In case of multiple arguments, only the first one is used. The others are ignored, but have
     * to be evaluated for the sake of side-effects.
     */
    public static EvalNode create(JSContext context, Object env, JavaScriptNode sourceArg, JavaScriptNode[] args, JavaScriptNode functionNode, JavaScriptNode thisObject) {
        return EvalNodeGen.create(context, env, thisObject, JSFunctionArgumentsNode.create(args), functionNode, sourceArg);
    }

    abstract JavaScriptNode getFunction();

    abstract JavaScriptNode getSource();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return EvalNodeGen.create(context, currEnv, cloneUninitialized(thisObject), AbstractFunctionArgumentsNode.cloneUninitialized(otherArguments), cloneUninitialized(getFunction()),
                        cloneUninitialized(getSource()));
    }
}
