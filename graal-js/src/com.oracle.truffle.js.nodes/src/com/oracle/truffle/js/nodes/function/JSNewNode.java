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

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.function.JSNewNodeGen.CachedPrototypeShapeNodeGen;
import com.oracle.truffle.js.nodes.function.JSNewNodeGen.SpecializedNewObjectNodeGen;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * 11.2.2 The new Operator.
 */
@ImportStatic(value = {JSProxy.class})
public abstract class JSNewNode extends JavaScriptNode {

    @Child @Executed protected JavaScriptNode targetNode;

    @Child private JSFunctionCallNode callNew;
    @Child private JSFunctionCallNode callNewTarget;
    @Child private AbstractFunctionArgumentsNode arguments;

    protected final JSContext context;

    protected JSNewNode(AbstractFunctionArgumentsNode arguments, JSFunctionCallNode callNew, JavaScriptNode targetNode, JSContext context) {
        this.callNew = callNew;
        this.arguments = arguments;
        this.targetNode = targetNode;
        this.context = context;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ObjectAllocationExpressionTag.class) {
            return true;
        } else if (tag == FunctionCallExpressionTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("isNew", true);
        descriptor.addProperty("isInvoke", false);
        return descriptor;
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ObjectAllocationExpressionTag.class) && materializationNeeded()) {
            JavaScriptNode newNew = create(context, cloneUninitialized(getTarget()), AbstractFunctionArgumentsNode.materializeArgumentsNode(arguments, getSourceSection()));
            transferSourceSection(this, newNew);
            return newNew;
        }
        return super.materializeInstrumentableNodes(materializedTags);
    }

    private boolean materializationNeeded() {
        // If arguments are not constant, no materialization is needed.
        return arguments instanceof JSFunctionOneConstantArgumentNode;
    }

    public static JSNewNode create(JSContext context, JavaScriptNode function, AbstractFunctionArgumentsNode arguments) {
        JSFunctionCallNode callNew = JSFunctionCallNode.createNew();
        return JSNewNodeGen.create(arguments, callNew, function, context);
    }

    public JavaScriptNode getTarget() {
        return targetNode;
    }

    @Specialization(guards = "isJSFunction(target)")
    public Object doNewReturnThis(VirtualFrame frame, DynamicObject target) {
        int userArgumentCount = arguments.getCount(frame);
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, target, userArgumentCount);
        args = arguments.executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        return callNew.executeCall(args);
    }

    @Specialization(guards = "isJSAdapter(target)")
    public Object doJSAdapter(VirtualFrame frame, DynamicObject target) {
        Object newFunction = JSObject.get(JSAdapter.getAdaptee(target), JSAdapter.NEW);
        if (JSFunction.isJSFunction(newFunction)) {
            Object[] args = getAbstractFunctionArguments(frame);
            return JSFunction.call((DynamicObject) newFunction, target, args);
        } else {
            return Undefined.instance;
        }
    }

    /**
     * Implements [[Construct]] for Proxy.
     */
    @Specialization(guards = "isProxy(proxy)")
    protected Object callJSProxy(VirtualFrame frame, DynamicObject proxy) {
        if (!JSRuntime.isCallableProxy(proxy)) {
            throw Errors.createTypeErrorNotAFunction(proxy, this);
        }
        DynamicObject handler = JSProxy.getHandlerChecked(proxy);
        TruffleObject target = JSProxy.getTarget(proxy);
        DynamicObject trap = JSProxy.getTrapFromObject(handler, JSProxy.CONSTRUCT);
        if (trap == Undefined.instance) {
            if (JSObject.isJSObject(target)) {
                // Construct(F=target, argumentsList=frame, newTarget=proxy)
                int userArgumentCount = arguments.getCount(frame);
                Object[] args = JSArguments.createInitialWithNewTarget(JSFunction.CONSTRUCT, target, proxy, userArgumentCount);
                args = arguments.executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT + 1);
                return getCallNewTarget().executeCall(args);
            } else {
                return JSInteropNodeUtil.construct(target, getAbstractFunctionArguments(frame));
            }
        }
        Object[] args = getAbstractFunctionArguments(frame);
        Object[] trapArgs = new Object[]{target, JSArray.createConstantObjectArray(context, args), proxy};
        Object result = JSFunction.call(trap, handler, trapArgs);
        if (!JSRuntime.isObject(result)) {
            throw Errors.createTypeErrorNotAnObject(result, this);
        }
        return result;
    }

    @TruffleBoundary
    @Specialization(guards = "isJavaPackage(target)")
    public Object createClassNotFoundError(DynamicObject target) {
        throw UserScriptException.createJavaException(new ClassNotFoundException(JavaPackage.getPackageName(target)), this);
    }

    @Specialization
    public Object doNewJavaObject(VirtualFrame frame, JavaClass target) {
        if (!target.isPublic()) {
            throwCannotExtendError(target);
        }
        Object[] args = JSArguments.createInitial(target, target, arguments.getCount(frame));
        args = arguments.executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        return callNew.executeCall(args);
    }

    @TruffleBoundary
    private static void throwCannotExtendError(JavaClass target) {
        throw Errors.createTypeError("new cannot be used with non-public java type " + target.getType().getName() + ".");
    }

    @Specialization(guards = "isJavaConstructor(target)")
    public Object doNewJavaObjectSpecialConstructor(VirtualFrame frame, JavaMethod target) {
        Object[] args = JSArguments.createInitial(target, target, arguments.getCount(frame));
        args = arguments.executeFillObjectArray(frame, args, JSArguments.RUNTIME_ARGUMENT_COUNT);
        return callNew.executeCall(args);
    }

    @Specialization(guards = {"isForeignObject(target)"})
    public Object doNewForeignObject(VirtualFrame frame, TruffleObject target,
                    @Cached("createNewCache()") Node newNode,
                    @Cached("create(context)") ExportValueNode convert,
                    @Cached("create()") JSForeignToJSTypeNode toJSType) {
        int count = arguments.getCount(frame);
        Object[] args = new Object[count];
        args = arguments.executeFillObjectArray(frame, args, 0);
        // We need to convert (e.g., bind functions) before invoking the constructor
        for (int i = 0; i < args.length; i++) {
            args[i] = convert.executeWithTarget(args[i], Undefined.instance);
        }
        return toJSType.executeWithTarget(JSInteropNodeUtil.construct(target, args, newNode, this));
    }

    protected Node createNewCache() {
        return JSInteropUtil.createNew();
    }

    @Specialization(guards = {"!isJSFunction(target)", "!isJavaClass(target)", "!isJSAdapter(target)", "!isProxy(target)", "!isJavaPackage(target)", "!isJavaConstructor(target)",
                    "!isForeignObject(target)"})
    public Object createFunctionTypeError(Object target) {
        throw Errors.createTypeErrorNotAFunction(target, this);
    }

    private Object[] getAbstractFunctionArguments(VirtualFrame frame) {
        Object[] args = new Object[arguments.getCount(frame)];
        args = arguments.executeFillObjectArray(frame, args, 0);
        return args;
    }

    private JSFunctionCallNode getCallNewTarget() {
        if (callNewTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callNewTarget = insert(JSFunctionCallNode.createNewTarget());
        }
        return callNewTarget;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(getTarget()), AbstractFunctionArgumentsNode.cloneUninitialized(arguments));
    }

    public abstract static class SpecializedNewObjectNode extends JSTargetableNode {
        private final JSContext context;
        protected final boolean isBuiltin;
        protected final boolean isConstructor;
        protected final boolean isGenerator;

        @Child @Executed protected JavaScriptNode targetNode;
        @Child @Executed(with = "targetNode") protected CachedPrototypeShapeNode cachedShapeNode;

        public SpecializedNewObjectNode(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, JavaScriptNode targetNode) {
            this.context = context;
            this.isBuiltin = isBuiltin;
            this.isConstructor = isConstructor;
            this.isGenerator = isGenerator;
            this.targetNode = targetNode;
            this.cachedShapeNode = CachedPrototypeShapeNode.create(context);
        }

        public static SpecializedNewObjectNode create(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, JavaScriptNode target) {
            return SpecializedNewObjectNodeGen.create(context, isBuiltin, isConstructor, isGenerator, target);
        }

        @Override
        public JavaScriptNode getTarget() {
            return targetNode;
        }

        @Override
        public final Object evaluateTarget(VirtualFrame frame) {
            return getTarget().execute(frame);
        }

        @Specialization(guards = {"!isBuiltin", "isConstructor"})
        public DynamicObject createUserObject(@SuppressWarnings("unused") DynamicObject target, Shape shape) {
            return JSObject.create(context, shape);
        }

        @Specialization(guards = {"!isBuiltin", "isConstructor", "isJSObject(proto)"})
        public DynamicObject createUserObject(@SuppressWarnings("unused") DynamicObject target, DynamicObject proto) {
            return JSUserObject.createWithPrototypeInObject(proto, context);
        }

        @Specialization(guards = {"!isBuiltin", "isConstructor", "isUndefined(shape)"})
        public DynamicObject createUserObjectAsObject(DynamicObject target, Object shape) {
            assert shape == Undefined.instance;
            // user-provided prototype is not an object
            JSRealm realm = JSRuntime.getFunctionRealm(target, context.getRealm());
            return createUserObject(target, isGenerator ? realm.getInitialGeneratorObjectShape() : realm.getInitialUserObjectShape());
        }

        @Specialization(guards = {"isBuiltin", "isConstructor"})
        public Object useConstruct(@SuppressWarnings("unused") DynamicObject target, @SuppressWarnings("unused") Object shape) {
            return JSFunction.CONSTRUCT;
        }

        @TruffleBoundary
        @Specialization(guards = {"!isConstructor"})
        public Object throwNotConstructorFunctionTypeError(DynamicObject target, @SuppressWarnings("unused") Object shape) {
            throw Errors.createTypeErrorNotConstructible(target);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(context, isBuiltin, isConstructor, isGenerator, cloneUninitialized(targetNode));
        }
    }

    @ImportStatic(JSTruffleOptions.class)
    protected abstract static class CachedPrototypeShapeNode extends JavaScriptBaseNode {
        protected final JSContext context;
        @Child private JSTargetableNode getPrototype;

        protected CachedPrototypeShapeNode(JSContext context) {
            this.context = context;
            this.getPrototype = PropertyNode.createProperty(context, null, JSObject.PROTOTYPE);
        }

        public static CachedPrototypeShapeNode create(JSContext context) {
            return CachedPrototypeShapeNodeGen.create(context);
        }

        public final Object executeWithTarget(VirtualFrame frame, Object target) {
            Object result = getPrototype.executeWithTarget(frame, target);
            return executeWithPrototype(frame, result);
        }

        public abstract Object executeWithPrototype(VirtualFrame frame, Object prototype);

        protected Object getProtoChildShape(Object prototype) {
            CompilerAsserts.neverPartOfCompilation();
            if (JSGuards.isJSObject(prototype)) {
                return JSObjectUtil.getProtoChildShape(((DynamicObject) prototype), JSUserObject.INSTANCE, context);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "prototype == cachedPrototype", limit = "PropertyCacheLimit")
        protected static Object doCached(Object prototype,
                        @Cached("prototype") Object cachedPrototype,
                        @Cached("getProtoChildShape(prototype)") Object cachedShape) {
            return cachedShape;
        }

        /** Many different prototypes. */
        @Specialization(replaces = "doCached")
        protected final Object doUncached(Object prototype,
                        @Cached("create()") BranchProfile notAnObjectBranch,
                        @Cached("create()") BranchProfile slowBranch) {
            if (JSGuards.isJSObject(prototype)) {
                return JSObjectUtil.getProtoChildShape(((DynamicObject) prototype), JSUserObject.INSTANCE, context, slowBranch);
            }
            notAnObjectBranch.enter();
            return Undefined.instance;
        }
    }
}
