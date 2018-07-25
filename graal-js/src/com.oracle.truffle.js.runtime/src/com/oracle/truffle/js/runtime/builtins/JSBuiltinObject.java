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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public abstract class JSBuiltinObject extends JSClass {

    /**
     * If more than {@code threshold} properties are added to object, transition to a hash map.
     */
    private static final int DICTIONARY_TRANSITION_THRESHOLD = 400;
    private static final int DICTIONARY_TRANSITION_MAXIMUM = DICTIONARY_TRANSITION_THRESHOLD + 1;

    protected JSBuiltinObject() {
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        return DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, key, desc, doThrow);
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key) {
        Property entry = DefinePropertyUtil.getPropertyByKey(store, key);
        if (entry != null) {
            return JSProperty.getValue(entry, store, thisObj, false);
        } else {
            return null;
        }
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        return getOwnHelper(store, thisObj, String.valueOf(index));
    }

    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object thisObj, Object key) {
        Object value = getOwnHelper(store, thisObj, key);
        if (value != null) {
            return value;
        } else {
            return getPropertyHelperGeneric(thisObj, store, key);
        }
    }

    @TruffleBoundary
    private static Object getPropertyHelperGeneric(Object thisObj, DynamicObject store, Object key) {
        DynamicObject prototype = JSObject.getPrototype(store);
        if (prototype != Null.instance) {
            return JSObject.getJSClass(prototype).getHelper(prototype, thisObj, key);
        }
        return null;
    }

    @TruffleBoundary
    @Override
    public Object getHelper(DynamicObject store, Object thisObj, long index) {
        Object value = getOwnHelper(store, thisObj, index);
        if (value != null) {
            return value;
        } else {
            return getPropertyHelperGeneric(thisObj, store, index);
        }
    }

    @TruffleBoundary
    private static Object getPropertyHelperGeneric(Object thisObj, DynamicObject store, long index) {
        DynamicObject prototype = JSObject.getPrototype(store);
        if (prototype != Null.instance) {
            return JSObject.getJSClass(prototype).getHelper(prototype, thisObj, index);
        }
        return null;
    }

    @Override
    public Object getMethodHelper(DynamicObject store, Object thisObj, Object name) {
        return getHelper(store, thisObj, name);
    }

    @TruffleBoundary
    @Override
    public Iterable<Object> ownPropertyKeys(DynamicObject thisObj) {
        if (JSTruffleOptions.FastOwnKeys) {
            return IteratorUtil.convertIterable(JSShape.getProperties(thisObj.getShape()), Property::getKey);
        } else {
            return ownPropertyKeysList(thisObj);
        }
    }

    @TruffleBoundary
    protected List<Object> ownPropertyKeysList(DynamicObject thisObj) {
        List<Object> list = new ArrayList<>(thisObj.getShape().getPropertyCount());
        list.addAll(thisObj.getShape().getKeyList());
        Collections.sort(list, JSRuntime::comparePropertyKeys);
        return list;
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        return deletePropertyDefault(thisObj, key, isStrict);
    }

    protected static boolean deletePropertyDefault(DynamicObject object, Object key, boolean isStrict) {
        Property foundProperty = object.getShape().getProperty(key);
        if (foundProperty != null) {
            if (!JSProperty.isConfigurable(foundProperty)) {
                if (isStrict) {
                    throw Errors.createTypeErrorNotConfigurableProperty(key);
                }
                return false;
            }
            return object.delete(key);
        } else {
            /* the prototype might have a property with that name, but we don't care */
            return true;
        }
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return deletePropertyDefault(thisObj, String.valueOf(index), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        return thisObj.getShape().hasProperty(key);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, String.valueOf(index));
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, long index) {
        if (hasOwnProperty(thisObj, index)) {
            return true;
        }
        // shape does not call the object's overwritten function
        if (JSObject.getPrototype(thisObj) != Null.instance) {
            return JSObject.hasProperty(JSObject.getPrototype(thisObj), index);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, Object key) {
        if (hasOwnProperty(thisObj, key)) {
            return true;
        }
        // shape does not call the object's overwritten function
        DynamicObject prototype = JSObject.getPrototype(thisObj);
        if (prototype != Null.instance) {
            return JSObject.hasProperty(prototype, key);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        return setOwn(thisObj, String.valueOf(index), value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        return set(thisObj, String.valueOf(index), value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean setOwn(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        Property entry = DefinePropertyUtil.getPropertyByKey(thisObj, key);
        if (entry != null) {
            JSProperty.setValue(entry, thisObj, receiver, value, null, isStrict);
            return true;
        } else {
            return false;
        }
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        if (setOwn(thisObj, key, value, receiver, isStrict)) {
            return true;
        }
        return setPropertySlow(thisObj, key, value, receiver, isStrict);
    }

    private static boolean setPropertySlow(DynamicObject thisObj, Object name, Object value, Object receiver, boolean isStrict) {
        if (setPropertyPrototypes(thisObj, name, value, receiver, isStrict)) {
            return true;
        }

        if (!JSObject.isExtensible(thisObj)) {
            if (isStrict) {
                throw Errors.createTypeErrorNotExtensible(thisObj, name);
            }
            return true;
        }

        if (JSTruffleOptions.DictionaryObject) {
            boolean isDictionaryObject = JSDictionaryObject.isJSDictionaryObject(thisObj);
            if (!isDictionaryObject && isDictionaryObjectCandidate(thisObj, name)) {
                JSDictionaryObject.makeDictionaryObject(thisObj, "set");
                isDictionaryObject = true;
            }
            if (isDictionaryObject) {
                JSDictionaryObject.getHashMap(thisObj).put(name, PropertyDescriptor.createDataDefault(value));
                return true;
            }
        }

        // add it here
        JSObjectUtil.putDataProperty(JSObject.getJSContext(thisObj), thisObj, name, value, JSAttributes.getDefault());
        return true;
    }

    @TruffleBoundary
    private static boolean setPropertyPrototypes(DynamicObject thisObj, Object propertyKey, Object value, Object receiver, boolean isStrict) {
        // check prototype chain for accessors
        assert JSRuntime.isPropertyKey(propertyKey);
        DynamicObject current = JSObject.getPrototype(thisObj);
        while (current != Null.instance) {
            if (JSProxy.isProxy(current)) {
                return JSObject.setWithReceiver(current, propertyKey, value, receiver, false);
            } else {
                PropertyDescriptor desc = JSObject.getOwnProperty(current, propertyKey);
                if (desc != null) {
                    if (desc.isDataDescriptor() && !desc.getWritable()) {
                        if (isStrict) {
                            throw Errors.createTypeError("not writable");
                        }
                        return true;
                    } else if (desc.isAccessorDescriptor()) {
                        invokeAccessorPropertySetter(desc, thisObj, propertyKey, value, receiver, isStrict);
                        return true;
                    } else {
                        break;
                    }
                }
            }
            current = JSObject.getPrototype(current);
        }
        return false;
    }

    protected static void invokeAccessorPropertySetter(PropertyDescriptor desc, DynamicObject thisObj, Object propertyKey, Object value, Object receiver, boolean isStrict) {
        CompilerAsserts.neverPartOfCompilation();
        assert desc.isAccessorDescriptor();
        DynamicObject setter = (DynamicObject) desc.getSet();
        if (setter != Undefined.instance) {
            JSFunction.call(setter, receiver, new Object[]{value});
        } else if (isStrict) {
            throw Errors.createTypeErrorCannotSetAccessorProperty(propertyKey, thisObj);
        }
    }

    private static boolean isDictionaryObjectCandidate(DynamicObject thisObj, Object name) {
        if (!JSTruffleOptions.DictionaryObject) {
            return false;
        }

        if (!JSUserObject.isJSUserObject(thisObj)) {
            return false;
        }

        int count = thisObj.getShape().getPropertyCount();
        return (count >= DICTIONARY_TRANSITION_THRESHOLD && count < DICTIONARY_TRANSITION_MAXIMUM) || (count == 0 && JSRuntime.propertyKeyToIntegerIndex(name) != JSRuntime.INVALID_INTEGER_INDEX);
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object property) {
        return ordinaryGetOwnProperty(thisObj, property);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P).
     */
    public static PropertyDescriptor ordinaryGetOwnProperty(DynamicObject thisObj, Object property) {
        assert JSRuntime.isPropertyKey(property) || property instanceof HiddenKey;
        Property x = thisObj.getShape().getProperty(property);
        if (x == null) {
            return null;
        }
        return ordinaryGetOwnPropertyIntl(thisObj, property, x);
    }

    @TruffleBoundary
    protected static PropertyDescriptor ordinaryGetOwnPropertyIntl(DynamicObject thisObj, Object property, Property x) {
        PropertyDescriptor d = null;
        if (JSProperty.isData(x)) {
            d = PropertyDescriptor.createData(JSObject.get(thisObj, property));
            d.setWritable(JSProperty.isWritable(x));
        } else if (JSProperty.isAccessor(x)) {
            Accessor acc = (Accessor) x.get(thisObj, false);
            d = PropertyDescriptor.createAccessor(acc.getGetter(), acc.getSetter());
        } else {
            d = PropertyDescriptor.createEmpty();
        }
        d.setEnumerable(JSProperty.isEnumerable(x));
        d.setConfigurable(JSProperty.isConfigurable(x));
        return d;
    }

    @Override
    @TruffleBoundary
    public boolean setIntegrityLevel(DynamicObject obj, boolean freeze) {
        Shape oldShape = obj.getShape();
        Shape newShape = freeze ? JSShape.freeze(oldShape) : JSShape.seal(oldShape);
        if (oldShape != newShape) {
            obj.setShapeAndGrow(oldShape, newShape);
        }
        return JSObject.preventExtensions(obj);
    }

    @TruffleBoundary
    @Override
    public boolean preventExtensions(DynamicObject thisObj) {
        Shape oldShape = thisObj.getShape();
        Shape newShape = JSShape.makeNotExtensible(oldShape);
        if (oldShape != newShape) {
            thisObj.setShapeAndGrow(oldShape, newShape);
        }
        return true;
    }

    @Override
    public final boolean isExtensible(DynamicObject thisObj) {
        return JSShape.isExtensible(thisObj.getShape());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, getClassName(obj));
        }
    }

    @Override
    public final DynamicObject getPrototypeOf(DynamicObject thisObj) {
        return (DynamicObject) JSShape.getPrototypeProperty(thisObj.getShape()).get(thisObj, false);
    }

    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        return setPrototypeStatic(thisObj, newPrototype);
    }

    @TruffleBoundary
    static boolean setPrototypeStatic(DynamicObject thisObj, DynamicObject newPrototype) {
        if (!checkProtoCycle(thisObj, newPrototype)) {
            return false;
        }
        Shape shape = thisObj.getShape();
        Object oldPrototype = JSObject.getPrototype(thisObj);
        if (oldPrototype == newPrototype) {
            return true;
        }
        if (!JSShape.isExtensible(shape)) {
            return false;
        }
        if (JSShape.isPrototypeInShape(shape)) {
            JSObjectUtil.setPrototype(thisObj, newPrototype);
        } else {
            JSShape.getPrototypeProperty(shape).setSafe(thisObj, newPrototype, null);
        }
        return true;
    }

    private static boolean checkProtoCycle(DynamicObject thisObj, DynamicObject newPrototype) {
        DynamicObject check = newPrototype;
        while (check != Null.instance) {
            if (check == thisObj) {
                if (JSObject.getJSContext(thisObj).isOptionV8CompatibilityMode() || JSTruffleOptions.NashornCompatibilityMode) {
                    throw Errors.createTypeErrorProtoCycle(thisObj);
                }
                return false;
            }
            // 9.1.2.1 If p.[[GetPrototypeOf]] is not the ordinary object internal method
            if (JSProxy.isProxy(check)) {
                return true;
            }
            check = JSObject.getPrototype(check);
        }
        return true;
    }

    protected static void putConstructorSpeciesGetter(JSRealm realm, DynamicObject constructor) {
        JSObjectUtil.putConstantAccessorProperty(realm.getContext(), constructor, Symbol.SYMBOL_SPECIES, createSymbolSpeciesGetterFunction(realm), Undefined.instance);
    }

    protected static DynamicObject createSymbolSpeciesGetterFunction(JSRealm realm) {
        return JSFunction.create(realm, JSFunctionData.createCallOnly(realm.getContext(), realm.getContext().getSpeciesGetterFunctionCallTarget(), 0, "get [Symbol.species]"));
    }

    @Override
    public final ForeignAccess getForeignAccessFactory(DynamicObject object) {
        return JSObject.getJSContext(object).getInteropRuntime().getForeignAccessFactory();
    }

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        return "Object";
    }
}
