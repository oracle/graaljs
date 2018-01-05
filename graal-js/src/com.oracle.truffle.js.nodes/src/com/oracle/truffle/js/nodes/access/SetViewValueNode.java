/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSDataView;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeChildren({@NodeChild("view"), @NodeChild("requestIndex"), @NodeChild("isLittleEndian"), @NodeChild("value")})
public abstract class SetViewValueNode extends JavaScriptNode {
    private final TypedArrayFactory factory;
    private final JSContext context;

    protected SetViewValueNode(JSContext context, String type) {
        this(context, GetViewValueNode.typedArrayFactoryFromType(type));
    }

    protected SetViewValueNode(JSContext context, TypedArrayFactory factory) {
        this.factory = factory;
        this.context = context;
    }

    @Specialization
    protected final Object doSet(Object view, Object requestIndex, boolean isLittleEndian, Object value,
                    @Cached("create()") JSToIndexNode toIndexNode,
                    @Cached("create()") JSToNumberNode valueToNumberNode,
                    @Cached("create()") BranchProfile errorBranch,
                    @Cached("createClassProfile()") ValueProfile typeProfile) {
        if (!JSDataView.isJSDataView(view)) {
            errorBranch.enter();
            throw Errors.createTypeErrorNotADataView();
        }
        DynamicObject dataView = (DynamicObject) view;
        DynamicObject buffer = JSDataView.getArrayBuffer(dataView);

        long getIndex = toIndexNode.executeLong(requestIndex);
        Number numberValue = valueToNumberNode.executeNumber(value);
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
        strategy.setBufferElement(buffer, bufferIndex, isLittleEndian, JSDataView.isJSDataView(view), numberValue);
        return Undefined.instance;
    }

    public static SetViewValueNode create(JSContext context, String type, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian, JavaScriptNode value) {
        return SetViewValueNodeGen.create(context, type, view, requestIndex, isLittleEndian, value);
    }

    abstract JavaScriptNode getView();

    abstract JavaScriptNode getRequestIndex();

    abstract JavaScriptNode getIsLittleEndian();

    abstract JavaScriptNode getValue();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return SetViewValueNodeGen.create(context, factory, cloneUninitialized(getView()), cloneUninitialized(getRequestIndex()), cloneUninitialized(getIsLittleEndian()),
                        cloneUninitialized(getValue()));
    }
}
