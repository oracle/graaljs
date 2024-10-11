/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.oracle.truffle.api.strings.TruffleString;

/**
 * ParserContextNode that represents a function that is currently being parsed
 */
class ParserContextFunctionNode extends ParserContextBaseNode {

    /** Function name */
    private final TruffleString name;

    /** Function identifier node */
    private final IdentNode ident;

    /** Line number for function declaration */
    private final int line;

    private final Scope parentScope;

    /** List of parameter identifiers (for simple and rest parameters). */
    private List<IdentNode> parameters;
    /** Optional parameter initialization block (replaces parameter list). */
    private ParserContextBlockNode parameterBlock;
    /** Function body (i.e. var declaration) scope. */
    private Scope bodyScope;

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

    /**
     * If true, this is a provisional function produced by an arrow head cover grammar. Once the
     * ambiguity is resolved, the flag is cleared.
     */
    private boolean coverArrowHead;
    private long yieldOrAwaitInParameters;

    private Module module;
    private TruffleString internalName;

    private List<Map.Entry<VarNode, Scope>> hoistedVarDeclarations;
    private List<Map.Entry<VarNode, Scope>> hoistableBlockFunctionDeclarations;

    /**
     * @param token The token for the function
     * @param ident External function name
     * @param name Internal name of the function
     * @param line The source line of the function
     * @param parameters The parameters of the function
     * @param parentScope The parent scope
     */
    ParserContextFunctionNode(final long token, final IdentNode ident, final TruffleString name, final int line, final int flags,
                    final List<IdentNode> parameters, final int length, Scope parentScope, Scope functionScope) {
        super(flags);
        this.ident = ident;
        this.line = line;
        this.name = name;
        this.parameters = parameters;
        this.token = token;
        this.length = length;
        this.parentScope = parentScope;
        this.bodyScope = functionScope;
        this.parameterCount = parameters == null ? 0 : parameters.size();
        assert calculateLength(parameters) == length;
        assert functionScope == null || (functionScope.isFunctionTopScope() || functionScope.isEvalScope()) : functionScope;
    }

    /**
     * @return Name of the function
     */
    public String getName() {
        return name.toJavaStringUncached();
    }

    public TruffleString getNameTS() {
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
     * @return true if the function is a module.
     */
    public boolean isModule() {
        return getFlag(FunctionNode.IS_MODULE) != 0;
    }

    /**
     * @return true if the function has a direct eval call.
     */
    public boolean hasEval() {
        return getFlag(FunctionNode.HAS_EVAL) != 0;
    }

    /**
     * @return true if the function has nested evals
     */
    public boolean hasNestedEval() {
        return getFlag(FunctionNode.HAS_NESTED_EVAL) != 0;
    }

    /**
     * @return true if the function has a direct eval call nested in an arrow function.
     */
    public boolean hasArrowEval() {
        return getFlag(FunctionNode.HAS_ARROW_EVAL) != 0;
    }

    /**
     * @return true if the function uses this.
     */
    public boolean usesThis() {
        return getFlag(FunctionNode.USES_THIS) != 0;
    }

    /**
     * @return true if the function uses super.
     */
    public boolean usesSuper() {
        return getFlag(FunctionNode.USES_SUPER) != 0;
    }

    /**
     * @return true if the function uses new.target.
     */
    public boolean usesNewTarget() {
        return getFlag(FunctionNode.USES_NEW_TARGET) != 0;
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
            return List.of();
        }
        return parameters;
    }

