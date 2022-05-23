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
package com.oracle.truffle.js.nodes.function;

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.DeclareTagProvider;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

public abstract class JSFunctionExpressionNode extends JavaScriptNode implements FunctionNameHolder {

    protected final JSFunctionData functionData;

    protected JSFunctionExpressionNode(JSFunctionData functionData) {
        this.functionData = functionData;
    }

    public static JSFunctionExpressionNode create(JSFunctionData function) {
        assert !function.needsParentFrame();
        return new AutonomousFunctionExpressionNode(function);
    }

    public static JSFunctionExpressionNode create(JSFunctionData function, FrameSlot blockScopeSlot) {
        if (function.needsParentFrame()) {
            return new ClosureFunctionExpressionNode(function, blockScopeSlot);
        } else {
            return new AutonomousFunctionExpressionNode(function);
        }
    }

    public static JSFunctionExpressionNode createLexicalThis(JSFunctionData function, FrameSlot blockScopeSlot, JavaScriptNode thisNode) {
        if (function.needsParentFrame()) {
            return new LexicalThisClosureFunctionExpressionNode(function, blockScopeSlot, thisNode);
        } else {
            return new LexicalThisAutonomousFunctionExpressionNode(function, thisNode);
        }
    }

    public static JSFunctionExpressionNode createEmpty(JSContext context, int length, String sourceName) {
        return new AutonomousFunctionExpressionNode(JSFunctionData.create(context, context.getEmptyFunctionCallTarget(), length, sourceName));
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeWithRealm(frame, getRealm());
    }

    public abstract Object executeWithRealm(VirtualFrame frame, JSRealm realm);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralTag.class) {
            return true;
        } else if (tag == JSTags.InputNodeTag.class) {
            return true;
        } else if (tag == JSTags.DeclareTag.class) {
            // a function not tagged as an expression is a declaration
            return !super.hasTag(ExpressionTag.class);
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        if (super.hasTag(ExpressionTag.class)) {
            return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.FunctionLiteral.name());
        }
        // function declaration, add declaration data and combine with literal type
        NodeObjectDescriptor descriptor = DeclareTagProvider.createDeclareNodeObject(functionData.getName(), "var");
        descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.FunctionLiteral.name());
        return descriptor;
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (JSConfig.LazyFunctionData && !materializedTags.isEmpty()) {
            // when instruments require the materialization of function expression nodes, we force
            // initialization.
            functionData.materialize();
        }
        return this;
    }

    public final JSFunctionData getFunctionData() {
        return functionData;
    }

    @Override
    public String getFunctionName() {
        return functionData.getName();
    }

    @Override
    public void setFunctionName(String name) {
        CompilerAsserts.neverPartOfCompilation();
        functionData.setName(name);
    }

    private static final class ClosureFunctionExpressionNode extends JSFunctionExpressionNode {
        private final FrameSlot blockScopeSlot;

        protected ClosureFunctionExpressionNode(JSFunctionData functionData, FrameSlot blockScopeSlot) {
            super(functionData);
            this.blockScopeSlot = blockScopeSlot;
        }

        @Override
        public Object executeWithRealm(VirtualFrame frame, JSRealm realm) {
            MaterializedFrame closureFrame;
            if (blockScopeSlot != null) {
                Object blockScope = FrameUtil.getObjectSafe(frame, blockScopeSlot);
                closureFrame = JSFrameUtil.castMaterializedFrame(blockScope);
            } else {
                closureFrame = frame.materialize();
            }
            return JSFunction.create(realm, functionData, closureFrame);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ClosureFunctionExpressionNode(functionData, blockScopeSlot);
        }
    }

    /**
     * Autonomous function expressions do not need access to the enclosing frame.
     */
    private static final class AutonomousFunctionExpressionNode extends JSFunctionExpressionNode {
        protected AutonomousFunctionExpressionNode(JSFunctionData functionData) {
            super(functionData);
        }

        @Override
        public Object executeWithRealm(VirtualFrame frame, JSRealm realm) {
            return JSFunction.create(realm, functionData);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new AutonomousFunctionExpressionNode(functionData);
        }
    }

    private static final class LexicalThisClosureFunctionExpressionNode extends JSFunctionExpressionNode {
        @Child private JavaScriptNode thisNode;
        private final FrameSlot blockScopeSlot;

        protected LexicalThisClosureFunctionExpressionNode(JSFunctionData functionData, FrameSlot blockScopeSlot, JavaScriptNode thisNode) {
            super(functionData);
            this.blockScopeSlot = blockScopeSlot;
            this.thisNode = thisNode;
        }

        @Override
        public Object executeWithRealm(VirtualFrame frame, JSRealm realm) {
            MaterializedFrame closureFrame;
            if (blockScopeSlot != null) {
                Object blockScope = FrameUtil.getObjectSafe(frame, blockScopeSlot);
                closureFrame = JSFrameUtil.castMaterializedFrame(blockScope);
            } else {
                closureFrame = frame.materialize();
            }
            return JSFunction.createLexicalThis(realm, functionData, closureFrame, thisNode.execute(frame));
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new LexicalThisClosureFunctionExpressionNode(functionData, blockScopeSlot, cloneUninitialized(thisNode, materializedTags));
        }
    }

    private static final class LexicalThisAutonomousFunctionExpressionNode extends JSFunctionExpressionNode {
        @Child private JavaScriptNode thisNode;

        protected LexicalThisAutonomousFunctionExpressionNode(JSFunctionData functionData, JavaScriptNode thisNode) {
            super(functionData);
            this.thisNode = thisNode;
        }

        @Override
        public Object executeWithRealm(VirtualFrame frame, JSRealm realm) {
            return JSFunction.createLexicalThis(realm, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, thisNode.execute(frame));
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new LexicalThisAutonomousFunctionExpressionNode(functionData, cloneUninitialized(thisNode, materializedTags));
        }
    }
}
