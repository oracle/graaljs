/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Provides the functionality of ScriptArray.firstElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */
public abstract class JSArrayFirstElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayFirstElementIndexNode(JSContext context) {
        super(context);
    }

    public static JSArrayFirstElementIndexNode create(JSContext context) {
        return JSArrayFirstElementIndexNodeGen.create(context);
    }

    public final long executeLong(TruffleObject object, long length) {
        return executeLong(object, length, isArray(object));
    }

    public abstract long executeLong(TruffleObject object, long length, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object, isArray) == cachedArrayType", "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(DynamicObject object, @SuppressWarnings("unused") long length, boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object, isArray);
        return cachedArrayType.firstElementIndex(object);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(DynamicObject object, @SuppressWarnings("unused") long length, boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object, isArray).firstElementIndex(object);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"})
    public long doWithHoles(DynamicObject object, long length, boolean isArray,
                    @Cached("create(context)") JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isZero,
                    @Cached("createClassProfile()") ValueProfile arrayType) {
        assert isSupportedArray(object);
        ScriptArray array = arrayType.profile(getArrayType(object, isArray));
        long firstIndex = array.firstElementIndex(object);
        if (isZero.profile(firstIndex == 0)) {
            return firstIndex;
        }

        // object itself might have larger indices in the shape
        DynamicObject prototype = object;
        while (prototype != Null.instance) {
            long firstProtoIndex = nextElementIndexNode.executeLong(prototype, -1, length);
            if (firstProtoIndex == 0) {
                return 0;
            }
            if (firstIndex > 0) {
                firstIndex = Math.min(firstIndex, firstProtoIndex);
            }
            if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
                prototype = JSObject.getPrototype(prototype);
            } else {
                break;
            }
        }
        return firstIndex;
    }

    @Specialization(guards = {"!isArray", "!isArraySuitableForEnumBasedProcessing(object, length)"})
    public long doObject(TruffleObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long index = 0;
        while (!hasPropertyNode.executeBoolean(object, index) && index <= (length - 1)) {
            index++;
        }
        return index;
    }

    @Specialization(guards = {"!isArray", "isArraySuitableForEnumBasedProcessing(object, length)"})
    public long firstObjectViaEnumeration(DynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        if (hasPropertyNode.executeBoolean(object, 0)) {
            return 0;
        }
        return firstObjectViaEnumerationIntl(object, length);
    }

    @TruffleBoundary
    private static long firstObjectViaEnumerationIntl(DynamicObject object, long length) {
        long result = length == 0 ? 1 : length; // not found
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof String) {
                long candidate = JSRuntime.propertyNameToIntegerIndex((String) key);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate >= 0 && candidate < result) {
                    result = candidate;
                }
            }
        }
        return result;
    }
}
