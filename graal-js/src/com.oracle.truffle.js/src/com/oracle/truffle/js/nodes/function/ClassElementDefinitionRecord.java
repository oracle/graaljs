/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.ArrayList;
import java.util.List;

public class ClassElementDefinitionRecord {

    public enum Kind {
        Method,
        Field,
        Getter,
        Setter,
        AutoAccessor,
    }

    private static final Object[] EMPTY = new Object[0];

    protected final JSContext context;

    private final Kind kind;
    private final Object key;
    private final boolean anonymousFunctionDefinition;
    private final boolean isPrivate;

    Object value;
    private Object[] decorators;
    private Object getter;
    private Object setter;
    private List<Object> appendedInitializers;
    private Object[] initializers;

    public static ClassElementDefinitionRecord createField(JSContext context, Object key, Object value, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Field, context, key, value, isPrivate, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicMethod(JSContext context, Object key, Object value, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Method, context, key, value, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicGetter(JSContext context, Object key, Object getter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Getter, context, key, getter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicSetter(JSContext context, Object key, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Setter, context, key, setter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateMethod(JSContext context, Object key, int frameSlot, int brandSlot, int blockSlot, Object value, boolean anonymousFunctionDefinition,
                    Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Method, context, key, frameSlot, brandSlot, blockSlot, value, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateGetter(JSContext context, Object key, int frameSlot, int brandSlot, int blockSlot, Object value, boolean anonymousFunctionDefinition,
                    Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Getter, context, key, frameSlot, brandSlot, blockSlot, value, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateSetter(JSContext context, Object key, int frameSlot, int brandSlot, int blockSlot, Object value, boolean anonymousFunctionDefinition,
                    Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Getter, context, key, frameSlot, brandSlot, blockSlot, value, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createAutoAccessor(JSContext context, Object key, HiddenKey backingStorageKey, Object value, boolean isPrivate, boolean anonymousFunctionDefinition,
                    Object[] decorators) {
        return new AutoAccessor(context, key, backingStorageKey, value, isPrivate, anonymousFunctionDefinition, decorators);
    }

    protected ClassElementDefinitionRecord(Kind kind, JSContext context, Object key, Object value, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.anonymousFunctionDefinition = anonymousFunctionDefinition;
        this.decorators = decorators;
        this.isPrivate = isPrivate;
        this.context = context;
    }

    public final boolean isMethod() {
        return this.kind == Kind.Method;
    }

    public final boolean isGetter() {
        return this.kind == Kind.Getter;
    }

    public final boolean isSetter() {
        return this.kind == Kind.Setter;
    }

    public final boolean isAutoAccessor() {
        return this.kind == Kind.AutoAccessor;
    }

    public final boolean isField() {
        return this.kind == Kind.Field;
    }

    public final Kind getKind() {
        return kind;
    }

    public final boolean isPrivate() {
        return isPrivate;
    }

    public Object[] getDecorators() {
        return decorators;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Object[] getInitializers() {
        return initializers == null ? EMPTY : initializers;
    }

    @TruffleBoundary
    public void appendInitializer(Object initializer) {
        if (appendedInitializers == null) {
            appendedInitializers = new ArrayList<>();
        }
        appendedInitializers.add(initializer);
    }

    public void setValue(Object newValue) {
        this.value = newValue;
    }

    @TruffleBoundary
    public void cleanDecorator() {
        this.decorators = EMPTY;
        this.initializers = appendedInitializers == null ? EMPTY : appendedInitializers.toArray(EMPTY);
    }

    public Object isAnonymousFunction() {
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

    public static final class PrivateFrameBasedElementDefinitionRecord extends ClassElementDefinitionRecord {

        private final int keySlot;
        private final int brandSlot;
        private final int blockScopeSlot;

        private PrivateFrameBasedElementDefinitionRecord(Kind kind, JSContext context, Object key, int keySlot, int brandSlot, int blockScopeSlot, Object value, boolean anonymousFunctionDefinition,
                        Object[] decorators) {
            super(kind, context, key, value, true, anonymousFunctionDefinition, decorators);
            this.keySlot = keySlot;
            this.brandSlot = brandSlot;
            this.blockScopeSlot = blockScopeSlot;
        }

        public int getKeySlot() {
            return keySlot;
        }

        public int getBrandSlot() {
            return brandSlot;
        }

        public int getBlockScopeSlot() {
            return blockScopeSlot;
        }
    }

    public static class AutoAccessor extends ClassElementDefinitionRecord {

        private final HiddenKey backingStorageKey;

        protected AutoAccessor(JSContext context, Object key, HiddenKey backingStorageKey, Object value, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
            super(Kind.AutoAccessor, context, key, value, isPrivate, anonymousFunctionDefinition, decorators);
            this.backingStorageKey = backingStorageKey;
        }

        public HiddenKey getBackingStorageKey() {
            return backingStorageKey;
        }
    }
}
