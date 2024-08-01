/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.interop.InteropArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public final class JSTypedArrayObject extends JSArrayBufferViewBase {

    TypedArray arrayType;

    protected JSTypedArrayObject(Shape shape, JSDynamicObject proto, TypedArray arrayType, JSArrayBufferObject arrayBuffer, int length, int offset) {
        super(shape, proto, arrayBuffer, length, offset);
        this.arrayType = arrayType;
    }

    @SuppressWarnings("static-method")
    public TypedArrayAccess typedArrayAccess() {
        return TypedArrayAccess.SINGLETON;
    }

    public TypedArray getArrayType() {
        return arrayType;
    }

    public static JSTypedArrayObject create(Shape shape, JSDynamicObject proto, TypedArray arrayType, JSArrayBufferObject arrayBuffer, int length, int offset) {
        return new JSTypedArrayObject(shape, proto, arrayType, arrayBuffer, length, offset);
    }

    @Override
    public TruffleString getClassName() {
        return typedArrayAccess().getTypedArrayName(this);
    }

    @Override
    public TruffleString getBuiltinToStringTag() {
        return Strings.UC_OBJECT;
    }

    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        // Do not include array indices
        assert JSObject.getJSClass(this) == JSArrayBufferView.INSTANCE;
        return InteropArray.create(filterEnumerableNames(this, JSNonProxy.ordinaryOwnPropertyKeys(this), JSArrayBufferView.INSTANCE));
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public long getArraySize() {
        return JSArrayBufferView.typedArrayGetLength(this);
    }

    @ExportMessage
    public Object readArrayElement(long index,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached(value = "create(language(self).getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached ExportValueNode exportNode) throws InvalidArrayIndexException, UnsupportedMessageException {
        var target = this;
        if (index < 0 || index >= self.getArraySize(this)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object result;
        if (readNode == null) {
            result = JSArrayBufferView.INSTANCE.getOwnHelper(target, target, index, self);
        } else {
            result = readNode.executeWithTargetAndIndexOrDefault(target, index, Undefined.instance);
        }
        return exportNode.execute(result);
    }

    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    public boolean isArrayElementReadable(long index,
                    @CachedLibrary("this") InteropLibrary thisLibrary) {
        try {
            return (index >= 0 && index < thisLibrary.getArraySize(this));
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value,
                    @Cached ImportValueNode castValueNode,
                    @Cached(value = "createCachedInterop()", uncached = "getUncachedWrite()") WriteElementNode writeNode,
                    @CachedLibrary("this") InteropLibrary self) throws InvalidArrayIndexException, UnsupportedMessageException {
        var target = this;
        if (index < 0 || index >= self.getArraySize(this)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object importedValue = castValueNode.executeWithTarget(value);
        if (writeNode == null) {
            JSArrayBufferView.INSTANCE.set(target, index, importedValue, target, true, self);
        } else {
            writeNode.executeWithTargetAndIndexAndValue(target, index, importedValue);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        // typed array elements are never insertable
        return false;
    }
}
