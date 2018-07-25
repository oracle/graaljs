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

import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LiteralsTest extends FineGrainedAccessTest {

    protected void testLiteral(String src, LiteralExpressionTag.Type expectedTagType) {
        testLiteral(src, expectedTagType, null);
    }

    protected void testLiteral(String src, LiteralExpressionTag.Type expectedTagType, Object expectedValue) {
        evalAllTags(src);

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
