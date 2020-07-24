/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationFactory;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.Builtin;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSOrdinaryObjectImpl;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;

/**
 * @see DynamicObject
 */
public final class JSObjectUtil {
    private static final HiddenKey PROTOTYPE_DATA = new HiddenKey("PROTOTYPE_DATA");

    private JSObjectUtil() {
        // this utility class should not be instantiated
    }

    /**
     * Formats {@link JSDynamicObject#defaultToString()}, by default returns "[object ...]".
     *
     * @param object object to be used
     * @return "[object ...]" by default
     */
    @TruffleBoundary
    public static String formatToString(String object) {
        return "[object " + object + "]";
    }

    public static DynamicObject createOrdinaryPrototypeObject(JSRealm realm) {
        CompilerAsserts.neverPartOfCompilation();
        return createOrdinaryPrototypeObject(realm, realm.getObjectPrototype());
    }

    public static DynamicObject createOrdinaryPrototypeObject(JSRealm realm, DynamicObject prototype) {
        CompilerAsserts.neverPartOfCompilation();
        // slow; only use for initialization
        assert prototype == Null.instance || JSRuntime.isObject(prototype);

        JSContext context = realm.getContext();
        DynamicObject obj;
        if (context.isMultiContext()) {
            obj = JSUserObject.createInitWithInstancePrototype(prototype, context);
        } else {
            Shape initialShape = prototype == Null.instance ? context.getEmptyShapeNullPrototype() : JSObjectUtil.getProtoChildShape(prototype, JSUserObject.INSTANCE, context);
            obj = JSOrdinaryObjectImpl.create(initialShape);
        }
        return obj;
    }

    public static void setOrVerifyPrototype(JSContext context, DynamicObject obj, DynamicObject prototype) {
        CompilerAsserts.neverPartOfCompilation();
        assert prototype == Null.instance || JSRuntime.isObject(prototype);
        if (context.isMultiContext()) {
            JSObjectUtil.putHiddenProperty(obj, JSObject.HIDDEN_PROTO, prototype);
        } else {
            assert JSObjectUtil.getHiddenProperty(obj, JSObject.HIDDEN_PROTO) == prototype;
        }
    }

    public static boolean isValidPrototype(Object proto) {
        return proto == Null.instance || JSRuntime.isObject(proto);
    }

    private static LocationFactory declaredLocationFactory() {
        return (shape, val) -> shape.allocator().declaredLocation(val);
    }

    public static Shape shapeDefineDataProperty(JSContext context, Shape shape, Object key, Object value, int flags) {
        CompilerAsserts.neverPartOfCompilation();
        return shape.defineProperty(checkForNoSuchPropertyOrMethod(context, key), value, flags);
    }

    @SuppressWarnings("deprecation")
    public static Shape shapeDefineDeclaredDataProperty(JSContext context, Shape shape, Object key, Object value, int flags) {
        CompilerAsserts.neverPartOfCompilation();
        checkForNoSuchPropertyOrMethod(context, key);
        return shape.defineProperty(key, value, flags, declaredLocationFactory());
    }

    @TruffleBoundary
    public static void putDataProperty(JSContext context, DynamicObject thisObj, Object key, Object value, int flags) {
        assert checkForExistingProperty(thisObj, key);
        defineDataProperty(context, thisObj, key, value, flags);
    }

    @TruffleBoundary
    public static void putDataProperty(DynamicObject thisObj, Object name, Object value, int flags) {
        JSContext context = JSObject.getJSContext(thisObj);
        putDataProperty(context, thisObj, name, value, flags);
    }

    @TruffleBoundary
    public static void defineDataProperty(JSContext context, DynamicObject thisObj, Object key, Object value, int flags) {
        checkForNoSuchPropertyOrMethod(context, key);
        DynamicObjectLibrary.getUncached().putWithFlags(thisObj, key, value, flags);
    }

    @TruffleBoundary
    public static void defineDataProperty(DynamicObject thisObj, Object key, Object value, int flags) {
        JSContext context = JSObject.getJSContext(thisObj);
        defineDataProperty(context, thisObj, key, value, flags);
    }

    public static void putOrSetDataProperty(JSContext context, DynamicObject thisObj, Object key, Object value, int flags) {
        if (!JSObject.hasOwnProperty(thisObj, key)) {
            JSObjectUtil.putDataProperty(context, thisObj, key, value, flags);
        } else {
            JSObject.set(thisObj, key, value);
        }
    }

