/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDictionary;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;
import com.oracle.truffle.js.runtime.util.UnmodifiablePropertyKeyList;

/**
 * Static helper methods for JS-specific operations on shapes.
 *
 * @see JSShapeData
 */
public final class JSShape {

    public static final int NOT_EXTENSIBLE_FLAG = 1 << 0;
    public static final int SEALED_FLAG = 1 << 1;
    public static final int FROZEN_FLAG = 1 << 2;
    public static final int SEALED_FLAGS = NOT_EXTENSIBLE_FLAG | SEALED_FLAG;
    public static final int FROZEN_FLAGS = SEALED_FLAGS | FROZEN_FLAG;

    /**
     * If this flag is set, the object has extra properties that are not included in the
     * DynamicObject's shape.
     */
    public static final int EXTERNAL_PROPERTIES_FLAG = 1 << 3;

    /**
     * Marks %Array.prototype% and prototype objects that have (or had) %Array.prototype% on their
     * prototype chain. Setting an element on such an object invalidates the no-elements assumption.
     */
    public static final int ARRAY_PROTOTYPE_FLAG = 1 << 4;
    /**
     * Marks %Object.prototype%. Setting an element on it invalidates the no-elements assumption.
     */
    public static final int OBJECT_PROTOTYPE_FLAG = 1 << 5;
    /**
     * Marks objects, setting an element on which invalidates the no-elements assumption.
     */
    public static final int NO_ELEMENTS_ASSUMPTION_FLAGS = ARRAY_PROTOTYPE_FLAG | OBJECT_PROTOTYPE_FLAG;

    /**
     * Marks objects, [[GetOwnProperty]] of which is not implemented using OrdinaryGetOwnProperty.
     */
    public static final int UNORDINARY_GETOWNPROPERTY_FLAG = 1 << 6;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private JSShape() {
    }

    public static Shape createPrototypeShape(JSContext context, JSClass jsclass, JSDynamicObject prototype) {
        assert prototype == Null.instance || JSRuntime.isObject(prototype);
        if (context.isMultiContext()) {
            return JSObjectUtil.getProtoChildShape(null, jsclass, context);
        } else {
            return prototype == Null.instance ? context.getEmptyShapeNullPrototype() : JSObjectUtil.getProtoChildShape(prototype, jsclass, context);
        }
    }

    static Shape createObjectShape(JSContext context, JSClass jsclass, JSDynamicObject prototype) {
        Shape rootShape = newBuilder(context, jsclass, prototype).build();
        return Shape.newBuilder(rootShape).addConstantProperty(JSObject.HIDDEN_PROTO, prototype, 0).build();
    }

    @Idempotent
    @NeverDefault
    public static JSClass getJSClass(Shape shape) {
        return (JSClass) shape.getDynamicType();
    }

    @Idempotent
    public static Object getJSClassNoCast(Shape shape) {
        return shape.getDynamicType();
    }

    public static JSSharedData getSharedData(Shape shape) {
        return (JSSharedData) shape.getSharedData();
    }

    /**
     * Get empty shape for all objects inheriting from the prototype this shape is describing.
     */
    public static Shape getProtoChildTree(JSDynamicObject prototype, JSClass jsclass) {
        JSPrototypeData prototypeData = JSObjectUtil.getPrototypeData(prototype);
        if (prototypeData != null) {
            return prototypeData.getProtoChildTree(jsclass);
        }
        return null;
    }

    public static boolean isExtensible(Shape shape) {
        return (shape.getFlags() & NOT_EXTENSIBLE_FLAG) == 0;
    }

    /**
     * Returns true if this object is the Array.prototype or the prototype of an Array subclass.
     */
    public static boolean isArrayPrototypeOrDerivative(Shape shape) {
        return (shape.getFlags() & ARRAY_PROTOTYPE_FLAG) != 0;
    }

    public static boolean isArrayPrototypeOrDerivative(JSDynamicObject obj) {
        return isArrayPrototypeOrDerivative(obj.getShape());
    }

    /**
     * Returns true if this object is not supposed to have elements and adding an element to it
     * should invalidate the no-array-prototype-elements assumption, including Object.prototype,
     * Array.prototype, and prototypes of Array subclasses, i.e. all prototype objects that are
     * expected on the prototype chain of an Array exotic object, while the assumption is valid.
     */
    public static boolean hasNoElementsAssumption(Shape shape) {
        return (shape.getFlags() & NO_ELEMENTS_ASSUMPTION_FLAGS) != 0;
    }

    public static boolean hasNoElementsAssumption(JSDynamicObject obj) {
        return hasNoElementsAssumption(obj.getShape());
    }

    public static boolean isPrototypeInShape(Shape shape) {
        return getPrototypeProperty(shape).getLocation().isConstant();
    }

    public static Property getPrototypeProperty(Shape shape) {
        return shape.getProperty(JSObject.HIDDEN_PROTO);
    }

    public static Assumption getPropertyAssumption(Shape shape, Object key) {
        return getPropertyAssumption(shape, key, false);
    }

