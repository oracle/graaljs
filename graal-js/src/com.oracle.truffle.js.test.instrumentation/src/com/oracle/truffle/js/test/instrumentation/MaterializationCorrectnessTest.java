/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.control.ForNode;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;

public class MaterializationCorrectnessTest extends FineGrainedAccessTest {
    @Test
    public void testNoDoubleMaterializationReadElementNode() {
        Source source = evalAllTags("var a = [1]; a[0];");

        assertGlobalArrayLiteralDeclaration("a");

        List<ReadElementNode> readElementNodeList = new ArrayList<>();
        testNoDoubleMaterializationReadElementNodeCheck(readElementNodeList);
        assertFalse(readElementNodeList.isEmpty());

        InstrumentedNodesExecutionEventListener listener = new InstrumentedNodesExecutionEventListener(ReadElementNode.class);
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, listener);

        evalWithCurrentBinding(source);

        assertGlobalArrayLiteralDeclaration("a");

        List<ReadElementNode> readElementNodeList2 = new ArrayList<>();
        testNoDoubleMaterializationReadElementNodeCheck(readElementNodeList2);
        assertFalse(readElementNodeList2.isEmpty());

        listener.checkEnteredNodes(readElementNodeList);
        listener.checkExitedNodes(readElementNodeList);
        listener.checkEnteredNodes(readElementNodeList2);
        listener.checkExitedNodes(readElementNodeList2);

