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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;

public class IfStatementTest extends FineGrainedAccessTest {

    @Test
    public void basicNoBranch() {
        evalAllTags("if (!true) {};");

        enter(ControlFlowRootTag.class, (e1, ifbody) -> {
            assertAttribute(e1, TYPE, ControlFlowRootTag.Type.Conditional.name());
            // condition
            enter(ControlFlowBranchTag.class, (e2, ifstatement) -> {
                assertAttribute(e2, TYPE, ControlFlowBranchTag.Type.Condition.name());

                enter(LiteralExpressionTag.class).exit();
                ifstatement.input(false);
            }).exit(assertReturnValue(false));
            ifbody.input(false);
            // no branch is executed; body returns
        }).exit();
    }

    @Test
    public void basicBranch() {
        evalAllTags("if (true) { 3; };");

        enter(ControlFlowRootTag.class, (e1, ifbody) -> {
            assertAttribute(e1, TYPE, ControlFlowRootTag.Type.Conditional.name());
            // condition
            enter(ControlFlowBranchTag.class, (e2, ifstatement) -> {
                assertAttribute(e2, TYPE, ControlFlowBranchTag.Type.Condition.name());

                enter(LiteralExpressionTag.class).exit();
                ifstatement.input(true);
            }).exit(assertReturnValue(true));
            ifbody.input(true);
            // enter if branch
            enter(ControlFlowBlockTag.class, (e2, b) -> {
                enter(LiteralExpressionTag.class).exit(assertReturnValue(3));
            }).exit();
            ifbody.input(3);
        }).exit();
    }

    @Test
    public void basicFilter() {
        String src = "if (true) { 3; };";

        evalWithTags(src, new Class[]{ControlFlowRootTag.class, ControlFlowBlockTag.class, ControlFlowBranchTag.class},
                        new Class[]{});

        enter(ControlFlowRootTag.class, (e1) -> {
            assertAttribute(e1, TYPE, ControlFlowRootTag.Type.Conditional.name());
            // condition
            enter(ControlFlowBranchTag.class, (e) -> {
                assertAttribute(e, TYPE, ControlFlowBranchTag.Type.Condition.name());
            }).exit(assertReturnValue(true));
            // enter if branch
            enter(ControlFlowBlockTag.class).exit();
        }).exit();
    }

    @Test
    public void writeWithTernary() {
        String src = "var a = {x:0}; a.x = 100 > 0 ? 1 : 0;";
        evalWithTag(src, WritePropertyExpressionTag.class);

        enter(WritePropertyExpressionTag.class, (e1, pw1) -> {
            assertAttribute(e1, KEY, "a");
            pw1.input(assertGlobalObjectInput);
            pw1.input(assertJSObjectInput);
        }).exit();
        enter(WritePropertyExpressionTag.class, (e1, pw1) -> {
            assertAttribute(e1, KEY, "x");
            pw1.input(assertJSObjectInput);
            pw1.input(1);
        }).exit();
    }

}
