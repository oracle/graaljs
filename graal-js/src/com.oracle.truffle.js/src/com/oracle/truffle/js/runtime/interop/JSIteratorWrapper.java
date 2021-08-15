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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.interop.JSInteropGetIteratorNextNode;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

/**
 * A wrapper around a JS iterator that fetches the next element on
 * {@link InteropLibrary#hasIteratorNextElement(Object)} and saves it for
 * {@link InteropLibrary#getIteratorNextElement(Object)}.
 */
@ExportLibrary(value = InteropLibrary.class, delegateTo = "iterator")
public final class JSIteratorWrapper implements TruffleObject {

    final DynamicObject iterator;
    private final IteratorRecord iteratorRecord;
    private Object next;

    private static final Object STOP = StopIterationException.create();

    private JSIteratorWrapper(IteratorRecord iterator) {
        this.iterator = iterator.getIterator();
        this.iteratorRecord = iterator;
    }

    public static JSIteratorWrapper create(IteratorRecord iterator) {
        return new JSIteratorWrapper(iterator);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isIterator() {
        return true;
    }

    private Object next(JavaScriptLanguage language, JSRealm realm, JSInteropGetIteratorNextNode iteratorNextNode) {
        language.interopBoundaryEnter(realm);
        try {
            return iteratorNextNode.getIteratorNextElement(iteratorRecord, language, STOP);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    boolean hasIteratorNextElement(
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached @Shared("getIteratorNext") JSInteropGetIteratorNextNode iteratorNextNode) {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        if (next == null) {
            next = next(language, realm, iteratorNextNode);
        }
        return next != STOP;
    }

    @ExportMessage
    Object getIteratorNextElement(
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached @Shared("getIteratorNext") JSInteropGetIteratorNextNode iteratorNextNode) throws StopIterationException {
        if (hasIteratorNextElement(self, iteratorNextNode)) {
            Object result = next;
            next = null;
            return result;
        } else {
            throw StopIterationException.create();
        }
    }

}
