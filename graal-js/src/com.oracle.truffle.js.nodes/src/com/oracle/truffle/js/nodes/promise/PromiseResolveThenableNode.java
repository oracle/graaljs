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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

public class PromiseResolveThenableNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private CreateResolvingFunctionNode createResolvingFunctions;
    @Child private JSFunctionCallNode callResolveNode;
    @Child private JSFunctionCallNode callRejectNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

    protected PromiseResolveThenableNode(JSContext context) {
        this.context = context;
        this.createResolvingFunctions = CreateResolvingFunctionNode.create(context);
        this.callResolveNode = JSFunctionCallNode.createCall();
    }

    public static PromiseResolveThenableNode create(JSContext context) {
        return new PromiseResolveThenableNode(context);
    }

    public Object execute(DynamicObject promiseToResolve, Object thenable, Object then) {
        Pair<DynamicObject, DynamicObject> resolvingFunctions = createResolvingFunctions.execute(promiseToResolve);
        DynamicObject resolve = resolvingFunctions.getFirst();
        DynamicObject reject = resolvingFunctions.getSecond();
        try {
            return callResolveNode.executeCall(JSArguments.create(thenable, then, resolve, reject));
        } catch (Throwable ex) {
            if (shouldCatch(ex)) {
                return callReject().executeCall(JSArguments.create(Undefined.instance, reject, getErrorObjectNode.execute(ex)));
            } else {
                throw ex;
            }
        }
    }

    private boolean shouldCatch(Throwable exception) {
        if (getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        return TryCatchNode.shouldCatch(exception);
    }

    private JSFunctionCallNode callReject() {
        if (callRejectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
        }
        return callRejectNode;
    }
}
