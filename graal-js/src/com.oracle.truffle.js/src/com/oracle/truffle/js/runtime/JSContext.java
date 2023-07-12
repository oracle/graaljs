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
package com.oracle.truffle.js.runtime;

import static com.oracle.truffle.js.runtime.JSRealm.SYMBOL_ITERATOR_NAME;
import static com.oracle.truffle.js.runtime.builtins.JSNonProxy.GET_SYMBOL_SPECIES_NAME;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ThrowTypeErrorRootNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.promise.BuiltinPromiseRejectionTracker;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.Builtin;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDictionary;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistryObject;
import com.oracle.truffle.js.runtime.builtins.JSForInIterator;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapIterator;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetIterator;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringIterator;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUncheckedProxyHandler;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshot;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextVariable;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDisplayNames;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZone;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSPrototypeData;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.JSShapeData;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;
import com.oracle.truffle.js.runtime.util.DebugJSAgent;
import com.oracle.truffle.js.runtime.util.StableContextOptionValue;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TimeProfiler;

public class JSContext {

    private static final VarHandle FUNCTION_DATA_ARRAY_VAR_HANDLE = MethodHandles.arrayElementVarHandle(JSFunctionData[].class);

    private final Evaluator evaluator;

    private final JavaScriptLanguage language;

    private final Shape emptyShape;
    private final Shape emptyShapePrototypeInObject;
    private final Shape promiseShapePrototypeInObject;
    private final Shape globalScopeShape;

    /**
     * Slot for a context-specific data of the embedder of the JS engine.
     */
    private Object embedderData;

    /**
     * Nashorn compatibility mode only: Assumption is valid as long as no
     * {@code __noSuchProperty__}, {@code __noSuchMethod__} properties have been defined.
     */
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
    private final Assumption typedArrayNotDetachedAssumption;

    /**
     * Assumption: Static RegExp results (RegExp.$1 etc) are never used. As long as this assumption
     * holds, just the arguments of the last RegExp execution are stored, allowing RegExp result
     * objects to be virtualized in RegExp#exec().
     */
    private final Assumption regExpStaticResultUnusedAssumption;

    /**
     * Assumption: The global object has not been replaced and its prototype has not been changed.
     * While valid, guarantees that globalObject.[[HasProperty]] is side effect free.
     */
    private final Assumption globalObjectPristineAssumption;

    private final Map<TruffleString, Symbol> symbolRegistry = new ConcurrentHashMap<>();
    private final Map<TruffleString, Symbol> privateSymbolRegistry = new ConcurrentHashMap<>();

    private final Object nodeFactory;

    private final TimeProfiler timeProfiler;

    private final JSObjectFactory.BoundProto moduleNamespaceFactory;

    @CompilationFinal private Object tRegexEmptyResult;

    private final String regexOptions;
    private final String regexValidateOptions;

    private final Shape regExpGroupsEmptyShape;

    private PrepareStackTraceCallback prepareStackTraceCallback;
    private final Assumption prepareStackTraceCallbackNotUsedAssumption;

    private PromiseRejectionTracker promiseRejectionTracker;
    private final Assumption promiseRejectionTrackerNotUsedAssumption;

    private PromiseHook promiseHook;
    private final Assumption promiseHookNotUsedAssumption;

    private ImportMetaInitializer importMetaInitializer;
    private final Assumption importMetaInitializerNotUsedAssumption;
    private ImportModuleDynamicallyCallback importModuleDynamicallyCallback;
    private final Assumption importModuleDynamicallyCallbackNotUsedAssumption;

    private final CallTarget emptyFunctionCallTarget;

    public final JSFunctionData symbolSpeciesThisGetterFunctionData;
    public final JSFunctionData symbolIteratorThisGetterFunctionData;

    private volatile CallTarget notConstructibleCallTargetCache;
    private volatile CallTarget generatorNotConstructibleCallTargetCache;

    private static final VarHandle notConstructibleCallTargetVarHandle;
    private static final VarHandle generatorNotConstructibleCallTargetVarHandle;

    // Used to track singleton symbols allocations across aux engine cache runs.
    private Object symbolUsageMarker = new Object();

    public void resetSymbolUsageMarker() {
        CompilerAsserts.neverPartOfCompilation();
        // Symbols that were used exactly once in previous executions, will most-likely
        // be created one time only when the cache is re-loaded. In this case we can re-use
        // the Symbol cached in the aux image cache to avoid de-optimizations.
        this.symbolUsageMarker = new Object();
    }

    public Object getSymbolUsageMarker() {
        return symbolUsageMarker;
    }

    public enum BuiltinFunctionKey {
        BoundFunction,
        BoundConstructor,
        BoundFunctionAsync,
        BoundConstructorAsync,
        ArrayFlattenIntoArray,
        AwaitFulfilled,
        AwaitRejected,
        AsyncGeneratorReturnFulfilled,
        AsyncGeneratorReturnRejected,
        AsyncFromSyncIteratorValueUnwrap,
        AsyncIteratorYield,
        AsyncIteratorUnwrapYieldResumptionClose,
        AsyncIteratorUnwrapYieldResumptionCloseResumption,
        AsyncIteratorUnwrapYieldResumptionCloseInnerIterator,
        AsyncIteratorUnwrapYieldResumptionCloseInnerIteratorResumption,
        AsyncIteratorIfAbruptClose,
        AsyncIteratorIfAbruptReturn,
        AsyncIteratorGeneratorIfAbruptClose,
        AsyncIteratorGeneratorIfAbruptReturn,
        AsyncIteratorGeneratorReturn,
        AsyncIteratorClose,
        AsyncIteratorCloseAbrupt,
        AsyncIteratorMap,
        AsyncIteratorMapWithValue,
        AsyncIteratorFilter,
        AsyncIteratorFilterWithValue,
        AsyncIteratorFilterWithResult,
        AsyncIteratorTake,
        AsyncIteratorTakeWithValue,
        AsyncIteratorDrop,
        AsyncIteratorDropWithValueLoop,
        AsyncIteratorDropWithValue,
        AsyncIteratorFlatMap,
        AsyncIteratorFlatMapWithValue,
        AsyncIteratorFlatMapWithResult,
        AsyncIteratorFlatMapInnerWithValue,
        AsyncIteratorReduce,
        AsyncIteratorReduceWithResult,
        AsyncIteratorReduceInitial,
        AsyncIteratorToArray,
        AsyncIteratorForEach,
        AsyncIteratorForEachWithResult,
        AsyncIteratorSome,
        AsyncIteratorSomeWithResult,
        AsyncIteratorEvery,
        AsyncIteratorEveryWithResult,
        AsyncIteratorFind,
        AsyncIteratorFindWithResult,
        AsyncGeneratorAwaitReturnFulfilled,
        AsyncGeneratorAwaitReturnRejected,
        IteratorMap,
        IteratorFilter,
        IteratorTake,
        IteratorDrop,
        IteratorIndexed,
        IteratorFlatMap,
        CollatorCompare,
        DateTimeFormatFormat,
        NumberFormatFormat,
        OrdinaryHasInstance,
        ProxyCall,
        ProxyRevokerFunction,
        PromiseResolveFunction,
        PromiseRejectFunction,
        PromiseGetCapabilitiesExecutor,
        PromiseResolveThenableJob,
        PromiseReactionJob,
        PromiseAllResolveElement,
        PromiseAllSettledResolveElement,
        PromiseAllSettledRejectElement,
        PromiseAnyRejectElement,
        PromiseThenFinally,
        PromiseCatchFinally,
        PromiseValueThunk,
        PromiseThrower,
        ImportModuleDynamically,
        JavaPackageToPrimitive,
        RegExpMultiLine,
        RegExpLastMatch,
        RegExpLastParen,
        RegExpLeftContext,
        RegExpRightContext,
        RegExp$1,
        RegExp$2,
        RegExp$3,
        RegExp$4,
        RegExp$5,
        RegExp$6,
        RegExp$7,
        RegExp$8,
        RegExp$9,
        FunctionAsyncIterator,
        IsGraalRuntime,
        SetUnhandledPromiseRejectionHandler,
        AsyncModuleExecutionFulfilled,
        AsyncModuleExecutionRejected,
        TopLevelAwaitResolve,
        TopLevelAwaitReject,
        WebAssemblySourceInstantiation,
        FinishImportModuleDynamicallyReject,
        FinishImportModuleDynamicallyResolve,
        ExportGetter,
        OrdinaryWrappedFunctionCall,
        DecoratorContextAddInitializer,
    }

