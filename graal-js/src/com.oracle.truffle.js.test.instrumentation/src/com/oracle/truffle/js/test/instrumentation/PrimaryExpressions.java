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
