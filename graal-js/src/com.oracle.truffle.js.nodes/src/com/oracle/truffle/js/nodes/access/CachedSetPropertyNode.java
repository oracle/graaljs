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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@ImportStatic({JSRuntime.class, CachedGetPropertyNode.class})
public abstract class CachedSetPropertyNode extends JavaScriptBaseNode {
    static final int MAX_DEPTH = 1;

    protected final JSContext context;
    protected final boolean strict;

    CachedSetPropertyNode(JSContext context, boolean strict) {
        this.context = context;
        this.strict = strict;
    }

    public abstract void execute(DynamicObject target, Object propertyKey, Object value);

    public static CachedSetPropertyNode create(JSContext context, boolean strict) {
        return CachedSetPropertyNodeGen.create(context, strict);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedKey != null", "!isArrayIndex(cachedKey)", "propertyKeyEquals(cachedKey, key)"}, limit = "MAX_DEPTH")
    void doCachedKey(DynamicObject target, Object key, Object value,
                    @Cached("cachedPropertyKey(key)") Object cachedKey,
                    @Cached("createSet(cachedKey)") PropertySetNode propertyNode) {
        propertyNode.setValue(target, value);
    }

    @Specialization(guards = {"isArrayIndex(index)"})
    void doIntIndex(DynamicObject target, int index, Object value,
                    @Cached("create()") JSClassProfile jsclassProfile) {
        JSObject.set(target, index, value, strict, jsclassProfile);
    }

    @Specialization(guards = {"toArrayIndexNode.isArrayIndex(key)"}, replaces = {"doIntIndex"})
    void doArrayIndex(DynamicObject target, Object key, Object value,
                    @Cached("createNoToString()") ToArrayIndexNode toArrayIndexNode,
                    @Cached("create()") JSClassProfile jsclassProfile) {
        long index = (long) toArrayIndexNode.execute(key);
        JSObject.set(target, index, value, strict, jsclassProfile);
    }

    @Specialization(replaces = {"doCachedKey", "doArrayIndex"})
    void doGeneric(DynamicObject target, Object key, Object value,
                    @Cached("create()") ToArrayIndexNode toArrayIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile getType,
                    @Cached("create()") JSClassProfile jsclassProfile) {
        Object arrayIndex = toArrayIndexNode.execute(key);
        if (getType.profile(arrayIndex instanceof Long)) {
            JSObject.set(target, (long) arrayIndex, value, strict, jsclassProfile);
        } else {
            assert JSRuntime.isPropertyKey(arrayIndex);
            JSObject.set(target, arrayIndex, value, strict, jsclassProfile);
        }
    }

    PropertySetNode createSet(Object key) {
        return PropertySetNode.create(key, false, context, strict);
    }
}
