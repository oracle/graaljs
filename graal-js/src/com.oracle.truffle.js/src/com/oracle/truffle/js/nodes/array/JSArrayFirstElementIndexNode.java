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
 * Provides the functionality of ScriptArray.firstElementIndex but additionally adheres to the
 * prototype chain. It is implemented in a specialized and profiled fashion.
 *
 */
public abstract class JSArrayFirstElementIndexNode extends JSArrayElementIndexNode {

    protected JSArrayFirstElementIndexNode(JSContext context) {
        super(context);
    }

    @NeverDefault
    public static JSArrayFirstElementIndexNode create(JSContext context) {
        return JSArrayFirstElementIndexNodeGen.create(context);
    }

    public final long executeLong(Object object, long length) {
        return executeLong(object, length, isArray(object));
    }

    public abstract long executeLong(Object object, long length, boolean isArray);

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "!cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithoutHolesCached(JSDynamicObject object, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return cachedArrayType.firstElementIndex(object);
    }

    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "!hasHoles(object)"}, replaces = "doWithoutHolesCached")
    public long doWithoutHolesUncached(JSDynamicObject object, @SuppressWarnings("unused") long length, @SuppressWarnings("unused") boolean isArray) {
        assert isSupportedArray(object);
        return getArrayType(object).firstElementIndex(object);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isArray", "!hasPrototypeElements(object)", "getArrayType(object) == cachedArrayType",
                    "cachedArrayType.hasHoles(object)"}, limit = "MAX_CACHED_ARRAY_TYPES")
    public long doWithHolesCached(JSDynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("getArrayTypeIfArray(object, isArray)") ScriptArray cachedArrayType,
                    @Bind Node node,
                    @Cached("create(context)") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isZero) {
        assert isSupportedArray(object) && cachedArrayType == getArrayType(object);
        return holesArrayImpl(object, length, cachedArrayType, node, nextElementIndexNode, isZero);
    }

    @Specialization(guards = {"isArray", "hasPrototypeElements(object) || hasHoles(object)"}, replaces = "doWithHolesCached")
    public long doWithHolesUncached(JSDynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached("create(context)") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached @Shared InlinedConditionProfile isZero,
                    @Cached InlinedExactClassProfile arrayTypeProfile) {
        assert isSupportedArray(object);
        ScriptArray array = arrayTypeProfile.profile(this, getArrayType(object));
        return holesArrayImpl(object, length, array, this, nextElementIndexNode, isZero);
    }

    private long holesArrayImpl(JSDynamicObject object, long length, ScriptArray array,
                    Node node, JSArrayNextElementIndexNode nextElementIndexNode, InlinedConditionProfile isZero) {
        long firstIndex = array.firstElementIndex(object);
        if (isZero.profile(node, firstIndex == 0)) {
            return firstIndex;
        }

        // object itself might have larger indices in the shape
        JSDynamicObject prototype = object;
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

    @Specialization(guards = {"!isArray", "isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)"})
    public long firstObjectViaEnumeration(JSDynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        if (hasPropertyNode.executeBoolean(object, 0)) {
            return 0;
        }
        return firstObjectViaEnumerationIntl(object, length);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessingUsingOwnKeys(object, length)", "isSuitableForEnumBasedProcessing(object, length)"})
    public long firstObjectViaFullEnumeration(JSDynamicObject object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        if (hasPropertyNode.executeBoolean(object, 0)) {
            return 0;
        }
        return firstObjectViaFullEnumerationIntl(object, length);
    }

    @Specialization(guards = {"!isArray", "!isSuitableForEnumBasedProcessing(object, length)"})
    public long doObject(Object object, long length, @SuppressWarnings("unused") boolean isArray,
                    @Cached @Shared JSHasPropertyNode hasPropertyNode) {
        long index = 0;
        while (!hasPropertyNode.executeBoolean(object, index) && index <= (length - 1)) {
            index++;
        }
        return index;
    }

    @TruffleBoundary
    private static long firstObjectViaEnumerationIntl(JSDynamicObject object, long length) {
        long result = length == 0 ? 1 : length; // not found
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if (key == null) {
                continue;
            }
            if (key instanceof TruffleString indexStr) {
                long candidate = JSRuntime.propertyNameToIntegerIndex(indexStr);
                // no other length check necessary - current result is guarded by ToLength
                if (candidate >= 0 && candidate < result) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    private static long firstObjectViaFullEnumerationIntl(JSDynamicObject object, long length) {
        long result = Long.MAX_VALUE;
        JSDynamicObject chainObject = object;
        do {
            result = Math.min(result, firstObjectViaEnumerationIntl(chainObject, length));
            chainObject = JSObject.getPrototype(chainObject);
        } while (chainObject != Null.instance);
        return result;
    }

}
