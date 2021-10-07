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
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil.Props.CompiledRegex;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropIsMemberReadableNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadBooleanMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadIntMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadStringMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropToIntNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InvokeExecMethodNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InvokeGetGroupBoundariesMethodNodeGen;

public final class TRegexUtil {

    private TRegexUtil() {
        // should not be constructed
    }

    public static final class Props {
        private Props() {
            // should not be constructed
        }

        public static final class CompiledRegex {
            private CompiledRegex() {
                // should not be constructed
            }

            public static final String PATTERN = "pattern";
            public static final String FLAGS = "flags";
            public static final String EXEC = "exec";
            public static final String GROUP_COUNT = "groupCount";
            public static final String GROUPS = "groups";
        }

        public static final class Flags {
            private Flags() {
                // should not be constructed
            }

            public static final String SOURCE = "source";
            public static final String GLOBAL = "global";
            public static final String MULTILINE = "multiline";
            public static final String IGNORE_CASE = "ignoreCase";
            public static final String STICKY = "sticky";
            public static final String UNICODE = "unicode";
            public static final String DOT_ALL = "dotAll";
            public static final String HAS_INDICES = "hasIndices";
        }

        public static final class RegexResult {
            private RegexResult() {
                // should not be constructed
            }

            public static final String IS_MATCH = "isMatch";
            public static final String GET_START = "getStart";
            public static final String GET_END = "getEnd";
        }
    }

    private static final String NUMBER_OF_REGEX_RESULT_TYPES = "9";
    private static final int NUMBER_OF_REGEX_RESULT_TYPES_INT = 9;

    public static final class Constants {
        private Constants() {
            // should not be constructed
        }

        public static final int CAPTURE_GROUP_NO_MATCH = -1;
    }

    @GenerateUncached
    public abstract static class InteropIsMemberReadableNode extends Node {

        public abstract boolean execute(Object obj, String key);

