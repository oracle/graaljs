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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.Builtin;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSCollator;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.JSDictionaryObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.builtins.JSJavaWorkerBuiltin;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.builtins.SIMDType;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDTypeFactory;
import com.oracle.truffle.js.runtime.interop.DefaultJavaInteropWorker;
import com.oracle.truffle.js.runtime.interop.DefaultJavaInteropWorker.DefaultMainWorker;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaImporter;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.java.adapter.JavaAdapterFactory;
import com.oracle.truffle.js.runtime.joni.JoniRegexCompiler;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSPrototypeData;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.JSShapeData;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;
import com.oracle.truffle.js.runtime.util.CompilableFunction;
import com.oracle.truffle.js.runtime.util.DebugJSAgent;
import com.oracle.truffle.js.runtime.util.TimeProfiler;
import com.oracle.truffle.regex.CachingRegexEngine;
import com.oracle.truffle.regex.RegexCompiler;
import com.oracle.truffle.regex.RegexLanguage;

public class JSContext {
    private final Evaluator evaluator;
    private final JSFunctionLookup functionLookup;

    private final AbstractJavaScriptLanguage language;

    private final Shape emptyShape;
    private final Shape emptyShapePrototypeInObject;
    private final Shape globalScopeShape;

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
    private final TimeProfiler timeProfiler;

    private final JSObjectFactory.BoundProto moduleNamespaceFactory;
    private final JSObjectFactory.BoundProto javaWrapperFactory;

    private final Shape dictionaryShapeNullPrototype;

