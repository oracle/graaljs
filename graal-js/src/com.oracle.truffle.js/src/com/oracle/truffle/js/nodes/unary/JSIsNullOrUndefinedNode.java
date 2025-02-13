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
package com.oracle.truffle.js.nodes.unary;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This node optimizes the check whether the argument is null or undefined. Used from the
 * {@link JSEqualNode} for optimizing {@code a == undefined;} and {@code a == null;}
 *
 */
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class JSIsNullOrUndefinedNode extends JSUnaryNode {

    private final boolean isLeft;
    private final boolean isUndefined;

    protected JSIsNullOrUndefinedNode(JavaScriptNode operand, boolean isUndefined, boolean isLeft) {
        super(operand);
        this.isUndefined = isUndefined;
        this.isLeft = isLeft;
    }

    public abstract boolean executeBoolean(Object input);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryOperationTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryOperationTag.class)) {
            JSConstantNode constantNode = isUndefined ? JSConstantNode.createUndefined() : JSConstantNode.createNull();
            JavaScriptNode newOperand = cloneUninitialized(getOperand(), materializedTags);
            JavaScriptNode left = isLeft ? constantNode : newOperand;
            JavaScriptNode right = isLeft ? newOperand : constantNode;
            JavaScriptNode materialized = JSEqualNode.createUnoptimized(left, right);
            transferSourceSectionAddExpressionTag(this, constantNode);
            transferSourceSectionAndTags(this, materialized);
            return materialized;
        } else {
            return this;
        }
    }

    @Specialization(guards = "isJSNull(operand)")
    protected static boolean doNull(@SuppressWarnings("unused") Object operand) {
        return true;
    }

    @Specialization(guards = "isUndefined(operand)")
    protected static boolean doUndefined(@SuppressWarnings("unused") Object operand) {
        return true;
    }

    @Specialization
    protected static boolean doSymbol(@SuppressWarnings("unused") Symbol operand) {
        return false;
    }

    @Specialization
    protected static boolean doTString(@SuppressWarnings("unused") TruffleString operand) {
        return false;
    }

    @Specialization
    protected static boolean doSafeInteger(@SuppressWarnings("unused") SafeInteger operand) {
        return false;
    }

    @Specialization
    protected static boolean doBigInt(@SuppressWarnings("unused") BigInt operand) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected static boolean doJSObjectCached(Object object,
                    @Cached(value = "getClassIfJSObject(object)") Class<?> cachedClass) {
        assert !JSGuards.isNullOrUndefined(object);
        return false;
    }

    @Specialization(guards = {"isJSObject(object)"}, replaces = {"doJSObjectCached"})
    protected static boolean doJSObject(Object object) {
        assert !JSGuards.isNullOrUndefined(object);
        return false;
    }

    @Specialization(guards = {"!isJSDynamicObject(operand)"}, limit = "InteropLibraryLimit")
    protected boolean doJSValueOrForeign(Object operand,
                    @CachedLibrary("operand") InteropLibrary interop) {
        assert JSRuntime.isJSPrimitive(operand) || JSGuards.isForeignObjectOrNumber(operand) : operand;
        return interop.isNull(operand);
    }

    public static JSIsNullOrUndefinedNode createFromEquals(JavaScriptNode left, JavaScriptNode right) {
        assert isNullOrUndefined(left) || isNullOrUndefined(right);
        boolean isLeft = isNullOrUndefined(left);
        JavaScriptNode operand = isLeft ? right : left;
        JavaScriptNode constant = isLeft ? left : right;
        boolean isUndefined = constant instanceof JSConstantUndefinedNode;
        return JSIsNullOrUndefinedNodeGen.create(operand, isUndefined, isLeft);
    }

    @NeverDefault
    public static JSIsNullOrUndefinedNode create() {
        return JSIsNullOrUndefinedNodeGen.create(null, true, true);
    }

    private static boolean isNullOrUndefined(JavaScriptNode node) {
        return node instanceof JSConstantUndefinedNode || node instanceof JSConstantNullNode;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSIsNullOrUndefinedNodeGen.create(cloneUninitialized(getOperand(), materializedTags), isUndefined, isLeft);
    }
}