        assertFalse(JSNodeUtil.isTaggedNode(readElementNodeList.get(0).getIndexNode()));
        assertFalse(JSNodeUtil.isTaggedNode(readElementNodeList.get(0).getTarget()));
    }

    private void testNoDoubleMaterializationReadElementNodeCheck(List<ReadElementNode> readElementNodeList) {
        enter(JSTags.ReadElementTag.class, (e, elem) -> {
            assertTrue(e.instrumentedNode instanceof ReadElementNode);
            readElementNodeList.add((ReadElementNode) e.instrumentedNode);
            enter(JSTags.ReadPropertyTag.class).input().exit();
            elem.input();
            enter(JSTags.LiteralTag.class).exit();
            elem.input();
        }).exit();
    }

    @Test
    public void testNoDoubleMaterializationReadElementNodeNoSourceSectionTargetAndIndex() {
        Source source = evalWithTag("var u=[2,4,6]; var p = 1; u[p] -= 42", JSTags.ReadElementTag.class);

        List<ReadElementNode> readElementNodeList = new ArrayList<>();
        testNoDoubleMaterializationReadElementNodeNoSourceSectionTargetAndIndexCheck(readElementNodeList);
        assertFalse(readElementNodeList.isEmpty());

        InstrumentedNodesExecutionEventListener listener = new InstrumentedNodesExecutionEventListener(ReadElementNode.class);
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, listener);

        evalWithCurrentBinding(source);

        List<ReadElementNode> readElementNodeList2 = new ArrayList<>();
        testNoDoubleMaterializationReadElementNodeNoSourceSectionTargetAndIndexCheck(readElementNodeList2);
        assertFalse(readElementNodeList2.isEmpty());

        listener.checkEnteredNodes(readElementNodeList);
        listener.checkExitedNodes(readElementNodeList);

        listener.checkEnteredNodes(readElementNodeList2);
        listener.checkExitedNodes(readElementNodeList2);

        assertTrue(JSNodeUtil.isTaggedNode(readElementNodeList.get(0).getIndexNode()));
        assertTrue(JSNodeUtil.isTaggedNode(readElementNodeList.get(0).getTarget()));
    }

    private void testNoDoubleMaterializationReadElementNodeNoSourceSectionTargetAndIndexCheck(List<ReadElementNode> readElementNodeList) {
        enter(JSTags.ReadElementTag.class, (e, b) -> {
            assertTrue(e.instrumentedNode instanceof ReadElementNode);
            readElementNodeList.add((ReadElementNode) e.instrumentedNode);
        }).input().input().exit();
    }

    @Test
    public void testNoDoubleMaterializationIfNode() {
        Source source = evalAllTags("if (!true) {};");

        List<IfNode> ifNodeList = new ArrayList<>();
        List<JSTaggedExecutionNode> thenNodeList = new ArrayList<>();
        testNoDoubleMaterializationIfNodeCheck(ifNodeList, thenNodeList);
        assertFalse(ifNodeList.isEmpty());
        assertFalse(thenNodeList.isEmpty());

        InstrumentedNodesExecutionEventListener listener = new InstrumentedNodesExecutionEventListener(IfNode.class);
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, listener);

        evalWithCurrentBinding(source);

        List<IfNode> ifNodeList2 = new ArrayList<>();
        List<JSTaggedExecutionNode> thenNodeList2 = new ArrayList<>();
        testNoDoubleMaterializationIfNodeCheck(ifNodeList2, thenNodeList2);
        assertFalse(ifNodeList2.isEmpty());
        assertFalse(thenNodeList2.isEmpty());

        listener.checkEnteredNodes(ifNodeList);
        listener.checkExitedNodes(ifNodeList);
        listener.checkEnteredNodes(ifNodeList2);
        listener.checkExitedNodes(ifNodeList2);

        assertTrue(JSNodeUtil.isTaggedNode(ifNodeList.get(0).getCondition()));
        assertTrue(JSNodeUtil.isTaggedNode(ifNodeList.get(0).getThenPart()));
    }

    private void testNoDoubleMaterializationIfNodeCheck(List<IfNode> ifNodeList, List<JSTaggedExecutionNode> thenNodeList) {
        enter(JSTags.ControlFlowRootTag.class, (e1, ifbody) -> {
            assertTrue(e1.instrumentedNode instanceof IfNode);
            ifNodeList.add((IfNode) e1.instrumentedNode);
            // condition
            enter(JSTags.ControlFlowBranchTag.class, (e2, ifstatement) -> {
                assertTrue(e2.instrumentedNode instanceof JSTaggedExecutionNode);
                thenNodeList.add((JSTaggedExecutionNode) e2.instrumentedNode);

                enter(JSTags.LiteralTag.class).exit();
                ifstatement.input();
            }).exit();
            ifbody.input();
            // no branch is executed; body returns
        }).exit();
    }

    @Test
    public void testNoDoubleMaterializationWhileNode() {
        testNoDoubleMaterializationWhileOrForNode("for (var a=0; a<3; a++) { 42;};", WhileNode.class);
    }

    @Test
    public void testNoDoubleMaterializationForNode() {
        testNoDoubleMaterializationWhileOrForNode("for (let i = 0; i < 3; i++) { function dummy(){return i;} };", ForNode.class);
    }

    private void testNoDoubleMaterializationWhileOrForNode(String src, Class<?> whileOrForNodeClass) {
        Source source = evalWithTags(src, new Class[]{
                        JSTags.ControlFlowRootTag.class,
                        JSTags.ControlFlowBranchTag.class,
                        JSTags.ControlFlowBlockTag.class
        }, new Class[]{/* no input events */});

        List<Node> whileOrForNodeList = new ArrayList<>();
        testNoDoubleMaterializationWhileOrForNodeCheck(whileOrForNodeList, whileOrForNodeClass);
        assertFalse(whileOrForNodeList.isEmpty());

        InstrumentedNodesExecutionEventListener listener = new InstrumentedNodesExecutionEventListener(whileOrForNodeClass);
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, listener);

        evalWithCurrentBinding(source);

        List<Node> whileOrForNodeList2 = new ArrayList<>();
        testNoDoubleMaterializationWhileOrForNodeCheck(whileOrForNodeList2, whileOrForNodeClass);
        assertFalse(whileOrForNodeList2.isEmpty());

        listener.checkEnteredNodes(whileOrForNodeList);
        listener.checkExitedNodes(whileOrForNodeList);
        listener.checkEnteredNodes(whileOrForNodeList2);
        listener.checkExitedNodes(whileOrForNodeList);

        // First child of WhileNode/ForNode is a LoopNode, which has a RepeatingNode on it, and
        // second child
        // of the RepeatingNode is its bodyNode which should be tagged and the tagged node should be
        // wrapped.
        assertTrue(NodeUtil.findNodeChildren(whileOrForNodeList.get(0)).get(0) instanceof LoopNode);
        assertTrue(NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(whileOrForNodeList.get(0)).get(0)).getRepeatingNode()).get(1) instanceof InstrumentableNode.WrapperNode);
        assertTrue(JSNodeUtil.isTaggedNode(NodeUtil.findNodeChildren((Node) ((LoopNode) NodeUtil.findNodeChildren(whileOrForNodeList.get(0)).get(0)).getRepeatingNode()).get(1)));
    }

    private void testNoDoubleMaterializationWhileOrForNodeCheck(List<Node> whileNodeList, Class<?> whileOrForNodeClass) {
        enter(JSTags.ControlFlowRootTag.class, (e) -> {
            assertTrue(whileOrForNodeClass.isAssignableFrom(e.instrumentedNode.getClass()));
            whileNodeList.add(e.instrumentedNode);
            for (int a = 0; a < 3; a++) {
                enter(JSTags.ControlFlowBranchTag.class).exit();
                enter(JSTags.ControlFlowBlockTag.class).exit();
            }
            enter(JSTags.ControlFlowBranchTag.class).exit();
        }).exit();
    }

    @Test
    public void testNoDoubleMaterializationPropertyNode() {
        Source source = evalAllTags("var a = {x:42}; a.x;");

        List<PropertyNode> propertyNodeList = new ArrayList<>();
        testNoDoubleMaterializationPropertyNodeCheck(propertyNodeList);
        assertFalse(propertyNodeList.isEmpty());

        InstrumentedNodesExecutionEventListener listener = new InstrumentedNodesExecutionEventListener(PropertyNode.class);
        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, listener);

        evalWithCurrentBinding(source);

        List<PropertyNode> propertyNodeList2 = new ArrayList<>();
        testNoDoubleMaterializationPropertyNodeCheck(propertyNodeList2);
        assertFalse(propertyNodeList2.isEmpty());

        listener.checkEnteredNodes(propertyNodeList);
        listener.checkExitedNodes(propertyNodeList);
        listener.checkEnteredNodes(propertyNodeList2);
        listener.checkExitedNodes(propertyNodeList2);

        assertTrue(JSNodeUtil.isTaggedNode(propertyNodeList.get(0).getTarget()));
    }

    private void testNoDoubleMaterializationPropertyNodeCheck(List<PropertyNode> propertyNodeList) {
        // var a = {x:42}
        enter(JSTags.WritePropertyTag.class, (e, write) -> {
            write.input();
            enter(JSTags.LiteralTag.class, (e2) -> {
                // num literal
                enter(JSTags.LiteralTag.class).exit();
            }).input().exit();
        }).input().exit();
        // a.x;
        enter(JSTags.ReadPropertyTag.class, (e) -> {
            Assert.assertTrue(e.instrumentedNode instanceof PropertyNode);
            propertyNodeList.add((PropertyNode) e.instrumentedNode);
            enter(JSTags.ReadPropertyTag.class).input().exit();
        }).input().exit();
    }

    @Test
    public void nestedInvokeReadsMultipleInstrumentation() {
        Source source = evalWithTags("function setKey(obj, keys) {" +
                        " obj.a;" +
                        " keys.slice[0][1][2](0, -1).forEach(function(key) {});" +
                        "};" +
                        "var callable = {" +
                        " slice : [['',['','',function fakeslice() { return [1,2]; }]]]" +
                        "};" +
                        "setKey({}, callable);" +
                        "for (var i = 0; i < 2; i++) {" +
                        " setKey({" +
                        " a: 1" +
                        " }, callable);" +
                        "}", new Class[]{JSTags.ReadElementTag.class, JSTags.FunctionCallTag.class});

        nestedInvokeReadsMultipleInstrumentationCheck();

        instrumenter.attachExecutionEventListener(SourceSectionFilter.ANY, new ExecutionEventListener() {
            @Override
            public void onEnter(EventContext context, VirtualFrame frame) {
            }

            @Override
            public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            }

            @Override
            public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            }
        });

        evalWithCurrentBinding(source);

        nestedInvokeReadsMultipleInstrumentationCheck();
    }

    private void nestedInvokeReadsMultipleInstrumentationCheck() {
        for (int i = 0; i < 3; i++) {
            enter(JSTags.FunctionCallTag.class, (e, elem) -> {
                elem.input().input().input().input();
                enter(JSTags.FunctionCallTag.class, (e1, elem1) -> {
                    enter(JSTags.FunctionCallTag.class, (e2, elem2) -> {
                        // First two reads are to retrieve the invoke "target"
                        enter(JSTags.ReadElementTag.class, (e3, elem3) -> {
                            enter(JSTags.ReadElementTag.class, (e4, elem4) -> {
                            }).input().input().exit();
                            elem3.input();
                            elem3.input();
                        }).exit();

                        elem2.input();
                        // Third read to retrieve the invoked function
                        enter(JSTags.ReadElementTag.class, (e3, elem3) -> {
                            elem3.input();
                            elem3.input();
                        }).exit();

                        elem2.input().input().input();
                    }).exit();
                    elem1.input().input().input();
                }).exit();
            }).exit();
        }
    }

    @Test
    public void nestedInvokeReadsMultipleDifferentInstrumentation() {
        // execute first without instrumentation to avoid the first execution optimization on the
        // next instrumentation
        Source source = eval("function setKey(obj, keys) {" +
                        " obj.a;" +
                        " keys.slice[0][1][2](0, -1).forEach(function(key) {});" +
                        "};" +
                        "var callable = {" +
                        " slice : [['',['','',function fakeslice() { return [1,2]; }]]]" +
                        "};" +
                        "setKey({}, callable);" +
                        "for (var i = 0; i < 2; i++) {" +
                        " setKey({" +
                        " a: 1" +
                        " }, callable);" +
                        "}");

        evalWithTags(source, new Class[]{JSTags.FunctionCallTag.class}, new Class[]{});

        for (int i = 0; i < 3; i++) {
            enter(JSTags.FunctionCallTag.class, (e, elem) -> {
                enter(JSTags.FunctionCallTag.class, (e1, elem1) -> {
                    enter(JSTags.FunctionCallTag.class, (e2, elem2) -> {
                    }).exit();
                }).exit();
            }).exit();
        }

        evalWithNewTags(source, new Class[]{JSTags.FunctionCallTag.class, JSTags.ReadElementTag.class}, new Class[]{});

        nestedInvokeReadsNoInputInstrumentationCheck();
    }

    @Test
    public void nestedInvokeReadsNoInputInstrumentation() {
        evalWithTags("function setKey(obj, keys) {" +
                        " obj.a;" +
                        " keys.slice[0][1][2](0, -1).forEach(function(key) {});" +
                        "};" +
                        "var callable = {" +
                        " slice : [['',['','',function fakeslice() { return [1,2]; }]]]" +
                        "};" +
                        "setKey({}, callable);" +
                        "for (var i = 0; i < 2; i++) {" +
                        " setKey({" +
                        " a: 1" +
                        " }, callable);" +
                        "}", new Class[]{JSTags.FunctionCallTag.class, JSTags.ReadElementTag.class}, new Class[]{});

        nestedInvokeReadsNoInputInstrumentationCheck();
    }

    private void nestedInvokeReadsNoInputInstrumentationCheck() {
        for (int i = 0; i < 3; i++) {
            enter(JSTags.FunctionCallTag.class, (e, elem) -> {
                enter(JSTags.FunctionCallTag.class, (e1, elem1) -> {
                    enter(JSTags.FunctionCallTag.class, (e2, elem2) -> {
                        // First two reads are to retrieve the invoke "target"
                        enter(JSTags.ReadElementTag.class, (e3, elem3) -> {
                            enter(JSTags.ReadElementTag.class, (e4, elem4) -> {
                            }).exit();
                        }).exit();

                        // Third read to retrieve the invoked function
                        enter(JSTags.ReadElementTag.class, (e3, elem3) -> {
                        }).exit();

                    }).exit();
                }).exit();
            }).exit();
        }
    }
}
