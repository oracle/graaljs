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
package com.oracle.truffle.js.builtins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import org.graalvm.polyglot.io.ByteSequence;

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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.AssumedValue;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.AbstractClassConstructorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallNumberNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallRequiresNewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructAggregateErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayBufferNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructAsyncIteratorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructCollatorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDataViewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDisplayNamesNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFinalizationRegistryNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructIteratorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJSAdapterNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJSProxyNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJavaImporterNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructListFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructLocaleNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructNumberFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructNumberNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructObjectNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructPluralRulesNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructRegExpNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructRelativeTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSegmenterNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructShadowRealmNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalDurationNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalInstantNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakRefNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyGlobalNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyInstanceNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyMemoryNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyModuleNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyTableNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWorkerNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CreateDynamicFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.PromiseConstructorNodeGen;
import com.oracle.truffle.js.builtins.helper.CanBeHeldWeaklyNode;
import com.oracle.truffle.js.builtins.json.JSONBuiltins;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyBuiltins;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode.ArrayContentType;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.access.InstallErrorCauseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IsRegExpNode;
import com.oracle.truffle.js.nodes.access.IterableToListNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.ArrayCreateNode;
import com.oracle.truffle.js.nodes.cast.JSNumericToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerThrowOnInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.ToArrayLengthNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.intl.CreateRegExpNode;
import com.oracle.truffle.js.nodes.intl.GetBooleanOptionNode;
import com.oracle.truffle.js.nodes.intl.InitializeCollatorNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializeDisplayNamesNode;
import com.oracle.truffle.js.nodes.intl.InitializeListFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializeLocaleNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializePluralRulesNode;
import com.oracle.truffle.js.nodes.intl.InitializeRelativeTimeFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializeSegmenterNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveThenableNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarSlotValueNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.wasm.ExportByteSourceNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyIndexOrSizeNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.WorkerAgent;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferViewBase;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSBooleanObject;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.builtins.JSWorker;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollatorObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormatObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSDisplayNames;
import com.oracle.truffle.js.runtime.builtins.intl.JSDisplayNamesObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormatObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocaleObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormatObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRulesObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormatObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenterObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.builtins.wasm.WebAssemblyValueType;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.LRUCache;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Contains built-in constructor functions.
 */
public final class ConstructorBuiltins extends JSBuiltinsContainer.SwitchEnum<ConstructorBuiltins.Constructor> {
    public static final JSBuiltinsContainer BUILTINS = new ConstructorBuiltins();

    protected ConstructorBuiltins() {
        super(null, Constructor.class);
    }

    public enum Constructor implements BuiltinEnum<Constructor> {
        Array(1),
        Boolean(1),
        Date(7),
        RegExp(2),
        String(1),
        Object(1),
        Number(1),
        BigInt(1),
        Function(1),
        ArrayBuffer(1),

        // Intl
        Collator(0),
        NumberFormat(0),
        ListFormat(0),
        PluralRules(0),
        DateTimeFormat(0),
        RelativeTimeFormat(0),
        Segmenter(0),
        DisplayNames(2),
        Locale(1),

        Error(1),
        RangeError(1),
        TypeError(1),
        ReferenceError(1),
        SyntaxError(1),
        EvalError(1),
        URIError(1),
        AggregateError(2),

        // WebAssembly
        CompileError(1),
        LinkError(1),
        RuntimeError(1),

        Int8Array(3),
        Uint8Array(3),
        Uint8ClampedArray(3),
        Int16Array(3),
        Uint16Array(3),
        Int32Array(3),
        Uint32Array(3),
        Float16Array(3),
        Float32Array(3),
        Float64Array(3),
        BigInt64Array(3),
        BigUint64Array(3),
        DataView(1),

        Map(0),
        Set(0),
        WeakRef(1),
        FinalizationRegistry(1),
        WeakMap(0),
        WeakSet(0),
        Iterator(0),
        AsyncIterator(0),
        GeneratorFunction(1),
        Proxy(2),
        Promise(1),

        AsyncFunction(1),
        SharedArrayBuffer(1),
        AsyncGeneratorFunction(1),

        // WebAssembly
        Global(1),
        Instance(1),
        Memory(1),
        Module(1),
        Table(1),

        ShadowRealm(0),

        Worker(2),

        // Temporal
        PlainTime(0),
        PlainDate(3),
        PlainDateTime(3),
        Duration(0),
        PlainYearMonth(2),
        PlainMonthDay(2),
        Instant(1),
        ZonedDateTime(2),

        // --- not new.target-capable below ---
        TypedArray(0),
        Symbol(0),
        AbstractModuleSource(0),

        // non-standard (Nashorn) extensions
        JSAdapter(1),
        JavaImporter(1);

        private final int length;

