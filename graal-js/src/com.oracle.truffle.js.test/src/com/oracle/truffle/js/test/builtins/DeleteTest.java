/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignTestMap;

/**
 * Tests for the delete keyword.
 */
public class DeleteTest extends JSTest {

    @Test
    public void testDeleteGlobalVariable() {
        assertEquals(false, testHelper.run("var a = 'foo'; delete a;"));
    }

    @Test
    public void testDeleteSymbol() {
        assertEquals(true, testHelper.run("var s = Symbol('foo'); delete s.nonExistentProperty;"));
        assertEquals(true, testHelper.run("var s = Symbol('foo'); s.bar='bar'; delete s.bar"));
    }

    @Test
    public void testDeleteBigInt() {
        assertEquals(true, testHelper.run("var bi = BigInt(1234567890); delete bi.nonExistentProperty;"));
        assertEquals(true, testHelper.run("var bi = BigInt(1234567890); bi.bar='yes'; delete bi.bar;"));
    }

    @Test
    public void testDeleteSafeInteger() {
        assertEquals(true, testHelper.run("var li = 2147483647; li+=li; delete li.nonExistentProperty;"));
        assertEquals(true, testHelper.run("var li = 2147483647; li+=li; li.bar='yes'; delete li.bar;"));
    }

    @Test
    public void testDeleteString() {
        assertEquals(true, testHelper.run("var str='str'; delete str.nonExistentProperty;"));
        assertEquals(true, testHelper.run("var str='str'; str.bar='yes'; delete str.bar;"));
        assertEquals(false, testHelper.run("var str='str'; delete str[1];"));
        assertEquals(true, testHelper.run("var str='str'; delete str[-1];"));
        assertEquals(true, testHelper.run("var str='str'; delete str[10];"));
    }

    @Test
    public void testDeleteForeign() {
        final ForeignTestMap map = new ForeignTestMap();
        map.getContainer().put("foo", 42);
        map.getContainer().put("bar", "test");
        map.getContainer().put("1", "test");
        map.getContainer().put("1.5", "test");
        testHelper.getPolyglotContext().getBindings(ID).putMember("foreign", map);

        assertEquals(true, testHelper.run("delete foreign.nonExistentProperty;"));
        assertEquals(true, testHelper.run("delete foreign.foo;"));
        assertEquals(true, testHelper.run("delete foreign[1];"));
        assertEquals(true, testHelper.run("delete foreign[1.5];"));
        assertEquals(true, testHelper.run("delete foreign[foreign];"));
        assertEquals(true, testHelper.run("delete foreign[new String('test')];"));
        assertEquals(true, testHelper.run("delete foreign[new Number(123)];"));
    }

    @Test
    public void testDeleteSuperReference() {
        final String expectedErrorMessage = "Unsupported reference";
        try (Context c = JSTest.newContextBuilder(ID).build()) {
            assertTrue(c.eval(ID, "" +
                            "let success = false;" +
                            "class C extends class {} {" +
                            "  constructor() {" +
                            "    super();" +
                            "    delete super.x;" +
                            "  }" +
                            "}" +
                            "try {" +
                            "  new C();" +
                            "} catch (e) {" +
                            "  if (e instanceof ReferenceError) {" +
                            "    success = e.message.includes(" + JSRuntime.quote(expectedErrorMessage) + ");" +
                            "  }" +
                            "}" +
                            "success;").asBoolean());
        }
        try (Context c = JSTest.newContextBuilder(ID).build()) {
            assertTrue(c.eval(ID, "" +
                            "let success = false, sideEffect = false;" +
                            "class C extends class {} {" +
                            "  constructor() {" +
                            "    super();" +
                            "    delete super[sideEffect = true, 'x'];" +
                            "  }" +
                            "}" +
                            "try {" +
                            "  new C();" +
                            "} catch (e) {" +
                            "  if (e instanceof ReferenceError) {" +
                            "    success = !sideEffect && e.message.includes(" + JSRuntime.quote(expectedErrorMessage) + ");" +
                            "  }" +
                            "}" +
                            "success;").asBoolean());
        }
    }
}
