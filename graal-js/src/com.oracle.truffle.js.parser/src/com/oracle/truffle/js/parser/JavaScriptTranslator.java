/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.parser;

import java.util.List;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.Scope;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.env.EvalEnvironment;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;

public final class JavaScriptTranslator extends GraalJSTranslator {

    private JavaScriptTranslator(LexicalContext lc, NodeFactory factory, JSContext context, Source source, List<String> argumentNames, int prologLength, Environment environment,
                    boolean isParentStrict, ScriptOrModule scriptOrModule) {
        super(lc, factory, context, source, argumentNames, prologLength, environment, isParentStrict, scriptOrModule);
    }

    private JavaScriptTranslator(NodeFactory factory, JSContext context, Source source, int prologLength, Environment environment, boolean isParentStrict, ScriptOrModule scriptOrModule) {
        this(new LexicalContext(), factory, context, source, null, prologLength, environment, isParentStrict, scriptOrModule);
    }

    public static ScriptNode translateScript(NodeFactory factory, JSContext context, Source source, boolean isParentStrict, String prologue, String epilogue) {
        return translateScript(factory, context, source, isParentStrict, prologue, epilogue, null);
    }

    public static ScriptNode translateScript(NodeFactory factory, JSContext context, Source source, boolean isParentStrict, String prologue,
                    String epilogue, List<String> argumentNames) {
        ScriptOrModule script = factory.createScript(context, source);
        return translateScript(factory, context, null, source, isParentStrict, false, false, null, prologue, epilogue, argumentNames, script);
    }

    public static ScriptNode translateEvalScript(NodeFactory factory, JSContext context, Source source, boolean isParentStrict, DirectEvalContext directEval, ScriptOrModule activeScriptOrModule) {
        Environment parentEnv = directEval == null ? null : directEval.env;
        EvalEnvironment env = new EvalEnvironment(parentEnv, factory, context, directEval != null);
        boolean evalInFunction = parentEnv != null && (parentEnv.function() == null || !parentEnv.function().isGlobal());
        return translateScript(factory, context, env, source, isParentStrict, true, evalInFunction, directEval, "", "", null, activeScriptOrModule);
    }

    public static ScriptNode translateInlineScript(NodeFactory factory, JSContext context, Environment env, Source source, boolean isParentStrict, ScriptOrModule activeScriptOrModule) {
        boolean evalInFunction = env != null && env.getParent() != null;
        return translateScript(factory, context, env, source, isParentStrict, true, evalInFunction, null, "", "", null, activeScriptOrModule);
    }

    private static ScriptNode translateScript(NodeFactory factory, JSContext context, Environment env, Source source, boolean isParentStrict,
                    boolean isEval, boolean evalInFunction, DirectEvalContext directEval, String prologue, String epilogue, List<String> argumentNames, ScriptOrModule activeScriptOrModule) {
        Scope parentScope = directEval == null ? null : directEval.scope;
        FunctionNode parserFunctionNode = GraalJSParserHelper.parseScript(context, source, context.getParserOptions().withStrict(isParentStrict), isEval, evalInFunction, parentScope, prologue,
                        epilogue, argumentNames);
        Source src = applyExplicitSourceURL(source, parserFunctionNode);
        LexicalContext lc = new LexicalContext();
        if (directEval != null && directEval.enclosingClass != null) {
            lc.push(directEval.enclosingClass);
        }
        ScriptOrModule script = activeScriptOrModule != null ? activeScriptOrModule : factory.createScript(context, source);
        JavaScriptTranslator translator = new JavaScriptTranslator(lc, factory, context, src, argumentNames, prologue.length(), env, isParentStrict, script);
        return translator.translateScript(parserFunctionNode);
    }

    private static Source applyExplicitSourceURL(Source source, FunctionNode parserFunctionNode) {
        String explicitURL = parserFunctionNode.getSource().getExplicitURL();
        if (explicitURL != null) {
            boolean internal = source.isInternal() || explicitURL.startsWith(JavaScriptLanguage.INTERNAL_SOURCE_URL_PREFIX);
            if (!(explicitURL.equals(source.getName()) && internal == source.isInternal())) {
                return Source.newBuilder(source).name(explicitURL).internal(internal).build();
            }
        }
        return source;
    }

    public static ScriptNode translateFunction(NodeFactory factory, JSContext context, Environment env, Source source, boolean isParentStrict,
                    com.oracle.js.parser.ir.FunctionNode rootNode) {
        return translateFunction(factory, context, env, source, 0, isParentStrict, rootNode);
    }

    public static ScriptNode translateFunction(NodeFactory factory, JSContext context, Environment env, Source source, int prologLength, boolean isParentStrict,
                    com.oracle.js.parser.ir.FunctionNode rootNode) {
        ScriptOrModule script = factory.createScript(context, source);
        return new JavaScriptTranslator(factory, context, source, prologLength, env, isParentStrict, script).translateScript(rootNode);
    }

    public static JSModuleData translateModule(NodeFactory factory, JSContext context, Source source) {
        FunctionNode parsed = GraalJSParserHelper.parseModule(context, source, context.getParserOptions().withStrict(true));
        JSModuleData moduleData = new JSModuleData(parsed.getModule(), context, source);
        JavaScriptTranslator translator = new JavaScriptTranslator(factory, context, source, 0, null, true, moduleData);
        return translator.translateModule(parsed, moduleData);
    }

    private ScriptNode translateScript(FunctionNode functionNode) {
        if (!functionNode.isScript()) {
            throw new IllegalArgumentException("root function node is not a script");
        }
        JSFunctionExpressionNode functionExpression = (JSFunctionExpressionNode) transformFunction(functionNode);
        JSFunctionData functionData = functionExpression.getFunctionData();
        ScriptNode script = ScriptNode.fromFunctionData(functionData);
        return script;
    }

    private JSModuleData translateModule(com.oracle.js.parser.ir.FunctionNode functionNode, JSModuleData moduleData) {
        if (!functionNode.isModule()) {
            throw new IllegalArgumentException("root function node is not a module");
        }
        JSFunctionExpressionNode functionExpression = (JSFunctionExpressionNode) transformFunction(functionNode);
        JSFunctionData functionData = functionExpression.getFunctionData();
        moduleData.setFunctionData(functionData);
        return moduleData;
    }

    @Override
    protected GraalJSTranslator newTranslator(Environment env, LexicalContext savedLC) {
        return new JavaScriptTranslator(savedLC.copy(), factory, context, source, argumentNames, prologLength, env, false, activeScriptOrModule);
    }
}
