/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.AsyncFromSyncIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.AsyncGeneratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.EnumerateIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.ForInIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltins;
import com.oracle.truffle.js.builtins.GeneratorPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.binary.InstanceofNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Nullish;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;

public final class JSFunction extends JSNonProxy {

    public static final TruffleString TYPE_NAME = Strings.FUNCTION;
    public static final TruffleString CLASS_NAME = Strings.constant("Function");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Function.prototype");
    public static final TruffleString GENERATOR_FUNCTION_NAME = Strings.constant("GeneratorFunction");
    public static final TruffleString GENERATOR_NAME = Strings.constant("Generator");
    public static final TruffleString ASYNC_FUNCTION_NAME = Strings.constant("AsyncFunction");
    public static final TruffleString ASYNC_GENERATOR_FUNCTION_NAME = Strings.constant("AsyncGeneratorFunction");
    public static final TruffleString ASYNC_GENERATOR_NAME = Strings.constant("AsyncGenerator");
    public static final TruffleString ENUMERATE_ITERATOR_PROTOTYPE_NAME = Strings.constant("[[Enumerate]].prototype");
    public static final TruffleString CALLER = Strings.CALLER;
    public static final TruffleString ARGUMENTS = Strings.ARGUMENTS;
    public static final TruffleString LENGTH = Strings.LENGTH;
    public static final TruffleString NAME = Strings.NAME;
    public static final TruffleString ORDINARY_HAS_INSTANCE = Strings.constant("OrdinaryHasInstance");
    public static final String PROGRAM_FUNCTION_NAME = ":program";

    public static final String BUILTIN_SOURCE_NAME = "<builtin>";
    public static final TruffleString TS_BUILTIN_SOURCE_NAME = Strings.constant(BUILTIN_SOURCE_NAME);
    public static final SourceSection BUILTIN_SOURCE_SECTION = createBuiltinSourceSection(BUILTIN_SOURCE_NAME);

    public static final TruffleString ASYNC_FROM_SYNC_ITERATOR_PROTOTYPE_NAME = Strings.constant("%AsyncFromSyncIteratorPrototype%");

    public static final PropertyProxy PROTOTYPE_PROXY = new ClassPrototypeProxyProperty();

    public static final class FunctionLengthPropertyProxy extends PropertyProxy {
        @Override
        public Object get(JSDynamicObject store) {
            if (store instanceof JSFunctionObject.BoundOrWrapped boundFunction) {
                return boundFunction.getBoundLength();
            }
            return JSFunction.getLength((JSFunctionObject) store);
        }

        public static int getProfiled(JSDynamicObject store, BranchProfile isBoundBranch) {
            if (store instanceof JSFunctionObject.BoundOrWrapped boundFunction) {
                isBoundBranch.enter();
                return boundFunction.getBoundLength();
            }
            return JSFunction.getLength((JSFunctionObject) store);
        }

    }

    public static final PropertyProxy LENGTH_PROXY = new FunctionLengthPropertyProxy();

    public static final class FunctionNamePropertyProxy extends PropertyProxy {
        @Override
        public TruffleString get(JSDynamicObject store) {
            if (store instanceof JSFunctionObject.BoundOrWrapped boundFunction) {
                return boundFunction.getBoundName();
            }
            return JSFunction.getName((JSFunctionObject) store);
        }

        public static Object getProfiled(JSDynamicObject store, BranchProfile isBoundBranch) {
            if (store instanceof JSFunctionObject.BoundOrWrapped boundFunction) {
                isBoundBranch.enter();
                return boundFunction.getBoundName();
            }
            return JSFunction.getName((JSFunctionObject) store);
        }
    }

    public static final PropertyProxy NAME_PROXY = new FunctionNamePropertyProxy();

    public static final PropertyProxy ARGUMENTS_PROXY = new ArgumentsProxyProperty();
    public static final PropertyProxy CALLER_PROXY = new CallerProxyProperty();

    /** Placeholder for lazy initialization of the prototype property. */
    public static final Object CLASS_PROTOTYPE_PLACEHOLDER = new Object();

    public static final JSFunction INSTANCE = new JSFunction();

    public static final HiddenKey HOME_OBJECT_ID = new HiddenKey("HomeObject");
    public static final HiddenKey CLASS_ELEMENTS_ID = new HiddenKey("Elements");
    public static final HiddenKey CLASS_INITIALIZERS_ID = new HiddenKey("Initializers");
    public static final HiddenKey PRIVATE_BRAND_ID = new HiddenKey("PrivateBrand");

    /** Marker property to ensure generator function shapes are distinct from normal functions. */
    private static final HiddenKey GENERATOR_FUNCTION_MARKER_ID = new HiddenKey("generator function");
    private static final HiddenKey ASYNC_GENERATOR_FUNCTION_MARKER_ID = new HiddenKey("async generator function");

