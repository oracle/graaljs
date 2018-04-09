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
package com.oracle.truffle.trufflenode.node;

import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_BUFFER_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_FALSE;
import static com.oracle.truffle.trufflenode.ValueType.BOOLEAN_VALUE_TRUE;
import static com.oracle.truffle.trufflenode.ValueType.DATE_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.DATA_VIEW_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.EXTERNAL_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FLOAT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FLOAT64ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.FUNCTION_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.INT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.LAZY_STRING_VALUE;
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
import static com.oracle.truffle.trufflenode.ValueType.UINT16ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT32ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT8ARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UINT8CLAMPEDARRAY_OBJECT;
import static com.oracle.truffle.trufflenode.ValueType.UNDEFINED_VALUE;
import static com.oracle.truffle.trufflenode.ValueType.UNKNOWN_TYPE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.JSExternalObject;

/**
 * Keep in sync with {@link GraalJSAccess#valueType}.
 */
@SuppressWarnings("unused")
@ImportStatic({JSExternalObject.class, JSRuntime.class, JSUserObject.class, JSMap.class, JSSet.class, JSPromise.class, JSProxy.class, JSObject.class, JSDataView.class, JSInteropUtil.class})
abstract class ValueTypeNode extends JavaScriptBaseNode {
    protected final GraalJSAccess graalAccess;
    protected final JSContext context;
    protected final boolean useSharedBuffer;

    ValueTypeNode(GraalJSAccess graalAccess, JSContext context, boolean useSharedBuffer) {
        this.graalAccess = graalAccess;
        this.context = context;
        this.useSharedBuffer = useSharedBuffer;
    }

    public static ValueTypeNode create(GraalJSAccess graalAccess, JSContext context, boolean useSharedBuffer) {
        return ValueTypeNodeGen.create(graalAccess, context, useSharedBuffer);
    }

    protected abstract int executeInt(Object value);

