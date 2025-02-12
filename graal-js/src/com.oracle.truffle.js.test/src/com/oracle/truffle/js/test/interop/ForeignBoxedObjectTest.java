/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignNull;

public class ForeignBoxedObjectTest {

    @Test
    public void testForeignNull() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("obj", new ForeignNull());
            assertTrue(context.eval(ID, "Object.getPrototypeOf(Object(obj)) === Object.prototype").asBoolean());
            assertTrue(context.eval(ID, "Object.getPrototypeOf(new Object(obj)) === Object.prototype").asBoolean());
            assertTrue(context.eval(ID, "Object.getPrototypeOf(Object.create(obj)) === null").asBoolean());
            assertTrue(context.eval(ID, "try { obj.foo; false; } catch (e) { e instanceof TypeError }").asBoolean());
            assertTrue(context.eval(ID, "try { obj.foo(); false; } catch (e) { e instanceof TypeError }").asBoolean());
            assertFalse(context.eval(ID, "obj instanceof Object").asBoolean());
        }
    }

    @Test
    public void testForeignBoxedString() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("obj", ForeignBoxedObject.createNew("foo"));
            assertTrue(context.eval(ID, "typeof Object(obj) === 'object'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj.includes === 'function'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj['includes'] === 'function'").asBoolean());
            assertEquals("foo", context.eval(ID, "obj.toString()").asString());
            assertEquals("foo", context.eval(ID, "obj.valueOf()").asString());
            assertEquals("foo", context.eval(ID, "obj['valueOf']()").asString());
            assertTrue(context.eval(ID, "obj.includes('o')").asBoolean());
            assertFalse(context.eval(ID, "obj instanceof Object").asBoolean());
        }
    }

    @Test
    public void testForeignBoxedNumber() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.LOCALE_NAME, "en").build()) {
            context.getBindings("js").putMember("obj", ForeignBoxedObject.createNew(42));
            assertTrue(context.eval(ID, "typeof Object(obj) === 'object'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj.valueOf === 'function'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj['valueOf'] === 'function'").asBoolean());
            assertEquals("4.2e+1", context.eval(ID, "obj.toExponential()").asString());
            assertEquals("4e+1", context.eval(ID, "obj.toExponential(0)").asString());
            assertEquals("42.00", context.eval(ID, "obj.toFixed(2)").asString());
            assertEquals("42", context.eval(ID, "obj.toLocaleString()").asString());
            assertEquals("42", context.eval(ID, "obj.toPrecision()").asString());
            assertEquals("42.0", context.eval(ID, "obj.toPrecision(3)").asString());
            assertEquals("42", context.eval(ID, "obj.toString()").asString());
            assertEquals(42, context.eval(ID, "obj.valueOf()").asInt());
            assertEquals(42, context.eval(ID, "obj['valueOf']()").asInt());
            assertFalse(context.eval(ID, "obj instanceof Object").asBoolean());
        }
    }

    @Test
    public void testForeignBoxedBoolean() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.getBindings("js").putMember("obj", ForeignBoxedObject.createNew(true));
            assertTrue(context.eval(ID, "typeof Object(obj) === 'object'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj.valueOf === 'function'").asBoolean());
            assertTrue(context.eval(ID, "typeof obj['valueOf'] === 'function'").asBoolean());
            assertTrue(context.eval(ID, "obj.valueOf()").asBoolean());
            assertTrue(context.eval(ID, "obj['valueOf']()").asBoolean());
            assertEquals("true", context.eval(ID, "obj.toString()").asString());
            assertFalse(context.eval(ID, "obj instanceof Object").asBoolean());
        }
    }

}
