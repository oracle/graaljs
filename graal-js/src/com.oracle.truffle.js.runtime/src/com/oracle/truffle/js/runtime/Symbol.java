/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;

/**
 * @see JSSymbol
 */
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

    private final String name;

    private Symbol(String name) {
        this.name = name;
    }

    public static Symbol create(String name) {
        return new Symbol(name);
    }

    public String getName() {
        return name;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "Symbol(" + name + ")";
    }

    @TruffleBoundary
    public String toFunctionNameString() {
        return name.isEmpty() ? "" : '[' + name + ']';
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return SymbolMessageResolutionForeign.ACCESS;
    }
}
