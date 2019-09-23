/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.js.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.Symbol;
import com.oracle.js.parser.ir.VarNode;

/**
 * ParserContextNode that represents a function that is currently being parsed
 */
class ParserContextFunctionNode extends ParserContextBaseNode {

    /** Function name */
    private final String name;

    /** Function identifier node */
    private final IdentNode ident;

    /** Name space for function */
    private final Namespace namespace;

    /** Line number for function declaration */
    private final int line;

    private final Scope parentScope;

    /** List of parameter identifiers (for simple and rest parameters). */
    private List<IdentNode> parameters;
    /** Optional parameter initialization block (replaces parameter list). */
    private ParserContextBlockNode parameterBlock;
    /** Function body (i.e. var declaration) scope. */
    /** Function body block. */
    private ParserContextBlockNode bodyBlock;

    /** Token for function start */
    private final long token;

    /** Last function token */
    private long lastToken;

    /** Opaque node for parser end state, see {@link Parser} */
    private Object endParserState;

    private int length;
    private int parameterCount;
    private IdentNode duplicateParameterBinding;
    private boolean simpleParameterList = true;
    private boolean hasParameterExpressions;
    private boolean containsDefaultParameter;

    private Module module;

    /**
     * @param token The token for the function
     * @param ident External function name
     * @param name Internal name of the function
     * @param namespace Function's namespace
     * @param line The source line of the function
     * @param parameters The parameters of the function
     * @param parentScope The parent scope
     */
    ParserContextFunctionNode(final long token, final IdentNode ident, final String name, final Namespace namespace, final int line, final int flags,
                    final List<IdentNode> parameters, final int length, Scope parentScope) {
        super(flags);
        this.ident = ident;
        this.namespace = namespace;
        this.line = line;
        this.name = name;
        this.parameters = parameters;
        this.token = token;
        this.length = length;
        this.parentScope = parentScope;
        this.parameterCount = parameters == null ? 0 : parameters.size();
        assert calculateLength(parameters) == length;
    }

    /**
     * @return Internal name of the function
     */
    public String getName() {
        return name;
    }

    /**
     * @return The external identifier for the function
     */
    public IdentNode getIdent() {
        return ident;
    }

    /**
     *
     * @return true if function is the program function
     */
    public boolean isProgram() {
        return getFlag(FunctionNode.IS_PROGRAM) != 0;
    }

    /**
     * @return if function in strict mode
     */
    public boolean isStrict() {
        return getFlag(FunctionNode.IS_STRICT) != 0;
    }

    /**
     * @return true if the function has nested evals
     */
    public boolean hasNestedEval() {
        return getFlag(FunctionNode.HAS_NESTED_EVAL) != 0;
    }

    /**
     * Returns true if any of the blocks in this function create their own scope.
     *
     * @return true if any of the blocks in this function create their own scope.
     */
    public boolean hasScopeBlock() {
        return getFlag(FunctionNode.HAS_SCOPE_BLOCK) != 0;
    }

    /**
     * Create a unique name in the namespace of this FunctionNode
     *
     * @param base prefix for name
     * @return base if no collision exists, otherwise a name prefix with base
     */
    public String uniqueName(final String base) {
        return namespace.uniqueName(base);
    }

    /**
     * @return line number of the function
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * Get parameters
     *
     * @return The parameters of the function
     */
    public List<IdentNode> getParameters() {
        if (parameters == null) {
            return Collections.emptyList();
        }
        return parameters;
    }

    void setParameters(List<IdentNode> parameters) {
        this.parameters = parameters;
    }

    /**
     * Set last token
     *
     * @param token New last token
     */
    public void setLastToken(final long token) {
        this.lastToken = token;

    }

    /**
     * @return lastToken Function's last token
     */
    public long getLastToken() {
        return lastToken;
    }

    /**
     * Returns the ParserState of when the parsing of this function was ended
     *
     * @return endParserState The end parser state
     */
    public Object getEndParserState() {
        return endParserState;
    }

    /**
     * Sets the ParserState of when the parsing of this function was ended
     *
     * @param endParserState The end parser state
     */
    public void setEndParserState(final Object endParserState) {
        this.endParserState = endParserState;
    }

    /**
     * Returns the if of this function
     *
     * @return The function id
     */
    public int getId() {
        return isProgram() ? -1 : Token.descPosition(token);
    }

    public boolean isMethod() {
        return getFlag(FunctionNode.IS_METHOD) != 0;
    }

    public boolean isClassConstructor() {
        return getFlag(FunctionNode.IS_CLASS_CONSTRUCTOR) != 0;
    }

    public boolean isDerivedConstructor() {
        return getFlag(FunctionNode.IS_DERIVED_CONSTRUCTOR) != 0;
    }

