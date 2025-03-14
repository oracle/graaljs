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
package com.oracle.truffle.js.runtime;

import static com.oracle.truffle.api.CompilerDirectives.SLOWPATH_PROBABILITY;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

import org.graalvm.collections.Pair;
import org.graalvm.home.HomeFinder;
import org.graalvm.options.OptionValues;
import org.graalvm.shadowed.com.ibm.icu.text.DateFormat;
import org.graalvm.shadowed.com.ibm.icu.text.SimpleDateFormat;
import org.graalvm.shadowed.com.ibm.icu.text.TimeZoneFormat;
import org.graalvm.shadowed.com.ibm.icu.text.TimeZoneNames;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;
import org.graalvm.shadowed.com.ibm.icu.util.GregorianCalendar;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.AbstractModuleSourcePrototype;
import com.oracle.truffle.js.builtins.AsyncIteratorHelperPrototypeBuiltins;
import com.oracle.truffle.js.builtins.AtomicsBuiltins;
import com.oracle.truffle.js.builtins.ConsoleBuiltins;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltins;
import com.oracle.truffle.js.builtins.DebugBuiltins;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.builtins.IteratorHelperPrototypeBuiltins;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.JavaBuiltins;
import com.oracle.truffle.js.builtins.MLEBuiltins;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltins;
import com.oracle.truffle.js.builtins.OperatorsBuiltins;
import com.oracle.truffle.js.builtins.PerformanceBuiltins;
import com.oracle.truffle.js.builtins.PolyglotBuiltins;
import com.oracle.truffle.js.builtins.ReflectBuiltins;
import com.oracle.truffle.js.builtins.RegExpBuiltins;
import com.oracle.truffle.js.builtins.RegExpStringIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.commonjs.GlobalCommonJSRequireBuiltins;
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader;
import com.oracle.truffle.js.builtins.foreign.ForeignIterablePrototypeBuiltins;
import com.oracle.truffle.js.builtins.foreign.ForeignIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.json.JSON;
import com.oracle.truffle.js.builtins.temporal.TemporalNowBuiltins;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltins;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltins;
import com.oracle.truffle.js.builtins.web.JSTextDecoder;
import com.oracle.truffle.js.builtins.web.JSTextEncoder;
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
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapIterator;
import com.oracle.truffle.js.runtime.builtins.JSMath;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototypeObject;
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
import com.oracle.truffle.js.runtime.builtins.JSTest262;
import com.oracle.truffle.js.runtime.builtins.JSTestV8;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.JSWorker;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContext;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshot;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextVariable;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssembly;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryGrowCallback;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryNotifyCallback;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryWaitCallback;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.interop.DynamicScopeWrapper;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
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
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LRUCache;
import com.oracle.truffle.js.runtime.util.PrintWriterWrapper;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StableContextOptionValue;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexCompiledRegexAccessor;
import com.oracle.truffle.js.runtime.util.TemporalConstants;

/**
 * Container for JavaScript globals (i.e. an ECMAScript 6 Realm object).
 */
public class JSRealm {

    public static final TruffleString POLYGLOT_CLASS_NAME = Strings.constant("Polyglot");
    // used for non-public properties of Polyglot
    public static final TruffleString REFLECT_CLASS_NAME = Strings.constant("Reflect");
    public static final TruffleString ATOMICS_CLASS_NAME = Strings.constant("Atomics");
    public static final TruffleString REALM_BUILTIN_CLASS_NAME = Strings.constant("Realm");
    public static final TruffleString ARGUMENTS_NAME = Strings.ARGUMENTS;
    public static final TruffleString JAVA_CLASS_NAME = Strings.constant("Java");
    public static final TruffleString JAVA_CLASS_NAME_NASHORN_COMPAT = Strings.constant("JavaNashornCompat");
    public static final TruffleString PERFORMANCE_CLASS_NAME = Strings.constant("performance");
    public static final TruffleString DEBUG_CLASS_NAME = Strings.constant("Debug");
    public static final TruffleString CONSOLE_CLASS_NAME = Strings.constant("Console");
    public static final TruffleString SYMBOL_ITERATOR_NAME = Strings.constant("[Symbol.iterator]");
    public static final TruffleString MLE_CLASS_NAME = Strings.constant("MLE");

    private static final TruffleString GRAALVM_VERSION = Strings.fromJavaString(HomeFinder.getInstance().getVersion());

    private static final ContextReference<JSRealm> REFERENCE = ContextReference.create(JavaScriptLanguage.class);

    private final JSContext context;
    private final JSContextOptions contextOptions;

    @CompilationFinal private JSDynamicObject globalObject;

    private final JSFunctionObject objectConstructor;
    private final JSObjectPrototypeObject objectPrototype;
    private final JSFunctionObject functionConstructor;
    private final JSFunctionObject functionPrototype;

    private final JSFunctionObject arrayConstructor;
    private final JSArrayObject arrayPrototype;
    private final JSFunctionObject booleanConstructor;
    private final JSDynamicObject booleanPrototype;
    private final JSFunctionObject numberConstructor;
    private final JSDynamicObject numberPrototype;
    private final JSFunctionObject bigIntConstructor;
    private final JSDynamicObject bigIntPrototype;
    private final JSFunctionObject stringConstructor;
    private final JSDynamicObject stringPrototype;
    private final JSFunctionObject regExpConstructor;
    private final JSDynamicObject regExpPrototype;
    private final JSFunctionObject collatorConstructor;
    private final JSDynamicObject collatorPrototype;
    private final JSFunctionObject numberFormatConstructor;
    private final JSDynamicObject numberFormatPrototype;
    private final JSFunctionObject pluralRulesConstructor;
    private final JSDynamicObject pluralRulesPrototype;
    private final JSFunctionObject listFormatConstructor;
    private final JSDynamicObject listFormatPrototype;
    private final JSFunctionObject dateTimeFormatConstructor;
    private final JSDynamicObject dateTimeFormatPrototype;
    private final JSFunctionObject relativeTimeFormatConstructor;
    private final JSDynamicObject relativeTimeFormatPrototype;
    private final JSFunctionObject segmenterConstructor;
    private final JSDynamicObject segmenterPrototype;
    private final JSFunctionObject displayNamesConstructor;
    private final JSDynamicObject displayNamesPrototype;
    private final JSFunctionObject localeConstructor;
    private final JSDynamicObject localePrototype;
    private final JSFunctionObject dateConstructor;
    private final JSDynamicObject datePrototype;
    @CompilationFinal(dimensions = 1) private final JSFunctionObject[] errorConstructors;
    @CompilationFinal(dimensions = 1) private final JSDynamicObject[] errorPrototypes;
    private final JSFunctionObject callSiteConstructor;
    private final JSDynamicObject callSitePrototype;

    private final JSDynamicObject foreignArrayPrototype;
    private final JSDynamicObject foreignDatePrototype;
    private final JSDynamicObject foreignMapPrototype;
    private final JSDynamicObject foreignStringPrototype;
    private final JSDynamicObject foreignNumberPrototype;
    private final JSDynamicObject foreignBooleanPrototype;
    private final JSDynamicObject foreignErrorPrototype;
    private final JSDynamicObject foreignFunctionPrototype;
    private final JSDynamicObject foreignObjectPrototype;

    private final Shape initialRegExpPrototypeShape;
    private final JSObjectFactory.RealmData objectFactories;

    private final JSFunctionObject temporalPlainTimeConstructor;
    private final JSDynamicObject temporalPlainTimePrototype;
    private final JSFunctionObject temporalPlainDateConstructor;
    private final JSDynamicObject temporalPlainDatePrototype;
    private final JSFunctionObject temporalPlainDateTimeConstructor;
    private final JSDynamicObject temporalPlainDateTimePrototype;
    private final JSFunctionObject temporalDurationConstructor;
    private final JSDynamicObject temporalDurationPrototype;
    private final JSFunctionObject temporalPlainYearMonthConstructor;
    private final JSDynamicObject temporalPlainYearMonthPrototype;
    private final JSFunctionObject temporalPlainMonthDayConstructor;
    private final JSDynamicObject temporalPlainMonthDayPrototype;
    private final JSFunctionObject temporalInstantConstructor;
    private final JSDynamicObject temporalInstantPrototype;
    private final JSFunctionObject temporalZonedDateTimeConstructor;
    private final JSDynamicObject temporalZonedDateTimePrototype;

    // ES6:
    private final JSFunctionObject symbolConstructor;
    private final JSDynamicObject symbolPrototype;
    private final JSFunctionObject mapConstructor;
    private final JSDynamicObject mapPrototype;
    private final JSFunctionObject setConstructor;
    private final JSDynamicObject setPrototype;
    private final JSFunctionObject weakRefConstructor;
    private final JSDynamicObject weakRefPrototype;
    private final JSFunctionObject weakMapConstructor;
    private final JSDynamicObject weakMapPrototype;
    private final JSFunctionObject weakSetConstructor;
    private final JSDynamicObject weakSetPrototype;

    private final JSDynamicObject mathObject;
    private JSDynamicObject realmBuiltinObject;
    private Object evalFunctionObject;
    private final Object applyFunctionObject;
    private final Object callFunctionObject;
    private Object reflectApplyFunctionObject;
    private Object reflectConstructFunctionObject;
    private Object commonJSRequireFunctionObject;
    private Object jsonParseFunctionObject;

    private final JSFunctionObject arrayBufferConstructor;
    private final JSDynamicObject arrayBufferPrototype;
    private final JSFunctionObject sharedArrayBufferConstructor;
    private final JSDynamicObject sharedArrayBufferPrototype;

    @CompilationFinal(dimensions = 1) private final JSFunctionObject[] typedArrayConstructors;
    @CompilationFinal(dimensions = 1) private final JSDynamicObject[] typedArrayPrototypes;
    private final JSFunctionObject dataViewConstructor;
    private final JSDynamicObject dataViewPrototype;
    private final JSFunctionObject jsAdapterConstructor;
    private final JSDynamicObject jsAdapterPrototype;
    private final JSFunctionObject javaImporterConstructor;
    private final JSDynamicObject javaImporterPrototype;
    private final JSFunctionObject proxyConstructor;
    private final JSDynamicObject proxyPrototype;
    private final JSFunctionObject finalizationRegistryConstructor;
    private final JSDynamicObject finalizationRegistryPrototype;

    private final JSFunctionObject iteratorConstructor;
    private final JSDynamicObject iteratorPrototype;
    private final JSDynamicObject wrapForIteratorPrototype;
    private final JSDynamicObject wrapForAsyncIteratorPrototype;
    private final JSDynamicObject arrayIteratorPrototype;
    private final JSDynamicObject setIteratorPrototype;
    private final JSDynamicObject mapIteratorPrototype;
    private final JSDynamicObject asyncIteratorHelperPrototype;
    private final JSDynamicObject iteratorHelperPrototype;
    private final JSDynamicObject segmentsPrototype;
    private final JSDynamicObject segmentIteratorPrototype;
    private final JSDynamicObject stringIteratorPrototype;
    private final JSDynamicObject regExpStringIteratorPrototype;
    private final JSDynamicObject enumerateIteratorPrototype;
    private final JSDynamicObject forInIteratorPrototype;

    private final JSFunctionObject generatorFunctionConstructor;
    private final JSDynamicObject generatorFunctionPrototype;
    private final JSDynamicObject generatorObjectPrototype;

    private final JSFunctionObject asyncFunctionConstructor;
    private final JSDynamicObject asyncFunctionPrototype;

    private final JSFunctionObject asyncIteratorContructor;
    private final JSDynamicObject asyncIteratorPrototype;
    private final JSDynamicObject asyncFromSyncIteratorPrototype;
    private final JSDynamicObject asyncGeneratorObjectPrototype;
    private final JSFunctionObject asyncGeneratorFunctionConstructor;
    private final JSDynamicObject asyncGeneratorFunctionPrototype;

    private final JSFunctionObject throwTypeErrorFunction;
    private final Accessor throwerAccessor;

    private final JSFunctionObject promiseConstructor;
    private final JSDynamicObject promisePrototype;
    /** Promise.all function object, null in ES5 mode. */
    private JSFunctionObject promiseAllFunctionObject;
    private Object unhandledPromiseRejectionHandler;

    private final JSDynamicObject ordinaryHasInstanceFunction;

    @CompilationFinal private JSDynamicObject javaPackageToPrimitiveFunction;

    private final JSDynamicObject arrayProtoValuesIterator;
    @CompilationFinal private JSFunctionObject typedArrayConstructor;
    @CompilationFinal private JSDynamicObject typedArrayPrototype;

    private JSDynamicObject preinitIntlObject;
    private JSDynamicObject preinitConsoleBuiltinObject;
    private JSDynamicObject preinitPerformanceObject;

    private volatile Map<Object, JSArrayObject> templateRegistry;
    private volatile Map<Object, JSArrayObject> dedentMap;

    private final JSDynamicObject globalScope;

    private final JSDynamicObject scriptEngineImportScope;

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
    private TruffleString staticRegexResultInputString = Strings.EMPTY_STRING;
    private Object staticRegexResultCompiledRegex;
    private boolean staticRegexResultInvalidated;
    private long staticRegexResultFromIndex;
    private TruffleString staticRegexResultOriginalInputString;

    private final JSFunctionObject abstractModuleSourceConstructor;
    private final JSDynamicObject abstractModuleSourcePrototype;

    /** WebAssembly support. */
    private final Object wasmTableAlloc;
    private final Object wasmTableGrow;
    private final Object wasmTableRead;
    private final Object wasmTableWrite;
    private final Object wasmTableLength;
    private final Object wasmFuncType;
    private final Object wasmIsFunc;
    private final Object wasmMemAlloc;
    private final Object wasmMemGrow;
    private final Object wasmMemAsByteBuffer;
    private final Object wasmGlobalAlloc;
    private final Object wasmGlobalRead;
    private final Object wasmGlobalWrite;
    private final Object wasmModuleInstantiate;
    private final Object wasmModuleExports;
    private final Object wasmModuleImports;
    private final Object wasmCustomSections;
    private final Object wasmInstanceExport;
    private final Object wasmEmbedderDataGet;
    private final Object wasmEmbedderDataSet;
    private final Object wasmRefNull;

