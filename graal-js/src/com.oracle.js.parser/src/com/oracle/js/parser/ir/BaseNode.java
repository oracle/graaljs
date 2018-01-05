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

import com.oracle.js.parser.TokenType;

// @formatter:off
/**
 * IR base for accessing/indexing nodes.
 *
 * @see AccessNode
 * @see IndexNode
 */
public abstract class BaseNode extends Expression implements FunctionCall {

    /** Base Node. */
    protected final Expression base;

    private final boolean isFunction;

    /** Super property access. */
    private final boolean isSuper;

    /**
     * Constructor
     *
     * @param token  token
     * @param finish finish
     * @param base   base node
     * @param isFunction is this a function
     * @param isSuper is this a super property access
     */
    public BaseNode(final long token, final int finish, final Expression base, final boolean isFunction, final boolean isSuper) {
        super(token, base.getStart(), finish);
        this.base           = base;
        this.isFunction     = isFunction;
        this.isSuper        = isSuper;
    }

    /**
     * Copy constructor for immutable nodes
     * @param baseNode node to inherit from
     * @param base base
     * @param isFunction is this a function
     * @param isSuper is this a super property access
     */
    protected BaseNode(final BaseNode baseNode, final Expression base, final boolean isFunction, final boolean isSuper) {
        super(baseNode);
        this.base           = base;
        this.isFunction     = isFunction;
        this.isSuper        = isSuper;
    }

    /**
     * Get the base node for this access
     * @return the base node
     */
    public Expression getBase() {
        return base;
    }

    @Override
    public boolean isFunction() {
        return isFunction;
    }

    /**
     * @return {@code true} if a SuperProperty access.
     */
    public boolean isSuper() {
        return isSuper;
    }

    /**
     * Return true if this node represents an index operation normally represented as {@link IndexNode}.
     * @return true if an index access.
     */
    public boolean isIndex() {
        return isTokenType(TokenType.LBRACKET);
    }

    /**
     * Mark this node as being the callee operand of a {@link CallNode}.
     * @return a base node identical to this one in all aspects except with its function flag set.
     */
    public abstract BaseNode setIsFunction();

    /**
     * Mark this node as being a SuperProperty access.
     */
    public abstract BaseNode setIsSuper();
}
