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
package com.oracle.truffle.trufflenode.node.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class MakeMirrorNode extends JavaScriptRootNode {
    public static final String NAME = "MakeMirror";
    protected static final HiddenKey ORIGINAL_KEY = new HiddenKey("Original");
    private final JSContext context;
    private final JSFunctionData promiseStatusData;
    private final JSFunctionData promiseValueData;
    private final JSFunctionData iteratorPreviewData;

    public MakeMirrorNode(JSContext context,
                    JSFunctionData promiseStatusData,
                    JSFunctionData promiseValueData,
                    JSFunctionData iteratorPreviewData) {
        this.context = context;
        this.promiseStatusData = promiseStatusData;
        this.promiseValueData = promiseValueData;
        this.iteratorPreviewData = iteratorPreviewData;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        Object arg0 = JSArguments.getUserArgument(args, 0);
        if (JSPromise.isJSPromise(arg0)) {
            DynamicObject mirror = createMirror((DynamicObject) arg0);
            DynamicObject status = JSFunction.create(context.getRealm(), promiseStatusData);
            DynamicObject promiseValue = JSFunction.create(context.getRealm(), promiseValueData);
            JSObject.set(mirror, promiseStatusData.getName(), status);
            JSObject.set(mirror, promiseValueData.getName(), promiseValue);
            return mirror;
        } else if (arg0 instanceof DynamicObject && ((DynamicObject) arg0).containsKey(JSRuntime.ITERATED_OBJECT_ID)) { // iterator
            DynamicObject mirror = createMirror((DynamicObject) arg0);
            DynamicObject preview = JSFunction.create(context.getRealm(), iteratorPreviewData);
            JSObject.set(mirror, iteratorPreviewData.getName(), preview);
            return mirror;
        } else {
            unsupported();
            return Undefined.instance;
        }
    }

    private DynamicObject createMirror(DynamicObject original) {
        DynamicObject mirror = JSUserObject.create(context);
        mirror.define(ORIGINAL_KEY, original);
        return mirror;
    }

    @CompilerDirectives.TruffleBoundary
    private static void unsupported() {
        System.err.println("Unsupported usage of Debug.MakeMirror!");
    }

}
