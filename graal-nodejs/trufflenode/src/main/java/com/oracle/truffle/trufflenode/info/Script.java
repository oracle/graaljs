/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.ScriptNode;

/**
 *
 * @author Jan Stola
 */
public final class Script {

    private final int id;
    private final ScriptNode scriptNode;
    private final Object parseResult;
    private final boolean graalInternal;

    public Script(ScriptNode scriptNode, Object parseResult, int id) {
        this.scriptNode = scriptNode;
        this.parseResult = parseResult;
        this.id = id;
        this.graalInternal = isGraalInternalScript(scriptNode.getRootNode().getSourceSection().getSource());
    }

    public ScriptNode getScriptNode() {
        return scriptNode;
    }

    public Object getParseResult() {
        return parseResult;
    }

    public int getId() {
        return id;
    }

    public boolean isGraalInternal() {
        return graalInternal;
    }

    private static boolean isGraalInternalScript(Source source) {
        String name = source.getName();
        return name.startsWith("graal/") || name.startsWith("internal/graal/");
    }

}
