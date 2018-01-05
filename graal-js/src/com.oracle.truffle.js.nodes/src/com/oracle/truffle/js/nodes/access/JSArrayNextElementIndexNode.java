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
 * Provides the functionality of ScriptArray.nextElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */
public abstract class JSArrayNextElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayNextElementIndexNode(JSContext context) {
        super(context);
    }

    public static JSArrayNextElementIndexNode create(JSContext context) {
        return JSArrayNextElementIndexNodeGen.create(context);
    }

    public final long executeLong(TruffleObject object, long currentIndex, long length) {
        return executeLong(object, currentIndex, length, isArray(object));
    }

    public abstract long executeLong(TruffleObject object, long currentIndex, long length, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object, isArray) == cachedArrayType", "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(DynamicObject object, long currentIndex, @SuppressWarnings("unused") long length, boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object, isArray);
        return cachedArrayType.nextElementIndex(object, currentIndex, isArray);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(DynamicObject object, long currentIndex, @SuppressWarnings("unused") long length, boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object, isArray).nextElementIndex(object, currentIndex, isArray);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"})
    public long nextWithHoles(DynamicObject object, long currentIndex, long length, boolean isArray,
                    @Cached("create(context)") JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isPlusOne,
                    @Cached("createClassProfile()") ValueProfile arrayType) {
        assert isSupportedArray(object);
        long nextIndex = arrayType.profile(getArrayType(object, isArray)).nextElementIndex(object, currentIndex, isArray);
        long plusOne = currentIndex + 1;
        if (isPlusOne.profile(nextIndex == plusOne)) {
            return nextIndex;
        }

        if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
            DynamicObject prototype = JSObject.getPrototype(object);
            while (prototype != Null.instance) {
                long candidate = nextElementIndexNode.executeLong(prototype, currentIndex, length);
                if (plusOne <= candidate && candidate < length) {
                    nextIndex = Math.min(nextIndex, candidate);
                }
                prototype = JSObject.getPrototype(prototype);
            }
        }
        return nextIndex;
    }

    @Specialization(guards = {"!isArray", "!isArraySuitableForEnumBasedProcessing(object, length)"})
    public long nextObjectViaPolling(TruffleObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long index = currentIndex + 1;
        while (!hasPropertyNode.executeBoolean(object, index)) {
            index++;
            if (index >= length) {
                return JSRuntime.MAX_SAFE_INTEGER_LONG;
            }
        }
        return index;
    }

    @Specialization(guards = {"!isArray", "isArraySuitableForEnumBasedProcessing(object, length)"})
    public long nextObjectViaEnumeration(DynamicObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long currentIndexPlusOne = currentIndex + 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexPlusOne)) {
            return currentIndexPlusOne;
        }
        return nextObjectViaEnumerationIntl(object, currentIndex, length);
    }

    @TruffleBoundary
    private static long nextObjectViaEnumerationIntl(DynamicObject object, long currentIndex, long length) {
        long result = length == 0 ? 1 : length; // not found
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof String) {
                long candidate = JSRuntime.propertyNameToIntegerIndex((String) key);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate > currentIndex && candidate < result) {
                    result = candidate;
                }
            }
        }
        return result;
    }
}
