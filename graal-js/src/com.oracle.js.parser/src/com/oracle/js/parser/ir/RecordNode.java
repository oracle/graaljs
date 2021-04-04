/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.List;

/**
 * IR representation of an record literal.
 */
public final class RecordNode extends Expression {

    /**
     * Literal elements.
     */
    private final List<RecordPropertyNode> elements;

    /**
     * Constructor
     *
     * @param token    token
     * @param finish   finish
     * @param elements the elements used to initialize this ObjectNode
     */
    public RecordNode(final long token, final int finish, final List<RecordPropertyNode> elements) {
        super(token, finish);
        this.elements = elements;
    }

    private RecordNode(final RecordNode objectNode, final List<RecordPropertyNode> elements) {
        super(objectNode);
        this.elements = elements;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterRecordNode(this)) {
            return visitor.leaveRecordNode(setElements(Node.accept(visitor, elements)));
        }
        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterRecordNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("#{");
        if (!elements.isEmpty()) {
            sb.append(' ');
            boolean first = true;
            for (final Node element : elements) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                element.toString(sb, printType);
            }
            sb.append(' ');
        }
        sb.append('}');
    }

    /**
     * Get the elements of this literal node
     *
     * @return a list of elements
     */
    public List<RecordPropertyNode> getElements() {
        return Collections.unmodifiableList(elements);
    }

    private RecordNode setElements(final List<RecordPropertyNode> elements) {
        if (this.elements == elements) {
            return this;
        }
        return new RecordNode(this, elements);
    }
}
