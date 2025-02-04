/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * Provides the functionality of ScriptArray.nextElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */
public abstract class JSArrayNextElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayNextElementIndexNode(JSContext context) {
        super(context);
    }

    @NeverDefault
    public static JSArrayNextElementIndexNode create(JSContext context) {
        return JSArrayNextElementIndexNodeGen.create(context);
    }

    public final long executeLong(Object object, long currentIndex, long length) {
        return executeLong(object, currentIndex, length, isArray(object));
    }

    public abstract long executeLong(Object object, long currentIndex, long length, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return cachedArrayType.nextElementIndex(object, currentIndex);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(JSDynamicObject object, long currentIndex, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object).nextElementIndex(object, currentIndex);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long nextWithHolesCached(JSDynamicObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType,
                    @Bind Node node,
                    @Cached("create(context)") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isPlusOne) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return holesArrayImpl(object, currentIndex, length, cachedArrayType, node, nextElementIndexNode, isPlusOne);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"}, replaces = "nextWithHolesCached")
    public long nextWithHolesUncached(JSDynamicObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create(context)") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isPlusOne,
                    @Cached InlinedExactClassProfile arrayTypeProfile) {
        assert isSupportedArray(object);
        ScriptArray arrayType = arrayTypeProfile.profile(this, getArrayType(object));
        return holesArrayImpl(object, currentIndex, length, arrayType, this, nextElementIndexNode, isPlusOne);
    }

    private long holesArrayImpl(JSDynamicObject object, long currentIndex, long length, ScriptArray array,
                    Node node, JSArrayNextElementIndexNode nextElementIndexNode, InlinedConditionProfile isPlusOne) {
        long nextIndex = array.nextElementIndex(object, currentIndex);
        long plusOne = currentIndex + 1;
        if (isPlusOne.profile(node, nextIndex == plusOne)) {
            return nextIndex;
        }

        if (!context.getArrayPrototypeNoElementsAssumption().isValid()) {
            JSDynamicObject prototype = JSObject.getPrototype(object);
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

    @Specialization(guards = {"!isArray", "isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)"})
    public long nextObjectViaEnumeration(JSDynamicObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long currentIndexPlusOne = currentIndex + 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexPlusOne)) {
            return currentIndexPlusOne;
        }
        return nextObjectViaEnumerationIntl(object, currentIndex, length);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)", "isSuitableForEnumBasedProcessing(object, length)"})
    public long nextObjectViaFullEnumeration(JSDynamicObject object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long currentIndexPlusOne = currentIndex + 1;
        if (hasPropertyNode.executeBoolean(object, currentIndexPlusOne)) {
            return currentIndexPlusOne;
        }
        return nextObjectViaFullEnumerationIntl(object, currentIndex, length);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessing(object, length)"})
    public long nextObjectViaPolling(Object object, long currentIndex, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long index = currentIndex + 1;
        while (index < length && !hasPropertyNode.executeBoolean(object, index)) {
            index++;
        }
        if (index >= length) {
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return index;
    }

    @TruffleBoundary
    private static long nextObjectViaEnumerationIntl(JSDynamicObject object, long currentIndex, long length) {
        long result = length == 0 ? 1 : length; // not found
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof TruffleString indexStr) {
                long candidate = JSRuntime.propertyNameToIntegerIndex(indexStr);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate > currentIndex && candidate < result) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    private static long nextObjectViaFullEnumerationIntl(JSDynamicObject object, long currentIndex, long length) {
        long result = Long.MAX_VALUE;
        JSDynamicObject chainObject = object;
        do {
            result = Math.min(result, nextObjectViaEnumerationIntl(chainObject, currentIndex, length));
            chainObject = JSObject.getPrototype(chainObject);
        } while (chainObject != Null.instance);
        return result;
    }

}
