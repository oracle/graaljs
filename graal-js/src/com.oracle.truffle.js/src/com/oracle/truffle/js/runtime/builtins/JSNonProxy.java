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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.Accessor;
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
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

/**
 * Common base class for non-proxy object types.
 *
 * @see com.oracle.truffle.js.runtime.objects.JSNonProxyObject
 */
public abstract class JSNonProxy extends JSClass {

    public static final TruffleString GET_SYMBOL_SPECIES_NAME = Strings.constant("get [Symbol.species]");

    protected JSNonProxy() {
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        return ordinaryDefineOwnProperty(thisObj, key, desc, doThrow);
    }

    /**
     * This is a copy of OrdinaryDefineOwnProperty that avoids unnecessary virtual dispatch for
     * [[GetOwnProperty]] and [[IsExtensible]]. For all non-Proxy objects, [[GetOwnProperty]] and
     * [[IsExtensible]] must never have a side effect that changes the JSClass.
     *
     * @see DefinePropertyUtil#ordinaryDefineOwnProperty
     */
    private boolean ordinaryDefineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        PropertyDescriptor current = getOwnProperty(thisObj, key);
        boolean extensible = isExtensible(thisObj);
        assert thisObj.getJSClass() == this;
        return DefinePropertyUtil.validateAndApplyPropertyDescriptor(thisObj, key, extensible, desc, current, doThrow);
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        Property entry = DefinePropertyUtil.getPropertyByKey(store, key);
        if (entry != null) {
            return JSProperty.getValue(entry, store, thisObj, encapsulatingNode);
        } else {
            return null;
        }
    }

    /**
     * Like getOwnProperty, but returns {@code null} if value is not present instead of undefined.
     */
    @TruffleBoundary
    @Override
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        return getOwnHelper(store, thisObj, Strings.fromLong(index), encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        Object value = getOwnHelper(store, thisObj, key, encapsulatingNode);
        if (value != null || JSRuntime.isPrivateSymbol(key)) {
            return value;
        } else {
            return getPropertyHelperGeneric(thisObj, store, key, encapsulatingNode);
        }
    }

    @TruffleBoundary
    private static Object getPropertyHelperGeneric(Object thisObj, JSDynamicObject store, Object key, Node encapsulatingNode) {
        JSDynamicObject prototype = JSObject.getPrototype(store);
        if (prototype != Null.instance) {
            return JSObject.getJSClass(prototype).getHelper(prototype, thisObj, key, encapsulatingNode);
        }
        return null;
    }

    @TruffleBoundary
    @Override
    public Object getHelper(JSDynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        Object value = getOwnHelper(store, thisObj, index, encapsulatingNode);
        if (value != null) {
            return value;
        } else {
            return getPropertyHelperGeneric(thisObj, store, index, encapsulatingNode);
        }
    }

    @TruffleBoundary
    private static Object getPropertyHelperGeneric(Object thisObj, JSDynamicObject store, long index, Node encapsulatingNode) {
        JSDynamicObject prototype = JSObject.getPrototype(store);
        if (prototype != Null.instance) {
            return JSObject.getJSClass(prototype).getHelper(prototype, thisObj, index, encapsulatingNode);
        }
        return null;
    }

    @Override
    public Object getMethodHelper(JSDynamicObject store, Object thisObj, Object name, Node encapsulatingNode) {
        return getHelper(store, thisObj, name, encapsulatingNode);
    }

    @Override
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        return ordinaryOwnPropertyKeys(thisObj, strings, symbols);
    }

    public static List<Object> ordinaryOwnPropertyKeys(JSDynamicObject thisObj) {
        return ordinaryOwnPropertyKeys(thisObj, true, true);
    }

    @TruffleBoundary
    protected static List<Object> ordinaryOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        if (JSConfig.FastOwnKeys) {
            return JSShape.getPropertyKeyList(thisObj.getShape(), strings, symbols);
        } else {
            return ordinaryOwnPropertyKeysSlow(thisObj, strings, symbols);
        }
    }

    protected static List<Object> ordinaryOwnPropertyKeysSlow(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        CompilerAsserts.neverPartOfCompilation();
        List<Object> keyList = thisObj.getShape().getKeyList();
        List<Object> list = new ArrayList<>(keyList.size());
        for (Object key : keyList) {
            if ((!symbols && key instanceof Symbol) || (!strings && key instanceof TruffleString)) {
                continue;
            }
            list.add(key);
        }
        list.sort(JSRuntime::comparePropertyKeys);
        return list;
    }

    @Override
    public boolean hasOnlyShapeProperties(JSDynamicObject obj) {
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        return deletePropertyDefault(thisObj, key, isStrict);
    }

    protected static boolean deletePropertyDefault(JSDynamicObject object, Object key, boolean isStrict) {
        Property foundProperty = object.getShape().getProperty(key);
        if (foundProperty != null) {
            if (!JSProperty.isConfigurable(foundProperty)) {
                if (isStrict) {
                    throw Errors.createTypeErrorNotConfigurableProperty(key);
                }
                return false;
            }
            return Properties.removeKeyUncached(object, key);
        } else {
            /* the prototype might have a property with that name, but we don't care */
            return true;
        }
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        return deletePropertyDefault(thisObj, Strings.fromLong(index), isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        return thisObj.getShape().hasProperty(key);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(JSDynamicObject thisObj, long index) {
        return hasOwnProperty(thisObj, Strings.fromLong(index));
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(JSDynamicObject thisObj, long index) {
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
    public boolean hasProperty(JSDynamicObject thisObj, Object key) {
        if (hasOwnProperty(thisObj, key)) {
            return true;
        }
        // shape does not call the object's overwritten function
        JSDynamicObject prototype = JSObject.getPrototype(thisObj);
        if (prototype != Null.instance) {
            return JSObject.hasProperty(prototype, key);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return ordinarySetIndex(thisObj, index, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return ordinarySet(thisObj, key, value, receiver, isStrict, encapsulatingNode);
    }

    protected static boolean ordinarySetIndex(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        Object key = Strings.fromLong(index);
        if (receiver != thisObj) {
            // OrdinarySet: set the property on the receiver instead
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        Property entry = DefinePropertyUtil.getPropertyByKey(thisObj, key);
        if (entry != null) {
            return JSProperty.setValue(entry, thisObj, receiver, value, isStrict, encapsulatingNode);
        }
        return setPropertySlow(thisObj, key, value, receiver, isStrict, true, encapsulatingNode);
    }

    protected static boolean ordinarySet(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (receiver != thisObj) {
            // OrdinarySet: set the property on the receiver instead
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        Property entry = DefinePropertyUtil.getPropertyByKey(thisObj, key);
        if (entry != null) {
            return JSProperty.setValue(entry, thisObj, receiver, value, isStrict, encapsulatingNode);
        }
        return setPropertySlow(thisObj, key, value, receiver, isStrict, false, encapsulatingNode);
    }

    protected static boolean ordinarySetWithReceiver(JSDynamicObject target, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor descriptor = JSObject.getOwnProperty(target, key);
        boolean result = performOrdinarySetWithOwnDescriptor(target, key, value, receiver, descriptor, isStrict, encapsulatingNode);
        assert !isStrict || result : "should have thrown";
        return result;
    }

    @TruffleBoundary
    protected static boolean performOrdinarySetWithOwnDescriptor(JSDynamicObject target, Object key, Object value, Object receiver, PropertyDescriptor desc, boolean isStrict, Node encapsulatingNode) {
        PropertyDescriptor descriptor = desc;
        if (descriptor == null) {
            JSDynamicObject parent = JSObject.getPrototype(target);
            if (parent != Null.instance) {
                return JSObject.getJSClass(parent).set(parent, key, value, receiver, isStrict, encapsulatingNode);
            } else {
                descriptor = PropertyDescriptor.undefinedDataDesc;
            }
        }
        if (descriptor.isDataDescriptor()) {
            if (!descriptor.getWritable()) {
                if (isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(key, target, encapsulatingNode);
                }
                return false;
            }
            if (!JSRuntime.isObject(receiver)) {
                if (isStrict) {
                    throw Errors.createTypeErrorSetNonObjectReceiver(receiver, key);
                }
                return false;
            }
            JSDynamicObject receiverObj = (JSDynamicObject) receiver;
            PropertyDescriptor existingDesc = JSObject.getOwnProperty(receiverObj, key);
            if (existingDesc != null) {
                if (existingDesc.isAccessorDescriptor()) {
                    if (isStrict) {
                        throw Errors.createTypeErrorCannotRedefineProperty(key);
                    }
                    return false;
                }
                if (!existingDesc.getWritable()) {
                    if (isStrict) {
                        throw Errors.createTypeErrorNotWritableProperty(key, receiverObj, encapsulatingNode);
                    }
                    return false;
                }
                PropertyDescriptor valueDesc = PropertyDescriptor.createData(value);
                return JSObject.defineOwnProperty(receiverObj, key, valueDesc, isStrict);
            } else {
                return JSRuntime.createDataProperty(receiverObj, key, value, isStrict);
            }
        } else {
            assert descriptor.isAccessorDescriptor();
            Object setter = descriptor.getSet();
            if (setter == Undefined.instance || setter == null) {
                if (isStrict) {
                    throw Errors.createTypeErrorCannotSetAccessorProperty(key, target, encapsulatingNode);
                }
                return false;
            }
            JSRuntime.call(setter, receiver, new Object[]{value}, encapsulatingNode);
            return true;
        }
    }

    @TruffleBoundary
    protected static boolean setPropertySlow(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, boolean isIndex, Node encapsulatingNode) {
        // check prototype chain for accessors
        assert JSRuntime.isPropertyKey(key);
        JSDynamicObject current = JSObject.getPrototype(thisObj);
        while (current != Null.instance) {
            if (JSProxy.isJSProxy(current) || JSArrayBufferView.isJSArrayBufferView(current)) {
                return JSObject.getJSClass(current).set(current, key, value, receiver, isStrict, encapsulatingNode);
            } else {
                PropertyDescriptor desc = JSObject.getOwnProperty(current, key);
                if (desc != null) {
                    if (desc.isDataDescriptor() && !desc.getWritable()) {
                        if (isStrict) {
                            throw Errors.createTypeErrorNotWritableProperty(key, current, encapsulatingNode);
                        }
                        return false;
                    } else if (desc.isAccessorDescriptor()) {
                        return invokeAccessorPropertySetter(desc, thisObj, key, value, receiver, isStrict, encapsulatingNode);
                    } else {
                        break;
                    }
                }
            }
            current = JSObject.getPrototype(current);
        }

        assert thisObj == receiver;
        JSDynamicObject receiverObj = (JSDynamicObject) receiver;
        if (!JSObject.isExtensible(receiverObj)) {
            if (isStrict) {
                throw Errors.createTypeErrorNotExtensible(receiverObj, key);
            }
            return false;
        }

        JSContext context = JSObject.getJSContext(thisObj);
        if (JSShape.hasNoElementsAssumption(thisObj)) {
            if (context.getArrayPrototypeNoElementsAssumption().isValid() && (isIndex || JSRuntime.isArrayIndex(key))) {
                context.getArrayPrototypeNoElementsAssumption().invalidate("Set element on an Array prototype");
            }
        }

        if (JSConfig.DictionaryObject) {
            boolean isDictionaryObject = JSDictionary.isJSDictionaryObject(thisObj);
            if (!isDictionaryObject && isDictionaryObjectCandidate(thisObj, isIndex)) {
                JSDictionary.makeDictionaryObject(thisObj, "set");
                isDictionaryObject = true;
            }
            if (isDictionaryObject) {
                JSDictionary.getHashMap(thisObj).put(key, PropertyDescriptor.createDataDefault(value));
                return true;
            }
        }

        // add it here
        JSObjectUtil.defineDataProperty(context, thisObj, key, value, JSAttributes.getDefault());
        return true;
    }

    protected static boolean invokeAccessorPropertySetter(PropertyDescriptor desc, JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert desc.isAccessorDescriptor();
        Object setter = desc.getSet();
        if (setter != Undefined.instance) {
            JSRuntime.call(setter, receiver, new Object[]{value}, encapsulatingNode);
            return true;
        } else if (isStrict) {
            throw Errors.createTypeErrorCannotSetAccessorProperty(key, thisObj, encapsulatingNode);
        } else {
            return false;
        }
    }

    private static boolean isDictionaryObjectCandidate(JSDynamicObject thisObj, boolean isIndex) {
        if (!JSConfig.DictionaryObject) {
            return false;
        }

        if (!JSOrdinary.isJSOrdinaryObject(thisObj)) {
            return false;
        }

        int count = thisObj.getShape().getPropertyCount();
        return (count == 0 && isIndex) || (count == JSConfig.DictionaryObjectTransitionThreshold);
    }

    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        return ordinaryGetOwnProperty(thisObj, key);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P).
     */
    public static PropertyDescriptor ordinaryGetOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        Property prop = thisObj.getShape().getProperty(key);
        if (prop == null) {
            return null;
        }
        return ordinaryGetOwnPropertyIntl(thisObj, key, prop);
    }

    @TruffleBoundary
    public static PropertyDescriptor ordinaryGetOwnPropertyIntl(JSDynamicObject thisObj, Object key, Property prop) {
        PropertyDescriptor desc;
        if (JSProperty.isData(prop)) {
            Object value = JSDynamicObject.getOrNull(thisObj, key);
            if (JSProperty.isProxy(prop)) {
                value = ((PropertyProxy) value).get(thisObj);
            } else {
                assert !JSProperty.isDataSpecial(prop) : prop;
            }
            desc = PropertyDescriptor.createData(value);
            desc.setWritable(JSProperty.isWritable(prop));
        } else if (JSProperty.isAccessor(prop)) {
            Accessor acc = (Accessor) JSDynamicObject.getOrNull(thisObj, key);
            desc = PropertyDescriptor.createAccessor(acc.getGetter(), acc.getSetter());
        } else {
            desc = PropertyDescriptor.createEmpty();
        }
        desc.setEnumerable(JSProperty.isEnumerable(prop));
        desc.setConfigurable(JSProperty.isConfigurable(prop));
        return desc;
    }

    @TruffleBoundary
    public static boolean setIntegrityLevelFast(JSDynamicObject thisObj, boolean freeze) {
        if (testIntegrityLevelFast(thisObj, freeze)) {
            return true;
        }

        for (Property property : JSDynamicObject.getPropertyArray(thisObj)) {
            if (!property.isHidden()) {
                int oldFlags = property.getFlags();
                int newFlags = oldFlags | JSAttributes.NOT_CONFIGURABLE;
                if (freeze && ((oldFlags & JSProperty.ACCESSOR) == 0)) {
                    newFlags |= JSAttributes.NOT_WRITABLE;
                }
                if (newFlags != oldFlags) {
                    Object key = property.getKey();
                    JSDynamicObject.setPropertyFlags(thisObj, key, newFlags);
                    assert JSDynamicObject.getPropertyFlags(thisObj, key) == newFlags;
                }
            }
        }
        assert testSealedProperties(thisObj) && (!freeze || testFrozenProperties(thisObj));
        boolean result = ordinaryPreventExtensions(thisObj, JSShape.SEALED_FLAG | (freeze ? JSShape.FROZEN_FLAG : 0));
        assert result && thisObj.testIntegrityLevel(freeze);
        return true;
    }

    public static boolean testIntegrityLevelFast(JSDynamicObject obj, boolean frozen) {
        int objectFlags = JSDynamicObject.getObjectFlags(obj);
        if (frozen) {
            if ((objectFlags & JSShape.FROZEN_FLAG) != 0) {
                return true;
            }
        } else {
            if ((objectFlags & JSShape.SEALED_FLAG) != 0) {
                return true;
            }
        }
        if ((objectFlags & JSShape.NOT_EXTENSIBLE_FLAG) != 0) {
            return testSealedProperties(obj) && (!frozen || testFrozenProperties(obj));
        } else {
            return false;
        }
    }

    @TruffleBoundary
    @Override
    public boolean preventExtensions(JSDynamicObject thisObj, boolean doThrow) {
        return ordinaryPreventExtensions(thisObj, 0);
    }

    public static boolean ordinaryPreventExtensions(JSDynamicObject thisObj, int extraFlags) {
        int objectFlags = JSDynamicObject.getObjectFlags(thisObj);
        if ((objectFlags & JSShape.NOT_EXTENSIBLE_FLAG) != 0 && ((objectFlags & extraFlags) == extraFlags)) {
            return true;
        }

        int newFlags = objectFlags | JSShape.NOT_EXTENSIBLE_FLAG | extraFlags;
        // add sealed/frozen flags if properties already have appropriate attributes.
        if ((newFlags & JSShape.SEALED_FLAG) == 0 && testSealedProperties(thisObj)) {
            newFlags |= JSShape.SEALED_FLAG;
        }
        if ((newFlags & JSShape.SEALED_FLAG) != 0 && (newFlags & JSShape.FROZEN_FLAG) == 0 && testFrozenProperties(thisObj)) {
            newFlags |= JSShape.FROZEN_FLAG;
        }
        if (newFlags != objectFlags) {
            JSDynamicObject.setObjectFlags(thisObj, newFlags);
        }
        assert !thisObj.isExtensible();
        return true;
    }

    @TruffleBoundary
    private static boolean testSealedProperties(JSDynamicObject thisObj) {
        return JSDynamicObject.testProperties(thisObj, p -> p.isHidden() || (p.getFlags() & JSAttributes.NOT_CONFIGURABLE) != 0);
    }

    @TruffleBoundary
    private static boolean testFrozenProperties(JSDynamicObject thisObj) {
        return JSDynamicObject.testProperties(thisObj, p -> p.isHidden() || (p.getFlags() & JSProperty.ACCESSOR) != 0 || (p.getFlags() & JSAttributes.NOT_WRITABLE) != 0);
    }

    @Override
    public final boolean isExtensible(JSDynamicObject thisObj) {
        return ordinaryIsExtensible(thisObj);
    }

    public static boolean ordinaryIsExtensible(JSDynamicObject thisObj) {
        assert thisObj.getJSClass().usesOrdinaryIsExtensible() : thisObj;
        return JSShape.isExtensible(thisObj.getShape());
    }

    @TruffleBoundary
    @Override
    public final JSDynamicObject getPrototypeOf(JSDynamicObject thisObj) {
        return JSObjectUtil.getPrototype(thisObj);
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        return setPrototypeStatic(thisObj, newPrototype);
    }

    @TruffleBoundary
    static boolean setPrototypeStatic(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        Object oldPrototype = JSObject.getPrototype(thisObj);
        if (oldPrototype == newPrototype) {
            return true;
        }
        if (!checkProtoCycle(thisObj, newPrototype)) {
            return false;
        }
        Shape shape = thisObj.getShape();
        if (!JSShape.isExtensible(shape)) {
            return false;
        }
        if (JSShape.isPrototypeInShape(shape)) {
            JSObjectUtil.setPrototypeImpl(thisObj, newPrototype);
        } else {
            boolean success = Properties.putIfPresentUncached(thisObj, JSObject.HIDDEN_PROTO, newPrototype);
            assert success;
        }
        validatePrototypeAssumptions(thisObj, newPrototype);
        return true;
    }

    /**
     * If this object is an Array instance, the %Array.prototype%, or an Array subclass prototype,
     * changing the prototype invalidates the assumption that there are no elements on the prototype
     * chain of arrays, unless the new prototype is either already marked and vetted, or null.
     */
    private static void validatePrototypeAssumptions(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        if (JSShape.isArrayPrototypeOrDerivative(thisObj) || JSArray.isJSArray(thisObj)) {
            /*
             * Setting the prototype to *null* is always OK. If [[SetPrototypeOf]] is ever called
             * with another prototype object, we'll repeat this check anyway.
             */
            if (newPrototype != Null.instance && !JSShape.hasNoElementsAssumption(newPrototype)) {
                String reason = JSShape.isArrayPrototypeOrDerivative(newPrototype) ? "Prototype of Array prototype changed" : "Prototype of Array changed";
                JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate(reason);
            }
        }
    }

    public static boolean checkProtoCycle(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        JSDynamicObject proto = newPrototype;
        while (proto != Null.instance) {
            if (proto == thisObj) {
                return false;
            }
            // 9.1.2.1 If p.[[GetPrototypeOf]] is not the ordinary object internal method
            if (JSProxy.isJSProxy(proto)) {
                return true;
            }
            proto = JSObject.getPrototype(proto);
        }
        return true;
    }

    protected static void putConstructorSpeciesGetter(JSRealm realm, JSDynamicObject constructor) {
        JSObjectUtil.putBuiltinAccessorProperty(constructor, Symbol.SYMBOL_SPECIES, createSymbolSpeciesGetterFunction(realm), Undefined.instance);
    }

    protected static JSDynamicObject createSymbolSpeciesGetterFunction(JSRealm realm) {
        return JSFunction.create(realm, realm.getContext().getSymbolSpeciesThisGetterFunctionData());
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return true;
    }

    @Override
    public final boolean usesOrdinaryIsExtensible() {
        return true;
    }
}
