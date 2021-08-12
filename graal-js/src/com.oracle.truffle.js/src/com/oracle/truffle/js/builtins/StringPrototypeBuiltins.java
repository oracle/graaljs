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
package com.oracle.truffle.js.builtins;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
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
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

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
                return createHTMLNode(context, builtin, "a", "name");
            case big:
                return createHTMLNode(context, builtin, "big", "");
            case blink:
                return createHTMLNode(context, builtin, "blink", "");
            case bold:
                return createHTMLNode(context, builtin, "b", "");
            case fixed:
                return createHTMLNode(context, builtin, "tt", "");
            case fontcolor:
                return createHTMLNode(context, builtin, "font", "color");
            case fontsize:
                return createHTMLNode(context, builtin, "font", "size");
            case italics:
                return createHTMLNode(context, builtin, "i", "");
            case link:
                return createHTMLNode(context, builtin, "a", "href");
            case small:
                return createHTMLNode(context, builtin, "small", "");
            case strike:
                return createHTMLNode(context, builtin, "strike", "");
            case sub:
                return createHTMLNode(context, builtin, "sub", "");
            case sup:
                return createHTMLNode(context, builtin, "sup", "");

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

        protected String toString(Object target) {
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
        protected final ConditionProfile isSpecialProfile = ConditionProfile.createBinaryProfile();
        protected final ConditionProfile callSpecialProfile = ConditionProfile.createBinaryProfile();

        public JSStringOperationWithRegExpArgument(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final Object matchIgnoreLastIndex(DynamicObject regExp, String input, int fromIndex) {
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

        protected final Object invoke(DynamicObject regExp, Symbol symbol, String thisStr) {
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
                getMethodNode = insert(GetMethodNode.create(getContext(), null, key));
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

        public JSStringCharAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String stringCharAt(String thisStr, int pos) {
            if (indexOutOfBounds.profile(pos < 0 || pos >= thisStr.length())) {
                return "";
            } else {
                return String.valueOf(thisStr.charAt(pos));
            }
        }

        @Specialization
        protected String charAt(Object thisObj, Object index) {
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
            protected String charAt(Object thisObj, Object indexObj) {
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

        protected static boolean posInBounds(String thisStr, int pos) {
            return pos >= 0 && pos < thisStr.length();
        }

        @Specialization
        protected Object charCodeAtLazyString(JSLazyString thisStr, int index,
                        @Cached("createBinaryProfile()") ConditionProfile flatten) {
            if (indexOutOfBounds.profile(0 > index || index >= thisStr.length())) {
                return Double.NaN;
            } else {
                String s = thisStr.toString(flatten);
                return Integer.valueOf(s.charAt(index));
            }
        }

        @Specialization(guards = {"posInBounds(thisStr, pos)"})
        protected int charCodeAtInBounds(String thisStr, int pos) {
            return thisStr.charAt(pos);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!posInBounds(thisStr, pos)")
        protected double charCodeAtOutOfBounds(String thisStr, int pos) {
            return Double.NaN;
        }

        @Specialization(replaces = {"charCodeAtLazyString", "charCodeAtInBounds", "charCodeAtOutOfBounds"})
        protected Object charCodeAtGeneric(Object thisObj, Object indexObj,
                        @Cached("create()") JSToNumberNode toNumberNode) {
            requireObjectCoercible(thisObj);
            String s = toString(thisObj);
            Number index = toNumberNode.executeNumber(indexObj);
            long lIndex = JSRuntime.toInteger(index);
            if (indexOutOfBounds.profile(0 > lIndex || lIndex >= s.length())) {
                return Double.NaN;
            } else {
                return Integer.valueOf(s.charAt((int) lIndex));
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
                            @Cached("create()") JSToNumberNode toNumberNode) {
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

        public JSStringSubstringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String substring(String thisStr, int start, int end) {
            int len = thisStr.length();
            int finalStart = within(start, 0, len);
            int finalEnd = within(end, 0, len);
            return substringIntl(thisStr, finalStart, finalEnd);
        }

        @Specialization(guards = "isUndefined(end)")
        protected String substringStart(String thisStr, int start, @SuppressWarnings("unused") Object end) {
            int len = thisStr.length();
            int finalStart = within(start, 0, len);
            int finalEnd = len;
            return substringIntl(thisStr, finalStart, finalEnd);
        }

        private String substringIntl(String thisStr, int start, int end) {
            if (startLowerEnd.profile(start <= end)) {
                return Boundaries.substring(thisStr, start, end);
            } else {
                return Boundaries.substring(thisStr, end, start);
            }
        }

        @Specialization(replaces = {"substring", "substringStart"})
        protected String substringGeneric(Object thisObj, Object start, Object end,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached("create()") JSToNumberNode toNumber2Node,
                        @Cached("createBinaryProfile()") ConditionProfile startUndefined,
                        @Cached("createBinaryProfile()") ConditionProfile endUndefined) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            int len = thisStr.length();
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
            protected String substringGeneric(Object thisObj, Object start, Object end,
                            @Cached("create()") JSToNumberNode toNumberNode,
                            @Cached("create()") JSToNumberNode toNumber2Node,
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
        protected int indexOfStringUndefined(String thisStr, String searchStr, @SuppressWarnings("unused") Object position) {
            return Boundaries.stringIndexOf(thisStr, searchStr);
        }

        @Specialization
        protected int indexOfStringInt(String thisStr, String searchStr, int position) {
            return indexOfIntl(thisStr, searchStr, position);
        }

        @Specialization(replaces = {"indexOfStringInt"})
        // replace only the StringInt specialization that duplicates code
        protected int indexOfGeneric(Object thisObj, Object searchObj, Object position,
                        @Cached("create()") JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            String searchStr = toString2Node.executeString(searchObj);
            return indexOfIntl(thisStr, searchStr, position);
        }

        private int indexOfIntl(String thisStr, String searchStr, Object position) {
            int startPos;
            if (hasPos.profile(position != Undefined.instance)) {
                startPos = Math.min(toIntegerAsInt(position), thisStr.length());
            } else {
                startPos = 0;
            }
            return Boundaries.stringIndexOf(thisStr, searchStr, startPos);
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

        protected boolean isStringLength1(String str) {
            return str.length() == 1;
        }

        // search for a single-character string without position argument.
        // Use-case: path.js:normalizeString
        @Specialization(guards = {"isStringLength1(searchString)", "isUndefined(position)"})
        protected int lastIndexOfChar(String thisObj, String searchString, @SuppressWarnings("unused") Object position) {
            return lastIndexOfChar(thisObj, searchString, thisObj.length());
        }

        private static int lastIndexOfChar(String thisStr, String searchStr, int startPos) {
            assert searchStr.length() == 1;
            char searchChar = searchStr.charAt(0);
            int start = startPos < thisStr.length() ? startPos : thisStr.length() - 1;
            for (int i = start; i >= 0; i--) {
                if (thisStr.charAt(i) == searchChar) {
                    return i;
                }
            }
            return -1;
        }

        @Specialization(guards = "isUndefined(position)")
        protected int lastIndexOfString(String thisObj, String searchString, @SuppressWarnings("unused") Object position,
                        @Cached("createBinaryProfile()") @Shared("searchStrZero") ConditionProfile searchStrZero,
                        @Cached("createBinaryProfile()") @Shared("searchStrOne") ConditionProfile searchStrOne) {
            int len = thisObj.length();
            int pos = len;
            return lastIndexOfImpl(thisObj, searchString, pos, searchStrZero, searchStrOne);
        }

        @Specialization
        protected int lastIndexOfString(String thisObj, String searchString, int position,
                        @Cached("createBinaryProfile()") @Shared("searchStrZero") ConditionProfile searchStrZero,
                        @Cached("createBinaryProfile()") @Shared("searchStrOne") ConditionProfile searchStrOne) {
            int len = thisObj.length();
            int pos = within(position, 0, len);
            return lastIndexOfImpl(thisObj, searchString, pos, searchStrZero, searchStrOne);
        }

        @Specialization(replaces = {"lastIndexOfChar", "lastIndexOfString"})
        protected int lastIndexOf(Object thisObj, Object searchString, Object position,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached("createBinaryProfile()") ConditionProfile posNaN,
                        @Cached("createBinaryProfile()") @Shared("searchStrZero") ConditionProfile searchStrZero,
                        @Cached("createBinaryProfile()") @Shared("searchStrOne") ConditionProfile searchStrOne) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            String searchStr = toString2Node.executeString(searchString);
            Number numPos = toNumberNode.executeNumber(position);
            int len = thisStr.length();
            int pos;

            double dVal = JSRuntime.doubleValue(numPos);
            if (posNaN.profile(Double.isNaN(dVal))) {
                pos = len;
            } else {
                pos = within((int) dVal, 0, len);
            }
            return lastIndexOfImpl(thisStr, searchStr, pos, searchStrZero, searchStrOne);
        }

        private static int lastIndexOfImpl(String thisStr, String searchStr, int pos, ConditionProfile searchStrZero, ConditionProfile searchStrOne) {
            if (searchStrZero.profile(searchStr.length() == 0)) {
                return pos;
            } else if (searchStrOne.profile(searchStr.length() == 1)) {
                return lastIndexOfChar(thisStr, searchStr, pos);
            } else {
                return Boundaries.stringLastIndexOf(thisStr, searchStr, pos);
            }
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

        private int toUInt32(Object target) {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return (int) Math.min(Integer.MAX_VALUE, JSRuntime.toInteger((Number) toUInt32Node.execute(target)));
        }

        private String toString2(Object obj) {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node.executeString(obj);
        }

        protected boolean isES6OrNewer() {
            return getContext().getEcmaScriptVersion() >= 6;
        }

        @Specialization(guards = "!isES6OrNewer()")
        protected Object splitES5(Object thisObj, Object separator, Object limitObj) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            int limit = getLimit(limitObj);
            if (separator == Undefined.instance) {
                isUndefinedBranch.enter();
                return split(thisStr, limit, NOP_SPLITTER, null);
            } else if (JSRegExp.isJSRegExp(separator)) {
                isRegexpBranch.enter();
                return split(thisStr, limit, REGEXP_SPLITTER, (DynamicObject) separator);
            } else {
                isStringBranch.enter();
                String separatorStr = toString2(separator);
                return split(thisStr, limit, STRING_SPLITTER, separatorStr);
            }
        }

        protected boolean isFastPath(Object thisObj, Object separator, Object limit) {
            return JSRuntime.isString(thisObj) && JSRuntime.isString(separator) && limit == Undefined.instance;
        }

        @Specialization(guards = {"isES6OrNewer()", "isFastPath(thisStr, sepStr, limit)"})
        protected Object splitES6StrStrUndefined(String thisStr, String sepStr, @SuppressWarnings("unused") DynamicObject limit) {
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
            String thisStr = toString(thisObj);
            int lim = getLimit(limit);
            String sepStr = toString2(separator);
            if (separator == Undefined.instance) {
                return split(thisStr, lim, NOP_SPLITTER, null);
            } else {
                return split(thisStr, lim, STRING_SPLITTER, sepStr);
            }
        }

        private int getLimit(Object limit) {
            return (limit == Undefined.instance) ? Integer.MAX_VALUE : toUInt32(limit);
        }

        private <T> DynamicObject split(String thisStr, int limit, Splitter<T> splitter, T separator) {
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
            Object[] split(String input, int limit, T separator, JSStringSplitNode parent);
        }

        private static final Splitter<Void> NOP_SPLITTER = (input, limit, separator, parent) -> new Object[]{input};
        private static final Splitter<String> STRING_SPLITTER = new StringSplitter();
        private static final Splitter<DynamicObject> REGEXP_SPLITTER = new RegExpSplitter();

        private static final class StringSplitter implements Splitter<String> {
            @Override
            public Object[] split(String input, int limit, String separator, JSStringSplitNode parent) {
                if (parent.emptySeparator.profile(separator.isEmpty())) {
                    return individualCharSplit(input, limit);
                } else {
                    return regularSplit(input, limit, separator, parent);
                }
            }

            private static Object[] regularSplit(String input, int limit, String separator, JSStringSplitNode parent) {
                int end = input.indexOf(separator);
                if (parent.matchProfile.profile(end == -1)) {
                    return new Object[]{input};
                }
                return regularSplitIntl(input, limit, separator, end, parent);
            }

            @TruffleBoundary
            private static Object[] regularSplitIntl(String input, int limit, String separator, int endParam, JSStringSplitNode parent) {
                SimpleArrayList<String> splits = SimpleArrayList.create(limit);
                int start = 0;
                int end = endParam;
                while (end != -1) {
                    splits.add(input.substring(start, end), parent.growProfile);
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = end + separator.length();
                    end = input.indexOf(separator, start);
                }
                splits.add(input.substring(start), parent.growProfile);
                return splits.toArray();
            }

            private static Object[] individualCharSplit(String input, int limit) {
                int len = Math.min(input.length(), limit);
                Object[] array = new Object[len];
                for (int i = 0; i < len; i++) {
                    array[i] = String.valueOf(input.charAt(i));
                }
                return array;
            }
        }

        private static final class RegExpSplitter implements Splitter<DynamicObject> {
            private static final Object[] EMPTY_SPLITS = {};
            private static final Object[] SINGLE_ZERO_LENGTH_SPLIT = {""};

            @Override
            public Object[] split(String input, int limit, DynamicObject regExp, JSStringSplitNode parent) {
                if (parent.emptyInput.profile(input.isEmpty())) {
                    return splitEmptyString(regExp, parent);
                } else {
                    return splitNonEmptyString(input, limit, regExp, parent);
                }
            }

            private static Object[] splitEmptyString(DynamicObject regExp, JSStringSplitNode parent) {
                Object result = parent.matchIgnoreLastIndex(regExp, "", 0);
                return parent.matchProfile.profile(parent.getResultAccessor().isMatch(result)) ? EMPTY_SPLITS : SINGLE_ZERO_LENGTH_SPLIT;
            }

            private static Object[] splitNonEmptyString(String input, int limit, DynamicObject regExp, JSStringSplitNode parent) {
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
                        if (matchStart == input.length() - 1) {
                            break;
                        }
                        result = parent.matchIgnoreLastIndex(regExp, input, start + 1);
                        continue;
                    }
                    String split = Boundaries.substring(input, start, matchStart);
                    splits.add(split, parent.growProfile);
                    int count = Math.min(parent.getCompiledRegexAccessor().groupCount(JSRegExp.getCompiledRegex(regExp)) - 1, limit - splits.size());
                    for (int i = 1; i <= count; i++) {
                        int groupStart = parent.getResultAccessor().captureGroupStart(result, i);
                        if (groupStart == TRegexUtil.Constants.CAPTURE_GROUP_NO_MATCH) {
                            splits.add(Undefined.instance, parent.growProfile);
                        } else {
                            splits.add(Boundaries.substring(input, groupStart, parent.getResultAccessor().captureGroupEnd(result, i)), parent.growProfile);
                        }
                    }
                    if (splits.size() == limit) {
                        return splits.toArray();
                    }
                    start = matchEnd + (matchEnd == start ? 1 : 0);
                    result = parent.matchIgnoreLastIndex(regExp, input, start);
                }
                splits.add(Boundaries.substring(input, start), parent.growProfile);
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
        protected String concat(Object thisObj, Object[] args,
                        @Cached("create()") JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            StringBuilder builder = stringBuilderProfile.newStringBuilder();
            stringBuilderProfile.append(builder, toString(thisObj));
            for (Object o : args) {
                stringBuilderProfile.append(builder, toString2Node.executeString(o));
                TruffleSafepoint.poll(this);
            }
            return stringBuilderProfile.toString(builder);
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
        protected final ValueProfile searchValueProfile = ValueProfile.createIdentityProfile();
        protected final ValueProfile replaceValueProfile = ValueProfile.createIdentityProfile();

        public JSStringReplaceBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static ReplaceStringParser.Token[] parseReplaceValue(String replaceValue) {
            return ReplaceStringParser.parse(replaceValue, 0, false);
        }

        protected static void appendSubstitution(StringBuilder sb, String input, String replaceStr, String matched, int pos, BranchProfile dollarProfile) {
            ReplaceStringParser.process(replaceStr, 0, false, dollarProfile, new ReplaceStringConsumer(sb, input, replaceStr, matched, pos), null);
        }

        protected static final class ReplaceStringConsumer implements ReplaceStringParser.Consumer<Void> {

            private final StringBuilder sb;
            private final String input;
            private final String replaceStr;
            private final String matched;
            private final int matchedPos;

            private ReplaceStringConsumer(StringBuilder sb, String input, String replaceStr, String matched, int matchedPos) {
                this.sb = sb;
                this.input = input;
                this.replaceStr = replaceStr;
                this.matched = matched;
                this.matchedPos = matchedPos;
            }

            @Override
            public void literal(Void node, int start, int end) {
                Boundaries.builderAppend(sb, replaceStr, start, end);
            }

            @Override
            public void match(Void node) {
                Boundaries.builderAppend(sb, matched);
            }

            @Override
            public void matchHead(Void node) {
                Boundaries.builderAppend(sb, input, 0, matchedPos);
            }

            @Override
            public void matchTail(Void node) {
                Boundaries.builderAppend(sb, input, matchedPos + matched.length(), input.length());
            }

            @Override
            public void captureGroup(Void node, int groupNumber, int literalStart, int literalEnd) {
                throw Errors.shouldNotReachHere();
            }

            @Override
            public void namedCaptureGroup(Void node, String groupName) {
                throw Errors.shouldNotReachHere();
            }
        }

        protected final Object functionReplaceCall(Object splitter, Object separator, Object[] args) {
            if (functionReplaceCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionReplaceCallNode = insert(JSFunctionCallNode.createCall());
            }
            return functionReplaceCallNode.executeCall(JSArguments.create(separator, splitter, args));
        }

    }

    /**
     * Implementation of the String.prototype.replace() method as specified by ECMAScript 5.1 in
     * 15.5.4.11.
     */
    public abstract static class JSStringReplaceNode extends JSStringReplaceBaseNode {

        public JSStringReplaceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "cachedReplaceValue.equals(replaceValue)")
        protected Object replaceStringCached(Object thisObj, String searchValue, @SuppressWarnings("unused") String replaceValue,
                        @Cached("replaceValue") String cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, String searchValue, String replaceValue) {
            requireObjectCoercible(thisObj);
            return builtinReplaceString(searchValue, replaceValue, thisObj, null);
        }

        // have a guard instead of a replaces, that removes the other specializations
        protected boolean isStringString(Object arg1, Object arg2) {
            return JSRuntime.isString(arg1) && JSRuntime.isString(arg2);
        }

        @Specialization(guards = "!isStringString(searchValue, replaceValue)")
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            Object searchVal = searchValueProfile.profile(searchValue);
            Object replaceVal = replaceValueProfile.profile(replaceValue);
            if (isSpecialProfile.profile(!(searchVal == Undefined.instance || searchVal == Null.instance))) {
                Object replacer = getMethod(searchVal, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(replacer != Undefined.instance)) {
                    return call(replacer, searchVal, new Object[]{thisObj, replaceVal});
                }
            }
            // all child nodes must be checked to avoid race conditions on shared ASTs
            if (toString2Node == null || toString3Node == null || isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
                toString3Node = insert(JSToStringNode.create());
                isCallableNode = insert(IsCallableNode.create());
            }
            return builtinReplace(searchVal, replaceVal, thisObj);
        }

        private String builtinReplace(Object searchValue, Object replParam, Object o) {
            String string = toString(o);
            String searchString = toString2Node.executeString(searchValue);
            boolean functionalReplace = isCallableNode.executeBoolean(replParam);
            String replaceString = null;
            if (!functionalReplaceProfile.profile(functionalReplace)) {
                replaceString = toString3Node.executeString(replParam);
            }
            int pos = string.indexOf(searchString);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                return string;
            }
            StringBuilder sb = new StringBuilder(pos + (string.length() - (pos + searchString.length())) + 20);
            Boundaries.builderAppend(sb, string, 0, pos);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, pos, string});
                Boundaries.builderAppend(sb, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(sb, string, replaceString, searchString, pos, dollarProfile);
            }
            Boundaries.builderAppend(sb, string, pos + searchString.length(), string.length());
            return Boundaries.builderToString(sb);
        }

        private String builtinReplaceString(String searchString, String replaceString, Object o, ReplaceStringParser.Token[] parsedReplaceParam) {
            String input = toString(o);
            int pos = input.indexOf(searchString);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                return input;
            }
            StringBuilder sb = new StringBuilder(pos + (input.length() - (pos + searchString.length())) + 20);
            Boundaries.builderAppend(sb, input, 0, pos);
            if (parsedReplaceParam == null) {
                appendSubstitution(sb, input, replaceString, searchString, pos, dollarProfile);
            } else {
                ReplaceStringParser.processParsed(parsedReplaceParam, new ReplaceStringConsumer(sb, input, replaceString, searchString, pos), null);
            }
            Boundaries.builderAppend(sb, input, pos + searchString.length(), input.length());
            return Boundaries.builderToString(sb);
        }
    }

    public abstract static class JSStringReplaceAllNode extends JSStringReplaceBaseNode {
        private final ConditionProfile isSearchValueEmpty = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorBranch = BranchProfile.create();

        @Child private IsRegExpNode isRegExpNode;
        @Child private PropertyGetNode getFlagsNode;

        public JSStringReplaceAllNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "cachedReplaceValue.equals(replaceValue)")
        protected Object replaceStringCached(Object thisObj, String searchValue, @SuppressWarnings("unused") String replaceValue,
                        @Cached("replaceValue") String cachedReplaceValue,
                        @Cached(value = "parseReplaceValue(replaceValue)", dimensions = 1) ReplaceStringParser.Token[] cachedParsedReplaceValue) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, cachedReplaceValue, thisObj, cachedParsedReplaceValue);
        }

        @Specialization(replaces = "replaceStringCached")
        protected Object replaceString(Object thisObj, String searchValue, String replaceValue) {
            requireObjectCoercible(thisObj);
            return performReplaceAll(searchValue, replaceValue, thisObj, null);
        }

        protected Object performReplaceAll(String searchValue, String replaceValue, Object thisObj, ReplaceStringParser.Token[] parsedReplaceParam) {
            String thisStr = toString(thisObj);
            if (isSearchValueEmpty.profile(searchValue.isEmpty())) {
                return Boundaries.stringReplaceAll(thisStr, "", replaceValue);
            }
            StringBuilder result = new StringBuilder();
            int position = 0;
            while (position < thisStr.length()) {
                position = builtinReplaceString(searchValue, replaceValue, thisStr, parsedReplaceParam, position, result);
            }
            return Boundaries.builderToString(result);
        }

        @Specialization(replaces = {"replaceString", "replaceStringCached"})
        protected Object replaceGeneric(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            Object searchVal = searchValueProfile.profile(searchValue);
            Object replaceVal = replaceValueProfile.profile(replaceValue);
            if (isSpecialProfile.profile(!(searchVal == Undefined.instance || searchVal == Null.instance))) {
                if (isRegExp.profile(getIsRegExpNode().executeBoolean(searchValue))) {
                    Object flags = getFlags(searchValue);
                    requireObjectCoercible(flags);
                    if (toString(flags).indexOf('g') == -1) {
                        errorBranch.enter();
                        throw Errors.createTypeError("Only global regexps allowed");
                    }
                }
                Object replacer = getMethod(searchVal, Symbol.SYMBOL_REPLACE);
                if (callSpecialProfile.profile(replacer != Undefined.instance)) {
                    return call(replacer, searchVal, new Object[]{thisObj, replaceVal});
                }
            }
            // all child nodes must be checked to avoid race conditions on shared ASTs
            if (toString2Node == null || toString3Node == null || isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
                toString3Node = insert(JSToStringNode.create());
                isCallableNode = insert(IsCallableNode.create());
            }
            return performReplaceAllGeneric(searchVal, replaceVal, thisObj);
        }

        protected Object performReplaceAllGeneric(Object searchValue, Object replParam, Object thisObj) {
            String thisStr = toString(thisObj);
            String searchString = toString2Node.executeString(searchValue);
            StringBuilder result = new StringBuilder();
            int position = 0;

            boolean functionalReplace = isCallableNode.executeBoolean(replParam);
            Object replaceValue;
            if (functionalReplaceProfile.profile(functionalReplace)) {
                replaceValue = replParam;
            } else {
                replaceValue = toString3Node.executeString(replParam);
            }
            if (isSearchValueEmpty.profile(searchString.isEmpty())) {
                while (position <= thisStr.length()) {
                    builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, result);
                    if (position < thisStr.length()) {
                        Boundaries.builderAppend(result, thisStr.charAt(position));
                    }
                    ++position;
                }
                return Boundaries.builderToString(result);
            }
            while (position < thisStr.length()) {
                position = builtinReplace(searchString, functionalReplace, replaceValue, thisStr, position, result);
            }
            return Boundaries.builderToString(result);
        }

        private int builtinReplace(String searchString, boolean functionalReplace, Object replParam, String input, int position, StringBuilder result) {
            int pos = input.indexOf(searchString, position);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                Boundaries.builderAppend(result, input, position, input.length());
                return input.length();
            }
            Boundaries.builderAppend(result, input, position, pos);
            if (functionalReplaceProfile.profile(functionalReplace)) {
                Object replValue = functionReplaceCall(replParam, Undefined.instance, new Object[]{searchString, pos, input});
                Boundaries.builderAppend(result, toString3Node.executeString(replValue));
            } else {
                appendSubstitution(result, input, (String) replParam, searchString, pos, dollarProfile);
            }
            return pos + searchString.length();
        }

        private int builtinReplaceString(String searchString, String replaceString, String input, ReplaceStringParser.Token[] parsedReplaceParam, int position, StringBuilder result) {
            int pos = input.indexOf(searchString, position);
            if (replaceNecessaryProfile.profile(pos < 0)) {
                Boundaries.builderAppend(result, input, position, input.length());
                return input.length();
            }
            Boundaries.builderAppend(result, input, position, pos);
            if (parsedReplaceParam == null) {
                appendSubstitution(result, input, replaceString, searchString, pos, dollarProfile);
            } else {
                ReplaceStringParser.processParsed(parsedReplaceParam, new ReplaceStringConsumer(result, input, replaceString, searchString, pos), null);
            }
            return pos + searchString.length();
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
    public abstract static class JSStringReplaceES5Node extends JSStringOperationWithRegExpArgument {
        @Child private PropertySetNode setLastIndexNode;
        @Child private StringReplacer stringReplacerNode;
        @Child private FunctionReplacer functionReplacerNode;
        @Child private JSToStringNode toString2Node;
        @Child private JSToStringNode toString3Node;
        @Child private TRegexUtil.TRegexCompiledRegexSingleFlagAccessor globalFlagAccessor = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(TRegexUtil.Props.Flags.GLOBAL);
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createCountingProfile();
        private final ConditionProfile isFnRepl = ConditionProfile.createCountingProfile();

        public JSStringReplaceES5Node(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            assert context.getEcmaScriptVersion() < 6;
        }

        @Specialization
        protected String replace(Object thisObj, Object searchValue, Object replaceValue) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (thisStr.length() > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            if (isRegExp.profile(JSRegExp.isJSRegExp(searchValue))) {
                JSRegExpObject searchRegExp = (JSRegExpObject) searchValue;
                int groupCount = compiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(searchRegExp));
                if (isFnRepl.profile(JSFunction.isJSFunction(replaceValue))) {
                    DynamicObject replaceFunc = (DynamicObject) replaceValue;
                    if (globalFlagAccessor.get(JSRegExp.getCompiledRegex(searchRegExp))) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getFunctionReplacerNode(), replaceFunc);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, getFunctionReplacerNode(), replaceFunc);
                    }
                } else {
                    String replaceStr = toString3(replaceValue);
                    if (globalFlagAccessor.get(JSRegExp.getCompiledRegex(searchRegExp))) {
                        return replaceAll(searchRegExp, thisStr, groupCount, getStringReplacerNode(), replaceStr);
                    } else {
                        return replaceFirst(thisStr, searchRegExp, getStringReplacerNode(), replaceStr);
                    }
                }
            } else {
                String searchStr = toString2(searchValue);
                if (isFnRepl.profile(JSFunction.isJSFunction(replaceValue))) {
                    return replaceFirst(thisStr, searchStr, getFunctionReplacerNode(), (DynamicObject) replaceValue);
                } else {
                    String replaceStr = toString3(replaceValue);
                    return replaceFirst(thisStr, searchStr, getStringReplacerNode(), replaceStr);
                }
            }
        }

        private String toString2(Object obj) {
            if (toString2Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString2Node = insert(JSToStringNode.create());
            }
            return toString2Node.executeString(obj);
        }

        private String toString3(Object obj) {
            if (toString3Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toString3Node = insert(JSToStringNode.create());
            }
            return toString3Node.executeString(obj);
        }

        private void setLastIndex(DynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            setLastIndexNode.setValueInt(regExp, value);
        }

        private StringReplacer getStringReplacerNode() {
            if (stringReplacerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringReplacerNode = insert(StringReplacer.create());
            }
            return stringReplacerNode;
        }

        private FunctionReplacer getFunctionReplacerNode() {
            if (functionReplacerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionReplacerNode = insert(FunctionReplacer.create());
            }
            return functionReplacerNode;
        }

        private <T> String replaceFirst(String thisStr, String searchStr, Replacer<T> replacer, T replaceValue) {
            int start = thisStr.indexOf(searchStr);
            if (match.profile(start < 0)) {
                return thisStr;
            }
            int end = start + searchStr.length();
            StringBuilder sb = new StringBuilder(thisStr.length() * 2);
            Boundaries.builderAppend(sb, thisStr, 0, start);
            replacer.appendReplacement(sb, thisStr, searchStr, start, replaceValue);
            Boundaries.builderAppend(sb, thisStr, end, thisStr.length());
            return Boundaries.builderToString(sb);
        }

        private <T> String replaceFirst(String thisStr, JSRegExpObject regExp, Replacer<T> replacer, T replaceValue) {
            Object result = match(regExp, thisStr);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return thisStr;
            }
            return replace(thisStr, result, compiledRegexAccessor.groupCount(JSRegExp.getCompiledRegex(regExp)), replacer, replaceValue);
        }

        protected final Object match(JSRegExpObject regExp, String input) {
            assert getContext().getEcmaScriptVersion() <= 5;
            return getRegExpNode().execute(regExp, input);
        }

        private <T> String replace(String thisStr, Object result, int groupCount, Replacer<T> replacer, T replaceValue) {
            StringBuilder sb = new StringBuilder(replacer.guessResultLength(result, replaceValue, thisStr));
            Boundaries.builderAppend(sb, thisStr, 0, resultAccessor.captureGroupStart(result, 0));
            replacer.appendReplacement(sb, thisStr, result, groupCount, replaceValue);
            Boundaries.builderAppend(sb, thisStr, resultAccessor.captureGroupEnd(result, 0), thisStr.length());
            return Boundaries.builderToString(sb);
        }

        private <T> String replaceAll(DynamicObject regExp, String input, int groupCount, Replacer<T> replacer, T replaceValue) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return input;
            }
            StringBuilder sb = new StringBuilder(replacer.guessResultLength(result, replaceValue, input));
            int thisIndex = 0;
            int lastIndex = 0;
            while (resultAccessor.isMatch(result)) {
                Boundaries.builderAppend(sb, input, thisIndex, resultAccessor.captureGroupStart(result, 0));
                replacer.appendReplacement(sb, input, result, groupCount, replaceValue);
                if (sb.length() > getContext().getStringLengthLimit()) {
                    CompilerDirectives.transferToInterpreter();
                    throw Errors.createRangeErrorInvalidStringLength();
                }
                thisIndex = resultAccessor.captureGroupEnd(result, 0);
                if (thisIndex == input.length() && resultAccessor.captureGroupLength(result, 0) == 0) {
                    // Avoid getting empty match at end of string twice.
                    break;
                }
                lastIndex = thisIndex + (thisIndex == lastIndex ? 1 : 0);
                result = matchIgnoreLastIndex(regExp, input, lastIndex);
            }
            Boundaries.builderAppend(sb, input, thisIndex, input.length());
            return Boundaries.builderToString(sb);
        }

        private abstract static class Replacer<T> extends JavaScriptBaseNode {

            @Child TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
            @Child TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();
            @Child TRegexUtil.TRegexMaterializeResultNode resultMaterializer = TRegexUtil.TRegexMaterializeResultNode.create();

            protected final ConditionProfile emptyReplace = ConditionProfile.createBinaryProfile();
            protected final ConditionProfile groups = ConditionProfile.createBinaryProfile();
            protected final BranchProfile replaceDollar = BranchProfile.create();

            int guessResultLength(@SuppressWarnings("unused") Object result, @SuppressWarnings("unused") T replaceValue, String input) {
                return input.length() * 2;
            }

            abstract void appendReplacement(StringBuilder sb, String input, Object result, int groupCount, T replaceValue);

            abstract void appendReplacement(StringBuilder sb, String input, String matchedString, int pos, T replaceValue);
        }

        protected static final class StringReplacer extends Replacer<String> {

            private final BranchProfile dollarProfile = BranchProfile.create();

            private StringReplacer() {
            }

            public static StringReplacer create() {
                return new StringReplacer();
            }

            @Override
            int guessResultLength(Object result, String replaceStr, String input) {
                if (replaceStr.isEmpty()) {
                    return input.length() - resultAccessor.captureGroupLength(result, 0);
                } else {
                    return input.length() * 2;
                }
            }

            @Override
            void appendReplacement(StringBuilder sb, String input, Object result, int groupCount, String replaceStr) {
                if (emptyReplace.profile(!replaceStr.isEmpty())) {
                    int pos = nextDollar(sb, 0, replaceStr);
                    while (pos != -1) {
                        replaceDollar.enter();
                        pos = appendSubstitution(sb, input, pos + 1, groupCount, result, replaceStr);
                        pos = nextDollar(sb, pos, replaceStr);
                    }
                }
            }

            @Override
            void appendReplacement(StringBuilder sb, String input, String matchedString, int pos, String replaceValue) {
                JSStringReplaceNode.appendSubstitution(sb, input, replaceValue, matchedString, pos, dollarProfile);
            }

            private int appendSubstitution(StringBuilder sb, String input, int pos, int groupCount, Object result, String replaceStr) {
                if (pos == replaceStr.length()) {
                    Boundaries.builderAppend(sb, '$');
                    return pos;
                }

                char ch = replaceStr.charAt(pos);
                switch (ch) {
                    case '$':
                        Boundaries.builderAppend(sb, '$');
                        break;
                    case '&':
                        Boundaries.builderAppend(sb, (String) resultMaterializer.materializeGroup(result, 0, input));
                        break;
                    case '`':
                        Boundaries.builderAppend(sb, input, 0, resultAccessor.captureGroupStart(result, 0));
                        break;
                    case '\'':
                        Boundaries.builderAppend(sb, input, resultAccessor.captureGroupEnd(result, 0), input.length());
                        break;
                    default:
                        if (groups.profile(Boundaries.characterIsDigit(ch))) {
                            return pos + appendGroup(sb, input, pos + 1, ch, groupCount, result, replaceStr);
                        } else {
                            Boundaries.builderAppend(sb, '$');
                            Boundaries.builderAppend(sb, ch);
                        }
                }
                return pos + 1;
            }

            private static int nextDollar(StringBuilder sb, int start, String replaceStr) {
                int pos = replaceStr.indexOf('$', start);
                int end = (pos == -1) ? replaceStr.length() : pos;
                Boundaries.builderAppend(sb, replaceStr, start, end);
                return pos;
            }

            // Returns 2 for valid two digit group references ($nn), otherwise returns 1.
            private int appendGroup(StringBuilder sb, String input, int pos, char digit, int groupCount, Object result, String replaceStr) {
                int groupNr = parseGroupNr(replaceStr, pos, digit, groupCount - 1);
                if (groupNr == -1) {
                    Boundaries.builderAppend(sb, '$');
                    Boundaries.builderAppend(sb, digit);
                    return 1;
                }
                String group = (String) resultMaterializer.materializeGroup(result, groupNr, input);
                Boundaries.builderAppend(sb, group);
                return (groupNr > 9) ? 2 : 1;
            }

            // 15.5.4.11 String.prototype.replace, http://es5.github.io/#x15.5.4.11
            // Behavior in case n1/n2 >= groupNr is "implementation-defined" acc. to Table 22.
            private static int parseGroupNr(String str, int pos, char digit, int groupCount) {
                int n = toInt(digit);
                if (n > groupCount) {
                    return -1;
                }

                if (pos < str.length()) {
                    char ch = str.charAt(pos);
                    if (Boundaries.characterIsDigit(ch)) {
                        int nn = n * 10 + toInt(ch);
                        if (nn < groupCount) {
                            return nn;
                        }
                    }
                }
                return n;
            }

            private static int toInt(char digit) {
                return digit - '0';
            }
        }

        protected static final class FunctionReplacer extends Replacer<DynamicObject> {
            @Child private JSFunctionCallNode functionCallNode = JSFunctionCallNode.createCall();
            @Child private JSToStringNode toStringNode = JSToStringNode.create();

            private FunctionReplacer() {
            }

            public static FunctionReplacer create() {
                return new FunctionReplacer();
            }

            @Override
            void appendReplacement(StringBuilder sb, String input, Object result, int groupCount, DynamicObject replaceFunc) {
                String replaceStr = callReplaceValueFunc(result, input, groupCount, replaceFunc);
                Boundaries.builderAppend(sb, replaceStr);
            }

            @Override
            void appendReplacement(StringBuilder sb, String input, String matchedString, int pos, DynamicObject replaceFunc) {
                Object[] arguments = createArguments(new Object[]{matchedString}, pos, input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                String replaceStr = toStringNode.executeString(replaceValue);
                Boundaries.builderAppend(sb, replaceStr);
            }

            private String callReplaceValueFunc(Object result, String input, int groupCount, DynamicObject replaceFunc) {
                Object[] matches = resultMaterializer.materializeFull(result, groupCount, input);
                Object[] arguments = createArguments(matches, resultAccessor.captureGroupStart(result, 0), input, replaceFunc);
                Object replaceValue = functionCallNode.executeCall(arguments);
                return toStringNode.executeString(replaceValue);
            }

            private static Object[] createArguments(Object[] matches, int matchIndex, CharSequence input, DynamicObject replaceFunc) {
                DynamicObject target = Undefined.instance;
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

        protected abstract String executeString(Object charSequence);

        private String executeString(CharSequence charSequence) {
            // workaround: `protected abstract String executeString(CharSequence charSequence)`
            // does not give us the desired implicit cast specialization
            return executeString((Object) charSequence);
        }

        @Specialization(guards = "isJSString(thisStr)")
        protected String toStringString(DynamicObject thisStr, @Cached("createStringToString(getContext())") JSStringToStringNode nestedToString) {
            // using nested toString node to specialize on exact type of the CharSequence
            return nestedToString.executeString(JSString.getCharSequence(thisStr));
        }

        @Specialization
        @TruffleBoundary
        protected String toStringCharseq(CharSequence thisStr) {
            return thisStr.toString();
        }

        @Specialization(guards = "isForeignObject(thisObj)", limit = "InteropLibraryLimit")
        protected String toStringForeignObject(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            if (interop.isString(thisObj)) {
                try {
                    return interop.asString(thisObj);
                } catch (UnsupportedMessageException ex) {
                    throw Errors.createTypeErrorUnboxException(thisObj, ex, this);
                }
            }
            return toStringOther(thisObj);
        }

        @Fallback
        protected String toStringOther(@SuppressWarnings("unused") Object thisObj) {
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
        protected String toLowerCaseString(String thisStr) {
            return toLowerCaseIntl(thisStr);
        }

        @Specialization(replaces = "toLowerCaseString")
        protected String toLowerCase(Object thisObj) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            return toLowerCaseIntl(thisStr);
        }

        private String toLowerCaseIntl(String str) {
            return Boundaries.stringToLowerCase(str, locale ? getContext().getLocale() : Locale.US);
        }
    }

    public abstract static class JSStringToLocaleXCaseIntl extends JSStringOperation {

        @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;

        public JSStringToLocaleXCaseIntl(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        }

        @Specialization
        protected String toDesiredCase(Object thisObj, Object locale) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (thisStr == null || thisStr.isEmpty()) {
                return thisStr;
            }
            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(locale);
            return toXCase(thisStr, locales);
        }

        @SuppressWarnings("unused")
        protected String toXCase(String thisStr, String[] locales) {
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
        protected String toXCase(String thisStr, String[] locales) {
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
        protected String toXCase(String thisStr, String[] locales) {
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
        protected String toUpperCaseString(String thisStr) {
            return toUpperCaseIntl(thisStr);
        }

        @Specialization(replaces = "toUpperCaseString")
        protected String toUpperCaseGeneric(Object thisObj) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            return toUpperCaseIntl(thisStr);
        }

        private String toUpperCaseIntl(String str) {
            return Boundaries.stringToUpperCase(str, locale ? getContext().getLocale() : Locale.US);
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
            String thisStr = toString(thisObj);
            Object cRe = getCompileRegexNode().compile(regex == Undefined.instance ? "" : toString(regex));
            DynamicObject regExp = getCreateRegExpNode().createRegExp(cRe);
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
            String thisStr = toString(thisObj);
            DynamicObject regExp = toRegExpNode.execute(searchObj);
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

        public JSStringSubstrNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String substrInt(String thisStr, int start, int length) {
            return substrIntl(thisStr, start, length);
        }

        @Specialization(guards = "isUndefined(length)")
        protected String substrLenUndef(String thisStr, int start, @SuppressWarnings("unused") Object length) {
            return substrIntl(thisStr, start, thisStr.length());
        }

        @Specialization(replaces = {"substrInt", "substrLenUndef"})
        protected String substrGeneric(Object thisObj, Object start, Object length) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            int startInt = toIntegerAsInt(start);
            int len = (length == Undefined.instance) ? thisStr.length() : toIntegerAsInt(length);
            return substrIntl(thisStr, startInt, len);
        }

        private String substrIntl(String thisStr, int start, int length) {
            int startInt = start;
            if (startInt < 0) {
                startNegativeBranch.enter();
                startInt = Math.max(startInt + thisStr.length(), 0);
            }
            int finalLen = within(length, 0, Math.max(0, thisStr.length() - startInt));
            if (finalLen <= 0) {
                finalLenEmptyBranch.enter();
                return "";
            }
            return Boundaries.substring(thisStr, startInt, startInt + finalLen);
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
                    if (toString(flags).indexOf('g') == -1) {
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
            String thisStr = toString(thisObj);
            Object cRe = getCompileRegexNode().compile(regex == Undefined.instance ? "" : toString(regex), matchAll ? "g" : "");
            DynamicObject regExp = getCreateRegExpNode().createRegExp(cRe);
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

        private void setLastIndex(DynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, getContext(), true));
            }
            setLastIndexNode.setValue(regExp, value);
        }

        @Specialization
        protected DynamicObject matchRegExpNotGlobal(Object thisObj, Object searchObj) {
            requireObjectCoercible(thisObj);
            if (isGlobalRegExp.profile(JSRegExp.isJSRegExp(searchObj) && globalFlagAccessor.get(JSRegExp.getCompiledRegex((DynamicObject) searchObj)))) {
                String thisStr = toString(thisObj);
                return matchAll((DynamicObject) searchObj, thisStr);
            } else {
                return matchNotRegExpIntl(thisObj, searchObj);
            }
        }

        private DynamicObject matchNotRegExpIntl(Object thisObj, Object searchObj) {
            String thisStr = toString(thisObj);
            JSRegExpObject regExp = toRegExpNode.execute(searchObj);
            return regExpExecNode.exec(regExp, thisStr);
        }

        private DynamicObject matchAll(DynamicObject regExp, String input) {
            setLastIndex(regExp, 0);
            Object result = matchIgnoreLastIndex(regExp, input, 0);
            if (match.profile(!resultAccessor.isMatch(result))) {
                return Null.instance;
            }
            List<String> matches = new ArrayList<>();
            int lastIndex = 0;
            while (resultAccessor.isMatch(result)) {
                Boundaries.listAdd(matches, (String) resultMaterializer.materializeGroup(result, 0, input));

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
        protected String trimString(String thisStr,
                        @Shared("trimWhitespace") @Cached JSTrimWhitespaceNode trimWhitespaceNode) {
            return trimWhitespaceNode.executeString(thisStr);
        }

        @Specialization
        protected String trimObject(Object thisObj,
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
        protected String trimLeft(Object thisObj) {
            requireObjectCoercible(thisObj);
            String string = toString(thisObj);

            int firstIdx = JSRuntime.firstNonWhitespaceIndex(string, true);
            if (lengthZero.profile(firstIdx == 0)) {
                return string;
            } else if (lengthExceeded.profile(firstIdx >= string.length())) {
                return "";
            } else {
                return Boundaries.substring(string, firstIdx);
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
        protected String trimRight(Object thisObj) {
            requireObjectCoercible(thisObj);
            String string = toString(thisObj);

            int lastIdx = JSRuntime.lastNonWhitespaceIndex(string, true);
            if (lengthExceeded.profile(lastIdx >= string.length())) {
                return string;
            } else {
                return Boundaries.substring(string, 0, lastIdx + 1);
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
                        @Cached("create()") JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            String thatStr = toString2Node.executeString(thatObj);
            return doLocaleCompare(thisStr, thatStr);
        }

        @TruffleBoundary
        private static int doLocaleCompare(String thisStr, String thatStr) {
            return getCollator().compare(thisStr, thatStr);
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
        private DynamicObject createCollator(Object locales, Object options) {
            DynamicObject collatorObj = JSCollator.create(getContext(), getRealm());
            initCollatorNode.executeInit(collatorObj, locales, options);
            return collatorObj;
        }

        @Specialization
        protected int localeCompare(Object thisObj, Object thatObj, Object locales, Object options,
                        @Cached("create()") JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            String thatStr = toString2Node.executeString(thatObj);
            DynamicObject collator = createCollator(locales, options);
            return JSCollator.compare(collator, thisStr, thatStr);
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

        public JSStringSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String sliceStringIntInt(String str, int start, int end) {
            int len = str.length();
            int istart = JSRuntime.getOffset(start, len, offsetProfile1);
            int iend = JSRuntime.getOffset(end, len, offsetProfile2);
            if (canReturnEmpty.profile(iend > istart)) {
                return Boundaries.substring(str, istart, iend);
            } else {
                return "";
            }
        }

        @Specialization(replaces = {"sliceStringIntInt"})
        protected String sliceObjectIntInt(Object thisObj, int start, int end) {
            requireObjectCoercible(thisObj);
            return sliceStringIntInt(toString(thisObj), start, end);
        }

        @Specialization(guards = "isUndefined(end)")
        protected String sliceStringIntUndefined(String str, int start, @SuppressWarnings("unused") Object end) {
            int len = str.length();
            int istart = JSRuntime.getOffset(start, len, offsetProfile1);
            if (canReturnEmpty.profile(len > istart)) {
                return Boundaries.substring(str, istart, len);
            } else {
                return "";
            }
        }

        @Specialization(replaces = {"sliceStringIntInt", "sliceObjectIntInt", "sliceStringIntUndefined"})
        protected String sliceGeneric(Object thisObj, Object start, Object end,
                        @Cached("createBinaryProfile()") ConditionProfile isUndefined) {
            requireObjectCoercible(thisObj);
            String s = toString(thisObj);

            long len = s.length();
            long istart = JSRuntime.getOffset(toIntegerAsInt(start), len, offsetProfile1);
            long iend = isUndefined.profile(end == Undefined.instance) ? len : JSRuntime.getOffset(toIntegerAsInt(end), len, offsetProfile2);
            if (canReturnEmpty.profile(iend > istart)) {
                return Boundaries.substring(s, (int) istart, (int) iend);
            } else {
                return "";
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
        protected boolean startsWithString(String thisObj, String searchStr, @SuppressWarnings("unused") DynamicObject position) {
            if (searchStr.length() <= 0) {
                return true;
            }
            if (thisObj.length() < searchStr.length()) {
                return false;
            }

            for (int i = 0; i < searchStr.length(); i++) {
                if (thisObj.charAt(i) != searchStr.charAt(i)) {
                    return false;
                }
                TruffleSafepoint.poll(this);
            }
            return true;
        }

        @Specialization
        protected boolean startsWithGeneric(Object thisObj, Object searchString, Object position,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.startsWith must not be a regular expression");
            }
            String searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            if (searchStr.length() <= 0) {
                return true;
            }
            return Boundaries.stringStartsWith(thisStr, searchStr, fromIndex);
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
        protected boolean endsWithStringUndefined(String thisStr, String searchStr, @SuppressWarnings("unused") Object position) {
            int fromIndex = thisStr.length();
            if (searchStr.length() <= 0) {
                return true;
            }
            if (fromIndex >= thisStr.length()) {
                fromIndex = thisStr.length();
            } else if (fromIndex < 0) {
                return false;
            }
            return endsWithIntl(thisStr, searchStr, fromIndex);
        }

        @Specialization
        protected boolean endsWithGeneric(Object thisObj, Object searchString, Object position,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.endsWith must not be a regular expression");
            }
            String searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            if (searchStr.length() <= 0) {
                return true;
            }
            if (fromIndex >= thisStr.length() || position == Undefined.instance) {
                fromIndex = thisStr.length();
            } else if (fromIndex < 0) {
                return false;
            }
            return endsWithIntl(thisStr, searchStr, fromIndex);
        }

        private static boolean endsWithIntl(String thisStr, String searchStr, int fromIndex) {
            int foundIndex = Boundaries.stringLastIndexOf(thisStr, searchStr, fromIndex);
            return foundIndex >= 0 && foundIndex == fromIndex - searchStr.length();
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
        protected boolean includesString(String thisStr, String searchStr, @SuppressWarnings("unused") Object position) {
            return Boundaries.stringIndexOf(thisStr, searchStr) != -1;
        }

        @Specialization
        protected boolean includesGeneric(Object thisObj, Object searchString, Object position,
                        @Cached("create()") JSToStringNode toString2Node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (isRegExpNode.executeBoolean(searchString)) {
                noStringBranch.enter();
                throw Errors.createTypeError("First argument to String.prototype.includes must not be a regular expression");
            }
            String searchStr = toString2Node.executeString(searchString);
            int fromIndex = toIntegerAsInt(position);
            return Boundaries.stringIndexOf(thisStr, searchStr, fromIndex) != -1;
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
        protected String repeat(Object thisObj, Object count,
                        @Cached("create()") JSToNumberNode toNumberNode) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            Number repeatCountN = toNumberNode.executeNumber(count);
            long repeatCount = JSRuntime.toInteger(repeatCountN);
            if (repeatCount < 0 || (repeatCountN instanceof Double && Double.isInfinite(repeatCountN.doubleValue()))) {
                errorBranch.enter();
                throw Errors.createRangeError("illegal repeat count");
            }
            if (repeatCount == 1) {
                return thisStr;
            } else if (repeatCount == 0 || thisStr.length() == 0) {
                // fast path for repeating an empty string an arbitrary number of times
                // or repeating a string 0 times
                return "";
            }
            int repeatCountInt = (int) repeatCount;
            if (repeatCountInt != repeatCount || repeatCount * thisStr.length() > getContext().getStringLengthLimit()) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            return repeatImpl(thisStr, repeatCountInt);
        }

        @TruffleBoundary
        private String repeatImpl(String str, int repeatCount) {
            StringBuilder sb = new StringBuilder(str.length() * repeatCount);
            for (int i = 0; i < repeatCount; i++) {
                sb.append(str);
                TruffleSafepoint.poll(this);
            }
            return sb.toString();
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
        protected Object codePointAt(Object thisObj, Object position) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            int pos = toIntegerAsInt(position);
            if (pos < 0 || thisStr.length() <= pos) {
                undefinedBranch.enter();
                return Undefined.instance;
            }
            int first = Boundaries.stringCodePointAt(thisStr, pos);
            boolean isEnd = (pos + 1 == thisStr.length());
            if (isEnd || first < 0xD800 || first > 0xDBFF) {
                return first;
            }
            needSecondBranch.enter();
            int second = Boundaries.stringCodePointAt(thisStr, pos + 1);
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
        protected String normalize(Object thisObj, Object form) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            String formStr = toString(form);
            return doNormalize(thisStr, form, formStr);
        }

        @TruffleBoundary
        private static String doNormalize(String thisStr, Object form, String formStr) {
            Normalizer.Form useForm = null;
            if (form == Undefined.instance || formStr.length() <= 0 || formStr.equals("NFC")) {
                useForm = Normalizer.Form.NFC;
            } else if (formStr.equals("NFD")) {
                useForm = Normalizer.Form.NFD;
            } else if (formStr.equals("NFKC")) {
                useForm = Normalizer.Form.NFKC;
            } else if (formStr.equals("NFKD")) {
                useForm = Normalizer.Form.NFKD;
            } else {
                throw Errors.createRangeError("invalid form string");
            }
            return Normalizer.normalize(thisStr, useForm);
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
        protected String pad(Object thisObj, Object[] args,
                        @Cached("create()") JSToStringNode toString2Node) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            if (args.length == 0) {
                return thisStr;
            }
            int len = toIntegerAsInt(args[0]);
            if (len <= thisStr.length()) {
                return thisStr;
            }
            String fillStr;
            if (args.length <= 1 || args[1] == Undefined.instance) {
                fillStr = " ";
            } else {
                fillStr = toString2Node.executeString(args[1]);
                if (fillStr.isEmpty()) {
                    return thisStr; // explicit empty string
                }
            }
            if (len > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            return padIntl(thisStr, fillStr, len);
        }

        @TruffleBoundary
        private String padIntl(String str, String fillStr, int len) {
            assert !fillStr.isEmpty();
            int pos = len - str.length();
            int fillLen = fillStr.length();
            StringBuilder sb = new StringBuilder(len);
            if (!atStart) {
                sb.append(str);
            }
            while (pos >= fillLen) {
                sb.append(fillStr);
                pos -= fillLen;
            }
            if (pos > 0) {
                sb.append(fillStr, 0, pos);
            }
            if (atStart) {
                sb.append(str);
            }
            return sb.toString();
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
            adoptChildren();
        }

        public DynamicObject createIterator(VirtualFrame frame, Object regex, String string, Boolean global, Boolean fullUnicode) {
            DynamicObject regExpStringIteratorPrototype = getRealm().getRegExpStringIteratorPrototype();
            DynamicObject iterator = createObjectNode.execute(frame, regExpStringIteratorPrototype);
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

        @Specialization
        protected DynamicObject doString(VirtualFrame frame, String string) {
            DynamicObject iterator = createObjectNode.execute(frame, getRealm().getStringIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, string);
            setNextIndexNode.setValueInt(iterator, 0);
            return iterator;
        }

        @Specialization(guards = "!isString(thisObj)")
        protected DynamicObject doCoerce(VirtualFrame frame, Object thisObj,
                        @Cached("create()") RequireObjectCoercibleNode requireObjectCoercibleNode,
                        @Cached("create()") JSToStringNode toStringNode) {
            return doString(frame, toStringNode.executeString(requireObjectCoercibleNode.execute(thisObj)));
        }

    }

    static CreateHTMLNode createHTMLNode(JSContext context, JSBuiltin builtin, String tag, String attribute) {
        return CreateHTMLNodeGen.create(context, builtin, tag, attribute, args().withThis().fixedArgs(1).createArgumentNodes(context));
    }

    abstract static class CreateHTMLNode extends JSBuiltinNode {
        private final String tag;
        private final String attribute;

        CreateHTMLNode(JSContext context, JSBuiltin builtin, String tag, String attribute) {
            super(context, builtin);
            this.tag = tag;
            this.attribute = attribute;
        }

        @Specialization
        protected String createHTML(Object thisObj, Object value,
                        @Cached("create()") RequireObjectCoercibleNode requireObjectCoercibleNode,
                        @Cached("create()") JSToStringNode toStringNode) {
            String string = toStringNode.executeString(requireObjectCoercibleNode.execute(thisObj));
            if (!attribute.isEmpty()) {
                String attrVal = toStringNode.executeString(value);
                return wrapInTagWithAttribute(string, attrVal);
            }
            return wrapInTag(string);
        }

        @TruffleBoundary
        private String wrapInTag(String string) {
            return "<" + tag + ">" + string + "</" + tag + ">";
        }

        @TruffleBoundary
        private String wrapInTagWithAttribute(String string, String attrVal) {
            String escapedVal = attrVal.replace("\"", "&quot;");
            return "<" + tag + " " + attribute + "=\"" + escapedVal + "\"" + ">" + string + "</" + tag + ">";
        }
    }

    public abstract static class JSStringAtNode extends JSStringOperation {
        public JSStringAtNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object at(Object thisObj, Object index) {
            requireObjectCoercible(thisObj);
            String thisStr = toString(thisObj);
            int relativeIndex = toIntegerAsInt(index);
            int k = (relativeIndex >= 0) ? relativeIndex : thisStr.length() + relativeIndex;
            if (k < 0 || k >= thisStr.length()) {
                return Undefined.instance;
            }
            return String.valueOf(thisStr.charAt(k));
        }
    }
}
