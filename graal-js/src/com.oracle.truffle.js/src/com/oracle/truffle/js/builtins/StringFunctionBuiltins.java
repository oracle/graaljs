/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltins.JSNumberOperation;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCharCodeNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCodePointNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.StringRawNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt16Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSString;

/**
 * Contains builtins for {@linkplain JSString} function (constructor).
 */
public final class StringFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<StringFunctionBuiltins.StringFunction> {

    public static final JSBuiltinsContainer BUILTINS = new StringFunctionBuiltins();

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
        protected Object fromCharCode(@SuppressWarnings("unused") Object[] args) {
            return Strings.EMPTY_STRING;
        }

        @Specialization(guards = "args.length == 1")
        protected Object fromCharCodeOneArg(Object[] args,
                        @Cached TruffleString.FromCodePointNode fromCodePointNode) {
            return Strings.fromCodePoint(fromCodePointNode, toChar(args[0]));
        }

        @Specialization(guards = "args.length >= 2")
        protected Object fromCharCodeTwoOrMore(Object[] args,
                        @Cached TruffleString.FromCharArrayUTF16Node fromCharArrayNode) {
            char[] chars = new char[args.length];
            for (int i = 0; i < args.length; i++) {
                chars[i] = toChar(args[i]);
            }
            return Strings.fromCharArray(fromCharArrayNode, chars);
        }
    }

    public abstract static class JSFromCodePointNode extends JSBuiltinNode {

        public JSFromCodePointNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object fromCodePoint(Object[] args,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached TruffleString.FromCodePointNode fromCodePointNode,
                        @Cached TruffleString.ConcatNode concatNode) {
            TruffleString st = Strings.EMPTY_STRING;
            for (Object arg : args) {
                Number value = toNumberNode.executeNumber(arg);
                double valueDouble = JSRuntime.doubleValue(value);
                int valueInt = JSRuntime.intValue(value);
                if (JSRuntime.isNegativeZero(valueDouble)) {
                    valueInt = 0;
                } else if (!JSRuntime.doubleIsRepresentableAsInt(valueDouble) || (valueInt < 0) || (0x10FFFF < valueInt)) {
                    throwRangeError(value);
                }
                st = Strings.concat(concatNode, st, Strings.fromCodePoint(fromCodePointNode, valueInt));
            }
            return st;
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
        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;
        private final ConditionProfile emptyProf = ConditionProfile.createBinaryProfile();

        public StringRawNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.templateToObjectNode = JSToObjectNode.createToObject(context);
            this.rawToObjectNode = JSToObjectNode.createToObject(context);
            this.getRawNode = PropertyGetNode.create(Strings.RAW, false, context);
            this.getRawLengthNode = JSGetLengthNode.create(context);
            this.segToStringNode = JSToStringNode.create();
            this.subToStringNode = JSToStringNode.create();
            this.readRawElementNode = ReadElementNode.create(context);
            this.appendStringNode = TruffleStringBuilder.AppendStringNode.create();
            this.builderToStringNode = TruffleStringBuilder.ToStringNode.create();
        }

        @Specialization
        protected Object raw(Object template, Object[] substitutions) {
            int numberOfSubstitutions = substitutions.length;
            Object cooked = templateToObjectNode.execute(template);
            Object raw = rawToObjectNode.execute(getRawNode.getValue(cooked));

            int literalSegments = getRawLength(raw);
            if (emptyProf.profile(literalSegments <= 0)) {
                return Strings.EMPTY_STRING;
            }

            TruffleStringBuilder result = Strings.builderCreate();
            for (int i = 0;; i++) {
                Object rawElement = readRawElementNode.executeWithTargetAndIndex(raw, i);
                TruffleString nextSeg = segToStringNode.executeString(rawElement);
                appendChecked(result, nextSeg);
                if (i + 1 == literalSegments) {
                    break;
                }
                if (i < numberOfSubstitutions) {
                    TruffleString nextSub = subToStringNode.executeString(substitutions[i]);
                    appendChecked(result, nextSub);
                }
            }
            return Strings.builderToString(builderToStringNode, result);
        }

        private int getRawLength(Object raw) {
            long length = getRawLengthNode.executeLong(raw);
            try {
                return Math.toIntExact(length);
            } catch (ArithmeticException e) {
                return 0;
            }
        }

        private void appendChecked(TruffleStringBuilder result, TruffleString str) {
            if (Strings.builderLength(result) + Strings.length(str) > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            Strings.builderAppend(appendStringNode, result, str);
        }
    }
}
