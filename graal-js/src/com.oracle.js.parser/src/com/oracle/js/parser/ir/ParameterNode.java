/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * IR representation of a positional parameter value. Used for desugaring parameter initialization.
 */
public final class ParameterNode extends Expression {
    private final int index;
    private final boolean rest;

    public ParameterNode(final long token, final int finish, final int index, final boolean rest) {
        super(token, finish);
        this.index = index;
        this.rest = rest;
    }

    public ParameterNode(final long token, final int finish, final int index) {
        this(token, finish, index, false);
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterParameterNode(this)) {
            return visitor.leaveParameterNode(this);
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterParameterNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        if (!isRestParameter()) {
            sb.append("arguments[").append(index).append("]");
        } else {
            sb.append("arguments.slice(").append(index).append(")");
        }
    }

    /**
     * Formal parameter index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * If true, this is a rest parameter.
     */
    public boolean isRestParameter() {
        return rest;
    }
}