        Constructor(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isConstructor() {
            return true;
        }

        @Override
        public boolean isNewTargetConstructor() {
            return EnumSet.range(Array, ZonedDateTime).contains(this);
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case Map, Set, WeakMap, WeakSet, GeneratorFunction, Proxy, Promise, TypedArray, Symbol -> JSConfig.ECMAScript2015;
                case AsyncFunction, SharedArrayBuffer -> JSConfig.ECMAScript2017;
                case AsyncGeneratorFunction -> JSConfig.ECMAScript2018;
                case WeakRef, FinalizationRegistry -> JSConfig.ECMAScript2021;
                case PlainTime, Duration, PlainDate, PlainDateTime, PlainYearMonth, PlainMonthDay, Instant, ZonedDateTime -> JSConfig.StagingECMAScriptVersion;
                case Iterator, AsyncIterator -> JSConfig.StagingECMAScriptVersion;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Constructor builtinEnum) {
        switch (builtinEnum) {
            case Array:
                return ConstructArrayNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
            case Boolean:
                return construct
                                ? ConstructBooleanNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context))
                                : CallBooleanNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case Date:
                return construct
                                ? ConstructDateNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context))
                                : CallDateNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case RegExp:
                return construct
                                ? ConstructRegExpNodeGen.create(context, builtin, false, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : ConstructRegExpNodeGen.create(context, builtin, true, false, args().function().fixedArgs(2).createArgumentNodes(context));
            case String:
                return construct
                                ? ConstructStringNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context))
                                : CallStringNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));

            case WeakRef:
                if (construct) {
                    return ConstructWeakRefNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case FinalizationRegistry:
                if (construct) {
                    return ConstructFinalizationRegistryNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case Collator:
                return ConstructCollatorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
            case ListFormat:
                return construct
                                ? ConstructListFormatNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case NumberFormat:
                return ConstructNumberFormatNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
            case PluralRules:
                return construct
                                ? ConstructPluralRulesNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case DateTimeFormat:
                return ConstructDateTimeFormatNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
            case RelativeTimeFormat:
                return construct
                                ? ConstructRelativeTimeFormatNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Segmenter:
                return construct
                                ? ConstructSegmenterNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case DisplayNames:
                return construct
                                ? ConstructDisplayNamesNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Locale:
                return construct
                                ? ConstructLocaleNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Object:
                return ConstructObjectNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
            case Number:
                return construct
                                ? ConstructNumberNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context))
                                : CallNumberNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case BigInt:
                return construct ? ConstructBigIntNodeGen.create(context, builtin, args().createArgumentNodes(context))
                                : CallBigIntNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case Function:
                return ConstructFunctionNodeGen.create(context, builtin, false, false, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
            case ArrayBuffer:
                if (construct) {
                    return ConstructArrayBufferNodeGen.create(context, builtin, false, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case Error:
            case RangeError:
            case TypeError:
            case ReferenceError:
            case SyntaxError:
            case EvalError:
            case URIError:
            case CompileError:
            case LinkError:
            case RuntimeError:
                return ConstructErrorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
            case AggregateError:
                return ConstructAggregateErrorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(3).createArgumentNodes(context));

            case TypedArray:
            case AbstractModuleSource:
                return AbstractClassConstructorNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case Int8Array:
            case Uint8Array:
            case Uint8ClampedArray:
            case Int16Array:
            case Uint16Array:
            case Int32Array:
            case Uint32Array:
            case Float16Array:
            case Float32Array:
            case Float64Array:
            case BigInt64Array:
            case BigUint64Array:
                if (construct) {
                    TypedArrayFactory typedArray = switch (builtinEnum) {
                        case Int8Array -> TypedArrayFactory.Int8Array;
                        case Uint8Array -> TypedArrayFactory.Uint8Array;
                        case Uint8ClampedArray -> TypedArrayFactory.Uint8ClampedArray;
                        case Int16Array -> TypedArrayFactory.Int16Array;
                        case Uint16Array -> TypedArrayFactory.Uint16Array;
                        case Int32Array -> TypedArrayFactory.Int32Array;
                        case Uint32Array -> TypedArrayFactory.Uint32Array;
                        case Float16Array -> TypedArrayFactory.Float16Array;
                        case Float32Array -> TypedArrayFactory.Float32Array;
                        case Float64Array -> TypedArrayFactory.Float64Array;
                        case BigInt64Array -> TypedArrayFactory.BigInt64Array;
                        case BigUint64Array -> TypedArrayFactory.BigUint64Array;
                        default -> throw Errors.shouldNotReachHereUnexpectedValue(builtinEnum);
                    };
                    return JSConstructTypedArrayNodeGen.create(context, builtin, typedArray, args().functionOrNewTarget(newTarget).fixedArgs(3).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case DataView:
                if (construct) {
                    return ConstructDataViewNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(3).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case Map:
                if (construct) {
                    return ConstructMapNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Set:
                if (construct) {
                    return ConstructSetNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case WeakMap:
                if (construct) {
                    return ConstructWeakMapNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case WeakSet:
                if (construct) {
                    return ConstructWeakSetNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Iterator:
                if (construct) {
                    return ConstructIteratorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case AsyncIterator:
                if (construct) {
                    return ConstructAsyncIteratorNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case GeneratorFunction:
                return ConstructFunctionNodeGen.create(context, builtin, true, false, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
            case SharedArrayBuffer:
                if (construct) {
                    return ConstructArrayBufferNodeGen.create(context, builtin, true, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case AsyncFunction:
                return ConstructFunctionNodeGen.create(context, builtin, false, true, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));
            case AsyncGeneratorFunction:
                return ConstructFunctionNodeGen.create(context, builtin, true, true, newTarget, args().functionOrNewTarget(newTarget).varArgs().createArgumentNodes(context));

            case Symbol:
                return construct ? ConstructSymbolNodeGen.create(context, builtin, args().createArgumentNodes(context))
                                : CallSymbolNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case Proxy:
                if (construct) {
                    return ConstructJSProxyNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(3).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Promise:
                if (construct) {
                    return PromiseConstructorNodeGen.create(context, builtin, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case PlainTime:
                if (construct) {
                    return ConstructTemporalPlainTimeNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(6).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainDate:
                if (construct) {
                    return ConstructTemporalPlainDateNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainDateTime:
                if (construct) {
                    return ConstructTemporalPlainDateTimeNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(10).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Duration:
                if (construct) {
                    return ConstructTemporalDurationNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(10).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainYearMonth:
                if (construct) {
                    return ConstructTemporalPlainYearMonthNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainMonthDay:
                if (construct) {
                    return ConstructTemporalPlainMonthDayNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Instant:
                if (construct) {
                    return ConstructTemporalInstantNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case ZonedDateTime:
                if (construct) {
                    return ConstructTemporalZonedDateTimeNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case ShadowRealm:
                if (construct) {
                    return ConstructShadowRealmNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case JSAdapter:
                return ConstructJSAdapterNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case JavaImporter:
                return ConstructJavaImporterNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case Global:
                return construct
                                ? ConstructWebAssemblyGlobalNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).varArgs().createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Instance:
                return construct
                                ? ConstructWebAssemblyInstanceNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Memory:
                return construct
                                ? ConstructWebAssemblyMemoryNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Module:
                return construct
                                ? ConstructWebAssemblyModuleNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Table:
                return construct
                                ? ConstructWebAssemblyTableNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(1).varArgs().createArgumentNodes(context))
                                : createCallRequiresNew(context, builtin);
            case Worker:
                if (construct) {
                    return ConstructWorkerNodeGen.create(context, builtin, newTarget, args().functionOrNewTarget(newTarget).fixedArgs(2).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
        }
        return null;

    }

    public static JSBuiltinNode createCallRequiresNew(JSContext context, JSBuiltin builtin) {
        return CallRequiresNewNodeGen.create(context, builtin, args().createArgumentNodes(context));
    }

    public abstract static class ConstructWithNewTargetNode extends JSBuiltinNode {
        protected final boolean isNewTargetCase;
        @Child private PropertyGetNode getPrototypeNode;

        protected ConstructWithNewTargetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin);
            this.isNewTargetCase = isNewTargetCase;
            if (isNewTargetCase) {
                getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, context);
            }
        }

        protected JSRealm getRealmFromNewTarget(Object newTarget) {
            if (isNewTargetCase) {
                return JSRuntime.getFunctionRealm(newTarget, getRealm());
            }
            return getRealm();
        }

        protected abstract JSDynamicObject getIntrinsicDefaultProto(JSRealm realm);

        protected JSDynamicObject getPrototype(JSRealm realm, JSDynamicObject newTarget) {
            if (isNewTargetCase) {
                return getPrototypeFromNewTarget(newTarget);
            }
            return getIntrinsicDefaultProto(realm);
        }

        protected <T extends JSObject> T swapPrototype(T resultObj, JSDynamicObject newTarget) {
            if (isNewTargetCase) {
                return setPrototypeFromNewTarget(resultObj, newTarget);
            }
            return resultObj;
        }

        private JSDynamicObject getPrototypeFromNewTarget(JSDynamicObject newTarget) {
            Object prototype = getPrototypeNode.getValue(newTarget);
            if (!JSRuntime.isObject(prototype)) {
                prototype = getIntrinsicDefaultProto(getRealmFromNewTarget(newTarget));
            }
            return (JSDynamicObject) prototype;
        }

        protected <T extends JSObject> T setPrototypeFromNewTarget(T resultObj, JSDynamicObject newTarget) {
            JSDynamicObject prototype = getPrototypeFromNewTarget(newTarget);
            JSObject.setPrototype(resultObj, prototype);
            return resultObj;
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ConstructArrayNode extends ConstructWithNewTargetNode {

        public ConstructArrayNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @CompilationFinal private ConstructArrayAllocationSite arrayAllocationSite = createAllocationSite();

        protected static boolean isOneNumberArg(Object[] args) {
            return args.length == 1 && (JSRuntime.isNumber(args[0]) || args[0] instanceof Long);
        }

        protected static boolean isOneForeignArg(Object[] args) {
            return args.length == 1 && JSRuntime.isForeignObject(args[0]);
        }

        protected static boolean isOneIntegerArg(Object[] args) {
            return args.length == 1 && args[0] instanceof Integer && (int) args[0] >= 0;
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSObject constructArray0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSArray.createConstantEmptyArray(getContext(), realm, proto, arrayAllocationSite);
        }

        @Specialization(guards = "isOneIntegerArg(args)")
        protected JSObject constructArrayWithIntLength(JSDynamicObject newTarget, Object[] args) {
            int length = (int) args[0];
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            if (JSConfig.TrackArrayAllocationSites && arrayAllocationSite != null && arrayAllocationSite.isTyped()) {
                ScriptArray initialType = arrayAllocationSite.getInitialArrayType();
                // help checker tool see this is always true, guarded by isTyped()
                if (initialType != null) {
                    return JSArray.create(getContext(), realm, proto, initialType, ((AbstractWritableArray) initialType).allocateArray(length), length);
                }
            }
            return JSArray.createConstantEmptyArray(getContext(), realm, proto, arrayAllocationSite, length);
        }

        @Specialization(guards = {"args.length == 1", "toArrayLengthNode.isTypeNumber(len)"}, replaces = "constructArrayWithIntLength", limit = "1")
        protected JSObject constructWithLength(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args,
                        @Cached @SuppressWarnings("unused") ToArrayLengthNode toArrayLengthNode,
                        @Cached("create(getContext())") @Shared ArrayCreateNode arrayCreateNode,
                        @Bind("toArrayLengthNode.executeLong(firstArg(args))") long len) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return arrayCreateNode.execute(len, realm, proto);
        }

        static Object firstArg(Object[] arguments) {
            return arguments[0];
        }

        /*
         * GR-53718: Cannot use @CachedLibrary(firstArg(args)) here because then firstArg is being
         * evaluated before the guard checking the array length.
         */
        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"isOneForeignArg(args)"})
        protected JSObject constructWithForeignArg(JSDynamicObject newTarget, Object[] args,
                        @Bind Node node,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached("create(getContext())") @Shared ArrayCreateNode arrayCreateNode,
                        @Cached @Exclusive InlinedConditionProfile isNumber,
                        @Cached @Exclusive InlinedBranchProfile rangeErrorProfile) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            Object len = args[0];
            if (isNumber.profile(node, interop.isNumber(len))) {
                if (interop.fitsInLong(len)) {
                    try {
                        long length = interop.asLong(len);
                        if (JSRuntime.isArrayIndex(length)) {
                            return arrayCreateNode.execute(length, realm, proto);
                        }
                    } catch (UnsupportedMessageException umex) {
                        rangeErrorProfile.enter(node);
                        throw Errors.createTypeErrorInteropException(len, umex, "asLong", this);
                    }
                }
                rangeErrorProfile.enter(node);
                throw Errors.createRangeErrorInvalidArrayLength(this);
            } else {
                return JSArray.create(getContext(), realm, proto, ConstantObjectArray.createConstantObjectArray(), args, 1);
            }
        }

        @Specialization(guards = {"!isOneNumberArg(args)", "!isOneForeignArg(args)"})
        protected JSObject constructArrayVarargs(JSDynamicObject newTarget, Object[] args,
                        @Cached @Exclusive InlinedBranchProfile isIntegerCase,
                        @Cached @Exclusive InlinedBranchProfile isDoubleCase,
                        @Cached @Exclusive InlinedBranchProfile isObjectCase,
                        @Cached @Exclusive InlinedConditionProfile isLengthZero) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            if (isLengthZero.profile(this, args == null || args.length == 0)) {
                return JSArray.create(getContext(), realm, proto, ScriptArray.createConstantEmptyArray(), args, 0);
            } else {
                ArrayContentType type = ArrayLiteralNode.identifyPrimitiveContentType(args, false);
                if (type == ArrayContentType.Integer) {
                    isIntegerCase.enter(this);
                    return JSArray.createZeroBasedIntArray(getContext(), realm, proto, ArrayLiteralNode.createIntArray(args));
                } else if (type == ArrayContentType.Double) {
                    isDoubleCase.enter(this);
                    return JSArray.createZeroBasedDoubleArray(getContext(), realm, proto, ArrayLiteralNode.createDoubleArray(args));
                } else {
                    isObjectCase.enter(this);
                    return JSArray.create(getContext(), realm, proto, ConstantObjectArray.createConstantObjectArray(), args, args.length);
                }
            }
        }

        @Override
        protected JSDynamicObject getPrototype(JSRealm realm, JSDynamicObject newTarget) {
            var prototype = super.getPrototype(realm, newTarget);
            assert prototype != Null.instance;
            /*
             * The prototype object needs to be marked as derived from Array.prototype. If it's not,
             * the prototype was created by some unvetted means (i.e. not by the constructor of an
             * Array subclass), so we cannot make any assumptions about prototype elements anymore.
             */
            if (isNewTargetCase) {
                if (getContext().getArrayPrototypeNoElementsAssumption().isValid() && !JSShape.isArrayPrototypeOrDerivative(prototype)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getContext().getArrayPrototypeNoElementsAssumption().invalidate("Unexpected Array prototype");
                }
            } else {
                // Must be %Array.prototype%, since the "prototype" property is immutable.
                assert JSArray.isArrayPrototype(prototype);
            }
            return prototype;
        }

        @Override
        public JavaScriptNode copy() {
            ConstructArrayNode copy = (ConstructArrayNode) super.copy();
            copy.arrayAllocationSite = createAllocationSite();
            return copy;
        }

        private static ConstructArrayAllocationSite createAllocationSite() {
            return JSConfig.TrackArrayAllocationSites ? new ConstructArrayAllocationSite() : null;
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getArrayPrototype();
        }

        private static final class ConstructArrayAllocationSite implements ArrayAllocationSite {
            private static final ScriptArray UNINIT_ARRAY_TYPE = ScriptArray.createConstantEmptyArray();
            @CompilationFinal private ScriptArray concreteArrayType = UNINIT_ARRAY_TYPE;
            @CompilationFinal private Assumption assumption = Truffle.getRuntime().createAssumption("Array allocation site (untyped)");

            public boolean isTyped() {
                return assumption.isValid() && concreteArrayType != UNINIT_ARRAY_TYPE && concreteArrayType != null;
            }

            @Override
            public void notifyArrayTransition(ScriptArray arrayType, int length) {
                CompilerAsserts.neverPartOfCompilation("do not notify array transitions from compiled code");
                assert JSConfig.TrackArrayAllocationSites;
                if (arrayType instanceof AbstractWritableArray && length > 0) {
                    if (concreteArrayType == UNINIT_ARRAY_TYPE) {
                        concreteArrayType = arrayType;
                        assumption.invalidate("TypedArray type initialization");
                        assumption = Truffle.getRuntime().createAssumption("Array allocation site (typed)");
                    } else if (concreteArrayType != arrayType) {
                        concreteArrayType = null;
                        assumption.invalidate("TypedArray type rewrite");
                    }
                }
            }

            @Override
            public ScriptArray getInitialArrayType() {
                if (isTyped()) {
                    return concreteArrayType;
                }
                return null;
            }
        }
    }

    public abstract static class CallBooleanNode extends JSBuiltinNode {
        public CallBooleanNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean callBoolean(Object value,
                        @Cached(inline = true) JSToBooleanNode toBoolean) {
            return toBoolean.executeBoolean(this, value);
        }
    }

    public abstract static class ConstructBooleanNode extends ConstructWithNewTargetNode {
        public ConstructBooleanNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSBooleanObject constructBoolean(JSDynamicObject newTarget, Object value,
                        @Cached(inline = true) JSToBooleanNode toBoolean) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSBoolean.create(getContext(), realm, proto, toBoolean.executeBoolean(this, value));
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getBooleanPrototype();
        }
    }

    public abstract static class CallDateNode extends JSBuiltinNode {
        public CallDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object callDate() {
            // called as function ECMAScript 15.9.2.1
            JSRealm realm = getRealm();
            return JSDate.toString(realm.currentTimeMillis(), realm);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ConstructDateNode extends ConstructWithNewTargetNode {

        public ConstructDateNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = {"args.length == 0"})
        protected final JSObject constructDateZero(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSDate.create(getContext(), realm, proto, realm.currentTimeMillis());
        }

        protected static Object arg0(Object[] args) {
            return args[0];
        }

        @Specialization(guards = {"args.length == 1", "isJSDate(arg0(args))"})
        protected final JSObject constructDateFromDate(JSDynamicObject newTarget, Object[] args) {
            double dateValue = JSDate.getTimeMillisField((JSDateObject) args[0]);
            assert JSRuntime.isSameValue(JSDate.timeClip(dateValue), dateValue);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSDate.create(getContext(), realm, proto, dateValue);
        }

        @Specialization(guards = {"args.length == 1", "!isJSDate(arg0(args))"})
        protected final JSObject constructDateOne(JSDynamicObject newTarget, Object[] args,
                        @Cached InlinedConditionProfile isSpecialCase,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached InlinedConditionProfile stringOrNumberProfile,
                        @Cached("createHintDefault()") JSToPrimitiveNode toPrimitiveNode,
                        @Cached @Shared JSToDoubleNode toDoubleNode) {
            JSRealm realm = getRealm();
            Object arg0 = args[0];
            double rawDateValue;
            if (getContext().getEcmaScriptVersion() >= 6 && interop.isInstant(arg0)) {
                rawDateValue = JSDate.getDateValueFromInstant(arg0, interop);
            } else {
                Object value = toPrimitiveNode.execute(arg0);
                if (stringOrNumberProfile.profile(this, value instanceof TruffleString)) {
                    rawDateValue = parseDate(getContext(), realm, (TruffleString) value);
                } else {
                    rawDateValue = toDoubleNode.executeDouble(value);
                }
            }
            double dateValue = timeClip(rawDateValue, isSpecialCase);
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSDate.create(getContext(), realm, proto, dateValue);
        }

        @Specialization(guards = {"args.length >= 2"})
        protected final JSObject constructDateMult(JSDynamicObject newTarget, Object[] args,
                        @Cached @Shared JSToDoubleNode toDoubleNode) {
            double dateValue = constructorImpl(args, toDoubleNode);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSDate.create(getContext(), realm, proto, dateValue);
        }

        // inlined JSDate.timeClip to use profiles
        private double timeClip(double dateValue, InlinedConditionProfile isSpecialCase) {
            if (isSpecialCase.profile(this, Double.isInfinite(dateValue) || Double.isNaN(dateValue) || Math.abs(dateValue) > JSDate.MAX_DATE)) {
                return Double.NaN;
            }
            return (long) dateValue;
        }

        @TruffleBoundary
        public static double parseDate(JSContext context, JSRealm realm, TruffleString dateStr) {
            Integer[] fields = context.getEvaluator().parseDate(realm, Strings.toJavaString(Strings.lazyTrim(dateStr)), false);
            if (fields != null) {
                return JSDate.makeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7]);
            }
            return Double.NaN;
        }

        private static double constructorImpl(Object[] args, JSToDoubleNode toDoubleNode) {
            double[] argsEvaluated = new double[args.length];
            boolean isNaN = false;
            for (int i = 0; i < args.length; i++) {
                double d = toDoubleNode.executeDouble(args[i]);
                if (Double.isNaN(d)) {
                    isNaN = true;
                }
                argsEvaluated[i] = d;
            }
            if (isNaN) {
                return Double.NaN;
            }
            return JSDate.executeConstructor(argsEvaluated, false);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDatePrototype();
        }

    }

    public abstract static class ConstructTemporalPlainDateNode extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainDateNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainDate(JSDynamicObject newTarget, Object isoYear, Object isoMonth,
                        Object isoDay, Object calendarLike,
                        @Cached JSToIntegerThrowOnInfinityNode toIntegerNode,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                        @Cached InlinedBranchProfile errorBranch) {
            final int y = toIntegerNode.executeIntOrThrow(isoYear);
            final int m = toIntegerNode.executeIntOrThrow(isoMonth);
            final int d = toIntegerNode.executeIntOrThrow(isoDay);
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalPlainDate.create(getContext(), realm, proto, y, m, d, calendar, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainDatePrototype();
        }
    }

    public abstract static class ConstructTemporalPlainTimeNode extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainTimeNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainTime(JSDynamicObject newTarget, Object hourObj, Object minuteObj,
                        Object secondObj, Object millisecondObject,
                        Object microsecondObject, Object nanosecondObject,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached JSToIntegerThrowOnInfinityNode toIntegerNode) {
            final int hour = toIntegerNode.executeIntOrThrow(hourObj);
            final int minute = toIntegerNode.executeIntOrThrow(minuteObj);
            final int second = toIntegerNode.executeIntOrThrow(secondObj);
            final int millisecond = toIntegerNode.executeIntOrThrow(millisecondObject);
            final int microsecond = toIntegerNode.executeIntOrThrow(microsecondObject);
            final int nanosecond = toIntegerNode.executeIntOrThrow(nanosecondObject);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalPlainTime.create(getContext(), realm, proto,
                            hour, minute, second, millisecond, microsecond, nanosecond, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainTimePrototype();
        }
    }

    public abstract static class ConstructTemporalPlainDateTimeNode extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainDateTimeNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainDateTime(JSDynamicObject newTarget, Object yearObj, Object monthObj, Object dayObj, Object hourObj, Object minuteObj,
                        Object secondObj, Object millisecondObject,
                        Object microsecondObject, Object nanosecondObject, Object calendarLike,
                        @Cached JSToIntegerThrowOnInfinityNode toIntegerNode,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                        @Cached InlinedBranchProfile errorBranch) {
            final int year = toIntegerNode.executeIntOrThrow(yearObj);
            final int month = toIntegerNode.executeIntOrThrow(monthObj);
            final int day = toIntegerNode.executeIntOrThrow(dayObj);

            final int hour = toIntegerNode.executeIntOrThrow(hourObj);
            final int minute = toIntegerNode.executeIntOrThrow(minuteObj);
            final int second = toIntegerNode.executeIntOrThrow(secondObj);
            final int millisecond = toIntegerNode.executeIntOrThrow(millisecondObject);
            final int microsecond = toIntegerNode.executeIntOrThrow(microsecondObject);
            final int nanosecond = toIntegerNode.executeIntOrThrow(nanosecondObject);
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalPlainDateTime.create(getContext(), realm, proto,
                            year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainDateTimePrototype();
        }
    }

    public abstract static class ConstructTemporalDurationNode extends ConstructWithNewTargetNode {

        protected ConstructTemporalDurationNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalDuration(JSDynamicObject newTarget, Object yearsObj, Object monthsObj,
                        Object weeksObj, Object daysObj, Object hoursObj, Object minutesObj, Object secondsObj,
                        Object millisecondsObject, Object microsecondsObject, Object nanosecondsObject,
                        @Cached JSToIntegerWithoutRoundingNode toIntegerNode,
                        @Cached InlinedBranchProfile errorBranch) {
            final double years = toIntegerNode.executeDouble(yearsObj);
            final double months = toIntegerNode.executeDouble(monthsObj);
            final double weeks = toIntegerNode.executeDouble(weeksObj);
            final double days = toIntegerNode.executeDouble(daysObj);
            final double hours = toIntegerNode.executeDouble(hoursObj);
            final double minutes = toIntegerNode.executeDouble(minutesObj);
            final double seconds = toIntegerNode.executeDouble(secondsObj);
            final double milliseconds = toIntegerNode.executeDouble(millisecondsObject);
            final double microseconds = toIntegerNode.executeDouble(microsecondsObject);
            final double nanoseconds = toIntegerNode.executeDouble(nanosecondsObject);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalDuration.createTemporalDuration(getContext(), realm, proto,
                            years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalDurationPrototype();
        }
    }

    public abstract static class ConstructTemporalPlainYearMonth extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainYearMonth(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainYearMonth(JSDynamicObject newTarget, Object isoYear,
                        Object isoMonth, Object calendarLike, Object refISODay,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached JSToIntegerThrowOnInfinityNode toInteger,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue) {

            Object referenceISODay = refISODay;
            if (referenceISODay == Undefined.instance || referenceISODay == null) {
                referenceISODay = 1;
            }
            int y = toInteger.executeIntOrThrow(isoYear);
            int m = toInteger.executeIntOrThrow(isoMonth);
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);
            int ref = toInteger.executeIntOrThrow(referenceISODay);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalPlainYearMonth.create(getContext(), realm, proto, y, m, calendar, ref, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainYearMonthPrototype();
        }
    }

    public abstract static class ConstructTemporalPlainMonthDay extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainMonthDay(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainMonthDay(JSDynamicObject newTarget, Object isoMonth,
                        Object isoDay, Object calendarLike, Object refISOYear,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached JSToIntegerThrowOnInfinityNode toInt,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue) {
            Object referenceISOYear = refISOYear;
            if (referenceISOYear == Undefined.instance || referenceISOYear == null) {
                referenceISOYear = 1972;
            }
            int m = toInt.executeIntOrThrow(isoMonth);
            int d = toInt.executeIntOrThrow(isoDay);
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);
            int ref = toInt.executeIntOrThrow(referenceISOYear); // non-spec
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalPlainMonthDay.create(getContext(), realm, proto, m, d, calendar, ref, this, errorBranch);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainMonthDayPrototype();
        }
    }

    public abstract static class ConstructTemporalInstant extends ConstructWithNewTargetNode {

        protected ConstructTemporalInstant(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalInstant(JSDynamicObject newTarget, Object epochNanoseconds,
                        @Cached InlinedBranchProfile errorBranch) {
            BigInt bi = JSRuntime.toBigInt(epochNanoseconds);
            if (!TemporalUtil.isValidEpochNanoseconds(bi)) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalInstant.create(getContext(), realm, proto, bi);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalInstantPrototype();
        }
    }

    public abstract static class ConstructTemporalZonedDateTime extends ConstructWithNewTargetNode {

        protected ConstructTemporalZonedDateTime(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalZonedDateTime(JSDynamicObject newTarget, Object epochNanoseconds, Object timeZoneLike, Object calendarLike,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                        @Cached JSToBigIntNode toBigIntNode,
                        @Cached InlinedBranchProfile errorBranch) {
            BigInt ns = toBigIntNode.executeBigInteger(epochNanoseconds);
            if (!TemporalUtil.isValidEpochNanoseconds(ns)) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            TruffleString timeZone = toTimeZoneIdentifier.execute(timeZoneLike);
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);

            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSTemporalZonedDateTime.create(getContext(), realm, proto, ns, timeZone, calendar);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalZonedDateTimePrototype();
        }
    }

    public abstract static class ConstructRegExpNode extends ConstructWithNewTargetNode {
        private final boolean isCall;

        public ConstructRegExpNode(JSContext context, JSBuiltin builtin, boolean isCall, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.isCall = isCall;
        }

        @Child private JSToStringNode patternToStringNode;
        @Child private JSToStringNode flagsToStringNode;
        @Child private CompileRegexNode compileRegexNode;
        @Child private CreateRegExpNode createRegExpNode;
        @Child private PropertyGetNode getConstructorNode;
        @Child private PropertyGetNode getSourceNode;
        @Child private PropertyGetNode getFlagsNode;

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSObject constructRegExp(JSDynamicObject newTarget, Object pattern, Object flags,
                        @Bind Node node,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode,
                        @Cached InlinedBranchProfile regexpObject,
                        @Cached InlinedBranchProfile regexpMatcherObject,
                        @Cached InlinedBranchProfile regexpNonObject,
                        @Cached InlinedBranchProfile regexpObjectNewFlagsBranch,
                        @Cached InlinedConditionProfile callIsRegExpProfile,
                        @Cached InlinedConditionProfile constructorEquivalentProfile,
                        @Cached(inline = true) TRegexUtil.InteropReadStringMemberNode readPattern) {
            JSRealm realm = getRealm();
            boolean hasMatchSymbol = isRegExpNode.executeBoolean(pattern);
            boolean legacyFeaturesEnabled;
            if (isCall) {
                // we are in the "call" case, i.e. NewTarget is undefined (before)
                if (callIsRegExpProfile.profile(node, hasMatchSymbol && flags == Undefined.instance && pattern instanceof JSObject)) {
                    JSObject patternObj = (JSObject) pattern;
                    Object patternConstructor = getConstructor(patternObj);
                    if (constructorEquivalentProfile.profile(node, patternConstructor == realm.getRegExpConstructor())) {
                        return patternObj;
                    }
                }
                legacyFeaturesEnabled = true;
            } else {
                // we are in the "construct" case, i.e. NewTarget is NOT undefined
                legacyFeaturesEnabled = newTarget == realm.getRegExpConstructor();
            }

            Object p;
            Object f;
            Object compiledRegex;
            JSDynamicObject proto;
            TruffleString patternStr;
            if (pattern instanceof JSRegExpObject regExpPattern) {
                regexpObject.enter(node);
                compiledRegex = JSRegExp.getCompiledRegex(regExpPattern);
                proto = getPrototype(realm, newTarget);
                patternStr = readPattern.execute(node, compiledRegex, TRegexUtil.Props.CompiledRegex.PATTERN);
                if (flags != Undefined.instance) {
                    regexpObjectNewFlagsBranch.enter(node);
                    if (getContext().getEcmaScriptVersion() < 6) {
                        throw Errors.createTypeError("Cannot supply flags when constructing one RegExp from another");
                    }
                    Object flagsStr = flagsToString(flags);
                    compiledRegex = getCompileRegexNode().compile(patternStr, flagsStr);
                }
            } else {
                if (hasMatchSymbol) {
                    regexpMatcherObject.enter(node);
                    JSObject patternJSObj = (JSObject) pattern;
                    p = getSource(patternJSObj);
                    if (flags == Undefined.instance) {
                        f = getFlags(patternJSObj);
                    } else {
                        f = flags;
                    }
                } else {
                    regexpNonObject.enter(node);
                    p = pattern;
                    f = flags;
                }
                proto = getPrototype(realm, newTarget);
                patternStr = getPatternToStringNode().executeString(p);
                Object flagsStr = flagsToString(f);
                compiledRegex = getCompileRegexNode().compile(patternStr, flagsStr);
            }

            JSRegExpObject regExp = getCreateRegExpNode().createRegExp(compiledRegex, legacyFeaturesEnabled, realm, proto);
            if (getContext().getLanguageOptions().testV8Mode()) {
                // workaround for the reference equality check at the end of mjsunit/regexp.js
                // TODO: remove this as soon as option maps are available for TRegex Sources
                JSObjectUtil.putDataProperty(regExp, Strings.SOURCE, JSRegExp.escapeRegExpPattern(patternStr), JSAttributes.configurableNotEnumerableNotWritable());
            }
            return regExp;
        }

        private JSToStringNode getPatternToStringNode() {
            if (patternToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                patternToStringNode = insert(JSToStringNode.createUndefinedToEmpty());
            }
            return patternToStringNode;
        }

        private CompileRegexNode getCompileRegexNode() {
            if (compileRegexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                compileRegexNode = insert(CompileRegexNode.create(getContext()));
            }
            return compileRegexNode;
        }

        private CreateRegExpNode getCreateRegExpNode() {
            if (createRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createRegExpNode = insert(CreateRegExpNode.create(getContext()));
            }
            return createRegExpNode;
        }

        private Object flagsToString(Object f) {
            if (flagsToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                flagsToStringNode = insert(JSToStringNode.createUndefinedToEmpty());
            }
            return flagsToStringNode.executeString(f);
        }

        private Object getConstructor(JSDynamicObject obj) {
            if (getConstructorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getConstructorNode = insert(PropertyGetNode.create(JSObject.CONSTRUCTOR, getContext()));
            }
            return getConstructorNode.getValue(obj);
        }

        private Object getSource(JSDynamicObject obj) {
            if (getSourceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSourceNode = insert(PropertyGetNode.create(JSRegExp.SOURCE, getContext()));
            }
            return getSourceNode.getValue(obj);
        }

        private Object getFlags(JSDynamicObject obj) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, getContext()));
            }
            return getFlagsNode.getValue(obj);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getRegExpPrototype();
        }

    }

    public abstract static class CallStringNode extends JSBuiltinNode {
        public CallStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"args.length == 0"})
        protected Object callStringInt0(@SuppressWarnings("unused") Object[] args) {
            return Strings.EMPTY_STRING;
        }

        @Specialization(guards = {"args.length != 0"})
        protected Object callStringGeneric(Object[] args,
                        @Cached("createSymbolToString()") JSToStringNode toStringNode) {
            return toStringNode.executeString(args[0]);
        }
    }

    public abstract static class ConstructStringNode extends ConstructWithNewTargetNode {

        public ConstructStringNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSStringObject constructStringInt0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSString.create(getContext(), realm, proto, Strings.EMPTY_STRING);
        }

        @Specialization(guards = {"args.length != 0"})
        protected JSStringObject constructString(JSDynamicObject newTarget, Object[] args,
                        @Cached JSToStringNode toStringNode) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSString.create(getContext(), realm, proto, toStringNode.executeString(args[0]));
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getStringPrototype();
        }

    }

    public abstract static class ConstructWeakRefNode extends ConstructWithNewTargetNode {
        public ConstructWeakRefNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"canBeHeldWeakly.execute(this, target)"})
        protected JSObject constructWeakRef(JSDynamicObject newTarget, Object target,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWeakRef.create(getContext(), realm, proto, target);
        }

        @Specialization(guards = {"!canBeHeldWeakly.execute(this, target)"})
        protected static JSObject constructWeakRefNonObject(@SuppressWarnings("unused") JSDynamicObject newTarget, @SuppressWarnings("unused") Object target,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            throw Errors.createTypeError("WeakRef: invalid target");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakRefPrototype();
        }
    }

    public abstract static class ConstructFinalizationRegistryNode extends ConstructWithNewTargetNode {

        @Child protected IsCallableNode isCallableNode = IsCallableNode.create();

        public ConstructFinalizationRegistryNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"isCallableNode.executeBoolean(cleanupCallback)"})
        protected JSObject constructFinalizationRegistry(JSDynamicObject newTarget, Object cleanupCallback) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSFinalizationRegistry.create(getContext(), realm, proto, realm.getAgent().hostMakeJobCallback(cleanupCallback));
        }

        @Specialization(guards = {"!isCallableNode.executeBoolean(cleanupCallback)"})
        protected JSObject constructFinalizationRegistryNonObject(@SuppressWarnings("unused") JSDynamicObject newTarget, @SuppressWarnings("unused") Object cleanupCallback) {
            throw Errors.createTypeError("FinalizationRegistry: cleanup must be callable");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getFinalizationRegistryPrototype();
        }
    }

    public abstract static class ConstructCollatorNode extends ConstructWithNewTargetNode {

        @Child InitializeCollatorNode initializeCollatorNode;

        public ConstructCollatorNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeCollatorNode = InitializeCollatorNode.createInitalizeCollatorNode(context);
        }

        @Specialization
        protected JSCollatorObject constructCollator(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSCollatorObject collator = JSCollator.create(getContext(), realm, proto);
            return initializeCollatorNode.executeInit(collator, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getCollatorPrototype();
        }

    }

    public abstract static class ConstructListFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeListFormatNode initializeListFormatNode;

        public ConstructListFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeListFormatNode = InitializeListFormatNode.createInitalizeListFormatNode(context);
        }

        @Specialization
        protected JSListFormatObject constructListFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSListFormatObject listFormat = JSListFormat.create(getContext(), realm, proto);
            return initializeListFormatNode.executeInit(listFormat, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getListFormatPrototype();
        }
    }

    public abstract static class ConstructRelativeTimeFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeRelativeTimeFormatNode initializeRelativeTimeFormatNode;

        public ConstructRelativeTimeFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeRelativeTimeFormatNode = InitializeRelativeTimeFormatNode.createInitalizeRelativeTimeFormatNode(context);
        }

        @Specialization
        protected JSRelativeTimeFormatObject constructRelativeTimeFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSRelativeTimeFormatObject listFormat = JSRelativeTimeFormat.create(getContext(), realm, proto);
            return initializeRelativeTimeFormatNode.executeInit(listFormat, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getRelativeTimeFormatPrototype();
        }
    }

    public abstract static class ConstructSegmenterNode extends ConstructWithNewTargetNode {

        @Child InitializeSegmenterNode initializeSegmenterNode;

        public ConstructSegmenterNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeSegmenterNode = InitializeSegmenterNode.createInitalizeSegmenterNode(context);
        }

        @Specialization
        protected JSSegmenterObject constructSegmenter(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSSegmenterObject segmenter = JSSegmenter.create(getContext(), realm, proto);
            return initializeSegmenterNode.executeInit(segmenter, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSegmenterPrototype();
        }
    }

    public abstract static class ConstructDisplayNamesNode extends ConstructWithNewTargetNode {

        @Child InitializeDisplayNamesNode initializeDisplayNamesNode;

        public ConstructDisplayNamesNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeDisplayNamesNode = InitializeDisplayNamesNode.createInitalizeDisplayNamesNode(context);
        }

        @Specialization
        protected JSDisplayNamesObject constructDisplayNames(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSDisplayNamesObject displayNames = JSDisplayNames.create(getContext(), realm, proto);
            return initializeDisplayNamesNode.executeInit(displayNames, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDisplayNamesPrototype();
        }
    }

    public abstract static class ConstructLocaleNode extends ConstructWithNewTargetNode {
        @Child InitializeLocaleNode initializeLocaleNode;

        public ConstructLocaleNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeLocaleNode = InitializeLocaleNode.createInitalizeLocaleNode(context);
        }

        @Specialization
        protected JSLocaleObject constructLocale(JSDynamicObject newTarget, Object tag, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSLocaleObject locale = JSLocale.create(getContext(), realm, proto);
            return initializeLocaleNode.executeInit(locale, tag, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getLocalePrototype();
        }
    }

    public abstract static class ConstructNumberFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeNumberFormatNode initializeNumberFormatNode;

        public ConstructNumberFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @Specialization
        protected JSNumberFormatObject constructNumberFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSNumberFormatObject numberFormat = JSNumberFormat.create(getContext(), realm, proto);
            return initializeNumberFormatNode.executeInit(numberFormat, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getNumberFormatPrototype();
        }

    }

    public abstract static class ConstructPluralRulesNode extends ConstructWithNewTargetNode {

        @Child InitializePluralRulesNode initializePluralRulesNode;

        public ConstructPluralRulesNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializePluralRulesNode = InitializePluralRulesNode.createInitalizePluralRulesNode(context);
        }

        @Specialization
        protected JSPluralRulesObject constructPluralRules(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSPluralRulesObject pluralRules = JSPluralRules.create(getContext(), realm, proto);
            return initializePluralRulesNode.executeInit(pluralRules, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getPluralRulesPrototype();
        }
    }

    public abstract static class ConstructDateTimeFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeDateTimeFormatNode initializeDateTimeFormatNode;

        public ConstructDateTimeFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, InitializeDateTimeFormatNode.Required.ANY,
                            InitializeDateTimeFormatNode.Defaults.DATE);
        }

        @Specialization
        protected JSDateTimeFormatObject constructDateTimeFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSDateTimeFormatObject dateTimeFormat = JSDateTimeFormat.create(getContext(), realm, proto);
            return initializeDateTimeFormatNode.executeInit(dateTimeFormat, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDateTimeFormatPrototype();
        }

    }

    @ImportStatic({JSConfig.class})
    public abstract static class ConstructObjectNode extends ConstructWithNewTargetNode {
        public ConstructObjectNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        protected static boolean arg0NullOrUndefined(Object[] args) {
            Object arg0 = args[0];
            return (arg0 == Undefined.instance) || (arg0 == Null.instance);
        }

        protected static Object firstArgument(Object[] arguments) {
            return (arguments.length == 0) ? Undefined.instance : arguments[0];
        }

        @Specialization(guards = {"isNewTargetCase"})
        protected JSObject constructObjectNewTarget(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"arguments.length == 0"})
        protected JSObject constructObject0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"!isNewTargetCase", "arguments.length > 0", "!arg0NullOrUndefined(arguments)"}, limit = "InteropLibraryLimit")
        protected Object constructObjectJSObject(@SuppressWarnings("unused") JSDynamicObject newTarget, Object[] arguments,
                        @Bind Node node,
                        @Cached JSToObjectNode toObjectNode,
                        @CachedLibrary("firstArgument(arguments)") InteropLibrary interop,
                        @Cached InlinedConditionProfile isNull) {
            Object arg0 = arguments[0];
            if (isNull.profile(node, interop.isNull(arg0))) {
                return newObject(Null.instance);
            } else {
                return toObjectNode.execute(arg0);
            }
        }

        @Specialization(guards = {"arguments.length > 0", "arg0NullOrUndefined(arguments)"})
        protected JSObject constructObjectNullOrUndefined(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        private JSObject newObject(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSOrdinary.create(getContext(), realm, proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getObjectPrototype();
        }

    }

    public abstract static class CallNumberNode extends JSBuiltinNode {
        public CallNumberNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"args.length == 0"})
        protected int callNumberZero(@SuppressWarnings("unused") Object[] args) {
            return 0;
        }

        @Specialization(guards = {"args.length > 0"})
        protected Number callNumber(Object[] args,
                        @Cached JSToNumericNode toNumericNode,
                        @Cached JSNumericToNumberNode toNumberFromNumericNode) {
            return toNumberFromNumericNode.executeNumeric(toNumericNode.execute(args[0]));
        }
    }

    public abstract static class ConstructNumberNode extends ConstructWithNewTargetNode {
        public ConstructNumberNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSNumberObject constructNumberZero(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSNumber.create(getContext(), realm, proto, 0);
        }

        @Specialization(guards = {"args.length > 0"})
        protected JSNumberObject constructNumber(JSDynamicObject newTarget, Object[] args,
                        @Cached JSToNumericNode toNumericNode,
                        @Cached JSNumericToNumberNode toNumberFromNumericNode) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            Number number = toNumberFromNumericNode.executeNumeric(toNumericNode.execute(args[0]));
            return JSNumber.create(getContext(), realm, proto, number);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getNumberPrototype();
        }

    }

    public abstract static class CallBigIntNode extends JSBuiltinNode {

        public CallBigIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static BigInt doBigInt(BigInt value) {
            return value;
        }

        @Specialization
        protected final BigInt toBigInt(Object value,
                        @Cached JSToBigIntNode.CoercePrimitiveToBigIntNode toBigIntNode,
                        @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
            Object primitive = toPrimitiveNode.execute(value);
            return toBigIntNode.executeBigInt(this, primitive);
        }
    }

    /**
     * @see CallBigIntNode
     */
    public abstract static class ConstructBigIntNode extends JSBuiltinNode {

        public ConstructBigIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static final JSDynamicObject construct() {
            throw Errors.createTypeError("BigInt is not a constructor");
        }
    }

    public abstract static class ConstructFunctionNode extends ConstructWithNewTargetNode {
        private final boolean generatorFunction;
        private final boolean asyncFunction;
        @Child private JSToStringNode toStringNode;
        @Child private CreateDynamicFunctionNode functionNode;

        public ConstructFunctionNode(JSContext context, JSBuiltin builtin, boolean generatorFunction, boolean asyncFunction, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.generatorFunction = generatorFunction;
            this.asyncFunction = asyncFunction;
            this.toStringNode = JSToStringNode.create();
            this.functionNode = CreateDynamicFunctionNodeGen.create(context, generatorFunction, asyncFunction);
        }

        @Specialization
        protected final JSFunctionObject constructFunction(JSDynamicObject newTarget, Object[] args,
                        @Cached InlinedConditionProfile hasArgsProfile,
                        @Cached InlinedConditionProfile hasParamsProfile) {
            int argc = args.length;
            TruffleString[] params;
            TruffleString body;
            if (hasArgsProfile.profile(this, argc > 0)) {
                params = new TruffleString[argc - 1];
                for (int i = 0; i < argc - 1; i++) {
                    params[i] = toStringNode.executeString(args[i]);
                }
                body = toStringNode.executeString(args[argc - 1]);
            } else {
                params = new TruffleString[0];
                body = Strings.EMPTY_STRING;
            }
            TruffleString paramList = hasParamsProfile.profile(this, argc > 1) ? join(params) : Strings.EMPTY_STRING;
            assert isCallerSensitive();
            Node callNode = EvalNode.findCallNode(getRealm());
            String sourceName = EvalNode.formatEvalOrigin(callNode, getContext(), Evaluator.FUNCTION_SOURCE_NAME);
            ScriptOrModule activeScriptOrModule = EvalNode.findActiveScriptOrModule(callNode);
            JSFunctionObject functionObj = functionNode.executeFunction(Strings.toJavaString(paramList), Strings.toJavaString(body), sourceName, activeScriptOrModule);
            return swapPrototype(functionObj, newTarget);
        }

        @TruffleBoundary
        private static TruffleString join(TruffleString[] params) {
            assert params.length > 0;
            var sb = Strings.builderCreate();
            Strings.builderAppend(sb, params[0]);
            for (int i = 1; i < params.length; i++) {
                Strings.builderAppend(sb, Strings.COMMA);
                Strings.builderAppend(sb, params[i]);
            }
            return Strings.builderToString(sb);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            if (generatorFunction && asyncFunction) {
                return realm.getAsyncGeneratorFunctionPrototype();
            } else if (generatorFunction) {
                return realm.getGeneratorFunctionPrototype();
            } else if (asyncFunction) {
                return realm.getAsyncFunctionPrototype();
            } else {
                return realm.getFunctionPrototype();
            }
        }

        @Override
        public boolean isCallerSensitive() {
            return true;
        }
    }

    /**
     * Create (and potentially cache) dynamic function from parameter list and body strings.
     */
    abstract static class CreateDynamicFunctionNode extends JavaScriptBaseNode {
        private final boolean generatorFunction;
        private final boolean asyncFunction;
        private final JSContext context;

        protected CreateDynamicFunctionNode(JSContext context, boolean generatorFunction, boolean asyncFunction) {
            this.generatorFunction = generatorFunction;
            this.asyncFunction = asyncFunction;
            this.context = context;
        }

        protected abstract JSFunctionObject executeFunction(String paramList, String body, String sourceName, ScriptOrModule activeScriptOrModule);

        protected static boolean equals(String a, String b) {
            return a.equals(b);
        }

        @NeverDefault
        protected LRUCache<CachedSourceKey, ScriptNode> createCache() {
            return new LRUCache<>(context.getLanguageOptions().functionConstructorCacheSize());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(cachedParamList, paramList)", "equals(cachedBody, body)", "equals(cachedSourceName, sourceName)"}, limit = "1")
        protected final JSFunctionObject doCached(String paramList, String body, String sourceName, ScriptOrModule activeScriptOrModule,
                        @Cached(value = "paramList") String cachedParamList,
                        @Cached(value = "body") String cachedBody,
                        @Cached(value = "sourceName") String cachedSourceName,
                        @Cached(value = "createAssumedValue()") AssumedValue<ScriptNode> cachedParsedFunction) {
            ScriptNode parsedFunction = cachedParsedFunction.get();
            if (parsedFunction == null) {
                parsedFunction = parseFunction(paramList, body, sourceName, activeScriptOrModule);
                cachedParsedFunction.set(parsedFunction);
            }

            return evalParsedFunction(getRealm(), parsedFunction);
        }

        @Specialization(replaces = "doCached")
        protected final JSFunctionObject doUncached(String paramList, String body, String sourceName, ScriptOrModule activeScriptOrModule,
                        @Cached("createCache()") LRUCache<CachedSourceKey, ScriptNode> cache) {
            ScriptNode cached = cacheLookup(cache, new CachedSourceKey(paramList, body, sourceName, activeScriptOrModule));
            JSRealm realm = getRealm();
            if (cached == null) {
                return parseAndEvalFunction(cache, realm, paramList, body, sourceName, activeScriptOrModule);
            } else {
                return evalParsedFunction(realm, cached);
            }
        }

        @TruffleBoundary
        protected ScriptNode cacheLookup(LRUCache<CachedSourceKey, ScriptNode> cache, CachedSourceKey sourceKey) {
            synchronized (cache) {
                return cache.get(sourceKey);
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final ScriptNode parseFunction(String paramList, String body, String sourceName, ScriptOrModule activeScriptOrModule) {
            CompilerAsserts.neverPartOfCompilation();
            return context.getEvaluator().parseFunction(context, paramList, body, generatorFunction, asyncFunction, sourceName, activeScriptOrModule);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static JSFunctionObject evalParsedFunction(JSRealm realm, ScriptNode parsedFunction) {
            return (JSFunctionObject) parsedFunction.run(realm);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private JSFunctionObject parseAndEvalFunction(LRUCache<CachedSourceKey, ScriptNode> cache, JSRealm realm, String paramList, String body, String sourceName,
                        ScriptOrModule activeScriptOrModule) {
            ScriptNode parsedBody = parseFunction(paramList, body, sourceName, activeScriptOrModule);
            synchronized (cache) {
                cache.put(new CachedSourceKey(paramList, body, sourceName, activeScriptOrModule), parsedBody);
            }
            return evalParsedFunction(realm, parsedBody);
        }

        AssumedValue<ScriptNode> createAssumedValue() {
            return new AssumedValue<>("parsedFunction", null);
        }

        protected record CachedSourceKey(String paramList, String body, String sourceName, ScriptOrModule activeScriptOrModule) {
        }
    }

    public abstract static class AbstractClassConstructorNode extends JSBuiltinNode {
        public AbstractClassConstructorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization
        protected final Object construct() {
            throw Errors.createTypeErrorFormat("Abstract class %s not directly constructable", getBuiltin().getName());
        }
    }

    abstract static class GetArrayBufferMaxByteLengthOption extends JavaScriptBaseNode {
        protected static final TruffleString MAX_BYTE_LENGTH = Strings.constant("maxByteLength");

        protected abstract long execute(Object options);

        @Specialization(guards = {"isUndefined(options)"})
        protected long doUndefined(@SuppressWarnings("unused") Object options) {
            return JSArrayBuffer.FIXED_LENGTH;
        }

        @Specialization(replaces = "doUndefined")
        protected long doGeneric(Object options,
                        @Cached IsObjectNode isObjectNode,
                        @Cached("create(MAX_BYTE_LENGTH, getJSContext())") PropertyGetNode getMaxByteLengthNode,
                        @Cached JSToIndexNode toIndexNode) {
            if (!isObjectNode.executeBoolean(options)) {
                return JSArrayBuffer.FIXED_LENGTH;
            }
            Object maxByteLength = getMaxByteLengthNode.getValue(options);
            if (maxByteLength == Undefined.instance) {
                return JSArrayBuffer.FIXED_LENGTH;
            }
            return toIndexNode.executeLong(maxByteLength);
        }

    }

    @ImportStatic({JSConfig.class})
    public abstract static class ConstructArrayBufferNode extends ConstructWithNewTargetNode {
        private final boolean useShared;

        public ConstructArrayBufferNode(JSContext context, JSBuiltin builtin, boolean useShared, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.useShared = useShared;
        }

        @Specialization(guards = {"!bufferInterop.hasBufferElements(length)"})
        protected JSDynamicObject constructFromLength(JSDynamicObject newTarget, Object length, Object options,
                        @Cached JSToIndexNode toIndexNode,
                        @Cached GetArrayBufferMaxByteLengthOption getMaxByteLengthOption,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared @SuppressWarnings("unused") InteropLibrary bufferInterop) {
            long byteLength = toIndexNode.executeLong(length);
            long maxByteLength = getMaxByteLengthOption.execute(options);

            boolean allocatingResizableBuffer = (maxByteLength != JSArrayBuffer.FIXED_LENGTH);
            if (allocatingResizableBuffer && (byteLength > maxByteLength)) {
                errorBranch.enter(this);
                throw Errors.createRangeError("byteLength exceeds maxByteLength");
            }

            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);

            if (Math.max(maxByteLength, byteLength) > getContext().getLanguageOptions().maxTypedArrayLength()) {
                errorBranch.enter(this);
                throw Errors.createRangeError("Array buffer allocation failed");
            }
            int byteLengthInt = (int) byteLength;
            int maxByteLengthInt = (int) maxByteLength;

            JSDynamicObject arrayBuffer;
            JSContext contextFromNewTarget = getContext();
            if (useShared) {
                arrayBuffer = JSSharedArrayBuffer.createSharedArrayBuffer(contextFromNewTarget, realm, proto, byteLengthInt, maxByteLengthInt);
            } else {
                if (getContext().isOptionDirectByteBuffer()) {
                    arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(contextFromNewTarget, realm, proto, byteLengthInt, maxByteLengthInt);
                } else {
                    arrayBuffer = JSArrayBuffer.createArrayBuffer(contextFromNewTarget, realm, proto, byteLengthInt, maxByteLengthInt);
                }
            }
            return arrayBuffer;
        }

        @Specialization(guards = {"bufferInterop.hasBufferElements(buffer)"})
        protected JSDynamicObject constructFromInteropBuffer(JSDynamicObject newTarget, Object buffer, @SuppressWarnings("unused") Object options,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared @SuppressWarnings("unused") InteropLibrary bufferInterop) {
            getBufferSizeSafe(buffer, bufferInterop, this, errorBranch);
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSArrayBuffer.createInteropArrayBuffer(getContext(), realm, proto, buffer);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return useShared ? realm.getSharedArrayBufferPrototype() : realm.getArrayBufferPrototype();
        }

        static int getBufferSizeSafe(Object buffer, InteropLibrary bufferInterop, Node node, InlinedBranchProfile errorBranch) {
            try {
                long bufferSize = bufferInterop.getBufferSize(buffer);
                if (bufferSize < 0 || bufferSize > Integer.MAX_VALUE) {
                    errorBranch.enter(node);
                    throw Errors.createRangeErrorInvalidBufferSize();
                }
                return (int) bufferSize;
            } catch (UnsupportedMessageException e) {
                return 0;
            }
        }
    }

    public abstract static class ConstructErrorNode extends ConstructWithNewTargetNode {
        private final JSErrorType errorType;
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;

        public ConstructErrorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.errorType = JSErrorType.valueOf(Strings.toJavaString(builtin.getName()));
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create();
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
            assert errorType != JSErrorType.AggregateError;
        }

        @Specialization
        protected JSDynamicObject constructError(JSDynamicObject newTarget, TruffleString message, Object options) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return constructErrorImpl(newTarget, message, options, realm, proto);
        }

        @Specialization(guards = "!isString(message)")
        protected JSDynamicObject constructError(JSDynamicObject newTarget, Object message, Object options,
                        @Cached JSToStringNode toStringNode) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return constructErrorImpl(newTarget, message == Undefined.instance ? null : toStringNode.executeString(message), options, realm, proto);
        }

        private JSDynamicObject constructErrorImpl(JSDynamicObject newTarget, TruffleString messageOpt, Object options, JSRealm realm, JSDynamicObject proto) {
            JSErrorObject errorObj = JSError.createErrorObject(getContext(), realm, errorType, proto);

            int stackTraceLimit = stackTraceLimitNode.executeInt();
            JSFunctionObject errorFunction = realm.getErrorConstructor(errorType);

            // We skip until newTarget (if any) so as to also skip user-defined Error constructors.
            JSDynamicObject skipUntil = newTarget == Undefined.instance ? errorFunction : newTarget;

            String messageAsJavaString = messageOpt == null ? null : Strings.toJavaString(messageOpt);
            GraalJSException exception = JSException.createCapture(errorType, messageAsJavaString, errorObj, realm, stackTraceLimit, skipUntil, skipUntil != errorFunction);
            return initErrorObjectNode.execute(errorObj, exception, messageOpt, null, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorPrototype(errorType);
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    @ImportStatic(Strings.class)
    public abstract static class ConstructAggregateErrorNode extends ConstructWithNewTargetNode {
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;
        @Child private DynamicObjectLibrary setMessage;
        @Child private InstallErrorCauseNode installErrorCauseNode;

        public ConstructAggregateErrorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create();
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
            this.setMessage = JSObjectUtil.createDispatched(JSError.MESSAGE);
        }

        @Specialization
        protected JSDynamicObject constructError(JSDynamicObject newTarget, Object errorsObj, Object messageObj, Object options,
                        @Cached JSToStringNode toStringNode,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached IterableToListNode iterableToListNode) {
            JSContext context = getContext();
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            JSErrorObject errorObj = JSError.createErrorObject(context, realm, JSErrorType.AggregateError, proto);

            TruffleString message;
            String messageAsJavaString;
            if (messageObj == Undefined.instance) {
                message = null;
                messageAsJavaString = null;
            } else {
                message = toStringNode.executeString(messageObj);
                setMessage.putWithFlags(errorObj, JSError.MESSAGE, message, JSError.MESSAGE_ATTRIBUTES);
                messageAsJavaString = Strings.toJavaString(message);
            }

            if (context.getLanguageOptions().errorCause() && options != Undefined.instance) {
                installErrorCause(errorObj, options);
            }

            IteratorRecord iterator = getIteratorNode.execute(this, errorsObj);
            SimpleArrayList<Object> errors = iterableToListNode.execute(iterator);
            JSArrayObject errorsArray = JSArray.createConstantObjectArray(context, getRealm(), errors.toArray());

            int stackTraceLimit = stackTraceLimitNode.executeInt();
            JSFunctionObject errorFunction = realm.getErrorConstructor(JSErrorType.AggregateError);

            // We skip until newTarget (if any) so as to also skip user-defined Error constructors.
            JSDynamicObject skipUntil = newTarget == Undefined.instance ? errorFunction : newTarget;

            GraalJSException exception = JSException.createCapture(JSErrorType.AggregateError, messageAsJavaString, errorObj, realm, stackTraceLimit, skipUntil, skipUntil != errorFunction);
            initErrorObjectNode.execute(errorObj, exception, null, errorsArray);
            return errorObj;
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorPrototype(JSErrorType.AggregateError);
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }

        private void installErrorCause(JSObject errorObj, Object options) {
            if (installErrorCauseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                installErrorCauseNode = insert(new InstallErrorCauseNode(getContext()));
            }
            installErrorCauseNode.executeVoid(errorObj, options);
        }
    }

    @ImportStatic({JSArrayBuffer.class, JSConfig.class})
    public abstract static class ConstructDataViewNode extends ConstructWithNewTargetNode {
        public ConstructDataViewNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSDataViewObject ofHeapArrayBuffer(JSDynamicObject newTarget, JSArrayBufferObject.Heap buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedConditionProfile byteLengthCondition,
                        @Cached @Shared JSToIndexNode offsetToIndexNode,
                        @Cached @Shared JSToIndexNode lengthToIndexNode) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, false, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, null);
        }

        @Specialization
        protected final JSDataViewObject ofDirectArrayBuffer(JSDynamicObject newTarget, JSArrayBufferObject.Direct buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedConditionProfile byteLengthCondition,
                        @Cached @Shared JSToIndexNode offsetToIndexNode,
                        @Cached @Shared JSToIndexNode lengthToIndexNode) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, false, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, null);
        }

        @Specialization
        protected final JSDataViewObject ofSharedArrayBuffer(JSDynamicObject newTarget, JSArrayBufferObject.Shared buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedConditionProfile byteLengthCondition,
                        @Cached @Shared JSToIndexNode offsetToIndexNode,
                        @Cached @Shared JSToIndexNode lengthToIndexNode) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, false, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, null);
        }

        @Specialization
        protected final JSDataViewObject ofInteropArrayBuffer(JSDynamicObject newTarget, JSArrayBufferObject.Interop buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedConditionProfile byteLengthCondition,
                        @Cached @Shared JSToIndexNode offsetToIndexNode,
                        @Cached @Shared JSToIndexNode lengthToIndexNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary bufferInterop) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, true, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, bufferInterop);
        }

        @Specialization(guards = {"!isJSAbstractBuffer(buffer)", "bufferInterop.hasBufferElements(buffer)"})
        protected final JSDataViewObject ofInteropBuffer(JSDynamicObject newTarget, Object buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedConditionProfile byteLengthCondition,
                        @Cached @Shared JSToIndexNode offsetToIndexNode,
                        @Cached @Shared JSToIndexNode lengthToIndexNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary bufferInterop) {
            JSArrayBufferObject.Interop arrayBuffer = JSArrayBuffer.createInteropArrayBuffer(getContext(), getRealm(), buffer);
            return ofInteropArrayBuffer(newTarget, arrayBuffer, byteOffset, byteLength, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, bufferInterop);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSAbstractBuffer(buffer)", "!bufferInterop.hasBufferElements(buffer)"})
        protected static JSDynamicObject error(JSDynamicObject newTarget, Object buffer, Object byteOffset, Object byteLength,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary bufferInterop) {
            throw Errors.createTypeError("Not an ArrayBuffer");
        }

        protected final JSDataViewObject constructDataView(JSDynamicObject newTarget, JSArrayBufferObject arrayBuffer, Object byteOffset, Object byteLength,
                        boolean isInteropBuffer,
                        InlinedBranchProfile errorBranch,
                        InlinedConditionProfile byteLengthCondition,
                        JSToIndexNode offsetToIndexNode,
                        JSToIndexNode lengthToIndexNode,
                        InteropLibrary bufferInterop) {
            long offset = offsetToIndexNode.executeLong(byteOffset);

            checkDetachedBuffer(arrayBuffer, errorBranch);

            int bufferByteLength;
            if (isInteropBuffer) {
                bufferByteLength = ConstructArrayBufferNode.getBufferSizeSafe(JSArrayBuffer.getInteropBuffer(arrayBuffer), bufferInterop, this, errorBranch);
            } else {
                bufferByteLength = arrayBuffer.getByteLength();
            }
            if (offset > bufferByteLength) {
                errorBranch.enter(this);
                throw Errors.createRangeError("offset > bufferByteLength");
            }

            final long viewByteLength;
            if (byteLengthCondition.profile(this, byteLength != Undefined.instance)) {
                viewByteLength = lengthToIndexNode.executeLong(byteLength);
                if (viewByteLength < 0) {
                    errorBranch.enter(this);
                    throw Errors.createRangeError("viewByteLength < 0");
                }
                if (offset + viewByteLength > bufferByteLength) {
                    errorBranch.enter(this);
                    throw Errors.createRangeError("offset + viewByteLength > bufferByteLength");
                }
            } else {
                if (arrayBuffer.isFixedLength()) {
                    viewByteLength = bufferByteLength - offset;
                } else {
                    viewByteLength = JSArrayBufferViewBase.AUTO_LENGTH;
                }
            }
            assert offset >= 0 && offset <= Integer.MAX_VALUE && viewByteLength >= -1 && viewByteLength <= Integer.MAX_VALUE;
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);

            // GetPrototypeFromConstructor might have detached the ArrayBuffer as a side effect.
            checkDetachedBuffer(arrayBuffer, errorBranch);
            if (isInteropBuffer) {
                bufferByteLength = ConstructArrayBufferNode.getBufferSizeSafe(JSArrayBuffer.getInteropBuffer(arrayBuffer), bufferInterop, this, errorBranch);
            } else {
                bufferByteLength = arrayBuffer.getByteLength();
            }
            if (offset > bufferByteLength) {
                errorBranch.enter(this);
                throw Errors.createRangeError("offset > bufferByteLength");
            }
            if (byteLengthCondition.profile(this, byteLength != Undefined.instance)) {
                if (offset + viewByteLength > bufferByteLength) {
                    errorBranch.enter(this);
                    throw Errors.createRangeError("offset + viewByteLength > bufferByteLength");
                }
            }

            return JSDataView.createDataView(getContext(), realm, proto, arrayBuffer, (int) offset, (int) viewByteLength);
        }

        private void checkDetachedBuffer(JSArrayBufferObject arrayBuffer, InlinedBranchProfile errorBranch) {
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDataViewPrototype();
        }

    }

    public abstract static class CallRequiresNewNode extends JSBuiltinNode {

        public CallRequiresNewNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization
        protected final Object call() {
            throw Errors.createTypeErrorFormat("Constructor %s requires 'new'", getBuiltin().getName());
        }
    }

    public abstract static class ConstructJSAdapterNode extends JSBuiltinNode {
        public ConstructJSAdapterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isUndefined(undefined1)", "isUndefined(undefined2)"})
        protected JSObject constructJSAdapter(JSObject adaptee, @SuppressWarnings("unused") Object undefined1, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, null, null);
        }

        @Specialization(guards = {"isUndefined(undefined2)"})
        protected JSObject constructJSAdapter(JSObject overrides, JSObject adaptee, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, overrides, null);
        }

        @Specialization
        protected JSObject constructJSAdapter(JSObject proto, JSObject overrides, JSObject adaptee) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, overrides, proto);
        }

        @Fallback
        protected JSObject constructJSAdapter(Object proto, Object overrides, Object adaptee) {
            Object notAnObject;
            if (!JSRuntime.isObject(proto)) {
                notAnObject = proto;
            } else if (!JSRuntime.isObject(overrides)) {
                notAnObject = overrides;
            } else if (!JSRuntime.isObject(adaptee)) {
                notAnObject = adaptee;
            } else {
                throw Errors.shouldNotReachHere();
            }
            throw Errors.createTypeErrorNotAnObject(notAnObject);
        }

    }

    @ImportStatic(value = {JSProxy.class})
    public abstract static class ConstructJSProxyNode extends ConstructWithNewTargetNode {

        public ConstructJSProxyNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject constructJSProxy(JSDynamicObject newTarget, Object target, Object handler,
                        @Cached InlinedConditionProfile targetNonObject,
                        @Cached InlinedConditionProfile handlerNonObject) {
            if (targetNonObject.profile(this, !JSGuards.isTruffleObject(target) || target instanceof Symbol || target == Undefined.instance || target == Null.instance ||
                            target instanceof TruffleString || target instanceof SafeInteger || target instanceof BigInt)) {
                throw Errors.createTypeError("target expected to be an object");
            }
            if (handlerNonObject.profile(this, !JSGuards.isJSObject(handler))) {
                throw Errors.createTypeError("handler expected to be an object");
            }
            JSDynamicObject handlerObj = (JSDynamicObject) handler;
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSProxy.create(getContext(), realm, proto, target, handlerObj);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getProxyPrototype();
        }

        public abstract JSDynamicObject execute(JSDynamicObject newTarget, Object target, Object handler);
    }

    public abstract static class ConstructJavaImporterNode extends JSBuiltinNode {
        public ConstructJavaImporterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject constructJavaImporter(Object[] args) {
            JSRealm realm = getRealm();
            TruffleLanguage.Env env = realm.getEnv();
            SimpleArrayList<Object> imports = new SimpleArrayList<>(args.length);
            for (Object anImport : args) {
                if (JavaPackage.isJavaPackage(anImport)) {
                    imports.addUnchecked(anImport);
                } else if (env.isHostObject(anImport)) {
                    InteropLibrary interop = InteropLibrary.getUncached(anImport);
                    if (interop.isMetaObject(anImport)) {
                        imports.addUnchecked(anImport);
                    }
                }
            }
            return JavaImporter.create(getContext(), realm, imports.toArray());
        }
    }

    public abstract static class JSConstructIterableOperation extends ConstructWithNewTargetNode {
        public JSConstructIterableOperation(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private JSFunctionCallNode callAdderNode;
        @Child private PropertyGetNode getAdderFnNode;

        protected void iteratorCloseAbrupt(Object iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected Object getIteratorValue(Object iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create());
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected Object iteratorStep(IteratorRecord iterator) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create());
            }
            return iteratorStepNode.execute(iterator);
        }

        protected Object call(Object target, Object function, Object... userArguments) {
            if (callAdderNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callAdderNode = insert(JSFunctionCallNode.createCall());
            }
            return callAdderNode.executeCall(JSArguments.create(target, function, userArguments));
        }

        protected Object getAdderFn(JSDynamicObject obj, TruffleString name) {
            if (getAdderFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAdderFnNode = insert(PropertyGetNode.create(name, getContext()));
            }
            return getAdderFnNode.getValue(obj);
        }
    }

    public abstract static class ConstructMapNode extends JSConstructIterableOperation {

        public ConstructMapNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = "isNullOrUndefined(iterable)")
        protected JSObject constructEmptyMap(JSDynamicObject newTarget, @SuppressWarnings("unused") Object iterable) {
            return newMapObject(newTarget);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!isNullOrUndefined(iterable)")
        protected final JSObject constructMapFromIterable(JSDynamicObject newTarget, Object iterable,
                        @Bind Node node,
                        @Cached("create(getContext())") ReadElementNode readElementNode,
                        @Cached IsObjectNode isObjectNode,
                        @Cached IsCallableNode isCallableNode,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSObject mapObj = newMapObject(newTarget);

            Object adder = getAdderFn(mapObj, Strings.SET);
            if (!isCallableNode.executeBoolean(adder)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("function set not callable");
            }
            IteratorRecord iter = getIteratorNode.execute(node, iterable);

            try {
                while (true) {
                    Object next = iteratorStep(iter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextItem = getIteratorValue(next);
                    if (!isObjectNode.executeBoolean(nextItem)) {
                        errorBranch.enter(node);
                        throw Errors.createTypeErrorIteratorResultNotObject(nextItem, this);
                    }
                    Object k = readElementNode.executeWithTargetAndIndex(nextItem, 0);
                    Object v = readElementNode.executeWithTargetAndIndex(nextItem, 1);
                    call(mapObj, adder, k, v);
                }
            } catch (AbstractTruffleException ex) {
                iteratorCloseAbrupt(iter.getIterator());
                throw ex;
            }

            return mapObj;
        }

        protected JSObject newMapObject(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSMap.create(getContext(), realm, proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getMapPrototype();
        }

    }

    public abstract static class ConstructSetNode extends JSConstructIterableOperation {
        public ConstructSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = "isNullOrUndefined(iterable)")
        protected JSObject constructEmptySet(JSDynamicObject newTarget, @SuppressWarnings("unused") Object iterable) {
            return newSetObject(newTarget);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!isNullOrUndefined(iterable)")
        protected JSObject constructSetFromIterable(JSDynamicObject newTarget, Object iterable,
                        @Bind Node node,
                        @Cached IsCallableNode isCallableNode,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSObject setObj = newSetObject(newTarget);

            Object adder = getAdderFn(setObj, Strings.ADD);
            if (!isCallableNode.executeBoolean(adder)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("function add not callable");
            }
            IteratorRecord iter = getIteratorNode.execute(node, iterable);

            try {
                while (true) {
                    Object next = iteratorStep(iter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextValue = getIteratorValue(next);
                    call(setObj, adder, nextValue);
                }
            } catch (AbstractTruffleException ex) {
                iteratorCloseAbrupt(iter.getIterator());
                throw ex;
            }

            return setObj;
        }

        protected JSObject newSetObject(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSSet.create(getContext(), realm, proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSetPrototype();
        }

    }

    public abstract static class ConstructIteratorNode extends ConstructWithNewTargetNode {
        public ConstructIteratorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        protected final boolean isValidTarget(VirtualFrame frame, JSDynamicObject newTarget) {
            return isNewTargetCase && newTarget != Undefined.instance && newTarget != JSFrameUtil.getFunctionObjectNoCast(frame);
        }

        @Specialization(guards = {"isValidTarget(frame, newTarget)"})
        protected JSObject constructIterator(@SuppressWarnings("unused") VirtualFrame frame, JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSIterator.create(getContext(), realm, proto);
        }

        @Specialization(guards = {"!isValidTarget(frame, newTarget)"})
        protected JSObject constructIteratorTypeError(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject newTarget,
                        @SuppressWarnings("unused") Object[] args) {
            throw Errors.createTypeError("Cannot construct a new Iterator as it is an abstract class.");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getIteratorPrototype();
        }
    }

    public abstract static class ConstructAsyncIteratorNode extends ConstructWithNewTargetNode {
        public ConstructAsyncIteratorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        protected final boolean isValidTarget(VirtualFrame frame, JSDynamicObject newTarget) {
            return isNewTargetCase && newTarget != Undefined.instance && newTarget != JSFrameUtil.getFunctionObjectNoCast(frame);
        }

        @Specialization(guards = {"isValidTarget(frame, newTarget)"})
        protected JSObject constructIterator(@SuppressWarnings("unused") VirtualFrame frame, JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSAsyncIterator.create(getContext(), realm, proto);
        }

        @Specialization(guards = {"!isValidTarget(frame, newTarget)"})
        protected JSObject constructIteratorTypeError(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject newTarget,
                        @SuppressWarnings("unused") Object[] args) {
            throw Errors.createTypeError("Cannot construct a new AsyncIterator as it is an abstract class.");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getAsyncIteratorPrototype();
        }
    }

    public abstract static class ConstructWeakSetNode extends ConstructSetNode {
        public ConstructWeakSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Override
        protected JSObject newSetObject(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWeakSet.create(getContext(), getRealm(), proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakSetPrototype();
        }

    }

    public abstract static class ConstructWeakMapNode extends ConstructMapNode {
        public ConstructWeakMapNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Override
        protected JSObject newMapObject(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWeakMap.create(getContext(), realm, proto);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakMapPrototype();
        }

    }

    @ImportStatic(Symbol.class)
    public abstract static class CallSymbolNode extends JSBuiltinNode implements JSBuiltinNode.Inlineable {

        public CallSymbolNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Symbol callSymbolString(TruffleString value) {
            return Symbol.create(value);
        }

        @Specialization(guards = "!isString(value)")
        protected Symbol callSymbolGeneric(Object value,
                        @Cached JSToStringNode toStringNode) {
            return Symbol.create(value == Undefined.instance ? null : toStringNode.executeString(value));
        }

        @Override
        public Inlined createInlined() {
            return CallSymbolNodeGen.InlinedNodeGen.create(getContext(), getBuiltin(), new JavaScriptNode[0]);
        }

        @SuppressWarnings("unused")
        public abstract static class Inlined extends CallSymbolNode implements JSBuiltinNode.Inlined {

            public Inlined(JSContext context, JSBuiltin builtin) {
                super(context, builtin);
            }

            protected abstract Object executeWithArguments(Object arg0);

            @Specialization(guards = {"acceptCache(equalNode, value, cachedValue, symbolUsageMarker)"}, limit = "1")
            protected Symbol callSymbolSingleton(TruffleString value,
                            @Cached("value") TruffleString cachedValue,
                            @Cached TruffleString.EqualNode equalNode,
                            @Cached("createSymbolUsageMarker()") AtomicReference<Object> symbolUsageMarker,
                            @Cached(value = "createCachedSingletonSymbol(value)", weak = true) Symbol cachedSymbol) {
                return cachedSymbol;
            }

            @Override
            @Specialization
            protected Symbol callSymbolString(TruffleString value) {
                throw rewriteToCall();
            }

            @Specialization
            protected TruffleString callInlinedSymbolGeneric(Object value) {
                throw rewriteToCall();
            }

            @Override
            public Object callInlined(Object[] arguments) {
                if (JSArguments.getUserArgumentCount(arguments) < 1) {
                    throw rewriteToCall();
                }
                return executeWithArguments(JSArguments.getUserArgument(arguments, 0));
            }

            @TruffleBoundary
            protected boolean acceptCache(TruffleString.EqualNode equalNode, TruffleString value, TruffleString cachedValue, AtomicReference<Object> symbolUsageMarker) {
                if (getContext().isMultiContext() && JSConfig.UseSingletonSymbols && Strings.equals(equalNode, value, cachedValue)) {
                    Object currentMarker = getContext().getSymbolUsageMarker();
                    Object oldMarker = symbolUsageMarker.getAndSet(currentMarker);
                    return currentMarker != oldMarker;
                }
                return false;
            }

            protected AtomicReference<Object> createSymbolUsageMarker() {
                return new AtomicReference<>();
            }

            protected Symbol createCachedSingletonSymbol(TruffleString value) {
                return Symbol.create(value);
            }
        }
    }

    public abstract static class ConstructSymbolNode extends JSBuiltinNode {

        public ConstructSymbolNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static final Object construct() {
            throw Errors.createTypeError("cannot construct a Symbol");
        }
    }

    public abstract static class PromiseConstructorNode extends JSBuiltinNode {
        @Child protected IsCallableNode isCallable;
        @Child private PromiseResolveThenableNode promiseResolveThenable;
        @Child private OrdinaryCreateFromConstructorNode createPromiseFromConstructor;

        public PromiseConstructorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isCallable = IsCallableNode.create();
            this.promiseResolveThenable = PromiseResolveThenableNode.create(context);
            this.createPromiseFromConstructor = OrdinaryCreateFromConstructorNode.create(context, null, JSRealm::getPromisePrototype, JSPromise.INSTANCE);
        }

        @Specialization(guards = "isCallable.executeBoolean(executor)")
        protected JSPromiseObject construct(JSDynamicObject newTarget, Object executor) {
            JSPromiseObject promise = (JSPromiseObject) createPromiseFromConstructor.executeWithConstructor(newTarget);
            promise.setPromiseState(JSPromise.PENDING);
            promise.setIsHandled(false);
            promise.allocatePromiseReactions();

            getContext().notifyPromiseHook(PromiseHook.TYPE_INIT, promise);

            promiseResolveThenable.executePromiseConstructor(promise, executor);
            return promise;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable.executeBoolean(executor)")
        protected JSPromiseObject notCallable(JSDynamicObject newTarget, Object executor) {
            throw Errors.createTypeError("cannot create promise: executor not callable");
        }
    }

    public abstract static class ConstructShadowRealmNode extends ConstructWithNewTargetNode {

        public ConstructShadowRealmNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject construct(JSDynamicObject newTarget) {
            JSRealm currentRealm = getRealm();
            JSRealm shadowRealm = currentRealm.createChildRealm();
            hostInitializeShadowRealm(shadowRealm);
            JSDynamicObject proto = getPrototype(currentRealm, newTarget);
            return JSShadowRealm.create(getContext(), currentRealm, proto, shadowRealm);
        }

        private void hostInitializeShadowRealm(@SuppressWarnings("unused") JSRealm shadowRealm) {
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getShadowRealmPrototype();
        }
    }

    public abstract static class ConstructWebAssemblyModuleNode extends ConstructWithNewTargetNode {
        @Child ExportByteSourceNode exportByteSourceNode;
        @Child InteropLibrary decodeModuleLib;

        public ConstructWebAssemblyModuleNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.exportByteSourceNode = ExportByteSourceNode.create(context, "WebAssembly.Module(): Argument 0 must be a buffer source", "WebAssembly.Module(): BufferSource argument is empty");
            this.decodeModuleLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSObject constructModule(JSDynamicObject newTarget, Object bytes) {
            ByteSequence byteSource = exportByteSourceNode.execute(bytes);
            JSRealm realm = getRealm();
            Source wasmSource = WebAssemblyBuiltins.buildSource(byteSource);
            Object wasmModule;
            try {
                wasmModule = WebAssemblyBuiltins.moduleDecode(realm, wasmSource);
            } catch (AbstractTruffleException tex) {
                try {
                    ExceptionType type = InteropLibrary.getUncached(tex).getExceptionType(tex);
                    if (type == ExceptionType.PARSE_ERROR) {
                        throw Errors.createCompileError(tex, this);
                    }
                } catch (UnsupportedMessageException ex) {
                    throw Errors.shouldNotReachHere(ex);
                }
                throw tex;
            }
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWebAssemblyModule.create(getContext(), realm, proto, wasmModule, wasmSource);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyModulePrototype();
        }

    }

    public abstract static class ConstructWebAssemblyInstanceNode extends ConstructWithNewTargetNode {
        @Child IsObjectNode isObjectNode;
        @Child InteropLibrary instantiateModuleLib;

        public ConstructWebAssemblyInstanceNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.instantiateModuleLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSObject constructInstanceFromModule(JSDynamicObject newTarget, JSWebAssemblyModuleObject module, Object importObject) {
            if (importObject != Undefined.instance && !isObjectNode.executeBoolean(importObject)) {
                throw Errors.createTypeError("WebAssembly.Instance(): Argument 1 must be an object", this);
            }

            Object wasmInstance;
            Object wasmModule = module.getWASMModule();
            JSRealm realm = getRealm();
            try {
                Object wasmImportObject = JSWebAssemblyInstance.transformImportObject(getContext(), realm, wasmModule, importObject);
                Object instantiate = realm.getWASMModuleInstantiate();
                try {
                    wasmInstance = instantiateModuleLib.execute(instantiate, wasmModule, wasmImportObject);
                } catch (GraalJSException jsex) {
                    throw jsex;
                } catch (AbstractTruffleException tex) {
                    throw Errors.createLinkError(tex, this);
                }
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWebAssemblyInstance.create(getContext(), realm, proto, wasmInstance, wasmModule);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWebAssemblyModule(other)")
        protected JSObject constructInstanceFromOther(JSDynamicObject newTarget, Object other, Object importObject) {
            throw Errors.createTypeError("WebAssembly.Instance(): Argument 0 must be a WebAssembly.Module");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyInstancePrototype();
        }

    }

    public abstract static class ConstructWebAssemblyMemoryNode extends ConstructWithNewTargetNode {

        @Child IsObjectNode isObjectNode;
        @Child PropertyGetNode getInitialNode;
        @Child PropertyGetNode getMaximumNode;
        @Child GetBooleanOptionNode getSharedNode;
        @Child ToWebAssemblyIndexOrSizeNode toInitialSizeNode;
        @Child ToWebAssemblyIndexOrSizeNode toMaximumSizeNode;
        @Child InteropLibrary memAllocLib;

        public ConstructWebAssemblyMemoryNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.getInitialNode = PropertyGetNode.create(Strings.INITIAL, context);
            this.getMaximumNode = PropertyGetNode.create(Strings.MAXIMUM, context);
            this.getSharedNode = GetBooleanOptionNode.create(context, Strings.SHARED, false);
            this.toInitialSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Memory(): Property 'initial'");
            this.toMaximumSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Memory(): Property 'maximum'");
            this.memAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSObject constructMemory(JSDynamicObject newTarget, Object descriptor,
                        @Cached InlinedConditionProfile isShared) {
            if (!isObjectNode.executeBoolean(descriptor)) {
                throw Errors.createTypeError("WebAssembly.Memory(): Argument 0 must be a memory descriptor", this);
            }
            Object initial = getInitialNode.getValue(descriptor);
            if (initial == Undefined.instance) {
                throw Errors.createTypeError("WebAssembly.Memory(): Property 'initial' is required", this);
            }
            int initialInt = toInitialSizeNode.executeInt(initial);
            if (initialInt > JSWebAssemblyMemory.MAX_MEMORY_SIZE) {
                throw Errors.createRangeErrorFormat("WebAssembly.Memory(): Property 'initial': value %d is above the upper bound %d", this, initialInt, JSWebAssemblyMemory.MAX_MEMORY_SIZE);
            }
            Boolean shared = getSharedNode.executeValue(descriptor);
            boolean sharedBoolean = isShared.profile(this, Boolean.TRUE.equals(shared));
            int maximumInt;
            Object maximum = getMaximumNode.getValue(descriptor);
            if (maximum == Undefined.instance) {
                if (sharedBoolean) {
                    throw Errors.createTypeError("WebAssembly.Memory(): Property 'maximum' is required for shared memory", this);
                }
                maximumInt = JSWebAssemblyMemory.MAX_MEMORY_SIZE;
            } else {
                maximumInt = toMaximumSizeNode.executeInt(maximum);
                if (maximumInt < initialInt) {
                    throw Errors.createRangeErrorFormat("WebAssembly.Memory(): Property 'maximum': value %d is below the lower bound %d", this, maximumInt, initialInt);
                }
                if (maximumInt > JSWebAssemblyMemory.MAX_MEMORY_SIZE) {
                    throw Errors.createRangeErrorFormat("WebAssembly.Memory(): Property 'maximum': value %d is above the upper bound %d", this, maximumInt, JSWebAssemblyMemory.MAX_MEMORY_SIZE);
                }
            }
            JSRealm realm = getRealm();
            Object wasmMemory;
            try {
                Object createMemory = realm.getWASMMemAlloc();
                wasmMemory = memAllocLib.execute(createMemory, initialInt, maximumInt, sharedBoolean);
            } catch (AbstractTruffleException tex) {
                throw createCouldNotAllocateMemoryError(tex);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWebAssemblyMemory.create(getContext(), realm, proto, wasmMemory, sharedBoolean);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyMemoryPrototype();
        }

        @TruffleBoundary
        private JSException createCouldNotAllocateMemoryError(AbstractTruffleException cause) {
            return Errors.createRangeErrorFormat("WebAssembly.Memory(): %s", this, cause.getMessage());
        }

    }

    public abstract static class ConstructWebAssemblyTableNode extends ConstructWithNewTargetNode {

        @Child IsObjectNode isObjectNode;
        @Child PropertyGetNode getElementNode;
        @Child PropertyGetNode getInitialNode;
        @Child PropertyGetNode getMaximumNode;
        @Child ToWebAssemblyIndexOrSizeNode toInitialSizeNode;
        @Child ToWebAssemblyIndexOrSizeNode toMaximumSizeNode;
        @Child InteropLibrary tableAllocLib;

        public ConstructWebAssemblyTableNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.getElementNode = PropertyGetNode.create(Strings.ELEMENT, context);
            this.getInitialNode = PropertyGetNode.create(Strings.INITIAL, context);
            this.getMaximumNode = PropertyGetNode.create(Strings.MAXIMUM, context);
            this.toInitialSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table(): Property 'initial'");
            this.toMaximumSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table(): Property 'maximum'");
            this.tableAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSObject constructTable(JSDynamicObject newTarget, Object descriptor, Object[] args,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.ToJavaStringNode toJavaString,
                        @Cached ToWebAssemblyValueNode toWebAssemblyValueNode) {
            if (!isObjectNode.executeBoolean(descriptor)) {
                throw Errors.createTypeError("WebAssembly.Table(): Argument 0 must be a table descriptor", this);
            }
            String elementKindStr = toJavaString.execute(toStringNode.executeString(getElementNode.getValue(descriptor)));
            WebAssemblyValueType elementKind = WebAssemblyValueType.lookupType(elementKindStr);
            if (elementKind == null || !elementKind.isReference()) {
                throw Errors.createTypeError("WebAssembly.Table(): Descriptor property 'element' must be 'anyfunc' or 'externref'", this);
            }
            Object initial = getInitialNode.getValue(descriptor);
            if (initial == Undefined.instance) {
                throw Errors.createTypeError("WebAssembly.Table(): Property 'initial' is required", this);
            }
            int initialInt = toInitialSizeNode.executeInt(initial);
            if (initialInt > JSWebAssemblyTable.MAX_TABLE_SIZE) {
                throw Errors.createRangeErrorFormat("WebAssembly.Table(): Property 'initial': value %d is above the upper bound %d", this, initialInt, JSWebAssemblyTable.MAX_TABLE_SIZE);
            }
            int maximumInt;
            Object maximum = getMaximumNode.getValue(descriptor);
            if (maximum == Undefined.instance) {
                maximumInt = JSWebAssemblyTable.MAX_TABLE_SIZE;
            } else {
                maximumInt = toMaximumSizeNode.executeInt(maximum);
                if (initialInt > maximumInt) {
                    throw Errors.createRangeErrorFormat("WebAssembly.Table(): Property 'maximum': value %d is below the lower bound %d", this, maximumInt, initialInt);
                }
                if (maximumInt > JSWebAssemblyTable.MAX_TABLE_SIZE) {
                    throw Errors.createRangeErrorFormat("WebAssembly.Table(): Property 'maximum': value %d is above the upper bound %d", this, maximumInt, JSWebAssemblyTable.MAX_TABLE_SIZE);
                }
            }
            final JSRealm realm = getRealm();
            Object wasmValue;
            if (args.length == 0) {
                wasmValue = elementKind.getDefaultValue(realm);
            } else {
                wasmValue = toWebAssemblyValueNode.execute(args[0], elementKind);
            }
            Object wasmTable;
            try {
                Object createTable = realm.getWASMTableAlloc();
                wasmTable = tableAllocLib.execute(createTable, initialInt, maximumInt, elementKindStr, wasmValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWebAssemblyTable.create(getContext(), realm, proto, wasmTable, elementKind);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyTablePrototype();
        }

    }

    public abstract static class ConstructWebAssemblyGlobalNode extends ConstructWithNewTargetNode {
        @Child PropertyGetNode getValueNode;
        @Child PropertyGetNode getMutableNode;
        @Child InteropLibrary globalAllocLib;

        public ConstructWebAssemblyGlobalNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.getValueNode = PropertyGetNode.create(Strings.VALUE, context);
            this.getMutableNode = PropertyGetNode.create(Strings.MUTABLE, context);
            this.globalAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSObject constructGlobal(JSDynamicObject newTarget, Object descriptor, Object[] args,
                        @Cached IsObjectNode isObjectNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.ToJavaStringNode toJavaString,
                        @Cached ToWebAssemblyValueNode toWebAssemblyValueNode) {
            if (!isObjectNode.executeBoolean(descriptor)) {
                throw Errors.createTypeError("WebAssembly.Global(): Argument 0 must be a global descriptor", this);
            }
            boolean mutable = toBooleanNode.executeBoolean(this, getMutableNode.getValue(descriptor));
            String valueTypeStr = toJavaString.execute(toStringNode.executeString(getValueNode.getValue(descriptor)));
            WebAssemblyValueType valueType = WebAssemblyValueType.lookupType(valueTypeStr);
            if (valueType == null) {
                throw Errors.createTypeError("WebAssembly.Global(): Descriptor property 'value' must be a WebAssembly type (i32, i64, f32, f64, anyfunc, externref)", this);
            }
            if (valueType == WebAssemblyValueType.v128) {
                throw Errors.createTypeError("WebAssembly.Global(): Descriptor property 'value' must not be v128", this);
            }
            final JSRealm realm = getRealm();
            Object webAssemblyValue;
            // According to the spec only missing values should produce a default value.
            // According to the tests also undefined should use the default value.
            if (args.length == 0 || args[0] == Undefined.instance) {
                webAssemblyValue = valueType.getDefaultValue(realm);
            } else {
                if (!getContext().getLanguageOptions().wasmBigInt() && valueType == WebAssemblyValueType.i64) {
                    throw Errors.createTypeError("WebAssembly.Global(): Can't set the value of i64 WebAssembly.Global", this);
                }
                webAssemblyValue = toWebAssemblyValueNode.execute(args[0], valueType);
            }
            Object wasmGlobal;
            try {
                Object createGlobal = realm.getWASMGlobalAlloc();
                wasmGlobal = globalAllocLib.execute(createGlobal, valueTypeStr, mutable, webAssemblyValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            JSDynamicObject proto = getPrototype(realm, newTarget);
            return JSWebAssemblyGlobal.create(getContext(), realm, proto, wasmGlobal, valueType, mutable);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyGlobalPrototype();
        }

    }

    public abstract static class ConstructWorkerNode extends ConstructWithNewTargetNode {
        private static final TruffleString STRINGIFY = Strings.constant("stringify");
        private static final TruffleString CLASSIC = Strings.constant("classic");

        public ConstructWorkerNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject construct(JSDynamicObject newTarget, Object codeParam, Object options) {
            JSRealm realm = getRealm();

            if (!(codeParam instanceof TruffleString) && !JSFunction.isJSFunction(codeParam)) {
                throw Errors.createError("1st argument must be a string or a function");
            }

            String code = null;
            if (JSRuntime.isObject(options)) {
                Object typeValue = JSRuntime.get(options, Strings.TYPE);
                Object arguments = JSRuntime.get(options, Strings.ARGUMENTS);
                if (typeValue instanceof TruffleString type) {
                    if (Strings.equals(type, CLASSIC)) {
                        code = codeFromFileName(realm, codeParam);
                    } else if (Strings.equals(type, Strings.FUNCTION)) {
                        code = codeFromFunction(realm, codeParam, arguments);
                    } else if (Strings.equals(type, Strings.STRING)) {
                        if (codeParam instanceof TruffleString codeTS) {
                            code = Strings.toJavaString(codeTS);
                        }
                    }
                }
            } else {
                code = codeFromFileName(realm, codeParam);
            }
            if (code == null) {
                throw Errors.createError("Invalid argument");
            }

            JSDynamicObject proto = getPrototype(realm, newTarget);
            WorkerAgent agent = new WorkerAgent();
            agent.start(code);
            return JSWorker.create(getContext(), realm, proto, agent);
        }

        @TruffleBoundary
        private static String codeFromFileName(JSRealm realm, Object fileNameParam) {
            if (fileNameParam instanceof TruffleString fileName) {
                try {
                    TruffleFile file = GlobalBuiltins.resolveRelativeFilePath(Strings.toJavaString(fileName), realm.getEnv());
                    if (file.isRegularFile()) {
                        return new String(file.readAllBytes(), StandardCharsets.UTF_8);
                    } else {
                        throw cannotLoadScript(fileName);
                    }
                } catch (IOException | SecurityException | IllegalArgumentException ex) {
                    throw Errors.createErrorFromException(ex);
                }
            }
            return null;
        }

        @TruffleBoundary
        private static JSException cannotLoadScript(TruffleString fileName) {
            return Errors.createError("Cannot load script: " + fileName);
        }

        @TruffleBoundary
        private static String codeFromFunction(JSRealm realm, Object functionParam, Object arguments) {
            if (!JSFunction.isJSFunction(functionParam)) {
                return null;
            }
            JSFunctionObject function = (JSFunctionObject) functionParam;
            CallTarget callTarget = JSFunction.getCallTarget(function);
            if (!(callTarget instanceof RootCallTarget)) {
                return null;
            }
            RootNode rootNode = ((RootCallTarget) callTarget).getRootNode();
            SourceSection sourceSection = rootNode.getSourceSection();
            if (sourceSection == null || !sourceSection.isAvailable() || sourceSection.getSource().isInternal()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(sourceSection.getCharacters().toString());
            sb.append(")(");
            if (arguments != Undefined.instance) {
                if (JSArray.isJSArray(arguments)) {
                    JSFunctionObject stringify = realm.lookupFunction(JSONBuiltins.BUILTINS, STRINGIFY);
                    JSArrayObject args = (JSArrayObject) arguments;
                    long length = JSAbstractArray.arrayGetLength(args);
                    for (long i = 0; i < length; i++) {
                        Object argument = JSObject.get(args, i);
                        if (i != 0) {
                            sb.append(',');
                        }
                        Object json = JSFunction.call(stringify, Undefined.instance, new Object[]{argument});
                        sb.append(JSRuntime.toString(json));
                    }
                } else {
                    throw Errors.createError("'arguments' must be an array");
                }
            }
            sb.append(')');
            return sb.toString();
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getWorkerPrototype();
        }
    }

}
