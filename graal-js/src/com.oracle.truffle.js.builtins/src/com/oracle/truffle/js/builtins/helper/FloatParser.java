/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.js.runtime.JSRuntime;

public class FloatParser {

    private final String input;
    private int pos;
    private boolean isNaN;
    private final double value;

    private final BranchProfile exponentBranch;

    public FloatParser(String s, BranchProfile exponentBranch) {
        input = s;
        pos = 0;
        isNaN = false;
        this.exponentBranch = exponentBranch;
        value = parse();
    }

    public double getResult() {
        return value;
    }

    @TruffleBoundary
    private double parse() {
        strDecimalLiteral();
        if (isNaN) {
            return Double.NaN;
        }
        return parseValidSubstring();
    }

    @TruffleBoundary
    private double parseValidSubstring() {
        return Double.parseDouble(input.substring(0, pos));
    }

    private void strDecimalLiteral() {
        char currentChar = current();
        if (currentChar == '+' || currentChar == '-') {
            next();
            currentChar = current();
        }
        if (JSRuntime.isAsciiDigit(currentChar) || currentChar == '.') {
            strUnsignedDecimalLiteral();
        } else {
            isNaN = true;
        }
    }

    private void strUnsignedDecimalLiteral() {
        if (JSRuntime.isAsciiDigit(current())) {
            decimalDigits();
        }
        int prevPos = pos;
        if (hasNext() && current() == '.') {
            next();
            if (JSRuntime.isAsciiDigit(current())) {
                decimalDigits();
            }
        }
        if (isNaN) {
            pos = prevPos;
            isNaN = false;
            return;
        }
        prevPos = pos;
        if (isExponentPart()) {
            exponentPart();
        }
        if (isNaN) {
            pos = prevPos;
            isNaN = false;
            return;
        }
    }

    private void next() {
        pos++;
    }

    private char current() {
        if (hasNext()) {
            return input.charAt(pos);
        } else {
            return 0;
        }
    }

    private boolean hasNext() {
        return pos < input.length();
    }

    private void exponentPart() {
        exponentBranch.enter();
        assert current() == 'e' || current() == 'E';
        next();
        char currentChar = current();
        if (JSRuntime.isAsciiDigit(currentChar)) {
            decimalDigits();
        } else if (currentChar == '+' || currentChar == '-') {
            next();
            decimalDigits();
        } else {
            isNaN = true;
        }
    }

    private boolean isExponentPart() {
        if (hasNext()) {
            char firstChar = current();
            return firstChar == 'e' || firstChar == 'E';
        }
        return false;
    }

    private void decimalDigits() {
        char currentChar = current();
        boolean valid = false;
        while (JSRuntime.isAsciiDigit(currentChar) && hasNext()) {
            valid = true;
            next();
            currentChar = current();
        }
        if (!valid) {
            isNaN = true;
        }
    }
}
