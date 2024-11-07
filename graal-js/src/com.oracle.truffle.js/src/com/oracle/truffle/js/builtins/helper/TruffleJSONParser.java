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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.js.parser.ParserException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

public class TruffleJSONParser {

    protected final JSContext context;
    protected int pos;
    protected int len;
    protected TruffleString parseStr;
    protected int parseDepth;

    protected static final char[] NullLiteral = new char[]{'n', 'u', 'l', 'l'};
    protected static final char[] BooleanTrueLiteral = new char[]{'t', 'r', 'u', 'e'};
    protected static final char[] BooleanFalseLiteral = new char[]{'f', 'a', 'l', 's', 'e'};
    protected static final int MAX_PARSE_DEPTH = 100000;

    private static final String MALFORMED_NUMBER = "malformed number";

    public TruffleJSONParser(JSContext context) {
        this.context = context;
    }

    public Object parse(TruffleString value, JSRealm realm) {
        this.pos = 0;
        this.parseDepth = 0;
        this.parseStr = value;
        this.len = Strings.length(parseStr);
        try {
            skipWhitespace();
            Object result = parseJSONValue(realm);
            skipWhitespace();
            if (posValid()) {
                error("JSON cannot be fully parsed");
            }
            return result;
        } catch (StackOverflowError ex) {
            throwStackError();
        } catch (JSException ex) {
            throw ex;
        } catch (IndexOutOfBoundsException ex) {
            throwSyntaxError(unexpectedEndOfInputMessage());
        } catch (Exception ex) {
            throwSyntaxError(null);
        } finally {
            parseStr = null;
        }
        return null;
    }

    private String unexpectedEndOfInputMessage() {
        return context.isOptionNashornCompatibilityMode() ? "Unexpected end of input" : "Unexpected end of JSON input";
    }

    protected Object parseJSONValue(JSRealm realm) {
        char c = get();

        if (isStringQuote(c)) {
            return parseJSONString();
        } else if (isObjectStart(c)) {
            return parseJSONObject(realm);
        } else if (isArrayStart(c)) {
            return parseJSONArray(realm);
        } else if (isNullLiteral(c)) {
            return parseNullLiteral();
        } else if (isBooleanLiteral(c)) {
            return parseBooleanLiteral();
        } else if (isNumber(c)) {
            return parseJSONNumber();
        }
        return unexpectedToken();
    }

    protected static boolean isNumber(char cur) {
        return cur == '-' || JSRuntime.isAsciiDigit(cur);
    }

    protected static boolean isObjectStart(char c) {
        return c == '{';
    }

    protected static boolean isArrayStart(char c) {
        return c == '[';
    }

    private Object parseJSONObject(JSRealm realm) {
        assert isObjectStart(get());
        incDepth();
        skipChar('{');
        skipWhitespace();
        JSObject object = JSOrdinary.create(context, realm);
        if (get() != '}') {
            parseJSONMemberList(object, realm);
            if (get() != '}') {
                if (get() == '"') {
                    unexpectedString();
                } else {
                    unexpectedToken();
                }
            }
        }
        skipChar('}');
        skipWhitespace();
        decDepth();
        return object;
    }

    private void parseJSONMemberList(JSObject object, JSRealm realm) {
        Member member = parseJSONMember(realm);
        JSRuntime.createDataProperty(object, member.getKey(), member.getValue());
        while (get() == ',') {
            skipChar(',');
            skipWhitespace();
            member = parseJSONMember(realm);
            JSRuntime.createDataProperty(object, member.getKey(), member.getValue());
        }
    }

    private Member parseJSONMember(JSRealm realm) {
        TruffleString jsonString = parseJSONString();
        expectChar(':');
        skipWhitespace();
        Object jsonValue = parseJSONValue(realm);
        return new Member(jsonString, jsonValue);
    }

