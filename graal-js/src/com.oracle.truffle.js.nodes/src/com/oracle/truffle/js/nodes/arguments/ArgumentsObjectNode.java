/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.RealmNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Allocate arguments object from arguments array.
 */
public abstract class ArgumentsObjectNode extends JavaScriptNode {
    private final boolean strict;
    @Child private RealmNode realmNode;
    private final int leadingArgCount;
    private final int trailingArgCount;

    protected ArgumentsObjectNode(JSContext context, boolean strict, int leadingArgCount, int trailingArgCount) {
        this.strict = strict;
        this.realmNode = RealmNode.create(context);
        this.leadingArgCount = leadingArgCount;
        this.trailingArgCount = trailingArgCount;
    }

    public static JavaScriptNode create(JSContext context, boolean strict, int leadingArgCount, int trailingArgCount) {
        return ArgumentsObjectNodeGen.create(context, strict, leadingArgCount, trailingArgCount);
    }

    protected final boolean isStrict(VirtualFrame frame) {
        // non-strict functions may have unmapped (strict) arguments, but not the other way around.
        // (namely, if simpleParameterList is false, or if it is a built-in function)
        assert strict == JSFunction.isStrict(getFunctionObject(frame)) || strict;
        return strict;
    }

    @Specialization(guards = "isStrict(frame)")
    protected DynamicObject doStrict(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        return JSArgumentsObject.createStrict(realmNode.execute(frame), arguments);
    }

    @Specialization(guards = "!isStrict(frame)")
    protected DynamicObject doNonStrict(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        return JSArgumentsObject.createNonStrict(realmNode.execute(frame), arguments, getFunctionObject(frame));
    }

    private static DynamicObject getFunctionObject(VirtualFrame frame) {
        return (DynamicObject) JSArguments.getFunctionObject(frame.getArguments());
    }

    public Object[] getObjectArray(VirtualFrame frame) {
        return JSArguments.extractUserArguments(frame.getArguments(), leadingArgCount, trailingArgCount);
    }

    static boolean isInitialized(Object argumentsArray) {
        return argumentsArray != Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return ArgumentsObjectNodeGen.create(realmNode.getContext(), strict, leadingArgCount, trailingArgCount);
    }
}
