/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNodeGen.OrdinaryHasInstanceNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
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
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/*
 * ES6, 12.9.4: Runtime Semantics: InstanceofOperator(O, C).
 */
@ImportStatic({JSConfig.class})
@NodeInfo(shortName = "instanceof")
public abstract class InstanceofNode extends JSBinaryNode {
    protected final JSContext context;

    protected InstanceofNode(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
        this.context = context;
    }

    @NeverDefault
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

    @NeverDefault
    GetMethodNode createGetMethodHasInstance() {
        return GetMethodNode.create(context, Symbol.SYMBOL_HAS_INSTANCE);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isObjectNode.executeBoolean(target)"}, limit = "1")
    protected boolean doJSObject(Object obj, JSDynamicObject target,
                    @Bind Node node,
                    @Cached @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                    @Cached("createGetMethodHasInstance()") GetMethodNode getMethodHasInstanceNode,
                    @Cached(inline = true) JSToBooleanNode toBooleanNode,
                    @Cached("createCall()") JSFunctionCallNode callHasInstanceNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached InlinedConditionProfile hasInstanceProfile,
                    @Cached InlinedBranchProfile errorBranch) {
        Object hasInstance = getMethodHasInstanceNode.executeWithTarget(target);
        if (hasInstanceProfile.profile(node, hasInstance == Undefined.instance)) {
            // Fall back to default instanceof semantics (legacy instanceof).
            if (!isCallableNode.executeBoolean(target)) {
                errorBranch.enter(node);
                throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
            }
            // Call OrdinaryHasInstance via an internal helper function. By doing it this way, we
            // can break out of any recursion and let the compiler decide where to cut off inlining.
            hasInstance = getRealm().getOrdinaryHasInstanceFunction();
        }
        Object res = callHasInstanceNode.executeCall(JSArguments.createOneArg(target, hasInstance, obj));
        return toBooleanNode.executeBoolean(node, res);
    }

    @Specialization(guards = {"isNullOrUndefined(target)"})
    protected boolean doNullOrUndefinedTarget(@SuppressWarnings("unused") Object obj, Object target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization
    protected boolean doStringTarget(@SuppressWarnings("unused") Object obj, TruffleString target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization()
    protected boolean doDoubleTarget(@SuppressWarnings("unused") Object obj, double target) {
        throw Errors.createTypeErrorInvalidInstanceofTarget(target, this);
    }

    @Specialization
    protected boolean doLongTarget(@SuppressWarnings("unused") Object obj, long target) {
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

    @Specialization(guards = {"isForeignObject(target)"})
    protected boolean doForeignTargetJSType(@SuppressWarnings("unused") JSDynamicObject instance, @SuppressWarnings("unused") Object target) {
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
    @ImportStatic({JSFunction.class})
    public abstract static class OrdinaryHasInstanceNode extends JavaScriptBaseNode {

        protected final JSContext context;
        @CompilationFinal private boolean lessThan4 = true;
        @Child private PropertyGetNode getPrototypeNode;
        @Child protected IsCallableNode isCallableNode;

        public abstract boolean executeBoolean(Object left, Object right);

        protected OrdinaryHasInstanceNode(JSContext context) {
            this.context = context;
            this.isCallableNode = IsCallableNode.create();
            this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, context);
        }

        @NeverDefault
        public static OrdinaryHasInstanceNode create(JSContext context) {
            return OrdinaryHasInstanceNodeGen.create(context);
        }

        private JSObject getConstructorPrototype(JSObject target, InlinedBranchProfile invalidPrototypeBranch) {
            Object proto = getPrototypeNode.getValue(target);
            if (!(proto instanceof JSObject)) {
                invalidPrototypeBranch.enter(this);
                throw createTypeErrorInvalidPrototype(target, proto);
            }
            return (JSObject) proto;
        }

        @Specialization(guards = {"isObjectNode.executeBoolean(left)", "!isBoundFunction(right)"})
        protected final boolean doJSObjectFunction(Object left, JSFunctionObject right,
                        @Cached @Shared @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached @Shared GetPrototypeNode getPrototype1Node,
                        @Cached @Shared GetPrototypeNode getPrototype2Node,
                        @Cached @Shared GetPrototypeNode getPrototype3Node,
                        @Cached @Shared InlinedBranchProfile firstTrue,
                        @Cached @Shared InlinedBranchProfile firstFalse,
                        @Cached @Shared InlinedBranchProfile need2Hops,
                        @Cached @Shared InlinedBranchProfile need3Hops,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedBranchProfile invalidPrototypeBranch) {
            return doJSObject((JSObject) left, right, getPrototype1Node, getPrototype2Node, getPrototype3Node, firstTrue, firstFalse, need2Hops, need3Hops, errorBranch, invalidPrototypeBranch);
        }

        @Specialization(guards = {"isObjectNode.executeBoolean(left)", "isCallableProxy(right)"})
        protected final boolean doJSObjectProxy(Object left, JSProxyObject right,
                        @Cached @Shared @SuppressWarnings("unused") IsJSObjectNode isObjectNode,
                        @Cached @Shared GetPrototypeNode getPrototype1Node,
                        @Cached @Shared GetPrototypeNode getPrototype2Node,
                        @Cached @Shared GetPrototypeNode getPrototype3Node,
                        @Cached @Shared InlinedBranchProfile firstTrue,
                        @Cached @Shared InlinedBranchProfile firstFalse,
                        @Cached @Shared InlinedBranchProfile need2Hops,
                        @Cached @Shared InlinedBranchProfile need3Hops,
                        @Cached @Shared InlinedBranchProfile errorBranch,
                        @Cached @Shared InlinedBranchProfile invalidPrototypeBranch) {
            return doJSObject((JSObject) left, right, getPrototype1Node, getPrototype2Node, getPrototype3Node, firstTrue, firstFalse, need2Hops, need3Hops, errorBranch, invalidPrototypeBranch);
        }

        @Specialization
        protected boolean doBound(Object obj, JSFunctionObject.Bound bound,
                        @Cached("create(context)") InstanceofNode instanceofNode) {
            Object boundTargetFunction = bound.getBoundTargetFunction();
            return instanceofNode.executeBoolean(obj, boundTargetFunction);
        }

        private boolean doForeignObject(Object left, JSObject right,
                        @Cached @Shared IsObjectNode isAnyObjectNode,
                        @Cached @Shared ForeignObjectPrototypeNode getForeignPrototypeNode,
                        @Cached @Shared InlinedBranchProfile invalidPrototypeBranch,
                        @Cached("create(context)") @Shared OrdinaryHasInstanceNode ordinaryHasInstanceNode) {
            if (context.isOptionForeignObjectPrototype()) {
                return doForeignObjectPrototype(left, right, isAnyObjectNode, getForeignPrototypeNode, invalidPrototypeBranch, ordinaryHasInstanceNode);
            } else {
                return false;
            }
        }

        private boolean doForeignObjectPrototype(Object left, JSObject right, IsObjectNode isObjectNode, ForeignObjectPrototypeNode getForeignPrototypeNode,
                        InlinedBranchProfile invalidPrototypeBranch, OrdinaryHasInstanceNode ordinaryHasInstanceNode) {
            if (!isObjectNode.executeBoolean(left)) {
                return false;
            }
            JSObject rightProto = getConstructorPrototype(right, invalidPrototypeBranch);
            Object foreignProto = getForeignPrototypeNode.execute(left);
            if (foreignProto == rightProto) {
                return true;
            }
            return ordinaryHasInstanceNode.executeBoolean(foreignProto, right);
        }

        @Specialization(guards = {"!isJSObject(left)", "isForeignObject(left)", "!isBoundFunction(right)"})
        protected final boolean doForeignObjectUnbound(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") JSFunctionObject right,
                        @Cached @Shared IsObjectNode isAnyObjectNode,
                        @Cached @Shared ForeignObjectPrototypeNode getForeignPrototypeNode,
                        @Cached @Shared InlinedBranchProfile invalidPrototypeBranch,
                        @Cached("create(context)") @Shared OrdinaryHasInstanceNode ordinaryHasInstanceNode) {
            return doForeignObject(left, right, isAnyObjectNode, getForeignPrototypeNode, invalidPrototypeBranch, ordinaryHasInstanceNode);
        }

        @Specialization(guards = {"!isJSObject(left)", "isForeignObject(left)", "isCallableProxy(right)"})
        protected final boolean doForeignObjectProxy(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") JSProxyObject right,
                        @Cached @Shared IsObjectNode isAnyObjectNode,
                        @Cached @Shared ForeignObjectPrototypeNode getForeignPrototypeNode,
                        @Cached @Shared InlinedBranchProfile invalidPrototypeBranch,
                        @Cached("create(context)") @Shared OrdinaryHasInstanceNode ordinaryHasInstanceNode) {
            return doForeignObject(left, right, isAnyObjectNode, getForeignPrototypeNode, invalidPrototypeBranch, ordinaryHasInstanceNode);
        }

        @Specialization(guards = {"!isJSObject(left)", "!isForeignObject(left)", "!isBoundFunction(right)"})
        protected static boolean doNotAnObjectUnbound(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") JSFunctionObject right) {
            return false;
        }

        @Specialization(guards = {"!isJSObject(left)", "!isForeignObject(left)", "isCallableProxy(right)"})
        protected static boolean doNotAnObjectProxy(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") JSProxyObject right) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isCallableNode.executeBoolean(target)")
        protected static boolean doNotCallable(Object obj, Object target) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isForeignObject(target)")
        protected static boolean doForeignTarget(Object obj, Object target) {
            return false;
        }

        private boolean doJSObject(JSObject left, JSObject right,
                        GetPrototypeNode getPrototype1Node,
                        GetPrototypeNode getPrototype2Node,
                        GetPrototypeNode getPrototype3Node,
                        InlinedBranchProfile firstTrue,
                        InlinedBranchProfile firstFalse,
                        InlinedBranchProfile need2Hops,
                        InlinedBranchProfile need3Hops,
                        InlinedBranchProfile errorBranch,
                        InlinedBranchProfile invalidPrototypeBranch) {
            assert JSRuntime.isCallableIsJSObject(right) : right;
            JSObject ctorPrototype = getConstructorPrototype(right, invalidPrototypeBranch);
            if (lessThan4) {
                JSDynamicObject proto = getPrototype1Node.execute(left);
                if (proto == ctorPrototype) {
                    firstTrue.enter(this);
                    return true;
                } else if (proto == Null.instance) {
                    firstFalse.enter(this);
                    return false;
                }
                need2Hops.enter(this);
                proto = getPrototype2Node.execute(proto);
                if (proto == ctorPrototype) {
                    return true;
                } else if (proto == Null.instance) {
                    return false;
                }
                need3Hops.enter(this);
                proto = getPrototype3Node.execute(proto);
                if (proto == ctorPrototype) {
                    return true;
                }
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lessThan4 = false;
            }
            return doJSObject4(left, ctorPrototype, getPrototype3Node, errorBranch);
        }

        private boolean doJSObject4(JSDynamicObject obj, JSObject check, GetPrototypeNode getLoopedPrototypeNode, InlinedBranchProfile errorBranch) {
            JSDynamicObject proto = obj;
            int counter = 0;
            while ((proto = getLoopedPrototypeNode.execute(proto)) != Null.instance) {
                counter++;
                if (counter > context.getLanguageOptions().maxPrototypeChainLength()) {
                    errorBranch.enter(this);
                    throw Errors.createRangeError("prototype chain length exceeded");
                }
                if (proto == check) {
                    return true;
                }
            }
            return false;
        }

        @TruffleBoundary
        private JSException createTypeErrorInvalidPrototype(JSDynamicObject obj, Object proto) {
            if (context.isOptionV8CompatibilityMode()) {
                return Errors.createTypeError("Function has non-object prototype '" + JSRuntime.safeToString(proto) + "' in instanceof check");
            } else {
                return Errors.createTypeError("\"prototype\" of " + JSRuntime.safeToString(obj) + " is not an Object, it is " + JSRuntime.safeToString(proto), this);
            }
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
