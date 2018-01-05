/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Jan Stola
 */
public final class ObjectTemplate {

    private List<Accessor> accessors;
    private List<Value> values;
    private PropertyHandler indexedPropertyHandler;
    private PropertyHandler namedPropertyHandler;
    private boolean stringKeysOnly;
    private FunctionTemplate functionHandler;
    private FunctionTemplate parentFunctionTemplate;

    public List<Accessor> getAccessors() {
        return (accessors == null) ? Collections.emptyList() : accessors;
    }

    public void addAccessor(Accessor accessor) {
        if (accessors == null) {
            accessors = new ArrayList<>();
        }
        accessors.add(accessor);
    }

    public List<Value> getValues() {
        return (values == null) ? Collections.emptyList() : values;
    }

    public void addValue(Value value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
    }

    public void setIndexedPropertyHandler(PropertyHandler indexedPropertyHandler) {
        this.indexedPropertyHandler = indexedPropertyHandler;
    }

    public PropertyHandler getIndexedPropertyHandler() {
        return indexedPropertyHandler;
    }

    public void setNamedPropertyHandler(PropertyHandler namedPropertyHandler, boolean stringKeysOnly) {
        this.namedPropertyHandler = namedPropertyHandler;
        this.stringKeysOnly = stringKeysOnly;
    }

    public PropertyHandler getNamedPropertyHandler() {
        return namedPropertyHandler;
    }

    public boolean getStringKeysOnly() {
        return stringKeysOnly;
    }

    public boolean hasPropertyHandler() {
        return (namedPropertyHandler != null) || (indexedPropertyHandler != null);
    }

    public void setFunctionHandler(FunctionTemplate functionHandler) {
        this.functionHandler = functionHandler;
    }

    public FunctionTemplate getFunctionHandler() {
        return functionHandler;
    }

    public void setParentFunctionTemplate(FunctionTemplate parentFunctionTemplate) {
        this.parentFunctionTemplate = parentFunctionTemplate;
    }

    public FunctionTemplate getParentFunctionTemplate() {
        return parentFunctionTemplate;
    }

}
