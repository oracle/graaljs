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

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.IntLocation;
import com.oracle.truffle.api.object.LongLocation;
import com.oracle.truffle.api.object.ObjectLocation;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNodeFactory.GetPropertyFromJSObjectNodeGen;
import com.oracle.truffle.js.nodes.array.ArrayLengthNode.ArrayLengthReadNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSNoSuchMethodAdapter;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultIndicesArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpGroupsObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResultNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexResultAccessor;

/**
 * ES6 9.1.8 [[Get]] (P, Receiver).
 *
 * @see PropertyNode
 * @see GlobalPropertyNode
 */
public class PropertyGetNode extends PropertyCacheNode<PropertyGetNode.GetCacheNode> {
    protected final boolean isGlobal;
    protected final boolean getOwnProperty;
    @CompilationFinal protected boolean isMethod;
    private boolean propertyAssumptionCheckEnabled = true;
    @Child protected GetCacheNode cacheNode;

    public static PropertyGetNode create(Object key, JSContext context) {
        return create(key, false, context);
    }

    public static PropertyGetNode create(Object key, boolean isGlobal, JSContext context) {
        final boolean getOwnProperty = false;
        final boolean isMethod = false;
        return createImpl(key, isGlobal, context, getOwnProperty, isMethod);
    }

    public static PropertyGetNode create(Object key, boolean isGlobal, JSContext context, boolean getOwnProperty, boolean isMethod) {
        return createImpl(key, isGlobal, context, getOwnProperty, isMethod);
    }

