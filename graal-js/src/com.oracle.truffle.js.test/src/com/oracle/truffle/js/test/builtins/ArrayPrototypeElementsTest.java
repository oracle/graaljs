/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.js.test.TestHelper;

public class ArrayPrototypeElementsTest {

    @Test
    public void testAddArrayPrototypeElement() {
        for (var allocate : new String[]{"""
                        var myArray = new Array();
                        """, """
                        class MyArray extends Array {}
                        var myArray = new MyArray();
                        """, """
                        class MyArray extends Array {}
                        class OhMyArray extends MyArray {}
                        var myArray = new OhMyArray();
                        """, """
                        function MyArray() {
                            return Reflect.construct(Array, arguments, MyArray);
                        }
                        MyArray.prototype = {
                            __proto__: Array.prototype,
                            constructor: MyArray,
                        };
                        Object.setPrototypeOf(MyArray, Array);
                        var myArray = new MyArray();
                        """, """
                        function MyArray() {
                            return Reflect.construct(Array, arguments, Object.getPrototypeOf(this).constructor);
                        }
                        MyArray.prototype = Object.create(Array.prototype, {
                            constructor: { value: MyArray, configurable: true, writable: true }
                        });
                        Object.setPrototypeOf(MyArray, Array);
                        var myArray = new MyArray();
                        """,
        }) {
            for (var invalidate : new String[]{
                            "OhMyArray.prototype[0] = 'oh no';",
                            "MyArray.prototype[0] = 'oh no';",
                            "Array.prototype[0] = 'oh no';",
                            "Object.prototype[0] = 'oh no';",
                            "OhMyArray.prototype['0'] = 'oh no';",
                            "MyArray.prototype['0'] = 'oh no';",
                            "Array.prototype['0'] = 'oh no';",
                            "Object.prototype['0'] = 'oh no';",
                            "Reflect.set(OhMyArray.prototype, '0', 'oh no');",
                            "Reflect.set(MyArray.prototype, '0', 'oh no');",
                            "Reflect.set(Array.prototype, '0', 'oh no');",
                            "Reflect.set(Object.prototype, '0', 'oh no');",
                            "Object.defineProperty(OhMyArray.prototype, '0', {value: 'oh no', writable: true, configurable: true});",
                            "Object.defineProperty(MyArray.prototype, '0', {value: 'oh no', writable: true, configurable: true});",
                            "Object.defineProperty(Array.prototype, '0', {value: 'oh no', writable: true, configurable: true});",
                            "Object.defineProperty(Object.prototype, '0', {value: 'oh no', writable: true, configurable: true});",
                            "Object.defineProperty(OhMyArray.prototype, '0', {get: () => 'oh no', configurable: true});",
                            "Object.defineProperty(MyArray.prototype, '0', {get: () => 'oh no', configurable: true});",
                            "Object.defineProperty(Array.prototype, '0', {get: () => 'oh no', configurable: true});",
                            "Object.defineProperty(Object.prototype, '0', {get: () => 'oh no', configurable: true});",
            }) {
                // skip not applicable test cases
                if (invalidate.contains("OhMyArray") && !allocate.contains("OhMyArray")) {
                    continue;
                } else if (invalidate.contains("MyArray") && !allocate.contains("MyArray")) {
                    continue;
                }

                try (var helper = new TestHelper()) {
                    var context = helper.getPolyglotContext();

                    context.eval(ID, allocate);

                    Value element;
                    element = context.eval(ID, "myArray[0]");
                    assertTrue(element.isNull());

                    Assumption noElementsAssumption = helper.getJSContext().getArrayPrototypeNoElementsAssumption();
                    assertTrue(noElementsAssumption.isValid());

                    context.eval(ID, invalidate);

                    assertFalse(noElementsAssumption.isValid());

                    element = context.eval(ID, "myArray[0]");
                    assertEquals("oh no", element.asString());
                }
            }
        }
    }

    @Test
    public void testSetPrototypeToNull() {
        for (var testCase : new String[]{
                        "Object.setPrototypeOf(myArray, null)",
                        "Object.setPrototypeOf(Array.prototype, null)",
                        "Object.setPrototypeOf(MyArray.prototype, null)",
                        "Object.setPrototypeOf(OhMyArray.prototype, null)",
        }) {
            checkSetPrototype(testCase, true);
        }
    }

    @Test
    public void testSetPrototypeToAnArrayPrototype() {
        for (var testCase : new String[]{
                        "Object.setPrototypeOf(MyArray.prototype, Array.prototype)",
                        "Object.setPrototypeOf(OhMyArray.prototype, Array.prototype)",
                        "Object.setPrototypeOf(MyArray.prototype, (class extends Array {}).prototype)",
                        "Object.setPrototypeOf(OhMyArray.prototype, (class extends Array {}).prototype)",
                        "Object.setPrototypeOf(myArray, MyArray.prototype)",
                        "Object.setPrototypeOf(myArray, Array.prototype)",
        }) {
            checkSetPrototype(testCase, true);
        }
    }

    @Test
    public void testSetPrototypeToObjectPrototype() {
        for (var testCase : new String[]{
                        "Object.setPrototypeOf(myArray, Object.prototype)",
                        "Object.setPrototypeOf(MyArray.prototype, Object.prototype)",
                        "Object.setPrototypeOf(OhMyArray.prototype, Object.prototype)",
        }) {
            checkSetPrototype(testCase, true);
        }
    }

    @Test
    public void testSetPrototypeToDifferentObject() {
        for (var testCase : new String[]{
                        "Object.setPrototypeOf(myArray, {})",
                        "Object.setPrototypeOf(Array.prototype, {})",
                        "Object.setPrototypeOf(MyArray.prototype, {})",
                        "Object.setPrototypeOf(OhMyArray.prototype, {})",
                        "Object.setPrototypeOf(myArray, [])",
                        "Object.setPrototypeOf(MyArray.prototype, [])",
                        "Object.setPrototypeOf(OhMyArray.prototype, [])",
        }) {
            checkSetPrototype(testCase, false);
        }
    }

    private static void checkSetPrototype(String testCase, boolean expectValid) {
        try (var helper = new TestHelper()) {
            var context = helper.getPolyglotContext();

            context.eval(ID, """
                            class MyArray extends Array {}
                            class OhMyArray extends MyArray {}
                            var myArray = new OhMyArray();
                            """);

            Assumption noElementsAssumption = helper.getJSContext().getArrayPrototypeNoElementsAssumption();
            assertTrue(noElementsAssumption.isValid());

            context.eval(ID, testCase);

            assertEquals(expectValid, noElementsAssumption.isValid());
        }
    }
}
