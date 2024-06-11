/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

public class GR43097 {

    private static final String SIMPLE_MODULE_SOURCE_CODE = "" +
                    "export function f(a) { return a; };\n" +
                    "export var a = 41;\n";

    @Test
    public void testModuleNamespaceObjectPolyglotValue() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build();) {
            Source moduleSrc = Source.newBuilder(JavaScriptLanguage.ID, SIMPLE_MODULE_SOURCE_CODE, "esm-ns.mjs").buildLiteral();
            Value ns = context.eval(moduleSrc);

            // Check member 'f'
            assertTrue(ns.hasMember("f"));
            assertTrue(ns.canInvokeMember("f"));
            assertEquals(43, ns.invokeMember("f", 43).asInt());
            assertTrue(ns.getMember("f").canExecute());
            assertEquals(43, ns.getMember("f").execute(43).asInt());

            // Note: Module namespace property names are always sorted.
            assertArrayEquals(new String[]{"a", "f"}, ns.getMemberKeys().toArray());

            // Properties are writable, but writing throws.
            try {
                ns.putMember("f", "bla");
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), startsWith("TypeError"));
            }
            assertTrue(ns.getMember("f").canExecute());
            assertEquals(43, ns.getMember("f").execute(43).asInt());

            try {
                ns.removeMember("f");
                fail("should have thrown");
            } catch (UnsupportedOperationException e) {
                // non-removable member key
            }
            assertTrue(ns.hasMember("f"));
            assertTrue(ns.getMember("f").canExecute());
            assertEquals(43, ns.getMember("f").execute(43).asInt());

            // Check member 'a'
            assertTrue(ns.hasMember("a"));
            assertFalse(ns.canInvokeMember("a"));
            assertEquals(41, ns.getMember("a").asInt());

            // Properties are writable, but writing throws.
            try {
                ns.putMember("a", 42);
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), startsWith("TypeError"));
            }
            assertTrue(ns.hasMember("a"));
            assertEquals(41, ns.getMember("a").asInt());

            try {
                ns.removeMember("a");
                fail("should have thrown");
            } catch (UnsupportedOperationException e) {
                // non-removable member key
            }
            assertTrue(ns.hasMember("a"));
            assertEquals(41, ns.getMember("a").asInt());
        }
    }

    @Test
    public void testModuleNamespaceObjectInterop() throws InteropException {
        try (TestHelper testHelper = new TestHelper(JSTest.newContextBuilder().option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true"))) {
            com.oracle.truffle.api.source.Source moduleSrc = com.oracle.truffle.api.source.Source.newBuilder(JavaScriptLanguage.ID, SIMPLE_MODULE_SOURCE_CODE, "esm-ns.mjs").build();
            Object ns = testHelper.runNoPolyglot(moduleSrc);
            InteropLibrary cached = TestHelper.adopt(InteropLibrary.getFactory().create(ns));
            InteropLibrary uncached = InteropLibrary.getUncached();

            testHelper.enterContext();

            // Check member 'f'
            assertTrue(cached.isMemberInvocable(ns, "f"));
            assertTrue(cached.isMemberReadable(ns, "f"));
            assertEquals(43, cached.invokeMember(ns, "f", 43));
            assertTrue(uncached.isExecutable(cached.readMember(ns, "f")));
            assertEquals(43, uncached.execute(cached.readMember(ns, "f"), 43));

            // Check member 'a'
            assertTrue(cached.isMemberReadable(ns, "a"));
            assertFalse(cached.isMemberInvocable(ns, "a"));
            assertEquals(41, cached.readMember(ns, "a"));

            testHelper.leaveContext();
        }
    }

    @Test
    public void testModuleNamespaceObjectDefineProperty() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build();) {
            Source moduleSrc = Source.newBuilder(JavaScriptLanguage.ID, SIMPLE_MODULE_SOURCE_CODE, "esm-ns.mjs").buildLiteral();
            Value ns = context.eval(moduleSrc);

            Value tester = context.eval(Source.create(JavaScriptLanguage.ID, "(function(ns){ Object.defineProperty(ns, 'f', {writable: false, value: 'boom'}) })"));
            try {
                tester.executeVoid(ns);
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), startsWith("TypeError"));
            }

            assertTrue(ns.canInvokeMember("f"));
            assertEquals(43, ns.invokeMember("f", 43).asInt());
        }
    }

}
