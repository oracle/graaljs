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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag.Type;

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
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                assertAttribute(e2, LITERAL_TYPE, Type.ArrayLiteral.name());
            }).exit();
            write.input(assertJSArrayInput);
        }).exit();

        enter(WriteElementTag.class, (e, write) -> {
            enter(ReadPropertyTag.class).input().exit();
            write.input(assertJSArrayInput);
            enter(LiteralTag.class).exit(assertReturnValue(0));
            write.input(0);
            enter(BinaryOperationTag.class, (e1, bin) -> {
                assertAttribute(e1, "operator", "+");

                enter(ReadElementTag.class, (e2, p) -> {
                    p.input(assertJSArrayInput);
                    p.input(0);
                }).exit();
                bin.input(42);

                enter(LiteralTag.class).exit(assertReturnValue(1));
                bin.input(1);
            }).exit(assertReturnValue(43));
            write.input(43);
        }).exit(assertReturnValue(43));
    }

    @Test
    public void assignExpr() {
        evalWithTags("var a = 42;", new Class[]{WritePropertyTag.class, LiteralTag.class});

        enter(WritePropertyTag.class, (e, write) -> {
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit(assertReturnValue(42));
            write.input(42);
        }).exit();
    }

    private void assertDesugaredOperation(String operation, String desugaredOperator, int initial, int operand, int result) {
        evalAllTags("var a = " + initial + "; a " + operation + " " + operand + ";");

        assertGlobalVarDeclaration("a", initial);

        // '+=' operation de-sugared to e.g. 'a = a + 3';
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(BinaryOperationTag.class, (e1, bin) -> {
                assertAttribute(e1, "operator", desugaredOperator);

                enter(ReadPropertyTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit();
                bin.input(initial);

                enter(LiteralTag.class).exit(assertReturnValue(operand));
                bin.input(operand);
            }).exit(assertReturnValue(result));
            write.input(result);
        }).exit();
    }
}
