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
package com.oracle.truffle.trufflenode.buffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.RealmData;

public final class NIOBuffer extends JSNonProxy {

    public static final TruffleString NIO_BUFFER_MODULE_NAME = Strings.constant("node:internal/graal/buffer");

    private static final TruffleString NIO_BUFFER_BUILTINS_INIT_FUNCTION = Strings.constant("NIOBufferBuiltinsInitFunction");

    private NIOBuffer() {
    }

    @TruffleBoundary
    private static JSObject create(JSRealm realm) {
        JSContext context = realm.getContext();
        JSObject obj = JSOrdinary.createWithNullPrototype(context);
        JSObjectUtil.putFunctionsFromContainer(realm, obj, NIOBufferPrototype.BUILTINS);
        return obj;
    }

    @TruffleBoundary
    public static Object createInitFunction(JSRealm functionRealm) {
        // This JS function will be executed at node.js bootstrap time to register
        // the "default" Buffer API functions.
        JavaScriptRootNode wrapperNode = new JavaScriptRootNode(functionRealm.getContext().getLanguage()) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                assert JSArguments.getUserArgumentCount(args) == 1 : JSArguments.getUserArgumentCount(args);
                JSFunctionObject nativeUtf8Write = (JSFunctionObject) JSArguments.getUserArgument(args, 0);
                JSRealm realm = getRealm();
                RealmData embedderData = GraalJSAccess.getRealmEmbedderData(realm);
                embedderData.setNativeUtf8Write(nativeUtf8Write);
                return create(realm);
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(functionRealm.getContext(), wrapperNode.getCallTarget(), 2, NIO_BUFFER_BUILTINS_INIT_FUNCTION);
        return JSFunction.create(functionRealm, functionData);
    }

}
