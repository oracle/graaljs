/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Source;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.ForNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;

public class IterationsTest extends FineGrainedAccessTest {

    @Test
    public void testNoDoubleMaterializationWhileNode() {
        String src = "for (var a=0; a<3; a++) { 42;};";

        Source source = evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        WhileNode[] whileNode = new WhileNode[1];
        enter(ControlFlowRootTag.class, (e) -> {
            assertTrue(e.instrumentedNode instanceof WhileNode);
            whileNode[0] = (WhileNode) e.instrumentedNode;
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
        assertNotNull(whileNode[0]);

        WhileNode[] secondTimeEnteredWhileNode = new WhileNode[1];
        WhileNode[] secondTimeExitedWhileNode = new WhileNode[1];
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, new ExecutionEventListener() {
            @Override
            public void onEnter(EventContext context, VirtualFrame frame) {
                if (context.getInstrumentedNode() instanceof WhileNode) {
                    secondTimeEnteredWhileNode[0] = (WhileNode) context.getInstrumentedNode();
                }
            }

            @Override
            public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
                if (context.getInstrumentedNode() instanceof WhileNode) {
                    secondTimeExitedWhileNode[0] = (WhileNode) context.getInstrumentedNode();
                }
            }

            @Override
            public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            }
        });

        evalWithCurrentBinding(source);
        assertSame(whileNode[0], secondTimeEnteredWhileNode[0]);
        assertSame(whileNode[0], secondTimeExitedWhileNode[0]);

        enter(ControlFlowRootTag.class, (e) -> {
            assertTrue(e.instrumentedNode instanceof WhileNode);
            assertSame(whileNode[0], e.instrumentedNode);
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();

        // First child of WhileNode is a LoopNode, which has a RepeatingNode on it, and second child
        // of the RepeatingNode is its bodyNode which should be tagged and the tagged node should be
        // wrapped.
        assertTrue(NodeUtil.findNodeChildren(whileNode[0]).get(0) instanceof LoopNode);
        assertTrue(NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(whileNode[0]).get(0)).getRepeatingNode()).get(1) instanceof InstrumentableNode.WrapperNode);
        assertTrue(JavaScriptNode.isTaggedNode(NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(whileNode[0]).get(0)).getRepeatingNode()).get(1)));
    }

    @Test
    public void basicFor() {
        String src = "for (var a=0; a<3; a++) { 42;};";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
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
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.WhileIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void basicDoWhile() {
        String src = "var a=0; do { a++; } while(a<3);";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.DoWhileIteration.name());
            for (int a = 0; a < 2; a++) {
                enter(ControlFlowBlockTag.class).exit();
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
            }
            enter(ControlFlowBlockTag.class).exit();
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void emptyForLet() {
        String src = "for (let i = 0; i < 3; i++) {};";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void testNoDoubleMaterializationForNode() {
        String src = "for (let i = 0; i < 3; i++) { function dummy(){return i;} };";

        Source source = evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        ForNode[] forNode = new ForNode[1];
        enter(ControlFlowRootTag.class, (e) -> {
            assertTrue(e.instrumentedNode instanceof ForNode);
            forNode[0] = (ForNode) e.instrumentedNode;
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
        assertNotNull(forNode[0]);

        ForNode[] secondTimeEnteredForNode = new ForNode[1];
        ForNode[] secondTimeExitedForNode = new ForNode[1];
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, new ExecutionEventListener() {
            @Override
            public void onEnter(EventContext context, VirtualFrame frame) {
                if (context.getInstrumentedNode() instanceof ForNode) {
                    secondTimeEnteredForNode[0] = (ForNode) context.getInstrumentedNode();
                }
            }

            @Override
            public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
                if (context.getInstrumentedNode() instanceof ForNode) {
                    secondTimeExitedForNode[0] = (ForNode) context.getInstrumentedNode();
                }
            }

            @Override
            public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            }
        });

        evalWithCurrentBinding(source);
        assertSame(forNode[0], secondTimeEnteredForNode[0]);
        assertSame(forNode[0], secondTimeExitedForNode[0]);

        enter(ControlFlowRootTag.class, (e) -> {
            assertTrue(e.instrumentedNode instanceof ForNode);
            assertSame(forNode[0], e.instrumentedNode);
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
        // First child of ForNode is a LoopNode, which has a RepeatingNode on it, and second child
        // of the RepeatingNode is its bodyNode which should be tagged and the tagged node should be
        // wrapped.
        assertTrue(NodeUtil.findNodeChildren(forNode[0]).get(0) instanceof LoopNode);
        assertTrue(NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(forNode[0]).get(0)).getRepeatingNode()).get(1) instanceof InstrumentableNode.WrapperNode);
        assertTrue(JavaScriptNode.isTaggedNode((JavaScriptNode) NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(forNode[0]).get(0)).getRepeatingNode()).get(1)));
    }

    @Test
    public void forLetWithPerIterationScope() {
        String src = "for (let i = 0; i < 3; i++) { function dummy(){return i;} };";

        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, ControlFlowRootTag.Type.ForIteration.name());
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBranchTag.class).exit(assertReturnValue(true));
                enter(ControlFlowBlockTag.class).exit();
            }
            enter(ControlFlowBranchTag.class).exit(assertReturnValue(false));
        }).exit();
    }

    @Test
    public void emptyForOf() {
        testForInForOf("var obj = ['a', 'b', 'c']; for (var i of obj) {};", ControlFlowRootTag.Type.ForOfIteration.name());
    }

    @Test
    public void emptyForIn() {
        testForInForOf("var obj = ['a', 'b', 'c']; for (var i in obj) {};", ControlFlowRootTag.Type.ForInIteration.name());
    }

    private void testForInForOf(String src, String expectedName) {
        evalWithTags(src, new Class[]{
                        ControlFlowRootTag.class,
                        ControlFlowBranchTag.class,
                        ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        enter(ControlFlowRootTag.class, (e) -> {
            assertAttribute(e, TYPE, expectedName);
            for (int a = 0; a < 3; a++) {
                enter(ControlFlowBlockTag.class).exit();
            }
        }).exit();
    }

}
