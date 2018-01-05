/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.*;

import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.array.*;
import com.oracle.truffle.js.runtime.objects.*;

public final class JSArgumentsObject extends JSAbstractArgumentsObject {
    static final JSArgumentsObject INSTANCE = new JSArgumentsObject();

    private JSArgumentsObject() {
    }

    public static DynamicObject createStrict(JSRealm realm, Object[] elements) {
        // (array, arrayType, length, usedLength, indexOffset, arrayOffset, holeCount, length)
        return JSObject.create(realm.getContext(), realm.getStrictArgumentsFactory(), elements, ScriptArray.createConstantArray(elements), null, elements.length, 0, 0, 0, 0, elements.length,
                        elements.length,
                        realm.getArrayProtoValuesIterator());
    }

    public static DynamicObject createNonStrict(JSRealm realm, Object[] elements, DynamicObject callee) {
        // (array, arrayType, len, usedLen, indexOffset, arrayOffset, holeCount, length, callee)
        return JSObject.create(realm.getContext(), realm.getNonStrictArgumentsFactory(), elements, ScriptArray.createConstantArray(elements), null, elements.length, 0, 0, 0, 0, elements.length,
                        elements.length,
                        realm.getArrayProtoValuesIterator(), callee);
    }

    public static Shape makeInitialNonStrictArgumentsShape(JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject objectPrototype = realm.getObjectPrototype();
        DynamicObject dummyArray = JSObject.create(realm, objectPrototype, INSTANCE);

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

    public static Shape makeInitialStrictArgumentsShape(JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject dummyArray = JSObject.create(realm, realm.getObjectPrototype(), INSTANCE);

        putArrayProperties(dummyArray, ScriptArray.createConstantEmptyArray());

        JSObjectUtil.putHiddenProperty(dummyArray, CONNECTED_ARGUMENT_COUNT_PROPERTY, 0);

        Property lengthProperty = JSObjectUtil.makeDataProperty(LENGTH, dummyArray.getShape().allocator().locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)),
                        JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, dummyArray, lengthProperty, 0);

        putIteratorProperty(context, dummyArray);

        DynamicObject throwerFn = realm.getThrowerFunction();
        JSObjectUtil.putConstantAccessorProperty(context, dummyArray, CALLEE, throwerFn, throwerFn, JSAttributes.notConfigurableNotEnumerable());
        if (context.getEcmaScriptVersion() < JSTruffleOptions.ECMAScript2017) {
            JSObjectUtil.putConstantAccessorProperty(context, dummyArray, CALLER, throwerFn, throwerFn, JSAttributes.notConfigurableNotEnumerable());
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
