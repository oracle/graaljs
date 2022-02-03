/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.oracle.js.parser.Source;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

/**
 * IR representation for function (or script.)
 */
public final class FunctionNode extends LexicalContextExpression implements Flags<FunctionNode> {

    /** Source of entity. */
    private final Source source;

    /**
     * Opaque object representing parser state at the end of the function. Used when reparsing outer
     * functions to skip parsing inner functions.
     */
    private final Object endParserState;

    /** External function identifier. */
    private final IdentNode ident;

    /** The body of the function node */
    private final Block body;

    /** Function name. */
    private final String name;

    /** List of parameters. */
    private final List<IdentNode> parameters;

    /** First token of function. **/
    private final long firstToken;

    /** Last token of function. **/
    private final long lastToken;

    /** Function flags. */
    private final int flags;

    /** Line number of function start */
    private final int lineNumber;

    /** Number of formal parameters (including optional ones). */
    private final int numOfParams;

    /** The typical number of arguments expected by the function. */
    private final int length;

    private final Module module;

    /** Optional internal/inferred function name. */
    private final String internalName;

    private boolean usesAncestorScope;

    /** Is anonymous function flag. */
    public static final int IS_ANONYMOUS = 1 << 0;

    /** Is the function created in a function declaration (as opposed to a function expression) */
    public static final int IS_DECLARED = 1 << 1;

    /** is this a strict mode function? */
    public static final int IS_STRICT = 1 << 2;

    /** Does the function use the "arguments" identifier ? */
    public static final int USES_ARGUMENTS = 1 << 3;

    /** Is it a statement? */
    public static final int IS_STATEMENT = 1 << 4;

    /**
     * Does the function call eval? If it does, then all variables in this function might be get/set
     * by it and it can introduce new variables into this function's scope too.
     */
    public static final int HAS_EVAL = 1 << 5;

    /**
     * Does a nested function contain eval? If it does, then all variables in this function might be
     * get/set by it.
     */
    public static final int HAS_NESTED_EVAL = 1 << 6;

    /**
     * Does this function have any blocks that create a scope? This is used to determine if the
     * function needs to have a local variable slot for the scope symbol.
     */
    public static final int HAS_SCOPE_BLOCK = 1 << 7;

    /**
     * Flag this function as one that defines the identifier "arguments" as a function parameter or
     * nested function name. This precludes it from needing to have an Arguments object defined as
     * "arguments" local variable. Note that defining a local variable named "arguments" still
     * requires construction of the Arguments object (see ECMAScript 5.1 Chapter 10.5).
     *
     * @see #needsArguments()
     */
    public static final int DEFINES_ARGUMENTS = 1 << 8;

    /**
     * Does this function or any of its descendants use variables from an ancestor function's scope
     * (incl. globals)?
     */
    public static final int USES_ANCESTOR_SCOPE = 1 << 9;

    /** A script function */
    public static final int IS_SCRIPT = 1 << 10;

    /** A getter function */
    public static final int IS_GETTER = 1 << 11;

    /** A setter function */
    public static final int IS_SETTER = 1 << 12;

    /**
     * Is this function the top-level program?
     */
    public static final int IS_PROGRAM = 1 << 13;

    /** Does this function have closures? */
    public static final int HAS_CLOSURES = 1 << 14;

    /** Does this function use the "this" keyword? */
    public static final int USES_THIS = 1 << 15;

    /** Does this function or any nested functions contain an eval? */
    private static final int HAS_DEEP_EVAL = HAS_EVAL | HAS_NESTED_EVAL;

    /**
     * Does this function potentially need "arguments"? Note that this is not a full test, as
     * further negative check of REDEFINES_ARGS is needed.
     */
    private static final int MAYBE_NEEDS_ARGUMENTS = USES_ARGUMENTS | HAS_EVAL;

