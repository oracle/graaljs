/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
