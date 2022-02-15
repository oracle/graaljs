/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes;

import java.util.Objects;
import java.util.StringJoiner;

import org.graalvm.collections.EconomicMap;

import com.oracle.js.parser.ir.Scope;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSFrameDescriptor {

    private final Object defaultValue;
    private final EconomicMap<Object, JSFrameSlot> identifierToSlotMap = EconomicMap.create();
    private int size;
    private FrameDescriptor frameDescriptor;

    public JSFrameDescriptor() {
        this(Undefined.instance);
    }

    public JSFrameDescriptor(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public JSFrameSlot addFrameSlot(Object identifier) {
        return addFrameSlot(identifier, 0, FrameSlotKind.Illegal);
    }

    public JSFrameSlot addFrameSlot(Object identifier, FrameSlotKind kind) {
        return addFrameSlot(identifier, 0, kind);
    }

    public JSFrameSlot addFrameSlot(Object identifier, int flags, FrameSlotKind kind) {
        CompilerAsserts.neverPartOfCompilation();
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(kind, "kind");
        if (isClosed()) {
            throw new IllegalArgumentException("frame slot registration is closed: " + identifier);
        }
        if (identifierToSlotMap.containsKey(identifier)) {
            throw new IllegalArgumentException("duplicate frame slot: " + identifier);
        }
        int index = size;
        JSFrameSlot slot = new JSFrameSlot(index, toSlotName(identifier), toSlotFlags(flags), kind);
        size++;
        identifierToSlotMap.put(identifier, slot);
        assert identifierToSlotMap.size() == size;
        return slot;
    }

    public JSFrameSlot findFrameSlot(Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        return identifierToSlotMap.get(identifier);
    }

    public JSFrameSlot findOrAddFrameSlot(Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        JSFrameSlot result = findFrameSlot(identifier);
        if (result != null) {
            return result;
        }
        return addFrameSlot(identifier);
    }

    public JSFrameSlot findOrAddFrameSlot(Object identifier, FrameSlotKind kind) {
        CompilerAsserts.neverPartOfCompilation();
        JSFrameSlot result = findFrameSlot(identifier);
        if (result != null) {
            return result;
        }
        return addFrameSlot(identifier, kind);
    }

    public JSFrameSlot findOrAddFrameSlot(Object identifier, int flags, FrameSlotKind kind) {
        CompilerAsserts.neverPartOfCompilation();
        JSFrameSlot result = findFrameSlot(identifier);
        if (result != null) {
            return result;
        }
        return addFrameSlot(identifier, flags, kind);
    }

    public int getSize() {
        return this.size;
    }

    public boolean contains(Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        return identifierToSlotMap.containsKey(identifier);
    }

    public Iterable<Object> getIdentifiers() {
        CompilerAsserts.neverPartOfCompilation();
        return identifierToSlotMap.getKeys();
    }

    public FrameDescriptor toFrameDescriptor() {
        if (this.frameDescriptor != null) {
            return this.frameDescriptor;
        }

        FrameDescriptor.Builder b = FrameDescriptor.newBuilder(size);
        b.defaultValue(defaultValue);
        for (JSFrameSlot slot : identifierToSlotMap.getValues()) {
            int index = b.addSlot(slot.getKind(), slot.getIdentifier(), slot.getInfo());
            assert slot.getIndex() == index;
        }
        FrameDescriptor descriptor = b.build();
        this.frameDescriptor = descriptor;
        return descriptor;
    }

    public boolean isClosed() {
        return frameDescriptor != null;
    }

    public static JSFrameDescriptor createFunctionFrameDescriptor() {
        return new JSFrameDescriptor(Undefined.instance);
    }

    public static JSFrameDescriptor createBlockFrameDescriptor() {
        JSFrameDescriptor desc = new JSFrameDescriptor(Undefined.instance);
        desc.addFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER, FrameSlotKind.Object);
        return desc;
    }

    private static int toSlotFlags(int flags) {
        // other bits not needed
        return flags & JSFrameUtil.SYMBOL_FLAG_MASK;
    }

    private static Object toSlotName(Object identifier) {
        if (identifier instanceof ScopedIdentifier) {
            return ((ScopedIdentifier) identifier).identifier;
        } else {
            return identifier;
        }
    }

    /**
     * A scoped identifier is only equal to identifiers of the same scope.
     */
    public static Object scopedIdentifier(Object identifier, Scope scope) {
        return new ScopedIdentifier(identifier, scope);
    }

    @Override
    public String toString() {
        StringJoiner slots = new StringJoiner(", ", "{", "}");
        for (JSFrameSlot slot : identifierToSlotMap.getValues()) {
            slots.add(slot.getIdentifier().toString());
        }
        return "FrameDescriptor[size=" + size + ", slots=" + slots + "]";
    }

    private static final class ScopedIdentifier {
        final Object identifier;
        final Scope scope;

        ScopedIdentifier(Object identifier, Scope scope) {
            this.identifier = Objects.requireNonNull(identifier);
            this.scope = scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, scope);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ScopedIdentifier)) {
                return false;
            }
            ScopedIdentifier other = (ScopedIdentifier) obj;
            return Objects.equals(identifier, other.identifier) && Objects.equals(scope, other.scope);
        }

        @Override
        public String toString() {
            return identifier.toString();
        }
    }
}
