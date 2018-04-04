/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.binary.JSAddNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode.InvokeNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
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
        JavaScriptNode int42 = JSConstantIntegerNode.create(42);
        JavaScriptNode double42 = JSConstantDoubleNode.create(42.42);
        // This will create an optimized JSAddConstantRightNumberNodeGen
        JavaScriptNode optimized = JSAddNode.create(double42, int42);
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

}
