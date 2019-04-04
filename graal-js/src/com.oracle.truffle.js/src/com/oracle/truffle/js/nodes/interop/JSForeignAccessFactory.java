/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNodeGen;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@SuppressWarnings("deprecation")
@MessageResolution(receiverType = DynamicObject.class)
public class JSForeignAccessFactory {

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

        private static final String TO_JSON_PROPERTY = "toJSON";

        @Child private ReadElementNode readNode;
        @Child private JSFunctionCallNode callToJSONFunction;

        public Object access(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject target, @SuppressWarnings("unused") Object arg) {
            if (JSDate.isJSDate(target)) {
                if (readNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    readNode = insert(ReadElementNode.create(JSObject.getJSContext(target)));
                }
                Object toJSON = readNode.executeWithTargetAndIndex(target, TO_JSON_PROPERTY);
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