    @TruffleBoundary
    public static void defineAccessorProperty(DynamicObject thisObj, Object key, Accessor accessor, int flags) {
        int finalFlags = flags | JSProperty.ACCESSOR;

        JSContext context = JSObject.getJSContext(thisObj);
        checkForNoSuchPropertyOrMethod(context, key);
        DynamicObjectLibrary.getUncached().putWithFlags(thisObj, key, accessor, finalFlags);
    }

    @TruffleBoundary
    public static void defineProxyProperty(DynamicObject thisObj, Object key, PropertyProxy proxy, int flags) {
        int finalFlags = flags | JSProperty.PROXY;

        JSContext context = JSObject.getJSContext(thisObj);
        checkForNoSuchPropertyOrMethod(context, key);
        DynamicObjectLibrary.getUncached().putConstant(thisObj, key, proxy, finalFlags);
    }

    @TruffleBoundary
    public static void changePropertyFlags(DynamicObject thisObj, Object key, int flags) {
        // only javascript flags allowed here
        assert flags == (flags & JSAttributes.ATTRIBUTES_MASK);

        JSDynamicObject.updatePropertyFlags(thisObj, key, (attr) -> (attr & ~JSAttributes.ATTRIBUTES_MASK) | flags);
    }

    public static void putDataProperty(JSContext context, DynamicObject thisObj, String name, Object value) {
        putDataProperty(context, thisObj, name, value, JSAttributes.notConfigurableNotEnumerableNotWritable());
    }

    @TruffleBoundary
    public static void putDeclaredDataProperty(JSContext context, DynamicObject thisObj, Object key, Object value, int flags) {
        assert JSRuntime.isPropertyKey(key);
        assert checkForExistingProperty(thisObj, key);

        checkForNoSuchPropertyOrMethod(context, key);
        DynamicObjectLibrary.getUncached().putConstant(thisObj, key, value, flags);
    }

    public static void putConstructorProperty(JSContext context, DynamicObject prototype, DynamicObject constructor) {
        putDataProperty(context, prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableWritable());
    }

    public static void putConstructorPrototypeProperty(JSContext ctx, DynamicObject constructor, DynamicObject prototype) {
        putDataProperty(ctx, constructor, JSObject.PROTOTYPE, prototype, JSAttributes.notConfigurableNotEnumerableNotWritable());
    }

    public static void putToStringTag(DynamicObject prototype, String toStringTag) {
        assert checkForExistingProperty(prototype, Symbol.SYMBOL_TO_STRING_TAG);
        DynamicObjectLibrary.getUncached().putWithFlags(prototype, Symbol.SYMBOL_TO_STRING_TAG, toStringTag, JSAttributes.configurableNotEnumerableNotWritable());
    }

    @TruffleBoundary
    public static void putAccessorProperty(JSContext context, DynamicObject thisObj, Object key, DynamicObject getter, DynamicObject setter, int flags) {
        Accessor accessor = new Accessor(getter, setter);
        putAccessorProperty(context, thisObj, key, accessor, flags);
    }

    @TruffleBoundary
    public static void putAccessorProperty(JSContext context, DynamicObject thisObj, Object key, Accessor accessor, int flags) {
        assert JSRuntime.isPropertyKey(key);
        assert checkForExistingProperty(thisObj, key);

        checkForNoSuchPropertyOrMethod(context, key);
        DynamicObjectLibrary.getUncached().putWithFlags(thisObj, key, accessor, flags | JSProperty.ACCESSOR);
    }

    public static void putBuiltinAccessorProperty(DynamicObject thisObj, Object key, DynamicObject getter, DynamicObject setter) {
        putBuiltinAccessorProperty(thisObj, key, getter, setter, JSAttributes.configurableNotEnumerable());
    }

    @TruffleBoundary
    public static void putBuiltinAccessorProperty(DynamicObject thisObj, Object key, DynamicObject getter, DynamicObject setter, int flags) {
        Accessor accessor = new Accessor(getter, setter);
        putBuiltinAccessorProperty(thisObj, key, accessor, flags);
    }

    @TruffleBoundary
    public static void putBuiltinAccessorProperty(DynamicObject thisObj, Object key, Accessor accessor, int flags) {
        assert JSRuntime.isPropertyKey(key) && !isNoSuchPropertyOrMethod(key);
        assert checkForExistingProperty(thisObj, key);
        DynamicObjectLibrary.getUncached().putWithFlags(thisObj, key, accessor, flags | JSProperty.ACCESSOR);
    }

