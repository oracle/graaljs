/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;

/**
 *
 * @author Jan Stola
 */
public final class FunctionTemplate {
    /** Key that holds the FunctionTemplate used to create an object. */
    public static final HiddenKey CONSTRUCTOR = new HiddenKey("FunctionTemplateConstructor");

    private final ObjectTemplate functionObjectTemplate;
    private final ObjectTemplate instanceTemplate;
    private final ObjectTemplate prototypeTemplate;
    private final int id;
    private long functionPointer;
    private Object additionalData;
    private final FunctionTemplate signature;
    private FunctionTemplate parent;
    private String className = "";
    private DynamicObject functionObj;

    public FunctionTemplate(int id, long functionPointer, Object additionalData, FunctionTemplate signature) {
        functionObjectTemplate = new ObjectTemplate();
        instanceTemplate = new ObjectTemplate();
        prototypeTemplate = new ObjectTemplate();
        this.id = id;
        this.functionPointer = functionPointer;
        this.additionalData = additionalData;
        this.signature = signature;
    }

    public ObjectTemplate getFunctionObjectTemplate() {
        return functionObjectTemplate;
    }

    public ObjectTemplate getInstanceTemplate() {
        return instanceTemplate;
    }

    public ObjectTemplate getPrototypeTemplate() {
        return prototypeTemplate;
    }

    public void setFunctionObject(DynamicObject functionObj) {
        this.functionObj = functionObj;
    }

    public DynamicObject getFunctionObject() {
        return functionObj;
    }

    public int getID() {
        return id;
    }

    public void setFunctionPointer(long functionPointer) {
        this.functionPointer = functionPointer;
    }

    public long getFunctionPointer() {
        return functionPointer;
    }

    public void setAdditionalData(Object additionalData) {
        this.additionalData = additionalData;
    }

    public Object getAdditionalData() {
        return additionalData;
    }

    public FunctionTemplate getSignature() {
        return signature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public FunctionTemplate getParent() {
        return parent;
    }

    public void setParent(FunctionTemplate parent) {
        this.parent = parent;
    }

}
