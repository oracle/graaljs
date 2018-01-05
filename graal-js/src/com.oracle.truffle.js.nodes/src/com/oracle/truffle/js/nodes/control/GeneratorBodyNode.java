/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessFunctionNode;
import com.oracle.truffle.js.nodes.function.JSNewNode.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.GeneratorResumeMethod;
import com.oracle.truffle.js.runtime.builtins.JSFunction.GeneratorState;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

public final class GeneratorBodyNode extends JavaScriptNode {
    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of generator functions in JavaScript.")
    private static class GeneratorRootNode extends JavaScriptRealmBoundaryRootNode {
        @Child private CreateIterResultObjectNode createIterResultObject;
        @Child private PropertyGetNode getGeneratorContext;
        @Child private PropertyGetNode getGeneratorState;
        @Child private PropertySetNode setGeneratorState;
        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeYieldValue;
        @Child private JSReadFrameSlotNode readYieldResult;
        private final JSContext context;

        GeneratorRootNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, SourceSection functionSourceSection) {
            super(context.getLanguage(), functionSourceSection, null);
            this.createIterResultObject = CreateIterResultObjectNode.create(context);
            this.getGeneratorContext = PropertyGetNode.create(JSFunction.GENERATOR_CONTEXT_ID, false, context);
            this.getGeneratorState = PropertyGetNode.create(JSFunction.GENERATOR_STATE_ID, false, context);
            this.setGeneratorState = PropertySetNode.create(JSFunction.GENERATOR_STATE_ID, false, context, false);
            this.functionBody = functionBody;
            this.writeYieldValue = writeYieldValueNode;
            this.readYieldResult = readYieldResultNode;
            this.context = context;
        }

        @Override
        public Object executeAndSetRealm(VirtualFrame frame) {
            DynamicObject generatorObject = (DynamicObject) frame.getArguments()[0];
            Object value = frame.getArguments()[1];
            GeneratorResumeMethod method = (GeneratorResumeMethod) frame.getArguments()[2];

            VirtualFrame generatorFrame = (VirtualFrame) getGeneratorContext.getValue(generatorObject);
            GeneratorState generatorState = generatorValidate(generatorObject);

            if (method == GeneratorResumeMethod.Next) {
                if (GeneratorState.Completed.equals(generatorState)) {
                    return createIterResultObject.execute(frame, Undefined.instance, true);
                }
                assert GeneratorState.SuspendedStart.equals(generatorState) || GeneratorState.SuspendedYield.equals(generatorState);
            } else {
                assert method == GeneratorResumeMethod.Throw || method == GeneratorResumeMethod.Return;
                if (GeneratorState.SuspendedStart.equals(generatorState)) {
                    setGeneratorState.setValue(generatorObject, generatorState = GeneratorState.Completed);
                }
                if (GeneratorState.Completed.equals(generatorState)) {
                    if (method == GeneratorResumeMethod.Return) {
                        return createIterResultObject.execute(frame, value, true);
                    } else {
                        assert method == GeneratorResumeMethod.Throw;
                        throw UserScriptException.create(value, this);
                    }
                }
                assert GeneratorState.SuspendedYield.equals(generatorState);
                value = new Pair<>(value, method);
            }

            setGeneratorState.setValue(generatorObject, generatorState = GeneratorState.Executing);

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
                throw Errors.createTypeError("not a generator");
            } else if (GeneratorState.Executing.equals(generatorState)) {
                throw Errors.createTypeError("generator is already executing");
            }
            return (GeneratorState) generatorState;
        }

        @Override
        protected JSRealm getRealm() {
            return context.getRealm();
        }
    }

    @Child private JavaScriptNode createGeneratorObject;
    @Child private PropertySetNode setGeneratorState;
    @Child private PropertySetNode setGeneratorContext;
    @Child private PropertySetNode setGeneratorTarget;
    @CompilationFinal private RootCallTarget generatorCallTarget;
    private final JSContext context;

    @Child private JavaScriptNode functionBody;
    @Child private JSWriteFrameSlotNode writeYieldValueNode;
    @Child private JSReadFrameSlotNode readYieldResultNode;

    private GeneratorBodyNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode) {
        this.context = context;
        JavaScriptNode functionObject = AccessFunctionNode.create();
        this.createGeneratorObject = SpecializedNewObjectNode.create(context, false, true, functionObject);
        this.setGeneratorState = PropertySetNode.create(JSFunction.GENERATOR_STATE_ID, false, context, false);
        this.setGeneratorContext = PropertySetNode.create(JSFunction.GENERATOR_CONTEXT_ID, false, context, false);
        this.setGeneratorTarget = PropertySetNode.create(JSFunction.GENERATOR_TARGET_ID, false, context, false);

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
            GeneratorRootNode generatorRootNode = new GeneratorRootNode(context, functionBody, writeYieldValueNode, readYieldResultNode, getRootNode().getSourceSection());
            this.generatorCallTarget = Truffle.getRuntime().createCallTarget(generatorRootNode);
            // these children have been transferred to the generator root node and are now disowned
            this.functionBody = null;
            this.writeYieldValueNode = null;
            this.readYieldResultNode = null;
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
        DynamicObject generatorObject;
        try {
            generatorObject = createGeneratorObject.executeDynamicObject(frame);
        } catch (UnexpectedResultException e) {
            throw new AssertionError();
        }

        generatorStart(frame, generatorObject);

        return generatorObject;
    }

    private void generatorStart(VirtualFrame frame, DynamicObject generatorObject) {
        setGeneratorState.setValue(generatorObject, GeneratorState.SuspendedStart);
        setGeneratorContext.setValue(generatorObject, frame.materialize());
        setGeneratorTarget.setValue(generatorObject, generatorCallTarget);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        if (generatorCallTarget == null) {
            return create(context, cloneUninitialized(functionBody), cloneUninitialized(writeYieldValueNode), cloneUninitialized(readYieldResultNode));
        } else {
            GeneratorRootNode generatorRoot = (GeneratorRootNode) generatorCallTarget.getRootNode();
            return create(context, cloneUninitialized(generatorRoot.functionBody), cloneUninitialized(generatorRoot.writeYieldValue), cloneUninitialized(generatorRoot.readYieldResult));
        }
    }
}
