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

import static com.oracle.js.parser.TokenKind.BINARY;
import static com.oracle.js.parser.TokenKind.BRACKET;
import static com.oracle.js.parser.TokenKind.CONTEXTUAL;
import static com.oracle.js.parser.TokenKind.FUTURE;
import static com.oracle.js.parser.TokenKind.FUTURESTRICT;
import static com.oracle.js.parser.TokenKind.IR;
import static com.oracle.js.parser.TokenKind.KEYWORD;
import static com.oracle.js.parser.TokenKind.LITERAL;
import static com.oracle.js.parser.TokenKind.SPECIAL;
import static com.oracle.js.parser.TokenKind.UNARY;

import java.util.Locale;

// @formatter:off
// Checkstyle: stop
/**
 * Description of all the JavaScript tokens.
 */
public enum TokenType {
    ERROR                (SPECIAL,  null),
    EOF                  (SPECIAL,  null),
    EOL                  (SPECIAL,  null),
    COMMENT              (SPECIAL,  null),
    // comments of the form //@ foo=bar or //# foo=bar
    // These comments are treated as special instructions
    // to the lexer, parser or codegenerator.
    DIRECTIVE_COMMENT    (SPECIAL,  null),

    NOT            (UNARY,   "!",    15, false),
    NE             (BINARY,  "!=",    9, true),
    NE_STRICT      (BINARY,  "!==",   9, true),
    MOD            (BINARY,  "%",    13, true),
    ASSIGN_MOD     (BINARY,  "%=",    2, false),
    BIT_AND        (BINARY,  "&",     8, true),
    AND            (BINARY,  "&&",    5, true),
    ASSIGN_BIT_AND (BINARY,  "&=",    2, false),
    LPAREN         (BRACKET, "(",    17, true),
    RPAREN         (BRACKET, ")",     0, true),
    MUL            (BINARY,  "*",    13, true),
    ASSIGN_MUL     (BINARY,  "*=",    2, false),
    EXP            (BINARY,  "**",   14, false),
    ASSIGN_EXP     (BINARY,  "**=",   2, false),
    ADD            (BINARY,  "+",    12, true),
    INCPREFIX      (UNARY,   "++",   16, true),
    ASSIGN_ADD     (BINARY,  "+=",    2, false),
    COMMARIGHT     (BINARY,  ",",     1, true),
    SUB            (BINARY,  "-",    12, true),
    DECPREFIX      (UNARY,   "--",   16, true),
    ASSIGN_SUB     (BINARY,  "-=",    2, false),
    PERIOD         (BRACKET, ".",    18, true),
    DIV            (BINARY,  "/",    13, true),
    ASSIGN_DIV     (BINARY,  "/=",    2, false),
    COLON          (BINARY,  ":"),
    SEMICOLON      (BINARY,  ";"),
    LT             (BINARY,  "<",    10, true),
    SHL            (BINARY,  "<<",   11, true),
    ASSIGN_SHL     (BINARY,  "<<=",   2, false),
    LE             (BINARY,  "<=",   10, true),
    ASSIGN         (BINARY,  "=",     2, false),
    EQ             (BINARY,  "==",    9, true),
    EQ_STRICT      (BINARY,  "===",   9, true),
    ARROW          (BINARY,  "=>",    2, true),
    GT             (BINARY,  ">",    10, true),
    GE             (BINARY,  ">=",   10, true),
    SAR            (BINARY,  ">>",   11, true),
    ASSIGN_SAR     (BINARY,  ">>=",   2, false),
    SHR            (BINARY,  ">>>",  11, true),
    ASSIGN_SHR     (BINARY,  ">>>=",  2, false),
    TERNARY        (BINARY,  "?",     3, false),
    LBRACKET       (BRACKET, "[",    18, true),
    RBRACKET       (BRACKET, "]",     0, true),
    BIT_XOR        (BINARY,  "^",     7, true),
    ASSIGN_BIT_XOR (BINARY,  "^=",    2, false),
    LBRACE         (BRACKET,  "{"),
    BIT_OR         (BINARY,  "|",     6, true),
    ASSIGN_BIT_OR  (BINARY,  "|=",    2, false),
    OR             (BINARY,  "||",    4, true),
    RBRACE         (BRACKET, "}"),
    BIT_NOT        (UNARY,   "~",     15, false),
    ELLIPSIS       (UNARY,   "..."),

