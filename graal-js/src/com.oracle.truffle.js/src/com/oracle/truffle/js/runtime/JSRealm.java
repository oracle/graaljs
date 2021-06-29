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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_SOURCE_NAME_SUFFIX;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

import com.oracle.truffle.js.runtime.builtins.JSRecord;
import org.graalvm.collections.Pair;
import org.graalvm.home.HomeFinder;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.ArrayIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.AtomicsBuiltins;
import com.oracle.truffle.js.builtins.ConsoleBuiltins;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.DebugBuiltins;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.JavaBuiltins;
import com.oracle.truffle.js.builtins.MapIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltins;
import com.oracle.truffle.js.builtins.OperatorsBuiltins;
import com.oracle.truffle.js.builtins.PerformanceBuiltins;
import com.oracle.truffle.js.builtins.PolyglotBuiltins;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltins;
import com.oracle.truffle.js.builtins.ReflectBuiltins;
import com.oracle.truffle.js.builtins.RegExpBuiltins;
import com.oracle.truffle.js.builtins.RegExpStringIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.SetIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.StringIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.commonjs.CommonJSRequireBuiltin;
import com.oracle.truffle.js.builtins.commonjs.GlobalCommonJSRequireBuiltins;
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader;
import com.oracle.truffle.js.builtins.foreign.ForeignIterablePrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.Builtin;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMath;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSON;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSTest262;
import com.oracle.truffle.js.runtime.builtins.JSTestV8;
import com.oracle.truffle.js.runtime.builtins.JSTuple;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDisplayNames;
import com.oracle.truffle.js.runtime.builtins.intl.JSIntl;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssembly;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.interop.DynamicScopeWrapper;
import com.oracle.truffle.js.runtime.interop.TopScopeObject;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.PrintWriterWrapper;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Container for JavaScript globals (i.e. an ECMAScript 6 Realm object).
 */
public class JSRealm {

    public static final String POLYGLOT_CLASS_NAME = "Polyglot";
    // used for non-public properties of Polyglot
    public static final String REFLECT_CLASS_NAME = "Reflect";
    public static final String SHARED_ARRAY_BUFFER_CLASS_NAME = "SharedArrayBuffer";
    public static final String ATOMICS_CLASS_NAME = "Atomics";
    public static final String REALM_BUILTIN_CLASS_NAME = "Realm";
    public static final String ARGUMENTS_NAME = "arguments";
    public static final String JAVA_CLASS_NAME = "Java";
    public static final String JAVA_CLASS_NAME_NASHORN_COMPAT = "JavaNashornCompat";
    public static final String PERFORMANCE_CLASS_NAME = "performance";
    public static final String DEBUG_CLASS_NAME = "Debug";
    public static final String CONSOLE_CLASS_NAME = "Console";

    private static final String GRAALVM_VERSION = HomeFinder.getInstance().getVersion();

    private final JSContext context;

    @CompilationFinal private DynamicObject globalObject;

    private final DynamicObject objectConstructor;
    private final DynamicObject objectPrototype;
    private final DynamicObject functionConstructor;
    private final DynamicObject functionPrototype;

    private final DynamicObject arrayConstructor;
    private final DynamicObject arrayPrototype;
    private final DynamicObject booleanConstructor;
    private final DynamicObject booleanPrototype;
    private final DynamicObject numberConstructor;
    private final DynamicObject numberPrototype;
    private final DynamicObject bigIntConstructor;
    private final DynamicObject bigIntPrototype;
    private final DynamicObject stringConstructor;
    private final DynamicObject stringPrototype;
    private final DynamicObject regExpConstructor;
    private final DynamicObject regExpPrototype;
    private final DynamicObject collatorConstructor;
    private final DynamicObject collatorPrototype;
    private final DynamicObject numberFormatConstructor;
    private final DynamicObject numberFormatPrototype;
    private final DynamicObject pluralRulesConstructor;
    private final DynamicObject pluralRulesPrototype;
    private final DynamicObject listFormatConstructor;
    private final DynamicObject listFormatPrototype;
    private final DynamicObject dateTimeFormatConstructor;
    private final DynamicObject dateTimeFormatPrototype;
    private final DynamicObject relativeTimeFormatConstructor;
    private final DynamicObject relativeTimeFormatPrototype;
    private final DynamicObject segmenterConstructor;
    private final DynamicObject segmenterPrototype;
    private final DynamicObject displayNamesConstructor;
    private final DynamicObject displayNamesPrototype;
    private final DynamicObject localeConstructor;
    private final DynamicObject localePrototype;
    private final DynamicObject dateConstructor;
    private final DynamicObject datePrototype;
    @CompilationFinal(dimensions = 1) private final DynamicObject[] errorConstructors;
    @CompilationFinal(dimensions = 1) private final DynamicObject[] errorPrototypes;
    private final DynamicObject callSiteConstructor;
    private final DynamicObject callSitePrototype;

    private final Shape initialRegExpPrototypeShape;
    private final JSObjectFactory.RealmData objectFactories;

    // ES6:
    private final DynamicObject symbolConstructor;
    private final DynamicObject symbolPrototype;
    private final DynamicObject mapConstructor;
    private final DynamicObject mapPrototype;
    private final DynamicObject setConstructor;
    private final DynamicObject setPrototype;
    private final DynamicObject weakRefConstructor;
    private final DynamicObject weakRefPrototype;
    private final DynamicObject weakMapConstructor;
    private final DynamicObject weakMapPrototype;
    private final DynamicObject weakSetConstructor;
    private final DynamicObject weakSetPrototype;

    private final DynamicObject mathObject;
    private DynamicObject realmBuiltinObject;
    private Object evalFunctionObject;
    private final Object applyFunctionObject;
    private final Object callFunctionObject;
    private Object reflectApplyFunctionObject;
    private Object reflectConstructFunctionObject;
    private Object commonJSRequireFunctionObject;
    private Map<String, Object> commonJSPreLoadedBuiltins;
    private Object jsonParseFunctionObject;

    private final DynamicObject arrayBufferConstructor;
    private final DynamicObject arrayBufferPrototype;
    private final DynamicObject sharedArrayBufferConstructor;
    private final DynamicObject sharedArrayBufferPrototype;

    @CompilationFinal(dimensions = 1) private final DynamicObject[] typedArrayConstructors;
    @CompilationFinal(dimensions = 1) private final DynamicObject[] typedArrayPrototypes;
    private final DynamicObject dataViewConstructor;
    private final DynamicObject dataViewPrototype;
    private final DynamicObject jsAdapterConstructor;
    private final DynamicObject jsAdapterPrototype;
    private final DynamicObject javaImporterConstructor;
    private final DynamicObject javaImporterPrototype;
    private final DynamicObject proxyConstructor;
    private final DynamicObject proxyPrototype;
    private final DynamicObject finalizationRegistryConstructor;
    private final DynamicObject finalizationRegistryPrototype;

    private final DynamicObject iteratorPrototype;
    private final DynamicObject arrayIteratorPrototype;
    private final DynamicObject setIteratorPrototype;
    private final DynamicObject mapIteratorPrototype;
    private final DynamicObject segmentIteratorPrototype;
    private final DynamicObject stringIteratorPrototype;
    private final DynamicObject regExpStringIteratorPrototype;
    private final DynamicObject enumerateIteratorPrototype;
    private final DynamicObject forInIteratorPrototype;

    private final DynamicObject generatorFunctionConstructor;
    private final DynamicObject generatorFunctionPrototype;
    private final DynamicObject generatorObjectPrototype;

    private final DynamicObject asyncFunctionConstructor;
    private final DynamicObject asyncFunctionPrototype;

    private final DynamicObject asyncIteratorPrototype;
    private final DynamicObject asyncFromSyncIteratorPrototype;
    private final DynamicObject asyncGeneratorObjectPrototype;
    private final DynamicObject asyncGeneratorFunctionConstructor;
    private final DynamicObject asyncGeneratorFunctionPrototype;

    private final DynamicObject throwerFunction;
    private final Accessor throwerAccessor;

    private final DynamicObject promiseConstructor;
    private final DynamicObject promisePrototype;
    private DynamicObject promiseAllFunctionObject;

    @CompilationFinal private DynamicObject javaPackageToPrimitiveFunction;

    private final DynamicObject arrayProtoValuesIterator;
    @CompilationFinal private DynamicObject typedArrayConstructor;
    @CompilationFinal private DynamicObject typedArrayPrototype;

    private DynamicObject preinitIntlObject;
    private DynamicObject preinitConsoleBuiltinObject;
    private DynamicObject preinitPerformanceObject;

    private volatile Map<Object, DynamicObject> templateRegistry;

    private final DynamicObject globalScope;

    private final DynamicObject scriptEngineImportScope;

    @CompilationFinal private TopScopeObject topScope;

    private TruffleLanguage.Env truffleLanguageEnv;

    /**
     * True while calling Error.prepareStackTrace via the stack property of an error object.
     */
    private boolean preparingStackTrace;

    /**
     * Slot for Realm-specific data of the embedder of the JS engine.
     */
    private Object embedderData;

    /** Support for RegExp.$1. */
    private Object staticRegexResult;
    private String staticRegexResultInputString = "";
    private Object staticRegexResultCompiledRegex;
    private boolean staticRegexResultInvalidated;
    private long staticRegexResultFromIndex;
    private String staticRegexResultOriginalInputString;

    /** WebAssembly support. */
    private final Object wasmCompileFunction;
    private final Object wasmInstantiateFunction;
    private final Object wasmValidateFunction;
    private final Object wasmModuleExportsFunction;
    private final Object wasmModuleImportsFunction;
    private final Object wasmModuleCustomSectionsFunction;
    private final Object wasmGlobalConstructor;
    private final Object wasmMemoryConstructor;
    private final Object wasmTableConstructor;

    private final DynamicObject webAssemblyObject;
    private final DynamicObject webAssemblyGlobalConstructor;
    private final DynamicObject webAssemblyGlobalPrototype;
    private final DynamicObject webAssemblyInstanceConstructor;
    private final DynamicObject webAssemblyInstancePrototype;
    private final DynamicObject webAssemblyMemoryConstructor;
    private final DynamicObject webAssemblyMemoryPrototype;
    private final DynamicObject webAssemblyModuleConstructor;
    private final DynamicObject webAssemblyModulePrototype;
    private final DynamicObject webAssemblyTableConstructor;
    private final DynamicObject webAssemblyTablePrototype;

    /** Foreign object prototypes. */
    private final DynamicObject foreignIterablePrototype;

    /** Record and Tuple support. */
    private final DynamicObject recordConstructor;
    private final DynamicObject recordPrototype;
    private final DynamicObject tupleConstructor;
    private final DynamicObject tuplePrototype;

    /**
     * Local time zone ID. Initialized lazily.
     */
    @CompilationFinal private ZoneId localTimeZoneId;

    public static final long NANOSECONDS_PER_MILLISECOND = 1000000;
    private SplittableRandom random;
    private long nanoToZeroTimeOffset;
    private long nanoToCurrentTimeOffset;
    private long lastFuzzyTime = Long.MIN_VALUE;

    private OutputStream outputStream;
    private OutputStream errorStream;
    private PrintWriterWrapper outputWriter;
    private PrintWriterWrapper errorWriter;

