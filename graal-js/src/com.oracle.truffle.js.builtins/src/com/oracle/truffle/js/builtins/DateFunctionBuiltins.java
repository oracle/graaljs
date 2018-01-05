/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.DateFunctionBuiltinsFactory.DateNowNodeGen;
import com.oracle.truffle.js.builtins.DateFunctionBuiltinsFactory.DateParseNodeGen;
import com.oracle.truffle.js.builtins.DateFunctionBuiltinsFactory.DateUTCNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltins.JSNumberOperation;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;

/**
 * Contains builtins for {@linkplain JSDate} function (constructor).
 */
public final class DateFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<DateFunctionBuiltins.DateFunction> {
    protected DateFunctionBuiltins() {
        super(JSDate.CLASS_NAME, DateFunction.class);
    }

    public enum DateFunction implements BuiltinEnum<DateFunction> {
        parse(1),
        now(0),
        UTC(7);

        private final int length;

        DateFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DateFunction builtinEnum) {
        switch (builtinEnum) {
            case parse:
                return DateParseNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case now:
                return DateNowNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case UTC:
                return DateUTCNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class DateParseNode extends JSNumberOperation {
        private final ConditionProfile gotFieldsProfile = ConditionProfile.createBinaryProfile();

        public DateParseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double parse(Object parseDate) {
            String dateString = toString(parseDate);
            Integer[] fields = getContext().getEvaluator().parseDate(getContext().getRealm(), dateString.trim());
            if (gotFieldsProfile.profile(fields != null)) {
                return JSDate.makeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7], getContext());
            }
            return Double.NaN;
        }

    }

    public abstract static class DateNowNode extends JSBuiltinNode {
        public DateNowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected static double now() {
            return System.currentTimeMillis();
        }
    }

    public abstract static class DateUTCNode extends JSNumberOperation {
        public DateUTCNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double utc(Object... args) {
            double[] argsEvaluated = new double[args.length];
            boolean isNaN = false;
            for (int i = 0; i < args.length; i++) {
                double d = JSRuntime.doubleValue(toNumber(args[i]));
                if (Double.isNaN(d)) {
                    isNaN = true;
                }
                argsEvaluated[i] = d;
            }
            if (isNaN) {
                return Double.NaN;
            }
            return JSDate.executeConstructor(argsEvaluated, true, getContext());
        }
    }
}
