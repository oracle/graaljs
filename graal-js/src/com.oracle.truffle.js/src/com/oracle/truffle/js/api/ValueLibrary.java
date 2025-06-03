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

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;

/**
 * Value library provides basic operations that are common to all JavaScript values (like
 * {@code ToString()} and {@code ToNumber()} operations). It also provides basic value type
 * information.
 */
@GenerateLibrary
public abstract class ValueLibrary extends Library {
    private static final LibraryFactory<ValueLibrary> FACTORY = LibraryFactory.resolve(ValueLibrary.class);

    /**
     * Returns the library factory for the value library.
     */
    public static LibraryFactory<ValueLibrary> getFactory() {
        return FACTORY;
    }

    /**
     * Returns the uncached automatically dispatched version of the value library.
     */
    public static ValueLibrary getUncached() {
        return FACTORY.getUncached();
    }

    /**
     * Returns {@code true} if the value is JavaScript {@code Proxy}, returns {@code false}
     * otherwise.
     */
    public boolean isProxy(Object value) {
        return value instanceof JSProxyObject;
    }

    /**
     * Returns {@code true} if the value is JavaScript {@code Promise}, returns {@code false}
     * otherwise.
     */
    public boolean isPromise(Object value) {
        return value instanceof JSPromiseObject;
    }

    /**
     * Returns {@code true} if the value is JavaScript {@code ArrayBuffer}, returns {@code false}
     * otherwise.
     */
    public boolean isArrayBuffer(Object value) {
        return value instanceof JSArrayBufferObject;
    }

    /**
     * Performs {@code ToString()} operation on the given {@code value}.
     */
    public TruffleString toString(Object value) {
        return JSRuntime.toString(value);
    }

    /**
     * Performs {@code ToNumber()} operation on the given {@code value}.
     */
    public Number toNumber(Object value) {
        return JSRuntime.toNumber(value);
    }

    /**
     * Performs {@code ToBoolean()} operation on the given {@code value}.
     */
    public boolean toBoolean(Object value) {
        return JSRuntime.toBoolean(value);
    }

}
