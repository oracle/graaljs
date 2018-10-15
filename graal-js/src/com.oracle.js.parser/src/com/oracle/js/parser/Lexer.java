/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser;

import static com.oracle.js.parser.TokenType.ADD;
import static com.oracle.js.parser.TokenType.BIGINT;
import static com.oracle.js.parser.TokenType.BINARY_NUMBER;
import static com.oracle.js.parser.TokenType.COMMENT;
import static com.oracle.js.parser.TokenType.DECIMAL;
import static com.oracle.js.parser.TokenType.DIRECTIVE_COMMENT;
import static com.oracle.js.parser.TokenType.EOF;
import static com.oracle.js.parser.TokenType.EOL;
import static com.oracle.js.parser.TokenType.ERROR;
import static com.oracle.js.parser.TokenType.ESCSTRING;
import static com.oracle.js.parser.TokenType.EXECSTRING;
import static com.oracle.js.parser.TokenType.FLOATING;
import static com.oracle.js.parser.TokenType.FUNCTION;
import static com.oracle.js.parser.TokenType.HEXADECIMAL;
import static com.oracle.js.parser.TokenType.LBRACE;
import static com.oracle.js.parser.TokenType.LPAREN;
import static com.oracle.js.parser.TokenType.NON_OCTAL_DECIMAL;
import static com.oracle.js.parser.TokenType.OCTAL;
import static com.oracle.js.parser.TokenType.OCTAL_LEGACY;
import static com.oracle.js.parser.TokenType.RBRACE;
import static com.oracle.js.parser.TokenType.REGEX;
import static com.oracle.js.parser.TokenType.RPAREN;
import static com.oracle.js.parser.TokenType.STRING;
import static com.oracle.js.parser.TokenType.TEMPLATE;
import static com.oracle.js.parser.TokenType.TEMPLATE_HEAD;
import static com.oracle.js.parser.TokenType.TEMPLATE_MIDDLE;
import static com.oracle.js.parser.TokenType.TEMPLATE_TAIL;
import static com.oracle.js.parser.TokenType.XML;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

// @formatter:off
/**
 * Responsible for converting source content into a stream of tokens.
 *
 */
@SuppressWarnings("fallthrough")
public class Lexer extends Scanner {
    private static final boolean XML_LITERALS = Options.getBooleanProperty("lexer.xmlliterals");

    /** Content source. */
    private final Source source;

    /** Buffered stream for tokens. */
    private final TokenStream stream;

    /** True if here and edit strings are supported. */
    private final boolean scripting;

    /** True if shebang is supported. */
    private final boolean shebang;

    /** True if parsing in ECMAScript 6 mode. */
    private final boolean es6;

    /** True if a nested scan. (scan to completion, no EOF.) */
    private final boolean nested;

    /** Pending new line number and position. */
    int pendingLine;

    /** Position of last EOL + 1. */
    private int linePosition;

    /** Type of last token added. */
    private TokenType last;

    private final boolean pauseOnFunctionBody;
    private boolean pauseOnNextLeftBrace;
    boolean pauseOnRightBrace;

    /** Map to intern strings during parsing (memory footprint). */
    private final Map<String, String> internedStrings;

    private static final String JAVASCRIPT_WHITESPACE_HIGH =
        "\u1680" + // Ogham space mark
        "\u2000" + // en quad
        "\u2001" + // em quad
        "\u2002" + // en space
        "\u2003" + // em space
        "\u2004" + // three-per-em space
        "\u2005" + // four-per-em space
        "\u2006" + // six-per-em space
        "\u2007" + // figure space
        "\u2008" + // punctuation space
        "\u2009" + // thin space
        "\u200a" + // hair space
        "\u2028" + // line separator
        "\u2029" + // paragraph separator
        "\u202f" + // narrow no-break space
        "\u205f" + // medium mathematical space
        "\u3000" + // ideographic space
        "\ufeff"   // byte order mark
        ;

    private static final int JAVASCRIPT_WHITESPACE_HIGH_START = JAVASCRIPT_WHITESPACE_HIGH.charAt(0);

    public static String unicodeEscape(final char ch) {
        final StringBuilder sb = new StringBuilder();

        sb.append("\\u");

        final String hex = Integer.toHexString(ch);
        for (int i = hex.length(); i < 4; i++) {
            sb.append('0');
        }
        sb.append(hex);

        return sb.toString();
    }

    /**
     * Constructor
     *
     * @param source    the source
     * @param stream    the token stream to lex
     * @param scripting are we in scripting mode
     * @param es6       are we in ECMAScript 6 mode
     * @param shebang   do we support shebang
     */
    public Lexer(final Source source, final TokenStream stream, final boolean scripting, final boolean es6, final boolean shebang) {
        this(source, 0, source.getLength(), stream, scripting, es6, shebang, false);
    }

    /**
     * Constructor
     *
     * @param source    the source
     * @param start     start position in source from which to start lexing
     * @param len       length of source segment to lex
     * @param stream    token stream to lex
     * @param scripting are we in scripting mode
     * @param es6       are we in ECMAScript 6 mode
     * @param shebang   do we support shebang
     * @param pauseOnFunctionBody if true, lexer will return from {@link #lexify()} when it encounters a
     * function body. This is used with the feature where the parser is skipping nested function bodies to
     * avoid reading ahead unnecessarily when we skip the function bodies.
     */
    public Lexer(final Source source, final int start, final int len, final TokenStream stream, final boolean scripting, final boolean es6, final boolean shebang, final boolean pauseOnFunctionBody) {
        super(source.getContent().toString().toCharArray(), 1, start, len);
        this.source      = source;
        this.stream      = stream;
        this.scripting   = scripting;
        this.es6         = es6;
        this.shebang     = shebang;
        this.nested      = false;
        this.pendingLine = 1;
        this.last        = EOL;

        this.pauseOnFunctionBody = pauseOnFunctionBody;
        this.internedStrings = new HashMap<>();
    }

    private Lexer(final Lexer lexer, final State state) {
        super(lexer, state);

        source = lexer.source;
        stream = lexer.stream;
        scripting = lexer.scripting;
        es6 = lexer.es6;
        shebang = lexer.shebang;
        nested = true;

        pendingLine = state.pendingLine;
        linePosition = state.linePosition;
        last = EOL;
        pauseOnFunctionBody = false;
        internedStrings = lexer.internedStrings;
    }

    static class State extends Scanner.State {
        /** Pending new line number and position. */
        public final int pendingLine;

        /** Position of last EOL + 1. */
        public final int linePosition;

        /** Type of last token added. */
        public final TokenType last;

        /*
         * Constructor.
         */

        State(final int position, final int limit, final int line, final int pendingLine, final int linePosition, final TokenType last) {
            super(position, limit, line);

            this.pendingLine = pendingLine;
            this.linePosition = linePosition;
            this.last = last;
        }
    }

    /**
     * Save the state of the scan.
     *
     * @return Captured state.
     */
    @Override
    State saveState() {
        return new State(position, limit, line, pendingLine, linePosition, last);
    }

