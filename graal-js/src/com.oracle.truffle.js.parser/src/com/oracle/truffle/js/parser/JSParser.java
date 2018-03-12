/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeEvaluator;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.runtime.JSContext;

public interface JSParser extends NodeEvaluator {

    ScriptNode parseScriptNode(JSContext context, Source source);

    ScriptNode parseScriptNode(JSContext context, String sourceString);

    /**
     * Creates a script that will be evaluated in a specified lexical context.
     */
    JavaScriptNode parseInlineExpression(JSContext context, Source source, Environment environment, boolean isStrict);

    ScriptNode parseScriptNode(JSContext context, Source source, ByteBuffer binary);

    ScriptNode parseScriptNode(JSContext context, Source source, SnapshotProvider snapshotProvider);
}
