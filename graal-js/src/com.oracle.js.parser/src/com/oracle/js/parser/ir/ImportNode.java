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

public class ImportNode extends Node {

    private final LiteralNode<String> moduleSpecifier;

    private final ImportClauseNode importClause;

    private final FromNode from;

    public ImportNode(final long token, final int start, final int finish, final LiteralNode<String> moduleSpecifier) {
        this(token, start, finish, moduleSpecifier, null, null);
    }

    public ImportNode(final long token, final int start, final int finish, final ImportClauseNode importClause, final FromNode from) {
        this(token, start, finish, null, importClause, from);
    }

    private ImportNode(final long token, final int start, final int finish, final LiteralNode<String> moduleSpecifier, ImportClauseNode importClause, FromNode from) {
        super(token, start, finish);
        this.moduleSpecifier = moduleSpecifier;
        this.importClause = importClause;
        this.from = from;
    }

    private ImportNode(final ImportNode node, final LiteralNode<String> moduleSpecifier, ImportClauseNode importClause, FromNode from) {
        super(node);
        this.moduleSpecifier = moduleSpecifier;
        this.importClause = importClause;
        this.from = from;
    }

    public LiteralNode<String> getModuleSpecifier() {
        return moduleSpecifier;
    }

    public ImportClauseNode getImportClause() {
        return importClause;
    }

    public FromNode getFrom() {
        return from;
    }

    public ImportNode setModuleSpecifier(LiteralNode<String> moduleSpecifier) {
        if (this.moduleSpecifier == moduleSpecifier) {
            return this;
        }
        return new ImportNode(this, moduleSpecifier, importClause, from);
    }

    public ImportNode setImportClause(ImportClauseNode importClause) {
        if (this.importClause == importClause) {
            return this;
        }
        return new ImportNode(this, moduleSpecifier, importClause, from);
    }

    public ImportNode setFrom(FromNode from) {
        if (this.from == from) {
            return this;
        }
        return new ImportNode(this, moduleSpecifier, importClause, from);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterImportNode(this)) {
            LiteralNode<String> newModuleSpecifier = moduleSpecifier == null ? null
                            : (LiteralNode<String>) moduleSpecifier.accept(visitor);
            ImportClauseNode newImportClause = importClause == null ? null
                            : (ImportClauseNode) importClause.accept(visitor);
            FromNode newFrom = from == null ? null
                            : (FromNode) from.accept(visitor);
            return visitor.leaveImportNode(
                            setModuleSpecifier(newModuleSpecifier).setImportClause(newImportClause).setFrom(newFrom));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterImportNode(this);
    }

    @Override
    public void toString(StringBuilder sb, boolean printType) {
        sb.append("import");
        sb.append(' ');
        if (moduleSpecifier != null) {
            moduleSpecifier.toString(sb, printType);
        } else {
            importClause.toString(sb, printType);
            sb.append(' ');
            from.toString(sb, printType);
        }
        sb.append(';');
    }

}
