/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowStatementRootTag;

public class IterationsTest extends FineGrainedAccessTest {

    @Test
    public void basicFor() {
        String src = "for (var a=0; a<3; a++) { 42;};";

        evalWithTags(src, new Class[]{
                        ControlFlowStatementRootTag.class,
                        ControlFlowBranchStatementTag.class,
                        ControlFlowBlockStatementTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowStatementRootTag.class, (e) -> {
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchStatementTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockStatementTag.class).exit();
            }
            enter(ControlFlowBranchStatementTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void basicWhileDo() {
        String src = "var a=0; while(a<3) { a++; };";

        evalWithTags(src, new Class[]{
                        ControlFlowStatementRootTag.class,
                        ControlFlowBranchStatementTag.class,
                        ControlFlowBlockStatementTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowStatementRootTag.class, (e) -> {
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchStatementTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockStatementTag.class).exit();
            }
            enter(ControlFlowBranchStatementTag.class).exit(assertReturnValue(false));
        }).exit();
    }

}
