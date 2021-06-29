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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementation of the Record (primitive) type which is a mapping from Strings to ECMAScript primitive values.
 * Since a Record value is completely immutable and will not change over time, you will not find a setter-method here.
 *
 * @see com.oracle.truffle.js.runtime.builtins.JSRecordObject
 */
@ValueType
public final class Record implements TruffleObject {

    private final TreeMap<String, Object> map;

    private Record(Map<String, Object> map) {
        this.map = new TreeMap<>(map);
    }

    public static Record create(Map<String, Object> map) {
        return new Record(map);
    }

    @TruffleBoundary
    public Object get(String key) {
        return map.get(key);
    }

    @TruffleBoundary
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public String[] getKeys() {
        return map.keySet().toArray(new String[]{});
    }

    @TruffleBoundary
    public Set<Map.Entry<String, Object>> getEntries() {
        return map.entrySet();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return equals((Record) obj);
    }

    @TruffleBoundary
    public boolean equals(Record other) {
        if (map.size() != other.map.size()) {
            return false;
        }
        for (String key : map.keySet()) {
            if (!other.map.containsKey(key)) {
                return false;
            }
            if (!JSRuntime.isSameValueZero(
                    map.get(key),
                    other.map.get(key)
            )) {
                return false;
            }
        }
        return true;
    }
}
