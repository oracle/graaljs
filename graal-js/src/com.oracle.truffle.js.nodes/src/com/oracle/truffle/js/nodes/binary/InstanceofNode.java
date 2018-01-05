/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNodeGen.IsBoundFunctionCacheNodeGen;
import com.oracle.truffle.js.nodes.binary.InstanceofNodeGen.OrdinaryHasInstanceNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/*
 * ES6, 12.9.4: Runtime Semantics: InstanceofOperator(O, C).
 */
public abstract class InstanceofNode extends JSBinaryNode {
    protected final JSContext context;
    @Child private OrdinaryHasInstanceNode ordinaryHasInstanceNode;

    InstanceofNode(JSContext context) {
        this.context = context;
    }

    public static InstanceofNode create(JSContext context) {
        return create(context, null, null);
    }

    public static InstanceofNode create(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        return InstanceofNodeGen.create(context, left, right);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    public abstract boolean executeBoolean(Object left, Object right);

    GetMethodNode createGetMethodHasInstance() {
        return GetMethodNode.create(context, null, Symbol.SYMBOL_HAS_INSTANCE);
    }

    @Specialization
    protected boolean doGeneric(Object obj, DynamicObject target,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("createGetMethodHasInstance()") GetMethodNode getMethodHasInstanceNode,
                    @Cached("create()") JSToBooleanNode toBooleanNode,
                    @Cached("createCall()") JSFunctionCallNode callHasInstanceNode,
                    @Cached("createBinaryProfile()") ConditionProfile hasInstanceProfile,
                    @Cached("create()") BranchProfile errorBranch,
                    @Cached("create()") BranchProfile proxyBranch) {
        if (!isObjectNode.executeBoolean(target)) {
            throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
        }
        Object hasInstance = getMethodHasInstanceNode.executeWithTarget(target);
        if (hasInstanceProfile.profile(hasInstance != Undefined.instance)) {
            Object res = callHasInstanceNode.executeCall(JSArguments.createOneArg(target, hasInstance, obj));
            return toBooleanNode.executeBoolean(res);
        } else {
            // legacy instanceof
            if (!isCallable(target, proxyBranch)) {
                errorBranch.enter();
                throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
            }
            if (ordinaryHasInstanceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                ordinaryHasInstanceNode = insert(OrdinaryHasInstanceNode.create(context));
            }
            return ordinaryHasInstanceNode.executeBoolean(obj, target);
        }

    }

    private static boolean isCallable(DynamicObject target, BranchProfile proxyBranch) {
        if (JSFunction.isJSFunction(target)) {
            return true;
        } else if (JSProxy.isProxy(target)) {
            proxyBranch.enter();
            return JSRuntime.isCallableProxy(target);
        } else {
            return false;
        }
    }

    @Specialization(guards = {"!isJavaObject(obj)", "isJavaInteropClass(clazz)"})
    protected boolean instanceofJavaClass(Object obj, Object clazz) {
        return ((Class<?>) JavaInterop.asJavaObject((TruffleObject) clazz)).isInstance(obj);
    }

    @Specialization(guards = {"isJavaObject(obj)", "isJavaInteropClass(clazz)"})
    protected boolean instanceofJavaClassUnwrap(Object obj, Object clazz) {
        return ((Class<?>) JavaInterop.asJavaObject((TruffleObject) clazz)).isInstance(JavaInterop.asJavaObject((TruffleObject) obj));
    }

    protected static boolean isJavaObject(Object obj) {
        return JavaInterop.isJavaObject(obj);
    }

    protected static boolean isJavaInteropClass(Object obj) {
        return JavaInterop.isJavaObject(obj) && JavaInterop.asJavaObject((TruffleObject) obj) instanceof Class;
    }

    @Specialization(guards = {"!isJavaObject(obj)"})
    protected static boolean doJava(Object obj, JavaClass clazz) {
        return clazz.getType().isInstance(obj);
    }

    @Specialization(guards = {"isJavaObject(obj)"})
    protected static boolean doJavaUnwrap(Object obj, JavaClass clazz) {
        return clazz.getType().isInstance(JavaInterop.asJavaObject((TruffleObject) obj));
    }

    @Specialization(guards = {"!isDynamicObject(target)", "!isJavaInteropClass(target)", "!isJavaClass(target)"})
    protected boolean doRHSNotAnObject(@SuppressWarnings("unused") Object obj, Object target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @TruffleBoundary
    private static String functionToString(DynamicObject fnObj) {
        assert JSFunction.isJSFunction(fnObj);
        RootCallTarget dct = (RootCallTarget) JSFunction.getCallTarget(fnObj);
        RootNode rn = dct.getRootNode();
        SourceSection ssect = rn.getSourceSection();
        return ((ssect == null || !ssect.isAvailable()) ? "function " + JSFunction.getName(fnObj) + "() { [native code] }" : ssect.getCharacters().toString());
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return InstanceofNodeGen.create(context, cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }

    // ES6, 7.3.19, OrdinaryHasInstance (C, O).
    public abstract static class OrdinaryHasInstanceNode extends JavaScriptBaseNode {

        protected final JSContext context;
        @CompilationFinal private boolean lessThan4 = true;
        @Child private PropertyNode getPrototypeNode;
        @Child private IsBoundFunctionCacheNode boundFuncCacheNode;
        @Child private IsObjectNode isObjectNode;
        private final BranchProfile invalidPrototypeBranch = BranchProfile.create();

        public abstract boolean executeBoolean(Object left, Object right);

        protected OrdinaryHasInstanceNode(JSContext context) {
            this.context = context;
            this.getPrototypeNode = NodeFactory.getInstance(context).createProperty(context, null, JSObject.PROTOTYPE);
            this.boundFuncCacheNode = IsBoundFunctionCacheNode.create();
            this.isObjectNode = IsObjectNode.create();
        }

        public static OrdinaryHasInstanceNode create(JSContext context) {
            return OrdinaryHasInstanceNodeGen.create(context);
        }

        // longer name to avoid name-clash
        boolean isObjectLocal(DynamicObject lhs) {
            return isObjectNode.executeBoolean(lhs);
        }

        private DynamicObject getConstructorPrototype(DynamicObject rhs) {
            Object proto = getPrototypeNode.executeWithTarget(rhs);
            if (!(JSRuntime.isObject(proto))) {
                invalidPrototypeBranch.enter();
                throw typeErrorInvalidPrototype(rhs, proto);
            }
            return (DynamicObject) proto;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallable(check)")
        protected boolean doNotCallable(Object obj, Object check) {
            return false;
        }

        @Specialization(guards = {"isJSFunction(check)", "isBoundFunction(check)"})
        protected boolean doIsBound(Object obj, DynamicObject check,
                        @Cached("create(context)") InstanceofNode instanceofNode) {
            DynamicObject boundTargetFunction = JSFunction.getBoundTargetFunction(check);
            return instanceofNode.executeBoolean(obj, boundTargetFunction);
        }

        @Specialization(guards = {"!isJSObject(left)", "isJSFunction(right)", "!isBoundFunction(right)"})
        protected boolean doNotAnObject(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") DynamicObject right) {
            return false;
        }

        @Specialization(guards = {"!isJSObject(left)", "isJSProxy(right)", "isCallableProxy(right)"})
        protected boolean doNotAnObjectProxy(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") DynamicObject right) {
            return false;
        }

        @Specialization(guards = {"isObjectLocal(left)", "isJSFunction(right)", "!isBoundFunction(right)"})
        protected boolean doJSObject(DynamicObject left, DynamicObject right,
                        @Cached("create()") GetPrototypeNode getPrototype1Node,
                        @Cached("create()") GetPrototypeNode getPrototype2Node,
                        @Cached("create()") GetPrototypeNode getPrototype3Node,
                        @Cached("create()") BranchProfile firstTrue,
                        @Cached("create()") BranchProfile firstFalse,
                        @Cached("create()") BranchProfile need2Hops,
                        @Cached("create()") BranchProfile need3Hops) {
            DynamicObject ctorPrototype = getConstructorPrototype(right);
            if (lessThan4) {
                DynamicObject proto = getPrototype1Node.executeJSObject(left);
                if (proto == ctorPrototype) {
                    firstTrue.enter();
                    return true;
                } else if (proto == Null.instance) {
                    firstFalse.enter();
                    return false;
                }
                need2Hops.enter();
                proto = getPrototype2Node.executeJSObject(proto);
                if (proto == ctorPrototype) {
                    return true;
                } else if (proto == Null.instance) {
                    return false;
                }
                need3Hops.enter();
                proto = getPrototype3Node.executeJSObject(proto);
                if (proto == ctorPrototype) {
                    return true;
                }
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lessThan4 = false;
            }
            return doJSObject4(left, ctorPrototype, getPrototype3Node);
        }

        @Specialization(guards = {"isObjectLocal(left)", "isJSProxy(right)", "isCallableProxy(right)"})
        protected boolean doJSObjectProxy(DynamicObject left, DynamicObject right,
                        @Cached("create()") GetPrototypeNode getPrototype1Node,
                        @Cached("create()") GetPrototypeNode getPrototype2Node,
                        @Cached("create()") GetPrototypeNode getPrototype3Node,
                        @Cached("create()") BranchProfile firstTrue,
                        @Cached("create()") BranchProfile firstFalse,
                        @Cached("create()") BranchProfile need2Hops,
                        @Cached("create()") BranchProfile need3Hops) {
            return doJSObject(left, right, getPrototype1Node, getPrototype2Node, getPrototype3Node, firstTrue, firstFalse, need2Hops, need3Hops);
        }

        private static boolean doJSObject4(DynamicObject obj, DynamicObject check, GetPrototypeNode getPrototypeNode) {
            DynamicObject proto = obj;
            int counter = 0;
            while ((proto = getPrototypeNode.executeJSObject(proto)) != Null.instance) {
                counter++;
                if (counter > JSTruffleOptions.MaxExpectedPrototypeChainLength) {
                    throw Errors.createRangeError("prototype chain length exceeded");
                }
                if (proto == check) {
                    return true;
                }
            }
            return false;
        }

        protected boolean isBoundFunction(DynamicObject func) {
            assert JSFunction.isJSFunction(func);
            return boundFuncCacheNode.executeBoolean(func);
        }

        @TruffleBoundary
        private static RuntimeException typeErrorInvalidPrototype(DynamicObject obj, Object proto) {
            String name;
            if (JSFunction.isJSFunction(obj)) {
                name = functionToString(obj);
            } else {
                name = obj.toString();
            }
            throw Errors.createTypeError("\"prototype\" of " + name + " is not an Object, it is " + JSRuntime.objectToString(proto));
        }
    }

    /**
     * Caches on the passed function and its property of being a "bound function exotic object".
     *
     * Bound functions have internal slots due to which we can identify them with a shape check.
     */
    public abstract static class IsBoundFunctionCacheNode extends JavaScriptBaseNode {
        public abstract boolean executeBoolean(DynamicObject func);

        protected IsBoundFunctionCacheNode() {
        }

        public static IsBoundFunctionCacheNode create() {
            return IsBoundFunctionCacheNodeGen.create();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "func == cachedFunction", limit = "1")
        protected static boolean doCachedInstance(DynamicObject func,
                        @Cached("func") DynamicObject cachedFunction,
                        @Cached("isBoundFunction(func)") boolean cachedIsBound) {
            assert isBoundFunction(func) == cachedIsBound;
            return cachedIsBound;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "cachedShape.check(func)", replaces = "doCachedInstance")
        protected static boolean doCachedShape(DynamicObject func,
                        @Cached("func.getShape()") Shape cachedShape,
                        @Cached("isBoundFunction(func)") boolean cachedIsBound) {
            assert isBoundFunction(func) == cachedIsBound;
            return cachedIsBound;
        }

        @Specialization(replaces = "doCachedShape")
        protected static boolean isBoundFunction(DynamicObject func) {
            assert JSFunction.isJSFunction(func);
            return JSFunction.isBoundFunction(func);
        }
    }

}
