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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class ObjectFunctionBuiltinsInteropTest {

    private static void verifyDescriptor(Value desc, boolean expectedEnumerable, boolean expectedWritable, boolean expectedConfigurable) {
        assertTrue(desc.hasMember("enumerable"));
        Value enumerable = desc.getMember("enumerable");
        assertTrue(enumerable.isBoolean());
        assertTrue(enumerable.asBoolean() == expectedEnumerable);

        assertTrue(desc.hasMember("writable"));
        Value writable = desc.getMember("writable");
        assertTrue(writable.isBoolean());
        assertTrue(writable.asBoolean() == expectedWritable);

        assertTrue(desc.hasMember("configurable"));
        Value configurable = desc.getMember("configurable");
        assertTrue(configurable.isBoolean());
        assertTrue(configurable.asBoolean() == expectedConfigurable);
    }

    @Test
    public void testGetOwnPropertyDescriptor() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).build()) {
            String code = "Object.getOwnPropertyDescriptor(new java.awt.Point(42, 211), 'x')";
            Value desc = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(desc.hasMember("value"));
            Value value = desc.getMember("value");
            assertTrue(value.isNumber());
            assertEquals(42, value.asInt());
            verifyDescriptor(desc, true, true, false);

            code = "Object.getOwnPropertyDescriptor(new java.awt.Point(42, 211), 'getY')";
            desc = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(desc.hasMember("value"));
            value = desc.getMember("value");
            assertTrue(value.canExecute());
            verifyDescriptor(desc, true, false, false);

            code = "Object.getOwnPropertyDescriptor(new java.awt.Point(42, 211), 'z')";
            desc = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(desc.isNull());
        }
    }

    @Test
    public void testGetOwnPropertyDescriptorArray() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).build()) {
            String code = "var byteArray = Java.type('byte[]'); var array = new byteArray(2); array[0] = 42; Object.getOwnPropertyDescriptor(array, 0)";
            Value desc = context.eval(JavaScriptLanguage.ID, code);

            assertTrue(desc.hasMember("value"));
            Value value = desc.getMember("value");
            assertTrue(value.isNumber());
            assertEquals(42, value.asInt());
            verifyDescriptor(desc, true, true, false);

            code = "var byteArray = Java.type('byte[]'); var array = new byteArray(2); array[0] = 42; Object.getOwnPropertyDescriptor(array, 2)";
            desc = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(desc.isNull());
        }
    }

    @Test
    public void testGetOwnPropertyDescriptors() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).build()) {
            String code = "Object.getOwnPropertyDescriptors(new java.awt.Point(42, 211))";
            Value result = context.eval(JavaScriptLanguage.ID, code);

            assertTrue(result.hasMember("x"));
            Value desc = result.getMember("x");
            assertTrue(desc.hasMember("value"));
            Value value = desc.getMember("value");
            assertTrue(value.isNumber());
            assertEquals(42, value.asInt());
            verifyDescriptor(desc, true, true, false);

            assertTrue(result.hasMember("getY"));
            desc = result.getMember("getY");
            assertTrue(desc.hasMember("value"));
            value = desc.getMember("value");
            assertTrue(value.canExecute());
            verifyDescriptor(desc, true, false, false);

            assertFalse(result.hasMember("z"));
        }
    }

    @Test
    public void testGetOwnPropertyDescriptorsArray() {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(true).build()) {
            String code = "var byteArray = Java.type('byte[]'); var array = new byteArray(2); array[0] = 42; Object.getOwnPropertyDescriptors(array)";
            Value result = context.eval(JavaScriptLanguage.ID, code);

            assertTrue(result.hasMember("0"));
            Value desc = result.getMember("0");
            assertTrue(desc.hasMember("value"));
            Value value = desc.getMember("value");
            assertTrue(value.isNumber());
            assertEquals(42, value.asInt());
            verifyDescriptor(desc, true, true, false);

            assertFalse(result.hasMember("2"));
        }
    }

}
