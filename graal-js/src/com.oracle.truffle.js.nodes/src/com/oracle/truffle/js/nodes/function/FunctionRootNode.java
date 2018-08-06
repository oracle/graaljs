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
package com.oracle.truffle.js.nodes.function;

import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.FrameDescriptorProvider;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData.Target;

@NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of all functions in JavaScript.")
public class FunctionRootNode extends JavaScriptRealmBoundaryRootNode implements FrameDescriptorProvider, JSFunctionData.CallTargetInitializer {

    @Child private JavaScriptNode body;

    private final JSFunctionData functionData;
    private String internalFunctionName;

    protected FunctionRootNode(AbstractBodyNode body, FrameDescriptor frameDescriptor, JSFunctionData functionData, SourceSection sourceSection, String internalFunctionName) {
        super(functionData.getContext().getLanguage(), sourceSection, frameDescriptor);
        this.body = body;
        this.body.addRootTag();
        if (!this.body.hasSourceSection()) {
            this.body.setSourceSection(sourceSection);
        }
        this.functionData = functionData;
        this.internalFunctionName = internalFunctionName;
    }

    public static FunctionRootNode create(AbstractBodyNode body, FrameDescriptor frameDescriptor, JSFunctionData functionData, SourceSection sourceSection, String internalFunctionName) {
        FunctionRootNode rootNode = new FunctionRootNode(body, frameDescriptor, functionData, sourceSection, internalFunctionName);
        if (JSTruffleOptions.TestCloneUninitialized) {
            return (FunctionRootNode) rootNode.cloneUninitialized();
        } else {
            return rootNode;
        }
    }

    public JSFunctionData getFunctionData() {
        return functionData;
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override
    protected JavaScriptRootNode cloneUninitialized() {
        return new FunctionRootNode((AbstractBodyNode) JavaScriptNode.cloneUninitialized(body), getFrameDescriptor(), functionData, getSourceSection(), internalFunctionName);
    }

    public boolean isInlineImmediately() {
        return functionData.isBuiltin();
    }

    @Override
    public String getName() {
        if (functionData.isBuiltin() || (functionData.getName().isEmpty() && internalFunctionName != null)) {
            assert internalFunctionName != null;
            return internalFunctionName;
        }
        return functionData.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    public JavaScriptNode getBody() {
        return body;
    }

    @Override
    protected JSContext getContext() {
        return functionData.getContext();
    }

    @Override
    protected Object executeInRealm(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    @TruffleBoundary
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> map = super.getDebugProperties();
        map.put("name", "function " + getName() + "(" + getParamCount() + "/" + getFrameDescriptor().getSize() + ")");
        return map;
    }

    public int getParamCount() {
        return functionData.getLength();
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public void initializeRoot(JSFunctionData fd) {
        fd.setRootTarget(Truffle.getRuntime().createCallTarget(this));
    }

    @Override
    public void initializeCallTarget(JSFunctionData fd, Target target, CallTarget rootTarget) {
        initializeFunctionDataCallTarget(fd, target, rootTarget, this);
    }

    private static void initializeFunctionDataCallTarget(JSFunctionData functionData, JSFunctionData.Target target, CallTarget rootTarget, FunctionRootNode functionRoot) {
        NodeFactory factory = NodeFactory.getDefaultInstance();
        if (target == JSFunctionData.Target.Call) {
            CallTarget functionCallTarget;
            if (functionData.requiresNew()) {
                functionCallTarget = Truffle.getRuntime().createCallTarget(factory.createConstructorRequiresNewRoot(functionData, functionRoot.getSourceSection()));
            } else {
                if (functionData.needsNewTarget()) {
                    // functions that use new.target are wrapped with a delegating call target that
                    // supplies an additional implicit newTarget argument to the original function.
                    functionCallTarget = Truffle.getRuntime().createCallTarget(factory.createNewTargetCall(rootTarget));
                } else {
                    functionCallTarget = rootTarget;
                }
            }
            functionData.setCallTarget(functionCallTarget);
        } else if (target == JSFunctionData.Target.Construct) {
            CallTarget constructCallTarget;
            if (functionData.isGenerator()) {
                constructCallTarget = functionData.getContext().getGeneratorNotConstructibleCallTarget();
            } else if (functionData.isAsync()) {
                constructCallTarget = functionData.getContext().getNotConstructibleCallTarget();
            } else {
                constructCallTarget = Truffle.getRuntime().createCallTarget(factory.createConstructorRootNode(functionData, rootTarget, false));
                if (functionData.needsNewTarget()) {
                    // functions that use new.target are wrapped with a delegating call target that
                    // supplies an additional implicit newTarget argument to the original function.
                    constructCallTarget = Truffle.getRuntime().createCallTarget(factory.createNewTargetConstruct(constructCallTarget));
                }
            }
            functionData.setConstructTarget(constructCallTarget);
        } else if (target == JSFunctionData.Target.ConstructNewTarget) {
            CallTarget newTargetCallTarget;
            if (functionData.needsNewTarget()) {
                newTargetCallTarget = rootTarget;
            } else {
                newTargetCallTarget = Truffle.getRuntime().createCallTarget(factory.createDropNewTarget(rootTarget));
            }
            CallTarget constructNewTargetCallTarget = Truffle.getRuntime().createCallTarget(factory.createConstructorRootNode(functionData, newTargetCallTarget, true));
            functionData.setConstructNewTarget(constructNewTargetCallTarget);
        }
    }
}
