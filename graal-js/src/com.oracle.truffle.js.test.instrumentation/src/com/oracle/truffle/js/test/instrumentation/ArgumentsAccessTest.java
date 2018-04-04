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

import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ArgumentsAccessTest extends FineGrainedAccessTest {

    @Test
    public void oneArg() {
        evalAllTags("function foo(a) { var x = a; }; foo(42);");

        assertGlobalFunctionExpressionDeclaration("foo");

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            call.input(assertJSFunctionInput);
            enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
            call.input(42);
            // Argument 'a' is stored in the frame
            enter(WriteVariableExpressionTag.class, (e1, call1) -> {
                assertAttribute(e1, NAME, "a");
                call1.input(42);
            }).exit();
            // Variable 'x' is created
            enter(WriteVariableExpressionTag.class, (e1, call1) -> {
                assertAttribute(e1, NAME, "x");
                enter(ReadVariableExpressionTag.class, (e2, call2) -> {
                    assertAttribute(e2, NAME, "a");
                }).exit();
                call1.input(42);
            }).exit();

        }).exit();
    }

    @Test
    public void oneArgNotUsed() {

        evalAllTags("function foo(id) {}; foo(42);");

        assertGlobalFunctionExpressionDeclaration("foo");

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            call.input(assertJSFunctionInput);
            enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
            call.input(42);
            // Argument 'id' is stored in frame even if not used
            enter(WriteVariableExpressionTag.class, (e1, call1) -> {
                assertAttribute(e1, NAME, "id");
                call1.input(42);
            }).exit();
        }).exit();
    }

    @Test
    public void twoArgs() {

        evalAllTags("function foo(a,b) {}; foo(42);");

        assertGlobalFunctionExpressionDeclaration("foo");

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
            call.input(assertJSFunctionInput);
            enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
            call.input(42);
            // Argument 'a' is stored in frame even if not used
            enter(WriteVariableExpressionTag.class, (e1, call1) -> {
                assertAttribute(e1, NAME, "a");
                call1.input(42);
            }).exit();
            // No argument: set undefined
            enter(WriteVariableExpressionTag.class, (e1, call1) -> {
                assertAttribute(e1, NAME, "b");
                call1.input(Undefined.instance);
            }).exit();
        }).exit();
    }

}
