/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.parser.env;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public final class FunctionEnvironment extends Environment {
    private static final String RETURN_SLOT_IDENTIFIER = "<return>";
    static final String ARGUMENTS_SLOT_IDENTIFIER = "<arguments>";
    static final String THIS_SLOT_IDENTIFIER = "<this>";
    static final String SUPER_SLOT_IDENTIFIER = "<super>";
    static final String NEW_TARGET_SLOT_IDENTIFIER = "<new.target>";
    static final String YIELD_VALUE_SLOT_IDENTIFIER = "<yieldvalue>";
    static final String ASYNC_CONTEXT_SLOT_IDENTIFIER = "<asynccontext>";
    static final String ASYNC_RESULT_SLOT_IDENTIFIER = "<asyncresult>";
    private static final String YIELD_RESULT_SLOT_IDENTIFIER = "<yieldresult>";
    public static final String DYNAMIC_SCOPE_IDENTIFIER = ScopeFrameNode.EVAL_SCOPE_IDENTIFIER;

    private final FunctionEnvironment parentFunction;
    private final JSFrameDescriptor frameDescriptor;
    private EconomicMap<JSFrameSlot, Integer> parameters;
    private final boolean isStrictMode;

    private JSFrameSlot returnSlot;
    private JSFrameSlot blockScopeSlot;

    private String functionName = "";
    private String internalFunctionName = "";
    private boolean isNamedExpression;
    private boolean needsParentFrame;
    private boolean frozen;

    private int breakNodeCount;
    private int continueNodeCount;
    private boolean hasReturn;
    private boolean hasYield;
    private boolean hasAwait;

    private List<BreakTarget> jumpTargetStack;
    private boolean directArgumentsAccess;

    private final boolean isGlobal;
    private final boolean isEval;
    private final boolean isDirectEval;
    private final boolean isArrowFunction;
    private final boolean isGeneratorFunction;
    private final boolean isDerivedConstructor;
    private final boolean isAsyncFunction;
    // Synthetic arguments are declared externally, e.g. when parsing with arguments.
    private final boolean hasSyntheticArguments;
    private boolean hasRestParameter;
    private boolean simpleParameterList = true;
    private boolean isDynamicallyScoped;
    private boolean needsNewTarget;
    private final boolean inDirectEval;

    public FunctionEnvironment(Environment parent, NodeFactory factory, JSContext context,
                    boolean isStrictMode, boolean isEval, boolean isDirectEval, boolean isArrowFunction, boolean isGeneratorFunction, boolean isDerivedConstructor, boolean isAsyncFunction,
                    boolean isGlobal, boolean hasSyntheticArguments) {
        super(parent, factory, context);
        this.isDirectEval = isDirectEval;
        this.isAsyncFunction = isAsyncFunction;
        this.isStrictMode = isStrictMode;
        this.isEval = isEval;
        this.isArrowFunction = isArrowFunction;
        this.isGeneratorFunction = isGeneratorFunction;
        this.isDerivedConstructor = isDerivedConstructor;
        this.isGlobal = isGlobal;
        this.hasSyntheticArguments = hasSyntheticArguments;
        this.parentFunction = parent == null ? null : parent.function();

        this.frameDescriptor = factory.createFunctionFrameDescriptor();
        this.inDirectEval = isDirectEval || (parent != null && parent.function() != null && parent.function().inDirectEval());
    }

    @Override
    public JSFrameSlot declareLocalVar(Object name) {
        assert !isFrozen() : name;
        return getFunctionFrameDescriptor().findOrAddFrameSlot(name, FrameSlotKind.Illegal);
    }

    public JSFrameSlot getReturnSlot() {
        if (returnSlot == null) {
            returnSlot = declareLocalVar(RETURN_SLOT_IDENTIFIER);
        }
        return returnSlot;
    }

    public JSFrameSlot getAsyncResultSlot() {
        return declareLocalVar(ASYNC_RESULT_SLOT_IDENTIFIER);
    }

    public JSFrameSlot getAsyncContextSlot() {
        return declareLocalVar(ASYNC_CONTEXT_SLOT_IDENTIFIER);
    }

    public JSFrameSlot getYieldResultSlot() {
        return declareLocalVar(YIELD_RESULT_SLOT_IDENTIFIER);
    }

    public JSFrameSlot getOrCreateBlockScopeSlot() {
        if (blockScopeSlot == null) {
            blockScopeSlot = declareLocalVar(ScopeFrameNode.BLOCK_SCOPE_IDENTIFIER);
        }
        return blockScopeSlot;
    }

    public JSFrameSlot getBlockScopeSlot() {
        return blockScopeSlot;
    }

    @Override
    public JSFrameSlot getCurrentBlockScopeSlot() {
        return null;
    }

    public boolean isEval() {
        return isEval;
    }

    public boolean isArrowFunction() {
        return isArrowFunction;
    }

    public boolean isGeneratorFunction() {
        return isGeneratorFunction;
    }

    @Override
    public JSFrameDescriptor getBlockFrameDescriptor() {
        return getFunctionFrameDescriptor();
    }

    @Override
    protected JSFrameSlot findBlockFrameSlot(Object name) {
        return getFunctionFrameDescriptor().findFrameSlot(name);
    }

    private <T extends BreakTarget> T pushJumpTarget(T target) {
        if (jumpTargetStack == null) {
            jumpTargetStack = new ArrayList<>(4);
        }
        jumpTargetStack.add(target);
        return target;
    }

    private void popJumpTarget(BreakTarget target) {
        assert jumpTargetStack != null && jumpTargetStack.get(jumpTargetStack.size() - 1) == target;
        jumpTargetStack.remove(jumpTargetStack.size() - 1);
    }

    public JumpTargetCloseable<ContinueTarget> pushContinueTarget(Object label) {
        ContinueTarget target = ContinueTarget.forLoop(label, -1);
        pushJumpTarget(target);
        return new JumpTargetCloseable<>(target);
    }

    public JumpTargetCloseable<BreakTarget> pushBreakTarget(Object label) {
        BreakTarget target = label == null ? BreakTarget.forSwitch() : BreakTarget.forLabel(label, -1);
        pushJumpTarget(target);
        return new JumpTargetCloseable<>(target);
    }

    public BreakTarget findBreakTarget(Object label) {
        breakNodeCount++;
        return findJumpTarget(label, BreakTarget.class, true);
    }

    public ContinueTarget findContinueTarget(Object label) {
        continueNodeCount++;
        return findJumpTarget(label, ContinueTarget.class, false);
    }

    private <T extends BreakTarget> T findJumpTarget(Object label, Class<T> targetClass, boolean direct) {
        T applicableTarget = null;
        for (ListIterator<BreakTarget> iterator = jumpTargetStack.listIterator(jumpTargetStack.size()); iterator.hasPrevious();) {
            BreakTarget target = iterator.previous();
            if (direct && label == null) {
                // break without a label is consumed by a switch or an iteration statement,
                // not by other labelled statements
                if ((BreakTarget.forSwitch() == target) || (target instanceof ContinueTarget)) {
                    return targetClass.cast(target);
                }
            } else if (direct || label == null) {
                // ignore label or label directly on target
                if (label == null || label.equals(target.getLabel())) {
                    if (targetClass.isInstance(target)) {
                        return targetClass.cast(target);
                    }
                }
            } else {
                assert !direct;
                // label is indirectly associated with last applicable target
                if (targetClass.isInstance(target)) {
                    applicableTarget = targetClass.cast(target);
                }
                if (label.equals(target.getLabel())) {
                    assert applicableTarget != null : "Illegal or duplicate label"; // SyntaxError
                    return applicableTarget;
                }
            }
        }
        throw new NoSuchElementException("jump target not found");
    }

    public boolean hasReturn() {
        return hasReturn;
    }

    public void addReturn() {
        hasReturn = true;
    }

    public boolean hasAwait() {
        return hasAwait;
    }

    public void addAwait() {
        hasAwait = true;
    }

    public boolean hasYield() {
        return hasYield;
    }

    public void addYield() {
        hasYield = true;
    }

    public void setDirectArgumentsAccess(boolean directArgumentsAccess) {
        this.directArgumentsAccess = directArgumentsAccess;
    }

    public boolean isDirectArgumentsAccess() {
        return directArgumentsAccess;
    }

    public void addMappedParameter(JSFrameSlot slot, int index) {
        assert slot != null && JSFrameUtil.isParam(slot) : slot;
        if (parameters == null) {
            parameters = EconomicMap.create();
        }
        parameters.put(slot, index);
    }

    protected int getMappedParameterIndex(JSFrameSlot slot) {
        return parameters.get(slot, -1);
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getInternalFunctionName() {
        return internalFunctionName;
    }

    public void setInternalFunctionName(String internalFunctionName) {
        this.internalFunctionName = internalFunctionName;
    }

    public void setNamedFunctionExpression(boolean isNamedExpression) {
        this.isNamedExpression = isNamedExpression;
    }

    protected boolean isNamedFunctionExpression() {
        return isNamedExpression;
    }

    public boolean needsParentFrame() {
        return needsParentFrame;
    }

    public void setNeedsParentFrame(boolean needsParentFrame) {
        if (frozen && needsParentFrame != this.needsParentFrame) {
            throw errorFrozenEnv();
        }
        this.needsParentFrame = needsParentFrame;
    }

    private static RuntimeException errorFrozenEnv() {
        return new IllegalStateException("frozen function environment cannot be mutated");
    }

    public void freeze() {
        if (this.frozen) {
            return;
        }
        this.frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isDeepFrozen() {
        return isFrozen() && (getParentFunction() == null || getParentFunction().isDeepFrozen());
    }

    public boolean hasMappedParameters() {
        return parameters != null;
    }

    @Override
    public JSFrameDescriptor getFunctionFrameDescriptor() {
        return frameDescriptor;
    }

    @Override
    public boolean isStrictMode() {
        return this.isStrictMode;
    }

    public FunctionEnvironment getParentFunction() {
        return parentFunction;
    }

    public FunctionEnvironment getParentFunction(int level) {
        assert level >= 0;
        if (level == 0) {
            return this;
        } else {
            return parentFunction.getParentFunction(level - 1);
        }
    }

    public FunctionEnvironment getNonArrowParentFunction() {
        if (isArrowFunction() || isDirectEval()) {
            return getParentFunction().getNonArrowParentFunction();
        }
        return this;
    }

    @Override
    public int getScopeLevel() {
        return 0;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean hasSyntheticArguments() {
        return this.hasSyntheticArguments;
    }

    public boolean returnsLastStatementResult() {
        return isGlobal() || isDirectEval() || hasSyntheticArguments();
    }

    public void setIsDynamicallyScoped(boolean isDynamicallyScoped) {
        this.isDynamicallyScoped = isDynamicallyScoped;
    }

    /**
     * Function is dynamically scope because it's non-strict and has a direct eval call.
     */
    @Override
    public boolean isDynamicallyScoped() {
        return isDynamicallyScoped;
    }

    @Override
    public boolean isDynamicScopeContext() {
        return isDynamicallyScoped() || isCallerContextEval() || super.isDynamicScopeContext();
    }

    @Override
    public Environment getVariableEnvironment() {
        if (isCallerContextEval()) {
            return getParentFunction().getVariableEnvironment();
        }
        return this;
    }

    public class JumpTargetCloseable<T extends BreakTarget> implements AutoCloseable {

        private final T target;
        private final int prevBreakCount = breakNodeCount;
        private final int prevContinueCount = continueNodeCount;

        protected JumpTargetCloseable(T target) {
            this.target = target;
        }

        public T getTarget() {
            return target;
        }

        @Override
        public void close() {
            popJumpTarget(target);
        }

        private boolean hasBreak() {
            return breakNodeCount != prevBreakCount;
        }

        private boolean hasContinue() {
            return continueNodeCount != prevContinueCount;
        }

        public JavaScriptNode wrapContinueTargetNode(JavaScriptNode child) {
            boolean hasContinue = hasContinue();
            return hasContinue ? factory.createContinueTarget(child, (ContinueTarget) target) : child;
        }

        public JavaScriptNode wrapBreakTargetNode(JavaScriptNode child) {
            assert target.getLabel() == null;
            boolean hasBreak = hasBreak();
            return hasBreak ? factory.createDirectBreakTarget(child) : child;
        }

        public JavaScriptNode wrapLabelBreakTargetNode(JavaScriptNode child) {
            assert target.getLabel() != null;
            boolean hasBreak = hasBreak();
            return hasBreak ? factory.createLabel(child, target) : child;
        }
    }

    public boolean isDirectEval() {
        return isDirectEval;
    }

    public boolean isIndirectEval() {
        return isEval() && !isDirectEval();
    }

    public boolean isCallerContextEval() {
        return isDirectEval() && !isStrictMode() && !isGlobal();
    }

    public boolean inDirectEval() {
        return inDirectEval;
    }

    public void setNeedsNewTarget(boolean needsNewTarget) {
        this.needsNewTarget = needsNewTarget;
    }

    public void setRestParameter(boolean restParameter) {
        this.hasRestParameter = restParameter;
    }

    public boolean hasRestParameter() {
        return hasRestParameter;
    }

    public void setSimpleParameterList(boolean simpleParameterList) {
        this.simpleParameterList = simpleParameterList;
    }

    public boolean hasSimpleParameterList() {
        return simpleParameterList;
    }

    public int getLeadingArgumentCount() {
        return needsNewTarget ? 1 : 0;
    }

    public boolean isDerivedConstructor() {
        return isDerivedConstructor;
    }

    /**
     * Returns the number of function levels to skip to reach the function with the [[ThisValue]].
     * Loosely resembles GetThisEnvironment(), but we need to consider eval functions, too.
     */
    public int getThisFunctionLevel() {
        int level = 0;
        for (FunctionEnvironment currentFunction = this; currentFunction.isArrowFunction() || currentFunction.isDirectEval(); currentFunction = currentFunction.getParentFunction(), level++) {
            currentFunction.setNeedsParentFrame(true);
        }
        return level;
    }

    /**
     * Returns the number of frame levels to skip to reach the outermost function (module/script).
     */
    public int getOutermostFunctionLevel() {
        int level = 0;
        for (FunctionEnvironment currentFunction = this; currentFunction.getParentFunction() != null; currentFunction = currentFunction.getParentFunction(), level++) {
            currentFunction.setNeedsParentFrame(true);
        }
        return level;
    }

    public boolean isAsyncFunction() {
        return isAsyncFunction;
    }

    public boolean isAsyncGeneratorFunction() {
        return isAsyncFunction && isGeneratorFunction;
    }

    @Override
    protected String toStringImpl(Map<String, Integer> state) {
        int currentFrameLevel = state.getOrDefault("frameLevel", 0);
        state.put("frameLevel", currentFrameLevel + 1);
        state.put("scopeLevel", 0);
        return "Function(" + currentFrameLevel + ")" +
                        " size=" + getFunctionFrameDescriptor().getSize() + " " + joinElements(getFunctionFrameDescriptor().getIdentifiers());
    }
}
