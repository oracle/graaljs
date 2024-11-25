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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltinsFactory.DataViewGetNodeGen;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltinsFactory.DataViewGetterNodeGen;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltinsFactory.DataViewSetNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class DataViewPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<DataViewPrototypeBuiltins.DataViewPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new DataViewPrototypeBuiltins();

    protected DataViewPrototypeBuiltins() {
        super(JSDataView.PROTOTYPE_NAME, DataViewPrototype.class);
    }

    public enum DataViewPrototype implements BuiltinEnum<DataViewPrototype> {
        getBigInt64(1),
        getBigUint64(1),
        getFloat32(1),
        getFloat64(1),
        getInt8(1),
        getInt16(1),
        getInt32(1),
        getUint8(1),
        getUint16(1),
        getUint32(1),
        setBigInt64(2),
        setBigUint64(2),
        setFloat32(2),
        setFloat64(2),
        setInt8(2),
        setInt16(2),
        setInt32(2),
        setUint8(2),
        setUint16(2),
        setUint32(2),

        // getters
        buffer(0),
        byteLength(0),
        byteOffset(0);

        private final int length;

        DataViewPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(buffer, byteLength, byteOffset).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DataViewPrototype builtinEnum) {
        switch (builtinEnum) {
            case getBigInt64:
            case getBigUint64:
            case getFloat32:
            case getFloat64:
            case getInt16:
            case getInt32:
            case getInt8:
            case getUint16:
            case getUint32:
            case getUint8:
                return DataViewGetNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case setBigInt64:
            case setBigUint64:
            case setFloat32:
            case setFloat64:
            case setInt16:
            case setInt32:
            case setInt8:
            case setUint16:
            case setUint32:
            case setUint8:
                return DataViewSetNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case buffer:
            case byteLength:
            case byteOffset:
                return DataViewGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSDataView.class})
    public abstract static class DataViewAccessNode extends JSBuiltinNode {
        protected final TypedArrayFactory factory;
        @Child private InteropLibrary interopLibrary;

        public DataViewAccessNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            // string doesn't escape
            this.factory = typedArrayFactoryFromType(Strings.lazySubstring(builtin.getName(), 3));
        }

        private static TypedArrayFactory typedArrayFactoryFromType(TruffleString type) {
            for (TypedArrayFactory factory : TypedArray.factories()) {
                if (Strings.startsWith(factory.getName(), type)) {
                    return factory;
                }
            }
            throw new IllegalArgumentException(Strings.toJavaString(type));
        }

        protected final InteropLibrary getInterop() {
            InteropLibrary lib = interopLibrary;
            if (lib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopLibrary = lib = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            return lib;
        }

        protected final void ensureNotDetached(JSArrayBufferObject buffer, InlinedBranchProfile errorBranch) {
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid()) {
                if (JSArrayBuffer.isDetachedBuffer(buffer)) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorDetachedBuffer();
                }
            }
        }

        protected final int getBufferIndex(JSDataViewObject dataView, long getIndex, InlinedBranchProfile errorBranch) {
            int viewLength = JSDataView.typedArrayGetLength(dataView);
            int elementSize = factory.getBytesPerElement();
            if (getIndex + elementSize > viewLength) {
                errorBranch.enter(this);
                throw Errors.createRangeError("index + elementSize > viewLength");
            }
            int viewOffset = JSDataView.typedArrayGetOffset(dataView);

            assert getIndex + viewOffset <= Integer.MAX_VALUE;
            int bufferIndex = (int) (getIndex + viewOffset);
            return bufferIndex;
        }
    }

    public abstract static class DataViewGetNode extends DataViewAccessNode {

        public DataViewGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final Object doDataView(JSDataViewObject dataView, Object byteOffset, Object littleEndian,
                        @Cached JSToIndexNode toIndexNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedExactClassProfile bufferTypeProfile,
                        @Cached InlinedExactClassProfile arrayTypeProfile) {
            long getIndex = toIndexNode.executeLong(byteOffset);
            boolean isLittleEndian = factory.getBytesPerElement() == 1 || toBooleanNode.executeBoolean(this, littleEndian);

            JSArrayBufferObject buffer = bufferTypeProfile.profile(this, JSDataView.getArrayBuffer(dataView));
            ensureNotDetached(buffer, errorBranch);
            int bufferIndex = getBufferIndex(dataView, getIndex, errorBranch);

            boolean isInteropBuffer = JSArrayBuffer.isJSInteropArrayBuffer(buffer);
            TypedArray strategy = arrayTypeProfile.profile(this, factory.createArrayType(JSArrayBuffer.isJSDirectOrSharedArrayBuffer(buffer), true, isInteropBuffer));
            return strategy.getBufferElement(buffer, bufferIndex, isLittleEndian, isInteropBuffer ? getInterop() : null);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSDataView(thisObj)")
        protected JSDynamicObject doIncompatibleReceiver(Object thisObj, Object byteOffset, Object littleEndian) {
            throw Errors.createTypeErrorNotADataView();
        }
    }

    public abstract static class DataViewSetNode extends DataViewAccessNode {
        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToDoubleNode toDoubleNode;
        @Child private JSToInt32Node toInt32Node;

        public DataViewSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            if (factory.isBigInt()) {
                this.toBigIntNode = JSToBigIntNode.create();
            } else if (factory.isFloat()) {
                this.toDoubleNode = JSToDoubleNode.create();
            } else {
                this.toInt32Node = JSToInt32Node.create();
            }
        }

        @Specialization
        protected Object doDataView(JSDataViewObject dataView, Object byteOffset, Object value, Object littleEndian,
                        @Cached JSToIndexNode toIndexNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedExactClassProfile bufferTypeProfile,
                        @Cached InlinedExactClassProfile arrayTypeProfile) {
            long getIndex = toIndexNode.executeLong(byteOffset);
            Object numberValue = switch (factory) {
                case BigInt64Array, BigUint64Array -> toBigIntNode.executeBigInteger(value);
                case Float64Array, Float32Array -> toDoubleNode.executeDouble(value);
                default -> toInt32Node.executeInt(value);
            };
            boolean isLittleEndian = factory.getBytesPerElement() == 1 || toBooleanNode.executeBoolean(this, littleEndian);

            JSArrayBufferObject buffer = bufferTypeProfile.profile(this, JSDataView.getArrayBuffer(dataView));
            ensureNotDetached(buffer, errorBranch);
            int bufferIndex = getBufferIndex(dataView, getIndex, errorBranch);

            boolean isInteropBuffer = JSArrayBuffer.isJSInteropArrayBuffer(buffer);
            TypedArray strategy = arrayTypeProfile.profile(this, factory.createArrayType(JSArrayBuffer.isJSDirectOrSharedArrayBuffer(buffer), true, isInteropBuffer));
            strategy.setBufferElement(buffer, bufferIndex, isLittleEndian, numberValue, isInteropBuffer ? getInterop() : null);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSDataView(thisObj)")
        protected JSDynamicObject doIncompatibleReceiver(Object thisObj, Object byteOffset, Object value, Object littleEndian) {
            throw Errors.createTypeErrorNotADataView();
        }
    }

    @ImportStatic({JSDataView.class})
    public abstract static class DataViewGetterNode extends JSBuiltinNode {

        private final DataViewPrototype getter;

        public DataViewGetterNode(JSContext context, JSBuiltin builtin, DataViewPrototype getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected final Object doDataView(JSDataViewObject dataView,
                        @Cached InlinedBranchProfile errorBranch) {
            JSArrayBufferObject arrayBuffer = JSDataView.getArrayBuffer(dataView);
            switch (getter) {
                case buffer:
                    return arrayBuffer;
                case byteLength:
                    if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                        errorBranch.enter(this);
                        throw Errors.createTypeErrorDetachedBuffer();
                    }
                    return JSDataView.typedArrayGetLength(dataView);
                case byteOffset:
                    if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                        errorBranch.enter(this);
                        throw Errors.createTypeErrorDetachedBuffer();
                    }
                    return JSDataView.typedArrayGetOffset(dataView);
                default:
                    throw Errors.shouldNotReachHere();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSDataView(thisObj)")
        protected Object doIncompatibleReceiver(Object thisObj) {
            throw Errors.createTypeErrorNotADataView();
        }
    }
}
