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

package com.oracle.truffle.js.parser.internal.ir.debug;

import java.util.ArrayList;
import java.util.List;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Lexer.RegexToken;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.AccessNode;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.BlockStatement;
import com.oracle.js.parser.ir.BreakNode;
import com.oracle.js.parser.ir.CallNode;
import com.oracle.js.parser.ir.CaseNode;
import com.oracle.js.parser.ir.CatchNode;
import com.oracle.js.parser.ir.ContinueNode;
import com.oracle.js.parser.ir.DebuggerNode;
import com.oracle.js.parser.ir.EmptyNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.ForNode;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IfNode;
import com.oracle.js.parser.ir.IndexNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LabelNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.Node;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.ReturnNode;
import com.oracle.js.parser.ir.RuntimeNode;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.SwitchNode;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.ThrowNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WhileNode;
import com.oracle.js.parser.ir.WithNode;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.truffle.js.parser.json.JSONParser;

/**
 * This IR writer produces a JSON string that represents AST as a JSON string.
 */
public final class JSONWriter extends NodeVisitor<LexicalContext> {

    /**
     * Returns AST as JSON compatible string.
     *
     * @param env environment
     * @param code code to be parsed
     * @param name name of the code source (used for location)
     * @param includeLoc tells whether to include location information for nodes or not
     * @return JSON string representation of AST of the supplied code
     */
    public static String parse(final ScriptEnvironment env, final String code, final String name, final boolean includeLoc) {
        final Parser parser = new Parser(env, Source.sourceFor(name, code), new ErrorManager.ThrowErrorManager(), env.isStrict());
        final JSONWriter jsonWriter = new JSONWriter(includeLoc);
        final FunctionNode functionNode = parser.parse(); // symbol name is ":program", default
        functionNode.accept(jsonWriter);
        return jsonWriter.getString();
    }

    @Override
    public boolean enterJoinPredecessorExpression(final JoinPredecessorExpression joinPredecessorExpression) {
        final Expression expr = joinPredecessorExpression.getExpression();
        if (expr != null) {
            expr.accept(this);
        } else {
            nullValue();
        }
        return false;
    }

    @Override
    protected boolean enterDefault(final Node node) {
        objectStart();
        location(node);

        return true;
    }

    private boolean leave() {
        objectEnd();
        return false;
    }

    @Override
    protected Node leaveDefault(final Node node) {
        objectEnd();
        return null;
    }

    @Override
    public boolean enterAccessNode(final AccessNode accessNode) {
        enterDefault(accessNode);

        type("MemberExpression");
        comma();

        property("object");
        accessNode.getBase().accept(this);
        comma();

        property("property", accessNode.getProperty());
        comma();

        property("computed", false);

        return leave();
    }

    @Override
    public boolean enterBlock(final Block block) {
        enterDefault(block);

        type("BlockStatement");
        comma();

        array("body", block.getStatements());

        return leave();
    }

    @Override
    public boolean enterBinaryNode(final BinaryNode binaryNode) {
        enterDefault(binaryNode);

        final String name;
        if (binaryNode.isAssignment()) {
            name = "AssignmentExpression";
        } else if (binaryNode.isLogical()) {
            name = "LogicalExpression";
        } else {
            name = "BinaryExpression";
        }

        type(name);
        comma();

        property("operator", binaryNode.tokenType().getName());
        comma();

        property("left");
        binaryNode.lhs().accept(this);
        comma();

        property("right");
        binaryNode.rhs().accept(this);

        return leave();
    }

    @Override
    public boolean enterBreakNode(final BreakNode breakNode) {
        enterDefault(breakNode);

        type("BreakStatement");
        comma();

        final String label = breakNode.getLabelName();
        if (label != null) {
            property("label", label);
        } else {
            property("label");
            nullValue();
        }

        return leave();
    }

    @Override
    public boolean enterCallNode(final CallNode callNode) {
        enterDefault(callNode);

        type("CallExpression");
        comma();

        property("callee");
        callNode.getFunction().accept(this);
        comma();

        array("arguments", callNode.getArgs());

        return leave();
    }

