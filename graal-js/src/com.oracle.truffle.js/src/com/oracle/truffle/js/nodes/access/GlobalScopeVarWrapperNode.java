/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.Errors;

/**
 * Wrapper around a global property access that handles potential lexical declarations shadowing
 * global properties.
 */
public final class GlobalScopeVarWrapperNode extends VarWrapperNode implements ReadNode, WriteNode {

    private final String varName;
    @Child private JavaScriptNode dynamicScopeNode;
    @Child private JavaScriptNode defaultDelegate;
    @Child private JSTargetableNode scopeAccessNode;
    @Child private GlobalScopeLookupNode scopeHasBinding;

    public GlobalScopeVarWrapperNode(String varName, JavaScriptNode defaultDelegate, JavaScriptNode dynamicScope, JSTargetableNode scopeAccessNode) {
        this.varName = varName;
        this.dynamicScopeNode = dynamicScope;
        this.defaultDelegate = Objects.requireNonNull(defaultDelegate);
        this.scopeAccessNode = scopeAccessNode;
        this.scopeHasBinding = GlobalScopeLookupNode.create(varName, isWrite());
    }

    @Override
    public JavaScriptNode getDelegateNode() {
        return defaultDelegate;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return false;
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    private boolean isWrite() {
        return scopeAccessNode instanceof WritePropertyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object dynamicScope = dynamicScopeNode.execute(frame);
        if (scopeHasBinding.execute(dynamicScope)) {
            if (isWrite()) {
                Object value = getRhs().execute(frame);
                ((WritePropertyNode) scopeAccessNode).executeWithValue(dynamicScope, value);
                return value;
            } else {
                // read or delete
                return scopeAccessNode.executeWithTarget(frame, dynamicScope);
            }
        }
        return defaultDelegate.execute(frame);
    }

    @Override
    public void executeWrite(VirtualFrame frame, Object value) {
        throw Errors.shouldNotReachHere();
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) JSNodeUtil.getWrappedNode(defaultDelegate)).getRhs();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new GlobalScopeVarWrapperNode(varName, cloneUninitialized(defaultDelegate, materializedTags), cloneUninitialized(dynamicScopeNode, materializedTags),
                        cloneUninitialized(scopeAccessNode, materializedTags));
    }

    public void setMethod() {
        if (defaultDelegate instanceof GlobalPropertyNode) {
            ((GlobalPropertyNode) defaultDelegate).setMethod();
        }
    }
}
