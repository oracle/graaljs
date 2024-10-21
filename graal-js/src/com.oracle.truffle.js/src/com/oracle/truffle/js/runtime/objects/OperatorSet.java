/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.js.runtime.Symbol;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;

/**
 * Instances of this class represent instances of the OperatorSet spec object. These hold the
 * definitions of any overloaded operators for a given type. Each such OperatorSet has a unique
 * numerical ID ({@link #getOperatorCounter()}), which are used for dispatching overloaded operators
 * between different classes (e.g. Matrix * Vector). An instance of this class is held in an
 * internal slot by the constructor of the class that overloads operators. This instance is then
 * also reachable from an internal slot of every instance of that class (we use a constant property
 * that is bound to the objects' Shape).
 */
public class OperatorSet {

    public static final EconomicSet<TruffleString> BINARY_OPERATORS;
    public static final EconomicSet<TruffleString> UNARY_OPERATORS;
    public static final EconomicSet<TruffleString> ALL_OPERATORS;

    private static final EconomicSet<TruffleString> STRING_OPEN_OPERATORS;

    static {
        List<TruffleString> binaryOperators = Arrays.asList(Strings.SYMBOL_MINUS, Strings.SYMBOL_STAR, Strings.SLASH, Strings.SYMBOL_PERCENT, Strings.SYMBOL_STAR_STAR, Strings.SYMBOL_AMPERSAND,
                        Strings.SYMBOL_CARET, Strings.SYMBOL_PIPE, Strings.ANGLE_BRACKET_OPEN_2, Strings.ANGLE_BRACKET_CLOSE_2, Strings.ANGLE_BRACKET_CLOSE_3, Strings.SYMBOL_EQUALS_EQUALS,
                        Strings.SYMBOL_PLUS, Strings.ANGLE_BRACKET_OPEN);
        BINARY_OPERATORS = EconomicSet.create(binaryOperators.size());
        BINARY_OPERATORS.addAll(binaryOperators);

        List<TruffleString> unaryOperators = Arrays.asList(Strings.POS, Strings.NEG, Strings.SYMBOL_PLUS_PLUS, Strings.SYMBOL_MINUS_MINUS, Strings.SYMBOL_TILDE);
        UNARY_OPERATORS = EconomicSet.create(unaryOperators.size());
        UNARY_OPERATORS.addAll(unaryOperators);

        ALL_OPERATORS = EconomicSet.create(BINARY_OPERATORS.size() + UNARY_OPERATORS.size());
        ALL_OPERATORS.addAll(BINARY_OPERATORS);
        ALL_OPERATORS.addAll(UNARY_OPERATORS);

        STRING_OPEN_OPERATORS = EconomicSet.create(3);
        STRING_OPEN_OPERATORS.addAll(Arrays.asList(Strings.SYMBOL_PLUS, Strings.SYMBOL_EQUALS_EQUALS, Strings.ANGLE_BRACKET_OPEN));
    }

    public static final OperatorSet NUMBER_OPERATOR_SET = new OperatorSet(0, BINARY_OPERATORS);
    public static final OperatorSet BIGINT_OPERATOR_SET = new OperatorSet(1, BINARY_OPERATORS);
    public static final OperatorSet STRING_OPERATOR_SET = new OperatorSet(2, STRING_OPEN_OPERATORS);

    private final int operatorCounter;
    private final EconomicMap<TruffleString, Object> selfOperatorDefinitions;
    private final EconomicMap<TruffleString, Object[]> leftOperatorDefinitions;
    private final EconomicMap<TruffleString, Object[]> rightOperatorDefinitions;
    private final EconomicSet<TruffleString> openOperators;

    public OperatorSet(int operatorCounter, EconomicSet<TruffleString> openOperators) {
        this(operatorCounter, null, null, null, openOperators);
    }

    public OperatorSet(int operatorCounter, EconomicMap<TruffleString, Object> selfOperatorDefinitions, EconomicMap<TruffleString, Object[]> leftOperatorDefinitions,
                    EconomicMap<TruffleString, Object[]> rightOperatorDefinitions, EconomicSet<TruffleString> openOperators) {
        this.operatorCounter = operatorCounter;
        this.selfOperatorDefinitions = selfOperatorDefinitions;
        this.leftOperatorDefinitions = leftOperatorDefinitions;
        this.rightOperatorDefinitions = rightOperatorDefinitions;
        this.openOperators = openOperators;
    }

    @TruffleBoundary
    public boolean isOperatorOpen(TruffleString operator) {
        return openOperators.contains(operator);
    }

    public int getOperatorCounter() {
        return operatorCounter;
    }

    public static OperatorSet getOperatorSet(Object object) {
        if (JSRuntime.isNumber(object)) {
            return OperatorSet.NUMBER_OPERATOR_SET;
        } else if (JSRuntime.isBigInt(object)) {
            return OperatorSet.BIGINT_OPERATOR_SET;
        } else if (Strings.isTString(object)) {
            return OperatorSet.STRING_OPERATOR_SET;
        } else {
            assert object instanceof JSOverloadedOperatorsObject;
            return ((JSOverloadedOperatorsObject) object).getOperatorSet();
        }
    }

    @TruffleBoundary
    public static Object getOperatorImplementation(JSOverloadedOperatorsObject operand, TruffleString operatorName) {
        return operand.getOperatorSet().selfOperatorDefinitions.get(operatorName);
    }

    @TruffleBoundary
    public static Object getOperatorImplementation(Object left, Object right, TruffleString operatorName) {
        if (isUnsupportedPrimitive(left) || isUnsupportedPrimitive(right)) {
            return null;
        }
        OperatorSet leftOperatorSet = getOperatorSet(left);
        OperatorSet rightOperatorSet = getOperatorSet(right);
        if (leftOperatorSet == rightOperatorSet) {
            return leftOperatorSet.selfOperatorDefinitions.get(operatorName);
        } else if (leftOperatorSet.operatorCounter < rightOperatorSet.operatorCounter) {
            Object[] rightOperatorDefinitions = rightOperatorSet.rightOperatorDefinitions.get(operatorName);
            if (rightOperatorDefinitions != null) {
                return rightOperatorDefinitions[leftOperatorSet.operatorCounter];
            } else {
                return null;
            }
        } else {
            assert leftOperatorSet.operatorCounter > rightOperatorSet.operatorCounter;
            Object[] leftOperatorDefinitions = leftOperatorSet.leftOperatorDefinitions.get(operatorName);
            if (leftOperatorDefinitions != null) {
                return leftOperatorDefinitions[rightOperatorSet.operatorCounter];
            } else {
                return null;
            }
        }
    }

    public static boolean isUnsupportedPrimitive(Object value) {
        return JSRuntime.isNullOrUndefined(value) || value instanceof Boolean || value instanceof Symbol;
    }
}