    private static PropertyGetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean getOwnProperty, boolean isMethod) {
        return new PropertyGetNode(key, context, isGlobal, getOwnProperty, isMethod);
    }

    public static PropertyGetNode createGetOwn(Object key, JSContext context) {
        final boolean global = false;
        final boolean getOwnProperty = true;
        final boolean isMethod = false;
        return createImpl(key, global, context, getOwnProperty, isMethod);
    }

    public static PropertyGetNode createGetHidden(HiddenKey key, JSContext context) {
        return createGetOwn(key, context);
    }

    protected PropertyGetNode(Object key, JSContext context, boolean isGlobal, boolean getOwnProperty, boolean isMethod) {
        super(key, context);
        this.isGlobal = isGlobal;
        this.getOwnProperty = getOwnProperty;
        this.isMethod = isMethod;
    }

    public final Object getValue(Object obj) {
        return getValueOrDefault(obj, obj, Undefined.instance);
    }

    public final int getValueInt(Object obj) throws UnexpectedResultException {
        return getValueInt(obj, obj);
    }

    public final double getValueDouble(Object obj) throws UnexpectedResultException {
        return getValueDouble(obj, obj);
    }

    public final boolean getValueBoolean(Object obj) throws UnexpectedResultException {
        return getValueBoolean(obj, obj);
    }

    public final long getValueLong(Object obj) throws UnexpectedResultException {
        return getValueLong(obj, obj);
    }

    public final Object getValueOrDefault(Object obj, Object defaultValue) {
        return getValueOrDefault(obj, obj, defaultValue);
    }

    protected Object getValueOrUndefined(Object thisObj, Object receiver) {
        return getValueOrDefault(thisObj, receiver, Undefined.instance);
    }

    @ExplodeLoop
    protected int getValueInt(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = cacheNode;
        if (c instanceof GenericPropertyGetNode) {
            return ((GenericPropertyGetNode) c).getValueInt(thisObj, receiver, this, false);
        }
        for (; c != null; c = c.next) {
            if (c.isConstantObjectSpecialization()) {
                JSDynamicObject expectedObj = c.getExpectedObject();
                if (thisObj != expectedObj) {
                    if (expectedObj == null) {
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (!c.isValid(this)) {
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.getValueInt(thisObj, receiver, this, guard);
            }
        }
        deoptimize(c);
        return getValueIntAndSpecialize(thisObj, receiver);
    }

    @TruffleBoundary
    private int getValueIntAndSpecialize(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = specialize(thisObj);
        return c.getValueInt(thisObj, receiver, this, false);
    }

    @ExplodeLoop
    protected double getValueDouble(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = cacheNode;
        if (c instanceof GenericPropertyGetNode) {
            return ((GenericPropertyGetNode) c).getValueDouble(thisObj, receiver, this, false);
        }
        for (; c != null; c = c.next) {
            if (c.isConstantObjectSpecialization()) {
                JSDynamicObject expectedObj = c.getExpectedObject();
                if (thisObj != expectedObj) {
                    if (expectedObj == null) {
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (!c.isValid(this)) {
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.getValueDouble(thisObj, receiver, this, guard);
            }
        }
        deoptimize(c);
        return getValueDoubleAndSpecialize(thisObj, receiver);
    }

    @TruffleBoundary
    private double getValueDoubleAndSpecialize(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = specialize(thisObj);
        return c.getValueDouble(thisObj, receiver, this, false);
    }

    @ExplodeLoop
    protected boolean getValueBoolean(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = cacheNode;
        if (c instanceof GenericPropertyGetNode) {
            return ((GenericPropertyGetNode) c).getValueBoolean(thisObj, receiver, this, false);
        }
        for (; c != null; c = c.next) {
            if (c.isConstantObjectSpecialization()) {
                JSDynamicObject expectedObj = c.getExpectedObject();
                if (thisObj != expectedObj) {
                    if (expectedObj == null) {
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (!c.isValid(this)) {
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.getValueBoolean(thisObj, receiver, this, guard);
            }
        }
        deoptimize(c);
        return getValueBooleanAndSpecialize(thisObj, receiver);
    }

    @TruffleBoundary
    private boolean getValueBooleanAndSpecialize(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = specialize(thisObj);
        return c.getValueBoolean(thisObj, receiver, this, false);
    }

    @ExplodeLoop
    protected long getValueLong(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = cacheNode;
        if (c instanceof GenericPropertyGetNode) {
            return ((GenericPropertyGetNode) c).getValueLong(thisObj, receiver, this, false);
        }
        for (; c != null; c = c.next) {
            if (c.isConstantObjectSpecialization()) {
                JSDynamicObject expectedObj = c.getExpectedObject();
                if (thisObj != expectedObj) {
                    if (expectedObj == null) {
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (!c.isValid(this)) {
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.getValueLong(thisObj, receiver, this, guard);
            }
        }
        deoptimize(c);
        return getValueLongAndSpecialize(thisObj, receiver);
    }

    @TruffleBoundary
    private long getValueLongAndSpecialize(Object thisObj, Object receiver) throws UnexpectedResultException {
        GetCacheNode c = specialize(thisObj);
        return c.getValueLong(thisObj, receiver, this, false);
    }

    @ExplodeLoop
    protected Object getValueOrDefault(Object thisObj, Object receiver, Object defaultValue) {
        GetCacheNode c = cacheNode;
        if (c instanceof GenericPropertyGetNode) {
            return ((GenericPropertyGetNode) c).getValue(thisObj, receiver, defaultValue, this, false);
        }
        for (; c != null; c = c.next) {
            if (c.isConstantObjectSpecialization()) {
                JSDynamicObject expectedObj = c.getExpectedObject();
                if (thisObj != expectedObj) {
                    if (expectedObj == null) {
                        break;
                    } else {
                        continue;
                    }
                }
            }
            if (!c.isValid(this)) {
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.getValue(thisObj, receiver, defaultValue, this, guard);
            }
        }
        deoptimize(c);
        return getValueAndSpecialize(thisObj, receiver, defaultValue);
    }

    @TruffleBoundary
    private Object getValueAndSpecialize(Object thisObj, Object receiver, Object defaultValue) {
        GetCacheNode c = specialize(thisObj);
        return c.getValue(thisObj, receiver, defaultValue, this, false);
    }

    @Override
    protected GetCacheNode getCacheNode() {
        return this.cacheNode;
    }

    @Override
    protected void setCacheNode(GetCacheNode cache) {
        this.cacheNode = cache;
    }

    public abstract static class GetCacheNode extends PropertyCacheNode.CacheNode<GetCacheNode> {
        @Child protected GetCacheNode next;

        protected GetCacheNode(ReceiverCheckNode receiverCheck) {
            this(receiverCheck, 0);
        }

        protected GetCacheNode(ReceiverCheckNode receiverCheck, int specializationFlags) {
            super(receiverCheck, specializationFlags);
        }

        protected GetCacheNode(GetCacheNode next, ReceiverCheckNode receiverCheck) {
            this(receiverCheck);
            this.next = next;
        }

        @Override
        protected final GetCacheNode getNext() {
            return next;
        }

        @Override
        protected final void setNext(GetCacheNode next) {
            this.next = next;
        }

        protected abstract Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard);

        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(getValue(thisObj, receiver, Undefined.instance, root, guard));
        }

        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(getValue(thisObj, receiver, Undefined.instance, root, guard));
        }

        protected boolean getValueBoolean(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) throws UnexpectedResultException {
            return JSTypesGen.expectBoolean(getValue(thisObj, receiver, Undefined.instance, root, guard));
        }

        protected long getValueLong(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) throws UnexpectedResultException {
            return JSTypesGen.expectLong(getValue(thisObj, receiver, Undefined.instance, root, guard));
        }
    }

    public abstract static class LinkedPropertyGetNode extends GetCacheNode {
        protected LinkedPropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        protected LinkedPropertyGetNode(ReceiverCheckNode receiverCheck, int specializationFlags) {
            super(receiverCheck, specializationFlags);
        }

    }

    public static final class ObjectPropertyGetNode extends LinkedPropertyGetNode {

        private final Property property;

        public ObjectPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            this.property = property;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Object value = property.getLocation().get(store, guard);
            if (JSProperty.isProxy(property)) {
                return ((PropertyProxy) value).get(store);
            } else {
                assert JSProperty.isData(property);
                return value;
            }
        }
    }

    protected abstract static class AbstractFinalPropertyGetNode extends LinkedPropertyGetNode {
        private final Assumption finalAssumption;
        private final WeakReference<JSDynamicObject> expectedObjRef;

        protected AbstractFinalPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, JSDynamicObject expectedObj) {
            super(shapeCheck, IS_FINAL | (expectedObj != null ? IS_FINAL_CONSTANT_OBJECT : 0));
            this.finalAssumption = property.getLocation().isFinal() ? null : property.getLocation().getFinalAssumption();
            this.expectedObjRef = expectedObj == null ? null : new WeakReference<>(expectedObj);
        }

        @Override
        protected final boolean isValidFinalAssumption() {
            return finalAssumption == null || finalAssumption.isValid();
        }

        @Override
        protected final JSDynamicObject getExpectedObject() {
            return expectedObjRef.get();
        }

        @Override
        protected final void clearExpectedObject() {
            expectedObjRef.clear();
        }

        protected final boolean assertFinalValue(Object finalValue, Object thisObj, PropertyGetNode root) {
            if (!JSConfig.AssertFinalPropertySpecialization) {
                return true;
            }
            int depth = ((AbstractShapeCheckNode) receiverCheck).getDepth();
            DynamicObject store = (DynamicObject) thisObj;
            for (int i = 0; i < depth; i++) {
                store = JSObject.getPrototype(store);
            }
            Object actualValue = JSDynamicObject.getOrNull(store, root.getKey());
            assert finalValue.equals(actualValue);
            return true;
        }
    }

    public static final class FinalObjectPropertyGetNode extends AbstractFinalPropertyGetNode {

        private final Object finalValue;

        public FinalObjectPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, Object value, JSDynamicObject expectedObjRef) {
            super(property, shapeCheck, expectedObjRef);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            assert assertFinalValue(finalValue, thisObj, root);
            return finalValue;
        }
    }

    public static final class IntPropertyGetNode extends LinkedPropertyGetNode {

        private final IntLocation location;

        public IntPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            this.location = (IntLocation) property.getLocation();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return location.getInt(receiverCheck.getStore(thisObj), guard);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class FinalIntPropertyGetNode extends AbstractFinalPropertyGetNode {

        private final int finalValue;

        public FinalIntPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, int value, JSDynamicObject expectedObj) {
            super(property, shapeCheck, expectedObj);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            assert assertFinalValue(finalValue, thisObj, root);
            return finalValue;
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class DoublePropertyGetNode extends LinkedPropertyGetNode {

        private final DoubleLocation location;

        public DoublePropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            this.location = (DoubleLocation) property.getLocation();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueDouble(thisObj, receiver, root, guard);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return location.getDouble(receiverCheck.getStore(thisObj), guard);
        }
    }

    public static final class FinalDoublePropertyGetNode extends AbstractFinalPropertyGetNode {

        private final double finalValue;

        public FinalDoublePropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, double value, JSDynamicObject expectedObj) {
            super(property, shapeCheck, expectedObj);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueDouble(thisObj, receiver, root, guard);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            assert assertFinalValue(finalValue, thisObj, root);
            return finalValue;
        }
    }

    public static final class BooleanPropertyGetNode extends LinkedPropertyGetNode {

        private final BooleanLocation location;

        public BooleanPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            this.location = (BooleanLocation) property.getLocation();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueBoolean(thisObj, receiver, root, guard);
        }

        @Override
        protected boolean getValueBoolean(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return location.getBoolean(receiverCheck.getStore(thisObj), guard);
        }
    }

    public static final class FinalBooleanPropertyGetNode extends AbstractFinalPropertyGetNode {

        private final boolean finalValue;

        public FinalBooleanPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean value, JSDynamicObject expectedObj) {
            super(property, shapeCheck, expectedObj);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueBoolean(thisObj, receiver, root, guard);
        }

        @Override
        protected boolean getValueBoolean(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            assert assertFinalValue(finalValue, thisObj, root);
            return finalValue;
        }
    }

    public static final class LongPropertyGetNode extends LinkedPropertyGetNode {

        private final LongLocation location;

        public LongPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            this.location = (LongLocation) property.getLocation();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueLong(thisObj, receiver, root, guard);
        }

        @Override
        protected long getValueLong(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return location.getLong(receiverCheck.getStore(thisObj), guard);
        }
    }

    public static final class FinalLongPropertyGetNode extends AbstractFinalPropertyGetNode {

        private final long finalValue;

        public FinalLongPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, long value, JSDynamicObject expectedObj) {
            super(property, shapeCheck, expectedObj);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueLong(thisObj, receiver, root, guard);
        }

        @Override
        protected long getValueLong(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            assert assertFinalValue(finalValue, thisObj, root);
            return finalValue;
        }
    }

    public static final class AccessorPropertyGetNode extends LinkedPropertyGetNode {
        private final Property property;
        @Child private JSFunctionCallNode callNode;
        private final BranchProfile undefinedGetterBranch = BranchProfile.create();

        public AccessorPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isAccessor(property);
            this.property = property;
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Accessor accessor = (Accessor) property.getLocation().get(store, guard);

            DynamicObject getter = accessor.getGetter();
            if (getter != Undefined.instance) {
                return callNode.executeCall(JSArguments.createZeroArg(receiver, getter));
            } else {
                undefinedGetterBranch.enter();
                return Undefined.instance;
            }
        }
    }

    public static final class FinalAccessorPropertyGetNode extends AbstractFinalPropertyGetNode {

        @Child private JSFunctionCallNode callNode;
        private final BranchProfile undefinedGetterBranch = BranchProfile.create();
        private final Accessor finalAccessor;

        public FinalAccessorPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, Accessor finalAccessor, JSDynamicObject expectedObj) {
            super(property, shapeCheck, expectedObj);
            assert JSProperty.isAccessor(property);
            this.callNode = JSFunctionCallNode.createCall();
            this.finalAccessor = finalAccessor;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject getter = finalAccessor.getGetter();
            if (getter != Undefined.instance) {
                return callNode.executeCall(JSArguments.createZeroArg(receiver, getter));
            } else {
                undefinedGetterBranch.enter();
                return Undefined.instance;
            }
        }
    }

    /**
     * For use when a property is undefined. Returns undefined.
     */
    public static final class UndefinedPropertyGetNode extends LinkedPropertyGetNode {

        public UndefinedPropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return defaultValue;
        }
    }

    /**
     * For use when a global property is undefined. Throws ReferenceError.
     */
    public static final class UndefinedPropertyErrorNode extends LinkedPropertyGetNode {

        public UndefinedPropertyErrorNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            throw Errors.createReferenceErrorNotDefined(root.getContext(), root.getKey(), this);
        }
    }

    /**
     * For use when a property is undefined and __noSuchProperty__/__noSuchMethod__ had been set.
     */
    public static final class CheckNoSuchPropertyNode extends LinkedPropertyGetNode {
        private final JSContext context;
        @Child private PropertyGetNode getNoSuchPropertyNode;
        @Child private PropertyGetNode getNoSuchMethodNode;
        @Child private JSHasPropertyNode hasPropertyNode;
        @Child private JSFunctionCallNode callNoSuchNode;

        public CheckNoSuchPropertyNode(Object key, ReceiverCheckNode receiverCheck, JSContext context) {
            super(receiverCheck);
            this.context = context;
            assert !(key instanceof Symbol);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            if (JSRuntime.isObject(thisObj) && !JSAdapter.isJSAdapter(thisObj) && !JSProxy.isJSProxy(thisObj)) {
                if (!context.getNoSuchMethodUnusedAssumption().isValid() && root.isMethod() && getHasProperty().executeBoolean(thisObj, JSObject.NO_SUCH_METHOD_NAME)) {
                    Object function = getNoSuchMethod().getValue(thisObj);
                    if (function != Undefined.instance) {
                        if (JSFunction.isJSFunction(function)) {
                            return callNoSuchHandler((DynamicObject) thisObj, (DynamicObject) function, root, false);
                        } else {
                            return getFallback(defaultValue, root);
                        }
                    }
                }
                if (!context.getNoSuchPropertyUnusedAssumption().isValid()) {
                    Object function = getNoSuchProperty().getValue(thisObj);
                    if (JSFunction.isJSFunction(function)) {
                        return callNoSuchHandler((DynamicObject) thisObj, (DynamicObject) function, root, true);
                    }
                }
            }
            return getFallback(defaultValue, root);
        }

        private Object callNoSuchHandler(DynamicObject thisObj, DynamicObject function, PropertyGetNode root, boolean noSuchProperty) {
            // if accessing a global variable, pass undefined as `this` instead of global object.
            // only matters if callee is strict. cf. Nashorn ScriptObject.noSuch{Property,Method}.
            Object thisObject = root.isGlobal() ? Undefined.instance : thisObj;
            if (noSuchProperty) {
                return getCallNoSuch().executeCall(JSArguments.createOneArg(thisObject, function, root.getKey()));
            } else {
                return new JSNoSuchMethodAdapter(function, root.getKey(), thisObject);
            }
        }

        private Object getFallback(Object defaultValue, PropertyGetNode root) {
            if (root.isGlobal()) {
                throw Errors.createReferenceErrorNotDefined(root.getContext(), root.getKey(), this);
            } else {
                return defaultValue;
            }
        }

        public PropertyGetNode getNoSuchProperty() {
            if (getNoSuchPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNoSuchPropertyNode = insert(PropertyGetNode.create(JSObject.NO_SUCH_PROPERTY_NAME, context));
            }
            return getNoSuchPropertyNode;
        }

        public PropertyGetNode getNoSuchMethod() {
            if (getNoSuchMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNoSuchMethodNode = insert(PropertyGetNode.create(JSObject.NO_SUCH_METHOD_NAME, context));
            }
            return getNoSuchMethodNode;
        }

        public JSHasPropertyNode getHasProperty() {
            if (hasPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasPropertyNode = insert(JSHasPropertyNode.create());
            }
            return hasPropertyNode;
        }

        public JSFunctionCallNode getCallNoSuch() {
            if (callNoSuchNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNoSuchNode = insert(JSFunctionCallNode.createCall());
            }
            return callNoSuchNode;
        }
    }

    /**
     * If object is undefined or null, throw TypeError.
     */
    public static final class TypeErrorPropertyGetNode extends LinkedPropertyGetNode {
        public TypeErrorPropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            assert (thisObj == Undefined.instance || thisObj == Null.instance || thisObj == null) : thisObj;
            throw Errors.createTypeErrorCannotGetProperty(root.getContext(), root.getKey(), thisObj, root.isMethod(), this);
        }
    }

    public static final class JavaPackagePropertyGetNode extends LinkedPropertyGetNode {
        public JavaPackagePropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            Object key = root.getKey();
            if (key instanceof String) {
                return JavaPackage.getJavaClassOrConstructorOrSubPackage(root.getContext(), (DynamicObject) thisObj, (String) key);
            } else {
                return Undefined.instance;
            }
        }
    }

    public static final class JavaStringMethodGetNode extends LinkedPropertyGetNode {
        @Child private InteropLibrary interop;

        public JavaStringMethodGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            String thisStr = JSRuntime.toStringIsString(thisObj);
            if (root.getKey() instanceof String) {
                Object boxedString = root.getRealm().getEnv().asBoxedGuestValue(thisStr);
                try {
                    return interop.readMember(boxedString, (String) root.getKey());
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                }
            }
            return Undefined.instance;
        }
    }

    public static final class JSProxyDispatcherPropertyGetNode extends LinkedPropertyGetNode {

        @Child private JSProxyPropertyGetNode proxyGet;

        @SuppressWarnings("unused")
        public JSProxyDispatcherPropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(receiverCheck);
            this.proxyGet = JSProxyPropertyGetNode.create(context);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return proxyGet.executeWithReceiver(receiverCheck.getStore(thisObj), receiver, root.getKey());
        }
    }

    public static final class JSProxyDispatcherRequiredPropertyGetNode extends LinkedPropertyGetNode {

        @Child private JSProxyPropertyGetNode proxyGet;
        @Child private JSProxyHasPropertyNode proxyHas;

        @SuppressWarnings("unused")
        public JSProxyDispatcherRequiredPropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(receiverCheck);
            this.proxyGet = JSProxyPropertyGetNode.create(context);
            this.proxyHas = JSProxyHasPropertyNode.create(context);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            Object key = root.getKey();
            DynamicObject proxy = receiverCheck.getStore(thisObj);
            if (proxyHas.executeWithTargetAndKeyBoolean(proxy, key)) {
                return proxyGet.executeWithReceiver(proxy, receiver, key);
            } else {
                throw Errors.createReferenceErrorNotDefined(root.getContext(), key, this);
            }
        }

    }

    public static final class JSAdapterPropertyGetNode extends LinkedPropertyGetNode {
        public JSAdapterPropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            Object key = root.getKey();
            DynamicObject obj = (DynamicObject) thisObj;
            Object result = root.isMethod() ? JSAdapter.INSTANCE.getMethodHelper(obj, obj, key, root) : JSAdapter.INSTANCE.getHelper(obj, obj, key, root);
            return JSRuntime.nullToUndefined(result);
        }
    }

    public static final class UnspecializedPropertyGetNode extends LinkedPropertyGetNode {
        public UnspecializedPropertyGetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return JSObject.get((DynamicObject) thisObj, root.getKey());
        }
    }

    public static final class ForeignPropertyGetNode extends LinkedPropertyGetNode {

        @Child private ImportValueNode importValueNode;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child private PropertyGetNode getFromJSObjectNode;
        private final boolean isLength;
        private final boolean isMethod;
        private final boolean isGlobal;
        private final JSContext context;
        @Child private InteropLibrary interop;
        @Child private InteropLibrary getterInterop;

        private final BranchProfile errorBranch = BranchProfile.create();
        @CompilationFinal private boolean optimistic = true;

        public ForeignPropertyGetNode(Object key, boolean isMethod, boolean isGlobal, JSContext context) {
            super(new ForeignLanguageCheckNode());
            this.context = context;
            this.importValueNode = ImportValueNode.create();
            this.isLength = key.equals(JSAbstractArray.LENGTH);
            this.isMethod = isMethod;
            this.isGlobal = isGlobal;
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        private Object foreignGet(Object thisObj, PropertyGetNode root) {
            Object key = root.getKey();
            if (interop.isNull(thisObj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorCannotGetProperty(context, key, thisObj, isMethod, this);
            }
            Object foreignResult = getImpl(thisObj, key, root);
            return importValueNode.executeWithTarget(foreignResult);
        }

        private Object getImpl(Object thisObj, Object key, PropertyGetNode root) {
            if (!(key instanceof String)) {
                return maybeGetFromPrototype(thisObj, key);
            }
            String stringKey = (String) key;

            if (context.getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(thisObj)) {
                try {
                    return interop.readHashValue(thisObj, key);
                } catch (UnknownKeyException e) {
                    // fall through: still need to try members
                } catch (UnsupportedMessageException e) {
                    return Undefined.instance;
                }
            }

            if (context.isOptionNashornCompatibilityMode()) {
                Object result = tryGetters(thisObj, root);
                if (result != null) {
                    return result;
                }
            }
            if (optimistic) {
                try {
                    return interop.readMember(thisObj, stringKey);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    optimistic = false;
                    return maybeGetFromPrototype(thisObj, key);
                }
            } else {
                if (interop.isMemberReadable(thisObj, stringKey)) {
                    try {
                        return interop.readMember(thisObj, stringKey);
                    } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                        return Undefined.instance;
                    }
                } else {
                    return maybeGetFromPrototype(thisObj, key);
                }
            }
        }

        private Object maybeGetFromPrototype(Object thisObj, Object key) {
            if (context.getContextOptions().hasForeignObjectPrototype() || key instanceof Symbol || JSInteropUtil.isBoxedPrimitive(thisObj, interop)) {
                if (foreignObjectPrototypeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
                }
                DynamicObject prototype = foreignObjectPrototypeNode.executeDynamicObject(thisObj);
                return getFromJSObject(prototype, key);
            }
            return Undefined.instance;
        }

        private Object getFromJSObject(Object object, Object key) {
            assert JSObject.isJSObject(object);
            if (getFromJSObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getFromJSObjectNode = insert(PropertyGetNode.create(key, context));
            }
            assert key.equals(getFromJSObjectNode.getKey());
            return getFromJSObjectNode.getValue(object);
        }

        // in nashorn-compat mode, `javaObj.xyz` can mean `javaObj.getXyz()` or `javaObj.isXyz()`.
        private Object tryGetters(Object thisObj, PropertyGetNode root) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                Object result = tryInvokeGetter(thisObj, "get", root);
                if (result != null) {
                    return result;
                }
                result = tryInvokeGetter(thisObj, "is", root);
                // Nashorn would only accept `isXyz` of type boolean. We cannot check upfront!
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        private Object tryInvokeGetter(Object thisObj, String prefix, PropertyGetNode root) {
            assert context.isOptionNashornCompatibilityMode();
            String getterKey = root.getAccessorKey(prefix);
            if (getterKey == null) {
                return null;
            }
            if (getterInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            if (!getterInterop.isMemberInvocable(thisObj, getterKey)) {
                return null;
            }
            try {
                return getterInterop.invokeMember(thisObj, getterKey, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                return null; // try the next fallback
            }
        }

        private Object getSize(Object thisObj) {
            try {
                return JSRuntime.longToIntOrDouble(interop.getArraySize(thisObj));
            } catch (UnsupportedMessageException e) {
                errorBranch.enter();
                throw Errors.createTypeErrorInteropException(thisObj, e, "getArraySize", this);
            }
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            if (isMethod && !isGlobal) {
                return thisObj;
            }
            if (isLength && interop.hasArrayElements(thisObj)) {
                return getSize(thisObj);
            }
            return foreignGet(thisObj, root);
        }

    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static final class GenericPropertyGetNode extends GetCacheNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ForeignPropertyGetNode foreignGetNode;
        @Child private GetPropertyFromJSObjectNode getFromJSObjectNode;
        private final ConditionProfile isJSObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isForeignObject = ConditionProfile.createBinaryProfile();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();
        private final BranchProfile fallbackBranch = BranchProfile.create();

        public GenericPropertyGetNode() {
            super(null);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            if (isJSObject.profile(JSDynamicObject.isJSDynamicObject(thisObj))) {
                return getPropertyFromJSObject((DynamicObject) thisObj, receiver, defaultValue, root);
            } else {
                if (isForeignObject.profile(JSGuards.isForeignObject(thisObj))) {
                    // a TruffleObject from another language
                    if (foreignGetNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        foreignGetNode = insert(new ForeignPropertyGetNode(root.getKey(), root.isMethod(), root.isGlobal(), root.getContext()));
                    }
                    return foreignGetNode.getValue(thisObj, receiver, defaultValue, root, guard);
                } else {
                    // a primitive, or a Symbol
                    if (toObjectNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        toObjectNode = insert(JSToObjectNode.createToObjectNoCheck(root.getContext()));
                    }
                    DynamicObject object = JSRuntime.expectJSObject(toObjectNode.execute(thisObj), notAJSObjectBranch);
                    return getPropertyFromJSObject(object, receiver, defaultValue, root);
                }
            }
        }

        private Object getPropertyFromJSObject(DynamicObject thisObj, Object receiver, Object defaultValue, PropertyGetNode root) {
            if (root.getKey() instanceof HiddenKey) {
                Object result = JSDynamicObject.getOrNull(thisObj, root.getKey());
                if (result != null) {
                    return result;
                } else {
                    fallbackBranch.enter();
                    return getFallback(thisObj, root);
                }
            } else {
                if (getFromJSObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getFromJSObjectNode = insert(GetPropertyFromJSObjectNode.create(root));
                }
                return getFromJSObjectNode.executeWithJSObject(thisObj, receiver, defaultValue, root);
            }
        }

        protected Object getFallback(@SuppressWarnings("unused") DynamicObject thisObj, PropertyGetNode root) {
            if (root.isRequired()) {
                throw Errors.createReferenceErrorNotDefined(root.getContext(), root.getKey(), this);
            }
            return Undefined.instance;
        }
    }

    abstract static class GetPropertyFromJSObjectNode extends JavaScriptBaseNode {
        private final Object key;
        private final boolean isRequired;
        private final JSContext context;
        private final BranchProfile nullOrUndefinedBranch = BranchProfile.create();
        private final BranchProfile fallbackBranch = BranchProfile.create();

        GetPropertyFromJSObjectNode(PropertyGetNode root) {
            this.key = root.getKey();
            this.isRequired = root.isRequired();
            this.context = root.getContext();
        }

        public abstract Object executeWithJSObject(DynamicObject thisObj, Object receiver, Object defaultValue, PropertyGetNode root);

        public static GetPropertyFromJSObjectNode create(PropertyGetNode root) {
            return GetPropertyFromJSObjectNodeGen.create(root);
        }

        @Specialization(limit = "2", guards = {"!isGlobal()", "cachedClass == getJSClass(object)"})
        protected Object doJSObjectCached(DynamicObject object, Object receiver, Object defaultValue, PropertyGetNode root,
                        @Cached("getJSClass(object)") JSClass cachedClass) {
            return getPropertyFromJSObjectIntl(cachedClass, object, receiver, defaultValue, root);
        }

        @Specialization(replaces = "doJSObjectCached", guards = {"!isGlobal()"})
        protected Object doJSObjectDirect(DynamicObject object, Object receiver, Object defaultValue, PropertyGetNode root) {
            return getPropertyFromJSObjectIntl(JSObject.getJSClass(object), object, receiver, defaultValue, root);
        }

        @Specialization(guards = {"isGlobal()"})
        protected Object doRequired(DynamicObject object, Object receiver, Object defaultValue, PropertyGetNode root,
                        @Cached("create()") JSHasPropertyNode hasPropertyNode,
                        @Cached("create()") JSClassProfile classProfile) {
            if (hasPropertyNode.executeBoolean(object, key)) {
                return getPropertyFromJSObjectIntl(classProfile.profile(JSObject.getJSClass(object)), object, receiver, defaultValue, root);
            } else {
                fallbackBranch.enter();
                return getNoSuchProperty(object, defaultValue, root);
            }
        }

        protected JSClass getJSClass(DynamicObject object) {
            return JSObject.getJSClass(object);
        }

        private Object getPropertyFromJSObjectIntl(JSClass jsclass, DynamicObject object, Object receiver, Object defaultValue, PropertyGetNode root) {
            final boolean isMethod = root.isMethod();
            assert !(key instanceof HiddenKey);
            // 0. check for null or undefined
            if (jsclass == Null.NULL_CLASS) {
                nullOrUndefinedBranch.enter();
                throw Errors.createTypeErrorCannotGetProperty(root.getContext(), key, object, isMethod, this);
            }

            // 1. try to get a JS property
            Object value = isMethod ? jsclass.getMethodHelper(object, receiver, key, this) : jsclass.getHelper(object, receiver, key, this);
            if (value != null) {
                return value;
            }

            // 2. try to call fallback handler or return undefined
            fallbackBranch.enter();
            return getNoSuchProperty(object, defaultValue, root);
        }

        protected Object getNoSuchProperty(DynamicObject thisObj, Object defaultValue, PropertyGetNode root) {
            if (root.getContext().isOptionNashornCompatibilityMode() &&
                            (!root.getContext().getNoSuchPropertyUnusedAssumption().isValid() || (root.isMethod() && !root.getContext().getNoSuchMethodUnusedAssumption().isValid()))) {
                return getNoSuchPropertySlow(thisObj, defaultValue, root.isMethod());
            }
            return getFallback(defaultValue);
        }

        @TruffleBoundary
        private Object getNoSuchPropertySlow(DynamicObject thisObj, Object defaultValue, boolean isMethod) {
            if (!(key instanceof Symbol) && JSRuntime.isObject(thisObj) && !JSAdapter.isJSAdapter(thisObj) && !JSProxy.isJSProxy(thisObj)) {
                if (isMethod) {
                    Object function = JSObject.get(thisObj, JSObject.NO_SUCH_METHOD_NAME);
                    if (function != Undefined.instance) {
                        if (JSFunction.isJSFunction(function)) {
                            return callNoSuchHandler(thisObj, (DynamicObject) function, false);
                        } else {
                            return getFallback(defaultValue);
                        }
                    }
                }
                Object function = JSObject.get(thisObj, JSObject.NO_SUCH_PROPERTY_NAME);
                if (JSFunction.isJSFunction(function)) {
                    return callNoSuchHandler(thisObj, (DynamicObject) function, true);
                }
            }
            return getFallback(defaultValue);
        }

        private Object callNoSuchHandler(DynamicObject thisObj, DynamicObject function, boolean noSuchProperty) {
            // if accessing a global variable, pass undefined as `this` instead of global object.
            // only matters if callee is strict. cf. Nashorn ScriptObject.noSuch{Property,Method}.
            Object thisObject = isGlobal() ? Undefined.instance : thisObj;
            if (noSuchProperty) {
                return JSFunction.call(function, thisObject, new Object[]{key});
            } else {
                return new JSNoSuchMethodAdapter(function, key, thisObject);
            }
        }

        protected boolean isGlobal() {
            return isRequired;
        }

        protected Object getFallback(Object defaultValue) {
            if (isRequired) {
                throw Errors.createReferenceErrorNotDefined(context, key, this);
            }
            return defaultValue;
        }
    }

    public static final class ArrayLengthPropertyGetNode extends LinkedPropertyGetNode {
        @Child private ArrayLengthReadNode arrayLengthRead;
        @CompilationFinal private boolean longLength;

        public ArrayLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            assert isArrayLengthProperty(property);
            this.arrayLengthRead = ArrayLengthReadNode.create();
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            if (!longLength) {
                try {
                    return arrayLengthRead.executeInt(store);
                } catch (UnexpectedResultException e) {
                    longLength = true;
                    return e.getResult();
                }
            } else {
                return arrayLengthRead.executeDouble(store);
            }
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) throws UnexpectedResultException {
            assert assertIsArray(thisObj);
            return arrayLengthRead.executeInt(receiverCheck.getStore(thisObj));
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            assert assertIsArray(thisObj);
            return arrayLengthRead.executeDouble(receiverCheck.getStore(thisObj));
        }

        private boolean assertIsArray(Object thisObj) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            // shape check should be sufficient to guarantee this assertion
            assert JSArray.isJSArray(store);
            return true;
        }
    }

    public static final class FunctionLengthPropertyGetNode extends LinkedPropertyGetNode {
        private final BranchProfile isBoundBranch = BranchProfile.create();
        private final JSFunction.FunctionLengthPropertyProxy property;

        public FunctionLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            assert isFunctionLengthProperty(property);
            this.property = (JSFunction.FunctionLengthPropertyProxy) JSProperty.getConstantProxy(property);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return property.getProfiled(receiverCheck.getStore(thisObj), isBoundBranch);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class FunctionNamePropertyGetNode extends LinkedPropertyGetNode {
        private final BranchProfile isBoundBranch = BranchProfile.create();
        private final JSFunction.FunctionNamePropertyProxy property;

        public FunctionNamePropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            assert isFunctionNameProperty(property);
            this.property = (JSFunction.FunctionNamePropertyProxy) JSProperty.getConstantProxy(property);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return property.getProfiled(receiverCheck.getStore(thisObj), isBoundBranch);
        }
    }

    public static final class ClassPrototypePropertyGetNode extends LinkedPropertyGetNode {

        @CompilationFinal private DynamicObject constantFunction;
        @Child private CreateMethodPropertyNode setConstructor;
        @CompilationFinal private int kind;
        private final JSContext context;
        private final ConditionProfile prototypeInitializedProfile = ConditionProfile.createCountingProfile();

        private static final int UNKNOWN = 0;
        private static final int CONSTRUCTOR = 1;
        private static final int GENERATOR = 2;
        private static final int ASYNC_GENERATOR = 3;

        private static final DynamicObject UNKNOWN_FUN = Undefined.instance;
        private static final DynamicObject GENERIC_FUN = null;

        public ClassPrototypePropertyGetNode(Property property, ReceiverCheckNode receiverCheck, JSContext context) {
            super(receiverCheck);
            assert JSProperty.isData(property) && isClassPrototypeProperty(property);
            this.context = context;
            this.constantFunction = context.isMultiContext() ? GENERIC_FUN : UNKNOWN_FUN;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject functionObj = receiverCheck.getStore(thisObj);
            assert JSFunction.isJSFunction(functionObj);
            DynamicObject constantFun = constantFunction;
            if (constantFun == UNKNOWN_FUN) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                constantFunction = functionObj;
                // ensure `prototype` is initialized
                return JSFunction.getClassPrototype(functionObj);
            } else if (constantFun != GENERIC_FUN) {
                if (constantFun == functionObj) {
                    return JSFunction.getClassPrototypeInitialized(constantFun);
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    constantFunction = GENERIC_FUN;
                }
            }
            if (prototypeInitializedProfile.profile(JSFunction.isClassPrototypeInitialized(functionObj))) {
                return JSFunction.getClassPrototypeInitialized(functionObj);
            } else {
                return getPrototypeNotInitialized(functionObj);
            }
        }

        private Object getPrototypeNotInitialized(DynamicObject functionObj) {
            if (kind == UNKNOWN) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSFunctionData functionData = JSFunction.getFunctionData(functionObj);
                if (functionData.isAsyncGenerator()) {
                    kind = ASYNC_GENERATOR;
                } else if (functionData.isGenerator()) {
                    kind = GENERATOR;
                } else {
                    kind = CONSTRUCTOR;
                }
            }
            JSRealm realm = JSFunction.getRealm(functionObj, context, this);
            // Function kind guaranteed by shape check, see JSFunction
            DynamicObject prototype;
            if (kind == CONSTRUCTOR) {
                assert JSFunction.getFunctionData(functionObj).isConstructor();
                if (setConstructor == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setConstructor = insert(CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR));
                }
                prototype = JSOrdinary.create(context, realm);
                setConstructor.executeVoid(prototype, functionObj);
            } else if (kind == GENERATOR) {
                assert JSFunction.getFunctionData(functionObj).isGenerator();
                prototype = JSOrdinary.createWithRealm(context, context.getGeneratorObjectFactory(), realm);
            } else {
                assert kind == ASYNC_GENERATOR;
                assert JSFunction.getFunctionData(functionObj).isAsyncGenerator();
                prototype = JSOrdinary.createWithRealm(context, context.getAsyncGeneratorObjectFactory(), realm);
            }
            JSFunction.setClassPrototype(functionObj, prototype);
            return prototype;
        }
    }

    public static final class StringLengthPropertyGetNode extends LinkedPropertyGetNode {

        public StringLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property) && isStringLengthProperty(property);
            assert receiverCheck instanceof InstanceofCheckNode;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            CharSequence charSequence = (CharSequence) CompilerDirectives.castExact(thisObj, ((InstanceofCheckNode) receiverCheck).type);
            return JSRuntime.length(charSequence);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class StringObjectLengthPropertyGetNode extends LinkedPropertyGetNode {
        private final ValueProfile charSequenceClassProfile = ValueProfile.createClassProfile();

        public StringObjectLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property) && isStringLengthProperty(property);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            CharSequence charSequence = JSString.getCharSequence(receiverCheck.getStore(thisObj));
            return JSRuntime.length(charSequenceClassProfile.profile(charSequence));
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class LazyRegexResultIndexPropertyGetNode extends LinkedPropertyGetNode {

        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode readStartNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child private DynamicObjectLibrary readLazyRegexResult = JSObjectUtil.createDispatched(JSAbstractArray.LAZY_REGEX_RESULT_ID);

        public LazyRegexResultIndexPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            assert isLazyRegexResultIndexProperty(property);
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }

        @Override
        protected int getValueInt(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Object lazyRegexResult = readLazyRegexResult.getOrDefault(store, JSAbstractArray.LAZY_REGEX_RESULT_ID, null);
            assert lazyRegexResult != null;
            return readStartNode.execute(lazyRegexResult, TRegexUtil.Props.RegexResult.GET_START, 0);
        }

        @Override
        protected double getValueDouble(Object thisObj, Object receiver, PropertyGetNode root, boolean guard) {
            return getValueInt(thisObj, receiver, root, guard);
        }
    }

    public static final class LazyNamedCaptureGroupPropertyGetNode extends LinkedPropertyGetNode {

        private final int groupIndex;
        @Child private TRegexMaterializeResultNode materializeNode = TRegexMaterializeResultNode.create();
        @Child private TRegexResultAccessor resultAccessor = TRegexResultAccessor.create();
        private final ConditionProfile isIndicesObject = ConditionProfile.createBinaryProfile();

        public LazyNamedCaptureGroupPropertyGetNode(Property property, ReceiverCheckNode receiverCheck, int groupIndex) {
            super(receiverCheck);
            assert isLazyNamedCaptureGroupProperty(property);
            this.groupIndex = groupIndex;
        }

        @Override
        protected Object getValue(Object thisObj, Object receiver, Object defaultValue, PropertyGetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            JSRegExpGroupsObject groups = (JSRegExpGroupsObject) store;
            Object regexResult = groups.getRegexResult();
            if (isIndicesObject.profile(groups.isIndices())) {
                return LazyRegexResultIndicesArray.getIntIndicesArray(root.getContext(), resultAccessor, regexResult, groupIndex);
            } else {
                String input = groups.getInputString();
                return materializeNode.materializeGroup(regexResult, groupIndex, input);
            }
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected GetCacheNode createCachedPropertyNode(Property property, Object thisObj, int depth, Object value, GetCacheNode currentHead) {
        assert !isOwnProperty() || depth == 0;
        if (!(JSDynamicObject.isJSDynamicObject(thisObj))) {
            return createCachedPropertyNodeNotJSObject(property, thisObj, depth);
        }

        JSDynamicObject thisJSObj = (JSDynamicObject) thisObj;
        Shape cacheShape = thisJSObj.getShape();

        if ((JSProperty.isData(property) && !JSProperty.isProxy(property) || JSProperty.isAccessor(property)) &&
                        (property.getLocation().isFinal() || property.getLocation().isAssumedFinal())) {
            /**
             * if property is final and: <br>
             * (1) shape not in cache: specialize on final property with constant object [prototype
             * [chain]] shape check. <br>
             * (2) shape already in cache: if cache entry is constant object prototype [chain] shape
             * check, evict cache entry and specialize on final property with normal shape check,
             * otherwise go to (3). <br>
             *
             * (3) evict cache entry and treat property as non-final.
             */
            boolean isConstantObjectFinal = isPropertyAssumptionCheckEnabled();
            for (GetCacheNode cur = currentHead; cur != null; cur = cur.next) {
                if (isFinalSpecialization(cur)) {
                    if (cur.isConstantObjectSpecialization()) {
                        // invalidate the specialization and disable constant object checks
                        cur.clearExpectedObject();
                        setPropertyAssumptionCheckEnabled(false);
                        return null; // clean up cache
                    }
                    assert !cur.isConstantObjectSpecialization() || cur.getExpectedObject() == thisObj;
                }
            }
            if (isConstantObjectFinal && depth > 0 && !JSShape.getPropertyAssumption(cacheShape, key).isValid()) {
                // If no constant object specialization is possible, we can still try to specialize
                // on final prototype properties with a shape check on the receiver.
                isConstantObjectFinal = false;
            }

            if (JSProperty.isData(property) && !JSProperty.isProxy(property)) {
                if (isEligibleForFinalSpecialization(cacheShape, thisJSObj, depth, isConstantObjectFinal)) {
                    return createFinalDataPropertySpecialization(property, cacheShape, thisJSObj, depth, isConstantObjectFinal);
                }
            } else if (JSProperty.isAccessor(property)) {
                if (isEligibleForFinalSpecialization(cacheShape, thisJSObj, depth, isConstantObjectFinal)) {
                    return createFinalAccessorSpecialization(property, cacheShape, thisJSObj, depth, isConstantObjectFinal);
                }
            }
        }

        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisJSObj, depth, false, false);
        if (JSProperty.isData(property)) {
            return createSpecializationFromDataProperty(property, shapeCheck, context);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertyGetNode(property, shapeCheck);
        }
    }

    private static boolean isFinalSpecialization(GetCacheNode existingNode) {
        return existingNode instanceof AbstractFinalPropertyGetNode;
    }

    private boolean isEligibleForFinalSpecialization(Shape cacheShape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        if (depth == 0) {
            return (JSConfig.SkipFinalShapeCheck && isPropertyAssumptionCheckEnabled() && JSShape.getPropertyAssumption(cacheShape, key).isValid());
        } else {
            return (prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal));
        }
    }

    private GetCacheNode createCachedPropertyNodeNotJSObject(Property property, Object thisObj, int depth) {
        final ReceiverCheckNode receiverCheck;
        if (depth == 0) {
            if (isMethod() && JSRuntime.isString(thisObj) && context.isOptionNashornCompatibilityMode()) {
                // This hack ensures we get the Java method instead of the JavaScript property
                // for length in s.length() where s is a java.lang.String. Required by Nashorn.
                // We do this only for depth 0, because JavaScript prototype functions in turn
                // are preferred over Java methods with the same name.
                GetCacheNode javaPropertyNode = createJavaPropertyNodeMaybe(thisObj, depth);
                if (javaPropertyNode != null) {
                    return javaPropertyNode;
                }
            }

            receiverCheck = new InstanceofCheckNode(thisObj.getClass());

            if (isStringLengthProperty(property)) {
                return new StringLengthPropertyGetNode(property, receiverCheck);
            }
        } else {
            receiverCheck = createPrimitiveReceiverCheck(thisObj, depth);
        }

        if (JSProperty.isData(property)) {
            return createSpecializationFromDataProperty(property, receiverCheck, context);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertyGetNode(property, receiverCheck);
        }
    }

    private static GetCacheNode createSpecializationFromDataProperty(Property property, ReceiverCheckNode receiverCheck, JSContext context) {
        Property dataProperty = property;

        if (property.getLocation() instanceof IntLocation) {
            return new IntPropertyGetNode(dataProperty, receiverCheck);
        } else if (property.getLocation() instanceof DoubleLocation) {
            return new DoublePropertyGetNode(dataProperty, receiverCheck);
        } else if (property.getLocation() instanceof BooleanLocation) {
            return new BooleanPropertyGetNode(dataProperty, receiverCheck);
        } else if (property.getLocation() instanceof LongLocation) {
            return new LongPropertyGetNode(dataProperty, receiverCheck);
        } else {
            if (isArrayLengthProperty(property)) {
                return new ArrayLengthPropertyGetNode(dataProperty, receiverCheck);
            } else if (isFunctionLengthProperty(property)) {
                return new FunctionLengthPropertyGetNode(dataProperty, receiverCheck);
            } else if (isFunctionNameProperty(property)) {
                return new FunctionNamePropertyGetNode(dataProperty, receiverCheck);
            } else if (isClassPrototypeProperty(property)) {
                return new ClassPrototypePropertyGetNode(dataProperty, receiverCheck, context);
            } else if (isStringLengthProperty(property)) {
                return new StringObjectLengthPropertyGetNode(dataProperty, receiverCheck);
            } else if (isLazyRegexResultIndexProperty(property)) {
                return new LazyRegexResultIndexPropertyGetNode(dataProperty, receiverCheck);
            } else if (isLazyNamedCaptureGroupProperty(property)) {
                int groupIndex = ((JSRegExp.LazyNamedCaptureGroupProperty) JSProperty.getConstantProxy(property)).getGroupIndex();
                return new LazyNamedCaptureGroupPropertyGetNode(dataProperty, receiverCheck, groupIndex);
            } else {
                return new ObjectPropertyGetNode(dataProperty, receiverCheck);
            }
        }
    }

    private GetCacheNode createFinalDataPropertySpecialization(Property property, Shape cacheShape, JSDynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        AbstractShapeCheckNode finalShapeCheckNode = createShapeCheckNode(cacheShape, thisObj, depth, isConstantObjectFinal, false);
        finalShapeCheckNode.adoptChildren();
        DynamicObject store = finalShapeCheckNode.getStore(thisObj);

        JSDynamicObject constObjOrNull = isConstantObjectFinal ? thisObj : null;
        if (property.getLocation() instanceof IntLocation) {
            return new FinalIntPropertyGetNode(property, finalShapeCheckNode, ((IntLocation) property.getLocation()).getInt(store, false), constObjOrNull);
        } else if (property.getLocation() instanceof DoubleLocation) {
            return new FinalDoublePropertyGetNode(property, finalShapeCheckNode, ((DoubleLocation) property.getLocation()).getDouble(store, false), constObjOrNull);
        } else if (property.getLocation() instanceof BooleanLocation) {
            return new FinalBooleanPropertyGetNode(property, finalShapeCheckNode, ((BooleanLocation) property.getLocation()).getBoolean(store, false), constObjOrNull);
        } else if (property.getLocation() instanceof LongLocation) {
            return new FinalLongPropertyGetNode(property, finalShapeCheckNode, ((LongLocation) property.getLocation()).getLong(store, false), constObjOrNull);
        } else {
            assert property.getLocation() instanceof ObjectLocation;
            return new FinalObjectPropertyGetNode(property, finalShapeCheckNode, property.get(store, false), constObjOrNull);
        }
    }

    private GetCacheNode createFinalAccessorSpecialization(Property property, Shape cacheShape, JSDynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        AbstractShapeCheckNode finalShapeCheckNode = createShapeCheckNode(cacheShape, thisObj, depth, isConstantObjectFinal, false);
        finalShapeCheckNode.adoptChildren();
        DynamicObject store = finalShapeCheckNode.getStore(thisObj);
        Accessor accessor = (Accessor) property.get(store, null);
        JSDynamicObject constObjOrNull = isConstantObjectFinal ? thisObj : null;
        return new FinalAccessorPropertyGetNode(property, finalShapeCheckNode, accessor, constObjOrNull);
    }

    @Override
    protected GetCacheNode createJavaPropertyNodeMaybe(Object thisObj, int depth) {
        if (JavaPackage.isJavaPackage(thisObj)) {
            return new JavaPackagePropertyGetNode(new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)));
        } else if (JavaImporter.isJavaImporter(thisObj)) {
            return new UnspecializedPropertyGetNode(new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)));
        }
        if (JSConfig.SubstrateVM) {
            return null;
        }
        if (context.isOptionNashornCompatibilityMode() && getRealm().isJavaInteropEnabled()) {
            if (JSRuntime.isString(thisObj) && isMethod()) {
                return new JavaStringMethodGetNode(createPrimitiveReceiverCheck(thisObj, depth));
            }
        }
        return null;
    }

    @Override
    protected GetCacheNode createUndefinedPropertyNode(Object thisObj, Object store, int depth, Object value) {
        GetCacheNode javaPropertyNode = createJavaPropertyNodeMaybe(thisObj, depth);
        if (javaPropertyNode != null) {
            return javaPropertyNode;
        }

        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            JSDynamicObject jsobject = (JSDynamicObject) thisObj;
            Shape cacheShape = jsobject.getShape();
            AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, jsobject, depth, false, false);
            ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(jsobject)) : shapeCheck;
            if (JSAdapter.isJSAdapter(store)) {
                return new JSAdapterPropertyGetNode(receiverCheck);
            } else if (JSProxy.isJSProxy(store) && JSRuntime.isPropertyKey(key)) {
                return createJSProxyCache(receiverCheck);
            } else if (JSModuleNamespace.isJSModuleNamespace(store)) {
                return new UnspecializedPropertyGetNode(receiverCheck);
            } else {
                return createUndefinedJSObjectPropertyNode(jsobject, depth);
            }
        } else if (JSProxy.isJSProxy(store)) {
            ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth);
            return createJSProxyCache(receiverCheck);
        } else {
            if (thisObj == null) {
                return new TypeErrorPropertyGetNode(new NullCheckNode());
            } else {
                ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth);
                return createUndefinedOrErrorPropertyNode(receiverCheck);
            }
        }
    }

    protected GetCacheNode createJSProxyCache(ReceiverCheckNode receiverCheck) {
        if (isProxyHandlerGetNode()) {
            // avoid building deeply nested property caches
            return createGenericPropertyNode();
        }

        if (isRequired()) {
            return new JSProxyDispatcherRequiredPropertyGetNode(context, key, receiverCheck, isMethod());
        } else {
            return new JSProxyDispatcherPropertyGetNode(context, key, receiverCheck, isMethod());
        }
    }

    private boolean isProxyHandlerGetNode() {
        Node parent = getParent();
        if (parent instanceof GetMethodNode) {
            parent = parent.getParent();
        }
        return (parent instanceof JSProxyPropertyGetNode);
    }

    private GetCacheNode createUndefinedJSObjectPropertyNode(JSDynamicObject jsobject, int depth) {
        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(jsobject.getShape(), jsobject, depth, false, false);
        if (JSRuntime.isObject(jsobject)) {
            if (context.isOptionNashornCompatibilityMode() && !(key instanceof Symbol)) {
                if ((!context.getNoSuchMethodUnusedAssumption().isValid() && JSObject.hasProperty(jsobject, JSObject.NO_SUCH_METHOD_NAME)) ||
                                (!context.getNoSuchPropertyUnusedAssumption().isValid() && JSObject.hasProperty(jsobject, JSObject.NO_SUCH_PROPERTY_NAME))) {
                    return new CheckNoSuchPropertyNode(key, shapeCheck, context);
                }
            }
            return createUndefinedOrErrorPropertyNode(shapeCheck);
        } else {
            return new TypeErrorPropertyGetNode(shapeCheck);
        }
    }

    protected GetCacheNode createUndefinedOrErrorPropertyNode(ReceiverCheckNode receiverCheck) {
        if (isRequired()) {
            return new UndefinedPropertyErrorNode(receiverCheck);
        } else {
            return new UndefinedPropertyGetNode(receiverCheck);
        }
    }

    /**
     * Make a generic-case node, for when polymorphism becomes too high.
     */
    @Override
    protected GetCacheNode createGenericPropertyNode() {
        return new GenericPropertyGetNode();
    }

    protected final boolean isRequired() {
        return isGlobal();
    }

    @Override
    protected final boolean isGlobal() {
        return isGlobal;
    }

    @Override
    protected final boolean isOwnProperty() {
        return getOwnProperty;
    }

    protected boolean isMethod() {
        return isMethod;
    }

    protected void setMethod() {
        CompilerAsserts.neverPartOfCompilation();
        this.isMethod = true;
    }

    @Override
    protected boolean isPropertyAssumptionCheckEnabled() {
        return propertyAssumptionCheckEnabled && getContext().isSingleRealm();
    }

    @Override
    protected void setPropertyAssumptionCheckEnabled(boolean value) {
        CompilerAsserts.neverPartOfCompilation();
        this.propertyAssumptionCheckEnabled = value;
    }

    @Override
    protected GetCacheNode createTruffleObjectPropertyNode() {
        return new ForeignPropertyGetNode(key, isMethod(), isGlobal(), context);
    }

    @Override
    protected boolean canCombineShapeCheck(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property) {
        assert shapesHaveCommonLayoutForKey(parentShape, cacheShape);
        if (JSDynamicObject.isJSDynamicObject(thisObj) && JSProperty.isData(property)) {
            if (!JSProperty.isAccessor(property) && !JSProperty.isProxy(property)) {
                return !property.getLocation().isFinal() && !property.getLocation().isAssumedFinal();
            }
        }
        return false;
    }

    @Override
    protected GetCacheNode createCombinedIcPropertyNode(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property) {
        CombinedShapeCheckNode receiverCheck = new CombinedShapeCheckNode(parentShape, cacheShape);

        if (property.getLocation() instanceof IntLocation) {
            return new IntPropertyGetNode(property, receiverCheck);
        } else if (property.getLocation() instanceof DoubleLocation) {
            return new DoublePropertyGetNode(property, receiverCheck);
        } else if (property.getLocation() instanceof BooleanLocation) {
            return new BooleanPropertyGetNode(property, receiverCheck);
        } else if (property.getLocation() instanceof LongLocation) {
            return new LongPropertyGetNode(property, receiverCheck);
        } else {
            assert !JSProperty.isProxy(property);
            return new ObjectPropertyGetNode(property, receiverCheck);
        }
    }
}
