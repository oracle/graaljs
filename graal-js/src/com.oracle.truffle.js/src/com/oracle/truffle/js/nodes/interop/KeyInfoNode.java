/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsExtensibleNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This node implements the {@code isMember*} messages.
 */
@ImportStatic(Strings.class)
@GenerateUncached
public abstract class KeyInfoNode extends JavaScriptBaseNode {
    public static final int READABLE = 1 << 0;
    public static final int MODIFIABLE = 1 << 1;
    public static final int INSERTABLE = 1 << 2;
    public static final int INVOCABLE = 1 << 3;
    public static final int REMOVABLE = 1 << 4;
    public static final int READ_SIDE_EFFECTS = 1 << 5;
    public static final int WRITE_SIDE_EFFECTS = 1 << 6;
    public static final int WRITABLE = INSERTABLE | MODIFIABLE;

    KeyInfoNode() {
    }

    public abstract boolean execute(JSDynamicObject receiver, String key, int query);

    @Specialization(guards = {"!isJSProxy(target)", "property != null"}, limit = "2")
    static boolean cachedOwnProperty(JSDynamicObject target, @SuppressWarnings("unused") String key, int query,
                    @Bind("this") Node node,
                    @CachedLibrary("target") DynamicObjectLibrary objectLibrary,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.FromJavaStringNode fromJavaStringNode,
                    @Bind("fromJavaString(fromJavaStringNode, key)") TruffleString tStringKey,
                    @Bind("objectLibrary.getProperty(target, tStringKey)") Property property,
                    @Cached @Shared IsCallableNode isCallable,
                    @Cached InlinedBranchProfile proxyBranch,
                    @Cached InlinedBranchProfile moduleNamespaceBranch) {
        if ((query & READABLE) != 0) {
            return true;
        }
        if (JSProperty.isAccessor(property)) {
            Accessor accessor = (Accessor) Properties.getOrDefault(objectLibrary, target, tStringKey, null);
            if ((query & MODIFIABLE) != 0 && accessor.hasSetter()) {
                return true;
            }
            if ((query & READ_SIDE_EFFECTS) != 0 && accessor.hasGetter()) {
                return true;
            }
            if ((query & WRITE_SIDE_EFFECTS) != 0 && accessor.hasSetter()) {
                return true;
            }
            if ((query & REMOVABLE) != 0 && JSProperty.isConfigurable(property)) {
                return true;
            }
        } else {
            assert JSProperty.isData(property);
            if ((query & MODIFIABLE) != 0 && JSProperty.isWritable(property)) {
                return true;
            }
            if ((query & INVOCABLE) != 0) {
                Object value = Properties.getOrDefault(objectLibrary, target, tStringKey, Undefined.instance);
                if (JSProperty.isDataSpecial(property)) {
                    if (JSProperty.isProxy(property)) {
                        proxyBranch.enter(node);
                        value = ((PropertyProxy) value).get(target);
                    } else {
                        assert JSProperty.isModuleNamespaceExport(property) : property;
                        moduleNamespaceBranch.enter(node);
                        value = JSModuleNamespace.getBindingValue((ExportResolution) value);
                    }
                }
                if (isCallable.executeBoolean(value)) {
                    return true;
                }
            }
            if ((query & REMOVABLE) != 0 && JSProperty.isConfigurable(property)) {
                return true;
            }
        }
        return false;
    }

    @Specialization
    boolean doProxy(JSProxyObject proxy, String key, int query,
                    @Cached @Shared GetPrototypeNode getPrototype,
                    @Cached @Shared IsCallableNode isCallable,
                    @Cached @Shared IsExtensibleNode isExtensible,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaStringNode) {
        JavaScriptLanguage language = JavaScriptLanguage.get(this);
        JSRealm realm = JSRealm.get(this);
        language.interopBoundaryEnter(realm);
        try {
            return doNonProxy(proxy, key, query, getPrototype, isCallable, isExtensible, fromJavaStringNode);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @Specialization(guards = {"!isJSProxy(target)"}, replaces = "cachedOwnProperty")
    static boolean doNonProxy(JSDynamicObject target, String key, int query,
                    @Cached @Shared GetPrototypeNode getPrototype,
                    @Cached @Shared IsCallableNode isCallable,
                    @Cached @Shared IsExtensibleNode isExtensible,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaStringNode) {
        TruffleString tStringKey = Strings.fromJavaString(fromJavaStringNode, key);
        PropertyDescriptor desc = null;
        boolean isProxy = false;
        for (JSDynamicObject proto = target; proto != Null.instance; proto = getPrototype.execute(proto)) {
            desc = JSObject.getOwnProperty(proto, tStringKey);
            if (JSProxy.isJSProxy(proto)) {
                isProxy = true;
                break;
            }
            if (desc != null) {
                break;
            }
        }
        if (desc == null) {
            if ((query & INSERTABLE) != 0 && isExtensible.executeBoolean(target)) {
                return true;
            }
            return false;
        }

        boolean hasGet = desc.hasGet() && desc.getGet() != Undefined.instance;
        boolean hasSet = desc.hasSet() && desc.getSet() != Undefined.instance;
        boolean writable = hasSet || (!hasGet && desc.getIfHasWritable(true));
        boolean readSideEffects = isProxy || hasGet;
        boolean writeSideEffects = isProxy || hasSet;
        if ((query & READABLE) != 0) {
            return true;
        }
        if ((query & MODIFIABLE) != 0 && writable) {
            return true;
        }
        if ((query & READ_SIDE_EFFECTS) != 0 && readSideEffects) {
            return true;
        }
        if ((query & WRITE_SIDE_EFFECTS) != 0 && writeSideEffects) {
            return true;
        }
        if ((query & INVOCABLE) != 0 && desc.isDataDescriptor() && isCallable.executeBoolean(desc.getValue())) {
            return true;
        }
        if ((query & REMOVABLE) != 0 && desc.getConfigurable()) {
            return true;
        }
        return false;
    }
}
