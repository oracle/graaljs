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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JSIteratorWrapper;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic({JSConfig.class, JSRuntime.class, Symbol.class, Strings.class})
@GenerateUncached
public abstract class JSInteropGetIteratorNode extends JSInteropCallNode {
    JSInteropGetIteratorNode() {
    }

    public final boolean hasIterator(JSObject receiver, JavaScriptLanguage language) {
        try {
            return (boolean) execute(receiver, language, true);
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    public final Object getIterator(JSObject receiver, JavaScriptLanguage language) throws UnsupportedMessageException {
        return execute(receiver, language, false);
    }

    protected abstract Object execute(JSObject receiver, JavaScriptLanguage language, boolean hasIteratorCheck) throws UnsupportedMessageException;

    @Specialization
    Object doDefault(JSObject receiver, JavaScriptLanguage language, boolean hasIteratorCheck,
                    @Cached(value = "create(SYMBOL_ITERATOR, language.getJSContext())", uncached = "getUncachedProperty()") PropertyGetNode iteratorPropertyGetNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callNode,
                    @Cached(value = "create(NEXT, language.getJSContext())", uncached = "getUncachedProperty()") PropertyGetNode nextPropertyGetNode,
                    @Cached InlinedBranchProfile exceptionBranch) throws UnsupportedMessageException {
        JSRealm realm = JSRealm.get(this);
        language.interopBoundaryEnter(realm);
        try {
            Object method = getProperty(receiver, iteratorPropertyGetNode, Symbol.SYMBOL_ITERATOR, null);
            boolean hasIterator = method != null && isCallableNode.executeBoolean(method);
            if (hasIteratorCheck) {
                return hasIterator;
            }
            if (hasIterator) {
                Object iterator = callNode.executeCall(JSArguments.createZeroArg(receiver, method));
                if (iterator instanceof JSObject) {
                    JSObject jsIterator = (JSObject) iterator;
                    Object nextMethod = getProperty(jsIterator, nextPropertyGetNode, Strings.NEXT, null);
                    if (nextMethod != null && isCallableNode.executeBoolean(nextMethod)) {
                        return JSIteratorWrapper.create(IteratorRecord.create(jsIterator, nextMethod));
                    }
                }
                exceptionBranch.enter(this);
                throw Errors.createTypeErrorNotIterable(receiver, null);
            } else {
                exceptionBranch.enter(this);
                throw UnsupportedMessageException.create();
            }
        } finally {
            language.interopBoundaryExit(realm);
        }
    }
}
