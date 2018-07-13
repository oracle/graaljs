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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpCompileNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpExecES5NodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpExecNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpMatchAllNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpMatchNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpReplaceNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpSearchNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpSplitNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpTestNodeGen;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpToStringNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltins.MatchAllIteratorNode;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode.JSRegExpExecBuiltinNode;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DelimitedStringBuilder;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Contains builtin methods for {@linkplain JSRegExp}.prototype.
 */
public final class RegExpPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpPrototypeBuiltins.RegExpPrototype> {

    protected RegExpPrototypeBuiltins() {
        super(JSRegExp.PROTOTYPE_NAME, RegExpPrototype.class);
    }

    public enum RegExpPrototype implements BuiltinEnum<RegExpPrototype> {
        exec(1),
        test(1),
        toString(0),

        // ES6
        _match(1, Symbol.SYMBOL_MATCH),
        _replace(2, Symbol.SYMBOL_REPLACE),
        _search(1, Symbol.SYMBOL_SEARCH),
        _split(2, Symbol.SYMBOL_SPLIT),

        // Annex B
        compile(2),

        // TBD
        _matchAll(1, Symbol.SYMBOL_MATCH_ALL);

        private final int length;
        private final Object key;

        RegExpPrototype(int length) {
            this(length, null);
        }

