/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putConstructorProperty;
import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putFunctionsFromContainer;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.ArrayBufferFunctionBuiltins;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithFunctionsAndSpecies, PrototypeSupplier {

    public static final String CLASS_NAME = "ArrayBuffer";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    public static final JSArrayBuffer HEAP_INSTANCE = new JSArrayBuffer();
    public static final JSArrayBuffer DIRECT_INSTANCE = new JSArrayBuffer();
    public static final JSArrayBuffer INTEROP_INSTANCE = new JSArrayBuffer();

    private JSArrayBuffer() {
    }

    public static DynamicObject createArrayBuffer(JSContext context, JSRealm realm, int length) {
        return createArrayBuffer(context, realm, new byte[length]);
    }

    public static DynamicObject createArrayBuffer(JSContext context, JSRealm realm, byte[] byteArray) {
        JSObjectFactory factory = context.getArrayBufferFactory();
        DynamicObject obj = JSArrayBufferObject.createHeapArrayBuffer(factory.getShape(realm), byteArray);
        factory.initProto(obj, realm);
        assert isJSHeapArrayBuffer(obj);
        return context.trackAllocation(obj);
    }

    public static byte[] getByteArray(Object thisObj) {
        assert isJSHeapArrayBuffer(thisObj);
        return JSArrayBufferObject.getByteArray(thisObj);
    }

    public static int getHeapByteLength(Object thisObj) {
        assert isJSHeapArrayBuffer(thisObj);
        return getByteArray(thisObj).length;
    }

    public static int getDirectByteLength(Object thisObj) {
        return getDirectByteBuffer(thisObj).capacity();
    }

    public static ByteBuffer getDirectByteBuffer(Object thisObj) {
        assert isJSDirectArrayBuffer(thisObj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return JSArrayBufferObject.getDirectByteBuffer(thisObj);
    }

    public static DynamicObject createDirectArrayBuffer(JSContext context, JSRealm realm, int length) {
        return createDirectArrayBuffer(context, realm, DirectByteBufferHelper.allocateDirect(length));
    }

    public static DynamicObject createDirectArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer) {
        JSObjectFactory factory = context.getDirectArrayBufferFactory();
        DynamicObject obj = JSArrayBufferObject.createDirectArrayBuffer(factory.getShape(realm), buffer);
        factory.initProto(obj, realm);
        assert isJSDirectArrayBuffer(obj);
        return context.trackAllocation(obj);
    }

    public static Object getInteropBuffer(Object thisObj) {
        assert isJSInteropArrayBuffer(thisObj);
        return JSArrayBufferObject.getInteropBuffer(thisObj);
    }

    public static DynamicObject createInteropArrayBuffer(JSContext context, JSRealm realm, Object buffer) {
        assert InteropLibrary.getUncached().hasBufferElements(buffer);
        JSObjectFactory factory = context.getInteropArrayBufferFactory();
        DynamicObject obj = JSArrayBufferObject.createInteropArrayBuffer(factory.getShape(realm), buffer);
        factory.initProto(obj, realm);
        assert isJSInteropArrayBuffer(obj);
        return context.trackAllocation(obj);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject arrayBufferPrototype;
        if (context.getEcmaScriptVersion() < 6) {
            Shape protoShape = JSShape.createPrototypeShape(context, HEAP_INSTANCE, realm.getObjectPrototype());
            arrayBufferPrototype = JSArrayBufferObject.createHeapArrayBuffer(protoShape, new byte[0]);
            JSObjectUtil.setOrVerifyPrototype(context, arrayBufferPrototype, realm.getObjectPrototype());
        } else {
            arrayBufferPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        }

        putConstructorProperty(context, arrayBufferPrototype, ctor);
        putFunctionsFromContainer(realm, arrayBufferPrototype, ArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putBuiltinAccessorProperty(arrayBufferPrototype, BYTE_LENGTH, realm.lookupAccessor(ArrayBufferPrototypeBuiltins.BUILTINS, BYTE_LENGTH));
        JSObjectUtil.putToStringTag(arrayBufferPrototype, CLASS_NAME);
        return arrayBufferPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
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
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
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
    @TruffleBoundary
    public static boolean isDetachedBuffer(Object arrayBuffer) {
        assert isJSAbstractBuffer(arrayBuffer);
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
    public static void detachArrayBuffer(DynamicObject arrayBuffer) {
        assert isJSAbstractBuffer(arrayBuffer);
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
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getArrayBufferPrototype();
    }
}
