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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation of a {@code try} statement.
 */
public final class TryNode extends Statement {
    /** Try statements. */
    private final Block body;

    /** List of catch clauses. */
    private final List<Block> catchBlocks;

    /** Finally clause. */
    private final Block finallyBody;

    /** Exception symbol. */
    private Symbol exception;

    /**
     * Constructor
     *
     * @param lineNumber  lineNumber
     * @param token       token
     * @param finish      finish
     * @param body        try node body
     * @param catchBlocks list of catch blocks in order
     * @param finallyBody body of finally block or null if none
     */
    public TryNode(final int lineNumber, final long token, final int finish, final Block body, final List<Block> catchBlocks, final Block finallyBody) {
        super(lineNumber, token, finish);
        this.body        = body;
        this.catchBlocks = catchBlocks;
        this.finallyBody = finallyBody;
    }

    private TryNode(final TryNode tryNode, final Block body, final List<Block> catchBlocks, final Block finallyBody) {
        super(tryNode);
        this.body        = body;
        this.catchBlocks = catchBlocks;
        this.finallyBody = finallyBody;
        this.exception = tryNode.exception;
    }

    @Override
    public boolean isTerminal() {
        if (body.isTerminal()) {
            for (final Block catchBlock : catchBlocks) {
                if (!catchBlock.isTerminal()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Assist in IR navigation.
     * @param visitor IR navigating visitor.
     */
    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterTryNode(this)) {
            // Need to do finally body first for termination analysis. TODO still necessary?
            final Block newFinallyBody = finallyBody == null ? null : (Block)finallyBody.accept(visitor);
            final Block newBody        = (Block)body.accept(visitor);
            return visitor.leaveTryNode(
                setBody(newBody).
                setFinallyBody(newFinallyBody).
                setCatchBlocks(Node.accept(visitor, catchBlocks)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterTryNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        sb.append("try ");
    }

    /**
     * Get the body for this try block
     * @return body
     */
    public Block getBody() {
        return body;
    }

    /**
     * Reset the body of this try block
     * @param body new body
     * @return new TryNode or same if unchanged
     */
    public TryNode setBody(final Block body) {
        if (this.body == body) {
            return this;
        }
        return new TryNode(this,  body, catchBlocks, finallyBody);
    }

    /**
     * Get the catches for this try block
     * @return a list of catch nodes
     */
    public List<CatchNode> getCatches() {
        final List<CatchNode> catches = new ArrayList<>(catchBlocks.size());
        for (final Block catchBlock : catchBlocks) {
            catches.add(getCatchNodeFromBlock(catchBlock));
        }
        return Collections.unmodifiableList(catches);
    }

    private static CatchNode getCatchNodeFromBlock(final Block catchBlock) {
        return (CatchNode)catchBlock.getLastStatement();
    }

    /**
     * Get the catch blocks for this try block
     * @return a list of blocks
     */
    public List<Block> getCatchBlocks() {
        return Collections.unmodifiableList(catchBlocks);
    }

    /**
     * Set the catch blocks of this try
     * @param catchBlocks list of catch blocks
     * @return new TryNode or same if unchanged
     */
    public TryNode setCatchBlocks(final List<Block> catchBlocks) {
        if (this.catchBlocks == catchBlocks) {
            return this;
        }
        return new TryNode(this, body, catchBlocks, finallyBody);
    }

    /**
     * Get the exception symbol for this try block
     * @return a symbol for the compiler to store the exception in
     */
    public Symbol getException() {
        return exception;
    }

    /**
     * Get the body of the finally clause for this try
     * @return finally body, or null if no finally
     */
    public Block getFinallyBody() {
        return finallyBody;
    }

    /**
     * Set the finally body of this try
     * @param finallyBody new finally body
     * @return new TryNode or same if unchanged
     */
    public TryNode setFinallyBody(final Block finallyBody) {
        if (this.finallyBody == finallyBody) {
            return this;
        }
        return new TryNode(this, body, catchBlocks, finallyBody);
    }

    @Override
    public boolean isCompletionValueNeverEmpty() {
        return true;
    }
}
