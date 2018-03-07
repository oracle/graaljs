/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;

public class IncDecOperationTest extends FineGrainedAccessTest {

    @Test
    public void inc() {
        evalAllTags("var a = 42; a++;");

        assertGlobalVarDeclaration("a", 42);

        // Inc operation de-sugared to a = a + 1;
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertJSObjectInput);
            enter(BinaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, OPERATOR, "+");
                // read lhs global 'a'
                enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit(assertReturnValue(42));
                bin.input(42);
                // read rhs '1'
                enter(LiteralExpressionTag.class).exit(assertReturnValue(1));
                bin.input(1);
            }).exit();
            write.input(43);
        }).exit();
    }

    @Test
    public void dec() {
        evalAllTags("var a = 42; a--;");

        assertGlobalVarDeclaration("a", 42);

        // Dec operation de-sugared to tmp = tmp - 1;
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertJSObjectInput);
            enter(BinaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, OPERATOR, "-");
                // read lhs global 'a'
                enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit(assertReturnValue(42));
                bin.input(42);
                // read rhs '1'
                enter(LiteralExpressionTag.class).exit(assertReturnValue(1));
                bin.input(1);
            }).exit();
            write.input(41);
        }).exit();
    }

    @Test
    public void decProperty() {
        evalWithTag("var a = {x:42}; a.x--;", BinaryExpressionTag.class);

        enter(BinaryExpressionTag.class, (e, b) -> {
            assertAttribute(e, OPERATOR, "-");
            b.input(42);
            b.input(1);
        }).exit();
    }

    @Test
    public void incDecVar() {
        evalWithTag("function foo(a){var b = 0; b+=a.x;}; foo({x:42});", WriteVariableExpressionTag.class);

        enter(WriteVariableExpressionTag.class, (e, b) -> {
            assertAttribute(e, NAME, "b");
            b.input(0);
        }).exit();
        enter(WriteVariableExpressionTag.class, (e, b) -> {
            assertAttribute(e, NAME, "b");
            b.input(42);
        }).exit();
    }

}
