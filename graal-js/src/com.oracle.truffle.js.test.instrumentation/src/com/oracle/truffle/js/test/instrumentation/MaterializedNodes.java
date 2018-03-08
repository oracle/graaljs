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
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;

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

}
