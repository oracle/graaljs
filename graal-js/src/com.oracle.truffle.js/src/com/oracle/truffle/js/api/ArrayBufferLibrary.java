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
package com.oracle.truffle.js.api;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;

/**
 * ArrayBuffer library provides operations on and information about JavaScript {@code ArrayBuffer}
 * objects.
 */
@GenerateLibrary
public abstract class ArrayBufferLibrary extends Library {
    private static final LibraryFactory<ArrayBufferLibrary> FACTORY = LibraryFactory.resolve(ArrayBufferLibrary.class);

    /**
     * Returns the library factory for the ArrayBuffer library.
     */
    public static LibraryFactory<ArrayBufferLibrary> getFactory() {
        return FACTORY;
    }

    /**
     * Returns the uncached automatically dispatched version of the ArrayBuffer library.
     */
    public static ArrayBufferLibrary getUncached() {
        return FACTORY.getUncached();
    }

    /**
     * Returns {@code true} if the {@code object} is JavaScript {@code ArrayBuffer}, returns
     * {@code false} otherwise.
     */
    public boolean isArrayBuffer(Object object) {
        return object instanceof JSArrayBufferObject;
    }

    /**
     * Returns the content of the {@code arrayBuffer}. Throws {@code UnsupportedMessageException}
     * when the receiver is not {@link #isArrayBuffer(Object)}. Returns {@code null} when
     * {@code arrayBuffer} is detached. May also return {@code null} when {@code arrayBuffer} wraps
     * non-JavaScript buffer.
     */
    public ByteBuffer getContents(@SuppressWarnings("unused") Object arrayBuffer) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    /**
     * Returns the byte length of the {@code arrayBuffer}. Throws
     * {@code UnsupportedMessageException} when the receiver is not {@link #isArrayBuffer(Object)}.
     */
    public int getByteLength(@SuppressWarnings("unused") Object arrayBuffer) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

}
