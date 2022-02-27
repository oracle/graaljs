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
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;

/**
 * @see JSOrdinary
 */
public abstract class JSOrdinaryObject extends JSNonProxyObject implements JSCopyableObject {

    protected JSOrdinaryObject(Shape shape) {
        super(shape);
    }

    public static JSOrdinaryObject create(Shape shape) {
        Class<? extends DynamicObject> layout = shape.getLayoutClass();
        if (layout == DefaultLayout.class) {
            return new DefaultLayout(shape);
        } else if (layout == InternalFieldLayout.class) {
            return new InternalFieldLayout(shape);
        } else {
            return new BareLayout(shape);
        }
    }

    @Override
    public TruffleString getClassName() {
        return JSOrdinary.CLASS_NAME;
    }

    @TruffleBoundary
    @Override
    public Object getValue(long index) {
        // convert index only once
        return getValue(Strings.fromLong(index));
    }

    @Override
    public final boolean hasOnlyShapeProperties() {
        return true;
    }

    public static final class BareLayout extends JSOrdinaryObject {
        protected BareLayout(Shape shape) {
            super(shape);
        }

        @Override
        protected JSObject copyWithoutProperties(Shape shape) {
            return new BareLayout(shape);
        }
    }

    public static final class DefaultLayout extends JSOrdinaryObject {
        @DynamicField Object o0;
        @DynamicField Object o1;
        @DynamicField Object o2;
        @DynamicField Object o3;
        @DynamicField long p0;
        @DynamicField long p1;
        @DynamicField long p2;

        protected DefaultLayout(Shape shape) {
            super(shape);
        }

        @Override
        protected JSObject copyWithoutProperties(Shape shape) {
            return new DefaultLayout(shape);
        }
    }

    public static final class InternalFieldLayout extends JSOrdinaryObject {
        @DynamicField Object o0;
        @DynamicField Object o1;
        @DynamicField Object o2;

        private static final long[] EMPTY_LONG_ARRAY = new long[0];
        private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

        private long[] internalPointerFields = EMPTY_LONG_ARRAY;
        private Object[] internalObjectFields = EMPTY_OBJECT_ARRAY;

        protected InternalFieldLayout(Shape shape) {
            super(shape);
        }

        @Override
        protected JSObject copyWithoutProperties(Shape shape) {
            return new InternalFieldLayout(shape);
        }

        public long getInternalFieldPointer(int index) {
            return internalPointerFields[index];
        }

        public void setInternalFieldPointer(int index, long value) {
            this.internalPointerFields[index] = value;
        }

        public Object getInternalFieldObject(int index) {
            if (index >= internalObjectFields.length) {
                return null;
            }
            return internalObjectFields[index];
        }

        public void setInternalFieldObject(int index, Object value) {
            if (internalObjectFields.length == 0) {
                internalObjectFields = new Object[getInternalFieldCount()];
            }
            this.internalObjectFields[index] = value;
        }

        public int getInternalFieldCount() {
            return internalPointerFields.length;
        }

        public void setInternalFieldCount(int internalFieldCount) {
            assert getInternalFieldCount() == 0;
            internalPointerFields = new long[internalFieldCount];
            // Object[] is initialized lazily.
        }
    }
}
