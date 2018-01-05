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

import java.util.Collections;
import java.util.List;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation for a function call.
 */
public final class CallNode extends LexicalContextExpression {

    /** Function identifier or function body. */
    private final Expression function;

    /** Call arguments. */
    private final List<Expression> args;

    /** Is this a "new" operation */
    private static final int IS_NEW = 1 << 0;

    /** Can this be an eval? */
    private static final int IS_EVAL = 1 << 1;

    private final int flags;

    private final int lineNumber;

    /**
     * Constructors
     *
     * @param lineNumber line number
     * @param token      token
     * @param finish     finish
     * @param function   the function to call
     * @param args       args to the call
     * @param isNew      true if this is a constructor call with the "new" keyword
     */
    public CallNode(final int lineNumber, final long token, final int finish, final Expression function, final List<Expression> args, final boolean isNew) {
        super(token, finish);

        this.function       = function;
        this.args           = args;
        this.flags          = isNew ? IS_NEW : 0;
        this.lineNumber     = lineNumber;
    }

    private CallNode(final CallNode callNode, final Expression function, final List<Expression> args, final int flags) {
        super(callNode);
        this.lineNumber = callNode.lineNumber;
        this.function = function;
        this.args = args;
        this.flags = flags;
    }

    /**
     * Returns the line number.
     * @return the line number.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Assist in IR navigation.
     *
     * @param visitor IR navigating visitor.
     *
     * @return node or replacement
     */
    @Override
    public Node accept(final LexicalContext lc, final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterCallNode(this)) {
            final CallNode newCallNode = (CallNode)visitor.leaveCallNode(
                    setFunction((Expression)function.accept(visitor)).
                    setArgs(Node.accept(visitor, args)));
            // Theoretically, we'd need to instead pass lc to every setter and do a replacement on each. In practice,
            // setType from TypeOverride can't accept a lc, and we don't necessarily want to go there now.
            if (this != newCallNode) {
                return Node.replaceInLexicalContext(lc, this, newCallNode);
            }
        }

        return this;
    }

    @Override
    public <R> R accept(LexicalContext lc, TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterCallNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final StringBuilder fsb = new StringBuilder();
        function.toString(fsb, printType);
        sb.append(fsb);

        sb.append('(');

        boolean first = true;

        for (final Node arg : args) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }

            arg.toString(sb, printType);
        }

        sb.append(')');
    }

    /**
     * Get the arguments for the call
     * @return a list of arguments
     */
    public List<Expression> getArgs() {
        return Collections.unmodifiableList(args);
    }

    /**
     * Reset the arguments for the call
     * @param args new arguments list
     * @return new callnode, or same if unchanged
     */
    public CallNode setArgs(final List<Expression> args) {
        if (this.args == args) {
            return this;
        }
        return new CallNode(this, function, args, flags);
    }

    /**
     * Check if this call is a call to {@code eval}
     * @return true if this is a call to {@code eval}
     */
    public boolean isEval() {
        return (flags & IS_EVAL) != 0;
    }

    public CallNode setIsEval() {
        return setFlags(flags | IS_EVAL);
    }

    /**
     * Return the function expression that this call invokes
     * @return the function
     */
    public Expression getFunction() {
        return function;
    }

    /**
     * Reset the function expression that this call invokes
     * @param function the function
     * @return same node or new one on state change
     */
    public CallNode setFunction(final Expression function) {
        if (this.function == function) {
            return this;
        }
        return new CallNode(this, function, args, flags);
    }

    /**
     * Check if this call is a new operation
     * @return true if this a new operation
     */
    public boolean isNew() {
        return (flags & IS_NEW) != 0;
    }

    private CallNode setFlags(final int flags) {
        if (this.flags == flags) {
            return this;
        }
        return new CallNode(this, function, args, flags);
    }
}
