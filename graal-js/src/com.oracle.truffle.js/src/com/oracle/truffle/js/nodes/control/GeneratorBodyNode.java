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
package com.oracle.truffle.js.nodes.control;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionRootNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.GeneratorState;
import com.oracle.truffle.js.runtime.builtins.JSGenerator;
import com.oracle.truffle.js.runtime.builtins.JSGeneratorObject;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class GeneratorBodyNode extends JavaScriptNode {
    private static final class GeneratorRootNode extends AbstractFunctionRootNode {
        @Child private CreateIterResultObjectNode createIterResultObject;
        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeYieldValue;
        @Child private JSReadFrameSlotNode readYieldResult;
        private final BranchProfile errorBranch = BranchProfile.create();
        private final ConditionProfile returnOrExceptionProfile = ConditionProfile.create();
        private final TruffleString functionName;

        GeneratorRootNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, SourceSection functionSourceSection,
                        TruffleString functionName, ScriptOrModule activeScriptOrModule) {
            super(context.getLanguage(), functionSourceSection, null, activeScriptOrModule);
            this.createIterResultObject = CreateIterResultObjectNode.create(context);
            this.functionBody = new FunctionBodyNode(functionBody);
            this.writeYieldValue = Objects.requireNonNull(writeYieldValueNode);
            this.readYieldResult = Objects.requireNonNull(readYieldResultNode);
            this.functionName = functionName;
        }

        @Override
        protected Object executeInRealm(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame generatorFrame = JSArguments.getResumeExecutionContext(arguments);
            JSGeneratorObject generatorObject = (JSGeneratorObject) JSArguments.getResumeGeneratorOrPromiseCapability(arguments);
            Completion.Type completionType = JSArguments.getResumeCompletionType(arguments);
            Object value = JSArguments.getResumeCompletionValue(arguments);
            GeneratorState generatorState = generatorValidate(generatorObject);

            if (completionType == Completion.Type.Normal) {
                if (GeneratorState.Completed.equals(generatorState)) {
                    return createIterResultObject.execute(Undefined.instance, true);
                }
                assert GeneratorState.SuspendedStart.equals(generatorState) || GeneratorState.SuspendedYield.equals(generatorState);
            } else {
                Completion completion = Completion.create(completionType, value);
                assert completion.isThrow() || completion.isReturn();
                if (GeneratorState.SuspendedStart.equals(generatorState)) {
                    generatorState = GeneratorState.Completed;
                    generatorObject.setGeneratorState(generatorState);
                }
                if (GeneratorState.Completed.equals(generatorState)) {
                    if (returnOrExceptionProfile.profile(completion.isReturn())) {
                        return createIterResultObject.execute(completion.getValue(), true);
                    } else {
                        assert completion.isThrow();
                        throw UserScriptException.create(completion.getValue(), this, getLanguage().getJSContext().getLanguageOptions().stackTraceLimit());
                    }
                }
                assert GeneratorState.SuspendedYield.equals(generatorState);
                value = completion;
            }

            generatorState = GeneratorState.Executing;
            generatorObject.setGeneratorState(generatorState);

            writeYieldValue.executeWrite(generatorFrame, value);

            try {
                Object result = functionBody.execute(generatorFrame);
                return createIterResultObject.execute(result, true);
            } catch (YieldException e) {
                generatorState = GeneratorState.SuspendedYield;
                return readYieldResult == null ? e.getResult() : readYieldResult.execute(generatorFrame);
            } finally {
                if (GeneratorState.Executing.equals(generatorState)) {
                    generatorState = GeneratorState.Completed;
                }
                generatorObject.setGeneratorState(generatorState);
            }
        }

        private GeneratorState generatorValidate(JSGeneratorObject generatorObject) {
            JSFunction.GeneratorState generatorState = generatorObject.getGeneratorState();
            if (GeneratorState.Executing.equals(generatorState)) {
                errorBranch.enter();
                throw Errors.createTypeError("generator is already executing");
            }
            return generatorState;
        }

        @Override
        public boolean isResumption() {
            return true;
        }

        @Override
        public String getName() {
            if (functionName != null && !functionName.isEmpty()) {
                return Strings.toJavaString(functionName);
            }
            return ":generator";
        }
    }

    @Child private SpecializedNewObjectNode createGeneratorObject;
    private final GeneratorRootNode generatorRootNode;
    private final JSContext context;

    private GeneratorBodyNode(JSContext context, GeneratorRootNode generatorRootNode) {
        this.context = context;
        this.createGeneratorObject = SpecializedNewObjectNode.create(context, false, true, true, false, JSGenerator.INSTANCE);
        this.generatorRootNode = generatorRootNode;
    }

    public static GeneratorBodyNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult,
                    SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        var resumptionRootNode = new GeneratorRootNode(context, body, writeYieldValue, readYieldResult, functionSourceSection, functionName, activeScriptOrModule);
        return new GeneratorBodyNode(context, resumptionRootNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // 14.4.11 Runtime Semantics: GeneratorBody : EvaluateBody
        // Let G be OrdinaryCreateFromConstructor(functionObject, "%GeneratorPrototype%",
        // <<[[GeneratorState]], [[GeneratorContext]]>>).
        JSGeneratorObject generatorObject = (JSGeneratorObject) createGeneratorObject.execute(frame, JSFrameUtil.getFunctionObject(frame));

        generatorStart(frame, generatorObject);

        return generatorObject;
    }

    private void generatorStart(VirtualFrame frame, JSGeneratorObject generatorObject) {
        generatorObject.setGeneratorState(GeneratorState.SuspendedStart);
        generatorObject.setGeneratorContext(frame.materialize());
        generatorObject.setGeneratorTarget(generatorRootNode.getCallTarget());
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new GeneratorBodyNode(context, generatorRootNode);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (!materializedTags.isEmpty()) {
            // ensure resumption call target is visible to instrumentation.
            generatorRootNode.getCallTarget();
        }
        return this;
    }
}
