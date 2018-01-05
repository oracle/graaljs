/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSPromise;

public class PromiseStatusNode extends JavaScriptRootNode {
    public static final String NAME = "status";

    @Override
    public Object execute(VirtualFrame frame) {
        Object thiz = JSArguments.getThisObject(frame.getArguments());
        Object original = ((DynamicObject) thiz).get(MakeMirrorNode.ORIGINAL_KEY);
        assert JSPromise.isJSPromise(original);
        Object state = ((DynamicObject) original).get(JSPromise.PROMISE_STATE);
        switch (((Number) state).intValue()) {
            case 0:
                return "pending";
            case 1:
                return "resolved";
            case 2:
                return "rejected";
            default:
                CompilerDirectives.transferToInterpreter();
                throw new InternalError(JSRuntime.stringConcat("Unexpected promise state ", JSRuntime.toString(state)));
        }
    }

}
