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

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.IntLocation;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.AlwaysValidAssumption;
import com.oracle.truffle.api.utilities.NeverValidAssumption;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.array.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * @see WritePropertyNode
 */
public class PropertySetNode extends PropertyCacheNode<PropertySetNode.SetCacheNode> {
    private final boolean isGlobal;
    private final boolean isStrict;
    private final boolean setOwnProperty;
    private final boolean declaration;
    private final boolean superProperty;
    private final byte attributeFlags;
    private boolean propertyAssumptionCheckEnabled;

    public static PropertySetNode create(Object key, boolean isGlobal, JSContext context, boolean isStrict) {
        final boolean setOwnProperty = false;
        return createImpl(key, isGlobal, context, isStrict, setOwnProperty, JSAttributes.getDefault());
    }

    public static PropertySetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean isStrict, boolean setOwnProperty, int attributeFlags) {
        return createImpl(key, isGlobal, context, isStrict, setOwnProperty, attributeFlags, false, false);
    }

    public static PropertySetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean isStrict, boolean setOwnProperty, int attributeFlags, boolean declaration) {
        return createImpl(key, isGlobal, context, isStrict, setOwnProperty, attributeFlags, declaration, false);
    }

    public static PropertySetNode createImpl(Object key, boolean isGlobal, JSContext context, boolean isStrict, boolean setOwnProperty, int attributeFlags, boolean declaration,
                    boolean superProperty) {
        return new PropertySetNode(key, context, isGlobal, isStrict, setOwnProperty, attributeFlags, declaration, superProperty);
    }

    public static PropertySetNode createSetHidden(HiddenKey key, JSContext context) {
        return createImpl(key, false, context, false, true, 0);
    }

    protected PropertySetNode(Object key, JSContext context, boolean isGlobal, boolean isStrict, boolean setOwnProperty, int attributeFlags, boolean declaration, boolean superProperty) {
        super(key, context);
        assert setOwnProperty ? attributeFlags == (attributeFlags & (JSAttributes.ATTRIBUTES_MASK | JSProperty.CONST)) : attributeFlags == JSAttributes.getDefault();
        this.isGlobal = isGlobal;
        this.isStrict = isStrict;
        this.setOwnProperty = setOwnProperty;
        this.attributeFlags = (byte) attributeFlags;
        this.declaration = declaration;
        this.superProperty = superProperty;
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

    @ExplodeLoop
    protected void setValue(Object thisObj, Object value, Object receiver) {
        for (SetCacheNode c = cacheNode; c != null; c = c.next) {
            if (c.isGeneric()) {
                c.setValue(thisObj, value, receiver, this, false);
                return;
            }
            if (!c.isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                if (c.setValue(thisObj, value, receiver, this, guard)) {
                    return;
                }
            }
        }
        deoptimize();
        setValueAndSpecialize(thisObj, value, receiver);
    }

    @TruffleBoundary
    private boolean setValueAndSpecialize(Object thisObj, Object value, Object receiver) {
        return specialize(thisObj, value).setValue(thisObj, value, receiver, this, false);
    }

    @ExplodeLoop
    protected void setValueInt(Object thisObj, int value, Object receiver) {
        for (SetCacheNode c = cacheNode; c != null; c = c.next) {
            if (c.isGeneric()) {
                c.setValueInt(thisObj, value, receiver, this, false);
                return;
            }
            if (!c.isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                if (c.setValueInt(thisObj, value, receiver, this, guard)) {
                    return;
                }
            }
        }
        deoptimize();
        setValueIntAndSpecialize(thisObj, value, receiver);
    }

    @TruffleBoundary
    private void setValueIntAndSpecialize(Object thisObj, int value, Object receiver) {
        specialize(thisObj, value).setValueInt(thisObj, value, receiver, this, false);
    }

    @ExplodeLoop
    protected void setValueDouble(Object thisObj, double value, Object receiver) {
        for (SetCacheNode c = cacheNode; c != null; c = c.next) {
            if (c.isGeneric()) {
                c.setValueDouble(thisObj, value, receiver, this, false);
                return;
            }
            if (!c.isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                if (c.setValueDouble(thisObj, value, receiver, this, guard)) {
                    return;
                }
            }
        }
        deoptimize();
        setValueDoubleAndSpecialize(thisObj, value, receiver);
    }

    @TruffleBoundary
    private void setValueDoubleAndSpecialize(Object thisObj, double value, Object receiver) {
        specialize(thisObj, value).setValueDouble(thisObj, value, receiver, this, false);
    }

    @ExplodeLoop
    protected void setValueBoolean(Object thisObj, boolean value, Object receiver) {
        for (SetCacheNode c = cacheNode; c != null; c = c.next) {
            if (c.isGeneric()) {
                c.setValueBoolean(thisObj, value, receiver, this, false);
                return;
            }
            if (!c.isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                if (c.setValueBoolean(thisObj, value, receiver, this, guard)) {
                    return;
                }
            }
        }
        deoptimize();
        setValueBooleanAndSpecialize(thisObj, value, receiver);
    }

    @TruffleBoundary
    private void setValueBooleanAndSpecialize(Object thisObj, boolean value, Object receiver) {
        specialize(thisObj, value).setValueBoolean(thisObj, value, receiver, this, false);
    }

    public abstract static class SetCacheNode extends PropertyCacheNode.CacheNode<SetCacheNode> {
        protected SetCacheNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        protected abstract boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard);

        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }

        protected boolean setValueDouble(Object thisObj, double value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }

        protected boolean setValueBoolean(Object thisObj, boolean value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }

        @Override
        protected boolean acceptsValue(Object value) {
            return true;
        }
    }

    public abstract static class LinkedPropertySetNode extends SetCacheNode {
        protected LinkedPropertySetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }
    }

    public static final class ObjectPropertySetNode extends LinkedPropertySetNode {
        private final Property property;

        public ObjectPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(shapeCheck);
            this.property = property;
            assert JSProperty.isData(property) && JSProperty.isWritable(property) && !JSProperty.isProxy(property) && !property.getLocation().isFinal();
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (property.getLocation().canSet(value)) {
                DynamicObject store = receiverCheck.getStore(thisObj);
                try {
                    property.set(store, value, receiverCheck.getShape());
                } catch (IncompatibleLocationException | FinalLocationException e) {
                    throw Errors.shouldNotReachHere(e);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean acceptsValue(Object value) {
            return property.getLocation().canSet(value);
        }
    }

    public static final class PropertyProxySetNode extends LinkedPropertySetNode {
        private final Property property;
        private final boolean isStrict;
        private final BranchProfile errorBranch = BranchProfile.create();

        public PropertyProxySetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean isStrict) {
            super(shapeCheck);
            this.property = property;
            this.isStrict = isStrict;
            assert JSProperty.isData(property) && JSProperty.isWritable(property) && JSProperty.isProxy(property) && !property.getLocation().isFinal();
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            boolean ret = ((PropertyProxy) property.get(store, guard)).set(store, value);
            if (!ret && isStrict) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
            }
            return true;
        }
    }

    public static final class IntPropertySetNode extends LinkedPropertySetNode {

        private final Property property;
        private final IntLocation location;

        public IntPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(shapeCheck);
            this.property = property;
            this.location = (IntLocation) property.getLocation();
            assert JSProperty.isData(property) && JSProperty.isWritable(property) && !property.getLocation().isFinal();
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (value instanceof Integer) {
                DynamicObject store = receiverCheck.getStore(thisObj);
                try {
                    property.set(store, value, receiverCheck.getShape());
                } catch (IncompatibleLocationException | FinalLocationException e) {
                    throw Errors.shouldNotReachHere(e);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            try {
                location.setInt(store, value, receiverCheck.getShape());
                return true;
            } catch (FinalLocationException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @Override
        protected boolean acceptsValue(Object value) {
            return value instanceof Integer;
        }
    }

    public static final class DoublePropertySetNode extends LinkedPropertySetNode {
        private final DoubleLocation location;

        @CompilationFinal int valueProfile;
        private static final int INT = 1 << 0;
        private static final int DOUBLE = 1 << 1;
        private static final int OTHER = 1 << 2;

        public DoublePropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(shapeCheck);
            this.location = (DoubleLocation) property.getLocation();
            assert JSProperty.isData(property) && JSProperty.isWritable(property) && !property.getLocation().isFinal();
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            int p = valueProfile;
            double doubleValue;
            if ((p & DOUBLE) != 0 && value instanceof Double) {
                doubleValue = (double) value;
            } else if ((p & INT) != 0 && value instanceof Integer) {
                doubleValue = (int) value;
            } else if ((p & OTHER) != 0 && !(value instanceof Double || value instanceof Integer)) {
                return false;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (value instanceof Double) {
                    p |= DOUBLE;
                } else if (value instanceof Integer) {
                    p |= INT;
                } else {
                    p |= OTHER;
                }
                valueProfile = p;
                return setValue(thisObj, value, receiver, root, guard);
            }
            DynamicObject store = receiverCheck.getStore(thisObj);
            try {
                location.setDouble(store, doubleValue, receiverCheck.getShape());
                return true;
            } catch (FinalLocationException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            try {
                location.setDouble(store, value, receiverCheck.getShape());
                return true;
            } catch (FinalLocationException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @Override
        protected boolean setValueDouble(Object thisObj, double value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            try {
                location.setDouble(store, value, receiverCheck.getShape());
                return true;
            } catch (FinalLocationException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @Override
        protected boolean acceptsValue(Object value) {
            return value instanceof Double || value instanceof Integer;
        }
    }

    public static final class BooleanPropertySetNode extends LinkedPropertySetNode {

        private final Property property;
        private final BooleanLocation location;

        public BooleanPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck) {
            super(shapeCheck);
            this.property = property;
            this.location = (BooleanLocation) property.getLocation();
            assert JSProperty.isData(property);
            assert JSProperty.isWritable(property);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (value instanceof Boolean) {
                DynamicObject store = receiverCheck.getStore(thisObj);
                try {
                    property.set(store, value, receiverCheck.getShape());
                } catch (IncompatibleLocationException | FinalLocationException e) {
                    throw Errors.shouldNotReachHere(e);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean setValueBoolean(Object thisObj, boolean value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            try {
                location.setBoolean(store, value, receiverCheck.getShape());
                return true;
            } catch (FinalLocationException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @Override
        protected boolean acceptsValue(Object value) {
            return value instanceof Boolean;
        }
    }

    public static final class AccessorPropertySetNode extends LinkedPropertySetNode {
        private final Property property;
        private final boolean isStrict;
        @Child private JSFunctionCallNode callNode;
        private final BranchProfile undefinedSetterBranch = BranchProfile.create();

        public AccessorPropertySetNode(Property property, ReceiverCheckNode receiverCheck, boolean isStrict) {
            super(receiverCheck);
            assert JSProperty.isAccessor(property);
            this.property = property;
            this.isStrict = isStrict;
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            Accessor accessor = (Accessor) property.get(store, guard);

            DynamicObject setter = accessor.getSetter();
            if (setter != Undefined.instance) {
                callNode.executeCall(JSArguments.createOneArg(receiver, setter, value));
            } else {
                undefinedSetterBranch.enter();
                if (isStrict) {
                    throw Errors.createTypeErrorCannotSetAccessorProperty(root.getKey(), store);
                }
            }
            return true;
        }
    }

    static final class DefinePropertyCache {
        protected final Shape oldShape;
        protected final Shape newShape;
        protected final Property property;
        protected final Assumption newShapeValidAssumption;
        protected final DefinePropertyCache next;
        protected static final DefinePropertyCache GENERIC = new DefinePropertyCache(null, null, null, null, null);

        protected DefinePropertyCache(Shape oldShape, Shape newShape, Property property, Assumption newShapeValidAssumption, DefinePropertyCache next) {
            this.oldShape = oldShape;
            this.newShape = newShape;
            this.property = property;
            this.newShapeValidAssumption = newShapeValidAssumption;
            this.next = next;
        }

        protected boolean isValid() {
            return newShapeValidAssumption == NeverValidAssumption.INSTANCE || newShapeValidAssumption.isValid();
        }

        protected void maybeUpdateShape(DynamicObject store) {
            if (newShapeValidAssumption == NeverValidAssumption.INSTANCE) {
                updateShape(store);
            }
        }

        @TruffleBoundary
        private static void updateShape(DynamicObject store) {
            DynamicObjectLibrary.getUncached().updateShape(store);
        }

        protected boolean acceptsValue(Object value) {
            if (oldShape != newShape) {
                return property.getLocation().canStore(value);
            } else {
                return property.getLocation().canSet(value);
            }
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            StringBuilder sb = new StringBuilder();
            int count = 0;
            sb.append('[');
            for (DefinePropertyCache current = this; current != null; current = current.next) {
                sb.append(++count);
                sb.append(": {property=").append(current.property);
                if (current.oldShape != current.newShape) {
                    sb.append(", oldShape=").append(current.oldShape).append(", newShape=").append(current.newShape);
                } else {
                    sb.append(", shape=").append(current.oldShape);
                }
                sb.append("}");
                if (current.next != null) {
                    sb.append(", ");
                }
            }
            sb.append(']');
            return sb.toString();
        }

        protected DefinePropertyCache withNext(DefinePropertyCache newNext) {
            return new DefinePropertyCache(oldShape, newShape, property, newShapeValidAssumption, newNext);
        }
    }

    static DefinePropertyCache filterValid(DefinePropertyCache cache) {
        if (cache == null) {
            return null;
        }
        DefinePropertyCache filteredNext = filterValid(cache.next);
        if (cache.isValid()) {
            if (filteredNext == cache.next) {
                return cache;
            } else {
                return cache.withNext(filteredNext);
            }
        } else {
            return filteredNext;
        }
    }

    public static class DataPropertySetNode extends LinkedPropertySetNode {
        @CompilationFinal protected DefinePropertyCache cache;

        public DataPropertySetNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        public DataPropertySetNode(Object key, ReceiverCheckNode receiverCheck, Shape oldShape, Shape newShape, Property property) {
            super(receiverCheck);
            assert JSProperty.isData(property);
            assert property.getKey().equals(key) : "property=" + property + " key=" + key;
            assert property == newShape.getProperty(key);
            this.cache = new DefinePropertyCache(oldShape, newShape, property, getShapeValidAssumption(oldShape, newShape), null);
        }

        protected JSDynamicObject getStore(Object thisObj) {
            return ((JSDynamicObject) thisObj);
        }

        @ExplodeLoop
        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            JSDynamicObject store = getStore(thisObj);
            for (DefinePropertyCache resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved.oldShape.check(store)) {
                    if (!resolved.isValid()) {
                        break;
                    }
                    if (setCachedObject(store, value, resolved)) {
                        return true;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            return setValueAndSpecialize(store, value, root);
        }

        @ExplodeLoop
        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            JSDynamicObject store = getStore(thisObj);
            for (DefinePropertyCache resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved.oldShape.check(store)) {
                    if (!resolved.isValid()) {
                        break;
                    }
                    if (setCachedInt(store, value, resolved)) {
                        return true;
                    } else if (setCachedDouble(store, value, resolved)) {
                        return true;
                    } else if (setCachedObject(store, value, resolved)) {
                        return true;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            return setValueAndSpecialize(store, value, root);
        }

        @ExplodeLoop
        @Override
        protected boolean setValueDouble(Object thisObj, double value, Object receiver, PropertySetNode root, boolean guard) {
            JSDynamicObject store = getStore(thisObj);
            for (DefinePropertyCache resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved.oldShape.check(store)) {
                    if (!resolved.isValid()) {
                        break;
                    }
                    if (setCachedDouble(store, value, resolved)) {
                        return true;
                    } else if (setCachedObject(store, value, resolved)) {
                        return true;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            return setValueAndSpecialize(store, value, root);
        }

        @ExplodeLoop
        @Override
        protected boolean setValueBoolean(Object thisObj, boolean value, Object receiver, PropertySetNode root, boolean guard) {
            JSDynamicObject store = getStore(thisObj);
            for (DefinePropertyCache resolved = cache; resolved != null; resolved = resolved.next) {
                if (resolved.oldShape.check(store)) {
                    if (!resolved.isValid()) {
                        break;
                    }
                    if (setCachedBoolean(store, value, resolved)) {
                        return true;
                    } else if (setCachedObject(store, value, resolved)) {
                        return true;
                    }
                }
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();
            return setValueAndSpecialize(store, value, root);
        }

        private static boolean setCachedObject(DynamicObject store, Object value, DefinePropertyCache resolved) {
            if (resolved.oldShape != resolved.newShape) {
                if (resolved.property.getLocation().canStore(value)) {
                    resolved.property.setSafe(store, value, resolved.oldShape, resolved.newShape);
                    resolved.maybeUpdateShape(store);
                    return true;
                }
            } else {
                if (resolved.property.getLocation().canSet(value)) {
                    resolved.property.setSafe(store, value, resolved.oldShape);
                    return true;
                }
            }
            return false;
        }

        private static boolean setCachedInt(DynamicObject store, int value, DefinePropertyCache resolved) {
            if (resolved.property.getLocation() instanceof IntLocation) {
                IntLocation intLocation = (IntLocation) resolved.property.getLocation();
                if (resolved.oldShape != resolved.newShape) {
                    intLocation.setInt(store, value, resolved.oldShape, resolved.newShape);
                    resolved.maybeUpdateShape(store);
                    return true;
                } else {
                    if (!resolved.property.getLocation().isFinal()) {
                        try {
                            intLocation.setInt(store, value, resolved.oldShape);
                        } catch (FinalLocationException e) {
                            throw Errors.shouldNotReachHere();
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean setCachedDouble(DynamicObject store, double value, DefinePropertyCache resolved) {
            if (resolved.property.getLocation() instanceof DoubleLocation) {
                DoubleLocation doubleLocation = (DoubleLocation) resolved.property.getLocation();
                if (resolved.oldShape != resolved.newShape) {
                    doubleLocation.setDouble(store, value, resolved.oldShape, resolved.newShape);
                    resolved.maybeUpdateShape(store);
                    return true;
                } else {
                    if (!resolved.property.getLocation().isFinal()) {
                        try {
                            doubleLocation.setDouble(store, value, resolved.oldShape);
                        } catch (FinalLocationException e) {
                            throw Errors.shouldNotReachHere();
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean setCachedBoolean(DynamicObject store, boolean value, DefinePropertyCache resolved) {
            if (resolved.property.getLocation() instanceof BooleanLocation) {
                BooleanLocation booleanLocation = (BooleanLocation) resolved.property.getLocation();
                if (resolved.oldShape != resolved.newShape) {
                    booleanLocation.setBoolean(store, value, resolved.oldShape, resolved.newShape);
                    resolved.maybeUpdateShape(store);
                    return true;
                } else {
                    if (!resolved.property.getLocation().isFinal()) {
                        try {
                            booleanLocation.setBoolean(store, value, resolved.oldShape);
                        } catch (FinalLocationException e) {
                            throw Errors.shouldNotReachHere();
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean setValueAndSpecialize(DynamicObject obj, Object value, PropertySetNode root) {
            CompilerAsserts.neverPartOfCompilation();
            Object key = root.getKey();
            DefinePropertyCache res;
            Lock lock = root.getLock();
            lock.lock();
            try {
                DefinePropertyCache currentHead = cache;
                do {
                    assert currentHead == cache;
                    int cachedCount = 0;
                    boolean invalid = false;
                    res = null;

                    for (DefinePropertyCache c = currentHead; c != null; c = c.next) {
                        cachedCount++;
                        if (!c.isValid()) {
                            invalid = true;
                            break;
                        } else {
                            if (res == null && c.acceptsValue(value)) {
                                res = c;
                                // continue checking for invalid cache entries
                            }
                        }
                    }
                    if (invalid) {
                        assert cachedCount > 0;
                        currentHead = filterValid(currentHead);
                        this.cache = currentHead;
                        res = null;
                        continue; // restart
                    }
                    if (res == null) {
                        Shape oldShape = obj.getShape();
                        Property property = oldShape.getProperty(key);
                        Shape newShape;
                        Property newProperty;
                        if (property == null) {
                            JSObjectUtil.putDataProperty(root.getContext(), obj, key, value, root.getAttributeFlags());
                            newShape = obj.getShape();
                            newProperty = newShape.getLastProperty();
                            assert key.equals(newProperty.getKey());
                        } else {
                            if (JSProperty.isData(property) && !JSProperty.isProxy(property)) {
                                assert JSProperty.isWritable(property);
                                property.setGeneric(obj, value, null);
                            } else {
                                JSObjectUtil.defineDataProperty(obj, key, value, property.getFlags());
                            }
                            newShape = obj.getShape();
                            newProperty = newShape.getProperty(key);
                        }

                        if (!oldShape.isValid()) {
                            // pending removal
                            this.cache = null;
                            return true; // already set
                        }

                        Assumption newShapeValidAssumption = getShapeValidAssumption(oldShape, newShape);
                        this.cache = new DefinePropertyCache(oldShape, newShape, newProperty, newShapeValidAssumption, currentHead);
                        return true; // already set
                    }
                } while (res == null);
            } finally {
                lock.unlock();
            }
            assert res.acceptsValue(value);
            res.property.setSafe(obj, value, res.oldShape, res.newShape);
            return true;
        }

        private static Assumption getShapeValidAssumption(Shape oldShape, Shape newShape) {
            if (oldShape == newShape) {
                return AlwaysValidAssumption.INSTANCE;
            }
            return newShape.isValid() ? newShape.getValidAssumption() : NeverValidAssumption.INSTANCE;
        }

        @Override
        protected boolean sweep() {
            DefinePropertyCache before = this.cache;
            DefinePropertyCache after = filterValid(before);
            if (before == after) {
                return false;
            } else {
                this.cache = after;
                return true;
            }
        }
    }

    public static final class ReadOnlyPropertySetNode extends LinkedPropertySetNode {
        private final boolean isStrict;

        public ReadOnlyPropertySetNode(ReceiverCheckNode receiverCheck, boolean isStrict) {
            super(receiverCheck);
            this.isStrict = isStrict;
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (isStrict) {
                throw Errors.createTypeErrorNotWritableProperty(root.getKey(), thisObj, this);
            }
            return true;
        }
    }

    /**
     * If object is undefined or null, throw TypeError.
     */
    public static final class TypeErrorPropertySetNode extends LinkedPropertySetNode {

        public TypeErrorPropertySetNode(AbstractShapeCheckNode shapeCheckNode) {
            super(shapeCheckNode);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            assert thisObj == Undefined.instance || thisObj == Null.instance;
            throw Errors.createTypeErrorCannotSetProperty(root.getKey(), thisObj, this, root.getContext());
        }
    }

    public static final class JSAdapterPropertySetNode extends LinkedPropertySetNode {
        public JSAdapterPropertySetNode(ReceiverCheckNode receiverCheckNode) {
            super(receiverCheckNode);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            JSObject.set((DynamicObject) thisObj, root.getKey(), value, root.isStrict(), root);
            return true;
        }
    }

    public static final class JSProxyDispatcherPropertySetNode extends LinkedPropertySetNode {
        @Child private JSProxyPropertySetNode proxySet;

        public JSProxyDispatcherPropertySetNode(JSContext context, ReceiverCheckNode receiverCheckNode, boolean isStrict) {
            super(receiverCheckNode);
            this.proxySet = JSProxyPropertySetNode.create(context, isStrict);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            proxySet.executeWithReceiverAndValue(receiverCheck.getStore(thisObj), receiver, value, root.getKey());
            return true;
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            proxySet.executeWithReceiverAndValueInt(receiverCheck.getStore(thisObj), receiver, value, root.getKey());
            return true;
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static final class GenericPropertySetNode extends SetCacheNode {
        @Child private JSToObjectNode toObjectNode;
        @Child private ForeignPropertySetNode foreignSetNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isStrictSymbol = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isForeignObject = ConditionProfile.createBinaryProfile();

        public GenericPropertySetNode(JSContext context) {
            super(null);
            this.toObjectNode = JSToObjectNode.createToObjectNoCheck(context);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (isObject.profile(JSDynamicObject.isJSDynamicObject(thisObj))) {
                setValueInDynamicObject(thisObj, value, receiver, root);
            } else if (isStrictSymbol.profile(root.isStrict() && thisObj instanceof Symbol)) {
                throw Errors.createTypeError("Cannot create property on symbol", this);
            } else if (isForeignObject.profile(JSGuards.isForeignObject(thisObj))) {
                if (foreignSetNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    foreignSetNode = insert(new ForeignPropertySetNode(root.getContext()));
                }
                foreignSetNode.setValue(thisObj, value, receiver, root, guard);
            } else {
                setValueInDynamicObject(toObjectNode.execute(thisObj), value, receiver, root);
            }
            return true;
        }

        private void setValueInDynamicObject(Object thisObj, Object value, Object receiver, PropertySetNode root) {
            JSDynamicObject thisJSObj = ((JSDynamicObject) thisObj);
            Object key = root.getKey();
            if (key instanceof HiddenKey) {
                JSObjectUtil.putHiddenProperty(thisJSObj, key, value);
            } else if (root.isOwnProperty()) {
                if (root.isDeclaration()) {
                    assert JSGlobal.isJSGlobalObject(thisJSObj) && !JSObject.hasProperty(thisJSObj, key);
                    JSObjectUtil.putDeclaredDataProperty(root.getContext(), thisJSObj, key, value, root.getAttributeFlags());
                } else {
                    JSObject.defineOwnProperty(thisJSObj, key, PropertyDescriptor.createData(value, root.getAttributeFlags()), root.isStrict());
                }
            } else {
                JSObject.setWithReceiver(thisJSObj, key, value, receiver, root.isStrict(), jsclassProfile, root);
            }
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }

        @Override
        protected boolean setValueDouble(Object thisObj, double value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }

        @Override
        protected boolean setValueBoolean(Object thisObj, boolean value, Object receiver, PropertySetNode root, boolean guard) {
            return setValue(thisObj, value, receiver, root, guard);
        }
    }

    public static final class ForeignPropertySetNode extends LinkedPropertySetNode {

        @Child private ExportValueNode export;
        @CompilationFinal private boolean optimistic = true;
        private final JSContext context;
        @Child private InteropLibrary interop;
        @Child private InteropLibrary setterInterop;
        private final BranchProfile errorBranch = BranchProfile.create();

        public ForeignPropertySetNode(JSContext context) {
            super(new ForeignLanguageCheckNode());
            this.context = context;
            this.export = ExportValueNode.create();
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        private Object nullCheck(Object truffleObject, Object key) {
            if (interop.isNull(truffleObject)) {
                throw Errors.createTypeErrorCannotSetProperty(key, truffleObject, this, context);
            }
            return truffleObject;
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            Object key = root.getKey();
            Object truffleObject = nullCheck(thisObj, key);
            if (!(key instanceof String)) {
                return false;
            }
            return performWriteMember(truffleObject, value, root);
        }

        @Override
        protected boolean setValueDouble(Object thisObj, double value, Object receiver, PropertySetNode root, boolean guard) {
            Object key = root.getKey();
            Object truffleObject = nullCheck(thisObj, key);
            if (!(key instanceof String)) {
                return false;
            }
            return performWriteMember(truffleObject, value, root);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            Object key = root.getKey();
            Object truffleObject = nullCheck(thisObj, key);
            if (!(key instanceof String)) {
                return false;
            }
            Object exportedValue = export.execute(value);
            return performWriteMember(truffleObject, exportedValue, root);
        }

        private boolean performWriteMember(Object truffleObject, Object value, PropertySetNode root) {
            String stringKey = (String) root.getKey();

            if (context.getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(truffleObject)) {
                try {
                    interop.writeHashEntry(truffleObject, stringKey, value);
                } catch (UnknownKeyException | UnsupportedMessageException | UnsupportedTypeException e) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorInteropException(truffleObject, e, "writeHashEntry", this);
                }
            }

            if (context.isOptionNashornCompatibilityMode()) {
                if (tryInvokeSetter(truffleObject, value, root)) {
                    return true;
                }
            }
            if (optimistic) {
                try {
                    interop.writeMember(truffleObject, stringKey, value);
                } catch (UnknownIdentifierException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    optimistic = false;
                } catch (UnsupportedTypeException | UnsupportedMessageException e) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorInteropException(truffleObject, e, "writeMember", stringKey, this);
                }
            } else {
                if (interop.isMemberWritable(truffleObject, stringKey)) {
                    try {
                        interop.writeMember(truffleObject, stringKey, value);
                    } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorInteropException(truffleObject, e, "writeMember", stringKey, this);
                    }
                }
            }
            return true;
        }

        // in nashorn-compat mode, `javaObj.xyz = a` can mean `javaObj.setXyz(a)`.
        private boolean tryInvokeSetter(Object thisObj, Object value, PropertySetNode root) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = context.getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                String setterKey = root.getAccessorKey("set");
                if (setterKey == null) {
                    return false;
                }
                if (setterInterop == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                if (!setterInterop.isMemberInvocable(thisObj, setterKey)) {
                    return false;
                }
                try {
                    setterInterop.invokeMember(thisObj, setterKey, value);
                    return true;
                } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                    // silently ignore
                }
            }
            return false;
        }
    }

    public static final class ArrayBufferViewNonIntegerIndexSetNode extends LinkedPropertySetNode {

        public ArrayBufferViewNonIntegerIndexSetNode(AbstractShapeCheckNode shapeCheck) {
            super(shapeCheck);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            if (JSArrayBufferView.hasDetachedBuffer((DynamicObject) thisObj)) {
                throw Errors.createTypeErrorDetachedBuffer();
            } else {
                return false;
            }
        }

    }

    public static final class ArrayLengthPropertySetNode extends LinkedPropertySetNode {

        @Child private ArrayLengthWriteNode arrayLengthWrite;
        private final Property property;
        private final boolean isStrict;
        private final BranchProfile errorBranch = BranchProfile.create();

        public ArrayLengthPropertySetNode(Property property, AbstractShapeCheckNode shapeCheck, boolean isStrict) {
            super(shapeCheck);
            assert JSProperty.isData(property) && JSProperty.isWritable(property) && isArrayLengthProperty(property);
            this.property = property;
            this.isStrict = isStrict;
            this.arrayLengthWrite = ArrayLengthWriteNode.create(isStrict);
        }

        @Override
        protected boolean setValue(Object thisObj, Object value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            boolean ret = ((PropertyProxy) property.get(store, guard)).set(store, value);
            if (!ret && isStrict) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
            }
            return true;
        }

        @Override
        protected boolean setValueInt(Object thisObj, int value, Object receiver, PropertySetNode root, boolean guard) {
            DynamicObject store = receiverCheck.getStore(thisObj);
            // shape check should be sufficient to guarantee this
            assert JSArray.isJSFastArray(store);
            if (value < 0) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidArrayLength();
            }
            arrayLengthWrite.executeVoid(store, value);
            return true;
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected SetCacheNode createCachedPropertyNode(Property property, Object thisObj, int depth, Object value, SetCacheNode currentHead) {
        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            return createCachedPropertyNodeJSObject(property, (JSDynamicObject) thisObj, depth, value);
        } else {
            return createCachedPropertyNodeNotJSObject(property, thisObj, depth);
        }
    }

    private SetCacheNode createCachedPropertyNodeJSObject(Property property, JSDynamicObject thisObj, int depth, Object value) {
        Shape cacheShape = thisObj.getShape();
        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisObj, depth, false, false);

        if (JSProperty.isData(property)) {
            return createCachedDataPropertyNodeJSObject(thisObj, depth, value, shapeCheck, property);
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertySetNode(property, shapeCheck, isStrict());
        }
    }

    private SetCacheNode createCachedDataPropertyNodeJSObject(JSDynamicObject thisObj, int depth, Object value, AbstractShapeCheckNode shapeCheck, Property property) {
        assert !JSProperty.isConst(property) || (depth == 0 && isGlobal() && property.getLocation().isValue() && property.getLocation().get(null) == Dead.instance()) : "const assignment";
        if (!JSProperty.isWritable(property)) {
            return new ReadOnlyPropertySetNode(shapeCheck, isStrict());
        } else if (superProperty) {
            // define the property on the receiver; currently not handled, rewrite to generic
            return createGenericPropertyNode();
        } else if (depth > 0) {
            // define a new own property, shadowing an existing prototype property
            // NB: must have a guarding test that the inherited property is writable
            assert JSProperty.isWritable(property);
            return createUndefinedPropertyNode(thisObj, thisObj, depth, value);
        } else if (JSProperty.isProxy(property)) {
            if (isArrayLengthProperty(property) && JSArray.isJSFastArray(thisObj)) {
                return new ArrayLengthPropertySetNode(property, shapeCheck, isStrict());
            }
            return new PropertyProxySetNode(property, shapeCheck, isStrict());
        } else {
            assert JSProperty.isWritable(property) && depth == 0 && !JSProperty.isProxy(property);
            if (property.getLocation().isDeclared()) {
                return createRedefinePropertyNode(key, shapeCheck, shapeCheck.getShape(), property, value, context);
            } else if (!property.getLocation().canSet(value)) {
                return createCachedDataPropertyGeneralize(thisObj, depth);
            }

            if (property.getLocation() instanceof IntLocation) {
                return new IntPropertySetNode(property, shapeCheck);
            } else if (property.getLocation() instanceof DoubleLocation) {
                return new DoublePropertySetNode(property, shapeCheck);
            } else if (property.getLocation() instanceof BooleanLocation) {
                return new BooleanPropertySetNode(property, shapeCheck);
            } else {
                return new ObjectPropertySetNode(property, shapeCheck);
            }
        }
    }

    private static SetCacheNode createDefinePropertyNode(Object key, ReceiverCheckNode shapeCheck, Object value, JSContext context, int attributeFlags, boolean declaration) {
        Shape oldShape = shapeCheck.getShape();
        Shape newShape = declaration ? JSObjectUtil.shapeDefineDeclaredDataProperty(context, oldShape, key, value, attributeFlags)
                        : JSObjectUtil.shapeDefineDataProperty(context, oldShape, key, value, attributeFlags);
        return createResolvedDefinePropertyNode(key, shapeCheck, oldShape, newShape, attributeFlags);
    }

    private static SetCacheNode createRedefinePropertyNode(Object key, ReceiverCheckNode shapeCheck, Shape oldShape, Property property, Object value, JSContext context) {
        assert JSProperty.isData(property) && JSProperty.isWritable(property);
        assert property == oldShape.getProperty(key);

        Shape newShape = JSObjectUtil.shapeDefineDataProperty(context, oldShape, key, value, property.getFlags());
        return createResolvedDefinePropertyNode(key, shapeCheck, oldShape, newShape, property.getFlags());
    }

    private SetCacheNode createCachedDataPropertyGeneralize(JSDynamicObject thisObj, int depth) {
        Shape oldShape = thisObj.getShape();
        AbstractShapeCheckNode shapeCheck = createShapeCheckNode(oldShape, thisObj, depth, false, false);
        return new DataPropertySetNode(shapeCheck);
    }

    private SetCacheNode createCachedPropertyNodeNotJSObject(Property property, Object thisObj, int depth) {
        ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth);

        if (JSProperty.isData(property)) {
            return new ReadOnlyPropertySetNode(receiverCheck, isStrict());
        } else {
            assert JSProperty.isAccessor(property);
            return new AccessorPropertySetNode(property, receiverCheck, isStrict());
        }
    }

    private static SetCacheNode createResolvedDefinePropertyNode(Object key, ReceiverCheckNode receiverCheck, Shape oldShape, Shape newShape, int attributeFlags) {
        Property prop = newShape.getProperty(key);
        assert (prop.getFlags() & (JSAttributes.ATTRIBUTES_MASK | JSProperty.CONST)) == attributeFlags;

        return new DataPropertySetNode(key, receiverCheck, oldShape, newShape, prop);
    }

    @Override
    protected SetCacheNode createUndefinedPropertyNode(Object thisObj, Object store, int depth, Object value) {
        SetCacheNode specialized = createJavaPropertyNodeMaybe(thisObj, depth);
        if (specialized != null) {
            return specialized;
        }
        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            JSDynamicObject thisJSObj = (JSDynamicObject) thisObj;
            Shape cacheShape = thisJSObj.getShape();
            AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisJSObj, depth, false, true);
            if (JSAdapter.isJSAdapter(store)) {
                ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(thisJSObj)) : shapeCheck;
                return new JSAdapterPropertySetNode(receiverCheck);
            } else if (JSProxy.isJSProxy(store) && JSRuntime.isPropertyKey(key)) {
                ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(thisJSObj)) : shapeCheck;
                return new JSProxyDispatcherPropertySetNode(context, receiverCheck, isStrict());
            } else if (!JSRuntime.isObject(thisJSObj)) {
                return new TypeErrorPropertySetNode(shapeCheck);
            } else if (JSArrayBufferView.isJSArrayBufferView(store) && isNonIntegerIndex(key)) {
                return new ArrayBufferViewNonIntegerIndexSetNode(shapeCheck);
            } else if (superProperty) {
                // define the property on the receiver; currently not handled, rewrite to generic
                return createGenericPropertyNode();
            } else if (JSShape.isExtensible(cacheShape) || key instanceof HiddenKey) {
                return createDefinePropertyNode(key, shapeCheck, value, context, getAttributeFlags(), isDeclaration());
            } else {
                return new ReadOnlyPropertySetNode(createShapeCheckNode(cacheShape, thisJSObj, depth, false, false), isStrict());
            }
        } else if (JSProxy.isJSProxy(store)) {
            ReceiverCheckNode receiverCheck = createPrimitiveReceiverCheck(thisObj, depth);
            return new JSProxyDispatcherPropertySetNode(context, receiverCheck, isStrict());
        } else {
            boolean doThrow = isStrict();
            if (!JSRuntime.isJSNative(thisObj)) {
                // Nashorn never throws when setting inexistent properties on Java objects
                doThrow = false;
            }
            return new ReadOnlyPropertySetNode(new InstanceofCheckNode(thisObj.getClass(), context), doThrow);
        }
    }

    @Override
    protected SetCacheNode createJavaPropertyNodeMaybe(Object thisObj, int depth) {
        return null;
    }

    @Override
    protected SetCacheNode createGenericPropertyNode() {
        return new GenericPropertySetNode(context);
    }

    @Override
    protected boolean isGlobal() {
        return isGlobal;
    }

    @Override
    protected boolean isOwnProperty() {
        return setOwnProperty;
    }

    protected final boolean isStrict() {
        return this.isStrict;
    }

    protected final int getAttributeFlags() {
        return attributeFlags;
    }

    protected final boolean isDeclaration() {
        return declaration;
    }

    @Override
    protected SetCacheNode createTruffleObjectPropertyNode() {
        return new ForeignPropertySetNode(context);
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
