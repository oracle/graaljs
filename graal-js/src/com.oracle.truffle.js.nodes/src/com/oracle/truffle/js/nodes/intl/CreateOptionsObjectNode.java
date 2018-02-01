/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;

public abstract class CreateOptionsObjectNode extends JavaScriptBaseNode {

    @Child JSToObjectNode toObjectNode;
    private final JSContext context;

    public JSContext getContext() {
        return context;
    }

    public CreateOptionsObjectNode(JSContext context) {
        super();
        this.context = context;
    }

    public abstract DynamicObject execute(Object opts);

    @SuppressWarnings("unused")
    @Specialization(guards = "isUndefined(opts)")
    public DynamicObject fromUndefined(Object opts) {
        return JSUserObject.createWithPrototype(null, getContext());
    }

    @Specialization(guards = "!isUndefined(opts)")
    public DynamicObject fromOtherThenUndefined(Object opts) {
        return toDynamicObject(opts);
    }

    private DynamicObject toDynamicObject(Object o) {
        if (toObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
        }
        return (DynamicObject) toObjectNode.executeTruffleObject(o);
    }
}