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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSAddNode;
import com.oracle.truffle.js.nodes.binary.JSSubtractNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.nodes.unary.JSOverloadedUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;

public abstract class LocalVarIncNode extends FrameSlotNode.WithDescriptor {
    public enum Op {
        Inc(new IncOp()),
        Dec(new DecOp());

        public final LocalVarOp op;

        Op(LocalVarOp op) {
            this.op = op;
        }
    }

    abstract static class LocalVarOp {
        public abstract int doInt(int value);

        public abstract double doDouble(double value);

        public abstract Number doNumber(Number value, ConditionProfile isIntegerProfile, ConditionProfile isBoundaryValue);

        public abstract BigInt doBigInt(BigInt value);

        public abstract SafeInteger doSafeInteger(SafeInteger value);

        public abstract String getOverloadedOperatorName();
    }

    protected static class IncOp extends LocalVarOp {
        @Override
        public int doInt(int value) {
            return Math.addExact(value, 1);
        }

        @Override
        public double doDouble(double value) {
            return value + 1d;
        }

        @Override
        public Number doNumber(Number numValue, ConditionProfile isIntegerProfile, ConditionProfile isBoundaryValue) {
            if (isIntegerProfile.profile(numValue instanceof Integer)) {
                int intValue = (Integer) numValue;
                if (isBoundaryValue.profile(intValue != Integer.MAX_VALUE)) {
                    return intValue + 1;
                } else {
                    return intValue + 1d;
                }
            } else {
                double doubleValue = JSRuntime.doubleValue(numValue);
                return doubleValue + 1d;
            }
        }

        @Override
        public BigInt doBigInt(BigInt value) {
            return value.add(BigInt.ONE);
        }

        @Override
        public SafeInteger doSafeInteger(SafeInteger value) {
            return value.incrementExact();
        }

        @Override
        public String getOverloadedOperatorName() {
            return "++";
        }
    }

    protected static class DecOp extends LocalVarOp {
        @Override
        public int doInt(int value) {
            return Math.subtractExact(value, 1);
        }

        @Override
        public double doDouble(double value) {
            return value - 1d;
        }

        @Override
        public Number doNumber(Number numValue, ConditionProfile isIntegerProfile, ConditionProfile isBoundaryValue) {
            if (isIntegerProfile.profile(numValue instanceof Integer)) {
                int intValue = (Integer) numValue;
                if (isBoundaryValue.profile(intValue != Integer.MIN_VALUE)) {
                    return intValue - 1;
                } else {
                    return intValue - 1d;
                }
            } else {
                double doubleValue = JSRuntime.doubleValue(numValue);
                return doubleValue - 1d;
            }
        }

        @Override
        public BigInt doBigInt(BigInt value) {
            return value.subtract(BigInt.ONE);
        }

        @Override
        public SafeInteger doSafeInteger(SafeInteger value) {
            return value.decrementExact();
        }

        @Override
        public String getOverloadedOperatorName() {
            return "--";
        }
    }

    protected final LocalVarOp op;
    protected final boolean hasTemporalDeadZone;
    @Child @Executed protected ScopeFrameNode scopeFrameNode;

    protected LocalVarIncNode(LocalVarOp op, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        super(frameSlot);
        this.op = op;
        this.hasTemporalDeadZone = hasTemporalDeadZone;
        this.scopeFrameNode = scopeFrameNode;
    }

    public static LocalVarIncNode createPrefix(Op op, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        return LocalVarPrefixIncNodeGen.create(op.op, frameSlot, hasTemporalDeadZone, scopeFrameNode);
    }

    public static LocalVarIncNode createPostfix(Op op, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        return LocalVarPostfixIncNodeGen.create(op.op, frameSlot, hasTemporalDeadZone, scopeFrameNode);
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return hasTemporalDeadZone;
    }

    @Override
    public final ScopeFrameNode getLevelFrameNode() {
        return scopeFrameNode;
    }
}

abstract class LocalVarOpMaterializedNode extends LocalVarIncNode {

    @Child protected JavaScriptNode convertOld;
    @Child protected JavaScriptNode writeNew;

