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

import java.lang.reflect.Array;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthReadNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@ImportStatic(JSInteropUtil.class)
abstract class GetLengthHelperNode extends JavaScriptBaseNode {

    private final JSContext context;

    @Child private JSToUInt32Node toUInt32Node;
    @Child private JSToLengthNode toLengthNode;

    /** Apply ES6 ToLength. */
    private final boolean toLength;

    GetLengthHelperNode(JSContext context) {
        this.context = context;
        this.toLength = context.getEcmaScriptVersion() >= 6;
    }

    public static GetLengthHelperNode create(JSContext context) {
        return GetLengthHelperNodeGen.create(context);
    }

    public abstract Object execute(TruffleObject value, boolean isArray);

    public final long executeLong(TruffleObject value, boolean isArray) {
        return toLengthLong(execute(value, isArray));
    }

    @Specialization(guards = "isArray", rewriteOn = UnexpectedResultException.class)
    public int getArrayLengthInt(DynamicObject target, boolean isArray,
                    @Cached("create()") ArrayLengthReadNode arrayLengthReadNode) throws UnexpectedResultException {
        return arrayLengthReadNode.executeInt(target, isArray);
    }

    @Specialization(guards = "isArray")
    public double getArrayLength(DynamicObject target, boolean isArray,
                    @Cached("create()") ArrayLengthReadNode arrayLengthReadNode) {
        return arrayLengthReadNode.executeDouble(target, isArray);
    }

    @Specialization(guards = "isJSJavaWrapper(target)")
    public int getJavaWrapperLength(DynamicObject target, @SuppressWarnings("unused") boolean isArray) {
        Object wrapped = JSJavaWrapper.getWrapped(target);
        if (wrapped.getClass().isArray()) {
            return Array.getLength(wrapped);
        } else if (wrapped instanceof List) {
            return Boundaries.listSize((List<?>) wrapped);
        } else {
            return 0;
        }
    }

    @Specialization(guards = "!isArray")
    public double getLengthDynamicObject(DynamicObject target, @SuppressWarnings("unused") boolean isArray,
                    @Cached("createLengthProperty()") PropertyNode getLengthPropertyNode) {
        return toLengthDouble(getLengthPropertyNode.executeWithTarget(target));
    }

    @Specialization(guards = "!isDynamicObject(target)")
    public double getLengthForeign(TruffleObject target, @SuppressWarnings("unused") boolean isArray,
                    @Cached("createGetSize()") Node getSizeNode) {
        return (int) JSRuntime.toLength(JSInteropNodeUtil.getSize(target, getSizeNode));
    }

    protected PropertyNode createLengthProperty() {
        return NodeFactory.getInstance(context).createProperty(context, null, JSArray.LENGTH);

    }

    private double toUInt32Double(Object target) {
        return JSRuntime.doubleValue((Number) getUInt32Node().execute(target));
    }

    private long toUInt32Long(Object target) {
        return JSRuntime.longValue((Number) getUInt32Node().execute(target));
    }

    private double toLengthDouble(Object target) {
        if (toLength) {
            return getToLengthNode().executeLong(target);
        } else {
            return toUInt32Double(target);
        }
    }

    private long toLengthLong(Object target) {
        if (toLength) {
            return getToLengthNode().executeLong(target);
        } else {
            return toUInt32Long(target);
        }
    }

    private JSToLengthNode getToLengthNode() {
        if (toLengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toLengthNode = insert(JSToLengthNode.create());
        }
        return toLengthNode;
    }

    private JSToUInt32Node getUInt32Node() {
        if (toUInt32Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toUInt32Node = insert(JSToUInt32Node.create());
        }
        return toUInt32Node;
    }
}
