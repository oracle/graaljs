/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class DeclareGlobalVariableNode extends DeclareGlobalNode {
    private final boolean configurable;
    @Child private HasPropertyCacheNode hasOwnPropertyNode;
    @Child private IsExtensibleNode isExtensibleNode = IsExtensibleNode.create();

    protected DeclareGlobalVariableNode(TruffleString varName, boolean configurable) {
        super(varName);
        this.configurable = configurable;
    }

    public static DeclareGlobalVariableNode create(TruffleString varName, boolean configurable) {
        return DeclareGlobalVariableNodeGen.create(varName, configurable);
    }

    @Override
    public void verify(JSContext context, JSRealm realm) {
        super.verify(context, realm);
        JSDynamicObject globalObject = realm.getGlobalObject();
        if (hasOwnPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasOwnPropertyNode = insert(HasPropertyCacheNode.create(varName, context, true));
        }
        // CanDeclareGlobalVar
        if (!hasOwnPropertyNode.hasProperty(globalObject)) {
            if (!isExtensibleNode.executeBoolean(globalObject)) {
                errorProfile.enter();
                throw Errors.createTypeErrorGlobalObjectNotExtensible(this);
            }
        }
    }

    @Override
    public final void executeVoid(JSContext context, JSRealm realm) {
        JSDynamicObject globalObject = realm.getGlobalObject();
        if (!hasOwnPropertyNode.hasProperty(globalObject)) {
            assert JSObject.isExtensible(globalObject);
            executeVoid(globalObject, context);
        }
    }

    protected abstract void executeVoid(JSDynamicObject globalObject, JSContext context);

    @Specialization(guards = {"context.getPropertyCacheLimit() > 0"})
    protected void doCached(JSGlobalObject globalObject, @SuppressWarnings("unused") JSContext context,
                    @Cached("makeDefineOwnPropertyCache(context)") PropertySetNode cache) {
        cache.setValue(globalObject, Undefined.instance);
    }

    @Specialization(replaces = {"doCached"})
    protected void doUncached(JSGlobalObject globalObject, JSContext context) {
        JSObjectUtil.defineConstantDataProperty(context, globalObject, varName, Undefined.instance, getAttributeFlags());
    }

    @Fallback
    protected void doGeneric(JSDynamicObject globalObject, @SuppressWarnings("unused") JSContext context) {
        assert !(globalObject instanceof JSGlobalObject);
        PropertyDescriptor desc = configurable ? PropertyDescriptor.undefinedDataDesc : PropertyDescriptor.undefinedDataDescNotConfigurable;
        JSObject.defineOwnProperty(globalObject, varName, desc, true);
    }

    private int getAttributeFlags() {
        return (configurable ? JSAttributes.configurableEnumerableWritable() : JSAttributes.notConfigurableEnumerableWritable());
    }

    @NeverDefault
    protected final PropertySetNode makeDefineOwnPropertyCache(JSContext context) {
        return PropertySetNode.createImpl(varName, false, context, true, true, getAttributeFlags(), true);
    }

    @Override
    protected DeclareGlobalNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(varName, configurable);
    }
}
