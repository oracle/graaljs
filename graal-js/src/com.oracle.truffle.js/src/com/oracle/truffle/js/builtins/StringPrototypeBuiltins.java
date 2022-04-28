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

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltins.JSRegExpExecES5Node;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltinsFactory.JSRegExpExecES5NodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.CreateHTMLNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.CreateStringIteratorNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringAtNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringCharAtNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringCharCodeAtNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringCodePointAtNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringConcatNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringEndsWithNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringIncludesNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringIndexOfNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringLastIndexOfNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringLocaleCompareIntlNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringLocaleCompareNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringMatchES5NodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringMatchNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringNormalizeNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringPadNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringRepeatNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringReplaceAllNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringReplaceES5NodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringReplaceNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSearchES5NodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSearchNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSliceNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSplitNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringStartsWithNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSubstrNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringSubstringNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToLocaleLowerCaseIntlNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToLocaleUpperCaseIntlNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToLowerCaseNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToStringNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToUpperCaseNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringTrimLeftNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringTrimNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringTrimRightNodeGen;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNode.JSRegExpExecIntlIgnoreLastIndexNode;
import com.oracle.truffle.js.builtins.helper.ReplaceStringParser;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsRegExpNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToRegExpNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.cast.JSTrimWhitespaceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.intl.CreateRegExpNode;
import com.oracle.truffle.js.nodes.intl.InitializeCollatorNode;
import com.oracle.truffle.js.nodes.intl.JSToCanonicalizedLocaleListNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexCompiledRegexAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexNamedCaptureGroupsAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

/**
 * Contains builtins for {@linkplain JSString}.prototype.
 */
