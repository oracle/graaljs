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

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;

public class BranchStatementsTest extends FineGrainedAccessTest {

    @Test
    public void breakContinueTest() {
        String src = "let i = 0;" +
                        "outer: while (true) {" +
                        "  if (1 === i) break;" +
                        "    i = i + 1;" +
                        "  continue;" +
                        "}";

        evalWithTags(src, new Class[]{ControlFlowBlockTag.class, ControlFlowBranchTag.class});

        enter(ControlFlowBranchTag.class, (e, b) -> {
            b.input(true);
        }).exit();

        enter(ControlFlowBlockTag.class, (e, b) -> {
            enter(ControlFlowBranchTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchTag.Type.Condition.name());
                b1.input(false);
            }).exit();
            enter(ControlFlowBranchTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchTag.Type.Continue.name());
            }).exitExceptional();
        }).exit();

        enter(ControlFlowBranchTag.class, (e, b) -> {
            assertAttribute(e, TYPE, ControlFlowBranchTag.Type.Condition.name());
            b.input(true);
        }).exit();

        enter(ControlFlowBlockTag.class, (e, b) -> {
            enter(ControlFlowBranchTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchTag.Type.Condition.name());
                b1.input(true);
            }).exit();

            enter(ControlFlowBlockTag.class, (e1, b1) -> {
                enter(ControlFlowBranchTag.class, (e2, b2) -> {
                    assertAttribute(e2, TYPE, ControlFlowBranchTag.Type.Break.name());

                }).exitExceptional();
            }).exitExceptional();
        }).exitExceptional();
    }

    @Test
    public void throwTest() {
        String src = "try {" +
                        "  throw 'foo';" +
                        "} catch (e) {};";

        evalWithTags(src, new Class[]{ControlFlowBlockTag.class, ControlFlowRootTag.class, ControlFlowBranchTag.class});

        enter(ControlFlowRootTag.class, (e, b) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ExceptionHandler.name());
            enter(ControlFlowBranchTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchTag.Type.Throw.name());
                b1.input("foo");
            }).exitExceptional();
        }).exit();
    }

}
