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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltinsFactory.HasInstanceNodeGen;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltinsFactory.JSApplyNodeGen;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltinsFactory.JSBindNodeGen;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltinsFactory.JSCallNodeGen;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltinsFactory.JSFunctionToStringNodeGen;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode.OrdinaryHasInstanceNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Contains builtins for {@linkplain JSFunction Function}.prototype.
 */
public final class FunctionPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<FunctionPrototypeBuiltins.FunctionPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new FunctionPrototypeBuiltins();
    public static final JSBuiltinsContainer BUILTINS_NASHORN_COMPAT = new FunctionPrototypeNashornCompatBuiltins();

    protected FunctionPrototypeBuiltins() {
        super(JSFunction.PROTOTYPE_NAME, FunctionPrototype.class);
    }

    public enum FunctionPrototype implements BuiltinEnum<FunctionPrototype> {
        bind(1),
        toString(0),
        apply(2),
        call(1),

        _hasInstance(1) {
            @Override
            public Object getKey() {
                return Symbol.SYMBOL_HAS_INSTANCE;
            }

            @Override
            public boolean isWritable() {
                return false;
            }

            @Override
            public boolean isConfigurable() {
                return false;
            }
        };

        private final int length;

        FunctionPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case _hasInstance -> 6;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FunctionPrototype builtinEnum) {
        switch (builtinEnum) {
            case bind:
                return JSBindNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case toString:
                return JSFunctionToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case apply:
                return JSApplyNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case call:
                return JSCallNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case _hasInstance:
                return HasInstanceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static final class FunctionPrototypeNashornCompatBuiltins extends JSBuiltinsContainer.SwitchEnum<FunctionPrototypeNashornCompatBuiltins.FunctionNashornCompat> {
        protected FunctionPrototypeNashornCompatBuiltins() {
            super(FunctionNashornCompat.class);
        }

        public enum FunctionNashornCompat implements BuiltinEnum<FunctionNashornCompat> {
            toSource(0);

            private final int length;

            FunctionNashornCompat(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FunctionNashornCompat builtinEnum) {
            switch (builtinEnum) {
                case toSource:
                    return JSFunctionToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            }
            return null;
        }
    }

    public static final class CopyFunctionNameAndLengthNode extends JavaScriptBaseNode {
        @Child private HasPropertyCacheNode hasFunctionLengthNode;
        @Child private PropertyGetNode getFunctionLengthNode;
        @Child private PropertyGetNode getFunctionNameNode;
        @Child private DynamicObjectLibrary functionLengthLib;
        @Child private DynamicObjectLibrary functionNameLib;
        private final ConditionProfile hasFunctionLengthProfile = ConditionProfile.create();
        private final ConditionProfile hasIntegerFunctionLengthProfile = ConditionProfile.create();
        private final ConditionProfile isJSFunctionProfile = ConditionProfile.create();

        public CopyFunctionNameAndLengthNode(JSContext context) {
            this.hasFunctionLengthNode = HasPropertyCacheNode.create(JSFunction.LENGTH, context, true);
            this.getFunctionLengthNode = PropertyGetNode.create(JSFunction.LENGTH, false, context);
            this.getFunctionNameNode = PropertyGetNode.create(JSFunction.NAME, false, context);
            this.functionLengthLib = JSObjectUtil.createDispatched(JSFunction.LENGTH);
            this.functionNameLib = JSObjectUtil.createDispatched(JSFunction.NAME);
        }

        @NeverDefault
        public static CopyFunctionNameAndLengthNode create(JSContext context) {
            return new CopyFunctionNameAndLengthNode(context);
        }

        public void execute(JSFunctionObject boundFunction, JSFunctionObject targetFunction, TruffleString prefix, int argCount) {
            if (hasFunctionLengthProfile.profile(hasFunctionLengthNode.hasProperty(targetFunction))) {
                if (!JSProperty.isProxy(functionLengthLib.getPropertyFlagsOrDefault(targetFunction, Strings.LENGTH, 0))) {
                    // The Get node serves as an implicit branch profile.
                    copyLength(boundFunction, targetFunction, argCount);
                } else {
                    int targetLen = targetFunction instanceof JSFunctionObject.BoundOrWrapped
                                    ? ((JSFunctionObject.BoundOrWrapped) targetFunction).getBoundLength()
                                    : JSFunction.getLength(targetFunction);
                    assert targetLen >= 0;
                    int length = Math.max(0, targetLen - argCount);
                    ((JSFunctionObject.BoundOrWrapped) boundFunction).setBoundLength(length);
                }
            }

            // If the target has name proxy property, the name can be lazily computed.
            if (!JSProperty.isProxy(functionNameLib.getPropertyFlagsOrDefault(targetFunction, Strings.NAME, 0))) {
                // The Get node serves as an implicit branch profile.
                copyName(boundFunction, targetFunction, prefix);
            }
        }

        public void execute(JSFunctionObject boundFunction, Object target, TruffleString prefix, int argCount) {
            if (isJSFunctionProfile.profile(target instanceof JSFunctionObject)) {
                execute(boundFunction, (JSFunctionObject) target, prefix, argCount);
                return;
            }

            if (hasFunctionLengthProfile.profile(hasFunctionLengthNode.hasProperty(target))) {
                copyLength(boundFunction, target, argCount);
            }

            copyName(boundFunction, target, prefix);
        }

        private void copyLength(JSFunctionObject boundFunction, Object target, int argCount) {
            Object targetLen = getFunctionLengthNode.getValue(target);
            if (hasIntegerFunctionLengthProfile.profile(targetLen instanceof Integer)) {
                int targetLenAsInt = (int) targetLen;
                // inner Math.max() avoids potential underflow during the subtraction
                int lengthAsInt = Math.max(0, Math.max(0, targetLenAsInt) - argCount);
                ((JSFunctionObject.BoundOrWrapped) boundFunction).setBoundLength(lengthAsInt);
            } else if (JSRuntime.isNumber(targetLen) || targetLen instanceof Long) {
                Number length;
                double targetLenAsInt = toIntegerOrInfinity((Number) targetLen);
                if (targetLenAsInt != Double.NEGATIVE_INFINITY) {
                    length = JSRuntime.doubleToNarrowestNumber(Math.max(0, targetLenAsInt - argCount));
                } else {
                    length = 0;
                }
                JSFunction.setFunctionLength(boundFunction, length);
            }
        }

        private void copyName(JSFunctionObject boundFunction, Object target, TruffleString prefix) {
            Object targetName = getFunctionNameNode.getValue(target);
            if (!JSGuards.isString(targetName)) {
                targetName = Strings.EMPTY_STRING;
            }
            ((JSFunctionObject.BoundOrWrapped) boundFunction).setBoundName((TruffleString) targetName, prefix);
        }

        private static double toIntegerOrInfinity(Number number) {
            if (number instanceof Double) {
                double doubleValue = (Double) number;
                return Double.isNaN(doubleValue) ? 0 : JSRuntime.truncateDouble(doubleValue);
            } else {
                return JSRuntime.doubleValue(number);
            }
        }
    }

    public abstract static class JSBindNode extends JSBuiltinNode {

        public JSBindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSDynamicObject bindJSFunction(JSFunctionObject thisFnObj, Object thisArg, Object[] args,
                        @Cached @Shared GetPrototypeNode getPrototypeNode,
                        @Cached("create(getContext())") @Shared CopyFunctionNameAndLengthNode copyNameAndLengthNode,
                        @Cached @Shared InlinedConditionProfile isConstructorProfile,
                        @Cached @Exclusive InlinedConditionProfile isAsyncProfile,
                        @Cached @Shared InlinedConditionProfile setProtoProfile) {
            JSDynamicObject proto = getPrototypeNode.execute(thisFnObj);

            JSFunctionObject boundFunction = JSFunction.boundFunctionCreate(getContext(), thisFnObj, thisArg, args, proto,
                            isConstructorProfile, isAsyncProfile, setProtoProfile, this);

            copyNameAndLengthNode.execute(boundFunction, thisFnObj, Strings.BOUND_SPC, args.length);

            return boundFunction;
        }

        @Specialization(guards = {"!isJSFunction(thisObj)", "isCallableNode.executeBoolean(thisObj)"})
        protected final JSDynamicObject bindOther(Object thisObj, Object thisArg, Object[] args,
                        @SuppressWarnings("unused") @Cached @Shared IsCallableNode isCallableNode,
                        @Cached @Shared GetPrototypeNode getPrototypeNode,
                        @Cached ForeignObjectPrototypeNode foreignPrototypeNode,
                        @Cached IsConstructorNode isConstructorNode,
                        @Cached("create(getContext())") @Shared CopyFunctionNameAndLengthNode copyNameAndLengthNode,
                        @Cached @Exclusive InlinedConditionProfile isProxyProfile,
                        @Cached @Shared InlinedConditionProfile isConstructorProfile,
                        @Cached @Shared InlinedConditionProfile setProtoProfile) {
            JSRealm realm = JSRuntime.getFunctionRealm(thisObj, JSRealm.get(this));
            JSDynamicObject proto;
            if (isProxyProfile.profile(this, JSProxy.isJSProxy(thisObj))) {
                proto = getPrototypeNode.execute(thisObj);
            } else {
                assert JSRuntime.isCallableForeign(thisObj);
                if (getContext().getLanguageOptions().hasForeignObjectPrototype()) {
                    proto = foreignPrototypeNode.execute(thisObj);
                } else {
                    proto = Null.instance;
                }
            }

            JSContext context = getContext();
            boolean constructor = isConstructorProfile.profile(this, isConstructorNode.executeBoolean(thisObj));
            JSFunctionData boundFunctionData = context.getBoundFunctionData(constructor, false);
            JSFunctionObject boundFunction = JSFunction.createBound(context, realm, boundFunctionData, thisObj, thisArg, args);

            boolean needSetProto = proto != realm.getFunctionPrototype();
            if (setProtoProfile.profile(this, needSetProto)) {
                JSObject.setPrototype(boundFunction, proto);
            }

            copyNameAndLengthNode.execute(boundFunction, thisObj, Strings.BOUND_SPC, args.length);

            return boundFunction;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isCallableNode.executeBoolean(thisObj)"})
        protected static JSDynamicObject bindError(Object thisObj, Object thisArg, Object[] arg,
                        @SuppressWarnings("unused") @Cached @Shared IsCallableNode isCallableNode) {
            throw Errors.createTypeErrorNotAFunction(thisObj);
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class JSFunctionToStringNode extends JSBuiltinNode {

        public JSFunctionToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected final TruffleString toStringFunction(JSFunctionObject fnObj) {
            if (fnObj instanceof JSFunctionObject.Bound) {
                return toStringBound(fnObj);
            } else {
                return toStringDefaultTarget(fnObj);
            }
        }

        private TruffleString toStringBound(JSFunctionObject fnObj) {
            if (getContext().isOptionV8CompatibilityMode()) {
                return Strings.FUNCTION_NATIVE_CODE;
            } else {
                TruffleString name = JSFunction.getName(fnObj);
                return getNameIntl(name);
            }
        }

        @TruffleBoundary
        private static TruffleString getNameIntl(TruffleString name) {
            int spacePos = Strings.lastIndexOf(name, ' ');
            return Strings.concatAll(Strings.FUNCTION_SPC, spacePos < 0 ? name : Strings.lazySubstring(name, spacePos + 1), Strings.FUNCTION_NATIVE_CODE_BODY);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isES2019OrLater()", "!isJSFunction(fnObj)", "isCallable.executeBoolean(fnObj)"})
        protected static TruffleString toStringCallable(Object fnObj,
                        @Cached @Shared IsCallableNode isCallable,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interopStr,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            try {
                Object name = null;
                if (interop.hasExecutableName(fnObj)) {
                    name = interop.getExecutableName(fnObj);
                } else if (interop.isMetaObject(fnObj)) {
                    name = interop.getMetaSimpleName(fnObj);
                }
                if (name != null) {
                    return getNameIntl(Strings.interopAsTruffleString(name, interopStr, switchEncoding));
                }
            } catch (UnsupportedMessageException e) {
            }
            return Strings.FUNCTION_NATIVE_CODE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isES2019OrLater()", "!isCallable.executeBoolean(fnObj)"})
        protected static TruffleString toStringNotCallable(Object fnObj,
                        @Cached @Shared IsCallableNode isCallable) {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }

        @Specialization(guards = {"!isES2019OrLater()", "!isJSFunction(fnObj)"})
        protected static TruffleString toStringNotFunction(Object fnObj) {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }

        @Idempotent
        final boolean isES2019OrLater() {
            return getContext().getEcmaScriptVersion() >= JSConfig.ECMAScript2019;
        }

        @TruffleBoundary
        private static TruffleString toStringDefaultTarget(JSFunctionObject fnObj) {
            CallTarget ct = JSFunction.getCallTarget(fnObj);
            if (!(ct instanceof RootCallTarget)) {
                return Strings.fromJavaString(ct.toString());
            }
            RootCallTarget dct = (RootCallTarget) ct;
            RootNode rn = dct.getRootNode();
            SourceSection ssect = rn.getSourceSection();
            TruffleString result;
            if (ssect == null || !ssect.isAvailable() || ssect.getSource().isInternal()) {
                result = Strings.concatAll(Strings.FUNCTION_SPC, JSFunction.getName(fnObj), Strings.FUNCTION_NATIVE_CODE_BODY);
            } else {
                result = Strings.fromJavaString(ssect.getCharacters().toString());
            }
            return result;
        }
    }

    public abstract static class JSApplyNode extends JSBuiltinNode {

        @Child private JSFunctionCallNode call;
        @Child private JSToObjectArrayNode toObjectArray;

        public JSApplyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.call = JSFunctionCallNode.createCall();
            this.toObjectArray = JSToObjectArrayNode.create(true);
        }

        @Specialization(guards = "isJSFunction(function)")
        protected Object applyFunction(JSDynamicObject function, Object target, Object args) {
            return apply(function, target, args);
        }

        @Specialization(guards = "isCallable.executeBoolean(function)", replaces = "applyFunction")
        protected Object applyCallable(Object function, Object target, Object args,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable) {
            return apply(function, target, args);
        }

        private Object apply(Object function, Object target, Object args) {
            int maxApplyArgumentLength = getContext().getLanguageOptions().maxApplyArgumentLength();
            Object[] applyUserArgs = toObjectArray.executeObjectArray(args, maxApplyArgumentLength);
            assert applyUserArgs.length <= maxApplyArgumentLength;
            Object[] passedOnArguments = JSArguments.create(target, function, applyUserArgs);
            return call.executeCall(passedOnArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable.executeBoolean(function)")
        protected Object error(Object function, Object target, Object args,
                        @Cached @Shared IsCallableNode isCallable) {
            throw Errors.createTypeErrorNotAFunction(function);
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    public abstract static class JSCallNode extends JSBuiltinNode {

        @Child private JSFunctionCallNode callNode;

        public JSCallNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected Object call(Object function, Object target, Object[] args) {
            return callNode.executeCall(JSArguments.create(target, function, args));
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    public abstract static class HasInstanceNode extends JSBuiltinNode {
        @Child OrdinaryHasInstanceNode ordinaryHasInstanceNode;

        public HasInstanceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.ordinaryHasInstanceNode = OrdinaryHasInstanceNode.create(context);
        }

        @Specialization
        protected boolean hasInstance(Object thisObj, Object value) {
            return ordinaryHasInstanceNode.executeBoolean(value, thisObj);
        }
    }
}