        RegExpPrototype(int length, Symbol symbol) {
            this.length = length;
            this.key = symbol;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isAnnexB() {
            return this == compile;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.of(_match, _replace, _search, _split).contains(this)) {
                return 6;
            }
            if (this.equals(_matchAll)) {
                return JSTruffleOptions.ECMAScript2019;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }

        @Override
        public Object getKey() {
            return key != null ? key : BuiltinEnum.super.getKey();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RegExpPrototype builtinEnum) {
        switch (builtinEnum) {
            case exec:
                if (context.getEcmaScriptVersion() >= 6) {
                    return JSRegExpExecNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSRegExpExecES5NodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                }
            case test:
                return JSRegExpTestNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSRegExpToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case _match:
                return JSRegExpMatchNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case _replace:
                return JSRegExpReplaceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case _search:
                return JSRegExpSearchNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case _split:
                return JSRegExpSplitNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case compile:
                return JSRegExpCompileNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case _matchAll:
                return JSRegExpMatchAllNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    @TruffleBoundary
    private static JSException createNoRegExpError(Object obj) {
        String objName = String.valueOf(JSRuntime.toPrimitive(obj, JSRuntime.HINT_STRING));
        return Errors.createTypeError(objName + " is not a RegExp");
    }

    /**
     * This implements the deprecated RegExp.prototype.compile() method.
     */
    public abstract static class JSRegExpCompileNode extends JSBuiltinNode {
        @Child private PropertySetNode setLastIndexNode;

        protected JSRegExpCompileNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            setLastIndexNode = PropertySetNode.create(JSRegExp.LAST_INDEX, false, context, true);
        }

        @Specialization(guards = "isJSRegExp(thisRegExp)")
        protected DynamicObject compile(DynamicObject thisRegExp, Object patternObj, Object flagsObj,
                        @Cached("create(getContext())") CompileRegexNode compileRegexNode,
                        @Cached("createUndefinedToEmpty()") JSToStringNode toStringNode,
                        @Cached("createBinaryProfile()") ConditionProfile isRegExp,
                        @Cached("create()") TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        @Cached("create()") TRegexUtil.TRegexFlagsAccessor flagsAccessor) {
            String pattern;
            String flags;

            if (isRegExp.profile(JSRegExp.isJSRegExp(patternObj))) {
                if (flagsObj != Undefined.instance) {
                    throw Errors.createTypeError("flags must be undefined", this);
                }
                TruffleObject regex = JSRegExp.getCompiledRegex((DynamicObject) patternObj);
                pattern = compiledRegexAccessor.pattern(regex);
                flags = flagsAccessor.source(compiledRegexAccessor.flags(regex));
            } else {
                pattern = toStringNode.executeString(patternObj);
                flags = toStringNode.executeString(flagsObj);
            }

            TruffleObject regex = compileRegexNode.compile(pattern, flags);
            JSRegExp.updateCompilation(getContext(), thisRegExp, regex);
            setLastIndexNode.setValueInt(thisRegExp, 0);
            return thisRegExp;
        }

        @Specialization(guards = "!isJSRegExp(thisObj)")
        protected Object compile(Object thisObj, @SuppressWarnings("unused") Object pattern, @SuppressWarnings("unused") Object flags) {
            throw createNoRegExpError(thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.exec() method as defined by ECMAScript 6, 21.2.5.2.
     */
    public abstract static class JSRegExpExecNode extends JSBuiltinNode {

        @Child private JSRegExpExecBuiltinNode regExpNode;

        protected JSRegExpExecNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() >= 6;
            this.regExpNode = JSRegExpExecBuiltinNode.create(context);
        }

        @Specialization(guards = "isJSRegExp(thisRegExp)") // footprint
        protected DynamicObject execString(DynamicObject thisRegExp, String inputStr) {
            return (DynamicObject) regExpNode.execute(thisRegExp, inputStr);
        }

        @Specialization(guards = "isJSRegExp(thisRegExp)", replaces = "execString")
        protected DynamicObject exec(DynamicObject thisRegExp, Object input,
                        @Cached("create()") JSToStringNode toStringNode) {
            String inputStr = toStringNode.executeString(input);
            return (DynamicObject) regExpNode.execute(thisRegExp, inputStr);
        }

        @Specialization(guards = "!isJSRegExp(thisObj)")
        protected Object exec(Object thisObj, @SuppressWarnings("unused") Object input) {
            throw createNoRegExpError(thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.exec() method as defined by ECMAScript 5.
     */
    public abstract static class JSRegExpExecES5Node extends JSBuiltinNode {

        @Child private JSToStringNode toStringNode;
        @Child private JSRegExpExecBuiltinNode regExpNode;
        @Child private PropertySetNode setIndexNode;
        @Child private PropertySetNode setInputNode;
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private TRegexUtil.TRegexMaterializeResultNode resultMaterializer = TRegexUtil.TRegexMaterializeResultNode.create();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();

        protected JSRegExpExecES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
            this.regExpNode = JSRegExpExecBuiltinNode.create(context);
            this.toStringNode = JSToStringNode.create();
        }

        @Specialization(guards = "isJSRegExp(thisRegExp)")
        protected DynamicObject exec(DynamicObject thisRegExp, Object input) {
            String inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisRegExp, inputStr);
            TruffleObject reResult = (TruffleObject) result;
            if (match.profile(resultAccessor.isMatch(reResult))) {
                return getMatchResult(reResult, inputStr);
            } else {
                return Null.instance;
            }
        }

        @Specialization(guards = "!isJSRegExp(thisObj)")
        protected Object exec(Object thisObj, @SuppressWarnings("unused") Object input) {
            throw createNoRegExpError(thisObj);
        }

        // converts RegexResult into DynamicObject
        protected DynamicObject getMatchResult(TruffleObject result, String inputStr) {
            assert inputStr.equals(resultAccessor.input(result));
            assert getContext().getEcmaScriptVersion() < 6;

            if (setIndexNode == null || setInputNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.setIndexNode = insert(PropertySetNode.create("index", false, getContext(), false));
                this.setInputNode = insert(PropertySetNode.create("input", false, getContext(), false));
            }
            Object[] matches = resultMaterializer.materializeFull(result);
            DynamicObject array = JSArray.createConstant(getContext(), matches);
            setIndexNode.setValueInt(array, resultAccessor.captureGroupStart(result, 0));
            setInputNode.setValue(array, inputStr);
            return array;
        }
    }

    /**
     * This implements the RegExp.prototype.test() method as defined by ECMAScript 5.1 15.10.6.3.
     */
    public abstract static class JSRegExpTestNode extends JSBuiltinNode {

        @Child private Node readIsMatch;

        protected JSRegExpTestNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected Object test(DynamicObject thisObj, Object input,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create(getContext())") JSRegExpExecIntlNode regExpNode) {
            String inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisObj, inputStr);
            if (getContext().getEcmaScriptVersion() >= 6) {
                return (result != Null.instance);
            } else {
                return TRegexUtil.readResultIsMatch(getReadIsMatch(), (TruffleObject) result);
            }
        }

        private Node getReadIsMatch() {
            if (readIsMatch == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIsMatch = insert(TRegexUtil.createReadNode());
            }
            return readIsMatch;
        }

        @Specialization(guards = "!isJSObject(thisNonObj)")
        protected Object test(Object thisNonObj, @SuppressWarnings("unused") Object input) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.test", thisNonObj);
        }
    }

    /**
     * This implements the RegExp.prototype.toString() method.
     */
    public abstract static class JSRegExpToStringNode extends JSBuiltinNode {
        @Child private PropertyGetNode getSourceNode;
        @Child private PropertyGetNode getFlagsNode;

        protected JSRegExpToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getFlagsNode = PropertyGetNode.create("flags", false, context);
            this.getSourceNode = PropertyGetNode.create("source", false, context);
        }

        @Specialization
        protected String toString(DynamicObject thisObj,
                        @Cached("create()") JSToStringNode toString1Node,
                        @Cached("create()") JSToStringNode toString2Node) {
            String source = toString1Node.executeString(getSourceNode.getValue(thisObj));
            String flags = toString2Node.executeString(getFlagsNode.getValue(thisObj));
            return toStringIntl(source, flags);
        }

        @TruffleBoundary
        private static String toStringIntl(String source, String flags) {
            return "/" + source + "/" + flags;
        }

        @Specialization(guards = "!isJSObject(thisNonObj)")
        protected Object toString(Object thisNonObj) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.toString", thisNonObj);
        }

    }

    public abstract static class RegExpPrototypeSymbolOperation extends JSBuiltinNode {

        @Child private JSRegExpExecIntlNode regexExecIntlNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private WriteElementNode writeNode;
        @Child private ReadElementNode readNode;
        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;
        private final ConditionProfile advanceIndexLengthProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile advanceIndexFirstProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile advanceIndexSecondProfile = ConditionProfile.createBinaryProfile();

        public RegExpPrototypeSymbolOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected Object read(Object target, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ReadElementNode.create(getContext()));
            }
            return readNode.executeWithTargetAndIndex(target, index);
        }

