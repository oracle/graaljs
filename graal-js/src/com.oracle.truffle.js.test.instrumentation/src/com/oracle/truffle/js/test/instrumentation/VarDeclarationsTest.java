/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.DeclareTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;

public class VarDeclarationsTest extends FineGrainedAccessTest {

    @Test
    public void var() {
        evalWithTag("(function() { var b = 42; })();", DeclareTag.class);

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "b");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
    }

    @Test
    public void argAndVar() {
        evalWithTag("(function(a) { var b = 42; })();", DeclareTag.class);

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "a");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "b");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
    }

    @Test
    public void let() {
        evalWithTag("(function() { let b = 42; })();", DeclareTag.class);

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "b");
            assertAttribute(e, DECL_TYPE, "let");
        }).exit();
    }

    @Test
    public void constDeclareVar() {
        evalWithTags("(function() { const b = 42; })();", new Class<?>[]{DeclareTag.class, WriteVariableTag.class});

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "b");
            assertAttribute(e, DECL_TYPE, "const");
        }).exit();
        enter(WriteVariableTag.class, (e1, w1) -> {
            assertAttribute(e1, NAME, "b");
            w1.input(42);
        }).exit();
    }

    @Test
    public void letConstBlock() {
        evalWithTag("(function() { const c = 1; for(var i = 0; i<c; i++) { let b = i; } })();", DeclareTag.class);

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "c");
            assertAttribute(e, DECL_TYPE, "const");
        }).exit();

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "i");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();

        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "b");
            assertAttribute(e, DECL_TYPE, "let");
        }).exit();
    }

    @Ignore("GR-21919")
    @Test
    public void classDeclareVar() {
        evalWithTags("class Foo{}", new Class<?>[]{DeclareTag.class, WriteVariableTag.class});
        enter(DeclareTag.class, (e2) -> {
            assertAttribute(e2, DECL_NAME, "Foo");
            assertAttribute(e2, DECL_TYPE, "const");
        }).exit();
        enter(WriteVariableTag.class, (e1, w1) -> {
            assertAttribute(e1, NAME, "Foo");
            w1.input(assertJSFunctionInput);
        }).exit();
    }

    @Test
    public void classDeclare() {
        evalWithTags("class Foo{}", new Class<?>[]{DeclareTag.class});

        enter(DeclareTag.class, (e2) -> {
            assertAttribute(e2, DECL_NAME, "Foo");
            assertAttribute(e2, DECL_TYPE, "const");
        }).exit();
    }

    @Test
    public void functionDeclare() {
        evalWithTag("(function() { function foo() {} })();", DeclareTag.class);
        // outer expression must not be entered as declaration
        enter(DeclareTag.class, (e, expr) -> {
            assertAttribute(e, DECL_NAME, "foo");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
    }

    @Test
    public void functionDeclareLiteral() {
        evalWithTag("function foo() {};", LiteralTag.class);
        enter(LiteralTag.class, (e, expr) -> {
            assertAttribute(e, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Test
    public void generatorDeclare() {
        evalWithTag("function* foo() {};", DeclareTag.class);
        enter(DeclareTag.class, (e, expr) -> {
            assertAttribute(e, DECL_NAME, "foo");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
    }

    @Test
    public void generatorDeclareLiteral() {
        evalWithTag("function* foo() {};", LiteralTag.class);
        enter(LiteralTag.class, (e, expr) -> {
            assertAttribute(e, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
        }).exit();
    }

    @Test
    public void mappedArguments() {
        evalWithTags("(function(\\u0061) { \\u0061 = \\u0061 + 1; return arguments; })(42);",
                        new Class<?>[]{DeclareTag.class, StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class});

        String varName = "a";
        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, varName);
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
        enter(DeclareTag.class, (e, decl) -> {
            assertAttribute(e, DECL_NAME, "arguments");
            assertAttribute(e, DECL_TYPE, "var");
        }).exit();
        enter(StandardTags.WriteVariableTag.class, (e1, w1) -> {
            assertAttribute(e1, NAME, varName);
            w1.input(42);
        }).exit();
        enter(StandardTags.WriteVariableTag.class, (e1, w1) -> {
            assertAttribute(e1, NAME, varName);
            enter(StandardTags.ReadVariableTag.class, (e2, r2) -> {
                assertAttribute(e2, NAME, varName);
            }).exit();
            w1.input(43);
        }).exit();
        enter(StandardTags.ReadVariableTag.class, (e1, w1) -> {
            assertAttribute(e1, NAME, "arguments");
        }).exit();
    }
}
