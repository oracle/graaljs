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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltins.JSNumberOperation;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCharCodeNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCodePointNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.StringRawNodeGen;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt16Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
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
        fromCodePoint(1),
        raw(1);

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
            case raw:
                return StringRawNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
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

    public abstract static class StringRawNode extends JSBuiltinNode {
        @Child private JSToObjectNode templateToObjectNode;
        @Child private JSToObjectNode rawToObjectNode;
        @Child private PropertyGetNode getRawNode;
        @Child private JSGetLengthNode getRawLengthNode;
        @Child private JSToStringNode segToStringNode;
        @Child private JSToStringNode subToStringNode;
        @Child private ReadElementNode readRawElementNode;
        private final ConditionProfile emptyProf = ConditionProfile.createBinaryProfile();

        public StringRawNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.templateToObjectNode = JSToObjectNode.createToObject(context);
            this.rawToObjectNode = JSToObjectNode.createToObject(context);
            this.getRawNode = PropertyGetNode.create("raw", false, context);
            this.getRawLengthNode = JSGetLengthNode.create(context);
            this.segToStringNode = JSToStringNode.create();
            this.subToStringNode = JSToStringNode.create();
            this.readRawElementNode = ReadElementNode.create(context);
        }

        @Specialization
        protected String raw(Object template, Object[] substitutions) {
            int numberOfSubstitutions = substitutions.length;
            TruffleObject cooked = templateToObjectNode.executeTruffleObject(template);
            TruffleObject raw = rawToObjectNode.executeTruffleObject(getRawNode.getValue(cooked));

            int literalSegments = getRawLength(raw);
            if (emptyProf.profile(literalSegments <= 0)) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0;; i++) {
                Object rawElement = readRawElementNode.executeWithTargetAndIndex(raw, i);
                String nextSeg = segToStringNode.executeString(rawElement);
                appendChecked(result, nextSeg);
                if (i + 1 == literalSegments) {
                    break;
                }
                if (i < numberOfSubstitutions) {
                    String nextSub = subToStringNode.executeString(substitutions[i]);
                    appendChecked(result, nextSub);
                }
            }
            return Boundaries.builderToString(result);
        }

        private int getRawLength(TruffleObject raw) {
            long length = getRawLengthNode.executeLong(raw);
            try {
                return Math.toIntExact(length);
            } catch (ArithmeticException e) {
                return 0;
            }
        }

        private static void appendChecked(StringBuilder result, String str) {
            if (result.length() + str.length() > JSTruffleOptions.StringLengthLimit) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            Boundaries.builderAppend(result, str);
        }
    }
}
