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

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class IncDecOperationTest extends FineGrainedAccessTest {

    @Test
    public void inc() {
        evalAllTags("var a = 42; a++;");

        assertGlobalVarDeclaration("a", 42);

        // Inc operation de-sugared to a = a + 1;
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertJSObjectInput);
            enter(UnaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, OPERATOR, "++");
                // read lhs global 'a'
                enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit(assertReturnValue(42));
                bin.input(42);
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
            enter(UnaryExpressionTag.class, (e1, bin) -> {
                assertAttribute(e1, OPERATOR, "--");
                // read lhs global 'a'
                enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                    assertAttribute(e2, KEY, "a");
                    p.input(assertGlobalObjectInput);
                }).exit(assertReturnValue(42));
                bin.input(42);
            }).exit();
            write.input(41);
        }).exit();
    }

    @Test
    public void decProperty() {
        evalWithTag("var a = {x:42}; a.x--;", UnaryExpressionTag.class);

        enter(UnaryExpressionTag.class, (e, b) -> {
            assertAttribute(e, OPERATOR, "--");
            b.input(42);
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

    @Test
    public void incLocal() {
        evalAllTags("function foo(a) { var x = a++; return x; }; foo(42);");
        assertAllLocalOperationsPost("+", 43, 42);
    }

    @Test
    public void decLocal() {
        evalAllTags("function foo(a) { var x = a--; return x; }; foo(42);");
        assertAllLocalOperationsPost("-", 41, 42);
    }

    @Test
    public void incLocalPre() {
        evalAllTags("function foo(a) { var x = ++a; return x; }; foo(42);");
        assertAllLocalOperationsPost("+", 43, 43);
    }

    @Test
    public void decLocalPre() {
        evalAllTags("function foo(a) { var x = --a; return x; }; foo(42);");
        assertAllLocalOperationsPost("-", 41, 41);
    }

    private void assertAllLocalOperationsPost(String operator, int valueSet, int exprReturns) {
        assertGlobalFunctionExpressionDeclaration("foo");

        enter(FunctionCallExpressionTag.class, (e1, p1) -> {
            // Read target and arguments
            enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
            p1.input(Undefined.instance);
            enter(ReadPropertyExpressionTag.class, (e2, p2) -> {
                assertAttribute(e2, KEY, "foo");
                p2.input(assertGlobalObjectInput);
            }).exit(assertJSFunctionReturn);
            p1.input(assertJSFunctionInput);
            enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
            p1.input(42);
            // Set local argument 'a'
            enter(WriteVariableExpressionTag.class, (e3, p3) -> {
                assertAttribute(e3, NAME, "a");
                p3.input(42);
            }).exit();
            // Enter function
            enter(WriteVariableExpressionTag.class, (e3, p3) -> {
                assertAttribute(e3, NAME, "x");
                enter(WriteVariableExpressionTag.class, (e4, p4) -> {
                    assertAttribute(e4, NAME, "a");
                    // De-sugared to a = a + 1;
                    enter(BinaryExpressionTag.class, (e5, p5) -> {
                        assertAttribute(e5, OPERATOR, operator);
                        enter(ReadVariableExpressionTag.class, (e6, p6) -> {
                            assertAttribute(e6, NAME, "a");
                        }).exit(assertReturnValue(42));
                        p5.input(42);
                        enter(LiteralExpressionTag.class).exit(assertReturnValue(1));
                        p5.input(1);
                    }).exit();
                    // Write to 'a' sets new value
                    p4.input(valueSet);
                }).exit();
                // expression returns
                p3.input(exprReturns);
            }).exit();
            // return x;
            enter(ReadVariableExpressionTag.class, (e6, p6) -> {
                assertAttribute(e6, NAME, "x");
            }).exit(assertReturnValue(exprReturns));
        }).exit();
    }

}
