/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

public class NameSpaceImportNode extends Node {

    private final IdentNode bindingIdentifier;

    public NameSpaceImportNode(final long token, final int start, final int finish, final IdentNode bindingIdentifier) {
        super(token, start, finish);
        this.bindingIdentifier = bindingIdentifier;
    }

    private NameSpaceImportNode(final NameSpaceImportNode node, final IdentNode bindingIdentifier) {
        super(node);
        this.bindingIdentifier = bindingIdentifier;
    }

    public IdentNode getBindingIdentifier() {
        return bindingIdentifier;
    }

    public NameSpaceImportNode setBindingIdentifier(IdentNode bindingIdentifier) {
        if (this.bindingIdentifier == bindingIdentifier) {
            return this;
        }
        return new NameSpaceImportNode(this, bindingIdentifier);
    }

    @Override
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterNameSpaceImportNode(this)) {
            return visitor.leaveNameSpaceImportNode(
                            setBindingIdentifier((IdentNode) bindingIdentifier.accept(visitor)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterNameSpaceImportNode(this);
    }

    @Override
    public void toString(StringBuilder sb, boolean printType) {
        sb.append("* as ");
        bindingIdentifier.toString(sb, printType);
    }

}
