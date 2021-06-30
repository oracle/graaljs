/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.js.parser.ir.Expression;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.module.ModuleBlockNode;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;

public interface Evaluator {

    String EVAL_SOURCE_NAME = "<eval>";
    String FUNCTION_SOURCE_NAME = "<function>";
    String EVAL_AT_SOURCE_NAME_PREFIX = "eval at ";

    /**
     * Parse (indirect) eval code using the global execution context.
     *
     * @param lastNode the node invoking the eval or {@code null}
     */
    ScriptNode parseEval(JSContext context, Node lastNode, Source code);

    /**
     * Parse module block code using the global execution context
     */
    JavaScriptNode parseModuleBlock(JSContext context, Source source);

    /**
     * Parse direct eval code using the local execution context.
     *
     * @param lastNode the node invoking the eval or {@code null}
     */
    ScriptNode parseDirectEval(JSContext context, Node lastNode, Source source, Object currEnv);

    Integer[] parseDate(JSRealm realm, String date);

    String parseToJSON(JSContext context, String code, String name, boolean includeLoc);

    /**
     * Returns the NodeFactory used by this parser instance to create AST nodes.
     */
    Object getDefaultNodeFactory();

    JSModuleRecord parseModule(JSContext context, Source source, JSModuleLoader moduleLoader);

    JSModuleRecord hostResolveImportedModule(JSContext context, ScriptOrModule referencingScriptOrModule, String specifier);

    JSModuleRecord hostResolveImportedModule(JSContext context, ScriptOrModule referencingScriptOrModule, DynamicObject moduleBlock, Source source);

    JSModuleRecord hostResolveImportedModuleBlock(JSContext context, ScriptOrModule referencingScriptOrModule, JSModuleRecord moduleRecord, DynamicObject specifier);

    void moduleInstantiation(JSRealm realm, JSModuleRecord moduleRecord);

    Object moduleEvaluation(JSRealm realm, JSModuleRecord moduleRecord);

    DynamicObject getModuleNamespace(JSModuleRecord moduleRecord);

    ExportResolution resolveExport(JSModuleRecord moduleRecord, String exportName);

    /**
     * Parses a script string. Returns an executable script object.
     */
    ScriptNode evalCompile(JSContext context, String sourceCode, String name);

    /**
     * Parse function using parameter list and body, to be used by the {@code Function} constructor.
     */
    ScriptNode parseFunction(JSContext context, String parameterList, String body, boolean generatorFunction, boolean asyncFunction, String sourceName);

    default ScriptNode parseScript(JSContext context, Source source) {
        return parseScript(context, source, "", "");
    }

    default ScriptNode parseScript(JSContext context, Source source, String prolog, String epilog) {
        return parseScript(context, source, prolog, epilog, null);
    }

    ScriptNode parseScript(JSContext context, Source source, String prolog, String epilog, String[] argumentNames);

    ScriptNode parseScript(JSContext context, String sourceString);

    Expression parseExpression(JSContext context, String sourceString);

    /**
     * Creates a script that will be evaluated in a specified lexical context.
     */
    JavaScriptNode parseInlineScript(JSContext context, Source source, MaterializedFrame lexicalContextFrame, boolean isStrict, Node locationNode);
}
