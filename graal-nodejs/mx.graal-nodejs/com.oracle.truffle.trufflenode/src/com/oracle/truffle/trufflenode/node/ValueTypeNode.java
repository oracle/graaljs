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
package com.oracle.truffle.trufflenode.node;

import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.BIG_INT_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_FALSE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_TRUE;
import static com.oracle.truffle.trufflenode.ValueType.DATA_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DATE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_BIGINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_BIGUINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_INT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DIRECT_UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.EXTERNAL_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FUNCTION_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_BIGINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_BIGUINT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_INT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INTEROP_UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.MAP_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.NULL_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.NUMBER_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.ORDINARY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.PROMISE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.PROXY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.REGEXP_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.SET_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.STRING_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.SYMBOL_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNDEFINED_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNKNOWN_TYPE;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.array.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSDataViewObject;
import com.oracle.truffle.js.runtime.builtins.JSDateObject;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.JSExternal;
import com.oracle.truffle.trufflenode.JSExternalObject;

/**
 * Keep in sync with {@link GraalJSAccess#valueType}.
 */
@SuppressWarnings("unused")
@ImportStatic({JSExternal.class, JSRuntime.class, JSMap.class, JSSet.class, JSPromise.class, JSProxy.class, JSObject.class, JSDataView.class})
abstract class ValueTypeNode extends JavaScriptBaseNode {
    protected final JSContext context;
    protected final boolean useSharedBuffer;

    ValueTypeNode(JSContext context, boolean useSharedBuffer) {
        this.context = context;
        this.useSharedBuffer = useSharedBuffer;
    }

    public static ValueTypeNode create(JSContext context, boolean useSharedBuffer) {
        return ValueTypeNodeGen.create(context, useSharedBuffer);
    }

    protected abstract int executeInt(Object value);

    protected JSContext getContext() {
        return context;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static int doUndefined(Object value) {
        return UNDEFINED_VALUE;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(Object value) {
        return NULL_VALUE;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return value ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE;
    }

    @Specialization
    protected static int doString(@SuppressWarnings("unused") String value) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    @Specialization
    protected static int doTruffleString(TruffleString value) {
        return STRING_VALUE;
    }

    @Specialization
    protected final int doInt(int value) {
        return doDouble(value);
    }

    @Specialization
    protected final int doDouble(double value) {
        if (useSharedBuffer) {
            GraalJSAccess.get(this).getSharedBuffer().putDouble(value);
        }
        return NUMBER_VALUE;
    }

    @Specialization(guards = "isNumber(value) || isNumberLong(value)", replaces = {"doInt", "doDouble"})
    protected final int doNumber(Object value) {
        if (useSharedBuffer) {
            GraalJSAccess.get(this).getSharedBuffer().putDouble(JSRuntime.doubleValue((Number) value));
        }
        return NUMBER_VALUE;
    }

    @Specialization
    protected static int doExternalObject(JSExternalObject value) {
        return EXTERNAL_OBJECT;
    }

    @Specialization
    protected static int doFunction(JSFunctionObject value) {
        return FUNCTION_OBJECT;
    }

    @Specialization
    protected static int doArray(JSArrayObject value) {
        return ARRAY_OBJECT;
    }

    @Specialization
    protected static int doDate(JSDateObject value) {
        return DATE_OBJECT;
    }

    @Specialization
    protected static int doRegExp(JSRegExpObject value) {
        return REGEXP_OBJECT;
    }

    @Specialization
    protected static int doMap(JSMapObject value) {
        return MAP_OBJECT;
    }

    @Specialization
    protected static int doSet(JSSetObject value) {
        return SET_OBJECT;
    }

    @Specialization
    protected static int doPromise(JSPromiseObject value) {
        return PROMISE_OBJECT;
    }

    @Specialization
    protected static int doProxy(JSProxyObject value) {
        return PROXY_OBJECT;
    }

    static final int TYPED_ARRAY_LIMIT = 3;

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"cachedArray == value.getArrayType()"}, limit = "TYPED_ARRAY_LIMIT")
    protected final int doArrayBufferView(JSTypedArrayObject value,
                    @Bind Node node,
                    @Cached("value.getArrayType()") TypedArray cachedArray,
                    @Cached("identifyType(cachedArray)") int cachedTypeInt,
                    @Cached @Shared ArrayBufferViewGetByteLengthNode getByteLengthNode) {
        if (useSharedBuffer) {
            ByteBuffer sharedBuffer = GraalJSAccess.get(this).getSharedBuffer();
            sharedBuffer.putInt(getByteLengthNode.executeInt(node, value, context));
            sharedBuffer.putInt(GraalJSAccess.arrayBufferViewByteOffset(context, value));
        }
        return cachedTypeInt;
    }