    void setParameters(List<IdentNode> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return lastToken Function's first token
     */
    public long getFirstToken() {
        return token;
    }

    /**
     * @return lastToken Function's last token
     */
    public long getLastToken() {
        return lastToken;
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
        // Parameters have a temporal dead zone if the parameter list contains expressions.
        boolean tdz = hasParameterExpressions();
        Symbol paramSymbol = new Symbol(bindingIdentifier.getNameTS(), Symbol.IS_LET | Symbol.IS_PARAM | (!tdz ? Symbol.HAS_BEEN_DECLARED : 0));
        if (getParameterScope().putSymbol(paramSymbol) == null) {
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

    public void addParameterInitialization(int lineNumber, Expression assignment, boolean isDefault, boolean isRest) {
        ensureParameterBlock();
        parameterBlock.appendStatement(new ExpressionStatement(lineNumber, assignment.getToken(), assignment.getFinish(), assignment));
        recordParameter(isDefault, isRest, true);
    }

    private void ensureParameterBlock() {
        if (!hasParameterExpressions()) {
            hasParameterExpressions = true;
            initParameterBlock();
        }
    }

    private void initParameterBlock() {
        createParameterBlock();

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                IdentNode paramIdent = parameters.get(i);
                addParameterInit(paramIdent, i);
            }
        }
        parameters = List.of();
    }

    public ParserContextBlockNode createParameterBlock() {
        assert bodyScope == null : "parameter block must be created before body block";
        assert !isScriptOrModule();
        if (parameterBlock != null) {
            return parameterBlock;
        }
        parameterBlock = new ParserContextBlockNode(token, Scope.createFunctionParameter(parentScope, getFlags()));
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
        assert hasParameterExpressions() && getParameterScope().hasSymbol(param.getName());
    }

    public Scope createBodyScope() {
        assert !isScriptOrModule();
        // We only need the parameter scope if the parameter list contains expressions.
        Scope parent;
        if (hasParameterExpressions()) {
            parent = getParameterScope();
            // Note: Not adding 'arguments' yet in order to simplify var redeclaration checks.
            assert !parent.hasSymbol(Parser.ARGUMENTS_NAME) || !parent.getExistingSymbol(Parser.ARGUMENTS_NAME).isArguments();
            parameters = List.of();
        } else {
            parent = parentScope;
        }

        Scope scope = Scope.createFunctionBody(parent, getFlags(), !hasParameterExpressions());
        if (!hasParameterExpressions()) {
            // finalize parameters
            if (parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    IdentNode parameter = parameters.get(i);
                    scope.putSymbol(new Symbol(parameter.getNameTS(), Symbol.IS_VAR | Symbol.IS_PARAM));
                }
            }
        }
        return initBodyScope(scope);
    }

    private Scope initBodyScope(Scope scope) {
        assert this.bodyScope == null && scope != null;
        this.bodyScope = scope;
        return scope;
    }

    public Scope getBodyScope() {
        return bodyScope;
    }

    /**
     * Replace non-strict with strict eval scope.
     */
    public void replaceBodyScope(Scope scope) {
        assert this.bodyScope != null && this.bodyScope.getSymbolCount() == 0 && scope != null;
        this.bodyScope = scope;
    }

    public Scope getParameterScope() {
        return parameterBlock.getScope();
    }

    private boolean needsArguments() {
        return getFlag(FunctionNode.USES_ARGUMENTS | FunctionNode.HAS_EVAL) != 0 &&
                        getFlag(FunctionNode.DEFINES_ARGUMENTS | FunctionNode.IS_ARROW | FunctionNode.IS_CLASS_FIELD_INITIALIZER | FunctionNode.IS_PROGRAM) == 0;
    }

    private boolean hasFunctionSelf() {
        return getFlag(FunctionNode.NO_FUNCTION_SELF) == 0 && !name.isEmpty();
    }

    /**
     * Add a function-level binding if it is not shadowed by a parameter or var declaration.
     */
    private void putFunctionSymbolIfAbsent(String bindingName, TruffleString bindingNameTS, int symbolFlags) {
        assert !isScriptOrModule();
        boolean isArguments = (symbolFlags & Symbol.IS_ARGUMENTS) != 0;
        if (hasParameterExpressions()) {
            // if arguments is used (or eval() in parameters), it must be defined in parameter scope
            Scope parameterScope = getParameterScope();
            if (!parameterScope.hasSymbol(bindingName) && (isArguments || !bodyScope.hasSymbol(bindingName))) {
                parameterScope.putSymbol(new Symbol(bindingNameTS, Symbol.IS_LET | symbolFlags | Symbol.HAS_BEEN_DECLARED));
            } else if (isArguments) {
                // Formal parameter overrides implicit arguments.
                setFlag(FunctionNode.DEFINES_ARGUMENTS);
            }
        } else {
            Symbol existingSymbol = bodyScope.getExistingSymbol(bindingName);
            if (existingSymbol == null) {
                bodyScope.putSymbol(new Symbol(bindingNameTS, Symbol.IS_VAR | symbolFlags | Symbol.HAS_BEEN_DECLARED));
            } else if (isArguments && (existingSymbol.isBlockScoped() || existingSymbol.isParam() || existingSymbol.isHoistableDeclaration())) {
                // Declaration overrides implicit arguments.
                setFlag(FunctionNode.DEFINES_ARGUMENTS);
            }
        }
    }

