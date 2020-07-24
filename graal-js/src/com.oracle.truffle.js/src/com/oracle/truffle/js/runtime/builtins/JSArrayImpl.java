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

import com.oracle.truffle.api.TruffleLanguage.LanguageReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.interop.KeyInfoNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractConstantEmptyArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.objects.JSClassObject;
import com.oracle.truffle.js.runtime.objects.JSCopyableObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropArray;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@ExportLibrary(InteropLibrary.class)
public class JSArrayImpl extends JSArrayBase implements JSCopyableObject {
    protected JSArrayImpl(Shape shape, ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        super(shape, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    public static DynamicObject create(Shape shape, ScriptArray arrayType, Object array, ArrayAllocationSite site,
                    long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        return new JSArrayImpl(shape, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    public static DynamicObject createEmpty(Shape shape, ScriptArray arrayType) {
        assert arrayType instanceof AbstractConstantEmptyArray || arrayType instanceof ConstantObjectArray || arrayType instanceof AbstractObjectArray;
        return new JSArrayImpl(shape, arrayType, ScriptArray.EMPTY_OBJECT_ARRAY, null, 0, 0, 0, 0, 0);
    }

    @Override
    public JSAbstractArray getJSClass() {
        return (JSAbstractArray) super.getJSClass();
    }

    @Override
    protected JSClassObject copyWithoutProperties(Shape shape) {
        Object clonedArray = ((DynamicArray) arrayType).cloneArray(this);
        return new JSArrayImpl(shape, arrayType, clonedArray, null, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    @SuppressWarnings("unused")
    @ImportStatic({JSGuards.class, JSObject.class})
    @ExportMessage
    public abstract static class GetMembers {
        @Specialization(guards = "isJSFastArray(target)")
        public static Object fastArray(JSArrayImpl target, boolean internal) {
            // Do not include array indices
            return InteropArray.create(filterEnumerableNames(target, JSBuiltinObject.ordinaryOwnPropertyKeys(target), JSObject.getJSClass(target)));
        }

        @Specialization(guards = {"!isJSFastArray(target)"})
        public static Object slowArray(JSArrayImpl target, boolean internal) {
            // Do not include array indices
            return InteropArray.create(filterEnumerableNames(target, JSObject.ownPropertyKeys(target), JSObject.getJSClass(target)));
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public final long getArraySize() {
        return JSArray.arrayGetLength(this);
    }

    @ExportMessage
    public final Object readArrayElement(long index,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "create(languageRef.get().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached ExportValueNode exportNode,
                    @CachedLibrary("this") InteropLibrary thisLibrary) throws InvalidArrayIndexException, UnsupportedMessageException {
        if (!hasArrayElements()) {
            throw UnsupportedMessageException.create();
        }
        DynamicObject target = this;
        if (index < 0 || index >= thisLibrary.getArraySize(target)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object result;
        if (readNode == null) {
            result = JSObject.getOrDefault(target, index, target, Undefined.instance, JSClassProfile.getUncached());
        } else {
            result = readNode.executeWithTargetAndIndexOrDefault(target, index, Undefined.instance);
        }
        return exportNode.execute(result);
    }

    @ExportMessage
    public final boolean isArrayElementReadable(long index,
                    @CachedLibrary("this") InteropLibrary thisLibrary) {
        try {
            return hasArrayElements() && (index >= 0 && index < thisLibrary.getArraySize(this));
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public final void writeArrayElement(long index, Object value,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo,
                    @Cached ImportValueNode castValueNode,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "createCachedInterop(languageRef)", uncached = "getUncachedWrite()") WriteElementNode writeNode) throws InvalidArrayIndexException, UnsupportedMessageException {
        if (!hasArrayElements() || testIntegrityLevel(true)) {
            throw UnsupportedMessageException.create();
        }
        DynamicObject target = this;
        if (!keyInfo.execute(target, index, KeyInfoNode.WRITABLE)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object importedValue = castValueNode.executeWithTarget(value);
        if (writeNode == null) {
            JSObject.set(target, index, importedValue, true);
        } else {
            writeNode.executeWithTargetAndIndexAndValue(target, index, importedValue);
        }
    }

    @ExportMessage
    public final boolean isArrayElementModifiable(long index,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return hasArrayElements() && keyInfo.execute(this, index, KeyInfoNode.MODIFIABLE);
    }

    @ExportMessage
    public final boolean isArrayElementInsertable(long index,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return hasArrayElements() && keyInfo.execute(this, index, KeyInfoNode.INSERTABLE);
    }

    @ExportMessage
    public final boolean isArrayElementRemovable(long index) {
        DynamicObject target = this;
        assert JSArray.isJSArray(target);
        ScriptArray strategy = this.arrayType;
        if (!strategy.isSealed() && !strategy.isLengthNotWritable()) {
            long len = strategy.length(target);
            if (index >= 0 && index < len) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    public final void removeArrayElement(long index) throws UnsupportedMessageException, InvalidArrayIndexException {
        DynamicObject target = this;
        assert JSArray.isJSArray(target);
        ScriptArray strategy = this.arrayType;
        if (!strategy.isSealed() && !strategy.isLengthNotWritable()) {
            long len = strategy.length(target);
            if (index >= 0 && index < len) {
                strategy = strategy.removeRange(target, index, index + 1);
                JSObject.setArray(target, strategy);
                strategy = strategy.setLength(target, len - 1, true);
                JSObject.setArray(target, strategy);
                return;
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        }
        throw UnsupportedMessageException.create();
    }
}
