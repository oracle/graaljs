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
package com.oracle.truffle.js.runtime.builtins;

import java.util.Collections;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This is a variant of {@link JSUserObject} that stores its contents as a HashMap of properties
 * (excepts hidden properties, incl. prototype).
 */
public final class JSDictionaryObject extends JSBuiltinObject {

    public static final String CLASS_NAME = "Object";

    private static final HiddenKey HASHMAP_PROPERTY_NAME = new HiddenKey("%hashMap");
    private static final Property HASHMAP_PROPERTY;

    public static final JSDictionaryObject INSTANCE = new JSDictionaryObject();

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        HASHMAP_PROPERTY = JSObjectUtil.makeHiddenProperty(HASHMAP_PROPERTY_NAME, allocator.locationForType(EconomicMap.class));
    }

    private JSDictionaryObject() {
    }

    public static boolean isJSDictionaryObject(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSDictionaryObject((DynamicObject) obj);
    }

    public static boolean isJSDictionaryObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String safeToString(DynamicObject obj) {
        return defaultToString(obj);
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key) {
        PropertyDescriptor desc = getHashMap(store).get(key);
        if (desc != null) {
            return getValue(desc, thisObj);
        }

        return super.getOwnHelper(store, thisObj, key);
    }

    public static Object getValue(PropertyDescriptor property, Object receiver) {
        if (property.isAccessorDescriptor()) {
            DynamicObject getter = (DynamicObject) property.getGet();
            if (getter != Undefined.instance) {
                return JSFunction.call(getter, receiver, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            } else {
                return Undefined.instance;
            }
        } else {
            assert property.isDataDescriptor();
            return property.getValue();
        }
    }

    @Override
    @TruffleBoundary
    public List<Object> ownPropertyKeys(DynamicObject thisObj) {
        assert isJSDictionaryObject(thisObj);
        List<Object> keys = super.ownPropertyKeysList(thisObj);
        for (Object key : getHashMap(thisObj).getKeys()) {
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
    public boolean setOwn(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        EconomicMap<Object, PropertyDescriptor> hashMap = getHashMap(thisObj);
        PropertyDescriptor property = hashMap.get(key);
        if (property != null) {
            setValue(key, property, thisObj, receiver, value, isStrict);
            return true;
        }

        return super.setOwn(thisObj, key, value, receiver, isStrict);
    }

    private static void setValue(Object key, PropertyDescriptor property, DynamicObject store, Object thisObj, Object value, boolean isStrict) {
        if (property.isAccessorDescriptor()) {
            DynamicObject setter = (DynamicObject) property.getSet();
            if (setter != Undefined.instance) {
                JSFunction.call(setter, thisObj, new Object[]{value});
            } else if (isStrict) {
                throw Errors.createTypeErrorCannotSetAccessorProperty(key, store);
            }
        } else {
            assert property.isDataDescriptor();
            if (property.getWritable()) {
                property.setValue(value);
            } else {
                if (isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(key, thisObj);
                }
            }
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
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
        assert JSDictionaryObject.isJSDictionaryObject(obj);
        Property hashMapProperty = obj.getShape().getProperty(HASHMAP_PROPERTY_NAME);
        return (EconomicMap<Object, PropertyDescriptor>) hashMapProperty.get(obj, false);
    }

    public static void makeDictionaryObject(DynamicObject obj, String reason) {
        CompilerAsserts.neverPartOfCompilation();
        if (!JSTruffleOptions.DictionaryObject) {
            return;
        }

        if (JSTruffleOptions.TraceDictionaryObject) {
            System.out.printf("transitioning to dictionary object: %s\n%s\n", reason, obj.getShape());
        }

        Shape currentShape = obj.getShape();
        assert !isJSDictionaryObject(obj) && currentShape.getProperty(HASHMAP_PROPERTY_NAME) == null;
        Property prototypeProperty = JSShape.getPrototypeProperty(currentShape);
        Shape hashedShape;
        DynamicObject prototype = (DynamicObject) prototypeProperty.get(obj, false);
        Property hashMapProperty = null;
        if (prototype == null) {
            JSContext context = JSObject.getJSContext(obj);
            hashedShape = context.getDictionaryShapeNullPrototype();
            hashMapProperty = hashedShape.getProperty(HASHMAP_PROPERTY_NAME);
        } else {
            hashedShape = JSShape.makeUniqueRoot(currentShape.getLayout(), JSDictionaryObject.INSTANCE, JSShape.getJSContext(currentShape), prototypeProperty);
        }

        List<Property> properties = currentShape.getPropertyListInternal(true);
        EconomicMap<Object, PropertyDescriptor> hashMap = EconomicMap.create();
        for (Property p : properties) {
            if (p.equals(prototypeProperty)) {
                continue; // has already been added
            } else if (p.isHidden() || p.getLocation().isValue()) {
                hashedShape = hashedShape.addProperty(p);
            } else {
                // normal properties
                Object value = p.get(obj, false);
                hashMap.put(p.getKey(), toPropertyDescriptor(p, value));
            }
        }

        if (hashMapProperty == null) {
            hashedShape = hashedShape.addProperty(JSObjectUtil.makeHiddenProperty(HASHMAP_PROPERTY_NAME, hashedShape.allocator().locationForType(hashMap.getClass()), true));
            hashMapProperty = hashedShape.getLastProperty();
        }
        assert isHashMapProperty(hashMapProperty);

        obj.setShapeAndResize(currentShape, hashedShape);

        hashMapProperty.setSafe(obj, hashMap, null);

        // invalidate property assumptions (rewrite assumption check nodes for final properties)
        for (Object key : hashMap.getKeys()) {
            JSShape.invalidatePropertyAssumption(currentShape, key);
        }

        assert isJSDictionaryObject(obj) && obj.getShape().getProperty(HASHMAP_PROPERTY_NAME) != null;
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
        if (JSTruffleOptions.TraceDictionaryObject) {
            System.out.printf("transitioning from dictionary object to ordinary object: %s\n", reason);
        }

        EconomicMap<Object, PropertyDescriptor> hashMap = getHashMap(obj);
        Shape oldShape = obj.getShape();
        Property prototypeProperty = JSShape.getPrototypeProperty(oldShape);
        Shape newShape = JSShape.makeUniqueRoot(oldShape.getLayout(), JSUserObject.INSTANCE, JSShape.getJSContext(oldShape), prototypeProperty);

        List<Property> properties = oldShape.getPropertyListInternal(true);
        for (Property p : properties) {
            if (p.equals(prototypeProperty)) {
                continue; // has already been added
            } else if (!p.getKey().equals(HASHMAP_PROPERTY_NAME)) {
                newShape = newShape.addProperty(p);
            }
        }
        obj.setShapeAndGrow(oldShape, newShape);

        MapCursor<Object, PropertyDescriptor> cursor = hashMap.getEntries();
        while (cursor.advance()) {
            Object key = cursor.getKey();
            PropertyDescriptor desc = cursor.getValue();
            if (desc.isDataDescriptor()) {
                JSObjectUtil.defineDataProperty(obj, key, desc.getValue(), desc.getFlags());
            } else {
                JSObjectUtil.defineAccessorProperty(obj, key, new Accessor((DynamicObject) desc.getGet(), (DynamicObject) desc.getSet()), desc.getFlags());
            }
        }

        assert JSUserObject.isJSUserObject(obj) && obj.getShape().getProperty(HASHMAP_PROPERTY_NAME) == null;
    }

    private static boolean isHashMapProperty(Property property) {
        return property != null && property.getKey() == HASHMAP_PROPERTY_NAME;
    }

    public static Shape makeDictionaryShape(JSContext context, DynamicObject prototype) {
        Shape emptyShape;
        if (prototype == null) {
            emptyShape = JSShape.makeEmptyRoot(JSObject.LAYOUT, JSDictionaryObject.INSTANCE, context);
        } else {
            emptyShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        }
        return emptyShape.addProperty(HASHMAP_PROPERTY);
    }

    public static DynamicObject create(JSContext context) {
        Shape shape = context.getRealm().getDictionaryShapeObjectPrototype();
        DynamicObject object = JSObject.create(context, shape);
        HASHMAP_PROPERTY.setSafe(object, EconomicMap.create(), shape);
        return object;
    }
}
