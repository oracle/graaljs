/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser;

import static com.oracle.js.parser.TokenType.COMMENT;
import static com.oracle.js.parser.TokenType.DIRECTIVE_COMMENT;
import static com.oracle.js.parser.TokenType.EOF;
import static com.oracle.js.parser.TokenType.EOL;
import static com.oracle.js.parser.TokenType.IDENT;

import java.math.BigInteger;

import java.util.function.Function;

import com.oracle.js.parser.Lexer.LexerToken;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.LiteralNode;

/**
 * Base class for parsers.
 */
public abstract class AbstractParser {
    /** Source to parse. */
    protected final Source source;

    /** Error manager to report errors. */
    protected final ErrorManager errors;

    /** Stream of lex tokens to parse. */
    protected TokenStream stream;

    /** Index of current token. */
    protected int k;

    /** Previous token - accessible to sub classes */
    protected long previousToken;

    /** Descriptor of current token. */
    protected long token;

    /** Type of current token. */
    protected TokenType type;

    /** Type of last token. */
    protected TokenType last;

    /** Start position of current token. */
    protected int start;

    /** Finish position of previous token. */
    protected int finish;

    /** Current line number. */
    protected int line;

    /** Position of last EOL + 1. */
    protected int linePosition;

    /** Lexer used to scan source content. */
    protected Lexer lexer;

    /** Is this parser running under strict mode? */
    protected boolean isStrictMode;

    /** What should line numbers be counted from? */
    protected final int lineOffset;

    /**
     * Construct a parser.
     *
     * @param source Source to parse.
     * @param errors Error reporting manager.
     * @param strict True if we are in strict mode
     * @param lineOffset Offset from which lines should be counted
     */
    protected AbstractParser(final Source source, final ErrorManager errors, final boolean strict, final int lineOffset) {
        if (source.getLength() > Token.LENGTH_MASK) {
            throw new RuntimeException("Source exceeds size limit of " + Token.LENGTH_MASK + " bytes");
        }
        this.source = source;
        this.errors = errors;
        this.k = -1;
        this.token = Token.toDesc(EOL, 0, 1);
        this.type = EOL;
        this.last = EOL;
        this.isStrictMode = strict;
        this.lineOffset = lineOffset;
    }

    /**
     * Get the ith token.
     *
     * @param i Index of token.
     *
     * @return the token
     */
    protected final long getToken(final int i) {
        // Make sure there are enough tokens available.
        while (i > stream.last()) {
            // If we need to buffer more for lookahead.
            if (stream.isFull()) {
                stream.grow();
            }

            // Get more tokens.
            lexer.lexify();
        }

        return stream.get(i);
    }

    // Checkstyle: stop
    /**
     * Return the tokenType of the ith token.
     *
     * @param i Index of token
     *
     * @return the token type
     */
    protected final TokenType T(final int i) {
        // Get token descriptor and extract tokenType.
        return Token.descType(getToken(i));
    }

    // Checkstyle: resume
    /**
     * Seek next token that is not an EOL or comment.
     *
     * @return tokenType of next token.
     */
    protected final TokenType next() {
        do {
            nextOrEOL();
        } while (type == EOL || type == COMMENT);

        return type;
    }

    /**
     * Seek next token or EOL (skipping comments.)
     *
     * @return tokenType of next token.
     */
    protected final TokenType nextOrEOL() {
        do {
            nextToken();
            if (type == DIRECTIVE_COMMENT) {
                checkDirectiveComment();
            }
        } while (type == COMMENT || type == DIRECTIVE_COMMENT);

        return type;
    }

    // sourceURL= after directive comment
    private static final String SOURCE_URL_PREFIX = "sourceURL=";

    // currently only @sourceURL=foo supported
    private void checkDirectiveComment() {
        // if already set, ignore this one
        if (source.getExplicitURL() != null) {
            return;
        }

        final String comment = (String) lexer.getValueOf(token, isStrictMode);
        final int len = comment.length();
        // 4 characters for directive comment marker //@\s or //#\s
        if (len > 4 && comment.substring(4).startsWith(SOURCE_URL_PREFIX)) {
            source.setExplicitURL(comment.substring(4 + SOURCE_URL_PREFIX.length()));
        }
    }

    /**
     * Seek next token.
     *
     * @return tokenType of next token.
     */
    private TokenType nextToken() {
        if (type != EOF) {
            // Set up next token.
            k++;
            final long lastToken = token;
            // Capture last token type, but ignore comments (which are irrelevant for the purpose of
            // newline detection).
            if (type != COMMENT) {
                last = type;
                previousToken = token;
            }
            token = getToken(k);
            type = Token.descType(token);

            // do this before the start is changed below
            if (last != EOL) {
                finish = start + Token.descLength(lastToken);
            }

            if (type == EOL) {
                line = Token.descLength(token);
                linePosition = Token.descPosition(token);
            } else {
                start = Token.descPosition(token);
            }

        }

        return type;
    }

