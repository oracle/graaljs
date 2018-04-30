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
package com.oracle.truffle.js.parser;

import java.io.PrintWriter;
import java.util.function.Function;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Lexer.LexerToken;
import com.oracle.js.parser.Lexer.RegexToken;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.ScriptEnvironment.FunctionStatementBehavior;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.parser.internal.ir.debug.JSONWriter;
import com.oracle.truffle.js.parser.json.JSONParser;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;

public class GraalJSParserHelper {

    private static final String NEVER_PART_OF_COMPILATION_MESSAGE = "do not parse from compiled code";

    public static FunctionNode parseScript(com.oracle.truffle.api.source.Source truffleSource, GraalJSParserOptions parserOptions) {
        return parseScript(truffleSource, parserOptions, false, false);
    }

    public static FunctionNode parseScript(com.oracle.truffle.api.source.Source truffleSource, GraalJSParserOptions parserOptions, boolean eval, boolean evalInGlobalScope) {
        FunctionNode parsed = parseSource(truffleSource, parserOptions, false, eval);
        GraalJSTranslator.earlyVariableDeclarationPass(parsed, parserOptions, eval, evalInGlobalScope);
        return parsed;
    }

    public static FunctionNode parseModule(com.oracle.truffle.api.source.Source truffleSource, GraalJSParserOptions parserOptions) {
        return parseSource(truffleSource, parserOptions, true, false);
    }

    private static FunctionNode parseSource(com.oracle.truffle.api.source.Source truffleSource, GraalJSParserOptions parserOptions, boolean parseModule, boolean eval) {
        CompilerAsserts.neverPartOfCompilation(NEVER_PART_OF_COMPILATION_MESSAGE);
        CharSequence code = truffleSource.getCharacters();
        com.oracle.js.parser.Source source = com.oracle.js.parser.Source.sourceFor(truffleSource.getName(), code, eval);

        ScriptEnvironment env = makeScriptEnvironment(parserOptions);
        ErrorManager errors;
        if (eval) {
            errors = new ErrorManager.ThrowErrorManager();
        } else {
            errors = new ErrorManager.StringBuilderErrorManager();
        }
        errors.setLimit(0);

        Parser parser = createParser(env, source, errors, parserOptions);
        FunctionNode parsed = parseModule ? parser.parseModule(":module") : parser.parse();
        if (errors.hasErrors()) {
            throwErrors(truffleSource, errors);
        }
        return parsed;
    }

    public static Expression parseExpression(com.oracle.truffle.api.source.Source truffleSource, GraalJSParserOptions parserOptions) {
        CompilerAsserts.neverPartOfCompilation(NEVER_PART_OF_COMPILATION_MESSAGE);
        CharSequence code = truffleSource.getCharacters();
        com.oracle.js.parser.Source source = com.oracle.js.parser.Source.sourceFor(truffleSource.getName(), code, true);

        ScriptEnvironment env = makeScriptEnvironment(parserOptions);
        ErrorManager errors = new ErrorManager.ThrowErrorManager();
        errors.setLimit(0);

        Parser parser = createParser(env, source, errors, parserOptions);
        Expression expression = parser.parseExpression();
        if (errors.hasErrors()) {
            throwErrors(truffleSource, errors);
        }

        expression.accept(new com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext>(new LexicalContext()) {
            @Override
            public boolean enterFunctionNode(FunctionNode functionNode) {
                GraalJSTranslator.functionVarDeclarationPass(functionNode, parserOptions);
                return false;
            }
        });

        return expression;
    }

    private static Parser createParser(ScriptEnvironment env, com.oracle.js.parser.Source source, ErrorManager errors, GraalJSParserOptions parserOptions) {
        return new Parser(env, source, errors) {
            @Override
            protected void validateLexerToken(LexerToken lexerToken) {
                if (lexerToken instanceof RegexToken) {
                    final RegexToken regex = (RegexToken) lexerToken;
                    // validate regular expression
                    if (JSTruffleOptions.ValidateRegExpLiterals) {
                        try {
                            RegexCompilerInterface.validate(regex.getExpression(), regex.getOptions(), parserOptions.getEcmaScriptVersion());
                        } catch (JSException e) {
                            throw error(e.getRawMessage());
                        }
                    }
                }
            }

            @Override
            protected Function<Number, String> getNumberToStringConverter() {
                return JSRuntime::numberToString;
            }
        };
    }

