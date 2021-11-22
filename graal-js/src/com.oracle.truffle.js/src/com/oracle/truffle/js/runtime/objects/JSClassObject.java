/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;

/**
 * Delegates methods to JSClass.
 */
public abstract class JSClassObject extends JSObject {

    protected JSClassObject(Shape shape) {
        super(shape);
    }

    @Override
    public String getClassName() {
        return getJSClass().getClassName(this);
    }

    @Override
    public JSDynamicObject getPrototypeOf() {
        return (JSDynamicObject) getJSClass().getPrototypeOf(this);
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject newPrototype) {
        return getJSClass().setPrototypeOf(this, newPrototype);
    }

    @Override
    public boolean isExtensible() {
        return getJSClass().isExtensible(this);
    }

    @Override
    public boolean preventExtensions(boolean doThrow) {
        return getJSClass().preventExtensions(this, doThrow);
    }

    @Override
    public PropertyDescriptor getOwnProperty(Object propertyKey) {
        return getJSClass().getOwnProperty(this, propertyKey);
    }

    @Override
    public boolean defineOwnProperty(Object key, PropertyDescriptor value, boolean doThrow) {
        return getJSClass().defineOwnProperty(this, key, value, doThrow);
    }

    @Override
    public boolean hasProperty(Object key) {
        return getJSClass().hasProperty(this, key);
    }

    @Override
    public boolean hasProperty(long index) {
        return getJSClass().hasProperty(this, index);
    }

    @Override
    public boolean hasOwnProperty(Object key) {
        return getJSClass().hasOwnProperty(this, key);
    }

    @Override
    public boolean hasOwnProperty(long index) {
        return getJSClass().hasOwnProperty(this, index);
    }

    @Override
    public Object getHelper(Object receiver, Object key, Node encapsulatingNode) {
        return getJSClass().getHelper(this, receiver, key, encapsulatingNode);
    }

    @Override
    public Object getHelper(Object receiver, long index, Node encapsulatingNode) {
        return getJSClass().getHelper(this, receiver, index, encapsulatingNode);
    }

    @Override
    public Object getOwnHelper(Object receiver, Object key, Node encapsulatingNode) {
        return getJSClass().getOwnHelper(this, receiver, key, encapsulatingNode);
    }

    @Override
    public Object getOwnHelper(Object receiver, long index, Node encapsulatingNode) {
        return getJSClass().getOwnHelper(this, receiver, index, encapsulatingNode);
    }

    @Override
    public Object getMethodHelper(Object receiver, Object key, Node encapsulatingNode) {
        return getJSClass().getMethodHelper(this, receiver, key, encapsulatingNode);
    }

    @Override
    public boolean set(Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return getJSClass().set(this, key, value, receiver, isStrict, encapsulatingNode);
    }

    @Override
    public boolean set(long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return getJSClass().set(this, index, value, receiver, isStrict, encapsulatingNode);
    }

    @Override
    public boolean delete(Object key, boolean isStrict) {
        return getJSClass().delete(this, key, isStrict);
    }

    @Override
    public boolean delete(long index, boolean isStrict) {
        return getJSClass().delete(this, index, isStrict);
    }

    @Override
    public List<Object> getOwnPropertyKeys(boolean strings, boolean symbols) {
        return getJSClass().getOwnPropertyKeys(this, strings, symbols);
    }

    @Override
    public boolean hasOnlyShapeProperties() {
        return getJSClass().hasOnlyShapeProperties(this);
    }

    @Override
    public String toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        return getJSClass().toDisplayStringImpl(this, allowSideEffects, format, depth);
    }

    @Override
    public String getBuiltinToStringTag() {
        return getJSClass().getBuiltinToStringTag(this);
    }

    @Override
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        return getJSClass().setIntegrityLevel(this, freeze, doThrow);
    }

    @Override
    public boolean testIntegrityLevel(boolean frozen) {
        return getJSClass().testIntegrityLevel(this, frozen);
    }

    @Override
    public String toString() {
        return getJSClass().toString(this);
    }

}
