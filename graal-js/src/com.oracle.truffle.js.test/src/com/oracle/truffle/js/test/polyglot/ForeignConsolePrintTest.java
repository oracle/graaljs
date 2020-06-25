/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.truffleinterop.JavaScriptLanguageView;
import com.oracle.truffle.js.test.JSTest;

public final class ForeignConsolePrintTest {
    private Context ctx;

    @Before
    public void setup() {
        ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build();
    }

    @After
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void testForeignArray() {
        final String script = "(function (a) { return '' + a; })";
        final Value fun = ctx.eval(JavaScriptLanguage.ID, script);
        Value res = fun.execute(new ArrayTruffleObject(new int[]{0, 1, 2, 3, 4}));
        String sRes = res.asString();
        assertEquals("(5)[0, 1, 2, 3, 4]", sRes);
    }

    @Test
    public void testToDisplayString() {
        ctx.enter();
        try {
            ctx.initialize(JavaScriptLanguage.ID);
            final ForeignTestMap map = new ForeignTestMap();
            map.getContainer().put("foo", "bar");
            ForeignTestFunction f = new ForeignTestFunction("test", (arg) -> {
                return arg[0] + ", " + arg[1];
            });
            map.getContainer().put("fun", f);

            JavaScriptLanguageView jslv = JavaScriptLanguageView.create(map);
            Object resWithSideEffects = InteropLibrary.getFactory().getUncached(jslv).toDisplayString(jslv, true);
            Object resWithoutSideEffects = InteropLibrary.getFactory().getUncached(jslv).toDisplayString(jslv, false);
            assertEquals("{foo: \"bar\", fun: function test()}", resWithSideEffects);
            assertEquals("{foo: \"bar\", fun: f()}", resWithoutSideEffects);
        } finally {
            ctx.leave();
        }
    }

    @Test
    public void testForeignObject() {
        final String script = "(function (a) { return '' + a; })";
        final Value fun = ctx.eval(JavaScriptLanguage.ID, script);
        final ForeignTestMap map = new ForeignTestMap();
        map.getContainer().put("x", 42);
        map.getContainer().put("y", "foo");

        Value res = fun.execute(map);
        String sRes = res.asString();
        assertEquals("{x: 42, y: \"foo\"}", sRes);
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ArrayTruffleObject implements TruffleObject {

        private final int[] array;

        ArrayTruffleObject(int[] array) {
            this.array = array;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return array.length;
        }

        @ExportMessage
        Object readArrayElement(long index) throws UnsupportedMessageException {
            if (!isArrayElementReadable(index)) {
                throw UnsupportedMessageException.create();
            }
            return array[(int) index];
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < array.length;
        }
    }
}
