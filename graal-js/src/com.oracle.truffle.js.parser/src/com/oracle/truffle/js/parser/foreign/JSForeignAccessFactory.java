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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.graalvm.collections.EconomicSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInvokeNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

@MessageResolution(receiverType = DynamicObject.class)
public class JSForeignAccessFactory {

    @Resolve(message = "EXECUTE")
    abstract static class ExecuteNode extends Node {

        private final ConditionProfile rejected = ConditionProfile.createBinaryProfile();

        @Child private IsCallableNode isCallableNode = IsCallableNode.create();
        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createExecute();
        @Child private ExportValueNode export;
        @CompilationFinal ContextReference<JSRealm> contextRef;

        public Object access(DynamicObject target, Object[] args) {
            if (isCallableNode.executeBoolean(target)) {
                return common(target, Undefined.instance, args);
            } else {
                throw UnsupportedTypeException.raise(new Object[]{target});
            }
        }

        public Object access(InteropBoundFunction target, Object[] args) {
            return common(target.getFunction(), target.getReceiver(), args);
        }

        private Object common(DynamicObject function, Object receiver, Object[] args) {
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
                result = export.executeWithTarget(callNode.execute(function, receiver, args), Undefined.instance);
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

        public Object access(DynamicObject target, String id, Object[] args) {
            if (callNode == null || export == null || contextRef == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target);
                callNode = insert(JSInteropInvokeNode.create(context));
                export = insert(ExportValueNode.create(context.getLanguage()));
                contextRef = context.getLanguage().getContextReference();
            }
            JSContext context = contextRef.get().getContext();
            context.interopBoundaryEnter();
            try {
                return export.executeWithTarget(callNode.execute(target, id, args), Undefined.instance);
            } finally {
                context.interopBoundaryExit();
            }
        }

        public Object access(InteropBoundFunction target, String id, Object[] args) {
            return access(target.getFunction(), id, args);
        }
    }

    @Resolve(message = "NEW")
    abstract static class NewNode extends Node {

        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createNew();
        @Child private ExportValueNode export;
        @CompilationFinal ContextReference<JSRealm> contextRef;

        public Object access(DynamicObject target, Object[] args) {
            if (contextRef == null || export == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target);
                contextRef = context.getLanguage().getContextReference();
                export = insert(ExportValueNode.create(context.getLanguage()));
            }
            JSContext context = contextRef.get().getContext();
            context.interopBoundaryEnter();
            try {
                return export.executeWithTarget(callNode.execute(target, Undefined.instance, args), Undefined.instance);
            } finally {
                context.interopBoundaryExit();
            }
        }

