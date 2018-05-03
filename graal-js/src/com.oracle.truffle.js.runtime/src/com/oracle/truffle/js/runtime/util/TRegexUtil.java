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
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.regex.result.RegexResult;

public final class TRegexUtil {

    public static final class Props {

        public static final class CompiledRegex {

            public static final String PATTERN = "pattern";
            public static final String FLAGS = "flags";
            public static final String EXEC = "exec";
            public static final String GROUPS = "groups";
        }

        public static final class Flags {

            public static final String SOURCE = "source";
            public static final String GLOBAL = "global";
            public static final String MULTILINE = "multiline";
            public static final String IGNORE_CASE = "ignoreCase";
            public static final String STICKY = "sticky";
            public static final String UNICODE = "unicode";
            public static final String DOT_ALL = "dotAll";
        }

        public static final class RegexResult {

            public static final String IS_MATCH = "isMatch";
            public static final String INPUT = "input";
            public static final String GROUP_COUNT = "groupCount";
            public static final String START = "start";
            public static final String END = "end";
            public static final String REGEX = "regex";
        }
    }

    public static final class Constants {
        public static final int CAPTURE_GROUP_NO_MATCH = -1;
    }

    public static TruffleObject getTRegexEmptyResult() {
        return RegexResult.NO_MATCH;
    }

    public static Object readExceptionIsFatal(Node readNode, TruffleObject object, Object property) {
        try {
            return ForeignAccess.sendRead(readNode, object, property);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(e);
        }
    }

    public static Node createReadNode() {
        return Message.READ.createNode();
    }

    public static String readPattern(Node readNode, TruffleObject compiledRegexObject) {
        return (String) readExceptionIsFatal(readNode, compiledRegexObject, Props.CompiledRegex.PATTERN);
    }

