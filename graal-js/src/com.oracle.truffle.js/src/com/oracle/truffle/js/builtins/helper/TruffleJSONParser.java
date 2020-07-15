/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.Null;

public class TruffleJSONParser {

    protected final JSContext context;
    protected int pos;
    protected int len;
    protected String parseStr;
    protected int parseDepth;

    protected static final char[] NullLiteral = new char[]{'n', 'u', 'l', 'l'};
    protected static final char[] BooleanTrueLiteral = new char[]{'t', 'r', 'u', 'e'};
    protected static final char[] BooleanFalseLiteral = new char[]{'f', 'a', 'l', 's', 'e'};
    protected static final int MAX_PARSE_DEPTH = 100000;

    private static final String MALFORMED_NUMBER = "malformed number";

    public TruffleJSONParser(JSContext context) {
        this.context = context;
    }

    public Object parse(String value) {
        this.pos = 0;
        this.parseDepth = 0;
        this.parseStr = value;
        this.len = parseStr.length();
        try {
            skipWhitespace();
            Object result = parseJSONText();
            skipWhitespace();
            if (posValid()) {
                throw Errors.createSyntaxError("JSON cannot be fully parsed");
            }
            return result;
        } catch (StackOverflowError ex) {
            throwStackError();
        } catch (JSException ex) {
            throw ex;
        } catch (StringIndexOutOfBoundsException ex) {
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

    private Object parseJSONText() {
        return parseJSONValue();
    }

    protected Object parseJSONValue() {
        char c = get();
        if (c == 'n' && isNullLiteral()) {
            return parseNullLiteral();
        } else if ((c == 't' || c == 'f') && isBooleanLiteral()) {
            return parseBooleanLiteral();
        } else if (isNumber(c)) {
            return parseJSONNumber();
        } else if (isString(c)) {
            return parseJSONString();
        } else if (isArray(c)) {
            return parseJSONArray();
        } else if (isObject(c)) {
            return parseJSONObject();
        }
        return unexpectedToken();
    }

    protected static boolean isNumber(char cur) {
        return cur == '-' || JSRuntime.isAsciiDigit(cur);
    }

    protected static boolean isString(char c) {
        return isStringQuote(c);
    }

    protected static boolean isObject(char c) {
        return c == '{';
    }

    protected static boolean isArray(char c) {
        return c == '[';
    }

    private Object parseJSONObject() {
        assert isObject(get());
        incDepth();
        read(); // parseJSONValue ensures this char is a "{"
        DynamicObject object = JSUserObject.create(context);
        if (get() != '}') {
            parseJSONMemberList(object);
            if (get() != '}') {
                if (get() == '"') {
                    unexpectedString();
                } else {
                    unexpectedToken();
                }
            }
        }
        read('}');
        decDepth();
        return object;
    }

    private void parseJSONMemberList(DynamicObject object) {
        Member member = parseJSONMember();
        JSRuntime.createDataProperty(object, member.getKey(), member.getValue());
        while (get() == ',') {
            read();
            member = parseJSONMember();
            JSRuntime.createDataProperty(object, member.getKey(), member.getValue());
        }
    }

    private Member parseJSONMember() {
        String jsonString = parseJSONString();
        read(':');
        Object jsonValue = parseJSONValue();
        return new Member(jsonString, jsonValue);
    }

    private Object parseJSONArray() {
        assert isArray(get());
        incDepth();
        read(); // parseJSONValue ensures this is a "["
        DynamicObject array = JSArray.createEmptyZeroLength(context);
        if (get() != ']') {
            parseJSONElementList(array);
            if (get() != ']') {
                error("closing quote ] expected");
            }
        }
        read(']');
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

    protected ScriptArray parseJSONElementList(DynamicObject arrayObject) {
        int index = 0;
        ScriptArray scriptArray = JSAbstractArray.arrayGetArrayType(arrayObject);
        scriptArray = scriptArray.setElement(arrayObject, index, parseJSONValue(), false);
        while (get() == ',') {
            read();
            index++;
            scriptArray = scriptArray.setElement(arrayObject, index, parseJSONValue(), false);
        }
        JSAbstractArray.arraySetArrayType(arrayObject, scriptArray);
        return scriptArray;
    }

    protected String parseJSONString() {
        if (!isStringQuote(get())) {
            if (isDigit(get())) {
                unexpectedNumber();
            } else {
                unexpectedToken();
            }
        }
        pos++; // don't skip whitespace here
        String str = parseJSONStringCharacters();
        if (!isStringQuote(get())) {
            error("String quote expected");
        }
        read();
        return str;
    }

    protected static boolean isStringQuote(char c) {
        return c == '"';
    }

    protected static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    protected String parseJSONStringCharacters() {
        int startPos = pos;
        boolean hasEscapes = false;
        boolean curIsEscaped = false;
        char c = get();
        while (c != '\"' || curIsEscaped) {
            if (c < ' ') {
                error("invalid string");
            } else if (c == '\\') {
                hasEscapes = true;
                curIsEscaped = !curIsEscaped;
            } else {
                curIsEscaped = false;
            }
            pos++; // don't skip whitespace here
            c = get();
        }
        String s = parseStr.substring(startPos, pos);
        if (hasEscapes) {
            return unquoteJSON(s);
        } else {
            return s;
        }
    }

    protected String unquoteJSON(String string) {
        int posBackslash = string.indexOf('\\');
        if (posBackslash >= 0) {
            int curPos = 0;
            StringBuilder builder = new StringBuilder(string.length());
            while (posBackslash >= 0) {
                builder.append(string, curPos, posBackslash);
                curPos = posBackslash;
                char c = string.charAt(posBackslash + 1);
                switch (c) {
                    case '"':
                        builder.append('"');
                        break;
                    case '\\':
                        builder.append('\\');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case '/':
                        builder.append('/');
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
                posBackslash = string.indexOf('\\', curPos);
            }
            if (curPos < string.length()) {
                builder.append(string, curPos, string.length());
            }
            return builder.toString();
        } else {
            return string;
        }
    }

    protected int hexDigitValue(char c) {
        int value = JSRuntime.valueInHex(c);
        if (value < 0) {
            error("invalid string");
            return -1;
        }
        return value;
    }

    protected void unquoteJSONUnicode(String string, int posBackslash, StringBuilder builder) {
        char c1 = string.charAt(posBackslash + 2);
        char c2 = string.charAt(posBackslash + 3);
        char c3 = string.charAt(posBackslash + 4);
        char c4 = string.charAt(posBackslash + 5);
        char unencodedC = (char) ((hexDigitValue(c1) << 12) | (hexDigitValue(c2) << 8) | (hexDigitValue(c3) << 4) | hexDigitValue(c4));
        builder.append(unencodedC);
    }

    protected Number parseJSONNumber() {
        int sign = 1;
        if (get() == '-') {
            read();
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
            pos++; // don't skip whitespace
            if (!posValid()) {
                break;
            }
            c = get(pos); // don't use cache here
        }
        if (pos == startPos) {
            error("Expected number but found ident");
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
            pos++; // reads the "E" without skipping whitespace
            readDigits();
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
        String valueStr = parseStr.substring(startPos, endPos);
        return parseAsDouble(sign, valueStr);
    }

    protected static Number parseAsDouble(int sign, String valueStr) {
        return Double.parseDouble(valueStr) * sign;
    }

    protected int readDigits() {
        int sign = 1;
        char cur = get();
        if (cur == '-') {
            read();
            sign = -1;
        } else if (cur == '+') {
            read();
            sign = 1;
        }
        if (!posValid()) {
            error(MALFORMED_NUMBER);
        }
        cur = get();
        int startPos = pos;
        while (JSRuntime.isAsciiDigit(cur)) {
            pos++; // don't skip whitespace
            if (!posValid()) {
                break;
            }
            cur = get(pos); // don't use cache here
        }
        if (pos == startPos) {
            error("Expected number but found ident");
        }
        return sign * Integer.parseInt(parseStr.substring(startPos, pos));
    }

    protected boolean isExponentPart() {
        return get() == 'e' || get() == 'E';
    }

    protected boolean isNullLiteral() {
        return isLiteral(NullLiteral);
    }

    protected Object parseNullLiteral() {
        assert isNullLiteral();
        read("null");
        return Null.instance;
    }

    protected boolean isBooleanLiteral() {
        return isLiteral(BooleanTrueLiteral) || isLiteral(BooleanFalseLiteral);
    }

    protected Object parseBooleanLiteral() {
        assert isBooleanLiteral();
        if (get() == 't') {
            read("true");
            return true;
        } else if (get() == 'f') {
            read("false");
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
            throw Errors.createSyntaxError(message);
        }
    }

    private Object unexpectedToken() {
        error("Unexpected token " + get() + " in JSON at position " + pos);
        return null;
    }

    private Object unexpectedString() {
        error("Unexpected string in JSON at position " + pos);
        return null;
    }

    private Object unexpectedNumber() {
        error("Unexpected number in JSON at position " + pos);
        return null;
    }

    // ************************* Helper Functions ****************************************//

    protected char get() {
        return get(pos);
    }

    protected char get(int posParam) {
        return parseStr.charAt(posParam);
    }

    protected void read() {
        assert len > pos;
        pos++;
        skipWhitespace();
    }

    protected void read(String expected) {
        assert len >= pos + expected.length();
        assert parseStr.substring(pos, pos + expected.length()).equals(expected);
        pos += expected.length();
        skipWhitespace();
    }

    protected void read(char expected) {
        if (get(pos) != expected) {
            error(expected + " expected");
        }
        pos++;
        skipWhitespace();
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
        if (len < pos + literal.length) {
            return false;
        }
        // fastpath for the cached current character
        if (get() != literal[0]) {
            return false;
        }
        for (int i = 1; i < literal.length; i++) {
            if (get(pos + i) != literal[i]) {
                return false;
            }
        }
        return true;
    }

    protected final class Member {
        private final String key;
        private final Object value;

        public Member(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
