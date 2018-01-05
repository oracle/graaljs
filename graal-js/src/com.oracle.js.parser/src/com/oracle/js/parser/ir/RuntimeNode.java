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

import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation for a runtime call.
 */
public class RuntimeNode extends Expression {

    /**
     * Request enum used for meta-information about the runtime request
     */
    public enum Request {
        /** ReferenceError type */
        REFERENCE_ERROR,
        /** ToString conversion */
        TO_STRING(TokenType.VOID, Object.class, 1),
        /** Get template object from raw and cooked string arrays. */
        GET_TEMPLATE_OBJECT(TokenType.TEMPLATE, Object.class, 2);

        /** token type */
        private final TokenType tokenType;

        /** return type for request */
        private final Class<?> returnType;

        /** arity of request */
        private final int arity;

        Request() {
            this(TokenType.VOID, Object.class, 0);
        }

        Request(final TokenType tokenType, final Class<?> returnType, final int arity) {
            this.tokenType = tokenType;
            this.returnType = returnType;
            this.arity = arity;
        }

        /**
         * Get arity
         *
         * @return the arity of the request
         */
        public int getArity() {
            return arity;
        }

        /**
         * Get the return type
         *
         * @return return type for request
         */
        public Class<?> getReturnType() {
            return returnType;
        }

        /**
         * Get token type
         *
         * @return token type for request
         */
        public TokenType getTokenType() {
            return tokenType;
        }
    }

    /** Runtime request. */
    private final Request request;

    /** Call arguments. */
    private final List<Expression> args;

    /**
     * Constructor
     *
     * @param token token
     * @param finish finish
     * @param request the request
     * @param args arguments to request
     */
    public RuntimeNode(final long token, final int finish, final Request request, final List<Expression> args) {
        super(token, finish);

        this.request = request;
        this.args = args;
    }

    private RuntimeNode(final RuntimeNode runtimeNode, final Request request, final List<Expression> args) {
        super(runtimeNode);

        this.request = request;
        this.args = args;
    }

    /**
     * Constructor
     *
     * @param token token
     * @param finish finish
     * @param request the request
     * @param args arguments to request
     */
    public RuntimeNode(final long token, final int finish, final Request request, final Expression... args) {
        this(token, finish, request, Arrays.asList(args));
    }

    /**
     * Constructor
     *
     * @param parent parent node from which to inherit source, token, finish
     * @param request the request
     * @param args arguments to request
     */
    public RuntimeNode(final Expression parent, final Request request, final List<Expression> args) {
        super(parent);

        this.request = request;
        this.args = args;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterRuntimeNode(this)) {
            return visitor.leaveRuntimeNode(setArgs(Node.accept(visitor, args)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterRuntimeNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("Runtime.");
        sb.append(request);
        sb.append('(');

        boolean first = true;

        for (final Node arg : args) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }

            arg.toString(sb, printType);
        }

        sb.append(')');
    }

    /**
     * Get the arguments for this runtime node
     *
     * @return argument list
     */
    public List<Expression> getArgs() {
        return Collections.unmodifiableList(args);
    }

    /**
     * Set the arguments of this runtime node
     *
     * @param args new arguments
     * @return new runtime node, or identical if no change
     */
    public RuntimeNode setArgs(final List<Expression> args) {
        if (this.args == args) {
            return this;
        }
        return new RuntimeNode(this, request, args);
    }

    /**
     * Get the request that this runtime node implements
     *
     * @return the request
     */
    public Request getRequest() {
        return request;
    }
}