    private final JSDynamicObject webAssemblyObject;
    private final JSFunctionObject webAssemblyGlobalConstructor;
    private final JSDynamicObject webAssemblyGlobalPrototype;
    private final JSFunctionObject webAssemblyInstanceConstructor;
    private final JSDynamicObject webAssemblyInstancePrototype;
    private final JSFunctionObject webAssemblyMemoryConstructor;
    private final JSDynamicObject webAssemblyMemoryPrototype;
    private final JSFunctionObject webAssemblyModuleConstructor;
    private final JSDynamicObject webAssemblyModulePrototype;
    private final JSFunctionObject webAssemblyTableConstructor;
    private final JSDynamicObject webAssemblyTablePrototype;

    private final JSFunctionObject shadowRealmConstructor;
    private final JSDynamicObject shadowRealmPrototype;

    private final JSFunctionObject workerConstructor;
    private final JSDynamicObject workerPrototype;

    private final JSFunctionObject asyncContextSnapshotConstructor;
    private final JSDynamicObject asyncContextSnapshotPrototype;
    private final JSFunctionObject asyncContextVariableConstructor;
    private final JSDynamicObject asyncContextVariablePrototype;

    /** Foreign object prototypes. */
    private final JSDynamicObject foreignIterablePrototype;
    private final JSDynamicObject foreignIteratorPrototype;

    private final JSFunctionObject textDecoderConstructor;
    private final JSDynamicObject textDecoderPrototype;
    private final JSFunctionObject textEncoderConstructor;
    private final JSDynamicObject textEncoderPrototype;

    /**
     * Local time zone ID. Initialized lazily. May be reinitialized by {@link #setLocalTimeZone}.
     */
    private ZoneId localTimeZoneId;
    private TimeZone localTimeZone;

    // local time zone independent formats; initialized once
    @CompilationFinal private DateFormat jsDateFormat;
    @CompilationFinal private DateFormat jsDateFormatBeforeYear0;
    @CompilationFinal private DateFormat jsDateFormatAfterYear9999;
    @CompilationFinal private DateFormat jsDateFormatISO;
    // local time zone dependent formats; may be reset multiple times
    private DateFormat jsShortDateFormat;
    private DateFormat jsShortDateLocalFormat;
    private DateFormat jsShortTimeFormat;
    private DateFormat jsShortTimeLocalFormat;
    private DateFormat jsDateToStringFormat;

    public static final long NANOSECONDS_PER_MILLISECOND = 1000000;
    public static final long NANOSECONDS_PER_SECOND = 1000 * NANOSECONDS_PER_MILLISECOND;
    private SplittableRandom random;
    private long nanoToZeroTimeOffset;
    private long lastFuzzyTime = Long.MIN_VALUE;

    private final Charset charset;
    private PrintWriterWrapper outputWriter;
    private PrintWriterWrapper errorWriter;

    private final JSConsoleUtil consoleUtil;
    private JSModuleLoader moduleLoader;
    private long lastAsyncEvaluationOrder;

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
    private final JSRealm parentRealm;

    /**
     * Currently active realm in this context.
     */
    private JSRealm currentRealm;

    /**
     * Current realm (as returned by {@code Realm.current()} V8 built-in). Not always the same as
     * {@link #currentRealm}.
     */
    private JSRealm v8RealmCurrent = this;

    /**
     * Value shared across V8 realms ({@code Realm.shared}).
     */
    Object v8RealmShared = Undefined.instance;

    /**
     * Used to the pass call site source location for caller sensitive built-in functions.
     */
    private JavaScriptBaseNode callNode;

    /**
     * Per-realm CommonJs `require` cache.
     */
    private final Map<TruffleFile, JSDynamicObject> commonJSRequireCache;

    /**
     * Stack of receivers of (Typed)Array.prototype.join. Used to avoid cyclic calls.
     */
    private final SimpleArrayList<Object> joinStack = new SimpleArrayList<>();

    /**
     * Cache of least recently compiled compiled regular expressions.
     */
    private Map<Source, Object> compiledRegexCache;

    /**
     * Private MLE-only custom Path resolution callback for ESM.
     */
    private Object customEsmPathMappingCallback;

    /**
     * Helper field for PromiseHook.TYPE_INIT event (stores the parent promise).
     */
    private JSDynamicObject parentPromise;

    /** 0 = Number, 1 = BigInt, 2 = String. */
    private int operatorCounter = 3;

