/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIterator(obj, hint = sync).
 */
@GenerateUncached
@ImportStatic(JSInteropUtil.class)
public abstract class GetIteratorBaseNode extends JavaScriptBaseNode {

    protected GetIteratorBaseNode() {
    }

    public final IteratorRecord execute(Object iteratedObject) {
        return execute(iteratedObject, Undefined.instance);
    }

    public abstract IteratorRecord execute(Object iteratedObject, Object method);

    public static GetIteratorBaseNode create() {
        return GetIteratorBaseNodeGen.create();
    }

    public static GetIteratorBaseNode getUncached() {
        return GetIteratorBaseNodeGen.getUncached();
    }

    @Specialization
    protected final IteratorRecord doGetIterator(Object items, Object methodOpt,
                    @Cached(value = "createIteratorMethodNode()", uncached = "uncachedIteratorMethodNode()") GetMethodNode getIteratorMethodNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode iteratorCallNode,
                    @Cached IsJSObjectNode isObjectNode,
                    @Cached(value = "createNextMethodNode()", uncached = "uncachedNextMethodNode()") PropertyGetNode getNextMethodNode,
                    @Cached BranchProfile errorBranch) {
        Object method;
        if (methodOpt != Undefined.instance) {
            method = methodOpt;
        } else {
            if (getIteratorMethodNode == null) {
                method = getIteratorMethodUncached(items);
            } else {
                method = getIteratorMethodNode.executeWithTarget(items);
            }
        }

        return getIterator(items, method, isCallableNode, iteratorCallNode, isObjectNode, getNextMethodNode, errorBranch, this);
    }

    private Object getIteratorMethodUncached(Object items) {
        Object method;
        Object obj = JSRuntime.toObject(getLanguage().getJSContext(), items);
        if (JSRuntime.isForeignObject(obj)) {
            obj = ForeignObjectPrototypeNode.getUncached().execute(obj);
        }
        if (obj instanceof JSDynamicObject) {
            method = JSObject.get((JSDynamicObject) obj, Symbol.SYMBOL_ITERATOR);
        } else {
            method = Undefined.instance;
        }
        return method;
    }

    public static IteratorRecord getIterator(Object iteratedObject, Object method,
                    IsCallableNode isCallableNode,
                    JSFunctionCallNode methodCallNode,
                    IsJSObjectNode isObjectNode,
                    PropertyGetNode getNextMethodNode,
                    BranchProfile errorBranch,
                    JavaScriptBaseNode origin) {
        if (!isCallableNode.executeBoolean(method)) {
            errorBranch.enter();
            throw Errors.createTypeErrorNotIterable(iteratedObject, origin);
        }
        return getIterator(iteratedObject, method, methodCallNode, isObjectNode, getNextMethodNode, origin);
    }

    public static IteratorRecord getIterator(Object iteratedObject, Object method,
                    JSFunctionCallNode methodCallNode,
                    IsJSObjectNode isObjectNode,
                    PropertyGetNode getNextMethodNode,
                    JavaScriptBaseNode origin) {
        Object iterator = methodCallNode.executeCall(JSArguments.createZeroArg(iteratedObject, method));
        if (isObjectNode.executeBoolean(iterator)) {
            JSDynamicObject jsIterator = (JSDynamicObject) iterator;
            Object nextMethod = getNextMethodNode != null ? getNextMethodNode.getValue(jsIterator) : JSObject.get(jsIterator, Strings.NEXT);
            return IteratorRecord.create(jsIterator, nextMethod, false);
        } else {
            throw Errors.createTypeErrorNotAnObject(iterator, origin);
        }
    }

    GetMethodNode createIteratorMethodNode() {
        return GetMethodNode.create(getLanguage().getJSContext(), Symbol.SYMBOL_ITERATOR);
    }

    static GetMethodNode uncachedIteratorMethodNode() {
        return null;
    }

    PropertyGetNode createNextMethodNode() {
        return PropertyGetNode.create(Strings.NEXT, getLanguage().getJSContext());
    }

    static PropertyGetNode uncachedNextMethodNode() {
        return null;
    }
}
