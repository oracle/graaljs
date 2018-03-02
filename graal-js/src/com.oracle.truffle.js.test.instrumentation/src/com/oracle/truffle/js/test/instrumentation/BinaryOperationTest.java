/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;

public class BinaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void fullAnd() {
        evalAllTags("var a = 42; var b = 43; var c = a + b;");

        assertEngineInit();

        // var a = 42
        assertGlobalVarDeclaration("a", 42);
        // var a = 43
        assertGlobalVarDeclaration("b", 43);
        // var c = a + b;
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "c");
            write.input(assertGlobalObjectInput);

            enter(BinaryExpressionTag.class, (e2, binary) -> {
                enter(ReadPropertyExpressionTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "a");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                binary.input(42);

                enter(ReadPropertyExpressionTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "b");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                binary.input(43);

            }).exit();

            write.input(85);
        }).exit();
    }

    @Test
    public void leftConstantAnd() {
        String src = "var a = 42; var c = 43 + a;";

        evalWithTag(src, BinaryExpressionTag.class);

        enter(BinaryExpressionTag.class, (e2, binary) -> {
            binary.input(43);
            binary.input(42);
        }).exit();
    }

    @Test
    public void rightConstantPlus() {
        constantBinaryOperationTestRight(43, "+");
    }

    @Test
    public void rightConstantAnd() {
        constantBinaryOperationTestRight(43, "&");
    }

    @Test
    public void rightConstantOr() {
        constantBinaryOperationTestRight(43, "|");
    }

    @Test
    public void rightConstantXor() {
        constantBinaryOperationTestRight(43, "^");
    }

    @Test
    public void rightConstantLeftShift() {
        constantBinaryOperationTestRight(43, "<<");
    }

    @Test
    public void rightConstantRightShift() {
        constantBinaryOperationTestRight(43, ">>");
    }

    @Test
    public void rightConstantUnsignedRightShift() {
        constantBinaryOperationTestRight(43, ">>>");
    }

    @Test
    public void plus() {
        binaryOperationTest(42, 43, "+");
    }

    @Test
    public void minus() {
        binaryOperationTest(42, 43, "-");
    }

    @Test
    public void mult() {
        binaryOperationTest(42, 43, "*");
    }

    @Test
    public void div() {
        binaryOperationTest(42, 43, "/");
    }

    @Test
    public void and() {
        binaryOperationTest(42, 43, "&");
    }

    @Test
    public void or() {
        binaryOperationTest(42, 43, "|");
    }

    @Test
    public void xor() {
        binaryOperationTest(42, 43, "^");
    }

    @Test
    public void leftShift() {
        binaryOperationTest(42, 43, "<<");
    }

    @Test
    public void rightShift() {
        binaryOperationTest(42, 43, ">>");
    }

    @Test
    public void unsignedRightShift() {
        binaryOperationTest(42, 43, ">>>");
    }

    @Test
    public void fakeUnary() {
        String src = "var a = 2 != 1";

        evalWithTags(src, new Class[]{BinaryExpressionTag.class, UnaryExpressionTag.class});

        enter(UnaryExpressionTag.class, (e, u) -> {
            assertAttribute(e, OPERATOR, "!");
            enter(BinaryExpressionTag.class, (e2, b) -> {
                assertAttribute(e2, OPERATOR, "==");
                b.input(2);
                b.input(1);
            }).exit();
            u.input(false);
        }).exit();
    }

    private void binaryOperationTest(int leftValue, int rightValue, String operator) {
        String src = "var a = " + leftValue + " ; var b = a " + operator + " " + rightValue + ";";

        evalWithTag(src, BinaryExpressionTag.class);

        // we assign the left value to another var to avoid optimizations
        enter(BinaryExpressionTag.class, (e, binary) -> {
            assertAttribute(e, OPERATOR, operator);
            binary.input(leftValue);
            binary.input(rightValue);
        }).exit();
    }

    private void constantBinaryOperationTestRight(int rightValue, String operator) {
        String src = "var a = 42; var c = a " + operator + " " + rightValue + ";";

        evalWithTag(src, BinaryExpressionTag.class);

        enter(BinaryExpressionTag.class, (e, binary) -> {
            assertAttribute(e, OPERATOR, operator);
            binary.input(42);
            binary.input(rightValue);
        }).exit();
    }

}
