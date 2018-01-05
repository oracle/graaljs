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

public abstract class CachedGetPropertyNode extends JavaScriptBaseNode {

    private static final int MAX_DEPTH = 2;

    protected final int depth;

    @Child private CachedGetPropertyNode nextNode;
    protected final JSContext context;

    CachedGetPropertyNode(JSContext context, int depth) {
        this.depth = depth;
        this.context = context;
    }

    public abstract Object getCachedProperty(DynamicObject target, Object propertyKey);

    protected final Object executeNext(DynamicObject target, Object key) {
        if (nextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            int newDepth = depth + 1;
            if (newDepth > MAX_DEPTH) {
                return rewriteToGeneric().getCachedProperty(target, key);
            } else {
                nextNode = insert(makeCacheNode(context, key, newDepth));
            }
        }
        return nextNode.getCachedProperty(target, key);
    }

    private CachedGetPropertyNode rewriteToGeneric() {
        CachedGetPropertyNode parent = this;
        for (int i = 0; i < depth - 1; i++) {
            parent = (CachedGetPropertyNode) parent.getParent();
        }
        return parent.replace(new GenericGetPropertyNode(context));
    }

    private static CachedGetPropertyNode makeCacheNode(JSContext context, Object key, int newDepth) {
        if (JSRuntime.isString(key)) {
            String name = key.toString();
            return JSRuntime.isArrayIndex(name) ? new CachedGetNumericPropertyNode(context, name, newDepth) : new CachedGetNamedPropertyNode(context, name, newDepth);
        } else if (key instanceof Symbol) {
            return new CachedGetNamedPropertyNode(context, key, newDepth);
        } else if (key instanceof Long) {
            return new CachedGetNumericPropertyNode(context, (Long) key, newDepth);
        } else if (key instanceof HiddenKey) {
            return new CachedHiddenGetPropertyNode(context, (HiddenKey) key, newDepth);
        } else {
            return new GenericGetPropertyNode(context);
        }
    }

    public static CachedGetPropertyNode create(JSContext context, Object key) {
        return makeCacheNode(context, key, 1);
    }

    private static class CachedGetNamedPropertyNode extends CachedGetPropertyNode {
        @Child protected PropertyNode propertyNode;
        private final Object cachedKey;

        CachedGetNamedPropertyNode(JSContext context, Object propertyKey, int depth) {
            super(context, depth);
            this.propertyNode = NodeFactory.getInstance(context).createProperty(context, null, propertyKey);
            this.cachedKey = propertyKey;
        }

        @Override
        public Object getCachedProperty(DynamicObject target, Object key) {
            if (JSRuntime.propertyKeyEquals(cachedKey, key)) {
                return propertyNode.executeWithTarget(target);
            }
            return executeNext(target, key);
        }
    }

    private static class CachedGetNumericPropertyNode extends CachedGetPropertyNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        CachedGetNumericPropertyNode(JSContext context, String arrayIndex, int depth) {
            this(context, JSRuntime.propertyNameToArrayIndex(arrayIndex), depth);
        }

        CachedGetNumericPropertyNode(JSContext context, long arrayIndex, int depth) {
            super(context, depth);
            this.toArrayIndexNode = ToArrayIndexNode.createNoToString();
            assert JSRuntime.isArrayIndex(arrayIndex);
        }

        @Override
        public Object getCachedProperty(DynamicObject target, Object key) {
            Object convertedIndex = toArrayIndexNode.execute(key);
            if (convertedIndex instanceof Long) {
                return JSObject.get(target, (long) convertedIndex, jsclassProfile);
            }
            return executeNext(target, convertedIndex);
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    private static class GenericGetPropertyNode extends CachedGetPropertyNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        private final ConditionProfile getType = ConditionProfile.createBinaryProfile();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();

        GenericGetPropertyNode(JSContext context) {
            super(context, 0);
            this.toObjectNode = JSToObjectNode.createToObject(context);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        public Object getCachedProperty(DynamicObject target, Object key) {
            assert !(key instanceof HiddenKey);
            DynamicObject castTarget = JSRuntime.expectJSObject(toObjectNode.executeTruffleObject(target), notAJSObjectBranch);
            Object arrayIndex = toArrayIndexNode.execute(key);
            if (getType.profile(arrayIndex instanceof Long)) {
                return JSObject.get(castTarget, (long) arrayIndex, jsclassProfile);
            } else {
                assert JSRuntime.isPropertyKey(arrayIndex);
                return JSObject.get(castTarget, arrayIndex, jsclassProfile);
            }
        }
    }

    private static class CachedHiddenGetPropertyNode extends CachedGetPropertyNode {
        @Child protected PropertyNode propertyNode;
        private final HiddenKey cachedKey;

        CachedHiddenGetPropertyNode(JSContext context, HiddenKey propertyKey, int depth) {
            super(context, depth);
            this.propertyNode = NodeFactory.getInstance(context).createProperty(context, null, propertyKey);
            this.cachedKey = propertyKey;
        }

        @Override
        public Object getCachedProperty(DynamicObject target, Object key) {
            if (cachedKey.equals(key)) {
                return propertyNode.executeWithTarget(target);
            }
            return executeNext(target, key);
        }
    }
}