    public static Assumption getPropertyAssumption(Shape shape, Object key, boolean prototype) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey : key;
        if (shape == Null.SHAPE) {
            return Assumption.ALWAYS_VALID;
        }
        if (prototype && JSConfig.LeafShapeAssumption) {
            return shape.getLeafAssumption();
        }
        return shape.getPropertyAssumption(key);
    }

    public static JSContext getJSContext(Shape shape) {
        return getSharedData(shape).getContext();
    }

    public static boolean isObjectPrototype(Shape shape) {
        return getJSClassNoCast(shape) == JSObjectPrototype.INSTANCE;
    }

    public static Assumption getPrototypeAssumption(Shape shape) {
        if (isObjectPrototype(shape)) {
            return Assumption.ALWAYS_VALID;
        }
        return getSharedData(shape).getPrototypeAssumption();
    }

    public static void invalidatePrototypeAssumption(Shape shape) {
        getSharedData(shape).invalidatePrototypeAssumption();
    }

    public static <T> UnmodifiablePropertyKeyList<T> getPropertyKeyList(Shape shape, boolean strings, boolean symbols) {
        assert JSConfig.FastOwnKeys;
        return JSShapeData.getPropertyKeyList(shape, strings, symbols);
    }

    public static UnmodifiableArrayList<TruffleString> getEnumerablePropertyNames(Shape shape) {
        assert JSConfig.FastOwnKeys;
        return JSShapeData.getEnumerablePropertyNames(shape);
    }

    public static UnmodifiableArrayList<Property> getPropertiesIfHasEnumerablePropertyNames(Shape shape) {
        assert JSConfig.FastOwnKeys;
        return JSShapeData.getPropertiesIfHasEnumerablePropertyNames(shape);
    }

    /**
     * Internal constructor for null and undefined shapes.
     */
    public static Shape makeStaticRoot(JSClass jsclass) {
        return Shape.newBuilder().layout(getLayout(jsclass), LOOKUP).dynamicType(jsclass).build();
    }

    public static Shape makeEmptyRoot(JSClass jsclass, JSContext context) {
        return createObjectShape(context, jsclass, Null.instance);
    }

    public static Shape createRootWithNullProto(JSContext context, JSClass jsclass) {
        return createObjectShape(context, jsclass, Null.instance);
    }

    /**
     * Empty shape constructor with prototype in field.
     */
    public static Shape makeEmptyRootWithInstanceProto(JSContext context, JSClass jsclass) {
        return newBuilder(context, jsclass, null).build();
    }

    public static JSSharedData makeJSSharedData(JSContext context, JSDynamicObject proto) {
        return new JSSharedData(context, proto);
    }

    public static Class<? extends JSDynamicObject> getLayout(JSClass jsclass) {
        if (jsclass == JSOrdinary.INSTANCE || jsclass == JSDictionary.INSTANCE) {
            return JSOrdinaryObject.DefaultLayout.class;
        } else if (jsclass == JSOrdinary.INTERNAL_FIELD_INSTANCE) {
            return JSOrdinaryObject.InternalFieldLayout.class;
        } else if (jsclass == JSOrdinary.OVERLOADED_OPERATORS_INSTANCE) {
            return JSOverloadedOperatorsObject.class;
        } else if (jsclass == JSArray.INSTANCE) {
            return JSArrayObject.class;
        }
        return JSDynamicObject.class;
    }

    public static Shape.Builder newBuilder(JSContext context, JSClass jsclass, JSDynamicObject proto) {
        return newBuilder(context, jsclass, proto, 0);
    }

    public static Shape.Builder newBuilder(JSContext context, JSClass jsclass, JSDynamicObject proto, int shapeFlags) {
        assert !context.isMultiContext() || (proto == null || proto == Null.instance);
        return Shape.newBuilder().//
                        layout(getLayout(jsclass), LOOKUP).//
                        dynamicType(jsclass).//
                        sharedData(JSShape.makeJSSharedData(context, proto)).//
                        shapeFlags(shapeFlags | getDefaultShapeFlags(jsclass)).//
                        allowImplicitCastIntToDouble(true).//
                        propertyAssumptions(JSConfig.PropertyAssumption && !context.isMultiContext());
    }

    public static int getDefaultShapeFlags(JSClass jsclass) {
        int flags = 0;
        if (jsclass == JSDictionary.INSTANCE) {
            flags |= EXTERNAL_PROPERTIES_FLAG;
        } else if (jsclass == JSObjectPrototype.INSTANCE) {
            flags |= OBJECT_PROTOTYPE_FLAG;
        }
        if (!jsclass.usesOrdinaryGetOwnProperty()) {
            flags |= UNORDINARY_GETOWNPROPERTY_FLAG;
        }
        return flags;
    }

    /**
     * If this flag is set, the object has extra properties that are not included in the
     * DynamicObject's shape.
     */
    public static boolean hasExternalProperties(int shapeFlags) {
        return (shapeFlags & EXTERNAL_PROPERTIES_FLAG) != 0;
    }

    /**
     * Returns true if the object's [[GetOwnProperty]] is implemented using OrdinaryGetOwnProperty.
     */
    @Idempotent
    public static boolean usesOrdinaryGetOwnProperty(Shape shape) {
        return (shape.getFlags() & UNORDINARY_GETOWNPROPERTY_FLAG) == 0;
    }
}
