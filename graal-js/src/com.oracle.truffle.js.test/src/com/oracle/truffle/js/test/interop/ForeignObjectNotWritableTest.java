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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;

public class ForeignObjectNotWritableTest {

    @Test
    public void testJavaArrayPush() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                        option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").build()) {
            Value b = context.getBindings("js");

            b.putMember("array", new Object[]{"a"});

            assertThrowsTypeError(() -> context.eval("js", "array.push('b');"));
            assertEquals(1, b.getMember("array").getArraySize());
        }
    }

    @Test
    public void testJavaArrayOOB() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).build()) {
            Value b = context.getBindings("js");

            b.putMember("array", new Object[]{"a"});

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; array[1] = 'b';"));
            assertEquals(1, b.getMember("array").getArraySize());

            context.eval("js", "array[1] = 'b';");
            assertEquals(1, b.getMember("array").getArraySize());
        }
    }

    @Test
    public void testJavaArrayIncompatibleType() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).build()) {
            Value b = context.getBindings("js");

            b.putMember("array", new int[]{42, 43, 44});

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; array[2] = {};"));
            context.eval("js", "array[2] = {};");

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; array[4] = {};"));
            context.eval("js", "array[4] = {};");
        }
    }

    @Test
    public void testJavaListOOB() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).build()) {
            Value b = context.getBindings("js");

            List<Object> list = new ArrayList<>();
            list.add("a");
            b.putMember("array", list);

            context.eval("js", "'use strict'; array[1] = 'b';");
            assertEquals(2, b.getMember("array").getArraySize());
            context.eval("js", "array[2] = 'c';");
            assertEquals(3, b.getMember("array").getArraySize());

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; array[4] = 'd';"));
            context.eval("js", "array[4] = 'd';");
            assertEquals(3, b.getMember("array").getArraySize());
        }
    }

    @Test
    public void testJavaObjectSetMember() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).build()) {
            Value b = context.getBindings("js");

            Object list = new Object();
            b.putMember("object", list);

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; object['new'] = 'b';"));

            context.eval("js", "object['new'] = 'b';");
            assertFalse(b.getMember("object").hasMember("new"));
        }
    }

    @Test
    public void testJavaObjectFieldIncompatibleType() {
        try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).build()) {
            Value b = context.getBindings("js");

            Object obj = new ObjectWithField();
            b.putMember("object", obj);

            assertTrue(b.getMember("object").hasMember("field"));

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; object['field'] = 'b';"));
            context.eval("js", "object['field'] = 'b';");

            context.eval("js", "'use strict'; object['field'] = 42;");
            assertEquals(42, b.getMember("object").getMember("field").asInt());

            assertThrowsTypeError(() -> context.eval("js", "'use strict'; object.field = 'b';"));
            context.eval("js", "object.field = 'b';");

            context.eval("js", "'use strict'; object.field = 43;");
            assertEquals(43, b.getMember("object").getMember("field").asInt());
        }
    }

    public static class ObjectWithField {
        public int field;
    }

    private static void assertThrowsTypeError(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected TypeError");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertTrue(e.getMessage().startsWith("TypeError"));
        }
    }

}
