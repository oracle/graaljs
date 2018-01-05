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

/**
 * Common superclass for all expression nodes. Expression nodes can have an associated symbol as
 * well as a type.
 *
 */
public abstract class Expression extends Node {
    Expression(final long token, final int start, final int finish) {
        super(token, start, finish);
    }

    Expression(final long token, final int finish) {
        super(token, finish);
    }

    Expression(final Expression expr) {
        super(expr);
    }

    /**
     * Is this a self modifying assignment?
     *
     * @return true if self modifying, e.g. a++, or a*= 17
     */
    public boolean isSelfModifying() {
        return false;
    }

    /**
     * Returns true if the runtime value of this expression is always false when converted to
     * boolean as per ECMAScript ToBoolean conversion. Used in control flow calculations.
     *
     * @return true if this expression's runtime value converted to boolean is always false.
     */
    public boolean isAlwaysFalse() {
        return false;
    }

    /**
     * Returns true if the runtime value of this expression is always true when converted to boolean
     * as per ECMAScript ToBoolean conversion. Used in control flow calculations.
     *
     * @return true if this expression's runtime value converted to boolean is always true.
     */
    public boolean isAlwaysTrue() {
        return false;
    }
}
