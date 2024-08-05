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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSDataView extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("DataView");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("DataView.prototype");

    public static final JSDataView INSTANCE = new JSDataView();

    private JSDataView() {
    }

    public static JSDataViewObject createDataView(JSContext context, JSRealm realm, JSArrayBufferObject arrayBuffer, int offset, int length) {
        return createDataView(context, realm, INSTANCE.getIntrinsicDefaultProto(realm), arrayBuffer, offset, length);
    }

    public static JSDataViewObject createDataView(JSContext context, JSRealm realm, JSDynamicObject proto, JSArrayBufferObject arrayBuffer, int offset, int length) {
        JSObjectFactory factory = context.getDataViewFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSDataViewObject(shape, proto, arrayBuffer, length, offset), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, DataViewPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, DataViewPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    public static int dataViewGetByteLength(JSDataViewObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(thisObj.getArrayBuffer())) {
            return 0;
        }
        return thisObj.getByteLength();
    }

    public static int dataViewGetByteOffset(JSDataViewObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(thisObj.getArrayBuffer())) {
            return 0;
        }
        return thisObj.getByteOffset();
    }

    // IsViewOutOfBounds()
    public static boolean isOutOfBounds(JSDataViewObject dataView, JSContext ctx) {
        if (!ctx.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(dataView.getArrayBuffer())) {
            return true;
        }
        if (ctx.getArrayBufferNotShrunkAssumption().isValid()) {
            return false;
        } else {
            long bufferByteLength = dataView.getArrayBuffer().getByteLength();
            int byteOffsetStart = dataView.getByteOffset();
            long byteOffsetEnd;
            if (dataView.hasAutoLength()) {
                byteOffsetEnd = bufferByteLength;
            } else {
                byteOffsetEnd = byteOffsetStart + dataView.getByteLength();
            }
            return (byteOffsetStart > bufferByteLength || byteOffsetEnd > bufferByteLength);
        }
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape childTree = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return childTree;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static boolean isJSDataView(Object obj) {
        return obj instanceof JSDataViewObject;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDataViewPrototype();
    }
}
