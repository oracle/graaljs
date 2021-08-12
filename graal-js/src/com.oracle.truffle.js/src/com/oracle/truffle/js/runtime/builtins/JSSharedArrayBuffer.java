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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.SharedArrayBufferFunctionBuiltins;
import com.oracle.truffle.js.builtins.SharedArrayBufferPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSSharedArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithFunctionsAndSpecies, PrototypeSupplier {

    public static final String CLASS_NAME = "SharedArrayBuffer";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    public static final JSSharedArrayBuffer INSTANCE = new JSSharedArrayBuffer();

    private JSSharedArrayBuffer() {
    }

    public static DynamicObject createSharedArrayBuffer(JSContext context, JSRealm realm, int length) {
        return createSharedArrayBuffer(context, realm, DirectByteBufferHelper.allocateDirect(length));
    }

    public static DynamicObject createSharedArrayBuffer(JSContext context, JSRealm realm, ByteBuffer buffer) {
        assert buffer != null;
        JSObjectFactory factory = context.getSharedArrayBufferFactory();
        DynamicObject obj = JSArrayBufferObject.createSharedArrayBuffer(factory.getShape(realm), buffer, new JSAgentWaiterList());
        factory.initProto(obj, realm);
        assert isJSSharedArrayBuffer(obj);
        return context.trackAllocation(obj);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject arrayBufferPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        putConstructorProperty(context, arrayBufferPrototype, ctor);
        putFunctionsFromContainer(realm, arrayBufferPrototype, SharedArrayBufferPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putBuiltinAccessorProperty(arrayBufferPrototype, BYTE_LENGTH, realm.lookupAccessor(SharedArrayBufferPrototypeBuiltins.BUILTINS, BYTE_LENGTH));
        JSObjectUtil.putToStringTag(arrayBufferPrototype, CLASS_NAME);
        return arrayBufferPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, SharedArrayBufferFunctionBuiltins.BUILTINS);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    public static boolean isJSSharedArrayBuffer(Object obj) {
        return obj instanceof JSArrayBufferObject.Shared;
    }

    public static ByteBuffer getDirectByteBuffer(DynamicObject thisObj) {
        assert isJSSharedArrayBuffer(thisObj);
        return JSArrayBufferObject.getDirectByteBuffer(thisObj);
    }

    public static JSAgentWaiterList getWaiterList(DynamicObject thisObj) {
        assert isJSSharedArrayBuffer(thisObj);
        return JSArrayBufferObject.getWaiterList(thisObj);
    }

    public static void setWaiterList(DynamicObject thisObj, JSAgentWaiterList wl) {
        assert isJSSharedArrayBuffer(thisObj);
        JSArrayBufferObject.setWaiterList(thisObj, wl);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSharedArrayBufferPrototype();
    }
}