    /**
     * Restore the state of the scan.
     *
     * @param state
     *            Captured state.
     */
    void restoreState(final State state) {
        super.restoreState(state);

        pendingLine = state.pendingLine;
        linePosition = state.linePosition;
        last = state.last;
    }

    /**
     * Add a new token to the stream.
     *
     * @param type
     *            Token type.
     * @param start
     *            Start position.
     * @param end
     *            End position.
     */
    protected void add(final TokenType type, final int start, final int end) {
        // Record last token.
        last = type;

        // Only emit the last EOL in a cluster.
        if (type == EOL) {
            pendingLine = end;
            linePosition = start;
        } else {
            // Write any pending EOL to stream.
            if (pendingLine != -1) {
                stream.put(Token.toDesc(EOL, linePosition, pendingLine));
                pendingLine = -1;
            }

            // Write token to stream.
            stream.put(Token.toDesc(type, start, end - start));
        }
    }

    /**
     * Add a new token to the stream.
     *
     * @param type
     *            Token type.
     * @param start
     *            Start position.
     */
    protected void add(final TokenType type, final int start) {
        add(type, start, position);
    }

    /**
     * Skip end of line.
     *
     * @param addEOL true if EOL token should be recorded.
     */
    private void skipEOL(final boolean addEOL) {

        if (ch0 == '\r') { // detect \r\n pattern
            skip(1);
            if (ch0 == '\n') {
                skip(1);
            }
        } else { // all other space, ch0 is guaranteed to be EOL or \0
            skip(1);
        }

        // bump up line count
        line++;

        if (addEOL) {
            // Add an EOL token.
            add(EOL, position, line);
        }
    }

    /**
     * Skip over rest of line including end of line.
     *
     * @param addEOL true if EOL token should be recorded.
     */
    private void skipLine(final boolean addEOL) {
        // Ignore characters.
        while (!isEOL(ch0) && !atEOF()) {
            skip(1);
        }
        // Skip over end of line.
        skipEOL(addEOL);
    }

    /**
     * Test whether a char is valid JavaScript whitespace
     * @param ch a char
     * @return true if valid JavaScript whitespace
     */
    public static boolean isJSWhitespace(final char ch) {
        if (ch <= 0x000d) {
            return (ch >= 0x0009); // \t\n\u000b\u000c\r
        } else if (ch < JAVASCRIPT_WHITESPACE_HIGH_START) {
            return (ch == ' ' || ch == 0x00a0);
        } else {
            return isWhitespaceHigh(ch);
        }
    }

    private static boolean isWhitespaceHigh(final char ch) {
        for (int pos = 0; pos < JAVASCRIPT_WHITESPACE_HIGH.length(); pos++) {
            char cur = JAVASCRIPT_WHITESPACE_HIGH.charAt(pos);
            if (cur == ch) {
                return true;
            } else if (cur > ch) {
                return false;
            }
        }
        return false;
    }

    /**
     * Test whether a char is valid JavaScript end of line
     * @param ch a char
     * @return true if valid JavaScript end of line
     */
    public static boolean isJSEOL(final char ch) {
        return ch == '\n' || ch == '\r' || ch == '\u2028' || ch == '\u2029';
    }

    /**
     * Test if char is a string delimiter, e.g. '\' or '"'.
     * @param ch a char
     * @return true if string delimiter
     */
    protected boolean isStringDelimiter(final char ch) {
        return ch == '\'' || ch == '"';
    }

    /**
     * Test if char is a template literal delimiter ('`').
     */
    private static boolean isTemplateDelimiter(char ch) {
        return ch == '`';
    }

    /**
     * Test whether a char is valid JavaScript whitespace
     * @param ch a char
     * @return true if valid JavaScript whitespace
     */
    protected boolean isWhitespace(final char ch) {
        return Lexer.isJSWhitespace(ch);
    }

    /**
     * Test whether a char is valid JavaScript end of line
     * @param ch a char
     * @return true if valid JavaScript end of line
     */
    protected boolean isEOL(final char ch) {
        return Lexer.isJSEOL(ch);
    }

    /**
     * Skip over whitespace and detect end of line, adding EOL tokens if
     * encountered.
     *
     * @param addEOL true if EOL tokens should be recorded.
     */
    private void skipWhitespace(final boolean addEOL) {
        while (isWhitespace(ch0)) {
            if (isEOL(ch0)) {
                skipEOL(addEOL);
            } else {
                skip(1);
            }
        }
    }

    /**
     * Skip over comments.
     *
     * @return True if a comment.
     */
    protected boolean skipComments() {
        // Save the current position.
        final int start = position;

        if (ch0 == '/') {
            // Is it a // comment.
            if (ch1 == '/') {
                // Skip over //.
                skip(2);

                boolean directiveComment = false;
                if ((ch0 == '#' || ch0 == '@') && (ch1 == ' ')) {
                    directiveComment = true;
                }

                // Scan for EOL.
                while (!atEOF() && !isEOL(ch0)) {
                    skip(1);
                }
                // Did detect a comment.
                add(directiveComment ? DIRECTIVE_COMMENT : COMMENT, start);
                return true;
            } else if (ch1 == '*') {
                // Skip over /*.
                skip(2);
                // Scan for */.
                while (!atEOF() && !(ch0 == '*' && ch1 == '/')) {
                    // If end of line handle else skip character.
                    if (isEOL(ch0)) {
                        skipEOL(true);
                    } else {
                        skip(1);
                    }
                }

                if (atEOF()) {
                    // TODO - Report closing */ missing in parser.
                    add(ERROR, start);
                } else {
                    // Skip */.
                    skip(2);
                }

                // Did detect a comment.
                add(COMMENT, start);
                return true;
            }
        } else if (ch0 == '#') {
            assert scripting;
            // shell style comment
            // Skip over #.
            skip(1);
            // Scan for EOL.
            while (!atEOF() && !isEOL(ch0)) {
                skip(1);
            }
            // Did detect a comment.
            add(COMMENT, start);
            return true;
        }

        // Not a comment.
        return false;
    }

    /**
     * Convert a regex token to a token object.
     *
     * @param start  Position in source content.
     * @param length Length of regex token.
     * @return Regex token object.
     */
    public RegexToken valueOfPattern(final int start, final int length) {
        // Save the current position.
        final int savePosition = position;
        // Reset to beginning of content.
        reset(start);
        // Buffer for recording characters.
        final StringBuilder sb = new StringBuilder(length);

        // Skip /.
        skip(1);
        boolean inBrackets = false;
        // Scan for closing /, stopping at end of line.
        while (!atEOF() && ch0 != '/' && !isEOL(ch0) || inBrackets) {
            // Skip over escaped character.
            if (ch0 == '\\') {
                sb.append(ch0);
                sb.append(ch1);
                skip(2);
            } else {
                if (ch0 == '[') {
                    inBrackets = true;
                } else if (ch0 == ']') {
                    inBrackets = false;
                }

                // Skip literal character.
                sb.append(ch0);
                skip(1);
            }
        }

        // Get pattern as string.
        final String regex = stringIntern(sb.toString());

        // Skip /.
        skip(1);

        // Options as string.
        final String options = stringIntern(source.getString(position, scanIdentifier()));

        reset(savePosition);

        // Compile the pattern.
        return new RegexToken(regex, options);
    }

