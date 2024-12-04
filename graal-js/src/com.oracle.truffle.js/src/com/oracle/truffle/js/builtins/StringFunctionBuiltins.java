/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.api.strings.TruffleStringIterator;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.DedentTemplateStringsArrayNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCharCodeNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.JSFromCodePointNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.StringDedentNodeGen;
import com.oracle.truffle.js.builtins.StringFunctionBuiltinsFactory.StringRawNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt16Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

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
        raw(1),

        // staging
        dedent(1);

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
            return switch (this) {
                case fromCodePoint -> JSConfig.ECMAScript2015;
                case dedent -> JSConfig.StagingECMAScriptVersion;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
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
            case dedent:
                return StringDedentNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSFromCharCodeNode extends JSBuiltinNode {

        public JSFromCharCodeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "args.length == 0")
        protected Object fromCharCode(@SuppressWarnings("unused") Object[] args) {
            return Strings.EMPTY_STRING;
        }

        @Specialization(guards = "args.length == 1")
        protected Object fromCharCodeOneArg(Object[] args,
                        @Shared @Cached JSToUInt16Node toUint16,
                        @Cached TruffleString.FromCodePointNode fromCodePointNode) {
            return Strings.fromCodePoint(fromCodePointNode, toUint16.executeChar(args[0]));
        }

        @Specialization(guards = "args.length >= 2")
        protected Object fromCharCodeTwoOrMore(Object[] args,
                        @Shared @Cached JSToUInt16Node toUint16,
                        @Cached TruffleString.FromCharArrayUTF16Node fromCharArrayNode) {
            char[] chars = new char[args.length];
            for (int i = 0; i < args.length; i++) {
                chars[i] = toUint16.executeChar(args[i]);
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

        public StringRawNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.templateToObjectNode = JSToObjectNode.create();
            this.rawToObjectNode = JSToObjectNode.create();
            this.getRawNode = PropertyGetNode.create(Strings.RAW, false, context);
            this.getRawLengthNode = JSGetLengthNode.create(context);
            this.segToStringNode = JSToStringNode.create();
            this.subToStringNode = JSToStringNode.create();
            this.readRawElementNode = ReadElementNode.create(context);
            this.appendStringNode = TruffleStringBuilder.AppendStringNode.create();
            this.builderToStringNode = TruffleStringBuilder.ToStringNode.create();
        }

        @Specialization
        protected Object raw(Object template, Object[] substitutions,
                        @Cached InlinedConditionProfile emptyProf) {
            int numberOfSubstitutions = substitutions.length;
            Object cooked = templateToObjectNode.execute(template);
            Object raw = rawToObjectNode.execute(getRawNode.getValue(cooked));

            int literalSegments = getRawLength(raw);
            if (emptyProf.profile(this, literalSegments <= 0)) {
                return Strings.EMPTY_STRING;
            }

            var result = Strings.builderCreate();
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

        private void appendChecked(TruffleStringBuilderUTF16 result, TruffleString str) {
            if (Strings.builderLength(result) + Strings.length(str) > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            Strings.builderAppend(appendStringNode, result, str);
        }
    }

    @ImportStatic(StringDedentNode.class)
    public abstract static class StringDedentNode extends JSBuiltinNode {

        static final HiddenKey TAG_KEY = new HiddenKey("TagKey");

        public StringDedentNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isObject.executeBoolean(callback)", "isCallable.executeBoolean(callback)"})
        protected Object dedentCallback(Object callback, @SuppressWarnings("unused") Object[] substitutions,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObject,
                        @Cached("createSetHidden(TAG_KEY, getContext())") PropertySetNode setArgs) {
            JSFunctionData functionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.DedentCallback, (c) -> callbackBody(c));
            JSFunctionObject function = JSFunction.create(getRealm(), functionData);
            setArgs.setValue(function, callback);
            return function;
        }

        private static JSFunctionData callbackBody(JSContext context) {
            class CallbackBody extends JavaScriptRootNode {
                @Child private DedentTemplateStringsArrayNode dedentTemplateStringsArray = DedentTemplateStringsArrayNodeGen.create(context);
                @Child private PropertyGetNode getTag = PropertyGetNode.createGetHidden(TAG_KEY, context);
                @Child private JSFunctionCallNode callResolve = JSFunctionCallNode.createCall();
                @Child private IsObjectNode isObject = IsObjectNode.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    Object tag = getTag.getValue(functionObject);
                    Object r = JSFrameUtil.getThisObj(frame);

                    Object[] args = JSFrameUtil.getArgumentsArray(frame);
                    if (args.length < 1) {
                        throw Errors.createTypeError("Expected at least one argument");
                    }
                    Object template = args[0];
                    if (!isObject.executeBoolean(template)) {
                        throw Errors.createTypeErrorNotAnObject(template);
                    }
                    JSArrayObject dedentedArray = dedentTemplateStringsArray.execute(template, context);
                    Object[] callbackArgs = Arrays.copyOf(args, args.length);
                    callbackArgs[0] = dedentedArray;
                    return callResolve.executeCall(JSArguments.create(r, tag, callbackArgs));
                }
            }
            return JSFunctionData.createCallOnly(context, new CallbackBody().getCallTarget(), 2, Strings.EMPTY_STRING);
        }

        @Specialization(guards = {"isObject.executeBoolean(template)", "!isCallable.executeBoolean(template)"})
        protected static Object dedentTemplate(Object template, Object[] substitutions,
                        @Bind Node self,
                        @Bind("getContext()") JSContext context,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObject,
                        @Cached("create(getContext())") DedentTemplateStringsArrayNode dedentTemplateStringsArray,
                        @Cached("create(getContext())") ReadElementNode readElementNode,
                        @Cached JSToStringNode segToStringNode,
                        @Cached JSToStringNode subToStringNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.ToStringNode builderToStringNode) {
            int stringLengthLimit = context.getStringLengthLimit();
            JSArrayObject dedentedArray = dedentTemplateStringsArray.execute(template, context);
            // CookTemplateStringsArray (prep = cooked)
            int substitutionCount = substitutions.length;
            long literalCount = JSArray.arrayGetLength(dedentedArray);
            if (literalCount <= 0) {
                return Strings.EMPTY_STRING;
            }
            var result = Strings.builderCreate();
            for (int i = 0; i < literalCount; i++) {
                TruffleString nextSeg = segToStringNode.executeString(readElementNode.executeWithTargetAndIndex(dedentedArray, i));
                appendChecked(result, nextSeg, stringLengthLimit,
                                self, errorBranch, appendStringNode);
                if (i + 1 == literalCount) {
                    break;
                }
                if (i < substitutionCount) {
                    TruffleString nextSub = subToStringNode.executeString(substitutions[i]);
                    appendChecked(result, nextSub, stringLengthLimit,
                                    self, errorBranch, appendStringNode);
                }
            }

            return Strings.builderToString(builderToStringNode, result);
        }

        private static void appendChecked(TruffleStringBuilderUTF16 result, TruffleString str, int stringLengthLimit,
                        Node self,
                        InlinedBranchProfile errorBranch,
                        TruffleStringBuilder.AppendStringNode appendStringNode) {
            if (Strings.builderLength(result) + Strings.length(str) > stringLengthLimit) {
                errorBranch.enter(self);
                throw Errors.createRangeErrorInvalidStringLength();
            }
            Strings.builderAppend(appendStringNode, result, str);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        protected static Object notAnObject(Object template, @SuppressWarnings("unused") Object substitutions) {
            throw Errors.createTypeErrorNotAnObject(template);
        }
    }

    public abstract static class DedentTemplateStringsArrayNode extends JavaScriptBaseNode {

        @Child private PropertyGetNode getRawNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private ReadElementNode readRawElementNode;
        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.AppendSubstringByteIndexNode appendSubstringNode;
        @Child private TruffleStringBuilder.AppendCharUTF16Node appendCharNode;
        @Child private TruffleStringBuilder.AppendCodePointNode appendCodePointNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;
        @Child private TruffleString.ReadCharUTF16Node readCharNode;
        @Child private TruffleString.SubstringByteIndexNode substringNode;
        @Child private TruffleStringIterator.NextNode iteratorNextNode;
        @Child private TruffleStringIterator.PreviousNode iteratorPreviousNode;

        DedentTemplateStringsArrayNode(JSContext context) {
            this.getLengthNode = JSGetLengthNode.create(context);
            this.getRawNode = PropertyGetNode.create(Strings.RAW, context);
            this.readRawElementNode = ReadElementNode.create(context);
            this.appendStringNode = TruffleStringBuilder.AppendStringNode.create();
            this.appendSubstringNode = TruffleStringBuilder.AppendSubstringByteIndexNode.create();
            this.appendCharNode = TruffleStringBuilder.AppendCharUTF16Node.create();
            this.appendCodePointNode = TruffleStringBuilder.AppendCodePointNode.create();
            this.builderToStringNode = TruffleStringBuilder.ToStringNode.create();
            this.readCharNode = TruffleString.ReadCharUTF16Node.create();
            this.substringNode = TruffleString.SubstringByteIndexNode.create();
            this.iteratorNextNode = TruffleStringIterator.NextNode.create();
            this.iteratorPreviousNode = TruffleStringIterator.PreviousNode.create();
        }

        protected abstract JSArrayObject execute(Object template, JSContext context);

        @Specialization
        protected final JSArrayObject dedentTemplateStringsArray(Object template, JSContext context,
                        @Cached JSToObjectNode rawToObjectNode,
                        @Cached InlinedConditionProfile emptyProf,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedBranchProfile growBranch,
                        @Cached TruffleString.CreateCodePointIteratorNode createCodePointIterator,
                        @Cached JSCollectionsNormalizeNode collectionsNormalize) {
            Object rawInput = collectionsNormalize.execute(getRawNode.getValue(template));
            JSRealm realm = getRealm();
            Map<Object, JSArrayObject> dedentMap = realm.getDedentMap();
            JSArrayObject cached = Boundaries.mapGet(dedentMap, rawInput);
            if (cached != null) {
                return cached;
            }

            TruffleString[] dedentedList = dedentStringsArray(rawInput, context, rawToObjectNode, emptyProf, errorBranch, growBranch);

            JSArrayObject rawArr = JSArray.createConstant(context, realm, dedentedList);
            JSArrayObject cookedArr = JSArray.createConstant(context, realm, cookStrings(dedentedList, createCodePointIterator, errorBranch));
            JSRuntime.definePropertyOrThrow(cookedArr, Strings.RAW, PropertyDescriptor.createData(rawArr, false, false, false));
            rawArr.setIntegrityLevel(true, true);
            cookedArr.setIntegrityLevel(true, true);
            Boundaries.mapPut(dedentMap, rawInput, cookedArr);

            return cookedArr;
        }

        private TruffleString[] dedentStringsArray(Object template, JSContext context,
                        JSToObjectNode rawToObjectNode,
                        InlinedConditionProfile emptyProf,
                        InlinedBranchProfile errorBranch,
                        InlinedBranchProfile growBranch) {
            Object templateObj = rawToObjectNode.execute(template);
            int literalSegments = getLength(templateObj);
            if (emptyProf.profile(this, literalSegments <= 0)) {
                errorBranch.enter(this);
                // Note: Well-formed template strings arrays always contain at least 1 string.
                throw Errors.createTypeError("Template raw array must contain at least 1 string");
            }

            SegmentRecord[][] blocks = splitTemplateIntoBlockLines(templateObj, literalSegments, context.getStringLengthLimit(), errorBranch, growBranch);
            emptyWhiteSpaceLines(blocks);
            removeOpeningAndClosingLines(blocks, errorBranch);

            TruffleString indent = determineCommonLeadingIndentation(blocks);
            int indentLength = Strings.length(indent);

            TruffleString[] dedented = new TruffleString[blocks.length];
            for (int j = 0; j < blocks.length; j++) {
                SegmentRecord[] lines = blocks[j];
                var partialResult = Strings.builderCreate();
                for (int i = 0; i < lines.length; i++) {
                    SegmentRecord line = lines[i];
                    int currentIndentation = i == 0 || Strings.isEmpty(line.substr) ? 0 : indentLength;
                    Strings.builderAppendLen(appendSubstringNode, partialResult, line.substr, currentIndentation, Strings.length(line.substr) - currentIndentation);
                    Strings.builderAppend(appendStringNode, partialResult, line.newline);
                }
                dedented[j] = Strings.builderToString(builderToStringNode, partialResult);
            }

            return dedented;
        }

        private static final class SegmentRecord {
            TruffleString substr;
            TruffleString newline;
            boolean lineEndsWithSubstitution;

            SegmentRecord(TruffleString substr, TruffleString newline, boolean lineEndsWithSubstitution) {
                this.substr = substr;
                this.newline = newline;
                this.lineEndsWithSubstitution = lineEndsWithSubstitution;
            }
        }

        private SegmentRecord[][] splitTemplateIntoBlockLines(Object raw, int len, int stringLengthLimit,
                        InlinedBranchProfile errorBranch,
                        InlinedBranchProfile growBranch) {
            SegmentRecord[][] blocks = new SegmentRecord[len][];
            int totalLength = 0;
            for (int k = 0; k < len; k++) {
                Object rawElement = readRawElementNode.executeWithTargetAndIndex(raw, k);
                if (!(rawElement instanceof TruffleString nextSeg)) {
                    throw Errors.createTypeError("Template raw array may only contain strings");
                }
                int segLength = Strings.length(nextSeg);
                totalLength += segLength;
                if (totalLength > stringLengthLimit) {
                    errorBranch.enter(this);
                    throw Errors.createRangeErrorInvalidStringLength();
                }
                int start = 0;
                SimpleArrayList<SegmentRecord> lines = new SimpleArrayList<>(segLength + 1);
                for (int i = 0; i < segLength;) {
                    char c = Strings.charAt(readCharNode, nextSeg, i);
                    int n = (c == '\r' && i + 1 < segLength && Strings.charAt(readCharNode, nextSeg, i + 1) == '\n') ? 2 : 1;
                    if (JSRuntime.isLineTerminator(c)) {
                        TruffleString substr = Strings.lazySubstring(substringNode, nextSeg, start, i - start);
                        TruffleString newline = Strings.lazySubstring(substringNode, nextSeg, i, n);
                        lines.add(new SegmentRecord(substr, newline, false), this, growBranch);
                        start = i + n;
                    }
                    i = i + n;
                }
                TruffleString tail = Strings.lazySubstring(substringNode, nextSeg, start, segLength - start);
                boolean lineEndsWithSubstitution = k + 1 < len;
                lines.add(new SegmentRecord(tail, Strings.EMPTY_STRING, lineEndsWithSubstitution), this, growBranch);
                blocks[k] = lines.toArray(new SegmentRecord[lines.size()]);
            }
            return blocks;
        }

        private void emptyWhiteSpaceLines(SegmentRecord[][] blocks) {
            for (SegmentRecord[] lines : blocks) {
                for (int i = 1; i < lines.length; i++) {
                    SegmentRecord line = lines[i];
                    if (!line.lineEndsWithSubstitution && isAllWhitespace(line.substr)) {
                        line.substr = Strings.EMPTY_STRING;
                    }
                }
            }
        }

        private boolean isAllWhitespace(TruffleString str) {
            int len = Strings.length(str);
            for (int i = 0; i < len; i++) {
                if (!JSRuntime.isWhiteSpaceOrLineTerminator(Strings.charAt(readCharNode, str, i))) {
                    return false;
                }
            }
            return true;
        }

        private void removeOpeningAndClosingLines(SegmentRecord[][] blocks,
                        InlinedBranchProfile errorBranch) {
            SegmentRecord[] firstBlock = blocks[0];
            /*
             * firstBlock is not empty, because SplitTemplateIntoBlockLines guarantees there is at
             * least 1 line per block.
             */
            int lineCount = firstBlock.length;
            assert lineCount != 0;
            if (lineCount == 1) {
                /*
                 * The opening line is required to contain a trailing newline, and checking that
                 * there are at least 2 elements in lines ensures it. If it does not, either the
                 * opening line and the closing line are the same line, or the opening line contains
                 * a substitution.
                 */
                errorBranch.enter(this);
                throw Errors.createTypeError("The opening line must contain a trailing newline.");
            }
            SegmentRecord openingLine = firstBlock[0];
            if (!Strings.isEmpty(openingLine.substr)) {
                // The opening line must not contain code units besides the trailing newline.
                errorBranch.enter(this);
                throw Errors.createTypeError("The opening line must be empty.");
            }
            // Removes the opening line from the output
            openingLine.newline = Strings.EMPTY_STRING;

            SegmentRecord[] lastBlock = blocks[blocks.length - 1];
            lineCount = lastBlock.length;
            if (lineCount == 1) {
                /*
                 * The closing line is required to be preceded by a newline, and checking that there
                 * are at least 2 elements in lines ensures it. If it does not, either the opening
                 * line and the closing line are the same line, or the closing line contains a
                 * substitution.
                 */
                errorBranch.enter(this);
                throw Errors.createTypeError("The closing line must be preceded by a newline.");
            }
            SegmentRecord closingLine = lastBlock[lineCount - 1];
            if (!Strings.isEmpty(closingLine.substr)) {
                /*
                 * The closing line may only contain whitespace. We've already performed
                 * EmptyWhiteSpaceLines, so if the line is not empty now, it contained some
                 * non-whitespace character.
                 */
                errorBranch.enter(this);
                throw Errors.createTypeError("The closing line must be empty.");
            }
            SegmentRecord preceding = lastBlock[lineCount - 2];
            closingLine.substr = Strings.EMPTY_STRING;
            preceding.newline = Strings.EMPTY_STRING;
        }

        private TruffleString determineCommonLeadingIndentation(SegmentRecord[][] blocks) {
            TruffleString common = null;
            for (SegmentRecord[] lines : blocks) {
                /*
                 * We start i at 1 because because the first line of every block is either (a) the
                 * opening line which must be empty or (b) the continuation of a line directly after
                 * a template substitution. Neither can be the start of a content line.
                 */
                for (int i = 1; i < lines.length; i++) {
                    SegmentRecord line = lines[i];
                    /*
                     * Lines which contain substitutions are considered when finding the common
                     * indentation. Lines which contain only whitespace have already been emptied.
                     */
                    if (line.lineEndsWithSubstitution || !Strings.isEmpty(line.substr)) {
                        TruffleString leading = leadingWhiteSpaceSubstring(line.substr);
                        if (common == null) {
                            common = leading;
                        } else {
                            common = longestMatchingLeadingSubstring(common, leading);
                        }
                    }
                }
            }
            /*
             * common is not empty, because SplitTemplateIntoBlockLines guarantees there is at least
             * 1 line per block, and we know the length of the template strings array is at least 1.
             */
            assert common != null;
            return common;
        }

        private TruffleString leadingWhiteSpaceSubstring(TruffleString str) {
            int length = Strings.length(str);
            for (int i = 0; i < length; i++) {
                if (!JSRuntime.isWhiteSpaceExcludingLineTerminator(Strings.charAt(readCharNode, str, i))) {
                    return Strings.lazySubstring(substringNode, str, 0, i);
                }
            }
            return str;
        }

        private TruffleString longestMatchingLeadingSubstring(TruffleString strA, TruffleString strB) {
            int len = Math.min(Strings.length(strA), Strings.length(strB));
            for (int i = 0; i < len; i++) {
                if (Strings.charAt(readCharNode, strA, i) != Strings.charAt(readCharNode, strB, i)) {
                    return Strings.lazySubstring(substringNode, strA, 0, i);
                }
            }
            return Strings.lazySubstring(substringNode, strA, 0, len);
        }

        private Object[] cookStrings(TruffleString[] raw,
                        TruffleString.CreateCodePointIteratorNode createCodePointIterator,
                        InlinedBranchProfile errorBranch) {
            Object[] cooked = new Object[raw.length];
            for (int i = 0; i < raw.length; i++) {
                TruffleString str = raw[i];
                TruffleStringIterator iterator = createCodePointIterator.execute(str, TruffleString.Encoding.UTF_16);
                cooked[i] = parseText(iterator, errorBranch);
            }
            return cooked;
        }

        /**
         * Returns either a TruffleString or undefined.
         */
        private Object parseText(TruffleStringIterator iterator,
                        InlinedBranchProfile errorBranch) {
            var partialResult = Strings.builderCreate();
            while (iterator.hasNext()) {
                int ch = iteratorNextNode.execute(iterator);
                if (ch == '\\') {
                    if (!iterator.hasNext()) {
                        // Lone backslash at the end.
                        return Undefined.instance;
                    }
                    final int next = iteratorNextNode.execute(iterator);
                    switch (next) {
                        case '0': {
                            // only `\0` by itself is allowed but not e.g. `\02`.
                            if (JSRuntime.isAsciiDigit(peekNext(iterator))) {
                                return Undefined.instance;
                            }
                            appendCharNode.execute(partialResult, '\0');
                            break;
                        }
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            return Undefined.instance;
                        case 'n':
                            appendCharNode.execute(partialResult, '\n');
                            break;
                        case 't':
                            appendCharNode.execute(partialResult, '\t');
                            break;
                        case 'b':
                            appendCharNode.execute(partialResult, '\b');
                            break;
                        case 'f':
                            appendCharNode.execute(partialResult, '\f');
                            break;
                        case 'r':
                            appendCharNode.execute(partialResult, '\r');
                            break;
                        case '\'':
                            appendCharNode.execute(partialResult, '\'');
                            break;
                        case '\"':
                            appendCharNode.execute(partialResult, '\"');
                            break;
                        case '\\':
                            appendCharNode.execute(partialResult, '\\');
                            break;
                        case '\r': // CR | CRLF
                            if (iteratorNextNode.execute(iterator) != '\n') {
                                iteratorPreviousNode.execute(iterator);
                            }
                            break;
                        case '\n':
                        case '\u2028':
                        case '\u2029':
                            break;
                        case 'x': {
                            // Hex sequence.
                            final int asciiCh = hexSequence(iterator, 2, errorBranch);
                            appendCharNode.execute(partialResult, (char) asciiCh);
                            break;
                        }
                        case 'u': {
                            final int unicodeChar = unicodeEscapeSequence(iterator, errorBranch);
                            if (unicodeChar < 0) {
                                appendStringNode.execute(partialResult, Strings.BACKSLASH_U);
                            } else if (unicodeChar <= 0xffff && Character.isSurrogate((char) unicodeChar)) {
                                appendCharNode.execute(partialResult, (char) unicodeChar);
                            } else {
                                appendCodePointNode.execute(partialResult, unicodeChar);
                            }
                            break;
                        }
                        case 'v':
                            appendCharNode.execute(partialResult, '\u000b');
                            break;
                        default:
                            if (next <= 0xffff && Character.isSurrogate((char) next)) {
                                appendCharNode.execute(partialResult, (char) next);
                            } else {
                                appendCodePointNode.execute(partialResult, next);
                            }
                            break;
                    }
                } else {
                    if (ch <= 0xffff && Character.isSurrogate((char) ch)) {
                        appendCharNode.execute(partialResult, (char) ch);
                    } else {
                        appendCodePointNode.execute(partialResult, ch);
                    }
                }
            }
            return Strings.builderToString(builderToStringNode, partialResult);
        }

        private int peekNext(TruffleStringIterator iterator) {
            int ch = iteratorNextNode.execute(iterator);
            iteratorPreviousNode.execute(iterator);
            return ch;
        }

        private int unicodeEscapeSequence(TruffleStringIterator iterator,
                        InlinedBranchProfile errorBranch) {
            int ch = peekNext(iterator);
            if (ch == '{') {
                return varlenHexSequence(iterator, errorBranch);
            } else {
                return hexSequence(iterator, 4, errorBranch);
            }
        }

        private int varlenHexSequence(TruffleStringIterator iterator,
                        InlinedBranchProfile errorBranch) {
            int ch = iteratorNextNode.execute(iterator);
            assert ch == '{';

            int value = 0;
            boolean firstIteration = true;
            while (iterator.hasNext()) {
                ch = iteratorNextNode.execute(iterator);
                if (ch == '}') {
                    if (!firstIteration) {
                        break;
                    } else {
                        errorBranch.enter(this);
                        throw Errors.createSyntaxError("Invalid Unicode escape sequence");
                    }
                }

                final int digit = convertDigit(ch, 16);

                if (digit == -1) {
                    errorBranch.enter(this);
                    throw Errors.createSyntaxError("Invalid Unicode escape sequence");
                }

                value = digit | value << 4;

                if (value > 1114111) {
                    errorBranch.enter(this);
                    throw Errors.createSyntaxError("Invalid Unicode escape sequence");
                }
                firstIteration = false;
            }

            return value;
        }

        private int hexSequence(TruffleStringIterator iterator, int length,
                        InlinedBranchProfile errorBranch) {
            int value = 0;
            int i;
            for (i = 0; i < length && iterator.hasNext(); i++) {
                int ch = iteratorNextNode.execute(iterator);
                int digit = convertDigit(ch, 16);
                if (digit == -1) {
                    errorBranch.enter(this);
                    throw Errors.createSyntaxError("Invalid hex digit");
                }
                value = digit | value << 4;
            }

            if (i != length) {
                errorBranch.enter(this);
                throw Errors.createSyntaxError("Invalid hex length");
            }

            return value;
        }

        protected static int convertDigit(final int ch, final int base) {
            int digit;

            if ('0' <= ch && ch <= '9') {
                digit = ch - '0';
            } else if ('A' <= ch && ch <= 'Z') {
                digit = ch - 'A' + 10;
            } else if ('a' <= ch && ch <= 'z') {
                digit = ch - 'a' + 10;
            } else {
                return -1;
            }

            return digit < base ? digit : -1;
        }

        private int getLength(Object raw) {
            long length = getLengthNode.executeLong(raw);
            try {
                return Math.toIntExact(length);
            } catch (ArithmeticException e) {
                return 0;
            }
        }
    }
}
