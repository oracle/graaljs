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
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.function.Function;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSDataView extends JSBuiltinObject implements JSConstructorFactory.Default {

    public static final String CLASS_NAME = "DataView";
    public static final String PROTOTYPE_NAME = "DataView.prototype";

    private static final JSDataView INSTANCE = new JSDataView();

    private static final String BYTE_LENGTH = "byteLength";
    private static final String BUFFER = "buffer";
    private static final String BYTE_OFFSET = "byteOffset";
    private static final HiddenKey ARRAY_BUFFER_ID = new HiddenKey("arrayBuffer");
    private static final HiddenKey OFFSET_ID = new HiddenKey("offset");
    private static final HiddenKey LENGTH_ID = new HiddenKey(JSAbstractArray.LENGTH);
    private static final Property ARRAY_BUFFER_PROPERTY;
    private static final Property ARRAY_LENGTH_PROPERTY;
    private static final Property ARRAY_OFFSET_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        ARRAY_BUFFER_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_BUFFER_ID, allocator.locationForType(JSObject.CLASS, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
        ARRAY_LENGTH_PROPERTY = JSObjectUtil.makeHiddenProperty(LENGTH_ID, allocator.locationForType(int.class));
        ARRAY_OFFSET_PROPERTY = JSObjectUtil.makeHiddenProperty(OFFSET_ID, allocator.locationForType(int.class));
    }

    public static int typedArrayGetLength(DynamicObject thisObj) {
        return (int) ARRAY_LENGTH_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static int typedArrayGetLength(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_LENGTH_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetLength(DynamicObject thisObj, int length) {
        ARRAY_LENGTH_PROPERTY.setSafe(thisObj, length, null);
    }

    public static int typedArrayGetOffset(DynamicObject thisObj) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static int typedArrayGetOffset(DynamicObject thisObj, boolean condition) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, condition);
    }

    public static void typedArraySetOffset(DynamicObject thisObj, int arrayOffset) {
        ARRAY_OFFSET_PROPERTY.setSafe(thisObj, arrayOffset, null);
    }

    private JSDataView() {
    }

    public static DynamicObject getArrayBuffer(DynamicObject thisObj) {
        assert JSDataView.isJSDataView(thisObj);
        return (DynamicObject) ARRAY_BUFFER_PROPERTY.get(thisObj, JSDataView.isJSDataView(thisObj));
    }

    public static DynamicObject createDataView(JSContext context, DynamicObject arrayBuffer, int offset, int length) {
        assert offset >= 0 && offset + length <= (JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer) ? JSArrayBuffer.getDirectByteLength(arrayBuffer) : JSArrayBuffer.getByteLength(arrayBuffer));

        // (arrayBuffer, length, offset)
        DynamicObject dataView = JSObject.create(context, context.getDataViewFactory(), arrayBuffer, length, offset);
        assert JSArrayBuffer.isJSHeapArrayBuffer(arrayBuffer) || JSArrayBuffer.isJSDirectOrSharedArrayBuffer(arrayBuffer);
        assert isJSDataView(dataView);
        return dataView;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        putGetters(realm, prototype);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(context, prototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return prototype;
    }

    private static void putGetters(JSRealm realm, DynamicObject prototype) {
        putGetter(realm, prototype, BUFFER, view -> getArrayBuffer(view));
        putGetter(realm, prototype, BYTE_LENGTH, view -> typedArrayGetLengthChecked(view));
        putGetter(realm, prototype, BYTE_OFFSET, view -> typedArrayGetOffsetChecked(view));
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

    private static void putGetter(JSRealm realm, DynamicObject prototype, String name, Function<DynamicObject, Object> function) {
        JSContext context = realm.getContext();
        DynamicObject getter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile notDataViewBranch = BranchProfile.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject dataView = JSObject.castJSObject(obj);
                    if (isJSDataView(dataView)) {
                        return function.apply(dataView);
                    }
                }
                notDataViewBranch.enter();
                throw Errors.createTypeErrorNotADataView().setRealm(realm);
            }
        }), 0, "get " + name));
        JSObjectUtil.putConstantAccessorProperty(context, prototype, name, getter, Undefined.instance);
    }

    public static Shape makeInitialArrayBufferViewShape(JSContext ctx, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape childTree = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        // hidden properties
        childTree = childTree.addProperty(ARRAY_BUFFER_PROPERTY);
        childTree = childTree.addProperty(ARRAY_LENGTH_PROPERTY);
        childTree = childTree.addProperty(ARRAY_OFFSET_PROPERTY);
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
        return JSObject.isDynamicObject(obj) && isJSDataView((DynamicObject) obj);
    }

    public static boolean isJSDataView(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }
}
