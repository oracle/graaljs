/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.InlineSupport;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.dyn.AbstractConstantArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InlinedProfileBag;

public abstract class ScriptArray {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public abstract Object getElement(JSDynamicObject object, long index);

    public abstract Object getElementInBounds(JSDynamicObject object, long index);

    public abstract ScriptArray setElementImpl(JSDynamicObject object, long index, Object value, boolean strict);

    public final ScriptArray setElement(JSDynamicObject object, long index, Object value, boolean strict) {
        if (isFrozen()) {
            if (strict) {
                setElementFrozenStrict(index);
            }
            return this;
        } else if (isLengthNotWritable()) {
            if (index >= length(object)) {
                if (strict) {
                    throw Errors.createTypeErrorLengthNotWritable();
                }
                return this;
            }
        }
        return setElementImpl(object, index, value, strict);
    }

    @TruffleBoundary
    private static void setElementFrozenStrict(long index) {
        JSContext context = JavaScriptLanguage.getCurrentLanguage().getJSContext();
        if (context.isOptionNashornCompatibilityMode()) {
            throw Errors.createTypeErrorFormat("Cannot set property \"%d\" of frozen array", index);
        } else {
            throw Errors.createTypeErrorCannotRedefineProperty(Strings.fromLong(index));
        }
    }

    public abstract ScriptArray deleteElementImpl(JSDynamicObject object, long index, boolean strict);

    public final ScriptArray deleteElement(JSDynamicObject object, long index, boolean strict) {
        assert canDeleteElement(object, index, strict);
        return deleteElementImpl(object, index, strict);
    }

