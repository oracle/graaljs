/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDictionaryObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.builtins.SIMDType;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDTypeFactory;
import com.oracle.truffle.js.runtime.interop.DefaultJavaInteropWorker;
import com.oracle.truffle.js.runtime.interop.DefaultJavaInteropWorker.DefaultMainWorker;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.joni.JoniRegexCompiler;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.JSShapeData;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DebugJSAgent;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TimeProfiler;
import com.oracle.truffle.regex.RegexCompiler;
import com.oracle.truffle.regex.RegexEngine;
import com.oracle.truffle.regex.RegexLanguage;

public class JSContext implements ShapeContext {
    private final Evaluator evaluator;
    private final JSFunctionLookup functionLookup;

    private AbstractJavaScriptLanguage language;

    private final Shape emptyShape;
    private final Shape emptyShapePrototypeInObject;

    private final PrintWriterWrapper writer;
    private OutputStream writerStream;
    private final PrintWriterWrapper errorWriter;
    private OutputStream errorWriterStream;

    /**
     * Slot for a context-specific data of the embedder of the JS engine.
     */
    private Object embedderData;

    private final Assumption noSuchPropertyUnusedAssumption;
    private final Assumption noSuchMethodUnusedAssumption;

    /**
     * Assumption: There is no array that has a prototype that has indexed (array) elements. As long
     * as this assumption holds, certain checks can be omitted as all the indexed elements are in
     * the ScriptArray itself.
     */
    private final Assumption arrayPrototypeNoElementsAssumption;
    private final Assumption fastArrayAssumption;
    private final Assumption fastArgumentsObjectAssumption;

    /**
     * Assumption: TypedArrays never have their elements detached (i.e., buffer set to null). Can
     * typically not happen by the ES6 spec, but be used by tests (and by future versions of the
     * spec).
     */
    @CompilationFinal private Assumption typedArrayNotDetachedAssumption;

    /**
     * Local time zone information. Initialized lazily.
     */
    private LocalTimeZoneHolder localTimeZoneHolder;

    private volatile Map<String, Symbol> symbolRegistry;

    /**
     * ECMA 8.4 "PromiseJobs" job queue.
     */
    private final Deque<DynamicObject> promiseJobsQueue;
    private final Assumption promiseJobsQueueNotUsedAssumption;

    private final Object nodeFactory;

    private JSInteropRuntime interopRuntime;
    private TruffleLanguage.Env truffleLanguageEnv;
    private final TimeProfiler timeProfiler;

    private final DynamicObjectFactory moduleNamespaceFactory;
    private final DynamicObjectFactory javaWrapperFactory;

    private final Shape dictionaryShapeNullPrototype;

    /** The RegExp engine, as obtained from RegexLanguage. */
    private TruffleObject regexEngine;
    /** Support for RegExp.$1. */
    private TruffleObject regexResult;

    private JSModuleLoader moduleLoader;

    private PromiseRejectionTracker promiseRejectionTracker;
    private final Assumption promiseRejectionTrackerNotUsedAssumption;

    private PromiseHook promiseHook;
    private final Assumption promiseHookNotUsedAssumption;

    private final CallTarget emptyFunctionCallTarget;
    private final CallTarget speciesGetterFunctionCallTarget;
    private volatile CallTarget notConstructibleCallTargetCache;
    private volatile CallTarget generatorNotConstructibleCallTargetCache;
    private volatile CallTarget boundFunctionCallTargetCache;
    private volatile CallTarget boundFunctionConstructTargetCache;
    private volatile CallTarget boundFunctionConstructNewTargetCache;

    public enum BuiltinFunctionKey {
        AwaitFulfilled,
        AwaitRejected,
        AsyncGeneratorReturnFulfilled,
        AsyncGeneratorReturnRejected,
        AsyncFromSyncIteratorValueUnwrap,
        ProxyRevokerFunction,
    }

    @CompilationFinal(dimensions = 1) private final JSFunctionData[] builtinFunctionDataCache;

    private volatile JSFunctionData boundFunctionData;
    private volatile JSFunctionData boundConstructorFunctionData;

    private volatile Map<Shape, JSShapeData> shapeDataMap;

    private volatile List<JSRealm> realmList;

    private final boolean isChildContext;

    private boolean isRealmInitialized = false;

    /**
     * According to ECMA2017 8.4 the queue of pending jobs (promises reactions) must be processed
     * when the current stack is empty. For Interop, we assume that the stack is empty when (1) we
     * are called from another foreign language, and (2) there are no other nested JS Interop calls.
     *
     * This flag is used to implement this semantics.
     */
    private int interopCallStackDepth = 0;

    /**
     * Temporary field to access shapes from realm until transition is complete.
     */
    @CompilationFinal private ShapeContext shapeContext;

    /**
     * Temporary field until transition is complete.
     */
    @CompilationFinal private JSRealm realm;

    @CompilationFinal private ContextReference<JSRealm> contextRef;

    /**
     * ECMA2017 8.7 Agent object.
     *
     * Temporary field until JSAgents are supported in Node.js.
     *
     * Initialized after engine creation either by some testing harness or by node.
     */
    @CompilationFinal private JSAgent agent;

