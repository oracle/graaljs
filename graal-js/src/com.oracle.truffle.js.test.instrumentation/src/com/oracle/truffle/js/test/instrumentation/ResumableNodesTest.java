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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;

public class ResumableNodesTest extends FineGrainedAccessTest {

    @Test
    public void asyncAwait() {
        String src = "function res() {" +
                        "  var local2 = 20;" +
                        "  return new Promise(resolve => {" +
                        "    resolve(local2);" +
                        "  });" +
                        "};" +
                        "async function asyncCall() {" +
                        "  var local = 2;" +
                        "  return await res() + await res() + local;" +
                        "};" +
                        "asyncCall();";

        evalWithTags(src, new Class[]{WriteVariableExpressionTag.class,
                        ReadVariableExpressionTag.class});

        assertVariableWrite("local", 2);
        // 1st call to res()
        assertVariableWrite("local2", 20);
        assertVariableWrite("resolve");
        assertVariableRead("resolve");
        assertVariableRead("local2", 20);

        // 2nd call to res()
        assertVariableWrite("local2", 20);
        assertVariableWrite("resolve");
        assertVariableRead("resolve");
        assertVariableRead("local2", 20);

        assertVariableRead("local", 2);
    }

    @Test
    public void generatorYield() {
        String src = "function* foo(index) {" +
                        "  var local = 1;" +
                        "  while (index < 3) {" +
                        "    yield (local + index++);" +
                        "  };" +
                        "};" +
                        "var val = 0;" +
                        "var iterator = foo(val);" +
                        "iterator.next().value;" +
                        "iterator.next().value;";

        evalWithTags(src, new Class[]{WriteVariableExpressionTag.class,
                        ReadVariableExpressionTag.class});
        // iterator init
        assertVariableWrite("local", 1);
        // 1st next() call
        assertVariableRead("index", 0);
        assertVariableRead("local", 1);
        assertVariableInc("index", 0);
        // 2nd next() call
        assertVariableRead("index", 1);
        assertVariableRead("local", 1);
        assertVariableInc("index", 1);
    }

    @Test
    public void generatorYieldInCall() {
        String src = "function dummy(arg) {" +
                        "  return {};" +
                        "};" +
                        "function crash() {" +
                        "  return 'Crash';" +
                        "};" +
                        "function* myGen() {" +
                        "  const c = crash();" +
                        "  const boom = dummy(" +
                        "    yield c" +
                        "  );" +
                        "};" +
                        "a = myGen();" +
                        "a.next();" +
                        "a.next();" +
                        "a.next();";

        evalWithTags(src, new Class[]{FunctionCallExpressionTag.class});

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            call.input(assertUndefinedInput);
            call.input(assertJSFunctionInput("myGen"));
        }).exit();
        // 1st next() calls 'crash()' and yields
        enter(FunctionCallExpressionTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput("next"));
            enter(FunctionCallExpressionTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput("crash"));
            }).exit();
        }).exit();
        // 2nd next() resumes and calls 'dummy()' -- but does not call cash
        enter(FunctionCallExpressionTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput("next"));
            enter(FunctionCallExpressionTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput("dummy"));
            }).exit();
        }).exit();
        // 3rd next() does no calls
        enter(FunctionCallExpressionTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput("next"));
        }).exit();
    }

    protected void assertVariableInc(String name, int value) {
        enter(WriteVariableExpressionTag.class, (e, var) -> {
            assertAttribute(e, NAME, name);
            enter(ReadVariableExpressionTag.class, (e1) -> {
                assertAttribute(e, NAME, name);
            }).exit(assertReturnValue(value));
            var.input(value + 1);
        }).exit();
    }

    protected void assertVariableRead(String name, Object value) {
        enter(ReadVariableExpressionTag.class, (e) -> {
            assertAttribute(e, NAME, name);
        }).exit(assertReturnValue(value));
    }

    protected void assertVariableRead(String name) {
        enter(ReadVariableExpressionTag.class, (e) -> {
            assertAttribute(e, NAME, name);
        }).exit();
    }

    protected void assertVariableWrite(String name, Object value) {
        enter(WriteVariableExpressionTag.class, (e, var) -> {
            assertAttribute(e, NAME, name);
            var.input(value);
        }).exit();
    }

    protected void assertVariableWrite(String name) {
        enter(WriteVariableExpressionTag.class, (e, var) -> {
            assertAttribute(e, NAME, name);
            var.input();
        }).exit();
    }
}
