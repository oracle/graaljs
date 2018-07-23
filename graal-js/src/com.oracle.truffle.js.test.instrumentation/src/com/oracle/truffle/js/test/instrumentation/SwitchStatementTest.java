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

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;

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
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e, r) -> {
            // first 'if' statement condition is false
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
            // we enter the first 'else' branch
            enter(ControlFlowBlockTag.class, (e1, b) -> {
                // a nested if is executed for the second case
                enter(ControlFlowRootTag.class, (e2, r2) -> {
                    // second case returns true
                    enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                    // we enter the 'case 2' branch
                    enter(ControlFlowBlockTag.class, (e3, b2) -> {
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
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            // first 'if' statement condition is false
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
            // we enter the first 'else' branch
            enter(ControlFlowBlockTag.class, (e1) -> {
                // a nested if is executed for the second case
                enter(ControlFlowRootTag.class, (e2) -> {
                    // second case returns true
                    enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
                    // the innermost 'else' is the default branch
                    enter(ControlFlowBlockTag.class, (e3) -> {
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

    @Test
    public void defaultSwitchNode() {
        String src = "var a = {" +
                        "  x: 3" +
                        "};" +
                        "var b = {" +
                        "  x: 1," +
                        "  y: 2," +
                        "  z: 3" +
                        "};" +
                        "var x = 42;" +
                        "switch (a.x) {" +
                        "  case b.x:" +
                        "    x++;" +
                        "  case b.y:" +
                        "    x++;" +
                        "  case b.z:" +
                        "    ++x;" +
                        "};";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e, r) -> {
            // first 'case' a.x == b.x is false
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
            // first 'case' a.x == b.y is false
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
            // first 'case' a.x == b.z is true
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
            // statement returns value from `++x` statement
        }).exit(assertReturnValue(43));

    }
}
