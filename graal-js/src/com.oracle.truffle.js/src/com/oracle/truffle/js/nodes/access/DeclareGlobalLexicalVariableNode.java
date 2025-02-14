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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

/**
 * Instantiates a global lexical (let or const) declaration.
 */
public abstract class DeclareGlobalLexicalVariableNode extends DeclareGlobalNode {
    private final boolean isConst;
    @Child private HasRestrictedGlobalPropertyNode hasRestrictedGlobalPropertyNode;

    protected DeclareGlobalLexicalVariableNode(TruffleString varName, boolean isConst) {
        super(varName);
        this.isConst = isConst;
        this.hasRestrictedGlobalPropertyNode = HasRestrictedGlobalPropertyNodeGen.create();
    }

    public static DeclareGlobalLexicalVariableNode create(TruffleString varName, boolean isConst) {
        return DeclareGlobalLexicalVariableNodeGen.create(varName, isConst);
    }

    @Override
    public void verify(JSContext context, JSRealm realm) {
        super.verify(context, realm);
        if (hasRestrictedGlobalPropertyNode.execute(realm.getGlobalObject(), varName)) {
            errorProfile.enter();
            throw Errors.createSyntaxErrorVariableAlreadyDeclared(varName, this);
        }
    }

    @Override
    public final void executeVoid(JSContext context, JSRealm realm) {
        JSDynamicObject globalScope = realm.getGlobalScope();
        assert !JSObject.hasOwnProperty(globalScope, varName); // checked in advance
        assert JSObject.isExtensible(globalScope);
        executeVoid(globalScope, context);
    }

    private int getAttributeFlags() {
        // Note: const variables are writable so as to not interfere with initialization (for now).
        // As a consequence, dynamically resolved const variable assignments need explicit guards.
        return isConst ? (JSAttributes.notConfigurableEnumerableWritable() | JSProperty.CONST) : JSAttributes.notConfigurableEnumerableWritable();
    }

    protected abstract void executeVoid(JSDynamicObject globalScope, JSContext context);

    @Specialization(guards = {"context.getPropertyCacheLimit() > 0"})
    protected void doCached(JSDynamicObject globalScope, @SuppressWarnings("unused") JSContext context,
                    @Cached("makeDefineOwnPropertyCache(context)") PropertySetNode cache) {
        cache.setValue(globalScope, Dead.instance());
    }

    @Specialization(replaces = {"doCached"})
    protected void doUncached(JSDynamicObject globalScope, JSContext context) {
        JSObjectUtil.defineConstantDataProperty(context, globalScope, varName, Dead.instance(), getAttributeFlags());
    }

    @NeverDefault
    protected final PropertySetNode makeDefineOwnPropertyCache(JSContext context) {
        return PropertySetNode.createImpl(varName, false, context, true, true, getAttributeFlags(), true);
    }

    @Override
    public boolean isLexicallyDeclared() {
        return true;
    }

    @Override
    protected DeclareGlobalNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(varName, isConst);
    }
}

/**
 * Checks if HasRestrictedGlobalProperty is true for this binding.
 *
 * The object must be either a {@link JSGlobalObject} or a {@link JSProxyObject}.
 */
abstract class HasRestrictedGlobalPropertyNode extends JavaScriptBaseNode {
    protected HasRestrictedGlobalPropertyNode() {
    }

    public abstract boolean execute(JSDynamicObject object, Object key);

    @Specialization(guards = {
                    "cachedShape == thisObj.getShape()"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "3")
    static boolean doGlobalObjectCached(@SuppressWarnings("unused") JSGlobalObject thisObj, Object propertyKey,
                    @Cached("thisObj.getShape()") @SuppressWarnings("unused") Shape cachedShape,
                    @Cached("cachedShape.getProperty(propertyKey)") Property cachedProperty) {
        CompilerAsserts.partialEvaluationConstant(propertyKey);
        return cachedProperty != null && !JSProperty.isConfigurable(cachedProperty.getFlags());
    }

    @Specialization(replaces = "doGlobalObjectCached")
    static boolean doGlobalObjectUncached(JSGlobalObject thisObj, Object propertyKey) {
        CompilerAsserts.partialEvaluationConstant(propertyKey);
        Property property = thisObj.getShape().getProperty(propertyKey);
        return property != null && !JSProperty.isConfigurable(property.getFlags());
    }

    @Specialization
    static boolean doProxy(JSProxyObject thisObj, Object propertyKey) {
        CompilerAsserts.partialEvaluationConstant(propertyKey);
        PropertyDescriptor desc = JSObject.getOwnProperty(thisObj, propertyKey);
        return desc != null && !desc.getConfigurable();
    }
}