    private final JSConsoleUtil consoleUtil;
    private JSModuleLoader moduleLoader;

    /**
     * ECMA2017 8.7 Agent object.
     */
    @CompilationFinal private JSAgent agent;

    /**
     * List of realms (for V8 Realm built-in). The list is available in top-level realm only (not in
     * child realms).
     */
    private List<JSRealm> realmList;
    /**
     * Parent realm (for a child realm) or {@code null} for a top-level realm.
     */
    private JSRealm parentRealm;
    /**
     * Current realm (as returned by {@code Realm.current()} V8 built-in).
     */
    private JSRealm v8RealmCurrent = this;
    /**
     * Value shared across V8 realms ({@code Realm.shared}).
     */
    Object v8RealmShared = Undefined.instance;

    static final ThreadLocal<Boolean> CREATING_CHILD_REALM = new ThreadLocal<>();

    /**
     * Used to the pass call site source location for caller sensitive built-in functions.
     */
    private JavaScriptBaseNode callNode;

    /**
     * Per-realm CommonJs `require` cache.
     */
    private final Map<TruffleFile, DynamicObject> commonJSRequireCache;

    /**
     * Stack of receivers of (Typed)Array.prototype.join. Used to avoid cyclic calls.
     */
    private final SimpleArrayList<Object> joinStack = new SimpleArrayList<>();

    public JSRealm(JSContext context, TruffleLanguage.Env env) {
        this.context = context;
        this.truffleLanguageEnv = env; // can be null

        // need to build Function and Function.proto in a weird order to avoid circular dependencies
        this.objectPrototype = JSObjectPrototype.create(context);

        this.functionPrototype = JSFunction.createFunctionPrototype(this, objectPrototype);

        this.objectFactories = context.newObjectFactoryRealmData();

        this.throwerFunction = createThrowerFunction();
        this.throwerAccessor = new Accessor(throwerFunction, throwerFunction);

        if (context.isOptionAnnexB()) {
            putProtoAccessorProperty(this);
        }

        this.globalObject = JSGlobal.create(this, objectPrototype);
        this.globalScope = JSGlobal.createGlobalScope(context);
        if (context.getContextOptions().isScriptEngineGlobalScopeImport()) {
            this.scriptEngineImportScope = JSOrdinary.createWithNullPrototypeInit(context);
        } else {
            this.scriptEngineImportScope = null;
        }
        this.topScope = createTopScope();

        this.objectConstructor = createObjectConstructor(this, objectPrototype);
        JSObjectUtil.putDataProperty(context, this.objectPrototype, JSObject.CONSTRUCTOR, objectConstructor, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putFunctionsFromContainer(this, this.objectPrototype, JSObjectPrototype.BUILTINS);
        this.functionConstructor = JSFunction.createFunctionConstructor(this);
        JSFunction.fillFunctionPrototype(this);

        this.applyFunctionObject = JSDynamicObject.getOrNull(getFunctionPrototype(), "apply");
        this.callFunctionObject = JSDynamicObject.getOrNull(getFunctionPrototype(), "call");

        JSConstructor ctor;
        ctor = JSArray.createConstructor(this);
        this.arrayConstructor = ctor.getFunctionObject();
        this.arrayPrototype = ctor.getPrototype();
        ctor = JSBoolean.createConstructor(this);
        this.booleanConstructor = ctor.getFunctionObject();
        this.booleanPrototype = ctor.getPrototype();
        ctor = JSNumber.createConstructor(this);
        this.numberConstructor = ctor.getFunctionObject();
        this.numberPrototype = ctor.getPrototype();
        ctor = JSString.createConstructor(this);
        this.stringConstructor = ctor.getFunctionObject();
        this.stringPrototype = ctor.getPrototype();
        ctor = JSRegExp.createConstructor(this);
        this.regExpConstructor = ctor.getFunctionObject();
        this.regExpPrototype = ctor.getPrototype();
        ctor = JSDate.createConstructor(this);
        this.dateConstructor = ctor.getFunctionObject();
        this.datePrototype = ctor.getPrototype();
        this.initialRegExpPrototypeShape = this.regExpPrototype.getShape();
        boolean es6 = context.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2015;
        if (es6) {
            ctor = JSSymbol.createConstructor(this);
            this.symbolConstructor = ctor.getFunctionObject();
            this.symbolPrototype = ctor.getPrototype();
            ctor = JSMap.createConstructor(this);
            this.mapConstructor = ctor.getFunctionObject();
            this.mapPrototype = ctor.getPrototype();
            ctor = JSSet.createConstructor(this);
            this.setConstructor = ctor.getFunctionObject();
            this.setPrototype = ctor.getPrototype();
            ctor = JSWeakMap.createConstructor(this);
            this.weakMapConstructor = ctor.getFunctionObject();
            this.weakMapPrototype = ctor.getPrototype();
            ctor = JSWeakSet.createConstructor(this);
            this.weakSetConstructor = ctor.getFunctionObject();
            this.weakSetPrototype = ctor.getPrototype();
            ctor = JSProxy.createConstructor(this);
            this.proxyConstructor = ctor.getFunctionObject();
            this.proxyPrototype = ctor.getPrototype();
            ctor = JSPromise.createConstructor(this);
            this.promiseConstructor = ctor.getFunctionObject();
            this.promisePrototype = ctor.getPrototype();
        } else {
            this.symbolConstructor = null;
            this.symbolPrototype = null;
            this.mapConstructor = null;
            this.mapPrototype = null;
            this.setConstructor = null;
            this.setPrototype = null;
            this.weakMapConstructor = null;
            this.weakMapPrototype = null;
            this.weakSetConstructor = null;
            this.weakSetPrototype = null;
            this.proxyConstructor = null;
            this.proxyPrototype = null;
            this.promiseConstructor = null;
            this.promisePrototype = null;
        }

        this.errorConstructors = new DynamicObject[JSErrorType.errorTypes().length];
        this.errorPrototypes = new DynamicObject[JSErrorType.errorTypes().length];
        initializeErrorConstructors();
        ctor = JSError.createCallSiteConstructor(this);
        this.callSiteConstructor = ctor.getFunctionObject();
        this.callSitePrototype = ctor.getPrototype();

        ctor = JSArrayBuffer.createConstructor(this);
        this.arrayBufferConstructor = ctor.getFunctionObject();
        this.arrayBufferPrototype = ctor.getPrototype();
        this.typedArrayConstructors = new DynamicObject[TypedArray.factories(context).length];
        this.typedArrayPrototypes = new DynamicObject[TypedArray.factories(context).length];
        initializeTypedArrayConstructors();
        ctor = JSDataView.createConstructor(this);
        this.dataViewConstructor = ctor.getFunctionObject();
        this.dataViewPrototype = ctor.getPrototype();

        if (context.getContextOptions().isBigInt()) {
            ctor = JSBigInt.createConstructor(this);
            this.bigIntConstructor = ctor.getFunctionObject();
            this.bigIntPrototype = ctor.getPrototype();
        } else {
            this.bigIntConstructor = null;
            this.bigIntPrototype = null;
        }

        this.iteratorPrototype = createIteratorPrototype();
        this.arrayIteratorPrototype = es6 ? createArrayIteratorPrototype() : null;
        this.setIteratorPrototype = es6 ? createSetIteratorPrototype() : null;
        this.mapIteratorPrototype = es6 ? createMapIteratorPrototype() : null;
        this.stringIteratorPrototype = es6 ? createStringIteratorPrototype() : null;
        this.regExpStringIteratorPrototype = context.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2019 ? createRegExpStringIteratorPrototype() : null;

        ctor = JSCollator.createConstructor(this);
        this.collatorConstructor = ctor.getFunctionObject();
        this.collatorPrototype = ctor.getPrototype();
        ctor = JSNumberFormat.createConstructor(this);
        this.numberFormatConstructor = ctor.getFunctionObject();
        this.numberFormatPrototype = ctor.getPrototype();
        ctor = JSDateTimeFormat.createConstructor(this);
        this.dateTimeFormatConstructor = ctor.getFunctionObject();
        this.dateTimeFormatPrototype = ctor.getPrototype();
        ctor = JSPluralRules.createConstructor(this);
        this.pluralRulesConstructor = ctor.getFunctionObject();
        this.pluralRulesPrototype = ctor.getPrototype();
        ctor = JSListFormat.createConstructor(this);
        this.listFormatConstructor = ctor.getFunctionObject();
        this.listFormatPrototype = ctor.getPrototype();
        ctor = JSRelativeTimeFormat.createConstructor(this);
        this.relativeTimeFormatConstructor = ctor.getFunctionObject();
        this.relativeTimeFormatPrototype = ctor.getPrototype();
        ctor = JSSegmenter.createConstructor(this);
        this.segmenterConstructor = ctor.getFunctionObject();
        this.segmenterPrototype = ctor.getPrototype();
        this.segmentIteratorPrototype = JSSegmenter.createSegmentIteratorPrototype(context, this);
        ctor = JSDisplayNames.createConstructor(this);
        this.displayNamesConstructor = ctor.getFunctionObject();
        this.displayNamesPrototype = ctor.getPrototype();
        ctor = JSLocale.createConstructor(this);
        this.localeConstructor = ctor.getFunctionObject();
        this.localePrototype = ctor.getPrototype();

        if (es6) {
            ctor = JSFunction.createGeneratorFunctionConstructor(this);
            this.generatorFunctionConstructor = ctor.getFunctionObject();
            this.generatorFunctionPrototype = ctor.getPrototype();
            this.generatorObjectPrototype = (DynamicObject) JSDynamicObject.getOrNull(generatorFunctionPrototype, JSObject.PROTOTYPE);
        } else {
            this.generatorFunctionConstructor = null;
            this.generatorFunctionPrototype = null;
            this.generatorObjectPrototype = null;
        }
        this.enumerateIteratorPrototype = JSFunction.createEnumerateIteratorPrototype(this);
        this.forInIteratorPrototype = JSFunction.createForInIteratorPrototype(this);
        this.arrayProtoValuesIterator = (DynamicObject) JSDynamicObject.getOrDefault(getArrayPrototype(), Symbol.SYMBOL_ITERATOR, Undefined.instance);

        if (context.isOptionSharedArrayBuffer()) {
            ctor = JSSharedArrayBuffer.createConstructor(this);
            this.sharedArrayBufferConstructor = ctor.getFunctionObject();
            this.sharedArrayBufferPrototype = ctor.getPrototype();
        } else {
            this.sharedArrayBufferConstructor = null;
            this.sharedArrayBufferPrototype = null;
        }

        this.mathObject = JSMath.create(this);

        boolean es8 = context.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2017;
        if (es8) {
            ctor = JSFunction.createAsyncFunctionConstructor(this);
            this.asyncFunctionConstructor = ctor.getFunctionObject();
            this.asyncFunctionPrototype = ctor.getPrototype();
        } else {
            this.asyncFunctionConstructor = null;
            this.asyncFunctionPrototype = null;
        }

        boolean es9 = context.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2018;
        if (es9) {
            this.asyncIteratorPrototype = JSFunction.createAsyncIteratorPrototype(this);
            this.asyncFromSyncIteratorPrototype = JSFunction.createAsyncFromSyncIteratorPrototype(this);
            ctor = JSFunction.createAsyncGeneratorFunctionConstructor(this);
            this.asyncGeneratorFunctionConstructor = ctor.getFunctionObject();
            this.asyncGeneratorFunctionPrototype = ctor.getPrototype();
            this.asyncGeneratorObjectPrototype = (DynamicObject) JSDynamicObject.getOrNull(asyncGeneratorFunctionPrototype, JSObject.PROTOTYPE);
        } else {
            this.asyncIteratorPrototype = null;
            this.asyncFromSyncIteratorPrototype = null;
            this.asyncGeneratorFunctionConstructor = null;
            this.asyncGeneratorFunctionPrototype = null;
            this.asyncGeneratorObjectPrototype = null;
        }

        boolean es12 = context.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2021;
        if (es12) {
            ctor = JSWeakRef.createConstructor(this);
            this.weakRefConstructor = ctor.getFunctionObject();
            this.weakRefPrototype = ctor.getPrototype();

            ctor = JSFinalizationRegistry.createConstructor(this);
            this.finalizationRegistryConstructor = ctor.getFunctionObject();
            this.finalizationRegistryPrototype = ctor.getPrototype();
        } else {
            this.weakRefConstructor = null;
            this.weakRefPrototype = null;
            this.finalizationRegistryConstructor = null;
            this.finalizationRegistryPrototype = null;
        }

        boolean nashornCompat = context.isOptionNashornCompatibilityMode();
        if (nashornCompat) {
            ctor = JSAdapter.createConstructor(this);
            this.jsAdapterConstructor = ctor.getFunctionObject();
            this.jsAdapterPrototype = ctor.getPrototype();
            ctor = JavaImporter.createConstructor(this);
            this.javaImporterConstructor = ctor.getFunctionObject();
            this.javaImporterPrototype = ctor.getPrototype();
        } else {
            this.jsAdapterConstructor = null;
            this.jsAdapterPrototype = null;
            this.javaImporterConstructor = null;
            this.javaImporterPrototype = null;
        }

        this.outputStream = System.out;
        this.errorStream = System.err;
        this.outputWriter = new PrintWriterWrapper(outputStream, true);
        this.errorWriter = new PrintWriterWrapper(errorStream, true);
        this.consoleUtil = new JSConsoleUtil();

        if (context.getContextOptions().isCommonJSRequire()) {
            this.commonJSRequireCache = new HashMap<>();
        } else {
            this.commonJSRequireCache = null;
        }

        if (context.getContextOptions().isWebAssembly()) {
            if (!isWasmAvailable()) {
                throw new IllegalStateException("WebAssembly API enabled but wasm language cannot be accessed!");
            }
            LanguageInfo wasmLanguageInfo = truffleLanguageEnv.getInternalLanguages().get("wasm");
            truffleLanguageEnv.initializeLanguage(wasmLanguageInfo);
            Object wasmObject = truffleLanguageEnv.importSymbol("WebAssembly");

            try {
                InteropLibrary wasmInterop = InteropLibrary.getUncached(wasmObject);
                wasmCompileFunction = wasmInterop.readMember(wasmObject, "compile");
                wasmInstantiateFunction = wasmInterop.readMember(wasmObject, "instantiate");
                wasmValidateFunction = wasmInterop.readMember(wasmObject, "validate");
                wasmGlobalConstructor = wasmInterop.readMember(wasmObject, "Global");
                wasmMemoryConstructor = wasmInterop.readMember(wasmObject, "Memory");
                wasmTableConstructor = wasmInterop.readMember(wasmObject, "Table");
                Object wasmModuleConstructor = wasmInterop.readMember(wasmObject, "Module");
                InteropLibrary moduleInterop = InteropLibrary.getUncached(wasmModuleConstructor);
                wasmModuleExportsFunction = moduleInterop.readMember(wasmModuleConstructor, "exports");
                wasmModuleImportsFunction = moduleInterop.readMember(wasmModuleConstructor, "imports");
                wasmModuleCustomSectionsFunction = moduleInterop.readMember(wasmModuleConstructor, "customSections");
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }

            this.webAssemblyObject = JSWebAssembly.create(this);
            ctor = JSWebAssemblyModule.createConstructor(this);
            this.webAssemblyModuleConstructor = ctor.getFunctionObject();
            this.webAssemblyModulePrototype = ctor.getPrototype();
            ctor = JSWebAssemblyInstance.createConstructor(this);
            this.webAssemblyInstanceConstructor = ctor.getFunctionObject();
            this.webAssemblyInstancePrototype = ctor.getPrototype();
            ctor = JSWebAssemblyMemory.createConstructor(this);
            this.webAssemblyMemoryConstructor = ctor.getFunctionObject();
            this.webAssemblyMemoryPrototype = ctor.getPrototype();
            ctor = JSWebAssemblyTable.createConstructor(this);
            this.webAssemblyTableConstructor = ctor.getFunctionObject();
            this.webAssemblyTablePrototype = ctor.getPrototype();
            ctor = JSWebAssemblyGlobal.createConstructor(this);
            this.webAssemblyGlobalConstructor = ctor.getFunctionObject();
            this.webAssemblyGlobalPrototype = ctor.getPrototype();
        } else {
            this.wasmCompileFunction = null;
            this.wasmInstantiateFunction = null;
            this.wasmValidateFunction = null;
            this.wasmModuleExportsFunction = null;
            this.wasmModuleImportsFunction = null;
            this.wasmModuleCustomSectionsFunction = null;
            this.wasmGlobalConstructor = null;
            this.wasmMemoryConstructor = null;
            this.wasmTableConstructor = null;

            this.webAssemblyObject = null;
            this.webAssemblyGlobalConstructor = null;
            this.webAssemblyGlobalPrototype = null;
            this.webAssemblyInstanceConstructor = null;
            this.webAssemblyInstancePrototype = null;
            this.webAssemblyMemoryConstructor = null;
            this.webAssemblyMemoryPrototype = null;
            this.webAssemblyModuleConstructor = null;
            this.webAssemblyModulePrototype = null;
            this.webAssemblyTableConstructor = null;
            this.webAssemblyTablePrototype = null;
        }

        this.foreignIterablePrototype = createForeignIterablePrototype();

        if (context.isRecordAndTupleEnabled()) {
            ctor = JSRecord.createConstructor(this);
            this.recordConstructor = ctor.getFunctionObject();
            this.recordPrototype = ctor.getPrototype();
            ctor = JSTuple.createConstructor(this);
            this.tupleConstructor = ctor.getFunctionObject();
            this.tuplePrototype = ctor.getPrototype();
        } else {
            this.recordConstructor = null;
            this.recordPrototype = null;
            this.tupleConstructor = null;
            this.tuplePrototype= null;
        }
    }

    private void initializeTypedArrayConstructors() {
        JSConstructor taConst = JSArrayBufferView.createTypedArrayConstructor(this);
        typedArrayConstructor = taConst.getFunctionObject();
        typedArrayPrototype = taConst.getPrototype();

        for (TypedArrayFactory factory : TypedArray.factories(context)) {
            JSConstructor constructor = JSArrayBufferView.createConstructor(this, factory, taConst);
            typedArrayConstructors[factory.getFactoryIndex()] = constructor.getFunctionObject();
            typedArrayPrototypes[factory.getFactoryIndex()] = constructor.getPrototype();
        }
    }

    private void initializeErrorConstructors() {
        for (JSErrorType type : JSErrorType.errorTypes()) {
            JSConstructor errorConstructor = JSError.createErrorConstructor(this, type);
            errorConstructors[type.ordinal()] = errorConstructor.getFunctionObject();
            errorPrototypes[type.ordinal()] = errorConstructor.getPrototype();
        }
    }

    public final JSContext getContext() {
        return context;
    }

    public final DynamicObject lookupFunction(JSBuiltinsContainer container, String methodName) {
        Builtin builtin = Objects.requireNonNull(container.lookupFunctionByName(methodName), methodName);
        JSFunctionData functionData = builtin.createFunctionData(context);
        return JSFunction.create(this, functionData);
    }

    public final Accessor lookupAccessor(JSBuiltinsContainer container, Object key) {
        Pair<JSBuiltin, JSBuiltin> pair = container.lookupAccessorByKey(key);
        JSBuiltin getterBuiltin = pair.getLeft();
        JSBuiltin setterBulitin = pair.getRight();
        DynamicObject getterFunction = null;
        DynamicObject setterFunction = null;
        if (getterBuiltin != null) {
            JSFunctionData functionData = getterBuiltin.createFunctionData(context);
            getterFunction = JSFunction.create(this, functionData);
        }
        if (setterBulitin != null) {
            JSFunctionData functionData = setterBulitin.createFunctionData(context);
            setterFunction = JSFunction.create(this, functionData);
        }
        return new Accessor(getterFunction, setterFunction);
    }

    public static DynamicObject createObjectConstructor(JSRealm realm, DynamicObject objectPrototype) {
        JSContext context = realm.getContext();
        DynamicObject objectConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, JSOrdinary.CLASS_NAME);
        JSObjectUtil.putConstructorPrototypeProperty(context, objectConstructor, objectPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, objectConstructor, ObjectFunctionBuiltins.BUILTINS);
        if (context.isOptionNashornCompatibilityMode()) {
            JSObjectUtil.putFunctionsFromContainer(realm, objectConstructor, ObjectFunctionBuiltins.BUILTINS_NASHORN_COMPAT);
        }
        return objectConstructor;
    }

