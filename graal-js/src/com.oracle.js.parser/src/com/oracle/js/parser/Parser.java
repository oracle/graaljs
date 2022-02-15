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

package com.oracle.js.parser;

import static com.oracle.js.parser.TokenType.ARROW;
import static com.oracle.js.parser.TokenType.AS;
import static com.oracle.js.parser.TokenType.ASSERT;
import static com.oracle.js.parser.TokenType.ASSIGN;
import static com.oracle.js.parser.TokenType.ASSIGN_INIT;
import static com.oracle.js.parser.TokenType.ASYNC;
import static com.oracle.js.parser.TokenType.AWAIT;
import static com.oracle.js.parser.TokenType.CASE;
import static com.oracle.js.parser.TokenType.CATCH;
import static com.oracle.js.parser.TokenType.CLASS;
import static com.oracle.js.parser.TokenType.COLON;
import static com.oracle.js.parser.TokenType.COMMARIGHT;
import static com.oracle.js.parser.TokenType.COMMENT;
import static com.oracle.js.parser.TokenType.CONST;
import static com.oracle.js.parser.TokenType.DECPOSTFIX;
import static com.oracle.js.parser.TokenType.DECPREFIX;
import static com.oracle.js.parser.TokenType.ELLIPSIS;
import static com.oracle.js.parser.TokenType.ELSE;
import static com.oracle.js.parser.TokenType.EOF;
import static com.oracle.js.parser.TokenType.EOL;
import static com.oracle.js.parser.TokenType.EQ_STRICT;
import static com.oracle.js.parser.TokenType.ESCSTRING;
import static com.oracle.js.parser.TokenType.EXPORT;
import static com.oracle.js.parser.TokenType.EXTENDS;
import static com.oracle.js.parser.TokenType.FINALLY;
import static com.oracle.js.parser.TokenType.FROM;
import static com.oracle.js.parser.TokenType.FUNCTION;
import static com.oracle.js.parser.TokenType.GET;
import static com.oracle.js.parser.TokenType.IDENT;
import static com.oracle.js.parser.TokenType.IF;
import static com.oracle.js.parser.TokenType.IMPORT;
import static com.oracle.js.parser.TokenType.IN;
import static com.oracle.js.parser.TokenType.INCPOSTFIX;
import static com.oracle.js.parser.TokenType.INCPREFIX;
import static com.oracle.js.parser.TokenType.LBRACE;
import static com.oracle.js.parser.TokenType.LBRACKET;
import static com.oracle.js.parser.TokenType.LET;
import static com.oracle.js.parser.TokenType.LPAREN;
import static com.oracle.js.parser.TokenType.MUL;
import static com.oracle.js.parser.TokenType.OF;
import static com.oracle.js.parser.TokenType.PERIOD;
import static com.oracle.js.parser.TokenType.PRIVATE_IDENT;
import static com.oracle.js.parser.TokenType.RBRACE;
import static com.oracle.js.parser.TokenType.RBRACKET;
import static com.oracle.js.parser.TokenType.RPAREN;
import static com.oracle.js.parser.TokenType.SEMICOLON;
import static com.oracle.js.parser.TokenType.SET;
import static com.oracle.js.parser.TokenType.SPREAD_ARGUMENT;
import static com.oracle.js.parser.TokenType.SPREAD_ARRAY;
import static com.oracle.js.parser.TokenType.SPREAD_OBJECT;
import static com.oracle.js.parser.TokenType.STATIC;
import static com.oracle.js.parser.TokenType.STRING;
import static com.oracle.js.parser.TokenType.SUPER;
import static com.oracle.js.parser.TokenType.TEMPLATE;
import static com.oracle.js.parser.TokenType.TEMPLATE_HEAD;
import static com.oracle.js.parser.TokenType.TEMPLATE_MIDDLE;
import static com.oracle.js.parser.TokenType.TEMPLATE_TAIL;
import static com.oracle.js.parser.TokenType.TERNARY;
import static com.oracle.js.parser.TokenType.THIS;
import static com.oracle.js.parser.TokenType.VAR;
import static com.oracle.js.parser.TokenType.VOID;
import static com.oracle.js.parser.TokenType.WHILE;
import static com.oracle.js.parser.TokenType.YIELD;
import static com.oracle.js.parser.TokenType.YIELD_STAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.graalvm.collections.Pair;

import com.oracle.js.parser.ir.AccessNode;
import com.oracle.js.parser.ir.BaseNode;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.BlockStatement;
import com.oracle.js.parser.ir.BreakNode;
import com.oracle.js.parser.ir.CallNode;
import com.oracle.js.parser.ir.CaseNode;
import com.oracle.js.parser.ir.CatchNode;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.ContinueNode;
import com.oracle.js.parser.ir.DebuggerNode;
import com.oracle.js.parser.ir.EmptyNode;
import com.oracle.js.parser.ir.ErrorNode;
import com.oracle.js.parser.ir.ExportNode;
import com.oracle.js.parser.ir.ExportSpecifierNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionList;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.ForNode;
import com.oracle.js.parser.ir.FromNode;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IfNode;
import com.oracle.js.parser.ir.ImportClauseNode;
import com.oracle.js.parser.ir.ImportNode;
import com.oracle.js.parser.ir.ImportSpecifierNode;
import com.oracle.js.parser.ir.IndexNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LabelNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.LiteralNode.ArrayLiteralNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.js.parser.ir.NameSpaceImportNode;
import com.oracle.js.parser.ir.NamedExportsNode;
import com.oracle.js.parser.ir.NamedImportsNode;
import com.oracle.js.parser.ir.Node;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.PropertyKey;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.ReturnNode;
import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.SwitchNode;
import com.oracle.js.parser.ir.Symbol;
import com.oracle.js.parser.ir.TemplateLiteralNode;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.ThrowNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WhileNode;
import com.oracle.js.parser.ir.WithNode;
import com.oracle.js.parser.ir.visitor.NodeVisitor;

/**
 * Builds the IR.
 */
@SuppressWarnings("fallthrough")
public class Parser extends AbstractParser {
    /** The arguments variable name. */
    static final String ARGUMENTS_NAME = "arguments";
    /** The eval function variable name. */
    private static final String EVAL_NAME = "eval";
    private static final String CONSTRUCTOR_NAME = "constructor";
    private static final String PRIVATE_CONSTRUCTOR_NAME = "#constructor";
    private static final String PROTO_NAME = "__proto__";
    static final String NEW_TARGET_NAME = "new.target";
    private static final String IMPORT_META_NAME = "import.meta";
    private static final String PROTOTYPE_NAME = "prototype";
    /** Function.prototype.apply method name. */
    private static final String APPLY_NAME = "apply";

    /** EXEC name - special property used by $EXEC API. */
    private static final String EXEC_NAME = "$EXEC";
    /** Function name for anonymous functions. */
    private static final String ANONYMOUS_FUNCTION_NAME = ":anonymous";
    /** Function name for the program entry point. */
    private static final String PROGRAM_NAME = ":program";
    /** Internal lexical binding name for {@code catch} error (destructuring). */
    private static final String ERROR_BINDING_NAME = ":error";
    /** Internal lexical binding name for {@code switch} expression. */
    private static final String SWITCH_BINDING_NAME = ":switch";
    /** Function name for arrow functions. */
    private static final String ARROW_FUNCTION_NAME = ":=>";
    /** Internal function name for class field initializer. */
    private static final String INITIALIZER_FUNCTION_NAME = ":initializer";

    private static final String FUNCTION_PARAMETER_CONTEXT = "function parameter";
    private static final String CATCH_PARAMETER_CONTEXT = "catch parameter";
    private static final String IMPORTED_BINDING_CONTEXT = "imported binding";
    private static final String CLASS_NAME_CONTEXT = "class name";
    private static final String VARIABLE_NAME_CONTEXT = "variable name";
    private static final String ASSIGNMENT_TARGET_CONTEXT = "assignment target";

    private static final boolean ES6_FOR_OF = Options.getBooleanProperty("parser.for.of", true);
    private static final boolean ES6_CLASS = Options.getBooleanProperty("parser.class", true);
    private static final boolean ES6_ARROW_FUNCTION = Options.getBooleanProperty("parser.arrow.function", true);
    private static final boolean ES6_REST_PARAMETER = Options.getBooleanProperty("parser.rest.parameter", true);
    private static final boolean ES6_SPREAD_ARGUMENT = Options.getBooleanProperty("parser.spread.argument", true);
    private static final boolean ES6_GENERATOR_FUNCTION = Options.getBooleanProperty("parser.generator.function", true);
    private static final boolean ES6_DESTRUCTURING = Options.getBooleanProperty("parser.destructuring", true);
    private static final boolean ES6_SPREAD_ARRAY = Options.getBooleanProperty("parser.spread.array", true);
    private static final boolean ES6_COMPUTED_PROPERTY_NAME = Options.getBooleanProperty("parser.computed.property.name", true);
    private static final boolean ES6_DEFAULT_PARAMETER = Options.getBooleanProperty("parser.default.parameter", true);
    private static final boolean ES6_NEW_TARGET = Options.getBooleanProperty("parser.new.target", true);

    private static final boolean ES8_TRAILING_COMMA = Options.getBooleanProperty("parser.trailing.comma", true);
    private static final boolean ES8_ASYNC_FUNCTION = Options.getBooleanProperty("parser.async.function", true);
    private static final boolean ES8_REST_SPREAD_PROPERTY = Options.getBooleanProperty("parser.rest.spread.property", true);
    private static final boolean ES8_FOR_AWAIT_OF = Options.getBooleanProperty("parser.for.await.of", true);
    private static final boolean ES2019_OPTIONAL_CATCH_BINDING = Options.getBooleanProperty("parser.optional.catch.binding", true);
    private static final boolean ES2020_CLASS_FIELDS = Options.getBooleanProperty("parser.class.fields", true);
    private static final boolean ES2022_TOP_LEVEL_AWAIT = Options.getBooleanProperty("parser.top.level.await", true);

    private static final int REPARSE_IS_PROPERTY_ACCESSOR = 1 << 0;
    private static final int REPARSE_IS_METHOD = 1 << 1;
    /** Parsing eval. */
    private static final int PARSE_EVAL = 1 << 2;
    /** Parsing eval in a function (i.e. not script or module) context. */
    private static final int PARSE_FUNCTION_CONTEXT_EVAL = 1 << 3;

    private static final String MESSAGE_INVALID_LVALUE = "invalid.lvalue";
    private static final String MESSAGE_EXPECTED_STMT = "expected.stmt";
    private static final String MESSAGE_ESCAPED_KEYWORD = "escaped.keyword";
    private static final String MESSAGE_INVALID_PROPERTY_INITIALIZER = "invalid.property.initializer";
    private static final String MESSAGE_INVALID_ARROW_PARAMETER = "invalid.arrow.parameter";
    private static final String MESSAGE_EXPECTED_OPERAND = "expected.operand";
    private static final String MESSAGE_PROPERTY_REDEFINITON = "property.redefinition";

    /** Current env. */
    private final ScriptEnvironment env;

    /** Is scripting mode. */
    private final boolean scripting;

    /** Is shebang supported */
    private final boolean shebang;

    /** Is BigInt supported */
    private final boolean allowBigInt;

    private List<Statement> functionDeclarations;

    private final ParserContext lc;
    private final List<Object> defaultNames;

    /** Namespace for function names where not explicitly given */
    private final Namespace namespace;

    /** to receive line information from Lexer when scanning multiline literals. */
    protected final Lexer.LineInfoReceiver lineInfoReceiver;

    private RecompilableScriptFunctionData reparsedFunction;

    private boolean isModule;

    /**
     * Used to pass (async) arrow function flags from head to body.
     *
     * Must be immediately consumed, i.e. before parsing the next expression, in order to avoid any
     * nesting issues.
     */
    private ParserContextFunctionNode coverArrowFunction;

    public static final boolean PROFILE_PARSING = Options.getBooleanProperty("parser.profiling", false);
    public static final boolean PROFILE_PARSING_PRINT = Options.getBooleanProperty("parser.profiling.print", true);

    /**
     * Constructor
     *
     * @param env script environment
     * @param source source to parse
     * @param errors error manager
     */
    public Parser(final ScriptEnvironment env, final Source source, final ErrorManager errors) {
        this(env, source, errors, env.strict);
    }

    /**
     * Constructor
     *
     * @param env script environment
     * @param source source to parse
     * @param errors error manager
     * @param strict strict
     */
    public Parser(final ScriptEnvironment env, final Source source, final ErrorManager errors, final boolean strict) {
        this(env, source, errors, strict, 0);
    }

    /**
     * Construct a parser.
     *
     * @param env script environment
     * @param source source to parse
     * @param errors error manager
     * @param strict parser created with strict mode enabled.
     * @param lineOffset line offset to start counting lines from
     */
    public Parser(final ScriptEnvironment env, final Source source, final ErrorManager errors, final boolean strict, final int lineOffset) {
        super(source, errors, strict, lineOffset);
        this.lc = new ParserContext();
        this.defaultNames = new ArrayList<>();
        this.env = env;
        this.namespace = new Namespace(env.getNamespace());
        this.scripting = env.scripting && env.syntaxExtensions;
        this.shebang = env.shebang || scripting;
        this.allowBigInt = env.allowBigInt;
        if (this.scripting) {
            this.lineInfoReceiver = new Lexer.LineInfoReceiver() {
                @Override
                public void lineInfo(final int receiverLine, final int receiverLinePosition) {
                    // update the parser maintained line information
                    Parser.this.line = receiverLine;
                    Parser.this.linePosition = receiverLinePosition;
                }
            };
        } else {
            // non-scripting mode script can't have multi-line literals
            this.lineInfoReceiver = null;
        }
    }

    /**
     * Execute parse and return the resulting function node. Errors will be thrown and the error
     * manager will contain information if parsing should fail
     *
     * This is the default parse call, which will name the function node {@link #PROGRAM_NAME}.
     *
     * @return function node resulting from successful parse
     */
    public FunctionNode parse() {
        return parse(PROGRAM_NAME, 0, source.getLength(), 0, null, null);
    }

    /**
     * Sets the @link RecompilableScriptFunctionData representing the function being reparsed (when
     * this parser instance is used to reparse a previously parsed function, as part of its
     * on-demand compilation). This will trigger various special behaviors, such as skipping nested
     * function bodies.
     *
     * @param reparsedFunction the function being reparsed.
     */
    public void setReparsedFunction(final RecompilableScriptFunctionData reparsedFunction) {
        this.reparsedFunction = reparsedFunction;
    }

    /**
     * Set up first token. Skips opening EOL.
     */
    private void scanFirstToken() {
        k = -1;
        next();
    }

    /**
     * Prepare {@link TokenStream} and {@link Lexer} for parsing.
     *
     * @param startPos source start position
     * @param len source length
     */
    private void prepareLexer(final int startPos, final int len) {
        stream = new TokenStream();
        lexer = new Lexer(source, startPos, len, stream, scripting, env.ecmaScriptVersion, shebang, isModule, reparsedFunction != null, allowBigInt);
        lexer.line = lexer.pendingLine = lineOffset + 1;
        line = lineOffset;
    }

    /**
     * Peek ahead at the next token, skipping COMMENT and EOL tokens.
     */
    private TokenType lookahead() {
        for (int i = 1;; i++) {
            TokenType t = T(k + i);
            if (t == EOL || t == COMMENT) {
                continue;
            } else {
                return t;
            }
        }
    }

    /**
     * Execute parse and return the resulting function node. Errors will be thrown and the error
     * manager will contain information if parsing should fail
     *
     * This should be used to create one and only one function node
     *
     * @param scriptName name for the script, given to the parsed FunctionNode
     * @param startPos start position in source
     * @param len length of parse
     * @param reparseFlags flags provided by {@link RecompilableScriptFunctionData} as context for
     *            the code being reparsed. This allows us to recognize special forms of functions
     *            such as property getters and setters or instances of ES6 method shorthand in
     *            object literals.
     * @param argumentNames optional names of arguments assumed by the parsed function node.
     *
     * @return function node resulting from successful parse
     */
    public FunctionNode parse(final String scriptName, final int startPos, final int len, final int reparseFlags, Scope parentScope, String[] argumentNames) {
        long startTime = PROFILE_PARSING ? System.nanoTime() : 0L;
        try {
            prepareLexer(startPos, len);

            scanFirstToken();

            return program(scriptName, reparseFlags, parentScope, argumentNames);
        } catch (final Exception e) {
            handleParseException(e);

            return null;
        } finally {
            if (PROFILE_PARSING) {
                long duration = (System.nanoTime() - startTime);
                if (PROFILE_PARSING_PRINT) {
                    System.out.println("Parsing: " + duration / 1_000_000);
                }
            }
        }
    }

    /**
     * Parse and return the resulting module. Errors will be thrown and the error manager will
     * contain information if parsing should fail
     *
     * @param moduleName name for the module, given to the parsed FunctionNode
     * @param startPos start position in source
     * @param len length of parse
     *
     * @return function node resulting from successful parse
     */
    public FunctionNode parseModule(final String moduleName, final int startPos, final int len) {
        boolean oldModule = isModule;
        boolean oldStrictMode = isStrictMode;
        try {
            isModule = true;
            isStrictMode = true; // Module code is always strict mode code. (ES6 10.2.1)
            prepareLexer(startPos, len);

            scanFirstToken();

            return module(moduleName);
        } catch (final Exception e) {
            handleParseException(e);

            return null;
        } finally {
            isStrictMode = oldStrictMode;
            isModule = oldModule;
        }
    }

    public FunctionNode parseModule(final String moduleName) {
        return parseModule(moduleName, 0, source.getLength());
    }

    /**
     * Parse eval code.
     *
     * @param parentScope optional caller context scope (direct eval)
     */
    public FunctionNode parseEval(boolean functionContext, Scope parentScope) {
        return parse(PROGRAM_NAME, 0, source.getLength(), PARSE_EVAL | (functionContext ? PARSE_FUNCTION_CONTEXT_EVAL : 0), parentScope, null);
    }

    /**
     * Parse code assuming a set of given arguments for the returned {@code FunctionNode}.
     *
     * @param argumentNames names of arguments assumed by the parsed function node.
     */
    public FunctionNode parseWithArguments(String[] argumentNames) {
        return parse(PROGRAM_NAME, 0, source.getLength(), 0, null, argumentNames);
    }

    /**
     * Parse and return the list of function parameter list. A comma separated list of function
     * parameter identifiers is expected to be parsed. Errors will be thrown and the error manager
     * will contain information if parsing should fail. This method is used to check if parameter
     * Strings passed to "Function" constructor is a valid or not.
     *
     * @see #parseFunctionBody
     */
    public void parseFormalParameterList() {
        try {
            stream = new TokenStream();
            lexer = new Lexer(source, stream, scripting, env.ecmaScriptVersion, shebang, isModule, allowBigInt);

            scanFirstToken();

            assert lc.getCurrentScope() == null;
            formalParameterList(TokenType.EOF, false, false);
        } catch (final Exception e) {
            handleParseException(e);
        }
    }

    /**
     * Execute parse and return the resulting function node. Errors will be thrown and the error
     * manager will contain information if parsing should fail. This method is used to check if code
     * String passed to "Function" constructor is a valid function body or not.
     *
     * @return function node resulting from successful parse
     * @see #parseFormalParameterList
     */
    public FunctionNode parseFunctionBody(boolean generator, boolean async) {
        try {
            stream = new TokenStream();
            lexer = new Lexer(source, stream, scripting, env.ecmaScriptVersion, shebang, isModule, allowBigInt);
            final int functionLine = line;

            scanFirstToken();

            // Make a fake token for the function.
            final long functionToken = Token.toDesc(FUNCTION, 0, source.getLength());
            // Set up the function to append elements.

            final IdentNode ident = new IdentNode(functionToken, Token.descPosition(functionToken), PROGRAM_NAME);
            final int functionFlags = (generator ? FunctionNode.IS_GENERATOR : 0) | (async ? FunctionNode.IS_ASYNC : 0);
            final ParserContextFunctionNode function = createParserContextFunctionNode(ident, functionToken, functionFlags, functionLine, Collections.<IdentNode> emptyList(), 0);
            function.clearFlag(FunctionNode.IS_PROGRAM);

            assert lc.getCurrentScope() == null;
            lc.push(function);
            final ParserContextBlockNode body = newBlock(function.createBodyScope());
            functionDeclarations = new ArrayList<>();
            try {
                sourceElements(generator, async, 0);
                addFunctionDeclarations(function);
            } finally {
                functionDeclarations = null;
                function.finishBodyScope();
                restoreBlock(body);
                lc.pop(function);
            }
            body.setFlag(Block.NEEDS_SCOPE);
            final Block functionBody = new Block(functionToken, finish, body.getFlags() | Block.IS_SYNTHETIC | Block.IS_BODY, body.getScope(), body.getStatements());

            expect(EOF);

            final FunctionNode functionNode = createFunctionNode(
                            function,
                            functionToken,
                            ident,
                            functionLine,
                            functionBody);
            return functionNode;
        } catch (final Exception e) {
            handleParseException(e);
            return null;
        }
    }

    private void handleParseException(final Exception e) {
        // Issue message.
        if (e instanceof ParserException) {
            errors.error((ParserException) e);
        } else {
            // Extract message from exception. The message will be in error
            // message format.
            String message = e.getMessage();

            // If empty message.
            if (message == null) {
                message = e.toString();
            }

            errors.error(message);
        }

        if (env.dumpOnError) {
            e.printStackTrace(env.getErr());
        }
    }

    /**
     * Skip to a good parsing recovery point.
     */
    private void recover(final Exception e) {
        if (e != null) {
            // Issue message.
            if (e instanceof ParserException) {
                errors.error((ParserException) e);
            } else {
                // Extract message from exception. The message will be in error
                // message format.
                String message = e.getMessage();

                // If empty message.
                if (message == null) {
                    message = e.toString();
                }

                errors.error(message);
            }

            if (env.dumpOnError) {
                e.printStackTrace(env.getErr());
            }
        }

        // Skip to a recovery point.
        loop: while (true) {
            switch (type) {
                case EOF:
                    // Can not go any further.
                    break loop;
                case EOL:
                case SEMICOLON:
                case RBRACE:
                    // Good recovery points.
                    next();
                    break loop;
                default:
                    // So we can recover after EOL.
                    nextOrEOL();
                    break;
            }
        }
    }

    /**
     * Set up a new block.
     *
     * @return New block.
     */
    private ParserContextBlockNode newBlock() {
        Scope scope = Scope.createBlock(lc.getCurrentScope());
        return newBlock(scope);
    }

    /**
     * Set up a new block.
     *
     * @return New block.
     */
    private ParserContextBlockNode newBlock(Scope scope) {
        return lc.push(new ParserContextBlockNode(token, scope));
    }

    private ParserContextFunctionNode createParserContextFunctionNode(final IdentNode ident, final long functionToken, final int functionFlags, final int functionLine) {
        return createParserContextFunctionNode(ident, functionToken, functionFlags, functionLine, null, 0);
    }

    private ParserContextFunctionNode createParserContextFunctionNode(final IdentNode ident, final long functionToken, final int functionFlags, final int functionLine,
                    final List<IdentNode> parameters, int functionLength) {
        return createParserContextFunctionNode(ident, functionToken, functionFlags, functionLine, parameters, functionLength, null);
    }

    private ParserContextFunctionNode createParserContextFunctionNode(final IdentNode ident, final long functionToken, final int functionFlags, final int functionLine,
                    final List<IdentNode> parameters, int functionLength, Scope functionTopScope) {
        final ParserContextFunctionNode parentFunction = lc.getCurrentFunction();

        final String name = ident == null ? "" : ident.getName();

        int flags = functionFlags;
        if (isStrictMode) {
            flags |= FunctionNode.IS_STRICT;
        }
        if (parentFunction == null) {
            flags |= FunctionNode.IS_PROGRAM;
            flags |= FunctionNode.IS_ANONYMOUS;
        }

        final Scope parentScope = lc.getCurrentScope();
        return new ParserContextFunctionNode(functionToken, ident, name, namespace, functionLine, flags, parameters, functionLength, parentScope, functionTopScope);
    }

    private FunctionNode createFunctionNode(final ParserContextFunctionNode function, final long startToken, final IdentNode ident,
                    final int functionLine, final Block body) {
        assert body.isFunctionBody() || (body.isParameterBlock() && ((BlockStatement) body.getLastStatement()).getBlock().isFunctionBody());

        VarNode varNode = function.verifyHoistedVarDeclarations();
        if (varNode != null) {
            throw error(ECMAErrors.getMessage("syntax.error.redeclare.variable", varNode.getName().getName()), varNode.getToken());
        }

        long lastTokenWithDelimiter = Token.withDelimiter(function.getLastToken());
        // EOL uses length field to store the line number
        int lastTokenFinish = Token.descPosition(lastTokenWithDelimiter) + (Token.descType(lastTokenWithDelimiter) == TokenType.EOL ? 0 : Token.descLength(lastTokenWithDelimiter));

        final FunctionNode functionNode = new FunctionNode(
                        source,
                        functionLine,
                        body.getToken(),
                        lastTokenFinish,
                        startToken,
                        function.getLastToken(),
                        ident,
                        function.getName(),
                        function.getLength(),
                        function.getParameterCount(),
                        optimizeList(function.getParameters()),
                        function.getFlags(),
                        body,
                        function.getEndParserState(),
                        function.getModule(),
                        function.getInternalName());

        return functionNode;
    }

    /**
     * Restore the current block.
     */
    private ParserContextBlockNode restoreBlock(final ParserContextBlockNode block) {
        block.getScope().close();
        return lc.pop(block);
    }

    /**
     * Get the statements in a block.
     *
     * @return Block statements.
     */
    private Block getBlock(boolean yield, boolean await, boolean needsBraces) {
        final long blockToken = token;
        final ParserContextBlockNode newBlock = newBlock();
        try {
            // Block opening brace.
            if (needsBraces) {
                expect(LBRACE);
            }
            // Accumulate block statements.
            statementList(yield, await);

        } finally {
            restoreBlock(newBlock);
        }

        // Block closing brace.
        int realFinish;
        if (needsBraces) {
            expectDontAdvance(RBRACE);
            // otherwise in case block containing single braced block the inner
            // block could end somewhere later after comments and spaces
            realFinish = Token.descPosition(token) + Token.descLength(token);
            expect(RBRACE);
        } else {
            realFinish = finish;
        }

        final int flags = newBlock.getFlags() | (needsBraces ? 0 : Block.IS_SYNTHETIC);
        return new Block(blockToken, Math.max(realFinish, Token.descPosition(blockToken)), flags, newBlock.getScope(), newBlock.getStatements());
    }

    /**
     * Get the statements in a case clause.
     */
    private List<Statement> caseStatementList(boolean yield, boolean await) {
        // case clauses share the same scope.
        final ParserContextBlockNode newBlock = newBlock(lc.getCurrentScope());
        try {
            statementList(yield, await);
        } finally {
            lc.pop(newBlock);
        }
        return newBlock.getStatements();
    }

    /**
     * Get all the statements generated by a single statement.
     *
     * @return Statements.
     */
    private Block getStatement(boolean yield, boolean await) {
        return getStatement(yield, await, false, false);
    }

    private Block getStatement(boolean yield, boolean await, boolean labelledStatement, boolean mayBeFunctionDeclaration) {
        return getStatement(yield, await, labelledStatement, mayBeFunctionDeclaration, mayBeFunctionDeclaration);
    }

    private Block getStatement(boolean yield, boolean await, boolean labelledStatement, boolean mayBeFunctionDeclaration, boolean mayBeLabeledFunctionDeclaration) {
        if (type == LBRACE) {
            return getBlock(yield, await, true);
        }
        // Set up new block. Captures first token.
        final ParserContextBlockNode newBlock = newBlock();
        newBlock.setFlag(Block.IS_SYNTHETIC);
        try {
            statement(yield, await, false, 0, true, labelledStatement, mayBeFunctionDeclaration, mayBeLabeledFunctionDeclaration);
        } finally {
            restoreBlock(newBlock);
        }
        return new Block(newBlock.getToken(), finish, newBlock.getFlags(), newBlock.getScope(), newBlock.getStatements());
    }

    /**
     * Detect use of special properties.
     *
     * @param ident Referenced property.
     */
    private IdentNode detectSpecialProperty(final IdentNode ident) {
        if (isArguments(ident)) {
            return markArguments(ident);
        }
        return ident;
    }

    private IdentNode markArguments(final IdentNode ident) {
        Scope currentScope = lc.getCurrentScope();
        if (currentScope.inClassFieldInitializer()) {
            throw error(AbstractParser.message("arguments.in.field.initializer"), ident.getToken());
        }
        if (currentScope.isGlobalScope()) {
            // arguments has no special meaning in the global scope
            return ident;
        }
        // skip over arrow functions, e.g. function f() { return (() => arguments.length)(); }
        lc.getCurrentNonArrowFunction().setFlag(FunctionNode.USES_ARGUMENTS);
        return ident.setIsArguments();
    }

    private boolean useBlockScope() {
        return isES6();
    }

    /**
     * ES6 (a.k.a. ES2015) or newer.
     */
    private boolean isES6() {
        return env.ecmaScriptVersion >= 6;
    }

    /**
     * ES2017 or newer.
     */
    private boolean isES2017() {
        return env.ecmaScriptVersion >= 8;
    }

    /**
     * ES2020 or newer.
     */
    private boolean isES2020() {
        return env.ecmaScriptVersion >= 11;
    }

    /**
     * ES2021 or newer.
     */
    private boolean isES2021() {
        return env.ecmaScriptVersion >= 12;
    }

    /**
     * ES2022 or newer.
     */
    private boolean isES2022() {
        return env.ecmaScriptVersion >= 13;
    }

    private boolean isClassFields() {
        return ES2020_CLASS_FIELDS && env.classFields;
    }

    private static boolean isArguments(final String name) {
        return ARGUMENTS_NAME.equals(name);
    }

    static boolean isArguments(final IdentNode ident) {
        return isArguments(ident.getName());
    }

    /**
     * Tells whether a IdentNode can be used as L-value of an assignment
     *
     * @param ident IdentNode to be checked
     * @return whether the ident can be used as L-value
     */
    private static boolean checkIdentLValue(final IdentNode ident) {
        return ident.tokenType().getKind() != TokenKind.KEYWORD;
    }

