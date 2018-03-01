/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@ImportStatic(JSTruffleOptions.class)
public abstract class HasHiddenKeyCacheNode extends JavaScriptBaseNode {
    protected final HiddenKey key;

    protected HasHiddenKeyCacheNode(HiddenKey key) {
        this.key = key;
    }

    public static HasHiddenKeyCacheNode create(HiddenKey key) {
        return HasHiddenKeyCacheNodeGen.create(key);
    }

    public abstract boolean executeHasHiddenKey(Object object);

    @Specialization(guards = {"cachedShape.check(object)"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "PropertyCacheLimit")
    protected static boolean doCached(@SuppressWarnings("unused") DynamicObject object,
                    @SuppressWarnings("unused") @Cached("object.getShape()") Shape cachedShape,
                    @Cached("doUncached(object)") boolean hasOwnProperty) {
        return hasOwnProperty;
    }

    @Specialization(guards = "isJSObject(object)", replaces = {"doCached"})
    protected final boolean doUncached(DynamicObject object) {
        return object.containsKey(key);
    }

    @Specialization(guards = "!isJSObject(object)")
    protected static boolean doNonObject(@SuppressWarnings("unused") Object object) {
        return false;
    }

    public final HiddenKey getKey() {
        return key;
    }
}
