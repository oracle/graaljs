/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ClassElementDefinitionRecord {

    public enum Kind {
        Method,
        Field,
        Getter,
        Setter,
        AutoAccessor,
        /** Combined getter + setter pair. Cannot have any decorators. */
        AccessorPair,
        StaticBlock,
    }

    private static final Object[] EMPTY = new Object[0];

    private final Kind kind;
    private final Object key;
    private final boolean anonymousFunctionDefinition;
    private final boolean isPrivate;

    /** The function for a method or the initializer function for a field. */
    private Object value;
    /** The getter function for an accessor definition. */
    private Object getter;
    /** The setter function for an accessor definition. */
    private Object setter;
    /** The decorators applied to the class element, if any. */
    private Object[] decorators;
    /** The initializers of the field or accessor, if any. May contain null elements. */
    private Object[] initializers;
    private int initializersCount;
    /** Private field storage key for an auto accessor or a private field. */
    private HiddenKey backingStorageKey;

    public static ClassElementDefinitionRecord createPublicField(Object key, Object value, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Field, key, value, null, null, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateField(Object key, Object value, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Field, key, value, null, null, true, false, decorators, (HiddenKey) key);
    }

    public static ClassElementDefinitionRecord createPublicMethod(Object key, Object value, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Method, key, value, null, null, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateMethod(Object key, Object value, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Method, key, value, null, null, true, false, decorators);
    }

    public static ClassElementDefinitionRecord createPublicGetter(Object key, Object getter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Getter, key, null, getter, null, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateGetter(Object key, Object getter, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Getter, key, null, getter, null, true, false, decorators);
    }

    public static ClassElementDefinitionRecord createPublicSetter(Object key, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Setter, key, null, null, setter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateSetter(Object key, Object setter, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Setter, key, null, null, setter, true, false, decorators);
    }

    public static ClassElementDefinitionRecord createPublicAccessor(Object key, Object getter, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.AccessorPair, key, null, getter, setter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateAccessor(Object key, Object getter, Object setter, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.AccessorPair, key, null, getter, setter, true, false, decorators);
    }

    public static ClassElementDefinitionRecord createPublicAutoAccessor(Object key, HiddenKey backingStorageKey, Object value, Object getter, Object setter,
                    boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.AutoAccessor, key, value, getter, setter, false, anonymousFunctionDefinition, decorators, backingStorageKey);
    }

    public static ClassElementDefinitionRecord createPrivateAutoAccessor(Object key, HiddenKey backingStorageKey, Object value, Object getter, Object setter, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.AutoAccessor, key, value, getter, setter, true, false, decorators, backingStorageKey);
    }

    public static ClassElementDefinitionRecord createStaticBlock(Object initializer) {
        return new ClassElementDefinitionRecord(Kind.StaticBlock, null, initializer, null, null, false, false, null);
    }

    protected ClassElementDefinitionRecord(Kind kind, Object key, Object value, Object getter, Object setter, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        this(kind, key, value, getter, setter, isPrivate, anonymousFunctionDefinition, decorators, null);
    }

    protected ClassElementDefinitionRecord(Kind kind, Object key, Object value, Object getter, Object setter, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators,
                    HiddenKey backingStorageKey) {
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.getter = getter;
        this.setter = setter;
        this.anonymousFunctionDefinition = anonymousFunctionDefinition;
        this.isPrivate = isPrivate;
        this.decorators = decorators;
        this.initializers = (decorators == null || decorators.length == 0) ? ScriptArray.EMPTY_OBJECT_ARRAY : new Object[decorators.length];
        this.backingStorageKey = backingStorageKey;
        assert kind == Kind.AutoAccessor
                        ? (value != null && getter != null && setter != null)
                        : (value != null) != (getter != null || setter != null);
        assert kind != Kind.AccessorPair || (decorators == null || decorators.length == 0);
    }

    public boolean isMethod() {
        return this.kind == Kind.Method;
    }

    public boolean isGetter() {
        return this.kind == Kind.Getter;
    }

    public boolean isSetter() {
        return this.kind == Kind.Setter;
    }

    public boolean isAccessor() {
        return isGetter() || isSetter() || kind == Kind.AccessorPair;
    }

    public boolean isAutoAccessor() {
        return this.kind == Kind.AutoAccessor;
    }

    public boolean isField() {
        return this.kind == Kind.Field;
    }

    public boolean isStaticBlock() {
        return this.kind == Kind.StaticBlock;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Object[] getDecorators() {
        return decorators;
    }

    public boolean hasDecorators() {
        return decorators != null;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object newValue) {
        this.value = newValue;
    }

    public Object[] getInitializers() {
        return initializers;
    }

    public int getInitializersCount() {
        return initializersCount;
    }

    /**
     * Adds an initializer to be applied when the field is defined. Each decorator invocation may
     * add only one initializer to this list, therefore we can use a fixed size array here (unused
     * elements will be null).
     */
    public void addInitializer(Object initializer) {
        initializers[initializersCount++] = initializer;
    }

    public void cleanDecorator() {
        this.decorators = EMPTY;
    }

    public boolean isAnonymousFunction() {
        return anonymousFunctionDefinition;
    }

    public void setGetter(Object newGetter) {
        this.getter = newGetter;
    }

    public void setSetter(Object newSetter) {
        this.setter = newSetter;
    }

    public Object getGetter() {
        return getter;
    }

    public Object getSetter() {
        return setter;
    }

    public HiddenKey getBackingStorageKey() {
        return backingStorageKey;
    }

    @Override
    public String toString() {
        return "ClassElementDefinitionRecord [kind=" + kind + ", key=" + key + ", value=" + value + ", getter=" + getter + ", setter=" + setter + "]";
    }
}