    /**
     * Verify an assignment expression.
     *
     * @param op Operation token.
     * @param lhs Left hand side expression.
     * @param rhs Right hand side expression.
     * @return Verified expression.
     */
    private Expression verifyAssignment(final long op, final Expression lhs, final Expression rhs, boolean inPatternPosition) {
        final TokenType opType = Token.descType(op);
        Expression rhsExpr = rhs;

        switch (opType) {
            case ASSIGN:
            case ASSIGN_INIT:
            case ASSIGN_ADD:
            case ASSIGN_BIT_AND:
            case ASSIGN_BIT_OR:
            case ASSIGN_BIT_XOR:
            case ASSIGN_DIV:
            case ASSIGN_MOD:
            case ASSIGN_MUL:
            case ASSIGN_EXP:
            case ASSIGN_SAR:
            case ASSIGN_SHL:
            case ASSIGN_SHR:
            case ASSIGN_SUB:
            case ASSIGN_AND:
            case ASSIGN_OR:
            case ASSIGN_NULLCOAL:
                if (lhs instanceof IdentNode) {
                    IdentNode ident = (IdentNode) lhs;
                    if (!checkIdentLValue(ident) || ident.isMetaProperty()) {
                        throw invalidLHSError(lhs);
                    }
                    verifyStrictIdent(ident, ASSIGNMENT_TARGET_CONTEXT);

                    // IsIdentifierRef must be true, so lhs must not be parenthesized.
                    if (!lhs.isParenthesized() && isAnonymousFunctionDefinition(rhsExpr)) {
                        rhsExpr = setAnonymousFunctionName(rhsExpr, ident.getName());
                    }

                    break;
                } else if (lhs instanceof AccessNode || lhs instanceof IndexNode) {
                    if (((BaseNode) lhs).isOptional()) {
                        throw invalidLHSError(lhs);
                    }
                    break;
                } else if ((opType == ASSIGN || opType == ASSIGN_INIT) && isDestructuringLhs(lhs) && (inPatternPosition || !lhs.isParenthesized())) {
                    verifyDestructuringAssignmentPattern(lhs, ASSIGNMENT_TARGET_CONTEXT);
                    break;
                } else {
                    throw invalidLHSError(lhs);
                }
            default:
                break;
        }

        assert !BinaryNode.isLogical(opType);
        return new BinaryNode(op, lhs, rhsExpr);
    }

    private boolean isDestructuringLhs(Expression lhs) {
        if (lhs instanceof ObjectNode || lhs instanceof ArrayLiteralNode) {
            return ES6_DESTRUCTURING && isES6();
        }
        return false;
    }

    private void verifyDestructuringAssignmentPattern(Expression pattern, String contextString) {
        assert pattern instanceof ObjectNode || pattern instanceof ArrayLiteralNode;
        pattern.accept(new VerifyDestructuringPatternNodeVisitor(new LexicalContext()) {
            @Override
            protected void verifySpreadElement(Expression lvalue) {
                if (!checkValidLValue(lvalue, contextString)) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), lvalue.getToken());
                }
                lvalue.accept(this);
            }

