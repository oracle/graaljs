/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * GetIterator(obj, hint = sync).
 */
@ImportStatic(JSInteropUtil.class)
@NodeChild(value = "iteratedObject", type = JavaScriptNode.class)
public abstract class GetIteratorNode extends JavaScriptNode {
    @Child private GetMethodNode getIteratorMethodNode;

    protected final JSContext context;

    protected GetIteratorNode(JSContext context) {
        this.context = context;
    }

    public static GetIteratorNode create(JSContext context) {
        return create(context, null);
    }

    public static GetIteratorNode create(JSContext context, JavaScriptNode iteratedObject) {
        return GetIteratorNodeGen.create(context, iteratedObject);
    }

    public static GetIteratorNode createAsync(JSContext context, JavaScriptNode iteratedObject) {
        return GetAsyncIteratorNodeGen.create(context, iteratedObject);
    }

    protected JSContext getContext() {
        return context;
    }

    @Specialization(guards = {"!isForeignObject(iteratedObject)"})
    protected DynamicObject doGetIterator(Object iteratedObject,
                    @Cached("createCall()") JSFunctionCallNode methodCallNode,
                    @Cached("create()") IsObjectNode isObjectNode) {
        Object method = getIteratorMethodNode().executeWithTarget(iteratedObject);
        return getIterator(iteratedObject, method, methodCallNode, isObjectNode, this);
    }

    public static DynamicObject getIterator(Object iteratedObject, Object method, JSFunctionCallNode methodCallNode, IsObjectNode isObjectNode, JavaScriptBaseNode origin) {
        Object iterator = methodCallNode.executeCall(JSArguments.createZeroArg(iteratedObject, method));
        if (isObjectNode.executeBoolean(iterator)) {
            return (DynamicObject) iterator;
        } else {
            throw Errors.createNotAnObjectError(origin);
        }
    }

    @Specialization(guards = "isForeignObject(iteratedObject)")
    protected DynamicObject doGetIteratorWithForeignObject(TruffleObject iteratedObject,
                    @Cached("createEnumerateValues()") EnumerateNode enumerateNode,
                    @Cached("createIsBoxed()") Node isBoxedNode,
                    @Cached("create()") JSUnboxOrGetNode unboxNode,
                    @Cached("create(getContext())") GetIteratorNode getIteratorNode) {
        if (ForeignAccess.sendIsBoxed(isBoxedNode, iteratedObject)) {
            Object unboxed = unboxNode.executeWithTarget(iteratedObject);
            return getIteratorNode.execute(unboxed);
        } else {
            return enumerateNode.execute(iteratedObject);
        }
    }

    protected EnumerateNode createEnumerateValues() {
        return EnumerateNode.create(getContext(), null, true);
    }

    @Override
    public abstract DynamicObject execute(VirtualFrame frame);

    public abstract DynamicObject execute(Object iteratedObject);

    abstract JavaScriptNode getIteratedObject();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GetIteratorNodeGen.create(getContext(), cloneUninitialized(getIteratedObject()));
    }

    protected GetMethodNode getIteratorMethodNode() {
        if (getIteratorMethodNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getIteratorMethodNode = insert(GetMethodNode.create(context, null, Symbol.SYMBOL_ITERATOR));
        }
        return getIteratorMethodNode;
    }
}
