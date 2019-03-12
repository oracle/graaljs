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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * @see JSSymbol
 */
@MessageResolution(receiverType = Symbol.class)
public final class Symbol implements TruffleObject {
    // Predefined symbols, as per ES6 6.1.5.1 Well-Known Symbols (in alphabetical order)
    /**
     * A method that determines if a constructor object recognizes an object as one of the
     * constructor's instances. Called by the semantics of the instanceof operator.
     */
    public static final Symbol SYMBOL_HAS_INSTANCE = Symbol.create("Symbol.hasInstance");
    /**
     * A Boolean valued property that if true indicates that an object should be flatten to its
     * array elements by Array.prototype.concat.
     */
    public static final Symbol SYMBOL_IS_CONCAT_SPREADABLE = Symbol.create("Symbol.isConcatSpreadable");
    /**
     * A method that returns the default iterator for an object. Called by the semantics of the
     * for-of statement.
     */
    public static final Symbol SYMBOL_ITERATOR = Symbol.create("Symbol.iterator");
    /**
     * A method that returns the default asynchronous iterator for an object. Called by the
     * semantics of the for-await-of statement.
     */
    public static final Symbol SYMBOL_ASYNC_ITERATOR = Symbol.create("Symbol.asyncIterator");
    /**
     * A regular expression method that matches the regular expression against a string. Called by
     * the String.prototype.match method.
     */
    public static final Symbol SYMBOL_MATCH = Symbol.create("Symbol.match");
    /**
     * A regular expression method that returns an iterator, that yields matches of the regular
     * expression against a string. Called by the String.prototype.matchAll method.
     */
    public static final Symbol SYMBOL_MATCH_ALL = Symbol.create("Symbol.matchAll");
    /**
     * A regular expression method that replaces matched substrings of a string. Called by the
     * String.prototype.replace method.
     */
    public static final Symbol SYMBOL_REPLACE = Symbol.create("Symbol.replace");
    /**
     * A regular expression method that returns the index within a string that matches the regular
     * expression. Called by the String.prototype.search method.
     */
    public static final Symbol SYMBOL_SEARCH = Symbol.create("Symbol.search");
    /**
     * A function valued property that is the constructor function that is used to create derived
     * objects.
     */
    public static final Symbol SYMBOL_SPECIES = Symbol.create("Symbol.species");
    /**
     * A regular expression method that splits a string at the indices that match the regular
     * expression. Called by the String.prototype.split method.
     */
    public static final Symbol SYMBOL_SPLIT = Symbol.create("Symbol.split");
    /**
     * A method that converts an object to a corresponding primitive value. Called by the
     * ToPrimitive abstract operation.
     */
    public static final Symbol SYMBOL_TO_PRIMITIVE = Symbol.create("Symbol.toPrimitive");
    /**
     * A property whose String value that is used in the creation of the default string description
     * of an object. Called by the built-in method Object.prototype.toString.
     */
    public static final Symbol SYMBOL_TO_STRING_TAG = Symbol.create("Symbol.toStringTag");
    /**
     * A property whose value is an Object whose own property names are property names that are
     * excluded from the with environment bindings of the associated object.
     */
    public static final Symbol SYMBOL_UNSCOPABLES = Symbol.create("Symbol.unscopables");

    /**
     * [[Description]] of Symbol if it is a String value, {@code null} otherwise ([[Description]] is
     * undefined).
     */
    private final String description;

    private Symbol(String description) {
        this.description = description;
    }

    public static Symbol create(String description) {
        return new Symbol(description);
    }

    public Object getDescription() {
        return (description == null) ? Undefined.instance : description;
    }

    public String getName() {
        return description == null ? "" : description;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "Symbol(" + getName() + ")";
    }

    @TruffleBoundary
    public String toFunctionNameString() {
        return (description == null) ? "" : '[' + description + ']';
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof Symbol;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return SymbolForeign.ACCESS;
    }
}
