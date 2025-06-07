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
package com.oracle.truffle.js.nodes.access;

import java.util.Arrays;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.ClassDefinitionNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.FunctionNameHolder;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.nodes.function.NamedEvaluationTargetNode;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ObjectLiteralNode extends JavaScriptNode {

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.ObjectLiteral.name());
    }

    protected static Object executeWithRealm(JavaScriptNode valueNode, VirtualFrame frame, JSRealm realm) {
        if (valueNode instanceof JSFunctionExpressionNode) {
            return ((JSFunctionExpressionNode) valueNode).executeWithRealm(frame, realm);
        } else {
            return valueNode.execute(frame);
        }
    }

    public static final class MakeMethodNode extends JavaScriptNode implements FunctionNameHolder.Delegate {
        @Child private JavaScriptNode functionNode;
        @Child private PropertySetNode makeMethodNode;

        private MakeMethodNode(JSContext context, JavaScriptNode functionNode) {
            this.functionNode = functionNode;
            this.makeMethodNode = PropertySetNode.createSetHidden(JSFunction.HOME_OBJECT_ID, context);
        }

        private MakeMethodNode(JSContext context, JavaScriptNode functionNode, HiddenKey key) {
            this.functionNode = functionNode;
            this.makeMethodNode = PropertySetNode.createSetHidden(key, context);
        }

        public static JavaScriptNode create(JSContext context, JavaScriptNode functionNode) {
            return new MakeMethodNode(context, functionNode);
        }

        public static JavaScriptNode createWithKey(JSContext context, JavaScriptNode functionNode, HiddenKey key) {
            return new MakeMethodNode(context, functionNode, key);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return functionNode.execute(frame);
        }

        public Object executeWithObject(VirtualFrame frame, JSObject obj, JSRealm realm) {
            Object function = executeWithRealm(functionNode, frame, realm);
            makeMethodNode.setValue(function, obj);
            return function;
        }

        @Override
        public FunctionNameHolder getFunctionNameHolder() {
            return (FunctionNameHolder) functionNode;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return create(makeMethodNode.getContext(), cloneUninitialized(functionNode, materializedTags));
        }
    }

    public abstract static class ObjectLiteralMemberNode extends JavaScriptBaseNode {

        public static final ObjectLiteralMemberNode[] EMPTY = {};

        protected final boolean isStatic;
        protected final byte attributes;
        protected final boolean isFieldOrStaticBlock;
        protected final boolean isAnonymousFunctionDefinition;

        protected ObjectLiteralMemberNode(boolean isStatic, int attributes) {
            this(isStatic, attributes, false, false);
        }

        protected ObjectLiteralMemberNode(boolean isStatic, int attributes, boolean isFieldOrStaticBlock, boolean isAnonymousFunctionDefinition) {
            assert attributes == (attributes & JSAttributes.ATTRIBUTES_MASK);
            this.isStatic = isStatic;
            this.attributes = (byte) attributes;
            this.isFieldOrStaticBlock = isFieldOrStaticBlock;
            this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        }

        public abstract void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm);

        public final void executeVoid(VirtualFrame frame, JSObject obj, JSRealm realm) {
            executeVoid(frame, obj, obj, realm);
        }

        @SuppressWarnings("unused")
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            throw Errors.shouldNotReachHere();
        }

        @SuppressWarnings("unused")
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            throw Errors.shouldNotReachHere();
        }

        public final boolean isStatic() {
            return isStatic;
        }

        public boolean isPrivate() {
            return false;
        }

        public final boolean isFieldOrStaticBlock() {
            return isFieldOrStaticBlock;
        }

        public final boolean isAnonymousFunctionDefinition() {
            return isAnonymousFunctionDefinition;
        }

        static boolean isAnonymousFunctionDefinition(JavaScriptNode expression) {
            return expression instanceof FunctionNameHolder && ((FunctionNameHolder) expression).isAnonymous();
        }

        @Idempotent
        protected static boolean isMethodNode(JavaScriptNode valueNode) {
            return valueNode instanceof MakeMethodNode;
        }

        protected static Object evaluateWithHomeObject(JavaScriptNode valueNode, VirtualFrame frame, JSObject obj, JSRealm realm) {
            if (isMethodNode(valueNode)) {
                return ((MakeMethodNode) valueNode).executeWithObject(frame, obj, realm);
            }
            return executeWithRealm(valueNode, frame, realm);
        }

        protected abstract ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags);

        public static ObjectLiteralMemberNode[] cloneUninitialized(ObjectLiteralMemberNode[] members, Set<Class<? extends Tag>> materializedTags) {
            ObjectLiteralMemberNode[] copy = members.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] = copy[i].copyUninitialized(materializedTags);
            }
            return copy;
        }

        public int getAttributes() {
            return attributes;
        }
    }

    /**
     * Base class for object members that can be used as ES class elements.
     */
    public abstract static class ClassElementNode extends ObjectLiteralMemberNode {

        protected ClassElementNode(boolean isStatic, int attributes, boolean isFieldOrStaticBlock, boolean isAnonymousFunctionDefinition) {
            super(isStatic, attributes, isFieldOrStaticBlock, isAnonymousFunctionDefinition);
        }

        protected ClassElementNode(boolean isStatic, int attributes) {
            super(isStatic, attributes);
        }

        @Override
        public abstract ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators);

        @Override
        public abstract void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement);

        /**
         * Unused in case of class element definition evaluation.
         */
        @Override
        public void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
        }

        protected final void checkNoElementsAssumption(JSObject obj, Object key) {
            Node parent = getParent();
            boolean canHaveNoElementsAssumption;
            if (parent instanceof ObjectLiteralNode objectLit) {
                canHaveNoElementsAssumption = objectLit.objectCreateNode.seenArrayPrototype();
            } else if (parent instanceof ClassDefinitionNode classDef) {
                canHaveNoElementsAssumption = classDef.getCreatePrototypeNode().seenArrayPrototype();
            } else {
                canHaveNoElementsAssumption = true;
            }
            CompilerAsserts.partialEvaluationConstant(canHaveNoElementsAssumption);
            if (!canHaveNoElementsAssumption) {
                /*
                 * The created object is not derived from Array.prototype, so there's no need to
                 * check if the property key is an array index. Since the object creation is part of
                 * the same compilation unit, if this changes, we can assume that the object
                 * creation node will transfer to interpreter before we reach here.
                 */
                assert !JSShape.hasNoElementsAssumption(obj);
                return;
            }
            actuallyCheckNoElementsAssumption(obj, key);
        }

        private void actuallyCheckNoElementsAssumption(JSObject obj, Object key) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, JSShape.hasNoElementsAssumption(obj))) {
                if (key instanceof TruffleString name) {
                    var noPrototypeElementsAssumption = getJSContext().getArrayPrototypeNoElementsAssumption();
                    if (noPrototypeElementsAssumption.isValid() && JSRuntime.isArrayIndexString(name)) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        noPrototypeElementsAssumption.invalidate("DefineOwnProperty on an Array prototype");
                    }
                }
            }
        }
    }

    /**
     * Base class for all private class elements.
     */
    public abstract static class PrivateClassElementNode extends ClassElementNode {

        @Child protected JSWriteFrameSlotNode writePrivateNode;

        protected PrivateClassElementNode(boolean isStatic, boolean isFieldOrStaticBlock, JSWriteFrameSlotNode writePrivateNode) {
            super(isStatic, JSAttributes.getDefaultNotEnumerable(), isFieldOrStaticBlock, false);
            this.writePrivateNode = writePrivateNode;
        }

        @Override
        public final boolean isPrivate() {
            return true;
        }

        public final ScopeFrameNode getPrivateScopeNode() {
            return writePrivateNode.getLevelFrameNode();
        }

        public final int getPrivateMemberSlotIndex() {
            return writePrivateNode.getSlotIndex();
        }

        public abstract int getPrivateBrandSlotIndex();
    }

    private abstract static class CachingObjectLiteralMemberNode extends ClassElementNode {
        protected final Object name;
        @Child private DynamicObjectLibrary dynamicObjectLibrary;

        CachingObjectLiteralMemberNode(Object name, boolean isStatic, int attributes, boolean isFieldOrStaticBlock) {
            super(isStatic, attributes, isFieldOrStaticBlock, false);
            assert this instanceof AutoAccessorDataMemberNode || JSRuntime.isPropertyKey(name) || (name == null && isStatic && isFieldOrStaticBlock) : name;
            this.name = name;
        }

        protected Object evaluateKey(@SuppressWarnings("unused") VirtualFrame frame) {
            return name;
        }

        protected final DynamicObjectLibrary dynamicObjectLibrary() {
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary;
            if (dynamicObjectLib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = getLanguage().getJSContext();
                dynamicObjectLibrary = dynamicObjectLib = insert(JSObjectUtil.createDispatched(name, context.getPropertyCacheLimit()));
                JSObjectUtil.checkForNoSuchPropertyOrMethod(context, name);
            }
            return dynamicObjectLib;
        }
    }

    public static class ComputedAutoAccessorDataMemberNode extends AutoAccessorDataMemberNode {

        @Child private JavaScriptNode keyNode;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        ComputedAutoAccessorDataMemberNode(JavaScriptNode keyNode, boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(Undefined.instance, isStatic, attributes, valueNode);
            this.keyNode = keyNode;
            this.toPropertyKeyNode = JSToPropertyKeyNode.create();
        }

        @Override
        protected Object evaluateKey(VirtualFrame frame) {
            return toPropertyKeyNode.execute(keyNode.execute(frame));
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ComputedAutoAccessorDataMemberNode(keyNode, isStatic, attributes, valueNode);
        }
    }

    public static class AutoAccessorDataMemberNode extends ObjectLiteralDataMemberNode {

        private static final String ACCESSOR_STORAGE = " accessor storage";
        private static final HiddenKey STORAGE_KEY_MAGIC = new HiddenKey(":storage-key-magic");

        @Child private PropertySetNode backingStorageMagicSetNode;

        private final JSFunctionData getterFunctionData;
        private final JSFunctionData setterFunctionData;

        AutoAccessorDataMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(name, isStatic, attributes, valueNode, false);
            JSContext context = getLanguage().getJSContext();
            this.setterFunctionData = createAutoAccessorSetFunctionData(context);
            this.getterFunctionData = createAutoAccessorGetFunctionData(context);
            this.backingStorageMagicSetNode = PropertySetNode.createSetHidden(STORAGE_KEY_MAGIC, context);
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = evaluateKey(frame);
            HiddenKey backingStorageKey = createBackingStorageKey(key);
            JSFunctionObject setter = createAutoAccessorSetter(backingStorageKey, realm);
            JSFunctionObject getter = createAutoAccessorGetter(backingStorageKey, realm);
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            return ClassElementDefinitionRecord.createPublicAutoAccessor(key, backingStorageKey, value, getter, setter, isAnonymousFunctionDefinition(), decorators);
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            executeWithGetterSetter(homeObject, classElement.getKey(), classElement.getGetter(), classElement.getSetter());
        }

        private static HiddenKey checkAutoAccessorTarget(VirtualFrame frame, PropertyGetNode getMagicNode, DynamicObjectLibrary storageLibrary, Object thiz) {
            Object function = JSFrameUtil.getFunctionObject(frame);
            HiddenKey backingStorageKey = (HiddenKey) getMagicNode.getValue(function);
            if (!(thiz instanceof JSObject) || !storageLibrary.containsKey((JSObject) thiz, backingStorageKey)) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createTypeError("Bad auto-accessor target.");
            }
            return backingStorageKey;
        }

        private static JSFunctionData createAutoAccessorSetFunctionData(JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Child private PropertyGetNode getStorageKeyNode = PropertyGetNode.createGetHidden(STORAGE_KEY_MAGIC, context);
                @Child private DynamicObjectLibrary storageLibrary = DynamicObjectLibrary.getFactory().createDispatched(5);

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    HiddenKey backingStorageKey = checkAutoAccessorTarget(frame, getStorageKeyNode, storageLibrary, thiz);
                    Object[] args = frame.getArguments();
                    Object value = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                    storageLibrary.put((DynamicObject) thiz, backingStorageKey, value);
                    return value;
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
        }

        private static JSFunctionData createAutoAccessorGetFunctionData(JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Child private PropertyGetNode getStorageKeyNode = PropertyGetNode.createGetHidden(STORAGE_KEY_MAGIC, context);
                @Child private DynamicObjectLibrary storageLibrary = DynamicObjectLibrary.getFactory().createDispatched(5);

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    HiddenKey backingStorageKey = checkAutoAccessorTarget(frame, getStorageKeyNode, storageLibrary, thiz);
                    return storageLibrary.getOrDefault((DynamicObject) thiz, backingStorageKey, Undefined.instance);
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
        }

        private void executeWithGetterSetter(JSObject obj, Object key, Object getterV, Object setterV) {
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary();
            checkNoElementsAssumption(obj, key);
            Accessor accessor = new Accessor(getterV, setterV);
            dynamicObjectLib.putWithFlags(obj, key, accessor, attributes | JSProperty.ACCESSOR);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new AutoAccessorDataMemberNode(name, isStatic, attributes, valueNode);
        }

        public JSFunctionObject createAutoAccessorSetter(HiddenKey backingStorageKey, JSRealm realm) {
            JSFunctionObject functionObject = JSFunction.create(realm, setterFunctionData);
            backingStorageMagicSetNode.setValue(functionObject, backingStorageKey);
            return functionObject;
        }

        public JSFunctionObject createAutoAccessorGetter(HiddenKey backingStorageKey, JSRealm realm) {
            JSFunctionObject functionObject = JSFunction.create(realm, getterFunctionData);
            backingStorageMagicSetNode.setValue(functionObject, backingStorageKey);
            return functionObject;
        }

        @TruffleBoundary
        public HiddenKey createBackingStorageKey(Object key) {
            return new HiddenKey(JSRuntime.safeToString(key) + ACCESSOR_STORAGE);
        }
    }

    private static class ObjectLiteralDataMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralDataMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode, boolean isFieldOrStaticBlock) {
            super(name, isStatic, attributes, isFieldOrStaticBlock);
            this.valueNode = valueNode;
        }

        @Override
        public void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            execute(receiver, name, value);
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = evaluateKey(frame);
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            if (isFieldOrStaticBlock) {
                return ClassElementDefinitionRecord.createPublicField(key, value, isAnonymousFunctionDefinition(), decorators);
            } else {
                return ClassElementDefinitionRecord.createPublicMethod(key, value, isAnonymousFunctionDefinition(), decorators);
            }
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            execute(homeObject, classElement.getKey(), classElement.getValue());
        }

        private void execute(JSObject obj, Object key, Object value) {
            if (isFieldOrStaticBlock) {
                return;
            }
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary();
            checkNoElementsAssumption(obj, key);
            dynamicObjectLib.putWithFlags(obj, key, value, attributes);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectLiteralDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode, materializedTags), isFieldOrStaticBlock);
        }
    }

    public static class ObjectLiteralAccessorMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode getterNode;
        @Child protected JavaScriptNode setterNode;

        ObjectLiteralAccessorMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(name, isStatic, attributes, false);
            this.getterNode = getter;
            this.setterNode = setter;
        }

        public boolean hasGetter() {
            return getterNode != null;
        }

        public boolean hasSetter() {
            return setterNode != null;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = evaluateKey(frame);
            Object getterV = null;
            Object setterV = null;
            if (hasGetter()) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject, realm);
            }
            if (hasSetter()) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject, realm);
            }
            assert getterV != null || setterV != null;
            if (hasGetter() && hasSetter()) {
                return ClassElementDefinitionRecord.createPublicAccessor(key, getterV, setterV, isAnonymousFunctionDefinition, decorators);
            } else if (hasGetter()) {
                return ClassElementDefinitionRecord.createPublicGetter(key, getterV, isAnonymousFunctionDefinition, decorators);
            } else {
                assert hasSetter();
                return ClassElementDefinitionRecord.createPublicSetter(key, setterV, isAnonymousFunctionDefinition, decorators);
            }
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            execute(homeObject, classElement.getGetter(), classElement.getSetter());
        }

        @Override
        public final void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            Object getterV = null;
            Object setterV = null;
            if (hasGetter()) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject, realm);
            }
            if (hasSetter()) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject, realm);
            }
            assert getterV != null || setterV != null;
            execute(receiver, getterV, setterV);
        }

        private void execute(JSObject obj, Object getterV, Object setterV) {
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary();
            checkNoElementsAssumption(obj, name);

            Object getter = getterV;
            Object setter = setterV;

            if ((getterNode == null || setterNode == null) && JSProperty.isAccessor(dynamicObjectLib.getPropertyFlagsOrDefault(obj, name, 0))) {
                // No full accessor information and there is an accessor property already
                // => merge the new and existing accessor functions
                Accessor existing = (Accessor) dynamicObjectLib.getOrDefault(obj, name, null);
                getter = (getter == null) ? existing.getGetter() : getter;
                setter = (setter == null) ? existing.getSetter() : setter;
            }
            Accessor accessor = new Accessor(getter, setter);

            dynamicObjectLib.putWithFlags(obj, name, accessor, attributes | JSProperty.ACCESSOR);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectLiteralAccessorMemberNode(name, isStatic, attributes,
                            JavaScriptNode.cloneUninitialized(getterNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(setterNode, materializedTags));
        }

    }

    public abstract static class ComputedObjectLiteralDataMemberNode extends ClassElementNode {
        @Child private JavaScriptNode propertyKey;
        @Child protected JavaScriptNode valueNode;
        @Child private JSToPropertyKeyNode toPropertyKey;
        @Child protected SetFunctionNameNode setFunctionName;

        ComputedObjectLiteralDataMemberNode(JavaScriptNode key, boolean isStatic, int attributes, JavaScriptNode valueNode, boolean isField, boolean isAnonymousFunctionDefinition) {
            super(isStatic, attributes, isField, isAnonymousFunctionDefinition);
            this.propertyKey = key;
            this.valueNode = valueNode;
            this.toPropertyKey = JSToPropertyKeyNode.create();
            this.setFunctionName = isAnonymousFunctionDefinition(valueNode) ? SetFunctionNameNode.create() : null;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFieldOrStaticBlock", "!isAnonymousFunctionDefinition", "setFunctionName==null", "!isMethodNode(valueNode)"}, limit = "3")
        public final void doNoFieldNoFunctionDef(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm,
                        @CachedLibrary("receiver") DynamicObjectLibrary dynamicObject) {
            Object key = evaluateKey(frame);
            Object value = valueNode.execute(frame);
            checkNoElementsAssumption(receiver, key);
            dynamicObject.putWithFlags(receiver, key, value, attributes | (JSRuntime.isPrivateSymbol(key) ? JSAttributes.NOT_ENUMERABLE : 0));
        }

        @SuppressWarnings("unused")
        @Specialization
        public final void doGeneric(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            if (isFieldOrStaticBlock) {
                return;
            }
            Object key = evaluateKey(frame);
            Object value;
            JavaScriptNode unwrappedValueNode;
            if (isAnonymousFunctionDefinition && valueNode instanceof NamedEvaluationTargetNode) {
                value = ((NamedEvaluationTargetNode) valueNode).executeWithName(frame, key);
            } else {
                value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
                if (setFunctionName != null) {
                    setFunctionName.execute(value, key);
                }
            }

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSRuntime.definePropertyOrThrow(receiver, key, propDesc);
        }

        private Object evaluateKey(VirtualFrame frame) {
            Object key = propertyKey.execute(frame);
            return toPropertyKey.execute(key);
        }

        private Object evaluateValue(VirtualFrame frame, JSObject homeObject, Object key, JSRealm realm) {
            if (!isFieldOrStaticBlock && !isAnonymousFunctionDefinition && setFunctionName == null && !isMethodNode(valueNode)) {
                return valueNode.execute(frame);
            } else {
                Object value;
                if (isAnonymousFunctionDefinition && valueNode instanceof NamedEvaluationTargetNode) {
                    value = ((NamedEvaluationTargetNode) valueNode).executeWithName(frame, key);
                } else {
                    value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
                    if (setFunctionName != null) {
                        setFunctionName.execute(value, key);
                    }
                }
                return value;
            }
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = evaluateKey(frame);
            Object value = evaluateValue(frame, homeObject, key, realm);
            if (isFieldOrStaticBlock) {
                return ClassElementDefinitionRecord.createPublicField(key, value, isAnonymousFunctionDefinition(), decorators);
            } else {
                return ClassElementDefinitionRecord.createPublicMethod(key, value, isAnonymousFunctionDefinition(), decorators);
            }
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            PropertyDescriptor propDesc = PropertyDescriptor.createData(classElement.getValue(), attributes);
            JSRuntime.definePropertyOrThrow(homeObject, classElement.getKey(), propDesc);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return ObjectLiteralNodeFactory.ComputedObjectLiteralDataMemberNodeGen.create(JavaScriptNode.cloneUninitialized(propertyKey, materializedTags), isStatic, attributes,
                            JavaScriptNode.cloneUninitialized(valueNode, materializedTags), isFieldOrStaticBlock, isAnonymousFunctionDefinition);
        }
    }

    private static class ComputedObjectLiteralAccessorMemberNode extends ClassElementNode {
        @Child private JavaScriptNode propertyKey;
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;
        @Child private JSToPropertyKeyNode toPropertyKey;
        @Child private SetFunctionNameNode setFunctionName;
        private final boolean isGetterAnonymousFunction;
        private final boolean isSetterAnonymousFunction;

        ComputedObjectLiteralAccessorMemberNode(JavaScriptNode key, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(isStatic, attributes);
            this.propertyKey = key;
            this.getterNode = getter;
            this.setterNode = setter;
            this.toPropertyKey = JSToPropertyKeyNode.create();
            this.isGetterAnonymousFunction = isAnonymousFunctionDefinition(getter);
            this.isSetterAnonymousFunction = isAnonymousFunctionDefinition(setter);
            this.setFunctionName = (isGetterAnonymousFunction || isSetterAnonymousFunction) ? SetFunctionNameNode.create() : null;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = evaluateKey(frame);
            Object getterV = null;
            Object setterV = null;
            if (hasGetter()) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject, realm);
                if (isGetterAnonymousFunction) {
                    setFunctionName.execute(getterV, key, Strings.GET);
                }
            }
            if (hasSetter()) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject, realm);
                if (isSetterAnonymousFunction) {
                    setFunctionName.execute(setterV, key, Strings.SET);
                }
            }
            if (hasGetter() && hasSetter()) {
                return ClassElementDefinitionRecord.createPublicAccessor(key, getterV, setterV, isGetterAnonymousFunction || isSetterAnonymousFunction, decorators);
            } else if (hasGetter()) {
                return ClassElementDefinitionRecord.createPublicGetter(key, getterV, isGetterAnonymousFunction, decorators);
            } else {
                assert hasSetter();
                return ClassElementDefinitionRecord.createPublicSetter(key, setterV, isSetterAnonymousFunction, decorators);
            }
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            Object getter = classElement.getGetter();
            Object setter = classElement.getSetter();
            assert (getter != null || setter != null) && !(getter instanceof Accessor || setter instanceof Accessor);
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor(getter, setter, attributes);
            JSRuntime.definePropertyOrThrow(homeObject, classElement.getKey(), propDesc);
        }

        @Override
        public final void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            Object key = evaluateKey(frame);
            Object getterV = null;
            Object setterV = null;
            if (hasGetter()) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject, realm);
                if (isGetterAnonymousFunction) {
                    setFunctionName.execute(getterV, key, Strings.GET);
                }
            }
            if (hasSetter()) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject, realm);
                if (isSetterAnonymousFunction) {
                    setFunctionName.execute(setterV, key, Strings.SET);
                }
            }

            assert getterV != null || setterV != null;
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor(getterV, setterV, attributes);
            JSRuntime.definePropertyOrThrow(receiver, key, propDesc);
        }

        private Object evaluateKey(VirtualFrame frame) {
            Object key = propertyKey.execute(frame);
            return toPropertyKey.execute(key);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ComputedObjectLiteralAccessorMemberNode(JavaScriptNode.cloneUninitialized(propertyKey, materializedTags), isStatic, attributes,
                            JavaScriptNode.cloneUninitialized(getterNode, materializedTags), JavaScriptNode.cloneUninitialized(setterNode, materializedTags));
        }

        public boolean hasGetter() {
            return getterNode != null;
        }

        public boolean hasSetter() {
            return setterNode != null;
        }
    }

    private static class ObjectLiteralProtoMemberNode extends ObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralProtoMemberNode(boolean isStatic, JavaScriptNode valueNode) {
            super(isStatic, 0);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            Object value = valueNode.execute(frame);
            if (JSDynamicObject.isJSDynamicObject(value)) {
                if (value == Undefined.instance) {
                    return;
                }
                JSObject.setPrototype(receiver, (JSDynamicObject) value);
            }
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectLiteralProtoMemberNode(isStatic, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    private static class ObjectLiteralSpreadMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode valueNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private CopyDataPropertiesNode copyDataPropertiesNode;

        ObjectLiteralSpreadMemberNode(boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(isStatic, attributes);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, JSObject receiver, JSObject target, JSRealm realm) {
            Object sourceValue = valueNode.execute(frame);
            if (JSGuards.isNullOrUndefined(sourceValue)) {
                return;
            }
            if (toObjectNode == null || copyDataPropertiesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = getLanguage().getJSContext();
                toObjectNode = insert(JSToObjectNode.create());
                copyDataPropertiesNode = insert(CopyDataPropertiesNode.create(context));
            }
            Object from = toObjectNode.execute(sourceValue);
            copyDataPropertiesNode.execute(target, from);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectLiteralSpreadMemberNode(isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    private static class DictionaryObjectDataMemberNode extends ObjectLiteralMemberNode {
        private final Object name;
        @Child private JavaScriptNode valueNode;

        DictionaryObjectDataMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(isStatic, attributes);
            assert JSRuntime.isPropertyKey(name);
            this.name = name;
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, JSObject receiver, JSObject homeObject, JSRealm realm) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSObject.defineOwnProperty(receiver, name, propDesc, true);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DictionaryObjectDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    private static class PrivateFieldMemberNode extends PrivateClassElementNode {
        @Child private JavaScriptNode keyNode;
        @Child private JavaScriptNode valueNode;

        PrivateFieldMemberNode(JavaScriptNode key, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
            super(isStatic, true, writePrivateNode);
            this.keyNode = key;
            this.valueNode = valueNode;
            this.writePrivateNode = writePrivateNode;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            writePrivateNode.execute(frame);
            Object key = keyNode.execute(frame);
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            return ClassElementDefinitionRecord.createPrivateField(key, value, decorators);
        }

        /**
         * Nothing to do: private frame slot has already been assigned and actual field value
         * initialization will be performed by {@link InitializeInstanceElementsNode}.
         */
        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
        }

        @Override
        public int getPrivateBrandSlotIndex() {
            return -1;
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateFieldMemberNode(JavaScriptNode.cloneUninitialized(keyNode, materializedTags), isStatic, JavaScriptNode.cloneUninitialized(valueNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags));
        }
    }

    public static class PrivateMethodMemberNode extends PrivateClassElementNode {
        @Child private JavaScriptNode valueNode;

        private final TruffleString privateName;
        private final int privateBrandSlotIndex;

        PrivateMethodMemberNode(TruffleString privateName, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode, int privateBrandSlotIndex) {
            super(isStatic, false, writePrivateNode);
            this.privateName = privateName;
            this.valueNode = valueNode;
            this.writePrivateNode = writePrivateNode;
            this.privateBrandSlotIndex = privateBrandSlotIndex;
        }

        @Override
        public int getPrivateBrandSlotIndex() {
            return privateBrandSlotIndex;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            return ClassElementDefinitionRecord.createPrivateMethod(privateName, value, decorators);
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            writePrivateNode.executeWrite(frame, classElement.getValue());
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateMethodMemberNode(privateName, isStatic, JavaScriptNode.cloneUninitialized(valueNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags), privateBrandSlotIndex);
        }
    }

    public static class PrivateAccessorMemberNode extends PrivateClassElementNode {
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;

        private final int privateBrandSlotIndex;

        PrivateAccessorMemberNode(boolean isStatic, JavaScriptNode getterNode, JavaScriptNode setterNode, JSWriteFrameSlotNode writePrivateNode, int privateBrandSlotIndex) {
            super(isStatic, false, writePrivateNode);
            this.getterNode = getterNode;
            this.setterNode = setterNode;
            this.writePrivateNode = writePrivateNode;
            this.privateBrandSlotIndex = privateBrandSlotIndex;
        }

        @Override
        public int getPrivateBrandSlotIndex() {
            return privateBrandSlotIndex;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object key = writePrivateNode.getIdentifier();
            Object getter = null;
            Object setter = null;
            if (hasGetter()) {
                getter = evaluateWithHomeObject(getterNode, frame, homeObject, realm);
            }
            if (hasSetter()) {
                setter = evaluateWithHomeObject(setterNode, frame, homeObject, realm);
            }
            assert getter != null || setter != null;
            if (hasGetter() && hasSetter()) {
                return ClassElementDefinitionRecord.createPrivateAccessor(key, getter, setter, decorators);
            } else if (hasGetter()) {
                return ClassElementDefinitionRecord.createPrivateGetter(key, getter, decorators);
            } else {
                assert hasSetter();
                return ClassElementDefinitionRecord.createPrivateSetter(key, setter, decorators);
            }
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
            Object getter = classElement.getGetter();
            Object setter = classElement.getSetter();
            assert getter != null || setter != null;
            Frame privateFrame = writePrivateNode.getLevelFrameNode().executeFrame(frame);
            int slotIndex = writePrivateNode.getSlotIndex();
            if (privateFrame.isObject(slotIndex)) {
                Object previous = privateFrame.getObject(slotIndex);
                if (previous instanceof Accessor) {
                    getter = getter == null ? ((Accessor) previous).getGetter() : getter;
                    setter = setter == null ? ((Accessor) previous).getSetter() : setter;
                }
            }
            Accessor accessor = new Accessor(getter, setter);
            writePrivateNode.executeWrite(frame, accessor);
        }

        public boolean hasGetter() {
            return getterNode != null;
        }

        public boolean hasSetter() {
            return setterNode != null;
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateAccessorMemberNode(isStatic, JavaScriptNode.cloneUninitialized(getterNode, materializedTags), JavaScriptNode.cloneUninitialized(setterNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags), privateBrandSlotIndex);
        }
    }

    public static class PrivateAutoAccessorMemberNode extends PrivateClassElementNode {
        private static final HiddenKey STORAGE_KEY_MAGIC = new HiddenKey(":storage-key-magic");

        @Child private JavaScriptNode valueNode;
        @Child private JavaScriptNode storageKeyNode;
        @Child private PropertySetNode backingStorageMagicSetNode;

        private final int privateBrandSlotIndex;
        private final JSFunctionData getterFunctionData;
        private final JSFunctionData setterFunctionData;

        PrivateAutoAccessorMemberNode(boolean isStatic, JavaScriptNode valueNode,
                        JSWriteFrameSlotNode writePrivateAccessorNode, JavaScriptNode storageKeyNode, int privateBrandSlot) {
            super(isStatic, false, writePrivateAccessorNode);
            this.valueNode = valueNode;
            this.storageKeyNode = storageKeyNode;
            this.privateBrandSlotIndex = privateBrandSlot;
            JSContext context = getLanguage().getJSContext();
            this.backingStorageMagicSetNode = PropertySetNode.createSetHidden(STORAGE_KEY_MAGIC, context);
            this.setterFunctionData = createAutoAccessorSetFunctionData(context);
            this.getterFunctionData = createAutoAccessorGetFunctionData(context);
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            HiddenKey storageKey = (HiddenKey) storageKeyNode.execute(frame);
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            JSFunctionObject setter = createAutoAccessorSetter(storageKey, realm);
            JSFunctionObject getter = createAutoAccessorGetter(storageKey, realm);
            Accessor accessor = new Accessor(getter, setter);
            writePrivateNode.executeWrite(frame, accessor);
            Object accessorKey = writePrivateNode.getIdentifier();
            return ClassElementDefinitionRecord.createPrivateAutoAccessor(accessorKey, storageKey, value, getter, setter, decorators);
        }

        /**
         * Nothing to do: private accessor frame slot has already been assigned and actual field
         * value initialization will be performed by {@link InitializeInstanceElementsNode}.
         */
        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
        }

        private static HiddenKey checkAutoAccessorTarget(VirtualFrame frame, PropertyGetNode getStorageKeyNode, DynamicObjectLibrary storageLibrary, Object thiz) {
            Object function = JSFrameUtil.getFunctionObject(frame);
            HiddenKey backingStorageKey = (HiddenKey) getStorageKeyNode.getValue(function);
            if (!(thiz instanceof JSObject) || !storageLibrary.containsKey((JSObject) thiz, backingStorageKey)) {
                throw Errors.createTypeError("Bad auto-accessor target.");
            }
            return backingStorageKey;
        }

        private static JSFunctionData createAutoAccessorGetFunctionData(JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Child private PropertyGetNode getStorageKeyNode = PropertyGetNode.createGetHidden(STORAGE_KEY_MAGIC, context);
                @Child private DynamicObjectLibrary storageLibrary = DynamicObjectLibrary.getFactory().createDispatched(5);

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    HiddenKey backingStorageKey = checkAutoAccessorTarget(frame, getStorageKeyNode, storageLibrary, thiz);
                    return storageLibrary.getOrDefault((JSObject) thiz, backingStorageKey, Undefined.instance);
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
        }

        private static JSFunctionData createAutoAccessorSetFunctionData(JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Child private PropertyGetNode getStorageKeyNode = PropertyGetNode.createGetHidden(STORAGE_KEY_MAGIC, context);
                @Child private DynamicObjectLibrary storageLibrary = DynamicObjectLibrary.getFactory().createDispatched(5);

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    HiddenKey backingStorageKey = checkAutoAccessorTarget(frame, getStorageKeyNode, storageLibrary, thiz);
                    Object[] args = frame.getArguments();
                    Object value = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                    storageLibrary.put((JSObject) thiz, backingStorageKey, value);
                    return value;
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
        }

        public JSFunctionObject createAutoAccessorGetter(HiddenKey backingStorageKey, JSRealm realm) {
            JSFunctionObject functionObject = JSFunction.create(realm, getterFunctionData);
            backingStorageMagicSetNode.setValue(functionObject, backingStorageKey);
            return functionObject;
        }

        public JSFunctionObject createAutoAccessorSetter(HiddenKey backingStorageKey, JSRealm realm) {
            JSFunctionObject functionObject = JSFunction.create(realm, setterFunctionData);
            backingStorageMagicSetNode.setValue(functionObject, backingStorageKey);
            return functionObject;
        }

        @Override
        public int getPrivateBrandSlotIndex() {
            return privateBrandSlotIndex;
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateAutoAccessorMemberNode(isStatic,
                            JavaScriptNode.cloneUninitialized(valueNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(storageKeyNode, materializedTags),
                            privateBrandSlotIndex);
        }
    }

    private static class StaticBlockNode extends ClassElementNode {
        @Child protected JavaScriptNode valueNode;

        StaticBlockNode(JavaScriptNode valueNode) {
            super(true, 0, true, false);
            this.valueNode = valueNode;
        }

        @Override
        public ClassElementDefinitionRecord evaluateClassElementDefinition(VirtualFrame frame, JSObject homeObject, JSRealm realm, Object[] decorators) {
            Object initializer = evaluateWithHomeObject(valueNode, frame, homeObject, realm);
            return ClassElementDefinitionRecord.createStaticBlock(initializer);
        }

        @Override
        public void defineClassElement(VirtualFrame frame, JSObject homeObject, ClassElementDefinitionRecord classElement) {
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new StaticBlockNode(JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    public static ObjectLiteralMemberNode newDataMember(TruffleString name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode, boolean isField) {
        return new ObjectLiteralDataMemberNode(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode, isField);
    }

    public static ObjectLiteralMemberNode newAutoAccessor(TruffleString name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode) {
        return new AutoAccessorDataMemberNode(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode);
    }

    public static ObjectLiteralMemberNode newComputedAutoAccessor(JavaScriptNode keyNode, boolean isStatic, boolean enumerable, JavaScriptNode valueNode) {
        return new ComputedAutoAccessorDataMemberNode(keyNode, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode);
    }

    public static ObjectLiteralMemberNode newAccessorMember(TruffleString name, boolean isStatic, boolean enumerable, JavaScriptNode getterNode, JavaScriptNode setterNode) {
        return new ObjectLiteralAccessorMemberNode(name, isStatic, JSAttributes.fromConfigurableEnumerable(true, enumerable), getterNode, setterNode);
    }

    public static ObjectLiteralMemberNode newComputedDataMember(JavaScriptNode name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode, boolean isField,
                    boolean isAnonymousFunctionDefinition) {
        int attributes = enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable();
        return ObjectLiteralNodeFactory.ComputedObjectLiteralDataMemberNodeGen.create(name, isStatic, attributes, valueNode, isField, isAnonymousFunctionDefinition);
    }

    public static ObjectLiteralMemberNode newComputedAccessorMember(JavaScriptNode name, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return new ComputedObjectLiteralAccessorMemberNode(name, isStatic, JSAttributes.fromConfigurableEnumerable(true, enumerable), getter, setter);
    }

    public static ObjectLiteralMemberNode newDataMember(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
        return new ObjectLiteralDataMemberNode(name, isStatic, attributes, valueNode, false);
    }

    public static ObjectLiteralMemberNode newAccessorMember(Object name, boolean isStatic, int attributes, JavaScriptNode getterNode, JavaScriptNode setterNode) {
        return new ObjectLiteralAccessorMemberNode(name, isStatic, attributes, getterNode, setterNode);
    }

    public static ObjectLiteralMemberNode newComputedDataMember(JavaScriptNode name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
        return ObjectLiteralNodeFactory.ComputedObjectLiteralDataMemberNodeGen.create(name, isStatic, attributes, valueNode, false, false);
    }

    public static ObjectLiteralMemberNode newPrivateFieldMember(JavaScriptNode name, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
        return new PrivateFieldMemberNode(name, isStatic, valueNode, writePrivateNode);
    }

    public static ObjectLiteralMemberNode newPrivateMethodMember(TruffleString privateName, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode,
                    int privateBrandSlotIndex) {
        return new PrivateMethodMemberNode(privateName, isStatic, valueNode, writePrivateNode, privateBrandSlotIndex);
    }

    public static ObjectLiteralMemberNode newPrivateAccessorMember(boolean isStatic, JavaScriptNode getterNode, JavaScriptNode setterNode, JSWriteFrameSlotNode writePrivateNode,
                    int privateBrandSlotIndex) {
        return new PrivateAccessorMemberNode(isStatic, getterNode, setterNode, writePrivateNode, privateBrandSlotIndex);
    }

    public static ObjectLiteralMemberNode newPrivateAutoAccessorMember(boolean isStatic, JavaScriptNode valueNode,
                    JSWriteFrameSlotNode writePrivateAccessor, JavaScriptNode storageKey, int privateBrandSlotIndex) {
        return new PrivateAutoAccessorMemberNode(isStatic, valueNode, writePrivateAccessor, storageKey, privateBrandSlotIndex);
    }

    public static ObjectLiteralMemberNode newProtoMember(TruffleString name, boolean isStatic, JavaScriptNode valueNode) {
        assert Strings.equals(JSObject.PROTO, name);
        return new ObjectLiteralProtoMemberNode(isStatic, valueNode);
    }

    public static ObjectLiteralMemberNode newSpreadObjectMember(boolean isStatic, JavaScriptNode valueNode) {
        return new ObjectLiteralSpreadMemberNode(isStatic, JSAttributes.getDefault(), valueNode);
    }

    public static ObjectLiteralMemberNode newStaticBlockMember(JavaScriptNode valueNode) {
        return new StaticBlockNode(valueNode);
    }

    @Children private final ObjectLiteralMemberNode[] members;
    @Child private CreateObjectNode objectCreateNode;
    @Child private JavaScriptNode prototypeExpression;

    private ObjectLiteralNode(ObjectLiteralMemberNode[] members, CreateObjectNode objectCreateNode, JavaScriptNode prototypeExpression) {
        this.members = members;
        this.objectCreateNode = objectCreateNode;
        this.prototypeExpression = prototypeExpression;
    }

    public static ObjectLiteralNode create(JSContext context, ObjectLiteralMemberNode[] members) {
        if (members.length > 0 && members[0] instanceof ObjectLiteralProtoMemberNode protoMember) {
            return new ObjectLiteralNode(Arrays.copyOfRange(members, 1, members.length),
                            CreateObjectNode.createOrdinaryWithPrototype(context), protoMember.valueNode);
        } else if (JSConfig.DictionaryObject && members.length > JSConfig.DictionaryObjectThreshold && onlyDataMembers(members)) {
            return createDictionaryObject(context, members);
        } else {
            return new ObjectLiteralNode(members, CreateObjectNode.create(context), null);
        }
    }

    private static boolean onlyDataMembers(ObjectLiteralMemberNode[] members) {
        for (ObjectLiteralMemberNode member : members) {
            if (!(member instanceof ObjectLiteralDataMemberNode)) {
                return false;
            }
        }
        return true;
    }

    private static ObjectLiteralNode createDictionaryObject(JSContext context, ObjectLiteralMemberNode[] members) {
        ObjectLiteralMemberNode[] newMembers = new ObjectLiteralMemberNode[members.length];
        for (int i = 0; i < members.length; i++) {
            ObjectLiteralDataMemberNode member = (ObjectLiteralDataMemberNode) members[i];
            newMembers[i] = new DictionaryObjectDataMemberNode(member.name, member.isStatic, member.attributes, member.valueNode);
        }
        return new ObjectLiteralNode(newMembers, CreateObjectNode.createDictionary(context), null);
    }

    @Override
    public JSObject execute(VirtualFrame frame) {
        JSRealm realm = getRealm();
        Object proto = prototypeExpression == null ? realm.getObjectPrototype() : prototypeExpression.execute(frame);
        JSObject ret = objectCreateNode.executeWithPrototype(realm, proto);
        return executeWithObject(frame, ret, realm);
    }

    @ExplodeLoop
    private JSObject executeWithObject(VirtualFrame frame, JSObject ret, JSRealm realm) {
        for (int i = 0; i < members.length; i++) {
            members[i].executeVoid(frame, ret, realm);
        }
        return ret;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == JSDynamicObject.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ObjectLiteralNode(ObjectLiteralMemberNode.cloneUninitialized(members, materializedTags), objectCreateNode, cloneUninitialized(prototypeExpression, materializedTags));
    }
}
