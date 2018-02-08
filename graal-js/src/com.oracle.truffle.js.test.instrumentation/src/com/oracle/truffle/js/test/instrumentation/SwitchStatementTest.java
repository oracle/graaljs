/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowConditionStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowStatementRootTag;

public class SwitchStatementTest extends FineGrainedAccessTest {

    @Test
    public void desugaredSwitch() {
        // Graal.js converts certain switch statements to if-then-else chains. This generates nested
        // events.
        String src = "var a = 42;" +
                        "switch (a) {\n" +
                        "  case 1:" +
                        "    break;" +
                        "  case 42:" +
                        "    42;" +
                        "    break;" +
                        "  default:" +
                        "}";

        evalWithTags(src, new Class[]{
                        ControlFlowStatementRootTag.class,
                        ControlFlowConditionStatementTag.class,
                        ControlFlowBlockStatementTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowStatementRootTag.class, (e, r) -> {
            // first 'if' statement condition is false
            enter(ControlFlowConditionStatementTag.class).exit(assertReturnValue(false));
            // we enter the first 'else' branch
            enter(ControlFlowBlockStatementTag.class, (e1, b) -> {
                // a nested if is executed for the second case
                enter(ControlFlowStatementRootTag.class, (e2, r2) -> {
                    // second case returns true
                    enter(ControlFlowConditionStatementTag.class).exit(assertReturnValue(true));
                    // we enter the 'case 2' branch
                    enter(ControlFlowBlockStatementTag.class, (e3, b2) -> {
                        // the branch returns. The statement evaluates '42'
                    }).exit(assertReturnValue(42));
                }).exit();
            }).exit();
        }).exit();
    }

    @Test
    public void desugaredSwitchDefault() {
        // Graal.js converts certain switch statements to if-then-else chains. This generates nested
        // events.
        String src = "var a = 42;" +
                        "switch (a) {\n" +
                        "  case 1:" +
                        "    break;" +
                        "  case 2:" +
                        "    break;" +
                        "  default:" +
                        "    42;" +
                        "}";

        evalWithTags(src, new Class[]{
                        ControlFlowStatementRootTag.class,
                        ControlFlowConditionStatementTag.class,
                        ControlFlowBlockStatementTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowStatementRootTag.class, (e) -> {
            // first 'if' statement condition is false
            enter(ControlFlowConditionStatementTag.class).exit(assertReturnValue(false));
            // we enter the first 'else' branch
            enter(ControlFlowBlockStatementTag.class, (e1) -> {
                // a nested if is executed for the second case
                enter(ControlFlowStatementRootTag.class, (e2) -> {
                    // second case returns true
                    enter(ControlFlowConditionStatementTag.class).exit(assertReturnValue(false));
                    // the innermost 'else' is the default branch
                    enter(ControlFlowBlockStatementTag.class, (e3) -> {
                        // the default branch evaluates '42'
                    }).exit(assertReturnValue(42));
                }).exit();
            }).exit();
        }).exit();
    }

    @Test
    public void propSwitchTest() {
        String src = "var a = {x:2};" +
                        "   var b = {x:1, y:2, z:3};" +
                        "   switch (a.x) {" +
                        "      case b.x:" +
                        "         break;" +
                        "      case b.y:" +
                        "         break;" +
                        "      case b.z:" +
                        "         break;" +
                        "}";

        evalWithTag(src, BinaryExpressionTag.class);

        enter(BinaryExpressionTag.class, (e, b) -> {
            assertAttribute(e, "operator", "===");
            b.input(2);
            b.input(1);
        }).exit();

        enter(BinaryExpressionTag.class, (e, b) -> {
            assertAttribute(e, "operator", "===");
            b.input(2);
            b.input(2);
        }).exit();
    }
}