    /**
     * Return true if the given token can be the beginning of a literal.
     *
     * @param token a token
     * @return true if token can start a literal.
     */
    public boolean canStartLiteral(final TokenType token) {
        return token.startsWith('/') || ((scripting || XML_LITERALS) && token.startsWith('<'));
    }

    /**
     * interface to receive line information for multi-line literals.
     */
    protected interface LineInfoReceiver {
        /**
         * Receives line information
         * @param line last line number
         * @param linePosition position of last line
         */
        void lineInfo(int line, int linePosition);
    }

    /**
     * Check whether the given token represents the beginning of a literal. If so scan
     * the literal and return <tt>true</tt>, otherwise return false.
     *
     * @param token the token.
     * @param startTokenType the token type.
     * @param lir LineInfoReceiver that receives line info for multi-line string literals.
     * @return True if a literal beginning with startToken was found and scanned.
     */
    protected boolean scanLiteral(final long token, final TokenType startTokenType, final LineInfoReceiver lir) {
        // Check if it can be a literal.
        if (!canStartLiteral(startTokenType)) {
            return false;
        }
        // We break on ambiguous tokens so if we already moved on it can't be a literal.
        if (stream.get(stream.last()) != token) {
            return false;
        }

        // Record current position in case multiple heredocs start on this line - see JDK-8073653
        final State state = saveState();
        // Rewind to token start position
        reset(Token.descPosition(token));

        if (ch0 == '/') {
            return scanRegEx();
        } else if (ch0 == '<') {
            if (ch1 == '<') {
                return scanHereString(lir, state);
            } else if (Character.isJavaIdentifierStart(ch1)) {
                return scanXMLLiteral();
            }
        }

        return false;
    }

    /**
     * Scan over regex literal.
     *
     * @return True if a regex literal.
     */
    private boolean scanRegEx() {
        assert ch0 == '/';
        // Make sure it's not a comment.
        if (ch1 != '/' && ch1 != '*') {
            // Record beginning of literal.
            final int start = position;
            // Skip /.
            skip(1);
            boolean inBrackets = false;

            // Scan for closing /, stopping at end of line.
            while (!atEOF() && (ch0 != '/' || inBrackets) && !isEOL(ch0)) {
                // Skip over escaped character.
                if (ch0 == '\\') {
                    skip(1);
                    if (isEOL(ch0)) {
                        reset(start);
                        return false;
                    }
                    skip(1);
                } else {
                    if (ch0 == '[') {
                        inBrackets = true;
                    } else if (ch0 == ']') {
                        inBrackets = false;
                    }

                    // Skip literal character.
                    skip(1);
                }
            }

            // If regex literal.
            if (ch0 == '/') {
                // Skip /.
                skip(1);

                // Skip over options.
                while (!atEOF() && Character.isJavaIdentifierPart(ch0) || ch0 == '\\' && ch1 == 'u') {
                    skip(1);
                }

                // Add regex token.
                add(REGEX, start);
                // Regex literal detected.
                return true;
            }

            // False start try again.
            reset(start);
        }

        // Regex literal not detected.
        return false;
    }

    /**
     * Convert a digit to a integer.  Can't use Character.digit since we are
     * restricted to ASCII by the spec.
     *
     * @param ch   Character to convert.
     * @param base Numeric base.
     *
     * @return The converted digit or -1 if invalid.
     */
    protected static int convertDigit(final char ch, final int base) {
        int digit;

        if ('0' <= ch && ch <= '9') {
            digit = ch - '0';
        } else if ('A' <= ch && ch <= 'Z') {
            digit = ch - 'A' + 10;
        } else if ('a' <= ch && ch <= 'z') {
            digit = ch - 'a' + 10;
        } else {
            return -1;
        }

        return digit < base ? digit : -1;
    }


    /**
     * Get the value of a hexadecimal numeric sequence.
     *
     * @param length Number of digits.
     * @param type   Type of token to report against.
     * @return Value of sequence or < 0 if no digits.
     */
    private int hexSequence(final int length, final TokenType type) {
        int value = 0;

        for (int i = 0; i < length; i++) {
            final int digit = convertDigit(ch0, 16);

            if (digit == -1) {
                error(Lexer.message("invalid.hex"), type, position, limit - position);
                return i == 0 ? -1 : value;
            }

            value = digit | value << 4;
            skip(1);
        }

        return value;
    }

    /**
     * Get the value of a variable-length hexadecimal numeric sequence delimited by curly braces.
     *
     * @param type   Type of token to report against.
     * @return Value of sequence or < 0 if no digits.
     */
    private int varlenHexSequence(final TokenType type) {
        assert ch0 == '{';
        skip(1);

        int value = 0;

        for (int i = 0; !atEOF(); i++) {
            if (ch0 == '}') {
                if (i != 0) {
                    skip(1);
                    return value;
                } else {
                    error(Lexer.message("invalid.hex"), type, position, limit - position);
                    skip(1);
                    return -1;
                }
            }

            final int digit = convertDigit(ch0, 16);

            if (digit == -1) {
                error(Lexer.message("invalid.hex"), type, position, limit - position);
                return i == 0 ? -1 : value;
            }

            value = digit | value << 4;

            if (value > 1114111) {
                error(Lexer.message("invalid.hex"), type, position, limit - position);
                return -1;
            }

            skip(1);
        }

        return value;
    }

    /**
     * Get the value of a UnicodeEscapeSequence ('u' already scanned).
     *
     * @param type   Type of token to report against.
     * @return Value of sequence or < 0 if no digits.
     */
    private int unicodeEscapeSequence(final TokenType type) {
        if (ch0 == '{' && es6) {
            return varlenHexSequence(type);
        } else {
            return hexSequence(4, type);
        }
    }

    /**
     * Get the value of an octal numeric sequence. This parses up to 3 digits with a maximum value of 255.
     *
     * @return Value of sequence.
     */
    private int octalSequence() {
        int value = 0;

        for (int i = 0; i < 3; i++) {
            final int digit = convertDigit(ch0, 8);

            if (digit == -1) {
                break;
            }
            value = digit | value << 3;
            skip(1);

            if (i == 1 && value >= 32) {
                break;
            }
        }
        return value;
    }