    /**
     * Does this function need the parent scope? It needs it if either it or its descendants use
     * variables from it, or have a deep eval, or it's the program.
     */
    public static final int NEEDS_PARENT_SCOPE = USES_ANCESTOR_SCOPE | HAS_DEEP_EVAL | IS_PROGRAM;

    /** An arrow function */
    public static final int IS_ARROW = 1 << 16;

    /** A module function */
    public static final int IS_MODULE = 1 << 17;

    /**
     * Does this function contain a super call? (cf. ES6 14.3.5 Static Semantics: HasDirectSuper)
     */
    public static final int HAS_DIRECT_SUPER = 1 << 18;

    /** Does this function use the super binding? */
    public static final int USES_SUPER = 1 << 19;

    /** Is this function a (class or object) method? */
    public static final int IS_METHOD = 1 << 20;

    /** If one of these flags are set, this function does not have a function self binding. */
    public static final int NO_FUNCTION_SELF = IS_PROGRAM | IS_ANONYMOUS | IS_DECLARED | IS_METHOD;

    /** Is this the constructor method? */
    public static final int IS_CLASS_CONSTRUCTOR = 1 << 21;

    /** Is this the constructor of a subclass (i.e., a class with an extends declaration)? */
    public static final int IS_DERIVED_CONSTRUCTOR = 1 << 22;

    /** Does this function use new.target? */
    public static final int USES_NEW_TARGET = 1 << 23;

    /** A generator function */
    public static final int IS_GENERATOR = 1 << 24;

    /** Is it an async function? */
    public static final int IS_ASYNC = 1 << 25;

    /** Flag indicating that this function has a non-simple parameter list. */
    public static final int HAS_NON_SIMPLE_PARAMETER_LIST = 1 << 26;

    /** Flag indicating that this function has an eval nested in an arrow function. */
    public static final int HAS_ARROW_EVAL = 1 << 27;

    /** Does this function have nested declarations? */
    public static final int HAS_FUNCTION_DECLARATIONS = 1 << 28;

    /** Does this function contain a {@code fn.apply(_, arguments)} call? */
    public static final int HAS_APPLY_ARGUMENTS_CALL = 1 << 29;

    /** Is this function a class field/static initializer? */
    public static final int IS_CLASS_FIELD_INITIALIZER = 1 << 30;

    /**
     * All flags that may be set during parsing of an arrow head cover grammar and that have to be
     * propagated to the enclosing function if the expression ends up not being an arrow function.
     */
    public static final int ARROW_HEAD_FLAGS = USES_THIS | USES_ARGUMENTS | USES_SUPER |
                    HAS_EVAL | HAS_ARROW_EVAL | HAS_NESTED_EVAL | HAS_SCOPE_BLOCK | HAS_CLOSURES;

    /**
     * Constructor
     *
     * @param source the source
     * @param lineNumber line number
     * @param token token
     * @param finish finish
     * @param firstToken first token of the function node (including the function declaration)
     * @param lastToken lastToken
     * @param ident the identifier
     * @param name the name of the function
     * @param parameters parameter list
     * @param flags initial flags
     * @param body body of the function
     * @param endParserState The parser state at the end of the parsing.
     */
    public FunctionNode(
                    final Source source,
                    final int lineNumber,
                    final long token,
                    final int finish,
                    final long firstToken,
                    final long lastToken,
                    final IdentNode ident,
                    final String name,
                    final int length,
                    final int numOfParams,
                    final List<IdentNode> parameters,
                    final int flags,
                    final Block body,
                    final Object endParserState,
                    final Module module,
                    final String internalName) {
        super(token, Token.descPosition(firstToken), finish);

        this.source = source;
        this.lineNumber = lineNumber;
        this.ident = ident;
        this.name = Objects.requireNonNull(name);
        this.length = length;
        this.numOfParams = numOfParams;
        this.parameters = parameters;
        this.firstToken = firstToken;
        this.lastToken = lastToken;
        this.flags = flags;
        this.body = body;
        this.endParserState = endParserState;
        this.module = module;
        this.internalName = internalName;
    }

