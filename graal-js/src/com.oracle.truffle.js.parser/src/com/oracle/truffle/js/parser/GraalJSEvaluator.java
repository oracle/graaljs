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
package com.oracle.truffle.js.parser;

import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.MODULE_SOURCE_NAME_PREFIX;
import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.MODULE_SOURCE_NAME_SUFFIX;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.parser.date.DateParser;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.env.EvalEnvironment;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * This is the main external entry into the GraalJS parser.
 *
 * The GraalJS parser is derived from the Oracle Nashorn JavaScript parser
 * (http://openjdk.java.net/projects/nashorn/).
 *
 */
public final class GraalJSEvaluator implements JSParser {

    /**
     * Evaluate indirect eval.
     */
    @Override
    public Object evaluate(JSRealm realm, Node lastNode, Source source) {
        Object thisObj = realm.getGlobalObject();
        return doEvaluate(realm, lastNode, null, thisObj, JSFrameUtil.NULL_MATERIALIZED_FRAME, source, false);
    }

    /**
     * Evaluate Function(parameterList, body).
     */
    @Override
    public ScriptNode parseFunction(JSContext context, Node lastNode, String parameterList, String body, boolean generatorFunction, boolean asyncFunction) {
        boolean paramListEndsWithLineComment = false;
        try {
            paramListEndsWithLineComment = GraalJSParserHelper.checkFunctionSyntax((GraalJSParserOptions) context.getParserOptions(), parameterList, body, generatorFunction, asyncFunction);
        } catch (com.oracle.js.parser.ParserException e) {
            throw parserToJSError(lastNode, e);
        }
        StringBuilder code = new StringBuilder();
        if (asyncFunction) {
            code.append("(async function");
        } else {
            code.append("(function");
        }
        if (generatorFunction) {
            code.append("*");
        }
        if (context.getEcmaScriptVersion() >= 6) {
            code.append(" anonymous");
        }
        if (JSTruffleOptions.NashornCompatibilityMode) {
            code.append(' ');
        }
        code.append('(');
        code.append(parameterList);
        if (paramListEndsWithLineComment) {
            code.append('\n');
        }
        code.append(") {");
        code.append(JSRuntime.LINE_SEPARATOR);
        code.append(body);
        code.append(JSRuntime.LINE_SEPARATOR);
        code.append("})");
        Source source = Source.newBuilder(code.toString()).name(Evaluator.FUNCTION_SOURCE_NAME).language(AbstractJavaScriptLanguage.ID).build();

        return parseEval(context, lastNode, null, source, false);
    }

    /**
     * Evaluate direct eval.
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    @Override
    public Object evaluate(JSRealm realm, Node lastNode, Source source, Object currEnvironment, MaterializedFrame frame, Object thisObj) {
        assert currEnvironment != null;
        assert currEnvironment instanceof Environment;
        assert frame != null;

        Environment outerEnv = (Environment) currEnvironment;
        return doEvaluate(realm, lastNode, outerEnv, thisObj, frame.materialize(), source, outerEnv.isStrictMode());
    }

    @Override
    public JavaScriptNode parseInlineExpression(JSContext context, Source source, Environment env, boolean isStrict) {
        Expression expression = GraalJSParserHelper.parseExpression(source, ((GraalJSParserOptions) context.getParserOptions()).putStrict(isStrict));
        return JavaScriptTranslator.translateExpression(NodeFactory.getInstance(context), context, env, source, isStrict, expression);
    }

    @TruffleBoundary
    private static Object doEvaluate(JSRealm realm, Node lastNode, Environment env, Object thisObj, MaterializedFrame materializedFrame, Source source, boolean isStrict) {
        JSContext context = realm.getContext();
        ScriptNode scriptNode = parseEval(context, lastNode, env, source, isStrict);
        return runParsed(scriptNode, realm, thisObj, materializedFrame);
    }

    private static Object runParsed(ScriptNode scriptNode, JSRealm realm, Object thisObj, MaterializedFrame materializedFrame) {
        DynamicObject functionObj = JSFunction.create(realm, scriptNode.getFunctionData(), materializedFrame);
        return scriptNode.run(JSArguments.createZeroArg(thisObj, functionObj));
    }

    private static ScriptNode parseEval(JSContext context, Node lastNode, Environment env, Source source, boolean isStrict) {
        try {
            EvalEnvironment evalEnv = new EvalEnvironment(env, NodeFactory.getInstance(context), context, env != null);
            return JavaScriptTranslator.translateEvalScript(NodeFactory.getInstance(context), context, evalEnv, source, isStrict);
        } catch (com.oracle.js.parser.ParserException e) {
            throw parserToJSError(lastNode, e);
        }
    }

    @TruffleBoundary
    private static JSException parserToJSError(Node lastNode, com.oracle.js.parser.ParserException e) {
        String message = e.getMessage().replace("\r\n", "\n");
        if (e.getErrorType() == com.oracle.js.parser.JSErrorType.ReferenceError) {
            return Errors.createReferenceError(message, e, lastNode);
        }
        assert e.getErrorType() == com.oracle.js.parser.JSErrorType.SyntaxError;
        if (JSTruffleOptions.NashornCompatibilityMode && lastNode instanceof EvalNode) {
            SourceSection sourceSection = lastNode.getSourceSection();
            String name = sourceSection.getSource().getName();
            int lineNumber = sourceSection.getStartLine();
            int columnNumber = sourceSection.getStartColumn() - 1;
            message = name + '#' + lineNumber + ':' + columnNumber + message;
        }
        return Errors.createSyntaxError(message, e, lastNode);
    }

    @TruffleBoundary
    @Override
    public ScriptNode loadCompile(JSContext context, Source source) {
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, source, false);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }

    @TruffleBoundary
    @Override
    public ScriptNode evalCompile(JSContext context, String sourceCode, String name) {
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, Source.newBuilder(sourceCode).name(name).language(AbstractJavaScriptLanguage.ID).build(), false);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }

    @Override
    public Object parseJSON(JSContext context, String jsonString) {
        CompilerAsserts.neverPartOfCompilation();
        return GraalJSParserHelper.parseJSON(jsonString, context);
    }

    // JSParser methods below

    @Override
    public ScriptNode parseScriptNode(JSContext context, Source source) {
        GraalJSParserOptions po = ((GraalJSParserOptions) context.getParserOptions());
        if (source.getName().startsWith(MODULE_SOURCE_NAME_PREFIX) || source.getName().endsWith(MODULE_SOURCE_NAME_SUFFIX)) {
            return fakeScriptForModule(context, source);
        }
        return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, source, po.isStrict());
    }

    private ScriptNode fakeScriptForModule(JSContext context, Source source) {
        RootNode rootNode = new JavaScriptRootNode(context.getLanguage(), JSBuiltin.createSourceSection("evalModule"), null) {
            @Override
            public Object execute(VirtualFrame frame) {
                JSRealm realm = JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
                return evalModule(realm);
            }

            @TruffleBoundary
            private Object evalModule(JSRealm realm) {
                JSModuleRecord moduleRecord = context.getModuleLoader().loadModule(source);
                moduleDeclarationInstantiation(moduleRecord);
                return moduleEvaluation(realm, moduleRecord);
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(rootNode), 0, "");
        return ScriptNode.fromFunctionData(context, functionData);
    }

    @Override
    public ScriptNode parseScriptNode(JSContext context, String sourceCode) {
        return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, Source.newBuilder(sourceCode).name("<unknown>").language(AbstractJavaScriptLanguage.ID).build(), false);
    }

    @TruffleBoundary
    @Override
    public Integer[] parseDate(JSRealm realm, String date) {
        DateParser dateParser = new DateParser(realm, date);
        return dateParser.parse() ? dateParser.getDateFields() : null;
    }

    @Override
    public String parseToJSON(JSContext context, String code, String name, boolean includeLoc) {
        return GraalJSParserHelper.parseToJSON(code, name, includeLoc, (GraalJSParserOptions) context.getParserOptions());
    }

    @Override
    public Object getDefaultNodeFactory() {
        return NodeFactory.getDefaultInstance();
    }

    /**
     * Parses source to intermediate AST and returns a closure for the translation to Truffle AST.
     */
    public static Supplier<ScriptNode> internalParseForTiming(JSContext context, Source source) {
        com.oracle.js.parser.ir.FunctionNode ast = GraalJSParserHelper.parseScript(source, new GraalJSParserOptions());
        return () -> JavaScriptTranslator.translateFunction(NodeFactory.getInstance(context), context, null, source, false, ast);
    }

    @Override
    public JSModuleRecord parseModule(JSContext context, Source source, JSModuleLoader moduleLoader) {
        try {
            return JavaScriptTranslator.translateModule(NodeFactory.getInstance(context), context, source, moduleLoader);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage(), e, null);
        }
    }

    @Override
    public JSModuleRecord hostResolveImportedModule(JSModuleRecord referencingModule, String specifier) {
        return referencingModule.getModuleLoader().resolveImportedModule(referencingModule, specifier);
    }

    Collection<String> getExportedNames(JSModuleRecord moduleRecord) {
        return getExportedNames(moduleRecord, new HashSet<>());
    }

    private Collection<String> getExportedNames(JSModuleRecord moduleRecord, Set<JSModuleRecord> exportStarSet) {
        if (exportStarSet.contains(moduleRecord)) {
            // Assert: We’ve reached the starting point of an import * circularity.
            return Collections.emptySortedSet();
        }
        exportStarSet.add(moduleRecord);
        Collection<String> exportedNames = new HashSet<>();
        Module module = (Module) moduleRecord.getModule();
        for (ExportEntry exportEntry : module.getLocalExportEntries()) {
            // Assert: module provides the direct binding for this export.
            exportedNames.add(exportEntry.getExportName());
        }
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            // Assert: module imports a specific binding for this export.
            exportedNames.add(exportEntry.getExportName());
        }
        for (ExportEntry exportEntry : module.getStarExportEntries()) {
            JSModuleRecord requestedModule = hostResolveImportedModule(moduleRecord, exportEntry.getModuleRequest());
            Collection<String> starNames = getExportedNames(requestedModule, exportStarSet);
            for (String starName : starNames) {
                if (!starName.equals(Module.DEFAULT_NAME)) {
                    if (!exportedNames.contains(starName)) {
                        exportedNames.add(starName);
                    }
                }
            }
        }
        return exportedNames;
    }

    public ExportResolution resolveExport(JSModuleRecord referencingModule, String exportName) {
        return resolveExport(referencingModule, exportName, new HashSet<>(), new HashSet<>());
    }

    /**
     * ResolveExport attempts to resolve an imported binding to the actual defining module and local
     * binding name. The defining module may be the module represented by the Module Record this
     * method was invoked on or some other module that is imported by that module. The parameter
     * resolveSet is use to detect unresolved circular import/export paths. If a pair consisting of
     * specific Module Record and exportName is reached that is already in resolveSet, an import
     * circularity has been encountered. Before recursively calling ResolveExport, a pair consisting
     * of module and exportName is added to resolveSet.
     *
     * If a defining module is found a Record {[[module]], [[bindingName]]} is returned. This record
     * identifies the resolved binding of the originally requested export. If no definition was
     * found or the request is found to be circular, null is returned. If the request is found to be
     * ambiguous, the string "ambiguous" is returned.
     */
    private ExportResolution resolveExport(JSModuleRecord referencingModule, String exportName, Set<Pair<JSModuleRecord, String>> resolveSet, Set<JSModuleRecord> exportStarSet) {
        Pair<JSModuleRecord, String> resolved = new Pair<>(referencingModule, exportName);
        if (resolveSet.contains(resolved)) {
            // Assert: this is a circular import request.
            return ExportResolution.notFound();
        }
        resolveSet.add(resolved);
        Module module = (Module) referencingModule.getModule();
        for (ExportEntry exportEntry : module.getLocalExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                // Assert: module provides the direct binding for this export.
                return ExportResolution.resolved(referencingModule, exportEntry.getLocalName());
            }
        }
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                // Assert: module imports a specific binding for this export.
                JSModuleRecord importedModule = hostResolveImportedModule(referencingModule, exportEntry.getModuleRequest());
                ExportResolution indirectResolution = resolveExport(importedModule, exportEntry.getImportName(), resolveSet, exportStarSet);
                if (!indirectResolution.isNull()) {
                    return indirectResolution;
                }
            }
        }
        if (exportName.equals(Module.DEFAULT_NAME)) {
            // Assert: A default export was not explicitly defined by this module.
            // Throw a SyntaxError exception.
            // NOTE A default export cannot be provided by an export *.
            throw Errors.createSyntaxError("A default export cannot be provided by an export *");
        }
        if (exportStarSet.contains(referencingModule)) {
            return ExportResolution.notFound();
        }
        exportStarSet.add(referencingModule);
        ExportResolution starResolution = ExportResolution.notFound();
        for (ExportEntry exportEntry : module.getStarExportEntries()) {
            JSModuleRecord importedModule = hostResolveImportedModule(referencingModule, exportEntry.getModuleRequest());
            ExportResolution resolution = resolveExport(importedModule, exportName, resolveSet, exportStarSet);
            if (resolution.isAmbiguous()) {
                return resolution;
            }
            if (!resolution.isNull()) {
                if (starResolution.isNull()) {
                    starResolution = resolution;
                } else {
                    // Assert: there is more than one * import that includes the requested name.
                    if (!resolution.equals(starResolution)) {
                        return ExportResolution.ambiguous();
                    }
                }
            }
        }
        return starResolution;
    }

    public DynamicObject getModuleNamespace(JSModuleRecord moduleRecord) {
        if (moduleRecord.getNamespace() != null) {
            return moduleRecord.getNamespace();
        }

        Collection<String> exportedNames = getExportedNames(moduleRecord);
        List<Pair<String, ExportResolution>> unambiguousNames = new ArrayList<>();
        for (String exportedName : exportedNames) {
            ExportResolution resolution = resolveExport(moduleRecord, exportedName);
            if (resolution.isNull()) {
                throw Errors.createSyntaxError("Could not resolve export");
            } else if (!resolution.isAmbiguous()) {
                unambiguousNames.add(new Pair<>(exportedName, resolution));
            }
        }
        Map<String, ExportResolution> sortedNames = new LinkedHashMap<>();
        unambiguousNames.stream().sorted(Comparator.comparing(Pair::getFirst)).forEachOrdered(p -> sortedNames.put(p.getFirst(), p.getSecond()));
        DynamicObject namespace = JSModuleNamespace.create(moduleRecord.getContext(), moduleRecord, sortedNames);
        moduleRecord.setNamespace(namespace);
        return namespace;
    }

    @Override
    public void moduleDeclarationInstantiation(JSModuleRecord moduleRecord) {
        if (moduleRecord.isResolved()) {
            return;
        }

        moduleRecord.setResolved(true);
        Module module = (Module) moduleRecord.getModule();
        for (String requestedModule : module.getRequestedModules()) {
            JSModuleRecord requiredModule = hostResolveImportedModule(moduleRecord, requestedModule);
            moduleDeclarationInstantiation(requiredModule);
        }
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            ExportResolution resolution = resolveExport(moduleRecord, exportEntry.getExportName());
            if (resolution.isNull() || resolution.isAmbiguous()) {
                throw Errors.createSyntaxError("Could not resolve indirect export entry");
            }
        }
        // Assert: all named exports from module are resolvable.
        for (ImportEntry importEntry : module.getImportEntries()) {
            JSModuleRecord importedModule = hostResolveImportedModule(moduleRecord, importEntry.getModuleRequest());
            if (importEntry.getImportName().equals(Module.STAR_NAME)) {
                // GetModuleNamespace(importedModule)
                DynamicObject namespace = getModuleNamespace(importedModule);
                assert JSModuleNamespace.isJSModuleNamespace(namespace);
                // envRec.CreateImmutableBinding(in.[[LocalName]], true).
                // Call envRec.InitializeBinding(in.[[LocalName]], namespace).
                // bindings initialized in the translator
            } else {
                // Let resolution be importedModule.ResolveExport(in.[[ImportName]], << >>, << >>).
                ExportResolution resolution = resolveExport(importedModule, importEntry.getImportName());
                // If resolution is null or resolution is "ambiguous", throw SyntaxError.
                if (resolution.isNull() || resolution.isAmbiguous()) {
                    throw Errors.createSyntaxError("Could not resolve import entry");
                }
                // Call envRec.CreateImportBinding(in.[[LocalName]], resolution.[[module]],
                // resolution.[[bindingName]]).
                // bindings initialized in the translator
            }
        }

        moduleRecord.finishTranslation();
    }

    @Override
    public Object moduleEvaluation(JSRealm realm, JSModuleRecord moduleRecord) {
        assert moduleRecord.isResolved();
        if (moduleRecord.isEvaluated()) {
            return Undefined.instance;
        }

        moduleRecord.setEvaluated(true);
        Module module = (Module) moduleRecord.getModule();
        for (String requestedModule : module.getRequestedModules()) {
            JSModuleRecord requiredModule = hostResolveImportedModule(moduleRecord, requestedModule);
            moduleEvaluation(realm, requiredModule);
        }
        return JSFunction.call(JSFunction.create(realm, moduleRecord.getFunctionData()), Undefined.instance, JSArguments.EMPTY_ARGUMENTS_ARRAY);
    }

    @Override
    public ScriptNode parseScriptNode(JSContext context, Source source, ByteBuffer binary) {
        if (binary == null) {
            return parseScriptNode(context, source);
        }
        return ScriptNode.fromFunctionRoot(context, (FunctionRootNode) new BinarySnapshotProvider(binary).apply(NodeFactory.getInstance(context), context, source));
    }

    @Override
    public ScriptNode parseScriptNode(JSContext context, Source source, SnapshotProvider snapshotProvider) {
        if (snapshotProvider == null) {
            return parseScriptNode(context, source);
        }
        return ScriptNode.fromFunctionRoot(context, (FunctionRootNode) snapshotProvider.apply(NodeFactory.getInstance(context), context, source));
    }
}