    public static void putProxyProperty(DynamicObject thisObj, Object key, PropertyProxy proxy, int flags) {
        assert JSRuntime.isPropertyKey(key) && !isNoSuchPropertyOrMethod(key);
        assert checkForExistingProperty(thisObj, key);
        defineProxyProperty(thisObj, key, proxy, flags);
    }

    private static boolean checkForExistingProperty(DynamicObject thisObj, Object key) {
        assert !thisObj.getShape().hasProperty(key) : "Don't put a property that already exists. Use the setters.";
        return true;
    }

    /**
     * Get or create a prototype child shape inheriting from this object, migrating the object to a
     * unique shape in the process. Creating unique shapes should be avoided in the fast path.
     */
    public static Shape getProtoChildShape(DynamicObject obj, JSClass jsclass, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        if (obj == null) {
            return context.makeEmptyShapeWithPrototypeInObject(jsclass);
        }
        assert JSRuntime.isObject(obj);
        Shape protoChild = getProtoChildShapeMaybe(obj, jsclass);
        if (protoChild != null) {
            return protoChild;
        }

        return getProtoChildShapeSlowPath(obj, jsclass, context);
    }

    public static Shape getProtoChildShape(DynamicObject obj, JSClass jsclass, JSContext context, BranchProfile branchProfile) {
        Shape protoChild = getProtoChildShapeMaybe(obj, jsclass);
        if (protoChild != null) {
            return protoChild;
        }

        branchProfile.enter();
        return getProtoChildShapeSlowPath(obj, jsclass, context);
    }

    private static Shape getProtoChildShapeMaybe(DynamicObject obj, JSClass jsclass) {
        Shape protoChild = JSShape.getProtoChildTree(obj, jsclass);
        assert protoChild == null || JSShape.getJSClassNoCast(protoChild) == jsclass;
        return protoChild;
    }

    @TruffleBoundary
    private static Shape getProtoChildShapeSlowPath(DynamicObject obj, JSClass jsclass, JSContext context) {
        JSPrototypeData prototypeData = getPrototypeData(obj);
        if (prototypeData == null) {
            prototypeData = putPrototypeData(obj);
        }
        return prototypeData.getOrAddProtoChildTree(jsclass, createChildRootShape(obj, jsclass, context));
    }

    private static Shape createChildRootShape(DynamicObject proto, JSClass jsclass, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        assert proto != null && proto != Null.instance;
        return JSShape.createObjectShape(context, jsclass, proto);
    }

    public static JSPrototypeData putPrototypeData(DynamicObject obj) {
        CompilerAsserts.neverPartOfCompilation();
        assert getPrototypeData(obj) == null;
        JSPrototypeData prototypeData = new JSPrototypeData();
        putPrototypeData(obj, prototypeData);
        return prototypeData;
    }

    private static void putPrototypeData(DynamicObject obj, JSPrototypeData prototypeData) {
        boolean extensible = JSShape.isExtensible(obj.getShape());
        JSObjectUtil.putHiddenProperty(obj, PROTOTYPE_DATA, prototypeData);
        assert extensible == JSShape.isExtensible(obj.getShape());
    }

    static JSPrototypeData getPrototypeData(DynamicObject obj) {
        return (JSPrototypeData) JSDynamicObject.getOrNull(obj, PROTOTYPE_DATA);
    }

    public static Map<Object, Object> archive(DynamicObject obj) {
        HashMap<Object, Object> ret = new HashMap<>();
        Shape shape = obj.getShape();
        for (Property prop : shape.getPropertyListInternal(false)) {
            if (!(prop.getLocation().isValue()) && !ret.containsKey(prop.getKey())) {
                ret.put(prop.getKey(), prop.get(obj, false));
            }
        }
        return ret;
    }

    @TruffleBoundary
    public static void setPrototypeImpl(DynamicObject object, DynamicObject newPrototype) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSShape.isPrototypeInShape(object.getShape());

        final JSContext context = JSObject.getJSContext(object);
        final Shape oldShape = object.getShape();
        JSShape.invalidatePrototypeAssumption(oldShape);
        final Shape newRootShape;
        if (newPrototype == Null.instance) {
            newRootShape = context.makeEmptyShapeWithNullPrototype(JSShape.getJSClass(oldShape));
        } else {
            assert JSRuntime.isObject(newPrototype) : newPrototype;
            newRootShape = JSObjectUtil.getProtoChildShape(newPrototype, JSShape.getJSClass(oldShape), context);
        }

        DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();