    public boolean checkIdentForKeyword(final long token, final String keyword) {
        final int len = Token.descLength(token);
        final int start = Token.descPosition(token);
        if (len != keyword.length()) {
            return false;
        }

        for (int i = 0; i < len; ++i) {
            if (content[start + i] != keyword.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convert a string to a JavaScript identifier.
     *
     * @param start  Position in source content.
     * @param length Length of token.
     * @param convertUnicode convert Unicode symbols in the Ident string.
     * @return Ident string or null if an error.
     */
    private String valueOfIdent(final int start, final int length, final boolean convertUnicode) {
        // End of scan.
        final int end = start + length;
        // Buffer for recording characters.
        final StringBuilder sb = new StringBuilder(length);

        int pos = start;

        // Scan until end of line or end of file.
        while (pos < end) {

            char curCh0 = content[pos];

            // If escape character.
            if (convertUnicode && curCh0 == '\\' && charAt(pos + 1) == 'u') {
                // Save the current position.
                final int savePosition = position;
                reset(pos + 2);
                final int ch = unicodeEscapeSequence(TokenType.IDENT);
                if (Character.isBmpCodePoint(ch) && isWhitespace((char)ch)) {
                    return null;
                }
                if (ch < 0) {
                    sb.append('\\');
                    sb.append('u');
                } else {
                    sb.appendCodePoint(ch);
                }
                pos = position;
                reset(savePosition);
            } else {
                // Add regular character.
                sb.append(curCh0);
                pos++;
            }
        }

        return stringIntern(sb.toString());
    }

    /**
     * Scan over and identifier or keyword. Handles identifiers containing
     * encoded Unicode chars.
     *
     * Example:
     *
     * var \u0042 = 44;
     */
    private void scanIdentifierOrKeyword() {
        // Record beginning of identifier.
        final int start = position;
        // Scan identifier.
        final int length = scanIdentifier();
        // Check to see if it is a keyword.
        final TokenType type = TokenLookup.lookupKeyword(content, start, length);
        if (type == FUNCTION && pauseOnFunctionBody) {
            pauseOnNextLeftBrace = true;
        }
        // Add keyword or identifier token.
        add(type, start);
    }

    /**
     * Convert a string to a JavaScript string object.
     *
     * @param start  Position in source content.
     * @param length Length of token.
     * @return JavaScript string object.
     */
    private String valueOfString(final int start, final int length, final boolean strict) {
        // Save the current position.
        final int savePosition = position;
        // Calculate the end position.
        final int end = start + length;
        // Reset to beginning of string.
        reset(start);

        // Buffer for recording characters.
        final StringBuilder sb = new StringBuilder(length);

        // Scan until end of string.
        while (position < end) {
            // If escape character.
            if (ch0 == '\\') {
                skip(1);

                final char next = ch0;
                final int afterSlash = position;

                skip(1);

                // Special characters.
                switch (next) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    if (strict) {
                        // "\0" itself is allowed in strict mode. Only other 'real'
                        // octal escape sequences are not allowed (eg. "\02", "\31").
                        // See section 7.8.4 String literals production EscapeSequence
                        if (next != '0' || (ch0 >= '0' && ch0 <= '9')) {
                            error(Lexer.message("strict.no.octal"), STRING, position, limit - position);
                        }
                    }
                    reset(afterSlash);
                    // Octal sequence.
                    final int ch = octalSequence();

                    if (ch < 0) {
                        sb.append('\\');
                        sb.append('x');
                    } else {
                        sb.append((char)ch);
                    }
                    break;
                }
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case '\'':
                    sb.append('\'');
                    break;
                case '\"':
                    sb.append('\"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '\r': // CR | CRLF
                    if (ch0 == '\n') {
                        skip(1);
                    }
                    // fall through
                case '\n': // LF
                case '\u2028': // LS
                case '\u2029': // PS
                    // continue on the next line, slash-return continues string
                    // literal
                    break;
                case 'x': {
                    // Hex sequence.
                    final int ch = hexSequence(2, STRING);

                    if (ch < 0) {
                        sb.append('\\');
                        sb.append('x');
                    } else {
                        sb.append((char)ch);
                    }
                    break;
                }
                case 'u': {
                    // Unicode sequence.
                    final int ch = unicodeEscapeSequence(STRING);

                    if (ch < 0) {
                        sb.append('\\');
                        sb.append('u');
                    } else {
                        sb.appendCodePoint(ch);
                    }
                    break;
                }
                case 'v':
                    sb.append('\u000B');
                    break;
                // All other characters.
                default:
                    sb.append(next);
                    break;
                }
            } else if (ch0 == '\r') {
                // Convert CR-LF or CR to LF line terminator.
                sb.append('\n');
                skip(ch1 == '\n' ? 2 : 1);
            } else {
                // Add regular character.
                sb.append(ch0);
                skip(1);
            }
        }

        // Restore position.
        reset(savePosition);

        return stringIntern(sb.toString());
    }

    /**
     * Scan over a string literal.
     * @param add true if we are not just scanning but should actually modify the token stream
     */
    protected void scanString(final boolean add) {
        // Type of string.
        TokenType type = STRING;
        // Record starting quote.
        final char quote = ch0;
        // Skip over quote.
        skip(1);

        // Record beginning of string content.
        final State stringState = saveState();

        // Scan until close quote or end of line.
        while (!atEOF() && ch0 != quote && !isEOL(ch0)) {
            // Skip over escaped character.
            if (ch0 == '\\') {
                type = ESCSTRING;
                skip(1);
                if (!isEscapeCharacter(ch0)) {
                    error(Lexer.message("invalid.escape.char"), STRING, position, limit - position);
                }
                if (isEOL(ch0)) {
                    // Multiline string literal
                    skipEOL(false);
                    continue;
                }
            }
            // Skip literal character.
            skip(1);
        }

        // If close quote.
        if (ch0 == quote) {
            // Skip close quote.
            skip(1);
        } else {
            error(Lexer.message("missing.close.quote"), STRING, position, limit - position);
        }

        // If not just scanning.
        if (add) {
            // Record end of string.
            stringState.setLimit(position - 1);

            if (scripting && !stringState.isEmpty()) {
                switch (quote) {
                case '`':
                    // Mark the beginning of an exec string.
                    add(EXECSTRING, stringState.position, stringState.limit);
                    // Frame edit string with left brace.
                    add(LBRACE, stringState.position, stringState.position);
                    // Process edit string.
                    editString(type, stringState);
                    // Frame edit string with right brace.
                    add(RBRACE, stringState.limit, stringState.limit);
                    break;
                case '"':
                    // Only edit double quoted strings.
                    editString(type, stringState);
                    break;
                case '\'':
                    // Add string token without editing.
                    add(type, stringState.position, stringState.limit);
                    break;
                default:
                    break;
                }
            } else {
                /// Add string token without editing.
                add(type, stringState.position, stringState.limit);
            }
        }
    }

    /**
     * Scan a template literal, stopping at the first expression.
     */
    private void scanTemplate() {
        assert ch0 == '`';
        // Skip over quote
        skip(1);

        scanTemplateString(TEMPLATE);
    }

    /**
     * Continue scanning a template literal after an expression.
     */
    protected final void scanTemplateSpan() {
        scanTemplateString(TEMPLATE_MIDDLE);
    }

    /**
     * Scan a template literal string span.
     */
    private void scanTemplateString(TokenType type) {
        assert type == TEMPLATE || type == TEMPLATE_MIDDLE;
        // already skipped over '`' or '}'

        // Record beginning of string content.
        State stringState = saveState();

        // Scan until close quote
        while (!atEOF()) {
            // Skip over escaped character.
            if (ch0 == '`') {
                skip(1);
                // Record end of string.
                stringState.setLimit(position - 1);
                add(type == TEMPLATE ? type : TEMPLATE_TAIL, stringState.position, stringState.limit);
                return;
            } else if (ch0 == '$' && ch1 == '{') {
                skip(2);
                stringState.setLimit(position - 2);
                add(type == TEMPLATE ? TEMPLATE_HEAD : type, stringState.position, stringState.limit);
                return;
            } else if (ch0 == '\\') {
                skip(1);
                // EscapeSequence
                if (!isEscapeCharacter(ch0)) {
                    error(Lexer.message("invalid.escape.char"), TEMPLATE, position, limit - position);
                }
                if (isEOL(ch0)) {
                    // LineContinuation
                    skipEOL(false);
                    continue;
                }
            }  else if (isEOL(ch0)) {
                // LineTerminatorSequence
                skipEOL(false);
                continue;
            }

            // Skip literal character.
            skip(1);
        }

        error(Lexer.message("missing.close.quote"), TEMPLATE, position, limit - position);
    }

    /**
     * Is the given character a valid escape char after "\" ?
     *
     * @param ch character to be checked
     * @return if the given character is valid after "\"
     */
    protected boolean isEscapeCharacter(final char ch) {
        return true;
    }

    /**
     * Convert string to number.
     *
     * @param valueString  String to convert.
     * @param radix        Numeric base.
     * @return Converted number.
     */
    private static Number valueOf(final String valueString, final int radix) throws NumberFormatException {
        try {
            final long value = Long.parseLong(valueString, radix);
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int)value;
            }
            return value;
        } catch (final NumberFormatException e) {
            if (radix == 10) {
                return Double.valueOf(valueString);
            }
            // (CWirth) added by Oracle Labs Graal.js
            if (radix == 16 && valueString.length() >= 15) {
                //special case to parse large hex values; see testv8/hex-parsing.js
                return (new BigInteger(valueString, 16)).doubleValue();
            }

            double value = 0.0;

            for (int i = 0; i < valueString.length(); i++) {
                final char ch = valueString.charAt(i);
                // Preverified, should always be a valid digit.
                final int digit = convertDigit(ch, radix);
                value *= radix;
                value += digit;
            }

            return value;
        }
    }

