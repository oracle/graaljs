/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSDataView;

public abstract class GetViewValueNode extends JavaScriptNode {
    private final TypedArrayFactory factory;
    private final JSContext context;
    @Child @Executed protected JavaScriptNode viewNode;
    @Child @Executed protected JavaScriptNode requestIndexNode;
    @Child @Executed protected JavaScriptNode isLittleEndianNode;

    protected GetViewValueNode(JSContext context, String type, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian) {
        this(context, typedArrayFactoryFromType(type), view, requestIndex, isLittleEndian);
    }

    protected GetViewValueNode(JSContext context, TypedArrayFactory factory, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian) {
        this.factory = factory;
        this.context = context;
        this.viewNode = view;
        this.requestIndexNode = requestIndex;
        this.isLittleEndianNode = isLittleEndian;
    }

    static TypedArrayFactory typedArrayFactoryFromType(String type) {
        for (TypedArrayFactory factory : TypedArray.FACTORIES) {
            if (factory.getName().startsWith(type)) {
                return factory;
            }
        }
        throw new IllegalArgumentException(type);
    }

    @Specialization
    protected final Object doGet(Object view, Object requestIndex, boolean isLittleEndian,
                    @Cached("create()") JSToIndexNode toIndexNode,
                    @Cached("create()") BranchProfile errorBranch,
                    @Cached("createClassProfile()") ValueProfile typeProfile) {
        if (!JSDataView.isJSDataView(view)) {
            errorBranch.enter();
            throw Errors.createTypeErrorNotADataView();
        }
        DynamicObject dataView = (DynamicObject) view;
        DynamicObject buffer = JSDataView.getArrayBuffer(dataView);
        long getIndex = toIndexNode.executeLong(requestIndex);

        if (!context.getTypedArrayNotDetachedAssumption().isValid()) {
            if (JSArrayBuffer.isDetachedBuffer(buffer)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }

        int viewLength = JSDataView.typedArrayGetLength(dataView);
        int elementSize = factory.bytesPerElement();
        if (getIndex + elementSize > viewLength) {
            errorBranch.enter();
            throw Errors.createRangeError("index + elementSize > viewLength");
        }
        int viewOffset = JSDataView.typedArrayGetOffset(dataView);

        assert getIndex + viewOffset <= Integer.MAX_VALUE;
        int bufferIndex = (int) (getIndex + viewOffset);
        TypedArray strategy = typeProfile.profile(factory.createArrayType(JSArrayBuffer.isJSDirectOrSharedArrayBuffer(buffer), true));
        return strategy.getBufferElement(buffer, bufferIndex, isLittleEndian, JSDataView.isJSDataView(view));
    }

    public static GetViewValueNode create(JSContext context, String type, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian) {
        return GetViewValueNodeGen.create(context, type, view, requestIndex, isLittleEndian);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GetViewValueNodeGen.create(context, factory, cloneUninitialized(viewNode), cloneUninitialized(requestIndexNode), cloneUninitialized(isLittleEndianNode));
    }
}
