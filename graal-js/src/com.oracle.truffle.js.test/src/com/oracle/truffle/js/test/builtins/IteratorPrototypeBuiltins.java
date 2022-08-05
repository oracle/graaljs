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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class IteratorPrototypeBuiltins {
    @Test
    public void testObject() {
        String src = "var parent = Object.getPrototypeOf(Iterator.prototype) === Object.prototype; var proto = typeof Iterator.prototype";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            context.eval(JavaScriptLanguage.ID, src);
            var parent = context.getBindings(JavaScriptLanguage.ID).getMember("parent");
            var proto = context.getBindings(JavaScriptLanguage.ID).getMember("proto");

            Assert.assertTrue(parent.asBoolean());
            Assert.assertEquals("object", proto.asString());
        }
    }

    @Test
    public void testConstructor() {
        String src = "Iterator.prototype.constructor === Iterator";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Assert.assertTrue(context.eval(JavaScriptLanguage.ID, src).asBoolean());
        }
    }

    @Test
    public void testMap() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1, 2, 3].values().map(x => 2*x).toArray()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasArrayElements());
            Assert.assertEquals(3, result.getArraySize());
            Assert.assertEquals(2, result.getArrayElement(0).asInt());
            Assert.assertEquals(4, result.getArrayElement(1).asInt());
            Assert.assertEquals(6, result.getArrayElement(2).asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.prototype.map.call({}, x => x)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            try {
                context.eval(JavaScriptLanguage.ID, "[].values().map(1)");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            result = context.eval(JavaScriptLanguage.ID, "Iterator.prototype.map.call({next: () => ({value: 1, done: true})}, x => x).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; () => Iterator.prototype.map.call({next: () => ({value: 1, done: false}), return: () => called = true}, x => {throw new Error('test')}).next()");
            try {
                result.execute();
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("Error: test", e.getMessage());
                Assert.assertTrue(context.eval(JavaScriptLanguage.ID, "called").asBoolean());
            }
        }
    }
}
