/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import java.util.HashMap;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Mockup object for JavaScript objects. Used for testing purposes, to replace the existing
 * JavaScript object types.
 *
 */
@ExportLibrary(InteropLibrary.class)
public class ForeignTestMap implements TruffleObject {

    private final HashMap<Object, Object> container = new HashMap<>();

    public HashMap<Object, Object> getContainer() {
        CompilerAsserts.neverPartOfCompilation();
        return container;
    }

    public static TruffleObject newNull() {
        ForeignTestMap obj = new ForeignTestMap();
        obj.getContainer().put("IS_NULL", true);
        return obj;
    }

    @ExportMessage
    @TruffleBoundary
    public boolean isNull() {
        return getContainer().containsKey("IS_NULL");
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    final Object readMember(String key) throws UnknownIdentifierException {
        if (getContainer().containsKey(key)) {
            return JSRuntime.nullToUndefined(getContainer().get(key));
        } else {
            throw UnknownIdentifierException.create(key);
        }
    }

    @ExportMessage
    @TruffleBoundary
    final void writeMember(String key, Object value) {
        getContainer().put(key, value);
    }

    @ExportMessage
    @TruffleBoundary
    final void removeMember(String key) throws UnknownIdentifierException {
        if (getContainer().containsKey(key)) {
            getContainer().remove(key);
        } else {
            throw UnknownIdentifierException.create(key);
        }
    }

    @ExportMessage
    @TruffleBoundary
    final Object invokeMember(String key, Object[] args) throws UnknownIdentifierException, UnsupportedTypeException, ArityException, UnsupportedMessageException {
        if (getContainer().containsKey(key)) {
            Object member = getContainer().get(key);
            InteropLibrary lib = InteropLibrary.getFactory().getUncached();
            return lib.execute(member, args);
        } else {
            throw UnknownIdentifierException.create(key);
        }
    }

    @ExportMessage
    @TruffleBoundary
    final boolean isMemberInvocable(String key) {
        if (getContainer().containsKey(key)) {
            Object member = getContainer().get(key);
            InteropLibrary lib = InteropLibrary.getFactory().getUncached();
            return lib.isExecutable(member);
        } else {
            return false;
        }
    }

    @ExportMessage(name = "isMemberReadable")
    @ExportMessage(name = "isMemberModifiable")
    @ExportMessage(name = "isMemberRemovable")
    @TruffleBoundary
    final boolean isMemberExisting(String key) {
        return getContainer().containsKey(key);
    }

    @ExportMessage
    @TruffleBoundary
    final boolean isMemberInsertable(String key) {
        return !isMemberExisting(key);
    }

    @ExportMessage
    @TruffleBoundary
    final Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this") InteropLibrary self) {
        Set<Object> keys = getContainer().keySet();
        TruffleLanguage.Env env = JSRealm.get(self).getEnv();
        return env.asGuestValue(keys.toArray(new Object[keys.size()]));
    }

    @ExportMessage
    @TruffleBoundary
    final boolean hasArrayElements() {
        return getContainer().containsKey("length");
    }

    @ExportMessage
    @TruffleBoundary
    final long getArraySize() throws UnsupportedMessageException {
        if (hasArrayElements()) {
            InteropLibrary lib = InteropLibrary.getFactory().getUncached();
            return lib.asLong(getContainer().get("length"));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    final Object readArrayElement(long index) throws UnsupportedMessageException, InvalidArrayIndexException {
        if (hasArrayElements()) {
            if (getContainer().containsKey(index)) {
                return getContainer().get(index);
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    final void writeArrayElement(long index, Object value) throws UnsupportedMessageException, InvalidArrayIndexException {
        if (hasArrayElements()) {
            if (index >= 0L && index < getArraySize()) {
                getContainer().put(index, value);
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage
    @TruffleBoundary
    final boolean isArrayElementReadable(long index) {
        return hasArrayElements() && getContainer().containsKey(index);
    }

    @ExportMessage
    @TruffleBoundary
    final boolean isArrayElementInsertable(long index) {
        return hasArrayElements() && !getContainer().containsKey(index);
    }

    @ExportMessage
    @TruffleBoundary
    final boolean isString() {
        return getContainer().get("BOX") instanceof String;
    }

    @ExportMessage
    @TruffleBoundary
    final String asString() throws UnsupportedMessageException {
        if (isString()) {
            return (String) getContainer().get("BOX");
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doSLObject(ForeignTestMap receiver, ForeignTestMap other) {
            return TriState.valueOf(receiver == other);
        }

        @SuppressWarnings("unused")
        @Fallback
        static TriState doOther(ForeignTestMap receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @TruffleBoundary
    @ExportMessage
    final int identityHashCode() {
        return System.identityHashCode(this);
    }
}
