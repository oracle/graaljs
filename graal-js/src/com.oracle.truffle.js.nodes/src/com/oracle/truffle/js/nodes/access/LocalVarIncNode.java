/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;

@NodeChild(value = "levelFrameNode", type = ScopeFrameNode.class)
public abstract class LocalVarIncNode extends FrameSlotNode {
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

        public abstract LargeInteger doLargeInteger(LargeInteger value);
    }

    private static class IncOp extends LocalVarOp {
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
        public LargeInteger doLargeInteger(LargeInteger value) {
            return value.incrementExact();
        }
    }

    private static class DecOp extends LocalVarOp {
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
        public LargeInteger doLargeInteger(LargeInteger value) {
            return value.decrementExact();
        }
    }

    protected final LocalVarOp op;
    private final boolean hasTemporalDeadZone;

    protected LocalVarIncNode(LocalVarOp op, FrameSlot frameSlot, boolean hasTemporalDeadZone) {
        super(frameSlot);
        this.op = op;
        this.hasTemporalDeadZone = hasTemporalDeadZone;
    }

    public static LocalVarIncNode createPrefix(Op op, FrameSlotNode frameSlotNode) {
        return LocalVarPrefixIncNodeGen.create(op.op, frameSlotNode.getFrameSlot(), frameSlotNode.hasTemporalDeadZone(), frameSlotNode.getLevelFrameNode());
    }

    public static LocalVarIncNode createPostfix(Op op, FrameSlotNode frameSlotNode) {
        return LocalVarPostfixIncNodeGen.create(op.op, frameSlotNode.getFrameSlot(), frameSlotNode.hasTemporalDeadZone(), frameSlotNode.getLevelFrameNode());
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return hasTemporalDeadZone;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }
}

abstract class LocalVarPostfixIncNode extends LocalVarIncNode {

    protected LocalVarPostfixIncNode(LocalVarOp op, FrameSlot frameSlot, boolean hasTemporalDeadZone) {
        super(op, frameSlot, hasTemporalDeadZone);
    }

