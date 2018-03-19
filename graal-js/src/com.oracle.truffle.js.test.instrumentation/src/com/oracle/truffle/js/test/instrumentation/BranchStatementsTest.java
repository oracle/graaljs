/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
