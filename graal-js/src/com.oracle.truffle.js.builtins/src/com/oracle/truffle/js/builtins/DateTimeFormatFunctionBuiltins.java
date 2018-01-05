/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.js.builtins.CollatorFunctionBuiltinsFactory.SupportedLocalesOfNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;

/**
 * Contains builtins for {@linkplain JSDateTimeFormat} function (constructor).
 */
public final class DateTimeFormatFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<DateTimeFormatFunctionBuiltins.DateTimeFormatFunction> {
    protected DateTimeFormatFunctionBuiltins() {
        super(JSDateTimeFormat.CLASS_NAME, DateTimeFormatFunction.class);
    }

    public enum DateTimeFormatFunction implements BuiltinEnum<DateTimeFormatFunction> {
        supportedLocalesOf(1);

        private final int length;

        DateTimeFormatFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DateTimeFormatFunction builtinEnum) {
        switch (builtinEnum) {
            case supportedLocalesOf:
                return SupportedLocalesOfNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }
}