    // ECMA 7.6.1.1 Keywords, 7.6.1.2 Future Reserved Words.
    // All other Java keywords are commented out.

//  ABSTRACT       (FUTURE,       "abstract"),
    AS             (CONTEXTUAL,   "as"),
    ASYNC          (CONTEXTUAL,   "async"),
    AWAIT          (CONTEXTUAL,   "await"),
//  BOOLEAN        (FUTURE,       "boolean"),
    BREAK          (KEYWORD,      "break"),
//  BYTE           (FUTURE,       "byte"),
    CASE           (KEYWORD,      "case"),
    CATCH          (KEYWORD,      "catch"),
//  CHAR           (FUTURE,       "char"),
    CLASS          (FUTURE,       "class"),
    CONST          (KEYWORD,      "const"),
    CONTINUE       (KEYWORD,      "continue"),
    DEBUGGER       (KEYWORD,      "debugger"),
    DEFAULT        (KEYWORD,      "default"),
    DELETE         (UNARY,        "delete",     15, false),
    DO             (KEYWORD,      "do"),
//  DOUBLE         (FUTURE,       "double"),
//  EACH           (KEYWORD,      "each"),  // Contextual.
    ELSE           (KEYWORD,      "else"),
    ENUM           (FUTURE,       "enum"),
    EXPORT         (FUTURE,       "export"),
    EXTENDS        (FUTURE,       "extends"),
    FALSE          (LITERAL,      "false"),
//  FINAL          (FUTURE,       "final"),
    FINALLY        (KEYWORD,      "finally"),
//  FLOAT          (FUTURE,       "float"),
    FOR            (KEYWORD,      "for"),
    FROM           (CONTEXTUAL,   "from"),
    FUNCTION       (KEYWORD,      "function"),
    GET            (CONTEXTUAL,   "get"),
//  GOTO           (FUTURE,       "goto"),
    IF             (KEYWORD,      "if"),
    IMPLEMENTS     (FUTURESTRICT, "implements"),
    IMPORT         (FUTURE,       "import"),
    IN             (BINARY,       "in",         10, true),
    INSTANCEOF     (BINARY,       "instanceof", 10, true),
//  INT            (FUTURE,       "int"),
    INTERFACE      (FUTURESTRICT, "interface"),
    LET            (FUTURESTRICT, "let"),
//  LONG           (FUTURE,       "long"),
//  NATIVE         (FUTURE,       "native"),
    NEW            (UNARY,        "new",        18, false),
    NULL           (LITERAL,      "null"),
    OF             (CONTEXTUAL,   "of"),
    PACKAGE        (FUTURESTRICT, "package"),
    PRIVATE        (FUTURESTRICT, "private"),
    PROTECTED      (FUTURESTRICT, "protected"),
    PUBLIC         (FUTURESTRICT, "public"),
    RETURN         (KEYWORD,      "return"),
    SET            (CONTEXTUAL,   "set"),
//  SHORT          (FUTURE,       "short"),
    STATIC         (FUTURESTRICT, "static"),
    SUPER          (FUTURE,       "super"),
    SWITCH         (KEYWORD,      "switch"),
//  SYNCHRONIZED   (FUTURE,       "synchronized"),
    THIS           (KEYWORD,      "this"),
    THROW          (KEYWORD,      "throw"),
//  THROWS         (FUTURE,       "throws"),
//  TRANSIENT      (FUTURE,       "transient"),
    TRUE           (LITERAL,      "true"),
    TRY            (KEYWORD,      "try"),
    TYPEOF         (UNARY,        "typeof",     15, false),
    VAR            (KEYWORD,      "var"),
    VOID           (UNARY,        "void",       15, false),
//  VOLATILE       (FUTURE,       "volatile"),
    WHILE          (KEYWORD,      "while"),
    WITH           (KEYWORD,      "with"),
    YIELD          (FUTURESTRICT, "yield"),

