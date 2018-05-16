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
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.JSTruffleOptions.ECMAScript2017;
import static com.oracle.truffle.js.runtime.JSTruffleOptions.ECMAScript2018;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.WeakHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
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
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayBufferNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructArrayNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBigIntNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructBooleanNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructCollatorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDataViewNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructDateTimeFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructErrorNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJSAdapterNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJSProxyNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJavaImporterNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJavaInteropWorkerNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructNumberFormatNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructNumberNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructObjectNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructPluralRulesNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructRegExpNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructStringNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructSymbolNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakMapNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructWeakSetNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.CreateDynamicFunctionNodeGen;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.PromiseConstructorNodeGen;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeEvaluator;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ArrayCreateNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode.ArrayContentType;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeFromConstructorNode;
import com.oracle.truffle.js.nodes.access.IsRegExpNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
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
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.control.TryCatchNode.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.intl.CreateRegExpNode;
import com.oracle.truffle.js.nodes.intl.InitializeCollatorNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.nodes.intl.InitializePluralRulesNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveThenableNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.PromiseHook;
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
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.interop.JavaImporter;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.WeakMap;

/**
 * Contains builtins for the global object.
 */
public final class ConstructorBuiltins extends JSBuiltinsContainer.SwitchEnum<ConstructorBuiltins.Constructor> {
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
        PluralRules(0),
        DateTimeFormat(0),

        Error(1),
        RangeError(1),
        TypeError(1),
        ReferenceError(1),
        SyntaxError(1),
        EvalError(1),
        URIError(1),

        Int8Array(3),
        Uint8Array(3),
        Uint8ClampedArray(3),
        Int16Array(3),
        Uint16Array(3),
        Int32Array(3),
        Uint32Array(3),
        Float32Array(3),
        Float64Array(3),
        DataView(1),

