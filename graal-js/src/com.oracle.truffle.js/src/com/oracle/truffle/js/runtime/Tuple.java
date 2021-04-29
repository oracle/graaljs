package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.interop.JSMetaType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Implementation of the Tuple (primitive) type which is an ordered sequence of ECMAScript primitive values.
 * Since a Tuple value is completely immutable and will not change over time, you will not find a setter-method here.
 *
 * @see com.oracle.truffle.js.runtime.builtins.JSTupleObject
 */
@ExportLibrary(InteropLibrary.class)
@ValueType
public final class Tuple implements TruffleObject {

    public static final Tuple EMPTY_TUPLE = new Tuple(new Object[]{});

    private final Object[] value;

    private Tuple(Object[] v) {
        this.value = v;
    }

    public static Tuple create() {
        return EMPTY_TUPLE;
    }

    public static Tuple create(Object[] v) {
        if (v == null || v.length == 0) {
            return EMPTY_TUPLE;
        }
        return new Tuple(v);
    }

    @Override
    @TruffleBoundary
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    @TruffleBoundary
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return equals((Tuple) obj);
    }

    public boolean equals(Tuple other) {
        if (value.length != other.value.length) {
            return false;
        }
        for (int i = 0; i < value.length; i++) {
            if (!JSRuntime.identical(value[i], other.value[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        if (value == null) return "";
        return Arrays.stream(value).map(String::valueOf).collect(Collectors.joining(","));
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @TruffleBoundary
    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return "#[" + toString() + "]";
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMetaObject() {
        return JSMetaType.JS_TUPLE;
    }

    @ExportMessage
    public long getArraySize() {
        return value.length;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public Object readArrayElement(long index) throws InvalidArrayIndexException {
        if (index < 0 || index >= value.length) {
            throw InvalidArrayIndexException.create(index);
        }
        return value[(int) index];
    }

    @ExportMessage
    public boolean isArrayElementReadable(long index) {
        return index >= 0 && index < value.length;
    }

    /**
     * @return true if the index isn't out of range.
     */
    public boolean hasElement(long index) {
        return index >= 0 && index < value.length;
    }

    /**
     * @return value at the given index.
     */
    public Object getElement(long index) {
        return value[(int) index];
    }

    /**
     * @return all values in a List.
     */
    public Object[] getElements() {
        return value.clone();
    }

    public int getArraySizeInt() {
        return value.length;
    }
}
