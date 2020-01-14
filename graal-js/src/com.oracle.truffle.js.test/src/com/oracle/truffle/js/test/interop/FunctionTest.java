/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class FunctionTest {

    @Test
    public void testInstantiate() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value functionHolder = context.eval(JavaScriptLanguage.ID, "({ fn: function(arg) { this.arg = arg; } })");
            Value function = functionHolder.getMember("fn");

            // function should be instantiable
            assertTrue(function.canInstantiate());
            Value instance = function.newInstance(42);

            // verify that the argument was passed correctly
            assertTrue(instance.hasMember("arg"));
            Value argument = instance.getMember("arg");
            assertTrue(argument.fitsInInt());
            assertEquals(42, argument.asInt());

            // verify that the result is an instance of the original function
            Value instanceOf = context.eval("js", "(function(instance, fn) { return instance instanceof fn; })");
            assertTrue(instanceOf.canExecute());
            assertTrue(instanceOf.execute(instance, function).asBoolean());
        }
    }

    @Test
    public void testInteropBindMemberFunctions() throws Exception {
        Source source = Source.newBuilder(ID, "" +
                        "var ob = {v:2};\n" +
                        "var v = 0;" +
                        "var f = function(a) {\n" +
                        "  this.v += a;" +
                        "}\n" +
                        "ob.f = f;\n" +
                        "ob\n",
                        "bindTest.js").build();

        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.BIND_MEMBER_FUNCTIONS_NAME, "true").build()) {
            Value object = context.eval(source);
            object.getMember("f").execute(40);
            Value result = object.getMember("v");
            assertTrue("Is number: " + result, result.isNumber());
            assertEquals(42, result.asInt());
        }

        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.BIND_MEMBER_FUNCTIONS_NAME, "false").build()) {
            Value object = context.eval(source);
            object.getMember("f").execute(40);
            Value result = object.getMember("v");
            assertTrue("Is number: " + result, result.isNumber());
            assertEquals(2, result.asInt());
            result = context.getBindings(ID).getMember("v");
            assertTrue("Is number: " + result, result.isNumber());
            assertEquals(40, result.asInt());
        }

        // default
        try (Context context = JSTest.newContextBuilder().build()) {
            Value object = context.eval(source);
            object.getMember("f").execute(40);
            Value result = object.getMember("v");
            assertTrue("Is number: " + result, result.isNumber());
            assertEquals(42, result.asInt());
        }
    }

}
