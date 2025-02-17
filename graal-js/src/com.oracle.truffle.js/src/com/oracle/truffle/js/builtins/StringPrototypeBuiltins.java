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

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleString.CodeRange;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltins.JSRegExpExecES5Node;
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
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringIsWellFormedNodeGen;
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
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToLocaleLowerOrUpperCaseNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToLowerCaseNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToStringNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToUpperCaseNodeGen;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltinsFactory.JSStringToWellFormedNodeGen;
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
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringIterator;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollatorObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadBooleanMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InvokeGetGroupBoundariesMethodNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexCompiledRegexAccessor;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResult;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

/**
 * Contains builtins for {@linkplain JSString}.prototype.
 */
public final class StringPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<StringPrototypeBuiltins.StringPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new StringPrototypeBuiltins();

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

        // ES2019 (and nashorn-compat mode)
        trimStart(0),
        trimEnd(0),

        // ES2020
        matchAll(1),

        // ES2021
        replaceAll(2),

        // ES2022
        at(1),

        // ES2024
        isWellFormed(0),
        toWellFormed(0);

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
            return switch (this) {
                case startsWith, endsWith, includes, repeat, codePointAt, _iterator, normalize -> JSConfig.ECMAScript2015;
                case padStart, padEnd -> JSConfig.ECMAScript2017;
                // Note: trimStart and trimEnd are manually added.
                case trimStart, trimEnd -> JSConfig.ECMAScript2019;
                case matchAll -> JSConfig.ECMAScript2020;
                case replaceAll -> JSConfig.ECMAScript2021;
                case at -> JSConfig.ECMAScript2022;
                case isWellFormed, toWellFormed -> JSConfig.ECMAScript2024;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }

        @Override
        public Object getKey() {
            return this == _iterator ? Symbol.SYMBOL_ITERATOR : BuiltinEnum.super.getKey();
        }

        @Override
        public boolean isOptional() {
            return this == trimStart || this == trimEnd;
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
                    return JSStringToLocaleLowerOrUpperCaseNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return JSStringToLowerCaseNodeGen.create(context, builtin, true, args().withThis().createArgumentNodes(context));
                }
            case toUpperCase:
                return JSStringToUpperCaseNodeGen.create(context, builtin, false, args().withThis().createArgumentNodes(context));
            case toLocaleUpperCase:
                if (context.isOptionIntl402()) {
                    return JSStringToLocaleLowerOrUpperCaseNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
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
            case trimStart:
                return JSStringTrimLeftNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case trimEnd:
                return JSStringTrimRightNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

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

            case isWellFormed:
                return JSStringIsWellFormedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toWellFormed:
                return JSStringToWellFormedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    abstract static class JSStringOperation extends JSBuiltinNode {
        JSStringOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode;
        @Child private JSToStringNode toStringNode;
        @Child private JSToIntegerAsIntNode toIntegerNode;

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

        public JSStringOperationWithRegExpArgument(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final Object matchIgnoreLastIndex(JSRegExpObject regExp, TruffleString input, int fromIndex) {
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

        protected final Object invoke(JSRegExpObject regExp, Symbol symbol, TruffleString thisStr) {
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

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringCharAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString stringCharAt(TruffleString thisObj, int pos,
                        @Shared @Cached InlinedConditionProfile indexOutOfBounds) {
            if (indexOutOfBounds.profile(this, pos < 0 || pos >= Strings.length(thisObj))) {
                return Strings.EMPTY_STRING;
            } else {
                return Strings.substring(getContext(), substringNode, thisObj, pos, 1);
            }
        }

        @Specialization
        protected TruffleString charAt(Object thisObj, Object index,
                        @Shared @Cached InlinedConditionProfile indexOutOfBounds) {
            requireObjectCoercible(thisObj);
            return stringCharAt(toString(thisObj), toIntegerAsInt(index), indexOutOfBounds);
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
            protected TruffleString charAt(Object thisObj, Object indexObj,
                            @Shared @Cached InlinedConditionProfile indexOutOfBounds) {
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

        public JSStringCharCodeAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static boolean posInBounds(TruffleString thisStr, int pos) {
            return pos >= 0 && pos < Strings.length(thisStr);
        }

        @Specialization(guards = {"posInBounds(thisStr, pos)"})
        protected int charCodeAtInBounds(TruffleString thisStr, int pos,
                        @Cached @Shared TruffleString.ReadCharUTF16Node readChar) {
            return Strings.charAt(readChar, thisStr, pos);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!posInBounds(thisStr, pos)")
        protected double charCodeAtOutOfBounds(TruffleString thisStr, int pos) {
            return Double.NaN;
        }

        @Specialization(replaces = {"charCodeAtInBounds", "charCodeAtOutOfBounds"})
        protected Object charCodeAtGeneric(Object thisObj, Object indexObj,
                        @Cached @Shared TruffleString.ReadCharUTF16Node readChar,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached InlinedConditionProfile indexOutOfBounds) {
            requireObjectCoercible(thisObj);
            TruffleString s = toString(thisObj);
            Number index = toNumberNode.executeNumber(indexObj);
            long lIndex = JSRuntime.toInteger(index);
            if (indexOutOfBounds.profile(this, 0 > lIndex || lIndex >= Strings.length(s))) {
                return Double.NaN;
            } else {
                return Integer.valueOf(Strings.charAt(readChar, s, (int) lIndex));
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
                            @Cached @Shared TruffleString.ReadCharUTF16Node readChar,
                            @Cached JSToNumberNode toNumberNode,
                            @Cached InlinedConditionProfile indexOutOfBounds) {
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

        public JSStringSubstringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString substring(TruffleString thisStr, int start, int end,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedConditionProfile startLowerEnd) {
            int len = Strings.length(thisStr);
            int finalStart = within(start, 0, len);
            int finalEnd = within(end, 0, len);
            return substringIntl(thisStr, finalStart, finalEnd, substringNode, startLowerEnd);
        }

        @Specialization(guards = "isUndefined(end)")
        protected TruffleString substringStart(TruffleString thisStr, int start, @SuppressWarnings("unused") Object end,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedConditionProfile startLowerEnd) {
            int len = Strings.length(thisStr);
            int finalStart = within(start, 0, len);
            int finalEnd = len;
            return substringIntl(thisStr, finalStart, finalEnd, substringNode, startLowerEnd);
        }

        private TruffleString substringIntl(TruffleString thisStr, int start, int end,
                        TruffleString.SubstringByteIndexNode substringNode,
                        InlinedConditionProfile startLowerEnd) {
            final int fromIndex;
            final int length;
            if (startLowerEnd.profile(this, start <= end)) {
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
                        @Cached @Exclusive InlinedConditionProfile startUndefined,
                        @Cached @Exclusive InlinedConditionProfile endUndefined,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedConditionProfile startLowerEnd) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int len = Strings.length(thisStr);
            int intStart;
            int intEnd;
            if (startUndefined.profile(this, start == Undefined.instance)) {
                intStart = 0;
            } else {
                intStart = withinNumber(toNumberNode.executeNumber(start), 0, len);
            }
            if (endUndefined.profile(this, end == Undefined.instance)) {
                intEnd = len;
            } else {
                intEnd = withinNumber(toNumber2Node.executeNumber(end), 0, len);
            }
            return substringIntl(thisStr, intStart, intEnd, substringNode, startLowerEnd);
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
                            @Cached @Exclusive InlinedConditionProfile startUndefined,
                            @Cached @Exclusive InlinedConditionProfile endUndefined,
                            @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                            @Cached @Shared InlinedConditionProfile startLowerEnd) {
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

        public JSStringIndexOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isUndefined(position)")
        protected int indexOfStringUndefined(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr);
        }

        @Specialization
        protected int indexOfStringInt(TruffleString thisStr, TruffleString searchStr, int position,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode indexOfStringNode,
                        @Cached @Shared InlinedConditionProfile hasPos) {
            return indexOfIntl(thisStr, searchStr, position, indexOfStringNode, hasPos);
        }

        // replace only the StringInt specialization that duplicates code
        @Specialization(guards = "!isStringString(thisObj, searchObj) || !isUndefined(position)", replaces = {"indexOfStringInt"})
        protected int indexOfGeneric(Object thisObj, Object searchObj, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode indexOfStringNode,
                        @Cached @Shared InlinedConditionProfile hasPos) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString searchStr = toString2Node.executeString(searchObj);
            return indexOfIntl(thisStr, searchStr, position, indexOfStringNode, hasPos);
        }

        private int indexOfIntl(TruffleString thisStr, TruffleString searchStr, Object position,
                        TruffleString.ByteIndexOfStringNode indexOfStringNode,
                        InlinedConditionProfile hasPos) {
            int startPos;
            if (hasPos.profile(this, position != Undefined.instance)) {
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
                        @Cached @Shared TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            return Strings.lastIndexOf(lastIndexOfNode, thisObj, searchString, Strings.length(thisObj));
        }

        @Specialization
        protected int lastIndexOfString(TruffleString thisObj, TruffleString searchString, int position,
                        @Cached @Shared TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            int len = Strings.length(thisObj);
            int pos = within(position, 0, len);
            return Strings.lastIndexOf(lastIndexOfNode, thisObj, searchString, pos);
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected int lastIndexOf(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached InlinedConditionProfile posNaN,
                        @Cached @Shared TruffleString.LastByteIndexOfStringNode lastIndexOfNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString searchStr = toString2Node.executeString(searchString);
            Number numPos = toNumberNode.executeNumber(position);
            int lastPos = Strings.length(thisStr);
            int pos;

            double dVal = JSRuntime.doubleValue(numPos);
            if (posNaN.profile(this, Double.isNaN(dVal))) {
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

        @Child private JSToUInt32Node toUInt32Node;
        @Child private JSToStringNode toString2Node;

        @Child private TruffleString.SubstringByteIndexNode substringNode;
        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfNode;

        @Child private InteropReadBooleanMemberNode readIsMatch = InteropReadBooleanMemberNode.create();
        @Child private InvokeGetGroupBoundariesMethodNode getStartNode = InvokeGetGroupBoundariesMethodNode.create();
        @Child private InvokeGetGroupBoundariesMethodNode getEndNode = InvokeGetGroupBoundariesMethodNode.create();

        private int toUInt32(Object target) {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return (int) Math.min(Integer.MAX_VALUE, JSRuntime.toInteger(toUInt32Node.executeNumber(target)));
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

        @Idempotent
        protected final boolean isES6OrNewer() {
            return getContext().getEcmaScriptVersion() >= 6;
        }

        @Specialization(guards = "!isES6OrNewer()")
        protected Object splitES5(Object thisObj, Object separator, Object limitObj,
                        @Cached @Exclusive InlinedBranchProfile isUndefinedBranch,
                        @Cached @Exclusive InlinedBranchProfile isRegexpBranch,
                        @Cached @Exclusive InlinedBranchProfile isStringBranch,
                        @Cached @Shared StringSplitter stringSplitter,
                        @Cached RegExpSplitter regexpSplitter,
                        @Cached @Shared InlinedConditionProfile zeroLimit,
                        @Cached TRegexUtil.InteropReadIntMemberNode readGroupCount) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int limit = getLimit(limitObj);
            if (separator == Undefined.instance) {
                isUndefinedBranch.enter(this);
                return split(thisStr, limit, NOP_SPLITTER, null, 1, zeroLimit);
            } else if (separator instanceof JSRegExpObject regExp) {
                isRegexpBranch.enter(this);
                int groupCount = TRegexCompiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(regExp), this, readGroupCount);
                return split(thisStr, limit, regexpSplitter, regExp, groupCount, zeroLimit);
            } else {
                isStringBranch.enter(this);
                TruffleString separatorStr = toString2(separator);
                return split(thisStr, limit, stringSplitter, separatorStr, 1, zeroLimit);
            }
        }

        protected boolean isFastPath(Object thisObj, Object separator, Object limit) {
            return Strings.isTString(thisObj) && Strings.isTString(separator) && limit == Undefined.instance;
        }

        @Specialization(guards = {"isES6OrNewer()", "isUndefined(limit)"})
        protected Object splitES6StrStrUndefined(TruffleString thisStr, TruffleString sepStr, @SuppressWarnings("unused") Object limit,
                        @Cached @Shared StringSplitter stringSplitter,
                        @Cached @Shared InlinedConditionProfile zeroLimit) {
            return split(thisStr, Integer.MAX_VALUE, stringSplitter, sepStr, 1, zeroLimit);
        }

        @Specialization(guards = {"isES6OrNewer()", "!isFastPath(thisObj, separator, limit)"})
        protected Object splitES6Generic(Object thisObj, Object separator, Object limit,
                        @Cached @Shared StringSplitter stringSplitter,
                        @Cached @Shared InlinedConditionProfile zeroLimit,
                        @Cached @Exclusive InlinedConditionProfile isSpecialProfile,
                        @Cached @Exclusive InlinedConditionProfile callSpecialProfile) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(this, !(separator == Undefined.instance || separator == Null.instance))) {
                Object splitter = getMethod(separator, Symbol.SYMBOL_SPLIT);
                if (callSpecialProfile.profile(this, splitter != Undefined.instance)) {
                    return call(splitter, separator, new Object[]{thisObj, limit});
                }
            }
            return builtinSplit(thisObj, separator, limit, stringSplitter, zeroLimit);
        }

        private Object builtinSplit(Object thisObj, Object separator, Object limit, StringSplitter stringSplitter, InlinedConditionProfile zeroLimit) {
            TruffleString thisStr = toString(thisObj);
            int lim = getLimit(limit);
            if (separator == Undefined.instance) {
                return split(thisStr, lim, NOP_SPLITTER, null, 1, zeroLimit);
            } else {
                TruffleString sepStr = toString2(separator);
                return split(thisStr, lim, stringSplitter, sepStr, 1, zeroLimit);
            }
        }

        private int getLimit(Object limit) {
            return (limit == Undefined.instance) ? Integer.MAX_VALUE : toUInt32(limit);
        }

        private <T> JSDynamicObject split(TruffleString thisStr, int limit, Splitter<T> splitter, T separator, int groupCount, InlinedConditionProfile zeroLimit) {
            JSRealm realm = getRealm();
            if (zeroLimit.profile(this, limit == 0)) {
                return JSArray.createEmptyZeroLength(getContext(), realm);
            }
            Object[] splits = splitter.execute(this, thisStr, limit, separator, groupCount, this);
            return JSArray.createConstant(getContext(), realm, splits);
        }

        abstract static class Splitter<T> extends JavaScriptBaseNode {
            abstract Object[] execute(Node node, TruffleString input, int limit, T separator, int groupCount, JSStringSplitNode parent);
        }

        private static final Splitter<Void> NOP_SPLITTER = new NoSeparatorSplitter();

        static final class NoSeparatorSplitter extends Splitter<Void> {

            @Override
            Object[] execute(Node node, TruffleString input, int limit, Void separator, int groupCount, JSStringSplitNode parent) {
                return new Object[]{input};
            }

            @Override
            public boolean isAdoptable() {
                return false;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        abstract static class StringSplitter extends Splitter<TruffleString> {

            @Override
            abstract Object[] execute(Node node, TruffleString input, int limit, TruffleString separator, int groupCount, JSStringSplitNode parent);

            @Specialization
            static Object[] splitString(Node node, TruffleString input, int limit, TruffleString separator, @SuppressWarnings("unused") int groupCount, JSStringSplitNode parent,
                            @Cached InlinedConditionProfile emptySeparator,
                            @Cached InlinedBranchProfile growBranch,
                            @Cached InlinedCountingConditionProfile matchProfile) {
                if (emptySeparator.profile(parent, Strings.isEmpty(separator))) {
                    return individualCharSplit(input, limit, parent);
                } else {
                    return regularSplit(node, input, limit, separator, parent, growBranch, matchProfile);
                }
            }

            private static Object[] regularSplit(Node node, TruffleString input, int limit, TruffleString separator, JSStringSplitNode parent,
                            InlinedBranchProfile growBranch,
                            InlinedCountingConditionProfile matchProfile) {
                int end = parent.indexOf(input, separator, 0);
                if (matchProfile.profile(node, end == -1)) {
                    return new Object[]{input};
                }
                return regularSplitIntl(node, input, limit, separator, end, parent, growBranch);
            }

            private static Object[] regularSplitIntl(Node node, TruffleString input, int limit, TruffleString separator, int endParam, JSStringSplitNode parent,
                            InlinedBranchProfile growBranch) {
                SimpleArrayList<Object> splits = SimpleArrayList.create(limit);
                int start = 0;
                int end = endParam;
                while (end != -1) {
                    splits.add(parent.substring(input, start, end - start), node, growBranch);
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = end + Strings.length(separator);
                    end = parent.indexOf(input, separator, start);
                }
                splits.add(parent.substring(input, start), node, growBranch);
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

        @GenerateInline
        @GenerateCached(false)
        abstract static class RegExpSplitter extends Splitter<JSRegExpObject> {
            private static final Object[] EMPTY_SPLITS = {};
            private static final Object[] SINGLE_ZERO_LENGTH_SPLIT = {Strings.EMPTY_STRING};

            @Override
            abstract Object[] execute(Node node, TruffleString input, int limit, JSRegExpObject regExp, int groupCount, JSStringSplitNode parent);

            @Specialization
            static Object[] splitRegExp(Node node, TruffleString input, int limit, JSRegExpObject regExp, int groupCount, JSStringSplitNode parent,
                            @Cached InlinedConditionProfile emptyInput,
                            @Cached InlinedBranchProfile growBranch,
                            @Cached InlinedCountingConditionProfile matchProfile) {
                if (emptyInput.profile(node, Strings.isEmpty(input))) {
                    return splitEmptyString(node, regExp, parent, matchProfile);
                } else {
                    return splitNonEmptyString(node, input, limit, regExp, groupCount, parent, growBranch, matchProfile);
                }
            }

            private static Object[] splitEmptyString(Node node, JSRegExpObject regExp, JSStringSplitNode parent,
                            InlinedCountingConditionProfile matchProfile) {
                Object result = parent.matchIgnoreLastIndex(regExp, Strings.EMPTY_STRING, 0);
                return matchProfile.profile(node, TRegexResultAccessor.isMatch(result, parent, parent.readIsMatch)) ? EMPTY_SPLITS : SINGLE_ZERO_LENGTH_SPLIT;
            }

            private static Object[] splitNonEmptyString(Node node, TruffleString input, int limit, JSRegExpObject regExp, int groupCount, JSStringSplitNode parent,
                            @Cached InlinedBranchProfile growBranch,
                            @Cached InlinedCountingConditionProfile matchProfile) {
                Object result = parent.matchIgnoreLastIndex(regExp, input, 0);
                if (matchProfile.profile(node, !TRegexResultAccessor.isMatch(result, parent, parent.readIsMatch))) {
                    return new Object[]{input};
                }
                SimpleArrayList<Object> splits = new SimpleArrayList<>();
                int start = 0;
                while (TRegexResultAccessor.isMatch(result, parent, parent.readIsMatch)) {
                    int matchStart = TRegexResultAccessor.captureGroupStart(result, 0, parent, parent.getStartNode);
                    int matchEnd = TRegexResultAccessor.captureGroupEnd(result, 0, parent, parent.getEndNode);
                    if (matchEnd - matchStart == 0 && matchStart == start) {
                        // Avoid empty splits when using a regex that matches the empty string.
                        if (matchStart >= Strings.length(input) - 1) {
                            break;
                        }
                        result = parent.matchIgnoreLastIndex(regExp, input, start + 1);
                        continue;
                    }
                    TruffleString split = parent.substring(input, start, matchStart - start);
                    splits.add(split, node, growBranch);
                    int count = Math.min(groupCount - 1, limit - splits.size());
                    for (int i = 1; i <= count; i++) {
                        int groupStart = TRegexResultAccessor.captureGroupStart(result, i, parent, parent.getStartNode);
                        if (groupStart == TRegexUtil.Constants.CAPTURE_GROUP_NO_MATCH) {
                            splits.add(Undefined.instance, node, growBranch);
                        } else {
                            int groupEnd = TRegexResultAccessor.captureGroupEnd(result, i, parent, parent.getEndNode);
                            splits.add(parent.substring(input, groupStart, groupEnd - groupStart), node, growBranch);
                        }
                    }
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = matchEnd + (matchEnd == start ? 1 : 0);
                    result = parent.matchIgnoreLastIndex(regExp, input, start);
                }
                splits.add(parent.substring(input, start), node, growBranch);
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
            var sb = stringBuilderProfile.newStringBuilder();
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
        protected final ConditionProfile functionalReplaceProfile = ConditionProfile.create();
        protected final ConditionProfile replaceNecessaryProfile = ConditionProfile.create();

        @Child protected TruffleStringBuilder.ToStringNode builderToStringNode = TruffleStringBuilder.ToStringNode.create();
        @Child protected TruffleStringBuilder.AppendStringNode appendStringNode = TruffleStringBuilder.AppendStringNode.create();
        @Child protected TruffleStringBuilder.AppendSubstringByteIndexNode appendSubStringNode = TruffleStringBuilder.AppendSubstringByteIndexNode.create();

        public JSStringReplaceBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected ReplaceStringParser.Token[] parseReplaceValue(TruffleString replaceValue) {
            return ReplaceStringParser.parse(getContext(), replaceValue, 0, false);
        }

        protected static void appendSubstitution(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString replaceStr, TruffleString searchStr, int pos, JSStringReplaceBaseNode node,
                        Node profileNode, InlinedBranchProfile dollarProfile) {
            ReplaceStringParser.process(node.getContext(), replaceStr, 0, false, new ReplaceStringConsumer(sb, input, replaceStr, searchStr, pos), node, profileNode, dollarProfile);
        }

        protected static final class ReplaceStringConsumer implements ReplaceStringParser.Consumer<JSStringReplaceBaseNode, TruffleStringBuilderUTF16> {

            private final TruffleStringBuilderUTF16 sb;
            private final TruffleString input;
            private final TruffleString searchStr;
            private final TruffleString replaceStr;
            private final int matchedPos;

            private ReplaceStringConsumer(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString replaceStr, TruffleString searchStr, int matchedPos) {
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
            public TruffleStringBuilderUTF16 getResult() {
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

        void append(TruffleStringBuilderUTF16 sb, TruffleString s) {
            Strings.builderAppend(appendStringNode, sb, s);
        }

        void append(TruffleStringBuilderUTF16 sb, TruffleString s, int fromIndex, int toIndex) {
            Strings.builderAppend(appendSubStringNode, sb, s, fromIndex, toIndex);
        }

        void appendLen(TruffleStringBuilderUTF16 sb, TruffleString s, int fromIndex, int length) {
            Strings.builderAppendLen(appendSubStringNode, sb, s, fromIndex, length);
        }

        TruffleString builderToString(TruffleStringBuilderUTF16 sb) {
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

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)", limit = "1")
        protected Object replaceStringCached(TruffleString thisObj, TruffleString searchValue, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Bind Node node,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode,
                        @Cached @Shared InlinedBranchProfile dollarProfile) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue, dollarProfile, node);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, TruffleString searchValue, TruffleString replaceValue,
                        @Bind Node node,
                        @Cached @Shared InlinedBranchProfile dollarProfile) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, replaceValue, thisObj, null, dollarProfile, node);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!isStringString(searchValue, replaceValue)")
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue,
                        @Bind Node node,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToStringNode toString3Node,
                        @Cached IsCallableNode isCallableNode,
                        @Cached @Exclusive InlinedBranchProfile dollarProfile,
                        @Cached InlinedConditionProfile isSpecialProfile,
                        @Cached InlinedConditionProfile callSpecialProfile) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(node, !(searchValue == Undefined.instance || searchValue == Null.instance))) {
                Object replacer = getMethod(searchValue, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(node, replacer != Undefined.instance)) {
                    return call(replacer, searchValue, new Object[]{thisObj, replaceValue});
                }
            }
            return builtinReplace(searchValue, replaceValue, thisObj,
                            toString2Node, toString3Node, isCallableNode, dollarProfile, node);
        }

        private Object builtinReplace(Object searchValue, Object replParam, Object o,
                        JSToStringNode toString2Node, JSToStringNode toString3Node, IsCallableNode isCallableNode, InlinedBranchProfile dollarProfile, Node node) {
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
            var sb = Strings.builderCreate();
            append(sb, input, 0, pos);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, pos, input});
                append(sb, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(sb, input, replaceString, searchString, pos, this, node, dollarProfile);
            }
            append(sb, input, pos + Strings.length(searchString), Strings.length(input));
            return builderToString(sb);
        }

        private Object builtinReplaceString(TruffleString searchString, TruffleString replaceString, Object o, ReplaceStringParser.Token[] parsedReplaceParam, InlinedBranchProfile dollarProfile,
                        Node node) {
            TruffleString input = toString(o);
            int pos = indexOf(input, searchString);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                return input;
            }
            var sb = Strings.builderCreate();
            append(sb, input, 0, pos);
            if (parsedReplaceParam == null) {
                appendSubstitution(sb, input, replaceString, searchString, pos, this, node, dollarProfile);
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

        @Child private IsRegExpNode isRegExpNode;
        @Child private PropertyGetNode getFlagsNode;

        public JSStringReplaceAllNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "stringEquals(equalsNode, cachedReplaceValue, replaceValue)", limit = "1")
        protected Object replaceStringCached(Object thisObj, TruffleString searchValue, @SuppressWarnings("unused") TruffleString replaceValue,
                        @Cached("replaceValue") TruffleString cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue,
                        @Bind Node node,
                        @SuppressWarnings("unused") @Cached TruffleString.EqualNode equalsNode,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode stringIndexOfStringNode,
                        @Cached @Shared InlinedConditionProfile isSearchValueEmpty,
                        @Cached @Shared InlinedBranchProfile dollarProfile) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue,
                            node, stringIndexOfStringNode, isSearchValueEmpty, dollarProfile);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, TruffleString searchValue, TruffleString replaceValue,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode stringIndexOfStringNode,
                        @Cached @Shared InlinedConditionProfile isSearchValueEmpty,
                        @Cached @Shared InlinedBranchProfile dollarProfile) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, replaceValue, thisObj, null,
                            this, stringIndexOfStringNode, isSearchValueEmpty, dollarProfile);
        }

        protected Object performReplaceAll(TruffleString searchValue, TruffleString replaceValue, Object thisObj, ReplaceStringParser.Token[] parsedReplaceParam,
                        Node node, TruffleString.ByteIndexOfStringNode stringIndexOfStringNode, InlinedConditionProfile isSearchValueEmpty, InlinedBranchProfile dollarProfile) {
            TruffleString thisStr = toString(thisObj);
            if (isSearchValueEmpty.profile(node, Strings.isEmpty(searchValue))) {
                int len = Strings.length(thisStr);
                var sb = Strings.builderCreate((len + 1) * Strings.length(replaceValue) + len);
                append(sb, replaceValue);
                for (int i = 0; i < len; i++) {
                    appendLen(sb, thisStr, i, 1);
                    append(sb, replaceValue);
                }
                return builderToString(sb);
            }
            var sb = Strings.builderCreate();
            int position = 0;
            while (position < Strings.length(thisStr)) {
                int nextPosition = Strings.indexOf(stringIndexOfStringNode, thisStr, searchValue, position);
                builtinReplaceString(searchValue, replaceValue, thisStr, parsedReplaceParam, position, nextPosition, sb, node, dollarProfile);
                if (nextPosition < 0) {
                    break;
                }
                position = nextPosition + Strings.length(searchValue);
            }
            return builderToString(sb);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!isStringString(searchValue, replaceValue)")
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue,
                        @Bind Node node,
                        @Cached @Exclusive InlinedBranchProfile errorBranch,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToStringNode toString3Node,
                        @Cached IsCallableNode isCallableNode,
                        @Cached @Exclusive InlinedConditionProfile isRegExp,
                        @Cached TruffleString.ByteIndexOfCodePointNode stringIndexOfNode,
                        @Cached @Exclusive TruffleString.ByteIndexOfStringNode stringIndexOfStringNode,
                        @Cached @Exclusive InlinedConditionProfile isSearchValueEmpty,
                        @Cached @Exclusive InlinedBranchProfile dollarProfile,
                        @Cached @Exclusive InlinedConditionProfile isSpecialProfile,
                        @Cached @Exclusive InlinedConditionProfile callSpecialProfile) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(node, !(searchValue == Undefined.instance || searchValue == Null.instance))) {
                if (isRegExp.profile(node, getIsRegExpNode().executeBoolean(searchValue))) {
                    Object flags = getFlags(searchValue);
                    requireObjectCoercible(flags);
                    TruffleString flagsStr = toString(flags);
                    if (Strings.indexOf(stringIndexOfNode, flagsStr, 'g') == -1) {
                        errorBranch.enter(node);
                        throw Errors.createTypeError("Only global regexps allowed");
                    }
                }
                Object replacer = getMethod(searchValue, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(node, replacer != Undefined.instance)) {
                    return call(replacer, searchValue, new Object[]{thisObj, replaceValue});
                }
            }
            return performReplaceAllGeneric(searchValue, replaceValue, thisObj,
                            node, toString2Node, toString3Node, isCallableNode, stringIndexOfStringNode, isSearchValueEmpty, dollarProfile);
        }

        protected Object performReplaceAllGeneric(Object searchValue, Object replParam, Object thisObj,
                        Node node, JSToStringNode toString2Node, JSToStringNode toString3Node, IsCallableNode isCallableNode,
                        TruffleString.ByteIndexOfStringNode stringIndexOfStringNode, InlinedConditionProfile isSearchValueEmpty, InlinedBranchProfile dollarProfile) {
            TruffleString thisStr = toString(thisObj);
            TruffleString searchString = toString2Node.executeString(searchValue);
            var sb = Strings.builderCreate();
            int position = 0;

            boolean functionalReplace = isCallableNode.executeBoolean(replParam);
            Object replaceValue;
            if (functionalReplaceProfile.profile(functionalReplace)) {
                replaceValue = replParam;
            } else {
                replaceValue = toString3Node.executeString(replParam);
            }
            if (isSearchValueEmpty.profile(node, Strings.isEmpty(searchString))) {
                while (position <= Strings.length(thisStr)) {
                    builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, position, sb, node, toString3Node, dollarProfile);
                    if (position < Strings.length(thisStr)) {
                        appendLen(sb, thisStr, position, 1);
                    }
                    ++position;
                }
                return builderToString(sb);
            }
            while (position < Strings.length(thisStr)) {
                int nextPosition = Strings.indexOf(stringIndexOfStringNode, thisStr, searchString, position);
                builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, nextPosition, sb, node, toString3Node, dollarProfile);
                if (nextPosition < 0) {
                    break;
                }
                position = nextPosition + Strings.length(searchString);
            }
            return builderToString(sb);
        }

        private void builtinReplace(TruffleString searchString, boolean functionalReplace, Object replParam, TruffleString input, int lastPosition, int curPosition, TruffleStringBuilderUTF16 sb,
                        Node node, JSToStringNode toString3Node, InlinedBranchProfile dollarProfile) {
            if (replaceNecessaryProfile.profile(curPosition < 0)) {
                append(sb, input, lastPosition, Strings.length(input));
                return;
            }
            append(sb, input, lastPosition, curPosition);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, curPosition, input});
                append(sb, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(sb, input, (TruffleString) replParam, searchString, curPosition, this, node, dollarProfile);
            }
        }

        private void builtinReplaceString(TruffleString searchString, TruffleString replaceString, TruffleString input, ReplaceStringParser.Token[] parsedReplaceParam,
                        int lastPosition, int curPosition, TruffleStringBuilderUTF16 sb,
                        Node node, InlinedBranchProfile dollarProfile) {
            if (replaceNecessaryProfile.profile(curPosition < 0)) {
                append(sb, input, lastPosition, Strings.length(input));
                return;
            }
            append(sb, input, lastPosition, curPosition);
            if (parsedReplaceParam == null) {
                appendSubstitution(sb, input, replaceString, searchString, curPosition, this, node, dollarProfile);
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

    }

    /**
     * Implementation of the String.prototype.replace() method as specified by ECMAScript 5.1 in
     * 15.5.4.11.
     */
    public abstract static class JSStringReplaceES5Node extends JSStringReplaceBaseNode {
        @Child private PropertySetNode setLastIndexNode;
        @Child private StringReplacer stringReplacerNode;
        @Child private FunctionReplacer functionReplacerNode;
        @Child private TruffleString.ByteIndexOfStringNode stringIndexOfNode;
        @Child TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();
        @Child TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();

        public JSStringReplaceES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected Object replace(Object thisObj, Object searchValue, Object replaceValue,
                        @Bind Node node,
                        @Cached JSToStringNode toString2Node,
                        @Cached JSToStringNode toString3Node,
                        @Cached InlinedCountingConditionProfile ifIsMatch,
                        @Cached InlinedConditionProfile isRegExp,
                        @Cached InlinedCountingConditionProfile isFnRepl,
                        @Cached TRegexUtil.InteropReadIntMemberNode readGroupCount,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getGlobalFlag,
                        @Cached InlinedBranchProfile errorBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (Strings.length(thisStr) > getContext().getStringLengthLimit()) {
                errorBranch.enter(node);
                throw Errors.createRangeErrorInvalidStringLength();
            }
            if (isRegExp.profile(node, searchValue instanceof JSRegExpObject)) {
                JSRegExpObject searchRegExp = (JSRegExpObject) searchValue;
                Object tRegexCompiledRegex = JSRegExp.getCompiledRegex(searchRegExp);
                int groupCount = TRegexCompiledRegexAccessor.groupCount(tRegexCompiledRegex, node, readGroupCount);
                if (isFnRepl.profile(node, JSFunction.isJSFunction(replaceValue))) {
                    JSFunctionObject replaceFunc = (JSFunctionObject) replaceValue;
                    if (isGlobal(tRegexCompiledRegex, node, getGlobalFlag)) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getFunctionReplacerNode(), replaceFunc, tRegexCompiledRegex,
                                        node, ifIsMatch, readIsMatch);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, groupCount, getFunctionReplacerNode(), replaceFunc,
                                        tRegexCompiledRegex, node, ifIsMatch, readIsMatch);
                    }
                } else {
                    TruffleString replaceStr = toString3Node.executeString(replaceValue);
                    if (isGlobal(tRegexCompiledRegex, node, getGlobalFlag)) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getStringReplacerNode(), replaceStr, tRegexCompiledRegex,
                                        node, ifIsMatch, readIsMatch);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, groupCount, getStringReplacerNode(), replaceStr,
                                        tRegexCompiledRegex, node, ifIsMatch, readIsMatch);
                    }
                }
            } else {
                TruffleString searchStr = toString2Node.executeString(searchValue);
                if (isFnRepl.profile(node, JSFunction.isJSFunction(replaceValue))) {
                    return replaceFirst(thisStr, searchStr, getFunctionReplacerNode(), (JSFunctionObject) replaceValue,
                                    node, ifIsMatch);
                } else {
                    TruffleString replaceStr = toString3Node.executeString(replaceValue);
                    return replaceFirst(thisStr, searchStr, getStringReplacerNode(), replaceStr,
                                    node, ifIsMatch);
                }
            }
        }

        private static boolean isGlobal(Object tRegexCompiledRegex, Node node, TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getGlobalFlag) {
            return getGlobalFlag.execute(node, tRegexCompiledRegex, TRegexUtil.Props.Flags.GLOBAL);
        }

        private void setLastIndex(JSRegExpObject regExp, int value) {
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

        private <T> Object replaceFirst(TruffleString thisStr, TruffleString searchStr, Replacer<T> replacer, T replaceValue,
                        Node node, InlinedCountingConditionProfile ifIsMatch) {
            int start = indexOf(thisStr, searchStr);
            if (ifIsMatch.profile(node, start < 0)) {
                return thisStr;
            }
            int end = start + Strings.length(searchStr);
            var sb = Strings.builderCreate();
            append(sb, thisStr, 0, start);
            replacer.appendReplacementString(sb, thisStr, searchStr, start, replaceValue, this);
            append(sb, thisStr, end, Strings.length(thisStr));
            return builderToString(sb);
        }

        private <T> TruffleString replaceFirst(TruffleString thisStr, JSRegExpObject regExp, int groupCount, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex, Node node,
                        InlinedCountingConditionProfile ifIsMatch,
                        InteropReadBooleanMemberNode readIsMatch) {
            Object result = match(regExp, thisStr);
            if (ifIsMatch.profile(node, !TRegexResultAccessor.isMatch(result, node, readIsMatch))) {
                return thisStr;
            }
            return replace(thisStr, result, groupCount, replacer, replaceValue, tRegexCompiledRegex, node);
        }

        protected final Object match(JSRegExpObject regExp, TruffleString input) {
            assert getContext().getEcmaScriptVersion() <= 5;
            return getRegExpNode().execute(regExp, input);
        }

        private <T> TruffleString replace(TruffleString thisStr, Object result, int groupCount, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex, Node node) {
            var sb = Strings.builderCreate();
            int matchStart = TRegexResultAccessor.captureGroupStart(result, 0, node, getStart);
            int matchEnd = TRegexResultAccessor.captureGroupEnd(result, 0, node, getEnd);
            append(sb, thisStr, 0, matchStart);
            replacer.appendReplacementRegex(sb, thisStr, result, groupCount, replaceValue, this, tRegexCompiledRegex, matchStart, matchEnd);
            append(sb, thisStr, matchEnd, Strings.length(thisStr));
            return builderToString(sb);
        }

        private <T> TruffleString replaceAll(JSRegExpObject regExp, TruffleString input, int groupCount, Replacer<T> replacer, T replaceValue, Object tRegexCompiledRegex, Node node,
                        InlinedCountingConditionProfile ifIsMatch,
                        TRegexUtil.InteropReadBooleanMemberNode readIsMatch) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (ifIsMatch.profile(node, !TRegexResultAccessor.isMatch(result, node, readIsMatch))) {
                return input;
            }
            var sb = Strings.builderCreate();
            int thisIndex = 0;
            int lastIndex = 0;
            while (TRegexResultAccessor.isMatch(result, node, readIsMatch)) {
                int matchStart = TRegexResultAccessor.captureGroupStart(result, 0, node, getStart);
                int matchEnd = TRegexResultAccessor.captureGroupEnd(result, 0, node, getEnd);
                append(sb, input, thisIndex, matchStart);
                replacer.appendReplacementRegex(sb, input, result, groupCount, replaceValue, this, tRegexCompiledRegex, matchStart, matchEnd);
                if (Strings.builderLength(sb) > getContext().getStringLengthLimit()) {
                    CompilerDirectives.transferToInterpreter();
                    throw Errors.createRangeErrorInvalidStringLength();
                }
                thisIndex = matchEnd;
                if (thisIndex == Strings.length(input) && matchEnd - matchStart == 0) {
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

            protected Replacer(JSStringReplaceES5Node parent) {
                this.parentNode = parent;
            }

            abstract void appendReplacementRegex(TruffleStringBuilderUTF16 sb, TruffleString input, Object result, int groupCount, T replaceValue, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex, int matchStart, int matchEnd);

            abstract void appendReplacementString(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString matchedString, int pos, T replaceValue, JSStringReplaceES5Node parent);
        }

        protected static final class StringReplacer extends Replacer<TruffleString> implements RegExpPrototypeBuiltins.ReplaceStringConsumerTRegex.ParentNode {

            private final ConditionProfile emptyReplace = ConditionProfile.create();
            private final BranchProfile invalidGroupNumberProfile = BranchProfile.create();

            private StringReplacer(JSStringReplaceES5Node parent) {
                super(parent);
            }

            public static StringReplacer create(JSStringReplaceES5Node parent) {
                return new StringReplacer(parent);
            }

            @Override
            void appendReplacementRegex(TruffleStringBuilderUTF16 sb, TruffleString input, Object result, int groupCount, TruffleString replaceStr, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex, int matchStart, int matchEnd) {
                if (emptyReplace.profile(!Strings.isEmpty(replaceStr))) {
                    ReplaceStringParser.process(parent.getContext(), replaceStr, groupCount, false,
                                    new RegExpPrototypeBuiltins.ReplaceStringConsumerTRegex(sb, input, replaceStr, matchStart, matchEnd, result, tRegexCompiledRegex, groupCount),
                                    this, null, InlinedBranchProfile.getUncached());
                }
            }

            @Override
            void appendReplacementString(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString matchedString, int pos, TruffleString replaceValue, JSStringReplaceES5Node parent) {
                JSStringReplaceNode.appendSubstitution(sb, input, replaceValue, matchedString, pos, parent, null, InlinedBranchProfile.getUncached());
            }

            @Override
            public void append(TruffleStringBuilderUTF16 sb, TruffleString s) {
                parentNode.append(sb, s);
            }

            @Override
            public void append(TruffleStringBuilderUTF16 sb, TruffleString s, int fromIndex, int toIndex) {
                parentNode.append(sb, s, fromIndex, toIndex);
            }

            @Override
            public InvokeGetGroupBoundariesMethodNode getGetStartNode() {
                return parentNode.getStart;
            }

            @Override
            public InvokeGetGroupBoundariesMethodNode getGetEndNode() {
                return parentNode.getEnd;
            }

            @Override
            public BranchProfile getInvalidGroupNumberProfile() {
                return invalidGroupNumberProfile;
            }
        }

        protected static final class FunctionReplacer extends Replacer<JSFunctionObject> {
            @Child private JSFunctionCallNode functionCallNode = JSFunctionCallNode.createCall();
            @Child private JSToStringNode toStringNode = JSToStringNode.create();

            private FunctionReplacer(JSStringReplaceES5Node parent) {
                super(parent);
            }

            public static FunctionReplacer create(JSStringReplaceES5Node parent) {
                return new FunctionReplacer(parent);
            }

            @Override
            void appendReplacementRegex(TruffleStringBuilderUTF16 sb, TruffleString input, Object result, int groupCount, JSFunctionObject replaceFunc, JSStringReplaceES5Node parent,
                            Object tRegexCompiledRegex, int matchStart, int matchEnd) {
                parent.append(sb, callReplaceValueFunc(parent.getContext(), result, input, groupCount, replaceFunc, matchStart));
            }

            @Override
            void appendReplacementString(TruffleStringBuilderUTF16 sb, TruffleString input, TruffleString matchedString, int pos, JSFunctionObject replaceFunc, JSStringReplaceES5Node parent) {
                Object[] arguments = createArguments(new Object[]{matchedString}, pos, input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                TruffleString replaceStr = toStringNode.executeString(replaceValue);
                parent.append(sb, replaceStr);
            }

            private TruffleString callReplaceValueFunc(JSContext context, Object result, TruffleString input, int groupCount, JSFunctionObject replaceFunc, int matchStart) {
                Object[] matches = TRegexMaterializeResult.materializeFull(context, result, groupCount, input,
                                parentNode, parentNode.substringNode, parentNode.getStart, parentNode.getEnd);
                Object[] arguments = createArguments(matches, matchStart, input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                return toStringNode.executeString(replaceValue);
            }

            private static Object[] createArguments(Object[] matches, int matchIndex, Object input, JSFunctionObject replaceFunc) {
                Object[] arguments = JSArguments.createInitial(Undefined.instance, replaceFunc, matches.length + 2);
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

        @Specialization
        protected TruffleString toStringTString(TruffleString thisStr) {
            return thisStr;
        }

        @Specialization
        protected static TruffleString toStringStringObject(JSStringObject thisStr) {
            return JSString.getString(thisStr);
        }

        @InliningCutoff
        @Specialization(guards = "isForeignObject(thisObj)", limit = "InteropLibraryLimit")
        protected TruffleString toStringForeignObject(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            if (interop.isString(thisObj)) {
                return Strings.interopAsTruffleString(thisObj, interop, switchEncoding);
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
     *
     * @see JSStringToUpperCaseNode
     */
    public abstract static class JSStringToLowerCaseNode extends JSStringOperation {

        private static final TruffleString.CodePointSet UPPER_CASE_ASCII_SET = TruffleString.CodePointSet.fromRanges(new int[]{'A', 'Z'}, TruffleString.Encoding.UTF_16);

        private final boolean locale;

        public JSStringToLowerCaseNode(JSContext context, JSBuiltin builtin, boolean locale) {
            super(context, builtin);
            this.locale = locale;
        }

        private static boolean isUpperCaseAscii(int c) {
            return c >= 'A' && c <= 'Z';
        }

        private static byte toLowerCaseAscii(byte c) {
            return (byte) (c | 0x20);
        }

        @Specialization
        protected final TruffleString toLowerCaseString(TruffleString thisStr,
                        @Cached TruffleString.CodeRangeEqualsNode codeRangeEquals,
                        @Cached TruffleString.ByteIndexOfCodePointSetNode indexOfCodePointSet,
                        @Cached TruffleString.SwitchEncodingNode switchEncodingNode,
                        @Cached TruffleString.CopyToByteArrayNode copyToByteArrayNode,
                        @Cached TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Shared @Cached TruffleString.FromJavaStringNode fromJavaString,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString,
                        @Cached InlinedConditionProfile isAscii,
                        @Cached InlinedConditionProfile isAlreadyLowerCase) {
            if (isAscii.profile(this, codeRangeEquals.execute(thisStr, CodeRange.ASCII))) {
                int firstUpperCase = indexOfCodePointSet.execute(thisStr, 0, thisStr.byteLength(TruffleString.Encoding.UTF_16), UPPER_CASE_ASCII_SET) >> 1;
                if (isAlreadyLowerCase.profile(this, firstUpperCase < 0)) {
                    return thisStr;
                } else if (!locale) {
                    TruffleString ascii = switchEncodingNode.execute(thisStr, TruffleString.Encoding.US_ASCII);
                    byte[] buf = new byte[ascii.byteLength(TruffleString.Encoding.US_ASCII)];
                    copyToByteArrayNode.execute(ascii, 0, buf, 0, buf.length, TruffleString.Encoding.US_ASCII);
                    for (int i = firstUpperCase; i < buf.length; ++i) {
                        byte c = buf[i];
                        if (isUpperCaseAscii(c)) {
                            buf[i] = toLowerCaseAscii(c);
                        }
                    }
                    return switchEncodingNode.execute(fromByteArrayNode.execute(buf, TruffleString.Encoding.US_ASCII, false), TruffleString.Encoding.UTF_16);
                }
            }
            return toLowerCaseJava(thisStr, fromJavaString, toJavaString);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected final Object toLowerCaseGeneric(Object thisObj,
                        @Shared @Cached TruffleString.FromJavaStringNode fromJavaString,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            return toLowerCaseJava(thisStr, fromJavaString, toJavaString);
        }

        private TruffleString toLowerCaseJava(TruffleString str,
                        TruffleString.FromJavaStringNode fromJavaString,
                        TruffleString.ToJavaStringNode toJavaString) {
            Locale usingLocale = locale ? getContext().getLocale() : Locale.US;
            return fromJavaString.execute(Strings.javaStringToLowerCase(toJavaString.execute(str), usingLocale), TruffleString.Encoding.UTF_16);
        }
    }

    /**
     * Implementation of the String.prototype.toLocaleLowerCase() and toLocaleUpperCase() methods as
     * specified by ECMAScript Internationalization API, 1.0.
     *
     * https://tc39.github.io/ecma402/#sup-string.prototype.tolocalelowercase
     * https://tc39.github.io/ecma402/#sup-string.prototype.tolocaleuppercase
     */
    public abstract static class JSStringToLocaleLowerOrUpperCaseNode extends JSStringOperation {

        private final boolean toUpperCase;
        @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;

        public JSStringToLocaleLowerOrUpperCaseNode(JSContext context, JSBuiltin builtin, boolean toUpperCase) {
            super(context, builtin);
            this.toUpperCase = toUpperCase;
            this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        }

        @Specialization
        protected final TruffleString doString(TruffleString thisStr, Object locale,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached @Shared TruffleString.FromJavaStringNode fromJavaStringNode) {
            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(locale);
            if (Strings.isEmpty(thisStr)) {
                return thisStr;
            }
            String thisJStr = Strings.toJavaString(toJavaStringNode, thisStr);
            String resultJStr = toUpperCase
                            ? IntlUtil.toUpperCase(getContext(), thisJStr, locales)
                            : IntlUtil.toLowerCase(getContext(), thisJStr, locales);
            return Strings.fromJavaString(fromJavaStringNode, resultJStr);
        }

        @Specialization(replaces = "doString")
        protected final TruffleString doGeneric(Object thisObj, Object locale,
                        @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached @Shared TruffleString.FromJavaStringNode fromJavaStringNode) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            return doString(thisStr, locale, toJavaStringNode, fromJavaStringNode);
        }
    }

    /**
     * Implementation of the String.prototype.toUpperCase() method as specified by ECMAScript 5.1 in
     * 15.5.4.18.
     *
     * @see JSStringToLowerCaseNode
     */
    public abstract static class JSStringToUpperCaseNode extends JSStringOperation {

        private static final TruffleString.CodePointSet LOWER_CASE_ASCII_SET = TruffleString.CodePointSet.fromRanges(new int[]{'a', 'z'}, TruffleString.Encoding.UTF_16);

        private final boolean locale;

        public JSStringToUpperCaseNode(JSContext context, JSBuiltin builtin, boolean locale) {
            super(context, builtin);
            this.locale = locale;
        }

        private static boolean isLowerCaseAscii(int c) {
            return c >= 'a' && c <= 'z';
        }

        private static byte toUpperCaseAscii(byte c) {
            return (byte) (c & ~0x20);
        }

        @Specialization
        protected final Object toUpperCaseString(TruffleString thisStr,
                        @Cached TruffleString.CodeRangeEqualsNode codeRangeEquals,
                        @Cached TruffleString.ByteIndexOfCodePointSetNode indexOfCodePointSet,
                        @Cached TruffleString.SwitchEncodingNode switchEncodingNode,
                        @Cached TruffleString.CopyToByteArrayNode copyToByteArrayNode,
                        @Cached TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Shared @Cached TruffleString.FromJavaStringNode fromJavaString,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString,
                        @Cached InlinedConditionProfile isAscii,
                        @Cached InlinedConditionProfile isAlreadyUpperCase) {
            if (isAscii.profile(this, codeRangeEquals.execute(thisStr, CodeRange.ASCII))) {
                int firstLowerCase = indexOfCodePointSet.execute(thisStr, 0, thisStr.byteLength(TruffleString.Encoding.UTF_16), LOWER_CASE_ASCII_SET) >> 1;
                if (isAlreadyUpperCase.profile(this, firstLowerCase < 0)) {
                    return thisStr;
                } else if (!locale) {
                    TruffleString ascii = switchEncodingNode.execute(thisStr, TruffleString.Encoding.US_ASCII);
                    byte[] buf = new byte[ascii.byteLength(TruffleString.Encoding.US_ASCII)];
                    copyToByteArrayNode.execute(ascii, 0, buf, 0, buf.length, TruffleString.Encoding.US_ASCII);
                    for (int i = firstLowerCase; i < buf.length; ++i) {
                        byte c = buf[i];
                        if (isLowerCaseAscii(c)) {
                            buf[i] = toUpperCaseAscii(c);
                        }
                    }
                    return switchEncodingNode.execute(fromByteArrayNode.execute(buf, TruffleString.Encoding.US_ASCII, false), TruffleString.Encoding.UTF_16);
                }
            }
            return toUpperCaseJava(thisStr, fromJavaString, toJavaString);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected final Object toUpperCaseGeneric(Object thisObj,
                        @Shared @Cached TruffleString.FromJavaStringNode fromJavaString,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            return toUpperCaseJava(thisStr, fromJavaString, toJavaString);
        }

        private Object toUpperCaseJava(TruffleString str,
                        TruffleString.FromJavaStringNode fromJavaString,
                        TruffleString.ToJavaStringNode toJavaString) {
            Locale usingLocale = locale ? getContext().getLocale() : Locale.US;
            return fromJavaString.execute(Strings.javaStringToUpperCase(toJavaString.execute(str), usingLocale), TruffleString.Encoding.UTF_16);
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
        protected Object search(Object thisObj, Object regex,
                        @Cached InlinedConditionProfile isSpecialProfile,
                        @Cached InlinedConditionProfile callSpecialProfile) {
            assert getContext().getEcmaScriptVersion() >= 6;
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(this, !(regex == Undefined.instance || regex == Null.instance))) {
                Object searcher = getMethod(regex, Symbol.SYMBOL_SEARCH);
                if (callSpecialProfile.profile(this, searcher != Undefined.instance)) {
                    return call(searcher, regex, new Object[]{thisObj});
                }
            }
            return builtinSearch(thisObj, regex);
        }

        private Object builtinSearch(Object thisObj, Object regex) {
            TruffleString thisStr = toString(thisObj);
            Object cRe = getCompileRegexNode().compile(regex == Undefined.instance ? Strings.EMPTY_STRING : toString(regex));
            JSRegExpObject regExp = getCreateRegExpNode().createRegExp(cRe);
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

        public JSStringSearchES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int search(Object thisObj, Object[] args,
                        @Cached("create(getContext())") JSToRegExpNode toRegExpNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart) {
            assert getContext().getEcmaScriptVersion() < 6;
            Object searchObj = JSRuntime.getArgOrUndefined(args, 0);
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            JSRegExpObject regExp = toRegExpNode.execute(searchObj);
            Object result = matchIgnoreLastIndex(regExp, thisStr, 0);
            if (TRegexResultAccessor.isMatch(result, this, readIsMatch)) {
                return TRegexResultAccessor.captureGroupStart(result, 0, this, getStart);
            } else {
                return -1;
            }
        }
    }

    /**
     * Implementation of the String.prototype.substr() method as specified by ECMAScript 5.1 in
     * Annex B.2.3.
     */
    public abstract static class JSStringSubstrNode extends JSStringOperation {

        public JSStringSubstrNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object substrInt(TruffleString thisStr, int start, int length,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedBranchProfile startNegativeBranch,
                        @Cached @Shared InlinedBranchProfile finalLenEmptyBranch) {
            return substrIntl(thisStr, start, length,
                            substringNode, startNegativeBranch, finalLenEmptyBranch);
        }

        @Specialization(guards = "isUndefined(length)")
        protected Object substrLenUndef(TruffleString thisStr, int start, @SuppressWarnings("unused") Object length,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedBranchProfile startNegativeBranch,
                        @Cached @Shared InlinedBranchProfile finalLenEmptyBranch) {
            return substrIntl(thisStr, start, Strings.length(thisStr),
                            substringNode, startNegativeBranch, finalLenEmptyBranch);
        }

        @Specialization
        protected Object substrGeneric(Object thisObj, Object start, Object length,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode,
                        @Cached @Shared InlinedBranchProfile startNegativeBranch,
                        @Cached @Shared InlinedBranchProfile finalLenEmptyBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int startInt = toIntegerAsInt(start);
            int len = (length == Undefined.instance) ? Strings.length(thisStr) : toIntegerAsInt(length);
            return substrIntl(thisStr, startInt, len,
                            substringNode, startNegativeBranch, finalLenEmptyBranch);
        }

        private Object substrIntl(TruffleString thisStr, int start, int length,
                        TruffleString.SubstringByteIndexNode substringNode,
                        InlinedBranchProfile startNegativeBranch,
                        InlinedBranchProfile finalLenEmptyBranch) {
            int startInt = start;
            if (startInt < 0) {
                startNegativeBranch.enter(this);
                startInt = Math.max(startInt + Strings.length(thisStr), 0);
            }
            int finalLen = within(length, 0, Math.max(0, Strings.length(thisStr) - startInt));
            if (finalLen <= 0) {
                finalLenEmptyBranch.enter(this);
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
        private final boolean matchAll;

        protected JSStringMatchNode(JSContext context, JSBuiltin builtin, boolean matchAll) {
            super(context, builtin);
            this.matchAll = matchAll;
        }

        @Specialization
        protected Object match(Object thisObj, Object regex,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile isSpecialProfile,
                        @Cached InlinedConditionProfile callSpecialProfile) {
            requireObjectCoercible(thisObj);
            if (isSpecialProfile.profile(this, !(regex == Undefined.instance || regex == Null.instance))) {
                if (matchAll && getIsRegExpNode().executeBoolean(regex)) {
                    Object flags = getFlags(regex);
                    requireObjectCoercible(flags);
                    if (indexOf(toString(flags), 'g') == -1) {
                        errorBranch.enter(this);
                        throw Errors.createTypeError("Regular expression passed to matchAll() is missing 'g' flag.");
                    }
                }
                Object matcher = getMethod(regex, matchSymbol());
                if (callSpecialProfile.profile(this, matcher != Undefined.instance)) {
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
            JSRegExpObject regExp = getCreateRegExpNode().createRegExp(cRe);
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
     * 15.5.4.10.
     */
    public abstract static class JSStringMatchES5Node extends JSStringOperationWithRegExpArgument {
        @Child private PropertySetNode setLastIndexNode;

        public JSStringMatchES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
        }

        private void setLastIndex(JSRegExpObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            setLastIndexNode.setValue(regExp, value);
        }

        @Specialization
        protected JSDynamicObject match(Object thisObj, Object searchObj,
                        @Cached("create(getContext())") JSToRegExpNode toRegExpNode,
                        @Cached("create(getContext())") JSRegExpExecES5Node regExpExecNode,
                        @Cached InlinedCountingConditionProfile isMatch,
                        @Cached InlinedCountingConditionProfile isGlobalRegExp,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        @Cached TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd,
                        @Cached TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getGlobalFlag) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isGlobalRegExp.profile(this, searchObj instanceof JSRegExpObject searchRegExp && isGlobal(JSRegExp.getCompiledRegex(searchRegExp), getGlobalFlag))) {
                return matchGlobal(thisStr, (JSRegExpObject) searchObj,
                                this, isMatch, substringNode, readIsMatch, getStart, getEnd);
            } else {
                return matchNotGlobal(thisStr, searchObj, toRegExpNode, regExpExecNode);
            }
        }

        private boolean isGlobal(Object compiledRegex, TRegexUtil.TRegexCompiledRegexSingleFlagAccessorNode getGlobalFlag) {
            return getGlobalFlag.execute(this, compiledRegex, TRegexUtil.Props.Flags.GLOBAL);
        }

        private static JSDynamicObject matchNotGlobal(TruffleString thisStr, Object searchObj,
                        JSToRegExpNode toRegExpNode, JSRegExpExecES5Node regExpExecNode) {
            JSRegExpObject regExp = toRegExpNode.execute(searchObj);
            return regExpExecNode.execute(regExp, thisStr);
        }

        private JSDynamicObject matchGlobal(TruffleString input, JSRegExpObject regExp,
                        Node node,
                        InlinedCountingConditionProfile isMatch,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        TRegexUtil.InteropReadBooleanMemberNode readIsMatch,
                        TRegexUtil.InvokeGetGroupBoundariesMethodNode getStart,
                        TRegexUtil.InvokeGetGroupBoundariesMethodNode getEnd) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (isMatch.profile(this, !TRegexResultAccessor.isMatch(result, node, readIsMatch))) {
                return Null.instance;
            }
            List<Object> matches = new ArrayList<>();
            int lastIndex = 0;
            while (TRegexResultAccessor.isMatch(result, node, readIsMatch)) {
                Boundaries.listAdd(matches, TRegexMaterializeResult.materializeGroup(getContext(), result, 0, input, node, substringNode, getStart, getEnd));

                int thisIndex = TRegexResultAccessor.captureGroupEnd(result, 0, node, getEnd);
                lastIndex = thisIndex + (thisIndex == lastIndex ? 1 : 0);
                result = lastIndex > Strings.length(input) ? getContext().getTRegexEmptyResult() : matchIgnoreLastIndex(regExp, input, lastIndex);
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
                        @Shared @Cached JSTrimWhitespaceNode trimWhitespaceNode) {
            return trimWhitespaceNode.executeString(thisStr);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected Object trimObject(Object thisObj,
                        @Shared @Cached JSTrimWhitespaceNode trimWhitespaceNode) {
            requireObjectCoercible(thisObj);
            return trimWhitespaceNode.executeString(toString(thisObj));
        }
    }

    /**
     * Non-standard String.prototype.trimLeft to provide compatibility with Nashorn and V8.
     */
    public abstract static class JSStringTrimLeftNode extends JSStringOperation {

        public JSStringTrimLeftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final Object trimLeft(Object thisObj,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached TruffleString.ReadCharUTF16Node readRawNode,
                        @Cached InlinedConditionProfile lengthExceeded,
                        @Cached InlinedConditionProfile lengthZero) {
            requireObjectCoercible(thisObj);
            TruffleString string = toString(thisObj);

            int firstIdx = JSRuntime.firstNonWhitespaceIndex(string, readRawNode);
            if (lengthZero.profile(this, firstIdx == 0)) {
                return string;
            } else if (lengthExceeded.profile(this, firstIdx >= Strings.length(string))) {
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

        public JSStringTrimRightNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final Object trimRight(Object thisObj,
                        @Cached TruffleString.ReadCharUTF16Node readRawNode,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @Cached InlinedConditionProfile lengthExceeded) {
            requireObjectCoercible(thisObj);
            TruffleString string = toString(thisObj);

            int lastIdx = JSRuntime.lastNonWhitespaceIndex(string, readRawNode);
            if (lengthExceeded.profile(this, lastIdx >= Strings.length(string))) {
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

        private static final LazyValue<Collator> lazyCollator = new LazyValue<>(JSStringLocaleCompareNode::getCollator);

        public JSStringLocaleCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        private static Collator getCollator() {
            Collator collator = Collator.getInstance(Locale.ROOT);
            collator.setStrength(Collator.TERTIARY);
            collator.setDecomposition(Collator.FULL_DECOMPOSITION);
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
            return lazyCollator.get().compare(Strings.toJavaString(thisStr), Strings.toJavaString(thatStr));
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

        private JSCollatorObject createCollator(Object locales, Object options) {
            JSCollatorObject collatorObj = JSCollator.create(getContext(), getRealm());
            initCollatorNode.executeInit(collatorObj, locales, options);
            return collatorObj;
        }

        @Specialization
        protected int localeCompare(Object thisObj, Object thatObj, Object locales, Object options,
                        @Cached JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            TruffleString thatStr = toString2Node.executeString(thatObj);
            JSCollatorObject collator = createCollator(locales, options);
            return JSCollator.compare(collator, Strings.toJavaString(thisStr), Strings.toJavaString(thatStr));
        }
    }

    /**
     * Implementation of the String.prototype.slice() method as specified by ECMAScript 5.1 in
     * 15.5.4.13.
     */
    public abstract static class JSStringSliceNode extends JSStringOperation {

        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();

        public JSStringSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object sliceStringIntInt(TruffleString thisObj, int start, int end,
                        @Cached @Shared InlinedConditionProfile offsetProfile1,
                        @Cached @Shared InlinedConditionProfile offsetProfile2,
                        @Cached @Shared InlinedConditionProfile canReturnEmpty) {
            int len = Strings.length(thisObj);
            int istart = JSRuntime.getOffset(start, len, this, offsetProfile1);
            int iend = JSRuntime.getOffset(end, len, this, offsetProfile2);
            if (canReturnEmpty.profile(this, iend > istart)) {
                return Strings.substring(getContext(), substringNode, thisObj, istart, iend - istart);
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        @Specialization(guards = "!isString(thisObj)", replaces = "sliceStringIntInt")
        protected Object sliceObjectIntInt(Object thisObj, int start, int end,
                        @Cached @Shared InlinedConditionProfile offsetProfile1,
                        @Cached @Shared InlinedConditionProfile offsetProfile2,
                        @Cached @Shared InlinedConditionProfile canReturnEmpty) {
            requireObjectCoercible(thisObj);
            return sliceStringIntInt(toString(thisObj), start, end,
                            offsetProfile1, offsetProfile2, canReturnEmpty);
        }

        @Specialization(guards = "isUndefined(end)")
        protected Object sliceStringIntUndefined(TruffleString str, int start, @SuppressWarnings("unused") Object end,
                        @Cached @Shared InlinedConditionProfile offsetProfile1,
                        @Cached @Shared InlinedConditionProfile canReturnEmpty) {
            int len = Strings.length(str);
            int istart = JSRuntime.getOffset(start, len, this, offsetProfile1);
            if (canReturnEmpty.profile(this, len > istart)) {
                return Strings.substring(getContext(), substringNode, str, istart, len - istart);
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        @Specialization(replaces = {"sliceStringIntInt", "sliceObjectIntInt", "sliceStringIntUndefined"})
        protected Object sliceGeneric(Object thisObj, Object start, Object end,
                        @Cached @Exclusive InlinedConditionProfile isUndefined,
                        @Cached @Shared InlinedConditionProfile canReturnEmpty,
                        @Cached @Shared InlinedConditionProfile offsetProfile1,
                        @Cached @Shared InlinedConditionProfile offsetProfile2) {
            requireObjectCoercible(thisObj);
            TruffleString s = toString(thisObj);

            long len = Strings.length(s);
            long istart = JSRuntime.getOffset(toIntegerAsInt(start), len, this, offsetProfile1);
            long iend = isUndefined.profile(this, end == Undefined.instance)
                            ? len
                            : JSRuntime.getOffset(toIntegerAsInt(end), len, this, offsetProfile2);
            if (canReturnEmpty.profile(this, iend > istart)) {
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

        @Specialization(guards = "isUndefined(position)")
        protected boolean startsWithString(TruffleString thisObj, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
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
                        @Cached @Shared TruffleString.RegionEqualByteIndexNode regionEqualsNode,
                        @Cached InlinedBranchProfile noStringBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter(this);
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

        @Specialization(guards = "isUndefined(position)")
        protected boolean endsWithStringUndefined(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared TruffleString.RegionEqualByteIndexNode regionEqualsNode) {
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
                        @Cached @Shared TruffleString.RegionEqualByteIndexNode regionEqualsNode,
                        @Cached InlinedBranchProfile noStringBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter(this);
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

        @Specialization(guards = "isUndefined(position)")
        protected boolean includesString(TruffleString thisStr, TruffleString searchStr, @SuppressWarnings("unused") Object position,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode indexOfStringNode) {
            return Strings.indexOf(indexOfStringNode, thisStr, searchStr) != -1;
        }

        @Specialization(guards = "!isStringString(thisObj, searchString) || !isUndefined(position)")
        protected boolean includesGeneric(Object thisObj, Object searchString, Object position,
                        @Cached JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode,
                        @Cached @Shared TruffleString.ByteIndexOfStringNode indexOfStringNode,
                        @Cached InlinedBranchProfile errorBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                errorBranch.enter(this);
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

        public JSStringRepeatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object repeat(Object thisObj, Object count,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached TruffleString.RepeatNode repeatNode,
                        @Cached InlinedBranchProfile errorBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            Number repeatCountN = toNumberNode.executeNumber(count);
            long repeatCount = JSRuntime.toInteger(repeatCountN);
            if (repeatCount < 0 || (repeatCountN instanceof Double && Double.isInfinite(repeatCountN.doubleValue()))) {
                errorBranch.enter(this);
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
                errorBranch.enter(this);
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

        @Specialization
        protected Object codePointAt(Object thisObj, Object position,
                        @Cached TruffleString.CodePointAtByteIndexNode codePointAtRawNode,
                        @Cached InlinedBranchProfile undefinedBranch) {
            requireObjectCoercible(thisObj);
            TruffleString thisStr = toString(thisObj);
            int pos = toIntegerAsInt(position);
            if (pos < 0 || Strings.length(thisStr) <= pos) {
                undefinedBranch.enter(this);
                return Undefined.instance;
            }
            return Strings.codePointAt(codePointAtRawNode, thisStr, pos);
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
            Normalizer.Form useForm;
            if (form == Undefined.instance) {
                useForm = Normalizer.Form.NFC;
            } else {
                TruffleString formStr = toString(form);
                if (Strings.equals(stringEqualsNode, formStr, Strings.NFC)) {
                    useForm = Normalizer.Form.NFC;
                } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFD)) {
                    useForm = Normalizer.Form.NFD;
                } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFKC)) {
                    useForm = Normalizer.Form.NFKC;
                } else if (Strings.equals(stringEqualsNode, formStr, Strings.NFKD)) {
                    useForm = Normalizer.Form.NFKD;
                } else {
                    throw Errors.createRangeError("The normalization form should be one of NFC, NFD, NFKC, NFKD.");
                }
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
            var sb = Strings.builderCreate(len);
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

    public abstract static class JSStringIsWellFormedNode extends JSStringOperation {

        public JSStringIsWellFormedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Object doString(TruffleString thisStr,
                        @Shared @Cached TruffleString.IsValidNode isValidNode) {
            return isValidNode.execute(thisStr, TruffleString.Encoding.UTF_16);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected final Object doOther(Object thisObj,
                        @Shared @Cached TruffleString.IsValidNode isValidNode) {
            requireObjectCoercible(thisObj);
            return doString(toString(thisObj), isValidNode);
        }
    }

    public abstract static class JSStringToWellFormedNode extends JSStringOperation {

        public JSStringToWellFormedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static TruffleString doString(TruffleString thisStr,
                        @Shared @Cached TruffleString.ToValidStringNode toValidNode) {
            return toValidNode.execute(thisStr, TruffleString.Encoding.UTF_16);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected final TruffleString doOther(Object thisObj,
                        @Shared @Cached TruffleString.ToValidStringNode toValidNode) {
            requireObjectCoercible(thisObj);
            return doString(toString(thisObj), toValidNode);
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

        public CreateStringIteratorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSObject doString(TruffleString thisObj) {
            return JSStringIterator.create(getContext(), getRealm(), thisObj, 0);
        }

        @Specialization(guards = "!isString(thisObj)")
        protected final JSObject doCoerce(Object thisObj,
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