    @Override
    public boolean enterCaseNode(final CaseNode caseNode) {
        enterDefault(caseNode);

        type("SwitchCase");
        comma();

        final Node test = caseNode.getTest();
        property("test");
        if (test != null) {
            test.accept(this);
        } else {
            nullValue();
        }
        comma();

        array("consequent", caseNode.getStatements());

        return leave();
    }

    @Override
    public boolean enterCatchNode(final CatchNode catchNode) {
        enterDefault(catchNode);

        type("CatchClause");
        comma();

        property("param");
        catchNode.getException().accept(this);
        comma();

        final Node guard = catchNode.getExceptionCondition();
        if (guard != null) {
            property("guard");
            guard.accept(this);
            comma();
        }

        property("body");
        catchNode.getBody().accept(this);

        return leave();
    }

    @Override
    public boolean enterContinueNode(final ContinueNode continueNode) {
        enterDefault(continueNode);

        type("ContinueStatement");
        comma();

        final String label = continueNode.getLabelName();
        if (label != null) {
            property("label", label);
        } else {
            property("label");
            nullValue();
        }

        return leave();
    }

    @Override
    public boolean enterDebuggerNode(final DebuggerNode debuggerNode) {
        enterDefault(debuggerNode);
        type("DebuggerStatement");
        return leave();
    }

    @Override
    public boolean enterEmptyNode(final EmptyNode emptyNode) {
        enterDefault(emptyNode);

        type("EmptyStatement");

        return leave();
    }

    @Override
    public boolean enterExpressionStatement(final ExpressionStatement expressionStatement) {
        // handle debugger statement
        final Node expression = expressionStatement.getExpression();
        if (expression instanceof RuntimeNode) {
            assert false : "should not reach here: RuntimeNode";
            return false;
        }

        enterDefault(expressionStatement);

        type("ExpressionStatement");
        comma();

        property("expression");
        expression.accept(this);

        return leave();
    }

    @Override
    public boolean enterBlockStatement(final BlockStatement blockStatement) {
        if (blockStatement.isSynthetic()) {
            final Block blk = blockStatement.getBlock();
            blk.getStatements().get(0).accept(this);
            return false;
        }

        enterDefault(blockStatement);

        type("BlockStatement");
        comma();

        array("body", blockStatement.getBlock().getStatements());
        return leave();
    }

    @Override
    public boolean enterForNode(final ForNode forNode) {
        enterDefault(forNode);

        if (forNode.isForIn() || (forNode.isForEach() && forNode.getInit() != null)) {
            type("ForInStatement");
            comma();

            final Node init = forNode.getInit();
            assert init != null;
            property("left");
            init.accept(this);
            comma();

            final Node modify = forNode.getModify();
            assert modify != null;
            property("right");
            modify.accept(this);
            comma();

            property("body");
            forNode.getBody().accept(this);
            comma();

            property("each", forNode.isForEach());
        } else {
            type("ForStatement");
            comma();

            final Node init = forNode.getInit();
            property("init");
            if (init != null) {
                init.accept(this);
            } else {
                nullValue();
            }
            comma();

            final Node test = forNode.getTest();
            property("test");
            if (test != null) {
                test.accept(this);
            } else {
                nullValue();
            }
            comma();

            final Node update = forNode.getModify();
            property("update");
            if (update != null) {
                update.accept(this);
            } else {
                nullValue();
            }
            comma();

            property("body");
            forNode.getBody().accept(this);
        }

        return leave();
    }

    @Override
    public boolean enterFunctionNode(final FunctionNode functionNode) {
        final boolean program = functionNode.isProgram();
        if (program) {
            return emitProgram(functionNode);
        }

        enterDefault(functionNode);
        final String name;
        if (functionNode.isDeclared()) {
            name = "FunctionDeclaration";
        } else {
            name = "FunctionExpression";
        }
        type(name);
        comma();

        property("id");
        final FunctionNode.Kind kind = functionNode.getKind();
        if (functionNode.isAnonymous() || kind == FunctionNode.Kind.GETTER || kind == FunctionNode.Kind.SETTER) {
            nullValue();
        } else {
            functionNode.getIdent().accept(this);
        }
        comma();

        array("params", functionNode.getParameters());
        comma();

        arrayStart("defaults");
        arrayEnd();
        comma();

        property("rest");
        nullValue();
        comma();

        property("body");
        functionNode.getBody().accept(this);
        comma();

        property("generator", false);
        comma();

        property("expression", false);

        return leave();
    }

