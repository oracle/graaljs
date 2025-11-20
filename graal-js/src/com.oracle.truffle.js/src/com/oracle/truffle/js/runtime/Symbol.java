/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.interop.JSMetaType;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.ConcurrentWeakIdentityHashMap;
import com.oracle.truffle.js.runtime.util.WeakMap;

/**
 * @see JSSymbol
 */
@ExportLibrary(InteropLibrary.class)
public final class Symbol implements TruffleObject {
    // Predefined symbols, as per ES6 6.1.5.1 Well-Known Symbols (in alphabetical order)
    /**
     * A method that determines if a constructor object recognizes an object as one of the
     * constructor's instances. Called by the semantics of the instanceof operator.
     */
    public static final Symbol SYMBOL_HAS_INSTANCE = Symbol.createWellKnown(Strings.constant("Symbol.hasInstance"));
    /**
     * A Boolean valued property that if true indicates that an object should be flatten to its
     * array elements by Array.prototype.concat.
     */
    public static final Symbol SYMBOL_IS_CONCAT_SPREADABLE = Symbol.createWellKnown(Strings.constant("Symbol.isConcatSpreadable"));
    /**
     * A method that returns the default iterator for an object. Called by the semantics of the
     * for-of statement.
     */
    public static final Symbol SYMBOL_ITERATOR = Symbol.createWellKnown(Strings.constant("Symbol.iterator"));
    /**
     * A method that returns the default asynchronous iterator for an object. Called by the
     * semantics of the for-await-of statement.
     */
    public static final Symbol SYMBOL_ASYNC_ITERATOR = Symbol.createWellKnown(Strings.constant("Symbol.asyncIterator"));
    /**
     * A regular expression method that matches the regular expression against a string. Called by
     * the String.prototype.match method.
     */
    public static final Symbol SYMBOL_MATCH = Symbol.createWellKnown(Strings.constant("Symbol.match"));
    /**
     * A regular expression method that returns an iterator, that yields matches of the regular
     * expression against a string. Called by the String.prototype.matchAll method.
     */
    public static final Symbol SYMBOL_MATCH_ALL = Symbol.createWellKnown(Strings.constant("Symbol.matchAll"));
    /**
     * A regular expression method that replaces matched substrings of a string. Called by the
     * String.prototype.replace method.
     */
    public static final Symbol SYMBOL_REPLACE = Symbol.createWellKnown(Strings.constant("Symbol.replace"));
    /**
     * A regular expression method that returns the index within a string that matches the regular
     * expression. Called by the String.prototype.search method.
     */
    public static final Symbol SYMBOL_SEARCH = Symbol.createWellKnown(Strings.constant("Symbol.search"));
    /**
     * A function valued property that is the constructor function that is used to create derived
     * objects.
     */
    public static final Symbol SYMBOL_SPECIES = Symbol.createWellKnown(Strings.constant("Symbol.species"));
    /**
     * A regular expression method that splits a string at the indices that match the regular
     * expression. Called by the String.prototype.split method.
     */
    public static final Symbol SYMBOL_SPLIT = Symbol.createWellKnown(Strings.constant("Symbol.split"));
    /**
     * A method that converts an object to a corresponding primitive value. Called by the
     * ToPrimitive abstract operation.
     */
    public static final Symbol SYMBOL_TO_PRIMITIVE = Symbol.createWellKnown(Strings.constant("Symbol.toPrimitive"));
    /**
     * A property whose String value that is used in the creation of the default string description
     * of an object. Called by the built-in method Object.prototype.toString.
     */
    public static final Symbol SYMBOL_TO_STRING_TAG = Symbol.createWellKnown(Strings.constant("Symbol.toStringTag"));
    /**
     * A property whose value is an Object whose own property names are property names that are
     * excluded from the with environment bindings of the associated object.
     */
    public static final Symbol SYMBOL_UNSCOPABLES = Symbol.createWellKnown(Strings.constant("Symbol.unscopables"));
    /**
     * A method that performs explicit resource cleanup on an object. Called by the semantics of the
     * using declaration and DisposableStack objects.
     */
    public static final Symbol SYMBOL_DISPOSE = Symbol.createWellKnown(Strings.constant("Symbol.dispose"));
    /**
     * A method that performs explicit resource cleanup on an object. Called by the semantics of the
     * await using declaration and AsyncDisposableStack objects.
     */
    public static final Symbol SYMBOL_ASYNC_DISPOSE = Symbol.createWellKnown(Strings.constant("Symbol.asyncDispose"));

    /**
     * [[Description]] of Symbol if it is a String value, {@code null} otherwise ([[Description]] is
     * undefined).
     */
    private final TruffleString description;

    /**
     * If true, the symbol is in the GlobalSymbolRegistry (i.e. created by {@code Symbol.for}).
     */
    private final boolean registered;

    /**
     * Determines whether the symbol is private (can be true in V8 compatibility mode only).
     */
    private final boolean isPrivate;

    private Symbol(TruffleString description, boolean registered, boolean isPrivate) {
        this.description = description;
        this.registered = registered;
        this.isPrivate = isPrivate;
    }

    public static Symbol create(TruffleString description) {
        Symbol symbol = new Symbol(description, false, false);
        JavaScriptLanguage.getCurrentLanguage().getJSContext().unregisteredSymbolCreated(symbol);
        return symbol;
    }

    private static Symbol createWellKnown(TruffleString description) {
        Symbol symbol = new Symbol(description, false, false);
        symbol.setInvertedMap(ConcurrentWeakIdentityHashMap.create());
        return symbol;
    }

    public static Symbol createRegistered(TruffleString description) {
        return new Symbol(Objects.requireNonNull(description), true, false);
    }

    public static Symbol createPrivate(TruffleString description) {
        return new Symbol(description, false, true);
    }

    public static Symbol createPrivateRegistered(TruffleString description) {
        return new Symbol(Objects.requireNonNull(description), true, true);
    }

    public Object getDescription() {
        return (description == null) ? Undefined.instance : description;
    }

    public TruffleString getName() {
        return description == null ? Strings.EMPTY_STRING : description;
    }

    public boolean isRegistered() {
        return registered;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return Strings.toJavaString(toTString());
    }

    public TruffleString toTString() {
        return Strings.concatAll(Strings.SYMBOL_PAREN_OPEN, getName(), Strings.PAREN_CLOSE);
    }

    @TruffleBoundary
    public TruffleString toFunctionNameString() {
        return (description == null) ? Strings.EMPTY_STRING : Strings.concatAll(Strings.BRACKET_OPEN, description, Strings.BRACKET_CLOSE);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
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

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMetaObject() {
        return JSMetaType.JS_SYMBOL;
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doHostObject(Symbol receiver, Symbol other) {
            return TriState.valueOf(receiver == other);
        }

        @SuppressWarnings("unused")
        @Fallback
        static TriState doOther(Symbol receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @TruffleBoundary
    @ExportMessage
    int identityHashCode() {
        return super.hashCode();
    }

    private Map<WeakMap, Object> invertedMap;

    public Map<WeakMap, Object> getInvertedMap() {
        return invertedMap;
    }

    public void setInvertedMap(Map<WeakMap, Object> invMap) {
        assert this.invertedMap == null;
        this.invertedMap = invMap;
    }

    void clearInvertedMap() {
        this.invertedMap = null;
    }

}
