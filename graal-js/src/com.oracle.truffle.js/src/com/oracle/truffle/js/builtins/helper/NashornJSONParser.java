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

import static com.oracle.js.parser.TokenType.STRING;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import com.oracle.js.parser.ECMAErrors;
import com.oracle.js.parser.JSErrorType;
import com.oracle.js.parser.JSType;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.Token;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Parses JSON text and returns the corresponding JS object representation.
 *
 * Derived from the ObjectLiteral production of the main parser.
 *
 * See: 15.12.1.2 The JSON Syntactic Grammar
 */
public class NashornJSONParser {

    private final TruffleString source;
    private final JSContext context;
    private final int length;
    private int pos = 0;

    private static final int EOF = -1;

    private static final TruffleString TRUE = Strings.constant("true");
    private static final TruffleString FALSE = Strings.constant("false");
    private static final TruffleString NULL = Strings.constant("null");

    private static final String MSG_INVALID_ESCAPE_CHAR = "invalid.escape.char";
    private static final String MSG_INVALID_HEX = "invalid.hex";
    private static final String MSG_JSON_INVALID_NUMBER = "json.invalid.number";
    private static final String MSG_LEXER_ERROR = "lexer.error.";
    private static final String MSG_MISSING_CLOSE_QUOTE = "missing.close.quote";
    private static final String MSG_PARSER_ERROR = "parser.error.";
    private static final String MSG_SYNTAX_ERROR_INVALID_JSON = "syntax.error.invalid.json";
    private static final String MSG_TRAILING_COMMA_IN_JSON = "trailing.comma.in.json";

    private static final String ERR_COLON = ":";
    private static final String ERR_COMMA_OR_RBRACE = ", or }";
    private static final String ERR_COMMA_OR_RBRACKET = ", or ]";
    private static final String ERR_EOF_STR = "eof";
    private static final String ERR_EXPECTED = "expected";
    private static final String ERR_IDENT = "ident";
    private static final String ERR_JSON_LITERAL = "json literal";
    private static final String ERR_STRING_CONTAINS_CONTROL_CHARACTER = "String contains control character";

    private static final int STATE_EMPTY = 0;
    private static final int STATE_ELEMENT_PARSED = 1;
    private static final int STATE_COMMA_PARSED = 2;

    /**
     * Constructor.
     *
     * @param source the source
     * @param context the global object
     */
    public NashornJSONParser(final TruffleString source, final JSContext context) {
        this.source = source;
        this.context = context;
        this.length = Strings.length(source);
    }

    /**
     * Public parse method. Parse a string into a JSON object.
     *
     * @return the parsed JSON Object
     */
    public Object parse() {
        final Object value = parseLiteral();
        skipWhiteSpace();
        if (pos < length) {
            throw expectedError(pos, ERR_EOF_STR, toString(peek()));
        }
        return value;
    }

