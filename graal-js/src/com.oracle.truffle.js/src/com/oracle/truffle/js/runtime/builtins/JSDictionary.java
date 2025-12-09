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

import static com.oracle.truffle.js.runtime.util.DefinePropertyUtil.nonConfigurableMessage;
import static com.oracle.truffle.js.runtime.util.DefinePropertyUtil.nonWritableMessage;
import static com.oracle.truffle.js.runtime.util.DefinePropertyUtil.notExtensibleMessage;
import static com.oracle.truffle.js.runtime.util.DefinePropertyUtil.reject;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

/**
 * This is a variant of {@link JSOrdinary} that stores its contents as a HashMap of properties
 * (excepts hidden properties, incl. prototype).
 */
public final class JSDictionary extends JSNonProxy {

    private static final HiddenKey HASHMAP_PROPERTY_NAME = new HiddenKey("%hashMap");

    public static final JSDictionary INSTANCE = new JSDictionary();

    private JSDictionary() {
    }

    public static boolean isJSDictionaryObject(JSDynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor desc = getHashMap(store).get(key);
        if (desc != null) {
            return getValue(desc, thisObj, encapsulatingNode);
        }

        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    public static Object getValue(PropertyDescriptor property, Object receiver, Node encapsulatingNode) {
        if (property.isAccessorDescriptor()) {
            Object getter = property.getGet();
            if (getter != Undefined.instance) {
                return JSRuntime.call(getter, receiver, JSArguments.EMPTY_ARGUMENTS_ARRAY, encapsulatingNode);
            } else {
                return Undefined.instance;
            }
        } else {
            assert property.isDataDescriptor();
            return property.getValue();
        }
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        assert isJSDictionaryObject(thisObj);
        List<Object> keys = ordinaryOwnPropertyKeysSlow(thisObj, strings, symbols);
        for (Object key : getHashMap(thisObj).getKeys()) {
            assert JSRuntime.isPropertyKey(key);
            if ((!symbols && key instanceof Symbol) || (!strings && key instanceof TruffleString)) {
                continue;
            }
            keys.add(key);
        }
        keys.sort(JSRuntime::comparePropertyKeys);
        return keys;
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        EconomicMap<Object, PropertyDescriptor> hashMap = getHashMap(thisObj);
        PropertyDescriptor desc = hashMap.get(key);
        if (desc != null) {
            if (!desc.getConfigurable()) {
                if (isStrict) {
                    throw Errors.createTypeErrorNotConfigurableProperty(key);
                }
                return false;
            }
            hashMap.removeKey(key);
            return true;
        }
        return super.delete(thisObj, key, isStrict);
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        return delete(thisObj, Strings.fromLong(index), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (getHashMap(thisObj).containsKey(key)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        Object key = Strings.fromLong(index);
        return dictionaryObjectSet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        return dictionaryObjectSet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    protected static boolean dictionaryObjectSet(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        PropertyDescriptor property = getHashMap(thisObj).get(key);
        if (property != null) {
            return setValue(key, property, thisObj, receiver, value, isStrict, encapsulatingNode);
        }
        Property entry = DefinePropertyUtil.getPropertyByKey(thisObj, key);
        if (entry != null) {
            return JSProperty.setValue(entry, thisObj, receiver, value, isStrict, encapsulatingNode);
        }
        return setPropertySlow(thisObj, key, value, receiver, isStrict, false, encapsulatingNode);
    }

    private static boolean setValue(Object key, PropertyDescriptor property, JSDynamicObject store, Object thisObj, Object value, boolean isStrict, Node encapsulatingNode) {
        if (property.isAccessorDescriptor()) {
            Object setter = property.getSet();
            if (setter != Undefined.instance) {
                JSRuntime.call(setter, thisObj, new Object[]{value}, encapsulatingNode);
                return true;
            } else if (isStrict) {
                throw Errors.createTypeErrorCannotSetAccessorProperty(key, store, encapsulatingNode);
            } else {
                return false;
            }
        } else {
            assert property.isDataDescriptor();
            if (property.getWritable()) {
                property.setValue(value);
                return true;
            } else {
                if (isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(key, thisObj, encapsulatingNode);
                }
                return false;
            }
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor prop = getHashMap(thisObj).get(key);
        if (prop != null) {
            return prop;
        }
        return super.getOwnProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor current = getHashMap(thisObj).get(key);
        if (current == null) {
            current = super.getOwnProperty(thisObj, key);
            boolean extensible = JSObject.isExtensible(thisObj);
            if (current == null) {
                if (!extensible) {
                    return reject(doThrow, notExtensibleMessage(key, doThrow));
                }
                validateAndPutDesc(thisObj, key, makeFullyPopulatedPropertyDescriptor(desc));
                return true;
            } else {
                return DefinePropertyUtil.validateAndApplyPropertyDescriptor(thisObj, key, extensible, desc, current, doThrow);
            }
        } else {
            return validateAndApplyPropertyDescriptorExisting(thisObj, key, desc, current, doThrow);
        }
    }

    private static PropertyDescriptor validateAndPutDesc(JSDynamicObject thisObj, Object key, PropertyDescriptor newDesc) {
        assert newDesc.isFullyPopulatedPropertyDescriptor();
        return getHashMap(thisObj).put(key, newDesc);
    }

    /**
     * Returns a fully populated property descriptor that is either an accessor property descriptor
     * or a data property descriptor that has all of the corresponding fields. Missing fields are
     * filled with default values.
     */
    private static PropertyDescriptor makeFullyPopulatedPropertyDescriptor(PropertyDescriptor desc) {
        if (desc.isAccessorDescriptor()) {
            if (desc.hasGet() && desc.hasSet() && desc.hasEnumerable() && desc.hasConfigurable()) {
                return desc;
            } else {
                return PropertyDescriptor.createAccessor(desc.getGet(), desc.getSet(), desc.getEnumerable(), desc.getConfigurable());
            }
        } else if (desc.isDataDescriptor()) {
            if (desc.hasValue() && desc.hasWritable() && desc.hasEnumerable() && desc.hasConfigurable()) {
                return desc;
            } else {
                Object value = desc.hasValue() ? desc.getValue() : Undefined.instance;
                return PropertyDescriptor.createData(value, desc.getEnumerable(), desc.getWritable(), desc.getConfigurable());
            }
        } else {
            assert desc.isGenericDescriptor();
            return PropertyDescriptor.createData(Undefined.instance, desc.getEnumerable(), desc.getWritable(), desc.getConfigurable());
        }
    }

    private static boolean validateAndApplyPropertyDescriptorExisting(JSDynamicObject thisObj, Object key, PropertyDescriptor descriptor, PropertyDescriptor currentDesc, boolean doThrow) {
        CompilerAsserts.neverPartOfCompilation();
        assert currentDesc.isFullyPopulatedPropertyDescriptor();
        if (descriptor.hasNoFields()) {
            return true;
        }

        if (!currentDesc.getConfigurable()) {
            if ((descriptor.hasConfigurable() && descriptor.getConfigurable()) ||
                            (descriptor.hasEnumerable() && (descriptor.getEnumerable() != currentDesc.getEnumerable()))) {
                return reject(doThrow, nonConfigurableMessage(key, doThrow));
            }
            if (!descriptor.isGenericDescriptor() && descriptor.isAccessorDescriptor() != currentDesc.isAccessorDescriptor()) {
                return reject(doThrow, nonConfigurableMessage(key, doThrow));
            }
            if (currentDesc.isAccessorDescriptor()) {
                if ((descriptor.hasGet() && !JSRuntime.isSameValue(descriptor.getGet(), currentDesc.getGet())) ||
                                (descriptor.hasSet() && !JSRuntime.isSameValue(descriptor.getSet(), currentDesc.getSet()))) {
                    return reject(doThrow, nonConfigurableMessage(key, doThrow));
                }
            } else {
                assert currentDesc.isDataDescriptor();
                if (!currentDesc.getWritable()) {
                    if (descriptor.hasWritable() && descriptor.getWritable()) {
                        return reject(doThrow, nonConfigurableMessage(key, doThrow));
                    }
                    if (descriptor.hasValue() && !JSRuntime.isSameValue(descriptor.getValue(), currentDesc.getValue())) {
                        return reject(doThrow, nonWritableMessage(key, doThrow));
                    }
                }
            }
        }

        if (currentDesc.isDataDescriptor() && descriptor.isAccessorDescriptor()) {
            PropertyDescriptor newDesc = PropertyDescriptor.createAccessor(descriptor.getGet(), descriptor.getSet(),
                            descriptor.getIfHasEnumerable(currentDesc.getEnumerable()),
                            descriptor.getIfHasConfigurable(currentDesc.getConfigurable()));
            validateAndPutDesc(thisObj, key, newDesc);
            return true;
        } else if (currentDesc.isAccessorDescriptor() && descriptor.isDataDescriptor()) {
            Object value = descriptor.hasValue() ? descriptor.getValue() : Undefined.instance;
            PropertyDescriptor newDesc = PropertyDescriptor.createData(value,
                            descriptor.getIfHasEnumerable(currentDesc.getEnumerable()),
                            descriptor.getIfHasConfigurable(currentDesc.getConfigurable()),
                            descriptor.getWritable());
            validateAndPutDesc(thisObj, key, newDesc);
            return true;
        } else {
            if (descriptor.hasConfigurable()) {
                currentDesc.setConfigurable(descriptor.getConfigurable());
            }
            if (descriptor.hasEnumerable()) {
                currentDesc.setEnumerable(descriptor.getEnumerable());
            }
            if (descriptor.hasWritable()) {
                currentDesc.setWritable(descriptor.getWritable());
            }
            if (descriptor.hasValue()) {
                currentDesc.setValue(descriptor.getValue());
            }
            if (descriptor.hasGet()) {
                currentDesc.setGet(descriptor.getGet());
            }
            if (descriptor.hasSet()) {
                currentDesc.setSet(descriptor.getSet());
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    static EconomicMap<Object, PropertyDescriptor> getHashMap(JSDynamicObject obj) {
        assert JSDictionary.isJSDictionaryObject(obj);
        return (EconomicMap<Object, PropertyDescriptor>) JSDynamicObject.getOrNull(obj, HASHMAP_PROPERTY_NAME);
    }

    @SuppressWarnings("deprecation")
    public static void makeDictionaryObject(JSDynamicObject obj, String reason) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSConfig.DictionaryObject;
        if (!JSOrdinary.isJSOrdinaryObject(obj)) {
            return;
        }

        if (JSConfig.TraceDictionaryObject) {
            System.out.printf("transitioning to dictionary object: %s\n%s\n", reason, obj.getShape());
        }

        Shape currentShape = obj.getShape();
        assert !isJSDictionaryObject(obj) && currentShape.getProperty(HASHMAP_PROPERTY_NAME) == null;
        JSContext context = JSObject.getJSContext(obj);
        Shape newRootShape = makeEmptyShapeForNewType(context, currentShape, JSDictionary.INSTANCE, obj);
        assert JSShape.hasExternalProperties(newRootShape.getFlags());

        List<Property> allProperties = currentShape.getPropertyListInternal(true);
        List<Object> archive = new ArrayList<>(allProperties.size());
        for (Property prop : allProperties) {
            Object key = prop.getKey();
            Object value = Properties.getOrDefaultUncached(obj, key, null);
            assert value != null;
            archive.add(value);
        }

        // Temporary workaround for GR-71599
        // DynamicObject.ResetShapeNode.getUncached().execute(obj, newRootShape);
        com.oracle.truffle.api.object.DynamicObjectLibrary.getUncached().resetShape(obj, newRootShape);

        int newFlags = currentShape.getFlags() | newRootShape.getFlags();
        if (newRootShape.getFlags() != newFlags) {
            DynamicObject.SetShapeFlagsNode.getUncached().execute(obj, newFlags);
        }

        EconomicMap<Object, PropertyDescriptor> hashMap = EconomicMap.create();
        for (int i = 0; i < archive.size(); i++) {
            Property p = allProperties.get(i);
            Object key = p.getKey();
            if (!newRootShape.hasProperty(key)) {
                Object value = archive.get(i);
                if (key instanceof HiddenKey || JSProperty.isProxy(p)) {
                    if (p.getLocation().isConstant()) {
                        Properties.putConstantUncached(obj, key, value, p.getFlags());
                    } else {
                        Properties.putWithFlagsUncached(obj, key, value, p.getFlags());
                    }
                } else {
                    hashMap.put(key, toPropertyDescriptor(p, value));
                }
            }
        }

        JSObjectUtil.putHiddenProperty(obj, HASHMAP_PROPERTY_NAME, hashMap);

        assert isJSDictionaryObject(obj) && obj.getShape().getProperty(HASHMAP_PROPERTY_NAME) != null;
    }

    private static Shape makeEmptyShapeForNewType(JSContext context, Shape currentShape, JSClass jsclass, JSDynamicObject fromObject) {
        Property prototypeProperty = JSShape.getPrototypeProperty(currentShape);
        if (!prototypeProperty.getLocation().isConstant()) {
            return context.makeEmptyShapeWithPrototypeInObject(jsclass);
        } else {
            JSDynamicObject prototype = JSObjectUtil.getPrototype(fromObject);
            if (prototype == Null.instance) {
                return context.makeEmptyShapeWithNullPrototype(jsclass);
            } else {
                return JSObjectUtil.getProtoChildShape(prototype, jsclass, context);
            }
        }
    }

    private static PropertyDescriptor toPropertyDescriptor(Property p, Object value) {
        PropertyDescriptor desc;
        if (JSProperty.isAccessor(p)) {
            desc = PropertyDescriptor.createAccessor(((Accessor) value).getGetter(), ((Accessor) value).getSetter());
            desc.setConfigurable(JSProperty.isConfigurable(p));
            desc.setEnumerable(JSProperty.isEnumerable(p));
        } else {
            assert JSProperty.isData(p) && !JSProperty.isDataSpecial(p) : p;
            desc = PropertyDescriptor.createData(value, JSProperty.isEnumerable(p), JSProperty.isWritable(p), JSProperty.isConfigurable(p));
        }
        return desc;
    }

    public static Shape makeDictionaryShape(JSContext context, JSDynamicObject prototype) {
        assert prototype != Null.instance;
        return JSObjectUtil.getProtoChildShape(prototype, JSDictionary.INSTANCE, context);
    }

    public static JSObject create(JSContext context, JSRealm realm) {
        JSObjectFactory factory = context.getDictionaryObjectFactory();
        JSObject obj = JSOrdinaryObject.create(factory.getShape(realm), factory.getPrototype(realm));
        factory.initProto(obj, realm);
        JSObjectUtil.putHiddenProperty(obj, HASHMAP_PROPERTY_NAME, newHashMap());
        return context.trackAllocation(obj);
    }

    private static EconomicMap<Object, PropertyDescriptor> newHashMap() {
        return EconomicMap.create();
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