    /** Slot for scope object passed to inline parsed scripts. */
    public static final HiddenKey DEBUG_SCOPE_ID = new HiddenKey("Scope");

    public enum GeneratorState {
        SuspendedStart,
        SuspendedYield,
        Executing,
        Completed,
    }

    public enum AsyncGeneratorState {
        SuspendedStart,
        SuspendedYield,
        Executing,
        AwaitingReturn,
        Completed,
    }

    private JSFunction() {
    }

    public static CallTarget getCallTarget(JSFunctionObject obj) {
        return getFunctionData(obj).getCallTarget();
    }

    public static MaterializedFrame getEnclosingFrame(JSFunctionObject obj) {
        return obj.getEnclosingFrame();
    }

    public static JSFunctionData getFunctionData(JSFunctionObject obj) {
        return obj.getFunctionData();
    }

    private static Object getClassPrototypeField(JSFunctionObject obj) {
        return obj.getClassPrototype();
    }

    private static void setClassPrototypeField(JSFunctionObject obj, Object classPrototype) {
        obj.setClassPrototype(classPrototype);
    }

    public static JSRealm getRealm(JSFunctionObject obj) {
        return obj.getRealm();
    }

    /**
     * Version optimized for a single Realm.
     */
    public static JSRealm getRealm(JSFunctionObject functionObj, JSContext context, Node node) {
        JSRealm realm;
        if (context.isSingleRealm()) {
            realm = JSRealm.get(node);
            assert realm == getRealm(functionObj);
        } else {
            realm = getRealm(functionObj);
        }
        return realm;
    }

    public static JSFunctionObject create(JSRealm realm, JSFunctionData functionData) {
        return create(realm, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME);
    }

    public static JSFunctionObject create(JSRealm realm, JSFunctionData functionData, MaterializedFrame enclosingFrame) {
        return createDefault(functionData, enclosingFrame, CLASS_PROTOTYPE_PLACEHOLDER, realm);
    }

    public static JSFunctionObject createWithPrototype(JSRealm realm, JSFunctionData functionData, JSDynamicObject prototype) {
        return createWithPrototype(initialFactory(functionData), realm, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, prototype);
    }

    public static JSFunctionObject createWithPrototype(JSFunctionFactory factory, JSRealm realm, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSDynamicObject prototype) {
        return createWithPrototype(factory, functionData, enclosingFrame, CLASS_PROTOTYPE_PLACEHOLDER, realm, prototype);
    }

    public static JSFunctionObject createLexicalThis(JSRealm realm, JSFunctionData functionData, MaterializedFrame enclosingFrame, Object lexicalThis) {
        return createDefault(functionData, enclosingFrame, lexicalThis, realm);
    }

    @InliningCutoff
    private static JSFunctionObject createDefault(JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm) {
        JSFunctionFactory factory = initialFactory(functionData);
        return factory.create(functionData, enclosingFrame, classPrototype, realm);
    }

    private static JSFunctionObject createWithPrototype(JSFunctionFactory factory, JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm,
                    JSDynamicObject prototype) {
        return factory.createWithPrototype(functionData, enclosingFrame, classPrototype, realm, prototype);
    }

    public static JSFunctionObject createBound(JSContext context, JSRealm realm, JSFunctionData functionData, Object boundTargetFunction, Object boundThis, Object[] boundArguments) {
        JSFunctionFactory factory = context.getBoundFunctionFactory(functionData);
        return factory.createBound(functionData, CLASS_PROTOTYPE_PLACEHOLDER, realm, boundTargetFunction, boundThis, boundArguments);
    }

    public static JSFunctionObject createWrapped(JSContext context, JSRealm realm, JSFunctionData functionData, Object wrappedTargetFunction) {
        JSFunctionFactory factory = context.getWrappedFunctionFactory();
        return factory.createWrapped(functionData, realm, wrappedTargetFunction);
    }

    private static JSFunctionFactory initialFactory(JSFunctionData functionData) {
        return functionData.getContext().getFunctionFactory(functionData);
    }

    public static TruffleString getName(JSFunctionObject obj) {
        return getFunctionData(obj).getName();
    }

    public static Object call(JSFunctionObject functionObject, Object thisObject, Object[] argumentValues) {
        assert thisObject != null;
        Object[] arguments = JSArguments.create(thisObject, functionObject, argumentValues);
        return getCallTarget(functionObject).call(arguments);
    }

    public static Object call(Object[] jsArguments) {
        assert JSFunction.isJSFunction(JSArguments.getFunctionObject(jsArguments));
        assert JSArguments.getThisObject(jsArguments) != null;
        return getCallTarget((JSFunctionObject) JSArguments.getFunctionObject(jsArguments)).call(jsArguments);
    }

