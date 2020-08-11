/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.WeakHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallCollatorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallDateTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallNumberFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallNumberNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallRequiresNewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CallTypedArrayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructAggregateErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayBufferNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayNodeGen;
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
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakRefNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CreateDynamicFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.PromiseConstructorNodeGen;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode.ArrayContentType;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeFromConstructorNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IsRegExpNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.ArrayCreateNode;
import com.oracle.truffle.js.nodes.cast.ToArrayLengthNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSNumericToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
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
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
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
import com.oracle.truffle.js.runtime.builtins.JSCollator;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.JSDisplayNames;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSListFormat;
import com.oracle.truffle.js.runtime.builtins.JSLocale;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRelativeTimeFormat;
import com.oracle.truffle.js.runtime.builtins.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.builtins.JSWeakRef;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.WeakMap;

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
        ReferenceError(1),
        SyntaxError(1),
        EvalError(1),
        URIError(1),
        AggregateError(2),

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
            return EnumSet.range(Array, AsyncGeneratorFunction).contains(this);
        }

        @Override
        public int getECMAScriptVersion() {
            if (AsyncGeneratorFunction == this) {
                return JSConfig.ECMAScript2018;
            } else if (EnumSet.of(SharedArrayBuffer, AsyncFunction).contains(this)) {
                return JSConfig.ECMAScript2017;
            } else if (EnumSet.range(Map, Symbol).contains(this)) {
                return 6;
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
                if (newTarget) {
                    return ConstructErrorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context));
                }
                return ConstructErrorNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
            case AggregateError:
                if (newTarget) {
                    return ConstructAggregateErrorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context));
                }
                return ConstructAggregateErrorNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context));

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

            case JSAdapter:
                return ConstructJSAdapterNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case JavaImporter:
                return ConstructJavaImporterNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
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
                return JSRuntime.getFunctionRealm(newTarget, getContext());
            }
            return getContext().getRealm();
        }

        protected abstract DynamicObject getIntrinsicDefaultProto(JSRealm realm);

        protected DynamicObject swapPrototype(DynamicObject resultObj, DynamicObject newTarget) {
            if (isNewTargetCase) {
                return setPrototypeFromNewTarget(resultObj, newTarget);
            }
            return resultObj;
        }

        protected DynamicObject setPrototypeFromNewTarget(DynamicObject resultObj, DynamicObject newTarget) {
            Object prototype = JSObject.get(newTarget, JSObject.PROTOTYPE);
            if (!JSRuntime.isObject(prototype)) {
                prototype = getIntrinsicDefaultProto(getRealmFromNewTarget(newTarget));
            }
            JSObject.setPrototype(resultObj, (DynamicObject) prototype);
            return resultObj;
        }
    }

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
        protected DynamicObject constructArray0(DynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSArray.createConstantEmptyArray(getContext(), arrayAllocationSite), newTarget);
        }

        @Specialization(guards = "isOneIntegerArg(args)")
        protected DynamicObject constructArrayWithIntLength(DynamicObject newTarget, Object[] args) {
            int length = (int) args[0];
            if (JSConfig.TrackArrayAllocationSites && arrayAllocationSite != null && arrayAllocationSite.isTyped()) {
                ScriptArray initialType = arrayAllocationSite.getInitialArrayType();
                // help checker tool see this is always true, guarded by isTyped()
                if (initialType != null) {
                    return swapPrototype(JSArray.create(getContext(), initialType, ((AbstractWritableArray) initialType).allocateArray(length), length), newTarget);
                }
            }
            return swapPrototype(JSArray.createConstantEmptyArray(getContext(), arrayAllocationSite, length), newTarget);
        }

        @Specialization(guards = {"args.length == 1", "toArrayLengthNode.isTypeNumber(len)"}, replaces = "constructArrayWithIntLength")
        protected DynamicObject constructWithLength(DynamicObject newTarget, @SuppressWarnings("unused") Object[] args,
                        @Cached @SuppressWarnings("unused") ToArrayLengthNode toArrayLengthNode,
                        @Cached("create(getContext())") ArrayCreateNode arrayCreateNode,
                        @Bind("toArrayLengthNode.executeLong(firstArg(args))") long len) {
            DynamicObject array = arrayCreateNode.execute(len);
            return swapPrototype(array, newTarget);
        }

        static Object firstArg(Object[] arguments) {
            return arguments[0];
        }

        @Specialization(guards = "isOneForeignArg(args)", limit = "3")
        protected DynamicObject constructWithForeignArg(DynamicObject newTarget, Object[] args,
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
                            DynamicObject array = arrayCreateNode.execute(length);
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
                return swapPrototype(JSArray.create(getContext(), ConstantObjectArray.createConstantObjectArray(), args, 1), newTarget);
            }
        }

        @Specialization(guards = {"!isOneNumberArg(args)", "!isOneForeignArg(args)"})
        protected DynamicObject constructArrayVarargs(DynamicObject newTarget, Object[] args,
                        @Cached("create()") BranchProfile isIntegerCase,
                        @Cached("create()") BranchProfile isDoubleCase,
                        @Cached("create()") BranchProfile isObjectCase,
                        @Cached("createBinaryProfile()") ConditionProfile isLengthZero) {
            if (isLengthZero.profile(args == null || args.length == 0)) {
                return swapPrototype(JSArray.create(getContext(), ScriptArray.createConstantEmptyArray(), args, 0), newTarget);
            } else {
                ArrayContentType type = ArrayLiteralNode.identifyPrimitiveContentType(args, false);
                if (type == ArrayContentType.Integer) {
                    isIntegerCase.enter();
                    return swapPrototype(JSArray.createZeroBasedIntArray(getContext(), ArrayLiteralNode.createIntArray(args)), newTarget);
                } else if (type == ArrayContentType.Double) {
                    isDoubleCase.enter();
                    return swapPrototype(JSArray.createZeroBasedDoubleArray(getContext(), ArrayLiteralNode.createDoubleArray(args)), newTarget);
                } else {
                    isObjectCase.enter();
                    return swapPrototype(JSArray.create(getContext(), ConstantObjectArray.createConstantObjectArray(), args, args.length), newTarget);
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
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructBoolean(DynamicObject newTarget, Object value,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            return swapPrototype(JSBoolean.create(getContext(), toBoolean.executeBoolean(value)), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getBooleanPrototype();
        }
    }

    public abstract static class CallDateNode extends JSBuiltinNode {
        public CallDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected String callDate() {
            // called as function ECMAScript 15.9.2.1
            JSRealm realm = getContext().getRealm();
            return JSDate.toString(realm.currentTimeMillis(), realm);
        }
    }

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
                toPrimitiveNode = insert(JSToPrimitiveNode.create(Hint.None));
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
        protected DynamicObject constructDateZero(DynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSDate.create(getContext(), now()), newTarget);
        }

        @Specialization(guards = {"args.length == 1"})
        protected DynamicObject constructDateOne(DynamicObject newTarget, Object[] args,
                        @Cached("createBinaryProfile()") ConditionProfile isSpecialCase,
                        @CachedLibrary(limit = "3") InteropLibrary interop) {
            double dateValue = getDateValue(args[0], interop);
            return swapPrototype(JSDate.create(getContext(), timeClip(dateValue, isSpecialCase)), newTarget);
        }

        @Specialization(guards = {"args.length >= 2"})
        protected DynamicObject constructDateMult(DynamicObject newTarget, Object[] args) {
            double val = constructorImpl(args);
            return swapPrototype(JSDate.create(getContext(), val), newTarget);
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
            return getContext().getRealm().currentTimeMillis();
        }

        @TruffleBoundary
        private double parseDate(String target) {
            Integer[] fields = getContext().getEvaluator().parseDate(getContext().getRealm(), target.trim());
            if (gotFieldsProfile.profile(fields != null)) {
                return JSDate.makeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7], getContext());
            }
            return Double.NaN;
        }

        private double getDateValue(Object arg0, InteropLibrary interop) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                if (isDateProfile.profile(JSDate.isJSDate(arg0))) {
                    return JSDate.getTimeMillisField((DynamicObject) arg0);
                } else if (interop.isInstant(arg0)) {
                    return JSDate.getDateValueFromInstant(arg0, interop);
                }
            }
            Object value = toPrimitive(arg0);
            if (stringOrNumberProfile.profile(JSRuntime.isString(value))) {
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
            return JSDate.executeConstructor(argsEvaluated, false, getContext());
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDatePrototype();
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
        protected DynamicObject constructRegExp(DynamicObject newTarget, Object pattern, Object flags,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            boolean hasMatchSymbol = isRegExpNode.executeBoolean(pattern);
            if (isCall) {
                // we are in the "call" case, i.e. NewTarget is undefined (before)
                if (callIsRegExpProfile.profile(hasMatchSymbol && flags == Undefined.instance && JSObject.isJSObject(pattern))) {
                    DynamicObject patternObj = (DynamicObject) pattern;
                    Object patternConstructor = getConstructor(patternObj);
                    if (constructorEquivalentProfile.profile(patternConstructor == getContext().getRealm().getRegExpConstructor())) {
                        return patternObj;
                    }
                }
                return constructRegExpImpl(pattern, flags, hasMatchSymbol, true);
            } else {
                // we are in the "construct" case, i.e. NewTarget is NOT undefined
                return swapPrototype(constructRegExpImpl(pattern, flags, hasMatchSymbol, newTarget == getContext().getRealm().getRegExpConstructor()), newTarget);
            }

        }

        protected DynamicObject constructRegExpImpl(Object patternObj, Object flags, boolean hasMatchSymbol, boolean legacyFeaturesEnabled) {
            Object p;
            Object f;
            boolean isJSRegExp = JSRegExp.isJSRegExp(patternObj);
            if (isJSRegExp) {
                regexpObject.enter();
                Object compiledRegex = JSRegExp.getCompiledRegexUnchecked((DynamicObject) patternObj, isJSRegExp);
                if (flags == Undefined.instance) {
                    return getCreateRegExpNode().createRegExp(compiledRegex);
                } else {
                    if (getContext().getEcmaScriptVersion() < 6) {
                        throw Errors.createTypeError("Cannot supply flags when constructing one RegExp from another");
                    }
                    String flagsStr = flagsToString(flags);
                    regexpObjectNewFlagsBranch.enter();
                    Object newCompiledRegex = getCompileRegexNode().compile(getInteropReadPatternNode().execute(compiledRegex, TRegexUtil.Props.CompiledRegex.PATTERN), flagsStr);
                    return getCreateRegExpNode().createRegExp(newCompiledRegex);
                }
            } else if (hasMatchSymbol) {
                regexpMatcherObject.enter();
                DynamicObject patternJSObj = (DynamicObject) patternObj;
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

            String patternStr = getPatternToStringNode().executeString(p);
            String flagsStr = flagsToString(f);
            Object compiledRegex = getCompileRegexNode().compile(patternStr, flagsStr);
            return getCreateRegExpNode().createRegExp(compiledRegex, legacyFeaturesEnabled);
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

        private String flagsToString(Object f) {
            if (flagsToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                flagsToStringNode = insert(JSToStringNode.createUndefinedToEmpty());
            }
            return flagsToStringNode.executeString(f);
        }

        private Object getConstructor(DynamicObject obj) {
            if (getConstructorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getConstructorNode = insert(PropertyGetNode.create(JSObject.CONSTRUCTOR, false, getContext()));
            }
            return getConstructorNode.getValue(obj);
        }

        private Object getSource(DynamicObject obj) {
            if (getSourceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSourceNode = insert(PropertyGetNode.create(JSRegExp.SOURCE, false, getContext()));
            }
            return getSourceNode.getValue(obj);
        }

        private Object getFlags(DynamicObject obj) {
            if (getFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFlagsNode = insert(PropertyGetNode.create(JSRegExp.FLAGS, false, getContext()));
            }
            return getFlagsNode.getValue(obj);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getRegExpPrototype();
        }

    }

    public abstract static class CallStringNode extends JSBuiltinNode {
        public CallStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"args.length == 0"})
        protected String callStringInt0(@SuppressWarnings("unused") Object[] args) {
            return "";
        }

        @Specialization(guards = {"args.length != 0"})
        protected String callStringGeneric(Object[] args,
                        @Cached("createSymbolToString()") JSToStringNode toStringNode) {
            return toStringNode.executeString(args[0]);
        }
    }

    public abstract static class ConstructStringNode extends ConstructWithNewTargetNode {

        public ConstructStringNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"args.length == 0"})
        protected DynamicObject constructStringInt0(DynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSString.create(getContext(), ""), newTarget);
        }

        @Specialization(guards = {"args.length != 0"})
        protected DynamicObject constructString(DynamicObject newTarget, Object[] args,
                        @Cached("create()") JSToStringNode toStringNode) {
            return swapPrototype(JSString.create(getContext(), toStringNode.executeString(args[0])), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getStringPrototype();
        }

    }

    public abstract static class ConstructWeakRefNode extends ConstructWithNewTargetNode {
        public ConstructWeakRefNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"isJSObject(target)"})
        protected DynamicObject constructWeakRef(DynamicObject newTarget, Object target) {
            return swapPrototype(JSWeakRef.create(getContext(), target), newTarget);
        }

        @Specialization(guards = {"!isJSObject(target)"})
        protected DynamicObject constructWeakRefNonObject(@SuppressWarnings("unused") DynamicObject newTarget, @SuppressWarnings("unused") Object target) {
            throw Errors.createTypeError("WeakRef: target must be an object");
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakRefPrototype();
        }
    }

    public abstract static class ConstructFinalizationRegistryNode extends ConstructWithNewTargetNode {
        public ConstructFinalizationRegistryNode(JSContext context, JSBuiltin builtin, boolean newTargetCase) {
            super(context, builtin, newTargetCase);
        }

        @Specialization(guards = {"isCallable(cleanupCallback)"})
        protected DynamicObject constructFinalizationRegistry(DynamicObject newTarget, TruffleObject cleanupCallback) {
            return swapPrototype(JSFinalizationRegistry.create(getContext(), cleanupCallback), newTarget);
        }

        @Specialization(guards = {"!isCallable(cleanupCallback)"})
        protected DynamicObject constructFinalizationRegistryNonObject(@SuppressWarnings("unused") DynamicObject newTarget, @SuppressWarnings("unused") Object cleanupCallback) {
            throw Errors.createTypeError("FinalizationRegistry: cleanup must be callable");
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject callCollator(Object locales, Object options) {
            DynamicObject collator = JSCollator.create(getContext());
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
        protected DynamicObject constructCollator(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject collator = swapPrototype(JSCollator.create(getContext()), newTarget);
            return initializeCollatorNode.executeInit(collator, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructListFormat(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject listFormat = swapPrototype(JSListFormat.create(getContext()), newTarget);
            return initializeListFormatNode.executeInit(listFormat, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructRelativeTimeFormat(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject listFormat = swapPrototype(JSRelativeTimeFormat.create(getContext()), newTarget);
            return initializeRelativeTimeFormatNode.executeInit(listFormat, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructSegmenter(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject segmenter = swapPrototype(JSSegmenter.create(getContext()), newTarget);
            return initializeSegmenterNode.executeInit(segmenter, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructDisplayNames(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject displayNames = swapPrototype(JSDisplayNames.create(getContext()), newTarget);
            return initializeDisplayNamesNode.executeInit(displayNames, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructLocale(DynamicObject newTarget, Object tag, Object options) {
            DynamicObject locale = swapPrototype(JSLocale.create(getContext()), newTarget);
            return initializeLocaleNode.executeInit(locale, tag, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject callNumberFormat(Object locales, Object options) {
            DynamicObject numberFormat = JSNumberFormat.create(getContext());
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
        protected DynamicObject constructNumberFormat(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject numberFormat = swapPrototype(JSNumberFormat.create(getContext()), newTarget);
            return initializeNumberFormatNode.executeInit(numberFormat, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructPluralRules(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject pluralRules = swapPrototype(JSPluralRules.create(getContext()), newTarget);
            return initializePluralRulesNode.executeInit(pluralRules, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject callDateTimeFormat(Object locales, Object options) {
            DynamicObject dateTimeFormat = JSDateTimeFormat.create(getContext());
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
        protected DynamicObject constructDateTimeFormat(DynamicObject newTarget, Object locales, Object options) {
            DynamicObject dateTimeFormat = swapPrototype(JSDateTimeFormat.create(getContext()), newTarget);
            return initializeDateTimeFormatNode.executeInit(dateTimeFormat, locales, options);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDateTimeFormatPrototype();
        }

    }

    public abstract static class ConstructObjectNode extends ConstructWithNewTargetNode {
        public ConstructObjectNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        protected static boolean arg0NullOrUndefined(Object[] args) {
            Object arg0 = args[0];
            return (arg0 == Undefined.instance) || (arg0 == Null.instance);
        }

        @Specialization(guards = {"isNewTargetCase"})
        protected DynamicObject constructObjectNewTarget(DynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"arguments.length == 0"})
        protected DynamicObject constructObject0(DynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"!isNewTargetCase", "arguments.length > 0", "!arg0NullOrUndefined(arguments)"})
        protected Object constructObjectJSObject(@SuppressWarnings("unused") DynamicObject newTarget, Object[] arguments,
                        @Cached("createToObject(getContext())") JSToObjectNode toObjectNode) {
            return toObjectNode.execute(arguments[0]);
        }

        @Specialization(guards = {"arguments.length > 0", "arg0NullOrUndefined(arguments)"})
        protected DynamicObject constructObjectNullOrUndefined(DynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        private DynamicObject newObject(DynamicObject newTarget) {
            return swapPrototype(JSUserObject.create(getContext()), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
        protected DynamicObject constructNumberZero(DynamicObject newTarget, @SuppressWarnings("unused") Object[] args) {
            return swapPrototype(JSNumber.create(getContext(), 0), newTarget);
        }

        @Specialization(guards = {"args.length > 0"})
        protected DynamicObject constructNumber(DynamicObject newTarget, Object[] args,
                        @Cached("create()") JSToNumericNode toNumericNode,
                        @Cached("create()") JSNumericToNumberNode toNumberFromNumericNode) {
            return swapPrototype(JSNumber.create(getContext(), toNumberFromNumericNode.executeNumeric(toNumericNode.execute(args[0]))), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
                toPrimitiveNode = insert(JSToPrimitiveNode.create(Hint.Number));
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
                return toBigIntNode.executeBigInteger(value);
            }
        }
    }

    public abstract static class ConstructBigIntNode extends JSBuiltinNode {

        public ConstructBigIntNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static final DynamicObject construct() {
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
        protected final DynamicObject constructFunction(DynamicObject newTarget, Object[] args,
                        @Cached("createBinaryProfile()") ConditionProfile hasArgsProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasParamsProfile) {
            int argc = args.length;
            String[] params;
            String body;
            if (hasArgsProfile.profile(argc > 0)) {
                params = new String[argc - 1];
                for (int i = 0; i < argc - 1; i++) {
                    params[i] = toStringNode.executeString(args[i]);
                }
                body = toStringNode.executeString(args[argc - 1]);
            } else {
                params = new String[0];
                body = "";
            }
            String paramList = hasParamsProfile.profile(argc > 1) ? join(params) : "";
            return swapPrototype(functionNode.executeFunction(paramList, body, getSourceName()), newTarget);
        }

        @TruffleBoundary
        private static String join(String[] params) {
            StringJoiner sj = new StringJoiner(",");
            for (String param : params) {
                sj.add(param);
            }
            return sj.toString();
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
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
                sourceName = EvalNode.findAndFormatEvalOrigin(getContext().getRealm().getCallNode(), getContext());
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

    static final class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 7813848977534444613L;
        private final int maxCacheSize;

        LRUCache(int maxCacheSize) {
            super(16, 0.75F, true);
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
            return size() > maxCacheSize;
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

        protected abstract DynamicObject executeFunction(String paramList, String body, String sourceName);

        protected static boolean equals(String a, String b) {
            return a.equals(b);
        }

        protected LRUCache<CachedSourceKey, ScriptNode> createCache() {
            return new LRUCache<>(context.getContextOptions().getFunctionConstructorCacheSize());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(cachedParamList, paramList)", "equals(cachedBody, body)", "equals(cachedSourceName, sourceName)"}, limit = "1")
        protected final DynamicObject doCached(String paramList, String body, String sourceName,
                        @Cached("paramList") String cachedParamList,
                        @Cached("body") String cachedBody,
                        @Cached("sourceName") String cachedSourceName,
                        @Cached("parseFunction(paramList, body, sourceName)") ScriptNode parsedFunction) {
            return evalParsedFunction(context.getRealm(), parsedFunction);
        }

        @Specialization(replaces = "doCached")
        protected final DynamicObject doUncached(String paramList, String body, String sourceName,
                        @Cached("createCache()") LRUCache<CachedSourceKey, ScriptNode> cache,
                        @Cached("createCountingProfile()") ConditionProfile cacheHit) {
            ScriptNode cached = cacheLookup(cache, new CachedSourceKey(paramList, body, sourceName));
            if (cacheHit.profile(cached == null)) {
                return parseAndEvalFunction(cache, context.getRealm(), paramList, body, sourceName);
            } else {
                return evalParsedFunction(context.getRealm(), cached);
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
        private static DynamicObject evalParsedFunction(JSRealm realm, ScriptNode parsedFunction) {
            return (DynamicObject) parsedFunction.run(realm);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private DynamicObject parseAndEvalFunction(LRUCache<CachedSourceKey, ScriptNode> cache, JSRealm realm, String paramList, String body, String sourceName) {
            ScriptNode parsedBody = parseFunction(paramList, body, sourceName);
            synchronized (cache) {
                cache.put(new CachedSourceKey(paramList, body, sourceName), parsedBody);
            }
            return evalParsedFunction(realm, parsedBody);
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

    public abstract static class ConstructArrayBufferNode extends ConstructWithNewTargetNode {
        private final ConditionProfile badLengthCondition = ConditionProfile.createBinaryProfile();
        private final boolean useShared;
        @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorNode;

        public ConstructArrayBufferNode(JSContext context, JSBuiltin builtin, boolean useShared, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.useShared = useShared;
            if (isNewTargetCase) {
                getPrototypeFromConstructorNode = GetPrototypeFromConstructorNode.create(context, null, realm -> getIntrinsicDefaultProto(realm));
            }
        }

        protected boolean isHostByteBuffer(Object buffer) {
            return getContext().getRealm().getEnv().isHostObject(buffer);
        }

        @Specialization(guards = {"!isByteBuffer(length)", "!isHostByteBuffer(length)"})
        protected DynamicObject constructFromLength(DynamicObject newTarget, Object length,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            long byteLength = toIndexNode.executeLong(length);

            DynamicObject prototype = null;
            if (isNewTargetCase) {
                prototype = getPrototypeFromConstructorNode.executeWithConstructor(newTarget);
            }

            if (badLengthCondition.profile(byteLength > getContext().getContextOptions().getMaxTypedArrayLength())) {
                throw Errors.createRangeError("Array buffer allocation failed");
            }

            DynamicObject arrayBuffer;
            JSContext contextFromNewTarget = getContext();
            if (useShared) {
                arrayBuffer = JSSharedArrayBuffer.createSharedArrayBuffer(contextFromNewTarget, (int) byteLength);
            } else {
                if (getContext().isOptionDirectByteBuffer()) {
                    arrayBuffer = JSArrayBuffer.createDirectArrayBuffer(contextFromNewTarget, (int) byteLength);
                } else {
                    arrayBuffer = JSArrayBuffer.createArrayBuffer(contextFromNewTarget, (int) byteLength);
                }
            }
            if (isNewTargetCase) {
                JSObject.setPrototype(arrayBuffer, prototype);
            }
            return arrayBuffer;
        }

        @Specialization(guards = "isHostByteBuffer(buffer)")
        protected DynamicObject constructFromHostByteBuffer(DynamicObject newTarget, Object buffer,
                        @Cached("create()") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") ConditionProfile isDirect) {
            Object maybeBuffer = getContext().getRealm().getEnv().asHostObject(buffer);
            if (maybeBuffer instanceof ByteBuffer) {
                ByteBuffer byteBuffer = (ByteBuffer) maybeBuffer;
                if (isDirect.profile(byteBuffer.isDirect())) {
                    return swapPrototype(JSArrayBuffer.createDirectArrayBuffer(getContext(), byteBuffer), newTarget);
                } else {
                    return swapPrototype(JSArrayBuffer.createArrayBuffer(getContext(), byteBuffer.array()), newTarget);
                }
            } else {
                errorBranch.enter();
                throw Errors.createTypeError("Unsupported input data type");
            }
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return useShared ? realm.getSharedArrayBufferPrototype() : realm.getArrayBufferPrototype();
        }

    }

    public abstract static class ConstructErrorNode extends ConstructWithNewTargetNode {
        private final JSErrorType errorType;
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;

        public ConstructErrorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.errorType = JSErrorType.valueOf(getBuiltin().getName());
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create(context);
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
        }

        @Specialization
        protected DynamicObject constructError(VirtualFrame frame, DynamicObject newTarget, String message) {
            return constructErrorImpl(frame, newTarget, message);
        }

        @Specialization
        protected DynamicObject constructError(VirtualFrame frame, DynamicObject newTarget, Object message,
                        @Cached("create()") JSToStringNode toStringNode) {
            return constructErrorImpl(frame, newTarget, message == Undefined.instance ? null : toStringNode.executeString(message));
        }

        private DynamicObject constructErrorImpl(VirtualFrame frame, DynamicObject newTarget, String message) {
            DynamicObject errorObj;
            JSContext context = getContext();
            JSRealm realm = context.getRealm();
            if (message == null) {
                errorObj = JSObject.create(context, context.getErrorFactory(errorType, false));
            } else {
                errorObj = JSObject.create(context, context.getErrorFactory(errorType, true), message);
            }
            swapPrototype(errorObj, newTarget);

            int stackTraceLimit = stackTraceLimitNode.executeInt(frame);
            DynamicObject errorFunction = realm.getErrorConstructor(errorType);
            GraalJSException exception = JSException.createCapture(errorType, message, errorObj, realm, stackTraceLimit, errorFunction);
            return initErrorObjectNode.execute(errorObj, exception);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorPrototype(errorType);
        }

    }

    @ImportStatic(JSRuntime.class)
    public abstract static class ConstructAggregateErrorNode extends ConstructWithNewTargetNode {
        @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
        @Child private InitErrorObjectNode initErrorObjectNode;

        public ConstructAggregateErrorNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
            this.stackTraceLimitNode = ErrorStackTraceLimitNode.create(context);
            this.initErrorObjectNode = InitErrorObjectNode.create(context);
        }

        GetMethodNode createGetIteratorMethod() {
            return GetMethodNode.create(getContext(), null, Symbol.SYMBOL_ITERATOR);
        }

        @Specialization
        protected DynamicObject constructError(VirtualFrame frame, DynamicObject newTarget, Object errorsObj, Object messageObj,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("createGetIteratorMethod()") GetMethodNode getIteratorMethodNode,
                        @Cached("createCall()") JSFunctionCallNode iteratorCallNode,
                        @Cached("create()") IsJSObjectNode isObjectNode,
                        @Cached("create(getContext())") IteratorStepNode iteratorStepNode,
                        @Cached("create(getContext())") IteratorValueNode getIteratorValueNode,
                        @Cached("create(NEXT, getContext())") PropertyGetNode getNextMethodNode,
                        @Cached("create()") BranchProfile growProfile) {
            String message = messageObj == Undefined.instance ? null : toStringNode.executeString(messageObj);
            Object usingIterator = getIteratorMethodNode.executeWithTarget(errorsObj);
            SimpleArrayList<Object> errors = GetIteratorNode.iterableToList(errorsObj, usingIterator, iteratorCallNode, isObjectNode, iteratorStepNode, getIteratorValueNode, getNextMethodNode, this,
                            growProfile);
            DynamicObject errorObj;
            JSContext context = getContext();
            JSRealm realm = context.getRealm();
            Object errorsArray = JSArray.createConstantObjectArray(context, errors.toArray());
            if (message == null) {
                errorObj = JSObject.create(context, context.getErrorFactory(JSErrorType.AggregateError, false), errorsArray);
            } else {
                errorObj = JSObject.create(context, context.getErrorFactory(JSErrorType.AggregateError, true), message, errorsArray);
            }
            swapPrototype(errorObj, newTarget);

            int stackTraceLimit = stackTraceLimitNode.executeInt(frame);
            DynamicObject errorFunction = realm.getErrorConstructor(JSErrorType.AggregateError);
            GraalJSException exception = JSException.createCapture(JSErrorType.AggregateError, message, errorObj, realm, stackTraceLimit, errorFunction);
            return initErrorObjectNode.execute(errorObj, exception);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorPrototype(JSErrorType.AggregateError);
        }

    }

    public abstract static class ConstructDataViewNode extends ConstructWithNewTargetNode {
        public ConstructDataViewNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final DynamicObject constructDataView(DynamicObject newTarget, Object buffer, Object byteOffset, Object byteLength,
                        @Cached("create()") BranchProfile errorBranch,
                        @Cached("createBinaryProfile()") ConditionProfile arrayBufferCondition,
                        @Cached("createBinaryProfile()") ConditionProfile byteLengthCondition,
                        @Cached("create()") JSToIndexNode offsetToIndexNode,
                        @Cached("create()") JSToIndexNode lengthToIndexNode) {
            boolean direct;
            if (arrayBufferCondition.profile(JSArrayBuffer.isJSHeapArrayBuffer(buffer))) {
                direct = false;
            } else if (JSArrayBuffer.isJSDirectOrSharedArrayBuffer(buffer)) {
                direct = true;
            } else {
                errorBranch.enter();
                throw Errors.createTypeError("Not an ArrayBuffer");
            }

            DynamicObject arrayBuffer = (DynamicObject) buffer;

            long offset = offsetToIndexNode.executeLong(byteOffset);

            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeError("detached buffer cannot be used");
            }

            int bufferByteLength = direct ? JSArrayBuffer.getDirectByteLength(arrayBuffer) : JSArrayBuffer.getByteLength(arrayBuffer);
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
            DynamicObject result = swapPrototype(JSDataView.createDataView(getContext(), arrayBuffer, (int) offset, (int) viewByteLength), newTarget);
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            return result;
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDataViewPrototype();
        }

    }

    public abstract static class CallRequiresNewNode extends JSBuiltinNode {

        public CallRequiresNewNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final DynamicObject call() {
            throw Errors.createTypeErrorFormat("Constructor %s requires 'new'", getBuiltin().getName());
        }
    }

    public abstract static class ConstructJSAdapterNode extends JSBuiltinNode {
        public ConstructJSAdapterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSObject(adaptee)", "isUndefined(undefined1)", "isUndefined(undefined2)"})
        protected DynamicObject constructJSAdapter(DynamicObject adaptee, @SuppressWarnings("unused") Object undefined1, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), adaptee, null, null);
        }

        @Specialization(guards = {"isJSObject(overrides)", "isJSObject(adaptee)", "isUndefined(undefined2)"})
        protected DynamicObject constructJSAdapter(DynamicObject overrides, DynamicObject adaptee, @SuppressWarnings("unused") Object undefined2) {
            return JSAdapter.create(getContext(), adaptee, overrides, null);
        }

        @Specialization(guards = {"isJSObject(proto)", "isJSObject(overrides)", "isJSObject(adaptee)"})
        protected DynamicObject constructJSAdapter(DynamicObject proto, DynamicObject overrides, DynamicObject adaptee) {
            return JSAdapter.create(getContext(), adaptee, overrides, proto);
        }

        @Fallback
        protected DynamicObject constructJSAdapter(Object proto, Object overrides, Object adaptee) {
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
        protected DynamicObject constructJSProxy(DynamicObject newTarget, Object target, Object handler) {
            if (targetNonObject.profile(!JSGuards.isTruffleObject(target) || target instanceof Symbol || target == Undefined.instance || target == Null.instance || target instanceof JSLazyString ||
                            target instanceof SafeInteger || target instanceof BigInt)) {
                throw Errors.createTypeError("target expected to be an object");
            }
            if (handlerNonObject.profile(!JSGuards.isJSObject(handler))) {
                throw Errors.createTypeError("handler expected to be an object");
            }
            DynamicObject handlerObj = (DynamicObject) handler;
            return swapPrototype(JSProxy.create(getContext(), target, handlerObj), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getProxyPrototype();
        }

        public abstract DynamicObject execute(DynamicObject newTarget, Object target, Object handler);
    }

    public abstract static class ConstructJavaImporterNode extends JSBuiltinNode {
        public ConstructJavaImporterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject constructJavaImporter(Object[] args) {
            SimpleArrayList<DynamicObject> pkgs = new SimpleArrayList<>(args.length);
            for (Object pkg : args) {
                if (JavaPackage.isJavaPackage(pkg)) {
                    pkgs.addUnchecked((DynamicObject) pkg);
                }
            }
            return JavaImporter.create(getContext(), pkgs.toArray(new DynamicObject[pkgs.size()]));
        }
    }

    public abstract static class JSConstructIterableOperation extends ConstructWithNewTargetNode {
        public JSConstructIterableOperation(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private JSFunctionCallNode callAdderNode;
        @Child private PropertyGetNode getAdderFnNode;
        protected final ConditionProfile needFillIterable = ConditionProfile.createBinaryProfile();
        protected final BranchProfile errorBranch = BranchProfile.create();

        protected void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected IteratorRecord getIterator(Object iterator) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorNode.create(getContext()));
            }
            return getIteratorNode.execute(iterator);
        }

        protected Object getIteratorValue(DynamicObject iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected Object iteratorStep(IteratorRecord iterator) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iterator);
        }

        protected Object call(Object target, DynamicObject function, Object... userArguments) {
            if (callAdderNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callAdderNode = insert(JSFunctionCallNode.createCall());
            }
            return callAdderNode.executeCall(JSArguments.create(target, function, userArguments));
        }

        protected Object getAdderFn(DynamicObject obj, String name) {
            if (getAdderFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAdderFnNode = insert(PropertyGetNode.create(name, false, getContext()));
            }
            return getAdderFnNode.getValue(obj);
        }
    }

    public abstract static class ConstructMapNode extends JSConstructIterableOperation {

        private @Child ReadElementNode readElementNode;

        public ConstructMapNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected DynamicObject constructMap(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject mapObj = JSMap.create(context);
            fillWithIterable(mapObj, iterable);
            return swapPrototype(mapObj, newTarget);
        }

        protected void fillWithIterable(DynamicObject mapObj, Object iterable) {
            if (needFillIterable.profile(iterable == Undefined.instance || iterable == Null.instance)) {
                return;
            } else {
                Object adder = getAdderFn(mapObj, "set");
                if (!JSFunction.isJSFunction(adder)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("function set not callable");
                }
                DynamicObject adderFn = (DynamicObject) adder;
                IteratorRecord iter = getIterator(iterable);

                try {
                    while (true) {
                        Object next = iteratorStep(iter);
                        if (next == Boolean.FALSE) {
                            return;
                        }
                        Object nextItem = getIteratorValue((DynamicObject) next);
                        if (!JSObject.isDynamicObject(nextItem)) {
                            errorBranch.enter();
                            throw Errors.createTypeErrorIteratorResultNotObject(nextItem, this);
                        }
                        Object k = readElement(nextItem, 0);
                        Object v = readElement(nextItem, 1);
                        call(mapObj, adderFn, k, v);
                    }
                } catch (Exception ex) {
                    iteratorCloseAbrupt(iter.getIterator());
                    throw ex;
                }
            }
        }

        private Object readElement(Object target, int index) {
            if (readElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readElementNode = insert(ReadElementNode.create(getContext()));
            }
            return readElementNode.executeWithTargetAndIndex(target, index);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getMapPrototype();
        }

    }

    public abstract static class ConstructSetNode extends JSConstructIterableOperation {
        public ConstructSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected DynamicObject constructSet(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject setObj = JSSet.create(context);
            fillWithIterable(setObj, iterable);
            return swapPrototype(setObj, newTarget);
        }

        protected void fillWithIterable(DynamicObject setObj, Object iterable) {
            if (needFillIterable.profile(iterable == Undefined.instance || iterable == Null.instance)) {
                return;
            } else {
                Object adder = getAdderFn(setObj, "add");
                if (!JSFunction.isJSFunction(adder)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("function add not callable");
                }
                DynamicObject adderFn = (DynamicObject) adder;
                IteratorRecord iter = getIterator(iterable);

                try {
                    while (true) {
                        Object next = iteratorStep(iter);
                        if (next == Boolean.FALSE) {
                            return;
                        }
                        Object nextValue = getIteratorValue((DynamicObject) next);
                        call(setObj, adderFn, nextValue);
                    }
                } catch (Exception ex) {
                    iteratorCloseAbrupt(iter.getIterator());
                    throw ex;
                }
            }
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSetPrototype();
        }

    }

    public abstract static class ConstructWeakSetNode extends ConstructSetNode {
        public ConstructWeakSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @TruffleBoundary
        protected static Map<DynamicObject, Object> constructWeakHashMap() {
            return new WeakHashMap<>();
        }

        @Override
        @Specialization
        protected DynamicObject constructSet(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject setObj = JSObject.create(context, context.getWeakSetFactory(), constructWeakHashMap());
            fillWithIterable(setObj, iterable);
            return swapPrototype(setObj, newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakSetPrototype();
        }

    }

    public abstract static class ConstructWeakMapNode extends ConstructMapNode {
        public ConstructWeakMapNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @TruffleBoundary
        protected static Map<DynamicObject, Object> constructWeakMap() {
            return new WeakMap();
        }

        @Override
        @Specialization
        protected DynamicObject constructMap(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject mapObj = JSObject.create(context, context.getWeakMapFactory(), constructWeakMap());
            fillWithIterable(mapObj, iterable);
            return swapPrototype(mapObj, newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getWeakMapPrototype();
        }

    }

    public abstract static class CallSymbolNode extends JSBuiltinNode {
        public CallSymbolNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Symbol callSymbolString(String value) {
            return Symbol.create(value);
        }

        @Specialization(guards = "!isString(value)")
        protected Symbol callSymbolGeneric(Object value,
                        @Cached JSToStringNode toStringNode) {
            return Symbol.create(value == Undefined.instance ? null : toStringNode.executeString(value));
        }
    }

    public abstract static class ConstructSymbolNode extends JSBuiltinNode {

        public ConstructSymbolNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static final DynamicObject construct() {
            throw Errors.createTypeError("cannot construct a Symbol");
        }
    }

    public abstract static class PromiseConstructorNode extends JSBuiltinNode {
        @Child protected IsCallableNode isCallable;
        @Child private PromiseResolveThenableNode promiseResolveThenable;
        @Child private OrdinaryCreateFromConstructorNode createPromiseFromConstructor;
        @Child private PropertySetNode setPromiseState;
        @Child private PropertySetNode setPromiseFulfillReactions;
        @Child private PropertySetNode setPromiseRejectReactions;
        @Child private PropertySetNode setPromiseIsHandled;

        public PromiseConstructorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isCallable = IsCallableNode.create();
            this.promiseResolveThenable = PromiseResolveThenableNode.create(context);
            this.createPromiseFromConstructor = OrdinaryCreateFromConstructorNode.create(context, null, JSRealm::getPromisePrototype, JSPromise.INSTANCE);
            this.setPromiseState = PropertySetNode.createSetHidden(JSPromise.PROMISE_STATE, context);
            this.setPromiseFulfillReactions = PropertySetNode.createSetHidden(JSPromise.PROMISE_FULFILL_REACTIONS, context);
            this.setPromiseRejectReactions = PropertySetNode.createSetHidden(JSPromise.PROMISE_REJECT_REACTIONS, context);
            this.setPromiseIsHandled = PropertySetNode.createSetHidden(JSPromise.PROMISE_IS_HANDLED, context);
        }

        @Specialization(guards = "isCallable.executeBoolean(executor)")
        protected DynamicObject construct(VirtualFrame frame, DynamicObject newTarget, Object executor) {
            DynamicObject promise = createPromiseFromConstructor.executeWithConstructor(frame, newTarget);
            setPromiseState.setValueInt(promise, JSPromise.PENDING);
            setPromiseFulfillReactions.setValue(promise, new SimpleArrayList<>());
            setPromiseRejectReactions.setValue(promise, new SimpleArrayList<>());
            setPromiseIsHandled.setValueBoolean(promise, false);

            getContext().notifyPromiseHook(PromiseHook.TYPE_INIT, promise);

            promiseResolveThenable.execute(promise, Undefined.instance, executor);
            return promise;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable.executeBoolean(executor)")
        protected DynamicObject notCallable(DynamicObject newTarget, Object executor) {
            throw Errors.createTypeError("cannot create promise: executor not callable");
        }
    }
}