    public static TruffleObject readFlags(Node readNode, TruffleObject compiledRegexObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, compiledRegexObject, Props.CompiledRegex.FLAGS);
    }

    public static TruffleObject readNamedCaptureGroups(Node readNode, TruffleObject compiledRegexObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, compiledRegexObject, Props.CompiledRegex.GROUPS);
    }

    public static TruffleObject readExecMethod(Node readNode, TruffleObject compiledRegexObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, compiledRegexObject, Props.CompiledRegex.EXEC);
    }

    public static TruffleObject execExecMethod(Node execNode, TruffleObject compiledRegexExecMethodObject, String input, long fromIndex) {
        try {
            return (TruffleObject) ForeignAccess.sendExecute(execNode, compiledRegexExecMethodObject, input, fromIndex);
        } catch (UnsupportedTypeException | UnsupportedMessageException | ArityException e) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(e);
        }
    }

    public static String readFlagsSource(Node readNode, TruffleObject regexFlagsObject) {
        return (String) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.SOURCE);
    }

    public static boolean readGlobalFlag(Node readNode, TruffleObject regexFlagsObject) {
        return (boolean) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.GLOBAL);
    }

    public static boolean readMultiLineFlag(Node readNode, TruffleObject regexFlagsObject) {
        return (boolean) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.MULTILINE);
    }

    public static boolean readIgnoreCaseFlag(Node readNode, TruffleObject regexFlagsObject) {
        return (boolean) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.IGNORE_CASE);
    }

    public static boolean readStickyFlag(Node readNode, TruffleObject regexFlagsObject) {
        return (boolean) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.STICKY);
    }

    public static boolean readUnicodeFlag(Node readNode, TruffleObject regexFlagsObject) {
        return (boolean) readExceptionIsFatal(readNode, regexFlagsObject, Props.Flags.UNICODE);
    }

    public static boolean readResultIsMatch(Node readNode, TruffleObject regexResultObject) {
        return (boolean) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.IS_MATCH);
    }

    public static String readResultInput(Node readNode, TruffleObject regexResultObject) {
        return (String) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.INPUT);
    }

    public static int readResultGroupCount(Node readNode, TruffleObject regexResultObject) {
        return (int) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.GROUP_COUNT);
    }

    public static TruffleObject readResultStart(Node readNode, TruffleObject regexResultObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.START);
    }

    public static TruffleObject readResultEnd(Node readNode, TruffleObject regexResultObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.END);
    }

    public static int readResultIndex(Node readNode, TruffleObject startOrEndIndexArray, int i) {
        return (int) readExceptionIsFatal(readNode, startOrEndIndexArray, i);
    }

    public static int readResultStartIndex(Node readStartNode, Node readIndexNode, TruffleObject regexResultObject, int i) {
        return readResultIndex(readIndexNode, readResultStart(readStartNode, regexResultObject), i);
    }

    public static int readResultEndIndex(Node readEndNode, Node readIndexNode, TruffleObject regexResultObject, int i) {
        return readResultIndex(readIndexNode, readResultEnd(readEndNode, regexResultObject), i);
    }

    public static int readResultMatchLength(
                    Node readStartNode,
                    Node readStartIndexNode,
                    Node readEndNode,
                    Node readEndIndexNode,
                    TruffleObject regexResultObject) {
        return readResultEndIndex(readEndNode, readEndIndexNode, regexResultObject, 0) - readResultStartIndex(readStartNode, readStartIndexNode, regexResultObject, 0);
    }

    public static TruffleObject readResultRegex(Node readNode, TruffleObject regexResultObject) {
        return (TruffleObject) readExceptionIsFatal(readNode, regexResultObject, Props.RegexResult.REGEX);
    }

    public static final class TRegexCompiledRegexAccessor extends Node {

        @Child private Node readPatternNode;
        @Child private Node readFlagsNode;
        @Child private Node readExecMethodNode;
        @Child private Node execExecMethodNode;
        @Child private Node readGroupsNode;

        private TRegexCompiledRegexAccessor() {
        }

        public static TRegexCompiledRegexAccessor create() {
            return new TRegexCompiledRegexAccessor();
        }

        public String pattern(TruffleObject compiledRegexObject) {
            return readPattern(getReadPatternNode(), compiledRegexObject);
        }

        public TruffleObject flags(TruffleObject compiledRegexObject) {
            return readFlags(getReadFlagsNode(), compiledRegexObject);
        }

        public TruffleObject exec(TruffleObject compiledRegexObject, String input, long fromIndex) {
            return execExecMethod(getExecExecMethodNode(), readExecMethod(getReadExecMethodNode(), compiledRegexObject), input, fromIndex);
        }

        public TruffleObject namedCaptureGroups(TruffleObject compiledRegexObject) {
            return readNamedCaptureGroups(getReadGroupsNode(), compiledRegexObject);
        }

        private Node getReadPatternNode() {
            if (readPatternNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readPatternNode = insert(createReadNode());
            }
            return readPatternNode;
        }

        private Node getReadFlagsNode() {
            if (readFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readFlagsNode = insert(createReadNode());
            }
            return readFlagsNode;
        }

        private Node getReadExecMethodNode() {
            if (readExecMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readExecMethodNode = insert(createReadNode());
            }
            return readExecMethodNode;
        }

        private Node getExecExecMethodNode() {
            if (execExecMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                execExecMethodNode = insert(Message.createExecute(2).createNode());
            }
            return execExecMethodNode;
        }

        private Node getReadGroupsNode() {
            if (readGroupsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGroupsNode = insert(createReadNode());
            }
            return readGroupsNode;
        }
    }

    public static final class TRegexFlagsAccessor extends Node {

        @Child private Node readSourceNode;
        @Child private Node readGlobalNode;
        @Child private Node readMultilineNode;
        @Child private Node readIgnoreCaseNode;
        @Child private Node readStickyNode;
        @Child private Node readUnicodeNode;

        private TRegexFlagsAccessor() {
        }

        public static TRegexFlagsAccessor create() {
            return new TRegexFlagsAccessor();
        }

        public String source(TruffleObject regexFlagsObject) {
            return readFlagsSource(getReadSourceNode(), regexFlagsObject);
        }

        public boolean global(TruffleObject regexFlagsObject) {
            return readGlobalFlag(getReadGlobalNode(), regexFlagsObject);
        }

        public boolean multiline(TruffleObject regexFlagsObject) {
            return readMultiLineFlag(getReadMultilineNode(), regexFlagsObject);
        }

        public boolean ignoreCase(TruffleObject regexFlagsObject) {
            return readIgnoreCaseFlag(getReadIgnoreCaseNode(), regexFlagsObject);
        }

        public boolean sticky(TruffleObject regexFlagsObject) {
            return readStickyFlag(getReadStickyNode(), regexFlagsObject);
        }

        public boolean unicode(TruffleObject regexFlagsObject) {
            return readUnicodeFlag(getReadUnicodeNode(), regexFlagsObject);
        }

        private Node getReadSourceNode() {
            if (readSourceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readSourceNode = insert(createReadNode());
            }
            return readSourceNode;
        }

        private Node getReadGlobalNode() {
            if (readGlobalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGlobalNode = insert(createReadNode());
            }
            return readGlobalNode;
        }

        private Node getReadMultilineNode() {
            if (readMultilineNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readMultilineNode = insert(createReadNode());
            }
            return readMultilineNode;
        }

        private Node getReadIgnoreCaseNode() {
            if (readIgnoreCaseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIgnoreCaseNode = insert(createReadNode());
            }
            return readIgnoreCaseNode;
        }

        private Node getReadStickyNode() {
            if (readStickyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readStickyNode = insert(createReadNode());
            }
            return readStickyNode;
        }

        private Node getReadUnicodeNode() {
            if (readUnicodeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readUnicodeNode = insert(createReadNode());
            }
            return readUnicodeNode;
        }
    }

    public static final class TRegexCompiledRegexSingleFlagAccessor extends Node {

        private final String flag;

        @Child private Node readFlagsObjectNode = createReadNode();
        @Child private Node readFlagNode = createReadNode();

        private TRegexCompiledRegexSingleFlagAccessor(String flag) {
            this.flag = flag;
        }

        public static TRegexCompiledRegexSingleFlagAccessor create(String flag) {
            return new TRegexCompiledRegexSingleFlagAccessor(flag);
        }

        public boolean get(TruffleObject compiledRegex) {
            return (boolean) readExceptionIsFatal(readFlagNode, readFlags(readFlagsObjectNode, compiledRegex), flag);
        }
    }

    public static final class TRegexResultAccessor extends Node {

        @Child private Node readIsMatchNode;
        @Child private Node readInputNode;
        @Child private Node readGroupCountNode;
        @Child private Node readStartNode;
        @Child private Node readStartIndexNode;
        @Child private Node readEndNode;
        @Child private Node readEndIndexNode;
        @Child private Node readRegexNode;

        private TRegexResultAccessor() {
        }

        public static TRegexResultAccessor create() {
            return new TRegexResultAccessor();
        }

        public boolean isMatch(TruffleObject regexResultObject) {
            return readResultIsMatch(getReadIsMatchNode(), regexResultObject);
        }

        public String input(TruffleObject regexResultObject) {
            return readResultInput(getReadInputNode(), regexResultObject);
        }

        public int groupCount(TruffleObject regexResultObject) {
            return readResultGroupCount(getReadGroupCountNode(), regexResultObject);
        }

        public int captureGroupStart(TruffleObject regexResultObject, int i) {
            return readResultStartIndex(getReadStartNode(), getReadStartIndexNode(), regexResultObject, i);
        }

        public int captureGroupEnd(TruffleObject regexResultObject, int i) {
            return readResultEndIndex(getReadEndNode(), getReadEndIndexNode(), regexResultObject, i);
        }

        public int captureGroupLength(TruffleObject regexResultObject, int i) {
            return captureGroupEnd(regexResultObject, i) - captureGroupStart(regexResultObject, i);
        }

        public TruffleObject regex(TruffleObject regexResultObject) {
            return readResultRegex(getReadRegexNode(), regexResultObject);
        }

        private Node getReadIsMatchNode() {
            if (readIsMatchNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIsMatchNode = insert(createReadNode());
            }
            return readIsMatchNode;
        }

        private Node getReadInputNode() {
            if (readInputNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readInputNode = insert(createReadNode());
            }
            return readInputNode;
        }

        private Node getReadGroupCountNode() {
            if (readGroupCountNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGroupCountNode = insert(createReadNode());
            }
            return readGroupCountNode;
        }

        private Node getReadStartNode() {
            if (readStartNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readStartNode = insert(createReadNode());
            }
            return readStartNode;
        }

        private Node getReadStartIndexNode() {
            if (readStartIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readStartIndexNode = insert(createReadNode());
            }
            return readStartIndexNode;
        }

        private Node getReadEndNode() {
            if (readEndNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readEndNode = insert(createReadNode());
            }
            return readEndNode;
        }

        private Node getReadEndIndexNode() {
            if (readEndIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readEndIndexNode = insert(createReadNode());
            }
            return readEndIndexNode;
        }

        private Node getReadRegexNode() {
            if (readRegexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readRegexNode = insert(createReadNode());
            }
            return readRegexNode;
        }
    }

    public static final class TRegexMaterializeResultNode extends Node {

        @Child TRegexResultAccessor accessor = TRegexResultAccessor.create();

        private TRegexMaterializeResultNode() {
        }

        public static TRegexMaterializeResultNode create() {
            return new TRegexMaterializeResultNode();
        }

        public Object materializeGroup(TruffleObject regexResult, int i) {
            final String input = accessor.input(regexResult);
            final int beginIndex = accessor.captureGroupStart(regexResult, i);
            if (beginIndex == Constants.CAPTURE_GROUP_NO_MATCH) {
                assert i > 0;
                return Undefined.instance;
            } else {
                return input.substring(beginIndex, accessor.captureGroupEnd(regexResult, i));
            }
        }

        public Object[] materializeFull(TruffleObject regexResult) {
            final int groupCount = accessor.groupCount(regexResult);
            Object[] result = new Object[groupCount];
            for (int i = 0; i < groupCount; i++) {
                result[i] = materializeGroup(regexResult, i);
            }
            return result;
        }
    }
}