    private static ScriptEnvironment makeScriptEnvironment(GraalJSParserOptions parserOptions) {
        ScriptEnvironment.Builder builder = ScriptEnvironment.builder();
        builder.strict(parserOptions.isStrict());
        builder.es6(parserOptions.isES6());
        builder.es8(parserOptions.isES8());
        builder.emptyStatements(parserOptions.isEmptyStatements());
        builder.syntaxExtensions(parserOptions.isSyntaxExtensions());
        builder.scripting(parserOptions.isScripting());
        builder.shebang(parserOptions.isShebang());
        builder.constAsVar(parserOptions.isConstAsVar());
        if (parserOptions.isFunctionStatementError()) {
            builder.functionStatementBehavior(FunctionStatementBehavior.ERROR);
        } else {
            builder.functionStatementBehavior(FunctionStatementBehavior.ACCEPT);
        }
        if (parserOptions.isDumpOnError()) {
            builder.dumpOnError(new PrintWriter(System.err, true));
        }
        return builder.build();
    }

    public static boolean checkFunctionSyntax(GraalJSParserOptions parserOptions, String parameterList, String body, boolean generator, boolean async) {
        CompilerAsserts.neverPartOfCompilation(NEVER_PART_OF_COMPILATION_MESSAGE);
        ScriptEnvironment env = makeScriptEnvironment(parserOptions);
        ErrorManager errors = new com.oracle.js.parser.ErrorManager.ThrowErrorManager();
        Parser parser = createParser(env, com.oracle.js.parser.Source.sourceFor(Evaluator.FUNCTION_SOURCE_NAME, parameterList), errors, parserOptions);
        boolean endsWithLineComment = parser.parseFormalParameterList();
        parser = createParser(env, com.oracle.js.parser.Source.sourceFor(Evaluator.FUNCTION_SOURCE_NAME, body), errors, parserOptions);
        parser.parseFunctionBody(generator, async);
        return endsWithLineComment;
    }

    private static void throwErrors(com.oracle.truffle.api.source.Source source, ErrorManager errors) {
        ParserException parserException = errors.getParserException();
        SourceSection sourceLocation = null;
        if (parserException != null) {
            if (parserException.getPosition() >= 0) {
                // For EOL tokens, length is the line number
                int length = Token.descType(parserException.getToken()) == TokenType.EOL ? 0 : Token.descLength(parserException.getToken());
                sourceLocation = source.createSection(parserException.getPosition(), length);
            }
            if (parserException.getErrorType() == com.oracle.js.parser.JSErrorType.ReferenceError) {
                throw Errors.createReferenceError(parserException.getMessage(), sourceLocation);
            } else {
                assert parserException.getErrorType() == com.oracle.js.parser.JSErrorType.SyntaxError;
            }
        }
        throw Errors.createSyntaxError(((ErrorManager.StringBuilderErrorManager) errors).getOutput(), sourceLocation);
    }

    public static Object parseJSON(String jsonString, JSContext context) throws ParserException {
        JSONParser jsonParser = new JSONParser(jsonString, context);
        try {
            return jsonParser.parse();
        } catch (ParserException ex) {
            throw TruffleJSONParser.createSyntaxError(ex, context);
        }
    }

    public static String parseToJSON(String code, String name, boolean includeLoc, GraalJSParserOptions parserOptions) {
        CompilerAsserts.neverPartOfCompilation(NEVER_PART_OF_COMPILATION_MESSAGE);
        ScriptEnvironment env = makeScriptEnvironment(parserOptions);
        try {
            return JSONWriter.parse(env, code, name, includeLoc);
        } catch (ParserException e) {
            throw Errors.createSyntaxError(e.getMessage());
        }
    }
}