    public void finishBodyScope(StringPool strings) {
        if (needsArguments()) {
            putFunctionSymbolIfAbsent(Parser.ARGUMENTS_NAME, strings.stringIntern(Parser.ARGUMENTS_NAME_TS), Symbol.IS_ARGUMENTS);
        }
        if (hoistableBlockFunctionDeclarations != null) {
            declareHoistedBlockFunctionDeclarations();
        }
        if (isScriptOrModule()) {
            return;
        }
        if (hasFunctionSelf()) {
            putFunctionSymbolIfAbsent(getName(), getNameTS(), Symbol.IS_FUNCTION_SELF);
        }
        if (!isArrow()) {
            boolean needsThisForEval = hasEval() || hasArrowEval();
            if (usesThis() || usesSuper() || needsThisForEval || getFlag(FunctionNode.HAS_DIRECT_SUPER) != 0) {
                putFunctionSymbolIfAbsent(TokenType.THIS.getName(), strings.stringIntern(TokenType.THIS.getNameTS()), Symbol.IS_THIS);
            }
            if (usesSuper() || (isMethod() && needsThisForEval)) {
                putFunctionSymbolIfAbsent(TokenType.SUPER.getName(), strings.stringIntern(TokenType.SUPER.getNameTS()), Symbol.IS_SUPER);
            }
            if (usesNewTarget() || needsThisForEval) {
                putFunctionSymbolIfAbsent(Parser.NEW_TARGET_NAME, strings.stringIntern(Parser.NEW_TARGET_NAME_TS), Symbol.IS_NEW_TARGET);
            }
        }
        // Close the scopes already to make sure we don't add any more symbols.
        bodyScope.close();
        if (hasParameterExpressions()) {
            getParameterScope().close();
        }
    }

    public String getInternalName() {
        return internalName.toJavaStringUncached();
    }

    public TruffleString getInternalNameTS() {
        return internalName;
    }

    public void setInternalName(TruffleString internalName) {
        this.internalName = internalName;
    }

    public boolean isClassStaticBlock() {
        return getFlag(FunctionNode.IS_CLASS_FIELD_INITIALIZER) != 0;
    }

    public boolean isCoverArrowHead() {
        return coverArrowHead;
    }

    public void setCoverArrowHead(boolean coverArrowHead) {
        this.coverArrowHead = coverArrowHead;
    }

    public void setYieldOrAwaitInParameters(long yieldOrAwaitInParameters) {
        // Record only the first yield or await token.
        assert this.yieldOrAwaitInParameters == 0L;
        this.yieldOrAwaitInParameters = yieldOrAwaitInParameters;
    }

    public long getYieldOrAwaitInParameters() {
        return yieldOrAwaitInParameters;
    }

    public void recordHoistedVarDeclaration(final VarNode varDecl, final Scope scope) {
        assert !varDecl.isBlockScoped();
        assert scope.isBlockScope();
        if (hoistedVarDeclarations == null) {
            hoistedVarDeclarations = new ArrayList<>();
        }
        hoistedVarDeclarations.add(new AbstractMap.SimpleImmutableEntry<>(varDecl, scope));
    }

