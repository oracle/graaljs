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

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object, isArray) == cachedArrayType", "cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long previousWithHolesCached(DynamicObject object, long currentIndex, boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType,
                    @Cached("create(context)") JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isMinusOne) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object, isArray);
        return holesArrayImpl(object, currentIndex, isArray, cachedArrayType, previousElementIndexNode, isMinusOne);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"}, replaces = "previousWithHolesCached")
    public long previousWithHolesUncached(DynamicObject object, long currentIndex, boolean isArray,
                    @Cached("create(context)") JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isMinusOne,
                    @Cached("createClassProfile()") ValueProfile arrayTypeProfile) {
        assert isSupportedArray(object);
        ScriptArray arrayType = arrayTypeProfile.profile(getArrayType(object, isArray));
        return holesArrayImpl(object, currentIndex, isArray, arrayType, previousElementIndexNode, isMinusOne);
    }

    private long holesArrayImpl(DynamicObject object, long currentIndex, boolean isArray, ScriptArray array,
                    JSArrayPreviousElementIndexNode previousElementIndexNode, ConditionProfile isMinusOne) {
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
