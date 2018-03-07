/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag.Type;

public class AssignmentExpressions extends FineGrainedAccessTest {

    @Test
    public void plusEqual() {
        assertDesugaredOperation("+=", "+", 39, 3, 42);
    }

    @Test
    public void minEqual() {
        assertDesugaredOperation("-=", "-", 45, 3, 42);
    }

    @Test
    public void mulEqual() {
        assertDesugaredOperation("*=", "*", 21, 2, 42);
    }

    @Test
    public void divEqual() {
        assertDesugaredOperation("/=", "/", 84, 2, 42);
    }

    @Test
    public void andEqual() {
        assertDesugaredOperation("&=", "&", 42, 42, 42);
    }

    @Test
    public void orEqual() {
        assertDesugaredOperation("^=", "^", 42, 0, 42);
    }

    @Test
    public void lshiftEqual() {
        assertDesugaredOperation("<<=", "<<", 42, 0, 42);
    }

    @Test
    public void rshiftEqual() {
        assertDesugaredOperation(">>=", ">>", 42, 0, 42);
    }

    @Test
    public void plusEqualElem() {
        evalAllTags("var a = [42]; a[0] += 1;");

        // var a = [42];
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e2) -> {
                assertAttribute(e2, TYPE, Type.ArrayLiteral.name());
            }).exit();
            write.input(assertJSArrayInput);
        }).exit();

        enter(ReadPropertyExpressionTag.class).input().exit();

        enter(WriteElementExpressionTag.class, (e, write) -> {
            write.input(assertJSArrayInput);
            enter(LiteralExpressionTag.class).exit(assertReturnValue(0));
            write.input(0);
            enter(BinaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, "operator", "+");

                enter(ReadElementExpressionTag.class, (e2, p) -> {
                    p.input(assertJSArrayInput);
                    // TODO(db) should be int
                    p.input(0L);
                }).exit();
                bin.input(42);

                enter(LiteralExpressionTag.class).exit(assertReturnValue(1));
                bin.input(1);
            }).exit(assertReturnValue(43));
            write.input(43);
        }).exit(assertReturnValue(43));
    }

    private void assertDesugaredOperation(String operation, String desugaredOperator, int initial, int operand, int result) {
        evalAllTags("var a = " + initial + "; a " + operation + " " + operand + ";");

        assertGlobalVarDeclaration("a", initial);

        // '+=' operation de-sugared to e.g. 'a = a + 3';
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(BinaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, "operator", desugaredOperator);

                enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit();
                bin.input(initial);

                enter(LiteralExpressionTag.class).exit(assertReturnValue(operand));
                bin.input(operand);
            }).exit(assertReturnValue(result));
            write.input(result);
        }).exit();
    }
}