    private Object parseJSONArray(JSRealm realm) {
        assert isArrayStart(get());
        incDepth();
        skipChar('[');
        skipWhitespace();
        JSArrayObject array = JSArray.createEmptyZeroLength(context, realm);
        if (get() != ']') {
            parseJSONElementList(array, realm);
            if (get() != ']') {
                error("closing quote ] expected");
            }
        }
        skipChar(']');
        skipWhitespace();
        decDepth();
        return array;
    }

    private void incDepth() {
        this.parseDepth++;
        if (this.parseDepth > MAX_PARSE_DEPTH) {
            throwStackError();
        }
    }

    protected static void throwStackError() {
        throw Errors.createRangeError("Cannot parse JSON constructs nested that deep");
    }

    protected static void throwSyntaxError(String msg) {
        throw Errors.createSyntaxError(msg == null ? "Cannot parse JSON" : msg);
    }

    protected void decDepth() {
        this.parseDepth--;
    }

    protected ScriptArray parseJSONElementList(JSArrayObject arrayObject, JSRealm realm) {
        int index = 0;
        ScriptArray scriptArray = JSAbstractArray.arrayGetArrayType(arrayObject);
        scriptArray = scriptArray.setElement(arrayObject, index, parseJSONValue(realm), false);
        while (get() == ',') {
            skipChar(',');
            skipWhitespace();
            index++;
            scriptArray = scriptArray.setElement(arrayObject, index, parseJSONValue(realm), false);
        }
        JSAbstractArray.arraySetArrayType(arrayObject, scriptArray);
        return scriptArray;
    }

    protected TruffleString parseJSONString() {
        if (!isStringQuote(get())) {
            if (isDigit(get())) {
                unexpectedNumber();
            } else {
                unexpectedToken();
            }
        }
        skipChar('"');
        TruffleString str = parseJSONStringCharacters();
        if (!isStringQuote(get())) {
            error("String quote expected");
        }
        skipChar('"');
        skipWhitespace();
        return str;
    }

    protected static boolean isStringQuote(char c) {
        return c == '"';
    }

    protected static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    protected TruffleString parseJSONStringCharacters() {
        int startPos = pos;
        boolean hasEscapes = false;
        int firstEscape = -1;
        char c = get();
        while (!isStringQuote(c)) {
            if (c < ' ') {
                error("invalid string");
            } else if (c == '\\') {
                if (!hasEscapes) {
                    firstEscape = pos;
                }
                hasEscapes = true;
                skipChar('\\');
            }
            skipChar();
            c = get();
        }
        int sLength = pos - startPos;
        TruffleString s = Strings.substring(context, parseStr, startPos, sLength);
        if (hasEscapes) {
            return unquoteJSON(s, sLength, firstEscape - startPos);
        } else {
            return s;
        }
    }

    protected TruffleString unquoteJSON(TruffleString string, int sLength, int posFirstBackslash) {
        assert sLength == Strings.length(string);
        assert posFirstBackslash >= 0; // guaranteed by caller
        int posBackslash = posFirstBackslash;

        int curPos = 0;
        var builder = Strings.builderCreate(sLength);
        while (posBackslash >= 0) {
            Strings.builderAppend(builder, string, curPos, posBackslash);
            curPos = posBackslash;
            char c = Strings.charAt(string, posBackslash + 1);
            switch (c) {
                case '"':
                    Strings.builderAppend(builder, '"');
                    break;
                case '\\':
                    Strings.builderAppend(builder, '\\');
                    break;
                case 'b':
                    Strings.builderAppend(builder, '\b');
                    break;
                case 'f':
                    Strings.builderAppend(builder, '\f');
                    break;
                case 'n':
                    Strings.builderAppend(builder, '\n');
                    break;
                case 'r':
                    Strings.builderAppend(builder, '\r');
                    break;
                case 't':
                    Strings.builderAppend(builder, '\t');
                    break;
                case '/':
                    Strings.builderAppend(builder, '/');
                    break;
                case 'u':
                    unquoteJSONUnicode(string, posBackslash, builder);
                    curPos += 4; // 6 chars for this escape, 2 are added below
                    break;
                default:
                    error("wrong escape sequence");
                    break;
            }
            curPos += 2; // valid for all escapes
            posBackslash = Strings.indexOf(string, '\\', curPos);
        }
        if (curPos < sLength) {
            Strings.builderAppend(builder, string, curPos, sLength);
        }
        return Strings.builderToString(builder);
    }

