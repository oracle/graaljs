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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSProxyCallNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.instrumentation.JSInputGeneratingNodeWrapper;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedTargetableExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.interop.ExportArgumentsNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.FlattenNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSNoSuchMethodAdapter;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptFunctionCallNode;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.interop.Converters;
import com.oracle.truffle.js.runtime.interop.Converters.Converter;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaMethod.AbstractJavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.DebugCounter;
import com.oracle.truffle.js.runtime.util.Pair;

public abstract class JSFunctionCallNode extends JavaScriptNode implements JavaScriptFunctionCallNode {
    private static final DebugCounter megamorphicCount = DebugCounter.create("Megamorphic call site count");

    static final byte CALL = 0;
    static final byte NEW = 1;
    static final byte NEW_TARGET = 2;

    protected final byte flags;
    @Child protected AbstractCacheNode cacheNode;

    protected JSFunctionCallNode(byte flags) {
        this.flags = flags;
    }

    public static JSFunctionCallNode createCall() {
        return create(false);
    }

    public static JSFunctionCallNode createNew() {
        return create(true);
    }

    public static JSFunctionCallNode createNewTarget() {
        return create(false, true);
    }

    public static JSFunctionCallNode create(boolean isNew) {
        return create(isNew, false);
    }

    public static JSFunctionCallNode create(boolean isNew, boolean isNewTarget) {
        return new ExecuteCallNode(flags(isNew, isNewTarget));
    }

    private static byte flags(boolean isNew, boolean isNewTarget) {
        return isNewTarget ? NEW_TARGET : (isNew ? NEW : CALL);
    }

    public static JSFunctionCallNode create(JavaScriptNode function, JavaScriptNode target, AbstractFunctionArgumentsNode arguments, boolean isNew, boolean isNewTarget) {
        return new CallNode(target, function, arguments, flags(isNew, isNewTarget));
    }

    public static JSFunctionCallNode createInvoke(JSTargetableNode targetFunction, AbstractFunctionArgumentsNode arguments, boolean isNew, boolean isNewTarget) {
        return new InvokeNode(targetFunction, arguments, flags(isNew, isNewTarget));
    }

    private AbstractCacheNode createUninitializedCache() {
        return new UninitializedCacheNode(flags, JSTruffleOptions.FunctionCacheLimit <= 0);
    }

    static boolean isNewTarget(byte flags) {
        return (flags & NEW_TARGET) != 0;
    }

    static boolean isNew(byte flags) {
        return (flags & NEW) != 0;
    }

    public final boolean isNew() {
        return isNew(flags);
    }

    public final boolean isInvoke() {
        return this instanceof InvokeNode;
    }

