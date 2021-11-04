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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
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
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDictionary;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistryObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionFactory;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUncheckedProxyHandler;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDisplayNames;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
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
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;
import com.oracle.truffle.js.runtime.util.DebugJSAgent;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TimeProfiler;

public class JSContext {
    private final Evaluator evaluator;

    private final JavaScriptLanguage language;
    private TruffleLanguage.Env initialEnvironment;

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

    private volatile Map<String, Symbol> symbolRegistry;

    // 0 = Number, 1 = BigInt, 2 = String
    private int operatorCounter = 3;

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
    private final CallTarget speciesGetterFunctionCallTarget;
    private volatile CallTarget notConstructibleCallTargetCache;
    private volatile CallTarget generatorNotConstructibleCallTargetCache;
    private volatile CallTarget boundFunctionCallTargetCache;
    private volatile CallTarget boundFunctionConstructTargetCache;
    private volatile CallTarget boundFunctionConstructNewTargetCache;

    public enum BuiltinFunctionKey {
        ArrayFlattenIntoArray,
        AwaitFulfilled,
        AwaitRejected,
        AsyncGeneratorReturnFulfilled,
        AsyncGeneratorReturnRejected,
        AsyncFromSyncIteratorValueUnwrap,
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
        SymbolGetDescription,
        MapGetSize,
        SetGetSize,
        ArrayBufferViewLength,
        ArrayBufferViewBuffer,
        ArrayBufferViewByteLength,
        ArrayBufferViewByteByteOffset,
        ArrayBufferViewToString,
        DataViewBuffer,
        DataViewByteLength,
        DataViewByteOffset,
        CollatorGetCompare,
        NumberFormatGetFormat,
        DateTimeFormatGetFormat,
        SegmenterBreakType,
        SegmenterPosition,
        FunctionAsyncIterator,
        IsGraalRuntime,
        AsyncModuleExecutionFulfilled,
        AsyncModuleExecutionRejected,
        TopLevelAwaitResolve,
        TopLevelAwaitReject,
        WebAssemblyInstanceGetExports,
        WebAssemblyMemoryGetBuffer,
        WebAssemblyTableGetLength,
        WebAssemblyGlobalGetValue,
        WebAssemblyGlobalSetValue,
        WebAssemblySourceInstantiation,
        FinishImportModuleDynamicallyReject,
        FinishImportModuleDynamicallyResolve,
    }

    @CompilationFinal(dimensions = 1) private final JSFunctionData[] builtinFunctionData;

    private volatile JSFunctionData boundFunctionData;
    private volatile JSFunctionData boundConstructorFunctionData;

    final JSFunctionData throwerFunctionData;
    final JSFunctionData protoGetterFunctionData;
    final JSFunctionData protoSetterFunctionData;

    private Map<Shape, JSShapeData> shapeDataMap;

    final Assumption noChildRealmsAssumption;
    private final Assumption singleRealmAssumption;
    private final boolean isMultiContext;

    private final AtomicInteger realmInit = new AtomicInteger();
    private static final int REALM_UNINITIALIZED = 0;
    private static final int REALM_INITIALIZING = 1;
    private static final int REALM_INITIALIZED = 2;

    @CompilationFinal private AllocationReporter allocationReporter;

    private final JSContextOptions contextOptions;

    private final Map<Builtin, JSFunctionData> builtinFunctionDataMap = new ConcurrentHashMap<>();

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

    static final PrototypeSupplier functionPrototypeSupplier = JSRealm::getFunctionPrototype;
    static final PrototypeSupplier asyncFunctionPrototypeSupplier = JSRealm::getAsyncFunctionPrototype;
    static final PrototypeSupplier generatorFunctionPrototypeSupplier = JSRealm::getGeneratorFunctionPrototype;
    static final PrototypeSupplier asyncGeneratorFunctionPrototypeSupplier = JSRealm::getAsyncGeneratorFunctionPrototype;

    private final JSObjectFactory ordinaryObjectFactory;
    private final JSObjectFactory arrayFactory;
    private final JSObjectFactory lazyRegexArrayFactory;
    private final JSObjectFactory lazyRegexIndicesArrayFactory;
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

