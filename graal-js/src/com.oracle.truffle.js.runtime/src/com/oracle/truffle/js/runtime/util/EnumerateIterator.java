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
