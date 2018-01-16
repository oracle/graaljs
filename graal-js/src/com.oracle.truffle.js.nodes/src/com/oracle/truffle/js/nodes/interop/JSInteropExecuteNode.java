/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSProxyCallNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeFactory.JSInteropDispatchCallNodeGen;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

@ImportStatic(JSFunction.class)
public abstract class JSInteropExecuteNode extends JavaScriptBaseNode {

    public static JSInteropExecuteNode createExecute() {
        return JSInteropDispatchCallNodeGen.create(false);
    }

    public static JSInteropExecuteNode createNew() {
        return JSInteropDispatchCallNodeGen.create(true);
    }

    public abstract Object executeInterop(Object target, Object[] args);

    abstract static class JSInteropDispatchCall extends JSInteropExecuteNode {

        @Child private JSFunctionCallNode call;
        @Child private AbstractFunctionArgumentsNode argumentsNode;
        @Child private JSForeignToJSTypeNode convertReceiverNode;
        @Child private JSForeignToJSTypeNode convertArgsNode;
        private final boolean isNew;

        protected JSInteropDispatchCall(boolean isNew) {
            this.call = JSFunctionCallNode.create(isNew);
            this.isNew = isNew;
        }

        @Specialization(guards = {"cachedTarget==target", "isBound"})
        public Object doCachedBound(@SuppressWarnings("unused") DynamicObject target, Object[] args,
                        @Cached("target") DynamicObject cachedTarget,
                        @SuppressWarnings("unused") @Cached("isBoundFunction(target)") boolean isBound) {
            return call.executeCall(JSArguments.create(Null.instance, cachedTarget, prepare(args)));
        }

        @Specialization(guards = {"cachedProxy==proxy", "!isBound", "isJSProxy(proxy)"})
        public Object doCachedUnbound(@SuppressWarnings("unused") DynamicObject proxy, Object[] arguments,
                        @Cached("proxy") DynamicObject cachedProxy,
                        @SuppressWarnings("unused") @Cached("isBoundFunction(proxy)") boolean isBound,
                        @Cached("createProxyCallNode(cachedProxy)") JSProxyCallNode proxyCallNode) {
            return proxyCallNode.execute(JSArguments.create(Null.instance, cachedProxy, prepare(arguments)));
        }

        @Specialization(guards = {"cachedTarget==target", "!isBound", "!isJSProxy(target)"})
        public Object doCachedUnbound(@SuppressWarnings("unused") DynamicObject target, Object[] args,
                        @Cached("target") DynamicObject cachedTarget,
                        @SuppressWarnings("unused") @Cached("isBoundFunction(target)") boolean isBound) {
            Object[] shifted = new Object[args.length - 1];
            System.arraycopy(args, 1, shifted, 0, shifted.length);
            return call.executeCall(JSArguments.create(prepareReceiver(args[0]), cachedTarget, prepare(shifted)));
        }

        @Specialization
        public Object doGeneric(DynamicObject target, Object[] args) {
            if (JSFunction.isBoundFunction(target)) {
                return call.executeCall(JSArguments.create(Null.instance, target, prepare(args)));
            } else {
                Object[] shifted = new Object[args.length - 1];
                System.arraycopy(args, 1, shifted, 0, shifted.length);
                return call.executeCall(JSArguments.create(prepareReceiver(args[0]), target, prepare(shifted)));
            }
        }

        private Object[] prepare(Object[] shifted) {
            if (convertArgsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                convertArgsNode = insert(JSForeignToJSTypeNodeGen.create());
            }
            for (int i = 0; i < shifted.length; i++) {
                shifted[i] = convertArgsNode.executeWithTarget(shifted[i]);
            }
            return shifted;
        }

        private Object prepareReceiver(Object object) {
            if (convertReceiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                convertReceiverNode = insert(JSForeignToJSTypeNodeGen.create());
            }
            return convertReceiverNode.executeWithTarget(object);
        }

        protected JSProxyCallNode createProxyCallNode(DynamicObject proxy) {
            return JSProxyCallNode.create(JSObject.getJSContext(proxy), isNew, false);
        }
    }
}
