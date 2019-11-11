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
