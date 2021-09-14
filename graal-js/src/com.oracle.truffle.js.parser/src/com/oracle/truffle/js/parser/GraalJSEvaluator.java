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
package com.oracle.truffle.js.parser;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_SOURCE_NAME_SUFFIX;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.parser.date.DateParser;
import com.oracle.truffle.js.parser.env.DebugEnvironment;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSParserOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord.Status;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
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

    private static final HiddenKey STORE_MODULE_KEY = new HiddenKey("store-module-key");

    /**
     * Evaluate indirect eval.
     */
    @Override
    public ScriptNode parseEval(JSContext context, Node lastNode, Source source) {
        return parseEval(context, lastNode, source, false, null);
    }

    /**
     * Evaluate Function(parameterList, body).
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    @Override
    public ScriptNode parseFunction(JSContext context, String parameterList, String body, boolean generatorFunction, boolean asyncFunction, String sourceName) {
        String wrappedBody = JSRuntime.LINE_SEPARATOR + body + JSRuntime.LINE_SEPARATOR;
        try {
            GraalJSParserHelper.checkFunctionSyntax(context, context.getParserOptions(), parameterList, wrappedBody, generatorFunction, asyncFunction, sourceName);
        } catch (com.oracle.js.parser.ParserException e) {
            e.setLineNumber(e.getLineNumber() - 1); // undo the shift caused by the wrapping
            throw parserToJSError(null, e, context);
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
        if (context.isOptionNashornCompatibilityMode()) {
            code.append(' ');
        }
        code.append('(');
        code.append(parameterList);
        code.append(JSRuntime.LINE_SEPARATOR);
        code.append(") {");
        code.append(wrappedBody);
        code.append("})");
        Source source = Source.newBuilder(JavaScriptLanguage.ID, code.toString(), sourceName).build();

        return parseEval(context, null, source, false, null);
    }

    /**
     * Evaluate direct eval.
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    @Override
    public ScriptNode parseDirectEval(JSContext context, Node lastNode, Source source, Object evalEnv) {
        DirectEvalContext directEval = (DirectEvalContext) evalEnv;
        return parseEval(context, lastNode, source, directEval.env.isStrictMode(), directEval);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static ScriptNode parseEval(JSContext context, Node lastNode, Source source, boolean isStrict, DirectEvalContext directEval) {
        context.checkEvalAllowed();
        NodeFactory nodeFactory = NodeFactory.getInstance(context);
        try {
            return JavaScriptTranslator.translateEvalScript(nodeFactory, context, source, isStrict, directEval);
        } catch (com.oracle.js.parser.ParserException e) {
            throw parserToJSError(lastNode, e, context);
        }
    }

    private static JSException parserToJSError(Node lastNode, com.oracle.js.parser.ParserException e, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        String message = e.getMessage().replace("\r\n", "\n");
        if (e.getErrorType() == com.oracle.js.parser.JSErrorType.ReferenceError) {
            return Errors.createReferenceError(message, e, lastNode);
        }
        assert e.getErrorType() == com.oracle.js.parser.JSErrorType.SyntaxError;
        if (context.isOptionNashornCompatibilityMode() && lastNode instanceof EvalNode) {
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
    public ScriptNode evalCompile(JSContext context, String sourceCode, String name) {
        try {
            context.checkEvalAllowed();
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, Source.newBuilder(JavaScriptLanguage.ID, sourceCode, name).build(), false, "", "");
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }

    // JSParser methods below

    @TruffleBoundary
    @Override
    public ScriptNode parseScript(JSContext context, Source source, String prolog, String epilog, String[] argumentNames) {
        if (isModuleSource(source)) {
            return fakeScriptForModule(context, source);
        }
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, source, context.getParserOptions().isStrict(), prolog, epilog, argumentNames);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }

    private static boolean isModuleSource(Source source) {
        String mimeType = source.getMimeType();
        return MODULE_MIME_TYPE.equals(mimeType) || (mimeType == null && source.getName().endsWith(MODULE_SOURCE_NAME_SUFFIX));
    }

    private ScriptNode fakeScriptForModule(JSContext context, Source source) {
        JSModuleData parsedModule = parseModule(context, source);
        RootNode rootNode = new ModuleScriptRoot(context, parsedModule, source);
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(rootNode), 0, "");
        return ScriptNode.fromFunctionData(context, functionData);
    }

    private final class ModuleScriptRoot extends JavaScriptRootNode {
        private final JSContext context;
        private final JSModuleData parsedModule;
        private final Source source;
        @Child private PerformPromiseThenNode performPromiseThenNode;

        private ModuleScriptRoot(JSContext context, JSModuleData parsedModule, Source source) {
            super(context.getLanguage(), JSBuiltin.createSourceSection(), null);
            this.context = context;
            this.parsedModule = parsedModule;
            this.source = source;
            this.performPromiseThenNode = PerformPromiseThenNode.create(context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSRealm realm = JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
            return evalModule(realm);
        }

        @TruffleBoundary
        private Object evalModule(JSRealm realm) {
            JSModuleRecord moduleRecord = realm.getModuleLoader().loadModule(source, parsedModule);
            moduleInstantiation(realm, moduleRecord);
            Object promise = moduleEvaluation(realm, moduleRecord);
            if (context.isOptionTopLevelAwait() && JSPromise.isJSPromise(promise)) {
                DynamicObject onRejected = createTopLevelAwaitReject(context, realm);
                DynamicObject onAccepted = createTopLevelAwaitResolve(context, realm);
                performPromiseThenNode.execute((DynamicObject) promise, onAccepted, onRejected, null);
            }
            if (realm.getContext().getContextOptions().isEsmEvalReturnsExports()) {
                DynamicObject moduleNamespace = getModuleNamespace(moduleRecord);
                assert moduleNamespace != null;
                return moduleNamespace;
            } else {
                assert promise != null;
                return promise;
            }
        }

        JSModuleData getModuleData() {
            return parsedModule;
        }
    }

    private static DynamicObject createTopLevelAwaitReject(JSContext context, JSRealm realm) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.TopLevelAwaitReject, (c) -> createTopLevelAwaitRejectImpl(c));
        return JSFunction.create(realm, functionData);
    }

    private static JSFunctionData createTopLevelAwaitRejectImpl(JSContext context) {
        class TopLevelAwaitRejectedRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);

            @Override
            public Object execute(VirtualFrame frame) {
                Object error = argumentNode.execute(frame);
                throw JSRuntime.getException(error);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new TopLevelAwaitRejectedRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    private static DynamicObject createTopLevelAwaitResolve(JSContext context, JSRealm realm) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.TopLevelAwaitResolve, (c) -> createTopLevelAwaitResolveImpl(c));
        return JSFunction.create(realm, functionData);
    }

    private static JSFunctionData createTopLevelAwaitResolveImpl(JSContext context) {
        class TopLevelAwaitFulfilledRootNode extends JavaScriptRootNode {

            @Override
            public Object execute(VirtualFrame frame) {
                return Undefined.instance;
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new TopLevelAwaitFulfilledRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    @Override
    public ScriptNode parseScript(JSContext context, String sourceCode) {
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "<unknown>").build(), false, "", "");
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }

    @TruffleBoundary
    @Override
    public Integer[] parseDate(JSRealm realm, String date) {
        DateParser dateParser = new DateParser(realm, date);
        return dateParser.parse() ? dateParser.getDateFields() : null;
    }

    @Override
    public String parseToJSON(JSContext context, String code, String name, boolean includeLoc) {
        return GraalJSParserHelper.parseToJSON(code, name, includeLoc, context.getParserOptions());
    }

    @Override
    public Object getDefaultNodeFactory() {
        return NodeFactory.getDefaultInstance();
    }

    /**
     * Parses source to intermediate AST and returns a closure for the translation to Truffle AST.
     */
    public static Supplier<ScriptNode> internalParseForTiming(JSContext context, Source source) {
        com.oracle.js.parser.ir.FunctionNode ast = GraalJSParserHelper.parseScript(context, source, new JSParserOptions());
        return () -> JavaScriptTranslator.translateFunction(NodeFactory.getInstance(context), context, null, source, 0, false, ast);
    }

    @TruffleBoundary
    @Override
    public JSModuleData parseModule(JSContext context, Source source) {
        try {
            return JavaScriptTranslator.translateModule(NodeFactory.getInstance(context), context, source);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage(), e, null);
        }

    }

    @TruffleBoundary
    @Override
    public JSModuleData envParseModule(JSRealm realm, Source source) {
        assert isModuleSource(source) : source;
        CallTarget parseResult = realm.getEnv().parsePublic(source);
        CallTarget moduleScriptCallTarget = JavaScriptLanguage.getParsedProgramCallTarget(((RootCallTarget) parseResult).getRootNode());
        ModuleScriptRoot moduleScriptRoot = (ModuleScriptRoot) ((RootCallTarget) moduleScriptCallTarget).getRootNode();
        return moduleScriptRoot.getModuleData();
    }

    @TruffleBoundary
    @Override
    public JSModuleRecord parseJSONModule(JSRealm realm, Source source) {
        assert isModuleSource(source) : source;
        Object json = JSFunction.call(JSArguments.createOneArg(Undefined.instance, realm.getJsonParseFunctionObject(), source.getCharacters().toString()));
        return createSyntheticJSONModule(realm, source, json);
    }

    private static JSModuleRecord createSyntheticJSONModule(JSRealm realm, Source source, Object hostDefined) {
        final String exportName = "default";
        FrameDescriptor frameDescriptor = new FrameDescriptor(Undefined.instance);
        FrameSlot slot = frameDescriptor.addFrameSlot(exportName);
        List<ExportEntry> localExportEntries = Collections.singletonList(ExportEntry.exportSpecifier(exportName));
        Module moduleNode = new Module(Collections.emptyList(), Collections.emptyList(), localExportEntries, Collections.emptyList(), Collections.emptyList(), null, null);
        JavaScriptRootNode rootNode = new JavaScriptRootNode(realm.getContext().getLanguage(), source.createUnavailableSection(), frameDescriptor) {
            private final FrameSlot defaultSlot = slot;

            @Override
            public Object execute(VirtualFrame frame) {
                JSModuleRecord module = (JSModuleRecord) JSArguments.getUserArgument(frame.getArguments(), 0);
                if (module.getEnvironment() == null) {
                    assert module.getStatus() == Status.Linking;
                    module.setEnvironment(frame.materialize());
                } else {
                    assert module.getStatus() == Status.Evaluating;
                    setSyntheticModuleExport(module);
                }
                return Undefined.instance;
            }

            private void setSyntheticModuleExport(JSModuleRecord module) {
                module.getEnvironment().setObject(defaultSlot, module.getHostDefined());
            }
        };
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        JSFunctionData functionData = JSFunctionData.createCallOnly(realm.getContext(), callTarget, 0, "");
        final JSModuleData parseModule = new JSModuleData(moduleNode, source, functionData, frameDescriptor);
        return new JSModuleRecord(parseModule, realm.getModuleLoader(), hostDefined);
    }

    @TruffleBoundary
    @Override
    public JSModuleRecord hostResolveImportedModule(JSContext context, ScriptOrModule referrer, ModuleRequest moduleRequest) {
        filterSupportedImportAssertions(context, moduleRequest);
        JSModuleLoader moduleLoader = referrer instanceof JSModuleRecord ? ((JSModuleRecord) referrer).getModuleLoader() : JSRealm.get(null).getModuleLoader();
        return moduleLoader.resolveImportedModule(referrer, moduleRequest);
    }

    private static JSModuleRecord hostResolveImportedModule(JSModuleRecord referencingModule, ModuleRequest moduleRequest) {
        filterSupportedImportAssertions(referencingModule.getContext(), moduleRequest);
        return referencingModule.getModuleLoader().resolveImportedModule(referencingModule, moduleRequest);
    }

    private static void filterSupportedImportAssertions(final JSContext context, final ModuleRequest moduleRequest) {
        if (moduleRequest.getAssertions().isEmpty()) {
            return;
        }
        Map<String, String> supportedAssertions = new HashMap<>();
        for (Map.Entry<String, String> assertion : moduleRequest.getAssertions().entrySet()) {
            String key = assertion.getKey();
            String value = assertion.getValue();
            if (context.getSupportedImportAssertions().contains(key)) {
                supportedAssertions.put(key, value);
            }
        }
        moduleRequest.setAssertions(supportedAssertions);
    }

    Collection<String> getExportedNames(JSModuleRecord moduleRecord) {
        return getExportedNames(moduleRecord, new HashSet<>());
    }

    private Collection<String> getExportedNames(JSModuleRecord moduleRecord, Set<JSModuleRecord> exportStarSet) {
        if (exportStarSet.contains(moduleRecord)) {
            // Assert: We've reached the starting point of an import * circularity.
            return Collections.emptySortedSet();
        }
        exportStarSet.add(moduleRecord);
        Collection<String> exportedNames = new HashSet<>();
        Module module = moduleRecord.getModule();
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

    @TruffleBoundary
    @Override
    public ExportResolution resolveExport(JSModuleRecord referencingModule, String exportName) {
        return resolveExport(referencingModule, exportName, new HashSet<>());
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
    private ExportResolution resolveExport(JSModuleRecord referencingModule, String exportName, Set<Pair<JSModuleRecord, String>> resolveSet) {
        Pair<JSModuleRecord, String> resolved = new Pair<>(referencingModule, exportName);
        if (resolveSet.contains(resolved)) {
            // Assert: this is a circular import request.
            return ExportResolution.notFound();
        }
        resolveSet.add(resolved);
        Module module = referencingModule.getModule();
        for (ExportEntry exportEntry : module.getLocalExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                // Assert: module provides the direct binding for this export.
                return ExportResolution.resolved(referencingModule, exportEntry.getLocalName());
            }
        }
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                JSModuleRecord importedModule = hostResolveImportedModule(referencingModule, exportEntry.getModuleRequest());
                if (exportEntry.getImportName().equals(Module.STAR_NAME)) {
                    // Assert: module does not provide the direct binding for this export.
                    return ExportResolution.resolved(importedModule, Module.NAMESPACE_EXPORT_BINDING_NAME);
                } else {
                    // Assert: module imports a specific binding for this export.
                    return resolveExport(importedModule, exportEntry.getImportName(), resolveSet);
                }
            }
        }
        if (exportName.equals(Module.DEFAULT_NAME)) {
            // Assert: A default export was not explicitly defined by this module.
            return ExportResolution.notFound();
            // NOTE: A default export cannot be provided by an `export *` or `export * from "mod"`.
        }
        ExportResolution starResolution = ExportResolution.notFound();
        for (ExportEntry exportEntry : module.getStarExportEntries()) {
            JSModuleRecord importedModule = hostResolveImportedModule(referencingModule, exportEntry.getModuleRequest());
            ExportResolution resolution = resolveExport(importedModule, exportName, resolveSet);
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

    @TruffleBoundary
    @Override
    public DynamicObject getModuleNamespace(JSModuleRecord moduleRecord) {
        if (moduleRecord.getNamespace() != null) {
            return moduleRecord.getNamespace();
        }

        assert moduleRecord.getStatus() != Status.Unlinked;
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
        DynamicObject namespace = JSModuleNamespace.create(moduleRecord.getContext(), JSRealm.get(null), moduleRecord, sortedNames);
        moduleRecord.setNamespace(namespace);
        return namespace;
    }

    @TruffleBoundary
    @Override
    public void moduleInstantiation(JSRealm realm, JSModuleRecord moduleRecord) {
        assert moduleRecord.getStatus() != Status.Linking && moduleRecord.getStatus() != Status.Evaluating;
        Deque<JSModuleRecord> stack = new ArrayDeque<>(4);

        try {
            innerModuleInstantiation(realm, moduleRecord, stack, 0);
        } catch (GraalJSException e) {
            for (JSModuleRecord m : stack) {
                assert m.getStatus() == Status.Linking;
                m.setUninstantiated();
            }
            assert moduleRecord.getStatus() == Status.Unlinked;
            throw e;
        }

        assert moduleRecord.getStatus() == Status.Linked || moduleRecord.getStatus() == Status.Evaluated;
        assert stack.isEmpty();
    }

    private int innerModuleInstantiation(JSRealm realm, JSModuleRecord moduleRecord, Deque<JSModuleRecord> stack, int index0) {
        int index = index0;
        if (moduleRecord.getStatus() == Status.Linking || moduleRecord.getStatus() == Status.Linked || moduleRecord.getStatus() == Status.Evaluated) {
            return index;
        }
        assert moduleRecord.getStatus() == Status.Unlinked;
        moduleRecord.setStatus(Status.Linking);
        moduleRecord.setDFSIndex(index);
        moduleRecord.setDFSAncestorIndex(index);
        index++;
        stack.push(moduleRecord);

        Module module = moduleRecord.getModule();
        for (ModuleRequest requestedModule : module.getRequestedModules()) {
            JSModuleRecord requiredModule = hostResolveImportedModule(moduleRecord, requestedModule);
            index = innerModuleInstantiation(realm, requiredModule, stack, index);
            assert requiredModule.getStatus() == Status.Linking || requiredModule.getStatus() == Status.Linked ||
                            requiredModule.getStatus() == Status.Evaluated : requiredModule.getStatus();
            assert (requiredModule.getStatus() == Status.Linking) == stack.contains(requiredModule);
            if (requiredModule.getStatus() == Status.Linking) {
                moduleRecord.setDFSAncestorIndex(Math.min(moduleRecord.getDFSAncestorIndex(), requiredModule.getDFSAncestorIndex()));
            }
        }
        moduleInitializeEnvironment(realm, moduleRecord);

        assert occursExactlyOnce(moduleRecord, stack);
        assert moduleRecord.getDFSAncestorIndex() <= moduleRecord.getDFSIndex();
        if (moduleRecord.getDFSAncestorIndex() == moduleRecord.getDFSIndex()) {
            while (true) {
                JSModuleRecord requiredModule = stack.pop();
                requiredModule.setStatus(Status.Linked);
                if (requiredModule.equals(moduleRecord)) {
                    break;
                }
            }
        }
        return index;
    }

    private void moduleInitializeEnvironment(JSRealm realm, JSModuleRecord moduleRecord) {
        assert moduleRecord.getStatus() == Status.Linking;
        Module module = moduleRecord.getModule();
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            ExportResolution resolution = resolveExport(moduleRecord, exportEntry.getExportName());
            if (resolution.isNull() || resolution.isAmbiguous()) {
                throw Errors.createSyntaxError("Could not resolve indirect export entry");
            }
        }

        // Initialize the environment by executing the module function.
        // It will automatically yield when the module is instantiated.
        moduleExecution(realm, moduleRecord, null);
    }

    @TruffleBoundary
    @Override
    public Object moduleEvaluation(JSRealm realm, JSModuleRecord moduleRecord) {
        // Evaluate ( ) Concrete Method
        JSModuleRecord module = moduleRecord;
        Deque<JSModuleRecord> stack = new ArrayDeque<>(4);
        if (realm.getContext().isOptionTopLevelAwait()) {
            assert module.getStatus() == Status.Linked || module.getStatus() == Status.Evaluated;
            if (module.getStatus() == Status.Evaluated) {
                module = getAsyncCycleRoot(module);
            }
            if (module.getTopLevelCapability() != null) {
                return module.getTopLevelCapability().getPromise();
            }
            PromiseCapabilityRecord capability = NewPromiseCapabilityNode.createDefault(realm);
            module.setTopLevelCapability(capability);
            try {
                innerModuleEvaluation(realm, module, stack, 0);
                assert module.getStatus() == Status.Evaluated;
                assert module.getEvaluationError() == null;
                if (!module.isAsyncEvaluating()) {
                    JSFunction.call(JSArguments.create(Undefined.instance, capability.getResolve(), Undefined.instance));
                }
                assert stack.isEmpty();
            } catch (Throwable e) {
                if (TryCatchNode.shouldCatch(e)) {
                    for (JSModuleRecord m : stack) {
                        assert m.getStatus() == Status.Evaluating;
                        m.setStatus(Status.Evaluated);
                        m.setEvaluationError(e);
                    }
                    assert module.getStatus() == Status.Evaluated && module.getEvaluationError() == e;
                    throw e;
                } else {
                    // Not a JS error: throw
                    throw e;
                }
            }
            return capability.getPromise();
        } else {
            try {
                innerModuleEvaluation(realm, module, stack, 0);
            } catch (Throwable e) {
                if (TryCatchNode.shouldCatch(e)) {
                    for (JSModuleRecord m : stack) {
                        assert m.getStatus() == Status.Evaluating;
                        m.setStatus(Status.Evaluated);
                        m.setEvaluationError(e);
                    }
                    assert module.getStatus() == Status.Evaluated && module.getEvaluationError() == e;
                }
                throw e;
            }
            assert module.getStatus() == Status.Evaluated && module.getEvaluationError() == null;

            assert stack.isEmpty();
            Object result = module.getExecutionResult();
            return result == null ? Undefined.instance : result;
        }
    }

    @TruffleBoundary
    private static JSModuleRecord getAsyncCycleRoot(JSModuleRecord moduleRecord) {
        // GetAsyncCycleRoot ( module )
        JSModuleRecord module = moduleRecord;
        assert module.getStatus() == Status.Evaluated;

        if (module.getAsyncParentModules().size() == 0) {
            return module;
        }
        while (module.getDFSIndex() > module.getDFSAncestorIndex()) {
            assert module.getAsyncParentModules().size() != 0;
            JSModuleRecord nextCycleModule = module.getAsyncParentModules().remove(0);
            assert nextCycleModule.getDFSAncestorIndex() <= module.getDFSAncestorIndex();
            module = nextCycleModule;
        }

        assert module.getDFSIndex() == module.getDFSAncestorIndex();
        return module;
    }

    @TruffleBoundary
    private int innerModuleEvaluation(JSRealm realm, JSModuleRecord moduleRecord, Deque<JSModuleRecord> stack, int index0) {
        // InnerModuleEvaluation( module, stack, index )
        int index = index0;
        if (moduleRecord.getStatus() == Status.Evaluated) {
            if (moduleRecord.getEvaluationError() == null) {
                return index;
            } else {
                throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
            }
        }
        if (moduleRecord.getStatus() == Status.Evaluating) {
            return index;
        }
        assert moduleRecord.getStatus() == Status.Linked;
        moduleRecord.setStatus(Status.Evaluating);
        moduleRecord.setDFSIndex(index);
        moduleRecord.setDFSAncestorIndex(index);
        moduleRecord.setPendingAsyncDependencies(0);
        moduleRecord.initAsyncParentModules();
        index++;
        stack.push(moduleRecord);

        Module module = moduleRecord.getModule();
        for (ModuleRequest requestedModule : module.getRequestedModules()) {
            JSModuleRecord requiredModule = hostResolveImportedModule(moduleRecord, requestedModule);
            // Note: Instantiate must have completed successfully prior to invoking this method,
            // so every requested module is guaranteed to resolve successfully.
            index = innerModuleEvaluation(realm, requiredModule, stack, index);
            assert requiredModule.getStatus() == Status.Evaluating || requiredModule.getStatus() == Status.Evaluated : requiredModule.getStatus();
            assert (requiredModule.getStatus() == Status.Evaluating) == stack.contains(requiredModule);
            if (requiredModule.getStatus() == Status.Evaluating) {
                moduleRecord.setDFSAncestorIndex(Math.min(moduleRecord.getDFSAncestorIndex(), requiredModule.getDFSAncestorIndex()));
            } else {
                requiredModule = getAsyncCycleRoot(requiredModule);
                assert requiredModule.getStatus() == Status.Evaluated;
                if (requiredModule.getEvaluationError() != null) {
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                }
            }
            if (requiredModule.isAsyncEvaluating()) {
                moduleRecord.incPendingAsyncDependencies();
                requiredModule.appendAsyncParentModules(moduleRecord);
            }
        }
        if (moduleRecord.getPendingAsyncDependencies() > 0) {
            moduleRecord.setAsyncEvaluating(true);
        } else if (moduleRecord.isTopLevelAsync()) {
            moduleAsyncExecution(realm, moduleRecord);
        } else {
            Object result = moduleExecution(realm, moduleRecord, null);
            moduleRecord.setExecutionResult(result);
        }

        assert occursExactlyOnce(moduleRecord, stack);
        assert moduleRecord.getDFSAncestorIndex() <= moduleRecord.getDFSIndex();
        if (moduleRecord.getDFSAncestorIndex() == moduleRecord.getDFSIndex()) {
            while (true) {
                JSModuleRecord requiredModule = stack.pop();
                requiredModule.setStatus(Status.Evaluated);
                if (requiredModule.equals(moduleRecord)) {
                    break;
                }
            }
        }
        return index;
    }

    @TruffleBoundary
    private static void moduleAsyncExecution(JSRealm realm, JSModuleRecord module) {
        // ExecuteAsyncModule ( module )
        assert module.getStatus() == Status.Evaluating || module.getStatus() == Status.Evaluated;
        assert module.isTopLevelAsync();
        module.setAsyncEvaluating(true);
        PromiseCapabilityRecord capability = NewPromiseCapabilityNode.createDefault(realm);
        DynamicObject onFulfilled = createCallAsyncModuleFulfilled(realm, module);
        DynamicObject onRejected = createCallAsyncModuleRejected(realm, module);
        Object then = JSObject.get(capability.getPromise(), "then");
        JSFunction.call(JSArguments.create(capability.getPromise(), then, onFulfilled, onRejected));
        moduleExecution(realm, module, capability);
    }

    @TruffleBoundary
    private static DynamicObject createCallAsyncModuleFulfilled(JSRealm realm, JSModuleRecord module) {
        // AsyncModuleExecutionFulfilled ( module )
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncModuleExecutionFulfilled, (c) -> createCallAsyncModuleFulfilledImpl(c));
        DynamicObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(function, STORE_MODULE_KEY, module);
        return function;
    }

    private static JSFunctionData createCallAsyncModuleFulfilledImpl(JSContext context) {
        // AsyncModuleExecutionFulfilled ( module )
        class AsyncModuleFulfilledRoot extends JavaScriptRootNode {
            @Child private JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getModule = PropertyGetNode.createGetHidden(STORE_MODULE_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object dynamicImportResolutionResult = argumentNode.execute(frame);
                Object module = getModule.getValue(JSArguments.getFunctionObject(frame.getArguments()));
                return asyncModuleExecutionFulfilled(getRealm(), (JSModuleRecord) module, dynamicImportResolutionResult);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new AsyncModuleFulfilledRoot());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    @TruffleBoundary
    private static DynamicObject createCallAsyncModuleRejected(JSRealm realm, JSModuleRecord module) {
        // AsyncModuleExecutionRejected ( module )
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncModuleExecutionRejected, (c) -> createCallAsyncModuleRejectedImpl(c));
        DynamicObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(function, STORE_MODULE_KEY, module);
        return function;
    }

    private static JSFunctionData createCallAsyncModuleRejectedImpl(JSContext context) {
        // AsyncModuleExecutionRejected ( module )
        class AsyncModuleExecutionRejectedRoot extends JavaScriptRootNode {
            @Child private PropertyGetNode getModule = PropertyGetNode.createGetHidden(STORE_MODULE_KEY, context);
            @Child private PropertyGetNode getRejectionError = PropertyGetNode.createGetHidden(JSPromise.PROMISE_RESULT, context);

            @Override
            public Object execute(VirtualFrame frame) {
                JSModuleRecord module = (JSModuleRecord) getModule.getValue(JSArguments.getFunctionObject(frame.getArguments()));
                Object resolvedPromise = module.getExecutionContinuation();
                assert JSPromise.isJSPromise(resolvedPromise);
                assert JSPromise.isRejected((DynamicObject) resolvedPromise);
                Object reaction = getRejectionError.getValue(resolvedPromise);
                return asyncModuleExecutionRejected(getRealm(), module, reaction);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new AsyncModuleExecutionRejectedRoot());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    @TruffleBoundary
    private static Object asyncModuleExecutionFulfilled(JSRealm realm, JSModuleRecord module, Object dynamicImportResolutionResult) {
        assert module.getStatus() == Status.Evaluated;
        if (!module.isAsyncEvaluating()) {
            assert module.getEvaluationError() != null;
            return Undefined.instance;
        }
        assert module.getEvaluationError() == null;
        module.setAsyncEvaluating(false);
        for (JSModuleRecord m : module.getAsyncParentModules()) {
            if (module.getDFSIndex() != module.getDFSAncestorIndex()) {
                assert m.getDFSAncestorIndex() <= module.getDFSAncestorIndex();
            }
            m.decPendingAsyncDependencies();
            if (m.getPendingAsyncDependencies() == 0 && m.getEvaluationError() == null) {
                assert m.isAsyncEvaluating();
                JSModuleRecord cycleRoot = getAsyncCycleRoot(m);
                if (cycleRoot.getEvaluationError() != null) {
                    return Undefined.instance;
                }
                if (m.isTopLevelAsync()) {
                    moduleAsyncExecution(realm, m);
                } else {
                    try {
                        moduleExecution(realm, m, null);
                        asyncModuleExecutionFulfilled(realm, m, dynamicImportResolutionResult);
                    } catch (Exception e) {
                        asyncModuleExecutionRejected(realm, m, e);
                    }
                }
            }
        }
        if (module.getTopLevelCapability() != null) {
            assert module.getDFSIndex() == module.getDFSAncestorIndex();
            JSFunction.call(JSArguments.create(Undefined.instance, module.getTopLevelCapability().getResolve(), dynamicImportResolutionResult));
        }
        return Undefined.instance;
    }

    @TruffleBoundary
    private static Object asyncModuleExecutionRejected(JSRealm realm, JSModuleRecord module, Object error) {
        assert error != null : "Cannot reject a module creation with null error";
        assert module.getStatus() == Status.Evaluated;
        if (!module.isAsyncEvaluating()) {
            assert module.getEvaluationError() != null;
            return Undefined.instance;
        }
        assert module.getEvaluationError() == null;
        module.setEvaluationError(JSRuntime.getException(error));
        module.setAsyncEvaluating(false);
        for (JSModuleRecord m : module.getAsyncParentModules()) {
            if (module.getDFSIndex() != module.getDFSAncestorIndex()) {
                assert m.getDFSAncestorIndex() == module.getDFSAncestorIndex();
            }
            asyncModuleExecutionRejected(realm, m, error);
        }
        if (module.getTopLevelCapability() != null) {
            assert module.getDFSIndex() == module.getDFSAncestorIndex();
            JSFunction.call((DynamicObject) module.getTopLevelCapability().getReject(), Undefined.instance, new Object[]{error});
        }
        return Undefined.instance;
    }

    private static Object moduleExecution(JSRealm realm, JSModuleRecord moduleRecord, PromiseCapabilityRecord capability) {
        if (!moduleRecord.isTopLevelAsync()) {
            assert capability == null;
            return JSFunction.call(JSArguments.create(Undefined.instance, JSFunction.create(realm, moduleRecord.getFunctionData()), moduleRecord));
        } else {
            Object asyncFunctionResultPromise = JSFunction.call(JSArguments.create(Undefined.instance, JSFunction.create(realm, moduleRecord.getFunctionData()), moduleRecord, capability));
            moduleRecord.setExecutionContinuation(asyncFunctionResultPromise);
            return asyncFunctionResultPromise;
        }
    }

    private static boolean occursExactlyOnce(JSModuleRecord moduleRecord, Collection<JSModuleRecord> stack) {
        return stack.stream().filter(moduleRecord::equals).count() == 1;
    }

    @Override
    public ScriptNode parseScript(JSContext context, Source source, ByteBuffer binary) {
        return ScriptNode.fromFunctionRoot(context, (FunctionRootNode) new BinarySnapshotProvider(binary).apply(NodeFactory.getInstance(context), context, source));
    }

    @Override
    public ScriptNode parseScript(JSContext context, Source source, SnapshotProvider snapshotProvider) {
        return ScriptNode.fromFunctionRoot(context, (FunctionRootNode) snapshotProvider.apply(NodeFactory.getInstance(context), context, source));
    }

    @Override
    public JavaScriptNode parseInlineScript(JSContext context, Source source, MaterializedFrame lexicalContextFrame, boolean isStrict, Node locationNode) {
        Environment env;
        Object scope;
        try {
            scope = NodeLibrary.getUncached().getScope(locationNode, lexicalContextFrame, true);
            env = new DebugEnvironment(null, NodeFactory.getInstance(context), context, scope);
        } catch (UnsupportedMessageException e) {
            scope = null;
            env = null;
        }
        ScriptNode script = JavaScriptTranslator.translateInlineScript(NodeFactory.getInstance(context), context, env, source, isStrict);
        return createInlineScriptCallNode(context, script.getFunctionData(), script.getCallTarget(), locationNode);
    }

    private static JavaScriptNode createInlineScriptCallNode(JSContext context, JSFunctionData functionData, RootCallTarget callTarget, Node locationNode) {
        return new JavaScriptNode() {
            @Child private DirectCallNode callNode = DirectCallNode.create(callTarget);
            @Child private PropertySetNode setScopeNode = PropertySetNode.createSetHidden(JSFunction.DEBUG_SCOPE_ID, context);
            @Child private NodeLibrary nodeLibrary = NodeLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject closure = JSFunction.create(getRealm(), functionData);
                try {
                    Object scope = nodeLibrary.getScope(locationNode, frame, true);
                    setScopeNode.setValue(closure, scope);
                } catch (UnsupportedMessageException e) {
                    // ignore
                }
                return callNode.call(JSArguments.createZeroArg(JSFrameUtil.getThisObj(frame), closure));
            }
        };
    }

    @Override
    public Expression parseExpression(JSContext context, String sourceString) {
        return GraalJSParserHelper.parseExpression(context, Source.newBuilder(JavaScriptLanguage.ID, sourceString, "<unknown>").build(), context.getParserOptions());
    }

}
