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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
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
public abstract class GetIteratorNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode objectNode;
    @Child private GetMethodNode getIteratorMethodNode;

    protected final JSContext context;

    protected GetIteratorNode(JSContext context, JavaScriptNode objectNode) {
        this.context = context;
        this.objectNode = objectNode;
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
            throw Errors.createTypeErrorNotAnObject(iterator, origin);
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

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GetIteratorNodeGen.create(getContext(), cloneUninitialized(objectNode));
    }

    protected GetMethodNode getIteratorMethodNode() {
        if (getIteratorMethodNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getIteratorMethodNode = insert(GetMethodNode.create(context, null, Symbol.SYMBOL_ITERATOR));
        }
        return getIteratorMethodNode;
    }
}
