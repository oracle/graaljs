/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.simd;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.SIMDType;

public final class SIMDCastNode extends JavaScriptBaseNode {

    private final SIMDType simdContext;

    @Child private JSToNumberNode toNumberNode;
    @Child private JSToInt32Node toInt32Node;
    private final ConditionProfile p1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile p2 = ConditionProfile.createBinaryProfile();

    private SIMDCastNode(SIMDType simdContext) {
        this.simdContext = simdContext;
    }

    public static SIMDCastNode create(SIMDType simdContext) {
        return new SIMDCastNode(simdContext);
    }

    public Object execute(Object o) {
        Number n = o instanceof Number ? (Number) o : getToNumberNode().executeNumber(o);
        return executeNumber(n);
    }

    private Object executeNumber(Number o) {
        if (simdContext.getFactory() == SIMDType.INT32X4_FACTORY) {
            if (p1.profile(o instanceof Float)) {
                return getToInt32Node().executeInt((double) (float) o);
            }
            return getToInt32Node().executeInt(o);
        } else if (simdContext.getFactory() == SIMDType.INT16X8_FACTORY) {
            if (p1.profile(o instanceof Short)) {
                return (int) (short) o;
            }
            if (p2.profile(o instanceof Float)) {
                return JSRuntime.toInt16((double) (float) o);
            }
            return JSRuntime.toInt16(o);
        } else if (simdContext.getFactory() == SIMDType.INT8X16_FACTORY) {
            if (p1.profile(o instanceof Byte)) {
                return (int) (byte) o;
            }
            if (p2.profile(o instanceof Float)) {
                return JSRuntime.toInt8((float) o);
            }
            return JSRuntime.toInt8(o);
        } else if (simdContext.getFactory() == SIMDType.UINT32X4_FACTORY) {
            if (p1.profile(o instanceof Float)) {
                return (int) JSRuntime.toUInt32((float) o);
            }
            return (int) JSRuntime.toUInt32(o);
        } else if (simdContext.getFactory() == SIMDType.UINT16X8_FACTORY) {
            if (p1.profile(o instanceof Short)) {
                return (int) (short) o;
            }
            if (p2.profile(o instanceof Float)) {
                return (int) JSRuntime.toUInt16((double) (float) o);
            }
            return JSRuntime.toUInt16(o);
        } else if (simdContext.getFactory() == SIMDType.UINT8X16_FACTORY) {
            if (p1.profile(o instanceof Byte)) {
                return (int) (byte) o;
            }
            if (p2.profile(o instanceof Float)) {
                return (int) JSRuntime.toUInt8((float) o);
            }
            return JSRuntime.toUInt8(o);
        } else if (simdContext.getFactory() == SIMDType.FLOAT32X4_FACTORY) {
            if (p1.profile(o instanceof Float)) {
                return o;
            }
            if (p2.profile(o instanceof Double)) {
                return (float) (double) o;
            }
            return JSRuntime.floatValueVirtual(o);
        } else if (simdContext.getFactory() == SIMDType.BOOL32X4_FACTORY || simdContext.getFactory() == SIMDType.BOOL16X8_FACTORY || simdContext.getFactory() == SIMDType.BOOL8X16_FACTORY) {
            return JSRuntime.toBoolean(o);
        } else {
            return simdContext.cast(o);
        }
    }

    private JSToInt32Node getToInt32Node() {
        if (toInt32Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toInt32Node = insert(JSToInt32Node.create());
        }
        return toInt32Node;
    }

    protected JSToNumberNode getToNumberNode() {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode;
    }
}
