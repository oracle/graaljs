/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import java.util.HashMap;
import java.util.List;
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
    private final List<Shape> protoChildTrees;
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
            assumption.invalidate();
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
