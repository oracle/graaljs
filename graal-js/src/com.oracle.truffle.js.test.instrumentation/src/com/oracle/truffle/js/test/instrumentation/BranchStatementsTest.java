/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowStatementRootTag;

public class BranchStatementsTest extends FineGrainedAccessTest {

    @Test
    public void breakContinueTest() {
        String src = "let i = 0;" +
                        "outer: while (true) {" +
                        "  if (1 === i) break;" +
                        "    i = i + 1;" +
                        "  continue;" +
                        "}";

        evalWithTags(src, new Class[]{ControlFlowBlockStatementTag.class, ControlFlowBranchStatementTag.class});

        enter(ControlFlowBranchStatementTag.class, (e, b) -> {
            b.input(true);
        }).exit();

        enter(ControlFlowBlockStatementTag.class, (e, b) -> {
            enter(ControlFlowBranchStatementTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchStatementTag.Type.Condition.name());
                b1.input(false);
            }).exit();
            enter(ControlFlowBranchStatementTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchStatementTag.Type.Continue.name());
            }).exitExceptional();
        }).exit();

        enter(ControlFlowBranchStatementTag.class, (e, b) -> {
            assertAttribute(e, TYPE, ControlFlowBranchStatementTag.Type.Condition.name());
            b.input(true);
        }).exit();

        enter(ControlFlowBlockStatementTag.class, (e, b) -> {
            enter(ControlFlowBranchStatementTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchStatementTag.Type.Condition.name());
                b1.input(true);
            }).exit();

            enter(ControlFlowBlockStatementTag.class, (e1, b1) -> {
                enter(ControlFlowBranchStatementTag.class, (e2, b2) -> {
                    assertAttribute(e2, TYPE, ControlFlowBranchStatementTag.Type.Break.name());

                }).exitExceptional();
            }).exitExceptional();
        }).exitExceptional();
    }

    @Test
    public void throwTest() {
        String src = "try {" +
                        "  throw 'foo';" +
                        "} catch (e) {};";

        evalWithTags(src, new Class[]{ControlFlowBlockStatementTag.class, ControlFlowStatementRootTag.class, ControlFlowBranchStatementTag.class});

        enter(ControlFlowStatementRootTag.class, (e, b) -> {
            assertAttribute(e, TYPE, ControlFlowStatementRootTag.Type.ExcetionHandler.name());
            enter(ControlFlowBranchStatementTag.class, (e1, b1) -> {
                assertAttribute(e1, TYPE, ControlFlowBranchStatementTag.Type.Throw.name());
                b1.input("foo");
            }).exitExceptional();
        }).exit();
    }

}