    @CompilationFinal private AllocationReporter allocationReporter;

    /**
     * Java Interop Workers factory.
     */
    @CompilationFinal private EcmaAgent mainWorker;
    @CompilationFinal private EcmaAgent.Factory javaInteropWorkersFactory;
    @CompilationFinal private boolean shouldProcessJavaInteropAsyncTasks = true;

    private final JSContextOptions contextOptions;

    public JSContext(Evaluator evaluator, JSFunctionLookup lookup, JSContextOptions contextOptions, AbstractJavaScriptLanguage lang, TruffleLanguage.Env env, boolean isChildContext) {
        this.functionLookup = lookup;
        this.contextOptions = contextOptions;
        this.truffleLanguageEnv = env; // could still be null
        this.contextOptions.setEnv(env);
        this.isChildContext = isChildContext;

        this.language = lang;

        this.emptyShape = createEmptyShape();
        this.emptyShapePrototypeInObject = createEmptyShapePrototypeInObject();

        this.noSuchPropertyUnusedAssumption = JSTruffleOptions.NashornExtensions ? Truffle.getRuntime().createAssumption("noSuchPropertyUnusedAssumption") : null;
        this.noSuchMethodUnusedAssumption = JSTruffleOptions.NashornExtensions ? Truffle.getRuntime().createAssumption("noSuchMethodUnusedAssumption") : null;
        this.arrayPrototypeNoElementsAssumption = Truffle.getRuntime().createAssumption("arrayPrototypeNoElementsAssumption");
        this.typedArrayNotDetachedAssumption = Truffle.getRuntime().createAssumption("typedArrayNotDetachedAssumption");
        this.fastArrayAssumption = Truffle.getRuntime().createAssumption("fastArrayAssumption");
        this.fastArgumentsObjectAssumption = Truffle.getRuntime().createAssumption("fastArgumentsObjectAssumption");

        this.evaluator = evaluator;
        this.nodeFactory = evaluator.getDefaultNodeFactory();

        this.moduleNamespaceFactory = JSModuleNamespace.makeInitialShape(this).createFactory();

        this.writer = new PrintWriterWrapper(System.out, true);
        this.writerStream = System.out;
        this.errorWriter = new PrintWriterWrapper(System.err, true);
        this.errorWriterStream = System.err;

        this.promiseJobsQueue = new LinkedList<>();
        this.promiseJobsQueueNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseJobsQueueNotUsedAssumption");

        this.promiseHookNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseHookNotUsedAssumption");
        this.promiseRejectionTrackerNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseRejectionTrackerNotUsedAssumption");

        this.emptyFunctionCallTarget = createEmptyFunctionCallTarget(lang);
        this.speciesGetterFunctionCallTarget = createSpeciesGetterFunctionCallTarget(lang);

        this.builtinFunctionDataCache = new JSFunctionData[BuiltinFunctionKey.values().length];

        this.timeProfiler = JSTruffleOptions.ProfileTime ? new TimeProfiler() : null;
        this.javaWrapperFactory = JSTruffleOptions.NashornJavaInterop ? JSJavaWrapper.makeShape(this).createFactory() : null;

        this.dictionaryShapeNullPrototype = JSTruffleOptions.DictionaryObject ? JSDictionaryObject.makeDictionaryShape(this, null) : null;

        if (JSTruffleOptions.Test262Mode || JSTruffleOptions.TestV8Mode) {
            this.setJSAgent(new DebugJSAgent(this));
        }
    }

    public final JSFunctionLookup getFunctionLookup() {
        return functionLookup;
    }

    public final Evaluator getEvaluator() {
        return evaluator;
    }

    public Object getNodeFactory() {
        return nodeFactory;
    }

    public final PrintWriter getWriter() {
        return writer;
    }

    /**
     * Returns the stream used by {@link #getWriter}, or null if the stream is not available.
     *
     * Do not write to the stream directly, always use the {@link #getWriter writer} instead. Use
     * this method only to check if the current writer is already writing to the stream you want to
     * use, in which case you can avoid creating a new {@link PrintWriter}.
     */
    public OutputStream getWriterStream() {
        return writerStream;
    }

    public final PrintWriter getErrorWriter() {
        return errorWriter;
    }

    /**
     * Returns the stream used by {@link #getErrorWriter}, or null if the stream is not available.
     *
     * Do not write to the stream directly, always use the {@link #getErrorWriter writer} instead.
     * Use this method only to check if the current writer is already writing to the stream you want
     * to use, in which case you can avoid creating a new {@link PrintWriter}.
     */
    public OutputStream getErrorWriterStream() {
        return errorWriterStream;
    }

    public final void setWriter(Writer writer, OutputStream writerStream) {
        if (writer instanceof PrintWriterWrapper) {
            this.writer.setFrom((PrintWriterWrapper) writer);
        } else {
            if (writerStream != null) {
                this.writer.setDelegate(writerStream);
            } else {
                this.writer.setDelegate(writer);
            }
        }
        this.writerStream = writerStream;
    }

