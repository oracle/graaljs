/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.js.parser.Source;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

// @formatter:off
/**
 * IR representation for function (or script.)
 */
public final class FunctionNode extends LexicalContextExpression implements Flags<FunctionNode> {

    /** Function kinds */
    public enum Kind {
        /** a normal function - nothing special */
        NORMAL,
        /** a script function */
        SCRIPT,
        /** a getter function */
        GETTER,
        /** a setter function */
        SETTER,
        /** an arrow function */
        ARROW,
        /** a generator function */
        GENERATOR,
        /** a module function */
        MODULE,
    }

    /** Source of entity. */
    private final Source source;

    /**
     * Opaque object representing parser state at the end of the function. Used when reparsing outer functions
     * to skip parsing inner functions.
     */
    private final Object endParserState;

    /** External function identifier. */
    /*@Ignore*/ private final IdentNode ident;

    /** The body of the function node */
    private final Block body;

    /** Internal function name. */
    private final String name;

    /** Function kind. */
    private final Kind kind;

    /** List of parameters. */
    private final List<IdentNode> parameters;

    /** First token of function. **/
    private final long firstToken;

    /** Last token of function. **/
    private final long lastToken;

    /** Number of properties of "this" object assigned in this function */
    /*@Ignore*/ private final int thisProperties;

    /** Function flags. */
    private final int flags;

    /** Line number of function start */
    private final int lineNumber;

    /** Number of formal parameters (including optional ones). */
    private final int numOfParams;

    /** The typical number of arguments expected by the function. */
    private final int length;

    private final Module module;

    private boolean analyzed;
    private boolean usesAncestorScope;

    /** Is anonymous function flag. */
    public static final int IS_ANONYMOUS = 1 << 0;

    /** Is the function created in a function declaration (as opposed to a function expression) */
    public static final int IS_DECLARED = 1 << 1;

    /** is this a strict mode function? */
    public static final int IS_STRICT = 1 << 2;

    /** Does the function use the "arguments" identifier ? */
    public static final int USES_ARGUMENTS = 1 << 3;

    /** Has this function been split because it was too large? */
    public static final int IS_SPLIT = 1 << 4;

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

    /** Does this function have nested declarations? */
    public static final int HAS_FUNCTION_DECLARATIONS = 1 << 10;

    /** Are we vararg, but do we just pass the arguments along to apply or call */
    public static final int HAS_APPLY_TO_CALL_SPECIALIZATION = 1 << 12;

    /**
     * Is this function the top-level program?
     */
    public static final int IS_PROGRAM = 1 << 13;

    /**
     * Flag indicating whether this function uses the local variable symbol for itself. Only named
     * function expressions can have this flag set if they reference themselves (e.g.
     * "(function f() { return f })". Declared functions will use the symbol in their parent scope
     * instead when they reference themselves by name.
     */
    public static final int USES_SELF_SYMBOL = 1 << 14;

    /** Does this function use the "this" keyword? */
    public static final int USES_THIS = 1 << 15;

    /** Does this function or any nested functions contain an eval? */
    private static final int HAS_DEEP_EVAL = HAS_EVAL | HAS_NESTED_EVAL;

    /**
     * Does this function potentially need "arguments"? Note that this is not a full test, as
     * further negative check of REDEFINES_ARGS is needed.
     */
    private static final int MAYBE_NEEDS_ARGUMENTS = USES_ARGUMENTS | HAS_EVAL;

    /** Does this function need the parent scope? It needs it if either it or its descendants use variables from it, or have a deep eval, or it's the program. */
    public static final int NEEDS_PARENT_SCOPE = USES_ANCESTOR_SCOPE | HAS_DEEP_EVAL | IS_PROGRAM;

    /** Does this function contain a super call? (cf. ES6 14.3.5 Static Semantics: HasDirectSuper) */
    public static final int HAS_DIRECT_SUPER = 1 << 18;

    /** Does this function use the super binding? */
    public static final int USES_SUPER = 1 << 19;

    /** Is this function a (class or object) method? */
    public static final int IS_METHOD = 1 << 20;

    /** Is this the constructor method? */
    public static final int IS_CLASS_CONSTRUCTOR = 1 << 21;