    @SuppressWarnings("this-escape")
    protected JSRealm(JSContext context, TruffleLanguage.Env env, JSRealm parentRealm) {
        this.context = context;
        this.contextOptions = JSContextOptions.fromOptionValues(env.getSandboxPolicy(), env.getOptions());
        this.truffleLanguageEnv = env;

        if (!env.isPreInitialization()) {
            context.updateStableOptions(contextOptions, StableContextOptionValue.UpdateKind.UPDATE);
        }

        this.parentRealm = parentRealm;
        if (parentRealm == null) {
            // top-level realm
            this.currentRealm = this;
        } else {
            this.currentRealm = null;
            this.agent = parentRealm.agent;
        }

        // need to build Function and Function.proto in a weird order to avoid circular dependencies
        this.objectPrototype = JSObjectPrototype.create(context);

        this.functionPrototype = JSFunction.createFunctionPrototype(this, objectPrototype);

        this.objectFactories = context.newObjectFactoryRealmData();

        this.throwTypeErrorFunction = createThrowTypeErrorFunction(false);
        JSFunctionObject throwerFunction = createThrowTypeErrorFunction(true);
        this.throwerAccessor = new Accessor(throwerFunction, throwerFunction);

        if (context.isOptionAnnexB()) {
            putProtoAccessorProperty(this);
        }

        this.globalObject = JSGlobal.create(this, objectPrototype);
        this.globalScope = JSGlobal.createGlobalScope(context);
        if (context.getLanguageOptions().scriptEngineGlobalScopeImport()) {
            this.scriptEngineImportScope = JSOrdinary.createWithNullPrototypeInit(context);
        } else {
            this.scriptEngineImportScope = null;
        }
        this.topScope = createTopScope();

        this.objectConstructor = createObjectConstructor(this, objectPrototype);
        JSObjectUtil.putDataProperty(this.objectPrototype, JSObject.CONSTRUCTOR, objectConstructor, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putFunctionsFromContainer(this, this.objectPrototype, JSObjectPrototype.BUILTINS);
        this.functionConstructor = JSFunction.createFunctionConstructor(this);
        JSFunction.fillFunctionPrototype(this);

        this.applyFunctionObject = JSDynamicObject.getOrNull(getFunctionPrototype(), Strings.APPLY);
        this.callFunctionObject = JSDynamicObject.getOrNull(getFunctionPrototype(), Strings.CALL);

        JSConstructor ctor;
        ctor = JSArray.createConstructor(this);
        this.arrayConstructor = ctor.getFunctionObject();
        this.arrayPrototype = (JSArrayObject) ctor.getPrototype();
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
        int ecmaScriptVersion = context.getLanguageOptions().ecmaScriptVersion();
        boolean es6 = ecmaScriptVersion >= JSConfig.ECMAScript2015;
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

        this.errorConstructors = new JSFunctionObject[JSErrorType.errorTypes().length];
        this.errorPrototypes = new JSDynamicObject[JSErrorType.errorTypes().length];
        initializeErrorConstructors();
        ctor = JSError.createCallSiteConstructor(this);
        this.callSiteConstructor = ctor.getFunctionObject();
        this.callSitePrototype = ctor.getPrototype();

        ctor = JSArrayBuffer.createConstructor(this);
        this.arrayBufferConstructor = ctor.getFunctionObject();
        this.arrayBufferPrototype = ctor.getPrototype();
        this.typedArrayConstructors = new JSFunctionObject[TypedArray.factories(context).length];
        this.typedArrayPrototypes = new JSDynamicObject[TypedArray.factories(context).length];
        initializeTypedArrayConstructors();
        ctor = JSDataView.createConstructor(this);
        this.dataViewConstructor = ctor.getFunctionObject();
        this.dataViewPrototype = ctor.getPrototype();

        if (contextOptions.isBigInt()) {
            ctor = JSBigInt.createConstructor(this);
            this.bigIntConstructor = ctor.getFunctionObject();
            this.bigIntPrototype = ctor.getPrototype();
        } else {
            this.bigIntConstructor = null;
            this.bigIntPrototype = null;
        }

        if (context.getLanguageOptions().iteratorHelpers()) {
            assert ecmaScriptVersion >= JSConfig.ECMAScript2018;
            ctor = JSIterator.createConstructor(this);
            this.iteratorConstructor = ctor.getFunctionObject();
            this.iteratorPrototype = ctor.getPrototype();

            this.wrapForIteratorPrototype = JSWrapForValidIterator.INSTANCE.createPrototype(this, iteratorConstructor);
            this.iteratorHelperPrototype = createIteratorHelperPrototype();
        } else {
            this.iteratorPrototype = createIteratorPrototype();

            this.iteratorConstructor = null;
            this.wrapForIteratorPrototype = null;
            this.iteratorHelperPrototype = null;
        }

        if (context.getLanguageOptions().asyncIteratorHelpers()) {
            ctor = JSAsyncIterator.createConstructor(this);
            this.asyncIteratorPrototype = ctor.getPrototype();
            this.asyncIteratorContructor = ctor.getFunctionObject();
            this.wrapForAsyncIteratorPrototype = JSWrapForValidAsyncIterator.INSTANCE.createPrototype(this, asyncIteratorContructor);
            this.asyncIteratorHelperPrototype = createAsyncIteratorHelperPrototype();
        } else {
            if (ecmaScriptVersion >= JSConfig.ECMAScript2018) {
                this.asyncIteratorPrototype = JSFunction.createAsyncIteratorPrototype(this);
            } else {
                this.asyncIteratorPrototype = null;
            }
            this.asyncIteratorContructor = null;
            this.wrapForAsyncIteratorPrototype = null;
            this.asyncIteratorHelperPrototype = null;
        }

        this.arrayIteratorPrototype = es6 ? JSArrayIterator.INSTANCE.createPrototype(this, iteratorConstructor) : null;
        this.setIteratorPrototype = es6 ? JSSetIterator.INSTANCE.createPrototype(this, iteratorConstructor) : null;
        this.mapIteratorPrototype = es6 ? JSMapIterator.INSTANCE.createPrototype(this, iteratorConstructor) : null;
        this.stringIteratorPrototype = es6 ? JSStringIterator.INSTANCE.createPrototype(this, iteratorConstructor) : null;
        this.regExpStringIteratorPrototype = ecmaScriptVersion >= JSConfig.ECMAScript2019 ? createRegExpStringIteratorPrototype() : null;

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
        this.segmentsPrototype = JSSegmenter.createSegmentsPrototype(this);
        this.segmentIteratorPrototype = JSSegmenter.createSegmentIteratorPrototype(this);
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
            this.generatorObjectPrototype = (JSDynamicObject) JSDynamicObject.getOrNull(generatorFunctionPrototype, JSObject.PROTOTYPE);
        } else {
            this.generatorFunctionConstructor = null;
            this.generatorFunctionPrototype = null;
            this.generatorObjectPrototype = null;
        }
        this.enumerateIteratorPrototype = JSFunction.createEnumerateIteratorPrototype(this);
        this.forInIteratorPrototype = JSFunction.createForInIteratorPrototype(this);
        this.arrayProtoValuesIterator = (JSDynamicObject) JSDynamicObject.getOrDefault(getArrayPrototype(), Symbol.SYMBOL_ITERATOR, Undefined.instance);

        if (context.isOptionSharedArrayBuffer()) {
            ctor = JSSharedArrayBuffer.createConstructor(this);
            this.sharedArrayBufferConstructor = ctor.getFunctionObject();
            this.sharedArrayBufferPrototype = ctor.getPrototype();
        } else {
            this.sharedArrayBufferConstructor = null;
            this.sharedArrayBufferPrototype = null;
        }

        this.mathObject = JSMath.create(this);

        if (ecmaScriptVersion >= JSConfig.ECMAScript2017) {
            ctor = JSFunction.createAsyncFunctionConstructor(this);
            this.asyncFunctionConstructor = ctor.getFunctionObject();
            this.asyncFunctionPrototype = ctor.getPrototype();
        } else {
            this.asyncFunctionConstructor = null;
            this.asyncFunctionPrototype = null;
        }

        if (ecmaScriptVersion >= JSConfig.ECMAScript2018) {
            this.asyncFromSyncIteratorPrototype = JSFunction.createAsyncFromSyncIteratorPrototype(this);
            ctor = JSFunction.createAsyncGeneratorFunctionConstructor(this);
            this.asyncGeneratorFunctionConstructor = ctor.getFunctionObject();
            this.asyncGeneratorFunctionPrototype = ctor.getPrototype();
            this.asyncGeneratorObjectPrototype = (JSDynamicObject) JSDynamicObject.getOrNull(asyncGeneratorFunctionPrototype, JSObject.PROTOTYPE);
        } else {
            this.asyncFromSyncIteratorPrototype = null;
            this.asyncGeneratorFunctionConstructor = null;
            this.asyncGeneratorFunctionPrototype = null;
            this.asyncGeneratorObjectPrototype = null;
        }

        if (ecmaScriptVersion >= JSConfig.ECMAScript2021) {
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

        this.ordinaryHasInstanceFunction = JSFunction.createOrdinaryHasInstanceFunction(this);

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

        this.charset = getCharsetImpl();
        this.outputWriter = new PrintWriterWrapper(env.out(), true, charset);
        this.errorWriter = new PrintWriterWrapper(env.err(), true, charset);
        this.consoleUtil = new JSConsoleUtil();

        if (context.getLanguageOptions().commonJSRequire()) {
            this.commonJSRequireCache = new HashMap<>();
        } else {
            this.commonJSRequireCache = null;
        }

        ctor = createAbstractModuleSourcePrototype();
        this.abstractModuleSourceConstructor = ctor.getFunctionObject();
        this.abstractModuleSourcePrototype = ctor.getPrototype();

        if (context.getLanguageOptions().webAssembly()) {
            if (!isWasmAvailable()) {
                String msg = "WebAssembly API enabled but wasm language cannot be accessed! Did you forget to set the --polyglot flag?";
                throw new IllegalStateException(msg);
            }
            LanguageInfo wasmLanguageInfo = truffleLanguageEnv.getInternalLanguages().get("wasm");
            truffleLanguageEnv.initializeLanguage(wasmLanguageInfo);
            Object wasmObject = truffleLanguageEnv.importSymbol("WebAssembly");

            try {
                InteropLibrary wasmInterop = InteropLibrary.getUncached(wasmObject);
                wasmTableAlloc = wasmInterop.readMember(wasmObject, "table_alloc");
                wasmTableGrow = wasmInterop.readMember(wasmObject, "table_grow");
                wasmTableRead = wasmInterop.readMember(wasmObject, "table_read");
                wasmTableWrite = wasmInterop.readMember(wasmObject, "table_write");
                wasmTableLength = wasmInterop.readMember(wasmObject, "table_size");
                wasmFuncType = wasmInterop.readMember(wasmObject, "func_type");
                wasmIsFunc = wasmInterop.readMember(wasmObject, "is_func");
                wasmMemAlloc = wasmInterop.readMember(wasmObject, "mem_alloc");
                wasmMemGrow = wasmInterop.readMember(wasmObject, "mem_grow");
                wasmGlobalAlloc = wasmInterop.readMember(wasmObject, "global_alloc");
                wasmGlobalRead = wasmInterop.readMember(wasmObject, "global_read");
                wasmGlobalWrite = wasmInterop.readMember(wasmObject, "global_write");
                wasmModuleInstantiate = wasmInterop.readMember(wasmObject, "module_instantiate");
                wasmModuleExports = wasmInterop.readMember(wasmObject, "module_exports");
                wasmModuleImports = wasmInterop.readMember(wasmObject, "module_imports");
                wasmCustomSections = wasmInterop.readMember(wasmObject, "custom_sections");
                wasmInstanceExport = wasmInterop.readMember(wasmObject, "instance_export");
                wasmEmbedderDataGet = wasmInterop.readMember(wasmObject, "embedder_data_get");
                wasmEmbedderDataSet = wasmInterop.readMember(wasmObject, "embedder_data_set");
                wasmMemAsByteBuffer = wasmInterop.readMember(wasmObject, "mem_as_byte_buffer");
                wasmRefNull = wasmInterop.readMember(wasmObject, "ref_null");

                InteropLibrary settersInterop = InteropLibrary.getUncached();
                settersInterop.execute(wasmInterop.readMember(wasmObject, "mem_set_grow_callback"), new JSWebAssemblyMemoryGrowCallback(this));
                settersInterop.execute(wasmInterop.readMember(wasmObject, "mem_set_notify_callback"), new JSWebAssemblyMemoryNotifyCallback(this, context));
                settersInterop.execute(wasmInterop.readMember(wasmObject, "mem_set_wait_callback"), new JSWebAssemblyMemoryWaitCallback(this, context));
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
            this.wasmTableAlloc = null;
            this.wasmTableGrow = null;
            this.wasmTableRead = null;
            this.wasmTableWrite = null;
            this.wasmTableLength = null;
            this.wasmFuncType = null;
            this.wasmIsFunc = null;
            this.wasmMemAlloc = null;
            this.wasmMemGrow = null;
            this.wasmMemAsByteBuffer = null;
            this.wasmGlobalAlloc = null;
            this.wasmGlobalRead = null;
            this.wasmGlobalWrite = null;
            this.wasmModuleInstantiate = null;
            this.wasmModuleExports = null;
            this.wasmModuleImports = null;
            this.wasmCustomSections = null;
            this.wasmInstanceExport = null;
            this.wasmEmbedderDataGet = null;
            this.wasmEmbedderDataSet = null;
            this.wasmRefNull = null;

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
        this.foreignIteratorPrototype = createForeignIteratorPrototype();

        if (context.isOptionTemporal()) {
            ctor = JSTemporalPlainTime.createConstructor(this);
            this.temporalPlainTimeConstructor = ctor.getFunctionObject();
            this.temporalPlainTimePrototype = ctor.getPrototype();
            ctor = JSTemporalPlainDate.createConstructor(this);
            this.temporalPlainDateConstructor = ctor.getFunctionObject();
            this.temporalPlainDatePrototype = ctor.getPrototype();

            ctor = JSTemporalPlainDateTime.createConstructor(this);
            this.temporalPlainDateTimeConstructor = ctor.getFunctionObject();
            this.temporalPlainDateTimePrototype = ctor.getPrototype();

            ctor = JSTemporalDuration.createConstructor(this);
            this.temporalDurationConstructor = ctor.getFunctionObject();
            this.temporalDurationPrototype = ctor.getPrototype();

            ctor = JSTemporalPlainYearMonth.createConstructor(this);
            this.temporalPlainYearMonthConstructor = ctor.getFunctionObject();
            this.temporalPlainYearMonthPrototype = ctor.getPrototype();

            ctor = JSTemporalPlainMonthDay.createConstructor(this);
            this.temporalPlainMonthDayConstructor = ctor.getFunctionObject();
            this.temporalPlainMonthDayPrototype = ctor.getPrototype();

            ctor = JSTemporalInstant.createConstructor(this);
            this.temporalInstantConstructor = ctor.getFunctionObject();
            this.temporalInstantPrototype = ctor.getPrototype();

            ctor = JSTemporalZonedDateTime.createConstructor(this);
            this.temporalZonedDateTimeConstructor = ctor.getFunctionObject();
            this.temporalZonedDateTimePrototype = ctor.getPrototype();
        } else {
            this.temporalPlainTimeConstructor = null;
            this.temporalPlainTimePrototype = null;
            this.temporalPlainDateConstructor = null;
            this.temporalPlainDatePrototype = null;
            this.temporalPlainDateTimeConstructor = null;
            this.temporalPlainDateTimePrototype = null;
            this.temporalDurationConstructor = null;
            this.temporalDurationPrototype = null;
            this.temporalPlainYearMonthConstructor = null;
            this.temporalPlainYearMonthPrototype = null;
            this.temporalPlainMonthDayConstructor = null;
            this.temporalPlainMonthDayPrototype = null;
            this.temporalInstantConstructor = null;
            this.temporalInstantPrototype = null;
            this.temporalZonedDateTimeConstructor = null;
            this.temporalZonedDateTimePrototype = null;
        }

        if (context.getLanguageOptions().shadowRealm()) {
            ctor = JSShadowRealm.createConstructor(this);
            this.shadowRealmConstructor = ctor.getFunctionObject();
            this.shadowRealmPrototype = ctor.getPrototype();
        } else {
            this.shadowRealmConstructor = null;
            this.shadowRealmPrototype = null;
        }
        if (context.getLanguageOptions().worker()) {
            ctor = JSWorker.createConstructor(this);
            this.workerConstructor = ctor.getFunctionObject();
            this.workerPrototype = ctor.getPrototype();
        } else {
            this.workerConstructor = null;
            this.workerPrototype = null;
        }
        if (context.getLanguageOptions().asyncContext()) {
            ctor = JSAsyncContextSnapshot.createConstructor(this);
            this.asyncContextSnapshotConstructor = ctor.getFunctionObject();
            this.asyncContextSnapshotPrototype = ctor.getPrototype();
            ctor = JSAsyncContextVariable.createConstructor(this);
            this.asyncContextVariableConstructor = ctor.getFunctionObject();
            this.asyncContextVariablePrototype = ctor.getPrototype();
        } else {
            this.asyncContextSnapshotConstructor = null;
            this.asyncContextSnapshotPrototype = null;
            this.asyncContextVariableConstructor = null;
            this.asyncContextVariablePrototype = null;
        }

        if (contextOptions.isTextEncoding()) {
            ctor = JSTextDecoder.createConstructor(this);
            this.textDecoderConstructor = ctor.getFunctionObject();
            this.textDecoderPrototype = ctor.getPrototype();
            ctor = JSTextEncoder.createConstructor(this);
            this.textEncoderConstructor = ctor.getFunctionObject();
            this.textEncoderPrototype = ctor.getPrototype();
        } else {
            this.textDecoderConstructor = null;
            this.textDecoderPrototype = null;
            this.textEncoderConstructor = null;
            this.textEncoderPrototype = null;
        }

        // always create, regardless of context.isOptionForeignObjectPrototype()
        // we use them in some scenarios even when option is turned off
        this.foreignArrayPrototype = JSOrdinary.createInit(this, this.arrayPrototype);
        this.foreignDatePrototype = JSOrdinary.createInit(this, this.datePrototype);
        // mapPrototype can be null in ES5 mode
        this.foreignMapPrototype = JSOrdinary.createInit(this, this.mapPrototype == null ? Null.instance : this.mapPrototype);
        this.foreignStringPrototype = JSOrdinary.createInit(this, this.stringPrototype);
        this.foreignNumberPrototype = JSOrdinary.createInit(this, this.numberPrototype);
        this.foreignBooleanPrototype = JSOrdinary.createInit(this, this.booleanPrototype);
        this.foreignErrorPrototype = JSError.createForeignErrorPrototype(this);
        this.foreignFunctionPrototype = JSOrdinary.createInit(this, this.functionPrototype);
        this.foreignObjectPrototype = JSOrdinary.createInit(this, this.objectPrototype);
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

    public static JSRealm getMain(Node node) {
        return REFERENCE.get(node);
    }

    public static JSRealm get(Node node) {
        JSRealm mainRealm = REFERENCE.get(node);
        // We can skip the indirection as long as the single realm assumption is valid.
        // In the interpreter, checking the assumption would incur more overhead than the
        // indirection, so we do that only in compiled code.
        if (CompilerDirectives.inCompiledCode()) {
            if (CompilerDirectives.isPartialEvaluationConstant(node) && node != null && JavaScriptLanguage.get(node).getJSContext().isSingleRealm()) {
                assert mainRealm == mainRealm.currentRealm;
                return mainRealm;
            }
        } else {
            assert mainRealm.currentRealm == mainRealm || !JavaScriptLanguage.get(node).getJSContext().isSingleRealm();
        }
        return mainRealm.currentRealm;
    }

    private boolean allowEnterLeave(Node node, JSRealm otherRealm) {
        assert isMainRealm() && getMain(node) == this;
        // single realm assumption must be invalidated before an attempt to enter another realm
        assert !JavaScriptLanguage.get(node).getJSContext().isSingleRealm() || currentRealm == otherRealm;
        return true;
    }

    public JSRealm enterRealm(Node node, JSRealm childRealm) {
        assert allowEnterLeave(node, childRealm);
        JSRealm prev = this.currentRealm;
        this.currentRealm = childRealm;
        return prev;
    }

    public void leaveRealm(Node node, JSRealm prevRealm) {
        assert allowEnterLeave(node, prevRealm);
        this.currentRealm = prevRealm;
    }

    public final JSFunctionObject lookupFunction(JSBuiltinsContainer container, Object key) {
        assert JSRuntime.isPropertyKey(key);
        Builtin builtin = Objects.requireNonNull(container.lookupFunctionByKey(key));
        JSFunctionData functionData = builtin.createFunctionData(context);
        return JSFunction.create(this, functionData);
    }

    public final JSFunctionObject lookupFunctionWithPrototype(JSBuiltinsContainer container, Object key, JSDynamicObject prototype) {
        assert JSRuntime.isPropertyKey(key);
        Builtin builtin = Objects.requireNonNull(container.lookupFunctionByKey(key));
        JSFunctionData functionData = builtin.createFunctionData(context);
        return JSFunction.createWithPrototype(this, functionData, prototype);
    }

    public final Accessor lookupAccessor(JSBuiltinsContainer container, Object key) {
        Pair<JSBuiltin, JSBuiltin> pair = container.lookupAccessorByKey(key);
        JSBuiltin getterBuiltin = pair.getLeft();
        JSBuiltin setterBuiltin = pair.getRight();
        JSFunctionObject getterFunction = null;
        JSFunctionObject setterFunction = null;
        if (getterBuiltin != null) {
            JSFunctionData functionData = getterBuiltin.createFunctionData(context);
            getterFunction = JSFunction.create(this, functionData);
        }
        if (setterBuiltin != null) {
            JSFunctionData functionData = setterBuiltin.createFunctionData(context);
            setterFunction = JSFunction.create(this, functionData);
        }
        return new Accessor(getterFunction, setterFunction);
    }

    public static JSFunctionObject createObjectConstructor(JSRealm realm, JSDynamicObject objectPrototype) {
        JSContext context = realm.getContext();
        JSFunctionObject objectConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, JSOrdinary.CLASS_NAME);
        JSObjectUtil.putConstructorPrototypeProperty(objectConstructor, objectPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, objectConstructor, ObjectFunctionBuiltins.BUILTINS);
        if (context.isOptionNashornCompatibilityMode()) {
            JSObjectUtil.putFunctionsFromContainer(realm, objectConstructor, ObjectFunctionBuiltins.BUILTINS_NASHORN_COMPAT);
        }
        return objectConstructor;
    }

    public final JSFunctionObject getErrorConstructor(JSErrorType type) {
        return errorConstructors[type.ordinal()];
    }

    public final JSDynamicObject getErrorPrototype(JSErrorType type) {
        return errorPrototypes[type.ordinal()];
    }

    public final JSDynamicObject getGlobalObject() {
        return globalObject;
    }

    public final void setGlobalObject(JSDynamicObject global) {
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

    public final JSFunctionObject getObjectConstructor() {
        return objectConstructor;
    }

    public final JSDynamicObject getObjectPrototype() {
        return objectPrototype;
    }

    public final JSFunctionObject getFunctionConstructor() {
        return functionConstructor;
    }

    public final JSDynamicObject getFunctionPrototype() {
        return functionPrototype;
    }

    public final JSFunctionObject getArrayConstructor() {
        return arrayConstructor;
    }

    public final JSDynamicObject getArrayPrototype() {
        return arrayPrototype;
    }

    public final JSFunctionObject getBooleanConstructor() {
        return booleanConstructor;
    }

    public final JSDynamicObject getBooleanPrototype() {
        return booleanPrototype;
    }

    public final JSFunctionObject getNumberConstructor() {
        return numberConstructor;
    }

    public final JSDynamicObject getNumberPrototype() {
        return numberPrototype;
    }

    public final JSFunctionObject getBigIntConstructor() {
        return bigIntConstructor;
    }

    public final JSDynamicObject getBigIntPrototype() {
        return bigIntPrototype;
    }

    public final JSFunctionObject getStringConstructor() {
        return stringConstructor;
    }

    public final JSDynamicObject getStringPrototype() {
        return stringPrototype;
    }

    public final JSFunctionObject getRegExpConstructor() {
        return regExpConstructor;
    }

    public final JSDynamicObject getRegExpPrototype() {
        return regExpPrototype;
    }

    public final JSFunctionObject getCollatorConstructor() {
        return collatorConstructor;
    }

    public final JSDynamicObject getCollatorPrototype() {
        return collatorPrototype;
    }

    public final JSFunctionObject getNumberFormatConstructor() {
        return numberFormatConstructor;
    }

    public final JSDynamicObject getNumberFormatPrototype() {
        return numberFormatPrototype;
    }

    public final JSFunctionObject getPluralRulesConstructor() {
        return pluralRulesConstructor;
    }

    public final JSDynamicObject getPluralRulesPrototype() {
        return pluralRulesPrototype;
    }

    public final JSFunctionObject getListFormatConstructor() {
        return listFormatConstructor;
    }

    public final JSDynamicObject getListFormatPrototype() {
        return listFormatPrototype;
    }

    public final JSFunctionObject getRelativeTimeFormatConstructor() {
        return relativeTimeFormatConstructor;
    }

    public final JSDynamicObject getRelativeTimeFormatPrototype() {
        return relativeTimeFormatPrototype;
    }

    public final JSFunctionObject getDateTimeFormatConstructor() {
        return dateTimeFormatConstructor;
    }

    public final JSDynamicObject getDateTimeFormatPrototype() {
        return dateTimeFormatPrototype;
    }

    public final JSFunctionObject getDateConstructor() {
        return dateConstructor;
    }

    public final JSDynamicObject getDatePrototype() {
        return datePrototype;
    }

    public final JSFunctionObject getSegmenterConstructor() {
        return segmenterConstructor;
    }

    public final JSDynamicObject getSegmenterPrototype() {
        return segmenterPrototype;
    }

    public final JSFunctionObject getDisplayNamesConstructor() {
        return displayNamesConstructor;
    }

    public final JSDynamicObject getDisplayNamesPrototype() {
        return displayNamesPrototype;
    }

    public final JSFunctionObject getLocaleConstructor() {
        return localeConstructor;
    }

    public final JSDynamicObject getLocalePrototype() {
        return localePrototype;
    }

    public final JSFunctionObject getSymbolConstructor() {
        return symbolConstructor;
    }

    public final JSDynamicObject getSymbolPrototype() {
        return symbolPrototype;
    }

    public final JSFunctionObject getMapConstructor() {
        return mapConstructor;
    }

    public final JSDynamicObject getMapPrototype() {
        return mapPrototype;
    }

    public final JSFunctionObject getSetConstructor() {
        return setConstructor;
    }

    public final JSDynamicObject getSetPrototype() {
        return setPrototype;
    }

    public final JSFunctionObject getWeakRefConstructor() {
        return weakRefConstructor;
    }

    public final JSDynamicObject getWeakRefPrototype() {
        return weakRefPrototype;
    }

    public final JSFunctionObject getFinalizationRegistryConstructor() {
        return finalizationRegistryConstructor;
    }

    public final JSDynamicObject getFinalizationRegistryPrototype() {
        return finalizationRegistryPrototype;
    }

    public final JSFunctionObject getWeakMapConstructor() {
        return weakMapConstructor;
    }

    public final JSDynamicObject getWeakMapPrototype() {
        return weakMapPrototype;
    }

    public final JSFunctionObject getWeakSetConstructor() {
        return weakSetConstructor;
    }

    public final JSDynamicObject getWeakSetPrototype() {
        return weakSetPrototype;
    }

    public final Shape getInitialRegExpPrototypeShape() {
        return initialRegExpPrototypeShape;
    }

    public final JSFunctionObject getArrayBufferConstructor() {
        return arrayBufferConstructor;
    }

    public final JSDynamicObject getArrayBufferPrototype() {
        return arrayBufferPrototype;
    }

    public final JSFunctionObject getSharedArrayBufferConstructor() {
        assert context.isOptionSharedArrayBuffer();
        return sharedArrayBufferConstructor;
    }

    public final JSDynamicObject getSharedArrayBufferPrototype() {
        assert context.isOptionSharedArrayBuffer();
        return sharedArrayBufferPrototype;
    }

    public final JSFunctionObject getArrayBufferViewConstructor(TypedArrayFactory factory) {
        return typedArrayConstructors[factory.getFactoryIndex()];
    }

    public final JSDynamicObject getArrayBufferViewPrototype(TypedArrayFactory factory) {
        return typedArrayPrototypes[factory.getFactoryIndex()];
    }

    public final JSFunctionObject getDataViewConstructor() {
        return dataViewConstructor;
    }

    public final JSDynamicObject getDataViewPrototype() {
        return dataViewPrototype;
    }

    public final JSFunctionObject getTypedArrayConstructor() {
        return typedArrayConstructor;
    }

    public final JSDynamicObject getTypedArrayPrototype() {
        return typedArrayPrototype;
    }

    public final JSDynamicObject getRealmBuiltinObject() {
        return realmBuiltinObject;
    }

    public final JSFunctionObject getProxyConstructor() {
        return proxyConstructor;
    }

    public final JSDynamicObject getProxyPrototype() {
        return proxyPrototype;
    }

    public final JSFunctionObject getGeneratorFunctionConstructor() {
        return generatorFunctionConstructor;
    }

    public final JSDynamicObject getGeneratorFunctionPrototype() {
        return generatorFunctionPrototype;
    }

    public final JSFunctionObject getAsyncFunctionConstructor() {
        return asyncFunctionConstructor;
    }

    public final JSDynamicObject getAsyncFunctionPrototype() {
        return asyncFunctionPrototype;
    }

    public final JSFunctionObject getAsyncGeneratorFunctionConstructor() {
        return asyncGeneratorFunctionConstructor;
    }

    public final JSDynamicObject getAsyncGeneratorFunctionPrototype() {
        return asyncGeneratorFunctionPrototype;
    }

    public final JSDynamicObject getEnumerateIteratorPrototype() {
        return enumerateIteratorPrototype;
    }

    public final JSDynamicObject getForInIteratorPrototype() {
        return forInIteratorPrototype;
    }

    public final JSDynamicObject getGeneratorObjectPrototype() {
        return generatorObjectPrototype;
    }

    public final JSDynamicObject getAsyncGeneratorObjectPrototype() {
        return asyncGeneratorObjectPrototype;
    }

    public final JSFunctionObject getJavaImporterConstructor() {
        return javaImporterConstructor;
    }

    public final JSDynamicObject getJavaImporterPrototype() {
        return javaImporterPrototype;
    }

    public final JSDynamicObject getJavaPackageToPrimitiveFunction() {
        assert javaPackageToPrimitiveFunction != null;
        return javaPackageToPrimitiveFunction;
    }

    public final JSFunctionObject getTemporalPlainTimeConstructor() {
        return temporalPlainTimeConstructor;
    }

    public final JSDynamicObject getTemporalPlainTimePrototype() {
        return temporalPlainTimePrototype;
    }

    public final JSFunctionObject getTemporalPlainDateConstructor() {
        return temporalPlainDateConstructor;
    }

    public final JSDynamicObject getTemporalPlainDatePrototype() {
        return temporalPlainDatePrototype;
    }

    public final JSFunctionObject getTemporalPlainDateTimeConstructor() {
        return temporalPlainDateTimeConstructor;
    }

    public final JSDynamicObject getTemporalPlainDateTimePrototype() {
        return temporalPlainDateTimePrototype;
    }

    public final JSFunctionObject getTemporalDurationConstructor() {
        return temporalDurationConstructor;
    }

    public final JSDynamicObject getTemporalDurationPrototype() {
        return temporalDurationPrototype;
    }

    public final JSFunctionObject getTemporalPlainYearMonthConstructor() {
        return temporalPlainYearMonthConstructor;
    }

    public JSDynamicObject getTemporalPlainYearMonthPrototype() {
        return temporalPlainYearMonthPrototype;
    }

    public JSFunctionObject getTemporalPlainMonthDayConstructor() {
        return temporalPlainMonthDayConstructor;
    }

    public JSDynamicObject getTemporalPlainMonthDayPrototype() {
        return temporalPlainMonthDayPrototype;
    }

    public JSFunctionObject getTemporalInstantConstructor() {
        return temporalInstantConstructor;
    }

    public JSDynamicObject getTemporalInstantPrototype() {
        return temporalInstantPrototype;
    }

    public JSFunctionObject getTemporalZonedDateTimeConstructor() {
        return temporalZonedDateTimeConstructor;
    }

    public JSDynamicObject getTemporalZonedDateTimePrototype() {
        return temporalZonedDateTimePrototype;
    }

    public final JSDynamicObject getForeignArrayPrototype() {
        return foreignArrayPrototype;
    }

    public final JSDynamicObject getForeignDatePrototype() {
        return foreignDatePrototype;
    }

    public JSDynamicObject getForeignMapPrototype() {
        return foreignMapPrototype;
    }

    public JSDynamicObject getForeignStringPrototype() {
        return foreignStringPrototype;
    }

    public JSDynamicObject getForeignNumberPrototype() {
        return foreignNumberPrototype;
    }

    public JSDynamicObject getForeignBooleanPrototype() {
        return foreignBooleanPrototype;
    }

    public JSDynamicObject getForeignErrorPrototype() {
        return foreignErrorPrototype;
    }

    public JSDynamicObject getForeignFunctionPrototype() {
        return foreignFunctionPrototype;
    }

    public JSDynamicObject getForeignObjectPrototype() {
        return foreignObjectPrototype;
    }

    public final Map<Object, JSArrayObject> getTemplateRegistry() {
        if (templateRegistry == null) {
            createTemplateRegistry();
        }
        return templateRegistry;
    }

    @TruffleBoundary
    private void createTemplateRegistry() {
        if (templateRegistry == null) {
            templateRegistry = new WeakHashMap<>();
        }
    }

    public final Map<Object, JSArrayObject> getDedentMap() {
        if (dedentMap == null) {
            createDedentMap();
        }
        return dedentMap;
    }

    @TruffleBoundary
    private void createDedentMap() {
        if (dedentMap == null) {
            dedentMap = new WeakHashMap<>();
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

    public final JSFunctionObject getPromiseAllFunctionObject() {
        return promiseAllFunctionObject;
    }

    public final Object getUnhandledPromiseRejectionHandler() {
        return unhandledPromiseRejectionHandler;
    }

    private static void putProtoAccessorProperty(final JSRealm realm) {
        JSContext context = realm.getContext();
        JSDynamicObject getProto = JSFunction.create(realm, context.protoGetterFunctionData);
        JSDynamicObject setProto = JSFunction.create(realm, context.protoSetterFunctionData);

        // ES6 draft annex, B.2.2 Additional Properties of the Object.prototype Object
        JSObjectUtil.putBuiltinAccessorProperty(realm.getObjectPrototype(), JSObject.PROTO, getProto, setProto);
    }

    public final JSFunctionObject getThrowTypeErrorFunction() {
        assert throwTypeErrorFunction != null;
        return throwTypeErrorFunction;
    }

    public final Accessor getThrowerAccessor() {
        assert throwerAccessor != null;
        return throwerAccessor;
    }

    public JSFunctionObject getIteratorConstructor() {
        return iteratorConstructor;
    }

    public JSFunctionObject getAsyncIteratorConstructor() {
        return asyncIteratorContructor;
    }

    public JSDynamicObject getIteratorPrototype() {
        return iteratorPrototype;
    }

    public JSDynamicObject getWrapForIteratorPrototype() {
        return wrapForIteratorPrototype;
    }

    public JSDynamicObject getWrapForAsyncIteratorPrototype() {
        return wrapForAsyncIteratorPrototype;
    }

    public JSDynamicObject getAsyncIteratorPrototype() {
        return asyncIteratorPrototype;
    }

    public JSDynamicObject getAsyncFromSyncIteratorPrototype() {
        return asyncFromSyncIteratorPrototype;
    }

    public JSDynamicObject getArrayIteratorPrototype() {
        return arrayIteratorPrototype;
    }

    public JSDynamicObject getSetIteratorPrototype() {
        return setIteratorPrototype;
    }

    public JSDynamicObject getMapIteratorPrototype() {
        return mapIteratorPrototype;
    }

    public JSDynamicObject getIteratorHelperPrototype() {
        return iteratorHelperPrototype;
    }

    public JSDynamicObject getAsyncIteratorHelperPrototype() {
        return asyncIteratorHelperPrototype;
    }

    public JSDynamicObject getStringIteratorPrototype() {
        return stringIteratorPrototype;
    }

    public JSDynamicObject getRegExpStringIteratorPrototype() {
        return regExpStringIteratorPrototype;
    }

    public JSDynamicObject getSegmentsPrototype() {
        return segmentsPrototype;
    }

    public JSDynamicObject getSegmentIteratorPrototype() {
        return segmentIteratorPrototype;
    }

    /**
     * Creates the %ThrowTypeError% function object (https://tc39.es/ecma262/#sec-%throwtypeerror%).
     * It is used where a function is needed that always throws a TypeError, including getters and
     * setters for restricted (i.e. deprecated) function and arguments object properties (namely,
     * 'caller', 'callee', 'arguments') that may not be accessed in strict mode.
     */
    private JSFunctionObject createThrowTypeErrorFunction(boolean restrictedProperty) {
        CompilerAsserts.neverPartOfCompilation();
        JSFunctionObject thrower = JSFunction.create(this, restrictedProperty ? context.throwTypeErrorRestrictedPropertyFunctionData : context.throwTypeErrorFunctionData);
        thrower.preventExtensions(true);
        thrower.setIntegrityLevel(true, true);
        return thrower;
    }

    public JSFunctionObject getPromiseConstructor() {
        return promiseConstructor;
    }

    public JSDynamicObject getPromisePrototype() {
        return promisePrototype;
    }

    public final JSObjectFactory.RealmData getObjectFactories() {
        return objectFactories;
    }

    public final JSFunctionObject getShadowRealmConstructor() {
        return shadowRealmConstructor;
    }

    public final JSDynamicObject getShadowRealmPrototype() {
        return shadowRealmPrototype;
    }

    public final JSFunctionObject getWorkerConstructor() {
        return workerConstructor;
    }

    public final JSDynamicObject getWorkerPrototype() {
        return workerPrototype;
    }

    public final JSFunctionObject getAsyncContextSnapshotConstructor() {
        return asyncContextSnapshotConstructor;
    }

    public final JSDynamicObject getAsyncContextSnapshotPrototype() {
        return asyncContextSnapshotPrototype;
    }

    public final JSFunctionObject getAsyncContexVariableConstructor() {
        return asyncContextVariableConstructor;
    }

    public final JSDynamicObject getAsyncContextVariablePrototype() {
        return asyncContextVariablePrototype;
    }

    public final JSContextOptions getContextOptions() {
        return contextOptions;
    }

    public void setupGlobals() {
        CompilerAsserts.neverPartOfCompilation("do not setup globals from compiled code");
        long time = context.getLanguageOptions().profileTime() ? System.nanoTime() : 0L;

        JSDynamicObject global = getGlobalObject();
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

        JSObjectUtil.putDataProperty(global, Strings.NAN, Double.NaN);
        JSObjectUtil.putDataProperty(global, Strings.INFINITY, Double.POSITIVE_INFINITY);
        JSObjectUtil.putDataProperty(global, Undefined.NAME, Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(this, global, GlobalBuiltins.GLOBAL_FUNCTIONS);

        this.evalFunctionObject = JSObject.get(global, JSGlobal.EVAL_NAME);
        JSDynamicObject jsonBuiltin = (JSDynamicObject) JSObject.get(global, JSON.CLASS_NAME);
        this.jsonParseFunctionObject = JSObject.get(jsonBuiltin, Strings.PARSE);

        boolean webassembly = getContextOptions().isWebAssembly();
        for (JSErrorType type : JSErrorType.errorTypes()) {
            switch (type) {
                case CompileError:
                case LinkError:
                case RuntimeError:
                    if (webassembly) {
                        JSObjectUtil.putDataProperty(webAssemblyObject, Strings.fromJavaString(type.name()), getErrorConstructor(type), JSAttributes.getDefaultNotEnumerable());
                    }
                    break;
                case AggregateError:
                    if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2021) {
                        putGlobalProperty(Strings.fromJavaString(type.name()), getErrorConstructor(type));
                    }
                    break;
                default:
                    putGlobalProperty(Strings.fromJavaString(type.name()), getErrorConstructor(type));
                    break;
            }
        }

        putGlobalProperty(JSArrayBuffer.CLASS_NAME, getArrayBufferConstructor());
        for (TypedArrayFactory factory : TypedArray.factories(context)) {
            putGlobalProperty(factory.getName(), getArrayBufferViewConstructor(factory));
        }
        putGlobalProperty(JSDataView.CLASS_NAME, getDataViewConstructor());

        if (getContextOptions().isBigInt()) {
            putGlobalProperty(JSBigInt.CLASS_NAME, getBigIntConstructor());
        }

        if (context.isOptionNashornCompatibilityMode()) {
            initGlobalNashornExtensions();
            removeNashornIncompatibleBuiltins();
        }
        if (getContextOptions().isScriptEngineGlobalScopeImport()) {
            TruffleString builtin = Strings.IMPORT_SCRIPT_ENGINE_GLOBAL_BINDINGS;
            JSObjectUtil.putDataProperty(getScriptEngineImportScope(), builtin, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, builtin),
                            JSAttributes.notConfigurableNotEnumerableNotWritable());
        }
        if (getContextOptions().isPolyglotBuiltin() && (getEnv().isPolyglotEvalAllowed(null) || getEnv().isPolyglotBindingsAccessAllowed())) {
            setupPolyglot();
        }
        if (getContextOptions().isDebugBuiltin()) {
            putGlobalProperty(Strings.fromJavaString(getContextOptions().getDebugPropertyName()), createDebugObject());
        }
        if (context.isOptionMleBuiltin()) {
            putGlobalProperty(Strings.fromJavaString(JSContextOptions.MLE_PROPERTY_NAME), createMleObject());
        }
        if (getContextOptions().isTest262Mode()) {
            putGlobalProperty(JSTest262.GLOBAL_PROPERTY_NAME, JSTest262.create(this));
        }
        if (getContextOptions().isTestV8Mode()) {
            putGlobalProperty(JSTestV8.CLASS_NAME, JSTestV8.create(this));
        }
        if (getContextOptions().isV8RealmBuiltin()) {
            initRealmBuiltinObject();
        }
        if (context.getEcmaScriptVersion() >= 6) {
            Object parseInt = JSObject.get(global, Strings.PARSE_INT);
            Object parseFloat = JSObject.get(global, Strings.PARSE_FLOAT);
            putProperty(getNumberConstructor(), Strings.PARSE_INT, parseInt);
            putProperty(getNumberConstructor(), Strings.PARSE_FLOAT, parseFloat);

            putGlobalProperty(JSMap.CLASS_NAME, getMapConstructor());
            putGlobalProperty(JSSet.CLASS_NAME, getSetConstructor());
            putGlobalProperty(JSWeakMap.CLASS_NAME, getWeakMapConstructor());
            putGlobalProperty(JSWeakSet.CLASS_NAME, getWeakSetConstructor());
            putGlobalProperty(JSSymbol.CLASS_NAME, getSymbolConstructor());
            setupPredefinedSymbols(getSymbolConstructor());

            JSDynamicObject reflectObject = createReflect();
            putGlobalProperty(REFLECT_CLASS_NAME, reflectObject);
            this.reflectApplyFunctionObject = JSObject.get(reflectObject, Strings.APPLY);
            this.reflectConstructFunctionObject = JSObject.get(reflectObject, Strings.CONSTRUCT);

            putGlobalProperty(JSProxy.CLASS_NAME, getProxyConstructor());
            putGlobalProperty(JSPromise.CLASS_NAME, getPromiseConstructor());
            this.promiseAllFunctionObject = (JSFunctionObject) JSObject.get(getPromiseConstructor(), Strings.ALL);
        }
        if (getContextOptions().isIteratorHelpers()) {
            putGlobalProperty(JSIterator.CLASS_NAME, getIteratorConstructor());
        }
        if (getContextOptions().isAsyncIteratorHelpers()) {
            putGlobalProperty(JSAsyncIterator.CLASS_NAME, getAsyncIteratorConstructor());
        }

        if (context.isOptionSharedArrayBuffer()) {
            putGlobalProperty(JSSharedArrayBuffer.CLASS_NAME, getSharedArrayBufferConstructor());
        }
        if (getContextOptions().isAtomics()) {
            putGlobalProperty(ATOMICS_CLASS_NAME, createAtomics());
        }
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            putGlobalProperty(Strings.GLOBAL_THIS, global);
        }
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2021) {
            putGlobalProperty(JSWeakRef.CLASS_NAME, getWeakRefConstructor());
            putGlobalProperty(JSFinalizationRegistry.CLASS_NAME, getFinalizationRegistryConstructor());
        }
        if (getContextOptions().isGraalBuiltin()) {
            putGraalObject();
        }
        if (webassembly) {
            putGlobalProperty(JSWebAssembly.CLASS_NAME, webAssemblyObject);
            JSObjectUtil.putDataProperty(webAssemblyObject, JSFunction.getName(webAssemblyGlobalConstructor), webAssemblyGlobalConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(webAssemblyObject, JSFunction.getName(webAssemblyInstanceConstructor), webAssemblyInstanceConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(webAssemblyObject, JSFunction.getName(webAssemblyMemoryConstructor), webAssemblyMemoryConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(webAssemblyObject, JSFunction.getName(webAssemblyModuleConstructor), webAssemblyModuleConstructor, JSAttributes.getDefaultNotEnumerable());
            JSObjectUtil.putDataProperty(webAssemblyObject, JSFunction.getName(webAssemblyTableConstructor), webAssemblyTableConstructor, JSAttributes.getDefaultNotEnumerable());
        }
        if (getContextOptions().isOperatorOverloading()) {
            JSObjectUtil.putFunctionsFromContainer(this, global, OperatorsBuiltins.BUILTINS);
        }
        if (context.isOptionTemporal()) {
            addTemporalGlobals();
        }
        if (getContextOptions().isShadowRealm()) {
            putGlobalProperty(JSShadowRealm.CLASS_NAME, getShadowRealmConstructor());
        }
        if (getContextOptions().isWorker()) {
            putGlobalProperty(JSWorker.CLASS_NAME, getWorkerConstructor());
        }
        if (getContextOptions().isAsyncContext()) {
            putGlobalProperty(JSAsyncContext.NAMESPACE_NAME, JSAsyncContext.create(this));
        }
        if (getContextOptions().isTextEncoding()) {
            putGlobalProperty(JSTextEncoder.CLASS_NAME, textEncoderConstructor);
            putGlobalProperty(JSTextDecoder.CLASS_NAME, textDecoderConstructor);
        }

        if (context.getLanguageOptions().profileTime()) {
            System.out.println("SetupGlobals: " + (System.nanoTime() - time) / 1000000);
        }
    }

    private void initGlobalNashornExtensions() {
        assert getContext().isOptionNashornCompatibilityMode();
        putGlobalProperty(JSAdapter.CLASS_NAME, jsAdapterConstructor);
        putGlobalProperty(Strings.EXIT, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.EXIT));
        if (!getContextOptions().isShell()) {
            putGlobalProperty(Strings.QUIT, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.QUIT));
        } // else 'quit' built-in will be defined together with GLOBAL_SHELL built-ins
        putGlobalProperty(Strings.PARSE_TO_JSON, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.PARSE_TO_JSON));
    }

    private void removeNashornIncompatibleBuiltins() {
        assert getContext().isOptionNashornCompatibilityMode();

        // Nashorn has no join method on TypedArrays
        JSObject.delete(typedArrayPrototype, Strings.JOIN);
    }

    private void addPrintGlobals() {
        if (getContextOptions().isPrint()) {
            putGlobalProperty(Strings.PRINT, lookupFunction(GlobalBuiltins.GLOBAL_PRINT, Strings.PRINT));
            putGlobalProperty(Strings.PRINT_ERR, lookupFunction(GlobalBuiltins.GLOBAL_PRINT, Strings.PRINT_ERR));
        }
    }

    @TruffleBoundary
    private void addCommonJSGlobals() {
        if (getContextOptions().isCommonJSRequire()) {
            String cwdOption = getContextOptions().getRequireCwd();
            try {
                if (!cwdOption.isEmpty()) {
                    TruffleFile cwdFile = getEnv().getPublicTruffleFile(cwdOption);
                    if (!cwdFile.exists()) {
                        throw Errors.createError("Invalid CommonJS root folder: " + cwdOption);
                    }
                }
            } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException se) {
                throw Errors.createError("Access denied to CommonJS root folder: " + cwdOption);
            }
            // Define `require` and other globals in global scope.
            JSBuiltinsContainer builtins = GlobalCommonJSRequireBuiltins.GLOBAL_COMMONJS_REQUIRE_EXTENSIONS;
            JSDynamicObject requireFunction = lookupFunction(builtins, Strings.REQUIRE_PROPERTY_NAME);
            JSDynamicObject resolveFunction = lookupFunction(builtins, Strings.RESOLVE_PROPERTY_NAME);
            JSObject.set(requireFunction, Strings.RESOLVE_PROPERTY_NAME, resolveFunction);
            putGlobalProperty(Strings.REQUIRE_PROPERTY_NAME, requireFunction);
            JSDynamicObject dirnameGetter = lookupFunction(builtins, GlobalCommonJSRequireBuiltins.GlobalRequire.dirnameGetter.getKey());
            JSObject.defineOwnProperty(getGlobalObject(), Strings.DIRNAME_VAR_NAME, PropertyDescriptor.createAccessor(dirnameGetter, Undefined.instance, false, false));
            JSDynamicObject filenameGetter = lookupFunction(builtins, GlobalCommonJSRequireBuiltins.GlobalRequire.filenameGetter.getKey());
            JSObject.defineOwnProperty(getGlobalObject(), Strings.FILENAME_VAR_NAME, PropertyDescriptor.createAccessor(filenameGetter, Undefined.instance, false, false));
            JSDynamicObject moduleGetter = lookupFunction(builtins, GlobalCommonJSRequireBuiltins.GlobalRequire.globalModuleGetter.getKey());
            JSObject.defineOwnProperty(getGlobalObject(), Strings.MODULE_PROPERTY_NAME, PropertyDescriptor.createAccessor(moduleGetter, Undefined.instance, false, false));
            JSDynamicObject exportsGetter = lookupFunction(builtins, GlobalCommonJSRequireBuiltins.GlobalRequire.globalExportsGetter.getKey());
            JSObject.defineOwnProperty(getGlobalObject(), Strings.EXPORTS_PROPERTY_NAME, PropertyDescriptor.createAccessor(exportsGetter, Undefined.instance, false, false));
            this.commonJSRequireFunctionObject = requireFunction;
        }
    }

    private void addLoadGlobals() {
        if (getContextOptions().isLoad()) {
            putGlobalProperty(Strings.LOAD, lookupFunction(GlobalBuiltins.GLOBAL_LOAD, Strings.LOAD));
            putGlobalProperty(Strings.LOAD_WITH_NEW_GLOBAL, lookupFunction(GlobalBuiltins.GLOBAL_LOAD, Strings.LOAD_WITH_NEW_GLOBAL));
        }
    }

    private void addPerformanceGlobal() {
        if (getContextOptions().isPerformance()) {
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
        if (getContextOptions().isGlobalProperty()) {
            putGlobalProperty(Strings.GLOBAL, getGlobalObject());
        }
    }

    private void addShellGlobals() {
        if (getContextOptions().isShell()) {
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

    private void addTemporalGlobals() {
        assert context.isOptionTemporal();
        JSObject temporalObject = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(temporalObject, TemporalConstants.TEMPORAL);

        int flags = JSAttributes.configurableNotEnumerableWritable();
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalPlainTime.CLASS_NAME, getTemporalPlainTimeConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalPlainDate.CLASS_NAME, getTemporalPlainDateConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalPlainDateTime.CLASS_NAME, getTemporalPlainDateTimeConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalDuration.CLASS_NAME, getTemporalDurationConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalPlainYearMonth.CLASS_NAME, getTemporalPlainYearMonthConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalPlainMonthDay.CLASS_NAME, getTemporalPlainMonthDayConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalInstant.CLASS_NAME, getTemporalInstantConstructor(), flags);
        JSObjectUtil.putDataProperty(temporalObject, JSTemporalZonedDateTime.CLASS_NAME, getTemporalZonedDateTimeConstructor(), flags);

        JSObject nowObject = JSOrdinary.createInit(this);

        JSObjectUtil.putDataProperty(temporalObject, TemporalConstants.NOW, nowObject, flags);
        JSObjectUtil.putFunctionsFromContainer(this, nowObject, TemporalNowBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(nowObject, TemporalConstants.TEMPORAL_NOW_TO_STRING_TAG);

        putGlobalProperty(TemporalConstants.TEMPORAL, temporalObject);

        // Date.prototype.toTemporalInstant
        Object toTemporalInstantFn = lookupFunction(DatePrototypeBuiltins.BUILTINS, Strings.TO_TEMPORAL_INSTANT);
        JSObjectUtil.putDataProperty(getDatePrototype(), Strings.TO_TEMPORAL_INSTANT, toTemporalInstantFn, JSAttributes.getDefaultNotEnumerable());
    }

    private JSDynamicObject createIntlObject() {
        JSObject intlObject = JSIntl.create(this);
        JSFunctionObject collatorFn = getCollatorConstructor();
        JSFunctionObject numberFormatFn = getNumberFormatConstructor();
        JSFunctionObject dateTimeFormatFn = getDateTimeFormatConstructor();
        JSFunctionObject pluralRulesFn = getPluralRulesConstructor();
        JSFunctionObject listFormatFn = getListFormatConstructor();
        JSFunctionObject relativeTimeFormatFn = getRelativeTimeFormatConstructor();
        JSFunctionObject segmenterFn = getSegmenterConstructor();
        JSFunctionObject displayNamesFn = getDisplayNamesConstructor();
        JSFunctionObject localeFn = getLocaleConstructor();
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(collatorFn), collatorFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(numberFormatFn), numberFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(dateTimeFormatFn), dateTimeFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(pluralRulesFn), pluralRulesFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(listFormatFn), listFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(relativeTimeFormatFn), relativeTimeFormatFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(segmenterFn), segmenterFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(displayNamesFn), displayNamesFn, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(intlObject, JSFunction.getName(localeFn), localeFn, JSAttributes.getDefaultNotEnumerable());
        return intlObject;
    }

    private void putGraalObject() {
        JSObject graalObject = JSOrdinary.createInit(this);
        int flags = JSAttributes.notConfigurableEnumerableNotWritable();
        JSContextOptions options = getContextOptions();
        int esVersion = options.getEcmaScriptVersion();
        esVersion = (esVersion > JSConfig.ECMAScript6 ? esVersion + JSConfig.ECMAScriptVersionYearDelta : esVersion);
        JSObjectUtil.putDataProperty(graalObject, Strings.LANGUAGE, Strings.fromJavaString(JavaScriptLanguage.NAME), flags);
        assert GRAALVM_VERSION != null;
        JSObjectUtil.putDataProperty(graalObject, Strings.VERSION_GRAAL_VM, GRAALVM_VERSION, flags);
        JSObjectUtil.putDataProperty(graalObject, Strings.VERSION_ECMA_SCRIPT, esVersion, flags);
        JSObjectUtil.putDataProperty(graalObject, Strings.IS_GRAAL_RUNTIME, JSFunction.create(this, isGraalRuntimeFunction(context)), flags);
        if (options.getUnhandledRejectionsMode() == JSContextOptions.UnhandledRejectionsTrackingMode.HANDLER) {
            JSFunctionObject registerFunction = JSFunction.create(this, setUnhandledPromiseRejectionHandlerFunction(context));
            JSObjectUtil.putDataProperty(graalObject, Strings.SET_UNHANDLED_PROMISE_REJECTION_HANDLER, registerFunction, flags);
        }
        putGlobalProperty(Strings.GRAAL, graalObject);
    }

    private static JSFunctionData setUnhandledPromiseRejectionHandlerFunction(JSContext context) {
        return context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SetUnhandledPromiseRejectionHandler, (c) -> {
            return JSFunctionData.createCallOnly(c, new JavaScriptRootNode(c.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object[] args = frame.getArguments();
                    Object handler = null;
                    if (JSArguments.getUserArgumentCount(args) > 0) {
                        Object arg = JSArguments.getUserArgument(args, 0);
                        if (JSRuntime.isCallable(arg)) {
                            handler = arg;
                        } else if (!JSRuntime.isNullOrUndefined(arg)) {
                            throw Errors.createTypeError("Value provided for the unhandled promise rejection handler is not callable");
                        }
                    }
                    getRealm().unhandledPromiseRejectionHandler = handler;
                    return Undefined.instance;
                }
            }.getCallTarget(), 0, Strings.SET_UNHANDLED_PROMISE_REJECTION_HANDLER);
        });
    }

    private static JSFunctionData isGraalRuntimeFunction(JSContext context) {
        return context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.IsGraalRuntime, (c) -> {
            return JSFunctionData.createCallOnly(context, new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    return isGraalRuntime();
                }

                @TruffleBoundary
                private boolean isGraalRuntime() {
                    return Truffle.getRuntime().getName().contains("Graal");
                }
            }.getCallTarget(), 0, Strings.IS_GRAAL_RUNTIME);
        });
    }

