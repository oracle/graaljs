/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser.ir;

import java.util.List;

import com.oracle.js.parser.Token;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * Template string literal.
 */
public abstract class TemplateLiteralNode extends Expression {

    protected TemplateLiteralNode(final long token, final int finish) {
        super(token, finish);
    }

    protected TemplateLiteralNode(final TemplateLiteralNode literalNode) {
        super(literalNode);
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterTemplateLiteralNode(this);
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterTemplateLiteralNode(this)) {
            return visitor.leaveTemplateLiteralNode(this);
        }
        return this;
    }

    public static class TaggedTemplateLiteralNode extends TemplateLiteralNode {
        private final List<Expression> rawStrings;
        private final List<Expression> cookedStrings;

        protected TaggedTemplateLiteralNode(long token, int finish, List<Expression> rawStrings, List<Expression> cookedStrings) {
            super(token, finish);
            this.rawStrings = List.copyOf(rawStrings);
            this.cookedStrings = List.copyOf(cookedStrings);
        }

        public List<Expression> getRawStrings() {
            return rawStrings;
        }

        public List<Expression> getCookedStrings() {
            return cookedStrings;
        }

        @Override
        public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
            if (visitor.enterTemplateLiteralNode(this)) {
                return visitor.leaveTemplateLiteralNode(this);
            }
            return this;
        }

        @Override
        public void toString(final StringBuilder sb, final boolean printType) {
            sb.append('`');
            for (int i = 0; i < rawStrings.size(); i++) {
                Expression expression = rawStrings.get(i);
                if (expression instanceof LiteralNode<?>) {
                    sb.append(((LiteralNode<?>) expression).getString());
                } else {
                    expression.toString(sb, printType);
                }
                if (i < rawStrings.size() - 1) {
                    sb.append("${");
                    sb.append(i);
                    sb.append("}");
                }
            }
            sb.append('`');
        }
    }

    public static class UntaggedTemplateLiteralNode extends TemplateLiteralNode {
        private final List<Expression> expressions;

        protected UntaggedTemplateLiteralNode(long token, int finish, List<Expression> expressions) {
            super(token, finish);
            assert verifyStringLiterals(expressions);
            this.expressions = List.copyOf(expressions);
        }

        public UntaggedTemplateLiteralNode(UntaggedTemplateLiteralNode literalNode, List<Expression> expressions) {
            super(literalNode);
            this.expressions = expressions;
        }

        public List<Expression> getExpressions() {
            return expressions;
        }

        @Override
        public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
            if (visitor.enterTemplateLiteralNode(this)) {
                final List<Expression> newExpressions = Node.accept(visitor, expressions);
                return visitor.leaveTemplateLiteralNode(expressions != newExpressions ? new UntaggedTemplateLiteralNode(this, newExpressions) : this);
            }
            return this;
        }

        @Override
        public void toString(final StringBuilder sb, final boolean printType) {
            sb.append('`');
            for (int i = 0; i < expressions.size(); i++) {
                Expression expression = expressions.get(i);
                if (i % 2 == 0) {
                    sb.append(((LiteralNode<?>) expression).getString());
                } else {
                    sb.append("${");
                    expression.toString(sb, printType);
                    sb.append("}");
                }
            }
            sb.append('`');
        }

        private static boolean verifyStringLiterals(List<Expression> expressions) {
            for (int i = 0; i < expressions.size(); i++) {
                if (i % 2 == 0) {
                    Expression expression = expressions.get(i);
                    if (!(expression instanceof LiteralNode<?>)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Create a new tagged template string literal.
     *
     * @param token token
     * @param finish finish
     *
     * @return the new literal node
     */
    public static TemplateLiteralNode newTagged(final long token, final int finish, final List<Expression> rawStrings, final List<Expression> cookedStrings) {
        return new TaggedTemplateLiteralNode(Token.withDelimiter(token), finish, rawStrings, cookedStrings);
    }

    /**
     * Create a new untagged template string literal.
     *
     * @param token token
     * @param finish finish
     * @param expressions interleaved string parts and expressions
     *
     * @return the new template literal node
     */
    public static TemplateLiteralNode newUntagged(final long token, final int finish, final List<Expression> expressions) {
        return new UntaggedTemplateLiteralNode(Token.withDelimiter(token), finish, expressions);
    }
}