    private FunctionNode(
                    final FunctionNode functionNode,
                    final long lastToken,
                    final Object endParserState,
                    final int flags,
                    final String name,
                    final Block body,
                    final List<IdentNode> parameters,
                    final Source source) {
        super(functionNode);

        this.endParserState = endParserState;
        this.lineNumber = functionNode.lineNumber;
        this.flags = flags;
        this.name = Objects.requireNonNull(name);
        this.lastToken = lastToken;
        this.body = body;
        this.parameters = parameters;
        this.source = source;

        // the fields below never change - they are final and assigned in constructor
        this.ident = functionNode.ident;
        this.firstToken = functionNode.firstToken;
        this.length = functionNode.length;
        this.numOfParams = functionNode.numOfParams;
        this.module = functionNode.module;
        this.internalName = functionNode.internalName;
    }

    @Override
    public Node accept(final LexicalContext lc, final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterFunctionNode(this)) {
            return visitor.leaveFunctionNode(setBody(lc, (Block) body.accept(visitor)));
        }
        return this;
    }

    @Override
    public <R> R accept(LexicalContext lc, TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterFunctionNode(this);
    }

    /**
     * Get the source for this function
     *
     * @return the source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Get the unique ID for this function within the script file.
     *
     * @return the id
     */
    public int getId() {
        return isProgram() ? -1 : Token.descPosition(firstToken);
    }

    /**
     * get source name - sourceURL or name derived from Source.
     *
     * @return name for the script source
     */
    public String getSourceName() {
        return getSourceName(source);
    }

    /**
     * Static source name getter
     *
     * @param source the source
     * @return source name
     */
    public static String getSourceName(final Source source) {
        final String explicitURL = source.getExplicitURL();
        return explicitURL != null ? explicitURL : source.getName();
    }

    /**
     * Returns the line number.
     *
     * @return the line number.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public void toString(final StringBuilder sb, final boolean printTypes) {
        if (isAsync()) {
            sb.append("async ");
        }
        sb.append("function");
        if (isGenerator()) {
            sb.append('*');
        }

        if (ident != null) {
            sb.append(' ');
            ident.toString(sb, printTypes);
        } else if (!name.isEmpty()) {
            sb.append(' ').append(name);
        } else if (internalName != null && !internalName.isEmpty()) {
            sb.append(' ').append(internalName);
        }

        toStringTail(sb, printTypes);
    }

    void toStringTail(final StringBuilder sb, final boolean printTypes) {
        sb.append('(');

        for (final Iterator<IdentNode> iter = parameters.iterator(); iter.hasNext();) {
            final IdentNode parameter = iter.next();
            parameter.toString(sb, printTypes);
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(')');
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public boolean getFlag(final int flag) {
        return (flags & flag) != 0;
    }

    @Override
    public FunctionNode setFlags(final LexicalContext lc, final int flags) {
        if (this.flags == flags) {
            return this;
        }
        return Node.replaceInLexicalContext(lc, this, new FunctionNode(
                        this,
                        lastToken,
                        endParserState,
                        flags,
                        name,
                        body,
                        parameters,
                        source));
    }

    @Override
    public FunctionNode setFlag(final LexicalContext lc, final int flag) {
        return setFlags(lc, flags | flag);
    }

    /**
     * Returns true if the function is the top-level program.
     *
     * @return True if this function node represents the top-level program.
     */
    public boolean isProgram() {
        return getFlag(IS_PROGRAM);
    }

    /**
     * Check if this function has a call expression for the identifier "eval" (that is,
     * {@code eval(...)}).
     *
     * @return true if {@code eval} is called.
     */
    public boolean hasEval() {
        return getFlag(HAS_EVAL);
    }

    /**
     * Get the first token for this function
     *
     * @return the first token
     */
    public long getFirstToken() {
        return firstToken;
    }

    /**
     * Return {@code true} if this function makes use of the {@code this} object.
     *
     * @return true if function uses {@code this} object
     */
    public boolean usesThis() {
        return getFlag(USES_THIS);
    }

    /**
     * Get the identifier for this function, this is its symbol.
     *
     * @return the identifier as an IdentityNode
     */
    public IdentNode getIdent() {
        return ident;
    }

    /**
     * Get the function body, i.e., the top-most block of the function.
     *
     * @return the function body
     */
    public Block getBody() {
        return body;
    }

    /**
     * Get the {@code var} declaration block, i.e., the actual function body, which is either
     * {@link #getBody()} or the next block after skipping the parameter initialization block.
     */
    public Block getVarDeclarationBlock() {
        if (body.isParameterBlock()) {
            return ((BlockStatement) body.getLastStatement()).getBlock();
        }
        return body;
    }

    /**
     * Reset the function body
     *
     * @param lc lexical context
     * @param body new body
     * @return new function node if body changed, same if not
     */
    public FunctionNode setBody(final LexicalContext lc, final Block body) {
        if (this.body == body) {
            return this;
        }
        return Node.replaceInLexicalContext(lc, this, new FunctionNode(
                        this,
                        lastToken,
                        endParserState,
                        flags | (body.needsScope() ? FunctionNode.HAS_SCOPE_BLOCK : 0),
                        name,
                        body,
                        parameters,
                        source));
    }

    /**
     * Check whether a function would need dynamic scope, which is does if it has evals and isn't
     * strict.
     *
     * @return true if dynamic scope is needed
     */
    public boolean needsDynamicScope() {
        // Function has a direct eval in it (so a top-level "var ..." in the eval code can introduce
        // a new variable into the function's scope), and it isn't strict (as evals in strict
        // functions get an isolated scope).
        return hasEval() && !isStrict();
    }

    /**
     * Returns true if this function needs to have an Arguments object defined as a local variable
     * named "arguments". Functions that use "arguments" as identifier and don't define it as a name
     * of a parameter or a nested function (see ECMAScript 5.1 Chapter 10.5), as well as any
     * function that uses eval or with, or has a nested function that does the same, will have an
     * "arguments" object. Also, if this function is a script, it will not have an "arguments"
     * object, because it does not have local variables; rather the Global object will have an
     * explicit "arguments" property that provides command-line arguments for the script.
     *
     * @return true if this function needs an arguments object.
     */
    public boolean needsArguments() {
        // uses "arguments" or calls eval, but it does not redefine "arguments", and finally, it's
        // not a script, since for top-level scripts, "arguments" is picked up from a global
        // property instead.
        return getFlag(MAYBE_NEEDS_ARGUMENTS) && !getFlag(DEFINES_ARGUMENTS | IS_ARROW | IS_CLASS_FIELD_INITIALIZER) && !isProgram();
    }

    /**
     * Return the last token for this function's code
     *
     * @return last token
     */
    public long getLastToken() {
        return lastToken;
    }

    /**
     * Returns the end parser state for this function.
     *
     * @return the end parser state for this function.
     */
    public Object getEndParserState() {
        return endParserState;
    }

    /**
     * Get the name of this function
     *
     * @return the name
     */
    public String getName() {
        if (!isAnonymous()) {
            return getIdent().getName();
        }
        return name;
    }

    /**
     * Set the name of this function.
     *
     * @param lc lexical context
     * @param name new name
     * @return new function node if changed, otherwise the same
     */
    public FunctionNode setName(final LexicalContext lc, final String name) {
        if (this.name.equals(name)) {
            return this;
        }
        return Node.replaceInLexicalContext(lc, this, new FunctionNode(
                        this,
                        lastToken,
                        endParserState,
                        flags,
                        name,
                        body,
                        parameters,
                        source));
    }

    public String getInternalName() {
        return internalName;
    }

    /**
     * Get the parameters to this function
     *
     * @return a list of IdentNodes which represent the function parameters, in order
     */
    public List<IdentNode> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Return the number of parameters to this function
     *
     * @return the number of parameters
     */
    public int getNumOfParams() {
        return numOfParams;
    }

    /**
     * The value of the function's length property, i.e., the typical number of arguments expected
     * by the function.
     */
    public int getLength() {
        return length;
    }

    /**
     * Check if this function is created as a function declaration (as opposed to function
     * expression)
     *
     * @return true if function is declared.
     */
    public boolean isDeclared() {
        return getFlag(IS_DECLARED);
    }

    /**
     * Check if this function is anonymous
     *
     * @return true if function is anonymous
     */
    public boolean isAnonymous() {
        return getFlag(IS_ANONYMOUS);
    }

    /**
     * Returns true if this is a named function expression (that is, it isn't a declared function,
     * it isn't an anonymous function expression, it isn't a method, and it isn't a program).
     *
     * @return true if this is a named function expression
     */
    public boolean isNamedFunctionExpression() {
        return !getFlag(NO_FUNCTION_SELF);
    }

    /**
     * Check if the function is generated in strict mode
     *
     * @return true if strict mode enabled for function
     */
    public boolean isStrict() {
        return getFlag(IS_STRICT);
    }

    public boolean isMethod() {
        return getFlag(IS_METHOD);
    }

    public boolean usesSuper() {
        return getFlag(USES_SUPER);
    }

    public boolean hasDirectSuper() {
        return getFlag(HAS_DIRECT_SUPER);
    }

    public boolean isClassConstructor() {
        return getFlag(IS_CLASS_CONSTRUCTOR);
    }

    public boolean isDerivedConstructor() {
        return getFlag(IS_DERIVED_CONSTRUCTOR);
    }

    public boolean usesNewTarget() {
        return getFlag(USES_NEW_TARGET);
    }

    public boolean isScript() {
        return getFlag(IS_SCRIPT);
    }

    public boolean isGetter() {
        return getFlag(IS_GETTER);
    }

    public boolean isSetter() {
        return getFlag(IS_SETTER);
    }

    public boolean isArrow() {
        return getFlag(IS_ARROW);
    }

    public boolean isGenerator() {
        return getFlag(IS_GENERATOR);
    }

    public boolean isModule() {
        return getFlag(IS_MODULE);
    }

    public Module getModule() {
        return module;
    }

    public boolean isStatement() {
        return getFlag(IS_STATEMENT);
    }

    public boolean isAsync() {
        return getFlag(IS_ASYNC);
    }

    public boolean hasSimpleParameterList() {
        return !getFlag(HAS_NON_SIMPLE_PARAMETER_LIST);
    }

    public boolean usesAncestorScope() {
        return usesAncestorScope;
    }

    public void setUsesAncestorScope(boolean usesAncestorScope) {
        this.usesAncestorScope = usesAncestorScope;
    }

    public boolean isNormal() {
        return !getFlag(IS_SCRIPT | IS_MODULE | IS_GETTER | IS_SETTER | IS_METHOD | IS_ARROW | IS_GENERATOR | IS_ASYNC);
    }

    boolean isFunctionDeclaration() {
        return isDeclared() && isNormal();
    }

    public boolean hasApplyArgumentsCall() {
        return getFlag(HAS_APPLY_ARGUMENTS_CALL);
    }

    public boolean hasArrowEval() {
        return getFlag(HAS_ARROW_EVAL);
    }

    public boolean needsThis() {
        return usesThis() || hasDirectSuper() || (hasEval() || hasArrowEval());
    }

    public boolean needsNewTarget() {
        return usesNewTarget() || hasDirectSuper() || (!isArrow() && !isProgram() && (hasEval() || hasArrowEval()));
    }

    public boolean needsSuper() {
        return usesSuper() || (isMethod() && (hasEval() || hasArrowEval()));
    }

    public boolean isClassFieldInitializer() {
        return getFlag(IS_CLASS_FIELD_INITIALIZER);
    }

    public boolean hasClosures() {
        return getFlag(HAS_CLOSURES);
    }
}
