/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSArgumentsArray extends JSAbstractArgumentsArray {
    public static final JSArgumentsArray INSTANCE = new JSArgumentsArray();

    private JSArgumentsArray() {
    }

    public static JSArgumentsObject.Unmapped createUnmapped(Shape shape, Object[] elements) {
        return new JSArgumentsObject.Unmapped(shape, ScriptArray.createConstantArray(elements), elements, elements.length);
    }

    public static JSArgumentsObject.Mapped createMapped(Shape shape, Object[] elements) {
        return new JSArgumentsObject.Mapped(shape, ScriptArray.createConstantArray(elements), elements, elements.length);
    }

    @TruffleBoundary
    public static DynamicObject createStrictSlow(JSRealm realm, Object[] elements) {
        JSContext context = realm.getContext();
        JSObjectFactory factory = context.getStrictArgumentsFactory();
        DynamicObject argumentsObject = createUnmapped(factory.getShape(realm), elements);
        factory.initProto(argumentsObject, realm);

        JSObjectUtil.putDataProperty(context, argumentsObject, LENGTH, elements.length, JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.configurableNotEnumerableWritable());

        Accessor throwerAccessor = realm.getThrowerAccessor();
        JSObjectUtil.putBuiltinAccessorProperty(argumentsObject, CALLEE, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        if (context.getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            JSObjectUtil.putBuiltinAccessorProperty(argumentsObject, CALLER, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        }

        return context.trackAllocation(argumentsObject);
    }

    @TruffleBoundary
    public static DynamicObject createNonStrictSlow(JSRealm realm, Object[] elements, DynamicObject callee) {
        JSContext context = realm.getContext();
        JSObjectFactory factory = context.getNonStrictArgumentsFactory();
        DynamicObject argumentsObject = createMapped(factory.getShape(realm), elements);
        factory.initProto(argumentsObject, realm);

        JSObjectUtil.putDataProperty(context, argumentsObject, LENGTH, elements.length, JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.configurableNotEnumerableWritable());

        JSObjectUtil.putDataProperty(context, argumentsObject, CALLEE, callee, JSAttributes.configurableNotEnumerableWritable());
        return context.trackAllocation(argumentsObject);
    }

    public static boolean isJSArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsArray.INSTANCE);
    }

    public static boolean isJSArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsArray.INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE);
    }
}