    public VarNode verifyHoistedVarDeclarations() {
        if (!hasHoistedVarDeclarations()) {
            // nothing to do
            return null;
        }
        for (Map.Entry<VarNode, Scope> entry : hoistedVarDeclarations) {
            VarNode varDecl = entry.getKey();
            Scope declScope = entry.getValue();
            String varName = varDecl.getName().getName();
            for (Scope current = declScope; current != bodyScope; current = current.getParent()) {
                Symbol existing = current.getExistingSymbol(varName);
                if (existing != null && existing.isBlockScoped()) {
                    if (existing.isCatchParameter()) {
                        continue; // B.3.5 VariableStatements in Catch Blocks
                    }
                    // let the caller throw the error
                    return varDecl;
                }
            }
        }
        return null;
    }

    public boolean hasHoistedVarDeclarations() {
        return hoistedVarDeclarations != null;
    }

    public void recordHoistableBlockFunctionDeclaration(final VarNode functionDeclaration, final Scope scope) {
        assert functionDeclaration.isFunctionDeclaration() && functionDeclaration.isBlockScoped();
        if (hoistableBlockFunctionDeclarations == null) {
            hoistableBlockFunctionDeclarations = new ArrayList<>();
        }
        hoistableBlockFunctionDeclarations.add(new AbstractMap.SimpleImmutableEntry<>(functionDeclaration, scope));
    }

    public void declareHoistedBlockFunctionDeclarations() {
        if (hoistableBlockFunctionDeclarations == null) {
            // nothing to do
            return;
        }
        next: for (Map.Entry<VarNode, Scope> entry : hoistableBlockFunctionDeclarations) {
            VarNode functionDecl = entry.getKey();
            Scope functionDeclScope = entry.getValue();
            String varName = functionDecl.getName().getName();
            for (Scope current = functionDeclScope.getParent(); current != null; current = current.getParent()) {
                Symbol existing = current.getExistingSymbol(varName);
                if (existing != null && (existing.isBlockScoped() && !existing.isCatchParameter())) {
                    // lexical declaration found, do not hoist
                    continue next;
                }
                if (current.isFunctionBodyScope()) {
                    break;
                }
            }
            // declare var (if not already declared) and hoist the function declaration
            if (bodyScope.getExistingSymbol(varName) == null) {
                int symbolFlags = Symbol.IS_VAR | (bodyScope.isGlobalScope() ? Symbol.IS_GLOBAL : 0);
                if (hasParameterExpressions() && getParameterScope().hasSymbol(functionDecl.getName().getName())) {
                    /**
                     * Since parameterNames are excluded from function hoisting, this case may only
                     * happen for the implicit "arguments" binding. The var binding created for the
                     * hoisted function is initialized with the value from the top-level env.
                     */
                    assert Parser.ARGUMENTS_NAME.equals(functionDecl.getName().getName()) : functionDecl;
                    symbolFlags |= Symbol.IS_VAR_REDECLARED_HERE;
                }
                bodyScope.putSymbol(new Symbol(functionDecl.getName().getNameTS(), symbolFlags));
            }
            functionDeclScope.getExistingSymbol(varName).setHoistedBlockFunctionDeclaration();
        }
    }

    /**
     * Propagate relevant flags to the enclosing function.
     */
    public void propagateFlagsToParent(ParserContextFunctionNode parent) {
        // Propagate the presence of eval to all parents.
        if (hasEval() || hasNestedEval()) {
            parent.setFlag(FunctionNode.HAS_NESTED_EVAL | FunctionNode.HAS_SCOPE_BLOCK);
        }
        if (isArrow()) {
            // Propagate the presence of eval to the first non-arrow and all arrow parents in
            // between as HAS_ARROW_EVAL.
            // This ensures that `this`, `new.target`, and `super` are available to the eval
            // even if the parent does not use them (and does not have an eval itself).
            // e.g.:
            // function fun(){ return (() => eval("this"))(); };
            // function fun(){ return eval("() => this")(); };
            // (() => (() => eval("this"))())();
            if (hasEval() || hasArrowEval()) {
                parent.setFlag(FunctionNode.HAS_ARROW_EVAL);
            }
            // Propagate use of `this` up to the first non-arrow and all arrow parents in between.
            if (usesThis()) {
                parent.setFlag(FunctionNode.USES_THIS);
            }
        }
        parent.setFlag(FunctionNode.HAS_CLOSURES);
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
