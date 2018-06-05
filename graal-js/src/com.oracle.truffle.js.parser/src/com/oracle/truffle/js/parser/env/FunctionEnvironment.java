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
package com.oracle.truffle.js.parser.env;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;

public class FunctionEnvironment extends Environment {
    private static final String RETURN_SLOT_IDENTIFIER = "<return>";
    static final String ARGUMENTS_SLOT_IDENTIFIER = "<arguments>";
    static final String THIS_SLOT_IDENTIFIER = "<this>";
    static final String SUPER_SLOT_IDENTIFIER = "<super>";
    static final String NEW_TARGET_SLOT_IDENTIFIER = "<new.target>";
    static final String YIELD_VALUE_SLOT_IDENTIFIER = "<yieldvalue>";
    static final String ASYNC_CONTEXT_SLOT_IDENTIFIER = "<asynccontext>";
    static final String ASYNC_RESULT_SLOT_IDENTIFIER = "<asyncresult>";
    private static final String YIELD_RESULT_SLOT_IDENTIFIER = "<yieldresult>";
    static final String DYNAMIC_SCOPE_IDENTIFIER = "<evalscope>";

    private final FunctionEnvironment parent;
    private final FrameDescriptor frameDescriptor;
    private final List<FrameSlot> parameters;
    private final boolean isStrictMode;

    private FrameSlot argumentsSlot;
    private FrameSlot returnSlot;
    private FrameSlot thisSlot;
    private FrameSlot superSlot;
    private FrameSlot newTargetSlot;
    private FrameSlot dynamicScopeSlot;

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
    private boolean hasRestParameter;
    private boolean simpleParameterList = true;
    private boolean isDynamicallyScoped;
    private final boolean inDirectEval;

    private Map<String, ImportBindingRef> importBindings;

    public FunctionEnvironment(Environment parent, NodeFactory factory, JSContext context,
                    boolean isStrictMode, boolean isEval, boolean isDirectEval, boolean isArrowFunction, boolean isGeneratorFunction, boolean isDerivedConstructor, boolean isAsyncFunction) {
        super(parent, factory, context);
        this.isDirectEval = isDirectEval;
        this.isAsyncFunction = isAsyncFunction;
        this.isStrictMode = isStrictMode || (parent != null && parent.isStrictMode());
        this.isEval = isEval;
        this.isArrowFunction = isArrowFunction;
        this.isGeneratorFunction = isGeneratorFunction;
        this.isDerivedConstructor = isDerivedConstructor;
        this.parent = parent == null ? null : parent.function();

        this.frameDescriptor = factory.createFrameDescriptor();
        this.parameters = new ArrayList<>();
        this.isGlobal = parent == null || isDirectEval && (!isStrictMode && parent.function().isGlobal());
        this.inDirectEval = isDirectEval || (parent != null && parent.function() != null && parent.function().inDirectEval());
    }

    @Override
    public FunctionEnvironment function() {
        return this;
    }

    @Override
    public FrameSlot declareLocalVar(Object name) {
        return getFunctionFrameDescriptor().findOrAddFrameSlot(name, FrameSlotKind.Illegal);
    }

    @Override
    public FrameSlot declareVar(Object name) {
        if (isCallerContextEval()) {
            return getParentFunction().declareVar(name);
        }
        if (frozen && getFunctionFrameDescriptor().findFrameSlot(name) == null) {
            throw errorFrozenEnv();
        }
        return declareLocalVar(name);
    }

    public FrameSlot reserveArgumentsSlot() {
        if (argumentsSlot == null) {
            argumentsSlot = declareVar(ARGUMENTS_SLOT_IDENTIFIER);
        }
        return argumentsSlot;
    }

    public FrameSlot getReturnSlot() {
        if (returnSlot == null) {
            returnSlot = declareLocalVar(RETURN_SLOT_IDENTIFIER);
        }
        return returnSlot;
    }

    public void reserveThisSlot() {
        if (thisSlot == null) {
            thisSlot = declareVar(THIS_SLOT_IDENTIFIER);
        }
    }

    public void reserveNewTargetSlot() {
        if (newTargetSlot == null) {
            newTargetSlot = declareVar(NEW_TARGET_SLOT_IDENTIFIER);
        }
    }

    public FrameSlot getAsyncResultSlot() {
        return declareLocalVar(ASYNC_RESULT_SLOT_IDENTIFIER);
    }

    public FrameSlot getAsyncContextSlot() {
        return declareLocalVar(ASYNC_CONTEXT_SLOT_IDENTIFIER);
    }

