/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.util.TreeMap;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public class ArrayAccess {
    public static final ArrayAccess SINGLETON = new ArrayAccess();

    protected ArrayAccess() {
    }

    public ScriptArray getArrayType(Object thisObj) {
        return ((JSArrayBase) thisObj).arrayType;
    }

    public long getLength(Object thisObj) {
        return Integer.toUnsignedLong(((JSArrayBase) thisObj).length);
    }

    public int getUsedLength(Object thisObj) {
        return ((JSArrayBase) thisObj).usedLength;
    }

    public long getIndexOffset(Object thisObj) {
        return Integer.toUnsignedLong(((JSArrayBase) thisObj).indexOffset);
    }

    public int getArrayOffset(Object thisObj) {
        return ((JSArrayBase) thisObj).arrayOffset;
    }

    public void setArrayType(Object thisObj, ScriptArray arrayType) {
        ((JSArrayBase) thisObj).arrayType = arrayType;
    }

    public void setLength(Object thisObj, long length) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        ((JSArrayBase) thisObj).length = (int) length;
    }

    public void setUsedLength(Object thisObj, int usedLength) {
        assert usedLength >= 0;
        ((JSArrayBase) thisObj).usedLength = usedLength;
    }

    public void setIndexOffset(Object thisObj, long indexOffset) {
        assert JSRuntime.isRepresentableAsUnsignedInt(indexOffset);
        ((JSArrayBase) thisObj).indexOffset = (int) indexOffset;
    }

    public void setArrayOffset(Object thisObj, int arrayOffset) {
        assert arrayOffset >= 0;
        ((JSArrayBase) thisObj).arrayOffset = arrayOffset;
    }

    public Object getArray(Object thisObj) {
        return ((JSArrayBase) thisObj).theArray;
    }

    public void setArray(Object thisObj, Object array) {
        assert array != null && (array.getClass().isArray() || array instanceof TreeMap<?, ?>);
        ((JSArrayBase) thisObj).theArray = array;
    }

    public int getHoleCount(Object thisObj) {
        return ((JSArrayBase) thisObj).holeCount;
    }

    public void setHoleCount(Object thisObj, int holeCount) {
        assert holeCount >= 0;
        ((JSArrayBase) thisObj).holeCount = holeCount;
    }

    public ArrayAllocationSite getAllocationSite(Object thisObj) {
        return ((JSArrayBase) thisObj).allocationSite;
    }
}
