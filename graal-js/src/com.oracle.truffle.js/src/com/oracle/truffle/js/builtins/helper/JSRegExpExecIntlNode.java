/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNodeGen.BuildGroupsObjectNodeGen;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNodeGen.JSRegExpExecBuiltinNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsJSClassNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Implements ES6 21.2.5.2.1 Runtime Semantics: RegExpExec ( R, S ).
 */
public abstract class JSRegExpExecIntlNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private JSRegExpExecBuiltinNode regExpBuiltinNode;
    @Child private PropertyGetNode getExecNode;
    @Child private IsJSClassNode isJSRegExpNode;
    @Child private IsJSObjectNode isJSObjectNode;
    @Child private IsPristineObjectNode isPristineObjectNode;
    @Child private IsCallableNode isCallableNode = IsCallableNode.create();
    @Child private JSFunctionCallNode specialCallNode;
    private final ConditionProfile isPristineProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isCallableProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile validResultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isRegExpProfile = ConditionProfile.createBinaryProfile();

    JSRegExpExecIntlNode(JSContext context) {
        this.context = context;
    }

    public static JSRegExpExecIntlNode create(JSContext context) {
        return JSRegExpExecIntlNodeGen.create(context);
    }

    public abstract Object execute(DynamicObject regExp, String input);

    @Specialization
    Object doGeneric(DynamicObject regExp, String input) {
        if (context.getEcmaScriptVersion() >= 6) {
            if (isPristineProfile.profile(isPristine(regExp))) {
                return executeBuiltIn(regExp, input);
            } else {
                Object exec = getExecProperty(regExp);
                if (isCallableProfile.profile(isCallable(exec))) {
                    return callJSFunction(regExp, input, exec);
                }
            }
        }
        if (!isRegExpProfile.profile(isJSRegExp(regExp))) {
            throw Errors.createTypeError("RegExp expected");
        }
        return executeBuiltIn(regExp, input);
    }

    private Object callJSFunction(DynamicObject regExp, String input, Object exec) {
        Object result = doCallJSFunction(exec, regExp, input);
        if (validResultProfile.profile(result == Null.instance || isJSObject(result) && result != Undefined.instance)) {
            return result;
        }
        throw Errors.createTypeError("object or null expected");
    }

    private boolean isPristine(DynamicObject regExp) {
        if (isPristineObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(context));
        }
        return isPristineObjectNode.execute(regExp);
    }

    private Object getExecProperty(DynamicObject regExp) {
        if (getExecNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getExecNode = insert(PropertyGetNode.create("exec", false, context));
        }
        return getExecNode.getValue(regExp);
    }

    private boolean isCallable(Object obj) {
        if (isCallableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isCallableNode = insert(IsCallableNode.create());
        }
        return isCallableNode.executeBoolean(obj);
    }

    private Object executeBuiltIn(DynamicObject regExp, String input) {
        if (regExpBuiltinNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            regExpBuiltinNode = insert(JSRegExpExecBuiltinNode.create(context));
        }
        return regExpBuiltinNode.execute(regExp, input);
    }

    private boolean isJSRegExp(DynamicObject regExp) {
        if (isJSRegExpNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isJSRegExpNode = insert(IsJSClassNode.create(JSRegExp.INSTANCE));
        }
        return isJSRegExpNode.executeBoolean(regExp);
    }

    private boolean isJSObject(Object obj) {
        if (isJSObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isJSObjectNode = insert(IsJSObjectNode.create());
        }
        return isJSObjectNode.executeBoolean(obj);
    }

    private Object doCallJSFunction(Object exec, DynamicObject regExp, String input) {
        if (specialCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialCallNode = insert(JSFunctionCallNode.createCall());
        }
        return specialCallNode.executeCall(JSArguments.createOneArg(regExp, exec, input));
    }

    public static IsJSClassNode createIsJSRegExpNode() {
        return IsJSClassNode.create(JSRegExp.INSTANCE);
    }

    /**
     * Ignores the {@code lastIndex} and {@code global} properties of the RegExp during matching.
     */
    @ImportStatic(JSRegExpExecIntlNode.class)
    public abstract static class JSRegExpExecIntlIgnoreLastIndexNode extends JavaScriptBaseNode {

        private final JSContext context;
        private final boolean doStaticResultUpdate;
        private final ValueProfile compiledRegexProfile = ValueProfile.createIdentityProfile();

        JSRegExpExecIntlIgnoreLastIndexNode(JSContext context, boolean doStaticResultUpdate) {
            this.context = context;
            this.doStaticResultUpdate = doStaticResultUpdate;
        }

        public static JSRegExpExecIntlIgnoreLastIndexNode create(JSContext context, boolean doStaticResultUpdate) {
            return JSRegExpExecIntlNodeGen.JSRegExpExecIntlIgnoreLastIndexNodeGen.create(context, doStaticResultUpdate);
        }

        public abstract Object execute(DynamicObject regExp, String input, long lastIndex);

        @Specialization
        Object doGeneric(DynamicObject regExp, String input, long lastIndex,
                        @Cached("create()") TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        @Cached("create()") TRegexUtil.TRegexResultAccessor regexResultAccessor) {
            assert JSRegExp.isJSRegExp(regExp);
            Object compiledRegex = compiledRegexProfile.profile(JSRegExp.getCompiledRegex(regExp));
            Object result = executeCompiledRegex(compiledRegex, input, lastIndex, compiledRegexAccessor);
            if (doStaticResultUpdate && context.isOptionRegexpStaticResult() && regexResultAccessor.isMatch(result)) {
                context.getRealm().setStaticRegexResult(context, compiledRegex, input, lastIndex, result);
            }
            return result;
        }
    }

    @ImportStatic({JSRegExp.class, JSRegExpExecIntlNode.class})
    public abstract static class BuildGroupsObjectNode extends JavaScriptBaseNode {

        public static BuildGroupsObjectNode create() {
            return BuildGroupsObjectNodeGen.create();
        }

        public abstract DynamicObject execute(JSContext context, DynamicObject regExp, Object regexResult, String input, boolean isIndices);

        // We can reuse the cachedGroupsFactory even if the new groups factory is different, as long
        // as the compiledRegex is the same. This can happen if a new RegExp instance is repeatedly
        // created for the same regular expression.
        @Specialization(guards = "getGroupsFactory(regExp) == cachedGroupsFactory || getCompiledRegex(regExp) == cachedCompiledRegex")
        static DynamicObject doCachedGroupsFactory(JSContext context,
                        @SuppressWarnings("unused") DynamicObject regExp,
                        Object regexResult,
                        String input,
                        boolean isIndices,
                        @Cached("getCompiledRegex(regExp)") @SuppressWarnings("unused") Object cachedCompiledRegex,
                        @Cached("getGroupsFactory(regExp)") JSObjectFactory cachedGroupsFactory,
                        @Cached("createIsJSRegExpNode()") @SuppressWarnings("unused") IsJSClassNode isJSRegExpNode) {
            return doIt(context, cachedGroupsFactory, regexResult, input, isIndices);
        }

        @Specialization
        @TruffleBoundary
        static DynamicObject doVaryingGroupsFactory(JSContext context, DynamicObject regExp, Object regexResult, String input, boolean isIndices) {
            return doIt(context, JSRegExp.getGroupsFactory(regExp), regexResult, input, isIndices);
        }

        private static DynamicObject doIt(JSContext context, JSObjectFactory groupsFactory, Object regexResult, String input, boolean isIndices) {
            if (groupsFactory == null) {
                return Undefined.instance;
            } else {
                return JSRegExp.createGroupsObject(context, groupsFactory, regexResult, input, isIndices);
            }
        }
    }

    // implements ES6 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
    @ImportStatic({JSRegExp.class, JSRegExpExecIntlNode.class})
    public abstract static class JSRegExpExecBuiltinNode extends JavaScriptBaseNode {

        private final JSContext context;
        @Child private PropertySetNode setRegexResultNode;
        @Child private PropertySetNode setRegexOriginalInputNode;
        @Child private DynamicObjectLibrary setInputNode;
        @Child private DynamicObjectLibrary setIndexNode;
        @Child private DynamicObjectLibrary setGroupsNode;
        @Child private DynamicObjectLibrary setIndicesNode;
        @Child private DynamicObjectLibrary setIndicesRegexResultNode;
        @Child private DynamicObjectLibrary setIndicesGroupsNode;
        private final ConditionProfile invalidLastIndex = ConditionProfile.createBinaryProfile();
        private final ConditionProfile match = ConditionProfile.createCountingProfile();
        private final ConditionProfile stickyProfile = ConditionProfile.createBinaryProfile();
        private final int ecmaScriptVersion;

        @Child protected IsJSClassNode isJSRegExpNode;
        @Child private JSToLengthNode toLengthNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexFlagsAccessor flagsAccessor = TRegexUtil.TRegexFlagsAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor regexResultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private BuildGroupsObjectNode groupsBuilder;

        JSRegExpExecBuiltinNode(JSContext context) {
            this.context = context;
            this.ecmaScriptVersion = context.getEcmaScriptVersion();
            this.setRegexResultNode = PropertySetNode.createSetHidden(JSArray.LAZY_REGEX_RESULT_ID, context);
            this.setRegexOriginalInputNode = PropertySetNode.createSetHidden(JSArray.LAZY_REGEX_ORIGINAL_INPUT_ID, context);
            this.setInputNode = JSObjectUtil.createDispatched(JSRegExp.INPUT);
            this.setIndexNode = JSObjectUtil.createDispatched(JSRegExp.INDEX);
            this.setGroupsNode = JSObjectUtil.createDispatched(JSRegExp.GROUPS);
            this.setIndicesNode = JSObjectUtil.createDispatched(JSRegExp.INDICES);
            this.setIndicesRegexResultNode = JSObjectUtil.createDispatched(JSArray.LAZY_REGEX_RESULT_ID);
            this.setIndicesGroupsNode = JSObjectUtil.createDispatched(JSRegExp.GROUPS);
        }

        public static JSRegExpExecBuiltinNode create(JSContext context) {
            return JSRegExpExecBuiltinNodeGen.create(context);
        }

        private Object getEmptyResult() {
            return ecmaScriptVersion >= 6 ? Null.instance : context.getTRegexEmptyResult();
        }

        public abstract Object execute(DynamicObject regExp, String input);

        @Specialization(guards = "getCompiledRegex(regExp) == cachedCompiledRegex")
        Object doCached(DynamicObject regExp, String input,
                        @Cached("getCompiledRegex(regExp)") Object cachedCompiledRegex) {
            return doExec(regExp, cachedCompiledRegex, input);
        }

        @Specialization(replaces = "doCached")
        Object doDynamic(DynamicObject regExp, String input) {
            return doExec(regExp, JSRegExp.getCompiledRegex(regExp), input);
        }

        // Implements 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
        private Object doExec(DynamicObject regExp, Object compiledRegex, String input) {
            Object flags = compiledRegexAccessor.flags(compiledRegex);
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

            Object result = executeCompiledRegex(compiledRegex, input, lastIndex, compiledRegexAccessor);
            if (context.isOptionRegexpStaticResult() && regexResultAccessor.isMatch(result)) {
                JSRealm thisRealm = context.getRealm();
                if (thisRealm == JSRegExp.getRealm(regExp)) {
                    if (JSRegExp.getLegacyFeaturesEnabled(regExp)) {
                        thisRealm.setStaticRegexResult(context, compiledRegex, input, lastIndex, result);
                    } else {
                        thisRealm.invalidateStaticRegexResult();
                    }
                }
            }
            if (match.profile(regexResultAccessor.isMatch(result))) {
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
                int groupCount = compiledRegexAccessor.groupCount(compiledRegex);
                DynamicObject groups = getGroupsObject(regExp, result, input, false);
                DynamicObject indicesGroups = getGroupsObject(regExp, result, input, true);
                return getMatchResult(result, groupCount, input, groups, indicesGroups);
            } else {
                if (ecmaScriptVersion < 8 || global || sticky) {
                    setLastIndex(regExp, 0);
                }
                return getEmptyResult();
            }
        }

        // converts RegexResult into DynamicObject
        private DynamicObject getMatchResult(Object regexResult, int groupCount, String inputStr, DynamicObject groups, DynamicObject indicesGroups) {
            DynamicObject resultArray = JSArray.createLazyRegexArray(context, groupCount);
            setRegexResultNode.setValue(resultArray, regexResult);
            setRegexOriginalInputNode.setValue(resultArray, inputStr);
            setIndexNode.putConstant(resultArray, JSRegExp.INDEX, JSRegExp.LAZY_INDEX_PROXY, JSAttributes.getDefault() | JSProperty.PROXY);
            setInputNode.put(resultArray, JSRegExp.INPUT, inputStr);
            setGroupsNode.put(resultArray, JSRegExp.GROUPS, groups);
            if (context.isOptionRegexpMatchIndices()) {
                DynamicObject indices = JSArray.createLazyRegexIndicesArray(context, groupCount);
                setIndicesRegexResultNode.put(indices, JSRegExp.GROUPS_RESULT_ID, regexResult);
                setIndicesGroupsNode.put(indices, JSRegExp.GROUPS, indicesGroups);
                setIndicesNode.put(resultArray, JSRegExp.INDICES, indices);
            }
            return resultArray;
        }

        // builds the object containing the matches of the named capture groups
        private DynamicObject getGroupsObject(DynamicObject regExp, Object result, String input, boolean isIndices) {
            if (groupsBuilder == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                groupsBuilder = insert(BuildGroupsObjectNode.create());
            }
            return groupsBuilder.execute(context, regExp, result, input, isIndices);
        }

        private long getLastIndex(DynamicObject regExp) {
            if (getLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLastIndexNode = insert(PropertyGetNode.create(JSRegExp.LAST_INDEX, false, context));
            }
            Object lastIndex = getLastIndexNode.getValue(regExp);
            if (ecmaScriptVersion < 6) {
                return JSRuntime.toInteger(lastIndex);
            } else {
                if (toLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toLengthNode = insert(JSToLengthNode.create());
                }
                return toLengthNode.executeLong(lastIndex);
            }
        }

        private void setLastIndex(DynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, context, true));
            }
            setLastIndexNode.setValueInt(regExp, value);
        }

        protected boolean isJSRegExp(DynamicObject regExp) {
            if (isJSRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isJSRegExpNode = createIsJSRegExpNode();
            }
            return isJSRegExpNode.executeBoolean(regExp);
        }
    }

    private static Object executeCompiledRegex(Object compiledRegex, String input, long fromIndex,
                    TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor) {
        try {
            return compiledRegexAccessor.exec(compiledRegex, input, fromIndex);
        } catch (RuntimeException e) {
            CompilerDirectives.transferToInterpreter();
            // thrown if none of the regex engines supports the given regex
            throw Errors.createError(e.getMessage());
        }
    }
}
