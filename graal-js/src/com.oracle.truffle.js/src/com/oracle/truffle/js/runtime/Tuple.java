/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.interop.JSMetaType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Implementation of the Tuple (primitive) type which is an ordered sequence of ECMAScript primitive values.
 * Since a Tuple value is completely immutable and will not change over time, you will not find a setter-method here.
 *
 * @see com.oracle.truffle.js.runtime.builtins.JSTupleObject
 */
@ValueType
public final class Tuple implements TruffleObject {

    public static final Tuple EMPTY_TUPLE = new Tuple(new Object[]{});

    private final Object[] value;

    private Tuple(Object[] v) {
        assert v != null;
        this.value = v;
    }

    public static Tuple create() {
        return EMPTY_TUPLE;
    }

    public static Tuple create(Object[] v) {
        if (v == null || v.length == 0) {
            return EMPTY_TUPLE;
        }
        return new Tuple(v);
    }

    public long getArraySize() {
        return value.length;
    }

    public int getArraySizeInt() {
        return value.length;
    }

    /**
     * @return true if the index isn't out of range.
     */
    public boolean hasElement(long index) {
        return index >= 0 && index < value.length;
    }

    /**
     * @return value at the given index.
     */
    public Object getElement(long index) {
        if (hasElement(index)) {
            return value[(int) index];
        }
        return null;
    }

    /**
     * @return all values in a List.
     */
    public Object[] getElements() {
        return value.clone();
    }

    @Override
    @TruffleBoundary
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    @TruffleBoundary
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return equals((Tuple) obj);
    }

    public boolean equals(Tuple other) {
        if (value.length != other.value.length) {
            return false;
        }
        for (int i = 0; i < value.length; i++) {
            if (!JSRuntime.isSameValueZero(value[i], other.value[i])) {
                return false;
            }
        }
        return true;
    }
}
