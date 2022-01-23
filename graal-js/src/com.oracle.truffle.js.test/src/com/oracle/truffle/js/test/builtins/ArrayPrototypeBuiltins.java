/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.js.runtime.JSContextOptions;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class ArrayPrototypeBuiltins {

    @Test
    public void testUnshift() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(JavaScriptLanguage.ID, "var o = Object.create({ length: 20000, 10: 'foo' }); Array.prototype.unshift.call(o, 'bar');");

            // inserted "bar"
            Value value = context.eval(JavaScriptLanguage.ID, "o[0]");
            assertTrue(value.isString());
            assertEquals("bar", value.asString());

            // "foo" in prototype
            value = context.eval(JavaScriptLanguage.ID, "o.hasOwnProperty(10)");
            assertTrue(value.isBoolean());
            assertFalse(value.asBoolean());
            value = context.eval(JavaScriptLanguage.ID, "o[10]");
            assertTrue(value.isString());
            assertEquals("foo", value.asString());

            // unshifted "foo"
            value = context.eval(JavaScriptLanguage.ID, "o.hasOwnProperty(11)");
            assertTrue(value.isBoolean());
            assertTrue(value.asBoolean());
            value = context.eval(JavaScriptLanguage.ID, "o[11]");
            assertTrue(value.isString());
            assertEquals("foo", value.asString());
        }
    }

    @Test
    public void testBasicGroupBy() {
        String src = "[41, 42, 43, 44, 45].groupBy(n => n % 2 === 0 ? 'even' : 'odd');";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            var value = context.eval(JavaScriptLanguage.ID, src);
            Assert.assertTrue(value.hasMembers());
            Assert.assertTrue(value.hasMember("even"));
            Assert.assertTrue(value.hasMember("odd"));
            var even = value.getMember("even");
            Assert.assertTrue(even.hasMembers());
            Assert.assertEquals(2, even.getArraySize());
            Assert.assertEquals(42, even.getArrayElement(0).asInt());
            Assert.assertEquals(44, even.getArrayElement(1).asInt());
            var odd = value.getMember("odd");
            Assert.assertTrue(odd.hasMembers());
            Assert.assertEquals(3, odd.getArraySize());
            Assert.assertEquals(41, odd.getArrayElement(0).asInt());
            Assert.assertEquals(43, odd.getArrayElement(1).asInt());
            Assert.assertEquals(45, odd.getArrayElement(2).asInt());
        }
    }

    @Test
    public void testBasicGroupByToMap() {
        String src = "var odd = { odd: true };" +
                        "var even = { even: true };" +
                        "[41, 42, 43, 44, 45].groupByToMap(n => {" +
                        "  return n % 2 === 0 ? even : odd;" +
                        "});";
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            var result = context.eval(JavaScriptLanguage.ID, src);
            var odd = context.getBindings(JavaScriptLanguage.ID).getMember("odd");
            var even = context.getBindings(JavaScriptLanguage.ID).getMember("even");

            var oddResult = result.invokeMember("get", odd);
            Assert.assertEquals(3, oddResult.getArraySize());
            Assert.assertEquals(41, oddResult.getArrayElement(0).asInt());
            Assert.assertEquals(43, oddResult.getArrayElement(1).asInt());
            Assert.assertEquals(45, oddResult.getArrayElement(2).asInt());

            var evenResult = result.invokeMember("get", even);
            Assert.assertEquals(2, evenResult.getArraySize());
            Assert.assertEquals(42, evenResult.getArrayElement(0).asInt());
            Assert.assertEquals(44, evenResult.getArrayElement(1).asInt());
        }
    }

}
