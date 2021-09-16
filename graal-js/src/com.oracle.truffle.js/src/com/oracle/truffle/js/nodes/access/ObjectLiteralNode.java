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
package com.oracle.truffle.js.nodes.access;

import java.util.Arrays;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.function.ClassDefinitionNode;
import com.oracle.truffle.js.nodes.function.FunctionNameHolder;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ObjectLiteralNode extends JavaScriptNode {

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

        public Object executeWithObject(VirtualFrame frame, DynamicObject obj) {
            Object function = execute(frame);
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

        public ObjectLiteralMemberNode(boolean isStatic, int attributes) {
            this(isStatic, attributes, false, false);
        }

        public ObjectLiteralMemberNode(boolean isStatic, int attributes, boolean isFieldOrStaticBlock, boolean isAnonymousFunctionDefinition) {
            assert attributes == (attributes & JSAttributes.ATTRIBUTES_MASK);
            this.isStatic = isStatic;
            this.attributes = (byte) attributes;
            this.isFieldOrStaticBlock = isFieldOrStaticBlock;
            this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        }

        public abstract void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context);

        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            executeVoid(frame, obj, obj, context);
        }

        public Object evaluateKey(@SuppressWarnings("unused") VirtualFrame frame) {
            throw Errors.shouldNotReachHere();
        }

        public Object evaluateValue(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") DynamicObject homeObject) {
            throw Errors.shouldNotReachHere();
        }

        public final boolean isStatic() {
            return isStatic;
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

        protected static boolean isMethodNode(JavaScriptNode valueNode) {
            return valueNode instanceof MakeMethodNode;
        }

        protected static Object evaluateWithHomeObject(JavaScriptNode valueNode, VirtualFrame frame, DynamicObject obj) {
            if (isMethodNode(valueNode)) {
                return ((MakeMethodNode) valueNode).executeWithObject(frame, obj);
            }
            return valueNode.execute(frame);
        }

        protected abstract ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags);

        public static ObjectLiteralMemberNode[] cloneUninitialized(ObjectLiteralMemberNode[] members, Set<Class<? extends Tag>> materializedTags) {
            ObjectLiteralMemberNode[] copy = members.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] = copy[i].copyUninitialized(materializedTags);
            }
            return copy;
        }
    }

    private abstract static class CachingObjectLiteralMemberNode extends ObjectLiteralMemberNode {
        protected final Object name;
        @CompilationFinal private DynamicObjectLibrary dynamicObjectLibrary;

        CachingObjectLiteralMemberNode(Object name, boolean isStatic, int attributes, boolean isFieldOrStaticBlock) {
            super(isStatic, attributes, isFieldOrStaticBlock, false);
            assert JSRuntime.isPropertyKey(name) || (name == null && isStatic && isFieldOrStaticBlock) : name;
            this.name = name;
        }

        @Override
        public final Object evaluateKey(VirtualFrame frame) {
            return name;
        }

        protected final DynamicObjectLibrary dynamicObjectLibrary(JSContext context) {
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary;
            if (dynamicObjectLib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dynamicObjectLibrary = dynamicObjectLib = insert(JSObjectUtil.createDispatched(name, context.getPropertyCacheLimit()));
                JSObjectUtil.checkForNoSuchPropertyOrMethod(context, name);
            }
            return dynamicObjectLib;
        }
    }

    private static class ObjectLiteralDataMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralDataMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode, boolean isFieldOrStaticBlock) {
            super(name, isStatic, attributes, isFieldOrStaticBlock);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject);
            execute(receiver, value, context);
        }

        @Override
        public Object evaluateValue(VirtualFrame frame, DynamicObject homeObject) {
            return evaluateWithHomeObject(valueNode, frame, homeObject);
        }

        private void execute(DynamicObject obj, Object value, JSContext context) {
            if (isFieldOrStaticBlock) {
                return;
            }
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary(context);
            dynamicObjectLib.putWithFlags(obj, name, value, attributes);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectLiteralDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode, materializedTags), isFieldOrStaticBlock);
        }
    }

    private static class ObjectLiteralAccessorMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode getterNode;
        @Child protected JavaScriptNode setterNode;

        ObjectLiteralAccessorMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(name, isStatic, attributes, false);
            this.getterNode = getter;
            this.setterNode = setter;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object getterV = null;
            Object setterV = null;
            if (getterNode != null) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject);
            }
            if (setterNode != null) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject);
            }
            assert getterV != null || setterV != null;
            execute(receiver, getterV, setterV, context);
        }

        private void execute(DynamicObject obj, Object getterV, Object setterV, JSContext context) {
            DynamicObjectLibrary dynamicObjectLib = dynamicObjectLibrary(context);

            DynamicObject getter = (DynamicObject) getterV;
            DynamicObject setter = (DynamicObject) setterV;

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

    public abstract static class ComputedObjectLiteralDataMemberNode extends ObjectLiteralMemberNode {
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
        public final void doNoFieldNoFunctionDef(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context,
                        @CachedLibrary("receiver") DynamicObjectLibrary dynamicObject) {
            Object key = evaluateKey(frame);
            Object value = valueNode.execute(frame);
            dynamicObject.put(receiver, key, value);
        }

        @SuppressWarnings("unused")
        @Specialization
        public final void doGeneric(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            if (isFieldOrStaticBlock) {
                return;
            }
            Object key = evaluateKey(frame);
            Object value;
            JavaScriptNode unwrappedValueNode;
            if (isAnonymousFunctionDefinition && (unwrappedValueNode = JSNodeUtil.getWrappedNode(valueNode)) instanceof ClassDefinitionNode) {
                value = ((ClassDefinitionNode) unwrappedValueNode).executeWithClassName(frame, key);
            } else {
                value = evaluateWithHomeObject(valueNode, frame, homeObject);
                if (setFunctionName != null) {
                    setFunctionName.execute(value, key);
                }
            }

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSRuntime.definePropertyOrThrow(receiver, key, propDesc);
        }

        @Override
        public Object evaluateKey(VirtualFrame frame) {
            Object key = propertyKey.execute(frame);
            return toPropertyKey.execute(key);
        }

        @Override
        public Object evaluateValue(VirtualFrame frame, DynamicObject homeObject) {
            return evaluateWithHomeObject(valueNode, frame, homeObject);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return ObjectLiteralNodeFactory.ComputedObjectLiteralDataMemberNodeGen.create(JavaScriptNode.cloneUninitialized(propertyKey, materializedTags), isStatic, attributes,
                            JavaScriptNode.cloneUninitialized(valueNode, materializedTags), isFieldOrStaticBlock, isAnonymousFunctionDefinition);
        }
    }

    private static class ComputedObjectLiteralAccessorMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode propertyKey;
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;
        @Child private JSToPropertyKeyNode toPropertyKey;
        @Child private SetFunctionNameNode setFunctionName;
        private final boolean isGetterAnonymousFunction;
        private final boolean isSetterAnonymousFunction;

        ComputedObjectLiteralAccessorMemberNode(JavaScriptNode key, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(isStatic, attributes);
            this.propertyKey = JSToPropertyKeyWrapperNode.create(key);
            this.getterNode = getter;
            this.setterNode = setter;
            this.toPropertyKey = JSToPropertyKeyNode.create();
            this.isGetterAnonymousFunction = isAnonymousFunctionDefinition(getter);
            this.isSetterAnonymousFunction = isAnonymousFunctionDefinition(setter);
            this.setFunctionName = (isGetterAnonymousFunction || isSetterAnonymousFunction) ? SetFunctionNameNode.create() : null;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object key = evaluateKey(frame);
            Object getterV = null;
            Object setterV = null;
            if (getterNode != null) {
                getterV = evaluateWithHomeObject(getterNode, frame, homeObject);
                if (isGetterAnonymousFunction) {
                    setFunctionName.execute(getterV, key, "get");
                }
            }
            if (setterNode != null) {
                setterV = evaluateWithHomeObject(setterNode, frame, homeObject);
                if (isSetterAnonymousFunction) {
                    setFunctionName.execute(setterV, key, "set");
                }
            }

            assert getterV != null || setterV != null;
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getterV, (DynamicObject) setterV, attributes);
            JSRuntime.definePropertyOrThrow(receiver, key, propDesc);
        }

        @Override
        public Object evaluateKey(VirtualFrame frame) {
            Object key = propertyKey.execute(frame);
            return toPropertyKey.execute(key);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ComputedObjectLiteralAccessorMemberNode(JavaScriptNode.cloneUninitialized(propertyKey, materializedTags), isStatic, attributes,
                            JavaScriptNode.cloneUninitialized(getterNode, materializedTags), JavaScriptNode.cloneUninitialized(setterNode, materializedTags));
        }
    }

    private static class ObjectLiteralProtoMemberNode extends ObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralProtoMemberNode(boolean isStatic, JavaScriptNode valueNode) {
            super(isStatic, 0);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object value = valueNode.execute(frame);
            if (JSDynamicObject.isJSDynamicObject(value)) {
                if (value == Undefined.instance) {
                    return;
                }
                JSObject.setPrototype(receiver, (DynamicObject) value);
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
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject target, JSContext context) {
            Object sourceValue = valueNode.execute(frame);
            if (JSGuards.isNullOrUndefined(sourceValue)) {
                return;
            }
            if (toObjectNode == null || copyDataPropertiesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObjectNoCheck(context));
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
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject);
            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSObject.defineOwnProperty(receiver, name, propDesc, true);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DictionaryObjectDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    private static class PrivateFieldMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode keyNode;
        @Child private JavaScriptNode valueNode;
        @Child private JSWriteFrameSlotNode writePrivateNode;

        PrivateFieldMemberNode(JavaScriptNode key, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
            super(isStatic, JSAttributes.getDefaultNotEnumerable(), true, false);
            this.keyNode = key;
            this.valueNode = valueNode;
            this.writePrivateNode = writePrivateNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            writePrivateNode.execute(frame);
        }

        @Override
        public Object evaluateKey(VirtualFrame frame) {
            return keyNode.execute(frame);
        }

        @Override
        public Object evaluateValue(VirtualFrame frame, DynamicObject homeObject) {
            return evaluateWithHomeObject(valueNode, frame, homeObject);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateFieldMemberNode(JavaScriptNode.cloneUninitialized(keyNode, materializedTags), isStatic, JavaScriptNode.cloneUninitialized(valueNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags));
        }
    }

    private static class PrivateMethodMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode valueNode;
        @Child private JSWriteFrameSlotNode writePrivateNode;

        PrivateMethodMemberNode(boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
            super(isStatic, JSAttributes.getDefaultNotEnumerable(), false, false);
            this.valueNode = valueNode;
            this.writePrivateNode = writePrivateNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object value = evaluateWithHomeObject(valueNode, frame, homeObject);
            writePrivateNode.executeWrite(frame, value);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateMethodMemberNode(isStatic, JavaScriptNode.cloneUninitialized(valueNode, materializedTags), JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags));
        }
    }

    private static class PrivateAccessorMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;
        @Child private JSWriteFrameSlotNode writePrivateNode;

        PrivateAccessorMemberNode(boolean isStatic, JavaScriptNode getterNode, JavaScriptNode setterNode, JSWriteFrameSlotNode writePrivateNode) {
            super(isStatic, JSAttributes.getDefaultNotEnumerable(), false, false);
            this.getterNode = getterNode;
            this.setterNode = setterNode;
            this.writePrivateNode = writePrivateNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject receiver, DynamicObject homeObject, JSContext context) {
            Object getter = null;
            Object setter = null;
            if (getterNode != null) {
                getter = evaluateWithHomeObject(getterNode, frame, homeObject);
            }
            if (setterNode != null) {
                setter = evaluateWithHomeObject(setterNode, frame, homeObject);
            }

            assert getter != null || setter != null;
            Accessor accessor = new Accessor((DynamicObject) getter, (DynamicObject) setter);
            writePrivateNode.executeWrite(frame, accessor);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateAccessorMemberNode(isStatic, JavaScriptNode.cloneUninitialized(getterNode, materializedTags), JavaScriptNode.cloneUninitialized(setterNode, materializedTags),
                            JavaScriptNode.cloneUninitialized(writePrivateNode, materializedTags));
        }
    }

    public static ObjectLiteralMemberNode newDataMember(String name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode, boolean isField) {
        return new ObjectLiteralDataMemberNode(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode, isField);
    }

    public static ObjectLiteralMemberNode newAccessorMember(String name, boolean isStatic, boolean enumerable, JavaScriptNode getterNode, JavaScriptNode setterNode) {
        return new ObjectLiteralAccessorMemberNode(name, isStatic, JSAttributes.fromConfigurableEnumerable(true, enumerable), getterNode, setterNode);
    }

    public static ObjectLiteralMemberNode newComputedDataMember(JavaScriptNode name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode, boolean isField,
                    boolean isAnonymousFunctionDefinition) {
        return ObjectLiteralNodeFactory.ComputedObjectLiteralDataMemberNodeGen.create(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode,
                        isField,
                        isAnonymousFunctionDefinition);
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

    public static ObjectLiteralMemberNode newPrivateMethodMember(boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
        return new PrivateMethodMemberNode(isStatic, valueNode, writePrivateNode);
    }

    public static ObjectLiteralMemberNode newPrivateAccessorMember(boolean isStatic, JavaScriptNode getterNode, JavaScriptNode setterNode, JSWriteFrameSlotNode writePrivateNode) {
        return new PrivateAccessorMemberNode(isStatic, getterNode, setterNode, writePrivateNode);
    }

    public static ObjectLiteralMemberNode newProtoMember(String name, boolean isStatic, JavaScriptNode valueNode) {
        assert JSObject.PROTO.equals(name);
        return new ObjectLiteralProtoMemberNode(isStatic, valueNode);
    }

    public static ObjectLiteralMemberNode newSpreadObjectMember(boolean isStatic, JavaScriptNode valueNode) {
        return new ObjectLiteralSpreadMemberNode(isStatic, JSAttributes.getDefault(), valueNode);
    }

    public static ObjectLiteralMemberNode newStaticBlockMember(JavaScriptNode valueNode) {
        return new ObjectLiteralDataMemberNode(null, true, JSAttributes.getDefaultNotEnumerable(), valueNode, true);
    }

    @Children private final ObjectLiteralMemberNode[] members;
    @Child private CreateObjectNode objectCreateNode;

    public ObjectLiteralNode(ObjectLiteralMemberNode[] members, CreateObjectNode objectCreateNode) {
        this.members = members;
        this.objectCreateNode = objectCreateNode;
    }

    public static ObjectLiteralNode create(JSContext context, ObjectLiteralMemberNode[] members) {
        if (members.length > 0 && members[0] instanceof ObjectLiteralProtoMemberNode) {
            return new ObjectLiteralNode(Arrays.copyOfRange(members, 1, members.length),
                            CreateObjectNode.createOrdinaryWithPrototype(context, ((ObjectLiteralProtoMemberNode) members[0]).valueNode));
        } else if (JSConfig.DictionaryObject && members.length > JSConfig.DictionaryObjectThreshold && onlyDataMembers(members)) {
            return createDictionaryObject(context, members);
        } else {
            return new ObjectLiteralNode(members, CreateObjectNode.create(context));
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
        return new ObjectLiteralNode(newMembers, CreateObjectNode.createDictionary(context));
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        DynamicObject ret = objectCreateNode.execute(frame);
        return executeWithObject(frame, ret);
    }

    @ExplodeLoop
    public DynamicObject executeWithObject(VirtualFrame frame, DynamicObject ret) {
        JSContext context = objectCreateNode.getContext();
        for (int i = 0; i < members.length; i++) {
            members[i].executeVoid(frame, ret, context);
        }
        return ret;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    static CharSequence reasonResolved(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("resolved property %s", key);
        }
        return "resolved property";
    }

    static CharSequence reasonNewShapeAssumptionInvalidated(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("new shape assumption invalidated (property %s)", key);
        }
        return "new shape assumption invalidated";
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ObjectLiteralNode(ObjectLiteralMemberNode.cloneUninitialized(members, materializedTags), objectCreateNode.copyUninitialized(materializedTags));
    }
}