    public int getLength() {
        return length;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Add simple or rest parameter.
     */
    public void addParameter(IdentNode param) {
        addParameterBinding(param);
        if (hasParameterExpressions()) {
            addParameterInit(param, getParameterCount());
        } else {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameters.add(param);
        }
        recordParameter(false, param.isRestParameter(), false);
    }

    public boolean hasParameterExpressions() {
        return hasParameterExpressions;
    }

    /**
     * Update number of parameters, length, and simple parameter list flag.
     */
    private void recordParameter(boolean isDefault, boolean isRest, boolean isPattern) {
        if (!isDefault && !isRest) {
            if (!containsDefaultParameter) {
                length++;
            }
        } else {
            containsDefaultParameter = true;
        }
        if ((isDefault || isRest || isPattern) && simpleParameterList) {
            recordNonSimpleParameterList();
        }
        parameterCount++;
    }

    private void recordNonSimpleParameterList() {
        this.simpleParameterList = false;
        setFlag(FunctionNode.HAS_NON_SIMPLE_PARAMETER_LIST);
    }

    public boolean isSimpleParameterList() {
        return simpleParameterList;
    }

    private boolean addParameterBinding(IdentNode bindingIdentifier) {
        if (Parser.isArguments(bindingIdentifier)) {
            setFlag(FunctionNode.DEFINES_ARGUMENTS);
        }

        // Parameters have a temporal dead zone if the parameter list contains expressions.
        boolean tdz = hasParameterExpressions();
        Symbol paramSymbol = new Symbol(bindingIdentifier.getName(), Symbol.IS_LET | Symbol.IS_PARAM);
        if (getParameterScope().putSymbol(paramSymbol) == null) {
            // declareParameter(bindingIdentifier.getName());
            return true;
        } else {
            if (duplicateParameterBinding == null) {
                duplicateParameterBinding = bindingIdentifier;
            }
            return false;
        }
    }

    public IdentNode getDuplicateParameterBinding() {
        return duplicateParameterBinding;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public boolean isAsync() {
        return getFlag(FunctionNode.IS_ASYNC) != 0;
    }

    public boolean isArrow() {
        return getFlag(FunctionNode.IS_ARROW) != 0;
    }

    public boolean isGenerator() {
        return getFlag(FunctionNode.IS_GENERATOR) != 0;
    }

    public boolean isScriptOrModule() {
        return getFlag(FunctionNode.IS_SCRIPT | FunctionNode.IS_MODULE) != 0;
    }

    public ParserContextBlockNode getParameterBlock() {
        return parameterBlock;
    }

    public void addDefaultParameter(VarNode varNode) {
        ensureParameterBlock();
        parameterBlock.appendStatement(varNode);
        addParameterBinding(varNode.getName());
        recordParameter(true, false, false);
    }

    public void addParameterBindingDeclaration(VarNode varNode) {
        ensureParameterBlock();
        parameterBlock.appendStatement(varNode);
        addParameterBinding(varNode.getName());
    }

    public void addParameterInitialization(int lineNumber, Expression assignment, boolean isDefault) {
        ensureParameterBlock();
        parameterBlock.appendStatement(new ExpressionStatement(lineNumber, assignment.getToken(), assignment.getFinish(), assignment));
        recordParameter(isDefault, false, true);
    }

    private void ensureParameterBlock() {
        if (!hasParameterExpressions()) {
            hasParameterExpressions = true;
            initParameterBlock();
        }
    }

    private void initParameterBlock() {
        if (parameterBlock == null) {
            createParameterBlock();
        }

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                IdentNode paramIdent = parameters.get(i);
                addParameterInit(paramIdent, i);
            }
        }
        parameters = Collections.emptyList();
    }

    public ParserContextBlockNode createParameterBlock() {
        assert bodyBlock == null; // parameter block must be created before body block
        parameterBlock = new ParserContextBlockNode(token, Scope.createParameter(parentScope, getFlags()));
        parameterBlock.setFlag(Block.IS_PARAMETER_BLOCK | Block.IS_SYNTHETIC);
        return parameterBlock;
    }

    private void addParameterInit(IdentNode param, int index) {
        long paramToken = param.getToken();
        int paramFinish = param.getFinish();
        ParameterNode paramValue;
        if (param.isRestParameter()) {
            paramValue = new ParameterNode(paramToken, paramFinish, index, true);
        } else {
            paramValue = new ParameterNode(paramToken, paramFinish, index);
        }
        parameterBlock.appendStatement(new VarNode(line, Token.recast(paramToken, TokenType.LET), paramFinish, param, paramValue, VarNode.IS_LET));
        declareParameter(param.getName());
    }

    private void declareParameter(String parameterName) {
        if (hasParameterExpressions()) {
            // Parameters have a temporal dead zone (unless the parameter list is simple).
            // parameterBlock.getScope().putSymbol(new Symbol(parameterName, Symbol.IS_LET |
            // Symbol.IS_PARAM));
            assert parameterBlock.getScope().hasSymbol(parameterName);
        }
    }

    private void finalizeParameters() {
        if (!hasParameterExpressions()) {
            if (parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    IdentNode parameter = parameters.get(i);
                    getBodyScope().putSymbol(new Symbol(parameter.getName(), Symbol.IS_VAR | Symbol.IS_PARAM));
                }
            }
        } else {
            parameterBlock.getScope().close();
            parameters = Collections.emptyList();
        }
    }

    public ParserContextBlockNode getBodyBlock() {
        return bodyBlock;
    }

    public void setBodyBlock(ParserContextBlockNode bodyBlock) {
        assert this.bodyBlock == null;
        this.bodyBlock = bodyBlock;
        finalizeParameters();
    }

    public void setParameterBlock(ParserContextBlockNode parameterBlock) {
        assert this.parameterBlock == null;
        this.parameterBlock = parameterBlock;
    }

    public Scope createBodyScope() {
        assert bodyBlock == null;
        // We only need the parameter scope if the parameter list contains expressions.
        Scope parent = hasParameterExpressions() ? parameterBlock.getScope() : parentScope;
        return Scope.createFunctionBody(parent, getFlags());
    }

    public Scope getBodyScope() {
        return bodyBlock.getScope();
    }

    public Scope getParameterScope() {
        return parameterBlock.getScope();
    }

    private static int calculateLength(final List<IdentNode> parameters) {
        int length = 0;
        if (parameters != null) {
            for (IdentNode param : parameters) {
                if (param.isRestParameter()) {
                    break;
                }
                length++;
            }
        }
        return length;
    }
}
