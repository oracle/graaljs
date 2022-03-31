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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@GenerateUncached
public abstract class JSInteropInvokeNode extends JSInteropCallNode {
    JSInteropInvokeNode() {
    }

    public static JSInteropInvokeNode create() {
        return JSInteropInvokeNodeGen.create();
    }

    public abstract Object execute(JSDynamicObject receiver, TruffleString name, Object[] arguments) throws UnknownIdentifierException, UnsupportedMessageException;

    @Specialization(guards = {"stringEquals(equalNode, cachedName, name)"}, limit = "1")
    Object doCached(JSDynamicObject receiver, @SuppressWarnings("unused") TruffleString name, Object[] arguments,
                    @Cached("name") TruffleString cachedName,
                    @Cached @SuppressWarnings("unused") TruffleString.EqualNode equalNode,
                    @Cached("createGetProperty(cachedName)") PropertyGetNode functionPropertyGetNode,
                    @Shared("isCallable") @Cached IsCallableNode isCallableNode,
                    @Shared("call") @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callNode,
                    @Shared("importValue") @Cached ImportValueNode importValueNode) throws UnknownIdentifierException, UnsupportedMessageException {
        Object function = functionPropertyGetNode.getValueOrDefault(receiver, null);
        if (function == null) {
            throw UnknownIdentifierException.create(cachedName.toJavaStringUncached());
        }
        if (isCallableNode.executeBoolean(function)) {
            return callNode.executeCall(JSArguments.create(receiver, function, prepare(arguments, importValueNode)));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @Specialization(replaces = "doCached")
    Object doUncached(JSDynamicObject receiver, TruffleString name, Object[] arguments,
                    @Cached(value = "create(getLanguage().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Shared("isCallable") @Cached IsCallableNode isCallableNode,
                    @Shared("call") @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callNode,
                    @Shared("importValue") @Cached ImportValueNode importValueNode) throws UnknownIdentifierException, UnsupportedMessageException {
        Object function;
        if (readNode == null) {
            function = JSObject.getOrDefault(receiver, name, receiver, null);
        } else {
            function = readNode.executeWithTargetAndIndexOrDefault(receiver, name, null);
        }
        if (function == null) {
            throw UnknownIdentifierException.create(name.toJavaStringUncached());
        }
        if (isCallableNode.executeBoolean(function)) {
            Object[] preparedArgs = prepare(arguments, importValueNode);
            return callNode.executeCall(JSArguments.create(receiver, function, preparedArgs));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    PropertyGetNode createGetProperty(TruffleString name) {
        return PropertyGetNode.create(name, false, getLanguage().getJSContext());
    }

    static ReadElementNode getUncachedRead() {
        return null;
    }
}
