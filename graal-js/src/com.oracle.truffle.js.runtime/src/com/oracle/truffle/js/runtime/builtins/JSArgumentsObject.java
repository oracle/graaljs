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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSArgumentsObject extends JSAbstractArgumentsObject {
    public static final JSArgumentsObject INSTANCE = new JSArgumentsObject();

    private JSArgumentsObject() {
    }

    public static DynamicObject createStrict(JSContext context, JSRealm realm, Object[] elements) {
        // (array, arrayType, length, usedLength, indexOffset, arrayOffset, holeCount, length)
        if (context.getEcmaScriptVersion() < JSTruffleOptions.ECMAScript2017) {
            return JSObject.createWithPrototype(context, context.getStrictArgumentsFactory(), realm, realm.getObjectPrototype(),
                            elements, ScriptArray.createConstantArray(elements), null, elements.length, 0, 0, 0, 0, elements.length, elements.length, realm.getArrayProtoValuesIterator(),
                            realm.getThrowerAccessor(), realm.getThrowerAccessor());
        }
        return JSObject.createWithPrototype(context, context.getStrictArgumentsFactory(), realm, realm.getObjectPrototype(),
                        elements, ScriptArray.createConstantArray(elements), null, elements.length, 0, 0, 0, 0, elements.length, elements.length, realm.getArrayProtoValuesIterator(),
                        realm.getThrowerAccessor());
    }

    public static DynamicObject createNonStrict(JSContext context, JSRealm realm, Object[] elements, DynamicObject callee) {
        // (array, arrayType, len, usedLen, indexOffset, arrayOffset, holeCount, length, callee)
        return JSObject.createWithPrototype(context, context.getNonStrictArgumentsFactory(), realm, realm.getObjectPrototype(),
                        elements, ScriptArray.createConstantArray(elements), null, elements.length, 0, 0, 0, 0, elements.length, elements.length, realm.getArrayProtoValuesIterator(), callee);
    }

    public static Shape makeInitialNonStrictArgumentsShape(JSContext context, DynamicObject objectPrototype) {
        DynamicObject dummyArray = JSObject.create(context, objectPrototype, INSTANCE);

        putArrayProperties(dummyArray, ScriptArray.createConstantEmptyArray());

        JSObjectUtil.putHiddenProperty(dummyArray, CONNECTED_ARGUMENT_COUNT_PROPERTY, 0);

        // force these to non-final to avoid obsolescence of initial shape (same below).
        // (GR-2051) make final and do not obsolete initial shape or allow obsolescence
        Property lengthProperty = JSObjectUtil.makeDataProperty(LENGTH, dummyArray.getShape().allocator().locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)),
                        JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, dummyArray, lengthProperty, 0);

        putIteratorProperty(context, dummyArray);

        Property calleeProperty = JSObjectUtil.makeDataProperty(CALLEE, dummyArray.getShape().allocator().locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)),
                        JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, dummyArray, calleeProperty, Undefined.instance);
        return dummyArray.getShape();
    }

    public static Shape makeInitialStrictArgumentsShape(JSContext context, DynamicObject objectPrototype) {
        DynamicObject dummyArray = JSObject.create(context, objectPrototype, INSTANCE);

        putArrayProperties(dummyArray, ScriptArray.createConstantEmptyArray());

        JSObjectUtil.putHiddenProperty(dummyArray, CONNECTED_ARGUMENT_COUNT_PROPERTY, 0);

        Property lengthProperty = JSObjectUtil.makeDataProperty(LENGTH, dummyArray.getShape().allocator().locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)),
                        JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, dummyArray, lengthProperty, 0);

        putIteratorProperty(context, dummyArray);

        Accessor throwerAccessor = new Accessor(Undefined.instance, Undefined.instance);
        JSObjectUtil.putAccessorProperty(context, dummyArray, CALLEE, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        if (context.getEcmaScriptVersion() < JSTruffleOptions.ECMAScript2017) {
            JSObjectUtil.putAccessorProperty(context, dummyArray, CALLER, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        }
        return dummyArray.getShape();
    }

    public static boolean isJSArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsObject.INSTANCE);
    }

    public static boolean isJSArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsObject.INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE);
    }

    private static void putIteratorProperty(JSContext context, DynamicObject dummyArray) {
        Property iteratorProperty = JSObjectUtil.makeDataProperty(Symbol.SYMBOL_ITERATOR, dummyArray.getShape().allocator().locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)),
                        JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, dummyArray, iteratorProperty, Undefined.instance);
    }
}