public final class StringPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<StringPrototypeBuiltins.StringPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new StringPrototypeBuiltins();
    public static final JSBuiltinsContainer EXTENSION_BUILTINS = new StringPrototypeExtensionBuiltins();

    protected StringPrototypeBuiltins() {
        super(JSString.PROTOTYPE_NAME, StringPrototype.class);
    }

    public enum StringPrototype implements BuiltinEnum<StringPrototype> {
        charAt(1),
        charCodeAt(1),
        concat(1),
        indexOf(1),
        lastIndexOf(1),
        localeCompare(1),
        match(1),
        replace(2),
        search(1),
        slice(2),
        split(2),
        substring(2),
        toLowerCase(0),
        toLocaleLowerCase(0),
        toUpperCase(0),
        toLocaleUpperCase(0),
        toString(0),
        valueOf(0),
        trim(0),

        // Annex B
        substr(2),
        anchor(1),
        big(0),
        blink(0),
        bold(0),
        fixed(0),
        fontcolor(1),
        fontsize(1),
        italics(0),
        link(1),
        small(0),
        strike(0),
        sub(0),
        sup(0),

        // ES6/ES2015
        startsWith(1),
        endsWith(1),
        includes(1),
        repeat(1),
        codePointAt(1),
        _iterator(0),
        normalize(0),

        // ES2017
        padStart(1),
        padEnd(1),

        // ES2020
        matchAll(1),

        // ES2021
        replaceAll(2),

        // ES2022
        at(1);

        private final int length;

        StringPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isAnnexB() {
            return EnumSet.range(substr, sup).contains(this);
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.range(startsWith, normalize).contains(this)) {
                return 6;
            } else if (EnumSet.range(padStart, padEnd).contains(this)) {
                return JSConfig.ECMAScript2017;
            } else if (matchAll == this) {
                return JSConfig.ECMAScript2020;
            } else if (replaceAll == this) {
                return JSConfig.ECMAScript2021;
            } else if (at == this) {
                return JSConfig.ECMAScript2022;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }

        @Override
        public Object getKey() {
            return this == _iterator ? Symbol.SYMBOL_ITERATOR : BuiltinEnum.super.getKey();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, StringPrototype builtinEnum) {
        switch (builtinEnum) {
            case charAt:
                return JSStringCharAtNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case charCodeAt:
                return JSStringCharCodeAtNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case concat:
                return JSStringConcatNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case indexOf:
                return JSStringIndexOfNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case lastIndexOf:
                return JSStringLastIndexOfNodeGen.create(context, builtin, args().withThis().fixedArgs(2).varArgs().createArgumentNodes(context));
            case localeCompare:
                if (context.isOptionIntl402()) {
                    return JSStringLocaleCompareIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
                } else {
                    return JSStringLocaleCompareNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                }
            case match:
                if (context.getEcmaScriptVersion() >= 6) {
                    return JSStringMatchNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSStringMatchES5NodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                }
            case replace:
                if (context.getEcmaScriptVersion() >= 6) {
                    return JSStringReplaceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSStringReplaceES5NodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                }
            case replaceAll:
                return JSStringReplaceAllNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case search:
                if (context.getEcmaScriptVersion() >= 6) {
                    return JSStringSearchNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSStringSearchES5NodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
                }
            case slice:
                return JSStringSliceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case split:
                return JSStringSplitNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case substr:
                return JSStringSubstrNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case substring:
                return JSStringSubstringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case toLowerCase:
                return JSStringToLowerCaseNodeGen.create(context, builtin, false, args().withThis().createArgumentNodes(context));
            case toLocaleLowerCase:
                if (context.isOptionIntl402()) {
                    return JSStringToLocaleLowerCaseIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSStringToLowerCaseNodeGen.create(context, builtin, true, args().withThis().createArgumentNodes(context));
                }
            case toUpperCase:
                return JSStringToUpperCaseNodeGen.create(context, builtin, false, args().withThis().createArgumentNodes(context));
            case toLocaleUpperCase:
                if (context.isOptionIntl402()) {
                    return JSStringToLocaleUpperCaseIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSStringToUpperCaseNodeGen.create(context, builtin, true, args().withThis().createArgumentNodes(context));
                }
            case toString:
                return JSStringToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSStringToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case trim:
                return JSStringTrimNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

            case startsWith:
                return JSStringStartsWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case endsWith:
                return JSStringEndsWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case includes:
                return JSStringIncludesNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case repeat:
                return JSStringRepeatNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case codePointAt:
                return JSStringCodePointAtNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case _iterator:
                return CreateStringIteratorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case normalize:
                return JSStringNormalizeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));

            case matchAll:
                return JSStringMatchNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case padStart:
                return JSStringPadNodeGen.create(context, builtin, true, args().withThis().varArgs().createArgumentNodes(context));
            case padEnd:
                return JSStringPadNodeGen.create(context, builtin, false, args().withThis().varArgs().createArgumentNodes(context));

            case anchor:
                return createHTMLNode(context, builtin, Strings.A, Strings.NAME);
            case big:
                return createHTMLNode(context, builtin, Strings.BIG, Strings.EMPTY_STRING);
            case blink:
                return createHTMLNode(context, builtin, Strings.BLINK, Strings.EMPTY_STRING);
            case bold:
                return createHTMLNode(context, builtin, Strings.B, Strings.EMPTY_STRING);
            case fixed:
                return createHTMLNode(context, builtin, Strings.TT, Strings.EMPTY_STRING);
            case fontcolor:
                return createHTMLNode(context, builtin, Strings.FONT, Strings.COLOR);
            case fontsize:
                return createHTMLNode(context, builtin, Strings.FONT, Strings.SIZE);
            case italics:
                return createHTMLNode(context, builtin, Strings.I, Strings.EMPTY_STRING);
            case link:
                return createHTMLNode(context, builtin, Strings.A, Strings.HREF);
            case small:
                return createHTMLNode(context, builtin, Strings.SMALL, Strings.EMPTY_STRING);
            case strike:
                return createHTMLNode(context, builtin, Strings.STRIKE, Strings.EMPTY_STRING);
            case sub:
                return createHTMLNode(context, builtin, Strings.SUB, Strings.EMPTY_STRING);
            case sup:
                return createHTMLNode(context, builtin, Strings.SUP, Strings.EMPTY_STRING);

            case at:
                return JSStringAtNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static final class StringPrototypeExtensionBuiltins extends JSBuiltinsContainer.SwitchEnum<StringPrototypeExtensionBuiltins.StringExtensionBuiltins> {
        protected StringPrototypeExtensionBuiltins() {
            super(JSString.CLASS_NAME_EXTENSIONS, StringExtensionBuiltins.class);
        }

        public enum StringExtensionBuiltins implements BuiltinEnum<StringExtensionBuiltins> {
            trimStart(0),
            trimEnd(0);

            private final int length;

            StringExtensionBuiltins(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, StringExtensionBuiltins builtinEnum) {
            switch (builtinEnum) {
                case trimStart:
                    return JSStringTrimLeftNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case trimEnd:
                    return JSStringTrimRightNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            }
            return null;
        }
    }

    abstract static class JSStringOperation extends JSBuiltinNode {
        JSStringOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode;
        @Child private JSToStringNode toStringNode;
        @Child private JSToIntegerAsIntNode toIntegerNode;
        @Child private TruffleString.ReadCharUTF16Node stringReadNode;

        protected static int within(int value, int min, int max) {
            assert min <= max;
            if (value >= max) {
                return max;
            } else if (value <= min) {
                return min;
            }
            return value;
        }

        protected static int withinNumber(Number value, int min, int max) {
            assert min <= max;
            double dValue = JSRuntime.doubleValue(value);
            if (Double.isInfinite(dValue)) {
                return dValue < 0 ? min : max;
            }
            long lValue = JSRuntime.intValue(value);
            if (lValue >= max) {
                return max;
            } else if (lValue <= min) {
                return min;
            }
            return (int) lValue;
        }

        protected final void requireObjectCoercible(Object target) {
            if (requireObjectCoercibleNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                requireObjectCoercibleNode = insert(RequireObjectCoercibleNode.create());
            }
            requireObjectCoercibleNode.executeVoid(target);
        }

        protected TruffleString toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        protected char charAt(TruffleString s, int i) {
            if (stringReadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringReadNode = insert(TruffleString.ReadCharUTF16Node.create());
            }
            return Strings.charAt(stringReadNode, s, i);
        }

        protected int toIntegerAsInt(Object target) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerAsIntNode.create());
            }
            return toIntegerNode.executeInt(target);
        }
    }

    public abstract static class JSStringOperationWithRegExpArgument extends JSStringOperation {
        @Child protected JSRegExpExecIntlNode regExpNode;
        @Child protected JSRegExpExecIntlIgnoreLastIndexNode regExpIgnoreLastIndexNode;

        @Child private JSFunctionCallNode callNode;
        @Child private PropertyGetNode getSymbolNode;
        @Child private GetMethodNode getMethodNode;
        protected final ConditionProfile isSpecialProfile = ConditionProfile.createBinaryProfile();
        protected final ConditionProfile callSpecialProfile = ConditionProfile.createBinaryProfile();

        public JSStringOperationWithRegExpArgument(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final Object matchIgnoreLastIndex(JSDynamicObject regExp, TruffleString input, int fromIndex) {
            assert getContext().getEcmaScriptVersion() <= 5;
            return getRegExpIgnoreLastIndexNode().execute(regExp, input, fromIndex);
        }

        // only used in ES5 mode
        protected JSRegExpExecIntlNode getRegExpNode() {
            if (regExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                regExpNode = insert(JSRegExpExecIntlNode.create(getContext()));
            }
            return regExpNode;
        }

        // only used in ES5 mode
        protected JSRegExpExecIntlIgnoreLastIndexNode getRegExpIgnoreLastIndexNode() {
            if (regExpIgnoreLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                regExpIgnoreLastIndexNode = insert(JSRegExpExecIntlIgnoreLastIndexNode.create(getContext(), true));
            }
            return regExpIgnoreLastIndexNode;
        }

        protected final Object call(Object function, Object target, Object[] args) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSFunctionCallNode.createCall());
            }
            return callNode.executeCall(JSArguments.create(target, function, args));
        }

        protected final Object invoke(JSDynamicObject regExp, Symbol symbol, TruffleString thisStr) {
            assert JSRuntime.isPropertyKey(symbol);
            if (getSymbolNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSymbolNode = insert(PropertyGetNode.create(symbol, false, getContext()));
            }
            Object func = getSymbolNode.getValue(regExp);
            return call(func, regExp, new Object[]{thisStr});
        }

        protected final Object getMethod(Object target, Object key) {
            if (getMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMethodNode = insert(GetMethodNode.create(getContext(), key));
            }
            return getMethodNode.executeWithTarget(target);
        }
    }

    /**
     * Implementation of the String.prototype.charAt() method as specified by ECMAScript 5.1 in
     * 15.5.4.4.
     */
    public abstract static class JSStringCharAtNode extends JSStringOperation implements JSBuiltinNode.Inlineable {
        private final ConditionProfile indexOutOfBounds = ConditionProfile.createBinaryProfile();

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringCharAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString stringCharAt(TruffleString thisObj, int pos) {
            if (indexOutOfBounds.profile(pos < 0 || pos >= Strings.length(thisObj))) {
                return Strings.EMPTY_STRING;
            } else {
                return Strings.substring(getContext(), substringNode, thisObj, pos, 1);
            }
        }

        @Specialization
        protected TruffleString charAt(Object thisObj, Object index) {
            requireObjectCoercible(thisObj);
            return stringCharAt(toString(thisObj), toIntegerAsInt(index));
        }

        @Override
        public Inlined createInlined() {
            return JSStringCharAtNodeGen.InlinedNodeGen.create(getContext(), getBuiltin(), getArguments());
        }

        public abstract static class Inlined extends JSStringCharAtNode implements JSBuiltinNode.Inlined {
            public Inlined(JSContext context, JSBuiltin builtin) {
                super(context, builtin);
            }

            @Override
            @Specialization
            protected TruffleString charAt(Object thisObj, Object indexObj) {
                throw rewriteToCall();
            }

            protected abstract Object executeWithArguments(Object arg0, Object arg1);

            @Override
            public Object callInlined(Object[] arguments) {
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    throw rewriteToCall();
                }
                return executeWithArguments(JSArguments.getThisObject(arguments), JSArguments.getUserArgument(arguments, 0));
            }
        }
    }

    /**
     * Implementation of the String.prototype.charCodeAt() method as specified by ECMAScript 5.1 in
     * 15.5.4.5.
     */
    public abstract static class JSStringCharCodeAtNode extends JSStringOperation implements JSBuiltinNode.Inlineable {
        private final ConditionProfile indexOutOfBounds = ConditionProfile.createBinaryProfile();

        public JSStringCharCodeAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static boolean posInBounds(TruffleString thisStr, int pos) {
            return pos >= 0 && pos < Strings.length(thisStr);
        }

        @Specialization(guards = {"posInBounds(thisStr, pos)"})
        protected int charCodeAtInBounds(TruffleString thisStr, int pos) {
            return charAt(thisStr, pos);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!posInBounds(thisStr, pos)")
        protected double charCodeAtOutOfBounds(TruffleString thisStr, int pos) {
            return Double.NaN;
        }

        @Specialization(replaces = {"charCodeAtInBounds", "charCodeAtOutOfBounds"})
        protected Object charCodeAtGeneric(Object thisObj, Object indexObj,
                        @Cached JSToNumberNode toNumberNode) {
            requireObjectCoercible(thisObj);
            TruffleString s = toString(thisObj);
            Number index = toNumberNode.executeNumber(indexObj);
            long lIndex = JSRuntime.toInteger(index);
            if (indexOutOfBounds.profile(0 > lIndex || lIndex >= Strings.length(s))) {
                return Double.NaN;
            } else {
                return Integer.valueOf(charAt(s, (int) lIndex));
            }
        }

        @Override
        public Inlined createInlined() {
            return JSStringCharCodeAtNodeGen.InlinedNodeGen.create(getContext(), getBuiltin(), getArguments());
        }

        public abstract static class Inlined extends JSStringCharCodeAtNode implements JSBuiltinNode.Inlined {
            public Inlined(JSContext context, JSBuiltin builtin) {
                super(context, builtin);
            }

            @Override
            @Specialization
            protected Object charCodeAtGeneric(Object thisObj, Object indexObj,
                            @Cached JSToNumberNode toNumberNode) {
                throw rewriteToCall();
            }

            protected abstract Object executeWithArguments(Object arg0, Object arg1);

            @Override
            public Object callInlined(Object[] arguments) {
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    throw rewriteToCall();
                }
                return executeWithArguments(JSArguments.getThisObject(arguments), JSArguments.getUserArgument(arguments, 0));
            }
        }
    }

    /**
     * Implementation of the String.prototype.substring() method as specified by ECMAScript 5.1 in
     * 15.5.4.15.
     */
    public abstract static class JSStringSubstringNode extends JSStringOperation implements JSBuiltinNode.Inlineable {
        private final ConditionProfile startLowerEnd = ConditionProfile.createBinaryProfile();

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringSubstringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString substring(TruffleString thisStr, int start, int end) {
            int len = Strings.length(thisStr);
            int finalStart = within(start, 0, len);
            int finalEnd = within(end, 0, len);
            return substringIntl(thisStr, finalStart, finalEnd);
        }

        @Specialization(guards = "isUndefined(end)")
        protected TruffleString substringStart(TruffleString thisStr, int start, @SuppressWarnings("unused") Object end) {
            int len = Strings.length(thisStr);
            int finalStart = within(start, 0, len);
            int finalEnd = len;
            return substringIntl(thisStr, finalStart, finalEnd);
        }

        private TruffleString substringIntl(TruffleString thisStr, int start, int end) {
            final int fromIndex;
            final int length;
            if (startLowerEnd.profile(start <= end)) {
                fromIndex = start;
                length = end - start;
            } else {
                fromIndex = end;
                length = start - end;
            }
            return Strings.substring(getContext(), substringNode, thisStr, fromIndex, length);
        }

        @Specialization(replaces = {"substring", "substringStart"})
        protected TruffleString substringGeneric(Object thisObj, Object start, Object end,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached JSToNumberNode toNumber2Node,
                        @Cached("createBinaryProfile()") ConditionProfile startUndefined,
                        @Cached("createBinaryProfile()") ConditionProfile endUndefined) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int len = Strings.length(thisStr);
            int intStart;
            int intEnd;
            if (startUndefined.profile(start == Undefined.instance)) {
                intStart = 0;
            } else {
                intStart = withinNumber(toNumberNode.executeNumber(start), 0, len);
            }
            if (endUndefined.profile(end == Undefined.instance)) {
                intEnd = len;
            } else {
                intEnd = withinNumber(toNumber2Node.executeNumber(end), 0, len);
            }
            return substringIntl(thisStr, intStart, intEnd);
        }

        @Override
        public Inlined createInlined() {
            return JSStringSubstringNodeGen.InlinedNodeGen.create(getContext(), getBuiltin(), getArguments());
        }

        public abstract static class Inlined extends JSStringSubstringNode implements JSBuiltinNode.Inlined {
            public Inlined(JSContext context, JSBuiltin builtin) {
                super(context, builtin);
            }

            @Override
            @Specialization
            protected TruffleString substringGeneric(Object thisObj, Object start, Object end,
                            @Cached JSToNumberNode toNumberNode,
                            @Cached JSToNumberNode toNumber2Node,
                            @Cached("createBinaryProfile()") ConditionProfile startUndefined,
                            @Cached("createBinaryProfile()") ConditionProfile endUndefined) {
                throw rewriteToCall();
            }

            protected abstract Object executeWithArguments(Object arg0, Object arg1, Object arg2);

            @Override
            public Object callInlined(Object[] arguments) {
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    throw rewriteToCall();
                }
                Object thisObj = JSArguments.getThisObject(arguments);
                Object start = JSArguments.getUserArgument(arguments, 0);
                Object end = JSArguments.getUserArgumentCount(arguments) >= 2 ? JSArguments.getUserArgument(arguments, 1) : Undefined.instance;
                return executeWithArguments(thisObj, start, end);
            }
        }
    }

    /**
     * Implementation of the String.prototype.indexOf() method as specified by ECMAScript 5.1 in
     * 15.5.4.7.
     */
    public abstract static class JSStringIndexOfNode extends JSStringOperation {
        private final ConditionProfile hasPos = ConditionProfile.createBinaryProfile();

        public JSStringIndexOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isUndefined(position)")
        protected int indexOfStringUndefined(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared("indexOfStringNode") TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr);
        }

        @Specialization
        protected int indexOfStringInt(TruffleString thisStr, TruffleString searchStr, int position,
                        @Cached @Shared("indexOfStringNode") TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            return indexOfIntl(thisStr, searchStr, position, indexOfStringNode);
        }

        @Specialization(guards = "!isStringString(thisObj, searchObj) || !isUndefined(position)", replaces = {"indexOfStringInt"})
        // replace only the StringInt specialization that duplicates code
        protected int indexOfGeneric(Object thisObj, Object searchObj, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached @Shared("indexOfStringNode") TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString searchStr = toString2Node.executeString(searchObj);
            return indexOfIntl(thisStr, searchStr, position, indexOfStringNode);
        }

        private int indexOfIntl(TruffleString thisStr, TruffleString searchStr, Object position, TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            int startPos;
            if (hasPos.profile(position != Undefined.instance)) {
                startPos = Math.min(toIntegerAsInt(position), Strings.length(thisStr));
            } else {
                startPos = 0;
            }
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr, startPos);
        }
    }

    /**
     * Implementation of the String.prototype.lastIndexOf() method as specified by ECMAScript 5.1 in
     * 15.5.4.8.
     */
    public abstract static class JSStringLastIndexOfNode extends JSStringOperation {

        public JSStringLastIndexOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isUndefined(position)")
        protected int lastIndexOfString(TruffleString thisObj, TruffleString searchString, @SuppressWarnings("unused") Object position,
                        @Cached @Shared("lastIndexOfNode") TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            return Strings.lastIndexOf(lastIndexOfNode, thisObj, searchString, Strings.length(thisObj));
        }

        @Specialization
        protected int lastIndexOfString(TruffleString thisObj, TruffleString searchString, int position,
                        @Cached @Shared("lastIndexOfNode") TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            int len = Strings.length(thisObj);
            int pos = within(position, 0, len);
            return Strings.lastIndexOf(lastIndexOfNode, thisObj, searchString, pos);
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected int lastIndexOf(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached ConditionProfile posNaN,
                        @Cached @Shared("lastIndexOfNode") TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString searchStr = toString2Node.executeString(searchString);
            Number numPos = toNumberNode.executeNumber(position);
            int lastPos = Strings.length(thisStr);
            int pos;

            double dVal = JSRuntime.doubleValue(numPos);
            if (posNaN.profile(Double.isNaN(dVal))) {
                pos = lastPos;
            } else {
                pos = within((int) dVal, 0, lastPos);
            }
            return Strings.lastIndexOf(lastIndexOfNode, thisStr, searchStr, pos);
        }
    }

    /**
     * Implementation of the String.prototype.split() method as specified by ECMAScript 5.1 in
     * 15.5.4.14.
     */
    public abstract static class JSStringSplitNode extends JSStringOperationWithRegExpArgument {
        public JSStringSplitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile emptyInput = ConditionProfile.createBinaryProfile();
        private final ConditionProfile emptySeparator = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zeroLimit = ConditionProfile.createBinaryProfile();
        private final ConditionProfile matchProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile isUndefinedBranch = BranchProfile.create();
        private final BranchProfile isStringBranch = BranchProfile.create();
        private final BranchProfile isRegexpBranch = BranchProfile.create();
        private final BranchProfile growProfile = BranchProfile.create();

        @Child private JSToUInt32Node toUInt32Node;
        @Child private JSToStringNode toString2Node;

        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor;
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor;

        @Child private TruffleString.SubstringByteIndexNode substringNode;
        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfNode;

        private int toUInt32(Object target) {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return (int) Math.min(Integer.MAX_VALUE, JSRuntime.toInteger((Number) toUInt32Node.execute(target)));
        }

        private TruffleString toString2(Object obj) {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node.executeString(obj);
        }

        private TruffleString substring(TruffleString a, int fromIndex) {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                substringNode = insert(TruffleString.SubstringByteIndexNode.create());
            }
            return Strings.substring(getContext(), substringNode, a, fromIndex);
        }

        private TruffleString substring(TruffleString a, int fromIndex, int length) {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                substringNode = insert(TruffleString.SubstringByteIndexNode.create());
            }
            return Strings.substring(getContext(), substringNode, a, fromIndex, length);
        }

        private int indexOf(TruffleString s1, TruffleString s2, int fromIndex) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfStringNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, s1, s2, fromIndex);
        }

        protected boolean isES6OrNewer() {
            return getContext().getEcmaScriptVersion() >= 6;
        }

        @Specialization(guards = "!isES6OrNewer()")
        protected Object splitES5(Object thisObj, Object separator, Object limitObj) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int limit = getLimit(limitObj);
            if (separator == Undefined.instance) {
                isUndefinedBranch.enter();
                return split(thisStr, limit, NOP_SPLITTER, null);
            } else if (JSRegExp.isJSRegExp(separator)) {
                isRegexpBranch.enter();
                return split(thisStr, limit, REGEXP_SPLITTER, (JSDynamicObject) separator);
            } else {
                isStringBranch.enter();
                TruffleString separatorStr = toString2(separator);
                return split(thisStr, limit, STRING_SPLITTER, separatorStr);
            }
        }

        protected boolean isFastPath(Object thisObj, Object separator, Object limit) {
            return Strings.isTString(thisObj) && Strings.isTString(separator) && limit == Undefined.instance;
        }

        @Specialization(guards = {"isES6OrNewer()", "isUndefined(limit)"})
        protected Object splitES6StrStrUndefined(TruffleString thisStr, TruffleString sepStr, @SuppressWarnings("unused") JSDynamicObject limit) {
            return split(thisStr, Integer.MAX_VALUE, STRING_SPLITTER, sepStr);
        }

        @Specialization(guards = {"isES6OrNewer()", "!isFastPath(thisObj, separator, limit)"})
        protected Object splitES6Generic(Object thisObj, Object separator, Object limit) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(!(separator == Undefined.instance || separator == Null.instance))) {
                Object splitter = getMethod(separator, Symbol.SYMBOL_SPLIT);
                if (callSpecialProfile.profile(splitter != Undefined.instance)) {
                    return call(splitter, separator, new Object[]{thisObj, limit});
                }
            }
            return builtinSplit(thisObj, separator, limit);
        }

        private Object builtinSplit(Object thisObj, Object separator, Object limit) {
            TruffleString thisStr = toString(thisObj);
            int lim = getLimit(limit);
            TruffleString sepStr = toString2(separator);
            if (separator == Undefined.instance) {
                return split(thisStr, lim, NOP_SPLITTER, null);
            } else {
                return split(thisStr, lim, STRING_SPLITTER, sepStr);
            }
        }

        private int getLimit(Object limit) {
            return (limit == Undefined.instance) ? Integer.MAX_VALUE : toUInt32(limit);
        }

        private <T> JSDynamicObject split(TruffleString thisStr, int limit, Splitter<T> splitter, T separator) {
            JSRealm realm = getRealm();
            if (zeroLimit.profile(limit == 0)) {
                return JSArray.createEmptyZeroLength(getContext(), realm);
            }
            Object[] splits = splitter.split(thisStr, limit, separator, this);
            return JSArray.createConstant(getContext(), realm, splits);
        }

        public TRegexUtil.TRegexCompiledRegexAccessor getCompiledRegexAccessor() {
            if (compiledRegexAccessor == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compiledRegexAccessor = insert(TRegexUtil.TRegexCompiledRegexAccessor.create());
            }
            return compiledRegexAccessor;
        }

        public TRegexUtil.TRegexResultAccessor getResultAccessor() {
            if (resultAccessor == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                resultAccessor = insert(TRegexUtil.TRegexResultAccessor.create());
            }
            return resultAccessor;
        }

        private interface Splitter<T> {
            Object[] split(TruffleString input, int limit, T separator, JSStringSplitNode parent);
        }

        private static final Splitter<Void> NOP_SPLITTER = (input, limit, separator, parent) -> new Object[]{input};
        private static final Splitter<TruffleString> STRING_SPLITTER = new StringSplitter();
        private static final Splitter<JSDynamicObject> REGEXP_SPLITTER = new RegExpSplitter();

        private static final class StringSplitter implements Splitter<TruffleString> {
            @Override
            public Object[] split(TruffleString input, int limit, TruffleString separator, JSStringSplitNode parent) {
                if (parent.emptySeparator.profile(Strings.isEmpty(separator))) {
                    return individualCharSplit(input, limit, parent);
                } else {
                    return regularSplit(input, limit, separator, parent);
                }
            }

            private static Object[] regularSplit(TruffleString input, int limit, TruffleString separator, JSStringSplitNode parent) {
                int end = parent.indexOf(input, separator, 0);
                if (parent.matchProfile.profile(end == -1)) {
                    return new Object[]{input};
                }
                return regularSplitIntl(input, limit, separator, end, parent);
            }

            private static Object[] regularSplitIntl(TruffleString input, int limit, TruffleString separator, int endParam, JSStringSplitNode parent) {
                SimpleArrayList<Object> splits = SimpleArrayList.create(limit);
                int start = 0;
                int end = endParam;
                while (end != -1) {
                    splits.add(parent.substring(input, start, end - start), parent.growProfile);
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = end + Strings.length(separator);
                    end = parent.indexOf(input, separator, start);
                }
                splits.add(parent.substring(input, start), parent.growProfile);
                return splits.toArray();
            }

            private static Object[] individualCharSplit(TruffleString input, int limit, JSStringSplitNode parent) {
                int len = Math.min(Strings.length(input), limit);
                Object[] array = new Object[len];
                for (int i = 0; i < len; i++) {
                    array[i] = parent.substring(input, i, 1);
                }
                return array;
            }
        }

        private static final class RegExpSplitter implements Splitter<JSDynamicObject> {
            private static final Object[] EMPTY_SPLITS = {};
            private static final Object[] SINGLE_ZERO_LENGTH_SPLIT = {Strings.EMPTY_STRING};

            @Override
            public Object[] split(TruffleString input, int limit, JSDynamicObject regExp, JSStringSplitNode parent) {
                if (parent.emptyInput.profile(Strings.isEmpty(input))) {
                    return splitEmptyString(regExp, parent);
                } else {
                    return splitNonEmptyString(input, limit, regExp, parent);
                }
            }

            private static Object[] splitEmptyString(JSDynamicObject regExp, JSStringSplitNode parent) {
                Object result = parent.matchIgnoreLastIndex(regExp, Strings.EMPTY_STRING, 0);
                return parent.matchProfile.profile(parent.getResultAccessor().isMatch(result)) ? EMPTY_SPLITS : SINGLE_ZERO_LENGTH_SPLIT;
            }

            private static Object[] splitNonEmptyString(TruffleString input, int limit, JSDynamicObject regExp, JSStringSplitNode parent) {
                Object result = parent.matchIgnoreLastIndex(regExp, input, 0);
                if (parent.matchProfile.profile(!parent.getResultAccessor().isMatch(result))) {
                    return new Object[]{input};
                }
                SimpleArrayList<Object> splits = new SimpleArrayList<>();
                int start = 0;
                while (parent.getResultAccessor().isMatch(result)) {
                    int matchStart = parent.getResultAccessor().captureGroupStart(result, 0);
                    int matchEnd = parent.getResultAccessor().captureGroupEnd(result, 0);
                    if (matchEnd - matchStart == 0 && matchStart == start) {
                        // Avoid empty splits when using a regex that matches the empty string.
                        if (matchStart == Strings.length(input) - 1) {
                            break;
                        }
                        result = parent.matchIgnoreLastIndex(regExp, input, start + 1);
                        continue;
                    }
                    TruffleString split = parent.substring(input, start, matchStart - start);
                    splits.add(split, parent.growProfile);
                    int count = Math.min(parent.getCompiledRegexAccessor().groupCount(JSRegExp.getCompiledRegex(regExp)) - 1, limit - splits.size());
                    for (int i = 1; i <= count; i++) {
                        int groupStart = parent.getResultAccessor().captureGroupStart(result, i);
                        if (groupStart == TRegexUtil.Constants.CAPTURE_GROUP_NO_MATCH) {
                            splits.add(Undefined.instance, parent.growProfile);
                        } else {
                            splits.add(parent.substring(input, groupStart, parent.getResultAccessor().captureGroupEnd(result, i) - groupStart), parent.growProfile);
                        }
                    }
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = matchEnd + (matchEnd == start ? 1 : 0);
                    result = parent.matchIgnoreLastIndex(regExp, input, start);
                }
                splits.add(parent.substring(input, start), parent.growProfile);
                return splits.toArray();
            }
        }
    }

    /**
     * Implementation of the String.prototype.concat() method as specified by ECMAScript 5.1 in
     * 15.5.4.6.
     */
    public abstract static class JSStringConcatNode extends JSStringOperation {

        private final StringBuilderProfile stringBuilderProfile;

        public JSStringConcatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @Specialization
        protected Object concat(Object thisObj, Object[] args,
                        @Cached JSToStringNode toString2Node,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.ToStringNode sbToStringNode) {
            requireObjectCoercible(thisObj);
            TruffleStringBuilder sb = stringBuilderProfile.newStringBuilder();
            stringBuilderProfile.append(appendStringNode, sb, toString(thisObj));
            for (Object o : args) {
                stringBuilderProfile.append(appendStringNode, sb, toString2Node.executeString(o));
                TruffleSafepoint.poll(this);
            }
            return StringBuilderProfile.toString(sbToStringNode, sb);
        }
    }

    public abstract static class JSStringReplaceBaseNode extends JSStringOperationWithRegExpArgument {
        @Child protected JSFunctionCallNode functionReplaceCallNode;
        @Child protected JSToStringNode toString2Node;
        @Child protected JSToStringNode toString3Node;
        @Child protected IsCallableNode isCallableNode;
        protected final ConditionProfile functionalReplaceProfile = ConditionProfile.createBinaryProfile();
        protected final ConditionProfile replaceNecessaryProfile = ConditionProfile.createBinaryProfile();
        protected final BranchProfile dollarProfile = BranchProfile.create();

        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.AppendSubstringByteIndexNode appendSubStringNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;

        public JSStringReplaceBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected ReplaceStringParser.Token[] parseReplaceValue(TruffleString replaceValue) {
            return ReplaceStringParser.parse(getContext(), replaceValue, 0, false);
        }

        protected static void appendSubstitution(TruffleStringBuilder sb, TruffleString input, TruffleString replaceStr, TruffleString searchStr, int pos, BranchProfile dollarProfile,
                        JSStringReplaceBaseNode node) {
            ReplaceStringParser.process(node.getContext(), replaceStr, 0, false, dollarProfile, new ReplaceStringConsumer(sb, input, replaceStr, searchStr, pos), node);
        }

        protected static final class ReplaceStringConsumer implements ReplaceStringParser.Consumer<JSStringReplaceBaseNode, TruffleStringBuilder> {

            private final TruffleStringBuilder sb;
            private final TruffleString input;
            private final TruffleString searchStr;
            private final TruffleString replaceStr;
            private final int matchedPos;

            private ReplaceStringConsumer(TruffleStringBuilder sb, TruffleString input, TruffleString replaceStr, TruffleString searchStr, int matchedPos) {
                this.sb = sb;
                this.input = input;
                this.replaceStr = replaceStr;
                this.searchStr = searchStr;
                this.matchedPos = matchedPos;
            }

            @Override
            public void literal(JSStringReplaceBaseNode node, int start, int end) {
                node.append(sb, replaceStr, start, end);
            }

            @Override
            public void match(JSStringReplaceBaseNode node) {
                node.append(sb, searchStr);
            }

            @Override
            public void matchHead(JSStringReplaceBaseNode node) {
                node.append(sb, input, 0, matchedPos);
            }

            @Override
            public void matchTail(JSStringReplaceBaseNode node) {
                node.append(sb, input, matchedPos + Strings.length(searchStr), Strings.length(input));
            }

            @Override
            public void captureGroup(JSStringReplaceBaseNode node, int groupNumber, int literalStart, int literalEnd) {
                throw Errors.shouldNotReachHere();
            }

            @Override
            public void namedCaptureGroup(JSStringReplaceBaseNode node, TruffleString groupName) {
                throw Errors.shouldNotReachHere();
            }

            @Override
            public TruffleStringBuilder getResult() {
                return sb;
            }
        }

        protected final Object functionReplaceCall(Object splitter, Object separator, Object[] args) {
            if (functionReplaceCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionReplaceCallNode = insert(JSFunctionCallNode.createCall());
            }
            return functionReplaceCallNode.executeCall(JSArguments.create(separator, splitter, args));
        }

        void append(TruffleStringBuilder sb, TruffleString s) {
            if (appendStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendStringNode = insert(TruffleStringBuilder.AppendStringNode.create());
            }
            Strings.builderAppend(appendStringNode, sb, s);
        }

        void append(TruffleStringBuilder sb, TruffleString s, int fromIndex, int toIndex) {
            if (appendSubStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendSubStringNode = insert(TruffleStringBuilder.AppendSubstringByteIndexNode.create());
            }
            Strings.builderAppend(appendSubStringNode, sb, s, fromIndex, toIndex);
        }

        void appendLen(TruffleStringBuilder sb, TruffleString s, int fromIndex, int length) {
            if (appendSubStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendSubStringNode = insert(TruffleStringBuilder.AppendSubstringByteIndexNode.create());
            }
            Strings.builderAppendLen(appendSubStringNode, sb, s, fromIndex, length);
        }

        TruffleString builderToString(TruffleStringBuilder sb) {
            if (builderToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                builderToStringNode = insert(TruffleStringBuilder.ToStringNode.create());
            }
            return Strings.builderToString(builderToStringNode, sb);
        }
    }

    /**
     * Implementation of the String.prototype.replace() method as specified by ECMAScript 5.1 in
     * 15.5.4.11.
     */
    public abstract static class JSStringReplaceNode extends JSStringReplaceBaseNode {

        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfNode;

        public JSStringReplaceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)")
        protected Object replaceStringCached(TruffleString thisObj, TruffleString searchValue, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, TruffleString searchValue, TruffleString replaceValue) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, replaceValue, thisObj, null);
        }

        @Specialization(guards = "!isStringString(searchValue, replaceValue)")
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(!(searchValue == Undefined.instance || searchValue == Null.instance))) {
                Object replacer = getMethod(searchValue, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(replacer != Undefined.instance)) {
                    return call(replacer, searchValue, new Object[]{thisObj, replaceValue});
                }
            }
            // all child nodes must be checked to avoid race conditions on shared ASTs
            if (toString2Node == null || toString3Node == null || isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
                toString3Node = insert(JSToStringNode.create());
                isCallableNode = insert(IsCallableNode.create());
            }
            return builtinReplace(searchValue, replaceValue, thisObj);
        }

        private Object builtinReplace(Object searchValue, Object replParam, Object o) {
            TruffleString input = toString(o);
            TruffleString searchString = toString2Node.executeString(searchValue);
            boolean functionalReplace = isCallableNode.executeBoolean(replParam);
            TruffleString replaceString = null;
            if (!functionalReplaceProfile.profile(functionalReplace)) {
                replaceString = toString3Node.executeString(replParam);
            }
            int pos = indexOf(input, searchString);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                return input;
            }
            TruffleStringBuilder sb = Strings.builderCreate();
            append(sb, input, 0, pos);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, pos, input});
                append(sb, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(sb, input, replaceString, searchString, pos, dollarProfile, this);
            }
            append(sb, input, pos + Strings.length(searchString), Strings.length(input));
            return builderToString(sb);
        }

        private Object builtinReplaceString(TruffleString searchString, TruffleString replaceString, Object o, ReplaceStringParser.Token[] parsedReplaceParam) {
            TruffleString input = toString(o);
            int pos = indexOf(input, searchString);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                return input;
            }
            TruffleStringBuilder sb = Strings.builderCreate();
            append(sb, input, 0, pos);
            if (parsedReplaceParam == null) {
                appendSubstitution(sb, input, replaceString, searchString, pos, dollarProfile, this);
            } else {
                ReplaceStringParser.processParsed(parsedReplaceParam, new ReplaceStringConsumer(sb, input, replaceString, searchString, pos), this);
            }
            append(sb, input, pos + Strings.length(searchString), Strings.length(input));
            return builderToString(sb);
        }

        private int indexOf(TruffleString s1, TruffleString s2) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfStringNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, s1, s2);
        }
    }

    public abstract static class JSStringReplaceAllNode extends JSStringReplaceBaseNode {
        private final ConditionProfile isSearchValueEmpty = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorBranch = BranchProfile.create();

        @Child private IsRegExpNode isRegExpNode;
        @Child private PropertyGetNode getFlagsNode;
        @Child private TruffleString.ByteIndexOfCodePointNode stringIndexOfNode;
        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfStringNode;

        public JSStringReplaceAllNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)")
        protected Object replaceStringCached(Object thisObj, TruffleString searchValue, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, TruffleString searchValue, TruffleString replaceValue) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, replaceValue, thisObj, null);
        }

        protected Object performReplaceAll(TruffleString searchValue, TruffleString replaceValue, Object thisObj, ReplaceStringParser.Token[] parsedReplaceParam) {
            TruffleString thisStr = toString(thisObj);
            if (isSearchValueEmpty.profile(Strings.isEmpty(searchValue))) {
                int len = Strings.length(thisStr);
                TruffleStringBuilder sb = Strings.builderCreate((len + 1) * Strings.length(replaceValue) + len);
                append(sb, replaceValue);
                for (int i = 0; i < len; i++) {
                    appendLen(sb, thisStr, i, 1);
                    append(sb, replaceValue);
                }
                return builderToString(sb);
            }
            TruffleStringBuilder sb = Strings.builderCreate();
            int position = 0;
            while (position < Strings.length(thisStr)) {
                int nextPosition = indexOf(thisStr, searchValue, position);
                builtinReplaceString(searchValue, replaceValue, thisStr, parsedReplaceParam, position, nextPosition, sb);
                if (nextPosition < 0) {
                    break;
                }
                position = nextPosition + Strings.length(searchValue);
            }
            return builderToString(sb);
        }

        @Specialization(guards = "!isStringString(searchValue, replaceValue)")
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(!(searchValue == Undefined.instance || searchValue == Null.instance))) {
                if (isRegExp.profile(getIsRegExpNode().executeBoolean(searchValue))) {
                    Object flags = getFlags(searchValue);
                    requireObjectCoercible(flags);
                    if (indexOf(toString(flags), 'g') == -1) {
                        errorBranch.enter();
                        throw Errors.createTypeError("Only global regexps allowed");
                    }
                }
                Object replacer = getMethod(searchValue, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(replacer != Undefined.instance)) {
                    return call(replacer, searchValue, new Object[]{thisObj, replaceValue});
                }
            }
            // all child nodes must be checked to avoid race conditions on shared ASTs
            if (toString2Node == null || toString3Node == null || isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
                toString3Node = insert(JSToStringNode.create());
                isCallableNode = insert(IsCallableNode.create());
            }
            return performReplaceAllGeneric(searchValue, replaceValue, thisObj);
        }

        protected Object performReplaceAllGeneric(Object searchValue, Object replParam, Object thisObj) {
            TruffleString thisStr = toString(thisObj);
            TruffleString searchString = toString2Node.executeString(searchValue);
            TruffleStringBuilder sb = Strings.builderCreate();
            int position = 0;

            boolean functionalReplace = isCallableNode.executeBoolean(replParam);
            Object replaceValue;
            if (functionalReplaceProfile.profile(functionalReplace)) {
                replaceValue = replParam;
            } else {
                replaceValue = toString3Node.executeString(replParam);
            }
            if (isSearchValueEmpty.profile(Strings.isEmpty(searchString))) {
                while (position <= Strings.length(thisStr)) {
                    builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, position, sb);
                    if (position < Strings.length(thisStr)) {
                        appendLen(sb, thisStr, position, 1);
                    }
                    ++position;
                }
                return builderToString(sb);
            }
            while (position < Strings.length(thisStr)) {
                int nextPosition = indexOf(thisStr, searchString, position);
                builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, nextPosition, sb);
                if (nextPosition < 0) {
                    break;
                }
                position = nextPosition + Strings.length(searchString);
            }
            return builderToString(sb);
        }

        private void builtinReplace(TruffleString searchString, boolean functionalReplace, Object replParam, TruffleString input, int lastPosition, int curPosition, TruffleStringBuilder sb) {
            if (replaceNecessaryProfile.profile(curPosition < 0)) {
                append(sb, input, lastPosition, Strings.length(input));
                return;
            }
            append(sb, input, lastPosition, curPosition);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, curPosition, input});
                append(sb, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(sb, input, (TruffleString) replParam, searchString, curPosition, dollarProfile, this);
            }
        }

        private void builtinReplaceString(TruffleString searchString, TruffleString replaceString, TruffleString input, ReplaceStringParser.Token[] parsedReplaceParam,
                        int lastPosition, int curPosition,
                        TruffleStringBuilder sb) {
            if (replaceNecessaryProfile.profile(curPosition < 0)) {
                append(sb, input, lastPosition, Strings.length(input));
                return;
            }
            append(sb, input, lastPosition, curPosition);
            if (parsedReplaceParam == null) {
                appendSubstitution(sb, input, replaceString, searchString, curPosition, dollarProfile, this);
            } else {
                ReplaceStringParser.processParsed(parsedReplaceParam, new ReplaceStringConsumer(sb, input, replaceString, searchString, curPosition), this);
            }
        }

        private IsRegExpNode getIsRegExpNode() {
            if (isRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isRegExpNode = insert(IsRegExpNode.create(getContext()));
            }
            return isRegExpNode;
        }

        private Object getFlags(Object regexp) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, getContext()));
            }
            return getFlagsNode.getValue(regexp);
        }

        private int indexOf(TruffleString a, int codepoint) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfCodePointNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, a, codepoint);
        }

        private int indexOf(TruffleString s1, TruffleString s2, int fromIndex) {
            if (stringIndexOfStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfStringNode = insert(TruffleString.ByteIndexOfStringNode.create());
            }
            return Strings.indexOf(stringIndexOfStringNode, s1, s2, fromIndex);
        }
    }

    /**
     * Implementation of the String.prototype.replace() method as specified by ECMAScript 5.1 in
     * 15.5.4.11.
     */
    public abstract static class JSStringReplaceES5Node extends JSStringReplaceBaseNode {
        @Child private PropertySetNode setLastIndexNode;
        @Child private StringReplacer stringReplacerNode;
        @Child private FunctionReplacer functionReplacerNode;
        @Child private TRegexUtil.TRegexCompiledRegexSingleFlagAccessor globalFlagAccessor = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(TRegexUtil.Props.Flags.GLOBAL);
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfNode;
        private final ConditionProfile match = ConditionProfile.createCountingProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createCountingProfile();
        private final ConditionProfile isFnRepl = ConditionProfile.createCountingProfile();

        public JSStringReplaceES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
        }

        @Specialization
        protected Object replace(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (Strings.length(thisStr) > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            if (isRegExp.profile(JSRegExp.isJSRegExp(searchValue))) {
                JSRegExpObject searchRegExp = (JSRegExpObject) searchValue;
                Object tRegexCompiledRegex = JSRegExp.getCompiledRegex(searchRegExp);
                int groupCount = compiledRegexAccessor.groupCount(tRegexCompiledRegex);
                if (isFnRepl.profile(JSFunction.isJSFunction(replaceValue))) {
                    JSDynamicObject replaceFunc = (JSDynamicObject) replaceValue;
                    if (globalFlagAccessor.get(tRegexCompiledRegex)) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getFunctionReplacerNode(), replaceFunc, tRegexCompiledRegex);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, getFunctionReplacerNode(), replaceFunc, tRegexCompiledRegex);
                    }
                } else {
                    TruffleString replaceStr = toString3(replaceValue);
                    if (globalFlagAccessor.get(tRegexCompiledRegex)) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getStringReplacerNode(), replaceStr, tRegexCompiledRegex);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, getStringReplacerNode(), replaceStr, tRegexCompiledRegex);
                    }
                }
            } else {
                TruffleString searchStr = toString2(searchValue);
                if (isFnRepl.profile(JSFunction.isJSFunction(replaceValue))) {
                    return replaceFirst(thisStr, searchStr, getFunctionReplacerNode(), (JSDynamicObject) replaceValue, null);
                } else {
                    TruffleString replaceStr = toString3(replaceValue);
                    return replaceFirst(thisStr, searchStr, getStringReplacerNode(), replaceStr, null);
                }
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

        private void setLastIndex(JSDynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            setLastIndexNode.setValueInt(regExp, value);
        }

        private StringReplacer getStringReplacerNode() {
            if (stringReplacerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringReplacerNode = insert(StringReplacer.create(this));
            }
            return stringReplacerNode;
        }

        private FunctionReplacer getFunctionReplacerNode() {
            if (functionReplacerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionReplacerNode = insert(FunctionReplacer.create(this));
            }
            return functionReplacerNode;
        }

        private int indexOf(TruffleString s1, TruffleString s2) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfStringNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, s1, s2);
        }

        private <T> Object replaceFirst(TruffleString thisStr, TruffleString searchStr, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex) {
            int start = indexOf(thisStr, searchStr);
            if (match.profile(start < 0)) {
                return thisStr;
            }
            int end = start + Strings.length(searchStr);
            TruffleStringBuilder sb = Strings.builderCreate();
            append(sb, thisStr, 0, start);
            replacer.appendReplacementString(sb, thisStr, searchStr, start, replaceValue, this, tRegexCompiledRegex);
            append(sb, thisStr, end, Strings.length(thisStr));
            return builderToString(sb);
        }

        private <T> TruffleString replaceFirst(TruffleString thisStr, JSRegExpObject regExp, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex) {
            Object result = match(regExp, thisStr);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return thisStr;
            }
            return replace(thisStr, result, compiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(regExp)), replacer, replaceValue, tRegexCompiledRegex);
        }

        protected final Object match(JSRegExpObject regExp, TruffleString input) {
            assert getContext().getEcmaScriptVersion() <= 5;
            return getRegExpNode().execute(regExp, input);
        }

        private <T> TruffleString replace(TruffleString thisStr, Object result, int groupCount, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex) {
            TruffleStringBuilder sb = Strings.builderCreate();
            append(sb, thisStr, 0, resultAccessor.captureGroupStart(result, 0));
            replacer.appendReplacementRegex(sb, thisStr, result, groupCount, replaceValue, this, tRegexCompiledRegex);
            append(sb, thisStr, resultAccessor.captureGroupEnd(result, 0), Strings.length(thisStr));
            return builderToString(sb);
        }

        private <T> TruffleString replaceAll(JSDynamicObject regExp, TruffleString input, int groupCount, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return input;
            }
            TruffleStringBuilder sb = Strings.builderCreate();
            int thisIndex = 0;
            int lastIndex = 0;
            while (resultAccessor.isMatch(result)) {
                append(sb, input, thisIndex, resultAccessor.captureGroupStart(result, 0));
                replacer.appendReplacementRegex(sb, input, result, groupCount, replaceValue, this, tRegexCompiledRegex);
                if (Strings.builderLength(sb) > getContext().getStringLengthLimit()) {
                    CompilerDirectives.transferToInterpreter();
                    throw Errors.createRangeErrorInvalidStringLength();
                }
                thisIndex = resultAccessor.captureGroupEnd(result, 0);
                if (thisIndex == Strings.length(input) && resultAccessor.captureGroupLength(result, 0) == 0) {
                    // Avoid getting empty match at end of string twice.
                    break;
                }
                lastIndex = thisIndex + (thisIndex == lastIndex ? 1 : 0);
                result = matchIgnoreLastIndex(regExp, input, lastIndex);
            }
            append(sb, input, thisIndex, Strings.length(input));
            return builderToString(sb);
        }

        private abstract static class Replacer<T> extends JavaScriptBaseNode {

            final JSStringReplaceES5Node parentNode;
            @Child TRegexUtil.TRegexMaterializeResultNode resultMaterializer = TRegexUtil.TRegexMaterializeResultNode.create();
            protected final ConditionProfile emptyReplace = ConditionProfile.createBinaryProfile();

            protected Replacer(JSStringReplaceES5Node parent) {
                this.parentNode = parent;
            }

            abstract void appendReplacementRegex(TruffleStringBuilder sb, TruffleString input, Object result, int groupCount, T replaceValue, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex);

            abstract void appendReplacementString(TruffleStringBuilder sb, TruffleString input, TruffleString matchedString, int pos, T replaceValue, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex);
        }

        protected static final class StringReplacer extends Replacer<TruffleString> implements RegExpPrototypeBuiltins.ReplaceStringConsumerTRegex.ParentNode {

            private final BranchProfile dollarProfile = BranchProfile.create();
            private final BranchProfile invalidGroupNumberProfile = BranchProfile.create();

            private StringReplacer(JSStringReplaceES5Node parent) {
                super(parent);
            }

            public static StringReplacer create(JSStringReplaceES5Node parent) {
                return new StringReplacer(parent);
            }

            @Override
            void appendReplacementRegex(TruffleStringBuilder sb, TruffleString input, Object result, int groupCount, TruffleString replaceStr, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex) {
                if (emptyReplace.profile(!Strings.isEmpty(replaceStr))) {
                    ReplaceStringParser.process(parent.getContext(), replaceStr, groupCount, false, dollarProfile, new RegExpPrototypeBuiltins.ReplaceStringConsumerTRegex(
                                    sb, input, replaceStr, parent.resultAccessor.captureGroupStart(result, 0), parent.resultAccessor.captureGroupEnd(result, 0), result, tRegexCompiledRegex), this);
                }
            }

            @Override
            void appendReplacementString(TruffleStringBuilder sb, TruffleString input, TruffleString matchedString, int pos, TruffleString replaceValue, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex) {
                JSStringReplaceNode.appendSubstitution(sb, input, replaceValue, matchedString, pos, dollarProfile, parent);
            }

            @Override
            public TRegexCompiledRegexAccessor getCompiledRegexAccessor() {
                return parentNode.compiledRegexAccessor;
            }

            @Override
            public TRegexResultAccessor getResultAccessor() {
                return parentNode.resultAccessor;
            }

            @Override
            public TRegexNamedCaptureGroupsAccessor getNamedCaptureGroupsAccessor() {
                throw Errors.shouldNotReachHere();
            }

            @Override
            public void append(TruffleStringBuilder sb, TruffleString s) {
                parentNode.append(sb, s);
            }

            @Override
            public void append(TruffleStringBuilder sb, TruffleString s, int fromIndex, int toIndex) {
                parentNode.append(sb, s, fromIndex, toIndex);
            }

            @Override
            public BranchProfile getInvalidGroupNumberProfile() {
                return invalidGroupNumberProfile;
            }
        }

        protected static final class FunctionReplacer extends Replacer<JSDynamicObject> {
            @Child private JSFunctionCallNode functionCallNode = JSFunctionCallNode.createCall();
            @Child private JSToStringNode toStringNode = JSToStringNode.create();

            private FunctionReplacer(JSStringReplaceES5Node parent) {
                super(parent);
            }

            public static FunctionReplacer create(JSStringReplaceES5Node parent) {
                return new FunctionReplacer(parent);
            }

            @Override
            void appendReplacementRegex(TruffleStringBuilder sb, TruffleString input, Object result, int groupCount, JSDynamicObject replaceFunc, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex) {
                parent.append(sb, callReplaceValueFunc(parent.getContext(), result, input, groupCount, replaceFunc));
            }

            @Override
            void appendReplacementString(TruffleStringBuilder sb, TruffleString input, TruffleString matchedString, int pos, JSDynamicObject replaceFunc, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex) {
                Object[] arguments = createArguments(new Object[]{matchedString}, pos, input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                TruffleString replaceStr = toStringNode.executeString(replaceValue);
                parent.append(sb, replaceStr);
            }

            private TruffleString callReplaceValueFunc(JSContext context, Object result, TruffleString input, int groupCount, JSDynamicObject replaceFunc) {
                Object[] matches = resultMaterializer.materializeFull(context, result, groupCount, input);
                Object[] arguments = createArguments(matches, parentNode.resultAccessor.captureGroupStart(result, 0), input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                return toStringNode.executeString(replaceValue);
            }

            private static Object[] createArguments(Object[] matches, int matchIndex, Object input, JSDynamicObject replaceFunc) {
                JSDynamicObject target = Undefined.instance;
                Object[] arguments = JSArguments.createInitial(target, replaceFunc, matches.length + 2);
                JSArguments.setUserArguments(arguments, 0, matches);
                JSArguments.setUserArgument(arguments, matches.length, matchIndex);
                JSArguments.setUserArgument(arguments, matches.length + 1, input);
                return arguments;
            }
        }
    }

    /**
     * Implementation of the String.prototype.toString() and String.prototype.valueOf() methods as
     * specified by ECMAScript 5.1 in 15.5.4.2 and 15.5.4.3.
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSStringToStringNode extends JSBuiltinNode {
        public JSStringToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static JSStringToStringNode createStringToString(JSContext context) {
            return JSStringToStringNodeGen.create(context, null, null);
        }

        @Specialization
        protected TruffleString toStringTString(TruffleString thisStr) {
            return thisStr;
        }

        @Specialization(guards = "isJSString(thisStr)")
        protected TruffleString toStringString(JSDynamicObject thisStr) {
            return JSString.getString(thisStr);
        }

        @Specialization(guards = "isForeignObject(thisObj)", limit = "InteropLibraryLimit")
        protected TruffleString toStringForeignObject(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            if (interop.isString(thisObj)) {
                try {
                    return interop.asTruffleString(thisObj);
                } catch (UnsupportedMessageException ex) {
                    throw Errors.createTypeErrorUnboxException(thisObj, ex, this);
                }
            }
            return toStringOther(thisObj);
        }

        @Fallback
        protected TruffleString toStringOther(@SuppressWarnings("unused") Object thisObj) {
            // unlike other String.prototype.[function]s, toString is NOT generic
            throw Errors.createTypeError("string object expected");
        }
    }

    /**
     * Implementation of the String.prototype.toLowerCase() method as specified by ECMAScript 5.1 in
     * 15.5.4.16.
     */
    public abstract static class JSStringToLowerCaseNode extends JSStringOperation {
        private final boolean locale;

        public JSStringToLowerCaseNode(JSContext context, JSBuiltin builtin, boolean locale) {
            super(context, builtin);
            this.locale = locale;
        }

        @Specialization
        protected Object toLowerCaseString(TruffleString thisStr) {
            return toLowerCaseIntl(thisStr);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected Object toLowerCase(Object thisObj) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            return toLowerCaseIntl(thisStr);
        }

        private Object toLowerCaseIntl(TruffleString str) {
            return Strings.toLowerCase(str, locale ? getContext().getLocale() : Locale.US);
        }
    }

    public abstract static class JSStringToLocaleXCaseIntl extends JSStringOperation {

        @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;

        public JSStringToLocaleXCaseIntl(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        }

        @Specialization
        protected Object toDesiredCase(Object thisObj, Object locale) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (thisStr == null || Strings.isEmpty(thisStr)) {
                return thisStr;
            }
            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(locale);
            return toXCase(Strings.toJavaString(thisStr), locales);
        }

        @SuppressWarnings("unused")
        protected TruffleString toXCase(String thisStr, String[] locales) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Implementation of the String.prototype.toLocaleLowerCase() method as specified by ECMAScript
     * Internationalization API, 1.0.
     * https://tc39.github.io/ecma402/#sup-string.prototype.tolocalelowercase
     */
    public abstract static class JSStringToLocaleLowerCaseIntlNode extends JSStringToLocaleXCaseIntl {

        public JSStringToLocaleLowerCaseIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Override
        protected TruffleString toXCase(String thisStr, String[] locales) {
            return IntlUtil.toLowerCase(getContext(), thisStr, locales);
        }
    }

    /**
     * Implementation of the String.prototype.toLocaleUpperCase() method as specified by ECMAScript
     * Internationalization API, 1.0.
     * https://tc39.github.io/ecma402/#sup-string.prototype.tolocaleuppercase
     */
    public abstract static class JSStringToLocaleUpperCaseIntlNode extends JSStringToLocaleXCaseIntl {

        public JSStringToLocaleUpperCaseIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Override
        protected TruffleString toXCase(String thisStr, String[] locales) {
            return IntlUtil.toUpperCase(getContext(), thisStr, locales);
        }
    }

    /**
     * Implementation of the String.prototype.toUpperCase() method as specified by ECMAScript 5.1 in
     * 15.5.4.18.
     */
    public abstract static class JSStringToUpperCaseNode extends JSStringOperation {
        private final boolean locale;

        public JSStringToUpperCaseNode(JSContext context, JSBuiltin builtin, boolean locale) {
            super(context, builtin);
            this.locale = locale;
        }

        @Specialization
        protected Object toUpperCaseString(TruffleString thisStr) {
            return toUpperCaseIntl(thisStr);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected Object toUpperCaseGeneric(Object thisObj) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            return toUpperCaseIntl(thisStr);
        }

        private Object toUpperCaseIntl(TruffleString str) {
            return Strings.toUpperCase(str, locale ? getContext().getLocale() : Locale.US);
        }
    }

    /**
     * Implementation of the String.prototype.search() method as specified by ECMAScript 6 in
     * 21.1.3.15.
     */
    public abstract static class JSStringSearchNode extends JSStringOperationWithRegExpArgument {
        @Child private CompileRegexNode compileRegexNode;
        @Child private CreateRegExpNode createRegExpNode;

        public JSStringSearchNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object search(Object thisObj, Object regex) {
            assert getContext().getEcmaScriptVersion() >= 6;
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(!(regex == Undefined.instance || regex == Null.instance))) {
                Object searcher = getMethod(regex, Symbol.SYMBOL_SEARCH);
                if (callSpecialProfile.profile(searcher != Undefined.instance)) {
                    return call(searcher, regex, new Object[]{thisObj});
                }
            }
            return builtinSearch(thisObj, regex);
        }

        private Object builtinSearch(Object thisObj, Object regex) {
            TruffleString thisStr = toString(thisObj);
            Object cRe = getCompileRegexNode().compile(regex == Undefined.instance ? Strings.EMPTY_STRING : toString(regex));
            JSDynamicObject regExp = getCreateRegExpNode().createRegExp(cRe);
            return invoke(regExp, Symbol.SYMBOL_SEARCH, thisStr);
        }

        private CompileRegexNode getCompileRegexNode() {
            if (compileRegexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compileRegexNode = insert(CompileRegexNode.create(getContext()));
            }
            return compileRegexNode;
        }

        private CreateRegExpNode getCreateRegExpNode() {
            if (createRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createRegExpNode = insert(CreateRegExpNode.create(getContext()));
            }
            return createRegExpNode;
        }
    }

    /**
     * Legacy Implementation of the String.prototype.search() method as specified by ECMAScript 5.
     */
    public abstract static class JSStringSearchES5Node extends JSStringOperationWithRegExpArgument {

        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();

        public JSStringSearchES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int search(Object thisObj, Object[] args,
                        @Cached("create(getContext())") JSToRegExpNode toRegExpNode) {
            assert getContext().getEcmaScriptVersion() < 6;
            Object searchObj = JSRuntime.getArgOrUndefined(args, 0);
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            JSDynamicObject regExp = toRegExpNode.execute(searchObj);
            Object result = matchIgnoreLastIndex(regExp, thisStr, 0);
            return resultAccessor.isMatch(result) ? resultAccessor.captureGroupStart(result, 0) : -1;
        }
    }

    /**
     * Implementation of the String.prototype.substr() method as specified by ECMAScript 5.1 in
     * Annex B.2.3.
     */
    public abstract static class JSStringSubstrNode extends JSStringOperation {

        private final BranchProfile startNegativeBranch = BranchProfile.create();
        private final BranchProfile finalLenEmptyBranch = BranchProfile.create();

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringSubstrNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object substrInt(TruffleString thisStr, int start, int length) {
            return substrIntl(thisStr, start, length);
        }

        @Specialization(guards = "isUndefined(length)")
        protected Object substrLenUndef(TruffleString thisStr, int start, @SuppressWarnings("unused") Object length) {
            return substrIntl(thisStr, start, Strings.length(thisStr));
        }

        @Specialization
        protected Object substrGeneric(Object thisObj, Object start, Object length) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int startInt = toIntegerAsInt(start);
            int len = (length == Undefined.instance) ? Strings.length(thisStr) : toIntegerAsInt(length);
            return substrIntl(thisStr, startInt, len);
        }

        private Object substrIntl(TruffleString thisStr, int start, int length) {
            int startInt = start;
            if (startInt < 0) {
                startNegativeBranch.enter();
                startInt = Math.max(startInt + Strings.length(thisStr), 0);
            }
            int finalLen = within(length, 0, Math.max(0, Strings.length(thisStr) - startInt));
            if (finalLen <= 0) {
                finalLenEmptyBranch.enter();
                return Strings.EMPTY_STRING;
            }
            return Strings.substring(getContext(), substringNode, thisStr, startInt, startInt + finalLen - startInt);
        }
    }

    /**
     * Implementation of the String.prototype.match() method as specified by ECMAScript 6 in
     * 21.1.3.11 and the String.prototype.matchAll() method as specified by the
     * String.prototype.matchAll draft proposal.
     */
    public abstract static class JSStringMatchNode extends JSStringOperationWithRegExpArgument {
        @Child private CompileRegexNode compileRegexNode;
        @Child private CreateRegExpNode createRegExpNode;
        @Child private IsRegExpNode isRegExpNode;
        @Child private PropertyGetNode getFlagsNode;
        @Child private TruffleString.ByteIndexOfCodePointNode stringIndexOfNode;
        private final BranchProfile errorBranch;
        private final boolean matchAll;

        protected JSStringMatchNode(JSContext context, JSBuiltin builtin, boolean matchAll) {
            super(context, builtin);
            this.matchAll = matchAll;
            this.errorBranch = matchAll ? BranchProfile.create() : null;
        }

        @Specialization
        protected Object match(Object thisObj, Object regex) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(!(regex == Undefined.instance || regex == Null.instance))) {
                if (matchAll && getIsRegExpNode().executeBoolean(regex)) {
                    Object flags = getFlags(regex);
                    requireObjectCoercible(flags);
                    if (indexOf(toString(flags), 'g') == -1) {
                        errorBranch.enter();
                        throw Errors.createTypeError("Regular expression passed to matchAll() is missing 'g' flag.");
                    }
                }
                Object matcher = getMethod(regex, matchSymbol());
                if (callSpecialProfile.profile(matcher != Undefined.instance)) {
                    return call(matcher, regex, new Object[]{thisObj});
                }
            }
            return builtinMatch(thisObj, regex);
        }

        private Symbol matchSymbol() {
            return matchAll ? Symbol.SYMBOL_MATCH_ALL : Symbol.SYMBOL_MATCH;
        }

        private Object builtinMatch(Object thisObj, Object regex) {
            TruffleString thisStr = toString(thisObj);
            Object cRe = getCompileRegexNode().compile(regex == Undefined.instance ? Strings.EMPTY_STRING : toString(regex), matchAll ? Strings.G : Strings.EMPTY_STRING);
            JSDynamicObject regExp = getCreateRegExpNode().createRegExp(cRe);
            return invoke(regExp, matchSymbol(), thisStr);
        }

        private CompileRegexNode getCompileRegexNode() {
            if (compileRegexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compileRegexNode = insert(CompileRegexNode.create(getContext()));
            }
            return compileRegexNode;
        }

        private CreateRegExpNode getCreateRegExpNode() {
            if (createRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createRegExpNode = insert(CreateRegExpNode.create(getContext()));
            }
            return createRegExpNode;
        }

        private IsRegExpNode getIsRegExpNode() {
            if (isRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isRegExpNode = insert(IsRegExpNode.create(getContext()));
            }
            return isRegExpNode;
        }

        private Object getFlags(Object regexp) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, getContext()));
            }
            return getFlagsNode.getValue(regexp);
        }

        private int indexOf(TruffleString a, int codepoint) {
            if (stringIndexOfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringIndexOfNode = insert(TruffleString.ByteIndexOfCodePointNode.create());
            }
            return Strings.indexOf(stringIndexOfNode, a, codepoint);
        }
    }

    /**
     * Implementation of the String.prototype.match() method as specified by ECMAScript 5.1 in
     * 15.5.4.19.
     */
    public abstract static class JSStringMatchES5Node extends JSStringOperationWithRegExpArgument {
        @Child private PropertySetNode setLastIndexNode;
        @Child private JSToRegExpNode toRegExpNode;
        @Child private JSRegExpExecES5Node regExpExecNode;
        @Child private TRegexUtil.TRegexCompiledRegexSingleFlagAccessor globalFlagAccessor = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(TRegexUtil.Props.Flags.GLOBAL);
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private TRegexUtil.TRegexMaterializeResultNode resultMaterializer = TRegexUtil.TRegexMaterializeResultNode.create();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();
        private final ConditionProfile isGlobalRegExp = ConditionProfile.createCountingProfile();

        public JSStringMatchES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
            toRegExpNode = JSToRegExpNode.create(context);
            this.regExpExecNode = JSRegExpExecES5NodeGen.create(context, null, null);

        }

        private void setLastIndex(JSDynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            setLastIndexNode.setValue(regExp, value);
        }

        @Specialization
        protected JSDynamicObject matchRegExpNotGlobal(Object thisObj, Object searchObj) {
            requireObjectCoercible(thisObj);
            if (isGlobalRegExp.profile(JSRegExp.isJSRegExp(searchObj) && globalFlagAccessor.get(JSRegExp.getCompiledRegex((JSDynamicObject) searchObj)))) {
                TruffleString thisStr = toString(thisObj);
                return matchAll((JSDynamicObject) searchObj, thisStr);
            } else {
                return matchNotRegExpIntl(thisObj, searchObj);
            }
        }

        private JSDynamicObject matchNotRegExpIntl(Object thisObj, Object searchObj) {
            Object thisStr = toString(thisObj);
            JSRegExpObject regExp = toRegExpNode.execute(searchObj);
            return regExpExecNode.exec(regExp, thisStr);
        }

        private JSDynamicObject matchAll(JSDynamicObject regExp, TruffleString input) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return Null.instance;
            }
            List<Object> matches = new ArrayList<>();
            int lastIndex = 0;
            while (resultAccessor.isMatch(result)) {
                Boundaries.listAdd(matches, resultMaterializer.materializeGroup(getContext(), result, 0, input));

                int thisIndex = resultAccessor.captureGroupEnd(result, 0);
                lastIndex = thisIndex + (thisIndex == lastIndex ? 1 : 0);
                result = matchIgnoreLastIndex(regExp, input, lastIndex);
            }
            return JSArray.createConstant(getContext(), getRealm(), Boundaries.listToArray(matches));
        }

    }

    /**
     * Implementation of the String.prototype.trim() method as specified by ECMAScript 5.1 in
     * 15.5.4.20.
     */
    public abstract static class JSStringTrimNode extends JSStringOperation {

        public JSStringTrimNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object trimString(TruffleString thisStr,
                        @Shared("trimWhitespace") @Cached JSTrimWhitespaceNode trimWhitespaceNode) {
            return trimWhitespaceNode.executeString(thisStr);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected Object trimObject(Object thisObj,
                        @Shared("trimWhitespace") @Cached JSTrimWhitespaceNode trimWhitespaceNode) {
            requireObjectCoercible(thisObj);
            return trimWhitespaceNode.executeString(toString(thisObj));
        }
    }

    /**
     * Non-standard String.prototype.trimLeft to provide compatibility with Nashorn and V8.
     */
    public abstract static class JSStringTrimLeftNode extends JSStringOperation {
        private final ConditionProfile lengthExceeded = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lengthZero = ConditionProfile.createBinaryProfile();

        public JSStringTrimLeftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object trimLeft(Object thisObj,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TruffleString.ReadCharUTF16Node readRawNode) {
            requireObjectCoercible(thisObj);
            TruffleString string = toString(thisObj);

            int firstIdx = JSRuntime.firstNonWhitespaceIndex(string, true, readRawNode);
            if (lengthZero.profile(firstIdx == 0)) {
                return string;
            } else if (lengthExceeded.profile(firstIdx >= Strings.length(string))) {
                return Strings.EMPTY_STRING;
            } else {
                return Strings.substring(getContext(), substringNode, string, firstIdx);
            }
        }
    }

    /**
     * Non-standard String.prototype.trimRight to provide compatibility with Nashorn and V8.
     */
    public abstract static class JSStringTrimRightNode extends JSStringOperation {

        private final ConditionProfile lengthExceeded = ConditionProfile.createBinaryProfile();

        public JSStringTrimRightNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object trimRight(Object thisObj,
                        @Cached TruffleString.ReadCharUTF16Node readRawNode,
                        @Cached TruffleString.SubstringByteIndexNode substringNode) {
            requireObjectCoercible(thisObj);
            TruffleString string = toString(thisObj);

            int lastIdx = JSRuntime.lastNonWhitespaceIndex(string, true, readRawNode);
            if (lengthExceeded.profile(lastIdx >= Strings.length(string))) {
                return string;
            } else {
                return Strings.substring(getContext(), substringNode, string, 0, lastIdx + 1);
            }
        }
    }

    /**
     * Implementation of the String.prototype.localeCompare() method as specified by ECMAScript 5.1
     * in 15.5.4.9.
     */
    public abstract static class JSStringLocaleCompareNode extends JSStringOperation {

        public JSStringLocaleCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private static Collator collator;

        @TruffleBoundary
        private static Collator getCollator() {
            if (collator == null) {
                collator = Collator.getInstance(Locale.ROOT);
                collator.setStrength(Collator.TERTIARY);
                collator.setDecomposition(Collator.FULL_DECOMPOSITION);
            }
            return collator;
        }

        @Specialization
        protected int localeCompare(Object thisObj, Object thatObj,
                        @Cached JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString thatStr = toString2Node.executeString(thatObj);
            return doLocaleCompare(thisStr, thatStr);
        }

        @TruffleBoundary
        private static int doLocaleCompare(TruffleString thisStr, TruffleString thatStr) {
            return getCollator().compare(Strings.toJavaString(thisStr), Strings.toJavaString(thatStr));
        }
    }

    /**
     * Implementation of the String.prototype.localeCompare() method as specified by ECMAScript
     * Internationalization API, 1.0. http://ecma-international.org/ecma-402/1.0/#sec-13.1.1
     */
    public abstract static class JSStringLocaleCompareIntlNode extends JSStringOperation {

        @Child InitializeCollatorNode initCollatorNode;

        public JSStringLocaleCompareIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initCollatorNode = InitializeCollatorNode.createInitalizeCollatorNode(context);
        }

        @TruffleBoundary
        private JSDynamicObject createCollator(Object locales, Object options) {
            JSDynamicObject collatorObj = JSCollator.create(getContext(), getRealm());
            initCollatorNode.executeInit(collatorObj, locales, options);
            return collatorObj;
        }

        @Specialization
        protected int localeCompare(Object thisObj, Object thatObj, Object locales, Object options,
                        @Cached JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString thatStr = toString2Node.executeString(thatObj);
            JSDynamicObject collator = createCollator(locales, options);
            return JSCollator.compare(collator, Strings.toJavaString(thisStr), Strings.toJavaString(thatStr));
        }
    }

    /**
     * Implementation of the String.prototype.slice() method as specified by ECMAScript 5.1 in
     * 15.5.4.13.
     */
    public abstract static class JSStringSliceNode extends JSStringOperation {
        private final ConditionProfile canReturnEmpty = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile2 = ConditionProfile.createBinaryProfile();

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object sliceStringIntInt(TruffleString thisObj, int start, int end) {
            int len = Strings.length(thisObj);
            int istart = JSRuntime.getOffset(start, len, offsetProfile1);
            int iend = JSRuntime.getOffset(end, len, offsetProfile2);
            if (canReturnEmpty.profile(iend > istart)) {
                return Strings.substring(getContext(), substringNode, thisObj, istart, iend - istart);
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        @Specialization(guards = "!isString(thisObj)", replaces = "sliceStringIntInt")
        protected Object sliceObjectIntInt(Object thisObj, int start, int end) {
            requireObjectCoercible(thisObj);
            return sliceStringIntInt(toString(thisObj), start, end);
        }

        @Specialization(guards = "isUndefined(end)")
        protected Object sliceStringIntUndefined(TruffleString str, int start, @SuppressWarnings("unused") Object end) {
            int len = Strings.length(str);
            int istart = JSRuntime.getOffset(start, len, offsetProfile1);
            if (canReturnEmpty.profile(len > istart)) {
                return Strings.substring(getContext(), substringNode, str, istart, len - istart);
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        @Specialization(replaces = {"sliceStringIntInt", "sliceObjectIntInt", "sliceStringIntUndefined"})
        protected Object sliceGeneric(Object thisObj, Object start, Object end,
                        @Cached("createBinaryProfile()") ConditionProfile isUndefined) {
            requireObjectCoercible(thisObj);
            TruffleString s = toString(thisObj);

            long len = Strings.length(s);
            long istart = JSRuntime.getOffset(toIntegerAsInt(start), len, offsetProfile1);
            long iend = isUndefined.profile(end == Undefined.instance) ? len : JSRuntime.getOffset(toIntegerAsInt(end), len, offsetProfile2);
            if (canReturnEmpty.profile(iend > istart)) {
                int begin = (int) istart;
                return Strings.substring(getContext(), substringNode, s, begin, (int) iend - begin);
            } else {
                return Strings.EMPTY_STRING;
            }
        }
    }

    /**
     * Implementation of the String.prototype.startsWith() method of ECMAScript6/Harmony.
     */
    public abstract static class JSStringStartsWithNode extends JSStringOperation {

        public JSStringStartsWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile noStringBranch = BranchProfile.create();

        @Specialization(guards = "isUndefined(position)")
        protected boolean startsWithString(TruffleString thisObj, TruffleString searchStr, @SuppressWarnings("unused") JSDynamicObject position,
                        @Cached @Cached.Shared("regionEqualsNode") TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
            if (Strings.length(searchStr) <= 0) {
                return true;
            }
            if (Strings.length(thisObj) < Strings.length(searchStr)) {
                return false;
            }
            return Strings.startsWith(regionEqualsNode, thisObj, searchStr);
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected boolean startsWithGeneric(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode,
                        @Cached @Cached.Shared("regionEqualsNode") TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.startsWith must not be a regular expression");
            }
            TruffleString searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            if (Strings.length(searchStr) <= 0) {
                return true;
            }
            return Strings.startsWith(regionEqualsNode, thisStr, searchStr, Math.max(0, fromIndex));
        }
    }

    /**
     * Implementation of the String.prototype.endsWith() method of ECMAScript6/Harmony.
     */
    public abstract static class JSStringEndsWithNode extends JSStringOperation {

        public JSStringEndsWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile noStringBranch = BranchProfile.create();

        @Specialization(guards = "isUndefined(position)")
        protected boolean endsWithStringUndefined(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Cached.Shared("regionEqualsNode") TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
            int fromIndex = Strings.length(thisStr);
            if (Strings.length(searchStr) <= 0) {
                return true;
            }
            if (fromIndex >= Strings.length(thisStr)) {
                fromIndex = Strings.length(thisStr);
            } else if (fromIndex < 0) {
                return false;
            }
            return endsWithIntl(regionEqualsNode, thisStr, searchStr, fromIndex);
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected boolean endsWithGeneric(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode,
                        @Cached @Cached.Shared("regionEqualsNode") TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.endsWith must not be a regular expression");
            }
            TruffleString searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            if (Strings.length(searchStr) <= 0) {
                return true;
            }
            if (fromIndex >= Strings.length(thisStr) || position == Undefined.instance) {
                fromIndex = Strings.length(thisStr);
            } else if (fromIndex < 0) {
                return false;
            }
            return endsWithIntl(regionEqualsNode, thisStr, searchStr, fromIndex);
        }

        private static boolean endsWithIntl(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString thisStr, TruffleString searchStr, int fromIndex) {
            int searchStrLength = Strings.length(searchStr);
            int offset1 = fromIndex - searchStrLength;
            return offset1 >= 0 && Strings.regionEquals(regionEqualsNode, thisStr, offset1, searchStr, 0, searchStrLength);
        }
    }

    /**
     * Implementation of the String.prototype.contains() or .includes() method of
     * ECMAScript6/Harmony.
     */
    public abstract static class JSStringIncludesNode extends JSStringOperation {

        public JSStringIncludesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile noStringBranch = BranchProfile.create();

        @Specialization(guards = "isUndefined(position)")
        protected boolean includesString(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared("indexOfStringNode") TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr) != -1;
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected boolean includesGeneric(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode,
                        @Cached @Shared("indexOfStringNode") TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.includes must not be a regular expression");
            }
            TruffleString searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr, fromIndex) != -1;
        }
    }

    /**
     * Implementation of the String.prototype.repeat() method of ECMAScript6/Harmony.
     */
    public abstract static class JSStringRepeatNode extends JSStringOperation {
        private final BranchProfile errorBranch = BranchProfile.create();

        public JSStringRepeatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object repeat(Object thisObj, Object count,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached TruffleString.RepeatNode repeatNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            Number repeatCountN = toNumberNode.executeNumber(count);
            long repeatCount = JSRuntime.toInteger(repeatCountN);
            if (repeatCount < 0 || (repeatCountN instanceof Double && Double.isInfinite(repeatCountN.doubleValue()))) {
                errorBranch.enter();
                throw Errors.createRangeError("illegal repeat count");
            }
            if (repeatCount == 1) {
                return thisStr;
            } else if (repeatCount == 0 || Strings.length(thisStr) == 0) {
                // fast path for repeating an empty string an arbitrary number of times
                // or repeating a string 0 times
                return Strings.EMPTY_STRING;
            }
            int repeatCountInt = (int) repeatCount;
            if (repeatCountInt != repeatCount || repeatCount * Strings.length(thisStr) > getContext().getStringLengthLimit()) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            return repeatNode.execute(thisStr, repeatCountInt, TruffleString.Encoding.UTF_16);
        }
    }

    /**
     * Implementation of the String.prototype.codePointAt() method of ECMAScript6/Harmony.
     */
    public abstract static class JSStringCodePointAtNode extends JSStringOperation {

        public JSStringCodePointAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile undefinedBranch = BranchProfile.create();
        private final BranchProfile needSecondBranch = BranchProfile.create();
        private final BranchProfile needCalculationBranch = BranchProfile.create();

        @Specialization
        protected Object codePointAt(Object thisObj, Object position,
                        @Cached TruffleString.CodePointAtByteIndexNode codePointAtRawNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int pos = toIntegerAsInt(position);
            if (pos < 0 || Strings.length(thisStr) <= pos) {
                undefinedBranch.enter();
                return Undefined.instance;
            }
            int first = Strings.codePointAt(codePointAtRawNode, thisStr, pos);
            boolean isEnd = (pos + 1 == Strings.length(thisStr));
            if (isEnd || first < 0xD800 || first > 0xDBFF) {
                return first;
            }
            needSecondBranch.enter();
            int second = Strings.codePointAt(codePointAtRawNode, thisStr, pos + 1);
            if (second < 0xDC00 || second > 0xDFFF) {
                return first;
            }
            needCalculationBranch.enter();
            return ((first - 0xD800) * 1024) + (second - 0xDC00) + 0x10000;
        }
    }

    /**
     * Implementation of the String.prototype.normalize() method of ECMAScript6/Harmony.
     */
    public abstract static class JSStringNormalizeNode extends JSStringOperation {

        public JSStringNormalizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString normalize(Object thisObj, Object form,
                        @Cached TruffleString.EqualNode stringEqualsNode,
                        @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString formStr = toString(form);
            Normalizer.Form useForm = null;
            if (form == Undefined.instance || Strings.length(formStr) <= 0 || Strings.equals(stringEqualsNode, formStr, Strings.NFC)) {
                useForm = Normalizer.Form.NFC;
            } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFD)) {
                useForm = Normalizer.Form.NFD;
            } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFKC)) {
                useForm = Normalizer.Form.NFKC;
            } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFKD)) {
                useForm = Normalizer.Form.NFKD;
            } else {
                throw Errors.createRangeError("invalid form string");
            }
            return Strings.fromJavaString(fromJavaStringNode, doNormalize(Strings.toJavaString(toJavaStringNode, thisStr), useForm));
        }

        @TruffleBoundary
        private static String doNormalize(String thisStr, Normalizer.Form form) {
            return Normalizer.normalize(thisStr, form);
        }
    }

    /**
     * Implementation of the String.prototype.padStart() and padEnd() method as proposed for
     * ECMAScript 2016 (ES7).
     */
    public abstract static class JSStringPadNode extends JSStringOperation {
        private final boolean atStart;

        public JSStringPadNode(JSContext context, JSBuiltin builtin, boolean atStart) {
            super(context, builtin);
            this.atStart = atStart;
        }

        @Specialization
        protected Object pad(Object thisObj, Object[] args,
                        @Cached JSToStringNode toString2Node,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.AppendSubstringByteIndexNode appendSubStringNode,
                        @Cached TruffleStringBuilder.ToStringNode builderToStringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (args.length == 0) {
                return thisStr;
            }
            int len = toIntegerAsInt(args[0]);
            if (len <= Strings.length(thisStr)) {
                return thisStr;
            }
            TruffleString fillStr;
            if (args.length <= 1 || args[1] == Undefined.instance) {
                fillStr = Strings.SPACE;
            } else {
                fillStr = toString2Node.executeString(args[1]);
                if (Strings.isEmpty(fillStr)) {
                    return thisStr; // explicit empty string
                }
            }
            if (len > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            assert !Strings.isEmpty(fillStr);
            int pos = len - Strings.length(thisStr);
            int fillLen = Strings.length(fillStr);
            TruffleStringBuilder sb = Strings.builderCreate(len);
            if (!atStart) {
                Strings.builderAppend(appendStringNode, sb, thisStr);
            }
            while (pos >= fillLen) {
                Strings.builderAppend(appendStringNode, sb, fillStr);
                pos -= fillLen;
            }
            if (pos > 0) {
                Strings.builderAppend(appendSubStringNode, sb, fillStr, 0, pos);
            }
            if (atStart) {
                Strings.builderAppend(appendStringNode, sb, thisStr);
            }
            return Strings.builderToString(builderToStringNode, sb);
        }

    }

    /**
     * Implementation of the CreateRegExpStringIterator abstract operation as specified by the
     * String.prototype.matchAll draft proposal.
     */
    public static class CreateRegExpStringIteratorNode extends JavaScriptBaseNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setIteratingRegExpNode;
        @Child private PropertySetNode setIteratedStringNode;
        @Child private PropertySetNode setGlobalNode;
        @Child private PropertySetNode setUnicodeNode;
        @Child private PropertySetNode setDoneNode;

        public CreateRegExpStringIteratorNode(JSContext context) {
            // The CreateRegExpStringIteratorNode is used only in the MatchAllIteratorNode, where
            // it is lazily constructed just before its first execution. Furthermore, an execution
            // of the CreateRegExpStringIteratorNode necessitates the execution of all its children,
            // therefore there is nothing to gain by constructing the children of this node lazily.
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratingRegExpNode = PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_ITERATING_REGEXP_ID, context);
            this.setIteratedStringNode = PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_ITERATED_STRING_ID, context);
            this.setGlobalNode = PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_GLOBAL_ID, context);
            this.setUnicodeNode = PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_UNICODE_ID, context);
            this.setDoneNode = PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_DONE_ID, context);
        }

        public JSDynamicObject createIterator(Object regex, Object string, Boolean global, Boolean fullUnicode) {
            JSDynamicObject regExpStringIteratorPrototype = getRealm().getRegExpStringIteratorPrototype();
            JSDynamicObject iterator = createObjectNode.execute(regExpStringIteratorPrototype);
            setIteratingRegExpNode.setValue(iterator, regex);
            setIteratedStringNode.setValue(iterator, string);
            setGlobalNode.setValueBoolean(iterator, global);
            setUnicodeNode.setValueBoolean(iterator, fullUnicode);
            setDoneNode.setValueBoolean(iterator, false);
            return iterator;
        }
    }

    public abstract static class CreateStringIteratorNode extends JSBuiltinNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;

        public CreateStringIteratorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSString.ITERATED_STRING_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSString.STRING_ITERATOR_NEXT_INDEX_ID, context);
        }

        @Specialization(guards = "isString(thisObj)")
        protected JSDynamicObject doString(Object thisObj) {
            JSDynamicObject iterator = createObjectNode.execute(getRealm().getStringIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, thisObj);
            setNextIndexNode.setValueInt(iterator, 0);
            return iterator;
        }

        @Specialization(guards = "!isString(thisObj)")
        protected JSDynamicObject doCoerce(Object thisObj,
                        @Cached RequireObjectCoercibleNode requireObjectCoercibleNode,
                        @Cached JSToStringNode toStringNode) {
            return doString(toStringNode.executeString(requireObjectCoercibleNode.execute(thisObj)));
        }

    }

    static CreateHTMLNode createHTMLNode(JSContext context, JSBuiltin builtin, TruffleString tag, TruffleString attribute) {
        return CreateHTMLNodeGen.create(context, builtin, tag, attribute, args().withThis().fixedArgs(1).createArgumentNodes(context));
    }

    abstract static class CreateHTMLNode extends JSBuiltinNode {
        private final TruffleString tag;
        private final TruffleString attribute;
        private final boolean emptyAttr;

        CreateHTMLNode(JSContext context, JSBuiltin builtin, TruffleString tag, TruffleString attribute) {
            super(context, builtin);
            this.tag = tag;
            this.attribute = attribute;
            this.emptyAttr = Strings.isEmpty(attribute);
        }

        @Specialization
        protected Object createHTML(Object thisObj, Object value,
                        @Cached RequireObjectCoercibleNode requireObjectCoercibleNode,
                        @Cached JSToStringNode toStringNode) {
            TruffleString string = toStringNode.executeString(requireObjectCoercibleNode.execute(thisObj));
            if (!emptyAttr) {
                TruffleString attrVal = toStringNode.executeString(value);
                return wrapInTagWithAttribute(string, attrVal);
            }
            return wrapInTag(string);
        }

        @TruffleBoundary
        private Object wrapInTag(TruffleString string) {
            return Strings.concatAll(Strings.ANGLE_BRACKET_OPEN, tag, Strings.ANGLE_BRACKET_CLOSE, string, Strings.ANGLE_BRACKET_OPEN_SLASH, tag, Strings.ANGLE_BRACKET_CLOSE);
        }

        @TruffleBoundary
        private Object wrapInTagWithAttribute(TruffleString string, TruffleString attrVal) {
            TruffleString escapedVal = Strings.replace(attrVal, Strings.DOUBLE_QUOTE, Strings.HTML_QUOT);
            return Strings.concatAll(Strings.ANGLE_BRACKET_OPEN, tag, Strings.SPACE, attribute, Strings.EQUALS_DOUBLE_QUOTE, escapedVal, Strings.DOUBLE_QUOTE, Strings.ANGLE_BRACKET_CLOSE, string,
                            Strings.ANGLE_BRACKET_OPEN_SLASH, tag, Strings.ANGLE_BRACKET_CLOSE);
        }
    }

    public abstract static class JSStringAtNode extends JSStringOperation {
        public JSStringAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object at(Object thisObj, Object index,
                        @Cached TruffleString.SubstringByteIndexNode substringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int relativeIndex = toIntegerAsInt(index);
            int k = (relativeIndex >= 0) ? relativeIndex : Strings.length(thisStr) + relativeIndex;
            if (k < 0 || k >= Strings.length(thisStr)) {
                return Undefined.instance;
            }
            return Strings.substring(getContext(), substringNode, thisStr, k, 1);
        }
    }
}
