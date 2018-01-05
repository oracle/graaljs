/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;

public interface NodeEvaluator extends Evaluator {

    /**
     * Loads a script file and compiles it. Returns an executable script object.
     */
    ScriptNode loadCompile(JSContext context, Source source);

    /**
     * Parses a script string. Returns an executable script object.
     */
    ScriptNode evalCompile(JSContext context, String sourceCode, String name);

    /**
     * Parse function using parameter list and body, to be used by the {@code Function} constructor.
     *
     * @param lastNode the node invoking the constructor or {@code null}
     */
    ScriptNode parseFunction(JSContext context, Node lastNode, String parameterList, String body, boolean generatorFunction, boolean asyncFunction);
}
