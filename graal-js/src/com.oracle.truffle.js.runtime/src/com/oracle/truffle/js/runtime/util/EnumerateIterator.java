/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class EnumerateIterator implements Iterator<Object> {
    private DynamicObject current;
    private Shape currentShape;
    private Iterator<?> iterator;
    private Object visitedInPrototypeChain;
    private Object next;
    private static final Object FIND_NEXT = new Object();

    public EnumerateIterator(DynamicObject obj) {
        this.current = obj;
        this.currentShape = obj.getShape();
        this.iterator = makePropertyIterator(obj);
        this.next = findNext();
    }

    @Override
    public boolean hasNext() {
        if (next == FIND_NEXT) {
            next = findNext();
        }
        return next != null;
    }

    @Override
    public Object next() {
        if (hasNext()) {
            try {
                return next;
            } finally {
                next = FIND_NEXT;
            }
        }
        throw new NoSuchElementException();
    }

    @TruffleBoundary
    private Object findNext() {
        for (;;) {
            while (iterator.hasNext()) {
                final Object p = iterator.next();
                final Object key;
                if (p instanceof Property) {
                    Property prop = (Property) p;
                    Object propKey = prop.getKey();
                    if (JSProperty.isEnumerable(prop) && propKey instanceof String) {
                        if (currentShape == current.getShape()) {
                            key = propKey;
                        } else {
                            // shape has changed => re-check the property
                            PropertyDescriptor desc = JSObject.getOwnProperty(current, propKey);
                            if (desc != null && desc.getEnumerable()) {
                                key = propKey;
                            } else {
                                continue;
                            }
                        }
                    } else {
                        continue;
                    }
                } else if (p instanceof String) {
                    PropertyDescriptor desc = JSObject.getOwnProperty(current, p);
                    // desc can be null if obj is a Proxy
                    if (desc != null && desc.getEnumerable()) {
                        key = p;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                if (!alreadyVisitedKey(key)) {
                    return key;
                }
            }
            DynamicObject proto = JSObject.getPrototype(current);
            if (proto == Null.instance) {
                return null;
            }
            addVisitedObjectInPrototypeChain(current);
            current = proto;
            currentShape = proto.getShape();
            iterator = makePropertyIterator(proto);
        }
    }

    @SuppressWarnings("unchecked")
    private void addVisitedObjectInPrototypeChain(DynamicObject object) {
        if (visitedInPrototypeChain == null) {
            visitedInPrototypeChain = object;
        } else if (visitedInPrototypeChain instanceof ArrayList) {
            ArrayList<DynamicObject> list = (ArrayList<DynamicObject>) visitedInPrototypeChain;
            avoidRecursion(list, object);
            list.add(object);
        } else {
            ArrayList<DynamicObject> list = new ArrayList<>(4);
            list.add((DynamicObject) visitedInPrototypeChain);
            avoidRecursion(list, object);
            list.add(object);
            visitedInPrototypeChain = list;
        }
    }

    private static void avoidRecursion(ArrayList<DynamicObject> list, DynamicObject object) {
        if (list.contains(object)) {
            throw Errors.createRangeError("cannot recurse in Enumeration");
        }
    }

    private boolean alreadyVisitedKey(Object key) {
        if (visitedInPrototypeChain == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Iterable<DynamicObject> visitedObjects = visitedInPrototypeChain instanceof ArrayList ? (ArrayList<DynamicObject>) visitedInPrototypeChain
                        : Collections.singleton((DynamicObject) visitedInPrototypeChain);
        for (DynamicObject visitedObject : visitedObjects) {
            // also add non-enumerable properties, they hide enumerable ones in the prototypes.
            for (Object visitedKey : JSObject.ownPropertyKeys(visitedObject)) {
                if (visitedKey.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @TruffleBoundary
    private static Iterator<?> makePropertyIterator(DynamicObject current) {
        return ownKeysOrProperties(current).iterator();
    }

    private static Iterable<?> ownKeysOrProperties(DynamicObject current) {
        JSClass jsclass = JSObject.getJSClass(current);
        if (JSTruffleOptions.FastOwnKeys && jsclass.hasOnlyShapeProperties(current)) {
            return JSShape.getProperties(current.getShape());
        }
        return jsclass.ownPropertyKeys(current);
    }
}
