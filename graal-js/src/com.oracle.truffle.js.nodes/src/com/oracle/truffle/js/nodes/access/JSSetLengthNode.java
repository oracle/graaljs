/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;

public abstract class JSSetLengthNode extends JavaScriptBaseNode {
    private final JSContext context;
    protected final boolean isStrict;

    protected JSSetLengthNode(JSContext context, boolean isStrict) {
        this.context = context;
        this.isStrict = isStrict;
    }

    public static JSSetLengthNode create(JSContext context, boolean strict) {
        return JSSetLengthNodeGen.create(context, strict);
    }

    public abstract Object execute(TruffleObject target, Object value);

    protected final WritePropertyNode createWritePropertyNode() {
        return NodeFactory.getInstance(context).createWriteProperty(null, JSArray.LENGTH, null, context, isStrict);
    }

    protected static boolean isArray(TruffleObject object) {
        // currently, must be fast array
        return JSArray.isJSFastArray(object);
    }

    @Specialization(guards = "isArray(object)")
    protected static int setArrayLength(DynamicObject object, int length,
                    @Cached("create(isStrict)") ArrayLengthWriteNode arrayLengthWriteNode) {
        arrayLengthWriteNode.executeVoid(object, length, isArray(object));
        return length;
    }

    @Specialization
    protected static int setIntLength(DynamicObject object, int length,
                    @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
        setLengthProperty.executeIntWithValue(object, length);
        return length;
    }

    @Specialization(replaces = "setIntLength")
    protected static Object setLength(DynamicObject object, Object length,
                    @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
        setLengthProperty.executeWithValue(object, length);
        return length;
    }

    @Specialization(guards = "!isDynamicObject(object)")
    protected static Object setLengthForeign(@SuppressWarnings("unused") TruffleObject object, Object length) {
        // there is no SET_SIZE message. Let's assume WRITE already has done the job
        return length;
    }
}
