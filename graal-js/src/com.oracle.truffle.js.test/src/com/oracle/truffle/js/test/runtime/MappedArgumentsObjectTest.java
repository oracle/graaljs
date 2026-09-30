/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

public class MappedArgumentsObjectTest extends JSTest {

    @Override
    public void setup() {
        super.setup();
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void testConnectedArgumentCount() {
        assertConnectedArgumentCount("(function () { return arguments; })(1)", 0, 1);
        assertConnectedArgumentCount("(function (a) { return arguments; })(1, 2)", 1, 2);
        assertConnectedArgumentCount("(function (a, b) { return arguments; })(1)", 1, 1);
        assertConnectedArgumentCount("(function (a) { return arguments; })()", 0, 0);
        assertConnectedArgumentCount("(function (a, b) { return arguments; })(1, 2)", 2, 2);
    }

    @Test
    public void testDirectArgumentsAccessFallback() {
        JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) testHelper.runNoPolyglot("""
                        var target = {
                            apply(thisArg, args) {
                                delete args[1];
                                return args;
                            }
                        };
                        (function () { return target.apply(null, arguments); })(1, {});
                        """);
        assertEquals(0, arguments.getConnectedArgumentCount());
        assertEquals(2, JSObject.get(arguments, JSArgumentsArray.LENGTH));
        assertEquals(1, JSObject.get(arguments, 0));
        assertTrue(arguments.getDisconnectedIndices().isEmpty());
    }

    @Test
    public void testModifiedFunctionLength() {
        for (int length : new int[]{0, 10}) {
            assertConnectedArgumentCount("""
                            var f = function (a) { return arguments; };
                            Object.defineProperty(f, 'length', {value: %d});
                            f(1, 2);
                            """.formatted(length), 1, 2);
        }
    }

    @Test
    public void testDeleteExcessArgument() {
        assertExcessArgumentNotDisconnected("delete arguments[%d]");
    }

    @Test
    public void testRedefineExcessArgument() {
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%d', {get() { return 42; }})");
    }

    @Test
    public void testMakeExcessArgumentNonWritable() {
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%1$d', {writable: false}); delete arguments[%1$d]");
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%d', {value: 42, writable: false})");
    }

    @Test
    public void testLegacyFunctionArguments() {
        try (TestHelper helper = new TestHelper(newContextBuilder().option(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true"))) {
            JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) helper.runNoPolyglot("""
                            (function f(a) {
                                var args = f.arguments;
                                delete args[1];
                                return args;
                            })(1, {});
                            """);
            assertEquals(1, arguments.getConnectedArgumentCount());
            assertTrue(arguments.getDisconnectedIndices().isEmpty());
        }
    }

    @Test
    public void testDisconnectedMappedArgument() {
        JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) testHelper.runNoPolyglot("""
                        (function (a) {
                            delete arguments[0];
                            arguments.saved = a;
                            return arguments;
                        })({});
                        """);
        assertEquals(1, arguments.getDisconnectedIndices().size());
        assertSame(JSObject.get(arguments, Strings.constant("saved")), arguments.getDisconnectedIndices().get(0L));
    }

    private void assertConnectedArgumentCount(String source, int expectedConnectedCount, int expectedLength) {
        JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) testHelper.runNoPolyglot(source);
        assertEquals(expectedConnectedCount, arguments.getConnectedArgumentCount());
        assertEquals(expectedLength, JSObject.get(arguments, JSArgumentsArray.LENGTH));
    }

    private void assertExcessArgumentNotDisconnected(String operation) {
        for (int parameterCount = 0; parameterCount <= 1; parameterCount++) {
            String parameters = parameterCount == 0 ? "" : "a";
            String actualArguments = parameterCount == 0 ? "{}" : "1, {}";
            JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) testHelper.runNoPolyglot("""
                            (function (%s) {
                                %s;
                                return arguments;
                            })(%s);
                            """.formatted(parameters, operation.formatted(parameterCount), actualArguments));
            // The escaping arguments object must not retain the old value of an excess argument.
            assertTrue(arguments.getDisconnectedIndices().isEmpty());
            assertFalse(JSAbstractArgumentsArray.isIndexConnected(arguments, parameterCount));
            assertEquals(parameterCount + 1, JSObject.get(arguments, JSArgumentsArray.LENGTH));
            if (parameterCount != 0) {
                assertEquals(1, JSObject.get(arguments, 0));
            }
        }
    }
}
