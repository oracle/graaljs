/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;

public class PrimaryExpressions extends FineGrainedAccessTest {

    @Test
    public void literalUnd() {
        evalWithTag("x = undefined;", LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.UndefinedLiteral.name());
        }).exit();
    }

    @Test
    public void literalObj() {
        evalWithTag("x = {};", LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.ObjectLiteral.name());
        }).exit();
    }

    @Test
    public void literalArray() {
        evalWithTag("x = [];", LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.ArrayLiteral.name());
        }).exit();
    }

    @Test
    public void functionExpression() {
        String src = "x = function() {}";

        evalWithTag(src, LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Ignore
    @Test
    public void classExpression() {
        String src = "x = class A {}";

        evalWithTag(src, LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Test
    public void generatorExpression() {
        String src = "x = function* foo() {}";

        evalWithTag(src, LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Test
    public void asyncFunctionExpression() {
        String src = "x = async function foo() {}";

        evalWithTag(src, LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Test
    public void coverParentheszed() {
        String src = "x = (async function foo() {}, undefined)";

        evalWithTag(src, LiteralExpressionTag.class);

        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
        }).exit();
        enter(LiteralExpressionTag.class, (l) -> {
            assertAttribute(l, TYPE, LiteralExpressionTag.Type.UndefinedLiteral.name());
        }).exit();
    }

    @Test
    public void thiz() {
        String src = "(function foo() { this.x = 3 })();";

        evalWithTag(src, WritePropertyExpressionTag.class);

        // actual test
        enter(WritePropertyExpressionTag.class, (e, w) -> {
            assertAttribute(e, KEY, "x");
            w.input(assertJSObjectInput);
            w.input(3);
        }).exit();
    }

}
