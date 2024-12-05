/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
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

    @NeverDefault
    public static JSArrayPreviousElementIndexNode create(JSContext context) {
        return JSArrayPreviousElementIndexNodeGen.create(context);
    }

    public final long executeLong(Object object, long currentIndex) {
        return executeLong(object, currentIndex, isArray(object));
    }

    public abstract long executeLong(Object object, long currentIndex, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return cachedArrayType.previousElementIndex(object, currentIndex);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object).previousElementIndex(object, currentIndex);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long previousWithHolesCached(JSDynamicObject object, long currentIndex, boolean isArray,
                    @Bind Node node,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType,
                    @Cached("create(context)") @Shared JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isMinusOne) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return holesArrayImpl(object, currentIndex, isArray, cachedArrayType, node, previousElementIndexNode, isMinusOne);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"}, replaces = "previousWithHolesCached")
    public long previousWithHolesUncached(JSDynamicObject object, long currentIndex, boolean isArray,
                    @Cached("create(context)") @Shared JSArrayPreviousElementIndexNode previousElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isMinusOne,
                    @Cached InlinedExactClassProfile arrayTypeProfile) {
        assert isSupportedArray(object);
        ScriptArray arrayType = arrayTypeProfile.profile(this, getArrayType(object));
        return holesArrayImpl(object, currentIndex, isArray, arrayType, this, previousElementIndexNode, isMinusOne);
    }

    private long holesArrayImpl(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray, ScriptArray array,
                    Node node, JSArrayPreviousElementIndexNode previousElementIndexNode, InlinedConditionProfile isMinusOne) {
        long previousIndex = array.previousElementIndex(object, currentIndex);
        long minusOne = (currentIndex - 1);
        if (isMinusOne.profile(node, previousIndex == minusOne)) {
            return previousIndex;
        }

        if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
            JSDynamicObject prototype = JSObject.getPrototype(object);
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

    @Specialization(guards = {"!isArray", "isSuitableForEnumBasedProcessingUsingOwnKeys(object, currentIndex)"})
    public long previousObjectViaEnumeration(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long currentIndexMinusOne = currentIndex - 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexMinusOne)) {
            return currentIndexMinusOne;
        }

        return previousObjectViaEnumerationIntl(object, currentIndex);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessingUsingOwnKeys(object, currentIndex)", "isSuitableForEnumBasedProcessing(object, currentIndex)"})
    public long previousObjectViaFullEnumeration(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long currentIndexMinusOne = currentIndex - 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexMinusOne)) {
            return currentIndexMinusOne;
        }

        return previousObjectViaFullEnumerationIntl(object, currentIndex);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessing(object, currentIndex)"})
    public long previousObjectViaIteration(Object object, long currentIndex, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long index = currentIndex - 1;
        while (index >= 0 && !hasPropertyNode.executeBoolean(object, index)) {
            index--;
        }
        return index;
    }

    @TruffleBoundary
    private static long previousObjectViaEnumerationIntl(JSDynamicObject object, long currentIndex) {
        long result = -1;
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof TruffleString indexStr) {
                long candidate = JSRuntime.propertyNameToIntegerIndex(indexStr);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate < currentIndex && candidate > result) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    private static long previousObjectViaFullEnumerationIntl(JSDynamicObject object, long currentIndex) {
        long result = -1;
        JSDynamicObject chainObject = object;
        do {
            result = Math.max(result, previousObjectViaEnumerationIntl(chainObject, currentIndex));
            chainObject = JSObject.getPrototype(chainObject);
        } while (chainObject != Null.instance);
        return result;
    }

}