    protected int hexDigitValue(char c) {
        int value = JSRuntime.valueInHex(c);
        if (value < 0) {
            error("invalid string");
            return -1;
        }
        return value;
    }

    protected void unquoteJSONUnicode(TruffleString string, int posBackslash, TruffleStringBuilderUTF16 builder) {
        char c1 = Strings.charAt(string, posBackslash + 2);
        char c2 = Strings.charAt(string, posBackslash + 3);
        char c3 = Strings.charAt(string, posBackslash + 4);
        char c4 = Strings.charAt(string, posBackslash + 5);
        char unencodedC = (char) ((hexDigitValue(c1) << 12) | (hexDigitValue(c2) << 8) | (hexDigitValue(c3) << 4) | hexDigitValue(c4));
        Strings.builderAppend(builder, unencodedC);
    }

    protected Number parseJSONNumber() {
        int sign = 1;
        if (get() == '-') {
            skipChar();
            sign = -1;
        }
        if (!posValid()) {
            error(MALFORMED_NUMBER);
        }
        final int startPos = pos;
        int fractionPos = -1;
        boolean firstPosIsZero = false;
        char c = get();
        while (JSRuntime.isAsciiDigit(c) || c == '.') {
            if (c == '.') {
                if (fractionPos >= 0) {
                    error(MALFORMED_NUMBER);
                }
                fractionPos = pos;
            } else if (pos == startPos && c == '0') {
                firstPosIsZero = true;
            }
            skipChar();
            if (!posValid()) {
                break;
            }
            c = get(pos); // don't use cache here
        }
        if (pos == startPos) {
            unexpectedToken();
        } else if (firstPosIsZero) {
            // "0" should be parsable, but "08" not
            if ((startPos + 1) < len) {
                c = get(startPos + 1);
                if (c == 'x' || c == 'X' || JSRuntime.isAsciiDigit(c)) {
                    error("octal and hexadecimal not allowed");
                }
            }
        }
        if (fractionPos == startPos || fractionPos == (pos - 1)) {
            error(MALFORMED_NUMBER);
        }

        boolean hasExponent = false;
        if (posValid() && isExponentPart()) {
            hasExponent = true;
            skipExponent();
        }
        final int endPos = pos;
        skipWhitespace(); // after the number

        if (firstPosIsZero && (endPos - startPos == 1)) {
            if (sign == 1) {
                return 0;
            } else {
                return -0.0;
            }
        } else if (fractionPos == -1 && !hasExponent && (endPos - startPos <= JSRuntime.MAX_SAFE_INTEGER_DIGITS)) {
            // safe integer but not zero
            final int radix = 10;
            long safeInt = JSRuntime.parseSafeInteger(parseStr, startPos, endPos, radix);
            assert safeInt != 0;
            if (safeInt != JSRuntime.INVALID_SAFE_INTEGER) {
                safeInt *= sign;
                if (JSRuntime.longIsRepresentableAsInt(safeInt)) {
                    return (int) safeInt;
                } else {
                    return (double) safeInt;
                }
            }
        }
        TruffleString valueStr = Strings.lazySubstring(parseStr, startPos, endPos - startPos);
        return parseAsDouble(sign, valueStr);
    }