    /**
     * Get the message string for a message ID and arguments
     *
     * @param msgId The Message ID
     * @param args The arguments
     *
     * @return The message string
     */
    protected static String message(final String msgId, final String... args) {
        return ECMAErrors.getMessage("parser.error." + msgId, args);
    }

    /**
     * Report an error.
     *
     * @param message Error message.
     * @param errorToken Offending token.
     * @return ParserException upon failure. Caller should throw and not ignore
     */
    protected final ParserException error(final String message, final long errorToken) {
        return error(JSErrorType.SyntaxError, message, errorToken);
    }

    /**
     * Report an error.
     *
     * @param errorType The error type
     * @param message Error message.
     * @param errorToken Offending token.
     * @return ParserException upon failure. Caller should throw and not ignore
     */
    protected final ParserException error(final JSErrorType errorType, final String message, final long errorToken) {
        final int position = Token.descPosition(errorToken);
        final int lineNum = source.getLine(position);
        final int columnNum = source.getColumn(position);
        final String formatted = ErrorManager.format(message, source, lineNum, columnNum, errorToken);
        return new ParserException(errorType, formatted, source, lineNum, columnNum, errorToken);
    }

    /**
     * Report an error.
     *
     * @param message Error message.
     * @return ParserException upon failure. Caller should throw and not ignore
     */
    protected final ParserException error(final String message) {
        return error(JSErrorType.SyntaxError, message);
    }

    /**
     * Report an error.
     *
     * @param errorType The error type
     * @param message Error message.
     * @return ParserException upon failure. Caller should throw and not ignore
     */
    protected final ParserException error(final JSErrorType errorType, final String message) {
        final int position = Token.descPosition(token);
        final int column = source.getColumn(position);
        final String formatted = ErrorManager.format(message, source, line, column, token);
        return new ParserException(errorType, formatted, source, line, column, token);
    }

    /**
     * Report a warning to the error manager.
     *
     * @param errorType The error type of the warning
     * @param message Warning message.
     * @param errorToken error token
     */
    protected final void warning(final JSErrorType errorType, final String message, final long errorToken) {
        errors.warning(error(errorType, message, errorToken));
    }

    /**
     * Generate 'expected' message.
     *
     * @param expected Expected tokenType.
     *
     * @return the message string
     */
    protected final String expectMessage(final TokenType expected) {
        final String tokenString = Token.toString(source, token);
        String msg;

        if (expected == null) {
            msg = AbstractParser.message("expected.stmt", tokenString);
        } else {
            final String expectedName = expected.getNameOrType();
            msg = AbstractParser.message("expected", expectedName, tokenString);
        }

        return msg;
    }

    protected final String expectMessage(final TokenType expected, final long errorToken) {
        final String expectedName = expected.getNameOrType();
        final String tokenString = Token.toString(source, errorToken);
        return AbstractParser.message("expected", expectedName, tokenString);
    }

    /**
     * Check current token and advance to the next token.
     *
     * @param expected Expected tokenType.
     *
     * @throws ParserException on unexpected token type
     */
    protected final void expect(final TokenType expected) throws ParserException {
        expectDontAdvance(expected);
        next();
    }

    /**
     * Check current token, but don't advance to the next token.
     *
     * @param expected Expected tokenType.
     *
     * @throws ParserException on unexpected token type
     */
    protected final void expectDontAdvance(final TokenType expected) throws ParserException {
        if (type != expected) {
            throw error(expectMessage(expected));
        }
    }

    /**
     * Get the value of the current token. If the current token contains an escape sequence, the
     * method does not attempt to convert it.
     *
     * @return JavaScript value of the token.
     */
    protected final Object getValueNoEscape() {
        try {
            return lexer.getValueOf(token, isStrictMode, false);
        } catch (final ParserException e) {
            errors.error(e);
        }
        return null;
    }

    /**
     * Get the value of the current token.
     *
     * @return JavaScript value of the token.
     */
    protected final Object getValue() {
        return getValue(token);
    }

    /**
     * Get the value of a specific token
     *
     * @param valueToken the token
     *
     * @return JavaScript value of the token
     */
    protected final Object getValue(final long valueToken) {
        try {
            return lexer.getValueOf(valueToken, isStrictMode);
        } catch (final ParserException e) {
            errors.error(e);
        }

        return null;
    }

    /**
     * Certain future reserved words can be used as identifiers in non-strict mode. Check if the
     * current token is one such.
     *
     * @return true if non strict mode identifier
     */
    protected final boolean isNonStrictModeIdent() {
        return !isStrictMode && type.getKind() == TokenKind.FUTURESTRICT;
    }

