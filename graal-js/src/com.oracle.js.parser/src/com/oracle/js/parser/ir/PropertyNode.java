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

import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation of an object literal property.
 */
public final class PropertyNode extends Node {

    /** Property key. */
    private final Expression key;

    /** Property value. */
    private final Expression value;

    /** Property getter. */
    private final FunctionNode getter;

    /** Property setter. */
    private final FunctionNode setter;

    private final boolean isStatic;

    private final boolean computed;

    private final boolean coverInitializedName;

    private final boolean proto;

    /**
     * Constructor
     *
     * @param token   token
     * @param finish  finish
     * @param key     the key of this property
     * @param value   the value of this property
     * @param getter  getter function body
     * @param setter  setter function body
     */
    public PropertyNode(long token, int finish, Expression key, Expression value, FunctionNode getter, FunctionNode setter,
                    boolean isStatic, boolean computed, boolean coverInitializedName, boolean proto) {
        super(token, finish);
        this.key = key;
        this.value = value;
        this.getter = getter;
        this.setter = setter;
        this.isStatic = isStatic;
        this.computed = computed;
        this.coverInitializedName = coverInitializedName;
        this.proto = proto;
    }

    private PropertyNode(PropertyNode propertyNode, Expression key, Expression value, FunctionNode getter, FunctionNode setter,
                    boolean isStatic, boolean computed, boolean coverInitializedName, boolean proto) {
        super(propertyNode);
        this.key = key;
        this.value = value;
        this.getter = getter;
        this.setter = setter;
        this.isStatic = isStatic;
        this.computed = computed;
        this.coverInitializedName = coverInitializedName;
        this.proto = proto;
    }

    /**
     * Get the name of the property key
     * @return key name
     */
    public String getKeyName() {
        return key instanceof PropertyKey ? ((PropertyKey) key).getPropertyName() : null;
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterPropertyNode(this)) {
            return visitor.leavePropertyNode(
                setKey((Expression)key.accept(visitor)).
                setValue(value == null ? null : (Expression)value.accept(visitor)).
                setGetter(getter == null ? null : (FunctionNode)getter.accept(visitor)).
                setSetter(setter == null ? null : (FunctionNode)setter.accept(visitor)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterPropertyNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        if (value != null) {
            if (isStatic) {
                sb.append("static ");
            }
            if (value instanceof FunctionNode && ((FunctionNode)value).isMethod()) {
                toStringKey(sb, printType);
                ((FunctionNode)value).toStringTail(sb, printType);
            } else {
                toStringKey(sb, printType);
                sb.append(": ");
                value.toString(sb, printType);
            }
        }

        if (getter != null) {
            if (isStatic) {
                sb.append("static ");
            }
            sb.append("get ");
            toStringKey(sb, printType);
            getter.toStringTail(sb, printType);
        }

        if (setter != null) {
            if (isStatic) {
                sb.append("static ");
            }
            sb.append("set ");
            toStringKey(sb, printType);
            setter.toStringTail(sb, printType);
        }
    }

    private void toStringKey(final StringBuilder sb, final boolean printType) {
        if (computed) {
            sb.append('[');
        }
        key.toString(sb, printType);
        if (computed) {
            sb.append(']');
        }
    }

    /**
     * Get the getter for this property
     * @return getter or null if none exists
     */
    public FunctionNode getGetter() {
        return getter;
    }

    /**
     * Set the getter of this property, null if none
     * @param getter getter
     * @return same node or new node if state changed
     */
    public PropertyNode setGetter(final FunctionNode getter) {
        if (this.getter == getter) {
            return this;
        }
        return new PropertyNode(this, key, value, getter, setter, isStatic, computed, coverInitializedName, proto);
    }

    /**
     * Return the key for this property node
     * @return the key
     */
    public Expression getKey() {
        return key;
    }

    private PropertyNode setKey(final Expression key) {
        if (this.key == key) {
            return this;
        }
        return new PropertyNode(this, key, value, getter, setter, isStatic, computed, coverInitializedName, proto);
    }

    /**
     * Get the setter for this property
     * @return setter or null if none exists
     */
    public FunctionNode getSetter() {
        return setter;
    }

    /**
     * Set the setter for this property, null if none
     * @param setter setter
     * @return same node or new node if state changed
     */
    public PropertyNode setSetter(final FunctionNode setter) {
        if (this.setter == setter) {
            return this;
        }
        return new PropertyNode(this, key, value, getter, setter, isStatic, computed, coverInitializedName, proto);
    }

    /**
     * Get the value of this property
     * @return property value
     */
    public Expression getValue() {
        return value;
    }

    /**
     * Set the value of this property
     * @param value new value
     * @return same node or new node if state changed
     */
    public PropertyNode setValue(final Expression value) {
        if (this.value == value) {
            return this;
        }
        return new PropertyNode(this, key, value, getter, setter, isStatic, computed, coverInitializedName, proto);
   }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isComputed() {
        return computed;
    }

    public boolean isCoverInitializedName() {
        return coverInitializedName;
    }

    public boolean isProto() {
        return proto;
    }

    public boolean isRest() {
        return key != null && key.isTokenType(TokenType.SPREAD_OBJECT);
    }
}
