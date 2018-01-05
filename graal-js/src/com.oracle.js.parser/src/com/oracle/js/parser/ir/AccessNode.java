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

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation of a property access (period operator.)
 */
public final class AccessNode extends BaseNode {
    /** Property name. */
    private final String property;

    /**
     * Constructor
     *
     * @param token     token
     * @param finish    finish
     * @param base      base node
     * @param property  property
     */
    public AccessNode(final long token, final int finish, final Expression base, final String property) {
        super(token, finish, base, false, false);
        this.property = property;
    }

    private AccessNode(final AccessNode accessNode, final Expression base, final String property, final boolean isFunction, final boolean isSuper) {
        super(accessNode, base, isFunction, isSuper);
        this.property = property;
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterAccessNode(this)) {
            return visitor.leaveAccessNode(
                setBase((Expression)base.accept(visitor)));
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
