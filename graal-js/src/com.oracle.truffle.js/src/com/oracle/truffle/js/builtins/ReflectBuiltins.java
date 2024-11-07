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

import java.util.List;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectApplyNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectConstructNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectDefinePropertyNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectDeletePropertyNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectGetNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectGetOwnPropertyDescriptorNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectGetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectHasNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectIsExtensibleNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectOwnKeysNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectPreventExtensionsNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectSetNodeGen;
import com.oracle.truffle.js.builtins.ReflectBuiltinsFactory.ReflectSetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.access.FromPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.access.IsExtensibleNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.access.ToPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * Contains builtins for Reflect (ES2015, 26.1).
 */
public class ReflectBuiltins extends JSBuiltinsContainer.SwitchEnum<ReflectBuiltins.Reflect> {

    public static final JSBuiltinsContainer BUILTINS = new ReflectBuiltins();

    protected ReflectBuiltins() {
        super(JSRealm.REFLECT_CLASS_NAME, Reflect.class);
    }

    public enum Reflect implements BuiltinEnum<Reflect> {
        apply(3),
        construct(2),
        defineProperty(3),
        deleteProperty(2),
        get(2),
        getOwnPropertyDescriptor(2),
        getPrototypeOf(1),
        has(2),
        isExtensible(1),
        ownKeys(1),
        preventExtensions(1),
        set(3),
        setPrototypeOf(2);

        private final int length;

