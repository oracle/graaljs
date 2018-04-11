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
