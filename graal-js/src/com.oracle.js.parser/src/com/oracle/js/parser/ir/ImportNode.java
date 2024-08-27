/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.Objects;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;
import com.oracle.truffle.api.strings.TruffleString;

public class ImportNode extends Node {

    private final LiteralNode<TruffleString> moduleSpecifier;

    private final ImportClauseNode importClause;

    private final Map<TruffleString, TruffleString> attributes;

    public ImportNode(long token, int start, int finish, LiteralNode<TruffleString> moduleSpecifier,
                    Map<TruffleString, TruffleString> attributes) {
        this(token, start, finish, moduleSpecifier, null, attributes);
    }

    public ImportNode(long token, int start, int finish, ImportClauseNode importClause, LiteralNode<TruffleString> moduleSpecifier,
                    Map<TruffleString, TruffleString> attributes) {
        this(token, start, finish, moduleSpecifier, importClause, attributes);
    }

    private ImportNode(long token, int start, int finish, LiteralNode<TruffleString> moduleSpecifier, ImportClauseNode importClause, Map<TruffleString, TruffleString> attributes) {
        super(token, start, finish);
        this.moduleSpecifier = Objects.requireNonNull(moduleSpecifier);
        this.importClause = importClause;
        this.attributes = Objects.requireNonNull(attributes);
    }

    private ImportNode(final ImportNode node, final LiteralNode<TruffleString> moduleSpecifier, ImportClauseNode importClause) {
        super(node);
        this.moduleSpecifier = Objects.requireNonNull(moduleSpecifier);
        this.importClause = importClause;
        this.attributes = Objects.requireNonNull(node.attributes);
    }

    public LiteralNode<TruffleString> getModuleSpecifier() {
        return moduleSpecifier;
    }

    public ImportClauseNode getImportClause() {
        return importClause;
    }

    public Map<TruffleString, TruffleString> getAttributes() {
        return attributes;
    }

    public ImportNode setModuleSpecifier(LiteralNode<TruffleString> moduleSpecifier) {
        if (this.moduleSpecifier == moduleSpecifier) {
            return this;
        }
        return new ImportNode(this, moduleSpecifier, importClause);
    }

    public ImportNode setImportClause(ImportClauseNode importClause) {
        if (this.importClause == importClause) {
            return this;
        }
        return new ImportNode(this, moduleSpecifier, importClause);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterImportNode(this)) {
            LiteralNode<TruffleString> newModuleSpecifier = (LiteralNode<TruffleString>) moduleSpecifier.accept(visitor);
            ImportClauseNode newImportClause = importClause == null ? null : (ImportClauseNode) importClause.accept(visitor);
            return visitor.leaveImportNode(setModuleSpecifier(newModuleSpecifier).setImportClause(newImportClause));
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
        if (importClause != null) {
            importClause.toString(sb, printType);
            sb.append(' ');
            sb.append("from");
            sb.append(' ');
        }
        moduleSpecifier.toString(sb, printType);
        if (!attributes.isEmpty()) {
            sb.append(" with ");
            attributesToString(attributes, sb);
        }
        sb.append(';');
    }

    static void attributesToString(Map<TruffleString, TruffleString> attributes, StringBuilder sb) {
        sb.append("{");
        for (var iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
            var attr = iterator.next();
            sb.append(attr.getKey());
            sb.append(": ");
            sb.append('"').append(attr.getValue()).append('"');
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
    }
}
