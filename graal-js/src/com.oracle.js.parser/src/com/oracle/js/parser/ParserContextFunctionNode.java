/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.js.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.ParameterNode;
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

    /** Function node kind, see FunctionNode.Kind */
    private final FunctionNode.Kind kind;

    /** List of parameter identifiers (for simple and rest parameters). */
    private List<IdentNode> parameters;
    /** Optional parameter initialization block (replaces parameter list). */
    private ParserContextBlockNode parameterBlock;

    /** Token for function start */
    private final long token;

    /** Last function token */
    private long lastToken;

    /** Opaque node for parser end state, see {@link Parser} */
    private Object endParserState;

    private int length;
    private int parameterCount;
    private HashSet<String> parameterBoundNames;
    private IdentNode duplicateParameterBinding;
    private boolean simpleParameterList = true;
    private boolean containsDefaultParameter;

    private Module module;

    /**
     * @param token The token for the function
     * @param ident External function name
     * @param name Internal name of the function
     * @param namespace Function's namespace
     * @param line The source line of the function
     * @param kind Function kind
     * @param parameters The parameters of the function
     */
    ParserContextFunctionNode(final long token, final IdentNode ident, final String name, final Namespace namespace, final int line, final FunctionNode.Kind kind,
                    final List<IdentNode> parameters, final int length) {
        this.ident = ident;
        this.namespace = namespace;
        this.line = line;
        this.kind = kind;
        this.name = name;
        this.parameters = parameters;
        this.token = token;
        this.length = length;
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
     * @return The kind if function
     */
    public FunctionNode.Kind getKind() {
        return kind;
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

    public boolean isSubclassConstructor() {
        return getFlag(FunctionNode.IS_SUBCLASS_CONSTRUCTOR) != 0;
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
        if (parameterBlock != null) {
            addParameterInit(param, getParameterCount());
        } else {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameters.add(param);
        }
        recordParameter(false, param.isRestParameter(), false);
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

    private boolean addParameterBinding(IdentNode bindingIdentifier) {
        if (Parser.isArguments(bindingIdentifier)) {
            setFlag(FunctionNode.DEFINES_ARGUMENTS);
        }

        if (parameterBoundNames == null) {
            parameterBoundNames = new HashSet<>();
        }
        if (parameterBoundNames.add(bindingIdentifier.getName())) {
            return true;
        } else {
            duplicateParameterBinding = bindingIdentifier;
            return false;
        }
    }

    public IdentNode getDuplicateParameterBinding() {
        return duplicateParameterBinding;
    }

    public boolean isSimpleParameterList() {
        return simpleParameterList;
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
        if (parameterBlock == null) {
            initParameterBlock();
        }
    }

    private void initParameterBlock() {
        parameterBlock = new ParserContextBlockNode(token);
        parameterBlock.setFlag(Block.IS_PARAMETER_BLOCK);

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                IdentNode paramIdent = parameters.get(i);
                addParameterInit(paramIdent, i);
            }
        }
        parameters = Collections.emptyList();
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
