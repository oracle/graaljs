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

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.IntLocation;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.NeverValidAssumption;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.nodes.cast.AsDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.interop.Converters;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaAccess;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMember;
import com.oracle.truffle.js.runtime.interop.JavaSetter;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * @see WritePropertyNode
 */
public abstract class PropertySetNode extends PropertyCacheNode<PropertySetNode> {

    public static PropertySetNode create(Object key, boolean isGlobal, JSContext context, boolean isStrict) {
        final boolean setOwnProperty = false;
        return createImpl(key, isGlobal, context, isStrict, setOwnProperty, JSAttributes.getDefault());
    }

    public static PropertySetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean isStrict, boolean setOwnProperty, int attributeFlags) {
        if (JSTruffleOptions.PropertyCacheLimit > 0) {
            return new UninitializedPropertySetNode(key, isGlobal, context, isStrict, setOwnProperty, attributeFlags);
        } else {
            return new GenericPropertySetNode(key, isGlobal, isStrict, setOwnProperty, context);
        }
    }

    public static PropertySetNode createSetHidden(HiddenKey key, JSContext context) {
        return createImpl(key, false, context, false, true, 0);
    }

    protected PropertySetNode(Object key) {
        super(key);
    }

    public final void setValue(Object obj, Object value) {
        setValue(obj, value, obj);
    }

    public final void setValueInt(Object obj, int value) {
        setValueInt(obj, value, obj);
    }

    public final void setValueDouble(Object obj, double value) {
        setValueDouble(obj, value, obj);
    }

    public final void setValueBoolean(Object obj, boolean value) {
        setValueBoolean(obj, value, obj);
    }

    protected abstract void setValue(Object thisObj, Object value, Object receiver);

    protected abstract void setValueInt(Object thisObj, int value, Object receiver);

    protected abstract void setValueDouble(Object thisObj, double value, Object receiver);

    protected abstract void setValueBoolean(Object thisObj, boolean value, Object receiver);

    public abstract static class LinkedPropertySetNode extends PropertySetNode {
        @Child protected PropertySetNode next;
        @Child protected ReceiverCheckNode receiverCheck;

        public LinkedPropertySetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key);
            this.receiverCheck = receiverCheck;
        }

        @Override
        public NodeCost getCost() {
            if (next != null && next.getCost() == NodeCost.MONOMORPHIC) {
                return NodeCost.POLYMORPHIC;
            }
            return super.getCost();
        }

        @Override
        public final void setValue(Object thisObj, Object value, Object receiver) {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    setValueUnchecked(thisObj, value, receiver, condition);
                } else {
                    next.setValue(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValue(thisObj, value, receiver);
            }
        }

        public abstract void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition);

        @Override
        public final void setValueInt(Object thisObj, int value, Object receiver) {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    setValueUncheckedInt(thisObj, value, receiver, condition);
                } else {
                    next.setValueInt(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueInt(thisObj, value, receiver);
            }
        }

        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            setValueUnchecked(thisObj, value, receiver, condition);
        }

        @Override
        public final void setValueDouble(Object thisObj, double value, Object receiver) {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    setValueUncheckedDouble(thisObj, value, receiver, condition);
                } else {
                    next.setValueDouble(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueDouble(thisObj, value, receiver);
            }
        }

        public void setValueUncheckedDouble(Object thisObj, double value, Object receiver, boolean condition) {
            setValueUnchecked(thisObj, value, receiver, condition);
        }

        @Override
        public final void setValueBoolean(Object thisObj, boolean value, Object receiver) {
            try {
                boolean condition = receiverCheck.accept(thisObj);
                if (condition) {
                    setValueUncheckedBoolean(thisObj, value, receiver, condition);
                } else {
                    next.setValueBoolean(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueBoolean(thisObj, value, receiver);
            }
        }

        public void setValueUncheckedBoolean(Object thisObj, boolean value, Object receiver, boolean condition) {
            setValueUnchecked(thisObj, value, receiver, condition);
        }

        protected PropertySetNode rewrite(CharSequence reason) {
            CompilerAsserts.neverPartOfCompilation();
            assert next != null;
            PropertySetNode replacedNext = replace(next, reason);
            return replacedNext;
        }

        protected static CharSequence asReason(Exception e) {
            return e.getClass().getName();
        }

        protected CharSequence reasonAssumptionInvalidated() {
            return reasonShapeAssumptionInvalidated(key);
        }

        @Override
        protected final Shape getShape() {
            return receiverCheck.getShape();
        }

        @Override
        public final PropertySetNode getNext() {
            return next;
        }

        @Override
        protected final void setNext(PropertySetNode to) {
            next = to;
        }

        @Override
        @TruffleBoundary
        public String debugString() {
            return getClass().getSimpleName() + "<property=" + key + ",shape=" + getShape() + ">\n" + ((next == null) ? "" : next.debugString());
        }

        @Override
        public JSContext getContext() {
            return getNext().getContext();
        }

        @Override
        protected boolean isStrict() {
            return getNext().isStrict();
        }

        @Override
        protected boolean isGlobal() {
            return getNext().isGlobal();
        }

        @Override
        protected boolean isOwnProperty() {
            return getNext().isOwnProperty();
        }
    }

    public static final class ObjectPropertySetNode extends LinkedPropertySetNode {
        private final Property property;
        private final boolean isStrict;

        public ObjectPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean isStrict) {
            super(property.getKey(), shapeCheck);
            this.property = property;
            this.isStrict = isStrict;
            assert JSProperty.isData(property);
            assert JSProperty.isWritable(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            try {
                JSProperty.setValueThrow(property, receiverCheck.getStore(thisObj), thisObj, value, getShape(), isStrict);
            } catch (IncompatibleLocationException | FinalLocationException e) {
                rewrite(asReason(e)).setValue(thisObj, value, receiver);
            }
        }
    }

    public static final class IntPropertySetNode extends LinkedPropertySetNode {

        private final Property property;
        private final IntLocation location;

        public IntPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(property.getKey(), shapeCheck);
            this.property = property;
            this.location = (IntLocation) property.getLocation();
            assert JSProperty.isData(property);
            assert JSProperty.isWritable(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                if (value instanceof Integer) {
                    property.set(receiverCheck.getStore(thisObj), value, getShape());
                    return;
                } else {
                    reason = "not int";
                }
            } catch (IncompatibleLocationException | FinalLocationException e) {
                reason = asReason(e);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            try {
                location.setInt(receiverCheck.getStore(thisObj), value, getShape());
            } catch (FinalLocationException e) {
                rewrite(asReason(e)).setValueInt(thisObj, value, receiver);
            }
        }
    }

    public static final class DoublePropertySetNode extends LinkedPropertySetNode {
        private final DoubleLocation location;
        @Child private AsDoubleNode asDoubleNode;

        public DoublePropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(property.getKey(), shapeCheck);
            this.location = (DoubleLocation) property.getLocation();
            this.asDoubleNode = AsDoubleNode.create();
            assert JSProperty.isData(property);
            assert JSProperty.isWritable(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                double doubleValue = asDoubleNode.executeDouble(value);
                location.setDouble(receiverCheck.getStore(thisObj), doubleValue, getShape());
                return;
            } catch (UnexpectedResultException e) {
                reason = "not number";
            } catch (FinalLocationException e) {
                reason = asReason(e);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            try {
                location.setDouble(receiverCheck.getStore(thisObj), value, getShape());
            } catch (FinalLocationException e) {
                rewrite(asReason(e)).setValueInt(thisObj, value, receiver);
            }
        }

        @Override
        public void setValueUncheckedDouble(Object thisObj, double value, Object receiver, boolean condition) {
            try {
                location.setDouble(receiverCheck.getStore(thisObj), value, getShape());
            } catch (FinalLocationException e) {
                rewrite(asReason(e)).setValueDouble(thisObj, value, receiver);
            }
        }
    }

    public static final class BooleanPropertySetNode extends LinkedPropertySetNode {

        private final Property property;
        private final BooleanLocation location;

        public BooleanPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(property.getKey(), shapeCheck);
            this.property = property;
            this.location = (BooleanLocation) property.getLocation();
            assert JSProperty.isData(property);
            assert JSProperty.isWritable(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                if (value instanceof Boolean) {
                    property.set(receiverCheck.getStore(thisObj), value, getShape());
                    return;
                } else {
                    reason = "not boolean";
                }
            } catch (IncompatibleLocationException | FinalLocationException e) {
                reason = asReason(e);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedBoolean(Object thisObj, boolean value, Object receiver, boolean condition) {
            try {
                location.setBoolean(receiverCheck.getStore(thisObj), value, getShape());
            } catch (FinalLocationException e) {
                rewrite(asReason(e)).setValueBoolean(thisObj, value, receiver);
            }
        }
    }

    public static final class AccessorPropertySetNode extends LinkedPropertySetNode {
        private final Property property;
        private final boolean isStrict;
        @Child private JSFunctionCallNode callNode;
        private final BranchProfile undefinedSetterBranch = BranchProfile.create();

        public AccessorPropertySetNode(Property property, ReceiverCheckNode receiverCheck, boolean isStrict) {
            super(property.getKey(), receiverCheck);
            assert JSProperty.isAccessor(property);
            this.property = property;
            this.isStrict = isStrict;
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Accessor accessor = (Accessor) property.get(store, condition);

            DynamicObject setter = accessor.getSetter();
            if (setter != Undefined.instance) {
                callNode.executeCall(JSArguments.createOneArg(receiver, setter, value));
            } else {
                undefinedSetterBranch.enter();
                if (isStrict) {
                    throw Errors.createTypeErrorCannotSetAccessorProperty(key, store);
                }
            }
        }
    }

    /**
     * For use when a property is undefined. Adds a new property.
     */
    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static class UninitializedDefinePropertyNode extends LinkedPropertySetNode {
        private final int attributeFlags;
        private final JSContext context;

        public UninitializedDefinePropertyNode(Object key, ReceiverCheckNode receiverCheck, JSContext context, int attributeFlags) {
            super(key, receiverCheck);
            this.context = context;
            this.attributeFlags = attributeFlags;
        }

        public UninitializedDefinePropertyNode(Object key, Shape shape, JSContext context, int attributeFlags) {
            this(key, new ShapeCheckNode(shape), context, attributeFlags);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert receiverCheck.getShape().isValid();
            LinkedPropertySetNode resolved = createDefinePropertyNode(key, receiverCheck, value, context, attributeFlags);
            resolved.setNext(next);
            replace(resolved);
            resolved.setValue(thisObj, value, receiver);
        }
    }

    public abstract static class DefinePropertyNode extends LinkedPropertySetNode {

        protected final Shape newShape;
        protected final Property property;
        private final Assumption newShapeNotObsoletedAssumption;
        private final JSContext context;

        public DefinePropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, JSContext context, Property property) {
            super(key, receiverCheck);
            this.newShape = newShape;
            this.newShapeNotObsoletedAssumption = newShape.isValid() ? newShape.getValidAssumption() : NeverValidAssumption.INSTANCE;
            this.property = property;
            this.context = context;
            assert property.getKey().equals(key) : "property=" + property + " key=" + key;
            assert property == newShape.getProperty(key);
        }

        protected final void setValueGeneralize(Object thisObj, Object value, Object receiver) {
            CompilerAsserts.neverPartOfCompilation();
            LinkedPropertySetNode specialized = createRedefinePropertyNode(key, receiverCheck, newShape, property, value, context);
            Lock lock = getLock();
            lock.lock();
            try {
                if (newShape.isValid()) {
                    specialized.setNext(this);
                    this.replace(specialized, "widen type");
                } else {
                    specialized.setNext(next);
                    this.replace(specialized, "widen type");
                }
            } finally {
                lock.unlock();
            }
            specialized.setValue(thisObj, value, receiver);
        }

        @Override
        protected UninitializedDefinePropertyNode rewrite(CharSequence reason) {
            CompilerAsserts.neverPartOfCompilation();
            UninitializedDefinePropertyNode rep = new UninitializedDefinePropertyNode(key, receiverCheck, context, getAttributeFlags());
            rep.setNext(next);
            replace(rep, reason);
            return rep;
        }

        @Override
        protected final int getAttributeFlags() {
            return property.getFlags() & JSAttributes.ATTRIBUTES_MASK;
        }

        protected final boolean isNewShapeValid() {
            return newShapeNotObsoletedAssumption != NeverValidAssumption.INSTANCE;
        }

        protected final void checkNewShapeNotObsolete() throws InvalidAssumptionException {
            if (isNewShapeValid()) {
                newShapeNotObsoletedAssumption.check();
            }
        }

        protected final void maybeUpdateShape(DynamicObject store) {
            if (!isNewShapeValid()) {
                updateShape(store);
            }
        }

        @TruffleBoundary
        private static void updateShape(DynamicObject store) {
            store.updateShape();
        }
    }

    public static final class DefineIntPropertyNode extends DefinePropertyNode {
        private final IntLocation location;

        public DefineIntPropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, JSContext context, Property property) {
            super(key, receiverCheck, newShape, context, property);
            this.location = (IntLocation) property.getLocation();
            assert JSProperty.isData(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                checkNewShapeNotObsolete();
                if (value instanceof Integer) {
                    DynamicObject store = JSObject.castJSObject(thisObj);
                    location.setInt(store, (int) value, receiverCheck.getShape(), newShape);
                    maybeUpdateShape(store);
                    return;
                } else {
                    reason = "not int";
                }
            } catch (InvalidAssumptionException e) {
                reason = reasonAssumptionInvalidated();
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            try {
                checkNewShapeNotObsolete();
                DynamicObject store = JSObject.castJSObject(thisObj);
                location.setInt(store, value, receiverCheck.getShape(), newShape);
                maybeUpdateShape(store);
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueInt(thisObj, value, receiver);
            }
        }
    }

    public static final class DefineBooleanPropertyNode extends DefinePropertyNode {
        private final BooleanLocation location;

        public DefineBooleanPropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, JSContext context, Property property) {
            super(key, receiverCheck, newShape, context, property);
            this.location = (BooleanLocation) property.getLocation();
            assert JSProperty.isData(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                checkNewShapeNotObsolete();
                if (value instanceof Boolean) {
                    DynamicObject store = JSObject.castJSObject(thisObj);
                    location.setBoolean(store, (boolean) value, receiverCheck.getShape(), newShape);
                    maybeUpdateShape(store);
                    return;
                } else {
                    reason = "not boolean";
                }
            } catch (InvalidAssumptionException e) {
                reason = reasonAssumptionInvalidated();
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedBoolean(Object thisObj, boolean value, Object receiver, boolean condition) {
            try {
                checkNewShapeNotObsolete();
                DynamicObject store = JSObject.castJSObject(thisObj);
                location.setBoolean(store, value, receiverCheck.getShape(), newShape);
                maybeUpdateShape(store);
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueBoolean(thisObj, value, receiver);
            }
        }
    }

    public static final class DefineDoublePropertyNode extends DefinePropertyNode {
        private final DoubleLocation location;
        @Child private AsDoubleNode asDoubleNode;

        public DefineDoublePropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newCache, JSContext context, Property property) {
            super(key, receiverCheck, newCache, context, property);
            this.location = (DoubleLocation) property.getLocation();
            assert JSProperty.isData(property);
            this.asDoubleNode = AsDoubleNode.create();
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            CharSequence reason;
            try {
                checkNewShapeNotObsolete();
                DynamicObject store = JSObject.castJSObject(thisObj);
                double doubleValue = asDoubleNode.executeDouble(value);
                location.setDouble(store, doubleValue, receiverCheck.getShape(), newShape);
                maybeUpdateShape(store);
                return;
            } catch (UnexpectedResultException e) {
                reason = "not number";
            } catch (InvalidAssumptionException e) {
                reason = reasonAssumptionInvalidated();
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            rewrite(reason).setValue(thisObj, value, receiver);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            try {
                checkNewShapeNotObsolete();
                DynamicObject store = JSObject.castJSObject(thisObj);
                location.setDouble(store, value, receiverCheck.getShape(), newShape);
                maybeUpdateShape(store);
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueInt(thisObj, value, receiver);
            }
        }

        @Override
        public void setValueUncheckedDouble(Object thisObj, double value, Object receiver, boolean condition) {
            try {
                checkNewShapeNotObsolete();
                DynamicObject store = JSObject.castJSObject(thisObj);
                location.setDouble(store, value, receiverCheck.getShape(), newShape);
                maybeUpdateShape(store);
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValueDouble(thisObj, value, receiver);
            }
        }
    }

    public static final class DefineObjectPropertyNode extends DefinePropertyNode {

        public DefineObjectPropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, JSContext context, Property property) {
            super(key, receiverCheck, newShape, context, property);
            assert JSProperty.isData(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            try {
                checkNewShapeNotObsolete();
                if (property.getLocation().canStore(value)) {
                    DynamicObject store = JSObject.castJSObject(thisObj);
                    property.getLocation().set(store, value, receiverCheck.getShape(), newShape);
                    maybeUpdateShape(store);
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setValueGeneralize(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValue(thisObj, value, receiver);
            } catch (IncompatibleLocationException e) {
                throw new IllegalStateException();
            }
        }
    }

    public static final class RedefineObjectPropertyNode extends LinkedPropertySetNode {
        private final Property property;
        private final Shape newShape;
        private final Assumption newShapeNotObsoletedAssumption;

        public RedefineObjectPropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, Property property) {
            super(key, receiverCheck);
            this.property = property;
            this.newShape = newShape;
            this.newShapeNotObsoletedAssumption = newShape.getValidAssumption();
            assert JSProperty.isData(property);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            try {
                newShapeNotObsoletedAssumption.check();
                if (property.getLocation().canStore(value)) {
                    property.setSafe(JSObject.castJSObject(thisObj), value, receiverCheck.getShape(), newShape);
                } else {
                    next.setValue(thisObj, value, receiver);
                }
            } catch (InvalidAssumptionException e) {
                rewrite(reasonAssumptionInvalidated()).setValue(thisObj, value, receiver);
            }
        }
    }

    public static final class ReadOnlyPropertySetNode extends LinkedPropertySetNode {
        private final boolean isStrict;

        public ReadOnlyPropertySetNode(Object key, ReceiverCheckNode receiverCheck, boolean isStrict) {
            super(key, receiverCheck);
            this.isStrict = isStrict;
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            if (isStrict) {
                throw Errors.createTypeErrorNotWritableProperty(key, thisObj);
            }
        }
    }

    public static final class JavaStaticFieldPropertySetNode extends LinkedPropertySetNode {
        private final boolean allowReflection;

        public JavaStaticFieldPropertySetNode(Object key, ReceiverCheckNode receiverCheck, boolean allowReflection) {
            super(key, receiverCheck);
            this.allowReflection = allowReflection;
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            JavaClass type = (JavaClass) thisObj;
            JavaMember member = type.getMember((String) key, JavaClass.STATIC, JavaClass.SETTER, allowReflection);
            if (member instanceof JavaSetter) {
                ((JavaSetter) member).setValue(null, value);
            }
        }
    }

    public static final class JavaSetterPropertySetNode extends LinkedPropertySetNode {
        @Child private JSFunctionCallNode.JavaMethodCallNode methodCall;

        public JavaSetterPropertySetNode(Object key, ReceiverCheckNode receiverCheck, JavaSetter setter) {
            super(key, receiverCheck);
            this.methodCall = JSFunctionCallNode.JavaMethodCallNode.create(setter);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            methodCall.executeCall(JSArguments.createOneArg(thisObj, null, value));
        }
    }

    /**
     * If object is undefined or null, throw TypeError.
     */
    public static final class TypeErrorPropertySetNode extends LinkedPropertySetNode {

        public TypeErrorPropertySetNode(Object key, AbstractShapeCheckNode shapeCheckNode) {
            super(key, shapeCheckNode);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            assert thisObj == Undefined.instance || thisObj == Null.instance;
            setValueUncheckedIntl(thisObj);
        }

        @TruffleBoundary
        private void setValueUncheckedIntl(Object thisObj) {
            throw Errors.createTypeErrorCannotSetProperty(getKey(), thisObj, this);
        }
    }

    /**
     * If object is the global object and we are in strict mode, throw ReferenceError.
     */
    public static final class ReferenceErrorPropertySetNode extends LinkedPropertySetNode {

        public ReferenceErrorPropertySetNode(Object key, AbstractShapeCheckNode shapeCheckNode) {
            super(key, shapeCheckNode);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            globalPropertySetInStrictMode(thisObj);
        }
    }

    public static final class JSAdapterPropertySetNode extends LinkedPropertySetNode {
        private final boolean isStrict;

        public JSAdapterPropertySetNode(Object key, ReceiverCheckNode receiverCheckNode, boolean isStrict) {
            super(key, receiverCheckNode);
            this.isStrict = isStrict;
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            JSObject.set((DynamicObject) thisObj, key, value, isStrict);
        }
    }

    public static final class JSProxyDispatcherPropertySetNode extends LinkedPropertySetNode {

        private final boolean propagateFloatingCondition;
        @Child private JSProxyPropertySetNode proxySet;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        public JSProxyDispatcherPropertySetNode(JSContext context, Object key, ReceiverCheckNode receiverCheckNode, boolean isStrict) {
            super(key, receiverCheckNode);
            this.proxySet = JSProxyPropertySetNode.create(context, isStrict);
            this.toPropertyKeyNode = JSToPropertyKeyNode.create();
            this.propagateFloatingCondition = receiverCheck instanceof JSClassCheckNode;
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            proxySet.executeWithReceiverAndValue(receiverCheck.getStore(thisObj), receiver, value, toPropertyKeyNode.execute(key), propagateFloatingCondition && condition);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            proxySet.executeWithReceiverAndValueInt(receiverCheck.getStore(thisObj), receiver, value, toPropertyKeyNode.execute(key), propagateFloatingCondition && condition);
        }
    }

    public abstract static class TerminalPropertySetNode extends PropertySetNode {
        private final boolean isGlobal;
        private final boolean isStrict;
        private final boolean setOwnProperty;
        protected final JSContext context;

        public TerminalPropertySetNode(Object key, boolean isGlobal, boolean isStrict, boolean setOwnProperty, JSContext context) {
            super(key);
            this.isGlobal = isGlobal;
            this.isStrict = isStrict;
            this.setOwnProperty = setOwnProperty;
            this.context = context;
        }

        @Override
        protected final PropertySetNode getNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected final void setNext(PropertySetNode next) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected final Shape getShape() {
            return null;
        }

        @Override
        public final void setValueInt(Object thisObj, int value, Object receiver) {
            setValue(thisObj, value, receiver);
        }

        @Override
        public final void setValueDouble(Object obj, double value, Object receiver) {
            setValue(obj, value, receiver);
        }

        @Override
        public final void setValueBoolean(Object obj, boolean value, Object receiver) {
            setValue(obj, value, receiver);
        }

        @Override
        protected boolean isGlobal() {
            return isGlobal;
        }

        @Override
        protected final boolean isStrict() {
            return this.isStrict;
        }

        @Override
        protected boolean isOwnProperty() {
            return setOwnProperty;
        }

        @Override
        public JSContext getContext() {
            return context;
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static final class GenericPropertySetNode extends TerminalPropertySetNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ForeignPropertySetNode foreignSetNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isStrictSymbol = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isMap = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isForeignObject = ConditionProfile.createBinaryProfile();
        @CompilerDirectives.CompilationFinal private Converters.Converter converter;

        public GenericPropertySetNode(Object key, boolean isGlobal, boolean isStrict, boolean setOwnProperty, JSContext context) {
            super(key, isGlobal, isStrict, setOwnProperty, context);
            this.toObjectNode = JSToObjectNode.createToObjectNoCheck(context);
        }

        @Override
        public void setValue(Object thisObj, Object value, Object receiver) {
            if (isObject.profile(JSObject.isDynamicObject(thisObj))) {
                setValueInDynamicObject(thisObj, value, receiver);
            } else if (isStrictSymbol.profile(isStrict() && thisObj instanceof Symbol)) {
                throw Errors.createTypeError("Cannot create property on symbol", this);
            } else if (isMap.profile(thisObj instanceof Map)) {
                if (converter == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    converter = Converters.JS_TO_JAVA_CONVERTER;
                }
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) thisObj;
                Boundaries.mapPut(map, key, converter.convert(value));
            } else if (isForeignObject.profile(JSGuards.isForeignObject(thisObj))) {
                if (foreignSetNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    foreignSetNode = insert(new ForeignPropertySetNode(key, context));
                }
                foreignSetNode.setValue(thisObj, value, receiver);
            } else {
                setValueInDynamicObject(toObjectNode.executeTruffleObject(thisObj), value, receiver);
            }
        }

        private void setValueInDynamicObject(Object thisObj, Object value, Object receiver) {
            DynamicObject thisJSObj = JSObject.castJSObject(thisObj);
            if (key instanceof HiddenKey) {
                thisJSObj.define(key, value);
            } else if (isGlobal() && isStrict() && !JSObject.hasProperty(thisJSObj, key, jsclassProfile)) {
                globalPropertySetInStrictMode(thisObj);
            } else {
                JSObject.setWithReceiver(thisJSObj, key, value, receiver, isStrict(), jsclassProfile);
            }
        }
    }

    public static final class ForeignPropertySetNode extends LinkedPropertySetNode {

        @Child private Node foreignSet;
        @Child private ExportValueNode export;
        @Child private Node foreignSetterInvoke;
        private final JSContext context;

        public ForeignPropertySetNode(Object key, JSContext context) {
            super(key, new ForeignLanguageCheckNode());
            this.context = context;
            this.foreignSet = Message.WRITE.createNode();
            this.export = ExportValueNode.create(context);
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            TruffleObject truffleObject = (TruffleObject) thisObj;
            try {
                ForeignAccess.sendWrite(foreignSet, truffleObject, key, value);
            } catch (UnknownIdentifierException e) {
                if (context.isOptionNashornCompatibilityMode()) {
                    tryInvokeSetter(truffleObject, value);
                }
                // do nothing
            } catch (UnsupportedMessageException e) {
                // do nothing
            } catch (UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(truffleObject, e, Message.WRITE, this);
            }
        }

        @Override
        public void setValueUncheckedDouble(Object thisObj, double value, Object receiver, boolean condition) {
            TruffleObject truffleObject = (TruffleObject) thisObj;
            try {
                ForeignAccess.sendWrite(foreignSet, truffleObject, key, value);
            } catch (UnknownIdentifierException e) {
                if (context.isOptionNashornCompatibilityMode()) {
                    tryInvokeSetter(truffleObject, value);
                }
                // do nothing
            } catch (UnsupportedMessageException e) {
                // do nothing
            } catch (UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(truffleObject, e, Message.WRITE, this);
            }
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            TruffleObject truffleObject = (TruffleObject) thisObj;
            Object boundValue = export.executeWithTarget(value, Undefined.instance);
            try {
                ForeignAccess.sendWrite(foreignSet, truffleObject, key, boundValue);
            } catch (UnknownIdentifierException e) {
                if (context.isOptionNashornCompatibilityMode()) {
                    tryInvokeSetter((TruffleObject) thisObj, boundValue);
                }
                // do nothing
            } catch (UnsupportedMessageException e) {
                // do nothing
            } catch (UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(truffleObject, e, Message.WRITE, this);
            }
        }

        // in nashorn-compat mode, `javaObj.xyz = a` can mean `javaObj.setXyz(a)`.
        private void tryInvokeSetter(TruffleObject thisObj, Object value) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = context.getRealm().getEnv();
            if (env.isHostObject(thisObj) && JSRuntime.isString(getKey())) {
                if (foreignSetterInvoke == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    foreignSetterInvoke = insert(Message.createInvoke(1).createNode());
                }
                try {
                    String setterKey = getAccessorKey("set");
                    if (setterKey != null) {
                        ForeignAccess.sendInvoke(foreignSetterInvoke, thisObj, setterKey, new Object[]{value});
                    }
                } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                    // silently ignore
                }
            }
        }
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static final class UninitializedPropertySetNode extends TerminalPropertySetNode {

        private boolean propertyAssumptionCheckEnabled;
        private final int attributeFlags;

        public UninitializedPropertySetNode(Object key, boolean isGlobal, JSContext context, boolean isStrict, boolean setOwnProperty, int attributeFlags) {
            super(key, isGlobal, isStrict, setOwnProperty, context);
            this.attributeFlags = attributeFlags;
        }

        @Override
        public void setValue(Object thisObject, Object value, Object receiver) {
            rewrite(context, thisObject, value).setValue(thisObject, value, receiver);
        }

        @Override
        protected boolean isPropertyAssumptionCheckEnabled() {
            return propertyAssumptionCheckEnabled && context.isSingleRealm();
        }

        @Override
        protected void setPropertyAssumptionCheckEnabled(boolean value) {
            this.propertyAssumptionCheckEnabled = value;
        }

        @Override
        protected int getAttributeFlags() {
            return attributeFlags;
        }
    }

    public static final class ArrayLengthPropertySetNode extends LinkedPropertySetNode {

        @Child private ArrayLengthWriteNode arrayLengthWrite;
        private final Property property;
        private final boolean isStrict;

        public ArrayLengthPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean isStrict) {
            super(property.getKey(), shapeCheck);
            assert JSProperty.isData(property);
            assert isArrayLengthProperty(property) && JSProperty.isWritable(property);
            this.property = property;
            this.isStrict = isStrict;
            this.arrayLengthWrite = ArrayLengthWriteNode.create(isStrict);
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            try {
                JSProperty.setValueThrow(property, receiverCheck.getStore(thisObj), thisObj, value, getShape(), isStrict);
            } catch (IncompatibleLocationException | FinalLocationException e) {
                rewrite(asReason(e)).setValue(thisObj, value, receiver);
            }
        }

        @Override
        public void setValueUncheckedInt(Object thisObj, int value, Object receiver, boolean condition) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            // shape check should be sufficient to guarantee this
            assert JSArray.isJSArray(store);
            arrayLengthWrite.executeVoid(store, value, condition);
        }
    }

    public static final class MapPropertySetNode extends LinkedPropertySetNode {
        private final Converters.Converter converter;

        public MapPropertySetNode(Object key, ReceiverCheckNode receiverCheck) {
            super(key, receiverCheck);
            this.converter = Converters.JS_TO_JAVA_CONVERTER;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            Boundaries.mapPut((Map<Object, Object>) thisObj, key, converter.convert(value));
        }
    }

    public static final class JSJavaWrapperPropertySetNode extends LinkedPropertySetNode {
        @Child private PropertySetNode nested;

        public JSJavaWrapperPropertySetNode(Object key, boolean isGlobal, boolean isStrict, boolean setOwnProperty, JSContext context) {
            super(key, new JSClassCheckNode(JSJavaWrapper.getJSClassInstance()));
            this.nested = new UninitializedPropertySetNode(key, isGlobal, context, isStrict, setOwnProperty, JSAttributes.getDefault());
        }

        @Override
        public void setValueUnchecked(Object thisObj, Object value, Object receiver, boolean condition) {
            nested.setValue(JSJavaWrapper.getWrapped((DynamicObject) thisObj), value, receiver);
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected LinkedPropertySetNode createCachedPropertyNode(Property property, Object thisObj, int depth, JSContext context, Object value) {
        if (JSObject.isDynamicObject(thisObj)) {
            return createCachedPropertyNodeJSObject(property, (DynamicObject) thisObj, depth, context, value);
        } else {
            return createCachedPropertyNodeNotJSObject(property, thisObj, depth, context);
        }
    }

    private LinkedPropertySetNode createCachedPropertyNodeJSObject(Property property, DynamicObject thisObj, int depth, JSContext context, Object value) {
        Shape cacheShape = thisObj.getShape();
        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisObj, depth, false, false);

        if (JSProperty.isData(property)) {
            return createCachedDataPropertyNodeJSObject(thisObj, depth, context, value, shapeCheck, property);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertySetNode(property, shapeCheck, isStrict());
        }
    }

    private LinkedPropertySetNode createCachedDataPropertyNodeJSObject(DynamicObject thisObj, int depth, JSContext context, Object value, AbstractShapeCheckNode shapeCheck, Property dataProperty) {
        assert !JSProperty.isConst(dataProperty) || (depth == 0 && isGlobal() && dataProperty.getLocation().isDeclared()) : "const assignment";
        if (!JSProperty.isWritable(dataProperty)) {
            return new ReadOnlyPropertySetNode(key, shapeCheck, isStrict());
        } else if (depth > 0) {
            // define a new own property, shadowing an existing prototype property
            // NB: must have a guarding test that the inherited property is writable
            assert JSProperty.isWritable(dataProperty);
            return createUndefinedPropertyNode(thisObj, thisObj, depth, context, value);
        } else {
            if (JSProperty.isData(dataProperty) && !JSProperty.isProxy(dataProperty) && !dataProperty.getLocation().isDeclared() && !dataProperty.getLocation().canSet(value)) {
                return createCachedDataPropertyGeneralize(thisObj, depth, context, value, dataProperty);
            }

            // existing own property
            if (dataProperty.getLocation() instanceof IntLocation) {
                return new IntPropertySetNode(dataProperty, shapeCheck);
            } else if (dataProperty.getLocation() instanceof DoubleLocation) {
                return new DoublePropertySetNode(dataProperty, shapeCheck);
            } else if (dataProperty.getLocation() instanceof BooleanLocation) {
                return new BooleanPropertySetNode(dataProperty, shapeCheck);
            } else {
                if (isArrayLengthProperty(dataProperty)) {
                    return new ArrayLengthPropertySetNode(dataProperty, shapeCheck, isStrict());
                } else if (dataProperty.getLocation().isDeclared()) {
                    return createRedefinePropertyNode(key, shapeCheck, shapeCheck.getShape(), dataProperty, value, context);
                } else {
                    return new ObjectPropertySetNode(dataProperty, shapeCheck, isStrict());
                }
            }
        }
    }

    private static LinkedPropertySetNode createDefinePropertyNode(Object key, ReceiverCheckNode shapeCheck, Object value, JSContext context, int attributeFlags) {
        Shape oldShape = shapeCheck.getShape();
        Shape newShape = JSObjectUtil.shapeDefineDataProperty(context, oldShape, key, value, attributeFlags);
        return createResolvedDefinePropertyNode(key, shapeCheck, newShape, context, attributeFlags);
    }

    private static LinkedPropertySetNode createRedefinePropertyNode(Object key, ReceiverCheckNode shapeCheck, Shape oldShape, Property dataProperty, Object value, JSContext context) {
        assert JSProperty.isData(dataProperty);
        assert JSProperty.isWritable(dataProperty);
        assert dataProperty == oldShape.getProperty(key);

        Shape newShape = JSObjectUtil.shapeDefineDataProperty(context, oldShape, key, value, dataProperty.getFlags());
        return createResolvedDefinePropertyNode(key, shapeCheck, newShape, context, dataProperty.getFlags());
    }

    private LinkedPropertySetNode createCachedDataPropertyGeneralize(DynamicObject thisObj, int depth, JSContext context, Object value, Property dataProperty) {
        Shape oldShape = thisObj.getShape();
        dataProperty.setGeneric(thisObj, value, null);
        Shape newShape = thisObj.getShape();
        assert newShape != oldShape;

        Property newProperty = newShape.getProperty(key);
        if (oldShape.isValid()) {
            // change property type (basic only)
            return createResolvedRedefinePropertyNode(key, createShapeCheckNode(oldShape, thisObj, depth, false, false), newShape, newProperty);
        } else {
            return createCachedPropertyNode(newProperty, thisObj, depth, context, value);
        }
    }

    private LinkedPropertySetNode createCachedPropertyNodeNotJSObject(Property property, Object thisObj, int depth, JSContext context) {
        ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth, context);

        if (JSProperty.isData(property)) {
            return new ReadOnlyPropertySetNode(key, receiverCheck, isStrict());
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertySetNode(property, receiverCheck, isStrict());
        }
    }

    private static LinkedPropertySetNode createResolvedDefinePropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, JSContext context, int attributeFlags) {
        Property prop = newShape.getProperty(key);
        assert (prop.getFlags() & (JSAttributes.ATTRIBUTES_MASK | JSProperty.CONST)) == attributeFlags;

        if (prop.getLocation() instanceof IntLocation) {
            return new DefineIntPropertyNode(key, receiverCheck, newShape, context, prop);
        } else if (prop.getLocation() instanceof DoubleLocation) {
            return new DefineDoublePropertyNode(key, receiverCheck, newShape, context, prop);
        } else if (prop.getLocation() instanceof BooleanLocation) {
            return new DefineBooleanPropertyNode(key, receiverCheck, newShape, context, prop);
        } else {
            return new DefineObjectPropertyNode(key, receiverCheck, newShape, context, prop);
        }
    }

    private static LinkedPropertySetNode createResolvedRedefinePropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape newShape, Property property) {
        return new RedefineObjectPropertyNode(key, receiverCheck, newShape, property);
    }

    @Override
    protected LinkedPropertySetNode createUndefinedPropertyNode(Object thisObj, Object store, int depth, JSContext context, Object value) {
        LinkedPropertySetNode specialized = createJavaPropertyNodeMaybe(thisObj, depth, context);
        if (specialized != null) {
            return specialized;
        }
        if (JSObject.isDynamicObject(thisObj)) {
            DynamicObject thisJSObj = (DynamicObject) thisObj;
            Shape cacheShape = thisJSObj.getShape();
            AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisJSObj, depth, false, true);
            ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(thisJSObj)) : shapeCheck;
            if (JSAdapter.isJSAdapter(store)) {
                return new JSAdapterPropertySetNode(key, receiverCheck, isStrict());
            } else if (JSProxy.isProxy(store) && JSRuntime.isPropertyKey(key) && (!isStrict() || !isGlobal() || JSObject.hasOwnProperty(thisJSObj, key))) {
                return new JSProxyDispatcherPropertySetNode(context, key, receiverCheck, isStrict());
            } else if (!JSRuntime.isObject(thisJSObj)) {
                return new TypeErrorPropertySetNode(key, shapeCheck);
            } else if (isStrict() && isGlobal()) {
                return new ReferenceErrorPropertySetNode(key, shapeCheck);
            } else if (JSShape.isExtensible(cacheShape)) {
                return createDefinePropertyNode(key, shapeCheck, value, context, getAttributeFlags());
            } else {
                return new ReadOnlyPropertySetNode(key, createShapeCheckNode(cacheShape, thisJSObj, depth, false, false), isStrict());
            }
        } else if (JSProxy.isProxy(store)) {
            ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth, context);
            return new JSProxyDispatcherPropertySetNode(context, key, receiverCheck, isStrict());
        } else {
            boolean doThrow = isStrict();
            if (!JSRuntime.isJSNative(thisObj)) {
                // Nashorn never throws when setting inexistent properties on Java objects
                doThrow = false;
            }
            return new ReadOnlyPropertySetNode(key, new InstanceofCheckNode(thisObj.getClass(), context), doThrow);
        }
    }

    @Override
    protected LinkedPropertySetNode createJavaPropertyNodeMaybe(Object thisObj, int depth, JSContext context) {
        if (!JSTruffleOptions.NashornJavaInterop) {
            return null;
        }
        return createJavaPropertyNodeMaybe0(thisObj, context);
    }

    /* In a separate method for Substrate VM support. */
    private LinkedPropertySetNode createJavaPropertyNodeMaybe0(Object thisObj, JSContext context) {
        if (!(JSObject.isDynamicObject(thisObj))) {
            if (hasSettableField(thisObj, context)) {
                if (thisObj instanceof JavaClass) {
                    return new JavaStaticFieldPropertySetNode(key, new InstanceofCheckNode(thisObj.getClass(), context), JavaAccess.isReflectionAllowed(context));
                } else {
                    return new JavaSetterPropertySetNode(key, new InstanceofCheckNode(thisObj.getClass(), context), getSetter(thisObj, context));
                }
            } else if (thisObj instanceof java.util.Map) {
                return new MapPropertySetNode(key, new InstanceofCheckNode(thisObj.getClass(), context));
            }
        } else {
            if (JSJavaWrapper.isJSJavaWrapper(thisObj)) {
                return new JSJavaWrapperPropertySetNode(key, isGlobal(), isStrict(), isOwnProperty(), context);
            }
        }
        return null;
    }

    private boolean hasSettableField(Object thisObj, JSContext context) {
        if (thisObj == null) {
            return false;
        }
        if (!(key instanceof String)) {
            // could be Symbol!
            return false;
        }
        if (thisObj instanceof JavaClass) {
            return getStaticSetter((JavaClass) thisObj, context) != null;
        } else {
            return getSetter(thisObj, context) != null;
        }
    }

    private JavaSetter getStaticSetter(JavaClass thisObj, JSContext context) {
        JavaMember member = thisObj.getMember((String) key, JavaClass.STATIC, JavaClass.SETTER, JavaAccess.isReflectionAllowed(context));
        assert member == null || member instanceof JavaSetter;
        return (member != null) ? (JavaSetter) member : null;
    }

    private JavaSetter getSetter(Object thisObj, JSContext context) {
        assert !(thisObj instanceof JavaClass);
        JavaClass javaClass = JavaClass.forClass(thisObj.getClass());
        JavaMember member = javaClass.getMember((String) key, JavaClass.INSTANCE, JavaClass.SETTER, JavaAccess.isReflectionAllowed(context));
        return (JavaSetter) member;
    }

    @Override
    protected PropertySetNode createGenericPropertyNode(JSContext context) {
        return new GenericPropertySetNode(key, isGlobal(), isStrict(), isOwnProperty(), context);
    }

    protected boolean isStrict() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Class<PropertySetNode> getBaseClass() {
        return PropertySetNode.class;
    }

    @Override
    protected Class<? extends PropertySetNode> getUninitializedNodeClass() {
        return UninitializedPropertySetNode.class;
    }

    @Override
    protected PropertySetNode createTruffleObjectPropertyNode(TruffleObject thisObject, JSContext context) {
        return new ForeignPropertySetNode(key, context);
    }

    protected int getAttributeFlags() {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    protected void globalPropertySetInStrictMode(Object thisObj) {
        assert JSObject.isDynamicObject(thisObj) && JSObject.getJSContext((DynamicObject) thisObj).getRealm().getGlobalObject() == thisObj;
        throw Errors.createReferenceErrorNotDefined(getKey(), this);
    }

    @Override
    public abstract JSContext getContext();
}
