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
package com.oracle.truffle.js.nodes.access;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.function.FunctionNameHolder;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ObjectLiteralNode extends JavaScriptNode {

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", LiteralExpressionTag.Type.ObjectLiteral.name());
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
        protected JavaScriptNode copyUninitialized() {
            return create(makeMethodNode.getContext(), cloneUninitialized(functionNode));
        }
    }

    public abstract static class ObjectLiteralMemberNode extends JavaScriptBaseNode {

        public static final ObjectLiteralMemberNode[] EMPTY = {};

        protected final boolean isStatic;
        protected final byte attributes;

        public ObjectLiteralMemberNode(boolean isStatic, int attributes) {
            assert attributes == (attributes & JSAttributes.ATTRIBUTES_MASK);
            this.isStatic = isStatic;
            this.attributes = (byte) attributes;
        }

        public abstract void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context);

        public final boolean isStatic() {
            return isStatic;
        }

        protected static Object executeWithObject(JavaScriptNode valueNode, VirtualFrame frame, DynamicObject obj) {
            if (valueNode instanceof MakeMethodNode) {
                return ((MakeMethodNode) valueNode).executeWithObject(frame, obj);
            }
            return valueNode.execute(frame);
        }

        protected abstract ObjectLiteralMemberNode copyUninitialized();

        public static ObjectLiteralMemberNode[] cloneUninitialized(ObjectLiteralMemberNode[] members) {
            ObjectLiteralMemberNode[] copy = members.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] = copy[i].copyUninitialized();
            }
            return copy;
        }
    }

    private abstract static class CachingObjectLiteralMemberNode extends ObjectLiteralMemberNode {
        protected final Object name;
        @CompilationFinal protected CacheEntry cache;
        protected static final CacheEntry GENERIC = new CacheEntry(null, null, null, null, null);

        CachingObjectLiteralMemberNode(Object name, boolean isStatic, int attributes) {
            super(isStatic, attributes);
            assert JSRuntime.isPropertyKey(name);
            this.name = name;
        }

        protected static final class CacheEntry {
            protected final Shape oldShape;
            protected final Shape newShape;
            protected final Property property;
            protected final Assumption newShapeNotObsoleteAssumption;
            protected final CacheEntry next;

            protected CacheEntry(Shape oldShape, Shape newShape, Property property, Assumption newShapeNotObsoleteAssumption, CacheEntry next) {
                this.oldShape = oldShape;
                this.newShape = newShape;
                this.property = property;
                this.newShapeNotObsoleteAssumption = newShapeNotObsoleteAssumption;
                this.next = next;
            }

            protected boolean isValid() {
                return newShapeNotObsoleteAssumption.isValid();
            }

            protected static int getDepth(CacheEntry head) {
                return head == null ? 0 : 1 + getDepth(head.next);
            }

            @Override
            public String toString() {
                CompilerAsserts.neverPartOfCompilation();
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (CacheEntry current = this; current != null; current = current.next) {
                    sb.append(++count + " [property=" + current.property + ", oldShape=" + current.oldShape + ", newShape=" + current.newShape + "]\n");
                }
                return sb.toString();
            }
        }

        protected final void insertIntoCache(Shape oldShape, Shape newShape, Property property, Assumption newShapeNotObsoleteAssumption) {
            CompilerAsserts.neverPartOfCompilation();
            this.cache = new CacheEntry(oldShape, newShape, property, newShapeNotObsoleteAssumption, filterValid(cache));
        }

        private static CacheEntry filterValid(CacheEntry cache) {
            if (cache == null) {
                return null;
            }
            CacheEntry filteredNext = filterValid(cache.next);
            if (cache.isValid()) {
                if (filteredNext == cache.next) {
                    return cache;
                } else {
                    return new CacheEntry(cache.oldShape, cache.newShape, cache.property, cache.newShapeNotObsoleteAssumption, filteredNext);
                }
            } else {
                return filteredNext;
            }
        }

        protected final void setGeneric() {
            CompilerAsserts.neverPartOfCompilation();
            this.cache = GENERIC;
        }

        protected final int getCacheDepth() {
            return CacheEntry.getDepth(cache);
        }
    }

    private static class ObjectLiteralDataMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralDataMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(name, isStatic, attributes);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object value = executeWithObject(valueNode, frame, obj);
            execute(obj, value, context);
        }

        @ExplodeLoop(kind = LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
        private void execute(DynamicObject obj, Object value, JSContext context) {
            for (CacheEntry resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved == GENERIC) {
                    executeGeneric(obj, value, context);
                    return;
                }
                if (resolved.oldShape.check(obj)) {
                    try {
                        resolved.newShapeNotObsoleteAssumption.check();
                    } catch (InvalidAssumptionException e) {
                        break;
                    }
                    if (resolved.property.getLocation().canStore(value)) {
                        resolved.property.setSafe(obj, value, resolved.oldShape, resolved.newShape);
                        return;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(obj, value, context);
        }

        private void executeGeneric(DynamicObject obj, Object value, JSContext context) {
            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSRuntime.definePropertyOrThrow(obj, name, propDesc, context);
        }

        private void rewrite(DynamicObject obj, Object value, JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            if (getCacheDepth() >= JSTruffleOptions.PropertyCacheLimit) {
                setGeneric();
                executeGeneric(obj, value, context);
                return;
            }
            Shape oldShape = obj.getShape();
            Property property = oldShape.getProperty(name);
            Shape newShape;
            Property newProperty;
            if (property != null) {
                if (JSProperty.isData(property) && !JSProperty.isProxy(property)) {
                    assert JSProperty.isWritable(property);
                    property.setGeneric(obj, value, null);
                } else {
                    JSObjectUtil.defineDataProperty(obj, name, value, attributes);
                }
                newShape = obj.getShape();
                newProperty = newShape.getProperty(name);
            } else {
                JSObjectUtil.putDataProperty(context, obj, name, value, attributes);
                newShape = obj.getShape();
                newProperty = newShape.getLastProperty();
            }
            insertIntoCache(oldShape, newShape, newProperty, newShape.getValidAssumption());
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            return new ObjectLiteralDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode));
        }
    }

    private static class ObjectLiteralAccessorMemberNode extends CachingObjectLiteralMemberNode {
        @Child protected JavaScriptNode getterNode;
        @Child protected JavaScriptNode setterNode;

        ObjectLiteralAccessorMemberNode(Object name, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(name, isStatic, attributes);
            this.getterNode = getter;
            this.setterNode = setter;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object getterV = null;
            Object setterV = null;
            if (getterNode != null) {
                getterV = executeWithObject(getterNode, frame, obj);
            }
            if (setterNode != null) {
                setterV = executeWithObject(setterNode, frame, obj);
            }
            assert getterV != null || setterV != null;
            execute(obj, getterV, setterV, context);
        }

        @ExplodeLoop(kind = LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
        private void execute(DynamicObject obj, Object getterV, Object setterV, JSContext context) {
            for (CacheEntry resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved == GENERIC) {
                    executeGeneric(obj, getterV, setterV, context);
                    return;
                }
                if (resolved.oldShape.check(obj)) {
                    try {
                        resolved.newShapeNotObsoleteAssumption.check();
                    } catch (InvalidAssumptionException e) {
                        break;
                    }
                    Accessor accessor = new Accessor((DynamicObject) getterV, (DynamicObject) setterV);
                    if (resolved.property.getLocation().canStore(accessor)) {
                        resolved.property.setSafe(obj, accessor, resolved.oldShape, resolved.newShape);
                        return;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(obj, getterV, setterV, context);
        }

        private void executeGeneric(DynamicObject obj, Object getterV, Object setterV, JSContext context) {
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getterV, (DynamicObject) setterV, attributes);
            JSRuntime.definePropertyOrThrow(obj, name, propDesc, context);
        }

        private void rewrite(DynamicObject obj, Object getterV, Object setterV, JSContext context) {
            CompilerAsserts.neverPartOfCompilation();
            if (getCacheDepth() >= JSTruffleOptions.PropertyCacheLimit) {
                setGeneric();
                executeGeneric(obj, getterV, setterV, context);
                return;
            }
            Shape oldShape = obj.getShape();
            Property property = oldShape.getProperty(name);
            Accessor value = new Accessor((DynamicObject) getterV, (DynamicObject) setterV);
            Property newProperty;
            Shape newShape;
            if (property != null) {
                if (JSProperty.isAccessor(property)) {
                    property.setGeneric(obj, value, null);
                } else {
                    JSObjectUtil.defineAccessorProperty(obj, name, value, attributes);
                }
                newShape = obj.getShape();
                newProperty = newShape.getProperty(name);
            } else {
                JSObjectUtil.putAccessorProperty(context, obj, name, value, attributes);
                newShape = obj.getShape();
                newProperty = newShape.getLastProperty();
            }
            insertIntoCache(oldShape, newShape, newProperty, newShape.getValidAssumption());
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            return new ObjectLiteralAccessorMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(getterNode), JavaScriptNode.cloneUninitialized(setterNode));
        }
    }

    private static class ComputedObjectLiteralDataMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode propertyKey;
        @Child private JavaScriptNode valueNode;
        @Child private SetFunctionNameNode setFunctionName;

        ComputedObjectLiteralDataMemberNode(JavaScriptNode key, boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(isStatic, attributes);
            this.propertyKey = JSToPropertyKeyWrapperNode.create(key);
            this.valueNode = valueNode;
            this.setFunctionName = isAnonymousFunctionDefinition(valueNode) ? SetFunctionNameNode.create() : null;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object key = propertyKey.execute(frame);
            Object value = executeWithObject(valueNode, frame, obj);
            if (setFunctionName != null) {
                setFunctionName.execute(value, key);
            }

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSRuntime.definePropertyOrThrow(obj, key, propDesc, context);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            ComputedObjectLiteralDataMemberNode copy = (ComputedObjectLiteralDataMemberNode) copy();
            copy.propertyKey = JavaScriptNode.cloneUninitialized(propertyKey);
            copy.valueNode = JavaScriptNode.cloneUninitialized(valueNode);
            copy.setFunctionName = setFunctionName == null ? null : SetFunctionNameNode.create();
            return copy;
        }
    }

    private static class ComputedObjectLiteralAccessorMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode propertyKey;
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;
        @Child private SetFunctionNameNode setFunctionName;
        private final boolean isGetterAnonymousFunction;
        private final boolean isSetterAnonymousFunction;

        ComputedObjectLiteralAccessorMemberNode(JavaScriptNode key, boolean isStatic, int attributes, JavaScriptNode getter, JavaScriptNode setter) {
            super(isStatic, attributes);
            this.propertyKey = JSToPropertyKeyWrapperNode.create(key);
            this.getterNode = getter;
            this.setterNode = setter;
            this.isGetterAnonymousFunction = isAnonymousFunctionDefinition(getter);
            this.isSetterAnonymousFunction = isAnonymousFunctionDefinition(setter);
            this.setFunctionName = (isGetterAnonymousFunction || isSetterAnonymousFunction) ? SetFunctionNameNode.create() : null;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object key = propertyKey.execute(frame);
            Object getterV = null;
            Object setterV = null;
            if (getterNode != null) {
                getterV = executeWithObject(getterNode, frame, obj);
                if (isGetterAnonymousFunction) {
                    setFunctionName.execute(getterV, key, "get");
                }
            }
            if (setterNode != null) {
                setterV = executeWithObject(setterNode, frame, obj);
                if (isSetterAnonymousFunction) {
                    setFunctionName.execute(setterV, key, "set");
                }
            }

            assert getterV != null || setterV != null;
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getterV, (DynamicObject) setterV, attributes);
            JSRuntime.definePropertyOrThrow(obj, key, propDesc, context);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            ComputedObjectLiteralAccessorMemberNode copy = (ComputedObjectLiteralAccessorMemberNode) copy();
            copy.propertyKey = JavaScriptNode.cloneUninitialized(propertyKey);
            copy.getterNode = JavaScriptNode.cloneUninitialized(getterNode);
            copy.setterNode = JavaScriptNode.cloneUninitialized(setterNode);
            copy.setFunctionName = setFunctionName == null ? null : SetFunctionNameNode.create();
            return copy;
        }
    }

    static boolean isAnonymousFunctionDefinition(JavaScriptNode getter) {
        return getter instanceof FunctionNameHolder && ((FunctionNameHolder) getter).isAnonymous();
    }

    private static class ObjectLiteralProtoMemberNode extends ObjectLiteralMemberNode {
        @Child protected JavaScriptNode valueNode;

        ObjectLiteralProtoMemberNode(boolean isStatic, JavaScriptNode valueNode) {
            super(isStatic, 0);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object value = valueNode.execute(frame);
            if (JSObject.isDynamicObject(value)) {
                if (value == Undefined.instance) {
                    return;
                }
                JSObject.setPrototype(obj, (DynamicObject) value);
            }
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            return new ObjectLiteralProtoMemberNode(isStatic, JavaScriptNode.cloneUninitialized(valueNode));
        }
    }

    private static class ObjectLiteralSpreadMemberNode extends ObjectLiteralMemberNode {
        @Child private JavaScriptNode valueNode;
        @Child private JSToObjectNode toObjectNode;

        ObjectLiteralSpreadMemberNode(boolean isStatic, int attributes, JavaScriptNode valueNode) {
            super(isStatic, attributes);
            this.valueNode = valueNode;
        }

        @Override
        public final void executeVoid(VirtualFrame frame, DynamicObject target, JSContext context) {
            Object sourceValue = valueNode.execute(frame);
            if (JSGuards.isNullOrUndefined(sourceValue)) {
                return;
            }
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObjectNoCheckNoForeign(context));
            }
            DynamicObject from = (DynamicObject) toObjectNode.executeTruffleObject(sourceValue);
            copyDataProperties(target, from);
        }

        @TruffleBoundary
        private static void copyDataProperties(DynamicObject target, DynamicObject from) {
            Iterable<Object> ownPropertyKeys = JSObject.ownPropertyKeys(from);
            for (Object nextKey : ownPropertyKeys) {
                PropertyDescriptor desc = JSObject.getOwnProperty(from, nextKey);
                if (desc != null && desc.getEnumerable()) {
                    Object propValue = JSObject.get(from, nextKey);
                    JSRuntime.createDataProperty(target, nextKey, propValue);
                }
            }
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            return new ObjectLiteralSpreadMemberNode(isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode));
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
        public final void executeVoid(VirtualFrame frame, DynamicObject obj, JSContext context) {
            Object value = executeWithObject(valueNode, frame, obj);
            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSObject.defineOwnProperty(obj, name, propDesc, true);
        }

        @Override
        protected ObjectLiteralMemberNode copyUninitialized() {
            return new DictionaryObjectDataMemberNode(name, isStatic, attributes, JavaScriptNode.cloneUninitialized(valueNode));
        }
    }

    public static ObjectLiteralMemberNode newDataMember(String name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode) {
        return new ObjectLiteralDataMemberNode(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode);
    }

    public static ObjectLiteralMemberNode newAccessorMember(String name, boolean isStatic, boolean enumerable, JavaScriptNode getterNode, JavaScriptNode setterNode) {
        return new ObjectLiteralAccessorMemberNode(name, isStatic, JSAttributes.fromConfigurableEnumerable(true, enumerable), getterNode, setterNode);
    }

    public static ObjectLiteralMemberNode newComputedDataMember(JavaScriptNode name, boolean isStatic, boolean enumerable, JavaScriptNode valueNode) {
        return new ComputedObjectLiteralDataMemberNode(name, isStatic, enumerable ? JSAttributes.getDefault() : JSAttributes.getDefaultNotEnumerable(), valueNode);
    }

    public static ObjectLiteralMemberNode newComputedAccessorMember(JavaScriptNode name, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return new ComputedObjectLiteralAccessorMemberNode(name, isStatic, JSAttributes.fromConfigurableEnumerable(true, enumerable), getter, setter);
    }

    public static ObjectLiteralMemberNode newDataMember(Object name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
        return new ObjectLiteralDataMemberNode(name, isStatic, attributes, valueNode);
    }

    public static ObjectLiteralMemberNode newAccessorMember(String name, boolean isStatic, int attributes, JavaScriptNode getterNode, JavaScriptNode setterNode) {
        return new ObjectLiteralAccessorMemberNode(name, isStatic, attributes, getterNode, setterNode);
    }

    public static ObjectLiteralMemberNode newComputedDataMember(JavaScriptNode name, boolean isStatic, int attributes, JavaScriptNode valueNode) {
        return new ComputedObjectLiteralDataMemberNode(name, isStatic, attributes, valueNode);
    }

    public static ObjectLiteralMemberNode newProtoMember(String name, boolean isStatic, JavaScriptNode valueNode) {
        assert JSObject.PROTO.equals(name);
        return new ObjectLiteralProtoMemberNode(isStatic, valueNode);
    }

    public static ObjectLiteralMemberNode newSpreadObjectMember(boolean isStatic, JavaScriptNode valueNode) {
        return new ObjectLiteralSpreadMemberNode(isStatic, JSAttributes.getDefault(), valueNode);
    }

    @Children private final ObjectLiteralMemberNode[] members;
    @Child private CreateObjectNode objectCreateNode;

    private static final int DICTIONARY_OBJECT_THRESHOLD = 400;

    public ObjectLiteralNode(ObjectLiteralMemberNode[] members, CreateObjectNode objectCreateNode) {
        this.members = members;
        this.objectCreateNode = objectCreateNode;
    }

    public static ObjectLiteralNode create(JSContext context, ObjectLiteralMemberNode[] members) {
        if (members.length > 0 && members[0] instanceof ObjectLiteralProtoMemberNode) {
            return new ObjectLiteralNode(Arrays.copyOfRange(members, 1, members.length),
                            CreateObjectNode.createWithCachedPrototype(context, ((ObjectLiteralProtoMemberNode) members[0]).valueNode));
        } else if (JSTruffleOptions.DictionaryObject && members.length > DICTIONARY_OBJECT_THRESHOLD && onlyDataMembers(members)) {
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
        DynamicObject ret = objectCreateNode.executeDynamicObject(frame);
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
    protected JavaScriptNode copyUninitialized() {
        return new ObjectLiteralNode(ObjectLiteralMemberNode.cloneUninitialized(members), objectCreateNode.copyUninitialized());
    }
}
