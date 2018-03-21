/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;

public class IterationsTest extends FineGrainedAccessTest {

    @Test
    public void basicFor() {
        String src = "for (var a=0; a<3; a++) { 42;};";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.Iteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void basicWhileDo() {
        String src = "var a=0; while(a<3) { a++; };";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.Iteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

}