        Reflect(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Reflect builtinEnum) {
        assert context.getEcmaScriptVersion() >= 6;
        switch (builtinEnum) {
            case apply:
                return ReflectApplyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case construct:
                return ReflectConstructNodeGen.create(context, builtin, args().fixedArgs(2).varArgs().createArgumentNodes(context));
            case defineProperty:
                return ReflectDefinePropertyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case deleteProperty:
                return ReflectDeletePropertyNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case get:
                return ReflectGetNodeGen.create(context, builtin, args().fixedArgs(2).varArgs().createArgumentNodes(context));
            case getOwnPropertyDescriptor:
                return ReflectGetOwnPropertyDescriptorNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case getPrototypeOf:
                return ReflectGetPrototypeOfNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case has:
                return ReflectHasNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case isExtensible:
                return ReflectIsExtensibleNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case ownKeys:
                return ReflectOwnKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case preventExtensions:
                return ReflectPreventExtensionsNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case set:
                return ReflectSetNodeGen.create(context, builtin, args().fixedArgs(3).varArgs().createArgumentNodes(context));
            case setPrototypeOf:
                return ReflectSetPrototypeOfNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ReflectOperation extends JSBuiltinNode {
        protected final BranchProfile errorBranch = BranchProfile.create();

        public ReflectOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected void ensureJSObject(Object target) {
            if (!JSRuntime.isObject(target)) {
                errorBranch.enter();
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }
    }

    public abstract static class ReflectApplyNode extends JSBuiltinNode {

        @Child private JSFunctionCallNode call = JSFunctionCallNode.createCall();
        @Child private JSToObjectArrayNode toObjectArray = JSToObjectArrayNodeGen.create();

        public ReflectApplyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFunction(target)")
        protected final Object applyFunction(JSDynamicObject target, Object thisArgument, Object argumentsList) {
            return apply(target, thisArgument, argumentsList);
        }

        @Specialization(guards = "isCallable.executeBoolean(target)", replaces = "applyFunction", limit = "1")
        protected final Object applyCallable(Object target, Object thisArgument, Object argumentsList,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            return apply(target, thisArgument, argumentsList);
        }

        private Object apply(Object target, Object thisArgument, Object argumentsList) {
            int maxApplyArgumentLength = getContext().getLanguageOptions().maxApplyArgumentLength();
            Object[] applyUserArgs = toObjectArray.executeObjectArray(argumentsList, maxApplyArgumentLength);
            assert applyUserArgs.length <= maxApplyArgumentLength;
            Object[] passedOnArguments = JSArguments.create(thisArgument, target, applyUserArgs);
            return call.executeCall(passedOnArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable.executeBoolean(target)", limit = "1")
        protected static Object error(Object target, Object thisArgument, Object argumentsList,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            throw Errors.createTypeErrorCallableExpected();
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    public abstract static class ReflectConstructNode extends ReflectOperation {

        @Child private JSFunctionCallNode constructCall = JSFunctionCallNode.createNewTarget();
        @Child private JSToObjectArrayNode toObjectArray = JSToObjectArrayNodeGen.create();

        public ReflectConstructNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object reflectConstruct(Object target, Object argumentsList, Object[] optionalArgs,
                        @Cached IsConstructorNode isConstructorNode) {
            if (!isConstructorNode.executeBoolean(target)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAConstructor(target, getContext());
            }
            Object newTarget;
            if (optionalArgs.length == 0) {
                newTarget = target;
            } else {
                newTarget = optionalArgs[0];
                if (!isConstructorNode.executeBoolean(newTarget)) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorNotAConstructor(newTarget, getContext());
                }
            }

            if (!JSRuntime.isObject(argumentsList)) {
                throw Errors.createTypeError("Reflect.construct: Arguments list has wrong type");
            }
            int maxApplyArgumentLength = getContext().getLanguageOptions().maxApplyArgumentLength();
            Object[] args = toObjectArray.executeObjectArray(argumentsList, maxApplyArgumentLength);
            assert args.length <= maxApplyArgumentLength;
            Object[] passedOnArguments = JSArguments.createWithNewTarget(JSFunction.CONSTRUCT, target, newTarget, args);
            return constructCall.executeCall(passedOnArguments);
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    public abstract static class ReflectDefinePropertyNode extends ReflectOperation {

        public ReflectDefinePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectDefineProperty(Object target, Object propertyKey, Object attributes,
                        @Cached JSToPropertyKeyNode toPropertyKeyNode,
                        @Cached("create(getContext())") ToPropertyDescriptorNode toPropertyDescriptorNode) {
            ensureJSObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            PropertyDescriptor descriptor = toPropertyDescriptorNode.execute(attributes);
            return JSObject.defineOwnProperty((JSDynamicObject) target, key, descriptor);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ReflectDeletePropertyNode extends ReflectOperation {

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        public ReflectDeletePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean doObject(JSObject target, Object propertyKey,
                        @Cached JSClassProfile classProfile) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSObject.delete(target, key, false, classProfile);
        }

        @Specialization(guards = {"isForeignObject(target)"}, limit = "InteropLibraryLimit")
        protected boolean doForeignObject(Object target, Object propertyKey,
                        @CachedLibrary("target") InteropLibrary interop,
                        @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            if (interop.hasMembers(target)) {
                if (key instanceof TruffleString) {
                    String memberName = Strings.toJavaString(toJavaStringNode, (TruffleString) key);
                    if (interop.isMemberRemovable(target, memberName)) {
                        try {
                            InteropLibrary.getUncached().removeMember(target, memberName);
                        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                            throw Errors.createTypeErrorInteropException(target, e, "removeMember", memberName, null);
                        }
                        return true;
                    }
                }
                return false;
            } else {
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSObject(target)", "!isForeignObject(target)"})
        protected boolean doNonObject(Object target, Object propertyKey) {
            throw Errors.createTypeErrorCalledOnNonObject();
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ReflectGetNode extends ReflectOperation {

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        public ReflectGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doObject(JSObject target, Object propertyKey, Object[] optionalArgs,
                        @Shared("jsclassProf") @Cached JSClassProfile classProfile) {
            Object receiver = JSRuntime.getArg(optionalArgs, 0, target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSRuntime.nullToUndefined(classProfile.getJSClass(target).getHelper(target, receiver, key, this));
        }

        @InliningCutoff
        @Specialization(guards = {"isForeignObject(target)"}, limit = "InteropLibraryLimit")
        protected Object doForeignObject(Object target, Object propertyKey, Object[] optionalArgs,
                        @CachedLibrary("target") InteropLibrary interop,
                        @Cached ImportValueNode importValue,
                        @Cached ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                        @Shared("jsclassProf") @Cached JSClassProfile classProfile) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            if (interop.hasMembers(target)) {
                Object result = JSInteropUtil.readMemberOrDefault(target, key, null, interop, importValue, this);
                if (result == null) {
                    if (getContext().getLanguageOptions().hasForeignObjectPrototype()) {
                        JSDynamicObject prototype = foreignObjectPrototypeNode.execute(target);
                        Object receiver = JSRuntime.getArg(optionalArgs, 0, target);
                        return JSRuntime.nullToUndefined(classProfile.getJSClass(prototype).getHelper(prototype, receiver, key, this));
                    } else {
                        result = Undefined.instance;
                    }
                }
                return result;
            } else {
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSObject(target)", "!isForeignObject(target)"})
        protected Object doNonObject(Object target, Object propertyKey, Object[] optionalArgs) {
            throw Errors.createTypeErrorCalledOnNonObject();
        }
    }

    public abstract static class ReflectGetOwnPropertyDescriptorNode extends ReflectOperation {

        public ReflectGetOwnPropertyDescriptorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject reflectGetOwnPropertyDescriptor(Object target, Object key,
                        @Cached JSToPropertyKeyNode toPropertyKeyNode,
                        @Cached JSGetOwnPropertyNode getOwnPropertyNode,
                        @Cached FromPropertyDescriptorNode fromPropertyDescriptorNode) {
            ensureJSObject(target);
            Object propertyKey = toPropertyKeyNode.execute(key);
            PropertyDescriptor desc = getOwnPropertyNode.execute((JSDynamicObject) target, propertyKey);
            return fromPropertyDescriptorNode.execute(desc, getContext());
        }
    }

    public abstract static class ReflectGetPrototypeOfNode extends ReflectOperation {

        public ReflectGetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object reflectGetPrototypeOf(Object target) {
            ensureJSObject(target);
            return JSObject.getPrototype((JSDynamicObject) target);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ReflectHasNode extends ReflectOperation {

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        public ReflectHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doObject(JSObject target, Object propertyKey,
                        @Shared("jsclassProf") @Cached JSClassProfile jsclassProfile) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSObject.hasProperty(target, key, jsclassProfile);
        }

        @InliningCutoff
        @Specialization(guards = {"isForeignObject(target)"}, limit = "InteropLibraryLimit")
        protected Object doForeignObject(Object target, Object propertyKey,
                        @CachedLibrary("target") InteropLibrary interop,
                        @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                        @Shared("jsclassProf") @Cached JSClassProfile classProfile) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            if (interop.hasMembers(target)) {
                if (key instanceof TruffleString) {
                    boolean result = interop.isMemberExisting(target, Strings.toJavaString(toJavaStringNode, (TruffleString) key));
                    if (!result && getContext().getLanguageOptions().hasForeignObjectPrototype()) {
                        JSDynamicObject prototype = foreignObjectPrototypeNode.execute(target);
                        result = JSObject.hasProperty(prototype, key, classProfile);
                    }
                    return result;
                } else {
                    return false;
                }
            } else {
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSObject(target)", "!isForeignObject(target)"})
        protected Object doNonObject(Object target, Object propertyKey) {
            throw Errors.createTypeErrorCalledOnNonObject();
        }
    }

    public abstract static class ReflectIsExtensibleNode extends ReflectOperation {

        public ReflectIsExtensibleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectIsExtensible(Object target,
                        @Cached IsExtensibleNode isExtensibleNode) {
            ensureJSObject(target);
            return isExtensibleNode.executeBoolean((JSDynamicObject) target);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ReflectOwnKeysNode extends ReflectOperation {

        public ReflectOwnKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject reflectOwnKeys(JSObject target,
                        @Cached JSClassProfile jsclassProfile,
                        @Cached ListSizeNode listSize) {
            List<Object> list = JSObject.ownPropertyKeys(target, jsclassProfile);
            if (getContext().isOptionV8CompatibilityMode()) {
                list = JSRuntime.filterPrivateSymbols(list);
            }
            return JSArray.createLazyArray(getContext(), getRealm(), list, listSize.execute(list));
        }

        @Specialization(guards = {"isForeignObject(target)"}, limit = "InteropLibraryLimit")
        protected Object doForeignObject(Object target,
                        @CachedLibrary("target") InteropLibrary interop) {
            if (interop.hasMembers(target)) {
                try {
                    return interop.getMembers(target);
                } catch (UnsupportedMessageException e) {
                    throw Errors.createTypeErrorInteropException(target, e, "getMembers", this);
                }
            } else {
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSObject(target)", "!isForeignObject(target)"})
        protected Object doNonObject(Object target) {
            throw Errors.createTypeErrorCalledOnNonObject();
        }
    }

    public abstract static class ReflectPreventExtensionsNode extends ReflectOperation {

        public ReflectPreventExtensionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectPreventExtensions(Object target) {
            ensureJSObject(target);
            return JSObject.preventExtensions((JSDynamicObject) target);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ReflectSetNode extends ReflectOperation {

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        public ReflectSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSObject(target)"})
        protected boolean reflectSet(JSDynamicObject target, Object propertyKey, Object value, Object[] optionalArgs,
                        @Cached JSClassProfile jsclassProfile) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            Object receiver = JSRuntime.getArg(optionalArgs, 0, target);
            return JSObject.setWithReceiver(target, key, value, receiver, false, jsclassProfile, this);
        }

        @Specialization(guards = {"isForeignObject(target)"}, limit = "InteropLibraryLimit")
        protected Object doForeignObject(Object target, Object propertyKey, Object value, @SuppressWarnings("unused") Object[] optionalArgs,
                        @CachedLibrary("target") InteropLibrary interop,
                        @Cached ExportValueNode exportValue) {
            Object key = toPropertyKeyNode.execute(propertyKey);
            if (interop.hasMembers(target)) {
                JSInteropUtil.writeMember(target, key, value, interop, exportValue, this);
                return true;
            } else {
                throw Errors.createTypeErrorCalledOnNonObject();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSObject(target)", "!isForeignObject(target)"})
        protected Object doNonObject(Object target, Object propertyKey, Object value, Object[] optionalArgs) {
            throw Errors.createTypeErrorCalledOnNonObject();
        }
    }

    public abstract static class ReflectSetPrototypeOfNode extends ReflectOperation {

        public ReflectSetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectSetPrototypeOf(Object target, Object proto) {
            ensureJSObject(target);
            if (!(JSDynamicObject.isJSDynamicObject(proto) || proto == Null.instance) || proto == Undefined.instance) {
                throw Errors.createTypeErrorInvalidPrototype(proto);
            }
            return JSObject.setPrototype((JSDynamicObject) target, (JSDynamicObject) proto);
        }
    }
}
