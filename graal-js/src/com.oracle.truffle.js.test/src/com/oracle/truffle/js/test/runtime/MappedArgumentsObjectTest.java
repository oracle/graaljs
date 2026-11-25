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
        assertFastArguments(arguments);
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
        assertExcessArgumentNotDisconnected("delete arguments[%d]", true);
        assertTrue(testHelper.getJSContext().getFastArgumentsObjectAssumption().isValid());
    }

    @Test
    public void testDeleteAbsentArgument() {
        for (String strict : new String[]{"", "'use strict';"}) {
            JSArgumentsObject arguments = (JSArgumentsObject) testHelper.runNoPolyglot("""
                            (function (a, b) {
                                %s
                                if (!delete arguments[1] || !delete arguments[100]) {
                                    throw new Error('deleting an absent argument failed');
                                }
                                b = 42;
                                arguments.saved = b;
                                return arguments;
                            })(1);
                            """.formatted(strict));
            assertEquals(42, JSObject.get(arguments, Strings.constant("saved")));
            assertFalse(JSObject.hasOwnProperty(arguments, 1));
            assertFalse(JSArgumentsArray.INSTANCE.delete(arguments, 100L, false, false));
            assertFastArguments(arguments);
        }
        assertTrue(testHelper.getJSContext().getFastArgumentsObjectAssumption().isValid());
    }

    @Test
    public void testRejectedDeleteKeepsFastArguments() {
        for (String operation : new String[]{"seal", "freeze"}) {
            String parameters = operation.equals("seal") ? "a" : "";
            JSArgumentsObject.Mapped arguments = (JSArgumentsObject.Mapped) testHelper.runNoPolyglot("""
                            (function (%s) {
                                Object.%s(arguments);
                                for (var index of [0, 1]) {
                                    if (Reflect.deleteProperty(arguments, index)) {
                                        throw new Error('deleting a sealed argument succeeded');
                                    }
                                    try {
                                        (function (args, i) { 'use strict'; delete args[i]; })(arguments, index);
                                        throw new Error('strict deletion should have thrown');
                                    } catch (e) {
                                        if (!(e instanceof TypeError)) throw e;
                                    }
                                }
                                return arguments;
                            })(1, 2);
                            """.formatted(parameters, operation));
            assertEquals(1, JSObject.get(arguments, 0));
            assertEquals(2, JSObject.get(arguments, 1));
            assertEquals(2, JSObject.get(arguments, JSArgumentsArray.LENGTH));
            assertFastArguments(arguments);
        }
        assertTrue(testHelper.getJSContext().getFastArgumentsObjectAssumption().isValid());
    }

    @Test
    public void testNonArrayIndexPropertiesKeepFastArguments() {
        for (String strict : new String[]{"", "'use strict';"}) {
            for (String key : new String[]{"4294967295", "4294967296", "9999999999"}) {
                for (boolean numericDelete : new boolean[]{false, true}) {
                    JSArgumentsObject arguments = (JSArgumentsObject) testHelper.runNoPolyglot("""
                                    (function (a) {
                                        %s
                                        Object.defineProperty(arguments, '%s', {get() { return 42; }, configurable: true});
                                        return arguments;
                                    })(1);
                                    """.formatted(strict, key));
                    assertFastArguments(arguments);
                    assertEquals(42, JSObject.get(arguments, Strings.fromJavaString(key)));
                    assertTrue(JSObject.getOwnProperty(arguments, Strings.fromJavaString(key)).isAccessorDescriptor());
                    assertEquals(1, JSObject.get(arguments, JSArgumentsArray.LENGTH));
                    if (numericDelete) {
                        assertTrue(JSObject.delete(arguments, Long.parseLong(key), true));
                    } else {
                        assertTrue(JSObject.delete(arguments, Strings.fromJavaString(key), true));
                    }
                    assertFalse(JSObject.hasOwnProperty(arguments, Strings.fromJavaString(key)));
                    assertFastArguments(arguments);
                }
            }
        }
        assertTrue(testHelper.getJSContext().getFastArgumentsObjectAssumption().isValid());
    }

    @Test
    public void testLastArrayIndexStillNeedsSlowArguments() {
        JSArgumentsObject arguments = (JSArgumentsObject) testHelper.runNoPolyglot("""
                        (function () {
                            Object.defineProperty(arguments, '4294967294', {get() { return 42; }, configurable: true});
                            return arguments;
                        })();
                        """);
        assertFalse(JSArgumentsArray.isJSFastArgumentsObject(arguments));
        assertEquals(42, JSObject.get(arguments, Strings.constant("4294967294")));
        assertTrue(JSObject.delete(arguments, Strings.constant("4294967294"), true));
        assertFalse(JSObject.hasOwnProperty(arguments, Strings.constant("4294967294")));
    }

    @Test
    public void testRedefineExcessArgument() {
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%d', {get() { return 42; }})", false);
    }

    @Test
    public void testRedefineAccessorDoesNotCallGetter() {
        for (String strict : new String[]{"", "'use strict';"}) {
            for (String key : new String[]{"0", "1"}) {
                assertEquals(0, testHelper.runNoPolyglot("""
                                var calls = 0;
                                (function (a) {
                                    %1$s
                                    const getter = function () { calls++; };
                                    Object.defineProperty(arguments, '%2$s', {get: getter, configurable: true});
                                    Object.defineProperty(arguments, '%2$s', {enumerable: false});
                                    const desc = Object.getOwnPropertyDescriptor(arguments, '%2$s');
                                    if (desc.get !== getter || desc.enumerable || a !== 0) {
                                        throw new Error('redefining an arguments accessor changed its getter or parameter');
                                    }
                                })(0, 1);
                                calls;
                                """.formatted(strict, key)));
            }
        }
    }

    @Test
    public void testDefinePropertyDoesNotCallInheritedGetter() {
        for (String strict : new String[]{"", "'use strict';"}) {
            assertEquals(0, testHelper.runNoPolyglot("""
                            var calls = 0;
                            (function (a) {
                                %s
                                delete arguments[0];
                                const proto = Object.create(Object.getPrototypeOf(arguments), {
                                    0: {get() { calls++; }}
                                });
                                Object.setPrototypeOf(arguments, proto);
                                Object.defineProperty(arguments, '0', {value: 42});
                                if (arguments[0] !== 42 || a !== 0) {
                                    throw new Error('defining an arguments property changed its value or parameter');
                                }
                            })(0);
                            calls;
                            """.formatted(strict)));
        }
    }

    @Test
    public void testRedefineSealedArgument() {
        for (String strict : new String[]{"", "'use strict';"}) {
            assertEquals(true, testHelper.runNoPolyglot("""
                            (function (a, b) {
                                %s
                                const mapped = %s;
                                Object.seal(arguments);
                                if (Reflect.defineProperty(arguments, '0', {get() { return 42; }})) {
                                    throw new Error('replaced a sealed argument with an accessor');
                                }
                                a = 2;
                                if (arguments[0] !== (mapped ? 2 : 1)) {
                                    throw new Error('rejected definition changed the parameter mapping');
                                }
                                Object.defineProperty(arguments, '0', {value: 3});
                                const updated = Object.getOwnPropertyDescriptor(arguments, '0');
                                if (arguments[0] !== 3 || a !== (mapped ? 3 : 2) || updated.value !== 3 ||
                                                !updated.writable || !updated.enumerable || updated.configurable) {
                                    throw new Error('updating a sealed argument changed its attributes or failed to update its value');
                                }
                                a = 4;
                                Object.defineProperty(arguments, '0', {writable: false});
                                a = 5;
                                const locked = Object.getOwnPropertyDescriptor(arguments, '0');
                                Object.defineProperty(arguments, '1', {value: 8, writable: false});
                                if (b !== (mapped ? 8 : 2)) {
                                    throw new Error('making a sealed argument non-writable failed to update its mapped parameter');
                                }
                                b = 9;
                                return arguments[0] === (mapped ? 4 : 3) && locked.value === arguments[0] &&
                                                !locked.writable && !Reflect.set(arguments, '0', 6) &&
                                                Reflect.ownKeys(arguments).filter(key => key === '0').length === 1 &&
                                                arguments[1] === 8;
                            })(1, 2);
                            """.formatted(strict, strict.isEmpty())));
        }
    }

    @Test
    public void testRedefineFrozenArgument() {
        for (String strict : new String[]{"", "'use strict';"}) {
            assertEquals(true, testHelper.runNoPolyglot("""
                            (function (a) {
                                %s
                                Object.freeze(arguments);
                                return !Reflect.defineProperty(arguments, '0', {value: 2}) &&
                                                Reflect.defineProperty(arguments, '0', {value: 1}) &&
                                                !Reflect.set(arguments, '0', 2) && arguments[0] === 1 &&
                                                Reflect.ownKeys(arguments).filter(key => key === '0').length === 1;
                            })(1);
                            """.formatted(strict)));
        }
    }

    @Test
    public void testFreezeDisconnectsMappedArguments() {
        testHelper.runNoPolyglot("""
                        (function (a, b, c) {
                            Object.defineProperty(arguments, '0', {enumerable: false});
                            Object.defineProperty(arguments, '1', {get() { throw new Error('called getter'); }});
                            a = 4;
                            b = 5;
                            c = 6;
                            Object.freeze(arguments);
                            if (a !== 4 || b !== 5 || c !== 6) {
                                throw new Error('freezing changed a parameter value');
                            }
                            a = 7;
                            b = 8;
                            c = 9;
                            if (!Object.isFrozen(arguments) || arguments[0] !== 4 || arguments[2] !== 6 || a !== 7 || b !== 8 || c !== 9) {
                                throw new Error('freezing failed to disconnect a parameter mapping');
                            }
                        })(1, 2, 3);
                        """);
    }

    @Test
    public void testCallerWithStrictFunctionValue() {
        assertEquals(42, testHelper.runNoPolyglot("""
                        (function () {
                            const strictFunction = function () { 'use strict'; };
                            Object.defineProperty(arguments, 'caller', {value: strictFunction, configurable: true});
                            if (Object.getOwnPropertyDescriptor(arguments, 'caller').value !== strictFunction) {
                                throw new Error('caller descriptor did not preserve the strict function');
                            }
                            Object.defineProperty(arguments, 'caller', {value: 42});
                            return arguments.caller;
                        })();
                        """));
    }

    @Test
    public void testMakeExcessArgumentNonWritable() {
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%1$d', {writable: false}); delete arguments[%1$d]", false);
        assertExcessArgumentNotDisconnected("Object.defineProperty(arguments, '%d', {value: 42, writable: false})", false);
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
            assertFastArguments(arguments);
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

    private static void assertFastArguments(JSArgumentsObject arguments) {
        assertTrue(JSArgumentsArray.isJSFastArgumentsObject(arguments));
        assertFalse(JSAbstractArgumentsArray.hasDisconnectedIndices(arguments));
    }

    private void assertExcessArgumentNotDisconnected(String operation, boolean staysFast) {
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
            if (staysFast) {
                assertFastArguments(arguments);
                assertFalse(JSObject.hasOwnProperty(arguments, parameterCount));
            } else {
                assertTrue(arguments.getDisconnectedIndices().isEmpty());
            }
            assertFalse(JSAbstractArgumentsArray.isIndexConnected(arguments, parameterCount));
            assertEquals(parameterCount + 1, JSObject.get(arguments, JSArgumentsArray.LENGTH));
            if (parameterCount != 0) {
                assertEquals(1, JSObject.get(arguments, 0));
            }
        }
    }
}