    public final DynamicObject getErrorConstructor(JSErrorType type) {
        return errorConstructors[type.ordinal()];
    }

    public final DynamicObject getErrorPrototype(JSErrorType type) {
        return errorPrototypes[type.ordinal()];
    }

    public final DynamicObject getGlobalObject() {
        return globalObject;
    }

    public final void setGlobalObject(DynamicObject global) {
        context.getGlobalObjectPristineAssumption().invalidate();
        this.globalObject = global;
        this.topScope = createTopScope();
    }

    private TopScopeObject createTopScope() {
        return new TopScopeObject(new Object[]{scriptEngineImportScope, new DynamicScopeWrapper(globalScope), globalObject});
    }

    public final void dispose() {
        this.globalObject = Undefined.instance;
        this.topScope = TopScopeObject.empty();
    }

    public final DynamicObject getObjectConstructor() {
        return objectConstructor;
    }

    public final DynamicObject getObjectPrototype() {
        return objectPrototype;
    }

    public final DynamicObject getFunctionConstructor() {
        return functionConstructor;
    }

    public final DynamicObject getFunctionPrototype() {
        return functionPrototype;
    }

    public final DynamicObject getArrayConstructor() {
        return arrayConstructor;
    }

    public final DynamicObject getArrayPrototype() {
        return arrayPrototype;
    }

    public final DynamicObject getBooleanConstructor() {
        return booleanConstructor;
    }

    public final DynamicObject getBooleanPrototype() {
        return booleanPrototype;
    }

    public final DynamicObject getNumberConstructor() {
        return numberConstructor;
    }

    public final DynamicObject getNumberPrototype() {
        return numberPrototype;
    }

    public final DynamicObject getBigIntConstructor() {
        return bigIntConstructor;
    }

    public final DynamicObject getBigIntPrototype() {
        return bigIntPrototype;
    }

    public final DynamicObject getStringConstructor() {
        return stringConstructor;
    }