        public Object access(InteropBoundFunction target, Object[] args) {
            return access(target.getFunction(), args);
        }
    }

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {

        @Child private ReadElementNode readNode;
        @Child private ExportValueNode export;
        @Child private JSForeignToJSTypeNode castKey = JSForeignToJSTypeNode.create();

        public Object access(DynamicObject target, Object key) {
            if (readNode == null || export == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target);
                readNode = insert(ReadElementNode.create(context));
                export = insert(ExportValueNode.create(context.getLanguage()));
            }
            return export.executeWithTarget(readNode.executeWithTargetAndIndex(target, castKey.executeWithTarget(key)), target);
        }

        public Object access(InteropBoundFunction target, Object key) {
            return access(target.getFunction(), key);
        }
    }

    @Resolve(message = "WRITE")
    abstract static class WriteNode extends Node {

        @Child private WriteElementNode writeNode;
        @Child private JSForeignToJSTypeNode castKey = JSForeignToJSTypeNode.create();
        @Child private JSForeignToJSTypeNode castValue = JSForeignToJSTypeNode.create();

        public Object access(DynamicObject target, Object key, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target);
                writeNode = insert(WriteElementNode.create(context, false));
            }
            writeNode.executeWithTargetAndIndexAndValue(target, castKey.executeWithTarget(key), castValue.executeWithTarget(value));
            return value;
        }

        public Object access(InteropBoundFunction target, Object key, Object value) {
            return access(target.getFunction(), key, value);
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSizeNode extends Node {

        public int access(DynamicObject target) {
            if (JSArray.isJSArray(target)) {
                long size = JSArray.arrayGetLength(target);
                if (size < Integer.MAX_VALUE) {
                    return (int) size;
                } else {
                    throw UnsupportedMessageException.raise(Message.GET_SIZE);
                }
            } else if (JSArrayBufferView.isJSArrayBufferView(target)) {
                return JSArrayBufferView.typedArrayGetLength(target);
            } else if (JSString.isJSString(target)) {
                return JSString.getStringLength(target);
            } else {
                throw UnsupportedMessageException.raise(Message.GET_SIZE);
            }
        }
    }

    @Resolve(message = "UNBOX")
    abstract static class UnboxNode extends Node {

        public Object access(DynamicObject target) {
            JSClass builtinClass = JSObject.getJSClass(target);
            if (builtinClass == JSNumber.INSTANCE) {
                return JSRuntime.toNumber(target);
            } else if (builtinClass == JSString.INSTANCE) {
                return JSRuntime.toString(target);
            } else if (builtinClass == JSBoolean.INSTANCE) {
                return JSBoolean.valueOf(target);
            }
            throw UnsupportedMessageException.raise(Message.UNBOX);
        }
    }

    @Resolve(message = "IS_BOXED")
    abstract static class IsBoxedNode extends Node {

        public Object access(DynamicObject target) {
            JSClass builtinClass = JSObject.getJSClass(target);
            if (builtinClass == JSNumber.INSTANCE) {
                return true;
            } else if (builtinClass == JSString.INSTANCE) {
                return true;
            } else if (builtinClass == JSBoolean.INSTANCE) {
                return true;
            }
            return false;
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    abstract static class IsExecutableNode extends Node {

        public Object access(DynamicObject target) {
            return JSFunction.isJSFunction(target);
        }
    }

    @Resolve(message = "IS_NULL")
    abstract static class IsNullNode extends Node {

        public Object access(DynamicObject target) {
            return target.getShape().getObjectType() == Null.NULL_CLASS;
        }
    }

    @Resolve(message = "IS_INSTANTIABLE")
    abstract static class IsInstantiableNode extends Node {

        public Object access(DynamicObject target) {
            return JSRuntime.isConstructor(target);
        }

        public Object access(InteropBoundFunction target) {
            return JSRuntime.isConstructor(target.getFunction());
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSizeNode extends Node {

        public Object access(DynamicObject target) {
            ObjectType objectType = target.getShape().getObjectType();
            return objectType instanceof JSArray || objectType instanceof JSArrayBufferView || objectType instanceof JSString;
        }
    }

    @Resolve(message = "HAS_KEYS")
    abstract static class HasKeysNode extends Node {

        public Object access(Object target) {
            return JSObject.isJSObject(target);
        }
    }

    @Resolve(message = "KEYS")
    abstract static class KeysNode extends Node {

        @TruffleBoundary
        public Object access(DynamicObject target, @SuppressWarnings("unused") boolean internal) {
            EconomicSet<Object> keySet = EconomicSet.create();
            for (DynamicObject proto = target; proto != Null.instance; proto = JSObject.getPrototype(proto)) {
                for (Object key : JSObject.ownPropertyKeys(proto)) {
                    if (key instanceof String) {
                        keySet.add(key);
                    }
                }
                if (JSProxy.isProxy(proto)) {
                    break;
                }
            }
            Object[] keys = keySet.toArray(new Object[keySet.size()]);
            JSContext context = JSObject.getJSContext(target);
            return JSArray.createConstant(context, keys);
        }

        public Object access(InteropBoundFunction target, boolean internal) {
            return access(target.getFunction(), internal);
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoNode extends Node {

        @Child protected JSToPropertyKeyNode toKey = JSToPropertyKeyNode.create();
        @Child protected JSForeignToJSTypeNode cast = JSForeignToJSTypeNode.create();

        public Object access(DynamicObject target, Object key) {
            Object propertyKey = toKey.execute(cast.executeWithTarget(key));
            PropertyDescriptor desc = null;
            for (DynamicObject proto = target; proto != Null.instance; proto = JSObject.getPrototype(proto)) {
                desc = JSObject.getOwnProperty(proto, propertyKey);
                if (desc != null) {
                    break;
                }
                if (JSProxy.isProxy(proto)) {
                    break;
                }
            }
            if (desc == null) {
                if (JSObject.isExtensible(target)) {
                    return KeyInfo.INSERTABLE;
                }
                return 0;
            }

            boolean readable = true;
            boolean writable = desc.getIfHasWritable(true);
            boolean invocable = desc.isDataDescriptor() & JSRuntime.isCallable(desc.getValue());
            boolean removable = desc.getConfigurable();
            return (readable ? KeyInfo.READABLE : 0) | (writable ? KeyInfo.MODIFIABLE : 0) | (invocable ? KeyInfo.INVOCABLE : 0) | (removable ? KeyInfo.REMOVABLE : 0);
        }

        public Object access(InteropBoundFunction target, Object key) {
            return access(target.getFunction(), key);
        }
    }

    @Resolve(message = "REMOVE")
    abstract static class RemoveNode extends Node {

        @Child private DeletePropertyNode deleteNode;
        @Child protected JSToPropertyKeyNode toKey = JSToPropertyKeyNode.create();
        @Child protected JSForeignToJSTypeNode cast = JSForeignToJSTypeNode.create();

        public Object access(DynamicObject target, Object key) {
            if (deleteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = JSObject.getJSContext(target);
                deleteNode = insert(DeletePropertyNode.create(true, context));
            }
            Object castKey = toKey.execute(cast.executeWithTarget(key));
            return deleteNode.executeEvaluated(target, castKey);
        }

        public Object access(InteropBoundFunction target, Object key) {
            return access(target.getFunction(), key);
        }
    }

    // ##### Extra, non-standard interop messages

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.AllocateTypedArrayMessage")
    abstract static class AllocateTypedArrayMR extends Node {
        public Object access(@SuppressWarnings("unused") VirtualFrame frame, Object target, Object buffer) {
            assert buffer instanceof ByteBuffer;
            assert target instanceof DynamicObject;
            JSContext context = JSObject.getJSContext((DynamicObject) target);
            return createArray((ByteBuffer) buffer, context);
        }

        @TruffleBoundary
        private static DynamicObject createArray(ByteBuffer buffer, JSContext context) {
            return JSArrayBuffer.createDirectArrayBuffer(context, buffer);
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.IsStringifiableMessage")
    abstract static class IsStringifiableMR extends Node {

        public boolean access(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object target, Object value) {
            return value != Undefined.instance && !JSFunction.isJSFunction(value) && !(value instanceof Symbol);
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.IsArrayMessage")
    abstract static class IsArrayMR extends Node {

        public boolean access(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject target, @SuppressWarnings("unused") Object arg) {
            return target.getShape().getObjectType() instanceof JSArray;
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.DoubleToStringMessage")
    abstract static class DoubleToStringMR extends Node {

        @Child protected JSDoubleToStringNode toStringNode = JSDoubleToStringNodeGen.create();

        public String access(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") DynamicObject target, double value) {
            return toStringNode.executeString(value);
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.HasPropertyMessage")
    abstract static class HasPropertyMR extends Node {

        @Child private JSHasPropertyNode has = JSHasPropertyNode.create();

        public boolean access(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject target, Object key) {
            return has.executeBoolean(target, key);
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.TryConvertMessage")
    abstract static class TryConvertMR extends Node {
        public Object access(@SuppressWarnings("unused") VirtualFrame frame, Object target) {
            assert target instanceof DynamicObject && JSArrayBufferView.isJSArrayBufferView(target);
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer((DynamicObject) target);
            if (JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer)) {
                return extractByteBuffer(target, arrayBuffer);
            }
            return createZeroLengthBuffer(target);
        }

        @TruffleBoundary
        private static Object createZeroLengthBuffer(Object target) {
            JSContext context = JSObject.getJSContext((DynamicObject) target);
            return JSArray.createEmptyZeroLength(context);
        }

        @TruffleBoundary
        private static Object extractByteBuffer(Object target, DynamicObject arrayBuffer) {
            int byteOffset = JSArrayBufferView.getByteOffset((DynamicObject) target, true, JSObject.getJSContext(arrayBuffer));
            ByteBuffer buffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
            Object byteBuffer = ((ByteBuffer) buffer.duplicate().position(byteOffset)).slice().order(ByteOrder.nativeOrder());
            JSContext context = JSObject.getJSContext((DynamicObject) target);
            return JSArray.createConstant(context, new Object[]{byteBuffer});
        }
    }

    @Resolve(message = "com.oracle.truffle.js.parser.foreign.JSForeignAccessExtraMessages.GetJSONConvertedMessage")
    abstract static class GetJSONConvertedMR extends Node {

        private static final String KEY = "toJSON";

        @Child private ReadElementNode readNode;
        @Child private JSFunctionCallNode callToJSONFunction;

        public Object access(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject target, @SuppressWarnings("unused") Object arg) {
            if (JSDate.isJSDate(target)) {
                if (readNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    readNode = insert(ReadElementNode.create(JSObject.getJSContext(target)));
                }
                Object toJSON = readNode.executeWithTargetAndIndex(target, KEY);
                if (JSFunction.isJSFunction(toJSON)) {
                    if (callToJSONFunction == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        callToJSONFunction = insert(JSFunctionCallNode.createCall());
                    }
                    return callToJSONFunction.executeCall(JSArguments.createZeroArg(target, toJSON));
                }
            }
            return false;
        }
    }

    @CanResolve
    public abstract static class CanResolveNode extends Node {

        @Child private IsObjectNode isJSObjectNode = IsObjectNode.create();

        protected boolean test(TruffleObject receiver) {
            return isJSObjectNode.executeBoolean(receiver);
        }
    }
}