    /** The RegExp engine, as obtained from RegexLanguage. */
    private TruffleObject regexEngine;

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
        CollatorCaseSensitiveCompare,
        CollatorCompare,
        DateTimeFormatFormat,
        NumberFormatFormat,
        ProxyRevokerFunction,
        PromiseResolveFunction,
        PromiseRejectFunction,
        PromiseGetCapabilitiesExecutor,
        PromiseResolveThenableJob,
        PromiseReactionJob,
        PromiseAllResolveElement,
        PromiseThenFinally,
        PromiseCatchFinally,
        PromiseValueThunk,
        PromiseThrower,
    }

    @CompilationFinal(dimensions = 1) private final JSFunctionData[] builtinFunctionData;

    private volatile JSFunctionData boundFunctionData;
    private volatile JSFunctionData boundConstructorFunctionData;

    final JSFunctionData throwerFunctionData;
    final JSFunctionData protoGetterFunctionData;
    final JSFunctionData protoSetterFunctionData;

    private volatile Map<Shape, JSShapeData> shapeDataMap;

    private List<JSRealm> realmList;

    final Assumption noChildRealmsAssumption;
    final Assumption singleRealmAssumption;
    final boolean isMultiContext;

    private volatile boolean isRealmInitialized;

    /**
     * According to ECMA2017 8.4 the queue of pending jobs (promises reactions) must be processed
     * when the current stack is empty. For Interop, we assume that the stack is empty when (1) we
     * are called from another foreign language, and (2) there are no other nested JS Interop calls.
     *
     * This flag is used to implement this semantics.
     */
    private int interopCallStackDepth = 0;

    /**
     * Temporary field until transition is complete.
     */
    @CompilationFinal private JSRealm lastRealm;

    private final ContextReference<JSRealm> contextRef;
    @CompilationFinal private AllocationReporter allocationReporter;

    /**
     * ECMA2017 8.7 Agent object.
     *
     * Temporary field until JSAgents are supported in Node.js.
     *
     * Initialized after engine creation either by some testing harness or by node.
     */
    @CompilationFinal private JSAgent agent;

    /**
     * Java Interop Workers factory.
     */
    @CompilationFinal private EcmaAgent mainWorker;
    @CompilationFinal private EcmaAgent.Factory javaInteropWorkersFactory;
    @CompilationFinal private boolean shouldProcessJavaInteropAsyncTasks = true;

    private final JSContextOptions contextOptions;

    private final Map<Builtin, JSFunctionData> builtinFunctionDataMap = new ConcurrentHashMap<>();

    private final JSPrototypeData nullPrototypeData = new JSPrototypeData();
    private final JSPrototypeData inObjectPrototypeData = new JSPrototypeData();

    private volatile ClassValue<Class<?>> javaAdapterClasses;

    private final JSFunctionFactory functionFactoryNamed;
    private final JSFunctionFactory functionFactoryAnonymous;
    private final JSFunctionFactory constructorFactoryNamed;
    private final JSFunctionFactory constructorFactoryAnonymous;
    private final JSFunctionFactory strictFunctionFactoryNamed;
    private final JSFunctionFactory strictFunctionFactoryAnonymous;
    private final JSFunctionFactory strictConstructorFactoryNamed;
    private final JSFunctionFactory strictConstructorFactoryAnonymous;

    private final JSFunctionFactory generatorFunctionFactoryNamed;
    private final JSFunctionFactory generatorFunctionFactoryAnonymous;
    private final JSFunctionFactory asyncFunctionFactoryNamed;
    private final JSFunctionFactory asyncFunctionFactoryAnonymous;
    private final JSFunctionFactory asyncGeneratorFunctionFactoryNamed;
    private final JSFunctionFactory asyncGeneratorFunctionFactoryAnonymous;

    private final JSFunctionFactory boundFunctionFactoryNamed;
    private final JSFunctionFactory boundFunctionFactoryAnonymous;

    static final CompilableFunction<JSRealm, DynamicObject> functionPrototypeSupplier = JSRealm::getFunctionPrototype;
    static final CompilableFunction<JSRealm, DynamicObject> asyncFunctionPrototypeSupplier = r -> r.getAsyncFunctionConstructor().getPrototype();
    static final CompilableFunction<JSRealm, DynamicObject> generatorFunctionPrototypeSupplier = r -> r.getGeneratorFunctionConstructor().getPrototype();
    static final CompilableFunction<JSRealm, DynamicObject> asyncGeneratorFunctionPrototypeSupplier = r -> r.getAsyncGeneratorFunctionConstructor().getPrototype();

    private final JSObjectFactory arrayFactory;
    private final JSObjectFactory lazyRegexArrayFactory;
    private final JSObjectFactory booleanFactory;
    private final JSObjectFactory numberFactory;
    private final JSObjectFactory bigIntFactory;
    private final JSObjectFactory stringFactory;
    private final JSObjectFactory regExpFactory;
    private final JSObjectFactory dateFactory;
    private final JSObjectFactory nonStrictArgumentsFactory;
    private final JSObjectFactory strictArgumentsFactory;
    private final JSObjectFactory callSiteFactory;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] errorObjectFactories;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] errorWithMessageObjectFactories;

    private final JSObjectFactory symbolFactory;
    private final JSObjectFactory mapFactory;
    private final JSObjectFactory setFactory;
    private final JSObjectFactory weakMapFactory;
    private final JSObjectFactory weakSetFactory;
    private final JSObjectFactory proxyFactory;
    private final JSObjectFactory promiseFactory;
    private final JSObjectFactory dataViewFactory;
    private final JSObjectFactory arrayBufferFactory;
    private final JSObjectFactory directArrayBufferFactory;
    private final JSObjectFactory sharedArrayBufferFactory;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] typedArrayFactories;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] directTypedArrayFactories;

    private final JSObjectFactory enumerateIteratorFactory;
    private final JSObjectFactory generatorObjectFactory;
    private final JSObjectFactory asyncGeneratorObjectFactory;
    private final JSObjectFactory asyncFromSyncIteratorFactory;

    private final JSObjectFactory collatorFactory;
    private final JSObjectFactory numberFormatFactory;
    private final JSObjectFactory pluralRulesFactory;
    private final JSObjectFactory dateTimeFormatFactory;

    private final JSObjectFactory javaImporterFactory;
    private final JSObjectFactory javaPackageFactory;
    private final JSObjectFactory javaInteropWorkerObjectFactory;
    private final JSObjectFactory jsAdapterFactory;

    private final int factoryCount;

    protected JSContext(Evaluator evaluator, JSFunctionLookup lookup, JSContextOptions contextOptions, AbstractJavaScriptLanguage lang, TruffleLanguage.Env env) {
        this.functionLookup = lookup;
        this.contextOptions = contextOptions;

        if (env != null) { // env could still be null
            this.contextOptions.setOptionValues(env.getOptions());
            setAllocationReporter(env);
        }

        this.language = lang;
        this.contextRef = lang == null ? null : lang.getContextReference();

        this.emptyShape = createEmptyShape();
        this.emptyShapePrototypeInObject = createEmptyShapePrototypeInObject();
        this.globalScopeShape = createGlobalScopeShape();

        this.noSuchPropertyUnusedAssumption = Truffle.getRuntime().createAssumption("noSuchPropertyUnusedAssumption");
        this.noSuchMethodUnusedAssumption = Truffle.getRuntime().createAssumption("noSuchMethodUnusedAssumption");
        this.arrayPrototypeNoElementsAssumption = Truffle.getRuntime().createAssumption("arrayPrototypeNoElementsAssumption");
        this.typedArrayNotDetachedAssumption = Truffle.getRuntime().createAssumption("typedArrayNotDetachedAssumption");
        this.fastArrayAssumption = Truffle.getRuntime().createAssumption("fastArrayAssumption");
        this.fastArgumentsObjectAssumption = Truffle.getRuntime().createAssumption("fastArgumentsObjectAssumption");

        this.evaluator = evaluator;
        this.nodeFactory = evaluator.getDefaultNodeFactory();

        this.moduleNamespaceFactory = JSObjectFactory.createBound(this, Null.instance, JSModuleNamespace.makeInitialShape(this).createFactory());

        this.promiseJobsQueue = new LinkedList<>();
        this.promiseJobsQueueNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseJobsQueueNotUsedAssumption");

        this.promiseHookNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseHookNotUsedAssumption");
        this.promiseRejectionTrackerNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseRejectionTrackerNotUsedAssumption");

        this.emptyFunctionCallTarget = createEmptyFunctionCallTarget(lang);
        this.speciesGetterFunctionCallTarget = createSpeciesGetterFunctionCallTarget(lang);

        this.builtinFunctionData = new JSFunctionData[BuiltinFunctionKey.values().length];

        this.timeProfiler = JSTruffleOptions.ProfileTime ? new TimeProfiler() : null;
        this.javaWrapperFactory = JSTruffleOptions.NashornJavaInterop ? JSObjectFactory.createBound(this, Null.instance, JSJavaWrapper.makeShape(this).createFactory()) : null;

        this.dictionaryShapeNullPrototype = JSTruffleOptions.DictionaryObject ? JSDictionaryObject.makeDictionaryShape(this, null) : null;

        this.singleRealmAssumption = Truffle.getRuntime().createAssumption("single realm");
        this.noChildRealmsAssumption = Truffle.getRuntime().createAssumption("no child realms");

        if (JSTruffleOptions.Test262Mode || JSTruffleOptions.TestV8Mode) {
            this.setJSAgent(new DebugJSAgent(env, contextOptions.canAgentBlock()));
        } else {
            this.setJSAgent(new MainJSAgent());
        }
        if (contextOptions.isV8RealmBuiltin()) {
            this.realmList = new ArrayList<>();
        }

        this.throwerFunctionData = throwTypeErrorFunction();
        boolean annexB = isOptionAnnexB();
        this.protoGetterFunctionData = annexB ? protoGetterFunction() : null;
        this.protoSetterFunctionData = annexB ? protoSetterFunction() : null;

        this.isMultiContext = lang != null && lang.isMultiContext();

        // shapes and factories
        PrototypeSupplier objectPrototypeSupplier = JSUserObject.INSTANCE;
        CompilableBiFunction<JSContext, DynamicObject, Shape> ordinaryObjectShapeSupplier = JSUserObject.INSTANCE::makeInitialShape;
        JSObjectFactory.IntrinsicBuilder builder = new JSObjectFactory.IntrinsicBuilder(this);

        this.functionFactoryNamed = builder.function(functionPrototypeSupplier, false, false, false, false, false, false);
        this.functionFactoryAnonymous = builder.function(functionPrototypeSupplier, false, true, false, false, false, false);
        this.constructorFactoryNamed = builder.function(functionPrototypeSupplier, false, false, true, false, false, false);
        this.constructorFactoryAnonymous = builder.function(functionPrototypeSupplier, false, true, true, false, false, false);
        this.strictFunctionFactoryNamed = builder.function(functionPrototypeSupplier, true, false, false, false, false, false);
        this.strictFunctionFactoryAnonymous = builder.function(functionPrototypeSupplier, true, true, false, false, false, false);
        this.strictConstructorFactoryNamed = builder.function(functionPrototypeSupplier, true, false, true, false, false, false);
        this.strictConstructorFactoryAnonymous = builder.function(functionPrototypeSupplier, true, true, true, false, false, false);

        this.asyncFunctionFactoryNamed = builder.function(asyncFunctionPrototypeSupplier, true, false, false, false, false, true);
        this.asyncFunctionFactoryAnonymous = builder.function(asyncFunctionPrototypeSupplier, true, true, false, false, false, true);
        this.generatorFunctionFactoryNamed = builder.function(generatorFunctionPrototypeSupplier, true, false, false, true, false, false);
        this.generatorFunctionFactoryAnonymous = builder.function(generatorFunctionPrototypeSupplier, true, true, false, true, false, false);
        this.asyncGeneratorFunctionFactoryNamed = builder.function(asyncGeneratorFunctionPrototypeSupplier, true, false, false, true, false, true);
        this.asyncGeneratorFunctionFactoryAnonymous = builder.function(asyncGeneratorFunctionPrototypeSupplier, true, true, false, true, false, true);

        this.boundFunctionFactoryNamed = builder.function(functionPrototypeSupplier, true, false, false, false, true, false);
        this.boundFunctionFactoryAnonymous = builder.function(functionPrototypeSupplier, true, true, false, false, true, false);

        this.arrayFactory = builder.create(JSArray.INSTANCE);
        this.lazyRegexArrayFactory = builder.create(JSArray.INSTANCE, JSRegExp::makeLazyRegexArrayShape);
        this.booleanFactory = builder.create(JSBoolean.INSTANCE);
        this.numberFactory = builder.create(JSNumber.INSTANCE);
        this.bigIntFactory = builder.create(JSBigInt.INSTANCE);
        this.stringFactory = builder.create(JSString.INSTANCE);
        this.regExpFactory = builder.create(JSRegExp.INSTANCE);
        this.dateFactory = builder.create(JSDate.INSTANCE);

        this.symbolFactory = builder.create(JSSymbol.INSTANCE);
        this.mapFactory = builder.create(JSMap.INSTANCE);
        this.setFactory = builder.create(JSSet.INSTANCE);
        this.weakMapFactory = builder.create(JSWeakMap.INSTANCE);
        this.weakSetFactory = builder.create(JSWeakSet.INSTANCE);
        this.proxyFactory = builder.create(JSProxy.INSTANCE);
        this.promiseFactory = builder.create(JSPromise.INSTANCE);
        this.dataViewFactory = builder.create(JSDataView.INSTANCE);
        this.arrayBufferFactory = builder.create(JSArrayBuffer.HEAP_INSTANCE);
        this.directArrayBufferFactory = builder.create(JSArrayBuffer.DIRECT_INSTANCE);
        this.sharedArrayBufferFactory = isOptionSharedArrayBuffer() ? builder.create(JSSharedArrayBuffer.INSTANCE) : null;
        this.typedArrayFactories = new JSObjectFactory[TypedArray.factories().length];
        this.directTypedArrayFactories = new JSObjectFactory[TypedArray.factories().length];
        for (TypedArrayFactory factory : TypedArray.factories()) {
            directTypedArrayFactories[factory.getFactoryIndex()] = builder.create(factory, (c, p) -> JSArrayBufferView.makeInitialArrayBufferViewShape(c, p, true));
            typedArrayFactories[factory.getFactoryIndex()] = builder.create(factory, (c, p) -> JSArrayBufferView.makeInitialArrayBufferViewShape(c, p, false));
        }

        JSErrorType[] errorTypes = JSErrorType.values();
        this.errorObjectFactories = new JSObjectFactory[errorTypes.length];
        this.errorWithMessageObjectFactories = new JSObjectFactory[errorTypes.length];
        for (JSErrorType type : errorTypes) {
            errorObjectFactories[type.ordinal()] = builder.create(type, JSError.INSTANCE::makeInitialShape);
            errorWithMessageObjectFactories[type.ordinal()] = builder.create(type, (c, p) -> JSError.addMessagePropertyToShape(JSError.INSTANCE.makeInitialShape(c, p)));
        }
        this.callSiteFactory = builder.create(r -> r.getCallSiteConstructor().getPrototype(), JSError::makeInitialCallSiteShape);
        this.nonStrictArgumentsFactory = builder.create(objectPrototypeSupplier, JSArgumentsObject::makeInitialNonStrictArgumentsShape);
        this.strictArgumentsFactory = builder.create(objectPrototypeSupplier, JSArgumentsObject::makeInitialStrictArgumentsShape);
        this.enumerateIteratorFactory = builder.create(JSRealm::getEnumerateIteratorPrototype, JSFunction::makeInitialEnumerateIteratorShape);

        this.generatorObjectFactory = builder.create(JSRealm::getGeneratorObjectPrototype, ordinaryObjectShapeSupplier);
        this.asyncGeneratorObjectFactory = builder.create(JSRealm::getAsyncGeneratorObjectPrototype, ordinaryObjectShapeSupplier);
        this.asyncFromSyncIteratorFactory = builder.create(JSRealm::getAsyncFromSyncIteratorPrototype, ordinaryObjectShapeSupplier);

        this.collatorFactory = builder.create(JSCollator.INSTANCE);
        this.numberFormatFactory = builder.create(JSNumberFormat.INSTANCE);
        this.dateTimeFormatFactory = builder.create(JSDateTimeFormat.INSTANCE);
        this.pluralRulesFactory = builder.create(JSPluralRules.INSTANCE);

        boolean nashornCompat = isOptionNashornCompatibilityMode() || JSTruffleOptions.NashornCompatibilityMode;
        boolean nashornJavaInterop = JSRealm.isJavaInteropAvailable() && (isOptionNashornCompatibilityMode() || JSTruffleOptions.NashornJavaInterop);
        this.javaImporterFactory = nashornJavaInterop ? builder.create(JavaImporter.instance()) : null;
        this.jsAdapterFactory = nashornCompat ? builder.create(JSAdapter.INSTANCE) : null;
        this.javaPackageFactory = JSRealm.isJavaInteropAvailable() ? builder.create(objectPrototypeSupplier, JavaPackage.INSTANCE::makeInitialShape) : null;
        this.javaInteropWorkerObjectFactory = JSRealm.isJavaInteropAvailable() ? builder.create(JSJavaWorkerBuiltin.INSTANCE) : null;

        this.factoryCount = builder.finish();
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

    public final ParserOptions getParserOptions() {
        return contextOptions.getParserOptions();
    }

    public final void setParserOptions(ParserOptions parserOptions) {
        this.contextOptions.setParserOptions(parserOptions);
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
        return new JSContext(evaluator, lookup, contextOptions, lang, env);
    }

    public JSRealm createRealm(TruffleLanguage.Env env) {
        boolean isTop = env == null || env.getContext().getParent() == null;
        if (isRealmInitialized) {
            singleRealmAssumption.invalidate("single realm assumption");
        }
        JSRealm newRealm = new JSRealm(this, env);
        newRealm.setupGlobals();
        if (realmList != null) {
            addToRealmList(newRealm);
        }
        if (isTop) {
            newRealm.initRealmBuiltinObject();
        }
        setRealmInitialized(true);
        return newRealm;
    }

    public final Shape createEmptyShape() {
        return makeEmptyShapeWithNullPrototype(JSUserObject.INSTANCE);
    }

    private Shape createEmptyShapePrototypeInObject() {
        return makeEmptyShapeWithPrototypeInObject(JSUserObject.INSTANCE, JSObject.PROTO_PROPERTY);
    }

    public final Shape makeEmptyShapeWithNullPrototype(JSClass jsclass) {
        Shape protoChildTree = nullPrototypeData.getProtoChildTree(jsclass);
        if (protoChildTree != null) {
            return protoChildTree;
        }
        return nullPrototypeData.getOrAddProtoChildTree(jsclass, JSShape.makeEmptyRoot(JSObject.LAYOUT, jsclass, this));
    }

    public final Shape makeEmptyShapeWithPrototypeInObject(JSClass jsclass, Property protoProperty) {
        Shape protoChildTree = inObjectPrototypeData.getProtoChildTree(jsclass);
        if (protoChildTree != null) {
            return protoChildTree;
        }
        return inObjectPrototypeData.getOrAddProtoChildTree(jsclass, JSShape.makeEmptyRoot(JSObject.LAYOUT, jsclass, this, protoProperty));
    }

    private Shape createGlobalScopeShape() {
        return JSShape.makeEmptyRoot(JSObject.LAYOUT, JSGlobalObject.INSTANCE, this);
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
        promiseJobQueueAdd(newTarget);
    }

    @TruffleBoundary
    private void promiseJobQueueAdd(DynamicObject newTarget) {
        promiseJobsQueue.push(newTarget);
    }

    private void invalidatePromiseQueueNotUsedAssumption() {
        if (promiseJobsQueueNotUsedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseJobsQueueNotUsedAssumption.invalidate("promise jobs queue unused assumption");
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
        Object thisArg = Undefined.instance;
        while (promiseJobsQueue.size() > 0) {
            DynamicObject nextJob = promiseJobsQueue.pollLast();
            if (JSFunction.isJSFunction(nextJob)) {
                JSRealm functionRealm = JSFunction.getRealm(nextJob);
                Object prev = functionRealm.getTruffleContext().enter();
                try {
                    JSFunction.call(nextJob, thisArg, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                } finally {
                    functionRealm.getTruffleContext().leave(prev);
                }
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
        this.interopRuntime = interopRuntime;
    }

    public TimeProfiler getTimeProfiler() {
        return timeProfiler;
    }

    /**
     * Get the current Realm using {@link ContextReference}.
     */
    public JSRealm getRealm() {
        if (CompilerDirectives.inInterpreter() && !isRealmInitialized) {
            throw Errors.shouldNotReachHere("getRealm() while initializing Realm");
        }
        if (contextRef == null) {
            return lastRealm;
        }
        if (isSingleRealm()) {
            assert lastRealm == contextRef.get();
            return lastRealm;
        }
        JSRealm currentRealm = contextRef.get();
        assert currentRealm != null;
        return currentRealm;
    }

    public final Shape getEmptyShapeNullPrototype() {
        return emptyShape;
    }

    public final Shape getEmptyShapePrototypeInObject() {
        return emptyShapePrototypeInObject;
    }

    public final Shape getGlobalScopeShape() {
        return globalScopeShape;
    }

    public final JSObjectFactory getArrayFactory() {
        return arrayFactory;
    }

    public final JSObjectFactory getLazyRegexArrayFactory() {
        return lazyRegexArrayFactory;
    }

    public final JSObjectFactory getStringFactory() {
        return stringFactory;
    }

    public final JSObjectFactory getBooleanFactory() {
        return booleanFactory;
    }

    public final JSObjectFactory getNumberFactory() {
        return numberFactory;
    }

    public final JSObjectFactory getBigIntFactory() {
        return bigIntFactory;
    }

    public final JSObjectFactory getSymbolFactory() {
        return symbolFactory;
    }

    public final JSObjectFactory getArrayBufferViewFactory(TypedArrayFactory factory) {
        return typedArrayFactories[factory.getFactoryIndex()];
    }

    public final JSObjectFactory getArrayBufferFactory() {
        return arrayBufferFactory;
    }

    public final JSObjectFactory getDirectArrayBufferViewFactory(TypedArrayFactory factory) {
        return directTypedArrayFactories[factory.getFactoryIndex()];
    }

    public final JSObjectFactory getDirectArrayBufferFactory() {
        return directArrayBufferFactory;
    }

    public final JSObjectFactory getRegExpFactory() {
        return regExpFactory;
    }

    public final JSObjectFactory getDateFactory() {
        return dateFactory;
    }

    public final JSObjectFactory getEnumerateIteratorFactory() {
        return enumerateIteratorFactory;
    }

    public final JSObjectFactory getMapFactory() {
        return mapFactory;
    }

    public final JSObjectFactory getWeakMapFactory() {
        return weakMapFactory;
    }

    public final JSObjectFactory getSetFactory() {
        return setFactory;
    }

    public final JSObjectFactory getWeakSetFactory() {
        return weakSetFactory;
    }

    public final JSObjectFactory getDataViewFactory() {
        return dataViewFactory;
    }

    public final JSObjectFactory getProxyFactory() {
        return proxyFactory;
    }

    public final JSObjectFactory getSharedArrayBufferFactory() {
        assert isOptionSharedArrayBuffer();
        return sharedArrayBufferFactory;
    }

    public final JSObjectFactory getNonStrictArgumentsFactory() {
        return nonStrictArgumentsFactory;
    }

    public final JSObjectFactory getStrictArgumentsFactory() {
        return strictArgumentsFactory;
    }

    public final JSObjectFactory getCallSiteFactory() {
        return callSiteFactory;
    }

    public final JSObjectFactory getErrorFactory(JSErrorType type, boolean withMessage) {
        return (withMessage ? errorWithMessageObjectFactories : errorObjectFactories)[type.ordinal()];
    }

    public final JSObjectFactory getPromiseFactory() {
        return promiseFactory;
    }

    public final JSObjectFactory.BoundProto getModuleNamespaceFactory() {
        return moduleNamespaceFactory;
    }

    public final JSObjectFactory getGeneratorObjectFactory() {
        return generatorObjectFactory;
    }

    public final JSObjectFactory getAsyncGeneratorObjectFactory() {
        return asyncGeneratorObjectFactory;
    }

    public final JSObjectFactory getAsyncFromSyncIteratorFactory() {
        return asyncFromSyncIteratorFactory;
    }

    public final JSObjectFactory getCollatorFactory() {
        return collatorFactory;
    }

    public final JSObjectFactory getNumberFormatFactory() {
        return numberFormatFactory;
    }

    public final JSObjectFactory getPluralRulesFactory() {
        return pluralRulesFactory;
    }

    public final JSObjectFactory getDateTimeFormatFactory() {
        return dateTimeFormatFactory;
    }

    public final JSObjectFactory getJavaImporterFactory() {
        return javaImporterFactory;
    }

    public final JSObjectFactory getJSAdapterFactory() {
        return jsAdapterFactory;
    }

    public final JSObjectFactory getJavaPackageFactory() {
        return javaPackageFactory;
    }

    public final JSObjectFactory.BoundProto getJavaWrapperFactory() {
        return javaWrapperFactory;
    }

    public JSObjectFactory getSIMDTypeFactory(SIMDTypeFactory<? extends SIMDType> factory) {
        return getRealm().getSIMDTypeFactory(factory);
    }

    void setRealm(JSRealm realm) {
        this.lastRealm = realm;
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
                TruffleObject regexEngineBuilder = (TruffleObject) getRealm().getEnv().parse(Source.newBuilder(RegexLanguage.ID, "", "TRegex Engine Builder Request").build()).call();
                String regexOptions = createRegexEngineOptions();
                try {
                    regexEngine = (TruffleObject) ForeignAccess.sendExecute(Message.EXECUTE.createNode(), regexEngineBuilder, regexOptions, joniCompiler);
                } catch (InteropException ex) {
                    throw ex.raise();
                }
            } else {
                regexEngine = new CachingRegexEngine(joniCompiler, JSTruffleOptions.RegexRegressionTestMode);
            }
        }
        return regexEngine;
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
                        TruffleFile truffleFile = getRealm().getEnv().getTruffleFile(moduleFile.getPath());
                        Source source = Source.newBuilder(AbstractJavaScriptLanguage.ID, truffleFile).name(specifier).build();
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

    private synchronized void addToRealmList(JSRealm newRealm) {
        CompilerAsserts.neverPartOfCompilation();
        assert !realmList.contains(newRealm);
        realmList.add(newRealm);
    }

    public synchronized JSRealm getFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        return realmList.get(idx);
    }

    public synchronized int getIndexFromRealmList(JSRealm rlm) {
        CompilerAsserts.neverPartOfCompilation();
        return realmList.indexOf(rlm);
    }

    public synchronized void removeFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        realmList.set(idx, null);
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

    void setAllocationReporter(TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        this.allocationReporter = env.lookup(AllocationReporter.class);
    }

    public final AllocationReporter getAllocationReporter() {
        return allocationReporter;
    }

    public final <T> T trackAllocation(T object) {
        AllocationReporter reporter = getAllocationReporter();
        if (reporter != null) {
            reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
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

    public boolean isOptionNashornCompatibilityMode() {
        return contextOptions.isNashornCompatibilityMode();
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

    public long getTimerResolution() {
        return contextOptions.getTimerResolution();
    }

    public boolean isOptionAgentCanBlock() {
        return contextOptions.canAgentBlock();
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

    public JSObjectFactory getJavaInteropWorkerObjectFactory() {
        return javaInteropWorkerObjectFactory;
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
            promiseRejectionTrackerNotUsedAssumption.invalidate("promise rejection tracker unused");
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
            promiseHookNotUsedAssumption.invalidate("promise hook unused");
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
        JSFunctionData functionData = builtinFunctionData[index];
        if (functionData != null) {
            return functionData;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        synchronized (this) {
            functionData = builtinFunctionData[index];
            if (functionData == null) {
                functionData = factory.apply(this);
                builtinFunctionData[index] = functionData;
            }
            return functionData;
        }
    }

    public final JSFunctionData getBuiltinFunctionData(Builtin key) {
        CompilerAsserts.neverPartOfCompilation();
        return builtinFunctionDataMap.get(key);
    }

    public final void putBuiltinFunctionData(Builtin key, JSFunctionData functionData) {
        CompilerAsserts.neverPartOfCompilation();
        builtinFunctionDataMap.putIfAbsent(key, functionData);
    }

    public final boolean neverCreatedChildRealms() {
        return noChildRealmsAssumption.isValid();
    }

    public final boolean isSingleRealm() {
        return singleRealmAssumption.isValid();
    }

    public final void assumeSingleRealm() throws InvalidAssumptionException {
        singleRealmAssumption.check();
    }

    void setRealmInitialized(boolean initialized) {
        this.isRealmInitialized = initialized;
    }

    public JSContextOptions getContextOptions() {
        return contextOptions;
    }

    public Class<?> getJavaAdapterClassFor(Class<?> clazz) {
        if (JSTruffleOptions.SubstrateVM) {
            throw Errors.unsupported("JavaAdapter");
        }
        if (javaAdapterClasses == null) {
            synchronized (this) {
                if (javaAdapterClasses == null) {
                    javaAdapterClasses = new ClassValue<Class<?>>() {
                        @Override
                        protected Class<?> computeValue(Class<?> type) {
                            return JavaAdapterFactory.getAdapterClassFor(type);
                        }
                    };
                }
            }
        }
        return javaAdapterClasses.get(clazz);
    }

    public final boolean isMultiContext() {
        return isMultiContext;
    }

    public JSFunctionFactory getFunctionFactory(JSFunctionData functionData) {
        boolean isBuiltin = functionData.isBuiltin();
        boolean strictFunctionProperties = functionData.hasStrictFunctionProperties();
        boolean isConstructor = functionData.isConstructor();
        boolean isGenerator = functionData.isGenerator();
        boolean isAsync = functionData.isAsync();
        boolean isAnonymous = functionData.getName().isEmpty();
        assert !isBuiltin || (!isGenerator && !isAsync) : "built-in functions are never generator or async functions!";
        if (isAsync) {
            if (isGenerator) {
                return isAnonymous ? asyncGeneratorFunctionFactoryAnonymous : asyncGeneratorFunctionFactoryNamed;
            } else {
                return isAnonymous ? asyncFunctionFactoryAnonymous : asyncFunctionFactoryNamed;
            }
        } else if (isGenerator) {
            return isAnonymous ? generatorFunctionFactoryAnonymous : generatorFunctionFactoryNamed;
        } else if (isConstructor && !isBuiltin) {
            if (strictFunctionProperties) {
                return isAnonymous ? strictConstructorFactoryAnonymous : strictConstructorFactoryNamed;
            } else {
                return isAnonymous ? constructorFactoryAnonymous : constructorFactoryNamed;
            }
        } else {
            // Built-in constructor functions end up here due to the way they're initialized.
            if (strictFunctionProperties) {
                return isAnonymous ? strictFunctionFactoryAnonymous : strictFunctionFactoryNamed;
            } else {
                return isAnonymous ? functionFactoryAnonymous : functionFactoryNamed;
            }
        }
    }

    public JSFunctionFactory getBoundFunctionFactory(JSFunctionData functionData, boolean isAnonymous) {
        assert functionData.isStrict();
        return isAnonymous ? boundFunctionFactoryAnonymous : boundFunctionFactoryNamed;
    }

    JSObjectFactory.RealmData newObjectFactoryRealmData() {
        if (isMultiContext()) {
            return null; // unused
        } else {
            return new JSObjectFactory.RealmData(factoryCount);
        }
    }

    private JSFunctionData throwTypeErrorFunction() {
        CallTarget throwTypeErrorCallTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                throw Errors.createTypeError("[[ThrowTypeError]] defined by ECMAScript");
            }
        });
        return JSFunctionData.create(this, throwTypeErrorCallTarget, throwTypeErrorCallTarget, 0, "", false, false, false, false);
    }

    private JSFunctionData protoSetterFunction() {
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = JSRuntime.toObject(JSContext.this, JSArguments.getThisObject(arguments));
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    return Undefined.instance;
                }
                Object value = JSArguments.getUserArgument(arguments, 0);

                DynamicObject current = JSObject.getPrototype(thisObj);
                if (current == value) {
                    return Undefined.instance; // true in OrdinarySetPrototype
                }
                if (!JSObject.isExtensible(thisObj)) {
                    throwCannotSetNonExtensibleProtoError(thisObj);
                }

                if (!(JSObject.isDynamicObject(value)) || value == Undefined.instance) {
                    return Undefined.instance;
                }
                if (!JSObject.setPrototype(thisObj, (DynamicObject) value)) {
                    throwCannotSetProtoError(thisObj);
                }
                return Undefined.instance;
            }

            @TruffleBoundary
            private void throwCannotSetNonExtensibleProtoError(DynamicObject thisObj) {
                throw Errors.createTypeError("Cannot set __proto__ of non-extensible " + JSObject.defaultToString(thisObj));
            }

            @TruffleBoundary
            private void throwCannotSetProtoError(DynamicObject thisObj) {
                throw Errors.createTypeError("Cannot set __proto__ of " + JSObject.defaultToString(thisObj));
            }
        });
        return JSFunctionData.createCallOnly(this, callTarget, 0, "set " + JSObject.PROTO);
    }

    private JSFunctionData protoGetterFunction() {
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                return JSObject.getPrototype(JSRuntime.toObject(JSContext.this, obj));
            }
        });
        return JSFunctionData.createCallOnly(this, callTarget, 0, "get " + JSObject.PROTO);
    }
}
