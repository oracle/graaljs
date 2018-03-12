/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNodeGen.ToArrayIndexWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Converts value to array index according to ES5 15.4 Array Objects.
 */
public abstract class ToArrayIndexNode extends JavaScriptBaseNode {
    protected final boolean convertToString;

    public abstract int executeInt(Object operand) throws UnexpectedResultException;

    public abstract Object execute(Object operand);

    protected ToArrayIndexNode(boolean convertToString) {
        this.convertToString = convertToString;
    }

    public static ToArrayIndexNode create() {
        return ToArrayIndexNodeGen.create(true);
    }

    public static ToArrayIndexNode createNoToString() {
        return ToArrayIndexNodeGen.create(false);
    }

    @Specialization(guards = "isIntArrayIndex(value)")
    protected static long doInteger(int value) {
        return value;
    }

    @Specialization(guards = "isLongArrayIndex(value)")
    protected static long doLong(long value) {
        return JSRuntime.castArrayIndex(value);
    }

    protected static boolean doubleIsIntIndex(double d) {
        return JSRuntime.doubleIsRepresentableAsInt(d) && d >= 0;
    }

    @Specialization(guards = "doubleIsIntIndex(value)")
    protected static long doDoubleAsIntIndex(double value) {
        return (long) value;
    }

    protected static boolean doubleIsUintIndex(double d) {
        return JSRuntime.doubleIsRepresentableAsUnsignedInt(d, true) && d >= 0 && d < 0xffff_ffffL;
    }

    @Specialization(guards = "doubleIsUintIndex(value)")
    protected static long doDoubleAsUintIndex(double value) {
        return JSRuntime.castArrayIndex(value);
    }

    @Specialization
    protected static Symbol doSymbol(Symbol value) {
        return value;
    }

    @Specialization
    protected static HiddenKey doHiddenKey(HiddenKey value) {
        return value;
    }

    @Specialization(guards = "indexLengthInRange(index)")
    protected static Object convertFromString(String index,
                    @Cached("create()") BranchProfile startsWithDigitBranch,
                    @Cached("create()") BranchProfile isArrayIndexBranch,
                    @Cached("create()") BranchProfile needPassStringBranch) {
        if (JSRuntime.isAsciiDigit(index.charAt(0))) {
            startsWithDigitBranch.enter();
            long longValue = JSRuntime.parseArrayIndexRaw(index);
            if (JSRuntime.isArrayIndex(longValue)) {
                isArrayIndexBranch.enter();
                return JSRuntime.castArrayIndex(longValue);
            }
        }
        needPassStringBranch.enter();
        return index;
    }

    @Specialization(guards = "!indexLengthInRange(index)")
    protected static Object convertFromStringNotInRange(String index) {
        return index;
    }

    protected boolean indexLengthInRange(String index) {
        int len = index.length();
        return 0 < len && len <= JSRuntime.MAX_UINT32_DIGITS;
    }

    protected static boolean notArrayIndex(Object o) {
        assert !(o instanceof HiddenKey);
        return (!(o instanceof Integer) || !JSGuards.isIntArrayIndex((int) o)) && (!(o instanceof Double) || !doubleIsUintIndex((double) o)) &&
                        (!(o instanceof Long) || !JSGuards.isLongArrayIndex((long) o)) && !(o instanceof String) && !(o instanceof Symbol);
    }

    @Specialization(guards = {"!convertToString", "notArrayIndex(value)"})
    protected static Object doNonArrayIndex(Object value) {
        return value;
    }

    @Specialization(guards = {"convertToString", "notArrayIndex(value)"})
    protected static String doNonArrayIndex(Object value,
                    @Cached("create()") JSToStringNode toStringNode) {
        return toStringNode.executeString(value);
    }

    public abstract static class ToArrayIndexWrapperNode extends JSUnaryNode {
        @Child private ToArrayIndexNode toArrayIndexNode;

        public static ToArrayIndexWrapperNode create(JavaScriptNode operand) {
            return ToArrayIndexWrapperNodeGen.create(operand);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return ToArrayIndexWrapperNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}
