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
