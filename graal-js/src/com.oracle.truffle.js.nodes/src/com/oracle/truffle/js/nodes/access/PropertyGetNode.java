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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResult;

import java.util.Map;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.DynamicObject;
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
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthReadNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNodeGen;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSNoSuchMethodAdapter;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaGetter;
import com.oracle.truffle.js.runtime.interop.JavaImporter;
import com.oracle.truffle.js.runtime.interop.JavaMember;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.interop.JavaSuperAdapter;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResultNode;

/**
 * ES6 9.1.8 [[Get]] (P, Receiver).
 *
 * @see PropertyNode
 * @see GlobalPropertyNode
 */
public abstract class PropertyGetNode extends PropertyCacheNode<PropertyGetNode> {

    public static PropertyGetNode create(Object key, boolean isGlobal, JSContext context) {
        final boolean getOwnProperty = false;
        return createImpl(key, isGlobal, context, getOwnProperty);
    }

    private static PropertyGetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean getOwnProperty) {
        if (JSTruffleOptions.PropertyCacheLimit > 0) {
            return new UninitializedPropertyGetNode(key, isGlobal, context, getOwnProperty);
        } else {
            return createGeneric(key, isGlobal, false, getOwnProperty, context);
        }
    }

    public static PropertyGetNode createGetOwn(Object key, JSContext context) {
        final boolean global = false;
        final boolean getOwnProperty = true;
        return createImpl(key, global, context, getOwnProperty);
    }

    public static PropertyGetNode createGetHidden(HiddenKey key, JSContext context) {
        return createGetOwn(key, context);
    }

    protected PropertyGetNode(Object key) {
        super(key);
    }

    public final Object getValue(Object obj) {
        return getValue(obj, obj);
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

    protected abstract Object getValue(Object obj, Object receiver);

    protected abstract int getValueInt(Object obj, Object receiver) throws UnexpectedResultException;

    protected abstract double getValueDouble(Object obj, Object receiver) throws UnexpectedResultException;

    protected abstract boolean getValueBoolean(Object obj, Object receiver) throws UnexpectedResultException;

    protected abstract long getValueLong(Object obj, Object receiver) throws UnexpectedResultException;

    public abstract static class LinkedPropertyGetNode extends PropertyGetNode {
        @Child protected PropertyGetNode next;
        @Child protected ReceiverCheckNode receiverCheck;

        public LinkedPropertyGetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key);
            this.receiverCheck = receiverCheck;
        }

        @Override
        public final Object getValue(Object thisObj, Object receiver) {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    return getValueUnchecked(thisObj, receiver, condition);
                } else {
                    return next.getValue(thisObj, receiver);
                }
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonShapeAssumptionInvalidated(key)).getValue(thisObj);
            }
        }

        @Override
        public NodeCost getCost() {
            if (next != null && next.getCost() == NodeCost.MONOMORPHIC) {
                return NodeCost.POLYMORPHIC;
            }
            return super.getCost();
        }

        public abstract Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition);

        @Override
        public final int getValueInt(Object thisObj, Object receiver) throws UnexpectedResultException {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    return getValueUncheckedInt(thisObj, receiver, condition);
                } else {
                    return next.getValueInt(thisObj, receiver);
                }
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonShapeAssumptionInvalidated(key)).getValueInt(thisObj);
            }
        }

        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(getValueUnchecked(thisObj, receiver, floatingCondition));
        }

        @Override
        public final double getValueDouble(Object thisObj, Object receiver) throws UnexpectedResultException {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    return getValueUncheckedDouble(thisObj, receiver, condition);
                } else {
                    return next.getValueDouble(thisObj, receiver);
                }
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonShapeAssumptionInvalidated(key)).getValueDouble(thisObj);
            }
        }

        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(getValueUnchecked(thisObj, receiver, floatingCondition));
        }

        @Override
        public final boolean getValueBoolean(Object thisObj, Object receiver) throws UnexpectedResultException {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    return getValueUncheckedBoolean(thisObj, receiver, condition);
                } else {
                    return next.getValueBoolean(thisObj, receiver);
                }
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonShapeAssumptionInvalidated(key)).getValueBoolean(thisObj);
            }
        }

        public boolean getValueUncheckedBoolean(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            return JSTypesGen.expectBoolean(getValueUnchecked(thisObj, receiver, floatingCondition));
        }

        @Override
        public final long getValueLong(Object thisObj, Object receiver) throws UnexpectedResultException {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    return getValueUncheckedLong(thisObj, receiver, condition);
                } else {
                    return next.getValueLong(thisObj, receiver);
                }
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonShapeAssumptionInvalidated(key)).getValueLong(thisObj);
            }
        }

        public long getValueUncheckedLong(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            return JSTypesGen.expectLong(getValueUnchecked(thisObj, receiver, floatingCondition));
        }

        protected PropertyGetNode rewrite(CharSequence reason) {
            CompilerAsserts.neverPartOfCompilation();
            assert next != null;
            PropertyGetNode replacedNext = replace(next, reason);
            return replacedNext;
        }

        @Override
        protected final Shape getShape() {
            return receiverCheck.getShape();
        }

        @Override
        @TruffleBoundary
        public String debugString() {
            return getClass().getSimpleName() + "<property=" + key + ",shape=" + getShape() + ">\n" + ((next == null) ? "" : next.debugString());
        }

        @Override
        @TruffleBoundary
        public String toString() {
            return super.toString() + " property=" + key;
        }

        @Override
        public PropertyGetNode getNext() {
            return next;
        }

        @Override
        protected final void setNext(PropertyGetNode to) {
            next = to;
        }

        protected final boolean assertFinalValue(Object finalValue, Object thisObj) {
            if (!JSTruffleOptions.AssertFinalPropertySpecialization) {
                return true;
            }
            int depth = ((AbstractShapeCheckNode) (receiverCheck != null ? receiverCheck : ((AssumedFinalPropertyGetNode) getParent()).receiverCheck)).getDepth();
            DynamicObject store = (DynamicObject) thisObj;
            for (int i = 0; i < depth; i++) {
                store = JSObject.getPrototype(store);
            }
            return finalValue.equals(store.get(key));
        }

        @Override
        public JSContext getContext() {
            return getNext().getContext();
        }

        @Override
        protected boolean isGlobal() {
            return getNext().isGlobal();
        }

        @Override
        protected boolean isMethod() {
            return getNext().isMethod();
        }

        @Override
        protected boolean isOwnProperty() {
            return getNext().isOwnProperty();
        }
    }

    public static final class ObjectPropertyGetNode extends LinkedPropertyGetNode {

        private final Property property;

        public ObjectPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.property = property;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return JSProperty.getValue(property, receiverCheck.getStore(thisObj), receiver, floatingCondition);
        }
    }

    public static final class FinalObjectPropertyGetNode extends LinkedPropertyGetNode {

        private final Object finalValue;

        public FinalObjectPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, Object value) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertFinalValue(finalValue, thisObj);
            return finalValue;
        }
    }

    public static final class IntPropertyGetNode extends LinkedPropertyGetNode {

        private final IntLocation location;

        public IntPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.location = (IntLocation) property.getLocation();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getInt(receiverCheck.getStore(thisObj), floatingCondition);
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getInt(receiverCheck.getStore(thisObj), floatingCondition);
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getInt(receiverCheck.getStore(thisObj), floatingCondition);
        }
    }

    public static final class FinalIntPropertyGetNode extends LinkedPropertyGetNode {

        private final int finalValue;

        public FinalIntPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, int value) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertFinalValue(finalValue, thisObj);
            return finalValue;
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }
    }

    public static final class DoublePropertyGetNode extends LinkedPropertyGetNode {

        private final DoubleLocation location;

        public DoublePropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.location = (DoubleLocation) property.getLocation();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getDouble(receiverCheck.getStore(thisObj), floatingCondition);
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getDouble(receiverCheck.getStore(thisObj), floatingCondition);
        }
    }

    public static final class FinalDoublePropertyGetNode extends LinkedPropertyGetNode {

        private final double finalValue;

        public FinalDoublePropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, double value) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedDouble(thisObj, receiver, floatingCondition);
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertFinalValue(finalValue, thisObj);
            return finalValue;
        }
    }

    public static final class BooleanPropertyGetNode extends LinkedPropertyGetNode {

        private final BooleanLocation location;

        public BooleanPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.location = (BooleanLocation) property.getLocation();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedBoolean(thisObj, receiver, floatingCondition);
        }

        @Override
        public boolean getValueUncheckedBoolean(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getBoolean(receiverCheck.getStore(thisObj), floatingCondition);
        }
    }

    public static final class FinalBooleanPropertyGetNode extends LinkedPropertyGetNode {

        private final boolean finalValue;

        public FinalBooleanPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean value) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedBoolean(thisObj, receiver, floatingCondition);
        }

        @Override
        public boolean getValueUncheckedBoolean(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertFinalValue(finalValue, thisObj);
            return finalValue;
        }
    }

    public static final class LongPropertyGetNode extends LinkedPropertyGetNode {

        private final LongLocation location;

        public LongPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.location = (LongLocation) property.getLocation();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getLong(receiverCheck.getStore(thisObj), floatingCondition);
        }

        @Override
        public long getValueUncheckedLong(Object thisObj, Object receiver, boolean floatingCondition) {
            return location.getLong(receiverCheck.getStore(thisObj), floatingCondition);
        }
    }

    public static final class FinalLongPropertyGetNode extends LinkedPropertyGetNode {

        private final long finalValue;

        public FinalLongPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, long value) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalValue = value;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedLong(thisObj, receiver, floatingCondition);
        }

        @Override
        public long getValueUncheckedLong(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertFinalValue(finalValue, thisObj);
            return finalValue;
        }
    }

    public static final class AssumedFinalPropertyGetNode extends LinkedPropertyGetNode {
        @Child private LinkedPropertyGetNode finalGetNode;
        private final Assumption finalAssumption;

        public AssumedFinalPropertyGetNode(Property property, AbstractShapeCheckNode shapeCheck, LinkedPropertyGetNode getNode) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            this.finalGetNode = getNode;
            this.finalAssumption = property.getLocation().getFinalAssumption();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            try {
                finalAssumption.check();
                return finalGetNode.getValueUnchecked(thisObj, receiver, floatingCondition);
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonFinalAssumptionInvalidated(key)).getValue(thisObj, receiver);
            }
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            try {
                finalAssumption.check();
                return finalGetNode.getValueUncheckedInt(thisObj, receiver, floatingCondition);
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonFinalAssumptionInvalidated(key)).getValueInt(thisObj, receiver);
            }
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            try {
                finalAssumption.check();
                return finalGetNode.getValueUncheckedDouble(thisObj, receiver, floatingCondition);
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonFinalAssumptionInvalidated(key)).getValueDouble(thisObj, receiver);
            }
        }

        @Override
        public boolean getValueUncheckedBoolean(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            try {
                finalAssumption.check();
                return finalGetNode.getValueUncheckedBoolean(thisObj, receiver, floatingCondition);
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonFinalAssumptionInvalidated(key)).getValueBoolean(thisObj, receiver);
            }
        }

        @Override
        public long getValueUncheckedLong(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            try {
                finalAssumption.check();
                return finalGetNode.getValueUncheckedLong(thisObj, receiver, floatingCondition);
            } catch (InvalidAssumptionException e) {
                return rewrite(reasonFinalAssumptionInvalidated(key)).getValueLong(thisObj, receiver);
            }
        }
    }

    public static final class AccessorPropertyGetNode extends LinkedPropertyGetNode {
        private final Property property;
        @Child private JSFunctionCallNode callNode;
        private final BranchProfile undefinedGetterBranch = BranchProfile.create();

        public AccessorPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isAccessor(property);
            this.property = property;
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Accessor accessor = (Accessor) property.get(store, floatingCondition);

            DynamicObject getter = accessor.getGetter();
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

        public UndefinedPropertyGetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return Undefined.instance;
        }
    }

    /**
     * For use when a global property is undefined. Throws ReferenceError.
     */
    public static final class UndefinedPropertyErrorNode extends LinkedPropertyGetNode {

        public UndefinedPropertyErrorNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            throw Errors.createReferenceErrorNotDefined(key, this);
        }
    }

    /**
     * For use when a property is undefined and __noSuchProperty__/__noSuchMethod__ had been set.
     */
    public static final class CheckNoSuchPropertyNode extends LinkedPropertyGetNode {
        private final boolean isGlobal;
        private final JSContext context;
        @Child private PropertyNode getNoSuchProperty;
        @Child private PropertyNode getNoSuchMethod;
        @Child private JSHasPropertyNode hasProperty;
        @Child private JSFunctionCallNode callNoSuch;

        public CheckNoSuchPropertyNode(Object key, ReceiverCheckNode receiverCheck, boolean isGlobal, JSContext context) {
            super(key, receiverCheck);
            this.isGlobal = isGlobal;
            this.context = context;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (!(key instanceof Symbol) && JSRuntime.isObject(thisObj) && !JSAdapter.isJSAdapter(thisObj) && !JSProxy.isProxy(thisObj)) {
                if (!context.getNoSuchMethodUnusedAssumption().isValid() && isMethod() && getHasProperty().executeBoolean((DynamicObject) thisObj, JSObject.NO_SUCH_METHOD_NAME)) {
                    Object function = getNoSuchMethod().executeWithTarget(thisObj);
                    if (function != Undefined.instance) {
                        if (JSFunction.isJSFunction(function)) {
                            return callNoSuchHandler((DynamicObject) thisObj, (DynamicObject) function, false);
                        } else {
                            return getFallback();
                        }
                    }
                }
                if (!context.getNoSuchPropertyUnusedAssumption().isValid()) {
                    Object function = getNoSuchProperty().executeWithTarget(thisObj);
                    if (JSFunction.isJSFunction(function)) {
                        return callNoSuchHandler((DynamicObject) thisObj, (DynamicObject) function, true);
                    }
                }
            }
            return getFallback();
        }

        private Object callNoSuchHandler(DynamicObject thisObj, DynamicObject function, boolean noSuchProperty) {
            // if accessing a global variable, pass undefined as `this` instead of global object.
            // only matters if callee is strict. cf. Nashorn ScriptObject.noSuch{Property,Method}.
            Object thisObject = isGlobal ? Undefined.instance : thisObj;
            if (noSuchProperty) {
                return getCallNoSuch().executeCall(JSArguments.createOneArg(thisObject, function, key));
            } else {
                return new JSNoSuchMethodAdapter(function, key, thisObject);
            }
        }

        private Object getFallback() {
            if (isGlobal) {
                throw Errors.createReferenceErrorNotDefined(key, this);
            } else {
                return Undefined.instance;
            }
        }

        public PropertyNode getNoSuchProperty() {
            if (getNoSuchProperty == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNoSuchProperty = insert(NodeFactory.getInstance(context).createProperty(context, null, JSObject.NO_SUCH_PROPERTY_NAME));
            }
            return getNoSuchProperty;
        }

        public PropertyNode getNoSuchMethod() {
            if (getNoSuchMethod == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNoSuchMethod = insert(NodeFactory.getInstance(context).createProperty(context, null, JSObject.NO_SUCH_METHOD_NAME));
            }
            return getNoSuchMethod;
        }

        public JSHasPropertyNode getHasProperty() {
            if (hasProperty == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasProperty = insert(JSHasPropertyNode.create());
            }
            return hasProperty;
        }

        public JSFunctionCallNode getCallNoSuch() {
            if (callNoSuch == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNoSuch = insert(JSFunctionCallNode.createCall());
            }
            return callNoSuch;
        }
    }

    /**
     * If object is undefined or null, throw TypeError.
     */
    public static final class TypeErrorPropertyGetNode extends LinkedPropertyGetNode {
        private final boolean isMethod;

        public TypeErrorPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(key, receiverCheck);
            this.isMethod = isMethod;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            assert (thisObj == Undefined.instance || thisObj == Null.instance || thisObj == null) : thisObj;
            throw Errors.createTypeErrorCannotGetProperty(key, thisObj, isMethod, this);
        }
    }

    public static final class JavaGetterPropertyGetNode extends LinkedPropertyGetNode {
        @Child private JSFunctionCallNode.JavaMethodCallNode methodCall;

        public JavaGetterPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, JavaGetter getter) {
            super(key, receiverCheck);
            this.methodCall = JSFunctionCallNode.JavaMethodCallNode.create(getter);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return methodCall.executeCall(JSArguments.createZeroArg(thisObj, null));
        }
    }

    public static final class JavaMethodPropertyGetNode extends LinkedPropertyGetNode {
        private final JavaMethod method;

        public JavaMethodPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, JavaMethod method) {
            super(key, receiverCheck);
            this.method = method;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return method;
        }
    }

    public static final class JavaPackagePropertyGetNode extends LinkedPropertyGetNode {
        private final JSContext context;

        public JavaPackagePropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
            this.context = context;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (key instanceof String) {
                return JavaPackage.getJavaClassOrConstructorOrSubPackage(context, (DynamicObject) thisObj, (String) key);
            } else {
                return Undefined.instance;
            }
        }
    }

    public static final class JSJavaWrapperPropertyGetNode extends LinkedPropertyGetNode {
        @Child private PropertyGetNode nested;

        public JSJavaWrapperPropertyGetNode(Object key, boolean isGlobal, boolean isMethod, boolean getOwnProperty, JSContext context) {
            super(key, new JSClassCheckNode(JSJavaWrapper.getJSClassInstance()));
            this.nested = new UninitializedPropertyGetNode(key, isGlobal, context, getOwnProperty);
            if (isMethod) {
                this.nested.setMethod();
            }
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return nested.getValue(JSJavaWrapper.getWrapped((DynamicObject) thisObj));
        }
    }

    public static final class CachedJavaPackagePropertyGetNode extends LinkedPropertyGetNode {
        private final JSContext context;
        private final DynamicObject javaPackage;
        private final Object member;

        public CachedJavaPackagePropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, DynamicObject javaPackage) {
            super(key, receiverCheck);
            this.context = context;
            this.javaPackage = javaPackage;
            this.member = key instanceof String ? JavaPackage.getJavaClassOrConstructorOrSubPackage(context, javaPackage, (String) key) : Undefined.instance;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (javaPackage == thisObj) {
                return member;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JavaPackagePropertyGetNode newNode = new JavaPackagePropertyGetNode(context, key, receiverCheck);
                newNode.next = this.next;
                return this.replace(newNode).getValueUnchecked(thisObj, receiver, floatingCondition);
            }
        }
    }

    public static class JavaClassPropertyGetNode extends LinkedPropertyGetNode {
        protected final boolean isMethod;
        protected final boolean isClassFilterPresent;

        public JavaClassPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean isMethod, boolean isClassFilterPresent) {
            super(key, receiverCheck);
            this.isMethod = isMethod;
            this.isClassFilterPresent = isClassFilterPresent;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getMember((JavaClass) thisObj);
        }

        protected final Object getMember(JavaClass type) {
            JavaMember member = type.getMember((String) key, JavaClass.STATIC, getJavaMemberTypes(isMethod), isClassFilterPresent);
            if (member == null) {
                return JSRuntime.nullToUndefined(type.getInnerClass((String) key));
            }
            if (member instanceof JavaGetter) {
                return JSRuntime.toJSNull(((JavaGetter) member).getValue(null));
            }
            return JSRuntime.nullToUndefined(member);
        }
    }

    public static final class CachedJavaClassPropertyGetNode extends JavaClassPropertyGetNode {
        private final JavaClass javaClass;
        private final Object cachedMember;

        public CachedJavaClassPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean isMethod, boolean isClassFilterPresent, JavaClass javaClass) {
            super(key, receiverCheck, isMethod, isClassFilterPresent);
            this.javaClass = javaClass;
            this.cachedMember = getMember(javaClass);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (javaClass == thisObj) {
                return cachedMember;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JavaClassPropertyGetNode newNode = new JavaClassPropertyGetNode(key, receiverCheck, isMethod, isClassFilterPresent);
                newNode.next = this.next;
                return this.replace(newNode).getValueUnchecked(thisObj, receiver, floatingCondition);
            }
        }
    }

    public static class JavaSuperMethodPropertyGetNode extends LinkedPropertyGetNode {
        protected final boolean classFilterPresent;

        public JavaSuperMethodPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean classFilterPresent) {
            super(key, receiverCheck);
            this.classFilterPresent = classFilterPresent;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getSuperMethod(((JavaSuperAdapter) thisObj).getAdapter().getClass());
        }

        @TruffleBoundary
        protected final Object getSuperMethod(Class<? extends Object> adapterClass) {
            return JSRuntime.nullToUndefined(JavaClass.forClass(adapterClass).getSuperMethod((String) key, classFilterPresent));
        }
    }

    public static final class CachedJavaSuperMethodPropertyGetNode extends JavaSuperMethodPropertyGetNode {
        private final Class<? extends Object> expectedClass;
        private final Object cachedMember;

        public CachedJavaSuperMethodPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean classFilterPresent, JavaSuperAdapter javaSuperAdapter) {
            super(key, receiverCheck, classFilterPresent);
            this.expectedClass = javaSuperAdapter.getAdapter().getClass();
            this.cachedMember = getSuperMethod(expectedClass);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (((JavaSuperAdapter) thisObj).getAdapter().getClass() == expectedClass) {
                return cachedMember;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JavaSuperMethodPropertyGetNode newNode = new JavaSuperMethodPropertyGetNode(key, receiverCheck, classFilterPresent);
                newNode.next = this.next;
                return this.replace(newNode).getValueUnchecked(thisObj, receiver, floatingCondition);
            }
        }
    }

    public static final class JSProxyDispatcherPropertyGetNode extends LinkedPropertyGetNode {

        private final boolean propagateFloatingCondition;
        @Child private JSProxyPropertyGetNode proxyGet;

        @SuppressWarnings("unused")
        public JSProxyDispatcherPropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(key, receiverCheck);
            // (db) when the check node is used to check whether the object is JSProxy
            // (the object, not its prototype!) we can propagate the floating
            // condition down to avoid the 2nd class check
            this.propagateFloatingCondition = receiverCheck instanceof JSClassCheckNode;
            this.proxyGet = JSProxyPropertyGetNode.create(context);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return proxyGet.executeWithReceiver(receiverCheck.getStore(thisObj), receiver, propagateFloatingCondition && floatingCondition, key);
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(proxyGet.executeWithReceiver(receiverCheck.getStore(thisObj), receiver, propagateFloatingCondition && floatingCondition, key));
        }
    }

    public static final class JSProxyDispatcherRequiredPropertyGetNode extends LinkedPropertyGetNode {

        private final boolean propagateFloatingCondition;
        @Child private JSProxyPropertyGetNode proxyGet;
        @Child private JSProxyHasPropertyNode proxyHas;

        @SuppressWarnings("unused")
        public JSProxyDispatcherRequiredPropertyGetNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(key, receiverCheck);
            this.propagateFloatingCondition = receiverCheck instanceof JSClassCheckNode;
            this.proxyGet = JSProxyPropertyGetNode.create(context);
            this.proxyHas = JSProxyHasPropertyNode.create(context);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            DynamicObject proxy = receiverCheck.getStore(thisObj);
            if (proxyHas.executeWithTargetAndKeyBoolean(proxy, key)) {
                return proxyGet.executeWithReceiver(proxy, receiver, propagateFloatingCondition && floatingCondition, key);
            } else {
                throw Errors.createReferenceErrorNotDefined(key, this);
            }
        }

    }

    public static final class JSAdapterPropertyGetNode extends LinkedPropertyGetNode {
        private final boolean isMethod;

        public JSAdapterPropertyGetNode(Object key, ReceiverCheckNode receiverCheck, boolean isMethod) {
            super(key, receiverCheck);
            this.isMethod = isMethod;
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (isMethod) {
                return JSObject.getMethod((DynamicObject) thisObj, key);
            } else {
                return JSObject.get((DynamicObject) thisObj, key);
            }
        }
    }

    public static final class UnspecializedPropertyGetNode extends LinkedPropertyGetNode {
        public UnspecializedPropertyGetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return JSObject.get((DynamicObject) thisObj, key);
        }
    }

    public abstract static class TerminalPropertyGetNode extends PropertyGetNode {
        protected final JSContext context;
        @CompilationFinal private boolean isMethod;
        private final boolean getOwnProperty;

        public TerminalPropertyGetNode(Object key, JSContext context, boolean getOwnProperty) {
            super(key);
            this.context = context;
            this.getOwnProperty = getOwnProperty;
        }

        @Override
        protected final PropertyGetNode getNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected final void setNext(PropertyGetNode next) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected final Shape getShape() {
            return null;
        }

        @Override
        public final int getValueInt(Object obj, Object receiver) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(getValue(obj, receiver));
        }

        @Override
        public final double getValueDouble(Object obj, Object receiver) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(getValue(obj, receiver));
        }

        @Override
        public final boolean getValueBoolean(Object obj, Object receiver) throws UnexpectedResultException {
            return JSTypesGen.expectBoolean(getValue(obj, receiver));
        }

        @Override
        public final long getValueLong(Object obj, Object receiver) throws UnexpectedResultException {
            return JSTypesGen.expectLong(getValue(obj, receiver));
        }

        @Override
        protected boolean isMethod() {
            return isMethod;
        }

        @Override
        protected void setMethod() {
            CompilerAsserts.neverPartOfCompilation();
            this.isMethod = true;
        }

        @Override
        protected boolean isOwnProperty() {
            return this.getOwnProperty;
        }

        @Override
        public JSContext getContext() {
            return context;
        }
    }

    public static final class ForeignPropertyGetNode extends LinkedPropertyGetNode {

        @Child private Node isNull;
        @Child private Node foreignGet;
        @Child private Node hasSizeProperty;
        @Child private Node getSize;
        @Child private JSForeignToJSTypeNode toJSType;
        private final boolean isLength;
        private final boolean isMethod;
        private final boolean isGlobal;

        public ForeignPropertyGetNode(Object key, boolean isMethod, boolean isGlobal) {
            super(key, new ForeignLanguageCheckNode());
            this.toJSType = JSForeignToJSTypeNodeGen.create();
            this.isLength = key.equals(JSAbstractArray.LENGTH);
            this.isMethod = isMethod;
            this.isGlobal = isGlobal;
        }

        private Object foreignGet(TruffleObject thisObj) {
            if (isNull == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.isNull = insert(Message.IS_NULL.createNode());
            }
            if (ForeignAccess.sendIsNull(isNull, thisObj)) {
                throw Errors.createTypeErrorCannotGetProperty(key, thisObj, isMethod, this);
            }
            if (foreignGet == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.foreignGet = insert(Message.READ.createNode());
            }
            try {
                Object foreignResult = ForeignAccess.sendRead(foreignGet, thisObj, key);
                return toJSType.executeWithTarget(foreignResult);
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                return Undefined.instance;
            }
        }

        private Object getSize(TruffleObject thisObj) {
            if (getSize == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.getSize = insert(Message.GET_SIZE.createNode());
            }
            try {
                Object foreignResult = ForeignAccess.sendGetSize(getSize, thisObj);
                return toJSType.executeWithTarget(foreignResult);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, Message.GET_SIZE, this);
            }
        }

        private boolean hasSizeProperty(TruffleObject thisObj) {
            if (hasSizeProperty == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.hasSizeProperty = insert(Message.HAS_SIZE.createNode());
            }
            return ForeignAccess.sendHasSize(hasSizeProperty, thisObj);
        }

        @Override
        public Object getValueUnchecked(Object object, Object receiver, boolean floatingCondition) {
            TruffleObject thisObj = (TruffleObject) object;
            if (isMethod && !isGlobal) {
                return thisObj;
            }
            if (isLength && hasSizeProperty(thisObj)) {
                return getSize(thisObj);
            }
            return foreignGet(thisObj);
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static class GenericPropertyGetNode extends TerminalPropertyGetNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ForeignPropertyGetNode foreignGetNode;
        private final ConditionProfile isJSObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isForeignObject = ConditionProfile.createBinaryProfile();
        private final BranchProfile nullOrUndefinedBranch = BranchProfile.create();
        private final BranchProfile fallbackBranch = BranchProfile.create();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        public GenericPropertyGetNode(Object key, JSContext context, boolean isMethod, boolean getOwnProperty) {
            super(key, context, getOwnProperty);
            this.toObjectNode = JSToObjectNode.createToObjectNoCheck(context);
            if (isMethod) {
                setMethod();
            }
        }

        @Override
        public Object getValue(Object thisObj, Object receiver) {
            if (isJSObject.profile(JSObject.isJSObject(thisObj))) {
                return getPropertyFromJSObject(thisObj, receiver, (DynamicObject) thisObj);
            } else {
                if (isForeignObject.profile(JSGuards.isForeignObject(thisObj))) {
                    // a TruffleObject from another language
                    if (foreignGetNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        foreignGetNode = insert(new ForeignPropertyGetNode(key, isMethod(), isGlobal()));
                    }
                    return foreignGetNode.getValue(thisObj, receiver);
                } else {
                    // a primitive, or a Symbol
                    DynamicObject object = JSRuntime.expectJSObject(toObjectNode.executeTruffleObject(thisObj), notAJSObjectBranch);
                    return getPropertyFromJSObject(thisObj, receiver, object);
                }
            }
        }

        private Object getPropertyFromJSObject(Object thisObj, Object receiver, DynamicObject object) {
            if (key instanceof HiddenKey) {
                Object result = object.get(key);
                if (result != null) {
                    return result;
                } else {
                    fallbackBranch.enter();
                    return getFallback(object);
                }
            } else {
                JSClass jsclass = jsclassProfile.getJSClass(object);
                // 0. check for null or undefined
                if (jsclass == Null.NULL_CLASS) {
                    nullOrUndefinedBranch.enter();
                    throw Errors.createTypeErrorCannotGetProperty(key, thisObj, isMethod(), this);
                }

                // 1. try to get a JS property
                Object value = isMethod() ? jsclass.getMethodHelper(object, receiver, key) : jsclass.getHelper(object, receiver, key);
                if (value != null) {
                    return value;
                }

                // 2. try to call fallback handler or return undefined
                fallbackBranch.enter();
                return getNoSuchProperty(object);
            }
        }

        protected Object getNoSuchProperty(DynamicObject thisObj) {
            if (JSTruffleOptions.NashornExtensions && (!context.getNoSuchPropertyUnusedAssumption().isValid() || (isMethod() && !context.getNoSuchMethodUnusedAssumption().isValid()))) {
                return getNoSuchPropertySlow(thisObj);
            }
            return getFallback(thisObj);
        }

        @TruffleBoundary
        private Object getNoSuchPropertySlow(DynamicObject thisObj) {
            if (!(key instanceof Symbol) && JSRuntime.isObject(thisObj) && !JSAdapter.isJSAdapter(thisObj) && !JSProxy.isProxy(thisObj)) {
                if (isMethod()) {
                    Object function = JSObject.get(thisObj, JSObject.NO_SUCH_METHOD_NAME);
                    if (function != Undefined.instance) {
                        if (JSFunction.isJSFunction(function)) {
                            return callNoSuchHandler(thisObj, (DynamicObject) function, false);
                        } else {
                            return getFallback(thisObj);
                        }
                    }
                }
                Object function = JSObject.get(thisObj, JSObject.NO_SUCH_PROPERTY_NAME);
                if (JSFunction.isJSFunction(function)) {
                    return callNoSuchHandler(thisObj, (DynamicObject) function, true);
                }
            }
            return getFallback(thisObj);
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

        protected Object getFallback(@SuppressWarnings("unused") DynamicObject thisObj) {
            return Undefined.instance;
        }

        @Override
        protected void setPropertyAssumptionCheckEnabled(boolean value) {
        }

        @Override
        protected boolean isGlobal() {
            return false;
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static final class GenericRequiredPropertyGetNode extends GenericPropertyGetNode {

        public GenericRequiredPropertyGetNode(Object key, JSContext context, boolean isMethod, boolean getOwnProperty) {
            super(key, context, isMethod, getOwnProperty);
        }

        @Override
        protected Object getFallback(DynamicObject thisObj) {
            throw Errors.createReferenceErrorNotDefined(key, this);
        }

        @Override
        protected boolean isGlobal() {
            return true;
        }
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static final class UninitializedPropertyGetNode extends TerminalPropertyGetNode {
        private final boolean isGlobal;
        private boolean propertyAssumptionCheckEnabled = true;

        public UninitializedPropertyGetNode(Object key, boolean isGlobal, JSContext context, boolean getOwnProperty) {
            super(key, context, getOwnProperty);
            this.isGlobal = isGlobal;
        }

        @Override
        public Object getValue(Object thisObject, Object receiver) {
            return rewrite(context, thisObject, null).getValue(thisObject, receiver);
        }

        @Override
        protected boolean isGlobal() {
            return isGlobal;
        }

        @Override
        protected boolean isPropertyAssumptionCheckEnabled() {
            return propertyAssumptionCheckEnabled && context.isSingleRealm();
        }

        @Override
        protected void setPropertyAssumptionCheckEnabled(boolean value) {
            this.propertyAssumptionCheckEnabled = value;
        }
    }

    public static final class ArrayLengthPropertyGetNode extends LinkedPropertyGetNode {
        @Child private ArrayLengthReadNode arrayLengthRead;
        @CompilationFinal private boolean longLength;

        public ArrayLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            assert isArrayLengthProperty(property);
            this.arrayLengthRead = ArrayLengthReadNode.create();
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            if (!longLength) {
                try {
                    return arrayLengthRead.executeInt(receiverCheck.getStore(thisObj), floatingCondition);
                } catch (UnexpectedResultException e) {
                    longLength = true;
                    return e.getResult();
                }
            } else {
                return arrayLengthRead.executeDouble((DynamicObject) thisObj, floatingCondition);
            }
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) throws UnexpectedResultException {
            assert assertIsArray(thisObj);
            return arrayLengthRead.executeInt(receiverCheck.getStore(thisObj), floatingCondition);
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            assert assertIsArray(thisObj);
            return arrayLengthRead.executeDouble(receiverCheck.getStore(thisObj), floatingCondition);
        }

        private boolean assertIsArray(Object thisObj) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            // shape check should be sufficient to guarantee this assertion
            assert JSArray.isJSArray(store);
            return true;
        }
    }

    public static final class ClassPrototypePropertyGetNode extends LinkedPropertyGetNode {

        @CompilationFinal private DynamicObject constantFunction = Undefined.instance;
        @Child private CreateMethodPropertyNode setConstructor;
        @CompilationFinal private Shape generatorObjectShape;
        @CompilationFinal private Boolean generatorFunction;
        private final JSContext context;
        private final ConditionProfile prototypeInitializedProfile = ConditionProfile.createCountingProfile();

        public ClassPrototypePropertyGetNode(Property property, ReceiverCheckNode receiverCheck, JSContext context) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            this.context = context;
            assert isClassPrototypeProperty(property);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            DynamicObject functionObj = receiverCheck.getStore(thisObj);
            assert JSFunction.isJSFunction(functionObj);
            if (constantFunction == Undefined.instance) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                constantFunction = functionObj;
                // ensure `prototype` is initialized
                JSFunction.getClassPrototype(functionObj);
            }
            if (constantFunction != null) {
                if (constantFunction == functionObj) {
                    return JSFunction.getClassPrototypeInitialized(functionObj);
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    constantFunction = null;
                }
            }
            if (prototypeInitializedProfile.profile(JSFunction.isClassPrototypeInitialized(functionObj))) {
                return JSFunction.getClassPrototypeInitialized(functionObj);
            } else {
                return getPrototypeNotInitialized(functionObj);
            }
        }

        private Object getPrototypeNotInitialized(DynamicObject functionObj) {
            if (generatorFunction == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                generatorFunction = JSFunction.isGenerator(functionObj);
            }
            // Guaranteed by shape check, see JSFunction
            assert generatorFunction == JSFunction.isGenerator(functionObj);
            if (generatorFunction) {
                if (generatorObjectShape == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    generatorObjectShape = JSFunction.getRealm(functionObj).getInitialGeneratorObjectShape();
                }
                return JSObject.create(context, generatorObjectShape);
            } else {
                if (setConstructor == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setConstructor = insert(CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR));
                }
                DynamicObject prototype = JSUserObject.create(context);
                setConstructor.executeVoid(prototype, functionObj);
                JSFunction.setClassPrototype(functionObj, prototype);
                return prototype;
            }
        }
    }

    public static final class StringLengthPropertyGetNode extends LinkedPropertyGetNode {
        private final ValueProfile charSequenceClassProfile = ValueProfile.createClassProfile();

        public StringLengthPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            assert isStringLengthProperty(property);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) {
            CharSequence charSequence = JSString.getCharSequence(receiverCheck.getStore(thisObj));
            return charSequenceClassProfile.profile(charSequence).length();
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }
    }

    public static final class LazyRegexResultIndexPropertyGetNode extends LinkedPropertyGetNode {

        @Child private Node readStartArrayNode = TRegexUtil.createReadNode();
        @Child private Node readStartArrayElementNode = TRegexUtil.createReadNode();

        public LazyRegexResultIndexPropertyGetNode(Property property, ReceiverCheckNode receiverCheck) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isData(property);
            assert isLazyRegexResultIndexProperty(property);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }

        @Override
        public int getValueUncheckedInt(Object thisObj, Object receiver, boolean floatingCondition) {
            return TRegexUtil.readResultStartIndex(readStartArrayNode, readStartArrayElementNode, arrayGetRegexResult(receiverCheck.getStore(thisObj)), 0);
        }

        @Override
        public double getValueUncheckedDouble(Object thisObj, Object receiver, boolean floatingCondition) {
            return getValueUncheckedInt(thisObj, receiver, floatingCondition);
        }
    }

    public static final class LazyNamedCaptureGroupPropertyGetNode extends LinkedPropertyGetNode {

        private final int groupIndex;
        @Child private PropertyGetNode getResultNode;
        @Child private TRegexMaterializeResultNode materializeNode = TRegexMaterializeResultNode.create();

        public LazyNamedCaptureGroupPropertyGetNode(Property property, ReceiverCheckNode receiverCheck, JSContext context, int groupIndex) {
            super(property.getKey(), receiverCheck);
            assert isLazyNamedCaptureGroupProperty(property);
            this.groupIndex = groupIndex;
            this.getResultNode = PropertyGetNode.create(JSRegExp.GROUPS_RESULT_ID, false, context);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            TruffleObject regexResult = (TruffleObject) getResultNode.getValue(store);
            return materializeNode.materializeGroup(regexResult, groupIndex);
        }
    }

    public static final class MapPropertyGetNode extends LinkedPropertyGetNode {
        public MapPropertyGetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
        }

        @Override
        public Object getValueUnchecked(Object thisObj, Object receiver, boolean floatingCondition) {
            Map<?, ?> map = (Map<?, ?>) thisObj;
            Object value = Boundaries.mapGet(map, key);
            if (value != null) {
                return value;
            } else {
                return Null.instance;
            }
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected LinkedPropertyGetNode createCachedPropertyNode(Property property, Object thisObj, int depth, JSContext context, Object value) {
        assert !isOwnProperty() || depth == 0;
        if (!(JSObject.isDynamicObject(thisObj))) {
            return createCachedPropertyNodeNotJSObject(property, thisObj, depth, context);
        }

        Shape cacheShape = ((DynamicObject) thisObj).getShape();

        if (JSProperty.isData(property) && !JSProperty.isProxy(property) && (property.getLocation().isFinal() || property.getLocation().isAssumedFinal())) {
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
            PropertyGetNode existingNode = getCacheEntryByShape(cacheShape);
            boolean existingNodeIsFinal = existingNode instanceof FinalObjectPropertyGetNode || existingNode instanceof FinalDoublePropertyGetNode || existingNode instanceof FinalIntPropertyGetNode ||
                            existingNode instanceof FinalBooleanPropertyGetNode || existingNode instanceof FinalLongPropertyGetNode || existingNode instanceof AssumedFinalPropertyGetNode;
            // assert existingNode == null || existingNodeIsFinal : existingNode.getClass();

            boolean finalSpecializationAllowed = existingNode == null || ((LinkedPropertyGetNode) existingNode).receiverCheck instanceof ConstantObjectPrototypeShapeCheckNode ||
                            ((LinkedPropertyGetNode) existingNode).receiverCheck instanceof ConstantObjectPrototypeChainShapeCheckNode;

            if (existingNode != null && existingNodeIsFinal) {
                // evict existing createNode from cache
                ((LinkedPropertyGetNode) existingNode).rewrite("evict existing cache node");
            }

            boolean isConstantObjectFinal = existingNode == null;
            if (finalSpecializationAllowed && isEligibleForFinalSpecialization(cacheShape, (DynamicObject) thisObj, depth, isConstantObjectFinal)) {
                return createFinalSpecialization(property, cacheShape, (DynamicObject) thisObj, depth, isConstantObjectFinal);
            }
        }

        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, (DynamicObject) thisObj, depth, false, false);
        if (JSProperty.isData(property)) {
            return createSpecializationFromDataProperty(property, shapeCheck, context);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertyGetNode(property, shapeCheck);
        }
    }

    private boolean isEligibleForFinalSpecialization(Shape cacheShape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        /*
         * NB: We need to check whether the property assumption of the store is valid, even if we do
         * not actually check the assumption in the specialization but check the shape instead (note
         * that we always check the expected object instance, too, either directly (depth 0) or
         * indirectly (prototype derived through shape or property assumption for depth >= 1)). This
         * is because we cannot guarantee a final location value to be constant for (object, shape)
         * anymore once the assumption has been invalidated. Namely, one could remove and re-add a
         * property without invalidating its finality. Perhaps we should invalidate the finality of
         * removed properties. For now, we have to be conservative.
         */
        return depth >= 1 ? (prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal))
                        : (JSTruffleOptions.SkipFinalShapeCheck && isPropertyAssumptionCheckEnabled() && JSShape.getPropertyAssumption(cacheShape, key).isValid());
    }

    private LinkedPropertyGetNode createCachedPropertyNodeNotJSObject(Property property, Object thisObj, int depth, JSContext context) {
        final ReceiverCheckNode receiverCheck;
        if (depth == 0) {
            if (JSTruffleOptions.NashornJavaInterop && JSTruffleOptions.NashornCompatibilityMode && isMethod() && JSRuntime.isJSPrimitive(thisObj)) {
                // This hack ensures we get the Java method instead of the JavaScript property
                // for length in s.length() where s is a java.lang.String. Required by Nashorn.
                // We do this only for depth 0, because JavaScript prototype functions in turn
                // are preferred over Java methods with the same name.
                LinkedPropertyGetNode javaPropertyNode = createJavaPropertyNodeMaybe(thisObj, context);
                if (javaPropertyNode != null) {
                    return javaPropertyNode;
                }
            }

            receiverCheck = new InstanceofCheckNode(thisObj.getClass(), context);
        } else {
            receiverCheck = createPrimitiveReceiverCheck(thisObj, depth, context);
        }

        if (JSProperty.isData(property)) {
            return createSpecializationFromDataProperty(property, receiverCheck, context);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertyGetNode(property, receiverCheck);
        }
    }

    private static LinkedPropertyGetNode createSpecializationFromDataProperty(Property property, ReceiverCheckNode receiverCheck, JSContext context) {
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
            } else if (isClassPrototypeProperty(property)) {
                return new ClassPrototypePropertyGetNode(dataProperty, receiverCheck, context);
            } else if (isStringLengthProperty(property)) {
                return new StringLengthPropertyGetNode(dataProperty, receiverCheck);
            } else if (isLazyRegexResultIndexProperty(property)) {
                return new LazyRegexResultIndexPropertyGetNode(dataProperty, receiverCheck);
            } else if (isLazyNamedCaptureGroupProperty(property)) {
                int groupIndex = ((JSRegExp.LazyNamedCaptureGroupProperty) JSProperty.getConstantProxy(property)).getGroupIndex();
                return new LazyNamedCaptureGroupPropertyGetNode(dataProperty, receiverCheck, context, groupIndex);
            } else {
                return new ObjectPropertyGetNode(dataProperty, receiverCheck);
            }
        }
    }

    private LinkedPropertyGetNode createFinalSpecialization(Property property, Shape cacheShape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        AbstractShapeCheckNode finalShapeCheckNode = createShapeCheckNode(cacheShape, thisObj, depth, isConstantObjectFinal, false);
        finalShapeCheckNode.adoptChildren();
        DynamicObject store = finalShapeCheckNode.getStore(thisObj);

        if (property.getLocation().isFinal()) {
            return createFinalSpecializationImpl(property, finalShapeCheckNode, store);
        } else {
            assert property.getLocation().isAssumedFinal();
            return new AssumedFinalPropertyGetNode(property, finalShapeCheckNode, createFinalSpecializationImpl(property, null, store));
        }
    }

    private static LinkedPropertyGetNode createFinalSpecializationImpl(Property property, AbstractShapeCheckNode shapeCheckNode, DynamicObject store) {
        if (property.getLocation() instanceof IntLocation) {
            return new FinalIntPropertyGetNode(property, shapeCheckNode, ((IntLocation) property.getLocation()).getInt(store, false));
        } else if (property.getLocation() instanceof DoubleLocation) {
            return new FinalDoublePropertyGetNode(property, shapeCheckNode, ((DoubleLocation) property.getLocation()).getDouble(store, false));
        } else if (property.getLocation() instanceof BooleanLocation) {
            return new FinalBooleanPropertyGetNode(property, shapeCheckNode, ((BooleanLocation) property.getLocation()).getBoolean(store, false));
        } else if (property.getLocation() instanceof LongLocation) {
            return new FinalLongPropertyGetNode(property, shapeCheckNode, ((LongLocation) property.getLocation()).getLong(store, false));
        } else {
            assert property.getLocation() instanceof ObjectLocation;
            return new FinalObjectPropertyGetNode(property, shapeCheckNode, property.get(store, false));
        }
    }

    private PropertyGetNode getCacheEntryByShape(Shape theShape) {
        assert this instanceof UninitializedPropertyGetNode;
        PropertyGetNode cur = this;
        while (cur.getParent() instanceof PropertyGetNode) {
            cur = (PropertyGetNode) cur.getParent();
            if (cur.getShape() == theShape) {
                return cur;
            }
        }
        return null;
    }

    @Override
    protected LinkedPropertyGetNode createJavaPropertyNodeMaybe(Object thisObj, JSContext context) {
        if (JSTruffleOptions.SubstrateVM) {
            return null;
        }
        if (JSObject.isDynamicObject(thisObj)) {
            if (JavaPackage.isJavaPackage(thisObj)) {
                return new CachedJavaPackagePropertyGetNode(context, key, new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)), (DynamicObject) thisObj);
            }
        }
        if (!JSTruffleOptions.NashornJavaInterop) {
            return null;
        }
        return createJavaPropertyNodeMaybe0(thisObj, context);
    }

    /* In a separate method for Substrate VM support. */
    private LinkedPropertyGetNode createJavaPropertyNodeMaybe0(Object thisObj, JSContext context) {
        if (JSObject.isDynamicObject(thisObj)) {
            if (JSJavaWrapper.isJSJavaWrapper(thisObj)) {
                return new JSJavaWrapperPropertyGetNode(key, isGlobal(), isMethod(), isOwnProperty(), context);
            } else if (JavaPackage.isJavaPackage(thisObj)) {
                return new CachedJavaPackagePropertyGetNode(context, key, new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)), (DynamicObject) thisObj);
            } else if (JavaImporter.isJavaImporter(thisObj)) {
                return new UnspecializedPropertyGetNode(key, new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)));
            } else {
                return null;
            }
        } else if (thisObj instanceof JavaClass) {
            return new CachedJavaClassPropertyGetNode(key, new InstanceofCheckNode(JavaClass.class, context), isMethod(), JSJavaWrapper.isClassFilterPresent(context), (JavaClass) thisObj);
        } else if (thisObj instanceof JavaSuperAdapter) {
            return new CachedJavaSuperMethodPropertyGetNode(key, new JavaSuperAdapterCheckNode((JavaSuperAdapter) thisObj), JSJavaWrapper.isClassFilterPresent(context), (JavaSuperAdapter) thisObj);
        } else {
            JavaMember member = getInstanceMember(thisObj, context);
            if (member != null) {
                if (member instanceof JavaGetter) {
                    return new JavaGetterPropertyGetNode(key, new InstanceofCheckNode(thisObj.getClass(), context), (JavaGetter) member);
                } else {
                    assert member instanceof JavaMethod;
                    return new JavaMethodPropertyGetNode(key, new InstanceofCheckNode(thisObj.getClass(), context), (JavaMethod) member);
                }
            } else {
                if (thisObj instanceof Map) {
                    return new MapPropertyGetNode(key, new InstanceofCheckNode(thisObj.getClass(), context));
                }
            }
            return null;
        }
    }

    @Override
    protected LinkedPropertyGetNode createUndefinedPropertyNode(Object thisObj, Object store, int depth, JSContext context, Object value) {
        LinkedPropertyGetNode javaPropertyNode = createJavaPropertyNodeMaybe(thisObj, context);
        if (javaPropertyNode != null) {
            return javaPropertyNode;
        }

        if (JSObject.isDynamicObject(thisObj)) {
            DynamicObject jsobject = (DynamicObject) thisObj;
            Shape cacheShape = jsobject.getShape();
            AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, jsobject, depth, false, false);
            ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(jsobject)) : shapeCheck;
            if (JSAdapter.isJSAdapter(store)) {
                return new JSAdapterPropertyGetNode(key, receiverCheck, isMethod());
            } else if (JSProxy.isProxy(store) && !(key instanceof HiddenKey)) {
                if (isRequired()) {
                    return new JSProxyDispatcherRequiredPropertyGetNode(context, key, receiverCheck, isMethod());
                } else {
                    return new JSProxyDispatcherPropertyGetNode(context, key, receiverCheck, isMethod());
                }
            } else if (JSModuleNamespace.isJSModuleNamespace(store)) {
                return new UnspecializedPropertyGetNode(key, receiverCheck);
            } else {
                return createUndefinedJSObjectPropertyNode(depth, context, jsobject);
            }
        } else if (JSProxy.isProxy(store)) {
            ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth, context);
            return new JSProxyDispatcherPropertyGetNode(context, key, receiverCheck, isMethod());
        } else {
            if (thisObj == null) {
                return new TypeErrorPropertyGetNode(key, new NullCheckNode(), isMethod());
            } else {
                ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth, context);
                return createUndefinedOrErrorPropertyNode(receiverCheck);
            }
        }
    }

    private LinkedPropertyGetNode createUndefinedJSObjectPropertyNode(int depth, JSContext context, DynamicObject jsobject) {
        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(jsobject.getShape(), jsobject, depth, false, false);
        if (JSRuntime.isObject(jsobject)) {
            if (JSTruffleOptions.NashornExtensions && !(key instanceof Symbol)) {
                if ((!context.getNoSuchMethodUnusedAssumption().isValid() && JSObject.hasProperty(jsobject, JSObject.NO_SUCH_METHOD_NAME)) ||
                                (!context.getNoSuchPropertyUnusedAssumption().isValid() && JSObject.hasProperty(jsobject, JSObject.NO_SUCH_PROPERTY_NAME))) {
                    return new CheckNoSuchPropertyNode(key, shapeCheck, isGlobal(), context);
                }
            }
            return createUndefinedOrErrorPropertyNode(shapeCheck);
        } else {
            return new TypeErrorPropertyGetNode(key, shapeCheck, isMethod());
        }
    }

    private LinkedPropertyGetNode createUndefinedOrErrorPropertyNode(ReceiverCheckNode receiverCheck) {
        if (isRequired()) {
            return new UndefinedPropertyErrorNode(key, receiverCheck);
        } else {
            return new UndefinedPropertyGetNode(key, receiverCheck);
        }
    }

    private JavaMember getInstanceMember(Object thisObj, JSContext context) {
        if (thisObj == null) {
            return null;
        }
        if (!(key instanceof String)) {
            // could be Symbol!
            return null;
        }
        JavaClass javaClass = JavaClass.forClass(thisObj.getClass());
        return javaClass.getMember((String) key, JavaClass.INSTANCE, getJavaMemberTypes(isMethod()), JSJavaWrapper.isClassFilterPresent(context));
    }

    /**
     * Make a generic-case createNode, for when polymorphism becomes too high.
     */
    @Override
    protected PropertyGetNode createGenericPropertyNode(JSContext context) {
        return createGeneric(key, isRequired(), isMethod(), isOwnProperty(), context);
    }

    private static PropertyGetNode createGeneric(Object key, boolean required, boolean isMethod, boolean getOwnProperty, JSContext context) {
        return required ? new GenericRequiredPropertyGetNode(key, context, isMethod, getOwnProperty) : new GenericPropertyGetNode(key, context, isMethod, getOwnProperty);
    }

    protected final boolean isRequired() {
        return isGlobal();
    }

    protected abstract boolean isMethod();

    protected void setMethod() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Class<PropertyGetNode> getBaseClass() {
        return PropertyGetNode.class;
    }

    @Override
    protected Class<? extends PropertyGetNode> getUninitializedNodeClass() {
        return UninitializedPropertyGetNode.class;
    }

    @Override
    protected PropertyGetNode createTruffleObjectPropertyNode(TruffleObject thisObject, JSContext context) {
        return new ForeignPropertyGetNode(key, isMethod(), isGlobal());
    }

    protected static Class<? extends JavaMember>[] getJavaMemberTypes(boolean isMethod) {
        return isMethod ? JavaClass.METHOD_GETTER : JavaClass.GETTER_METHOD;
    }

    @Override
    public abstract JSContext getContext();
}
