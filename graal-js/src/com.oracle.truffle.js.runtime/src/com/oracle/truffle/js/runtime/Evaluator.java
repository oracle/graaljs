/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;

public interface Evaluator {

    String EVAL_SOURCE_NAME = "<eval>";
    String FUNCTION_SOURCE_NAME = "<function>";
    String EVAL_AT_SOURCE_NAME_PREFIX = "eval at ";

    /**
     * Evaluate using the global execution context. For example, this method can be used to compute
     * the result of indirect calls to eval.
     *
     * @param lastNode the node invoking the eval or {@code null}
     */
    Object evaluate(JSRealm realm, Node lastNode, Source code);

    /**
     * Evaluate using the local execution context. For example, this method can be used to compute
     * the result of direct calls to eval.
     *
     * @param lastNode the node invoking the eval or {@code null}
     */
    Object evaluate(JSRealm realm, Node lastNode, Source source, Object currEnv, MaterializedFrame frame, Object thisObj);

    Object parseJSON(JSContext context, String jsonString);

    Integer[] parseDate(JSRealm realm, String date);

    String parseToJSON(JSContext context, final String code, final String name, final boolean includeLoc);

    Object loadInternal(JSRealm realm, String fileName);

    /**
     * Returns the NodeFactory used by this parser instance to create AST nodes.
     */
    Object getDefaultNodeFactory();

    JSModuleRecord parseModule(JSContext context, Source source, JSModuleLoader moduleLoader);

    JSModuleRecord hostResolveImportedModule(JSModuleRecord referencingModule, String specifier);

    void moduleDeclarationInstantiation(JSModuleRecord moduleRecord);

    Object moduleEvaluation(JSRealm realm, JSModuleRecord moduleRecord);
}
