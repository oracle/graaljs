/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Converts a JS array to an Object[]. If the array is sparse or has holes, it is compacted to a
 * dense array that consists of only actual elements.
 *
 * Used by {@code Array.prototype.sort}.
 */
public abstract class JSArrayToDenseObjectArrayNode extends JavaScriptBaseNode {

    protected JSArrayToDenseObjectArrayNode() {
    }

    public abstract Object[] executeObjectArray(JSArrayObject array, ScriptArray arrayType, long length);

    @Specialization(guards = {"cachedArrayType.isInstance(arrayType)", "!cachedArrayType.isHolesType()"}, limit = "5")
    protected static Object[] fromDenseArray(JSArrayObject array, @SuppressWarnings("unused") ScriptArray arrayType, long length,
                    @Cached("arrayType") @SuppressWarnings("unused") ScriptArray cachedArrayType,
                    @Cached("create(getJSContext())") @Shared ReadElementNode readNode) {
        assert JSRuntime.longIsRepresentableAsInt(length);
        long start = cachedArrayType.firstElementIndex(array);
        long end = cachedArrayType.lastElementIndex(array) + 1;
        Object[] arr = new Object[(int) (end - start)];
        int toIndex = 0;
        for (long fromIndex = start; fromIndex < end; fromIndex++) {
            assert cachedArrayType.hasElement(array, fromIndex);
            Object value = readNode.executeWithTargetAndIndex(array, fromIndex);
            arr[toIndex++] = value;
        }
        return arr;
    }

    @Specialization(guards = {"cachedArrayType.isInstance(arrayType)", "cachedArrayType.isHolesType()"}, limit = "5")
    protected static Object[] fromSparseArray(JSArrayObject array, @SuppressWarnings("unused") ScriptArray arrayType, long length,
                    @Cached("arrayType") @SuppressWarnings("unused") ScriptArray cachedArrayType,
                    @Bind("this") Node node,
                    @Cached("create(getJSContext())") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached @Shared InlinedBranchProfile growProfile) {
        long pos = cachedArrayType.firstElementIndex(array);
        SimpleArrayList<Object> list = new SimpleArrayList<>();
        while (pos <= cachedArrayType.lastElementIndex(array)) {
            assert cachedArrayType.hasElement(array, pos);
            list.add(cachedArrayType.getElement(array, pos), node, growProfile);
            pos = nextElementIndexNode.executeLong(array, pos, length);
        }
        return list.toArray(new Object[list.size()]);
    }

    @Specialization(replaces = {"fromDenseArray", "fromSparseArray"})
    protected Object[] doUncached(JSArrayObject array, ScriptArray arrayType, long length,
                    @Cached("create(getJSContext())") @Shared JSArrayNextElementIndexNode nextElementIndexNode,
                    @Cached("create(getJSContext())") @Shared ReadElementNode readNode,
                    @Cached @Shared InlinedBranchProfile growProfile) {
        if (arrayType.isHolesType()) {
            return fromSparseArray(array, arrayType, length, arrayType, this, nextElementIndexNode, growProfile);
        } else {
            return fromDenseArray(array, arrayType, length, arrayType, readNode);
        }
    }
}