    private boolean emitProgram(final FunctionNode functionNode) {
        enterDefault(functionNode);
        type("Program");
        comma();

        // body consists of nested functions and statements
        final List<Statement> stats = functionNode.getBody().getStatements();
        final int size = stats.size();
        int idx = 0;
        arrayStart("body");

        for (final Node stat : stats) {
            stat.accept(this);
            if (idx != (size - 1)) {
                comma();
            }
            idx++;
        }
        arrayEnd();

        return leave();
    }

    @Override
    public boolean enterIdentNode(final IdentNode identNode) {
        enterDefault(identNode);

        final String name = identNode.getName();
        if ("this".equals(name)) {
            type("ThisExpression");
        } else {
            type("Identifier");
            comma();
            property("name", identNode.getName());
        }

        return leave();
    }

    @Override
    public boolean enterIfNode(final IfNode ifNode) {
        enterDefault(ifNode);

        type("IfStatement");
        comma();

        property("test");
        ifNode.getTest().accept(this);
        comma();

        property("consequent");
        ifNode.getPass().accept(this);
        final Node elsePart = ifNode.getFail();
        comma();

        property("alternate");
        if (elsePart != null) {
            elsePart.accept(this);
        } else {
            nullValue();
        }

        return leave();
    }

    @Override
    public boolean enterIndexNode(final IndexNode indexNode) {
        enterDefault(indexNode);

        type("MemberExpression");
        comma();

        property("object");
        indexNode.getBase().accept(this);
        comma();

        property("property");
        indexNode.getIndex().accept(this);
        comma();

        property("computed", true);

        return leave();
    }