    public static Object construct(JSFunctionObject functionObject, Object[] argumentValues) {
        assert isConstructor(functionObject);
        Object[] arguments = JSArguments.create(CONSTRUCT, functionObject, argumentValues);
        return getConstructTarget(functionObject).call(arguments);
    }

    @TruffleBoundary
    public static JSFunctionObject bind(JSRealm realm, JSFunctionObject thisFnObj, Object thisArg, Object[] boundArguments) {
        JSContext context = realm.getContext();
        JSDynamicObject proto = JSObject.getPrototype(thisFnObj);
        JSFunctionObject boundFunction = boundFunctionCreate(context, thisFnObj, thisArg, boundArguments, proto,
                        InlinedConditionProfile.getUncached(), InlinedConditionProfile.getUncached(), InlinedConditionProfile.getUncached(), null);

        long length = 0;
        boolean targetHasLength = JSObject.hasOwnProperty(thisFnObj, JSFunction.LENGTH);
        boolean mustSetLength = true;
        if (targetHasLength) {
            Object targetLen = JSObject.get(thisFnObj, JSFunction.LENGTH);
            if (JSRuntime.isNumber(targetLen)) {
                long targetLenInt = JSRuntime.toInteger((Number) targetLen);
                length = Math.max(0, targetLenInt - boundArguments.length);
                if (targetLenInt == getLength(thisFnObj)) {
                    mustSetLength = false;
                }
            }
        }
        if (mustSetLength) {
            setFunctionLength(boundFunction, JSRuntime.longToIntOrDouble(length));
        }

        TruffleString targetName = getFunctionName(thisFnObj);
        if (!targetName.equals(getName(thisFnObj))) {
            setBoundFunctionName(boundFunction, targetName);
        }
        return boundFunction;
    }

    public static JSFunctionObject boundFunctionCreate(JSContext context, JSFunctionObject boundTargetFunction, Object boundThis, Object[] boundArguments, JSDynamicObject proto,
                    InlinedConditionProfile isConstructorProfile, InlinedConditionProfile isAsyncProfile, InlinedConditionProfile setProtoProfile, Node node) {
        CompilerAsserts.partialEvaluationConstant(context);

        JSFunctionData targetFunctionData = JSFunction.getFunctionData(boundTargetFunction);
        boolean constructor = isConstructorProfile.profile(node, targetFunctionData.isConstructor());
        boolean isAsync = isAsyncProfile.profile(node, targetFunctionData.isAsync());
        JSFunctionData boundFunctionData = context.getBoundFunctionData(constructor, isAsync);
        JSRealm realm = getRealm(boundTargetFunction, context, node);
        JSFunctionObject boundFunction = JSFunction.createBound(context, realm, boundFunctionData, boundTargetFunction, boundThis, boundArguments);
        boolean needSetProto = proto != realm.getFunctionPrototype();
        if (setProtoProfile.profile(node, needSetProto)) {
            JSObject.setPrototype(boundFunction, proto);
        }
        assert JSObject.getPrototype(boundFunction) == proto;
        return boundFunction;
    }

    @TruffleBoundary
    private static TruffleString getFunctionName(JSDynamicObject thisFnObj) {
        Object name = JSObject.get(thisFnObj, NAME);
        if (!(name instanceof TruffleString nameStr)) {
            return Strings.EMPTY_STRING;
        }
        return nameStr;
    }

    @TruffleBoundary
    public static void setFunctionLength(JSDynamicObject functionObj, Number length) {
        JSObject.defineOwnProperty(functionObj, JSFunction.LENGTH, PropertyDescriptor.createData(length, false, false, true));
    }

    @TruffleBoundary
    public static void setFunctionName(JSDynamicObject functionObj, TruffleString name) {
        JSObject.defineOwnProperty(functionObj, JSFunction.NAME, PropertyDescriptor.createData(name, false, false, true));
    }

    @TruffleBoundary
    public static void setBoundFunctionName(JSDynamicObject boundFunction, TruffleString targetName) {
        JSObject.defineOwnProperty(boundFunction, JSFunction.NAME, PropertyDescriptor.createData(Strings.concat(Strings.BOUND_SPC, targetName), false, false, true));
    }

    public static boolean isStrict(JSFunctionObject obj) {
        return getFunctionData(obj).isStrict();
    }

    public static boolean isBuiltin(JSFunctionObject obj) {
        return getFunctionData(obj).isBuiltin();
    }

    public static boolean isConstructor(JSFunctionObject obj) {
        return getFunctionData(obj).isConstructor();
    }

    public static boolean isConstructor(Object obj) {
        return obj instanceof JSFunctionObject function && getFunctionData(function).isConstructor();
    }

    public static boolean isGenerator(JSFunctionObject obj) {
        return getFunctionData(obj).isGenerator();
    }

    public static boolean needsParentFrame(JSFunctionObject obj) {
        return getFunctionData(obj).needsParentFrame();
    }

    public static int getLength(JSFunctionObject obj) {
        return getFunctionData(obj).getLength();
    }

