/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.engine;

import javax.script.*;

import com.oracle.truffle.js.nodes.*;

public class GraalJSCompiledScript extends CompiledScript {
    private final GraalJSEngine engine;
    private final ScriptNode scriptNode;

    public GraalJSCompiledScript(GraalJSEngine engine, ScriptNode scriptNode) {
        this.engine = engine;
        this.scriptNode = scriptNode;
    }

    @Override
    public GraalJSEngine getEngine() {
        return engine;
    }

    @Override
    public Object eval(ScriptContext ctxt) throws ScriptException {
        return engine.eval(scriptNode, ctxt);
    }

    public ScriptNode getScriptNode() {
        return scriptNode;
    }
}
