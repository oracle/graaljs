/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.util.DebugCounter;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

/**
 * Extra metadata associated with JavaScript object shapes.
 *
 * @see JSShape
 */
public final class JSShapeData {
    private static final Property[] EMPTY_PROPERTY_ARRAY = new Property[0];

    private Property[] propertyArray;
    private Property[] enumerablePropertyArray;

    private JSShapeData() {
    }

    private static Property[] createPropertiesArray(Shape shape) {
        CompilerAsserts.neverPartOfCompilation();
        propertyListAllocCount.inc();
        List<Property> ownProperties = shape.getPropertyList();
        sortProperties(ownProperties);
        return ownProperties.toArray(EMPTY_PROPERTY_ARRAY);
    }

    private static Property[] createEnumerablePropertiesArray(Shape shape) {
        CompilerAsserts.neverPartOfCompilation();
        enumerablePropertyListAllocCount.inc();
        List<Property> ownProperties = new ArrayList<>();
        shape.getPropertyList().forEach(property -> {
            if (JSProperty.isEnumerable(property) && property.getKey() instanceof String) {
                ownProperties.add(property);
            }
        });
        sortProperties(ownProperties);
        return ownProperties.toArray(EMPTY_PROPERTY_ARRAY);
    }

    private static void sortProperties(List<Property> ownProperties) {
        CompilerAsserts.neverPartOfCompilation();
        Collections.sort(ownProperties, (o1, o2) -> JSRuntime.comparePropertyKeys(o1.getKey(), o2.getKey()));
    }

    private static JSShapeData getShapeData(Shape shape) {
        CompilerAsserts.neverPartOfCompilation();
        JSContext context = JSShape.getJSContext(shape);
        Map<Shape, JSShapeData> map = context.getShapeDataMap();
        return map.computeIfAbsent(shape, s -> new JSShapeData());
    }

    @TruffleBoundary
    private static Property[] getPropertiesArray(Shape shape) {
        if (shape.getPropertyCount() == 0) {
            return EMPTY_PROPERTY_ARRAY;
        } else {
            JSShapeData shapeData = getShapeData(shape);
            if (shapeData.propertyArray == null) {
                assert shape.getPropertyCount() != 0;
                shapeData.propertyArray = createPropertiesArray(shape);
                assert shapeData.propertyArray.length == shape.getPropertyCount();
            }
            return shapeData.propertyArray;
        }
    }

    static List<Property> getProperties(Shape shape) {
        return asUnmodifiableList(getPropertiesArray(shape));
    }

    @TruffleBoundary
    private static Property[] getEnumerablePropertyArray(Shape shape) {
        if (shape.getPropertyCount() == 0) {
            return EMPTY_PROPERTY_ARRAY;
        } else {
            JSShapeData shapeData = getShapeData(shape);
            if (shapeData.enumerablePropertyArray == null) {
                assert shape.getPropertyCount() != 0;
                shapeData.enumerablePropertyArray = createEnumerablePropertiesArray(shape);
            }
            return shapeData.enumerablePropertyArray;
        }
    }

    static List<Property> getEnumerableProperties(Shape shape) {
        return asUnmodifiableList(getEnumerablePropertyArray(shape));
    }

    static List<String> getEnumerablePropertyNames(Shape shape) {
        return asConvertedList(getEnumerablePropertyArray(shape), p -> (String) p.getKey());
    }

    private static <T> List<T> asUnmodifiableList(T[] array) {
        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                return array[index];
            }

            @Override
            public int size() {
                return array.length;
            }

            @Override
            public Iterator<T> iterator() {
                return IteratorUtil.simpleArrayIterator(array);
            }
        };
    }

    private static <T, R> List<R> asConvertedList(T[] array, Function<T, R> converter) {
        return new AbstractList<R>() {
            @Override
            public R get(int index) {
                return converter.apply(array[index]);
            }

            @Override
            public int size() {
                return array.length;
            }

            @Override
            public Iterator<R> iterator() {
                return IteratorUtil.simpleListIterator(this);
            }
        };
    }

    static int getEnumerablePropertyCount(Shape shape) {
        return getEnumerablePropertyArray(shape).length;
    }

    private static final DebugCounter enumerablePropertyListAllocCount = DebugCounter.create("Enumerable property lists allocated");
    private static final DebugCounter propertyListAllocCount = DebugCounter.create("Property lists allocated");
}