    public static boolean isClassPrototypeInitialized(JSFunctionObject thisObj) {
        return getClassPrototypeField(thisObj) != CLASS_PROTOTYPE_PLACEHOLDER;
    }

    public static boolean isBoundFunction(Object function) {
        return function instanceof JSFunctionObject.Bound;
    }

    public static boolean isAsyncFunction(JSFunctionObject function) {
        return getFunctionData(function).isAsync();
    }

    public static Object getLexicalThis(JSFunctionObject thisObj) {
        return getClassPrototypeInitialized(thisObj);
    }

    public static Object getClassPrototypeInitialized(JSFunctionObject thisObj) {
        Object classPrototype = getClassPrototypeField(thisObj);
        assert classPrototype != CLASS_PROTOTYPE_PLACEHOLDER;
        return classPrototype;
    }

    public static Object getClassPrototype(JSFunctionObject thisObj) {
        Object classPrototype = getClassPrototypeField(thisObj);
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, classPrototype == CLASS_PROTOTYPE_PLACEHOLDER)) {
            initializeClassPrototype(thisObj);
            classPrototype = getClassPrototypeField(thisObj);
        }
        return classPrototype;
    }

    private static void initializeClassPrototype(JSFunctionObject thisObj) {
        assert !isClassPrototypeInitialized(thisObj);
        setClassPrototypeField(thisObj, createPrototype(thisObj));
    }

    @TruffleBoundary
    private static JSDynamicObject createPrototype(JSFunctionObject constructor) {
        JSFunctionData functionData = getFunctionData(constructor);
        JSRealm realm = getRealm(constructor);
        JSContext context = functionData.getContext();
        if (!functionData.isGenerator()) {
            JSDynamicObject prototype = JSOrdinary.create(context, realm);
            JSObjectUtil.putConstructorProperty(prototype, constructor);
            return prototype;
        } else {
            assert functionData.isGenerator();
            if (functionData.isAsync()) {
                return JSOrdinary.createWithRealm(context, context.getAsyncGeneratorObjectPrototypeFactory(), realm);
            } else {
                return JSOrdinary.createWithRealm(context, context.getGeneratorObjectPrototypeFactory(), realm);
            }
        }
    }

    public static void setClassPrototype(JSFunctionObject thisObj, Object value) {
        assert value != null;
        setClassPrototypeField(thisObj, value);
    }

    public static final class ClassPrototypeProxyProperty extends PropertyProxy {
        private ClassPrototypeProxyProperty() {
        }

        @Override
        public boolean set(JSDynamicObject store, Object value) {
            JSFunction.setClassPrototype((JSFunctionObject) store, value);
            return true;
        }

        @Override
        public Object get(JSDynamicObject store) {
            return JSFunction.getClassPrototype((JSFunctionObject) store);
        }
    }

    static class BoundRootNode extends JavaScriptRootNode {
        private static final SourceSection SOURCE_SECTION = createBuiltinSourceSection("bound function");

        @Child protected IndirectCallNode callNode;
        protected final BranchProfile initProfile = BranchProfile.create();
        protected final ConditionProfile jsFunctionProfile = ConditionProfile.create();

        BoundRootNode(JSContext context) {
            super(context.getLanguage(), SOURCE_SECTION, null);
            this.callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] originalArguments = frame.getArguments();
            JSFunctionObject.Bound boundFunction = (JSFunctionObject.Bound) JSArguments.getFunctionObject(originalArguments);
            Object boundTargetFunction = boundFunction.getBoundTargetFunction();
            Object[] boundArguments = boundFunction.getBoundArguments();
            Object boundThis = boundFunction.getBoundThis();
            Object[] argumentValues = JSArguments.extractUserArguments(originalArguments);
            Object[] arguments = prependBoundArguments(boundArguments, argumentValues);
            if (jsFunctionProfile.profile(JSFunction.isJSFunction(boundTargetFunction))) {
                Object[] newArguments = JSArguments.create(boundThis, boundTargetFunction, arguments);
                return callNode.call(JSFunction.getFunctionData((JSFunctionObject) boundTargetFunction).getCallTarget(initProfile), newArguments);
            } else {
                return JSRuntime.call(boundTargetFunction, boundThis, arguments);
            }
        }

        protected static Object[] prependBoundArguments(Object[] boundArguments, Object[] argumentValues) {
            Object[] arguments = new Object[boundArguments.length + argumentValues.length];
            System.arraycopy(boundArguments, 0, arguments, 0, boundArguments.length);
            System.arraycopy(argumentValues, 0, arguments, boundArguments.length, argumentValues.length);
            return arguments;
        }
    }

    static final class BoundConstructRootNode extends BoundRootNode {
        BoundConstructRootNode(JSContext context) {
            super(context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] originalArguments = frame.getArguments();
            JSFunctionObject.Bound boundFunction = (JSFunctionObject.Bound) JSArguments.getFunctionObject(originalArguments);
            Object boundTargetFunction = boundFunction.getBoundTargetFunction();
            Object[] boundArguments = boundFunction.getBoundArguments();
            Object[] argumentValues = JSArguments.extractUserArguments(originalArguments);
            Object[] arguments = prependBoundArguments(boundArguments, argumentValues);
            Object originalThis = JSArguments.getThisObject(originalArguments);
            if (jsFunctionProfile.profile(JSFunction.isJSFunction(boundTargetFunction))) {
                Object[] newArguments = JSArguments.create(originalThis, boundTargetFunction, arguments);
                return callNode.call(JSFunction.getFunctionData((JSFunctionObject) boundTargetFunction).getConstructTarget(initProfile), newArguments);
            } else {
                return JSRuntime.construct(boundTargetFunction, arguments);
            }
        }
    }

    static final class BoundConstructNewTargetRootNode extends BoundRootNode {
        BoundConstructNewTargetRootNode(JSContext context) {
            super(context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] originalArguments = frame.getArguments();
            JSFunctionObject.Bound boundFunction = (JSFunctionObject.Bound) JSArguments.getFunctionObject(originalArguments);
            Object boundTargetFunction = boundFunction.getBoundTargetFunction();
            Object[] boundArguments = boundFunction.getBoundArguments();
            Object[] argumentValues = JSArguments.extractUserArguments(originalArguments, 1);
            Object[] arguments = prependBoundArguments(boundArguments, argumentValues);
            Object originalThis = JSArguments.getThisObject(originalArguments);
            if (jsFunctionProfile.profile(JSFunction.isJSFunction(boundTargetFunction))) {
                Object newTarget = JSArguments.getNewTarget(originalArguments);
                if (newTarget == boundFunction) {
                    newTarget = boundTargetFunction;
                }
                Object[] newArguments = JSArguments.createWithNewTarget(originalThis, boundTargetFunction, newTarget, arguments);
                return callNode.call(JSFunction.getFunctionData((JSFunctionObject) boundTargetFunction).getConstructNewTarget(initProfile), newArguments);
            } else {
                return JSRuntime.construct(boundTargetFunction, arguments);
            }
        }
    }

    public static RootNode createBoundRootNode(JSContext context, boolean construct, boolean newTarget) {
        if (newTarget) {
            return new BoundConstructNewTargetRootNode(context);
        } else if (construct) {
            return new BoundConstructRootNode(context);
        } else {
            return new BoundRootNode(context);
        }
    }

    public static JSFunctionObject createFunctionPrototype(JSRealm realm, JSDynamicObject objectPrototype) {
        JSContext context = realm.getContext();
        Shape protoShape = JSShape.createPrototypeShape(context, INSTANCE, objectPrototype);
        JSFunctionObject proto = JSFunctionObject.create(protoShape, objectPrototype, createEmptyFunctionData(context), JSFrameUtil.NULL_MATERIALIZED_FRAME, realm, CLASS_PROTOTYPE_PLACEHOLDER);
        JSObjectUtil.setOrVerifyPrototype(context, proto, objectPrototype);
        JSObjectUtil.putDataProperty(proto, LENGTH, 0, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(proto, NAME, Strings.EMPTY_STRING, JSAttributes.configurableNotEnumerableNotWritable());
        return proto;
    }

    public static void addRestrictedFunctionProperties(JSRealm realm, JSDynamicObject obj) {
        JSObjectUtil.putBuiltinAccessorProperty(obj, CALLER, realm.getThrowerAccessor());
        JSObjectUtil.putBuiltinAccessorProperty(obj, ARGUMENTS, realm.getThrowerAccessor());
    }

    public static JSFunctionData createNamedEmptyFunctionData(JSContext context, TruffleString name) {
        return context.getNamedEmptyFunctionData(name);
    }

    public static JSFunctionData createEmptyFunctionData(JSContext context) {
        return createNamedEmptyFunctionData(context, Strings.EMPTY_STRING);
    }

    public static JSFunctionObject createNamedEmptyFunction(JSRealm realm, TruffleString name) {
        return JSFunction.create(realm, createNamedEmptyFunctionData(realm.getContext(), name));
    }

    public static JSFunctionObject createEmptyFunction(JSRealm realm) {
        return JSFunction.create(realm, createEmptyFunctionData(realm.getContext()));
    }

    public static void fillFunctionPrototype(JSRealm realm) {
        JSContext ctx = realm.getContext();
        JSObjectUtil.putConstructorProperty(realm.getFunctionPrototype(), realm.getFunctionConstructor());
        JSObjectUtil.putFunctionsFromContainer(realm, realm.getFunctionPrototype(), FunctionPrototypeBuiltins.BUILTINS);
        if (ctx.getEcmaScriptVersion() >= 6) {
            addRestrictedFunctionProperties(realm, realm.getFunctionPrototype());
        }
        if (ctx.isOptionNashornCompatibilityMode()) {
            JSObjectUtil.putFunctionsFromContainer(realm, realm.getFunctionPrototype(), FunctionPrototypeBuiltins.BUILTINS_NASHORN_COMPAT);
        }
    }

    public static Shape makeFunctionShape(JSContext context, JSDynamicObject prototype, boolean isGenerator, boolean isAsync) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        if (isGenerator) {
            initialShape = Shape.newBuilder(initialShape).addConstantProperty(isAsync ? ASYNC_GENERATOR_FUNCTION_MARKER_ID : GENERATOR_FUNCTION_MARKER_ID, null, 0).build();
        }
        return initialShape;
    }

    public static JSFunctionObject createFunctionConstructor(JSRealm realm) {
        JSFunctionObject functionConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, CLASS_NAME);
        JSObjectUtil.putDataProperty(functionConstructor, JSObject.PROTOTYPE, realm.getFunctionPrototype(), JSAttributes.notConfigurableNotEnumerableNotWritable());
        return functionConstructor;
    }

    @Override
    public boolean hasOnlyShapeProperties(JSDynamicObject obj) {
        return true;
    }

    public static CallTarget getConstructTarget(JSFunctionObject obj) {
        return getFunctionData(obj).getConstructTarget();
    }

    public static CallTarget getConstructNewTarget(JSFunctionObject obj) {
        return getFunctionData(obj).getConstructNewTarget();
    }

    /**
     * Construct token. Passed from the {@code new} node as {@code this} argument to built-in
     * functions to differentiate between a constructor and a normal call (i.e., [[Construct]] and
     * [[Call]] internal methods, see ES5 13.2.1 and 13.2.2). Must not be passed anywhere else.
     */
    public static final JSDynamicObject CONSTRUCT = new Nullish();

    public static boolean isJSFunction(Object obj) {
        return obj instanceof JSFunctionObject;
    }

    // ##### Generator functions

    public static JSObject createGeneratorFunctionPrototype(JSRealm realm, JSDynamicObject constructor) {
        // intrinsic object %Generator%
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getFunctionPrototype());
        JSObjectUtil.putDataProperty(prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(prototype, JSObject.PROTOTYPE, createGeneratorPrototype(realm, prototype), JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(prototype, GENERATOR_FUNCTION_NAME);
        return prototype;
    }

    private static JSObject createGeneratorPrototype(JSRealm realm, JSDynamicObject constructor) {
        // intrinsic object %GeneratorPrototype%
        JSObject generatorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getIteratorPrototype());
        JSObjectUtil.putFunctionsFromContainer(realm, generatorPrototype, GeneratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putDataProperty(generatorPrototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(generatorPrototype, GENERATOR_NAME);
        return generatorPrototype;
    }

    public static JSConstructor createGeneratorFunctionConstructor(JSRealm realm) {
        // intrinsic object %GeneratorFunction%
        JSFunctionObject constructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, GENERATOR_FUNCTION_NAME);
        JSObject.setPrototype(constructor, realm.getFunctionConstructor());
        JSObject prototype = createGeneratorFunctionPrototype(realm, constructor);
        JSObjectUtil.putDataProperty(constructor, JSObject.PROTOTYPE, prototype, JSAttributes.notConfigurableNotEnumerableNotWritable());
        return new JSConstructor(constructor, prototype);
    }

    // ##### Async functions

    public static JSObject createAsyncFunctionPrototype(JSRealm realm, JSDynamicObject constructor) {
        // intrinsic object %AsyncFunctionPrototype%
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getFunctionPrototype());
        JSObjectUtil.putDataProperty(prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(prototype, ASYNC_FUNCTION_NAME);
        return prototype;
    }

    public static JSConstructor createAsyncFunctionConstructor(JSRealm realm) {
        // intrinsic constructor %AsyncFunction%
        JSFunctionObject constructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, ASYNC_FUNCTION_NAME);
        JSObject.setPrototype(constructor, realm.getFunctionConstructor());
        JSObject prototype = createAsyncFunctionPrototype(realm, constructor);
        JSObjectUtil.putDataProperty(constructor, JSObject.PROTOTYPE, prototype, JSAttributes.notConfigurableNotEnumerableNotWritable());
        return new JSConstructor(constructor, prototype);
    }

    /**
     * Creates the %AsyncIteratorPrototype% object (ES2018 11.1.2).
     */
    public static JSObject createAsyncIteratorPrototype(JSRealm realm) {
        JSContext context = realm.getContext();
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.FunctionAsyncIterator, (c) -> {
            return JSFunctionData.createCallOnly(context, new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    return JSFrameUtil.getThisObj(frame);
                }
            }.getCallTarget(), 0, Symbol.SYMBOL_ASYNC_ITERATOR.toFunctionNameString());
        });
        JSFunctionObject asyncIterator = JSFunction.create(realm, functionData);
        JSObjectUtil.putDataProperty(prototype, Symbol.SYMBOL_ASYNC_ITERATOR, asyncIterator, JSAttributes.getDefaultNotEnumerable());
        return prototype;
    }

    /**
     * Creates the %AsyncFromSyncIteratorPrototype% object (ES2018 11.1.3.2).
     */
    public static JSObject createAsyncFromSyncIteratorPrototype(JSRealm realm) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, AsyncFromSyncIteratorPrototypeBuiltins.BUILTINS);
        return prototype;
    }

    public static JSObject createAsyncGeneratorFunctionPrototype(JSRealm realm, JSDynamicObject constructor) {
        // intrinsic object %AsyncGenerator%
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getFunctionPrototype());
        JSObjectUtil.putDataProperty(prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putDataProperty(prototype, JSObject.PROTOTYPE, createAsyncGeneratorPrototype(realm, prototype), JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(prototype, ASYNC_GENERATOR_FUNCTION_NAME);
        return prototype;
    }

    private static JSObject createAsyncGeneratorPrototype(JSRealm realm, JSDynamicObject constructor) {
        // intrinsic object %AsyncGeneratorPrototype%
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getAsyncIteratorPrototype());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, AsyncGeneratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putDataProperty(prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(prototype, ASYNC_GENERATOR_NAME);
        return prototype;
    }

    public static JSConstructor createAsyncGeneratorFunctionConstructor(JSRealm realm) {
        // intrinsic constructor %AsyncGeneratorFunction%
        JSFunctionObject constructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, ASYNC_GENERATOR_FUNCTION_NAME);
        JSObject.setPrototype(constructor, realm.getFunctionConstructor());
        JSObject prototype = createAsyncGeneratorFunctionPrototype(realm, constructor);
        JSObjectUtil.putDataProperty(constructor, JSObject.PROTOTYPE, prototype, JSAttributes.notConfigurableNotEnumerableNotWritable());
        return new JSConstructor(constructor, prototype);
    }

    // ##### Bound functions and enumerate iterator

    public static JSDynamicObject createEnumerateIteratorPrototype(JSRealm realm) {
        JSDynamicObject iteratorPrototype = realm.getIteratorPrototype();
        JSDynamicObject enumerateIteratorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, enumerateIteratorPrototype, EnumerateIteratorPrototypeBuiltins.BUILTINS);
        return enumerateIteratorPrototype;
    }

    public static Shape makeInitialEnumerateIteratorShape(JSContext context, JSDynamicObject enumerateIteratorPrototype) {
        return JSObjectUtil.getProtoChildShape(enumerateIteratorPrototype, JSOrdinary.INSTANCE, context);
    }

    public static JSDynamicObject createForInIteratorPrototype(JSRealm realm) {
        JSDynamicObject iteratorPrototype = realm.getIteratorPrototype();
        JSDynamicObject enumerateIteratorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, enumerateIteratorPrototype, ForInIteratorPrototypeBuiltins.BUILTINS);
        return enumerateIteratorPrototype;
    }

    public static JSDynamicObject createOrdinaryHasInstanceFunction(JSRealm realm) {
        JSContext ctx = realm.getContext();
        return JSFunction.create(realm, ctx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.OrdinaryHasInstance, c -> {
            return JSFunctionData.createCallOnly(c, new InstanceofNode.OrdinaryHasInstanceRootNode(c).getCallTarget(), 1, ORDINARY_HAS_INSTANCE);
        }));
    }

    public static RootNode getFrameRootNode(FrameInstance frameInstance) {
        Node callNode = frameInstance.getCallNode();
        if (callNode != null) {
            return callNode.getRootNode();
        }
        CallTarget callTarget = frameInstance.getCallTarget();
        if (callTarget instanceof RootCallTarget) {
            return ((RootCallTarget) callTarget).getRootNode();
        }
        return null;
    }

    public static SourceSection createBuiltinSourceSection(String name) {
        return Source.newBuilder(JavaScriptLanguage.ID, "", name).internal(true).build().createUnavailableSection();
    }

    public static boolean isBuiltinSourceSection(SourceSection sourceSection) {
        return sourceSection == BUILTIN_SOURCE_SECTION;
    }

    public static boolean isBuiltinThatShouldNotAppearInStackTrace(JSRealm realm, JSDynamicObject function) {
        return function == realm.getApplyFunctionObject() || function == realm.getCallFunctionObject() || function == realm.getReflectApplyFunctionObject() ||
                        function == realm.getReflectConstructFunctionObject();
    }

    /**
     * V8 compatibility mode: retrieves the function's arguments from the stack.
     */
    public static final class ArgumentsProxyProperty extends PropertyProxy {

        private ArgumentsProxyProperty() {
        }

        @Override
        public Object get(JSDynamicObject thiz) {
            JSFunctionObject thisFunction = (JSFunctionObject) thiz;
            assert !getFunctionData(thisFunction).hasStrictFunctionProperties() && getFunctionData(thisFunction).getContext().isOptionV8CompatibilityMode();
            return JSRuntime.toJSNull(createArguments(thisFunction));
        }

        @TruffleBoundary
        private static Object createArguments(JSFunctionObject thisFunction) {
            JSFunctionData thisFunctionData = getFunctionData(thisFunction);
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<>() {
                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    CompilerAsserts.neverPartOfCompilation();
                    RootNode rootNode = getFrameRootNode(frameInstance);
                    if (rootNode instanceof FunctionRootNode) {
                        if (((FunctionRootNode) rootNode).getFunctionData() == thisFunctionData) {
                            Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                            Object function = JSArguments.getFunctionObject(frame.getArguments());
                            if (function == thisFunction) {
                                JSRealm realm = JSRealm.get(null);
                                Object[] userArguments = JSArguments.extractUserArguments(frame.getArguments());
                                return JSArgumentsArray.createNonStrictSlow(realm, userArguments, (JSFunctionObject) function);
                            }
                        }
                    }
                    return null;
                }
            });
        }

    }

    /**
     * V8 compatibility mode: retrieves the function's caller from the stack.
     */
    public static final class CallerProxyProperty extends PropertyProxy {

        private CallerProxyProperty() {
        }

        @Override
        public Object get(JSDynamicObject thiz) {
            JSFunctionObject thisFunction = (JSFunctionObject) thiz;
            assert !getFunctionData(thisFunction).hasStrictFunctionProperties() && getFunctionData(thisFunction).getContext().isOptionV8CompatibilityMode();
            return JSRuntime.toJSNull(findCaller(thisFunction));
        }

        @TruffleBoundary
        private static Object findCaller(JSFunctionObject thisFunction) {
            JSFunctionData thisFunctionData = getFunctionData(thisFunction);
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<>() {
                private boolean seenThisFunction = false;

                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    CompilerAsserts.neverPartOfCompilation();
                    RootNode rootNode = getFrameRootNode(frameInstance);
                    if (rootNode instanceof FunctionRootNode) {
                        if (seenThisFunction) {
                            Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                            Object function = JSArguments.getFunctionObject(frame.getArguments());
                            if (!isJSFunction(function)) {
                                return null;
                            }
                            JSFunctionObject callerFunction = (JSFunctionObject) function;
                            SourceSection ss = rootNode.getSourceSection();
                            if (ss == null) {
                                return null;
                            }
                            if (ss.getSource().isInternal() && !JSFunction.isBuiltinSourceSection(ss)) {
                                return null;
                            }
                            JSFunctionData functionData = JSFunction.getFunctionData(callerFunction);
                            if (JSFunction.isBuiltinSourceSection(ss)) {
                                JSRealm realm = JSRealm.get(null);
                                if (callerFunction == realm.getEvalFunctionObject()) {
                                    return null; // skip eval()
                                }
                                if (isBuiltinThatShouldNotAppearInStackTrace(realm, callerFunction)) {
                                    return null;
                                }
                                if (Strings.startsWith(functionData.getName(), Strings.BRACKET_SYMBOL_DOT)) {
                                    return null;
                                }
                                if (isStrictBuiltin(callerFunction, realm)) {
                                    return Null.instance; // do not go beyond a strict builtin
                                }
                            } else if (functionData.isStrict()) {
                                return Null.instance;
                            }
                            if (!PROGRAM_FUNCTION_NAME.equals(rootNode.getName())) {
                                return callerFunction;
                            }
                        } else {
                            if (((FunctionRootNode) rootNode).getFunctionData() == thisFunctionData) {
                                Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                                Object function = JSArguments.getFunctionObject(frame.getArguments());
                                if (function == thisFunction) {
                                    seenThisFunction = true;
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    // V8 distinguishes strict and sloppy builtins, see mjsunit/function-caller.js
    public static boolean isStrictBuiltin(JSFunctionObject function, JSRealm realm) {
        JSFunctionData functionData = JSFunction.getFunctionData(function);
        PropertyDescriptor desc = JSObject.getOwnProperty(realm.getArrayPrototype(), functionData.getName());
        return desc != null && desc.isDataDescriptor() && desc.getValue() == function;
    }

    public static Source getCallerSource() {
        RootNode callerRootNode = Truffle.getRuntime().iterateFrames(JSFunction::getFrameRootNode, 1);
        if (callerRootNode != null) {
            SourceSection callerSourceSection = callerRootNode.getSourceSection();
            if (callerSourceSection != null && callerSourceSection.isAvailable()) {
                return callerSourceSection.getSource();
            }
        }
        return null;
    }
}