    private static BigInteger valueOfBigInt(final String valueString) {
        if (valueString.length() > 2 && valueString.charAt(0) == '0') {
            switch (valueString.charAt(1)) {
                case 'x':
                case 'X':
                    return new BigInteger(valueString.substring(2), 16);
                case 'o':
                case 'O':
                    return new BigInteger(valueString.substring(2), 8);
                case 'b':
                case 'B':
                    return new BigInteger(valueString.substring(2), 2);
                default:
                    return new BigInteger(valueString, 10);
            }
        } else {
            return new BigInteger(valueString, 10);
        }
    }

    /**
     * Scan a number.
     */
    protected void scanNumber() {
        // Record beginning of number.
        final int start = position;
        // Assume value is a decimal.
        TokenType type = DECIMAL;

        // First digit of number.
        int digit = convertDigit(ch0, 10);

        // If number begins with 0x.
        if (digit == 0 && (ch1 == 'x' || ch1 == 'X') && convertDigit(ch2, 16) != -1) {
            // Skip over 0xN.
            skip(3);
            // Skip over remaining digits.
            while (convertDigit(ch0, 16) != -1) {
                skip(1);
            }

            type = HEXADECIMAL;
        } else if (digit == 0 && es6 && (ch1 == 'o' || ch1 == 'O') && convertDigit(ch2, 8) != -1) {
            // Skip over 0oN.
            skip(3);
            // Skip over remaining digits.
            while (convertDigit(ch0, 8) != -1) {
                skip(1);
            }

            type = OCTAL;
        } else if (digit == 0 && es6 && (ch1 == 'b' || ch1 == 'B') && convertDigit(ch2, 2) != -1) {
            // Skip over 0bN.
            skip(3);
            // Skip over remaining digits.
            while (convertDigit(ch0, 2) != -1) {
                skip(1);
            }

            type = BINARY_NUMBER;
        } else {
            // Check for possible octal constant.
            boolean octal = digit == 0;
            // Skip first digit if not leading '.'.
            if (digit != -1) {
                skip(1);
            }

            // Skip remaining digits.
            while ((digit = convertDigit(ch0, 10)) != -1) {
                // Check octal only digits.
                if (octal && digit >= 8) {
                    octal = false;
                    type = NON_OCTAL_DECIMAL;
                }
                // Skip digit.
                skip(1);
            }

            if (octal && position - start > 1) {
                type = OCTAL_LEGACY;
            } else if (ch0 == '.' || ch0 == 'E' || ch0 == 'e') {
                // Must be a double.
                if (ch0 == '.') {
                    // Skip period.
                    skip(1);
                    // Skip mantissa.
                    while (convertDigit(ch0, 10) != -1) {
                        skip(1);
                    }
                }

                // Detect exponent.
                if (ch0 == 'E' || ch0 == 'e') {
                    // Skip E.
                    skip(1);
                    // Detect and skip exponent sign.
                    if (ch0 == '+' || ch0 == '-') {
                        skip(1);
                    }
                    // Skip exponent.
                    while (convertDigit(ch0, 10) != -1) {
                        skip(1);
                    }
                }

                type = FLOATING;
            }
        }

        if (ch0 == 'n' && type != FLOATING) {
            // Skip n
            skip(1);
            type = BIGINT;
        }

        if (Character.isJavaIdentifierStart(ch0)) {
            error(Lexer.message("missing.space.after.number"), type, position, 1);
        }

        // Add number token.
        add(type, start);
    }

    /**
     * Convert a regex token to a token object.
     *
     * @param start  Position in source content.
     * @param length Length of regex token.
     * @return Regex token object.
     */
    XMLToken valueOfXML(final int start, final int length) {
        return new XMLToken(source.getString(start, length));
    }

