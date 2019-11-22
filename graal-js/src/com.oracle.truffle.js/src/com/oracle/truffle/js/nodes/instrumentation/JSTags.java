/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.instrumentation;

import com.oracle.truffle.api.instrumentation.Tag;

/**
 * Runtime profiling Tags provided by Graal.js.
 *
 */
public final class JSTags {

    private JSTags() {
        // should not be constructed
    }

    public static final Class<?>[] ALL = new Class[]{
                    ObjectAllocationTag.class,
                    BinaryOperationTag.class,
                    UnaryOperationTag.class,
                    WriteVariableTag.class,
                    ReadElementTag.class,
                    WriteElementTag.class,
                    ReadPropertyTag.class,
                    WritePropertyTag.class,
                    ReadVariableTag.class,
                    LiteralTag.class,
                    FunctionCallTag.class,
                    BuiltinRootTag.class,
                    EvalCallTag.class,
                    ControlFlowRootTag.class,
                    ControlFlowBlockTag.class,
                    ControlFlowBranchTag.class,
                    DeclareTag.class,
                    FunctionDeclarationTag.class
    };

    // ##### ECMAScript Language Expressions

    /**
     * Function Calls.
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
    @Tag.Identifier("FunctionCall")
    public static final class FunctionCallTag extends Tag {
        private FunctionCallTag() {
        }
    }

    /**
     * The <code>new</code> operator and literal allocations.
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
    @Tag.Identifier("ObjectAllocation")
    public static final class ObjectAllocationTag extends Tag {
        private ObjectAllocationTag() {
        }
    }

    /**
     * Literals.
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
    @Tag.Identifier("Literal")
    public static final class LiteralTag extends Tag {
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

        private LiteralTag() {
        }
    }

    /**
     * Unary expressions.
     *
     * Marks all code locations that perform unary operations, e.g., <code>!true</code>.
     * <p>
     * All unary expressions except <code>delete</code> have one input Intermediate value:
     * <ul>
     * <li><b>#0 Operand</b> The operand for the unary operation.</li>
     * </ul>
     * The <code>delete</code> expression provides two intermediate values:
     * <ul>
     * <li><b>#0 Target</b> The target object instance.</li>
     * <li><b>#1 Key</b> The property key to be deleted.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>operator</b> A <code>String</code> representation of the operator. Examples are:
     * <code>!</code>, <code>~</code>, <code>delete</code> etc.</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("UnaryOperation")
    public static final class UnaryOperationTag extends Tag {
        private UnaryOperationTag() {
        }
    }

    /**
     * Binary expressions, including Additive operations, Multiplicative operations, Bitwise
     * operations, and more.
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
    @Tag.Identifier("BinaryOperation")
    public static final class BinaryOperationTag extends Tag {
        private BinaryOperationTag() {
        }
    }

    /**
     * Assignment expressions to local variables.
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
    @Tag.Identifier("WriteVariable")
    public static final class WriteVariableTag extends Tag {
        private WriteVariableTag() {
        }
    }

    /**
     * Primary expressions reading local variables.
     *
     * Marks all code locations that read values from local variables.
     * <p>
     * Variable reads have no intermediate values. Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>name</b> The unique name of the variable involved in the operation</li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ReadVariable")
    public static final class ReadVariableTag extends Tag {
        private ReadVariableTag() {
        }
    }

    /**
     * Assignment operations to object properties using the <code>[ ]</code> expression.
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
    @Tag.Identifier("WriteElement")
    public static final class WriteElementTag extends Tag {
        private WriteElementTag() {
        }
    }

    /**
     * Property Accessors performed using the <code>[ ]</code> expression.
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
    @Tag.Identifier("ReadElement")
    public static final class ReadElementTag extends Tag {
        private ReadElementTag() {
        }
    }

    /**
     * Assignment operations to object properties using the "<code>.</code>" notation.
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
    @Tag.Identifier("WriteProperty")
    public static final class WritePropertyTag extends Tag {
        private WritePropertyTag() {
        }
    }

    /**
     * Property Accessors performed using the "<code>.</code>" notation.
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
    @Tag.Identifier("ReadProperty")
    public static final class ReadPropertyTag extends Tag {
        private ReadPropertyTag() {
        }
    }

    // ##### ECMAScript Statements

    /**
     * Control flow root nodes.
     *
     * Marks all code locations where a node that affects the control flow of an application is
     * declared.
     *
     * <p>
     * Examples are:
     * <ul>
     * <li>The <code>if</code> statement.</li>
     * <li>Iteration statements.</li>
     * <li>The <code>switch</code> statement.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>type</b> A <code>String</code> describing the control flow construct type. Possible
     * values are:
     * <ul>
     * <li><b>Conditional</b> A conditional branch statement (e.g., <code>if</code> or
     * <code>switch</code> statements.)</li>
     * <li><b>Iteration</b> An iterative block (e.g., <code>for</code> or <code>while</code> loop
     * statements.)</li>
     * <li><b>ExceptionHandler</b> An exception handling block (e.g., <code>try</code>.)</li>
     * </ul>
     * </p>
     **
     */
    @Tag.Identifier("ControlFlowRootTag")
    public static final class ControlFlowRootTag extends Tag {
        public enum Type {
            Conditional,
            ExceptionHandler,
            ForOfIteration,
            ForAwaitOfIteration,
            ForInIteration,
            ForIteration,
            DoWhileIteration,
            WhileIteration,
            AsyncFunction,
        }