    public final boolean canDeleteElement(JSDynamicObject object, long index, boolean strict) {
        if (isSealed()) {
            if (hasElement(object, index)) {
                if (strict) {
                    throw Errors.createTypeErrorCannotDeletePropertyOfSealedArray(index);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if array has an element (not a hole) at this index.
     */
    public abstract boolean hasElement(JSDynamicObject object, long index);

    public abstract long length(JSDynamicObject object);

    public abstract int lengthInt(JSDynamicObject object);

    public static class CreateWritableProfileAccess extends InlinedProfileBag {

        protected static final int REQUIRED_BITS = 4 * CONDITION_PROFILE_STATE_BITS;

        private static final int newArrayLengthZero;
        private static final int newArrayLengthBelowLimit;
        private static final int indexZero;
        private static final int indexLessThanLength;

        static {
            try (var b = new Builder(REQUIRED_BITS)) {
                newArrayLengthZero = b.conditionProfile();
                newArrayLengthBelowLimit = b.conditionProfile();
                indexZero = b.conditionProfile();
                indexLessThanLength = b.conditionProfile();
            }
        }

        private static final CreateWritableProfileAccess UNCACHED = new CreateWritableProfileAccess(null);

        @NeverDefault
        public static CreateWritableProfileAccess getUncached() {
            return UNCACHED;
        }

        @NeverDefault
        public static CreateWritableProfileAccess inline(
                        @InlineSupport.RequiredField(value = InlineSupport.StateField.class, bits = REQUIRED_BITS) InlineSupport.InlineTarget inlineTarget) {
            return new CreateWritableProfileAccess(inlineTarget.getState(0, REQUIRED_BITS));
        }

        protected CreateWritableProfileAccess(InlineSupport.StateField stateField) {
            super(stateField);
        }

        public boolean newArrayLengthZero(Node node, boolean condition) {
            return profile(node, condition, newArrayLengthZero);
        }

        public final boolean newArrayLengthBelowLimit(Node node, boolean condition) {
            return profile(node, condition, newArrayLengthBelowLimit);
        }

        public final boolean indexZero(Node node, boolean condition) {
            return profile(node, condition, indexZero);
        }

        public final boolean indexLessThanLength(Node node, boolean condition) {
            return profile(node, condition, indexLessThanLength);
        }
    }

    public static class SetLengthProfileAccess extends CreateWritableProfileAccess {

        private static final int REQUIRED_BITS = 8 * CONDITION_PROFILE_STATE_BITS;
        protected static final int TOTAL_REQUIRED_BITS = CreateWritableProfileAccess.REQUIRED_BITS + REQUIRED_BITS;

        private static final int lengthZero;
        private static final int lengthLess;
        private static final int zeroBasedSetUsedLength;
        private static final int zeroBasedClearUnusedArea;
        private static final int contiguousZeroUsed;
        private static final int contiguousNegativeUsed;
        private static final int contiguousShrinkUsed;
        private static final int clearUnusedArea;

        static {
            try (var b = new Builder(CreateWritableProfileAccess.REQUIRED_BITS, REQUIRED_BITS)) {
                lengthZero = b.conditionProfile();
                lengthLess = b.conditionProfile();
                zeroBasedSetUsedLength = b.conditionProfile();
                zeroBasedClearUnusedArea = b.conditionProfile();
                contiguousZeroUsed = b.conditionProfile();
                contiguousNegativeUsed = b.conditionProfile();
                contiguousShrinkUsed = b.conditionProfile();
                clearUnusedArea = b.conditionProfile();
            }
        }

        private static final SetLengthProfileAccess UNCACHED = new SetLengthProfileAccess(null);

        @NeverDefault
        public static SetLengthProfileAccess getUncached() {
            return UNCACHED;
        }

        @NeverDefault
        public static SetLengthProfileAccess inline(
                        @InlineSupport.RequiredField(value = InlineSupport.StateField.class, bits = TOTAL_REQUIRED_BITS) InlineSupport.InlineTarget inlineTarget) {
            return new SetLengthProfileAccess(inlineTarget.getState(0, TOTAL_REQUIRED_BITS));
        }

        protected SetLengthProfileAccess(InlineSupport.StateField stateField) {
            super(stateField);
        }

        public final boolean lengthZero(Node node, boolean condition) {
            return profile(node, condition, lengthZero);
        }

        public final boolean lengthLess(Node node, boolean condition) {
            return profile(node, condition, lengthLess);
        }

        public final boolean zeroBasedSetUsedLength(Node node, boolean condition) {
            return profile(node, condition, zeroBasedSetUsedLength);
        }

        public final boolean zeroBasedClearUnusedArea(Node node, boolean condition) {
            return profile(node, condition, zeroBasedClearUnusedArea);
        }

        public final boolean contiguousZeroUsed(Node node, boolean condition) {
            return profile(node, condition, contiguousZeroUsed);
        }

        public final boolean contiguousNegativeUsed(Node node, boolean condition) {
            return profile(node, condition, contiguousNegativeUsed);
        }

        public final boolean contiguousShrinkUsed(Node node, boolean condition) {
            return profile(node, condition, contiguousShrinkUsed);
        }

        public final boolean clearUnusedArea(Node node, boolean condition) {
            return profile(node, condition, clearUnusedArea);
        }
    }

    public abstract ScriptArray setLengthImpl(JSDynamicObject object, long len, Node node, SetLengthProfileAccess profile);

    public final ScriptArray setLength(JSDynamicObject object, long len, boolean strict, Node node, SetLengthProfileAccess profile) {
        if (isLengthNotWritable()) {
            if (strict) {
                throw Errors.createTypeErrorLengthNotWritable();
            }
            return this;
        } else if (isSealed()) {
            assert len >= lastElementIndex(object) + 1; // to be checked by caller
        }
        return setLengthImpl(object, len, node, profile);
    }

    public final ScriptArray setLength(JSDynamicObject object, long len, boolean strict) {
        return setLength(object, len, strict, null, SetLengthProfileAccess.getUncached());
    }

    /**
     * First element index (inclusive).
     */
    public abstract long firstElementIndex(JSDynamicObject object);

    /**
     * Last element index (inclusive).
     */
    public abstract long lastElementIndex(JSDynamicObject object);

    /**
     * Returns the next index. The index is guaranteed either to exist, or be MAX_SAFE_INTEGER.
     * Reason for MAX_SAFE_INTEGER is: this array could be the prototype of another one; returning
     * the length() of this array would be wrong, if the inheriting array is longer, but has a hole
     * at length().
     */
    public abstract long nextElementIndex(JSDynamicObject object, long index);

    /**
     * Returns the previous index. The index is guaranteed either to exist, or be smaller than
     * firstElementIndex().
     */
    public abstract long previousElementIndex(JSDynamicObject object, long index);

    /**
     * Range check only, might be a hole depending on array type.
     */
    public boolean isInBoundsFast(JSDynamicObject object, long index) {
        return firstElementIndex(object) <= index && index <= lastElementIndex(object);
    }

    public Iterable<Object> asIterable(JSDynamicObject object) {
        return new Iterable<>() {
            @Override
            public Iterator<Object> iterator() {
                return new DefaultIterator(object);
            }
        };
    }

    protected final class DefaultIterator implements Iterator<Object> {
        private long currentIndex;
        private final JSDynamicObject arrayObject;

        public DefaultIterator(JSDynamicObject arrayObject) {
            this.arrayObject = arrayObject;
            this.currentIndex = firstElementIndex(arrayObject);
        }

        @Override
        public void remove() {
            currentIndex--;
        }

        @Override
        public Object next() {
            assert currentIndex >= firstElementIndex(arrayObject);
            Object element = getElement(arrayObject, currentIndex);
            currentIndex = nextElementIndex(arrayObject, currentIndex);
            return element;
        }

        @Override
        public boolean hasNext() {
            assert currentIndex >= firstElementIndex(arrayObject);
            return currentIndex <= lastElementIndex(arrayObject);
        }
    }

    /**
     * Creates an Object[] from this array, of size array.length. Does not check the prototype
     * chain, i.e. result can be wrong. Use JSToObjectArrayNode for more correct results.
     *
     * This is mostly used in tests, but also in a few places in Node.js.
     */
    @TruffleBoundary
    public final Object[] toArray(JSDynamicObject thisObj) {
        int len = lengthInt(thisObj);
        Object[] newArray = new Object[len];
        Arrays.fill(newArray, Undefined.instance);
        for (long i = firstElementIndex(thisObj); i <= lastElementIndex(thisObj); i = nextElementIndex(thisObj, i)) {
            if (i >= 0) {
                newArray[(int) i] = getElement(thisObj, i);
            }
        }
        return newArray;
    }

    public static AbstractConstantArray createConstantEmptyArray() {
        return ConstantEmptyArray.createConstantEmptyArray();
    }

    public static AbstractConstantArray createConstantArray(Object[] elements) {
        if (elements == null || elements.length == 0) {
            return createConstantEmptyArray();
        } else {
            return ConstantObjectArray.createConstantObjectArray();
        }
    }

    public static boolean valueIsByte(int value) {
        return Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE;
    }

    @TruffleBoundary
    public String toString(JSDynamicObject object) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (; i < length(object); i++) {
            if (i != 0) {
                sb.append(",");
            }
            Object element = getElement(object, i);
            if (element != Null.instance && element != Undefined.instance) {
                sb.append(element);
            }
        }
        if (i < length(object)) {
            sb.append(",... [" + (length(object) - i + 1) + " more]");
        }
        return sb.toString();
    }

    @TruffleBoundary
    protected static final void traceArrayTransition(ScriptArray oldArray, ScriptArray newArray, long index, Object value) {
        String access = oldArray.getClass().getSimpleName() + " -> " + newArray.getClass().getSimpleName();

        Stream<Node> nodeStream = null;
        List<FrameInstance> stackTrace = new ArrayList<>();
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            stackTrace.add(frameInstance);
            return null;
        });
        nodeStream = StreamSupport.stream(stackTrace.spliterator(), false).filter(fi -> fi.getCallNode() != null).map(fi -> fi.getCallNode());
        int stackTraceLimit = JavaScriptLanguage.getCurrentLanguage().getJSContext().getLanguageOptions().stackTraceLimit();
        StackTraceElement[] array = nodeStream.filter(n -> n.getEncapsulatingSourceSection() != null).map(node -> {
            SourceSection callNodeSourceSection = node.getEncapsulatingSourceSection();
            String declaringClass = "js";
            String methodName = node.getRootNode().getName();
            String fileName = callNodeSourceSection.isAvailable() ? callNodeSourceSection.getSource().getName() : "<unknown>";
            int startLine = callNodeSourceSection.getStartLine();
            return new StackTraceElement(declaringClass, methodName != null ? methodName : "<unknown>", fileName, startLine);
        }).limit(stackTraceLimit).toArray(StackTraceElement[]::new);

        System.out.printf("[js]      array transition %-48s |index %5s |value %-20s |caller %5s\n", access, index, value, array[0]);
    }

    @TruffleBoundary
    protected static final void traceWrite(String access, long index, Object value) {
        System.out.printf("[js]      array set        %-48s |index %5s |value %-20s\n", access, index, value);
    }

    /**
     * Returns true if this array type could have hole values in it. Doesn't tell whether it
     * actually HAS holes.
     */
    @Idempotent
    public boolean isHolesType() {
        return false;
    }

    /**
     * Returns true when this array actually has holes between {@link #firstElementIndex} and
     * {@link #lastElementIndex}. Implies {@link #isHolesType()}.
     *
     * @see #hasHolesOrUnused
     */
    public abstract boolean hasHoles(JSDynamicObject object);

    /**
     * Returns true when this array has any hole or unused elements between 0 and {@link #length}.
     *
     * @see #hasHoles
     */
    public boolean hasHolesOrUnused(JSDynamicObject object) {
        return hasHoles(object);
    }

    /**
     * This function deletes all elements in the range from [start..end[. This is equivalent to
     * shifting the whole array, starting with element index end, by end-start positions to the
     * left. Can be used by e.g. Array.prototype.splice;
     */
    public abstract ScriptArray removeRangeImpl(JSDynamicObject object, long start, long end);

    public final ScriptArray removeRange(JSDynamicObject object, long start, long end) {
        assert start >= 0 && start <= end;
        if (isSealed()) {
            throw Errors.createTypeErrorCannotDeletePropertyOfSealedArray(start);
        }
        return removeRangeImpl(object, start, end);
    }

    public final ScriptArray removeRange(JSDynamicObject object, long start, long end, BranchProfile errorBranch) {
        assert start >= 0 && start <= end;
        if (isSealed()) {
            errorBranch.enter();
            throw Errors.createTypeErrorCannotDeletePropertyOfSealedArray(start);
        }
        return removeRangeImpl(object, start, end);
    }

    /**
     * This function shifts all elements in the range from [0..limit[. Depending on the underlying
     * implementation, the shift operation might be zero-copy. Can be used by e.g.
     * Array.prototype.shift;
     */
    public ScriptArray shiftRangeImpl(JSDynamicObject object, long limit) {
        return removeRangeImpl(object, 0, limit);
    }

    public final ScriptArray shiftRange(JSDynamicObject object, long from) {
        assert from >= 0;
        assert !isSealed();
        return shiftRangeImpl(object, from);
    }

    /**
     * This method grows the array by adding more elements of a given size. An offset parameter can
     * be used to specify where the new elements have to be added (starting from zero). The
     * operation is equivalent to shifting (right) the whole array or its part as defined by the
     * offset parameter.
     *
     * @param offset starting offset position
     * @param size size of the inserted empty array
     *
     * @return a {@link ScriptArray} instance with the new size
     */
    public abstract ScriptArray addRangeImpl(JSDynamicObject object, long offset, int size);

    public final ScriptArray addRange(JSDynamicObject object, long offset, int size) {
        if (!isExtensible()) {
            throw addRangeNotExtensible();
        }
        return addRangeImpl(object, offset, size);
    }

    @TruffleBoundary
    private JSException addRangeNotExtensible() {
        if (isFrozen()) {
            throw Errors.createTypeError("Cannot add property of frozen array");
        } else if (isSealed()) {
            throw Errors.createTypeError("Cannot add property to sealed array");
        } else {
            throw Errors.createTypeError("Cannot add property to non-extensible array");
        }
    }

    public List<Object> ownPropertyKeys(JSDynamicObject object) {
        assert !isHolesType() || !hasHoles(object);
        return ownPropertyKeysContiguous(object);
    }

    protected final List<Object> ownPropertyKeysContiguous(JSDynamicObject object) {
        return makeRangeList(firstElementIndex(object), lastElementIndex(object) + 1);
    }

    @TruffleBoundary
    protected final List<Object> ownPropertyKeysHoles(JSDynamicObject object) {
        long currentIndex = firstElementIndex(object);
        long start = currentIndex;
        long end = currentIndex;
        int total = 0;
        List<Long> rangeList = new ArrayList<>();
        while (currentIndex <= lastElementIndex(object)) {
            if (currentIndex == end) {
                end = currentIndex + 1;
            } else {
                assert end < currentIndex;
                assert start < end;
                total += end - start;
                rangeList.add(start);
                rangeList.add(end);
                start = currentIndex;
                end = currentIndex + 1;
            }
            currentIndex = nextElementIndex(object, currentIndex);
        }
        if (start < end) {
            total += end - start;
            if (rangeList.isEmpty()) {
                return makeRangeList(start, end);
            }
            rangeList.add(start);
            rangeList.add(end);
        }
        return makeMultiRangeList(total, toLongArray(rangeList));
    }

    private static long[] toLongArray(List<Long> longList) {
        long[] longArray = new long[longList.size()];
        for (int i = 0; i < longArray.length; i++) {
            longArray[i] = longList.get(i);
        }
        return longArray;
    }

    public static List<Object> makeRangeList(final long rangeStart, final long rangeEnd) {
        assert rangeEnd - rangeStart >= 0 && rangeEnd - rangeStart <= Integer.MAX_VALUE;
        return new AbstractList<>() {
            @Override
            public Object get(int index) {
                if (index >= 0 && rangeStart + index < rangeEnd) {
                    return Strings.fromLong(rangeStart + index);
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }

            @Override
            public int size() {
                return (int) (rangeEnd - rangeStart);
            }
        };
    }

    protected static List<Object> makeMultiRangeList(final int total, final long[] ranges) {
        return new AbstractList<>() {
            @Override
            public Object get(int index) {
                if (index >= 0) {
                    long relativeIndex = index;
                    for (int rangeIndex = 0; rangeIndex < ranges.length; rangeIndex += 2) {
                        long rangeStart = ranges[rangeIndex];
                        long rangeEnd = ranges[rangeIndex + 1];
                        long rangeLen = rangeEnd - rangeStart;
                        if (relativeIndex < rangeLen) {
                            return Strings.fromLong(rangeStart + relativeIndex);
                        } else {
                            relativeIndex -= rangeLen;
                        }
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return total;
            }
        };
    }

    protected static int nextPower(int length) {
        if (length < 8) {
            return 8;
        } else {
            return nextPow2(length);
        }
    }

    /** power of 2 >= x. */
    private static int nextPow2(int val) {
        int x = val - 1;
        x |= (x >> 1);
        x |= (x >> 2);
        x |= (x >> 4);
        x |= (x >> 8);
        x |= (x >> 16);
        return x + 1;
    }

    public boolean isSealed() {
        return false;
    }

    public boolean isFrozen() {
        return false;
    }

    public boolean isLengthNotWritable() {
        return false;
    }

    public boolean isExtensible() {
        return true;
    }

    public abstract ScriptArray seal();

    public abstract ScriptArray freeze();

    public abstract ScriptArray setLengthNotWritable();

    public abstract ScriptArray preventExtensions();

    public final boolean isInstance(ScriptArray other) {
        CompilerAsserts.partialEvaluationConstant(this);
        return this == other;
    }

    public final ScriptArray cast(ScriptArray other) {
        CompilerAsserts.partialEvaluationConstant(this);
        assert this == other;
        return this;
    }
}
