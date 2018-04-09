/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class ObjectAllocationTest extends FineGrainedAccessTest {

    @Test
    public void basic() {
        evalAllTags("var a = new Object(); var b = {}; var c = [];");

        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);

            enter(ObjectAllocationExpressionTag.class, (e1, alloc) -> {
                enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
                alloc.input((e2) -> {
                    assertTrue(JSFunction.isJSFunction(e2.val));
                });
                // TODO missing input event for arguments to ObjectAllocationTag
                enter(BuiltinRootTag.class, (e2) -> {
                    assertAttribute(e2, NAME, "Object");
                }).exit();
            }).exit();
            write.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(WritePropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "b");
            prop.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(WritePropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "c");
            prop.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class).exit();
            prop.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
        }).exit();
    }

    @Test
    public void nested() {
        evalAllTags("var a = {x:{}}; var b = [[]]; var c = {x:[]}");

        enter(WritePropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "a");
            prop.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralExpressionTag.Type.ObjectLiteral.name());
                enter(LiteralExpressionTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.ObjectLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSObject.isDynamicObject(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(WritePropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "b");
            prop.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralExpressionTag.Type.ArrayLiteral.name());
                enter(LiteralExpressionTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.ArrayLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
        }).exit();

        enter(WritePropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "c");
            prop.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralExpressionTag.Type.ObjectLiteral.name());
                enter(LiteralExpressionTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.ArrayLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();
    }

}
