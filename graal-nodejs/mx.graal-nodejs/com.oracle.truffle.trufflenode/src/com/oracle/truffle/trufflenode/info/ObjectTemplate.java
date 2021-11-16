/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

import com.oracle.truffle.trufflenode.node.ExecuteNativePropertyHandlerNode;

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
    private int internalFieldCount;

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

    public void setInternalFieldCount(int internalFieldCount) {
        this.internalFieldCount = internalFieldCount;
    }

    public int getInternalFieldCount() {
        return internalFieldCount;
    }

    public Descriptor getEngineCacheDescriptor(ExecuteNativePropertyHandlerNode.Mode mode) {
        return new Descriptor(this, mode);
    }

    @Override
    public String toString() {
        return "ObjectTemplate{" +
                        "internalFieldCount=" + internalFieldCount +
                        ", stringKeysOnly=" + stringKeysOnly +
                        ", accessors=" + (accessors != null) +
                        ", values=" + (values != null) +
                        ", indexedPropertyHandler=" + (indexedPropertyHandler != null) +
                        ", namedPropertyHandler=" + (namedPropertyHandler != null) +
                        ", functionHandler.id=" + (functionHandler != null ? functionHandler.getId() : "null") +
                        ", parentFunctionTemplate.id=" + (parentFunctionTemplate != null ? parentFunctionTemplate.getId() : "null") +
                        '}';
    }

    public static class Descriptor {

        private final int internalFieldCount;
        private final boolean stringKeysOnly;
        private final boolean hasPropertyHandler;
        private final ExecuteNativePropertyHandlerNode.Mode mode;

        public Descriptor(ObjectTemplate objectTemplate, ExecuteNativePropertyHandlerNode.Mode mode) {
            this.internalFieldCount = objectTemplate.internalFieldCount;
            this.stringKeysOnly = objectTemplate.stringKeysOnly;
            this.hasPropertyHandler = objectTemplate.hasPropertyHandler();
            this.mode = mode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Descriptor that = (Descriptor) o;
            return this.internalFieldCount == that.internalFieldCount &&
                            this.stringKeysOnly == that.stringKeysOnly &&
                            this.hasPropertyHandler == that.hasPropertyHandler &&
                            this.mode == that.mode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.internalFieldCount,
                            this.stringKeysOnly,
                            this.hasPropertyHandler,
                            this.mode);
        }

        @Override
        public String toString() {
            return "Descriptor{" +
                            "internalFieldCount=" + internalFieldCount +
                            ", stringKeysOnly=" + stringKeysOnly +
                            ", hasPropertyHandler=" + hasPropertyHandler +
                            ", mode=" + mode +
                            '}';
        }
    }
}
