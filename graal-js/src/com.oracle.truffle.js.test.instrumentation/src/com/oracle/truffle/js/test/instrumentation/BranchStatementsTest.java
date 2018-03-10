/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowConditionStatementTag;

public class BranchStatementsTest extends FineGrainedAccessTest {

    @Test
    public void propSwitchTest() {
        String src = "let i = 0;" +
                        "outer: while (true) {" +
                        "  if (1 === i) break;" +
                        "    i = i + 1;" +
                        "  continue;" +
                        "}";

        evalWithTags(src, new Class[]{ControlFlowBlockStatementTag.class, ControlFlowConditionStatementTag.class, ControlFlowBranchStatementTag.class});

        enter(ControlFlowConditionStatementTag.class, (e, b) -> {
            b.input(true);
        }).exit();

        enter(ControlFlowBlockStatementTag.class, (e, b) -> {
            enter(ControlFlowConditionStatementTag.class, (e1, b1) -> {
                b1.input(false);
            }).exit();
            enter(ControlFlowBranchStatementTag.class, (e1, b1) -> {
                assertAttribute(e1, "kind", "continue");
            }).exitExceptional();
        }).exit();

        enter(ControlFlowConditionStatementTag.class, (e, b) -> {
            b.input(true);
        }).exit();

        enter(ControlFlowBlockStatementTag.class, (e, b) -> {
            enter(ControlFlowConditionStatementTag.class, (e1, b1) -> {
                b1.input(true);
            }).exit();

            enter(ControlFlowBlockStatementTag.class, (e1, b1) -> {
                enter(ControlFlowBranchStatementTag.class, (e2, b2) -> {
                    assertAttribute(e2, "kind", "break");
                }).exitExceptional();
            }).exitExceptional();
        }).exitExceptional();
    }
}
