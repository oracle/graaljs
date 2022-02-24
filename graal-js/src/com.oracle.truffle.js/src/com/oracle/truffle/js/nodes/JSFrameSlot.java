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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.runtime.JSFrameUtil;

/**
 * Describes a JS frame slot. Used as a temporary representation during parsing.
 */
public final class JSFrameSlot {
    private final int index;
    private final int flags;
    private final Object identifier;
    private final Object info;
    private final FrameSlotKind kind;

    public JSFrameSlot(int index, Object identifier, int flags, FrameSlotKind kind) {
        this.index = index;
        this.flags = flags;
        this.identifier = Objects.requireNonNull(identifier);
        this.info = FrameSlotFlags.of(flags);
        this.kind = kind;
    }

    public JSFrameSlot(int index, Object identifier, int flags) {
        this(index, identifier, flags, FrameSlotKind.Illegal);
    }

    public static JSFrameSlot fromIndexedFrameSlot(FrameDescriptor desc, int index) {
        return new JSFrameSlot(index, desc.getSlotName(index), JSFrameUtil.getFlags(desc, index), desc.getSlotKind(index));
    }

    public int getIndex() {
        return index;
    }

    public Object getIdentifier() {
        return identifier;
    }

    public int getFlags() {
        return flags;
    }

    public Object getInfo() {
        return info;
    }

    public FrameSlotKind getKind() {
        return kind;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + flags;
        result = prime * result + identifier.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JSFrameSlot)) {
            return false;
        }
        JSFrameSlot other = (JSFrameSlot) obj;
        if (index != other.index) {
            return false;
        }
        if (flags != other.flags) {
            return false;
        }
        if (!identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FrameSlot[" + index +
                        ", " + identifier +
                        (flags != 0 ? ", " + flags : "") +
                        (kind != FrameSlotKind.Illegal ? ", " + kind : "") +
                        "]";
    }

    /**
     * Interning cache for boxed integer representation of frame slot flags.
     *
     * Frame slot flags are stored as boxed integer objects in the frame descriptor (as slot info)
     * and since there is going to be only a limited number of different flags, caching them allows
     * us to save memory footprint. Values <128 are already cached by {@link Integer#valueOf(int)},
     * so we do not cache them again. The interned objects are never compared by identity.
     */
    static final class FrameSlotFlags {
        private static final Map<Integer, Integer> cachedFlags = new ConcurrentHashMap<>();

        private FrameSlotFlags() {
        }

        static Integer of(int flags) {
            Integer boxed = Integer.valueOf(flags);
            if (flags < -128 || flags > 127) {
                Integer cached = cachedFlags.get(boxed);
                if (cached != null) {
                    return cached;
                }
                cached = cachedFlags.putIfAbsent(boxed, boxed);
                if (cached != null) {
                    return cached;
                }
            }
            return boxed;
        }
    }
}
