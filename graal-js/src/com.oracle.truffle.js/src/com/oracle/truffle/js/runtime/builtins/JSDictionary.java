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
package com.oracle.truffle.js.runtime.builtins;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
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
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

/**
 * This is a variant of {@link JSOrdinary} that stores its contents as a HashMap of properties
 * (excepts hidden properties, incl. prototype).
 */
public final class JSDictionary extends JSNonProxy {

    public static final String CLASS_NAME = "Object";

    private static final HiddenKey HASHMAP_PROPERTY_NAME = new HiddenKey("%hashMap");

    public static final JSDictionary INSTANCE = new JSDictionary();

    private JSDictionary() {
    }

    public static boolean isJSDictionaryObject(Object obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && isJSDictionaryObject((DynamicObject) obj);
    }

    public static boolean isJSDictionaryObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        return defaultToString(obj);
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        PropertyDescriptor desc = getHashMap(store).get(key);
        if (desc != null) {
            return getValue(desc, thisObj, encapsulatingNode);
        }

        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    public static Object getValue(PropertyDescriptor property, Object receiver, Node encapsulatingNode) {
        if (property.isAccessorDescriptor()) {
            DynamicObject getter = (DynamicObject) property.getGet();
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
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        assert isJSDictionaryObject(thisObj);
        List<Object> keys = ordinaryOwnPropertyKeysSlow(thisObj, strings, symbols);
        for (Object key : getHashMap(thisObj).getKeys()) {
            if ((!symbols && key instanceof Symbol) || (!strings && key instanceof String)) {
                continue;
            }
            keys.add(key);
        }
        Collections.sort(keys, JSRuntime::comparePropertyKeys);
        return keys;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
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
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return delete(thisObj, String.valueOf(index), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (getHashMap(thisObj).containsKey(key)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        Object key = Boundaries.stringValueOf(index);
        return dictionaryObjectSet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return dictionaryObjectSet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    protected static boolean dictionaryObjectSet(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
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

    private static boolean setValue(Object key, PropertyDescriptor property, DynamicObject store, Object thisObj, Object value, boolean isStrict, Node encapsulatingNode) {
        if (property.isAccessorDescriptor()) {
            DynamicObject setter = (DynamicObject) property.getSet();
            if (setter != Undefined.instance) {
                JSRuntime.call(setter, thisObj, new Object[]{value}, encapsulatingNode);
                return true;
            } else if (isStrict) {
                throw Errors.createTypeErrorCannotSetAccessorProperty(key, store);
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
                    throw Errors.createTypeErrorNotWritableProperty(key, thisObj);
                }
                return false;
            }
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor prop = getHashMap(thisObj).get(key);
        if (prop != null) {
            return prop;
        }
        return super.getOwnProperty(thisObj, key);
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        if (!hasOwnProperty(thisObj, key) && JSObject.isExtensible(thisObj)) {
            getHashMap(thisObj).put(key, desc);
            return true;
        }

        // defineProperty is currently not supported on dictionary objects,
        // so we need to convert back to a normal shape-based object.
        makeOrdinaryObject(thisObj, "defineOwnProperty");
        return super.defineOwnProperty(thisObj, key, desc, doThrow);
    }

    @SuppressWarnings("unchecked")
    static EconomicMap<Object, PropertyDescriptor> getHashMap(DynamicObject obj) {
        assert JSDictionary.isJSDictionaryObject(obj);
        Property hashMapProperty = obj.getShape().getProperty(HASHMAP_PROPERTY_NAME);
        return (EconomicMap<Object, PropertyDescriptor>) hashMapProperty.get(obj, false);
    }

    public static void makeDictionaryObject(DynamicObject obj, String reason) {
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

        DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();

        List<Property> allProperties = currentShape.getPropertyListInternal(true);
        List<Object> archive = new ArrayList<>(allProperties.size());
        for (Property prop : allProperties) {
            Object key = prop.getKey();
            Object value = lib.getOrDefault(obj, key, null);
            assert value != null;
            archive.add(value);
        }

        lib.resetShape(obj, newRootShape);

        EconomicMap<Object, PropertyDescriptor> hashMap = EconomicMap.create();
        for (int i = 0; i < archive.size(); i++) {
            Property p = allProperties.get(i);
            Object key = p.getKey();
            if (!newRootShape.hasProperty(key)) {
                Object value = archive.get(i);
                if (key instanceof HiddenKey || JSProperty.isProxy(p)) {
                    if (p.getLocation().isConstant()) {
                        lib.putConstant(obj, key, value, p.getFlags());
                    } else {
                        lib.putWithFlags(obj, key, value, p.getFlags());
                    }
                } else {
                    hashMap.put(key, toPropertyDescriptor(p, value));
                }
            }
        }

        JSObjectUtil.putHiddenProperty(obj, HASHMAP_PROPERTY_NAME, hashMap);

        assert isJSDictionaryObject(obj) && obj.getShape().getProperty(HASHMAP_PROPERTY_NAME) != null;
    }

    private static Shape makeEmptyShapeForNewType(JSContext context, Shape currentShape, JSClass jsclass, DynamicObject fromObject) {
        Property prototypeProperty = JSShape.getPrototypeProperty(currentShape);
        if (!prototypeProperty.getLocation().isConstant()) {
            return context.makeEmptyShapeWithPrototypeInObject(jsclass);
        } else {
            DynamicObject prototype = JSObjectUtil.getPrototype(fromObject);
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
            assert JSProperty.isData(p);
            desc = PropertyDescriptor.createData(value, JSProperty.isEnumerable(p), JSProperty.isWritable(p), JSProperty.isConfigurable(p));
        }
        return desc;
    }

    private static void makeOrdinaryObject(DynamicObject obj, String reason) {
        CompilerAsserts.neverPartOfCompilation();
        if (JSConfig.TraceDictionaryObject) {
            System.out.printf("transitioning from dictionary object to ordinary object: %s\n", reason);
        }

        EconomicMap<Object, PropertyDescriptor> hashMap = getHashMap(obj);
        Shape oldShape = obj.getShape();
        JSContext context = JSObject.getJSContext(obj);
        Shape newRootShape = makeEmptyShapeForNewType(context, oldShape, JSOrdinary.INSTANCE, obj);

        DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();

        List<Property> allProperties = oldShape.getPropertyListInternal(true);
        List<Map.Entry<Property, Object>> archive = new ArrayList<>(allProperties.size());
        for (Property prop : allProperties) {
            Object key = prop.getKey();
            Object value = lib.getOrDefault(obj, key, null);
            if (HASHMAP_PROPERTY_NAME.equals(key)) {
                continue;
            }
            archive.add(new AbstractMap.SimpleImmutableEntry<>(prop, value));
        }

        lib.resetShape(obj, newRootShape);

        for (int i = 0; i < archive.size(); i++) {
            Map.Entry<Property, Object> e = archive.get(i);
            Property p = e.getKey();
            Object key = p.getKey();
            if (!newRootShape.hasProperty(key)) {
                Object value = e.getValue();
                if (p.getLocation().isConstant()) {
                    lib.putConstant(obj, key, value, p.getFlags());
                } else {
                    lib.putWithFlags(obj, key, value, p.getFlags());
                }
            }
        }

        MapCursor<Object, PropertyDescriptor> cursor = hashMap.getEntries();
        while (cursor.advance()) {
            Object key = cursor.getKey();
            PropertyDescriptor desc = cursor.getValue();
            if (desc.isDataDescriptor()) {
                Object value = desc.getValue();
                assert !(value instanceof Accessor || value instanceof PropertyProxy);
                JSObjectUtil.defineDataProperty(obj, key, value, desc.getFlags());
            } else {
                JSObjectUtil.defineAccessorProperty(obj, key, new Accessor((DynamicObject) desc.getGet(), (DynamicObject) desc.getSet()), desc.getFlags());
            }
        }

        assert JSOrdinary.isJSOrdinaryObject(obj) && obj.getShape().getProperty(HASHMAP_PROPERTY_NAME) == null;
    }

    public static Shape makeDictionaryShape(JSContext context, DynamicObject prototype) {
        assert prototype != Null.instance;
        return JSObjectUtil.getProtoChildShape(prototype, JSDictionary.INSTANCE, context);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        JSObjectFactory factory = context.getDictionaryObjectFactory();
        DynamicObject obj = JSOrdinaryObject.create(factory.getShape(realm));
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
