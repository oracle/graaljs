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

// @formatter:off
abstract class BreakableStatement extends LexicalContextStatement implements BreakableNode {

    /**
     * Constructor
     *
     * @param lineNumber line number
     * @param token      token
     * @param finish     finish
     */
    protected BreakableStatement(final int lineNumber, final long token, final int finish) {
        super(lineNumber, token, finish);
    }

    /**
     * Copy constructor
     *
     * @param breakableNode source node
     */
    protected BreakableStatement(final BreakableStatement breakableNode) {
        super(breakableNode);
    }

    /**
     * Check whether this can be broken out from without using a label,
     * e.g. everything but Blocks, basically
     * @return true if breakable without label
     */
    @Override
    public boolean isBreakableWithoutLabel() {
        return true;
    }
}
