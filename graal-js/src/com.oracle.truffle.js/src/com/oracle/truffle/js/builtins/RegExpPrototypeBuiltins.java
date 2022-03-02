/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
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
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.RegExpFlagsGetterNodeGen;
import com.oracle.truffle.js.builtins.helper.IsPristineObjectNode;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode.JSRegExpExecBuiltinNode;
import com.oracle.truffle.js.builtins.helper.ReplaceStringParser;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

/**
 * Contains builtin methods for {@linkplain JSRegExp}.prototype.
 */
public final class RegExpPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpPrototypeBuiltins.RegExpPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new RegExpPrototypeBuiltins();

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
        private final Symbol key;

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
                return JSConfig.ECMAScript2019;
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
        TruffleString objName = Strings.fromObject(JSRuntime.toPrimitive(obj, Strings.HINT_STRING));
        return Errors.createTypeError(Strings.toJavaString(objName) + " is not a RegExp");
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

        @Specialization
        protected JSRegExpObject compile(JSRegExpObject thisRegExp, Object patternObj, Object flagsObj,
                        @Cached("create(getContext())") CompileRegexNode compileRegexNode,
                        @Cached("createUndefinedToEmpty()") JSToStringNode toStringNode,
                        @Cached("createBinaryProfile()") ConditionProfile isRegExpProfile,
                        @Cached TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        @Cached TRegexUtil.TRegexFlagsAccessor flagsAccessor) {
            Object pattern;
            Object flags;

            if (getRealm() != JSRegExp.getRealm(thisRegExp)) {
                throw Errors.createTypeError("RegExp.prototype.compile cannot be used on a RegExp from a different Realm.");
            }
            if (!JSRegExp.getLegacyFeaturesEnabled(thisRegExp)) {
                throw Errors.createTypeError("RegExp.prototype.compile cannot be used on subclasses of RegExp.");
            }
            boolean isRegExp = isRegExpProfile.profile(JSRegExp.isJSRegExp(patternObj));
            if (isRegExp) {
                if (flagsObj != Undefined.instance) {
                    throw Errors.createTypeError("flags must be undefined", this);
                }
                Object regex = JSRegExp.getCompiledRegex((DynamicObject) patternObj);
                pattern = compiledRegexAccessor.pattern(regex);
                flags = flagsAccessor.source(compiledRegexAccessor.flags(regex));
            } else {
                pattern = toStringNode.executeString(patternObj);
                flags = toStringNode.executeString(flagsObj);
            }

            Object regex = compileRegexNode.compile(pattern, flags);
            JSRegExp.updateCompilation(getContext(), thisRegExp, regex);
            setLastIndexNode.setValueInt(thisRegExp, 0);
            return thisRegExp;
        }

        @Fallback
        protected Object compile(Object thisObj, @SuppressWarnings("unused") Object pattern, @SuppressWarnings("unused") Object flags) {
            throw createNoRegExpError(thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.exec() method as defined by ECMAScript 6, 21.2.5.2.
     */
    public abstract static class JSRegExpExecNode extends JSBuiltinNode {

        @Child private JSRegExpExecBuiltinNode regExpNode;

        JSRegExpExecNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() >= 6;
            this.regExpNode = JSRegExpExecBuiltinNode.create(context);
        }

        @Specialization
        DynamicObject doString(JSRegExpObject thisRegExp, TruffleString inputStr) {
            return (DynamicObject) regExpNode.execute(thisRegExp, inputStr);
        }

        @Specialization(replaces = {"doString"})
        DynamicObject doObject(JSRegExpObject thisRegExp, Object input,
                        @Cached JSToStringNode toStringNode) {
            return (DynamicObject) regExpNode.execute(thisRegExp, toStringNode.executeString(input));
        }

        @Fallback
        Object doNoRegExp(Object thisObj, @SuppressWarnings("unused") Object input) {
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
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private TRegexUtil.TRegexMaterializeResultNode resultMaterializer = TRegexUtil.TRegexMaterializeResultNode.create();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();

        protected JSRegExpExecES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
            this.regExpNode = JSRegExpExecBuiltinNode.create(context);
            this.toStringNode = JSToStringNode.create();
        }

        @Specialization
        protected DynamicObject exec(JSRegExpObject thisRegExp, Object input) {
            TruffleString inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisRegExp, inputStr);
            if (match.profile(resultAccessor.isMatch(result))) {
                return getMatchResult(result, compiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(thisRegExp)), inputStr);
            } else {
                return Null.instance;
            }
        }

        @Fallback
        protected Object exec(Object thisObj, @SuppressWarnings("unused") Object input) {
            throw createNoRegExpError(thisObj);
        }

        // converts RegexResult into DynamicObject
        protected DynamicObject getMatchResult(Object result, int groupCount, TruffleString inputStr) {
            assert getContext().getEcmaScriptVersion() < 6;

            if (setIndexNode == null || setInputNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.setIndexNode = insert(PropertySetNode.create(JSRegExp.INDEX, false, getContext(), false));
                this.setInputNode = insert(PropertySetNode.create(JSRegExp.INPUT, false, getContext(), false));
            }
            Object[] matches = resultMaterializer.materializeFull(getContext(), result, groupCount, inputStr);
            DynamicObject array = JSArray.createConstant(getContext(), getRealm(), matches);
            setIndexNode.setValueInt(array, resultAccessor.captureGroupStart(result, 0));
            setInputNode.setValue(array, inputStr);
            return array;
        }
    }

    /**
     * This implements the RegExp.prototype.test() method as defined by ECMAScript 5.1 15.10.6.3.
     */
    public abstract static class JSRegExpTestNode extends JSBuiltinNode {

        @Child private TRegexUtil.InteropReadBooleanMemberNode readIsMatch;

        protected JSRegExpTestNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(thisObj)", limit = "1")
        protected Object testGeneric(DynamicObject thisObj, Object input,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached("create(getContext())") JSRegExpExecIntlNode regExpNode) {
            Object inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisObj, inputStr);
            if (getContext().getEcmaScriptVersion() >= 6) {
                return (result != Null.instance);
            } else {
                return getReadIsMatch().execute(result, TRegexUtil.Props.RegexResult.IS_MATCH);
            }
        }

        @Fallback
        protected Object testError(Object thisNonObj, @SuppressWarnings("unused") Object input) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.test", thisNonObj);
        }

        private TRegexUtil.InteropReadBooleanMemberNode getReadIsMatch() {
            if (readIsMatch == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readIsMatch = insert(TRegexUtil.InteropReadBooleanMemberNode.create());
            }
            return readIsMatch;
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
            this.getFlagsNode = PropertyGetNode.create(Strings.FLAGS, false, context);
            this.getSourceNode = PropertyGetNode.create(Strings.SOURCE, false, context);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(thisObj)", limit = "1")
        protected Object toString(DynamicObject thisObj,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toString1Node,
                        @Cached JSToStringNode toString2Node) {
            TruffleString source = toString1Node.executeString(getSourceNode.getValue(thisObj));
            TruffleString flags = toString2Node.executeString(getFlagsNode.getValue(thisObj));
            return toStringIntl(source, flags);
        }

        @Fallback
        protected Object toString(Object thisNonObj) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.toString", thisNonObj);
        }

        @TruffleBoundary
        private static Object toStringIntl(TruffleString source, TruffleString flags) {
            return Strings.concatAll(Strings.SLASH, source, Strings.SLASH, flags);
        }

    }

    public abstract static class RegExpPrototypeSymbolOperation extends JSBuiltinNode {

        @Child private JSRegExpExecIntlNode regexExecIntlNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private WriteElementNode writeNode;
        @Child private ReadElementNode readNode;
        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;
        @Child private JSToBooleanNode toBooleanNode;
        @Child private TruffleString.ReadCharUTF16Node stringReadNode;
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

        private char charAt(TruffleString s, int i) {
            if (stringReadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringReadNode = insert(TruffleString.ReadCharUTF16Node.create());
            }
            return Strings.charAt(stringReadNode, s, i);
        }

        protected final ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(getContext(), false));
            }
            return arraySpeciesCreateNode;
        }

        protected int advanceStringIndexUnicode(TruffleString s, int index) {
            if (advanceIndexLengthProfile.profile(index + 1 >= Strings.length(s))) {
                return index + 1;
            }
            char first = charAt(s, index);
            if (advanceIndexFirstProfile.profile(first < 0xD800 || first > 0xDBFF)) {
                return index + 1;
            }
            char second = charAt(s, index + 1);
            if (advanceIndexSecondProfile.profile(second < 0xDC00 || second > 0xDFFF)) {
                return index + 1;
            }
            return index + 2;
        }

        private void initLastIndexNode() {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
        }

        protected void setLastIndex(Object obj, int value) {
            initLastIndexNode();
            setLastIndexNode.setValueInt(obj, value);
        }

        protected void setLastIndex(Object obj, Object value) {
            initLastIndexNode();
            setLastIndexNode.setValue(obj, value);
        }

        protected Object getLastIndex(Object obj) {
            if (getLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLastIndexNode = insert(PropertyGetNode.create(JSRegExp.LAST_INDEX, false, getContext()));
            }
            return getLastIndexNode.getValue(obj);
        }

        protected Object regexExecIntl(DynamicObject regex, Object input) {
            if (regexExecIntlNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                regexExecIntlNode = insert(JSRegExpExecIntlNode.create(getContext()));
            }
            return regexExecIntlNode.execute(regex, input);
        }

        protected final boolean getFlag(DynamicObject re, PropertyGetNode getNode) {
            boolean flag;
            if (toBooleanNode == null) {
                try {
                    flag = getNode.getValueBoolean(re);
                } catch (UnexpectedResultException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.toBooleanNode = insert(JSToBooleanNode.create());
                    flag = toBooleanNode.executeBoolean(e.getResult());
                }
            } else {
                flag = toBooleanNode.executeBoolean(getNode.getValue(re));
            }
            return flag;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@split] method.
     */
    @ImportStatic(JSGuards.class)
    public abstract static class JSRegExpSplitNode extends RegExpPrototypeSymbolOperation {
        @Child private IsJSObjectNode isObjectNode;
        @Child private JSToStringNode toString1Node;
        @Child private JSToStringNode toString2Node;
        @Child private PropertyGetNode getFlagsNode;
        @Child private PropertyGetNode getLengthNode;
        @Child private JSToUInt32Node toUInt32Node;
        @Child private JSToLengthNode toLengthNode;
        @Child private JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndexNode;
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor;
        @Child private TRegexUtil.TRegexFlagsAccessor flagsAccessor;
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor;
        @Child private IsPristineObjectNode isPristineObjectNode;
        @Child private TruffleString.ConcatNode stringConcatNode;
        @Child private TruffleString.SubstringByteIndexNode substringNode;
        @Child private TruffleString.ByteIndexOfCodePointNode stringIndexOfNode;
        private final ConditionProfile sizeZeroProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile sameMatchEnd = ConditionProfile.createBinaryProfile();
        private final ConditionProfile resultIsNull = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isUnicode = ConditionProfile.createBinaryProfile();
        private final ConditionProfile limitProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile prematureReturnBranch = BranchProfile.create();
        private final ConditionProfile emptyFlags = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stickyFlagSet = ConditionProfile.createBinaryProfile();
        private final ValueProfile compiledRegexProfile = ValueProfile.createIdentityProfile();

        JSRegExpSplitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        DynamicObject splitIntLimit(DynamicObject rx, Object input, int limit) {
            return doSplit(rx, input, toUInt32(limit));
        }

        @Specialization
        DynamicObject splitLongLimit(DynamicObject rx, Object input, long limit) {
            return doSplit(rx, input, toUInt32(limit));
        }

        @Specialization(guards = "isUndefined(limit)")
        DynamicObject splitUndefinedLimit(DynamicObject rx, Object input, @SuppressWarnings("unused") Object limit) {
            return doSplit(rx, input, JSRuntime.MAX_SAFE_INTEGER_LONG);
        }

        @Specialization(guards = "!isUndefined(limit)")
        DynamicObject splitObjectLimit(DynamicObject rx, Object input, Object limit) {
            checkObject(rx);
            TruffleString str = toString1(input);
            DynamicObject constructor = getSpeciesConstructor(rx);
            return splitAccordingToSpec(rx, str, limit, constructor);
        }

        @Fallback
        Object doNoObject(Object rx, @SuppressWarnings("unused") Object input, @SuppressWarnings("unused") Object flags) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@split", rx);
        }

        private DynamicObject doSplit(DynamicObject rx, Object input, long limit) {
            checkObject(rx);
            TruffleString str = toString1(input);
            if (limit == 0) {
                prematureReturnBranch.enter();
                return JSArray.createEmptyZeroLength(getContext(), getRealm());
            }
            DynamicObject constructor = getSpeciesConstructor(rx);
            if (constructor == getRealm().getRegExpConstructor() && isPristine(rx)) {
                return splitInternal(rx, str, limit);
            }
            return splitAccordingToSpec(rx, str, limit, constructor);
        }

        private void checkObject(DynamicObject rx) {
            if (!isJSObject(rx)) {
                throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@split", rx);
            }
        }

        private DynamicObject getSpeciesConstructor(DynamicObject rx) {
            DynamicObject regexpConstructor = getRealm().getRegExpConstructor();
            return getArraySpeciesConstructorNode().speciesConstructor(rx, regexpConstructor);
        }

        private DynamicObject splitAccordingToSpec(DynamicObject rx, TruffleString str, Object limit, DynamicObject constructor) {
            TruffleString flags = toString2(getFlags(rx));
            boolean unicodeMatching = indexOf(flags, 'u') >= 0;
            DynamicObject splitter = (DynamicObject) getArraySpeciesConstructorNode().construct(constructor, rx, ensureSticky(flags));
            DynamicObject array = JSArray.createEmptyZeroLength(getContext(), getRealm());
            long lim;
            if (limitProfile.profile(limit == Undefined.instance)) {
                lim = JSRuntime.MAX_SAFE_INTEGER_LONG;
            } else {
                // NOT ToLength()v https://github.com/tc39/ecma262/issues/92
                lim = toUInt32(limit);
                if (lim == 0) {
                    prematureReturnBranch.enter();
                    return array;
                }
            }
            int size = Strings.length(str);
            if (sizeZeroProfile.profile(size == 0)) {
                if (regexExecIntl(splitter, str) == Null.instance) {
                    write(array, 0, str);
                }
                return array;
            }
            int arrayLength = 0;
            int prevMatchEnd = 0;
            int fromIndex = 0;
            while (fromIndex < size) {
                setLastIndex(splitter, fromIndex);
                DynamicObject regexResult = (DynamicObject) regexExecIntl(splitter, str);
                if (resultIsNull.profile(regexResult == Null.instance)) {
                    fromIndex = movePosition(str, unicodeMatching, fromIndex);
                } else {
                    int matchEnd = (int) toLength(getLastIndex(splitter));
                    if (sameMatchEnd.profile(matchEnd == prevMatchEnd)) {
                        fromIndex = movePosition(str, unicodeMatching, fromIndex);
                    } else {
                        write(array, arrayLength, substring(str, prevMatchEnd, fromIndex - prevMatchEnd));
                        arrayLength++;
                        if (arrayLength == lim) {
                            prematureReturnBranch.enter();
                            return array;
                        }
                        prevMatchEnd = matchEnd;
                        fromIndex = matchEnd;
                        long numberOfCaptures = toLength(getLength(regexResult));
                        for (int i = 1; i < numberOfCaptures; i++) {
                            write(array, arrayLength, read(regexResult, i));
                            arrayLength++;
                            if (arrayLength == lim) {
                                prematureReturnBranch.enter();
                                return array;
                            }
                        }
                    }
                }
            }
            int begin = Math.min(prevMatchEnd, Strings.length(str));
            write(array, arrayLength, substring(str, begin, size - begin));
            return array;
        }

        private DynamicObject splitInternal(DynamicObject rx, TruffleString str, long lim) {
            initTRegexAccessors();
            Object tRegexCompiledRegex = compiledRegexProfile.profile(JSRegExp.getCompiledRegex(rx));
            Object tRegexFlags = compiledRegexAccessor.flags(tRegexCompiledRegex);
            boolean unicodeMatching = flagsAccessor.unicode(tRegexFlags);
            DynamicObject splitter;
            if (stickyFlagSet.profile(flagsAccessor.sticky(tRegexFlags))) {
                DynamicObject regexpConstructor = getRealm().getRegExpConstructor();
                splitter = (DynamicObject) getArraySpeciesConstructorNode().construct(regexpConstructor, rx, removeStickyFlag(tRegexFlags));
            } else {
                splitter = rx;
            }
            DynamicObject array = JSArray.createEmptyZeroLength(getContext(), getRealm());
            int size = Strings.length(str);
            int arrayLength = 0;
            int prevMatchEnd = 0;
            int fromIndex = 0;
            int matchStart = -1;
            int matchEnd = -1;
            Object lastRegexResult = null;
            do {
                Object tRegexResult = execIgnoreLastIndexNode.execute(splitter, str, fromIndex);
                if (resultIsNull.profile(!resultAccessor.isMatch(tRegexResult))) {
                    if (sizeZeroProfile.profile(size == 0) || matchStart < 0) {
                        write(array, 0, str);
                        return array;
                    }
                    break;
                } else {
                    if (!getContext().getRegExpStaticResultUnusedAssumption().isValid()) {
                        lastRegexResult = tRegexResult;
                    }
                    matchStart = resultAccessor.captureGroupStart(tRegexResult, 0);
                    matchEnd = resultAccessor.captureGroupEnd(tRegexResult, 0);
                    if (matchEnd == prevMatchEnd) {
                        fromIndex = movePosition(str, unicodeMatching, fromIndex);
                    } else {
                        write(array, arrayLength++, substring(str, prevMatchEnd, matchStart - prevMatchEnd));
                        if (arrayLength == lim) {
                            prematureReturnBranch.enter();
                            return array;
                        }
                        prevMatchEnd = matchEnd;
                        long numberOfCaptures = compiledRegexAccessor.groupCount(tRegexCompiledRegex);
                        for (int i = 1; i < numberOfCaptures; i++) {
                            write(array, arrayLength, TRegexUtil.TRegexMaterializeResultNode.materializeGroup(getContext(), resultAccessor, substringNode, tRegexResult, i, str));
                            arrayLength++;
                            if (arrayLength == lim) {
                                prematureReturnBranch.enter();
                                return array;
                            }
                        }
                        if (matchStart == matchEnd) {
                            fromIndex = movePosition(str, unicodeMatching, fromIndex);
                        } else {
                            fromIndex = matchEnd;
                        }
                    }
                }
            } while (fromIndex < size);
            if (getContext().isOptionRegexpStaticResult() && matchStart >= 0 && matchStart < size) {
                getRealm().setStaticRegexResult(getContext(), tRegexCompiledRegex, str, matchStart, lastRegexResult);
            }
            if (matchStart != matchEnd || prevMatchEnd < size) {
                write(array, arrayLength, substring(str, prevMatchEnd, size - prevMatchEnd));
            }
            return array;
        }

        private Object removeStickyFlag(Object tRegexFlags) {
            char[] flags = new char[6];
            int len = 0;
            if (flagsAccessor.hasIndices(tRegexFlags)) {
                flags[len++] = 'd';
            }
            if (flagsAccessor.global(tRegexFlags)) {
                flags[len++] = 'g';
            }
            if (flagsAccessor.ignoreCase(tRegexFlags)) {
                flags[len++] = 'i';
            }
            if (flagsAccessor.multiline(tRegexFlags)) {
                flags[len++] = 'm';
            }
            if (flagsAccessor.dotAll(tRegexFlags)) {
                flags[len++] = 's';
            }
            if (flagsAccessor.unicode(tRegexFlags)) {
                flags[len++] = 'u';
            }
            if (len == 0) {
                return Strings.EMPTY_STRING;
            }
            return Strings.fromCharArray(flags, 0, len);
        }

        /**
         * Ensure sticky ("y") is part of the flags.
         */
        private Object ensureSticky(TruffleString flags) {
            if (emptyFlags.profile(Strings.length(flags) == 0)) {
                return Strings.Y;
            } else if (stickyFlagSet.profile(indexOf(flags, 'y') >= 0)) {
                return flags;
            } else {
                return concat(flags, Strings.Y);
            }
        }

        private void initTRegexAccessors() {
            if (compiledRegexAccessor == null || flagsAccessor == null || resultAccessor == null || execIgnoreLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compiledRegexAccessor = insert(TRegexUtil.TRegexCompiledRegexAccessor.create());
                flagsAccessor = insert(TRegexUtil.TRegexFlagsAccessor.create());
                resultAccessor = insert(TRegexUtil.TRegexResultAccessor.create());
                execIgnoreLastIndexNode = insert(JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode.create(getContext(), false));
            }
        }

        private boolean isJSObject(DynamicObject rx) {
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsJSObjectNode.create());
            }
            return isObjectNode.executeBoolean(rx);
        }

        private TruffleString toString1(Object obj) {
            if (toString1Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString1Node = insert(JSToStringNode.create());
            }
            return toString1Node.executeString(obj);
        }

        private TruffleString toString2(Object obj) {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node.executeString(obj);
        }

        private Object getFlags(DynamicObject rx) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, false, getContext()));
            }
            return getFlagsNode.getValue(rx);
        }

        private boolean isPristine(DynamicObject rx) {
            if (isPristineObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(getContext()));
            }
            return isPristineObjectNode.execute(rx);
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
                getLengthNode = insert(PropertyGetNode.create(Strings.LENGTH, false, getContext()));
            }
            return getLengthNode.getValue(obj);
        }

        private int movePosition(TruffleString s, boolean unicodeMatching, int lastIndex) {
            return isUnicode.profile(unicodeMatching) ? advanceStringIndexUnicode(s, lastIndex) : lastIndex + 1;
        }

        private long toUInt32(Object obj) {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return toUInt32Node.executeLong(obj);
        }

        private int indexOf(TruffleString a, int codepoint) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfCodePointNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, a, codepoint);
        }

        private TruffleString concat(TruffleString a, TruffleString b) {
            if (stringConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringConcatNode = insert(TruffleString.ConcatNode.create());
            }
            return Strings.concat(stringConcatNode, a, b);
        }

        private TruffleString substring(TruffleString a, int fromIndex, int length) {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                substringNode = insert(TruffleString.SubstringByteIndexNode.create());
            }
            return Strings.substring(getContext(), substringNode, a, fromIndex, length);
        }
    }

    /**
     * This implements the RegExp.prototype.[@@replace] method.
     */
    public abstract static class JSRegExpReplaceNode extends RegExpPrototypeSymbolOperation implements ReplaceStringConsumerTRegex.ParentNode {
        @Child private PropertyGetNode getGlobalNode;
        @Child private PropertyGetNode getUnicodeNode;
        @Child private PropertyGetNode getLengthNode;
        @Child private PropertyGetNode getIndexNode;
        @Child private PropertyGetNode getGroupsNode;
        @Child private TRegexUtil.TRegexResultAccessor readLazyLengthNode;
        @Child private DynamicObjectLibrary lazyRegexResultNode;
        @Child private JSToLengthNode toLengthNode;
        @Child private JSToIntegerAsIntNode toIntegerNode;
        @Child private JSToStringNode toString2Node;
        @Child private JSToStringNode toString3Node;
        @Child private JSToStringNode toString4Node;
        @Child private JSFunctionCallNode functionCallNode;
        @Child private IsJSObjectNode isObjectNode;
        @Child private IsCallableNode isCallableNode;
        @Child private ReadElementNode readNamedCaptureGroupNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private HasHiddenKeyCacheNode hasLazyRegexResultNode;
        @Child private JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndexNode;
        @Child TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor;
        @Child private TRegexUtil.TRegexFlagsAccessor flagsAccessor;
        @Child TRegexUtil.TRegexResultAccessor resultAccessor;
        @Child private TRegexUtil.TRegexNamedCaptureGroupsAccessor namedCaptureGroupsAccessor;
        @Child private IsPristineObjectNode isPristineObjectNode;

        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.AppendSubstringByteIndexNode appendSubStringNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;

        private final ConditionProfile unicodeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile globalProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stickyProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile functionalReplaceProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lazyResultArrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile noMatchProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile validPositionProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasNamedCaptureGroupsProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile dollarProfile = BranchProfile.create();
        final StringBuilderProfile stringBuilderProfile;
        final BranchProfile invalidGroupNumberProfile = BranchProfile.create();
        private final ValueProfile compiledRegexProfile = ValueProfile.createIdentityProfile();
        private final BranchProfile growProfile = BranchProfile.create();

        JSRegExpReplaceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getGlobalNode = PropertyGetNode.create(JSRegExp.GLOBAL, false, context);
            this.getIndexNode = PropertyGetNode.create(JSRegExp.INDEX, false, context);
            this.toIntegerNode = JSToIntegerAsIntNode.create();
            this.isObjectNode = IsJSObjectNode.create();
            this.isCallableNode = IsCallableNode.create();
            this.hasLazyRegexResultNode = HasHiddenKeyCacheNode.create(JSArray.LAZY_REGEX_RESULT_ID);
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
            this.lazyRegexResultNode = DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit);
        }

        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)")
        protected Object replaceCached(DynamicObject rx, Object searchString, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValueWithNCG(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValueWithNamedCG,
                        @Cached(value = "parseReplaceValueWithoutNCG(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValueWithoutNamedCG,
                        @Cached JSToStringNode toString1Node,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode) {
            checkObject(rx);
            if (isPristine(rx)) {
                return replaceInternal(rx, toString1Node.executeString(searchString), cachedReplaceValue, cachedParsedReplaceValueWithNamedCG, cachedParsedReplaceValueWithoutNamedCG);
            }
            return replaceAccordingToSpec(rx, toString1Node.executeString(searchString), cachedReplaceValue, false);
        }

        @Specialization(replaces = "replaceCached")
        protected Object replaceDynamic(DynamicObject rx, Object searchString, Object replaceValue,
                        @Cached JSToStringNode toString1Node) {
            checkObject(rx);
            boolean functionalReplace = functionalReplaceProfile.profile(isCallableNode.executeBoolean(replaceValue));
            Object replaceVal;
            if (functionalReplace) {
                replaceVal = replaceValue;
            } else {
                TruffleString replaceString = toString2(replaceValue);
                replaceVal = replaceString;
                if (isPristine(rx)) {
                    return replaceInternal(rx, toString1Node.executeString(searchString), replaceString, null, null);
                }
            }
            return replaceAccordingToSpec(rx, toString1Node.executeString(searchString), replaceVal, functionalReplace);
        }

        @Fallback
        protected Object doNoObject(Object rx, @SuppressWarnings("unused") Object searchString, @SuppressWarnings("unused") Object replaceValue) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@replace", rx);
        }

        private void checkObject(DynamicObject rx) {
            if (!isObjectNode.executeBoolean(rx)) {
                throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@replace", rx);
            }
        }

        ReplaceStringParser.Token[] parseReplaceValueWithNCG(TruffleString replaceValue) {
            return parseReplaceValue(replaceValue, true);
        }

        ReplaceStringParser.Token[] parseReplaceValueWithoutNCG(TruffleString replaceValue) {
            return parseReplaceValue(replaceValue, false);
        }

        ReplaceStringParser.Token[] parseReplaceValue(TruffleString replaceValue, boolean parseNamedCG) {
            return ReplaceStringParser.parse(getContext(), replaceValue, 100, parseNamedCG);
        }

        private void initTRegexAccessors() {
            if (compiledRegexAccessor == null || flagsAccessor == null || resultAccessor == null || execIgnoreLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compiledRegexAccessor = insert(TRegexUtil.TRegexCompiledRegexAccessor.create());
                flagsAccessor = insert(TRegexUtil.TRegexFlagsAccessor.create());
                resultAccessor = insert(TRegexUtil.TRegexResultAccessor.create());
                execIgnoreLastIndexNode = insert(JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode.create(getContext(), false));
            }
        }

        private Object replaceInternal(DynamicObject rx, TruffleString s, TruffleString replaceString, ReplaceStringParser.Token[] parsedWithNamedCG,
                        ReplaceStringParser.Token[] parsedWithoutNamedCG) {
            initTRegexAccessors();
            Object tRegexCompiledRegex = compiledRegexProfile.profile(JSRegExp.getCompiledRegex(rx));
            Object tRegexFlags = compiledRegexAccessor.flags(tRegexCompiledRegex);
            boolean global = globalProfile.profile(flagsAccessor.global(tRegexFlags));
            boolean unicode = unicodeProfile.profile(flagsAccessor.unicode(tRegexFlags));
            boolean sticky = stickyProfile.profile(flagsAccessor.sticky(tRegexFlags));
            int length = Strings.length(s);
            TruffleStringBuilder sb = stringBuilderProfile.newStringBuilder(length + 16);
            int lastMatchEnd = 0;
            int matchStart = -1;
            int lastIndex = sticky ? (int) toLength(getLastIndex(rx)) : 0;
            Object lastRegexResult = null;
            while (lastIndex <= length) {
                Object tRegexResult = execIgnoreLastIndexNode.execute(rx, s, lastIndex);
                if (noMatchProfile.profile(!resultAccessor.isMatch(tRegexResult))) {
                    if (matchStart < 0) {
                        if (global || sticky) {
                            setLastIndex(rx, 0);
                        }
                        return s;
                    }
                    break;
                }
                if (!getContext().getRegExpStaticResultUnusedAssumption().isValid()) {
                    lastRegexResult = tRegexResult;
                }
                matchStart = resultAccessor.captureGroupStart(tRegexResult, 0);
                int matchEnd = resultAccessor.captureGroupEnd(tRegexResult, 0);
                assert matchStart >= 0 && matchStart <= length && matchStart >= lastMatchEnd;
                append(sb, s, lastMatchEnd, matchStart);
                boolean namedCG = hasNamedCaptureGroupsProfile.profile(!getNamedCaptureGroupsAccessor().isNull(compiledRegexAccessor.namedCaptureGroups(tRegexCompiledRegex)));
                if (parsedWithNamedCG == null) {
                    ReplaceStringParser.process(getContext(), replaceString, compiledRegexAccessor.groupCount(tRegexCompiledRegex), namedCG, dollarProfile,
                                    new ReplaceStringConsumerTRegex(sb, s, replaceString, matchStart, matchEnd, tRegexResult, tRegexCompiledRegex), this);
                } else {
                    ReplaceStringParser.processParsed(namedCG ? parsedWithNamedCG : parsedWithoutNamedCG,
                                    new ReplaceStringConsumerTRegex(sb, s, replaceString, matchStart, matchEnd, tRegexResult, tRegexCompiledRegex), this);
                }
                lastMatchEnd = matchEnd;
                if (global) {
                    if (matchStart == matchEnd) {
                        lastIndex = unicode ? advanceStringIndexUnicode(s, matchEnd) : matchEnd + 1;
                    } else {
                        lastIndex = matchEnd;
                    }
                } else {
                    break;
                }
            }
            if (getContext().isOptionRegexpStaticResult() && matchStart >= 0) {
                getRealm().setStaticRegexResult(getContext(), tRegexCompiledRegex, s, matchStart, lastRegexResult);
            }
            if (global || sticky) {
                setLastIndex(rx, sticky ? lastMatchEnd : 0);
            }
            if (lastMatchEnd < length) {
                append(sb, s, lastMatchEnd, length);
            }
            return builderToString(sb);
        }

        private Object replaceAccordingToSpec(DynamicObject rx, TruffleString s, Object replaceValue, boolean functionalReplace) {
            TruffleString replaceString = null;
            DynamicObject replaceFunction = null;
            if (functionalReplace) {
                replaceFunction = (DynamicObject) replaceValue;
            } else {
                replaceString = (TruffleString) replaceValue;
            }
            boolean global = globalProfile.profile(getFlag(rx, getGlobalNode));
            boolean fullUnicode = false;
            if (global) {
                fullUnicode = unicodeProfile.profile(getFlag(rx, getGetUnicodeNode()));
                setLastIndex(rx, 0);
            }
            SimpleArrayList<DynamicObject> results = null;
            if (functionalReplace) {
                results = new SimpleArrayList<>();
            }
            int length = Strings.length(s);
            TruffleStringBuilder sb = stringBuilderProfile.newStringBuilder(length + 16);
            int nextSourcePosition = 0;
            int matchLength = -1;
            while (true) {
                DynamicObject result = (DynamicObject) regexExecIntl(rx, s);
                if (noMatchProfile.profile(result == Null.instance)) {
                    if (matchLength < 0) {
                        return s;
                    }
                    break;
                }
                if (lazyResultArrayProfile.profile(isLazyResultArray(result))) {
                    matchLength = getLazyLength(result);
                } else {
                    matchLength = processNonLazy(result);
                }
                if (functionalReplace) {
                    results.add(result, growProfile);
                } else {
                    int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), Strings.length(s)), 0);
                    if (validPositionProfile.profile(position >= nextSourcePosition)) {
                        append(sb, s, nextSourcePosition, position);
                        Object namedCaptures = getGroups(result);
                        if (namedCaptures != Undefined.instance) {
                            namedCaptures = toObject(namedCaptures);
                        }
                        ReplaceStringParser.process(getContext(), replaceString, (int) toLength(getLength(result)), namedCaptures != Undefined.instance, dollarProfile,
                                        new ReplaceStringConsumer(sb, s, replaceString, position, Math.min(position + matchLength, Strings.length(s)), result, (DynamicObject) namedCaptures), this);
                        nextSourcePosition = position + matchLength;
                    }
                }
                if (global) {
                    if (matchLength == 0) {
                        long lastI = toLength(getLastIndex(rx));
                        long nextIndex = lastI + 1;
                        if (JSRuntime.longIsRepresentableAsInt(nextIndex)) {
                            setLastIndex(rx, fullUnicode ? advanceStringIndexUnicode(s, (int) lastI) : (int) nextIndex);
                        } else {
                            setLastIndex(rx, (double) nextIndex);
                        }
                    }
                } else {
                    break;
                }
            }
            if (functionalReplace) {
                for (int i = 0; i < results.size(); i++) {
                    DynamicObject result = results.get(i);
                    int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), Strings.length(s)), 0);
                    int resultsLength = (int) toLength(getLength(result));
                    Object namedCaptures = getGroups(result);
                    Object[] arguments = new Object[resultsLength + 4 + (namedCaptures != Undefined.instance ? 1 : 0)];
                    arguments[0] = Undefined.instance;
                    arguments[1] = replaceFunction;
                    for (int i1 = 0; i1 < resultsLength; i1++) {
                        arguments[i1 + 2] = read(result, i1);
                    }
                    arguments[resultsLength + 2] = position;
                    arguments[resultsLength + 3] = s;
                    if (namedCaptures != Undefined.instance) {
                        arguments[resultsLength + 4] = namedCaptures;
                    }
                    Object callResult = callFunction(arguments);
                    TruffleString replacement = toString2(callResult);
                    if (validPositionProfile.profile(position >= nextSourcePosition)) {
                        append(sb, s, nextSourcePosition, position);
                        append(sb, replacement);
                        if (lazyResultArrayProfile.profile(isLazyResultArray(result))) {
                            nextSourcePosition = position + getLazyLength(result);
                        } else {
                            nextSourcePosition = position + Strings.length((TruffleString) read(result, 0));
                        }
                    }
                }
            }
            if (nextSourcePosition < length) {
                append(sb, s, nextSourcePosition, length);
            }
            return builderToString(sb);
        }

        private int processNonLazy(DynamicObject result) {
            int resultLength = (int) toLength(getLength(result));
            TruffleString result0Str = toString3(read(result, 0));
            write(result, 0, result0Str);
            for (int n = 1; n < resultLength; n++) {
                Object value = read(result, n);
                if (value != Undefined.instance) {
                    write(result, n, toString3(value));
                }
            }
            return Strings.length(result0Str);
        }

        private boolean isLazyResultArray(DynamicObject result) {
            boolean isLazyResultArray = hasLazyRegexResultNode.executeHasHiddenKey(result);
            assert isLazyResultArray == JSDynamicObject.hasProperty(result, JSArray.LAZY_REGEX_RESULT_ID);
            return isLazyResultArray;
        }

        private Object callFunction(Object[] arguments) {
            if (functionCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionCallNode = insert(JSFunctionCallNode.createCall());
            }
            return functionCallNode.executeCall(arguments);
        }

        private TruffleString toString2(Object obj) {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node.executeString(obj);
        }

        private TruffleString toString3(Object obj) {
            if (toString3Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString3Node = insert(JSToStringNode.create());
            }
            return toString3Node.executeString(obj);
        }

        final TruffleString toString4(Object obj) {
            if (toString4Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString4Node = insert(JSToStringNode.create());
            }
            return toString4Node.executeString(obj);
        }

        private int getLazyLength(DynamicObject obj) {
            if (readLazyLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readLazyLengthNode = insert(TRegexUtil.TRegexResultAccessor.create());
            }
            return readLazyLengthNode.captureGroupLength(JSAbstractArray.arrayGetRegexResult(obj, lazyRegexResultNode), 0);
        }

        private PropertyGetNode getGetUnicodeNode() {
            if (getUnicodeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getUnicodeNode = insert(PropertyGetNode.create(JSRegExp.UNICODE, false, getContext()));
            }
            return getUnicodeNode;
        }

        private boolean isPristine(DynamicObject rx) {
            if (isPristineObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(getContext()));
            }
            return isPristineObjectNode.execute(rx);
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

        private Object getGroups(Object regexResult) {
            if (getGroupsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getGroupsNode = insert(PropertyGetNode.create(JSRegExp.GROUPS, false, getContext()));
            }
            return getGroupsNode.getValue(regexResult);
        }

        final Object readNamedCaptureGroup(Object namedCaptureGroups, Object groupNameStr) {
            if (readNamedCaptureGroupNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNamedCaptureGroupNode = insert(ReadElementNode.create(getContext()));
            }
            return readNamedCaptureGroupNode.executeWithTargetAndIndex(namedCaptureGroups, groupNameStr);
        }

        private Object toObject(Object obj) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.execute(obj);
        }

        @Override
        public void append(TruffleStringBuilder sb, TruffleString s) {
            if (appendStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendStringNode = insert(TruffleStringBuilder.AppendStringNode.create());
            }
            stringBuilderProfile.append(appendStringNode, sb, s);
        }

        @Override
        public void append(TruffleStringBuilder sb, TruffleString s, int fromIndex, int toIndex) {
            if (appendSubStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendSubStringNode = insert(TruffleStringBuilder.AppendSubstringByteIndexNode.create());
            }
            stringBuilderProfile.append(appendSubStringNode, sb, s, fromIndex, toIndex);
        }

        private TruffleString builderToString(TruffleStringBuilder sb) {
            if (builderToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                builderToStringNode = insert(TruffleStringBuilder.ToStringNode.create());
            }
            return StringBuilderProfile.toString(builderToStringNode, sb);
        }

        @Override
        public final TRegexUtil.TRegexNamedCaptureGroupsAccessor getNamedCaptureGroupsAccessor() {
            if (namedCaptureGroupsAccessor == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                namedCaptureGroupsAccessor = insert(TRegexUtil.TRegexNamedCaptureGroupsAccessor.create());
            }
            return namedCaptureGroupsAccessor;
        }

        @Override
        public TRegexUtil.TRegexCompiledRegexAccessor getCompiledRegexAccessor() {
            return compiledRegexAccessor;
        }

        @Override
        public TRegexUtil.TRegexResultAccessor getResultAccessor() {
            return resultAccessor;
        }

        @Override
        public BranchProfile getInvalidGroupNumberProfile() {
            return invalidGroupNumberProfile;
        }
    }

    public static final class ReplaceStringConsumer implements ReplaceStringParser.Consumer<JSRegExpReplaceNode, TruffleStringBuilder> {

        private final TruffleStringBuilder sb;
        private final TruffleString input;
        private final TruffleString replaceStr;
        private final int startPos;
        private final int endPos;
        private final DynamicObject result;
        private final DynamicObject namedCaptures;

        private ReplaceStringConsumer(TruffleStringBuilder sb, TruffleString input, TruffleString replaceStr, int startPos, int endPos, DynamicObject result, DynamicObject namedCaptures) {
            this.sb = sb;
            this.input = input;
            this.replaceStr = replaceStr;
            this.startPos = startPos;
            this.endPos = endPos;
            this.result = result;
            this.namedCaptures = namedCaptures;
        }

        @Override
        public void literal(JSRegExpReplaceNode node, int start, int end) {
            node.append(sb, replaceStr, start, end);
        }

        @Override
        public void match(JSRegExpReplaceNode node) {
            node.append(sb, (TruffleString) node.read(result, 0));
        }

        @Override
        public void matchHead(JSRegExpReplaceNode node) {
            node.append(sb, input, 0, startPos);
        }

        @Override
        public void matchTail(JSRegExpReplaceNode node) {
            node.append(sb, input, endPos, Strings.length(input));
        }

        @Override
        public void captureGroup(JSRegExpReplaceNode node, int groupNumber, int literalStart, int literalEnd) {
            Object capture = node.read(result, groupNumber);
            if (capture != Undefined.instance) {
                node.append(sb, (TruffleString) capture);
            }
        }

        @Override
        public void namedCaptureGroup(JSRegExpReplaceNode node, TruffleString groupName) {
            Object namedCapture = node.readNamedCaptureGroup(namedCaptures, groupName);
            if (namedCapture != Undefined.instance) {
                node.append(sb, node.toString4(namedCapture));
            }
        }

        @Override
        public TruffleStringBuilder getResult() {
            return sb;
        }
    }

    public static final class ReplaceStringConsumerTRegex implements ReplaceStringParser.Consumer<ReplaceStringConsumerTRegex.ParentNode, TruffleStringBuilder> {

        public interface ParentNode {

            TRegexUtil.TRegexCompiledRegexAccessor getCompiledRegexAccessor();

            TRegexUtil.TRegexResultAccessor getResultAccessor();

            TRegexUtil.TRegexNamedCaptureGroupsAccessor getNamedCaptureGroupsAccessor();

            void append(TruffleStringBuilder sb, TruffleString s);

            void append(TruffleStringBuilder sb, TruffleString s, int fromIndex, int toIndex);

            BranchProfile getInvalidGroupNumberProfile();
        }

        private final TruffleStringBuilder sb;
        private final TruffleString input;
        private final TruffleString replaceStr;
        private final int startPos;
        private final int endPos;
        private final Object tRegexResult;
        private final Object tRegexCompiledRegex;

        public ReplaceStringConsumerTRegex(TruffleStringBuilder sb, TruffleString input, TruffleString replaceStr, int startPos, int endPos, Object tRegexResult, Object tRegexCompiledRegex) {
            this.sb = sb;
            this.input = input;
            this.replaceStr = replaceStr;
            this.startPos = startPos;
            this.endPos = endPos;
            this.tRegexResult = tRegexResult;
            this.tRegexCompiledRegex = tRegexCompiledRegex;
        }

        @Override
        public void literal(ParentNode node, int start, int end) {
            node.append(sb, replaceStr, start, end);
        }

        @Override
        public void match(ParentNode node) {
            node.append(sb, input, startPos, endPos);
        }

        @Override
        public void matchHead(ParentNode node) {
            node.append(sb, input, 0, startPos);
        }

        @Override
        public void matchTail(ParentNode node) {
            node.append(sb, input, endPos, Strings.length(input));
        }

        @Override
        public void captureGroup(ParentNode node, int groupNumber, int literalStart, int literalEnd) {
            TRegexResultAccessor resultAccessor = node.getResultAccessor();
            int groupCount = node.getCompiledRegexAccessor().groupCount(tRegexCompiledRegex);
            if (groupNumber < groupCount) {
                int start = resultAccessor.captureGroupStart(tRegexResult, groupNumber);
                if (start >= 0) {
                    node.append(sb, input, start, resultAccessor.captureGroupEnd(tRegexResult, groupNumber));
                }
            } else if (groupNumber > 9 && groupNumber / 10 < groupCount) {
                int start = resultAccessor.captureGroupStart(tRegexResult, groupNumber / 10);
                if (start >= 0) {
                    node.append(sb, input, start, resultAccessor.captureGroupEnd(tRegexResult, groupNumber / 10));
                }
                node.append(sb, replaceStr, literalStart + 2, literalEnd);
            } else {
                node.getInvalidGroupNumberProfile().enter();
                node.append(sb, replaceStr, literalStart, literalEnd);
            }
        }

        @Override
        public void namedCaptureGroup(ParentNode node, TruffleString groupName) {
            Object map = node.getCompiledRegexAccessor().namedCaptureGroups(tRegexCompiledRegex);
            if (node.getNamedCaptureGroupsAccessor().hasGroup(map, groupName)) {
                int groupNumber = node.getNamedCaptureGroupsAccessor().getGroupNumber(map, groupName);
                int start = node.getResultAccessor().captureGroupStart(tRegexResult, groupNumber);
                if (start >= 0) {
                    node.append(sb, input, start, node.getResultAccessor().captureGroupEnd(tRegexResult, groupNumber));
                }
            }
        }

        @Override
        public TruffleStringBuilder getResult() {
            return sb;
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
            this.getUnicodeNode = PropertyGetNode.create(JSRegExp.UNICODE, false, context);
            this.getGlobalNode = PropertyGetNode.create(JSRegExp.GLOBAL, false, context);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(rx)", limit = "1")
        protected Object match(DynamicObject rx, Object param,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toString1Node,
                        @Cached JSToStringNode toString2Node) {
            TruffleString s = toString1Node.executeString(param);
            if (isGlobalProfile.profile(!getFlag(rx, getGlobalNode))) {
                return regexExecIntl(rx, s);
            } else {
                boolean fullUnicode = getFlag(rx, getUnicodeNode);
                setLastIndex(rx, 0);
                DynamicObject array = JSArray.createEmptyZeroLength(getContext(), getRealm());
                int n = 0;
                DynamicObject result;
                TruffleString matchStr;
                while (true) {
                    result = (DynamicObject) regexExecIntl(rx, s);
                    if (result == Null.instance) {
                        return n == 0 ? Null.instance : array;
                    }
                    matchStr = toString2Node.executeString(read(result, 0));
                    write(array, n, matchStr);
                    if (Strings.length(matchStr) == 0) {
                        int lastI = toLength(getLastIndex(rx));
                        setLastIndex(rx, unicodeProfile.profile(fullUnicode) ? advanceStringIndexUnicode(s, lastI) : lastI + 1);
                    }
                    n++;
                }
            }
        }

        @Fallback
        protected Object match(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@match", thisObj);
        }

        private int toLength(Object obj) {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return (int) toLengthNode.executeLong(obj);
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
            this.getIndexNode = PropertyGetNode.create(JSRegExp.INDEX, false, context);
            this.sameValueNode = JSIdenticalNode.createSameValue();
        }

        @Specialization(guards = "isObjectNode.executeBoolean(rx)", limit = "1")
        protected Object search(DynamicObject rx, Object param,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toString1Node) {
            Object s = toString1Node.executeString(param);
            Object previousLastIndex = getLastIndex(rx);
            if (!sameValueNode.executeBoolean(previousLastIndex, 0)) {
                setLastIndex(rx, 0);
            }
            Object result = regexExecIntl(rx, s);
            Object currentLastIndex = getLastIndex(rx);
            if (!sameValueNode.executeBoolean(currentLastIndex, previousLastIndex)) {
                setLastIndex(rx, previousLastIndex);
            }
            return result == Null.instance ? -1 : getIndexNode.getValue(result);
        }

        @Fallback
        protected Object search(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@search", thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.[@@matchAll] method.
     */
    @ImportStatic(JSRegExp.class)
    public abstract static class JSRegExpMatchAllNode extends JSBuiltinNode {

        public JSRegExpMatchAllNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(regex)", limit = "1")
        protected Object matchAll(VirtualFrame frame, DynamicObject regex, Object stringObj,
                        @Cached JSToStringNode toStringNodeForInput,
                        @Cached("createSpeciesConstructNode()") ArraySpeciesConstructorNode speciesConstructNode,
                        @Cached("create(FLAGS, getContext())") PropertyGetNode getFlagsNode,
                        @Cached JSToStringNode toStringNodeForFlags,
                        @Cached("create(LAST_INDEX, getContext())") PropertyGetNode getLastIndexNode,
                        @Cached JSToLengthNode toLengthNode,
                        @Cached("create(LAST_INDEX, FALSE, getContext(), TRUE)") PropertySetNode setLastIndexNode,
                        @Cached("createCreateRegExpStringIteratorNode()") StringPrototypeBuiltins.CreateRegExpStringIteratorNode createRegExpStringIteratorNode,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached ConditionProfile indexInIntRangeProf,
                        @Cached TruffleString.ByteIndexOfCodePointNode stringIndexOfNode) {
            Object string = toStringNodeForInput.executeString(stringObj);
            DynamicObject regExpConstructor = getRealm().getRegExpConstructor();
            DynamicObject constructor = speciesConstructNode.speciesConstructor(regex, regExpConstructor);
            TruffleString flags = toStringNodeForFlags.executeString(getFlagsNode.getValue(regex));
            Object matcher = speciesConstructNode.construct(constructor, regex, flags);
            long lastIndex = toLengthNode.executeLong(getLastIndexNode.getValue(regex));
            setLastIndexNode.setValue(matcher, JSRuntime.boxIndex(lastIndex, indexInIntRangeProf));
            boolean global = Strings.indexOf(stringIndexOfNode, flags, 'g') != -1;
            boolean fullUnicode = Strings.indexOf(stringIndexOfNode, flags, 'u') != -1;
            return createRegExpStringIteratorNode.createIterator(frame, matcher, string, global, fullUnicode);
        }

        ArraySpeciesConstructorNode createSpeciesConstructNode() {
            return ArraySpeciesConstructorNode.create(getContext(), false);
        }

        StringPrototypeBuiltins.CreateRegExpStringIteratorNode createCreateRegExpStringIteratorNode() {
            return new StringPrototypeBuiltins.CreateRegExpStringIteratorNode(getContext());
        }

        @Fallback
        protected Object matchAll(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@matchAll", thisObj);
        }

    }

    public static final class RegExpPrototypeGetterBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpPrototypeGetterBuiltins.RegExpPrototypeGetters> {

        public static final JSBuiltinsContainer BUILTINS = new RegExpPrototypeGetterBuiltins();

        protected RegExpPrototypeGetterBuiltins() {
            super(JSRegExp.PROTOTYPE_NAME, RegExpPrototypeGetters.class);
        }

        public enum RegExpPrototypeGetters implements BuiltinEnum<RegExpPrototypeGetters> {

            flags(0),
            source(0),
            global(0),
            multiline(0),
            ignoreCase(0),
            sticky(0),     // ES 2015
            unicode(0),    // ES 2015
            dotAll(0),     // ES 2018
            hasIndices(0); // ES 2022

            private final int length;

            RegExpPrototypeGetters(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }

            @Override
            public int getECMAScriptVersion() {
                if (EnumSet.of(sticky, unicode).contains(this)) {
                    return 6;
                } else if (this == dotAll) {
                    return JSConfig.ECMAScript2018;
                }
                // Note: hasIndices (ES 2022) may be enabled using regexp-match-indices flag, too.
                return BuiltinEnum.super.getECMAScriptVersion();
            }

            @Override
            public boolean isGetter() {
                return true;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RegExpPrototypeGetters builtinEnum) {
            switch (builtinEnum) {
                case source:
                    return CompiledRegexPatternAccessor.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
                case flags:
                    return RegExpFlagsGetterNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
                default:
                    return CompiledRegexFlagPropertyAccessor.create(context, builtin, builtinEnum.name(), args().withThis().fixedArgs(0).createArgumentNodes(context));
            }
        }
    }

    /**
     * Implements the RegExp.prototype.flags getter.
     */
    public abstract static class RegExpFlagsGetterNode extends JSBuiltinNode {
        @Child private PropertyGetNode getGlobal;
        @Child private PropertyGetNode getIgnoreCase;
        @Child private PropertyGetNode getMultiline;
        @Child private PropertyGetNode getDotAll;
        @Child private PropertyGetNode getUnicode;
        @Child private PropertyGetNode getSticky;
        @Child private PropertyGetNode getHasIndices;
        @Child private JSToBooleanNode toBoolean;

        public RegExpFlagsGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getGlobal = PropertyGetNode.create(JSRegExp.GLOBAL, context);
            this.getIgnoreCase = PropertyGetNode.create(JSRegExp.IGNORE_CASE, context);
            this.getMultiline = PropertyGetNode.create(JSRegExp.MULTILINE, context);
            if (context.getEcmaScriptVersion() >= 9) {
                this.getDotAll = PropertyGetNode.create(JSRegExp.DOT_ALL, context);
            }
            this.getUnicode = PropertyGetNode.create(JSRegExp.UNICODE, context);
            this.getSticky = PropertyGetNode.create(JSRegExp.STICKY, context);
            if (context.isOptionRegexpMatchIndices()) {
                this.getHasIndices = PropertyGetNode.create(JSRegExp.HAS_INDICES, context);
            }
        }

        @Specialization(guards = "isObjectNode.executeBoolean(rx)", limit = "1")
        protected Object doObject(DynamicObject rx,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached TruffleString.FromCharArrayUTF16Node fromCharArrayNode) {
            char[] flags = new char[JSRegExp.MAX_FLAGS_LENGTH];
            int len = 0;
            if (getHasIndices != null && getFlag(rx, getHasIndices)) {
                flags[len++] = 'd';
            }
            if (getFlag(rx, getGlobal)) {
                flags[len++] = 'g';
            }
            if (getFlag(rx, getIgnoreCase)) {
                flags[len++] = 'i';
            }
            if (getFlag(rx, getMultiline)) {
                flags[len++] = 'm';
            }
            if (getDotAll != null && getFlag(rx, getDotAll)) {
                flags[len++] = 's';
            }
            if (getFlag(rx, getUnicode)) {
                flags[len++] = 'u';
            }
            if (getFlag(rx, getSticky)) {
                flags[len++] = 'y';
            }
            if (len == 0) {
                return Strings.EMPTY_STRING;
            }
            return Strings.fromCharArray(fromCharArrayNode, flags, 0, len);
        }

        @Fallback
        protected Object doNotObject(Object thisObj) {
            throw Errors.createTypeErrorNotAnObject(thisObj);
        }

        private boolean getFlag(DynamicObject re, PropertyGetNode getNode) {
            boolean flag;
            if (toBoolean == null) {
                try {
                    flag = getNode.getValueBoolean(re);
                } catch (UnexpectedResultException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.toBoolean = insert(JSToBooleanNode.create());
                    flag = toBoolean.executeBoolean(e.getResult());
                }
            } else {
                flag = toBoolean.executeBoolean(getNode.getValue(re));
            }
            return flag;
        }
    }

    abstract static class CompiledRegexFlagPropertyAccessor extends JSBuiltinNode {

        @Child TRegexUtil.TRegexCompiledRegexSingleFlagAccessor readNode;

        CompiledRegexFlagPropertyAccessor(JSContext context, JSBuiltin builtin, String flagName) {
            super(context, builtin);
            readNode = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(flagName);
        }

        @Specialization
        Object doRegExp(JSRegExpObject obj) {
            return readNode.get(JSRegExp.getCompiledRegex(obj));
        }

        @Specialization(guards = "isRegExpPrototype(obj)")
        Object doPrototype(@SuppressWarnings("unused") DynamicObject obj) {
            return Undefined.instance;
        }

        @Fallback
        public Object doObject(Object obj) {
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }

        boolean isRegExpPrototype(DynamicObject obj) {
            return obj == getRealm().getRegExpPrototype();
        }

        public static CompiledRegexFlagPropertyAccessor create(JSContext context, JSBuiltin builtin, String flagName, JavaScriptNode[] args) {
            return RegExpPrototypeBuiltinsFactory.CompiledRegexFlagPropertyAccessorNodeGen.create(context, builtin, flagName, args);
        }
    }

    abstract static class CompiledRegexPatternAccessor extends JSBuiltinNode {

        @Child TRegexUtil.InteropReadStringMemberNode readPatternNode = TRegexUtil.InteropReadStringMemberNode.create();

        CompiledRegexPatternAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        Object doRegExp(JSRegExpObject obj) {
            return JSRegExp.escapeRegExpPattern(readPatternNode.execute(JSRegExp.getCompiledRegex(obj), TRegexUtil.Props.CompiledRegex.PATTERN));
        }

        @Specialization(guards = "isRegExpPrototype(obj)")
        Object doPrototype(@SuppressWarnings("unused") DynamicObject obj) {
            return Strings.EMPTY_REGEX;
        }

        @Fallback
        public Object doObject(Object obj) {
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }

        boolean isRegExpPrototype(DynamicObject obj) {
            return obj == getRealm().getRegExpPrototype();
        }

        static CompiledRegexPatternAccessor create(JSContext context, JSBuiltin builtin, JavaScriptNode[] args) {
            return RegExpPrototypeBuiltinsFactory.CompiledRegexPatternAccessorNodeGen.create(context, builtin, args);
        }
    }

}
