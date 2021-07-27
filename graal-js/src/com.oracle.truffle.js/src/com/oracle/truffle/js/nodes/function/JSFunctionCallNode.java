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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.access.JSProxyCallNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
import com.oracle.truffle.js.nodes.instrumentation.JSInputGeneratingNodeWrapper;
import com.oracle.truffle.js.nodes.instrumentation.JSMaterializedInvokeTargetableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.interop.ExportArgumentsNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSNoSuchMethodAdapter;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptFunctionCallNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DebugCounter;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public abstract class JSFunctionCallNode extends JavaScriptNode implements JavaScriptFunctionCallNode {
    private static final DebugCounter megamorphicCount = DebugCounter.create("Megamorphic call site count");

    static final byte CALL = 0;
    static final byte NEW = 1 << 0;
    static final byte NEW_TARGET = 1 << 1;

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
        return new ExecuteCallNode(createFlags(isNew, isNewTarget));
    }

    private static byte createFlags(boolean isNew, boolean isNewTarget) {
        return (isNewTarget ? NEW_TARGET : (isNew ? NEW : CALL));
    }

    public static JSFunctionCallNode createCall(JavaScriptNode function, JavaScriptNode target, JavaScriptNode[] arguments, boolean isNew, boolean isNewTarget) {
        byte flags = createFlags(isNew, isNewTarget);
        boolean spread = hasSpreadArgument(arguments);
        if (spread) {
            return new CallSpreadNode(target, function, arguments, flags);
        }
        if (arguments.length == 0) {
            return new Call0Node(target, function, flags);
        } else if (arguments.length == 1) {
            return new Call1Node(target, function, arguments[0], flags);
        }
        return new CallNNode(target, function, arguments, flags);
    }

    public static JSFunctionCallNode createInvoke(JSTargetableNode targetFunction, JavaScriptNode[] arguments, boolean isNew, boolean isNewTarget) {
        byte flags = createFlags(isNew, isNewTarget);
        boolean spread = hasSpreadArgument(arguments);
        if (spread) {
            return new InvokeSpreadNode(targetFunction, arguments, flags);
        }
        if (arguments.length == 0) {
            return new Invoke0Node(targetFunction, flags);
        } else if (arguments.length == 1) {
            return new Invoke1Node(targetFunction, arguments[0], flags);
        }
        return new InvokeNNode(targetFunction, arguments, flags);
    }

    public static JSFunctionCallNode getUncachedCall() {
        return Uncached.CALL;
    }

    public static JSFunctionCallNode getUncachedNew() {
        return Uncached.NEW;
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

    protected Object getPropertyKey() {
        return null;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == FunctionCallTag.class) {
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

    @ExplodeLoop
    public Object executeCall(Object[] arguments) {
        Object function = JSArguments.getFunctionObject(arguments);
        for (AbstractCacheNode c = cacheNode; c != null; c = c.nextNode) {
            if (c.accept(function)) {
                return c.executeCall(arguments);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return executeAndSpecialize(arguments);
    }

    private Object executeAndSpecialize(Object[] arguments) {
        CompilerAsserts.neverPartOfCompilation();
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
                if (c.accept(function)) {
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
                if (cachedCount < JavaScriptLanguage.getCurrentJSRealm().getContext().getFunctionCacheLimit() && !generic) {
                    if (JSFunction.isJSFunction(function)) {
                        c = specializeDirectCall((DynamicObject) function, currentHead);
                    }
                }
                if (c == null) {
                    boolean hasCached = cachedCount > 0;
                    if (JSFunction.isJSFunction(function)) {
                        c = specializeGenericFunction(currentHead, hasCached);
                    } else if (JSProxy.isJSProxy(function)) {
                        c = insertAtFront(new JSProxyCacheNode(null, JSFunctionCallNode.isNew(flags), JSFunctionCallNode.isNewTarget(flags)), currentHead);
                    } else if (JSGuards.isForeignObject(function)) {
                        c = specializeForeignCall(arguments, currentHead);
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
        if (c.accept(function)) {
            return c.executeCall(arguments);
        } else {
            throw CompilerDirectives.shouldNotReachHere("Inconsistent guard.");
        }
    }

    private static boolean isCached(AbstractCacheNode c) {
        return c instanceof JSFunctionCacheNode;
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
        if (JSConfig.FunctionCacheOnInstance && !functionData.getContext().isMultiContext()) {
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
            if (JSConfig.TraceFunctionCache) {
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
        reportPolymorphicSpecialize();
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

    private AbstractCacheNode specializeForeignCall(Object[] arguments, AbstractCacheNode head) {
        AbstractCacheNode newNode = null;
        int userArgumentCount = JSArguments.getUserArgumentCount(arguments);
        Object thisObject = JSArguments.getThisObject(arguments);
        if (isNew(flags) || isNewTarget(flags)) {
            int skippedArgs = isNewTarget(flags) ? 1 : 0;
            newNode = new ForeignInstantiateNode(skippedArgs, userArgumentCount - skippedArgs);
        } else if (JSGuards.isForeignObject(thisObject)) {
            Object propertyKey = getPropertyKey();
            if (propertyKey != null && propertyKey instanceof String) {
                newNode = new ForeignInvokeNode((String) propertyKey, userArgumentCount);
            }
        }
        if (newNode == null) {
            newNode = new ForeignExecuteNode(userArgumentCount);
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
        } else if (isGeneric(cacheNode)) {
            return NodeCost.MEGAMORPHIC;
        } else if (cacheNode.nextNode != null) {
            return NodeCost.POLYMORPHIC;
        } else {
            return NodeCost.MONOMORPHIC;
        }
    }

    @Override
    public JavaScriptNode getTarget() {
        return null;
    }

    protected final Object evaluateReceiver(VirtualFrame frame, Object target) {
        JavaScriptNode targetNode = getTarget();
        if (targetNode instanceof SuperPropertyReferenceNode) {
            return ((SuperPropertyReferenceNode) targetNode).evaluateTarget(frame);
        }
        return target;
    }

    @ExplodeLoop
    protected static Object[] executeFillObjectArraySpread(JavaScriptNode[] arguments, VirtualFrame frame, Object[] args, int fixedArgumentsLength, BranchProfile growProfile) {
        // assume size that avoids growing
        SimpleArrayList<Object> argList = SimpleArrayList.create((long) fixedArgumentsLength + arguments.length + JSConfig.SpreadArgumentPlaceholderCount);
        for (int i = 0; i < fixedArgumentsLength; i++) {
            argList.addUnchecked(args[i]);
        }
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof SpreadArgumentNode) {
                ((SpreadArgumentNode) arguments[i]).executeToList(frame, argList, growProfile);
            } else {
                argList.add(arguments[i].execute(frame), growProfile);
            }
        }
        return argList.toArray();
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
            if (materializedTags.contains(FunctionCallTag.class)) {
                materializeInstrumentableArguments();
                if (this.hasSourceSection() && !functionNode.hasSourceSection()) {
                    transferSourceSectionAddExpressionTag(this, functionNode);
                }
                if (targetNode != null) {
                    // if we have a target, no de-sugaring needed
                    return this;
                } else {
                    JavaScriptNode materializedTargetNode = JSInputGeneratingNodeWrapper.create(JSConstantUndefinedNode.createUndefined());
                    JavaScriptNode call = createCall(cloneUninitialized(functionNode, materializedTags), materializedTargetNode, cloneUninitialized(getArgumentNodes(), materializedTags), isNew(flags),
                                    isNewTarget(flags));
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new Call0Node(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionNode, materializedTags), flags);
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new Call1Node(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionNode, materializedTags), cloneUninitialized(argument0, materializedTags), flags);
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new CallNNode(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionNode, materializedTags), cloneUninitialized(arguments, materializedTags), flags);
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

        private final BranchProfile growProfile = BranchProfile.create();

        protected CallSpreadNode(JavaScriptNode targetNode, JavaScriptNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, arguments, flags);
        }

        @Override
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            return executeFillObjectArraySpread(arguments, frame, args, delta, growProfile);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new CallSpreadNode(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionNode, materializedTags), cloneUninitialized(arguments, materializedTags), flags);
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
            if (materializedTags.contains(FunctionCallTag.class) || materializedTags.contains(ReadPropertyTag.class) ||
                            materializedTags.contains(ReadElementTag.class)) {
                materializeInstrumentableArguments();
                InvokeNode invoke = (InvokeNode) createInvoke(null, cloneUninitialized(getArgumentNodes(), materializedTags), isNew(flags), isNewTarget(flags));
                JSTargetableNode functionTargetNodeDelegate = cloneUninitialized(getFunctionTargetDelegate(), materializedTags);
                JavaScriptNode target = functionTargetNodeDelegate.getTarget();
                invoke.targetNode = !target.isInstrumentable() ? JSInputGeneratingNodeWrapper.create(target) : target;
                invoke.functionTargetNode = JSMaterializedInvokeTargetableNode.createFor(functionTargetNodeDelegate);
                transferSourceSectionAndTags(functionTargetNodeDelegate, invoke.functionTargetNode);
                transferSourceSectionAndTags(this, invoke);
                return invoke;
            } else {
                return this;
            }
        }

        private JSTargetableNode getFunctionTargetDelegate() {
            if (functionTargetNode instanceof WrapperNode) {
                return (JSTargetableNode) ((WrapperNode) functionTargetNode).getDelegateNode();
            } else {
                return functionTargetNode;
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new Invoke0Node(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionTargetNode, materializedTags), flags);
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new Invoke1Node(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionTargetNode, materializedTags), cloneUninitialized(argument0, materializedTags), flags);
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new InvokeNNode(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionTargetNode, materializedTags), cloneUninitialized(arguments, materializedTags), flags);
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
        private final BranchProfile growProfile = BranchProfile.create();

        protected InvokeSpreadNode(JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            this(null, functionNode, arguments, flags);
        }

        protected InvokeSpreadNode(JavaScriptNode targetNode, JSTargetableNode functionNode, JavaScriptNode[] arguments, byte flags) {
            super(targetNode, functionNode, arguments, flags);
        }

        @Override
        protected Object[] executeFillObjectArray(VirtualFrame frame, Object[] args, int delta) {
            return executeFillObjectArraySpread(arguments, frame, args, delta, growProfile);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new InvokeSpreadNode(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(functionTargetNode, materializedTags), cloneUninitialized(arguments, materializedTags),
                            flags);
        }
    }

    static class ExecuteCallNode extends JSFunctionCallNode {
        protected ExecuteCallNode(byte flags) {
            super(flags);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere();
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ExecuteCallNode(flags);
        }
    }

    protected static JSFunctionCacheNode createCallableNode(DynamicObject function, JSFunctionData functionData, boolean isNew, boolean isNewTarget, boolean cacheOnInstance) {
        CallTarget callTarget = getCallTarget(functionData, isNew, isNewTarget);
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

    protected static CallTarget getCallTarget(JSFunctionData functionData, boolean isNew, boolean isNewTarget) {
        if (isNewTarget) {
            return functionData.getConstructNewTarget();
        } else if (isNew) {
            return functionData.getConstructTarget();
        } else {
            return functionData.getCallTarget();
        }
    }

    private static JSFunctionCacheNode tryInlineBuiltinFunctionCall(DynamicObject function, JSFunctionData functionData, CallTarget callTarget, boolean cacheOnInstance) {
        if (!JSConfig.InlineTrivialBuiltins) {
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
                    } else if (builtinNode.isCallerSensitive()) {
                        if (cacheOnInstance) {
                            return new CallerSensitiveBuiltinFunctionInstanceCacheNode(function, functionData, callTarget);
                        } else {
                            return new CallerSensitiveBuiltinFunctionDataCacheNode(functionData, callTarget);
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
        protected boolean accept(Object function) {
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
        protected boolean accept(Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return functionData;
        }
    }

    private abstract static class AbstractCacheNode extends JavaScriptBaseNode {
        @Child protected AbstractCacheNode nextNode;

        protected abstract boolean accept(Object function);

        public abstract Object executeCall(Object[] arguments);

        protected AbstractCacheNode withNext(AbstractCacheNode newNext) {
            AbstractCacheNode copy = (AbstractCacheNode) copy();
            copy.nextNode = newNext;
            return copy;
        }

        @Override
        public final NodeCost getCost() {
            return NodeCost.NONE;
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
        protected boolean accept(Object function) {
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
        protected boolean accept(Object function) {
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
                    if (((FunctionRootNode) root).isSplitImmediately()) {
                        callNode.cloneCallTarget();
                    }
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
        protected boolean accept(Object function) {
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
        protected boolean accept(Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }

        @Override
        protected JSFunctionData getFunctionData() {
            return functionData;
        }
    }

    private abstract static class CallerSensitiveBuiltinCallNode extends JSFunctionCacheNode {
        @Child private DirectCallNode callNode;
        protected final JSFunctionData functionData;

        CallerSensitiveBuiltinCallNode(JSFunctionData functionData, CallTarget callTarget) {
            this.functionData = functionData;
            this.callNode = Truffle.getRuntime().createDirectCallNode(callTarget);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            JSRealm realm = getRealm();
            JavaScriptBaseNode prev = realm.getCallNode();
            try {
                realm.setCallNode(this);
                return callNode.call(arguments);
            } finally {
                realm.setCallNode(prev);
            }
        }

        @Override
        protected final JSFunctionData getFunctionData() {
            return functionData;
        }
    }

    private static final class CallerSensitiveBuiltinFunctionInstanceCacheNode extends CallerSensitiveBuiltinCallNode {
        private final DynamicObject functionObj;

        CallerSensitiveBuiltinFunctionInstanceCacheNode(DynamicObject functionObj, JSFunctionData functionData, CallTarget callTarget) {
            super(functionData, callTarget);
            assert JSFunction.isJSFunction(functionObj);
            this.functionObj = functionObj;
        }

        @Override
        protected boolean accept(Object function) {
            return functionObj == function;
        }

        @Override
        protected boolean isInstanceCache() {
            return true;
        }
    }

    private static final class CallerSensitiveBuiltinFunctionDataCacheNode extends CallerSensitiveBuiltinCallNode {

        CallerSensitiveBuiltinFunctionDataCacheNode(JSFunctionData functionData, CallTarget callTarget) {
            super(functionData, callTarget);
        }

        @Override
        protected boolean accept(Object function) {
            return JSFunction.isJSFunction(function) && functionData == JSFunction.getFunctionData((DynamicObject) function);
        }
    }

    private abstract static class ForeignCallNode extends AbstractCacheNode {
        @Child private ExportArgumentsNode exportArgumentsNode;
        @Child private ImportValueNode typeConvertNode;
        private final ValueProfile functionClassProfile = ValueProfile.createClassProfile();

        ForeignCallNode(int expectedArgumentCount) {
            this.exportArgumentsNode = ExportArgumentsNode.create(expectedArgumentCount);
            this.typeConvertNode = ImportValueNode.create();
        }

        @Override
        protected boolean accept(Object function) {
            return JSGuards.isForeignObject(functionClassProfile.profile(function));
        }

        protected final Object getForeignFunction(Object[] arguments) {
            return functionClassProfile.profile(JSArguments.getFunctionObject(arguments));
        }

        protected final Object[] exportArguments(Object[] arguments) {
            return exportArgumentsNode.export(JSArguments.extractUserArguments(arguments));
        }

        protected final Object[] exportArguments(Object[] arguments, int skip) {
            return exportArgumentsNode.export(JSArguments.extractUserArguments(arguments, skip));
        }

        protected final Object convertForeignReturn(Object returnValue) {
            return typeConvertNode.executeWithTarget(returnValue);
        }
    }

    private static class ForeignExecuteNode extends ForeignCallNode {
        @Child protected InteropLibrary interop;

        ForeignExecuteNode(int expectedArgumentCount) {
            super(expectedArgumentCount);
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = getForeignFunction(arguments);
            Object[] callArguments = exportArguments(arguments);
            try {
                return convertForeignReturn(interop.execute(function, callArguments));
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(function, e, "execute", this);
            }
        }
    }

    private static final class ForeignInvokeNode extends ForeignExecuteNode {
        private final String functionName;
        private final ValueProfile thisClassProfile = ValueProfile.createClassProfile();
        @Child protected Node invokeNode;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child protected JSFunctionCallNode callJSFunctionNode;
        @Child protected PropertyGetNode getFunctionNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        @CompilationFinal private boolean optimistic = true;

        ForeignInvokeNode(String functionName, int expectedArgumentCount) {
            super(expectedArgumentCount);
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
                assert JSArguments.getFunctionObject(arguments) == receiver;
                if (interop.isNull(receiver)) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorCannotGetProperty(getContext(), functionName, receiver, false, this);
                }
                if (optimistic) {
                    try {
                        callReturn = interop.invokeMember(receiver, functionName, callArguments);
                    } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        optimistic = false;
                        callReturn = fallback(receiver, arguments, callArguments, e);
                    } catch (UnsupportedTypeException | ArityException e) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorInteropException(receiver, e, "invokeMember", functionName, this);
                    }
                } else {
                    if (interop.isMemberInvocable(receiver, functionName)) {
                        try {
                            callReturn = interop.invokeMember(receiver, functionName, callArguments);
                        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                            errorBranch.enter();
                            throw Errors.createTypeErrorInteropException(receiver, e, "invokeMember", functionName, this);
                        }
                    } else {
                        callReturn = fallback(receiver, arguments, callArguments, null);
                    }
                }
            } else {
                Object function = getForeignFunction(arguments);
                try {
                    callReturn = interop.execute(function, callArguments);
                } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorInteropException(function, e, "execute", this);
                }
            }
            return convertForeignReturn(callReturn);
        }

        private Object fallback(Object receiver, Object[] arguments, Object[] callArguments, InteropException caughtException) {
            InteropException ex = caughtException;
            if (getContext().getContextOptions().hasForeignObjectPrototype() || JSInteropUtil.isBoxedPrimitive(receiver, interop)) {
                Object function = maybeGetFromPrototype(receiver);
                if (function != Undefined.instance) {
                    return callJSFunction(receiver, function, arguments);
                }
            }
            if (getContext().getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(receiver) && interop.isHashEntryReadable(receiver, functionName)) {
                try {
                    Object function = interop.readHashValue(receiver, functionName);
                    return InteropLibrary.getUncached().execute(function, callArguments);
                } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException | ArityException e) {
                    ex = e;
                    // fall through
                }
            }
            errorBranch.enter();
            throw Errors.createTypeErrorInteropException(receiver, ex != null ? ex : UnknownIdentifierException.create(functionName), "invokeMember", functionName, this);
        }

        private Object maybeGetFromPrototype(Object receiver) {
            if (foreignObjectPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
            }
            DynamicObject prototype = foreignObjectPrototypeNode.executeDynamicObject(receiver);
            return getFunction(prototype);
        }

        private Object getFunction(Object object) {
            if (getFunctionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFunctionNode = insert(PropertyGetNode.create(functionName, getContext()));
            }
            return getFunctionNode.getValue(object);
        }

        private Object callJSFunction(Object receiver, Object function, Object[] arguments) {
            if (callJSFunctionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callJSFunctionNode = insert(JSFunctionCallNode.createCall());
            }
            return callJSFunctionNode.executeCall(JSArguments.create(receiver, function, JSArguments.extractUserArguments(arguments)));
        }

        private JSContext getContext() {
            return getLanguage().getJSContext();
        }
    }

    private static class ForeignInstantiateNode extends ForeignCallNode {
        @Child protected InteropLibrary interop;
        private final int skip;

        ForeignInstantiateNode(int skip, int expectedArgumentCount) {
            super(expectedArgumentCount);
            this.skip = skip;
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = getForeignFunction(arguments);
            Object[] callArguments = exportArguments(arguments, skip);
            try {
                return convertForeignReturn(interop.instantiate(function, callArguments));
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(function, e, "instantiate", this);
            }
        }
    }

    /**
     * Generic case for {@link JSFunction}s.
     */
    private static class GenericJSFunctionCacheNode extends AbstractCacheNode {
        private final byte flags;

        @Child private IndirectCallNode indirectCallNode;
        @Child private AbstractCacheNode next;
        private final BranchProfile initBranch;

        GenericJSFunctionCacheNode(byte flags, AbstractCacheNode next) {
            this.flags = flags;
            this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
            this.next = next;
            this.initBranch = BranchProfile.create();
            megamorphicCount.inc();
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            DynamicObject functionObject = (DynamicObject) function;
            JSFunctionData functionData = JSFunction.getFunctionData(functionObject);
            if (isNewTarget(flags)) {
                return indirectCallNode.call(functionData.getConstructNewTarget(initBranch), arguments);
            } else if (isNew(flags)) {
                return indirectCallNode.call(functionData.getConstructTarget(initBranch), arguments);
            } else {
                return indirectCallNode.call(functionData.getCallTarget(initBranch), arguments);
            }
        }

        @Override
        protected boolean accept(Object function) {
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
            if (JSProxy.isJSProxy(function)) {
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
        protected boolean accept(Object function) {
            return JSProxy.isJSProxy(function);
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
        protected boolean accept(Object function) {
            return function instanceof JSNoSuchMethodAdapter;
        }
    }

    /**
     * Fallback (TypeError) and Java method/class/package.
     */
    private static class GenericFallbackCacheNode extends AbstractCacheNode {
        @Child private AbstractCacheNode next;

        GenericFallbackCacheNode(AbstractCacheNode next) {
            this.next = next;
            megamorphicCount.inc();
        }

        @Override
        protected boolean accept(Object function) {
            return !JSFunction.isJSFunction(function) && !JSProxy.isJSProxy(function) &&
                            !(JSGuards.isForeignObject(function)) &&
                            !(function instanceof JSNoSuchMethodAdapter);
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object function = JSArguments.getFunctionObject(arguments);
            throw typeError(function);
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
    }

    private static class Uncached extends JSFunctionCallNode {
        static final Uncached CALL = new Uncached(createFlags(false, false));
        static final Uncached NEW = new Uncached(createFlags(true, false));

        protected Uncached(byte flags) {
            super(flags);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere();
        }

        @Override
        public Object executeCall(Object[] arguments) {
            Object functionObject = JSArguments.getFunctionObject(arguments);
            Object[] functionArgs = JSArguments.extractUserArguments(arguments);
            if (isNew()) {
                return JSRuntime.construct(functionObject, functionArgs);
            } else {
                return JSRuntime.call(functionObject, JSArguments.getThisObject(arguments), functionArgs);
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return this;
        }
    }
}
