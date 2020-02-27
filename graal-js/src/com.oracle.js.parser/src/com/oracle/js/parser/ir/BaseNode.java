/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.js.parser.TokenType;

/**
 * IR base for accessing/indexing nodes.
 *
 * @see AccessNode
 * @see IndexNode
 */
public abstract class BaseNode extends OptionalExpression implements FunctionCall {

    /** Base Node. */
    protected final Expression base;

    /** Super property access. */
    private final boolean isSuper;

    /** Optional access or call ({@code a?.b}, {@code a?.[b]}, or {@code a?.()}). */
    private final boolean optional;

    /** Is this access part of an optional chain. */
    private final boolean optionalChain;

    private boolean isFunction;

    /**
     * Constructor
     *
     * @param token token
     * @param finish finish
     * @param base base node
     * @param isSuper is this a super property access
     */
    public BaseNode(final long token, final int finish, final Expression base, final boolean isSuper, final boolean optional, final boolean optionalChain) {
        super(token, base.getStart(), finish);
        this.base = base;
        this.isSuper = isSuper;
        this.optional = optional;
        this.optionalChain = optionalChain;
        assert !(isSuper && optional) && (!optional || optionalChain);
    }

    /**
     * Copy constructor for immutable nodes
     *
     * @param baseNode node to inherit from
     * @param base base
     * @param isSuper is this a super property access
     */
    protected BaseNode(final BaseNode baseNode, final Expression base, final boolean isSuper, final boolean optional, final boolean optionalChain) {
        super(baseNode);
        this.base = base;
        this.isSuper = isSuper;
        this.optional = optional;
        this.optionalChain = optionalChain;
        this.isFunction = baseNode.isFunction;
    }

    /**
     * Get the base node for this access
     *
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
     * @return {@code true} if this is a {@code super} property access.
     */
    public boolean isSuper() {
        return isSuper;
    }

    /**
     * Return true if this node represents an index operation normally represented as
     * {@link IndexNode}.
     *
     * @return true if an index access.
     */
    public boolean isIndex() {
        return isTokenType(TokenType.LBRACKET);
    }

    /**
     * Mark this node as being the callee operand of a {@link CallNode}.
     *
     * @return a base node identical to this one in all aspects except with its function flag set.
     */
    final BaseNode setIsFunction() {
        this.isFunction = true;
        return this;
    }

    /**
     * Mark this node as being a SuperProperty access.
     */
    public abstract BaseNode setIsSuper();

    /**
     * Returns {@code true} if this is an optional property access ({@code a?.b} or {@code a?.[b]}).
     */
    @Override
    public final boolean isOptional() {
        return optional;
    }

    @Override
    public final boolean isOptionalChain() {
        return optionalChain;
    }
}
