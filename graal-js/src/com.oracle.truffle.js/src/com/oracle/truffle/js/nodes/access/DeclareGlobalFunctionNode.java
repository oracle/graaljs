/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Set;

public abstract class DeclareGlobalFunctionNode extends DeclareGlobalNode {
    private final boolean configurable;
    @Child protected JavaScriptNode valueNode;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;
    @Child private IsExtensibleNode isExtensibleNode = IsExtensibleNode.create();

    protected DeclareGlobalFunctionNode(String varName, boolean configurable, JavaScriptNode valueNode) {
        super(varName);
        this.configurable = configurable;
        this.valueNode = valueNode;
        this.getOwnPropertyNode = JSGetOwnPropertyNode.create(false);
    }

    public static DeclareGlobalFunctionNode create(String varName, boolean configurable, JavaScriptNode valueNode) {
        return DeclareGlobalFunctionNodeGen.create(varName, configurable, valueNode);
    }

    @Override
    public void verify(JSContext context, JSRealm realm) {
        super.verify(context, realm);
        // CanDeclareGlobalFunction
        DynamicObject globalObject = realm.getGlobalObject();
        PropertyDescriptor desc = getOwnPropertyNode.execute(globalObject, varName);
        if (desc == null) {
            if (!isExtensibleNode.executeBoolean(globalObject)) {
                errorProfile.enter();
                throw Errors.createTypeErrorGlobalObjectNotExtensible(this);
            }
        } else {
            if (!desc.getConfigurable() && !(desc.isDataDescriptor() && desc.getWritable() && desc.getEnumerable())) {
                errorProfile.enter();
                throw Errors.createTypeErrorCannotDeclareGlobalFunction(varName, this);
            }
        }
    }

    @Override
    public final void executeVoid(VirtualFrame frame, JSContext context, JSRealm realm) {
        Object value = valueNode == null ? Undefined.instance : valueNode.execute(frame);
        DynamicObject globalObject = realm.getGlobalObject();
        PropertyDescriptor desc = getOwnPropertyNode.execute(globalObject, varName);
        executeVoid(globalObject, value, desc, context);
    }

    protected abstract void executeVoid(DynamicObject globalObject, Object value, PropertyDescriptor desc, JSContext context);

    @Specialization(guards = {"context.getPropertyCacheLimit() > 0", "isJSGlobalObject(globalObject)", "desc == null"})
    protected void doCached(DynamicObject globalObject, Object value, @SuppressWarnings("unused") PropertyDescriptor desc, @SuppressWarnings("unused") JSContext context,
                    @Cached("makeDefineOwnPropertyCache(context)") PropertySetNode cache) {
        cache.setValue(globalObject, value);
    }

    @Specialization(replaces = {"doCached"})
    protected void doUncached(DynamicObject globalObject, Object value, PropertyDescriptor desc, JSContext context) {
        if (valueNode == null && desc == null && JSGlobal.isJSGlobalObject(globalObject)) {
            JSObjectUtil.putDeclaredDataProperty(context, globalObject, varName, value, getAttributeFlags());
        } else {
            if (desc == null || desc.getConfigurable()) {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value, true, true, configurable), true);
            } else {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value), true);
            }
        }
    }

    private int getAttributeFlags() {
        return configurable ? JSAttributes.configurableEnumerableWritable() : JSAttributes.notConfigurableEnumerableWritable();
    }

    protected final PropertySetNode makeDefineOwnPropertyCache(JSContext context) {
        return PropertySetNode.createImpl(varName, false, context, true, true, getAttributeFlags(), valueNode == null);
    }

    @Override
    public boolean isGlobalFunctionDeclaration() {
        return true;
    }

    @Override
    protected DeclareGlobalNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(varName, configurable, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
    }
}
