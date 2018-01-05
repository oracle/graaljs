/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
