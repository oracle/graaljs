/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIterator(obj, hint = sync).
 */
@GenerateInline
@GenerateUncached
@ImportStatic(JSInteropUtil.class)
public abstract class GetIteratorNode extends JavaScriptBaseNode {

    protected GetIteratorNode() {
    }

    public final IteratorRecord execute(Node node, Object iteratedObject) {
        return execute(node, iteratedObject, Undefined.instance);
    }

    public abstract IteratorRecord execute(Node node, Object iteratedObject, Object method);

    @NeverDefault
    public static GetIteratorNode create() {
        return GetIteratorNodeGen.create();
    }

    public static GetIteratorNode getUncached() {
        return GetIteratorNodeGen.getUncached();
    }

    @Specialization
    protected static IteratorRecord doGetIterator(Node node, Object items, Object methodOpt,
                    @Cached(value = "createIteratorMethodNode()", uncached = "uncachedIteratorMethodNode()", inline = false) GetMethodNode getIteratorMethodNode,
                    @Cached(inline = false) IsCallableNode isCallableNode,
                    @Cached(value = "createCall()", uncached = "getUncachedCall()", inline = false) JSFunctionCallNode iteratorCallNode,
                    @Cached(inline = false) GetIteratorDirectNode getIteratorDirectNode,
                    @Cached InlinedBranchProfile errorBranch) {
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

        return getIterator(items, method, isCallableNode, iteratorCallNode, getIteratorDirectNode, errorBranch, node);
    }

    @InliningCutoff
    private static Object getIteratorMethodUncached(Object items) {
        Object obj = JSRuntime.toObject(items);
        JSDynamicObject target;
        if (obj instanceof JSDynamicObject) {
            target = (JSDynamicObject) obj;
        } else {
            target = ForeignObjectPrototypeNode.getUncached().execute(obj);
        }
        return JSObject.getOrDefault(target, Symbol.SYMBOL_ITERATOR, obj, Undefined.instance);
    }

    public static IteratorRecord getIterator(Object iteratedObject, Object method,
                    IsCallableNode isCallableNode,
                    JSFunctionCallNode methodCallNode,
                    GetIteratorDirectNode getIteratorDirectNode,
                    InlinedBranchProfile errorBranch,
                    Node node) {
        if (!isCallableNode.executeBoolean(method)) {
            errorBranch.enter(node);
            throw Errors.createTypeErrorNotIterable(iteratedObject, node);
        }
        return getIterator(iteratedObject, method, methodCallNode, getIteratorDirectNode);
    }

    public static IteratorRecord getIterator(Object iteratedObject, Object method,
                    JSFunctionCallNode methodCallNode,
                    GetIteratorDirectNode getIteratorDirectNode) {
        Object iterator = methodCallNode.executeCall(JSArguments.createZeroArg(iteratedObject, method));
        return getIteratorDirectNode.execute(iterator);
    }

    @NeverDefault
    GetMethodNode createIteratorMethodNode() {
        return GetMethodNode.create(getJSContext(), Symbol.SYMBOL_ITERATOR);
    }

    static GetMethodNode uncachedIteratorMethodNode() {
        return null;
    }

}
