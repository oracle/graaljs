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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;

public abstract class JSInteropExecuteNode extends JavaScriptBaseNode {
    private static final Uncached UNCACHED_EXECUTE = new Uncached(false);
    private static final Uncached UNCACHED_NEW = new Uncached(true);

    protected JSInteropExecuteNode() {
    }

    public static JSInteropExecuteNode createExecute() {
        return new Cached(false);
    }

    public static JSInteropExecuteNode createNew() {
        return new Cached(true);
    }

    public abstract Object execute(DynamicObject function, Object thisArg, Object[] args) throws UnsupportedMessageException;

    private static class Cached extends JSInteropExecuteNode {
        @Child private JSFunctionCallNode call;
        @Child private IsCallableNode isCallableNode;
        @Child private JSForeignToJSTypeNode convertArgsNode;

        protected Cached(boolean isNew) {
            this.call = JSFunctionCallNode.create(isNew);
            this.isCallableNode = IsCallableNode.create();
            this.convertArgsNode = JSForeignToJSTypeNode.create();
        }

        @Override
        public Object execute(DynamicObject function, Object thisArg, Object[] args) throws UnsupportedMessageException {
            if (!isCallableNode.executeBoolean(function)) {
                throw UnsupportedMessageException.create();
            }
            return call.executeCall(JSArguments.create(thisArg, function, prepare(args)));
        }

        private Object[] prepare(Object[] shifted) {
            for (int i = 0; i < shifted.length; i++) {
                shifted[i] = convertArgsNode.executeWithTarget(shifted[i]);
            }
            return shifted;
        }
    }

    private static class Uncached extends JSInteropExecuteNode {
        private final boolean isNew;

        protected Uncached(boolean isNew) {
            this.isNew = isNew;
        }

        @Override
        public Object execute(DynamicObject function, Object thisArg, Object[] args) throws UnsupportedMessageException {
            if (!IsCallableNode.getUncached().executeBoolean(function)) {
                throw UnsupportedMessageException.create();
            }
            Object[] preparedArgs = prepare(args);
            if (isNew) {
                if (JSFunction.isJSFunction(function)) {
                    return IndirectCallNode.getUncached().call(JSFunction.getConstructTarget(function), JSArguments.create(thisArg, function, preparedArgs));
                } else if (JSProxy.isProxy(function)) {
                    // TODO
                    throw UnsupportedMessageException.create();
                } else {
                    throw UnsupportedMessageException.create();
                }
            } else {
                return JSRuntime.call(function, thisArg, preparedArgs);
            }
        }

        private static Object[] prepare(Object[] shifted) {
            for (int i = 0; i < shifted.length; i++) {
                shifted[i] = JSForeignToJSTypeNode.getUncached().executeWithTarget(shifted[i]);
            }
            return shifted;
        }
    }

    public static JSInteropExecuteNode getUncachedExecute() {
        return UNCACHED_EXECUTE;
    }

    public static JSInteropExecuteNode getUncachedNew() {
        return UNCACHED_NEW;
    }
}
