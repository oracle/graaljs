/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.truffleinterop;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;

@ExportLibrary(InteropLibrary.class)
public class InteropFunction implements TruffleObject {
    final DynamicObject function;

    InteropFunction(DynamicObject function) {
        this.function = function;
    }

    public final DynamicObject getFunction() {
        return function;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    final Boolean hasMembers() {
        return true;
    }

    @ExportMessage
    final Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this.function") InteropLibrary delegate) throws UnsupportedMessageException {
        return delegate.getMembers(function);
    }

    @ExportMessage
    final boolean isMemberInvocable(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isMemberInvocable(function, key);
    }

    @ExportMessage
    final Object invokeMember(String key, Object[] arguments,
                    @CachedLibrary("this.function") InteropLibrary delegate)
                    throws UnsupportedTypeException, ArityException, UnsupportedMessageException, UnknownIdentifierException {
        return delegate.invokeMember(function, key, arguments);
    }

    @ExportMessage
    final boolean isMemberReadable(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isMemberReadable(function, key);
    }

    @ExportMessage
    final Object readMember(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) throws UnsupportedMessageException, UnknownIdentifierException {
        return delegate.readMember(function, key);
    }

    @ExportMessage
    final boolean isMemberModifiable(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isMemberModifiable(function, key);
    }

    @ExportMessage
    final boolean isMemberInsertable(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isMemberInsertable(function, key);
    }

    @ExportMessage
    final void writeMember(String key, Object value,
                    @CachedLibrary("this.function") InteropLibrary delegate) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        delegate.writeMember(function, key, value);
    }

    @ExportMessage
    final boolean isMemberRemovable(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isMemberRemovable(function, key);
    }

    @ExportMessage
    final void removeMember(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) throws UnsupportedMessageException, UnknownIdentifierException {
        delegate.removeMember(function, key);
    }

    @ExportMessage
    final boolean hasMemberReadSideEffects(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.hasMemberReadSideEffects(function, key);
    }

    @ExportMessage
    final boolean hasMemberWriteSideEffects(String key,
                    @CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.hasMemberWriteSideEffects(function, key);
    }

    @ExportMessage
    final boolean isInstantiable(@CachedLibrary("this.function") InteropLibrary delegate) {
        return delegate.isInstantiable(function);
    }

    @ExportMessage
    final Object instantiate(Object[] args,
                    @CachedLibrary("this.function") InteropLibrary delegate) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return delegate.instantiate(function, args);
    }
}
