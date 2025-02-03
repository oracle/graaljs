/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetLength;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * Common base class for lazy array strategies.
 */
public abstract class AbstractConstantLazyArray extends AbstractConstantArray {

    protected AbstractConstantLazyArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public final boolean hasElement(JSDynamicObject object, long index) {
        return index >= 0 && index < lengthInt(object);
    }

    @Override
    public final long length(JSDynamicObject object) {
        return lengthInt(object);
    }

    @Override
    public final int lengthInt(JSDynamicObject object) {
        return (int) arrayGetLength(object);
    }

    @Override
    public final AbstractWritableArray createWriteableDouble(JSDynamicObject object, long index, double value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public final AbstractWritableArray createWriteableInt(JSDynamicObject object, long index, int value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public final AbstractWritableArray createWriteableJSObject(JSDynamicObject object, long index, JSDynamicObject value, Node node, CreateWritableProfileAccess profile) {
        return createWriteableObject(object, index, value, node, profile);
    }

    @Override
    public final ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict) {
        return createWriteableObject(object, index, null, null, CreateWritableProfileAccess.getUncached()).deleteElementImpl(object, index, strict);
    }

    @Override
    public final ScriptArray setLengthImpl(JSDynamicObject object, long len, Node node, SetLengthProfileAccess profile) {
        return createWriteableObject(object, len - 1, null, node, profile).setLengthImpl(object, len, node, profile);
    }

    @Override
    public final ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end) {
        return createWriteableObject(object, start, null, null, CreateWritableProfileAccess.getUncached()).removeRangeImpl(object, start, end);
    }

    @Override
    public final ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size) {
        return createWriteableObject(object, offset, null, null, CreateWritableProfileAccess.getUncached()).addRangeImpl(object, offset, size);
    }
}