    DECIMAL        (LITERAL,      null),
    NON_OCTAL_DECIMAL (LITERAL,   null), // NonOctalDecimalIntegerLiteral
    HEXADECIMAL    (LITERAL,      null),
    OCTAL_LEGACY   (LITERAL,      null),
    OCTAL          (LITERAL,      null),
    BINARY_NUMBER  (LITERAL,      null),
    BIGINT         (LITERAL,      null), // BigInt literal
    FLOATING       (LITERAL,      null),
    STRING         (LITERAL,      null),
    ESCSTRING      (LITERAL,      null),
    EXECSTRING     (LITERAL,      null),
    IDENT          (LITERAL,      null),
    REGEX          (LITERAL,      null),
    XML            (LITERAL,      null),
    OBJECT         (LITERAL,      null),
    ARRAY          (LITERAL,      null),
    TEMPLATE       (LITERAL,      null),
    TEMPLATE_HEAD  (LITERAL,      null),
    TEMPLATE_MIDDLE(LITERAL,      null),
    TEMPLATE_TAIL  (LITERAL,      null),

    COMMALEFT      (IR,           null),
    DECPOSTFIX     (IR,           null),
    INCPOSTFIX     (IR,           null),
    SPREAD_ARGUMENT(IR,           null),
    SPREAD_ARRAY   (IR,           null),
    SPREAD_OBJECT  (IR,           null),
    YIELD_STAR     (IR,           null),
    ASSIGN_INIT    (IR,           null);

    /** Next token kind in token lookup table. */
    private TokenType next;

    /** Classification of token. */
    private final TokenKind kind;

    /** Printable name of token. */
    private final String name;

    /** Operator precedence. */
    private final int precedence;

    /** Left associativity */
    private final boolean isLeftAssociative;

    /** Cache values to avoid cloning. */
    private static final TokenType[] values;

    TokenType(final TokenKind kind, final String name) {
        next              = null;
        this.kind         = kind;
        this.name         = name;
        precedence        = 0;
        isLeftAssociative = false;
    }

    TokenType(final TokenKind kind, final String name, final int precedence, final boolean isLeftAssociative) {
        next                   = null;
        this.kind              = kind;
        this.name              = name;
        this.precedence        = precedence;
        this.isLeftAssociative = isLeftAssociative;
    }

    /**
     * Determines if the token has greater precedence than other.
     *
     * @param other  Compare token.
     * @param isLeft Is to the left of the other.
     *
     * @return {@code true} if greater precedence.
     */
    public boolean needsParens(final TokenType other, final boolean isLeft) {
        return other.precedence != 0 &&
               (precedence > other.precedence ||
               precedence == other.precedence && isLeftAssociative && !isLeft);
    }

    /**
     * Determines if the type is a valid operator.
     *
     * @param in {@code false} if IN operator should be ignored.
     *
     * @return {@code true} if valid operator.
     */
    public boolean isOperator(final boolean in) {
        return kind == BINARY && (in || this != IN) && precedence != 0;
    }

    public int getLength() {
        assert name != null : "Token name not set";
        return name.length();
    }

    public String getName() {
        return name;
    }

    public String getNameOrType() {
        return name == null ? super.name().toLowerCase(Locale.ENGLISH) : name;
    }

    public TokenType getNext() {
        return next;
    }

    public void setNext(final TokenType next) {
        this.next = next;
    }

    public TokenKind getKind() {
        return kind;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isLeftAssociative() {
        return isLeftAssociative;
    }

    boolean startsWith(final char c) {
        return name != null && name.length() > 0 && name.charAt(0) == c;
    }

    static TokenType[] getValues() {
       return values;
    }

    @Override
    public String toString() {
        return getNameOrType();
    }

    /**
     * Is type one of {@code = *= /= %= += -= <<= >>= >>>= &= ^= |= **=}?
     */
    public boolean isAssignment() {
        switch (this) {
        case ASSIGN:
        case ASSIGN_INIT:
        case ASSIGN_ADD:
        case ASSIGN_BIT_AND:
        case ASSIGN_BIT_OR:
        case ASSIGN_BIT_XOR:
        case ASSIGN_DIV:
        case ASSIGN_MOD:
        case ASSIGN_EXP:
        case ASSIGN_MUL:
        case ASSIGN_SAR:
        case ASSIGN_SHL:
        case ASSIGN_SHR:
        case ASSIGN_SUB:
           return true;
        default:
           return false;
        }
    }

    public boolean isContextualKeyword() {
        return kind == TokenKind.CONTEXTUAL;
    }

    public boolean isFutureStrict() {
        return kind == TokenKind.FUTURESTRICT;
    }

    static {
        // Avoid cloning of enumeration.
        values = TokenType.values();
    }
}