    public final void setErrorWriter(Writer errorWriter, OutputStream errorWriterStream) {
        if (errorWriter instanceof PrintWriterWrapper) {
            this.errorWriter.setFrom((PrintWriterWrapper) errorWriter);
        } else {
            if (errorWriterStream != null) {
                this.errorWriter.setDelegate(errorWriterStream);
            } else {
                this.errorWriter.setDelegate(errorWriter);
            }
        }
        this.errorWriterStream = errorWriterStream;
    }

    public final ParserOptions getParserOptions() {
        return contextOptions.getParserOptions();
    }

    public final void setParserOptions(ParserOptions parserOptions) {
        this.contextOptions.setParserOptions(parserOptions);
    }

    public final OptionValues getOptionValues() {
        return getEnv().getOptions();
    }

    public final Object getEmbedderData() {
        return embedderData;
    }

    public final void setEmbedderData(Object embedderData) {
        this.embedderData = embedderData;
    }

    public final Assumption getNoSuchPropertyUnusedAssumption() {
        return noSuchPropertyUnusedAssumption;
    }

    public final Assumption getNoSuchMethodUnusedAssumption() {
        return noSuchMethodUnusedAssumption;
    }

    public final Assumption getArrayPrototypeNoElementsAssumption() {
        return arrayPrototypeNoElementsAssumption;
    }

    public final Assumption getFastArrayAssumption() {
        return fastArrayAssumption;
    }

    public final Assumption getFastArgumentsObjectAssumption() {
        return fastArgumentsObjectAssumption;
    }

    public final Assumption getTypedArrayNotDetachedAssumption() {
        return typedArrayNotDetachedAssumption;
    }

    public static JSContext createContext(Evaluator evaluator, JSFunctionLookup lookup, JSContextOptions contextOptions, AbstractJavaScriptLanguage lang, TruffleLanguage.Env env) {
        return new JSContext(evaluator, lookup, contextOptions, lang, env, false);
    }

    public JSRealm createRealm() {
        return createRealm(true);
    }

    private JSRealm createRealm(boolean initRealmBuiltinObject) {
        JSRealm newRealm = new JSRealm(this);
        newRealm.setupGlobals();
        if (realmList == null) {
            realmList = new ArrayList<>();
        }
        realmList.add(newRealm);
        if (initRealmBuiltinObject) {
            newRealm.initRealmBuiltinObject();
        }
        newRealm.getContext().setRealmInitialized();
        return newRealm;
    }

    public final Shape createEmptyShape() {
        return JSShape.makeEmptyRoot(JSObject.LAYOUT, JSUserObject.INSTANCE, this);
    }

    private Shape createEmptyShapePrototypeInObject() {
        Property prototypeProperty = JSObjectUtil.makeHiddenProperty(JSObject.HIDDEN_PROTO,
                        JSShape.makeAllocator(JSObject.LAYOUT).locationForType(JSObject.CLASS, EnumSet.noneOf(LocationModifier.class)));
        return JSShape.makeEmptyRoot(JSObject.LAYOUT, JSUserObject.INSTANCE, this, prototypeProperty);
    }

    public void setLocalTimeZoneId(ZoneId zoneId) {
        localTimeZoneHolder = new LocalTimeZoneHolder(zoneId);
    }

    private LocalTimeZoneHolder getLocalTimeZoneHolder() {
        if (localTimeZoneHolder != null) {
            return localTimeZoneHolder;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return localTimeZoneHolder = new LocalTimeZoneHolder();
        }
    }

    public final ZoneId getLocalTimeZoneId() {
        return getLocalTimeZoneHolder().localTimeZoneId;
    }

    public final long getLocalTZA() {
        return getLocalTimeZoneHolder().localTZA;
    }

    public final Map<String, Symbol> getSymbolRegistry() {
        if (symbolRegistry == null) {
            createSymbolRegistry();
        }
        return symbolRegistry;
    }

    @TruffleBoundary
    private synchronized void createSymbolRegistry() {
        if (symbolRegistry == null) {
            symbolRegistry = new HashMap<>();
        }
    }

    /**
     * ECMA 8.4.1 EnqueueJob.
     */
    public final void promiseEnqueueJob(DynamicObject newTarget) {
        invalidatePromiseQueueNotUsedAssumption();
        promiseJobsQueue.push(newTarget);
    }

    private void invalidatePromiseQueueNotUsedAssumption() {
        if (promiseJobsQueueNotUsedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseJobsQueueNotUsedAssumption.invalidate();
        }
    }

    public final boolean processAllPendingPromiseJobs() {
        if (promiseJobsQueueNotUsedAssumption.isValid()) {
            return false;
        } else {
            return processAllPromises();
        }
    }

