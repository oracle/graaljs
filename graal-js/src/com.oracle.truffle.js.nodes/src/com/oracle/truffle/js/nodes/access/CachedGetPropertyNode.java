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