        @Specialization(limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static boolean read(Object obj, String key, @CachedLibrary("obj") InteropLibrary objs) {
            return objs.isMemberReadable(obj, key);
        }

        public static InteropIsMemberReadableNode create() {
            return InteropIsMemberReadableNodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class InteropReadMemberNode extends Node {

        public abstract Object execute(Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static Object read(Object obj, String key, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.readMember(obj, key);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadMemberNode create() {
            return InteropReadMemberNodeGen.create();
        }

        public static InteropReadMemberNode getUncached() {
            return InteropReadMemberNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class InteropReadIntMemberNode extends Node {

        public abstract int execute(Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static int read(Object obj, String key, @Cached InteropToIntNode coerceNode, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadIntMemberNode create() {
            return InteropReadIntMemberNodeGen.create();
        }

        public static InteropReadIntMemberNode getUncached() {
            return InteropReadIntMemberNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class InteropReadBooleanMemberNode extends Node {

        public abstract boolean execute(Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static boolean read(Object obj, String key, @Cached InteropToBooleanNode coerceNode, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadBooleanMemberNode create() {
            return InteropReadBooleanMemberNodeGen.create();
        }

        public static InteropReadBooleanMemberNode getUncached() {
            return InteropReadBooleanMemberNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class InteropReadStringMemberNode extends Node {

        public abstract String execute(Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = "3")
        static String read(Object obj, String key, @Cached InteropToStringNode coerceNode, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadStringMemberNode create() {
            return InteropReadStringMemberNodeGen.create();
        }

        public static InteropReadStringMemberNode getUncached() {
            return InteropReadStringMemberNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class InteropToBooleanNode extends Node {

        public abstract boolean execute(Object obj);

        @Specialization
        static boolean coerceDirect(boolean obj) {
            return obj;
        }

        @Specialization(guards = "objs.isBoolean(obj)", limit = "3")
        static boolean coerce(Object obj, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.asBoolean(obj);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateUncached
    public abstract static class InteropToIntNode extends Node {

        public abstract int execute(Object obj);

        @Specialization
        static int coerceDirect(int obj) {
            return obj;
        }

        @Specialization(guards = "objs.fitsInInt(obj)", limit = "3")
        static int coerce(Object obj, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.asInt(obj);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateUncached
    public abstract static class InteropToStringNode extends Node {

        public abstract String execute(Object obj);

        @Specialization
        static String coerceDirect(String obj) {
            return obj;
        }

        @Specialization(guards = "objs.isString(obj)", limit = "3")
        static String coerce(Object obj, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.asString(obj);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @ImportStatic(CompiledRegex.class)
    @GenerateUncached
    public abstract static class InvokeExecMethodNode extends Node {

        public abstract Object execute(Object compiledRegex, String input, long fromIndex);

        @Specialization(guards = "objs.isMemberInvocable(compiledRegex, EXEC)", limit = "3")
        static Object exec(Object compiledRegex, String input, long fromIndex,
                        @CachedLibrary("compiledRegex") InteropLibrary objs) {
            try {
                return objs.invokeMember(compiledRegex, CompiledRegex.EXEC, input, fromIndex);
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InvokeExecMethodNode create() {
            return InvokeExecMethodNodeGen.create();
        }

        public static InvokeExecMethodNode getUncached() {
            return InvokeExecMethodNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class InvokeGetGroupBoundariesMethodNode extends Node {

        public abstract int execute(Object regexResult, String method, int groupNumber);

        @Specialization(guards = "objs.isMemberInvocable(regexResult, method)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static int exec(Object regexResult, String method, int groupNumber,
                        @CachedLibrary("regexResult") InteropLibrary objs,
                        @Cached InteropToIntNode toIntNode) {
            try {
                return toIntNode.execute(objs.invokeMember(regexResult, method, groupNumber));
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InvokeGetGroupBoundariesMethodNode create() {
            return InvokeGetGroupBoundariesMethodNodeGen.create();
        }

        public static InvokeGetGroupBoundariesMethodNode getUncached() {
            return InvokeGetGroupBoundariesMethodNodeGen.getUncached();
        }
    }

    public static final class TRegexCompiledRegexAccessor extends Node {

        @Child private InteropReadStringMemberNode readPatternNode;
        @Child private InteropReadMemberNode readFlagsNode;
        @Child private InvokeExecMethodNode invokeExecMethodNode;
        @Child private InteropReadIntMemberNode readGroupCountNode;
        @Child private InteropReadMemberNode readGroupsNode;

        private TRegexCompiledRegexAccessor() {
        }

        public static TRegexCompiledRegexAccessor create() {
            return new TRegexCompiledRegexAccessor();
        }

        public String pattern(Object compiledRegexObject) {
            return getReadPatternNode().execute(compiledRegexObject, Props.CompiledRegex.PATTERN);
        }

        public Object flags(Object compiledRegexObject) {
            return getReadFlagsNode().execute(compiledRegexObject, Props.CompiledRegex.FLAGS);
        }

        public Object exec(Object compiledRegexObject, String input, long fromIndex) {
            return getInvokeExecMethodNode().execute(compiledRegexObject, input, fromIndex);
        }

        public int groupCount(Object regexResultObject) {
            return getReadGroupCountNode().execute(regexResultObject, Props.CompiledRegex.GROUP_COUNT);
        }

        public Object namedCaptureGroups(Object compiledRegexObject) {
            return getReadGroupsNode().execute(compiledRegexObject, Props.CompiledRegex.GROUPS);
        }

        private InteropReadStringMemberNode getReadPatternNode() {
            if (readPatternNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readPatternNode = insert(InteropReadStringMemberNode.create());
            }
            return readPatternNode;
        }

        private InteropReadMemberNode getReadFlagsNode() {
            if (readFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readFlagsNode = insert(InteropReadMemberNode.create());
            }
            return readFlagsNode;
        }

        private InvokeExecMethodNode getInvokeExecMethodNode() {
            if (invokeExecMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                invokeExecMethodNode = insert(InvokeExecMethodNode.create());
            }
            return invokeExecMethodNode;
        }

        private InteropReadIntMemberNode getReadGroupCountNode() {
            if (readGroupCountNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGroupCountNode = insert(InteropReadIntMemberNode.create());
            }
            return readGroupCountNode;
        }

        private InteropReadMemberNode getReadGroupsNode() {
            if (readGroupsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGroupsNode = insert(InteropReadMemberNode.create());
            }
            return readGroupsNode;
        }
    }

    public static final class TRegexNamedCaptureGroupsAccessor extends Node {

        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(NUMBER_OF_REGEX_RESULT_TYPES_INT);
        @Child private InteropToIntNode toIntNode;

        private TRegexNamedCaptureGroupsAccessor() {
        }

        public static TRegexNamedCaptureGroupsAccessor create() {
            return new TRegexNamedCaptureGroupsAccessor();
        }

        public boolean isNull(Object namedCaptureGroupsMap) {
            return interop.isNull(namedCaptureGroupsMap);
        }

        public boolean hasGroup(Object namedCaptureGroupsMap, String name) {
            return interop.isMemberReadable(namedCaptureGroupsMap, name);
        }

        public int getGroupNumber(Object namedCaptureGroupsMap, String name) {
            InteropToIntNode toInt = toIntNode;
            if (toInt == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = toInt = insert(InteropToIntNodeGen.create());
            }
            try {
                return toInt.execute(interop.readMember(namedCaptureGroupsMap, name));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    public static final class TRegexFlagsAccessor extends Node {

        @Child private InteropReadStringMemberNode readSourceNode;
        @Child private InteropReadBooleanMemberNode readGlobalNode;
        @Child private InteropReadBooleanMemberNode readMultilineNode;
        @Child private InteropReadBooleanMemberNode readIgnoreCaseNode;
        @Child private InteropReadBooleanMemberNode readStickyNode;
        @Child private InteropReadBooleanMemberNode readUnicodeNode;
        @Child private InteropReadBooleanMemberNode readDotAllNode;
        @Child private InteropReadBooleanMemberNode readHasIndicesNode;

        private TRegexFlagsAccessor() {
        }

        public static TRegexFlagsAccessor create() {
            return new TRegexFlagsAccessor();
        }

        public String source(Object regexFlagsObject) {
            return getReadSourceNode().execute(regexFlagsObject, Props.Flags.SOURCE);
        }

        public boolean global(Object regexFlagsObject) {
            return getReadGlobalNode().execute(regexFlagsObject, Props.Flags.GLOBAL);
        }

        public boolean multiline(Object regexFlagsObject) {
            return getReadMultilineNode().execute(regexFlagsObject, Props.Flags.MULTILINE);
        }

        public boolean ignoreCase(Object regexFlagsObject) {
            return getReadIgnoreCaseNode().execute(regexFlagsObject, Props.Flags.IGNORE_CASE);
        }

        public boolean sticky(Object regexFlagsObject) {
            return getReadStickyNode().execute(regexFlagsObject, Props.Flags.STICKY);
        }

        public boolean unicode(Object regexFlagsObject) {
            return getReadUnicodeNode().execute(regexFlagsObject, Props.Flags.UNICODE);
        }

        public boolean dotAll(Object regexFlagsObject) {
            return getReadDotAllNode().execute(regexFlagsObject, Props.Flags.DOT_ALL);
        }

        public boolean hasIndices(Object regexFlagsObject) {
            return getReadHasIndicesNode().execute(regexFlagsObject, Props.Flags.HAS_INDICES);
        }

        private InteropReadStringMemberNode getReadSourceNode() {
            if (readSourceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readSourceNode = insert(InteropReadStringMemberNode.create());
            }
            return readSourceNode;
        }

        private InteropReadBooleanMemberNode getReadGlobalNode() {
            if (readGlobalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readGlobalNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readGlobalNode;
        }

        private InteropReadBooleanMemberNode getReadMultilineNode() {
            if (readMultilineNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readMultilineNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readMultilineNode;
        }

        private InteropReadBooleanMemberNode getReadIgnoreCaseNode() {
            if (readIgnoreCaseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIgnoreCaseNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readIgnoreCaseNode;
        }

        private InteropReadBooleanMemberNode getReadStickyNode() {
            if (readStickyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readStickyNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readStickyNode;
        }

        private InteropReadBooleanMemberNode getReadUnicodeNode() {
            if (readUnicodeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readUnicodeNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readUnicodeNode;
        }

        private InteropReadBooleanMemberNode getReadDotAllNode() {
            if (readDotAllNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readDotAllNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readDotAllNode;
        }

        private InteropReadBooleanMemberNode getReadHasIndicesNode() {
            if (readHasIndicesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readHasIndicesNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readHasIndicesNode;
        }
    }

    public static final class TRegexCompiledRegexSingleFlagAccessor extends Node {

        private final String flag;

        @Child private InteropReadMemberNode readFlagsObjectNode = InteropReadMemberNode.create();
        @Child private InteropReadBooleanMemberNode readFlagNode = InteropReadBooleanMemberNode.create();

        private TRegexCompiledRegexSingleFlagAccessor(String flag) {
            this.flag = flag;
        }

        public static TRegexCompiledRegexSingleFlagAccessor create(String flag) {
            return new TRegexCompiledRegexSingleFlagAccessor(flag);
        }

        public boolean get(Object compiledRegex) {
            return readFlagNode.execute(readFlagsObjectNode.execute(compiledRegex, Props.CompiledRegex.FLAGS), flag);
        }
    }

    public static final class TRegexResultAccessor extends Node {

        private static final TRegexResultAccessor UNCACHED = new TRegexResultAccessor(false);

        @Child private InteropReadBooleanMemberNode readIsMatchNode;
        @Child private InvokeGetGroupBoundariesMethodNode getStartNode;
        @Child private InvokeGetGroupBoundariesMethodNode getEndNode;

        private TRegexResultAccessor(boolean cached) {
            if (!cached) {
                readIsMatchNode = InteropReadBooleanMemberNodeGen.getUncached();
                getStartNode = InvokeGetGroupBoundariesMethodNodeGen.getUncached();
                getEndNode = InvokeGetGroupBoundariesMethodNodeGen.getUncached();
            }
        }

        public static TRegexResultAccessor create() {
            return new TRegexResultAccessor(true);
        }

        public static TRegexResultAccessor getUncached() {
            return UNCACHED;
        }

        public boolean isMatch(Object regexResultObject) {
            return getReadIsMatchNode().execute(regexResultObject, Props.RegexResult.IS_MATCH);
        }

        public int captureGroupStart(Object regexResultObject, int i) {
            return getGetStartNode().execute(regexResultObject, Props.RegexResult.GET_START, i);
        }

        public int captureGroupEnd(Object regexResultObject, int i) {
            return getGetEndNode().execute(regexResultObject, Props.RegexResult.GET_END, i);
        }

        public int captureGroupLength(Object regexResultObject, int i) {
            return captureGroupEnd(regexResultObject, i) - captureGroupStart(regexResultObject, i);
        }

        private InteropReadBooleanMemberNode getReadIsMatchNode() {
            if (readIsMatchNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIsMatchNode = insert(InteropReadBooleanMemberNode.create());
            }
            return readIsMatchNode;
        }

        private InvokeGetGroupBoundariesMethodNode getGetStartNode() {
            if (getStartNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getStartNode = insert(InvokeGetGroupBoundariesMethodNode.create());
            }
            return getStartNode;
        }

        private InvokeGetGroupBoundariesMethodNode getGetEndNode() {
            if (getEndNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getEndNode = insert(InvokeGetGroupBoundariesMethodNode.create());
            }
            return getEndNode;
        }
    }

    public static final class TRegexMaterializeResultNode extends Node {

        private static final TRegexMaterializeResultNode UNCACHED = new TRegexMaterializeResultNode(false);

        @Child TRegexResultAccessor resultAccessor;

        private TRegexMaterializeResultNode(boolean cached) {
            resultAccessor = cached ? TRegexResultAccessor.create() : TRegexResultAccessor.getUncached();
        }

        public static TRegexMaterializeResultNode create() {
            return new TRegexMaterializeResultNode(true);
        }

        public static TRegexMaterializeResultNode getUncached() {
            return UNCACHED;
        }

        public Object materializeGroup(Object regexResult, int i, String input) {
            return materializeGroup(resultAccessor, regexResult, i, input);
        }

        public static Object materializeGroup(TRegexResultAccessor accessor, Object regexResult, int i, String input) {
            final int beginIndex = accessor.captureGroupStart(regexResult, i);
            if (beginIndex == Constants.CAPTURE_GROUP_NO_MATCH) {
                assert i > 0;
                return Undefined.instance;
            } else {
                return input.substring(beginIndex, accessor.captureGroupEnd(regexResult, i));
            }
        }

        public Object[] materializeFull(Object regexResult, int groupCount, String input) {
            Object[] result = new Object[groupCount];
            for (int i = 0; i < groupCount; i++) {
                result[i] = materializeGroup(regexResult, i, input);
            }
            return result;
        }
    }
}
