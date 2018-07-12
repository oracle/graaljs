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

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putConstructorProperty;
import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putFunctionsFromContainer;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
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
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSSharedArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final String CLASS_NAME = "SharedArrayBuffer";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    private static final JSSharedArrayBuffer INSTANCE = new JSSharedArrayBuffer();

    protected static final Property BUFFER_WAIT_LIST;
    protected static final HiddenKey BUFFER_WAIT_LIST_ID = new HiddenKey("waitList");

    static {
        BUFFER_WAIT_LIST = JSObjectUtil.makeHiddenProperty(BUFFER_WAIT_LIST_ID, allocator.locationForType(JSAgentWaiterList.class));
    }

    private JSSharedArrayBuffer() {
    }

    public static DynamicObject createSharedArrayBuffer(JSContext context, int length) {
        return createSharedArrayBuffer(context, DirectByteBufferHelper.allocateDirect(length));
    }

    public static DynamicObject createSharedArrayBuffer(JSContext context, ByteBuffer buffer) {
        DynamicObject obj = JSObject.create(context, context.getSharedArrayBufferFactory(), buffer, new JSAgentWaiterList());
        assert isJSSharedArrayBuffer(obj);
        return obj;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject arrayBufferPrototype = JSObject.create(realm, realm.getObjectPrototype(), INSTANCE);
        putConstructorProperty(context, arrayBufferPrototype, ctor);
        putFunctionsFromContainer(realm, arrayBufferPrototype, PROTOTYPE_NAME);
        /* ECMA2017 24.2.4.1 get SharedArrayBuffer.prototype.byteLength */
        DynamicObject byteLengthGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, createByteLengthGetterCallTarget(context), 0, "get " + BYTE_LENGTH));
        JSObjectUtil.putDataProperty(context, arrayBufferPrototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putConstantAccessorProperty(context, arrayBufferPrototype, BYTE_LENGTH, byteLengthGetter, Undefined.instance);
        return arrayBufferPrototype;
    }

    private static CallTarget createByteLengthGetterCallTarget(JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject buffer = (DynamicObject) obj;
                    if (hasArrayBufferData(buffer) && isJSSharedArrayBuffer(buffer)) {
                        return JSArrayBuffer.getDirectByteLength(buffer);
                    }
                }
                throw Errors.createTypeErrorIncompatibleReceiver(obj);
            }
        });
    }

    public static Shape makeInitialArrayBufferShape(JSContext context, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        initialShape = initialShape.addProperty(BYTE_BUFFER_PROPERTY);
        initialShape = initialShape.addProperty(BUFFER_WAIT_LIST);
        return initialShape;
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

    public static boolean isJSSharedArrayBuffer(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSharedArrayBuffer((DynamicObject) obj);
    }

    public static boolean isJSSharedArrayBuffer(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    public static boolean hasArrayBufferData(DynamicObject obj) {
        Object maybeBuffer = BYTE_BUFFER_PROPERTY.get(obj, isJSSharedArrayBuffer(obj));
        return maybeBuffer != null && maybeBuffer instanceof ByteBuffer;
    }

    public static ByteBuffer getDirectByteBuffer(DynamicObject thisObj) {
        return getDirectByteBuffer(thisObj, isJSSharedArrayBuffer(thisObj));
    }

    public static ByteBuffer getDirectByteBuffer(DynamicObject thisObj, boolean condition) {
        assert JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return DirectByteBufferHelper.cast((ByteBuffer) BYTE_BUFFER_PROPERTY.get(thisObj, condition));
    }

    public static JSAgentWaiterList getWaiterList(DynamicObject thisObj) {
        return (JSAgentWaiterList) BUFFER_WAIT_LIST.get(thisObj, JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj));
    }

    public static JSAgentWaiterList getWaiterList(DynamicObject thisObj, boolean condition) {
        assert JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return (JSAgentWaiterList) BUFFER_WAIT_LIST.get(thisObj, condition);
    }

}
