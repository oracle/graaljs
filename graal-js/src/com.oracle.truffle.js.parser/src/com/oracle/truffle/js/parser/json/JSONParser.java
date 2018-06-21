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

package com.oracle.truffle.js.parser.json;

import static com.oracle.js.parser.TokenType.STRING;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import com.oracle.js.parser.ECMAErrors;
import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.JSErrorType;
import com.oracle.js.parser.JSType;
import com.oracle.js.parser.Lexer;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.Token;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Parses JSON text and returns the corresponding JS object representation.
 *
 * Derived from the ObjectLiteral production of the main parser.
 *
 * See: 15.12.1.2 The JSON Syntactic Grammar
 */
public class JSONParser {

    private final String source;
    private final JSContext context;
    private final int length;
    private int pos = 0;

    private static final int EOF = -1;

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";

    private static final int STATE_EMPTY = 0;
    private static final int STATE_ELEMENT_PARSED = 1;
    private static final int STATE_COMMA_PARSED = 2;

    /**
     * Constructor.
     *
     * @param source the source
     * @param context the global object
     */
    public JSONParser(final String source, final JSContext context) {
        this.source = source;
        this.context = context;
        this.length = source.length();
    }

    /**
     * Implementation of the Quote(value) operation as defined in the ECMAscript spec. It wraps a
     * String value in double quotes and escapes characters within.
     *
     * @param value string to quote
     *
     * @return quoted and escaped string
     */
    public static String quote(final String value) {
        final StringBuilder product = new StringBuilder();
        product.append("\"");
        for (final char ch : value.toCharArray()) {
            if (ch < ' ') {
                if (ch == '\b') {
                    product.append("\\b");
                } else if (ch == '\f') {
                    product.append("\\f");
                } else if (ch == '\n') {
                    product.append("\\n");
                } else if (ch == '\r') {
                    product.append("\\r");
                } else if (ch == '\t') {
                    product.append("\\t");
                } else {
                    product.append(Lexer.unicodeEscape(ch));
                }
            } else {
                if (ch == '\\') {
                    product.append("\\\\");
                } else if (ch == '"') {
                    product.append("\\\"");
                } else {
                    product.append(ch);
                }
            }
        }
        product.append("\"");
        return product.toString();
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
            throw expectedError(pos, "eof", toString(peek()));
        }
        return value;
    }

    private Object parseLiteral() {
        skipWhiteSpace();

        final int c = peek();
        if (c == EOF) {
            throw expectedError(pos, "json literal", "eof");
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
                    throw expectedError(pos, "json literal", toString(c));
                }
        }
    }

    private Object parseObject() {
        DynamicObject jsobject = JSUserObject.create(context);

        int state = STATE_EMPTY;

        assert peek() == '{';
        pos++;

        while (pos < length) {
            skipWhiteSpace();
            final int c = peek();

            switch (c) {
                case '"':
                    if (state == STATE_ELEMENT_PARSED) {
                        throw expectedError(pos, ", or }", toString(c));
                    }
                    final String id = parseString();
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
                    throw expectedError(pos, ", or }", toString(c));
            }
        }
        throw expectedError(pos, ", or }", "eof");
    }

    private void addObjectProperty(final DynamicObject object, final String id, final Object value) {
        JSObjectUtil.defineDataProperty(context, object, id, value, JSAttributes.getDefault());
    }

    private void expectColon() {
        skipWhiteSpace();
        final int n = next();
        if (n != ':') {
            throw expectedError(pos - 1, ":", toString(n));
        }
    }

    private Object parseArray() {
        DynamicObject jsarray = JSArray.createEmptyZeroLength(context);
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
                        throw expectedError(pos, ", or ]", toString(c));
                    }
                    final long index = arrayData.length(jsarray);
                    arrayData = arrayData.setElement(jsarray, index, parseLiteral(), true);
                    arraySetArrayType(jsarray, arrayData);
                    state = STATE_ELEMENT_PARSED;
                    break;
            }
        }

        throw expectedError(pos, ", or ]", "eof");
    }

    private String parseString() {
        // String buffer is only instantiated if string contains escape sequences.
        int start = ++pos;
        StringBuilder sb = null;

        while (pos < length) {
            final int c = next();
            if (c <= 0x1f) {
                // Characters < 0x1f are not allowed in JSON strings.
                throw syntaxError(pos, "String contains control character");

            } else if (c == '\\') {
                if (sb == null) {
                    sb = new StringBuilder(pos - start + 16);
                }
                sb.append(source, start, pos - 1);
                sb.append(parseEscapeSequence());
                start = pos;

            } else if (c == '"') {
                if (sb != null) {
                    sb.append(source, start, pos - 1);
                    return sb.toString();
                }
                return source.substring(start, pos - 1);
            }
        }

        throw error(lexerMessage("missing.close.quote"), pos, length);
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
                throw error(lexerMessage("invalid.escape.char"), pos - 1, length);
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
        throw error(lexerMessage("invalid.hex"), pos - 1, length);
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

        final double d = Double.parseDouble(source.substring(start, pos));
        if (JSType.isRepresentableAsInt(d)) {
            return (int) d;
        } else if (JSType.isRepresentableAsLong(d)) {
            return (long) d;
        }
        return d;
    }

    private Object parseKeyword(final String keyword, final Object value) {
        if (!source.regionMatches(pos, keyword, 0, keyword.length())) {
            throw expectedError(pos, "json literal", "ident");
        }
        pos += keyword.length();
        return value;
    }

    private int peek() {
        if (pos >= length) {
            return -1;
        }
        return source.charAt(pos);
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
        return c == EOF ? "eof" : String.valueOf((char) c);
    }

    @SuppressWarnings("hiding")
    ParserException error(final String message, final int start, final int length) throws ParserException {
        final long token = Token.toDesc(STRING, start, length);
        final int pos = Token.descPosition(token);
        final Source src = Source.sourceFor("<json>", source);
        final int lineNum = src.getLine(pos);
        final int columnNum = src.getColumn(pos);
        final String formatted = ErrorManager.format(message, src, lineNum, columnNum, token);
        return new ParserException(JSErrorType.SyntaxError, formatted, src, lineNum, columnNum, token);
    }

    private ParserException error(final String message, final int start) {
        return error(message, start, length);
    }

    private ParserException numberError(final int start) {
        return error(lexerMessage("json.invalid.number"), start);
    }

    private ParserException expectedError(final int start, final String expected, final String found) {
        return context.isOptionV8CompatibilityMode()
                        ? expectedErrorV8(start, found)
                        : error(parserMessage("expected", expected, found), start);
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
        final String message = ECMAErrors.getMessage("syntax.error.invalid.json", reason);
        return error(message, start);
    }

    private static String lexerMessage(final String msgId, String... args) {
        return ECMAErrors.getMessage("lexer.error." + msgId, args);
    }

    private static String parserMessage(final String msgId, String... args) {
        return ECMAErrors.getMessage("parser.error." + msgId, args);
    }

    private ParserException trailingCommaError(int start, String found) {
        return JSTruffleOptions.NashornCompatibilityMode
                        ? error(parserMessage("trailing.comma.in.json"), start)
                        : expectedErrorV8(start, found);
    }
}
