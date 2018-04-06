/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantBooleanNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.binary.JSAddConstantLeftNumberNode;
import com.oracle.truffle.js.nodes.binary.JSAddConstantRightNumberNode;
import com.oracle.truffle.js.nodes.binary.JSAddNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseAndConstantNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseOrConstantNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseXorConstantNode;
import com.oracle.truffle.js.nodes.binary.JSLeftShiftConstantNode;
import com.oracle.truffle.js.nodes.binary.JSRightShiftConstantNode;
import com.oracle.truffle.js.nodes.binary.JSUnsignedRightShiftConstantNode;
import com.oracle.truffle.js.nodes.control.ForNode;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.IterationScopeNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode.InvokeNode;
import com.oracle.truffle.js.nodes.function.JSNewNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class MaterializedNodes {

    @Test
    public void functionNode() {
        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(new JavaScriptNode[]{});
        JSFunctionCallNode c = JSFunctionCallNode.create(JSConstantNode.createUndefined(), null, args, false, false);
        c.setSourceSection(Source.newBuilder("").name("").mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(FunctionCallExpressionTag.class);
        c.addStatementTag();
        JSFunctionCallNode m = (JSFunctionCallNode) c.materializeInstrumentableNodes(s);
        assertTrue(m.hasTag(StatementTag.class));
        assertTrue(!((InstrumentableNode) m.getTarget()).hasTag(StatementTag.class));
    }

    @Test
    public void materializeMulti() {
        JSTargetableNode undef = GlobalConstantNode.createGlobalConstant(JSObject.getJSContext(Undefined.instance), "test", Undefined.instance);
        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(new JavaScriptNode[]{});
        JSFunctionCallNode c = JSFunctionCallNode.createInvoke(undef, args, false, false);
        c.setSourceSection(Source.newBuilder("").name("").mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build().createUnavailableSection());
        undef.setSourceSection(Source.newBuilder("").name("").mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(FunctionCallExpressionTag.class);
        c.addStatementTag();
        JSFunctionCallNode m = (JSFunctionCallNode) c.materializeInstrumentableNodes(s);
        m = (JSFunctionCallNode) m.materializeInstrumentableNodes(s);
        InvokeNode i = (InvokeNode) m;
        assertTrue(i.getFunctionTargetNode().getSourceSection() != null);
        assertTrue(m.hasTag(StatementTag.class));
        assertTrue(!((InstrumentableNode) m.getTarget()).hasTag(StatementTag.class));
        assertTrue(m.hasSourceSection());
    }

    @Test
    public void desugaredAddNode() {

        // This will create an optimized JSAddConstantRightNumberNodeGen
        JavaScriptNode optimized = JSAddNode.create(dummyDouble, dummyInt);
        optimized.setSourceSection(Source.newBuilder("").name("").mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(BinaryExpressionTag.class);
        optimized.addStatementTag();
        // materialization should return a node of the same class
        JavaScriptNode desugared = (JavaScriptNode) optimized.materializeInstrumentableNodes(s);
        // otherwise cloning will crash
        JavaScriptNode cloned = JavaScriptNode.cloneUninitialized(desugared);
        assertTrue(cloned.getClass() == desugared.getClass());
    }

    // ##### Generic tests to ensure nodes are not materialized twice.

    private static class DummyConstantNode extends JavaScriptNode {

        private final Object value;

        public DummyConstantNode(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return value;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new DummyConstantNode(value);
        }

    }

    private static final JSConstantNode dummy = JSConstantNode.createUndefined();
    private static final JavaScriptNode dummyInt = JSConstantIntegerNode.create(42);
    private static final JavaScriptNode dummyDouble = JSConstantDoubleNode.create(42.42);
    private static final JavaScriptNode dummyJSNode = new DummyConstantNode(42);
    private static final SourceSection dummySourceSection = Source.newBuilder("").name("").mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build().createUnavailableSection();

    private static final JSContext getDummyCx() {
        return JSObject.getJSContext(Undefined.instance);
    }

    @Test
    public void materializeTwiceGlobalProperty() {
        JavaScriptNode prop = GlobalPropertyNode.createPropertyNode(getDummyCx(), "foo");
        assertNotMaterializedTwice(prop, ReadPropertyExpressionTag.class);
    }

    @Test
    public void materializeTwicePropertyRead() {
        PropertyNode prop = PropertyNode.createProperty(getDummyCx(), dummy, "foo");
        assertNotMaterializedTwice(prop, ReadPropertyExpressionTag.class);
    }

    @Test
    public void materializeTwiceElementRead() {
        ReadElementNode elem = ReadElementNode.create(dummy, dummy, getDummyCx());
        assertNotMaterializedTwice(elem, ReadElementExpressionTag.class);
    }

    @Test
    public void materializeTwiceElementWrite() {
        WriteElementNode elem = WriteElementNode.create(dummy, dummy, dummy, getDummyCx(), false);
        assertNotMaterializedTwice(elem, WriteElementExpressionTag.class);
    }

    @Test
    public void materializeTwicePropertyWrite() {
        WritePropertyNode prop = WritePropertyNode.create(dummy, "foo", dummy, getDummyCx(), false);
        assertNotMaterializedTwice(prop, WritePropertyExpressionTag.class);
    }

    @Test
    public void materializeTwiceAddLeft() {
        JavaScriptNode add = JSAddNode.create(dummyJSNode, dummyInt);
        assert add instanceof JSAddConstantRightNumberNode;
        assertNotMaterializedTwice(add, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceAddRight() {
        JavaScriptNode add = JSAddNode.create(dummyDouble, dummyJSNode);
        assert add instanceof JSAddConstantLeftNumberNode;
        assertNotMaterializedTwice(add, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceAnd() {
        JavaScriptNode node = JSBitwiseAndConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceOr() {
        JavaScriptNode node = JSBitwiseOrConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceXor() {
        JavaScriptNode node = JSBitwiseXorConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceLeftShift() {
        JavaScriptNode node = JSLeftShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceRightShift() {
        JavaScriptNode node = JSRightShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceUnsignedRightShift() {
        JavaScriptNode node = JSUnsignedRightShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceNot() {
        JSNotNode node = (JSNotNode) JSNotNode.create(dummyJSNode);
        assertNotMaterializedTwice(node, UnaryExpressionTag.class);
    }

    @Test
    public void materializeTwiceFor() {
        JavaScriptNode condition = JSConstantBooleanNode.create(true);
        JavaScriptNode body = VoidNode.create(dummy);
        JavaScriptNode modify = VoidNode.create(dummy);
        JavaScriptNode first = JSConstantBooleanNode.create(true);
        JavaScriptNode setNotFirst = VoidNode.create(dummy);
        JSReadFrameSlotNode[] reads = new JSReadFrameSlotNode[]{};
        JSWriteFrameSlotNode[] writes = new JSWriteFrameSlotNode[]{};
        IterationScopeNode dummyScope = IterationScopeNode.create(null, reads, writes);

        ForNode node = ForNode.createFor(condition, body, modify, dummyScope, first, setNotFirst);
        assertNotMaterializedTwice((JavaScriptNode) node.getLoopNode().getRepeatingNode(), ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiIf() {
        IfNode ifNode = IfNode.create(dummy, dummy, dummy);
        assertNotMaterializedTwice(ifNode, ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiWhile() {
        JavaScriptNode node = WhileNode.createDoWhile(dummyJSNode, dummyJSNode);
        assertNotMaterializedTwice(node, ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiCall() {
        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(new JavaScriptNode[]{});
        JSFunctionCallNode c = JSFunctionCallNode.create(JSConstantNode.createUndefined(), null, args, false, false);
        assertNotMaterializedTwice(c, FunctionCallExpressionTag.class);
    }

    @Test
    public void materializeMultiInvoke() {
        JSTargetableNode prop = PropertyNode.createProperty(getDummyCx(), dummy, "foo");
        prop.setSourceSection(dummySourceSection);
        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(new JavaScriptNode[]{});
        JSFunctionCallNode c = JSFunctionCallNode.createInvoke(prop, args, false, false);
        assertNotMaterializedTwice(c, FunctionCallExpressionTag.class);
    }

    @Test
    public void materializeNew() {
        JSTargetableNode prop = GlobalPropertyNode.createPropertyNode(getDummyCx(), "foo");
        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(new JavaScriptNode[]{});
        JSNewNode newnode = JSNewNode.create(prop, args);
        assertNotMaterializedTwice(newnode, ObjectAllocationExpressionTag.class);
    }

    private static void assertNotMaterializedTwice(JavaScriptNode node, Class<? extends Tag> tag) {
        node.setSourceSection(dummySourceSection);
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(tag);
        node.addStatementTag();
        InstrumentableNode m1 = node.materializeInstrumentableNodes(s);
        InstrumentableNode m2 = m1.materializeInstrumentableNodes(s);
        // Maretializing twice has no effect.
        assertEquals(m1, m2);
    }

}
