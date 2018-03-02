/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class ElementsAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        evalAllTags("var a = [1]; a[0];");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");

        enter(ReadElementExpressionTag.class, (e, elem) -> {
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralExpressionTag.class).exit();
            elem.input(0);
        }).exit();
    }

    @Test
    public void nestedRead() {
        evalAllTags("var a = [0]; a[a[0]];");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");

        enter(ReadElementExpressionTag.class, (e, elem) -> {
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            // nested read a[0]
            enter(ReadElementExpressionTag.class, (e1, elem1) -> {
                enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
                elem1.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
                enter(LiteralExpressionTag.class).exit();
                elem1.input(0);
            }).exit();
            // outer read
            elem.input(0);
        }).exit();
    }

    @Test
    public void write() {
        evalAllTags("var a = []; a[1] = 'foo';");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");
        // write element
        enter(WriteElementExpressionTag.class, (e, elem) -> {
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralExpressionTag.class).exit();
            elem.input(1);
            enter(LiteralExpressionTag.class).exit();
            elem.input("foo");
        }).exit();
    }

    @Test
    public void elementWriteIncDec() {
        evalWithTag("var u=[2,4,6]; var p = 1; u[p] -= 42", WriteElementExpressionTag.class);

        enter(WriteElementExpressionTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(1);
            b.input(-38);
        }).exit();
    }

    @Test
    public void elementReadInvoke() {
        evalWithTag("var u={x:[function(){}]}; u.x[0]()", ReadElementExpressionTag.class);

        enter(ReadElementExpressionTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(0);
        }).exit((res) -> {
            Object[] val = (Object[]) res.val;
            // # of input events + result
            assertEquals(3, val.length);
            // result
            assertTrue(JSFunction.isJSFunction(val[0]));
            // target
            assertTrue(JSArray.isJSArray(val[1]));
            // idx
            assertEquals(0, val[2]);
        });
    }

    @Test
    public void targetTest() {
        evalWithTag("var Box2d = {};Box2d.postDefs = [function(){}];function test(){var i = 0;Box2d.postDefs[i]();}; test();", ReadElementExpressionTag.class);

        enter(ReadElementExpressionTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(0);
        }).exit();
    }

}
