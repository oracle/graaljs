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
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.SharedArrayBufferPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSSharedArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithSpecies, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("SharedArrayBuffer");
    public static final TruffleString PROTOTYPE_NAME = Strings.concat(CLASS_NAME, Strings.DOT_PROTOTYPE);

    public static final JSSharedArrayBuffer INSTANCE = new JSSharedArrayBuffer();

    private JSSharedArrayBuffer() {
    }

    public static JSArrayBufferObject createSharedArrayBuffer(JSContext context, JSRealm realm, JSDynamicObject proto, int byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getSharedArrayBufferFactory();
        return createSharedArrayBuffer(realm, proto, DirectByteBufferHelper.allocateDirect(Math.max(byteLength, maxByteLength)), factory, new AtomicInteger(byteLength), maxByteLength);
    }

    public static JSArrayBufferObject createSharedArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer) {
        JSObjectFactory factory = context.getSharedArrayBufferFactory();
        return createSharedArrayBuffer(realm, factory.getPrototype(realm), buffer, factory, new AtomicInteger(buffer.capacity()), JSArrayBuffer.FIXED_LENGTH);
    }

    public static JSArrayBufferObject createSharedArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer, AtomicInteger byteLength, int maxByteLength) {
        JSObjectFactory factory = context.getSharedArrayBufferFactory();
        return createSharedArrayBuffer(realm, factory.getPrototype(realm), buffer, factory, byteLength, maxByteLength);
    }

    private static JSArrayBufferObject createSharedArrayBuffer(JSRealm realm, JSDynamicObject proto, ByteBuffer buffer, JSObjectFactory factory, AtomicInteger byteLength, int maxByteLength) {
        assert buffer != null;
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSArrayBufferObject.Shared(shape, proto, buffer, new JSAgentWaiterList(), byteLength, maxByteLength), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject arrayBufferPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(arrayBufferPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, arrayBufferPrototype, SharedArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, arrayBufferPrototype, SharedArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(arrayBufferPrototype, CLASS_NAME);
        return arrayBufferPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static boolean isJSSharedArrayBuffer(Object obj) {
        return obj instanceof JSArrayBufferObject.Shared;
    }

    public static ByteBuffer getDirectByteBuffer(JSDynamicObject thisObj) {
        assert isJSSharedArrayBuffer(thisObj);
        return JSArrayBufferObject.getDirectByteBuffer(thisObj);
    }

    public static JSAgentWaiterList getWaiterList(JSDynamicObject thisObj) {
        return JSArrayBufferObject.getWaiterList(thisObj);
    }

    public static void setWaiterList(JSDynamicObject thisObj, JSAgentWaiterList wl) {
        JSArrayBufferObject.setWaiterList(thisObj, wl);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSharedArrayBufferPrototype();
    }
}