    private final JSObjectFactory symbolFactory;
    private final JSObjectFactory mapFactory;
    private final JSObjectFactory setFactory;
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

    private final JSObjectFactory globalObjectFactory;

    private final JSObjectFactory webAssemblyModuleFactory;
    private final JSObjectFactory webAssemblyInstanceFactory;
    private final JSObjectFactory webAssemblyMemoryFactory;
    private final JSObjectFactory webAssemblyTableFactory;
    private final JSObjectFactory webAssemblyGlobalFactory;

    private final int factoryCount;

    @CompilationFinal private Locale locale;

    private final PropertyProxy argumentsPropertyProxy;
    private final PropertyProxy callerPropertyProxy;

    private final Set<String> supportedImportAssertions;

    private static final String TYPE_IMPORT_ASSERTION = "type";

    /**
     * A shared root node that acts as a parent providing a lock to nodes that are not rooted in a
     * tree but in shared object factories for the purpose of adding properties to newly allocated
     * objects.
     */
    private final SharedRootNode sharedRootNode;

    protected JSContext(Evaluator evaluator, JSContextOptions contextOptions, JavaScriptLanguage lang, TruffleLanguage.Env env) {
        this.contextOptions = contextOptions;

        if (env != null) { // env could still be null
            setAllocationReporter(env);
            this.contextOptions.setOptionValues(env.getOptions());
        }

        this.language = lang;
        this.initialEnvironment = env;

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
        this.speciesGetterFunctionCallTarget = createSpeciesGetterFunctionCallTarget(lang);

        this.builtinFunctionData = new JSFunctionData[BuiltinFunctionKey.values().length];

        this.timeProfiler = contextOptions.isProfileTime() ? new TimeProfiler() : null;

        this.singleRealmAssumption = Truffle.getRuntime().createAssumption("single realm");
        this.noChildRealmsAssumption = Truffle.getRuntime().createAssumption("no child realms");

        this.throwerFunctionData = throwTypeErrorFunction();
        boolean annexB = isOptionAnnexB();
        this.protoGetterFunctionData = annexB ? protoGetterFunction() : null;
        this.protoSetterFunctionData = annexB ? protoSetterFunction() : null;

        this.isMultiContext = lang.isMultiContext();

        // shapes and factories
        PrototypeSupplier objectPrototypeSupplier = JSOrdinary.INSTANCE;
        CompilableBiFunction<JSContext, DynamicObject, Shape> ordinaryObjectShapeSupplier = JSOrdinary.SHAPE_SUPPLIER;
        JSObjectFactory.IntrinsicBuilder builder = new JSObjectFactory.IntrinsicBuilder(this);

        this.functionFactory = builder.function(functionPrototypeSupplier, false, false, false, false, false);
        this.constructorFactory = builder.function(functionPrototypeSupplier, false, true, false, false, false);
        this.strictFunctionFactory = builder.function(functionPrototypeSupplier, true, false, false, false, false);
        this.strictConstructorFactory = builder.function(functionPrototypeSupplier, true, true, false, false, false);

        this.asyncFunctionFactory = builder.function(asyncFunctionPrototypeSupplier, true, false, false, false, true);
        this.generatorFunctionFactory = builder.function(generatorFunctionPrototypeSupplier, true, false, true, false, false);
        this.asyncGeneratorFunctionFactory = builder.function(asyncGeneratorFunctionPrototypeSupplier, true, false, true, false, true);

        this.boundFunctionFactory = builder.function(functionPrototypeSupplier, true, false, false, true, false);

        this.ordinaryObjectFactory = builder.create(JSOrdinary.INSTANCE);
        this.arrayFactory = builder.create(JSArray.INSTANCE);
        this.lazyRegexArrayFactory = builder.create(JSArray.INSTANCE);
        this.lazyRegexIndicesArrayFactory = builder.create(JSArray.INSTANCE);
        this.booleanFactory = builder.create(JSBoolean.INSTANCE);
        this.numberFactory = builder.create(JSNumber.INSTANCE);
        this.bigIntFactory = builder.create(JSBigInt.INSTANCE);
        this.stringFactory = builder.create(JSString.INSTANCE);
        this.regExpFactory = builder.create(JSRegExp.INSTANCE);
        this.dateFactory = builder.create(JSDate.INSTANCE);

        this.symbolFactory = builder.create(JSSymbol.INSTANCE);
        this.mapFactory = builder.create(JSMap.INSTANCE);
        this.setFactory = builder.create(JSSet.INSTANCE);
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
        this.forInIteratorFactory = builder.create(JSRealm::getForInIteratorPrototype, JSFunction::makeInitialForInIteratorShape);

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

        this.dictionaryObjectFactory = JSConfig.DictionaryObject ? builder.create(objectPrototypeSupplier, JSDictionary::makeDictionaryShape) : null;

        this.globalObjectFactory = builder.create(objectPrototypeSupplier, JSGlobal::makeGlobalObjectShape);

        this.webAssemblyModuleFactory = builder.create(JSWebAssemblyModule.INSTANCE);
        this.webAssemblyInstanceFactory = builder.create(JSWebAssemblyInstance.INSTANCE);
        this.webAssemblyMemoryFactory = builder.create(JSWebAssemblyMemory.INSTANCE);
        this.webAssemblyTableFactory = builder.create(JSWebAssemblyTable.INSTANCE);
        this.webAssemblyGlobalFactory = builder.create(JSWebAssemblyGlobal.INSTANCE);

        this.factoryCount = builder.finish();

        this.argumentsPropertyProxy = new JSFunction.ArgumentsProxyProperty(this);
        this.callerPropertyProxy = new JSFunction.CallerProxyProperty(this);

        this.regExpGroupsEmptyShape = JSRegExp.makeInitialGroupsObjectShape(this);

        this.regexOptions = createRegexOptions(contextOptions);
        this.regexValidateOptions = regexOptions.isEmpty() ? REGEX_OPTION_VALIDATE : REGEX_OPTION_VALIDATE + ',' + regexOptions;

        this.supportedImportAssertions = contextOptions.isImportAssertions() ? new HashSet<>() : Collections.emptySet();
        if (contextOptions.isImportAssertions()) {
            supportedImportAssertions.add(TYPE_IMPORT_ASSERTION);
        }

        if (contextOptions.getUnhandledRejectionsMode() != JSContextOptions.UnhandledRejectionsTrackingMode.NONE) {
            setPromiseRejectionTracker(new BuiltinPromiseRejectionTracker(this, contextOptions.getUnhandledRejectionsMode()));
        }
    }

