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
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetIndexOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetIndexOffset;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public abstract class AbstractContiguousJSObjectArray extends AbstractJSObjectArray {

    protected AbstractContiguousJSObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public DynamicObject getInBoundsFastJSObject(DynamicObject object, int index, boolean condition) {
        return castNonNull(getArray(object, condition)[(int) (index - getIndexOffset(object, condition))]);
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, DynamicObject value, boolean condition) {
        getArray(object, condition)[(int) (index - getIndexOffset(object, condition))] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected final void setLengthLess(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        setLengthLessContiguous(object, length, condition, profile);
    }

    @Override
    protected final int prepareInBoundsFast(DynamicObject object, long index, boolean condition) {
        return (int) (index - getIndexOffset(object, condition));
    }

    @Override
    protected final void setArrayOffset(DynamicObject object, int arrayOffset) {
        arraySetArrayOffset(object, arrayOffset);
    }

    @Override
    protected final int getArrayOffset(DynamicObject object) {
        return arrayGetArrayOffset(object);
    }

    @Override
    protected final int getArrayOffset(DynamicObject object, boolean condition) {
        return arrayGetArrayOffset(object, condition);
    }

    @Override
    protected final void setIndexOffset(DynamicObject object, long indexOffset) {
        arraySetIndexOffset(object, indexOffset);
    }

    @Override
    protected final long getIndexOffset(DynamicObject object) {
        return arrayGetIndexOffset(object);
    }

    @Override
    protected final long getIndexOffset(DynamicObject object, boolean condition) {
        return arrayGetIndexOffset(object, condition);
    }

    @Override
    public final long firstElementIndex(DynamicObject object, boolean condition) {
        return getIndexOffset(object, condition) + getArrayOffset(object, condition);
    }

    @Override
    public final long lastElementIndex(DynamicObject object, boolean condition) {
        return getIndexOffset(object, condition) + getArrayOffset(object, condition) + getUsedLength(object, condition) - 1;
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return true;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        return addRangeImplContiguous(object, offset, size);
    }
}
