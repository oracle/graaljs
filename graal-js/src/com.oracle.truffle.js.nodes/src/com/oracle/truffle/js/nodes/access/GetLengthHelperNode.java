/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
