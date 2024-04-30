/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
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
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
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
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadBooleanMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadIntMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropToIntNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InvokeGetGroupBoundariesMethodNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexCompiledRegexAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexFlagsAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResult;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexNamedCaptureGroupsAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtilFactory.InteropToIntNodeGen;

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
        TruffleString objName = Strings.fromObject(JSRuntime.toPrimitive(obj, JSToPrimitiveNode.Hint.String));
        return Errors.createTypeError(Strings.toJavaString(objName) + " is not a RegExp");
    }

    /**
     * This implements the deprecated RegExp.prototype.compile() method.
     */
    @ImportStatic(JSRegExp.class)
    public abstract static class JSRegExpCompileNode extends JSBuiltinNode {

        protected JSRegExpCompileNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSRegExpObject compile(JSRegExpObject thisRegExp, Object patternObj, Object flagsObj,
                        @Bind("this") Node node,
                        @Cached("create(LAST_INDEX, false, getContext(), true)") PropertySetNode setLastIndexNode,
                        @Cached("create(getContext())") CompileRegexNode compileRegexNode,
                        @Cached("createUndefinedToEmpty()") JSToStringNode toStringNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile isRegExpProfile,
                        @Cached(inline = true) TRegexUtil.InteropReadStringMemberNode readPattern,
                        @Cached(inline = true) TRegexUtil.InteropReadMemberNode readFlags,
                        @Cached(inline = true) TRegexUtil.InteropReadStringMemberNode readSource) {
            Object pattern;
            Object flags;

            if (getRealm() != JSRegExp.getRealm(thisRegExp)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("RegExp.prototype.compile cannot be used on a RegExp from a different Realm.");
            }
            if (!JSRegExp.getLegacyFeaturesEnabled(thisRegExp)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("RegExp.prototype.compile cannot be used on subclasses of RegExp.");
            }
            boolean isRegExp = isRegExpProfile.profile(node, JSRegExp.isJSRegExp(patternObj));
            if (isRegExp) {
                if (flagsObj != Undefined.instance) {
                    errorBranch.enter(node);
                    throw Errors.createTypeError("flags must be undefined", node);
                }
                Object regex = JSRegExp.getCompiledRegex((JSDynamicObject) patternObj);
                pattern = TRegexCompiledRegexAccessor.pattern(regex, node, readPattern);
                flags = TRegexFlagsAccessor.source(TRegexCompiledRegexAccessor.flags(regex, node, readFlags), node, readSource);
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
        JSDynamicObject doString(JSRegExpObject thisRegExp, TruffleString inputStr) {
            return (JSDynamicObject) regExpNode.execute(thisRegExp, inputStr);
        }

        @Specialization(replaces = {"doString"})
        JSDynamicObject doObject(JSRegExpObject thisRegExp, Object input,
                        @Cached JSToStringNode toStringNode) {
            return (JSDynamicObject) regExpNode.execute(thisRegExp, toStringNode.executeString(input));
        }

        @Fallback
        Object doNoRegExp(Object thisObj, @SuppressWarnings("unused") Object input) {
            throw createNoRegExpError(thisObj);
        }
    }

    /**
     * This implements the RegExp.prototype.exec() method as defined by ECMAScript 5.
     */
    @ImportStatic(JSRegExp.class)
    public abstract static class JSRegExpExecES5Node extends JSBuiltinNode {

        protected JSRegExpExecES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
        }

        public abstract JSDynamicObject execute(Object thisRegExp, Object input);

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSDynamicObject exec(JSRegExpObject thisRegExp, Object input,
                        @Bind("this") Node node,
                        @Cached JSToStringNode toStringNode,
                        @Cached("create(getContext())") JSRegExpExecBuiltinNode regExpNode,
                        @Cached(inline = true) TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached(inline = true) TRegexUtil.InteropReadIntMemberNode readGroupCount,
                        @Cached(inline = true) TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        @Cached(inline = true) TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached InlinedCountingConditionProfile ifIsMatch,
                        @Cached("create(INDEX, false, getContext(), false)") PropertySetNode setIndexNode,
                        @Cached("create(INPUT, false, getContext(), false)") PropertySetNode setInputNode) {
            assert getContext().getEcmaScriptVersion() < 6;
            TruffleString inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisRegExp, inputStr);
            if (ifIsMatch.profile(node, TRegexResultAccessor.isMatch(result, node, readIsMatch))) {
                int groupCount = TRegexCompiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(thisRegExp), node, readGroupCount);
                // Convert RegexResult into JS Array.
                Object[] matches = TRegexMaterializeResult.materializeFull(getContext(), result, groupCount, inputStr, node, substringNode, getStart, getEnd);
                JSObject array = JSArray.createConstant(getContext(), getRealm(), matches);
                setIndexNode.setValueInt(array, TRegexResultAccessor.captureGroupStart(result, 0, node, getStart));
                setInputNode.setValue(array, inputStr);
                return array;
            } else {
                return Null.instance;
            }
        }

        @Fallback
        protected static JSDynamicObject exec(Object thisObj, @SuppressWarnings("unused") Object input) {
            throw createNoRegExpError(thisObj);
        }

        @NeverDefault
        static JSRegExpExecES5Node create(JSContext context) {
            return JSRegExpExecES5NodeGen.create(context, null, null);
        }
    }

    /**
     * This implements the RegExp.prototype.test() method as defined by ECMAScript 5.1 15.10.6.3.
     */
    public abstract static class JSRegExpTestNode extends JSBuiltinNode {

        protected JSRegExpTestNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isObjectNode.executeBoolean(thisObj)", limit = "1")
        protected Object testGeneric(JSDynamicObject thisObj, Object input,
                        @Bind("this") Node node,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached("create(getContext())") JSRegExpExecIntlNode regExpNode,
                        @Cached(inline = true) TRegexUtil.InteropReadBooleanMemberNode readIsMatch) {
            TruffleString inputStr = toStringNode.executeString(input);
            Object result = regExpNode.execute(thisObj, inputStr);
            if (getContext().getEcmaScriptVersion() >= 6) {
                return (result != Null.instance);
            } else {
                return readIsMatch.execute(node, result, TRegexUtil.Props.RegexResult.IS_MATCH);
            }
        }

        @Fallback
        protected Object testError(Object thisNonObj, @SuppressWarnings("unused") Object input) {
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
            this.getFlagsNode = PropertyGetNode.create(Strings.FLAGS, false, context);
            this.getSourceNode = PropertyGetNode.create(Strings.SOURCE, false, context);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(thisObj)", limit = "1")
        protected Object toString(JSDynamicObject thisObj,
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

    @GenerateInline
    @GenerateCached(false)
    protected abstract static class AdvanceStringIndexUnicodeNode extends JavaScriptBaseNode {

        public abstract int execute(Node node, TruffleString s, int index);

        @Specialization
        protected static int advanceStringIndexUnicode(Node node, TruffleString s, int index,
                        @Cached(inline = false) TruffleString.ReadCharUTF16Node readChar,
                        @Cached InlinedConditionProfile advanceIndexLength,
                        @Cached InlinedConditionProfile advanceIndexFirst,
                        @Cached InlinedConditionProfile advanceIndexSecond) {
            if (advanceIndexLength.profile(node, index + 1 >= Strings.length(s))) {
                return index + 1;
            }
            char first = Strings.charAt(readChar, s, index);
            if (advanceIndexFirst.profile(node, first < 0xD800 || first > 0xDBFF)) {
                return index + 1;
            }
            char second = Strings.charAt(readChar, s, index + 1);
            if (advanceIndexSecond.profile(node, second < 0xDC00 || second > 0xDFFF)) {
                return index + 1;
            }
            return index + 2;
        }

    }

    public abstract static class RegExpPrototypeSymbolOperation extends JSBuiltinNode {

        @Child private JSRegExpExecIntlNode regexExecIntlNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private WriteElementNode writeNode;
        @Child private ReadElementNode readNode;
        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;

        public RegExpPrototypeSymbolOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected Object read(Object target, long index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ReadElementNode.create(getContext()));
            }
            return readNode.executeWithTargetAndIndex(target, index);
        }

        protected void write(Object target, long index, Object value) {
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

        protected Object regexExecIntl(Object regex, TruffleString input) {
            if (regexExecIntlNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                regexExecIntlNode = insert(JSRegExpExecIntlNode.create(getContext()));
            }
            return regexExecIntlNode.execute(regex, input);
        }

    }

    /**
     * This implements the RegExp.prototype.[@@split] method.
     */
    @ImportStatic(JSGuards.class)
    public abstract static class JSRegExpSplitNode extends RegExpPrototypeSymbolOperation {
        @Child private IsJSObjectNode isObjectNode;
        @Child private JSToStringNode toString1Node;
        @Child private IsPristineObjectNode isPristineObjectNode;

        JSRegExpSplitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        JSArrayObject splitIntLimit(JSDynamicObject rx, Object input, int limit,
                        @Cached @Shared JSToUInt32Node toUInt32,
                        @Cached @Shared InlinedBranchProfile limitZeroBranch,
                        @Cached @Shared SplitInternalNode splitInternal,
                        @Cached @Shared SplitAccordingToSpecNode splitAccordingToSpec) {
            return doSplit(rx, input, toUInt32.executeLong(limit),
                            this, limitZeroBranch, splitInternal, splitAccordingToSpec);
        }

        @Specialization
        JSArrayObject splitLongLimit(JSDynamicObject rx, Object input, long limit,
                        @Cached @Shared JSToUInt32Node toUInt32,
                        @Cached @Shared InlinedBranchProfile limitZeroBranch,
                        @Cached @Shared SplitInternalNode splitInternal,
                        @Cached @Shared SplitAccordingToSpecNode splitAccordingToSpec) {
            return doSplit(rx, input, toUInt32.executeLong(limit),
                            this, limitZeroBranch, splitInternal, splitAccordingToSpec);
        }

        @Specialization(guards = "isUndefined(limit)")
        JSArrayObject splitUndefinedLimit(JSDynamicObject rx, Object input, @SuppressWarnings("unused") Object limit,
                        @Cached @Shared InlinedBranchProfile limitZeroBranch,
                        @Cached @Shared SplitInternalNode splitInternal,
                        @Cached @Shared SplitAccordingToSpecNode splitAccordingToSpec) {
            return doSplit(rx, input, JSRuntime.MAX_SAFE_INTEGER_LONG,
                            this, limitZeroBranch, splitInternal, splitAccordingToSpec);
        }

        @Specialization(guards = "!isUndefined(limit)")
        JSArrayObject splitObjectLimit(JSDynamicObject rx, Object input, Object limit,
                        @Cached @Shared SplitAccordingToSpecNode splitAccordingToSpec) {
            checkObject(rx);
            TruffleString str = toString1(input);
            var constructor = getSpeciesConstructor(rx);
            return splitAccordingToSpec.execute(rx, str, limit, constructor, getContext(), this);
        }

        @Fallback
        static Object doNoObject(Object rx, @SuppressWarnings("unused") Object input, @SuppressWarnings("unused") Object flags) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@split", rx);
        }

        private JSArrayObject doSplit(JSDynamicObject rx, Object input, long limit,
                        Node node, InlinedBranchProfile limitZeroBranch, SplitInternalNode splitInternal, SplitAccordingToSpecNode splitAccordingToSpec) {
            checkObject(rx);
            TruffleString str = toString1(input);
            if (limit == 0) {
                limitZeroBranch.enter(node);
                return JSArray.createEmptyZeroLength(getContext(), getRealm());
            }
            var constructor = getSpeciesConstructor(rx);
            if (constructor == getRealm().getRegExpConstructor() && isPristine(rx)) {
                return splitInternal.execute((JSRegExpObject) rx, str, limit, getContext(), this);
            }
            return splitAccordingToSpec.execute(rx, str, limit, constructor, getContext(), this);
        }

        private void checkObject(JSDynamicObject rx) {
            if (!isJSObject(rx)) {
                throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@split", rx);
            }
        }

        private Object getSpeciesConstructor(JSDynamicObject rx) {
            JSDynamicObject regexpConstructor = getRealm().getRegExpConstructor();
            return getArraySpeciesConstructorNode().speciesConstructor(rx, regexpConstructor);
        }

        /**
         * Call RegExp (default or species) constructor.
         */
        static Object callRegExpConstructor(Object constructor, JSDynamicObject rx, TruffleString newFlags, JSFunctionCallNode constructorCall) {
            return constructorCall.executeCall(JSArguments.create(JSFunction.CONSTRUCT, constructor, rx, newFlags));
        }

        @ImportStatic({JSRegExp.class, Strings.class})
        protected abstract static class SplitAccordingToSpecNode extends JavaScriptBaseNode {

            protected abstract JSArrayObject execute(JSDynamicObject rx, TruffleString str, Object limit, Object constructor, JSContext context, JSRegExpSplitNode parent);

            @Specialization
            protected static JSArrayObject split(JSDynamicObject rx, TruffleString str, Object limit, Object constructor, JSContext context, JSRegExpSplitNode parent,
                            @Bind("this") Node node,
                            @Cached("create(FLAGS, context)") PropertyGetNode getFlags,
                            @Cached("create(LENGTH, context)") PropertyGetNode getLength,
                            @Cached JSToStringNode toString2,
                            @Cached JSToUInt32Node toUInt32,
                            @Cached TruffleString.CharIndexOfAnyCharUTF16Node indexOfNode,
                            @Cached EnsureStickyNode ensureSticky,
                            @Cached("createNew()") JSFunctionCallNode constructorCall,
                            @Cached JSToLengthNode toLength,
                            @Cached TruffleString.SubstringByteIndexNode substringNode,
                            @Cached AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached InlinedConditionProfile limitUndefined,
                            @Cached InlinedConditionProfile sizeIsZero,
                            @Cached InlinedConditionProfile resultIsNull,
                            @Cached InlinedConditionProfile sameMatchEnd,
                            @Cached InlinedConditionProfile isUnicode,
                            @Cached InlinedBranchProfile prematureReturnBranch) {
                TruffleString flags = toString2.executeString(getFlags.getValue(rx));
                boolean unicodeMatching = Strings.indexOfAny(indexOfNode, flags, 'u', 'v') >= 0;
                TruffleString newFlags = ensureSticky.execute(node, flags);
                Object splitter = callRegExpConstructor(constructor, rx, newFlags, constructorCall);
                JSArrayObject array = JSArray.createEmptyZeroLength(context, JSRealm.get(node));
                long lim;
                if (limitUndefined.profile(node, limit == Undefined.instance)) {
                    lim = JSRuntime.MAX_SAFE_INTEGER_LONG;
                } else {
                    // NOT ToLength()v https://github.com/tc39/ecma262/issues/92
                    lim = toUInt32.executeLong(limit);
                    if (lim == 0) {
                        prematureReturnBranch.enter(node);
                        return array;
                    }
                }
                int size = Strings.length(str);
                if (sizeIsZero.profile(node, size == 0)) {
                    if (parent.regexExecIntl(splitter, str) == Null.instance) {
                        parent.write(array, 0, str);
                    }
                    return array;
                }
                // limited by max array length and max string length
                int arrayLength = 0;
                int prevMatchEnd = 0;
                int fromIndex = 0;
                while (fromIndex < size) {
                    parent.setLastIndex(splitter, fromIndex);
                    JSDynamicObject regexResult = (JSDynamicObject) parent.regexExecIntl(splitter, str);
                    if (resultIsNull.profile(node, regexResult == Null.instance)) {
                        fromIndex = movePosition(str, unicodeMatching, fromIndex, node, isUnicode, advanceStringIndexUnicode);
                    } else {
                        long lastIndex = toLength.executeLong(parent.getLastIndex(splitter));
                        int matchEnd = (int) Math.min(lastIndex, size);
                        if (sameMatchEnd.profile(node, matchEnd == prevMatchEnd)) {
                            fromIndex = movePosition(str, unicodeMatching, fromIndex, node, isUnicode, advanceStringIndexUnicode);
                        } else {
                            TruffleString part = Strings.substring(context, substringNode, str, prevMatchEnd, fromIndex - prevMatchEnd);
                            parent.write(array, arrayLength, part);
                            arrayLength++;
                            if (arrayLength == lim) {
                                prematureReturnBranch.enter(node);
                                return array;
                            }
                            prevMatchEnd = matchEnd;
                            fromIndex = matchEnd;
                            long numberOfCaptures = toLength.executeLong(getLength.getValue(regexResult));
                            for (long i = 1; i < numberOfCaptures; i++) {
                                parent.write(array, arrayLength, parent.read(regexResult, i));
                                arrayLength++;
                                if (arrayLength == lim) {
                                    prematureReturnBranch.enter(node);
                                    return array;
                                }
                            }
                        }
                    }
                }
                TruffleString part = Strings.substring(context, substringNode, str, prevMatchEnd, size - prevMatchEnd);
                parent.write(array, arrayLength, part);
                return array;
            }
        }

        @ImportStatic(JSRegExp.class)
        protected abstract static class SplitInternalNode extends JavaScriptBaseNode {

            protected abstract JSArrayObject execute(JSRegExpObject rx, TruffleString str, long lim, JSContext context, JSRegExpSplitNode parent);

            @Specialization(guards = {"getCompiledRegex(rx) == tRegexCompiledRegex"}, limit = "1")
            protected static JSArrayObject doCached(JSRegExpObject rx, TruffleString str, long lim, JSContext context, JSRegExpSplitNode parent,
                            @Bind("this") Node node,
                            @Cached("getCompiledRegex(rx)") Object tRegexCompiledRegex,
                            @Cached(inline = true) @Shared InteropReadMemberNode readFlags,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readSticky,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicode,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicodeSets,
                            @Cached @Shared RemoveStickyFlagNode removeStickyFlag,
                            @Cached(inline = true) @Shared TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                            @Cached(inline = true) @Shared TRegexUtil.InteropReadIntMemberNode readGroupCount,
                            @Cached(inline = true) @Shared TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                            @Cached(inline = true) @Shared TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                            @Cached("createNew()") @Shared JSFunctionCallNode constructorCall,
                            @Cached("create(context, false)") @Shared JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndex,
                            @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                            @Cached @Shared AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached @Shared InlinedConditionProfile sizeIsZero,
                            @Cached @Shared InlinedConditionProfile resultIsNull,
                            @Cached @Shared InlinedConditionProfile isUnicode,
                            @Cached @Shared InlinedConditionProfile stickyFlagSet,
                            @Cached @Shared InlinedBranchProfile prematureReturnBranch) {
                Object tRegexFlags = TRegexCompiledRegexAccessor.flags(tRegexCompiledRegex, node, readFlags);
                boolean unicodeMatching = TRegexFlagsAccessor.unicode(tRegexFlags, node, readUnicode) || TRegexFlagsAccessor.unicodeSets(tRegexFlags, node, readUnicodeSets);
                JSRealm realm = JSRealm.get(node);
                JSRegExpObject splitter;
                if (stickyFlagSet.profile(node, TRegexFlagsAccessor.sticky(tRegexFlags, node, readSticky))) {
                    JSDynamicObject regexpConstructor = realm.getRegExpConstructor();
                    TruffleString newFlags = removeStickyFlag.execute(node, tRegexFlags);
                    splitter = (JSRegExpObject) callRegExpConstructor(regexpConstructor, rx, newFlags, constructorCall);
                } else {
                    splitter = rx;
                }
                JSArrayObject array = JSArray.createEmptyZeroLength(context, realm);
                int size = Strings.length(str);
                int arrayLength = 0;
                int prevMatchEnd = 0;
                int fromIndex = 0;
                int matchStart = -1;
                int matchEnd = -1;
                Object lastRegexResult = null;
                do {
                    Object tRegexResult = execIgnoreLastIndex.execute(splitter, str, fromIndex);
                    if (resultIsNull.profile(node, !TRegexResultAccessor.isMatch(tRegexResult, node, readIsMatch))) {
                        if (sizeIsZero.profile(node, size == 0) || matchStart < 0) {
                            parent.write(array, 0, str);
                            return array;
                        }
                        break;
                    } else {
                        if (!context.getRegExpStaticResultUnusedAssumption().isValid()) {
                            lastRegexResult = tRegexResult;
                        }
                        matchStart = TRegexResultAccessor.captureGroupStart(tRegexResult, 0, node, getStart);
                        matchEnd = TRegexResultAccessor.captureGroupEnd(tRegexResult, 0, node, getEnd);
                        if (matchEnd == prevMatchEnd) {
                            fromIndex = movePosition(str, unicodeMatching, fromIndex, node, isUnicode, advanceStringIndexUnicode);
                        } else {
                            TruffleString part = Strings.substring(context, substringNode, str, prevMatchEnd, matchStart - prevMatchEnd);
                            parent.write(array, arrayLength++, part);
                            if (arrayLength == lim) {
                                prematureReturnBranch.enter(node);
                                return array;
                            }
                            prevMatchEnd = matchEnd;
                            long numberOfCaptures = TRegexCompiledRegexAccessor.groupCount(tRegexCompiledRegex, node, readGroupCount);
                            for (int i = 1; i < numberOfCaptures; i++) {
                                parent.write(array, arrayLength, TRegexUtil.TRegexMaterializeResult.materializeGroup(context, tRegexResult, i, str,
                                                node, substringNode, getStart, getEnd));
                                arrayLength++;
                                if (arrayLength == lim) {
                                    prematureReturnBranch.enter(node);
                                    return array;
                                }
                            }
                            if (matchStart == matchEnd) {
                                fromIndex = movePosition(str, unicodeMatching, fromIndex, node, isUnicode, advanceStringIndexUnicode);
                            } else {
                                fromIndex = matchEnd;
                            }
                        }
                    }
                } while (fromIndex < size);
                if (context.isOptionRegexpStaticResult() && matchStart >= 0 && matchStart < size) {
                    realm.setStaticRegexResult(context, tRegexCompiledRegex, str, matchStart, lastRegexResult);
                }
                if (matchStart != matchEnd || prevMatchEnd < size) {
                    TruffleString part = Strings.substring(context, substringNode, str, prevMatchEnd, size - prevMatchEnd);
                    parent.write(array, arrayLength, part);
                }
                return array;
            }

            @Specialization(replaces = "doCached")
            protected static JSArrayObject doUncached(JSRegExpObject rx, TruffleString str, long lim, JSContext context, JSRegExpSplitNode parent,
                            @Bind("this") Node node,
                            @Cached(inline = true) @Shared InteropReadMemberNode readFlags,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readSticky,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicode,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicodeSets,
                            @Cached @Shared RemoveStickyFlagNode removeStickyFlag,
                            @Cached(inline = true) @Shared TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                            @Cached(inline = true) @Shared TRegexUtil.InteropReadIntMemberNode readGroupCount,
                            @Cached(inline = true) @Shared TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                            @Cached(inline = true) @Shared TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                            @Cached("createNew()") @Shared JSFunctionCallNode constructorCall,
                            @Cached("create(context, false)") @Shared JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndex,
                            @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                            @Cached @Shared AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached @Shared InlinedConditionProfile sizeIsZero,
                            @Cached @Shared InlinedConditionProfile resultIsNull,
                            @Cached @Shared InlinedConditionProfile isUnicode,
                            @Cached @Shared InlinedConditionProfile stickyFlagSet,
                            @Cached @Shared InlinedBranchProfile prematureReturnBranch) {
                return doCached(rx, str, lim, context, parent, node, JSRegExp.getCompiledRegex(rx),
                                readFlags, readSticky, readUnicode, readUnicodeSets, removeStickyFlag, readIsMatch, readGroupCount, getStart, getEnd, constructorCall, execIgnoreLastIndex,
                                substringNode, advanceStringIndexUnicode, sizeIsZero, resultIsNull, isUnicode, stickyFlagSet, prematureReturnBranch);
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class RemoveStickyFlagNode extends JavaScriptBaseNode {

            protected abstract TruffleString execute(Node node, Object tRegexFlags);

            @Specialization
            static TruffleString removeStickyFlag(Node node, Object tRegexFlags,
                            @Cached InteropReadBooleanMemberNode readGlobalNode,
                            @Cached InteropReadBooleanMemberNode readMultilineNode,
                            @Cached InteropReadBooleanMemberNode readIgnoreCaseNode,
                            @Cached InteropReadBooleanMemberNode readUnicodeNode,
                            @Cached InteropReadBooleanMemberNode readDotAllNode,
                            @Cached InteropReadBooleanMemberNode readHasIndicesNode) {
                char[] flags = new char[6];
                int len = 0;
                if (TRegexFlagsAccessor.hasIndices(tRegexFlags, node, readHasIndicesNode)) {
                    flags[len++] = 'd';
                }
                if (TRegexFlagsAccessor.global(tRegexFlags, node, readGlobalNode)) {
                    flags[len++] = 'g';
                }
                if (TRegexFlagsAccessor.ignoreCase(tRegexFlags, node, readIgnoreCaseNode)) {
                    flags[len++] = 'i';
                }
                if (TRegexFlagsAccessor.multiline(tRegexFlags, node, readMultilineNode)) {
                    flags[len++] = 'm';
                }
                if (TRegexFlagsAccessor.dotAll(tRegexFlags, node, readDotAllNode)) {
                    flags[len++] = 's';
                }
                if (TRegexFlagsAccessor.unicode(tRegexFlags, node, readUnicodeNode)) {
                    flags[len++] = 'u';
                }
                if (TRegexFlagsAccessor.unicodeSets(tRegexFlags, node, readUnicodeNode)) {
                    flags[len++] = 'v';
                }
                if (len == 0) {
                    return Strings.EMPTY_STRING;
                }
                return Strings.fromCharArray(flags, 0, len);
            }
        }

        /**
         * Ensure sticky ("y") is part of the flags.
         */
        @GenerateInline
        @GenerateCached(false)
        protected abstract static class EnsureStickyNode extends JavaScriptBaseNode {

            protected abstract TruffleString execute(Node node, Object flags);

            @Specialization
            static TruffleString ensureSticky(Node node, TruffleString flags,
                            @Cached InlinedConditionProfile emptyFlags,
                            @Cached InlinedConditionProfile stickyFlagSet,
                            @Cached(inline = false) TruffleString.ByteIndexOfCodePointNode indexOfNode,
                            @Cached(inline = false) TruffleString.ConcatNode concatNode) {
                if (emptyFlags.profile(node, Strings.length(flags) == 0)) {
                    return Strings.Y;
                } else if (stickyFlagSet.profile(node, Strings.indexOf(indexOfNode, flags, 'y') >= 0)) {
                    return flags;
                } else {
                    return Strings.concat(concatNode, flags, Strings.Y);
                }
            }
        }

        private boolean isJSObject(JSDynamicObject rx) {
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

        private boolean isPristine(JSDynamicObject rx) {
            if (isPristineObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(getContext()));
            }
            return isPristineObjectNode.execute(rx);
        }

        static int movePosition(TruffleString s, boolean unicodeMatching, int lastIndex,
                        Node node, InlinedConditionProfile isUnicode, AdvanceStringIndexUnicodeNode advanceIndex) {
            return isUnicode.profile(node, unicodeMatching) ? advanceIndex.execute(node, s, lastIndex) : lastIndex + 1;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@replace] method.
     */
    public abstract static class JSRegExpReplaceNode extends RegExpPrototypeSymbolOperation implements ReplaceStringConsumerTRegex.ParentNode {

        @Child private JSToStringNode toString2Node;
        @Child private JSToStringNode toString3Node;
        @Child private JSToStringNode toString4Node;
        @Child private IsJSObjectNode isObjectNode;
        @Child private IsCallableNode isCallableNode;
        @Child private ReadElementNode readNamedCaptureGroupNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private IsPristineObjectNode isPristineObjectNode;

        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.AppendSubstringByteIndexNode appendSubStringNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;

        final StringBuilderProfile stringBuilderProfile;
        final BranchProfile invalidGroupNumberProfile = BranchProfile.create();

        @Child private InvokeGetGroupBoundariesMethodNode getStartNode = InvokeGetGroupBoundariesMethodNode.create();
        @Child private InvokeGetGroupBoundariesMethodNode getEndNode = InvokeGetGroupBoundariesMethodNode.create();
        @Child private InteropReadMemberNode readGroupsNode = InteropReadMemberNode.create();
        @Child private InteropLibrary namedCaptureGroupInterop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child private InteropLibrary groupIndicesInterop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child private InteropToIntNode toIntNode = InteropToIntNodeGen.create();

        JSRegExpReplaceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isObjectNode = IsJSObjectNode.create();
            this.isCallableNode = IsCallableNode.create();
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)", limit = "1")
        protected Object replaceStringCached(JSDynamicObject rx, Object searchValue, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValueWithNCG(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValueWithNamedCG,
                        @Cached(value = "parseReplaceValueWithoutNCG(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValueWithoutNamedCG,
                        @Shared @Cached JSToStringNode toString1Node,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode,
                        @Cached @Shared ReplaceInternalNode replaceInternal,
                        @Cached @Shared ReplaceAccordingToSpecNode replaceAccordingToSpec) {
            checkObject(rx);
            TruffleString searchString = toString1Node.executeString(searchValue);
            if (isPristine(rx)) {
                return replaceInternal.execute((JSRegExpObject) rx, searchString, cachedReplaceValue, cachedParsedReplaceValueWithNamedCG, cachedParsedReplaceValueWithoutNamedCG, getContext(), this);
            }
            return replaceAccordingToSpec.execute(rx, searchString, cachedReplaceValue, false, getContext(), this);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(JSDynamicObject rx, Object searchValue, TruffleString replaceValue,
                        @Shared @Cached JSToStringNode toString1Node,
                        @Cached @Shared ReplaceInternalNode replaceInternal,
                        @Cached @Shared ReplaceAccordingToSpecNode replaceAccordingToSpec) {
            checkObject(rx);
            TruffleString searchString = toString1Node.executeString(searchValue);
            if (isPristine(rx)) {
                return replaceInternal.execute((JSRegExpObject) rx, searchString, replaceValue, null, null, getContext(), this);
            }
            return replaceAccordingToSpec.execute(rx, searchString, replaceValue, false, getContext(), this);
        }

        @Specialization(replaces = "replaceString")
        protected Object replaceDynamic(JSDynamicObject rx, Object searchValue, Object replaceValue,
                        @Shared @Cached JSToStringNode toString1Node,
                        @Cached @Shared ReplaceInternalNode replaceInternal,
                        @Cached @Shared ReplaceAccordingToSpecNode replaceAccordingToSpec,
                        @Cached InlinedConditionProfile functionalReplaceProfile) {
            checkObject(rx);
            TruffleString searchString = toString1Node.executeString(searchValue);
            boolean functionalReplace = functionalReplaceProfile.profile(this, isCallableNode.executeBoolean(replaceValue));
            Object replaceVal;
            if (functionalReplace) {
                replaceVal = replaceValue;
            } else {
                TruffleString replaceString = toString2(replaceValue);
                replaceVal = replaceString;
                if (isPristine(rx)) {
                    return replaceInternal.execute((JSRegExpObject) rx, searchString, replaceString, null, null, getContext(), this);
                }
            }
            return replaceAccordingToSpec.execute(rx, searchString, replaceVal, functionalReplace, getContext(), this);
        }

        @Fallback
        protected Object doNoObject(Object rx, @SuppressWarnings("unused") Object searchString, @SuppressWarnings("unused") Object replaceValue) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@replace", rx);
        }

        private void checkObject(JSDynamicObject rx) {
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

        @ImportStatic(JSRegExp.class)
        protected abstract static class ReplaceInternalNode extends JavaScriptBaseNode {

            protected abstract TruffleString execute(JSRegExpObject rx, TruffleString s, TruffleString replaceString,
                            ReplaceStringParser.Token[] parsedWithNamedCG, ReplaceStringParser.Token[] parsedWithoutNamedCG,
                            JSContext context, JSRegExpReplaceNode parent);

            @Specialization(guards = {"getCompiledRegex(rx) == tRegexCompiledRegex"}, limit = "1")
            protected static TruffleString doCached(JSRegExpObject rx, TruffleString s, TruffleString replaceString,
                            ReplaceStringParser.Token[] parsedWithNamedCG, ReplaceStringParser.Token[] parsedWithoutNamedCG,
                            JSContext context, JSRegExpReplaceNode parent,
                            @Bind("this") Node node,
                            @Cached("getCompiledRegex(rx)") Object tRegexCompiledRegex,
                            @Cached("create(context, false)") @Shared JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndexNode,
                            @Cached @Shared AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached @Shared JSToLengthNode toLength,
                            @Cached @Shared InlinedConditionProfile unicodeProfile,
                            @Cached @Shared InlinedConditionProfile globalProfile,
                            @Cached @Shared InlinedConditionProfile stickyProfile,
                            @Cached @Shared InlinedConditionProfile noMatchProfile,
                            @Cached @Shared InlinedConditionProfile hasNamedCaptureGroupsProfile,
                            @Cached @Shared InlinedBranchProfile dollarProfile,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readGlobal,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readSticky,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicode,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicodeSets,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readIsMatch,
                            @Cached(inline = true) @Shared InvokeGetGroupBoundariesMethodNode getStart,
                            @Cached(inline = true) @Shared InvokeGetGroupBoundariesMethodNode getEnd,
                            @Cached(inline = true) @Shared InteropReadMemberNode readFlags,
                            @Cached(inline = true) @Shared InteropReadMemberNode readGroups,
                            @Cached(inline = true) @Shared InteropReadIntMemberNode readGroupCount) {
                Object tRegexFlags = TRegexCompiledRegexAccessor.flags(tRegexCompiledRegex, node, readFlags);
                boolean global = globalProfile.profile(node, TRegexFlagsAccessor.global(tRegexFlags, node, readGlobal));
                boolean eitherUnicode = unicodeProfile.profile(node,
                                TRegexFlagsAccessor.unicode(tRegexFlags, node, readUnicode) || TRegexFlagsAccessor.unicodeSets(tRegexFlags, node, readUnicodeSets));
                boolean sticky = stickyProfile.profile(node, TRegexFlagsAccessor.sticky(tRegexFlags, node, readSticky));
                int length = Strings.length(s);
                var sb = parent.stringBuilderProfile.newStringBuilder(length + 16);
                int lastMatchEnd = 0;
                int matchStart = -1;
                long lastIndex = sticky ? toLength.executeLong(parent.getLastIndex(rx)) : 0;
                Object lastRegexResult = null;
                while (lastIndex <= length) {
                    Object tRegexResult = execIgnoreLastIndexNode.execute(rx, s, lastIndex);
                    if (noMatchProfile.profile(node, !TRegexResultAccessor.isMatch(tRegexResult, node, readIsMatch))) {
                        if (matchStart < 0) {
                            if (global || sticky) {
                                parent.setLastIndex(rx, 0);
                            }
                            return s;
                        }
                        break;
                    }
                    if (!context.getRegExpStaticResultUnusedAssumption().isValid()) {
                        lastRegexResult = tRegexResult;
                    }
                    matchStart = TRegexResultAccessor.captureGroupStart(tRegexResult, 0, node, getStart);
                    int matchEnd = TRegexResultAccessor.captureGroupEnd(tRegexResult, 0, node, getEnd);
                    assert matchStart >= 0 && matchStart <= length && matchStart >= lastMatchEnd;
                    parent.append(sb, s, lastMatchEnd, matchStart);
                    Object namedCG = TRegexCompiledRegexAccessor.namedCaptureGroups(tRegexCompiledRegex, node, readGroups);
                    boolean hasNamedCG = hasNamedCaptureGroupsProfile.profile(node, !parent.namedCaptureGroupInterop.isNull(namedCG));
                    int groupCount = TRegexCompiledRegexAccessor.groupCount(tRegexCompiledRegex, node, readGroupCount);
                    if (parsedWithNamedCG == null) {
                        ReplaceStringParser.process(context, replaceString, groupCount, hasNamedCG,
                                        new ReplaceStringConsumerTRegex(sb, s, replaceString, matchStart, matchEnd, tRegexResult, tRegexCompiledRegex, groupCount),
                                        parent, node, dollarProfile);
                    } else {
                        ReplaceStringParser.processParsed(hasNamedCG ? parsedWithNamedCG : parsedWithoutNamedCG,
                                        new ReplaceStringConsumerTRegex(sb, s, replaceString, matchStart, matchEnd, tRegexResult, tRegexCompiledRegex, groupCount), parent);
                    }
                    lastMatchEnd = matchEnd;
                    if (global) {
                        if (matchStart == matchEnd) {
                            lastIndex = eitherUnicode ? advanceStringIndexUnicode.execute(node, s, matchEnd) : matchEnd + 1;
                        } else {
                            lastIndex = matchEnd;
                        }
                    } else {
                        break;
                    }
                }
                if (context.isOptionRegexpStaticResult() && matchStart >= 0) {
                    JSRealm.get(node).setStaticRegexResult(context, tRegexCompiledRegex, s, matchStart, lastRegexResult);
                }
                if (global || sticky) {
                    parent.setLastIndex(rx, sticky ? lastMatchEnd : 0);
                }
                if (lastMatchEnd < length) {
                    parent.append(sb, s, lastMatchEnd, length);
                }
                return parent.builderToString(sb);
            }

            @Specialization(replaces = "doCached")
            protected static TruffleString doUncached(JSRegExpObject rx, TruffleString s, TruffleString replaceString,
                            ReplaceStringParser.Token[] parsedWithNamedCG, ReplaceStringParser.Token[] parsedWithoutNamedCG,
                            JSContext context, JSRegExpReplaceNode parent,
                            @Bind("this") Node node,
                            @Cached("create(context, false)") @Shared JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode execIgnoreLastIndexNode,
                            @Cached @Shared AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached @Shared JSToLengthNode toLength,
                            @Cached @Shared InlinedConditionProfile unicodeProfile,
                            @Cached @Shared InlinedConditionProfile globalProfile,
                            @Cached @Shared InlinedConditionProfile stickyProfile,
                            @Cached @Shared InlinedConditionProfile noMatchProfile,
                            @Cached @Shared InlinedConditionProfile hasNamedCaptureGroupsProfile,
                            @Cached @Shared InlinedBranchProfile dollarProfile,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readGlobal,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readSticky,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicode,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readUnicodeSets,
                            @Cached(inline = true) @Shared InteropReadBooleanMemberNode readIsMatch,
                            @Cached(inline = true) @Shared InvokeGetGroupBoundariesMethodNode getStart,
                            @Cached(inline = true) @Shared InvokeGetGroupBoundariesMethodNode getEnd,
                            @Cached(inline = true) @Shared InteropReadMemberNode readFlags,
                            @Cached(inline = true) @Shared InteropReadMemberNode readGroups,
                            @Cached(inline = true) @Shared InteropReadIntMemberNode readGroupCount) {
                return doCached(rx, s, replaceString, parsedWithNamedCG, parsedWithoutNamedCG, context, parent, node, JSRegExp.getCompiledRegex(rx),
                                execIgnoreLastIndexNode, advanceStringIndexUnicode, toLength, unicodeProfile, globalProfile, stickyProfile, noMatchProfile, hasNamedCaptureGroupsProfile, dollarProfile,
                                readGlobal, readSticky, readUnicode, readUnicodeSets, readIsMatch, getStart, getEnd, readFlags, readGroups, readGroupCount);
            }
        }

        @ImportStatic({JSRegExp.class, JSConfig.class, JSAbstractArray.class})
        protected abstract static class ReplaceAccordingToSpecNode extends JavaScriptBaseNode {

            protected abstract TruffleString execute(JSDynamicObject rx, TruffleString s, Object replaceValue, boolean functionalReplace,
                            JSContext context, JSRegExpReplaceNode parent);

            @Specialization
            protected static TruffleString replaceAccordingToSpec(JSDynamicObject rx, TruffleString s, Object replaceValue, boolean functionalReplace,
                            JSContext context, JSRegExpReplaceNode parent,
                            @Bind("this") Node node,
                            @Cached AdvanceStringIndexUnicodeNode advanceStringIndexUnicode,
                            @Cached JSToLengthNode toLength,
                            @Cached JSToIntegerAsIntNode toIntegerNode,
                            @Cached JSToStringNode toStringForFlagsNode,
                            @Cached TruffleString.ByteIndexOfCodePointNode indexOfNode,
                            @Cached TruffleString.CharIndexOfAnyCharUTF16Node indexOfAnyNode,
                            @Cached("create(LENGTH, context)") PropertyGetNode getLength,
                            @Cached("create(INDEX, context)") PropertyGetNode getIndexNode,
                            @Cached("create(GROUPS, context)") PropertyGetNode getGroupsNode,
                            @Cached("create(FLAGS, context)") PropertyGetNode getFlagsNode,
                            @CachedLibrary(limit = "InteropLibraryLimit") DynamicObjectLibrary getLazyRegexResult,
                            @Cached("create(LAZY_REGEX_RESULT_ID)") HasHiddenKeyCacheNode hasLazyRegexResult,
                            @Cached("createCall()") JSFunctionCallNode callFunction,
                            @Cached InlinedBranchProfile growProfile,
                            @Cached InlinedConditionProfile unicodeProfile,
                            @Cached InlinedConditionProfile globalProfile,
                            @Cached InlinedConditionProfile noMatchProfile,
                            @Cached InlinedConditionProfile lazyResultArrayProfile,
                            @Cached InlinedConditionProfile validPositionProfile,
                            @Cached InlinedBranchProfile dollarProfile) {
                TruffleString replaceString = null;
                JSDynamicObject replaceFunction = null;
                if (functionalReplace) {
                    replaceFunction = (JSDynamicObject) replaceValue;
                } else {
                    replaceString = (TruffleString) replaceValue;
                }
                TruffleString flags = toStringForFlagsNode.executeString(getFlagsNode.getValue(rx));
                boolean global = globalProfile.profile(node, Strings.indexOf(indexOfNode, flags, 'g') != -1);
                boolean fullUnicode = false;
                if (global) {
                    fullUnicode = unicodeProfile.profile(node, Strings.indexOfAny(indexOfAnyNode, flags, 'u', 'v') != -1);
                    parent.setLastIndex(rx, 0);
                }
                SimpleArrayList<JSDynamicObject> results = null;
                if (functionalReplace) {
                    results = new SimpleArrayList<>();
                }
                int length = Strings.length(s);
                var sb = parent.stringBuilderProfile.newStringBuilder(length + 16);
                int nextSourcePosition = 0;
                int matchLength = -1;
                while (true) {
                    JSDynamicObject result = (JSDynamicObject) parent.regexExecIntl(rx, s);
                    if (noMatchProfile.profile(node, result == Null.instance)) {
                        if (matchLength < 0) {
                            return s;
                        }
                        break;
                    }
                    if (lazyResultArrayProfile.profile(node, isLazyResultArray(result, hasLazyRegexResult))) {
                        matchLength = getLazyLength(result, getLazyRegexResult, parent);
                    } else {
                        matchLength = processNonLazy(result, toLength, getLength, parent);
                    }
                    if (functionalReplace) {
                        results.add(result, node, growProfile);
                    } else {
                        int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), Strings.length(s)), 0);
                        if (validPositionProfile.profile(node, position >= nextSourcePosition)) {
                            parent.append(sb, s, nextSourcePosition, position);
                            Object namedCaptures = getGroupsNode.getValue(result);
                            if (namedCaptures != Undefined.instance) {
                                namedCaptures = parent.toObject(namedCaptures);
                            }
                            ReplaceStringParser.process(context, replaceString, (int) toLength.executeLong(getLength.getValue(result)), namedCaptures != Undefined.instance,
                                            new ReplaceStringConsumer(sb, s, replaceString, position, Math.min(position + matchLength, Strings.length(s)), result, (JSDynamicObject) namedCaptures),
                                            parent, node, dollarProfile);
                            nextSourcePosition = position + matchLength;
                        }
                    }
                    if (global) {
                        if (matchLength == 0) {
                            long lastI = toLength.executeLong(parent.getLastIndex(rx));
                            long nextIndex = lastI + 1;
                            if (JSRuntime.longIsRepresentableAsInt(nextIndex)) {
                                parent.setLastIndex(rx, fullUnicode ? advanceStringIndexUnicode.execute(node, s, (int) lastI) : (int) nextIndex);
                            } else {
                                parent.setLastIndex(rx, (double) nextIndex);
                            }
                        }
                    } else {
                        break;
                    }
                }
                if (functionalReplace) {
                    for (int i = 0; i < results.size(); i++) {
                        JSDynamicObject result = results.get(i);
                        int position = Math.max(Math.min(toIntegerNode.executeInt(getIndexNode.getValue(result)), Strings.length(s)), 0);
                        int resultsLength = (int) toLength.executeLong(getLength.getValue(result));
                        Object namedCaptures = getGroupsNode.getValue(result);
                        boolean hasNamedCG = namedCaptures != Undefined.instance;
                        Object[] arguments = JSArguments.createInitial(Undefined.instance, replaceFunction, resultsLength + 2 + (hasNamedCG ? 1 : 0));
                        for (int i1 = 0; i1 < resultsLength; i1++) {
                            JSArguments.setUserArgument(arguments, i1, parent.read(result, i1));
                        }
                        JSArguments.setUserArgument(arguments, resultsLength, position);
                        JSArguments.setUserArgument(arguments, resultsLength + 1, s);
                        if (hasNamedCG) {
                            JSArguments.setUserArgument(arguments, resultsLength + 2, namedCaptures);
                        }
                        Object callResult = callFunction.executeCall(arguments);
                        TruffleString replacement = parent.toString2(callResult);
                        if (validPositionProfile.profile(node, position >= nextSourcePosition)) {
                            parent.append(sb, s, nextSourcePosition, position);
                            parent.append(sb, replacement);
                            if (lazyResultArrayProfile.profile(node, isLazyResultArray(result, hasLazyRegexResult))) {
                                nextSourcePosition = position + getLazyLength(result, getLazyRegexResult, parent);
                            } else {
                                nextSourcePosition = position + Strings.length((TruffleString) parent.read(result, 0));
                            }
                        }
                    }
                }
                if (nextSourcePosition < length) {
                    parent.append(sb, s, nextSourcePosition, length);
                }
                return parent.builderToString(sb);
            }

            private static boolean isLazyResultArray(JSDynamicObject result, HasHiddenKeyCacheNode hasLazyRegexResultNode) {
                boolean isLazyResultArray = hasLazyRegexResultNode.executeHasHiddenKey(result);
                assert isLazyResultArray == JSDynamicObject.hasProperty(result, JSArray.LAZY_REGEX_RESULT_ID);
                return isLazyResultArray;
            }

            private static int getLazyLength(JSDynamicObject obj, DynamicObjectLibrary lazyRegexResultNode, JSRegExpReplaceNode parent) {
                Object regexResult = JSAbstractArray.arrayGetRegexResult(obj, lazyRegexResultNode);
                return TRegexResultAccessor.captureGroupLength(regexResult, 0, null, parent.getStartNode, parent.getEndNode);
            }

            private static int processNonLazy(JSDynamicObject result, JSToLengthNode toLength, PropertyGetNode getLength, JSRegExpReplaceNode parent) {
                long resultLength = toLength.executeLong(getLength.getValue(result));
                TruffleString result0Str = parent.toString3(parent.read(result, 0));
                parent.write(result, 0, result0Str);
                for (long n = 1; n < resultLength; n++) {
                    Object value = parent.read(result, n);
                    if (value != Undefined.instance) {
                        parent.write(result, n, parent.toString3(value));
                    }
                }
                return Strings.length(result0Str);
            }
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

        private boolean isPristine(JSDynamicObject rx) {
            if (isPristineObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(getContext()));
            }
            return isPristineObjectNode.execute(rx);
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
                toObjectNode = insert(JSToObjectNode.create());
            }
            return toObjectNode.execute(obj);
        }

        @Override
        public void append(TruffleStringBuilderUTF16 sb, TruffleString s) {
            if (appendStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendStringNode = insert(TruffleStringBuilder.AppendStringNode.create());
            }
            stringBuilderProfile.append(appendStringNode, sb, s);
        }

        @Override
        public void append(TruffleStringBuilderUTF16 sb, TruffleString s, int fromIndex, int toIndex) {
            if (appendSubStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendSubStringNode = insert(TruffleStringBuilder.AppendSubstringByteIndexNode.create());
            }
            stringBuilderProfile.append(appendSubStringNode, sb, s, fromIndex, toIndex);
        }

        private TruffleString builderToString(TruffleStringBuilderUTF16 sb) {
            if (builderToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                builderToStringNode = insert(TruffleStringBuilder.ToStringNode.create());
            }
            return StringBuilderProfile.toString(builderToStringNode, sb);
        }

        @Override
        public BranchProfile getInvalidGroupNumberProfile() {
            return invalidGroupNumberProfile;
        }

        @Override
        public InvokeGetGroupBoundariesMethodNode getGetStartNode() {
            return getStartNode;
        }

        @Override
        public InvokeGetGroupBoundariesMethodNode getGetEndNode() {
            return getEndNode;
        }
    }

    public static final class ReplaceStringConsumer implements ReplaceStringParser.Consumer<JSRegExpReplaceNode, TruffleStringBuilderUTF16> {

        private final TruffleStringBuilderUTF16 sb;
        private final TruffleString input;
        private final TruffleString replaceStr;
        private final int startPos;
        private final int endPos;
        private final JSDynamicObject result;
        private final JSDynamicObject namedCaptures;

        private ReplaceStringConsumer(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString replaceStr, int startPos, int endPos, JSDynamicObject result, JSDynamicObject namedCaptures) {
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
        public TruffleStringBuilderUTF16 getResult() {
            return sb;
        }
    }

    public static final class ReplaceStringConsumerTRegex implements ReplaceStringParser.Consumer<ReplaceStringConsumerTRegex.ParentNode, TruffleStringBuilderUTF16> {

        public interface ParentNode {

            void append(TruffleStringBuilderUTF16 sb, TruffleString s);

            void append(TruffleStringBuilderUTF16 sb, TruffleString s, int fromIndex, int toIndex);

            BranchProfile getInvalidGroupNumberProfile();

            InvokeGetGroupBoundariesMethodNode getGetStartNode();

            InvokeGetGroupBoundariesMethodNode getGetEndNode();
        }

        private final TruffleStringBuilderUTF16 sb;
        private final TruffleString input;
        private final TruffleString replaceStr;
        private final int startPos;
        private final int endPos;
        private final Object tRegexResult;
        private final Object tRegexCompiledRegex;
        private final int groupCount;

        public ReplaceStringConsumerTRegex(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString replaceStr, int startPos, int endPos, Object tRegexResult, Object tRegexCompiledRegex,
                        int groupCount) {
            this.sb = sb;
            this.input = input;
            this.replaceStr = replaceStr;
            this.startPos = startPos;
            this.endPos = endPos;
            this.tRegexResult = tRegexResult;
            this.tRegexCompiledRegex = tRegexCompiledRegex;
            this.groupCount = groupCount;
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
        public void captureGroup(ParentNode parent, int groupNumber, int literalStart, int literalEnd) {
            Node node = (Node) parent;
            if (groupNumber < groupCount) {
                int start = TRegexResultAccessor.captureGroupStart(tRegexResult, groupNumber, node, parent.getGetStartNode());
                if (start >= 0) {
                    parent.append(sb, input, start, TRegexResultAccessor.captureGroupEnd(tRegexResult, groupNumber, node, parent.getGetEndNode()));
                }
            } else if (groupNumber > 9 && groupNumber / 10 < groupCount) {
                int start = TRegexResultAccessor.captureGroupStart(tRegexResult, groupNumber / 10, node, parent.getGetStartNode());
                if (start >= 0) {
                    parent.append(sb, input, start, TRegexResultAccessor.captureGroupEnd(tRegexResult, groupNumber / 10, node, parent.getGetEndNode()));
                }
                parent.append(sb, replaceStr, literalStart + 2, literalEnd);
            } else {
                parent.getInvalidGroupNumberProfile().enter();
                parent.append(sb, replaceStr, literalStart, literalEnd);
            }
        }

        @Override
        public void namedCaptureGroup(ParentNode node, TruffleString groupName) {
            JSRegExpReplaceNode parent = (JSRegExpReplaceNode) node;
            Object map = TRegexCompiledRegexAccessor.namedCaptureGroups(tRegexCompiledRegex, parent, parent.readGroupsNode);
            if (TRegexNamedCaptureGroupsAccessor.hasGroup(map, groupName, parent.namedCaptureGroupInterop)) {
                int[] groupNumbers = TRegexNamedCaptureGroupsAccessor.getGroupNumbers(map, groupName, parent.namedCaptureGroupInterop, parent.groupIndicesInterop, parent.toIntNode, parent);
                for (int groupNumber : groupNumbers) {
                    int start = TRegexResultAccessor.captureGroupStart(tRegexResult, groupNumber, parent, parent.getStartNode);
                    if (start >= 0) {
                        node.append(sb, input, start, TRegexResultAccessor.captureGroupEnd(tRegexResult, groupNumber, parent, parent.getEndNode));
                        break;
                    }
                }
            }
        }

        @Override
        public TruffleStringBuilderUTF16 getResult() {
            return sb;
        }
    }

    /**
     * This implements the RegExp.prototype.[@@match] method.
     */
    @ImportStatic(JSRegExp.class)
    public abstract static class JSRegExpMatchNode extends RegExpPrototypeSymbolOperation {

        protected JSRegExpMatchNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isObjectNode.executeBoolean(rx)", limit = "1")
        protected Object match(JSDynamicObject rx, Object param,
                        @Bind("this") Node node,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached("create(FLAGS, getContext())") PropertyGetNode getFlagsNode,
                        @Cached JSToStringNode toStringNodeForFlags,
                        @Cached TruffleString.ByteIndexOfCodePointNode stringIndexOfNode,
                        @Cached TruffleString.CharIndexOfAnyCharUTF16Node stringIndexOfAnyNode,
                        @Cached JSToStringNode toString1Node,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToLengthNode toLengthNode,
                        @Cached InlinedConditionProfile isGlobal,
                        @Cached InlinedConditionProfile isUnicode,
                        @Cached AdvanceStringIndexUnicodeNode advanceStringIndexUnicode) {
            TruffleString s = toString1Node.executeString(param);
            TruffleString flags = toStringNodeForFlags.executeString(getFlagsNode.getValue(rx));
            if (isGlobal.profile(node, Strings.indexOf(stringIndexOfNode, flags, 'g') == -1)) {
                return regexExecIntl(rx, s);
            } else {
                boolean fullUnicode = (Strings.indexOfAny(stringIndexOfAnyNode, flags, 'u', 'v') != -1);
                setLastIndex(rx, 0);
                JSDynamicObject array = JSArray.createEmptyZeroLength(getContext(), getRealm());
                int n = 0;
                JSDynamicObject result;
                TruffleString matchStr;
                while (true) {
                    result = (JSDynamicObject) regexExecIntl(rx, s);
                    if (result == Null.instance) {
                        return n == 0 ? Null.instance : array;
                    }
                    matchStr = toString2Node.executeString(read(result, 0));
                    write(array, n, matchStr);
                    if (Strings.length(matchStr) == 0) {
                        long lastIndex = toLengthNode.executeLong(getLastIndex(rx));
                        long nextIndex = lastIndex + 1;
                        if (JSRuntime.longIsRepresentableAsInt(nextIndex)) {
                            setLastIndex(rx, isUnicode.profile(node, fullUnicode) ? advanceStringIndexUnicode.execute(node, s, (int) lastIndex) : (int) nextIndex);
                        } else {
                            setLastIndex(rx, (double) nextIndex);
                        }
                    }
                    n++;
                }
            }
        }

        @Fallback
        protected static Object match(Object thisObj, @SuppressWarnings("unused") Object string) {
            throw Errors.createTypeErrorIncompatibleReceiver("RegExp.prototype.@@match", thisObj);
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
        protected Object search(JSDynamicObject rx, Object param,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached JSToStringNode toString1Node) {
            TruffleString s = toString1Node.executeString(param);
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

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isObjectNode.executeBoolean(regex)", limit = "1")
        protected final Object matchAll(JSDynamicObject regex, Object stringObj,
                        @Bind("this") Node node,
                        @Cached JSToStringNode toStringNodeForInput,
                        @Cached("create(getContext(), false)") ArraySpeciesConstructorNode speciesConstructNode,
                        @Cached("create(FLAGS, getContext())") PropertyGetNode getFlagsNode,
                        @Cached JSToStringNode toStringNodeForFlags,
                        @Cached("create(LAST_INDEX, getContext())") PropertyGetNode getLastIndexNode,
                        @Cached JSToLengthNode toLengthNode,
                        @Cached("create(LAST_INDEX, false, getContext(), true)") PropertySetNode setLastIndexNode,
                        @Cached("createCreateRegExpStringIteratorNode()") StringPrototypeBuiltins.CreateRegExpStringIteratorNode createRegExpStringIteratorNode,
                        @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached(inline = true) LongToIntOrDoubleNode indexToNumber,
                        @Cached TruffleString.ByteIndexOfCodePointNode stringIndexOfNode,
                        @Cached TruffleString.CharIndexOfAnyCharUTF16Node stringIndexOfAnyNode) {
            Object string = toStringNodeForInput.executeString(stringObj);
            JSDynamicObject regExpConstructor = getRealm().getRegExpConstructor();
            Object constructor = speciesConstructNode.speciesConstructor(regex, regExpConstructor);
            TruffleString flags = toStringNodeForFlags.executeString(getFlagsNode.getValue(regex));
            Object matcher = speciesConstructNode.construct(constructor, regex, flags);
            long lastIndex = toLengthNode.executeLong(getLastIndexNode.getValue(regex));
            setLastIndexNode.setValue(matcher, indexToNumber.fromIndex(node, lastIndex));
            boolean global = Strings.indexOf(stringIndexOfNode, flags, 'g') != -1;
            boolean fullUnicode = Strings.indexOfAny(stringIndexOfAnyNode, flags, 'u', 'v') != -1;
            return createRegExpStringIteratorNode.createIterator(matcher, string, global, fullUnicode);
        }

        @NeverDefault
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
            sticky(0),      // ES 2015
            unicode(0),     // ES 2015
            dotAll(0),      // ES 2018
            hasIndices(0),  // ES 2022
            unicodeSets(0); // Stage 3 proposal

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
        @Child private PropertyGetNode getUnicodeSets;
        @Child private JSToBooleanNode toBoolean;

        public RegExpFlagsGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getGlobal = PropertyGetNode.create(JSRegExp.GLOBAL, context);
            this.getIgnoreCase = PropertyGetNode.create(JSRegExp.IGNORE_CASE, context);
            this.getMultiline = PropertyGetNode.create(JSRegExp.MULTILINE, context);
            if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2018) {
                this.getDotAll = PropertyGetNode.create(JSRegExp.DOT_ALL, context);
            }
            this.getUnicode = PropertyGetNode.create(JSRegExp.UNICODE, context);
            this.getSticky = PropertyGetNode.create(JSRegExp.STICKY, context);
            if (context.isOptionRegexpMatchIndices()) {
                this.getHasIndices = PropertyGetNode.create(JSRegExp.HAS_INDICES, context);
            }
            if (context.isOptionRegexpUnicodeSets()) {
                this.getUnicodeSets = PropertyGetNode.create(JSRegExp.UNICODE_SETS, context);
            }
        }

        @Specialization(guards = "isObjectNode.executeBoolean(rx)", limit = "1")
        protected Object doObject(JSDynamicObject rx,
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
            if (getUnicodeSets != null && getFlag(rx, getUnicodeSets)) {
                flags[len++] = 'v';
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

        private boolean getFlag(JSDynamicObject re, PropertyGetNode getNode) {
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

        private final String flagName;

        CompiledRegexFlagPropertyAccessor(JSContext context, JSBuiltin builtin, String flagName) {
            super(context, builtin);
            this.flagName = flagName;
        }

        @Specialization
        Object doRegExp(JSRegExpObject obj,
                        @Cached TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode readFlag) {
            return readFlag.execute(this, JSRegExp.getCompiledRegex(obj), flagName);
        }

        @Specialization(guards = "isRegExpPrototype(obj)")
        Object doPrototype(@SuppressWarnings("unused") JSDynamicObject obj) {
            return Undefined.instance;
        }

        @Fallback
        public Object doObject(Object obj) {
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }

        boolean isRegExpPrototype(JSDynamicObject obj) {
            return obj == getRealm().getRegExpPrototype();
        }

        public static CompiledRegexFlagPropertyAccessor create(JSContext context, JSBuiltin builtin, String flagName, JavaScriptNode[] args) {
            return RegExpPrototypeBuiltinsFactory.CompiledRegexFlagPropertyAccessorNodeGen.create(context, builtin, flagName, args);
        }
    }

    abstract static class CompiledRegexPatternAccessor extends JSBuiltinNode {

        CompiledRegexPatternAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        Object doRegExp(JSRegExpObject obj,
                        @Cached(inline = true) TRegexUtil.InteropReadStringMemberNode readPatternNode) {
            return JSRegExp.escapeRegExpPattern(readPatternNode.execute(this, JSRegExp.getCompiledRegex(obj), TRegexUtil.Props.CompiledRegex.PATTERN));
        }

        @Specialization(guards = "isRegExpPrototype(obj)")
        Object doPrototype(@SuppressWarnings("unused") JSDynamicObject obj) {
            return Strings.EMPTY_REGEX;
        }

        @Fallback
        public Object doObject(Object obj) {
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }

        boolean isRegExpPrototype(JSDynamicObject obj) {
            return obj == getRealm().getRegExpPrototype();
        }

        static CompiledRegexPatternAccessor create(JSContext context, JSBuiltin builtin, JavaScriptNode[] args) {
            return RegExpPrototypeBuiltinsFactory.CompiledRegexPatternAccessorNodeGen.create(context, builtin, args);
        }
    }

}