    /**
     * Get ident.
     *
     * @return Ident node.
     */
    protected final IdentNode getIdent() {
        // Capture IDENT token.
        long identToken = token;

        if (type == IDENT) {
            final String ident = (String) getValue(identToken);

            next();

            return createIdentNode(identToken, finish, ident);
        } else if (type.isContextualKeyword() || isNonStrictModeIdent()) {
            final String ident = type.getName();

            next();

            return new IdentNode(identToken, finish, ident);
        } else {
            // Not an IDENT.
            throw error(expectMessage(IDENT));
        }
    }

    /**
     * Creates a new {@link IdentNode} as if invoked with a
     * {@link IdentNode#IdentNode(long, int, String) constructor} but making sure that the
     * {@code name} is deduplicated within this parse job.
     *
     * @param identToken the token for the new {@code IdentNode}
     * @param identFinish the finish for the new {@code IdentNode}
     * @param name the name for the new {@code IdentNode}. It will be de-duplicated.
     * @return a newly constructed {@code IdentNode} with the specified token, finish, and name; the
     *         name will be deduplicated.
     */
    protected IdentNode createIdentNode(final long identToken, final int identFinish, final String name) {
        assert isInterned(name) : name;
        return new IdentNode(identToken, identFinish, name);
    }

    private boolean isInterned(final String name) {
        return isSame(lexer.stringIntern(name), name) || isSame(name.intern(), name);
    }

    private static boolean isSame(Object a, Object b) {
        return a == b;
    }

    /**
     * Check if current token is in identifier name
     *
     * @return true if current token is an identifier name
     */
    protected final boolean isIdentifierName() {
        return isIdentifierName(token);
    }

    /**
     * Check if token is an identifier name
     *
     * @return true if token is an identifier name
     */
    protected final boolean isIdentifierName(long currentToken) {
        final TokenType currentType = Token.descType(currentToken);
        assert currentType != IDENT; // handled before
        final TokenKind kind = currentType.getKind();
        if (kind == TokenKind.KEYWORD || kind == TokenKind.FUTURE || kind == TokenKind.FUTURESTRICT || kind == TokenKind.CONTEXTUAL) {
            return true;
        }

        // only literals allowed are null, false and true
        if (kind == TokenKind.LITERAL) {
            switch (currentType) {
                case FALSE:
                case NULL:
                case TRUE:
                    return true;
                default:
                    return false;
            }
        }

        // Fake out identifier.
        final long identToken = Token.recast(currentToken, IDENT);
        // Get IDENT.
        final String ident = (String) getValue(identToken);
        return !ident.isEmpty() && Character.isJavaIdentifierStart(ident.charAt(0));
    }

    /**
     * Create an IdentNode from the current token
     *
     * @return an IdentNode representing the current token
     */
    protected final IdentNode getIdentifierName() {
        if (type == IDENT) {
            return getIdent();
        } else if (isIdentifierName()) {
            // Fake out identifier.
            final long identToken = Token.recast(token, IDENT);
            // Get IDENT.
            final String ident = (String) getValue(identToken);
            next();
            // Create IDENT node.
            return createIdentNode(identToken, finish, ident);
        } else {
            expect(IDENT);
            return null;
        }
    }

    /**
     * Create a LiteralNode from the current token
     *
     * @return LiteralNode representing the current token
     * @throws ParserException if any literals fails to parse
     */
    protected final LiteralNode<?> getLiteral() throws ParserException {
        // Capture LITERAL token.
        final long literalToken = token;

        // Create literal node.
        final Object value = getValue();
        // Advance to have a correct finish
        next();

        LiteralNode<?> node = null;

        if (value == null) {
            node = LiteralNode.newInstance(literalToken, finish);
        } else if (value instanceof BigInteger) {
            node = LiteralNode.newInstance(literalToken, finish, (BigInteger) value);
        } else if (value instanceof Number) {
            node = LiteralNode.newInstance(literalToken, finish, (Number) value, getNumberToStringConverter());
        } else if (value instanceof String) {
            node = LiteralNode.newInstance(literalToken, finish, (String) value);
        } else if (value instanceof LexerToken) {
            validateLexerToken((LexerToken) value);
            node = LiteralNode.newInstance(literalToken, finish, (LexerToken) value);
        } else {
            assert false : "unknown type for LiteralNode: " + value.getClass();
        }

        return node;
    }

    /**
     * Lexer token validation hook for subclasses.
     *
     * @param lexerToken the lexer token to validate
     */
    protected void validateLexerToken(final LexerToken lexerToken) {
    }

    /**
     * Custom number-to-string converter used to convert numeric property names to strings.
     *
     * @return custom number-to-string converter or {@code null} to use the default converter
     */
    protected Function<Number, String> getNumberToStringConverter() {
        return null;
    }
}
