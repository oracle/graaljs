/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.object.Shape.Allocator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSClass;

/**
 * Static helper methods for JS-specific operations on shapes.
 *
 * @see JSShapeData
 */
public final class JSShape {
    public static final HiddenKey NOT_EXTENSIBLE_KEY = new HiddenKey("\uf001!extensible");
    private static final Property NOT_EXTENSIBLE_PROPERTY = JSObjectUtil.makeHiddenProperty(NOT_EXTENSIBLE_KEY, JSObjectUtil.createConstantLocation(null));

    private JSShape() {
    }

    public static JSClass getJSClass(Shape shape) {
        return (JSClass) shape.getObjectType();
    }

    public static ObjectType getJSClassNoCast(Shape shape) {
        return shape.getObjectType();
    }

    /**
     * Make a unique shape for a prototype object.
     */
    public static Shape makeUniqueShape(Shape shape) {
        if (isUnique(shape)) {
            // this is already a prototype!
            return null;
        }
        CompilerDirectives.transferToInterpreter();
        return shape.createSeparateShape(new JSSharedData(true, getJSContext(shape), getPrototypeProperty(shape)));
    }

    public static JSSharedData getSharedData(Shape shape) {
        return (JSSharedData) shape.getSharedData();
    }

    private static boolean isUnique(Shape shape) {
        JSSharedData shared = getSharedData(shape);
        return shared.isUnique();
    }

    /**
     * Get empty shape for all objects inheriting from the prototype this shape is describing.
     */
    public static Shape getProtoChildTree(Shape prototypeShape, ObjectType jsclass) {
        JSSharedData shared = getSharedData(prototypeShape);
        if (shared.isUnique()) {
            return shared.getProtoChildTree(jsclass);
        }
        return null;
    }

    /**
     * Put a fake "not extensible" property at the end. Will always be the last property.
     */
    public static Shape makeNotExtensible(Shape shape) {
        if (isExtensible(shape)) {
            return shape.addProperty(NOT_EXTENSIBLE_PROPERTY);
        }
        return shape;
    }

    public static boolean isExtensible(Shape shape) {
        return shape.getLastProperty() != NOT_EXTENSIBLE_PROPERTY;
    }

    public static boolean isPrototypeInShape(Shape shape) {
        return getPrototypeProperty(shape).getLocation().isConstant();
    }

    public static Property getPrototypeProperty(Shape shape) {
        return getSharedData(shape).getPrototypeProperty();
    }

    static Property makePrototypeProperty(DynamicObject prototype) {
        return JSObjectUtil.makeHiddenProperty(JSObject.HIDDEN_PROTO, JSObjectUtil.createConstantLocation(prototype));
    }

    public static Assumption getPropertyAssumption(Shape shape, Object key) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
        return getSharedData(shape).getPropertyAssumption(key);
    }

    public static void invalidatePropertyAssumption(Shape shape, Object propertyName) {
        getSharedData(shape).invalidatePropertyAssumption(propertyName);
    }

    public static void invalidateAllPropertyAssumptions(Shape shape) {
        getSharedData(shape).invalidateAllPropertyAssumptions();
    }

    public static JSContext getJSContext(Shape shape) {
        return getSharedData(shape).getContext();
    }

    private static boolean isRoot(Shape shape) {
        return shape.getParent() == null;
    }

    private static Shape freezeHelper(Shape shape, Predicate<Shape> pred, Function<Property, Property> propertyConverter) {
        if (!pred.test(shape)) {
            Shape newShape = shape.getRoot();
            for (Property property : shape.getPropertyListInternal(true)) {
                newShape = newShape.addProperty(propertyConverter.apply(property));
            }
            assert pred.test(newShape);
            return newShape;
        }
        return shape;
    }

    public static Shape freeze(Shape shape) {
        return freezeHelper(shape, JSShape::isFrozen, JSProperty::freeze);
    }

    public static Shape seal(Shape shape) {
        return freezeHelper(shape, JSShape::isSealed, JSProperty::seal);
    }

    private static boolean isFrozenHelper(Shape shape, Predicate<Property> pred) {
        Shape current = shape;
        while (!isRoot(current)) {
            Property currentProperty = current.getLastProperty();
            if (pred.test(currentProperty)) {
                return false;
            }
            current = current.getParent();
        }
        return true;
    }

    private static boolean isFrozen(Shape shape) {
        return isFrozenHelper(shape, currentProperty -> (JSProperty.isConfigurable(currentProperty) || (JSProperty.isData(currentProperty) && JSProperty.isWritable(currentProperty))) &&
                        !currentProperty.isHidden());
    }

    private static boolean isSealed(Shape shape) {
        return isFrozenHelper(shape, currentProperty -> JSProperty.isConfigurable(currentProperty) && !currentProperty.isHidden());
    }

    public static List<Property> getEnumerableProperties(Shape shape) {
        assert JSTruffleOptions.FastOwnKeys;
        return JSShapeData.getEnumerableProperties(shape);
    }

    public static List<Property> getProperties(Shape shape) {
        assert JSTruffleOptions.FastOwnKeys;
        return JSShapeData.getProperties(shape);
    }

    public static List<String> getEnumerablePropertyNames(Shape shape) {
        assert JSTruffleOptions.FastOwnKeys;
        return JSShapeData.getEnumerablePropertyNames(shape);
    }

    /**
     * Internal constructor for null shape et al.
     */
    public static Shape makeStaticRoot(Layout layout, ObjectType jsclass, int id) {
        return makeRootShape(layout, jsclass, new JSSharedData(false, null, makePrototypeProperty(Null.instance)), id);
    }

    /**
     * Empty shape constructor.
     */
    public static Shape makeEmptyRoot(Layout layout, ObjectType jsclass, JSContext context) {
        return makeRootShape(layout, new JSSharedData(false, context, makePrototypeProperty(Null.instance)), jsclass);
    }

    /**
     * Empty shape constructor with prototype in field.
     */
    public static Shape makeEmptyRoot(Layout layout, ObjectType jsclass, JSContext context, Property prototypeProperty) {
        return makeRootShape(layout, new JSSharedData(false, context, prototypeProperty), jsclass);
    }

    /**
     * Constructor for makePrototypeShape.
     */
    public static Shape makeUniqueRoot(Layout layout, ObjectType jsclass, JSContext context, Property prototypeProperty) {
        return makeRootShape(layout, new JSSharedData(true, context, prototypeProperty), jsclass);
    }

    public static Shape makeUniqueRootWithPrototype(Layout layout, ObjectType jsclass, JSContext context, DynamicObject prototype) {
        return makeRootShape(layout, new JSSharedData(true, context, makePrototypeProperty(prototype)), jsclass);
    }

    /**
     * A RootShape starts a new shape tree and contains shared common data.
     */
    static Shape makeRootShape(Layout layout, JSSharedData sharedData, ObjectType builtinClass) {
        return makeRootShape(layout, builtinClass, sharedData, 1);
    }

    public static Allocator makeAllocator(Layout layout) {
        return layout.createAllocator();
    }

    private static Shape makeRootShape(Layout layout, ObjectType jsclass, JSSharedData sharedData, int id) {
        return layout.createShape(jsclass, sharedData, id).addProperty(sharedData.getPrototypeProperty());
    }
}
