/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import org.junit.Test;

public class AsyncFunctionTest extends FineGrainedAccessTest {

    @Test
    public void asyncFunctionBodyBlock() {
        assertSimpleAsyncFunction("(async function foo() { return 42; })();");
    }

    @Test
    public void asyncArrowFunctionBodyBlock() {
        assertSimpleAsyncFunction("(async () => { return 42; })();");
    }

    @Test
    public void asyncFunctionWithAwait() {
        String src = "(async function foo(v) { if (v < 1) {return await foo(v+1);} else {return 42;} })(0);";
        evalWithTags(src, new Class<?>[]{JSTags.ControlFlowRootTag.class, JSTags.ControlFlowBranchTag.class});
        // enter foo the first time
        enter(JSTags.ControlFlowRootTag.class, (e, i) -> {
            // this is the async function
            assertAttribute(e, TYPE, JSTags.ControlFlowRootTag.Type.AsyncFunction.name());
            // if statement
            enter(JSTags.ControlFlowRootTag.class, (e1, i1) -> {
                assertAttribute(e1, TYPE, JSTags.ControlFlowRootTag.Type.Conditional.name());
                // condition true
                enter(JSTags.ControlFlowBranchTag.class, (e2, i2) -> {
                    assertAttribute(e2, TYPE, JSTags.ControlFlowBranchTag.Type.Condition.name());
                    i2.input(true);
                }).exit();
                i1.input(true);
                // return statement
                enter(JSTags.ControlFlowBranchTag.class, (e4, i4) -> {
                    assertAttribute(e4, TYPE, JSTags.ControlFlowBranchTag.Type.Return.name());
                    // await
                    enter(JSTags.ControlFlowBranchTag.class, (e5, i5) -> {
                        assertAttribute(e5, TYPE, JSTags.ControlFlowBranchTag.Type.Await.name());
                        // enter foo again
                        enter(JSTags.ControlFlowRootTag.class, (e6, i6) -> {
                            assertAttribute(e, TYPE, JSTags.ControlFlowRootTag.Type.AsyncFunction.name());
                            // if statement
                            enter(JSTags.ControlFlowRootTag.class, (e7, i7) -> {
                                // condition false
                                enter(JSTags.ControlFlowBranchTag.class, (e8, i8) -> {
                                    i8.input(false);
                                }).exit();
                                i7.input(false);
                                // return 42;
                                enter(JSTags.ControlFlowBranchTag.class, (e9, i9) -> {
                                    assertAttribute(e4, TYPE, JSTags.ControlFlowBranchTag.Type.Return.name());
                                    i9.input(42);
                                }).exit();
                                i7.input(42);
                            }).exit();
                        }).exit();
                        // await got the promise, we can ignore the extra input
                        i5.input(assertJSPromiseInput);
                        i5.input(assertJSPromiseInput);
                    }).exitMaybeControlFlowException();
                }).exitMaybeControlFlowException();
            }).exitMaybeControlFlowException();
        }).exit();
        // resumed
        enter(JSTags.ControlFlowRootTag.class, (e, i) -> {
            assertAttribute(e, TYPE, JSTags.ControlFlowRootTag.Type.Conditional.name());
            enter(JSTags.ControlFlowBranchTag.class, (e1, i1) -> {
                assertAttribute(e1, TYPE, JSTags.ControlFlowBranchTag.Type.Return.name());
                enter(JSTags.ControlFlowBranchTag.class, (e2, i2) -> {
                    assertAttribute(e2, TYPE, JSTags.ControlFlowBranchTag.Type.Await.name());
                    i2.input(42);
                }).exit();
                i1.input(42);
            }).exitMaybeControlFlowException();
            i.input(42);
        }).exitMaybeControlFlowException();
    }

    private void assertSimpleAsyncFunction(String src) {
        evalWithTag(src, JSTags.ControlFlowRootTag.class);

        enter(JSTags.ControlFlowRootTag.class, (e, i) -> {
            assertAttribute(e, TYPE, JSTags.ControlFlowRootTag.Type.AsyncFunction.name());
        }).exit();
    }

}