    LocalVarOpMaterializedNode(LocalVarIncNode from, Set<Class<? extends Tag>> materializedTags) {
        super(from.op, from.frameSlot, from.hasTemporalDeadZone, from.scopeFrameNode);

        JavaScriptNode readOld = JSReadFrameSlotNode.create(frameSlot, scopeFrameNode, hasTemporalDeadZone);
        JavaScriptNode convert = (JavaScriptNode) JSToNumericNode.createToNumericOperand(readOld).materializeInstrumentableNodes(materializedTags);
        convertOld = cloneUninitialized(JSWriteFrameSlotNode.create(frameSlot, scopeFrameNode, convert, hasTemporalDeadZone), materializedTags);

        JavaScriptNode readTmp = JSReadFrameSlotNode.create(frameSlot, scopeFrameNode, hasTemporalDeadZone);
        JavaScriptNode one = JSConstantNode.createConstantNumericUnit();
        JavaScriptNode opNode;
        if (from.op instanceof DecOp) {
            opNode = JSSubtractNode.create(readTmp, one);
        } else {
            opNode = JSAddNode.create(readTmp, one);
        }
        /*
         * Have to transfer source sections before cloning and materialization. Some nodes might
         * become instrumentable by this operation.
         */
        transferSourceSectionAddExpressionTag(from, readTmp);
        transferSourceSectionAddExpressionTag(from, one);
        transferSourceSectionAddExpressionTag(from, opNode);
        this.writeNew = cloneUninitialized(JSWriteFrameSlotNode.create(frameSlot, scopeFrameNode, opNode, hasTemporalDeadZone), materializedTags);
        transferSourceSectionAddExpressionTag(from, writeNew);
        transferSourceSectionAndTags(from, this);
    }

    LocalVarOpMaterializedNode(LocalVarOp op, JSFrameSlot slot, boolean hasTdz, ScopeFrameNode scope, JavaScriptNode convert, JavaScriptNode write) {
        super(op, slot, hasTdz, scope);
        this.convertOld = convert;
        this.writeNew = write;
    }
}

class LocalVarPostfixIncMaterializedNode extends LocalVarOpMaterializedNode {

    LocalVarPostfixIncMaterializedNode(LocalVarOp op, JSFrameSlot slot, boolean hasTdz, ScopeFrameNode scope, JavaScriptNode read, JavaScriptNode write) {
        super(op, slot, hasTdz, scope, read, write);
    }

    LocalVarPostfixIncMaterializedNode(LocalVarPostfixIncNode from, Set<Class<? extends Tag>> materializedTags) {
        super(from, materializedTags);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = convertOld.execute(frame);
        writeNew.execute(frame);
        return value;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new LocalVarPostfixIncMaterializedNode(op, frameSlot, hasTemporalDeadZone(), scopeFrameNode, cloneUninitialized(convertOld, materializedTags),
                        cloneUninitialized(writeNew, materializedTags));
    }
}

class LocalVarPrefixIncMaterializedNode extends LocalVarOpMaterializedNode {

    LocalVarPrefixIncMaterializedNode(LocalVarOp op, JSFrameSlot slot, boolean hasTdz, ScopeFrameNode scope, JavaScriptNode read, JavaScriptNode write) {
        super(op, slot, hasTdz, scope, read, write);
    }

    LocalVarPrefixIncMaterializedNode(LocalVarPrefixIncNode from, Set<Class<? extends Tag>> materializedTags) {
        super(from, materializedTags);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        convertOld.execute(frame);
        return writeNew.execute(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new LocalVarPrefixIncMaterializedNode(op, frameSlot, hasTemporalDeadZone(), scopeFrameNode, cloneUninitialized(convertOld, materializedTags),
                        cloneUninitialized(writeNew, materializedTags));
    }

}

abstract class LocalVarPostfixIncNode extends LocalVarIncNode {