        private ControlFlowRootTag() {
        }
    }

    /**
     * Control flow branch node.
     *
     * Marks all code locations where a statement or expression that might change the control flow
     * of an application is declared.
     *
     * <p>
     * Examples are:
     * <ul>
     * <li>Condition of <code>if</code> statements.</li>
     * <li>Repeating conditions for iteration statements such as <code>while</code>,
     * <code>for</code>, etc.</li>
     * <li>The <code>case</code> expressions in <code>switch</code> statements.</li>
     * <li>The <code>continue</code> statement.</li>
     * <li>The <code>break</code> statement.</li>
     * <li>The <code>throw</code> statement.</li>
     * </ul>
     *
     * Tagged nodes provide the following metadata:
     * <ul>
     * <li><b>type</b> A <code>String</code> describing the control flow construct type. Possible
     * values are:
     * <ul>
     * <li><b>Condition</b> The control condition of a conditional branch statement (e.g.,
     * <code>if</code> or <code>switch</code> statements.</li>
     * <li><b>Continue</b> The <code>continue</code> statement.</li>
     * <li><b>Break</b> The <code>break</code> statement.</li>
     * <li><b>Throw</b> The <code>throw</code> statement.</li>
     * <li><b>Return</b> The <code>return</code> statement.</li>
     * </ul>
     * </p>
     *
     * </p>
     */
    @Tag.Identifier("ControlFlowBranchTag")
    public static final class ControlFlowBranchTag extends Tag {
        public enum Type {
            Condition,
            Continue,
            Break,
            Throw,
            Return,
            Await,
        }

        private ControlFlowBranchTag() {
        }
    }

    /**
     * Control flow block node.
     *
     * Marks all code locations where a block whose execution depends on a runtime condition are
     * declared.
     *
     * <p>
     * Examples are:
     * <ul>
     * <li>The <code>if</code> or the <code>else</code> branches of an <code>if</code> statement.
     * </li>
     * <li>The body of an iteration statement.</li>
     * <li>The block following any <code>case</code> expression in <code>switch</code> statements.
     * </li>
     * </ul>
     * </p>
     */
    @Tag.Identifier("ControlFlowBlockTag")
    public static final class ControlFlowBlockTag extends Tag {
        private ControlFlowBlockTag() {
        }
    }

    // ##### Builtin operations

    /**
     * Builtin Objects Calls.
     *
     * Marks all code locations that execute an ECMA built-in operation. Examples of such built-in
     * operations are all functions of ECMA Fundamental objects, e.g., <code>Math</code>,
     * <code>Array</code>, <code>Date</code>, etc.
     * <p>
     * Builtin calls do not provide Intermediate values. Call-specific values can be accessed using
     * the {@link FunctionCallTag}, since Builtin calls are regular JavaScript calls.
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
     * Eval.
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

    /**
     * Input generating node.
     *
     * Special Tag that marks Graal.js AST nodes that are expected to provide an input value to
     * their parent nodes, and are neither ECMA expressions or statements.
     *
     */
    @Tag.Identifier("InputNode")
    public static final class InputNodeTag extends Tag {
        private InputNodeTag() {
        }
    }

    /**
     * Variable declaration node.
     *
     * TODO(db) remove once we have an equivalent tag in Truffle.
     *
     */
    @Tag.Identifier("Declare")
    public static final class DeclareTag extends Tag {
        private DeclareTag() {
        }
    }

    /**
     * Function declaration.
     *
     * TODO(aj) remove or merge into an equivalent standard declaration tag in Truffle.
     *
     */
    @Tag.Identifier("FunctionDeclaration")
    public static final class FunctionDeclarationTag extends Tag {
        private FunctionDeclarationTag() {
        }
    }

    // ##### Utils

    public static NodeObjectDescriptor createNodeObjectDescriptor() {
        return new NodeObjectDescriptor();
    }

    public static NodeObjectDescriptor createNodeObjectDescriptor(String name, Object value) {
        NodeObjectDescriptor desc = new NodeObjectDescriptor();
        desc.addProperty(name, value);
        return desc;
    }
}
