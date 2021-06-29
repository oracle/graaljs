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

import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation of an record literal property.
 */
public final class RecordPropertyNode extends Node {

    private final Expression key;
    private final Expression value;
    private final boolean computed;

    /**
     * Constructor
     *
     * @param token    token
     * @param finish   finish
     * @param key      the key of this property
     * @param value    the value of this property
     * @param computed true if its key is computed
     */
    public RecordPropertyNode(long token, int finish, Expression key, Expression value, boolean computed) {
        super(token, finish);
        this.key = key;
        this.value = value;
        this.computed = computed;
    }

    private RecordPropertyNode(RecordPropertyNode propertyNode, Expression key, Expression value, boolean computed) {
        super(propertyNode);
        this.key = key;
        this.value = value;
        this.computed = computed;
    }

    /**
     * Get the name of the property key
     *
     * @return key name
     */
    public String getKeyName() {
        return key instanceof PropertyKey ? ((PropertyKey) key).getPropertyName() : null;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterPropertyNode(this)) {
            return visitor.leavePropertyNode(this
                    .setKey((Expression) key.accept(visitor))
                    .setValue(value == null ? null : (Expression) value.accept(visitor))
            );
        }
        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterPropertyNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        if (computed) {
            sb.append('[');
        }
        key.toString(sb, printType);
        if (computed) {
            sb.append(']');
        }
        if (value != null) {
            sb.append(": ");
            value.toString(sb, printType);
        }
    }

    /**
     * Return the key for this property node
     *
     * @return the key
     */
    public Expression getKey() {
        return key;
    }

    private RecordPropertyNode setKey(final Expression key) {
        if (this.key == key) {
            return this;
        }
        return new RecordPropertyNode(this, key, value, computed);
    }

    /**
     * Get the value of this property
     *
     * @return property value
     */
    public Expression getValue() {
        return value;
    }

    private RecordPropertyNode setValue(final Expression value) {
        if (this.value == value) {
            return this;
        }
        return new RecordPropertyNode(this, key, value, computed);
    }

    public boolean isComputed() {
        return computed;
    }

    public boolean isSpread() {
        return key != null && key.isTokenType(TokenType.SPREAD_RECORD);
    }
}