        List<Property> allProperties = oldShape.getPropertyListInternal(true);
        List<Object> archive = new ArrayList<>(allProperties.size());
        for (Property prop : allProperties) {
            Object value = lib.getOrDefault(object, prop.getKey(), null);
            archive.add(value);
        }

        lib.resetShape(object, newRootShape);

        for (int i = 0; i < allProperties.size(); i++) {
            Property property = allProperties.get(i);
            Object key = property.getKey();
            if (!newRootShape.hasProperty(key)) {
                Object value = archive.get(i);
                int propertyFlags = property.getFlags();
                if (property.getLocation().isConstant()) {
                    lib.putConstant(object, key, value, propertyFlags);
                } else {
                    lib.putWithFlags(object, key, value, propertyFlags);
                }

            }
        }
    }

    public static <T> T checkForNoSuchPropertyOrMethod(JSContext context, T key) {
        CompilerAsserts.neverPartOfCompilation();
        if (context != null && key != null && context.isOptionNashornCompatibilityMode()) {
            if (context.getNoSuchPropertyUnusedAssumption().isValid() && JSObject.NO_SUCH_PROPERTY_NAME.equals(key)) {
                context.getNoSuchPropertyUnusedAssumption().invalidate("NoSuchProperty is used");
            }
            if (context.getNoSuchMethodUnusedAssumption().isValid() && JSObject.NO_SUCH_METHOD_NAME.equals(key)) {
                context.getNoSuchMethodUnusedAssumption().invalidate("NoSuchMethod is used");
            }
        }
        return key;
    }

    public static boolean isNoSuchPropertyOrMethod(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        return (key instanceof String && (key.equals(JSObject.NO_SUCH_PROPERTY_NAME) || key.equals(JSObject.NO_SUCH_METHOD_NAME)));
    }

    public static DynamicObject createSymbolSpeciesGetterFunction(JSRealm realm) {
        return JSFunction.create(realm, JSFunctionData.createCallOnly(realm.getContext(), realm.getContext().getSpeciesGetterFunctionCallTarget(), 0, "get [Symbol.species]"));
    }

    public static void putFunctionsFromContainer(JSRealm realm, DynamicObject thisObj, JSBuiltinsContainer container) {
        JSContext context = realm.getContext();
        container.forEachBuiltin(new Consumer<Builtin>() {
            @Override
            public void accept(Builtin builtin) {
                if (builtin.getECMAScriptVersion() > context.getEcmaScriptVersion()) {
                    return;
                } else if (builtin.isAnnexB() && !context.isOptionAnnexB()) {
                    return;
                }
                JSFunctionData functionData = builtin.createFunctionData(context);
                putDataProperty(context, thisObj, builtin.getKey(), JSFunction.create(realm, functionData), builtin.getAttributeFlags());
            }
        });
    }

    public static void putHiddenProperty(DynamicObject obj, Object key, Object value) {
        assert key instanceof HiddenKey;
        DynamicObjectLibrary.getUncached().put(obj, key, value);
    }

    public static Object getHiddenProperty(DynamicObject obj, Object key) {
        assert key instanceof HiddenKey;
        return DynamicObjectLibrary.getUncached().getOrDefault(obj, key, null);
    }

    public static boolean hasHiddenProperty(DynamicObject obj, Object key) {
        assert key instanceof HiddenKey;
        return DynamicObjectLibrary.getUncached().containsKey(obj, key);
    }

    public static DynamicObjectLibrary createCached(Object key, DynamicObject obj) {
        assert key != null;
        return DynamicObjectLibrary.getFactory().create(obj);
    }

    public static DynamicObjectLibrary createDispatched(Object key, int limit) {
        assert key != null;
        return DynamicObjectLibrary.getFactory().createDispatched(limit);
    }

    public static DynamicObjectLibrary createDispatched(Object key) {
        return createDispatched(key, JSConfig.PropertyCacheLimit);
    }

    public static <T extends DynamicObject> T copyProperties(T target, DynamicObject source) {
        DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
        for (Property property : source.getShape().getPropertyListInternal(true)) {
            Object key = property.getKey();
            if (objectLibrary.containsKey(target, key)) {
                continue;
            }
            Object value = objectLibrary.getOrDefault(source, key, null);
            if (property.getLocation().isConstant()) {
                objectLibrary.putConstant(target, key, value, property.getFlags());
            } else {
                objectLibrary.putWithFlags(target, key, value, property.getFlags());
            }
        }
        return target;
    }
}
