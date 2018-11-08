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
import java.util.concurrent.locks.Lock;

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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.access.JSProxyCallNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
import com.oracle.truffle.js.nodes.instrumentation.JSInputGeneratingNodeWrapper;
import com.oracle.truffle.js.nodes.instrumentation.JSMaterializedInvokeTargetableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.interop.ExportArgumentsNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.FlattenNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
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

    public static JSFunctionCallNode createCall(JavaScriptNode function, JavaScriptNode target, JavaScriptNode[] arguments, boolean isNew, boolean isNewTarget) {
        assert arguments.length <= JSTruffleOptions.MaxFunctionArgumentsLength;
        boolean spread = hasSpreadArgument(arguments);
        if (spread) {
            return new CallSpreadNode(target, function, arguments, flags(isNew, isNewTarget));
        }
        if (arguments.length == 0) {
            return new Call0Node(target, function, flags(isNew, isNewTarget));
        } else if (arguments.length == 1) {
            return new Call1Node(target, function, arguments[0], flags(isNew, isNewTarget));
        }
        return new CallNNode(target, function, arguments, flags(isNew, isNewTarget));
    }

    public static JSFunctionCallNode createInvoke(JSTargetableNode targetFunction, JavaScriptNode[] arguments, boolean isNew, boolean isNewTarget) {
        assert arguments.length <= JSTruffleOptions.MaxFunctionArgumentsLength;
        boolean spread = hasSpreadArgument(arguments);
        if (spread) {
            return new InvokeSpreadNode(targetFunction, arguments, flags(isNew, isNewTarget));
        }
        if (arguments.length == 0) {
            return new Invoke0Node(targetFunction, flags(isNew, isNewTarget));
        } else if (arguments.length == 1) {
            return new Invoke1Node(targetFunction, arguments[0], flags(isNew, isNewTarget));
        }
        return new InvokeNNode(targetFunction, arguments, flags(isNew, isNewTarget));
    }

    static boolean isNewTarget(byte flags) {
        return (flags & NEW_TARGET) != 0;
    }

    static boolean isNew(byte flags) {
        return (flags & NEW) != 0;
    }

    private static boolean hasSpreadArgument(JavaScriptNode[] arguments) {
        for (JavaScriptNode arg : arguments) {
            if (arg instanceof SpreadArgumentNode) {
                return true;
            }
        }
        return false;
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
        return createCall(arguments[0], arguments[1], Arrays.copyOfRange(arguments, 2, arguments.length), false, false);
    }

    @ExplodeLoop(kind = LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    public final Object executeCall(Object[] arguments) {
        Object thisObject = JSArguments.getThisObject(arguments);
        Object function = JSArguments.getFunctionObject(arguments);
        for (AbstractCacheNode c = cacheNode; c != null; c = c.nextNode) {
            if (c.accept(thisObject, function)) {
                return c.executeCall(arguments);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return executeAndSpecialize(arguments);
    }

    private Object executeAndSpecialize(Object[] arguments) {
        CompilerAsserts.neverPartOfCompilation();
        Object thisObject = JSArguments.getThisObject(arguments);
        Object function = JSArguments.getFunctionObject(arguments);

        AbstractCacheNode c;
        Lock lock = getLock();
        lock.lock();
        try {
            AbstractCacheNode currentHead = cacheNode;
            int cachedCount = 0;
            boolean generic = false;
            c = currentHead;

            while (c != null) {
                if (c.accept(thisObject, function)) {
                    break;
                }
                if (isCached(c)) {
                    assert !generic;
                    cachedCount++;
                } else {
                    generic = generic || isGeneric(c);
                }
                c = c.nextNode;
            }
            if (c == null) {
                if (cachedCount < JSTruffleOptions.FunctionCacheLimit && !generic) {
                    if (JSFunction.isJSFunction(function)) {
                        c = specializeDirectCall((DynamicObject) function, currentHead);
                    } else if (JSTruffleOptions.NashornJavaInterop && (function instanceof JavaMethod || function instanceof JavaClass)) {
                        c = specializeJavaCall(function, currentHead);
                    }
                }
                if (c == null) {
                    boolean hasCached = cachedCount > 0;
                    if (JSFunction.isJSFunction(function)) {
                        c = specializeGenericFunction(currentHead, hasCached);
                    } else if (JSProxy.isProxy(function)) {
                        c = insertAtFront(new JSProxyCacheNode(null, JSFunctionCallNode.isNew(flags), JSFunctionCallNode.isNewTarget(flags)), currentHead);
                    } else if (JSGuards.isForeignObject(function)) {
                        c = specializeForeignCall(arguments, currentHead, this);
                    } else if (function instanceof JSNoSuchMethodAdapter) {
                        c = insertAtFront(new JSNoSuchMethodAdapterCacheNode(null), currentHead);
                    } else {
                        c = insertAtFront(new GenericFallbackCacheNode(null), dropCachedNodes(currentHead, hasCached));
                    }
                }
                assert c.getParent() != null;
            }
        } finally {
            lock.unlock();
        }
        assert c.accept(thisObject, function);
        return c.executeCall(arguments);
    }

    private static boolean isCached(AbstractCacheNode c) {
        return c instanceof JSFunctionCacheNode || c instanceof JavaCacheNode;
    }

    private static boolean isGeneric(AbstractCacheNode c) {
        return c instanceof GenericJSFunctionCacheNode || c instanceof GenericFallbackCacheNode;
    }

    private static boolean isUncached(AbstractCacheNode c) {
        return c instanceof JSProxyCacheNode || c instanceof ForeignCallNode || c instanceof JSNoSuchMethodAdapterCacheNode;
    }

    private static int getCachedCount(AbstractCacheNode head) {
        int count = 0;
        for (AbstractCacheNode c = head; c != null; c = c.nextNode) {
            if (isCached(c)) {
                count++;
            }
        }
        return count;
    }

    private AbstractCacheNode specializeDirectCall(DynamicObject functionObj, AbstractCacheNode head) {
        assert JSFunction.isJSFunction(functionObj);
        final JSFunctionData functionData = JSFunction.getFunctionData(functionObj);
        if (JSTruffleOptions.FunctionCacheOnInstance && !functionData.getContext().isMultiContext()) {
            return specializeDirectCallInstance(functionObj, functionData, head);
        } else {
            return specializeDirectCallShared(functionObj, functionData, head);
        }
    }

    private JSFunctionCacheNode specializeDirectCallInstance(DynamicObject functionObj, JSFunctionData functionData, AbstractCacheNode head) {
        JSFunctionCacheNode obsoleteNode = null;
        AbstractCacheNode previousNode = null;
        for (AbstractCacheNode p = null, c = head; c != null; p = c, c = c.nextNode) {
            if (c instanceof JSFunctionCacheNode) {
                JSFunctionCacheNode current = (JSFunctionCacheNode) c;
                if (current.isInstanceCache()) {
                    if (functionData == current.getFunctionData()) {
                        obsoleteNode = current;
                        previousNode = p;
                        break;
                    }
                }
            }
        }
        if (obsoleteNode == null) {
            JSFunctionCacheNode directCall = createCallableNode(functionObj, functionData, isNew(flags), isNewTarget(flags), true);
            return insertAtFront(directCall, head);
        } else {
            if (JSTruffleOptions.TraceFunctionCache) {
                System.out.printf("FUNCTION CACHE changed function instance to function data cache %s (depth=%d)\n", getEncapsulatingSourceSection(), getCachedCount(head));
            }
            JSFunctionCacheNode newNode;
            if (obsoleteNode instanceof FunctionInstanceCacheNode) {
                newNode = new FunctionDataCacheNode(functionData, ((FunctionInstanceCacheNode) obsoleteNode).callNode);
            } else {
                newNode = createCallableNode(functionObj, functionData, isNew(flags), isNewTarget(flags), false);
            }
            return replaceCached(newNode, head, obsoleteNode, previousNode);
        }
    }

    private JSFunctionCacheNode specializeDirectCallShared(DynamicObject functionObj, JSFunctionData functionData, AbstractCacheNode head) {
        final JSFunctionCacheNode directCall = createCallableNode(functionObj, functionData, isNew(flags), isNewTarget(flags), false);
        return insertAtFront(directCall, head);
    }

    private AbstractCacheNode specializeGenericFunction(AbstractCacheNode head, boolean hasCached) {
        AbstractCacheNode otherGeneric = dropCachedNodes(head, hasCached);
        AbstractCacheNode newNode = new GenericJSFunctionCacheNode(flags, otherGeneric);
        insert(newNode);
        this.cacheNode = newNode;
        return newNode;
    }

    private static AbstractCacheNode dropCachedNodes(AbstractCacheNode head, boolean hasCached) {
        if (!hasCached) {
            assert getCachedCount(head) == 0;
            return head;
        }
        AbstractCacheNode gen = null;
        for (AbstractCacheNode c = head; c != null; c = c.nextNode) {
            if (isCached(c)) {
                continue;
            }
            assert isGeneric(c) || isUncached(c);
            gen = c.withNext(gen);
        }
        return gen;
    }

    private AbstractCacheNode specializeJavaCall(Object method, AbstractCacheNode head) {
        assert method instanceof JavaMethod || method instanceof JavaClass;
        final JavaDirectCallNode directCall;
        if (method instanceof JavaClass && ((JavaClass) method).getType().isArray()) {
            directCall = new JavaClassCallNode((JavaClass) method);
        } else if (method instanceof JavaClass && ((JavaClass) method).isAbstract()) {
            JavaClass extendedClass = ((JavaClass) method).extend(null, null);
            if (JSTruffleOptions.JavaCallCache) {
                directCall = JavaMethodCallNode.create(extendedClass);
            } else {
                directCall = new JavaClassCallNode(extendedClass);
            }
        } else {
            if (JSTruffleOptions.JavaCallCache) {
                directCall = JavaMethodCallNode.create(method);
            } else {
                if (method instanceof JavaMethod) {
                    directCall = new SlowJavaMethodCallNode((JavaMethod) method);
                } else {
                    directCall = new JavaClassCallNode((JavaClass) method);
                }
            }
        }
        AbstractCacheNode newNode = new JavaCacheNode(method, directCall);
        return insertAtFront(newNode, head);
    }

    private AbstractCacheNode specializeForeignCall(Object[] arguments, AbstractCacheNode head, JSFunctionCallNode functionCallNode) {
        AbstractCacheNode newNode = null;
        int userArgumentCount = JSArguments.getUserArgumentCount(arguments);
        Object thisObject = JSArguments.getThisObject(arguments);
        AbstractJavaScriptLanguage language = AbstractJavaScriptLanguage.getCurrentLanguage();
        if (JSGuards.isForeignObject(thisObject)) {
            Object propertyKey = functionCallNode.getPropertyKey();
            if (propertyKey != null && propertyKey instanceof String) {
                newNode = new ForeignInvokeNode(language, (String) propertyKey, userArgumentCount);
            }
        }
        if (newNode == null) {
            newNode = new ForeignExecuteNode(language, userArgumentCount);
        }
        return insertAtFront(newNode, head);
    }

    private <T extends AbstractCacheNode> T insertAtFront(T newNode, AbstractCacheNode head) {
        insert(newNode);
        newNode.nextNode = head;
        this.cacheNode = newNode;
        return newNode;
    }

    @SuppressWarnings("unused")
    private <T extends AbstractCacheNode> T replaceCached(T newNode, AbstractCacheNode head, AbstractCacheNode obsoleteNode, AbstractCacheNode previousNode) {
        assert previousNode == null || previousNode.nextNode == obsoleteNode;
        insert(newNode);
        newNode.nextNode = obsoleteNode.nextNode;
        if (previousNode != null) {
            previousNode.nextNode = newNode;
        } else {
            this.cacheNode = newNode;
        }
        return newNode;
    }

    @Override
    public NodeCost getCost() {
        if (cacheNode == null) {
            return NodeCost.UNINITIALIZED;
        } else {
            return cacheNode.getCost();
        }
    }

    @Override
    public abstract JavaScriptNode getTarget();

    protected final Object evaluateReceiver(VirtualFrame frame, Object target) {
        Node targetNode = getTarget();
        if (targetNode instanceof WrapperNode) {
            targetNode = ((WrapperNode) targetNode).getDelegateNode();
        }
        if (targetNode instanceof SuperPropertyReferenceNode) {
            return ((SuperPropertyReferenceNode) targetNode).evaluateTarget(frame);
        }
        return target;
    }

    @ExplodeLoop
    protected static Object[] executeFillObjectArraySpread(JavaScriptNode[] arguments, VirtualFrame frame, Object[] args, int delta) {
        ArrayList<Object> argList = new ArrayList<>(arguments.length + delta);
        for (int i = 0; i < delta; i++) {
            Boundaries.listAdd(argList, args[i]);
        }
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof SpreadArgumentNode) {
                ((SpreadArgumentNode) arguments[i]).executeToList(frame, argList);
            } else {
                Boundaries.listAdd(argList, arguments[i].execute(frame));
            }
        }
        return Boundaries.listToArray(argList);
    }

    abstract static class CallNode extends JSFunctionCallNode {
        /**
         * May be {@code null}, the target value is {@code undefined}, then.
         */
        @Child protected JavaScriptNode targetNode;
        @Child protected JavaScriptNode functionNode;

        protected CallNode(JavaScriptNode targetNode, JavaScriptNode functionNode, byte flags) {
            super(flags);
            this.targetNode = targetNode;
            this.functionNode = functionNode;
        }

        @Override
        public final JavaScriptNode getTarget() {
            return targetNode;
        }

        public final JavaScriptNode getFunction() {
            return functionNode;
        }

        protected abstract Object[] createArguments(VirtualFrame frame, Object target, Object function);

        protected abstract JavaScriptNode[] getArgumentNodes();

        protected abstract void materializeInstrumentableArguments();

        @Override
        public Object execute(VirtualFrame frame) {
            Object target = executeTarget(frame);
            Object receiver = evaluateReceiver(frame, target);
            Object function = functionNode.execute(frame);
            // Note that the arguments must not be evaluated before the target.
            return executeCall(createArguments(frame, receiver, function));
        }

        final Object executeTarget(VirtualFrame frame) {
            if (targetNode != null) {
                return targetNode.execute(frame);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        public String expressionToString() {
            return Objects.toString(functionNode.expressionToString(), INTERMEDIATE_VALUE) + "(...)";
        }

        @Override
        protected Object getPropertyKey() {
            return null;
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (materializedTags.contains(FunctionCallExpressionTag.class)) {
                materializeInstrumentableArguments();
                if (this.hasSourceSection() && !functionNode.hasSourceSection()) {
                    transferSourceSectionAddExpressionTag(this, functionNode);
                }
                if (targetNode != null) {
                    // if we have a target, no de-sugaring needed
                    return this;
                } else {
                    JavaScriptNode materializedTargetNode = JSInputGeneratingNodeWrapper.create(JSConstantUndefinedNode.createUndefined());
                    JavaScriptNode call = createCall(functionNode, materializedTargetNode, getArgumentNodes(), isNew(flags), isNewTarget(flags));
                    transferSourceSectionAndTags(this, call);
                    return call;
                }
            } else {
                return this;
            }
        }
    }

    static class Call0Node extends CallNode {
        protected Call0Node(JavaScriptNode targetNode, JavaScriptNode functionNode, byte flags) {
            super(targetNode, functionNode, flags);
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            return JSArguments.createZeroArg(target, function);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new Call0Node(cloneUninitialized(targetNode), cloneUninitialized(functionNode), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return new JavaScriptNode[0];
        }

        @Override
        protected void materializeInstrumentableArguments() {
        }
    }

    static class Call1Node extends CallNode {
        @Child protected JavaScriptNode argument0;

        protected Call1Node(JavaScriptNode targetNode, JavaScriptNode functionNode, JavaScriptNode argument0, byte flags) {
            super(targetNode, functionNode, flags);
            this.argument0 = argument0;
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object arg0 = argument0.execute(frame);
            return JSArguments.createOneArg(target, function, arg0);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new Call1Node(cloneUninitialized(targetNode), cloneUninitialized(functionNode), cloneUninitialized(argument0), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return new JavaScriptNode[]{argument0};
        }

        @Override
        protected void materializeInstrumentableArguments() {
            if (!argument0.isInstrumentable()) {
                argument0 = insert(JSInputGeneratingNodeWrapper.create(argument0));
            }
        }
    }

    static class CallNNode extends CallNode {
        @Children protected final JavaScriptNode[] arguments;

        protected CallNNode(JavaScriptNode targetNode, JavaScriptNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, flags);
            this.arguments = arguments;
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object[] args = JSArguments.createInitial(target, function, arguments.length);
            return executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        }

        @ExplodeLoop
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            for (int i = 0; i < arguments.length; i++) {
                assert !(arguments[i] instanceof SpreadArgumentNode);
                args[i + delta] = arguments[i].execute(frame);
            }
            return args;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new CallNNode(cloneUninitialized(targetNode), cloneUninitialized(functionNode), cloneUninitialized(arguments), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return arguments;
        }

        @Override
        protected void materializeInstrumentableArguments() {
            for (int i = 0; i < arguments.length; i++) {
                if (!(arguments[i] instanceof SpreadArgumentNode) && !arguments[i].isInstrumentable()) {
                    arguments[i] = insert(JSInputGeneratingNodeWrapper.create(arguments[i]));
                }
            }
        }
    }

    static class CallSpreadNode extends CallNNode {
        protected CallSpreadNode(JavaScriptNode targetNode, JavaScriptNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, arguments, flags);
        }

        @Override
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            return executeFillObjectArraySpread(arguments, frame, args, delta);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new CallSpreadNode(cloneUninitialized(targetNode), cloneUninitialized(functionNode), cloneUninitialized(arguments), flags);
        }
    }

    /**
     * The target of {@link #functionTargetNode} also serves as the this argument of the call. If
     * {@code true}, target not only serves as the this argument of the call, but also the target
     * object for the member expression that retrieves the function.
     */
    public abstract static class InvokeNode extends JSFunctionCallNode {
        @Child protected JavaScriptNode targetNode;
        @Child protected JSTargetableNode functionTargetNode;

        protected InvokeNode(JSTargetableNode functionTargetNode, byte flags) {
            super(flags);
            this.functionTargetNode = functionTargetNode;
        }

        protected InvokeNode(JavaScriptNode targetNode, JSTargetableNode functionTargetNode, byte flags) {
            super(flags);
            this.targetNode = targetNode;
            this.functionTargetNode = functionTargetNode;
        }

        @Override
        public JavaScriptNode getTarget() {
            if (targetNode != null) {
                return targetNode;
            }
            return functionTargetNode.getTarget();
        }

        protected abstract Object[] createArguments(VirtualFrame frame, Object target, Object function);

        protected abstract JavaScriptNode[] getArgumentNodes();

        protected abstract void materializeInstrumentableArguments();

        @Override
        public Object execute(VirtualFrame frame) {
            Object target = executeTarget(frame);
            Object receiver = evaluateReceiver(frame, target);
            Object function = executeFunctionWithTarget(frame, target);
            // Note that the arguments must not be evaluated before the target.
            return executeCall(createArguments(frame, receiver, function));
        }

        protected Object executeTarget(VirtualFrame frame) {
            if (targetNode != null) {
                return targetNode.execute(frame);
            }
            return functionTargetNode.evaluateTarget(frame);
        }

        final Object executeFunctionWithTarget(VirtualFrame frame, Object target) {
            return functionTargetNode.executeWithTarget(frame, target);
        }

        @Override
        public String expressionToString() {
            return Objects.toString(functionTargetNode.expressionToString(), INTERMEDIATE_VALUE) + "(...)";
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (targetNode != null) {
                // if we have a target, no de-sugaring needed
                return this;
            }
            if (materializedTags.contains(FunctionCallExpressionTag.class) || materializedTags.contains(ReadPropertyExpressionTag.class) ||
                            materializedTags.contains(ReadElementExpressionTag.class)) {
                materializeInstrumentableArguments();
                InvokeNode invoke = (InvokeNode) createInvoke(null, getArgumentNodes(), isNew(flags), isNewTarget(flags));
                JavaScriptNode target = functionTargetNode.getTarget();
                invoke.targetNode = !target.isInstrumentable() ? JSInputGeneratingNodeWrapper.create(target) : target;
                invoke.functionTargetNode = JSMaterializedInvokeTargetableNode.createFor(functionTargetNode);
                transferSourceSectionAndTags(functionTargetNode, invoke.functionTargetNode);
                transferSourceSectionAndTags(this, invoke);
                return invoke;
            } else {
                return this;
            }
        }

        @Override
        protected Object getPropertyKey() {
            JavaScriptNode propertyNode = functionTargetNode;
            if (propertyNode instanceof WrapperNode) {
                propertyNode = (JavaScriptNode) ((WrapperNode) propertyNode).getDelegateNode();
            }
            if (propertyNode instanceof PropertyNode) {
                return ((PropertyNode) propertyNode).getPropertyKey();
            }
            return null;
        }

        public JSTargetableNode getFunctionTargetNode() {
            return functionTargetNode;
        }
    }

    static class Invoke0Node extends InvokeNode {
        protected Invoke0Node(JSTargetableNode functionNode, byte flags) {
            this(null, functionNode, flags);
        }

        protected Invoke0Node(JavaScriptNode targetNode, JSTargetableNode functionNode, byte flags) {
            super(targetNode, functionNode, flags);
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            return JSArguments.createZeroArg(target, function);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new Invoke0Node(cloneUninitialized(targetNode), cloneUninitialized(functionTargetNode), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return new JavaScriptNode[0];
        }

        @Override
        protected void materializeInstrumentableArguments() {
        }
    }

    static class Invoke1Node extends InvokeNode {
        @Child protected JavaScriptNode argument0;

        protected Invoke1Node(JSTargetableNode functionNode, JavaScriptNode argument0, byte flags) {
            this(null, functionNode, argument0, flags);
        }

        protected Invoke1Node(JavaScriptNode targetNode, JSTargetableNode functionNode, JavaScriptNode argument0, byte flags) {
            super(targetNode, functionNode, flags);
            this.argument0 = argument0;
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object arg0 = argument0.execute(frame);
            return JSArguments.createOneArg(target, function, arg0);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new Invoke1Node(cloneUninitialized(targetNode), cloneUninitialized(functionTargetNode), cloneUninitialized(argument0), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return new JavaScriptNode[]{argument0};
        }

        @Override
        protected void materializeInstrumentableArguments() {
            if (!argument0.isInstrumentable()) {
                argument0 = insert(JSInputGeneratingNodeWrapper.create(argument0));
            }
        }
    }

    static class InvokeNNode extends InvokeNode {
        @Children protected final JavaScriptNode[] arguments;

        protected InvokeNNode(JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            this(null, functionNode, arguments, flags);
        }

        protected InvokeNNode(JavaScriptNode targetNode, JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, flags);
            this.arguments = arguments;
        }

        @Override
        protected final Object[] createArguments(VirtualFrame frame, Object target, Object function) {
            Object[] args = JSArguments.createInitial(target, function, arguments.length);
            return executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        }

        @ExplodeLoop
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            for (int i = 0; i < arguments.length; i++) {
                assert !(arguments[i] instanceof SpreadArgumentNode);
                args[i + delta] = arguments[i].execute(frame);
            }
            return args;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InvokeNNode(cloneUninitialized(targetNode), cloneUninitialized(functionTargetNode), cloneUninitialized(arguments), flags);
        }

        @Override
        protected JavaScriptNode[] getArgumentNodes() {
            return arguments;
        }

        @Override
        protected void materializeInstrumentableArguments() {
            for (int i = 0; i < arguments.length; i++) {
                if (!(arguments[i] instanceof SpreadArgumentNode) && !arguments[i].isInstrumentable()) {
                    arguments[i] = insert(JSInputGeneratingNodeWrapper.create(arguments[i]));
                }
            }
        }
    }

    static class InvokeSpreadNode extends InvokeNNode {
        protected InvokeSpreadNode(JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            this(null, functionNode, arguments, flags);
        }

        protected InvokeSpreadNode(JavaScriptNode targetNode, JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, arguments, flags);
        }

        @Override
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            return executeFillObjectArraySpread(arguments, frame, args, delta);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InvokeSpreadNode(cloneUninitialized(targetNode), cloneUninitialized(functionTargetNode), cloneUninitialized(arguments), flags);
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

    protected static JSFunctionCacheNode createCallableNode(DynamicObject function, JSFunctionData functionData, boolean isNew, boolean isNewTarget, boolean cacheOnInstance) {
        CallTarget callTarget = getCallTarget(function, isNew, isNewTarget);
        assert callTarget != null;
        if (JSFunction.isBoundFunction(function)) {
            if (cacheOnInstance) {
                return new BoundFunctionInstanceCallNode(function, isNew, isNewTarget);
            } else {
                return new DynamicBoundFunctionCallNode(isNew, isNewTarget, functionData);
            }
        } else {
            JSFunctionCacheNode node = tryInlineBuiltinFunctionCall(function, functionData, callTarget, cacheOnInstance);
            if (node != null) {
                return node;
            }

            if (cacheOnInstance) {
                return new FunctionInstanceCacheNode(function, callTarget);
            } else {
                return new FunctionDataCacheNode(functionData, callTarget);
            }
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

    private static JSFunctionCacheNode tryInlineBuiltinFunctionCall(DynamicObject function, JSFunctionData functionData, CallTarget callTarget, boolean cacheOnInstance) {
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
                        if (cacheOnInstance) {
                            return new InlinedBuiltinFunctionInstanceCacheNode(function, callTarget, inlined);
                        } else {
                            return new InlinedBuiltinFunctionDataCacheNode(functionData, callTarget, inlined);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static final class FunctionInstanceCacheNode extends UnboundJSFunctionCacheNode {

        private final DynamicObject functionObj;

        FunctionInstanceCacheNode(DynamicObject functionObj, CallTarget callTarget) {
            super(callTarget);
            assert JSFunction.isJSFunction(functionObj);
            this.functionObj = functionObj;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return functionObj == function;
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return JSFunction.getFunctionData(functionObj);
        }

        @Override
        protected boolean isInstanceCache() {
            return true;
        }
    }

    private static final class FunctionDataCacheNode extends UnboundJSFunctionCacheNode {
        private final JSFunctionData functionData;

        FunctionDataCacheNode(JSFunctionData functionData, CallTarget callTarget) {
            super(callTarget);
            this.functionData = functionData;
        }

        FunctionDataCacheNode(JSFunctionData functionData, DirectCallNode directCallNode) {
            super(directCallNode);
            this.functionData = functionData;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return functionData;
        }
    }

    private abstract static class JavaDirectCallNode extends JavaScriptBaseNode {
        protected JavaDirectCallNode() {
        }

        public abstract Object executeCall(Object[] arguments);
    }

    private static final class JavaCacheNode extends AbstractCacheNode {
        private final Object method;
        @Child protected JavaDirectCallNode directCallNode;

        JavaCacheNode(Object method, JavaDirectCallNode directCallNode) {
            this.directCallNode = directCallNode;
            this.method = method;
            assert method instanceof JavaMethod || method instanceof JavaClass || JavaPackage.isJavaPackage(method);
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return function == method;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            return directCallNode.executeCall(arguments);
        }
    }

    private abstract static class AbstractCacheNode extends JavaScriptBaseNode {
        @Child protected AbstractCacheNode nextNode;

        protected abstract boolean accept(Object thisObject, Object function);

        public abstract Object executeCall(Object[] arguments);

        protected AbstractCacheNode withNext(AbstractCacheNode newNext) {
            AbstractCacheNode copy = (AbstractCacheNode) copy();
            copy.nextNode = newNext;
            return copy;
        }

        @Override
        public NodeCost getCost() {
            if (nextNode != null) {
                return NodeCost.POLYMORPHIC;
            }
            return NodeCost.MONOMORPHIC;
        }
    }

    private abstract static class JSFunctionCacheNode extends AbstractCacheNode {
        JSFunctionCacheNode() {
        }

        protected boolean isInstanceCache() {
            return false;
        }

        protected abstract JSFunctionData getFunctionData();
    }

    private static final class BoundFunctionInstanceCallNode extends JSFunctionCacheNode {
        @Child private AbstractCacheNode boundNode;

        private final DynamicObject boundFunctionObj;
        private final Object boundThis;
        private final DynamicObject targetFunctionObj;
        private final Object[] addArguments;
        private final boolean useDynamicThis;
        private final boolean isNewTarget;

        BoundFunctionInstanceCallNode(DynamicObject function, boolean isNew, boolean isNewTarget) {
            super();
            assert JSFunction.isBoundFunction(function);
            this.boundFunctionObj = function;
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
            this.boundNode = createCallableNode(lastFunction, JSFunction.getFunctionData(lastFunction), isNew, isNewTarget, true);
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

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return function == boundFunctionObj;
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return JSFunction.getFunctionData(boundFunctionObj);
        }

        @Override
        protected boolean isInstanceCache() {
            return true;
        }
    }

    private static final class DynamicBoundFunctionCallNode extends JSFunctionCacheNode {
        @Child private JSFunctionCallNode boundTargetCallNode;

        private final boolean useDynamicThis;
        private final boolean isNewTarget;
        private final JSFunctionData boundFunctionData;

        DynamicBoundFunctionCallNode(boolean isNew, boolean isNewTarget, JSFunctionData boundFunctionData) {
            super();
            this.useDynamicThis = (isNew || isNewTarget);
            this.isNewTarget = isNewTarget;
            this.boundFunctionData = boundFunctionData;
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

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSFunction.isJSFunction(function) && boundFunctionData == JSFunction.getFunctionData((DynamicObject) function);
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return boundFunctionData;
        }
    }

    private abstract static class UnboundJSFunctionCacheNode extends JSFunctionCacheNode {

        @Child DirectCallNode callNode;

        UnboundJSFunctionCacheNode(CallTarget callTarget) {
            this.callNode = Truffle.getRuntime().createDirectCallNode(callTarget);

            if (callTarget instanceof RootCallTarget) {
                RootNode root = ((RootCallTarget) callTarget).getRootNode();
                if (root instanceof FunctionRootNode && ((FunctionRootNode) root).isInlineImmediately()) {
                    insert(callNode);
                    callNode.cloneCallTarget();
                    callNode.forceInlining();
                }
            }
        }

        UnboundJSFunctionCacheNode(DirectCallNode callNode) {
            this.callNode = callNode;
        }

        @Override
        public final Object executeCall(Object[] arguments) {
            return callNode.call(arguments);
        }
    }

    private abstract static class InlinedBuiltinCallNode extends JSFunctionCacheNode {
        private final CallTarget callTarget;
        @Child private JSBuiltinNode.Inlined builtinNode;
        @Child private DirectCallNode callNode;

        InlinedBuiltinCallNode(CallTarget callTarget, JSBuiltinNode.Inlined builtinNode) {
            this.callTarget = callTarget;
            this.builtinNode = builtinNode;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            if (callNode != null) {
                return callNode.call(arguments);
            }
            try {
                return builtinNode.callInlined(arguments);
            } catch (JSBuiltinNode.RewriteToCallException e) {
                // rewrite inlined builtin to call
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
                callNode.cloneCallTarget();
                callNode.forceInlining();
                return callNode.call(arguments);
            }
        }
    }

    private static final class InlinedBuiltinFunctionInstanceCacheNode extends InlinedBuiltinCallNode {
        private final DynamicObject functionObj;

        InlinedBuiltinFunctionInstanceCacheNode(DynamicObject functionObj, CallTarget callTarget, JSBuiltinNode.Inlined builtinNode) {
            super(callTarget, builtinNode);
            assert JSFunction.isJSFunction(functionObj);
            this.functionObj = functionObj;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return functionObj == function;
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return JSFunction.getFunctionData(functionObj);
        }

        @Override
        protected boolean isInstanceCache() {
            return true;
        }
    }

    private static final class InlinedBuiltinFunctionDataCacheNode extends InlinedBuiltinCallNode {
        private final JSFunctionData functionData;

        InlinedBuiltinFunctionDataCacheNode(JSFunctionData functionData, CallTarget callTarget, JSBuiltinNode.Inlined builtinNode) {
            super(callTarget, builtinNode);
            this.functionData = functionData;
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return functionData;
        }
    }

    private abstract static class ForeignCallNode extends AbstractCacheNode {
        @Child private ExportArgumentsNode exportArgumentsNode;
        @Child private JSForeignToJSTypeNode typeConvertNode;
        private final ValueProfile functionClassProfile = ValueProfile.createClassProfile();

        ForeignCallNode(AbstractJavaScriptLanguage language, int expectedArgumentCount) {
            this.exportArgumentsNode = ExportArgumentsNode.create(expectedArgumentCount, language);
            this.typeConvertNode = JSForeignToJSTypeNode.create();
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSGuards.isForeignObject(functionClassProfile.profile(function));
        }

        protected final TruffleObject getForeignFunction(Object[] arguments) {
            return (TruffleObject) functionClassProfile.profile(JSArguments.getFunctionObject(arguments));
        }

        protected final Object[] exportArguments(Object[] arguments) {
            return exportArgumentsNode.export(JSArguments.extractUserArguments(arguments));
        }

        protected final Object convertForeignReturn(Object returnValue) {
            return typeConvertNode.executeWithTarget(returnValue);
        }
    }

    private static class ForeignExecuteNode extends ForeignCallNode {
        @Child protected Node callNode;

        ForeignExecuteNode(AbstractJavaScriptLanguage language, int expectedArgumentCount) {
            super(language, expectedArgumentCount);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            TruffleObject function = getForeignFunction(arguments);
            Object[] callArguments = exportArguments(arguments);
            return convertForeignReturn(JSInteropNodeUtil.call(function, callArguments, callNode()));
        }

        protected Node callNode() {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSInteropUtil.createCall());
            }
            return callNode;
        }
    }

    private static final class ForeignInvokeNode extends ForeignExecuteNode {
        private final String functionName;
        private final ValueProfile thisClassProfile = ValueProfile.createClassProfile();
        @Child protected Node invokeNode;

        ForeignInvokeNode(AbstractJavaScriptLanguage language, String functionName, int expectedArgumentCount) {
            super(language, expectedArgumentCount);
            this.functionName = functionName;
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object receiver = thisClassProfile.profile(JSArguments.getThisObject(arguments));
            Object[] callArguments = exportArguments(arguments);
            Object callReturn;
            /*
             * If the receiver is a foreign object, the property node does not send the READ message
             * but returns the receiver, in which case we send an INVOKE message here instead.
             */
            if (JSGuards.isForeignObject(receiver)) {
                assert getForeignFunction(arguments) == receiver;
                callReturn = JSInteropNodeUtil.invoke((TruffleObject) receiver, functionName, callArguments, invokeNode());
            } else {
                TruffleObject function = getForeignFunction(arguments);
                callReturn = JSInteropNodeUtil.call(function, callArguments, callNode());
            }
            return convertForeignReturn(callReturn);
        }

        private Node invokeNode() {
            if (invokeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                invokeNode = insert(JSInteropUtil.createInvoke());
            }
            return invokeNode;
        }
    }

    public abstract static class JavaMethodCallNode extends JavaDirectCallNode {
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

    private static class JavaClassCallNode extends JavaDirectCallNode {
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

    private static class SlowJavaMethodCallNode extends JavaDirectCallNode {
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
            Object target = JSArguments.getThisObject(arguments);
            return executeCallFunction(arguments, (DynamicObject) function, target);
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

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSFunction.isJSFunction(function);
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

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return JSProxy.isProxy(function);
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

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return function instanceof JSNoSuchMethodAdapter;
        }
    }

    /**
     * Fallback (TypeError) and Java method/class/package.
     */
    private static class GenericFallbackCacheNode extends AbstractCacheNode {
        private final BranchProfile hasSeenErrorBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaClassBranch = BranchProfile.create();
        private final BranchProfile hasSeenAbstractJavaClassBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaMethodBranch = BranchProfile.create();
        private final BranchProfile hasSeenJavaPackageBranch = BranchProfile.create();

        @Child private FlattenNode flattenNode;
        @Child private AbstractCacheNode next;

        GenericFallbackCacheNode(AbstractCacheNode next) {
            this.next = next;
            megamorphicCount.inc();
        }

        @Override
        protected boolean accept(Object thisObject, Object function) {
            return !JSFunction.isJSFunction(function) && !JSProxy.isProxy(function) &&
                            !(JSGuards.isForeignObject(function)) &&
                            !(function instanceof JSNoSuchMethodAdapter);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            Object target = JSArguments.getThisObject(arguments);
            return executeCallNonFunction(arguments, function, target);
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
                    expressionStr = ((InvokeNode) callNode).functionTargetNode.expressionToString();
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