    public final DynamicObject getStringPrototype() {
        return stringPrototype;
    }

    public final DynamicObject getRegExpConstructor() {
        return regExpConstructor;
    }

    public final DynamicObject getRegExpPrototype() {
        return regExpPrototype;
    }

    public final DynamicObject getCollatorConstructor() {
        return collatorConstructor;
    }

    public final DynamicObject getCollatorPrototype() {
        return collatorPrototype;
    }

    public final DynamicObject getNumberFormatConstructor() {
        return numberFormatConstructor;
    }

    public final DynamicObject getNumberFormatPrototype() {
        return numberFormatPrototype;
    }

    public final DynamicObject getPluralRulesConstructor() {
        return pluralRulesConstructor;
    }

    public final DynamicObject getPluralRulesPrototype() {
        return pluralRulesPrototype;
    }

    public final DynamicObject getListFormatConstructor() {
        return listFormatConstructor;
    }

    public final DynamicObject getListFormatPrototype() {
        return listFormatPrototype;
    }

    public final DynamicObject getRelativeTimeFormatConstructor() {
        return relativeTimeFormatConstructor;
    }

    public final DynamicObject getRelativeTimeFormatPrototype() {
        return relativeTimeFormatPrototype;
    }

    public final DynamicObject getDateTimeFormatConstructor() {
        return dateTimeFormatConstructor;
    }

    public final DynamicObject getDateTimeFormatPrototype() {
        return dateTimeFormatPrototype;
    }

    public final DynamicObject getDateConstructor() {
        return dateConstructor;
    }

    public final DynamicObject getDatePrototype() {
        return datePrototype;
    }

    public final DynamicObject getSegmenterConstructor() {
        return segmenterConstructor;
    }

    public final DynamicObject getSegmenterPrototype() {
        return segmenterPrototype;
    }

    public final DynamicObject getDisplayNamesConstructor() {
        return displayNamesConstructor;
    }

    public final DynamicObject getDisplayNamesPrototype() {
        return displayNamesPrototype;
    }

    public final DynamicObject getLocaleConstructor() {
        return localeConstructor;
    }

    public final DynamicObject getLocalePrototype() {
        return localePrototype;
    }

    public final DynamicObject getSymbolConstructor() {
        return symbolConstructor;
    }

    public final DynamicObject getSymbolPrototype() {
        return symbolPrototype;
    }

    public final DynamicObject getMapConstructor() {
        return mapConstructor;
    }

    public final DynamicObject getMapPrototype() {
        return mapPrototype;
    }

    public final DynamicObject getSetConstructor() {
        return setConstructor;
    }

    public final DynamicObject getSetPrototype() {
        return setPrototype;
    }

    public final DynamicObject getWeakRefConstructor() {
        return weakRefConstructor;
    }

    public final DynamicObject getWeakRefPrototype() {
        return weakRefPrototype;
    }

    public final DynamicObject getFinalizationRegistryConstructor() {
        return finalizationRegistryConstructor;
    }

    public final DynamicObject getFinalizationRegistryPrototype() {
        return finalizationRegistryPrototype;
    }

    public final DynamicObject getWeakMapConstructor() {
        return weakMapConstructor;
    }

    public final DynamicObject getWeakMapPrototype() {
        return weakMapPrototype;
    }

    public final DynamicObject getWeakSetConstructor() {
        return weakSetConstructor;
    }

    public final DynamicObject getWeakSetPrototype() {
        return weakSetPrototype;
    }

    public final Shape getInitialRegExpPrototypeShape() {
        return initialRegExpPrototypeShape;
    }

    public final DynamicObject getArrayBufferConstructor() {
        return arrayBufferConstructor;
    }

    public final DynamicObject getArrayBufferPrototype() {
        return arrayBufferPrototype;
    }

    public final DynamicObject getSharedArrayBufferConstructor() {
        assert context.isOptionSharedArrayBuffer();
        return sharedArrayBufferConstructor;
    }

    public final DynamicObject getSharedArrayBufferPrototype() {
        assert context.isOptionSharedArrayBuffer();
        return sharedArrayBufferPrototype;
    }

    public final DynamicObject getArrayBufferViewConstructor(TypedArrayFactory factory) {
        return typedArrayConstructors[factory.getFactoryIndex()];
    }

    public final DynamicObject getArrayBufferViewPrototype(TypedArrayFactory factory) {
        return typedArrayPrototypes[factory.getFactoryIndex()];
    }

    public final DynamicObject getDataViewConstructor() {
        return dataViewConstructor;
    }

    public final DynamicObject getDataViewPrototype() {
        return dataViewPrototype;
    }

    public final DynamicObject getTypedArrayConstructor() {
        return typedArrayConstructor;
    }

    public final DynamicObject getTypedArrayPrototype() {
        return typedArrayPrototype;
    }

    public final DynamicObject getRealmBuiltinObject() {
        return realmBuiltinObject;
    }

    public final DynamicObject getProxyConstructor() {
        return proxyConstructor;
    }

    public final DynamicObject getProxyPrototype() {
        return proxyPrototype;
    }

    public final DynamicObject getGeneratorFunctionConstructor() {
        return generatorFunctionConstructor;
    }

    public final DynamicObject getGeneratorFunctionPrototype() {
        return generatorFunctionPrototype;
    }

    public final DynamicObject getAsyncFunctionConstructor() {
        return asyncFunctionConstructor;
    }

    public final DynamicObject getAsyncFunctionPrototype() {
        return asyncFunctionPrototype;
    }

    public final DynamicObject getAsyncGeneratorFunctionConstructor() {
        return asyncGeneratorFunctionConstructor;
    }

    public final DynamicObject getAsyncGeneratorFunctionPrototype() {
        return asyncGeneratorFunctionPrototype;
    }

    public final DynamicObject getEnumerateIteratorPrototype() {
        return enumerateIteratorPrototype;
    }

    public final DynamicObject getForInIteratorPrototype() {
        return forInIteratorPrototype;
    }

    public final DynamicObject getGeneratorObjectPrototype() {
        return generatorObjectPrototype;
    }

    public final DynamicObject getAsyncGeneratorObjectPrototype() {
        return asyncGeneratorObjectPrototype;
    }

    public final DynamicObject getJavaImporterConstructor() {
        return javaImporterConstructor;
    }

    public final DynamicObject getJavaImporterPrototype() {
        return javaImporterPrototype;
    }

    public final DynamicObject getJavaPackageToPrimitiveFunction() {
        assert javaPackageToPrimitiveFunction != null;
        return javaPackageToPrimitiveFunction;
    }

    public final Map<Object, DynamicObject> getTemplateRegistry() {
        if (templateRegistry == null) {
            createTemplateRegistry();
        }
        return templateRegistry;
    }

    @TruffleBoundary
    private synchronized void createTemplateRegistry() {
        if (templateRegistry == null) {
            templateRegistry = new WeakHashMap<>();
        }
    }

    public final Object getEvalFunctionObject() {
        return evalFunctionObject;
    }

    public final Object getApplyFunctionObject() {
        return applyFunctionObject;
    }

    public final Object getCallFunctionObject() {
        return callFunctionObject;
    }

    public final Object getReflectApplyFunctionObject() {
        return reflectApplyFunctionObject;
    }

    public final Object getReflectConstructFunctionObject() {
        return reflectConstructFunctionObject;
    }

    public final Object getCommonJSRequireFunctionObject() {
        return commonJSRequireFunctionObject;
    }

    public final Object getJsonParseFunctionObject() {
        return jsonParseFunctionObject;
    }

    public final DynamicObject getPromiseAllFunctionObject() {
        return promiseAllFunctionObject;
    }

    private static void putProtoAccessorProperty(final JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject getProto = JSFunction.create(realm, context.protoGetterFunctionData);
        DynamicObject setProto = JSFunction.create(realm, context.protoSetterFunctionData);

        // ES6 draft annex, B.2.2 Additional Properties of the Object.prototype Object
        JSObjectUtil.putBuiltinAccessorProperty(realm.getObjectPrototype(), JSObject.PROTO, getProto, setProto);
    }

    public final DynamicObject getThrowerFunction() {
        assert throwerFunction != null;
        return throwerFunction;
    }

    public final Accessor getThrowerAccessor() {
        assert throwerAccessor != null;
        return throwerAccessor;
    }

    public DynamicObject getIteratorPrototype() {
        return iteratorPrototype;
    }

    public DynamicObject getAsyncIteratorPrototype() {
        return asyncIteratorPrototype;
    }

    public DynamicObject getAsyncFromSyncIteratorPrototype() {
        return asyncFromSyncIteratorPrototype;
    }

    public DynamicObject getArrayIteratorPrototype() {
        return arrayIteratorPrototype;
    }

    public DynamicObject getSetIteratorPrototype() {
        return setIteratorPrototype;
    }

    public DynamicObject getMapIteratorPrototype() {
        return mapIteratorPrototype;
    }

    public DynamicObject getStringIteratorPrototype() {
        return stringIteratorPrototype;
    }

    public DynamicObject getRegExpStringIteratorPrototype() {
        return regExpStringIteratorPrototype;
    }

    public DynamicObject getSegmentIteratorPrototype() {
        return segmentIteratorPrototype;
    }

    /**
     * This function is used whenever a function is required that throws a TypeError. It is used by
     * some of the builtins that provide accessor functions that should not be called (e.g., as a
     * method of deprecation). In the specification, this is often referred to as
     * "[[ThrowTypeError]] function Object (13.2.3)".
     *
     */
    private DynamicObject createThrowerFunction() {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject thrower = JSFunction.create(this, context.throwerFunctionData);
        JSObject.preventExtensions(thrower);
        JSObject.setIntegrityLevel(thrower, true);
        return thrower;
    }

    public DynamicObject getPromiseConstructor() {
        return promiseConstructor;
    }

    public DynamicObject getPromisePrototype() {
        return promisePrototype;
    }

    public final JSObjectFactory.RealmData getObjectFactories() {
        return objectFactories;
    }

