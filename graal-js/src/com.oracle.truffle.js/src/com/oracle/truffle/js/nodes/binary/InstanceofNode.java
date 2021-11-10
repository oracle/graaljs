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
package com.oracle.truffle.js.nodes.binary;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNodeGen.IsBoundFunctionCacheNodeGen;
import com.oracle.truffle.js.nodes.binary.InstanceofNodeGen.OrdinaryHasInstanceNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/*
 * ES6, 12.9.4: Runtime Semantics: InstanceofOperator(O, C).
 */
@ImportStatic({JSConfig.class})
public abstract class InstanceofNode extends JSBinaryNode {
    protected final JSContext context;

    protected InstanceofNode(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
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
        return GetMethodNode.create(context, Symbol.SYMBOL_HAS_INSTANCE);
    }

    @Specialization(guards = {"isObjectNode.executeBoolean(target)"}, limit = "1")
    protected boolean doJSObject(Object obj, DynamicObject target,
                    @Cached("create()") @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                    @Cached("createGetMethodHasInstance()") GetMethodNode getMethodHasInstanceNode,
                    @Cached("create()") JSToBooleanNode toBooleanNode,
                    @Cached("createCall()") JSFunctionCallNode callHasInstanceNode,
                    @Cached("create()") IsCallableNode isCallableNode,
                    @Cached("createBinaryProfile()") ConditionProfile hasInstanceProfile,
                    @Cached("create()") BranchProfile errorBranch) {
        Object hasInstance = getMethodHasInstanceNode.executeWithTarget(target);
        if (hasInstanceProfile.profile(hasInstance == Undefined.instance)) {
            // Fall back to default instanceof semantics (legacy instanceof).
            if (!isCallableNode.executeBoolean(target)) {
                errorBranch.enter();
                throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
            }
            // Call OrdinaryHasInstance via an internal helper function. By doing it this way, we
            // can break out of any recursion and let the compiler decide where to cut off inlining.
            hasInstance = getRealm().getOrdinaryHasInstanceFunction();
        }
        Object res = callHasInstanceNode.executeCall(JSArguments.createOneArg(target, hasInstance, obj));
        return toBooleanNode.executeBoolean(res);
    }

