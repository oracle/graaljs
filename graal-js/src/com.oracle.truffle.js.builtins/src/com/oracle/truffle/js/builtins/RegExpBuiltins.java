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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Contains builtin methods for {@linkplain JSRegExp}.
 */
public final class RegExpBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpBuiltins.RegExpBuiltin> {

    RegExpBuiltins() {
        super(JSRegExp.CLASS_NAME, RegExpBuiltin.class);
    }

    public enum RegExpBuiltin implements BuiltinEnum<RegExpBuiltin> {

        input,
        lastMatch,
        lastParen,
        leftContext,
        rightContext,
        multiline,
        $1,
        $2,
        $3,
        $4,
        $5,
        $6,
        $7,
        $8,
        $9;

        private final String key;

        RegExpBuiltin() {
            this.key = "get " + name();
        }

        RegExpBuiltin(String key) {
            this.key = "get " + key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public int getLength() {
            return 0;
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RegExpBuiltin builtinEnum) {
        switch (builtinEnum) {
            case input:
                return RegExpBuiltinsFactory.JSRegExpStaticResultInputNodeGen.create(context, builtin, args().createArgumentNodes(context));
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

    abstract static class GetStaticRegExpResultNode extends Node {

        private final JSContext context;
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();

        GetStaticRegExpResultNode(JSContext context) {
            this.context = context;
        }

        static GetStaticRegExpResultNode create(JSContext context) {
            return RegExpBuiltinsFactory.GetStaticRegExpResultNodeGen.create(context);
        }

        abstract TruffleObject execute();

        @Specialization
        TruffleObject get() {
            JSRealm realm = context.getRealm();
            TruffleObject compiledRegex = realm.getLazyStaticRegexResultCompiledRegex();
            String input = realm.getLazyStaticRegexResultInputString();
            long fromIndex = realm.getLazyStaticRegexResultFromIndex();
            realm.switchToEagerStaticRegExpResults();
            if (compiledRegex != null) {
                TruffleObject result = compiledRegexAccessor.exec(compiledRegex, input, fromIndex);
                realm.setRegexResult(result);
            }
            return realm.getRegexResult();
        }
    }

    abstract static class JSRegExpStaticResultInputNode extends JSBuiltinNode {

        JSRegExpStaticResultInputNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(assumptions = "getStaticResultUnusedAssumption()")
        String getInputLazy() {
            return getContext().getRealm().getLazyStaticRegexResultInputString();
        }

        @Specialization
        String getInputEager(@Cached("createGetResultNode()") GetStaticRegExpResultNode getResultNode,
                        @Cached("create()") TRegexUtil.TRegexResultAccessor resultAccessor) {
            TruffleObject result = getResultNode.execute();
            return resultAccessor.isMatch(result) ? resultAccessor.input(result) : "";
        }

        Assumption getStaticResultUnusedAssumption() {
            return getContext().getRegExpStaticResultUnusedAssumption();
        }

        GetStaticRegExpResultNode createGetResultNode() {
            return GetStaticRegExpResultNode.create(getContext());
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

        @Child private TRegexUtil.TRegexCompiledRegexSingleFlagAccessor multilineAccessor = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(TRegexUtil.Props.Flags.MULTILINE);

        JSRegExpStaticResultMultilineNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(assumptions = "getStaticResultUnusedAssumption()")
        boolean getMultilineLazy() {
            TruffleObject compiledRegex = getContext().getRealm().getLazyStaticRegexResultCompiledRegex();
            if (!JSTruffleOptions.NashornCompatibilityMode && compiledRegex != null) {
                return multilineAccessor.get(compiledRegex);
            } else {
                return false;
            }
        }

        @Specialization
        boolean getMultilineEager(@Cached("createGetResultNode()") GetStaticRegExpResultNode getResultNode,
                        @Cached("create()") TRegexUtil.TRegexResultAccessor resultAccessor) {
            TruffleObject result = getResultNode.execute();
            if (!JSTruffleOptions.NashornCompatibilityMode && resultAccessor.isMatch(result)) {
                return multilineAccessor.get(resultAccessor.regex(result));
            } else {
                return false;
            }
        }

        Assumption getStaticResultUnusedAssumption() {
            return getContext().getRegExpStaticResultUnusedAssumption();
        }

        GetStaticRegExpResultNode createGetResultNode() {
            return GetStaticRegExpResultNode.create(getContext());
        }
    }

    abstract static class JSRegExpStaticResultPropertyNode extends JSBuiltinNode {

        @Child GetStaticRegExpResultNode getResultNode;
        @Child TRegexUtil.TRegexResultAccessor resultAccessor;

        JSRegExpStaticResultPropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            getResultNode = GetStaticRegExpResultNode.create(context);
            resultAccessor = TRegexUtil.TRegexResultAccessor.create();
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
        String getGroup() {
            TruffleObject result = getResultNode.execute();
            if (resultAccessor.isMatch(result) && resultAccessor.groupCount(result) > groupNumber) {
                int start = resultAccessor.captureGroupStart(result, groupNumber);
                if (start >= 0) {
                    return Boundaries.substring(resultAccessor.input(result), start, resultAccessor.captureGroupEnd(result, groupNumber));
                }
            }
            return "";
        }
    }

    abstract static class JSRegExpStaticResultLastParenNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultLastParenNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        String lastParen() {
            TruffleObject result = getResultNode.execute();
            if (resultAccessor.isMatch(result)) {
                int groupNumber = resultAccessor.groupCount(result) - 1;
                if (groupNumber > 0) {
                    int start = resultAccessor.captureGroupStart(result, groupNumber);
                    if (start >= 0) {
                        return Boundaries.substring(resultAccessor.input(result), start, resultAccessor.captureGroupEnd(result, groupNumber));
                    }
                }
            }
            return "";
        }
    }

    abstract static class JSRegExpStaticResultLeftContextNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultLeftContextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        String leftContext() {
            TruffleObject result = getResultNode.execute();
            if (resultAccessor.isMatch(result)) {
                int start = resultAccessor.captureGroupStart(result, 0);
                return Boundaries.substring(resultAccessor.input(result), 0, start);
            } else {
                return "";
            }
        }
    }

    abstract static class JSRegExpStaticResultRightContextNode extends JSRegExpStaticResultPropertyNode {

        JSRegExpStaticResultRightContextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        String rightContext() {
            TruffleObject result = getResultNode.execute();
            if (resultAccessor.isMatch(result)) {
                int end = resultAccessor.captureGroupEnd(result, 0);
                return Boundaries.substring(resultAccessor.input(result), end);
            } else {
                return "";
            }
        }
    }
}