    @Override
    public boolean enterLabelNode(final LabelNode labelNode) {
        enterDefault(labelNode);

        type("LabeledStatement");
        comma();

        property("label", labelNode.getLabelName());
        comma();

        property("body");
        labelNode.getBody().accept(this);

        return leave();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean enterLiteralNode(final LiteralNode literalNode) {
        enterDefault(literalNode);

        if (literalNode instanceof LiteralNode.ArrayLiteralNode) {
            type("ArrayExpression");
            comma();

            array("elements", ((LiteralNode.ArrayLiteralNode) literalNode).getElementExpressions());
        } else {
            type("Literal");
            comma();

            property("value");
            final Object value = literalNode.getValue();
            if (value instanceof RegexToken) {
                // encode RegExp literals as Strings of the form /.../<flags>
                final RegexToken regex = (RegexToken) value;
                final StringBuilder regexBuf = new StringBuilder();
                regexBuf.append('/');
                regexBuf.append(regex.getExpression());
                regexBuf.append('/');
                regexBuf.append(regex.getOptions());
                buf.append(quote(regexBuf.toString()));
            } else {
                final String str = literalNode.getString();
                // encode every String literal with prefix '$' so that script
                // can differentiate b/w RegExps as Strings and Strings.
                buf.append(literalNode.isString() ? quote("$" + str) : str);
            }
        }

        return leave();
    }

    @Override
    public boolean enterObjectNode(final ObjectNode objectNode) {
        enterDefault(objectNode);

        type("ObjectExpression");
        comma();

        array("properties", objectNode.getElements());

        return leave();
    }

    @Override
    public boolean enterPropertyNode(final PropertyNode propertyNode) {
        final Node key = propertyNode.getKey();

        final Node value = propertyNode.getValue();
        if (value != null) {
            objectStart();
            location(propertyNode);

            property("key");
            key.accept(this);
            comma();

            property("value");
            value.accept(this);
            comma();

            property("kind", "init");

            objectEnd();
        } else {
            // getter
            final Node getter = propertyNode.getGetter();
            if (getter != null) {
                objectStart();
                location(propertyNode);

                property("key");
                key.accept(this);
                comma();

                property("value");
                getter.accept(this);
                comma();

                property("kind", "get");

                objectEnd();
            }

            // setter
            final Node setter = propertyNode.getSetter();
            if (setter != null) {
                if (getter != null) {
                    comma();
                }
                objectStart();
                location(propertyNode);

                property("key");
                key.accept(this);
                comma();

                property("value");
                setter.accept(this);
                comma();

                property("kind", "set");

                objectEnd();
            }
        }

        return false;
    }

    @Override
    public boolean enterReturnNode(final ReturnNode returnNode) {
        enterDefault(returnNode);

        type("ReturnStatement");
        comma();

        final Node arg = returnNode.getExpression();
        property("argument");
        if (arg != null) {
            arg.accept(this);
        } else {
            nullValue();
        }

        return leave();
    }

    @Override
    public boolean enterRuntimeNode(final RuntimeNode runtimeNode) {
        assert false : "should not reach here: RuntimeNode";
        return false;
    }

    @Override
    public boolean enterSwitchNode(final SwitchNode switchNode) {
        enterDefault(switchNode);

        type("SwitchStatement");
        comma();

        property("discriminant");
        switchNode.getExpression().accept(this);
        comma();

        array("cases", switchNode.getCases());

        return leave();
    }

    @Override
    public boolean enterTernaryNode(final TernaryNode ternaryNode) {
        enterDefault(ternaryNode);

        type("ConditionalExpression");
        comma();

        property("test");
        ternaryNode.getTest().accept(this);
        comma();

        property("consequent");
        ternaryNode.getTrueExpression().accept(this);
        comma();

        property("alternate");
        ternaryNode.getFalseExpression().accept(this);

        return leave();
    }

    @Override
    public boolean enterThrowNode(final ThrowNode throwNode) {
        enterDefault(throwNode);

        type("ThrowStatement");
        comma();

        property("argument");
        throwNode.getExpression().accept(this);

        return leave();
    }

    @Override
    public boolean enterTryNode(final TryNode tryNode) {
        enterDefault(tryNode);

        type("TryStatement");
        comma();

        property("block");
        tryNode.getBody().accept(this);
        comma();

        final List<CatchNode> catches = tryNode.getCatches();
        final List<CatchNode> guarded = new ArrayList<>();
        CatchNode unguarded = null;
        if (catches != null) {
            for (final CatchNode cn : catches) {
                if (cn.getExceptionCondition() != null) {
                    guarded.add(cn);
                } else {
                    assert unguarded == null : "too many unguarded?";
                    unguarded = cn;
                }
            }
        }

        array("guardedHandlers", guarded);
        comma();

        property("handler");
        if (unguarded != null) {
            unguarded.accept(this);
        } else {
            nullValue();
        }
        comma();

        property("finalizer");
        final Node finallyNode = tryNode.getFinallyBody();
        if (finallyNode != null) {
            finallyNode.accept(this);
        } else {
            nullValue();
        }

        return leave();
    }

    @Override
    public boolean enterUnaryNode(final UnaryNode unaryNode) {
        enterDefault(unaryNode);

        final TokenType tokenType = unaryNode.tokenType();
        if (tokenType == TokenType.NEW) {
            type("NewExpression");
            comma();

            final CallNode callNode = (CallNode) unaryNode.getExpression();
            property("callee");
            callNode.getFunction().accept(this);
            comma();

            array("arguments", callNode.getArgs());
        } else {
            final String operator;
            final boolean prefix;
            switch (tokenType) {
                case INCPOSTFIX:
                    prefix = false;
                    operator = "++";
                    break;
                case DECPOSTFIX:
                    prefix = false;
                    operator = "--";
                    break;
                case INCPREFIX:
                    operator = "++";
                    prefix = true;
                    break;
                case DECPREFIX:
                    operator = "--";
                    prefix = true;
                    break;
                default:
                    prefix = true;
                    operator = tokenType.getName();
                    break;
            }

            type(unaryNode.isAssignment() ? "UpdateExpression" : "UnaryExpression");
            comma();

            property("operator", operator);
            comma();

            property("prefix", prefix);
            comma();

            property("argument");
            unaryNode.getExpression().accept(this);
        }

        return leave();
    }

    @Override
    public boolean enterVarNode(final VarNode varNode) {
        final Node init = varNode.getInit();
        if (init instanceof FunctionNode && ((FunctionNode) init).isDeclared()) {
            // function declaration - don't emit VariableDeclaration instead
            // just emit FunctionDeclaration using 'init' Node.
            init.accept(this);
            return false;
        }

        enterDefault(varNode);

        type("VariableDeclaration");
        comma();

        arrayStart("declarations");

        // VariableDeclarator
        objectStart();
        location(varNode.getName());

        type("VariableDeclarator");
        comma();

        property("id");
        varNode.getName().accept(this);
        comma();

        property("init");
        if (init != null) {
            init.accept(this);
        } else {
            nullValue();
        }

        // VariableDeclarator
        objectEnd();

        // declarations
        arrayEnd();

        return leave();
    }

    @Override
    public boolean enterWhileNode(final WhileNode whileNode) {
        enterDefault(whileNode);

        type(whileNode.isDoWhile() ? "DoWhileStatement" : "WhileStatement");
        comma();

        if (whileNode.isDoWhile()) {
            property("body");
            whileNode.getBody().accept(this);
            comma();

            property("test");
            whileNode.getTest().accept(this);
        } else {
            property("test");
            whileNode.getTest().accept(this);
            comma();

            property("body");
            whileNode.getBody().accept(this);
        }

        return leave();
    }

    @Override
    public boolean enterWithNode(final WithNode withNode) {
        enterDefault(withNode);

        type("WithStatement");
        comma();

        property("object");
        withNode.getExpression().accept(this);
        comma();

        property("body");
        withNode.getBody().accept(this);

        return leave();
    }

    // Internals below

    private JSONWriter(final boolean includeLocation) {
        super(new LexicalContext());
        this.buf = new StringBuilder();
        this.includeLocation = includeLocation;
    }

    private final StringBuilder buf;
    private final boolean includeLocation;

    private String getString() {
        return buf.toString();
    }

    private void property(final String key, final String value, final boolean escape) {
        buf.append('"');
        buf.append(key);
        buf.append("\":");
        if (value != null) {
            if (escape) {
                buf.append('"');
            }
            buf.append(value);
            if (escape) {
                buf.append('"');
            }
        }
    }

    private void property(final String key, final String value) {
        property(key, value, true);
    }

    private void property(final String key, final boolean value) {
        property(key, Boolean.toString(value), false);
    }

    private void property(final String key, final int value) {
        property(key, Integer.toString(value), false);
    }

    private void property(final String key) {
        property(key, null);
    }

    private void type(final String value) {
        property("type", value);
    }

    private void objectStart(final String name) {
        buf.append('"');
        buf.append(name);
        buf.append("\":{");
    }

    private void objectStart() {
        buf.append('{');
    }

    private void objectEnd() {
        buf.append('}');
    }

    private void array(final String name, final List<? extends Node> nodes) {
        // The size, idx comparison is just to avoid trailing comma..
        final int size = nodes.size();
        int idx = 0;
        arrayStart(name);
        for (final Node node : nodes) {
            if (node != null) {
                node.accept(this);
            } else {
                nullValue();
            }
            if (idx != (size - 1)) {
                comma();
            }
            idx++;
        }
        arrayEnd();
    }

    private void arrayStart(final String name) {
        buf.append('"');
        buf.append(name);
        buf.append('"');
        buf.append(':');
        buf.append('[');
    }

    private void arrayEnd() {
        buf.append(']');
    }

    private void comma() {
        buf.append(',');
    }

    private void nullValue() {
        buf.append("null");
    }

    private void location(final Node node) {
        if (includeLocation) {
            objectStart("loc");

            // source name
            final Source src = lc.getCurrentFunction().getSource();
            property("source", src.getName());
            comma();

            // start position
            objectStart("start");
            final int start = node.getStart();
            property("line", src.getLine(start));
            comma();
            property("column", src.getColumn(start));
            objectEnd();
            comma();

            // end position
            objectStart("end");
            final int end = node.getFinish();
            property("line", src.getLine(end));
            comma();
            property("column", src.getColumn(end));
            objectEnd();

            // end 'loc'
            objectEnd();

            comma();
        }
    }

    private static String quote(final String str) {
        return JSONParser.quote(str);
    }
}
