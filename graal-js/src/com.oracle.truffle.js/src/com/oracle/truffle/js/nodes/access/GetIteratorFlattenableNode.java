/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIteratorFlattenable(obj, hint).
 */
@ImportStatic({Symbol.class, Strings.class})
public abstract class GetIteratorFlattenableNode extends JavaScriptBaseNode {

    protected final boolean rejectStrings;
    protected final boolean async;
    protected final JSContext context;

    protected GetIteratorFlattenableNode(boolean rejectStrings, boolean async, JSContext context) {
        this.rejectStrings = rejectStrings;
        this.async = async;
        this.context = context;
    }

    public abstract IteratorRecord execute(Object iteratedObject);

    public static GetIteratorFlattenableNode create(boolean rejectStrings, boolean async, JSContext context) {
        return GetIteratorFlattenableNodeGen.create(rejectStrings, async, context);
    }

    @Specialization
    protected final IteratorRecord getIteratorFlattenable(Object iteratedObject,
                    @Cached IsObjectNode isObjectNode,
                    @Cached IsObjectNode isIteratorObjectNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached(value = "create(context, SYMBOL_ASYNC_ITERATOR)") GetMethodNode getAsyncIteratorMethodNode,
                    @Cached(value = "create(context, SYMBOL_ITERATOR)") GetMethodNode getIteratorMethodNode,
                    @Cached(value = "createCall()") JSFunctionCallNode iteratorCallNode,
                    @Cached(value = "create(NEXT, context)") PropertyGetNode getNextMethodNode,
                    @Cached CreateAsyncFromSyncIteratorNode createAsyncFromSyncIteratorNode,
                    @Cached InlinedBranchProfile errorBranch) {

        if (!isObjectNode.executeBoolean(iteratedObject)) {
            if (rejectStrings || !(iteratedObject instanceof TruffleString)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotAnObject(iteratedObject, this);
            }
        }

        boolean alreadyAsync = false;
        Object method = Undefined.instance;
        if (async) {
            method = getAsyncIteratorMethodNode.executeWithTarget(iteratedObject);
            alreadyAsync = true;
        }
        if (!async || !isCallableNode.executeBoolean(method)) {
            method = getIteratorMethodNode.executeWithTarget(iteratedObject);
            alreadyAsync = false;
        }
        Object iterator;
        if (method == Undefined.instance) {
            iterator = iteratedObject;
            alreadyAsync = true;
        } else {
            iterator = iteratorCallNode.executeCall(JSArguments.create(iteratedObject, method));
        }
        if (!isIteratorObjectNode.executeBoolean(iterator)) {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAnObject(iterator, this);
        }
        Object nextMethod = getNextMethodNode.getValue(iterator);
        IteratorRecord iteratorRecord = IteratorRecord.create(iterator, nextMethod, false);
        if (async && !alreadyAsync) {
            return createAsyncFromSyncIteratorNode.execute(this, iteratorRecord);
        } else {
            return iteratorRecord;
        }
    }
}