    public FrameSlot getYieldResultSlot() {
        return declareLocalVar(YIELD_RESULT_SLOT_IDENTIFIER);
    }

    public FrameSlot getThisSlot() {
        return thisSlot;
    }

    public FrameSlot reserveDynamicScopeSlot() {
        if (dynamicScopeSlot == null) {
            dynamicScopeSlot = declareLocalVar(DYNAMIC_SCOPE_IDENTIFIER);
        }
        return dynamicScopeSlot;
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
    protected FrameSlot findBlockFrameSlot(String name) {
        return getFunctionFrameDescriptor().findFrameSlot(name);
    }

    @Override
    public boolean hasLocalVar(String name) {
        return getFunctionFrameDescriptor().getIdentifiers().contains(name);
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
            if (direct || label == null) {
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

    public final int getParameterCount() {
        return parameters.size();
    }

    public final List<FrameSlot> getParameters() {
        return parameters;
    }

    public final void declareParameter(String name) {
        parameters.add(declareLocalVar(name));
    }

    protected final boolean isParam(FrameSlot slot) {
        return parameters.contains(slot);
    }

    protected final int getParameterIndex(FrameSlot slot) {
        for (int i = parameters.size() - 1; i >= 0; --i) {
            if (parameters.get(i) == slot) {
                return i;
            }
        }
        return -1;
    }

    public final boolean isParameter(String name) {
        for (int i = 0; i < parameters.size(); i++) {
            if (name.equals(parameters.get(i).getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    public final String getFunctionName() {
        return functionName;
    }

    public final void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public final String getInternalFunctionName() {
        return internalFunctionName;
    }

    public final void setInternalFunctionName(String internalFunctionName) {
        this.internalFunctionName = internalFunctionName;
    }

    public final void setNamedFunctionExpression(boolean isNamedExpression) {
        this.isNamedExpression = isNamedExpression;
    }

    protected final boolean isNamedFunctionExpression() {
        return isNamedExpression;
    }

    public final boolean needsParentFrame() {
        return needsParentFrame;
    }

    public final void setNeedsParentFrame(boolean needsParentFrame) {
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

    public final boolean hasArgumentsSlot() {
        return argumentsSlot != null;
    }

    protected final FrameSlot getArgumentsSlot() {
        return argumentsSlot;
    }

    @Override
    public final FrameDescriptor getFunctionFrameDescriptor() {
        return frameDescriptor;
    }

    @Override
    public final boolean isStrictMode() {
        return this.isStrictMode;
    }

    public final FunctionEnvironment getParentFunction() {
        return parent;
    }

    public final FunctionEnvironment getParentFunction(int level) {
        assert level >= 0;
        if (level == 0) {
            return this;
        } else {
            return parent.getParentFunction(level - 1);
        }
    }

    public final FunctionEnvironment getNonArrowParentFunction() {
        if (!isArrowFunction()) {
            return this;
        }
        return getParentFunction(getArrowFunctionLevel());
    }

    @Override
    public FrameSlot[] getParentSlots() {
        return ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean returnsLastStatementResult() {
        return isGlobal() || isDirectEval();
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

    public void reserveSuperSlot() {
        if (superSlot == null) {
            superSlot = declareVar(SUPER_SLOT_IDENTIFIER);
        }
    }

    public FrameSlot getSuperSlot() {
        return superSlot;
    }

    public FrameSlot getNewTargetSlot() {
        return newTargetSlot;
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
        return getNewTargetSlot() != null ? 1 : 0;
    }

    public int getTrailingArgumentCount() {
        return 0;
    }

    public boolean isDerivedConstructor() {
        return isDerivedConstructor;
    }

    public int getArrowFunctionLevel() {
        return isArrowFunction ? 1 + getParentFunction().getArrowFunctionLevel() : 0;
    }

    public boolean isAsyncFunction() {
        return isAsyncFunction;
    }

    public boolean isAsyncGeneratorFunction() {
        return isAsyncFunction && isGeneratorFunction;
    }

    public void addImportBinding(String localName, JSModuleRecord module, String bindingName) {
        Map<String, ImportBindingRef> map = importBindings;
        if (map == null) {
            importBindings = map = new HashMap<>();
        }
        map.put(localName, new ImportBindingRef(localName, module, bindingName));
    }

    public VarRef getImportBinding(String localName) {
        Map<String, ImportBindingRef> map = importBindings;
        return map != null ? map.get(localName) : null;
    }
}
