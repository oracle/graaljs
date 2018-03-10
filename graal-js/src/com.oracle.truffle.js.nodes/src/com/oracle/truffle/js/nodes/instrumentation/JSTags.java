/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.instrumentation;

import com.oracle.truffle.api.instrumentation.Tag;

/**
 * Runtime profiling Tags provided by Graal.js.
 *
 */
public class JSTags {

    // ##### ECMA218 12.x - JavaScript Expressions

    /**
     * ECMA2018 12.3.4 Function Calls.
     *
     * Marks all code locations that perform function calls.
     * <p>
     * Intermediate values typically provided to function calls are:
     * <ul>
     * <li><b>#0 Target</b> The target object instance for the function call, if any.</li>
     * <li><b>#1 Function</b> The function object instance to be called.</li>
     * <li><b>#n Arguments</b> Zero or more arguments are provided as successive input data events.
     * </li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>isNew</b> If the call expression is part of a new instance allocation, e.g.
     * <code>new Object()</code></li>
     * <li><b>isInvoke</b> If the call expression is part of a member expression, e.g.
     * <code>a.b();</code></li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("FunctionCallExpression")
    public static final class FunctionCallExpressionTag extends Tag {
        private FunctionCallExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.3.3 The <code>new</code> operator and ECMA2018 12.2.4 literal allocations.
     *
     * Marks all code locations that allocate objects using either the <code>new</code> operator, or
     * Object and Array literals.
     * <p>
     * Intermediate values typically provided to allocation calls are:
     * <ul>
     * <li><b>#0 Function</b> The function constructor to be called, if any.</li>
     * <li><b>#n Arguments</b> Zero or more arguments are provided as successive input data events.
     * </li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ObjectAllocationExpression")
    public static final class ObjectAllocationExpressionTag extends Tag {
        private ObjectAllocationExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.2.4 Literals.
     *
     * Marks all code locations corresponding to literal expressions.
     * <p>
     * Literal expressions have no intermediate values. Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>type</b> A <code>String</code> describing the type of literal expression tagged by the
     * node. Possible values are:
     * <ul>
     * <li><b>ObjectLiteral</b> Object literals</li></li>
     * <li><b>ArrayLiteral</b> Array literals</li></li>
     * <li><b>FunctionLiteral</b> Function literals</li></li>
     * <li><b>NumericLiteral</b> Numeric literals</li></li>
     * <li><b>BooleanLiteral</b> <code>true</code> or <code>false</code></li></li>
     * <li><b>StringLiteral</b> String literals</li></li>
     * <li><b>NullLiteral</b> <code>null</code></li></li>
     * <li><b>UndnefinedLiteral</b> <code>undefined</code></li></li>
     * <li><b>RegExpLiteral</b> Regular expression literals</li></li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("LiteralExpression")
    public static final class LiteralExpressionTag extends Tag {
        public enum Type {
            ObjectLiteral,
            ArrayLiteral,
            FunctionLiteral,
            NumericLiteral,
            BooleanLiteral,
            StringLiteral,
            NullLiteral,
            UndefinedLiteral,
            RegExpLiteral,
        }

        private LiteralExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.5 Unary expressions.
     *
     * Marks all code locations that perform unary operations, e.g., <code>!true</code>.
     * <p>
     * Unary expressions have only one input Intermediate value:
     * <ul>
     * <li><b>#0 Operand</b> The operand for the unary operation.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>operator</b> A <code>String</code> representation of the operator. Examples are:
     * <code>!</code>, <code>~</code>, <code>delete</code> etc.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("UnaryExpression")
    public static final class UnaryExpressionTag extends Tag {
        private UnaryExpressionTag() {
        }
    }

    /**
     * Binary expressions, including ECMA2018 12.8 Additive operations, ECMA2018 12.7 Multiplicative
     * operations, ECMA2018 12.9 Bitwise operations, and more.
     *
     * Marks all code locations that perform binary operations, that is, expressions with a left and
     * right operands. E.g., <code>a + b</code>.
     * <p>
     * Unary expressions have two input Intermediate values:
     * <ul>
     * <li><b>#1 Left Operand</b> The left operand for the binary operation.</li>
     * <li><b>#2 Right Operand</b> The right operand for the binary operation.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>operator</b> A <code>String</code> representation of the operator. Examples are:
     * <code>+</code>, <code>*</code> etc.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("BinaryExpression")
    public static final class BinaryExpressionTag extends Tag {
        private BinaryExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.15 Assignment expressions to local variables.
     *
     * Marks all code locations that assign values to local variables.
     * <p>
     * Intermediate values typically provided to local variable writes are:
     * <ul>
     * <li><b>#0 Value</b> The value that will be assigned to the variable.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>name</b> The unique name of the variable involved in the expression</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("WriteVariableExpression")
    public static final class WriteVariableExpressionTag extends Tag {
        private WriteVariableExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.2 Primary expressions reading local variables.
     *
     * Marks all code locations that read values from local variables.
     * <p>
     * Variable reads have no intermediate values. Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>name</b> The unique name of the variable involved in the operation</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ReadVariableExpression")
    public static final class ReadVariableExpressionTag extends Tag {
        private ReadVariableExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.15 Assignment operations to object properties using the <code>[ ]</code>
     * expression.
     *
     * Marks all code locations that write an object property using the <code>[ ]</code> expression.
     * <p>
     * Intermediate values typically provided to property writes are:
     * <ul>
     * <li><b>#0 Receiver</b> The object from which the property will be read.</li>
     * <li><b>#1 Key</b> The property key to be read.</li>
     * <li><b>#2 Value</b> The value that will be set.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("WriteElementExpression")
    public static final class WriteElementExpressionTag extends Tag {
        private WriteElementExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.3.2 Property Accessors performed using the <code>[ ]</code> expression.
     *
     * Marks all code locations that read a property from an object using the <code>[ ]</code>
     * expression.
     * <p>
     * Intermediate values typically provided to property reads are:
     * <ul>
     * <li><b>#0 Receiver</b> The object from which the property will be read.</li>
     * <li><b>#1 Key</b> The property key to be read.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ReadElementExpression")
    public static final class ReadElementExpressionTag extends Tag {
        private ReadElementExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.15 Assignment operations to object properties using the "<code>.</code>"
     * notation.
     *
     * Marks all code locations that write an object property using the "<code>.</code>" notation.
     * <p>
     * Intermediate values typically provided to property writes are:
     * <ul>
     * <li><b>#0 Receiver</b> The object from which the property will be set.</li>
     * <li><b>#1 Value</b> The value about to be assigned to the object.</li>
     * </ul>
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>key</b>The unique name of the property key to be set</li>
     * </ul>
     * </p>
     * </p>
     */
    @Tag.Identifier("WritePropertyExpression")
    public static final class WritePropertyExpressionTag extends Tag {
        private WritePropertyExpressionTag() {
        }
    }