    /**
     * Scan over a XML token.
     *
     * @return TRUE if is an XML literal.
     */
    private boolean scanXMLLiteral() {
        assert ch0 == '<' && Character.isJavaIdentifierStart(ch1);
        if (XML_LITERALS) {
            // Record beginning of xml expression.
            final int start = position;

            int openCount = 0;

            do {
                if (ch0 == '<') {
                    if (ch1 == '/' && Character.isJavaIdentifierStart(ch2)) {
                        skip(3);
                        openCount--;
                    } else if (Character.isJavaIdentifierStart(ch1)) {
                        skip(2);
                        openCount++;
                    } else if (ch1 == '?') {
                        skip(2);
                    } else if (ch1 == '!' && ch2 == '-' && ch3 == '-') {
                        skip(4);
                    } else {
                        reset(start);
                        return false;
                    }

                    while (!atEOF() && ch0 != '>') {
                        if (ch0 == '/' && ch1 == '>') {
                            openCount--;
                            skip(1);
                            break;
                        } else if (ch0 == '\"' || ch0 == '\'') {
                            scanString(false);
                        } else {
                            skip(1);
                        }
                    }

                    if (ch0 != '>') {
                        reset(start);
                        return false;
                    }

                    skip(1);
                } else if (atEOF()) {
                    reset(start);
                    return false;
                } else {
                    skip(1);
                }
            } while (openCount > 0);

            add(XML, start);
            return true;
        }

        return false;
    }

    /**
     * Determines if the specified character is permissible as the first character in a ECMAScript identifier.
     *
     * @param codePoint the character to be tested
     * @return {@code true} if the character may start an ECMAScript identifier; {@code false} otherwise
     */
    private static boolean isJSIdentifierStart(int codePoint) {
        return (Character.isUnicodeIdentifierStart(codePoint) && codePoint != '\u2e2f')
                || codePoint == '$'
                || codePoint == '_'
                || isOtherIDStart(codePoint);
    }

    /**
     * Determines if the specified character has Other_ID_Start Unicode property.
     * 
     * @param codePoint the character to be tested.
     * @return {@code true} if the character has Other_ID_Start Unicode property,
     * {@code false} otherwise.
     */
    private static boolean isOtherIDStart(int codePoint) {
        return codePoint == 0x1885
                || codePoint == 0x1886
                || codePoint == 0x2118
                || codePoint == 0x212e
                || codePoint == 0x309b
                || codePoint == 0x309c;
    }

    /**
     * Determines if the specified character may be part of an ECMAScript identifier as other than the first character.
     *
     * @param codePoint the character to be tested
     * @return {@code true} if the character may be part of an ECMAScript identifier; {@code false} otherwise
     */
    private static boolean isJSIdentifierPart(int codePoint) {
        return (Character.isUnicodeIdentifierPart(codePoint) && !Character.isIdentifierIgnorable(codePoint) && codePoint != '\u2e2f')
                || codePoint == '$'
                || codePoint == '\u200c'  // <ZWNJ>
                || codePoint == '\u200d'  // <ZWJ>
                || isOtherIDContinue(codePoint);
    }

    /**
     * Determines if the specified character has Other_ID_Continue Unicode property.
     * 
     * @param codePoint the character to be tested.
     * @return {@code true} if the character has Other_ID_Continue Unicode property,
     * {@code false} otherwise.
     */
    private static boolean isOtherIDContinue(int codePoint) {
        return isOtherIDStart(codePoint)
                || codePoint == 0x00b7
                || codePoint == 0x0387
                || codePoint == 0x1369
                || codePoint == 0x136a
                || codePoint == 0x136b
                || codePoint == 0x136c
                || codePoint == 0x136d
                || codePoint == 0x136e
                || codePoint == 0x136f
                || codePoint == 0x1370
                || codePoint == 0x1371
                || codePoint == 0x19da;
    }

    /**
     * Scan over identifier characters.
     *
     * @return Length of identifier or zero if none found.
     */
    private int scanIdentifier() {
        final int start = position;

        // Make sure first character is valid start character.
        if (ch0 == '\\' && ch1 == 'u') {
            skip(2);
            final int codePoint = unicodeEscapeSequence(TokenType.IDENT);

            if (!isJSIdentifierStart(codePoint)) {
                error(Lexer.message("illegal.identifier.character"), TokenType.IDENT, start, position - start);
            }
        } else if (isJSIdentifierStart(ch0)) {
            skip(1);
        } else if (Character.isHighSurrogate(ch0) && Character.isLowSurrogate(ch1) && isJSIdentifierStart(Character.toCodePoint(ch0, ch1))) {
            skip(2);
        } else {
            // Not an identifier.
            return 0;
        }

        // Make sure remaining characters are valid part characters.
        while (!atEOF()) {
            if (ch0 == '\\' && ch1 == 'u') {
                skip(2);
                final int codePoint = unicodeEscapeSequence(TokenType.IDENT);

                if (!isJSIdentifierPart(codePoint)) {
                    error(Lexer.message("illegal.identifier.character"), TokenType.IDENT, start, position - start);
                }
            } else if (isJSIdentifierPart(ch0)) {
                skip(1);
            } else if (Character.isHighSurrogate(ch0) && Character.isLowSurrogate(ch1) && isJSIdentifierPart(Character.toCodePoint(ch0, ch1))) {
                skip(2);
            } else {
                break;
            }
        }

        // Length of identifier sequence.
        return position - start;
    }