    /**
     * Convenience method for defining global data properties with default attributes.
     */
    private void putGlobalProperty(TruffleString key, Object value) {
        putGlobalProperty(key, value, JSAttributes.getDefaultNotEnumerable());
    }

    private void putGlobalProperty(Object key, Object value, int attributes) {
        JSObjectUtil.putDataProperty(getGlobalObject(), key, value, attributes);
    }

    private static void putProperty(JSDynamicObject receiver, Object key, Object value) {
        JSObjectUtil.putDataProperty(receiver, key, value, JSAttributes.getDefaultNotEnumerable());
    }

    private static void setupPredefinedSymbols(JSDynamicObject symbolFunction) {
        putSymbolProperty(symbolFunction, Strings.HAS_INSTANCE, Symbol.SYMBOL_HAS_INSTANCE);
        putSymbolProperty(symbolFunction, Strings.IS_CONCAT_SPREADABLE, Symbol.SYMBOL_IS_CONCAT_SPREADABLE);
        putSymbolProperty(symbolFunction, Strings.ITERATOR, Symbol.SYMBOL_ITERATOR);
        putSymbolProperty(symbolFunction, Strings.ASYNC_ITERATOR, Symbol.SYMBOL_ASYNC_ITERATOR);
        putSymbolProperty(symbolFunction, Strings.MATCH, Symbol.SYMBOL_MATCH);
        putSymbolProperty(symbolFunction, Strings.MATCH_ALL, Symbol.SYMBOL_MATCH_ALL);
        putSymbolProperty(symbolFunction, Strings.REPLACE, Symbol.SYMBOL_REPLACE);
        putSymbolProperty(symbolFunction, Strings.SEARCH, Symbol.SYMBOL_SEARCH);
        putSymbolProperty(symbolFunction, Strings.SPECIES, Symbol.SYMBOL_SPECIES);
        putSymbolProperty(symbolFunction, Strings.SPLIT, Symbol.SYMBOL_SPLIT);
        putSymbolProperty(symbolFunction, Strings.TO_STRING_TAG, Symbol.SYMBOL_TO_STRING_TAG);
        putSymbolProperty(symbolFunction, Strings.TO_PRIMITIVE, Symbol.SYMBOL_TO_PRIMITIVE);
        putSymbolProperty(symbolFunction, Strings.UNSCOPABLES, Symbol.SYMBOL_UNSCOPABLES);
    }

