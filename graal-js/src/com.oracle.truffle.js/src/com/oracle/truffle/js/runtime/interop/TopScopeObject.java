/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;

/**
 * Object representing the top scope.
 */
@ImportStatic({JSConfig.class})
@ExportLibrary(InteropLibrary.class)
public final class TopScopeObject implements TruffleObject {

    @CompilationFinal(dimensions = 1) private static final String[] NAMES = {"scriptEngineImport", "global", "global"};

    private final Object[] objects;
    private final int scopeIndex;

    public TopScopeObject(Object[] objects) {
        this.objects = objects;
        if (objects[0] != null) {
            scopeIndex = 0;
        } else {
            scopeIndex = 1;
        }
    }

    private TopScopeObject(Object[] objects, int index) {
        this.objects = objects;
        this.scopeIndex = index;
    }

    public static TopScopeObject empty() {
        return new TopScopeObject(new Object[0], 0);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguageId() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    String getLanguageId() {
        return JavaScriptLanguage.ID;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isScope() {
        return true;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return NAMES[scopeIndex];
    }

    @ExportMessage
    boolean hasScopeParent() {
        return scopeIndex < (NAMES.length - 1);
    }

    @ExportMessage
    Object getScopeParent() throws UnsupportedMessageException {
        if (scopeIndex < (NAMES.length - 1)) {
            return new TopScopeObject(objects, scopeIndex + 1);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) throws UnsupportedMessageException {
        int length = NAMES.length;
        Object[] keys = new Object[length - scopeIndex];
        for (int i = scopeIndex; i < length; i++) {
            keys[i - scopeIndex] = interop.getMembers(objects[i]);
        }
        return new MergedPropertyNames(keys);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            if (interop.isMemberReadable(objects[i], member)) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    Object readMember(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) throws UnknownIdentifierException, UnsupportedMessageException {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberReadable(scope, member)) {
                return interop.readMember(scope, member);
            }
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    boolean isMemberModifiable(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberModifiable(scope, member)) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    boolean isMemberInsertable(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        boolean wasInsertable = false;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberExisting(scope, member)) {
                return false;
            }
            if (interop.isMemberInsertable(scope, member)) {
                wasInsertable = true;
            }
        }
        return wasInsertable;
    }

    @ExportMessage
    boolean hasMemberReadSideEffects(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberReadable(scope, member)) {
                return interop.hasMemberReadSideEffects(scope, member);
            }
        }
        return false;
    }

    @ExportMessage
    boolean hasMemberWriteSideEffects(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberWritable(scope, member)) {
                return interop.hasMemberWriteSideEffects(scope, member);
            }
        }
        return false;
    }

    @ExportMessage
    void writeMember(String member, Object value,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop)
                    throws UnknownIdentifierException, UnsupportedMessageException, UnsupportedTypeException {
        int length = NAMES.length;
        Object firstInsertableScope = null;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberExisting(scope, member)) {
                // existed therefore it cannot be insertable any more
                if (interop.isMemberModifiable(scope, member)) {
                    interop.writeMember(scope, member, value);
                    return;
                } else {
                    // we cannot modify nor insert
                    throw UnsupportedMessageException.create();
                }
            }
            if (interop.isMemberInsertable(scope, member) && firstInsertableScope == null) {
                firstInsertableScope = scope;
            }
        }

        if (firstInsertableScope != null) {
            interop.writeMember(firstInsertableScope, member, value);
            return;
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isMemberRemovable(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberRemovable(scope, member)) {
                return true;
            } else if (interop.isMemberExisting(scope, member)) {
                return false;
            }
        }
        return false;
    }

    @ExportMessage
    void removeMember(String member,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException {
        int length = NAMES.length;
        for (int i = scopeIndex; i < length; i++) {
            Object scope = this.objects[i];
            if (interop.isMemberRemovable(scope, member)) {
                interop.removeMember(scope, member);
                return;
            } else if (interop.isMemberExisting(scope, member)) {
                break;
            }
        }
        throw UnsupportedMessageException.create();
    }

    @ImportStatic({JSConfig.class})
    @ExportLibrary(InteropLibrary.class)
    static final class MergedPropertyNames implements TruffleObject {

        private final Object[] keys;
        private final long[] size;

        private MergedPropertyNames(Object[] keys) throws UnsupportedMessageException {
            this.keys = keys;
            size = new long[keys.length];
            long s = 0L;
            InteropLibrary interop = InteropLibrary.getUncached();
            for (int i = 0; i < keys.length; i++) {
                s += interop.getArraySize(keys[i]);
                size[i] = s;
            }
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return size[size.length - 1];
        }

        @ExportMessage
        boolean isArrayElementReadable(long index,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            if (index >= 0) {
                for (int i = 0; i < keys.length; i++) {
                    if (index < size[i]) {
                        long start = (i == 0) ? 0 : size[i - 1];
                        return interop.isArrayElementReadable(keys[i], index - start);
                    }
                }
            }
            return false;
        }

        @ExportMessage
        Object readArrayElement(long index,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) throws InvalidArrayIndexException, UnsupportedMessageException {
            if (index >= 0) {
                for (int i = 0; i < keys.length; i++) {
                    if (index < size[i]) {
                        long start = (i == 0) ? 0 : size[i - 1];
                        return interop.readArrayElement(keys[i], index - start);
                    }
                }
            }
            throw InvalidArrayIndexException.create(index);
        }

    }
}
