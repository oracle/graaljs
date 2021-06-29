/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

// Checkstyle: stop
/**
 * Description of all the JavaScript tokens.
 */
public enum TokenType {
    //@formatter:off
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
    ASSIGN_AND     (BINARY,  "&&=",   2, false, 12),
    LPAREN         (BRACKET, "(",    17, true),
    RPAREN         (BRACKET, ")",     0, true),
    MUL            (BINARY,  "*",    13, true),
    ASSIGN_MUL     (BINARY,  "*=",    2, false),
    EXP            (BINARY,  "**",   14, false, 6),
    ASSIGN_EXP     (BINARY,  "**=",   2, false, 6),
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
    ASSIGN_OR      (BINARY,  "||=",   2, false, 12),
    RBRACE         (BRACKET, "}"),
    BIT_NOT        (UNARY,   "~",    15, false),
    ELLIPSIS       (UNARY,   "..."),
    NULLISHCOALESC (BINARY,  "??",    4, true, 11),
    ASSIGN_NULLCOAL(BINARY,  "??=",   2, false, 12),
    OPTIONAL_CHAIN (BRACKET, "?.",   18, true, 11),

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
    PRIVATE_IDENT  (LITERAL,      null),

    COMMALEFT      (IR,           null),
    DECPOSTFIX     (IR,           null),
    INCPOSTFIX     (IR,           null),
    SPREAD_ARGUMENT(IR,           null),
    SPREAD_ARRAY   (IR,           null),
    SPREAD_OBJECT  (IR,           null),
    YIELD_STAR     (IR,           null),
    ASSIGN_INIT    (IR,           null),
    NAMEDEVALUATION(IR,           null),

    // Records & Tuples Proposal tokens
    RECORD         (LITERAL,      null),
    TUPLE          (LITERAL,      null),
    SPREAD_RECORD  (IR,           null),
    SPREAD_TUPLE   (IR,           null),
    // TODO: Associate with the correct ECMAScript Version
    HASH_BRACKET   (BRACKET, "#[", 0, true, 13),
    HASH_BRACE     (BRACKET, "#{", 0, true, 13);
    //@formatter:on

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

    /** ECMAScript version defining the token. */
    private final int ecmaScriptVersion;

    /** Cache values to avoid cloning. */
    private static final TokenType[] tokenValues;

    TokenType(final TokenKind kind, final String name) {
        this(kind, name, 0, false);
    }

    TokenType(final TokenKind kind, final String name, final int precedence, final boolean isLeftAssociative) {
        this(kind, name, precedence, isLeftAssociative, 5);
    }

    TokenType(final TokenKind kind, final String name, final int precedence, final boolean isLeftAssociative, final int ecmaScriptVersion) {
        next = null;
        this.kind = kind;
        this.name = name;
        this.precedence = precedence;
        this.isLeftAssociative = isLeftAssociative;
        this.ecmaScriptVersion = ecmaScriptVersion;
    }

    /**
     * Determines if the token has greater precedence than other.
     *
     * @param other Compare token.
     * @param isLeft Is to the left of the other.
     *
     * @return {@code true} if greater precedence.
     */
    public boolean needsParens(final TokenType other, final boolean isLeft) {
        return other.precedence != 0 &&
                        (precedence > other.precedence || (precedence == other.precedence && isLeftAssociative && !isLeft));
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

    void setNext(final TokenType next) {
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

    public int getECMAScriptVersion() {
        return ecmaScriptVersion;
    }

    boolean startsWith(final char c) {
        return name != null && name.length() > 0 && name.charAt(0) == c;
    }

    static TokenType[] getValues() {
        return tokenValues;
    }

    @Override
    public String toString() {
        return getNameOrType();
    }

    /**
     * Is type one of {@code = *= /= %= += -= <<= >>= >>>= &= ^= |= **= &&= ||= ??=}?
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
            case ASSIGN_AND:
            case ASSIGN_OR:
            case ASSIGN_NULLCOAL:
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
        tokenValues = TokenType.values();
    }
}
