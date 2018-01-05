/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
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
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.JSExternalObject;

/**
 * Keep in sync with {@link GraalJSAccess#valueType}.
 */
@SuppressWarnings("unused")
@ImportStatic({JSExternalObject.class, JSRuntime.class, JSUserObject.class, JSMap.class, JSSet.class, JSPromise.class, JSProxy.class, JSObject.class, JSDataView.class})
abstract class ValueTypeNode extends JavaScriptBaseNode {
    private final GraalJSAccess graalAccess;
    private final JSContext context;
    private final boolean useSharedBuffer;

    ValueTypeNode(GraalJSAccess graalAccess, JSContext context, boolean useSharedBuffer) {
        this.graalAccess = graalAccess;
        this.context = context;
        this.useSharedBuffer = useSharedBuffer;
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
            graalAccess.getSharedBuffer().putDouble(((Number) value).doubleValue());
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

    @Fallback
    protected static int doFallback(Object value) {
        GraalJSAccess.valueTypeError(value);
        return UNKNOWN_TYPE;
    }
}
