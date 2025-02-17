/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleString.SubstringByteIndexNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil.Props.CompiledRegex;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadBooleanMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadIntArrayMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadMemberNodeGen;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropReadStringMemberNodeGen;
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
            public static final String UNICODE_SETS = "unicodeSets";
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

    public static final class Constants {
        private Constants() {
            // should not be constructed
        }

        public static final int CAPTURE_GROUP_NO_MATCH = -1;
    }

    @GenerateCached
    @GenerateInline(inlineByDefault = true)
    @GenerateUncached
    public abstract static class InteropReadMemberNode extends Node {

        public abstract Object execute(Node node, Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static Object read(Object obj, String key, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.readMember(obj, key);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        @NeverDefault
        public static InteropReadMemberNode create() {
            return InteropReadMemberNodeGen.create();
        }

        public static InteropReadMemberNode getUncached() {
            return InteropReadMemberNodeGen.getUncached();
        }
    }

    @GenerateCached(false)
    @GenerateInline(inlineByDefault = true)
    public abstract static class InteropReadIntMemberNode extends Node {

        public abstract int execute(Node node, Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static int read(Node node, Object obj, String key, @Cached InteropToIntNode coerceNode, @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(node, objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateCached(false)
    @GenerateInline(inlineByDefault = true)
    @GenerateUncached
    public abstract static class InteropReadIntArrayMemberNode extends Node {

        public abstract int[] execute(Node node, Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static int[] read(Node node, Object obj, String key, @Cached InteropToIntNode coerceNode, @CachedLibrary("obj") InteropLibrary objs, @CachedLibrary(limit = "1") InteropLibrary arrays) {
            try {
                Object interopArray = objs.readMember(obj, key);
                int length = (int) arrays.getArraySize(interopArray);
                int[] array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = coerceNode.execute(node, arrays.readArrayElement(interopArray, i));
                }
                return array;
            } catch (UnsupportedMessageException | UnknownIdentifierException | InvalidArrayIndexException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadIntArrayMemberNode getUncached() {
            return InteropReadIntArrayMemberNodeGen.getUncached();
        }
    }

    @GenerateCached
    @GenerateInline(inlineByDefault = true)
    @GenerateUncached
    public abstract static class InteropReadBooleanMemberNode extends Node {

        public abstract boolean execute(Node node, Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static boolean read(Node node, Object obj, String key,
                        @Cached InteropToBooleanNode coerceNode,
                        @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(node, objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        @NeverDefault
        public static InteropReadBooleanMemberNode create() {
            return InteropReadBooleanMemberNodeGen.create();
        }

        public static InteropReadBooleanMemberNode getUncached() {
            return InteropReadBooleanMemberNodeGen.getUncached();
        }
    }

    @GenerateCached(false)
    @GenerateInline
    @GenerateUncached
    public abstract static class InteropReadStringMemberNode extends Node {

        public abstract TruffleString execute(Node node, Object obj, String key);

        @Specialization(guards = "objs.isMemberReadable(obj, key)", limit = "3")
        static TruffleString read(Node node, Object obj, String key,
                        @Cached InteropToStringNode coerceNode,
                        @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return coerceNode.execute(node, objs.readMember(obj, key));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InteropReadStringMemberNode getUncached() {
            return InteropReadStringMemberNodeGen.getUncached();
        }
    }

    @GenerateCached(false)
    @GenerateInline
    @GenerateUncached
    public abstract static class InteropToBooleanNode extends Node {

        public abstract boolean execute(Node node, Object obj);

        @Specialization
        static boolean coerceDirect(boolean obj) {
            return obj;
        }

        @Specialization(guards = "objs.isBoolean(obj)", limit = "3")
        static boolean coerce(Object obj,
                        @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.asBoolean(obj);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateCached
    @GenerateInline(inlineByDefault = true)
    @GenerateUncached
    public abstract static class InteropToIntNode extends Node {

        public abstract int execute(Node node, Object obj);

        @Specialization
        static int coerceDirect(int obj) {
            return obj;
        }

        @Specialization(guards = "objs.fitsInInt(obj)", limit = "3")
        static int coerce(Object obj,
                        @CachedLibrary("obj") InteropLibrary objs) {
            try {
                return objs.asInt(obj);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateCached(false)
    @GenerateInline
    @GenerateUncached
    @ImportStatic(JSGuards.class)
    public abstract static class InteropToStringNode extends Node {

        public abstract TruffleString execute(Node node, Object obj);

        @Specialization
        static TruffleString coerceJavaString(String obj,
                        @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
            return Strings.fromJavaString(fromJavaStringNode, obj);
        }

        @SuppressWarnings("truffle-inlining")
        @Specialization
        static TruffleString coerceDirect(TruffleString obj,
                        @Cached @Shared TruffleString.SwitchEncodingNode switchEncoding) {
            return switchEncoding.execute(obj, TruffleString.Encoding.UTF_16);
        }

        @SuppressWarnings("truffle-inlining")
        @Specialization(guards = {"!isTruffleString(obj)", "objs.isString(obj)"}, limit = "3")
        static TruffleString coerce(Object obj,
                        @CachedLibrary("obj") InteropLibrary objs,
                        @Cached @Shared TruffleString.SwitchEncodingNode switchEncoding) {
            return Strings.interopAsTruffleString(obj, objs, switchEncoding);
        }
    }

    @GenerateCached(false)
    @GenerateInline
    @GenerateUncached
    @ImportStatic(CompiledRegex.class)
    public abstract static class InvokeExecMethodNode extends Node {

        public abstract Object execute(Node node, Object compiledRegex, Object input, long fromIndex);

        @Specialization(guards = "objs.isMemberInvocable(compiledRegex, EXEC)", limit = "3")
        static Object exec(Object compiledRegex, Object input, long fromIndex,
                        @CachedLibrary("compiledRegex") InteropLibrary objs) {
            try {
                return objs.invokeMember(compiledRegex, CompiledRegex.EXEC, input, fromIndex);
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        public static InvokeExecMethodNode getUncached() {
            return InvokeExecMethodNodeGen.getUncached();
        }
    }

    @GenerateCached
    @GenerateInline(inlineByDefault = true)
    @GenerateUncached
    public abstract static class InvokeGetGroupBoundariesMethodNode extends Node {

        public abstract int execute(Node node, Object regexResult, Object method, int groupNumber);

        @Specialization(guards = "objs.isMemberInvocable(regexResult, method)", limit = NUMBER_OF_REGEX_RESULT_TYPES)
        static int exec(Node node, Object regexResult, String method, int groupNumber,
                        @CachedLibrary("regexResult") InteropLibrary objs,
                        @Cached InteropToIntNode toIntNode) {
            try {
                return toIntNode.execute(node, objs.invokeMember(regexResult, method, groupNumber));
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        @NeverDefault
        public static InvokeGetGroupBoundariesMethodNode create() {
            return InvokeGetGroupBoundariesMethodNodeGen.create();
        }

        public static InvokeGetGroupBoundariesMethodNode getUncached() {
            return InvokeGetGroupBoundariesMethodNodeGen.getUncached();
        }
    }

    public static final class TRegexCompiledRegexAccessor {

        private TRegexCompiledRegexAccessor() {
        }

        public static Object pattern(Object compiledRegexObject, Node node, InteropReadStringMemberNode readPattern) {
            return readPattern.execute(node, compiledRegexObject, Props.CompiledRegex.PATTERN);
        }

        public static Object flags(Object compiledRegexObject, Node node, InteropReadMemberNode readFlags) {
            return readFlags.execute(node, compiledRegexObject, Props.CompiledRegex.FLAGS);
        }

        public static Object exec(Object compiledRegexObject, Object input, long fromIndex, Node node, InvokeExecMethodNode invokeExec) {
            return invokeExec.execute(node, compiledRegexObject, input, fromIndex);
        }

        public static int groupCount(Object regexResultObject, Node node, InteropReadIntMemberNode readGroupCount) {
            return readGroupCount.execute(node, regexResultObject, Props.CompiledRegex.GROUP_COUNT);
        }

        public static Object namedCaptureGroups(Object compiledRegexObject, Node node, InteropReadMemberNode readGroups) {
            return readGroups.execute(node, compiledRegexObject, Props.CompiledRegex.GROUPS);
        }
    }

    public static final class TRegexNamedCaptureGroupsAccessor {

        private TRegexNamedCaptureGroupsAccessor() {
        }

        public static boolean hasGroup(Object namedCaptureGroupsMap, TruffleString name, InteropLibrary interop) {
            return interop.isMemberReadable(namedCaptureGroupsMap, Strings.toJavaString(name));
        }

        public static int[] getGroupNumbers(Object namedCaptureGroupsMap, TruffleString name, InteropLibrary libMap, InteropLibrary libArray, InteropToIntNode toIntNode, Node node) {
            try {
                Object interopArray = libMap.readMember(namedCaptureGroupsMap, Strings.toJavaString(name));
                int length = (int) libArray.getArraySize(interopArray);
                int[] array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = toIntNode.execute(node, libArray.readArrayElement(interopArray, i));
                }
                return array;
            } catch (UnsupportedMessageException | UnknownIdentifierException | InvalidArrayIndexException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    public static final class TRegexFlagsAccessor {

        private TRegexFlagsAccessor() {
        }

        public static Object source(Object regexFlagsObject, Node node, InteropReadStringMemberNode readSourceNode) {
            return readSourceNode.execute(node, regexFlagsObject, Props.Flags.SOURCE);
        }

        public static boolean global(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readGlobalNode) {
            return readGlobalNode.execute(node, regexFlagsObject, Props.Flags.GLOBAL);
        }

        public static boolean multiline(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readMultilineNode) {
            return readMultilineNode.execute(node, regexFlagsObject, Props.Flags.MULTILINE);
        }

        public static boolean ignoreCase(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readIgnoreCaseNode) {
            return readIgnoreCaseNode.execute(node, regexFlagsObject, Props.Flags.IGNORE_CASE);
        }

        public static boolean sticky(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readStickyNode) {
            return readStickyNode.execute(node, regexFlagsObject, Props.Flags.STICKY);
        }

        public static boolean unicode(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readUnicodeNode) {
            return readUnicodeNode.execute(node, regexFlagsObject, Props.Flags.UNICODE);
        }

        public static boolean dotAll(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readDotAllNode) {
            return readDotAllNode.execute(node, regexFlagsObject, Props.Flags.DOT_ALL);
        }

        public static boolean hasIndices(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readHasIndicesNode) {
            return readHasIndicesNode.execute(node, regexFlagsObject, Props.Flags.HAS_INDICES);
        }

        public static boolean unicodeSets(Object regexFlagsObject, Node node, InteropReadBooleanMemberNode readUnicodeSetsNode) {
            return readUnicodeSetsNode.execute(node, regexFlagsObject, Props.Flags.UNICODE_SETS);
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class TRegexCompiledRegexSingleFlagAccessorNode extends Node {

        public abstract boolean execute(Node node, Object compiledRegex, String flag);

        @Specialization
        static boolean get(Node node, Object compiledRegex, String flag,
                        @Cached InteropReadMemberNode readFlagsObjectNode,
                        @Cached InteropReadBooleanMemberNode readFlagNode) {
            CompilerAsserts.partialEvaluationConstant(flag);
            return readFlagNode.execute(node, readFlagsObjectNode.execute(node, compiledRegex, Props.CompiledRegex.FLAGS), flag);
        }
    }

    public static final class TRegexResultAccessor {

        private TRegexResultAccessor() {
        }

        public static boolean isMatch(Object result, Node node, TRegexUtil.InteropReadBooleanMemberNode readIsMatch) {
            return readIsMatch.execute(node, result, TRegexUtil.Props.RegexResult.IS_MATCH);
        }

        public static int groupCount(Object compiledRegex, Node node, TRegexUtil.InteropReadIntMemberNode readGroupCount) {
            return readGroupCount.execute(node, compiledRegex, Props.CompiledRegex.GROUP_COUNT);
        }

        public static int captureGroupStart(Object result, int groupNumber, Node node, TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart) {
            return getStart.execute(node, result, TRegexUtil.Props.RegexResult.GET_START, groupNumber);
        }

        public static int captureGroupEnd(Object result, int groupNumber, Node node, TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd) {
            return getEnd.execute(node, result, TRegexUtil.Props.RegexResult.GET_END, groupNumber);
        }

        public static int captureGroupLength(Object regexResultObject, int groupNumber, Node node,
                        TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd) {
            return captureGroupEnd(regexResultObject, groupNumber, node, getStart) - captureGroupStart(regexResultObject, groupNumber, node, getEnd);
        }

    }

    public static final class TRegexMaterializeResult {

        private TRegexMaterializeResult() {
        }

        public static Object materializeGroupUncached(Object regexResult, int i, TruffleString input) {
            return TRegexMaterializeResult.materializeGroup(JavaScriptLanguage.get(null).getJSContext(), regexResult, i, input,
                            null, SubstringByteIndexNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
        }

        public static Object materializeGroupUncached(Object regexResult, int[] indices, TruffleString input) {
            return TRegexMaterializeResult.materializeGroup(JavaScriptLanguage.get(null).getJSContext(), regexResult, indices, input,
                            null, SubstringByteIndexNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
        }

        public static Object materializeGroup(JSContext context, Object regexResult, int i, TruffleString input,
                        Node node, TruffleString.SubstringByteIndexNode substringNode, InvokeGetGroupBoundariesMethodNode getStart, InvokeGetGroupBoundariesMethodNode getEnd) {
            final int beginIndex = TRegexResultAccessor.captureGroupStart(regexResult, i, node, getStart);
            if (beginIndex == Constants.CAPTURE_GROUP_NO_MATCH) {
                assert i > 0;
                return Undefined.instance;
            } else {
                int endIndex = TRegexResultAccessor.captureGroupEnd(regexResult, i, node, getEnd);
                return Strings.substring(context, substringNode, input, beginIndex, endIndex - beginIndex);
            }
        }

        public static Object materializeGroup(JSContext context, Object regexResult, int[] indices, TruffleString input,
                        Node node, TruffleString.SubstringByteIndexNode substringNode, InvokeGetGroupBoundariesMethodNode getStart, InvokeGetGroupBoundariesMethodNode getEnd) {
            for (int i : indices) {
                final int beginIndex = TRegexResultAccessor.captureGroupStart(regexResult, i, node, getStart);
                if (beginIndex != Constants.CAPTURE_GROUP_NO_MATCH) {
                    int endIndex = TRegexResultAccessor.captureGroupEnd(regexResult, i, node, getEnd);
                    return Strings.substring(context, substringNode, input, beginIndex, endIndex - beginIndex);
                }
            }
            return Undefined.instance;
        }

        public static Object[] materializeFull(JSContext context, Object regexResult, int groupCount, TruffleString input,
                        Node node, TruffleString.SubstringByteIndexNode substringNode, InvokeGetGroupBoundariesMethodNode getStart, InvokeGetGroupBoundariesMethodNode getEnd) {
            Object[] result = new Object[groupCount];
            for (int i = 0; i < groupCount; i++) {
                result[i] = materializeGroup(context, regexResult, i, input, node, substringNode, getStart, getEnd);
            }
            return result;
        }

        public static Object[] materializeFullUncached(Object regexResult, int groupCount, TruffleString input) {
            return materializeFull(JavaScriptLanguage.get(null).getJSContext(), regexResult, groupCount, input,
                            null, SubstringByteIndexNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
        }
    }
}