    @CompilationFinal(dimensions = 1) private final JSFunctionData[] builtinFunctionData;

    final JSFunctionData throwTypeErrorFunctionData;
    final JSFunctionData throwTypeErrorRestrictedPropertyFunctionData;
    final JSFunctionData protoGetterFunctionData;
    final JSFunctionData protoSetterFunctionData;

    private Map<Shape, JSShapeData> shapeDataMap;

    private final Assumption singleRealmAssumption;
    private final boolean isMultiContext;

    private final AtomicInteger realmInit = new AtomicInteger();
    private static final int REALM_UNINITIALIZED = 0;
    private static final int REALM_INITIALIZING = 1;
    private static final int REALM_INITIALIZED = 2;

    @CompilationFinal private AllocationReporter allocationReporter;

    private final JSLanguageOptions languageOptions;
    private final JSParserOptions parserOptions;

    private final StableContextOptionValue<Boolean> optionRegexpStaticResult;
    private final StableContextOptionValue<Boolean> optionV8CompatibilityMode;
    private final StableContextOptionValue<Boolean> optionDirectByteBuffer;
    private final StableContextOptionValue<Long> optionTimerResolution;

    private final Map<Builtin, JSFunctionData> builtinFunctionDataMap = new ConcurrentHashMap<>();
    private final Map<TruffleString, JSFunctionData> namedEmptyFunctionsDataMap = new ConcurrentHashMap<>();

    private final JSPrototypeData nullPrototypeData = new JSPrototypeData();
    private final JSPrototypeData inObjectPrototypeData = new JSPrototypeData();

    private final JSFunctionFactory functionFactory;
    private final JSFunctionFactory constructorFactory;
    private final JSFunctionFactory strictFunctionFactory;
    private final JSFunctionFactory strictConstructorFactory;

    private final JSFunctionFactory generatorFunctionFactory;
    private final JSFunctionFactory asyncFunctionFactory;
    private final JSFunctionFactory asyncGeneratorFunctionFactory;

    private final JSFunctionFactory boundFunctionFactory;
    private final JSFunctionFactory wrappedFunctionFactory;

    static final PrototypeSupplier functionPrototypeSupplier = JSRealm::getFunctionPrototype;
    static final PrototypeSupplier asyncFunctionPrototypeSupplier = JSRealm::getAsyncFunctionPrototype;
    static final PrototypeSupplier generatorFunctionPrototypeSupplier = JSRealm::getGeneratorFunctionPrototype;
    static final PrototypeSupplier asyncGeneratorFunctionPrototypeSupplier = JSRealm::getAsyncGeneratorFunctionPrototype;

