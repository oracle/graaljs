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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexCompiledRegexAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

/**
 * Contains builtin methods for {@linkplain JSRegExp}.
 */
public final class RegExpBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpBuiltins.RegExpBuiltin> {

    public static final JSBuiltinsContainer BUILTINS = new RegExpBuiltins();

    protected RegExpBuiltins() {
        super(JSRegExp.CLASS_NAME, RegExpBuiltin.class);
    }

    public enum RegExpBuiltin implements BuiltinEnum<RegExpBuiltin> {

        input(0),
        set_input(1, Strings.INPUT),
        lastMatch(0),
        lastParen(0),
        leftContext(0),
        rightContext(0),
        multiline(0),
        $1(0),
        $2(0),
        $3(0),
        $4(0),
        $5(0),
        $6(0),
        $7(0),
        $8(0),
        $9(0);

        private final TruffleString key;
        private final int length;

        RegExpBuiltin(int length) {
            this.key = Strings.fromJavaString(name());
            this.length = length;
        }

        RegExpBuiltin(int length, TruffleString key) {
            this.key = key;
            this.length = length;
        }

        @Override
        public TruffleString getKey() {
            return key;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return getLength() == 0;
        }

        @Override
        public boolean isSetter() {
            return getLength() == 1;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RegExpBuiltin builtinEnum) {
        switch (builtinEnum) {
            case input:
                return RegExpBuiltinsFactory.JSRegExpStaticResultGetInputNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case set_input:
                return RegExpBuiltinsFactory.JSRegExpStaticResultSetInputNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case lastMatch:
                return RegExpBuiltinsFactory.JSRegExpStaticResultGetGroupNodeGen.create(context, builtin, 0, args().createArgumentNodes(context));
            case lastParen:
                return RegExpBuiltinsFactory.JSRegExpStaticResultLastParenNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case leftContext:
                return RegExpBuiltinsFactory.JSRegExpStaticResultLeftContextNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case rightContext:
                return RegExpBuiltinsFactory.JSRegExpStaticResultRightContextNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case multiline:
                return RegExpBuiltinsFactory.JSRegExpStaticResultMultilineNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case $1:
            case $2:
            case $3:
            case $4:
            case $5:
            case $6:
            case $7:
            case $8:
            case $9:
                return RegExpBuiltinsFactory.JSRegExpStaticResultGetGroupNodeGen.create(context, builtin, builtinEnum.name().charAt(1) - '0', args().createArgumentNodes(context));
            default:
                return null;
        }
    }

    static void checkStaticRegexResultPropertyGet(JSContext context, JSRealm realm, Object thisValue) {
        CompilerAsserts.partialEvaluationConstant(context);
        if (!context.isOptionV8CompatibilityMode()) {
            if (thisValue != realm.getRegExpConstructor() || realm.isRegexResultInvalidated()) {
                throw Errors.createTypeError("Static RegExp result properties cannot be used with subclasses of RegExp.");
            }
        }
    }

    static void checkStaticRegexResultPropertySet(JSContext context, JSRealm realm, Object thisValue) {
        CompilerAsserts.partialEvaluationConstant(context);
        if (!context.isOptionV8CompatibilityMode()) {
            if (thisValue != realm.getRegExpConstructor()) {
                throw Errors.createTypeError("Static RegExp result properties cannot be used with subclasses of RegExp.");
            }
        }
    }

    @GenerateInline
    @GenerateCached(false)
    abstract static class GetStaticRegExpResultNode extends JavaScriptBaseNode {

        GetStaticRegExpResultNode() {
        }

        abstract Object execute(Node node);

        @Specialization
        static Object get(Node node,
                        @Cached TRegexUtil.InvokeExecMethodNode invokeExec) {
            return JSRealm.get(node).getStaticRegexResult(JavaScriptLanguage.get(node).getJSContext(), node, invokeExec);
        }
    }

    abstract static class JSRegExpStaticResultGetInputNode extends JSBuiltinNode {

        JSRegExpStaticResultGetInputNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        TruffleString getInputProp(VirtualFrame frame) {
            checkStaticRegexResultPropertyGet(getContext(), getRealm(), JSFrameUtil.getThisObj(frame));
            return getRealm().getStaticRegexResultInputString();
        }
    }

    abstract static class JSRegExpStaticResultSetInputNode extends JSBuiltinNode {

        @Child private JSToStringNode toStringNode = JSToStringNode.create();

        JSRegExpStaticResultSetInputNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        Object setInputProp(VirtualFrame frame, Object val) {
            checkStaticRegexResultPropertySet(getContext(), getRealm(), JSFrameUtil.getThisObj(frame));
            getRealm().setStaticRegexResultInputString(toStringNode.executeString(val));
            return Undefined.instance;
        }
    }

    /**
     * Seems to be a leftover even in the nashorn implementation. QUOTE:
     *
     * <pre>
     * &#64;Getter(where = Where.CONSTRUCTOR, attributes = Attribute.CONSTANT, name = "multiline")
     * public static Object getLastMultiline(final Object self) {
     *     return false; // doesn't ever seem to become true and isn't documented anyhwere
     * }
     * </pre>
     */
    abstract static class JSRegExpStaticResultMultilineNode extends JSBuiltinNode {

        JSRegExpStaticResultMultilineNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "getContext().isOptionNashornCompatibilityMode()")
        static boolean getMultilineNashorn() {
            return false;
        }

