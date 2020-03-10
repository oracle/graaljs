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
package com.oracle.truffle.js.test.runtime;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.runtime.JSContextOptions.ECMASCRIPT_VERSION_NAME;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class PrivateClassMemberTest {
    @Test
    public void testPrivateStaticMethod() {
        try (Context context = JSTest.newContextBuilder().option(ECMASCRIPT_VERSION_NAME, String.valueOf(2021)).build()) {
            context.eval(ID, "class C { static #m() {} t() { class D { d() { return C.#m(); } } return new D().d(); }}\n" +
                            "new C().t();");
        }
    }

    @Test
    public void testPrivateStaticMethodWrongReceiver1() {
        try (Context context = JSTest.newContextBuilder().option(ECMASCRIPT_VERSION_NAME, String.valueOf(2021)).build()) {
            context.eval(ID, "class C { static #m() {} t() { class D { d() { return D.#m(); } } return new D().d(); }}\n" +
                            "new C().t();");
            fail("should have thrown a TypeError");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertThat(e.getMessage(), CoreMatchers.startsWith("TypeError"));
        }
    }

    @Test
    public void testPrivateStaticMethodWrongReceiver2() {
        try (Context context = JSTest.newContextBuilder().option(ECMASCRIPT_VERSION_NAME, String.valueOf(2021)).build()) {
            context.eval(ID, "class C { static #m() {} t() { class D { static #w(){} d(){ return D.#m(); } } return new D().d(); }}\n" +
                            "new C().t();");
            fail("should have thrown a TypeError");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertThat(e.getMessage(), CoreMatchers.startsWith("TypeError"));
        }
    }
}