    /**
     * ECMA2018 12.3.2 Property Accessors performed using the "<code>.</code>" notation.
     *
     * Marks all code locations that read a property from an object using the "<code>.</code>"
     * notation.
     * <p>
     * Intermediate values typically provided to property reads are:
     * <ul>
     * <li><b>#0 Receiver</b> The object from which the property will be read.</li>
     * </ul>
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>key</b>The unique name of the property key to be read</li>
     * </ul>
     * </p>
     * </p>
     */
    @Tag.Identifier("ReadPropertyExpression")
    public static final class ReadPropertyExpressionTag extends Tag {
        private ReadPropertyExpressionTag() {
        }
    }

    // ##### ECMA2018 13.x - JavaScript Statements

    /**
     * Control flow root statement.
     *
     * Marks all code locations where a statement that affects the control flow of an application is
     * declared.
     *
     * <p>
     * Examples of such statements are:
     * <ul>
     * <li>ECMA2018 13.6 The <code>if</code> statement.</li>
     * <li>ECMA2018 13.7 Iteration statements.</li>
     * <li>ECMA2018 13.12 The <code>switch</code> statement.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ControlFlowStatementRoot")
    public static final class ControlFlowStatementRootTag extends Tag {
        private ControlFlowStatementRootTag() {
        }
    }

    /**
     * Control flow condition statement.
     *
     * Marks all code locations where a conditional statement that might change the control flow of
     * an application is declared.
     *
     * <p>
     * Examples of such statements are:
     * <ul>
     * <li>ECMA2018 13.6 Condition of <code>if</code> statements.</li>
     * <li>ECMA2018 13.7 Repeating conditions for iteration statements such as <code>while</code>,
     * <code>for</code>, etc.</li>
     * <li>ECMA2018 13.12 The <code>case</code> expressions in <code>switch</code> statements.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ControlFlowConditionStatement")
    public static final class ControlFlowConditionStatementTag extends Tag {
        private ControlFlowConditionStatementTag() {
        }
    }

    /**
     * Control flow block statement.
     *
     * Marks all code locations where a block whose execution depends on a conditional statement are
     * declared.
     *
     * <p>
     * Examples of such block statements are:
     * <ul>
     * <li>ECMA2018 13.6 The <code>if</code> or the <code>else</code> branches of an <code>if</code>
     * statement.</li>
     * <li>ECMA2018 13.7 The body of an iteration statement.</li>
     * <li>ECMA2018 13.12 The block following any <code>case</code> expression in
     * <code>switch</code> statements.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ControlFlowBlockStatement")
    public static final class ControlFlowBlockStatementTag extends Tag {
        private ControlFlowBlockStatementTag() {
        }
    }

    @Tag.Identifier("ControlFlowBranchStatementTag")
    public static final class ControlFlowBranchStatementTag extends Tag {
        private ControlFlowBranchStatementTag() {
        }
    }

    // ##### ECMA Builtin operations

    /**
     * ECMA Builtin Objects Calls.
     *
     * Marks all code locations that execute an ECMA built-in operation. Examples of such built-in
     * operations are all functions of ECMA2018 19.x Fundamental objects, e.g., <code>Math</code>,
     * <code>Array</code>, <code>Date</code>, etc.
     * <p>
     * Builtin calls do not provide Intermediate values. Call-specific values can be accessed using
     * the {@link FunctionCallExpressionTag}, since Builtin calls are regular JavaScript calls.
     * <p>
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>name</b> A <code>String</code> name description of the ECMA builtin about to be
     * executed</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("BuiltinRoot")
    public static final class BuiltinRootTag extends Tag {
        private BuiltinRootTag() {
        }
    }

    /**
     * ECMA2018 18.2.1 Eval.
     *
     * Marks all code locations that call the <code>eval</code> built-in operation.
     * <p>
     * One intermediate value is provided to the builtin call:
     * <ul>
     * <li><b>#0 Source</b> The <code>String</code> to be executed.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("EvalCall")
    public static final class EvalCallTag extends Tag {
        private EvalCallTag() {
        }
    }

    // ##### Utils

    public static NodeObjectDescriptor createNodeObjectDescriptor() {
        return new NodeObjectDescriptor();
    }
}
