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
package com.oracle.truffle.js.builtins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
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
import com.oracle.truffle.js.nodes.access.ToPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSReflectUtils;

/**
 * Contains builtins for Reflect (ES2015, 26.1).
 */
public class ReflectBuiltins extends JSBuiltinsContainer.SwitchEnum<ReflectBuiltins.Reflect> {
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
                return ReflectConstructNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case defineProperty:
                return ReflectDefinePropertyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case deleteProperty:
                return ReflectDeletePropertyNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case get:
                return ReflectGetNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
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
                return ReflectSetNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case setPrototypeOf:
                return ReflectSetPrototypeOfNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ReflectOperation extends JSBuiltinNode {
        private final BranchProfile errorBranch = BranchProfile.create();

        public ReflectOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected void ensureObject(Object target) {
            if (!JSRuntime.isObject(target)) {
                errorBranch.enter();
                throw Errors.createTypeError("called on non-object");
            }
        }
    }

    public abstract static class ReflectApplyNode extends JSBuiltinNode {

        @Child private JSFunctionCallNode call = JSFunctionCallNode.createCall();
        @Child private JSToObjectArrayNode toObjectArray;

        public ReflectApplyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toObjectArray = JSToObjectArrayNodeGen.create(getContext());
        }

        @Specialization(guards = "isJSFunction(target)")
        protected final Object applyFunction(DynamicObject target, Object thisArgument, Object argumentsList) {
            return applyCallable(target, thisArgument, argumentsList);
        }

        @Specialization(guards = "isCallable(target)", replaces = "applyFunction")
        protected final Object applyCallable(Object target, Object thisArgument, Object argumentsList) {
            Object[] applyUserArgs = toObjectArray.executeObjectArray(argumentsList);
            Object[] passedOnArguments = JSArguments.create(thisArgument, target, applyUserArgs);
            return call.executeCall(passedOnArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable(target)")
        protected static Object error(Object target, Object thisArgument, Object argumentsList) {
            throw Errors.createTypeErrorCallableExpected();
        }
    }

    public abstract static class ReflectConstructNode extends ReflectOperation {

        @Child private JSFunctionCallNode constructCall = JSFunctionCallNode.createNewTarget();
        @Child private JSToObjectArrayNode toObjectArray;

        public ReflectConstructNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toObjectArray = JSToObjectArrayNodeGen.create(context);
        }

        @Specialization
        protected Object reflectConstruct(Object[] args) {
            Object target = JSRuntime.getArgOrUndefined(args, 0);
            Object argumentsList = JSRuntime.getArgOrUndefined(args, 1);
            ensureConstructor(target);

            Object newTarget = JSRuntime.getArg(args, 2, target);
            ensureConstructor(newTarget);

            if (!JSRuntime.isObject(argumentsList)) {
                throw Errors.createTypeError("Reflect.construct: Arguments list has wrong type");
            }
            Object[] applyUserArgs = toObjectArray.executeObjectArray(argumentsList);
            Object[] passedOnArguments = JSArguments.createWithNewTarget(JSFunction.CONSTRUCT, target, newTarget, applyUserArgs);
            return constructCall.executeCall(passedOnArguments);
        }

        private void ensureConstructor(Object obj) {
            ensureObject(obj);
            DynamicObject constrObj = (DynamicObject) (obj);
            if (!JSRuntime.isConstructor(constrObj)) {
                throw Errors.createTypeErrorConstructorExpected();
            }
        }
    }

    public abstract static class ReflectDefinePropertyNode extends ReflectOperation {

        public ReflectDefinePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectDefineProperty(Object target, Object propertyKey, Object attributes,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode,
                        @Cached("create(getContext())") ToPropertyDescriptorNode toPropertyDescriptorNode) {
            ensureObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            PropertyDescriptor descriptor = (PropertyDescriptor) toPropertyDescriptorNode.execute(attributes);
            return JSObject.defineOwnProperty((DynamicObject) target, key, descriptor);
        }
    }

    public abstract static class ReflectDeletePropertyNode extends ReflectOperation {

        public ReflectDeletePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectDeleteProperty(Object target, Object propertyKey,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
            ensureObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSObject.delete((DynamicObject) target, key);
        }
    }

    public abstract static class ReflectGetNode extends ReflectOperation {

        public ReflectGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object reflectGet(Object[] args,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
            Object target = JSRuntime.getArgOrUndefined(args, 0);
            Object propertyKey = JSRuntime.getArgOrUndefined(args, 1);
            Object receiver = JSRuntime.getArg(args, 2, target);
            ensureObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSReflectUtils.performOrdinaryGet((DynamicObject) target, key, receiver);
        }
    }