    @TruffleBoundary
    private boolean processAllPromises() {
        boolean queueContainsJobs = false;
        DynamicObject global = getRealm().getGlobalObject();
        while (promiseJobsQueue.size() > 0) {
            DynamicObject nextJob = promiseJobsQueue.pollLast();
            if (JSFunction.isJSFunction(nextJob)) {
                JSFunction.call(nextJob, global, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                queueContainsJobs = true;
            }
        }

        // In node.js-mode, tasks are processed by the uv loop.
        if (shouldProcessJavaInteropAsyncTasks) {
            queueContainsJobs = processJavaInteropAsyncTasks();
        }

        // If a job was executed, it might have scheduled other tasks, so we let the caller know.
        return queueContainsJobs;
    }

    public void interopBoundaryEnter() {
        if (getEcmaScriptVersion() >= 6) {
            interopCallStackDepth++;
        }
    }

    public void interopBoundaryExit() {
        if (getEcmaScriptVersion() >= 6 && --interopCallStackDepth == 0) {
            while (processAllPendingPromiseJobs()) {
                // we consume all pending jobs
            }
        }
    }

    public JSInteropRuntime getInteropRuntime() {
        return interopRuntime;
    }

    public void setInteropRuntime(JSInteropRuntime interopRuntime) {
        assert this.interopRuntime == null;
        this.interopRuntime = interopRuntime;
    }

    public void patchTruffleLanguageEnv(TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        Objects.requireNonNull(env, "New env cannot be null.");
        truffleLanguageEnv = env;
        activateAllocationReporter();
        this.contextOptions.setEnv(env);
    }

    public void activateAllocationReporter() {
        this.allocationReporter = truffleLanguageEnv.lookup(AllocationReporter.class);
    }

    public TimeProfiler getTimeProfiler() {
        return timeProfiler;
    }

    public JSRealm getRealm() {
        if (isChildContext || !isRealmInitialized || JSTruffleOptions.NashornCompatibilityMode) {
            return realm; // childContext Realm cannot be shared among Engines (GR-8695)
        }
        if (contextRef == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (getLanguage() != null) {  // could be uninitialized
                contextRef = getLanguage().getContextReference();
            } else {
                return realm;
            }
        }
        JSRealm realm2 = contextRef.get();
        return realm2 != null ? realm2 : realm;
    }

    public boolean hasRealm() {
        assert (getRealm() != null) == isRealmInitialized;
        return this.isRealmInitialized;
    }

    @Override
    public final Shape getEmptyShape() {
        return emptyShape;
    }

    @Override
    public final Shape getEmptyShapePrototypeInObject() {
        return emptyShapePrototypeInObject;
    }

    @Override
    public final Shape getInitialUserObjectShape() {
        return shapeContext.getInitialUserObjectShape();
    }

    @Override
    public final DynamicObjectFactory getArrayFactory() {
        return shapeContext.getArrayFactory();
    }

    @Override
    public final DynamicObjectFactory getStringFactory() {
        return shapeContext.getStringFactory();
    }

    @Override
    public final DynamicObjectFactory getBooleanFactory() {
        return shapeContext.getBooleanFactory();
    }

    @Override
    public final DynamicObjectFactory getNumberFactory() {
        return shapeContext.getNumberFactory();
    }

    @Override
    public final DynamicObjectFactory getSymbolFactory() {
        return shapeContext.getSymbolFactory();
    }

    @Override
    public final DynamicObjectFactory getArrayBufferViewFactory(TypedArrayFactory factory) {
        return shapeContext.getArrayBufferViewFactory(factory);
    }

    @Override
    public final DynamicObjectFactory getArrayBufferFactory() {
        return shapeContext.getArrayBufferFactory();
    }

    @Override
    public final DynamicObjectFactory getDirectArrayBufferViewFactory(TypedArrayFactory factory) {
        return shapeContext.getDirectArrayBufferViewFactory(factory);
    }

    @Override
    public final DynamicObjectFactory getDirectArrayBufferFactory() {
        return shapeContext.getDirectArrayBufferFactory();
    }

    @Override
    public final DynamicObjectFactory getRegExpFactory() {
        return shapeContext.getRegExpFactory();
    }

    @Override
    public final DynamicObjectFactory getDateFactory() {
        return shapeContext.getDateFactory();
    }

    @Override
    public final DynamicObjectFactory getEnumerateIteratorFactory() {
        return shapeContext.getEnumerateIteratorFactory();
    }

    @Override
    public final DynamicObjectFactory getMapFactory() {
        return shapeContext.getMapFactory();
    }

    @Override
    public final DynamicObjectFactory getWeakMapFactory() {
        return shapeContext.getWeakMapFactory();
    }

    @Override
    public final DynamicObjectFactory getSetFactory() {
        return shapeContext.getSetFactory();
    }

    @Override
    public final DynamicObjectFactory getWeakSetFactory() {
        return shapeContext.getWeakSetFactory();
    }

    @Override
    public final DynamicObjectFactory getDataViewFactory() {
        return shapeContext.getDataViewFactory();
    }

    @Override
    public final DynamicObjectFactory getProxyFactory() {
        return shapeContext.getProxyFactory();
    }

    @Override
    public final DynamicObjectFactory getSharedArrayBufferFactory() {
        assert isOptionSharedArrayBuffer();
        return shapeContext.getSharedArrayBufferFactory();
    }

    @Override
    public final DynamicObjectFactory getCollatorFactory() {
        return shapeContext.getCollatorFactory();
    }

    @Override
    public final DynamicObjectFactory getNumberFormatFactory() {
        return shapeContext.getNumberFormatFactory();
    }

    @Override
    public final DynamicObjectFactory getPluralRulesFactory() {
        return shapeContext.getPluralRulesFactory();
    }

    @Override
    public final DynamicObjectFactory getDateTimeFormatFactory() {
        return shapeContext.getDateTimeFormatFactory();
    }

    @Override
    public final DynamicObjectFactory getJavaImporterFactory() {
        return shapeContext.getJavaImporterFactory();
    }

    @Override
    public final DynamicObjectFactory getJSAdapterFactory() {
        return shapeContext.getJSAdapterFactory();
    }

    @Override
    public final DynamicObjectFactory getErrorFactory(JSErrorType type, boolean withMessage) {
        return shapeContext.getErrorFactory(type, withMessage);
    }

    @Override
    public final DynamicObjectFactory getPromiseFactory() {
        return shapeContext.getPromiseFactory();
    }

    @Override
    public DynamicObjectFactory getModuleNamespaceFactory() {
        return moduleNamespaceFactory;
    }

    @Override
    public DynamicObjectFactory getSIMDTypeFactory(SIMDTypeFactory<? extends SIMDType> factory) {
        return shapeContext.getSIMDTypeFactory(factory);
    }

    void setRealm(JSRealm realm) {
        assert this.realm == null;
        this.shapeContext = realm;
        this.realm = realm;
    }

    public TruffleLanguage.Env getEnv() {
        return truffleLanguageEnv;
    }

    public DynamicObjectFactory getJavaWrapperFactory() {
        return javaWrapperFactory;
    }

    private static String createRegexEngineOptions() {
        StringBuilder options = new StringBuilder(30);
        if (JSTruffleOptions.U180EWhitespace) {
            options.append("U180EWhitespace=true");
        }
        if (JSTruffleOptions.RegexRegressionTestMode) {
            if (options.length() > 0) {
                options.append(",");
            }
            options.append("RegressionTestMode=true");
        }
        return options.toString();
    }

    public TruffleObject getRegexEngine() {
        if (regexEngine == null) {
            RegexCompiler joniCompiler = new JoniRegexCompiler(null);
            if (JSTruffleOptions.UseTRegex) {
                TruffleObject regexEngineBuilder = (TruffleObject) getEnv().parse(Source.newBuilder("").name("TRegex Engine Builder Request").language(RegexLanguage.ID).build()).call();
                String regexOptions = createRegexEngineOptions();
                try {
                    regexEngine = (TruffleObject) ForeignAccess.sendExecute(Message.createExecute(2).createNode(), regexEngineBuilder, regexOptions, joniCompiler);
                } catch (InteropException ex) {
                    throw ex.raise();
                }
            } else {
                regexEngine = new RegexEngine(joniCompiler, JSTruffleOptions.RegexRegressionTestMode);
            }
        }
        return regexEngine;
    }

    public TruffleObject getRegexResult() {
        assert isOptionRegexpStaticResult();
        if (regexResult == null) {
            regexResult = TRegexUtil.getTRegexEmptyResult();
        }
        return regexResult;
    }

    public void setRegexResult(TruffleObject regexResult) {
        if (isOptionRegexpStaticResult()) {
            assert TRegexUtil.readResultIsMatch(TRegexUtil.createReadNode(), regexResult);
            this.regexResult = regexResult;
        }
    }

    public Shape getDictionaryShapeNullPrototype() {
        return dictionaryShapeNullPrototype;
    }

    public JSModuleLoader getModuleLoader() {
        if (moduleLoader == null) {
            createModuleLoader();
        }
        return moduleLoader;
    }

    @TruffleBoundary
    private synchronized void createModuleLoader() {
        if (moduleLoader == null) {
            moduleLoader = new JSModuleLoader() {
                private final Map<String, JSModuleRecord> moduleMap = new HashMap<>();

                @Override
                public JSModuleRecord resolveImportedModule(JSModuleRecord referencingModule, String specifier) {
                    try {
                        String path = getPath(referencingModule.getSource());
                        File moduleFile = Paths.get(path).resolveSibling(specifier).toFile();
                        String canonicalPath = moduleFile.getCanonicalPath();
                        JSModuleRecord existingModule = moduleMap.get(canonicalPath);
                        if (existingModule != null) {
                            return existingModule;
                        }
                        Source source = Source.newBuilder(moduleFile).name(specifier).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
                        JSModuleRecord newModule = getEvaluator().parseModule(JSContext.this, source, this);
                        moduleMap.put(canonicalPath, newModule);
                        return newModule;
                    } catch (IOException e) {
                        throw Errors.createError(e.getMessage());
                    }
                }

                String getPath(Source source) {
                    String path = source.getPath();
                    if (path == null) {
                        path = source.getName();
                        if (path.startsWith("module:")) {
                            path = path.substring("module:".length());
                        }
                    }
                    try {
                        return Paths.get(path).toFile().getCanonicalPath();
                    } catch (IOException e) {
                        throw Errors.createError(e.getMessage());
                    }
                }

                @Override
                public JSModuleRecord loadModule(Source source) {
                    String path = getPath(source);
                    return moduleMap.computeIfAbsent(path, (key) -> getEvaluator().parseModule(JSContext.this, source, this));
                }
            };
        }
    }

    private static class LocalTimeZoneHolder {
        final ZoneId localTimeZoneId;
        final long localTZA;

        LocalTimeZoneHolder(ZoneId zoneId) {
            this.localTimeZoneId = zoneId;
            this.localTZA = JSDate.getLocalTZA(zoneId);
        }

        LocalTimeZoneHolder() {
            this(ZoneId.systemDefault());
        }
    }

    public void setSymbolRegistry(Map<String, Symbol> newSymbolRegistry) {
        this.symbolRegistry = newSymbolRegistry;
    }

    public Map<Shape, JSShapeData> getShapeDataMap() {
        Map<Shape, JSShapeData> map = shapeDataMap;
        if (map == null) {
            map = createShapeDataMap();
        }
        return map;
    }

    private synchronized Map<Shape, JSShapeData> createShapeDataMap() {
        Map<Shape, JSShapeData> map = shapeDataMap;
        if (map == null) {
            return shapeDataMap = new WeakHashMap<>();
        } else {
            return map;
        }
    }

    public AbstractJavaScriptLanguage getLanguage() {
        return language;
    }

    public void setLanguage(AbstractJavaScriptLanguage language) {
        CompilerAsserts.neverPartOfCompilation();
        if (this.language != null) {
            throw new IllegalStateException("language can only be set once");
        }
        this.language = language;
    }

    public CallTarget getEmptyFunctionCallTarget() {
        return emptyFunctionCallTarget;
    }

    /** CallTarget for an empty function that returns undefined. */
    private static CallTarget createEmptyFunctionCallTarget(AbstractJavaScriptLanguage lang) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                return Undefined.instance;
            }
        });
    }

    public CallTarget getSpeciesGetterFunctionCallTarget() {
        return speciesGetterFunctionCallTarget;
    }

    private static CallTarget createSpeciesGetterFunctionCallTarget(AbstractJavaScriptLanguage lang) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                return JSFrameUtil.getThisObj(frame);
            }
        });
    }

    @TruffleBoundary
    public CallTarget getNotConstructibleCallTarget() {
        CallTarget result = notConstructibleCallTargetCache;
        if (result == null) {
            synchronized (this) {
                result = notConstructibleCallTargetCache;
                if (result == null) {
                    result = notConstructibleCallTargetCache = createNotConstructibleCallTarget(getLanguage(), false);
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    public CallTarget getGeneratorNotConstructibleCallTarget() {
        CallTarget result = generatorNotConstructibleCallTargetCache;
        if (result == null) {
            synchronized (this) {
                result = generatorNotConstructibleCallTargetCache;
                if (result == null) {
                    result = generatorNotConstructibleCallTargetCache = createNotConstructibleCallTarget(getLanguage(), true);
                }
            }
        }
        return result;
    }

    private static RootCallTarget createNotConstructibleCallTarget(AbstractJavaScriptLanguage lang, boolean generator) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                if (generator) {
                    throw Errors.createTypeError("cannot construct a generator");
                } else {
                    throw Errors.createTypeErrorNotConstructible((DynamicObject) JSArguments.getFunctionObject(frame.getArguments()));
                }
            }
        });
    }

    @TruffleBoundary
    public CallTarget getBoundFunctionCallTarget() {
        CallTarget result = boundFunctionCallTargetCache;
        if (result == null) {
            synchronized (this) {
                result = boundFunctionCallTargetCache;
                if (result == null) {
                    result = boundFunctionCallTargetCache = Truffle.getRuntime().createCallTarget(JSFunction.createBoundRootNode(this, false, false));
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    public CallTarget getBoundFunctionConstructTarget() {
        CallTarget result = boundFunctionConstructTargetCache;
        if (result == null) {
            synchronized (this) {
                result = boundFunctionConstructTargetCache;
                if (result == null) {
                    result = boundFunctionConstructTargetCache = Truffle.getRuntime().createCallTarget(JSFunction.createBoundRootNode(this, true, false));
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    public CallTarget getBoundFunctionConstructNewTarget() {
        CallTarget result = boundFunctionConstructNewTargetCache;
        if (result == null) {
            synchronized (this) {
                result = boundFunctionConstructNewTargetCache;
                if (result == null) {
                    result = boundFunctionConstructNewTargetCache = Truffle.getRuntime().createCallTarget(JSFunction.createBoundRootNode(this, true, true));
                }
            }
        }
        return result;
    }

    public JSFunctionData getBoundFunctionData(boolean constructor) {
        JSFunctionData result = constructor ? boundConstructorFunctionData : boundFunctionData;
        if (result == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            synchronized (this) {
                result = constructor ? boundConstructorFunctionData : boundFunctionData;
                if (result == null) {
                    result = JSFunctionData.create(this,
                                    getBoundFunctionCallTarget(), getBoundFunctionConstructTarget(), getBoundFunctionConstructNewTarget(),
                                    0, "bound", constructor, false, true, false, false, false, false, false, true, false, true);
                    if (constructor) {
                        boundConstructorFunctionData = result;
                    } else {
                        boundFunctionData = result;
                    }
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    public JSContext createChildContext() {
        JSContext childContext = new JSContext(getEvaluator(), getFunctionLookup(), contextOptions, getLanguage(), truffleLanguageEnv, true);
        childContext.setWriter(getWriter(), getWriterStream());
        childContext.setErrorWriter(getErrorWriter(), getErrorWriterStream());
        childContext.setLocalTimeZoneId(getLocalTimeZoneId());
        childContext.setSymbolRegistry(getSymbolRegistry());
        childContext.setRealmList(realmList);
        childContext.setInteropRuntime(interopRuntime);
        childContext.typedArrayNotDetachedAssumption = this.typedArrayNotDetachedAssumption;
        // cannot leave Realm uninitialized
        JSRealm childRealm = childContext.createRealm(false);
        // "Realm" object shared by all realms
        childRealm.setRealmBuiltinObject(getRealm().getRealmBuiltinObject());
        return childContext;
    }

    private void setRealmList(List<JSRealm> realmList) {
        this.realmList = realmList;
    }

    public synchronized JSRealm getFromRealmList(int idx) {
        return Boundaries.listGet(realmList, idx);
    }

    public synchronized int getIndexFromRealmList(JSRealm realm2) {
        return Boundaries.listIndexOf(realmList, realm2);
    }

    public synchronized void setInRealmList(int idx, JSRealm realm2) {
        Boundaries.listSet(realmList, idx, realm2);
    }

    public JSAgent getJSAgent() {
        assert agent != null : "Null agent!";
        return agent;
    }

    public void setJSAgent(JSAgent newAgent) {
        if (agent != null) {
            throw new RuntimeException("Cannot re-initialize JSAgent");
        }
        assert newAgent != null : "Cannot set a null agent!";
        CompilerAsserts.neverPartOfCompilation("Assigning agent to context in compiled code");
        this.agent = newAgent;
    }

    public int getEcmaScriptVersion() {
        int version = contextOptions.getEcmaScriptVersion();
        assert version >= 5 && version <= JSTruffleOptions.MaxECMAScriptVersion;
        return version;
    }

    public DynamicObject allocateObject(Shape shape) {
        DynamicObject object;
        AllocationReporter reporter = allocationReporter;
        if (reporter != null) {
            reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        object = shape.newInstance();
        if (reporter != null) {
            reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        return object;
    }

    public DynamicObject allocateObject(DynamicObjectFactory factory, Object... initialValues) {
        DynamicObject object;
        AllocationReporter reporter = allocationReporter;
        if (reporter != null) {
            reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        object = factory.newInstance(initialValues);
        if (reporter != null) {
            reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        return object;
    }

    public boolean isOptionAnnexB() {
        return contextOptions.isAnnexB();
    }

    public boolean isOptionIntl402() {
        return contextOptions.isIntl402();
    }

    public boolean isOptionRegexpStaticResult() {
        return contextOptions.isRegexpStaticResult();
    }

    public boolean isOptionArraySortInherited() {
        return contextOptions.isArraySortInherited();
    }

    public boolean isOptionSharedArrayBuffer() {
        return contextOptions.isSharedArrayBuffer();
    }

    public boolean isOptionAtomics() {
        return contextOptions.isAtomics();
    }

    public boolean isOptionV8CompatibilityMode() {
        return contextOptions.isV8CompatibilityMode();
    }

    public boolean isOptionDebugBuiltin() {
        return contextOptions.isDebugBuiltin();
    }

    public boolean isOptionDirectByteBuffer() {
        return contextOptions.isDirectByteBuffer();
    }

    public boolean isOptionParseOnly() {
        return contextOptions.isParseOnly();
    }

    public boolean isOptionPreciseTime() {
        return contextOptions.isPreciseTime();
    }

    /**
     * Creation of PrintWriter is expensive, this is why we change just the delegate writer in this
     * wrapper class.
     */
    private static final class PrintWriterWrapper extends PrintWriter {

        private OutputStreamWrapper outWrapper;

        PrintWriterWrapper(OutputStream out, boolean autoFlush) {
            this(new OutputStreamWrapper(out), autoFlush);
        }

        private PrintWriterWrapper(OutputStreamWrapper outWrapper, boolean autoFlush) {
            this(new OutputStreamWriter(outWrapper), autoFlush);
            this.outWrapper = outWrapper;
        }

        private PrintWriterWrapper(Writer out, boolean autoFlush) {
            super(out, autoFlush);
        }

        void setDelegate(Writer out) {
            synchronized (this.lock) {
                this.out = out;
                this.outWrapper = null;
            }
        }

        void setDelegate(OutputStream out) {
            synchronized (this.lock) {
                if (outWrapper != null) {
                    outWrapper.setDelegate(out);
                } else {
                    outWrapper = new OutputStreamWrapper(out);
                    this.out = new OutputStreamWriter(outWrapper);
                }
            }
        }

        void setFrom(PrintWriterWrapper otherWrapper) {
            synchronized (this.lock) {
                boolean newWrapper = false;
                if (otherWrapper.outWrapper != null) {
                    // Need to keep separate OutputStreamWrapper instances
                    if (this.outWrapper != null) {
                        // We both have wrappers, great, just need to update the delegate
                        this.outWrapper.setDelegate(otherWrapper.outWrapper.getDelegate());
                    } else {
                        // The other has a wrapper, but we do not. Create our own.
                        this.outWrapper = new OutputStreamWrapper(otherWrapper.outWrapper.getDelegate());
                        newWrapper = true;
                    }
                } else {
                    // No wrapper. Will copy the Writer only.
                    this.outWrapper = null;
                }
                if (this.outWrapper != null) {
                    if (newWrapper) {
                        this.out = new OutputStreamWriter(this.outWrapper);
                    }
                } else {
                    this.out = otherWrapper.out;
                }
            }
        }
    }

    /**
     * A simple wrapper of an {@link OutputStream} that allows to change the delegate. With this
     * it's not necessary to create a new {@link OutputStreamWriter} for a new {@link OutputStream},
     * it's enough to just replace the delegate {@link OutputStream}.
     */
    private static final class OutputStreamWrapper extends OutputStream {

        private volatile OutputStream out;

        OutputStreamWrapper(OutputStream out) {
            this.out = out;
        }

        void setDelegate(OutputStream out) {
            this.out = out;
        }

        OutputStream getDelegate() {
            return out;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    public void initializeJavaInteropWorkers(EcmaAgent workerMain, EcmaAgent.Factory workerFactory) {
        assert mainWorker == null && javaInteropWorkersFactory == null;
        mainWorker = workerMain;
        javaInteropWorkersFactory = workerFactory;
        shouldProcessJavaInteropAsyncTasks = false;
    }

    public EcmaAgent.Factory getJavaInteropWorkerFactory() {
        if (javaInteropWorkersFactory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            javaInteropWorkersFactory = new DefaultJavaInteropWorker.Factory((DefaultMainWorker) getMainWorker());
            // As soon as we load Java interop, we know we will use promises.
            invalidatePromiseQueueNotUsedAssumption();
        }
        return javaInteropWorkersFactory;
    }

    public EcmaAgent getMainWorker() {
        if (mainWorker == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            mainWorker = new DefaultMainWorker();
        }
        return mainWorker;
    }

    public boolean processJavaInteropAsyncTasks() {
        assert shouldProcessJavaInteropAsyncTasks;
        DefaultMainWorker main = (DefaultMainWorker) getMainWorker();
        return main.processPendingTasks();
    }

    public final void setPromiseRejectionTracker(PromiseRejectionTracker tracker) {
        invalidatePromiseRejectionTrackerNotUsedAssumption();
        this.promiseRejectionTracker = tracker;
    }

    private void invalidatePromiseRejectionTrackerNotUsedAssumption() {
        if (promiseRejectionTrackerNotUsedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseRejectionTrackerNotUsedAssumption.invalidate();
        }
    }

    public void notifyPromiseRejectionTracker(DynamicObject promise, int operation) {
        if (!promiseRejectionTrackerNotUsedAssumption.isValid() && promiseRejectionTracker != null) {
            if (operation == 0) {
                invokePromiseRejected(promise);
            } else {
                assert (operation == 1) : "Unknown operation: " + operation;
                invokePromiseRejectionHandled(promise);
            }
        }
    }

    @TruffleBoundary
    private void invokePromiseRejected(DynamicObject promise) {
        promiseRejectionTracker.promiseRejected(promise);
    }

    @TruffleBoundary
    private void invokePromiseRejectionHandled(DynamicObject promise) {
        promiseRejectionTracker.promiseRejectionHandled(promise);
    }

    public final void setPromiseHook(PromiseHook promiseHook) {
        invalidatePromiseHookNotUsedAssumption();
        this.promiseHook = promiseHook;
    }

    private void invalidatePromiseHookNotUsedAssumption() {
        if (promiseHookNotUsedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHookNotUsedAssumption.invalidate();
        }
    }

    // Helper field for PromiseHook.TYPE_INIT event (stores the parent promise)
    private DynamicObject parentPromise;

    public final void notifyPromiseHook(int changeType, DynamicObject promise) {
        if (!promiseHookNotUsedAssumption.isValid() && promiseHook != null) {
            if (changeType == -1) {
                // Information about parent for the incoming INIT event
                storeParentPromise(promise);
            } else {
                DynamicObject parent = (changeType == PromiseHook.TYPE_INIT) ? fetchParentPromise() : Undefined.instance;
                notifyPromiseHookImpl(changeType, promise, parent);
            }
        }
    }

    private void storeParentPromise(DynamicObject promise) {
        parentPromise = promise;
    }

    private DynamicObject fetchParentPromise() {
        DynamicObject parent = parentPromise;
        if (parent == null) {
            parent = Undefined.instance;
        } else {
            parentPromise = null;
        }
        return parent;
    }

    @TruffleBoundary
    private void notifyPromiseHookImpl(int changeType, DynamicObject promise, DynamicObject parent) {
        promiseHook.promiseChanged(changeType, promise, parent);
    }

    public final JSFunctionData getOrCreateBuiltinFunctionData(BuiltinFunctionKey key, Function<JSContext, JSFunctionData> factory) {
        final int index = key.ordinal();
        JSFunctionData callTarget = builtinFunctionDataCache[index];
        if (callTarget != null) {
            return callTarget;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        synchronized (this) {
            callTarget = builtinFunctionDataCache[index];
            if (callTarget == null) {
                callTarget = factory.apply(this);
                builtinFunctionDataCache[index] = callTarget;
            }
            return callTarget;
        }
    }

    public void setRealmInitialized() {
        this.isRealmInitialized = true;
    }
}
