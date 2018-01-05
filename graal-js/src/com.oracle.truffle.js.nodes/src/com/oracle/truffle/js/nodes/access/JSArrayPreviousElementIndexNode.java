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
 * Provides the functionality of ScriptArray.previousElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */
public abstract class JSArrayPreviousElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayPreviousElementIndexNode(JSContext context) {
        super(context);
    }

    public static JSArrayPreviousElementIndexNode create(JSContext context) {
        return JSArrayPreviousElementIndexNodeGen.create(context);
    }

    public final long executeLong(TruffleObject object, long currentIndex) {
        return executeLong(object, currentIndex, isArray(object));
    }

    public abstract long executeLong(TruffleObject object, long currentIndex, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object, isArray) == cachedArrayType", "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(DynamicObject object, long currentIndex, boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object, isArray);
        return cachedArrayType.previousElementIndex(object, currentIndex, isArray);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(DynamicObject object, long currentIndex, boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object, isArray).previousElementIndex(object, currentIndex, isArray);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"})
    public long previousWithHoles(DynamicObject object, long currentIndex, boolean isArray,
                    @Cached("create(context)") JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isMinusOne,
                    @Cached("createClassProfile()") ValueProfile arrayType) {
        assert isSupportedArray(object);
        ScriptArray array = arrayType.profile(getArrayType(object, isArray));
        long previousIndex = array.previousElementIndex(object, currentIndex, isArray);
        long minusOne = (currentIndex - 1);
        if (isMinusOne.profile(previousIndex == minusOne)) {
            return previousIndex;
        }

        if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
            DynamicObject prototype = JSObject.getPrototype(object);
            while (prototype != Null.instance) {
                long candidate = previousElementIndexNode.executeLong(prototype, currentIndex);
                if (minusOne >= candidate && candidate >= -1) {
                    previousIndex = Math.max(previousIndex, candidate);
                }
                prototype = JSObject.getPrototype(prototype);
            }
        }
        return previousIndex;
    }

    @Specialization(guards = {"!isArray", "!isArraySuitableForEnumBasedProcessing(object, currentIndex)"})
    public long previousObjectViaIteration(TruffleObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long index = currentIndex - 1;
        while (!hasPropertyNode.executeBoolean(object, index) && index > 0) {
            index--;
        }
        return index;
    }

    @Specialization(guards = {"!isArray", "isArraySuitableForEnumBasedProcessing(object, currentIndex)"})
    public long previousObjectViaEnumeration(DynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long currentIndexMinusOne = currentIndex - 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexMinusOne)) {
            return currentIndexMinusOne;
        }

        return previousObjectViaEnumerationIntl(object, currentIndex);
    }

    @TruffleBoundary
    private static long previousObjectViaEnumerationIntl(DynamicObject object, long currentIndex) {
        long result = -1;
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof String) {
                long candidate = JSRuntime.propertyNameToIntegerIndex((String) key);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate < currentIndex && candidate > result) {
                    result = candidate;
                }
            }
        }
        return result;
    }
}
