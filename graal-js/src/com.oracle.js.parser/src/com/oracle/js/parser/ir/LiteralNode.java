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

package com.oracle.js.parser.ir;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.oracle.js.parser.JSType;
import com.oracle.js.parser.Lexer.LexerToken;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * Literal nodes represent JavaScript values.
 *
 * @param <T> the literal type
 */
public abstract class LiteralNode<T> extends Expression {
    /** Literal value */
    protected final T value;

    /**
     * Constructor
     *
     * @param token token
     * @param finish finish
     * @param value the value of the literal
     */
    protected LiteralNode(final long token, final int finish, final T value) {
        super(token, finish);
        this.value = value;
    }

    /**
     * Copy constructor
     *
     * @param literalNode source node
     */
    protected LiteralNode(final LiteralNode<T> literalNode) {
        this(literalNode, literalNode.value);
    }

    /**
     * A copy constructor with value change.
     *
     * @param literalNode the original literal node
     * @param newValue new value for this node
     */
    protected LiteralNode(final LiteralNode<T> literalNode, final T newValue) {
        super(literalNode);
        this.value = newValue;
    }

    /**
     * Check if the literal value is null
     *
     * @return true if literal value is null
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Fetch boolean value of node.
     *
     * @return boolean value of node.
     */
    public boolean getBoolean() {
        return JSType.toBoolean(value);
    }

    /**
     * Fetch int32 value of node.
     *
     * @return Int32 value of node.
     */
    public int getInt32() {
        return JSType.toInt32(value);
    }

    /**
     * Fetch uint32 value of node.
     *
     * @return uint32 value of node.
     */
    public long getUint32() {
        return JSType.toUint32(value);
    }

    /**
     * Fetch long value of node
     *
     * @return long value of node
     */
    public long getLong() {
        return JSType.toLong(value);
    }

    /**
     * Fetch double value of node.
     *
     * @return double value of node.
     */
    public double getNumber() {
        return JSType.toNumber(value);
    }

    /**
     * Fetch String value of node.
     *
     * @return String value of node.
     */
    public String getString() {
        return String.valueOf(value);
    }

    /**
     * Fetch Object value of node.
     *
     * @return Object value of node.
     */
    public Object getObject() {
        return value;
    }

    /**
     * Test if the value is an array
     *
     * @return True if value is an array
     */
    public boolean isArray() {
        return false;
    }

    public List<Expression> getElementExpressions() {
        return null;
    }

    /**
     * Test if the value is a boolean.
     *
     * @return True if value is a boolean.
     */
    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    /**
     * Test if the value is a string.
     *
     * @return True if value is a string.
     */
    public boolean isString() {
        return value instanceof String;
    }

    /**
     * Test if tha value is a number
     *
     * @return True if value is a number
     */
    public boolean isNumeric() {
        return value instanceof Number;
    }

    /**
     * Assist in IR navigation.
     *
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterLiteralNode(this)) {
            return visitor.leaveLiteralNode(this);
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterLiteralNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.toString());
        }
    }

    /**
     * Get the literal node value
     *
     * @return the value
     */
    public final T getValue() {
        return value;
    }

    private static Expression[] valueToArray(final List<Expression> value) {
        return value.toArray(new Expression[value.size()]);
    }

    /**
     * Create a new null literal
     *
     * @param token token
     * @param finish finish
     *
     * @return the new literal node
     */
    public static LiteralNode<Object> newInstance(final long token, final int finish) {
        return new NullLiteralNode(token, finish);
    }

    /**
     * Super class for primitive (side-effect free) literals.
     *
     * @param <T> the literal type
     */
    public static class PrimitiveLiteralNode<T> extends LiteralNode<T> implements PropertyKey {
        private PrimitiveLiteralNode(final long token, final int finish, final T value) {
            super(token, finish, value);
        }

        private PrimitiveLiteralNode(final PrimitiveLiteralNode<T> literalNode) {
            super(literalNode);
        }

        /**
         * Check if the literal value is boolean true
         *
         * @return true if literal value is boolean true
         */
        public boolean isTrue() {
            return JSType.toBoolean(value);
        }

        @Override
        public boolean isAlwaysFalse() {
            return !isTrue();
        }

        @Override
        public boolean isAlwaysTrue() {
            return isTrue();
        }

        @Override
        public String getPropertyName() {
            return String.valueOf(getObject());
        }
    }

    private static final class BooleanLiteralNode extends PrimitiveLiteralNode<Boolean> {