    private static void putSymbolProperty(JSDynamicObject symbolFunction, TruffleString name, Symbol symbol) {
        Properties.putConstantUncached(symbolFunction, name, symbol, JSAttributes.notConfigurableNotEnumerableNotWritable());
    }

    /**
     * Is Java interop enabled in this Context.
     */
    public boolean isJavaInteropEnabled() {
        return getEnv() != null && getEnv().isHostLookupAllowed();
    }

    private void setupJavaInterop() {
        assert isJavaInteropEnabled();
        JSObject java = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putToStringTag(java, JAVA_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, java, JavaBuiltins.BUILTINS);
        if (context.isOptionNashornCompatibilityMode()) {
            JSObjectUtil.putFunctionsFromContainer(this, java, JavaBuiltins.BUILTINS_NASHORN_COMPAT);
        }
        putGlobalProperty(JAVA_CLASS_NAME, java);

        if (getEnv() != null && getEnv().isHostLookupAllowed()) {
            if (JSContextOptions.JAVA_PACKAGE_GLOBALS.getValue(getEnv().getOptions())) {
                javaPackageToPrimitiveFunction = JavaPackage.createToPrimitiveFunction(context, this);
                putGlobalProperty(Strings.UC_PACKAGES, JavaPackage.createInit(this, Strings.EMPTY_STRING));
                putGlobalProperty(Strings.JAVA, JavaPackage.createInit(this, Strings.JAVA));
                putGlobalProperty(Strings.JAVAFX, JavaPackage.createInit(this, Strings.JAVAFX));
                putGlobalProperty(Strings.JAVAX, JavaPackage.createInit(this, Strings.JAVAX));
                putGlobalProperty(Strings.COM, JavaPackage.createInit(this, Strings.COM));
                putGlobalProperty(Strings.ORG, JavaPackage.createInit(this, Strings.ORG));
                putGlobalProperty(Strings.EDU, JavaPackage.createInit(this, Strings.EDU));

                // JavaImporter can only be used with Package objects.
                if (context.isOptionNashornCompatibilityMode()) {
                    putGlobalProperty(JavaImporter.CLASS_NAME, getJavaImporterConstructor());
                }
            }
        }
    }

