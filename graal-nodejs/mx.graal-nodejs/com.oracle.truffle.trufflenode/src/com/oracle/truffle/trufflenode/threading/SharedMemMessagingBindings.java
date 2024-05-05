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
package com.oracle.truffle.trufflenode.threading;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * JS Builtins used by Node.s workers to send Java object references via message passing (@see
 * lib/internal/worker.js).
 */
public final class SharedMemMessagingBindings extends JSNonProxy {

    private static final TruffleString SHARED_MEM_MESSAGING_INIT = Strings.constant("SharedMemMessagingInit");

    private static final SharedMemMessagingBindings INSTANCE = new SharedMemMessagingBindings();

    private static final JSBuiltinsContainer BUILTINS = new SharedMemMessagingBuiltins();

    private SharedMemMessagingBindings() {
    }

    @TruffleBoundary
    private static JSObject create(JSRealm realm) {
        Shape shape = realm.getContext().makeEmptyShapeWithNullPrototype(INSTANCE);
        JSObject obj = new Instance(shape);
        JSObjectUtil.putFunctionsFromContainer(realm, obj, BUILTINS);
        return obj;
    }

    @TruffleBoundary
    public static Object createInitFunction(JSRealm realm) {
        // This JS function will be executed at node.js bootstrap time
        JavaScriptRootNode wrapperNode = new JavaScriptRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return create(getRealm());
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(realm.getContext(), wrapperNode.getCallTarget(), 2, SHARED_MEM_MESSAGING_INIT);
        return JSFunction.create(realm, functionData);
    }

    public static final class Instance extends JSNonProxyObject {

        protected Instance(Shape shape) {
            super(shape, Null.instance);
        }
    }
}
