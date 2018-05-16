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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNodeGen.IsObjectWrappedNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Checks whether the argument is an object.
 */
@ImportStatic(JSObject.class)
public abstract class IsObjectNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;
    private final boolean includeNullUndefined;

    protected IsObjectNode(boolean includeNullUndefined) {
        this.includeNullUndefined = includeNullUndefined;
    }

    public abstract boolean executeBoolean(Object obj);

    @SuppressWarnings("unused")
    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static boolean isObjectShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("guardIsJSObject(object)") boolean cachedResult) {
        return cachedResult;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedClass != null", "cachedClass.isInstance(object)"}, replaces = "isObjectShape", limit = "MAX_JSCLASS_COUNT")
    protected static boolean isObjectJSClass(DynamicObject object,
                    @Cached("getJSClassChecked(object)") JSClass cachedClass,
                    @Cached("guardIsJSObject(object)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(replaces = {"isObjectShape", "isObjectJSClass"})
    protected static boolean isObject(Object object,
                    @Cached("createBinaryProfile()") ConditionProfile resultProfile) {
        return resultProfile.profile(JSRuntime.isObject(object));
    }

    public static IsObjectNode create() {
        return IsObjectNodeGen.create(false);
    }

    public static IsObjectNode createIncludeNullUndefined() {
        return IsObjectNodeGen.create(true);
    }

    // name-clash with JSObject.isJSObject. Different behavior around null/undefined.
    protected boolean guardIsJSObject(DynamicObject obj) {
        if (includeNullUndefined) {
            return JSObject.isJSObject(obj);
        } else {
            return JSGuards.isJSObject(obj);
        }
    }

    /**
     * Wrapper of @link{IsObjectNode} when you really need a JavaScriptNode. IsObjectNode is a
     * JavaScriptBaseNode for footprint reasons.
     */
    public abstract static class IsObjectWrappedNode extends JSUnaryNode {

        @Child private IsObjectNode isObjectNode;

        protected IsObjectWrappedNode(JavaScriptNode operand) {
            super(operand);
            this.isObjectNode = IsObjectNode.create();
        }

        @Specialization
        protected boolean doObject(Object operand) {
            return isObjectNode.executeBoolean(operand);
        }

        public static JavaScriptNode create(JavaScriptNode operand) {
            return IsObjectWrappedNodeGen.create(operand);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return IsObjectWrappedNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}