        private BooleanLiteralNode(final long token, final int finish, final boolean value) {
            super(Token.recast(token, value ? TokenType.TRUE : TokenType.FALSE), finish, value);
        }

        private BooleanLiteralNode(final BooleanLiteralNode literalNode) {
            super(literalNode);
        }

        @Override
        public boolean isTrue() {
            return value;
        }
    }

    /**
     * Create a new boolean literal
     *
     * @param token token
     * @param finish finish
     * @param value true or false
     *
     * @return the new literal node
     */
    public static LiteralNode<Boolean> newInstance(final long token, final int finish, final boolean value) {
        return new BooleanLiteralNode(token, finish, value);
    }

    private static final class NumberLiteralNode extends PrimitiveLiteralNode<Number> {
        private final Function<Number, String> toStringConverter;

        private NumberLiteralNode(final long token, final int finish, final Number value, final Function<Number, String> toStringConverter) {
            super(Token.recast(token, TokenType.DECIMAL), finish, value);
            this.toStringConverter = toStringConverter;
        }

        private NumberLiteralNode(final NumberLiteralNode literalNode) {
            super(literalNode);
            this.toStringConverter = literalNode.toStringConverter;
        }

        @Override
        public String getPropertyName() {
            return toStringConverter == null ? super.getPropertyName() : toStringConverter.apply(getValue());
        }
    }

    /**
     * Create a new number literal
     *
     * @param token token
     * @param finish finish
     * @param value literal value
     *
     * @return the new literal node
     */
    public static LiteralNode<Number> newInstance(final long token, final int finish, final Number value) {
        return new NumberLiteralNode(token, finish, value, null);
    }

    /**
     * Create a new number literal
     *
     * @param token token
     * @param finish finish
     * @param value literal value
     *
     * @return the new literal node
     */
    public static LiteralNode<Number> newInstance(final long token, final int finish, final Number value, final Function<Number, String> toStringConverter) {
        return new NumberLiteralNode(token, finish, value, toStringConverter);
    }

    private static final class StringLiteralNode extends PrimitiveLiteralNode<String> {
        private StringLiteralNode(final long token, final int finish, final String value) {
            super(Token.recast(token, TokenType.STRING), finish, value);
        }

        @Override
        public void toString(final StringBuilder sb, final boolean printType) {
            sb.append('\"');
            sb.append(value);
            sb.append('\"');
        }
    }

    /**
     * Create a new string literal
     *
     * @param token token
     * @param finish finish
     * @param value string value
     *
     * @return the new literal node
     */
    public static LiteralNode<String> newInstance(final long token, final int finish, final String value) {
        long tokenWithDelimiter = Token.withDelimiter(token);
        int newFinish = Token.descPosition(tokenWithDelimiter) + Token.descLength(tokenWithDelimiter);
        return new StringLiteralNode(tokenWithDelimiter, newFinish, value);
    }

    private static final class LexerTokenLiteralNode extends LiteralNode<LexerToken> {
        private LexerTokenLiteralNode(final long token, final int finish, final LexerToken value) {
            super(Token.recast(token, TokenType.STRING), finish, value);
            // TODO is string the correct token type here?
        }

        private LexerTokenLiteralNode(final LexerTokenLiteralNode literalNode) {
            super(literalNode);
        }

        @Override
        public void toString(final StringBuilder sb, final boolean printType) {
            sb.append(value.toString());
        }
    }

    /**
     * Create a new literal node for a lexer token
     *
     * @param token token
     * @param finish finish
     * @param value lexer token value
     *
     * @return the new literal node
     */
    public static LiteralNode<LexerToken> newInstance(final long token, final int finish, final LexerToken value) {
        return new LexerTokenLiteralNode(token, finish, value);
    }

    private static final class NullLiteralNode extends PrimitiveLiteralNode<Object> {

        private NullLiteralNode(final long token, final int finish) {
            super(Token.recast(token, TokenType.OBJECT), finish, null);
        }
    }

    /**
     * Array literal node class.
     */
    public static final class ArrayLiteralNode extends LiteralNode<Expression[]> implements LexicalContextNode {
        private final boolean hasSpread;
        private final boolean hasTrailingComma;
        private final boolean hasCoverInitializedName;

        /**
         * Constructor
         *
         * @param token token
         * @param finish finish
         * @param value array literal value, a Node array
         */
        protected ArrayLiteralNode(final long token, final int finish, final Expression[] value) {
            this(token, finish, value, false, false, false);
        }

