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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class DeclareGlobalLexicalVariableNode extends DeclareGlobalNode {
    private final boolean isConst;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;

    protected DeclareGlobalLexicalVariableNode(String varName, boolean isConst) {
        super(varName);
        this.isConst = isConst;
        this.getOwnPropertyNode = JSGetOwnPropertyNode.create(false);
    }

    public static DeclareGlobalLexicalVariableNode create(String varName, boolean isConst) {
        return DeclareGlobalLexicalVariableNodeGen.create(varName, isConst);
    }

    @Override
    public void verify(JSContext context, JSRealm realm) {
        super.verify(context, realm);
        // HasRestrictedGlobalProperty
        PropertyDescriptor desc = getOwnPropertyNode.execute(realm.getGlobalObject(), varName);
        if (desc != null && !desc.getConfigurable()) {
            errorProfile.enter();
            throw Errors.createSyntaxErrorVariableAlreadyDeclared(varName, this);
        }
    }

    @Override
    public final void executeVoid(VirtualFrame frame, JSContext context, JSRealm realm) {
        DynamicObject globalScope = realm.getGlobalScope();
        assert !JSObject.hasOwnProperty(globalScope, varName); // checked in advance
        assert JSObject.isExtensible(globalScope);
        executeVoid(globalScope, context);
    }

    private int getAttributeFlags() {
        // Note: const variables are writable so as to not interfere with initialization (for now).
        // As a consequence, dynamically resolved const variable assignments need explicit guards.
        return isConst ? (JSAttributes.notConfigurableEnumerableWritable() | JSProperty.CONST) : JSAttributes.notConfigurableEnumerableWritable();
    }

    protected abstract void executeVoid(DynamicObject globalScope, JSContext context);

    @Specialization(guards = {"context.getPropertyCacheLimit() > 0"})
    protected void doCached(DynamicObject globalScope, @SuppressWarnings("unused") JSContext context,
                    @Cached("makeDefineOwnPropertyCache(context)") PropertySetNode cache) {
        cache.setValue(globalScope, Dead.instance());
    }

    @Specialization(replaces = {"doCached"})
    protected void doUncached(DynamicObject globalScope, JSContext context) {
        JSObjectUtil.putDeclaredDataProperty(context, globalScope, varName, Dead.instance(), getAttributeFlags());
    }

    protected final PropertySetNode makeDefineOwnPropertyCache(JSContext context) {
        return PropertySetNode.createImpl(varName, false, context, true, true, getAttributeFlags(), true);
    }

    @Override
    public boolean isLexicallyDeclared() {
        return true;
    }

    @Override
    protected DeclareGlobalNode copyUninitialized() {
        return create(varName, isConst);
    }
}