    public final Evaluator getEvaluator() {
        return evaluator;
    }

    public Object getNodeFactory() {
        return nodeFactory;
    }

    public final JSParserOptions getParserOptions() {
        return contextOptions.getParserOptions();
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

    public static JSContext createContext(Evaluator evaluator, JSContextOptions contextOptions, JavaScriptLanguage lang, TruffleLanguage.Env env) {
        return new JSContext(evaluator, contextOptions, lang, env);
    }

    public JSRealm createRealm(TruffleLanguage.Env env) {
        JSRealm parentRealm = JSRealm.PARENT_OF_NEW_REALM.get();
        boolean isTop = (parentRealm == null);
        if (realmInit.get() != REALM_UNINITIALIZED || !realmInit.compareAndSet(REALM_UNINITIALIZED, REALM_INITIALIZING)) {
            singleRealmAssumption.invalidate("single realm assumption");
        }

        if (!isTop) {
            noChildRealmsAssumption.invalidate();
        }
        JSRealm newRealm = new JSRealm(this, env);
        newRealm.setupGlobals();

        if (isTop) {
            if (contextOptions.isTest262Mode() || contextOptions.isTestV8Mode()) {
                newRealm.setAgent(new DebugJSAgent(contextOptions.canAgentBlock(), env.getOptions()));
            } else {
                newRealm.setAgent(new MainJSAgent(newRealm.getContext().getPromiseRejectionTracker()));
            }
            if (contextOptions.isV8RealmBuiltin()) {
                newRealm.initRealmList();
                newRealm.addToRealmList(newRealm);
            }
        } else {
            newRealm.setParent(parentRealm);
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

    public int getOperatorCounter() {
        return operatorCounter;
    }

    public int incOperatorCounter() {
        return operatorCounter++;
    }

    /**
     * ECMA 8.4.1 EnqueueJob.
     */
    public final void promiseEnqueueJob(JSRealm realm, DynamicObject job) {
        invalidatePromiseQueueNotUsedAssumption();
        realm.getAgent().enqueuePromiseJob(job);
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

    public final JSObjectFactory getLazyRegexArrayFactory() {
        return lazyRegexArrayFactory;
    }

    public final JSObjectFactory getLazyRegexIndicesArrayFactory() {
        return lazyRegexIndicesArrayFactory;
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

    private static final String REGEX_OPTION_U180E_WHITESPACE = "U180EWhitespace";
    private static final String REGEX_OPTION_REGRESSION_TEST_MODE = "RegressionTestMode";
    private static final String REGEX_OPTION_DUMP_AUTOMATA = "DumpAutomata";
    private static final String REGEX_OPTION_STEP_EXECUTION = "StepExecution";
    private static final String REGEX_OPTION_ALWAYS_EAGER = "AlwaysEager";
    private static final String REGEX_OPTION_VALIDATE = "Validate=true";

    private static String createRegexOptions(JSContextOptions contextOptions) {
        StringBuilder options = new StringBuilder();
        if (JSConfig.U180EWhitespace) {
            options.append(REGEX_OPTION_U180E_WHITESPACE + "=true,");
        }
        if (contextOptions.isRegexRegressionTestMode()) {
            options.append(REGEX_OPTION_REGRESSION_TEST_MODE + "=true,");
        }
        if (contextOptions.isRegexDumpAutomata()) {
            options.append(REGEX_OPTION_DUMP_AUTOMATA + "=true,");
        }
        if (contextOptions.isRegexStepExecution()) {
            options.append(REGEX_OPTION_STEP_EXECUTION + "=true,");
        }
        if (contextOptions.isRegexAlwaysEager()) {
            options.append(REGEX_OPTION_ALWAYS_EAGER + "=true,");
        }
        return options.toString();
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
            tRegexEmptyResult = TRegexUtil.InvokeExecMethodNode.getUncached().execute(RegexCompilerInterface.compile("[]", "", this, JSRealm.get(null)), "", 0);
            assert !TRegexUtil.TRegexResultAccessor.getUncached().isMatch(tRegexEmptyResult);
        }
        return tRegexEmptyResult;
    }

    public Shape getRegExpGroupsEmptyShape() {
        return regExpGroupsEmptyShape;
    }

    public void setSymbolRegistry(Map<String, Symbol> newSymbolRegistry) {
        this.symbolRegistry = newSymbolRegistry;
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

    private TruffleLanguage.Env getInitialEnvironment() {
        return initialEnvironment;
    }

    public void clearInitialEnvironment() {
        this.initialEnvironment = null;
    }

    public CallTarget getEmptyFunctionCallTarget() {
        return emptyFunctionCallTarget;
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

    public CallTarget getSpeciesGetterFunctionCallTarget() {
        return speciesGetterFunctionCallTarget;
    }

    private static CallTarget createSpeciesGetterFunctionCallTarget(JavaScriptLanguage lang) {
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
        if (result == null) {
            synchronized (this) {
                result = notConstructibleCallTargetCache;
                if (result == null) {
                    result = notConstructibleCallTargetCache = createNotConstructibleCallTarget(getLanguage(), false, this);
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
                    result = generatorNotConstructibleCallTargetCache = createNotConstructibleCallTarget(getLanguage(), true, this);
                }
            }
        }
        return result;
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

    @TruffleBoundary
    public CallTarget getBoundFunctionCallTarget() {
        CallTarget result = boundFunctionCallTargetCache;
        if (result == null) {
            synchronized (this) {
                result = boundFunctionCallTargetCache;
                if (result == null) {
                    result = boundFunctionCallTargetCache = JSFunction.createBoundRootNode(this, false, false).getCallTarget();
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
                    result = boundFunctionConstructTargetCache = JSFunction.createBoundRootNode(this, true, false).getCallTarget();
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
                    result = boundFunctionConstructNewTargetCache = JSFunction.createBoundRootNode(this, true, true).getCallTarget();
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

    private JSAgent getJSAgent() {
        return getRealm().getAgent();
    }

    public int getEcmaScriptVersion() {
        return contextOptions.getEcmaScriptVersion();
    }

    public int getPropertyCacheLimit() {
        return contextOptions.getPropertyCacheLimit();
    }

    public int getFunctionCacheLimit() {
        return contextOptions.getFunctionCacheLimit();
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
        return contextOptions.isAnnexB();
    }

    public boolean isOptionIntl402() {
        assert !(getInitialEnvironment() != null && getInitialEnvironment().isPreInitialization()) : "Patchable option intl-402 accessed during context pre-initialization.";
        return contextOptions.isIntl402();
    }

    public boolean isOptionRegexpMatchIndices() {
        return contextOptions.isRegexpMatchIndices();
    }

    public boolean isOptionRegexpStaticResult() {
        assert !(getInitialEnvironment() != null && getInitialEnvironment().isPreInitialization()) : "Patchable option static-regex-result accessed during context pre-initialization.";
        return contextOptions.isRegexpStaticResult();
    }

    public boolean isOptionRegexpStaticResultInContextInit() {
        return contextOptions.isRegexpStaticResult();
    }

    public boolean isOptionSharedArrayBuffer() {
        return contextOptions.isSharedArrayBuffer();
    }

    public boolean isOptionAtomics() {
        return contextOptions.isAtomics();
    }

    public boolean isOptionV8CompatibilityMode() {
        assert !(getInitialEnvironment() != null && getInitialEnvironment().isPreInitialization()) : "Patchable option v8-compat accessed during context pre-initialization.";
        return contextOptions.isV8CompatibilityMode();
    }

    /**
     * Returns whether the v8-compat option is set or not. This method is the same as
     * {@link #isOptionV8CompatibilityMode()}, but it does not trigger an assertion error when used
     * during context pre-initialization. It is meant to be used for taking decisions which can be
     * undone during context-patching.
     */
    public boolean isOptionV8CompatibilityModeInContextInit() {
        return contextOptions.isV8CompatibilityMode();
    }

    public boolean isOptionNashornCompatibilityMode() {
        return contextOptions.isNashornCompatibilityMode();
    }

    public boolean isOptionDebugBuiltin() {
        return contextOptions.isDebugBuiltin();
    }

    public boolean isOptionMleBuiltin() {
        return contextOptions.isMLEMode();
    }

    public boolean isOptionDirectByteBuffer() {
        assert !(getInitialEnvironment() != null && getInitialEnvironment().isPreInitialization()) : "Patchable option direct-byte-buffer accessed during context pre-initialization.";
        return contextOptions.isDirectByteBuffer();
    }

    public boolean isOptionParseOnly() {
        return contextOptions.isParseOnly();
    }

    public boolean isOptionDisableEval() {
        return contextOptions.isDisableEval();
    }

    public boolean isOptionDisableWith() {
        return contextOptions.isDisableWith();
    }

    public boolean isOptionAsyncStackTraces() {
        return contextOptions.isAsyncStackTraces();
    }

    public long getTimerResolution() {
        assert !(getInitialEnvironment() != null && getInitialEnvironment().isPreInitialization()) : "Patchable option timer-resolution accessed during context pre-initialization.";
        return contextOptions.getTimerResolution();
    }

    public long getFunctionArgumentsLimit() {
        return contextOptions.getFunctionArgumentsLimit();
    }

    public int getStringLengthLimit() {
        return contextOptions.getStringLengthLimit();
    }

    public boolean usePromiseResolve() {
        return contextOptions.isAwaitOptimization();
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

    public void notifyPromiseRejectionTracker(DynamicObject promise, int operation, Object value) {
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
    private void invokePromiseRejected(DynamicObject promise, Object value) {
        promiseRejectionTracker.promiseRejected(promise, value);
    }

    @TruffleBoundary
    private void invokePromiseRejectionHandled(DynamicObject promise) {
        promiseRejectionTracker.promiseRejectionHandled(promise);
    }

    @TruffleBoundary
    private void invokePromiseRejectedAfterResolved(DynamicObject promise, Object value) {
        promiseRejectionTracker.promiseRejectedAfterResolved(promise, value);
    }

    @TruffleBoundary
    private void invokePromiseResolvedAfterResolved(DynamicObject promise, Object value) {
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

    public final void setImportMetaInitializer(ImportMetaInitializer importMetaInitializer) {
        importMetaInitializerNotUsedAssumption.invalidate("ImportMetaInitializer unused");
        this.importMetaInitializer = importMetaInitializer;
    }

    public final boolean hasImportMetaInitializerBeenSet() {
        return !importMetaInitializerNotUsedAssumption.isValid();
    }

    @TruffleBoundary
    public final void notifyImportMetaInitializer(DynamicObject importMeta, JSModuleRecord module) {
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
    public final DynamicObject hostImportModuleDynamically(JSRealm realm, ScriptOrModule referrer, Module.ModuleRequest moduleRequest) {
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
        return !isMultiContext() && singleRealmAssumption.isValid();
    }

    public final void assumeSingleRealm() throws InvalidAssumptionException {
        singleRealmAssumption.check();
    }

    public final Assumption getSingleRealmAssumption() {
        return singleRealmAssumption;
    }

    public JSContextOptions getContextOptions() {
        return contextOptions;
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

    JSObjectFactory.RealmData newObjectFactoryRealmData() {
        if (isMultiContext()) {
            return null; // unused
        } else {
            return new JSObjectFactory.RealmData(factoryCount);
        }
    }

    private JSFunctionData throwTypeErrorFunction() {
        CallTarget throwTypeErrorCallTarget = new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                throw Errors.createTypeError("[[ThrowTypeError]] defined by ECMAScript");
            }
        }.getCallTarget();
        return JSFunctionData.create(this, throwTypeErrorCallTarget, throwTypeErrorCallTarget, 0, "", false, false, false, true);
    }

    private JSFunctionData protoSetterFunction() {
        CallTarget callTarget = new JavaScriptRootNode(getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                Object obj = JSRuntime.requireObjectCoercible(JSArguments.getThisObject(arguments), JSContext.this);
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
                DynamicObject thisObj = (DynamicObject) obj;
                if (!JSObject.setPrototype(thisObj, (DynamicObject) value)) {
                    throw Errors.createTypeErrorCannotSetProto(thisObj, (DynamicObject) value);
                }
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(this, callTarget, 0, "set " + JSObject.PROTO);
    }

    private JSFunctionData protoGetterFunction() {
        CallTarget callTarget = new JavaScriptRootNode(getLanguage(), null, null) {
            @Child private JSToObjectNode toObjectNode = JSToObjectNode.createToObject(JSContext.this);
            @Child private GetPrototypeNode getPrototypeNode = GetPrototypeNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = toObjectNode.execute(JSArguments.getThisObject(frame.getArguments()));
                if (JSDynamicObject.isJSDynamicObject(obj)) {
                    return getPrototypeNode.executeJSObject(obj);
                }
                return Null.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(this, callTarget, 0, "get " + JSObject.PROTO);
    }

    public void checkEvalAllowed() {
        if (isOptionDisableEval()) {
            throw Errors.createEvalDisabled();
        }
    }

    public boolean isOptionLoadFromURL() {
        return contextOptions.isLoadFromURL();
    }

    public boolean isOptionLoadFromClasspath() {
        return contextOptions.isLoadFromClasspath();
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
        String name = getContextOptions().getLocale();
        if (name.isEmpty()) {
            return Locale.getDefault();
        } else {
            return Locale.forLanguageTag(name);
        }
    }

    public PropertyProxy getArgumentsPropertyProxy() {
        return argumentsPropertyProxy;
    }

    public PropertyProxy getCallerPropertyProxy() {
        return callerPropertyProxy;
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
        return getContextOptions().isTopLevelAwait();
    }

    public final Set<String> getSupportedImportAssertions() {
        return supportedImportAssertions;
    }

    public static String getTypeImportAssertion() {
        return TYPE_IMPORT_ASSERTION;
    }
}