    @Specialization(replaces = "doArrayBufferView")
    protected final int doArrayBufferViewOverLimit(JSTypedArrayObject value,
                    @Cached @Shared ArrayBufferViewGetByteLengthNode getByteLengthNode) {
        if (useSharedBuffer) {
            ByteBuffer sharedBuffer = GraalJSAccess.get(this).getSharedBuffer();
            sharedBuffer.putInt(getByteLengthNode.executeInt(this, value, context));
            sharedBuffer.putInt(GraalJSAccess.arrayBufferViewByteOffset(context, value));
        }
        TypedArray array = value.getArrayType();
        return identifyType(array);
    }

    @Specialization
    protected final int doDataView(JSDataViewObject value) {
        if (useSharedBuffer) {
            ByteBuffer sharedBuffer = GraalJSAccess.get(this).getSharedBuffer();
            sharedBuffer.putInt(GraalJSAccess.arrayBufferViewByteLength(context, value));
            sharedBuffer.putInt(GraalJSAccess.arrayBufferViewByteOffset(context, value));
        }
        return DATA_VIEW_OBJECT;
    }

    protected int identifyType(ScriptArray array) {
        if (array instanceof TypedArray.DirectUint8Array) {
            return DIRECT_UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint8ClampedArray) {
            return DIRECT_UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt8Array) {
            return DIRECT_INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint16Array) {
            return DIRECT_UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt16Array) {
            return DIRECT_INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint32Array) {
            return DIRECT_UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt32Array) {
            return DIRECT_INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat32Array) {
            return DIRECT_FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat64Array) {
            return DIRECT_FLOAT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectBigInt64Array) {
            return DIRECT_BIGINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectBigUint64Array) {
            return DIRECT_BIGUINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint8Array) {
            return INTEROP_UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint8ClampedArray) {
            return INTEROP_UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt8Array) {
            return INTEROP_INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint16Array) {
            return INTEROP_UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt16Array) {
            return INTEROP_INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropUint32Array) {
            return INTEROP_UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropInt32Array) {
            return INTEROP_INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropFloat32Array) {
            return INTEROP_FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropFloat64Array) {
            return INTEROP_FLOAT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropBigInt64Array) {
            return INTEROP_BIGINT64ARRAY_OBJECT;
        } else if (array instanceof TypedArray.InteropBigUint64Array) {
            return INTEROP_BIGUINT64ARRAY_OBJECT;
        } else {
            return ARRAY_BUFFER_VIEW_OBJECT;
        }
    }

    @Specialization(guards = "isJSDirectArrayBuffer(value)")
    protected static int doDirectArrayBuffer(Object value) {
        return DIRECT_ARRAY_BUFFER_OBJECT;
    }

    @Specialization(guards = "isJSInteropArrayBuffer(value)")
    protected static int doInteropArrayBuffer(Object value) {
        return INTEROP_ARRAY_BUFFER_OBJECT;
    }

    @Specialization
    protected static int doOrdinaryObject(JSOrdinaryObject value) {
        return ORDINARY_OBJECT;
    }

    @Specialization(guards = {
                    "!isJSExternalObject(value)",
                    "!isJSFunction(value)",
                    "!isJSArray(value)",
                    "!isJSDate(value)",
                    "!isJSRegExp(value)",
                    "!isJSMap(value)",
                    "!isJSSet(value)",
                    "!isJSPromise(value)",
                    "!isJSProxy(value)",
                    "!isJSArrayBufferView(value)",
                    "!isJSDataView(value)",
                    "!isJSDirectArrayBuffer(value)"}, replaces = {"doOrdinaryObject"})
    protected static int doObject(JSObject value) {
        return ORDINARY_OBJECT;
    }

    @Specialization
    protected static int doSymbol(Symbol value) {
        return SYMBOL_VALUE;
    }

    @Specialization
    protected static int doBigInt(BigInt value) {
        return BIG_INT_VALUE;
    }

    @Specialization(guards = "isForeignObject(value)", limit = "5")
    protected final int doForeignObject(TruffleObject value,
                    @CachedLibrary("value") InteropLibrary interop) {
        if (interop.isExecutable(value) || interop.isInstantiable(value)) {
            return FUNCTION_OBJECT;
        } else if (interop.isNull(value)) {
            return NULL_VALUE;
        } else if (interop.isBoolean(value)) {
            try {
                return interop.asBoolean(value) ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE;
            } catch (UnsupportedMessageException e) {
                return doFallback(value);
            }
        } else if (interop.isString(value)) {
            return STRING_VALUE;
        } else if (interop.isNumber(value)) {
            return GraalJSAccess.get(this).valueTypeForeignNumber(value, interop, useSharedBuffer);
        } else {
            return ORDINARY_OBJECT;
        }
    }

    @Fallback
    protected static int doFallback(Object value) {
        GraalJSAccess.valueTypeError(value);
        return UNKNOWN_TYPE;
    }

}
