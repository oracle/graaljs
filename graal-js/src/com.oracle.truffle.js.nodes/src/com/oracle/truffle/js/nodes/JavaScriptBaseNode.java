/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.runtime.util.DebugCounter;

@TypeSystemReference(JSTypes.class)
@NodeInfo(language = "JavaScript", description = "The abstract base node for all JavaScript nodes")
@ImportStatic(JSGuards.class)
@Introspectable
public abstract class JavaScriptBaseNode extends Node {
    private static final DebugCounter NODE_REPLACE_COUNT = DebugCounter.create("Node replace count");

    public JavaScriptBaseNode() {
    }

    @Override
    public JavaScriptBaseNode copy() {
        return (JavaScriptBaseNode) super.copy();
    }

    @Override
    protected void onReplace(Node newNode, CharSequence reason) {
        super.onReplace(newNode, reason);
        NODE_REPLACE_COUNT.inc();
    }
}
