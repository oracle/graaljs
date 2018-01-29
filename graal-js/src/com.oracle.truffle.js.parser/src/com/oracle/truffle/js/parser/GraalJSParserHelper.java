/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.parser.internal.ir.debug.JSONWriter;
import com.oracle.truffle.js.parser.json.JSONParser;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.RegexCompiler;

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
            throwErrors(errors);
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
            throwErrors(errors);
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
                            RegexCompiler.validate(regex.getExpression(), regex.getOptions(), parserOptions.getEcmaScriptVersion());
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
        builder.earlyLvalueError(parserOptions.isEarlyLvalueError());
        builder.emptyStatements(parserOptions.isEmptyStatements());
        builder.syntaxExtensions(parserOptions.isSyntaxExtensions());
        builder.scripting(parserOptions.isScripting());
        builder.shebang(parserOptions.isShebang());
        builder.constAsVar(parserOptions.isConstAsVar());
        if (parserOptions.isFunctionStatementError()) {
            builder.functionStatementBehavior(FunctionStatementBehavior.ERROR);
        } else if (parserOptions.isFunctionStatementWarning()) {
            builder.functionStatementBehavior(FunctionStatementBehavior.WARNING);
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

    private static void throwErrors(ErrorManager errors) {
        ParserException parserException = errors.getParserException();
        if (parserException != null) {
            if (parserException.getErrorType() == com.oracle.js.parser.JSErrorType.ReferenceError) {
                throw Errors.createReferenceError(parserException.getMessage());
            } else {
                assert parserException.getErrorType() == com.oracle.js.parser.JSErrorType.SyntaxError;
            }
        }
        throw Errors.createSyntaxError(((ErrorManager.StringBuilderErrorManager) errors).getOutput());
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
