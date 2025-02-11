/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil.Constants;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InvokeGetGroupBoundariesMethodNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

public final class LazyRegexResultIndicesArray extends AbstractConstantLazyArray {

    public static final LazyRegexResultIndicesArray LAZY_REGEX_RESULT_INDICES_ARRAY = new LazyRegexResultIndicesArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static LazyRegexResultIndicesArray createLazyRegexResultIndicesArray() {
        return LAZY_REGEX_RESULT_INDICES_ARRAY;
    }

    protected LazyRegexResultIndicesArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    private static Object[] getArray(JSDynamicObject object) {
        return (Object[]) arrayGetArray(object);
    }

    public static Object materializeGroup(JSContext context, JSDynamicObject object, int index,
                    Node node, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        Object[] internalArray = getArray(object);
        if (internalArray[index] == null) {
            internalArray[index] = getIntIndicesArray(context, getRegexResultSlow(object), index, node, getStartNode, getEndNode);
        }
        return internalArray[index];
    }

    private static Object getRegexResultSlow(JSDynamicObject object) {
        assert JSArray.isJSArray(object) && JSArray.arrayGetArrayType(object) == LAZY_REGEX_RESULT_INDICES_ARRAY;
        return JSDynamicObject.getOrNull(object, JSRegExp.GROUPS_RESULT_ID);
    }

    public static Object getIntIndicesArray(JSContext context, Object regexResult, int index,
                    Node node, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        final int beginIndex = TRegexResultAccessor.captureGroupStart(regexResult, index, node, getStartNode);
        if (beginIndex == Constants.CAPTURE_GROUP_NO_MATCH) {
            assert index > 0;
            return Undefined.instance;
        }
        int[] intArray = new int[]{beginIndex, TRegexResultAccessor.captureGroupEnd(regexResult, index, node, getEndNode)};
        return JSArray.createConstantIntArray(context, JSRealm.get(node), intArray);
    }

    public static Object getIntIndicesArray(JSContext context, Object regexResult, int[] indices,
                    Node node, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        for (int index : indices) {
            int beginIndex = TRegexResultAccessor.captureGroupStart(regexResult, index, node, getStartNode);
            if (beginIndex != Constants.CAPTURE_GROUP_NO_MATCH) {
                int[] intArray = new int[]{beginIndex, TRegexResultAccessor.captureGroupEnd(regexResult, index, node, getEndNode)};
                return JSArray.createConstantIntArray(context, JSRealm.get(node), intArray);
            }
        }
        return Undefined.instance;
    }

    public ScriptArray createWritable(JSContext context, JSDynamicObject object, long index, Object value,
                    Node node, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        for (int i = 0; i < lengthInt(object); i++) {
            materializeGroup(context, object, i, node, getStartNode, getEndNode);
        }
        final Object[] internalArray = getArray(object);
        AbstractObjectArray newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, internalArray.length, internalArray.length, internalArray, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public Object getElementInBounds(JSDynamicObject object, int index) {
        return materializeGroup(JavaScriptLanguage.getCurrentLanguage().getJSContext(), object, index,
                        null, InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
    }

    @Override
    public AbstractObjectArray createWriteableObject(JSDynamicObject object, long index, Object value, Node node, CreateWritableProfileAccess profile) {
        Object[] array = materializeFull(object, lengthInt(object), node);
        AbstractObjectArray newArray;
        newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, array.length, array.length, array, integrityLevel);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public Object cloneArray(JSDynamicObject object) {
        return getArray(object);
    }

    @Override
    protected DynamicArray withIntegrityLevel(int newIntegrityLevel) {
        return new LazyRegexResultIndicesArray(newIntegrityLevel, cache);
    }

    protected static Object[] materializeFull(JSDynamicObject object, int groupCount, Node node) {
        Object[] result = new Object[groupCount];
        for (int i = 0; i < groupCount; ++i) {
            result[i] = materializeGroup(JavaScriptLanguage.getCurrentLanguage().getJSContext(), object, i,
                            node, InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
        }
        return result;
    }

}
