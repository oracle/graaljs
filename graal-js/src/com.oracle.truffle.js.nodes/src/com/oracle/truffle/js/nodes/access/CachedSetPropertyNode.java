/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public abstract class CachedSetPropertyNode extends JavaScriptBaseNode {

    private static final int MAX_DEPTH = 1;

    @Child private CachedSetPropertyNode nextNode;
    protected final JSContext context;
    protected final boolean strict;

    protected final int depth;

    CachedSetPropertyNode(JSContext context, boolean strict, int depth) {
        this.context = context;
        this.strict = strict;
        this.depth = depth;
    }

    public abstract void setCachedProperty(DynamicObject target, Object propertyKey, Object value);

    protected final void executeNext(DynamicObject target, Object key, Object value) {
        if (nextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            int newDepth = depth + 1;
            if (newDepth > MAX_DEPTH) {
                rewriteToGeneric().setCachedProperty(target, key, value);
                return;
            } else {
                nextNode = insert(makeCacheNode(context, key, strict, newDepth));
            }
        }
        nextNode.setCachedProperty(target, key, value);
    }

    private CachedSetPropertyNode rewriteToGeneric() {
        CachedSetPropertyNode parent = this;
        for (int i = 0; i < depth - 1; i++) {
            parent = (CachedSetPropertyNode) parent.getParent();
        }
        return parent.replace(new GenericSetPropertyNode(context, strict));
    }

    private static CachedSetPropertyNode makeCacheNode(JSContext context, Object key, boolean strict, int newDepth) {
        if (JSRuntime.isString(key)) {
            String name = key.toString();
            if (JSRuntime.isArrayIndex(name)) {
                return new CachedSetNumericPropertyNode(context, name, strict, newDepth);
            } else {
                return new CachedSetNamedPropertyNode(context, name, strict, newDepth);
            }
        } else if (key instanceof Symbol) {
            return new CachedSetNamedPropertyNode(context, key, strict, newDepth);
        } else if (key instanceof Long) {
            return new CachedSetNumericPropertyNode(context, (Long) key, strict, newDepth);
        } else if (key instanceof HiddenKey) {
            return new CachedSetHiddenPropertyNode(context, (HiddenKey) key, strict, newDepth);
        } else {
            return new GenericSetPropertyNode(context, strict);
        }
    }

    public static CachedSetPropertyNode create(JSContext context, Object key, boolean strict) {
        return makeCacheNode(context, key, strict, 1);
    }

    private static class CachedSetNamedPropertyNode extends CachedSetPropertyNode {
        @Child protected WritePropertyNode setPropertyNode;
        private final Object cachedKey;

        CachedSetNamedPropertyNode(JSContext context, Object propertyKey, boolean strict, int depth) {
            super(context, strict, depth);
            this.setPropertyNode = NodeFactory.getInstance(context).createWriteProperty(null, propertyKey, null, context, strict);
            this.cachedKey = propertyKey;
        }

        @Override
        public void setCachedProperty(DynamicObject target, Object key, Object value) {
            if (JSRuntime.propertyKeyEquals(cachedKey, key)) {
                setPropertyNode.executeWithValue(target, value);
            } else {
                executeNext(target, key, value);
            }
        }
    }

    private static class CachedSetNumericPropertyNode extends CachedSetPropertyNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        protected final JSClassProfile classProfile = JSClassProfile.create();

        CachedSetNumericPropertyNode(JSContext context, String arrayIndex, boolean strict, int depth) {
            this(context, JSRuntime.propertyNameToArrayIndex(arrayIndex), strict, depth);
        }

        CachedSetNumericPropertyNode(JSContext context, long arrayIndex, boolean strict, int depth) {
            super(context, strict, depth);
            this.toArrayIndexNode = ToArrayIndexNode.createNoToString();
            assert JSRuntime.isArrayIndex(arrayIndex);
        }

        @Override
        public void setCachedProperty(DynamicObject target, Object key, Object value) {
            Object convertedIndex = toArrayIndexNode.execute(key);
            if (convertedIndex instanceof Long) {
                JSObject.set(target, (long) convertedIndex, value, strict, classProfile);
            } else {
                executeNext(target, convertedIndex, value);
            }
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    private static class GenericSetPropertyNode extends CachedSetPropertyNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        private final ConditionProfile setType = ConditionProfile.createBinaryProfile();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();

        GenericSetPropertyNode(JSContext context, boolean strict) {
            super(context, strict, 0);
            this.toObjectNode = JSToObjectNode.createToObject(context);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        public void setCachedProperty(DynamicObject target, Object key, Object value) {
            assert !(key instanceof HiddenKey);
            DynamicObject castTarget = JSRuntime.expectJSObject(toObjectNode.executeTruffleObject(target), notAJSObjectBranch);
            Object arrayIndex = toArrayIndexNode.execute(key);
            if (setType.profile(arrayIndex instanceof Long)) {
                JSObject.set(castTarget, (long) arrayIndex, value, strict, jsclassProfile);
            } else {
                assert JSRuntime.isPropertyKey(arrayIndex);
                JSObject.set(castTarget, arrayIndex, value, strict, jsclassProfile);
            }
        }
    }

    private static class CachedSetHiddenPropertyNode extends CachedSetPropertyNode {
        @Child protected WritePropertyNode setPropertyNode;
        private final HiddenKey cachedKey;

        CachedSetHiddenPropertyNode(JSContext context, HiddenKey propertyKey, boolean strict, int depth) {
            super(context, strict, depth);
            this.setPropertyNode = NodeFactory.getInstance(context).createWriteProperty(null, propertyKey, null, context, strict);
            this.cachedKey = propertyKey;
        }

        @Override
        public void setCachedProperty(DynamicObject target, Object key, Object value) {
            if (cachedKey.equals(key)) {
                setPropertyNode.executeWithValue(target, value);
            } else {
                executeNext(target, key, value);
            }
        }
    }
}
