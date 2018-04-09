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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIterator(obj, hint = async).
 */
public abstract class GetAsyncIteratorNode extends GetIteratorNode {
    @Child private PropertySetNode setState;
    @Child private GetMethodNode getIteratorMethodNode;
    @Child private GetMethodNode getAsyncIteratorMethodNode;

    private final ConditionProfile asyncToSync = ConditionProfile.createBinaryProfile();

    protected GetAsyncIteratorNode(JSContext context, JavaScriptNode objectNode) {
        super(context, objectNode);
        this.setState = PropertySetNode.create(JSFunction.ASYNC_FROM_SYNC_ITERATOR_KEY, false, context, false);
        this.getAsyncIteratorMethodNode = GetMethodNode.create(context, null, Symbol.SYMBOL_ASYNC_ITERATOR);
    }

    @Override
    @Specialization(guards = {"!isForeignObject(iteratedObject)"})
    protected DynamicObject doGetIterator(Object iteratedObject,
                    @Cached("createCall()") JSFunctionCallNode methodCallNode,
                    @Cached("create()") IsObjectNode isObjectNode) {
        Object method = getAsyncIteratorMethodNode.executeWithTarget(iteratedObject);
        if (asyncToSync.profile(method == Undefined.instance)) {
            Object syncMethod = getIteratorMethodNode().executeWithTarget(iteratedObject);
            Object syncIterator = getIterator(iteratedObject, syncMethod, methodCallNode, isObjectNode, this);
            return createAsyncFromSyncIterator(syncIterator);
        }
        return getIterator(iteratedObject, method, methodCallNode, isObjectNode, this);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GetAsyncIteratorNodeGen.create(context, cloneUninitialized(objectNode));
    }

    private DynamicObject createAsyncFromSyncIterator(Object syncIterator) {
        if (!JSObject.isJSObject(syncIterator)) {
            throw Errors.createTypeErrorNotAnObject(syncIterator, this);
        }
        DynamicObject obj = JSObject.create(context.getRealm(), context.getRealm().getAsyncFromSyncIteratorPrototype(), JSUserObject.INSTANCE);
        setState.setValue(obj, syncIterator);
        return obj;
    }
}
