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

import java.util.List;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation for a function call.
 */
public final class CallNode extends OptionalExpression {

    /** Function identifier or function body. */
    private final Expression function;

    /** Call arguments. */
    private final List<Expression> args;

    /** Is this a "new" operation */
    private static final int IS_NEW = 1 << 0;

    /** Can this be an eval? */
    private static final int IS_EVAL = 1 << 1;

    /** Is this an ImportCall? */
    private static final int IS_IMPORT = 1 << 2;

    /** Does this look like an apply call? */
    private static final int IS_APPLY_ARGUMENTS = 1 << 3;

    /** Is this an optional call ({@code a?.()}). */
    private static final int IS_OPTIONAL = 1 << 4;

    /** Is this call part of an optional chain. */
    private static final int IS_OPTIONAL_CHAIN = 1 << 5;

    /** Is this a tagged template literal call. */
    private static final int IS_TAGGED_TEMPLATE_LITERAL = 1 << 6;

    /** It this a super call in the default derived constructor? */
    private static final int IS_DEFAULT_DERIVED_CONSTRUCTOR_SUPER_CALL = 1 << 7;

    /** Is this a source phase import call? */
    private static final int IS_IMPORT_SOURCE = 1 << 8;

    private final int flags;

    private final int lineNumber;

    public static Expression forNew(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args) {
        return new CallNode(lineNumber, token, start, finish, function, args, IS_NEW);
    }

    public static Expression forCall(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args) {
        return forCall(lineNumber, token, start, finish, function, args, false, false, false, false, false);
    }

    public static Expression forCall(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args,
                    boolean optional, boolean optionalChain) {
        return forCall(lineNumber, token, start, finish, function, args, optional, optionalChain, false, false, false);
    }

    public static Expression forCall(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args,
                    boolean optional, boolean optionalChain, boolean isEval, boolean isApplyArguments, boolean isDefaultDerivedConstructorSuperCall) {
        return create(lineNumber, token, start, finish, function, args, optional, optionalChain, isEval, isApplyArguments, isDefaultDerivedConstructorSuperCall, false);
    }

    public static Expression forTaggedTemplateLiteral(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args) {
        return create(lineNumber, token, start, finish, function, args, false, false, false, false, false, true);
    }

    private static Expression create(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args,
                    boolean optional, boolean optionalChain, boolean isEval, boolean isApplyArguments, boolean isDefaultDerivedConstructorSuperCall, boolean isTaggedTemplateLiteral) {
        return new CallNode(lineNumber, token, start, finish, setIsFunction(function), args,
                        (optional ? IS_OPTIONAL : 0) | (optionalChain ? IS_OPTIONAL_CHAIN : 0) |
                                        (isEval ? IS_EVAL : 0) | (isApplyArguments ? IS_APPLY_ARGUMENTS : 0) |
                                        (isTaggedTemplateLiteral ? IS_TAGGED_TEMPLATE_LITERAL : 0) |
                                        (isDefaultDerivedConstructorSuperCall ? IS_DEFAULT_DERIVED_CONSTRUCTOR_SUPER_CALL : 0));
    }

    public static Expression forImport(int lineNumber, long token, int start, int finish, IdentNode importIdent, List<Expression> args, boolean source) {
        return new CallNode(lineNumber, token, start, finish, importIdent, args, IS_IMPORT | (source ? IS_IMPORT_SOURCE : 0));
    }

    private CallNode(int lineNumber, long token, int start, int finish, Expression function, List<Expression> args, int flags) {
        super(token, start, finish);

        this.function = function;
        this.args = List.copyOf(args);
        this.flags = flags;
        this.lineNumber = lineNumber;
    }

    private CallNode(final CallNode callNode, final Expression function, final List<Expression> args, final int flags) {
        super(callNode);
        this.lineNumber = callNode.lineNumber;
        this.function = function;
        this.args = List.copyOf(args);
        this.flags = flags;
    }

    private static Expression setIsFunction(final Expression function) {
        return function instanceof BaseNode ? ((BaseNode) function).setIsFunction() : function;
    }

    /**
     * Returns the line number.
     *
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
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterCallNode(this)) {
            return visitor.leaveCallNode(setFunction((Expression) function.accept(visitor)).setArgs(Node.accept(visitor, args)));
        }

        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterCallNode(this);
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printType) {
        final StringBuilder fsb = new StringBuilder();
        function.toString(fsb, printType);
        sb.append(fsb);

        if (isOptional()) {
            sb.append('?').append('.');
        }

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
     *
     * @return a list of arguments
     */
    public List<Expression> getArgs() {
        return args;
    }

    /**
     * Reset the arguments for the call
     *
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
     *
     * @return true if this is a call to {@code eval}
     */
    public boolean isEval() {
        return (flags & IS_EVAL) != 0;
    }

    /**
     * Return the function expression that this call invokes
     *
     * @return the function
     */
    public Expression getFunction() {
        return function;
    }

    /**
     * Reset the function expression that this call invokes
     *
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
     *
     * @return true if this a new operation
     */
    public boolean isNew() {
        return (flags & IS_NEW) != 0;
    }

    /**
     * Check if this call is a dynamic import call.
     */
    public boolean isImport() {
        return (flags & IS_IMPORT) != 0;
    }

    /**
     * Check if this call is a dynamic import.source call.
     */
    public boolean isImportSource() {
        return (flags & IS_IMPORT_SOURCE) != 0;
    }

    /**
     * Check if this call is an apply call.
     */
    public boolean isApplyArguments() {
        return (flags & IS_APPLY_ARGUMENTS) != 0;
    }

    @Override
    public boolean isOptional() {
        return (flags & IS_OPTIONAL) != 0;
    }

    @Override
    public boolean isOptionalChain() {
        return (flags & IS_OPTIONAL_CHAIN) != 0;
    }

    /**
     * Check if this is a tagged template literal call.
     */
    public boolean isTaggedTemplateLiteral() {
        return (flags & IS_TAGGED_TEMPLATE_LITERAL) != 0;
    }

    public boolean isDefaultDerivedConstructorSuperCall() {
        return (flags & IS_DEFAULT_DERIVED_CONSTRUCTOR_SUPER_CALL) != 0;
    }

}
