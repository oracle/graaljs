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
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetIndexOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetIndexOffset;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public abstract class AbstractContiguousJSObjectArray extends AbstractJSObjectArray {

    protected AbstractContiguousJSObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public JSDynamicObject getInBoundsFastJSObject(JSDynamicObject object, int index) {
        return castNonNull(getArray(object)[(int) (index - getIndexOffset(object))]);
    }

    @Override
    public void setInBoundsFast(JSDynamicObject object, int index, JSDynamicObject value) {
        getArray(object)[(int) (index - getIndexOffset(object))] = checkNonNull(value);
        if (JSConfig.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected final void setLengthLess(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        setLengthLessContiguous(object, length, node, profile);
    }

    @Override
    protected final int prepareInBoundsFast(JSDynamicObject object, long index) {
        return (int) (index - getIndexOffset(object));
    }

    @Override
    protected final void setArrayOffset(JSDynamicObject object, int arrayOffset) {
        arraySetArrayOffset(object, arrayOffset);
    }

    @Override
    protected final int getArrayOffset(JSDynamicObject object) {
        return arrayGetArrayOffset(object);
    }

    @Override
    protected final void setIndexOffset(JSDynamicObject object, long indexOffset) {
        arraySetIndexOffset(object, indexOffset);
    }

    @Override
    protected final long getIndexOffset(JSDynamicObject object) {
        return arrayGetIndexOffset(object);
    }

    @Override
    public final long firstElementIndex(JSDynamicObject object) {
        return getIndexOffset(object) + getArrayOffset(object);
    }

    @Override
    public final long lastElementIndex(JSDynamicObject object) {
        return getIndexOffset(object) + getArrayOffset(object) + getUsedLength(object) - 1;
    }

    @Override
    public ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size) {
        return addRangeImplContiguous(object, offset, size);
    }
}
