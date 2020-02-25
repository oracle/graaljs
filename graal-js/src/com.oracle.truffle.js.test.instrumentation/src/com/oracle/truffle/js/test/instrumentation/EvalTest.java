/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class EvalTest extends FineGrainedAccessTest {

    @Test
    public void eval() {
        evalAllTags("eval('var a = 42;')");

        enter(EvalCallTag.class, (e, eval) -> {
            enter(ReadPropertyTag.class, assertPropertyReadName("eval")).input(assertGlobalObjectInput).exit();
            eval.input(assertJSFunctionInput);
            enter(LiteralTag.class, assertLiteralType(LiteralTag.Type.StringLiteral)).exit();
            eval.input("var a = 42;");
            enter(WritePropertyTag.class, (e1, prop) -> {
                prop.input(assertJSObjectInput);
                enter(LiteralTag.class).exit(assertReturnValue(42));
                prop.input(42);
            }).exit();
        }).exit();
    }

    @Test
    public void evalVariableReadWrite() {
        evalAllTags("function foo(a) {" +
                        "  function bar() {" +
                        "    a = a+1;" +
                        "    return a;" +
                        "  }" +
                        "  eval('var a = 30');" +
                        "  return bar;" +
                        "}" +
                        "foo(42)();");

        // write function foo
        enter(WritePropertyTag.class, (e, wp) -> {
            wp.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit((e2) -> {
                assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
            });
            wp.input(assertJSFunctionInput);
        }).exit();

        // foo(42)();
        enter(FunctionCallTag.class, (e1, call) -> {
            // receiver undefined
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);

            // foo(42);
            enter(FunctionCallTag.class, (e2, call2) -> {
                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                // get the function from the property foo
                enter(ReadPropertyTag.class, (e3, pr) -> {
                    assertAttribute(e3, KEY, "foo");
                    pr.input(assertGlobalObjectInput);
                }).exit();
                call.input(assertJSFunctionInput);

                // argument 42
                enter(LiteralTag.class).exit();
                call.input(42);

                // locals declaration
                enterDeclareTag("a");
                enterDeclareTag("arguments");

                // inside the foo function
                // write the 42 into the argument variable
                enter(WriteVariableTag.class, (e3, vw) -> {
                    vw.input(42);
                }).exit();

                // create a local function bar
                enter(WriteVariableTag.class, (e3, vw) -> {
                    enterDeclareTag("bar");
                    vw.input(assertJSFunctionInput);
                }).exit();

                // eval
                enter(EvalCallTag.class, (e3, eval) -> {
                    // read the eval function
                    enter(ReadVariableTag.class).exit();
                    eval.input();
                    // read the String
                    enter(LiteralTag.class).exit();
                    eval.input();

                    // var a = 30
                    enter(WriteVariableTag.class, (e4, vw) -> {
                        enter(LiteralTag.class).exit();
                        vw.input(30);
                    }).exit();
                }).exit();

                // return bar
                enter(ControlFlowBranchTag.class, (e4, v) -> {
                    assertAttribute(e4, TYPE, ControlFlowBranchTag.Type.Return.name());
                    enter(ReadVariableTag.class).exit();
                    v.input(assertJSFunctionInput);
                }).exitMaybeControlFlowException();
            }).exit();

            // foo(42) returns the function (bar)
            call.input(assertJSFunctionInput);

            // inside bar
            // a = a + 1;
            enter(WriteVariableTag.class, (e2, vw) -> {

                // a + 1
                enter(BinaryOperationTag.class, (e3, b) -> {
                    enter(ReadVariableTag.class).exit();
                    b.input(30);
                    enter(LiteralTag.class).exit();
                    b.input(1);
                }).exit();
                vw.input(31);
            }).exit();

            // return a;
            enter(ControlFlowBranchTag.class, (e4, v) -> {
                assertAttribute(e4, TYPE, ControlFlowBranchTag.Type.Return.name());
                enter(ReadVariableTag.class).exit();
                v.input(31);
            }).exitMaybeControlFlowException();
        }).exit();
    }

}