    public void setupGlobals() {
        CompilerAsserts.neverPartOfCompilation("do not setup globals from compiled code");
        long time = context.getContextOptions().isProfileTime() ? System.nanoTime() : 0L;

        DynamicObject global = getGlobalObject();
        putGlobalProperty(JSOrdinary.CLASS_NAME, getObjectConstructor());
        putGlobalProperty(JSFunction.CLASS_NAME, getFunctionConstructor());
        putGlobalProperty(JSArray.CLASS_NAME, getArrayConstructor());
        putGlobalProperty(JSString.CLASS_NAME, getStringConstructor());
        putGlobalProperty(JSDate.CLASS_NAME, getDateConstructor());
        putGlobalProperty(JSNumber.CLASS_NAME, getNumberConstructor());
        putGlobalProperty(JSBoolean.CLASS_NAME, getBooleanConstructor());
        putGlobalProperty(JSRegExp.CLASS_NAME, getRegExpConstructor());
        putGlobalProperty(JSMath.CLASS_NAME, mathObject);
        putGlobalProperty(JSON.CLASS_NAME, JSON.create(this));

        JSObjectUtil.putDataProperty(context, global, JSRuntime.NAN_STRING, Double.NaN);
        JSObjectUtil.putDataProperty(context, global, JSRuntime.INFINITY_STRING, Double.POSITIVE_INFINITY);
        JSObjectUtil.putDataProperty(context, global, Undefined.NAME, Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(this, global, GlobalBuiltins.GLOBAL_FUNCTIONS);

        this.evalFunctionObject = JSObject.get(global, JSGlobal.EVAL_NAME);
        DynamicObject jsonBuiltin = (DynamicObject) JSObject.get(global, "JSON");
        this.jsonParseFunctionObject = JSObject.get(jsonBuiltin, "parse");

        boolean webassembly = context.getContextOptions().isWebAssembly();
        for (JSErrorType type : JSErrorType.errorTypes()) {
            switch (type) {
                case CompileError:
                case LinkError:
                case RuntimeError:
                    if (webassembly) {
                        JSObjectUtil.putDataProperty(context, webAssemblyObject, type.name(), getErrorConstructor(type), JSAttributes.getDefaultNotEnumerable());
                    }
                    break;
                case AggregateError:
                    if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2021) {
                        putGlobalProperty(type.name(), getErrorConstructor(type));
                    }
                    break;
                default:
                    putGlobalProperty(type.name(), getErrorConstructor(type));
                    break;
            }
        }

        putGlobalProperty(JSArrayBuffer.CLASS_NAME, getArrayBufferConstructor());
        for (TypedArrayFactory factory : TypedArray.factories(context)) {
            putGlobalProperty(factory.getName(), getArrayBufferViewConstructor(factory));
        }
        putGlobalProperty(JSDataView.CLASS_NAME, getDataViewConstructor());

        if (context.getContextOptions().isBigInt()) {
            putGlobalProperty(JSBigInt.CLASS_NAME, getBigIntConstructor());
        }

        if (context.isOptionNashornCompatibilityMode()) {
            initGlobalNashornExtensions();
            removeNashornIncompatibleBuiltins();
        }
        if (context.getContextOptions().isScriptEngineGlobalScopeImport()) {
            String builtin = "importScriptEngineGlobalBindings";
            JSObjectUtil.putDataProperty(context, getScriptEngineImportScope(), builtin,
                            lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, builtin), JSAttributes.notConfigurableNotEnumerableNotWritable());
        }
        if (context.getContextOptions().isPolyglotBuiltin() && (getEnv().isPolyglotEvalAllowed() || getEnv().isPolyglotBindingsAccessAllowed())) {
            setupPolyglot();
        }
        if (context.isOptionDebugBuiltin()) {
            putGlobalProperty(context.getContextOptions().getDebugPropertyName(), createDebugObject());
        }
        if (context.getContextOptions().isTest262Mode()) {
            putGlobalProperty(JSTest262.GLOBAL_PROPERTY_NAME, JSTest262.create(this));
        }
        if (context.getContextOptions().isTestV8Mode()) {
            putGlobalProperty(JSTestV8.CLASS_NAME, JSTestV8.create(this));
        }
        if (context.getContextOptions().isV8RealmBuiltin()) {
            initRealmBuiltinObject();
        }
        if (context.getEcmaScriptVersion() >= 6) {
            Object parseInt = JSObject.get(global, "parseInt");
            Object parseFloat = JSObject.get(global, "parseFloat");
            putProperty(getNumberConstructor(), "parseInt", parseInt);
            putProperty(getNumberConstructor(), "parseFloat", parseFloat);

            putGlobalProperty(JSMap.CLASS_NAME, getMapConstructor());
            putGlobalProperty(JSSet.CLASS_NAME, getSetConstructor());
            putGlobalProperty(JSWeakMap.CLASS_NAME, getWeakMapConstructor());
            putGlobalProperty(JSWeakSet.CLASS_NAME, getWeakSetConstructor());
            putGlobalProperty(JSSymbol.CLASS_NAME, getSymbolConstructor());
            setupPredefinedSymbols(getSymbolConstructor());

            DynamicObject reflectObject = createReflect();
            putGlobalProperty(REFLECT_CLASS_NAME, reflectObject);
            this.reflectApplyFunctionObject = JSObject.get(reflectObject, "apply");
            this.reflectConstructFunctionObject = JSObject.get(reflectObject, "construct");

            putGlobalProperty(JSProxy.CLASS_NAME, getProxyConstructor());
            putGlobalProperty(JSPromise.CLASS_NAME, getPromiseConstructor());
            this.promiseAllFunctionObject = (DynamicObject) JSObject.get(getPromiseConstructor(), "all");
        }