    public abstract static class ReflectGetOwnPropertyDescriptorNode extends ReflectOperation {

        public ReflectGetOwnPropertyDescriptorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject reflectGetOwnPropertyDescriptor(Object target, Object key,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
            ensureObject(target);
            Object propertyKey = toPropertyKeyNode.execute(key);
            PropertyDescriptor desc = JSObject.getOwnProperty((DynamicObject) target, propertyKey);
            return JSRuntime.fromPropertyDescriptor(desc, getContext());
        }
    }

    public abstract static class ReflectGetPrototypeOfNode extends ReflectOperation {

        public ReflectGetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object reflectGetPrototypeOf(Object target) {
            ensureObject(target);
            return JSObject.getPrototype((DynamicObject) target);
        }
    }

    public abstract static class ReflectHasNode extends ReflectOperation {

        public ReflectHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectHas(Object target, Object propertyKey,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
            ensureObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            return JSObject.hasProperty((DynamicObject) target, key);
        }
    }

    public abstract static class ReflectIsExtensibleNode extends ReflectOperation {

        public ReflectIsExtensibleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectIsExtensible(Object target) {
            ensureObject(target);
            return JSObject.isExtensible((DynamicObject) target);
        }
    }

    public abstract static class ReflectOwnKeysNode extends ReflectOperation {

        public ReflectOwnKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject reflectOwnKeys(Object target) {
            ensureObject(target);
            List<Object> list = JSObject.ownPropertyKeysList((DynamicObject) target);
            return JSArray.createConstant(getContext(), Boundaries.listToArray(list));
        }
    }

    public abstract static class ReflectPreventExtensionsNode extends ReflectOperation {

        public ReflectPreventExtensionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectPreventExtensions(Object target) {
            ensureObject(target);
            return JSObject.preventExtensions((DynamicObject) target);
        }
    }

    public abstract static class ReflectSetNode extends ReflectOperation {

        public ReflectSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static boolean isProxy(Object[] args) {
            return args.length > 0 && JSProxy.isProxy(args[0]);
        }

        @Specialization(guards = "isProxy(args)")
        protected boolean reflectSetProxy(Object[] args,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode,
                        @Cached("create()") JSToBooleanNode toBooleanNode) {
            Object proxy = JSRuntime.getArgOrUndefined(args, 0);
            Object propertyKey = JSRuntime.getArgOrUndefined(args, 1);
            Object value = JSRuntime.getArgOrUndefined(args, 2);

            ensureObject(proxy);
            Object key = toPropertyKeyNode.execute(propertyKey);
            DynamicObject proxyObj = (DynamicObject) proxy;

            DynamicObject handler = JSProxy.getHandler(proxyObj);
            TruffleObject pxTarget = JSProxy.getTarget(proxyObj);
            DynamicObject trap = JSProxy.getTrapFromObject(handler, JSProxy.SET);

            Object[] trapArgs = new Object[]{pxTarget, key, value, proxyObj};
            boolean booleanTrapResult = toBooleanNode.executeBoolean(JSRuntime.call(trap, handler, trapArgs));
            if (!booleanTrapResult) {
                return false;
            }
            return JSProxy.checkProxySetTrapInvariants(proxyObj, key, value);
        }

        @Specialization(guards = "!isProxy(args)")
        protected boolean reflectSet(Object[] args,
                        @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
            Object target = JSRuntime.getArgOrUndefined(args, 0);
            Object propertyKey = JSRuntime.getArgOrUndefined(args, 1);
            Object value = JSRuntime.getArgOrUndefined(args, 2);
            ensureObject(target);
            Object key = toPropertyKeyNode.execute(propertyKey);
            Object receiver = JSRuntime.getArg(args, 3, target);
            return JSReflectUtils.performOrdinarySet((DynamicObject) target, key, value, receiver);
        }
    }

    public abstract static class ReflectSetPrototypeOfNode extends ReflectOperation {

        public ReflectSetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean reflectSetPrototypeOf(Object target, Object proto) {
            ensureObject(target);
            if (!(JSObject.isJSObject(proto) || proto == Null.instance) || proto == Undefined.instance) {
                throw Errors.createTypeErrorInvalidPrototype(proto);
            }
            return JSObject.setPrototype((DynamicObject) target, (DynamicObject) proto);
        }
    }
}
