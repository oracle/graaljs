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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsArrayNodeGen.IsArrayWrappedNodeGen;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Non-standard IsArray. Checks for array exotic objects.
 *
 * @see JSIsArrayNode
 */
@ImportStatic(value = JSGuards.class)
public abstract class IsArrayNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;

    private final boolean onlyArray;
    private final boolean fastArray;
    private final boolean fastAndTypedArray;

    protected IsArrayNode(boolean onlyArray, boolean fastArray, boolean fastAndTypedArray) {
        this.onlyArray = onlyArray;
        this.fastArray = fastArray;
        this.fastAndTypedArray = fastAndTypedArray;
    }

    public abstract boolean execute(TruffleObject operand);

    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static boolean doIsArrayShape(DynamicObject object, //
                    @Cached("object.getShape()") Shape cachedShape, //
                    @Cached("isArray(object)") boolean cachedResult) {
        // (aw) must do the shape check again to preserve the unsafe condition,
        // otherwise we could just do: return cachedResult;
        return cachedResult && cachedShape.check(object);
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doIsArrayShape", guards = {"cachedClass != null", "cachedClass.isInstance(object)"}, limit = "MAX_JSCLASS_COUNT")
    protected static boolean doIsArrayJSClass(DynamicObject object,
                    @Cached("isArray(object)") boolean cachedResult,
                    @Cached("getJSClassChecked(object)") JSClass cachedClass) {
        return cachedResult;
    }

    @Specialization(replaces = "doIsArrayJSClass")
    protected final boolean isArray(DynamicObject object) {
        if (fastAndTypedArray) {
            return JSArray.isJSFastArray(object) || JSArgumentsObject.isJSFastArgumentsObject(object) || JSArrayBufferView.isJSArrayBufferView(object);
        } else if (fastArray) {
            return JSArray.isJSFastArray(object);
        } else if (onlyArray) {
            return JSArray.isJSArray(object);
        } else {
            return JSObject.hasArray(object);
        }
    }

    @Specialization(guards = "!isDynamicObject(object)")
    protected static boolean isNotDynamicObject(@SuppressWarnings("unused") TruffleObject object) {
        return false;
    }

    public static IsArrayNode createIsAnyArray() {
        return IsArrayNodeGen.create(false, false, false);
    }

    public static IsArrayNode createIsArray() {
        return IsArrayNodeGen.create(true, false, false);
    }

    public static IsArrayNode createIsFastArray() {
        return IsArrayNodeGen.create(true, true, false);
    }

    public static IsArrayNode createIsFastOrTypedArray() {
        return IsArrayNodeGen.create(true, true, true);
    }

    /**
     * Wrapper of @link{IsArrayNode} when you really need a JavaScriptNode. IsArrayNode is a
     * JavaScriptBaseNode for footprint reasons.
     */
    public abstract static class IsArrayWrappedNode extends JSUnaryNode {

        @Child private IsArrayNode isArrayNode;

        protected IsArrayWrappedNode(JavaScriptNode operandNode, IsArrayNode isArrayNode) {
            super(operandNode);
            this.isArrayNode = isArrayNode;
        }

        @Specialization
        protected boolean doObject(Object operand) {
            if (JSObject.isDynamicObject(operand)) {
                return isArrayNode.execute((DynamicObject) operand);
            } else {
                return false;
            }
        }

        public static JavaScriptNode createIsArray(JavaScriptNode operand) {
            return IsArrayWrappedNodeGen.create(operand, IsArrayNode.createIsArray());
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return createIsArray(cloneUninitialized(getOperand()));
        }
    }
}