    protected abstract Object getPropertyKey();

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == FunctionCallExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("isNew", isNew());
        descriptor.addProperty("isInvoke", isInvoke());
        return descriptor;
    }

    /**
     * @param arguments function, this, ...rest
     */
    public static JSFunctionCallNode createInternalCall(JavaScriptNode[] arguments) {
        return create(arguments[0], arguments[1], JSFunctionArgumentsNode.create(Arrays.copyOfRange(arguments, 2, arguments.length)), false, false);
    }

    public final Object executeCall(Object[] arguments) {
        if (cacheNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cacheNode = insert(createUninitializedCache());
        }
        return cacheNode.executeCall(arguments);
    }

    static class CallNode extends JSFunctionCallNode {
        /**
         * May be {@code null}, the target value is {@code undefined}, then.
         */
        @Child protected JavaScriptNode targetNode;
        @Child protected JavaScriptNode functionNode;
        @Child protected AbstractFunctionArgumentsNode argumentsNode;

        protected CallNode(JavaScriptNode targetNode, JavaScriptNode functionNode, AbstractFunctionArgumentsNode argumentsNode, byte flags) {
            super(flags);
            this.targetNode = targetNode;
            this.functionNode = functionNode;
            this.argumentsNode = argumentsNode;
        }

        @Override
        public final JavaScriptNode getTarget() {
            return targetNode;
        }

        public final JavaScriptNode getFunction() {
            return functionNode;
        }

        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object[] arguments = JSArguments.createInitial(target, function, argumentsNode.getCount(frame));
            return argumentsNode.executeFillObjectArray(frame, arguments, JSArguments.RUNTIME_ARGUMENT_COUNT);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object target = executeTarget(frame);
            Object receiver = evaluateReceiver(frame, target);
            Object function = functionNode.execute(frame);
            // Note that the arguments must not be evaluated before the target.
            return executeCall(createArguments(frame, receiver, function));
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (materializedTags.contains(FunctionCallExpressionTag.class)) {
                if (this.hasSourceSection() && !functionNode.hasSourceSection()) {
                    transferSourceSectionAddExpressionTag(this, functionNode);
                }
                if (targetNode != null) {
                    // if we have a target, no de-sugaring needed
                    return this;
                } else {
                    JavaScriptNode materializedTargetNode = JSInputGeneratingNodeWrapper.create(JSConstantUndefinedNode.createUndefined());
                    AbstractFunctionArgumentsNode materializedArgumentsNode = argumentsNode.copyUninitialized();
                    JavaScriptNode call = CallNode.create(functionNode, materializedTargetNode, materializedArgumentsNode, isNew(flags), isNewTarget(flags));
                    transferSourceSectionAndTags(this, call);
                    return call;
                }
            } else {
                return this;
            }
        }

        final Object executeTarget(VirtualFrame frame) {
            if (targetNode != null) {
                return targetNode.execute(frame);
            } else {
                return Undefined.instance;
            }
        }

        private Object evaluateReceiver(VirtualFrame frame, Object target) {
            if (targetNode instanceof SuperPropertyReferenceNode) {
                return ((SuperPropertyReferenceNode) targetNode).evaluateTarget(frame);
            } else if (targetNode instanceof WrapperNode) {
                WrapperNode wrapper = (WrapperNode) targetNode;
                if (wrapper.getDelegateNode() instanceof SuperPropertyReferenceNode) {
                    return ((SuperPropertyReferenceNode) wrapper.getDelegateNode()).evaluateTarget(frame);
                }
            }
            return target;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new CallNode(cloneUninitialized(targetNode), cloneUninitialized(functionNode), AbstractFunctionArgumentsNode.cloneUninitialized(argumentsNode), flags);
        }

        @Override
        public String expressionToString() {
            return Objects.toString(functionNode.expressionToString(), INTERMEDIATE_VALUE) + "(...)";
        }

        @Override
        protected Object getPropertyKey() {
            if (functionNode instanceof PropertyNode) {
                return ((PropertyNode) functionNode).getPropertyKey();
            }
            return null;
        }
    }

    /**
     * The target of {@link #functionTargetNode} also serves as the this argument of the call. If
     * {@code true}, target not only serves as the this argument of the call, but also the target
     * object for the member expression that retrieves the function.
     */
    public static class InvokeNode extends JSFunctionCallNode {
        @Child private JSTargetableNode functionTargetNode;
        @Child private AbstractFunctionArgumentsNode argumentsNode;

        protected InvokeNode(byte flags) {
            super(flags);
        }

        protected InvokeNode(JSTargetableNode functionTargetNode, AbstractFunctionArgumentsNode argumentsNode, byte flags) {
            super(flags);
            this.functionTargetNode = functionTargetNode;
            this.argumentsNode = argumentsNode;
        }

        @Override
        public JavaScriptNode getTarget() {
            return functionTargetNode.getTarget();
        }

        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object[] arguments = JSArguments.createInitial(target, function, getArgumentsNode().getCount(frame));
            return getArgumentsNode().executeFillObjectArray(frame, arguments, JSArguments.RUNTIME_ARGUMENT_COUNT);
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            Object target = executeTarget(frame);
            Object receiver = evaluateReceiver(frame, target);
            Object function = executeFunctionWithTarget(frame, target);
            // Note that the arguments must not be evaluated before the target.
            return executeCall(createArguments(frame, receiver, function));
        }

        protected Object executeTarget(VirtualFrame frame) {
            return functionTargetNode.evaluateTarget(frame);
        }

        final Object executeFunctionWithTarget(VirtualFrame frame, Object target) {
            return getFunctionTargetNode().executeWithTarget(frame, target);
        }

        private Object evaluateReceiver(VirtualFrame frame, Object target) {
            Node targetNode = getTarget();
            if (targetNode instanceof SuperPropertyReferenceNode) {
                return ((SuperPropertyReferenceNode) targetNode).evaluateTarget(frame);
            } else if (targetNode instanceof WrapperNode) {
                WrapperNode wrapper = (WrapperNode) targetNode;
                if (wrapper.getDelegateNode() instanceof SuperPropertyReferenceNode) {
                    return ((SuperPropertyReferenceNode) wrapper.getDelegateNode()).evaluateTarget(frame);
                }
            }
            return target;
        }

        public JSTargetableNode getFunctionTargetNode() {
            return functionTargetNode;
        }

        protected AbstractFunctionArgumentsNode getArgumentsNode() {
            return argumentsNode;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InvokeNode(cloneUninitialized(getFunctionTargetNode()), AbstractFunctionArgumentsNode.cloneUninitialized(getArgumentsNode()), flags);
        }

        @Override
        public String expressionToString() {
            return Objects.toString(getFunctionTargetNode().expressionToString(), INTERMEDIATE_VALUE) + "(...)";
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (this instanceof MaterializedInvokeNode) {
                return this;
            }
            if (materializedTags.contains(FunctionCallExpressionTag.class) || materializedTags.contains(ReadPropertyExpressionTag.class) ||
                            materializedTags.contains(ReadElementExpressionTag.class)) {
                AbstractFunctionArgumentsNode materializedArgumentsNode = getArgumentsNode().copyUninitialized();
                JSTargetableNode clonedTarget = cloneUninitialized(getFunctionTargetNode());
                transferSourceSectionAddExpressionTag(this, clonedTarget);
                JavaScriptNode[] clonedArgumentsNodes = materializedArgumentsNode.getJavaScriptArgumentNodes();
                JavaScriptNode[] originalArgumentsNodes = getArgumentsNode().getJavaScriptArgumentNodes();
                for (int i = 0; i < clonedArgumentsNodes.length; i++) {
                    transferSourceSectionAddExpressionTag(originalArgumentsNodes[i], clonedArgumentsNodes[i]);
                }
                JavaScriptNode call = new MaterializedInvokeNode(clonedTarget, materializedArgumentsNode, flags);
                transferSourceSectionAndTags(this, call);
                return call;
            } else {
                return this;
            }
        }

        @Override
        protected Object getPropertyKey() {
            if (functionTargetNode instanceof PropertyNode) {
                return ((PropertyNode) functionTargetNode).getPropertyKey();
            }
            return null;
        }
    }

    /**
     * Materialized version of {@link InvokeNode}. Used by the instrumentation framework when invoke
     * calls are instrumented.
     */
    static final class MaterializedInvokeNode extends InvokeNode {

        @Child private JavaScriptNode targetNode;
        @Child private JSTargetableNode functionTargetNode;
        @Child private AbstractFunctionArgumentsNode argumentsNode;

        protected MaterializedInvokeNode(JSTargetableNode functionTargetNode, AbstractFunctionArgumentsNode argumentsNode, byte flags) {
            super(flags);
            this.argumentsNode = argumentsNode;
            this.functionTargetNode = createEventEmittingWrapper(functionTargetNode, functionTargetNode.getSourceSection());
            this.targetNode = functionTargetNode.getTarget();
            transferSourceSectionAndTags(functionTargetNode, this.targetNode);
            transferSourceSectionAndTags(functionTargetNode, this.functionTargetNode);
        }

        private JSTargetableNode createEventEmittingWrapper(JSTargetableNode functionTarget, SourceSection sourceSection) {
            assert sourceSection != null;
            if (functionTarget instanceof WrapperNode) {
                JSTargetableNode delegate = (JSTargetableNode) ((WrapperNode) functionTarget).getDelegateNode();
                return createEventEmittingWrapper(delegate, sourceSection);
            } else if (functionTarget instanceof JSTaggedTargetableExecutionNode) {
                JSTargetableNode delegate = ((JSTaggedTargetableExecutionNode) functionTarget).getChild();
                return createEventEmittingWrapper(delegate, sourceSection);
            } else {
                assert functionTarget instanceof PropertyNode || functionTarget instanceof ReadElementNode || functionTarget instanceof GlobalConstantNode;
                return JSTaggedTargetableExecutionNode.createFor(functionTarget, sourceSection);
            }
        }

        @Override
        protected Object executeTarget(VirtualFrame frame) {
            return targetNode.execute(frame);
        }

        @Override
        public JavaScriptNode getTarget() {
            return targetNode;
        }

        @Override
        public JSTargetableNode getFunctionTargetNode() {
            return functionTargetNode;
        }

        @Override
        protected AbstractFunctionArgumentsNode getArgumentsNode() {
            return argumentsNode;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new MaterializedInvokeNode(cloneUninitialized(getFunctionTargetNode()), AbstractFunctionArgumentsNode.cloneUninitialized(getArgumentsNode()), flags);
        }

        @Override
        protected Object getPropertyKey() {
            if (functionTargetNode instanceof JSTaggedTargetableExecutionNode) {
                return maybeGetPropertyKey(((JSTaggedTargetableExecutionNode) functionTargetNode).getChild());
            } else if (functionTargetNode instanceof WrapperNode) {
                return maybeGetPropertyKey(((WrapperNode) functionTargetNode).getDelegateNode());
            } else {
                return maybeGetPropertyKey(functionTargetNode);
            }
        }

        private static Object maybeGetPropertyKey(Node node) {
            if (node instanceof JSTaggedTargetableExecutionNode) {
                return maybeGetPropertyKey(((JSTaggedTargetableExecutionNode) node).getChild());
            } else if (node instanceof WrapperNode) {
                return maybeGetPropertyKey((((WrapperNode) node).getDelegateNode()));
            } else if (node instanceof PropertyNode) {
                return ((PropertyNode) node).getPropertyKey();
            }
            return null;
        }
    }

    static class ExecuteCallNode extends JSFunctionCallNode {
        protected ExecuteCallNode(byte flags) {
            super(flags);
        }

        @Override
        public final JavaScriptNode getTarget() {
            return null;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere();
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new ExecuteCallNode(flags);
        }

        @Override
        protected Object getPropertyKey() {
            return null;
        }
    }

    protected static JSDirectCallNode createCallableNode(DynamicObject function, boolean isNew, boolean isNewTarget, boolean cacheOnInstance) {
        CallTarget callTarget = getCallTarget(function, isNew, isNewTarget);
        assert callTarget != null;
        if (JSFunction.isBoundFunction(function)) {
            if (cacheOnInstance) {
                return new BoundCallNode(function, isNew, isNewTarget);
            } else {
                return new DynamicBoundCallNode(isNew, isNewTarget);
            }
        } else {
            JSDirectCallNode node = tryInlineBuiltinFunctionCall(callTarget);
            if (node != null) {
                return node;
            }

            return new DispatchedCallNode(callTarget);
        }
    }

    protected static CallTarget getCallTarget(DynamicObject function, boolean isNew, boolean isNewTarget) {
        if (isNewTarget) {
            return JSFunction.getConstructNewTarget(function);
        } else if (isNew) {
            return JSFunction.getConstructTarget(function);
        } else {
            return JSFunction.getCallTarget(function);
        }
    }

    private static JSDirectCallNode tryInlineBuiltinFunctionCall(CallTarget callTarget) {
        if (!JSTruffleOptions.InlineTrivialBuiltins) {
            return null;
        }
        if (callTarget instanceof RootCallTarget) {
            RootNode rootNode = ((RootCallTarget) callTarget).getRootNode();
            if (rootNode instanceof FunctionRootNode) {
                JavaScriptNode body = ((FunctionRootNode) rootNode).getBody();
                if (body instanceof JSBuiltinNode) {
                    JSBuiltinNode builtinNode = (JSBuiltinNode) body;
                    JSBuiltinNode.Inlined inlined = builtinNode.tryCreateInlined();
                    if (inlined != null) {
                        return new InlinedBuiltinCallNode(callTarget, inlined);
                    }
                }
            }
        }
        return null;
    }

    public abstract static class JSDirectCallNode extends JavaScriptBaseNode {
        protected JSDirectCallNode() {
        }

        public abstract Object executeCall(Object[] arguments);
    }

    private static final class FunctionInstanceCacheNode extends CacheNode {

        private final DynamicObject functionObj;

        FunctionInstanceCacheNode(DynamicObject functionObj, JSDirectCallNode current, AbstractCacheNode next) {
            super(current, next);
            assert JSFunction.isJSFunction(functionObj);
            this.functionObj = functionObj;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return functionObj == function;
        }

    }

    private static final class FunctionDataCacheNode extends CacheNode {
        private final JSFunctionData functionData;

        FunctionDataCacheNode(JSFunctionData functionData, JSDirectCallNode current, AbstractCacheNode next) {
            super(current, next);
            this.functionData = functionData;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }
    }

    private static final class CallForeignTargetCacheNode extends CacheNode {
        private final boolean needsForeignThis;

        CallForeignTargetCacheNode(boolean needsForeignThis, JSDirectCallNode current, AbstractCacheNode next) {
            super(current, next);
            this.needsForeignThis = needsForeignThis;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return (!needsForeignThis || JSGuards.isForeignObject(thisObject)) && JSGuards.isForeignObject(function);
        }

    }

    private static final class JavaCacheNode extends CacheNode {
        private final Object method;

        JavaCacheNode(Object method, JSDirectCallNode current, AbstractCacheNode next) {
            super(current, next);
            assert method instanceof JavaMethod || method instanceof JavaClass || JavaPackage.isJavaPackage(method);
            this.method = method;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return function == method;
        }

    }

    private abstract static class AbstractCacheNode extends JavaScriptBaseNode {
        public abstract Object executeCall(Object[] arguments);
    }

    private abstract static class CacheNode extends AbstractCacheNode {

        @Child protected AbstractCacheNode nextNode;
        @Child protected JSDirectCallNode currentNode;

        CacheNode(JSDirectCallNode current, AbstractCacheNode next) {
            this.currentNode = current;
            this.nextNode = next;
        }

        protected abstract boolean accept(Object thisObject, Object function);

        @Override
        public NodeCost getCost() {
            if (nextNode != null && nextNode.getCost() == NodeCost.MONOMORPHIC) {
                return NodeCost.POLYMORPHIC;
            }
            return super.getCost();
        }

        @Override
        public final Object executeCall(Object[] arguments) {
            if (accept(JSArguments.getThisObject(arguments), JSArguments.getFunctionObject(arguments))) {
                return currentNode.executeCall(arguments);
            }
            return nextNode.executeCall(arguments);
        }
    }

    private static final class UninitializedCacheNode extends AbstractCacheNode {

        private final byte depth;
        private final byte flags;
        private final boolean generic;

        UninitializedCacheNode(byte flags, boolean generic) {
            this((byte) 0, flags, generic);
        }

        UninitializedCacheNode(byte depth, byte flags, boolean generic) {
            this.depth = depth;
            this.flags = flags;
            this.generic = generic;
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.UNINITIALIZED;
        }

        private boolean isGeneric() {
            return generic;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(arguments).executeCall(arguments);
        }

        private AbstractCacheNode specialize(Object[] arguments) {
            CompilerAsserts.neverPartOfCompilation();
            Object function = JSArguments.getFunctionObject(arguments);

            assert JSTruffleOptions.FunctionCacheLimit > 0 || isGeneric();
            if (depth < JSTruffleOptions.FunctionCacheLimit && !isGeneric()) {
                if (JSFunction.isJSFunction(function)) {
                    return specializeDirectCall(function);
                } else if (JSTruffleOptions.NashornJavaInterop && (function instanceof JavaMethod || function instanceof JavaClass)) {
                    return specializeJavaCall(function);
                } else if (JSTruffleOptions.NashornJavaInterop && JavaPackage.isJavaPackage(function)) {
                    return specializeJavaPackage((DynamicObject) function);
                }
            }
            if (JSFunction.isJSFunction(function)) {
                return specializeGenericFunction();
            } else if (JSProxy.isProxy(function)) {
                return replace(new JSProxyCacheNode(createUninitialized(), JSFunctionCallNode.isNew(flags), JSFunctionCallNode.isNewTarget(flags)));
            } else if (JSGuards.isForeignObject(function)) {
                return specializeForeignCall(arguments);
            } else if (function instanceof JSNoSuchMethodAdapter) {
                return replace(new JSNoSuchMethodAdapterCacheNode(createUninitialized()));
            } else {
                return replace(new GenericFallbackCacheNode(flags, null));
            }
        }

        private UninitializedCacheNode createUninitialized() {
            assert depth + 1 <= Byte.MAX_VALUE;
            return new UninitializedCacheNode((byte) (depth + 1), flags, isGeneric());
        }

        private AbstractCacheNode specializeJavaPackage(DynamicObject pkg) {
            return replace(new JavaCacheNode(pkg, new JavaPackageCallNode(pkg), createUninitialized()));
        }

        private AbstractCacheNode specializeGenericFunction() {
            return atomic(new Callable<AbstractCacheNode>() {
                @Override
                public AbstractCacheNode call() {
                    if (JSTruffleOptions.TraceFunctionCache) {
                        System.out.printf("FUNCTION CACHE LIMIT HIT %s (depth=%d)\n", getEncapsulatingSourceSection(), depth);
                    }
                    AbstractCacheNode genericNode = new GenericJSFunctionCacheNode(flags, null);
                    AbstractCacheNode topMost = (AbstractCacheNode) NodeUtil.getNthParent(UninitializedCacheNode.this, depth);
                    return topMost.replace(genericNode);
                }
            });
        }

        private AbstractCacheNode specializeDirectCall(Object function) {
            final DynamicObject castFunctionObj = (DynamicObject) function;
            final JSFunctionData functionData = JSFunction.getFunctionData(castFunctionObj);

            final AbstractCacheNode cachedNode;
            if (JSTruffleOptions.FunctionCacheOnInstance) {
                return atomic(new Callable<AbstractCacheNode>() {
                    @Override
                    public AbstractCacheNode call() {
                        FunctionInstanceCacheNode obsoleteNode = findCachedNodeWithCallTarget(functionData);
                        if (obsoleteNode == null) {
                            final JSDirectCallNode current = createCallableNode(castFunctionObj, isNew(flags), isNewTarget(flags), true);
                            return replace(new FunctionInstanceCacheNode(castFunctionObj, current, createUninitialized()));
                        } else {
                            return specializeDirectCallObsolete(castFunctionObj, obsoleteNode, functionData);
                        }
                    }
                });
            } else {
                final JSDirectCallNode current = createCallableNode(castFunctionObj, isNew(flags), isNewTarget(flags), false);
                cachedNode = new FunctionDataCacheNode(functionData, current, createUninitialized());
                return replace(cachedNode);
            }
        }

        private AbstractCacheNode specializeDirectCallObsolete(DynamicObject function, FunctionInstanceCacheNode oldCacheNode, JSFunctionData functionData) {
            if (JSTruffleOptions.TraceFunctionCache) {
                System.out.printf("FUNCTION CACHE changed function instance to function data cache %s (depth=%d)\n", getEncapsulatingSourceSection(), depth);
            }
            JSDirectCallNode directCallNode = oldCacheNode.currentNode;
            if (directCallNode instanceof BoundCallNode) {
                directCallNode = createCallableNode(function, isNew(flags), isNewTarget(flags), false);
            }
            final AbstractCacheNode newCachedNode = new FunctionDataCacheNode(functionData, directCallNode, oldCacheNode.nextNode);
            oldCacheNode.replace(newCachedNode);
            return newCachedNode;
        }

        private FunctionInstanceCacheNode findCachedNodeWithCallTarget(JSFunctionData functionData) {
            AbstractCacheNode current = this;
            int d = depth;
            while (d-- > 0) {
                current = (AbstractCacheNode) current.getParent();
                if (current instanceof FunctionInstanceCacheNode) {
                    JSFunctionData currentFunctionData = JSFunction.getFunctionData(((FunctionInstanceCacheNode) current).functionObj);
                    if (currentFunctionData == functionData) {
                        return (FunctionInstanceCacheNode) current;
                    }
                }
            }
            return null;
        }

        private AbstractCacheNode specializeJavaCall(Object method) {
            assert method instanceof JavaMethod || method instanceof JavaClass;
            final JSDirectCallNode current;
            if (method instanceof JavaClass && ((JavaClass) method).getType().isArray()) {
                current = new JavaClassCallNode((JavaClass) method);
            } else if (method instanceof JavaClass && ((JavaClass) method).isAbstract()) {
                JavaClass extendedClass = ((JavaClass) method).extend(null, null);
                if (JSTruffleOptions.JavaCallCache) {
                    current = JavaMethodCallNode.create(extendedClass);
                } else {
                    current = new JavaClassCallNode(extendedClass);
                }
            } else {
                if (JSTruffleOptions.JavaCallCache) {
                    current = JavaMethodCallNode.create(method);
                } else {
                    if (method instanceof JavaMethod) {
                        current = new SlowJavaMethodCallNode((JavaMethod) method);
                    } else {
                        current = new JavaClassCallNode((JavaClass) method);
                    }
                }
            }
            final AbstractCacheNode cacheNode = new JavaCacheNode(method, current, createUninitialized());
            return replace(cacheNode);
        }

        private AbstractCacheNode specializeForeignCall(Object[] arguments) {
            int userArgumentCount = JSArguments.getUserArgumentCount(arguments);
            Object thisObject = JSArguments.getThisObject(arguments);
            AbstractJavaScriptLanguage language = AbstractJavaScriptLanguage.getCurrentLanguage();
            if (JSGuards.isForeignObject(thisObject)) {
                Node parent = getParent();
                while (parent instanceof AbstractCacheNode) {
                    parent = parent.getParent();
                }

                JSFunctionCallNode functionCallNode = (JSFunctionCallNode) parent;
                Object propertyKey = functionCallNode.getPropertyKey();
                if (propertyKey != null && propertyKey instanceof String) {
                    return replace(new CallForeignTargetCacheNode(true, new ForeignInvokeNode(language, (String) propertyKey, userArgumentCount), createUninitialized()));
                }
            }
            return replace(new CallForeignTargetCacheNode(false, new ForeignExecuteNode(language, userArgumentCount), createUninitialized()));
        }

    }

    private static final class BoundCallNode extends JSDirectCallNode {
        @Child private JSDirectCallNode boundNode;

        private final Object boundThis;
        private final DynamicObject targetFunctionObj;
        private final Object[] addArguments;
        private final boolean useDynamicThis;
        private final boolean isNewTarget;

        BoundCallNode(DynamicObject function, boolean isNew, boolean isNewTarget) {
            super();
            assert JSFunction.isBoundFunction(function);
            Object lastReceiver;
            DynamicObject lastFunction = function;
            List<Object> prefixArguments = new ArrayList<>();
            do {
                Object[] extraArguments = JSFunction.getBoundArguments(lastFunction);
                prefixArguments.addAll(0, Arrays.asList(extraArguments));

                lastReceiver = JSFunction.getBoundThis(lastFunction);
                lastFunction = JSFunction.getBoundTargetFunction(lastFunction);
            } while (JSFunction.isBoundFunction(lastFunction) && !isNewTarget);
            // Note: We cannot unpack nested bound functions if this is a construct-with-newTarget.
            // This is because we need to apply the SameValue(F, newTarget) check below recursively
            // for all bound functions F until we reach the unbound target function.
            // As a result, we nest a BoundCallNode for every bound function layer to be unpacked.

            this.addArguments = prefixArguments.toArray(JSArguments.EMPTY_ARGUMENTS_ARRAY);
            this.targetFunctionObj = lastFunction;
            if (isNew || isNewTarget) {
                this.useDynamicThis = true;
                this.boundThis = null;
            } else {
                this.useDynamicThis = false;
                this.boundThis = lastReceiver;
            }
            this.isNewTarget = isNewTarget;
            this.boundNode = createCallableNode(lastFunction, isNew, isNewTarget, true);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            assert checkTargetFunction(arguments);
            return boundNode.executeCall(bindExtraArguments(arguments));
        }

        private Object[] bindExtraArguments(Object[] origArgs) {
            Object target = useDynamicThis ? JSArguments.getThisObject(origArgs) : boundThis;
            int skip = isNewTarget ? 1 : 0;
            Object[] origUserArgs = JSArguments.extractUserArguments(origArgs, skip);
            int newUserArgCount = addArguments.length + origUserArgs.length;
            Object[] arguments = JSArguments.createInitial(target, targetFunctionObj, skip + newUserArgCount);
            JSArguments.setUserArguments(arguments, skip, addArguments);
            JSArguments.setUserArguments(arguments, skip + addArguments.length, origUserArgs);
            if (isNewTarget) {
                Object newTarget = JSArguments.getNewTarget(origArgs);
                if (newTarget == JSArguments.getFunctionObject(origArgs)) {
                    newTarget = targetFunctionObj;
                }
                arguments[JSArguments.RUNTIME_ARGUMENT_COUNT] = newTarget;
            }
            return arguments;
        }

        private boolean checkTargetFunction(Object[] arguments) {
            DynamicObject targetFunction = (DynamicObject) JSArguments.getFunctionObject(arguments);
            while (JSFunction.isBoundFunction(targetFunction)) {
                targetFunction = JSFunction.getBoundTargetFunction(targetFunction);
                if (isNewTarget) {
                    // see note above
                    return targetFunctionObj == targetFunction;
                }
            }
            return targetFunctionObj == targetFunction;
        }
    }

    private static final class DynamicBoundCallNode extends JSDirectCallNode {
        @Child private JSFunctionCallNode boundTargetCallNode;

        private final boolean useDynamicThis;
        private final boolean isNewTarget;

        DynamicBoundCallNode(boolean isNew, boolean isNewTarget) {
            super();
            if (isNew || isNewTarget) {
                this.useDynamicThis = true;
            } else {
                this.useDynamicThis = false;
            }
            this.isNewTarget = isNewTarget;
            this.boundTargetCallNode = JSFunctionCallNode.create(isNew, isNewTarget);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            return boundTargetCallNode.executeCall(bindExtraArguments(arguments));
        }

        private Object[] bindExtraArguments(Object[] origArgs) {
            DynamicObject function = (DynamicObject) JSArguments.getFunctionObject(origArgs);
            if (!JSFunction.isBoundFunction(function)) {
                throw Errors.shouldNotReachHere();
            }
            DynamicObject boundTargetFunction = JSFunction.getBoundTargetFunction(function);
            Object boundThis = useDynamicThis ? JSArguments.getThisObject(origArgs) : JSFunction.getBoundThis(function);
            Object[] boundArguments = JSFunction.getBoundArguments(function);
            int skip = isNewTarget ? 1 : 0;
            Object[] origUserArgs = JSArguments.extractUserArguments(origArgs, skip);
            int newUserArgCount = boundArguments.length + origUserArgs.length;
            Object[] arguments = JSArguments.createInitial(boundThis, boundTargetFunction, skip + newUserArgCount);
            JSArguments.setUserArguments(arguments, skip, boundArguments);
            JSArguments.setUserArguments(arguments, skip + boundArguments.length, origUserArgs);
            if (isNewTarget) {
                Object newTarget = JSArguments.getNewTarget(origArgs);
                if (newTarget == function) {
                    newTarget = boundTargetFunction;
                }
                arguments[JSArguments.RUNTIME_ARGUMENT_COUNT] = newTarget;
            }
            return arguments;
        }
    }

    private static final class DispatchedCallNode extends JSDirectCallNode {

        @Child private DirectCallNode callNode;

        DispatchedCallNode(CallTarget callTarget) {
            this.callNode = Truffle.getRuntime().createDirectCallNode(callTarget);

            if (callTarget instanceof RootCallTarget) {
                RootNode root = ((RootCallTarget) callTarget).getRootNode();
                if (root instanceof FunctionRootNode && ((FunctionRootNode) root).isInlineImmediately()) {
                    adoptChildren();
                    callNode.cloneCallTarget();
                    callNode.forceInlining();
                }
            }
        }

        @Override
        public Object executeCall(Object[] arguments) {
            return callNode.call(arguments);
        }
    }

    static final class InlinedBuiltinCallNode extends JSDirectCallNode {
        private final CallTarget callTarget;
        @Child private JSBuiltinNode.Inlined builtinNode;

        InlinedBuiltinCallNode(CallTarget callTarget, JSBuiltinNode.Inlined builtinNode) {
            this.callTarget = callTarget;
            this.builtinNode = builtinNode;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            try {
                return builtinNode.callInlined(arguments);
            } catch (JSBuiltinNode.RewriteToCallException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new DispatchedCallNode(callTarget), "rewrite inlined builtin to call").executeCall(arguments);
            }
        }
    }

    private static class ForeignExecuteNode extends JSDirectCallNode {
        @Child private ExportArgumentsNode exportArgumentsNode;
        @Child private JSForeignToJSTypeNode typeConvertNode;
        @Child protected Node callNode;
        private final ValueProfile classProfile = ValueProfile.createClassProfile();

        ForeignExecuteNode(AbstractJavaScriptLanguage language, int expectedArgumentCount) {
            this.exportArgumentsNode = ExportArgumentsNode.create(expectedArgumentCount, language);
            this.typeConvertNode = JSForeignToJSTypeNode.create();
        }

        @Override
        public final Object executeCall(Object[] arguments) {
            Object[] extractedUserArguments = exportArgumentsNode.export(JSArguments.extractUserArguments(arguments));
            TruffleObject function = (TruffleObject) classProfile.profile(JSArguments.getFunctionObject(arguments));
            return typeConvertNode.executeWithTarget(executeCallImpl(function, extractedUserArguments));
        }

        protected Object executeCallImpl(TruffleObject function, Object[] extractedUserArguments) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSInteropUtil.createCall());
            }
            return JSInteropNodeUtil.call(function, extractedUserArguments, callNode);
        }
    }

    private static final class ForeignInvokeNode extends ForeignExecuteNode {
        private final String functionName;

        ForeignInvokeNode(AbstractJavaScriptLanguage language, String functionName, int expectedArgumentCount) {
            super(language, expectedArgumentCount);
            this.functionName = functionName;
        }

        @Override
        protected Object executeCallImpl(TruffleObject receiver, Object[] extractedUserArguments) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSInteropUtil.createInvoke());
            }
            return JSInteropNodeUtil.invoke(receiver, functionName, extractedUserArguments, callNode);
        }
    }

    public abstract static class JavaMethodCallNode extends JSDirectCallNode {
        protected final Object method;

        JavaMethodCallNode(Object method) {
            super();
            this.method = method;
        }

        public static JavaMethodCallNode create(Object method) {
            return JSTruffleOptions.JavaConvertersAsMethodHandles ? new MHChainJavaMethodCallNode(method) : new UninitializedJavaMethodCallNode(method);
        }

        @TruffleBoundary(allowInlining = true)
        protected final Object invoke(MethodHandle methodHandle, Object target, Object[] arguments) {
            try {
                return methodHandle.invokeExact(target, arguments);
            } catch (ControlFlowException | GraalJSException e) {
                throw e;
            } catch (Throwable e) {
                CompilerDirectives.transferToInterpreter();
                throw UserScriptException.createJavaException(e, this);
            }
        }
    }

    private static final class UninitializedJavaMethodCallNode extends JavaMethodCallNode {
        UninitializedJavaMethodCallNode(Object method) {
            super(method);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return this.replace(makeMethodHandleCallNode(JSArguments.extractUserArguments(arguments))).executeCall(arguments);
        }

        private JavaMethodCallNode makeMethodHandleCallNode(Object[] arguments) {
            boolean isStatic;
            Pair<AbstractJavaMethod, Converter> bestMethod;
            if (method instanceof JavaMethod) {
                JavaMethod javaMethod = (JavaMethod) method;
                bestMethod = javaMethod.getBestMethod(arguments);
                isStatic = javaMethod.isStatic() || javaMethod.isConstructor();
            } else {
                assert method instanceof JavaClass;
                bestMethod = ((JavaClass) method).getBestConstructor(arguments);
                isStatic = true;
            }
            MethodHandle adaptedHandle = bestMethod.getFirst().getMethodHandle();
            Class<?>[] parameterTypes = bestMethod.getFirst().getParameterTypes();
            Converter converter = bestMethod.getSecond();

            // adapt this parameter and return type
            adaptedHandle = adaptSignature(adaptedHandle, isStatic);
            // spread arguments array to parameters
            adaptedHandle = adaptedHandle.asSpreader(Object[].class, parameterTypes.length);

            return new MHJavaMethodCallNode(method, adaptedHandle, converter);
        }

        private static MethodHandle adaptSignature(MethodHandle originalHandle, boolean isStatic) {
            MethodHandle adaptedHandle = originalHandle;
            if (isStatic) {
                adaptedHandle = MethodHandles.dropArguments(adaptedHandle, 0, Object.class);
            } else {
                adaptedHandle = adaptedHandle.asType(adaptedHandle.type().changeParameterType(0, Object.class));
            }
            adaptedHandle = adaptedHandle.asType(adaptedHandle.type().changeReturnType(Object.class));
            return adaptedHandle;
        }
    }

    private static final class MHJavaMethodCallNode extends JavaMethodCallNode {
        private final MethodHandle methodHandle;
        private final Converter converter;
        @Child private JavaMethodCallNode next;

        MHJavaMethodCallNode(Object method, MethodHandle methodHandle, Converter converter) {
            super(method);
            this.methodHandle = methodHandle;
            this.converter = converter;
            this.next = new UninitializedJavaMethodCallNode(method);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object[] userArgs = JSArguments.extractUserArguments(arguments);
            if (converter.guard(userArgs)) {
                return Converters.JAVA_TO_JS_CONVERTER.convert(invoke(methodHandle, JSArguments.getThisObject(arguments), (Object[]) converter.convert(userArgs)));
            } else {
                return next.executeCall(arguments);
            }
        }
    }

    private static class JavaClassCallNode extends JSDirectCallNode {
        private final JavaClass clazz;

        JavaClassCallNode(JavaClass clazz) {
            super();
            this.clazz = clazz;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            return clazz.newInstance(JSArguments.extractUserArguments(arguments));
        }
    }

    private static class SlowJavaMethodCallNode extends JSDirectCallNode {
        private final JavaMethod method;

        SlowJavaMethodCallNode(JavaMethod method) {
            super();
            this.method = method;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            return method.invoke(JSArguments.getThisObject(arguments), JSArguments.extractUserArguments(arguments));
        }
    }

    private static final class MHChainJavaMethodCallNode extends JavaMethodCallNode {
        private final MethodHandle methodHandle;
        private final MutableCallSite callSite;

        MHChainJavaMethodCallNode(Object method) {
            super(method);
            MethodType methodType = MethodType.methodType(Object.class, Object.class, Object[].class);
            try {
                this.callSite = new MutableCallSite(MethodHandles.lookup().findVirtual(getClass(), "initialize", methodType).bindTo(this));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            this.methodHandle = callSite.dynamicInvoker();
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object target = JSArguments.getThisObject(arguments);
            Object[] userArguments = JSArguments.extractUserArguments(arguments);
            return invoke(target, userArguments);
        }

        private Object invoke(Object target, Object[] userArguments) {
            return Converters.JAVA_TO_JS_CONVERTER.convert(invoke(methodHandle, target, userArguments));
        }

        @SuppressWarnings("unused")
        private Object initialize(Object target, Object[] arguments) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callSite.setTarget(makeMethodHandle(callSite.getTarget(), arguments));
            return invoke(null, arguments);
        }

        private MethodHandle makeMethodHandle(MethodHandle fallback, Object[] arguments) {
            Pair<AbstractJavaMethod, Converter> bestMethod;
            boolean isStatic;
            if (method instanceof JavaMethod) {
                JavaMethod javaMethod = (JavaMethod) method;
                bestMethod = javaMethod.getBestMethod(arguments);
                isStatic = javaMethod.isStatic() || javaMethod.isConstructor();
            } else {
                assert method instanceof JavaClass;
                bestMethod = ((JavaClass) method).getBestConstructor(arguments);
                isStatic = true;
            }
            MethodHandle adaptedHandle = bestMethod.getFirst().getMethodHandle();
            Class<?>[] parameterTypes = bestMethod.getFirst().getParameterTypes();
            Converter converter = bestMethod.getSecond();

            adaptedHandle = adaptSignature(adaptedHandle, isStatic);
            adaptedHandle = convertArguments(adaptedHandle, fallback, parameterTypes, converter);
            return adaptedHandle;
        }

        private static MethodHandle adaptSignature(MethodHandle originalHandle, boolean isStatic) {
            MethodHandle adaptedHandle = originalHandle;
            if (isStatic) {
                adaptedHandle = MethodHandles.dropArguments(adaptedHandle, 0, Object.class);
            } else {
                adaptedHandle = adaptedHandle.asType(adaptedHandle.type().changeParameterType(0, Object.class));
            }
            adaptedHandle = adaptedHandle.asType(adaptedHandle.type().changeReturnType(Object.class));
            return adaptedHandle;
        }

        private static MethodHandle convertArguments(MethodHandle originalHandle, MethodHandle fallback, Class<?>[] parameterTypes, Converter converter) {
            MethodHandle adaptedHandle = originalHandle.asSpreader(Object[].class, parameterTypes.length);
            if (parameterTypes.length > 0) {
                MethodHandle guard;
                MethodHandle convert;
                try {
                    guard = MethodHandles.dropArguments(
                                    MethodHandles.lookup().findVirtual(Converter.class, "guard", MethodType.methodType(boolean.class, Object.class)).bindTo(converter).asType(
                                                    MethodType.methodType(boolean.class, Object[].class)),
                                    0, Object.class);
                    convert = MethodHandles.lookup().findVirtual(Converter.class, "convert", MethodType.methodType(Object.class, Object.class)).bindTo(converter).asType(
                                    MethodType.methodType(Object[].class, Object[].class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }

                adaptedHandle = MethodHandles.filterArguments(adaptedHandle, 1, convert);
                adaptedHandle = MethodHandles.guardWithTest(guard, adaptedHandle, fallback);
            }
            return adaptedHandle;
        }
    }

    private static class JavaPackageCallNode extends JSDirectCallNode {
        private final DynamicObject pkg;

        JavaPackageCallNode(DynamicObject pkg) {
            super();
            this.pkg = pkg;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            throw createClassNotFoundException(pkg, this);
        }
    }

    /**
     * Generic case for {@link JSFunction}s.
     */
    private static class GenericJSFunctionCacheNode extends AbstractCacheNode {
        private final byte flags;

        @Child private IndirectCallNode indirectCallNode;
        @Child private AbstractCacheNode next;

        GenericJSFunctionCacheNode(byte flags, AbstractCacheNode next) {
            this.flags = flags;
            this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
            this.next = next;
            megamorphicCount.inc();
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            if (JSFunction.isJSFunction(function)) {
                Object target = JSArguments.getThisObject(arguments);
                return executeCallFunction(arguments, (DynamicObject) function, target);
            } else {
                if (next == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    next = insert(new UninitializedCacheNode(flags, true));
                }
                return next.executeCall(arguments);
            }
        }

        private Object executeCallFunction(Object[] arguments, DynamicObject functionObject, Object target) {
            assert functionObject == JSArguments.getFunctionObject(arguments) && target == JSArguments.getThisObject(arguments);
            if (isNewTarget(flags)) {
                return JSFunction.indirectConstructNewTarget(indirectCallNode, arguments);
            } else if (isNew(flags)) {
                return JSFunction.indirectConstruct(indirectCallNode, arguments);
            } else {
                return JSFunction.indirectCall(indirectCallNode, arguments);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.MEGAMORPHIC;
        }
    }

    private static class JSProxyCacheNode extends AbstractCacheNode {
        @Child private JSProxyCallNode proxyCall;
        @Child private AbstractCacheNode next;
        private final boolean isNew;
        private final boolean isNewTarget;

        JSProxyCacheNode(AbstractCacheNode next, boolean isNew, boolean isNewTarget) {
            this.next = next;
            this.isNew = isNew;
            this.isNewTarget = isNewTarget;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            if (JSProxy.isProxy(function)) {
                if (proxyCall == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    JSContext context = JSShape.getJSContext(((DynamicObject) function).getShape());
                    proxyCall = insert(JSProxyCallNode.create(context, isNew, isNewTarget));
                }
                return proxyCall.execute(arguments);
            } else {
                return next.executeCall(arguments);
            }
        }
    }

    private static class JSNoSuchMethodAdapterCacheNode extends AbstractCacheNode {
        @Child private JSFunctionCallNode noSuchMethodCallNode;
        @Child private AbstractCacheNode next;

        JSNoSuchMethodAdapterCacheNode(AbstractCacheNode next) {
            this.noSuchMethodCallNode = JSFunctionCallNode.createCall();
            this.next = next;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            if (function instanceof JSNoSuchMethodAdapter) {
                JSNoSuchMethodAdapter noSuchMethod = (JSNoSuchMethodAdapter) function;
                Object[] handlerArguments = JSArguments.createInitial(noSuchMethod.getThisObject(), noSuchMethod.getFunction(), JSArguments.getUserArgumentCount(arguments) + 1);
                JSArguments.setUserArgument(handlerArguments, 0, noSuchMethod.getKey());
                JSArguments.setUserArguments(handlerArguments, 1, JSArguments.extractUserArguments(arguments));
                return noSuchMethodCallNode.executeCall(handlerArguments);
            } else {
                return next.executeCall(arguments);
            }
        }
    }

    /**
     * Fallback (TypeError) and Java method/class/package.
     */
    private static class GenericFallbackCacheNode extends AbstractCacheNode {
        private final byte flags;

        private final BranchProfile hasSeenErrorBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaClassBranch = BranchProfile.create();
        private final BranchProfile hasSeenAbstractJavaClassBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaMethodBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaPackageBranch = BranchProfile.create();
        private final BranchProfile hasSeenInteropBranch = BranchProfile.create();

        @Child private FlattenNode flattenNode;
        @Child private AbstractCacheNode next;

        GenericFallbackCacheNode(byte flags, AbstractCacheNode next) {
            this.flags = flags;
            this.next = next;
            megamorphicCount.inc();
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            if (!JSFunction.isJSFunction(function) && !JSProxy.isProxy(function) &&
                            !(JSGuards.isForeignObject(function)) &&
                            !(function instanceof JSNoSuchMethodAdapter)) {
                hasSeenInteropBranch.enter();
                Object target = JSArguments.getThisObject(arguments);
                return executeCallNonFunction(arguments, function, target);
            } else {
                if (next == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    next = insert(new UninitializedCacheNode(flags, true));
                }
                return next.executeCall(arguments);
            }
        }

        private Object[] flatten(Object[] extractedUserArguments) {
            if (flattenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                flattenNode = insert(FlattenNode.create());
            }
            for (int i = 0; i < extractedUserArguments.length; i++) {
                extractedUserArguments[i] = flattenNode.execute(extractedUserArguments[i]);
            }
            return extractedUserArguments;
        }

        private Object executeCallNonFunction(Object[] arguments, Object function, Object target) {
            if (JSTruffleOptions.NashornJavaInterop) {
                if (function instanceof JavaClass) {
                    hasSeenJavaClassBranch.enter();
                    return executeJavaClassCall(arguments, function);
                } else if (function instanceof JavaMethod) {
                    hasSeenJavaMethodBranch.enter();
                    return executeJavaMethodCall(arguments, function, target);
                } else if (JavaPackage.isJavaPackage(function)) {
                    hasSeenJavaPackageBranch.enter();
                    DynamicObject pack = (DynamicObject) function;
                    throw createClassNotFoundException(pack, this);
                }
            }
            hasSeenErrorBranch.enter();
            throw typeError(function);
        }

        private Object executeJavaMethodCall(Object[] arguments, Object function, Object target) {
            JavaMethod javaMethod = (JavaMethod) function;
            Object receiver = javaMethod.isStatic() ? null : target;
            return javaMethod.invoke(receiver, flatten(JSArguments.extractUserArguments(arguments)));
        }

        private Object executeJavaClassCall(Object[] arguments, Object function) {
            JavaClass javaClass = (JavaClass) function;
            if (javaClass.isAbstract()) {
                hasSeenAbstractJavaClassBranch.enter();
                javaClass = javaClass.extend(null, null);
            }
            return javaClass.newInstance(flatten(JSArguments.extractUserArguments(arguments)));
        }

        @TruffleBoundary
        private JSException typeError(Object function) {
            Object expressionStr = null;
            JSFunctionCallNode callNode = null;
            for (Node current = this; current != null; current = current.getParent()) {
                if (current instanceof JSFunctionCallNode) {
                    callNode = (JSFunctionCallNode) current;
                    break;
                }
            }
            if (callNode != null) {
                if (callNode instanceof InvokeNode) {
                    expressionStr = ((InvokeNode) callNode).getFunctionTargetNode().expressionToString();
                } else if (callNode instanceof CallNode) {
                    expressionStr = ((CallNode) callNode).functionNode.expressionToString();
                }
            }
            return Errors.createTypeErrorNotAFunction(expressionStr != null ? expressionStr : function, this);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.MEGAMORPHIC;
        }
    }

    @TruffleBoundary
    protected static RuntimeException createClassNotFoundException(DynamicObject javaPackage, Node originatingNode) {
        return UserScriptException.createJavaException(new ClassNotFoundException(JavaPackage.getPackageName(javaPackage)), originatingNode);
    }
}
