/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class WrapForIteratorPrototypeBuiltinsTest {
    @Test
    public void testPrototype() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Object.getPrototypeOf(Object.getPrototypeOf(Iterator.from({next: () => ({done: true})}))) === Iterator.prototype");
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testNext() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true})}).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertEquals("object", result.getMember("value").asString());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true})}).next.call([].values())");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
        }
    }

    @Test
    public void testReturn() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: false}), return: () => ({done: true})}).return()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; Iterator.from({next: () => ({value: typeof this, done: false}), return: () => (called = true, {done: true})}).return(); called");
            Assert.assertTrue(result.asBoolean());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true}), return: () => 1}).return()");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true}), return: () => ({done: true})}).return.call([].values())");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
        }
    }
}
