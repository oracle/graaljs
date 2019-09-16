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
package com.oracle.truffle.trufflenode;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSExternalObject extends JSBuiltinObject {

    public static final String CLASS_NAME = "external";
    public static final JSExternalObject INSTANCE = new JSExternalObject();

    private static final HiddenKey POINTER_KEY = new HiddenKey("pointer");
    private static final Property POINTER_PROPERTY;

    private JSExternalObject() {
    }

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        POINTER_PROPERTY = JSObjectUtil.makeHiddenProperty(POINTER_KEY, allocator.locationForType(long.class));
    }

    public static DynamicObject create(JSContext context, long pointer) {
        ContextData contextData = GraalJSAccess.getContextEmbedderData(context);
        DynamicObject obj = contextData.getExternalObjectShape().newInstance();
        setPointer(obj, pointer);
        return obj;
    }

    public static Shape makeInitialShape(JSContext ctx) {
        Shape initialShape = ctx.getEmptyShapeNullPrototype().changeType(INSTANCE);
        initialShape = initialShape.addProperty(POINTER_PROPERTY);
        return initialShape;
    }

    public static boolean isJSExternalObject(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSExternalObject((DynamicObject) obj);
    }

    public static boolean isJSExternalObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    public static void setPointer(DynamicObject obj, long pointer) {
        assert isJSExternalObject(obj);
        POINTER_PROPERTY.setSafe(obj, pointer, null);
    }

    public static long getPointer(DynamicObject obj) {
        assert isJSExternalObject(obj);
        return (long) POINTER_PROPERTY.get(obj, isJSExternalObject(obj));
    }

}
