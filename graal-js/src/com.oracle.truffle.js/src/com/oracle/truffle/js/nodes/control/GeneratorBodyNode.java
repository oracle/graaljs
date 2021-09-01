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
package com.oracle.truffle.js.nodes.control;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.GeneratorState;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class GeneratorBodyNode extends JavaScriptNode {
    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of generator functions in JavaScript.")
    private static class GeneratorRootNode extends JavaScriptRootNode {
        @Child private CreateIterResultObjectNode createIterResultObject;
        @Child private PropertyGetNode getGeneratorState;
        @Child private PropertySetNode setGeneratorState;
        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeYieldValue;
        @Child private JSReadFrameSlotNode readYieldResult;
        private final BranchProfile errorBranch = BranchProfile.create();
        private final ConditionProfile returnOrExceptionProfile = ConditionProfile.createBinaryProfile();
        private final String functionName;

        GeneratorRootNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, SourceSection functionSourceSection,
                        String functionName) {
            super(context.getLanguage(), functionSourceSection, null);
            this.createIterResultObject = CreateIterResultObjectNode.create(context);
            this.getGeneratorState = PropertyGetNode.createGetHidden(JSFunction.GENERATOR_STATE_ID, context);
            this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
            this.functionBody = new FunctionBodyNode(functionBody);
            Objects.requireNonNull(writeYieldValueNode);
            Objects.requireNonNull(readYieldResultNode);
            this.writeYieldValue = writeYieldValueNode;
            this.readYieldResult = readYieldResultNode;
            this.functionName = functionName;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame generatorFrame = JSArguments.getResumeExecutionContext(arguments);
            DynamicObject generatorObject = (DynamicObject) JSArguments.getResumeGeneratorOrPromiseCapability(arguments);
            Completion.Type completionType = JSArguments.getResumeCompletionType(arguments);
            Object value = JSArguments.getResumeCompletionValue(arguments);
            GeneratorState generatorState = generatorValidate(generatorObject);

            if (completionType == Completion.Type.Normal) {
                if (GeneratorState.Completed.equals(generatorState)) {
                    return createIterResultObject.execute(frame, Undefined.instance, true);
                }
                assert GeneratorState.SuspendedStart.equals(generatorState) || GeneratorState.SuspendedYield.equals(generatorState);
            } else {
                Completion completion = Completion.create(completionType, value);
                assert completion.isThrow() || completion.isReturn();
                if (GeneratorState.SuspendedStart.equals(generatorState)) {
                    generatorState = GeneratorState.Completed;
                    setGeneratorState.setValue(generatorObject, generatorState);
                }
                if (GeneratorState.Completed.equals(generatorState)) {
                    if (returnOrExceptionProfile.profile(completion.isReturn())) {
                        return createIterResultObject.execute(frame, completion.getValue(), true);
                    } else {
                        assert completion.isThrow();
                        throw UserScriptException.create(completion.getValue(), this, getGeneratorState.getContext().getContextOptions().getStackTraceLimit());
                    }
                }
                assert GeneratorState.SuspendedYield.equals(generatorState);
                value = completion;
            }

            generatorState = GeneratorState.Executing;
            setGeneratorState.setValue(generatorObject, generatorState);

            writeYieldValue.executeWrite(generatorFrame, value);

            try {
                Object result = functionBody.execute(generatorFrame);
                return createIterResultObject.execute(frame, result, true);
            } catch (YieldException e) {
                generatorState = GeneratorState.SuspendedYield;
                return readYieldResult == null ? e.getResult() : readYieldResult.execute(generatorFrame);
            } finally {
                if (GeneratorState.Executing.equals(generatorState)) {
                    generatorState = GeneratorState.Completed;
                }
                setGeneratorState.setValue(generatorObject, generatorState);
            }
        }

        private GeneratorState generatorValidate(DynamicObject generatorObject) {
            Object generatorState = getGeneratorState.getValue(generatorObject);
            if (generatorState == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeErrorGeneratorObjectExpected();
            } else if (GeneratorState.Executing.equals(generatorState)) {
                errorBranch.enter();
                throw Errors.createTypeError("generator is already executing");
            }
            return (GeneratorState) generatorState;
        }

        @Override
        public boolean isResumption() {
            return true;
        }

        @Override
        public String getName() {
            if (functionName != null && !functionName.isEmpty()) {
                return functionName;
            }
            return ":generator";
        }
    }

    @Child private SpecializedNewObjectNode createGeneratorObject;
    @Child private PropertySetNode setGeneratorState;
    @Child private PropertySetNode setGeneratorContext;
    @Child private PropertySetNode setGeneratorTarget;
    @CompilationFinal private volatile RootCallTarget generatorCallTarget;
    private final JSContext context;

    @Child private JavaScriptNode functionBody;
    @Child private JSWriteFrameSlotNode writeYieldValueNode;
    @Child private JSReadFrameSlotNode readYieldResultNode;

    private GeneratorBodyNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode) {
        this.context = context;
        this.createGeneratorObject = SpecializedNewObjectNode.create(context, false, true, true, false);
        this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
        this.setGeneratorContext = PropertySetNode.createSetHidden(JSFunction.GENERATOR_CONTEXT_ID, context);
        this.setGeneratorTarget = PropertySetNode.createSetHidden(JSFunction.GENERATOR_TARGET_ID, context);

        // these children are adopted here only temporarily; they will be transferred later
        this.functionBody = functionBody;
        this.writeYieldValueNode = writeYieldValueNode;
        this.readYieldResultNode = readYieldResultNode;
    }

    public static GeneratorBodyNode create(JSContext context, JavaScriptNode expression, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult) {
        return new GeneratorBodyNode(context, expression, writeYieldValue, readYieldResult);
    }

    private void initializeGeneratorCallTarget() {
        CompilerAsserts.neverPartOfCompilation();
        atomic(() -> {
            if (generatorCallTarget == null) {
                RootNode rootNode = getRootNode();
                GeneratorRootNode generatorRootNode = new GeneratorRootNode(context, functionBody, writeYieldValueNode, readYieldResultNode, rootNode.getSourceSection(), rootNode.getName());
                this.generatorCallTarget = Truffle.getRuntime().createCallTarget(generatorRootNode);
                // these children have been transferred to the generator root node and are now
                // disowned
                this.functionBody = null;
                this.writeYieldValueNode = null;
                this.readYieldResultNode = null;
            }
        });
    }

    private void ensureGeneratorCallTargetInitialized() {
        if (generatorCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeGeneratorCallTarget();
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ensureGeneratorCallTargetInitialized();

        // 14.4.11 Runtime Semantics: GeneratorBody : EvaluateBody
        // Let G be OrdinaryCreateFromConstructor(functionObject, "%GeneratorPrototype%",
        // <<[[GeneratorState]], [[GeneratorContext]]>>).
        DynamicObject generatorObject = createGeneratorObject.execute(frame, JSFrameUtil.getFunctionObject(frame));

        generatorStart(frame, generatorObject);

        return generatorObject;
    }

    private void generatorStart(VirtualFrame frame, DynamicObject generatorObject) {
        setGeneratorState.setValue(generatorObject, GeneratorState.SuspendedStart);
        setGeneratorContext.setValue(generatorObject, frame.materialize());
        setGeneratorTarget.setValue(generatorObject, generatorCallTarget);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return atomic(() -> {
            if (generatorCallTarget == null) {
                return create(context, cloneUninitialized(functionBody, materializedTags), cloneUninitialized(writeYieldValueNode, materializedTags),
                                cloneUninitialized(readYieldResultNode, materializedTags));
            } else {
                GeneratorRootNode generatorRoot = (GeneratorRootNode) generatorCallTarget.getRootNode();
                return create(context, cloneUninitialized(generatorRoot.functionBody, materializedTags), cloneUninitialized(generatorRoot.writeYieldValue, materializedTags),
                                cloneUninitialized(generatorRoot.readYieldResult, materializedTags));
            }
        });
    }
}
