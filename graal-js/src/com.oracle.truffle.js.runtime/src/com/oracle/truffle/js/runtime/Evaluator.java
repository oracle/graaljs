/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

    /**
     * Returns the NodeFactory used by this parser instance to create AST nodes.
     */
    Object getDefaultNodeFactory();

    JSModuleRecord parseModule(JSContext context, Source source, JSModuleLoader moduleLoader);

    JSModuleRecord hostResolveImportedModule(JSModuleRecord referencingModule, String specifier);

    void moduleDeclarationInstantiation(JSModuleRecord moduleRecord);

    Object moduleEvaluation(JSRealm realm, JSModuleRecord moduleRecord);
}
