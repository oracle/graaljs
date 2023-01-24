/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIteratorFlattenable(obj, hint).
 */
@ImportStatic({Symbol.class, Strings.class, JSFunction.class})
public abstract class GetIteratorFlattenableNode extends JavaScriptBaseNode {

    protected final boolean async;
    protected final JSContext context;

    protected GetIteratorFlattenableNode(boolean async, JSContext context) {
        this.async = async;
        this.context = context;
    }

    public abstract IteratorRecord execute(Object iteratedObject);

    public static GetIteratorFlattenableNode create(boolean async, JSContext context) {
        return GetIteratorFlattenableNodeGen.create(async, context);
    }

    @Specialization(guards = "isObjectNode.executeBoolean(iteratedObject)", limit = "1")
    protected final IteratorRecord getIteratorFlattenable(Object iteratedObject,
                    @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObjectNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached(value = "create(SYMBOL_ASYNC_ITERATOR, context)") PropertyGetNode getAsyncIteratorMethodNode,
                    @Cached(value = "create(SYMBOL_ITERATOR, context)") PropertyGetNode getIteratorMethodNode,
                    @Cached(value = "createCall()") JSFunctionCallNode iteratorCallNode,
                    @Cached(value = "create(NEXT, context)") PropertyGetNode getNextMethodNode,
                    @Cached(value = "createSetHidden(ASYNC_FROM_SYNC_ITERATOR_KEY, context)") PropertySetNode setSyncIteratorRecordNode,
                    @Cached InlinedBranchProfile errorBranch) {
        boolean alreadyAsync = false;
        Object method = Undefined.instance;
        if (async) {
            method = getAsyncIteratorMethodNode.getValue(iteratedObject);
            alreadyAsync = true;
        }
        if (!async || !isCallableNode.executeBoolean(method)) {
            method = getIteratorMethodNode.getValue(iteratedObject);
            alreadyAsync = false;
        }
        Object iterator;
        if (!isCallableNode.executeBoolean(method)) {
            iterator = iteratedObject;
            alreadyAsync = true;
        } else {
            iterator = iteratorCallNode.executeCall(JSArguments.create(iteratedObject, method));
        }
        if (!(iterator instanceof JSObject)) {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAnObject(iterator, this);
        }
        Object nextMethod = getNextMethodNode.getValue(iterator);
        if (!isCallableNode.executeBoolean(nextMethod)) {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAFunction(nextMethod, this);
        }
        IteratorRecord iteratorRecord = IteratorRecord.create((JSObject) iterator, nextMethod, false);
        if (async && !alreadyAsync) {
            return createAsyncFromSyncIterator(iteratorRecord, getNextMethodNode, setSyncIteratorRecordNode);
        } else {
            return iteratorRecord;
        }
    }

    @Specialization(guards = "!isObjectNode.executeBoolean(obj)", limit = "1")
    protected final IteratorRecord unsupported(Object obj,
                    @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObjectNode) {
        throw Errors.createTypeErrorNotAnObject(obj, this);
    }

    private IteratorRecord createAsyncFromSyncIterator(IteratorRecord syncIteratorRecord, PropertyGetNode getNextMethodNode, PropertySetNode setSyncIteratorRecordNode) {
        JSObject asyncIterator = JSOrdinary.create(context, context.getAsyncFromSyncIteratorFactory(), getRealm());
        setSyncIteratorRecordNode.setValue(asyncIterator, syncIteratorRecord);
        Object nextMethod = getNextMethodNode.getValue(asyncIterator);
        return IteratorRecord.create(asyncIterator, nextMethod, false);
    }
}
