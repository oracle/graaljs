/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.trufflenode.GraalJSAccess;

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
    private final int length;
    private FunctionTemplate parent;
    private TruffleString className = Strings.EMPTY_STRING;
    private boolean readOnlyPrototype;
    private JSFunctionData functionData;
    private JSFunctionObject functionObj;
    private final boolean singleFunctionTemplate;

    public FunctionTemplate(int id, long functionPointer, Object additionalData, FunctionTemplate signature, int length, boolean isConstructor, boolean singleFunctionTemplate) {
        functionObjectTemplate = new ObjectTemplate();
        instanceTemplate = new ObjectTemplate();
        prototypeTemplate = isConstructor ? new ObjectTemplate() : null;
        this.id = id;
        this.functionPointer = functionPointer;
        this.additionalData = additionalData;
        this.signature = signature;
        this.length = length;
        this.singleFunctionTemplate = singleFunctionTemplate;
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

    public void setFunctionData(JSFunctionData functionData) {
        this.functionData = functionData;
    }

    public JSFunctionData getFunctionData() {
        return functionData;
    }

    public void setFunctionObject(JSRealm realm, JSFunctionObject functionObj) {
        if (singleFunctionTemplate) {
            this.functionObj = functionObj;
        } else {
            GraalJSAccess.getRealmEmbedderData(realm).setFunctionTemplateObject(id, functionObj);
        }
    }

    public JSFunctionObject getFunctionObject(JSRealm realm) {
        return singleFunctionTemplate ? functionObj : GraalJSAccess.getRealmEmbedderData(realm).getFunctionTemplateObject(id);
    }

    public int getId() {
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

    public int getLength() {
        return length;
    }

    public TruffleString getClassName() {
        return className;
    }

    public void setClassName(TruffleString className) {
        this.className = className;
    }

    public FunctionTemplate getParent() {
        return parent;
    }

    public void setParent(FunctionTemplate parent) {
        this.parent = parent;
    }

    public void markPrototypeReadOnly() {
        this.readOnlyPrototype = true;
    }

    public boolean hasReadOnlyPrototype() {
        return readOnlyPrototype;
    }

    public boolean isSingleFunctionTemplate() {
        return singleFunctionTemplate;
    }

    public Descriptor getEngineCacheDescriptor() {
        return new Descriptor(this);
    }

    @Override
    public String toString() {
        return "FunctionTemplate{" +
                        "id=" + id +
                        ", className='" + className + '\'' +
                        ", length=" + length +
                        ", readOnlyPrototype=" + readOnlyPrototype +
                        ", parent.id=" + (parent != null ? parent.getId() : "null") +
                        ", signature=" + (signature != null) +
                        ", functionData=" + (functionData != null) +
                        ", functionObj=" + (functionObj != null) +
                        ", additionalData=" + (additionalData != null) +
                        ", functionPointer=" + functionPointer +
                        ", singleFunctionTemplate=" + singleFunctionTemplate +
                        ", functionObjectTemplate=" + functionObjectTemplate +
                        ", instanceTemplate=" + instanceTemplate +
                        ", prototypeTemplate=" + prototypeTemplate +
                        '}';
    }

    public static class Descriptor {

        private final int length;
        private final TruffleString className;
        private final boolean readOnlyPrototype;
        private final boolean prototypeTemplateNull;
        private final boolean singleFunctionTemplate;
        private final int instanceTemplateInternalFieldCount;

        public Descriptor(FunctionTemplate functionTemplate) {
            this.length = functionTemplate.length;
            this.className = functionTemplate.className;
            this.readOnlyPrototype = functionTemplate.readOnlyPrototype;
            this.prototypeTemplateNull = functionTemplate.getPrototypeTemplate() == null;
            this.singleFunctionTemplate = functionTemplate.singleFunctionTemplate;
            this.instanceTemplateInternalFieldCount = functionTemplate.getInstanceTemplate().getInternalFieldCount();
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
            return this.length == that.length &&
                            Objects.equals(this.className, that.className) &&
                            this.readOnlyPrototype == that.readOnlyPrototype &&
                            this.prototypeTemplateNull == that.prototypeTemplateNull &&
                            this.singleFunctionTemplate == that.singleFunctionTemplate &&
                            this.instanceTemplateInternalFieldCount == that.instanceTemplateInternalFieldCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.length,
                            this.className,
                            this.readOnlyPrototype,
                            this.prototypeTemplateNull,
                            this.singleFunctionTemplate,
                            this.instanceTemplateInternalFieldCount);
        }

        @Override
        public String toString() {
            return "Descriptor{" +
                            "length=" + length +
                            ", className='" + className + '\'' +
                            ", readOnlyPrototype=" + readOnlyPrototype +
                            ", prototypeTemplateNull=" + prototypeTemplateNull +
                            ", singleFunctionTemplate=" + singleFunctionTemplate +
                            ", instanceTemplateInternalFieldCount=" + instanceTemplateInternalFieldCount +
                            '}';
        }
    }
}
