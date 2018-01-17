/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeFactory.JSInteropDispatchCallNodeGen;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

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
        @Child private JSForeignToJSTypeNode convertArgsNode;

        protected JSInteropDispatchCall(boolean isNew) {
            this.call = JSFunctionCallNode.create(isNew);
        }

        @Specialization
        public Object doJSFunction(DynamicObject target, Object[] args) {
            return call.executeCall(JSArguments.create(Undefined.instance, target, prepare(args)));
        }

        @Specialization
        public Object doInteropBoundFunction(InteropBoundFunction boundFunction, Object[] args) {
            return call.executeCall(JSArguments.create(boundFunction.getReceiver(), boundFunction.getFunction(), prepare(args)));
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
    }
}
