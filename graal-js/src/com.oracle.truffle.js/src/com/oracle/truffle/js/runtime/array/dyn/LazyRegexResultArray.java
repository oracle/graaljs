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
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResult;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResultOriginalInput;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleString.SubstringByteIndexNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InvokeGetGroupBoundariesMethodNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResult;

public final class LazyRegexResultArray extends AbstractConstantLazyArray {

    public static final LazyRegexResultArray LAZY_REGEX_RESULT_ARRAY = new LazyRegexResultArray(INTEGRITY_LEVEL_NONE, createCache()).maybePreinitializeCache();

    public static LazyRegexResultArray createLazyRegexResultArray() {
        return LAZY_REGEX_RESULT_ARRAY;
    }

    private LazyRegexResultArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    private static Object[] getArray(JSDynamicObject object) {
        return (Object[]) arrayGetArray(object);
    }

    public static Object materializeGroup(JSContext context, JSDynamicObject object, int index,
                    DynamicObjectLibrary lazyRegexResultNode, DynamicObjectLibrary lazyRegexResultOriginalInputNode,
                    Node node, SubstringByteIndexNode substringNode, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        Object[] internalArray = getArray(object);
        if (internalArray[index] == null) {
            Object regexResult = arrayGetRegexResult(object, lazyRegexResultNode);
            TruffleString originalInputString = arrayGetRegexResultOriginalInput(object, lazyRegexResultOriginalInputNode);
            internalArray[index] = TRegexMaterializeResult.materializeGroup(context, regexResult, index, originalInputString,
                            node, substringNode, getStartNode, getEndNode);
        }
        return internalArray[index];
    }

    public ScriptArray createWritable(JSContext context, JSDynamicObject object, long index, Object value,
                    DynamicObjectLibrary lazyRegexResultNode, DynamicObjectLibrary lazyRegexResultOriginalInputNode,
                    Node node, SubstringByteIndexNode substringNode, InvokeGetGroupBoundariesMethodNode getStartNode, InvokeGetGroupBoundariesMethodNode getEndNode) {
        for (int i = 0; i < lengthInt(object); i++) {
            materializeGroup(context, object, i, lazyRegexResultNode, lazyRegexResultOriginalInputNode, node,
                            substringNode, getStartNode, getEndNode);
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
        final Object[] internalArray = getArray(object);
        if (internalArray[index] == null) {
            Object regexResult = arrayGetRegexResult(object, DynamicObjectLibrary.getUncached());
            TruffleString originalInputString = arrayGetRegexResultOriginalInput(object, DynamicObjectLibrary.getUncached());
            internalArray[index] = TRegexMaterializeResult.materializeGroupUncached(regexResult, index, originalInputString);
        }
        return internalArray[index];
    }

    @Override
    public AbstractObjectArray createWriteableObject(JSDynamicObject object, long index, Object value, Node node, CreateWritableProfileAccess profile) {
        Object regexResult = arrayGetRegexResult(object, DynamicObjectLibrary.getUncached());
        int length = lengthInt(object);
        TruffleString originalInputString = arrayGetRegexResultOriginalInput(object, DynamicObjectLibrary.getUncached());
        Object[] array = TRegexMaterializeResult.materializeFullUncached(regexResult, length, originalInputString);
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
        return new LazyRegexResultArray(newIntegrityLevel, cache);
    }
}
