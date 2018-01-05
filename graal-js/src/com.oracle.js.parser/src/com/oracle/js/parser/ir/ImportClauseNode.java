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

public class ImportClauseNode extends Node {

    private final IdentNode defaultBinding;

    private final NameSpaceImportNode nameSpaceImport;

    private final NamedImportsNode namedImports;

    public ImportClauseNode(long token, int start, int finish, final IdentNode defaultBinding) {
        this(token, start, finish, defaultBinding, null, null);
    }

    public ImportClauseNode(long token, int start, int finish, final NameSpaceImportNode nameSpaceImport) {
        this(token, start, finish, null, nameSpaceImport, null);
    }

    public ImportClauseNode(long token, int start, int finish, final NamedImportsNode namedImportsNode) {
        this(token, start, finish, null, null, namedImportsNode);
    }

    public ImportClauseNode(long token, int start, int finish, final IdentNode defaultBinding, final NameSpaceImportNode nameSpaceImport) {
        this(token, start, finish, defaultBinding, nameSpaceImport, null);
    }

    public ImportClauseNode(long token, int start, int finish, final IdentNode defaultBinding, final NamedImportsNode namedImports) {
        this(token, start, finish, defaultBinding, null, namedImports);
    }

    private ImportClauseNode(long token, int start, int finish, final IdentNode defaultBinding, final NameSpaceImportNode nameSpaceImport, final NamedImportsNode namedImports) {
        super(token, start, finish);
        this.defaultBinding = defaultBinding;
        this.nameSpaceImport = nameSpaceImport;
        this.namedImports = namedImports;
    }

    private ImportClauseNode(final ImportClauseNode node, final IdentNode defaultBinding, final NameSpaceImportNode nameSpaceImport, final NamedImportsNode namedImports) {
        super(node);
        this.defaultBinding = defaultBinding;
        this.nameSpaceImport = nameSpaceImport;
        this.namedImports = namedImports;
    }

    public IdentNode getDefaultBinding() {
        return defaultBinding;
    }

    public NameSpaceImportNode getNameSpaceImport() {
        return nameSpaceImport;
    }

    public NamedImportsNode getNamedImports() {
        return namedImports;
    }

    public ImportClauseNode setDefaultBinding(IdentNode defaultBinding) {
        if (this.defaultBinding == defaultBinding) {
            return this;
        }
        return new ImportClauseNode(this, defaultBinding, nameSpaceImport, namedImports);
    }

    public ImportClauseNode setNameSpaceImport(NameSpaceImportNode nameSpaceImport) {
        if (this.nameSpaceImport == nameSpaceImport) {
            return this;
        }
        return new ImportClauseNode(this, defaultBinding, nameSpaceImport, namedImports);
    }

    public ImportClauseNode setNamedImports(NamedImportsNode namedImports) {
        if (this.namedImports == namedImports) {
            return this;
        }
        return new ImportClauseNode(this, defaultBinding, nameSpaceImport, namedImports);
    }

    @Override
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterImportClauseNode(this)) {
            IdentNode newDefaultBinding = defaultBinding == null ? null
                            : (IdentNode) defaultBinding.accept(visitor);
            NameSpaceImportNode newNameSpaceImport = nameSpaceImport == null ? null
                            : (NameSpaceImportNode) nameSpaceImport.accept(visitor);
            NamedImportsNode newNamedImports = namedImports == null ? null
                            : (NamedImportsNode) namedImports.accept(visitor);
            return visitor.leaveImportClauseNode(
                            setDefaultBinding(newDefaultBinding).setNameSpaceImport(newNameSpaceImport).setNamedImports(newNamedImports));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterImportClauseNode(this);
    }

    @Override
    public void toString(StringBuilder sb, boolean printType) {
        if (defaultBinding != null) {
            defaultBinding.toString(sb, printType);
            if (nameSpaceImport != null || namedImports != null) {
                sb.append(',');
            }
        }

        if (nameSpaceImport != null) {
            nameSpaceImport.toString(sb, printType);
        } else if (namedImports != null) {
            namedImports.toString(sb, printType);
        }

    }
}