        /**
         * Constructor
         *
         * @param token token
         * @param finish finish
         * @param value array literal value, a Node array
         * @param hasSpread true if the array has a spread element
         * @param hasTrailingComma true if the array literal has a comma after the last element
         */
        protected ArrayLiteralNode(final long token, final int finish, final Expression[] value, boolean hasSpread, boolean hasTrailingComma, boolean hasCoverInitializedName) {
            super(Token.recast(token, TokenType.ARRAY), finish, value);
            this.hasSpread = hasSpread;
            this.hasTrailingComma = hasTrailingComma;
            this.hasCoverInitializedName = hasCoverInitializedName;
        }

        /**
         * Copy constructor
         *
         * @param node source array literal node
         */
        private ArrayLiteralNode(final ArrayLiteralNode node, final Expression[] value) {
            super(node, value);
            this.hasSpread = node.hasSpread;
            this.hasTrailingComma = node.hasTrailingComma;
            this.hasCoverInitializedName = node.hasCoverInitializedName;
        }

        @Override
        public boolean isArray() {
            return true;
        }

        public boolean hasSpread() {
            return hasSpread;
        }

        public boolean hasTrailingComma() {
            return hasTrailingComma;
        }

        public boolean hasCoverInitializedName() {
            return hasCoverInitializedName;
        }

        /**
         * Returns a list of array element expressions. Note that empty array elements manifest
         * themselves as null.
         *
         * @return a list of array element expressions.
         */
        @Override
        public List<Expression> getElementExpressions() {
            return Collections.unmodifiableList(Arrays.asList(value));
        }

        @Override
        public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
            return LexicalContextNode.super.accept(visitor);
        }

        @Override
        public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
            return LexicalContextNode.super.accept(visitor);
        }

        @Override
        public Node accept(final LexicalContext lc, final NodeVisitor<? extends LexicalContext> visitor) {
            if (visitor.enterLiteralNode(this)) {
                final List<Expression> oldValue = Arrays.asList(value);
                final List<Expression> newValue = Node.accept(visitor, oldValue);
                return visitor.leaveLiteralNode(oldValue != newValue ? setValue(lc, newValue) : this);
            }
            return this;
        }

        @Override
        public <R> R accept(LexicalContext lc, TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
            return visitor.enterLiteralNode(this);
        }

        private ArrayLiteralNode setValue(final LexicalContext lc, final Expression[] value) {
            if (this.value == value) {
                return this;
            }
            return Node.replaceInLexicalContext(lc, this, new ArrayLiteralNode(this, value));
        }

        private ArrayLiteralNode setValue(final LexicalContext lc, final List<Expression> value) {
            return setValue(lc, value.toArray(new Expression[value.size()]));
        }

        @Override
        public void toString(final StringBuilder sb, final boolean printType) {
            sb.append('[');
            boolean first = true;
            for (final Node node : value) {
                if (!first) {
                    sb.append(',');
                    sb.append(' ');
                }
                if (node == null) {
                    sb.append("undefined");
                } else {
                    node.toString(sb, printType);
                }
                first = false;
            }
            sb.append(']');
        }
    }

    /**
     * Create a new array literal of Nodes from a list of Node values
     *
     * @param token token
     * @param finish finish
     * @param value literal value list
     *
     * @return the new literal node
     */
    public static LiteralNode<Expression[]> newInstance(final long token, final int finish, final List<Expression> value) {
        return newInstance(token, finish, valueToArray(value));
    }

    /**
     * Create a new array literal of Nodes from a list of Node values
     *
     * @param token token
     * @param finish finish
     * @param value literal value list
     * @param hasSpread true if the array has a spread element
     * @param hasTrailingComma true if the array literal has a comma after the last element
     *
     * @return the new literal node
     */
    public static LiteralNode<Expression[]> newInstance(long token, int finish, List<Expression> value, boolean hasSpread, boolean hasTrailingComma, boolean hasCoverInitializedName) {
        return new ArrayLiteralNode(token, finish, valueToArray(value), hasSpread, hasTrailingComma, hasCoverInitializedName);
    }

    /**
     * Create a new array literal of Nodes
     *
     * @param token token
     * @param finish finish
     * @param value literal value array
     *
     * @return the new literal node
     */
    public static LiteralNode<Expression[]> newInstance(final long token, final int finish, final Expression[] value) {
        return new ArrayLiteralNode(token, finish, value);
    }
}
