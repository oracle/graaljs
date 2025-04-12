/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;
import org.graalvm.polyglot.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class MaterializedNodes {

    public static final TruffleString TEST = Strings.constant("test");
    public static final TruffleString FOO = Strings.constant("foo");
    private JSContext jsContext;
    private Context polyContext;

    @BeforeClass
    public static void initClass() {
        dummyWithSourceSection.setSourceSection(dummySourceSection);
    }

    @Before
    public void init() {
        polyContext = TestUtil.newContextBuilder().build();
        polyContext.enter();
        JSRealm jsRealm = JavaScriptLanguage.getJSRealm(polyContext);
        this.jsContext = jsRealm.getContext();
    }

    @After
    public void dispose() {
        polyContext.leave();
        polyContext.close();
    }

    @Test
    public void functionNode() {
        JavaScriptNode[] args = new JavaScriptNode[]{};
        JSFunctionCallNode c = JSFunctionCallNode.createCall(JSConstantNode.createUndefined(), null, args, false, false);
        c.setSourceSection(Source.newBuilder(JavaScriptLanguage.ID, "", "").build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(FunctionCallTag.class);
        c.addStatementTag();
        JSFunctionCallNode m = (JSFunctionCallNode) c.materializeInstrumentableNodes(s);
        assertTrue(m.hasTag(StatementTag.class));
        assertTrue(!m.getTarget().hasTag(StatementTag.class));
    }

    @Test
    public void materializeMulti() {
        JSTargetableNode undef = GlobalConstantNode.createGlobalConstant(TEST, Undefined.instance);
        JavaScriptNode[] args = new JavaScriptNode[]{};
        JSFunctionCallNode c = JSFunctionCallNode.createInvoke(undef, args, false, false);
        c.setSourceSection(Source.newBuilder(JavaScriptLanguage.ID, "", "").build().createUnavailableSection());
        undef.setSourceSection(Source.newBuilder(JavaScriptLanguage.ID, "", "").build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(FunctionCallTag.class);
        c.addStatementTag();
        JSFunctionCallNode m = (JSFunctionCallNode) c.materializeInstrumentableNodes(s);
        m = (JSFunctionCallNode) m.materializeInstrumentableNodes(s);
        InvokeNode i = (InvokeNode) m;
        assertTrue(i.getFunctionTargetNode().getSourceSection() != null);
        assertTrue(m.hasTag(StatementTag.class));
        assertTrue(!m.getTarget().hasTag(StatementTag.class));
        assertTrue(m.hasSourceSection());
    }

    @Test
    public void desugaredAddNode() {
        // This will create an optimized JSAddConstantRightNumberNodeGen
        JavaScriptNode optimized = JSAddNode.create(dummyDouble, dummyInt);
        optimized.setSourceSection(Source.newBuilder(JavaScriptLanguage.ID, "", "").build().createUnavailableSection());
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(BinaryOperationTag.class);
        optimized.addStatementTag();
        // materialization should return a node of the same class
        JavaScriptNode desugared = (JavaScriptNode) optimized.materializeInstrumentableNodes(s);
        // otherwise cloning will crash
        JavaScriptNode cloned = JavaScriptNode.cloneUninitialized(desugared, null);
        assertTrue(cloned.getClass() == desugared.getClass());
    }

    // ##### Generic tests to ensure nodes are not materialized twice.

    private static class DummyConstantNode extends JavaScriptNode {

        private final Object value;

        DummyConstantNode(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return value;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DummyConstantNode(value);
        }

    }

    private static final JSConstantNode dummy = JSConstantNode.createUndefined();
    private static final JSConstantNode dummyWithSourceSection = JSConstantNode.createUndefined();
    private static final JavaScriptNode dummyInt = JSConstantNode.createInt(42);
    private static final JavaScriptNode dummyDouble = JSConstantNode.createDouble(42.42);
    private static final JavaScriptNode dummyJSNode = new DummyConstantNode(42);
    private static final SourceSection dummySourceSection = Source.newBuilder(JavaScriptLanguage.ID, "", "").build().createUnavailableSection();

    private JSContext getDummyCx() {
        assert jsContext != null;
        return jsContext;
    }

    @Test
    public void materializeTwiceGlobalProperty() {
        JavaScriptNode prop = GlobalPropertyNode.createPropertyNode(getDummyCx(), FOO);
        assertNotMaterializedTwice(prop, ReadPropertyTag.class);
    }

    @Test
    public void materializeTwicePropertyRead() {
        PropertyNode prop = PropertyNode.createProperty(getDummyCx(), dummy, FOO);
        assertNotMaterializedTwice(prop, ReadPropertyTag.class);
    }

    @Test
    public void materializeTwiceElementRead() {
        ReadElementNode elem = ReadElementNode.create(dummy, dummy, getDummyCx());
        assertNotMaterializedTwice(elem, ReadElementTag.class);
    }

    @Test
    public void materializeTwiceElementReadIndexAndTargetWithSourceSection() {
        ReadElementNode elem = ReadElementNode.create(dummyWithSourceSection, dummyWithSourceSection, getDummyCx());
        assertNotMaterializedTwice(elem, ReadElementTag.class);
    }

    @Test
    public void materializeTwiceElementWrite() {
        WriteElementNode elem = WriteElementNode.create(dummy, dummy, dummy, getDummyCx(), false);
        assertNotMaterializedTwice(elem, WriteElementTag.class);
    }

    @Test
    public void materializeTwicePropertyWrite() {
        WritePropertyNode prop = WritePropertyNode.create(dummy, FOO, dummy, getDummyCx(), false);
        assertNotMaterializedTwice(prop, WritePropertyTag.class);
    }

    @Test
    public void materializeTwiceAddLeft() {
        JavaScriptNode add = JSAddNode.create(dummyJSNode, dummyInt);
        assert add instanceof JSAddConstantRightNumberNode;
        assertNotMaterializedTwice(add, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceAddRight() {
        JavaScriptNode add = JSAddNode.create(dummyDouble, dummyJSNode);
        assert add instanceof JSAddConstantLeftNumberNode;
        assertNotMaterializedTwice(add, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceAnd() {
        JavaScriptNode node = JSBitwiseAndConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceOr() {
        JavaScriptNode node = JSBitwiseOrConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceXor() {
        JavaScriptNode node = JSBitwiseXorConstantNode.create(dummy, 42);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceLeftShift() {
        JavaScriptNode node = JSLeftShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceRightShift() {
        JavaScriptNode node = JSRightShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceUnsignedRightShift() {
        JavaScriptNode node = JSUnsignedRightShiftConstantNode.create(dummy, dummyInt);
        assertNotMaterializedTwice(node, BinaryOperationTag.class);
    }

    @Test
    public void materializeTwiceNot() {
        JSNotNode node = (JSNotNode) JSNotNode.create(dummyJSNode);
        assertNotMaterializedTwice(node, UnaryOperationTag.class);
    }

    @Test
    public void materializeTwiceFor() {
        JavaScriptNode condition = JSConstantNode.createBoolean(true);
        JavaScriptNode body = VoidNode.create(dummy);
        JavaScriptNode modify = VoidNode.create(dummy);
        JavaScriptNode first = JSConstantNode.createBoolean(true);
        JavaScriptNode setNotFirst = VoidNode.create(dummy);
        JSReadFrameSlotNode[] reads = new JSReadFrameSlotNode[]{};
        JSWriteFrameSlotNode[] writes = new JSWriteFrameSlotNode[]{};
        IterationScopeNode dummyScope = IterationScopeNode.create(null, reads, writes, 0);

        RepeatingNode repeatingNode = ForNode.createForRepeatingNode(condition, body, modify, dummyScope, first, setNotFirst);
        LoopNode loopNode = Truffle.getRuntime().createLoopNode(repeatingNode);
        ForNode node = ForNode.createFor(loopNode);
        assertNotMaterializedTwice((JavaScriptNode) node.getLoopNode().getRepeatingNode(), ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiIf() {
        IfNode ifNode = IfNode.create(dummy, dummy, dummy);
        assertNotMaterializedTwice(ifNode, ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiWhile() {
        RepeatingNode repeatingNode = WhileNode.createDoWhileRepeatingNode(dummyJSNode, dummyJSNode);
        LoopNode loopNode = Truffle.getRuntime().createLoopNode(repeatingNode);
        JavaScriptNode node = WhileNode.createDoWhile(loopNode);
        assertNotMaterializedTwice(node, ControlFlowRootTag.class);
    }

    @Test
    public void materializeMultiCall() {
        JavaScriptNode[] args = new JavaScriptNode[]{};
        JSFunctionCallNode c = JSFunctionCallNode.createCall(JSConstantNode.createUndefined(), null, args, false, false);
        assertNotMaterializedTwice(c, FunctionCallTag.class);
    }

    @Test
    public void materializeMultiInvoke() {
        JSTargetableNode prop = PropertyNode.createProperty(getDummyCx(), dummy, FOO);
        prop.setSourceSection(dummySourceSection);
        JavaScriptNode[] args = new JavaScriptNode[]{};
        JSFunctionCallNode c = JSFunctionCallNode.createInvoke(prop, args, false, false);
        assertNotMaterializedTwice(c, FunctionCallTag.class);
    }

    @Test
    public void materializeNew() {
        JSTargetableNode prop = GlobalPropertyNode.createPropertyNode(getDummyCx(), FOO);
        JavaScriptNode[] args = new JavaScriptNode[]{};
        AbstractFunctionArgumentsNode arguments = JSFunctionArgumentsNode.create(getDummyCx(), args);
        JSNewNode newnode = JSNewNode.create(getDummyCx(), prop, arguments);
        assertNotMaterializedTwice(newnode, ObjectAllocationTag.class);
    }

    private static void assertNotMaterializedTwice(JavaScriptNode node, Class<? extends Tag> tag) {
        node.setSourceSection(dummySourceSection);
        Set<Class<? extends Tag>> s = new HashSet<>();
        s.add(tag);
        node.addStatementTag();
        InstrumentableNode m1 = node.materializeInstrumentableNodes(s);
        InstrumentableNode m2 = m1.materializeInstrumentableNodes(s);
        // Materializing twice has no effect.
        assertEquals(m1, m2);
    }

}