    private void setupPolyglot() {
        JSObject polyglotObject = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putFunctionsFromContainer(this, polyglotObject, PolyglotBuiltins.BUILTINS);
        if (getContextOptions().isPolyglotEvalFile()) {
            JSObjectUtil.putDataProperty(polyglotObject, Strings.EVAL_FILE, lookupFunction(PolyglotBuiltins.BUILTINS, Strings.EVAL_FILE), JSAttributes.getDefaultNotEnumerable());
        }
        if (getContextOptions().isDebugBuiltin()) {
            JSObjectUtil.putFunctionsFromContainer(this, polyglotObject, PolyglotInternalBuiltins.BUILTINS);
        }
        putGlobalProperty(POLYGLOT_CLASS_NAME, polyglotObject);
    }

    private void addConsoleGlobals() {
        if (getContextOptions().isConsole()) {
            putGlobalProperty(Strings.CONSOLE, preinitConsoleBuiltinObject != null ? preinitConsoleBuiltinObject : createConsoleObject());
        }
    }

    private JSDynamicObject createConsoleObject() {
        JSObject console = JSOrdinary.createInit(this);
        JSObjectUtil.putFunctionsFromContainer(this, console, ConsoleBuiltins.BUILTINS);
        return console;
    }

    private JSDynamicObject createPerformanceObject() {
        JSObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putFunctionsFromContainer(this, obj, PerformanceBuiltins.BUILTINS);
        return obj;
    }

