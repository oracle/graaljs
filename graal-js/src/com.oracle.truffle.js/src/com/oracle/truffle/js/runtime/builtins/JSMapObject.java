/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.interop.InteropArray;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.util.JSHashMap;

@ExportLibrary(InteropLibrary.class)
public final class JSMapObject extends JSNonProxyObject {
    private final JSHashMap map;

    protected JSMapObject(Shape shape, JSHashMap map) {
        super(shape);
        this.map = map;
    }

    public JSHashMap getMap() {
        return map;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasHashEntries() {
        return true;
    }

    @ExportMessage
    long getHashSize() {
        return getMap().size();
    }

    @ExportMessage
    Object getHashEntriesIterator() {
        return new EntriesIterator(getMap().getEntries());
    }

    @ExportMessage
    boolean isHashEntryReadable(Object key,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        return getMap().has(normalizedKey);
    }

    @ExportMessage
    Object readHashValue(Object key,
                    @Cached @Shared("exportValueNode") ExportValueNode exportValueNode,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) throws UnknownKeyException {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        Object value = getMap().get(normalizedKey);
        if (value == null) {
            throw UnknownKeyException.create(key);
        }
        return exportValueNode.execute(value);
    }

    @ExportMessage
    Object readHashValueOrDefault(Object key, Object defaultValue,
                    @Cached @Shared("exportValueNode") ExportValueNode exportValueNode,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        Object value = getMap().get(normalizedKey);
        if (value == null) {
            return defaultValue;
        }
        return exportValueNode.execute(value);
    }

    @ExportMessage
    @ExportMessage(name = "isHashEntryRemovable")
    boolean isHashEntryModifiable(Object key,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        return getMap().has(normalizedKey);
    }

    @ExportMessage
    boolean isHashEntryInsertable(Object key,
                    @CachedLibrary("this") InteropLibrary thisLibrary) {
        return !thisLibrary.isHashEntryModifiable(this, key);
    }

    @ExportMessage
    void writeHashEntry(Object key, Object value,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Exclusive ImportValueNode importValueNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        getMap().put(normalizedKey, importValueNode.executeWithTarget(value));
    }

    @ExportMessage
    void removeHashEntry(Object key,
                    @Cached @Shared("importKeyNode") ImportValueNode importKeyNode,
                    @Cached @Shared("normalizeKeyNode") JSCollectionsNormalizeNode normalizeKeyNode) throws UnknownKeyException {
        Object normalizedKey = normalizeKeyNode.execute(importKeyNode.executeWithTarget(key));
        if (!getMap().remove(normalizedKey)) {
            throw UnknownKeyException.create(key);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class EntriesIterator implements TruffleObject {
        private JSHashMap.Cursor cursor;
        private Boolean hasNext;

        private EntriesIterator(JSHashMap.Cursor cursor) {
            this.cursor = cursor;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isIterator() {
            return true;
        }

        @ExportMessage
        boolean hasIteratorNextElement() {
            if (hasNext == null) {
                hasNext = cursor.advance();
            }
            return hasNext;
        }

        @ExportMessage
        Object getIteratorNextElement() throws StopIterationException {
            if (hasIteratorNextElement()) {
                Object entryTuple = InteropArray.create(new Object[]{cursor.getKey(), cursor.getValue()});
                hasNext = null;
                return entryTuple;
            } else {
                throw StopIterationException.create();
            }
        }
    }
}
