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
package com.oracle.truffle.js.nodes.array;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Provides the functionality of ScriptArray.lastElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */

public abstract class JSArrayLastElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayLastElementIndexNode(JSContext context) {
        super(context);
    }

    public static JSArrayLastElementIndexNode create(JSContext context) {
        return JSArrayLastElementIndexNodeGen.create(context);
    }

    public final long executeLong(Object object, long length) {
        return executeLong(object, length, isArray(object));
    }

    public abstract long executeLong(Object object, long length, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(DynamicObject object, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return cachedArrayType.lastElementIndex(object);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(DynamicObject object, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object).lastElementIndex(object);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithHolesCached(DynamicObject object, long length, boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType,
                    @Cached("create(context)") JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isLengthMinusOne) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return holesArrayImpl(object, length, cachedArrayType, previousElementIndexNode, isLengthMinusOne, isArray);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"}, replaces = "doWithHolesCached")
    public long doWithHolesUncached(DynamicObject object, long length, boolean isArray,
                    @Cached("create(context)") JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile isLengthMinusOne,
                    @Cached("createClassProfile()") ValueProfile arrayTypeProfile) {
        assert isSupportedArray(object);
        ScriptArray arrayType = arrayTypeProfile.profile(getArrayType(object));
        return holesArrayImpl(object, length, arrayType, previousElementIndexNode, isLengthMinusOne, isArray);
    }

    private long holesArrayImpl(DynamicObject object, long length, ScriptArray array,
                    JSArrayPreviousElementIndexNode previousElementIndexNode, ConditionProfile isLengthMinusOne, @SuppressWarnings("unused") boolean isArray) {
        long lastIndex = array.lastElementIndex(object);
        if (isLengthMinusOne.profile(lastIndex == length - 1)) {
            return lastIndex;
        }

        // object itself might have larger indices in the shape
        DynamicObject prototype = object;
        while (prototype != Null.instance) {
            long candidate = previousElementIndexNode.executeLong(prototype, length);
            lastIndex = Math.max(lastIndex, candidate);
            if (lastIndex >= (length - 1)) {
                return length - 1;
            }
            if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
                prototype = JSObject.getPrototype(prototype);
            } else {
                break;
            }
        }

        // prototype could have larger last index, but that would not count!
        return lastIndex;
    }

    @Specialization(guards = {"!isArray", "isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)"})
    public long doObjectViaEnumeration(DynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long lengthMinusOne = length - 1;
        if (hasPropertyNode.executeBoolean(object, lengthMinusOne)) {
            return lengthMinusOne;
        }

        return doObjectViaEnumerationIntl(object, lengthMinusOne);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)", "isSuitableForEnumBasedProcessing(object, length)"})
    public long doObjectViaFullEnumeration(DynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long lengthMinusOne = length - 1;
        if (hasPropertyNode.executeBoolean(object, lengthMinusOne)) {
            return lengthMinusOne;
        }

        return doObjectViaFullEnumerationIntl(object, lengthMinusOne);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessing(object, length)"})
    public long doObject(Object object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create()") JSHasPropertyNode hasPropertyNode) {
        long index = length - 1;
        while (!hasPropertyNode.executeBoolean(object, index) && index > 0) {
            index--;
        }
        return index;
    }

    @TruffleBoundary
    private static long doObjectViaEnumerationIntl(DynamicObject object, long lengthMinusOne) {
        long result = -1;
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof String) {
                long candidate = JSRuntime.propertyNameToIntegerIndex((String) key);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate < lengthMinusOne && candidate > result) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    private static long doObjectViaFullEnumerationIntl(DynamicObject object, long length) {
        long result = -1;
        DynamicObject chainObject = object;
        do {
            result = Math.max(result, doObjectViaEnumerationIntl(chainObject, length));
            chainObject = JSObject.getPrototype(chainObject);
        } while (chainObject != Null.instance);
        return result;
    }

}