    /**
     * Creates the %IteratorPrototype% object as specified in ES6 25.1.2.
     */
    private JSDynamicObject createIteratorPrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putDataProperty(prototype, Symbol.SYMBOL_ITERATOR, createIteratorPrototypeSymbolIteratorFunction(this), JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    private static JSDynamicObject createIteratorPrototypeSymbolIteratorFunction(JSRealm realm) {
        return JSFunction.create(realm, realm.getContext().getSymbolIteratorThisGetterFunctionData());
    }

    private JSDynamicObject createIteratorHelperPrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, IteratorHelperPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, IteratorHelperPrototypeBuiltins.TO_STRING_TAG);
        return prototype;
    }

    private JSDynamicObject createAsyncIteratorHelperPrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.asyncIteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, AsyncIteratorHelperPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, AsyncIteratorHelperPrototypeBuiltins.TO_STRING_TAG);
        return prototype;
    }

    /**
     * Creates the %RegExpStringIteratorPrototype% object.
     */
    private JSDynamicObject createRegExpStringIteratorPrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, RegExpStringIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, JSString.REGEXP_ITERATOR_CLASS_NAME);
        return prototype;
    }

    /**
     * Creates the prototype object of foreign iterables.
     */
    private JSDynamicObject createForeignIterablePrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, ForeignIterablePrototypeBuiltins.BUILTINS);
        return prototype;
    }

    private JSDynamicObject createForeignIteratorPrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this, this.iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(this, prototype, ForeignIteratorPrototypeBuiltins.BUILTINS);
        return prototype;
    }

    private JSConstructor createAbstractModuleSourcePrototype() {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(this);
        JSFunctionObject constructor = lookupFunction(ConstructorBuiltins.BUILTINS, ConstructorBuiltins.Constructor.AbstractModuleSource.getKey());
        JSObjectUtil.putConstructorPrototypeProperty(constructor, prototype);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putAccessorsFromContainer(this, prototype, AbstractModuleSourcePrototype.BUILTINS);
        return new JSConstructor(constructor, prototype);
    }

    public JSFunctionObject getAbstractModuleSourceConstructor() {
        return abstractModuleSourceConstructor;
    }

    public JSDynamicObject getAbstractModuleSourcePrototype() {
        return abstractModuleSourcePrototype;
    }

    public JSDynamicObject getArrayProtoValuesIterator() {
        return arrayProtoValuesIterator;
    }

    private JSDynamicObject createReflect() {
        JSObject obj = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putToStringTag(obj, REFLECT_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, ReflectBuiltins.BUILTINS);
        return obj;
    }

    private JSDynamicObject createAtomics() {
        JSObject obj = JSObjectUtil.createOrdinaryPrototypeObject(this, this.getObjectPrototype());
        JSObjectUtil.putToStringTag(obj, ATOMICS_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, AtomicsBuiltins.BUILTINS);
        if (getContextOptions().isAtomicsWaitAsync()) {
            JSObjectUtil.putFunctionFromContainer(this, obj, AtomicsBuiltins.BUILTINS, AtomicsBuiltins.Atomics.waitAsync.getKey());
        }
        return obj;
    }

    public final JSFunctionObject getCallSiteConstructor() {
        return callSiteConstructor;
    }

    public final JSDynamicObject getCallSitePrototype() {
        return callSitePrototype;
    }

    public final JSDynamicObject getGlobalScope() {
        return globalScope;
    }

    public JSDynamicObject getScriptEngineImportScope() {
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

        if (getContext().getParserOptions().scripting()) {
            // $OPTIONS
            String timezone = getLocalTimeZoneId().getId();
            JSDynamicObject timezoneObj = JSOrdinary.create(context, this);
            JSObjectUtil.putDataProperty(timezoneObj, Strings.CAPS_ID, Strings.fromJavaString(timezone), JSAttributes.configurableEnumerableWritable());

            JSDynamicObject optionsObj = JSOrdinary.create(context, this);
            JSObjectUtil.putDataProperty(optionsObj, Strings._TIMEZONE, timezoneObj, JSAttributes.configurableEnumerableWritable());
            JSObjectUtil.putDataProperty(optionsObj, Strings._SCRIPTING, true, JSAttributes.configurableEnumerableWritable());
            JSObjectUtil.putDataProperty(optionsObj, Strings._COMPILE_ONLY, false, JSAttributes.configurableEnumerableWritable());

            putGlobalProperty(Strings.$_OPTIONS, optionsObj, JSAttributes.configurableNotEnumerableWritable());

            // $ARG
            JSDynamicObject arguments = JSArray.createConstant(context, this, Strings.fromJavaStringArray(getEnv().getApplicationArguments()));

            putGlobalProperty(Strings.$_ARG, arguments, JSAttributes.configurableNotEnumerableWritable());

            // $ENV
            JSDynamicObject envObj = JSOrdinary.create(context, this);
            Map<String, String> sysenv = getEnv().getEnvironment();
            for (Map.Entry<String, String> entry : sysenv.entrySet()) {
                JSObjectUtil.defineDataProperty(context, envObj, Strings.fromJavaString(entry.getKey()), Strings.fromJavaString(entry.getValue()), JSAttributes.configurableEnumerableWritable());
            }

            putGlobalProperty(Strings.DOLLAR_ENV, envObj, JSAttributes.configurableNotEnumerableWritable());

            // $EXEC
            putGlobalProperty(Strings.$_EXEC, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.EXEC));
            putGlobalProperty(Strings.READ_FULLY, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.READ_FULLY));
            putGlobalProperty(Strings.READ_LINE, lookupFunction(GlobalBuiltins.GLOBAL_NASHORN_EXTENSIONS, Strings.READ_LINE));

            // $OUT, $ERR, $EXIT
            putGlobalProperty(Strings.$_EXIT, Undefined.instance);
            putGlobalProperty(Strings.$_OUT, Undefined.instance);
            putGlobalProperty(Strings.$_ERR, Undefined.instance);
        }
    }

    public void setRealmBuiltinObject(JSDynamicObject realmBuiltinObject) {
        if (this.realmBuiltinObject == null && realmBuiltinObject != null) {
            this.realmBuiltinObject = realmBuiltinObject;
            putGlobalProperty(REALM_BUILTIN_CLASS_NAME, realmBuiltinObject);
        }
    }

    public void initRealmBuiltinObject() {
        assert getContextOptions().isV8RealmBuiltin();
        setRealmBuiltinObject(createRealmBuiltinObject());
    }

    private JSObject createRealmBuiltinObject() {
        JSObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(obj, REALM_BUILTIN_CLASS_NAME);
        JSObjectUtil.putProxyProperty(obj, REALM_SHARED_NAME, REALM_SHARED_PROXY, JSAttributes.getDefault());
        JSObjectUtil.putFunctionsFromContainer(this, obj, RealmFunctionBuiltins.BUILTINS);
        return obj;
    }

    private JSObject createDebugObject() {
        JSObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(obj, DEBUG_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, DebugBuiltins.BUILTINS);
        return obj;
    }

    private JSObject createMleObject() {
        JSObject obj = JSOrdinary.createInit(this);
        JSObjectUtil.putToStringTag(obj, MLE_CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(this, obj, MLEBuiltins.BUILTINS);
        return obj;
    }

    private void addStaticRegexResultProperties() {
        if (context.isOptionRegexpStaticResult()) {
            if (context.isOptionNashornCompatibilityMode()) {
                putRegExpStaticPropertyAccessor(null, Strings.INPUT);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpMultiLine, JSRegExp.MULTILINE);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, JSRegExp.LAST_MATCH);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, JSRegExp.LAST_PAREN);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, JSRegExp.LEFT_CONTEXT);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, JSRegExp.RIGHT_CONTEXT);
            } else {
                putRegExpStaticPropertyAccessor(null, Strings.INPUT);
                putRegExpStaticPropertyAccessor(null, Strings.INPUT, JSRegExp.$_);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, JSRegExp.LAST_MATCH);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastMatch, JSRegExp.LAST_MATCH, JSRegExp.$_AMPERSAND);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, JSRegExp.LAST_PAREN);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLastParen, JSRegExp.LAST_PAREN, JSRegExp.$_PLUS);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, JSRegExp.LEFT_CONTEXT);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpLeftContext, JSRegExp.LEFT_CONTEXT, JSRegExp.$_BACKTICK);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, JSRegExp.RIGHT_CONTEXT);
                putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExpRightContext, JSRegExp.RIGHT_CONTEXT, JSRegExp.$_SQUOT);
            }
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$1, JSRegExp.$_1);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$2, JSRegExp.$_2);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$3, JSRegExp.$_3);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$4, JSRegExp.$_4);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$5, JSRegExp.$_5);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$6, JSRegExp.$_6);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$7, JSRegExp.$_7);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$8, JSRegExp.$_8);
            putRegExpStaticPropertyAccessor(BuiltinFunctionKey.RegExp$9, JSRegExp.$_9);
        }
    }

    private void putRegExpStaticPropertyAccessor(BuiltinFunctionKey builtinKey, TruffleString getterName) {
        putRegExpStaticPropertyAccessor(builtinKey, getterName, getterName);
    }

    private void putRegExpStaticPropertyAccessor(BuiltinFunctionKey builtinKey, TruffleString getterName, TruffleString propertyName) {
        Pair<JSBuiltin, JSBuiltin> pair = RegExpBuiltins.BUILTINS.lookupAccessorByKey(getterName);
        JSBuiltin getterBuiltin = pair.getLeft();
        JSDynamicObject getter = JSFunction.create(this, getterBuiltin.createFunctionData(context));

        JSDynamicObject setter;
        JSBuiltin setterBuiltin = pair.getRight();
        if (setterBuiltin != null) {
            assert Strings.equals(propertyName, Strings.INPUT) || Strings.equals(propertyName, JSRegExp.$_);
            setter = JSFunction.create(this, setterBuiltin.createFunctionData(context));
        } else if (context.isOptionV8CompatibilityMode()) {
            // set empty setter for V8 compatibility, see testv8/mjsunit/regress/regress-5566.js
            TruffleString setterName = Strings.concat(Strings.SET_SPC, getterName);
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

    public void setArguments(TruffleString[] arguments) {
        JSObjectUtil.defineDataProperty(context, getGlobalObject(), ARGUMENTS_NAME, JSArray.createConstant(context, this, arguments),
                        context.isOptionV8CompatibilityMode() ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable());
    }

    public final JSDynamicObject getOrdinaryHasInstanceFunction() {
        return ordinaryHasInstanceFunction;
    }

    public final JSFunctionObject getJSAdapterConstructor() {
        return jsAdapterConstructor;
    }

    public final JSDynamicObject getJSAdapterPrototype() {
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
        getContextOptions().setOptionValues(newEnv.getSandboxPolicy(), newEnv.getOptions());
        getContext().updateStableOptions(contextOptions, StableContextOptionValue.UpdateKind.PATCH);

        setOutputStreamsFromEnv(newEnv);

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

        // Reset usage metadata for singleton Symbols.
        getContext().resetSymbolUsageMarker();

        return true;
    }

    public void initialize() {
        CompilerAsserts.neverPartOfCompilation();
        if (getEnv().isPreInitialization()) {
            preinitializeObjects();
            return;
        }

        setOutputStreamsFromEnv(getEnv());

        addOptionalGlobals();

        addArgumentsFromEnv(getEnv());

        initTimeOffsetAndRandom();

        addStaticRegexResultProperties();
    }

    private void setOutputStreamsFromEnv(TruffleLanguage.Env newEnv) {
        if (newEnv.out() != outputWriter.getDelegate()) {
            setOutputWriter(newEnv.out());
        }
        if (newEnv.err() != errorWriter.getDelegate()) {
            setErrorWriter(newEnv.err());
        }
    }

    private void preinitializeObjects() {
        preinitIntlObject = createIntlObject();
        preinitConsoleBuiltinObject = createConsoleObject();
        preinitPerformanceObject = createPerformanceObject();
    }

    private void addArgumentsFromEnv(TruffleLanguage.Env newEnv) {
        String[] applicationArguments = newEnv.getApplicationArguments();
        if (getContextOptions().isGlobalArguments()) {
            TruffleString[] args = new TruffleString[applicationArguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = Strings.fromJavaString(applicationArguments[i]);
            }
            setArguments(args);
        }
    }

    @TruffleBoundary
    public JSRealm createChildRealm() {
        JSRealm childRealm = context.createRealm(getEnv(), this);
        childRealm.initialize();
        return childRealm;
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

    public Object getStaticRegexResult(JSContext ctx, Node node, TRegexUtil.InvokeExecMethodNode invokeExec) {
        CompilerAsserts.partialEvaluationConstant(ctx);
        assert ctx.isOptionRegexpStaticResult();
        if (staticRegexResultCompiledRegex != null && ctx.getRegExpStaticResultUnusedAssumption().isValid()) {
            // switch from lazy to eager static RegExp result
            ctx.getRegExpStaticResultUnusedAssumption().invalidate();
            staticRegexResult = TRegexCompiledRegexAccessor.exec(staticRegexResultCompiledRegex, staticRegexResultOriginalInputString, staticRegexResultFromIndex, node, invokeExec);
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
    public void setStaticRegexResult(JSContext ctx, Object compiledRegex, TruffleString input, long fromIndex, Object result) {
        CompilerAsserts.partialEvaluationConstant(ctx);
        assert ctx.isOptionRegexpStaticResult();
        staticRegexResultInvalidated = false;
        staticRegexResultCompiledRegex = compiledRegex;
        staticRegexResultInputString = input;
        staticRegexResultOriginalInputString = input;
        if (ctx.getRegExpStaticResultUnusedAssumption().isValid()) {
            staticRegexResultFromIndex = fromIndex;
        } else {
            assert TRegexUtil.InteropReadBooleanMemberNode.getUncached().execute(null, result, TRegexUtil.Props.RegexResult.IS_MATCH);
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

    public TruffleString getStaticRegexResultInputString() {
        return staticRegexResultInputString;
    }

    public void setStaticRegexResultInputString(TruffleString inputString) {
        staticRegexResultInputString = inputString;
    }

    public TruffleString getStaticRegexResultOriginalInputString() {
        return staticRegexResultOriginalInputString;
    }

    public OptionValues getOptions() {
        return getEnv().getOptions();
    }

    /**
     * Returns the environment's output stream as a PrintWriter.
     */
    public final PrintWriter getOutputWriter() {
        return outputWriter;
    }

    /**
     * Returns the environment's error stream as a PrintWriter.
     */
    public final PrintWriter getErrorWriter() {
        return errorWriter;
    }

    private void setOutputWriter(OutputStream stream) {
        this.outputWriter.setDelegate(stream);
    }

    private void setErrorWriter(OutputStream stream) {
        this.errorWriter.setDelegate(stream);
    }

    /**
     * The current time in nanoseconds precision (with fuzzed resolution for security reasons).
     * Counted from the start of the application, as required by Node.js' `performance.now()`.
     */
    public long nanoTime() {
        long ns = System.nanoTime() + nanoToZeroTimeOffset;
        return updateResolution(ns);
    }

    /**
     * The current time in nanoseconds precision (with fuzzed resolution for security reasons). Wall
     * clock time, to be in the same range as ECMAScript's `Date.now()`.
     */
    public long nanoTimeWallClock() {
        Instant instant = Instant.now();
        long ns = instant.getEpochSecond() * NANOSECONDS_PER_SECOND + instant.getNano();
        return updateResolution(ns);
    }

    private long updateResolution(long nanos) {
        long ns = nanos;
        long resolution = getContext().getTimerResolution();
        if (resolution > 0) {
            return Math.floorDiv(ns, resolution) * resolution;
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

    @TruffleBoundary
    public long currentTimeMillis() {
        return Math.floorDiv(nanoTimeWallClock(), NANOSECONDS_PER_MILLISECOND);
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
            JSModuleLoader loader = null;
            switch (getContextOptions().getModuleLoaderFactoryMode()) {
                case HANDLER -> loader = loadCustomModuleLoaderOrFallBack();
                case DEFAULT -> loader = createStandardModuleLoader(this);
            }
            assert loader != null;
            moduleLoader = loader;
        }
    }

    private JSModuleLoader loadCustomModuleLoaderOrFallBack() {
        JSModuleLoaderFactory fac = JSEngine.getModuleLoaderFactory();
        if (fac == null) {
            return createStandardModuleLoader(this);
        }
        var loader = fac.createLoader(this);
        if (loader == null) {
            return createStandardModuleLoader(this);
        }
        return loader;
    }

    private static JSModuleLoader createStandardModuleLoader(JSRealm realm) {
        if (realm.getContextOptions().isCommonJSRequire()) {
            return NpmCompatibleESModuleLoader.create(realm);
        }
        return DefaultESModuleLoader.create(realm);
    }

    public final JSAgent getAgent() {
        assert agent != null;
        return agent;
    }

    public void setAgent(JSAgent newAgent) {
        CompilerAsserts.neverPartOfCompilation("Assigning agent to context in compiled code");
        this.agent = Objects.requireNonNull(newAgent, "agent");
    }

    public TimeZone getLocalTimeZone() {
        TimeZone timeZone = localTimeZone;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, timeZone == null)) {
            timeZone = getICUTimeZoneFromEnv();
        }
        return timeZone;
    }

    @TruffleBoundary
    private TimeZone getICUTimeZoneFromEnv() {
        return IntlUtil.getICUTimeZone(getLocalTimeZoneId(), getContext());
    }

    public ZoneId getLocalTimeZoneId() {
        ZoneId id = localTimeZoneId;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, id == null)) {
            id = getTimeZoneFromEnv();
            localTimeZoneId = id;
        }
        return id;
    }

    @TruffleBoundary
    private ZoneId getTimeZoneFromEnv() {
        OptionValues options = getEnv().getOptions();
        String zoneId = JSContextOptions.TIME_ZONE.getValue(options);
        if (!zoneId.isEmpty()) {
            try {
                return ZoneId.of(zoneId);
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
        lastFuzzyTime = Long.MIN_VALUE;
    }

    public final SplittableRandom getRandom() {
        return random;
    }

    public JSRealm getParent() {
        return parentRealm;
    }

    public boolean isMainRealm() {
        return getParent() == null;
    }

    public JavaScriptBaseNode getCallNode() {
        return callNode;
    }

    public void setCallNode(JavaScriptBaseNode callNode) {
        this.callNode = callNode;
    }

    void initRealmList() {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        realmList = new ArrayList<>();
    }

    void addToRealmList(JSRealm newRealm) {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        assert !realmList.contains(newRealm);
        realmList.add(newRealm);
    }

    public JSRealm getFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        return (0 <= idx && idx < realmList.size()) ? realmList.get(idx) : null;
    }

    public void setInRealmList(int idx, JSRealm realm) {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        realmList.set(idx, realm);
    }

    public int getIndexFromRealmList(JSRealm rlm) {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        return realmList.indexOf(rlm);
    }

    public void removeFromRealmList(int idx) {
        CompilerAsserts.neverPartOfCompilation();
        assert isMainRealm();
        realmList.set(idx, null);
    }

    public JSRealm getCurrentV8Realm() {
        assert isMainRealm();
        return v8RealmCurrent;
    }

    public void setCurrentV8Realm(JSRealm realm) {
        assert isMainRealm();
        v8RealmCurrent = realm;
    }

    private static final TruffleString REALM_SHARED_NAME = Strings.SHARED;
    private static final PropertyProxy REALM_SHARED_PROXY = new RealmSharedPropertyProxy();

    public void registerCustomEsmPathMappingCallback(Object callback) {
        assert context.isOptionMleBuiltin();
        assert JSRuntime.isCallableForeign(callback);
        this.customEsmPathMappingCallback = callback;
    }

    public TruffleString getCustomEsmPathMapping(TruffleString refPath, TruffleString specifier) {
        CompilerAsserts.neverPartOfCompilation();
        if (getContext().isOptionMleBuiltin() && customEsmPathMappingCallback != null) {
            Object[] args = new Object[]{JSRuntime.toJSNull(refPath), specifier};
            Object custom = JSInteropUtil.call(customEsmPathMappingCallback, args);
            InteropLibrary interopLibrary = InteropLibrary.getUncached();
            if (interopLibrary.isString(custom)) {
                return Strings.interopAsTruffleString(custom);
            } else {
                throw Errors.createError("Cannot load ES module: " + specifier);
            }
        }
        return null;
    }

    private static final class RealmSharedPropertyProxy extends PropertyProxy {
        @Override
        public Object get(JSDynamicObject store) {
            return topLevelRealm().v8RealmShared;
        }

        @Override
        public boolean set(JSDynamicObject store, Object value) {
            topLevelRealm().v8RealmShared = value;
            return true;
        }

        private static JSRealm topLevelRealm() {
            return JSRealm.getMain(null);
        }
    }

    public boolean joinStackPush(Object o, Node node, InlinedBranchProfile growProfile) {
        InteropLibrary interop = (o instanceof JSObject) ? null : InteropLibrary.getFactory().getUncached(o);
        for (int i = 0; i < joinStack.size(); i++) {
            Object element = joinStack.get(i);
            if ((interop == null) ? (o == element) : interop.isIdentical(o, element, InteropLibrary.getFactory().getUncached(element))) {
                return false;
            }
        }
        joinStack.add(o, node, growProfile);
        return true;
    }

    public void joinStackPop() {
        joinStack.pop();
    }

    public final Map<TruffleFile, JSDynamicObject> getCommonJSRequireCache() {
        assert getContextOptions().isCommonJSRequire();
        return commonJSRequireCache;
    }

    private boolean isWasmAvailable() {
        return truffleLanguageEnv.isPolyglotBindingsAccessAllowed() && truffleLanguageEnv.getInternalLanguages().get("wasm") != null;
    }

    public Object getWASMModuleInstantiate() {
        return wasmModuleInstantiate;
    }

    public Object getWASMModuleExports() {
        return wasmModuleExports;
    }

    public Object getWASMModuleImports() {
        return wasmModuleImports;
    }

    public Object getWASMCustomSections() {
        return wasmCustomSections;
    }

    public Object getWASMTableAlloc() {
        return wasmTableAlloc;
    }

    public Object getWASMTableGrow() {
        return wasmTableGrow;
    }

    public Object getWASMTableRead() {
        return wasmTableRead;
    }

    public Object getWASMTableWrite() {
        return wasmTableWrite;
    }

    public Object getWASMTableLength() {
        return wasmTableLength;
    }

    public Object getWASMFuncType() {
        return wasmFuncType;
    }

    public Object getWASMIsFunc() {
        return wasmIsFunc;
    }

    public Object getWASMMemAlloc() {
        return wasmMemAlloc;
    }

    public Object getWASMMemGrow() {
        return wasmMemGrow;
    }

    public Object getWASMGlobalAlloc() {
        return wasmGlobalAlloc;
    }

    public Object getWASMGlobalRead() {
        return wasmGlobalRead;
    }

    public Object getWASMGlobalWrite() {
        return wasmGlobalWrite;
    }

    public Object getWASMInstanceExport() {
        return wasmInstanceExport;
    }

    public Object getWASMEmbedderDataGet() {
        return wasmEmbedderDataGet;
    }

    public Object getWASMEmbedderDataSet() {
        return wasmEmbedderDataSet;
    }

    public Object getWASMMemAsByteBuffer() {
        return wasmMemAsByteBuffer;
    }

    public Object getWasmRefNull() {
        return wasmRefNull;
    }

    public JSFunctionObject getWebAssemblyModuleConstructor() {
        return webAssemblyModuleConstructor;
    }

    public JSDynamicObject getWebAssemblyModulePrototype() {
        return webAssemblyModulePrototype;
    }

    public JSDynamicObject getWebAssemblyInstancePrototype() {
        return webAssemblyInstancePrototype;
    }

    public JSDynamicObject getWebAssemblyMemoryPrototype() {
        return webAssemblyMemoryPrototype;
    }

    public JSDynamicObject getWebAssemblyTablePrototype() {
        return webAssemblyTablePrototype;
    }

    public JSDynamicObject getWebAssemblyGlobalPrototype() {
        return webAssemblyGlobalPrototype;
    }

    public JSDynamicObject getTextDecoderPrototype() {
        return textDecoderPrototype;
    }

    public JSDynamicObject getTextEncoderPrototype() {
        return textEncoderPrototype;
    }

    public JSDynamicObject getForeignIterablePrototype() {
        return foreignIterablePrototype;
    }

    public JSDynamicObject getForeignIteratorPrototype() {
        return foreignIteratorPrototype;
    }

    public DateFormat getJSDateISOFormat(double time) {
        long milliseconds = (long) time;
        if (milliseconds < -62167219200000L) {
            if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, jsDateFormatBeforeYear0 == null)) {
                enterOncePerContextBranch();
                jsDateFormatBeforeYear0 = createDateFormat("uuuuuu-MM-dd'T'HH:mm:ss.SSS'Z'", false);
            }
            return jsDateFormatBeforeYear0;
        } else if (milliseconds >= 253402300800000L) {
            if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, jsDateFormatAfterYear9999 == null)) {
                enterOncePerContextBranch();
                jsDateFormatAfterYear9999 = createDateFormat("+uuuuuu-MM-dd'T'HH:mm:ss.SSS'Z'", false);
            }
            return jsDateFormatAfterYear9999;
        } else {
            if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, jsDateFormat == null)) {
                enterOncePerContextBranch();
                jsDateFormat = createDateFormat("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", false);
            }
            return jsDateFormat;
        }
    }

    public DateFormat getJSDateUTCFormat() {
        DateFormat dateFormat = jsDateFormatISO;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            enterOncePerContextBranch();
            jsDateFormatISO = dateFormat = createDateFormat("EEE, dd MMM uuuu HH:mm:ss 'GMT'", false);
        }
        return dateFormat;
    }

    public DateFormat getJSShortDateFormat() {
        DateFormat dateFormat = jsShortDateFormat;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            jsShortDateFormat = dateFormat = createDateFormat("EEE MMM dd uuuu", true);
        }
        return dateFormat;
    }

    public DateFormat getJSShortDateLocalFormat() {
        DateFormat dateFormat = jsShortDateLocalFormat;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            jsShortDateLocalFormat = dateFormat = createDateFormat("uuuu-MM-dd", true);
        }
        return dateFormat;
    }

    public DateFormat getJSShortTimeFormat() {
        DateFormat dateFormat = jsShortTimeFormat;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            jsShortTimeFormat = dateFormat = createDateFormat(appendTimeZoneNameFormat("HH:mm:ss 'GMT'xx"), true);
        }
        return dateFormat;
    }

    public DateFormat getJSShortTimeLocalFormat() {
        DateFormat dateFormat = jsShortTimeLocalFormat;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            // no UTC
            jsShortTimeLocalFormat = dateFormat = createDateFormat("HH:mm:ss", true);
        }
        return dateFormat;
    }

    public DateFormat getDateToStringFormat() {
        DateFormat dateFormat = jsDateToStringFormat;
        if (CompilerDirectives.injectBranchProbability(SLOWPATH_PROBABILITY, dateFormat == null)) {
            jsDateToStringFormat = dateFormat = createDateFormat(appendTimeZoneNameFormat("EEE MMM dd uuuu HH:mm:ss 'GMT'xx"), true);
        }
        return dateFormat;
    }

    @TruffleBoundary
    private String appendTimeZoneNameFormat(String format) {
        String timeZoneNameFormat = getContext().isOptionV8CompatibilityMode() ? "zzzz" : "z";
        return format + " (" + timeZoneNameFormat + ")";
    }

    @TruffleBoundary
    private DateFormat createDateFormat(String pattern, boolean local) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(local ? getLocalTimeZone() : TimeZone.GMT_ZONE);

        // TZDBTimeZoneNames provides short names only => do not use it when
        // long names are needed
        if (!pattern.contains("zzzz")) {
            TimeZoneFormat tzFormat = format.getTimeZoneFormat().cloneAsThawed();
            tzFormat.setTimeZoneNames(TimeZoneNames.getTZDBInstance(ULocale.US));
            format.setTimeZoneFormat(tzFormat);
        }

        Calendar calendar = format.getCalendar();
        if (calendar instanceof GregorianCalendar) {
            // Ensure that Gregorian calendar is used for all dates.
            // GregorianCalendar used by SimpleDateFormat is using
            // Julian calendar for dates before 1582 otherwise.
            ((GregorianCalendar) calendar).setGregorianChange(new Date(Long.MIN_VALUE));
        }
        return format;
    }

    @TruffleBoundary
    public void setLocalTimeZone(String tzId) {
        ZoneId newZoneId;
        TimeZone newTimeZone;
        try {
            if (tzId != null) {
                newZoneId = ZoneId.of(tzId);
                newTimeZone = IntlUtil.getICUTimeZone(tzId, getContext());
            } else {
                // Reset to default time zone (fields are reinitialized on next use).
                newZoneId = null;
                newTimeZone = null;
            }
        } catch (DateTimeException e) {
            // If new time zone is invalid/unknown, do not update anything.
            return;
        }
        localTimeZoneId = newZoneId;
        localTimeZone = newTimeZone;

        // Clear local time zone dependent date/time formats, so that they are updated on next use.
        jsDateToStringFormat = null;
        jsShortTimeFormat = null;
        jsShortTimeLocalFormat = null;
        jsShortDateFormat = null;
        jsShortDateLocalFormat = null;
    }

    /**
     * Used in lazy initialization branch of compilation final fields that are set once per context.
     * Transfers to interpreter if this branch is entered with a single constant context.
     */
    private void enterOncePerContextBranch() {
        if (CompilerDirectives.isPartialEvaluationConstant(this)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
    }

    public Charset getCharset() {
        return charset;
    }

    @TruffleBoundary
    private Charset getCharsetImpl() {
        String name = getContextOptions().getCharset();
        if (name.isEmpty()) {
            return Charset.defaultCharset();
        } else {
            return Charset.forName(name);
        }
    }

    public long nextAsyncEvaluationOrder() {
        return ++lastAsyncEvaluationOrder;
    }

    @TruffleBoundary
    public void putCachedCompiledRegex(Source regexSource, Object compiledRegex) {
        int regexCacheSize = getContextOptions().getRegexCacheSize();
        if (regexCacheSize > 0) {
            if (compiledRegexCache == null) {
                compiledRegexCache = new LRUCache<>(regexCacheSize);
            }
            compiledRegexCache.put(regexSource, compiledRegex);
        }
    }

    @TruffleBoundary
    public Object getCachedCompiledRegex(Source regexSource) {
        int regexCacheSize = getContextOptions().getRegexCacheSize();
        if (regexCacheSize > 0) {
            if (compiledRegexCache != null) {
                return compiledRegexCache.get(regexSource);
            }
        }
        return null;
    }

    public void storeParentPromise(JSDynamicObject promise) {
        parentPromise = promise;
    }

    public JSDynamicObject fetchParentPromise() {
        JSDynamicObject parent = parentPromise;
        if (parent == null) {
            parent = Undefined.instance;
        } else {
            parentPromise = null;
        }
        return parent;
    }

    public int getOperatorCounter() {
        assert isMainRealm();
        return operatorCounter;
    }

    public void incOperatorCounter() {
        assert isMainRealm();
        operatorCounter++;
    }
}