    /** Is this the constructor of a subclass (i.e., a class with an extends declaration)? */
    public static final int IS_SUBCLASS_CONSTRUCTOR = 1 << 22;

    /** Does this function use new.target? */
    public static final int USES_NEW_TARGET = 1 << 23;

    /** Is it a statement? */
    public static final int IS_STATEMENT = 1 << 24;

    /** Is it an async function? */
    public static final int IS_ASYNC = 1 << 25;

    /** Flag indicating that this function has a non-simple parameter list. */
    public static final int HAS_NON_SIMPLE_PARAMETER_LIST = 1 << 26;

    /**
     * Constructor
     *
     * @param source     the source
     * @param lineNumber line number
     * @param token      token
     * @param finish     finish
     * @param firstToken first token of the function node (including the function declaration)
     * @param lastToken  lastToken
     * @param ident      the identifier
     * @param name       the name of the function
     * @param parameters parameter list
     * @param kind       kind of function as in {@link FunctionNode.Kind}
     * @param flags      initial flags
     * @param body       body of the function
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
        final FunctionNode.Kind kind,
        final int flags,
        final Block body,
        final Object endParserState,
        final Module module) {
        super(token, Token.descPosition(firstToken), finish);

        this.source           = source;
        this.lineNumber       = lineNumber;
        this.ident            = ident;
        this.name             = name;
        this.kind             = kind;
        this.length           = length;
        this.numOfParams      = numOfParams;
        this.parameters       = parameters;
        this.firstToken       = firstToken;
        this.lastToken        = lastToken;
        this.flags            = flags;
        this.body             = body;
        this.thisProperties   = 0;
        this.endParserState   = endParserState;
        this.module           = module;
    }

    private FunctionNode(
        final FunctionNode functionNode,
        final long lastToken,
        final Object endParserState,
        final int flags,
        final String name,
        final Block body,
        final List<IdentNode> parameters,
        final int thisProperties,
        final Source source) {
        super(functionNode);

        this.endParserState   = endParserState;
        this.lineNumber       = functionNode.lineNumber;
        this.flags            = flags;
        this.name             = name;
        this.lastToken        = lastToken;
        this.body             = body;
        this.parameters       = parameters;
        this.thisProperties   = thisProperties;
        this.source           = source;

        // the fields below never change - they are final and assigned in constructor
        this.ident           = functionNode.ident;
        this.kind            = functionNode.kind;
        this.firstToken      = functionNode.firstToken;
        this.length          = functionNode.length;
        this.numOfParams     = functionNode.numOfParams;
        this.module          = functionNode.module;
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

        if (ident != null) {
            sb.append(' ');
            ident.toString(sb, printTypes);
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
        return Node.replaceInLexicalContext(
                lc,
                this,
                new FunctionNode(
                        this,
                        lastToken,
                        endParserState,
                        flags,
                        name,
                        body,
                        parameters,
                        thisProperties,
                        source));
    }

    @Override
    public FunctionNode setFlag(final LexicalContext lc, final int flag) {
        return setFlags(lc, flags | flag);
    }

    /**
     * Returns true if the function is the top-level program.
     * @return True if this function node represents the top-level program.
     */
    public boolean isProgram() {
        return getFlag(IS_PROGRAM);
    }

    /**
     * Check if this function has a call expression for the identifier "eval" (that is, {@code eval(...)}).
     *
     * @return true if {@code eval} is called.
     */
    public boolean hasEval() {
        return getFlag(HAS_EVAL);
    }

    /**
     * Get the first token for this function
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
     * @return the identifier as an IdentityNode
     */
    public IdentNode getIdent() {
        return ident;
    }

    /**
     * Get the function body, i.e., the top-most block of the function.
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
     * @param lc lexical context
     * @param body new body
     * @return new function node if body changed, same if not
     */
    public FunctionNode setBody(final LexicalContext lc, final Block body) {
        if (this.body == body) {
            return this;
        }
        return Node.replaceInLexicalContext(
                lc,
                this,
                new FunctionNode(
                        this,
                        lastToken,
                        endParserState,
                        flags |
                            (body.needsScope() ?
                                    FunctionNode.HAS_SCOPE_BLOCK :
                                    0),
                        name,
                        body,
                        parameters,
                        thisProperties,
                        source));
    }

