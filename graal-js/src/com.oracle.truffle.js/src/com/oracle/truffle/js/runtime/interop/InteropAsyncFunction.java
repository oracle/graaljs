/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.promise.UnwrapPromiseNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Interop wrapper for async functions that unwraps the returned promise.
 */
@ImportStatic({JSConfig.class})
@ExportLibrary(value = InteropLibrary.class, delegateTo = "function")
public final class InteropAsyncFunction extends InteropFunction {

    final JSFunctionObject function;

    public InteropAsyncFunction(JSFunctionObject function) {
        super(function);
        this.function = function;
    }

    @Override
    public int hashCode() {
        return function.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof InteropAsyncFunction) {
            InteropAsyncFunction other = (InteropAsyncFunction) obj;
            return function.equals(other.function);
        } else {
            return false;
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropExecuteNode callNode,
                    @Cached ExportValueNode exportNode,
                    @Cached UnwrapPromiseNode unwrapPromise) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        assert language.getJSContext().getContextOptions().isMLEMode();
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        Object result;
        try {
            result = callNode.execute(function, Undefined.instance, arguments);
            result = exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
        /*
         * InteropCompletePromises semantics: interop calls to async functions return the async
         * resolved value (if any). If the promise resolves, its value is made available by flushing
         * the queue of pending jobs.
         */
        return unwrapPromise.execute((DynamicObject) result);
    }
}
