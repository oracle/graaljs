/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LiteralsTest extends FineGrainedAccessTest {

    protected void testLiteral(String src, LiteralExpressionTag.Type expectedTagType) {
        testLiteral(src, expectedTagType, null);
    }

    protected void testLiteral(String src, LiteralExpressionTag.Type expectedTagType, Object expectedValue) {
        evalAllTags(src);

        enter(WriteVariableExpressionTag.class, (e, var) -> {
            assertAttribute(e, NAME, "<return>");
            enter(WritePropertyExpressionTag.class, (e1, prop) -> {
                prop.input(assertJSObjectInput);
                assertAttribute(e1, KEY, "x");
                enter(LiteralExpressionTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, expectedTagType.name());
                }).exit();
                if (expectedValue != null) {
                    prop.input(expectedValue);
                } else {
                    prop.input();
                }
            }).exit();
            if (expectedValue != null) {
                var.input(expectedValue);
            } else {
                var.input();
            }
        }).exit();
    }

    @Test
    public void object() {
        testLiteral("x = {};", LiteralExpressionTag.Type.ObjectLiteral);
    }

    @Test
    public void array() {
        testLiteral("x = [];", LiteralExpressionTag.Type.ArrayLiteral);
    }

    @Test
    public void number() {
        testLiteral("x = 42;", LiteralExpressionTag.Type.NumericLiteral, 42);
    }

    @Test
    public void string() {
        testLiteral("x = \"foo\";", LiteralExpressionTag.Type.StringLiteral, "foo");
    }

    @Test
    public void bool() {
        testLiteral("x = true;", LiteralExpressionTag.Type.BooleanLiteral, true);
    }

    @Test
    public void nullLit() {
        testLiteral("x = null;", LiteralExpressionTag.Type.NullLiteral);
    }

    @Test
    public void undefined() {
        testLiteral("x = undefined;", LiteralExpressionTag.Type.UndefinedLiteral, Undefined.instance);
    }

    @Test
    public void regexp() {
        testLiteral("x = /\\w+/;", LiteralExpressionTag.Type.RegExpLiteral);
    }

    @Test
    public void function() {
        testLiteral("x = function foo(){};", LiteralExpressionTag.Type.FunctionLiteral);
    }

    @Test
    public void anonFunction() {
        testLiteral("x = () => {};", LiteralExpressionTag.Type.FunctionLiteral);
    }
}
