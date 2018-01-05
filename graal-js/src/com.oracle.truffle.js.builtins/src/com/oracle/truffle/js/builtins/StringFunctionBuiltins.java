/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltins.JSNumberOperation;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCharCodeNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCodePointNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToUInt16Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSString;

/**
 * Contains builtins for {@linkplain JSString} function (constructor).
 */
public final class StringFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<StringFunctionBuiltins.StringFunction> {
    protected StringFunctionBuiltins() {
        super(JSString.CLASS_NAME, StringFunction.class);
    }

    public enum StringFunction implements BuiltinEnum<StringFunction> {
        fromCharCode(1),

        // ES6
        fromCodePoint(1);

        private final int length;

        StringFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == fromCodePoint) {
                return 6;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, StringFunction builtinEnum) {
        switch (builtinEnum) {
            case fromCharCode:
                return JSFromCharCodeNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case fromCodePoint:
                return JSFromCodePointNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSFromCharCodeNode extends JSNumberOperation {

        public JSFromCharCodeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToUInt16Node toUInt16Node;

        private char toChar(Object target) {
            if (toUInt16Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt16Node = insert(JSToUInt16Node.create());
            }
            return (char) toUInt16Node.executeInt(target);
        }

        @Specialization(guards = "args.length == 0")
        protected String fromCharCode(@SuppressWarnings("unused") Object[] args) {
            return "";
        }

        @Specialization(guards = "args.length == 1")
        protected String fromCharCodeOneArg(Object[] args) {
            return String.valueOf(toChar(args[0]));
        }

        @Specialization(guards = "args.length >= 2")
        protected String fromCharCodeTwoOrMore(Object[] args) {
            StringBuilder buffer = new StringBuilder(args.length + 4);
            for (int i = 0; i < args.length; i++) {
                Boundaries.builderAppend(buffer, toChar(args[i]));
            }
            return Boundaries.builderToString(buffer);
        }
    }

    public abstract static class JSFromCodePointNode extends JSNumberOperation {

        public JSFromCodePointNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String fromCodePoint(Object[] args) {
            StringBuilder st = new StringBuilder(args.length);
            for (Object arg : args) {
                Number value = toNumber(arg);
                double valueDouble = JSRuntime.doubleValue(value);
                int valueInt = JSRuntime.intValue(value);
                if (JSRuntime.isNegativeZero(valueDouble)) {
                    valueInt = 0;
                } else if (!JSRuntime.doubleIsRepresentableAsInt(valueDouble) || (valueInt < 0) || (0x10FFFF < valueInt)) {
                    throwRangeError(value);
                }
                if (valueInt < 0x10000) {
                    Boundaries.builderAppend(st, (char) valueInt);
                } else {
                    valueInt -= 0x10000;
                    Boundaries.builderAppend(st, (char) ((valueInt >> 10) + 0xD800));
                    Boundaries.builderAppend(st, (char) ((valueInt % 0x400) + 0xDC00));
                }
            }
            return Boundaries.builderToString(st);
        }

        @TruffleBoundary
        private static void throwRangeError(Number value) {
            throw Errors.createRangeError("Invalid code point " + value);
        }
    }
}