        @Specialization(guards = "!getContext().isOptionNashornCompatibilityMode()", assumptions = "getContext().getRegExpStaticResultUnusedAssumption()")
        boolean getMultilineLazy(
                        @Bind Node node,
                        @Cached @Shared @SuppressWarnings("unused") TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached @Shared TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getMultilineFlag) {
            Object compiledRegex = getRealm().getStaticRegexResultCompiledRegex();
            if (compiledRegex != null) {
                return getMultilineFlag.execute(node, compiledRegex, TRegexUtil.Props.Flags.MULTILINE);
            } else {
                return false;
            }
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!getContext().isOptionNashornCompatibilityMode()")
        boolean getMultilineEager(
                        @Bind Node node,
                        @Cached GetStaticRegExpResultNode getResultNode,
                        @Cached @Shared TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached @Shared TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getMultilineFlag) {
            Object compiledRegex = getRealm().getStaticRegexResultCompiledRegex();
            Object result = getResultNode.execute(node);
            if (TRegexResultAccessor.isMatch(result, node, readIsMatch)) {
                return getMultilineFlag.execute(node, compiledRegex, TRegexUtil.Props.Flags.MULTILINE);
            } else {
                return false;
            }
        }
    }

    abstract static class JSRegExpStaticResultPropertyNode extends JSBuiltinNode {

        JSRegExpStaticResultPropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final TruffleString getInput() {
            return getRealm().getStaticRegexResultOriginalInputString();
        }

        protected final boolean isMatchResult(Object result, TRegexUtil.InteropReadBooleanMemberNode readIsMatch) {
            return TRegexResultAccessor.isMatch(result, this, readIsMatch);
        }

        protected final int groupCount(Object compiledRegex, TRegexUtil.InteropReadIntMemberNode readGroupCount) {
            return TRegexCompiledRegexAccessor.groupCount(compiledRegex, this, readGroupCount);
        }

        protected final int captureGroupStart(Object result, int groupNumber, TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart) {
            return TRegexResultAccessor.captureGroupStart(result, groupNumber, this, getStart);
        }

        protected final int captureGroupEnd(Object result, int groupNumber, TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd) {
            return TRegexResultAccessor.captureGroupEnd(result, groupNumber, this, getEnd);
        }

    }

    abstract static class JSRegExpStaticResultGetGroupNode extends JSRegExpStaticResultPropertyNode {

        private final int groupNumber;

        JSRegExpStaticResultGetGroupNode(JSContext context, JSBuiltin builtin, int groupNumber) {
            super(context, builtin);
            assert groupNumber >= 0;
            this.groupNumber = groupNumber;
        }

        @Specialization
        TruffleString getGroup(VirtualFrame frame,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InteropReadIntMemberNode readGroupCount,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                        @Cached GetStaticRegExpResultNode getResultNode) {
            JSRealm realm = getRealm();
            checkStaticRegexResultPropertyGet(getContext(), realm, JSFrameUtil.getThisObj(frame));
            Object result = getResultNode.execute(this);
            if (isMatchResult(result, readIsMatch) && groupCount(realm.getStaticRegexResultCompiledRegex(), readGroupCount) > groupNumber) {
                int start = captureGroupStart(result, groupNumber, getStart);
                if (start >= 0) {
                    return Strings.substring(getContext(), substringNode, getInput(), start, captureGroupEnd(result, groupNumber, getEnd) - start);
                }
            }
            return Strings.EMPTY_STRING;
        }

    }

    abstract static class JSRegExpStaticResultLastParenNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultLastParenNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        TruffleString lastParen(VirtualFrame frame,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InteropReadIntMemberNode readGroupCount,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                        @Cached GetStaticRegExpResultNode getResultNode) {
            JSRealm realm = getRealm();
            checkStaticRegexResultPropertyGet(getContext(), realm, JSFrameUtil.getThisObj(frame));
            Object result = getResultNode.execute(this);
            if (isMatchResult(result, readIsMatch)) {
                int groupNumber = groupCount(realm.getStaticRegexResultCompiledRegex(), readGroupCount) - 1;
                if (groupNumber > 0) {
                    int start = captureGroupStart(result, groupNumber, getStart);
                    if (start >= 0) {
                        return Strings.substring(getContext(), substringNode, getInput(), start, captureGroupEnd(result, groupNumber, getEnd) - start);
                    }
                }
            }
            return Strings.EMPTY_STRING;
        }
    }

    abstract static class JSRegExpStaticResultLeftContextNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultLeftContextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        TruffleString leftContext(VirtualFrame frame,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        @Cached GetStaticRegExpResultNode getResultNode) {
            checkStaticRegexResultPropertyGet(getContext(), getRealm(), JSFrameUtil.getThisObj(frame));
            Object result = getResultNode.execute(this);
            if (isMatchResult(result, readIsMatch)) {
                int start = captureGroupStart(result, 0, getStart);
                return Strings.substring(getContext(), substringNode, getInput(), 0, start);
            } else {
                return Strings.EMPTY_STRING;
            }
        }
    }

    abstract static class JSRegExpStaticResultRightContextNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultRightContextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        TruffleString rightContext(VirtualFrame frame,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                        @Cached GetStaticRegExpResultNode getResultNode) {
            checkStaticRegexResultPropertyGet(getContext(), getRealm(), JSFrameUtil.getThisObj(frame));
            Object result = getResultNode.execute(this);
            if (isMatchResult(result, readIsMatch)) {
                int end = captureGroupEnd(result, 0, getEnd);
                return Strings.substring(getContext(), substringNode, getInput(), end);
            } else {
                return Strings.EMPTY_STRING;
            }
        }
    }
}