    @Specialization(guards = {"isNullOrUndefined(target)"})
    protected boolean doNullOrUndefinedTarget(@SuppressWarnings("unused") Object obj, DynamicObject target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doStringTarget(@SuppressWarnings("unused") Object obj, String target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doDoubleTarget(@SuppressWarnings("unused") Object obj, double target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doBooleanTarget(@SuppressWarnings("unused") Object obj, boolean target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doBigIntTarget(@SuppressWarnings("unused") Object obj, BigInt target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doSymbolTarget(@SuppressWarnings("unused") Object obj, Symbol target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization(guards = {"isForeignObject(target)", "isJSDynamicObject(instance)"})
    protected boolean doForeignTargetJSType(@SuppressWarnings("unused") DynamicObject instance, @SuppressWarnings("unused") Object target) {
        return false;
    }

    @Specialization(guards = {"isForeignObject(target)", "!isJSDynamicObject(instance)"}, limit = "InteropLibraryLimit")
    protected boolean doForeignTargetOther(Object instance, Object target,
                    @CachedLibrary("target") InteropLibrary interop) {
        try {
            return interop.isMetaInstance(target, instance);
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return InstanceofNodeGen.create(context, cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }

    // ES6, 7.3.19, OrdinaryHasInstance (C, O).
    public abstract static class OrdinaryHasInstanceNode extends JavaScriptBaseNode {

        protected final JSContext context;
        @CompilationFinal private boolean lessThan4 = true;
        @Child private PropertyGetNode getPrototypeNode;
        @Child private IsBoundFunctionCacheNode boundFuncCacheNode;
        @Child protected IsCallableNode isCallableNode;

        public abstract boolean executeBoolean(Object left, Object right);

        protected OrdinaryHasInstanceNode(JSContext context) {
            this.context = context;
            this.boundFuncCacheNode = IsBoundFunctionCacheNode.create(context);
            this.isCallableNode = IsCallableNode.create();
        }

        public static OrdinaryHasInstanceNode create(JSContext context) {
            return OrdinaryHasInstanceNodeGen.create(context);
        }

        private DynamicObject getConstructorPrototype(DynamicObject rhs, BranchProfile invalidPrototypeBranch) {
            if (getPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPrototypeNode = insert(PropertyGetNode.create(JSObject.PROTOTYPE, context));
            }
            Object proto = getPrototypeNode.getValue(rhs);
            if (!(JSRuntime.isObject(proto))) {
                invalidPrototypeBranch.enter();
                throw createTypeErrorInvalidPrototype(rhs, proto);
            }
            return (DynamicObject) proto;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallableNode.executeBoolean(check)")
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

        @Specialization(guards = {"isObjectNode.executeBoolean(left)", "isJSFunction(right)", "!isBoundFunction(right)"}, limit = "1")
        protected boolean doJSObject(DynamicObject left, DynamicObject right,
                        @Cached @Shared("isObjectNode") @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached @Shared("getPrototype1Node") GetPrototypeNode getPrototype1Node,
                        @Cached @Shared("getPrototype2Node") GetPrototypeNode getPrototype2Node,
                        @Cached @Shared("getPrototype3Node") GetPrototypeNode getPrototype3Node,
                        @Cached @Shared("firstTrue") BranchProfile firstTrue,
                        @Cached @Shared("firstFalse") BranchProfile firstFalse,
                        @Cached @Shared("need2Hops") BranchProfile need2Hops,
                        @Cached @Shared("need3Hops") BranchProfile need3Hops,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached @Shared("invalidPrototypeBranch") BranchProfile invalidPrototypeBranch) {
            DynamicObject ctorPrototype = getConstructorPrototype(right, invalidPrototypeBranch);
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
            return doJSObject4(left, ctorPrototype, getPrototype3Node, errorBranch);
        }

        @Specialization(guards = {"isObjectNode.executeBoolean(left)", "isJSProxy(right)", "isCallableProxy(right)"}, limit = "1")
        protected boolean doJSObjectProxy(DynamicObject left, DynamicObject right,
                        @Cached @Shared("isObjectNode") IsJSObjectNode isObjectNode,
                        @Cached @Shared("getPrototype1Node") GetPrototypeNode getPrototype1Node,
                        @Cached @Shared("getPrototype2Node") GetPrototypeNode getPrototype2Node,
                        @Cached @Shared("getPrototype3Node") GetPrototypeNode getPrototype3Node,
                        @Cached @Shared("firstTrue") BranchProfile firstTrue,
                        @Cached @Shared("firstFalse") BranchProfile firstFalse,
                        @Cached @Shared("need2Hops") BranchProfile need2Hops,
                        @Cached @Shared("need3Hops") BranchProfile need3Hops,
                        @Cached @Shared("errorBranch") BranchProfile errorBranch,
                        @Cached @Shared("invalidPrototypeBranch") BranchProfile invalidPrototypeBranch) {
            return doJSObject(left, right, isObjectNode, getPrototype1Node, getPrototype2Node, getPrototype3Node, firstTrue, firstFalse, need2Hops, need3Hops, errorBranch, invalidPrototypeBranch);
        }

        private boolean doJSObject4(DynamicObject obj, DynamicObject check, GetPrototypeNode getLoopedPrototypeNode, BranchProfile errorBranch) {
            DynamicObject proto = obj;
            int counter = 0;
            while ((proto = getLoopedPrototypeNode.executeJSObject(proto)) != Null.instance) {
                counter++;
                if (counter > context.getContextOptions().getMaxPrototypeChainLength()) {
                    errorBranch.enter();
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
        private JSException createTypeErrorInvalidPrototype(DynamicObject obj, Object proto) {
            return Errors.createTypeError("\"prototype\" of " + JSRuntime.safeToString(obj) + " is not an Object, it is " + JSRuntime.safeToString(proto), this);
        }
    }

    /**
     * Caches on the passed function and its property of being a "bound function exotic object".
     *
     * Bound functions have internal slots due to which we can identify them with a shape check.
     */
    public abstract static class IsBoundFunctionCacheNode extends JavaScriptBaseNode {
        final boolean multiContext;

        public abstract boolean executeBoolean(DynamicObject func);

        protected IsBoundFunctionCacheNode(boolean multiContext) {
            this.multiContext = multiContext;
        }

        public static IsBoundFunctionCacheNode create(JSContext context) {
            return IsBoundFunctionCacheNodeGen.create(context.isMultiContext());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!multiContext", "func == cachedFunction"}, limit = "1")
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

    public static final class OrdinaryHasInstanceRootNode extends JavaScriptRootNode {
        @Child OrdinaryHasInstanceNode ordinaryHasInstanceNode;

        public OrdinaryHasInstanceRootNode(JSContext context) {
            this.ordinaryHasInstanceNode = OrdinaryHasInstanceNode.create(context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            Object target = JSArguments.getThisObject(arguments);
            Object obj = JSArguments.getUserArgument(arguments, 0);
            return ordinaryHasInstanceNode.executeBoolean(obj, target);
        }

        @Override
        public boolean isInternal() {
            return true;
        }
    }
}
