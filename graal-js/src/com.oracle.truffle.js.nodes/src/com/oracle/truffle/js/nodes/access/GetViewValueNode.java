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
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSDataView;

public abstract class GetViewValueNode extends JavaScriptNode {
    private final TypedArrayFactory factory;
    private final JSContext context;
    @Child @Executed protected JavaScriptNode viewNode;
    @Child @Executed protected JavaScriptNode requestIndexNode;
    @Child @Executed protected JavaScriptNode isLittleEndianNode;
    @Child private JSToBooleanNode toBooleanNode;

    protected GetViewValueNode(JSContext context, String type, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian) {
        this(context, typedArrayFactoryFromType(type), view, requestIndex, isLittleEndian);
    }

    protected GetViewValueNode(JSContext context, TypedArrayFactory factory, JavaScriptNode view, JavaScriptNode requestIndex, JavaScriptNode isLittleEndian) {
        this.factory = factory;
        this.context = context;
        this.viewNode = view;
        this.requestIndexNode = requestIndex;
        this.isLittleEndianNode = isLittleEndian;
        this.toBooleanNode = factory.bytesPerElement() == 1 ? null : JSToBooleanNode.create();
    }

    static TypedArrayFactory typedArrayFactoryFromType(String type) {
        for (TypedArrayFactory factory : TypedArray.factories()) {
            if (factory.getName().startsWith(type)) {
                return factory;
            }
        }
        throw new IllegalArgumentException(type);
    }

    public abstract Object execute(Object dataView, Object requestIndex, Object littleEndian);

    @Specialization
    protected final Object doGet(Object view, Object requestIndex, Object littleEndian,
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
        boolean isLittleEndian = factory.bytesPerElement() == 1 ? true : toBooleanNode.executeBoolean(littleEndian);

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
