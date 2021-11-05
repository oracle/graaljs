/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.DeclareTagProvider;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

public abstract class JSFunctionExpressionNode extends JavaScriptNode implements FunctionNameHolder {

    protected final JSFunctionData functionData;
    protected final FunctionRootNode functionNode;

    protected JSFunctionExpressionNode(JSFunctionData functionData, FunctionRootNode functionNode) {
        this.functionData = functionData;
        this.functionNode = functionNode;
    }

    public static JSFunctionExpressionNode create(JSFunctionData function) {
        assert !function.needsParentFrame();
        return new AutonomousFunctionExpressionNode(function, null);
    }

    public static JSFunctionExpressionNode create(JSFunctionData function, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot) {
        if (function.needsParentFrame()) {
            return new ClosureFunctionExpressionNode(function, functionNode, blockScopeSlot);
        } else {
            return new AutonomousFunctionExpressionNode(function, functionNode);
        }
    }

    public static JSFunctionExpressionNode createLexicalThis(JSFunctionData function, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot, JavaScriptNode thisNode) {
        if (function.needsParentFrame()) {
            return new LexicalThisClosureFunctionExpressionNode(function, functionNode, blockScopeSlot, thisNode);
        } else {
            return new LexicalThisAutonomousFunctionExpressionNode(function, functionNode, thisNode);
        }
    }

    public static JSFunctionExpressionNode createEmpty(JSContext context, int length, String sourceName) {
        return new AutonomousFunctionExpressionNode(JSFunctionData.create(context, context.getEmptyFunctionCallTarget(), length, sourceName), null);
    }

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

    public JSFunctionData getFunctionData() {
        return functionData;
    }

    public FunctionRootNode getFunctionNode() {
        CompilerAsserts.neverPartOfCompilation();
        return functionNode;
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
        private final JSFrameSlot blockScopeSlot;

        protected ClosureFunctionExpressionNode(JSFunctionData functionData, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot) {
            super(functionData, functionNode);
            this.blockScopeSlot = blockScopeSlot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            MaterializedFrame closureFrame;
            if (blockScopeSlot != null) {
                Object blockScope = frame.getObject(blockScopeSlot.getIndex());
                closureFrame = JSFrameUtil.castMaterializedFrame(blockScope);
            } else {
                closureFrame = frame.materialize();
            }
            return JSFunction.create(getRealm(), functionData, closureFrame);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ClosureFunctionExpressionNode(functionData, functionNode, blockScopeSlot);
        }
    }

    /**
     * Autonomous function expressions do not need access to the enclosing frame.
     */
    private static final class AutonomousFunctionExpressionNode extends JSFunctionExpressionNode {
        protected AutonomousFunctionExpressionNode(JSFunctionData functionData, FunctionRootNode functionNode) {
            super(functionData, functionNode);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return JSFunction.create(getRealm(), functionData);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new AutonomousFunctionExpressionNode(functionData, functionNode);
        }
    }

    private static final class LexicalThisClosureFunctionExpressionNode extends JSFunctionExpressionNode {
        @Child private JavaScriptNode thisNode;
        private final JSFrameSlot blockScopeSlot;

        protected LexicalThisClosureFunctionExpressionNode(JSFunctionData functionData, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot, JavaScriptNode thisNode) {
            super(functionData, functionNode);
            this.blockScopeSlot = blockScopeSlot;
            this.thisNode = thisNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            MaterializedFrame closureFrame;
            if (blockScopeSlot != null) {
                Object blockScope = frame.getObject(blockScopeSlot.getIndex());
                closureFrame = JSFrameUtil.castMaterializedFrame(blockScope);
            } else {
                closureFrame = frame.materialize();
            }
            return JSFunction.createLexicalThis(getRealm(), functionData, closureFrame, thisNode.execute(frame));
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new LexicalThisClosureFunctionExpressionNode(functionData, functionNode, blockScopeSlot, cloneUninitialized(thisNode, materializedTags));
        }
    }

    private static final class LexicalThisAutonomousFunctionExpressionNode extends JSFunctionExpressionNode {
        @Child private JavaScriptNode thisNode;

        protected LexicalThisAutonomousFunctionExpressionNode(JSFunctionData functionData, FunctionRootNode functionNode, JavaScriptNode thisNode) {
            super(functionData, functionNode);
            this.thisNode = thisNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return JSFunction.createLexicalThis(getRealm(), functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, thisNode.execute(frame));
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new LexicalThisAutonomousFunctionExpressionNode(functionData, functionNode, cloneUninitialized(thisNode, materializedTags));
        }
    }
}