        Map(0),
        Set(0),
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
        JSAdapter(1) {
            @Override
            public boolean isEnabled() {
                return JSTruffleOptions.NashornExtensions;
            }
        },
        JavaImporter(1) {
            @Override
            public boolean isEnabled() {
                return JSTruffleOptions.NashornJavaInterop;
            }
        },
        JavaInteropWorker(1) {
            @Override
            public boolean isEnabled() {
                return !JSTruffleOptions.SubstrateVM;
            }
        };

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
            return EnumSet.range(Array, SharedArrayBuffer).contains(this);
        }

        @Override
        public int getECMAScriptVersion() {
            if (AsyncGeneratorFunction == this) {
                return ECMAScript2018;
            } else if (EnumSet.of(SharedArrayBuffer, AsyncFunction).contains(this)) {
                return ECMAScript2017;
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
            case Collator:
                return construct ? (newTarget
                                ? ConstructCollatorNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(2).createArgumentNodes(context))
                                : ConstructCollatorNodeGen.create(context, builtin, false, args().function().fixedArgs(2).createArgumentNodes(context)))
                                : CallCollatorNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
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

            default:
                if (!JSTruffleOptions.SubstrateVM) {
                    switch (builtinEnum) {
                        case JavaInteropWorker:
                            if (construct) {
                                return ConstructJavaInteropWorkerNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                            } else {
                                return createCallRequiresNew(context, builtin);
                            }
                    }
                }
                if (JSTruffleOptions.NashornJavaInterop) {
                    switch (builtinEnum) {
                        case JavaImporter:
                            return ConstructJavaImporterNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
                    }
                }
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
            JSRealm currentRealm = getContext().getRealm();
            if (isNewTargetCase) {
                return JSRuntime.getFunctionRealm(newTarget, currentRealm);
            }
            return currentRealm;
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
            if (JSTruffleOptions.TrackArrayAllocationSites && arrayAllocationSite != null && arrayAllocationSite.isTyped()) {
                return swapPrototype(JSArray.create(getContext(), arrayAllocationSite.getInitialArrayType(), ((AbstractWritableArray) arrayAllocationSite.getInitialArrayType()).allocateArray(length),
                                length), newTarget);
            }
            return swapPrototype(JSArray.createConstantEmptyArray(getContext(), arrayAllocationSite, length), newTarget);
        }

        @Specialization(guards = "isOneNumberArg(args)")
        protected DynamicObject constructWithLength(DynamicObject newTarget, Object[] args,
                        @Cached("create()") JSToUInt32Node toUInt32Node,
                        @Cached("create(getContext())") ArrayCreateNode arrayCreateNode) {
            Number origLen = (Number) args[0]; // guard ensures this is a Number
            Number origLen32 = (Number) toUInt32Node.execute(origLen);
            long len = JSArray.toArrayIndexOrRangeError(origLen, origLen32);
            DynamicObject array = arrayCreateNode.execute(len);
            return swapPrototype(array, newTarget);
        }

        @Specialization(guards = "!isOneNumberArg(args)")
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
            return JSTruffleOptions.TrackArrayAllocationSites ? new ConstructArrayAllocationSite() : null;
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getArrayConstructor().getPrototype();
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
                assert JSTruffleOptions.TrackArrayAllocationSites;
                if (arrayType instanceof AbstractWritableArray && length > 0) {
                    if (concreteArrayType == UNINIT_ARRAY_TYPE) {
                        concreteArrayType = arrayType;
                        assumption.invalidate();
                        assumption = Truffle.getRuntime().createAssumption("Array allocation site (typed)");
                    } else if (concreteArrayType != arrayType) {
                        concreteArrayType = null;
                        assumption.invalidate();
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
            return realm.getBooleanConstructor().getPrototype();
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
            return JSDate.toString(System.currentTimeMillis(), getContext());
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
                        @Cached("createBinaryProfile()") ConditionProfile isSpecialCase) {
            double dateValue = getDateValue(args[0]);
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
            return ((Double) dateValue).longValue();
        }

        @TruffleBoundary
        private static double now() {
            return System.currentTimeMillis();
        }

        @TruffleBoundary
        private double parseDate(String target) {
            Integer[] fields = getContext().getEvaluator().parseDate(getContext().getRealm(), target.trim());
            if (gotFieldsProfile.profile(fields != null)) {
                return JSDate.makeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7], getContext());
            }
            return Double.NaN;
        }

        private double getDateValue(Object arg0) {
            if (getContext().getEcmaScriptVersion() >= 6 && isDateProfile.profile(JSDate.isJSDate(arg0))) {
                return JSDate.getTimeMillisField((DynamicObject) arg0);
            } else {
                Object value = toPrimitive(arg0);
                if (stringOrNumberProfile.profile(JSRuntime.isString(value))) {
                    return parseDate(JSRuntime.toString(value));
                } else {
                    double dval = toDouble(value);
                    if (Double.isInfinite(dval) || Double.isNaN(dval)) {
                        return Double.NaN;
                    } else {
                        return dval;
                    }
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
            return realm.getDateConstructor().getPrototype();
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
        @Child private Node interopReadPatternNode;
        private final BranchProfile regexpObject = BranchProfile.create();
        private final BranchProfile regexpMatcherObject = BranchProfile.create();
        private final BranchProfile regexpNonObject = BranchProfile.create();
        private final BranchProfile regexpObjectNewFlagsBranch = BranchProfile.create();
        private final ConditionProfile callIsRegExpProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile constructorEquivalentProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected DynamicObject constructRegExp(DynamicObject newTarget, Object pattern, Object flags,
                        @Cached("create(getContext())") IsRegExpNode isRegExpNode) {
            boolean patternIsRegExp = isRegExpNode.executeBoolean(pattern);
            if (isCall) {
                // we are in the "call" case, i.e. NewTarget is undefined (before)
                if (callIsRegExpProfile.profile(patternIsRegExp && flags == Undefined.instance && JSObject.isJSObject(pattern))) {
                    DynamicObject patternObj = (DynamicObject) pattern;
                    Object patternConstructor = getConstructor(patternObj);
                    if (constructorEquivalentProfile.profile(patternConstructor == getContext().getRealm().getRegExpConstructor().getFunctionObject())) {
                        return patternObj;
                    }
                }
                return constructRegExpImpl(pattern, flags, patternIsRegExp);
            } else {
                // we are in the "construct" case, i.e. NewTarget is NOT undefined
                return swapPrototype(constructRegExpImpl(pattern, flags, patternIsRegExp), newTarget);
            }

        }

        protected DynamicObject constructRegExpImpl(Object patternObj, Object flags, boolean patternIsRegExp) {
            Object p;
            Object f;
            if (JSRegExp.isJSRegExp(patternObj)) {
                regexpObject.enter();
                TruffleObject compiledRegex = JSRegExp.getCompiledRegex((DynamicObject) patternObj);
                if (flags == Undefined.instance) {
                    return getCreateRegExpNode().execute(compiledRegex);
                } else {
                    if (getContext().getEcmaScriptVersion() < 6) {
                        throw Errors.createTypeError("Cannot supply flags when constructing one RegExp from another");
                    }
                    String flagsStr = flagsToString(flags);
                    regexpObjectNewFlagsBranch.enter();
                    TruffleObject newCompiledRegex = getCompileRegexNode().compile(TRegexUtil.readPattern(getInteropReadPatternNode(), compiledRegex), flagsStr);
                    return getCreateRegExpNode().execute(newCompiledRegex);
                }
            } else if (patternIsRegExp) {
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
            TruffleObject compiledRegex = getCompileRegexNode().compile(patternStr, flagsStr);
            return getCreateRegExpNode().execute(compiledRegex);
        }

        private JSToStringNode getPatternToStringNode() {
            if (patternToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                patternToStringNode = insert(JSToStringNode.createUndefinedToEmpty());
            }
            return patternToStringNode;
        }

        private Node getInteropReadPatternNode() {
            if (interopReadPatternNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopReadPatternNode = insert(Message.READ.createNode());
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
            return realm.getRegExpConstructor().getPrototype();
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
        protected String callString(Object[] args,
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
            return realm.getStringConstructor().getPrototype();
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
            return realm.getCollatorConstructor().getPrototype();
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
            return realm.getNumberFormatConstructor().getPrototype();
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
            return realm.getPluralRulesConstructor().getPrototype();
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
            return realm.getDateTimeFormatConstructor().getPrototype();
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

        @Specialization(guards = {"arguments.length == 0"})
        protected DynamicObject constructObject0(DynamicObject newTarget, @SuppressWarnings("unused") Object[] arguments) {
            return newObject(newTarget);
        }

        @Specialization(guards = {"arguments.length > 0", "!arg0NullOrUndefined(arguments)"})
        protected TruffleObject constructObjectJSObject(@SuppressWarnings("unused") DynamicObject newTarget, Object[] arguments,
                        @Cached("createToObject(getContext())") JSToObjectNode toObjectNode) {
            return toObjectNode.executeTruffleObject(arguments[0]);
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
            return (Number) toNumberFromNumericNode.executeObject((toNumericNode.executeObject(args[0])));
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
            return swapPrototype(JSNumber.create(getContext(), (Number) toNumberFromNumericNode.executeObject(toNumericNode.executeObject(args[0]))), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getNumberConstructor().getPrototype();
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

            Object primitiveObj = toPrimitive(args[0]);
            if (JSRuntime.isNumber(primitiveObj)) {
                return numberToBigIntNode.executeBigInt(primitiveObj);
            } else {
                return toBigIntNode.executeBigInteger(args[0]);
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
        protected final DynamicObject constructFunction(DynamicObject newTarget, Object[] args) {
            String[] params = new String[Math.max(0, args.length - 1)];
            for (int i = 0; i < args.length - 1; i++) {
                params[i] = toStringNode.executeString(args[i]);
            }
            String body = args.length > 0 ? toStringNode.executeString(args[args.length - 1]) : "";
            String paramList = args.length > 1 ? join(params) : "";
            return swapPrototype(functionNode.executeFunction(paramList, body), newTarget);
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
                return realm.getAsyncGeneratorFunctionConstructor().getPrototype();
            } else if (generatorFunction) {
                return realm.getGeneratorFunctionConstructor().getPrototype();
            } else if (asyncFunction) {
                return realm.getAsyncFunctionConstructor().getPrototype();
            } else {
                return realm.getFunctionPrototype();
            }
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

        protected abstract DynamicObject executeFunction(String paramList, String body);

        protected static boolean equals(String a, String b) {
            return a.equals(b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"equals(cachedParamList, paramList)", "equals(cachedBody, body)"}, limit = "1")
        protected final DynamicObject doCached(String paramList, String body,
                        @Cached("paramList") String cachedParamList,
                        @Cached("body") String cachedBody,
                        @Cached("parseFunction(paramList, body)") ScriptNode parsedFunction) {
            return evalParsedFunction(context.getRealm(), parsedFunction);
        }

        @Specialization
        protected final DynamicObject doUncached(String paramList, String body) {
            return parseAndEvalFunction(context.getRealm(), paramList, body);
        }

        protected final ScriptNode parseFunction(String paramList, String body) {
            CompilerAsserts.neverPartOfCompilation();
            return ((NodeEvaluator) context.getEvaluator()).parseFunction(context, this, paramList, body, generatorFunction, asyncFunction);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static DynamicObject evalParsedFunction(JSRealm realm, ScriptNode parsedFunction) {
            return (DynamicObject) parsedFunction.run(realm);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private DynamicObject parseAndEvalFunction(JSRealm realm, String paramList, String body) {
            return evalParsedFunction(realm, parseFunction(paramList, body));
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

        @Specialization(guards = "!isByteBuffer(length)")
        protected DynamicObject constructFromLength(DynamicObject newTarget, Object length,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            long byteLength = toIndexNode.executeLong(length);

            DynamicObject prototype = null;
            if (isNewTargetCase) {
                prototype = getPrototypeFromConstructorNode.executeWithConstructor(newTarget);
            }

            if (badLengthCondition.profile(byteLength > JSTruffleOptions.MaxTypedArrayLength)) {
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

        @Specialization(guards = "isByteBuffer(buffer)")
        protected DynamicObject constructFromByteBuffer(DynamicObject newTarget, Object buffer) {
            ByteBuffer byteBuffer = (ByteBuffer) buffer;
            return swapPrototype(JSArrayBuffer.createArrayBuffer(getContext(), byteBuffer.array()), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return (useShared ? realm.getSharedArrayBufferConstructor() : realm.getArrayBufferConstructor()).getPrototype();
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
            this.initErrorObjectNode = InitErrorObjectNode.create(context, false);
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
            DynamicObject errorFunction = realm.getErrorConstructor(errorType).getFunctionObject();
            GraalJSException exception = JSException.createCapture(errorType, message, errorObj, stackTraceLimit, errorFunction);
            return initErrorObjectNode.execute(errorObj, exception);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getErrorConstructor(errorType).getPrototype();
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
            return swapPrototype(JSDataView.createDataView(getContext(), arrayBuffer, (int) offset, (int) viewByteLength), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getDataViewConstructor().getPrototype();
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
        private final ConditionProfile revoked = ConditionProfile.createBinaryProfile();
        private final ConditionProfile targetNonObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile handlerNonObject = ConditionProfile.createBinaryProfile();

        public ConstructJSProxyNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        private void checkRevokedProxy(TruffleObject obj) {
            if (JSProxy.isProxy(obj) && revoked.profile(JSProxy.isRevoked((DynamicObject) obj))) {
                throw Errors.createTypeError("argument cannot be a revoked proxy");
            }
        }

        @Specialization
        protected DynamicObject constructJSProxy(DynamicObject newTarget, Object target, Object handler) {
            if (targetNonObject.profile(!JSGuards.isTruffleObject(target) || target instanceof Symbol || target == Undefined.instance || target == Null.instance || target instanceof JSLazyString ||
                            target instanceof LargeInteger || target instanceof BigInt)) {
                throw Errors.createTypeError("target expected to be an object");
            }
            if (handlerNonObject.profile(!JSGuards.isJSObject(handler))) {
                throw Errors.createTypeError("handler expected to be an object");
            }
            TruffleObject targetObj = (TruffleObject) target;
            DynamicObject handlerObj = (DynamicObject) handler;
            checkRevokedProxy(targetObj);
            checkRevokedProxy(handlerObj);
            return swapPrototype(JSProxy.create(getContext(), targetObj, handlerObj), newTarget);
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getProxyConstructor().getPrototype();
        }

        public abstract DynamicObject execute(DynamicObject newTarget, Object target, Object handler);
    }

    public abstract static class ConstructJavaImporterNode extends JSBuiltinNode {
        public ConstructJavaImporterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject constructJavaImporter(Object... args) {
            List<DynamicObject> pkgs = new ArrayList<>();
            for (Object pkg : args) {
                if (JavaPackage.isJavaPackage(pkg)) {
                    Boundaries.listAdd(pkgs, (DynamicObject) pkg);
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

        protected DynamicObject getIterator(Object iterator) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorNode.create(getContext()));
            }
            return getIteratorNode.execute(iterator);
        }

        protected Object getIteratorValue(DynamicObject iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create(getContext(), null));
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected Object iteratorStep(DynamicObject iterator) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext(), null));
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

        private final JSClassProfile classProfile = JSClassProfile.create();

        public ConstructMapNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected DynamicObject constructMap(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject mapObj = JSObject.create(context, context.getMapFactory(), new JSHashMap());
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
                DynamicObject iter = getIterator(iterable);

                try {
                    while (true) {
                        Object next = iteratorStep(iter);
                        if (next == Boolean.FALSE) {
                            return;
                        }
                        Object nextItem = getIteratorValue((DynamicObject) next);
                        if (!JSObject.isDynamicObject(nextItem)) {
                            errorBranch.enter();
                            throw Errors.createTypeError("not an object in iterator");
                        }
                        DynamicObject nextItemObj = (DynamicObject) (nextItem);
                        Object k = JSObject.get(nextItemObj, 0, classProfile);
                        Object v = JSObject.get(nextItemObj, 1, classProfile);
                        call(mapObj, adderFn, new Object[]{k, v});
                    }
                } catch (Exception ex) {
                    iteratorCloseAbrupt(iter);
                    throw ex;
                }
            }
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getMapConstructor().getPrototype();
        }

    }

    public abstract static class ConstructJavaInteropWorkerNode extends JSBuiltinNode {
        public ConstructJavaInteropWorkerNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject constructWorker() {
            JSContext context = getContext();
            DynamicObjectFactory factory = context.getRealm().getJavaInteropWorkerFactory();
            DynamicObject worker = JSObject.create(context, factory, context.getJavaInteropWorkerFactory().createAgent(context.getMainWorker()));
            return worker;
        }
    }

    public abstract static class ConstructSetNode extends JSConstructIterableOperation {
        public ConstructSetNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected DynamicObject constructSet(DynamicObject newTarget, Object iterable) {
            JSContext context = getContext();
            DynamicObject setObj = JSObject.create(context, context.getSetFactory(), new JSHashMap());
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
                DynamicObject iter = getIterator(iterable);

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
                    iteratorCloseAbrupt(iter);
                    throw ex;
                }
            }
        }

        @Override
        protected DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return realm.getSetConstructor().getPrototype();
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
            return realm.getWeakSetConstructor().getPrototype();
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
            return realm.getWeakMapConstructor().getPrototype();
        }

    }

    public abstract static class CallSymbolNode extends JSBuiltinNode {
        public CallSymbolNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToStringNode toStringNode = JSToStringNode.createUndefinedToEmpty();

        @Specialization
        protected Symbol callSymbol(Object value) {
            return Symbol.create(toStringNode.executeString(value));
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
            setPromiseFulfillReactions.setValue(promise, new ArrayList<>());
            setPromiseRejectReactions.setValue(promise, new ArrayList<>());
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
