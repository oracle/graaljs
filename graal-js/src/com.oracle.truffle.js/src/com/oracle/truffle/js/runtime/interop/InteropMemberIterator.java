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
package com.oracle.truffle.js.runtime.interop;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class InteropMemberIterator implements TruffleObject {
    private final boolean values;
    private final long keysSize;
    private long cursor;
    final Object iteratedObject;
    final Object keysObject;

    private InteropMemberIterator(boolean values, Object iteratedObject, Object keysObject, long keysSize) {
        this.values = values;
        this.iteratedObject = iteratedObject;
        this.keysObject = keysObject;
        this.keysSize = keysSize;
    }

    public static InteropMemberIterator create(boolean values, Object iteratedObject, Object keysObject, long keysSize) {
        return new InteropMemberIterator(values, iteratedObject, keysObject, keysSize);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isIterator() {
        return true;
    }

    @ExportMessage
    boolean hasIteratorNextElement() {
        return cursor < keysSize;
    }

    @ExportMessage
    Object getIteratorNextElement(
                    @CachedLibrary("this.iteratedObject") InteropLibrary objInterop,
                    @CachedLibrary("this.keysObject") InteropLibrary keysInterop) throws StopIterationException {
        if (hasIteratorNextElement()) {
            long index = cursor++;
            try {
                Object key = keysInterop.readArrayElement(keysObject, index);
                if (values) {
                    assert InteropLibrary.getUncached().isString(key);
                    String stringKey = key instanceof String ? (String) key : InteropLibrary.getUncached().asString(key);
                    // the value is imported in the iterator's next method node
                    return objInterop.readMember(iteratedObject, stringKey);
                } else {
                    return key;
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                throw StopIterationException.create(e);
            }
        } else {
            throw StopIterationException.create();
        }
    }
}