    /**
     * Does this function's method needs to be variable arity (gather all script-declared parameters in a final
     * {@code Object[]} parameter. Functions that need to have the "arguments" object as well as functions that simply
     * declare too many arguments for JVM to handle with fixed arity will need to be variable arity.
     * @return true if the Java method in the generated code that implements this function needs to be variable arity.
     * @see #needsArguments()
     */
    public boolean isVarArg() {
        return needsArguments() /* || parameters.size() > LinkerCallSite.ARGLIMIT */;
    }

    /**
     * Check whether a function would need dynamic scope, which is does if it has
     * evals and isn't strict.
     * @return true if dynamic scope is needed
     */
    public boolean needsDynamicScope() {
        // Function has a direct eval in it (so a top-level "var ..." in the eval code can introduce a new
        // variable into the function's scope), and it isn't strict (as evals in strict functions get an
        // isolated scope).
        return hasEval() && !isStrict();
    }

    /**
     * Returns true if this function needs to have an Arguments object defined as a local variable named "arguments".
     * Functions that use "arguments" as identifier and don't define it as a name of a parameter or a nested function
     * (see ECMAScript 5.1 Chapter 10.5), as well as any function that uses eval or with, or has a nested function that
     * does the same, will have an "arguments" object. Also, if this function is a script, it will not have an
     * "arguments" object, because it does not have local variables; rather the Global object will have an explicit
     * "arguments" property that provides command-line arguments for the script.
     * @return true if this function needs an arguments object.
     */
    public boolean needsArguments() {
        // uses "arguments" or calls eval, but it does not redefine "arguments", and finally, it's not a script, since
        // for top-level script, "arguments" is picked up from Context by Global.init() instead.
        return getFlag(MAYBE_NEEDS_ARGUMENTS) && !getFlag(DEFINES_ARGUMENTS) && !isProgram();
    }

    /**
     * Get the number of properties assigned to the this object in this function.
     * @return number of properties
     */
    public int getThisProperties() {
        return thisProperties;
    }

    /**
     * Return the kind of this function
     * @see FunctionNode.Kind
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Return the last token for this function's code
     * @return last token
     */
    public long getLastToken() {
        return lastToken;
    }

    /**
     * Returns the end parser state for this function.
     * @return the end parser state for this function.
     */
    public Object getEndParserState() {
        return endParserState;
    }

    /**
     * Get the name of this function
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this function is split into several smaller fragments.
     *
     * @return true if this function is split into several smaller fragments.
     */
    public boolean isSplit() {
        return getFlag(IS_SPLIT);
    }

    /**
     * Get the parameters to this function
     * @return a list of IdentNodes which represent the function parameters, in order
     */
    public List<IdentNode> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Return the number of parameters to this function
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
     * Check if this function is created as a function declaration (as opposed to function expression)
     * @return true if function is declared.
     */
    public boolean isDeclared() {
        return getFlag(IS_DECLARED);
    }

    /**
     * Check if this function is anonymous
     * @return true if function is anonymous
     */
    public boolean isAnonymous() {
        return getFlag(IS_ANONYMOUS);
    }

    /**
     * Returns true if this is a named function expression (that is, it isn't a declared function, it isn't an
     * anonymous function expression, it isn't a method, and it isn't a program).
     * @return true if this is a named function expression
     */
    public boolean isNamedFunctionExpression() {
        return !getFlag(IS_PROGRAM | IS_ANONYMOUS | IS_DECLARED | IS_METHOD);
    }

    /**
     * Check if the function is generated in strict mode
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

    public boolean isSubclassConstructor() {
        return getFlag(IS_SUBCLASS_CONSTRUCTOR);
    }

    public boolean usesNewTarget() {
        return getFlag(USES_NEW_TARGET);
    }

    public boolean isModule() {
        return kind == Kind.MODULE;
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

    public boolean isAnalyzed() {
        return analyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }

    public boolean usesAncestorScope() {
        return usesAncestorScope;
    }

    public void setUsesAncestorScope(boolean usesAncestorScope) {
        this.usesAncestorScope = usesAncestorScope;
    }

    boolean isFunctionDeclaration() {
        return isDeclared() && kind == FunctionNode.Kind.NORMAL && !isAsync();
    }
}
