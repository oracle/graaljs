/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Implements ES6 21.2.5.2.1 Runtime Semantics: RegExpExec ( R, S ).
 */
public final class JSRegExpExecIntlNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private JSRegExpExecBuiltinNode regExpBuiltinNode;
    @Child private PropertyGetNode getExecNode;
    @Child private JSFunctionCallNode specialCallNode;
    private final ConditionProfile isSpecialProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile specialIsObject = ConditionProfile.createBinaryProfile();
    private final ConditionProfile specialIsNotUndefined = ConditionProfile.createBinaryProfile();
    private final ConditionProfile specialIsNull = ConditionProfile.createBinaryProfile();

    private JSRegExpExecIntlNode(JSContext context) {
        this.context = context;
        this.getExecNode = PropertyGetNode.create("exec", false, context);
    }

    public static JSRegExpExecIntlNode create(JSContext context) {
        return new JSRegExpExecIntlNode(context);
    }

    public Object execute(DynamicObject regExp, String input) {
        if (context.getEcmaScriptVersion() >= 6) {
            Object exec = getExecNode.getValue(regExp);
            if (isSpecialProfile.profile(JSFunction.isJSFunction(exec))) {
                return executeSpecial(regExp, input, exec);
            }
        }
        expectRegExp(regExp);
        return callBuiltinExec(regExp, input);
    }

    public Object executeIgnoreLastIndex(DynamicObject regExp, String input, long lastIndex) {
        if (context.getEcmaScriptVersion() >= 6) {
            Object exec = getExecNode.getValue(regExp);
            if (isSpecialProfile.profile(JSFunction.isJSFunction(exec))) {
                return executeSpecial(regExp, input, exec);
            }
        }
        expectRegExp(regExp);
        return callBuiltinExecIgnoreLastIndex(regExp, input, lastIndex);
    }

    private Object executeSpecial(DynamicObject regExp, String input, Object exec) {
        Object result = callSpecial(exec, regExp, input);
        if (specialIsNull.profile(result == Null.instance)) {
            return result;
        } else if (specialIsObject.profile(JSObject.isJSObject(result))) {
            if (specialIsNotUndefined.profile(result != Undefined.instance)) {
                return result;
            }
        }
        throw Errors.createTypeError("object or null expected");
    }

    private static void expectRegExp(DynamicObject regExp) {
        if (!JSRegExp.isJSRegExp(regExp)) {
            throw Errors.createTypeError("RegExp expected");
        }
    }

    private Object callSpecial(Object exec, DynamicObject regExp, String input) {
        if (specialCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialCallNode = insert(JSFunctionCallNode.createCall());
        }
        return specialCallNode.executeCall(JSArguments.createOneArg(regExp, exec, input));
    }

    private Object callBuiltinExec(DynamicObject regExp, String input) {
        return getBuiltinNode().execute(regExp, input);
    }

    private TruffleObject callBuiltinExecIgnoreLastIndex(DynamicObject regExp, String input, long lastIndex) {
        return getBuiltinNode().executeIgnoreLastIndex(regExp, input, lastIndex);
    }

    private JSRegExpExecBuiltinNode getBuiltinNode() {
        if (regExpBuiltinNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            regExpBuiltinNode = insert(JSRegExpExecBuiltinNode.create(context));
        }
        return regExpBuiltinNode;
    }

    // implements ES6 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
    public static final class JSRegExpExecBuiltinNode extends JavaScriptBaseNode {

        private final JSContext context;
        private final ConditionProfile invalidLastIndex = ConditionProfile.createBinaryProfile();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();
        private final ConditionProfile stickyProfile = ConditionProfile.createBinaryProfile();
        private final int ecmaScriptVersion;

        @Child private JSToLengthNode toLengthNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexFlagsAccessor flagsAccessor = TRegexUtil.TRegexFlagsAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor regexResultAccessor = TRegexUtil.TRegexResultAccessor.create();

        private JSRegExpExecBuiltinNode(JSContext context) {
            this.context = context;
            ecmaScriptVersion = context.getEcmaScriptVersion();
        }

        public static JSRegExpExecBuiltinNode create(JSContext context) {
            return new JSRegExpExecBuiltinNode(context);
        }

        private Object getEmptyResult() {
            return ecmaScriptVersion >= 6 ? Null.instance : TRegexUtil.getTRegexEmptyResult();
        }

        // Implements 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
        public Object execute(DynamicObject regExp, String input) {
            TruffleObject flags = compiledRegexAccessor.flags(JSRegExp.getCompiledRegex(regExp));
            boolean global = flagsAccessor.global(flags);
            boolean sticky = ecmaScriptVersion >= 6 && flagsAccessor.sticky(flags);
            long lastIndex = getLastIndex(regExp);
            if (global || sticky) {
                if (invalidLastIndex.profile(lastIndex < 0 || lastIndex > input.length())) {
                    setLastIndex(regExp, 0);
                    return getEmptyResult();
                }
            } else {
                lastIndex = 0;
            }

            TruffleObject result = executeIgnoreLastIndex(regExp, input, lastIndex);
            if (match.profile(regexResultAccessor.isMatch(result))) {
                context.setRegexResult(result);
                if (stickyProfile.profile(sticky && regexResultAccessor.captureGroupStart(result, 0) != lastIndex)) {
                    // matcher should never have advanced that far!
                    setLastIndex(regExp, 0);
                    return getEmptyResult();
                }
                if (global || sticky) {
                    setLastIndex(regExp, regexResultAccessor.captureGroupEnd(result, 0));
                }
                if (ecmaScriptVersion < 6) {
                    return result;
                }
                return getMatchResult(result, input);
            } else {
                if (ecmaScriptVersion < 8 || global || sticky) {
                    setLastIndex(regExp, 0);
                }
                return getEmptyResult();
            }
        }

        /**
         * Ignores the {@code lastIndex} and {@code global} properties of the RegExp during
         * matching.
         */
        private TruffleObject executeIgnoreLastIndex(DynamicObject regExp, String input, long fromIndex) {
            TruffleObject result = compiledRegexAccessor.exec(JSRegExp.getCompiledRegex(regExp), input, fromIndex);
            if (regexResultAccessor.isMatch(result)) {
                context.setRegexResult(result);
            }
            return result;
        }

        // converts RegexResult into DynamicObject
        private DynamicObject getMatchResult(TruffleObject result, String inputStr) {
            return JSArray.createLazyRegexArray(context, regexResultAccessor.groupCount(result), result, inputStr);
        }

        private long getLastIndex(DynamicObject regExp) {
            if (ecmaScriptVersion < 6) {
                Object lastIndex = JSRegExp.getLastIndexRaw(regExp);
                return JSRuntime.intValueVirtual(JSRuntime.toNumber(lastIndex));
            } else {
                if (getLastIndexNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getLastIndexNode = insert(PropertyGetNode.create(JSRegExp.LAST_INDEX, false, context));
                }
                if (toLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toLengthNode = insert(JSToLengthNode.create());
                }
                Object value = getLastIndexNode.getValue(regExp);
                return toLengthNode.executeLong(value);
            }
        }

        private void setLastIndex(DynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, context, true));
            }
            setLastIndexNode.setValueInt(regExp, value);
        }
    }
}