    /**
     * Compare two identifiers (in content) for equality.
     *
     * @param aStart  Start of first identifier.
     * @param aLength Length of first identifier.
     * @param bStart  Start of second identifier.
     * @param bLength Length of second identifier.
     * @return True if equal.
     */
    private boolean identifierEqual(final int aStart, final int aLength, final int bStart, final int bLength) {
        if (aLength == bLength) {
            for (int i = 0; i < aLength; i++) {
                if (content[aStart + i] != content[bStart + i]) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Detect if a line starts with a marker identifier.
     *
     * @param identStart  Start of identifier.
     * @param identLength Length of identifier.
     * @return True if detected.
     */
    private boolean hasHereMarker(final int identStart, final int identLength) {
        // Skip any whitespace.
        skipWhitespace(false);

        return identifierEqual(identStart, identLength, position, scanIdentifier());
    }

    /**
     * Lexer to service edit strings.
     */
    private static class EditStringLexer extends Lexer {
        /** Type of string literals to emit. */
        final TokenType stringType;

        /*
         * Constructor.
         */

        EditStringLexer(final Lexer lexer, final TokenType stringType, final State stringState) {
            super(lexer, stringState);

            this.stringType = stringType;
        }

        /**
         * Lexify the contents of the string.
         */
        @Override
        public void lexify() {
            // Record start of string position.
            int stringStart = position;
            // Indicate that the priming first string has not been emitted.
            boolean primed = false;

            while (true) {
                // Detect end of content.
                if (atEOF()) {
                    break;
                }

                // Honour escapes (should be well formed.)
                if (ch0 == '\\' && stringType == ESCSTRING) {
                    skip(2);

                    continue;
                }

                // If start of expression.
                if (ch0 == '$' && ch1 == '{') {
                    if (!primed || stringStart != position) {
                        if (primed) {
                            add(ADD, stringStart, stringStart + 1);
                        }

                        add(stringType, stringStart, position);
                        primed = true;
                    }

                    // Skip ${
                    skip(2);

                    // Save expression state.
                    final State expressionState = saveState();

                    // Start with one open brace.
                    int braceCount = 1;

                    // Scan for the rest of the string.
                    while (!atEOF()) {
                        // If closing brace.
                        if (ch0 == '}') {
                            // Break only only if matching brace.
                            if (--braceCount == 0) {
                                break;
                            }
                        } else if (ch0 == '{') {
                            // Bump up the brace count.
                            braceCount++;
                        }

                        // Skip to next character.
                        skip(1);
                    }

                    // If braces don't match then report an error.
                    if (braceCount != 0) {
                        error(Lexer.message("edit.string.missing.brace"), LBRACE, expressionState.position - 1, 1);
                    }

                    // Mark end of expression.
                    expressionState.setLimit(position);
                    // Skip closing brace.
                    skip(1);

                    // Start next string.
                    stringStart = position;

                    // Concatenate expression.
                    add(ADD, expressionState.position, expressionState.position + 1);
                    add(LPAREN, expressionState.position, expressionState.position + 1);

                    // Scan expression.
                    final Lexer lexer = new Lexer(this, expressionState);
                    lexer.lexify();

                    // Close out expression parenthesis.
                    add(RPAREN, position - 1, position);

                    continue;
                }

                // Next character in string.
                skip(1);
            }

            // If there is any unemitted string portion.
            if (stringStart != limit) {
                // Concatenate remaining string.
                if (primed) {
                    add(ADD, stringStart, stringStart + 1);
                }

                add(stringType, stringStart, limit);
            }
        }

    }

    /**
     * Edit string for nested expressions.
     *
     * @param stringType  Type of string literals to emit.
     * @param stringState State of lexer at start of string.
     */
    private void editString(final TokenType stringType, final State stringState) {
        // Use special lexer to scan string.
        final EditStringLexer lexer = new EditStringLexer(this, stringType, stringState);
        lexer.lexify();

        // Need to keep lexer informed.
        last = stringType;
    }

    /**
     * Scan over a here string.
     *
     * @return TRUE if is a here string.
     */
    private boolean scanHereString(final LineInfoReceiver lir, final State oldState) {
        assert ch0 == '<' && ch1 == '<';
        if (scripting) {
            // Record beginning of here string.
            final State saved = saveState();

            // << or <<<
            final boolean excludeLastEOL = ch2 != '<';

            if (excludeLastEOL) {
                skip(2);
            } else {
                skip(3);
            }

            // Scan identifier. It might be quoted, indicating that no string editing should take place.
            final char quoteChar = ch0;
            final boolean noStringEditing = quoteChar == '"' || quoteChar == '\'';
            if (noStringEditing) {
                skip(1);
            }
            final int identStart = position;
            final int identLength = scanIdentifier();
            if (noStringEditing) {
                if (ch0 != quoteChar) {
                    error(Lexer.message("here.non.matching.delimiter"), last, position, 0);
                    restoreState(saved);
                    return false;
                }
                skip(1);
            }

            // Check for identifier.
            if (identLength == 0) {
                // Treat as shift.
                restoreState(saved);

                return false;
            }

            // Record rest of line.
            final State restState = saveState();
            // keep line number updated
            int lastLine = line;

            skipLine(false);
            lastLine++;
            int lastLinePosition = position;
            restState.setLimit(position);

            if (oldState.position > position) {
                restoreState(oldState);
                skipLine(false);
            }

            // Record beginning of string.
            final State stringState = saveState();
            int stringEnd = position;

            // Hunt down marker.
            while (!atEOF()) {
                // Skip any whitespace.
                skipWhitespace(false);

                if (hasHereMarker(identStart, identLength)) {
                    break;
                }

                skipLine(false);
                lastLine++;
                lastLinePosition = position;
                stringEnd = position;
            }

            // notify last line information
            lir.lineInfo(lastLine, lastLinePosition);

            // Record end of string.
            stringState.setLimit(stringEnd);

            // If marker is missing.
            if (stringState.isEmpty() || atEOF()) {
                error(Lexer.message("here.missing.end.marker", source.getString(identStart, identLength)), last, position, 0);
                restoreState(saved);

                return false;
            }

            // Remove last end of line if specified.
            if (excludeLastEOL) {
                // Handles \n.
                if (content[stringEnd - 1] == '\n') {
                    stringEnd--;
                }

                // Handles \r and \r\n.
                if (content[stringEnd - 1] == '\r') {
                    stringEnd--;
                }

                // Update end of string.
                stringState.setLimit(stringEnd);
            }

            // Edit string if appropriate.
            if (!noStringEditing && !stringState.isEmpty()) {
                editString(STRING, stringState);
            } else {
                // Add here string.
                add(STRING, stringState.position, stringState.limit);
            }

            // Scan rest of original line.
            final Lexer restLexer = new Lexer(this, restState);

            restLexer.lexify();

            return true;
        }

        return false;
    }

    /**
     * Breaks source content down into lex units, adding tokens to the token
     * stream. The routine scans until the stream buffer is full. Can be called
     * repeatedly until EOF is detected.
     */
    public void lexify() {
        while (!stream.isFull() || nested) {
            // Skip over whitespace.
            skipWhitespace(true);

            // Detect end of file.
            if (atEOF()) {
                if (!nested) {
                    // Add an EOF token at the end.
                    add(EOF, position);
                }

                break;
            }

            // Check for comments. Note that we don't scan for regexp and other literals here as
            // we may not have enough context to distinguish them from similar looking operators.
            // Instead we break on ambiguous operators below and let the parser decide.
            if (ch0 == '/' && skipComments()) {
                continue;
            }

            if ((scripting || shebang) && ch0 == '#' && skipComments()) {
                continue;
            }

            // TokenType for lookup of delimiter or operator.
            TokenType type;

            if (ch0 == '.' && convertDigit(ch1, 10) != -1) {
                // '.' followed by digit.
                // Scan and add a number.
                scanNumber();
            } else if ((type = TokenLookup.lookupOperator(ch0, ch1, ch2, ch3)) != null) {
                // Get the number of characters in the token.
                final int typeLength = type.getLength();
                // Skip that many characters.
                skip(typeLength);
                // Add operator token.
                add(type, position - typeLength);
                // Some operator tokens also mark the beginning of regexp, XML, or here string literals.
                // We break to let the parser decide what it is.
                if (canStartLiteral(type)) {
                    break;
                } else if (type == LBRACE && pauseOnNextLeftBrace) {
                    pauseOnNextLeftBrace = false;
                    break;
                } else if (type == RBRACE && pauseOnRightBrace) {
                    break;
                }
            } else if (isJSIdentifierStart((Character.isHighSurrogate(ch0) && Character.isLowSurrogate(ch1)) ? Character.toCodePoint(ch0, ch1) : ch0)
                    || (ch0 == '\\' && ch1 == 'u')) {
                // Scan and add identifier or keyword.
                scanIdentifierOrKeyword();
            } else if (isStringDelimiter(ch0)) {
                // Scan and add a string.
                scanString(true);
            } else if (Character.isDigit(ch0)) {
                // Scan and add a number.
                scanNumber();
            } else if (isTemplateDelimiter(ch0) && es6) {
                // Scan and add template in ES6 mode.
                scanTemplate();
                // Let the parser continue from here.
                break;
            } else if (isTemplateDelimiter(ch0) && scripting) {
                // Scan and add an exec string ('`') in scripting mode.
                scanString(true);
            } else {
                // Don't recognize this character.
                skip(1);
                add(ERROR, position - 1);
            }
        }
    }

    /**
     * Return value of token given its token descriptor.
     *
     * @param token  Token descriptor.
     * @return JavaScript value.
     */
    Object getValueOf(final long token, final boolean strict) {
        return getValueOf(token, strict, true);
    }

    /**
     * Return value of token given its token descriptor.
     *
     * @param token  Token descriptor.
     * @param convertUnicode Perform Unicode conversion.
     * @return JavaScript value.
     */
    Object getValueOf(final long token, final boolean strict, final boolean convertUnicode) {
        final int start = Token.descPosition(token);
        final int len   = Token.descLength(token);

        switch (Token.descType(token)) {
        case DECIMAL:
        case NON_OCTAL_DECIMAL:
            return Lexer.valueOf(source.getString(start, len), 10); // number
        case HEXADECIMAL:
            return Lexer.valueOf(source.getString(start + 2, len - 2), 16); // number
        case OCTAL_LEGACY:
            return Lexer.valueOf(source.getString(start, len), 8); // number
        case OCTAL:
            return Lexer.valueOf(source.getString(start + 2, len - 2), 8); // number
        case BINARY_NUMBER:
            return Lexer.valueOf(source.getString(start + 2, len - 2), 2); // number
        case BIGINT:
            return Lexer.valueOfBigInt(source.getString(start, len - 1)); // number
        case FLOATING:
            final String str   = source.getString(start, len);
            final double value = Double.valueOf(str);
            if (str.indexOf('.') != -1) {
                return value; //number
            }
            //anything without an explicit decimal point is still subject to a
            //"representable as int or long" check. Then the programmer does not
            //explicitly code something as a double. For example new Color(int, int, int)
            //and new Color(float, float, float) will get ambiguous for cases like
            //new Color(1.0, 1.5, 1.5) if we don't respect the decimal point.
            //yet we don't want e.g. 1e6 to be a double unnecessarily
            if (JSType.isStrictlyRepresentableAsInt(value)) {
                return (int)value;
            } else if (JSType.isStrictlyRepresentableAsLong(value)) {
                return (long)value;
            }
            return value;
        case STRING:
            return stringIntern(source.getString(start, len)); // String
        case ESCSTRING:
            return valueOfString(start, len, strict); // String
        case IDENT:
            return valueOfIdent(start, len, convertUnicode); // String
        case REGEX:
            return valueOfPattern(start, len); // RegexToken::LexerToken
        case TEMPLATE:
        case TEMPLATE_HEAD:
        case TEMPLATE_MIDDLE:
        case TEMPLATE_TAIL:
            return valueOfString(start, len, true); // String
        case XML:
            return valueOfXML(start, len); // XMLToken::LexerToken
        case DIRECTIVE_COMMENT:
            return source.getString(start, len);
        default:
            break;
        }

        return null;
    }

    /**
     * Returns the Template Value of the specified part of a tagged template literal.
     *
     * @param token template string token.
     * @return Template Value if the value is string, returns {@code null}
     * otherwise (i.e. if the value is undefined).
     */
    String valueOfTaggedTemplateString(final long token) {
        final int savePosition = position;

        try {
            return valueOfString(Token.descPosition(token), Token.descLength(token), true);
        } catch (ParserException ex) {
            // An invalid escape sequence in a tagged template string is not an error.
            return null;
        } finally {
            reset(savePosition);
        }
    }

    /**
     * Get the raw string value of a template literal string part.
     *
     * @param token template string token
     * @return raw string
     */
    public String valueOfRawString(final long token) {
        final int start  = Token.descPosition(token);
        final int length = Token.descLength(token);

        // Save the current position.
        final int savePosition = position;
        // Calculate the end position.
        final int end = start + length;
        // Reset to beginning of string.
        reset(start);

        // Buffer for recording characters.
        final StringBuilder sb = new StringBuilder(length);

        // Scan until end of string.
        while (position < end) {
            if (ch0 == '\r') {
                // Convert CR-LF or CR to LF line terminator.
                sb.append('\n');
                skip(ch1 == '\n' ? 2 : 1);
            } else {
                // Add regular character.
                sb.append(ch0);
                skip(1);
            }
        }

        // Restore position.
        reset(savePosition);

        return stringIntern(sb.toString());
    }

    public String stringIntern(String candidate) {
        String interned = internedStrings.putIfAbsent(candidate, candidate);
        return interned == null ? candidate : interned;
    }

    /**
     * Get the correctly localized error message for a given message id format arguments
     * @param msgId message id
     * @param args  format arguments
     * @return message
     */
    protected static String message(final String msgId, final String... args) {
        return ECMAErrors.getMessage("lexer.error." + msgId, args);
    }

    /**
     * Generate a runtime exception
     *
     * @param message       error message
     * @param type          token type
     * @param start         start position of lexed error
     * @param length        length of lexed error
     * @throws ParserException  unconditionally
     */
    protected void error(final String message, final TokenType type, final int start, final int length) throws ParserException {
        final long token     = Token.toDesc(type, start, length);
        final int  pos       = Token.descPosition(token);
        final int  lineNum   = source.getLine(pos);
        final int  columnNum = source.getColumn(pos);
        final String formatted = ErrorManager.format(message, source, lineNum, columnNum, token);
        throw new ParserException(JSErrorType.SyntaxError, formatted, source, lineNum, columnNum, token);
    }

    /**
     * Helper class for Lexer tokens, e.g XML or RegExp tokens.
     * This is the abstract superclass
     */
    public abstract static class LexerToken {
        private final String expression;

        /**
         * Constructor
         * @param expression token expression
         */
        protected LexerToken(final String expression) {
            this.expression = expression;
        }

        /**
         * Get the expression
         * @return expression
         */
        public String getExpression() {
            return expression;
        }
    }

    /**
     * Temporary container for regular expressions.
     */
    public static class RegexToken extends LexerToken {
        /** Options. */
        private final String options;

        /**
         * Constructor.
         *
         * @param expression  regexp expression
         * @param options     regexp options
         */
        public RegexToken(final String expression, final String options) {
            super(expression);
            this.options = options;
        }

        /**
         * Get regexp options
         * @return options
         */
        public String getOptions() {
            return options;
        }

        @Override
        public String toString() {
            return '/' + getExpression() + '/' + options;
        }
    }

    /**
     * Temporary container for XML expression.
     */
    public static class XMLToken extends LexerToken {

        /**
         * Constructor.
         *
         * @param expression  XML expression
         */
        public XMLToken(final String expression) {
            super(expression);
        }
    }
}
