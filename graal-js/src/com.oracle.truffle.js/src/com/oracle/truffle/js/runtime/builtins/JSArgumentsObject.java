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
package com.oracle.truffle.js.runtime.builtins;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
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
import com.oracle.truffle.js.nodes.interop.ArrayElementInfoNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.interop.InteropArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public class JSArgumentsObject extends JSArrayBase {

    protected JSArgumentsObject(Shape shape, JSDynamicObject proto, DynamicArray arrayType, Object array, int length) {
        super(shape, proto, arrayType, array, null, length, 0, 0, 0, 0);
    }

    public static final class Unmapped extends JSArgumentsObject {

        protected Unmapped(Shape shape, JSDynamicObject proto, DynamicArray arrayType, Object array, int length) {
            super(shape, proto, arrayType, array, length);
        }
    }

    public static final class Mapped extends JSArgumentsObject {

        protected Mapped(Shape shape, JSDynamicObject proto, DynamicArray arrayType, Object array, int length) {
            super(shape, proto, arrayType, array, length);
            this.connectedArgumentCount = length;
        }

        protected int connectedArgumentCount;
        protected Map<Long, Object> disconnectedIndices;

        public int getConnectedArgumentCount() {
            return connectedArgumentCount;
        }

        public Map<Long, Object> getDisconnectedIndices() {
            assert JSAbstractArgumentsArray.hasDisconnectedIndices(this);
            return disconnectedIndices;
        }

        @TruffleBoundary
        public void initDisconnectedIndices() {
            assert JSAbstractArgumentsArray.hasDisconnectedIndices(this);
            this.disconnectedIndices = new HashMap<>();
        }
    }

    @Override
    public final TruffleString getClassName() {
        return getBuiltinToStringTag();
    }

    @Override
    public final TruffleString getBuiltinToStringTag() {
        return JSArgumentsArray.CLASS_NAME;
    }

    @ExportMessage
    public final Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this") InteropLibrary self) {
        assert JSObject.getJSClass(this) == JSArgumentsArray.INSTANCE;
        boolean includeArrayIndices = language(self).getJSContext().getLanguageOptions().arrayElementsAmongMembers();
        if (includeArrayIndices) {
            return InteropArray.create(JSObject.enumerableOwnNames(this));
        } else {
            return InteropArray.create(filterEnumerableNames(this, JSObject.ownPropertyKeys(this), JSArgumentsArray.INSTANCE));
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public final long getArraySize() {
        return JSRuntime.toInteger(JSObject.get(this, JSAbstractArray.LENGTH));
    }

    @ExportMessage
    public final Object readArrayElement(long index,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached(value = "create(language(self).getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached ExportValueNode exportNode) throws InvalidArrayIndexException, UnsupportedMessageException {
        if (index < 0 || index >= self.getArraySize(this)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object result;
        if (readNode == null) {
            result = JSObject.getOrDefault(this, index, this, Undefined.instance);
        } else {
            result = readNode.executeWithTargetAndIndexOrDefault(this, index, Undefined.instance);
        }
        return exportNode.execute(result);
    }

    @ExportMessage
    public final boolean isArrayElementReadable(long index,
                    @CachedLibrary("this") InteropLibrary thisLibrary) {
        try {
            return index >= 0 && index < thisLibrary.getArraySize(this);
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public final void writeArrayElement(long index, Object value,
                    @Shared @Cached ArrayElementInfoNode elements,
                    @Cached ImportValueNode castValueNode,
                    @Cached(value = "createCachedInterop()", uncached = "getUncachedWrite()") WriteElementNode writeNode) throws InvalidArrayIndexException, UnsupportedMessageException {
        elements.executeCheck(this, index, ArrayElementInfoNode.WRITABLE);
        Object importedValue = castValueNode.executeWithTarget(value);
        if (writeNode == null) {
            JSObject.set(this, index, importedValue, true, null);
        } else {
            writeNode.executeWithTargetAndIndexAndValue(this, index, importedValue);
        }
    }

    @ExportMessage
    public final boolean isArrayElementModifiable(long index,
                    @Shared @Cached ArrayElementInfoNode elements) {
        return elements.executeBoolean(this, index, ArrayElementInfoNode.MODIFIABLE);
    }

    @ExportMessage
    public final boolean isArrayElementInsertable(long index,
                    @Shared @Cached ArrayElementInfoNode elements) {
        return elements.executeBoolean(this, index, ArrayElementInfoNode.INSERTABLE);
    }
}
