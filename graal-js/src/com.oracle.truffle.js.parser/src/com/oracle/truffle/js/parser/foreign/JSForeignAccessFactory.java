/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser.foreign;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
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
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteAfterReadNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropWrite;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
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
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

@MessageResolution(receiverType = DynamicObject.class)
public class JSForeignAccessFactory {

    static JSContext interopCallEnter(DynamicObject target) {
        JSContext context = JSObject.getJSContext(target);
        context.interopBoundaryEnter();
        return context;
    }

    static void interopCallExit(JSContext context) {
        context.interopBoundaryExit();
    }

    @Resolve(message = "EXECUTE")
    abstract static class ExecuteNode extends Node {

        private final ConditionProfile rejected = ConditionProfile.createBinaryProfile();

        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createExecute();
        @Child private ExportValueNode export = ExportValueNode.create();

        public Object access(VirtualFrame frame, DynamicObject target, Object[] args) {
            if (JSRuntime.isCallable(target)) {
                Object result = null;
                JSContext context = interopCallEnter(target);
                boolean asyncFunction = isAsyncFunction(target);
                try {
                    result = export.executeWithTarget(callNode.executeInterop(frame, target, args), Undefined.instance);
                } finally {
                    interopCallExit(context);
                }
                if (asyncFunction && result != null && JSPromise.isJSPromise(result)) {
                    /*
                     * InteropCompletePromises semantics: interop calls to async functions return
                     * the async resolved value (if any). If the promise resolves, its value is made
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
            } else {
                throw UnsupportedTypeException.raise(new Object[]{target});
            }
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

        @Child private JSInteropExecuteAfterReadNode callNode;
        @Child private ExportValueNode export = ExportValueNode.create();

        public Object access(VirtualFrame frame, DynamicObject target, @SuppressWarnings("unused") String id, Object[] args) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(new JSInteropExecuteAfterReadNode(args.length, JSObject.getJSContext(target)));
            }
            JSContext context = interopCallEnter(target);
            try {
                return export.executeWithTarget(callNode.execute(frame), ForeignAccess.getReceiver(frame));
            } finally {
                interopCallExit(context);
            }
        }
    }

    @Resolve(message = "NEW")
    abstract static class NewNode extends Node {

        @Child private ExportValueNode export = ExportValueNode.create();
        @Child private JSInteropExecuteNode callNode = JSInteropExecuteNode.createNew();

        public Object access(VirtualFrame frame, DynamicObject target, Object[] args) {
            JSContext context = interopCallEnter(target);
            try {
                return export.executeWithTarget(callNode.executeInterop(frame, target, args), Undefined.instance);
            } finally {
                interopCallExit(context);
            }
        }
    }

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {

        @Child private ReadElementNode readNode;
        @Child private ExportValueNode export = ExportValueNode.create();

        public Object access(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject target, Object key) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ReadElementNode.create(JSObject.getJSContext(target)));
            }
            return export.executeWithTarget(readNode.executeWithTargetAndIndex(target, key), target);
        }

    }

    @Resolve(message = "WRITE")
    abstract static class WriteNode extends Node {

        @Child private JavaScriptNode writeNode;
        @Child private ExportValueNode export = ExportValueNode.create();

        @SuppressWarnings("unused")
        public Object access(VirtualFrame frame, DynamicObject target, Object key, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(JSInteropWrite.create(JSObject.getJSContext(target), Message.WRITE));
            }
            return export.executeWithTarget(writeNode.execute(frame), target);
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

        @TruffleBoundary // due to toArray()
        public Object access(DynamicObject target, @SuppressWarnings("unused") boolean internal) {
            Object[] keys = JSObject.enumerableOwnNames(target).toArray();
            JSContext context = JSObject.getJSContext(target);
            return JSArray.createConstant(context, keys);
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoNode extends Node {

        @Child protected JSToPropertyKeyNode toKey = JSToPropertyKeyNode.create();
        @Child protected JSForeignToJSTypeNode cast = JSForeignToJSTypeNode.create();

        @TruffleBoundary
        public Object access(DynamicObject target, Object key) {
            PropertyDescriptor desc = JSObject.getOwnProperty(target, toKey.execute(cast.executeWithTarget(key)));
            if (desc == null) {
                return 0;
            }
            boolean readable = true;
            boolean writable = desc.getIfHasWritable(true);
            boolean invocable = desc.isDataDescriptor() & JSRuntime.isCallable(desc.getValue());

            return KeyInfo.newBuilder().setInternal(false).setInvocable(invocable).setWritable(writable).setReadable(readable).build();
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

        @Child private IsJSObjectNode isJSObjectNode = IsJSObjectNode.create();

        protected boolean test(TruffleObject receiver) {
            return isJSObjectNode.executeBoolean(receiver);
        }
    }
}