    protected LocalVarPostfixIncNode(LocalVarOp op, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        super(op, frameSlot, hasTemporalDeadZone, scopeFrameNode);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadVariableTag.class) ||
                        materializedTags.contains(WriteVariableTag.class) ||
                        materializedTags.contains(StandardTags.ReadVariableTag.class) ||
                        materializedTags.contains(StandardTags.WriteVariableTag.class)) {
            return new LocalVarPostfixIncMaterializedNode(this, materializedTags);
        } else {
            return this;
        }
    }

    @Specialization(guards = {"isBoolean(frame)", "isIntegerKind(frame)"})
    public int doBoolean(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setInt(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isBoolean(frame)", "isDoubleKind(frame)"}, replaces = "doBoolean")
    public int doBooleanDouble(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isBoolean(frame)"}, replaces = "doBooleanDouble")
    public int doBooleanObject(Frame frame) {
        ensureObjectKind(frame);
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setObject(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)", "isIntegerKind(frame)"}, rewriteOn = {ArithmeticException.class})
    public int doInt(Frame frame) {
        int value = getInt(frame);
        int newValue = op.doInt(value);
        frame.setInt(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)", "isDoubleKind(frame)"}, replaces = "doInt")
    public int doIntDouble(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)"}, replaces = "doIntDouble")
    public int doIntObject(Frame frame) {
        ensureObjectKind(frame);
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setObject(frameSlot.getIndex(), newValue);
        return value;
    }

    @Specialization(guards = {"isDouble(frame)", "isDoubleKind(frame)"})
    public double doDouble(Frame frame) {
        double doubleValue = getDouble(frame);
        frame.setDouble(frameSlot.getIndex(), op.doDouble(doubleValue));
        return doubleValue;
    }

    @Specialization(guards = {"isDouble(frame)"}, replaces = "doDouble")
    public double doDoubleObject(Frame frame) {
        ensureObjectKind(frame);
        double doubleValue = getDouble(frame);
        frame.setObject(frameSlot.getIndex(), op.doDouble(doubleValue));
        return doubleValue;
    }

    protected String getOverloadedOperatorName() {
        return op.getOverloadedOperatorName();
    }

    @Specialization(guards = {"isObject(frame)"})
    public Object doObject(Frame frame,
                    @Cached("createBinaryProfile()") ConditionProfile isNumberProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isIntegerProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBigIntProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBoundaryProfile,
                    @Cached("create(getOverloadedOperatorName())") JSOverloadedUnaryNode overloadedOperatorNode,
                    @Cached("createToNumericOperand()") JSToNumericNode toNumericOperand,
                    @Cached("create()") BranchProfile deadBranch) {
        ensureObjectKind(frame);
        Object value = getObject(frame);
        if (hasTemporalDeadZone()) {
            checkNotDead(value, deadBranch);
        }
        Object operand = toNumericOperand.execute(value);
        if (isNumberProfile.profile(operand instanceof Number)) {
            frame.setObject(frameSlot.getIndex(), op.doNumber((Number) operand, isIntegerProfile, isBoundaryProfile));
        } else if (isBigIntProfile.profile(operand instanceof BigInt)) {
            frame.setObject(frameSlot.getIndex(), op.doBigInt((BigInt) operand));
        } else {
            assert JSRuntime.isObject(operand) && JSOverloadedOperatorsObject.hasOverloadedOperators(operand);
            frame.setObject(frameSlot.getIndex(), overloadedOperatorNode.execute(value));
        }
        return operand;
    }

    @Specialization(guards = {"isLong(frame)", "isLongKind(frame)"}, rewriteOn = ArithmeticException.class)
    public SafeInteger doSafeInteger(Frame frame) {
        SafeInteger oldValue = SafeInteger.valueOf(getLong(frame));
        SafeInteger newValue = op.doSafeInteger(oldValue);
        frame.setLong(frameSlot.getIndex(), newValue.longValue());
        return oldValue;
    }

    @Specialization(guards = {"isLong(frame)", "isDoubleKind(frame)"}, replaces = "doSafeInteger")
    public double doSafeIntegerToDouble(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return oldValue;
    }

    @Specialization(guards = {"isLong(frame)"}, replaces = "doSafeIntegerToDouble")
    public double doSafeIntegerObject(Frame frame) {
        ensureObjectKind(frame);
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setObject(frameSlot.getIndex(), newValue);
        return oldValue;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return LocalVarPostfixIncNodeGen.create(op, frameSlot, hasTemporalDeadZone(), getLevelFrameNode());
    }
}

abstract class LocalVarPrefixIncNode extends LocalVarIncNode {

    protected LocalVarPrefixIncNode(LocalVarOp op, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        super(op, frameSlot, hasTemporalDeadZone, scopeFrameNode);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadVariableTag.class) ||
                        materializedTags.contains(WriteVariableTag.class) ||
                        materializedTags.contains(StandardTags.ReadVariableTag.class) ||
                        materializedTags.contains(StandardTags.WriteVariableTag.class)) {
            return new LocalVarPrefixIncMaterializedNode(this, materializedTags);
        } else {
            return this;
        }
    }

    @Specialization(guards = {"isBoolean(frame)", "isIntegerKind(frame)"})
    public int doBoolean(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setInt(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isBoolean(frame)", "isDoubleKind(frame)"}, replaces = "doBoolean")
    public int doBooleanDouble(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isBoolean(frame)"}, replaces = "doBooleanDouble")
    public int doBooleanObject(Frame frame) {
        ensureObjectKind(frame);
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setObject(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)", "isIntegerKind(frame)"}, rewriteOn = {ArithmeticException.class})
    public int doInt(Frame frame) {
        int value = getInt(frame);
        int newValue = op.doInt(value);
        frame.setInt(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)", "isDoubleKind(frame)"}, replaces = "doInt")
    public double doIntOverflow(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)"}, replaces = "doIntOverflow")
    public double doIntOverflowObject(Frame frame) {
        ensureObjectKind(frame);
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setObject(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isDouble(frame)", "isDoubleKind(frame)"})
    public double doDouble(Frame frame) {
        double doubleValue = getDouble(frame);
        double newValue = op.doDouble(doubleValue);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isDouble(frame)"}, replaces = "doDouble")
    public double doDoubleObject(Frame frame) {
        ensureObjectKind(frame);
        double doubleValue = getDouble(frame);
        double newValue = op.doDouble(doubleValue);
        frame.setObject(frameSlot.getIndex(), newValue);
        return newValue;
    }

    protected String getOverloadedOperatorName() {
        return op.getOverloadedOperatorName();
    }

    @Specialization(guards = {"isObject(frame)"})
    public Object doObject(Frame frame,
                    @Cached("createBinaryProfile()") ConditionProfile isNumberProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isIntegerProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBigIntProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBoundaryProfile,
                    @Cached("create(getOverloadedOperatorName())") JSOverloadedUnaryNode overloadedOperatorNode,
                    @Cached("createToNumericOperand()") JSToNumericNode toNumericOperand,
                    @Cached("create()") BranchProfile deadBranch) {
        ensureObjectKind(frame);
        Object value = getObject(frame);
        if (hasTemporalDeadZone()) {
            checkNotDead(value, deadBranch);
        }
        Object operand = toNumericOperand.execute(value);
        Object newValue;
        if (isNumberProfile.profile(operand instanceof Number)) {
            newValue = op.doNumber((Number) operand, isIntegerProfile, isBoundaryProfile);
        } else if (isBigIntProfile.profile(operand instanceof BigInt)) {
            newValue = op.doBigInt((BigInt) operand);
        } else {
            assert JSRuntime.isObject(operand) && JSOverloadedOperatorsObject.hasOverloadedOperators(operand);
            newValue = overloadedOperatorNode.execute(value);
        }
        frame.setObject(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)", "isLongKind(frame)"}, rewriteOn = ArithmeticException.class)
    public SafeInteger doSafeInteger(Frame frame) {
        SafeInteger oldValue = SafeInteger.valueOf(getLong(frame));
        SafeInteger newValue = op.doSafeInteger(oldValue);
        frame.setLong(frameSlot.getIndex(), newValue.longValue());
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)", "isDoubleKind(frame)"}, replaces = "doSafeInteger")
    public double doSafeIntegerToDouble(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setDouble(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)"}, replaces = "doSafeIntegerToDouble")
    public double doSafeIntegerToObject(Frame frame) {
        ensureObjectKind(frame);
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setObject(frameSlot.getIndex(), newValue);
        return newValue;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return LocalVarPrefixIncNodeGen.create(op, frameSlot, hasTemporalDeadZone(), getLevelFrameNode());
    }
}
