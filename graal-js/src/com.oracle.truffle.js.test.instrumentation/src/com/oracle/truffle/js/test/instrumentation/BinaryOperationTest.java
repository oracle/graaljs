/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class BinaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void fullAnd() {
        evalAllTags("var a = 42; var b = 43; var c = a + b;");

        // var a = 42
        assertGlobalVarDeclaration("a", 42);
        // var a = 43
        assertGlobalVarDeclaration("b", 43);
        // var c = a + b;
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "c");
            write.input(assertGlobalObjectInput);

            enter(BinaryOperationTag.class, (e2, binary) -> {
                enter(ReadPropertyTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "a");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                binary.input(42);

                enter(ReadPropertyTag.class, (e3, prop) -> {
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

        evalWithTag(src, BinaryOperationTag.class);

        enter(BinaryOperationTag.class, (e2, binary) -> {
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
    public void eq() {
        binaryOperationTest(42, 43, "==");
    }

    @Test
    public void strictEq() {
        binaryOperationTest(42, 43, "===");
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
    public void desugaredBinaryNeq() {
        binaryOperationTest("!=", "!", "==");
    }

    @Test
    public void desugaredBinarystrictNeq() {
        binaryOperationTest("!==", "!", "===");
    }

    @Test
    public void undefinedEqNullLeft() {
        testBinExpOnly("var foo = undefined; foo == null;",
                        Undefined.instance,
                        Null.instance);
    }

    @Test
    public void nullEqUndefinedLeft() {
        testBinExpOnly("var foo = null; foo == undefined;",
                        Null.instance,
                        Undefined.instance);
    }

    @Test
    public void nullEqUndefinedRight() {
        testBinExpOnly("var foo = null; undefined == foo;",
                        Undefined.instance,
                        Null.instance);
    }

    @Test
    public void undefinedEqNullRight() {
        testBinExpOnly("var foo = undefined; null == foo;",
                        Null.instance,
                        Undefined.instance);
    }

    @Test
    public void undefinedEqUndefined() {
        testBinExpOnly("var foo = undefined; undefined == foo;",
                        Undefined.instance,
                        Undefined.instance);
    }

    @Test
    public void nullEqNull() {
        testBinExpOnly("var foo = null; null == foo;",
                        Null.instance,
                        Null.instance);
    }

    @Test
    public void typeofIdenticalString1() {
        typeofIdenticalType("string", "==", true);
    }

    @Test
    public void typeofIdenticalString2() {
        typeofIdenticalType("string", "==", false);
    }

    @Test
    public void typeofIdenticalString3() {
        typeofIdenticalType("string", "===", true);
    }

    @Test
    public void typeofIdenticalString4() {
        typeofIdenticalType("string", "===", false);
    }

    @Test
    public void typeofIdenticalUnknown1() {
        typeofIdenticalType("foo", "==", true);
    }

    @Test
    public void typeofIdenticalUnknown2() {
        typeofIdenticalType("foo", "==", false);
    }

    @Test
    public void typeofIdenticalUnknown3() {
        typeofIdenticalType("foo", "===", true);
    }

    public void typeofIdenticalUnknown4() {
        typeofIdenticalType("foo", "===", false);
    }

    public void typeofIdenticalType(String type, String operator, boolean typeofAsLeftOperand) {
        String lhs = "'" + type + "'";
        String rhs = "(typeof a)";

        if (typeofAsLeftOperand) {
            String tmp = lhs;
            lhs = rhs;
            rhs = tmp;
        }

        String src = "var a = 42; var c = " + lhs + operator + rhs;

        evalWithTags(src, new Class[]{BinaryOperationTag.class, UnaryOperationTag.class, LiteralTag.class});

        enter(LiteralTag.class).exit(assertReturnValue(42));

        enter(BinaryOperationTag.class, (e, binary) -> {
            assertAttribute(e, OPERATOR, operator);
            for (int eventNo = 0; eventNo < 2; eventNo++) {
                if (typeofAsLeftOperand == (eventNo == 0)) {
                    enter(UnaryOperationTag.class, (e2, unary) -> {
                        assertAttribute(e2, OPERATOR, "typeof");
                        unary.input(42);
                    }).exit();
                    binary.input("number");
                } else {
                    enter(LiteralTag.class).exit(assertReturnValue(type));
                    binary.input(type);
                }
            }
        }).exit();
    }

    private void testBinExpOnly(String src, Object firstValue, Object secondValue) {
        evalWithTag(src, BinaryOperationTag.class);
        enter(BinaryOperationTag.class, (e, binary) -> {
            binary.input(firstValue);
            binary.input(secondValue);
        }).exit();
    }

    private void binaryOperationTest(String srcOperator, String unOperator, String binOperator) {
        String src = "var a = 42 " + srcOperator + " 41";

        evalWithTags(src, new Class[]{BinaryOperationTag.class, UnaryOperationTag.class});

        enter(UnaryOperationTag.class, (e, u) -> {
            assertAttribute(e, OPERATOR, unOperator);
            enter(BinaryOperationTag.class, (e2, b) -> {
                assertAttribute(e2, OPERATOR, binOperator);
                b.input(42);
                b.input(41);
            }).exit();
            u.input(false);
        }).exit();
    }

    private void binaryOperationTest(int leftValue, int rightValue, String operator) {
        String src = "var a = " + leftValue + " ; var b = a " + operator + " " + rightValue + ";";

        evalWithTag(src, BinaryOperationTag.class);

        // we assign the left value to another var to avoid optimizations
        enter(BinaryOperationTag.class, (e, binary) -> {
            assertAttribute(e, OPERATOR, operator);
            binary.input(leftValue);
            binary.input(rightValue);
        }).exit();
    }

    private void constantBinaryOperationTestRight(int rightValue, String operator) {
        String src = "var a = 42; var c = a " + operator + " " + rightValue + ";";

        evalWithTag(src, BinaryOperationTag.class);

        enter(BinaryOperationTag.class, (e, binary) -> {
            assertAttribute(e, OPERATOR, operator);
            binary.input(42);
            binary.input(rightValue);
        }).exit();
    }

}