        if (context.isOptionSharedArrayBuffer()) {
            putGlobalProperty(SHARED_ARRAY_BUFFER_CLASS_NAME, getSharedArrayBufferConstructor());
        }
        if (context.isOptionAtomics()) {
            putGlobalProperty(ATOMICS_CLASS_NAME, createAtomics());
        }
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            putGlobalProperty("globalThis", global);
        }
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2021) {
            putGlobalProperty(JSWeakRef.CLASS_NAME, getWeakRefConstructor());
            putGlobalProperty(JSFinalizationRegistry.CLASS_NAME, getFinalizationRegistryConstructor());
        }
        if (context.getContextOptions().isGraalBuiltin()) {
            putGraalObject();
        }
        if (webassembly) {
            putGlobalProperty(JSWebAssembly.CLASS_NAME, webAssemblyObject);
            JSObjectUtil.putDataProperty(context, webAssemblyObject, JSFunction.getName(webAssemblyGlobalConstructor), webAssemblyGlobalConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(context, webAssemblyObject, JSFunction.getName(webAssemblyInstanceConstructor), webAssemblyInstanceConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(context, webAssemblyObject, JSFunction.getName(webAssemblyMemoryConstructor), webAssemblyMemoryConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(context, webAssemblyObject, JSFunction.getName(webAssemblyModuleConstructor), webAssemblyModuleConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(context, webAssemblyObject, JSFunction.getName(webAssemblyTableConstructor), webAssemblyTableConstructor, JSAttributes.getDefaultNotEnumerable());
        }
        if (context.getContextOptions().isOperatorOverloading()) {
            JSObjectUtil.putFunctionsFromContainer(this, global, OperatorsBuiltins.BUILTINS);
        }

        if (context.getContextOptions().isProfileTime()) {
            System.out.println("SetupGlobals: " + (System.nanoTime() - time) / 1000000);
        }
        if (context.isRecordAndTupleEnabled()) {
            putGlobalProperty(JSRecord.CLASS_NAME, getRecordConstructor());
            putGlobalProperty(JSTuple.CLASS_NAME, getTupleConstructor());
        }
    }

    private void initGlobalNashornExtensions() {
        assert getContext().isOptionNashornCompatibilityMode();
        putGlobalProperty(JSAdapter.CLASS_NAME, jsAdapterConstructor);
        putGlobalProperty("exit", lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "exit"));
        putGlobalProperty("quit", lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "quit"));
        DynamicObject parseToJSON = lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "parseToJSON");
        putGlobalProperty("parseToJSON", parseToJSON);
    }

    private void removeNashornIncompatibleBuiltins() {
        assert getContext().isOptionNashornCompatibilityMode();

        // Nashorn has no join method on TypedArrays
        JSObject.delete(typedArrayPrototype, "join");
    }

    private void addPrintGlobals() {
        if (context.getContextOptions().isPrint()) {
            putGlobalProperty("print", lookupFunction(GlobalBuiltins.GLOBAL_PRINT, "print"));
            putGlobalProperty("printErr", lookupFunction(GlobalBuiltins.GLOBAL_PRINT, "printErr"));
        }
    }

    @TruffleBoundary
    private void addCommonJSGlobals() {
        if (getContext().getContextOptions().isCommonJSRequire()) {
            String cwdOption = getContext().getContextOptions().getRequireCwd();
            TruffleFile cwdFile = getEnv().getPublicTruffleFile(cwdOption);
            try {
                if (cwdOption != null && !cwdFile.exists()) {
                    throw Errors.createError("Invalid CommonJS root folder: " + cwdOption);
                }
            } catch (SecurityException se) {
                throw Errors.createError("Access denied to CommonJS root folder: " + cwdOption);
            }
            // Define `require` and other globals in global scope.
            DynamicObject requireFunction = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, CommonJSRequireBuiltin.REQUIRE_PROPERTY_NAME);
            DynamicObject resolveFunction = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, CommonJSRequireBuiltin.RESOLVE_PROPERTY_NAME);
            JSObject.set(requireFunction, CommonJSRequireBuiltin.RESOLVE_PROPERTY_NAME, resolveFunction);
            putGlobalProperty(CommonJSRequireBuiltin.REQUIRE_PROPERTY_NAME, requireFunction);
            DynamicObject dirnameGetter = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, GlobalCommonJSRequireBuiltins.GlobalRequire.dirnameGetter.getName());
            JSObject.defineOwnProperty(getGlobalObject(), CommonJSRequireBuiltin.DIRNAME_VAR_NAME, PropertyDescriptor.createAccessor(dirnameGetter, Undefined.instance, false, false));
            DynamicObject filenameGetter = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, GlobalCommonJSRequireBuiltins.GlobalRequire.filenameGetter.getName());
            JSObject.defineOwnProperty(getGlobalObject(), CommonJSRequireBuiltin.FILENAME_VAR_NAME, PropertyDescriptor.createAccessor(filenameGetter, Undefined.instance, false, false));
            DynamicObject moduleGetter = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, GlobalCommonJSRequireBuiltins.GlobalRequire.globalModuleGetter.getName());
            JSObject.defineOwnProperty(getGlobalObject(), CommonJSRequireBuiltin.MODULE_PROPERTY_NAME, PropertyDescriptor.createAccessor(moduleGetter, Undefined.instance, false, false));
            DynamicObject exportsGetter = lookupFunction(GlobalBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS, GlobalCommonJSRequireBuiltins.GlobalRequire.globalExportsGetter.getName());
            JSObject.defineOwnProperty(getGlobalObject(), CommonJSRequireBuiltin.EXPORTS_PROPERTY_NAME, PropertyDescriptor.createAccessor(exportsGetter, Undefined.instance, false, false));
            this.commonJSRequireFunctionObject = requireFunction;
            // Load an (optional) bootstrap module. Can be used to define global properties (e.g.,
            // Node.js builtin mock-ups).
            String commonJSRequireGlobals = getContext().getContextOptions().getCommonJSRequireGlobals();
            if (commonJSRequireGlobals != null && !commonJSRequireGlobals.isEmpty()) {
                // `require()` the module. Result is discarded and exceptions are propagated.
                JSFunction.call(JSArguments.create(commonJSRequireFunctionObject, commonJSRequireFunctionObject, commonJSRequireGlobals));
            }
            // Configure an (optional) mapping from reserved module names (e.g., 'buffer') to
            // arbitrary Npm modules. Can be used to provide user-specific implementations of the JS
            // builtins.
            Map<String, String> commonJSRequireBuiltins = getContext().getContextOptions().getCommonJSRequireBuiltins();
            this.commonJSPreLoadedBuiltins = new HashMap<>();
            for (Map.Entry<String, String> entry : commonJSRequireBuiltins.entrySet()) {
                String builtinModule = entry.getValue();
                // ES Modules are handled by the default module loader if used.
                if (builtinModule.endsWith(MODULE_SOURCE_NAME_SUFFIX)) {
                    continue;
                }
                Object loadedModule = JSFunction.call(JSArguments.create(commonJSRequireFunctionObject, commonJSRequireFunctionObject, builtinModule));
                this.commonJSPreLoadedBuiltins.put(entry.getKey(), loadedModule);
            }
        }
    }

    private void addLoadGlobals() {
        if (getContext().getContextOptions().isLoad()) {
            putGlobalProperty("load", lookupFunction(GlobalBuiltins.GLOBAL_LOAD, "load"));
            putGlobalProperty("loadWithNewGlobal", lookupFunction(GlobalBuiltins.GLOBAL_LOAD, "loadWithNewGlobal"));
        }
    }

    private void addPerformanceGlobal() {
        if (context.getContextOptions().isPerformance()) {
            putGlobalProperty(PERFORMANCE_CLASS_NAME, preinitPerformanceObject != null ? preinitPerformanceObject : createPerformanceObject());
        }
    }

    /**
     * Add optional global properties. Used by initializeContext and patchContext.
     */
    public void addOptionalGlobals() {
        assert !getEnv().isPreInitialization();

        addGlobalGlobal();
        addShellGlobals();
        addScriptingGlobals();
        addIntlGlobal();
        addLoadGlobals();
        addConsoleGlobals();
        addPrintGlobals();
        addPerformanceGlobal();

        if (isJavaInteropEnabled()) {
            setupJavaInterop();
        }
        addCommonJSGlobals();
    }

    private void addGlobalGlobal() {
        if (getContext().getContextOptions().isGlobalProperty()) {
            putGlobalProperty("global", getGlobalObject());
        }
    }

    private void addShellGlobals() {
        if (getContext().getContextOptions().isShell()) {
            GlobalBuiltins.GLOBAL_SHELL.forEachBuiltin((Builtin builtin) -> {
                JSFunctionData functionData = builtin.createFunctionData(getContext());
                putGlobalProperty(builtin.getKey(), JSFunction.create(JSRealm.this, functionData), builtin.getAttributeFlags());
            });
        }
    }

    private void addIntlGlobal() {
        if (context.isOptionIntl402()) {
            putGlobalProperty(JSIntl.CLASS_NAME, preinitIntlObject != null ? preinitIntlObject : createIntlObject());
        }
    }

    private DynamicObject createIntlObject() {
        DynamicObject intlObject = JSIntl.create(this);
        DynamicObject collatorFn = getCollatorConstructor();
        DynamicObject numberFormatFn = getNumberFormatConstructor();
        DynamicObject dateTimeFormatFn = getDateTimeFormatConstructor();
        DynamicObject pluralRulesFn = getPluralRulesConstructor();
        DynamicObject listFormatFn = getListFormatConstructor();
        DynamicObject relativeTimeFormatFn = getRelativeTimeFormatConstructor();
        DynamicObject segmenterFn = getSegmenterConstructor();
        DynamicObject displayNamesFn = getDisplayNamesConstructor();
        DynamicObject localeFn = getLocaleConstructor();
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(collatorFn), collatorFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(numberFormatFn), numberFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(dateTimeFormatFn), dateTimeFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(pluralRulesFn), pluralRulesFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(listFormatFn), listFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(relativeTimeFormatFn), relativeTimeFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(segmenterFn), segmenterFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(displayNamesFn), displayNamesFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(context, intlObject, JSFunction.getName(localeFn), localeFn, JSAttributes.getDefaultNotEnumerable());
        return intlObject;
    }

    private void putGraalObject() {
        DynamicObject graalObject = JSOrdinary.createInit(this);
        int flags = JSAttributes.notConfigurableEnumerableNotWritable();
        int esVersion = getContext().getContextOptions().getEcmaScriptVersion();
        esVersion = (esVersion > JSConfig.ECMAScript6 ? esVersion + JSConfig.ECMAScriptNumberYearDelta : esVersion);
        JSObjectUtil.putDataProperty(context, graalObject, "language", JavaScriptLanguage.NAME, flags);
        assert GRAALVM_VERSION != null;
        JSObjectUtil.putDataProperty(context, graalObject, "versionGraalVM", GRAALVM_VERSION, flags);
        JSObjectUtil.putDataProperty(context, graalObject, "versionECMAScript", esVersion, flags);
        JSObjectUtil.putDataProperty(context, graalObject, "isGraalRuntime", JSFunction.create(this, isGraalRuntimeFunction(context)), flags);
        putGlobalProperty("Graal", graalObject);
    }

    private static JSFunctionData isGraalRuntimeFunction(JSContext context) {
        return context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.IsGraalRuntime, (c) -> {
            return JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    return isGraalRuntime();
                }

                @TruffleBoundary
                private boolean isGraalRuntime() {
                    return Truffle.getRuntime().getName().contains("Graal");
                }
            }), 0, "isGraalRuntime");
        });
    }

    /**
     * Convenience method for defining global data properties with default attributes.
     */
    private void putGlobalProperty(Object key, Object value) {
        putGlobalProperty(key, value, JSAttributes.getDefaultNotEnumerable());
    }

    private void putGlobalProperty(Object key, Object value, int attributes) {
        JSObjectUtil.putDataProperty(getContext(), getGlobalObject(), key, value, attributes);
    }

    private void putProperty(DynamicObject receiver, Object key, Object value) {
        JSObjectUtil.putDataProperty(getContext(), receiver, key, value, JSAttributes.getDefaultNotEnumerable());
    }

    private static void setupPredefinedSymbols(DynamicObject symbolFunction) {
        putSymbolProperty(symbolFunction, "hasInstance", Symbol.SYMBOL_HAS_INSTANCE);
        putSymbolProperty(symbolFunction, "isConcatSpreadable", Symbol.SYMBOL_IS_CONCAT_SPREADABLE);
        putSymbolProperty(symbolFunction, "iterator", Symbol.SYMBOL_ITERATOR);
        putSymbolProperty(symbolFunction, "asyncIterator", Symbol.SYMBOL_ASYNC_ITERATOR);
        putSymbolProperty(symbolFunction, "match", Symbol.SYMBOL_MATCH);
        putSymbolProperty(symbolFunction, "matchAll", Symbol.SYMBOL_MATCH_ALL);
        putSymbolProperty(symbolFunction, "replace", Symbol.SYMBOL_REPLACE);
        putSymbolProperty(symbolFunction, "search", Symbol.SYMBOL_SEARCH);
        putSymbolProperty(symbolFunction, "species", Symbol.SYMBOL_SPECIES);
        putSymbolProperty(symbolFunction, "split", Symbol.SYMBOL_SPLIT);
        putSymbolProperty(symbolFunction, "toStringTag", Symbol.SYMBOL_TO_STRING_TAG);
        putSymbolProperty(symbolFunction, "toPrimitive", Symbol.SYMBOL_TO_PRIMITIVE);
        putSymbolProperty(symbolFunction, "unscopables", Symbol.SYMBOL_UNSCOPABLES);
    }

    private static void putSymbolProperty(DynamicObject symbolFunction, String name, Symbol symbol) {
        DynamicObjectLibrary.getUncached().putConstant(symbolFunction, name, symbol, JSAttributes.notConfigurableNotEnumerableNotWritable());
    }

    /**
     * Is Java interop enabled in this Context.
     */
    public boolean isJavaInteropEnabled() {
        return getEnv() != null && getEnv().isHostLookupAllowed();
    }

    private void setupJavaInterop() {
        assert isJavaInteropEnabled();
        DynamicObject java = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putToStringTag(java, JAVA_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, java, JavaBuiltins.BUILTINS);
        if (context.isOptionNashornCompatibilityMode()) {
            JSObjectUtil.putFunctionsFromContainer(this, java, JavaBuiltins.BUILTINS_NASHORN_COMPAT);
        }
        putGlobalProperty(JAVA_CLASS_NAME, java);

        if (getEnv() != null && getEnv().isHostLookupAllowed()) {
            if (JSContextOptions.JAVA_PACKAGE_GLOBALS.getValue(getEnv().getOptions())) {
                javaPackageToPrimitiveFunction = JavaPackage.createToPrimitiveFunction(context, this);
                putGlobalProperty("Packages", JavaPackage.createInit(this, ""));
                putGlobalProperty("java", JavaPackage.createInit(this, "java"));
                putGlobalProperty("javafx", JavaPackage.createInit(this, "javafx"));
                putGlobalProperty("javax", JavaPackage.createInit(this, "javax"));
                putGlobalProperty("com", JavaPackage.createInit(this, "com"));
                putGlobalProperty("org", JavaPackage.createInit(this, "org"));
                putGlobalProperty("edu", JavaPackage.createInit(this, "edu"));

                // JavaImporter can only be used with Package objects.
                if (context.isOptionNashornCompatibilityMode()) {
                    putGlobalProperty(JavaImporter.CLASS_NAME, getJavaImporterConstructor());
                }
            }
        }
    }

    private void setupPolyglot() {
        DynamicObject polyglotObject = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putFunctionsFromContainer(this, polyglotObject, PolyglotBuiltins.BUILTINS);

        if (getContext().isOptionDebugBuiltin()) {
            JSObjectUtil.putFunctionsFromContainer(this, polyglotObject, PolyglotBuiltins.INTERNAL_BUILTINS);
        } else if (getContext().getContextOptions().isPolyglotEvalFile()) {
            // already loaded above when `debug-builtin` is true
            JSObjectUtil.putDataProperty(context, polyglotObject, "evalFile", lookupFunction(PolyglotBuiltins.INTERNAL_BUILTINS, "evalFile"), JSAttributes.getDefaultNotEnumerable());
        }
        putGlobalProperty(POLYGLOT_CLASS_NAME, polyglotObject);
    }

    private void addConsoleGlobals() {
        if (context.getContextOptions().isConsole()) {
            putGlobalProperty("console", preinitConsoleBuiltinObject != null ? preinitConsoleBuiltinObject : createConsoleObject());
        }
    }

    private DynamicObject createConsoleObject() {
        DynamicObject console = JSOrdinary.createInit(this);
        JSObjectUtil.putFunctionsFromContainer(this, console, ConsoleBuiltins.BUILTINS);
        return console;
    }

    private DynamicObject createPerformanceObject() {
        DynamicObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putFunctionsFromContainer(this, obj, PerformanceBuiltins.BUILTINS);
        return obj;
    }

    /**
     * Creates the %IteratorPrototype% object as specified in ES6 25.1.2.
     */
    private DynamicObject createIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putDataProperty(context, prototype, Symbol.SYMBOL_ITERATOR, createIteratorPrototypeSymbolIteratorFunction(this), JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    private static DynamicObject createIteratorPrototypeSymbolIteratorFunction(JSRealm realm) {
        return JSFunction.create(realm, JSFunctionData.createCallOnly(realm.getContext(), realm.getContext().getSpeciesGetterFunctionCallTarget(), 0, "[Symbol.iterator]"));
    }

    /**
     * Creates the %ArrayIteratorPrototype% object as specified in ES6 22.1.5.2.
     */
    private DynamicObject createArrayIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, ArrayIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSArray.ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the %SetIteratorPrototype% object.
     */
    private DynamicObject createSetIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, SetIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSSet.ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the %MapIteratorPrototype% object.
     */
    private DynamicObject createMapIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, MapIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSMap.ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the %StringIteratorPrototype% object.
     */
    private DynamicObject createStringIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, StringIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSString.ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the %RegExpStringIteratorPrototype% object.
     */
    private DynamicObject createRegExpStringIteratorPrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, RegExpStringIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSString.REGEXP_ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the prototype object of foreign iterables.
     */
    private DynamicObject createForeignIterablePrototype() {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, ForeignIterablePrototypeBuiltins.BUILTINS);
        return prototype;
    }

    public DynamicObject getArrayProtoValuesIterator() {
        return arrayProtoValuesIterator;
    }

    private DynamicObject createReflect() {
        DynamicObject obj = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putToStringTag(obj, REFLECT_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, ReflectBuiltins.BUILTINS);
        return obj;
    }

    private DynamicObject createAtomics() {
        DynamicObject obj = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putToStringTag(obj, ATOMICS_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, AtomicsBuiltins.BUILTINS);
        if (context.isWaitAsyncEnabled()) {
            JSObjectUtil.putFunctionsFromContainer(this, obj, AtomicsBuiltins.WAIT_ASYNC_BUILTIN);
        }
        return obj;
    }

    public final DynamicObject getCallSiteConstructor() {
        return callSiteConstructor;
    }

    public final DynamicObject getCallSitePrototype() {
        return callSitePrototype;
    }

    public final DynamicObject getGlobalScope() {
        return globalScope;
    }

    public DynamicObject getScriptEngineImportScope() {
        return scriptEngineImportScope;
    }

    public Object getTopScopeObject() {
        return topScope;
    }

    /**
     * Adds several objects to the global object, in case scripting mode is enabled (for Nashorn
     * compatibility). This includes an {@code $OPTIONS} property that exposes several options to
     * the script, an {@code $ARG} array with arguments to the script, an {@code $ENV} object with
     * environment variables, and an {@code $EXEC} function to execute external code.
     */
    private void addScriptingGlobals() {
        CompilerAsserts.neverPartOfCompilation();

        if (getContext().getParserOptions().isScripting()) {
            // $OPTIONS
            String timezone = getLocalTimeZoneId().getId();
            DynamicObject timezoneObj = JSOrdinary.create(context, this);
            JSObjectUtil.putDataProperty(context, timezoneObj, "ID", timezone, JSAttributes.configurableEnumerableWritable());

            DynamicObject optionsObj = JSOrdinary.create(context, this);
            JSObjectUtil.putDataProperty(context, optionsObj, "_timezone", timezoneObj, JSAttributes.configurableEnumerableWritable());
            JSObjectUtil.putDataProperty(context, optionsObj, "_scripting", true, JSAttributes.configurableEnumerableWritable());
            JSObjectUtil.putDataProperty(context, optionsObj, "_compile_only", false, JSAttributes.configurableEnumerableWritable());

            putGlobalProperty("$OPTIONS", optionsObj, JSAttributes.configurableNotEnumerableWritable());

            // $ARG
            DynamicObject arguments = JSArray.createConstant(context, getEnv().getApplicationArguments());

            putGlobalProperty("$ARG", arguments, JSAttributes.configurableNotEnumerableWritable());

            // $ENV
            DynamicObject envObj = JSOrdinary.create(context, this);
            Map<String, String> sysenv = getEnv().getEnvironment();
            for (Map.Entry<String, String> entry : sysenv.entrySet()) {
                JSObjectUtil.putDataProperty(context, envObj, entry.getKey(), entry.getValue(), JSAttributes.configurableEnumerableWritable());
            }

            putGlobalProperty("$ENV", envObj, JSAttributes.configurableNotEnumerableWritable());

            // $EXEC
            putGlobalProperty("$EXEC", lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "exec"));
            putGlobalProperty("readFully", lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "readFully"));
            putGlobalProperty("readLine", lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, "readLine"));

            // $OUT, $ERR, $EXIT
            putGlobalProperty("$EXIT", Undefined.instance);
            putGlobalProperty("$OUT", Undefined.instance);
            putGlobalProperty("$ERR", Undefined.instance);
        }
    }

    public void setRealmBuiltinObject(DynamicObject realmBuiltinObject) {
        if (this.realmBuiltinObject == null && realmBuiltinObject != null) {
            this.realmBuiltinObject = realmBuiltinObject;
            putGlobalProperty("Realm", realmBuiltinObject);
        }
    }

    public void initRealmBuiltinObject() {
        assert context.getContextOptions().isV8RealmBuiltin();
        setRealmBuiltinObject(createRealmBuiltinObject());
    }

    private DynamicObject createRealmBuiltinObject() {
        DynamicObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(obj, REALM_BUILTIN_CLASS_NAME);
        JSObjectUtil.putProxyProperty(obj, REALM_SHARED_NAME, REALM_SHARED_PROXY, JSAttributes.getDefault());
        JSObjectUtil.putFunctionsFromContainer(this, obj, RealmFunctionBuiltins.BUILTINS);
        return obj;
    }

    private DynamicObject createDebugObject() {
        DynamicObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(obj, DEBUG_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, DebugBuiltins.BUILTINS);
        return obj;
    }

    private void addStaticRegexResultProperties() {
        if (context.isOptionRegexpStaticResultInContextInit()) {
            if (context.isOptionNashornCompatibilityMode()) {
                putRegExpStaticPropertyAccessor(null, "input");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpMultiLine, "multiline");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, "lastMatch");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, "lastParen");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, "leftContext");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, "rightContext");
            } else {
                putRegExpStaticPropertyAccessor(null, "input");
                putRegExpStaticPropertyAccessor(null, "input", "$_");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, "lastMatch");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, "lastMatch", "$&");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, "lastParen");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, "lastParen", "$+");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, "leftContext");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, "leftContext", "$`");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, "rightContext");
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, "rightContext", "$'");
            }
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$1, "$1");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$2, "$2");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$3, "$3");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$4, "$4");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$5, "$5");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$6, "$6");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$7, "$7");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$8, "$8");
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$9, "$9");
        }
    }

    private void putRegExpStaticPropertyAccessor(BuiltinFunctionKey builtinKey, String getterName) {
        putRegExpStaticPropertyAccessor(builtinKey, getterName, getterName);
    }

    private void putRegExpStaticPropertyAccessor(BuiltinFunctionKey builtinKey, String getterName, String propertyName) {
        Pair<JSBuiltin, JSBuiltin> pair = RegExpBuiltins.BUILTINS.lookupAccessorByKey(getterName);
        JSBuiltin getterBuiltin = pair.getLeft();
        DynamicObject getter = JSFunction.create(this, getterBuiltin.createFunctionData(context));

        DynamicObject setter;
        JSBuiltin setterBuiltin = pair.getRight();
        if (setterBuiltin != null) {
            assert propertyName.equals("input") || propertyName.equals("$_");
            setter = JSFunction.create(this, setterBuiltin.createFunctionData(context));
        } else if (context.isOptionV8CompatibilityModeInContextInit()) {
            // set empty setter for V8 compatibility, see testv8/mjsunit/regress/regress-5566.js
            String setterName = "set " + getterName;
            JSFunctionData setterData = context.getOrCreateBuiltinFunctionData(builtinKey,
                            (c) -> JSFunctionData.createCallOnly(c, context.getEmptyFunctionCallTarget(), 1, setterName));
            setter = JSFunction.create(this, setterData);
        } else {
            setter = Undefined.instance;
        }

        // https://github.com/tc39/proposal-regexp-legacy-features#additional-properties-of-the-regexp-constructor
        int propertyAttributes = context.isOptionNashornCompatibilityMode() ? JSAttributes.notConfigurableEnumerableWritable() : JSAttributes.configurableNotEnumerableWritable();
        JSObjectUtil.putBuiltinAccessorProperty(regExpConstructor, propertyName, getter, setter, propertyAttributes);
    }

    public void setArguments(Object[] arguments) {
        JSObjectUtil.defineDataProperty(context, getGlobalObject(), ARGUMENTS_NAME, JSArray.createConstant(context, arguments),
                        context.isOptionV8CompatibilityModeInContextInit() ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable());
    }

    public final DynamicObject getJSAdapterConstructor() {
        return jsAdapterConstructor;
    }

    public final DynamicObject getJSAdapterPrototype() {
        return jsAdapterPrototype;
    }

    public final TruffleLanguage.Env getEnv() {
        return truffleLanguageEnv;
    }

    public boolean patchContext(TruffleLanguage.Env newEnv) {
        CompilerAsserts.neverPartOfCompilation();
        Objects.requireNonNull(newEnv, "New env cannot be null.");

        truffleLanguageEnv = newEnv;
        getContext().setAllocationReporter(newEnv);
        getContext().getContextOptions().setOptionValues(newEnv.getOptions());

        if (newEnv.out() != getOutputStream()) {
            setOutputWriter(null, newEnv.out());
        }
        if (newEnv.err() != getErrorStream()) {
            setErrorWriter(null, newEnv.err());
        }

        // During context pre-initialization, optional globals are not added to global
        // environment. During context-patching time, we are obliged to call addOptionalGlobals
        // to add any necessary globals.
        addOptionalGlobals();

        addArgumentsFromEnv(newEnv);

        // Reflect any changes to the timezone option.
        if (localTimeZoneId != null) {
            localTimeZoneId = getTimeZoneFromEnv();
        }
        initTimeOffsetAndRandom();

        // Patch the RegExp constructor's static result properties
        addStaticRegexResultProperties();

        return true;
    }

    public void initialize() {
        CompilerAsserts.neverPartOfCompilation();
        if (getEnv().isPreInitialization()) {
            preinitializeObjects();
            return;
        }

        addOptionalGlobals();

        addArgumentsFromEnv(getEnv());

        initTimeOffsetAndRandom();

        addStaticRegexResultProperties();
    }

    private void preinitializeObjects() {
        preinitIntlObject = createIntlObject();
        preinitConsoleBuiltinObject = createConsoleObject();
        preinitPerformanceObject = createPerformanceObject();
    }

    private void addArgumentsFromEnv(TruffleLanguage.Env newEnv) {
        String[] applicationArguments = newEnv.getApplicationArguments();
        if (context.getContextOptions().isGlobalArguments()) {
            setArguments(applicationArguments);
        }
    }

    @TruffleBoundary
    public JSRealm createChildRealm() {
        assert CREATING_CHILD_REALM.get() != Boolean.TRUE;
        CREATING_CHILD_REALM.set(Boolean.TRUE);
        try {
            TruffleContext nestedContext = getEnv().newContextBuilder().build();
            Object prev = nestedContext.enter(null);
            try {
                JSRealm childRealm = JavaScriptLanguage.getCurrentJSRealm();
                childRealm.agent = this.agent;
                childRealm.parentRealm = this;

                if (getContext().getContextOptions().isV8RealmBuiltin()) {
                    JSRealm topLevelRealm = this;
                    while (topLevelRealm.parentRealm != null) {
                        topLevelRealm = topLevelRealm.parentRealm;
                    }
                    topLevelRealm.addToRealmList(childRealm);
                }

                return childRealm;
            } finally {
                nestedContext.leave(null, prev);
            }
        } finally {
            CREATING_CHILD_REALM.set(Boolean.FALSE);
        }
    }

    public boolean isPreparingStackTrace() {
        return preparingStackTrace;
    }

    public void setPreparingStackTrace(boolean preparingStackTrace) {
        this.preparingStackTrace = preparingStackTrace;
    }

    public final TruffleContext getTruffleContext() {
        return getEnv().getContext();
    }

    public final Object getEmbedderData() {
        return embedderData;
    }

    public final void setEmbedderData(Object embedderData) {
        this.embedderData = embedderData;
    }

    public Object getStaticRegexResult(JSContext ctx, TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor) {
        CompilerAsserts.partialEvaluationConstant(ctx);
        assert ctx.isOptionRegexpStaticResult();
        if (staticRegexResultCompiledRegex != null && ctx.getRegExpStaticResultUnusedAssumption().isValid()) {
            // switch from lazy to eager static RegExp result
            ctx.getRegExpStaticResultUnusedAssumption().invalidate();
            staticRegexResult = compiledRegexAccessor.exec(staticRegexResultCompiledRegex, staticRegexResultOriginalInputString, staticRegexResultFromIndex);
        }
        if (staticRegexResult == null) {
            staticRegexResult = ctx.getTRegexEmptyResult();
        }
        return staticRegexResult;
    }

    /**
     * To allow virtualization of TRegex RegexResults, we want to avoid storing the last result
     * globally. Instead, we store the values needed to calculate the result on demand, under the
     * assumption that this non-standard feature is often not used at all.
     */
    public void setStaticRegexResult(JSContext ctx, Object compiledRegex, String input, long fromIndex, Object result) {
        CompilerAsserts.partialEvaluationConstant(ctx);
        assert ctx.isOptionRegexpStaticResult();
        staticRegexResultInvalidated = false;
        staticRegexResultCompiledRegex = compiledRegex;
        staticRegexResultInputString = input;
        staticRegexResultOriginalInputString = input;
        if (ctx.getRegExpStaticResultUnusedAssumption().isValid()) {
            staticRegexResultFromIndex = fromIndex;
        } else {
            assert TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(result, TRegexUtil.Props.RegexResult.IS_MATCH);
            staticRegexResult = result;
        }
    }

    public void invalidateStaticRegexResult() {
        staticRegexResultInvalidated = true;
    }

    public boolean isRegexResultInvalidated() {
        return staticRegexResultInvalidated;
    }

    public Object getStaticRegexResultCompiledRegex() {
        return staticRegexResultCompiledRegex;
    }

    public String getStaticRegexResultInputString() {
        return staticRegexResultInputString;
    }

    public void setStaticRegexResultInputString(String inputString) {
        staticRegexResultInputString = inputString;
    }

    public String getStaticRegexResultOriginalInputString() {
        return staticRegexResultOriginalInputString;
    }

    public OptionValues getOptions() {
        return getEnv().getOptions();
    }

    public final PrintWriter getOutputWriter() {
        return outputWriter;
    }

    /**
     * Returns the stream used by {@link #getOutputWriter}, or null if the stream is not available.
     *
     * Do not write to the stream directly, always use the {@link #getOutputWriter writer} instead.
     * Use this method only to check if the current writer is already writing to the stream you want
     * to use, in which case you can avoid creating a new {@link PrintWriter}.
     */
    public final OutputStream getOutputStream() {
        return outputStream;
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
    public final OutputStream getErrorStream() {
        return errorStream;
    }

    public final void setOutputWriter(Writer writer, OutputStream stream) {
        if (writer instanceof PrintWriterWrapper) {
            this.outputWriter.setFrom((PrintWriterWrapper) writer);
        } else {
            if (stream != null) {
                this.outputWriter.setDelegate(stream);
            } else {
                this.outputWriter.setDelegate(writer);
            }
        }
        this.outputStream = stream;
    }

    public final void setErrorWriter(Writer writer, OutputStream stream) {
        if (writer instanceof PrintWriterWrapper) {
            this.errorWriter.setFrom((PrintWriterWrapper) writer);
        } else {
            if (stream != null) {
                this.errorWriter.setDelegate(stream);
            } else {
                this.errorWriter.setDelegate(writer);
            }
        }
        this.errorStream = stream;
    }

    public long nanoTime() {
        return nanoTime(nanoToZeroTimeOffset);
    }

    public long nanoTime(long offset) {
        long ns = System.nanoTime() + offset;
        long resolution = getContext().getTimerResolution();
        if (resolution > 0) {
            return (ns / resolution) * resolution;
        } else {
            // fuzzy time
            long fuzz = random.nextLong(NANOSECONDS_PER_MILLISECOND) + 1;
            ns = ns - ns % fuzz;
            long last = lastFuzzyTime;
            if (ns > last) {
                lastFuzzyTime = ns;
                return ns;
            } else {
                return last;
            }
        }
    }

    public long currentTimeMillis() {
        return nanoTime(nanoToCurrentTimeOffset) / NANOSECONDS_PER_MILLISECOND;
    }

    public JSConsoleUtil getConsoleUtil() {
        return consoleUtil;
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
            if (context.getContextOptions().isCommonJSRequire()) {
                moduleLoader = NpmCompatibleESModuleLoader.create(this);
            } else {
                moduleLoader = DefaultESModuleLoader.create(this);
            }
        }
    }

    public final JSAgent getAgent() {
        assert agent != null;
        return agent;
    }

    public void setAgent(JSAgent newAgent) {
        assert newAgent != null : "Cannot set a null agent!";
        CompilerAsserts.neverPartOfCompilation("Assigning agent to context in compiled code");
        this.agent = newAgent;
    }

    public ZoneId getLocalTimeZoneId() {
        ZoneId id = localTimeZoneId;
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, id == null)) {
            if (CompilerDirectives.isPartialEvaluationConstant(id)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
            }
            id = getTimeZoneFromEnv();
            localTimeZoneId = id;
        }
        return id;
    }

    @TruffleBoundary
    private ZoneId getTimeZoneFromEnv() {
        OptionValues options = getEnv().getOptions();
        if (JSContextOptions.TIME_ZONE.hasBeenSet(options)) {
            try {
                return ZoneId.of(JSContextOptions.TIME_ZONE.getValue(options));
            } catch (DateTimeException e) {
                // The time zone ID should have already been validated by the OptionType.
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
        return getEnv().getTimeZone();
    }

    private void initTimeOffsetAndRandom() {
        assert !getEnv().isPreInitialization();

        random = new SplittableRandom();
        nanoToZeroTimeOffset = -System.nanoTime();
        nanoToCurrentTimeOffset = System.currentTimeMillis() * NANOSECONDS_PER_MILLISECOND + nanoToZeroTimeOffset;
        lastFuzzyTime = Long.MIN_VALUE;
    }

    public final SplittableRandom getRandom() {
        return random;
    }

    public JSRealm getParent() {
        return parentRealm;
    }

    public JavaScriptBaseNode getCallNode() {
        return callNode;
    }

    public void setCallNode(JavaScriptBaseNode callNode) {
        this.callNode = callNode;
    }

    void initRealmList() {
        CompilerAsserts.neverPartOfCompilation();
        realmList = new ArrayList<>();
    }

    synchronized void addToRealmList(JSRealm newRealm) {
        CompilerAsserts.neverPartOfCompilation();
        assert !realmList.contains(newRealm);
        realmList.add(newRealm);
    }

    public synchronized JSRealm getFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        return (0 <= idx && idx < realmList.size()) ? realmList.get(idx) : null;
    }

    public synchronized int getIndexFromRealmList(JSRealm rlm) {
        CompilerAsserts.neverPartOfCompilation();
        return realmList.indexOf(rlm);
    }

    public synchronized void removeFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        realmList.set(idx, null);
    }

    public JSRealm getCurrentV8Realm() {
        return v8RealmCurrent;
    }

    public void setCurrentV8Realm(JSRealm realm) {
        v8RealmCurrent = realm;
    }

    private static final String REALM_SHARED_NAME = "shared";
    private static final PropertyProxy REALM_SHARED_PROXY = new RealmSharedPropertyProxy();

    private static class RealmSharedPropertyProxy implements PropertyProxy {
        @Override
        public Object get(DynamicObject store) {
            JSContext context = JSObject.getJSContext(store);
            return topLevelRealm(context).v8RealmShared;
        }

        @Override
        public boolean set(DynamicObject store, Object value) {
            JSContext context = JSObject.getJSContext(store);
            topLevelRealm(context).v8RealmShared = value;
            return true;
        }

        private static JSRealm topLevelRealm(JSContext context) {
            JSRealm realm = context.getRealm();
            while (realm.getParent() != null) {
                realm = realm.getParent();
            }
            return realm;
        }
    }

    public boolean joinStackPush(Object o, BranchProfile growProfile) {
        InteropLibrary interop = (o instanceof JSObject) ? null : InteropLibrary.getFactory().getUncached(o);
        for (int i = 0; i < joinStack.size(); i++) {
            Object element = joinStack.get(i);
            if ((interop == null) ? (o == element) : interop.isIdentical(o, element, InteropLibrary.getFactory().getUncached(element))) {
                return false;
            }
        }
        joinStack.add(o, growProfile);
        return true;
    }

    public void joinStackPop() {
        joinStack.pop();
    }

    public final Map<TruffleFile, DynamicObject> getCommonJSRequireCache() {
        assert context.getContextOptions().isCommonJSRequire();
        return commonJSRequireCache;
    }

    private boolean isWasmAvailable() {
        return truffleLanguageEnv.isPolyglotBindingsAccessAllowed() && truffleLanguageEnv.getInternalLanguages().get("wasm") != null;
    }

    public Object getWASMCompileFunction() {
        return wasmCompileFunction;
    }

    public Object getWASMInstantiateFunction() {
        return wasmInstantiateFunction;
    }

    public Object getWASMValidateFunction() {
        return wasmValidateFunction;
    }

    public Object getWASMModuleExportsFunction() {
        return wasmModuleExportsFunction;
    }

    public Object getWASMModuleImportsFunction() {
        return wasmModuleImportsFunction;
    }

    public Object getWASMModuleCustomSectionsFunction() {
        return wasmModuleCustomSectionsFunction;
    }

    public Object getWASMGlobalConstructor() {
        return wasmGlobalConstructor;
    }

    public Object getWASMMemoryConstructor() {
        return wasmMemoryConstructor;
    }

    public Object getWASMTableConstructor() {
        return wasmTableConstructor;
    }

    public DynamicObject getWebAssemblyModulePrototype() {
        return webAssemblyModulePrototype;
    }

    public DynamicObject getWebAssemblyInstancePrototype() {
        return webAssemblyInstancePrototype;
    }

    public DynamicObject getWebAssemblyMemoryPrototype() {
        return webAssemblyMemoryPrototype;
    }

    public DynamicObject getWebAssemblyTablePrototype() {
        return webAssemblyTablePrototype;
    }

    public DynamicObject getWebAssemblyGlobalPrototype() {
        return webAssemblyGlobalPrototype;
    }

    public DynamicObject getForeignIterablePrototype() {
        return foreignIterablePrototype;
    }

    public DynamicObject getRecordConstructor() {
        return recordConstructor;
    }

    public DynamicObject getRecordPrototype() {
        return recordPrototype;
    }

    public DynamicObject getTupleConstructor() {
        return tupleConstructor;
    }

    public DynamicObject getTuplePrototype() {
        return tuplePrototype;
    }
}