    @Specialization(guards = {"isBoolean(frame)", "isIntegerKind(frame)"})
    public int doBoolean(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setInt(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isBoolean(frame)", "isDoubleKind(frame)"}, replaces = "doBoolean")
    public int doBooleanDouble(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setDouble(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isBoolean(frame)", "ensureObjectKind(frame)"}, replaces = "doBooleanDouble")
    public int doBooleanObject(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setObject(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)", "isIntegerKind(frame)"}, rewriteOn = {ArithmeticException.class})
    public int doInt(Frame frame) {
        int value = getInt(frame);
        int newValue = op.doInt(value);
        frame.setInt(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)", "isDoubleKind(frame)"}, replaces = "doInt")
    public int doIntDouble(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setDouble(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isInt(frame)", "ensureObjectKind(frame)"}, replaces = "doIntDouble")
    public int doIntObject(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setObject(frameSlot, newValue);
        return value;
    }

    @Specialization(guards = {"isDouble(frame)", "isDoubleKind(frame)"})
    public double doDouble(Frame frame) {
        double doubleValue = getDouble(frame);
        frame.setDouble(frameSlot, op.doDouble(doubleValue));
        return doubleValue;
    }

    @Specialization(guards = {"isDouble(frame)", "ensureObjectKind(frame)"}, replaces = "doDouble")
    public double doDoubleObject(Frame frame) {
        double doubleValue = getDouble(frame);
        frame.setObject(frameSlot, op.doDouble(doubleValue));
        return doubleValue;
    }

    @Specialization(guards = {"isObject(frame)", "ensureObjectKind(frame)"})
    public Object doObject(Frame frame,
                    @Cached("createBinaryProfile()") ConditionProfile isIntegerProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBoundaryProfile,
                    @Cached("create()") JSToNumberNode toNumber,
                    @Cached("create()") BranchProfile deadBranch) {
        Object value = getObject(frame);
        if (hasTemporalDeadZone()) {
            checkNotDead(value, deadBranch);
        }
        Number number = toNumber.executeNumber(value);
        frame.setObject(frameSlot, op.doNumber(number, isIntegerProfile, isBoundaryProfile));
        return number;
    }

    @Specialization(guards = {"isLong(frame)", "isLongKind(frame)"}, rewriteOn = ArithmeticException.class)
    public LargeInteger doLargeInteger(Frame frame) {
        LargeInteger oldValue = LargeInteger.valueOf(getLong(frame));
        LargeInteger newValue = op.doLargeInteger(oldValue);
        frame.setLong(frameSlot, newValue.longValue());
        return oldValue;
    }

    @Specialization(guards = {"isLong(frame)", "isDoubleKind(frame)"}, replaces = "doLargeInteger")
    public double doLargeIntegerToDouble(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setDouble(frameSlot, newValue);
        return oldValue;
    }

    @Specialization(guards = {"isLong(frame)", "ensureObjectKind(frame)"}, replaces = "doLargeIntegerToDouble")
    public double doLargeIntegerObject(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setObject(frameSlot, newValue);
        return oldValue;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return LocalVarPostfixIncNodeGen.create(op, getFrameSlot(), hasTemporalDeadZone(), NodeUtil.cloneNode(getLevelFrameNode()));
    }
}

abstract class LocalVarPrefixIncNode extends LocalVarIncNode {

    protected LocalVarPrefixIncNode(LocalVarOp op, FrameSlot frameSlot, boolean hasTemporalDeadZone) {
        super(op, frameSlot, hasTemporalDeadZone);
    }

    @Specialization(guards = {"isBoolean(frame)", "isIntegerKind(frame)"})
    public int doBoolean(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setInt(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isBoolean(frame)", "isDoubleKind(frame)"}, replaces = "doBoolean")
    public int doBooleanDouble(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setDouble(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isBoolean(frame)", "ensureObjectKind(frame)"}, replaces = "doBooleanDouble")
    public int doBooleanObject(Frame frame) {
        int value = JSRuntime.booleanToNumber(getBoolean(frame));
        int newValue = op.doInt(value);
        frame.setObject(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)", "isIntegerKind(frame)"}, rewriteOn = {ArithmeticException.class})
    public int doInt(Frame frame) {
        int value = getInt(frame);
        int newValue = op.doInt(value);
        frame.setInt(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)", "isDoubleKind(frame)"}, replaces = "doInt")
    public double doIntOverflow(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setDouble(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isInt(frame)", "ensureObjectKind(frame)"}, replaces = "doIntOverflow")
    public double doIntOverflowObject(Frame frame) {
        int value = getInt(frame);
        double newValue = op.doDouble(value);
        frame.setObject(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isDouble(frame)", "isDoubleKind(frame)"})
    public double doDouble(Frame frame) {
        double doubleValue = getDouble(frame);
        double newValue = op.doDouble(doubleValue);
        frame.setDouble(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isDouble(frame)", "ensureObjectKind(frame)"}, replaces = "doDouble")
    public double doDoubleObject(Frame frame) {
        double doubleValue = getDouble(frame);
        double newValue = op.doDouble(doubleValue);
        frame.setObject(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isObject(frame)", "ensureObjectKind(frame)"})
    public Object doObject(Frame frame,
                    @Cached("createBinaryProfile()") ConditionProfile isIntegerProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isBoundaryProfile,
                    @Cached("create()") JSToNumberNode toNumber,
                    @Cached("create()") BranchProfile deadBranch) {
        Object value = getObject(frame);
        if (hasTemporalDeadZone()) {
            checkNotDead(value, deadBranch);
        }
        Number newValue = op.doNumber(toNumber.executeNumber(value), isIntegerProfile, isBoundaryProfile);
        frame.setObject(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)", "isLongKind(frame)"}, rewriteOn = ArithmeticException.class)
    public LargeInteger doLargeInteger(Frame frame) {
        LargeInteger oldValue = LargeInteger.valueOf(getLong(frame));
        LargeInteger newValue = op.doLargeInteger(oldValue);
        frame.setLong(frameSlot, newValue.longValue());
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)", "isDoubleKind(frame)"}, replaces = "doLargeInteger")
    public double doLargeIntegerToDouble(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setDouble(frameSlot, newValue);
        return newValue;
    }

    @Specialization(guards = {"isLong(frame)", "ensureObjectKind(frame)"}, replaces = "doLargeIntegerToDouble")
    public double doLargeIntegerToObject(Frame frame) {
        double oldValue = getLong(frame);
        double newValue = op.doDouble(oldValue);
        frame.setObject(frameSlot, newValue);
        return newValue;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return LocalVarPrefixIncNodeGen.create(op, getFrameSlot(), hasTemporalDeadZone(), NodeUtil.cloneNode(getLevelFrameNode()));
    }
}