            @Override
            public boolean enterIdentNode(IdentNode identNode) {
                if (!checkIdentLValue(identNode) || identNode.isMetaProperty()) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), identNode.getToken());
                }
                verifyStrictIdent(identNode, contextString);
                return false;
            }

            @Override
            public boolean enterAccessNode(AccessNode accessNode) {
                if (accessNode.isOptional()) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), accessNode.getToken());
                }
                return false;
            }

            @Override
            public boolean enterIndexNode(IndexNode indexNode) {
                if (indexNode.isOptional()) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), indexNode.getToken());
                }
                return false;
            }

            @Override
            protected boolean enterDefault(Node node) {
                throw error(String.format("unexpected node in AssignmentPattern: %s", node));
            }
        });
    }

    private Expression newBinaryExpression(final long op, final Expression lhs, final Expression rhs) {
        final TokenType opType = Token.descType(op);

        // Build up node.
        if (BinaryNode.isLogical(opType)) {
            if (forbiddenNullishCoalescingUsage(opType, lhs, rhs)) {
                throw error(String.format("nullish coalescing operator cannot immediately contain, or be contained within, an && or || operation"));
            }
            return new BinaryNode(op, new JoinPredecessorExpression(lhs), new JoinPredecessorExpression(rhs));
        }
        return new BinaryNode(op, lhs, rhs);
    }

    private static boolean forbiddenNullishCoalescingUsage(TokenType opType, Expression lhs, Expression rhs) {
        if (opType == TokenType.NULLISHCOALESC) {
            return forbiddenNullishCoalescingChaining(lhs) || forbiddenNullishCoalescingChaining(rhs);
        } else {
            assert opType == TokenType.AND || opType == TokenType.OR;
            return (!lhs.isParenthesized() && lhs.isTokenType(TokenType.NULLISHCOALESC)) || (!rhs.isParenthesized() && rhs.isTokenType(TokenType.NULLISHCOALESC));
        }
    }

    private static boolean forbiddenNullishCoalescingChaining(Expression expression) {
        return !expression.isParenthesized() && (expression.isTokenType(TokenType.AND) || expression.isTokenType(TokenType.OR));
    }

    /**
     * Reduce increment/decrement to simpler operations.
     *
     * @param firstToken First token.
     * @param tokenType Operation token (INCPREFIX/DEC.)
     * @param expression Left hand side expression.
     * @param isPostfix Prefix or postfix.
     * @return Reduced expression.
     */
    private static UnaryNode incDecExpression(final long firstToken, final TokenType tokenType, final Expression expression, final boolean isPostfix) {
        assert tokenType == INCPREFIX || tokenType == DECPREFIX;
        if (isPostfix) {
            long postfixToken = Token.recast(firstToken, tokenType == DECPREFIX ? DECPOSTFIX : INCPOSTFIX);
            return new UnaryNode(postfixToken, expression.getStart(), Token.descPosition(firstToken) + Token.descLength(firstToken), expression);
        }

        return new UnaryNode(firstToken, expression);
    }

    /**
     * Parse the top level script.
     *
     * <pre>
     * Program :
     *      SourceElements?
     * </pre>
     */
    private FunctionNode program(final String scriptName, final int parseFlags, final Scope parentScope, final String[] argumentNames) {
        // Make a pseudo-token for the script holding its start and length.
        int functionStart = Math.min(Token.descPosition(Token.withDelimiter(token)), finish);
        final long functionToken = Token.toDesc(FUNCTION, functionStart, source.getLength() - functionStart);
        final int functionLine = line;

        Scope topScope = (parseFlags & PARSE_EVAL) != 0 ? createEvalScope(parseFlags, parentScope) : Scope.createGlobal();
        topScope = applyArgumentsToScope(topScope, argumentNames);
        final IdentNode ident = null;
        final List<IdentNode> parameters = createFunctionNodeParameters(argumentNames);
        final ParserContextFunctionNode script = createParserContextFunctionNode(
                        ident,
                        functionToken,
                        FunctionNode.IS_SCRIPT,
                        functionLine,
                        parameters,
                        parameters.size(),
                        topScope);
        script.setInternalName(scriptName);

        lc.push(script);
        final ParserContextBlockNode body = newBlock(topScope);
        functionDeclarations = new ArrayList<>();
        try {
            sourceElements(false, false, parseFlags);
            addFunctionDeclarations(script);
        } finally {
            functionDeclarations = null;
            script.finishBodyScope();
            restoreBlock(body);
            lc.pop(script);
        }

        body.setFlag(Block.NEEDS_SCOPE);
        final Block programBody = new Block(functionToken, finish, body.getFlags() | Block.IS_SYNTHETIC | Block.IS_BODY, body.getScope(), body.getStatements());
        script.setLastToken(token);

        expect(EOF);

        return createFunctionNode(script, functionToken, ident, functionLine, programBody);
    }

    private static Scope applyArgumentsToScope(Scope scope, String[] argumentNames) {
        if (argumentNames == null) {
            return scope;
        }
        // If parsing with arguments, create an artificial local scope to emulate
        // function-like semantics:
        Scope body = Scope.createFunctionBody(scope);
        // We have to also explicitly put parameters in the top scope, because
        // ParserContextFunctionNode will not do it automatically for script nodes.
        for (String argument : argumentNames) {
            body.putSymbol(new Symbol(argument, Symbol.IS_VAR | Symbol.IS_PARAM));
        }
        return body;
    }

    private static List<IdentNode> createFunctionNodeParameters(String[] argumentNames) {
        if (argumentNames == null) {
            return Collections.emptyList();
        }
        ArrayList<IdentNode> list = new ArrayList<>();
        for (String argumentName : argumentNames) {
            // Create an artificial IdentNode that is not in the source.
            list.add(new IdentNode(0, 0, argumentName));
        }
        return list;
    }

    private Scope createEvalScope(final int parseFlags, Scope parentScope) {
        // 1. strict eval code always has its own scope
        // 2. non-strict indirect eval is in global scope
        // 3. non-strict direct eval is in global scope if the caller is
        // NOTE: In non-strict mode, the scope is preliminary: we may still encounter a "use strict"
        // directive and switch into strict mode (in which case we replace the scope).
        assert (parseFlags & PARSE_EVAL) != 0;
        if ((isStrictMode || (parseFlags & PARSE_FUNCTION_CONTEXT_EVAL) != 0)) {
            return Scope.createEval(parentScope, isStrictMode);
        } else {
            return Scope.createGlobal();
        }
    }

    /**
     * Directive value or null if statement is not a directive.
     *
     * @param stmt Statement to be checked
     * @return Directive value if the given statement is a directive
     */
    private String getDirective(final Node stmt) {
        if (stmt instanceof ExpressionStatement) {
            final Node expr = ((ExpressionStatement) stmt).getExpression();
            if (expr instanceof LiteralNode) {
                final LiteralNode<?> lit = (LiteralNode<?>) expr;
                final long litToken = lit.getToken();
                final TokenType tt = Token.descType(litToken);
                // A directive is either a string or an escape string
                if (tt == TokenType.STRING || tt == TokenType.ESCSTRING) {
                    // Make sure that we don't unescape anything. Return as seen in source!
                    return source.getString(lit.getStart() + 1, Token.descLength(litToken) - 2);
                }
            }
        }

        return null;
    }

    /**
     * Parse the statements of the script, module, or function.
     */
    private void sourceElements(boolean yield, boolean await, int parseFlags) {
        boolean checkDirective = true;
        int functionFlags = parseFlags;
        final boolean oldStrictMode = isStrictMode;

        try {
            // If is a script, then process until the end of the script.
            while (type != EOF) {
                final TokenType elementType = type;
                // Break if the end of a code block.
                if (elementType == RBRACE) {
                    break;
                }

                try {
                    // Get the next element.
                    statement(yield, await, true, functionFlags, false, false, true);
                    functionFlags = 0;

                    // Check for string directive prologues like "use strict".
                    // A directive is either an unescaped or an escaped string.
                    if (checkDirective) {
                        // skip any debug statement like line number to get actual first line
                        final Statement lastStatement = (elementType == STRING || elementType == ESCSTRING) ? lc.getLastStatement() : null;

                        // get directive prologue, if any
                        final String directive = getDirective(lastStatement);

                        // If we have seen first non-directive statement,
                        // no more directive statements!
                        checkDirective = directive != null;

                        if (checkDirective) {
                            // handle use strict directive
                            if (elementType == STRING && "use strict".equals(directive)) {
                                final ParserContextFunctionNode function = lc.getCurrentFunction();
                                if (!function.isSimpleParameterList()) {
                                    throw error(AbstractParser.message("use.strict.non.simple.param"), lastStatement.getToken());
                                }

                                // We don't need to check these, if lexical environment is already
                                // strict
                                if (!oldStrictMode) {
                                    function.setFlag(FunctionNode.IS_STRICT);
                                    isStrictMode = true;

                                    verifyUseStrict(function, parseFlags);
                                } else {
                                    assert function.isStrict();
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    final int errorLine = line;
                    final long errorToken = token;
                    // recover parsing
                    recover(e);
                    final ErrorNode errorExpr = new ErrorNode(errorToken, finish);
                    final ExpressionStatement expressionStatement = new ExpressionStatement(errorLine, errorToken, finish, errorExpr);
                    appendStatement(expressionStatement);
                }

                // No backtracking from here on.
                stream.commit(k);
            }
        } finally {
            isStrictMode = oldStrictMode;
        }
    }

    private void verifyUseStrict(final ParserContextFunctionNode function, final int parseFlags) {
        // check that directives preceding this one do not violate strictness
        for (final Node statement : lc.peek().getStatements()) {
            // the getValue will force unescape of preceding escaped string directives
            getValue(statement.getToken());
        }

        // verify that function name as well as parameter names
        // satisfy strict mode restrictions.
        if (function.getIdent() != null) {
            verifyStrictIdent(function.getIdent(), "function name");
        }
        for (final IdentNode param : function.getParameters()) {
            verifyStrictIdent(param, FUNCTION_PARAMETER_CONTEXT);
        }

        // Strict mode eval always gets its own var scope.
        if ((parseFlags & PARSE_EVAL) != 0) {
            setupStrictEvalScope();
        }
    }

    /**
     * Switch from non-strict to strict mode eval.
     */
    private void setupStrictEvalScope() {
        ParserContextBlockNode body = lc.getCurrentBlock();
        assert body.getScope().getSymbolCount() == 0;
        if (body.getScope().isGlobalScope()) {
            Scope evalScope = Scope.createEval(body.getScope(), true);
            body.setScope(evalScope);
            ParserContextFunctionNode function = lc.getCurrentFunction();
            function.replaceBodyScope(evalScope);
            assert function.getBodyScope() == evalScope;
        }
    }

    /**
     * Parse any of the basic statement types.
     *
     * <pre>
     * Statement :
     *      BlockStatement
     *      VariableStatement
     *      EmptyStatement
     *      ExpressionStatement
     *      IfStatement
     *      BreakableStatement
     *      ContinueStatement
     *      BreakStatement
     *      ReturnStatement
     *      WithStatement
     *      LabelledStatement
     *      ThrowStatement
     *      TryStatement
     *      DebuggerStatement
     *      DebuggerStatement
     *
     * BreakableStatement :
     *      IterationStatement
     *      SwitchStatement
     *
     * BlockStatement :
     *      Block
     *
     * Block :
     *      { StatementList opt }
     *
     * StatementList :
     *      StatementListItem
     *      StatementList StatementListItem
     *
     * StatementItem :
     *      Statement
     *      Declaration
     *
     * Declaration :
     *     HoistableDeclaration
     *     ClassDeclaration
     *     LexicalDeclaration
     *
     * HoistableDeclaration :
     *     FunctionDeclaration
     *     GeneratorDeclaration
     * </pre>
     */
    private void statement(boolean yield, boolean await) {
        statement(yield, await, false, 0, false, false, false);
    }

    private void statement(boolean yield, boolean await, boolean topLevel, int reparseFlags, boolean singleStatement, boolean labelledStatement, boolean mayBeFunctionDeclaration) {
        statement(yield, await, topLevel, reparseFlags, singleStatement, labelledStatement, mayBeFunctionDeclaration, mayBeFunctionDeclaration);
    }

    /**
     * @param topLevel does this statement occur at the "top level" of a script or a function?
     * @param reparseFlags reparse flags to decide whether to allow property "get" and "set"
     *            functions or ES6 methods.
     * @param singleStatement are we in a single statement context?
     */
    private void statement(boolean yield, boolean await,
                    boolean topLevel, int reparseFlags, boolean singleStatement, boolean labelledStatement, boolean mayBeFunctionDeclaration, boolean mayBeLabeledFunctionDeclaration) {
        switch (type) {
            case LBRACE:
                block(yield, await);
                return;
            case VAR:
                variableStatement(type, yield, await);
                return;
            case SEMICOLON:
                emptyStatement();
                return;
            case IF:
                ifStatement(yield, await);
                return;
            case FOR:
                forStatement(yield, await);
                return;
            case WHILE:
                whileStatement(yield, await);
                return;
            case DO:
                doStatement(yield, await);
                return;
            case CONTINUE:
                continueStatement(yield, await);
                return;
            case BREAK:
                breakStatement(yield, await);
                return;
            case RETURN:
                returnStatement(yield, await);
                return;
            case WITH:
                withStatement(yield, await);
                return;
            case SWITCH:
                switchStatement(yield, await);
                return;
            case THROW:
                throwStatement(yield, await);
                return;
            case TRY:
                tryStatement(yield, await);
                return;
            case DEBUGGER:
                debuggerStatement();
                return;
            case RPAREN:
            case RBRACKET:
            case EOF:
                expect(SEMICOLON);
                return;
            case FUNCTION:
                // As per spec (ECMA section 12), function declarations as arbitrary statement
                // is not "portable". Implementation can issue a warning or disallow the same.
                if (singleStatement) {
                    // ES6 B.3.2 Labelled Function Declarations
                    // It is a Syntax Error if any strict mode source code matches this rule:
                    // LabelledItem : FunctionDeclaration.
                    // ES6 B.3.4 FunctionDeclarations in IfStatement Statement Clauses
                    if (isStrictMode || !mayBeFunctionDeclaration) {
                        throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "function declaration"), token);
                    }
                }
                functionDeclaration(true, topLevel || labelledStatement, singleStatement, yield, await, false);
                return;
            case LET:
                if (useBlockScope()) {
                    TokenType lookahead = lookaheadOfLetDeclaration();
                    if (lookahead != null) { // lookahead is let declaration
                        if (singleStatement) {
                            // ExpressionStatement should not have "let [" in its lookahead.
                            // The IDENT check is not needed here - the only purpose of this
                            // shortcut is to produce the same error mesage as Nashorn.
                            if (lookahead == LBRACKET || T(k + 1) == IDENT) {
                                throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "let declaration"), token);
                            } // else break and call expressionStatement()
                        } else {
                            variableStatement(type, yield, await);
                            return;
                        }
                    }
                }
                break;
            case CONST:
                if (useBlockScope()) {
                    if (singleStatement) {
                        throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "const declaration"), token);
                    }
                    variableStatement(type, yield, await);
                    return;
                } else if (env.constAsVar) {
                    variableStatement(TokenType.VAR, yield, await);
                    return;
                }
                break;
            case CLASS:
                if (ES6_CLASS && isES6()) {
                    if (singleStatement) {
                        throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "class declaration"), token);
                    }
                    classDeclaration(yield, await, false);
                    return;
                }
                break;
            case ASYNC:
                if (isAsync() && lookaheadIsAsyncFunction()) {
                    if (singleStatement) {
                        throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "async function declaration"), token);
                    }
                    asyncFunctionDeclaration(true, topLevel || labelledStatement, yield, await, false);
                    return;
                }
                break;
            default:
                break;
        }

        if (isBindingIdentifier()) {
            if (T(k + 1) == COLON && (type != YIELD || !yield) && (!isAwait() || !await)) {
                labelStatement(yield, await, mayBeLabeledFunctionDeclaration);
                return;
            }
            if (reparseFlags != 0 && reparseFunctionStatement(reparseFlags)) {
                return;
            }
        }

        expressionStatement(yield, await);
    }

    private boolean reparseFunctionStatement(final int reparseFlags) {
        final boolean allowPropertyFunction = (reparseFlags & REPARSE_IS_PROPERTY_ACCESSOR) != 0;
        final boolean isES6Method = (reparseFlags & REPARSE_IS_METHOD) != 0;
        if (allowPropertyFunction) {
            final long propertyToken = token;
            final int propertyLine = line;
            if (type == GET) {
                next();
                addPropertyFunctionStatement(propertyGetterFunction(propertyToken, propertyLine, false, false, false));
                return true;
            } else if (type == SET) {
                next();
                addPropertyFunctionStatement(propertySetterFunction(propertyToken, propertyLine, false, false, false));
                return true;
            }
        } else if (isES6Method) {
            final String ident = (String) getValue();
            IdentNode identNode = createIdentNode(token, finish, ident).setIsPropertyName();
            final long propertyToken = token;
            final int propertyLine = line;
            next();
            final int flags = CONSTRUCTOR_NAME.equals(ident) ? FunctionNode.IS_CLASS_CONSTRUCTOR : FunctionNode.IS_METHOD;
            addPropertyFunctionStatement(propertyMethodFunction(identNode, propertyToken, propertyLine, false, flags, false, false));
            return true;
        }
        return false;
    }

    private void addPropertyFunctionStatement(final PropertyFunction propertyFunction) {
        final FunctionNode fn = propertyFunction.functionNode;
        functionDeclarations.add(new ExpressionStatement(fn.getLineNumber(), fn.getToken(), finish, fn));
    }

    /**
     * Parse ClassDeclaration.
     *
     * <pre>
     * ClassDeclaration[Yield, Await, Default] :
     *   class BindingIdentifier[?Yield, ?Await] ClassTail[?Yield, ?Await]
     *   [+Default] class ClassTail[?Yield, ?Await]
     * </pre>
     *
     * @return Class expression node.
     */
    private ClassNode classDeclaration(boolean yield, boolean await, boolean defaultExport) {
        assert type == CLASS;
        int classLineNumber = line;
        long classToken = token;
        next();

        boolean oldStrictMode = isStrictMode;
        isStrictMode = true;
        try {
            IdentNode className = null;
            if (!defaultExport || isBindingIdentifier()) {
                className = bindingIdentifier(yield, await, CLASS_NAME_CONTEXT);
            }

            ClassNode classExpression = classTail(classLineNumber, classToken, className, yield, await);

            if (!defaultExport) {
                VarNode classVar = new VarNode(classLineNumber, Token.recast(classExpression.getToken(), LET), classExpression.getFinish(), className, classExpression, VarNode.IS_LET);
                appendStatement(classVar);
                declareVar(lc.getCurrentScope(), classVar);
            }
            return classExpression;
        } finally {
            isStrictMode = oldStrictMode;
        }
    }

    /**
     * Parse ClassExpression.
     *
     * <pre>
     * ClassExpression[Yield, Await] :
     *   class BindingIdentifier[?Yield, ?Await]opt ClassTail[?Yield, ?Await]
     * </pre>
     *
     * @return Class expression node.
     */
    private ClassNode classExpression(boolean yield, boolean await) {
        assert type == CLASS;
        int classLineNumber = line;
        long classToken = token;
        next();

        boolean oldStrictMode = isStrictMode;
        isStrictMode = true;
        try {
            IdentNode className = null;
            if (isBindingIdentifier()) {
                className = bindingIdentifier(yield, await, CLASS_NAME_CONTEXT);
            }

            return classTail(classLineNumber, classToken, className, yield, await);
        } finally {
            isStrictMode = oldStrictMode;
        }
    }

    /**
     * Parse ClassTail and ClassBody.
     *
     * <pre>
     * ClassTail[Yield] : ClassHeritage[?Yield]opt { ClassBody[?Yield]opt } ClassHeritage[Yield] :
     * extends LeftHandSideExpression[?Yield]
     *
     * ClassBody[Yield] :
     *      ClassElementList[?Yield]
     * ClassElementList[Yield] :
     *      ClassElement[?Yield]
     *      ClassElementList[?Yield] ClassElement[?Yield]
     * ClassElement[Yield] :
     *      MethodDefinition[?Yield]
     *      static MethodDefinition[?Yield]
     *      ;
     * </pre>
     */
    private ClassNode classTail(int classLineNumber, long classToken, IdentNode className, boolean yield, boolean await) {
        assert isStrictMode;
        Scope classHeadScope = Scope.createClassHead(lc.getCurrentScope());
        if (className != null) {
            classHeadScope.putSymbol(new Symbol(className.getName(), Symbol.IS_CONST));
        }
        ParserContextClassNode classNode = new ParserContextClassNode(classHeadScope);
        lc.push(classNode);
        try {
            Expression classHeritage = null;
            if (type == EXTENDS) {
                next();
                classHeritage = leftHandSideExpression(yield, await);

                // Note: ClassHeritage should be parsed in the outer PrivateEnvironment.
                // We have only one scope for both classScope and classPrivateEnvironment, so we
                // emulate this by verifying the private identifiers before any private fields are
                // declared. Unresolved uses are reported as errors or pushed up to the outer class.
                IdentNode invalidPrivateIdent = classNode.verifyAllPrivateIdentifiersValid(lc);
                if (invalidPrivateIdent != null) {
                    throw error(AbstractParser.message("invalid.private.ident"), invalidPrivateIdent.getToken());
                }
            }

            expect(LBRACE);

            Scope classScope = Scope.createClassBody(classHeadScope);
            classNode.setScope(classScope);

            PropertyNode constructor = null;
            ArrayList<PropertyNode> classElements = new ArrayList<>();
            Map<String, Integer> privateNameToAccessorIndexMap = new HashMap<>();
            int instanceFieldCount = 0;
            int staticElementCount = 0;
            boolean hasPrivateMethods = false;
            boolean hasPrivateInstanceMethods = false;
            for (;;) {
                if (type == SEMICOLON) {
                    next();
                    continue;
                }
                if (type == RBRACE) {
                    break;
                }
                boolean isStatic = false;
                if (type == STATIC) {
                    TokenType nextToken = lookahead();
                    if (nextToken != LPAREN && nextToken != ASSIGN && nextToken != SEMICOLON && nextToken != RBRACE) {
                        isStatic = true;
                        int staticLine = line;
                        long staticToken = token;
                        next();

                        if (type == LBRACE && isES2022()) {
                            // static initialization block
                            PropertyNode staticInit = staticInitializer(staticLine, staticToken);
                            staticElementCount++;
                            classElements.add(staticInit);
                            continue;
                        }
                    } // else method/field named 'static'
                }
                long classElementToken = token;
                int classElementLine = line;
                boolean async = false;
                if (isAsync() && lookaheadIsAsyncMethod(true)) {
                    async = true;
                    next();
                }
                boolean generator = false;
                if (type == MUL && ES6_GENERATOR_FUNCTION && isES6()) {
                    generator = true;
                    next();
                }

                final TokenType nameTokenType = type;
                final boolean computed = nameTokenType == LBRACKET;
                final Expression classElementName = classElementName(yield, await, true);

                PropertyNode classElement;
                if (!generator && !async && isClassFieldDefinition(nameTokenType)) {
                    classElement = fieldDefinition(classElementName, isStatic, classElementToken, computed);
                    if (isStatic) {
                        staticElementCount++;
                    } else {
                        instanceFieldCount++;
                    }
                } else {
                    classElement = methodDefinition(classElementName, isStatic, classHeritage != null, generator, async, classElementToken, classElementLine, yield, await, nameTokenType, computed);

                    if (!classElement.isComputed() && classElement.isAccessor()) {
                        if (classElement.isPrivate()) {
                            // merge private accessor methods
                            String privateName = classElement.getPrivateName();
                            Integer existing = privateNameToAccessorIndexMap.get(privateName);
                            if (existing == null) {
                                privateNameToAccessorIndexMap.put(privateName, classElements.size());
                            } else {
                                PropertyNode otherAccessor = classElements.get(existing);
                                if (isStatic == otherAccessor.isStatic()) {
                                    if (otherAccessor.getGetter() == null && classElement.getGetter() != null) {
                                        classElements.set(existing, otherAccessor.setGetter(classElement.getGetter()));
                                        continue;
                                    } else if (otherAccessor.getSetter() == null && classElement.getSetter() != null) {
                                        classElements.set(existing, otherAccessor.setSetter(classElement.getSetter()));
                                        continue;
                                    }
                                }
                                // else: more than one getter or setter with the same private name
                                // fall through: a syntax error will be thrown below
                            }
                        } else if (!classElements.isEmpty()) {
                            // try to merge consecutive getter and setter pairs
                            PropertyNode lastElement = classElements.get(classElements.size() - 1);
                            if (!lastElement.isComputed() && lastElement.isAccessor() && isStatic == lastElement.isStatic() &&
                                            !lastElement.isPrivate() && classElement.getKeyName().equals(lastElement.getKeyName())) {
                                PropertyNode merged = classElement.getGetter() != null ? lastElement.setGetter(classElement.getGetter()) : lastElement.setSetter(classElement.getSetter());
                                classElements.set(classElements.size() - 1, merged);
                                continue;
                            }
                        }
                    }
                }

                if (classElement.isPrivate()) {
                    hasPrivateMethods = hasPrivateMethods || !classElement.isClassField();
                    hasPrivateInstanceMethods = hasPrivateInstanceMethods || (!classElement.isClassField() && !classElement.isStatic());
                    declarePrivateName(classScope, classElement);
                }

                if (!classElement.isStatic() && !classElement.isComputed() && classElement.getKeyName().equals(CONSTRUCTOR_NAME)) {
                    assert !classElement.isClassField();
                    if (constructor == null) {
                        constructor = classElement;
                    } else {
                        throw error(AbstractParser.message("multiple.constructors"), classElementToken);
                    }
                } else {
                    classElements.add(classElement);
                }
            }

            long lastToken = token;
            expect(RBRACE);

            classElements.trimToSize();
            int classFinish = Token.descPosition(lastToken) + Token.descLength(lastToken);

            if (constructor == null) {
                constructor = createDefaultClassConstructor(classLineNumber, classToken, lastToken, className, classHeritage != null);
            } else {
                // constructor takes on source section of class declaration/expression
                FunctionNode ctor = (FunctionNode) constructor.getValue();
                int flags = ctor.getFlags();
                if (className == null) {
                    flags |= FunctionNode.IS_ANONYMOUS;
                }
                constructor = constructor.setValue(new FunctionNode(ctor.getSource(), ctor.getLineNumber(), ctor.getToken(), classFinish, classToken, lastToken, className,
                                className == null ? "" : className.getName(),
                                ctor.getLength(), ctor.getNumOfParams(), ctor.getParameters(), flags, ctor.getBody(), ctor.getEndParserState(), ctor.getModule(), ctor.getInternalName()));
            }

            IdentNode invalidPrivateIdent = classNode.verifyAllPrivateIdentifiersValid(lc);
            if (invalidPrivateIdent != null) {
                throw error(AbstractParser.message("invalid.private.ident"), invalidPrivateIdent.getToken());
            }

            if (hasPrivateMethods) {
                // synthetic binding providing access to the constructor for private brand checks.
                classScope.putSymbol(new Symbol(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME, Symbol.IS_CONST | Symbol.IS_PRIVATE_NAME | Symbol.HAS_BEEN_DECLARED));
            }

            classScope.close();
            classHeadScope.close();
            return new ClassNode(classToken, classFinish, className, classHeritage, constructor, classElements, classScope,
                            instanceFieldCount, staticElementCount, hasPrivateMethods, hasPrivateInstanceMethods);
        } finally {
            lc.pop(classNode);
        }
    }

    private Expression classElementName(boolean yield, boolean await, boolean allowPrivate) {
        if (allowPrivate && type == TokenType.PRIVATE_IDENT) {
            return privateIdentifierDeclaration();
        }
        return propertyName(yield, await);
    }

    private IdentNode parsePrivateIdentifier() {
        assert type == TokenType.PRIVATE_IDENT;
        if (!isClassFields() && !isES2021()) {
            throw error(AbstractParser.message("unexpected.token", type.getNameOrType()));
        }

        final long identToken = token;
        final String name = (String) getValue(identToken);
        next();

        return createIdentNode(identToken, finish, name).setIsPrivate();
    }

    private IdentNode privateIdentifierDeclaration() {
        IdentNode privateIdent = parsePrivateIdentifier();

        ParserContextClassNode currentClass = lc.getCurrentClass();
        if (currentClass == null) {
            throw error(AbstractParser.message("invalid.private.ident"), privateIdent.getToken());
        }

        return privateIdent;
    }

    private void declarePrivateName(Scope classScope, PropertyNode classElement) {
        // Syntax Error if PrivateBoundIdentifiers of ClassBody contains any duplicate entries,
        // unless the name is used once for a getter and once for a setter and in no other entries.
        int privateFlags = (classElement.isStatic() ? Symbol.IS_PRIVATE_NAME_STATIC : 0);
        if (!classElement.isClassField()) {
            privateFlags |= classElement.isAccessor() ? Symbol.IS_PRIVATE_NAME_ACCESSOR : Symbol.IS_PRIVATE_NAME_METHOD;
        }
        if (!classScope.addPrivateName(classElement.getPrivateName(), privateFlags)) {
            throw error(ECMAErrors.getMessage("syntax.error.redeclare.variable", classElement.getPrivateName()), classElement.getKey().getToken());
        }
    }

    private IdentNode privateIdentifierUse() {
        IdentNode privateIdent = parsePrivateIdentifier();

        Scope currentScope = lc.getCurrentScope();
        ParserContextClassNode currentClass = lc.getCurrentClass();
        // In a class: try to eagerly resolve the private identifier; if it is not found,
        // defer resolving until the end of the class declaration.
        // In a direct eval: try to find a resolved private identifier in the caller scopes.
        if (currentClass != null) {
            currentClass.usePrivateName(privateIdent);
        } else {
            if (!currentScope.findPrivateName(privateIdent.getName())) {
                throw error(AbstractParser.message("invalid.private.ident"), privateIdent.getToken());
            }
        }

        currentScope.addIdentifierReference(privateIdent.getName());

        return privateIdent;
    }

    private boolean isClassFieldDefinition(final TokenType nameTokenType) {
        if (!isClassFields()) {
            return false;
        }

        switch (type) {
            case ASSIGN:
            case SEMICOLON:
            case RBRACE:
                // must be a field
                return true;
            case LPAREN:
                // must be a method
                return false;
            default:
                if (nameTokenType == GET || nameTokenType == SET) {
                    // `get` or `set` not followed by `;`, `=`, or `}`, must be an accessor method
                    return false;
                }
                // not a method, either a field or syntax error
                if (last == EOL) {
                    // field (automatic semicolon insertion)
                    return true;
                } else {
                    // syntax error
                    return false;
                }
        }
    }

    private PropertyNode createDefaultClassConstructor(int classLineNumber, long classToken, long lastToken, IdentNode className, boolean derived) {
        final int ctorFinish = finish;
        final List<Statement> statements;
        final List<IdentNode> parameters;
        final long identToken = Token.recast(classToken, TokenType.IDENT);
        if (derived) {
            IdentNode superIdent = new IdentNode(identToken, ctorFinish, SUPER.getName()).setIsDirectSuper();
            IdentNode argsIdent = new IdentNode(identToken, ctorFinish, "args").setIsRestParameter();
            Expression spreadArgs = new UnaryNode(Token.recast(classToken, TokenType.SPREAD_ARGUMENT), argsIdent);
            Expression superCall = CallNode.forCall(classLineNumber, classToken, Token.descPosition(classToken), ctorFinish, superIdent, Collections.singletonList(spreadArgs), false, false, false,
                            false, true);
            statements = Collections.singletonList(new ExpressionStatement(classLineNumber, classToken, ctorFinish, superCall));
            parameters = Collections.singletonList(argsIdent);
        } else {
            statements = Collections.emptyList();
            parameters = Collections.emptyList();
        }
        int functionFlags = FunctionNode.IS_METHOD | FunctionNode.IS_CLASS_CONSTRUCTOR;
        ParserContextFunctionNode function = createParserContextFunctionNode(className, classToken, functionFlags, classLineNumber, parameters, 0);
        function.setLastToken(lastToken);

        Scope scope = function.createBodyScope();
        scope.close();
        Block body = new Block(classToken, ctorFinish, Block.IS_BODY, scope, statements);

        if (derived) {
            function.setFlag(FunctionNode.IS_DERIVED_CONSTRUCTOR);
            function.setFlag(FunctionNode.HAS_DIRECT_SUPER);
        }
        if (className == null) {
            function.setFlag(FunctionNode.IS_ANONYMOUS);
            function.setInternalName(CONSTRUCTOR_NAME);
        }

        // currently required for all functions, including synthetic ones.
        lc.setCurrentFunctionFlag(FunctionNode.HAS_CLOSURES);

        PropertyNode constructor = new PropertyNode(classToken, ctorFinish, new IdentNode(identToken, ctorFinish, CONSTRUCTOR_NAME),
                        createFunctionNode(function, classToken, className, classLineNumber, body),
                        null, null, false, false, false, false);
        return constructor;
    }

    private PropertyNode methodDefinition(Expression propertyName, boolean isStatic, boolean derived, boolean generator, boolean async, long startToken, int methodLine, boolean yield,
                    boolean await, TokenType nameTokenType, boolean computed) {
        int flags = FunctionNode.IS_METHOD;
        if (!computed) {
            final String name = ((PropertyKey) propertyName).getPropertyName();
            if (!generator && nameTokenType == GET && type != LPAREN) {
                PropertyFunction methodDefinition = propertyGetterFunction(startToken, methodLine, yield, await, true);
                verifyAllowedMethodName(methodDefinition.key, isStatic, methodDefinition.computed, generator, true, async);
                return new PropertyNode(startToken, finish, methodDefinition.key, null, methodDefinition.functionNode, null, isStatic, methodDefinition.computed, false, false);
            } else if (!generator && nameTokenType == SET && type != LPAREN) {
                PropertyFunction methodDefinition = propertySetterFunction(startToken, methodLine, yield, await, true);
                verifyAllowedMethodName(methodDefinition.key, isStatic, methodDefinition.computed, generator, true, async);
                return new PropertyNode(startToken, finish, methodDefinition.key, null, null, methodDefinition.functionNode, isStatic, methodDefinition.computed, false, false);
            } else {
                if (!isStatic && !generator && name.equals(CONSTRUCTOR_NAME)) {
                    flags |= FunctionNode.IS_CLASS_CONSTRUCTOR;
                    if (derived) {
                        flags |= FunctionNode.IS_DERIVED_CONSTRUCTOR;
                    }
                }
                verifyAllowedMethodName(propertyName, isStatic, computed, generator, false, async);
            }
        }
        PropertyFunction methodDefinition = propertyMethodFunction(propertyName, startToken, methodLine, generator, flags, computed, async);
        return new PropertyNode(startToken, finish, methodDefinition.key, methodDefinition.functionNode, null, null, isStatic, computed, false, false);
    }

    /**
     * ES6 14.5.1 Static Semantics: Early Errors.
     */
    private void verifyAllowedMethodName(Expression key, boolean isStatic, boolean computed, boolean generator, boolean accessor, boolean async) {
        if (!computed) {
            final String name = ((PropertyKey) key).getPropertyName();
            if (!isStatic && generator && name.equals(CONSTRUCTOR_NAME)) {
                throw error(AbstractParser.message("generator.constructor"), key.getToken());
            }
            if (!isStatic && accessor && name.equals(CONSTRUCTOR_NAME)) {
                throw error(AbstractParser.message("accessor.constructor"), key.getToken());
            }
            if (!isStatic && async && name.equals(CONSTRUCTOR_NAME)) {
                throw error(AbstractParser.message("async.constructor"), key.getToken());
            }
            if (isStatic && name.equals(PROTOTYPE_NAME)) {
                throw error(AbstractParser.message("static.prototype.method"), key.getToken());
            }
            if (name.equals(PRIVATE_CONSTRUCTOR_NAME)) {
                throw error(AbstractParser.message("private.constructor.method"), key.getToken());
            }
        }
    }

    private PropertyNode fieldDefinition(Expression propertyName, boolean isStatic, long startToken, boolean computed) {
        // "constructor" or #constructor is not allowed as an instance field name
        if (!computed && propertyName instanceof PropertyKey) {
            String name = ((PropertyKey) propertyName).getPropertyName();
            if (CONSTRUCTOR_NAME.equals(name) || PRIVATE_CONSTRUCTOR_NAME.equals(name)) {
                throw error(AbstractParser.message("constructor.field"), startToken);
            }
            if (isStatic && PROTOTYPE_NAME.equals(name)) {
                throw error(AbstractParser.message("static.prototype.field"), startToken);
            }
        }

        FunctionNode initializer = null;
        boolean isAnonymousFunctionDefinition = false;
        if (type == ASSIGN) {
            next();

            // Parse AssignmentExpression[In] in a function.
            Pair<FunctionNode, Boolean> pair = fieldInitializer(line, startToken, propertyName, computed);
            initializer = pair.getLeft();
            isAnonymousFunctionDefinition = pair.getRight();

            endOfLine(); // semicolon or end of line
        }
        return new PropertyNode(startToken, finish, propertyName, initializer, null, null, isStatic, computed, false, false, true, isAnonymousFunctionDefinition);
    }

    private Pair<FunctionNode, Boolean> fieldInitializer(int lineNumber, long fieldToken, Expression propertyName, boolean computed) {
        int functionFlags = FunctionNode.IS_METHOD | FunctionNode.IS_CLASS_FIELD_INITIALIZER | FunctionNode.IS_ANONYMOUS;
        ParserContextFunctionNode function = createParserContextFunctionNode(null, fieldToken, functionFlags, lineNumber, Collections.emptyList(), 0);
        function.setInternalName(INITIALIZER_FUNCTION_NAME);
        lc.push(function);
        ParserContextBlockNode body = newBlock(function.createBodyScope());
        Expression initializer;
        try {
            initializer = assignmentExpression(true, false, false);
        } finally {
            function.finishBodyScope();
            restoreBlock(body);
            lc.propagateFunctionFlags();
            lc.pop(function);
        }

        // It is a Syntax Error if ContainsArguments of Initializer is true.
        assert function.getFlag(FunctionNode.USES_ARGUMENTS) == 0;
        function.setLastToken(token);

        boolean isAnonymousFunctionDefinition = false;
        if (isAnonymousFunctionDefinition(initializer)) {
            if (!computed && propertyName instanceof PropertyKey) {
                initializer = setAnonymousFunctionName(initializer, ((PropertyKey) propertyName).getPropertyName());
            } else {
                isAnonymousFunctionDefinition = true;
                initializer = new UnaryNode(Token.recast(initializer.getToken(), TokenType.NAMEDEVALUATION), initializer);
            }
        }

        // currently required for all functions, including synthetic ones.
        lc.setCurrentFunctionFlag(FunctionNode.HAS_CLOSURES);

        final List<Statement> statements = Collections.singletonList(new ReturnNode(lineNumber, fieldToken, finish, initializer));
        Block bodyBlock = new Block(fieldToken, finish, Block.IS_BODY | Block.IS_SYNTHETIC, body.getScope(), statements);
        return Pair.create(createFunctionNode(function, fieldToken, null, lineNumber, bodyBlock), isAnonymousFunctionDefinition);
    }

    /**
     * Parse <code>{ ClassStaticBlockBody }</code>.
     */
    private PropertyNode staticInitializer(int lineNumber, long staticToken) {
        assert type == LBRACE;
        int functionFlags = FunctionNode.IS_METHOD | FunctionNode.IS_CLASS_FIELD_INITIALIZER | FunctionNode.IS_ANONYMOUS;
        ParserContextFunctionNode function = createParserContextFunctionNode(null, staticToken, functionFlags, lineNumber, Collections.emptyList(), 0);
        function.setInternalName(INITIALIZER_FUNCTION_NAME);
        lc.push(function);
        Block bodyBlock;
        try {
            bodyBlock = functionBody(function);
        } finally {
            lc.pop(function);
        }

        // It is a Syntax Error if ContainsArguments of Initializer is true.
        assert function.getFlag(FunctionNode.USES_ARGUMENTS) == 0;

        // currently required for all nested functions.
        lc.setCurrentFunctionFlag(FunctionNode.HAS_CLOSURES);

        FunctionNode functionNode = createFunctionNode(function, staticToken, null, lineNumber, bodyBlock);
        return new PropertyNode(staticToken, finish, null, functionNode, null, null, true, false, false, false, false, false);
    }

    private boolean isPropertyName(long currentToken) {
        TokenType currentType = Token.descType(currentToken);
        if (ES6_COMPUTED_PROPERTY_NAME && currentType == LBRACKET && isES6()) {
            // computed property
            return true;
        }

        switch (currentType) {
            case IDENT:
                return true;
            case NON_OCTAL_DECIMAL:
            case OCTAL_LEGACY:
                if (isStrictMode) {
                    return false;
                }
            case STRING:
            case ESCSTRING:
            case DECIMAL:
            case HEXADECIMAL:
            case OCTAL:
            case BINARY_NUMBER:
            case BIGINT:
            case FLOATING:
                return true;
            default:
                return isIdentifierName(currentToken);
        }
    }

    /**
     * Parse a statement block.
     *
     * <pre>
     * Block :
     *      { StatementList? }
     * </pre>
     */
    private void block(boolean yield, boolean await) {
        appendStatement(new BlockStatement(line, getBlock(yield, await, true)));
    }

    /**
     * Parse a list of statements.
     *
     * <pre>
     * StatementList :
     *      Statement
     *      StatementList Statement
     * </pre>
     */
    private void statementList(boolean yield, boolean await) {
        // Accumulate statements until end of the statement list.
        loop: while (type != EOF) {
            switch (type) {
                case EOF:
                case CASE:
                case DEFAULT:
                case RBRACE:
                    break loop;
                default:
                    break;
            }

            // Get next statement.
            statement(yield, await);
        }
    }

    /**
     * Make sure that the identifier name used is allowed.
     *
     * @param ident the identifier that is verified
     */
    private void verifyIdent(final IdentNode ident, final boolean yield, final boolean await) {
        // It is a Syntax Error if StringValue of IdentifierName is the same as the StringValue of
        // any ReservedWord except for yield or await.
        if (isES6()) {
            if (isEscapedIdent(ident) && isReservedWordSequence(ident.getName())) {
                throw error(AbstractParser.message(MESSAGE_ESCAPED_KEYWORD, ident.getName()), ident.getToken());
            } else {
                assert !isReservedWordSequence(ident.getName()) : ident.getName();
            }
        }
        // It is a Syntax Error if this production has a [Yield] parameter and StringValue of
        // Identifier is "yield".
        if (yield) {
            if (ident.isTokenType(YIELD)) {
                throw error(expectMessage(IDENT, ident.getToken()), ident.getToken());
            } else if (isEscapedIdent(ident) && YIELD.getName().equals(ident.getName())) {
                throw error(AbstractParser.message(MESSAGE_ESCAPED_KEYWORD, ident.getName()), ident.getToken());
            } else {
                assert !YIELD.getName().equals(ident.getName());
            }
        }
        // It is a Syntax Error if this production has an [Await] parameter or if the goal symbol
        // of the syntactic grammar is Module and StringValue of Identifier is "await".
        // If we are inside CoverCallExpressionAndAsyncArrowHead, record the token for later
        // error handling: "await" is not a valid identifier iff it occurs inside AsyncArrowHead.
        boolean awaitOrModule = await || isModule;
        if (ident.isTokenType(AWAIT)) {
            if (awaitOrModule) {
                throw error(expectMessage(IDENT, ident.getToken()), ident.getToken());
            } else {
                recordYieldOrAwait(ident);
            }
        } else if (isEscapedIdent(ident) && AWAIT.getName().equals(ident.getName())) {
            if (awaitOrModule) {
                throw error(AbstractParser.message(MESSAGE_ESCAPED_KEYWORD, ident.getName()), ident.getToken());
            } else {
                recordYieldOrAwait(ident);
            }
        } else {
            assert !AWAIT.getName().equals(ident.getName());
        }
    }

    private static boolean isEscapedIdent(final IdentNode ident) {
        return ident.getName().length() != Token.descLength(ident.getToken());
    }

    private static boolean isReservedWordSequence(final String name) {
        TokenType tokenType = TokenLookup.lookupKeyword(name.toCharArray(), 0, name.length());
        return (tokenType != IDENT && !tokenType.isContextualKeyword() && !tokenType.isFutureStrict());
    }

    /**
     * Make sure that in strict mode, the identifier name used is allowed.
     *
     * @param ident Identifier that is verified
     * @param contextString String used in error message to give context to the user
     */
    private void verifyStrictIdent(final IdentNode ident, final String contextString, final boolean bindingIdentifier) {
        if (isStrictMode) {
            if (!isValidStrictIdent(ident, bindingIdentifier)) {
                throw error(AbstractParser.message("strict.name", ident.getName(), contextString), ident.getToken());
            }
        }
    }

    private void verifyStrictIdent(final IdentNode ident, final String contextString) {
        verifyStrictIdent(ident, contextString, true);
    }

    private static boolean isValidStrictIdent(final IdentNode ident, final boolean bindingIdentifier) {
        if (bindingIdentifier) {
            switch (ident.getName()) {
                case EVAL_NAME:
                case ARGUMENTS_NAME:
                    return false;
                default:
                    break;
            }
        }

        if (isFutureStrictName(ident)) {
            return false;
        }
        return true;
    }

    /**
     * Check if this IdentNode is a future strict name
     *
     * @return true if this is a future strict name
     */
    private static boolean isFutureStrictName(final IdentNode ident) {
        if (ident.tokenType().isFutureStrict()) {
            return true;
        } else if (isEscapedIdent(ident)) {
            TokenType tokenType = TokenLookup.lookupKeyword(ident.getName().toCharArray(), 0, ident.getName().length());
            return (tokenType != IDENT && tokenType.isFutureStrict());
        }
        return false;
    }

    /**
     * Parse a var statement.
     *
     * <pre>
     * VariableStatement :
     *      var VariableDeclarationList ;
     *
     * VariableDeclarationList :
     *      VariableDeclaration
     *      VariableDeclarationList , VariableDeclaration
     *
     * VariableDeclaration :
     *      Identifier Initializer?
     *
     * Initializer :
     *      = AssignmentExpression
     * </pre>
     */
    private void variableStatement(final TokenType varType, boolean yield, boolean await) {
        variableDeclarationList(varType, true, yield, await, -1);
    }

    private static final class ForVariableDeclarationListResult {
        /** First missing const or binding pattern initializer. */
        Expression missingAssignment;
        /** First declaration with an initializer. */
        long declarationWithInitializerToken;
        /** Destructuring assignments. */
        Expression init;
        Expression firstBinding;
        Expression secondBinding;

        void recordMissingAssignment(Expression binding) {
            if (missingAssignment == null) {
                missingAssignment = binding;
            }
        }

        void recordDeclarationWithInitializer(long token) {
            if (declarationWithInitializerToken == 0L) {
                declarationWithInitializerToken = token;
            }
        }

        void addBinding(Expression binding) {
            if (firstBinding == null) {
                firstBinding = binding;
            } else if (secondBinding == null) {
                secondBinding = binding;
            }
            // ignore the rest
        }

        void addAssignment(Expression assignment) {
            if (init == null) {
                init = assignment;
            } else {
                init = new BinaryNode(Token.recast(init.getToken(), COMMARIGHT), init, assignment);
            }
        }
    }

    /**
     * Parse VariableDeclarationList[In, Yield, Await].
     *
     * @param isStatement Same as In flag; {@code true} if a VariableStatement, {@code false} if a
     *            {@code for} loop VariableDeclarationList
     */
    private ForVariableDeclarationListResult variableDeclarationList(TokenType varType, boolean isStatement, boolean yield, boolean await, int sourceOrder) {
        // VAR tested in caller.
        int varStart = Token.descPosition(token);
        assert varType == VAR || varType == LET || varType == CONST;
        next();

        int varFlags = 0;
        if (varType == LET) {
            varFlags |= VarNode.IS_LET;
        } else if (varType == CONST) {
            varFlags |= VarNode.IS_CONST;
        }

        ForVariableDeclarationListResult forResult = isStatement ? null : new ForVariableDeclarationListResult();
        Scope scope = lc.getCurrentScope();
        while (true) {
            // Get starting token.
            final int varLine = line;
            final long varToken = Token.recast(token, varType);

            // Get name of var.
            final Expression binding = bindingIdentifierOrPattern(yield, await, VARIABLE_NAME_CONTEXT);
            final boolean isDestructuring = !(binding instanceof IdentNode);
            if (isDestructuring) {
                final int finalVarFlags = varFlags | VarNode.IS_DESTRUCTURING;
                verifyDestructuringBindingPattern(binding, new Consumer<IdentNode>() {
                    @Override
                    public void accept(IdentNode identNode) {
                        verifyStrictIdent(identNode, VARIABLE_NAME_CONTEXT);
                        if (varType != VAR && identNode.getName().equals(LET.getName())) {
                            // ES8 13.3.1.1
                            throw error(AbstractParser.message("let.lexical.binding"));
                        }
                        final VarNode var = new VarNode(varLine, varToken, sourceOrder, identNode.getFinish(), identNode.setIsDeclaredHere(), null, finalVarFlags);
                        appendStatement(var);
                        declareVar(scope, var);
                    }
                });
            }

            // Assume no init.
            Expression init = null;

            // Look for initializer assignment.
            if (type == ASSIGN) {
                if (!isStatement) {
                    forResult.recordDeclarationWithInitializer(varToken);
                }
                next();

                // Get initializer expression. Suppress IN if not statement.
                if (!isDestructuring) {
                    pushDefaultName(binding);
                }
                try {
                    init = assignmentExpression(isStatement, yield, await);
                } finally {
                    if (!isDestructuring) {
                        popDefaultName();
                    }
                }
            } else if (isStatement) {
                if (isDestructuring) {
                    throw error(AbstractParser.message("missing.destructuring.assignment"), token);
                } else if (varType == CONST) {
                    throw error(AbstractParser.message("missing.const.assignment", ((IdentNode) binding).getName()));
                }
                // else, if we are in a for loop, delay checking until we know the kind of loop
            }

            if (!isDestructuring) {
                assert init != null || varType != CONST || !isStatement;
                final IdentNode ident = (IdentNode) binding;
                if (varType != VAR && ident.getName().equals(LET.getName())) {
                    throw error(AbstractParser.message("let.lexical.binding")); // ES8 13.3.1.1
                }
                if (!isStatement) {
                    if (init == null && varType == CONST) {
                        forResult.recordMissingAssignment(binding);
                    }
                    forResult.addBinding(binding);
                }
                if (isAnonymousFunctionDefinition(init)) {
                    init = setAnonymousFunctionName(init, ident.getName());
                }
                final VarNode var = new VarNode(varLine, varToken, sourceOrder, varStart, finish, ident.setIsDeclaredHere(), init, varFlags);
                appendStatement(var);
                declareVar(scope, var);
            } else {
                assert init != null || !isStatement;
                if (init != null) {
                    final Expression assignment = verifyAssignment(Token.recast(varToken, ASSIGN_INIT), binding, init, true);
                    if (isStatement) {
                        appendStatement(new ExpressionStatement(varLine, assignment.getToken(), finish, assignment));
                    } else {
                        forResult.addAssignment(assignment);
                        forResult.addBinding(assignment);
                    }
                } else if (!isStatement) {
                    forResult.recordMissingAssignment(binding);
                    forResult.addBinding(binding);
                }
            }

            if (type != COMMARIGHT) {
                break;
            }
            next();
        }

        // If is a statement then handle end of line.
        if (isStatement) {
            endOfLine();
        }
        return forResult;
    }

    private void declareVar(Scope scope, VarNode varNode) {
        String name = varNode.getName().getName();
        if (detectVarNameConflict(scope, varNode)) {
            throw error(ECMAErrors.getMessage("syntax.error.redeclare.variable", name), varNode.getToken());
        }
        if (varNode.isBlockScoped()) {
            int symbolFlags = varNode.getSymbolFlags() |
                            (scope.isSwitchBlockScope() ? Symbol.IS_DECLARED_IN_SWITCH_BLOCK : 0) |
                            (varNode.isFunctionDeclaration() ? Symbol.IS_BLOCK_FUNCTION_DECLARATION : 0);
            Symbol existing = scope.putSymbol(new Symbol(name, symbolFlags));
            assert existing == null || (existing.isBlockFunctionDeclaration() && varNode.isFunctionDeclaration()) : existing;

            if (varNode.isFunctionDeclaration() && isAnnexB()) {
                // B.3.3 Block-Level Function Declaration hoisting
                // B.3.3.1 Changes to FunctionDeclarationInstantiation
                // B.3.3.2 Changes to GlobalDeclarationInstantiation
                ParserContextFunctionNode function = lc.getCurrentFunction();
                Scope varScope = function.getBodyScope();
                if (!function.isStrict() && scope != varScope && (varScope.isGlobalScope() || !name.equals(ARGUMENTS_NAME))) {
                    assert !scope.isFunctionBodyScope() && !scope.isFunctionParameterScope();
                    // If we already find a conflicting declaration, we can skip this step.
                    if (varScope.getExistingSymbol(name) == null && !scope.getParent().isLexicallyDeclaredName(name, true, true)) {
                        function.recordHoistableBlockFunctionDeclaration(varNode, scope);
                    }
                }
            }
        } else {
            // var declarations are added to the function body scope (a.k.a. VariableEnvironment).
            ParserContextFunctionNode function = lc.getCurrentFunction();
            Scope varScope = function.getBodyScope();
            int symbolFlags = varNode.getSymbolFlags() |
                            (varNode.isHoistableDeclaration() ? Symbol.IS_HOISTABLE_DECLARATION : 0) |
                            (varScope.isGlobalScope() ? Symbol.IS_GLOBAL : 0);
            // if the var name appears in a non-simple parameter list, we need to copy its value.
            if (function.hasParameterExpressions() && function.getParameterBlock().getScope().hasSymbol(name)) {
                symbolFlags |= Symbol.IS_VAR_REDECLARED_HERE;
            }
            varScope.putSymbol(new Symbol(name, symbolFlags));

            /*
             * Hoisted var declarations conflict with any lexical declaration of the same name in
             * this scope and any intermediate scopes. We cannot verify this in advance since we do
             * not know all the lexically declared names for the outer scopes yet, if any.
             *
             * e.g.: `{ { var x; } let x; }`
             *
             * So unless we are in the var declaration (a.k.a. top-level) scope, remember that there
             * has been a declaration in this scope and defer the duplicate check until the var
             * declaration scope is finalized.
             */
            if (scope != varScope) {
                assert scope.isBlockScope();
                function.recordHoistedVarDeclaration(varNode, scope);
            }
        }
    }

    private boolean detectVarNameConflict(Scope scope, VarNode varNode) {
        String varName = varNode.getName().getName();
        if (varNode.isBlockScoped()) {
            Scope currentScope = scope;
            Symbol existingSymbol = currentScope.getExistingSymbol(varName);
            if (existingSymbol != null) {
                // B.3.3.4 Changes to Block Static Semantics: Early Errors
                // In non-strict mode, allow duplicate function declarations in a block.
                if (existingSymbol.isBlockFunctionDeclaration() && !isStrictMode && isAnnexB() && varNode.isFunctionDeclaration()) {
                    return false;
                } else {
                    return true;
                }
            } else {
                Scope parentScope = scope.getParent();
                if (parentScope != null && (parentScope.isCatchParameterScope() || parentScope.isFunctionParameterScope())) {
                    existingSymbol = parentScope.getExistingSymbol(varName);
                    if (existingSymbol != null && !existingSymbol.isArguments()) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return scope.isLexicallyDeclaredName(varName, isAnnexB(), false);
        }
    }

    private boolean isAnnexB() {
        return env.annexB;
    }

    private boolean isIdentifier() {
        return type == IDENT || type.isContextualKeyword() || isNonStrictModeIdent();
    }

    private IdentNode identifier(boolean yield, boolean await, String contextString, boolean bindingIdentifier) {
        final IdentNode ident = getIdent();
        verifyIdent(ident, yield, await);
        verifyStrictIdent(ident, contextString, bindingIdentifier);
        return ident;
    }

    private IdentNode identifierReference(boolean yield, boolean await) {
        IdentNode ident = identifier(yield, await, "IdentifierReference", false);
        addIdentifierReference(ident.getName());
        return ident;
    }

    private IdentNode labelIdentifier(boolean yield, boolean await) {
        return identifier(yield, await, "LabelIdentifier", false);
    }

    private boolean isBindingIdentifier() {
        return type == IDENT || type.isContextualKeyword() || isNonStrictModeIdent();
    }

    private IdentNode bindingIdentifier(boolean yield, boolean await, String contextString) {
        IdentNode ident = identifier(yield, await, contextString, true);
        addIdentifierReference(ident.getName());
        return ident;
    }

    private void addIdentifierReference(String name) {
        Scope currentScope = lc.getCurrentScope();
        if (currentScope != null) { // can be null when parsing/verifying a parameter list.
            currentScope.addIdentifierReference(name);
        }
    }

    private Expression bindingPattern(boolean yield, boolean await) {
        if (type == LBRACKET) {
            return arrayLiteral(yield, await);
        } else if (type == LBRACE) {
            return objectLiteral(yield, await);
        } else {
            throw error(AbstractParser.message("expected.binding"));
        }
    }

    private Expression bindingIdentifierOrPattern(boolean yield, boolean await, String contextString) {
        if (isBindingIdentifier() || !(ES6_DESTRUCTURING && isES6())) {
            return bindingIdentifier(yield, await, contextString);
        } else {
            return bindingPattern(yield, await);
        }
    }

    private abstract class VerifyDestructuringPatternNodeVisitor extends NodeVisitor<LexicalContext> {
        VerifyDestructuringPatternNodeVisitor(LexicalContext lc) {
            super(lc);
        }

        @Override
        public boolean enterLiteralNode(LiteralNode<?> literalNode) {
            if (literalNode.isArray()) {
                if (literalNode.isParenthesized()) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), literalNode.getToken());
                }
                if (((ArrayLiteralNode) literalNode).hasSpread() && ((ArrayLiteralNode) literalNode).hasTrailingComma()) {
                    throw error("Rest element must be last", literalNode.getElementExpressions().get(literalNode.getElementExpressions().size() - 1).getToken());
                }
                boolean restElement = false;
                for (Expression element : literalNode.getElementExpressions()) {
                    if (element != null) {
                        if (restElement) {
                            throw error("Unexpected element after rest element", element.getToken());
                        }
                        if (element.isTokenType(SPREAD_ARRAY)) {
                            restElement = true;
                            Expression lvalue = ((UnaryNode) element).getExpression();
                            verifySpreadElement(lvalue);
                        } else {
                            element.accept(this);
                        }
                    }
                }
                return false;
            } else {
                return enterDefault(literalNode);
            }
        }

        protected abstract void verifySpreadElement(Expression lvalue);

        @Override
        public boolean enterObjectNode(ObjectNode objectNode) {
            if (objectNode.isParenthesized()) {
                throw error(AbstractParser.message(MESSAGE_INVALID_LVALUE), objectNode.getToken());
            }
            boolean restElement = false;
            for (PropertyNode property : objectNode.getElements()) {
                if (property != null) {
                    if (restElement) {
                        throw error("Unexpected element after rest element", property.getToken());
                    }
                    Expression key = property.getKey();
                    if (key.isTokenType(SPREAD_OBJECT)) {
                        restElement = true;
                        Expression lvalue = ((UnaryNode) key).getExpression();
                        verifySpreadElement(lvalue);
                    } else {
                        property.accept(this);
                    }
                }
            }
            return false;
        }

        @Override
        public boolean enterPropertyNode(PropertyNode propertyNode) {
            if (propertyNode.getValue() != null) {
                propertyNode.getValue().accept(this);
                return false;
            } else {
                return enterDefault(propertyNode);
            }
        }

        @Override
        public boolean enterBinaryNode(BinaryNode binaryNode) {
            if (binaryNode.isTokenType(ASSIGN)) {
                binaryNode.getLhs().accept(this);
                // Initializer(rhs) can be any AssignmentExpression
                return false;
            } else {
                return enterDefault(binaryNode);
            }
        }
    }

    /**
     * Verify destructuring variable declaration binding pattern and extract bound variable
     * declarations.
     */
    private void verifyDestructuringBindingPattern(Expression pattern, Consumer<IdentNode> identifierCallback) {
        assert pattern instanceof ObjectNode || pattern instanceof ArrayLiteralNode;
        pattern.accept(new VerifyDestructuringPatternNodeVisitor(new LexicalContext()) {
            @Override
            protected void verifySpreadElement(Expression lvalue) {
                if (lvalue instanceof IdentNode) {
                    enterIdentNode((IdentNode) lvalue);
                } else if (isDestructuringLhs(lvalue)) {
                    verifyDestructuringBindingPattern(lvalue, identifierCallback);
                } else {
                    throw error("Expected a valid binding identifier", lvalue.getToken());
                }
            }

            @Override
            public boolean enterIdentNode(IdentNode identNode) {
                if (identNode.isParenthesized()) {
                    throw error("Expected a valid binding identifier", identNode.getToken());
                }
                identifierCallback.accept(identNode);
                return false;
            }

            @Override
            protected boolean enterDefault(Node node) {
                throw error(String.format("unexpected node in BindingPattern: %s", node));
            }
        });
    }

    /**
     * Parse an empty statement ({@code ;}).
     */
    private void emptyStatement() {
        if (env.emptyStatements) {
            appendStatement(new EmptyNode(line, token, Token.descPosition(token) + Token.descLength(token)));
        }

        // SEMICOLON checked in caller.
        next();
    }

    /**
     * ExpressionStatement : Expression ; // [lookahead ~( or function )]
     *
     * See 12.4
     *
     * Parse an expression used in a statement block.
     */
    private void expressionStatement(boolean yield, boolean await) {
        // Lookahead checked in caller.
        final int expressionLine = line;
        final long expressionToken = token;

        // Get expression and add as statement.
        final Expression expression = expression(yield, await);

        if (expression != null) {
            endOfLine();
            ExpressionStatement expressionStatement = new ExpressionStatement(expressionLine, expressionToken, finish, expression);
            appendStatement(expressionStatement);
        } else {
            expect(null);
            endOfLine();
        }
    }

    /**
     * Parse an if statement.
     *
     * <pre>
     * IfStatement :
     *      if ( Expression ) Statement else Statement
     *      if ( Expression ) Statement
     * </pre>
     */
    private void ifStatement(boolean yield, boolean await) {
        // Capture IF token.
        final int ifLine = line;
        final long ifToken = token;
        // IF tested in caller.
        next();

        expect(LPAREN);
        final Expression test = expression(yield, await);
        expect(RPAREN);
        final Block pass = getStatement(yield, await, false, true, false);

        Block fail = null;
        if (type == ELSE) {
            next();
            fail = getStatement(yield, await, false, true, false);
        }

        appendStatement(new IfNode(ifLine, ifToken, fail != null ? fail.getFinish() : pass.getFinish(), test, pass, fail));
    }

    /**
     * Parse a {@code for} IterationStatement.
     */
    private void forStatement(boolean yield, boolean await) {
        final long forToken = token;
        final int forLine = line;
        // start position of this for statement. This is used
        // for sort order for variables declared in the initializer
        // part of this 'for' statement (if any).
        final int forStart = Token.descPosition(forToken);
        // When ES6 for-let is enabled we create a container block to capture the LET.
        ParserContextBlockNode outer;
        if (useBlockScope()) {
            outer = newBlock();
            outer.setFlag(Block.IS_SYNTHETIC);
        } else {
            outer = null;
        }

        // Create FOR node, capturing FOR token.
        final ParserContextLoopNode forNode = new ParserContextLoopNode();
        lc.push(forNode);
        Block body = null;
        Expression init = null;
        JoinPredecessorExpression test = null;
        JoinPredecessorExpression modify = null;
        ForVariableDeclarationListResult varDeclList = null;

        int flags = 0;
        boolean isForOf = false;
        boolean isForAwaitOf = false;
        // used for lookahead != let checks in "for (await) of" grammar
        boolean initStartsWithLet = false;
        // used for "lookahead != async of" check in "for of" grammar
        boolean initStartsWithAsyncOf = false;

        try {
            // FOR tested in caller.
            next();

            // Nashorn extension: for each expression.
            // iterate property values rather than property names.
            if (env.syntaxExtensions && type == IDENT && lexer.checkIdentForKeyword(token, "each")) {
                flags |= ForNode.IS_FOR_EACH;
                next();
            } else if (ES8_FOR_AWAIT_OF && type == AWAIT) {
                if (!await) {
                    throw error(AbstractParser.message("invalid.for.await.of"), token);
                }
                isForAwaitOf = true;
                next();
            }

            expect(LPAREN);

            TokenType varType = null;
            switch (type) {
                case VAR:
                    // Var declaration captured in for outer block.
                    varType = type;
                    varDeclList = variableDeclarationList(varType, false, yield, await, forStart);
                    break;
                case SEMICOLON:
                    break;
                default:
                    if (useBlockScope() && (type == LET && lookaheadIsLetDeclaration() || type == CONST)) {
                        // LET/CONST declaration captured in container block created above.
                        varType = type;
                        varDeclList = variableDeclarationList(varType, false, yield, await, forStart);
                        if (varType == LET) {
                            // Per-iteration scope not needed if BindingPattern is empty
                            if (!forNode.getStatements().isEmpty()) {
                                flags |= ForNode.PER_ITERATION_SCOPE;
                            }
                        }
                        break;
                    }
                    if (env.constAsVar && type == CONST) {
                        // Var declaration captured in for outer block.
                        varType = TokenType.VAR;
                        varDeclList = variableDeclarationList(varType, false, yield, await, forStart);
                        break;
                    }

                    initStartsWithLet = (type == LET);
                    initStartsWithAsyncOf = (type == ASYNC && !isForAwaitOf && lookaheadIsOf());
                    init = expression(false, yield, await, true);
                    break;
            }

            switch (type) {
                case SEMICOLON:
                    // for (init; test; modify)
                    if (varDeclList != null) {
                        assert init == null;
                        init = varDeclList.init;
                        // late check for missing assignment, now we know it's a
                        // for (init; test; modify) loop
                        if (varDeclList.missingAssignment != null) {
                            if (varDeclList.missingAssignment instanceof IdentNode) {
                                throw error(AbstractParser.message("missing.const.assignment", ((IdentNode) varDeclList.missingAssignment).getName()));
                            } else {
                                throw error(AbstractParser.message("missing.destructuring.assignment"), varDeclList.missingAssignment.getToken());
                            }
                        }
                    } else {
                        if (hasCoverInitializedName(init)) {
                            throw error(AbstractParser.message(MESSAGE_INVALID_PROPERTY_INITIALIZER));
                        }
                    }

                    // for each (init; test; modify) is invalid
                    if ((flags & ForNode.IS_FOR_EACH) != 0) {
                        throw error(AbstractParser.message("for.each.without.in"), token);
                    }

                    expect(SEMICOLON);
                    if (type != SEMICOLON) {
                        test = joinPredecessorExpression(yield, await);
                    }
                    expect(SEMICOLON);
                    if (type != RPAREN) {
                        modify = joinPredecessorExpression(yield, await);
                    }
                    break;

                case OF:
                    if (ES8_FOR_AWAIT_OF && isForAwaitOf && !initStartsWithLet) {
                        // fall through
                    } else if (ES6_FOR_OF && !initStartsWithLet && !initStartsWithAsyncOf) {
                        isForOf = true;
                        // fall through
                    } else {
                        expect(SEMICOLON); // fail with expected message
                        break;
                    }
                case IN:
                    if (isForAwaitOf) {
                        expectDontAdvance(OF);
                        flags |= ForNode.IS_FOR_AWAIT_OF;
                    } else {
                        flags |= isForOf ? ForNode.IS_FOR_OF : ForNode.IS_FOR_IN;
                    }
                    test = new JoinPredecessorExpression();
                    if (varDeclList != null) {
                        // for (var|let|const ForBinding in|of expression)
                        if (varDeclList.secondBinding != null) {
                            // for (var i, j in obj) is invalid
                            throw error(AbstractParser.message("many.vars.in.for.in.loop", isForOf || isForAwaitOf ? "of" : "in"), varDeclList.secondBinding.getToken());
                        }
                        if (varDeclList.declarationWithInitializerToken != 0 && (isStrictMode || type != TokenType.IN || varType != VAR || varDeclList.init != null)) {
                            // ES5 legacy: for (var i = AssignmentExpressionNoIn in Expression)
                            // Invalid in ES6, but allow it in non-strict mode if no ES6 features
                            // used, i.e., error if strict, for-of, let/const, or destructuring
                            throw error(AbstractParser.message("for.in.loop.initializer", isForOf || isForAwaitOf ? "of" : "in"), varDeclList.declarationWithInitializerToken);
                        }
                        init = varDeclList.firstBinding;
                        assert init instanceof IdentNode || isDestructuringLhs(init);
                        if (varType == CONST || varType == LET) {
                            flags |= ForNode.PER_ITERATION_SCOPE;
                        }
                    } else {
                        // for (LeftHandSideExpression in|of expression)
                        assert init != null : "for..in/of init expression can not be null here";

                        // check if initial expression is a valid L-value
                        if (!checkValidLValue(init, isForOf || isForAwaitOf ? "for-of iterator" : "for-in iterator")) {
                            throw error(AbstractParser.message("not.lvalue.for.in.loop", isForOf || isForAwaitOf ? "of" : "in"), init.getToken());
                        }
                    }

                    next();

                    // For-of only allows AssignmentExpression.
                    modify = isForOf || isForAwaitOf ? new JoinPredecessorExpression(assignmentExpression(true, yield, await)) : joinPredecessorExpression(yield, await);
                    break;

                default:
                    expect(SEMICOLON);
                    break;
            }

            expect(RPAREN);

            // Set the for body.
            body = getStatement(yield, await);
        } finally {
            lc.pop(forNode);

            boolean skipVars = (flags & ForNode.PER_ITERATION_SCOPE) != 0 && (isForOf || isForAwaitOf || (flags & ForNode.IS_FOR_IN) != 0);
            if (!skipVars) {
                for (final Statement var : forNode.getStatements()) {
                    assert var instanceof VarNode;
                    appendStatement(var);
                }
            }
            if (body != null) {
                appendStatement(new ForNode(forLine, forToken, body.getFinish(), body, (forNode.getFlags() | flags), init, test, modify));
            }
            if (outer != null) {
                restoreBlock(outer);
                if (body != null) {
                    appendStatement(new BlockStatement(forLine, new Block(outer.getToken(), body.getFinish(), 0, outer.getScope(), outer.getStatements())));
                }
            }
        }
    }

    private boolean checkValidLValue(Expression init, String contextString) {
        if (init instanceof IdentNode) {
            IdentNode ident = (IdentNode) init;
            if (!checkIdentLValue(ident)) {
                return false;
            }
            if (ident.isMetaProperty()) {
                return false;
            }
            verifyStrictIdent(ident, contextString);
            return true;
        } else if (init instanceof AccessNode || init instanceof IndexNode) {
            if (((BaseNode) init).isOptional()) {
                return false;
            }
            return true;
        } else if (isDestructuringLhs(init)) {
            verifyDestructuringAssignmentPattern(init, contextString);
            return true;
        } else {
            return false;
        }
    }

    private boolean lookaheadIsLetDeclaration() {
        return lookaheadOfLetDeclaration() != null;
    }

    private TokenType lookaheadOfLetDeclaration() {
        assert type == LET;
        for (int i = 1;; i++) {
            TokenType t = T(k + i);
            switch (t) {
                case EOL:
                case COMMENT:
                    continue;
                case OF:
                case IDENT:
                case LBRACKET:
                case LBRACE:
                    return t;
                default:
                    // accept future strict tokens in non-strict mode (including LET)
                    if (t.isContextualKeyword() || (!isStrictMode && t.isFutureStrict())) {
                        return t;
                    }
                    return null;
            }
        }
    }

    private boolean lookaheadIsOf() {
        for (int i = 1;; i++) {
            TokenType t = T(k + i);
            switch (t) {
                case EOL:
                case COMMENT:
                    continue;
                case OF:
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Parse a {@code while} IterationStatement.
     */
    private void whileStatement(boolean yield, boolean await) {
        // Capture WHILE token.
        final long whileToken = token;
        final int whileLine = line;
        // WHILE tested in caller.
        next();

        final ParserContextLoopNode whileNode = new ParserContextLoopNode();
        lc.push(whileNode);

        JoinPredecessorExpression test = null;
        Block body = null;

        try {
            expect(LPAREN);
            test = joinPredecessorExpression(yield, await);
            expect(RPAREN);
            body = getStatement(yield, await);
        } finally {
            lc.pop(whileNode);
        }

        if (body != null) {
            appendStatement(new WhileNode(whileLine, whileToken, body.getFinish(), false, test, body));
        }
    }

    /**
     * Parse a {@code do while} IterationStatement.
     */
    private void doStatement(boolean yield, boolean await) {
        // Capture DO token.
        final long doToken = token;
        int doLine = 0;
        // DO tested in the caller.
        next();

        final ParserContextLoopNode doWhileNode = new ParserContextLoopNode();
        lc.push(doWhileNode);

        Block body = null;
        JoinPredecessorExpression test = null;

        try {
            // Get DO body.
            body = getStatement(yield, await);

            expect(WHILE);
            expect(LPAREN);
            doLine = line;
            test = joinPredecessorExpression(yield, await);
            expect(RPAREN);

            if (type == SEMICOLON) {
                endOfLine();
            }
        } finally {
            lc.pop(doWhileNode);
        }

        appendStatement(new WhileNode(doLine, doToken, finish, true, test, body));
    }

    /**
     * Parse continue statement.
     *
     * <pre>
     * ContinueStatement :
     *      continue ;
     *      continue [no LineTerminator here] LabelIdentifier ;
     * </pre>
     */
    private void continueStatement(boolean yield, boolean await) {
        // Capture CONTINUE token.
        final int continueLine = line;
        final long continueToken = token;
        // CONTINUE tested in caller.
        nextOrEOL();

        boolean seenEOL = (type == EOL);
        if (seenEOL) {
            next();
        }

        ParserContextLabelNode labelNode = null;

        // SEMICOLON or label.
        switch (type) {
            case RBRACE:
            case SEMICOLON:
            case EOF:
                break;

            default:
                if (seenEOL) {
                    break;
                }
                final IdentNode ident = labelIdentifier(yield, await);
                labelNode = lc.findLabel(ident.getName());

                if (labelNode == null) {
                    throw error(AbstractParser.message("undefined.label", ident.getName()), ident.getToken());
                }

                break;
        }

        final String labelName = labelNode == null ? null : labelNode.getLabelName();
        final ParserContextLoopNode targetNode = lc.getContinueTo(labelName);

        if (targetNode == null) {
            throw error(AbstractParser.message("illegal.continue.stmt"), continueToken);
        }

        endOfLine();

        // Construct and add CONTINUE node.
        appendStatement(new ContinueNode(continueLine, continueToken, finish, labelName));
    }

    /**
     * Parse break statement.
     *
     * <pre>
     * BreakStatement :
     *      break ;
     *      break [no LineTerminator here] LabelIdentifier ;
     * </pre>
     */
    private void breakStatement(boolean yield, boolean await) {
        // Capture BREAK token.
        final int breakLine = line;
        final long breakToken = token;
        // BREAK tested in caller.
        nextOrEOL();

        boolean seenEOL = (type == EOL);
        if (seenEOL) {
            next();
        }

        ParserContextLabelNode labelNode = null;

        // SEMICOLON or label.
        switch (type) {
            case RBRACE:
            case SEMICOLON:
            case EOF:
                break;

            default:
                if (seenEOL) {
                    break;
                }
                final IdentNode ident = labelIdentifier(yield, await);
                labelNode = lc.findLabel(ident.getName());

                if (labelNode == null) {
                    throw error(AbstractParser.message("undefined.label", ident.getName()), ident.getToken());
                }

                break;
        }

        // either an explicit label - then get its node or just a "break" - get first breakable
        // targetNode is what we are breaking out from.
        final String labelName = labelNode == null ? null : labelNode.getLabelName();
        final ParserContextBreakableNode targetNode = lc.getBreakable(labelName);
        if (targetNode == null) {
            throw error(AbstractParser.message("illegal.break.stmt"), breakToken);
        }

        endOfLine();

        // Construct and add BREAK node.
        appendStatement(new BreakNode(breakLine, breakToken, finish, labelName));
    }

    /**
     * Parse return statement.
     *
     * <pre>
     * ReturnStatement :
     *      return ;
     *      return [no LineTerminator here] Expression ;
     * </pre>
     */
    private void returnStatement(boolean yield, boolean await) {
        // check for return outside function
        ParserContextFunctionNode currentFunction = lc.getCurrentFunction();
        if (currentFunction.isScriptOrModule() || currentFunction.isClassStaticBlock()) {
            throw error(AbstractParser.message("invalid.return"));
        }

        // Capture RETURN token.
        final int returnLine = line;
        final long returnToken = token;
        // RETURN tested in caller.
        nextOrEOL();

        boolean seenEOL = (type == EOL);
        if (seenEOL) {
            next();
        }

        Expression expression = null;

        // SEMICOLON or expression.
        switch (type) {
            case RBRACE:
            case SEMICOLON:
            case EOF:
                break;

            default:
                if (seenEOL) {
                    break;
                }
                expression = expression(yield, await);
                break;
        }

        endOfLine();

        // Construct and add RETURN node.
        appendStatement(new ReturnNode(returnLine, returnToken, finish, expression));
    }

    /**
     * Parse YieldExpression.
     *
     * <pre>
     * YieldExpression[In, Await] :
     *   yield
     *   yield [no LineTerminator here] AssignmentExpression[?In, +Yield, ?Await]
     *   yield [no LineTerminator here] * AssignmentExpression[?In, +Yield, ?Await]
     * </pre>
     */
    private Expression yieldExpression(boolean in, boolean await) {
        assert isES6();
        // Capture YIELD token.
        long yieldToken = token;
        // YIELD tested in caller.
        assert type == YIELD;

        recordYieldOrAwait();

        nextOrEOL();

        Expression expression = null;

        boolean yieldAsterisk = false;
        if (type == MUL) {
            yieldAsterisk = true;
            yieldToken = Token.recast(yieldToken, YIELD_STAR);
            next();
        }

        switch (type) {
            case RBRACE:
            case SEMICOLON:
            case EOL:
            case EOF:
            case COMMARIGHT:
            case RPAREN:
            case RBRACKET:
            case COLON:
                if (!yieldAsterisk) {
                    // treat (yield) as (yield void 0)
                    expression = newUndefinedLiteral(yieldToken, finish);
                    if (type == EOL) {
                        next();
                    }
                    break;
                } else {
                    // AssignmentExpression required, fall through
                }

            default:
                expression = assignmentExpression(in, true, await);
                break;
        }

        // Construct and add YIELD node.
        return new UnaryNode(yieldToken, expression);
    }

    private Expression awaitExpression(boolean yield) {
        assert isAwait();
        // Capture await token.
        long awaitToken = token;

        ParserContextFunctionNode currentFunction = lc.getCurrentFunction();
        if (currentFunction.isClassStaticBlock()) {
            throw error(AbstractParser.message("unexpected.token", type.getNameOrType()));
        }

        recordYieldOrAwait();

        next();

        Expression expression = unaryExpression(yield, true);

        if (isModule && currentFunction.isModule()) {
            // Top-level await module: mark the body of the module as async.
            currentFunction.setFlag(FunctionNode.IS_ASYNC);
        }
        // Construct and add AWAIT node.
        return new UnaryNode(Token.recast(awaitToken, AWAIT), expression);
    }

    private static UnaryNode newUndefinedLiteral(long token, int finish) {
        return new UnaryNode(Token.recast(token, VOID), LiteralNode.newInstance(token, finish, 0));
    }

    private void recordYieldOrAwait() {
        long yieldOrAwaitToken = token;
        assert Token.descType(yieldOrAwaitToken) == YIELD || Token.descType(yieldOrAwaitToken) == AWAIT;
        recordYieldOrAwait(yieldOrAwaitToken, false);
    }

    private void recordYieldOrAwait(IdentNode ident) {
        recordYieldOrAwait(ident.getToken(), true);
    }

    /**
     * It is an early SyntaxError if arrow parameter list contains yield or await. We cannot detect
     * this immediately due to grammar ambiguity, so we only record the potential error to be thrown
     * later once the ambiguity is resolved.
     */
    private void recordYieldOrAwait(long yieldOrAwaitToken, boolean ident) {
        for (Iterator<ParserContextFunctionNode> iterator = lc.getFunctions(); iterator.hasNext();) {
            ParserContextFunctionNode fn = iterator.next();
            if (fn.isCoverArrowHead()) {
                if (ident && !fn.isAsync()) {
                    // await identifiers are only relevant for async arrow heads
                    continue;
                }
                // Only record the first yield or await.
                if (fn.getYieldOrAwaitInParameters() == 0L) {
                    fn.setYieldOrAwaitInParameters(yieldOrAwaitToken);
                }
            } else {
                break;
            }
        }
    }

    /**
     * Parse with statement.
     *
     * <pre>
     * WithStatement :
     *      with ( Expression ) Statement
     * </pre>
     */
    private void withStatement(boolean yield, boolean await) {
        // Capture WITH token.
        final int withLine = line;
        final long withToken = token;
        // WITH tested in caller.
        next();

        // ECMA 12.10.1 strict mode restrictions
        if (isStrictMode) {
            throw error(AbstractParser.message("strict.no.with"), withToken);
        }

        expect(LPAREN);
        final Expression expression = expression(yield, await);
        expect(RPAREN);
        final Block body = getStatement(yield, await);

        appendStatement(new WithNode(withLine, withToken, finish, expression, body));
    }

    /**
     * Parse switch statement.
     *
     * <pre>
     * SwitchStatement :
     *      switch ( Expression ) CaseBlock
     *
     * CaseBlock :
     *      { CaseClauses? }
     *      { CaseClauses? DefaultClause CaseClauses }
     *
     * CaseClauses :
     *      CaseClause
     *      CaseClauses CaseClause
     *
     * CaseClause :
     *      case Expression : StatementList?
     *
     * DefaultClause :
     *      default : StatementList?
     * </pre>
     */
    private void switchStatement(boolean yield, boolean await) {
        final int switchLine = line;
        final long switchToken = token;

        // Block around the switch statement with a variable capturing the switch expression value.
        final ParserContextBlockNode outerBlock;
        if (useBlockScope()) {
            outerBlock = newBlock();
            outerBlock.setFlag(Block.IS_SYNTHETIC);
        } else {
            outerBlock = null;
        }

        // Block to capture variables declared inside the switch statement.
        final ParserContextBlockNode switchBlock = newBlock(Scope.createSwitchBlock(lc.getCurrentScope()));
        switchBlock.setFlag(Block.IS_SYNTHETIC | Block.IS_SWITCH_BLOCK);

        // SWITCH tested in caller.
        next();

        // Create and add switch statement.
        final ParserContextSwitchNode switchNode = new ParserContextSwitchNode();
        lc.push(switchNode);

        int defaultCaseIndex = -1;
        // Prepare to accumulate cases.
        final ArrayList<CaseNode> cases = new ArrayList<>();

        SwitchNode switchStatement = null;

        try {
            expect(LPAREN);
            int expressionLine = line;
            Expression expression = expression(yield, await);
            expect(RPAREN);

            expect(LBRACE);

            // Desugar expression to an assignment to a synthetic let variable in the outer block.
            // This simplifies lexical scope analysis (the expression is outside the switch block).
            // e.g.: let x = 1; switch (x) { case 0: let x = 2; } =>
            // let x = 1; { let :switch = x; { let x; switch (:switch) { case 0: x = 2; } } }
            if (useBlockScope()) {
                IdentNode switchExprName = new IdentNode(Token.recast(expression.getToken(), IDENT), expression.getFinish(), SWITCH_BINDING_NAME);
                VarNode varNode = new VarNode(expressionLine, Token.recast(expression.getToken(), LET), expression.getFinish(), switchExprName, expression, VarNode.IS_LET);
                outerBlock.appendStatement(varNode);
                declareVar(outerBlock.getScope(), varNode);
                expression = switchExprName;
            }

            while (type != RBRACE) {
                // Prepare for next case.
                Expression caseExpression = null;
                final long caseToken = token;

                switch (type) {
                    case CASE:
                        next();
                        caseExpression = expression(yield, await);
                        break;

                    case DEFAULT:
                        if (defaultCaseIndex != -1) {
                            throw error(AbstractParser.message("duplicate.default.in.switch"));
                        }
                        next();
                        break;

                    default:
                        // Force an error.
                        expect(CASE);
                        break;
                }

                expect(COLON);

                // Get CASE body.
                List<Statement> statements = caseStatementList(yield, await);
                final CaseNode caseNode = new CaseNode(caseToken, finish, caseExpression, statements);

                if (caseExpression == null) {
                    assert defaultCaseIndex == -1;
                    defaultCaseIndex = cases.size();
                }

                cases.add(caseNode);
            }

            next();

            switchStatement = new SwitchNode(switchLine, switchToken, finish, expression, optimizeList(cases), defaultCaseIndex);
        } finally {
            lc.pop(switchNode);
            restoreBlock(switchBlock);

            if (switchStatement != null) {
                appendStatement(new BlockStatement(switchLine,
                                new Block(switchToken, switchStatement.getFinish(), switchBlock.getFlags(), switchBlock.getScope(), switchStatement)));
            }
            if (outerBlock != null) {
                restoreBlock(outerBlock);
                if (switchStatement != null) {
                    appendStatement(new BlockStatement(switchLine,
                                    new Block(switchToken, switchStatement.getFinish(), outerBlock.getFlags(), outerBlock.getScope(), outerBlock.getStatements())));
                }
            }
        }
    }

    /**
     * Parse label statement.
     *
     * <pre>
     * LabelledStatement :
     *      Identifier : Statement
     * </pre>
     */
    private void labelStatement(boolean yield, boolean await, boolean mayBeFunctionDeclaration) {
        // Capture label token.
        final long labelToken = token;
        // Get label ident.
        final IdentNode ident = labelIdentifier(yield, await);

        expect(COLON);

        if (lc.findLabel(ident.getName()) != null) {
            throw error(AbstractParser.message("duplicate.label", ident.getName()), labelToken);
        }

        final ParserContextLabelNode labelNode = new ParserContextLabelNode(ident.getName());
        Block body = null;
        try {
            lc.push(labelNode);
            body = getStatement(yield, await, true, mayBeFunctionDeclaration);
        } finally {
            lc.pop(labelNode);
        }

        appendStatement(new LabelNode(line, labelToken, finish, ident.getName(), body));
    }

    /**
     * Parse throw statement.
     *
     * <pre>
     * {@code
     * ThrowStatement:
     *      throw Expression; // [no LineTerminator here]
     * }
     * </pre>
     */
    private void throwStatement(boolean yield, boolean await) {
        // Capture THROW token.
        final int throwLine = line;
        final long throwToken = token;
        // THROW tested in caller.
        nextOrEOL();

        Expression expression = null;

        // SEMICOLON or expression.
        switch (type) {
            case RBRACE:
            case SEMICOLON:
            case EOL:
                break;

            default:
                expression = expression(yield, await);
                break;
        }

        if (expression == null) {
            throw error(AbstractParser.message(MESSAGE_EXPECTED_OPERAND, type.getNameOrType()));
        }

        endOfLine();

        appendStatement(new ThrowNode(throwLine, throwToken, finish, expression, false));
    }

    /**
     * Parse try statement.
     *
     * <pre>
     * TryStatement :
     *      try Block Catch
     *      try Block Finally
     *      try Block Catch Finally
     *
     * Catch :
     *      catch( Identifier if Expression ) Block
     *      catch( Identifier ) Block
     *
     * Finally :
     *      finally Block
     * </pre>
     */
    private void tryStatement(boolean yield, boolean await) {
        // Capture TRY token.
        final int tryLine = line;
        final long tryToken = token;
        // TRY tested in caller.
        next();

        // Container block needed to act as target for labeled break statements
        final int startLine = line;
        final ParserContextBlockNode outer = newBlock();
        // Create try.

        try {
            final Block tryBody = getBlock(yield, await, true);
            final ArrayList<Block> catchBlocks = new ArrayList<>();

            while (type == CATCH) {
                final int catchLine = line;
                final long catchToken = token;
                next();

                boolean optionalCatchBinding = type == LBRACE && ES2019_OPTIONAL_CATCH_BINDING;
                if (!optionalCatchBinding) {
                    expect(LPAREN);
                }

                final ParserContextBlockNode catchBlock = newBlock(Scope.createCatchParameter(lc.getCurrentScope()));
                final Expression ifExpression;
                try {
                    final IdentNode exception;
                    final Expression pattern;
                    if (optionalCatchBinding) {
                        exception = null;
                        pattern = null;
                        ifExpression = null;
                    } else {
                        if (isBindingIdentifier() || !(ES6_DESTRUCTURING && isES6())) {
                            pattern = null;
                            IdentNode catchParameter = bindingIdentifier(yield, await, CATCH_PARAMETER_CONTEXT);
                            exception = catchParameter.setIsCatchParameter();
                        } else {
                            pattern = bindingPattern(yield, await);
                            exception = new IdentNode(Token.recast(pattern.getToken(), IDENT), pattern.getFinish(), ERROR_BINDING_NAME).setIsCatchParameter();
                        }

                        // Nashorn extension: catch clause can have optional
                        // condition. So, a single try can have more than one
                        // catch clause each with it's own condition.
                        if (env.syntaxExtensions && type == IF) {
                            next();
                            // Get the exception condition.
                            ifExpression = expression(yield, await);
                        } else {
                            ifExpression = null;
                        }

                        expect(RPAREN);
                    }

                    final CatchNode catchNode = catchBody(yield, await, catchToken, catchLine, exception, pattern, ifExpression);
                    appendStatement(catchNode);
                } finally {
                    restoreBlock(catchBlock);
                }

                int catchFinish = Math.max(finish, Token.descPosition(catchBlock.getToken()));
                Block catchBlockNode = new Block(catchBlock.getToken(), catchFinish, catchBlock.getFlags() | Block.IS_SYNTHETIC, catchBlock.getScope(), catchBlock.getStatements());
                catchBlocks.add(catchBlockNode);

                // If unconditional catch then should to be the end.
                if (ifExpression == null) {
                    break;
                }
            }

            // Prepare to capture finally statement.
            Block finallyStatements = null;

            if (type == FINALLY) {
                next();
                finallyStatements = getBlock(yield, await, true);
            }

            // Need at least one catch or a finally.
            if (catchBlocks.isEmpty() && finallyStatements == null) {
                throw error(AbstractParser.message("missing.catch.or.finally"), tryToken);
            }

            final TryNode tryNode = new TryNode(tryLine, tryToken, finish, tryBody, optimizeList(catchBlocks), finallyStatements);
            // Add try.
            assert lc.peek() == outer;
            appendStatement(tryNode);
        } finally {
            restoreBlock(outer);
        }

        appendStatement(new BlockStatement(startLine, new Block(tryToken, finish, outer.getFlags() | Block.IS_SYNTHETIC, outer.getScope(), outer.getStatements())));
    }

    private CatchNode catchBody(boolean yield, boolean await, long catchToken, int catchLine, IdentNode exception, Expression pattern, Expression ifExpression) {
        if (exception != null) {
            final Scope catchScope = lc.getCurrentScope();
            assert catchScope.isCatchParameterScope();
            VarNode exceptionVar = new VarNode(catchLine, Token.recast(exception.getToken(), LET), exception.getFinish(), exception.setIsDeclaredHere(), null, VarNode.IS_LET);
            appendStatement(exceptionVar);
            declareVar(catchScope, exceptionVar);
            if (pattern != null) {
                verifyDestructuringBindingPattern(pattern, new Consumer<IdentNode>() {
                    @Override
                    public void accept(IdentNode identNode) {
                        verifyStrictIdent(identNode, CATCH_PARAMETER_CONTEXT);
                        final int varFlags = VarNode.IS_LET | VarNode.IS_DESTRUCTURING;
                        final VarNode var = new VarNode(catchLine, Token.recast(identNode.getToken(), LET), identNode.getFinish(), identNode.setIsDeclaredHere(), null, varFlags);
                        appendStatement(var);
                        declareVar(catchScope, var);
                    }
                });
            }
        }

        final Block catchBody = getBlock(yield, await, true);
        return new CatchNode(catchLine, catchToken, finish, exception, pattern, ifExpression, catchBody, false);
    }

    /**
     * Parse debugger statement.
     */
    private void debuggerStatement() {
        // Capture DEBUGGER token.
        final int debuggerLine = line;
        final long debuggerToken = token;
        // DEBUGGER tested in caller.
        next();
        endOfLine();
        appendStatement(new DebuggerNode(debuggerLine, debuggerToken, finish));
    }

    /**
     * Parse primary expression.
     *
     * <pre>
     * PrimaryExpression :
     *      this
     *      IdentifierReference
     *      Literal
     *      ArrayLiteral
     *      ObjectLiteral
     *      RegularExpressionLiteral
     *      TemplateLiteral
     *      CoverParenthesizedExpressionAndArrowParameterList
     *
     * CoverParenthesizedExpressionAndArrowParameterList :
     *      ( Expression )
     *      ( )
     *      ( ... BindingIdentifier )
     *      ( Expression , ... BindingIdentifier )
     * </pre>
     *
     * @return Expression node.
     */
    @SuppressWarnings("fallthrough")
    private Expression primaryExpression(boolean yield, boolean await) {
        // Capture first token.
        final int primaryLine = line;
        final long primaryToken = token;

        switch (type) {
            case THIS: {
                final String name = type.getName();
                next();
                markThis();
                return new IdentNode(primaryToken, finish, name).setIsThis();
            }
            case IDENT: {
                final IdentNode ident = identifierReference(yield, await);
                if (ident == null) {
                    break;
                }
                return detectSpecialProperty(ident);
            }
            case NON_OCTAL_DECIMAL:
                if (isStrictMode) {
                    throw error(AbstractParser.message("strict.no.nonoctaldecimal"), token);
                }
            case OCTAL_LEGACY:
                if (isStrictMode) {
                    throw error(AbstractParser.message("strict.no.octal"), token);
                }
            case STRING:
            case ESCSTRING:
            case DECIMAL:
            case HEXADECIMAL:
            case OCTAL:
            case BINARY_NUMBER:
            case BIGINT:
            case FLOATING:
            case REGEX:
            case XML:
                return getLiteral();
            case EXECSTRING:
                return execString(primaryLine, primaryToken);
            case FALSE:
                next();
                return LiteralNode.newInstance(primaryToken, finish, false);
            case TRUE:
                next();
                return LiteralNode.newInstance(primaryToken, finish, true);
            case NULL:
                next();
                return LiteralNode.newInstance(primaryToken, finish);
            case LBRACKET:
                return arrayLiteral(yield, await);
            case LBRACE:
                return objectLiteral(yield, await);
            case LPAREN:
                return parenthesizedExpressionAndArrowParameterList(yield, await);
            case TEMPLATE:
            case TEMPLATE_HEAD:
                return templateLiteral(yield, await);

            default:
                // In this context some operator tokens mark the start of a literal.
                if (lexer.scanLiteral(primaryToken, type, lineInfoReceiver)) {
                    next();
                    return getLiteral();
                }
                if (type.isContextualKeyword() || isNonStrictModeIdent()) {
                    return identifierReference(yield, await);
                }
                break;
        }

        throw error(AbstractParser.message(MESSAGE_EXPECTED_OPERAND, type.getNameOrType()));
    }

    private boolean isPrivateFieldsIn() {
        return env.privateFieldsIn;
    }

    /**
     * Convert execString to a call to $EXEC.
     *
     * @param primaryToken Original string token.
     * @return callNode to $EXEC.
     */
    private Expression execString(final int primaryLine, final long primaryToken) {
        // Synthesize an ident to call $EXEC.
        final IdentNode execIdent = new IdentNode(primaryToken, finish, EXEC_NAME);
        // Skip over EXECSTRING.
        next();
        // Set up argument list for call.
        // Skip beginning of edit string expression.
        expect(LBRACE);
        // Add the following expression to arguments.
        final List<Expression> arguments = Collections.singletonList(expression(false, false));
        // Skip ending of edit string expression.
        expect(RBRACE);

        long tokenWithDelimiter = Token.withDelimiter(primaryToken);
        return CallNode.forCall(primaryLine, tokenWithDelimiter, Token.descPosition(tokenWithDelimiter), finish, execIdent, arguments);
    }

    /**
     * Parse ArrayLiteral.
     *
     * <pre>
     * ArrayLiteral :
     *      [ Elision? ]
     *      [ ElementList ]
     *      [ ElementList , Elision? ]
     *      [ expression for (LeftHandExpression in expression) ( (if ( Expression ) )? ]
     *
     * ElementList : Elision? AssignmentExpression
     *      ElementList , Elision? AssignmentExpression
     *
     * Elision :
     *      ,
     *      Elision ,
     * </pre>
     *
     * @return Expression node.
     */
    private LiteralNode<Expression[]> arrayLiteral(boolean yield, boolean await) {
        // Capture LBRACKET token.
        final long arrayToken = token;
        // LBRACKET tested in caller.
        next();

        // Prepare to accumulate elements.
        final ArrayList<Expression> elements = new ArrayList<>();
        // Track elisions.
        boolean elision = true;
        boolean hasSpread = false;
        boolean hasCoverInitializedName = false;
        loop: while (true) {
            long spreadToken = 0;
            switch (type) {
                case RBRACKET:
                    next();

                    break loop;

                case COMMARIGHT:
                    next();

                    // If no prior expression
                    if (elision) {
                        elements.add(null);
                    }

                    elision = true;

                    break;

                case ELLIPSIS:
                    if (ES6_SPREAD_ARRAY) {
                        hasSpread = true;
                        spreadToken = token;
                        next();
                    }
                    // fall through

                default:
                    if (!elision) {
                        throw error(AbstractParser.message("expected.comma", type.getNameOrType()));
                    }

                    // Add expression element.
                    Expression expression = assignmentExpression(true, yield, await, true);
                    if (expression != null) {
                        if (spreadToken != 0) {
                            expression = new UnaryNode(Token.recast(spreadToken, SPREAD_ARRAY), expression);
                        }
                        elements.add(expression);
                        hasCoverInitializedName = hasCoverInitializedName || hasCoverInitializedName(expression);
                    } else {
                        expect(RBRACKET);
                    }

                    elision = false;
                    break;
            }
        }

        return LiteralNode.newInstance(arrayToken, finish, optimizeList(elements), hasSpread, elision, hasCoverInitializedName);
    }

    /**
     * Parse an object literal.
     *
     * <pre>
     * ObjectLiteral :
     *      { }
     *      { PropertyNameAndValueList } { PropertyNameAndValueList , }
     *
     * PropertyNameAndValueList :
     *      PropertyAssignment
     *      PropertyNameAndValueList , PropertyAssignment
     * </pre>
     *
     * @return Expression node.
     */
    private ObjectNode objectLiteral(boolean yield, boolean await) {
        // Capture LBRACE token.
        final long objectToken = token;
        // LBRACE tested in caller.
        next();

        // Object context.
        // Prepare to accumulate elements.
        final ArrayList<PropertyNode> elements = new ArrayList<>();
        final Map<String, PropertyNode> map = new HashMap<>();

        // Create a block for the object literal.
        boolean commaSeen = true;
        boolean hasCoverInitializedName = false;
        loop: while (true) {
            switch (type) {
                case RBRACE:
                    next();
                    break loop;

                case COMMARIGHT:
                    if (commaSeen) {
                        throw error(AbstractParser.message("expected.property.id", type.getNameOrType()));
                    }
                    next();
                    commaSeen = true;
                    break;

                default:
                    if (!commaSeen) {
                        throw error(AbstractParser.message("expected.comma", type.getNameOrType()));
                    }

                    commaSeen = false;
                    // Get and add the next property.
                    final PropertyNode property = propertyDefinition(yield, await);
                    elements.add(property);
                    hasCoverInitializedName = hasCoverInitializedName || property.isCoverInitializedName() || hasCoverInitializedName(property.getValue());

                    if (property.isComputed() || property.getKey().isTokenType(SPREAD_OBJECT)) {
                        break;
                    }

                    final String key = property.getKeyName();
                    final PropertyNode existingProperty = map.get(key);

                    if (existingProperty == null) {
                        map.put(key, property);
                        break;
                    }

                    // ECMA section 11.1.5 Object Initialiser
                    // point # 4 on property assignment production
                    final Expression value = property.getValue();
                    final FunctionNode getter = property.getGetter();
                    final FunctionNode setter = property.getSetter();

                    final Expression prevValue = existingProperty.getValue();
                    final FunctionNode prevGetter = existingProperty.getGetter();
                    final FunctionNode prevSetter = existingProperty.getSetter();

                    if (isES6()) {
                        if (property.isProto() && existingProperty.isProto()) {
                            throw error(AbstractParser.message("multiple.proto.key"), property.getToken());
                        }
                    } else {
                        checkPropertyRedefinition(property, value, getter, setter, prevValue, prevGetter, prevSetter);

                        if (value == null && prevValue == null) {
                            // Update the map with existing (merged accessor) properties
                            // for the purpose of checkPropertyRedefinition() above
                            if (getter != null) {
                                assert prevGetter != null || prevSetter != null;
                                map.put(key, existingProperty.setGetter(getter));
                            } else if (setter != null) {
                                assert prevGetter != null || prevSetter != null;
                                map.put(key, existingProperty.setSetter(setter));
                            }
                        }
                    }
                    break;
            }
        }

        return new ObjectNode(objectToken, finish, optimizeList(elements), hasCoverInitializedName);
    }

    private static boolean hasCoverInitializedName(Expression value) {
        return (value != null && ((value instanceof ObjectNode && ((ObjectNode) value).hasCoverInitializedName()) ||
                        (value instanceof ArrayLiteralNode && ((ArrayLiteralNode) value).hasCoverInitializedName())));
    }

    private void checkPropertyRedefinition(final PropertyNode property, final Expression value, final FunctionNode getter, final FunctionNode setter,
                    final Expression prevValue, final FunctionNode prevGetter, final FunctionNode prevSetter) {
        // ECMA 11.1.5 strict mode restrictions
        if (isStrictMode && value != null && prevValue != null) {
            throw error(AbstractParser.message(MESSAGE_PROPERTY_REDEFINITON, property.getKeyName()), property.getToken());
        }

        final boolean isPrevAccessor = prevGetter != null || prevSetter != null;
        final boolean isAccessor = getter != null || setter != null;

        // data property redefined as accessor property
        if (prevValue != null && isAccessor) {
            throw error(AbstractParser.message(MESSAGE_PROPERTY_REDEFINITON, property.getKeyName()), property.getToken());
        }

        // accessor property redefined as data
        if (isPrevAccessor && value != null) {
            throw error(AbstractParser.message(MESSAGE_PROPERTY_REDEFINITON, property.getKeyName()), property.getToken());
        }

        if (isAccessor && isPrevAccessor) {
            if (getter != null && prevGetter != null || setter != null && prevSetter != null) {
                throw error(AbstractParser.message(MESSAGE_PROPERTY_REDEFINITON, property.getKeyName()), property.getToken());
            }
        }
    }

    /**
     * <pre>
     * LiteralPropertyName :
     *      IdentifierName
     *      StringLiteral
     *      NumericLiteral
     * </pre>
     *
     * @return PropertyName node
     */
    @SuppressWarnings("fallthrough")
    private PropertyKey literalPropertyName() {
        switch (type) {
            case IDENT:
                return getIdent().setIsPropertyName();
            case NON_OCTAL_DECIMAL:
                if (isStrictMode) {
                    throw error(AbstractParser.message("strict.no.nonoctaldecimal"), token);
                }
            case OCTAL_LEGACY:
                if (isStrictMode) {
                    throw error(AbstractParser.message("strict.no.octal"), token);
                }
            case STRING:
            case ESCSTRING:
            case DECIMAL:
            case HEXADECIMAL:
            case OCTAL:
            case BINARY_NUMBER:
            case BIGINT:
            case FLOATING:
                return (PropertyKey) getLiteral();
            default:
                return getIdentifierName().setIsPropertyName();
        }
    }

    /**
     * <pre>
     * ComputedPropertyName :
     *      AssignmentExpression
     * </pre>
     *
     * @return PropertyName node
     */
    private Expression computedPropertyName(boolean yield, boolean await) {
        expect(LBRACKET);
        Expression expression = assignmentExpression(true, yield, await);
        expect(RBRACKET);
        return expression;
    }

    /**
     * <pre>
     * PropertyName :
     *      LiteralPropertyName
     *      ComputedPropertyName
     * </pre>
     *
     * @return PropertyName node
     */
    private Expression propertyName(boolean yield, boolean await) {
        if (ES6_COMPUTED_PROPERTY_NAME && type == LBRACKET && isES6()) {
            return computedPropertyName(yield, await);
        } else {
            return (Expression) literalPropertyName();
        }
    }

    /**
     * Parse an object literal property definition.
     *
     * <pre>
     * PropertyDefinition :
     *      IdentifierReference
     *      CoverInitializedName
     *      PropertyName : AssignmentExpression
     *      MethodDefinition
     *
     * CoverInitializedName :
     *      IdentifierReference = AssignmentExpression
     * </pre>
     *
     * @return Property or reference node.
     */
    private PropertyNode propertyDefinition(boolean yield, boolean await) {
        // Capture firstToken.
        final long propertyToken = token;
        final int functionLine = line;

        final Expression propertyName;
        final boolean isIdentifier;

        boolean async = false;
        if (isAsync() && lookaheadIsAsyncMethod(false)) {
            async = true;
            next();
        }
        boolean generator = false;
        if (type == MUL && ES6_GENERATOR_FUNCTION && isES6()) {
            generator = true;
            next();
        }

        final boolean computed = type == LBRACKET;
        if (type == IDENT || (isIdentifier() && !(type == GET || type == SET))) {
            isIdentifier = true;
            propertyName = getIdent().setIsPropertyName();
        } else if (type == GET || type == SET) {
            final TokenType getOrSet = type;
            next();

            if (type != COLON && type != COMMARIGHT && type != RBRACE && ((type != ASSIGN && type != LPAREN) || !isES6())) {
                final long getOrSetToken = propertyToken;
                if (getOrSet == GET) {
                    final PropertyFunction getter = propertyGetterFunction(getOrSetToken, functionLine, yield, await, false);
                    return new PropertyNode(propertyToken, finish, getter.key, null, getter.functionNode, null, false, getter.computed, false, false);
                } else if (getOrSet == SET) {
                    final PropertyFunction setter = propertySetterFunction(getOrSetToken, functionLine, yield, await, false);
                    return new PropertyNode(propertyToken, finish, setter.key, null, null, setter.functionNode, false, setter.computed, false, false);
                }
            }

            isIdentifier = true;
            propertyName = new IdentNode(propertyToken, finish, getOrSet.getName()).setIsPropertyName();
        } else if (type == ELLIPSIS && ES8_REST_SPREAD_PROPERTY && isES2017() && !(generator || async)) {
            long spreadToken = Token.recast(propertyToken, TokenType.SPREAD_OBJECT);
            next();
            Expression assignmentExpression = assignmentExpression(true, yield, await);
            Expression spread = new UnaryNode(spreadToken, assignmentExpression);
            return new PropertyNode(propertyToken, finish, spread, null, null, null, false, false, false, false);
        } else {
            isIdentifier = false;
            propertyName = propertyName(yield, await);
        }

        Expression propertyValue;

        if (generator || async) {
            expectDontAdvance(LPAREN);
        }

        boolean coverInitializedName = false;
        boolean proto = false;
        boolean isAnonymousFunctionDefinition = false;
        if (type == LPAREN && isES6()) {
            propertyValue = propertyMethodFunction(propertyName, propertyToken, functionLine, generator, FunctionNode.IS_METHOD, computed, async).functionNode;
        } else if (isIdentifier && (type == COMMARIGHT || type == RBRACE || type == ASSIGN) && isES6()) {
            IdentNode ident = (IdentNode) propertyName;
            verifyIdent(ident, yield, await);
            ident = createIdentNode(propertyToken, finish, ident.getPropertyName());
            // IdentifierReference or CoverInitializedName
            if (type == ASSIGN && ES6_DESTRUCTURING) {
                // If not destructuring, this is a SyntaxError
                long assignToken = token;
                coverInitializedName = true;
                next();
                Expression rhs = assignmentExpression(true, yield, await);
                propertyValue = verifyAssignment(assignToken, ident, rhs, true);
            } else {
                propertyValue = detectSpecialProperty(ident);
            }
            lc.getCurrentScope().addIdentifierReference(ident.getName());
        } else {
            expect(COLON);

            if (!computed && PROTO_NAME.equals(((PropertyKey) propertyName).getPropertyName())) {
                proto = true;
            }

            pushDefaultName(propertyName);
            try {
                propertyValue = assignmentExpression(true, yield, await, true);
            } finally {
                popDefaultName();
            }

            if (!proto) {
                if (isAnonymousFunctionDefinition(propertyValue)) {
                    if (!computed && propertyName instanceof PropertyKey) {
                        propertyValue = setAnonymousFunctionName(propertyValue, ((PropertyKey) propertyName).getPropertyName());
                    } else {
                        isAnonymousFunctionDefinition = true;
                    }
                }
            }
        }

        return new PropertyNode(propertyToken, finish, propertyName, propertyValue, null, null, false, computed, coverInitializedName, proto, false, isAnonymousFunctionDefinition);
    }

    private PropertyFunction propertyGetterFunction(long getSetToken, int functionLine, boolean yield, boolean await, boolean allowPrivate) {
        final boolean computed = type == LBRACKET;
        final Expression propertyName = classElementName(yield, await, allowPrivate);
        final IdentNode getterName = computed ? null : createMethodNameIdent(propertyName, "get ");
        expect(LPAREN);
        expect(RPAREN);

        int functionFlags = FunctionNode.IS_GETTER | FunctionNode.IS_METHOD |
                        (computed ? FunctionNode.IS_ANONYMOUS : 0);
        final ParserContextFunctionNode functionNode = createParserContextFunctionNode(getterName, getSetToken, functionFlags, functionLine, Collections.<IdentNode> emptyList(), 0);
        lc.push(functionNode);

        Block functionBody;

        try {
            functionBody = functionBody(functionNode);
        } finally {
            lc.pop(functionNode);
        }

        final FunctionNode function = createFunctionNode(
                        functionNode,
                        getSetToken,
                        getterName,
                        functionLine,
                        functionBody);

        return new PropertyFunction(propertyName, function, computed);
    }

    private PropertyFunction propertySetterFunction(long getSetToken, int functionLine, boolean yield, boolean await, boolean allowPrivate) {
        final boolean computed = type == LBRACKET;
        final Expression propertyName = classElementName(yield, await, allowPrivate);
        final IdentNode setterName = computed ? null : createMethodNameIdent(propertyName, "set ");

        expect(LPAREN);

        int functionFlags = FunctionNode.IS_SETTER | FunctionNode.IS_METHOD |
                        (computed ? FunctionNode.IS_ANONYMOUS : 0);
        final ParserContextFunctionNode functionNode = createParserContextFunctionNode(setterName, getSetToken, functionFlags, functionLine);
        lc.push(functionNode);

        Block functionBody;
        try {
            final ParserContextBlockNode parameterBlock = functionNode.createParameterBlock();
            lc.push(parameterBlock);
            try {
                if (!env.syntaxExtensions || type != RPAREN) {
                    formalParameter(false, false);
                } // else Nashorn allows no-argument setters
                expect(RPAREN);

                functionBody = functionBody(functionNode);
            } finally {
                restoreBlock(parameterBlock);
            }
            if (parameterBlock != null) {
                functionBody = wrapParameterBlock(parameterBlock, functionBody);
            }
        } finally {
            lc.pop(functionNode);
        }

        final FunctionNode function = createFunctionNode(
                        functionNode,
                        getSetToken,
                        setterName,
                        functionLine,
                        functionBody);

        return new PropertyFunction(propertyName, function, computed);
    }

    private PropertyFunction propertyMethodFunction(Expression key, final long methodToken, final int methodLine, final boolean generator, final int flags, boolean computed, boolean async) {
        final IdentNode methodNameNode = computed ? null : createMethodNameIdent(key, "");

        expect(LPAREN);

        int functionFlags = flags |
                        (computed ? FunctionNode.IS_ANONYMOUS : 0) |
                        (generator ? FunctionNode.IS_GENERATOR : 0) |
                        (async ? FunctionNode.IS_ASYNC : 0);
        final ParserContextFunctionNode functionNode = createParserContextFunctionNode(methodNameNode, methodToken, functionFlags, methodLine);
        lc.push(functionNode);

        try {
            ParserContextBlockNode parameterBlock = functionNode.createParameterBlock();
            lc.push(parameterBlock);
            Block functionBody;
            try {
                formalParameterList(generator, async);
                expect(RPAREN);

                functionBody = functionBody(functionNode);
            } finally {
                restoreBlock(parameterBlock);
            }

            verifyParameterList(functionNode);
            if (parameterBlock != null) {
                functionBody = wrapParameterBlock(parameterBlock, functionBody);
            }

            final FunctionNode function = createFunctionNode(
                            functionNode,
                            methodToken,
                            methodNameNode,
                            methodLine,
                            functionBody);
            return new PropertyFunction(key, function, computed);
        } finally {
            lc.pop(functionNode);
        }
    }

    private IdentNode createMethodNameIdent(Expression propertyKey, String prefix) {
        String methodName;
        boolean intern = false;
        if (propertyKey instanceof IdentNode) {
            methodName = ((IdentNode) propertyKey).getPropertyName();
        } else if (propertyKey instanceof PropertyKey) {
            methodName = ((PropertyKey) propertyKey).getPropertyName();
            intern = true;
        } else {
            return null;
        }
        if (!prefix.isEmpty()) {
            methodName = prefix.concat(methodName);
            intern = true;
        }
        if (intern) {
            methodName = lexer.stringIntern(methodName);
        }
        return createIdentNode(propertyKey.getToken(), propertyKey.getFinish(), methodName);
    }

    private static boolean isAnonymousFunctionDefinition(Expression expression) {
        if (expression instanceof FunctionNode && ((FunctionNode) expression).isAnonymous()) {
            return true;
        } else if (expression instanceof ClassNode && ((ClassNode) expression).isAnonymous()) {
            return true;
        } else {
            return false;
        }
    }

    private Expression setAnonymousFunctionName(Expression expression, String functionName) {
        if (!isES6()) {
            return expression;
        }
        if (expression instanceof FunctionNode && ((FunctionNode) expression).isAnonymous()) {
            return ((FunctionNode) expression).setName(null, functionName);
        } else if (expression instanceof ClassNode && ((ClassNode) expression).isAnonymous()) {
            ClassNode classNode = (ClassNode) expression;
            FunctionNode constructorFunction = (FunctionNode) classNode.getConstructor().getValue();
            return classNode.setConstructor(classNode.getConstructor().setValue(constructorFunction.setName(null, functionName)));
        }
        return expression;
    }

    private static final class PropertyFunction {
        final Expression key;
        final FunctionNode functionNode;
        final boolean computed;

        PropertyFunction(final Expression key, final FunctionNode function, final boolean computed) {
            this.key = key;
            this.functionNode = function;
            this.computed = computed;
        }
    }

    /**
     * Parse left hand side expression.
     *
     * <pre>
     * LeftHandSideExpression :
     *      NewExpression
     *      CallExpression
     *
     * CallExpression :
     *      MemberExpression Arguments (CoverCallExpressionAndAsyncArrowHead)
     *      SuperCall
     *      CallExpression Arguments
     *      CallExpression [ Expression ]
     *      CallExpression . IdentifierName
     *      CallExpression TemplateLiteral
     *      CallExpression . PrivateIdentifier
     *
     * SuperCall :
     *      super Arguments
     * </pre>
     *
     * @return Expression node.
     */
    private Expression leftHandSideExpression(boolean yield, boolean await) {
        int callLine = line;
        long callToken = token;

        Expression lhs = memberExpression(yield, await);

        if (type == LPAREN) {
            boolean async = ES8_ASYNC_FUNCTION && isES2017() && lhs.isTokenType(ASYNC) && lookbehindNoLineTerminatorAfterAsync();
            final List<Expression> arguments = argumentList(yield, await, async, callToken, callLine);

            if (async) {
                if (type == ARROW && lookbehindNoLineTerminatorBeforeArrow()) {
                    // async () => ...
                    // async ( ArgumentsList ) => ...
                    return new ExpressionList(callToken, callLine, arguments);
                }
            }

            // Catch special functions.
            boolean eval = false;
            boolean applyArguments = false;
            if (lhs instanceof IdentNode) {
                final IdentNode ident = (IdentNode) lhs;
                final String name = ident.getName();
                if (EVAL_NAME.equals(name)) {
                    markEval();
                    eval = true;
                } else if (SUPER.getName().equals(name)) {
                    assert ident.isDirectSuper();
                    markSuperCall();
                }
            } else if (lhs instanceof AccessNode && !((AccessNode) lhs).isPrivate() && arguments.size() == 2 && arguments.get(1) instanceof IdentNode &&
                            ((IdentNode) arguments.get(1)).isArguments() && APPLY_NAME.equals(((AccessNode) lhs).getProperty())) {
                if (markApplyArgumentsCall(lc, arguments)) {
                    applyArguments = true;
                }
            }

            lhs = CallNode.forCall(callLine, callToken, lhs.getStart(), finish, lhs, optimizeList(arguments), false, false, eval, applyArguments, false);
        }

        boolean optionalChain = false;
        loop: while (true) {
            // Capture token.
            callLine = line;
            callToken = token;

            switch (type) {
                case LPAREN: {
                    final List<Expression> arguments = optimizeList(argumentList(yield, await));

                    lhs = CallNode.forCall(callLine, callToken, lhs.getStart(), finish, lhs, arguments, false, optionalChain);

                    break;
                }
                case LBRACKET: {
                    next();

                    // Get array index.
                    final Expression rhs = expression(true, yield, await);

                    expect(RBRACKET);

                    // Create indexing node.
                    lhs = new IndexNode(callToken, finish, lhs, rhs, false, false, optionalChain);

                    break;
                }
                case PERIOD: {
                    next();

                    final boolean isPrivate = type == TokenType.PRIVATE_IDENT;
                    final IdentNode property;
                    if (isPrivate) {
                        property = privateIdentifierUse();
                    } else {
                        property = getIdentifierName();
                    }

                    // Create property access node.
                    lhs = new AccessNode(callToken, finish, lhs, property.getName(), false, isPrivate, false, optionalChain);

                    break;
                }
                case TEMPLATE:
                case TEMPLATE_HEAD: {
                    // tagged template literal
                    if (optionalChain) {
                        // TemplateLiteral not allowed in OptionalChain
                        throw error(AbstractParser.message("optional.chain.template"));
                    }

                    final List<Expression> arguments = templateLiteralArgumentList(yield, await);

                    lhs = CallNode.forTaggedTemplateLiteral(callLine, callToken, lhs.getStart(), finish, lhs, arguments);

                    break;
                }
                case OPTIONAL_CHAIN: {
                    next();
                    optionalChain = true;

                    switch (type) {
                        case LPAREN: {
                            final List<Expression> arguments = optimizeList(argumentList(yield, await));

                            lhs = CallNode.forCall(callLine, callToken, lhs.getStart(), finish, lhs, arguments, true, optionalChain);
                            break;
                        }
                        case LBRACKET: {
                            next();

                            final Expression rhs = expression(true, yield, await);

                            expect(RBRACKET);

                            lhs = new IndexNode(callToken, finish, lhs, rhs, false, true, optionalChain);
                            break;
                        }
                        default: {
                            final boolean isPrivate = type == TokenType.PRIVATE_IDENT;
                            final IdentNode property;
                            if (isPrivate) {
                                property = privateIdentifierUse();
                            } else {
                                property = getIdentifierName();
                            }

                            lhs = new AccessNode(callToken, finish, lhs, property.getName(), false, isPrivate, true, optionalChain);
                            break;
                        }
                    }
                    break;
                }
                default:
                    break loop;
            }
        }

        return lhs;
    }

    /**
     * Parse new expression.
     *
     * <pre>
     * NewExpression :
     *      MemberExpression
     *      new NewExpression
     * </pre>
     *
     * @return Expression node.
     */
    private Expression newExpression(boolean yield, boolean await) {
        final long newToken = token;
        // NEW is tested in caller.
        assert type == TokenType.NEW;
        next();

        if (ES6_NEW_TARGET && type == PERIOD && isES6()) {
            next();
            if (type == IDENT && "target".equals(getValueNoEscape())) {
                next();
                markNewTarget();
                return new IdentNode(newToken, finish, NEW_TARGET_NAME).setIsNewTarget();
            } else {
                throw error(AbstractParser.message("expected.target"), token);
            }
        } else if (type == IMPORT && isES2020() && lookahead() == LPAREN) {
            // new cannot be used with import()
            throw error(AbstractParser.message(MESSAGE_EXPECTED_OPERAND, IMPORT.getName()), token);
        }

        // Get function base.
        final int callLine = line;
        final Expression constructor = memberExpression(yield, await);

        // Get arguments.
        ArrayList<Expression> arguments;

        // Allow for missing arguments.
        if (type == LPAREN) {
            arguments = argumentList(yield, await);
        } else {
            arguments = new ArrayList<>();

            if (type == TokenType.OPTIONAL_CHAIN) {
                // OptionalChain is not allowed directly after a NewExpression (without parentheses)
                throw error(AbstractParser.message("unexpected.token", type.getNameOrType()));
            }
        }

        // Nashorn extension: This is to support the following interface implementation
        // syntax:
        //
        // var r = new java.lang.Runnable() {
        // run: function() { println("run"); }
        // };
        //
        // The object literal following the "new Constructor()" expression
        // is passed as an additional (last) argument to the constructor.
        if (env.syntaxExtensions && type == LBRACE) {
            arguments.add(objectLiteral(yield, await));
        }

        final Expression callNode = CallNode.forNew(callLine, newToken, Token.descPosition(newToken), finish, constructor, optimizeList(arguments));

        return new UnaryNode(newToken, callNode);
    }

    /**
     * Parse member expression.
     *
     * <pre>
     * MemberExpression :
     *      PrimaryExpression
     *        FunctionExpression
     *        ClassExpression
     *        GeneratorExpression
     *      MemberExpression [ Expression ]
     *      MemberExpression . IdentifierName
     *      MemberExpression TemplateLiteral
     *      SuperProperty
     *      MetaProperty
     *      new MemberExpression Arguments
     *      MemberExpression . PrivateIdentifier
     *
     * SuperProperty :
     *      super [ Expression ]
     *      super . IdentifierName
     *
     * MetaProperty :
     *      NewTarget
     * </pre>
     *
     * @return Expression node.
     */
    private Expression memberExpression(boolean yield, boolean await) {
        // Prepare to build operation.
        Expression lhs;
        boolean isSuper = false;

        switch (type) {
            case NEW:
                // Get new expression.
                lhs = newExpression(yield, await);
                break;

            case FUNCTION:
                // Get function expression.
                lhs = functionExpression();
                break;

            case CLASS:
                if (ES6_CLASS && isES6()) {
                    lhs = classExpression(yield, await);
                    break;
                }
                // fall through

            case SUPER:
                if (ES6_CLASS && isES6()) {
                    Scope scope = lc.getCurrentScope();
                    if (scope.inMethod()) {
                        long identToken = Token.recast(token, IDENT);
                        next();
                        lhs = new IdentNode(identToken, finish, SUPER.getName()).setIsSuper();

                        switch (type) {
                            case LBRACKET:
                            case PERIOD:
                                ParserContextFunctionNode currentFunction = lc.getCurrentNonArrowFunction();
                                if (currentFunction.isMethod()) {
                                    currentFunction.setFlag(FunctionNode.USES_SUPER);
                                    addIdentifierReference(SUPER.getName());
                                    addIdentifierReference(THIS.getName());
                                }
                                isSuper = true;
                                break;
                            case LPAREN:
                                if (scope.inDerivedConstructor()) {
                                    lhs = ((IdentNode) lhs).setIsDirectSuper();
                                    break;
                                } else {
                                    // fall through to throw error
                                }
                            default:
                                throw error(AbstractParser.message("invalid.super"), identToken);
                        }
                        break;
                    }
                }
                // fall through

            case ASYNC:
                if (isAsync() && lookaheadIsAsyncFunction()) {
                    lhs = asyncFunctionExpression();
                    break;
                }
                // fall through

            case IMPORT:
                if (isES2020() && type == IMPORT) {
                    lhs = importExpression(yield, await);
                    break;
                }
                // fall through

            default:
                // Get primary expression.
                lhs = primaryExpression(yield, await);
                break;
        }

        loop: while (true) {
            // Capture token.
            final long callToken = token;

            switch (type) {
                case LBRACKET: {
                    next();

                    // Get array index.
                    final Expression index = expression(true, yield, await);

                    expect(RBRACKET);

                    // Create indexing node.
                    lhs = new IndexNode(callToken, finish, lhs, index, isSuper, false, false);

                    if (isSuper) {
                        isSuper = false;
                    }

                    break;
                }
                case PERIOD: {
                    next();

                    final boolean isPrivate = type == TokenType.PRIVATE_IDENT;
                    final IdentNode property;
                    if (!isSuper && isPrivate) {
                        property = privateIdentifierUse();
                    } else {
                        property = getIdentifierName();
                    }

                    // Create property access node.
                    lhs = new AccessNode(callToken, finish, lhs, property.getName(), isSuper, isPrivate, false, false);

                    if (isSuper) {
                        isSuper = false;
                    }

                    break;
                }
                case TEMPLATE:
                case TEMPLATE_HEAD: {
                    // tagged template literal

                    final int callLine = line;
                    final List<Expression> arguments = templateLiteralArgumentList(yield, await);

                    lhs = CallNode.forCall(callLine, callToken, lhs.getStart(), finish, lhs, arguments, false, false);

                    break;
                }
                default:
                    break loop;
            }
        }

        return lhs;
    }

    /**
     * Parse import expression
     *
     * <pre>
     * ImportCall:
     *     import ( AssignmentExpression ,opt )
     *     import ( AssignmentExpression, AssignmentExpression ,opt )
     * </pre>
     */
    private Expression importExpression(boolean yield, boolean await) {
        final long importToken = token;
        final int importLine = line;
        final int importStart = start;
        assert type == IMPORT;
        next();
        if (type == PERIOD) {
            next();
            expectDontAdvance(IDENT);
            String meta = (String) getValueNoEscape();
            if ("meta".equals(meta)) {
                if (!isModule) {
                    throw error(AbstractParser.message("unexpected.import.meta"), importToken);
                }
                next();
                return new IdentNode(importToken, finish, IMPORT_META_NAME).setIsImportMeta();
            } else {
                throw error(AbstractParser.message("unexpected.ident", meta), token);
            }
        } else if (type == LPAREN) {
            next();
            ArrayList<Expression> arguments = new ArrayList<>();
            arguments.add(assignmentExpression(true, yield, await));
            if (env.importAssertions && type == COMMARIGHT) {
                next();
                if (type != RPAREN) {
                    arguments.add(assignmentExpression(true, yield, await));
                    if (type == COMMARIGHT) {
                        next();
                    }
                }
            }
            expect(RPAREN);

            IdentNode importIdent = new IdentNode(importToken, Token.descPosition(importToken) + Token.descLength(importToken), IMPORT.getName());
            return CallNode.forImport(importLine, importToken, importStart, finish, importIdent, optimizeList(arguments));
        } else {
            throw error(AbstractParser.message(MESSAGE_EXPECTED_OPERAND, IMPORT.getName()), importToken);
        }
    }

    private ArrayList<Expression> argumentList(boolean yield, boolean await) {
        return argumentList(yield, await, false, 0L, 0);
    }

    /**
     * Parse function call arguments. Also used to parse CoverCallExpressionAndAsyncArrowHead.
     *
     * <pre>
     * {@code
     * Arguments :
     *      ( )
     *      ( ArgumentList )
     *
     * ArgumentList :
     *      AssignmentExpression
     *      ... AssignmentExpression
     *      ArgumentList , AssignmentExpression
     *      ArgumentList , ... AssignmentExpression
     * }
     * </pre>
     *
     * @return Argument list.
     */
    private ArrayList<Expression> argumentList(boolean yield, boolean await, boolean coverAsyncArrow, long startToken, int startLine) {
        // LPAREN tested in caller.
        assert type == LPAREN;
        next();

        final boolean inPatternPosition = coverAsyncArrow;
        // Prepare to accumulate list of arguments.
        final ArrayList<Expression> nodeList = new ArrayList<>();
        // Track commas.
        boolean first = true;
        boolean hasCoverInitializedName = false;

        ParserContextFunctionNode cover = null;
        ParserContextBlockNode parameterBlock = null;
        if (coverAsyncArrow) {
            cover = createParserContextArrowFunctionNode(startToken, startLine, true, true);
            lc.push(cover);
            parameterBlock = cover.createParameterBlock();
            lc.push(parameterBlock);
        }

        try {
            while (type != RPAREN) {
                // Comma prior to every argument except the first.
                if (!first) {
                    expect(COMMARIGHT);
                    // Trailing comma.
                    if (ES8_TRAILING_COMMA && isES2017() && type == RPAREN) {
                        break;
                    }
                } else {
                    first = false;
                }

                long spreadToken = 0;
                if (ES6_SPREAD_ARGUMENT && type == ELLIPSIS && isES6()) {
                    spreadToken = token;
                    next();
                }

                // Get argument expression.
                Expression expression = assignmentExpression(true, yield, await, inPatternPosition);

                hasCoverInitializedName = inPatternPosition && (hasCoverInitializedName || hasCoverInitializedName(expression));
                assert inPatternPosition || !hasCoverInitializedName(expression);

                if (spreadToken != 0) {
                    expression = new UnaryNode(Token.recast(spreadToken, TokenType.SPREAD_ARGUMENT), expression);
                }

                nodeList.add(expression);
            }
        } finally {
            if (coverAsyncArrow) {
                lc.pop(parameterBlock);
                lc.pop(cover);
            }
        }

        expect(RPAREN);

        if (coverAsyncArrow) {
            if (type == ARROW && lookbehindNoLineTerminatorBeforeArrow()) {
                commitArrowHead(cover);
            } else {
                // invocation of a function named 'async'
                if (hasCoverInitializedName) {
                    // would be thrown by assignmentExpression() if we knew that
                    // we are parsing arguments (and not arrow parameter list)
                    throw error(AbstractParser.message(MESSAGE_INVALID_PROPERTY_INITIALIZER));
                }
                revertArrowHead(cover);
            }
        }

        return nodeList;
    }

    private static <T> List<T> optimizeList(final ArrayList<T> list) {
        switch (list.size()) {
            case 0: {
                return Collections.emptyList();
            }
            case 1: {
                return Collections.singletonList(list.get(0));
            }
            default: {
                list.trimToSize();
                return list;
            }
        }
    }

    private static <T> List<T> optimizeList(final List<T> list) {
        switch (list.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(list.get(0));
            default:
                ((ArrayList<T>) list).trimToSize();
                return list;
        }
    }

    private long expectAsyncFunction() {
        assert isAsync() && lookaheadIsAsyncFunction();
        long asyncToken = token;
        nextOrEOL();
        return Token.recast(asyncToken, FUNCTION);
    }

    /**
     * Parse async (generator) function declaration.
     */
    private Expression asyncFunctionDeclaration(final boolean isStatement, final boolean topLevel, boolean yield, boolean await, boolean isDefault) {
        long functionToken = expectAsyncFunction();
        return functionDeclarationOrExpression(functionToken, isStatement, topLevel, true, false, true, yield, await, isDefault);
    }

    /**
     * Parse async (generator) function expression.
     */
    private Expression asyncFunctionExpression() {
        long functionToken = expectAsyncFunction();
        return functionDeclarationOrExpression(functionToken, false, false, true, false, false, false, true, true);
    }

    /**
     * Parse (generator) function declaration.
     */
    private Expression functionDeclaration(final boolean isStatement, final boolean topLevel, final boolean expressionStatement, boolean yield, boolean await, boolean isDefault) {
        return functionDeclarationOrExpression(token, isStatement, topLevel, false, expressionStatement, true, yield, await, isDefault);
    }

    /**
     * Parse (generator) function expression.
     */
    private Expression functionExpression() {
        return functionDeclarationOrExpression(token, false, false, false, false, false, false, false, true);
    }

    /**
     * Parse (async) (generator) function declaration or expression.
     *
     * <pre>
     * FunctionDeclaration[Yield, Await] :
     *      function Identifier ( FormalParameterList? ) { FunctionBody }
     *
     * FunctionExpression :
     *      function Identifier? ( FormalParameterList? ) { FunctionBody }
     * </pre>
     *
     * @param isStatement true if parsing in a statement context.
     * @param isDeclaration True if parsing a declaration, false if parsing an expression.
     * @param isYield Yield if parsing a declaration, otherwise ignored.
     * @param isAwait Await if parsing a declaration, otherwise ignored.
     * @param isDefault Default if parsing a declaration, otherwise ignored.
     * @return the function.
     */
    private Expression functionDeclarationOrExpression(long functionToken, boolean isStatement, boolean topLevel, boolean async,
                    boolean expressionStatement, boolean isDeclaration, boolean isYield, boolean isAwait, boolean isDefault) {
        final int functionLine = line;
        // FUNCTION is tested in caller.
        assert type == FUNCTION;
        next();

        boolean generator = false;
        if (type == MUL && ES6_GENERATOR_FUNCTION && isES6()) {
            if (expressionStatement) {
                throw error(AbstractParser.message(MESSAGE_EXPECTED_STMT, "generator function declaration"), token);
            }
            generator = true;
            next();
        }

        assert !(isDeclaration && !isDefault) || isStatement; // sanity check

        IdentNode name = null;
        boolean declared = isDeclaration;

        if (isBindingIdentifier()) {
            boolean yield = (!isDeclaration && generator) || (isDeclaration && isYield);
            boolean await = (!isDeclaration && async) || (isDeclaration && isAwait);
            name = bindingIdentifier(yield, await, "function name");
        } else if (isDeclaration && !isDefault) {
            // Nashorn extension: anonymous function statements.
            // Do not allow anonymous function statement if extensions
            // are not allowed. But if we are reparsing then anon function
            // statement is possible - because it was used as function
            // expression in surrounding code.
            if (env.syntaxExtensions) {
                // statement is treated like an anonymous function expression, not a declaration.
                declared = false;
            } else if (reparsedFunction == null) {
                expect(IDENT);
            }
        }

        expect(LPAREN);

        boolean isAnonymous = name == null;
        // Function declarations must be named, with the exception of default exports.
        assert !declared || (!isAnonymous || isDefault);

        int functionFlags = (generator ? FunctionNode.IS_GENERATOR : 0) |
                        (async ? FunctionNode.IS_ASYNC : 0) |
                        (isAnonymous ? FunctionNode.IS_ANONYMOUS : 0) |
                        (declared ? FunctionNode.IS_DECLARED : 0) |
                        ((isStatement && !isAnonymous) ? FunctionNode.IS_STATEMENT : 0);
        final ParserContextFunctionNode functionNode = createParserContextFunctionNode(name, functionToken, functionFlags, functionLine);
        if (isAnonymous) {
            // name is null, generate anonymous name
            functionNode.setInternalName(getDefaultFunctionName());
        }

        lc.push(functionNode);

        Block functionBody;
        // Hide the current default name across function boundaries.
        // E.g. "x3 = function x1() { function() {}}"
        // If we didn't hide the current default name, then the innermost anonymous function would
        // receive "x3".
        hideDefaultName();

        try {
            final ParserContextBlockNode parameterBlock = functionNode.createParameterBlock();
            lc.push(parameterBlock);
            try {
                formalParameterList(generator, async);
                expect(RPAREN);

                functionBody = functionBody(functionNode);
            } finally {
                restoreBlock(parameterBlock);
            }
            if (parameterBlock != null) {
                functionBody = wrapParameterBlock(parameterBlock, functionBody);
            }
        } finally {
            popDefaultName();
            lc.pop(functionNode);
        }

        if ((isStatement && !isAnonymous) && !(topLevel || useBlockScope()) &&
                        (isStrictMode || env.functionStatement != ScriptEnvironment.FunctionStatementBehavior.ACCEPT)) {
            // For compatibility with Nashorn, we report the error after parsing the body.
            reportIllegalES5BlockLevelFunctionDeclaration(functionToken);
        }

        verifyParameterList(functionNode);

        final FunctionNode function = createFunctionNode(
                        functionNode,
                        functionToken,
                        name,
                        functionLine,
                        functionBody);

        if (isStatement) {
            if (isAnonymous) {
                appendStatement(new ExpressionStatement(functionLine, functionToken, finish, function));
                return function;
            }

            // mark ES6 block functions as lexically scoped
            Scope scope = lc.getCurrentScope();
            final int varFlags = ((topLevel && !scope.isModuleScope()) || !useBlockScope()) ? 0 : VarNode.IS_LET;
            final VarNode varNode = new VarNode(functionLine, functionToken, finish, name, function, varFlags);
            declareVar(scope, varNode);
            if (topLevel) {
                functionDeclarations.add(varNode);
            } else if (useBlockScope()) {
                prependStatement(varNode); // Hoist to beginning of current block
            } else {
                appendStatement(varNode);
            }
        }

        return function;
    }

    private static Block wrapParameterBlock(ParserContextBlockNode parameterBlock, Block functionBody) {
        assert parameterBlock.getFlag(Block.IS_PARAMETER_BLOCK) != 0 && functionBody.isFunctionBody();
        if (parameterBlock.getStatements().isEmpty()) {
            return functionBody;
        } else {
            parameterBlock.getStatements().add(new BlockStatement(functionBody.getFirstStatementLineNumber(), functionBody));
            return new Block(parameterBlock.getToken(), functionBody.getFinish(), parameterBlock.getFlags(), parameterBlock.getScope(), parameterBlock.getStatements());
        }
    }

    private void verifyParameterList(final ParserContextFunctionNode functionNode) {
        IdentNode duplicateParameter = functionNode.getDuplicateParameterBinding();
        if (duplicateParameter != null) {
            if (functionNode.isStrict() || functionNode.isMethod() || functionNode.isArrow() || !functionNode.isSimpleParameterList()) {
                throw error(AbstractParser.message("strict.param.redefinition", duplicateParameter.getName()), duplicateParameter.getToken());
            }

            final List<IdentNode> parameters = functionNode.getParameters();
            final int arity = parameters.size();
            final HashSet<String> parametersSet = new HashSet<>(arity);

            for (int i = arity - 1; i >= 0; i--) {
                final IdentNode parameter = parameters.get(i);
                String parameterName = parameter.getName();

                if (parametersSet.contains(parameterName)) {
                    // redefinition of parameter name, rename in non-strict mode
                    parameterName = functionNode.uniqueName(parameterName);
                    final long parameterToken = parameter.getToken();
                    parameters.set(i, new IdentNode(parameterToken, Token.descPosition(parameterToken), functionNode.uniqueName(parameterName)));
                }
                parametersSet.add(parameterName);
            }
        }
    }

    private void reportIllegalES5BlockLevelFunctionDeclaration(long functionToken) {
        assert !isES6();
        if (isStrictMode) {
            throw error(JSErrorType.SyntaxError, AbstractParser.message("strict.no.func.decl.here"), functionToken);
        } else if (env.functionStatement == ScriptEnvironment.FunctionStatementBehavior.ERROR) {
            throw error(JSErrorType.SyntaxError, AbstractParser.message("no.func.decl.here"), functionToken);
        } else if (env.functionStatement == ScriptEnvironment.FunctionStatementBehavior.WARNING) {
            warning(JSErrorType.SyntaxError, AbstractParser.message("no.func.decl.here.warn"), functionToken);
        }
    }

    private void pushDefaultName(final Expression nameExpr) {
        defaultNames.add(nameExpr);
    }

    private Object popDefaultName() {
        return defaultNames.remove(defaultNames.size() - 1);
    }

    private String getDefaultFunctionName() {
        if (!defaultNames.isEmpty()) {
            final Object nameExpr = defaultNames.get(defaultNames.size() - 1);
            if (nameExpr instanceof PropertyKey) {
                markDefaultNameUsed();
                return ((PropertyKey) nameExpr).getPropertyName();
            } else if (nameExpr instanceof AccessNode) {
                AccessNode accessNode = (AccessNode) nameExpr;
                markDefaultNameUsed();
                if (accessNode.getBase() instanceof AccessNode) {
                    AccessNode base = (AccessNode) accessNode.getBase();
                    if (base.getBase() instanceof IdentNode && !base.isPrivate() && base.getProperty().equals(PROTOTYPE_NAME)) {
                        return ((IdentNode) base.getBase()).getName() + "." + accessNode.getProperty();
                    }
                } else if (accessNode.getBase() instanceof IdentNode) {
                    return ((IdentNode) accessNode.getBase()).getName() + "." + accessNode.getProperty();
                }
                return accessNode.getProperty();
            }
        }
        return ANONYMOUS_FUNCTION_NAME;
    }

    private void markDefaultNameUsed() {
        popDefaultName();
        hideDefaultName();
    }

    private void hideDefaultName() {
        // Can be any value as long as getDefaultFunctionName doesn't recognize it as something it
        // can extract a value from. Can't be null.
        defaultNames.add("");
    }

    /**
     * Parse function parameter list.
     */
    private void formalParameterList(final boolean yield, final boolean async) {
        formalParameterList(RPAREN, yield, async);
    }

    private void formalParameter(final boolean yield, final boolean await) {
        if (type == YIELD && yield || isAwait() && await) {
            throw error(expectMessage(IDENT));
        }

        final ParserContextFunctionNode currentFunction = lc.getCurrentFunction();
        final long paramToken = token;
        final int paramLine = line;
        IdentNode ident;
        if (isBindingIdentifier() || !(ES6_DESTRUCTURING && isES6())) {
            ident = bindingIdentifier(yield, await, FUNCTION_PARAMETER_CONTEXT);

            if (type == ASSIGN && (ES6_DEFAULT_PARAMETER && isES6())) {
                next();

                if (type == YIELD && yield || isAwait() && await) {
                    // error: yield in default expression
                    throw error(expectMessage(IDENT));
                }

                // default parameter
                Expression initializer = assignmentExpression(true, yield, await);

                if (isAnonymousFunctionDefinition(initializer)) {
                    initializer = setAnonymousFunctionName(initializer, ident.getName());
                }

                if (currentFunction != null) {
                    addDefaultParameter(paramToken, finish, paramLine, ident, initializer, currentFunction);
                }
            } else {
                if (currentFunction != null) {
                    currentFunction.addParameter(ident);
                }
            }
        } else {
            final Expression pattern = bindingPattern(yield, await);
            // Introduce synthetic temporary parameter to capture the object to be destructured.
            verifyDestructuringParameterBindingPattern(pattern, paramToken, paramLine);

            Expression initializer = null;
            if (type == ASSIGN) {
                next();
                // binding pattern with initializer
                initializer = assignmentExpression(true, yield, await);
            }

            if (currentFunction != null) {
                addDestructuringParameter(paramToken, finish, paramLine, pattern, initializer, currentFunction, false);
            }
        }
    }

    private void functionRestParameter(final TokenType endType, final boolean yield, final boolean await) {
        final long paramToken = token;
        final int paramLine = line;
        final ParserContextFunctionNode currentFunction = lc.getCurrentFunction();

        final Expression pattern = bindingIdentifierOrPattern(yield, await, FUNCTION_PARAMETER_CONTEXT);
        if (pattern instanceof IdentNode) {
            IdentNode ident = ((IdentNode) pattern).setIsRestParameter();

            if (currentFunction != null) {
                currentFunction.addParameter(ident);
            }
        } else {
            verifyDestructuringParameterBindingPattern(pattern, paramToken, paramLine);

            if (currentFunction != null) {
                addDestructuringParameter(paramToken, finish, paramLine, pattern, null, currentFunction, true);
            }
        }

        // rest parameter must be last
        expectDontAdvance(endType);
    }

    /**
     * Parse function parameter list. Same as the other method of the same name - except that the
     * end token type expected is passed as argument to this method.
     *
     * <pre>
     * FormalParameterList :
     *      Identifier
     *      FormalParameterList , Identifier
     * </pre>
     */
    private void formalParameterList(final TokenType endType, final boolean yield, final boolean await) {
        // Track commas.
        boolean first = true;

        while (type != endType) {
            // Comma prior to every argument except the first.
            if (!first) {
                expect(COMMARIGHT);
                // Trailing comma.
                if (ES8_TRAILING_COMMA && isES2017() && type == endType) {
                    break;
                }
            } else {
                first = false;
            }

            if (ES6_REST_PARAMETER && type == ELLIPSIS && isES6()) {
                next();
                functionRestParameter(endType, yield, await);
                break;
            }

            formalParameter(yield, await);
        }
    }

    private static void addDefaultParameter(long paramToken, int paramFinish, int paramLine, IdentNode target, Expression initializer, ParserContextFunctionNode function) {
        assert target != null && initializer != null;
        // desugar to: let target = (param === undefined) ? initializer : param;
        // we use an special positional parameter node not subjected to TDZ rules;
        // thereby, we forego the need for a synthetic param symbol to refer to the passed value.
        final int paramIndex = function.getParameterCount();
        final ParameterNode param = new ParameterNode(paramToken, paramFinish, paramIndex);
        final BinaryNode test = new BinaryNode(Token.recast(paramToken, EQ_STRICT), param, newUndefinedLiteral(paramToken, paramFinish));
        final Expression value = new TernaryNode(Token.recast(paramToken, TERNARY), test, new JoinPredecessorExpression(initializer), new JoinPredecessorExpression(param));
        final VarNode varNode = new VarNode(paramLine, Token.recast(paramToken, LET), paramFinish, target, value, VarNode.IS_LET);
        function.addDefaultParameter(varNode);
    }

    private void addDestructuringParameter(long paramToken, int paramFinish, int paramLine, Expression target, Expression initializer, ParserContextFunctionNode function, boolean isRest) {
        assert isDestructuringLhs(target);
        // desugar to: target := (param === undefined) ? initializer : param;
        // we use an special positional parameter node not subjected to TDZ rules;
        // thereby, we forego the need for a synthetic param symbol to refer to the passed value.
        final int paramIndex = function.getParameterCount();
        final ParameterNode param = new ParameterNode(paramToken, paramFinish, paramIndex, isRest);
        final Expression value;
        if (initializer == null) {
            value = param; // binding pattern without initializer
        } else {
            BinaryNode test = new BinaryNode(Token.recast(paramToken, EQ_STRICT), param, newUndefinedLiteral(paramToken, paramFinish));
            value = new TernaryNode(Token.recast(paramToken, TERNARY), test, new JoinPredecessorExpression(initializer), new JoinPredecessorExpression(param));
        }
        BinaryNode assignment = new BinaryNode(Token.recast(paramToken, ASSIGN_INIT), target, value);
        function.addParameterInitialization(paramLine, assignment, initializer != null, isRest);
    }

    private void verifyDestructuringParameterBindingPattern(final Expression pattern, final long paramToken, final int paramLine) {
        verifyDestructuringBindingPattern(pattern, new Consumer<IdentNode>() {
            @Override
            public void accept(IdentNode identNode) {
                verifyStrictIdent(identNode, FUNCTION_PARAMETER_CONTEXT);

                ParserContextFunctionNode currentFunction = lc.getCurrentFunction();
                if (currentFunction != null) {
                    // declare function-scope variables for destructuring bindings
                    VarNode declaration = new VarNode(paramLine, Token.recast(paramToken, LET), pattern.getFinish(), identNode, null, VarNode.IS_LET | VarNode.IS_DESTRUCTURING);
                    currentFunction.addParameterBindingDeclaration(declaration);
                    // detect duplicate bound names in parameter list
                }
            }
        });
    }

    /**
     * Parse function body.
     *
     * <pre>
     * FunctionBody :
     *      SourceElements?
     * </pre>
     *
     * @return function node (body.)
     */
    private Block functionBody(final ParserContextFunctionNode functionNode) {
        final boolean yield = functionNode.isGenerator();
        final boolean await = functionNode.isAsync() || (isTopLevelAwait() && isModule && functionNode.isModule()) || functionNode.isClassStaticBlock();
        final long bodyToken = token;
        final int bodyFinish;
        final boolean parseBody;
        Object endParserState = null;
        // Create a new function block.
        ParserContextBlockNode body = newBlock(functionNode.createBodyScope());
        try {
            final int functionId = functionNode.getId();
            parseBody = reparsedFunction == null || functionId <= reparsedFunction.getFunctionNodeId();
            // Nashorn extension: expression closures
            if ((env.syntaxExtensions || functionNode.isArrow()) && type != LBRACE) {
                // Example:
                // function square(x) x * x;
                // print(square(3));

                // just expression as function body
                final Expression expr = assignmentExpression(true, yield, await);
                long lastToken = previousToken;
                functionNode.setLastToken(previousToken);
                assert lc.getCurrentBlock().getScope().isFunctionBodyScope();
                // EOL uses length field to store the line number
                final int lastFinish = Token.descPosition(lastToken) + (Token.descType(lastToken) == EOL ? 0 : Token.descLength(lastToken));
                /*
                 * Only create the return node if we aren't skipping nested functions. Note that we
                 * aren't skipping parsing of these extended functions; they're considered to be
                 * small anyway. Also, they don't end with a single well known token, so it'd be
                 * very hard to get correctly (see the note below for reasoning on skipping
                 * happening before instead of after RBRACE for details).
                 */
                if (parseBody) {
                    final ReturnNode returnNode = new ReturnNode(functionNode.getLineNumber(), expr.getToken(), lastFinish, expr);
                    appendStatement(returnNode);
                }
                bodyFinish = finish;
            } else {
                expectDontAdvance(LBRACE);
                if (parseBody || !skipFunctionBody(functionNode)) {
                    next();
                    // Gather the function elements.
                    final List<Statement> prevFunctionDecls = functionDeclarations;
                    functionDeclarations = new ArrayList<>();
                    try {
                        sourceElements(yield, await, 0);
                        addFunctionDeclarations(functionNode);
                    } finally {
                        functionDeclarations = prevFunctionDecls;
                    }

                    if (parseBody) {
                        //@formatter:off
                        // Since the lexer can read ahead and lexify some number of tokens in advance and have
                        // them buffered in the TokenStream, we need to produce a lexer state as it was just
                        // before it lexified RBRACE, and not whatever is its current (quite possibly well read
                        // ahead) state.
                        endParserState = new ParserState(Token.descPosition(token), line, linePosition);

                        // NOTE: you might wonder why do we capture/restore parser state before RBRACE instead of
                        // after RBRACE; after all, we could skip the below "expect(RBRACE);" if we captured the
                        // state after it. The reason is that RBRACE is a well-known token that we can expect and
                        // will never involve us getting into a weird lexer state, and as such is a great reparse
                        // point. Typical example of a weird lexer state after RBRACE would be:
                        //     function this_is_skipped() { ... } "use strict";
                        // because lexer is doing weird off-by-one maneuvers around string literal quotes. Instead
                        // of compensating for the possibility of a string literal (or similar) after RBRACE,
                        // we'll rather just restart parsing from this well-known, friendly token instead.
                        //@formatter:on
                    }
                }
                bodyFinish = Token.descPosition(token) + Token.descLength(token);
                functionNode.setLastToken(token);
                expect(RBRACE);
            }
        } finally {
            functionNode.finishBodyScope();
            restoreBlock(body);
            lc.propagateFunctionFlags();
        }

        // NOTE: we can only do alterations to the function node after restoreFunctionNode.

        if (parseBody) {
            functionNode.setEndParserState(endParserState);
        } else if (!body.getStatements().isEmpty()) {
            /*
             * This is to ensure the body is empty when !parseBody but we couldn't skip parsing it
             * (see skipFunctionBody() for possible reasons). While it is not strictly necessary for
             * correctness to enforce empty bodies in nested functions that were supposed to be
             * skipped, we do assert it as an invariant in few places in the compiler pipeline, so
             * for consistency's sake we'll throw away nested bodies early if we were supposed to
             * skip 'em.
             */
            body.setStatements(Collections.<Statement> emptyList());
        }

        if (reparsedFunction != null) {
            /*
             * We restore the flags stored in the function's ScriptFunctionData that we got when we
             * first eagerly parsed the code. We're doing it because some flags would be set based
             * on the content of the function, or even content of its nested functions, most of
             * which are normally skipped during an on-demand compilation.
             */
            final RecompilableScriptFunctionData data = reparsedFunction.getScriptFunctionData(functionNode.getId());
            if (data != null) {
                // Data can be null if when we originally parsed the file, we removed the function
                // declaration as it was dead code.
                functionNode.setFlag(data.getFunctionFlags());
                // This compensates for missing markEval() in case the function contains an inner
                // function that contains eval(), that now we didn't discover since we skipped the
                // inner function.
                if (functionNode.hasNestedEval()) {
                    assert functionNode.hasScopeBlock();
                    body.setFlag(Block.NEEDS_SCOPE);
                }
            }
        }
        return new Block(bodyToken, bodyFinish, body.getFlags() | Block.IS_BODY, body.getScope(), body.getStatements());
    }

    private boolean skipFunctionBody(final ParserContextFunctionNode functionNode) {
        if (reparsedFunction == null) {
            // Not reparsing, so don't skip any function body.
            return false;
        }
        // Skip to the RBRACE of this function, and continue parsing from there.
        final RecompilableScriptFunctionData data = reparsedFunction.getScriptFunctionData(functionNode.getId());
        if (data == null) {
            /*
             * Nested function is not known to the reparsed function. This can happen if the
             * FunctionNode was in dead code that was removed. Both FoldConstants and Lower prune
             * dead code. In that case, the FunctionNode was dropped before a
             * RecompilableScriptFunctionData could've been created for it.
             */
            return false;
        }
        final ParserState parserState = (ParserState) data.getEndParserState();
        assert parserState != null;

        if (k < stream.last() && start < parserState.position && parserState.position <= Token.descPosition(stream.get(stream.last()))) {
            // RBRACE is already in the token stream, so fast forward to it
            for (; k < stream.last(); k++) {
                long nextToken = stream.get(k + 1);
                if (Token.descPosition(nextToken) == parserState.position && Token.descType(nextToken) == RBRACE) {
                    token = stream.get(k);
                    type = Token.descType(token);
                    next();
                    assert type == RBRACE && start == parserState.position;
                    return true;
                }
            }
        }

        stream.reset();
        lexer = parserState.createLexer(source, lexer, stream, scripting, env.ecmaScriptVersion, shebang, isModule, allowBigInt);
        line = parserState.line;
        linePosition = parserState.linePosition;
        // Doesn't really matter, but it's safe to treat it as if there were a semicolon before
        // the RBRACE.
        type = SEMICOLON;
        scanFirstToken();

        return true;
    }

    /**
     * Encapsulates part of the state of the parser, enough to reconstruct the state of both parser
     * and lexer for resuming parsing after skipping a function body.
     */
    private static class ParserState {
        private final int position;
        private final int line;
        private final int linePosition;

        ParserState(final int position, final int line, final int linePosition) {
            this.position = position;
            this.line = line;
            this.linePosition = linePosition;
        }

        Lexer createLexer(final Source source, final Lexer lexer, final TokenStream stream,
                        final boolean scripting, final int ecmaScriptVersion, final boolean shebang, final boolean isModule, final boolean allowBigInt) {
            final Lexer newLexer = new Lexer(source, position, lexer.limit - position, stream, scripting, ecmaScriptVersion, shebang, isModule, true, allowBigInt);
            newLexer.restoreState(new Lexer.State(position, Integer.MAX_VALUE, line, -1, linePosition, SEMICOLON));
            return newLexer;
        }
    }

    private void addFunctionDeclarations(final ParserContextFunctionNode functionNode) {
        VarNode lastDecl = null;
        for (int i = functionDeclarations.size() - 1; i >= 0; i--) {
            Statement decl = functionDeclarations.get(i);
            if (lastDecl == null && decl instanceof VarNode) {
                decl = lastDecl = ((VarNode) decl).setFlag(VarNode.IS_LAST_FUNCTION_DECLARATION);
                functionNode.setFlag(FunctionNode.HAS_FUNCTION_DECLARATIONS);
            }
            prependStatement(decl);
        }
    }

    private ParserException invalidLHSError(final Expression lhs) {
        JSErrorType errorType = isES2020() ? JSErrorType.SyntaxError : JSErrorType.ReferenceError;
        return error(errorType, AbstractParser.message(MESSAGE_INVALID_LVALUE), lhs.getToken());
    }

    /**
     * Parse unary expression.
     *
     * <pre>
     * PostfixExpression :
     *      LeftHandSideExpression
     *      LeftHandSideExpression ++ // [no LineTerminator here]
     *      LeftHandSideExpression -- // [no LineTerminator here]
     *
     * UnaryExpression :
     *      PostfixExpression
     *      delete UnaryExpression
     *      void UnaryExpression
     *      typeof UnaryExpression
     *      ++ UnaryExpression
     *      -- UnaryExpression
     *      + UnaryExpression
     *      - UnaryExpression
     *      ~ UnaryExpression
     *      ! UnaryExpression
     * </pre>
     *
     * @return Expression node.
     */
    private Expression unaryExpression(boolean yield, boolean await) {
        final int unaryLine = line;
        final long unaryToken = token;

        switch (type) {
            case DELETE: {
                next();
                final Expression expr = unaryExpression(yield, await);

                if (type == TokenType.EXP) {
                    throw error(AbstractParser.message("unexpected.token", type.getNameOrType()));
                }

                return verifyDeleteExpression(unaryLine, unaryToken, expr);
            }
            case VOID:
            case TYPEOF:
            case ADD:
            case SUB:
            case BIT_NOT:
            case NOT:
                next();
                final Expression expr = unaryExpression(yield, await);

                if (type == TokenType.EXP) {
                    throw error(AbstractParser.message("unexpected.token", type.getNameOrType()));
                }

                return new UnaryNode(unaryToken, expr);

            case INCPREFIX:
            case DECPREFIX:
                final TokenType opType = type;
                next();

                final Expression lhs = unaryExpression(yield, await);

                return verifyIncDecExpression(unaryToken, opType, lhs, false);

            default:
                if (isAwait() && await) {
                    return awaitExpression(yield);
                }
                break;
        }

        Expression expression = leftHandSideExpression(yield, await);

        if (last != EOL) {
            switch (type) {
                case INCPREFIX:
                case DECPREFIX:
                    final long opToken = token;
                    final TokenType opType = type;
                    final Expression lhs = expression;
                    next();

                    return verifyIncDecExpression(opToken, opType, lhs, true);
                default:
                    break;
            }
        }

        return expression;
    }

    private Expression verifyDeleteExpression(final int unaryLine, final long unaryToken, final Expression expr) {
        if (expr instanceof BaseNode || expr instanceof IdentNode) {
            if (isStrictMode) {
                if (expr instanceof IdentNode) {
                    IdentNode ident = (IdentNode) expr;
                    if (!ident.isThis() && !ident.isMetaProperty()) {
                        throw error(AbstractParser.message("strict.cant.delete.ident", ident.getName()), unaryToken);
                    }
                } else if (expr instanceof AccessNode && ((AccessNode) expr).isPrivate()) {
                    throw error(AbstractParser.message("strict.cant.delete.private"), unaryToken);
                }
            }
            return new UnaryNode(unaryToken, expr);
        }
        appendStatement(new ExpressionStatement(unaryLine, unaryToken, finish, expr));
        return LiteralNode.newInstance(unaryToken, finish, true);
    }

    private Expression verifyIncDecExpression(final long unaryToken, final TokenType opType, final Expression lhs, final boolean isPostfix) {
        assert lhs != null;

        if (lhs instanceof IdentNode) {
            IdentNode ident = (IdentNode) lhs;
            if (!checkIdentLValue(ident) || ident.isMetaProperty()) {
                throw invalidLHSError(lhs);
            }
            assert opType == TokenType.INCPREFIX || opType == TokenType.DECPREFIX;
            String contextString = opType == TokenType.INCPREFIX ? "operand for ++ operator" : "operand for -- operator";
            verifyStrictIdent((IdentNode) lhs, contextString);
        } else if (!(lhs instanceof AccessNode || lhs instanceof IndexNode) || ((BaseNode) lhs).isOptional()) {
            throw invalidLHSError(lhs);
        }

        return incDecExpression(unaryToken, opType, lhs, isPostfix);
    }

    /**
     * Parse Expression.
     *
     * {@code
     * MultiplicativeExpression :
     *      UnaryExpression
     *      MultiplicativeExpression * UnaryExpression
     *      MultiplicativeExpression / UnaryExpression
     *      MultiplicativeExpression % UnaryExpression
     *
     * See 11.5
     *
     * AdditiveExpression :
     *      MultiplicativeExpression
     *      AdditiveExpression + MultiplicativeExpression
     *      AdditiveExpression - MultiplicativeExpression
     *
     * See 11.6
     *
     * ShiftExpression :
     *      AdditiveExpression
     *      ShiftExpression << AdditiveExpression
     *      ShiftExpression >> AdditiveExpression
     *      ShiftExpression >>> AdditiveExpression
     *
     * See 11.7
     *
     * RelationalExpression :
     *      ShiftExpression
     *      RelationalExpression < ShiftExpression
     *      RelationalExpression > ShiftExpression
     *      RelationalExpression <= ShiftExpression
     *      RelationalExpression >= ShiftExpression
     *      RelationalExpression instanceof ShiftExpression
     *      RelationalExpression in ShiftExpression // if !noIf
     *
     * See 11.8
     *
     *      RelationalExpression
     *      EqualityExpression == RelationalExpression
     *      EqualityExpression != RelationalExpression
     *      EqualityExpression === RelationalExpression
     *      EqualityExpression !== RelationalExpression
     *
     * See 11.9
     *
     * BitwiseANDExpression :
     *      EqualityExpression
     *      BitwiseANDExpression & EqualityExpression
     *
     * BitwiseXORExpression :
     *      BitwiseANDExpression
     *      BitwiseXORExpression ^ BitwiseANDExpression
     *
     * BitwiseORExpression :
     *      BitwiseXORExpression
     *      BitwiseORExpression | BitwiseXORExpression
     *
     * See 11.10
     *
     * LogicalANDExpression :
     *      BitwiseORExpression
     *      LogicalANDExpression && BitwiseORExpression
     *
     * LogicalORExpression :
     *      LogicalANDExpression
     *      LogicalORExpression || LogicalANDExpression
     *
     * See 11.11
     *
     * ConditionalExpression :
     *      LogicalORExpression
     *      LogicalORExpression ? AssignmentExpression : AssignmentExpression
     *
     * See 11.12
     *
     * AssignmentExpression :
     *      ConditionalExpression
     *      LeftHandSideExpression AssignmentOperator AssignmentExpression
     *
     * AssignmentOperator :
     *      = *= /= %= += -= <<= >>= >>>= &= ^= |=
     *
     * See 11.13
     *
     * Expression :
     *      AssignmentExpression
     *      Expression , AssignmentExpression
     *
     * See 11.14
     * }
     *
     * @return Expression node.
     */
    private Expression expression(boolean in, boolean yield, boolean await) {
        return expression(in, yield, await, false);
    }

    private Expression expression(boolean yield, boolean await) {
        return expression(true, yield, await);
    }

    private Expression expression(boolean in, boolean yield, boolean await, boolean inPatternPosition) {
        Expression assignmentExpression = assignmentExpression(in, yield, await, inPatternPosition);
        while (type == COMMARIGHT) {
            long commaToken = token;
            next();

            Expression rhs = assignmentExpression(in, yield, await);
            assignmentExpression = new BinaryNode(commaToken, assignmentExpression, rhs);
        }
        return assignmentExpression;
    }

    private Expression parenthesizedExpressionAndArrowParameterList(boolean yield, boolean await) {
        long primaryToken = token;
        int startLine = line;
        assert type == LPAREN;
        next();

        if (ES6_ARROW_FUNCTION && isES6() && type == RPAREN) {
            // ()
            nextOrEOL();
            expectDontAdvance(ARROW);
            return new ExpressionList(primaryToken, finish, Collections.emptyList());
        }

        Expression assignmentExpression = null;
        boolean hasCoverInitializedName = false;
        boolean hasRestParameter = false;
        long commaToken = 0L;

        ParserContextFunctionNode cover = createParserContextArrowFunctionNode(primaryToken, startLine, false, true);
        lc.push(cover);
        ParserContextBlockNode parameterBlock = cover.createParameterBlock();
        lc.push(parameterBlock);
        try {
            while (true) {
                if (ES6_ARROW_FUNCTION && ES6_REST_PARAMETER && isES6() && type == ELLIPSIS) {
                    // (a, b, ...rest) is not a valid expression, but a valid arrow function
                    // parameter
                    // list. Since the rest parameter is always last, we know that the cover
                    // expression
                    // has to end here with a binding identifier/pattern followed by RPAREN and
                    // ARROW.
                    assignmentExpression = arrowFunctionRestParameter(assignmentExpression, commaToken, yield, await);
                    hasRestParameter = true;
                    break;
                } else if (ES6_ARROW_FUNCTION && ES8_TRAILING_COMMA && isES2017() && type == RPAREN && lookaheadIsArrow()) {
                    // Trailing comma at end of arrow function parameter list
                    break;
                }

                Expression rhs = assignmentExpression(true, yield, await, true);
                hasCoverInitializedName = hasCoverInitializedName || hasCoverInitializedName(rhs);

                if (assignmentExpression == null) {
                    assignmentExpression = rhs;
                } else {
                    assert Token.descType(commaToken) == COMMARIGHT;
                    assignmentExpression = new BinaryNode(commaToken, assignmentExpression, rhs);
                }

                if (type != COMMARIGHT) {
                    break;
                }
                commaToken = token;
                next();
            }
        } finally {
            lc.pop(parameterBlock);
            lc.pop(cover);
        }

        boolean arrowAhead = lookaheadIsArrow();
        if (hasCoverInitializedName && !(type == RPAREN && arrowAhead)) {
            throw error(AbstractParser.message(MESSAGE_INVALID_PROPERTY_INITIALIZER));
        }

        if (hasRestParameter) {
            // Rest parameter must be last and followed by `)` [NoLineTerminator] `=>`.
            expectDontAdvance(RPAREN);
            nextOrEOL();
            expectDontAdvance(ARROW);
        } else {
            expect(RPAREN);
        }

        if (arrowAhead) {
            // arrow parameter list
            commitArrowHead(cover);
        } else {
            // parenthesized expression
            assignmentExpression.makeParenthesized(Token.descPosition(primaryToken), finish);
            revertArrowHead(cover);
        }

        return assignmentExpression;
    }

    private void commitArrowHead(ParserContextFunctionNode cover) {
        assert coverArrowFunction == null;
        if (cover.getYieldOrAwaitInParameters() != 0L) {
            throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), cover.getYieldOrAwaitInParameters());
        }
        coverArrowFunction = cover;
    }

    private void revertArrowHead(ParserContextFunctionNode cover) {
        cover.getParameterScope().close();
        // merge flags gathered during expression parsing into the current function's flags.
        lc.setCurrentFunctionFlag(cover.getFlags() & FunctionNode.ARROW_HEAD_FLAGS);
    }

    private Expression arrowFunctionRestParameter(Expression paramListExpr, long commaToken, final boolean yield, final boolean await) {
        final long ellipsisToken = token;
        assert type == ELLIPSIS;
        next();

        final Expression pattern = bindingIdentifierOrPattern(yield, await, FUNCTION_PARAMETER_CONTEXT);
        final Expression restParam;
        if (pattern instanceof IdentNode) {
            restParam = ((IdentNode) pattern).setIsRestParameter();
        } else {
            restParam = new UnaryNode(Token.recast(ellipsisToken, SPREAD_ARGUMENT), pattern);
        }

        if (paramListExpr == null) {
            return restParam;
        } else {
            assert Token.descType(commaToken) == COMMARIGHT;
            return new BinaryNode(commaToken, paramListExpr, restParam);
        }
    }

    private Expression expression(int minPrecedence, boolean in, boolean yield, boolean await) {
        Expression lhs;
        if (in && type == PRIVATE_IDENT && isPrivateFieldsIn() && lookahead() == IN) {
            lhs = privateIdentifierUse().setIsPrivateInCheck();
        } else {
            lhs = unaryExpression(yield, await);
        }
        return expression(lhs, minPrecedence, in, yield, await);
    }

    private JoinPredecessorExpression joinPredecessorExpression(boolean yield, boolean await) {
        return new JoinPredecessorExpression(expression(yield, await));
    }

    private Expression expression(Expression exprLhs, int minPrecedence, boolean in, boolean yield, boolean await) {
        // Get the precedence of the next operator.
        int precedence = type.getPrecedence();
        Expression lhs = exprLhs;

        // While greater precedence.
        while (type.isOperator(in) && precedence >= minPrecedence) {
            // Capture the operator token.
            final long op = token;

            if (type == TERNARY) {
                // Skip operator.
                next();

                // Pass expression. Middle expression of a conditional expression can be a "in"
                // expression - even in the contexts where "in" is not permitted.
                final Expression trueExpr = assignmentExpression(true, yield, await);

                expect(COLON);

                // Fail expression.
                final Expression falseExpr = assignmentExpression(in, yield, await);

                // Build up node.
                lhs = new TernaryNode(op, lhs, new JoinPredecessorExpression(trueExpr), new JoinPredecessorExpression(falseExpr));
            } else {
                final TokenType opType = type;
                // Skip operator.
                next();

                assert !Token.descType(op).isAssignment();
                // Get the next primary expression.
                Expression rhs;
                if (in && type == PRIVATE_IDENT && isPrivateFieldsIn() && lookahead() == IN && precedence < IN.getPrecedence()) {
                    assert opType != IN;
                    rhs = privateIdentifierUse().setIsPrivateInCheck();
                } else {
                    rhs = unaryExpression(yield, await);
                }

                // Get precedence of next operator.
                int nextPrecedence = type.getPrecedence();

                // Subtask greater precedence.
                while (type.isOperator(in) && (nextPrecedence > precedence || (nextPrecedence == precedence && !type.isLeftAssociative()))) {
                    rhs = expression(rhs, nextPrecedence, in, yield, await);
                    nextPrecedence = type.getPrecedence();
                }
                lhs = newBinaryExpression(op, lhs, rhs);
            }

            precedence = type.getPrecedence();
        }

        return lhs;
    }

    private Expression assignmentExpression(boolean in, boolean yield, boolean await) {
        return assignmentExpression(in, yield, await, false);
    }

    /**
     * AssignmentExpression.
     *
     * AssignmentExpression[In, Yield] : ConditionalExpression[?In, ?Yield] [+Yield]
     * YieldExpression[?In] ArrowFunction[?In, ?Yield] AsyncArrowFunction
     * LeftHandSideExpression[?Yield] = AssignmentExpression[?In, ?Yield]
     * LeftHandSideExpression[?Yield] AssignmentOperator AssignmentExpression[?In, ?Yield]
     */
    private Expression assignmentExpression(boolean in, boolean yield, boolean await, boolean inPatternPosition) {
        if (type == YIELD && yield) {
            return yieldExpression(in, await);
        }

        boolean asyncArrow = isAsync() && lookaheadIsAsyncArrowParameterListStart();
        // If true, one of:
        // 1. async ident
        // .. This case is handled below.
        // 2. async (
        // .. May turn out to be a CallExpression instead of an AsyncArrowFunction; this is handled
        // .. in leftHandSideExpression() which returns an ExpressionList if an AsyncArrowFunction.

        final long startToken = token;
        final int startLine = line;
        Expression exprLhs = conditionalExpression(in, yield, await);

        if (asyncArrow && exprLhs instanceof IdentNode && isBindingIdentifier() && lookaheadIsArrow()) {
            // async ident =>
            exprLhs = primaryExpression(yield, await);
        }

        if (ES6_ARROW_FUNCTION && type == ARROW && isES6()) {
            // Look behind to check there's no LineTerminator between IDENT/RPAREN and ARROW
            if (lookbehindNoLineTerminatorBeforeArrow()) {
                return arrowFunction(startToken, startLine, exprLhs, asyncArrow);
            }
        }
        assert !(exprLhs instanceof ExpressionList);

        if (type.isAssignment()) {
            final boolean isAssign = type == ASSIGN;
            if (isAssign) {
                pushDefaultName(exprLhs);
            }
            try {
                long assignToken = token;
                next();
                Expression exprRhs = assignmentExpression(in, yield, await);
                return verifyAssignment(assignToken, exprLhs, exprRhs, inPatternPosition);
            } finally {
                if (isAssign) {
                    popDefaultName();
                }
            }
        } else {
            if (!inPatternPosition && hasCoverInitializedName(exprLhs)) {
                throw error(AbstractParser.message(MESSAGE_INVALID_PROPERTY_INITIALIZER));
            }
            return exprLhs;
        }
    }

    /**
     * ConditionalExpression.
     */
    private Expression conditionalExpression(boolean in, boolean yield, boolean await) {
        return expression(TERNARY.getPrecedence(), in, yield, await);
    }

    /**
     * ArrowFunction.
     *
     * @param startToken start token of the ArrowParameters expression
     * @param functionLine start line of the arrow function
     * @param paramListExpr ArrowParameters expression or {@code null} for {@code ()} (empty list)
     */
    private Expression arrowFunction(final long startToken, final int functionLine, final Expression paramListExpr, boolean async) {
        // caller needs to check that there's no LineTerminator between parameter list and arrow
        assert type != ARROW || lookbehindNoLineTerminatorBeforeArrow();
        expect(ARROW);

        final ParserContextFunctionNode functionNode;
        if (coverArrowFunction == null) {
            functionNode = createParserContextArrowFunctionNode(startToken, functionLine, async, false);
        } else {
            functionNode = coverArrowFunction;
            functionNode.setCoverArrowHead(false);
            coverArrowFunction = null;
        }
        assert functionNode.isArrow() && !functionNode.isCoverArrowHead();
        assert functionNode.isAsync() == async;
        functionNode.setInternalName(ARROW_FUNCTION_NAME);
        functionNode.setFlag(FunctionNode.IS_ANONYMOUS);

        lc.push(functionNode);
        try {
            final ParserContextBlockNode parameterBlock = functionNode.createParameterBlock();
            lc.push(parameterBlock);
            Block functionBody;
            try {
                convertArrowFunctionParameterList(paramListExpr, functionNode);
                functionBody = functionBody(functionNode);
            } finally {
                restoreBlock(parameterBlock);
            }

            verifyParameterList(functionNode);

            if (parameterBlock != null) {
                functionBody = wrapParameterBlock(parameterBlock, functionBody);
            }

            final FunctionNode function = createFunctionNode(
                            functionNode,
                            functionNode.getFirstToken(),
                            functionNode.getIdent(),
                            functionLine,
                            functionBody);
            return function;
        } finally {
            lc.pop(functionNode);
        }
    }

    private ParserContextFunctionNode createParserContextArrowFunctionNode(long startToken, int startLine, boolean async, boolean cover) {
        final long functionToken = Token.recast(startToken, ARROW);
        final IdentNode name = null;
        ParserContextFunctionNode function = createParserContextFunctionNode(name, functionToken, FunctionNode.IS_ARROW, startLine);
        if (async) {
            function.setFlag(FunctionNode.IS_ASYNC);
        }
        if (cover) {
            function.setCoverArrowHead(true);
            assert coverArrowFunction == null;
        }
        return function;
    }

    private static Expression convertExpressionListToExpression(ExpressionList exprList) {
        if (exprList.getExpressions().isEmpty()) {
            return null;
        } else if (exprList.getExpressions().size() == 1) {
            return exprList.getExpressions().get(0);
        } else {
            long recastToken = Token.recast(exprList.getToken(), COMMARIGHT);
            Expression result = null;
            for (Expression expression : exprList.getExpressions()) {
                result = result == null ? expression : new BinaryNode(recastToken, result, expression);
            }
            return result;
        }
    }

    private void convertArrowFunctionParameterList(Expression paramList, ParserContextFunctionNode function) {
        Expression paramListExpr = paramList;
        if (paramListExpr instanceof ExpressionList) {
            // see parenthesizedExpressionAndArrowParameterList() and leftHandSideExpression()
            paramListExpr = convertExpressionListToExpression((ExpressionList) paramListExpr);
        }
        if (paramListExpr == null) {
            // empty parameter list, i.e. () =>
            return;
        }
        final int functionLine = function.getLineNumber();
        if (paramListExpr instanceof IdentNode || paramListExpr.isTokenType(ASSIGN) || isDestructuringLhs(paramListExpr) || paramListExpr.isTokenType(SPREAD_ARGUMENT)) {
            convertArrowParameter(paramListExpr, 0, functionLine, function);
        } else if (paramListExpr instanceof BinaryNode && Token.descType(paramListExpr.getToken()) == COMMARIGHT) {
            ArrayList<Expression> params = new ArrayList<>();
            Expression car = paramListExpr;
            do {
                Expression cdr = ((BinaryNode) car).getRhs();
                params.add(cdr);
                car = ((BinaryNode) car).getLhs();
            } while (car instanceof BinaryNode && Token.descType(car.getToken()) == COMMARIGHT);
            params.add(car);

            for (int i = params.size() - 1, pos = 0; i >= 0; i--, pos++) {
                Expression param = params.get(i);
                if (i != 0 && param.isTokenType(SPREAD_ARGUMENT)) {
                    throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), param.getToken());
                } else {
                    convertArrowParameter(param, pos, functionLine, function);
                }
            }
        } else {
            throw error(AbstractParser.message("expected.arrow.parameter"), paramListExpr.getToken());
        }
    }

    private void convertArrowParameter(Expression param, int index, int paramLine, ParserContextFunctionNode currentFunction) {
        assert index == currentFunction.getParameterCount();
        if (param instanceof IdentNode) {
            IdentNode ident = (IdentNode) param;
            verifyStrictIdent(ident, FUNCTION_PARAMETER_CONTEXT);
            if (ident.isParenthesized()) {
                throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), param.getToken());
            }
            assert !(currentFunction.isAsync() && AWAIT.getName().equals(ident.getName()));
            currentFunction.addParameter(ident);
            return;
        }

        if (param.isTokenType(ASSIGN)) {
            Expression lhs = ((BinaryNode) param).getLhs();
            long paramToken = lhs.getToken();
            Expression initializer = ((BinaryNode) param).getRhs();
            assert !(initializer instanceof IdentNode && currentFunction.isAsync() && AWAIT.getName().equals(((IdentNode) initializer).getName()));
            if (lhs instanceof IdentNode && !lhs.isParenthesized()) {
                // default parameter
                IdentNode ident = (IdentNode) lhs;

                if (isAnonymousFunctionDefinition(initializer)) {
                    initializer = setAnonymousFunctionName(initializer, ident.getName());
                }

                addDefaultParameter(paramToken, param.getFinish(), paramLine, ident, initializer, currentFunction);
                return;
            } else if (isDestructuringLhs(lhs)) {
                // binding pattern with initializer
                verifyDestructuringParameterBindingPattern(lhs, paramToken, paramLine);

                addDestructuringParameter(paramToken, param.getFinish(), paramLine, lhs, initializer, currentFunction, false);
            } else {
                throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), paramToken);
            }
        } else if (isDestructuringLhs(param)) {
            // binding pattern
            long paramToken = param.getToken();

            verifyDestructuringParameterBindingPattern(param, paramToken, paramLine);

            addDestructuringParameter(paramToken, param.getFinish(), paramLine, param, null, currentFunction, false);
        } else if (param.isTokenType(SPREAD_ARGUMENT)) {
            // rest parameter
            if (lookbehindIsTrailingCommaInArrowParameters()) {
                throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), param.getToken());
            }
            Expression restParam = ((UnaryNode) param).getExpression();
            if (restParam instanceof IdentNode) {
                IdentNode ident = ((IdentNode) restParam).setIsRestParameter();
                convertArrowParameter(ident, index, paramLine, currentFunction);
            } else if (isDestructuringLhs(restParam)) {
                verifyDestructuringParameterBindingPattern(restParam, restParam.getToken(), paramLine);
                addDestructuringParameter(restParam.getToken(), restParam.getFinish(), paramLine, restParam, null, currentFunction, true);
            } else {
                throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), param.getToken());
            }
        } else {
            throw error(AbstractParser.message(MESSAGE_INVALID_ARROW_PARAMETER), param.getToken());
        }
    }

    // Checks whether arrow function parameters end with a trailing comma
    // (which is not allowed if rest parameter is present).
    private boolean lookbehindIsTrailingCommaInArrowParameters() {
        int idx = k - 1;
        while (true) {
            idx--;
            TokenType t = T(idx);
            switch (t) {
                case COMMENT:
                case EOL:
                case ARROW:
                case RPAREN:
                    continue;
                case COMMARIGHT:
                    return true;
                default:
                    return false;
            }
        }
    }

    private boolean lookbehindNoLineTerminatorBeforeArrow() {
        assert type == ARROW;
        if (last == RPAREN) {
            return true;
        } else if (last == IDENT) {
            return true;
        }
        for (int i = k - 1; i >= 0; i--) {
            TokenType t = T(i);
            switch (t) {
                case RPAREN:
                case IDENT:
                    return true;
                case EOL:
                    return false;
                case COMMENT:
                    continue;
                default:
                    return (t.isContextualKeyword() || t.isFutureStrict());
            }
        }
        return false;
    }

    private boolean lookbehindNoLineTerminatorAfterAsync() {
        assert type == LPAREN;
        return last == ASYNC;
    }

    private boolean lookaheadIsArrow() {
        // find ARROW, skipping over COMMENT
        int i = 1;
        for (;;) {
            TokenType t = T(k + i++);
            if (t == ARROW) {
                break;
            } else if (t == COMMENT) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse an end of line.
     */
    private void endOfLine() {
        switch (type) {
            case SEMICOLON:
            case EOL:
                next();
                break;
            case RPAREN:
            case RBRACKET:
            case RBRACE:
            case EOF:
                break;
            default:
                if (last != EOL) {
                    expect(SEMICOLON);
                }
                break;
        }
    }

    /**
     * Parse untagged template literal as string concatenation.
     */
    private Expression templateLiteral(boolean yield, boolean await) {
        assert type == TEMPLATE || type == TEMPLATE_HEAD;
        final boolean noSubstitutionTemplate = type == TEMPLATE;
        final long startToken = token;
        boolean previousPauseOnRightBrace = lexer.pauseOnRightBrace;
        try {
            lexer.pauseOnRightBrace = true;
            LiteralNode<?> literal = getLiteral();
            if (noSubstitutionTemplate) {
                return literal;
            }

            List<Expression> expressions = new ArrayList<>();
            expressions.add(literal);
            TokenType lastLiteralType;
            do {
                Expression expression = templateLiteralExpression(yield, await);
                expressions.add(expression);
                lastLiteralType = type;
                literal = getLiteral();
                expressions.add(literal);
            } while (lastLiteralType == TEMPLATE_MIDDLE);
            return TemplateLiteralNode.newUntagged(startToken, literal.getFinish(), expressions);
        } finally {
            lexer.pauseOnRightBrace = previousPauseOnRightBrace;
        }
    }

    /**
     * Parse expression inside a template literal.
     */
    private Expression templateLiteralExpression(boolean yield, boolean await) {
        assert lexer.pauseOnRightBrace;
        Expression expression = expression(true, yield, await);
        if (type != RBRACE) {
            throw error(AbstractParser.message("unterminated.template.expression"), token);
        }
        lexer.scanTemplateSpan();
        next();
        assert type == TEMPLATE_MIDDLE || type == TEMPLATE_TAIL;
        return expression;
    }

    /**
     * Parse tagged template literal as argument list.
     *
     * @return argument list for a tag function call (template object, ...substitutions)
     */
    private List<Expression> templateLiteralArgumentList(boolean yield, boolean await) {
        assert type == TEMPLATE || type == TEMPLATE_HEAD;
        final ArrayList<Expression> argumentList = new ArrayList<>();
        final ArrayList<Expression> rawStrings = new ArrayList<>();
        final ArrayList<Expression> cookedStrings = new ArrayList<>();
        argumentList.add(null); // filled at the end

        final long templateToken = token;
        final boolean hasSubstitutions = type == TEMPLATE_HEAD;
        boolean previousPauseOnRightBrace = lexer.pauseOnRightBrace;
        try {
            lexer.pauseOnRightBrace = true;
            addTemplateLiteralString(rawStrings, cookedStrings);

            if (hasSubstitutions) {
                TokenType lastLiteralType;
                do {
                    Expression expression = templateLiteralExpression(yield, await);
                    argumentList.add(expression);

                    lastLiteralType = type;
                    addTemplateLiteralString(rawStrings, cookedStrings);
                } while (lastLiteralType == TEMPLATE_MIDDLE);
            }

            final Expression templateObject = TemplateLiteralNode.newTagged(templateToken, rawStrings.get(rawStrings.size() - 1).getFinish(), rawStrings, cookedStrings);
            argumentList.set(0, templateObject);
            return optimizeList(argumentList);
        } finally {
            lexer.pauseOnRightBrace = previousPauseOnRightBrace;
        }
    }

    private void addTemplateLiteralString(final ArrayList<Expression> rawStrings, final ArrayList<Expression> cookedStrings) {
        final long stringToken = token;
        final String rawString = lexer.valueOfRawString(stringToken);
        final String cookedString = lexer.valueOfTaggedTemplateString(stringToken);
        next();
        Expression cookedExpression;
        if (cookedString == null) {
            // A tagged template string with an invalid escape sequence has value 'undefined'
            cookedExpression = newUndefinedLiteral(stringToken, finish);
        } else {
            cookedExpression = LiteralNode.newInstance(stringToken, cookedString);
        }
        rawStrings.add(LiteralNode.newInstance(stringToken, rawString));
        cookedStrings.add(cookedExpression);
    }

    /**
     * Parse a module.
     *
     * <pre>
     * Module :
     *      ModuleBody?
     *
     * ModuleBody :
     *      ModuleItemList
     * </pre>
     */
    private FunctionNode module(final String moduleName) {
        // Make a pseudo-token for the script holding its start and length.
        int functionStart = Math.min(Token.descPosition(Token.withDelimiter(token)), finish);
        final long functionToken = Token.toDesc(FUNCTION, functionStart, source.getLength() - functionStart);
        final int functionLine = line;

        final Scope moduleScope = Scope.createModule();
        final IdentNode ident = null;
        final ParserContextFunctionNode script = createParserContextFunctionNode(
                        ident,
                        functionToken,
                        FunctionNode.IS_MODULE,
                        functionLine,
                        Collections.<IdentNode> emptyList(), 0, moduleScope);
        script.setInternalName(moduleName);

        lc.push(script);
        final ParserContextModuleNode module = new ParserContextModuleNode(moduleName, moduleScope, this);
        final ParserContextBlockNode body = newBlock(moduleScope);
        functionDeclarations = new ArrayList<>();

        try {
            moduleBody(module);

            // Insert a synthetic yield before the module body but after any function declarations.
            long yieldToken = Token.toDesc(YIELD, functionStart, 0);
            prependStatement(new ExpressionStatement(functionLine, yieldToken, functionLine, new UnaryNode(yieldToken, newUndefinedLiteral(yieldToken, finish))));
            script.setFlag(FunctionNode.IS_GENERATOR);

            addFunctionDeclarations(script);
        } finally {
            functionDeclarations = null;
            restoreBlock(body);
            lc.pop(script);
        }

        body.setFlag(Block.NEEDS_SCOPE);
        final Block programBody = new Block(functionToken, finish, body.getFlags() | Block.IS_SYNTHETIC | Block.IS_BODY | Block.IS_MODULE_BODY, body.getScope(), body.getStatements());
        script.setLastToken(token);

        expect(EOF);

        script.setModule(module.createModule());
        return createFunctionNode(script, functionToken, ident, functionLine, programBody);
    }

    /**
     * Parse module body.
     *
     * <pre>
     * ModuleBody :
     *      ModuleItemList
     *
     * ModuleItemList :
     *      ModuleItem
     *      ModuleItemList ModuleItem
     *
     * ModuleItem :
     *      ImportDeclaration
     *      ExportDeclaration
     *      StatementListItem
     * </pre>
     */
    private void moduleBody(ParserContextModuleNode module) {
        loop: while (type != EOF) {
            switch (type) {
                case EOF:
                    break loop;
                case EXPORT:
                    exportDeclaration(module);
                    break;
                case IMPORT:
                    // Ensure we are parsing an import declaration and not import.meta or import().
                    if (!isImportExpression()) {
                        importDeclaration(module);
                        break;
                    }
                    // fall through
                default:
                    // StatementListItem
                    boolean await = isTopLevelAwait();
                    statement(false, await, true, 0, false, false, false);
                    break;
            }
        }
    }

    private boolean isTopLevelAwait() {
        return ES2022_TOP_LEVEL_AWAIT && env.topLevelAwait;
    }

    private boolean isImportExpression() {
        assert type == IMPORT;
        if (!isES2020()) {
            return false;
        }
        TokenType la = lookahead();
        return la == PERIOD || la == LPAREN;
    }

    private void declareImportBinding(IdentNode ident, boolean star) {
        Scope moduleScope = lc.getCurrentBlock().getScope();
        assert moduleScope.isModuleScope();
        if (moduleScope.hasSymbol(ident.getName())) {
            throw error(ECMAErrors.getMessage("syntax.error.redeclare.variable", ident.getName()), ident.getToken());
        }
        moduleScope.putSymbol(new Symbol(ident.getName(), Symbol.IS_CONST | Symbol.HAS_BEEN_DECLARED | (star ? 0 : Symbol.IS_IMPORT_BINDING)));
    }

    private void declareImportBinding(IdentNode ident) {
        declareImportBinding(ident, false);
    }

    private void declareImportStarBinding(IdentNode ident) {
        declareImportBinding(ident, true);
    }

    private IdentNode importedBindingIdentifier() {
        return bindingIdentifier(false, isTopLevelAwait(), IMPORTED_BINDING_CONTEXT);
    }

    /**
     * Parse import declaration.
     *
     * <pre>
     * ImportDeclaration :
     *     import ImportClause FromClause ;
     *     import ModuleSpecifier ;
     *     import ImportClause FromClause [no LineTerminator here] AssertClause ;
     *     import ModuleSpecifier [no LineTerminator here] AssertClause ;
     * ImportClause :
     *     ImportedDefaultBinding
     *     NameSpaceImport
     *     NamedImports
     *     ImportedDefaultBinding , NameSpaceImport
     *     ImportedDefaultBinding , NamedImports
     * ImportedDefaultBinding :
     *     ImportedBinding
     * ModuleSpecifier :
     *     StringLiteral
     * ImportedBinding :
     *     BindingIdentifier
     * </pre>
     */
    private void importDeclaration(ParserContextModuleNode module) {
        final long importToken = token;
        expect(IMPORT);
        if (type == STRING || type == ESCSTRING) {
            // import ModuleSpecifier ;
            String moduleSpecifier = (String) getValue();
            long specifierToken = token;
            next();
            LiteralNode<String> specifier = LiteralNode.newInstance(specifierToken, moduleSpecifier);
            Map<String, String> assertions = Collections.emptyMap();
            if (env.importAssertions && type == ASSERT && last != EOL) {
                assertions = assertClause();
            }
            module.addModuleRequest(ModuleRequest.create(moduleSpecifier, assertions));
            module.addImport(new ImportNode(importToken, Token.descPosition(importToken), finish, specifier));
        } else {
            // import ImportClause FromClause ;
            final List<ImportEntry> importEntries = new ArrayList<>();
            final ImportClauseNode importClause;
            final long startToken = token;
            if (type == MUL) {
                NameSpaceImportNode namespaceNode = nameSpaceImport();
                importClause = new ImportClauseNode(startToken, Token.descPosition(startToken), finish, namespaceNode);
                importEntries.add(ImportEntry.importStarAsNameSpaceFrom(namespaceNode.getBindingIdentifier().getName()));
            } else if (type == LBRACE) {
                NamedImportsNode namedImportsNode = namedImports(importEntries);
                importClause = new ImportClauseNode(startToken, Token.descPosition(startToken), finish, namedImportsNode);
            } else if (isBindingIdentifier()) {
                // ImportedDefaultBinding
                IdentNode importedDefaultBinding = importedBindingIdentifier();
                declareImportBinding(importedDefaultBinding);
                ImportEntry defaultImport = ImportEntry.importDefault(importedDefaultBinding.getName());
                importEntries.add(defaultImport);

                if (type == COMMARIGHT) {
                    next();
                    if (type == MUL) {
                        NameSpaceImportNode namespaceNode = nameSpaceImport();
                        importClause = new ImportClauseNode(startToken, Token.descPosition(startToken), finish, importedDefaultBinding, namespaceNode);
                        importEntries.add(ImportEntry.importStarAsNameSpaceFrom(namespaceNode.getBindingIdentifier().getName()));
                    } else if (type == LBRACE) {
                        NamedImportsNode namedImportsNode = namedImports(importEntries);
                        importClause = new ImportClauseNode(startToken, Token.descPosition(startToken), finish, importedDefaultBinding, namedImportsNode);
                    } else {
                        // expected NameSpaceImport or NamedImports
                        throw error(AbstractParser.message("expected.named.import"));
                    }
                } else {
                    importClause = new ImportClauseNode(startToken, Token.descPosition(startToken), finish, importedDefaultBinding);
                }
            } else {
                // expected ImportClause or ModuleSpecifier
                throw error(AbstractParser.message("expected.import"));
            }

            FromNode fromNode = fromClause();
            Map<String, String> assertions = Collections.emptyMap();
            if (env.importAssertions && type == ASSERT && last != EOL) {
                assertions = assertClause();
            }
            module.addImport(new ImportNode(importToken, Token.descPosition(importToken), finish, importClause, fromNode));
            String moduleSpecifier = fromNode.getModuleSpecifier().getValue();
            ModuleRequest moduleRequest = ModuleRequest.create(moduleSpecifier, assertions);
            module.addModuleRequest(moduleRequest);
            for (int i = 0; i < importEntries.size(); i++) {
                module.addImportEntry(importEntries.get(i).withFrom(moduleRequest));
            }
        }
        endOfLine();
    }

    /**
     * Parse assert clause.
     *
     * <pre>
     *     assert { }
     *     assert { AssertEntries ,opt }
     * </pre>
     */
    private Map<String, String> assertClause() {
        assert type == ASSERT;
        next();
        expect(LBRACE);
        Map<String, String> assertions = assertEntries();
        expect(RBRACE);
        return assertions;
    }

    /**
     * Parse assert entry.
     *
     * <pre>
     * AssertEntries:
     *     AssertionKey : StringLiteral
     *     AssertionKey : StringLiteral , AssertEntries
     * AssertKey:
     *     IdentifierName
     *     StringLiteral
     * </pre>
     */
    private Map<String, String> assertEntries() {
        Map<String, String> assertions = new HashMap<>();

        while (type != RBRACE) {
            final long errorToken = token;
            String assertionKey;
            if (type == STRING || type == ESCSTRING) {
                assertionKey = (String) getValue();
                next();
            } else {
                assertionKey = getIdentifierName().getName();
            }
            expect(COLON);
            String value = null;
            if (type == STRING || type == ESCSTRING) {
                value = (String) getValue();
                next();
            } else {
                expect(STRING);
            }
            if (assertions.containsKey(assertionKey)) {
                throw error(AbstractParser.message("duplicate.import.assertion", assertionKey), errorToken);
            } else {
                assertions.put(assertionKey, value);
            }
            if (type == COMMARIGHT) {
                next();
            } else {
                break;
            }
        }
        return assertions;
    }

    /**
     * <pre>
     * NameSpaceImport :
     *      * as ImportedBinding
     * </pre>
     *
     * @return imported binding identifier
     */
    private NameSpaceImportNode nameSpaceImport() {
        final long startToken = token;
        assert type == MUL;
        next();

        expect(AS);

        IdentNode localNameSpace = importedBindingIdentifier();
        declareImportStarBinding(localNameSpace);
        return new NameSpaceImportNode(startToken, Token.descPosition(startToken), finish, localNameSpace);
    }

    /**
     * <pre>
     * NamedImports :
     *     { }
     *     { ImportsList }
     *     { ImportsList , }
     * ImportsList :
     *     ImportSpecifier
     *     ImportsList , ImportSpecifier
     * ImportSpecifier :
     *     ImportedBinding
     *     IdentifierName as ImportedBinding
     * ImportedBinding :
     *     BindingIdentifier
     * </pre>
     */
    private NamedImportsNode namedImports(List<ImportEntry> importEntries) {
        final long startToken = token;
        assert type == LBRACE;
        next();
        ArrayList<ImportSpecifierNode> importSpecifiers = new ArrayList<>();
        while (type != RBRACE) {
            boolean bindingIdentifier = isBindingIdentifier();
            long nameToken = token;
            IdentNode importName = getIdentifierName();
            if (type == AS) {
                next();
                IdentNode localName = importedBindingIdentifier();
                importSpecifiers.add(new ImportSpecifierNode(nameToken, Token.descPosition(nameToken), finish, localName, importName));
                declareImportBinding(localName);
                importEntries.add(ImportEntry.importSpecifier(importName.getName(), localName.getName()));
            } else if (bindingIdentifier) {
                verifyIdent(importName, false, false);
                verifyStrictIdent(importName, IMPORTED_BINDING_CONTEXT);
                importSpecifiers.add(new ImportSpecifierNode(nameToken, Token.descPosition(nameToken), finish, importName, null));
                declareImportBinding(importName);
                importEntries.add(ImportEntry.importSpecifier(importName.getName()));
            } else {
                // expected BindingIdentifier
                throw error(AbstractParser.message("expected.binding.identifier"), nameToken);
            }
            if (type == COMMARIGHT) {
                next();
            } else {
                break;
            }
        }
        expect(RBRACE);
        return new NamedImportsNode(startToken, Token.descPosition(startToken), finish, optimizeList(importSpecifiers));
    }

    /**
     * <pre>
     * FromClause :
     *      from ModuleSpecifier
     * </pre>
     */
    private FromNode fromClause() {
        int fromStart = start;
        long fromToken = token;
        expect(FROM);

        if (type == STRING || type == ESCSTRING) {
            String moduleSpecifier = (String) getValue();
            long specifierToken = token;
            next();
            LiteralNode<String> specifier = LiteralNode.newInstance(specifierToken, moduleSpecifier);
            return new FromNode(fromToken, fromStart, finish, specifier);
        } else {
            throw error(expectMessage(STRING));
        }
    }

    /**
     * Parse export declaration.
     *
     * <pre>
     * ExportDeclaration :
     *     export ExportFromClause FromClause ;
     *     export ExportFromClause FromClause [no LineTerminator here] AssertClause;
     *     export NamedExports ;
     *     export VariableStatement
     *     export Declaration
     *     export default HoistableDeclaration[Default]
     *     export default ClassDeclaration[Default]
     *     export default [lookahead !in {function, class}] AssignmentExpression[In] ;
     * </pre>
     */
    private void exportDeclaration(ParserContextModuleNode module) {
        final long exportToken = token;
        Map<String, String> assertions = Collections.emptyMap();
        expect(EXPORT);
        final boolean yield = false;
        final boolean await = isTopLevelAwait();
        switch (type) {
            case MUL: {
                next();
                IdentNode exportName = null;
                if (type == AS && isES2020()) {
                    next();
                    exportName = getIdentifierName();
                }
                FromNode from = fromClause();
                if (env.importAssertions && type == ASSERT && last != EOL) {
                    assertions = assertClause();
                }
                String moduleRequest = from.getModuleSpecifier().getValue();
                module.addModuleRequest(ModuleRequest.create(moduleRequest, assertions));
                module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, exportName, from, assertions));
                endOfLine();
                break;
            }
            case LBRACE: {
                NamedExportsNode exportClause = namedExports();
                FromNode from = null;
                if (type == FROM) {
                    from = fromClause();
                    if (env.importAssertions && type == ASSERT && last != EOL) {
                        assertions = assertClause();
                    }
                    String moduleRequest = from.getModuleSpecifier().getValue();
                    module.addModuleRequest(ModuleRequest.create(moduleRequest, assertions));
                }
                module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, exportClause, from, assertions));
                endOfLine();
                break;
            }
            case DEFAULT: {
                next();
                Expression assignmentExpression;
                IdentNode ident = null;
                int lineNumber = line;
                long rhsToken = token;
                boolean hoistableDeclaration = false;
                switch (type) {
                    case FUNCTION:
                        assignmentExpression = functionDeclaration(false, true, false, yield, await, true);
                        hoistableDeclaration = true;
                        break;
                    case CLASS:
                        assignmentExpression = classDeclaration(yield, await, true);
                        ident = ((ClassNode) assignmentExpression).getIdent();
                        break;
                    default:
                        if (isAsync() && lookaheadIsAsyncFunction()) {
                            assignmentExpression = asyncFunctionDeclaration(false, true, yield, await, true);
                            hoistableDeclaration = true;
                            break;
                        }
                        assignmentExpression = assignmentExpression(true, yield, await);
                        endOfLine();
                        break;
                }
                if (hoistableDeclaration) {
                    FunctionNode functionNode = (FunctionNode) assignmentExpression;
                    assert functionNode.isDeclared();
                    if (!functionNode.isAnonymous()) {
                        ident = functionNode.getIdent();
                    }
                }
                if (ident == null) {
                    ident = new IdentNode(Token.recast(rhsToken, IDENT), finish, Module.DEFAULT_EXPORT_BINDING_NAME);

                    if (isAnonymousFunctionDefinition(assignmentExpression)) {
                        assignmentExpression = setAnonymousFunctionName(assignmentExpression, Module.DEFAULT_NAME);
                    }
                }
                VarNode varNode = new VarNode(lineNumber, Token.recast(rhsToken, hoistableDeclaration ? VAR : LET), finish, ident, assignmentExpression,
                                (hoistableDeclaration ? 0 : VarNode.IS_LET) | VarNode.IS_EXPORT);
                declareVar(lc.getCurrentScope(), varNode);
                if (hoistableDeclaration) {
                    functionDeclarations.add(varNode);
                } else {
                    lc.appendStatementToCurrentNode(varNode);
                }
                module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, ident, assignmentExpression, true));
                break;
            }
            case VAR:
            case LET:
            case CONST: {
                List<Statement> statements = lc.getCurrentBlock().getStatements();
                int previousEnd = statements.size();
                variableStatement(type, yield, await);
                for (Statement statement : statements.subList(previousEnd, statements.size())) {
                    if (statement instanceof VarNode) {
                        VarNode varNode = (VarNode) statement;
                        module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, varNode.getName(), varNode));
                    }
                }
                break;
            }
            case CLASS: {
                ClassNode classDeclaration = classDeclaration(yield, await, false);
                module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, classDeclaration.getIdent(), classDeclaration, false));
                break;
            }
            case FUNCTION: {
                FunctionNode functionDeclaration = (FunctionNode) functionDeclaration(true, true, false, yield, await, false);
                module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, functionDeclaration.getIdent(), functionDeclaration, false));
                break;
            }
            default:
                if (isAsync() && lookaheadIsAsyncFunction()) {
                    FunctionNode functionDeclaration = (FunctionNode) asyncFunctionDeclaration(true, true, yield, await, false);
                    module.addExport(new ExportNode(exportToken, Token.descPosition(exportToken), finish, functionDeclaration.getIdent(), functionDeclaration, false));
                    break;
                }
                throw error(AbstractParser.message("invalid.export"), token);
        }
    }

    /**
     * Parse a named exports clause.
     *
     * <pre>
     * NamedExports :
     *     { }
     *     { ExportsList }
     *     { ExportsList , }
     * ExportsList :
     *     ExportSpecifier
     *     ExportsList , ExportSpecifier
     * ExportSpecifier :
     *     IdentifierName
     *     IdentifierName as IdentifierName
     * </pre>
     *
     * @return a list of ExportSpecifiers
     */
    private NamedExportsNode namedExports() {
        final long startToken = token;
        assert type == LBRACE;
        next();
        ArrayList<ExportSpecifierNode> exports = new ArrayList<>();
        long reservedWordToken = 0L;
        while (type != RBRACE) {
            long nameToken = token;
            TokenType nameType = type;
            IdentNode localName = getIdentifierName();
            if (isReservedWord(nameType) || (isEscapedIdent(localName) && (isReservedWordSequence(localName.getName()) || isFutureStrictName(localName)))) {
                // Reserved words are allowed iff the ExportClause is followed by a FromClause.
                // Remember the first reserved word and throw an error if this is not the case.
                if (reservedWordToken == 0L) {
                    reservedWordToken = nameToken;
                }
            }
            if (type == AS) {
                next();
                IdentNode exportName = getIdentifierName();
                exports.add(new ExportSpecifierNode(nameToken, Token.descPosition(nameToken), finish, localName, exportName));
            } else {
                exports.add(new ExportSpecifierNode(nameToken, Token.descPosition(nameToken), finish, localName, null));
            }
            if (type == COMMARIGHT) {
                next();
            } else {
                break;
            }
        }
        expect(RBRACE);

        if (reservedWordToken != 0L && type != FROM) {
            throw error(expectMessage(IDENT, reservedWordToken), reservedWordToken);
        }

        return new NamedExportsNode(startToken, Token.descPosition(startToken), finish, optimizeList(exports));
    }

    private static boolean isReservedWord(TokenType type) {
        return type.getKind() == TokenKind.KEYWORD || type.getKind() == TokenKind.FUTURE || type.getKind() == TokenKind.FUTURESTRICT;
    }

    @Override
    public String toString() {
        return "'JavaScript Parsing'";
    }

    private void markEval() {
        lc.setCurrentFunctionFlag(FunctionNode.HAS_EVAL | FunctionNode.HAS_SCOPE_BLOCK);
        lc.getCurrentScope().setHasEval();
    }

    private void prependStatement(final Statement statement) {
        lc.prependStatementToCurrentNode(statement);
    }

    private void appendStatement(final Statement statement) {
        lc.appendStatementToCurrentNode(statement);
    }

    private void markSuperCall() {
        ParserContextFunctionNode fn = lc.getCurrentNonArrowFunction();
        if (!fn.isProgram()) {
            assert fn.isDerivedConstructor();
            fn.setFlag(FunctionNode.HAS_DIRECT_SUPER);
        }
    }

    private void markThis() {
        lc.setCurrentFunctionFlag(FunctionNode.USES_THIS);
        addIdentifierReference(THIS.getName());
    }

    private void markNewTarget() {
        if (!lc.getCurrentScope().inFunction()) {
            // `new.target` in script or module (may be wrapped in an arrow function or eval).
            throw error(AbstractParser.message("new.target.in.function"), token);
        }
        ParserContextFunctionNode fn = lc.getCurrentNonArrowFunction();
        if (!fn.isProgram()) {
            fn.setFlag(FunctionNode.USES_NEW_TARGET);
        }
        addIdentifierReference(NEW_TARGET_NAME);
    }

    private static boolean markApplyArgumentsCall(final ParserContext lc, List<Expression> arguments) {
        assert arguments.size() == 2 && arguments.get(1) instanceof IdentNode && ((IdentNode) arguments.get(1)).isArguments();
        ParserContextFunctionNode currentFunction = lc.getCurrentFunction();
        if (!currentFunction.isArrow()) {
            currentFunction.setFlag(FunctionNode.HAS_APPLY_ARGUMENTS_CALL);
            arguments.set(1, ((IdentNode) arguments.get(1)).setIsApplyArguments());
            return true;
        }
        return false;
    }

    private boolean isAwait() {
        return ES8_ASYNC_FUNCTION && isES2017() && type == AWAIT;
    }

    private boolean isAsync() {
        return ES8_ASYNC_FUNCTION && isES2017() && type == ASYNC;
    }

    private boolean lookaheadIsAsyncArrowParameterListStart() {
        assert isAsync();
        // find [no LineTerminator here] (IDENT || LPAREN)
        int i = 1;
        for (;;) {
            TokenType t = T(k + i++);
            if (t == LPAREN) {
                break;
            } else if (t == IDENT) {
                break;
            } else if (t.isContextualKeyword()) {
                break;
            } else if (t == COMMENT) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean lookaheadIsAsyncFunction() {
        assert isAsync();
        // find [no LineTerminator here] FUNCTION
        for (int i = 1;; i++) {
            long currentToken = getToken(k + i);
            TokenType t = Token.descType(currentToken);
            switch (t) {
                case COMMENT:
                    continue;
                case FUNCTION:
                    return true;
                default:
                    return false;
            }
        }
    }

    private boolean lookaheadIsAsyncMethod(boolean allowPrivate) {
        assert isAsync();
        // find [no LineTerminator here] ClassElementName
        // find [no LineTerminator here] *
        for (int i = 1;; i++) {
            long currentToken = getToken(k + i);
            TokenType t = Token.descType(currentToken);
            if (t == COMMENT) {
                continue;
            } else {
                return isPropertyName(currentToken) || t == MUL || (allowPrivate && t == TokenType.PRIVATE_IDENT);
            }
        }
    }

    /**
     * Parse and return an expression. Errors will be thrown and the error manager will contain
     * information if parsing should fail.
     *
     * @return expression node resulting from successful parse
     */
    public Expression parseExpression() {
        try {
            prepareLexer(0, source.getLength());
            scanFirstToken();

            return expression(false, false);
        } catch (final Exception e) {
            handleParseException(e);

            return null;
        }
    }
}
