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

import java.util.function.Function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSDataView extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final String CLASS_NAME = "DataView";
    public static final String PROTOTYPE_NAME = "DataView.prototype";

    public static final JSDataView INSTANCE = new JSDataView();

    private static final String BYTE_LENGTH = "byteLength";
    private static final String BUFFER = "buffer";
    private static final String BYTE_OFFSET = "byteOffset";

    public static int typedArrayGetLength(Object thisObj) {
        assert JSDataView.isJSDataView(thisObj);
        return JSDataViewObject.getLength(thisObj);
    }

    public static int typedArrayGetOffset(Object thisObj) {
        assert JSDataView.isJSDataView(thisObj);
        return JSDataViewObject.getOffset(thisObj);
    }

    private JSDataView() {
    }

    public static DynamicObject getArrayBuffer(Object thisObj) {
        assert JSDataView.isJSDataView(thisObj);
        return JSDataViewObject.getArrayBuffer(thisObj);
    }

    public static DynamicObject createDataView(JSContext context, JSRealm realm, DynamicObject arrayBuffer, int offset, int length) {
        assert offset >= 0 && offset + length <= ((JSArrayBufferObject) arrayBuffer).getByteLength();

        JSObjectFactory factory = context.getDataViewFactory();
        DynamicObject dataView = JSDataViewObject.create(realm, factory, (JSArrayBufferObject) arrayBuffer, length, offset);
        assert JSArrayBuffer.isJSAbstractBuffer(arrayBuffer);
        assert isJSDataView(dataView);
        return context.trackAllocation(dataView);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        putGetters(realm, prototype);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, DataViewPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    private static void putGetters(JSRealm realm, DynamicObject prototype) {
        putGetter(realm, prototype, BUFFER, BuiltinFunctionKey.DataViewBuffer, view -> getArrayBuffer(view));
        putGetter(realm, prototype, BYTE_LENGTH, BuiltinFunctionKey.DataViewByteLength, view -> typedArrayGetLengthChecked(view));
        putGetter(realm, prototype, BYTE_OFFSET, BuiltinFunctionKey.DataViewByteOffset, view -> typedArrayGetOffsetChecked(view));
    }

    public static int typedArrayGetLengthChecked(DynamicObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(JSDataView.getArrayBuffer(thisObj))) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return typedArrayGetLength(thisObj);
    }

    public static int typedArrayGetOffsetChecked(DynamicObject thisObj) {
        if (JSArrayBuffer.isDetachedBuffer(JSDataView.getArrayBuffer(thisObj))) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return typedArrayGetOffset(thisObj);
    }

    private static void putGetter(JSRealm realm, DynamicObject prototype, String name, BuiltinFunctionKey key, Function<DynamicObject, Object> function) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(key, (c) -> {
            return JSFunctionData.createCallOnly(c, new JavaScriptRootNode(c.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = JSArguments.getThisObject(frame.getArguments());
                    if (isJSDataView(obj)) {
                        return function.apply((JSDataViewObject) obj);
                    }
                    throw Errors.createTypeErrorNotADataView();
                }
            }.getCallTarget(), 0, "get " + name);
        });
        DynamicObject getter = JSFunction.create(realm, getterData);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, name, getter, Undefined.instance);
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape childTree = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return childTree;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    public static boolean isJSDataView(Object obj) {
        return obj instanceof JSDataViewObject;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDataViewPrototype();
    }
}
