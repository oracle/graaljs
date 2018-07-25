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
import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putHiddenProperty;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
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
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public final class JSArrayBuffer extends JSAbstractBuffer implements JSConstructorFactory.Default.WithFunctionsAndSpecies {

    public static final String CLASS_NAME = "ArrayBuffer";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    private static final JSArrayBuffer HEAP_INSTANCE = new JSArrayBuffer();
    private static final JSArrayBuffer DIRECT_INSTANCE = new JSArrayBuffer();

    private JSArrayBuffer() {
    }

    public static DynamicObject createArrayBuffer(JSContext context, int length) {
        return createArrayBuffer(context, new byte[length]);
    }

    public static DynamicObject createArrayBuffer(JSContext context, byte[] byteArray) {
        DynamicObject obj = JSObject.create(context, context.getArrayBufferFactory(), byteArray);
        assert isJSHeapArrayBuffer(obj);
        return obj;
    }

    public static int getDirectByteLength(DynamicObject thisObj) {
        return getDirectByteBuffer(thisObj).capacity();
    }

    public static ByteBuffer getDirectByteBuffer(DynamicObject thisObj) {
        return getDirectByteBuffer(thisObj, JSArrayBuffer.isJSDirectArrayBuffer(thisObj));
    }

    public static ByteBuffer getDirectByteBuffer(DynamicObject thisObj, boolean condition) {
        assert isJSDirectArrayBuffer(thisObj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return DirectByteBufferHelper.cast((ByteBuffer) BYTE_BUFFER_PROPERTY.get(thisObj, condition));
    }

    public static DynamicObject createDirectArrayBuffer(JSContext context, int length) {
        return createDirectArrayBuffer(context, DirectByteBufferHelper.allocateDirect(length));
    }

    public static DynamicObject createDirectArrayBuffer(JSContext context, ByteBuffer buffer) {
        DynamicObject obj = JSObject.create(context, context.getDirectArrayBufferFactory(), buffer);
        assert isJSDirectArrayBuffer(obj);
        return obj;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject arrayBufferPrototype = JSObject.create(realm, realm.getObjectPrototype(), context.getEcmaScriptVersion() < 6 ? HEAP_INSTANCE : JSUserObject.INSTANCE);
        if (context.getEcmaScriptVersion() < 6) {
            putHiddenProperty(arrayBufferPrototype, BYTE_ARRAY_PROPERTY, new byte[0]);
        }
        putConstructorProperty(context, arrayBufferPrototype, ctor);
        putFunctionsFromContainer(realm, arrayBufferPrototype, PROTOTYPE_NAME);
        DynamicObject byteLengthGetter = JSFunction.create(realm, JSFunctionData.createCallOnly(context, createByteLengthGetterCallTarget(context), 0, "get " + BYTE_LENGTH));
        JSObjectUtil.putConstantAccessorProperty(context, arrayBufferPrototype, BYTE_LENGTH, byteLengthGetter, Undefined.instance);
        JSObjectUtil.putDataProperty(context, arrayBufferPrototype, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        return arrayBufferPrototype;
    }

    private static CallTarget createByteLengthGetterCallTarget(JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final ConditionProfile isArrayBuffer = ConditionProfile.createBinaryProfile();
            private final ConditionProfile isDirectByteBuffer = ConditionProfile.createBinaryProfile();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (JSObject.isDynamicObject(obj)) {
                    DynamicObject buffer = (DynamicObject) obj;
                    if (isArrayBuffer.profile(isJSHeapArrayBuffer(buffer))) {
                        if (!context.getTypedArrayNotDetachedAssumption().isValid() && isDetachedBuffer(buffer)) {
                            throw Errors.createTypeErrorDetachedBuffer();
                        }
                        return getByteLength(buffer);
                    } else if (isDirectByteBuffer.profile(isJSDirectArrayBuffer(buffer))) {
                        if (!context.getTypedArrayNotDetachedAssumption().isValid() && isDetachedBuffer(buffer)) {
                            throw Errors.createTypeErrorDetachedBuffer();
                        }
                        return getDirectByteLength(buffer);
                    }
                }
                throw Errors.createTypeErrorArrayBufferExpected();
            }
        });
    }

    public static Shape makeInitialArrayBufferShape(JSContext context, DynamicObject prototype, boolean direct) {
        if (!direct) {
            assert JSShape.getProtoChildTree(prototype.getShape(), HEAP_INSTANCE) == null;
            Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, HEAP_INSTANCE, context);
            initialShape = initialShape.addProperty(BYTE_ARRAY_PROPERTY);
            return initialShape;
        } else {
            assert JSShape.getProtoChildTree(prototype.getShape(), DIRECT_INSTANCE) == null;
            Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, DIRECT_INSTANCE, context);
            initialShape = initialShape.addProperty(BYTE_BUFFER_PROPERTY);
            return initialShape;
        }
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return HEAP_INSTANCE.createConstructorAndPrototype(realm);
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
        return JSObject.isDynamicObject(obj) && isJSHeapArrayBuffer((DynamicObject) obj);
    }

    public static boolean isJSHeapArrayBuffer(DynamicObject obj) {
        return isInstance(obj, HEAP_INSTANCE);
    }

    public static boolean isJSDirectArrayBuffer(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSDirectArrayBuffer((DynamicObject) obj);
    }

    public static boolean isJSDirectArrayBuffer(DynamicObject obj) {
        return isInstance(obj, DIRECT_INSTANCE);
    }

    public static boolean isJSDirectOrSharedArrayBuffer(Object obj) {
        return isJSDirectArrayBuffer(obj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(obj);
    }

    public static boolean isJSDirectOrSharedArrayBuffer(DynamicObject obj) {
        return isJSDirectArrayBuffer(obj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(obj);
    }

    /**
     * ES2015, 24.1.1.2 IsDetachedBuffer.
     *
     * Warning: This is a slow method! Use the assumption provided in
     * getContext().getTypedArrayNotDetachedAssumption() for better performance.
     */
    @TruffleBoundary
    public static boolean isDetachedBuffer(DynamicObject arrayBuffer) {
        assert isJSAbstractBuffer(arrayBuffer);
        if (isJSDirectArrayBuffer(arrayBuffer)) {
            return BYTE_BUFFER_PROPERTY.get(arrayBuffer, isJSDirectArrayBuffer(arrayBuffer)) == null;
        } else {
            return BYTE_ARRAY_PROPERTY.get(arrayBuffer, isJSHeapArrayBuffer(arrayBuffer)) == null;
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
            BYTE_BUFFER_PROPERTY.setSafe(arrayBuffer, null, null);
        } else {
            BYTE_ARRAY_PROPERTY.setSafe(arrayBuffer, null, null);
        }
    }
}
