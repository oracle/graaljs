/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.runtime.JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class ForeignObjectPrototypeTest {

    @Test
    public void testProxyArray() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").build()) {
            ProxyArray array = ProxyArray.fromArray("fun", "with", "proxy", "array");
            Value result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList("array", "fun", "proxy", "with"), result.as(List.class));

            array = ProxyArray.fromArray(4, 5, 6, 1, 2, 3, 7, 8, 9);
            result = context.eval(ID, "(array) => array.reduce((a,b) => a + b)").execute(array);
            assertEquals(45, result.asInt());
        }
    }

    @Test
    public void testHostArray() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").allowHostAccess(HostAccess.ALL).build()) {
            Value array = context.asValue(new String[]{"fun", "with", "proxy", "array"});
            Value result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList("array", "fun", "proxy", "with"), result.as(List.class));

            array = context.asValue(new int[]{4, 5, 6, 1, 2, 3, 7, 8, 9});
            result = context.eval(ID, "(array) => array.reduce((a,b) => a + b)").execute(array);
            assertEquals(45, result.asInt());
            result = context.eval(ID, "(array) => array.sort()").execute(array);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result.as(List.class));
        }
    }

    @Test
    public void testProxyExecutable() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").build()) {
            context.getBindings(ID).putMember("moo", (ProxyExecutable) args -> {
                assertEquals(3, args.length);
                assertTrue(args[0].isString());
                assertTrue(args[1].isNumber());
                assertTrue(args[2].isBoolean());
                assertEquals("foo", args[0].asString());
                assertEquals(123, args[1].asInt());
                assertTrue(args[2].asBoolean());
                return "hi";
            });
            Value value = context.eval("js", "(function(...args) { return moo.apply(null, args); })('foo', 123, true)");
            assertTrue(value.isString());
            assertEquals("hi", value.asString());
        }
    }

    @Test
    public void testHostMethodStatic() {
        try (Context context = JSTest.newContextBuilder(ID).option(FOREIGN_OBJECT_PROTOTYPE_NAME, "true").allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true).build()) {
            Value method = context.eval(ID, "Java.type('java.util.Arrays').asList");
            assertTrue(method.canExecute());
            assertFalse(method.hasMembers());
            Value result = context.eval(ID, "m => m.call(null, 0, 8, 1, 5)").execute(method);
            assertEquals(Arrays.asList(0, 8, 1, 5), result.as(List.class));
            result = context.eval(ID, "m => m.apply(null, [0, 8, 1, 5])").execute(method);
            assertEquals(Arrays.asList(0, 8, 1, 5), result.as(List.class));
        }
    }

}
