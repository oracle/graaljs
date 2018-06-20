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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class GlobalPropertyNode extends JSTargetableNode implements ReadNode {

    private final String propertyName;
    private final JSContext context;
    @Child private PropertyGetNode cache;
    @Child private JavaScriptNode globalObjectNode;

    protected GlobalPropertyNode(JSContext context, String propertyName, JavaScriptNode globalObjectNode) {
        this.propertyName = propertyName;
        this.context = context;
        this.globalObjectNode = globalObjectNode;
    }

    public static JSTargetableNode createPropertyNode(JSContext ctx, String propertyName) {
        if (JSTruffleOptions.NashornExtensions) {
            if (propertyName.equals("__LINE__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.LineNumberNode());
            } else if (propertyName.equals("__FILE__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.FileNameNode());
            } else if (propertyName.equals("__DIR__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.DirNameNode());
            }
        }
        return new GlobalPropertyNode(ctx, propertyName, null);
    }

    public static JSTargetableNode createLexicalGlobal(JSContext ctx, String propertyName, boolean checkTDZ) {
        JavaScriptNode globalScope = checkTDZ ? GlobalScopeNode.createWithTDZCheck(ctx, propertyName) : GlobalScopeNode.create(ctx);
        return new GlobalPropertyNode(ctx, propertyName, globalScope);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadVariableExpressionTag.class && isScopeAccess()) {
            return true;
        } else if (tag == ReadPropertyExpressionTag.class && !isScopeAccess()) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    private boolean isScopeAccess() {
        return globalObjectNode instanceof GlobalScopeNode;
    }

    @Override
    public Object getNodeObject() {
        if (isScopeAccess()) {
            return JSTags.createNodeObjectDescriptor("name", getPropertyKey());
        }
        return JSTags.createNodeObjectDescriptor("key", getPropertyKey());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadPropertyExpressionTag.class) && !isScopeAccess()) {
            GlobalObjectNode globalObject = GlobalObjectNode.create(context);
            PropertyNode propertyNode = PropertyNode.createProperty(context, globalObject, getPropertyKey());
            transferSourceSection(this, propertyNode);
            transferSourceSectionNoTags(this, globalObject);
            return propertyNode;
        } else {
            return this;
        }
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        try {
            return getCache().getValue(JSTypesGen.expectDynamicObject(target));
        } catch (UnexpectedResultException e) {
            throw new AssertionError("target must always be a JSObject");
        }
    }

    @Override
    public final Object evaluateTarget(VirtualFrame frame) {
        if (globalObjectNode != null) {
            return globalObjectNode.execute(frame);
        }
        return GlobalObjectNode.getGlobalObject(context);
    }

    @Override
    public JavaScriptNode getTarget() {
        return getGlobalObjectNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return getCache().getValue(evaluateTarget(frame));
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return getCache().getValueInt(evaluateTarget(frame));
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return getCache().getValueDouble(evaluateTarget(frame));
    }

    public String getPropertyKey() {
        return propertyName;
    }

    public void setMethod() {
        getCache().setMethod();
    }

    public void setPropertyAssumptionCheckEnabled(boolean enabled) {
        getCache().setPropertyAssumptionCheckEnabled(enabled);
    }

    private PropertyGetNode getCache() {
        if (cache == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.cache = insert(PropertyGetNode.create(propertyName, true, context));
        }
        return cache;
    }

    private JavaScriptNode getGlobalObjectNode() {
        if (globalObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.globalObjectNode = insert(GlobalObjectNode.create(context));
        }
        return globalObjectNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        GlobalPropertyNode copy = new GlobalPropertyNode(context, propertyName, cloneUninitialized(globalObjectNode));
        if (this.cache != null && this.cache.isMethod()) {
            copy.getCache().setMethod();
        }
        return copy;
    }

    @Override
    public String expressionToString() {
        return getPropertyKey();
    }
}
