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

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation of a property access (period operator.)
 */
public final class AccessNode extends BaseNode {
    /** Property name. */
    private final String property;

    /** Private member access. */
    private final boolean isPrivate;

    /**
     * Constructor
     *
     * @param token token
     * @param finish finish
     * @param base base node
     * @param property property
     */
    public AccessNode(final long token, final int finish, final Expression base, final String property, final boolean isSuper, final boolean isPrivate) {
        super(token, finish, base, false, isSuper);
        this.property = property;
        this.isPrivate = isPrivate;
        assert !(isSuper && isPrivate);
    }

    public AccessNode(final long token, final int finish, final Expression base, final String property) {
        this(token, finish, base, property, false, false);
    }

    private AccessNode(final AccessNode accessNode, final Expression base, final String property, final boolean isFunction, final boolean isSuper) {
        super(accessNode, base, isFunction, isSuper);
        this.property = property;
        this.isPrivate = accessNode.isPrivate;
    }

    /**
     * Assist in IR navigation.
     *
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterAccessNode(this)) {
            return visitor.leaveAccessNode(setBase((Expression) base.accept(visitor)));
        }
        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterAccessNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final boolean needsParen = tokenType().needsParens(getBase().tokenType(), true);

        if (needsParen) {
            sb.append('(');
        }

        base.toString(sb, printType);

        if (needsParen) {
            sb.append(')');
        }
        sb.append('.');

        sb.append(property);
    }

    /**
     * Get the property name
     *
     * @return the property name
     */
    public String getProperty() {
        return property;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getPrivateName() {
        assert isPrivate();
        return property;
    }

    private AccessNode setBase(final Expression base) {
        if (this.base == base) {
            return this;
        }
        return new AccessNode(this, base, property, isFunction(), isSuper());
    }

    @Override
    public AccessNode setIsFunction() {
        if (isFunction()) {
            return this;
        }
        return new AccessNode(this, base, property, true, isSuper());
    }

    @Override
    public AccessNode setIsSuper() {
        if (isSuper()) {
            return this;
        }
        return new AccessNode(this, base, property, isFunction(), true);
    }
}
