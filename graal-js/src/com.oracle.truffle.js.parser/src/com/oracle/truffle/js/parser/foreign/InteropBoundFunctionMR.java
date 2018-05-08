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
package com.oracle.truffle.js.parser.foreign;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInvokeNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

@MessageResolution(receiverType = InteropBoundFunction.class)
public class InteropBoundFunctionMR {

    @Resolve(message = "EXECUTE")
    abstract static class ExecuteNode extends Node {

        private final ConditionProfile rejected = ConditionProfile.createBinaryProfile();

        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createExecute();
        @Child private ExportValueNode export;
        @CompilationFinal ContextReference<JSRealm> contextRef;

        public Object access(InteropBoundFunction target, Object[] args) {
            DynamicObject function = target.getFunction();
            if (contextRef == null || export == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(function);
                contextRef = context.getLanguage().getContextReference();
                export = insert(ExportValueNode.create(context.getLanguage()));
            }
            JSContext context = contextRef.get().getContext();
            context.interopBoundaryEnter();
            Object result = null;
            boolean asyncFunction = isAsyncFunction(function);
            try {
                result = export.executeWithTarget(callNode.executeInterop(target, args), Undefined.instance);
            } finally {
                context.interopBoundaryExit();
            }
            if (asyncFunction && result != null && JSPromise.isJSPromise(result)) {
                /*
                 * InteropCompletePromises semantics: interop calls to async functions return the
                 * async resolved value (if any). If the promise resolves, its value is made
                 * available by flushing the queue of pending jobs.
                 */
                DynamicObject promise = (DynamicObject) result;
                if (rejected.profile(JSPromise.isRejected(promise))) {
                    Object rejectReason = promise.get(JSPromise.PROMISE_RESULT);
                    throw UserScriptException.create(rejectReason);
                } else {
                    assert JSPromise.isFulfilled(promise);
                    return promise.get(JSPromise.PROMISE_RESULT);
                }
            }
            return result;
        }

        private static boolean isAsyncFunction(DynamicObject target) {
            if (JSTruffleOptions.InteropCompletePromises) {
                JSFunctionData functionData = JSRuntime.getFunctionData(target);
                if (functionData != null) {
                    return functionData.isAsync();
                }
            }
            return false;
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class InvokeNode extends Node {

        @Child private JSInteropInvokeNode callNode;
        @Child private ExportValueNode export;
        @CompilationFinal ContextReference<JSRealm> contextRef;

        public Object access(InteropBoundFunction target, String id, Object[] args) {
            DynamicObject function = target.getFunction();
            if (callNode == null || export == null || contextRef == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(function);
                callNode = insert(JSInteropInvokeNode.create(context));
                export = insert(ExportValueNode.create(context.getLanguage()));
                contextRef = context.getLanguage().getContextReference();
            }
            JSContext context = contextRef.get().getContext();
            context.interopBoundaryEnter();
            try {
                return export.executeWithTarget(callNode.execute(function, id, args), Undefined.instance);
            } finally {
                context.interopBoundaryExit();
            }
        }
    }

    @Resolve(message = "NEW")
    abstract static class NewNode extends Node {

        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createNew();
        @Child private ExportValueNode export;
        @CompilationFinal ContextReference<JSRealm> contextRef;

        public Object access(InteropBoundFunction target, Object[] args) {
            DynamicObject function = target.getFunction();
            if (contextRef == null || export == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(function);
                contextRef = context.getLanguage().getContextReference();
                export = insert(ExportValueNode.create(context.getLanguage()));
            }
            JSContext context = contextRef.get().getContext();
            context.interopBoundaryEnter();
            try {
                return export.executeWithTarget(callNode.executeInterop(function, args), Undefined.instance);
            } finally {
                context.interopBoundaryExit();
            }
        }
    }

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {

        @Child private ReadElementNode readNode;
        @Child private ExportValueNode export;
        @Child private JSForeignToJSTypeNode castKey = JSForeignToJSTypeNode.create();

        public Object access(InteropBoundFunction target, Object key) {
            DynamicObject function = target.getFunction();
            if (readNode == null || export == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(function);
                readNode = insert(ReadElementNode.create(context));
                export = insert(ExportValueNode.create(context.getLanguage()));
            }
            return export.executeWithTarget(readNode.executeWithTargetAndIndex(function, castKey.executeWithTarget(key)), function);
        }

    }

    @Resolve(message = "WRITE")
    abstract static class WriteNode extends Node {

        @Child private WriteElementNode writeNode;
        @Child private JSForeignToJSTypeNode castKey = JSForeignToJSTypeNode.create();
        @Child private JSForeignToJSTypeNode castValue = JSForeignToJSTypeNode.create();

        public Object access(InteropBoundFunction target, Object key, Object value) {
            DynamicObject function = target.getFunction();
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(function);
                writeNode = insert(WriteElementNode.create(context, false));
            }
            writeNode.executeWithTargetAndIndexAndValue(function, castKey.executeWithTarget(key), castValue.executeWithTarget(value));
            return value;
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    abstract static class IsExecutableNode extends Node {

        @SuppressWarnings("unused")
        public Object access(InteropBoundFunction target) {
            return true;
        }
    }

    @Resolve(message = "IS_INSTANTIABLE")
    abstract static class IsInstantiableNode extends Node {

        public Object access(InteropBoundFunction target) {
            return JSRuntime.isConstructor(target.getFunction());
        }
    }

    @Resolve(message = "KEYS")
    abstract static class KeysNode extends Node {

        @TruffleBoundary // due to toArray()
        public Object access(InteropBoundFunction target, @SuppressWarnings("unused") boolean internal) {
            DynamicObject function = target.getFunction();
            Object[] keys = JSObject.enumerableOwnNames(function).toArray();
            JSContext context = JSObject.getJSContext(function);
            return JSArray.createConstant(context, keys);
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoNode extends Node {

        @Child protected JSToPropertyKeyNode toKey = JSToPropertyKeyNode.create();
        @Child protected JSForeignToJSTypeNode cast = JSForeignToJSTypeNode.create();

        public Object access(InteropBoundFunction target, Object key) {
            PropertyDescriptor desc = JSObject.getOwnProperty(target.getFunction(), toKey.execute(cast.executeWithTarget(key)));
            if (desc == null) {
                if (JSObject.isExtensible(target.getFunction())) {
                    return KeyInfo.INSERTABLE;
                }
                return 0;
            }

            boolean readable = true;
            boolean writable = desc.getIfHasWritable(true);
            boolean invocable = desc.isDataDescriptor() & JSRuntime.isCallable(desc.getValue());
            boolean removable = desc.getIfHasConfigurable(false);
            return (readable ? KeyInfo.READABLE : 0) | (writable ? KeyInfo.MODIFIABLE : 0) | (invocable ? KeyInfo.INVOCABLE : 0) | (removable ? KeyInfo.REMOVABLE : 0);
        }
    }

    @Resolve(message = "REMOVE")
    abstract static class RemoveNode extends Node {

        @Child private DeletePropertyNode deleteNode;
        @Child protected JSToPropertyKeyNode toKey = JSToPropertyKeyNode.create();
        @Child protected JSForeignToJSTypeNode cast = JSForeignToJSTypeNode.create();

        public Object access(InteropBoundFunction target, Object key) {
            if (deleteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target.getFunction());
                deleteNode = insert(DeletePropertyNode.create(true, context));
            }
            Object castKey = toKey.execute(cast.executeWithTarget(key));
            return deleteNode.executeEvaluated(target.getFunction(), castKey);
        }

    }
}
