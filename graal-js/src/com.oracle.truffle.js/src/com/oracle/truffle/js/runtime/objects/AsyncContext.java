/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.Arrays;

import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;

/**
 * A list of records with fields [[AsyncContextKey]] (a Symbol) and [[AsyncContextValue]] (an
 * ECMAScript language value).
 */
public final class AsyncContext {

    private static final AsyncContext EMPTY = new AsyncContext();

    /** Pairs of Symbol, Object. */
    private final Object[] mapping;

    private AsyncContext() {
        this(ScriptArray.EMPTY_OBJECT_ARRAY);
    }

    private AsyncContext(Object[] mapping) {
        this.mapping = mapping;
    }

    public static AsyncContext empty() {
        return EMPTY;
    }

    public AsyncContext withMapping(Symbol key, Object value) {
        int len = mapping.length;
        for (int i = 0; i < len; i += 2) {
            Object k = mapping[i];
            Object v = mapping[i + 1];
            if (k == key) {
                /*
                 * Slight deviation from the spec: Since the order of the keys does not matter,
                 * there is no need to have the new entry at the end, so we just replace the value
                 * at the previous position (or return this context if the value is the same).
                 */
                if (v == value) {
                    return this;
                } else {
                    Object[] newMapping = Arrays.copyOf(mapping, len);
                    assert newMapping[i] == key;
                    newMapping[i + 1] = value;
                    return new AsyncContext(newMapping);
                }
            }
        }
        Object[] newMapping = Arrays.copyOf(mapping, len + 2);
        newMapping[len] = key;
        newMapping[len + 1] = value;
        return new AsyncContext(newMapping);
    }

    public Object getOrDefault(Symbol asyncContextKey, Object defaultValue) {
        for (int i = 0; i < mapping.length; i += 2) {
            Object k = mapping[i];
            if (k == asyncContextKey) {
                return mapping[i + 1];
            }
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "AsyncContext" + Arrays.toString(mapping);
    }
}
