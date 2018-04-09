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