    private Object parseLiteral() {
        skipWhiteSpace();

        final int c = peek();
        if (c == EOF) {
            throw expectedError(pos, ERR_JSON_LITERAL, ERR_EOF_STR);
        }
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 'f':
                return parseKeyword(FALSE, Boolean.FALSE);
            case 't':
                return parseKeyword(TRUE, Boolean.TRUE);
            case 'n':
                return parseKeyword(NULL, Null.instance);
            default:
                if (isDigit(c) || c == '-') {
                    return parseNumber();
                } else if (c == '.') {
                    throw numberError(pos);
                } else {
                    throw expectedError(pos, ERR_JSON_LITERAL, toString(c));
                }
        }
    }

    private Object parseObject() {
        JSDynamicObject jsobject = JSOrdinary.create(context, JSRealm.get(null));

        int state = STATE_EMPTY;

        assert peek() == '{';
        pos++;

        while (pos < length) {
            skipWhiteSpace();
            final int c = peek();

            switch (c) {
                case '"':
                    if (state == STATE_ELEMENT_PARSED) {
                        throw expectedError(pos, ERR_COMMA_OR_RBRACE, toString(c));
                    }
                    final Object id = parseString();
                    expectColon();
                    final Object value = parseLiteral();
                    addObjectProperty(jsobject, id, value);
                    state = STATE_ELEMENT_PARSED;
                    break;
                case ',':
                    if (state != STATE_ELEMENT_PARSED) {
                        throw trailingCommaError(pos, toString(c));
                    }
                    state = STATE_COMMA_PARSED;
                    pos++;
                    break;
                case '}':
                    if (state == STATE_COMMA_PARSED) {
                        throw trailingCommaError(pos, toString(c));
                    }
                    pos++;
                    return jsobject;
                default:
                    throw expectedError(pos, ERR_COMMA_OR_RBRACE, toString(c));
            }
        }
        throw expectedError(pos, ERR_COMMA_OR_RBRACE, ERR_EOF_STR);
    }

    private void addObjectProperty(final JSDynamicObject object, final Object idStr, final Object value) {
        JSObjectUtil.defineDataProperty(context, object, idStr, value, JSAttributes.getDefault());
    }

    private void expectColon() {
        skipWhiteSpace();
        final int n = next();
        if (n != ':') {
            throw expectedError(pos - 1, ERR_COLON, toString(n));
        }
    }

    private Object parseArray() {
        JSDynamicObject jsarray = JSArray.createEmptyZeroLength(context, JSRealm.get(null));
        ScriptArray arrayData = arrayGetArrayType(jsarray);

        int state = STATE_EMPTY;

        assert peek() == '[';
        pos++;

        while (pos < length) {
            skipWhiteSpace();
            final int c = peek();

            switch (c) {
                case ',':
                    if (state != STATE_ELEMENT_PARSED) {
                        throw trailingCommaError(pos, toString(c));
                    }
                    state = STATE_COMMA_PARSED;
                    pos++;
                    break;
                case ']':
                    if (state == STATE_COMMA_PARSED) {
                        throw trailingCommaError(pos, toString(c));
                    }
                    pos++;
                    return jsarray;
                default:
                    if (state == STATE_ELEMENT_PARSED) {
                        throw expectedError(pos, ERR_COMMA_OR_RBRACKET, toString(c));
                    }
                    final long index = arrayData.length(jsarray);
                    arrayData = arrayData.setElement(jsarray, index, parseLiteral(), true);
                    arraySetArrayType(jsarray, arrayData);
                    state = STATE_ELEMENT_PARSED;
                    break;
            }
        }

        throw expectedError(pos, ERR_COMMA_OR_RBRACKET, ERR_EOF_STR);
    }

    private TruffleString parseString() {
        // String buffer is only instantiated if string contains escape sequences.
        int start = ++pos;
        TruffleStringBuilderUTF16 sb = null;

        while (pos < length) {
            final int c = next();
            if (c <= 0x1f) {
                // Characters < 0x1f are not allowed in JSON strings.
                throw syntaxError(pos, ERR_STRING_CONTAINS_CONTROL_CHARACTER);

            } else if (c == '\\') {
                if (sb == null) {
                    sb = Strings.builderCreate(pos - start + 16);
                }
                Strings.builderAppend(sb, source, start, pos - 1);
                Strings.builderAppend(sb, parseEscapeSequence());
                start = pos;

            } else if (c == '"') {
                if (sb != null) {
                    Strings.builderAppend(sb, source, start, pos - 1);
                    return Strings.builderToString(sb);
                }
                return Strings.substring(context, source, start, pos - 1 - start);
            }
        }

        throw error(lexerMessage(MSG_MISSING_CLOSE_QUOTE), pos, length);
    }

    private char parseEscapeSequence() {
        final int c = next();
        switch (c) {
            case '"':
                return '"';
            case '\\':
                return '\\';
            case '/':
                return '/';
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                return parseUnicodeEscape();
            default:
                throw error(lexerMessage(MSG_INVALID_ESCAPE_CHAR), pos - 1, length);
        }
    }

    private char parseUnicodeEscape() {
        return (char) (parseHexDigit() << 12 | parseHexDigit() << 8 | parseHexDigit() << 4 | parseHexDigit());
    }

    private int parseHexDigit() {
        final int c = next();
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c + 10 - 'A';
        } else if (c >= 'a' && c <= 'f') {
            return c + 10 - 'a';
        }
        throw error(lexerMessage(MSG_INVALID_HEX), pos - 1, length);
    }

    private static boolean isDigit(final int c) {
        return c >= '0' && c <= '9';
    }

    private void skipDigits() {
        while (pos < length) {
            final int c = peek();
            if (!isDigit(c)) {
                break;
            }
            pos++;
        }
    }

    private Number parseNumber() {
        final int start = pos;
        int c = next();

        if (c == '-') {
            c = next();
        }
        if (!isDigit(c)) {
            throw numberError(start);
        }
        // no more digits allowed after 0
        if (c != '0') {
            skipDigits();
        }

        // fraction
        if (peek() == '.') {
            pos++;
            if (!isDigit(next())) {
                throw numberError(pos - 1);
            }
            skipDigits();
        }

        // exponent
        c = peek();
        if (c == 'e' || c == 'E') {
            pos++;
            c = next();
            if (c == '-' || c == '+') {
                c = next();
            }
            if (!isDigit(c)) {
                throw numberError(pos - 1);
            }
            skipDigits();
        }

        final double d;
        try {
            d = Strings.parseDouble(Strings.lazySubstring(source, start, pos - start));
        } catch (TruffleString.NumberFormatException e) {
            throw numberError(start);
        }
        if (JSType.isRepresentableAsInt(d)) {
            return (int) d;
        } else if (JSType.isRepresentableAsLong(d)) {
            return (long) d;
        }
        return d;
    }

    private Object parseKeyword(final TruffleString keyword, final Object value) {
        if (!Strings.regionEquals(source, pos, keyword, 0, Strings.length(keyword))) {
            throw expectedError(pos, ERR_JSON_LITERAL, ERR_IDENT);
        }
        pos += Strings.length(keyword);
        return value;
    }

    private int peek() {
        if (pos >= length) {
            return -1;
        }
        return Strings.charAt(source, pos);
    }

    private int next() {
        final int next = peek();
        pos++;
        return next;
    }

    private void skipWhiteSpace() {
        while (pos < length) {
            switch (peek()) {
                case '\t':
                case '\r':
                case '\n':
                case ' ':
                    pos++;
                    break;
                default:
                    return;
            }
        }
    }

    private static String toString(final int c) {
        return c == EOF ? ERR_EOF_STR : String.valueOf((char) c);
    }

    @SuppressWarnings("hiding")
    ParserException error(final String message, final int start, final int length) throws ParserException {
        final long token = Token.toDesc(STRING, start, length);
        final int pos = Token.descPosition(token);
        final Source src = Source.sourceFor("<json>", Strings.toJavaString(source));
        final int lineNum = src.getLine(pos);
        final int columnNum = src.getColumn(pos);
        return new ParserException(JSErrorType.SyntaxError, message, src, lineNum, columnNum, token);
    }

    private ParserException error(final String message, final int start) {
        return error(message, start, length);
    }

    private ParserException numberError(final int start) {
        return error(lexerMessage(MSG_JSON_INVALID_NUMBER), start);
    }

    private ParserException expectedError(final int start, final String expected, final String found) {
        return context.isOptionNashornCompatibilityMode()
                        ? error(parserMessage(ERR_EXPECTED, expected, found), start)
                        : expectedErrorV8(start, found);
    }

    private static ParserException expectedErrorV8(final int start, final String found) {
        char c = found.charAt(0);
        String entity;
        if (c == '"') {
            entity = "string";
        } else if (Character.isDigit(c)) {
            entity = "number";
        } else {
            entity = String.format("token %s", found);
        }
        String message = String.format("Unexpected %s in JSON at position %d", entity, start);
        return new ParserException(message);
    }

    private ParserException syntaxError(final int start, final String reason) {
        final String message = ECMAErrors.getMessage(MSG_SYNTAX_ERROR_INVALID_JSON, reason);
        return error(message, start);
    }

    private static String lexerMessage(final String msgId, String... args) {
        return ECMAErrors.getMessage(MSG_LEXER_ERROR + msgId, args);
    }

    private static String parserMessage(final String msgId, String... args) {
        return ECMAErrors.getMessage(MSG_PARSER_ERROR + msgId, args);
    }

    private ParserException trailingCommaError(int start, String found) {
        return context.isOptionNashornCompatibilityMode()
                        ? error(parserMessage(MSG_TRAILING_COMMA_IN_JSON), start)
                        : expectedErrorV8(start, found);
    }
}
