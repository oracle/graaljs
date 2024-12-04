/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.FrequencyBasedPolymorphicAccessNode.FrequencyBasedPropertySetNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNoToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@ImportStatic({JSRuntime.class})
abstract class CachedSetPropertyNode extends JavaScriptBaseNode {
    static final int MAX_DEPTH = 1;

    protected final JSContext context;
    protected final boolean strict;
    protected final boolean setOwn;
    protected final boolean superProperty;

    CachedSetPropertyNode(JSContext context, boolean strict, boolean setOwn, boolean superProperty) {
        this.context = context;
        this.strict = strict;
        this.setOwn = setOwn;
        this.superProperty = superProperty;
    }

    public abstract void execute(JSDynamicObject target, Object propertyKey, Object value, Object receiver);

    static CachedSetPropertyNode create(JSContext context, boolean strict, boolean setOwn, boolean superProperty) {
        return CachedSetPropertyNodeGen.create(context, strict, setOwn, superProperty);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedKey != null", "!isArrayIndex(cachedKey)", "propertyKeyEquals(equalsNode, cachedKey, key)"}, limit = "MAX_DEPTH")
    void doCachedKey(JSDynamicObject target, Object key, Object value, Object receiver,
                    @Cached("propertyKeyOrNull(key)") Object cachedKey,
                    @Cached("createSet(cachedKey)") PropertySetNode propertyNode,
                    @Cached @Shared TruffleString.EqualNode equalsNode) {
        propertyNode.setValue(target, value, receiver);
    }

    @Specialization(guards = {"isArrayIndex(index)", "!isJSProxy(target)"})
    void doIntIndex(JSDynamicObject target, int index, Object value, Object receiver,
                    @Cached @Shared JSClassProfile jsclassProfile) {
        doArrayIndexLong(target, index, value, receiver, jsclassProfile.getJSClass(target));
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"!isJSProxy(target)", "index >= 0"}, replaces = {"doIntIndex"}, limit = "1")
    void doArrayIndex(JSDynamicObject target, @SuppressWarnings("unused") Object key, Object value, Object receiver,
                    @Cached @SuppressWarnings("unused") ToArrayIndexNoToPropertyKeyNode toArrayIndexNode,
                    @Bind("toArrayIndexNode.executeLong($node, key)") long index,
                    @Cached @Shared JSClassProfile jsclassProfile) {
        assert JSRuntime.isArrayIndex(index) : index;
        doArrayIndexLong(target, index, value, receiver, jsclassProfile.getJSClass(target));
    }

    private void doArrayIndexLong(JSDynamicObject target, long index, Object value, Object receiver, JSClass jsclass) {
        if (setOwn) {
            createDataPropertyOrThrow(target, Strings.fromLong(index), value);
        } else {
            jsclass.set(target, index, value, receiver, strict, this);
        }
    }

    @Specialization(guards = {"isJSProxy(target)"})
    void doProxy(JSDynamicObject target, Object index, Object value, Object receiver,
                    @Cached("create(context, strict, setOwn, getAttributes())") JSProxyPropertySetNode proxySet) {
        proxySet.executeWithReceiverAndValue(target, receiver, value, index);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(replaces = {"doCachedKey", "doArrayIndex", "doProxy"})
    void doGeneric(JSDynamicObject target, Object key, Object value, Object receiver,
                    @Bind Node node,
                    @Cached ToArrayIndexNode toArrayIndexNode,
                    @Cached InlinedConditionProfile getType,
                    @Cached @Shared JSClassProfile jsclassProfile,
                    @Cached InlinedConditionProfile highFrequency,
                    @Cached("create(context, setOwn, strict, superProperty)") FrequencyBasedPropertySetNode hotKey,
                    @Cached @Shared TruffleString.EqualNode equalsNode) {
        Object arrayIndex = toArrayIndexNode.execute(key);
        if (getType.profile(node, arrayIndex instanceof Long)) {
            long index = (long) arrayIndex;
            doArrayIndexLong(target, index, value, receiver, jsclassProfile.getJSClass(target));
        } else {
            if (highFrequency.profile(node, hotKey.executeFastSet(target, arrayIndex, value, receiver, equalsNode))) {
                return;
            }
            if (setOwn) {
                createDataPropertyOrThrow(target, arrayIndex, value);
            } else {
                JSObject.setWithReceiver(target, arrayIndex, value, receiver, strict, jsclassProfile, this);
            }
        }
    }

    private static void createDataPropertyOrThrow(JSDynamicObject target, Object propertyKey, Object value) {
        JSObject.defineOwnProperty(target, propertyKey, PropertyDescriptor.createDataDefault(value), true);
    }

    @NeverDefault
    PropertySetNode createSet(Object key) {
        return PropertySetNode.createImpl(key, false, context, strict, setOwn, JSAttributes.getDefault(), false, superProperty);
    }

    static Object propertyKeyOrNull(Object key) {
        return JSRuntime.isPropertyKey(key) ? key : null;
    }

    static int getAttributes() {
        return JSAttributes.getDefault();
    }
}