    protected Number parseAsDouble(int sign, TruffleString valueStr) {
        try {
            return Strings.parseDouble(valueStr) * sign;
        } catch (TruffleString.NumberFormatException e) {
            error(MALFORMED_NUMBER);
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    protected void skipExponent() {
        skipChar(); // e or E
        char cur = get();
        if (cur == '-') {
            skipChar('-');
        } else if (cur == '+') {
            skipChar('+');
        }
        if (!posValid()) {
            error(MALFORMED_NUMBER);
        }
        cur = get();
        int startPos = pos;
        while (JSRuntime.isAsciiDigit(cur)) {
            skipChar();
            if (!posValid()) {
                break;
            }
            cur = get(pos); // don't use cache here
        }
        if (pos == startPos) {
            error("Expected number but found ident");
        }
    }

    protected boolean isExponentPart() {
        return get() == 'e' || get() == 'E';
    }

    protected boolean isNullLiteral(char c) {
        return c == 'n' && isLiteral(NullLiteral, 1);
    }

    protected Object parseNullLiteral() {
        assert isNullLiteral(get());
        skipString(Strings.NULL);
        return Null.instance;
    }

    protected boolean isBooleanLiteral(char c) {
        return (c == 't' && isLiteral(BooleanTrueLiteral, 1)) || (c == 'f' && isLiteral(BooleanFalseLiteral, 1));
    }

    protected Object parseBooleanLiteral() {
        assert isBooleanLiteral(get());
        if (get() == 't') {
            skipString(Strings.TRUE);
            return true;
        } else if (get() == 'f') {
            skipString(Strings.FALSE);
            return false;
        }
        return error("cannot parse JSONBooleanLiteral");
    }

    protected static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    protected Object error(String message) {
        if (context.isOptionNashornCompatibilityMode()) {
            // use the Nashorn parser to get the proper error
            NashornJSONParser parser = new NashornJSONParser(parseStr, context);
            try {
                parser.parse(); // should throw
            } catch (ParserException ex) {
                String msg = ex.getMessage().replace("\r\n", "\n");
                throw Errors.createSyntaxError("Invalid JSON: " + msg);
            }
            throw Errors.shouldNotReachHere("JSON parser did not throw error as expected");
        } else {
            assert !message.contains("at position");
            throw Errors.createSyntaxError(message + " at position " + pos);
        }
    }

    private Object unexpectedToken() {
        error("Unexpected token " + get() + " in JSON");
        return null;
    }

    private Object unexpectedString() {
        error("Unexpected string in JSON");
        return null;
    }

    private Object unexpectedNumber() {
        error("Unexpected number in JSON");
        return null;
    }

    // ************************* Helper Functions ****************************************//

    protected char get() {
        return get(pos);
    }

    protected char get(int posParam) {
        return Strings.charAt(parseStr, posParam);
    }

    // needs to be checked by the caller already that the content matches!
    protected void skipString(TruffleString expected) {
        int length = Strings.length(expected);
        assert len >= pos + length;
        assert Strings.equals(Strings.lazySubstring(parseStr, pos, length), expected);
        pos += length;
        skipWhitespace();
    }

    protected void expectChar(char expected) {
        if (get(pos) != expected) {
            error(expected + " expected");
        }
        skipChar(expected);
    }

    protected void skipChar() {
        assert posValid();
        pos++;
    }

    // needs to be checked by the caller already that the content matches!
    protected void skipChar(char expected) {
        assert get(pos) == expected;
        skipChar();
    }

    protected void skipWhitespace() {
        while (posValid() && isWhitespace(get())) {
            pos++;
        }
    }

    protected boolean posValid() {
        return pos < len;
    }

    protected boolean isLiteral(char[] literal) {
        return isLiteral(literal, 0);
    }

    protected boolean isLiteral(char[] literal, int startPos) {
        if (len < pos + literal.length) {
            return false;
        }
        for (int i = startPos; i < literal.length; i++) {
            if (get(pos + i) != literal[i]) {
                return false;
            }
        }
        return true;
    }

    protected final class Member {
        private final TruffleString key;
        private final Object value;

        public Member(TruffleString key, Object value) {
            this.key = key;
            this.value = value;
        }

        public TruffleString getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
