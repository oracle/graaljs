/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.JSON_MIME_TYPE;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.JSON_SOURCE_NAME_SUFFIX;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_SOURCE_NAME_SUFFIX;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ImportPhase;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.promise.ImportCallNode;
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
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.CyclicModuleRecord;
import com.oracle.truffle.js.runtime.objects.CyclicModuleRecord.Status;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.objects.WebAssemblyModuleRecord;

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
    public ScriptNode parseEval(JSContext context, Node lastNode, Source source, ScriptOrModule activeScriptOrModule) {
        return parseEval(context, lastNode, source, false, null, activeScriptOrModule);
    }

    /**
     * Evaluate Function(parameterList, body).
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    @Override
    public ScriptNode parseFunction(JSContext context, String parameterList, String body, boolean generatorFunction, boolean asyncFunction, String sourceName, ScriptOrModule activeScriptOrModule) {
        String wrappedBody = "\n" + body + "\n";
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
        code.append(' ');
        boolean nashornCompat = context.getEcmaScriptVersion() == 5 && context.isOptionNashornCompatibilityMode();
        if (!nashornCompat) {
            code.append("anonymous");
        }
        code.append('(');
        code.append(parameterList);
        if (!nashornCompat) {
            code.append('\n');
        }
        code.append(") {");
        code.append(wrappedBody);
        code.append("})");
        Source source = Source.newBuilder(JavaScriptLanguage.ID, code.toString(), sourceName).cached(false).build();
        return parseEval(context, null, source, false, null, activeScriptOrModule);
    }

    /**
     * Evaluate direct eval.
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    @Override
    public ScriptNode parseDirectEval(JSContext context, Node lastNode, Source source, Object evalEnv) {
        DirectEvalContext directEval = (DirectEvalContext) evalEnv;
        return parseEval(context, lastNode, source, directEval.env.isStrictMode(), directEval, directEval.activeScriptOrModule);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static ScriptNode parseEval(JSContext context, Node lastNode, Source source, boolean isStrict, DirectEvalContext directEval, ScriptOrModule activeScriptOrModule) {
        context.checkEvalAllowed();
        NodeFactory nodeFactory = NodeFactory.getInstance(context);
        try {
            return JavaScriptTranslator.translateEvalScript(nodeFactory, context, source, isStrict, directEval, activeScriptOrModule);
        } catch (com.oracle.js.parser.ParserException e) {
            throw parserToJSError(lastNode, e, context);
        }
    }

    private static JSException parserToJSError(Node lastNode, com.oracle.js.parser.ParserException e, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        String message;
        if (context.isOptionV8CompatibilityMode()) {
            message = e.getRawMessage();
        } else {
            message = e.getMessage();
        }
        message = message.replace("\r\n", "\n");
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
            throw Errors.createSyntaxError(e, context);
        }
    }

    // JSParser methods below

    @TruffleBoundary
    @Override
    public ScriptNode parseScript(JSContext context, Source source, String prolog, String epilog, boolean isStrict, List<String> argumentNames) {
        if (isModuleSource(source)) {
            return fakeScriptForModule(context, source);
        }
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, source, isStrict, prolog, epilog, argumentNames);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e, context);
        }
    }

    private static boolean isModuleSource(Source source) {
        String mimeType = source.getMimeType();
        return MODULE_MIME_TYPE.equals(mimeType) || (mimeType == null && source.getName().endsWith(MODULE_SOURCE_NAME_SUFFIX));
    }

    private ScriptNode fakeScriptForModule(JSContext context, Source source) {
        JSModuleData parsedModule = parseModule(context, source);
        RootNode rootNode = new ModuleScriptRoot(context, parsedModule, source);
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, rootNode.getCallTarget(), 0, Strings.EMPTY_STRING);
        return ScriptNode.fromFunctionData(functionData);
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
            JSModuleRecord moduleRecord = new JSModuleRecord(parsedModule, realm.getModuleLoader());
            ModuleRequest moduleRequest = ModuleRequest.create(Strings.fromJavaString(source.getName()));
            realm.getModuleLoader().addLoadedModule(moduleRequest, moduleRecord);

            moduleRecord.loadRequestedModulesSync(realm, Undefined.instance);
            // Note: If loading failed, we must not perform module linking.

            moduleRecord.link(realm);
            // On failure, an exception is thrown and this module's [[Status]] remains unlinked.

            Object promise = moduleRecord.evaluate(realm);
            if (context.isOptionTopLevelAwait()) {
                JSFunctionObject onRejected = createTopLevelAwaitReject(context, realm);
                JSFunctionObject onAccepted = createTopLevelAwaitResolve(context, realm);
                // Non-standard: throw error from onRejected handler.
                performPromiseThenNode.execute((JSPromiseObject) promise, onAccepted, onRejected, null);
            }

            if (context.getLanguageOptions().esmEvalReturnsExports()) {
                JSDynamicObject moduleNamespace = moduleRecord.getModuleNamespace();
                assert moduleNamespace != null;
                return moduleNamespace;
            } else if (context.isOptionTopLevelAwait() && moduleRecord.isAsyncEvaluation()) {
                return promise;
            } else {
                return moduleRecord.getExecutionResultOrThrow();
            }
        }

        JSModuleData getModuleData() {
            return parsedModule;
        }
    }

    private static JSFunctionObject createTopLevelAwaitReject(JSContext context, JSRealm realm) {
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
        return JSFunctionData.createCallOnly(context, new TopLevelAwaitRejectedRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static JSFunctionObject createTopLevelAwaitResolve(JSContext context, JSRealm realm) {
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
        return JSFunctionData.createCallOnly(context, new TopLevelAwaitFulfilledRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    @Override
    public ScriptNode parseScript(JSContext context, String sourceCode) {
        try {
            return JavaScriptTranslator.translateScript(NodeFactory.getInstance(context), context, Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "<unknown>").build(), false, "", "");
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e, context);
        }
    }

    @TruffleBoundary
    @Override
    public Integer[] parseDate(JSRealm realm, String date, boolean extraLenient) {
        DateParser dateParser = new DateParser(realm, date, extraLenient);
        return dateParser.parse() ? dateParser.getDateFields() : null;
    }

    @Override
    public String parseToJSON(JSContext context, String code, String name, boolean includeLoc) {
        return GraalJSParserHelper.parseToJSON(code, name, includeLoc, context);
    }

    @Override
    public Object getDefaultNodeFactory() {
        return NodeFactory.getDefaultInstance();
    }

    /**
     * Parses source to intermediate AST and returns a closure for the translation to Truffle AST.
     */
    public static Supplier<ScriptNode> internalParseForTiming(JSContext context, Source source) {
        com.oracle.js.parser.ir.FunctionNode ast = GraalJSParserHelper.parseScript(context, source, context.getParserOptions());
        return () -> JavaScriptTranslator.translateFunction(NodeFactory.getInstance(context), context, null, source, 0, false, ast);
    }

    @TruffleBoundary
    @Override
    public JSModuleData parseModule(JSContext context, Source source) {
        try {
            return JavaScriptTranslator.translateModule(NodeFactory.getInstance(context), context, source);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e, context);
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
    public AbstractModuleRecord parseWasmModuleSource(JSRealm realm, Source source) {
        assert realm.getContextOptions().isWebAssembly();
        Object compiledModule = WebAssemblyBuiltins.moduleDecode(realm, source);
        JSWebAssemblyModuleObject wasmModule = JSWebAssemblyModule.create(realm.getContext(), realm, compiledModule, source);
        return new WebAssemblyModuleRecord(realm.getContext(), source, wasmModule);
    }

    @TruffleBoundary
    @Override
    public JSModuleRecord parseJSONModule(JSRealm realm, Source source) {
        assert JSON_MIME_TYPE.equals(source.getMimeType()) || (source.getMimeType() == null && source.getName().endsWith(JSON_SOURCE_NAME_SUFFIX)) : source;
        Object json = JSFunction.call(JSArguments.createOneArg(Undefined.instance, realm.getJsonParseFunctionObject(), Strings.fromJavaString(source.getCharacters().toString())));
        return createSyntheticJSONModule(realm, source, json);
    }

    private static JSModuleRecord createSyntheticJSONModule(JSRealm realm, Source source, Object hostDefined) {
        final TruffleString exportName = Strings.DEFAULT;
        JSFrameDescriptor frameDescBuilder = new JSFrameDescriptor(Undefined.instance);
        JSFrameSlot slot = frameDescBuilder.addFrameSlot(exportName);
        FrameDescriptor frameDescriptor = frameDescBuilder.toFrameDescriptor();
        List<ExportEntry> localExportEntries = Collections.singletonList(ExportEntry.exportSpecifier(exportName));
        Module moduleNode = new Module(Collections.emptyList(), Collections.emptyList(), localExportEntries, Collections.emptyList(), Collections.emptyList(), null, null);
        JavaScriptRootNode rootNode = new JavaScriptRootNode(realm.getContext().getLanguage(), source.createUnavailableSection(), frameDescriptor) {
            private final int defaultSlot = slot.getIndex();

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
        CallTarget callTarget = rootNode.getCallTarget();
        JSFunctionData functionData = JSFunctionData.create(realm.getContext(), callTarget, callTarget, 0, Strings.EMPTY_STRING, false, false, true, true);
        final JSModuleData parseModule = new JSModuleData(moduleNode, source, functionData, frameDescriptor);
        return new JSModuleRecord(parseModule, realm.getModuleLoader(), hostDefined);
    }

    @TruffleBoundary
    private static AbstractModuleRecord hostResolveImportedModule(JSRealm realm, ScriptOrModule referrer, ModuleRequest moduleRequest) {
        JSModuleLoader moduleLoader = referrer instanceof JSModuleRecord ? ((JSModuleRecord) referrer).getModuleLoader() : realm.getModuleLoader();
        return moduleLoader.resolveImportedModule(referrer, filterSupportedImportAttributes(realm.getContext(), moduleRequest));
    }

    private static ModuleRequest filterSupportedImportAttributes(JSContext context, ModuleRequest moduleRequest) {
        if (moduleRequest.attributes().isEmpty() || context.getSupportedImportAttributes().containsAll(moduleRequest.attributes().keySet())) {
            return moduleRequest;
        }
        Map<TruffleString, TruffleString> supportedAttributes = new HashMap<>();
        for (Map.Entry<TruffleString, TruffleString> attributes : moduleRequest.attributes().entrySet()) {
            TruffleString key = attributes.getKey();
            TruffleString value = attributes.getValue();
            if (context.getSupportedImportAttributes().contains(key)) {
                supportedAttributes.put(key, value);
            }
        }
        return moduleRequest.withAttributes(supportedAttributes);
    }

    private static class GraphLoadingState {
        boolean isLoading;
        int pendingModulesCount;
        PromiseCapabilityRecord promiseCapability;
        Object hostDefined;
        Set<CyclicModuleRecord> visited;

        GraphLoadingState(PromiseCapabilityRecord promiseCapability, Object hostDefined) {
            this.isLoading = true;
            this.pendingModulesCount = 1;
            this.promiseCapability = promiseCapability;
            this.hostDefined = hostDefined;
            this.visited = new HashSet<>();
        }
    }

    @TruffleBoundary
    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, CyclicModuleRecord moduleRecord, Object hostDefined) {
        PromiseCapabilityRecord pc = NewPromiseCapabilityNode.createDefault(realm);
        GraphLoadingState state = new GraphLoadingState(pc, hostDefined);
        try {
            innerModuleLoading(realm, state, moduleRecord, true);
        } catch (AbstractTruffleException e) {
            assert false : e; // should not throw
            throw e;
        }
        return (JSPromiseObject) pc.getPromise();
    }

    private void innerModuleLoading(JSRealm realm, GraphLoadingState state, AbstractModuleRecord module, boolean recursiveLoad) {
        assert state.isLoading;
        if (recursiveLoad && module instanceof CyclicModuleRecord moduleRecord && moduleRecord.getStatus() == Status.New &&
                        !state.visited.contains(moduleRecord)) {
            state.visited.add(moduleRecord);
            int requestedModuleCount = moduleRecord.getRequestedModules().size();
            state.pendingModulesCount += requestedModuleCount;
            for (ModuleRequest required : moduleRecord.getRequestedModules()) {
                AbstractModuleRecord resolved = moduleRecord.getLoadedModule(realm, required);
                if (resolved != null) {
                    boolean innerRecursiveLoad = required.phase() != ImportPhase.Source;
                    innerModuleLoading(realm, state, resolved, innerRecursiveLoad);
                } else {
                    hostLoadImportedModule(realm, moduleRecord, required, state.hostDefined, state);
                }
                if (!state.isLoading) {
                    return;
                }
            }
        }
        assert state.pendingModulesCount >= 1 : state.pendingModulesCount;
        if (--state.pendingModulesCount == 0) {
            state.isLoading = false;
            for (var loaded : state.visited) {
                if (loaded.getStatus() == Status.New) {
                    loaded.setUnlinked();
                }
            }
            JSFunction.call(JSArguments.createOneArg(Undefined.instance, state.promiseCapability.getResolve(), Undefined.instance));
        }
    }

    /**
     * HostLoadImportedModule takes arguments referrer (a Script Record, a Cyclic Module Record, or
     * a Realm Record), specifier (a String), hostDefined (anything), and payload (a
     * GraphLoadingState Record or a PromiseCapability Record) and returns unused.
     *
     * The host environment must perform FinishLoadingImportedModule(referrer, specifier, payload,
     * result), where result is either a normal completion containing the loaded Module Record or a
     * throw completion, either synchronously or asynchronously.
     */
    @TruffleBoundary
    @Override
    public void hostLoadImportedModule(JSRealm realm, ScriptOrModule referrer, ModuleRequest moduleRequest, Object hostDefined, Object payload) {
        Completion moduleCompletion;
        try {
            AbstractModuleRecord module = hostResolveImportedModule(realm, referrer, moduleRequest);
            moduleCompletion = Completion.forNormal(module);
        } catch (AbstractTruffleException e) {
            moduleCompletion = Completion.forThrow(getErrorObject(e));
        }
        finishLoadingImportedModule(realm, referrer, moduleRequest, payload, moduleCompletion);
    }

    private void finishLoadingImportedModule(JSRealm realm, ScriptOrModule referrer, ModuleRequest moduleRequest, Object payload, Completion moduleCompletion) {
        if (moduleCompletion.isNormal()) {
            AbstractModuleRecord moduleResult = (AbstractModuleRecord) moduleCompletion.getValue();
            AbstractModuleRecord existing = referrer != null
                            ? referrer.addLoadedModule(realm, moduleRequest, moduleResult)
                            : realm.getModuleLoader().addLoadedModule(moduleRequest, moduleResult);
            if (existing != null) {
                assert existing == moduleResult;
            }
        }
        if (payload instanceof GraphLoadingState state) {
            continueModuleLoading(realm, state, moduleRequest.phase(), moduleCompletion);
        } else {
            continueDynamicImport(payload, moduleRequest.phase(), moduleCompletion);
        }
    }

    private void continueModuleLoading(JSRealm realm, GraphLoadingState state, ImportPhase phase, Completion moduleCompletion) {
        if (!state.isLoading) {
            return;
        }
        if (moduleCompletion.isNormal()) {
            boolean recursiveLoad = phase != ImportPhase.Source;
            innerModuleLoading(realm, state, (AbstractModuleRecord) moduleCompletion.getValue(), recursiveLoad);
        } else {
            state.isLoading = false;
            JSFunction.call(JSArguments.createOneArg(Undefined.instance, state.promiseCapability.getReject(), moduleCompletion.getValue()));
        }
    }

    private static void continueDynamicImport(Object payload, ImportPhase phase, Completion moduleCompletion) {
        var continuation = (ImportCallNode.ContinueDynamicImportPayload) payload;
        PromiseCapabilityRecord promiseCapability = continuation.promiseCapability();
        if (moduleCompletion.isAbrupt()) {
            JSFunction.call(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), moduleCompletion.getValue()));
            return;
        }

        AbstractModuleRecord module = (AbstractModuleRecord) moduleCompletion.getValue();
        if (phase == ImportPhase.Source) {
            try {
                Object moduleSource = module.getModuleSource();
                JSFunction.call(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), moduleSource));
            } catch (AbstractTruffleException e) {
                JSFunction.call(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), getErrorObject(e)));
            }
            return;
        }

        // Perform the remaining steps of ContinueDynamicImport.
        JSFunction.call(JSArguments.create(Undefined.instance, continuation.continueDynamicImportCallback(), promiseCapability, module));
    }

    @TruffleBoundary
    @Override
    public void moduleLinking(JSRealm realm, CyclicModuleRecord moduleRecord) {
        assert moduleRecord.getStatus() != Status.Linking && moduleRecord.getStatus() != Status.Evaluating && moduleRecord.getStatus() != Status.New : moduleRecord.getStatus();
        Deque<CyclicModuleRecord> stack = new ArrayDeque<>(4);

        try {
            innerModuleLinking(realm, moduleRecord, stack, 0);
        } catch (AbstractTruffleException e) {
            handleModuleLinkingError(moduleRecord, stack);
            throw e;
        }

        assert moduleRecord.getStatus() == Status.Linked || moduleRecord.getStatus() == Status.EvaluatingAsync || moduleRecord.getStatus() == Status.Evaluated;
        assert stack.isEmpty();
    }

    private static void handleModuleLinkingError(CyclicModuleRecord moduleRecord, Deque<CyclicModuleRecord> stack) {
        for (CyclicModuleRecord m : stack) {
            assert m.getStatus() == Status.Linking;
            m.setUnlinked();
        }
        assert moduleRecord.getStatus() == Status.Unlinked;
    }

    private int innerModuleLinking(JSRealm realm, AbstractModuleRecord abstractModule, Deque<CyclicModuleRecord> stack, int index0) {
        int index = index0;
        if (!(abstractModule instanceof CyclicModuleRecord moduleRecord)) {
            abstractModule.link(realm);
            return index;
        }
        if (moduleRecord.getStatus() == Status.Linking || moduleRecord.getStatus() == Status.Linked || moduleRecord.getStatus() == Status.EvaluatingAsync ||
                        moduleRecord.getStatus() == Status.Evaluated) {
            return index;
        }
        assert moduleRecord.getStatus() == Status.Unlinked : moduleRecord.getStatus();
        moduleRecord.setStatus(Status.Linking);
        moduleRecord.setDFSIndex(index);
        moduleRecord.setDFSAncestorIndex(index);
        index++;
        stack.push(moduleRecord);

        for (ModuleRequest required : moduleRecord.getRequestedModules()) {
            if (required.phase() != ImportPhase.Evaluation) {
                continue;
            }
            AbstractModuleRecord requiredAbstractModule = moduleRecord.getImportedModule(required);
            index = innerModuleLinking(realm, requiredAbstractModule, stack, index);
            if (requiredAbstractModule instanceof CyclicModuleRecord requiredModule) {
                assert requiredModule.getStatus() == Status.Linking || requiredModule.getStatus() == Status.Linked ||
                                requiredModule.getStatus() == Status.EvaluatingAsync || requiredModule.getStatus() == Status.Evaluated : requiredModule.getStatus();
                assert (requiredModule.getStatus() == Status.Linking) == stack.contains(requiredModule);
                if (requiredModule.getStatus() == Status.Linking) {
                    moduleRecord.setDFSAncestorIndex(Math.min(moduleRecord.getDFSAncestorIndex(), requiredModule.getDFSAncestorIndex()));
                }
            }
        }
        moduleRecord.initializeEnvironment(realm);

        assert occursExactlyOnce(moduleRecord, stack);
        assert moduleRecord.getDFSAncestorIndex() <= moduleRecord.getDFSIndex();
        if (moduleRecord.getDFSAncestorIndex() == moduleRecord.getDFSIndex()) {
            while (true) {
                CyclicModuleRecord requiredModule = stack.pop();
                requiredModule.setStatus(Status.Linked);
                if (requiredModule.equals(moduleRecord)) {
                    break;
                }
            }
        }
        return index;
    }

    @TruffleBoundary
    @Override
    public Object moduleEvaluation(JSRealm realm, CyclicModuleRecord moduleRecord) {
        // Evaluate ( ) Concrete Method
        CyclicModuleRecord module = moduleRecord;
        Deque<CyclicModuleRecord> stack = new ArrayDeque<>(4);
        if (realm.getContext().isOptionTopLevelAwait()) {
            assert module.getStatus() == Status.Linked || module.getStatus() == Status.EvaluatingAsync || module.getStatus() == Status.Evaluated : module.getStatus();
            if (module.getStatus() == Status.EvaluatingAsync || module.getStatus() == Status.Evaluated) {
                module = module.getCycleRoot();
            }
            if (module.getTopLevelCapability() != null) {
                return module.getTopLevelCapability().getPromise();
            }
            PromiseCapabilityRecord capability = NewPromiseCapabilityNode.createDefault(realm);
            module.setTopLevelCapability(capability);
            try {
                innerModuleEvaluation(realm, module, stack, 0);
                assert module.getStatus() == Status.EvaluatingAsync || module.getStatus() == Status.Evaluated;
                assert module.getEvaluationError() == null;
                if (!module.isAsyncEvaluation()) {
                    assert module.getStatus() == Status.Evaluated;
                    JSFunction.call(JSArguments.create(Undefined.instance, capability.getResolve(), Undefined.instance));
                }
                assert stack.isEmpty();
            } catch (AbstractTruffleException e) {
                handleModuleEvaluationError(module, stack, e);
            }
            return capability.getPromise();
        } else {
            try {
                innerModuleEvaluation(realm, module, stack, 0);
            } catch (AbstractTruffleException e) {
                handleModuleEvaluationError(module, stack, e);
                throw e;
            }
            assert module.getStatus() == Status.EvaluatingAsync || module.getStatus() == Status.Evaluated;
            assert module.getEvaluationError() == null;

            assert stack.isEmpty();
            Object result = module.getExecutionResult();
            return result == null ? Undefined.instance : result;
        }
    }

    private static void handleModuleEvaluationError(CyclicModuleRecord module, Deque<CyclicModuleRecord> stack, AbstractTruffleException e) {
        for (CyclicModuleRecord m : stack) {
            assert m.getStatus() == Status.Evaluating;
            m.setStatus(Status.Evaluated);
            m.setEvaluationError(e);
        }
        assert module.getStatus() == Status.Evaluated && module.getEvaluationError() == e;

        PromiseCapabilityRecord capability = module.getTopLevelCapability();
        if (capability != null) {
            JSFunction.call(JSArguments.create(Undefined.instance, capability.getReject(), getErrorObject(e)));
        }
    }

    private static Object getErrorObject(AbstractTruffleException e) {
        if (e instanceof GraalJSException jsex) {
            return jsex.getErrorObject();
        }
        return e;
    }

    @TruffleBoundary
    private int innerModuleEvaluation(JSRealm realm, AbstractModuleRecord abstractModule, Deque<CyclicModuleRecord> stack, int index0) {
        // InnerModuleEvaluation( module, stack, index )
        int index = index0;
        if (!(abstractModule instanceof CyclicModuleRecord moduleRecord)) {
            Object result = abstractModule.evaluate(realm);
            if (result instanceof JSPromiseObject promise) {
                assert !JSPromise.isPending(promise);
                if (JSPromise.isRejected(promise)) {
                    throw JSRuntime.getException(JSPromise.getPromiseResult(promise));
                }
            }
            return index;
        }
        if (moduleRecord.getStatus() == Status.EvaluatingAsync || moduleRecord.getStatus() == Status.Evaluated) {
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

        for (ModuleRequest required : moduleRecord.getRequestedModules()) {
            if (required.phase() != ImportPhase.Evaluation) {
                continue;
            }
            AbstractModuleRecord requiredAbstractModule = moduleRecord.getImportedModule(required);
            // Note: Link must have completed successfully prior to invoking this method,
            // so every requested module is guaranteed to resolve successfully.
            index = innerModuleEvaluation(realm, requiredAbstractModule, stack, index);
            if (requiredAbstractModule instanceof CyclicModuleRecord requiredModule) {
                assert requiredModule.getStatus() == Status.Evaluating || requiredModule.getStatus() == Status.EvaluatingAsync ||
                                requiredModule.getStatus() == Status.Evaluated : requiredModule.getStatus();
                assert (requiredModule.getStatus() == Status.Evaluating) == stack.contains(requiredModule);
                if (requiredModule.getStatus() == Status.Evaluating) {
                    moduleRecord.setDFSAncestorIndex(Math.min(moduleRecord.getDFSAncestorIndex(), requiredModule.getDFSAncestorIndex()));
                } else {
                    requiredModule = requiredModule.getCycleRoot();
                    assert requiredModule.getStatus() == Status.EvaluatingAsync || requiredModule.getStatus() == Status.Evaluated;
                    if (requiredModule.getEvaluationError() != null) {
                        throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                    }
                }
                if (requiredModule.isAsyncEvaluation()) {
                    moduleRecord.incPendingAsyncDependencies();
                    requiredModule.appendAsyncParentModules(moduleRecord);
                }
            }
        }
        if (moduleRecord.getPendingAsyncDependencies() > 0 || moduleRecord.hasTLA()) {
            assert !moduleRecord.isAsyncEvaluation();
            moduleRecord.setAsyncEvaluation(true);
            moduleRecord.setAsyncEvaluatingOrder(realm.nextAsyncEvaluationOrder());
            /*
             * NOTE: The order in which module records have their [[AsyncEvaluation]] fields
             * transition to true is significant.
             */
            if (moduleRecord.getPendingAsyncDependencies() == 0) {
                moduleAsyncExecution(realm, moduleRecord);
            }
        } else {
            Object result = moduleRecord.executeModule(realm, (PromiseCapabilityRecord) null);
            moduleRecord.setExecutionResult(result);
        }

        assert occursExactlyOnce(moduleRecord, stack);
        assert moduleRecord.getDFSAncestorIndex() <= moduleRecord.getDFSIndex();
        if (moduleRecord.getDFSAncestorIndex() == moduleRecord.getDFSIndex()) {
            while (true) {
                CyclicModuleRecord requiredModule = stack.pop();
                if (!requiredModule.isAsyncEvaluation()) {
                    requiredModule.setStatus(Status.Evaluated);
                } else {
                    requiredModule.setStatus(Status.EvaluatingAsync);
                }
                requiredModule.setCycleRoot(moduleRecord);
                if (requiredModule.equals(moduleRecord)) {
                    break;
                }
            }
        }
        return index;
    }

    @TruffleBoundary
    private static void moduleAsyncExecution(JSRealm realm, CyclicModuleRecord module) {
        // ExecuteAsyncModule ( module )
        assert module.getStatus() == Status.Evaluating || module.getStatus() == Status.EvaluatingAsync;
        assert module.hasTLA();
        PromiseCapabilityRecord capability = NewPromiseCapabilityNode.createDefault(realm);
        JSFunctionObject onFulfilled = createCallAsyncModuleFulfilled(realm, module);
        JSFunctionObject onRejected = createCallAsyncModuleRejected(realm, module);
        Object then = JSObject.get(capability.getPromise(), Strings.THEN);
        JSFunction.call(JSArguments.create(capability.getPromise(), then, onFulfilled, onRejected));
        module.executeModule(realm, capability);
    }

    @TruffleBoundary
    private static JSFunctionObject createCallAsyncModuleFulfilled(JSRealm realm, CyclicModuleRecord module) {
        // AsyncModuleExecutionFulfilled ( module )
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncModuleExecutionFulfilled, (c) -> createAsyncModuleExecutionFulfilledImpl(c));
        JSFunctionObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(function, STORE_MODULE_KEY, module);
        return function;
    }

    private static JSFunctionData createAsyncModuleExecutionFulfilledImpl(JSContext context) {
        // AsyncModuleExecutionFulfilled ( module )
        class AsyncModuleFulfilledRoot extends JavaScriptRootNode {
            @Child private PropertyGetNode getModule = PropertyGetNode.createGetHidden(STORE_MODULE_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object module = getModule.getValue(JSArguments.getFunctionObject(frame.getArguments()));
                return asyncModuleExecutionFulfilled(getRealm(), (JSModuleRecord) module);
            }
        }
        return JSFunctionData.createCallOnly(context, new AsyncModuleFulfilledRoot().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    @TruffleBoundary
    private static JSFunctionObject createCallAsyncModuleRejected(JSRealm realm, CyclicModuleRecord module) {
        // AsyncModuleExecutionRejected ( module )
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncModuleExecutionRejected, (c) -> createAsyncModuleExecutionRejectedImpl(c));
        JSFunctionObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(function, STORE_MODULE_KEY, module);
        return function;
    }

    private static JSFunctionData createAsyncModuleExecutionRejectedImpl(JSContext context) {
        // AsyncModuleExecutionRejected ( module, error )
        class AsyncModuleExecutionRejectedRoot extends JavaScriptRootNode {
            @Child private PropertyGetNode getModule = PropertyGetNode.createGetHidden(STORE_MODULE_KEY, context);
            @Child private JavaScriptNode errorArgument = AccessIndexedArgumentNode.create(0);

            @Override
            public Object execute(VirtualFrame frame) {
                JSModuleRecord module = (JSModuleRecord) getModule.getValue(JSArguments.getFunctionObject(frame.getArguments()));
                Object error = errorArgument.execute(frame);
                return asyncModuleExecutionRejected(getRealm(), module, error);
            }
        }
        return JSFunctionData.createCallOnly(context, new AsyncModuleExecutionRejectedRoot().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static void gatherAvailableAncestors(CyclicModuleRecord module, Set<CyclicModuleRecord> execList) {
        // GatherAvailableAncestors ( module, execList )
        for (CyclicModuleRecord m : module.getAsyncParentModules()) {
            if (!execList.contains(m) && m.getCycleRoot().getEvaluationError() == null) {
                assert m.getStatus() == Status.EvaluatingAsync;
                assert m.getEvaluationError() == null;
                assert m.isAsyncEvaluation();
                assert m.getPendingAsyncDependencies() > 0;
                m.decPendingAsyncDependencies();
                if (m.getPendingAsyncDependencies() == 0) {
                    execList.add(m);
                    if (!m.hasTLA()) {
                        gatherAvailableAncestors(m, execList);
                    }
                }
            }
        }
    }

    @TruffleBoundary
    private static Object asyncModuleExecutionFulfilled(JSRealm realm, JSModuleRecord module) {
        if (module.getStatus() == Status.Evaluated) {
            assert module.getEvaluationError() != null;
            return Undefined.instance;
        }
        assert module.getStatus() == Status.EvaluatingAsync;
        assert module.isAsyncEvaluation();
        assert module.getEvaluationError() == null;
        module.setAsyncEvaluation(false);
        module.setStatus(Status.Evaluated);
        if (module.getTopLevelCapability() != null) {
            assert module.getCycleRoot() == module;
            JSFunction.call(JSArguments.create(Undefined.instance, module.getTopLevelCapability().getResolve(), Undefined.instance));
        }
        Set<CyclicModuleRecord> execList = new TreeSet<>(new Comparator<CyclicModuleRecord>() {
            @Override
            public int compare(CyclicModuleRecord o1, CyclicModuleRecord o2) {
                return Long.compare(o1.getAsyncEvaluatingOrder(), o2.getAsyncEvaluatingOrder());
            }
        });
        gatherAvailableAncestors(module, execList);
        for (CyclicModuleRecord m : execList) {
            if (m.getStatus() == Status.Evaluated) {
                assert m.getEvaluationError() != null;
            } else if (m.hasTLA()) {
                moduleAsyncExecution(realm, m);
            } else {
                try {
                    Object result = m.executeModule(realm, (PromiseCapabilityRecord) null);
                    m.setExecutionResult(result);
                    m.setAsyncEvaluation(false);
                    m.setStatus(Status.Evaluated);
                    if (m.getTopLevelCapability() != null) {
                        assert m.getCycleRoot() == m;
                        JSFunction.call(JSArguments.create(Undefined.instance, m.getTopLevelCapability().getResolve(), Undefined.instance));
                    }
                } catch (AbstractTruffleException ex) {
                    asyncModuleExecutionRejected(realm, m, getErrorObject(ex));
                }
            }
        }
        return Undefined.instance;
    }

    @TruffleBoundary
    private static Object asyncModuleExecutionRejected(JSRealm realm, CyclicModuleRecord module, Object error) {
        assert error != null : "Cannot reject a module creation with null error";
        if (module.getStatus() == Status.Evaluated) {
            assert module.getEvaluationError() != null;
            return Undefined.instance;
        }
        assert module.getStatus() == Status.EvaluatingAsync;
        assert module.isAsyncEvaluation();
        assert module.getEvaluationError() == null;
        module.setEvaluationError(JSRuntime.getException(error));
        module.setStatus(Status.Evaluated);
        module.setAsyncEvaluation(false);
        /*
         * NOTE: _module_.[[AsyncEvaluation]] is set to *false* for symmetry with
         * AsyncModuleExecutionFulfilled. In InnerModuleEvaluation, the value of a module's
         * [[AsyncEvaluation]] internal slot is unused when its [[EvaluationError]] internal slot is
         * not ~empty~.
         */
        for (CyclicModuleRecord m : module.getAsyncParentModules()) {
            asyncModuleExecutionRejected(realm, m, error);
        }
        if (module.getTopLevelCapability() != null) {
            assert module.getCycleRoot() == module;
            JSFunction.call((JSFunctionObject) module.getTopLevelCapability().getReject(), Undefined.instance, new Object[]{error});
        }
        return Undefined.instance;
    }

    private static <S extends AbstractModuleRecord, T extends S> boolean occursExactlyOnce(T moduleRecord, Collection<S> stack) {
        return stack.stream().filter(moduleRecord::equals).count() == 1;
    }

    @Override
    public ScriptNode parseScript(JSContext context, Source source, ByteBuffer binary) {
        return ScriptNode.fromFunctionRoot((FunctionRootNode) new BinarySnapshotProvider(binary).apply(NodeFactory.getInstance(context), context, source));
    }

    @Override
    public ScriptNode parseScript(JSContext context, Source source, SnapshotProvider snapshotProvider) {
        return ScriptNode.fromFunctionRoot((FunctionRootNode) snapshotProvider.apply(NodeFactory.getInstance(context), context, source));
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
        ScriptNode script = JavaScriptTranslator.translateInlineScript(NodeFactory.getInstance(context), context, env, source, isStrict,
                        EvalNode.findActiveScriptOrModule(locationNode));
        return createInlineScriptCallNode(context, script.getFunctionData(), script.getCallTarget(), locationNode);
    }

    private static JavaScriptNode createInlineScriptCallNode(JSContext context, JSFunctionData functionData, RootCallTarget callTarget, Node locationNode) {
        return new JavaScriptNode() {
            @Child private DirectCallNode callNode = DirectCallNode.create(callTarget);
            @Child private PropertySetNode setScopeNode = PropertySetNode.createSetHidden(JSFunction.DEBUG_SCOPE_ID, context);
            @Child private NodeLibrary nodeLibrary = NodeLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

            @Override
            public Object execute(VirtualFrame frame) {
                JSDynamicObject closure = JSFunction.create(getRealm(), functionData);
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

    @Override
    public void checkFunctionSyntax(JSContext context, JSParserOptions parserOptions, String parameterList, String body, boolean generator, boolean async, String sourceName) {
        try {
            GraalJSParserHelper.checkFunctionSyntax(context, parserOptions, parameterList, body, generator, async, sourceName);
        } catch (com.oracle.js.parser.ParserException ex) {
            // throw the correct JS error
            parseFunction(context, parameterList, body, false, false, sourceName, null);
        }
    }
}