    protected JSContext getContext() {
        return context;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static int doUndefined(DynamicObject value) {
        return UNDEFINED_VALUE;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(DynamicObject value) {
        return NULL_VALUE;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return value ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE;
    }

    @Specialization
    protected static int doString(String value) {
        return STRING_VALUE;
    }

    @Specialization
    protected final int doInt(int value) {
        return doDouble(value);
    }

    @Specialization
    protected final int doDouble(double value) {
        if (useSharedBuffer) {
            graalAccess.getSharedBuffer().putDouble(value);
        }
        return NUMBER_VALUE;
    }

    @Specialization(guards = "isNumber(value)", replaces = {"doInt", "doDouble"})
    protected final int doNumber(Object value) {
        if (useSharedBuffer) {
            graalAccess.getSharedBuffer().putDouble(JSRuntime.doubleValue((Number) value));
        }
        return NUMBER_VALUE;
    }

    @Specialization(guards = "isJSExternalObject(value)")
    protected static int doExternalObject(DynamicObject value) {
        return EXTERNAL_OBJECT;
    }

    @Specialization(guards = "isJSFunction(value)")
    protected static int doFunction(DynamicObject value) {
        return FUNCTION_OBJECT;
    }

    @Specialization(guards = "isJSArray(value)")
    protected static int doArray(DynamicObject value) {
        return ARRAY_OBJECT;
    }

    @Specialization(guards = "isJSDate(value)")
    protected static int doDate(DynamicObject value) {
        return DATE_OBJECT;
    }

    @Specialization(guards = "isJSRegExp(value)")
    protected static int doRegExp(DynamicObject value) {
        return REGEXP_OBJECT;
    }

    @Specialization(guards = "isJSMap(value)")
    protected static int doMap(DynamicObject value) {
        return MAP_OBJECT;
    }

    @Specialization(guards = "isJSSet(value)")
    protected static int doSet(DynamicObject value) {
        return SET_OBJECT;
    }

    @Specialization(guards = "isJSPromise(value)")
    protected static int doPromise(DynamicObject value) {
        return PROMISE_OBJECT;
    }

    @Specialization(guards = "isProxy(value)")
    protected static int doProxy(DynamicObject value) {
        return PROXY_OBJECT;
    }

    @Specialization(guards = {"isJSArrayBufferView(value)", "cachedArray == getScriptArray(value)"})
    protected final int doArrayBufferView(DynamicObject value,
                    @Cached("getScriptArray(value)") ScriptArray cachedArray,
                    @Cached("identifyType(cachedArray)") int cachedTypeInt,
                    @Cached("create(getContext())") ArrayBufferViewGetByteLengthNode getByteLengthNode) {
        if (useSharedBuffer) {
            graalAccess.getSharedBuffer().putInt(getByteLengthNode.executeInt(value));
            graalAccess.getSharedBuffer().putInt(graalAccess.arrayBufferViewByteOffset(context, value));
        }
        return cachedTypeInt;
    }

    @Specialization(guards = {"isJSArrayBufferView(value)"}, replaces = "doArrayBufferView")
    protected final int doArrayBufferViewOverLimit(DynamicObject value,
                    @Cached("create(getContext())") ArrayBufferViewGetByteLengthNode getByteLengthNode) {
        if (useSharedBuffer) {
            graalAccess.getSharedBuffer().putInt(getByteLengthNode.executeInt(value));
            graalAccess.getSharedBuffer().putInt(graalAccess.arrayBufferViewByteOffset(context, value));
        }
        ScriptArray array = getScriptArray(value);
        return identifyType(array);
    }

    @Specialization(guards = {"isJSDataView(value)"})
    protected final int doDataView(DynamicObject value) {
        if (useSharedBuffer) {
            graalAccess.getSharedBuffer().putInt(graalAccess.arrayBufferViewByteLength(context, value));
            graalAccess.getSharedBuffer().putInt(graalAccess.arrayBufferViewByteOffset(context, value));
        }
        return DATA_VIEW_OBJECT;
    }

    protected ScriptArray getScriptArray(DynamicObject obj) {
        boolean condition = JSArrayBufferView.isJSArrayBufferView(obj);
        return JSObject.getArray(obj, condition);
    }

    protected int identifyType(ScriptArray array) {
        if (array instanceof TypedArray.DirectUint8Array) {
            return UINT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint8ClampedArray) {
            return UINT8CLAMPEDARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt8Array) {
            return INT8ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint16Array) {
            return UINT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt16Array) {
            return INT16ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectUint32Array) {
            return UINT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectInt32Array) {
            return INT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat32Array) {
            return FLOAT32ARRAY_OBJECT;
        } else if (array instanceof TypedArray.DirectFloat64Array) {
            return FLOAT64ARRAY_OBJECT;
        } else {
            return ARRAY_BUFFER_VIEW_OBJECT;
        }
    }

    @Specialization(guards = "isJSDirectArrayBuffer(value)")
    protected static int doArrayBuffer(DynamicObject value) {
        return ARRAY_BUFFER_OBJECT;
    }

    @Specialization(guards = {"isJSUserObject(value)"})
    protected static int doOrdinaryObject(DynamicObject value) {
        return ORDINARY_OBJECT;
    }

    @Specialization(guards = {
                    "isJSObject(value)",
                    "!isUndefined(value)",
                    "!isJSNull(value)",
                    "!isJSExternalObject(value)",
                    "!isJSFunction(value)",
                    "!isJSArray(value)",
                    "!isJSDate(value)",
                    "!isJSRegExp(value)",
                    "!isJSMap(value)",
                    "!isJSSet(value)",
                    "!isJSPromise(value)",
                    "!isProxy(value)",
                    "!isJSArrayBufferView(value)",
                    "!isJSDataView(value)",
                    "!isJSDirectArrayBuffer(value)"}, replaces = {"doOrdinaryObject"})
    protected static int doObject(DynamicObject value) {
        return ORDINARY_OBJECT;
    }

    @Specialization(guards = "isLazyString(value)")
    protected static int doCharSequence(CharSequence value) {
        return LAZY_STRING_VALUE;
    }

    @Specialization
    protected static int doSymbol(Symbol value) {
        return SYMBOL_VALUE;
    }

    @Specialization(guards = "isForeignObject(value)")
    protected static int doForeignObject(TruffleObject value,
                    @Cached("createIsExecutable()") Node isExecutable,
                    @Cached("createIsBoxed()") Node isBoxed,
                    @Cached("createUnbox()") Node unboxNode,
                    @Cached("create(graalAccess, context, useSharedBuffer)") ValueTypeNode unboxedValueType,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        if (ForeignAccess.sendIsBoxed(isBoxed, value)) {
            Object obj = foreignConvertNode.executeWithTarget(JSInteropNodeUtil.unbox(value, unboxNode));
            return unboxedValueType.executeInt(obj);
        } else if (ForeignAccess.sendIsExecutable(isExecutable, value)) {
            return FUNCTION_OBJECT;
        } else {
            return ORDINARY_OBJECT;
        }
    }

    @Fallback
    protected static int doFallback(Object value) {
        if (JSTruffleOptions.NashornJavaInterop) {
            return ORDINARY_OBJECT;
        }
        GraalJSAccess.valueTypeError(value);
        return UNKNOWN_TYPE;
    }

}
