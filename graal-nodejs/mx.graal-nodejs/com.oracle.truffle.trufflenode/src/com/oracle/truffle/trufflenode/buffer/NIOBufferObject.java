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
package com.oracle.truffle.trufflenode.buffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinLookup;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.RealmData;

public final class NIOBufferObject extends JSBuiltinObject {
    private static final JSBuiltinsContainer NIO_BUFFER_BUILTINS = new NIOBufferBuiltins();

    public static final String NIO_BUFFER_MODULE_NAME = "internal/graal/buffer.js";

    private static final String CLASS_NAME = "NIOBuffer";

    private NIOBufferObject() {
    }

    @TruffleBoundary
    private static DynamicObject create(JSContext context) {
        DynamicObject obj = context.getEmptyShape().newInstance();
        ((JSBuiltinLookup) context.getFunctionLookup()).defineBuiltins(NIO_BUFFER_BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(context.getRealm(), obj, NIO_BUFFER_BUILTINS.getName());
        return obj;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @TruffleBoundary
    public static Object createInitFunction(ScriptNode scriptNode) {
        JSContext context = scriptNode.getContext();
        JSRealm realm = context.getRealm();

        // This JS function will be executed at node.js bootstrap time to register
        // the "default" Buffer API functions.
        JavaScriptRootNode wrapperNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                assert args.length == 4;
                DynamicObject nativeUtf8Write = (DynamicObject) args[2];
                DynamicObject nativeUtf8Slice = (DynamicObject) args[3];
                RealmData embedderData = GraalJSAccess.getRealmEmbedderData(context.getRealm());
                embedderData.setNativeUtf8Write(nativeUtf8Write);
                embedderData.setNativeUtf8Slice(nativeUtf8Slice);
                return create(context);
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(wrapperNode), 2, "NIOBufferBuiltinsInitFunction");
        return JSFunction.create(realm, functionData);
    }

}
