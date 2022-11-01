/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.utilities.AssumedValue;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallCollatorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallFetchResponseNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallFetchRequestNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallFetchHeadersNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallDateTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallNumberFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallNumberNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallRequiresNewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallTypedArrayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructAggregateErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFetchErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayBufferNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructCollatorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDataViewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFetchResponseNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFetchRequestNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFetchHeadersNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDisplayNamesNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFinalizationRegistryNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFunctionNodeGen;
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
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalCalendarNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalDurationNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalInstantNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalTimeZoneNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructTemporalZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakRefNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyGlobalNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyInstanceNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyMemoryNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyModuleNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWebAssemblyTableNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CreateDynamicFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.PromiseConstructorNodeGen;
import com.oracle.truffle.js.builtins.helper.FetchHeaders;
import com.oracle.truffle.js.builtins.helper.FetchRequest;
import com.oracle.truffle.js.builtins.helper.FetchResponse;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode.ArrayContentType;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.GetIteratorBaseNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeFromConstructorNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.access.InstallErrorCauseNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IsRegExpNode;
import com.oracle.truffle.js.nodes.access.IterableToListNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.ArrayCreateNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
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
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarWithISODefaultNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
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
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateObject;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFetchHeaders;
import com.oracle.truffle.js.runtime.builtins.JSFetchRequest;
import com.oracle.truffle.js.runtime.builtins.JSFetchResponse;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyValueTypes;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
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
        FetchError(3),
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

        // Fetch
        Response(2),
        Request(2),
        Headers(1),

        // Temporal
        PlainTime(0),
        PlainDate(3),
        PlainDateTime(3),
        Duration(0),
        Calendar(1),
        PlainYearMonth(2),
        PlainMonthDay(2),
        Instant(1),
        TimeZone(1),
        ZonedDateTime(2),

        // --- not new.target-capable below ---
        TypedArray(0),
        Symbol(0),

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
            if (AsyncGeneratorFunction == this) {
                return JSConfig.ECMAScript2018;
            } else if (EnumSet.of(SharedArrayBuffer, AsyncFunction).contains(this)) {
                return JSConfig.ECMAScript2017;
            } else if (EnumSet.range(Map, Symbol).contains(this)) {
                return 6;
            } else if (EnumSet.of(PlainTime, Calendar, Duration, PlainDate, PlainDateTime, PlainYearMonth, PlainMonthDay, Instant, TimeZone, ZonedDateTime).contains(this)) {
                return JSConfig.ECMAScript2022;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Constructor builtinEnum) {
        switch (builtinEnum) {
            case Array:
                if (newTarget) {
                    return ConstructArrayNodeGen.create(context, builtin, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructArrayNodeGen.create(context, builtin, false, args().function().varArgs().createArgumentNodes(context));
            case Boolean:
                return construct ? (newTarget
                                ? ConstructBooleanNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                : ConstructBooleanNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context)))
                                : CallBooleanNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case Date:
                return construct ? (newTarget
                                ? ConstructDateNodeGen.create(context, builtin, true, args().newTarget().varArgs().createArgumentNodes(context))
                                : ConstructDateNodeGen.create(context, builtin, false, args().function().varArgs().createArgumentNodes(context)))
                                : CallDateNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case RegExp:
                return construct ? (newTarget
                                ? ConstructRegExpNodeGen.create(context, builtin, false, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructRegExpNodeGen.create(context, builtin, false, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : ConstructRegExpNodeGen.create(context, builtin, true, false, args().function().fixedArgs(2).createArgumentNodes(context));
            case String:
                return construct ? (newTarget
                                ? ConstructStringNodeGen.create(context, builtin, true, args().newTarget().varArgs().createArgumentNodes(context))
                                : ConstructStringNodeGen.create(context, builtin, false, args().function().varArgs().createArgumentNodes(context)))
                                : CallStringNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));

            case WeakRef:
                if (construct) {
                    return newTarget ? ConstructWeakRefNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructWeakRefNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case FinalizationRegistry:
                if (construct) {
                    return newTarget ? ConstructFinalizationRegistryNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructFinalizationRegistryNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case Response:
                return construct ? (newTarget
                        ? ConstructFetchResponseNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                        : ConstructFetchResponseNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                        : CallFetchResponseNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case Request:
                return construct ? (newTarget
                        ? ConstructFetchRequestNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                        : ConstructFetchRequestNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                        : CallFetchRequestNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case Headers:
                return construct ? (newTarget
                        ? ConstructFetchHeadersNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                        : ConstructFetchHeadersNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context)))
                        : CallFetchHeadersNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));

            case Collator:
                return construct ? (newTarget
                                ? ConstructCollatorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructCollatorNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : CallCollatorNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case ListFormat:
                return construct ? (newTarget
                                ? ConstructListFormatNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructListFormatNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case NumberFormat:
                return construct ? (newTarget
                                ? ConstructNumberFormatNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructNumberFormatNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : CallNumberFormatNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case PluralRules:
                return construct ? (newTarget
                                ? ConstructPluralRulesNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructPluralRulesNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case DateTimeFormat:
                return construct ? (newTarget
                                ? ConstructDateTimeFormatNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructDateTimeFormatNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : CallDateTimeFormatNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case RelativeTimeFormat:
                return construct ? (newTarget
                                ? ConstructRelativeTimeFormatNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructRelativeTimeFormatNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Segmenter:
                return construct ? (newTarget
                                ? ConstructSegmenterNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructSegmenterNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case DisplayNames:
                return construct ? (newTarget
                                ? ConstructDisplayNamesNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructDisplayNamesNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Locale:
                return construct ? (newTarget
                                ? ConstructLocaleNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructLocaleNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Object:
                if (newTarget) {
                    return ConstructObjectNodeGen.create(context, builtin, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructObjectNodeGen.create(context, builtin, false, args().function().varArgs().createArgumentNodes(context));
            case Number:
                return construct ? (newTarget
                                ? ConstructNumberNodeGen.create(context, builtin, true, args().newTarget().varArgs().createArgumentNodes(context))
                                : ConstructNumberNodeGen.create(context, builtin, false, args().function().varArgs().createArgumentNodes(context)))
                                : CallNumberNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case BigInt:
                return construct ? ConstructBigIntNodeGen.create(context, builtin, args().createArgumentNodes(context))
                                : CallBigIntNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case Function:
                if (newTarget) {
                    return ConstructFunctionNodeGen.create(context, builtin, false, false, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructFunctionNodeGen.create(context, builtin, false, false, false, args().function().varArgs().createArgumentNodes(context));
            case ArrayBuffer:
                if (construct) {
                    return newTarget ? ConstructArrayBufferNodeGen.create(context, builtin, false, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructArrayBufferNodeGen.create(context, builtin, false, false, args().function().fixedArgs(1).createArgumentNodes(context));
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
                if (newTarget) {
                    return ConstructErrorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context));
                }
                return ConstructErrorNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context));
            case AggregateError:
                if (newTarget) {
                    return ConstructAggregateErrorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(3).createArgumentNodes(context));
                }
                return ConstructAggregateErrorNodeGen.create(context, builtin, false, args().function().fixedArgs(3).createArgumentNodes(context));
            case FetchError:
                if (newTarget) {
                    return ConstructFetchErrorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context));
                }
                return ConstructFetchErrorNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));

            case TypedArray:
                return CallTypedArrayNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case Int8Array:
            case Uint8Array:
            case Uint8ClampedArray:
            case Int16Array:
            case Uint16Array:
            case Int32Array:
            case Uint32Array:
            case Float32Array:
            case Float64Array:
            case BigInt64Array:
            case BigUint64Array:
                if (construct) {
                    if (newTarget) {
                        return JSConstructTypedArrayNodeGen.create(context, builtin, args().newTarget().fixedArgs(3).createArgumentNodes(context));
                    } else {
                        return JSConstructTypedArrayNodeGen.create(context, builtin, args().function().fixedArgs(3).createArgumentNodes(context));
                    }
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case DataView:
                if (construct) {
                    return newTarget ? ConstructDataViewNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(3).createArgumentNodes(context))
                                    : ConstructDataViewNodeGen.create(context, builtin, false, args().function().fixedArgs(3).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case Map:
                if (construct) {
                    return newTarget ? ConstructMapNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructMapNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Set:
                if (construct) {
                    return newTarget ? ConstructSetNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructSetNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case WeakMap:
                if (construct) {
                    return newTarget ? ConstructWeakMapNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructWeakMapNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case WeakSet:
                if (construct) {
                    return newTarget ? ConstructWeakSetNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructWeakSetNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case GeneratorFunction:
                if (newTarget) {
                    return ConstructFunctionNodeGen.create(context, builtin, true, false, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructFunctionNodeGen.create(context, builtin, true, false, false, args().function().varArgs().createArgumentNodes(context));
            case SharedArrayBuffer:
                if (construct) {
                    return newTarget ? ConstructArrayBufferNodeGen.create(context, builtin, true, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructArrayBufferNodeGen.create(context, builtin, true, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case AsyncFunction:
                if (newTarget) {
                    return ConstructFunctionNodeGen.create(context, builtin, false, true, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructFunctionNodeGen.create(context, builtin, false, true, false, args().function().varArgs().createArgumentNodes(context));
            case AsyncGeneratorFunction:
                if (newTarget) {
                    return ConstructFunctionNodeGen.create(context, builtin, true, true, true, args().newTarget().varArgs().createArgumentNodes(context));
                }
                return ConstructFunctionNodeGen.create(context, builtin, true, true, false, args().function().varArgs().createArgumentNodes(context));

            case Symbol:
                return construct ? ConstructSymbolNodeGen.create(context, builtin, args().createArgumentNodes(context))
                                : CallSymbolNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case Proxy:
                if (construct) {
                    return newTarget ? ConstructJSProxyNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(3).createArgumentNodes(context))
                                    : ConstructJSProxyNodeGen.create(context, builtin, false, args().function().fixedArgs(3).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Promise:
                if (construct) {
                    return newTarget ? PromiseConstructorNodeGen.create(context, builtin, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : PromiseConstructorNodeGen.create(context, builtin, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case PlainTime:
                if (construct) {
                    return newTarget ? ConstructTemporalPlainTimeNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(6).createArgumentNodes(context))
                                    : ConstructTemporalPlainTimeNodeGen.create(context, builtin, false, args().function().fixedArgs(6).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainDate:
                if (construct) {
                    return newTarget ? ConstructTemporalPlainDateNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalPlainDateNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainDateTime:
                if (construct) {
                    return newTarget ? ConstructTemporalPlainDateTimeNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(10).createArgumentNodes(context))
                                    : ConstructTemporalPlainDateTimeNodeGen.create(context, builtin, false, args().function().fixedArgs(10).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Duration:
                if (construct) {
                    return newTarget ? ConstructTemporalDurationNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(10).createArgumentNodes(context))
                                    : ConstructTemporalDurationNodeGen.create(context, builtin, false, args().function().fixedArgs(10).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Calendar:
                if (construct) {
                    return newTarget ? ConstructTemporalCalendarNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructTemporalCalendarNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));

                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainYearMonth:
                if (construct) {
                    return newTarget ? ConstructTemporalPlainYearMonthNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalPlainYearMonthNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case PlainMonthDay:
                if (construct) {
                    return newTarget ? ConstructTemporalPlainMonthDayNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalPlainMonthDayNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case Instant:
                if (construct) {
                    return newTarget ? ConstructTemporalInstantNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalInstantNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case TimeZone:
                if (construct) {
                    return newTarget ? ConstructTemporalTimeZoneNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalTimeZoneNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }

            case ZonedDateTime:
                if (construct) {
                    return newTarget ? ConstructTemporalZonedDateTimeNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(4).createArgumentNodes(context))
                                    : ConstructTemporalZonedDateTimeNodeGen.create(context, builtin, false, args().function().fixedArgs(4).createArgumentNodes(context));
                } else {
                    return createCallRequiresNew(context, builtin);
                }
            case JSAdapter:
                return ConstructJSAdapterNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case JavaImporter:
                return ConstructJavaImporterNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case Global:
                return construct ? (newTarget
                                ? ConstructWebAssemblyGlobalNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructWebAssemblyGlobalNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Instance:
                return construct ? (newTarget
                                ? ConstructWebAssemblyInstanceNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructWebAssemblyInstanceNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Memory:
                return construct ? (newTarget
                                ? ConstructWebAssemblyMemoryNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                : ConstructWebAssemblyMemoryNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Module:
                return construct ? (newTarget
                                ? ConstructWebAssemblyModuleNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                : ConstructWebAssemblyModuleNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
            case Table:
                return construct ? (newTarget
                                ? ConstructWebAssemblyTableNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                : ConstructWebAssemblyTableNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context)))
                                : createCallRequiresNew(context, builtin);
        }
        return null;

    }

    private static CallRequiresNewNode createCallRequiresNew(JSContext context, JSBuiltin builtin) {
        return CallRequiresNewNodeGen.create(context, builtin, args().createArgumentNodes(context));
    }

    public abstract static class ConstructWithNewTargetNode extends JSBuiltinNode {
        protected final boolean isNewTargetCase;

        protected ConstructWithNewTargetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin);
            this.isNewTargetCase = isNewTargetCase;
        }

        protected JSRealm getRealmFromNewTarget(Object newTarget) {
            if (isNewTargetCase) {
                return JSRuntime.getFunctionRealm(newTarget, getRealm());
            }
            return getRealm();
        }

        protected abstract JSDynamicObject getIntrinsicDefaultProto(JSRealm realm);

        protected JSDynamicObject swapPrototype(JSDynamicObject resultObj, JSDynamicObject newTarget) {
            if (isNewTargetCase) {
                return setPrototypeFromNewTarget(resultObj, newTarget);
            }
            return resultObj;
        }

        protected JSDynamicObject setPrototypeFromNewTarget(JSDynamicObject resultObj, JSDynamicObject newTarget) {
            Object prototype = JSObject.get(newTarget, JSObject.PROTOTYPE);
            if (!JSRuntime.isObject(prototype)) {
                prototype = getIntrinsicDefaultProto(getRealmFromNewTarget(newTarget));
            }
            JSObject.setPrototype(resultObj, (JSDynamicObject) prototype);
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
            return args.length == 1 && JSRuntime.isNumber(args[0]);
        }

        protected static boolean isOneForeignArg(Object[] args) {
            return args.length == 1 && JSRuntime.isForeignObject(args[0]);
        }

        protected static boolean isOneIntegerArg(Object[] args) {
            return args.length == 1 && args[0] instanceof Integer && (int) args[0] >= 0;
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSDynamicObject constructArray0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSArray.createConstantEmptyArray(getContext(), getRealm(), arrayAllocationSite), newTarget);
        }

        @Specialization(guards = "isOneIntegerArg(args)")
        protected JSDynamicObject constructArrayWithIntLength(JSDynamicObject newTarget, Object[] args) {
            int length = (int) args[0];
            JSRealm realm = getRealm();
            if (JSConfig.TrackArrayAllocationSites && arrayAllocationSite != null && arrayAllocationSite.isTyped()) {
                ScriptArray initialType = arrayAllocationSite.getInitialArrayType();
                // help checker tool see this is always true, guarded by isTyped()
                if (initialType != null) {
                    return swapPrototype(JSArray.create(getContext(), realm, initialType, ((AbstractWritableArray) initialType).allocateArray(length), length), newTarget);
                }
            }
            return swapPrototype(JSArray.createConstantEmptyArray(getContext(), realm, arrayAllocationSite, length), newTarget);
        }

        @Specialization(guards = {"args.length == 1", "toArrayLengthNode.isTypeNumber(len)"}, replaces = "constructArrayWithIntLength")
        protected JSDynamicObject constructWithLength(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args,
                        @Cached @SuppressWarnings("unused") ToArrayLengthNode toArrayLengthNode,
                        @Cached("create(getContext())") ArrayCreateNode arrayCreateNode,
                        @Bind("toArrayLengthNode.executeLong(firstArg(args))") long len) {
            JSDynamicObject array = arrayCreateNode.execute(len);
            return swapPrototype(array, newTarget);
        }

        static Object firstArg(Object[] arguments) {
            return arguments[0];
        }

        @Specialization(guards = "isOneForeignArg(args)", limit = "InteropLibraryLimit")
        protected JSDynamicObject constructWithForeignArg(JSDynamicObject newTarget, Object[] args,
                        @CachedLibrary("firstArg(args)") InteropLibrary interop,
                        @Cached("create(getContext())") ArrayCreateNode arrayCreateNode,
                        @Cached("createBinaryProfile()") ConditionProfile isNumber,
                        @Cached("create()") BranchProfile rangeErrorProfile) {
            Object len = args[0];
            if (isNumber.profile(interop.isNumber(len))) {
                if (interop.fitsInLong(len)) {
                    try {
                        long length = interop.asLong(len);
                        if (JSRuntime.isArrayIndex(length)) {
                            JSDynamicObject array = arrayCreateNode.execute(length);
                            return swapPrototype(array, newTarget);
                        }
                    } catch (UnsupportedMessageException umex) {
                        rangeErrorProfile.enter();
                        throw Errors.createTypeErrorInteropException(len, umex, "asLong", this);
                    }
                }
                rangeErrorProfile.enter();
                throw Errors.createRangeErrorInvalidArrayLength();
            } else {
                return swapPrototype(JSArray.create(getContext(), getRealm(), ConstantObjectArray.createConstantObjectArray(), args, 1), newTarget);
            }
        }

        @Specialization(guards = {"!isOneNumberArg(args)", "!isOneForeignArg(args)"})
        protected JSDynamicObject constructArrayVarargs(JSDynamicObject newTarget, Object[] args,
                        @Cached("create()") BranchProfile isIntegerCase,
                        @Cached("create()") BranchProfile isDoubleCase,
                        @Cached("create()") BranchProfile isObjectCase,
                        @Cached("createBinaryProfile()") ConditionProfile isLengthZero) {
            JSRealm realm = getRealm();
            if (isLengthZero.profile(args == null || args.length == 0)) {
                return swapPrototype(JSArray.create(getContext(), realm, ScriptArray.createConstantEmptyArray(), args, 0), newTarget);
            } else {
                ArrayContentType type = ArrayLiteralNode.identifyPrimitiveContentType(args, false);
                if (type == ArrayContentType.Integer) {
                    isIntegerCase.enter();
                    return swapPrototype(JSArray.createZeroBasedIntArray(getContext(), realm, ArrayLiteralNode.createIntArray(args)), newTarget);
                } else if (type == ArrayContentType.Double) {
                    isDoubleCase.enter();
                    return swapPrototype(JSArray.createZeroBasedDoubleArray(getContext(), realm, ArrayLiteralNode.createDoubleArray(args)), newTarget);
                } else {
                    isObjectCase.enter();
                    return swapPrototype(JSArray.create(getContext(), realm, ConstantObjectArray.createConstantObjectArray(), args, args.length), newTarget);
                }
            }
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
        protected boolean callBoolean(Object value, @Cached("create()") JSToBooleanNode toBoolean) {
            return toBoolean.executeBoolean(value);
        }
    }

    public abstract static class ConstructBooleanNode extends ConstructWithNewTargetNode {
        public ConstructBooleanNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructBoolean(JSDynamicObject newTarget, Object value,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            return swapPrototype(JSBoolean.create(getContext(), getRealm(), toBoolean.executeBoolean(value)), newTarget);
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

        @Child private JSToPrimitiveNode toPrimitiveNode;
        @Child private JSToDoubleNode toDoubleNode;
        private final ConditionProfile stringOrNumberProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDateProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile gotFieldsProfile = ConditionProfile.createBinaryProfile();

        private Object toPrimitive(Object target) {
            if (toPrimitiveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPrimitiveNode = insert(JSToPrimitiveNode.createHintDefault());
            }
            return toPrimitiveNode.execute(target);
        }

        protected double toDouble(Object target) {
            if (toDoubleNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toDoubleNode = insert(JSToDoubleNode.create());
            }
            return toDoubleNode.executeDouble(target);
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSDynamicObject constructDateZero(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSDate.create(getContext(), getRealm(), now()), newTarget);
        }

        @Specialization(guards = {"args.length == 1"})
        protected JSDynamicObject constructDateOne(JSDynamicObject newTarget, Object[] args,
                        @Cached("createBinaryProfile()") ConditionProfile isSpecialCase,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            double dateValue = getDateValue(args[0], interop);
            return swapPrototype(JSDate.create(getContext(), getRealm(), timeClip(dateValue, isSpecialCase)), newTarget);
        }

        @Specialization(guards = {"args.length >= 2"})
        protected JSDynamicObject constructDateMult(JSDynamicObject newTarget, Object[] args) {
            double val = constructorImpl(args);
            return swapPrototype(JSDate.create(getContext(), getRealm(), val), newTarget);
        }

        // inlined JSDate.timeClip to use profiles
        private static double timeClip(double dateValue, ConditionProfile isSpecialCase) {
            if (isSpecialCase.profile(Double.isInfinite(dateValue) || Double.isNaN(dateValue) || Math.abs(dateValue) > JSDate.MAX_DATE)) {
                return Double.NaN;
            }
            return (long) dateValue;
        }

        @TruffleBoundary
        private double now() {
            return getRealm().currentTimeMillis();
        }

        @TruffleBoundary
        private double parseDate(TruffleString target) {
            Integer[] fields = getContext().getEvaluator().parseDate(getRealm(), Strings.toJavaString(Strings.lazyTrim(target)), false);
            if (gotFieldsProfile.profile(fields != null)) {
                return JSDate.makeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7]);
            }
            return Double.NaN;
        }

        private double getDateValue(Object arg0, InteropLibrary interop) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                if (isDateProfile.profile(JSDate.isJSDate(arg0))) {
                    return JSDate.getTimeMillisField((JSDateObject) arg0);
                } else if (interop.isInstant(arg0)) {
                    return JSDate.getDateValueFromInstant(arg0, interop);
                }
            }
            Object value = toPrimitive(arg0);
            if (stringOrNumberProfile.profile(Strings.isTString(value))) {
                return parseDate(JSRuntime.toStringIsString(value));
            } else {
                double dval = toDouble(value);
                if (Double.isInfinite(dval) || Double.isNaN(dval)) {
                    return Double.NaN;
                } else {
                    return dval;
                }
            }
        }

        private double constructorImpl(Object[] args) {
            double[] argsEvaluated = new double[args.length];
            boolean isNaN = false;
            for (int i = 0; i < args.length; i++) {
                double d = toDouble(args[i]);
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

    public abstract static class CallFetchResponseNode extends JSBuiltinNode {
        public CallFetchResponseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object callFetchResponse() {
            throw Errors.createTypeError("Class constructor Response cannot be invoked without 'new'");
        }
    }

    public abstract static class ConstructFetchResponseNode extends ConstructWithNewTargetNode {
        public ConstructFetchResponseNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        /**
         * The fetch response class constructor: https://fetch.spec.whatwg.org/#dom-response.
         * @param body A response body
         * @param init A optional ResponseInit object https://fetch.spec.whatwg.org/#responseinit
         * @return A {@linkplain JSFetchResponse} object
         */
        @Specialization
        protected JSDynamicObject constructFetchResponse(JSDynamicObject newTarget, Object body, Object init) {
            JSObject parsedOptions;
            if (init == Null.instance || init == Undefined.instance) {
                parsedOptions = JSOrdinary.create(getContext(), getRealm());
            } else {
                parsedOptions = (JSObject) init;
            }
            return swapPrototype(JSFetchResponse.create(getContext(), getRealm(), new FetchResponse(body, parsedOptions)), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getFetchResponsePrototype();
        }
    }

    public abstract static class CallFetchRequestNode extends JSBuiltinNode {
        public CallFetchRequestNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object callFetchRequest() {
            throw Errors.createTypeError("Class constructor Request cannot be invoked without 'new'");
        }
    }

    public abstract static class ConstructFetchRequestNode extends ConstructWithNewTargetNode {
        public ConstructFetchRequestNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        /**
         * The fetch request class constructor: https://fetch.spec.whatwg.org/#dom-request.
         * @param input A string or {@linkplain JSFetchRequest} object
         * @param options A optional RequestInit object https://fetch.spec.whatwg.org/#requestinit
         * @return A {@linkplain JSFetchRequest} object
         */
        @Specialization
        protected JSDynamicObject constructFetchRequest(JSDynamicObject newTarget, Object input, Object options, @Cached("create()") JSToStringNode toString) {
            JSObject parsedOptions;
            if (options == Null.instance || options == Undefined.instance) {
                parsedOptions = JSOrdinary.create(getContext(), getRealm());
            } else {
                parsedOptions = (JSObject) options;
            }

            // Par. 5.4, constructor step 6
            // requests can wrap requests
            if (JSFetchRequest.isJSFetchRequest(input) && input != Null.instance && input != Undefined.instance) {
                FetchRequest request = JSFetchRequest.getInternalData((JSObject) input);
                request.applyRequestInit(parsedOptions);
                return swapPrototype(JSFetchRequest.create(getContext(), getRealm(), request), newTarget);
            }

            TruffleString url = toString.executeString(input);
            return swapPrototype(JSFetchRequest.create(getContext(), getRealm(), new FetchRequest(url, parsedOptions)), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getFetchRequestPrototype();
        }
    }

    public abstract static class CallFetchHeadersNode extends JSBuiltinNode {
        public CallFetchHeadersNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object callFetchHeaders() {
            throw Errors.createTypeError("Class constructor Headers cannot be invoked without 'new'");
        }
    }

    public abstract static class ConstructFetchHeadersNode extends ConstructWithNewTargetNode {
        public ConstructFetchHeadersNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        /**
         * The fetch Headers class constructor https://fetch.spec.whatwg.org/#dom-headers.
         */
        @Specialization
        protected JSDynamicObject constructFetchHeaders(JSDynamicObject newTarget, Object init) {
            FetchHeaders headers = new FetchHeaders(init);
            return swapPrototype(JSFetchHeaders.create(getContext(), getRealm(), headers), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getFetchHeadersPrototype();
        }
    }

    public abstract static class ConstructTemporalPlainDateNode extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainDateNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainDate(JSDynamicObject newTarget, Object isoYear, Object isoMonth,
                        Object isoDay, Object calendarLike,
                        @Cached("create()") JSToIntegerThrowOnInfinityNode toIntegerNode,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode,
                        @Cached("create()") BranchProfile errorBranch) {
            final int y = toIntegerNode.executeIntOrThrow(isoYear);
            final int m = toIntegerNode.executeIntOrThrow(isoMonth);
            final int d = toIntegerNode.executeIntOrThrow(isoDay);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);
            return swapPrototype(JSTemporalPlainDate.create(getContext(), y, m, d, calendar, errorBranch), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainTimePrototype();
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
                        @Cached BranchProfile errorBranch,
                        @Cached("create()") JSToIntegerThrowOnInfinityNode toIntegerNode) {
            final int hour = toIntegerNode.executeIntOrThrow(hourObj);
            final int minute = toIntegerNode.executeIntOrThrow(minuteObj);
            final int second = toIntegerNode.executeIntOrThrow(secondObj);
            final int millisecond = toIntegerNode.executeIntOrThrow(millisecondObject);
            final int microsecond = toIntegerNode.executeIntOrThrow(microsecondObject);
            final int nanosecond = toIntegerNode.executeIntOrThrow(nanosecondObject);
            return swapPrototype(JSTemporalPlainTime.create(getContext(),
                            hour, minute, second, millisecond, microsecond, nanosecond, errorBranch), newTarget);
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
                        @Cached("create()") JSToIntegerThrowOnInfinityNode toIntegerNode,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode,
                        @Cached BranchProfile errorBranch) {
            final int year = toIntegerNode.executeIntOrThrow(yearObj);
            final int month = toIntegerNode.executeIntOrThrow(monthObj);
            final int day = toIntegerNode.executeIntOrThrow(dayObj);

            final int hour = toIntegerNode.executeIntOrThrow(hourObj);
            final int minute = toIntegerNode.executeIntOrThrow(minuteObj);
            final int second = toIntegerNode.executeIntOrThrow(secondObj);
            final int millisecond = toIntegerNode.executeIntOrThrow(millisecondObject);
            final int microsecond = toIntegerNode.executeIntOrThrow(microsecondObject);
            final int nanosecond = toIntegerNode.executeIntOrThrow(nanosecondObject);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);
            return swapPrototype(JSTemporalPlainDateTime.create(getContext(),
                            year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar, errorBranch), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalPlainTimePrototype();
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
                        @Cached("create()") JSToIntegerWithoutRoundingNode toIntegerNode,
                        @Cached BranchProfile errorBranch) {
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
            return swapPrototype(JSTemporalDuration.createTemporalDuration(getContext(),
                            years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, errorBranch), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalDurationPrototype();
        }
    }

    public abstract static class ConstructTemporalCalendar extends ConstructWithNewTargetNode {

        protected ConstructTemporalCalendar(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalCalendar(JSDynamicObject newTarget, Object arg,
                        @Cached BranchProfile errorBranch,
                        @Cached("create()") JSToStringNode toString) {
            final TruffleString id = toString.executeString(arg);
            return swapPrototype(JSTemporalCalendar.create(getContext(), getRealm(), id, errorBranch), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalCalendarPrototype();
        }
    }

    public abstract static class ConstructTemporalPlainYearMonth extends ConstructWithNewTargetNode {

        protected ConstructTemporalPlainYearMonth(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalPlainYearMonth(JSDynamicObject newTarget, Object isoYear,
                        Object isoMonth, Object calendarLike, Object refISODay,
                        @Cached("create()") BranchProfile errorBranch,
                        @Cached("create()") JSToIntegerThrowOnInfinityNode toInteger,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode) {

            Object referenceISODay = refISODay;
            if (referenceISODay == Undefined.instance || referenceISODay == null) {
                referenceISODay = 1;
            }
            int y = toInteger.executeIntOrThrow(isoYear);
            int m = toInteger.executeIntOrThrow(isoMonth);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);
            int ref = toInteger.executeIntOrThrow(referenceISODay);
            return swapPrototype(JSTemporalPlainYearMonth.create(getContext(), y, m, calendar, ref, errorBranch), newTarget);
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
                        @Cached("create()") BranchProfile errorBranch,
                        @Cached("create()") JSToIntegerThrowOnInfinityNode toInt,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode) {
            Object referenceISOYear = refISOYear;
            if (referenceISOYear == Undefined.instance || referenceISOYear == null) {
                referenceISOYear = 1972;
            }
            int m = toInt.executeIntOrThrow(isoMonth);
            int d = toInt.executeIntOrThrow(isoDay);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);
            int ref = toInt.executeIntOrThrow(referenceISOYear); // non-spec
            return swapPrototype(JSTemporalPlainMonthDay.create(getContext(), m, d, calendar, ref, errorBranch), newTarget);
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
                        @Cached BranchProfile errorBranch) {
            BigInt bi = JSRuntime.toBigInt(epochNanoseconds);
            if (!TemporalUtil.isValidEpochNanoseconds(bi)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            return swapPrototype(JSTemporalInstant.create(getContext(), getRealm(), bi), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalInstantPrototype();
        }
    }

    public abstract static class ConstructTemporalTimeZone extends ConstructWithNewTargetNode {

        protected ConstructTemporalTimeZone(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalTimeZone(JSDynamicObject newTarget, Object identifier,
                        @Cached("create()") JSToStringNode toStringNode) {
            TruffleString id = toStringNode.executeString(identifier);
            return constructTemporalTimeZoneIntl(newTarget, id);
        }

        @TruffleBoundary
        private JSDynamicObject constructTemporalTimeZoneIntl(JSDynamicObject newTarget, TruffleString idParam) {
            TruffleString id = idParam;
            boolean canParse = TemporalUtil.canParseAsTimeZoneNumericUTCOffset(id);
            if (!canParse) {
                if (!TemporalUtil.isValidTimeZoneName(id)) {
                    throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
                }
                id = TemporalUtil.canonicalizeTimeZoneName(id);
            }
            return swapPrototype(TemporalUtil.createTemporalTimeZone(getContext(), id), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getTemporalTimeZonePrototype();
        }
    }

    public abstract static class ConstructTemporalZonedDateTime extends ConstructWithNewTargetNode {

        protected ConstructTemporalZonedDateTime(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructTemporalZonedDateTime(JSDynamicObject newTarget, Object epochNanoseconds, Object timeZoneLike, Object calendarLike,
                        @Cached("create(getContext())") ToTemporalTimeZoneNode toTemporalTimeZone,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode,
                        @Cached("create()") JSToBigIntNode toBigIntNode,
                        @Cached BranchProfile errorBranch) {
            BigInt ns = toBigIntNode.executeBigInteger(epochNanoseconds);
            if (!TemporalUtil.isValidEpochNanoseconds(ns)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            JSDynamicObject timeZone = toTemporalTimeZone.executeDynamicObject(timeZoneLike);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);

            return swapPrototype(JSTemporalZonedDateTime.create(getContext(), getRealm(), ns, timeZone, calendar), newTarget);
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
        @Child private TRegexUtil.InteropReadStringMemberNode interopReadPatternNode;
        private final BranchProfile regexpObject = BranchProfile.create();
        private final BranchProfile regexpMatcherObject = BranchProfile.create();
        private final BranchProfile regexpNonObject = BranchProfile.create();
        private final BranchProfile regexpObjectNewFlagsBranch = BranchProfile.create();
        private final ConditionProfile callIsRegExpProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile constructorEquivalentProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected JSDynamicObject constructRegExp(JSDynamicObject newTarget, Object pattern, Object flags,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            boolean hasMatchSymbol = isRegExpNode.executeBoolean(pattern);
            if (isCall) {
                // we are in the "call" case, i.e. NewTarget is undefined (before)
                if (callIsRegExpProfile.profile(hasMatchSymbol && flags == Undefined.instance && JSDynamicObject.isJSDynamicObject(pattern))) {
                    JSDynamicObject patternObj = (JSDynamicObject) pattern;
                    Object patternConstructor = getConstructor(patternObj);
                    if (constructorEquivalentProfile.profile(patternConstructor == getRealm().getRegExpConstructor())) {
                        return patternObj;
                    }
                }
                return constructRegExpImpl(pattern, flags, hasMatchSymbol, true);
            } else {
                // we are in the "construct" case, i.e. NewTarget is NOT undefined
                return swapPrototype(constructRegExpImpl(pattern, flags, hasMatchSymbol, newTarget == getRealm().getRegExpConstructor()), newTarget);
            }

        }

        protected JSDynamicObject constructRegExpImpl(Object patternObj, Object flags, boolean hasMatchSymbol, boolean legacyFeaturesEnabled) {
            Object p;
            Object f;
            boolean isJSRegExp = JSRegExp.isJSRegExp(patternObj);
            if (isJSRegExp) {
                regexpObject.enter();
                Object compiledRegex = JSRegExp.getCompiledRegex((JSDynamicObject) patternObj);
                if (flags == Undefined.instance) {
                    return getCreateRegExpNode().createRegExp(compiledRegex);
                } else {
                    if (getContext().getEcmaScriptVersion() < 6) {
                        throw Errors.createTypeError("Cannot supply flags when constructing one RegExp from another");
                    }
                    Object flagsStr = flagsToString(flags);
                    regexpObjectNewFlagsBranch.enter();
                    Object newCompiledRegex = getCompileRegexNode().compile(getInteropReadPatternNode().execute(compiledRegex, TRegexUtil.Props.CompiledRegex.PATTERN), flagsStr);
                    return getCreateRegExpNode().createRegExp(newCompiledRegex);
                }
            } else if (hasMatchSymbol) {
                regexpMatcherObject.enter();
                JSDynamicObject patternJSObj = (JSDynamicObject) patternObj;
                p = getSource(patternJSObj);
                if (flags == Undefined.instance) {
                    f = getFlags(patternJSObj);
                } else {
                    f = flags;
                }
            } else {
                regexpNonObject.enter();
                p = patternObj;
                f = flags;
            }

            TruffleString patternStr = getPatternToStringNode().executeString(p);
            Object flagsStr = flagsToString(f);
            Object compiledRegex = getCompileRegexNode().compile(patternStr, flagsStr);
            JSDynamicObject regExp = getCreateRegExpNode().createRegExp(compiledRegex, legacyFeaturesEnabled);
            if (getContext().getContextOptions().isTestV8Mode()) {
                // workaround for the reference equality check at the end of mjsunit/regexp.js
                // TODO: remove this as soon as option maps are available for TRegex Sources
                JSObjectUtil.putDataProperty(getContext(), regExp, Strings.SOURCE, JSRegExp.escapeRegExpPattern(patternStr), JSAttributes.configurableNotEnumerableNotWritable());
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

        private TRegexUtil.InteropReadStringMemberNode getInteropReadPatternNode() {
            if (interopReadPatternNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopReadPatternNode = insert(TRegexUtil.InteropReadStringMemberNode.create());
            }
            return interopReadPatternNode;
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
                getConstructorNode = insert(PropertyGetNode.create(JSObject.CONSTRUCTOR, false, getContext()));
            }
            return getConstructorNode.getValue(obj);
        }

        private Object getSource(JSDynamicObject obj) {
            if (getSourceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSourceNode = insert(PropertyGetNode.create(JSRegExp.SOURCE, false, getContext()));
            }
            return getSourceNode.getValue(obj);
        }

        private Object getFlags(JSDynamicObject obj) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, false, getContext()));
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
        protected JSDynamicObject constructStringInt0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSString.create(getContext(), getRealm(), Strings.EMPTY_STRING), newTarget);
        }

        @Specialization(guards = {"args.length != 0"})
        protected JSDynamicObject constructString(JSDynamicObject newTarget, Object[] args,
                        @Cached("create()") JSToStringNode toStringNode) {
            return swapPrototype(JSString.create(getContext(), getRealm(), toStringNode.executeString(args[0])), newTarget);
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

        @Specialization(guards = {"isJSObject(target)"})
        protected JSDynamicObject constructWeakRef(JSDynamicObject newTarget, Object target) {
            return swapPrototype(JSWeakRef.create(getContext(), getRealm(), target), newTarget);
        }

        @Specialization(guards = {"!isJSObject(target)"})
        protected JSDynamicObject constructWeakRefNonObject(@SuppressWarnings("unused") JSDynamicObject newTarget, @SuppressWarnings("unused") Object target) {
            throw Errors.createTypeError("WeakRef: target must be an object");
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
        protected JSDynamicObject constructFinalizationRegistry(JSDynamicObject newTarget, Object cleanupCallback) {
            return swapPrototype(JSFinalizationRegistry.create(getContext(), getRealm(), cleanupCallback), newTarget);
        }

        @Specialization(guards = {"!isCallableNode.executeBoolean(cleanupCallback)"})
        protected JSDynamicObject constructFinalizationRegistryNonObject(@SuppressWarnings("unused") JSDynamicObject newTarget, @SuppressWarnings("unused") Object cleanupCallback) {
            throw Errors.createTypeError("FinalizationRegistry: cleanup must be callable");
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getFinalizationRegistryPrototype();
        }
    }

    public abstract static class CallCollatorNode extends JSBuiltinNode {

        @Child InitializeCollatorNode initializeCollatorNode;

        public CallCollatorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            initializeCollatorNode = InitializeCollatorNode.createInitalizeCollatorNode(context);
        }

        @Specialization
        protected JSDynamicObject callCollator(Object locales, Object options) {
            JSDynamicObject collator = JSCollator.create(getContext(), getRealm());
            return initializeCollatorNode.executeInit(collator, locales, options);
        }
    }

    public abstract static class ConstructCollatorNode extends ConstructWithNewTargetNode {

        @Child InitializeCollatorNode initializeCollatorNode;

        public ConstructCollatorNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeCollatorNode = InitializeCollatorNode.createInitalizeCollatorNode(context);
        }

        @Specialization
        protected JSDynamicObject constructCollator(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject collator = swapPrototype(JSCollator.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructListFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject listFormat = swapPrototype(JSListFormat.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructRelativeTimeFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject listFormat = swapPrototype(JSRelativeTimeFormat.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructSegmenter(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject segmenter = swapPrototype(JSSegmenter.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructDisplayNames(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject displayNames = swapPrototype(JSDisplayNames.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructLocale(JSDynamicObject newTarget, Object tag, Object options) {
            JSDynamicObject locale = swapPrototype(JSLocale.create(getContext(), getRealm()), newTarget);
            return initializeLocaleNode.executeInit(locale, tag, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getLocalePrototype();
        }
    }

    public abstract static class CallNumberFormatNode extends JSBuiltinNode {

        @Child InitializeNumberFormatNode initializeNumberFormatNode;

        public CallNumberFormatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            initializeNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @Specialization
        protected JSDynamicObject callNumberFormat(Object locales, Object options) {
            JSDynamicObject numberFormat = JSNumberFormat.create(getContext(), getRealm());
            return initializeNumberFormatNode.executeInit(numberFormat, locales, options);
        }
    }

    public abstract static class ConstructNumberFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeNumberFormatNode initializeNumberFormatNode;

        public ConstructNumberFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @Specialization
        protected JSDynamicObject constructNumberFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject numberFormat = swapPrototype(JSNumberFormat.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructPluralRules(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject pluralRules = swapPrototype(JSPluralRules.create(getContext(), getRealm()), newTarget);
            return initializePluralRulesNode.executeInit(pluralRules, locales, options);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getPluralRulesPrototype();
        }
    }

    public abstract static class CallDateTimeFormatNode extends JSBuiltinNode {

        @Child InitializeDateTimeFormatNode initializeDateTimeFormatNode;

        public CallDateTimeFormatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            initializeDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, "any", "date");
        }

        @Specialization
        protected JSDynamicObject callDateTimeFormat(Object locales, Object options) {
            JSDynamicObject dateTimeFormat = JSDateTimeFormat.create(getContext(), getRealm());
            return initializeDateTimeFormatNode.executeInit(dateTimeFormat, locales, options);
        }
    }

    public abstract static class ConstructDateTimeFormatNode extends ConstructWithNewTargetNode {

        @Child InitializeDateTimeFormatNode initializeDateTimeFormatNode;

        public ConstructDateTimeFormatNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            initializeDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, "any", "date");
        }

        @Specialization
        protected JSDynamicObject constructDateTimeFormat(JSDynamicObject newTarget, Object locales, Object options) {
            JSDynamicObject dateTimeFormat = swapPrototype(JSDateTimeFormat.create(getContext(), getRealm()), newTarget);
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
        protected JSDynamicObject constructObjectNewTarget(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"arguments.length == 0"})
        protected JSDynamicObject constructObject0(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"!isNewTargetCase", "arguments.length > 0", "!arg0NullOrUndefined(arguments)"}, limit = "InteropLibraryLimit")
        protected Object constructObjectJSObject(@SuppressWarnings("unused") JSDynamicObject newTarget, Object[] arguments,
                        @Cached("createToObject(getContext())") JSToObjectNode toObjectNode,
                        @CachedLibrary("firstArgument(arguments)") InteropLibrary interop,
                        @Cached("createBinaryProfile()") ConditionProfile isNull) {
            Object arg0 = arguments[0];
            if (isNull.profile(interop.isNull(arg0))) {
                return newObject(Null.instance);
            } else {
                return toObjectNode.execute(arg0);
            }
        }

        @Specialization(guards = {"arguments.length > 0", "arg0NullOrUndefined(arguments)"})
        protected JSDynamicObject constructObjectNullOrUndefined(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        private JSDynamicObject newObject(JSDynamicObject newTarget) {
            return swapPrototype(JSOrdinary.create(getContext(), getRealm()), newTarget);
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
                        @Cached("create()") JSToNumericNode toNumericNode,
                        @Cached("create()") JSNumericToNumberNode toNumberFromNumericNode) {
            return toNumberFromNumericNode.executeNumeric(toNumericNode.execute(args[0]));
        }
    }

    public abstract static class ConstructNumberNode extends ConstructWithNewTargetNode {
        public ConstructNumberNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = {"args.length == 0"})
        protected JSDynamicObject constructNumberZero(JSDynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSNumber.create(getContext(), getRealm(), 0), newTarget);
        }

        @Specialization(guards = {"args.length > 0"})
        protected JSDynamicObject constructNumber(JSDynamicObject newTarget, Object[] args,
                        @Cached("create()") JSToNumericNode toNumericNode,
                        @Cached("create()") JSNumericToNumberNode toNumberFromNumericNode) {
            return swapPrototype(JSNumber.create(getContext(), getRealm(), toNumberFromNumericNode.executeNumeric(toNumericNode.execute(args[0]))), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getNumberPrototype();
        }

    }

    public abstract static class CallBigIntNode extends JSBuiltinNode {

        @Child JSToPrimitiveNode toPrimitiveNode;

        public CallBigIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private Object toPrimitive(Object target) {
            if (toPrimitiveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPrimitiveNode = insert(JSToPrimitiveNode.createHintNumber());
            }
            return toPrimitiveNode.execute(target);
        }

        @Specialization(guards = {"args.length == 0"})
        protected void callBigIntZero(@SuppressWarnings("unused") Object[] args) {
            throw Errors.createErrorCanNotConvertToBigInt(JSErrorType.TypeError, Undefined.instance);
        }

        @Specialization(guards = {"args.length > 0"})
        protected Object callBigInt(Object[] args,
                        @Cached("create()") JSNumberToBigIntNode numberToBigIntNode,
                        @Cached("create()") JSToBigIntNode toBigIntNode) {
            Object value = args[0];
            Object primitiveObj = toPrimitive(value);
            if (JSRuntime.isNumber(primitiveObj)) {
                return numberToBigIntNode.executeBigInt(primitiveObj);
            } else {
                return toBigIntNode.executeBigInteger(primitiveObj);
            }
        }
    }

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
        protected final JSDynamicObject constructFunction(JSDynamicObject newTarget, Object[] args,
                        @Cached("createBinaryProfile()") ConditionProfile hasArgsProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasParamsProfile) {
            int argc = args.length;
            TruffleString[] params;
            TruffleString body;
            if (hasArgsProfile.profile(argc > 0)) {
                params = new TruffleString[argc - 1];
                for (int i = 0; i < argc - 1; i++) {
                    params[i] = toStringNode.executeString(args[i]);
                }
                body = toStringNode.executeString(args[argc - 1]);
            } else {
                params = new TruffleString[0];
                body = Strings.EMPTY_STRING;
            }
            TruffleString paramList = hasParamsProfile.profile(argc > 1) ? join(params) : Strings.EMPTY_STRING;
            return swapPrototype(functionNode.executeFunction(Strings.toJavaString(paramList), Strings.toJavaString(body), getSourceName()), newTarget);
        }

        @TruffleBoundary
        private static TruffleString join(TruffleString[] params) {
            assert params.length > 0;
            TruffleStringBuilder sb = Strings.builderCreate();
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

        private String getSourceName() {
            String sourceName = null;
            if (isCallerSensitive()) {
                sourceName = EvalNode.findAndFormatEvalOrigin(getRealm().getCallNode(), getContext());
            }
            if (sourceName == null) {
                sourceName = Evaluator.FUNCTION_SOURCE_NAME;
            }
            return sourceName;
        }

        @Override
        public boolean isCallerSensitive() {
            return getContext().isOptionV8CompatibilityMode();
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

        protected abstract JSDynamicObject executeFunction(String paramList, String body, String sourceName);

        protected static boolean equals(String a, String b) {
            return a.equals(b);
        }

        protected LRUCache<CachedSourceKey, ScriptNode> createCache() {
            return new LRUCache<>(context.getContextOptions().getFunctionConstructorCacheSize());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(cachedParamList, paramList)", "equals(cachedBody, body)", "equals(cachedSourceName, sourceName)"}, limit = "1")
        protected final JSDynamicObject doCached(String paramList, String body, String sourceName,
                        @Cached("paramList") String cachedParamList,
                        @Cached("body") String cachedBody,
                        @Cached("sourceName") String cachedSourceName,
                        @Cached("createAssumedValue()") AssumedValue<ScriptNode> cachedParsedFunction) {
            ScriptNode parsedFunction = cachedParsedFunction.get();
            if (parsedFunction == null) {
                parsedFunction = parseFunction(paramList, body, sourceName);
                cachedParsedFunction.set(parsedFunction);
            }

            return evalParsedFunction(getRealm(), parsedFunction);
        }

        @Specialization(replaces = "doCached")
        protected final JSDynamicObject doUncached(String paramList, String body, String sourceName,
                        @Cached("createCache()") LRUCache<CachedSourceKey, ScriptNode> cache,
                        @Cached("createCountingProfile()") ConditionProfile cacheHit) {
            ScriptNode cached = cacheLookup(cache, new CachedSourceKey(paramList, body, sourceName));
            JSRealm realm = getRealm();
            if (cacheHit.profile(cached == null)) {
                return parseAndEvalFunction(cache, realm, paramList, body, sourceName);
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
        protected final ScriptNode parseFunction(String paramList, String body, String sourceName) {
            CompilerAsserts.neverPartOfCompilation();
            return context.getEvaluator().parseFunction(context, paramList, body, generatorFunction, asyncFunction, sourceName);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static JSDynamicObject evalParsedFunction(JSRealm realm, ScriptNode parsedFunction) {
            return (JSDynamicObject) parsedFunction.run(realm);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private JSDynamicObject parseAndEvalFunction(LRUCache<CachedSourceKey, ScriptNode> cache, JSRealm realm, String paramList, String body, String sourceName) {
            ScriptNode parsedBody = parseFunction(paramList, body, sourceName);
            synchronized (cache) {
                cache.put(new CachedSourceKey(paramList, body, sourceName), parsedBody);
            }
            return evalParsedFunction(realm, parsedBody);
        }

        AssumedValue<ScriptNode> createAssumedValue() {
            return new AssumedValue<>("parsedFunction", null);
        }

        protected static class CachedSourceKey {
            private final String body;
            private final String paramList;
            private final String sourceName;

            CachedSourceKey(String paramList, String body, String sourceName) {
                this.body = body;
                this.paramList = paramList;
                this.sourceName = sourceName;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CachedSourceKey)) {
                    return false;
                }
                CachedSourceKey k = (CachedSourceKey) o;
                return k.body.equals(body) && k.paramList.equals(paramList) && k.sourceName.equals(sourceName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(body, paramList, sourceName);
            }
        }

    }

    /**
     * Implements ECMAScript 2015, 22.2.1.4 %TypedArray% (object).
     *
     */
    public abstract static class CallTypedArrayNode extends JSBuiltinNode {
        public CallTypedArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object callTypedArray(@SuppressWarnings("unused") Object... args) {
            throw Errors.createTypeError("wrong");
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ConstructArrayBufferNode extends ConstructWithNewTargetNode {
        private final boolean useShared;
        @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorNode;

        public ConstructArrayBufferNode(JSContext context, JSBuiltin builtin, boolean useShared, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.useShared = useShared;
            if (isNewTargetCase) {
                getPrototypeFromConstructorNode = GetPrototypeFromConstructorNode.create(context, null, realm -> getIntrinsicDefaultProto(realm));
            }
        }

        @Specialization(guards = {"!bufferInterop.hasBufferElements(length)"})
        protected JSDynamicObject constructFromLength(JSDynamicObject newTarget, Object length,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("bufferInterop") @SuppressWarnings("unused") InteropLibrary bufferInterop) {
            long byteLength = toIndexNode.executeLong(length);

            JSDynamicObject prototype = null;
            if (isNewTargetCase) {
                prototype = getPrototypeFromConstructorNode.executeWithConstructor(newTarget);
            }

            if (byteLength > getContext().getContextOptions().getMaxTypedArrayLength()) {
                errorBranch.enter();
                throw Errors.createRangeError("Array buffer allocation failed");
            }

            JSDynamicObject arrayBuffer;
            JSContext contextFromNewTarget = getContext();
            JSRealm realm = getRealm();
            if (useShared) {
                arrayBuffer = JSSharedArrayBuffer.createSharedArrayBuffer(contextFromNewTarget, realm, (int) byteLength);
            } else {
                if (getContext().isOptionDirectByteBuffer()) {
                    arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(contextFromNewTarget, realm, (int) byteLength);
                } else {
                    arrayBuffer = JSArrayBuffer.createArrayBuffer(contextFromNewTarget, realm, (int) byteLength);
                }
            }
            if (isNewTargetCase) {
                JSObject.setPrototype(arrayBuffer, prototype);
            }
            return arrayBuffer;
        }

        @Specialization(guards = {"bufferInterop.hasBufferElements(buffer)"})
        protected JSDynamicObject constructFromInteropBuffer(JSDynamicObject newTarget, Object buffer,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("bufferInterop") @SuppressWarnings("unused") InteropLibrary bufferInterop) {
            getBufferSizeSafe(buffer, bufferInterop, errorBranch);
            return swapPrototype(JSArrayBuffer.createInteropArrayBuffer(getContext(), getRealm(), buffer), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return useShared ? realm.getSharedArrayBufferPrototype() : realm.getArrayBufferPrototype();
        }

        static int getBufferSizeSafe(Object buffer, InteropLibrary bufferInterop, BranchProfile errorBranch) {
            try {
                long bufferSize = bufferInterop.getBufferSize(buffer);
                if (bufferSize < 0 || bufferSize > Integer.MAX_VALUE) {
                    errorBranch.enter();
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
            this.errorType = JSErrorType.valueOf(Strings.toJavaString(getBuiltin().getName()));
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create();
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
            assert errorType != JSErrorType.AggregateError;
        }

        @Specialization
        protected JSDynamicObject constructError(JSDynamicObject newTarget, TruffleString message, Object options) {
            return constructErrorImpl(newTarget, message, options);
        }

        @Specialization(guards = "!isString(message)")
        protected JSDynamicObject constructError(JSDynamicObject newTarget, Object message, Object options,
                        @Cached("create()") JSToStringNode toStringNode) {
            return constructErrorImpl(newTarget, message == Undefined.instance ? null : toStringNode.executeString(message), options);
        }

        private JSDynamicObject constructErrorImpl(JSDynamicObject newTarget, TruffleString messageOpt, Object options) {
            JSRealm realm = getRealm();
            JSErrorObject errorObj = JSError.createErrorObject(getContext(), realm, errorType);
            swapPrototype(errorObj, newTarget);

            int stackTraceLimit = stackTraceLimitNode.executeInt();
            JSDynamicObject errorFunction = realm.getErrorConstructor(errorType);

            // We skip until newTarget (if any) so as to also skip user-defined Error constructors.
            JSDynamicObject skipUntil = newTarget == Undefined.instance ? errorFunction : newTarget;

            GraalJSException exception = JSException.createCapture(errorType, Strings.toJavaString(messageOpt), errorObj, realm, stackTraceLimit, skipUntil, skipUntil != errorFunction);
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

        GetMethodNode createGetIteratorMethod() {
            return GetMethodNode.create(getContext(), Symbol.SYMBOL_ITERATOR);
        }

        @Specialization
        protected JSDynamicObject constructError(JSDynamicObject newTarget, Object errorsObj, Object messageObj, Object options,
                        @Cached JSToStringNode toStringNode,
                        @Cached("createGetIteratorMethod()") GetMethodNode getIteratorMethodNode,
                        @Cached("createCall()") JSFunctionCallNode iteratorCallNode,
                        @Cached IsJSObjectNode isObjectNode,
                        @Cached IterableToListNode iterableToListNode,
                        @Cached("create(NEXT, getContext())") PropertyGetNode getNextMethodNode) {
            JSContext context = getContext();
            JSRealm realm = getRealm();
            JSErrorObject errorObj = JSError.createErrorObject(context, realm, JSErrorType.AggregateError);
            swapPrototype(errorObj, newTarget);

            TruffleString message;
            if (messageObj == Undefined.instance) {
                message = null;
            } else {
                message = toStringNode.executeString(messageObj);
                setMessage.putWithFlags(errorObj, JSError.MESSAGE, message, JSError.MESSAGE_ATTRIBUTES);
            }

            if (context.getContextOptions().isErrorCauseEnabled() && options != Undefined.instance) {
                installErrorCause(errorObj, options);
            }

            Object usingIterator = getIteratorMethodNode.executeWithTarget(errorsObj);
            SimpleArrayList<Object> errors = iterableToListNode.execute(GetIteratorNode.getIterator(errorsObj, usingIterator, iteratorCallNode, isObjectNode, getNextMethodNode, this));
            JSDynamicObject errorsArray = JSArray.createConstantObjectArray(context, getRealm(), errors.toArray());

            int stackTraceLimit = stackTraceLimitNode.executeInt();
            JSDynamicObject errorFunction = realm.getErrorConstructor(JSErrorType.AggregateError);

            // We skip until newTarget (if any) so as to also skip user-defined Error constructors.
            JSDynamicObject skipUntil = newTarget == Undefined.instance ? errorFunction : newTarget;

            GraalJSException exception = JSException.createCapture(JSErrorType.AggregateError, Strings.toJavaString(message), errorObj, realm, stackTraceLimit, skipUntil, skipUntil != errorFunction);
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

    public abstract static class ConstructFetchErrorNode extends ConstructWithNewTargetNode {
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;
        @Child private DynamicObjectLibrary setMessage;

        public ConstructFetchErrorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create();
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
            this.setMessage = JSObjectUtil.createDispatched(JSError.ERRORS_TYPE);
        }

        @Specialization
        protected JSDynamicObject constructError(JSDynamicObject newTarget, TruffleString message, TruffleString type, Object sysErr) {
            return constructErrorImpl(newTarget, message, type, sysErr);
        }

        @Specialization(guards = "!isString(message)")
        protected JSDynamicObject constructError(JSDynamicObject newTarget, Object message, TruffleString type, Object sysErr,
                                                 @Cached("create()") JSToStringNode toStringNode) {
            return constructErrorImpl(newTarget, message == Undefined.instance ? null : toStringNode.executeString(message), type, sysErr);
        }

        private JSDynamicObject constructErrorImpl(JSDynamicObject newTarget, TruffleString messageOpt, TruffleString type, Object sysErr) {
            JSRealm realm = getRealm();
            JSErrorObject errorObj = JSError.createErrorObject(getContext(), realm, JSErrorType.FetchError);
            swapPrototype(errorObj, newTarget);

            setMessage.putWithFlags(errorObj, JSError.ERRORS_TYPE, type, JSError.MESSAGE_ATTRIBUTES);

            int stackTraceLimit = stackTraceLimitNode.executeInt();
            JSDynamicObject errorFunction = realm.getErrorConstructor(JSErrorType.FetchError);

            // We skip until newTarget (if any) so as to also skip user-defined Error constructors.
            JSDynamicObject skipUntil = newTarget == Undefined.instance ? errorFunction : newTarget;

            GraalJSException exception = JSException.createCapture(JSErrorType.FetchError, Strings.toJavaString(messageOpt), errorObj, realm, stackTraceLimit, skipUntil, skipUntil != errorFunction);
            return initErrorObjectNode.execute(errorObj, exception, messageOpt, null, sysErr);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorPrototype(JSErrorType.FetchError);
        }

        @Override
        public boolean countsTowardsStackTraceLimit() {
            return false;
        }
    }

    @ImportStatic({JSArrayBuffer.class, JSConfig.class})
    public abstract static class ConstructDataViewNode extends ConstructWithNewTargetNode {
        public ConstructDataViewNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization(guards = {"isJSHeapArrayBuffer(buffer)"})
        protected final JSDynamicObject ofHeapArrayBuffer(JSDynamicObject newTarget, JSDynamicObject buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") @Shared("byteLengthCondition") ConditionProfile byteLengthCondition,
                        @Cached @Shared("offsetToIndexNode") JSToIndexNode offsetToIndexNode,
                        @Cached @Shared("lengthToIndexNode") JSToIndexNode lengthToIndexNode) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, false, false, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, null);
        }

        @Specialization(guards = {"isJSDirectOrSharedArrayBuffer(buffer)"})
        protected final JSDynamicObject ofDirectArrayBuffer(JSDynamicObject newTarget, JSDynamicObject buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") @Shared("byteLengthCondition") ConditionProfile byteLengthCondition,
                        @Cached @Shared("offsetToIndexNode") JSToIndexNode offsetToIndexNode,
                        @Cached @Shared("lengthToIndexNode") JSToIndexNode lengthToIndexNode) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, true, false, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, null);
        }

        @Specialization(guards = {"isJSInteropArrayBuffer(buffer)"})
        protected final JSDynamicObject ofInteropArrayBuffer(JSDynamicObject newTarget, JSDynamicObject buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") @Shared("byteLengthCondition") ConditionProfile byteLengthCondition,
                        @Cached @Shared("offsetToIndexNode") JSToIndexNode offsetToIndexNode,
                        @Cached @Shared("lengthToIndexNode") JSToIndexNode lengthToIndexNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("bufferInterop") InteropLibrary bufferInterop) {
            return constructDataView(newTarget, buffer, byteOffset, byteLength, false, true, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, bufferInterop);
        }

        @Specialization(guards = {"!isJSAbstractBuffer(buffer)", "bufferInterop.hasBufferElements(buffer)"})
        protected final JSDynamicObject ofInteropBuffer(JSDynamicObject newTarget, Object buffer, Object byteOffset, Object byteLength,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") @Shared("byteLengthCondition") ConditionProfile byteLengthCondition,
                        @Cached @Shared("offsetToIndexNode") JSToIndexNode offsetToIndexNode,
                        @Cached @Shared("lengthToIndexNode") JSToIndexNode lengthToIndexNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("bufferInterop") InteropLibrary bufferInterop) {
            JSDynamicObject arrayBuffer = JSArrayBuffer.createInteropArrayBuffer(getContext(), getRealm(), buffer);
            return ofInteropArrayBuffer(newTarget, arrayBuffer, byteOffset, byteLength, errorBranch, byteLengthCondition, offsetToIndexNode, lengthToIndexNode, bufferInterop);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSAbstractBuffer(buffer)", "!bufferInterop.hasBufferElements(buffer)"})
        protected static JSDynamicObject error(JSDynamicObject newTarget, Object buffer, Object byteOffset, Object byteLength,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("bufferInterop") InteropLibrary bufferInterop) {
            throw Errors.createTypeError("Not an ArrayBuffer");
        }

        protected final JSDynamicObject constructDataView(JSDynamicObject newTarget, JSDynamicObject arrayBuffer, Object byteOffset, Object byteLength,
                        boolean direct, boolean isInteropBuffer,
                        BranchProfile errorBranch,
                        ConditionProfile byteLengthCondition,
                        JSToIndexNode offsetToIndexNode,
                        JSToIndexNode lengthToIndexNode,
                        InteropLibrary bufferInterop) {
            long offset = offsetToIndexNode.executeLong(byteOffset);

            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeError("detached buffer cannot be used");
            }

            int bufferByteLength;
            if (isInteropBuffer) {
                bufferByteLength = ConstructArrayBufferNode.getBufferSizeSafe(JSArrayBuffer.getInteropBuffer(arrayBuffer), bufferInterop, errorBranch);
            } else if (direct) {
                bufferByteLength = JSArrayBuffer.getDirectByteLength(arrayBuffer);
            } else {
                bufferByteLength = JSArrayBuffer.getHeapByteLength(arrayBuffer);
            }
            if (offset > bufferByteLength) {
                errorBranch.enter();
                throw Errors.createRangeError("offset > bufferByteLength");
            }

            final long viewByteLength;
            if (byteLengthCondition.profile(byteLength != Undefined.instance)) {
                viewByteLength = lengthToIndexNode.executeLong(byteLength);
                if (viewByteLength < 0) {
                    errorBranch.enter();
                    throw Errors.createRangeError("viewByteLength < 0");
                }
                if (offset + viewByteLength > bufferByteLength) {
                    errorBranch.enter();
                    throw Errors.createRangeError("offset + viewByteLength > bufferByteLength");
                }
            } else {
                viewByteLength = bufferByteLength - offset;
            }
            assert offset >= 0 && offset <= Integer.MAX_VALUE && viewByteLength >= 0 && viewByteLength <= Integer.MAX_VALUE;
            JSDynamicObject result = swapPrototype(JSDataView.createDataView(getContext(), getRealm(), arrayBuffer, (int) offset, (int) viewByteLength), newTarget);
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            return result;
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

        @Specialization
        protected final JSDynamicObject call() {
            throw Errors.createTypeErrorFormat("Constructor %s requires 'new'", getBuiltin().getName());
        }
    }

    public abstract static class ConstructJSAdapterNode extends JSBuiltinNode {
        public ConstructJSAdapterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSObject(adaptee)", "isUndefined(undefined1)", "isUndefined(undefined2)"})
        protected JSDynamicObject constructJSAdapter(JSDynamicObject adaptee, @SuppressWarnings("unused") Object undefined1, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, null, null);
        }

        @Specialization(guards = {"isJSObject(overrides)", "isJSObject(adaptee)", "isUndefined(undefined2)"})
        protected JSDynamicObject constructJSAdapter(JSDynamicObject overrides, JSDynamicObject adaptee, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, overrides, null);
        }

        @Specialization(guards = {"isJSObject(proto)", "isJSObject(overrides)", "isJSObject(adaptee)"})
        protected JSDynamicObject constructJSAdapter(JSDynamicObject proto, JSDynamicObject overrides, JSDynamicObject adaptee) {
            return JSAdapter.create(getContext(), getRealm(), adaptee, overrides, proto);
        }

        @Fallback
        protected JSDynamicObject constructJSAdapter(Object proto, Object overrides, Object adaptee) {
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
        private final ConditionProfile targetNonObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile handlerNonObject = ConditionProfile.createBinaryProfile();

        public ConstructJSProxyNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected JSDynamicObject constructJSProxy(JSDynamicObject newTarget, Object target, Object handler) {
            if (targetNonObject.profile(!JSGuards.isTruffleObject(target) || target instanceof Symbol || target == Undefined.instance || target == Null.instance ||
                            target instanceof TruffleString || target instanceof SafeInteger || target instanceof BigInt)) {
                throw Errors.createTypeError("target expected to be an object");
            }
            if (handlerNonObject.profile(!JSGuards.isJSObject(handler))) {
                throw Errors.createTypeError("handler expected to be an object");
            }
            JSDynamicObject handlerObj = (JSDynamicObject) handler;
            return swapPrototype(JSProxy.create(getContext(), getRealm(), target, handlerObj), newTarget);
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
        @Child private GetIteratorBaseNode getIteratorNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private JSFunctionCallNode callAdderNode;
        @Child private PropertyGetNode getAdderFnNode;
        protected final BranchProfile errorBranch = BranchProfile.create();

        protected void iteratorCloseAbrupt(JSDynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected IteratorRecord getIterator(Object iterator) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorBaseNode.create());
            }
            return getIteratorNode.execute(iterator);
        }

        protected Object getIteratorValue(JSDynamicObject iteratorResult) {
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
        protected JSDynamicObject constructEmptyMap(JSDynamicObject newTarget, @SuppressWarnings("unused") Object iterable) {
            JSDynamicObject mapObj = newMapObject();
            swapPrototype(mapObj, newTarget);
            return mapObj;
        }

        @Specialization(guards = "!isNullOrUndefined(iterable)")
        protected JSDynamicObject constructMapFromIterable(JSDynamicObject newTarget, Object iterable,
                        @Cached("create(getContext())") ReadElementNode readElementNode,
                        @Cached IsObjectNode isObjectNode,
                        @Cached IsCallableNode isCallableNode) {
            JSDynamicObject mapObj = newMapObject();
            swapPrototype(mapObj, newTarget);

            Object adder = getAdderFn(mapObj, Strings.SET);
            if (!isCallableNode.executeBoolean(adder)) {
                errorBranch.enter();
                throw Errors.createTypeError("function set not callable");
            }
            IteratorRecord iter = getIterator(iterable);

            try {
                while (true) {
                    Object next = iteratorStep(iter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextItem = getIteratorValue((JSDynamicObject) next);
                    if (!isObjectNode.executeBoolean(nextItem)) {
                        errorBranch.enter();
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

        protected JSDynamicObject newMapObject() {
            return JSMap.create(getContext(), getRealm());
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
        protected JSDynamicObject constructEmptySet(JSDynamicObject newTarget, @SuppressWarnings("unused") Object iterable) {
            JSDynamicObject setObj = newSetObject();
            swapPrototype(setObj, newTarget);
            return setObj;
        }

        @Specialization(guards = "!isNullOrUndefined(iterable)")
        protected JSDynamicObject constructSetFromIterable(JSDynamicObject newTarget, Object iterable,
                        @Cached IsCallableNode isCallableNode) {
            JSDynamicObject setObj = newSetObject();
            swapPrototype(setObj, newTarget);

            Object adder = getAdderFn(setObj, Strings.ADD);
            if (!isCallableNode.executeBoolean(adder)) {
                errorBranch.enter();
                throw Errors.createTypeError("function add not callable");
            }
            IteratorRecord iter = getIterator(iterable);

            try {
                while (true) {
                    Object next = iteratorStep(iter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextValue = getIteratorValue((JSDynamicObject) next);
                    call(setObj, adder, nextValue);
                }
            } catch (AbstractTruffleException ex) {
                iteratorCloseAbrupt(iter.getIterator());
                throw ex;
            }

            return setObj;
        }

        protected JSDynamicObject newSetObject() {
            return JSSet.create(getContext(), getRealm());
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSetPrototype();
        }

    }

    public abstract static class ConstructWeakSetNode extends ConstructSetNode {
        public ConstructWeakSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Override
        protected JSDynamicObject newSetObject() {
            return JSWeakSet.create(getContext(), getRealm());
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
        protected JSDynamicObject newMapObject() {
            return JSWeakMap.create(getContext(), getRealm());
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

            @Specialization(guards = {"acceptCache(equalNode, value, cachedValue, symbolUsageMarker)"})
            protected Symbol callSymbolSingleton(TruffleString value,
                            @Cached("value") TruffleString cachedValue,
                            @Cached TruffleString.EqualNode equalNode,
                            @Cached("createSymbolUsageMarker()") AtomicReference<Object> symbolUsageMarker,
                            @Cached("createCachedSingletonSymbol(value)") Symbol cachedSymbol) {
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
        protected static final JSDynamicObject construct() {
            throw Errors.createTypeError("cannot construct a Symbol");
        }
    }

    public abstract static class PromiseConstructorNode extends JSBuiltinNode {
        @Child protected IsCallableNode isCallable;
        @Child private PromiseResolveThenableNode promiseResolveThenable;
        @Child private OrdinaryCreateFromConstructorNode createPromiseFromConstructor;
        @Child private PropertySetNode setPromiseFulfillReactions;
        @Child private PropertySetNode setPromiseRejectReactions;
        @Child private PropertySetNode setPromiseIsHandled;

        public PromiseConstructorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isCallable = IsCallableNode.create();
            this.promiseResolveThenable = PromiseResolveThenableNode.create(context);
            this.createPromiseFromConstructor = OrdinaryCreateFromConstructorNode.create(context, null, JSRealm::getPromisePrototype, JSPromise.INSTANCE);
            this.setPromiseFulfillReactions = PropertySetNode.createSetHidden(JSPromise.PROMISE_FULFILL_REACTIONS, context);
            this.setPromiseRejectReactions = PropertySetNode.createSetHidden(JSPromise.PROMISE_REJECT_REACTIONS, context);
            this.setPromiseIsHandled = PropertySetNode.createSetHidden(JSPromise.PROMISE_IS_HANDLED, context);
        }

        @Specialization(guards = "isCallable.executeBoolean(executor)")
        protected JSDynamicObject construct(JSDynamicObject newTarget, Object executor) {
            JSDynamicObject promise = createPromiseFromConstructor.executeWithConstructor(newTarget);
            JSPromise.setPromiseState(promise, JSPromise.PENDING);
            setPromiseFulfillReactions.setValue(promise, new SimpleArrayList<>());
            setPromiseRejectReactions.setValue(promise, new SimpleArrayList<>());
            setPromiseIsHandled.setValueBoolean(promise, false);

            getContext().notifyPromiseHook(PromiseHook.TYPE_INIT, promise);

            promiseResolveThenable.execute(promise, Undefined.instance, executor);
            return promise;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable.executeBoolean(executor)")
        protected JSDynamicObject notCallable(JSDynamicObject newTarget, Object executor) {
            throw Errors.createTypeError("cannot create promise: executor not callable");
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
        protected JSDynamicObject constructModule(JSDynamicObject newTarget, Object bytes) {
            Object byteSource = exportByteSourceNode.execute(bytes);
            JSRealm realm = getRealm();
            Object wasmModule;
            try {
                Object decode = realm.getWASMModuleDecode();
                wasmModule = decodeModuleLib.execute(decode, byteSource);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
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
            return swapPrototype(JSWebAssemblyModule.create(getContext(), realm, wasmModule), newTarget);
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
        protected JSDynamicObject constructInstanceFromModule(JSDynamicObject newTarget, JSWebAssemblyModuleObject module, Object importObject) {
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
            return swapPrototype(JSWebAssemblyInstance.create(getContext(), realm, wasmInstance, wasmModule), newTarget);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWebAssemblyModule(other)")
        protected JSDynamicObject constructInstanceFromOther(JSDynamicObject newTarget, Object other, Object importObject) {
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
        @Child ToWebAssemblyIndexOrSizeNode toInitialSizeNode;
        @Child ToWebAssemblyIndexOrSizeNode toMaximumSizeNode;
        @Child InteropLibrary memAllocLib;

        public ConstructWebAssemblyMemoryNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.getInitialNode = PropertyGetNode.create(Strings.INITIAL, context);
            this.getMaximumNode = PropertyGetNode.create(Strings.MAXIMUM, context);
            this.toInitialSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Memory(): Property 'initial'");
            this.toMaximumSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Memory(): Property 'maximum'");
            this.memAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSDynamicObject constructMemory(JSDynamicObject newTarget, Object descriptor) {
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
            int maximumInt;
            Object maximum = getMaximumNode.getValue(descriptor);
            if (maximum == Undefined.instance) {
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
                wasmMemory = memAllocLib.execute(createMemory, initialInt, maximumInt);
            } catch (AbstractTruffleException tex) {
                throw Errors.createRangeError("WebAssembly.Memory(): could not allocate memory");
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            return swapPrototype(JSWebAssemblyMemory.create(getContext(), realm, wasmMemory), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyMemoryPrototype();
        }

    }

    public abstract static class ConstructWebAssemblyTableNode extends ConstructWithNewTargetNode {

        @Child IsObjectNode isObjectNode;
        @Child PropertyGetNode getElementNode;
        @Child PropertyGetNode getInitialNode;
        @Child PropertyGetNode getMaximumNode;
        @Child JSToStringNode toStringNode;
        @Child ToWebAssemblyIndexOrSizeNode toInitialSizeNode;
        @Child ToWebAssemblyIndexOrSizeNode toMaximumSizeNode;
        @Child InteropLibrary tableAllocLib;

        public ConstructWebAssemblyTableNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.getElementNode = PropertyGetNode.create(Strings.ELEMENT, context);
            this.getInitialNode = PropertyGetNode.create(Strings.INITIAL, context);
            this.getMaximumNode = PropertyGetNode.create(Strings.MAXIMUM, context);
            this.toStringNode = JSToStringNode.create();
            this.toInitialSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table(): Property 'initial'");
            this.toMaximumSizeNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table(): Property 'maximum'");
            this.tableAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSDynamicObject constructTable(JSDynamicObject newTarget, Object descriptor,
                        @Cached TruffleString.EqualNode stringEqualsNode) {
            if (!isObjectNode.executeBoolean(descriptor)) {
                throw Errors.createTypeError("WebAssembly.Table(): Argument 0 must be a table descriptor", this);
            }
            TruffleString element = toStringNode.executeString(getElementNode.getValue(descriptor));
            if (!Strings.equals(stringEqualsNode, Strings.ANYFUNC, element)) {
                throw Errors.createTypeError("WebAssembly.Table(): Descriptor property 'element' must be 'anyfunc'", this);
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
            JSRealm realm = getRealm();
            Object wasmTable;
            try {
                Object createTable = realm.getWASMTableAlloc();
                wasmTable = tableAllocLib.execute(createTable, initialInt, maximumInt);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            return swapPrototype(JSWebAssemblyTable.create(getContext(), realm, wasmTable), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyTablePrototype();
        }

    }

    public abstract static class ConstructWebAssemblyGlobalNode extends ConstructWithNewTargetNode {
        @Child IsObjectNode isObjectNode;
        @Child JSToStringNode toStringNode;
        @Child JSToBooleanNode toBooleanNode;
        @Child PropertyGetNode getValueNode;
        @Child PropertyGetNode getMutableNode;
        @Child ToWebAssemblyValueNode toWebAssemblyValueNode;
        @Child InteropLibrary globalAllocLib;

        public ConstructWebAssemblyGlobalNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
            this.isObjectNode = IsObjectNode.create();
            this.toStringNode = JSToStringNode.create();
            this.toBooleanNode = JSToBooleanNode.create();
            this.getValueNode = PropertyGetNode.create(Strings.VALUE, context);
            this.getMutableNode = PropertyGetNode.create(Strings.MUTABLE, context);
            this.toWebAssemblyValueNode = ToWebAssemblyValueNode.create();
            this.globalAllocLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected JSDynamicObject constructGlobal(JSDynamicObject newTarget, Object descriptor, Object value) {
            if (!isObjectNode.executeBoolean(descriptor)) {
                throw Errors.createTypeError("WebAssembly.Global(): Argument 0 must be a global descriptor", this);
            }
            boolean mutable = toBooleanNode.executeBoolean(getMutableNode.getValue(descriptor));
            TruffleString valueType = toStringNode.executeString(getValueNode.getValue(descriptor));
            if (!JSWebAssemblyValueTypes.isValueType(valueType)) {
                throw Errors.createTypeError("WebAssembly.Global(): Descriptor property 'value' must be a WebAssembly type (i32, i64, f32, f64)", this);
            }
            Object webAssemblyValue;
            if (value == Undefined.instance) {
                webAssemblyValue = 0;
            } else {
                if (!getContext().getContextOptions().isWasmBigInt() && JSWebAssemblyValueTypes.isI64(valueType)) {
                    throw Errors.createTypeError("WebAssembly.Global(): Can't set the value of i64 WebAssembly.Global", this);
                }
                webAssemblyValue = toWebAssemblyValueNode.execute(value, valueType);
            }
            JSRealm realm = getRealm();
            Object wasmGlobal;
            try {
                Object createGlobal = realm.getWASMGlobalAlloc();
                wasmGlobal = globalAllocLib.execute(createGlobal, valueType, mutable, webAssemblyValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            return swapPrototype(JSWebAssemblyGlobal.create(getContext(), realm, wasmGlobal, valueType, mutable), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWebAssemblyGlobalPrototype();
        }

    }

}
