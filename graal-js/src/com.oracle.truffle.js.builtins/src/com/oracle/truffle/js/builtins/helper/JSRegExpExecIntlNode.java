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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNodeFactory.BuildGroupsObjectNodeGen;
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
import com.oracle.truffle.regex.UnsupportedRegexException;

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

    @ImportStatic(JSRegExp.class)
    public abstract static class BuildGroupsObjectNode extends JavaScriptBaseNode {

        public static BuildGroupsObjectNode create() {
            return BuildGroupsObjectNodeGen.create();
        }

        public abstract DynamicObject execute(JSContext context, DynamicObject regExp, TruffleObject regexResult);

        // We can reuse the cachedGroupsFactory even if the new groups factory is different, as long
        // as the compiledRegex is the same. This can happen if a new RegExp instance is repeatedly
        // created for the same regular expression.
        @Specialization(guards = "getGroupsFactory(regExp) == cachedGroupsFactory || getCompiledRegex(regExp) == cachedCompiledRegex")
        protected static DynamicObject doCachedGroupsFactory(JSContext context,
                        @SuppressWarnings("unused") DynamicObject regExp,
                        TruffleObject regexResult,
                        @Cached("getCompiledRegex(regExp)") @SuppressWarnings("unused") TruffleObject cachedCompiledRegex,
                        @Cached("getGroupsFactory(regExp)") DynamicObjectFactory cachedGroupsFactory) {
            return doIt(context, cachedGroupsFactory, regexResult);
        }

        @Specialization
        @TruffleBoundary
        protected static DynamicObject doVaryingGroupsFactory(JSContext context, DynamicObject regExp, TruffleObject regexResult) {
            return doIt(context, JSRegExp.getGroupsFactory(regExp), regexResult);
        }

        private static DynamicObject doIt(JSContext context, DynamicObjectFactory groupsFactory, TruffleObject regexResult) {
            if (groupsFactory == null) {
                return Undefined.instance;
            } else {
                return JSObject.create(context, groupsFactory, regexResult);
            }
        }
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
        @Child private BuildGroupsObjectNode groupsBuilder;

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
                DynamicObject groups = getGroupsObject(regExp, result);
                return getMatchResult(result, input, groups);
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
            TruffleObject result = executeCompiledRegex(JSRegExp.getCompiledRegex(regExp), input, fromIndex);
            if (regexResultAccessor.isMatch(result)) {
                context.setRegexResult(result);
            }
            return result;
        }

        private TruffleObject executeCompiledRegex(TruffleObject compiledRegex, String input, long fromIndex) {
            try {
                return compiledRegexAccessor.exec(compiledRegex, input, fromIndex);
            } catch (UnsupportedRegexException e) {
                throw Errors.createError(e.getMessage());
            }
        }

        // converts RegexResult into DynamicObject
        private DynamicObject getMatchResult(TruffleObject result, String inputStr, DynamicObject groups) {
            return JSArray.createLazyRegexArray(context, regexResultAccessor.groupCount(result), result, inputStr, groups);
        }

        // builds the object containing the matches of the named capture groups
        private DynamicObject getGroupsObject(DynamicObject regExp, TruffleObject result) {
            if (groupsBuilder == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                groupsBuilder = insert(BuildGroupsObjectNode.create());
            }
            return groupsBuilder.execute(context, regExp, result);
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