        protected void write(Object target, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(WriteElementNode.create(getContext(), true, true));
            }
            writeNode.executeWithTargetAndIndexAndValue(target, index, value);
        }

        protected final ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(getContext(), false));
            }
            return arraySpeciesCreateNode;
        }

        protected int advanceStringIndexUnicode(String s, int index) {
            if (advanceIndexLengthProfile.profile(index + 1 >= s.length())) {
                return index + 1;
            }
            char first = s.charAt(index);
            if (advanceIndexFirstProfile.profile(first < 0xD800 || first > 0xDBFF)) {
                return index + 1;
            }
            char second = s.charAt(index + 1);
            if (advanceIndexSecondProfile.profile(second < 0xDC00 || second > 0xDFFF)) {
                return index + 1;
            }
            return index + 2;
        }

        protected PropertySetNode getSetLastIndexNode() {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(this.setLastIndexNode = PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            return setLastIndexNode;
        }

        protected PropertyGetNode getGetLastIndexNode() {
            if (getLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLastIndexNode = insert(PropertyGetNode.create(JSRegExp.LAST_INDEX, false, getContext()));
            }
            return getLastIndexNode;
        }

        protected JSRegExpExecIntlNode getRegexExecIntlNode() {
            if (regexExecIntlNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                regexExecIntlNode = insert(JSRegExpExecIntlNode.create(getContext()));
            }
            return regexExecIntlNode;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@split] method.
     */
    public abstract static class JSRegExpSplitNode extends RegExpPrototypeSymbolOperation {
        @Child private PropertyGetNode getFlagsNode;
        @Child private PropertyGetNode getLengthNode;
        @Child private JSToUInt32Node toUInt32Node;
        @Child private JSToLengthNode toLengthNode;

        protected JSRegExpSplitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getFlagsNode = PropertyGetNode.create("flags", false, context);
        }

        @Specialization(guards = "isJSObject(rx)")
        protected DynamicObject split(DynamicObject rx, Object separator, Object limit,
                        @Cached("create()") JSToStringNode toString1Node,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("createBinaryProfile()") ConditionProfile sizeZeroProfile,
                        @Cached("createBinaryProfile()") ConditionProfile eIsP,
                        @Cached("createBinaryProfile()") ConditionProfile zIsNull,
                        @Cached("createBinaryProfile()") ConditionProfile isUnicode,
                        @Cached("createBinaryProfile()") ConditionProfile limitProfile,
                        @Cached("create()") BranchProfile prematureReturnBranch) {

            String s = toString1Node.executeString(separator);

            DynamicObject regexpConstructor = getContext().getRealm().getRegExpConstructor().getFunctionObject();
            DynamicObject c = getArraySpeciesConstructorNode().speciesConstructor(rx, regexpConstructor);
            String flags = toString2Node.executeString(getFlagsNode.getValue(rx));
            boolean unicodeMatching = flags.indexOf('u') >= 0;
            DynamicObject splitter = (DynamicObject) getArraySpeciesConstructorNode().construct(c, rx, ensureSticky(flags));
            DynamicObject a = JSArray.createEmptyZeroLength(getContext());
            long lim;
            if (limitProfile.profile(limit == Undefined.instance)) {
                lim = (long) JSRuntime.MAX_SAFE_INTEGER;
            } else {
                // NOT ToLength()v https://github.com/tc39/ecma262/issues/92
                lim = getToUInt32Node().executeLong(limit);
                if (lim == 0) {
                    prematureReturnBranch.enter();
                    return a;
                }
            }
            int size = s.length();
            if (sizeZeroProfile.profile(size == 0)) {
                if (getRegexExecIntlNode().execute(splitter, s) == Null.instance) {
                    write(a, 0, s);
                }
                return a;
            }

            int lengthA = 0;
            int p = 0;
            int q = 0;
            while (q < size) {
                getSetLastIndexNode().setValueInt(splitter, q);

                DynamicObject z = (DynamicObject) getRegexExecIntlNode().execute(splitter, s);
                if (zIsNull.profile(z == Null.instance)) {
                    q = movePosition(isUnicode, s, unicodeMatching, q);
                } else {
                    int e = (int) toLength(getGetLastIndexNode().getValue(splitter));
                    if (eIsP.profile(e == p)) {
                        q = movePosition(isUnicode, s, unicodeMatching, q);
                    } else {
                        write(a, lengthA, Boundaries.substring(s, p, q));
                        lengthA++;
                        if (lengthA == lim) {
                            prematureReturnBranch.enter();
                            return a;
                        }
                        p = e;
                        long numberOfCaptures = Math.max(toLength(getLength(z)) - 1, 0);
                        int i = 1;
                        while (i <= numberOfCaptures) {
                            write(a, lengthA, read(z, i));
                            i++;
                            lengthA++;
                            if (lengthA == lim) {
                                prematureReturnBranch.enter();
                                return a;
                            }
                        }
                        q = p;
                    }
                }
            }
            write(a, lengthA, Boundaries.substring(s, Math.min(p, s.length()), size));
            return a;
        }

        /**
         * Ensure sticky ("y") is part of the flags.
         */
        private static Object ensureSticky(String flags) {
            return (flags.length() == 0) ? "y" : (flags.indexOf('y') >= 0) ? flags : addStickyFlag(flags);
        }

        private long toLength(Object obj) {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return toLengthNode.executeLong(obj);
        }

        private Object getLength(DynamicObject obj) {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(PropertyGetNode.create("length", false, getContext()));
            }
            return getLengthNode.getValue(obj);
        }

        private int movePosition(ConditionProfile isUnicode, String s, boolean unicodeMatching, int q) {
            return isUnicode.profile(unicodeMatching) ? advanceStringIndexUnicode(s, q) : q + 1;
        }

        @TruffleBoundary
        private static String addStickyFlag(String flags) {
            return flags + "y";
        }

        private JSToUInt32Node getToUInt32Node() {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return toUInt32Node;
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object split(Object thisObj, @SuppressWarnings("unused") Object pattern, @SuppressWarnings("unused") Object flags) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@split", thisObj);
        }

    }

    /**
     * This implements the RegExp.prototype.[@@replace] method.
     */
    public abstract static class JSRegExpReplaceNode extends RegExpPrototypeSymbolOperation {
        @Child private PropertyGetNode getGlobalNode;
        @Child private PropertyGetNode getUnicodeNode;
        @Child private PropertyGetNode getLengthNode;
        @Child private PropertyGetNode getIndexNode;
        @Child private PropertyGetNode getGroupsNode;
        @Child private TRegexUtil.TRegexResultAccessor readLazyLengthNode;
        @Child private JSToLengthNode toLengthNode;
        @Child private JSToIntegerNode toIntegerNode;
        @Child private JSToStringNode toString2Node;
        @Child private JSToStringNode toString3Node;
        @Child private JSToStringNode toString4Node;
        @Child private JSFunctionCallNode functionCallNode;
        @Child private JSToBooleanNode toBoolean1Node;
        @Child private JSToBooleanNode toBoolean2Node;
        @Child private IsCallableNode isCallableNode;
        @Child private ReadElementNode readNamedCaptureGroupNode;
        @Child private JSToObjectNode toObjectNode;

        private final ConditionProfile unicodeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile globalProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile functionalReplaceProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lazyResultArrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile replaceEmptyProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile replaceRawProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile noMatchProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile validPositionProfile = ConditionProfile.createBinaryProfile();

        protected JSRegExpReplaceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getGlobalNode = PropertyGetNode.create("global", false, context);
            this.getIndexNode = PropertyGetNode.create("index", false, context);
            this.toIntegerNode = JSToIntegerNode.create();
            this.toBoolean1Node = JSToBooleanNode.create();
            this.isCallableNode = IsCallableNode.create();
        }

        @Specialization(guards = "isJSObject(rx)")
        protected String replace(DynamicObject rx, Object searchString, Object replaceValue,
                        @Cached("create()") JSToStringNode toString1Node) {
            String s = toString1Node.executeString(searchString);
            boolean functionalReplace = isCallableNode.executeBoolean(replaceValue);

            String replaceString = null;
            DynamicObject replaceFunction = null;
            boolean replaceEmpty = false;
            boolean replaceRaw = false;

            if (functionalReplaceProfile.profile(functionalReplace)) {
                replaceFunction = (DynamicObject) replaceValue;
            } else {
                replaceString = getToString2Node().executeString(replaceValue);
                replaceEmpty = replaceString.isEmpty();
                replaceRaw = replaceString.length() > 0 && replaceString.indexOf('$') < 0;
            }

            boolean global = toBoolean1Node.executeBoolean(getGlobalNode.getValue(rx));
            boolean fullUnicode = false;
            if (globalProfile.profile(global)) {
                fullUnicode = getToBoolean2Node().executeBoolean(getGetUnicodeNode().getValue(rx));
                getSetLastIndexNode().setValue(rx, 0);
            }
            List<DynamicObject> results = null;
            if (functionalReplaceProfile.profile(functionalReplace)) {
                results = new ArrayList<>();
            }
            DelimitedStringBuilder accumulatedResult = new DelimitedStringBuilder(replaceEmptyProfile.profile(replaceEmpty) ? s.length() : s.length() + 16);
            int nextSourcePosition = 0;
            while (true) {
                DynamicObject result = (DynamicObject) getRegexExecIntlNode().execute(rx, s);
                if (noMatchProfile.profile(result == Null.instance)) {
                    break;
                }
                int matchLength;
                if (lazyResultArrayProfile.profile(isLazyResultArray(result))) {
                    matchLength = getLazyLength(result);
                } else {
                    matchLength = processNonLazy(result);
                }
                if (functionalReplaceProfile.profile(functionalReplace)) {
                    results.add(result);
                } else {
                    nextSourcePosition = processResult(accumulatedResult, result, s, replaceString, nextSourcePosition, matchLength, replaceEmpty, replaceRaw);
                }
                if (globalProfile.profile(global)) {
                    if (matchLength == 0) {
                        int lastI = (int) toLength(getGetLastIndexNode().getValue(rx));
                        getSetLastIndexNode().setValue(rx, unicodeProfile.profile(fullUnicode) ? advanceStringIndexUnicode(s, lastI) : lastI + 1);
                    }
                } else {
                    break;
                }
            }
            if (functionalReplaceProfile.profile(functionalReplace)) {
                for (int i = 0; i < results.size(); i++) {
                    nextSourcePosition = processResultFunctional(accumulatedResult, results.get(i), s, replaceFunction, nextSourcePosition);
                }
            }
            if (nextSourcePosition < s.length()) {
                accumulatedResult.append(s, nextSourcePosition, s.length());
            }
            return accumulatedResult.toString();
        }

        private int processNonLazy(DynamicObject result) {
            int resultLength = (int) toLength(getLength(result));
            String result0Str = getToString3Node().executeString(read(result, 0));
            write(result, 0, result0Str);
            for (int n = 1; n < resultLength; n++) {
                Object value = read(result, n);
                if (value != Undefined.instance) {
                    write(result, n, getToString3Node().executeString(value));
                }
            }
            return result0Str.length();
        }

        private boolean isLazyResultArray(DynamicObject result) {
            return result.getShape() == getContext().getRealm().getLazyRegexArrayFactory().getShape();
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object replace(Object thisObj, @SuppressWarnings("unused") Object pattern, @SuppressWarnings("unused") Object flags) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@replae", thisObj);
        }

        protected int processResult(DelimitedStringBuilder accumulatedResult, DynamicObject result, String s, String replaceString, int nextSourcePosition, int matchLength, boolean replaceEmpty,
                        boolean replaceRaw) {
            int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), s.length()), 0);
            if (validPositionProfile.profile(position >= nextSourcePosition)) {
                accumulatedResult.append(s, nextSourcePosition, position);
                if (!replaceEmptyProfile.profile(replaceEmpty)) {
                    if (replaceRawProfile.profile(replaceRaw)) {
                        accumulatedResult.append(replaceString);
                    } else {
                        Object namedCaptures = getGroups(result);
                        if (namedCaptures != Undefined.instance) {
                            namedCaptures = getToObjectNode().executeTruffleObject(namedCaptures);
                        }
                        appendSubstitution(accumulatedResult, result, (DynamicObject) namedCaptures, matchLength, s, position, replaceString);
                    }
                }
                return position + matchLength;
            }
            return nextSourcePosition;
        }

        protected int processResultFunctional(DelimitedStringBuilder accumulatedResult, DynamicObject result, String s, DynamicObject replaceFunction, int nextSourcePosition) {
            int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), s.length()), 0);

            int resultsLength = (int) toLength(getLength(result));
            Object namedCaptures = getGroups(result);
            Object[] arguments = new Object[resultsLength + 4 + (namedCaptures != Undefined.instance ? 1 : 0)];
            arguments[0] = Undefined.instance;
            arguments[1] = replaceFunction;
            for (int i = 0; i < resultsLength; i++) {
                arguments[i + 2] = read(result, i);
            }
            arguments[resultsLength + 2] = position;
            arguments[resultsLength + 3] = s;
            if (namedCaptures != Undefined.instance) {
                arguments[resultsLength + 4] = namedCaptures;
            }
            Object callResult = getFunctionCallNode().executeCall(arguments);

            String replacement = getToString2Node().executeString(callResult);
            if (validPositionProfile.profile(position >= nextSourcePosition)) {
                accumulatedResult.append(s, nextSourcePosition, position);
                accumulatedResult.append(replacement);
                if (lazyResultArrayProfile.profile(isLazyResultArray(result))) {
                    return position + getLazyLength(result);
                } else {
                    return position + ((String) read(result, 0)).length();
                }
            }
            return nextSourcePosition;
        }

        private void appendSubstitution(DelimitedStringBuilder accumulatedResult, DynamicObject result, DynamicObject namedCaptures, int matchLength, String str, int position, String replacement) {
            int dollarPos = replacement.indexOf('$');
            int tailPos = position + matchLength;
            accumulatedResult.append(replacement, 0, dollarPos);
            int pos = dollarPos;
            while (pos != -1) {
                pos = appendSubstitutionIntl(accumulatedResult, str, pos + 1, position, tailPos, replacement, result, namedCaptures);
                pos = nextDollar(accumulatedResult, pos, replacement);
            }
        }

        private static int nextDollar(DelimitedStringBuilder sb, int start, String replacement) {
            int pos = replacement.indexOf('$', start);
            int end = (pos <= -1) ? replacement.length() : pos;
            sb.append(replacement, start, end);
            return pos;
        }

        private int appendSubstitutionIntl(DelimitedStringBuilder sb, String input, int pos, int startPos, int endPos, String replaceStr, DynamicObject result, DynamicObject namedCaptures) {
            if (pos == replaceStr.length()) {
                sb.append('$');
                return pos;
            }
            char ch = replaceStr.charAt(pos);
            int retPos = pos + 1;
            switch (ch) {
                case '$':
                    sb.append('$');
                    break;
                case '&':
                    sb.append((String) read(result, 0));
                    break;
                case '`':
                    sb.append(input, 0, startPos);
                    break;
                case '\'':
                    if (endPos < input.length()) {
                        sb.append(input, endPos, input.length());
                    }
                    break;
                case '<':
                    if (namedCaptures == Undefined.instance) {
                        sb.append("$<");
                    } else {
                        int groupNameStart = retPos;
                        while (retPos < replaceStr.length() && replaceStr.charAt(retPos) != '>') {
                            retPos++;
                        }
                        if (retPos == replaceStr.length()) {
                            sb.append("$<");
                            retPos = groupNameStart;
                        } else {
                            assert replaceStr.charAt(retPos) == '>';
                            String groupName = replaceStr.substring(groupNameStart, retPos);
                            retPos++;
                            Object capture = readNamedCaptureGroup(namedCaptures, groupName);
                            if (capture != Undefined.instance) {
                                sb.append(getToString4Node().executeString(capture));
                            }
                        }
                    }
                    break;
                default:
                    if (isDigit(ch)) {
                        int firstDigit = (ch - '0');
                        boolean nextIsDigit = replaceStr.length() > (pos + 1) && isDigit(replaceStr.charAt(pos + 1));
                        int n = 0;
                        if (nextIsDigit) {
                            char nextDigit = (char) (replaceStr.charAt(pos + 1) - '0');
                            n = firstDigit * 10 + nextDigit;
                            retPos += 1;
                        } else {
                            n = firstDigit;
                        }
                        Object nCapture;
                        int resultLength = (int) toLength(getLength(result));
                        if (0 < n && n < resultLength) {
                            nCapture = read(result, n);
                            sb.append(nCapture == Undefined.instance ? "" : (String) nCapture);
                        } else if (nextIsDigit && firstDigit < resultLength && firstDigit > 0) {
                            // $nn does not fit, but $n does and is >0
                            nCapture = read(result, firstDigit);
                            if (nCapture != Undefined.instance) {
                                sb.append((String) nCapture);
                            }
                            retPos--;
                        } else {
                            sb.append('$');
                            sb.append(ch);
                            if (nextIsDigit) {
                                sb.append(replaceStr.charAt(pos + 1));
                            }
                        }
                    } else {
                        sb.append('$');
                        retPos--;
                    }
                    break;
            }
            return retPos;
        }

        private static boolean isDigit(char ch) {
            return '0' <= ch && ch <= '9';
        }

        private JSFunctionCallNode getFunctionCallNode() {
            if (functionCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionCallNode = insert(JSFunctionCallNode.create(false));
            }
            return functionCallNode;
        }

        private JSToStringNode getToString2Node() {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node;
        }

        private JSToStringNode getToString3Node() {
            if (toString3Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString3Node = insert(JSToStringNode.create());
            }
            return toString3Node;
        }

        private JSToStringNode getToString4Node() {
            if (toString4Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString4Node = insert(JSToStringNode.create());
            }
            return toString4Node;
        }

        protected int getLazyLength(DynamicObject obj) {
            if (readLazyLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readLazyLengthNode = insert(TRegexUtil.TRegexResultAccessor.create());
            }
            return readLazyLengthNode.captureGroupLength(JSAbstractArray.arrayGetRegexResult(obj), 0);
        }

        private PropertyGetNode getGetUnicodeNode() {
            if (getUnicodeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getUnicodeNode = insert(PropertyGetNode.create("unicode", false, getContext()));
            }
            return getUnicodeNode;
        }

        private Object getLength(Object obj) {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(PropertyGetNode.create(JSArray.LENGTH, false, getContext()));
            }
            return getLengthNode.getValue(obj);
        }

        private long toLength(Object value) {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return toLengthNode.executeLong(value);
        }

        private JSToBooleanNode getToBoolean2Node() {
            if (toBoolean2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBoolean2Node = insert(JSToBooleanNode.create());
            }
            return toBoolean2Node;
        }

        private Object getGroups(Object regexResult) {
            if (getGroupsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getGroupsNode = insert(PropertyGetNode.create(JSRegExp.GROUPS, false, getContext()));
            }
            return getGroupsNode.getValue(regexResult);
        }

        private Object readNamedCaptureGroup(Object namedCaptureGroups, String groupName) {
            if (readNamedCaptureGroupNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNamedCaptureGroupNode = insert(ReadElementNode.create(getContext()));
            }
            return readNamedCaptureGroupNode.executeWithTargetAndIndex(namedCaptureGroups, groupName);
        }

        private JSToObjectNode getToObjectNode() {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@match] method.
     */
    public abstract static class JSRegExpMatchNode extends RegExpPrototypeSymbolOperation {
        @Child private PropertyGetNode getUnicodeNode;
        @Child private PropertyGetNode getGlobalNode;
        @Child private JSToLengthNode toLengthNode;

        private final ConditionProfile isGlobalProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile unicodeProfile = ConditionProfile.createBinaryProfile();

        protected JSRegExpMatchNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getUnicodeNode = PropertyGetNode.create("unicode", false, context);
            this.getGlobalNode = PropertyGetNode.create("global", false, context);
            this.toLengthNode = JSToLengthNode.create();
        }

        @Specialization(guards = "isJSObject(rx)")
        protected Object match(DynamicObject rx, Object param,
                        @Cached("create()") JSToStringNode toString1Node,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("create()") JSToBooleanNode toBoolean1Node,
                        @Cached("create()") JSToBooleanNode toBoolean2Node) {
            String s = toString1Node.executeString(param);
            if (isGlobalProfile.profile(!toBoolean1Node.executeBoolean(getGlobalNode.getValue(rx)))) {
                return getRegexExecIntlNode().execute(rx, s);
            } else {
                boolean fullUnicode = toBoolean2Node.executeBoolean(getUnicodeNode.getValue(rx));
                getSetLastIndexNode().setValue(rx, 0);
                DynamicObject a = JSArray.createEmptyZeroLength(getContext());
                int n = 0;
                DynamicObject result;
                String matchStr;
                while (true) {
                    result = (DynamicObject) getRegexExecIntlNode().execute(rx, s);
                    if (result == Null.instance) {
                        return n == 0 ? Null.instance : a;
                    }
                    matchStr = toString2Node.executeString(read(result, 0));
                    write(a, n, matchStr);
                    if (matchStr.length() == 0) {
                        int lastI = (int) getToLengthNode().executeLong(getGetLastIndexNode().getValue(rx));
                        getSetLastIndexNode().setValue(rx, unicodeProfile.profile(fullUnicode) ? advanceStringIndexUnicode(s, lastI) : lastI + 1);
                    }
                    n++;
                }
            }
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object match(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@match", thisObj);
        }

        private JSToLengthNode getToLengthNode() {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return toLengthNode;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@search] method.
     */
    public abstract static class JSRegExpSearchNode extends RegExpPrototypeSymbolOperation {
        @Child private PropertyGetNode getIndexNode;
        @Child private JSIdenticalNode sameValueNode;

        protected JSRegExpSearchNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIndexNode = PropertyGetNode.create("index", false, context);
            this.sameValueNode = JSIdenticalNode.createSameValue();
        }

        @Specialization(guards = "isJSObject(rx)")
        protected Object search(DynamicObject rx, Object param,
                        @Cached("create()") JSToStringNode toString1Node) {
            String s = toString1Node.executeString(param);
            Object previousLastIndex = getGetLastIndexNode().getValue(rx);
            if (!sameValueNode.executeBoolean(previousLastIndex, 0)) {
                getSetLastIndexNode().setValue(rx, 0);
            }
            Object result = getRegexExecIntlNode().execute(rx, s);
            Object currentLastIndex = getGetLastIndexNode().getValue(rx);
            if (!sameValueNode.executeBoolean(currentLastIndex, previousLastIndex)) {
                getSetLastIndexNode().setValue(rx, previousLastIndex);
            }
            return result == Null.instance ? -1 : getIndexNode.getValue(result);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object search(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@search", thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.[@@matchAll] method.
     */
    public abstract static class JSRegExpMatchAllNode extends JSBuiltinNode {
        @Child private MatchAllIteratorNode matchAllIteratorNode;

        public JSRegExpMatchAllNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(regex)")
        protected Object matchAll(VirtualFrame frame, DynamicObject regex, Object stringObj) {
            return getMatchAllIteratorNode().createMatchAllIterator(frame, regex, stringObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object matchAll(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@matchAll", thisObj);
        }

        private MatchAllIteratorNode getMatchAllIteratorNode() {
            if (matchAllIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                matchAllIteratorNode = insert(new MatchAllIteratorNode(getContext()));
            }
            return matchAllIteratorNode;
        }
    }
}