    private final JSObjectFactory ordinaryObjectFactory;
    private final JSObjectFactory arrayFactory;
    private final JSObjectFactory iteratorFactory;
    private final JSObjectFactory asyncIteratorFactory;
    private final JSObjectFactory arrayIteratorFactory;
    private final JSObjectFactory wrapForIteratorFactory;
    private final JSObjectFactory wrapForAsyncIteratorFactory;
    private final JSObjectFactory lazyRegexArrayFactory;
    private final JSObjectFactory lazyRegexIndicesArrayFactory;
    private final JSObjectFactory booleanFactory;
    private final JSObjectFactory numberFactory;
    private final JSObjectFactory bigIntFactory;
    private final JSObjectFactory stringFactory;
    private final JSObjectFactory stringIteratorFactory;
    private final JSObjectFactory regExpFactory;
    private final JSObjectFactory dateFactory;
    private final JSObjectFactory nonStrictArgumentsFactory;
    private final JSObjectFactory strictArgumentsFactory;
    private final JSObjectFactory callSiteFactory;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] errorObjectFactories;

    private final JSObjectFactory symbolFactory;
    private final JSObjectFactory mapFactory;
    private final JSObjectFactory mapIteratorFactory;
    private final JSObjectFactory setFactory;
    private final JSObjectFactory setIteratorFactory;
    private final JSObjectFactory weakRefFactory;
    private final JSObjectFactory weakMapFactory;
    private final JSObjectFactory weakSetFactory;
    private final JSObjectFactory proxyFactory;
    private final JSObjectFactory uncheckedProxyHandlerFactory;
    private final JSObjectFactory promiseFactory;
    private final JSObjectFactory dataViewFactory;
    private final JSObjectFactory arrayBufferFactory;
    private final JSObjectFactory directArrayBufferFactory;
    private final JSObjectFactory sharedArrayBufferFactory;
    private final JSObjectFactory interopArrayBufferFactory;
    private final JSObjectFactory finalizationRegistryFactory;
    @CompilationFinal(dimensions = 1) private final JSObjectFactory[] typedArrayFactories;

    private final JSObjectFactory enumerateIteratorFactory;
    private final JSObjectFactory forInIteratorFactory;
    private final JSObjectFactory generatorObjectFactory;
    private final JSObjectFactory asyncGeneratorObjectFactory;
    private final JSObjectFactory asyncFromSyncIteratorFactory;

    private final JSObjectFactory collatorFactory;
    private final JSObjectFactory numberFormatFactory;
    private final JSObjectFactory pluralRulesFactory;
    private final JSObjectFactory dateTimeFormatFactory;
    private final JSObjectFactory listFormatFactory;
    private final JSObjectFactory relativeTimeFormatFactory;
    private final JSObjectFactory segmenterFactory;
    private final JSObjectFactory segmentsFactory;
    private final JSObjectFactory segmentIteratorFactory;
    private final JSObjectFactory displayNamesFactory;
    private final JSObjectFactory localeFactory;

    private final JSObjectFactory javaImporterFactory;
    private final JSObjectFactory javaPackageFactory;
    private final JSObjectFactory jsAdapterFactory;
    private final JSObjectFactory dictionaryObjectFactory;

    private final JSObjectFactory temporalPlainTimeFactory;
    private final JSObjectFactory temporalPlainDateFactory;
    private final JSObjectFactory temporalPlainDateTimeFactory;
    private final JSObjectFactory temporalDurationFactory;
    private final JSObjectFactory temporalCalendarFactory;
    private final JSObjectFactory temporalPlainYearMonthFactory;
    private final JSObjectFactory temporalPlainMonthDayFactory;
    private final JSObjectFactory temporalInstantFactory;
    private final JSObjectFactory temporalTimeZoneFactory;
    private final JSObjectFactory temporalZonedDateTimeFactory;

    private final JSObjectFactory globalObjectFactory;

    private final JSObjectFactory webAssemblyModuleFactory;
    private final JSObjectFactory webAssemblyInstanceFactory;
    private final JSObjectFactory webAssemblyMemoryFactory;
    private final JSObjectFactory webAssemblyTableFactory;
    private final JSObjectFactory webAssemblyGlobalFactory;

    private final JSObjectFactory shadowRealmFactory;
    private final JSObjectFactory asyncContextSnapshotFactory;
    private final JSObjectFactory asyncContextVariableFactory;

    private final int factoryCount;

    @CompilationFinal private Locale locale;

    private final Set<TruffleString> supportedImportAssertions;

    private static final TruffleString TYPE_IMPORT_ASSERTION = Strings.constant("type");

    /**
     * A shared root node that acts as a parent providing a lock to nodes that are not rooted in a
     * tree but in shared object factories for the purpose of adding properties to newly allocated
     * objects.
     */
    private final SharedRootNode sharedRootNode;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            notConstructibleCallTargetVarHandle = lookup.findVarHandle(JSContext.class, "notConstructibleCallTargetCache", CallTarget.class);
            generatorNotConstructibleCallTargetVarHandle = lookup.findVarHandle(JSContext.class, "generatorNotConstructibleCallTargetCache", CallTarget.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    protected JSContext(Evaluator evaluator, JavaScriptLanguage lang, JSLanguageOptions languageOptions, TruffleLanguage.Env env) {
        this.language = lang;
        this.languageOptions = languageOptions;
        this.parserOptions = JSParserOptions.fromLanguageOptions(languageOptions);

        if (env != null) { // env could still be null
            setAllocationReporter(env);
        }

        this.optionRegexpStaticResult = new StableContextOptionValue<>(JSContextOptions::isRegexpStaticResult, JSContextOptions.REGEXP_STATIC_RESULT, JSContextOptions.REGEXP_STATIC_RESULT_NAME);
        this.optionV8CompatibilityMode = new StableContextOptionValue<>(JSContextOptions::isV8CompatibilityMode, JSContextOptions.V8_COMPATIBILITY_MODE, JSContextOptions.V8_COMPATIBILITY_MODE_NAME);
        this.optionDirectByteBuffer = new StableContextOptionValue<>(JSContextOptions::isDirectByteBuffer, JSContextOptions.DIRECT_BYTE_BUFFER, JSContextOptions.DIRECT_BYTE_BUFFER_NAME);
        this.optionTimerResolution = new StableContextOptionValue<>(JSContextOptions::getTimerResolution, JSContextOptions.TIMER_RESOLUTION, JSContextOptions.TIMER_RESOLUTION_NAME);

        this.sharedRootNode = new SharedRootNode();

        this.emptyShape = createEmptyShape();
        this.emptyShapePrototypeInObject = createEmptyShapePrototypeInObject();
        this.promiseShapePrototypeInObject = createPromiseShapePrototypeInObject();
        this.globalScopeShape = createGlobalScopeShape();

        this.noSuchPropertyUnusedAssumption = Truffle.getRuntime().createAssumption("noSuchPropertyUnusedAssumption");
        this.noSuchMethodUnusedAssumption = Truffle.getRuntime().createAssumption("noSuchMethodUnusedAssumption");
        this.arrayPrototypeNoElementsAssumption = Truffle.getRuntime().createAssumption("arrayPrototypeNoElementsAssumption");
        this.typedArrayNotDetachedAssumption = Truffle.getRuntime().createAssumption("typedArrayNotDetachedAssumption");
        this.fastArrayAssumption = Truffle.getRuntime().createAssumption("fastArrayAssumption");
        this.fastArgumentsObjectAssumption = Truffle.getRuntime().createAssumption("fastArgumentsObjectAssumption");
        this.regExpStaticResultUnusedAssumption = Truffle.getRuntime().createAssumption("regExpStaticResultUnusedAssumption");
        this.globalObjectPristineAssumption = Truffle.getRuntime().createAssumption("globalObjectPristineAssumption");

        this.evaluator = evaluator;
        this.nodeFactory = evaluator.getDefaultNodeFactory();

        this.moduleNamespaceFactory = JSObjectFactory.createBound(this, Null.instance, JSModuleNamespace.makeInitialShape(this));

        this.prepareStackTraceCallbackNotUsedAssumption = Truffle.getRuntime().createAssumption("prepareStackTraceCallbackNotUsedAssumption");
        this.promiseHookNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseHookNotUsedAssumption");
        this.promiseRejectionTrackerNotUsedAssumption = Truffle.getRuntime().createAssumption("promiseRejectionTrackerNotUsedAssumption");
        this.importMetaInitializerNotUsedAssumption = Truffle.getRuntime().createAssumption("importMetaInitializerNotUsedAssumption");
        this.importModuleDynamicallyCallbackNotUsedAssumption = Truffle.getRuntime().createAssumption("importModuleDynamicallyCallbackNotUsedAssumption");

        this.emptyFunctionCallTarget = createEmptyFunctionCallTarget(lang);
        this.symbolSpeciesThisGetterFunctionData = JSFunctionData.createCallOnly(this, createReadFrameThisCallTarget(lang), 0, GET_SYMBOL_SPECIES_NAME);
        this.symbolIteratorThisGetterFunctionData = JSFunctionData.createCallOnly(this, createReadFrameThisCallTarget(lang), 0, SYMBOL_ITERATOR_NAME);

        this.builtinFunctionData = new JSFunctionData[BuiltinFunctionKey.values().length];

        this.timeProfiler = languageOptions.profileTime() ? new TimeProfiler() : null;

        this.singleRealmAssumption = Truffle.getRuntime().createAssumption("single realm");

        this.throwTypeErrorFunctionData = throwTypeErrorFunction(false);
        this.throwTypeErrorRestrictedPropertyFunctionData = throwTypeErrorFunction(true);
        boolean annexB = isOptionAnnexB();
        this.protoGetterFunctionData = annexB ? protoGetterFunction() : null;
        this.protoSetterFunctionData = annexB ? protoSetterFunction() : null;

        this.isMultiContext = lang.isMultiContext();

        // shapes and factories
        PrototypeSupplier objectPrototypeSupplier = JSOrdinary.INSTANCE;
        CompilableBiFunction<JSContext, JSDynamicObject, Shape> ordinaryObjectShapeSupplier = JSOrdinary.SHAPE_SUPPLIER;
        JSObjectFactory.IntrinsicBuilder builder = new JSObjectFactory.IntrinsicBuilder(this);

        this.functionFactory = builder.function(functionPrototypeSupplier, false, false, false, false, false);
        this.constructorFactory = builder.function(functionPrototypeSupplier, false, true, false, false, false);
        this.strictFunctionFactory = builder.function(functionPrototypeSupplier, true, false, false, false, false);
        this.strictConstructorFactory = builder.function(functionPrototypeSupplier, true, true, false, false, false);

        this.asyncFunctionFactory = builder.function(asyncFunctionPrototypeSupplier, true, false, false, false, true);
        this.generatorFunctionFactory = builder.function(generatorFunctionPrototypeSupplier, true, false, true, false, false);
        this.asyncGeneratorFunctionFactory = builder.function(asyncGeneratorFunctionPrototypeSupplier, true, false, true, false, true);

        this.boundFunctionFactory = builder.function(functionPrototypeSupplier, true, false, false, true, false);
        this.wrappedFunctionFactory = builder.function(functionPrototypeSupplier, true, false, false, true, false);

        this.ordinaryObjectFactory = builder.create(JSOrdinary.INSTANCE);
        this.arrayFactory = builder.create(JSArray.INSTANCE);
        this.iteratorFactory = builder.create(JSIterator.INSTANCE);
        this.asyncIteratorFactory = builder.create(JSAsyncIterator.INSTANCE);
        this.arrayIteratorFactory = builder.create(JSArrayIterator.INSTANCE);
        this.wrapForIteratorFactory = builder.create(JSWrapForValidIterator.INSTANCE);
        this.wrapForAsyncIteratorFactory = builder.create(JSWrapForValidAsyncIterator.INSTANCE);
        this.lazyRegexArrayFactory = builder.create(JSArray.INSTANCE);
        this.lazyRegexIndicesArrayFactory = builder.create(JSArray.INSTANCE);
        this.booleanFactory = builder.create(JSBoolean.INSTANCE);
        this.numberFactory = builder.create(JSNumber.INSTANCE);
        this.bigIntFactory = builder.create(JSBigInt.INSTANCE);
        this.stringFactory = builder.create(JSString.INSTANCE);
        this.stringIteratorFactory = builder.create(JSStringIterator.INSTANCE);
        this.regExpFactory = builder.create(JSRegExp.INSTANCE);
        this.dateFactory = builder.create(JSDate.INSTANCE);

        this.symbolFactory = builder.create(JSSymbol.INSTANCE);
        this.mapFactory = builder.create(JSMap.INSTANCE);
        this.mapIteratorFactory = builder.create(JSMapIterator.INSTANCE);
        this.setFactory = builder.create(JSSet.INSTANCE);
        this.setIteratorFactory = builder.create(JSSetIterator.INSTANCE);
        this.weakRefFactory = builder.create(JSWeakRef.INSTANCE);
        this.weakMapFactory = builder.create(JSWeakMap.INSTANCE);
        this.weakSetFactory = builder.create(JSWeakSet.INSTANCE);
        this.proxyFactory = builder.create(JSProxy.INSTANCE);
        this.uncheckedProxyHandlerFactory = builder.create(JSUncheckedProxyHandler.INSTANCE);
        this.promiseFactory = builder.create(JSPromise.INSTANCE);
        this.dataViewFactory = builder.create(JSDataView.INSTANCE);
        this.arrayBufferFactory = builder.create(JSArrayBuffer.HEAP_INSTANCE);
        this.directArrayBufferFactory = builder.create(JSArrayBuffer.DIRECT_INSTANCE);
        this.sharedArrayBufferFactory = isOptionSharedArrayBuffer() ? builder.create(JSSharedArrayBuffer.INSTANCE) : null;
        this.interopArrayBufferFactory = builder.create(JSArrayBuffer.INTEROP_INSTANCE);
        this.finalizationRegistryFactory = builder.create(JSFinalizationRegistry.INSTANCE);
        this.typedArrayFactories = new JSObjectFactory[TypedArray.factories(this).length];
        for (TypedArrayFactory factory : TypedArray.factories(this)) {
            typedArrayFactories[factory.getFactoryIndex()] = builder.create(factory, (c, p) -> JSArrayBufferView.makeInitialArrayBufferViewShape(c, p));
        }

        this.errorObjectFactories = new JSObjectFactory[JSErrorType.errorTypes().length];
        for (JSErrorType type : JSErrorType.errorTypes()) {
            errorObjectFactories[type.ordinal()] = builder.create(type, JSError.INSTANCE::makeInitialShape);
        }

        this.callSiteFactory = builder.create(JSRealm::getCallSitePrototype, JSError::makeInitialCallSiteShape);
        this.nonStrictArgumentsFactory = builder.create(objectPrototypeSupplier, JSArgumentsArray.INSTANCE);
        this.strictArgumentsFactory = builder.create(objectPrototypeSupplier, JSArgumentsArray.INSTANCE);
        this.enumerateIteratorFactory = builder.create(JSRealm::getEnumerateIteratorPrototype, JSFunction::makeInitialEnumerateIteratorShape);
        this.forInIteratorFactory = builder.create(JSForInIterator.INSTANCE);

        this.generatorObjectFactory = builder.create(JSRealm::getGeneratorObjectPrototype, ordinaryObjectShapeSupplier);
        this.asyncGeneratorObjectFactory = builder.create(JSRealm::getAsyncGeneratorObjectPrototype, ordinaryObjectShapeSupplier);
        this.asyncFromSyncIteratorFactory = builder.create(JSRealm::getAsyncFromSyncIteratorPrototype, ordinaryObjectShapeSupplier);

        this.collatorFactory = builder.create(JSCollator.INSTANCE);
        this.numberFormatFactory = builder.create(JSNumberFormat.INSTANCE);
        this.dateTimeFormatFactory = builder.create(JSDateTimeFormat.INSTANCE);
        this.pluralRulesFactory = builder.create(JSPluralRules.INSTANCE);
        this.listFormatFactory = builder.create(JSListFormat.INSTANCE);
        this.relativeTimeFormatFactory = builder.create(JSRelativeTimeFormat.INSTANCE);
        this.segmenterFactory = builder.create(JSSegmenter.INSTANCE);
        this.segmentsFactory = builder.create(JSRealm::getSegmentsPrototype, JSSegmenter::makeInitialSegmentsShape);
        this.segmentIteratorFactory = builder.create(JSRealm::getSegmentIteratorPrototype, JSSegmenter::makeInitialSegmentIteratorShape);
        this.displayNamesFactory = builder.create(JSDisplayNames.INSTANCE);
        this.localeFactory = builder.create(JSLocale.INSTANCE);

        this.javaPackageFactory = builder.create(objectPrototypeSupplier, JavaPackage.INSTANCE::makeInitialShape);
        boolean nashornCompat = isOptionNashornCompatibilityMode();
        this.jsAdapterFactory = nashornCompat ? builder.create(JSAdapter.INSTANCE) : null;
        this.javaImporterFactory = nashornCompat ? builder.create(JavaImporter.instance()) : null;

        this.temporalPlainTimeFactory = builder.create(JSTemporalPlainTime.INSTANCE);
        this.temporalPlainDateFactory = builder.create(JSTemporalPlainDate.INSTANCE);
        this.temporalPlainDateTimeFactory = builder.create(JSTemporalPlainDateTime.INSTANCE);
        this.temporalDurationFactory = builder.create(JSTemporalDuration.INSTANCE);
        this.temporalCalendarFactory = builder.create(JSTemporalCalendar.INSTANCE);
        this.temporalPlainYearMonthFactory = builder.create(JSTemporalPlainYearMonth.INSTANCE);
        this.temporalPlainMonthDayFactory = builder.create(JSTemporalPlainMonthDay.INSTANCE);
        this.temporalInstantFactory = builder.create(JSTemporalInstant.INSTANCE);
        this.temporalTimeZoneFactory = builder.create(JSTemporalTimeZone.INSTANCE);
        this.temporalZonedDateTimeFactory = builder.create(JSTemporalZonedDateTime.INSTANCE);

        this.dictionaryObjectFactory = JSConfig.DictionaryObject ? builder.create(objectPrototypeSupplier, JSDictionary::makeDictionaryShape) : null;

        this.globalObjectFactory = builder.create(objectPrototypeSupplier, JSGlobal::makeGlobalObjectShape);

        this.webAssemblyModuleFactory = builder.create(JSWebAssemblyModule.INSTANCE);
        this.webAssemblyInstanceFactory = builder.create(JSWebAssemblyInstance.INSTANCE);
        this.webAssemblyMemoryFactory = builder.create(JSWebAssemblyMemory.INSTANCE);
        this.webAssemblyTableFactory = builder.create(JSWebAssemblyTable.INSTANCE);
        this.webAssemblyGlobalFactory = builder.create(JSWebAssemblyGlobal.INSTANCE);

        this.shadowRealmFactory = builder.create(JSShadowRealm.INSTANCE);
        this.asyncContextSnapshotFactory = builder.create(JSAsyncContextSnapshot.INSTANCE);
        this.asyncContextVariableFactory = builder.create(JSAsyncContextVariable.INSTANCE);

        this.factoryCount = builder.finish();

        this.regExpGroupsEmptyShape = JSRegExp.makeInitialGroupsObjectShape(this);

        this.regexOptions = createRegexOptions(languageOptions);
        this.regexValidateOptions = regexOptions.isEmpty() ? REGEX_OPTION_VALIDATE : REGEX_OPTION_VALIDATE + "," + regexOptions;

        this.supportedImportAssertions = languageOptions.importAssertions() ? Set.of(TYPE_IMPORT_ASSERTION) : Set.of();

        if (languageOptions.unhandledRejectionsMode() != JSContextOptions.UnhandledRejectionsTrackingMode.NONE) {
            setPromiseRejectionTracker(new BuiltinPromiseRejectionTracker(this, languageOptions.unhandledRejectionsMode()));
        }
    }

    public final Evaluator getEvaluator() {
        return evaluator;
    }

    public Object getNodeFactory() {
        return nodeFactory;
    }

    public final JSParserOptions getParserOptions() {
        return parserOptions;
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

    public final Assumption getRegExpStaticResultUnusedAssumption() {
        return regExpStaticResultUnusedAssumption;
    }

    public final Assumption getGlobalObjectPristineAssumption() {
        return globalObjectPristineAssumption;
    }

    public static JSContext createContext(Evaluator evaluator, JavaScriptLanguage language, TruffleLanguage.Env env) {
        JSContextOptions contextOptions = JSContextOptions.fromOptionValues(env.getSandboxPolicy(), env.getOptions());
        JSLanguageOptions languageOptions = JSLanguageOptions.fromContextOptions(contextOptions);
        JSContext context = new JSContext(evaluator, language, languageOptions, env);
        if (!env.isPreInitialization()) {
            context.updateStableOptions(contextOptions, StableContextOptionValue.UpdateKind.INITIALIZE);
        }
        return context;
    }

    public JSRealm createRealm(TruffleLanguage.Env env) {
        return createRealm(env, null);
    }

    protected JSRealm createRealm(TruffleLanguage.Env env, JSRealm parentRealm) {
        boolean isTop = (parentRealm == null);
        realmInit.compareAndSet(REALM_UNINITIALIZED, REALM_INITIALIZING);

        if (!isTop) {
            singleRealmAssumption.invalidate("creating another realm");
        }
        JSRealm newRealm = new JSRealm(this, env, parentRealm);
        newRealm.setupGlobals();

        if (isTop) {
            if (languageOptions.test262Mode() || languageOptions.testV8Mode()) {
                newRealm.setAgent(new DebugJSAgent(getPromiseRejectionTracker(), languageOptions.agentCanBlock()));
            } else {
                newRealm.setAgent(new MainJSAgent(getPromiseRejectionTracker()));
            }
            if (languageOptions.v8RealmBuiltin()) {
                newRealm.initRealmList();
                newRealm.addToRealmList(newRealm);
            }
        }

        realmInit.set(REALM_INITIALIZED);
        return newRealm;
    }

    public final Shape createEmptyShape() {
        return makeEmptyShapeWithNullPrototype(JSOrdinary.INSTANCE);
    }

    private Shape createEmptyShapePrototypeInObject() {
        return makeEmptyShapeWithPrototypeInObject(JSOrdinary.INSTANCE);
    }

    private Shape createPromiseShapePrototypeInObject() {
        return makeEmptyShapeWithPrototypeInObject(JSPromise.INSTANCE);
    }

    public final Shape makeEmptyShapeWithNullPrototype(JSClass jsclass) {
        Shape protoChildTree = nullPrototypeData.getProtoChildTree(jsclass);
        if (protoChildTree != null) {
            return protoChildTree;
        }
        return nullPrototypeData.getOrAddProtoChildTree(jsclass, JSShape.makeEmptyRoot(jsclass, this));
    }

    public final Shape makeEmptyShapeWithPrototypeInObject(JSClass jsclass) {
        Shape protoChildTree = inObjectPrototypeData.getProtoChildTree(jsclass);
        if (protoChildTree != null) {
            return protoChildTree;
        }
        return inObjectPrototypeData.getOrAddProtoChildTree(jsclass, JSShape.makeEmptyRootWithInstanceProto(this, jsclass));
    }

    private Shape createGlobalScopeShape() {
        return JSShape.makeEmptyRoot(JSGlobal.INSTANCE, this);
    }

    public final Map<TruffleString, Symbol> getSymbolRegistry() {
        return symbolRegistry;
    }

    public final Map<TruffleString, Symbol> getPrivateSymbolRegistry() {
        return privateSymbolRegistry;
    }

    /**
     * ES abstract operation HostEnqueuePromiseJob.
     */
    public final void enqueuePromiseJob(JSRealm realm, JSFunctionObject job) {
        invalidatePromiseQueueNotUsedAssumption();
        JSAgent agent = realm.getAgent();
        agent.enqueuePromiseJob(job);
    }

    public final void signalAsyncWaiterRecordUsage() {
        invalidatePromiseQueueNotUsedAssumption();
    }

    private void invalidatePromiseQueueNotUsedAssumption() {
        Assumption promiseJobsQueueEmptyAssumption = language.getPromiseJobsQueueEmptyAssumption();
        if (promiseJobsQueueEmptyAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseJobsQueueEmptyAssumption.invalidate();
        }
    }

    public final void processAllPendingPromiseJobs(JSRealm realm) {
        if (!language.getPromiseJobsQueueEmptyAssumption().isValid()) {
            realm.getAgent().processAllPromises(false);
        }
    }

    public boolean addWeakRefTargetToSet(Object target) {
        invalidatePromiseQueueNotUsedAssumption();
        return getJSAgent().addWeakRefTargetToSet(target);
    }

    public void registerFinalizationRegistry(JSFinalizationRegistryObject finalizationRegistry) {
        invalidatePromiseQueueNotUsedAssumption();
        getJSAgent().registerFinalizationRegistry(finalizationRegistry);
    }

    public TimeProfiler getTimeProfiler() {
        return timeProfiler;
    }

    /**
     * Get the current Realm using {@link ContextReference}.
     */
    private JSRealm getRealm() {
        assert realmInit.get() == REALM_INITIALIZED : "getRealm() while initializing Realm";
        JSRealm currentRealm = JSRealm.get(null);
        assert currentRealm != null;
        return currentRealm;
    }

    public final Shape getEmptyShapeNullPrototype() {
        return emptyShape;
    }

    public final Shape getEmptyShapePrototypeInObject() {
        return emptyShapePrototypeInObject;
    }

    public final Shape getPromiseShapePrototypeInObject() {
        return promiseShapePrototypeInObject;
    }

    public final Shape getGlobalScopeShape() {
        return globalScopeShape;
    }

    public final JSObjectFactory getOrdinaryObjectFactory() {
        return ordinaryObjectFactory;
    }

    public final JSObjectFactory getArrayFactory() {
        return arrayFactory;
    }

    public final JSObjectFactory getIteratorFactory() {
        return iteratorFactory;
    }

    public final JSObjectFactory getAsyncIteratorFactory() {
        return asyncIteratorFactory;
    }

    public final JSObjectFactory getArrayIteratorFactory() {
        return arrayIteratorFactory;
    }

    public final JSObjectFactory getWrapForIteratorFactory() {
        return wrapForIteratorFactory;
    }

    public final JSObjectFactory getWrapForAsyncIteratorFactory() {
        return wrapForAsyncIteratorFactory;
    }

    public final JSObjectFactory getLazyRegexArrayFactory() {
        return lazyRegexArrayFactory;
    }

    public final JSObjectFactory getLazyRegexIndicesArrayFactory() {
        return lazyRegexIndicesArrayFactory;
    }

    public final JSObjectFactory getStringFactory() {
        return stringFactory;
    }

    public final JSObjectFactory getStringIteratorFactory() {
        return stringIteratorFactory;
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

    public final JSObjectFactory getForInIteratorFactory() {
        return forInIteratorFactory;
    }

    public final JSObjectFactory getMapFactory() {
        return mapFactory;
    }

    public final JSObjectFactory getMapIteratorFactory() {
        return mapIteratorFactory;
    }

    public final JSObjectFactory getFinalizationRegistryFactory() {
        return finalizationRegistryFactory;
    }

    public final JSObjectFactory getWeakRefFactory() {
        return weakRefFactory;
    }

    public final JSObjectFactory getWeakMapFactory() {
        return weakMapFactory;
    }

    public final JSObjectFactory getSetFactory() {
        return setFactory;
    }

    public final JSObjectFactory getSetIteratorFactory() {
        return setIteratorFactory;
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

    public final JSObjectFactory getUncheckedProxyHandlerFactory() {
        return uncheckedProxyHandlerFactory;
    }

    public final JSObjectFactory getSharedArrayBufferFactory() {
        assert isOptionSharedArrayBuffer();
        return sharedArrayBufferFactory;
    }

    public JSObjectFactory getInteropArrayBufferFactory() {
        return interopArrayBufferFactory;
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

    public final JSObjectFactory getErrorFactory(JSErrorType type) {
        return errorObjectFactories[type.ordinal()];
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

    public final JSObjectFactory getListFormatFactory() {
        return listFormatFactory;
    }

    public final JSObjectFactory getRelativeTimeFormatFactory() {
        return relativeTimeFormatFactory;
    }

    public final JSObjectFactory getSegmenterFactory() {
        return segmenterFactory;
    }

    public final JSObjectFactory getSegmentsFactory() {
        return segmentsFactory;
    }

    public final JSObjectFactory getSegmentIteratorFactory() {
        return segmentIteratorFactory;
    }

    public final JSObjectFactory getDisplayNamesFactory() {
        return displayNamesFactory;
    }

    public final JSObjectFactory getLocaleFactory() {
        return localeFactory;
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

    public final JSObjectFactory getTemporalPlainTimeFactory() {
        return temporalPlainTimeFactory;
    }

    public final JSObjectFactory getTemporalPlainDateFactory() {
        return temporalPlainDateFactory;
    }

    public final JSObjectFactory getTemporalPlainDateTimeFactory() {
        return temporalPlainDateTimeFactory;
    }

    public final JSObjectFactory getTemporalDurationFactory() {
        return temporalDurationFactory;
    }

    public final JSObjectFactory getTemporalCalendarFactory() {
        return temporalCalendarFactory;
    }

    public JSObjectFactory getTemporalPlainYearMonthFactory() {
        return temporalPlainYearMonthFactory;
    }

    public JSObjectFactory getTemporalPlainMonthDayFactory() {
        return temporalPlainMonthDayFactory;
    }

    public JSObjectFactory getTemporalInstantFactory() {
        return temporalInstantFactory;
    }

    public JSObjectFactory getTemporalZonedDateTimeFactory() {
        return temporalZonedDateTimeFactory;
    }

    public JSObjectFactory getTemporalTimeZoneFactory() {
        return temporalTimeZoneFactory;
    }

    public JSObjectFactory getDictionaryObjectFactory() {
        return dictionaryObjectFactory;
    }

    public JSObjectFactory getGlobalObjectFactory() {
        return globalObjectFactory;
    }

    public JSObjectFactory getWebAssemblyModuleFactory() {
        return webAssemblyModuleFactory;
    }

    public JSObjectFactory getWebAssemblyInstanceFactory() {
        return webAssemblyInstanceFactory;
    }

    public JSObjectFactory getWebAssemblyMemoryFactory() {
        return webAssemblyMemoryFactory;
    }

    public JSObjectFactory getWebAssemblyTableFactory() {
        return webAssemblyTableFactory;
    }

    public JSObjectFactory getWebAssemblyGlobalFactory() {
        return webAssemblyGlobalFactory;
    }

    public final JSObjectFactory getShadowRealmFactory() {
        return shadowRealmFactory;
    }

    public final JSObjectFactory getAsyncContextSnapshotFactory() {
        return asyncContextSnapshotFactory;
    }

    public final JSObjectFactory getAsyncContextVariableFactory() {
        return asyncContextVariableFactory;
    }

    private static final String REGEX_OPTION_REGRESSION_TEST_MODE = "RegressionTestMode";
    private static final String REGEX_OPTION_DUMP_AUTOMATA = "DumpAutomata";
    private static final String REGEX_OPTION_STEP_EXECUTION = "StepExecution";
    private static final String REGEX_OPTION_ALWAYS_EAGER = "AlwaysEager";
    private static final String REGEX_OPTION_VALIDATE = "Validate=true";

    private static String createRegexOptions(JSLanguageOptions jsOptions) {
        StringBuilder regexOptions = new StringBuilder();
        if (jsOptions.regexRegressionTestMode()) {
            regexOptions.append(REGEX_OPTION_REGRESSION_TEST_MODE).append("=true,");
        }
        if (jsOptions.regexDumpAutomata()) {
            regexOptions.append(REGEX_OPTION_DUMP_AUTOMATA).append("=true,");
        }
        if (jsOptions.regexStepExecution()) {
            regexOptions.append(REGEX_OPTION_STEP_EXECUTION).append("=true,");
        }
        if (jsOptions.regexAlwaysEager()) {
            regexOptions.append(REGEX_OPTION_ALWAYS_EAGER).append("=true,");
        }
        return regexOptions.toString();
    }

    public String getRegexOptions() {
        return regexOptions;
    }

    public String getRegexValidateOptions() {
        return regexValidateOptions;
    }

    public Object getTRegexEmptyResult() {
        if (tRegexEmptyResult == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            tRegexEmptyResult = TRegexUtil.InvokeExecMethodNode.getUncached().execute(null, RegexCompilerInterface.compile("[]", "", this, JSRealm.get(null)), "", 0);
            assert !TRegexUtil.TRegexResultAccessor.isMatch(tRegexEmptyResult, null, TRegexUtil.InteropReadBooleanMemberNode.getUncached());
        }
        return tRegexEmptyResult;
    }

    public Shape getRegExpGroupsEmptyShape() {
        return regExpGroupsEmptyShape;
    }

    public Map<Shape, JSShapeData> getShapeDataMap() {
        assert Thread.holdsLock(this);
        Map<Shape, JSShapeData> map = shapeDataMap;
        if (map == null) {
            map = createShapeDataMap();
        }
        return map;
    }

    private Map<Shape, JSShapeData> createShapeDataMap() {
        CompilerAsserts.neverPartOfCompilation();
        Map<Shape, JSShapeData> map = new WeakHashMap<>();
        shapeDataMap = map;
        return map;
    }

    public JavaScriptLanguage getLanguage() {
        return language;
    }

    public CallTarget getEmptyFunctionCallTarget() {
        return emptyFunctionCallTarget;
    }

    public JSFunctionData getNamedEmptyFunctionData(TruffleString name) {
        return namedEmptyFunctionsDataMap.computeIfAbsent(name, k -> JSFunctionData.createCallOnly(this, emptyFunctionCallTarget, 0, name));
    }

    /** CallTarget for an empty function that returns undefined. */
    private static CallTarget createEmptyFunctionCallTarget(JavaScriptLanguage lang) {
        return new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                return Undefined.instance;
            }
        }.getCallTarget();
    }

    public JSFunctionData getSymbolIteratorThisGetterFunctionData() {
        return symbolIteratorThisGetterFunctionData;
    }

    public JSFunctionData getSymbolSpeciesThisGetterFunctionData() {
        return symbolSpeciesThisGetterFunctionData;
    }

    private static CallTarget createReadFrameThisCallTarget(JavaScriptLanguage lang) {
        return new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                return JSFrameUtil.getThisObj(frame);
            }
        }.getCallTarget();
    }

    @TruffleBoundary
    public CallTarget getNotConstructibleCallTarget() {
        CallTarget result = notConstructibleCallTargetCache;
        if (result != null) {
            return result;
        } else {
            result = createNotConstructibleCallTarget(getLanguage(), false, this);
            if (!notConstructibleCallTargetVarHandle.compareAndSet(this, (CallTarget) null, result)) {
                result = notConstructibleCallTargetCache;
            }
            return Objects.requireNonNull(result);
        }
    }

    @TruffleBoundary
    public CallTarget getGeneratorNotConstructibleCallTarget() {
        CallTarget result = generatorNotConstructibleCallTargetCache;
        if (result != null) {
            return result;
        } else {
            result = createNotConstructibleCallTarget(getLanguage(), true, this);
            if (!generatorNotConstructibleCallTargetVarHandle.compareAndSet(this, (CallTarget) null, result)) {
                result = generatorNotConstructibleCallTargetCache;
            }
            return Objects.requireNonNull(result);
        }
    }

    private static RootCallTarget createNotConstructibleCallTarget(JavaScriptLanguage lang, boolean generator, JSContext context) {
        return new JavaScriptRootNode(lang, null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                if (generator) {
                    throw Errors.createTypeError("cannot construct a generator");
                } else {
                    throw Errors.createTypeErrorNotAConstructor(JSArguments.getFunctionObject(frame.getArguments()), context);
                }
            }
        }.getCallTarget();
    }

    public JSFunctionData getBoundFunctionData(boolean constructor, boolean async) {
        if (async) {
            // Mark bound async functions for MLE mode; see ExportValueNode.
            if (constructor) {
                return getOrCreateBuiltinFunctionData(BuiltinFunctionKey.BoundConstructorAsync, c -> makeBoundFunctionData(c, true, true));
            } else {
                return getOrCreateBuiltinFunctionData(BuiltinFunctionKey.BoundFunctionAsync, c -> makeBoundFunctionData(c, false, true));
            }
        } else {
            if (constructor) {
                return getOrCreateBuiltinFunctionData(BuiltinFunctionKey.BoundConstructor, c -> makeBoundFunctionData(c, true, false));
            } else {
                return getOrCreateBuiltinFunctionData(BuiltinFunctionKey.BoundFunction, c -> makeBoundFunctionData(c, false, false));
            }
        }
    }

    private static JSFunctionData makeBoundFunctionData(JSContext context, boolean constructor, boolean async) {
        CallTarget callTarget;
        CallTarget constructTarget;
        CallTarget constructNewTarget;
        // The call targets are the same for all bound function data instances, so reuse them.
        if (constructor || async) {
            JSFunctionData template = context.getBoundFunctionData(false, false);
            callTarget = template.getCallTarget();
            constructTarget = template.getConstructTarget();
            constructNewTarget = template.getConstructNewTarget();
        } else {
            callTarget = JSFunction.createBoundRootNode(context, false, false).getCallTarget();
            constructTarget = JSFunction.createBoundRootNode(context, true, false).getCallTarget();
            constructNewTarget = JSFunction.createBoundRootNode(context, true, true).getCallTarget();
        }
        return JSFunctionData.create(context,
                        callTarget, constructTarget, constructNewTarget,
                        0, Strings.BOUND, constructor, false, true, false, false, false, async, false, true, false, true);
    }

    private JSAgent getJSAgent() {
        return getRealm().getAgent();
    }

    public int getEcmaScriptVersion() {
        return languageOptions.ecmaScriptVersion();
    }

    @Idempotent
    public int getPropertyCacheLimit() {
        return languageOptions.propertyCacheLimit();
    }

    public int getFunctionCacheLimit() {
        return languageOptions.functionCacheLimit();
    }

    void setAllocationReporter(TruffleLanguage.Env env) {
        CompilerAsserts.neverPartOfCompilation();
        this.allocationReporter = env.lookup(AllocationReporter.class);
    }

    public final AllocationReporter getAllocationReporter() {
        assert realmInit.get() == REALM_INITIALIZED : "getAllocationReporter() during Realm initialization";
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
        return languageOptions.annexB();
    }

    public boolean isOptionIntl402() {
        return languageOptions.intl402();
    }

    public boolean isOptionRegexpMatchIndices() {
        return languageOptions.regexpMatchIndices();
    }

    public boolean isOptionRegexpUnicodeSets() {
        return languageOptions.regexpUnicodeSets();
    }

    public boolean isOptionRegexpStaticResult() {
        return optionRegexpStaticResult.get();
    }

    public boolean isOptionSharedArrayBuffer() {
        return languageOptions.sharedArrayBuffer();
    }

    public boolean isOptionTemporal() {
        return languageOptions.temporal();
    }

    public boolean isOptionV8CompatibilityMode() {
        return optionV8CompatibilityMode.get();
    }

    @Idempotent
    public boolean isOptionNashornCompatibilityMode() {
        return languageOptions.nashornCompatibilityMode();
    }

    public boolean isOptionMleBuiltin() {
        return languageOptions.isMLEMode();
    }

    public boolean isOptionDirectByteBuffer() {
        return optionDirectByteBuffer.get();
    }

    public boolean isOptionParseOnly() {
        return languageOptions.parseOnly();
    }

    public boolean isOptionDisableWith() {
        return languageOptions.disableWith();
    }

    public boolean isOptionAsyncStackTraces() {
        return languageOptions.asyncStackTraces();
    }

    public boolean isOptionForeignObjectPrototype() {
        return languageOptions.hasForeignObjectPrototype();
    }

    public long getTimerResolution() {
        return optionTimerResolution.get();
    }

    public long getFunctionArgumentsLimit() {
        return languageOptions.functionArgumentsLimit();
    }

    public int getStringLengthLimit() {
        return languageOptions.stringLengthLimit();
    }

    public boolean usePromiseResolve() {
        return languageOptions.awaitOptimization();
    }

    public final void setPrepareStackTraceCallback(PrepareStackTraceCallback callback) {
        invalidatePrepareStackTraceCallbackNotUsedAssumption();
        this.prepareStackTraceCallback = callback;
    }

    public final PrepareStackTraceCallback getPrepareStackTraceCallback() {
        return prepareStackTraceCallbackNotUsedAssumption.isValid() ? null : prepareStackTraceCallback;
    }

    private void invalidatePrepareStackTraceCallbackNotUsedAssumption() {
        if (prepareStackTraceCallbackNotUsedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            prepareStackTraceCallbackNotUsedAssumption.invalidate("prepare stack trace callback unused");
        }
    }

    public PromiseRejectionTracker getPromiseRejectionTracker() {
        return promiseRejectionTracker;
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

    public void notifyPromiseRejectionTracker(JSDynamicObject promise, int operation, Object value) {
        if (!promiseRejectionTrackerNotUsedAssumption.isValid() && promiseRejectionTracker != null) {
            switch (operation) {
                case JSPromise.REJECTION_TRACKER_OPERATION_REJECT:
                    invokePromiseRejected(promise, value);
                    break;
                case JSPromise.REJECTION_TRACKER_OPERATION_HANDLE:
                    invokePromiseRejectionHandled(promise);
                    break;
                case JSPromise.REJECTION_TRACKER_OPERATION_REJECT_AFTER_RESOLVED:
                    invokePromiseRejectedAfterResolved(promise, value);
                    break;
                case JSPromise.REJECTION_TRACKER_OPERATION_RESOLVE_AFTER_RESOLVED:
                    invokePromiseResolvedAfterResolved(promise, value);
                    break;
                default:
                    assert false : "Unknown operation: " + operation;
            }
        }
    }

    @TruffleBoundary
    private void invokePromiseRejected(JSDynamicObject promise, Object value) {
        promiseRejectionTracker.promiseRejected(promise, value);
    }

    @TruffleBoundary
    private void invokePromiseRejectionHandled(JSDynamicObject promise) {
        promiseRejectionTracker.promiseRejectionHandled(promise);
    }

    @TruffleBoundary
    private void invokePromiseRejectedAfterResolved(JSDynamicObject promise, Object value) {
        promiseRejectionTracker.promiseRejectedAfterResolved(promise, value);
    }

    @TruffleBoundary
    private void invokePromiseResolvedAfterResolved(JSDynamicObject promise, Object value) {
        promiseRejectionTracker.promiseResolvedAfterResolved(promise, value);
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

    public final void notifyPromiseHook(int changeType, JSDynamicObject promise) {
        if (!promiseHookNotUsedAssumption.isValid() && promiseHook != null) {
            JSRealm realm = JSRealm.getMain(null);
            if (changeType == -1) {
                // Information about parent for the incoming INIT event
                realm.storeParentPromise(promise);
            } else {
                JSDynamicObject parent = (changeType == PromiseHook.TYPE_INIT) ? realm.fetchParentPromise() : Undefined.instance;
                notifyPromiseHookImpl(changeType, promise, parent);
            }
        }
    }

    @TruffleBoundary
    private void notifyPromiseHookImpl(int changeType, JSDynamicObject promise, JSDynamicObject parent) {
        promiseHook.promiseChanged(changeType, promise, parent);
    }

    public final void setImportMetaInitializer(ImportMetaInitializer importMetaInitializer) {
        importMetaInitializerNotUsedAssumption.invalidate("ImportMetaInitializer unused");
        this.importMetaInitializer = importMetaInitializer;
    }

    public final boolean hasImportMetaInitializerBeenSet() {
        return !importMetaInitializerNotUsedAssumption.isValid();
    }

    @TruffleBoundary
    public final void notifyImportMetaInitializer(JSDynamicObject importMeta, JSModuleRecord module) {
        if (importMetaInitializer != null) {
            importMetaInitializer.initializeImportMeta(importMeta, module);
        }
    }

    public final void setImportModuleDynamicallyCallback(ImportModuleDynamicallyCallback callback) {
        importModuleDynamicallyCallbackNotUsedAssumption.invalidate();
        this.importModuleDynamicallyCallback = callback;
    }

    public final boolean hasImportModuleDynamicallyCallbackBeenSet() {
        return !importModuleDynamicallyCallbackNotUsedAssumption.isValid();
    }

    /**
     * Invokes the HostImportModuleDynamically (and FinishDynamicImport) callback. Returns a promise
     * of dynamic import completion or {@null} if no callback is installed or the callback failed.
     *
     * @return the callback result (a promise or {@code null}).
     */
    @TruffleBoundary
    public final JSDynamicObject hostImportModuleDynamically(JSRealm realm, ScriptOrModule referrer, Module.ModuleRequest moduleRequest) {
        if (importModuleDynamicallyCallback != null) {
            return importModuleDynamicallyCallback.importModuleDynamically(realm, referrer, moduleRequest);
        } else {
            return null;
        }
    }

    public final JSFunctionData getOrCreateBuiltinFunctionData(BuiltinFunctionKey key, Function<JSContext, JSFunctionData> factory) {
        final int index = key.ordinal();
        JSFunctionData functionData = builtinFunctionData[index];
        if (functionData != null) {
            return functionData;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        functionData = factory.apply(this);
        if (!FUNCTION_DATA_ARRAY_VAR_HANDLE.compareAndSet(builtinFunctionData, index, (JSFunctionData) null, functionData)) {
            functionData = (JSFunctionData) FUNCTION_DATA_ARRAY_VAR_HANDLE.getVolatile(builtinFunctionData, index);
        }
        return Objects.requireNonNull(functionData);
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
        return singleRealmAssumption.isValid();
    }

    public final boolean isSingleRealm() {
        return !isMultiContext() && singleRealmAssumption.isValid();
    }

    public final Assumption getSingleRealmAssumption() {
        return singleRealmAssumption;
    }

    public JSLanguageOptions getLanguageOptions() {
        return languageOptions;
    }

    @Idempotent
    public final boolean isMultiContext() {
        return isMultiContext;
    }

    public JSFunctionFactory getFunctionFactory(JSFunctionData functionData) {
        boolean isBuiltin = functionData.isBuiltin();
        boolean strictFunctionProperties = functionData.hasStrictFunctionProperties();
        boolean isConstructor = functionData.isConstructor();
        boolean isGenerator = functionData.isGenerator();
        boolean isAsync = functionData.isAsync();
        assert !isBuiltin || (!isGenerator && !isAsync) : "built-in functions are never generator or async functions!";
        if (isAsync) {
            if (isGenerator) {
                return asyncGeneratorFunctionFactory;
            } else {
                return asyncFunctionFactory;
            }
        } else if (isGenerator) {
            return generatorFunctionFactory;
        } else if (isConstructor && !isBuiltin) {
            if (strictFunctionProperties) {
                return strictConstructorFactory;
            } else {
                return constructorFactory;
            }
        } else {
            // Built-in constructor functions end up here due to the way they're initialized.
            if (strictFunctionProperties) {
                return strictFunctionFactory;
            } else {
                return functionFactory;
            }
        }
    }

    public JSFunctionFactory getBoundFunctionFactory(JSFunctionData functionData) {
        assert functionData.isStrict();
        return boundFunctionFactory;
    }

    public JSFunctionFactory getWrappedFunctionFactory() {
        return wrappedFunctionFactory;
    }

    JSObjectFactory.RealmData newObjectFactoryRealmData() {
        if (isMultiContext()) {
            return null; // unused
        } else {
            return new JSObjectFactory.RealmData(factoryCount);
        }
    }

    private JSFunctionData throwTypeErrorFunction(boolean restrictedProperty) {
        CallTarget throwTypeErrorCallTarget = new ThrowTypeErrorRootNode(getLanguage(), restrictedProperty).getCallTarget();
        return JSFunctionData.create(this, throwTypeErrorCallTarget, throwTypeErrorCallTarget, 0, Strings.EMPTY_STRING, false, false, false, true);
    }

    private JSFunctionData protoSetterFunction() {
        CallTarget callTarget = new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                Object obj = JSRuntime.requireObjectCoercible(JSArguments.getThisObject(arguments));
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    return Undefined.instance;
                }
                Object value = JSArguments.getUserArgument(arguments, 0);
                if (!JSDynamicObject.isJSDynamicObject(value) || value == Undefined.instance) {
                    return Undefined.instance;
                }
                if (!JSDynamicObject.isJSDynamicObject(obj)) {
                    return Undefined.instance;
                }
                JSDynamicObject thisObj = (JSDynamicObject) obj;
                if (!JSObject.setPrototype(thisObj, (JSDynamicObject) value)) {
                    throw Errors.createTypeErrorCannotSetProto(thisObj, (JSDynamicObject) value);
                }
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(this, callTarget, 0, Strings.concat(Strings.SET_SPC, JSObject.PROTO));
    }

    private JSFunctionData protoGetterFunction() {
        CallTarget callTarget = new JavaScriptRootNode(getLanguage(), null, null) {
            @Child private JSToObjectNode toObjectNode = JSToObjectNode.create();
            @Child private GetPrototypeNode getPrototypeNode = GetPrototypeNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = toObjectNode.execute(JSArguments.getThisObject(frame.getArguments()));
                if (JSDynamicObject.isJSDynamicObject(obj)) {
                    return getPrototypeNode.execute(obj);
                }
                return Null.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(this, callTarget, 0, Strings.concat(Strings.GET_SPC, JSObject.PROTO));
    }

    public void checkEvalAllowed() {
        if (languageOptions.disableEval()) {
            throw Errors.createEvalDisabled();
        }
    }

    public Locale getLocale() {
        Locale loc = locale;
        if (loc == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            loc = getLocaleImpl();
            locale = loc;
        }
        return loc;
    }

    @TruffleBoundary
    private Locale getLocaleImpl() {
        String name = getLanguageOptions().locale();
        if (name.isEmpty()) {
            return Locale.getDefault();
        } else {
            return Locale.forLanguageTag(name);
        }
    }

    public <T extends Node> T adoptNode(T node) {
        assert node.getParent() == null;
        sharedRootNode.insertAccessor(node);
        return node;
    }

    static final class SharedRootNode extends JavaScriptRootNode {
        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere();
        }

        void insertAccessor(Node node) {
            CompilerAsserts.neverPartOfCompilation();
            super.insert(node);
        }
    }

    public boolean isOptionTopLevelAwait() {
        return getLanguageOptions().topLevelAwait();
    }

    public final Set<TruffleString> getSupportedImportAssertions() {
        return supportedImportAssertions;
    }

    public static TruffleString getTypeImportAssertion() {
        return TYPE_IMPORT_ASSERTION;
    }

    void updateStableOptions(JSContextOptions contextOptions, StableContextOptionValue.UpdateKind kind) {
        optionRegexpStaticResult.update(contextOptions, kind);
        optionV8CompatibilityMode.update(contextOptions, kind);
        optionDirectByteBuffer.update(contextOptions, kind);
        optionTimerResolution.update(contextOptions, kind);
    }
}
