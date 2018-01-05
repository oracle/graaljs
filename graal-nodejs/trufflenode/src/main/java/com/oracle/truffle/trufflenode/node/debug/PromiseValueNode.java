/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node.debug;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class PromiseValueNode extends JavaScriptRootNode {
    public static final String NAME = "promiseValue";
    private final JSContext context;

    public PromiseValueNode(JSContext context) {
        this.context = context;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object thiz = JSArguments.getThisObject(frame.getArguments());
        Object original = ((DynamicObject) thiz).get(MakeMirrorNode.ORIGINAL_KEY);
        assert JSPromise.isJSPromise(original);
        DynamicObject result = JSUserObject.create(context);
        Object value = ((DynamicObject) original).get(JSPromise.PROMISE_RESULT);
        JSObject.set(result, "value_", value);
        return result;
    }

}
