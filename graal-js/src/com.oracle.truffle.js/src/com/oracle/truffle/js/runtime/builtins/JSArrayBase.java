/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

/**
 * Base class for Array, ArgumentsObject, and %Object.prototype%.
 */
public abstract class JSArrayBase extends JSNonProxyObject {
    protected JSArrayBase(Shape shape, JSDynamicObject proto, ScriptArray arrayType,
                    Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        super(shape, proto);
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        this.length = (int) length;
        this.usedLength = usedLength;
        this.indexOffset = indexOffset;
        this.arrayOffset = arrayOffset;
        this.holeCount = holeCount;
        this.arrayStorage = Objects.requireNonNull(array);
        this.arrayStrategy = arrayType;
        this.allocationSite = site;
    }

    // Simplified schema of the meaning of the fields of this class:
    // [------------------------- an array -------------------------]
    // <-------------------------- length -------------------------->
    // <-- indexOffset --><-- arrayOffset --><-- usedLength -->
    // <-- indexOffset --><------------ arrayStorage ------------>
    int length; // interpreted as unsigned int
    int usedLength;
    int indexOffset; // can be negative (but arrayOffset >= -indexOffset when indexOffset < 0)
    int arrayOffset; // must be non-negative
    int holeCount; // number of holes in usedLength section of arrayStorage
    Object arrayStorage;
    ScriptArray arrayStrategy;
    ArrayAllocationSite allocationSite;

    @SuppressWarnings("static-method")
    public final ArrayAccess arrayAccess() {
        return ArrayAccess.SINGLETON;
    }

    public final ScriptArray getArrayType() {
        return arrayStrategy;
    }

    public final void setArrayType(ScriptArray arrayType) {
        this.arrayStrategy = arrayType;
    }

    public final Object getArray() {
        return arrayStorage;
    }

    public final void setArray(Object array) {
        this.arrayStorage = Objects.requireNonNull(array);
    }

    @Override
    public final boolean testIntegrityLevel(boolean frozen) {
        DynamicArray array = (DynamicArray) getArrayType();
        boolean arrayIs = (frozen ? array.isFrozen() : array.isSealed()) ||
                        (!array.isExtensible() && array.firstElementIndex(this) > array.lastElementIndex(this));
        return arrayIs && JSNonProxy.testIntegrityLevelFast(this, frozen);
    }

    @TruffleBoundary
    @Override
    public final boolean preventExtensions(boolean doThrow) {
        boolean result = super.preventExtensions(doThrow);
        DynamicArray arr = (DynamicArray) getArrayType();
        setArrayType(arr.preventExtensions());
        assert !isExtensible();
        return result;
    }

    @TruffleBoundary
    @Override
    public final boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        if (testIntegrityLevel(freeze)) {
            return true;
        }

        DynamicArray arr = (DynamicArray) getArrayType();
        setArrayType(freeze ? arr.freeze() : arr.seal());
        return JSNonProxy.setIntegrityLevelFast(this, freeze);
    }
}
