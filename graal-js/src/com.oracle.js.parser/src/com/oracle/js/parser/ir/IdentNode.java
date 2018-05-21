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

import java.util.Objects;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation for an identifier.
 */
public final class IdentNode extends Expression implements PropertyKey, FunctionCall {
    private static final int PROPERTY_NAME     = 1 << 0;
    private static final int INITIALIZED_HERE  = 1 << 1;
    private static final int FUNCTION          = 1 << 2;
    private static final int NEW_TARGET        = 1 << 3;
    private static final int IS_DECLARED_HERE  = 1 << 4;
    private static final int THIS              = 1 << 5;
    private static final int SUPER             = 1 << 6;
    private static final int DIRECT_SUPER      = 1 << 7;
    private static final int REST_PARAMETER    = 1 << 8;
    private static final int CATCH_PARAMETER   = 1 << 9;

    /** Identifier. */
    private final String name;

    private final int flags;

    private Symbol symbol;

    /**
     * Constructor
     *
     * @param token   token
     * @param finish  finish position
     * @param name    name of identifier
     */
    public IdentNode(final long token, final int finish, final String name) {
        super(token, finish);
        this.name = Objects.requireNonNull(name);
        this.flags = 0;
    }

    private IdentNode(final IdentNode identNode, final String name, final int flags) {
        super(identNode);
        this.name = name;
        this.flags = flags;
        this.symbol = identNode.symbol;
    }

    /**
     * Assist in IR navigation.
     *
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterIdentNode(this)) {
            return visitor.leaveIdentNode(this);
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterIdentNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append(name);
    }

    /**
     * Get the name of the identifier
     * @return  IdentNode name
     */
    public String getName() {
        return name;
    }

    @Override
    public String getPropertyName() {
        return getName();
    }

    /**
     * Return the Symbol the compiler has assigned to this identifier. The symbol is a description of the storage
     * location for the identifier.
     *
     * @return the symbol
     */
    public Symbol getSymbol() {
        return symbol;
    }

    /**
     * Check if this IdentNode is a property name
     * @return true if this is a property name
     */
    public boolean isPropertyName() {
        return (flags & PROPERTY_NAME) == PROPERTY_NAME;
    }

    /**
     * Flag this IdentNode as a property name
     * @return a node equivalent to this one except for the requested change.
     */
    public IdentNode setIsPropertyName() {
        if (isPropertyName()) {
            return this;
        }
        return new IdentNode(this, name, flags | PROPERTY_NAME);
    }

    /**
     * Check if this IdentNode is a future strict name
     * @return true if this is a future strict name
     */
    public boolean isFutureStrictName() {
        return tokenType().isFutureStrict();
    }

    /**
     * Helper function for local def analysis.
     * @return true if IdentNode is initialized on creation
     */
    public boolean isInitializedHere() {
        return (flags & INITIALIZED_HERE) == INITIALIZED_HERE;
    }

    /**
     * Flag IdentNode to be initialized on creation
     * @return a node equivalent to this one except for the requested change.
     */
    public IdentNode setIsInitializedHere() {
        if (isInitializedHere()) {
            return this;
        }
        return new IdentNode(this, name, flags | INITIALIZED_HERE);
    }

    /**
     * Is this IdentNode declared here?
     *
     * @return true if identifier is declared here
     */
    public boolean isDeclaredHere() {
        return (flags & IS_DECLARED_HERE) != 0;
    }

    /**
     * Flag this IdentNode as being declared here.
     *
     * @return a new IdentNode equivalent to this but marked as declared here.
     */
    public IdentNode setIsDeclaredHere() {
        if (isDeclaredHere()) {
            return this;
        }
        return new IdentNode(this, name, flags | IS_DECLARED_HERE);
    }

    @Override
    public boolean isFunction() {
        return (flags & FUNCTION) == FUNCTION;
    }

    /**
     * Is this an internal symbol, i.e. one that starts with ':'. Those can
     * never be optimistic.
     * @return true if internal symbol
     */
    public boolean isInternal() {
        assert name != null;
        return name.charAt(0) == ':';
    }

    public boolean isThis() {
        return (flags & THIS) != 0;
    }

    public IdentNode setIsThis() {
        return new IdentNode(this, name, flags | THIS);
    }

    public boolean isSuper() {
        return (flags & SUPER) != 0;
    }

    public IdentNode setIsSuper() {
        return new IdentNode(this, name, flags | SUPER);
    }

    public boolean isDirectSuper() {
        return (flags & DIRECT_SUPER) != 0;
    }

    public IdentNode setIsDirectSuper() {
        return new IdentNode(this, name, flags | SUPER | DIRECT_SUPER);
    }

    public boolean isRestParameter() {
        return (flags & REST_PARAMETER) != 0;
    }

    public IdentNode setIsRestParameter() {
        return new IdentNode(this, name, flags | REST_PARAMETER);
    }

    public boolean isCatchParameter() {
        return (flags & CATCH_PARAMETER) != 0;
    }

    public IdentNode setIsCatchParameter() {
        return new IdentNode(this, name, flags | CATCH_PARAMETER);
    }

    public boolean isNewTarget() {
        return (flags & NEW_TARGET) != 0;
    }

    public IdentNode setIsNewTarget() {
        return new IdentNode(this, name, flags | NEW_TARGET);
    }
}
