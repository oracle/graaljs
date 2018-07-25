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
package com.oracle.truffle.js.runtime.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.object.ShapeListener;
import com.oracle.truffle.api.utilities.NeverValidAssumption;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.util.DebugCounter;

/**
 * @see JSShape
 */
public final class JSSharedData implements ShapeListener {
    private final boolean unique;
    private final JSContext context;
    private final CopyOnWriteArrayList<Shape> protoChildTrees;
    private final Property prototypeProperty;
    private Map<Object, Assumption> propertyAssumptions;

    private static final DebugCounter propertyAssumptionsCreated = DebugCounter.create("Property assumptions created");
    private static final DebugCounter propertyAssumptionsRemoved = DebugCounter.create("Property assumptions removed");

    public JSSharedData(boolean unique, JSContext context, Property prototypeProperty) {
        this.unique = unique;
        this.context = context;
        this.prototypeProperty = prototypeProperty;
        this.protoChildTrees = unique ? new CopyOnWriteArrayList<>() : null;
    }

    Shape getProtoChildTree(ObjectType jsclass) {
        for (Shape childTree : protoChildTrees) {
            if (JSShape.getJSClassNoCast(childTree) == jsclass) {
                return childTree;
            }
        }
        return null;
    }

    synchronized Shape getOrAddProtoChildTree(ObjectType jsclass, Shape newRootShape) {
        Shape existingRootShape = getProtoChildTree(jsclass);
        if (existingRootShape == null) {
            protoChildTrees.add(newRootShape);
            return newRootShape;
        }
        return existingRootShape;
    }

    public Assumption getPropertyAssumption(Object propertyName) {
        if (propertyAssumptions == null) {
            propertyAssumptions = new HashMap<>();
        } else {
            Assumption assumption = propertyAssumptions.get(propertyName);
            if (assumption != null) {
                return assumption;
            }
        }
        Assumption assumption = Truffle.getRuntime().createAssumption(propertyName.toString());
        propertyAssumptions.put(propertyName, assumption);
        propertyAssumptionsCreated.inc();
        return assumption;
    }

    public void invalidatePropertyAssumption(Object propertyName) {
        if (propertyAssumptions != null) {
            Assumption assumption = propertyAssumptions.get(propertyName);
            if (assumption != null) {
                invalidatePropertyAssumptionImpl(propertyName, assumption);
            }
        }
    }

    private void invalidatePropertyAssumptionImpl(Object propertyName, Assumption assumption) {
        if (assumption != NeverValidAssumption.INSTANCE) {
            assumption.invalidate("invalidatePropertyAssumption");
            propertyAssumptions.put(propertyName, NeverValidAssumption.INSTANCE);
            propertyAssumptionsRemoved.inc();
        }
    }

    void invalidateAllPropertyAssumptions() {
        if (propertyAssumptions != null) {
            for (Map.Entry<Object, Assumption> entry : propertyAssumptions.entrySet()) {
                invalidatePropertyAssumptionImpl(entry.getKey(), entry.getValue());
            }
        }
    }

    public JSContext getContext() {
        return context;
    }

    public boolean isUnique() {
        return unique;
    }

    Property getPrototypeProperty() {
        return prototypeProperty;
    }

    @Override
    public void onPropertyTransition(Object key) {
        invalidatePropertyAssumption(key);
    }
}
