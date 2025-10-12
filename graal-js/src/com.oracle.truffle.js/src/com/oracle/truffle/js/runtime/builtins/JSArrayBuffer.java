/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ArrayBufferFunctionBuiltins;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithFunctionsAndSpecies, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("ArrayBuffer");
    public static final TruffleString PROTOTYPE_NAME = Strings.concat(CLASS_NAME, Strings.DOT_PROTOTYPE);

    public static final JSArrayBuffer HEAP_INSTANCE = new JSArrayBuffer();
    public static final JSArrayBuffer DIRECT_INSTANCE = new JSArrayBuffer();
    public static final JSArrayBuffer INTEROP_INSTANCE = new JSArrayBuffer();

    // Used for maxByteLength of fixed length buffers
    public static final int FIXED_LENGTH = -1;
    // Used for maxByteLength of immutable buffers
    public static final int IMMUTABLE_BUFFER = -2;

    private JSArrayBuffer() {
    }

    public static JSArrayBufferObject createArrayBuffer(JSContext context, JSRealm realm, int length) {
        return createArrayBuffer(context, realm, new byte[length]);
    }

    public static JSArrayBufferObject createArrayBuffer(JSContext context, JSRealm realm, byte[] byteArray) {
        return createArrayBuffer(context, realm, byteArray, byteArray.length, FIXED_LENGTH);
    }

    public static JSArrayBufferObject createArrayBuffer(JSContext context, JSRealm realm, byte[] byteArray, int byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getArrayBufferFactory();
        return createHeapArrayBuffer(factory, realm, factory.getPrototype(realm), byteArray, byteLength, maxByteLength);
    }

    public static JSArrayBufferObject createArrayBuffer(JSContext context, JSRealm realm, JSDynamicObject proto, int byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getArrayBufferFactory();
        return createHeapArrayBuffer(factory, realm, proto, new byte[Math.max(byteLength, maxByteLength)], byteLength, maxByteLength);
    }

    private static JSArrayBufferObject createHeapArrayBuffer(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, byte[] byteArray, int byteLength, int maxByteLength) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSArrayBufferObject.Heap(shape, proto, byteArray, byteLength, maxByteLength), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static byte[] getByteArray(Object thisObj) {
        assert isJSHeapArrayBuffer(thisObj);
        return JSArrayBufferObject.getByteArray(thisObj);
    }

    public static ByteBuffer getDirectByteBuffer(Object thisObj) {
        assert isJSDirectArrayBuffer(thisObj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return JSArrayBufferObject.getDirectByteBuffer(thisObj);
    }

    public static JSArrayBufferObject createDirectArrayBuffer(JSContext context, JSRealm realm, int length) {
        return createDirectArrayBuffer(context, realm, DirectByteBufferHelper.allocateDirect(length));
    }

    public static JSArrayBufferObject createDirectArrayBuffer(JSContext context, JSRealm realm, JSDynamicObject proto, int byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getDirectArrayBufferFactory();
        return createDirectArrayBuffer(factory, realm, proto, DirectByteBufferHelper.allocateDirect(Math.max(byteLength, maxByteLength)), byteLength, maxByteLength);
    }

    public static JSArrayBufferObject createDirectArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer) {
        JSObjectFactory factory = context.getDirectArrayBufferFactory();
        return createDirectArrayBuffer(factory, realm, factory.getPrototype(realm), buffer);
    }

    public static JSArrayBufferObject createDirectArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer, int byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getDirectArrayBufferFactory();
        return createDirectArrayBuffer(factory, realm, factory.getPrototype(realm), buffer, byteLength, maxByteLength);
    }

    private static JSArrayBufferObject createDirectArrayBuffer(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, ByteBuffer buffer) {
        return createDirectArrayBuffer(factory, realm, proto, buffer, buffer.capacity(), FIXED_LENGTH);
    }

    private static JSArrayBufferObject createDirectArrayBuffer(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, ByteBuffer buffer, int byteLength, int maxByteLength) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSArrayBufferObject.Direct(shape, proto, buffer, byteLength, maxByteLength), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static Object getInteropBuffer(Object thisObj) {
        assert isJSInteropArrayBuffer(thisObj);
        return JSArrayBufferObject.getInteropBuffer(thisObj);
    }

    public static JSArrayBufferObject.Interop createInteropArrayBuffer(JSContext context, JSRealm realm, Object buffer) {
        JSObjectFactory factory = context.getInteropArrayBufferFactory();
        return createInteropArrayBuffer(factory, realm, factory.getPrototype(realm), buffer);
    }

    public static JSArrayBufferObject.Interop createInteropArrayBuffer(JSContext context, JSRealm realm, JSDynamicObject proto, Object buffer) {
        JSObjectFactory factory = context.getInteropArrayBufferFactory();
        return createInteropArrayBuffer(factory, realm, proto, buffer);
    }

    private static JSArrayBufferObject.Interop createInteropArrayBuffer(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, Object buffer) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSArrayBufferObject.Interop(shape, proto, buffer), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext context = realm.getContext();
        JSObject arrayBufferPrototype;
        if (context.getEcmaScriptVersion() < 6) {
            Shape protoShape = JSShape.createPrototypeShape(context, HEAP_INSTANCE, realm.getObjectPrototype());
            arrayBufferPrototype = JSArrayBufferObject.createHeapArrayBuffer(protoShape, realm.getObjectPrototype(), new byte[0]);
            JSObjectUtil.setOrVerifyPrototype(context, arrayBufferPrototype, realm.getObjectPrototype());
        } else {
            arrayBufferPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        }

        JSObjectUtil.putConstructorProperty(arrayBufferPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, arrayBufferPrototype, ArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, arrayBufferPrototype, ArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(arrayBufferPrototype, CLASS_NAME);
        return arrayBufferPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        if (this == INTEROP_INSTANCE) {
            return JSObjectUtil.getProtoChildShape(prototype, INTEROP_INSTANCE, context);
        } else if (this == HEAP_INSTANCE) {
            return JSObjectUtil.getProtoChildShape(prototype, HEAP_INSTANCE, context);
        } else {
            assert this == DIRECT_INSTANCE;
            return JSObjectUtil.getProtoChildShape(prototype, DIRECT_INSTANCE, context);
        }
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return HEAP_INSTANCE.createConstructorAndPrototype(realm, ArrayBufferFunctionBuiltins.BUILTINS);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static boolean isJSHeapArrayBuffer(Object obj) {
        return (obj instanceof JSArrayBufferObject.Heap);
    }

    public static boolean isJSDirectArrayBuffer(Object obj) {
        return (obj instanceof JSArrayBufferObject.Direct);
    }

    public static boolean isJSInteropArrayBuffer(Object obj) {
        return (obj instanceof JSArrayBufferObject.Interop);
    }

    public static boolean isJSDirectOrSharedArrayBuffer(Object obj) {
        return isJSDirectArrayBuffer(obj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(obj);
    }

    /**
     * ES2015, 24.1.1.2 IsDetachedBuffer.
     *
     * Warning: This is a slow method! Use the assumption provided in
     * getContext().getTypedArrayNotDetachedAssumption() for better performance.
     */
    public static boolean isDetachedBuffer(JSArrayBufferObject arrayBuffer) {
        if (isJSHeapArrayBuffer(arrayBuffer)) {
            return getByteArray(arrayBuffer) == null;
        } else if (isJSDirectOrSharedArrayBuffer(arrayBuffer)) {
            return getDirectByteBuffer(arrayBuffer) == null;
        } else {
            assert isJSInteropArrayBuffer(arrayBuffer);
            return getInteropBuffer(arrayBuffer) == null;
        }
    }

    /**
     * ES2015, 24.1.1.3 DetachArrayBuffer().
     */
    @TruffleBoundary
    public static void detachArrayBuffer(JSArrayBufferObject arrayBuffer) {
        if (arrayBuffer.isImmutable()) {
            throw Errors.createTypeErrorImmutableBuffer();
        }
        JSObject.getJSContext(arrayBuffer).getTypedArrayNotDetachedAssumption().invalidate("no detached array buffer");
        if (isJSDirectArrayBuffer(arrayBuffer)) {
            ((JSArrayBufferObject.Direct) arrayBuffer).detachArrayBuffer();
        } else if (isJSInteropArrayBuffer(arrayBuffer)) {
            ((JSArrayBufferObject.Interop) arrayBuffer).detachArrayBuffer();
        } else {
            assert isJSHeapArrayBuffer(arrayBuffer);
            ((JSArrayBufferObject.Heap) arrayBuffer).detachArrayBuffer();
        }
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getArrayBufferPrototype();
    }
}
